import { useState } from 'react';
import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';
import AccountPage from './pages/AccountPage';
import AdminUsersPage from './pages/AdminUsersPage';
import TopNav from './components/layout/TopNav';

export default function App() {
  const { token, user } = useAuth();
  const [page, setPage] = useState('dashboard');
  const [wsConnected, setWsConnected] = useState(false);

  if (!token) return <AuthPage />;

  return (
    <div className="min-h-screen bg-[#111111]">
      <TopNav page={page} setPage={setPage} user={user} wsConnected={wsConnected} />
      {page === 'dashboard' && (
        <DashboardPage onWsStatusChange={setWsConnected} />
      )}
      {page === 'account' && <AccountPage />}
      {page === 'admin-users' && user?.role === 'ADMIN' && <AdminUsersPage />}
    </div>
  );
}
