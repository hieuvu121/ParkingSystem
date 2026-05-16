import { useState, useEffect } from 'react';
import {
  getZones, createZone, updateZone, deleteZone,
  getSpotsByZone, createSpot, updateSpot, deleteSpot,
} from '../api/zones';

const ZONE_TYPES = ['INDOOR', 'OUTDOOR', 'ROOFTOP', 'UNDERGROUND'];
const SPOT_TYPES = ['STANDARD', 'HANDICAP', 'EV', 'COMPACT'];

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

function ZoneModal({ zone, onSave, onClose }) {
  const [level, setLevel] = useState(zone?.level ?? '');
  const [type, setType] = useState(zone?.type ?? ZONE_TYPES[0]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      const data = { level: Number(level), type };
      zone ? await updateZone(zone.id, data) : await createZone(data);
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
        <h3 className="text-white font-semibold mb-4">{zone ? 'Edit Zone' : 'New Zone'}</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-[#A1A1AA] text-xs block mb-1">Level</label>
            <input
              type="number"
              value={level}
              onChange={(e) => setLevel(e.target.value)}
              required
              className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            />
          </div>
          <div>
            <label className="text-[#A1A1AA] text-xs block mb-1">Type</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            >
              {ZONE_TYPES.map((t) => <option key={t}>{t}</option>)}
            </select>
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

function SpotModal({ zoneId, spot, existingSpots, onSave, onClose }) {
  const [row, setRow] = useState(spot?.row ?? '');
  const [col, setCol] = useState(spot?.col ?? '');
  const [type, setType] = useState(spot?.type ?? SPOT_TYPES[0]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    const r = Number(row);
    const c = Number(col);
    const duplicate = existingSpots.some(
      (s) => Number(s.row) === r && Number(s.col) === c && s.id !== spot?.id
    );
    if (duplicate) {
      setError(`Spot R${r}–C${c} already exists in this zone.`);
      return;
    }
    setSaving(true);
    try {
      const data = { row: r, col: c, type };
      const saved = spot ? await updateSpot(spot.id, data) : await createSpot(zoneId, data);
      onSave(saved);
    } catch (err) {
      setError(err.message ?? 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="bg-[#1C1C1E] border border-[#3C3C3E] rounded-2xl p-6 max-w-sm w-full mx-4">
        <h3 className="text-white font-semibold mb-4">{spot ? 'Edit Spot' : 'New Spot'}</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">Row</label>
              <input
                type="number"
                value={row}
                onChange={(e) => setRow(e.target.value)}
                required
                min="1"
                className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
            <div>
              <label className="text-[#A1A1AA] text-xs block mb-1">Col</label>
              <input
                type="number"
                value={col}
                onChange={(e) => setCol(e.target.value)}
                required
                min="1"
                className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
              />
            </div>
          </div>
          <div>
            <label className="text-[#A1A1AA] text-xs block mb-1">Type</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full bg-[#2C2C2E] text-white rounded-xl px-3 py-2 text-sm outline-none border border-[#3C3C3E] focus:border-[#F5D26B]"
            >
              {SPOT_TYPES.map((t) => <option key={t}>{t}</option>)}
            </select>
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

export default function AdminZonesPage() {
  const [zones, setZones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedZone, setSelectedZone] = useState(null);
  const [spots, setSpots] = useState([]);
  const [spotsLoading, setSpotsLoading] = useState(false);
  const [zoneModal, setZoneModal] = useState(null);
  const [spotModal, setSpotModal] = useState(null);
  const [confirm, setConfirm] = useState(null);

  function loadZones() {
    setLoading(true);
    getZones()
      .then(setZones)
      .catch((e) => setError(e.message ?? 'Failed to load zones'))
      .finally(() => setLoading(false));
  }

  function loadSpots(zoneId) {
    setSpotsLoading(true);
    getSpotsByZone(zoneId)
      .then(setSpots)
      .catch(() => setSpots([]))
      .finally(() => setSpotsLoading(false));
  }

  useEffect(() => { loadZones(); }, []);

  useEffect(() => {
    if (selectedZone) loadSpots(selectedZone.id);
    else setSpots([]);
  }, [selectedZone]);

  function handleDeleteZone(zone) {
    setConfirm({
      message: `Delete zone ${zone.type} L${zone.level}? All spots in this zone will also be deleted.`,
      onConfirm: async () => {
        setConfirm(null);
        await deleteZone(zone.id);
        if (selectedZone?.id === zone.id) setSelectedZone(null);
        loadZones();
      },
    });
  }

  function handleDeleteSpot(spot) {
    setConfirm({
      message: `Delete spot R${spot.row}–C${spot.col} (${spot.type})?`,
      onConfirm: async () => {
        setConfirm(null);
        await deleteSpot(spot.id);
        loadSpots(selectedZone.id);
      },
    });
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white px-4 py-6">
      <div className="max-w-6xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Zones panel */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-xl font-bold">Zones</h2>
                <p className="text-[#A1A1AA] text-sm mt-0.5">{zones.length} zones</p>
              </div>
              <button
                onClick={() => setZoneModal('create')}
                className="px-4 py-2 rounded-xl text-sm bg-[#F5D26B] text-black font-semibold hover:bg-[#e6c45a] transition"
              >
                + New Zone
              </button>
            </div>

            {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

            {loading ? (
              <div className="flex items-center justify-center h-32 text-[#A1A1AA]">Loading…</div>
            ) : (
              <div className="bg-[#1C1C1E] rounded-2xl overflow-hidden">
                {zones.length === 0 && (
                  <p className="px-4 py-8 text-center text-[#A1A1AA] text-sm">No zones yet.</p>
                )}
                {zones.map((zone) => (
                  <div
                    key={zone.id}
                    className={`flex items-center justify-between px-4 py-3 border-b border-[#2C2C2E] last:border-0 cursor-pointer transition ${
                      selectedZone?.id === zone.id ? 'bg-[#2C2C2E]' : 'hover:bg-[#242424]'
                    }`}
                    onClick={() => setSelectedZone(selectedZone?.id === zone.id ? null : zone)}
                  >
                    <div>
                      <p className="font-medium text-sm">{zone.type}</p>
                      <p className="text-[#A1A1AA] text-xs">Level {zone.level}</p>
                    </div>
                    <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => setZoneModal(zone)}
                        className="px-3 py-1 rounded-lg text-xs bg-[#3C3C3E] text-white hover:bg-[#4C4C4E] transition"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDeleteZone(zone)}
                        className="px-3 py-1 rounded-lg text-xs bg-[#3B0000] text-red-400 hover:bg-red-900/40 transition"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Spots panel */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-xl font-bold">
                  {selectedZone ? `Spots — ${selectedZone.type} L${selectedZone.level}` : 'Spots'}
                </h2>
                <p className="text-[#A1A1AA] text-sm mt-0.5">
                  {selectedZone ? `${spots.length} spots` : 'Select a zone to manage spots'}
                </p>
              </div>
              {selectedZone && (
                <button
                  onClick={() => setSpotModal('create')}
                  className="px-4 py-2 rounded-xl text-sm bg-[#F5D26B] text-black font-semibold hover:bg-[#e6c45a] transition"
                >
                  + New Spot
                </button>
              )}
            </div>

            {!selectedZone ? (
              <div className="bg-[#1C1C1E] rounded-2xl flex items-center justify-center h-48 text-[#A1A1AA] text-sm">
                ← Select a zone to manage its spots
              </div>
            ) : spotsLoading ? (
              <div className="flex items-center justify-center h-32 text-[#A1A1AA]">Loading…</div>
            ) : (
              <div className="bg-[#1C1C1E] rounded-2xl overflow-hidden">
                {spots.length === 0 && (
                  <p className="px-4 py-8 text-center text-[#A1A1AA] text-sm">No spots in this zone.</p>
                )}
                {spots.map((spot) => (
                  <div
                    key={spot.id}
                    className="flex items-center justify-between px-4 py-3 border-b border-[#2C2C2E] last:border-0"
                  >
                    <div>
                      <p className="font-medium text-sm">R{spot.row}–C{spot.col}</p>
                      <p className="text-[#A1A1AA] text-xs">{spot.type}</p>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => setSpotModal(spot)}
                        className="px-3 py-1 rounded-lg text-xs bg-[#3C3C3E] text-white hover:bg-[#4C4C4E] transition"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDeleteSpot(spot)}
                        className="px-3 py-1 rounded-lg text-xs bg-[#3B0000] text-red-400 hover:bg-red-900/40 transition"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {zoneModal && (
        <ZoneModal
          zone={zoneModal === 'create' ? null : zoneModal}
          onSave={() => { setZoneModal(null); loadZones(); }}
          onClose={() => setZoneModal(null)}
        />
      )}

      {spotModal && selectedZone && (
        <SpotModal
          zoneId={selectedZone.id}
          spot={spotModal === 'create' ? null : spotModal}
          existingSpots={spots}
          onSave={(saved) => {
            setSpotModal(null);
            if (spotModal === 'create') {
              setSpots((prev) => [...prev, saved]);
            } else {
              setSpots((prev) => prev.map((s) => s.id === saved.id ? saved : s));
            }
          }}
          onClose={() => setSpotModal(null)}
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
