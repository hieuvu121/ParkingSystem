import { apiFetch } from './client';

export async function getZones() {
  const res = await apiFetch('/api/zones');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load zones' };
  return data;
}

export async function createZone({ level, type }) {
  const res = await apiFetch('/api/zones', {
    method: 'POST',
    body: JSON.stringify({ level, type }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to create zone' };
  return data;
}

export async function updateZone(id, { level, type }) {
  const res = await apiFetch(`/api/zones/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ level, type }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to update zone' };
  return data;
}

export async function deleteZone(id) {
  const res = await apiFetch(`/api/zones/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to delete zone' };
  }
}

export async function getSpotsByZone(zoneId) {
  const res = await apiFetch(`/api/zones/${zoneId}/spots`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load spots' };
  return data;
}

export async function createSpot(zoneId, { row, col, type }) {
  const res = await apiFetch(`/api/zones/${zoneId}/spots`, {
    method: 'POST',
    body: JSON.stringify({ row, col, type }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to create spot' };
  return data;
}

export async function updateSpot(spotId, { row, col, type }) {
  const res = await apiFetch(`/api/spots/${spotId}`, {
    method: 'PUT',
    body: JSON.stringify({ row, col, type }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to update spot' };
  return data;
}

export async function deleteSpot(spotId) {
  const res = await apiFetch(`/api/spots/${spotId}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to delete spot' };
  }
}
