-- =========================================================
-- V2: 데이터 주도 설계 컬럼 추가
-- 대상: 이미 schema.sql + seed.sql 실행한 DB (학교 PC, 집 노트북)
-- 실행: 코끼리 New Query → 전체 붙여넣기 → ▷
-- 여러 번 실행해도 안전 (IF NOT EXISTS)
-- =========================================================

-- 1. country: 노출 여부, 정렬, 이미지, 외부 API 제공자
ALTER TABLE country ADD COLUMN IF NOT EXISTS is_active        BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE country ADD COLUMN IF NOT EXISTS sort_order       INTEGER;
ALTER TABLE country ADD COLUMN IF NOT EXISTS image_url        VARCHAR(500);
ALTER TABLE country ADD COLUMN IF NOT EXISTS routing_provider VARCHAR(20)  NOT NULL DEFAULT 'GOOGLE';
ALTER TABLE country ADD COLUMN IF NOT EXISTS place_provider   VARCHAR(20)  NOT NULL DEFAULT 'GOOGLE';

-- 2. area: 노출 여부, 이미지 (sort_order는 이미 있음)
ALTER TABLE area ADD COLUMN IF NOT EXISTS is_active  BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE area ADD COLUMN IF NOT EXISTS image_url  VARCHAR(500);

-- 3. region: 노출 여부, 정렬, 이미지
ALTER TABLE region ADD COLUMN IF NOT EXISTS is_active  BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE region ADD COLUMN IF NOT EXISTS sort_order INTEGER;
ALTER TABLE region ADD COLUMN IF NOT EXISTS image_url  VARCHAR(500);

-- 4. 코드값 제약
ALTER TABLE country DROP CONSTRAINT IF EXISTS chk_country_routing_provider;
ALTER TABLE country ADD  CONSTRAINT chk_country_routing_provider
    CHECK (routing_provider IN ('GOOGLE','KAKAO','AMAP'));
ALTER TABLE country DROP CONSTRAINT IF EXISTS chk_country_place_provider;
ALTER TABLE country ADD  CONSTRAINT chk_country_place_provider
    CHECK (place_provider IN ('GOOGLE','KAKAO','AMAP'));

-- 5. 일본 데이터 값 채우기 (공개 상태로)
UPDATE country SET is_active = TRUE, sort_order = 1 WHERE country_id = 1;
UPDATE area    SET is_active = TRUE WHERE country_id = 1;
UPDATE region  SET is_active = TRUE, sort_order = region_id
 WHERE area_id IN (SELECT area_id FROM area WHERE country_id = 1);

-- 확인
SELECT country_id, name, is_active, sort_order, routing_provider, place_provider FROM country;
