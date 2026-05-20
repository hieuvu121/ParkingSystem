import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { updateMyPlate } from '../api/user';
import { getMySubscription } from '../api/subscription';

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [subscription, setSubscription] = useState(undefined);
  const [subError, setSubError] = useState('');

  const [editingPlate, setEditingPlate] = useState(false);
  const [plateInput, setPlateInput] = useState('');
  const [savingPlate, setSavingPlate] = useState(false);
  const [plateError, setPlateError] = useState('');

  const [visible, setVisible] = useState(false);

  useEffect(() => {
    setVisible(true);
    getMySubscription()
      .then(setSubscription)
      .catch((err) => setSubError(err.message ?? 'Failed to load subscription'));
  }, []);

  function startEditPlate() {
    setPlateInput(user?.plateNumber ?? '');
    setPlateError('');
    setEditingPlate(true);
  }

  async function handleSavePlate() {
    if (savingPlate) return;
    setSavingPlate(true);
    setPlateError('');
    try {
      const updated = await updateMyPlate(plateInput.trim() || null);
      updateUser(updated);
      setEditingPlate(false);
    } catch (err) {
      setPlateError(err.message ?? 'Failed to update plate number');
    } finally {
      setSavingPlate(false);
    }
  }

  const initial = user?.fullName?.[0]?.toUpperCase() ?? '?';

  const fadeBase = 'transition-all duration-300';
  const fadeIn = visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2';

  return (
    <div className="min-h-screen bg-[#111111] px-4 py-8 flex justify-center">
      <div className="w-full max-w-md flex flex-col gap-4">

        {/* Hero */}
        <div className={`bg-[#1C1C1E] border border-[#2C2C2E] rounded-2xl p-6 flex flex-col items-center gap-3 ${fadeBase} ${fadeIn}`}>
          <div className="w-20 h-20 rounded-full bg-[#2C2C2E] ring-2 ring-[#F5D26B] flex items-center justify-center text-3xl font-bold text-[#F5D26B]">
            {initial}
          </div>
          <div className="text-center">
            <p className="text-white text-xl font-bold">{user?.fullName}</p>
            <p className="text-[#A1A1AA] text-sm mt-0.5">{user?.email}</p>
          </div>
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[#F5D26B]/10 text-[#F5D26B] uppercase tracking-wide">
            {user?.role}
          </span>
        </div>

        {/* Vehicle */}
        <div className={`bg-[#1C1C1E] border border-[#F5D26B]/30 rounded-2xl p-5 ${fadeBase} delay-[50ms] ${fadeIn}`}>
          <p className="text-[#A1A1AA] text-xs font-semibold uppercase tracking-wider mb-3">Vehicle</p>

          {!editingPlate ? (
            <div className="flex items-center justify-between">
              {user?.plateNumber ? (
                <div className="border border-[#F5D26B]/50 rounded-xl px-5 py-3 bg-[#111111]">
                  <span className="font-mono text-2xl tracking-widest text-[#F5D26B]">
                    {user.plateNumber}
                  </span>
                </div>
              ) : (
                <p className="text-[#6B7280] text-sm">No plate registered</p>
              )}
              <button
                type="button"
                onClick={startEditPlate}
                className="text-sm text-[#B89B3E] font-semibold hover:underline ml-3 shrink-0"
              >
                {user?.plateNumber ? 'Edit' : 'Add'}
              </button>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <input
                value={plateInput}
                onChange={(e) => setPlateInput(e.target.value.toUpperCase())}
                placeholder="e.g. ABC-1234"
                className="border border-[#F5D26B]/40 bg-[#111111] text-[#F5D26B] font-mono text-xl tracking-widest
                           rounded-xl px-5 py-3 outline-none focus:border-[#F5D26B] uppercase w-full"
              />
              {plateError && <p className="text-red-400 text-xs">{plateError}</p>}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleSavePlate}
                  disabled={savingPlate}
                  className="text-sm bg-[#F5D26B] px-4 py-1.5 rounded-full font-semibold text-black disabled:opacity-50"
                >
                  {savingPlate ? 'Saving…' : 'Save'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditingPlate(false)}
                  className="text-sm text-[#A1A1AA] px-4 py-1.5"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Subscription */}
        <div className={`bg-[#1C1C1E] border border-[#2C2C2E] rounded-2xl p-5 ${fadeBase} delay-[100ms] ${fadeIn}`}>
          <p className="text-[#A1A1AA] text-xs font-semibold uppercase tracking-wider mb-3">Subscription</p>

          {subscription === undefined && !subError && (
            <p className="text-[#6B7280] text-sm">Loading…</p>
          )}
          {subscription === null && !subError && (
            <span className="inline-block px-3 py-1 rounded-full text-xs font-semibold bg-[#6B7280]/10 text-[#A1A1AA]">
              No active subscription
            </span>
          )}
          {subscription && (
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center gap-2">
                <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[#4ADE80]/10 text-[#4ADE80]">
                  Active
                </span>
                <span className="text-white font-semibold">{subscription.packageName}</span>
              </div>
              <p className="text-[#A1A1AA] text-sm">
                {new Date(subscription.startDate).toLocaleDateString()} → {new Date(subscription.endDate).toLocaleDateString()}
              </p>
              <p className="text-[#6B7280] text-sm">${subscription.price}</p>
            </div>
          )}
          {subError && <p className="text-red-400 text-xs">{subError}</p>}
        </div>

      </div>
    </div>
  );
}
