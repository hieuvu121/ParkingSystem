import { useState } from 'react';

const SPOT_SIZE = 28;
const GAP = 6;
const LABEL_SPACE = 18;

export default function SpotGrid({ spots, loading, onSpotClick }) {
  const [hoveredId, setHoveredId] = useState(null);

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
  const svgWidth = (maxCol) * (SPOT_SIZE + GAP) + GAP + LABEL_SPACE;
  const svgHeight = (maxRow) * (SPOT_SIZE + GAP) + GAP + LABEL_SPACE;

  return (
    <div className="p-4">
      <div className="w-full">
        <svg
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          className="w-full h-auto"
          preserveAspectRatio="xMidYMid meet"
        >
          {Array.from({ length: maxCol }, (_, i) => (
            <text
              key={`col-${i + 1}`}
              x={LABEL_SPACE + (i * (SPOT_SIZE + GAP)) + GAP + SPOT_SIZE / 2}
              y={LABEL_SPACE - 4}
              textAnchor="middle"
              fontSize="10"
              fill="#A1A1AA"
            >
              {i + 1}
            </text>
          ))}
          {Array.from({ length: maxRow }, (_, i) => (
            <text
              key={`row-${i + 1}`}
              x={LABEL_SPACE - 4}
              y={LABEL_SPACE + (i * (SPOT_SIZE + GAP)) + GAP + SPOT_SIZE / 2 + 4}
              textAnchor="end"
              fontSize="10"
              fill="#A1A1AA"
            >
              {i + 1}
            </text>
          ))}
          {spots.map((spot) => {
            const x = LABEL_SPACE + (Number(spot.col) - 1) * (SPOT_SIZE + GAP) + GAP;
            const y = LABEL_SPACE + (Number(spot.row) - 1) * (SPOT_SIZE + GAP) + GAP;
            const isAvailable = spot.status === 'AVAILABLE';
            const isHovered = hoveredId === spot.id && isAvailable;
            const fill = isAvailable ? '#4ADE80' : '#EF4444';
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
                stroke={isHovered ? '#F5D26B' : 'none'}
                strokeWidth={2}
                style={{ cursor: isAvailable && onSpotClick ? 'pointer' : 'default' }}
                onClick={isAvailable && onSpotClick ? () => onSpotClick(spot) : undefined}
                onMouseEnter={isAvailable && onSpotClick ? () => setHoveredId(spot.id) : undefined}
                onMouseLeave={() => setHoveredId(null)}
              >
                <title>{`Spot R${spot.row}-C${spot.col}: ${spot.status}`}</title>
              </rect>
            );
          })}
        </svg>
      </div>
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
