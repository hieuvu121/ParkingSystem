import { createContext, useContext, useEffect, useState } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import { getMe } from '../api/user';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(null);

  // rehydrate user on page refresh when token already exists
  useEffect(() => {
    if (!token) return;
    getMe()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('token');
        setToken(null);
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function login(email, password) {
    const data = await apiLogin(email, password);
    localStorage.setItem('token', data.token);
    setToken(data.token);
    const profile = await getMe();
    setUser(profile);
  }

  async function register(fullName, email, password) {
    await apiRegister(fullName, email, password);
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  }

  function updateUser(profile) {
    setUser(profile);
  }

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
