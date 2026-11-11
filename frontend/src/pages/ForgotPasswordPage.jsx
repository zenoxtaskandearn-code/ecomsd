import { useState } from 'react';
import { Link } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import * as authApi from '../services/authApi.js';

const inputClass = 'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); setError(''); setResult(null);
    try {
      const res = await authApi.forgotPassword({ email });
      setResult(res?.message || 'If an account exists for this email, a password reset link has been sent.');
    } catch (err) {
      setError(err?.response?.data?.error?.message || err?.message || 'Unable to process request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-16">
        <div className="mx-auto max-w-[480px]">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink">Forgot password</h1>
          <p className="mt-3 text-[15px] leading-[1.75] text-fetch-muted">Enter your email address and we’ll send a one-time password reset link if an account exists.</p>
          {result ? (
            <div className="mt-8 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-[14px] leading-[1.7] text-emerald-900">
              <p>{result}</p>
              <p className="mt-2 text-emerald-800/90">The reset link is valid for a limited time and can be used once.</p>
            </div>
          ) : (
            <form className="mt-8 space-y-4" onSubmit={onSubmit}>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Email</span><input required type="email" className={`${inputClass} mt-1.5`} value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" /></label>
              {error ? <p className="text-[13px] text-red-700" role="alert">{error}</p> : null}
              <div className="pt-2"><Button type="submit" loading={loading}>Send reset link</Button></div>
            </form>
          )}
          <p className="mt-6 text-[14px] text-fetch-muted"><Link to="/login" className="font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Back to sign in</Link></p>
        </div>
      </div>
    </StoreLayout>
  );
}
