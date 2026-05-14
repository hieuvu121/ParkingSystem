import { apiFetch } from './client';

export async function getMe() {
  const res = await apiFetch('/api/users/me');
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load profile' };
  return data;
}

export async function updateMe(fullName) {
  const res = await apiFetch('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify({ fullName }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to update profile' };
  return data;
}

export async function listUsers(page = 0, size = 20) {
  const res = await apiFetch(`/api/admin/users?page=${page}&size=${size}`);
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to load users' };
  return data;
}

export async function changeRole(id, role) {
  const res = await apiFetch(`/api/admin/users/${id}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Failed to change role' };
  return data;
}

export async function deleteUser(id) {
  const res = await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, message: data.message ?? 'Failed to delete user' };
  }
}
