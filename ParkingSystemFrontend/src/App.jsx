import { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';
import ParkingZonePage from './pages/ParkingZonePage';
import BookingsPage from './pages/BookingsPage';
import AccountPage from './pages/AccountPage';
import ProfilePage from './pages/ProfilePage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminBookingsPage from './pages/AdminBookingsPage';
import AdminZonesPage from './pages/AdminZonesPage';
import AdminPackagesPage from './pages/AdminPackagesPage';
import AdminAnalyticsPage from './pages/AdminAnalyticsPage';
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
      {isAdmin && page === 'admin-bookings' && <AdminBookingsPage />}
      {isAdmin && page === 'admin-zones' && <AdminZonesPage />}
      {isAdmin && page === 'admin-packages' && <AdminPackagesPage />}
      {isAdmin && page === 'admin-analytics' && <AdminAnalyticsPage />}
      {page === 'account' && <AccountPage />}
      {!isAdmin && page === 'profile' && <ProfilePage />}

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
