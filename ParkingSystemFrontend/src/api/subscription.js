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
