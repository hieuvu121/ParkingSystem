import { useEffect, useState } from 'react';
import { getMySubscription, subscribe, getPackages } from '../../api/subscription';
import { createBooking, toLocalISO } from '../../api/bookings';

const DURATIONS = [
  { label: '30 min', minutes: 30, cents: 50 },
  { label: '1 hr',   minutes: 60, cents: 100 },
  { label: '2 hrs',  minutes: 120, cents: 200 },
];

export default function BookingModal({ spot, onClose, onBooked }) {
  const [step, setStep] = useState('loading');
  const [paymentType, setPaymentType] = useState(null);
  const [packages, setPackages] = useState([]);
  const [duration, setDuration] = useState(DURATIONS[1]);
  const [error, setError] = useState('');

  useEffect(() => {
    getMySubscription()
      .then((sub) => {
        if (sub) {
          setPaymentType('SUBSCRIPTION');
          setStep('form');
        } else {
          setStep('choose-method');
        }
      })
      .catch(() => setStep('choose-method'));
  }, []);

  async function handleSubscribeClick() {
    setError('');
    try {
      const pkgs = await getPackages();
      setPackages(pkgs);
      setStep('packages');
    } catch {
      setError('Failed to load packages. Try again.');
    }
  }

  async function handleSelectPackage(packageId) {
    setError('');
    try {
      await subscribe(packageId);
      setPaymentType('SUBSCRIPTION');
      setStep('form');
    } catch (e) {
      setError(e.message ?? 'Failed to subscribe. Try again.');
    }
  }

  async function handleConfirm() {
    setStep('submitting');
    setError('');
    const now = new Date();
    const end = new Date(now.getTime() + duration.minutes * 60 * 1000);
    try {
      await createBooking({
        spotId: spot.id,
        startTime: toLocalISO(now),
        endTime: toLocalISO(end),
        paymentType,
      });
      onBooked();
    } catch (e) {
      setError(
        e.status === 409
          ? 'Spot just got taken — please pick another.'
          : 'Something went wrong. Try again.'
      );
      setStep('form');
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-[#1C1C1E] rounded-2xl w-full max-w-sm p-6 flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <h2 className="text-white font-bold text-lg">Book a Spot</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[#A1A1AA] hover:text-white text-xl leading-none"
          >
            ✕
          </button>
        </div>

        <p className="text-[#A1A1AA] text-sm">
          Spot R{spot.row}–C{spot.col} · {spot.type}
        </p>

        {step === 'loading' && (
          <p className="text-[#A1A1AA] text-sm text-center py-4">Checking subscription…</p>
        )}

        {step === 'choose-method' && (
          <div className="flex flex-col gap-3">
            <p className="text-white text-sm font-medium">No active subscription</p>
            <button
              type="button"
              onClick={handleSubscribeClick}
              className="w-full py-3 rounded-xl bg-[#F5D26B] text-black font-semibold text-sm hover:opacity-90 transition"
            >
              Subscribe for unlimited parking
            </button>
            <button
              type="button"
              onClick={() => { setPaymentType('PAY_PER_USE'); setStep('form'); }}
              className="w-full py-3 rounded-xl bg-[#2C2C2E] text-white font-semibold text-sm hover:bg-[#3C3C3E] transition"
            >
              Pay per use — $1 / hr
            </button>
          </div>
        )}

        {step === 'packages' && (
          <div className="flex flex-col gap-3">
            <p className="text-white text-sm font-medium">Choose a plan</p>
            {packages.length === 0 && (
              <p className="text-[#A1A1AA] text-sm">No packages available.</p>
            )}
            {packages.map((pkg) => (
              <button
                key={pkg.id}
                type="button"
                onClick={() => handleSelectPackage(pkg.id)}
                className="w-full text-left p-4 rounded-xl bg-[#2C2C2E] hover:bg-[#3C3C3E] transition"
              >
                <p className="text-white font-semibold text-sm">{pkg.name}</p>
                <p className="text-[#A1A1AA] text-xs mt-1">
                  ${(pkg.price / 100).toFixed(2)} · {pkg.durations} days
                </p>
              </button>
            ))}
            <button
              type="button"
              onClick={() => setStep('choose-method')}
              className="text-[#A1A1AA] text-xs hover:text-white mt-1"
            >
              ← Back
            </button>
          </div>
        )}

        {(step === 'form' || step === 'submitting') && (
          <div className="flex flex-col gap-4">
            {paymentType === 'SUBSCRIPTION' && (
              <p className="text-[#4ADE80] text-xs font-medium">Covered by your subscription</p>
            )}

            <div>
              <p className="text-[#A1A1AA] text-xs mb-2 uppercase tracking-widest">Duration</p>
              <div className="flex gap-2">
                {DURATIONS.map((d) => (
                  <button
                    key={d.minutes}
                    type="button"
                    onClick={() => setDuration(d)}
                    className={`flex-1 py-2 rounded-full text-sm font-semibold transition ${
                      duration.minutes === d.minutes
                        ? 'bg-[#F5D26B] text-black'
                        : 'bg-[#2C2C2E] text-white hover:bg-[#3C3C3E]'
                    }`}
                  >
                    {d.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-between items-center">
              <span className="text-[#A1A1AA] text-sm">Total</span>
              <span className="text-white font-bold text-base">
                {paymentType === 'SUBSCRIPTION'
                  ? 'Free'
                  : `$${(duration.cents / 100).toFixed(2)}`}
              </span>
            </div>

            <button
              type="button"
              onClick={handleConfirm}
              disabled={step === 'submitting'}
              className="w-full py-3 rounded-xl bg-[#F5D26B] text-black font-bold text-sm hover:opacity-90 transition disabled:opacity-50"
            >
              {step === 'submitting' ? 'Booking…' : 'Confirm Booking'}
            </button>
          </div>
        )}

        {error && (
          <p className="text-red-400 text-xs text-center">{error}</p>
        )}
      </div>
    </div>
  );
}
