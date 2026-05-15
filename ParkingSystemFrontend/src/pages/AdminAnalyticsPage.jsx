import { useState, useEffect } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import { getPeakHours, getUtilization, getOccupancy } from '../api/analytics';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

const CHART_OPTIONS = {
  responsive: true,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: '#A1A1AA' }, grid: { color: '#2C2C2E' } },
    y: {
      ticks: { color: '#A1A1AA', callback: (v) => `${v}%` },
      grid: { color: '#2C2C2E' },
      beginAtZero: true,
      max: 100,
    },
  },
};

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function monthAgoStr() {
  const d = new Date();
  d.setMonth(d.getMonth() - 1);
  return d.toISOString().slice(0, 10);
}

export default function AdminAnalyticsPage() {
  const [peakHours, setPeakHours] = useState([]);
  const [utilization, setUtilization] = useState([]);
  const [occupancy, setOccupancy] = useState([]);
  const [from, setFrom] = useState(monthAgoStr());
  const [to, setTo] = useState(todayStr());
  const [peakLoading, setPeakLoading] = useState(true);
  const [rangeLoading, setRangeLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setPeakLoading(true);
    getPeakHours()
      .then(setPeakHours)
      .catch((e) => setError(e.message ?? 'Failed to load peak hours'))
      .finally(() => setPeakLoading(false));
  }, []);

  useEffect(() => {
    setRangeLoading(true);
    Promise.all([getUtilization(from, to), getOccupancy(from, to)])
      .then(([ut, occ]) => {
        setUtilization(ut);
        setOccupancy(occ);
      })
      .catch((e) => setError(e.message ?? 'Failed to load analytics'))
      .finally(() => setRangeLoading(false));
  }, [from, to]);

  const peakData = {
    labels: peakHours.map((h) => `${String(h.hour).padStart(2, '0')}:00`),
    datasets: [{
      data: peakHours.map((h) => Number(h.averageOccupancyPercent.toFixed(1))),
      backgroundColor: '#F5D26B',
      borderRadius: 6,
    }],
  };

  const utilData = {
    labels: utilization.map((u) => `Zone ${u.zoneId}`),
    datasets: [{
      data: utilization.map((u) => Number(u.utilizationPercent.toFixed(1))),
      backgroundColor: '#4ADE80',
      borderRadius: 6,
    }],
  };

  const occData = {
    labels: occupancy.map((o) => `Zone ${o.zoneId}`),
    datasets: [{
      data: occupancy.map((o) => Number(o.occupancyPercent.toFixed(1))),
      backgroundColor: '#60A5FA',
      borderRadius: 6,
    }],
  };

  return (
    <div className="min-h-screen bg-[#111111] text-white px-4 py-6">
      <div className="max-w-6xl mx-auto space-y-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h2 className="text-xl font-bold">Analytics</h2>
          <div className="flex gap-3 items-end">
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">From</label>
              <input
                type="date"
                value={from}
                max={to}
                onChange={(e) => setFrom(e.target.value)}
                className="bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-1.5 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">To</label>
              <input
                type="date"
                value={to}
                min={from}
                onChange={(e) => setTo(e.target.value)}
                className="bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-1.5 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
          </div>
        </div>

        {error && <p className="text-red-400 text-sm">{error}</p>}

        {/* Peak hours — all time, no date range */}
        <div className="bg-[#1C1C1E] rounded-2xl p-5">
          <h3 className="font-semibold mb-1">Peak Hours</h3>
          <p className="text-[#A1A1AA] text-xs mb-4">Average relative booking frequency by hour of day (all time)</p>
          {peakLoading ? (
            <div className="flex items-center justify-center h-32 text-[#A1A1AA]">Loading…</div>
          ) : peakHours.length === 0 ? (
            <p className="text-[#A1A1AA] text-sm text-center py-8">No data yet</p>
          ) : (
            <Bar data={peakData} options={CHART_OPTIONS} />
          )}
        </div>

        {rangeLoading ? (
          <div className="flex items-center justify-center h-48 text-[#A1A1AA]">Loading…</div>
        ) : (
          <>
            {/* Utilization */}
            <div className="bg-[#1C1C1E] rounded-2xl p-5">
              <h3 className="font-semibold mb-1">Zone Utilization</h3>
              <p className="text-[#A1A1AA] text-xs mb-4">
                % of available spot-hours that were booked in the selected period
              </p>
              {utilization.length === 0 ? (
                <p className="text-[#A1A1AA] text-sm text-center py-8">No data for this range</p>
              ) : (
                <>
                  <Bar data={utilData} options={CHART_OPTIONS} />
                  <div className="mt-5 overflow-x-auto">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="text-[#A1A1AA] text-left border-b border-[#2C2C2E]">
                          <th className="pb-2 pr-6">Zone</th>
                          <th className="pb-2 pr-6">Total Spots</th>
                          <th className="pb-2 pr-6">Booked Hours</th>
                          <th className="pb-2">Utilization</th>
                        </tr>
                      </thead>
                      <tbody>
                        {utilization.map((u) => (
                          <tr key={u.zoneId} className="border-b border-[#2C2C2E] last:border-0">
                            <td className="py-2 pr-6">Zone {u.zoneId}</td>
                            <td className="py-2 pr-6">{u.totalSpots}</td>
                            <td className="py-2 pr-6">{u.bookedHours.toFixed(1)} h</td>
                            <td className="py-2 font-medium">{u.utilizationPercent.toFixed(1)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>

            {/* Occupancy */}
            <div className="bg-[#1C1C1E] rounded-2xl p-5">
              <h3 className="font-semibold mb-1">Zone Occupancy</h3>
              <p className="text-[#A1A1AA] text-xs mb-4">
                % of available spot-hours that were occupied in the selected period
              </p>
              {occupancy.length === 0 ? (
                <p className="text-[#A1A1AA] text-sm text-center py-8">No data for this range</p>
              ) : (
                <Bar data={occData} options={CHART_OPTIONS} />
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
