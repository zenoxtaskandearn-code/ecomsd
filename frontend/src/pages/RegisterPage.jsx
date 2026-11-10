import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const inputClass =
  'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

export default function RegisterPage() {
  const { isAuthenticated, register } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });

  if (isAuthenticated) return <Navigate to="/account" replace />;

  const update = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    await register(form);
    navigate('/account', { replace: true });
  };

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-16">
        <div className="mx-auto max-w-[480px]">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.45rem]">Create account</h1>
          <p className="mt-3 text-[15px] leading-[1.75] text-fetch-muted">
            Demo registration for the frontend. Connect to secure backend registration, password hashing, and email flows in a later phase.
          </p>
          <form className="mt-8 space-y-4" onSubmit={onSubmit}>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">First name</span><input required className={`${inputClass} mt-1.5`} value={form.firstName} onChange={update('firstName')} autoComplete="given-name" /></label>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Last name</span><input required className={`${inputClass} mt-1.5`} value={form.lastName} onChange={update('lastName')} autoComplete="family-name" /></label>
            </div>
            <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Email</span><input required type="email" className={`${inputClass} mt-1.5`} value={form.email} onChange={update('email')} autoComplete="email" /></label>
            <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Password</span><input required type="password" className={`${inputClass} mt-1.5`} value={form.password} onChange={update('password')} autoComplete="new-password" /></label>
            <div className="pt-2">
              <Button type="submit" loading={loading} className="w-full sm:w-auto sm:min-w-[180px]">Create account</Button>
            </div>
          </form>
          <p className="mt-6 text-[14px] text-fetch-muted">
            Already have an account? <Link to="/login" className="font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Sign in</Link>
          </p>
        </div>
      </div>
    </StoreLayout>
  );
}
