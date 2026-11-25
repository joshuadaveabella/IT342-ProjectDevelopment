import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api, { ensureCsrf } from './api';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export const dashboardPath = (role) => (role === 'LEAD' ? '/lead' : '/member');

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore the session (if any) on page load: the server is the source of truth.
  useEffect(() => {
    ensureCsrf()
      .then(() => api.get('/auth/me'))
      .then((r) => setUser(r.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    await ensureCsrf();
    const r = await api.post('/auth/login', { email, password });
    setUser(r.data);
    return r.data;
  }, []);

  const register = useCallback(async (payload) => {
    await ensureCsrf();
    return (await api.post('/auth/register', payload)).data;
  }, []);

  const logout = useCallback(async () => {
    try { await api.post('/auth/logout'); } finally { setUser(null); }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser: setUser }}>
      {children}
    </AuthContext.Provider>
  );
}
