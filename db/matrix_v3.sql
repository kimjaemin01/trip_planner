-- =========================================================
-- V3: 지역 간 이동시간 행렬 (추정값)
-- 목적: Google Routes API 키 없이 추천 동선 로직 먼저 개발
-- 방식: PostGIS 직선거리 × 보정계수 ÷ 교통수단별 평균속도
-- 이후: Routes API 배치로 source = 'GOOGLE' 실제값으로 교체
-- 실행: 코끼리 New Query → 전체 붙여넣기 → ▷ (여러 번 실행해도 안전)
-- =========================================================

-- 1. 육로 연결 그룹 (같은 그룹끼리만 열차·자동차 이동 가능)
--    일본: 본토(간토~홋카이도) = 1, 오키나와 = 2
--    새 국가 추가 시 이 값만 INSERT/UPDATE → 코드 수정 X
ALTER TABLE area ADD COLUMN IF NOT EXISTS ground_group INTEGER;
UPDATE area SET ground_group = 1 WHERE country_id = 1 AND area_id IN (1,2,3,4,5);
UPDATE area SET ground_group = 2 WHERE country_id = 1 AND area_id = 6;

-- 2. 이동시간 행렬 테이블
CREATE TABLE IF NOT EXISTS region_travel_matrix (
    from_region_id  INTEGER      NOT NULL REFERENCES region(region_id),
    to_region_id    INTEGER      NOT NULL REFERENCES region(region_id),
    mode            VARCHAR(10)  NOT NULL
        CHECK (mode IN ('TRANSIT','CAR','FLIGHT')),
    duration_min    INTEGER      NOT NULL,
    distance_km     NUMERIC(7,1) NOT NULL,
    avg_cost        INTEGER,                         -- 현지 통화, 비용 단계에서 채움
    source          VARCHAR(10)  NOT NULL DEFAULT 'ESTIMATE'
        CHECK (source IN ('ESTIMATE','GOOGLE','MANUAL')),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (from_region_id, to_region_id, mode)
);

-- geog 비어있는 지역 채우기 (안전장치)
UPDATE region SET geog = ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography WHERE geog IS NULL;

-- 3. 추정값 다시 생성 (실제값 GOOGLE/MANUAL은 유지)
DELETE FROM region_travel_matrix WHERE source = 'ESTIMATE';

WITH pairs AS (
    SELECT a.region_id AS from_id,
           b.region_id AS to_id,
           aa.area_id  AS from_area,
           ab.area_id  AS to_area,
           aa.ground_group = ab.ground_group AS ground_ok,
           ST_Distance(a.geog, b.geog) / 1000.0 AS km   -- 직선거리 km
    FROM region a
    JOIN region b  ON a.region_id <> b.region_id
    JOIN area   aa ON aa.area_id = a.area_id
    JOIN area   ab ON ab.area_id = b.area_id
    WHERE a.is_active AND b.is_active
      AND aa.country_id = ab.country_id
)
INSERT INTO region_travel_matrix (from_region_id, to_region_id, mode, duration_min, distance_km, source)
-- 대중교통: 도로·선로 보정 1.2배 / 100km 이하 45km/h, 300km 이하 100km/h, 그 이상 180km/h(신칸센) + 대기 20분
SELECT from_id, to_id, 'TRANSIT',
       ROUND(km * 1.2 / CASE WHEN km <= 100 THEN 45 WHEN km <= 300 THEN 100 ELSE 180 END * 60 + 20)::INT,
       ROUND((km * 1.2)::NUMERIC, 1),
       'ESTIMATE'
FROM pairs WHERE ground_ok
UNION ALL
-- 자동차: 보정 1.3배 / 100km 이하 60km/h, 그 이상 80km/h(고속도로)
SELECT from_id, to_id, 'CAR',
       ROUND(km * 1.3 / CASE WHEN km <= 100 THEN 60 ELSE 80 END * 60)::INT,
       ROUND((km * 1.3)::NUMERIC, 1),
       'ESTIMATE'
FROM pairs WHERE ground_ok
UNION ALL
-- 항공: 다른 광역 + 300km 초과 (or 육로 불가) / 600km/h + 공항 이동·수속 150분
SELECT from_id, to_id, 'FLIGHT',
       ROUND(km / 600 * 60 + 150)::INT,
       ROUND(km::NUMERIC, 1),
       'ESTIMATE'
FROM pairs WHERE from_area <> to_area AND (km > 300 OR NOT ground_ok)
ON CONFLICT (from_region_id, to_region_id, mode) DO NOTHING;

-- 4. 확인
SELECT mode, COUNT(*) AS cnt FROM region_travel_matrix GROUP BY mode ORDER BY mode;

SELECT f.name AS from_name, t.name AS to_name, m.mode, m.duration_min, m.distance_km
FROM region_travel_matrix m
JOIN region f ON f.region_id = m.from_region_id
JOIN region t ON t.region_id = m.to_region_id
WHERE (f.name, t.name) IN (('도쿄','요코하마'), ('도쿄','하코네'), ('도쿄','오사카'),
                           ('도쿄','삿포로'), ('후쿠오카','나하'), ('오사카','교토'))
ORDER BY f.name, t.name, m.mode;
