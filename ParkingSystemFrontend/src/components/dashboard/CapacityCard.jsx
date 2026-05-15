export default function CapacityCard({ total, available, occupied }) {
  return (
    <div className="bg-[#1C1C1E] rounded-2xl p-4">
      <h3 className="text-[#A1A1AA] text-xs uppercase tracking-widest mb-3">Capacity</h3>
      <div className="space-y-2">
        <div className="flex justify-between items-center">
          <span className="text-white text-sm">Total</span>
          <span className="text-white font-bold">{total}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-sm text-[#4ADE80]">Available</span>
          <span className="text-[#4ADE80] font-bold">{available}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-sm text-[#EF4444]">Occupied</span>
          <span className="text-[#EF4444] font-bold">{occupied}</span>
        </div>
      </div>
    </div>
  );
}
