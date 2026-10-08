-- =========================================================
-- 여행 플래너 앱 - 기준 데이터 테이블 (일본 MVP)
-- 실행: DBeaver → travel DB → SQL 편집기 → 전체 실행 (Alt+X)
-- =========================================================

-- 0. PostGIS 확장 (DB당 1회)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. 국가
CREATE TABLE country (
    country_id   INTEGER PRIMARY KEY,
    name         VARCHAR(50)  NOT NULL,
    currency     VARCHAR(10)  NOT NULL,
    voltage      VARCHAR(20),
    plug_type    VARCHAR(20)
);

-- 2. 국가 공통 언어
CREATE TABLE country_language (
    country_id   INTEGER      NOT NULL REFERENCES country(country_id),
    language     VARCHAR(50)  NOT NULL,
    is_common    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (country_id, language)
);

-- 3. 광역 지역
CREATE TABLE area (
    area_id       INTEGER PRIMARY KEY,
    country_id    INTEGER      NOT NULL REFERENCES country(country_id),
    name          VARCHAR(50)  NOT NULL,
    main_airport  VARCHAR(50),
    avg_stay_min  NUMERIC(3,1),
    avg_stay_max  NUMERIC(3,1),
    sort_order    INTEGER
);

-- 4. 세부 도시/관광지
CREATE TABLE region (
    region_id       INTEGER PRIMARY KEY,
    area_id         INTEGER       NOT NULL REFERENCES area(area_id),
    name            VARCHAR(50)   NOT NULL,
    lat             NUMERIC(9,6)  NOT NULL,
    lng             NUMERIC(9,6)  NOT NULL,
    stay_min        NUMERIC(3,1)  NOT NULL,
    stay_rec        NUMERIC(3,1)  NOT NULL,
    stay_max        NUMERIC(3,1)  NOT NULL,
    stay_type       VARCHAR(20)   NOT NULL
        CHECK (stay_type IN ('BASE','BASE_OPTIONAL','DAYTRIP','OVERNIGHT','BUNDLE','DRIVE')),
    base_region_id  INTEGER REFERENCES region(region_id),
    group_id        INTEGER,
    description     VARCHAR(200),
    geog            GEOGRAPHY(POINT, 4326)
);

-- 5. 지역 특정 언어 (일본은 비어있음)
CREATE TABLE region_language (
    region_id    INTEGER      NOT NULL REFERENCES region(region_id),
    language     VARCHAR(50)  NOT NULL,
    PRIMARY KEY (region_id, language)
);

-- 6. 여행 스타일
CREATE TABLE travel_style (
    style_id           INTEGER PRIMARY KEY,
    name               VARCHAR(50)  NOT NULL,
    daily_visit_count  INTEGER      NOT NULL,
    stay_tendency      VARCHAR(10)  NOT NULL
        CHECK (stay_tendency IN ('MIN','REC','MAX'))
);

-- 7. 세부 지역 × 스타일 점수
CREATE TABLE region_style_score (
    region_id  INTEGER NOT NULL REFERENCES region(region_id),
    style_id   INTEGER NOT NULL REFERENCES travel_style(style_id),
    score      INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    PRIMARY KEY (region_id, style_id)
);

-- 8. 광역 × 월 렌트 판정
CREATE TABLE rent_rule (
    area_id    INTEGER      NOT NULL REFERENCES area(area_id),
    month      INTEGER      NOT NULL CHECK (month BETWEEN 1 AND 12),
    judgment   VARCHAR(20)  NOT NULL
        CHECK (judgment IN ('RECOMMEND','OPTIONAL','NOT_RECOMMEND')),
    reason     VARCHAR(200),
    PRIMARY KEY (area_id, month)
);

-- 9. 세부 지역 렌트 예외
CREATE TABLE region_rent_exception (
    region_id  INTEGER PRIMARY KEY REFERENCES region(region_id),
    judgment   VARCHAR(20)  NOT NULL
        CHECK (judgment IN ('RECOMMEND','OPTIONAL','NOT_RECOMMEND')),
    reason     VARCHAR(200)
);


-- =========================================================
-- [CSV import 끝난 뒤 실행] region 좌표 → geog 컬럼 채우기
-- =========================================================
-- UPDATE region SET geog = ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography;


-- =========================================================
-- [확인용] 테이블별 행 수
-- =========================================================
-- SELECT 'country' t, COUNT(*) FROM country
-- UNION ALL SELECT 'country_language', COUNT(*) FROM country_language
-- UNION ALL SELECT 'area', COUNT(*) FROM area
-- UNION ALL SELECT 'region', COUNT(*) FROM region
-- UNION ALL SELECT 'travel_style', COUNT(*) FROM travel_style
-- UNION ALL SELECT 'region_style_score', COUNT(*) FROM region_style_score
-- UNION ALL SELECT 'rent_rule', COUNT(*) FROM rent_rule
-- UNION ALL SELECT 'region_rent_exception', COUNT(*) FROM region_rent_exception;
