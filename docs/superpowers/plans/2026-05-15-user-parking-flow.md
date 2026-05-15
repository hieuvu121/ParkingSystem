# User Parking Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a role-based flow for normal users: zone picker → spot grid (real-time) → booking modal (subscription or pay-per-use) → bookings list page.

**Architecture:** `App.jsx` routes `USERS`-role to `ParkingZonePage` (zone cards → spot grid in one component, WebSocket-connected) and `BookingsPage`; admin routing is unchanged. Backend `BookingResponse` is enriched with spot/zone/payment fields; `BookingRequest` gains `paymentType`; `BookingsEntity` stores the two new columns (auto-added by `ddl-auto=update`).

**Tech Stack:** React 18, Tailwind CSS, @stomp/stompjs + SockJS (already installed), Spring Boot 3 / JPA / PostgreSQL, JUnit 5 + Mockito (backend), manual browser smoke test (frontend — no test runner configured).

---

## File Map

### Backend — modified
| File | Change |
|------|--------|
| `ParkingSystemBackend/src/main/java/ParkingSystem/demo/entity/BookingsEntity.java` | Add `paymentType`, `costCents` fields |
| `ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingRequest.java` | Add `paymentType` field |
| `ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingResponse.java` | Expand record with spot/zone/payment fields |
| `ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/BookingService.java` | New `paymentType` param in `create()`; enrich `toResponse()` |
| `ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/BookingController.java` | Pass `paymentType` to service |
| `ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/BookingServiceTest.java` | Add two new tests; update existing `create()` calls |
| `ParkingSystemBackend/src/test/java/ParkingSystem/demo/controller/BookingControllerTest.java` | Add `paymentType` to request body; update `BookingResponse` construction |

### Frontend — new files
| File | Purpose |
|------|---------|
| `ParkingSystemFrontend/src/api/bookings.js` | `createBooking`, `getMyBookings`, `cancelBooking` |
| `ParkingSystemFrontend/src/components/parking/ZoneCard.jsx` | Single zone availability card |
| `ParkingSystemFrontend/src/components/parking/BookingModal.jsx` | Subscription check + duration picker + confirm |
| `ParkingSystemFrontend/src/pages/ParkingZonePage.jsx` | Zone picker + spot grid + WebSocket |
| `ParkingSystemFrontend/src/pages/BookingsPage.jsx` | User bookings list + cancel |

### Frontend — modified
| File | Change |
|------|--------|
| `ParkingSystemFrontend/src/api/subscription.js` | Add `subscribe()`, `getPackages()` |
| `ParkingSystemFrontend/src/components/dashboard/SpotGrid.jsx` | Add `onSpotClick` prop + hover ring on AVAILABLE spots |
| `ParkingSystemFrontend/src/App.jsx` | Role-based routing; `bookingRefreshKey` state |
| `ParkingSystemFrontend/src/components/layout/TopNav.jsx` | Normal-user tabs: Parking + Bookings |

---

## Task 1: Backend — Extend BookingsEntity + Update DTOs

**Files:**
- Modify: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/entity/BookingsEntity.java`
- Modify: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingRequest.java`
- Modify: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingResponse.java`

- [ ] **Step 1: Add `paymentType` and `costCents` to `BookingsEntity`**

Replace the full file content:

```java
package ParkingSystem.demo.entity;

import ParkingSystem.demo.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "bookings")
@Builder
public class BookingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BookingStatus status;

    @Column(nullable = false)
    private String paymentType;   // "SUBSCRIPTION" | "PAY_PER_USE"

    @Column(nullable = false)
    private Long costCents;       // 0 for subscription; 50/100/200 for pay-per-use

    @ManyToOne
    @JoinColumn(name = "createdBy", referencedColumnName = "id", nullable = false)
    private UserEntity userId;

    @ManyToOne
    @JoinColumn(name = "spotId", referencedColumnName = "id", nullable = false)
    private ParkingSpotsEntity spotId;
}
```

- [ ] **Step 2: Add `paymentType` to `BookingRequest`**

Replace the full file:

```java
package ParkingSystem.demo.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long spotId;
    @NotNull private LocalDateTime startTime;
    @NotNull @Future private LocalDateTime endTime;
    @NotNull private String paymentType;  // "SUBSCRIPTION" | "PAY_PER_USE"
}
```

- [ ] **Step 3: Expand `BookingResponse` record**

Replace the full file:

```java
package ParkingSystem.demo.dto.booking;

import ParkingSystem.demo.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id, Long spotId, Long userId,
        LocalDateTime startTime, LocalDateTime endTime,
        BookingStatus status,
        Long spotRow, Long spotCol, String spotType,
        Long zoneId, Long zoneLevel, String zoneType,
        String paymentType, Long costCents
) {}
```

- [ ] **Step 4: Compile check**

```bash
cd ParkingSystemBackend && ./mvnw compile -q 2>&1 | head -40
```

Expected: compilation errors in `BookingService` and `BookingController` (they still call the old `create()` signature and old `BookingResponse` constructor — that is expected and will be fixed in the next two tasks).

- [ ] **Step 5: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/entity/BookingsEntity.java \
        ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingRequest.java \
        ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/BookingResponse.java
git commit -m "feat: extend BookingsEntity + DTOs with paymentType and costCents"
```

---

## Task 2: Backend — Update BookingService (TDD)

**Files:**
- Modify: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/BookingService.java`
- Modify: `ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/BookingServiceTest.java`

- [ ] **Step 1: Write two failing tests in `BookingServiceTest.java`**

Open `BookingServiceTest.java`. Add the following imports at the top if not already present:

```java
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
```

Add these two test methods to the class (keep all existing tests):

```java
@Test
void create_withSubscription_setsZeroCost() {
    ParkingZonesEntity zone = ParkingZonesEntity.builder()
            .id(10L).level(1L).type("INDOOR").lat(0.0).lng(0.0).build();
    ParkingSpotsEntity spot = ParkingSpotsEntity.builder()
            .id(1L).row(2L).col(3L).type("STANDARD").status(SpotStatus.AVAILABLE)
            .zone_id(zone).build();
    UserEntity user = mock(UserEntity.class);
    when(user.getId()).thenReturn(5L);

    LocalDateTime start = LocalDateTime.now().plusMinutes(1);
    LocalDateTime end = start.plusHours(1);

    when(spotService.findOrThrow(1L)).thenReturn(spot);
    when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());
    when(bookingRepository.save(any())).thenAnswer(inv -> {
        BookingsEntity b = inv.getArgument(0);
        return BookingsEntity.builder().id(100L)
                .startTime(b.getStartTime()).endTime(b.getEndTime())
                .status(b.getStatus()).userId(b.getUserId()).spotId(b.getSpotId())
                .paymentType(b.getPaymentType()).costCents(b.getCostCents()).build();
    });

    BookingResponse response = bookingService.create(user, 1L, start, end, "SUBSCRIPTION");

    assertThat(response.paymentType()).isEqualTo("SUBSCRIPTION");
    assertThat(response.costCents()).isEqualTo(0L);
    assertThat(response.zoneId()).isEqualTo(10L);
    assertThat(response.zoneType()).isEqualTo("INDOOR");
}

@Test
void create_withPayPerUse_twoHours_computesTwoDollarCost() {
    ParkingZonesEntity zone = ParkingZonesEntity.builder()
            .id(10L).level(1L).type("INDOOR").lat(0.0).lng(0.0).build();
    ParkingSpotsEntity spot = ParkingSpotsEntity.builder()
            .id(1L).row(2L).col(3L).type("STANDARD").status(SpotStatus.AVAILABLE)
            .zone_id(zone).build();
    UserEntity user = mock(UserEntity.class);
    when(user.getId()).thenReturn(5L);

    LocalDateTime start = LocalDateTime.now().plusMinutes(1);
    LocalDateTime end = start.plusHours(2);

    when(spotService.findOrThrow(1L)).thenReturn(spot);
    when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());
    when(bookingRepository.save(any())).thenAnswer(inv -> {
        BookingsEntity b = inv.getArgument(0);
        return BookingsEntity.builder().id(100L)
                .startTime(b.getStartTime()).endTime(b.getEndTime())
                .status(b.getStatus()).userId(b.getUserId()).spotId(b.getSpotId())
                .paymentType(b.getPaymentType()).costCents(b.getCostCents()).build();
    });

    BookingResponse response = bookingService.create(user, 1L, start, end, "PAY_PER_USE");

    assertThat(response.paymentType()).isEqualTo("PAY_PER_USE");
    assertThat(response.costCents()).isEqualTo(200L);
}
```

Also find any existing test that calls `bookingService.create(user, spotId, startTime, endTime)` with 4 arguments and add `"PAY_PER_USE"` as the 5th argument. Update the mock for `bookingRepository.save()` in those tests to also set `.paymentType("PAY_PER_USE").costCents(...)` on the returned entity, and update any `new BookingResponse(...)` constructor calls to include the new fields (use `null` for zone fields in tests that don't need them — but prefer real values to avoid NPEs in `toResponse()`).

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest="BookingServiceTest" -q 2>&1 | tail -20
```

Expected: compilation error about `create()` missing argument — new tests can't compile yet because the service still has the old signature.

- [ ] **Step 3: Implement the updated `BookingService`**

Replace the full file:

```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotService spotService;

    @Transactional
    public BookingResponse create(UserEntity user, Long spotId,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  String paymentType) {
        ParkingSpotsEntity spot = spotService.findOrThrow(spotId);
        List<BookingsEntity> overlapping = bookingRepository.findOverlapping(spotId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Spot " + spotId + " is already booked for this time window");
        }
        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        long costCents = "SUBSCRIPTION".equals(paymentType) ? 0L : (durationMinutes * 100L / 60L);

        BookingsEntity booking = BookingsEntity.builder()
                .startTime(startTime).endTime(endTime)
                .status(BookingStatus.APPROVED)
                .userId(user).spotId(spot)
                .paymentType(paymentType)
                .costCents(costCents)
                .build();
        BookingsEntity saved = bookingRepository.save(booking);
        if (!startTime.isAfter(LocalDateTime.now())) {
            spotService.updateStatus(spotId, SpotStatus.OCCUPIED);
        }
        return toResponse(saved);
    }

    public List<BookingResponse> listForUser(Long userId) {
        return bookingRepository.findByUserId_Id(userId).stream()
                .map(this::checkAndExpire).toList();
    }

    public BookingResponse getById(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        if (!booking.getUserId().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }
        return checkAndExpire(booking);
    }

    @Transactional
    public void cancel(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        if (!booking.getUserId().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        spotService.updateStatus(booking.getSpotId().getId(), SpotStatus.AVAILABLE);
    }

    public PageResponse<BookingResponse> listAll(Pageable pageable) {
        return PageResponse.from(bookingRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional
    public void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> spotIds = bookingRepository.findExpired(now).stream()
                .map(b -> b.getSpotId().getId())
                .collect(Collectors.toList());
        if (spotIds.isEmpty()) return;
        bookingRepository.bulkExpire(now);
        spotIds.forEach(id -> spotService.updateStatus(id, SpotStatus.AVAILABLE));
    }

    private BookingResponse checkAndExpire(BookingsEntity b) {
        if (b.getStatus() == BookingStatus.APPROVED && b.getEndTime().isBefore(LocalDateTime.now())) {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            spotService.updateStatus(b.getSpotId().getId(), SpotStatus.AVAILABLE);
        }
        return toResponse(b);
    }

    private BookingsEntity findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private BookingResponse toResponse(BookingsEntity b) {
        ParkingSpotsEntity spot = b.getSpotId();
        ParkingZonesEntity zone = spot.getZone_id();
        return new BookingResponse(
                b.getId(), spot.getId(), b.getUserId().getId(),
                b.getStartTime(), b.getEndTime(), b.getStatus(),
                spot.getRow(), spot.getCol(), spot.getType(),
                zone.getId(), zone.getLevel(), zone.getType(),
                b.getPaymentType(), b.getCostCents()
        );
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest="BookingServiceTest" -q 2>&1 | tail -10
```

Expected output: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/BookingService.java \
        ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/BookingServiceTest.java
git commit -m "feat: update BookingService to record paymentType and compute costCents"
```

---

## Task 3: Backend — Update BookingController + Controller Test

**Files:**
- Modify: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/BookingController.java`
- Modify: `ParkingSystemBackend/src/test/java/ParkingSystem/demo/controller/BookingControllerTest.java`

- [ ] **Step 1: Update `BookingController.create()` to pass `paymentType`**

In `BookingController.java`, change only the `create()` method body:

```java
@PostMapping("/bookings")
@PreAuthorize("hasRole('USERS')")
public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal UserEntity user,
                                              @Valid @RequestBody BookingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
            bookingService.create(user, request.getSpotId(),
                    request.getStartTime(), request.getEndTime(),
                    request.getPaymentType()));
}
```

- [ ] **Step 2: Fix `BookingControllerTest` — add `paymentType` to request JSON and update `BookingResponse` construction**

Open `BookingControllerTest.java`. Find every place that:
1. Constructs a request JSON body for `POST /api/bookings` — add `"paymentType": "PAY_PER_USE"` to the JSON string.
2. Constructs a `new BookingResponse(...)` — add the 8 new fields at the end. Use these placeholder values for tests that don't exercise payment logic:

```java
// Full constructor for use in controller tests:
new BookingResponse(
    1L,          // id
    1L,          // spotId
    5L,          // userId
    startTime,   // startTime
    endTime,     // endTime
    BookingStatus.APPROVED,  // status
    2L,          // spotRow
    3L,          // spotCol
    "STANDARD",  // spotType
    10L,         // zoneId
    1L,          // zoneLevel
    "INDOOR",    // zoneType
    "PAY_PER_USE", // paymentType
    100L           // costCents
)
```

3. Mocks `bookingService.create(...)` with 4 args — update to 5 args, adding `"PAY_PER_USE"` or use `any(String.class)` as the 5th matcher.

- [ ] **Step 3: Run full backend test suite**

```bash
cd ParkingSystemBackend && ./mvnw test -q 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`. If any test fails due to a `BookingResponse` constructor mismatch or `create()` argument count, fix the relevant test file.

- [ ] **Step 4: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/BookingController.java \
        ParkingSystemBackend/src/test/java/ParkingSystem/demo/controller/BookingControllerTest.java
git commit -m "feat: wire paymentType through BookingController"
```

---

## Task 4: Frontend — API Layer

**Files:**
- Create: `ParkingSystemFrontend/src/api/bookings.js`
- Modify: `ParkingSystemFrontend/src/api/subscription.js`

- [ ] **Step 1: Create `src/api/bookings.js`**

```js
import { apiFetch } from './client';

function pad(n) { return String(n).padStart(2, '0'); }
export function toLocalISO(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
         `T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
}

export async function createBooking({ spotId, startTime, endTime, paymentType }) {
  const res = await apiFetch('/api/bookings', {
    method: 'POST',
    body: JSON.stringify({ spotId, startTime, endTime, paymentType }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to create booking' };
  return data;
}

export async function getMyBookings() {
  const res = await apiFetch('/api/bookings/my');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load bookings' };
  return data;
}

export async function cancelBooking(id) {
  const res = await apiFetch(`/api/bookings/${id}/cancel`, { method: 'PATCH' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to cancel booking' };
  }
}
```

- [ ] **Step 2: Extend `src/api/subscription.js` with `subscribe` and `getPackages`**

Append to the existing file (keep `getMySubscription` unchanged):

```js
export async function subscribe(packageId) {
  const res = await apiFetch('/api/subscriptions', {
    method: 'POST',
    body: JSON.stringify({ packageId }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to subscribe' };
  return data;
}

export async function getPackages() {
  const res = await apiFetch('/api/packages');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load packages' };
  return data;
}
```

- [ ] **Step 3: Commit**

```bash
git add ParkingSystemFrontend/src/api/bookings.js \
        ParkingSystemFrontend/src/api/subscription.js
git commit -m "feat: add bookings API and extend subscription API"
```

---

## Task 5: Frontend — ZoneCard Component

**Files:**
- Create: `ParkingSystemFrontend/src/components/parking/ZoneCard.jsx`

- [ ] **Step 1: Create `ZoneCard.jsx`**

```jsx
function dotColor(available, total) {
  if (total === 0) return '#6B7280';
  const pct = available / total;
  if (pct > 0.5) return '#4ADE80';
  if (pct > 0.2) return '#FACC15';
  return '#EF4444';
}

export default function ZoneCard({ zone, summary, onClick }) {
  const available = summary?.available ?? 0;
  const total = summary?.total ?? 0;
  const color = dotColor(available, total);

  return (
    <button
      type="button"
      onClick={onClick}
      className="bg-[#1C1C1E] rounded-2xl p-5 text-left hover:bg-[#2C2C2E] transition flex flex-col gap-3 border border-[#2C2C2E] hover:border-[#F5D26B] w-full"
    >
      <div className="flex items-center gap-2">
        <span className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: color }} />
        <span className="text-white font-semibold text-base">
          {zone.type} · L{zone.level}
        </span>
      </div>
      <p className="text-[#A1A1AA] text-sm">
        {available} / {total} available
      </p>
    </button>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add ParkingSystemFrontend/src/components/parking/ZoneCard.jsx
git commit -m "feat: add ZoneCard component"
```

---

## Task 6: Frontend — SpotGrid: Add `onSpotClick` Prop

**Files:**
- Modify: `ParkingSystemFrontend/src/components/dashboard/SpotGrid.jsx`

- [ ] **Step 1: Update `SpotGrid.jsx` to support `onSpotClick` and hover state**

Replace the full file:

```jsx
import { useState } from 'react';

const SPOT_SIZE = 28;
const GAP = 6;

export default function SpotGrid({ spots, loading, onSpotClick }) {
  const [hoveredId, setHoveredId] = useState(null);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-48 text-[#A1A1AA] text-sm">
        Loading spots…
      </div>
    );
  }
  if (!spots || spots.length === 0) {
    return (
      <div className="flex items-center justify-center h-48 text-[#A1A1AA] text-sm">
        No spots in this zone
      </div>
    );
  }

  const maxRow = Math.max(...spots.map((s) => Number(s.row)));
  const maxCol = Math.max(...spots.map((s) => Number(s.col)));
  const svgWidth = (maxCol) * (SPOT_SIZE + GAP) + GAP;
  const svgHeight = (maxRow) * (SPOT_SIZE + GAP) + GAP;

  return (
    <div className="overflow-auto p-4">
      <svg width={svgWidth} height={svgHeight}>
        {spots.map((spot) => {
          const x = (Number(spot.col) - 1) * (SPOT_SIZE + GAP) + GAP;
          const y = (Number(spot.row) - 1) * (SPOT_SIZE + GAP) + GAP;
          const isAvailable = spot.status === 'AVAILABLE';
          const isHovered = hoveredId === spot.id && isAvailable;
          const fill = isAvailable ? '#4ADE80' : '#EF4444';
          return (
            <rect
              key={spot.id}
              x={x}
              y={y}
              width={SPOT_SIZE}
              height={SPOT_SIZE}
              rx={4}
              fill={fill}
              opacity={0.9}
              stroke={isHovered ? '#F5D26B' : 'none'}
              strokeWidth={2}
              style={{ cursor: isAvailable && onSpotClick ? 'pointer' : 'default' }}
              onClick={isAvailable && onSpotClick ? () => onSpotClick(spot) : undefined}
              onMouseEnter={isAvailable && onSpotClick ? () => setHoveredId(spot.id) : undefined}
              onMouseLeave={() => setHoveredId(null)}
            >
              <title>{`Spot ${spot.row}-${spot.col}: ${spot.status}`}</title>
            </rect>
          );
        })}
      </svg>
      <div className="flex gap-4 mt-3 text-xs text-[#A1A1AA]">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-sm bg-[#4ADE80] inline-block" /> Available
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-sm bg-[#EF4444] inline-block" /> Occupied
        </span>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify admin DashboardPage still works** (it passes no `onSpotClick`, so spots remain non-clickable — no regression)

- [ ] **Step 3: Commit**

```bash
git add ParkingSystemFrontend/src/components/dashboard/SpotGrid.jsx
git commit -m "feat: add onSpotClick prop and hover ring to SpotGrid"
```

---

## Task 7: Frontend — BookingModal

**Files:**
- Create: `ParkingSystemFrontend/src/components/parking/BookingModal.jsx`

- [ ] **Step 1: Create `BookingModal.jsx`**

```jsx
import { useEffect, useState } from 'react';
import { getMySubscription, subscribe, getPackages } from '../../api/subscription';
import { createBooking, toLocalISO } from '../../api/bookings';

const DURATIONS = [
  { label: '30 min', minutes: 30, cents: 50 },
  { label: '1 hr',   minutes: 60, cents: 100 },
  { label: '2 hrs',  minutes: 120, cents: 200 },
];

export default function BookingModal({ spot, onClose, onBooked }) {
  const [step, setStep] = useState('loading'); // loading | choose-method | packages | form | submitting
  const [paymentType, setPaymentType] = useState(null);
  const [packages, setPackages] = useState([]);
  const [duration, setDuration] = useState(DURATIONS[1]);
  const [error, setError] = useState('');

  useEffect(() => {
    getMySubscription()
      .then((sub) => {
        if (sub) {
          setPaymentType('SUBSCRIPTION');
          setStep('form');
        } else {
          setStep('choose-method');
        }
      })
      .catch(() => setStep('choose-method'));
  }, []);

  async function handleSubscribeClick() {
    setError('');
    try {
      const pkgs = await getPackages();
      setPackages(pkgs);
      setStep('packages');
    } catch {
      setError('Failed to load packages. Try again.');
    }
  }

  async function handleSelectPackage(packageId) {
    setError('');
    try {
      await subscribe(packageId);
      setPaymentType('SUBSCRIPTION');
      setStep('form');
    } catch (e) {
      setError(e.message ?? 'Failed to subscribe. Try again.');
    }
  }

  async function handleConfirm() {
    setStep('submitting');
    setError('');
    const now = new Date();
    const end = new Date(now.getTime() + duration.minutes * 60 * 1000);
    try {
      await createBooking({
        spotId: spot.id,
        startTime: toLocalISO(now),
        endTime: toLocalISO(end),
        paymentType,
      });
      onBooked();
    } catch (e) {
      setError(
        e.status === 409
          ? 'Spot just got taken — please pick another.'
          : 'Something went wrong. Try again.'
      );
      setStep('form');
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-[#1C1C1E] rounded-2xl w-full max-w-sm p-6 flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h2 className="text-white font-bold text-lg">Book a Spot</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[#A1A1AA] hover:text-white text-xl leading-none"
          >
            ✕
          </button>
        </div>

        <p className="text-[#A1A1AA] text-sm">
          Spot R{spot.row}–C{spot.col} · {spot.type}
        </p>

        {/* Loading */}
        {step === 'loading' && (
          <p className="text-[#A1A1AA] text-sm text-center py-4">Checking subscription…</p>
        )}

        {/* Choose payment method */}
        {step === 'choose-method' && (
          <div className="flex flex-col gap-3">
            <p className="text-white text-sm font-medium">No active subscription</p>
            <button
              type="button"
              onClick={handleSubscribeClick}
              className="w-full py-3 rounded-xl bg-[#F5D26B] text-black font-semibold text-sm hover:opacity-90 transition"
            >
              Subscribe for unlimited parking
            </button>
            <button
              type="button"
              onClick={() => { setPaymentType('PAY_PER_USE'); setStep('form'); }}
              className="w-full py-3 rounded-xl bg-[#2C2C2E] text-white font-semibold text-sm hover:bg-[#3C3C3E] transition"
            >
              Pay per use — $1 / hr
            </button>
          </div>
        )}

        {/* Package list */}
        {step === 'packages' && (
          <div className="flex flex-col gap-3">
            <p className="text-white text-sm font-medium">Choose a plan</p>
            {packages.length === 0 && (
              <p className="text-[#A1A1AA] text-sm">No packages available.</p>
            )}
            {packages.map((pkg) => (
              <button
                key={pkg.id}
                type="button"
                onClick={() => handleSelectPackage(pkg.id)}
                className="w-full text-left p-4 rounded-xl bg-[#2C2C2E] hover:bg-[#3C3C3E] transition"
              >
                <p className="text-white font-semibold text-sm">{pkg.name}</p>
                <p className="text-[#A1A1AA] text-xs mt-1">
                  ${(pkg.price / 100).toFixed(2)} · {pkg.durations} days
                </p>
              </button>
            ))}
            <button
              type="button"
              onClick={() => setStep('choose-method')}
              className="text-[#A1A1AA] text-xs hover:text-white mt-1"
            >
              ← Back
            </button>
          </div>
        )}

        {/* Booking form */}
        {(step === 'form' || step === 'submitting') && (
          <div className="flex flex-col gap-4">
            {paymentType === 'SUBSCRIPTION' && (
              <p className="text-[#4ADE80] text-xs font-medium">Covered by your subscription</p>
            )}

            <div>
              <p className="text-[#A1A1AA] text-xs mb-2 uppercase tracking-widest">Duration</p>
              <div className="flex gap-2">
                {DURATIONS.map((d) => (
                  <button
                    key={d.minutes}
                    type="button"
                    onClick={() => setDuration(d)}
                    className={`flex-1 py-2 rounded-full text-sm font-semibold transition ${
                      duration.minutes === d.minutes
                        ? 'bg-[#F5D26B] text-black'
                        : 'bg-[#2C2C2E] text-white hover:bg-[#3C3C3E]'
                    }`}
                  >
                    {d.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-between items-center">
              <span className="text-[#A1A1AA] text-sm">Total</span>
              <span className="text-white font-bold text-base">
                {paymentType === 'SUBSCRIPTION'
                  ? 'Free'
                  : `$${(duration.cents / 100).toFixed(2)}`}
              </span>
            </div>

            <button
              type="button"
              onClick={handleConfirm}
              disabled={step === 'submitting'}
              className="w-full py-3 rounded-xl bg-[#F5D26B] text-black font-bold text-sm hover:opacity-90 transition disabled:opacity-50"
            >
              {step === 'submitting' ? 'Booking…' : 'Confirm Booking'}
            </button>
          </div>
        )}

        {error && (
          <p className="text-red-400 text-xs text-center">{error}</p>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add ParkingSystemFrontend/src/components/parking/BookingModal.jsx
git commit -m "feat: add BookingModal with subscription check and pay-per-use flow"
```

---

## Task 8: Frontend — ParkingZonePage

**Files:**
- Create: `ParkingSystemFrontend/src/pages/ParkingZonePage.jsx`

- [ ] **Step 1: Create `ParkingZonePage.jsx`**

```jsx
import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getZones, getDashboard, getSpots } from '../api/dashboard';
import ZoneCard from '../components/parking/ZoneCard';
import SpotGrid from '../components/dashboard/SpotGrid';
import BookingModal from '../components/parking/BookingModal';

export default function ParkingZonePage({ onWsStatusChange, onBooked }) {
  const [zones, setZones] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [selectedZoneId, setSelectedZoneId] = useState(null);
  const [spots, setSpots] = useState([]);
  const [loadingInit, setLoadingInit] = useState(true);
  const [loadingZone, setLoadingZone] = useState(false);
  const [error, setError] = useState('');
  const [modalSpot, setModalSpot] = useState(null);

  const selectedZoneIdRef = useRef(selectedZoneId);
  useEffect(() => { selectedZoneIdRef.current = selectedZoneId; }, [selectedZoneId]);

  // WebSocket — connect once, stay connected across zone switches
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        onWsStatusChange(true);

        client.subscribe('/topic/spots', (msg) => {
          const update = JSON.parse(msg.body);
          if (update.zoneId === selectedZoneIdRef.current) {
            setSpots((prev) =>
              prev.map((s) => s.id === update.spotId ? { ...s, status: update.status } : s)
            );
          }
        });

        client.subscribe('/topic/dashboard', (msg) => {
          setDashboard(JSON.parse(msg.body));
        });
      },
      onDisconnect: () => onWsStatusChange(false),
      onStompError: () => onWsStatusChange(false),
    });
    client.activate();
    return () => client.deactivate();
  }, []);  // eslint-disable-line react-hooks/exhaustive-deps

  // Initial load
  useEffect(() => {
    Promise.all([getZones(), getDashboard()])
      .then(([z, d]) => { setZones(z); setDashboard(d); })
      .catch((err) => setError(err.message ?? 'Failed to load zones'))
      .finally(() => setLoadingInit(false));
  }, []);

  // Load spots when a zone is selected
  useEffect(() => {
    if (!selectedZoneId) return;
    let ignore = false;
    setLoadingZone(true);
    setSpots([]);
    getSpots(selectedZoneId)
      .then((data) => { if (!ignore) setSpots(data); })
      .catch((err) => { if (!ignore) setError(err.message ?? 'Failed to load spots'); })
      .finally(() => { if (!ignore) setLoadingZone(false); });
    return () => { ignore = true; };
  }, [selectedZoneId]);

  const selectedZone = zones.find((z) => z.id === selectedZoneId);

  if (loadingInit) {
    return (
      <div className="min-h-screen bg-[#111111] flex items-center justify-center text-white">
        Loading…
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#111111] flex flex-col items-center justify-center gap-4 text-white">
        <p className="text-red-400">{error}</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-[#F5D26B] text-black rounded-full text-sm font-semibold"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white">
      {/* View 1: Zone card grid */}
      {!selectedZoneId && (
        <div className="max-w-2xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold mb-1">Choose a Parking Zone</h1>
          <p className="text-[#A1A1AA] text-sm mb-6">Select a zone to see available spots</p>
          <div className="grid grid-cols-2 gap-4">
            {zones.map((zone) => {
              const summary = dashboard?.byZone?.find((z) => z.zoneId === zone.id);
              return (
                <ZoneCard
                  key={zone.id}
                  zone={zone}
                  summary={summary}
                  onClick={() => setSelectedZoneId(zone.id)}
                />
              );
            })}
          </div>
        </div>
      )}

      {/* View 2: Spot grid */}
      {selectedZoneId && (
        <div className="max-w-2xl mx-auto px-4 py-8">
          <div className="flex items-center gap-3 mb-6">
            <button
              type="button"
              onClick={() => { setSelectedZoneId(null); setSpots([]); }}
              className="text-[#A1A1AA] hover:text-white text-sm flex items-center gap-1 transition"
            >
              ← Zones
            </button>
            <span className="text-[#2C2C2E]">|</span>
            <h1 className="text-white font-bold text-lg">
              {selectedZone ? `${selectedZone.type} · Level ${selectedZone.level}` : ''}
            </h1>
          </div>

          <div className="bg-[#1C1C1E] rounded-2xl">
            <div className="px-4 pt-4 pb-2">
              <h2 className="text-sm text-[#A1A1AA] uppercase tracking-widest">Spot Map</h2>
              <p className="text-xs text-[#A1A1AA] mt-1">Click an available spot to book</p>
            </div>
            <SpotGrid
              spots={spots}
              loading={loadingZone}
              onSpotClick={(spot) => setModalSpot(spot)}
            />
          </div>
        </div>
      )}

      {/* Booking modal */}
      {modalSpot && (
        <BookingModal
          spot={modalSpot}
          onClose={() => setModalSpot(null)}
          onBooked={() => {
            setModalSpot(null);
            onBooked?.();
          }}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add ParkingSystemFrontend/src/pages/ParkingZonePage.jsx
git commit -m "feat: add ParkingZonePage with zone picker, spot grid, and WebSocket"
```

---

## Task 9: Frontend — BookingsPage

**Files:**
- Create: `ParkingSystemFrontend/src/pages/BookingsPage.jsx`

- [ ] **Step 1: Create `BookingsPage.jsx`**

```jsx
import { useEffect, useState } from 'react';
import { getMyBookings, cancelBooking } from '../api/bookings';

function formatDateTime(iso) {
  const d = new Date(iso);
  const today = new Date();
  const isToday = d.toDateString() === today.toDateString();
  const time = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  return isToday ? `Today ${time}` : `${d.toLocaleDateString()} ${time}`;
}

function formatDuration(startIso, endIso) {
  const mins = (new Date(endIso) - new Date(startIso)) / 60000;
  if (mins < 60) return `${mins} min`;
  if (mins === 60) return '1 hr';
  return `${mins / 60} hrs`;
}

function formatCost(paymentType, costCents) {
  if (paymentType === 'SUBSCRIPTION') return 'Free (subscription)';
  return `$${(costCents / 100).toFixed(2)}`;
}

const STATUS_STYLE = {
  APPROVED:  'bg-[#14532d] text-[#4ADE80]',
  EXPIRED:   'bg-[#1C1C1E] text-[#6B7280]',
  CANCELLED: 'bg-[#3B0000] text-[#EF4444]',
};

export default function BookingsPage({ refreshKey }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(null);

  useEffect(() => {
    setLoading(true);
    getMyBookings()
      .then((data) => setBookings([...data].reverse()))
      .catch((err) => setError(err.message ?? 'Failed to load bookings'))
      .finally(() => setLoading(false));
  }, [refreshKey]);

  async function handleCancel(id) {
    setCancelling(id);
    try {
      await cancelBooking(id);
      setBookings((prev) =>
        prev.map((b) => b.id === id ? { ...b, status: 'CANCELLED' } : b)
      );
    } catch (e) {
      setError(e.message ?? 'Failed to cancel booking');
    } finally {
      setCancelling(null);
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#111111] flex items-center justify-center text-white">
        Loading…
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold mb-6">My Bookings</h1>

        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

        {bookings.length === 0 && (
          <p className="text-[#A1A1AA] text-sm">No bookings yet.</p>
        )}

        <div className="flex flex-col gap-3">
          {bookings.map((b) => (
            <div key={b.id} className="bg-[#1C1C1E] rounded-2xl p-4 flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <p className="text-white font-semibold text-sm">
                  {b.zoneType} L{b.zoneLevel} · R{b.spotRow}-C{b.spotCol}
                </p>
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_STYLE[b.status] ?? STATUS_STYLE.EXPIRED}`}>
                  {b.status}
                </span>
              </div>

              <p className="text-[#A1A1AA] text-xs">
                {formatDateTime(b.startTime)} – {formatDateTime(b.endTime)}
              </p>

              <p className="text-[#A1A1AA] text-xs">
                {formatDuration(b.startTime, b.endTime)} · {formatCost(b.paymentType, b.costCents)}
              </p>

              {b.status === 'APPROVED' && (
                <button
                  type="button"
                  disabled={cancelling === b.id}
                  onClick={() => handleCancel(b.id)}
                  className="self-end text-xs text-red-400 hover:text-red-300 disabled:opacity-50 transition"
                >
                  {cancelling === b.id ? 'Cancelling…' : 'Cancel'}
                </button>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add ParkingSystemFrontend/src/pages/BookingsPage.jsx
git commit -m "feat: add BookingsPage with booking list and cancel action"
```

---

## Task 10: Frontend — App + TopNav Wiring + Smoke Test

**Files:**
- Modify: `ParkingSystemFrontend/src/App.jsx`
- Modify: `ParkingSystemFrontend/src/components/layout/TopNav.jsx`

- [ ] **Step 1: Update `App.jsx` for role-based routing**

Replace the full file:

```jsx
import { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';
import ParkingZonePage from './pages/ParkingZonePage';
import BookingsPage from './pages/BookingsPage';
import AccountPage from './pages/AccountPage';
import AdminUsersPage from './pages/AdminUsersPage';
import TopNav from './components/layout/TopNav';

export default function App() {
  const { token, user } = useAuth();
  const [page, setPage] = useState('parking');
  const [wsConnected, setWsConnected] = useState(false);
  const [bookingRefreshKey, setBookingRefreshKey] = useState(0);

  // user is fetched async after token; redirect admin to correct default once role is known
  useEffect(() => {
    if (user?.role === 'ADMIN' && page === 'parking') setPage('dashboard');
  }, [user]);  // eslint-disable-line react-hooks/exhaustive-deps

  if (!token) return <AuthPage />;

  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="min-h-screen bg-[#111111]">
      <TopNav page={page} setPage={setPage} user={user} wsConnected={wsConnected} />

      {isAdmin && page === 'dashboard' && (
        <DashboardPage onWsStatusChange={setWsConnected} />
      )}
      {isAdmin && page === 'admin-users' && <AdminUsersPage />}
      {page === 'account' && <AccountPage />}

      {!isAdmin && page === 'parking' && (
        <ParkingZonePage
          onWsStatusChange={setWsConnected}
          onBooked={() => setBookingRefreshKey((k) => k + 1)}
        />
      )}
      {!isAdmin && page === 'bookings' && (
        <BookingsPage refreshKey={bookingRefreshKey} />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Update `TopNav.jsx` normal-user tabs**

Replace the full file:

```jsx
export default function TopNav({ page, setPage, user, wsConnected }) {
  const isAdmin = user?.role === 'ADMIN';

  const navItems = isAdmin
    ? [
        { key: 'dashboard',       label: 'Dashboard' },
        { key: 'admin-users',     label: 'Users' },
        { key: 'admin-analytics', label: 'Analytics' },
      ]
    : [
        { key: 'parking',  label: 'Parking' },
        { key: 'bookings', label: 'Bookings' },
      ];

  return (
    <header className="bg-[#1C1C1E] border-b border-[#2C2C2E] px-4 py-3 flex items-center gap-4">
      <span className="text-[#F5D26B] text-xl font-bold select-none">🅿</span>
      <span className="text-white font-semibold text-lg mr-2 hidden sm:block">Parking System</span>

      <nav className="flex items-center gap-1 flex-1">
        {navItems.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setPage(item.key)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition border-b-2 ${
              page === item.key
                ? 'text-[#F5D26B] border-[#F5D26B]'
                : 'text-[#A1A1AA] hover:text-white border-transparent'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <button
        type="button"
        onClick={() => setPage('account')}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
          page === 'account' ? 'text-[#F5D26B]' : 'text-[#A1A1AA] hover:text-white'
        }`}
      >
        <span className="w-6 h-6 rounded-full bg-[#2C2C2E] flex items-center justify-center text-xs font-bold text-white">
          {user?.fullName?.[0]?.toUpperCase() ?? '?'}
        </span>
        <span className="hidden sm:block">{user?.fullName ?? 'Account'}</span>
      </button>

      <span className="flex items-center gap-1.5 text-xs text-[#A1A1AA] ml-1">
        <span
          className={`w-2 h-2 rounded-full flex-shrink-0 ${
            wsConnected ? 'bg-[#4ADE80] animate-pulse' : 'bg-[#6B7280]'
          }`}
        />
        <span className="hidden sm:block">{wsConnected ? 'Live' : 'Connecting…'}</span>
      </span>
    </header>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add ParkingSystemFrontend/src/App.jsx \
        ParkingSystemFrontend/src/components/layout/TopNav.jsx
git commit -m "feat: role-based routing and user nav tabs in App and TopNav"
```

- [ ] **Step 4: Start backend and frontend servers**

Terminal 1 (backend):
```bash
cd ParkingSystemBackend && ./mvnw spring-boot:run
```
Expected: `Started DemoApplication in … seconds`

Terminal 2 (frontend):
```bash
cd ParkingSystemFrontend && npm run dev
```
Expected: `Local: http://localhost:5173/`

- [ ] **Step 5: Smoke test — normal user flow**

Open `http://localhost:5173` in a browser. Log in as a `USERS`-role account.

Verify:
1. After login, the page shows "Choose a Parking Zone" with zone cards (not the admin dashboard)
2. TopNav shows "Parking" and "Bookings" tabs
3. WS dot turns green (Live)
4. Zone cards show availability counts and colored dots
5. Click a zone card → spot grid appears with "← Zones" back button
6. Back button returns to zone card grid
7. Green (AVAILABLE) spots show a gold hover ring when moused over
8. Red (OCCUPIED) spots show no hover effect and are not clickable
9. Click an available spot → booking modal opens
10. Modal shows "Checking subscription…" then either booking form (if subscribed) or two option cards
11. Select "Pay per use" → booking form appears with 3 duration pills and correct cost display
12. Choose "2 hrs" → cost shows "$2.00"
13. Click "Confirm Booking" → modal closes, spot turns red in the grid (real-time update via WS)
14. Click "Bookings" tab → booking appears in list with correct zone/spot/time/cost
15. APPROVED booking shows "Cancel" button → click it → status changes to CANCELLED inline

- [ ] **Step 6: Smoke test — admin user flow (regression check)**

Log in as an `ADMIN`-role account.

Verify:
1. Default page is the admin Dashboard (zone tabs, SpotGrid, StatsPanel — unchanged)
2. TopNav shows Dashboard / Users / Analytics tabs
3. Admin SpotGrid has no click/hover behavior on spots

- [ ] **Step 7: Final commit if any fixes were needed**

```bash
git add -p  # stage only the fix
git commit -m "fix: <describe what was fixed>"
```
