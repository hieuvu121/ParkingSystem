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
