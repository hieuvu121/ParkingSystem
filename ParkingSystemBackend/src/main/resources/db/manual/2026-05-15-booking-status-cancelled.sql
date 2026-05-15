-- Update bookings status constraint to allow CANCELLED.
-- This project uses EnumType.ORDINAL, so status is stored as an integer (0..n).
-- Run with: psql -d parkingSystem -f 2026-05-15-booking-status-cancelled.sql

-- If your column is integer (ORDINAL), use this block:
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check;
ALTER TABLE bookings
  ADD CONSTRAINT bookings_status_check
  CHECK (status >= 0 AND status <= 3);

-- If your column is a Postgres enum, comment out the block above and use:
-- ALTER TYPE booking_status ADD VALUE IF NOT EXISTS 'CANCELLED';
-- ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check;
-- ALTER TABLE bookings
--   ADD CONSTRAINT bookings_status_check
--   CHECK (status IN ('PENDING', 'APPROVED', 'EXPIRED', 'CANCELLED'));

