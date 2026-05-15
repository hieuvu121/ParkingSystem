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
  PENDING:   'bg-[#1E3A8A] text-[#93C5FD]',
};

const TABS = [
  { key: 'ACTIVE', label: 'Active' },
  { key: 'EXPIRED', label: 'Expired' },
  { key: 'CANCELLED', label: 'Cancelled' },
];

export default function BookingsPage({ refreshKey }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(null);
  const [activeTab, setActiveTab] = useState('ACTIVE');
  const [sortOrder, setSortOrder] = useState('desc');
  const [cursor, setCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);

  async function loadBookings({ reset = false } = {}) {
    if (reset) {
      setLoading(true);
      setCursor(null);
      setHasNext(false);
    } else {
      setLoadingMore(true);
    }
    setError('');
    try {
      const data = await getMyBookings({
        status: activeTab,
        cursor: reset ? null : cursor,
        limit: 5,
        sort: sortOrder,
      });
      setBookings((prev) => reset ? data.data : [...prev, ...data.data]);
      setCursor(data.meta?.nextCursor ?? null);
      setHasNext(Boolean(data.meta?.hasNext));
    } catch (err) {
      setError(err.message ?? 'Failed to load bookings');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }

  useEffect(() => {
    loadBookings({ reset: true });
  }, [refreshKey, activeTab, sortOrder]);

  async function handleCancel(id) {
    setCancelling(id);
    try {
      await cancelBooking(id);
      setBookings((prev) => {
        if (activeTab === 'CANCELLED') {
          return prev.map((b) => b.id === id ? { ...b, status: 'CANCELLED' } : b);
        }
        return prev.filter((b) => b.id !== id);
      });
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
      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-6">
          <h1 className="text-2xl font-bold">My Bookings</h1>
          <div className="flex items-center gap-2 text-xs text-[#A1A1AA]">
            <span>Sort</span>
            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className="bg-[#1C1C1E] border border-[#2C2C2E] rounded-full px-3 py-1 text-white"
            >
              <option value="desc">Newest first</option>
              <option value="asc">Oldest first</option>
            </select>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 mb-6">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2 rounded-full text-sm font-semibold transition border ${
                activeTab === tab.key
                  ? 'bg-[#F5D26B] text-black border-[#F5D26B]'
                  : 'bg-[#1C1C1E] text-[#A1A1AA] border-[#2C2C2E] hover:text-white'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

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

              {b.status === 'APPROVED' && activeTab === 'ACTIVE' && (
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

        {hasNext && (
          <div className="mt-6 flex justify-center">
            <button
              type="button"
              disabled={loadingMore}
              onClick={() => loadBookings({ reset: false })}
              className="px-4 py-2 rounded-full text-sm font-semibold bg-[#2C2C2E] text-white hover:bg-[#3C3C3E] disabled:opacity-50"
            >
              {loadingMore ? 'Loading…' : 'Load more'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
