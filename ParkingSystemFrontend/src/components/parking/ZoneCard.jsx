function dotColor(available, total) {
  if (total === 0) return '#6B7280';
  const pct = available / total;
  if (pct > 0.5) return '#4ADE80';
  if (pct > 0.2) return '#FACC15';
  return '#EF4444';
}

export default function ZoneCard({ zone, summary, onClick }) {
  const available = summary?.available ?? 0;
  const total = summary?.total ?? 0;
  const color = dotColor(available, total);

  return (
    <button
      type="button"
      onClick={onClick}
      className="bg-[#1C1C1E] rounded-2xl p-5 text-left hover:bg-[#2C2C2E] transition flex flex-col gap-3 border border-[#2C2C2E] hover:border-[#F5D26B] w-full"
    >
      <div className="flex items-center gap-2">
        <span className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: color }} />
        <span className="text-white font-semibold text-base">
          {zone.type} · L{zone.level}
        </span>
      </div>
      <p className="text-[#A1A1AA] text-sm">
        {available} / {total} available
      </p>
    </button>
  );
}
