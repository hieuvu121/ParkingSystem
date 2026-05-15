# Seed Predictions Data

Seeds historical bookings for the prediction chart so the availability chart is not 100% for all hours.

## Prerequisites

- PostgreSQL running
- `psql` installed
- Environment variables set:
  - `PGDB_USERNAME`
  - `PGDB_PASSWORD`

## Run

```zsh
cd /Users/mac/Documents/Project/ParkingSystemApp/ParkingSystemBackend
./scripts/seed_predictions.sh
```

To target a different database name:

```zsh
./scripts/seed_predictions.sh parkingSystem
```

## Notes

- The script inserts expired bookings for the last 60 days.
- It uses any existing user and random spots.
- If there are no users or spots, no rows are inserted.

