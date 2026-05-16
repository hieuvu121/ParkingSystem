-- Seed historical bookings so prediction chart is not 100% across all hours.
-- Focuses on INDOOR level 1 zone (fallbacks to any zone if missing).

WITH base_user AS (
  SELECT id FROM users ORDER BY id LIMIT 1
),
base_zone AS (
  SELECT id FROM parking_zones WHERE type = 'INDOOR' AND level = 1 ORDER BY id LIMIT 1
),
fallback_zone AS (
  SELECT id FROM parking_zones ORDER BY id LIMIT 1
),
target_zone AS (
  SELECT id FROM base_zone
  UNION ALL
  SELECT id FROM fallback_zone WHERE NOT EXISTS (SELECT 1 FROM base_zone)
  LIMIT 1
),
spots AS (
  SELECT id FROM parking_spots WHERE zone_id = (SELECT id FROM target_zone)
),
valid AS (
  SELECT (SELECT COUNT(*) FROM base_user) AS has_user,
         (SELECT COUNT(*) FROM spots) AS has_spots
),
days AS (
  SELECT generate_series(current_date - interval '60 days', current_date - interval '1 day', interval '1 day')::date AS day
),
hours AS (
  SELECT generate_series(6, 22) AS hour
),
samples AS (
  SELECT d.day, h.hour
  FROM days d
  CROSS JOIN hours h
),
weighted_samples AS (
  SELECT s.day, s.hour,
         CASE
           WHEN s.hour BETWEEN 7 AND 9 THEN 5
           WHEN s.hour BETWEEN 12 AND 14 THEN 4
           WHEN s.hour BETWEEN 17 AND 19 THEN 6
           WHEN s.hour BETWEEN 20 AND 22 THEN 3
           ELSE 1
         END AS weight
  FROM samples s
),
bookings_gen AS (
  SELECT
    (ws.day + make_interval(hours => ws.hour) + make_interval(mins => (random() * 40)::int)) AS start_time,
    (30 + (random() * 90)::int) AS duration_mins
  FROM weighted_samples ws
  CROSS JOIN LATERAL generate_series(1, (random() * ws.weight)::int + 1) gs
)
INSERT INTO bookings (start_time, end_time, status, payment_type, cost_cents, created_by, spot_id)
SELECT
  bg.start_time,
  bg.start_time + make_interval(mins => bg.duration_mins) AS end_time,
  2 AS status,
  CASE WHEN random() < 0.2 THEN 'SUBSCRIPTION' ELSE 'PAY_PER_USE' END AS payment_type,
  CASE WHEN random() < 0.2 THEN 0 ELSE (50 + (random() * 250)::int) END AS cost_cents,
  (SELECT id FROM base_user) AS created_by,
  (SELECT id FROM spots ORDER BY random() LIMIT 1) AS spot_id
FROM bookings_gen bg
WHERE (SELECT has_user FROM valid) > 0
  AND (SELECT has_spots FROM valid) > 0;
