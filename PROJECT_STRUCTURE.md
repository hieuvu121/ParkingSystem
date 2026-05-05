# Project Structure

```
ParkingSystemApp/
├── README.md
├── PROJECT_STRUCTURE.md
└── ParkingSystemBackend/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/ParkingSystem/demo/
        │   │   ├── DemoApplication.java               # Spring Boot entry point
        │   │   │
        │   │   ├── config/
        │   │   │   └── WebSocketConfig.java            # STOMP broker, /ws SockJS endpoint
        │   │   │
        │   │   ├── security/
        │   │   │   ├── SecurityConfig.java             # Filter chain, permit rules, @EnableMethodSecurity
        │   │   │   ├── JwtService.java                 # Sign and validate HMAC-SHA256 JWT tokens
        │   │   │   ├── JwtAuthFilter.java              # Extracts JWT from header, sets SecurityContext
        │   │   │   └── UserDetailsServiceImpl.java     # Loads UserEntity by email for Spring Security
        │   │   │
        │   │   ├── entity/
        │   │   │   ├── UserEntity.java                 # users table — implements UserDetails
        │   │   │   ├── ParkingZonesEntity.java         # parking_zones table — level, type, lat, lng
        │   │   │   ├── ParkingSpotsEntity.java         # parkingSpots table — row, col, type, status, zone FK
        │   │   │   ├── BookingsEntity.java             # bookings table — startTime, endTime, status, user FK, spot FK
        │   │   │   ├── PackagesEntity.java             # package table — name, description, durations, price
        │   │   │   └── SubscriptionsEntity.java        # subscriptions table — startDate, endDate, price, package FK, user FK
        │   │   │
        │   │   ├── enums/
        │   │   │   ├── Role.java                       # ADMIN, USERS
        │   │   │   ├── BookingStatus.java              # PENDING, APPROVED, EXPIRED, CANCELLED
        │   │   │   └── SpotStatus.java                 # AVAILABLE, OCCUPIED
        │   │   │
        │   │   ├── repository/
        │   │   │   ├── UserRepository.java             # findByEmail
        │   │   │   ├── ParkingZoneRepository.java      # basic JPA
        │   │   │   ├── ParkingSpotRepository.java      # findByZone, countByStatus, countByZoneAndStatus
        │   │   │   ├── BookingRepository.java          # findOverlapping, findExpired, analytics/prediction queries
        │   │   │   ├── PackageRepository.java          # findByName
        │   │   │   └── SubscriptionRepository.java     # findByUserId, findActiveByUserId
        │   │   │
        │   │   ├── dto/
        │   │   │   ├── RegisterRequest.java            # fullName, email, password
        │   │   │   ├── LoginRequest.java               # email, password
        │   │   │   ├── AuthResponse.java               # token
        │   │   │   ├── user/
        │   │   │   │   ├── UserProfileResponse.java    # id, fullName, email, role
        │   │   │   │   ├── UpdateProfileRequest.java   # fullName
        │   │   │   │   └── UpdateRoleRequest.java      # role
        │   │   │   ├── zone/
        │   │   │   │   ├── ZoneRequest.java            # level, type
        │   │   │   │   └── ZoneResponse.java           # id, level, type
        │   │   │   ├── spot/
        │   │   │   │   ├── SpotRequest.java            # row, col, type
        │   │   │   │   ├── SpotResponse.java           # id, row, col, type, status, zoneId
        │   │   │   │   ├── SpotStatusUpdateRequest.java# spotId, status
        │   │   │   │   ├── ZoneSummary.java            # zoneId, total, available, occupied
        │   │   │   │   └── DashboardResponse.java      # total, available, occupied, byZone[]
        │   │   │   ├── booking/
        │   │   │   │   ├── BookingRequest.java         # spotId, startTime, endTime
        │   │   │   │   └── BookingResponse.java        # id, spotId, userId, startTime, endTime, status
        │   │   │   ├── pkg/
        │   │   │   │   ├── PackageRequest.java         # name, description, durations, price
        │   │   │   │   └── PackageResponse.java        # id, name, description, durations, price
        │   │   │   ├── subscription/
        │   │   │   │   ├── SubscriptionRequest.java    # packageId
        │   │   │   │   └── SubscriptionResponse.java   # id, userId, packageId, packageName, startDate, endDate, price
        │   │   │   ├── analytics/
        │   │   │   │   ├── OccupancyResponse.java      # zoneId, from, to, occupancyPercent
        │   │   │   │   ├── PeakHourResponse.java       # hour, averageOccupancyPercent
        │   │   │   │   └── UtilizationResponse.java    # zoneId, totalSpots, totalBookings, utilizationPercent
        │   │   │   ├── prediction/
        │   │   │   │   └── AvailabilityPredictionResponse.java  # zoneId, targetTime, availabilityProbability
        │   │   │   ├── recommendation/
        │   │   │   │   └── RecommendationResponse.java # zoneId, reason, availableSpots, predictedProbability
        │   │   │   └── ws/
        │   │   │       ├── SpotUpdateMessage.java      # spotId, row, col, zoneId, status  → /topic/spots
        │   │   │       └── DashboardMessage.java       # total, available, occupied, byZone → /topic/dashboard
        │   │   │
        │   │   ├── service/
        │   │   │   ├── AuthService.java                # register, verify email, login
        │   │   │   ├── EmailService.java               # send verification email via JavaMailSender
        │   │   │   ├── UserService.java                # profile CRUD, role change, delete
        │   │   │   ├── ParkingZoneService.java         # zone CRUD, findOrThrow (used by SpotService)
        │   │   │   ├── ParkingSpotService.java         # spot CRUD, updateStatus, dashboard, webhook, simulate
        │   │   │   ├── BookingService.java             # create, cancel, expire, on-query expiration check
        │   │   │   ├── PackageService.java             # package CRUD, findOrThrow (used by SubscriptionService)
        │   │   │   ├── SubscriptionService.java        # subscribe, getActive, listAll
        │   │   │   ├── RealtimeService.java            # STOMP broadcast — spots + dashboard
        │   │   │   ├── AnalyticsService.java           # occupancy by zone/period, peak hours, utilization
        │   │   │   ├── PredictionService.java          # rule-based availability probability by zone + time
        │   │   │   └── RecommendationService.java      # best zone by proximity, congestion, or prediction
        │   │   │
        │   │   ├── controller/
        │   │   │   ├── AuthController.java             # POST /api/auth/register|login, GET /api/auth/verify
        │   │   │   ├── UserController.java             # GET|PUT /api/users/me, admin CRUD /api/admin/users
        │   │   │   ├── ParkingZoneController.java      # CRUD /api/zones
        │   │   │   ├── ParkingSpotController.java      # CRUD /api/zones/{id}/spots, dashboard, webhook, simulate
        │   │   │   ├── BookingController.java          # POST|GET /api/bookings, PATCH cancel, admin list
        │   │   │   ├── PackageController.java          # CRUD /api/packages
        │   │   │   ├── SubscriptionController.java     # POST|GET /api/subscriptions, admin list
        │   │   │   ├── AnalyticsController.java        # GET /api/analytics/occupancy|peak-hours|utilization
        │   │   │   ├── PredictionController.java       # GET /api/predict/availability
        │   │   │   └── RecommendationController.java   # GET /api/recommend/spot
        │   │   │
        │   │   ├── scheduler/
        │   │   │   └── BookingExpirationJob.java       # @Scheduled every 5 min — expires overdue bookings
        │   │   │
        │   │   └── exception/
        │   │       ├── ErrorResponse.java              # record(error, message) — uniform error body
        │   │       ├── ResourceNotFoundException.java  # → 404
        │   │       ├── ConflictException.java          # → 409
        │   │       └── GlobalExceptionHandler.java     # @RestControllerAdvice — 400/404/409/500 handlers
        │   │
        │   └── resources/
        │       └── application.properties              # DB, JWT, mail, JPA config
        │
        └── test/java/ParkingSystem/demo/
            ├── DemoApplicationTests.java               # Spring context smoke test
            ├── entity/
            │   └── UserEntityTest.java
            ├── security/
            │   ├── JwtServiceTest.java
            │   └── UserDetailsServiceImplTest.java
            ├── controller/
            │   └── AuthControllerTest.java
            └── service/
                ├── AuthServiceTest.java
                ├── EmailServiceTest.java
                ├── UserServiceTest.java
                ├── ParkingZoneServiceTest.java
                ├── ParkingSpotServiceTest.java
                └── BookingServiceTest.java
```

## Package Responsibilities

| Package | Responsibility |
|---------|---------------|
| `config` | Spring configuration beans (WebSocket) |
| `security` | JWT lifecycle, request filter, Spring Security setup |
| `entity` | JPA-mapped database tables |
| `enums` | Shared constants stored as strings in DB |
| `repository` | Spring Data JPA interfaces — queries stay here, not in services |
| `dto` | Immutable request/response shapes — one sub-package per domain |
| `service` | Business logic — all inter-service calls go through service methods, never repositories directly |
| `controller` | HTTP layer — validates input, calls one service method, returns ResponseEntity |
| `scheduler` | Background jobs (@Scheduled) |
| `exception` | Typed exceptions + centralized handler |

## Key Data Flow

```
HTTP Request
  └── JwtAuthFilter          (validate token, set SecurityContext)
        └── Controller        (validate request body)
              └── Service     (business logic)
                    ├── Repository        (DB queries)
                    └── RealtimeService   (WebSocket push on spot status change)
                          └── SimpMessagingTemplate → /topic/spots, /topic/dashboard
```

## Database Tables

| Table | Entity | Notes |
|-------|--------|-------|
| `users` | `UserEntity` | Roles stored as strings (ADMIN, USERS) |
| `parking_zones` | `ParkingZonesEntity` | Optional lat/lng for proximity recommendations |
| `parkingSpots` | `ParkingSpotsEntity` | Status stored as string (AVAILABLE, OCCUPIED) |
| `bookings` | `BookingsEntity` | Status stored as string; createdBy FK → users |
| `package` | `PackagesEntity` | Duration in days |
| `subscriptions` | `SubscriptionsEntity` | Joins on package name column |
