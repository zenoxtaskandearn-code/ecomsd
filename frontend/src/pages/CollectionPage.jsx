import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import ProductGrid from '../components/products/ProductGrid.jsx';
import { ProductGridSkeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { listCategories, listProducts } from '../services/catalogApi.js';

export default function CollectionPage() {
  const { slug } = useParams();
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    (async () => {
      try {
        const [cats, res] = await Promise.all([listCategories(), listProducts({ category: slug })]);
        if (!mounted) return;
        setCategories(cats || []);
        setItems(res.content || []);
      } finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, [slug]);

  const category = useMemo(() => categories.find((c) => c.slug === slug), [categories, slug]);

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-10 sm:py-12">
          <p className="text-[13px] text-fetch-muted"><Link to="/shop" className="transition hover:text-fetch-ink">Shop</Link><span className="mx-2 text-fetch-ink/25">/</span><span className="text-fetch-ink/70">{category?.name || slug}</span></p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.5rem]">{category?.name || 'Collection'}</h1>
          <p className="mt-3 max-w-[52ch] text-[15px] leading-[1.75] text-fetch-muted">A curated selection of premium essentials designed for comfort, daily use, and a calm modern look.</p>
        </div>
      </div>
      <div className="section-container py-10 lg:py-12">
        {loading ? <ProductGridSkeleton count={6} columns={3} /> : items.length ? <ProductGrid products={items} columns={3} /> : <EmptyState title="This collection is being prepared" message="Products will appear here once the catalog is connected. Explore all essentials in the meantime." actionLabel="Browse shop" onAction={() => (window.location.href = '/shop')} />}
      </div>
    </StoreLayout>
  );
}
