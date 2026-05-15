-- Seed historical bookings so prediction chart is not 100% across all hours.
-- This uses existing users/spots and inserts expired bookings for the last 60 days.

WITH base_user AS (
  SELECT id FROM users ORDER BY id LIMIT 1
),
spots AS (
  SELECT id FROM parking_spots
),
valid AS (
  SELECT (SELECT COUNT(*) FROM base_user) AS has_user,
         (SELECT COUNT(*) FROM spots) AS has_spots
),
days AS (
  SELECT generate_series(current_date - interval '60 days', current_date - interval '1 day', interval '1 day')::date AS day
),
hours AS (
  SELECT generate_series(7, 21) AS hour
),
samples AS (
  SELECT d.day, h.hour
  FROM days d
  CROSS JOIN hours h
),
bookings_gen AS (
  SELECT
    (s.day + make_interval(hours => s.hour) + make_interval(mins => (random() * 40)::int)) AS start_time,
    (30 + (random() * 90)::int) AS duration_mins
  FROM samples s
  CROSS JOIN LATERAL generate_series(1, (random() * 3)::int + 1) gs
)
INSERT INTO bookings (start_time, end_time, status, payment_type, cost_cents, created_by, spot_id)
SELECT
  bg.start_time,
  bg.start_time + make_interval(mins => bg.duration_mins) AS end_time,
  2 AS status,
  CASE WHEN random() < 0.2 THEN 'SUBSCRIPTION' ELSE 'PAY_PER_USE' END AS payment_type,
  CASE WHEN random() < 0.2 THEN 0 ELSE (50 + (random() * 250)::int) END AS cost_cents,
  (SELECT id FROM base_user) AS created_by,
  (SELECT id FROM parking_spots ORDER BY random() LIMIT 1) AS spot_id
FROM bookings_gen bg
WHERE (SELECT has_user FROM valid) > 0
  AND (SELECT has_spots FROM valid) > 0;

