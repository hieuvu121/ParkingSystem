-- ============================================================
-- Seed: Parking Zones & Spots
-- Usage: psql -U <user> -d parkingSystem -f seed.sql
-- Safe to re-run: clears bookings, spots, zones before inserting.
--
-- Zone layout:
--   Level 1 INDOOR   → 4 cols × 5 rows = 20 spots
--   Level 1 OUTDOOR  → 6 cols × 5 rows = 30 spots
--   Level 2 INDOOR   → 4 cols × 5 rows = 20 spots
--   Level 2 OUTDOOR  → 6 cols × 5 rows = 30 spots
--   Total: 100 spots
--
-- Spot status ordinals: 0 = OCCUPIED, 1 = AVAILABLE
-- Spot types cycle: STANDARD → COMPACT → DISABLED → EV
-- ============================================================

-- ── Cleanup (FK order: bookings → spots → zones) ─────────────
DELETE FROM bookings;
DELETE FROM parking_spots;
DELETE FROM parking_zones;

-- ── Zones ────────────────────────────────────────────────────
INSERT INTO parking_zones (level, type, lat, lng) VALUES
    (1, 'INDOOR',  10.77690, 106.70090),   -- Level 1 Indoor  (ground floor, covered)
    (1, 'OUTDOOR', 10.77610, 106.69980),   -- Level 1 Outdoor (ground floor, open air)
    (2, 'INDOOR',  10.77730, 106.70150),   -- Level 2 Indoor  (2nd floor, covered)
    (2, 'OUTDOOR', 10.77800, 106.70250);   -- Level 2 Outdoor (rooftop, open air)

-- ── Spots ────────────────────────────────────────────────────
-- Grid width per zone type:
--   INDOOR  → 4 cols  (4 × 5 = 20 spots)
--   OUTDOOR → 6 cols  (6 × 5 = 30 spots)
-- Status: ~70% AVAILABLE (1), ~30% OCCUPIED (0)
DO $$
DECLARE
    z          RECORD;
    cols       INT;
    r          INT;
    c          INT;
    spot_types TEXT[] := ARRAY['STANDARD', 'COMPACT', 'DISABLED', 'EV'];
BEGIN
    FOR z IN SELECT id, type FROM parking_zones ORDER BY id LOOP
        cols := CASE z.type
            WHEN 'INDOOR'  THEN 4
            WHEN 'OUTDOOR' THEN 6
        END;

        FOR r IN 1..5 LOOP
            FOR c IN 1..cols LOOP
                INSERT INTO parking_spots (row, col, type, status, zone_id)
                VALUES (
                    r,
                    c,
                    spot_types[1 + ((r * cols + c - 1) % 4)],
                    CASE WHEN random() < 0.7 THEN 1 ELSE 0 END,
                    z.id
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- ── Zone verification ─────────────────────────────────────────
SELECT
    z.id                                            AS zone_id,
    z.level,
    z.type                                          AS zone_type,
    COUNT(s.id)                                     AS total_spots,
    SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END)  AS available,
    SUM(CASE WHEN s.status = 0 THEN 1 ELSE 0 END)  AS occupied
FROM parking_zones z
JOIN parking_spots s ON s.zone_id = z.id
GROUP BY z.id, z.level, z.type
ORDER BY z.id;

-- ============================================================
-- Seed: 50 random bookings for user_id = 5
-- Requires user with id=5 to exist in the users table.
-- BookingStatus ordinals: 0=PENDING  1=APPROVED  2=EXPIRED  3=CANCELLED
-- Booking durations: 1–8 hours, spread across the past 6 months.
-- ============================================================

DO $$
DECLARE
    i            INT;
    spot_ids     BIGINT[];
    chosen_spot  BIGINT;
    start_ts     TIMESTAMP;
    end_ts       TIMESTAMP;
    bstatus      INT;
    -- weighted pool: PENDING×1  APPROVED×3  EXPIRED×2
    weights      INT[] := ARRAY[0, 1, 1, 1, 2, 2];
BEGIN
    -- only seed bookings if user 5 exists
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = 5) THEN
        RAISE NOTICE 'user_id=5 not found — skipping bookings seed';
        RETURN;
    END IF;

    SELECT ARRAY(SELECT id FROM parking_spots ORDER BY id) INTO spot_ids;

    FOR i IN 1..50 LOOP
        chosen_spot := spot_ids[1 + (floor(random() * array_length(spot_ids, 1)))::INT];
        start_ts    := date_trunc('minute', NOW() - (random() * INTERVAL '180 days'));
        end_ts      := start_ts + make_interval(hours => 1 + floor(random() * 7)::INT);
        bstatus     := weights[1 + (floor(random() * array_length(weights, 1)))::INT];

        INSERT INTO bookings (start_time, end_time, status, created_by, spot_id)
        VALUES (start_ts, end_ts, bstatus, 5, chosen_spot);
    END LOOP;
END $$;

-- ── Booking verification ──────────────────────────────────────
SELECT
    status,
    CASE status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'APPROVED'
        WHEN 2 THEN 'EXPIRED'
        WHEN 3 THEN 'CANCELLED'
    END     AS status_name,
    COUNT(*) AS count
FROM bookings
WHERE created_by = 5
GROUP BY status
ORDER BY status;
