import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import AccountShell from '../components/account/AccountShell.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { demoOrders } from '../utils/accountDemo.js';
import { useAuth } from '../context/AuthContext.jsx';
import { apiEnabled } from '../services/api.js';
import * as orderApi from '../services/orderApi.js';

export default function AccountPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  useEffect(() => {
    let mounted = true;
    (async () => {
      try { setOrders(apiEnabled ? await orderApi.listMine() : demoOrders); }
      catch { setOrders(apiEnabled ? [] : demoOrders); }
      finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, []);

  const recent = orders[0];
  return (
    <StoreLayout>
      <AccountShell title="Account overview" description="Manage your profile, addresses, and order updates in one place.">
        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2"><Skeleton className="h-32 w-full" /><Skeleton className="h-32 w-full" /><Skeleton className="h-40 w-full sm:col-span-2" /></div>
        ) : (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5">
                <p className="text-[12px] font-medium uppercase tracking-[0.16em] text-fetch-ink/45">Profile</p>
                <p className="mt-3 text-[16px] font-medium text-fetch-ink">{user?.firstName} {user?.lastName}</p>
                <p className="mt-1 text-[14px] text-fetch-muted">{user?.email}</p>
                <Link to="/account/addresses" className="mt-4 inline-block text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">Manage profile</Link>
              </div>
              <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5">
                <p className="text-[12px] font-medium uppercase tracking-[0.16em] text-fetch-ink/45">Orders</p>
                <p className="mt-3 text-[16px] font-medium text-fetch-ink">{orders.length} {orders.length === 1 ? 'order' : 'orders'}</p>
                <p className="mt-1 text-[14px] text-fetch-muted">{apiEnabled ? 'Your order history and status updates.' : 'Demo order history (connect backend for real data).'}</p>
                <Link to="/account/orders" className="mt-4 inline-block text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">View orders</Link>
              </div>
            </div>
            {recent && (
              <div className="border border-fetch-ink/[0.08] p-5 sm:p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-[12px] font-medium uppercase tracking-[0.16em] text-fetch-ink/45">Recent order</p>
                    <p className="mt-2 text-[17px] font-semibold tracking-[-0.02em] text-fetch-ink">{recent.orderNumber}</p>
                    <p className="mt-1 text-[14px] text-fetch-muted">{recent.placedAt ? new Date(recent.placedAt).toLocaleDateString() : recent.placedAt} · {recent.status}</p>
                  </div>
                  <Link to={`/account/orders/${recent.orderNumber}`} className="btn-primary h-10 px-5 text-[14px]">View details</Link>
                </div>
              </div>
            )}
          </div>
        )}
      </AccountShell>
    </StoreLayout>
  );
}
