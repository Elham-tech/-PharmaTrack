/**
 * AuthContext Component
 * Holds the currently authenticated user and exposes login/logout actions.
 * On mount it calls /api/auth/me; a 401 response (or the 'auth:unauthorized'
 * event dispatched by the API interceptor) clears the user so protected
 * routes redirect to the login page.
 */
import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authApi } from '../api/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    const handleUnauthorized = () => {
      if (mounted) setUser(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);

    authApi.me()
      .then((data) => { if (mounted) setUser(data); })
      .catch(() => { if (mounted) setUser(null); })
      .finally(() => { if (mounted) setLoading(false); });

    return () => {
      mounted = false;
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = useCallback(async (username, password) => {
    await authApi.login(username, password);
    const me = await authApi.me();
    setUser(me);
    return me;
  }, []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch { /* session already invalid */ }
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
