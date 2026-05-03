# Parking System Backend — Full Design Spec

**Date:** 2026-05-03
**Scope:** Backend only (Spring Boot 4.0, PostgreSQL, STOMP WebSocket)
**Status:** Auth (Phase 0) complete. Phases 1–6 to be implemented.

---

## 1. Requirements Summary

| Feature | Module |
|---|---|
| User profiles + role-based access (ADMIN, USERS) | Phase 1 |
| Parking zone & spot CRUD + IoT webhook + simulate endpoint | Phase 2 |
| Booking creation, cancellation, auto-expiration | Phase 3 |
| Package & subscription management | Phase 4 |
| Real-time spot + dashboard updates via STOMP WebSocket | Phase 5 |
| Analytics, predictive availability, smart recommendations | Phase 6 |

---

## 2. Current State (Phase 0 — Done)

- `AuthController`: POST /api/auth/register, GET /api/auth/verify, POST /api/auth/login
- `AuthService`: register with email verification, login with JWT
- `EmailService`: sends verification link via SMTP
- `JwtService` + `JwtAuthFilter` + `SecurityConfig`: stateless JWT auth, BCrypt passwords
- `UserRepository`: findByEmail, findByVerificationToken
- All 6 entities created: User, ParkingZones, ParkingSpots, Bookings, Packages, Subscriptions
- Enums: `Role` (ADMIN, USERS), `BookingStatus` (PENDING, APPROVED, EXPIRED, CANCELLED), `SpotStatus` (OCCUPIED, AVAILABLE)

**Enum fix required before Phase 3:** Add `CANCELLED` to `BookingStatus`.

**Entity fix required before Phase 2:**
`ParkingSpotsEntity.status` is currently typed as `BookingStatus` — must be changed to `SpotStatus`.

---

## 3. Architecture

### Layers
```
Clients (App / Admin / IoT)
        ↓ REST + STOMP WebSocket
Security Layer (JwtAuthFilter, SecurityConfig)
        ↓
Controllers (REST + WebSocket)
        ↓
Services (Business Logic)
        ↓
Repositories (Spring Data JPA)
        ↓
PostgreSQL
```

### Package structure (additions)
```
controller/
  UserController
  ParkingZoneController
  ParkingSpotController
  BookingController
  PackageController
  SubscriptionController
  AnalyticsController
  RecommendationController
  PredictionController

service/
  UserService
  ParkingZoneService
  ParkingSpotService              ← owns updateStatus(), triggers RealtimeService
  BookingService                  ← owns expiration logic
  PackageService
  SubscriptionService
  AnalyticsService
  RealtimeService                 ← SimpMessagingTemplate broadcasts
  RecommendationService

repository/
  ParkingZoneRepository
  ParkingSpotRepository
  BookingRepository
  PackageRepository
  SubscriptionRepository

dto/                              ← one request + one response DTO per resource
scheduler/
  BookingExpirationJob            ← @Scheduled, runs every 5 minutes

exception/
  ResourceNotFoundException       ← 404
  ConflictException               ← 409
  GlobalExceptionHandler          ← @ControllerAdvice

config/
  WebSocketConfig                 ← STOMP broker, /ws endpoint
```

---

## 4. Build Phases

### Phase 1 — User Management

**Endpoints:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/users/me | Any | Get own profile |
| PUT | /api/users/me | Any | Update own profile (fullName) |
| GET | /api/admin/users | ADMIN | List all users |
| PATCH | /api/admin/users/{id}/role | ADMIN | Change user role |
| DELETE | /api/admin/users/{id} | ADMIN | Delete user |

**Notes:**
- No new repositories needed — uses existing `UserRepository`
- Password change not in scope for this phase
- Role check via `@PreAuthorize("hasRole('ADMIN')")`

---

### Phase 2 — Parking Zones & Spots

**Entity fix:** Change `ParkingSpotsEntity.status` from `BookingStatus` to `SpotStatus`.

**Endpoints:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/zones | Any (auth) | List all zones |
| POST | /api/zones | ADMIN | Create zone |
| PUT | /api/zones/{id} | ADMIN | Update zone |
| DELETE | /api/zones/{id} | ADMIN | Delete zone |
| GET | /api/zones/{id}/spots | Any (auth) | List spots in zone |
| POST | /api/zones/{id}/spots | ADMIN | Create spot in zone |
| PUT | /api/spots/{id} | ADMIN | Update spot |
| DELETE | /api/spots/{id} | ADMIN | Delete spot |
| GET | /api/spots/dashboard | Any (auth) | Total/available/occupied summary |
| POST | /api/spots/webhook | ADMIN | IoT sensor update: { spotId, status } |
| POST | /api/spots/simulate | ADMIN | Randomly flip spot statuses for testing |

**Spot status update flow:**
All status changes go through `ParkingSpotService.updateStatus(spotId, SpotStatus)` which persists to DB and calls `RealtimeService.broadcastSpotUpdate()` (wired in Phase 5 — no-op stub until then).

---

### Phase 3 — Booking Management

**Endpoints:**
| Method | Path | Role | Description |
|---|---|---|---|
| POST | /api/bookings | USERS | Create booking for a spot + time window |
| GET | /api/bookings/my | USERS | List own bookings |
| GET | /api/bookings/{id} | USERS | Get booking (with live status check) |
| PATCH | /api/bookings/{id}/cancel | USERS | Cancel a booking |
| GET | /api/admin/bookings | ADMIN | List all bookings |

**Expiration strategy:**
1. **On query:** `BookingService.getBooking()` checks if `endTime < now` and status is APPROVED → marks EXPIRED, frees spot.
2. **Scheduled sweep:** `BookingExpirationJob` runs every 5 minutes via `@Scheduled(fixedRate = 300000)`, finds all APPROVED bookings where `endTime < now`, marks them EXPIRED, and calls `ParkingSpotService.updateStatus(AVAILABLE)` for each.

**Double-booking prevention:**
Before creating a booking, check: no existing PENDING or APPROVED booking overlaps `[startTime, endTime]` for the same spot.

**Booking state transitions:**
```
PENDING → APPROVED (on creation, immediately)
APPROVED → EXPIRED (auto, by scheduler or on-query)
APPROVED → CANCELLED (user action)
```

---

### Phase 4 — Packages & Subscriptions

**Endpoints:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/packages | Any (auth) | List available packages |
| POST | /api/packages | ADMIN | Create package |
| PUT | /api/packages/{id} | ADMIN | Update package |
| DELETE | /api/packages/{id} | ADMIN | Delete package |
| POST | /api/subscriptions | USERS | Subscribe to a package |
| GET | /api/subscriptions/my | USERS | Get own active subscription |
| GET | /api/admin/subscriptions | ADMIN | List all subscriptions |

**Notes:**
- `startDate` = now, `endDate` = now + package.durations (days)
- One active subscription per user enforced in service layer
- Price copied from package at subscription time (snapshot)

---

### Phase 5 — Real-time WebSocket

**WebSocket config:**
```
Endpoint:     /ws  (SockJS fallback enabled)
App prefix:   /app
Broker:       SimpleBroker on /topic
```

**Topics:**
| Topic | Payload | Triggered by |
|---|---|---|
| /topic/spots | `{ spotId, row, col, zoneId, status }` | ParkingSpotService.updateStatus() |
| /topic/dashboard | `{ total, available, occupied, byZone[] }` | ParkingSpotService.updateStatus() |

**RealtimeService:**
- `broadcastSpotUpdate(ParkingSpotsEntity spot)` → converts to DTO, sends to `/topic/spots`
- `broadcastDashboard()` → queries live counts from DB, sends to `/topic/dashboard`
- Both called together inside `ParkingSpotService.updateStatus()` after DB persist

**No-op stub in Phase 2:** `RealtimeService` methods are written as empty stubs in Phase 2 so `ParkingSpotService` compiles. Full implementation happens in Phase 5.

---

### Phase 6 — Analytics, Predictions & Recommendations

**Analytics endpoints:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/analytics/occupancy | ADMIN | Occupancy % per zone over time (query param: from, to) |
| GET | /api/analytics/peak-hours | ADMIN | Average occupancy by hour-of-day |
| GET | /api/analytics/utilization | ADMIN | Utilization % per zone |

**Prediction endpoint:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/predict/availability | Any (auth) | Probability a zone has a spot available at a given time |

Prediction algorithm: query `bookings` table for historical records matching `dayOfWeek` + `hourOfDay`, compute `(spots - avg_booked) / spots` as probability. No ML library needed.

**Recommendation endpoint:**
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/recommend/spot | Any (auth) | Suggest best zone given current time + user location (optional: lat/lng) |

**Entity fix required before Phase 6:** Add `lat: Double` and `lng: Double` (nullable) fields to `ParkingZonesEntity` to support nearest-zone calculation.

Recommendation logic (priority order):
1. Nearest zone with available spots (if lat/lng provided by caller and zone has lat/lng set)
2. Least congested zone (lowest occupancy %)
3. Zone with highest predicted availability for next 30 minutes

---

## 5. Cross-Cutting Concerns

### Error Handling
- `ResourceNotFoundException` → 404 `{ "error": "...", "message": "..." }`
- `ConflictException` → 409
- `@ControllerAdvice` in `exception/GlobalExceptionHandler` catches all typed exceptions
- Validation errors from `@Valid` return 400 with field-level messages

### Security (role enforcement)
- `@PreAuthorize("hasRole('ADMIN')")` on all `/api/admin/**` endpoints
- `@EnableMethodSecurity` added to `SecurityConfig`
- All other authenticated endpoints require valid JWT (already enforced by filter chain)

### Testing
- Unit tests per service using Mockito (mock repositories)
- Integration tests per controller using `@SpringBootTest` + `MockMvc`
- Follow existing auth test patterns in the codebase

---

## 6. What Is NOT in Scope

- Frontend (React/Angular)
- Payment processing
- Push notifications
- ML-based predictions (using rule-based instead)
- External message broker (RabbitMQ/Redis) — SimpleBroker sufficient
- Multi-tenancy / multiple parking facilities
