import { apiFetch } from './client';

export async function getRecommendation({ lat, lng } = {}) {
  const params = new URLSearchParams();
  if (lat != null) params.set('lat', String(lat));
  if (lng != null) params.set('lng', String(lng));

  const url = params.toString()
    ? `/api/recommend/spot?${params.toString()}`
    : '/api/recommend/spot';

  const res = await apiFetch(url);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load recommendation' };
  return data;
}

