function dotColor(available, total) {
  if (total === 0) return '#6B7280';
  const pct = available / total;
  if (pct > 0.5) return '#4ADE80';
  if (pct > 0.2) return '#FACC15';
  return '#EF4444';
}

export default function ZoneTabBar({ zones, dashboard, selectedZoneId, onSelect }) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide px-4">
      {zones.map((zone) => {
        const summary = dashboard?.byZone?.find((z) => z.zoneId === zone.id);
        const color = summary ? dotColor(summary.available, summary.total) : '#6B7280';
        const active = zone.id === selectedZoneId;
        return (
          <button
            key={zone.id}
            onClick={() => onSelect(zone.id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold whitespace-nowrap transition ${
              active
                ? 'bg-[#F5D26B] text-black'
                : 'bg-[#2C2C2E] text-white hover:bg-[#3C3C3E]'
            }`}
          >
            <span
              className="w-2 h-2 rounded-full flex-shrink-0"
              style={{ backgroundColor: color }}
            />
            {zone.type} L{zone.level}
          </button>
        );
      })}
    </div>
  );
}
