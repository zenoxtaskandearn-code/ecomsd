import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import AdminLayout, { AdminCard } from '../../components/admin/AdminLayout.jsx';
import { adminOrders } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

export default function AdminOrdersPage() {
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [q, setQ] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      try { setOrders(apiEnabled ? await adminApi.listOrders() : adminOrders); }
      catch { setOrders(adminOrders); }
      finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, []);

  const filtered = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!s) return orders;
    return orders.filter((o) => [o.orderNumber, o.customer, o.customerEmail, o.status].some((v) => String(v||'').toLowerCase().includes(s)));
  }, [orders, q]);

  return (
    <AdminLayout title="Orders" subtitle="Review orders, payment state, and fulfillment">
      <AdminCard title="All orders" description={`${filtered.length} orders`}>
        <div className="mb-4"><input className="h-9 w-full max-w-[320px] rounded-lg border border-[#d1d5db] bg-white px-3 text-[13px]" placeholder="Search orders..." value={q} onChange={(e)=>setQ(e.target.value)} /></div>
        {loading ? <div className="space-y-2">{[1,2,3,4].map(i=><div key={i} className="h-14 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-[13px]">
              <thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Order</th><th className="py-2 pr-4 font-medium">Customer</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 pr-4 font-medium">Total</th><th className="py-2 font-medium"></th></tr></thead>
              <tbody>
                {filtered.map((o) => (
                  <tr key={o.orderNumber || o.id} className="border-b border-[#f3f4f6] last:border-0">
                    <td className="py-3 pr-4 font-medium text-[#111827]">{o.orderNumber || o.id}</td>
                    <td className="py-3 pr-4 text-[#374151]">{o.customer || o.customerEmail || '—'}</td>
                    <td className="py-3 pr-4"><span className="rounded-full bg-[#f3f4f6] px-2 py-1 text-[12px] text-[#374151]">{o.status}</span></td>
                    <td className="py-3 pr-4 tabular-nums text-[#111827]">${Number(o.total ?? o.totalAmount ?? 0).toFixed(2)}</td>
                    <td className="py-3">{(o.orderNumber) && <Link to={`/admin/orders/${o.orderNumber}`} className="text-[13px] font-medium text-[#374151] hover:underline">View</Link>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
