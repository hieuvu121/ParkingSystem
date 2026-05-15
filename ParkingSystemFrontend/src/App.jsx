import { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';
import ParkingZonePage from './pages/ParkingZonePage';
import BookingsPage from './pages/BookingsPage';
import AccountPage from './pages/AccountPage';
import AdminUsersPage from './pages/AdminUsersPage';
import TopNav from './components/layout/TopNav';

export default function App() {
  const { token, user } = useAuth();
  const [page, setPage] = useState('parking');
  const [wsConnected, setWsConnected] = useState(false);
  const [bookingRefreshKey, setBookingRefreshKey] = useState(0);

  useEffect(() => {
    if (user?.role === 'ADMIN' && page === 'parking') setPage('dashboard');
  }, [user]);  // eslint-disable-line react-hooks/exhaustive-deps

  if (!token) return <AuthPage />;

  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="min-h-screen bg-[#111111]">
      <TopNav page={page} setPage={setPage} user={user} wsConnected={wsConnected} />

      {isAdmin && page === 'dashboard' && (
        <DashboardPage onWsStatusChange={setWsConnected} />
      )}
      {isAdmin && page === 'admin-users' && <AdminUsersPage />}
      {page === 'account' && <AccountPage />}

      {!isAdmin && page === 'parking' && (
        <ParkingZonePage
          onWsStatusChange={setWsConnected}
          onBooked={() => setBookingRefreshKey((k) => k + 1)}
        />
      )}
      {!isAdmin && page === 'bookings' && (
        <BookingsPage refreshKey={bookingRefreshKey} />
      )}
    </div>
  );
}
