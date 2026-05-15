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
