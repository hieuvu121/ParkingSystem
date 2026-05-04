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
| GET | `/api/users/me` | USER / ADMIN | Get own profile |
| PUT | `/api/users/me` | USER / ADMIN | Update own full name |

#### Get Profile

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <token>"
```

Response `200`:
```json
{ "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "USER" }
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
{ "id": 1, "fullName": "John Updated", "email": "john@example.com", "role": "USER" }
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
  { "id": 1, "fullName": "John Doe", "email": "john@example.com", "role": "USER" },
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

Two roles exist: `USER` and `ADMIN`.

- `USER`: can view their own profile, browse zones and spots, view the dashboard.
- `ADMIN`: all of the above, plus create/update/delete zones and spots, manage users, trigger webhook/simulate.

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
| Phase 3 | Booking Management (create, approve, cancel, expire) |
| Phase 4 | Subscription Packages |
| Phase 5 | Real-time WebSocket push (STOMP) for spot and dashboard updates |
| Phase 6 | Predictive Availability & Smart Recommendations (rule-based ML) |
| Phase 6 | Admin Analytics (peak hours, occupancy trends) |
