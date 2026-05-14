export default function PillButton({ children, onClick, disabled = false, className = '' }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`w-full rounded-full bg-[#F5D26B] py-4 text-xl font-bold text-black transition hover:brightness-95 disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
    >
      {children}
    </button>
  );
}
