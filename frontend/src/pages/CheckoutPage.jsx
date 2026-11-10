import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { apiEnabled } from '../services/api.js';
import * as orderApi from '../services/orderApi.js';
import * as paymentApi from '../services/paymentApi.js';

function Field({ label, id, children }) {
  return (<label htmlFor={id} className="block"><span className="text-[13px] font-medium text-fetch-ink/80">{label}</span><div className="mt-1.5">{children}</div></label>);
}
const inputClass = 'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

function loadPayPalSdk(clientId, currency = 'USD') {
  if (window.paypal) return Promise.resolve(window.paypal);
  const existing = document.querySelector('script[data-paypal-sdk="true"]');
  if (existing) {
    return new Promise((resolve, reject) => { existing.addEventListener('load', () => resolve(window.paypal)); existing.addEventListener('error', reject); });
  }
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(clientId)}&currency=${encodeURIComponent(currency)}&intent=capture&components=buttons`;
    s.async = true; s.dataset.paypalSdk = 'true';
    s.onload = () => resolve(window.paypal);
    s.onerror = () => reject(new Error('Failed to load PayPal SDK'));
    document.body.appendChild(s);
  });
}

export default function CheckoutPage() {
  const { items, subtotal, clearCart, refresh } = useCart();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const paypalRef = useRef(null);
  const [placingDemo, setPlacingDemo] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [paypalConfig, setPaypalConfig] = useState(null);
  const [createdOrder, setCreatedOrder] = useState(null);
  const [paypalReady, setPaypalReady] = useState(false);
  const [form, setForm] = useState({ email: '', firstName: '', lastName: '', address1: '', address2: '', city: '', state: '', postalCode: '', country: 'US', phone: '' });

  useEffect(() => {
    if (user?.email && !form.email) setForm((f) => ({ ...f, email: user.email, firstName: user.firstName || f.firstName, lastName: user.lastName || f.lastName }));
  }, [user]); // eslint-disable-line

  useEffect(() => {
    if (!apiEnabled) return;
    paymentApi.getPayPalConfig().then(setPaypalConfig).catch(() => setPaypalConfig({ enabled: false }));
  }, []);

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const shipping = 0;
  const total = useMemo(() => Number(createdOrder?.totalAmount ?? subtotal) + shipping, [subtotal, shipping, createdOrder]);
  const addressSnapshot = { fullName: `${form.firstName} ${form.lastName}`.trim(), line1: form.address1, line2: form.address2 || null, city: form.city, state: form.state, postalCode: form.postalCode, countryCode: (form.country || 'US').slice(0,2).toUpperCase(), phone: form.phone || null };

  const placeDemo = (e) => {
    e.preventDefault();
    setPlacingDemo(true);
    window.setTimeout(() => { const orderNumber = `FT-DEMO-${Math.floor(100000 + Math.random() * 900000)}`; clearCart(); navigate('/order-confirmation', { state: { orderNumber, email: form.email, total, demo: true } }); }, 1100);
  };

  const createServerOrder = async () => {
    setBusy(true); setError('');
    try {
      const order = await orderApi.create({ shippingAddress: addressSnapshot, billingAddress: null, customerEmail: form.email });
      setCreatedOrder(order);
      await refresh?.();
      return order;
    } catch (e) {
      setError(e?.response?.data?.error?.message || 'Could not create order. Please check your cart and details.');
      return null;
    } finally { setBusy(false); }
  };

  useEffect(() => {
    if (!apiEnabled || !createdOrder || !paypalConfig?.enabled || !paypalConfig?.clientId || !paypalRef.current) return;
    let cancelled = false;
    (async () => {
      try {
        const paypal = await loadPayPalSdk(paypalConfig.clientId, paypalConfig.currency || 'USD');
        if (cancelled || !paypalRef.current) return;
        paypalRef.current.innerHTML = '';
        paypal.Buttons({
          style: { layout: 'vertical', shape: 'rect', label: 'paypal' },
          createOrder: async () => {
            const res = await paymentApi.createPayPalOrder(createdOrder.orderNumber);
            return res.paypalOrderId;
          },
          onApprove: async (data) => {
            setBusy(true); setError('');
            try {
              const res = await paymentApi.capturePayPalOrder(createdOrder.orderNumber, data.orderID);
              navigate('/order-confirmation', { state: { orderNumber: res.orderNumber || createdOrder.orderNumber, email: createdOrder.customerEmail || form.email, total: Number(createdOrder.totalAmount), status: res.status } });
            } catch (e) {
              setError(e?.response?.data?.error?.message || 'Payment capture failed. If you were charged, contact support with your order reference.');
            } finally { setBusy(false); }
          },
          onError: (err) => setError('PayPal checkout encountered an error. Please try again.'),
        }).render(paypalRef.current).then(() => !cancelled && setPaypalReady(true)).catch(() => setError('Could not render PayPal buttons.'));
      } catch (e) {
        if (!cancelled) setError('PayPal SDK could not be loaded. Check your connection and PayPal configuration.');
      }
    })();
    return () => { cancelled = true; };
  }, [createdOrder, paypalConfig, navigate, form.email]);

  if (items.length === 0 && !createdOrder) return <Navigate to="/cart" replace />;
  if (apiEnabled && !isAuthenticated) return <Navigate to="/login" state={{ from: { pathname: '/checkout' } }} replace />;

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-8 sm:py-10">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Checkout</p>
          <h1 className="mt-3 text-[2rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.4rem]">Complete your order</h1>
          <p className="mt-2 text-[14px] text-fetch-muted">{apiEnabled ? 'Secure checkout — order totals and payment state are confirmed by the server.' : 'Frontend demo checkout — no real payment is captured in demo mode.'}</p>
        </div>
      </div>
      <div className="section-container py-10 lg:py-12">
        <form className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start lg:gap-12" onSubmit={apiEnabled ? (e)=>e.preventDefault() : placeDemo}>
          <div className="space-y-10">
            <section>
              <h2 className="text-[1.2rem] font-semibold tracking-[-0.02em] text-fetch-ink">Contact</h2>
              <div className="mt-5 max-w-[520px]"><Field label="Email" id="email"><input id="email" type="email" required autoComplete="email" className={inputClass} value={form.email} onChange={update('email')} placeholder="you@example.com" disabled={!!createdOrder} /></Field></div>
            </section>
            <section className="border-t border-fetch-ink/[0.08] pt-10">
              <h2 className="text-[1.2rem] font-semibold tracking-[-0.02em] text-fetch-ink">Shipping address</h2>
              <div className="mt-5 grid max-w-[720px] gap-4 sm:grid-cols-2">
                <Field label="First name" id="firstName"><input id="firstName" required autoComplete="given-name" className={inputClass} value={form.firstName} onChange={update('firstName')} disabled={!!createdOrder} /></Field>
                <Field label="Last name" id="lastName"><input id="lastName" required autoComplete="family-name" className={inputClass} value={form.lastName} onChange={update('lastName')} disabled={!!createdOrder} /></Field>
                <div className="sm:col-span-2"><Field label="Address" id="address1"><input id="address1" required autoComplete="address-line1" className={inputClass} value={form.address1} onChange={update('address1')} disabled={!!createdOrder} /></Field></div>
                <Field label="City" id="city"><input id="city" required autoComplete="address-level2" className={inputClass} value={form.city} onChange={update('city')} disabled={!!createdOrder} /></Field>
                <Field label="State" id="state"><input id="state" required autoComplete="address-level1" className={inputClass} value={form.state} onChange={update('state')} disabled={!!createdOrder} /></Field>
                <Field label="ZIP code" id="postalCode"><input id="postalCode" required autoComplete="postal-code" className={inputClass} value={form.postalCode} onChange={update('postalCode')} disabled={!!createdOrder} /></Field>
                <Field label="Country" id="country"><select id="country" className={inputClass} value={form.country} onChange={update('country')} disabled={!!createdOrder}><option value="US">United States</option></select></Field>
              </div>
            </section>
            <section className="border-t border-fetch-ink/[0.08] pt-10">
              <h2 className="text-[1.2rem] font-semibold tracking-[-0.02em] text-fetch-ink">Payment</h2>
              <div className="mt-5 max-w-[720px] border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5">
                {!apiEnabled ? (
                  <><p className="text-[15px] font-medium text-fetch-ink">Demo checkout</p><p className="mt-2 text-[14px] leading-[1.7] text-fetch-muted">Set <code>VITE_API_BASE_URL</code> and configure PayPal sandbox credentials on the backend for real payment flow. No payment is captured in demo mode.</p></>
                ) : !createdOrder ? (
                  <><p className="text-[15px] font-medium text-fetch-ink">Create order for payment</p><p className="mt-2 text-[14px] leading-[1.7] text-fetch-muted">Your order will be created with server-calculated totals. Payment is captured only after server-side PayPal verification.</p><div className="mt-4"><Button type="button" loading={busy} onClick={async ()=>{ if (document.querySelector('form')?.reportValidity?.() === false) return; await createServerOrder(); }}>Continue to payment</Button></div></>
                ) : paypalConfig?.enabled && paypalConfig?.clientId ? (
                  <><p className="text-[15px] font-medium text-fetch-ink">PayPal</p><p className="mt-2 text-[14px] leading-[1.7] text-fetch-muted">Order <span className="font-medium text-fetch-ink">{createdOrder.orderNumber}</span> created (pending payment). Complete payment below; status is confirmed by the server.</p><div className="mt-4 min-h-[48px]" ref={paypalRef} />{busy && <p className="mt-3 text-[13px] text-fetch-muted">Confirming payment with server…</p>}</>
                ) : (
                  <><p className="text-[15px] font-medium text-fetch-ink">PayPal not configured</p><p className="mt-2 text-[14px] leading-[1.7] text-fetch-muted">Order <span className="font-medium text-fetch-ink">{createdOrder.orderNumber}</span> was created as <span className="font-medium">PENDING_PAYMENT</span>. Configure <code>PAYPAL_CLIENT_ID</code>, <code>PAYPAL_CLIENT_SECRET</code>, and <code>PAYPAL_WEBHOOK_ID</code> on the backend (and ensure <code>app.paypal</code> is set) to enable sandbox payment capture. No payment has been marked successful.</p><div className="mt-4"><Link to="/account/orders" className="text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">View your orders</Link></div></>
                )}
                {error && <p className="mt-3 text-[14px] text-red-700">{error}</p>}
              </div>
            </section>
          </div>
          <aside className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-6 lg:sticky lg:top-24">
            <h2 className="text-[16px] font-semibold tracking-[-0.01em] text-fetch-ink">Summary</h2>
            <ul className="mt-5 space-y-4">
              {(createdOrder?.items?.length ? createdOrder.items.map((i, idx)=>({ key: idx, name: i.productName, variant: i.variantTitle, image: null, quantity: i.quantity, price: Number(i.unitPrice), line: Number(i.lineTotal) })) : items).map((item, idx) => (
                <li key={item.key ?? item.id ?? item.slug ?? idx} className="flex gap-3">
                  {item.image ? <img src={item.image} alt="" className="h-16 w-13 shrink-0 bg-[#F0EEE9] object-cover" /> : <div className="h-16 w-13 shrink-0 bg-[#F0EEE9]" />}
                  <div className="min-w-0 flex-1"><p className="text-[14px] font-medium leading-snug text-fetch-ink">{item.name || item.productName}</p><p className="mt-0.5 text-[13px] text-fetch-muted">Qty {item.quantity}</p></div>
                  <p className="text-[14px] font-medium tabular-nums text-fetch-ink">${Number(item.line ?? (item.price * item.quantity)).toFixed(2)}</p>
                </li>
              ))}
            </ul>
            <dl className="mt-5 space-y-2 border-t border-fetch-ink/[0.08] pt-4 text-[14px]">
              <div className="flex justify-between"><dt className="text-fetch-muted">Subtotal</dt><dd className="tabular-nums text-fetch-ink">${Number(createdOrder?.subtotalAmount ?? subtotal).toFixed(2)}</dd></div>
              <div className="flex justify-between"><dt className="text-fetch-muted">Shipping</dt><dd className="text-fetch-muted">{apiEnabled ? '$0.00 (MVP)' : 'TBD'}</dd></div>
              <div className="flex justify-between border-t border-fetch-ink/[0.08] pt-3 text-[15px] font-medium"><dt className="text-fetch-ink">Total</dt><dd className="tabular-nums">${Number(createdOrder?.totalAmount ?? total).toFixed(2)}</dd></div>
            </dl>
            <div className="mt-6">
              {!apiEnabled ? <Button type="submit" loading={placingDemo} className="w-full">Place demo order</Button> : !createdOrder ? <Button type="button" loading={busy} className="w-full" onClick={async ()=>{ if (document.querySelector('form')?.reportValidity?.() === false) return; await createServerOrder(); }}>Review & continue</Button> : <p className="text-[13px] text-fetch-muted">Complete payment using the PayPal option to finalize this order.</p>}
            </div>
            <p className="mt-3 text-center text-[12px] leading-5 text-fetch-muted"><Link to="/cart" className="underline decoration-fetch-ink/15 underline-offset-2">Return to cart</Link>.</p>
          </aside>
        </form>
      </div>
    </StoreLayout>
  );
}
