import { apiFetch } from './client';

export async function getMySubscription() {
  const res = await apiFetch('/api/subscriptions/my');
  if (res.status === 404) return null;
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to load subscription' };
  }
  return res.json();
}

export async function subscribe(packageId) {
  const res = await apiFetch('/api/subscriptions', {
    method: 'POST',
    body: JSON.stringify({ packageId }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to subscribe' };
  return data;
}

export async function getPackages() {
  const res = await apiFetch('/api/packages');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load packages' };
  return data;
}
