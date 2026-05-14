const SPOT_SIZE = 28;
const GAP = 6;

export default function SpotGrid({ spots, loading }) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-48 text-[#A1A1AA] text-sm">
        Loading spots…
      </div>
    );
  }
  if (!spots || spots.length === 0) {
    return (
      <div className="flex items-center justify-center h-48 text-[#A1A1AA] text-sm">
        No spots in this zone
      </div>
    );
  }

  const maxRow = Math.max(...spots.map((s) => Number(s.row)));
  const maxCol = Math.max(...spots.map((s) => Number(s.col)));
  const svgWidth = (maxCol) * (SPOT_SIZE + GAP) + GAP;
  const svgHeight = (maxRow) * (SPOT_SIZE + GAP) + GAP;

  return (
    <div className="overflow-auto p-4">
      <svg width={svgWidth} height={svgHeight}>
        {spots.map((spot) => {
          const x = (Number(spot.col) - 1) * (SPOT_SIZE + GAP) + GAP;
          const y = (Number(spot.row) - 1) * (SPOT_SIZE + GAP) + GAP;
          const fill = spot.status === 'AVAILABLE' ? '#4ADE80' : '#EF4444';
          return (
            <rect
              key={spot.id}
              x={x}
              y={y}
              width={SPOT_SIZE}
              height={SPOT_SIZE}
              rx={4}
              fill={fill}
              opacity={0.9}
            >
              <title>{`Spot ${spot.row}-${spot.col}: ${spot.status}`}</title>
            </rect>
          );
        })}
      </svg>
      <div className="flex gap-4 mt-3 text-xs text-[#A1A1AA]">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-sm bg-[#4ADE80] inline-block" /> Available
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-sm bg-[#EF4444] inline-block" /> Occupied
        </span>
      </div>
    </div>
  );
}
