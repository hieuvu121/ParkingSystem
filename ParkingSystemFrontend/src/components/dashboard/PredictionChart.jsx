import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip);

export default function PredictionChart({ predictions }) {
  if (!predictions || predictions.length === 0) {
    return (
      <div className="bg-[#1C1C1E] rounded-2xl p-4">
        <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">
          Predicted Availability
        </h3>
        <div className="flex items-center justify-center h-24 text-[#A1A1AA] text-sm">
          No prediction data
        </div>
      </div>
    );
  }

  const data = {
    labels: predictions.map((p) => p.hour),
    datasets: [
      {
        data: predictions.map((p) => Math.round(p.probability * 100)),
        backgroundColor: '#F5D26B',
        borderRadius: 4,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: { color: '#A1A1AA', font: { size: 11 } },
        grid: { display: false },
        border: { display: false },
      },
      y: {
        min: 0,
        max: 100,
        ticks: { color: '#A1A1AA', font: { size: 11 }, callback: (v) => `${v}%` },
        grid: { color: '#2C2C2E' },
        border: { display: false },
      },
    },
    plugins: {
      tooltip: {
        callbacks: { label: (ctx) => `${ctx.raw}% available` },
      },
    },
  };

  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">
        Predicted Availability
      </h3>
      <div className="h-36">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
}
