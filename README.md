# Parking System Backend

A Spring Boot REST API for managing parking zones, spots, bookings, subscriptions, and users with JWT authentication, real-time WebSocket updates, and rule-based availability predictions.

## Tech Stack

- Java 17, Spring Boot 4.0
- PostgreSQL
- Spring Security + JWT (HMAC-SHA256, 24h expiry)
- Spring Data JPA / Hibernate
- Spring WebSocket — STOMP / SimpleBroker
- Spring Mail — SMTP email verification

---

## Setup

### Prerequisites
- Java 17+
- PostgreSQL running locally
- SMTP credentials for email verification

### Environment Variables

The application reads sensitive values from environment variables. Set these before running:

| Variable | Description |
|----------|-------------|
| `PGDB_USERNAME` | PostgreSQL username |
| `PGDB_PASSWORD` | PostgreSQL password |
| `MAIL_HOST` | SMTP host (e.g. `smtp.gmail.com`) |
| `MAIL_USERNAME` | SMTP username / sender address |
| `MAIL_PASSWORD` | SMTP password or app password |

### Configuration

`ParkingSystemBackend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/parkingSystem
spring.datasource.username=${PGDB_USERNAME}
spring.datasource.password=${PGDB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

spring.mail.host=${MAIL_HOST}
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

app.jwt.secret=<base64-encoded-secret>
app.jwt.expiration=86400000
app.base-url=http://localhost:8080
```

### Run

```bash
cd ParkingSystemBackend
./mvnw spring-boot:run
```

Base URL: `http://localhost:8080`

### Run Tests

```bash
cd ParkingSystemBackend
./mvnw test
```

---

## Authentication

All endpoints except `/api/auth/**` require a Bearer token:

```
Authorization: Bearer <your_jwt_token>
```

Obtain a token from `POST /api/auth/login`.

---

## Endpoints

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | None | Register a new user |
| POST | `/api/auth/login` | None | Login and receive JWT |
| GET | `/api/auth/verify?token=` | None | Verify email address |

#### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"secret123"}'
```

Response `201`:
```json
{ "message": "Registration successful. Check your email to verify your account." }
```

#### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}'
```

Response `200`:
```json
{ "token": "eyJhbGci..." }
```

#### Verify Email

```bash
curl "http://localhost:8080/api/auth/verify?token=<verification_token>"
```

Response `200`:
```json
{ "message": "Email verified successfully." }
```

---

### Users — Own Profile

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users/me` | USERS / ADMIN | Get own profile |
| PUT | `/api/users/me` | USERS / ADMIN | Update own full name |

#### Get Profile

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
{ "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "USERS" }
```

#### Update Profile

```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Updated"}'
```

Response `200`:
```json
{ "id": 1, "fullName": "John Updated", "email": "john@example.com", "role": "USERS" }
```

---

### Users — Admin

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/admin/users` | ADMIN | List all users |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Change a user's role |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete a user |

#### List All Users

```bash
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`:
```json
[
  { "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "USERS" },
  { "id": 2, "fullName": "Admin User", "email": "admin@example.com", "role": "ADMIN" }
]
```

#### Change Role

```bash
curl -X PATCH http://localhost:8080/api/admin/users/1/role \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'
```

Response `200`:
```json
{ "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "ADMIN" }
```

#### Delete User

```bash
curl -X DELETE http://localhost:8080/api/admin/users/1 \
  -H "Authorization: Bearer <admin_token>"
```

Response `204 No Content`

---

### Parking Zones

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/zones` | Any authenticated | List all zones |
| GET | `/api/zones/{id}` | Any authenticated | Get zone by ID |
| POST | `/api/zones` | ADMIN | Create a zone |
| PUT | `/api/zones/{id}` | ADMIN | Update a zone |
| DELETE | `/api/zones/{id}` | ADMIN | Delete a zone |

#### List Zones

```bash
curl http://localhost:8080/api/zones \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
[
  { "id": 1, "level": 1, "type": "INDOOR" },
  { "id": 2, "level": 2, "type": "OUTDOOR" }
]
```

#### Create Zone

```bash
curl -X POST http://localhost:8080/api/zones \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"level":1,"type":"INDOOR"}'
```

Response `201`:
```json
{ "id": 1, "level": 1, "type": "INDOOR" }
```

#### Update Zone

```bash
curl -X PUT http://localhost:8080/api/zones/1 \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"level":1,"type":"OUTDOOR"}'
```

Response `200`:
```json
{ "id": 1, "level": 1, "type": "OUTDOOR" }
```

#### Delete Zone

```bash
curl -X DELETE http://localhost:8080/api/zones/1 \
  -H "Authorization: Bearer <admin_token>"
```

Response `204 No Content`

---

### Parking Spots

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/zones/{zoneId}/spots` | Any authenticated | List spots in a zone |
| POST | `/api/zones/{zoneId}/spots` | ADMIN | Add a spot to a zone |
| PUT | `/api/spots/{id}` | ADMIN | Update a spot |
| DELETE | `/api/spots/{id}` | ADMIN | Delete a spot |
| GET | `/api/spots/dashboard` | Any authenticated | Get availability summary |
| POST | `/api/spots/webhook` | ADMIN | Update a spot's status (IoT/sensor) |
| POST | `/api/spots/simulate` | ADMIN | Randomly toggle spot statuses |

#### List Spots in Zone

```bash
curl http://localhost:8080/api/zones/1/spots \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
[
  { "id": 1, "row": 1, "col": 1, "type": "STANDARD", "status": "AVAILABLE", "zoneId": 1 },
  { "id": 2, "row": 1, "col": 2, "type": "STANDARD", "status": "OCCUPIED", "zoneId": 1 }
]
```

#### Add Spot

```bash
curl -X POST http://localhost:8080/api/zones/1/spots \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"row":1,"col":1,"type":"STANDARD"}'
```

Response `201`:
```json
{ "id": 1, "row": 1, "col": 1, "type": "STANDARD", "status": "AVAILABLE", "zoneId": 1 }
```

#### Dashboard

```bash
curl http://localhost:8080/api/spots/dashboard \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
{
  "total": 10,
  "available": 7,
  "occupied": 3,
  "byZone": [
    { "zoneId": 1, "total": 6, "available": 4, "occupied": 2 },
    { "zoneId": 2, "total": 4, "available": 3, "occupied": 1 }
  ]
}
```

#### Webhook — Update Spot Status

Used by physical sensors or IoT devices to report spot occupancy.

```bash
curl -X POST http://localhost:8080/api/spots/webhook \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"spotId":1,"status":"OCCUPIED"}'
```

Response `200`. Valid status values: `AVAILABLE`, `OCCUPIED`

Every status change also pushes a live update to all connected WebSocket clients.

#### Simulate

Randomly flips all spot statuses between AVAILABLE and OCCUPIED for demo or testing.

```bash
curl -X POST http://localhost:8080/api/spots/simulate \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`

---

### Bookings

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/bookings` | USERS | Create a booking |
| GET | `/api/bookings/my` | USERS | List own bookings |
| GET | `/api/bookings/{id}` | Any authenticated | Get booking by ID |
| PATCH | `/api/bookings/{id}/cancel` | USERS | Cancel a booking |
| GET | `/api/admin/bookings` | ADMIN | List all bookings |

#### Create Booking

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"spotId":1,"startTime":"2026-05-10T09:00:00","endTime":"2026-05-10T11:00:00"}'
```

Response `201`:
```json
{
  "id": 1,
  "spotId": 1,
  "userId": 1,
  "startTime": "2026-05-10T09:00:00",
  "endTime": "2026-05-10T11:00:00",
  "status": "APPROVED"
}
```

Returns `409` if another booking already occupies the spot in the requested time window.

#### List Own Bookings

```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Bearer <token>"
```

Response `200`: array of booking objects. Expired bookings are transitioned automatically on read.

#### Cancel Booking

```bash
curl -X PATCH http://localhost:8080/api/bookings/1/cancel \
  -H "Authorization: Bearer <token>"
```

Response `204 No Content`. Returns `404` if the booking doesn't belong to the requesting user.

#### Admin — List All Bookings

```bash
curl http://localhost:8080/api/admin/bookings \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`: array of all booking objects across all users.

---

### Packages

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/packages` | Any authenticated | List all packages |
| POST | `/api/packages` | ADMIN | Create a package |
| PUT | `/api/packages/{id}` | ADMIN | Update a package |
| DELETE | `/api/packages/{id}` | ADMIN | Delete a package |

#### List Packages

```bash
curl http://localhost:8080/api/packages \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
[
  { "id": 1, "name": "Monthly", "description": "30-day unlimited access", "durations": 30, "price": 500000 }
]
```

#### Create Package

```bash
curl -X POST http://localhost:8080/api/packages \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Monthly","description":"30-day unlimited access","durations":30,"price":500000}'
```

Response `201`:
```json
{ "id": 1, "name": "Monthly", "description": "30-day unlimited access", "durations": 30, "price": 500000 }
```

#### Update Package

```bash
curl -X PUT http://localhost:8080/api/packages/1 \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Monthly Plus","description":"30-day premium access","durations":30,"price":750000}'
```

Response `200`: updated package object.

#### Delete Package

```bash
curl -X DELETE http://localhost:8080/api/packages/1 \
  -H "Authorization: Bearer <admin_token>"
```

Response `204 No Content`

---

### Subscriptions

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/subscriptions` | USERS | Subscribe to a package |
| GET | `/api/subscriptions/my` | USERS | Get own active subscription |
| GET | `/api/admin/subscriptions` | ADMIN | List all subscriptions |

#### Subscribe

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"packageId":1}'
```

Response `201`:
```json
{
  "id": 1,
  "userId": 1,
  "packageId": 1,
  "packageName": "Monthly",
  "startDate": "2026-05-06",
  "endDate": "2026-06-05",
  "price": 500000
}
```

Returns `409` if the user already has an active subscription.

#### Get Active Subscription

```bash
curl http://localhost:8080/api/subscriptions/my \
  -H "Authorization: Bearer <token>"
```

Response `200`: subscription object. Returns `404` if no active subscription exists.

#### Admin — List All Subscriptions

```bash
curl http://localhost:8080/api/admin/subscriptions \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`: array of all subscription objects across all users.

---

### Analytics (ADMIN only)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/analytics/occupancy` | ADMIN | Booking count per zone as % of capacity in a time range |
| GET | `/api/analytics/peak-hours` | ADMIN | Busiest hours of the day |
| GET | `/api/analytics/utilization` | ADMIN | Lifetime bookings vs. total spots per zone |

#### Occupancy by Zone

Returns how occupied each zone was (as a percentage of its spot capacity) within a given time range.

```bash
curl "http://localhost:8080/api/analytics/occupancy?from=2026-05-01T00:00:00&to=2026-05-06T23:59:59" \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`:
```json
[
  { "zoneId": 1, "from": "2026-05-01T00:00:00", "to": "2026-05-06T23:59:59", "occupancyPercent": 66.7 },
  { "zoneId": 2, "from": "2026-05-01T00:00:00", "to": "2026-05-06T23:59:59", "occupancyPercent": 25.0 }
]
```

#### Peak Hours

Returns all hours of the day with their relative booking activity (0–100, normalised to the busiest hour).

```bash
curl http://localhost:8080/api/analytics/peak-hours \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`:
```json
[
  { "hour": 8,  "averageOccupancyPercent": 75.0 },
  { "hour": 9,  "averageOccupancyPercent": 100.0 },
  { "hour": 17, "averageOccupancyPercent": 90.0 }
]
```

#### Utilization by Zone

Returns total bookings vs. total spots per zone as a lifetime utilization percentage.

```bash
curl http://localhost:8080/api/analytics/utilization \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`:
```json
[
  { "zoneId": 1, "totalSpots": 10, "totalBookings": 42, "utilizationPercent": 100.0 },
  { "zoneId": 2, "totalSpots": 8,  "totalBookings": 15, "utilizationPercent": 100.0 }
]
```

---

### Predictions

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/predict/availability` | Any authenticated | Predict availability probability for a zone at a future time |

Uses historical booking data (same hour + day of week) to estimate how likely a zone is to have free spots at the requested time.

```bash
curl "http://localhost:8080/api/predict/availability?zoneId=1&targetTime=2026-05-07T09:00:00" \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
{ "zoneId": 1, "targetTime": "2026-05-07T09:00:00", "availabilityProbability": 0.7 }
```

`availabilityProbability` is a value between `0.0` (fully booked historically) and `1.0` (always free historically).

---

### Recommendations

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/recommend/spot` | Any authenticated | Get the best zone to park in right now |

Returns the single best zone using a three-tier strategy:
1. **Nearest available zone** — if `lat`/`lng` are provided and zones have coordinates set
2. **Least congested zone** — most available spots right now
3. **Best predicted availability** — highest predicted probability 30 minutes from now

#### Without GPS

```bash
curl http://localhost:8080/api/recommend/spot \
  -H "Authorization: Bearer <token>"
```

#### With GPS coordinates

```bash
curl "http://localhost:8080/api/recommend/spot?lat=10.762&lng=106.660" \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
{
  "zoneId": 2,
  "reason": "Nearest available zone",
  "availableSpots": 4,
  "predictedProbability": 0.8
}
```

Returns `404` if no zones have any available spots.

To enable proximity-based recommendations, set `lat` and `lng` on zones via `PUT /api/zones/{id}`.

---

### WebSocket — Real-time Updates

The server pushes live updates to connected clients over STOMP/WebSocket whenever any spot status changes (webhook, simulate, booking create/cancel/expire).

#### Connect

Connect via SockJS at `ws://localhost:8080/ws`, then subscribe to topics using a STOMP client.

**JavaScript example (using `@stomp/stompjs`):**

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  onConnect: () => {
    // Live spot status changes
    client.subscribe('/topic/spots', (message) => {
      const spot = JSON.parse(message.body);
      console.log('Spot update:', spot);
    });

    // Live dashboard counts
    client.subscribe('/topic/dashboard', (message) => {
      const dashboard = JSON.parse(message.body);
      console.log('Dashboard update:', dashboard);
    });
  },
});

client.activate();
```

#### Topic: `/topic/spots`

Published whenever a single spot's status changes.

```json
{
  "spotId": 3,
  "row": 1,
  "col": 3,
  "zoneId": 1,
  "status": "OCCUPIED"
}
```

#### Topic: `/topic/dashboard`

Published after every spot status change with the updated system-wide counts.

```json
{
  "total": 10,
  "available": 6,
  "occupied": 4,
  "byZone": [
    { "zoneId": 1, "total": 6, "available": 3, "occupied": 3 },
    { "zoneId": 2, "total": 4, "available": 3, "occupied": 1 }
  ]
}
```

---

## How It Works

### Authentication Flow

1. User registers via `POST /api/auth/register` — account is inactive until email is verified.
2. A verification link is sent to the registered email.
3. User visits the link (`GET /api/auth/verify?token=...`) to activate the account.
4. User logs in via `POST /api/auth/login` and receives a signed JWT (24h expiry).
5. All subsequent requests include the JWT as `Authorization: Bearer <token>`.

### Role-Based Access

Two roles exist: `USERS` and `ADMIN`.

- `USERS`: view/update own profile, browse zones and spots, view dashboard, create/cancel bookings, subscribe to packages, view predictions and recommendations.
- `ADMIN`: all of the above, plus create/update/delete zones, spots, and packages; manage all users; view all bookings and subscriptions; access analytics; trigger webhook/simulate.

### Smart Spot Detection

Spot status changes come from two sources:
- **Webhook** (`POST /api/spots/webhook`): called by physical sensors or IoT devices when a car parks or leaves.
- **Simulate** (`POST /api/spots/simulate`): randomly flips all spot statuses for demo/testing.

Every status change triggers a live broadcast to all connected WebSocket clients.

### Booking Lifecycle

```
create ──→ APPROVED ──→ EXPIRED   (past endTime — scheduler every 5 min, or on next read)
                   └──→ CANCELLED (user cancels — spot freed immediately)
```

Bookings are immediately APPROVED or rejected with `409`. There is no manual approval step.

### Packages and Subscriptions

An admin creates packages with a name, description, duration (in days), and price. Users subscribe to a package — the subscription's end date is calculated as `today + duration days`. Only one active subscription per user is allowed at a time.

### Predictive Availability

`PredictionService` counts how many bookings historically started in the same zone at the same hour and day of the week, then estimates availability probability as:

```
probability = (totalSpots - historicalBookings) / totalSpots
```

Clamped to [0.0, 1.0]. The more bookings historically at that time, the lower the probability.

### Smart Recommendations

`RecommendationService` picks the best zone using a three-tier fallback:
1. **Proximity** — if GPS coordinates are given and zones have `lat`/`lng` set, picks the nearest zone with available spots (haversine distance).
2. **Congestion** — picks the zone with the most available spots right now.
3. **Prediction** — picks the zone with the highest predicted availability 30 minutes from now.

---

## Error Responses

All errors return a consistent JSON body:

```json
{ "error": "NOT_FOUND", "message": "Parking zone not found with id: 99" }
```

| Status | Meaning |
|--------|---------|
| 400 | Validation error (missing or invalid field) |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but insufficient role |
| 404 | Resource not found |
| 409 | Conflict (e.g. overlapping booking, duplicate active subscription) |
| 500 | Unexpected server error |
