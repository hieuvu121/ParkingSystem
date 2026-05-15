import { apiFetch } from './client';

export async function getPackages() {
  const res = await apiFetch('/api/packages');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load packages' };
  return data;
}

export async function createPackage({ name, description, durations, price }) {
  const res = await apiFetch('/api/packages', {
    method: 'POST',
    body: JSON.stringify({ name, description, durations, price }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to create package' };
  return data;
}

export async function updatePackage(id, { name, description, durations, price }) {
  const res = await apiFetch(`/api/packages/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, description, durations, price }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to update package' };
  return data;
}

export async function deletePackage(id) {
  const res = await apiFetch(`/api/packages/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to delete package' };
  }
}
