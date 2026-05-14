import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { updateMe } from '../api/user';
import { getMySubscription } from '../api/subscription';

export default function AccountPage() {
  const { user, logout, updateUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [nameInput, setNameInput] = useState(user?.fullName ?? '');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [subscription, setSubscription] = useState(undefined);
  const [subError, setSubError] = useState('');

  useEffect(() => {
    getMySubscription()
      .then(setSubscription)
      .catch((err) => setSubError(err.message ?? 'Failed to load subscription'));
  }, []);

  async function handleSave() {
    if (saving) return;
    setSaving(true);
    setSaveError('');
    try {
      const updated = await updateMe(nameInput.trim());
      updateUser(updated);
      setEditing(false);
    } catch (err) {
      setSaveError(err.message ?? 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#111111] flex justify-center px-4 py-8">
      <div className="w-full max-w-md bg-white rounded-2xl p-6 flex flex-col gap-4">
        <h2 className="text-xl font-bold text-black">Account</h2>

        {/* Profile */}
        <div className="border border-gray-200 rounded-xl p-4 flex flex-col gap-2">
          <p className="font-semibold text-black">Profile</p>
          {!editing ? (
            <>
              <p className="text-gray-700">{user?.fullName}</p>
              <p className="text-gray-500 text-sm">{user?.email}</p>
              <button
                type="button"
                onClick={() => { setEditing(true); setNameInput(user?.fullName ?? ''); }}
                className="text-sm text-[#B89B3E] font-semibold self-start hover:underline"
              >
                Edit name
              </button>
            </>
          ) : (
            <>
              <input
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-[#F5D26B]"
              />
              {saveError && <p className="text-red-500 text-xs">{saveError}</p>}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={saving || !nameInput.trim()}
                  className="text-sm bg-[#F5D26B] px-4 py-1.5 rounded-full font-semibold disabled:opacity-50"
                >
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditing(false)}
                  className="text-sm text-gray-500 px-4 py-1.5"
                >
                  Cancel
                </button>
              </div>
            </>
          )}
        </div>

        {/* Preferred centre */}
        <div className="border border-gray-200 rounded-xl p-4">
          <p className="font-semibold text-black">Preferred centre</p>
          <p className="text-gray-400 text-sm mt-1">Coming soon</p>
        </div>

        {/* Notifications */}
        <div className="border border-gray-200 rounded-xl p-4">
          <p className="font-semibold text-black">Notifications</p>
          <p className="text-gray-400 text-sm mt-1">Coming soon</p>
        </div>

        {/* Subscription */}
        <div className="border border-gray-200 rounded-xl p-4">
          <p className="font-semibold text-black">Subscription</p>
          {subscription === undefined && !subError && (
            <p className="text-gray-400 text-sm mt-1">Loading…</p>
          )}
          {subscription === null && (
            <p className="text-gray-400 text-sm mt-1">No active subscription</p>
          )}
          {subscription && (
            <div className="mt-1 text-sm text-gray-700 flex flex-col gap-0.5">
              <p className="font-medium">{subscription.packageName}</p>
              <p className="text-gray-500">{subscription.startDate} → {subscription.endDate}</p>
            </div>
          )}
          {subError && <p className="text-red-500 text-xs mt-1">{subError}</p>}
        </div>

        {/* Sign out */}
        <button
          type="button"
          onClick={logout}
          className="w-full rounded-full bg-[#F5D26B] py-3 text-base font-bold text-black mt-2 hover:brightness-95 transition"
        >
          Sign out
        </button>
      </div>
    </div>
  );
}
