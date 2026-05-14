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
    let ignore = false;
    if (!selectedZoneId) return () => { ignore = true; };
    setError('');
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
        if (ignore) return;
        setSpots(spotData);
        setPredictions(predData);
      })
      .catch((err) => {
        if (ignore) return;
        setError(err.message ?? 'Failed to load zone data');
      })
      .finally(() => {
        if (!ignore) setLoadingZone(false);
      });
    return () => { ignore = true; };
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
