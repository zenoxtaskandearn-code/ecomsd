import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAdminAuth } from '../../context/AdminAuthContext.jsx';
import { AdminButton } from '../../components/admin/AdminLayout.jsx';
import { apiEnabled } from '../../services/api.js';

const inputClass = 'h-10 w-full rounded-lg border border-[#d1d5db] bg-white px-3 text-[14px] text-[#111827] focus:border-[#9ca3af] focus:outline-none';

export default function AdminLoginPage() {
  const { isAuthenticated, login } = useAdminAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ email: '', password: '' });
  const from = location.state?.from?.pathname || '/admin';

  if (isAuthenticated) return <Navigate to="/admin" replace />;

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      await login(form);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err?.message || err?.response?.data?.error?.message || 'Sign in failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f1f1f1] p-4">
      <div className="w-full max-w-[420px] rounded-2xl border border-[#e5e7eb] bg-white p-6 shadow-[0_1px_2px_rgba(16,24,40,0.04)] sm:p-8">
        <p className="text-[12px] font-medium uppercase tracking-[0.14em] text-[#6b7280]">Lupine &amp; Leash Admin</p>
        <h1 className="mt-2 text-[1.6rem] font-semibold tracking-[-0.03em] text-[#111827]">Sign in</h1>
        <p className="mt-2 text-[14px] leading-6 text-[#6b7280]">{apiEnabled ? 'Sign in with an ADMIN or STAFF account.' : 'Demo admin access (no backend configured).'}</p>
        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <label className="block"><span className="text-[13px] font-medium text-[#374151]">Email</span><input required type="email" className={`${inputClass} mt-1.5`} value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} /></label>
          <label className="block"><span className="text-[13px] font-medium text-[#374151]">Password</span><input required type="password" className={`${inputClass} mt-1.5`} value={form.password} onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} /></label>
          {error && <p className="text-[13px] text-red-700">{error}</p>}
          <AdminButton type="submit" loading={loading} className="w-full">Sign in to admin</AdminButton>
        </form>
      </div>
    </div>
  );
}
