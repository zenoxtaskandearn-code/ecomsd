import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const inputClass =
  'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ email: '', password: '' });
  const from = location.state?.from?.pathname || '/account';

  if (isAuthenticated) return <Navigate to="/account" replace />;

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login({ email: form.email, password: form.password });
      navigate(from, { replace: true });
    } catch (err) {
      setError(err?.response?.data?.error?.message || err?.message || 'Unable to sign in. Check your email and password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-16">
        <div className="mx-auto max-w-[440px]">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.45rem]">Sign in</h1>
          <p className="mt-3 text-[15px] leading-[1.75] text-fetch-muted">
            Sign in to access your account, cart, and orders.
          </p>
          <form className="mt-8 space-y-4" onSubmit={onSubmit}>
            <label className="block">
              <span className="text-[13px] font-medium text-fetch-ink/80">Email</span>
              <input required type="email" autoComplete="email" className={`${inputClass} mt-1.5`} value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} />
            </label>
            <label className="block">
              <div className="flex items-center justify-between gap-3">
                <span className="text-[13px] font-medium text-fetch-ink/80">Password</span>
                <Link to="/forgot-password" className="text-[13px] font-medium text-fetch-ink/70 underline decoration-fetch-ink/20 underline-offset-4 hover:text-fetch-ink">Forgot password?</Link>
              </div>
              <input required type="password" autoComplete="current-password" className={`${inputClass} mt-1.5`} value={form.password} onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} />
            </label>
            {error ? <p className="text-[13px] text-red-700" role="alert">{error}</p> : null}
            <div className="pt-2">
              <Button type="submit" loading={loading} className="w-full sm:w-auto sm:min-w-[160px]">Sign in</Button>
            </div>
          </form>
          <p className="mt-6 text-[14px] text-fetch-muted">
            New to Lupine &amp; Leash? <Link to="/register" className="font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Create an account</Link>
          </p>
          <p className="mt-2 text-[13px] text-fetch-muted">
            Didn’t receive activation email? <Link to="/resend-activation" className="font-medium text-fetch-ink/80 underline decoration-fetch-ink/20 underline-offset-4">Resend activation link</Link>
          </p>
        </div>
      </div>
    </StoreLayout>
  );
}
