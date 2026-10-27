import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, getToken, setToken } from './api.js';

const AuthContext = createContext(null);

function decode(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(decodeURIComponent(escape(atob(payload))));
    if (json.exp && json.exp * 1000 < Date.now()) return null;
    return { username: json.sub, roles: json.roles || [] };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getToken() ? decode(getToken()) : null));

  const login = useCallback(async (username, password) => {
    const res = await api.login(username, password);
    setToken(res.accessToken);
    setUser(decode(res.accessToken));
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    window.addEventListener('auth:expired', logout);
    return () => window.removeEventListener('auth:expired', logout);
  }, [logout]);

  const value = useMemo(
    () => ({ user, isAdmin: !!user?.roles.includes('ADMIN'), login, logout }),
    [user, login, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
