# Dashboard Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a responsive parking dashboard with zone tabs, SVG spot grid, capacity stats, donut chart, and hourly prediction bar chart — fully wired to the Spring Boot backend.

**Architecture:** `DashboardPage` fetches all data and owns state; child components receive props only. `api/dashboard.js` handles all HTTP calls via the existing `apiFetch` wrapper. `App.jsx` reads `token` from `AuthContext` to toggle between `AuthPage` and `DashboardPage`.

**Tech Stack:** React 18, Vite 5, Tailwind CSS 3, chart.js + react-chartjs-2 (donut + bar charts), no router.

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Install | `chart.js react-chartjs-2` | Chart rendering |
| Create | `src/api/dashboard.js` | getZones, getDashboard, getSpots, getPrediction |
| Modify | `src/App.jsx` | Toggle AuthPage ↔ DashboardPage via token |
| Create | `src/pages/DashboardPage.jsx` | Data fetching, state, layout shell |
| Create | `src/components/dashboard/ZoneTabBar.jsx` | Scrollable zone tabs + availability dots |
| Create | `src/components/dashboard/SpotGrid.jsx` | SVG grid of spots colored by status |
| Create | `src/components/dashboard/CapacityCard.jsx` | Total / available / occupied numbers |
| Create | `src/components/dashboard/DonutChart.jsx` | % free doughnut (react-chartjs-2) |
| Create | `src/components/dashboard/PredictionChart.jsx` | Next-6h bar chart (react-chartjs-2) |
| Create | `src/components/dashboard/StatsPanel.jsx` | Wraps CapacityCard + DonutChart + PredictionChart |

---

### Task 1: Install chart.js dependencies

**Files:**
- Modify: `package.json` (via npm install)

- [ ] **Step 1: Install packages**

```bash
cd /Users/mac/Documents/Project/ParkingSystemApp/ParkingSystemFrontend
npm install chart.js react-chartjs-2
```
Expected: packages added to `node_modules`, `package.json` updated.

- [ ] **Step 2: Verify install**

```bash
node -e "require('./node_modules/chart.js/package.json'); console.log('ok')"
```
Expected: `ok`

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
git commit -m "feat: install chart.js and react-chartjs-2"
```

---

### Task 2: api/dashboard.js

**Files:**
- Create: `src/api/dashboard.js`

- [ ] **Step 1: Create the file**

```js
import { apiFetch } from './client';

export async function getZones() {
  const res = await apiFetch('/api/zones');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load zones' };
  return data; // ZoneResponse[]  { id, level, type }
}

export async function getDashboard() {
  const res = await apiFetch('/api/spots/dashboard');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load dashboard' };
  return data; // { total, available, occupied, byZone: [{ zoneId, total, available, occupied }] }
}

export async function getSpots(zoneId) {
  const res = await apiFetch(`/api/zones/${zoneId}/spots`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load spots' };
  return data; // SpotResponse[]  { id, row, col, type, status, zoneId }
}

export async function getPrediction(zoneId, targetTime) {
  const res = await apiFetch(
    `/api/predict/availability?zoneId=${zoneId}&targetTime=${encodeURIComponent(targetTime)}`
  );
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load prediction' };
  return data; // { zoneId, targetTime, availabilityProbability }
}
```

- [ ] **Step 2: Verify file exists**

```bash
ls src/api/dashboard.js
```
Expected: file listed.

- [ ] **Step 3: Commit**

```bash
git add src/api/dashboard.js
git commit -m "feat: add dashboard API module"
```

---

### Task 3: Update App.jsx

**Files:**
- Modify: `src/App.jsx`

- [ ] **Step 1: Replace App.jsx**

```jsx
import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';

export default function App() {
  const { token } = useAuth();
  return token ? <DashboardPage /> : <AuthPage />;
}
```

- [ ] **Step 2: Verify file saved**

```bash
cat src/App.jsx
```
Expected: shows the three-line component above.

- [ ] **Step 3: Commit**

```bash
git add src/App.jsx
git commit -m "feat: toggle AuthPage/DashboardPage based on auth token"
```

---

### Task 4: ZoneTabBar

**Files:**
- Create: `src/components/dashboard/ZoneTabBar.jsx`

- [ ] **Step 1: Create the file**

```jsx
function dotColor(available, total) {
  if (total === 0) return '#6B7280';
  const pct = available / total;
  if (pct > 0.5) return '#4ADE80';
  if (pct > 0.2) return '#FACC15';
  return '#EF4444';
}

export default function ZoneTabBar({ zones, dashboard, selectedZoneId, onSelect }) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide px-4">
      {zones.map((zone) => {
        const summary = dashboard?.byZone?.find((z) => z.zoneId === zone.id);
        const color = summary ? dotColor(summary.available, summary.total) : '#6B7280';
        const active = zone.id === selectedZoneId;
        return (
          <button
            key={zone.id}
            onClick={() => onSelect(zone.id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold whitespace-nowrap transition ${
              active
                ? 'bg-[#F5D26B] text-black'
                : 'bg-[#2C2C2E] text-white hover:bg-[#3C3C3E]'
            }`}
          >
            <span
              className="w-2 h-2 rounded-full flex-shrink-0"
              style={{ backgroundColor: color }}
            />
            {zone.type} L{zone.level}
          </button>
        );
      })}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/ZoneTabBar.jsx
git commit -m "feat: add ZoneTabBar with availability dot indicators"
```

---

### Task 5: SpotGrid

**Files:**
- Create: `src/components/dashboard/SpotGrid.jsx`

- [ ] **Step 1: Create the file**

```jsx
const SPOT_SIZE = 28;
const GAP = 6;

export default function SpotGrid({ spots, loading }) {
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
          const fill = spot.status === 'AVAILABLE' ? '#4ADE80' : '#EF4444';
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

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/SpotGrid.jsx
git commit -m "feat: add SVG SpotGrid with availability color coding"
```

---

### Task 6: CapacityCard

**Files:**
- Create: `src/components/dashboard/CapacityCard.jsx`

- [ ] **Step 1: Create the file**

```jsx
export default function CapacityCard({ total, available, occupied }) {
  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">Capacity</h3>
      <div className="space-y-2">
        <div className="flex justify-between items-center">
          <span className="text-white text-sm">Total</span>
          <span className="text-white font-bold">{total}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-sm text-[#4ADE80]">Available</span>
          <span className="text-[#4ADE80] font-bold">{available}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-sm text-[#EF4444]">Occupied</span>
          <span className="text-[#EF4444] font-bold">{occupied}</span>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/CapacityCard.jsx
git commit -m "feat: add CapacityCard component"
```

---

### Task 7: DonutChart

**Files:**
- Create: `src/components/dashboard/DonutChart.jsx`

- [ ] **Step 1: Create the file**

```jsx
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip } from 'chart.js';

ChartJS.register(ArcElement, Tooltip);

export default function DonutChart({ available, total }) {
  const pct = total > 0 ? Math.round((available / total) * 100) : 0;
  const occupied = total - available;

  const data = {
    datasets: [
      {
        data: [available, occupied],
        backgroundColor: ['#F5D26B', '#2C2C2E'],
        borderWidth: 0,
        cutout: '72%',
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: { tooltip: { enabled: false } },
  };

  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">Availability</h3>
      <div className="relative w-36 h-36 mx-auto">
        <Doughnut data={data} options={options} />
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-white font-bold text-lg">{pct}% Free</span>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/DonutChart.jsx
git commit -m "feat: add DonutChart with % free indicator"
```

---

### Task 8: PredictionChart

**Files:**
- Create: `src/components/dashboard/PredictionChart.jsx`

- [ ] **Step 1: Create the file**

```jsx
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip);

export default function PredictionChart({ predictions }) {
  if (!predictions || predictions.length === 0) {
    return (
      <div className="bg-[#1C1C1E] rounded-2xl p-4">
        <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">
          Predicted Availability
        </h3>
        <div className="flex items-center justify-center h-24 text-[#A1A1AA] text-sm">
          No prediction data
        </div>
      </div>
    );
  }

  const data = {
    labels: predictions.map((p) => p.hour),
    datasets: [
      {
        data: predictions.map((p) => Math.round(p.probability * 100)),
        backgroundColor: '#F5D26B',
        borderRadius: 4,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: { color: '#A1A1AA', font: { size: 11 } },
        grid: { display: false },
        border: { display: false },
      },
      y: {
        min: 0,
        max: 100,
        ticks: { color: '#A1A1AA', font: { size: 11 }, callback: (v) => `${v}%` },
        grid: { color: '#2C2C2E' },
        border: { display: false },
      },
    },
    plugins: {
      tooltip: {
        callbacks: { label: (ctx) => `${ctx.raw}% available` },
      },
    },
  };

  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">
        Predicted Availability
      </h3>
      <div className="h-36">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/PredictionChart.jsx
git commit -m "feat: add PredictionChart bar chart for hourly predictions"
```

---

### Task 9: StatsPanel

**Files:**
- Create: `src/components/dashboard/StatsPanel.jsx`

- [ ] **Step 1: Create the file**

```jsx
import CapacityCard from './CapacityCard';
import DonutChart from './DonutChart';
import PredictionChart from './PredictionChart';

export default function StatsPanel({ zoneSummary, predictions, loading }) {
  const total = zoneSummary?.total ?? 0;
  const available = zoneSummary?.available ?? 0;
  const occupied = zoneSummary?.occupied ?? 0;

  if (loading) {
    return (
      <div className="flex flex-col gap-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-[#1C1C1E] rounded-2xl p-4 h-24 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <CapacityCard total={total} available={available} occupied={occupied} />
      <DonutChart available={available} total={total} />
      <PredictionChart predictions={predictions} />
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/dashboard/StatsPanel.jsx
git commit -m "feat: add StatsPanel wrapper with loading skeleton"
```

---

### Task 10: DashboardPage

**Files:**
- Create: `src/pages/DashboardPage.jsx`

- [ ] **Step 1: Create the file**

```jsx
import { useEffect, useState } from 'react';
import { getZones, getDashboard, getSpots, getPrediction } from '../api/dashboard';
import ZoneTabBar from '../components/dashboard/ZoneTabBar';
import SpotGrid from '../components/dashboard/SpotGrid';
import StatsPanel from '../components/dashboard/StatsPanel';

function pad(n) { return String(n).padStart(2, '0'); }
function toLocalISO(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:00:00`;
}

function buildPredictionTimes() {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  return Array.from({ length: 6 }, (_, i) => {
    const t = new Date(now);
    t.setHours(t.getHours() + i);
    const hour = t.getHours();
    const label = `${hour % 12 || 12} ${hour < 12 ? 'AM' : 'PM'}`;
    return { iso: toLocalISO(t), label };
  });
}

export default function DashboardPage() {
  const [zones, setZones] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [selectedZoneId, setSelectedZoneId] = useState(null);
  const [spots, setSpots] = useState([]);
  const [predictions, setPredictions] = useState([]);
  const [loadingInit, setLoadingInit] = useState(true);
  const [loadingZone, setLoadingZone] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getZones(), getDashboard()])
      .then(([z, d]) => {
        setZones(z);
        setDashboard(d);
        if (z.length > 0) setSelectedZoneId(z[0].id);
      })
      .catch((err) => setError(err.message ?? 'Failed to load dashboard'))
      .finally(() => setLoadingInit(false));
  }, []);

  useEffect(() => {
    if (!selectedZoneId) return;
    setLoadingZone(true);
    setSpots([]);
    setPredictions([]);
    const times = buildPredictionTimes();
    Promise.all([
      getSpots(selectedZoneId),
      ...times.map((t) => getPrediction(selectedZoneId, t.iso).then((r) => ({
        hour: t.label,
        probability: r.availabilityProbability,
      }))),
    ])
      .then(([spotData, ...predData]) => {
        setSpots(spotData);
        setPredictions(predData);
      })
      .catch((err) => setError(err.message ?? 'Failed to load zone data'))
      .finally(() => setLoadingZone(false));
  }, [selectedZoneId]);

  const zoneSummary = dashboard?.byZone?.find((z) => z.zoneId === selectedZoneId);

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
      {/* Header */}
      <header className="px-4 py-4 flex items-center gap-3 border-b border-[#2C2C2E]">
        <span className="text-[#F5D26B] text-xl font-bold">🅿</span>
        <h1 className="text-white font-semibold text-lg">Parking System</h1>
      </header>

      {/* Zone tab bar */}
      <div className="py-3 border-b border-[#2C2C2E]">
        <ZoneTabBar
          zones={zones}
          dashboard={dashboard}
          selectedZoneId={selectedZoneId}
          onSelect={setSelectedZoneId}
        />
      </div>

      {/* Main content — responsive two-column on desktop */}
      <div className="max-w-6xl mx-auto px-4 py-6 flex flex-col md:flex-row gap-6">
        {/* Left: spot grid */}
        <div className="flex-1 bg-[#1C1C1E] rounded-2xl min-h-64">
          <div className="px-4 pt-4 pb-2">
            <h2 className="text-sm text-[#A1A1AA] uppercase tracking-widest">Spot Map</h2>
          </div>
          <SpotGrid spots={spots} loading={loadingZone} />
        </div>

        {/* Right: stats panel */}
        <div className="w-full md:w-72 lg:w-80 flex-shrink-0">
          <StatsPanel
            zoneSummary={zoneSummary}
            predictions={predictions}
            loading={loadingZone}
          />
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/DashboardPage.jsx
git commit -m "feat: add DashboardPage with zone tabs, spot grid, and stats panel"
```

---

### Task 11: Visual smoke test

- [ ] **Step 1: Start the dev server**

```bash
npm run dev
```
Expected: Vite starts, URL printed (usually `http://localhost:5173`).

- [ ] **Step 2: Sign in**

Open the URL. Sign in with a verified account. Expected: page switches to the dashboard (dark background, header, zone tabs).

- [ ] **Step 3: Verify zone tabs**

Expected: zone tabs appear with availability dots. Dots are green/yellow/red based on availability. Clicking a tab changes the active (golden) tab.

- [ ] **Step 4: Verify spot grid**

Expected: SVG grid of colored rectangles renders. Green = available, red = occupied. Legend shows below grid. Hovering a spot shows a tooltip with its status.

- [ ] **Step 5: Verify stats panel**

Expected: Capacity card shows total/available/occupied. Donut chart shows % free in golden. Prediction chart shows 6 bars for upcoming hours.

- [ ] **Step 6: Verify responsive layout**

Resize browser to mobile width (<768px). Expected: stats panel stacks below the spot grid (single column). Zone tabs scroll horizontally.

- [ ] **Step 7: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: dashboard smoke test adjustments"
```
