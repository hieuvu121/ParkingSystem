import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { listUsers, changeRole, deleteUser } from '../api/user';

export default function AdminUsersPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);

  useEffect(() => {
    listUsers(0, 20)
      .then((data) => setUsers(data.content ?? data))
      .catch((err) => setError(err.message ?? 'Failed to load users'))
      .finally(() => setLoading(false));
  }, []);

  async function handleChangeRole(id, currentRole) {
    const newRole = currentRole === 'ADMIN' ? 'USERS' : 'ADMIN';
    try {
      const updated = await changeRole(id, newRole);
      setUsers((prev) => prev.map((u) => u.id === id ? updated : u));
    } catch (err) {
      setError(err.message ?? 'Failed to change role');
    }
  }

  async function handleDelete(id) {
    try {
      await deleteUser(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
      setConfirmDelete(null);
    } catch (err) {
      setError(err.message ?? 'Failed to delete user');
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#111111] flex items-center justify-center text-white">
        Loading…
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white px-4 py-6">
      <div className="max-w-4xl mx-auto">
        <h2 className="text-xl font-semibold mb-4">
          User Management{' '}
          <span className="text-[#A1A1AA] text-sm font-normal">({users.length})</span>
        </h2>
        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}
        <div className="bg-[#1C1C1E] rounded-2xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#2C2C2E] text-[#A1A1AA] text-xs uppercase tracking-wider">
                <th className="text-left px-4 py-3">Name</th>
                <th className="text-left px-4 py-3 hidden sm:table-cell">Email</th>
                <th className="text-left px-4 py-3">Role</th>
                <th className="text-right px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-[#2C2C2E] last:border-0 hover:bg-[#2C2C2E] transition">
                  <td className="px-4 py-3 font-medium">{u.fullName}</td>
                  <td className="px-4 py-3 text-[#A1A1AA] hidden sm:table-cell">{u.email}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                      u.role === 'ADMIN'
                        ? 'bg-[#F5D26B] text-black'
                        : 'bg-[#2C2C2E] text-white'
                    }`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    {confirmDelete === u.id ? (
                      <span className="flex items-center justify-end gap-2">
                        <span className="text-[#A1A1AA] text-xs">Sure?</span>
                        <button
                          type="button"
                          onClick={() => handleDelete(u.id)}
                          className="text-xs text-red-400 font-semibold hover:text-red-300"
                        >
                          Yes
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmDelete(null)}
                          className="text-xs text-[#A1A1AA] hover:text-white"
                        >
                          No
                        </button>
                      </span>
                    ) : (
                      <span className="flex items-center justify-end gap-3">
                        <button
                          type="button"
                          onClick={() => handleChangeRole(u.id, u.role)}
                          disabled={u.id === me?.id}
                          className="text-xs text-[#F5D26B] font-semibold hover:text-yellow-300 disabled:opacity-30 disabled:cursor-not-allowed"
                        >
                          {u.role === 'ADMIN' ? '→ User' : '→ Admin'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmDelete(u.id)}
                          disabled={u.id === me?.id}
                          className="text-xs text-red-400 font-semibold hover:text-red-300 disabled:opacity-30 disabled:cursor-not-allowed"
                        >
                          Delete
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
