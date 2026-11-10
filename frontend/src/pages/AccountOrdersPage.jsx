import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import AccountShell from '../components/account/AccountShell.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { demoOrders } from '../utils/accountDemo.js';
import { apiEnabled } from '../services/api.js';
import * as orderApi from '../services/orderApi.js';

export default function AccountOrdersPage() {
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        if (apiEnabled) setOrders(await orderApi.listMine());
        else setOrders(demoOrders);
      } catch (e) {
        setError(e?.response?.data?.error?.message || 'Could not load orders.');
        if (!apiEnabled) setOrders(demoOrders);
      } finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, []);

  return (
    <StoreLayout>
      <AccountShell title="Order history" description="Review past orders, status updates, and tracking details.">
        {loading ? (
          <div className="space-y-3">{[1, 2, 3].map((i) => <Skeleton key={i} className="h-24 w-full" />)}</div>
        ) : error && orders.length === 0 ? (
          <EmptyState title="Could not load orders" message={error} />
        ) : orders.length === 0 ? (
          <EmptyState title="No orders yet" message="When you place an order, it will appear here." actionLabel="Shop essentials" onAction={() => (window.location.href = '/shop')} />
        ) : (
          <div className="overflow-hidden border border-fetch-ink/[0.08]">
            <div className="hidden grid-cols-[1.2fr_0.9fr_0.8fr_0.8fr_auto] gap-4 border-b border-fetch-ink/[0.08] bg-[#FBFAF8] px-5 py-3 text-[12px] font-medium uppercase tracking-[0.12em] text-fetch-ink/45 md:grid">
              <span>Order</span><span>Date</span><span>Status</span><span>Total</span><span></span>
            </div>
            <ul className="divide-y divide-fetch-ink/[0.08]">
              {orders.map((order) => {
                const total = Number(order.total ?? order.totalAmount ?? 0);
                const date = order.placedAt ? new Date(order.placedAt).toLocaleDateString() : (order.placed || '—');
                return (
                  <li key={order.orderNumber} className="grid gap-2 px-5 py-4 md:grid-cols-[1.2fr_0.9fr_0.8fr_0.8fr_auto] md:items-center md:gap-4">
                    <div><p className="text-[15px] font-medium text-fetch-ink">{order.orderNumber}</p><p className="mt-0.5 text-[13px] text-fetch-muted md:hidden">{date} · {order.status}</p></div>
                    <p className="hidden text-[14px] text-fetch-muted md:block">{date}</p>
                    <p className="hidden text-[14px] text-fetch-ink/80 md:block">{order.status}</p>
                    <p className="text-[14px] font-medium tabular-nums text-fetch-ink">${total.toFixed(2)}</p>
                    <Link to={`/account/orders/${order.orderNumber}`} className="inline-flex text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Details</Link>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </AccountShell>
    </StoreLayout>
  );
}
