import { createContext, useContext, useState } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  async function login(email, password) {
    const data = await apiLogin(email, password); // throws on error
    localStorage.setItem('token', data.token);
    setToken(data.token);
  }

  async function register(fullName, email, password) {
    await apiRegister(fullName, email, password); // throws on error, no token stored
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
