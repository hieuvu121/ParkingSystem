# Design: Normal-User Parking Zone Flow

**Date:** 2026-05-15  
**Branch:** features/login  
**Scope:** Role-based routing, zone picker, spot grid, booking modal, bookings page, real-time WebSocket updates

---

## 1. Overview

Normal users (role `USERS`) currently land on the admin `DashboardPage` after login. This design replaces that with a dedicated flow:

1. **Zone picker** — card grid showing all parking zones with live availability
2. **Spot grid** — drill into a zone to see individual spots in real time
3. **Booking modal** — click an available spot to book it, with subscription/pay-per-use logic
4. **Bookings page** — list of the user's past and current bookings with cancel action

Admin users are unaffected.

---

## 2. Architecture & Routing

### Role-based routing (`App.jsx`)

| Role | Default page | Available pages |
|------|-------------|-----------------|
| `ADMIN` | `DashboardPage` (unchanged) | dashboard, admin-users, admin-analytics |
| `USERS` | `ParkingZonePage` | parking, bookings, account |

`App.jsx` checks `user?.role`:
- `ADMIN` → renders existing layout unchanged
- `USERS` → renders `ParkingZonePage` or `BookingsPage` based on `page` state; default page is `parking`

### TopNav (normal-user tabs)

```
[Parking]  [Bookings]       [Avatar]  [● Live]
```

Keys: `parking`, `bookings`. Account avatar and WS dot unchanged.

### `ParkingZonePage` internal navigation

Managed by a single `selectedZoneId` state:

```
selectedZoneId === null  →  Zone card grid  (view 1)
selectedZoneId !== null  →  Spot grid       (view 2)
```

No URL changes needed; this is in-page navigation.

---

## 3. Zone Card Grid (View 1)

**Data fetching:** on mount, fetch `GET /api/zones` and `GET /api/spots/dashboard` in parallel.

**Layout:** 2-column responsive card grid.

**Each zone card displays:**
- Type + level label: e.g. "INDOOR · L1"
- Availability: "18 / 20 available"
- Color dot:  
  - green (`#4ADE80`) if available > 50% of total  
  - yellow (`#FACC15`) if available > 20%  
  - red (`#EF4444`) otherwise

**Real-time updates:** WS `/topic/dashboard` updates the availability counts on every spot change. Same broadcast used by the admin dashboard.

**Interaction:** clicking a card sets `selectedZoneId` and fetches `GET /api/zones/{id}/spots`.

---

## 4. Spot Grid (View 2)

**Header:**
- "← Zones" back button → resets `selectedZoneId` to null
- Zone heading: e.g. "OUTDOOR · Level 2"

**Spot grid:** reuses existing `SpotGrid` component without modification.

**Interactivity:**
- `AVAILABLE` spots: pointer cursor, hover ring, clickable → opens `BookingModal`
- `OCCUPIED` spots: not clickable, no hover state

**Real-time updates:** WS `/topic/spots` messages filtered by `zoneId === selectedZoneId` update the spots array in place. Uses a ref to track current zone (same pattern as `DashboardPage`).

**Loading state:** skeleton/spinner while spot fetch is in flight.

---

## 5. Booking Modal

Triggered by clicking an available spot.

### Step 1 — Subscription check

On modal open, call `GET /api/subscriptions/my`:

| Result | Action |
|--------|--------|
| 200 (active subscription) | Skip to booking form; show "Covered by your subscription" label |
| 404 (no subscription) | Show two option cards (see below) |

**No-subscription option cards:**

- **"Subscribe"** — fetches `GET /api/packages`, lists packages (name, price, duration in days). User picks one → `POST /api/subscriptions { packageId }` → on success, proceed to booking form with subscription label.
- **"Pay per use — $1 / hr"** — proceed directly to booking form with per-use pricing.

### Step 2 — Booking form

- Spot identifier: "Spot R{row}–C{col} · {type}"
- Duration pills: **30 min** | **1 hr** | **2 hrs**
- Cost line:
  - Subscription → "Free"
  - Pay-per-use → "$0.50", "$1.00", "$2.00"
- **"Confirm Booking"** button

**On confirm:** `POST /api/bookings { spotId, startTime: now, endTime: now + duration }`

**On success:**
- Close modal
- Show brief success toast
- Emit `onBooked` callback → triggers `BookingsPage` to refresh

### Error handling

| Error | Message |
|-------|---------|
| 409 Conflict | "Spot just got taken — please pick another" |
| Network/5xx | "Something went wrong. Try again." + retry button |

---

## 6. Bookings Page

### Data

- Fetch `GET /api/bookings/my` on mount
- Re-fetch when `onBooked` fires (refresh trigger state in `App`)
- Display newest-first

### Booking card fields

`BookingResponse` will be enriched (see Backend section) to include spot and zone details.

| Field | Display |
|-------|---------|
| Zone + spot | "INDOOR L1 · R2-C3" |
| Time range | "Today 14:00 – 16:00" |
| Duration + cost | "2 hrs · $2.00" or "Free (subscription)" |
| Status | Badge: APPROVED (green), EXPIRED (grey), CANCELLED (red) |

### Actions

- APPROVED bookings show **"Cancel"** → `PATCH /api/bookings/{id}/cancel` → updates card status inline

---

## 7. Backend Changes

### 7a. Enrich `BookingResponse`

Add spot/zone context and payment info so `BookingsPage` renders correctly without extra calls:

```java
// BookingResponse.java (record)
public record BookingResponse(
    Long id, Long spotId, Long userId,
    LocalDateTime startTime, LocalDateTime endTime,
    BookingStatus status,
    // spot + zone context (new):
    Long spotRow, Long spotCol, String spotType,
    Long zoneId, Long zoneLevel, String zoneType,
    // payment info (new):
    String paymentType,   // "SUBSCRIPTION" | "PAY_PER_USE"
    Long costCents        // 0 for subscription; 50/100/200 for pay-per-use
) {}
```

`BookingService.toResponse()` joins through `spot.getZone_id()` to populate spot/zone fields.

### 7b. Extend `BookingRequest`

Frontend sends `paymentType` so backend records it:

```java
// BookingRequest.java
@NotNull private Long spotId;
@NotNull private LocalDateTime startTime;
@NotNull @Future private LocalDateTime endTime;
@NotNull private String paymentType; // "SUBSCRIPTION" | "PAY_PER_USE"
```

Backend derives `costCents` from `paymentType` + duration (50 cents/30 min, 100 cents/hr).

`BookingsEntity` needs two new columns: `payment_type VARCHAR` and `cost_cents BIGINT`. Spring Boot with `ddl-auto=update` will add these automatically; no migration script needed for dev.

### 7c. No other backend changes required

All needed endpoints already exist:
- `GET /api/zones` — zone list
- `GET /api/zones/{id}/spots` — spot list per zone
- `GET /api/spots/dashboard` — aggregate availability
- `GET /api/subscriptions/my` — active subscription (404 if none)
- `POST /api/subscriptions` — create subscription
- `GET /api/packages` — available packages
- `POST /api/bookings` — create booking
- `GET /api/bookings/my` — user's bookings
- `PATCH /api/bookings/{id}/cancel` — cancel booking
- WS `/topic/spots` + `/topic/dashboard` — real-time broadcasts (unchanged)

---

## 8. New Frontend Files

| File | Purpose |
|------|---------|
| `src/pages/ParkingZonePage.jsx` | Zone picker + spot grid (two-view component) |
| `src/pages/BookingsPage.jsx` | User booking list + cancel |
| `src/components/parking/ZoneCard.jsx` | Single zone card |
| `src/components/parking/BookingModal.jsx` | Booking flow (subscription check + form) |
| `src/api/bookings.js` | `createBooking`, `getMyBookings`, `cancelBooking` |
| `src/api/subscriptions.js` | `getMySubscription`, `subscribe`, `getPackages` |

### Modified files

| File | Change |
|------|--------|
| `src/App.jsx` | Role-based routing; pass `bookingRefreshKey` to BookingsPage |
| `src/components/layout/TopNav.jsx` | Normal-user nav tabs (parking, bookings) |
| `src/components/dashboard/SpotGrid.jsx` | Accept `onSpotClick` prop; add pointer/hover on AVAILABLE |
| `src/api/dashboard.js` | No change (getZones, getDashboard, getSpots reused) |

---

## 9. Data Flow Summary

```
Login (role=USERS)
  └─ App renders ParkingZonePage
       ├─ WS connects → /topic/spots + /topic/dashboard
       ├─ fetch /api/zones + /api/spots/dashboard
       ├─ [View 1] ZoneCard grid → click → selectedZoneId set
       │     └─ fetch /api/zones/{id}/spots
       └─ [View 2] SpotGrid → click AVAILABLE spot
             └─ BookingModal opens
                  ├─ GET /api/subscriptions/my
                  │    ├─ 200 → booking form (free)
                  │    └─ 404 → option cards
                  │         ├─ Subscribe → GET /api/packages
                  │         │              POST /api/subscriptions
                  │         │              → booking form (free)
                  │         └─ Pay per use → booking form ($1/hr)
                  └─ POST /api/bookings → success → onBooked()
                                                        └─ BookingsPage re-fetches
```

---

## 10. Out of Scope

- Real payment (Stripe) — confirm button is a mock; architecture is extensible
- Admin booking management — separate feature
- Push notifications for booking expiry
- Map view of zones (lat/lng exists in DB but not in ZoneResponse)
