export default function TopNav({ page, setPage, user, wsConnected }) {
  const isAdmin = user?.role === 'ADMIN';

  const navItems = isAdmin
    ? [
        { key: 'dashboard',   label: 'Dashboard' },
        { key: 'admin-users', label: 'Users' },
      ]
    : [
        { key: 'parking',  label: 'Parking' },
        { key: 'bookings', label: 'Bookings' },
      ];

  return (
    <header className="bg-[#1C1C1E] border-b border-[#2C2C2E] px-4 py-3 flex items-center gap-4">
      <span className="text-[#F5D26B] text-xl font-bold select-none">🅿</span>
      <span className="text-white font-semibold text-lg mr-2 hidden sm:block">Parking System</span>

      <nav className="flex items-center gap-1 flex-1">
        {navItems.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setPage(item.key)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition border-b-2 ${
              page === item.key
                ? 'text-[#F5D26B] border-[#F5D26B]'
                : 'text-[#A1A1AA] hover:text-white border-transparent'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <button
        type="button"
        onClick={() => setPage('account')}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
          page === 'account' ? 'text-[#F5D26B]' : 'text-[#A1A1AA] hover:text-white'
        }`}
      >
        <span className="w-6 h-6 rounded-full bg-[#2C2C2E] flex items-center justify-center text-xs font-bold text-white">
          {user?.fullName?.[0]?.toUpperCase() ?? '?'}
        </span>
        <span className="hidden sm:block">{user?.fullName ?? 'Account'}</span>
      </button>

      <span className="flex items-center gap-1.5 text-xs text-[#A1A1AA] ml-1">
        <span
          className={`w-2 h-2 rounded-full flex-shrink-0 ${
            wsConnected ? 'bg-[#4ADE80] animate-pulse' : 'bg-[#6B7280]'
          }`}
        />
        <span className="hidden sm:block">{wsConnected ? 'Live' : 'Connecting…'}</span>
      </span>
    </header>
  );
}
