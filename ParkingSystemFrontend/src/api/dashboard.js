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
