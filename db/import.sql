-- =========================================================
-- CSV import (schema.sql 실행 후)
-- 사전 작업 (터미널): docker cp C:\trip_planner\data travel-db:/tmp/data
-- 실행: VS Code PostgreSQL 확장 → New Query → 전체 붙여넣기 → Run
-- =========================================================

COPY country               FROM '/tmp/data/country.csv'               WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
COPY country_language      FROM '/tmp/data/country_language.csv'      WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
COPY area                  FROM '/tmp/data/area.csv'                  WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

COPY region (region_id, area_id, name, lat, lng, stay_min, stay_rec, stay_max,
             stay_type, base_region_id, group_id, description)
                           FROM '/tmp/data/region.csv'                WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

COPY travel_style          FROM '/tmp/data/travel_style.csv'          WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
COPY region_style_score    FROM '/tmp/data/region_style_score.csv'    WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
COPY rent_rule             FROM '/tmp/data/rent_rule.csv'             WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
COPY region_rent_exception FROM '/tmp/data/region_rent_exception.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

-- region 좌표 → geog
UPDATE region SET geog = ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography;

-- 행 수 확인
SELECT 'country' AS table_name, COUNT(*) FROM country
UNION ALL SELECT 'country_language',      COUNT(*) FROM country_language
UNION ALL SELECT 'area',                  COUNT(*) FROM area
UNION ALL SELECT 'region',                COUNT(*) FROM region
UNION ALL SELECT 'travel_style',          COUNT(*) FROM travel_style
UNION ALL SELECT 'region_style_score',    COUNT(*) FROM region_style_score
UNION ALL SELECT 'rent_rule',             COUNT(*) FROM rent_rule
UNION ALL SELECT 'region_rent_exception', COUNT(*) FROM region_rent_exception;
