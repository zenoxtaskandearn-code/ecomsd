import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { apiEnabled, clearTokens, setTokens } from '../services/api.js';
import * as authApi from '../services/authApi.js';

const AdminAuthContext = createContext(null);
const STORAGE_KEY = 'fetch_demo_admin_auth';

function readStored() { try { const raw = localStorage.getItem(STORAGE_KEY); return raw ? JSON.parse(raw) : null; } catch { return null; } }

export function AdminAuthProvider({ children }) {
  const [admin, setAdmin] = useState(() => (apiEnabled ? null : readStored()));
  const [hydrating, setHydrating] = useState(apiEnabled);

  useEffect(() => {
    if (!apiEnabled) return;
    let mounted = true;
    (async () => {
      try {
        const refreshToken = localStorage.getItem('fetch_refresh_token');
        if (refreshToken) {
          const data = await authApi.refresh({ refreshToken });
          if (data?.user && ['ADMIN', 'STAFF'].includes(data.user.role)) {
            setTokens(data);
            if (mounted) setAdmin({ email: data.user.email, name: [data.user.firstName, data.user.lastName].filter(Boolean).join(' ') || 'Admin', role: data.user.role, user: data.user });
          } else {
            clearTokens();
          }
        }
      } catch { clearTokens(); }
      finally { if (mounted) setHydrating(false); }
    })();
    return () => { mounted = false; };
  }, []);

  const persist = (next) => { setAdmin(next); try { if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); else localStorage.removeItem(STORAGE_KEY); } catch {} };

  const value = useMemo(() => ({
    admin, hydrating, isAuthenticated: !!admin,
    login: async ({ email, password }) => {
      if (apiEnabled) {
        const data = await authApi.login({ email, password });
        if (!['ADMIN', 'STAFF'].includes(data?.user?.role)) { clearTokens(); throw new Error('This account does not have admin access.'); }
        setTokens(data);
        setAdmin({ email: data.user.email, name: [data.user.firstName, data.user.lastName].filter(Boolean).join(' ') || 'Admin', role: data.user.role, user: data.user });
        return { ok: true };
      }
      await new Promise((r) => setTimeout(r, 650));
      persist({ email, name: 'Admin', role: 'ADMIN' });
      return { ok: true };
    },
    logout: async () => {
      if (apiEnabled) { try { await authApi.logout({ refreshToken: localStorage.getItem('fetch_refresh_token') }); } catch {} clearTokens(); setAdmin(null); return; }
      persist(null);
    },
  }), [admin, hydrating]);

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>;
}

export function useAdminAuth() {
  const ctx = useContext(AdminAuthContext);
  if (!ctx) throw new Error('useAdminAuth must be used within AdminAuthProvider');
  return ctx;
}
