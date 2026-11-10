import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import ProductGrid from '../components/products/ProductGrid.jsx';
import Button from '../components/ui/Button.jsx';
import { ProductGridSkeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { listProducts } from '../services/catalogApi.js';

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') || '';
  const [input, setInput] = useState(q);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [results, setResults] = useState([]);

  useEffect(() => {
    setInput(q);
    if (!q) { setResults([]); return; }
    let mounted = true;
    setLoading(true);
    (async () => {
      try {
        const res = await listProducts({ q });
        if (mounted) setResults(res.content || []);
      } finally { if (mounted) setLoading(false); }
    })();
    return () => { mounted = false; };
  }, [q]);

  const submit = async (e) => {
    e.preventDefault();
    setSearching(true);
    setSearchParams(input.trim() ? { q: input.trim() } : {});
    setTimeout(() => setSearching(false), 250);
  };

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-10 sm:py-12">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Search</p>
          <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.5rem]">Find essentials</h1>
          <form className="mt-6 flex max-w-[640px] flex-col gap-3 sm:flex-row" onSubmit={submit}>
            <label htmlFor="site-search" className="sr-only">Search products</label>
            <div className="relative flex-1">
              <Search size={18} strokeWidth={1.6} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-fetch-ink/35" aria-hidden="true" />
              <input id="site-search" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Search collars, leashes, harnesses..." className="h-11 w-full border border-fetch-ink/12 bg-white pl-11 pr-4 text-[15px] text-fetch-ink placeholder:text-fetch-muted/80 focus:border-fetch-ink/25 focus:outline-none" />
            </div>
            <Button type="submit" loading={searching} className="sm:min-w-[120px]">Search</Button>
          </form>
        </div>
      </div>
      <div className="section-container py-10 lg:py-12">
        {!q ? <EmptyState title="Search the collection" message="Enter a product name, category, or keyword to see matching essentials." /> : loading ? <ProductGridSkeleton count={6} columns={3} /> : results.length ? (<><p className="mb-6 text-[14px] text-fetch-muted">{results.length} results for <span className="text-fetch-ink">“{q}”</span></p><ProductGrid products={results} columns={3} /></>) : (<EmptyState title={`No results for “${q}”`} message="Try a broader term like collar, leash, harness, or travel." actionLabel="Browse shop" onAction={() => (window.location.href = '/shop')} />)}
      </div>
    </StoreLayout>
  );
}
