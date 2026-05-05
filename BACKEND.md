# Backend Documentation

## Overview

The backend is a stateless REST API built with Spring Boot 4.0. It handles user authentication, parking zone and spot management, real-time spot status broadcasts over WebSocket, booking lifecycle, package subscriptions, and admin analytics and predictions.

All state is stored in PostgreSQL. All endpoints except auth and email verification require a JWT Bearer token. Role-based access is enforced at both the route level (Spring Security) and the method level (`@PreAuthorize`).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0 |
| Security | Spring Security + JWT (HMAC-SHA256) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Real-time | Spring WebSocket (STOMP / SimpleBroker) |
| Email | Spring Mail (SMTP) |
| Build | Maven |
| Utilities | Lombok |
| Testing | JUnit 5, Mockito, AssertJ |

---

## Architecture

The application follows a standard layered architecture with strict one-way dependency flow:

```
Controller → Service → Repository
                ↓
          RealtimeService → WebSocket broker
```

- **Controllers** handle HTTP: validate input with Bean Validation, call exactly one service method, return `ResponseEntity`.
- **Services** own all business logic. Cross-service calls are allowed (e.g. `BookingService` calls `ParkingSpotService.updateStatus()`). Services never call other services' repositories directly.
- **Repositories** are Spring Data JPA interfaces. Complex queries use `@Query` with JPQL or native SQL where JPQL is insufficient (e.g. PostgreSQL `EXTRACT` functions).
- **RealtimeService** is called by `ParkingSpotService` after every spot status change and broadcasts to all connected WebSocket clients.

---

## Security

### JWT Authentication

1. On login, `AuthService` validates credentials and calls `JwtService.generateToken(user)`.
2. `JwtService` signs a token with HMAC-SHA256 using the secret from `app.jwt.secret`. Expiry: 24 hours (`app.jwt.expiration=86400000`).
3. On every request, `JwtAuthFilter` extracts the `Authorization: Bearer <token>` header, validates the token, loads the user via `UserDetailsServiceImpl`, and sets the `SecurityContext`.
4. If the token is missing, expired, or invalid, the request proceeds unauthenticated and is rejected by Spring Security's access rules.

### Roles

| Role | Access |
|------|--------|
| `USERS` | Own profile, browse zones/spots/dashboard, create/cancel bookings, subscribe to packages |
| `ADMIN` | Everything above + create/update/delete zones, spots, packages; manage all users; view all bookings/subscriptions; analytics; trigger webhook/simulate |

Roles are stored as strings in the database (`@Enumerated(EnumType.STRING)`) and exposed as Spring Security authorities in the form `ROLE_USERS` / `ROLE_ADMIN`.

### Email Verification

Accounts are inactive (`isActive = false`) on registration. A verification token is stored on the user and emailed via `EmailService`. Calling `GET /api/auth/verify?token=...` sets `isActive = true`. Inactive accounts cannot log in — Spring Security calls `isEnabled()` which returns `isActive`.

---

## Database Schema

### Entity Relationships

```
users (1) ──────────< bookings (N)
users (1) ──────────< subscriptions (N)

parking_zones (1) ──< parkingSpots (N)
parkingSpots (1) ───< bookings (N)

package (1) ────────< subscriptions (N)
```

### Tables

#### `users`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| fullName | VARCHAR | not null |
| email | VARCHAR | unique, not null |
| password | VARCHAR | BCrypt hashed |
| role | VARCHAR | ADMIN \| USERS |
| isActive | BOOLEAN | default false |
| verificationToken | VARCHAR | null after verified |

#### `parking_zones`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| level | BIGINT | floor number |
| type | VARCHAR | e.g. INDOOR, OUTDOOR |
| lat | DOUBLE | optional — for proximity recommendations |
| lng | DOUBLE | optional — for proximity recommendations |

#### `parkingSpots`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| row | BIGINT | grid row |
| col | BIGINT | grid column |
| type | VARCHAR | e.g. STANDARD, DISABLED |
| status | VARCHAR | AVAILABLE \| OCCUPIED |
| zone_id | BIGINT FK | → parking_zones.id |

#### `bookings`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| startTime | TIMESTAMP | not null |
| endTime | TIMESTAMP | not null |
| status | VARCHAR | PENDING \| APPROVED \| EXPIRED \| CANCELLED |
| createdBy | BIGINT FK | → users.id |
| spotId | BIGINT FK | → parkingSpots.id |

#### `package`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| name | VARCHAR | unique name |
| description | VARCHAR | |
| durations | BIGINT | duration in days |
| price | BIGINT | price in smallest currency unit |

#### `subscriptions`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | auto-increment |
| startDate | DATE | not null |
| endDate | DATE | not null |
| price | BIGINT | snapshot of package price at time of purchase |
| packageName | VARCHAR FK | → package.name |
| userId | BIGINT FK | → users.id |

---

## Service Layer

### AuthService
Handles registration, email verification, and login. On registration, generates a UUID verification token and delegates email delivery to `EmailService`. On login, validates password with `BCryptPasswordEncoder` and checks `isActive` before issuing a token.

### UserService
CRUD for user profiles. Admin-only operations (`changeRole`, `deleteUser`, `listAll`) are enforced via `@PreAuthorize` on the controller. `getProfile` and `updateProfile` use the authenticated user's id from `@AuthenticationPrincipal`.

### ParkingZoneService
Zone CRUD. Exposes `findOrThrow(Long id)` as `public` so `ParkingSpotService` can resolve a zone entity when creating spots without duplicating the not-found logic.

### ParkingSpotService
Spot CRUD plus the core status-change path. Every status change (webhook, simulate, booking create/cancel/expire) goes through `updateStatus()`, which persists the new status then calls `RealtimeService.broadcastSpotUpdate()` and `RealtimeService.broadcastDashboard()`. Exposes `findOrThrow` and `toResponse` as `public` for use by `BookingService`.

### BookingService
Booking lifecycle management:
- **create**: checks time-window overlap via `BookingRepository.findOverlapping()`, saves as APPROVED, marks spot OCCUPIED.
- **cancel**: verifies ownership (throws 404 if userId doesn't match), sets CANCELLED, marks spot AVAILABLE.
- **expireOverdue**: called by the scheduler — finds all APPROVED bookings past end time, sets EXPIRED, marks spots AVAILABLE.
- **checkAndExpire**: private on-query helper — called on every read to lazily expire bookings that the scheduler hasn't processed yet.

### BookingExpirationJob
`@Scheduled(fixedRate = 300000)` — runs every 5 minutes and calls `BookingService.expireOverdue()`. Provides background expiration independent of user activity.

### PackageService
Package CRUD. Exposes `findOrThrow` as `public` for use by `SubscriptionService`.

### SubscriptionService
- **subscribe**: rejects if user already has an active subscription (409). Calculates end date as `now + package.durations` days.
- **getActiveSubscription**: queries `findActiveByUserId` with current date — returns 404 if none active.

### RealtimeService
Injects `SimpMessagingTemplate` and broadcasts two message types:
- `broadcastSpotUpdate(spot)` → `/topic/spots` with `SpotUpdateMessage(spotId, row, col, zoneId, status)`
- `broadcastDashboard()` → `/topic/dashboard` with system-wide counts and per-zone breakdown

### AnalyticsService
Three admin analytics queries, all ADMIN-only:
- **getOccupancy**: counts bookings per zone in a time range, expressed as a percentage of zone capacity.
- **getPeakHours**: groups bookings by hour of day, normalises to 0–100% relative to the busiest hour.
- **getUtilization**: total lifetime bookings vs. total spots per zone.

### PredictionService
Rule-based prediction using historical booking data. For a given zone and target time:
1. Count total spots in the zone.
2. Look up how many bookings historically started at the same hour and day-of-week (PostgreSQL `EXTRACT(DOW/HOUR)`).
3. Availability probability = `(totalSpots - historicalBooked) / totalSpots`, clamped to [0.0, 1.0].

Day-of-week conversion: Java `getDayOfWeek().getValue()` (Mon=1..Sun=7) → PostgreSQL DOW (Sun=0..Sat=6) via `javaDow % 7`.

### RecommendationService
Recommends the best zone using a three-tier fallback:
1. **By proximity** (if `lat`/`lng` provided): nearest zone with available spots using the haversine formula.
2. **By congestion**: zone with the most available spots right now.
3. **By prediction**: zone with the highest predicted availability 30 minutes from now.

---

## Real-time WebSocket

### Connection

Clients connect via SockJS at `ws://localhost:8080/ws`, then subscribe to STOMP topics.

### Topics

| Topic | Payload | Triggered by |
|-------|---------|-------------|
| `/topic/spots` | `SpotUpdateMessage` | Any spot status change |
| `/topic/dashboard` | `DashboardMessage` | Any spot status change |

`SpotUpdateMessage`: `{ spotId, row, col, zoneId, status }`

`DashboardMessage`: `{ total, available, occupied, byZone: [{ zoneId, total, available, occupied }] }`

### What triggers a broadcast

Every call to `ParkingSpotService.updateStatus()` triggers both broadcasts. This covers:
- Webhook (`POST /api/spots/webhook`)
- Simulate (`POST /api/spots/simulate`)
- Booking created → spot marked OCCUPIED
- Booking cancelled → spot marked AVAILABLE
- Booking expired (scheduler or on-query) → spot marked AVAILABLE

---

## Booking Lifecycle

```
create ──→ APPROVED ──→ EXPIRED   (past endTime — scheduler or on-query)
                   └──→ CANCELLED (user cancels)
```

Spot status mirrors booking status:
- APPROVED → spot OCCUPIED
- EXPIRED / CANCELLED → spot AVAILABLE

There is no PENDING state used in practice — bookings are immediately APPROVED or rejected with 409.

---

## API Summary

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register |
| POST | `/api/auth/login` | Public | Login → JWT |
| GET | `/api/auth/verify` | Public | Verify email |
| GET | `/api/users/me` | Any | Own profile |
| PUT | `/api/users/me` | Any | Update name |
| GET | `/api/admin/users` | ADMIN | List users |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Change role |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete user |
| GET | `/api/zones` | Any | List zones |
| GET | `/api/zones/{id}` | Any | Get zone |
| POST | `/api/zones` | ADMIN | Create zone |
| PUT | `/api/zones/{id}` | ADMIN | Update zone |
| DELETE | `/api/zones/{id}` | ADMIN | Delete zone |
| GET | `/api/zones/{id}/spots` | Any | List spots |
| POST | `/api/zones/{id}/spots` | ADMIN | Add spot |
| PUT | `/api/spots/{id}` | ADMIN | Update spot |
| DELETE | `/api/spots/{id}` | ADMIN | Delete spot |
| GET | `/api/spots/dashboard` | Any | Availability summary |
| POST | `/api/spots/webhook` | ADMIN | Update spot status |
| POST | `/api/spots/simulate` | ADMIN | Randomise statuses |
| POST | `/api/bookings` | USERS | Create booking |
| GET | `/api/bookings/my` | USERS | Own bookings |
| GET | `/api/bookings/{id}` | Any | Get booking |
| PATCH | `/api/bookings/{id}/cancel` | USERS | Cancel booking |
| GET | `/api/admin/bookings` | ADMIN | All bookings |
| GET | `/api/packages` | Any | List packages |
| POST | `/api/packages` | ADMIN | Create package |
| PUT | `/api/packages/{id}` | ADMIN | Update package |
| DELETE | `/api/packages/{id}` | ADMIN | Delete package |
| POST | `/api/subscriptions` | USERS | Subscribe |
| GET | `/api/subscriptions/my` | USERS | Active subscription |
| GET | `/api/admin/subscriptions` | ADMIN | All subscriptions |
| GET | `/api/analytics/occupancy` | ADMIN | Occupancy by zone/period |
| GET | `/api/analytics/peak-hours` | ADMIN | Peak hours |
| GET | `/api/analytics/utilization` | ADMIN | Utilization by zone |
| GET | `/api/predict/availability` | Any | Availability prediction |
| GET | `/api/recommend/spot` | Any | Zone recommendation |

---

## Error Handling

All errors return a consistent JSON body via `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{ "error": "NOT_FOUND", "message": "Booking not found with id: 5" }
```

| Exception | Status |
|-----------|--------|
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `MethodArgumentNotValidException` | 400 |
| Any other `Exception` | 500 |

---

## Configuration

All sensitive values are injected via environment variables:

| Property | Env Variable | Description |
|----------|-------------|-------------|
| `spring.datasource.username` | `PGDB_USERNAME` | PostgreSQL username |
| `spring.datasource.password` | `PGDB_PASSWORD` | PostgreSQL password |
| `spring.mail.host` | `MAIL_HOST` | SMTP host |
| `spring.mail.username` | `MAIL_USERNAME` | SMTP username |
| `spring.mail.password` | `MAIL_PASSWORD` | SMTP password |
| `app.jwt.secret` | — | HMAC-SHA256 signing key (base64) |
| `app.jwt.expiration` | — | Token TTL in ms (default 86400000 = 24h) |
| `app.base-url` | — | Base URL used in verification email links |

Database: `jdbc:postgresql://localhost:5432/parkingSystem`

Schema is managed by Hibernate `ddl-auto=update` — tables are created/altered automatically on startup.

---

## Testing

49 unit tests across 10 test classes. All tests use `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks` — no Spring context is loaded for service tests, keeping them fast.

| Test Class | Tests |
|------------|-------|
| `DemoApplicationTests` | 1 (context load) |
| `AuthControllerTest` | 4 |
| `AuthServiceTest` | 8 |
| `EmailServiceTest` | 3 |
| `UserServiceTest` | 7 |
| `ParkingZoneServiceTest` | 6 |
| `ParkingSpotServiceTest` | 5 |
| `BookingServiceTest` | 4 |
| `JwtServiceTest` | 5 |
| `UserDetailsServiceImplTest` | 2 |
| `UserEntityTest` | 4 |

Run all tests:
```bash
cd ParkingSystemBackend
./mvnw test
```
