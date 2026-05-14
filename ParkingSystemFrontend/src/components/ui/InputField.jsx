export default function InputField({ label, value, onChange, type = 'text', placeholder = '' }) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-lg font-bold text-black">{label}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className="w-full rounded-full border-2 border-[#3A3A3A] bg-transparent px-5 py-3 text-base outline-none"
      />
    </div>
  );
}
