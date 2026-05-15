import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip } from 'chart.js';

ChartJS.register(ArcElement, Tooltip);

export default function DonutChart({ available, total }) {
  const pct = total > 0 ? Math.round((available / total) * 100) : 0;
  const occupied = total - available;

  const data = {
    datasets: [
      {
        data: [available, occupied],
        backgroundColor: ['#F5D26B', '#2C2C2E'],
        borderWidth: 0,
        cutout: '72%',
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: { tooltip: { enabled: false } },
  };

  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">Availability</h3>
      <div className="relative w-36 h-36 mx-auto">
        <Doughnut data={data} options={options} />
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-white font-bold text-lg">{pct}% Free</span>
        </div>
      </div>
    </div>
  );
}
