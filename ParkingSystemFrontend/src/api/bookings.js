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

export async function getMyBookings({ status, cursor, limit = 10, sort = 'desc' } = {}) {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (cursor) params.set('cursor', cursor);
  if (limit) params.set('limit', String(limit));
  if (sort) params.set('sort', sort);

  const res = await apiFetch(`/api/bookings/my?${params.toString()}`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load bookings' };
  return data;
}

export async function getAdminBookings(page = 0, size = 15, { status, from, to } = {}) {
  const params = new URLSearchParams({ page, size });
  if (status && status !== 'ALL') params.set('status', status);
  if (from) params.set('from', `${from}T00:00:00`);
  if (to) params.set('to', `${to}T23:59:59`);
  const res = await apiFetch(`/api/admin/bookings?${params}`);
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
