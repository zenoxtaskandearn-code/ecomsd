import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Check } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import AccountShell from '../components/account/AccountShell.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { getDemoOrder } from '../utils/accountDemo.js';
import { apiEnabled } from '../services/api.js';
import * as orderApi from '../services/orderApi.js';

const steps = ['PENDING_PAYMENT','PAID','PROCESSING','FULFILLMENT_SUBMITTED','SHIPPED','DELIVERED'];
function timelineFor(status) {
  const idx = steps.indexOf(status);
  const labels = { PENDING_PAYMENT: 'Awaiting payment', PAID: 'Paid', PROCESSING: 'Processing', FULFILLMENT_SUBMITTED: 'Submitted for fulfillment', SHIPPED: 'Shipped', DELIVERED: 'Delivered' };
  return steps.map((s, i) => ({ label: labels[s], done: idx >= i, at: idx === i ? 'Current' : idx > i ? 'Completed' : 'Pending' }));
}

export default function AccountOrderDetailPage() {
  const { orderNumber } = useParams();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        if (apiEnabled) setOrder(await orderApi.getMine(orderNumber));
        else setOrder(getDemoOrder(orderNumber));
      } catch (e) { setError(e?.response?.data?.error?.message || 'Order not found.'); }
      finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, [orderNumber]);

  const addr = order?.shippingAddress || order?.shipping;
  const items = order?.items || [];
  const shipments = order?.shipments || [];
  const total = Number(order?.total ?? order?.totalAmount ?? 0);

  return (
    <StoreLayout>
      <AccountShell title={order ? `Order ${order.orderNumber}` : 'Order details'} description="Status, items, shipping address, and tracking information.">
        {loading ? (
          <div className="space-y-4"><Skeleton className="h-28 w-full" /><Skeleton className="h-48 w-full" /><Skeleton className="h-36 w-full" /></div>
        ) : !order ? (
          <EmptyState title="Order not found" message={error || 'We couldn’t find that order.'} actionLabel="Back to orders" onAction={() => (window.location.href = '/account/orders')} />
        ) : (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Status</p><p className="mt-2 text-[15px] font-medium text-fetch-ink">{order.status}</p></div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Placed</p><p className="mt-2 text-[15px] font-medium text-fetch-ink">{order.placedAt ? new Date(order.placedAt).toLocaleString() : (order.placed || '—')}</p></div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Total</p><p className="mt-2 text-[15px] font-medium tabular-nums text-fetch-ink">${total.toFixed(2)}</p></div>
            </div>
            <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
              <h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Fulfillment</h2>
              {shipments.length > 0 ? (
                <div className="mt-3 space-y-3 text-[14px] leading-[1.75] text-fetch-muted">
                  {shipments.map((s, i) => (<div key={i}><p><span className="font-medium text-fetch-ink/80">{s.carrier || 'Carrier'}</span>{s.trackingNumber ? ` · ${s.trackingNumber}` : ''}</p><p className="text-fetch-ink/80">{s.status}{s.trackingUrl ? <> · <a className="underline decoration-fetch-ink/20" href={s.trackingUrl} target="_blank" rel="noreferrer">Track</a></> : ''}</p></div>))}
                </div>
              ) : order.tracking ? (
                <p className="mt-2 text-[14px] leading-[1.75] text-fetch-muted">{order.tracking.carrier} · {order.tracking.number}<br /><span className="text-fetch-ink/80">{order.tracking.status}</span> — {order.tracking.eta}</p>
              ) : (
                <p className="mt-2 text-[14px] leading-[1.75] text-fetch-muted">Tracking details will appear here after your order ships.</p>
              )}
              <ol className="mt-5 space-y-3">
                {(order.timeline || timelineFor(order.status)).map((step) => (
                  <li key={step.label} className="flex gap-3 text-[14px]">
                    <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${step.done ? 'bg-fetch-dark text-white' : 'border border-fetch-ink/15 text-transparent'}`}><Check size={12} strokeWidth={2.5} aria-hidden="true" /></span>
                    <div><p className={step.done ? 'font-medium text-fetch-ink' : 'text-fetch-muted'}>{step.label}</p><p className="text-[13px] text-fetch-muted">{step.at}{step.note ? ` · ${step.note}` : ''}</p></div>
                  </li>
                ))}
              </ol>
            </div>
            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
              <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
                <h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Items</h2>
                <ul className="mt-4 divide-y divide-fetch-ink/[0.08]">
                  {items.map((item, idx) => (
                    <li key={idx} className="flex items-start justify-between gap-4 py-3 text-[14px]">
                      <div><p className="font-medium text-fetch-ink">{item.productName || item.name}</p><p className="text-fetch-muted">{item.variantTitle || item.variant || ''} · Qty {item.quantity ?? item.qty}</p></div>
                      <p className="tabular-nums text-fetch-ink">${Number(item.lineTotal ?? (Number(item.unitPrice ?? item.price) * Number(item.quantity ?? item.qty))).toFixed(2)}</p>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5">
                <h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Shipping to</h2>
                <address className="mt-3 text-[14px] not-italic leading-[1.75] text-fetch-muted">
                  {addr?.fullName || addr?.name}<br />{addr?.line1}<br />{addr?.city}, {addr?.state} {addr?.postalCode}<br />{addr?.countryCode || addr?.country || 'US'}
                </address>
              </div>
            </div>
            <Link to="/account/orders" className="inline-block text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Back to order history</Link>
          </div>
        )}
      </AccountShell>
    </StoreLayout>
  );
}
