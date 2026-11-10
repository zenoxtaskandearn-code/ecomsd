import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Check, Truck, Package, Clock, ExternalLink, Info } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import AccountShell from '../components/account/AccountShell.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { getDemoOrder } from '../utils/accountDemo.js';
import { apiEnabled } from '../services/api.js';
import * as orderApi from '../services/orderApi.js';

function fmtDate(v) {
  if (!v) return null;
  if (typeof v === 'string' && !v.includes('T') && !/^\d{4}-\d{2}-\d{2}/.test(v)) return v;
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return String(v);
  return d.toLocaleString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
function labelStatus(s) {
  if (!s) return '—';
  return String(s).replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
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
  const fulfillment = order?.fulfillment;
  const timeline = useMemo(() => {
    const t = order?.timeline;
    if (Array.isArray(t) && t.length) return t;
    // fallback
    const steps = ['PENDING_PAYMENT','PAID','PROCESSING','FULFILLMENT_SUBMITTED','SHIPPED','DELIVERED'];
    const idx = steps.indexOf(order?.status);
    const labels = { PENDING_PAYMENT: 'Awaiting payment', PAID: 'Payment confirmed', PROCESSING: 'Processing', FULFILLMENT_SUBMITTED: 'Submitted for fulfillment', SHIPPED: 'Shipped', DELIVERED: 'Delivered' };
    return steps.map((s, i) => ({ key: s, label: labels[s], done: idx >= i, current: idx === i, at: idx === i ? 'Current' : idx > i ? 'Completed' : 'Pending', note: '' }));
  }, [order]);

  return (
    <StoreLayout>
      <AccountShell title={order ? `Order ${order.orderNumber}` : 'Order details'} description="Live order status, fulfillment, shipment tracking, timeline, and delivery details.">
        {loading ? (
          <div className="space-y-4"><Skeleton className="h-28 w-full" /><Skeleton className="h-48 w-full" /><Skeleton className="h-36 w-full" /></div>
        ) : !order ? (
          <EmptyState title="Order not found" message={error || 'We couldn’t find that order.'} actionLabel="Back to orders" onAction={() => (window.location.href = '/account/orders')} />
        ) : (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Status</p><p className="mt-2 text-[15px] font-medium text-fetch-ink">{labelStatus(order.status)}</p></div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Placed</p><p className="mt-2 text-[14px] font-medium text-fetch-ink">{fmtDate(order.placedAt || order.placed) || '—'}</p></div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Paid</p><p className="mt-2 text-[14px] font-medium text-fetch-ink">{fmtDate(order.paidAt) || '—'}</p></div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4"><p className="text-[12px] uppercase tracking-[0.14em] text-fetch-ink/45">Total</p><p className="mt-2 text-[15px] font-medium tabular-nums text-fetch-ink">${total.toFixed(2)} <span className="text-[12px] font-normal text-fetch-muted">{order.currency || 'USD'}</span></p></div>
            </div>

            {(fulfillment || shipments.length > 0) && (
              <div className="grid gap-4 lg:grid-cols-2">
                <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
                  <div className="flex items-center gap-2"><Package size={18} className="text-fetch-ink/70" /><h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Fulfillment & supplier</h2></div>
                  <dl className="mt-4 grid grid-cols-1 gap-x-4 gap-y-3 text-[14px] sm:grid-cols-2">
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Supplier / fulfillment partner</dt><dd className="mt-1 font-medium text-fetch-ink">{fulfillment?.supplierName || 'Fulfillment partner'}</dd></div>
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Fulfillment status</dt><dd className="mt-1 text-fetch-ink">{fulfillment?.fulfillmentStatus || labelStatus(order.status)}</dd></div>
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Supplier reference</dt><dd className="mt-1 text-fetch-ink">{fulfillment?.supplierOrderId || '—'}</dd></div>
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Submitted for fulfillment</dt><dd className="mt-1 text-fetch-ink">{fmtDate(fulfillment?.submittedAt) || '—'}</dd></div>
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Expected delivery</dt><dd className="mt-1 font-medium text-fetch-ink">{fulfillment?.expectedDelivery || 'To be confirmed'}</dd></div>
                    <div><dt className="text-[12px] uppercase tracking-[0.12em] text-fetch-ink/45">Last updated</dt><dd className="mt-1 text-fetch-ink">{fmtDate(fulfillment?.lastUpdatedAt) || '—'}</dd></div>
                  </dl>
                  {fulfillment?.statusMessage && <p className="mt-4 rounded-lg bg-[#f8fafc] p-3 text-[14px] leading-[1.7] text-fetch-muted"><span className="font-medium text-fetch-ink/80">Update: </span>{fulfillment.statusMessage}</p>}
                  {fulfillment?.lastError && <p className="mt-2 text-[13px] text-amber-800"><span className="font-medium">Fulfillment note:</span> {fulfillment.lastError}</p>}
                </div>

                <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
                  <div className="flex items-center gap-2"><Truck size={18} className="text-fetch-ink/70" /><h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Shipment tracking</h2></div>
                  {shipments.length > 0 ? (
                    <div className="mt-4 space-y-4">
                      {shipments.map((s, i) => (
                        <div key={i} className="rounded-xl border border-fetch-ink/[0.08] bg-[#FBFAF8] p-4 text-[14px]">
                          <div className="flex flex-wrap items-start justify-between gap-2">
                            <div>
                              <p className="font-medium text-fetch-ink">{s.carrier || 'Carrier pending'}</p>
                              <p className="mt-0.5 text-fetch-muted">Tracking #: <span className="font-medium text-fetch-ink/80">{s.trackingNumber || 'Pending'}</span></p>
                            </div>
                            <span className={`inline-flex rounded-full px-2.5 py-1 text-[12px] font-medium ${String(s.status).toUpperCase() === 'DELIVERED' ? 'bg-emerald-50 text-emerald-700' : String(s.status).toUpperCase() === 'SHIPPED' ? 'bg-blue-50 text-blue-700' : 'bg-[#eef2ff] text-[#3730a3]'}`}>{labelStatus(s.status || 'Pending')}</span>
                          </div>
                          <dl className="mt-3 grid grid-cols-2 gap-2 text-[13px]">
                            <div><dt className="text-fetch-ink/45">Shipped</dt><dd className="text-fetch-ink">{fmtDate(s.shippedAt) || '—'}</dd></div>
                            <div><dt className="text-fetch-ink/45">Delivered</dt><dd className="text-fetch-ink">{fmtDate(s.deliveredAt) || '—'}</dd></div>
                            <div><dt className="text-fetch-ink/45">Last tracked</dt><dd className="text-fetch-ink">{fmtDate(s.lastTrackedAt) || '—'}</dd></div>
                            <div><dt className="text-fetch-ink/45">Source</dt><dd className="text-fetch-ink">{s.source || '—'}</dd></div>
                          </dl>
                          {s.trackingUrl && <a className="mt-3 inline-flex items-center gap-1 text-[13px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4" href={s.trackingUrl} target="_blank" rel="noreferrer">Track package <ExternalLink size={13} /></a>}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="mt-4 rounded-xl border border-dashed border-fetch-ink/15 bg-[#FBFAF8] p-4 text-[14px] leading-[1.75] text-fetch-muted">
                      <p><span className="font-medium text-fetch-ink/80">No shipment yet.</span> Tracking number, carrier, and shipping dates will appear here as soon as your order is shipped.</p>
                      {fulfillment?.expectedDelivery && <p className="mt-2"><Clock size={14} className="mr-1 inline -mt-0.5" />Expected delivery: <span className="font-medium text-fetch-ink/80">{fulfillment.expectedDelivery}</span></p>}
                    </div>
                  )}
                </div>
              </div>
            )}

            <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
              <div className="flex items-center gap-2"><Clock size={18} className="text-fetch-ink/70" /><h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Order timeline</h2></div>
              <ol className="mt-5 space-y-4">
                {timeline.map((step, idx) => (
                  <li key={step.key || step.label || idx} className="flex gap-3 text-[14px]">
                    <span className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${step.done ? 'bg-fetch-dark text-white' : 'border border-fetch-ink/15 text-transparent'}`}><Check size={13} strokeWidth={2.5} aria-hidden="true" /></span>
                    <div className="min-w-0">
                      <p className={step.done || step.current ? 'font-medium text-fetch-ink' : 'text-fetch-muted'}>{step.label}{step.current && !step.done ? ' (current)' : ''}</p>
                      <p className="text-[13px] text-fetch-muted">{fmtDate(step.at) || (step.at || (step.done ? 'Completed' : 'Pending'))}{step.note ? ` · ${step.note}` : ''}</p>
                    </div>
                  </li>
                ))}
              </ol>
              {fulfillment?.expectedDelivery && <p className="mt-5 inline-flex items-start gap-2 rounded-lg bg-[#f8fafc] p-3 text-[13px] text-fetch-muted"><Info size={15} className="mt-0.5 shrink-0" /><span><span className="font-medium text-fetch-ink/80">Expected delivery:</span> {fulfillment.expectedDelivery}. Estimates may change once the carrier provides tracking updates.</span></p>}
            </div>

            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
              <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
                <h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Items</h2>
                <ul className="mt-4 divide-y divide-fetch-ink/[0.08]">
                  {items.map((item, idx) => (
                    <li key={idx} className="flex items-start justify-between gap-4 py-3 text-[14px]">
                      <div><p className="font-medium text-fetch-ink">{item.productName || item.name}</p><p className="text-fetch-muted">{item.variantTitle || item.variant || ''}{(item.variantTitle || item.variant) ? ' · ' : ''}SKU {item.sku || '—'} · Qty {item.quantity ?? item.qty}</p></div>
                      <div className="text-right"><p className="tabular-nums text-fetch-ink">${Number(item.lineTotal ?? (Number(item.unitPrice ?? item.price) * Number(item.quantity ?? item.qty))).toFixed(2)}</p><p className="text-[12px] tabular-nums text-fetch-muted">${Number(item.unitPrice ?? item.price ?? 0).toFixed(2)} each</p></div>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="space-y-4">
                <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5">
                  <h2 className="text-[1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Shipping to</h2>
                  <address className="mt-3 text-[14px] not-italic leading-[1.75] text-fetch-muted">
                    {addr?.fullName || addr?.name}<br />{addr?.line1}{addr?.line2 ? <><br />{addr.line2}</> : null}<br />{[addr?.city, addr?.state].filter(Boolean).join(', ')} {addr?.postalCode}<br />{addr?.countryCode || addr?.country || 'US'}{addr?.phone ? <><br />{addr.phone}</> : null}
                  </address>
                </div>
                <div className="border border-fetch-ink/[0.08] p-5 text-[14px] text-fetch-muted">
                  <h3 className="font-medium text-fetch-ink">Payment summary</h3>
                  <div className="mt-3 space-y-1.5">
                    <div className="flex justify-between"><span>Subtotal</span><span className="tabular-nums text-fetch-ink">${Number(order.subtotalAmount ?? total).toFixed(2)}</span></div>
                    <div className="flex justify-between"><span>Shipping</span><span className="tabular-nums text-fetch-ink">${Number(order.shippingAmount ?? 0).toFixed(2)}</span></div>
                    <div className="flex justify-between"><span>Tax</span><span className="tabular-nums text-fetch-ink">${Number(order.taxAmount ?? 0).toFixed(2)}</span></div>
                    {Number(order.discountAmount ?? 0) > 0 && <div className="flex justify-between"><span>Discount</span><span className="tabular-nums text-fetch-ink">-${Number(order.discountAmount).toFixed(2)}</span></div>}
                    <div className="flex justify-between border-t border-fetch-ink/[0.08] pt-2 font-medium text-fetch-ink"><span>Total</span><span className="tabular-nums">${total.toFixed(2)}</span></div>
                  </div>
                </div>
              </div>
            </div>
            <Link to="/account/orders" className="inline-block text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Back to order history</Link>
          </div>
        )}
      </AccountShell>
    </StoreLayout>
  );
}
