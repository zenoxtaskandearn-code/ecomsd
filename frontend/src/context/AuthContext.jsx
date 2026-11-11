import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { apiEnabled, clearTokens, setTokens, refreshSession } from '../services/api.js';
import * as authApi from '../services/authApi.js';

const AuthContext = createContext(null);
const STORAGE_KEY = 'fetch_demo_auth';

function readStored() {
  try { const raw = localStorage.getItem(STORAGE_KEY); return raw ? JSON.parse(raw) : null; } catch { return null; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (apiEnabled ? null : readStored()));
  const [hydrating, setHydrating] = useState(apiEnabled);

  useEffect(() => {
    if (!apiEnabled) return;
    let mounted = true;
    (async () => {
      try {
        const stored = localStorage.getItem('fetch_refresh_token');
        if (stored) {
          const data = await refreshSession();
          if (mounted) setUser(data.user);
        }
      } catch {
        clearTokens();
        if (mounted) setUser(null);
      } finally {
        if (mounted) setHydrating(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const persistDemo = (next) => {
    setUser(next);
    try { if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); else localStorage.removeItem(STORAGE_KEY); } catch {}
  };

  const value = useMemo(() => ({
    user,
    hydrating,
    isAuthenticated: !!user,
    login: async (payload) => {
      if (apiEnabled) {
        const data = await authApi.login(payload);
        setTokens(data); setUser(data.user); return { ok: true };
      }
      await new Promise((r) => setTimeout(r, 650));
      persistDemo({ email: payload.email, firstName: 'Alex', lastName: 'Morgan' }); return { ok: true };
    },
    register: async (payload) => {
      if (apiEnabled) {
        const data = await authApi.register(payload);
        // New flow: registration requires email activation via one-time link; no tokens are issued yet.
        return { ok: true, ...data };
      }
      await new Promise((r) => setTimeout(r, 750));
      persistDemo({ email: payload.email, firstName: payload.firstName || 'New', lastName: payload.lastName || 'Customer' });
      return { ok: true, success: true, requiresActivation: false, message: 'Account created.' };
    },
    logout: async () => {
      if (apiEnabled) {
        try { await authApi.logout({ refreshToken: localStorage.getItem('fetch_refresh_token') }); } catch {}
        clearTokens(); setUser(null); return;
      }
      persistDemo(null);
    },
    updateProfile: async (patch) => {
      if (apiEnabled) { const next = await authApi.updateMe(patch); setUser(next); return { ok: true }; }
      await new Promise((r) => setTimeout(r, 500)); persistDemo({ ...(user || {}), ...patch }); return { ok: true };
    },
  }), [user, hydrating]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
