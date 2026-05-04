# Parking System Backend

A Spring Boot REST API for managing parking zones, spots, bookings, and users with JWT authentication and role-based access control.

## Tech Stack

- Java 17, Spring Boot 4.0
- PostgreSQL
- Spring Security + JWT (HMAC-SHA256, 24h expiry)
- Spring Data JPA / Hibernate
- WebSocket / STOMP (stub — Phase 5)

## Setup

### Prerequisites
- Java 17+
- PostgreSQL running locally

### Configuration

Edit `ParkingSystemBackend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

jwt.secret=your_secret_key
jwt.expiration=86400000
```

### Run

```bash
cd ParkingSystemBackend
./mvnw spring-boot:run
```

Base URL: `http://localhost:8080`

---

## Authentication

All protected endpoints require a Bearer token in the `Authorization` header:

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
| GET | `/api/auth/verify` | None | Verify email via token |

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
| GET | `/api/users` | ADMIN | List all users |
| PUT | `/api/users/{id}/role` | ADMIN | Change a user's role |
| DELETE | `/api/users/{id}` | ADMIN | Delete a user |

#### List All Users

```bash
curl http://localhost:8080/api/users \
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
curl -X PUT http://localhost:8080/api/users/1/role \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'
```

Response `200`:
```json
{ "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "ADMIN" }
```

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

Returns `409` if another PENDING or APPROVED booking already occupies that spot in the requested time window.

#### List Own Bookings

```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Bearer <token>"
```

Response `200`: array of booking objects (expired bookings are auto-transitioned on read).

#### Cancel Booking

```bash
curl -X PATCH http://localhost:8080/api/bookings/1/cancel \
  -H "Authorization: Bearer <token>"
```

Response `204 No Content`. Returns `404` if the booking doesn't belong to the requesting user.

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
  "startDate": "2026-05-04T...",
  "endDate": "2026-06-03T...",
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

#### Delete User

```bash
curl -X DELETE http://localhost:8080/api/users/1 \
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
| PUT | `/api/zones/{zoneId}/spots/{id}` | ADMIN | Update spot details |
| DELETE | `/api/zones/{zoneId}/spots/{id}` | ADMIN | Delete a spot |
| GET | `/api/spots/dashboard` | Any authenticated | Get availability summary |
| POST | `/api/spots/webhook` | ADMIN | Update a spot's status (webhook) |
| POST | `/api/spots/simulate` | ADMIN | Randomly toggle a spot's status |

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

Used by external sensors or IoT devices to report spot occupancy.

```bash
curl -X POST http://localhost:8080/api/spots/webhook \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"spotId":1,"status":"OCCUPIED"}'
```

Response `200`:
```json
{ "id": 1, "row": 1, "col": 1, "type": "STANDARD", "status": "OCCUPIED", "zoneId": 1 }
```

Valid status values: `AVAILABLE`, `OCCUPIED`

#### Simulate

Randomly toggles all spots between AVAILABLE and OCCUPIED for demo/testing.

```bash
curl -X POST http://localhost:8080/api/spots/simulate \
  -H "Authorization: Bearer <admin_token>"
```

Response `200`:
```json
{ "message": "Simulation complete." }
```

---

## How It Works

### Authentication Flow

1. User registers via `POST /api/auth/register` — account is inactive until email is verified.
2. A verification link is sent to the registered email containing a token.
3. User clicks the link (`GET /api/auth/verify?token=...`) to activate the account.
4. User logs in via `POST /api/auth/login` and receives a signed JWT (24h expiry).
5. All subsequent requests include the JWT as `Authorization: Bearer <token>`.

### Role-Based Access

Two roles exist: `USERS` and `ADMIN`.

- `USERS`: can view/update their own profile, browse zones and spots, view the dashboard, create and cancel bookings, subscribe to packages.
- `ADMIN`: all of the above, plus create/update/delete zones, spots, and packages; manage users; trigger webhook/simulate; view all bookings and subscriptions.

Roles are enforced both at route level (Spring Security) and method level (`@PreAuthorize`).

### Parking Zones and Spots

- A **Zone** represents a physical area (e.g., Level 1 Indoor).
- A **Spot** belongs to a zone and has a row/column grid position, a type (e.g., STANDARD, DISABLED), and a status (`AVAILABLE` or `OCCUPIED`).
- Spot status is updated via the webhook endpoint (for real sensors) or the simulate endpoint (for demos).

### Smart Spot Detection

Spot status changes come from two sources:
- **Webhook** (`POST /api/spots/webhook`): called by physical sensors or IoT devices when a car parks or leaves.
- **Simulate** (`POST /api/spots/simulate`): randomly flips all spot statuses for demo/testing purposes.

When a spot's status changes, the service notifies `RealtimeService` (currently a no-op stub — see Phase 5).

### Real-time Dashboard

The dashboard endpoint (`GET /api/spots/dashboard`) provides an instant snapshot of total, available, and occupied counts — both system-wide and broken down by zone. In Phase 5, status changes will also be pushed to connected clients over WebSocket (STOMP).

### Bookings

A user creates a booking for a specific spot with a start and end time. The system checks for time-window conflicts with other PENDING or APPROVED bookings on the same spot. If none exist, the booking is saved as APPROVED and the spot is immediately marked OCCUPIED.

Bookings expire automatically via two mechanisms:
- A scheduled job runs every 5 minutes and expires all APPROVED bookings whose end time has passed.
- Any read operation (`GET /api/bookings/my`, `GET /api/bookings/{id}`) also checks and expires on the fly.

Cancelling a booking sets its status to CANCELLED and frees the spot back to AVAILABLE. Only the booking owner can cancel their own booking.

### Packages and Subscriptions

An admin creates packages with a name, description, duration (in days), and price. Users subscribe to a package, which creates a subscription valid from the current date for the package's duration. Only one active subscription per user is allowed at a time.

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
| 409 | Conflict (e.g., duplicate resource) |
| 500 | Unexpected server error |

---

## Coming Soon

| Phase | Feature |
|-------|---------|
| Phase 4 | Real-time WebSocket push (STOMP) for spot and dashboard updates |
| Phase 5 | Predictive Availability & Smart Recommendations (rule-based ML) |
| Phase 5 | Admin Analytics (peak hours, occupancy trends) |
