# Dashboard Page Design

**Date:** 2026-05-14
**Status:** Approved
**Scope:** Responsive parking dashboard — zone tabs, spot grid, capacity stats, prediction charts. Wired to backend. No router yet.

---

## Layout

### Desktop (≥768px)
```
┌─────────────────────────────────────────────────────┐
│  🅿 ParkingSystem   [Zone tabs scrollable]           │  ← header
├──────────────────────────────┬──────────────────────┤
│                              │  CapacityCard         │
│   SpotGrid (SVG)             │  DonutChart           │
│   color-coded spots          │  PredictionChart      │
│                              │  (chart.js)           │
└──────────────────────────────┴──────────────────────┘
```
Left column ~65%, right StatsPanel ~35%.

### Mobile (<768px)
Header → horizontal-scroll ZoneTabBar → SpotGrid (full width) → StatsPanel cards stacked vertically.

---

## Visual Tokens

| Token | Value |
|---|---|
| Page background | `#111111` (dark) |
| Panel / card background | `#1C1C1E` |
| Active zone tab | `#F5D26B` (golden), black text |
| Inactive zone tab | `#2C2C2E`, white text |
| Available spot | `#4ADE80` (green) |
| Occupied spot | `#EF4444` (red) |
| Chart accent | `#F5D26B` |
| Text primary | `#FFFFFF` |
| Text secondary | `#A1A1AA` |

---

## Zone Tab Availability Dots

| Availability | Dot color |
|---|---|
| > 50% available | green `#4ADE80` |
| 20–50% available | yellow `#FACC15` |
| < 20% available | red `#EF4444` |

**Zone label:** `{type} L{level}` e.g. "Indoor L1", "Outdoor L2"

---

## File Structure

| Action | Path | Responsibility |
|---|---|---|
| Create | `src/pages/DashboardPage.jsx` | Data fetching, selected zone state, layout shell |
| Create | `src/components/dashboard/ZoneTabBar.jsx` | Scrollable zone tabs with availability dots |
| Create | `src/components/dashboard/SpotGrid.jsx` | SVG grid of spots color-coded by status |
| Create | `src/components/dashboard/StatsPanel.jsx` | Wraps CapacityCard + DonutChart + PredictionChart |
| Create | `src/components/dashboard/CapacityCard.jsx` | Total / available / occupied numbers |
| Create | `src/components/dashboard/DonutChart.jsx` | % free ring chart (react-chartjs-2 Doughnut) |
| Create | `src/components/dashboard/PredictionChart.jsx` | Next-6-hour bar chart (react-chartjs-2 Bar) |
| Create | `src/api/dashboard.js` | getZones, getDashboard, getSpots, getPredictions |
| Modify | `src/App.jsx` | Toggle between AuthPage and DashboardPage on login |

---

## Component Contracts

### `DashboardPage`
- State: `zones[]`, `dashboard` (DashboardResponse), `selectedZoneId`, `spots[]`, `predictions[]`, `loading`, `error`
- On mount: fetch zones + dashboard summary in parallel
- On zone change: fetch spots + 6 prediction calls in parallel
- Passes data down as props — no prop drilling beyond one level

### `ZoneTabBar`
- Props: `zones[]`, `dashboard`, `selectedZoneId`, `onSelect(id)`
- Renders horizontally scrollable tabs
- Each tab shows `{type} L{level}` label + availability dot derived from dashboard.byZone

### `SpotGrid`
- Props: `spots[]`, `loading`
- Derives grid dimensions from max(row) × max(col)
- Renders SVG `<rect>` per spot, color by status
- Shows spinner when loading, "No spots" when empty

### `StatsPanel`
- Props: `zoneSummary` (ZoneSummary), `predictions[]`, `loading`
- Stacks CapacityCard, DonutChart, PredictionChart vertically

### `CapacityCard`
- Props: `total`, `available`, `occupied`
- Displays three stat rows with color-coded values

### `DonutChart`
- Props: `available`, `total`
- react-chartjs-2 Doughnut — golden segment = available %, dark = occupied %
- Centre label: "{pct}% Free"

### `PredictionChart`
- Props: `predictions[]` — array of `{ hour: string, probability: number }`
- react-chartjs-2 Bar — golden bars, x-axis = hour label, y-axis = 0–100%

### `api/dashboard.js`
```js
getZones()                          // GET /api/zones
getDashboard()                      // GET /api/spots/dashboard
getSpots(zoneId)                    // GET /api/zones/{zoneId}/spots
getPrediction(zoneId, targetTime)   // GET /api/predict/availability?zoneId&targetTime
```
All use `apiFetch` from `./client`. All throw `{ status, message }` on error.

---

## Data Flow

```
DashboardPage mounts
  → parallel: getZones() + getDashboard()
  → set zones, dashboard; select first zone

Zone selected
  → parallel: getSpots(zoneId) + getPrediction(zoneId, t) × 6
  → t = now, +1h, +2h, +3h, +4h, +5h (ISO format)
  → set spots, predictions

Loading states: spinner per section
Error states: inline message + retry button
```

---

## App.jsx Navigation

```jsx
const { token } = useAuth();
return token ? <DashboardPage /> : <AuthPage />;
```

`AuthContext.login()` sets `token` in context state → App re-renders → DashboardPage shows automatically. Page refresh preserves session because `token` is initialised from localStorage.

---

## Dependencies to Install

```bash
npm install chart.js react-chartjs-2
```

---

## Out of Scope

- React Router / protected routes
- Booking flow
- Admin panel
- WebSocket real-time updates
- Map with geographic coordinates
- Logout button (can be added later)
