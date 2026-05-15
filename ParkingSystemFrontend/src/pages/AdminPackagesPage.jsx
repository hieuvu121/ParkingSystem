import { useState, useEffect } from 'react';
import { getPackages, createPackage, updatePackage, deletePackage } from '../api/packages';

function ConfirmDialog({ message, onConfirm, onCancel }) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="bg-[#1C1C1E] border border-[#3C3C3E] rounded-2xl p-6 max-w-sm w-full mx-4">
        <p className="text-white text-sm mb-6">{message}</p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="px-4 py-2 rounded-lg text-sm bg-[#2C2C2E] text-white hover:bg-[#3C3C3E] transition">Cancel</button>
          <button onClick={onConfirm} className="px-4 py-2 rounded-lg text-sm bg-red-600 text-white hover:bg-red-700 transition">Confirm</button>
        </div>
      </div>
    </div>
  );
}

function PackageModal({ pkg, onSave, onClose }) {
  const [name, setName] = useState(pkg?.name ?? '');
  const [description, setDescription] = useState(pkg?.description ?? '');
  const [durations, setDurations] = useState(pkg?.durations ?? '');
  const [priceDollars, setPriceDollars] = useState(pkg ? (pkg.price / 100).toFixed(2) : '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      const data = {
        name,
        description,
        durations: Number(durations),
        price: Math.round(parseFloat(priceDollars) * 100),
      };
      pkg ? await updatePackage(pkg.id, data) : await createPackage(data);
      onSave();
    } catch (err) {
      setError(err.message ?? 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="bg-[#1C1C1E] border border-[#3C3C3E] rounded-2xl p-6 max-w-sm w-full mx-4">
        <h3 className="text-white font-semibold mb-4">{pkg ? 'Edit Package' : 'New Package'}</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-[#A1A1AA] text-xs block mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              placeholder="e.g. Monthly Pass"
              className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            />
          </div>
          <div>
            <label className="text-[#A1A1AA] text-xs block mb-1">Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. 30 days unlimited parking"
              className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">Duration (days)</label>
              <input
                type="number"
                value={durations}
                onChange={(e) => setDurations(e.target.value)}
                required
                min="1"
                className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">Price ($)</label>
              <input
                type="number"
                value={priceDollars}
                onChange={(e) => setPriceDollars(e.target.value)}
                required
                min="0"
                step="0.01"
                placeholder="0.00"
                className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
          </div>
          {error && <p className="text-red-400 text-xs">{error}</p>}
          <div className="flex gap-3 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-sm bg-[#2C2C2E] text-white hover:bg-[#3C3C3E] transition">Cancel</button>
            <button type="submit" disabled={saving} className="px-4 py-2 rounded-lg text-sm bg-[#F5D26B] text-black font-semibold hover:bg-[#e6c45a] disabled:opacity-50 transition">
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AdminPackagesPage() {
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);
  const [confirm, setConfirm] = useState(null);

  function loadPackages() {
    setLoading(true);
    getPackages()
      .then(setPackages)
      .catch((e) => setError(e.message ?? 'Failed to load packages'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { loadPackages(); }, []);

  function handleDelete(pkg) {
    setConfirm({
      message: `Delete package "${pkg.name}"? This cannot be undone.`,
      onConfirm: async () => {
        setConfirm(null);
        await deletePackage(pkg.id);
        loadPackages();
      },
    });
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white px-4 py-6">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-bold">Packages</h2>
            <p className="text-[#A1A1AA] text-sm mt-0.5">{packages.length} packages</p>
          </div>
          <button
            onClick={() => setModal('create')}
            className="px-4 py-2 rounded-xl text-sm bg-[#F5D26B] text-black font-semibold hover:bg-[#e6c45a] transition"
          >
            + New Package
          </button>
        </div>

        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

        {loading ? (
          <div className="flex items-center justify-center h-48 text-[#A1A1AA]">Loading…</div>
        ) : (
          <div className="space-y-3">
            {packages.length === 0 && (
              <div className="bg-[#1C1C1E] rounded-2xl px-4 py-8 text-center text-[#A1A1AA] text-sm">
                No packages yet.
              </div>
            )}
            {packages.map((pkg) => (
              <div key={pkg.id} className="bg-[#1C1C1E] rounded-2xl px-5 py-4 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-semibold truncate">{pkg.name}</p>
                  {pkg.description && (
                    <p className="text-[#A1A1AA] text-sm mt-0.5 truncate">{pkg.description}</p>
                  )}
                  <div className="flex gap-4 mt-2">
                    <span className="text-xs text-[#A1A1AA]">
                      {pkg.durations} day{pkg.durations !== 1 ? 's' : ''}
                    </span>
                    <span className="text-xs font-semibold text-[#F5D26B]">
                      ${(pkg.price / 100).toFixed(2)}
                    </span>
                  </div>
                </div>
                <div className="flex gap-2 flex-shrink-0">
                  <button
                    onClick={() => setModal(pkg)}
                    className="px-3 py-1.5 rounded-lg text-xs bg-[#3C3C3E] text-white hover:bg-[#4C4C4E] transition"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(pkg)}
                    className="px-3 py-1.5 rounded-lg text-xs bg-[#3B0000] text-red-400 hover:bg-red-900/40 transition"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {modal && (
        <PackageModal
          pkg={modal === 'create' ? null : modal}
          onSave={() => { setModal(null); loadPackages(); }}
          onClose={() => setModal(null)}
        />
      )}

      {confirm && (
        <ConfirmDialog
          message={confirm.message}
          onConfirm={confirm.onConfirm}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  );
}
