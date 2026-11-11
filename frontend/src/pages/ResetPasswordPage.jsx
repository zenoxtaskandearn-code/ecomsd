import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import * as authApi from '../services/authApi.js';

const inputClass = 'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = useMemo(() => params.get('token') || '', [params]);
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(''); setResult(null);
    if (!token) { setError('Reset token is missing. Please use the link from your email.'); return; }
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return; }
    if (form.password !== form.confirm) { setError('Passwords do not match.'); return; }
    setLoading(true);
    try {
      const res = await authApi.resetPassword({ token, newPassword: form.password });
      setResult(res?.message || 'Password reset successfully. You can now sign in.');
    } catch (err) {
      setError(err?.response?.data?.error?.message || err?.message || 'Reset link is invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-16">
        <div className="mx-auto max-w-[480px]">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink">Reset password</h1>
          <p className="mt-3 text-[15px] leading-[1.75] text-fetch-muted">Create a new password for your account. This one-time link expires after a short period.</p>
          {result ? (
            <div className="mt-8 rounded-xl border border-emerald-200 bg-emerald-50 p-5">
              <p className="text-[14px] leading-[1.7] text-emerald-900">{result}</p>
              <div className="mt-4"><Link to="/login" className="inline-flex h-10 items-center justify-center border border-fetch-ink/15 px-4 text-[14px] font-medium text-fetch-ink hover:border-fetch-ink/25">Go to sign in</Link></div>
            </div>
          ) : (
            <form className="mt-8 space-y-4" onSubmit={onSubmit}>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">New password</span><input required minLength={8} type="password" className={`${inputClass} mt-1.5`} value={form.password} onChange={(e) => setForm(f => ({ ...f, password: e.target.value }))} autoComplete="new-password" /></label>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Confirm new password</span><input required minLength={8} type="password" className={`${inputClass} mt-1.5`} value={form.confirm} onChange={(e) => setForm(f => ({ ...f, confirm: e.target.value }))} autoComplete="new-password" /></label>
              {!token && <p className="text-[13px] text-amber-800">Reset token missing from URL. Open the link from your email.</p>}
              {error ? <p className="text-[13px] text-red-700" role="alert">{error}</p> : null}
              <div className="pt-2"><Button type="submit" loading={loading} disabled={!token}>Reset password</Button></div>
            </form>
          )}
          <p className="mt-6 text-[14px] text-fetch-muted"><Link to="/forgot-password" className="font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Request a new reset link</Link></p>
        </div>
      </div>
    </StoreLayout>
  );
}
