import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminOrders } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

const inputClass = 'h-9 w-full rounded-lg border border-[#d1d5db] bg-white px-3 text-[13px] focus:border-[#9ca3af] focus:outline-none';

export default function AdminOrderDetailPage() {
  const { orderNumber } = useParams();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [saving, setSaving] = useState(false);
  const [cjBusy, setCjBusy] = useState(false);
  const [ship, setShip] = useState({ carrier: '', trackingNumber: '', trackingUrl: '', status: 'SHIPPED' });
  const [msg, setMsg] = useState('');

  const load = async () => {
    setLoading(true); setMsg('');
    try {
      if (apiEnabled) setData(await adminApi.getOrder(orderNumber));
      else { const o = adminOrders.find((x)=>x.orderNumber===orderNumber); setData(o ? { order: { ...o, totalAmount: o.total, customerEmail: 'demo@example.com', shippingAddressSnapshot: JSON.stringify(o.shipping||{}) }, items: (o.items||[]).map(i=>({productNameSnapshot:i.name, variantTitleSnapshot:i.variant, skuSnapshot:'—', quantity:i.qty, unitPriceAmount:i.price, lineTotalAmount:i.price*i.qty})), shipments: [] } : null); }
    } catch { setData(null); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [orderNumber]); // eslint-disable-line

  const order = data?.order || data;
  const items = data?.items || [];
  const shipments = data?.shipments || [];
  const cj = data?.cjFulfillment || {};
  const parseAddr = (v) => { if (!v) return null; if (typeof v === 'object') return v; try { return JSON.parse(v); } catch { return null; } };
  const addr = parseAddr(order?.shippingAddressSnapshot) || order?.shipping;

  const setStatus = async (status) => {
    if (!apiEnabled) { setMsg('Demo mode: status not persisted.'); return; }
    setSaving(true); setMsg('');
    try { await adminApi.updateOrderStatus(orderNumber, status); setMsg('Status updated.'); await load(); }
    catch (e) { setMsg(e?.response?.data?.error?.message || 'Update failed.'); }
    finally { setSaving(false); }
  };

  const addShipment = async (e) => {
    e.preventDefault();
    if (!apiEnabled) { setMsg('Demo mode: shipment not persisted.'); return; }
    setSaving(true); setMsg('');
    try { await adminApi.addShipment(orderNumber, ship); setMsg('Shipment added.'); setShip({ carrier:'', trackingNumber:'', trackingUrl:'', status:'SHIPPED' }); await load(); }
    catch (err) { setMsg(err?.response?.data?.error?.message || 'Could not add shipment.'); }
    finally { setSaving(false); }
  };

  const submitCj = async () => {
    if (!apiEnabled) { setMsg('Demo mode: CJ actions unavailable.'); return; }
    setCjBusy(true); setMsg('');
    try {
      const res = await adminApi.submitOrderToCj(orderNumber);
      setMsg(res?.cjOrderId ? `Submitted to CJ (CJ order ${res.cjOrderId}). Tracking/status will sync from CJ.` : 'Submitted to CJ.');
      await load();
    } catch (err) { setMsg(err?.response?.data?.error?.message || 'CJ submission failed.'); }
    finally { setCjBusy(false); }
  };

  const syncCj = async () => {
    if (!apiEnabled) { setMsg('Demo mode: CJ actions unavailable.'); return; }
    setCjBusy(true); setMsg('');
    try { await adminApi.syncOrderFromCj(orderNumber); setMsg('Synced tracking/status from CJ.'); await load(); }
    catch (err) { setMsg(err?.response?.data?.error?.message || 'CJ sync failed.'); }
    finally { setCjBusy(false); }
  };

  return (
    <AdminLayout title={order ? `Order ${order.orderNumber || orderNumber}` : 'Order'} subtitle="Fulfillment and tracking" actions={<Link to="/admin/orders"><AdminButton variant="secondary">Back to orders</AdminButton></Link>}>
      {loading ? <div className="h-40 animate-pulse rounded-2xl bg-[#e5e7eb]" /> : !order ? <AdminCard title="Not found">Order could not be loaded.</AdminCard> : (
        <div className="space-y-5">
          {msg && <p className="text-[13px] text-[#374151]">{msg}</p>}
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
            <AdminCard title="Items">
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Item</th><th className="py-2 pr-4 font-medium">SKU</th><th className="py-2 pr-4 font-medium">Qty</th><th className="py-2 font-medium">Total</th></tr></thead>
                  <tbody>{items.map((i,idx)=>(<tr key={idx} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4"><p className="font-medium text-[#111827]">{i.productNameSnapshot||i.name}</p><p className="text-[12px] text-[#6b7280]">{i.variantTitleSnapshot||i.variant||''}</p></td><td className="py-3 pr-4 text-[#6b7280]">{i.skuSnapshot||'—'}</td><td className="py-3 pr-4">{i.quantity??i.qty}</td><td className="py-3 tabular-nums">${Number(i.lineTotalAmount ?? (Number(i.unitPriceAmount||i.price)*Number(i.quantity||i.qty))).toFixed(2)}</td></tr>))}</tbody>
                </table>
              </div>
            </AdminCard>
            <div className="space-y-4">
              <AdminCard title="Order status" description={order.status}>
                <div className="flex flex-wrap gap-2">
                  {['PENDING_PAYMENT','PAID','PROCESSING','FULFILLMENT_SUBMITTED','SHIPPED','DELIVERED','CANCELLED'].map((s)=>(<AdminButton key={s} variant={order.status===s?'primary':'secondary'} className="h-8 px-2.5 text-[12px]" onClick={()=>setStatus(s)} disabled={saving || cjBusy}>{s.replace(/_/g,' ')}</AdminButton>))}
                </div>
                <p className="mt-3 text-[12px] text-[#6b7280]">Payment status should reflect PayPal server confirmation. Shipping/delivery can be auto-updated from CJ when fulfillment is submitted/synced.</p>
              </AdminCard>
              <AdminCard title="CJ fulfillment (automatic)" description={cj?.cjOrderId ? `CJ order ${cj.cjOrderId}` : 'Not submitted to CJ'}>
                <div className="space-y-2 text-[13px] text-[#374151]">
                  <p><span className="text-[#6b7280]">CJ configured:</span> {cj?.cjConfigured ? 'Yes' : 'No (set CJ_API_ENABLED/CJ_API_KEY)'}</p>
                  {cj?.cjFulfillmentStatus && <p><span className="text-[#6b7280]">CJ status:</span> {cj.cjFulfillmentStatus}</p>}
                  {cj?.cjLastSyncAt && <p><span className="text-[#6b7280]">Last sync:</span> {new Date(cj.cjLastSyncAt).toLocaleString()}</p>}
                  {cj?.cjLastError && <p className="rounded-lg border border-amber-200 bg-amber-50 p-2 text-[12px] text-amber-900"><span className="font-medium">CJ issue:</span> {cj.cjLastError}</p>}
                  <div className="flex flex-wrap gap-2 pt-1">
                    <AdminButton onClick={submitCj} loading={cjBusy} disabled={saving || !!cj?.cjOrderId}>{cj?.cjOrderId ? 'Submitted to CJ' : 'Submit to CJ'}</AdminButton>
                    <AdminButton variant="secondary" onClick={syncCj} loading={cjBusy} disabled={saving || !cj?.cjOrderId}>Sync from CJ</AdminButton>
                  </div>
                  <p className="text-[12px] text-[#6b7280]">Auto-submits when payment is confirmed (<code className="rounded bg-[#f3f4f6] px-1">CJ_AUTO_SUBMIT_ON_PAID=true</code>, default). Optional: <code className="rounded bg-[#f3f4f6] px-1">CJ_AUTO_SUBMIT_ON_CREATE=true</code> submits on order creation (before payment). Requires CJ variant IDs on order items. Configure CJ webhook <code className="rounded bg-[#f3f4f6] px-1">/api/v1/webhooks/cj</code> and <code className="rounded bg-[#f3f4f6] px-1">CJ_FULFILLMENT_SYNC_ENABLED=true</code> for automatic tracking/status updates.</p>
                </div>
              </AdminCard>
              <AdminCard title="Shipping address">
                {addr ? <address className="text-[13px] not-italic leading-6 text-[#374151]">{addr.fullName||addr.name}<br />{addr.line1}{addr.line2 ? <><br />{addr.line2}</> : null}<br />{addr.city}, {addr.state} {addr.postalCode}<br />{addr.countryCode||addr.country||'US'}</address> : <p className="text-[13px] text-[#6b7280]">No address snapshot.</p>}
              </AdminCard>
            </div>
          </div>
          <AdminCard title="Shipments / tracking">
            {shipments.length > 0 && <div className="mb-4 space-y-2">{shipments.map((s,idx)=>(<div key={idx} className="rounded-xl border border-[#eef0f2] p-3 text-[13px]"><p className="font-medium text-[#111827]">{s.carrier||'Carrier'} {s.trackingNumber?`· ${s.trackingNumber}`:''}</p><p className="text-[#6b7280]">{s.status}{s.trackingUrl?<> · <a className="underline" href={s.trackingUrl} target="_blank" rel="noreferrer">Track</a></>:''}</p></div>))}</div>}
            <form className="grid max-w-[640px] gap-3 sm:grid-cols-2" onSubmit={addShipment}>
              <label className="block"><span className="text-[12px] font-medium text-[#374151]">Carrier</span><input className={`${inputClass} mt-1`} value={ship.carrier} onChange={(e)=>setShip(f=>({...f,carrier:e.target.value}))} /></label>
              <label className="block"><span className="text-[12px] font-medium text-[#374151]">Tracking number</span><input className={`${inputClass} mt-1`} value={ship.trackingNumber} onChange={(e)=>setShip(f=>({...f,trackingNumber:e.target.value}))} /></label>
              <label className="block sm:col-span-2"><span className="text-[12px] font-medium text-[#374151]">Tracking URL</span><input className={`${inputClass} mt-1`} value={ship.trackingUrl} onChange={(e)=>setShip(f=>({...f,trackingUrl:e.target.value}))} /></label>
              <div className="sm:col-span-2"><AdminButton type="submit" loading={saving}>Add shipment</AdminButton></div>
            </form>
          </AdminCard>
        </div>
      )}
    </AdminLayout>
  );
}
