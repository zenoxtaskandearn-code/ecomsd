import { useEffect, useState } from 'react';
import AdminLayout, { AdminCard } from '../../components/admin/AdminLayout.jsx';
import { adminCustomers } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

export default function AdminCustomersPage() {
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState([]);
  useEffect(() => {
    let mounted = true;
    (async () => {
      try { setRows(apiEnabled ? await adminApi.listCustomers() : adminCustomers); }
      catch { setRows(adminCustomers); }
      finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, []);

  return (
    <AdminLayout title="Customers" subtitle="Customer accounts and order activity">
      <AdminCard title="All customers">
        {loading ? <div className="space-y-2">{[1,2,3].map(i=><div key={i} className="h-12 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-[13px]">
              <thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Customer</th><th className="py-2 pr-4 font-medium">Email</th><th className="py-2 pr-4 font-medium">Role/Status</th><th className="py-2 pr-4 font-medium">Orders</th><th className="py-2 font-medium">Spent</th></tr></thead>
              <tbody>{rows.map((c,idx)=>(<tr key={c.id||idx} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4 font-medium text-[#111827]">{c.name || '—'}</td><td className="py-3 pr-4 text-[#374151]">{c.email}</td><td className="py-3 pr-4 text-[#6b7280]">{c.role ? `${c.role} · ${c.status}` : c.status}</td><td className="py-3 pr-4">{c.orders ?? 0}</td><td className="py-3 tabular-nums">${Number(c.spent ?? 0).toFixed(2)}</td></tr>))}</tbody>
            </table>
          </div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
