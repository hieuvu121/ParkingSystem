export default function AuthTabs({ activeTab, onTabChange }) {
  return (
    <div className="flex rounded-full bg-[#4A4A4A] p-1">
      <button
        onClick={() => onTabChange('signin')}
        className={`flex-1 rounded-full py-4 text-xl font-bold transition ${
          activeTab === 'signin'
            ? 'bg-[#F5D26B] text-black'
            : 'bg-transparent text-white'
        }`}
      >
        Sign In
      </button>
      <button
        onClick={() => onTabChange('signup')}
        className={`flex-1 rounded-full py-4 text-xl font-bold transition ${
          activeTab === 'signup'
            ? 'bg-[#F5D26B] text-black'
            : 'bg-transparent text-white'
        }`}
      >
        Sign Up
      </button>
    </div>
  );
}
