import { Link, useLocation } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';

export default function OrderConfirmationPage() {
  const { state } = useLocation();
  const orderNumber = state?.orderNumber || 'FT-PENDING';
  const email = state?.email;
  const total = state?.total;
  const demo = state?.demo;
  const status = state?.status;

  return (
    <StoreLayout>
      <div className="section-container py-14 sm:py-20">
        <div className="mx-auto max-w-[560px] text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-fetch-lavender-soft text-fetch-ink/70">
            <CheckCircle2 size={26} strokeWidth={1.5} aria-hidden="true" />
          </div>
          <p className="mt-6 text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Order confirmation</p>
          <h1 className="mt-3 text-[2rem] font-semibold leading-[1.06] tracking-[-0.03em] text-fetch-ink sm:text-[2.4rem]">Thank you for your order</h1>
          <p className="mt-4 text-[15px] leading-[1.75] text-fetch-muted sm:text-[16px]">
            {demo
              ? 'This is a frontend demo confirmation. No real payment was captured and no supplier order was submitted.'
              : status === 'PAID'
                ? 'Your payment was captured and your order is confirmed. We’ll share updates as fulfillment progresses.'
                : 'Your order has been received. Final status will be confirmed once payment and server verification complete.'}
          </p>
          <div className="mt-8 border border-fetch-ink/[0.08] bg-[#FBFAF8] px-6 py-5 text-left">
            <dl className="space-y-3 text-[14px]">
              <div className="flex justify-between gap-4"><dt className="text-fetch-muted">Order reference</dt><dd className="font-medium text-fetch-ink">{orderNumber}</dd></div>
              {status && <div className="flex justify-between gap-4"><dt className="text-fetch-muted">Status</dt><dd className="font-medium text-fetch-ink">{status}</dd></div>}
              {email && <div className="flex justify-between gap-4"><dt className="text-fetch-muted">Contact</dt><dd className="text-fetch-ink">{email}</dd></div>}
              {typeof total === 'number' && <div className="flex justify-between gap-4"><dt className="text-fetch-muted">Total</dt><dd className="font-medium tabular-nums text-fetch-ink">${Number(total).toFixed(2)}</dd></div>}
            </dl>
          </div>
          <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link to="/shop" className="btn-primary min-w-[180px]">Continue shopping</Link>
            <Link to={demo ? '/' : '/account/orders'} className="inline-flex h-11 items-center justify-center px-4 text-[15px] font-medium text-fetch-ink/75 underline decoration-fetch-ink/15 underline-offset-4 transition hover:text-fetch-ink">{demo ? 'Back to home' : 'View orders'}</Link>
          </div>
        </div>
      </div>
    </StoreLayout>
  );
}
