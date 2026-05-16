import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getZones, getDashboard, getSpots } from '../api/dashboard';
import { getRecommendation } from '../api/recommendation';
import ZoneCard from '../components/parking/ZoneCard';
import SpotGrid from '../components/dashboard/SpotGrid';
import BookingModal from '../components/parking/BookingModal';

export default function ParkingZonePage({ onWsStatusChange, onBooked }) {
  const [zones, setZones] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [selectedZoneId, setSelectedZoneId] = useState(null);
  const [spots, setSpots] = useState([]);
  const [loadingInit, setLoadingInit] = useState(true);
  const [loadingZone, setLoadingZone] = useState(false);
  const [error, setError] = useState('');
  const [modalSpot, setModalSpot] = useState(null);
  const [recommendation, setRecommendation] = useState(null);
  const [recLoading, setRecLoading] = useState(false);
  const [recError, setRecError] = useState('');
  const [recExpanded, setRecExpanded] = useState(false);

  const selectedZoneIdRef = useRef(selectedZoneId);
  useEffect(() => { selectedZoneIdRef.current = selectedZoneId; }, [selectedZoneId]);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        onWsStatusChange(true);

        client.subscribe('/topic/spots', (msg) => {
          const update = JSON.parse(msg.body);
          if (update.zoneId === selectedZoneIdRef.current) {
            setSpots((prev) =>
              prev.map((s) => s.id === update.spotId ? { ...s, status: update.status } : s)
            );
          }
        });

        client.subscribe('/topic/dashboard', (msg) => {
          setDashboard(JSON.parse(msg.body));
        });
      },
      onDisconnect: () => onWsStatusChange(false),
      onStompError: () => onWsStatusChange(false),
    });
    client.activate();
    return () => client.deactivate();
  }, []);  // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    Promise.all([getZones(), getDashboard()])
      .then(([z, d]) => { setZones(z); setDashboard(d); })
      .catch((err) => setError(err.message ?? 'Failed to load zones'))
      .finally(() => setLoadingInit(false));
  }, []);

  useEffect(() => {
    if (!selectedZoneId) return;
    let ignore = false;
    setLoadingZone(true);
    setSpots([]);
    getSpots(selectedZoneId)
      .then((data) => { if (!ignore) setSpots(data); })
      .catch((err) => { if (!ignore) setError(err.message ?? 'Failed to load spots'); })
      .finally(() => { if (!ignore) setLoadingZone(false); });
    return () => { ignore = true; };
  }, [selectedZoneId]);

  useEffect(() => {
    let ignore = false;
    setRecLoading(true);
    setRecError('');
    getRecommendation()
      .then((rec) => { if (!ignore) setRecommendation(rec); })
      .catch((err) => { if (!ignore) setRecError(err.message ?? 'Failed to load recommendation'); })
      .finally(() => { if (!ignore) setRecLoading(false); });
    return () => { ignore = true; };
  }, []);

  const selectedZone = zones.find((z) => z.id === selectedZoneId);
  const recommendedZone = recommendation
    ? zones.find((z) => z.id === recommendation.zoneId)
    : null;

  if (loadingInit) {
    return (
      <div className="min-h-screen bg-[#111111] flex items-center justify-center text-white">
        Loading…
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#111111] flex flex-col items-center justify-center gap-4 text-white">
        <p className="text-red-400">{error}</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-[#F5D26B] text-black rounded-full text-sm font-semibold"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#111111] text-white">
      {!selectedZoneId && (
        <div className="max-w-2xl mx-auto px-4 py-8">
          <div className="mb-6 rounded-2xl border border-[#2C2C2E] bg-[#1C1C1E] p-4">
            {!recExpanded && (
              <button
                type="button"
                onClick={() => setRecExpanded(true)}
                className="rounded-full bg-[#F5D26B] px-4 py-2 text-xs font-semibold text-black"
              >
                View suggested zone
              </button>
            )}
            {recExpanded && (
              <>
                <div className="flex items-center justify-between">
                  <p className="text-xs uppercase tracking-widest text-[#A1A1AA]">Best zone to park</p>
                  <button
                    type="button"
                    onClick={() => setRecExpanded(false)}
                    className="text-xs text-[#A1A1AA] hover:text-white"
                  >
                    Hide
                  </button>
                </div>
                {recLoading && (
                  <p className="mt-2 text-sm text-[#A1A1AA]">Finding the best zone…</p>
                )}
                {!recLoading && recError && (
                  <p className="mt-2 text-sm text-red-400">{recError}</p>
                )}
                {!recLoading && !recError && recommendation && (
                  <div className="mt-3 flex flex-col gap-2">
                    <p className="text-base font-semibold">
                      {recommendedZone
                        ? `${recommendedZone.type} · Level ${recommendedZone.level}`
                        : `Zone ${recommendation.zoneId}`}
                    </p>
                    <p className="text-xs text-[#A1A1AA]">
                      {recommendation.reason} · {recommendation.availableSpots} open ·
                      {(recommendation.predictedProbability * 100).toFixed(0)}% predicted availability
                    </p>
                    <button
                      type="button"
                      onClick={() => setSelectedZoneId(recommendation.zoneId)}
                      className="self-start rounded-full bg-[#F5D26B] px-4 py-2 text-xs font-semibold text-black"
                    >
                      View zone
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          <h1 className="text-2xl font-bold mb-1">Choose a Parking Zone</h1>
          <p className="text-[#A1A1AA] text-sm mb-6">Select a zone to see available spots</p>
          <div className="grid grid-cols-2 gap-4">
            {zones.map((zone) => {
              const summary = dashboard?.byZone?.find((z) => z.zoneId === zone.id);
              return (
                <ZoneCard
                  key={zone.id}
                  zone={zone}
                  summary={summary}
                  onClick={() => setSelectedZoneId(zone.id)}
                />
              );
            })}
          </div>
        </div>
      )}

      {selectedZoneId && (
        <div className="max-w-2xl mx-auto px-4 py-8">
          <div className="flex items-center gap-3 mb-6">
            <button
              type="button"
              onClick={() => { setSelectedZoneId(null); setSpots([]); }}
              className="text-[#A1A1AA] hover:text-white text-sm flex items-center gap-1 transition"
            >
              ← Zones
            </button>
            <span className="text-[#2C2C2E]">|</span>
            <h1 className="text-white font-bold text-lg">
              {selectedZone ? `${selectedZone.type} · Level ${selectedZone.level}` : ''}
            </h1>
          </div>

          <div className="mb-4 rounded-2xl border border-[#2C2C2E] bg-[#1C1C1E] p-4">
            {!recExpanded && (
              <button
                type="button"
                onClick={() => setRecExpanded(true)}
                className="rounded-full bg-[#F5D26B] px-4 py-2 text-xs font-semibold text-black"
              >
                View suggested zone
              </button>
            )}
            {recExpanded && (
              <>
                <div className="flex items-center justify-between">
                  <p className="text-xs uppercase tracking-widest text-[#A1A1AA]">Best zone to park</p>
                  <button
                    type="button"
                    onClick={() => setRecExpanded(false)}
                    className="text-xs text-[#A1A1AA] hover:text-white"
                  >
                    Hide
                  </button>
                </div>
                {recLoading && (
                  <p className="mt-2 text-sm text-[#A1A1AA]">Finding the best zone…</p>
                )}
                {!recLoading && recError && (
                  <p className="mt-2 text-sm text-red-400">{recError}</p>
                )}
                {!recLoading && !recError && recommendation && (
                  <div className="mt-3 flex flex-col gap-2">
                    <p className="text-base font-semibold">
                      {recommendedZone
                        ? `${recommendedZone.type} · Level ${recommendedZone.level}`
                        : `Zone ${recommendation.zoneId}`}
                    </p>
                    <p className="text-xs text-[#A1A1AA]">
                      {recommendation.reason} · {recommendation.availableSpots} open ·
                      {(recommendation.predictedProbability * 100).toFixed(0)}% predicted availability
                    </p>
                    {recommendation.zoneId !== selectedZoneId && (
                      <button
                        type="button"
                        onClick={() => setSelectedZoneId(recommendation.zoneId)}
                        className="self-start rounded-full bg-[#F5D26B] px-4 py-2 text-xs font-semibold text-black"
                      >
                        Switch to recommended zone
                      </button>
                    )}
                    {recommendation.zoneId === selectedZoneId && (
                      <p className="text-xs text-[#4ADE80]">You are viewing the recommended zone.</p>
                    )}
                  </div>
                )}
              </>
            )}
          </div>

          <div className="bg-[#1C1C1E] rounded-2xl">
            <div className="px-4 pt-4 pb-2">
              <h2 className="text-sm text-[#A1A1AA] uppercase tracking-widest">Spot Map</h2>
              <p className="text-xs text-[#A1A1AA] mt-1">Click an available spot to book</p>
            </div>
            <SpotGrid
              spots={spots}
              loading={loadingZone}
              onSpotClick={(spot) => setModalSpot(spot)}
            />
          </div>
        </div>
      )}

      {modalSpot && (
        <BookingModal
          spot={modalSpot}
          onClose={() => setModalSpot(null)}
          onBooked={() => {
            setModalSpot(null);
            onBooked?.();
          }}
        />
      )}
    </div>
  );
}
