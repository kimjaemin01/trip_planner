# 여행 플래너 앱 (Travel Planner)

국가·지역·여행 스타일·기간을 선택하면 최적 동선과 비용·숙소·날씨·축제 정보를 제공하는 여행 계획 앱.
초기 버전(MVP)은 **일본**부터 개발 → 같은 구조로 베트남 → 제주도 → 중국 확장.

---

## 목차
0. [핵심 설계 원칙](#0-핵심-설계-원칙)
1. [기능 정의](#1-기능-정의)
2. [화면 흐름](#2-화면-흐름)
3. [기술 스택](#3-기술-스택)
4. [외부 API](#4-외부-api)
5. [시스템 구조](#5-시스템-구조)
6. [데이터 (CSV)](#6-데이터-csv)
7. [ERD](#7-erd)
8. [추천 로직](#8-추천-로직)
9. [환경 세팅 매뉴얼](#9-환경-세팅-매뉴얼)
10. [개발 순서](#10-개발-순서)

---

## 0. 핵심 설계 원칙

> **새 국가·지역 추가 = 코드 수정 없이 DB INSERT만** → 서버 재배포·앱 업데이트 없이 사용자 화면 반영

| 규칙 | 내용 |
|---|---|
| 하드코딩 금지 | 앱·서버에 국가·지역명 직접 작성 X → 전부 DB 조회 |
| 화면 = API 응답 | 국가 목록·지역 카드·추천도·근거 문구 전부 서버 데이터로 렌더링 |
| 이미지 | 앱 내장 X → `image_url` 컬럼 |
| 노출 스위치 | `is_active` → 데이터 준비 끝나면 `TRUE` |
| 정렬 | `sort_order` 컬럼 |
| 외부 API 선택 | `country.routing_provider / place_provider` (GOOGLE / KAKAO / AMAP) → 서버가 값 보고 구현체 선택 |
| 배치 | DB의 활성 국가·지역 전체 대상 → 새 지역 자동 계산 |
| 캐시 | TTL or 관리자 갱신 API → 즉시 반영 |

### 새 국가 추가 절차
| 순서 | 작업 | 코드 수정 |
|---|---|---|
| 1 | CSV 작성 → INSERT문 변환 (country·area·region·스타일점수·렌트규칙) | ❌ |
| 2 | INSERT (`is_active = FALSE`) | ❌ |
| 3 | 배치 실행 → 이동시간·날씨 수집 | ❌ |
| 4 | `UPDATE ... SET is_active = TRUE` | ❌ |
| 5 | 앱 자동 노출 | ❌ |

| 예외 | 내용 |
|---|---|
| 새 API 제공자 | 제주(KAKAO)·중국(AMAP)은 구현체 **1회** 개발 → 이후 같은 제공자 국가는 데이터만 |

---

## 1. 기능 정의

### 기본 기능
| 기능 | 내용 |
|---|---|
| 여행 스타일 선택 | 휴양, 쇼핑, 미식, 하루 3만보 관광, 한적한 여유, 자연, 온천, 테마파크 (1~2개) |
| 국가 선택 | 1개만 (다국가 이동은 차후) |
| 지역 선택 | 광역 → 세부 도시/관광지, 스타일 기준 추천도 + 한줄 근거 표시 |
| 기간 선택 | 정확한 날짜 or 기간 범위 (3~5일, 6~10일) |
| 추천 동선 | 일정 내 최적 동선, 일정 초과 시 경고 + 대안 동선 |
| 교통비 계산 | 렌트·대중교통·택시 구간별 설정, 최저가 / 효율 / 최단시간 옵션 |
| 렌트 추천 여부 | 광역 × 월 기준 (대중교통 발달도 + 날씨), 세부 예외 |
| 숙소 위치 추천 | 저렴 / 치안 / 교통 권역 구분 |
| 축제 정보 | 여행 기간 중 해당 지역 축제 알림 |
| 날씨·준비물 | 월별 평균 날씨 → 옷차림·준비물 추천 |
| 식비 예상 | 현지 평균 한끼 기준 하루 식비 |
| 특산품·후기 | 기념품 3~5개 추천, 사용자 후기(음식·맛집·체험·기념품) 공유 |
| 경로 저장 | 최종 동선 저장 |

### 여행 중 기능
| 기능 | 내용 |
|---|---|
| 실시간 위치 | GPS → 지도 표시 |
| 오프라인 | 저장된 계획·경로·메모·예약정보 오프라인 확인, 오프라인 지도 다운로드 |
| 경유지 메모 | 지도 경유지 클릭 → 메모 작성 |

### 추가 기능
| 기능 | 내용 |
|---|---|
| 출발지·항공편 | 입·출국 공항, 도착·출발 시간 → 첫날·마지막날 가용시간 |
| 영업시간·휴무일 | 동선 오류 방지 |
| 환율 | 원화 환산 (3일 1회 갱신) |
| 예약 정보 보관 | 항공권·숙소 바우처 (오프라인) |
| 동행자 공유 | 공동 편집 |
| 지출 기록·정산 | 예산 vs 실제, 더치페이 |
| 입국 정보 | 비자, 전압·플러그, 팁 문화, 대사관 연락처 |
| 여행경보 | 외교부 여행경보 |
| 체크리스트 | 날씨 기반 자동 생성 |
| 일정 알림 | 출발 전 푸시 |
| 언어 표시 | 저장된 일정 화면 → 국가 공통 언어 / 지역 특정 언어 |

### 제외
| 기능 | 이유 |
|---|---|
| 번역 | 기본 번역 앱으로 충분, 앱 무거워짐 → 언어 표시로 대체 |

### 비용 데이터 정책
| 항목 | 정책 |
|---|---|
| 렌트비 | 데이터 있음 → 평균값 / 부족 → 추천·비추천만 |
| 사용자 비용 후기 | 렌트·택시 실제 비용 후기 → 일정 개수 이상 쌓이면 **중앙값** 사용 |
| 후기 저장 단위 | 1일당·1인당·차종으로 정규화 |

---

## 2. 화면 흐름

### 공통 시작
```
여행 플랜 만들기 → 여행 스타일 선택 → 국가 선택 → 모드 선택(직접 / 추천)
```

### A. 직접 선택 모드
| 단계 | 동작 |
|---|---|
| 1 | 국가 선택 |
| 2 | 광역 선택 |
| 3 | 세부 선택 → 경유지 추가 |
| [다음 여행지] | 1·2 유지, 3만 다시 선택 |
| 광역 이동 시 | 같은 화면에서 2만 변경 → 해당 광역의 세부 목록 |

### B. 추천 모드
| 단계 | 동작 |
|---|---|
| 1 | 국가 선택 (1개) |
| 2 | 광역 복수 선택 |
| 3 | 기간 선택 |
| 결과 | "이 일정에는 ○○, ○○ 추천" + 최적 동선 + 렌트 추천 여부 |
| 원칙 | 선택 광역 전부 포함 X → 일정 내 최대한 |

### 세부 일정 계획 (공통)
```
[세부 일정 계획] → 교통수단·렌트(금액·소요시간) → 교통비 → 숙소 권역 → 식비 → 날씨·준비물 → 축제 → 저장
```

### 저장 이후
```
저장 일정 화면(언어 표시) → 여행 중 지도(실시간 위치 + 경유지 메모) → 여행 후 후기 작성
```

---

## 3. 기술 스택

| 영역 | 기술 | 비고 |
|---|---|---|
| 앱 | Flutter | iOS·Android 동시 |
| 앱 로컬 DB | SQLite (drift) | 오프라인 저장 |
| 지도 (온라인) | Google Maps SDK (`google_maps_flutter`) | 모바일 SDK 지도 표시 무료 |
| 지도 (오프라인) | MapLibre + OSM 타일 | 오프라인 단계에서 추가 |
| 길안내 | 구글맵 앱 딥링크 | 무료 |
| 백엔드 | Spring Boot 4.1.1 + JDK 21 (Gradle) | |
| 배치 | Spring Scheduler | 환율·날씨·이동시간 행렬 |
| DB | PostgreSQL 16 + PostGIS | Docker |
| 인증 | Spring Security + JWT (+ 소셜 로그인) | |
| 푸시 | FCM | |
| 파일 | S3 호환 스토리지 | 예약 바우처 |
| 배포 | AWS / Oracle Cloud 무료 티어 | |
| IDE | VS Code | Android Studio는 SDK·에뮬레이터용 |

---

## 4. 외부 API

> 원칙: 앱에서 외부 API 직접 호출 X → **전부 백엔드 경유** + 캐싱/배치로 무료 한도 절약

### 국가별 API 조합
| 데이터 | 일본 | 베트남 | 중국 | 제주도 |
|---|---|---|---|---|
| 지도 표시 | Google Maps SDK | Google Maps SDK | MapLibre + OSM | Google Maps SDK |
| 경로·이동시간 | Google Routes | Google Routes | Amap | 카카오모빌리티 / ODsay |
| 장소·영업시간 | Google Places | Google Places | Amap | 카카오 로컬 |
| 날씨 | Open-Meteo | Open-Meteo | Open-Meteo | 기상청 API |
| 축제 | 직접 수집 | 직접 수집 | 직접 수집 | 한국관광공사 TourAPI |
| 환율 | ExchangeRate-API | ExchangeRate-API | ExchangeRate-API | 불필요 |
| 여행경보 | 외교부 API | 외교부 API | 외교부 API | 불필요 |

### Google Maps Platform 무료 정책 (2025.03~)
| 구분 | 월 무료 |
|---|---|
| Essentials SKU | 10,000건 |
| Pro SKU | 5,000건 |
| Enterprise SKU | 1,000건 |
| Maps SDK (모바일 지도 표시) | 무제한 |

- SKU별 따로 계산, 매월 초기화
- **기본 지출 상한 없음 → Google Cloud 콘솔에서 API별 일일 할당량 제한 필수**
- 요금 정책은 바뀔 수 있음 → 개발 시작 전 공식 문서 확인

### 호출 절약 설계
| 항목 | 방식 |
|---|---|
| 지역 간 이동시간 | 배치 1회 계산 → `region_travel_matrix` 저장 |
| 장소 간 경로 | 요청 시 계산 → 캐싱 |
| 경로선 | 일정 저장 시 Routes API 1회 → `trip_leg.polyline` 저장 → 오프라인 표시 |
| 환율 | 3일 1회 배치 |
| 날씨 | 월별 평균 수집 배치 |

### 국가별 주의
| 지역 | 내용 |
|---|---|
| 중국 | 현지 Google 차단, Amap 좌표계 GCJ-02 → WGS-84 변환 필요, Amap 가입 조건 확인 |
| 제주도 | 한국은 Google 자동차 길찾기 미지원 → 카카오·ODsay |

---

## 5. 시스템 구조

> 국가 추가 시 **데이터 + 외부 API 구현체**만 교체, 화면·로직·DB 구조는 그대로

### 백엔드 패키지
| 패키지 | 역할 | 국가별 변경 |
|---|---|---|
| `domain.country / area / region / place` | 기준 데이터 | ❌ |
| `domain.trip` | 여행계획·경유지·메모 | ❌ |
| `domain.recommend` | 지역 추천·동선 추천·일정 초과 판단 | ❌ |
| `domain.cost` | 교통비·식비·렌트 | ❌ |
| `domain.review` | 비용 후기·장소 후기 | ❌ |
| `domain.expense` | 지출·정산 | ❌ |
| `external.routing` | 경로 API 인터페이스 + 구현체 (Google / Kakao / Amap) | ⭕ |
| `external.place` | 장소 API 인터페이스 + 구현체 | ⭕ |
| `external.weather / exchange / festival` | 수집 API | 일부 |
| `batch` | 환율·날씨·이동시간 행렬 | ❌ |
| `config` | 국가 코드 → 구현체 매핑 | 설정만 |

### 앱 화면 영역
| 영역 | 화면 |
|---|---|
| 시작 | 스타일 → 국가 → 모드 분기 |
| 계획 | 지역 카드 → 기간 → 동선 결과 → 세부 일정 → 저장 |
| 여행 중 | 지도 + 실시간 위치 + 경유지 메모 |
| 보관함 | 예약 정보, 체크리스트, 지출 |
| 공통 | 로컬 DB, 동기화 |

### 개발 국가 순서
1. 일본 (Google 기본형)
2. 베트남 (같은 조합, 데이터만 추가)
3. 제주도 (국내 API 구현체)
4. 중국 (Amap + 좌표 변환)

---

## 6. 데이터 (CSV)

### 파일 목록
| 파일 | 내용 | 행 수 |
|---|---|---|
| `country.csv` | 국가 기본정보 | 1 |
| `country_language.csv` | 국가 공통 언어 | 1 |
| `area.csv` | 광역 6개 + 대표공항 + 평균체류 | 6 |
| `region.csv` | 세부 27개 + 좌표 + 체류일 + 유형 + 거점 + 묶음 + 한줄근거 | 27 |
| `travel_style.csv` | 스타일 8개 + 하루 방문 수 + 체류 성향 | 8 |
| `region_style_score.csv` | 세부 × 스타일 점수 (1~5) | 216 |
| `rent_rule.csv` | 광역 × 월 렌트 판정 + 근거 | 72 |
| `region_rent_exception.csv` | 세부 렌트 예외 | 5 |

### 일본 지역 구성
| 광역 | 세부 |
|---|---|
| 수도권·간토 | 도쿄, 요코하마, 하코네, 가마쿠라&에노시마, 닛코, 가루이자와 |
| 간사이·주고쿠 | 오사카, 교토, 나라, 고베, 히로시마, 미야지마 |
| 시코쿠 | 다카마쓰, 마쓰야마 |
| 규슈 | 후쿠오카, 유후인, 벳푸, 나가사키, 구마모토, 가고시마 |
| 홋카이도 | 삿포로, 오타루, 비에이&후라노, 하코다테 |
| 오키나와 | 나하, 중부, 북부 |

### 코드값
| 컬럼 | 값 |
|---|---|
| `stay_type` | BASE 거점 / BASE_OPTIONAL 거점가능 / DAYTRIP 당일치기 / OVERNIGHT 숙박권장 / BUNDLE 묶음 / DRIVE 드라이브 |
| `stay_tendency` | MIN 압축 / REC 기본 / MAX 확장 |
| `judgment` | RECOMMEND / OPTIONAL / NOT_RECOMMEND |
| `main_airport` | `\|` 구분 (예: HND\|NRT) |
| `group_id` | 1 히로시마·미야지마 / 2 유후인·벳푸 / 3 삿포로·오타루 |
| `daily_visit_count` | 하루 일정에 넣을 장소 개수 (점수 아님) |
| 체류일 단위 | 일, 0.5 = 반일 |

### CSV import 규칙
| 항목 | 내용 |
|---|---|
| 순서 (FK) | country → country_language → area → region → travel_style → region_style_score → rent_rule → region_rent_exception |
| 헤더 | 첫 줄 = 컬럼명 → import 시 헤더 포함 옵션 |
| 빈 값 | NULL로 들어감 → 해당 컬럼 NULL 허용 |
| 인코딩 | UTF-8 (엑셀로 열면 한글 깨질 수 있음 → VS Code로 편집) |
| 방법 | `db/seed.sql` (CSV를 INSERT문으로 변환한 파일) — CSV 수정 시 seed.sql 재생성 |

---

## 7. ERD

### 기준 데이터
| 테이블 | PK | FK | 주요 컬럼 | 소스 |
|---|---|---|---|---|
| country | country_id | | 이름, 통화, 전압, 플러그, is_active, sort_order, image_url, routing_provider, place_provider | CSV |
| country_language | (country_id, language) | country | 공통 여부 | CSV |
| region_language | (region_id, language) | region | 지역 특정 언어 | 직접 입력 |
| area | area_id | country | 이름, 대표공항, 평균체류, is_active, image_url | CSV |
| region | region_id | area, 자기참조(거점) | 좌표, 체류 min/rec/max, 유형, 묶음, 한줄근거, is_active, sort_order, image_url | CSV |
| travel_style | style_id | | 하루 방문 수, 체류 성향 | CSV |
| region_style_score | (region_id, style_id) | region, style | 점수 | CSV |
| rent_rule | (area_id, month) | area | 판정, 근거 | CSV |
| region_rent_exception | region_id | region | 판정, 근거 | CSV |
| region_travel_matrix | (from, to, mode) | region ×2 | 소요시간, 거리, 평균비용, 갱신일 | 배치 |
| place | place_id | region | google_place_id, 이름, 분류, 좌표, 영업시간, 휴무일 | Places API |
| lodging_area | area_id | region | 이름, 중심좌표, 반경, 유형(저렴·치안·교통) | 직접 입력 |
| price_info | region_id | region | 한끼 식비, 택시 기본·km당, 대중교통 평균, 렌트 일평균 | 직접 입력 |
| weather_monthly | (region_id, month) | region | 최고·최저기온, 강수량, 강수일 | 배치 |
| festival | festival_id | region | 이름, 기간, 설명, 출처 | 직접 입력 |
| exchange_rate | (currency, fetched_at) | | 환율 | 배치 |

### 사용자 데이터
| 테이블 | PK | FK | 주요 컬럼 |
|---|---|---|---|
| users | user_id | | 이메일, 닉네임, 로그인 방식 |
| trip | trip_id | users, country | 제목, 모드, 시작·종료일 or 기간범위, 인원, 경로옵션, 예산, 입·출국 공항·시간 |
| trip_style | (trip_id, style_id) | trip, style | |
| trip_member | (trip_id, user_id) | trip, users | 권한 |
| trip_stop | stop_id | trip, region, place | 일차, 순서, 도착예정, 체류시간, 메모 |
| trip_leg | leg_id | trip, stop ×2 | 교통수단, 소요시간, 거리, 비용, polyline |
| trip_lodging | (trip_id, day) | trip, lodging_area | 숙소명 |
| reservation | reservation_id | trip | 유형, 예약번호, 파일경로 |
| checklist_item | item_id | trip | 항목, 체크여부, 자동생성 여부 |
| expense | expense_id | trip, users | 금액, 통화, 분류, 일시 |
| expense_split | (expense_id, user_id) | expense, users | 부담액 |

### 후기 데이터
| 테이블 | PK | FK | 주요 컬럼 |
|---|---|---|---|
| cost_review | review_id | region, users | 유형(렌트·택시), 일수, 총비용, 인원, 차종 |
| place_review | review_id | region, users | 분류(음식·맛집·체험·기념품), 이름, 내용, 평점 |

### ERD 규칙
| 항목 | 내용 |
|---|---|
| stop / leg 분리 | 경유지와 이동구간 분리 → 구간별 교통수단·비용·경로선 |
| 공통 컬럼 | 사용자 데이터 전부 `created_at`, `updated_at` (동기화 기준) |
| 삭제 | 사용자 데이터 soft delete (`deleted_at`) |
| 오프라인 동기화 대상 | trip, trip_stop, trip_leg, trip_lodging, reservation, checklist_item + 해당 지역 region·weather·price |
| 충돌 처리 | 마지막 수정 시간 기준 |

---

## 8. 추천 로직

### 스타일 추천도
| 항목 | 방식 |
|---|---|
| 세부 추천도 | 선택 스타일 점수 평균 |
| 광역 추천도 | 소속 세부 점수 상위값 평균 |
| 스타일 2개 선택 시 하루 방문 수 | 평균값 |
| 계절 가중치 | 추후 추가 (비에이·닛코·삿포로 등) |

### 체류일 유동 배분
| 상황 | 처리 |
|---|---|
| 기본 | 선택 지역 rec 합 + 이동시간 vs 여행 기간 |
| 여유 | 스타일 점수 높은 지역부터 max까지 확장 |
| 부족 | 점수 낮은 지역부터 min까지 압축 |
| 그래도 부족 | 점수 낮은 지역 제외 → "이 일정에는 ○○ 추천" |
| 첫날·마지막날 | 항공 도착·출발 시간 → 가용시간 → 반일 처리 |
| 당일치기 | 거점 숙소 유지 + 왕복 이동시간 차감 |
| 묶음 그룹 | 둘 다 선택 시 합산, 하나만 선택 시 개별값 |
| 체류시간 | 세부 일정에서 일 → 시간 변환 (1일 ≈ 10시간) |
| 오사카 | 테마파크 미선택 시 체류 축소 |

### 대안 동선
| 항목 | 내용 |
|---|---|
| 문제 유형 | Orienteering Problem (시간 제한 내 방문 수 최대화) |
| 지역 수 적음 | 조합 전체 탐색 |
| 지역 수 많음 | 그리디 / 휴리스틱 |
| 정렬 | ① 선택 지역 겹치는 수 ② 추천도 합 ③ 총 이동시간 |
| 결과 | 대안 3개 |
| 입·출국 공항 | 다른 공항 허용 (간토 IN → 규슈 OUT 등) |
| 광역 간 이동 | 국내선 항공 포함 |

### 렌트 판정
| 항목 | 내용 |
|---|---|
| 표시 시점 | 기간 선택 후 (날씨 반영) |
| 판정 단위 | 광역 × 월 + 세부 예외 |
| 상세 | 세부 일정에서 렌트 선택 시 금액·소요시간 |
| 공통 안내 | 국제운전면허증, 좌측통행·우핸들 |

---

## 9. 환경 세팅 매뉴얼

> Windows 기준 / VS Code 터미널(PowerShell)

### 0. 사전 확인
| 항목 | 방법 |
|---|---|
| 가상화 | 작업관리자 → 성능 → CPU → **가상화: 사용** (Docker·에뮬레이터 필수, 꺼져 있으면 BIOS에서 활성화) |
| 관리자 권한 | VS Code **우클릭 → 관리자 권한으로 실행** → 터미널 열기 |

### 1. WSL2 설치 (Docker용)
```powershell
wsl --install
```

### 2. 프로그램 설치
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Git.Git
winget install Docker.DockerDesktop
winget install Google.AndroidStudio
```
→ **재부팅**

> winget 실패 시 공식 사이트에서만 다운로드
> - Docker Desktop: docker.com → Products → Docker Desktop
> - Android Studio: developer.android.com/studio

### 3. 설치 확인 (재부팅 후 VS Code 다시 열기)
```powershell
java -version
git --version
```

### 4. Flutter SDK
```powershell
mkdir C:\dev
git clone https://github.com/flutter/flutter.git -b stable C:\dev\flutter
$p = [Environment]::GetEnvironmentVariable("Path","User")
[Environment]::SetEnvironmentVariable("Path", "$p;C:\dev\flutter\bin", "User")
```
→ **VS Code 완전 종료 후 재실행**
```powershell
flutter --version
```

### 5. VS Code 확장
```powershell
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension Dart-Code.flutter
code --install-extension ms-azuretools.vscode-docker
code --install-extension ms-ossdata.vscode-pgsql
```

### 6. Docker Desktop 초기 설정 (GUI)
| 순서 | 작업 |
|---|---|
| 1 | 시작메뉴 → Docker Desktop 실행 |
| 2 | 약관 Accept |
| 3 | **Use WSL 2 based engine** 체크 확인 |
| 4 | 로그인 → Skip 가능 |
| 5 | 좌측 하단 **Engine running** 확인 |
| 6 | Docker 사용 시 항상 Docker Desktop 실행 상태 유지 |

### 7. Android Studio 초기 설정 (GUI)
> 코딩용 X → Android SDK·빌드 도구·에뮬레이터 설치용. 코딩은 VS Code.

| 순서 | 작업 |
|---|---|
| 1 | 시작메뉴 → Android Studio 실행 |
| 2 | Import settings → Do not import |
| 3 | Setup Wizard → **Standard** → 다운로드 완료까지 대기 |
| 4 | 첫 화면 → **More Actions → SDK Manager** |
| 5 | **SDK Tools** 탭 → `Android SDK Command-line Tools` 체크 → Apply |
| 6 | 첫 화면 → **More Actions → Virtual Device Manager** |
| 7 | Create Device → Pixel 계열 → 최신 API 이미지 다운로드 → Finish |
| 8 | ▶ 실행 → 에뮬레이터 켜지는지 확인 → 종료 |

### 8. Android 라이선스 + 점검
```powershell
flutter doctor --android-licenses
flutter doctor
```
| 항목 | 기준 |
|---|---|
| Flutter | ✅ 필수 |
| Android toolchain | ✅ 필수 |
| Android Studio | ✅ |
| VS Code | ✅ |
| Visual Studio | 무시 가능 (Windows 데스크톱 앱용) |
| Chrome | 무시 가능 (웹 빌드용) |

### 9. PostgreSQL + PostGIS (Docker Desktop 실행 상태)
```powershell
docker run -d --name travel-db -e POSTGRES_USER=travel -e POSTGRES_PASSWORD=travel1234 -e POSTGRES_DB=travel -p 5432:5432 -v travel-pgdata:/var/lib/postgresql/data postgis/postgis:16-3.4
docker ps
```
| 항목 | 내용 |
|---|---|
| 확인 | `docker ps` → travel-db 상태 Up |
| 포트 충돌 | 로컬 PostgreSQL 설치된 경우 `-p 5433:5432` |
| 비밀번호 | 변경 가능 → Spring 설정과 동일하게 |
| 재시작 | PC 재부팅 후 `docker start travel-db` |

### 10. DB 접속 (VS Code PostgreSQL 확장)
| 항목 | 값 |
|---|---|
| SERVER NAME | `127.0.0.1` (`localhost`는 연결 시간초과 발생) |
| USER / PASSWORD | `travel` / `travel1234` |
| DATABASE NAME | `travel` |
| Advanced → SSL mode | **disable** |

| 연결 안 될 때 | 확인 |
|---|---|
| 컨테이너 상태 | `docker ps`, `docker logs travel-db --tail 20` |
| 포트 | `Test-NetConnection 127.0.0.1 -Port 5432` |
| 대체 도구 | DBeaver Community / `docker exec -it travel-db psql -U travel -d travel` |

### 11. DB 테이블 생성 + 데이터 넣기 (최초 1회)
| 순서 | 작업 |
|---|---|
| 1 | 코끼리 → New Query → `db/schema.sql` 붙여넣기 → ▷ |
| 2 | New Query → `db/seed.sql` 붙여넣기 → ▷ |
| 3 | 마지막 결과 행 수 = 6번 표와 일치 확인 |
| 4 | New Query → `db/matrix_v3.sql` 붙여넣기 → ▷ (지역 간 이동시간 추정값) |

> `docker cp` + `import.sql` 방식은 PC에 따라 파일 복사가 안 되는 문제 → **seed.sql(INSERT문)로 통일**

### DB 변경 적용 (기존 DB가 있는 PC)
| 파일 | 내용 | 실행 |
|---|---|---|
| `db/alter_v2.sql` | is_active·image_url·sort_order·provider 컬럼 추가 | 학교·집 각 1회 |
| `db/matrix_v3.sql` | area.ground_group + region_travel_matrix 테이블·추정값 | 학교·집 각 1회 (재실행 안전) |

> Git Pull은 **파일만** 받아옴 → DB 구조 변경은 각 PC에서 SQL 직접 실행

### 재부팅 후 매번
| 순서 | 작업 |
|---|---|
| 1 | Docker Desktop 실행 → Engine running |
| 2 | Source Control → **Pull** |
| 3 | `docker start travel-db` |
| 4 | `cd C:\trip_planner\backend` → `.\gradlew bootRun` |

### 12. 백엔드 로컬 설정 (clone 후 필수)
> `application-local.properties`는 Git에 안 올라감 (.gitignore) → PC마다 직접 생성

| 항목 | 내용 |
|---|---|
| 위치 | `backend\src\main\resources\application-local.properties` |

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/travel
spring.datasource.username=travel
spring.datasource.password=travel1234
```

### application.properties (Git 포함)
```properties
spring.application.name=backend
spring.profiles.active=local
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.force=true
```
> `encoding.force` 없으면 PowerShell·Flutter에서 한글 깨짐

### 13. 백엔드 실행
```powershell
cd C:\trip_planner\backend
.\gradlew bootRun
```
| 로그 | 의미 |
|---|---|
| `The following 1 profile is active: "local"` | 로컬 설정 적용 |
| `Started BackendApplication` | 성공 (`80% EXECUTING`은 정상) |
| 종료 | `Ctrl+C` |

### 폴더 구조
```
trip_planner/
├─ README.md
├─ data/      CSV 8개
├─ db/        schema.sql, seed.sql, alter_v2.sql, matrix_v3.sql (import.sql 미사용)
├─ backend/   Spring Boot 4.1.1 (Gradle, Java 21)
└─ app/       Flutter (추천 모드 화면)
```

### 주의
| 항목 | 내용 |
|---|---|
| PATH 반영 | 설치 후 VS Code 완전 종료 → 재실행 |
| iOS 빌드 | Mac + Xcode 필수 → Windows는 Android로 개발·테스트 |
| API 키 | 코드·Git에 직접 넣지 말 것 → 환경변수 / `.gitignore` |

---

## 9-1. 백엔드 API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/styles` | 여행 스타일 8개 |
| GET | `/api/countries` | 공개 국가 (is_active, sort_order) |
| GET | `/api/countries/{id}/areas?styles=1,2` | 광역 + 추천도(상위 3개 세부 평균) + topRegions |
| GET | `/api/areas/{id}/regions?styles=1,2` | 세부 지역 + 추천도(선택 스타일 점수 평균) + 한줄 근거 |
| GET | `/api/rent-advice?areaIds=1,5&month=7` | 렌트 추천 여부 (or `startDate`·`endDate`, `regionIds`로 세부 예외) |
| POST | `/api/recommend/route` | 추천 모드: 광역 복수 + 스타일 + 기간 → 최적 동선 + 대안 2개 |
| POST | `/api/route/check` | 직접 선택 모드: 세부 지역(순서) + 기간 → 가능 여부, 초과 시 대안 3개 |

### POST 테스트 (PowerShell, 한글 안 깨지게 파일로 저장)
```powershell
$body = '{"countryId":1,"areaIds":[1,2],"styleIds":[1],"tripDays":5}'
$r = Invoke-WebRequest -Uri http://localhost:8080/api/recommend/route -Method Post -ContentType "application/json" -Body $body -UseBasicParsing
[Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | Out-File C:\trip_planner\result.json -Encoding utf8
code C:\trip_planner\result.json
```
| 요청 필드 | 내용 |
|---|---|
| recommend/route | countryId, areaIds, styleIds(1~2), tripDays or startDate+endDate |
| route/check | countryId, regionIds(방문 순서, 최대 15), styleIds(선택), tripDays or startDate+endDate |

## 9-2. Flutter 앱 실행

| 순서 | 작업 |
|---|---|
| 1 | 백엔드 실행 (`.\gradlew bootRun`) |
| 2 | Android Studio → Device Manager → 에뮬레이터 ▶ |
| 3 | 새 터미널: `cd C:\trip_planner\app` → `flutter run` |

| 항목 | 내용 |
|---|---|
| 서버 주소 | 에뮬레이터 → 내 PC = `http://10.0.2.2:8080` (`lib/api/api_client.dart`) |
| HTTP 허용 | `android/app/src/main/AndroidManifest.xml` → `<application android:usesCleartextTraffic="true"` |
| 실행 중 | `r` 핫 리로드 / `R` 재시작 / `q` 종료 |

| 빌드 에러 | 해결 |
|---|---|
| `NDK 28.2.13676358 not installed` | Android Studio → SDK Manager → SDK Tools → Show Package Details → NDK (Side by side) → 해당 버전 체크 → Apply |
| 에뮬레이터 첫 실행 구글 로그인 화면 | 전부 Skip |

### 앱 구조 (`app/lib/`)
| 폴더 | 내용 |
|---|---|
| `api/` | 백엔드 호출 (UTF-8 디코딩) |
| `models/` | 응답 모델 + TripDraft (화면 간 입력값) |
| `widgets/` | 별점, 하단 버튼, 에러 화면 등 공통 |
| `screens/` | 홈 → 스타일 → 국가 → 광역 → 기간 → 추천 동선 결과 |

---

## 10. 개발 순서

### 개발 전 준비
| 순서 | 작업 | 상태 |
|---|---|---|
| 1 | region 위도·경도 | ✅ |
| 2 | ERD 확정 | ✅ (위 7번) |
| 3 | 환경 세팅 | ✅ |
| 4 | 화면 흐름도 / 와이어프레임 | ⬜ |
| 5 | 백엔드 API 목록 | ⬜ |
| 6 | Google Cloud 프로젝트 + Routes API 키 + 할당량 제한 | ⬜ |

### 개발
| 단계 | 작업 |
|---|---|
| 1 | DB 테이블 생성 + 데이터 ✅ → `db/schema.sql` → `db/seed.sql` (+ 기존 DB는 `db/alter_v2.sql`) |
| 2 | Spring Boot 기본 구조 + 기준 데이터 조회 API (국가·광역·세부·스타일·추천도·렌트) ✅ |
| 3 | 이동시간 행렬 — 추정값 ✅ (`matrix_v3.sql`) / Google Routes 배치 ⬜ |
| 4 | 추천 동선 로직 + API ✅ (추천 모드 + 직접 선택 모드) / 입·출국 공항 반영 ⬜ |
| 5 | Flutter 화면 — 추천 모드 ✅ (스타일 → 국가 → 광역 → 기간 → 결과) / 직접 선택 모드 ⬜ |
| 6 | 지도 + 오프라인 저장 + 메모 |
| 7 | 비용·날씨·축제·숙소 |
| 8 | 후기·공유·정산 |

→ 1~5단계 = MVP 핵심
