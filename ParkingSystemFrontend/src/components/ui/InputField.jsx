export default function InputField({ label, value, onChange, type = 'text', placeholder = '' }) {
  const id = label.toLowerCase().replace(/\s+/g, '-');
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-lg font-bold text-black">{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className="w-full rounded-full border-2 border-[#3A3A3A] bg-transparent px-5 py-3 text-base text-black placeholder:text-gray-400 outline-none"
      />
    </div>
  );
}
