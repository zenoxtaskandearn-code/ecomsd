import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import * as authApi from '../services/authApi.js';

export default function ActivateAccountPage() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [state, setState] = useState({ loading: true, ok: false, message: '' });

  useEffect(() => {
    let mounted = true;
    (async () => {
      if (!token) {
        if (mounted) setState({ loading: false, ok: false, message: 'Activation token is missing. Please use the link from your email.' });
        return;
      }
      try {
        const res = await authApi.verifyEmail({ token });
        if (mounted) setState({ loading: false, ok: true, message: res?.message || 'Account activated successfully. You can now sign in.' });
      } catch (e) {
        if (mounted) setState({ loading: false, ok: false, message: e?.response?.data?.error?.message || e?.message || 'Activation link is invalid or expired.' });
      }
    })();
    return () => { mounted = false; };
  }, [token]);

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-16">
        <div className="mx-auto max-w-[520px]">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink">Activate account</h1>
          <div className={`mt-8 rounded-xl border p-5 ${state.loading ? 'border-fetch-ink/10 bg-[#FBFAF8]' : state.ok ? 'border-emerald-200 bg-emerald-50' : 'border-red-200 bg-red-50'}`}>
            {state.loading ? (
              <div className="flex items-center gap-3 text-[14px] text-fetch-muted">
                <span className="h-5 w-5 animate-spin rounded-full border-2 border-fetch-ink/20 border-t-fetch-ink" aria-hidden="true" />
                Activating your account…
              </div>
            ) : (
              <p className={`text-[14px] leading-[1.7] ${state.ok ? 'text-emerald-900' : 'text-red-800'}`}>{state.message}</p>
            )}
          </div>
          {!state.loading && (
            <div className="mt-5 flex flex-wrap gap-3">
              <Link to="/login" className="inline-flex h-10 items-center justify-center border border-fetch-ink/15 px-4 text-[14px] font-medium text-fetch-ink hover:border-fetch-ink/25">Go to sign in</Link>
              {!state.ok && <Link to="/resend-activation" className="inline-flex h-10 items-center justify-center px-1 text-[14px] font-medium text-fetch-ink/75 underline decoration-fetch-ink/20 underline-offset-4">Resend activation link</Link>}
            </div>
          )}
        </div>
      </div>
    </StoreLayout>
  );
}
