import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ResponsiveContainer, AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminStats as demoStats, salesChart, statusChart as demoStatusChart, adminOrders } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

function ChartSkeleton() { return <div className="h-[260px] animate-pulse rounded-xl bg-[#f3f4f6]" />; }
function money(v) { const n = Number(v); return Number.isFinite(n) ? `$${n.toFixed(2)}` : '$0.00'; }
function StatCard({ label, value, note, sub, tone }) {
  const toneClass = tone === 'good' ? 'text-emerald-700' : tone === 'warn' ? 'text-amber-700' : tone === 'bad' ? 'text-red-700' : 'text-[#374151]';
  return (
    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <p className="text-[13px] font-medium text-[#6b7280]">{label}</p>
      <p className="mt-2 text-[1.55rem] font-semibold tracking-[-0.03em] text-[#111827]">{value}</p>
      {(note || sub) && <p className="mt-1 text-[12px] text-[#6b7280]">{note && <span className={`font-medium ${toneClass}`}>{note}</span>}{note && sub ? ' · ' : ''}{sub}</p>}
    </div>
  );
}

export default function AdminDashboardPage() {
  const [loading, setLoading] = useState(true);
  const [s, setS] = useState(null);
  const [orders, setOrders] = useState(adminOrders);
  const [orderChart, setOrderChart] = useState(demoStatusChart);

  useEffect(() => {
    let mounted = true;
    (async () => {
      if (apiEnabled) {
        try {
          const [statsRes, o] = await Promise.all([adminApi.getStats().catch(()=>null), adminApi.listOrders().catch(()=>[])]);
          if (!mounted) return;
          if (statsRes) {
            setS(statsRes);
            const by = statsRes.ordersByStatus || {};
            const preferred = ['PENDING_PAYMENT','PAID','PROCESSING','FULFILLMENT_SUBMITTED','SHIPPED','DELIVERED','CANCELLED','REFUNDED'];
            const entries = Object.keys(by);
            const keys = [...preferred.filter(k=>by[k] != null), ...entries.filter(k=>!preferred.includes(k))].filter((k,i,a)=>a.indexOf(k)===i);
            const data = keys.map((k)=>({ name: k.replace(/_/g,' '), value: Number(by[k]||0) })).filter(d=>d.value > 0);
            if (data.length) setOrderChart(data);
            else setOrderChart([{ name: 'No orders', value: 0 }]);
          }
          if (Array.isArray(o)) setOrders(o.slice(0, 8).map((x)=>({ orderNumber: x.orderNumber, customer: x.customerEmail, status: x.status, fulfillment: x.cjFulfillmentStatus || x.status, total: Number(x.totalAmount ?? x.total ?? 0), cjOrderId: x.cjOrderId })));
        } catch {}
      }
      if (mounted) setLoading(false);
    })();
    return () => { mounted = false; };
  }, []);

  const cards = useMemo(() => {
    if (!apiEnabled || !s) return demoStats.map(d => ({ label: d.label, value: d.value, note: d.change, sub: d.note }));
    const itemsSold = Number(s.itemsSold || 0);
    const itemsWithCost = Number(s.itemsWithCostData || 0);
    const profitKnown = s.profitMarginPercent != null;
    const profitNote = profitKnown ? `${Number(s.profitMarginPercent).toFixed(2)}% margin` : (itemsSold > 0 ? 'Add supplier costs' : 'No paid items yet');
    const profitSub = `Cost ${money(s.estimatedCost)} · ${itemsWithCost}/${itemsSold} items costed`;
    return [
      { label: 'Total sales', value: money(s.totalSales), note: 'Live', sub: 'Captured payments (PayPal)' },
      { label: 'Paid revenue', value: money(s.paidRevenue), note: `${Number(s.paidOrders||0)} paid orders`, sub: `AOV ${money(s.averageOrderValue)}` },
      { label: 'Estimated profit', value: money(s.estimatedProfit), note: profitNote, sub: profitSub, tone: Number(s.estimatedProfit||0) >= 0 ? 'good' : 'bad' },
      { label: 'Orders', value: String(s.orders ?? 0), note: `${Number(s.pendingPaymentOrders||0)} pending payment`, sub: `${Number(s.deliveredOrders||0)} delivered · ${Number(s.cancelledOrders||0)} cancelled/refunded` },
      { label: 'Fulfillment queue', value: String(s.fulfillmentQueue ?? 0), note: (Number(s.fulfillmentQueue||0) > 0 ? 'Needs action' : 'Clear'), sub: 'Paid/processing awaiting fulfillment', tone: Number(s.fulfillmentQueue||0) > 0 ? 'warn' : 'good' },
      { label: 'CJ fulfillment', value: `${Number(s.cjSubmitted||0)} submitted`, note: `${Number(s.cjPending||0)} pending`, sub: `${Number(s.cjErrors||0)} with CJ errors`, tone: Number(s.cjErrors||0) > 0 ? 'warn' : (Number(s.cjPending||0) > 0 ? 'warn' : 'good') },
      { label: 'Shipping progress', value: String(s.shippedOrders ?? 0), note: 'Shipped', sub: `${Number(s.deliveredOrders||0)} delivered` },
      { label: 'Catalog & customers', value: String(s.customers ?? 0), note: 'Customers', sub: `${Number(s.products||0)} products · ${itemsSold} items sold` },
    ];
  }, [s]);

  return (
    <AdminLayout title="Dashboard" subtitle="Sales, profit, orders, and fulfillment overview" actions={<AdminButton variant="secondary" onClick={() => window.location.reload()}>Refresh</AdminButton>}>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {loading ? Array.from({length:8}).map((_,i)=>(<div key={i} className="rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-[0_1px_2px_rgba(16,24,40,0.04)]"><div className="space-y-3"><div className="h-3 w-24 animate-pulse rounded bg-[#f3f4f6]" /><div className="h-7 w-28 animate-pulse rounded bg-[#f3f4f6]" /><div className="h-3 w-36 animate-pulse rounded bg-[#f3f4f6]" /></div></div>)) : cards.map((c) => <StatCard key={c.label} {...c} />)}
      </div>

      {apiEnabled && s && Number(s.itemsSold||0) > 0 && Number(s.itemsWithCostData||0) < Number(s.itemsSold||0) && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-[13px] text-amber-900">
          <span className="font-medium">Profit is estimated.</span> Supplier/shipping cost data is missing for some sold items ({Number(s.itemsSold||0) - Number(s.itemsWithCostData||0)} item(s) uncosted). Add supplier cost and shipping cost on product variants to see accurate profit/margins.
        </div>
      )}

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)]">
        <AdminCard title="Sales trend" description={apiEnabled ? 'Demo trend chart (connect analytics for live time-series)' : 'Weekly demo performance'}>
          {loading ? <ChartSkeleton /> : (
            <div className="h-[260px] w-full"><ResponsiveContainer width="100%" height="100%"><AreaChart data={salesChart} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}><defs><linearGradient id="salesFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#d7ddf7" stopOpacity={0.9} /><stop offset="95%" stopColor="#d7ddf7" stopOpacity={0.05} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" stroke="#eef0f2" vertical={false} /><XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} /><YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} /><Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e5e7eb', boxShadow: '0 8px 24px rgba(16,24,40,0.06)' }} /><Area type="monotone" dataKey="sales" stroke="#6b7280" strokeWidth={2} fill="url(#salesFill)" /></AreaChart></ResponsiveContainer></div>
          )}
        </AdminCard>
        <AdminCard title="Order status" description={apiEnabled ? 'Live order status breakdown' : 'Current fulfillment mix (demo)'}>
          {loading ? <ChartSkeleton /> : (
            <div className="h-[260px] w-full"><ResponsiveContainer width="100%" height="100%"><BarChart data={orderChart} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" stroke="#eef0f2" vertical={false} /><XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#6b7280' }} interval={0} angle={-20} textAnchor="end" height={50} /><YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} allowDecimals={false} /><Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e5e7eb' }} cursor={{ fill: '#f9fafb' }} /><Bar dataKey="value" fill="#c7d2fe" radius={[8, 8, 0, 0]} /></BarChart></ResponsiveContainer></div>
          )}
        </AdminCard>
      </div>

      <AdminCard title="Recent orders" description="Latest orders and payment/fulfillment status" action={<Link to="/admin/orders"><AdminButton variant="secondary">View all</AdminButton></Link>}>
        {loading ? <div className="space-y-3">{[1, 2, 3].map((i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Order</th><th className="py-2 pr-4 font-medium">Customer</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 pr-4 font-medium">CJ / Fulfillment</th><th className="py-2 font-medium">Total</th></tr></thead>
            <tbody>{orders.map((o) => (<tr key={o.orderNumber} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4 font-medium text-[#111827]"><Link to={`/admin/orders/${o.orderNumber}`} className="hover:underline">{o.orderNumber}</Link></td><td className="py-3 pr-4 text-[#374151]">{o.customer || '—'}</td><td className="py-3 pr-4"><span className={`rounded-full px-2 py-1 text-[12px] ${o.status === 'PAID' ? 'bg-emerald-50 text-emerald-700' : o.status === 'PENDING_PAYMENT' ? 'bg-amber-50 text-amber-700' : 'bg-[#f3f4f6] text-[#374151]'}`}>{o.status}</span></td><td className="py-3 pr-4 text-[#6b7280]">{o.cjOrderId ? `CJ ${o.cjOrderId}` : (o.fulfillment || o.status)}</td><td className="py-3 tabular-nums text-[#111827]">{money(o.total)}</td></tr>))}</tbody>
          </table></div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
