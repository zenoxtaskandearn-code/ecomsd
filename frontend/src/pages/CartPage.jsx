import { Link } from 'react-router-dom';
import { Minus, Plus, Trash2 } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { useCart } from '../context/CartContext.jsx';

export default function CartPage() {
  const { items, subtotal, updateQuantity, removeItem, clearCart } = useCart();

  return (
    <StoreLayout>
      <div className="section-container py-10 sm:py-12 lg:py-14">
        <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Cart</p>
        <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.5rem]">Your cart</h1>

        {items.length === 0 ? (
          <div className="mt-8">
            <EmptyState title="Your cart is empty" message="Add a few essentials and they’ll appear here, ready for checkout." actionLabel="Shop essentials" onAction={() => (window.location.href = '/shop')} />
          </div>
        ) : (
          <div className="mt-10 grid gap-10 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start lg:gap-12">
            <section aria-label="Cart items">
              <ul className="divide-y divide-fetch-ink/[0.08] border-y border-fetch-ink/[0.08]">
                {items.map((item) => {
                  const key = item.id ?? item.slug;
                  return (
                    <li key={key} className="grid gap-4 py-6 sm:grid-cols-[112px_minmax(0,1fr)_auto] sm:items-start sm:gap-5">
                      <Link to={`/product/${item.slug}`} className="block bg-[#F0EEE9]">
                        <img src={item.image} alt={item.name} className="aspect-[4/5] w-full object-cover sm:w-[112px]" />
                      </Link>
                      <div>
                        <Link to={`/product/${item.slug}`} className="text-[16px] font-medium tracking-[-0.01em] text-fetch-ink transition hover:text-fetch-ink/75">{item.name}</Link>
                        {item.variant && <p className="mt-1 text-[13px] text-fetch-muted">{item.variant}</p>}
                        <p className="mt-2 text-[14px] tabular-nums text-fetch-ink">${Number(item.price).toFixed(2)}</p>
                        <div className="mt-4 flex flex-wrap items-center gap-4">
                          <div className="inline-flex h-10 items-center border border-fetch-ink/12">
                            <button type="button" className="inline-flex h-10 w-10 items-center justify-center text-fetch-ink/70" aria-label={`Decrease quantity for ${item.name}`} onClick={() => updateQuantity(key, Number(item.quantity) - 1)}><Minus size={15} strokeWidth={1.6} /></button>
                            <span className="min-w-[2.25rem] text-center text-[14px] font-medium tabular-nums">{item.quantity}</span>
                            <button type="button" className="inline-flex h-10 w-10 items-center justify-center text-fetch-ink/70" aria-label={`Increase quantity for ${item.name}`} onClick={() => updateQuantity(key, Number(item.quantity) + 1)}><Plus size={15} strokeWidth={1.6} /></button>
                          </div>
                          <button type="button" className="inline-flex items-center gap-2 text-[13px] text-fetch-muted transition hover:text-fetch-ink" onClick={() => removeItem(key)}><Trash2 size={14} strokeWidth={1.6} aria-hidden="true" /> Remove</button>
                        </div>
                      </div>
                      <p className="text-[15px] font-medium tabular-nums text-fetch-ink sm:pt-0.5">${(Number(item.price) * Number(item.quantity)).toFixed(2)}</p>
                    </li>
                  );
                })}
              </ul>
              <div className="mt-5">
                <button type="button" className="text-[13px] text-fetch-muted underline decoration-fetch-ink/15 underline-offset-4 transition hover:text-fetch-ink" onClick={() => clearCart()}>Clear cart</button>
              </div>
            </section>
            <aside className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-6 lg:sticky lg:top-24">
              <h2 className="text-[16px] font-semibold tracking-[-0.01em] text-fetch-ink">Order summary</h2>
              <dl className="mt-5 space-y-3 text-[14px]">
                <div className="flex items-center justify-between gap-4"><dt className="text-fetch-muted">Subtotal</dt><dd className="font-medium tabular-nums text-fetch-ink">${subtotal.toFixed(2)}</dd></div>
                <div className="flex items-center justify-between gap-4"><dt className="text-fetch-muted">Shipping</dt><dd className="text-fetch-muted">Calculated at checkout</dd></div>
                <div className="flex items-center justify-between gap-4"><dt className="text-fetch-muted">Taxes</dt><dd className="text-fetch-muted">Calculated at checkout</dd></div>
              </dl>
              <div className="mt-5 flex items-center justify-between border-t border-fetch-ink/[0.08] pt-4 text-[15px]"><span className="font-medium text-fetch-ink">Estimated total</span><span className="font-semibold tabular-nums text-fetch-ink">${subtotal.toFixed(2)}</span></div>
              <div className="mt-6 flex flex-col gap-3">
                <Link to="/checkout" className="btn-primary w-full">Continue to checkout</Link>
                <Link to="/shop" className="text-center text-[14px] font-medium text-fetch-ink/70 underline decoration-fetch-ink/15 underline-offset-4 transition hover:text-fetch-ink">Continue shopping</Link>
              </div>
            </aside>
          </div>
        )}
      </div>
    </StoreLayout>
  );
}
