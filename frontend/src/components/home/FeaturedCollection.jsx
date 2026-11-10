import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ProductMarquee from '../products/ProductMarquee.jsx';
import { listProducts } from '../../services/catalogApi.js';

export default function FeaturedCollection() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const res = await listProducts({ category: 'walking-kits', size: 8 });
        if (mounted) setItems((res?.content || []).slice(0, 4));
      } catch {
        if (mounted) setItems([]);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  return (
    <section className="bg-[#F6F5F2] py-16 sm:py-20 lg:py-24">
      <div className="section-container">
        <div className="mb-10 flex flex-col gap-4 sm:mb-12 sm:flex-row sm:items-end sm:justify-between">
          <div className="max-w-[520px]">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">New arrivals</p>
            <h2 className="mt-3 text-[2rem] font-semibold leading-[1.06] tracking-[-0.025em] text-fetch-ink sm:text-[2.35rem]">Walk essentials in classic beige</h2>
            <p className="mt-3 max-w-[42ch] text-[15px] leading-[1.75] text-fetch-muted">A calm, coordinated set of everyday pieces — designed to feel good in hand and easy to wear.</p>
          </div>
          <Link to="/shop" className="inline-flex w-fit items-center text-[14px] font-medium text-fetch-ink/75 underline decoration-fetch-ink/15 underline-offset-[5px] transition duration-300 hover:text-fetch-ink hover:decoration-fetch-ink/40">View all essentials</Link>
        </div>
      </div>
      <div className="section-container">
        {loading ? (
          <div className="flex min-h-[220px] items-center justify-center">
            <span className="h-10 w-10 animate-spin rounded-full border-[3px] border-fetch-ink/10 border-t-fetch-ink" aria-label="Loading products" />
          </div>
        ) : items.length > 0 ? (
          <ProductMarquee products={items} duration="42s" />
        ) : (
          <div className="border border-fetch-ink/[0.08] bg-white/70 px-6 py-10 text-center text-[14px] text-fetch-muted">No products available right now. Check back soon.</div>
        )}
      </div>
    </section>
  );
}
