import { apiFetch } from './client';

export async function login(email, password) {
  const res = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Unknown error' };
  if (!data.token) throw { status: 200, message: 'Server did not return a token' };
  return data; // { token }
}

export async function register(fullName, email, password) {
  const res = await apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Unknown error' };
  return data; // { message }
}
