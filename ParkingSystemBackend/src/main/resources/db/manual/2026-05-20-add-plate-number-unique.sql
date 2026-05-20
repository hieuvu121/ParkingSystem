ALTER TABLE users ADD COLUMN IF NOT EXISTS plate_number VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS users_plate_number_key UNIQUE (plate_number);
