import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { listUsers, changeRole, deleteUser } from '../api/user';
import {
  adminGetUserSubscription,
  adminAddUserSubscription,
  adminChangeUserSubscription,
  adminRemoveUserSubscription,
  getPackages,
} from '../api/subscription';

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
}

function ConfirmDialog({ message, onConfirm, onCancel, loading }) {
  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-[#1C1C1E] rounded-2xl w-full max-w-xs p-6 flex flex-col gap-5">
        <p className="text-white text-sm text-center">{message}</p>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="flex-1 py-2 rounded-xl bg-[#2C2C2E] text-white text-sm font-semibold hover:bg-[#3C3C3E] transition disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 py-2 rounded-xl bg-[#EF4444] text-white text-sm font-semibold hover:opacity-90 transition disabled:opacity-50"
          >
            {loading ? 'Working…' : 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
}

function SubscriptionModal({ user, onClose }) {
  const [sub, setSub] = useState(undefined); // undefined = loading, null = none
  const [packages, setPackages] = useState([]);
  const [selectedPkgId, setSelectedPkgId] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(null); // { type: 'add'|'change'|'remove', pkgId? }

  useEffect(() => {
    Promise.all([
      adminGetUserSubscription(user.id),
      getPackages(),
    ]).then(([s, pkgs]) => {
      setSub(s);
      setPackages(pkgs);
      if (pkgs.length > 0) setSelectedPkgId(String(pkgs[0].id));
    }).catch((e) => setError(e.message ?? 'Failed to load data'));
  }, [user.id]);

  async function execConfirm() {
    setBusy(true);
    setError('');
    try {
      if (confirm.type === 'remove') {
        await adminRemoveUserSubscription(user.id);
        setSub(null);
      } else if (confirm.type === 'add') {
        const updated = await adminAddUserSubscription(user.id, confirm.pkgId);
        setSub(updated);
      } else if (confirm.type === 'change') {
        const updated = await adminChangeUserSubscription(user.id, confirm.pkgId);
        setSub(updated);
      }
      setConfirm(null);
    } catch (e) {
      setError(e.message ?? 'Operation failed');
      setConfirm(null);
    } finally {
      setBusy(false);
    }
  }

  const confirmMessage = confirm?.type === 'remove'
    ? `Remove ${user.fullName}'s subscription? They will lose access immediately.`
    : confirm?.type === 'add'
    ? `Add subscription to ${user.fullName}?`
    : `Change ${user.fullName}'s subscription to "${packages.find((p) => p.id === confirm?.pkgId)?.name}"? End date will reset.`;

  return (
    <>
      <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-40 p-4">
        <div className="bg-[#1C1C1E] rounded-2xl w-full max-w-sm p-6 flex flex-col gap-5">
          <div className="flex items-center justify-between">
            <h2 className="text-white font-bold text-lg">Subscription</h2>
            <button type="button" onClick={onClose} className="text-[#A1A1AA] hover:text-white text-xl leading-none">✕</button>
          </div>

          <p className="text-[#A1A1AA] text-sm">{user.fullName} · {user.email}</p>

          {sub === undefined && <p className="text-[#A1A1AA] text-sm text-center py-4">Loading…</p>}

          {sub !== undefined && (
            <>
              {/* Current subscription */}
              {sub ? (
                <div className="bg-[#2C2C2E] rounded-xl p-4 flex flex-col gap-1">
                  <p className="text-white font-semibold text-sm">{sub.packageName}</p>
                  <p className="text-[#A1A1AA] text-xs">
                    ${(sub.price / 100).toFixed(2)} · Expires {formatDate(sub.endDate)}
                  </p>
                </div>
              ) : (
                <p className="text-[#A1A1AA] text-sm">No active subscription.</p>
              )}

              {/* Package picker */}
              <div className="flex flex-col gap-2">
                <label className="text-[#A1A1AA] text-xs uppercase tracking-widest">
                  {sub ? 'Change to' : 'Assign package'}
                </label>
                <select
                  value={selectedPkgId}
                  onChange={(e) => setSelectedPkgId(e.target.value)}
                  className="w-full bg-[#2C2C2E] text-white text-sm rounded-xl px-3 py-2 outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
                >
                  {packages.map((pkg) => (
                    <option key={pkg.id} value={pkg.id}>
                      {pkg.name} — ${(pkg.price / 100).toFixed(2)} / {pkg.durations}d
                    </option>
                  ))}
                </select>

                <button
                  type="button"
                  disabled={!selectedPkgId || busy}
                  onClick={() => setConfirm({ type: sub ? 'change' : 'add', pkgId: Number(selectedPkgId) })}
                  className="w-full py-2.5 rounded-xl bg-[#F5D26B] text-black font-semibold text-sm hover:opacity-90 transition disabled:opacity-40"
                >
                  {sub ? 'Change Subscription' : 'Add Subscription'}
                </button>
              </div>

              {/* Remove */}
              {sub && (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setConfirm({ type: 'remove' })}
                  className="w-full py-2.5 rounded-xl bg-transparent border border-[#EF4444] text-[#EF4444] font-semibold text-sm hover:bg-[#EF4444]/10 transition disabled:opacity-40"
                >
                  Remove Subscription
                </button>
              )}

              {error && <p className="text-red-400 text-xs text-center">{error}</p>}
            </>
          )}
        </div>
      </div>

      {confirm && (
        <ConfirmDialog
          message={confirmMessage}
          loading={busy}
          onConfirm={execConfirm}
          onCancel={() => setConfirm(null)}
        />
      )}
    </>
  );
}

export default function AdminUsersPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirm, setConfirm] = useState(null); // { type: 'role'|'delete', user, newRole? }
  const [busy, setBusy] = useState(false);
  const [subUser, setSubUser] = useState(null); // user whose subscription modal is open

  useEffect(() => {
    listUsers(0, 20)
      .then((data) => setUsers(data.content ?? data))
      .catch((err) => setError(err.message ?? 'Failed to load users'))
      .finally(() => setLoading(false));
  }, []);

  async function execConfirm() {
    setBusy(true);
    try {
      if (confirm.type === 'role') {
        const updated = await changeRole(confirm.user.id, confirm.newRole);
        setUsers((prev) => prev.map((u) => u.id === confirm.user.id ? updated : u));
      } else if (confirm.type === 'delete') {
        await deleteUser(confirm.user.id);
        setUsers((prev) => prev.filter((u) => u.id !== confirm.user.id));
      }
      setConfirm(null);
    } catch (err) {
      setError(err.message ?? 'Operation failed');
      setConfirm(null);
    } finally {
      setBusy(false);
    }
  }

  const confirmMessage = confirm?.type === 'delete'
    ? `Delete ${confirm.user.fullName}? This cannot be undone.`
    : `Change ${confirm?.user.fullName}'s role to ${confirm?.newRole}?`;

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
                      u.role === 'ADMIN' ? 'bg-[#F5D26B] text-black' : 'bg-[#2C2C2E] text-white'
                    }`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <span className="flex items-center justify-end gap-3">
                      {u.role !== 'ADMIN' && (
                        <button
                          type="button"
                          onClick={() => setSubUser(u)}
                          className="text-xs text-[#A1A1AA] font-semibold hover:text-white transition"
                        >
                          Subscription
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setConfirm({ type: 'role', user: u, newRole: u.role === 'ADMIN' ? 'USERS' : 'ADMIN' })}
                        disabled={u.id === me?.id}
                        className="text-xs text-[#F5D26B] font-semibold hover:text-yellow-300 disabled:opacity-30 disabled:cursor-not-allowed"
                      >
                        {u.role === 'ADMIN' ? '→ User' : '→ Admin'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirm({ type: 'delete', user: u })}
                        disabled={u.id === me?.id}
                        className="text-xs text-red-400 font-semibold hover:text-red-300 disabled:opacity-30 disabled:cursor-not-allowed"
                      >
                        Delete
                      </button>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {confirm && (
        <ConfirmDialog
          message={confirmMessage}
          loading={busy}
          onConfirm={execConfirm}
          onCancel={() => setConfirm(null)}
        />
      )}

      {subUser && (
        <SubscriptionModal user={subUser} onClose={() => setSubUser(null)} />
      )}
    </div>
  );
}
