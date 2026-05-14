export default function PillButton({ children, onClick, className = '' }) {
  return (
    <button
      onClick={onClick}
      className={`w-full rounded-full bg-[#F5D26B] py-4 text-xl font-bold text-black transition hover:brightness-95 ${className}`}
    >
      {children}
    </button>
  );
}
