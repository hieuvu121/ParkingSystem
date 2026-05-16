import { apiFetch } from './client';

export async function getPeakHours() {
  const res = await apiFetch('/api/analytics/peak-hours');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load peak hours' };
  return data;
}

export async function getUtilization(from, to) {
  const params = new URLSearchParams();
  if (from) params.set('from', `${from}T00:00:00`);
  if (to) params.set('to', `${to}T23:59:59`);
  const res = await apiFetch(`/api/analytics/utilization?${params}`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load utilization' };
  return data;
}

export async function getOccupancy(from, to) {
  const params = new URLSearchParams({ from: `${from}T00:00:00`, to: `${to}T23:59:59` });
  const res = await apiFetch(`/api/analytics/occupancy?${params}`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load occupancy' };
  return data;
}
