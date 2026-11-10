import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Loader2, SlidersHorizontal, X } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import ProductGrid from '../components/products/ProductGrid.jsx';
import ShopSidebar from '../components/shop/ShopSidebar.jsx';
import { EmptyState, ErrorState } from '../components/ui/StatePanel.jsx';
import { listCategories, listProducts } from '../services/catalogApi.js';

const sortOptions = [
  { value: 'featured', label: 'Featured' },
  { value: 'priceAsc', label: 'Price: low to high' },
  { value: 'priceDesc', label: 'Price: high to low' },
  { value: 'name', label: 'Name' },
];

export default function ShopPage() {
  const [searchParams] = useSearchParams();
  const initialCategory = searchParams.get('category') || '';
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [applying, setApplying] = useState(false);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const [draftCategory, setDraftCategory] = useState(initialCategory);
  const [draftAvailability, setDraftAvailability] = useState('all');
  const [appliedCategory, setAppliedCategory] = useState(initialCategory);
  const [appliedAvailability, setAppliedAvailability] = useState('all');
  const [sort, setSort] = useState('featured');

  const load = async (opts = {}) => {
    setError('');
    setLoading(true);
    try {
      const [cats, res] = await Promise.all([
        categories.length ? Promise.resolve(categories) : listCategories(),
        listProducts({ category: opts.category ?? appliedCategory, sort: opts.sort ?? (sort === 'featured' ? undefined : sort) }),
      ]);
      if (!categories.length) setCategories(cats);
      setProducts(res.content || []);
    } catch (e) {
      setError(e?.response?.data?.error?.message || 'Unable to load products.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load({ category: initialCategory }); /* eslint-disable-next-line */ }, []);
  useEffect(() => { load({ sort }); /* eslint-disable-next-line */ }, [sort]);

  const filtered = useMemo(() => {
    let list = [...products];
    if (appliedCategory && !products.every((p) => p.category === appliedCategory || p.collection === appliedCategory)) {
      // API already filtered; local fallback filter:
      list = list.filter((p) => p.category === appliedCategory || p.collection === appliedCategory);
    }
    if (appliedAvailability === 'in') list = list.filter((p) => p.inStock !== false);
    if (appliedAvailability === 'out') list = list.filter((p) => p.inStock === false);
    if (sort === 'priceAsc') list.sort((a, b) => a.price - b.price);
    if (sort === 'priceDesc') list.sort((a, b) => b.price - a.price);
    if (sort === 'name') list.sort((a, b) => a.name.localeCompare(b.name));
    return list;
  }, [products, appliedAvailability, appliedCategory, sort]);

  const applyFilters = async () => {
    setApplying(true);
    setAppliedCategory(draftCategory);
    setAppliedAvailability(draftAvailability);
    try { await load({ category: draftCategory }); } finally { setApplying(false); setMobileFiltersOpen(false); }
  };

  const clearFilters = async () => {
    setDraftCategory(''); setDraftAvailability('all'); setAppliedCategory(''); setAppliedAvailability('all');
    await load({ category: '' });
  };

  const sidebarProps = { categories, selectedCategory: draftCategory, onCategoryChange: setDraftCategory, availability: draftAvailability, onAvailabilityChange: setDraftAvailability, onClear: clearFilters, onApply: applyFilters, applying };

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-10 sm:py-12">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Store</p>
          <h1 className="mt-3 text-[2.2rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.7rem]">Shop essentials</h1>
          <p className="mt-3 max-w-[52ch] text-[15px] leading-[1.75] text-fetch-muted sm:text-[16px]">Premium dog accessories designed for daily walks, travel, and modern routines.</p>
        </div>
      </div>
      <div className="section-container py-10 lg:py-12">
        <div className="lg:grid lg:grid-cols-[260px_minmax(0,1fr)] lg:gap-10 xl:grid-cols-[280px_minmax(0,1fr)] xl:gap-12">
          <div className="hidden lg:block"><div className="sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto pr-1"><ShopSidebar {...sidebarProps} /></div></div>
          <section aria-label="Products">
            <div className="mb-6 flex flex-col gap-3 border-b border-fetch-ink/[0.08] pb-5 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-3">
                <button type="button" className="inline-flex h-10 items-center gap-2 border border-fetch-ink/12 px-4 text-[14px] font-medium text-fetch-ink transition hover:border-fetch-ink/20 lg:hidden" onClick={() => setMobileFiltersOpen(true)}><SlidersHorizontal size={16} strokeWidth={1.6} aria-hidden="true" />Filters</button>
                <p className="text-[14px] text-fetch-muted">{loading ? 'Loading products…' : `${filtered.length} ${filtered.length === 1 ? 'product' : 'products'}`}</p>
              </div>
              <label className="flex items-center gap-3 text-[14px] text-fetch-muted"><span className="shrink-0">Sort</span>
                <select value={sort} onChange={(e) => setSort(e.target.value)} className="h-10 w-full min-w-[190px] border border-fetch-ink/12 bg-white px-3 text-[14px] text-fetch-ink focus:border-fetch-ink/25 focus:outline-none sm:w-auto">
                  {sortOptions.map((opt) => (<option key={opt.value} value={opt.value}>{opt.label}</option>))}
                </select>
              </label>
            </div>
            {error ? (
              <ErrorState message={error} onAction={() => load()} />
            ) : loading ? (
              <div className="flex min-h-[420px] flex-col items-center justify-center py-16" role="status" aria-live="polite" aria-label="Loading products">
                <div className="relative flex h-16 w-16 items-center justify-center">
                  <span className="absolute inset-0 rounded-full border-[3px] border-fetch-ink/10" />
                  <span className="absolute inset-0 animate-spin rounded-full border-[3px] border-transparent border-t-fetch-ink border-r-fetch-ink/60" />
                  <Loader2 className="h-6 w-6 animate-spin text-fetch-ink/70" aria-hidden="true" />
                </div>
                <p className="mt-5 text-[14px] font-medium text-fetch-ink">Loading products</p>
                <p className="mt-1 text-[13px] text-fetch-muted">Fetching the latest essentials for you…</p>
              </div>
            ) : filtered.length === 0 ? (
              <EmptyState title="No products match these filters" message="Try a different category or availability option to see more essentials." actionLabel="Clear filters" onAction={clearFilters} />
            ) : (
              <ProductGrid products={filtered} columns={3} />
            )}
          </section>
        </div>
      </div>
      {mobileFiltersOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Filters">
          <button type="button" className="absolute inset-0 bg-fetch-ink/20" aria-label="Close filters" onClick={() => setMobileFiltersOpen(false)} />
          <div className="absolute inset-y-0 right-0 flex w-full max-w-[360px] flex-col bg-white shadow-[0_20px_60px_rgba(23,23,23,0.12)]">
            <div className="flex items-center justify-between border-b border-fetch-ink/[0.08] px-5 py-4"><span className="text-[15px] font-semibold text-fetch-ink">Filters</span><button type="button" className="inline-flex h-9 w-9 items-center justify-center text-fetch-ink/70" onClick={() => setMobileFiltersOpen(false)} aria-label="Close"><X size={18} strokeWidth={1.6} /></button></div>
            <div className="flex-1 overflow-y-auto px-5 py-5"><ShopSidebar {...sidebarProps} /></div>
          </div>
        </div>
      )}
    </StoreLayout>
  );
}
