import { useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import DashboardPage from './pages/DashboardPage';

export default function App() {
  const { token } = useAuth();
  return token ? <DashboardPage /> : <AuthPage />;
}
