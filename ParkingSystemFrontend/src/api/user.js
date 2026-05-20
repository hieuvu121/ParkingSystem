import { apiFetch } from './client';

async function parseError(res, fallback) {
  const data = await res.json().catch(() => ({}));
  throw { status: res.status, message: data.message ?? fallback };
}

export async function getMe() {
  const res = await apiFetch('/api/users/me');
  if (!res.ok) await parseError(res, 'Failed to load profile');
  return res.json();
}

export async function updateMe(fullName) {
  const res = await apiFetch('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify({ fullName }),
  });
  if (!res.ok) await parseError(res, 'Failed to update profile');
  return res.json();
}

export async function updateMyPlate(plateNumber) {
  const res = await apiFetch('/api/users/me/plate', {
    method: 'PATCH',
    body: JSON.stringify({ plateNumber }),
  });
  if (!res.ok) await parseError(res, 'Failed to update plate number');
  return res.json();
}

export async function listUsers(page = 0, size = 20) {
  const params = new URLSearchParams({ page, size });
  const res = await apiFetch(`/api/admin/users?${params}`);
  if (!res.ok) await parseError(res, 'Failed to load users');
  return res.json();
}

export async function changeRole(id, role) {
  const res = await apiFetch(`/api/admin/users/${id}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  });
  if (!res.ok) await parseError(res, 'Failed to change role');
  return res.json();
}

export async function deleteUser(id) {
  const res = await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
  if (!res.ok) await parseError(res, 'Failed to delete user');
}
