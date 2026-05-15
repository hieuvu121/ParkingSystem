import CapacityCard from './CapacityCard';
import DonutChart from './DonutChart';
import PredictionChart from './PredictionChart';

export default function StatsPanel({ zoneSummary, predictions, loading }) {
  const total = zoneSummary?.total ?? 0;
  const available = zoneSummary?.available ?? 0;
  const occupied = zoneSummary?.occupied ?? 0;

  if (loading) {
    return (
      <div className="flex flex-col gap-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-[#1C1C1E] rounded-2xl p-4 h-24 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <CapacityCard total={total} available={available} occupied={occupied} />
      <DonutChart available={available} total={total} />
      <PredictionChart predictions={predictions} />
    </div>
  );
}
