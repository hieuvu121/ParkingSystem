import { useState, useEffect } from 'react';
import { getAdminBookings } from '../api/bookings';

const STATUS_STYLE = {
  APPROVED:  'bg-[#14532d] text-[#4ADE80]',
  EXPIRED:   'bg-[#27272A] text-[#6B7280]',
  CANCELLED: 'bg-[#3B0000] text-[#EF4444]',
  PENDING:   'bg-[#1e3a5f] text-[#60A5FA]',
};

function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  const today = new Date();
  const time = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  return d.toDateString() === today.toDateString()
    ? `Today ${time}`
    : `${d.toLocaleDateString([], { month: 'short', day: 'numeric' })} ${time}`;
}

function formatCost(paymentType, costCents) {
  if (paymentType === 'SUBSCRIPTION') return 'Free';
  return `$${(costCents / 100).toFixed(2)}`;
}

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function daysAgoStr(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

const STATUS_OPTIONS = ['ALL', 'APPROVED', 'PENDING', 'EXPIRED', 'CANCELLED'];

export default function AdminBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [from, setFrom] = useState(daysAgoStr(30));
  const [to, setTo] = useState(todayStr());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    getAdminBookings(page, 15, { status: statusFilter, from, to })
      .then((data) => {
        setBookings(data.content ?? data);
        setTotalPages(data.totalPages ?? 1);
        setTotal(data.totalElements ?? (data.content ?? data).length);
      })
      .catch((e) => setError(e.message ?? 'Failed to load bookings'))
      .finally(() => setLoading(false));
  }, [page, statusFilter, from, to]);

  function handleStatusChange(e) {
    setStatusFilter(e.target.value);
    setPage(0);
  }

  function handleFromChange(e) {
    setFrom(e.target.value);
    setPage(0);
  }

  function handleToChange(e) {
    setTo(e.target.value);
    setPage(0);
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white px-4 py-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex flex-wrap items-end gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold">All Bookings</h2>
            <p className="text-[#A1A1AA] text-sm mt-0.5">{total} results</p>
          </div>

          <div className="flex flex-wrap gap-3 ml-auto items-end">
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">From</label>
              <input
                type="date"
                value={from}
                max={to}
                onChange={handleFromChange}
                className="bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-2 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">To</label>
              <input
                type="date"
                value={to}
                min={from}
                onChange={handleToChange}
                className="bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-2 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
            <select
              value={statusFilter}
              onChange={handleStatusChange}
              className="bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-2 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            >
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>

        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

        {loading ? (
          <div className="flex items-center justify-center h-48 text-[#A1A1AA]">Loading…</div>
        ) : (
          <>
            <div className="bg-[#1C1C1E] rounded-2xl overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-[#2C2C2E] text-[#A1A1AA] text-xs uppercase tracking-wider">
                    <th className="text-left px-4 py-3">User</th>
                    <th className="text-left px-4 py-3">Zone · Spot</th>
                    <th className="text-left px-4 py-3 hidden md:table-cell">Start</th>
                    <th className="text-left px-4 py-3 hidden md:table-cell">End</th>
                    <th className="text-left px-4 py-3">Payment</th>
                    <th className="text-left px-4 py-3">Cost</th>
                    <th className="text-left px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-8 text-center text-[#A1A1AA]">No bookings found.</td>
                    </tr>
                  )}
                  {bookings.map((b) => (
                    <tr key={b.id} className="border-b border-[#2C2C2E] last:border-0 hover:bg-[#2C2C2E] transition">
                      <td className="px-4 py-3 text-[#A1A1AA]">#{b.userId}</td>
                      <td className="px-4 py-3 font-medium">
                        <span className="block">{b.zoneType} L{b.zoneLevel}</span>
                        <span className="text-[#A1A1AA] text-xs">R{b.spotRow}–C{b.spotCol} · {b.spotType}</span>
                      </td>
                      <td className="px-4 py-3 text-[#A1A1AA] hidden md:table-cell">{formatDateTime(b.startTime)}</td>
                      <td className="px-4 py-3 text-[#A1A1AA] hidden md:table-cell">{formatDateTime(b.endTime)}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          b.paymentType === 'SUBSCRIPTION'
                            ? 'bg-[#1e3a5f] text-[#60A5FA]'
                            : 'bg-[#2C2C2E] text-[#A1A1AA]'
                        }`}>
                          {b.paymentType === 'SUBSCRIPTION' ? 'Sub' : 'Pay/use'}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-semibold">{formatCost(b.paymentType, b.costCents)}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_STYLE[b.status] ?? STATUS_STYLE.EXPIRED}`}>
                          {b.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-4 mt-4">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 rounded-lg bg-[#2C2C2E] text-sm disabled:opacity-40 hover:bg-[#3C3C3E] transition"
                >
                  ← Prev
                </button>
                <span className="text-[#A1A1AA] text-sm">Page {page + 1} of {totalPages}</span>
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-4 py-2 rounded-lg bg-[#2C2C2E] text-sm disabled:opacity-40 hover:bg-[#3C3C3E] transition"
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
