import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ResponsiveContainer, AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminStats, salesChart, statusChart, adminOrders } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

function ChartSkeleton() { return <div className="h-[260px] animate-pulse rounded-xl bg-[#f3f4f6]" />; }

export default function AdminDashboardPage() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(adminStats);
  const [orders, setOrders] = useState(adminOrders);

  useEffect(() => {
    let mounted = true;
    (async () => {
      if (apiEnabled) {
        try {
          const [s, o] = await Promise.all([adminApi.getStats().catch(()=>null), adminApi.listOrders().catch(()=>[])]);
          if (!mounted) return;
          if (s) setStats([
            { label: 'Total sales', value: `$${Number(s.totalSales||0).toFixed(2)}`, change: 'Live', note: 'Captured payments' },
            { label: 'Orders', value: String(s.orders ?? 0), change: 'Live', note: 'All orders' },
            { label: 'Fulfillment queue', value: String(s.fulfillmentQueue ?? 0), change: 'Needs review', note: 'Paid/processing' },
            { label: 'Customers', value: String(s.customers ?? 0), change: 'Live', note: 'Accounts' },
          ]);
          if (Array.isArray(o) && o.length) setOrders(o.slice(0, 8).map((x)=>({ orderNumber: x.orderNumber, customer: x.customerEmail, status: x.status, fulfillment: x.status, total: Number(x.totalAmount||0) })));
        } catch {}
      }
      if (mounted) setLoading(false);
    })();
    return () => { mounted = false; };
  }, []);

  return (
    <AdminLayout title="Dashboard" subtitle="Overview of sales, orders, and fulfillment" actions={<AdminButton variant="secondary" onClick={() => {}}>Export</AdminButton>}>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((stat) => (
          <div key={stat.label} className="rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
            {loading ? <div className="space-y-3"><div className="h-3 w-24 animate-pulse rounded bg-[#f3f4f6]" /><div className="h-7 w-28 animate-pulse rounded bg-[#f3f4f6]" /><div className="h-3 w-20 animate-pulse rounded bg-[#f3f4f6]" /></div> : (<><p className="text-[13px] font-medium text-[#6b7280]">{stat.label}</p><p className="mt-2 text-[1.7rem] font-semibold tracking-[-0.03em] text-[#111827]">{stat.value}</p><p className="mt-1 text-[12px] text-[#6b7280]"><span className="font-medium text-[#374151]">{stat.change}</span> · {stat.note}</p></>)}
          </div>
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)]">
        <AdminCard title="Sales trend" description={apiEnabled ? 'Demo chart (connect analytics for live trends)' : 'Weekly demo performance'}>
          {loading ? <ChartSkeleton /> : (
            <div className="h-[260px] w-full"><ResponsiveContainer width="100%" height="100%"><AreaChart data={salesChart} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}><defs><linearGradient id="salesFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#d7ddf7" stopOpacity={0.9} /><stop offset="95%" stopColor="#d7ddf7" stopOpacity={0.05} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" stroke="#eef0f2" vertical={false} /><XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} /><YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} /><Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e5e7eb', boxShadow: '0 8px 24px rgba(16,24,40,0.06)' }} /><Area type="monotone" dataKey="sales" stroke="#6b7280" strokeWidth={2} fill="url(#salesFill)" /></AreaChart></ResponsiveContainer></div>
          )}
        </AdminCard>
        <AdminCard title="Order status" description={apiEnabled ? 'Demo chart (connect analytics for live mix)' : 'Current fulfillment mix (demo)'}>
          {loading ? <ChartSkeleton /> : (
            <div className="h-[260px] w-full"><ResponsiveContainer width="100%" height="100%"><BarChart data={statusChart} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" stroke="#eef0f2" vertical={false} /><XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} /><YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6b7280' }} allowDecimals={false} /><Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e5e7eb' }} cursor={{ fill: '#f9fafb' }} /><Bar dataKey="value" fill="#c7d2fe" radius={[8, 8, 0, 0]} /></BarChart></ResponsiveContainer></div>
          )}
        </AdminCard>
      </div>
      <AdminCard title="Recent orders" description="Orders needing attention appear here" action={<Link to="/admin/orders"><AdminButton variant="secondary">View all</AdminButton></Link>}>
        {loading ? <div className="space-y-3">{[1, 2].map((i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Order</th><th className="py-2 pr-4 font-medium">Customer</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 pr-4 font-medium">Fulfillment</th><th className="py-2 font-medium">Total</th></tr></thead>
            <tbody>{orders.map((o) => (<tr key={o.orderNumber} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4 font-medium text-[#111827]"><Link to={`/admin/orders/${o.orderNumber}`} className="hover:underline">{o.orderNumber}</Link></td><td className="py-3 pr-4 text-[#374151]">{o.customer || '—'}</td><td className="py-3 pr-4"><span className="rounded-full bg-[#f3f4f6] px-2 py-1 text-[12px] text-[#374151]">{o.status}</span></td><td className="py-3 pr-4 text-[#6b7280]">{o.fulfillment || o.status}</td><td className="py-3 tabular-nums text-[#111827]">${Number(o.total).toFixed(2)}</td></tr>))}</tbody>
          </table></div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
