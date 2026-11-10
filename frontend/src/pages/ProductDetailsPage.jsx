import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Minus, Plus, Check } from 'lucide-react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';
import { getProduct } from '../services/catalogApi.js';
import { useCart } from '../context/CartContext.jsx';
import { sanitizeHtml, hasHtml } from '../utils/sanitizeHtml.js';

const COLOR_MAP = {
  black: '#111827', white: '#f9fafb', red: '#dc2626', blue: '#2563eb', green: '#16a34a', brown: '#92400e',
  beige: '#d6c7b2', tan: '#c4a574', grey: '#9ca3af', gray: '#9ca3af', pink: '#ec4899', yellow: '#eab308',
  orange: '#f97316', purple: '#9333ea', navy: '#1e3a5f', khaki: '#bdb76b', camouflage: '#6b7280', camo: '#6b7280',
  silver: '#cbd5e1', gold: '#ca8a04', coffee: '#6f4e37',
};
function colorSwatch(value) {
  if (!value) return null;
  const key = String(value).trim().toLowerCase();
  if (COLOR_MAP[key]) return COLOR_MAP[key];
  for (const [k, v] of Object.entries(COLOR_MAP)) { if (key.includes(k)) return v; }
  return null;
}
function parseOpts(json) { try { return json ? JSON.parse(json) : null; } catch { return null; } }
function displayOptEntries(opts) {
  if (!opts || typeof opts !== 'object') return [];
  return Object.entries(opts).filter(([k,v]) => v && k !== 'Variant').map(([k,v]) => {
    const val = String(v);
    const isColorKey = /colou?r/i.test(k);
    const swatch = isColorKey || colorSwatch(val) ? colorSwatch(val) : null;
    let label = k;
    if (/^option\s*\d+$/i.test(k) && swatch) label = 'Color';
    return { key: k, label, value: val, swatch };
  });
}

export default function ProductDetailsPage() {
  const { slug } = useParams();
  const { addItem } = useCart();
  const [loading, setLoading] = useState(true);
  const [product, setProduct] = useState(null);
  const [selectedVariantId, setSelectedVariantId] = useState(null);
  const [activeImage, setActiveImage] = useState('');
  const [qty, setQty] = useState(1);
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const [error, setError] = useState('');
  const [showFullDesc, setShowFullDesc] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true); setQty(1); setAdded(false); setError('');
    (async () => {
      try {
        const p = await getProduct(slug);
        if (!mounted) return;
        setProduct(p);
        setSelectedVariantId(p?.variants?.[0]?.id || null);
        setActiveImage(p?.images?.[0]?.url || p?.image || '');
        setShowFullDesc(false);
      } catch {
        if (mounted) setProduct(null);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [slug]);

  const selectedVariant = useMemo(() => product?.variants?.find((v) => v.id === selectedVariantId) || product?.variants?.[0], [product, selectedVariantId]);
  const displayPrice = Number(selectedVariant?.price ?? product?.price ?? 0);
  const gallery = useMemo(() => {
    const imgs = (product?.images || []).map((i) => i?.url || i).filter(Boolean);
    return imgs.length ? imgs : (product?.image ? [product.image] : []);
  }, [product]);
  const selectedOptions = useMemo(() => parseOpts(selectedVariant?.optionValuesJson), [selectedVariant]);
  const detailItems = useMemo(() => {
    const items = [];
    if (product?.estimatedDelivery) items.push(`Estimated delivery: ${product.estimatedDelivery}`);
    if (selectedVariant?.sku) items.push(`SKU: ${selectedVariant.sku}`);
    if (selectedOptions && typeof selectedOptions === 'object') {
      for (const [k, v] of Object.entries(selectedOptions)) { if (v && k !== 'Variant') items.push(`${k}: ${v}`); }
    } else if (selectedVariant?.title) items.push(`Option: ${selectedVariant.title}`);
    if (product) items.push('Final delivery, taxes, and return eligibility are confirmed at checkout/policy pages.');
    return items;
  }, [product, selectedVariant, selectedOptions]);
  const mainImage = activeImage || gallery[0] || product?.image || '';
  const canBuy = !!product && product.inStock !== false && (!product.variants?.length || selectedVariant?.active !== false);

  const onAdd = async () => {
    if (!product) return;
    setAdding(true); setError('');
    try {
      await addItem({ ...product, _variantId: selectedVariant?.id, variant: selectedVariant?.title || product.variant, price: displayPrice }, qty);
      setAdded(true);
      window.setTimeout(() => setAdded(false), 1800);
    } catch (e) {
      setError(e?.response?.data?.error?.message || 'Could not add item. Sign in and try again.');
    } finally {
      setAdding(false);
    }
  };

  if (loading) {
    return (
      <StoreLayout>
        <div className="section-container py-10 lg:py-14">
          <div className="grid gap-10 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] lg:gap-14">
            <Skeleton className="aspect-[4/5] w-full" />
            <div><Skeleton className="h-4 w-24" /><Skeleton className="mt-4 h-10 w-[80%]" /><Skeleton className="mt-4 h-6 w-20" /><Skeleton className="mt-6 h-24 w-full" /><Skeleton className="mt-8 h-11 w-44 rounded-pill" /></div>
          </div>
        </div>
      </StoreLayout>
    );
  }

  if (!product) {
    return (
      <StoreLayout>
        <div className="section-container py-14">
          <EmptyState title="Product not found" message="This product may have moved or is not available yet." actionLabel="Back to shop" onAction={() => (window.location.href = '/shop')} />
        </div>
      </StoreLayout>
    );
  }

  return (
    <StoreLayout>
      <div className="section-container py-10 pb-28 lg:py-14 lg:pb-28">
        <p className="text-[13px] text-fetch-muted"><Link to="/shop" className="transition hover:text-fetch-ink">Shop</Link><span className="mx-2 text-fetch-ink/25">/</span><span className="text-fetch-ink/70">{product.name}</span></p>
        <div className="mt-6 grid gap-10 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] lg:gap-14">
          <div>
            <div className="bg-[#F0EEE9]"><div className="aspect-[4/5] overflow-hidden"><img src={mainImage} alt={product.name} className="h-full w-full object-cover" /></div></div>
            {gallery.length > 1 && (
              <div className="mt-3 flex flex-wrap gap-2">
                {gallery.slice(0, 12).map((src, i) => (
                  <button key={src + i} type="button" onClick={() => setActiveImage(src)} className={`h-16 w-16 overflow-hidden border ${mainImage === src ? 'border-fetch-ink/40' : 'border-fetch-ink/10'} bg-[#F0EEE9]`}>
                    <img src={src} alt="" className="h-full w-full object-cover" />
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="lg:py-2">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">{(product.category || '').replace('-', ' ')}</p>
            <h1 className="mt-3 text-[2.1rem] font-semibold leading-[1.06] tracking-[-0.03em] text-fetch-ink sm:text-[2.55rem]">{product.name}</h1>
            <p className="mt-3 text-[1.15rem] font-medium tabular-nums text-fetch-ink">${displayPrice.toFixed(2)}</p>
            {(selectedVariant?.title || product.variant) && <p className="mt-1 text-[14px] text-fetch-muted">{selectedVariant?.title || product.variant}</p>}

            {product.variants?.length > 0 && (
              <div className="mt-6">
                <label className="text-[13px] font-medium text-fetch-ink/80">{product.variants.length > 1 ? 'Choose option' : 'Option'}</label>
                {product.variants.length > 1 ? (
                  <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {product.variants.filter((v)=>v.active !== false).map((v) => {
                      const selected = v.id === selectedVariantId;
                      const opts = parseOpts(v.optionValuesJson);
                      const entries = displayOptEntries(opts);
                      const optText = entries.length ? entries.map((e)=>e.value).join(' / ') : (v.title || v.sku);
                      const swatch = entries.find((e)=>e.swatch)?.swatch;
                      return (
                        <button key={v.id} type="button" onClick={() => { setSelectedVariantId(v.id); if (v.imageUrl) setActiveImage(v.imageUrl); }}
                          className={`flex items-center gap-3 rounded-xl border p-2 text-left transition ${selected ? 'border-fetch-ink/40 bg-white ring-1 ring-fetch-ink/15' : 'border-fetch-ink/10 bg-white hover:border-fetch-ink/25'}`}>
                          {v.imageUrl ? <img src={v.imageUrl} alt="" className="h-12 w-12 shrink-0 rounded-lg object-cover bg-[#F0EEE9]" /> : swatch ? <span className="h-12 w-12 shrink-0 rounded-lg border border-fetch-ink/10" style={{ backgroundColor: swatch }} title={optText} /> : <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-[#F0EEE9] text-[11px] font-medium text-fetch-ink/50">{(optText || '•').slice(0,2)}</div>}
                          <span className="min-w-0">
                            <span className="block truncate text-[13px] font-medium text-fetch-ink">{optText || v.sku}</span>
                            <span className="block text-[12px] tabular-nums text-fetch-muted">${Number(v.price).toFixed(2)}</span>
                          </span>
                        </button>
                      );
                    })}
                  </div>
                ) : (
                  <p className="mt-1.5 text-[14px] text-fetch-muted">{selectedVariant?.title || selectedVariant?.sku || 'Default'}</p>
                )}
                {selectedOptions && typeof selectedOptions === 'object' && (
                  <div className="mt-2 flex flex-wrap gap-2">
                    {displayOptEntries(selectedOptions).map((e) => (
                      <span key={e.key} className="inline-flex items-center gap-1.5 rounded-full border border-fetch-ink/10 bg-white px-2.5 py-1 text-[12px] text-fetch-muted">
                        {e.swatch && <span className="h-3.5 w-3.5 rounded-full border border-fetch-ink/10" style={{ backgroundColor: e.swatch }} aria-hidden="true" />}
                        <span className="text-fetch-ink/70">{e.label}:</span> {e.value}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="mt-8 flex flex-wrap items-center gap-4">
              <div className="inline-flex h-11 items-center border border-fetch-ink/12">
                <button type="button" className="inline-flex h-11 w-11 items-center justify-center text-fetch-ink/70 transition hover:text-fetch-ink" aria-label="Decrease quantity" onClick={() => setQty((q) => Math.max(1, q - 1))}><Minus size={16} strokeWidth={1.6} /></button>
                <span className="min-w-[2.5rem] text-center text-[15px] font-medium tabular-nums text-fetch-ink" aria-live="polite">{qty}</span>
                <button type="button" className="inline-flex h-11 w-11 items-center justify-center text-fetch-ink/70 transition hover:text-fetch-ink" aria-label="Increase quantity" onClick={() => setQty((q) => q + 1)}><Plus size={16} strokeWidth={1.6} /></button>
              </div>
              <Button onClick={onAdd} loading={adding} disabled={!canBuy} className="min-w-[180px]">
                {added ? (<><Check size={16} strokeWidth={2} /> Added</>) : canBuy ? 'Add to cart' : 'Unavailable'}
              </Button>
              {added && <Link to="/cart" className="text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4">View cart</Link>}
            </div>
            {error && <p className="mt-3 text-[14px] text-red-700">{error}</p>}
            {!canBuy && <p className="mt-3 text-[14px] text-fetch-muted">This item is currently unavailable.</p>}
            {product.estimatedDelivery && <p className="mt-3 text-[14px] text-fetch-muted"><span className="font-medium text-fetch-ink/80">Estimated delivery:</span> {product.estimatedDelivery}</p>}

            <div className="mt-10 border-t border-fetch-ink/[0.08] pt-7">
              <h2 className="text-[13px] font-semibold uppercase tracking-[0.14em] text-fetch-ink/55">Description</h2>
              {hasHtml(product.description) ? (
                <div className="mt-4">
                  <div className={`cj-product-description relative max-w-none overflow-hidden text-[15px] leading-[1.85] text-fetch-muted sm:text-[16px] [&_.cj-desc-img]:mx-auto [&_.cj-desc-img]:my-3 [&_.cj-desc-img]:block [&_.cj-desc-img]:h-auto [&_.cj-desc-img]:max-h-[320px] [&_.cj-desc-img]:w-auto [&_.cj-desc-img]:max-w-full [&_.cj-desc-img]:rounded-sm [&_.cj-desc-img]:object-contain [&_img]:mx-auto [&_img]:my-3 [&_img]:block [&_img]:h-auto [&_img]:!max-h-[320px] [&_img]:w-auto [&_img]:!max-w-full [&_img]:rounded-sm [&_img]:object-contain [&_p]:mb-3 [&_ul]:mb-3 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mb-3 [&_ol]:list-decimal [&_ol]:pl-5 ${showFullDesc ? '' : 'max-h-[520px]'}`} dangerouslySetInnerHTML={{ __html: sanitizeHtml(product.description) }} />
                  {!showFullDesc && (product.description?.length > 700 || (product.description?.match(/<img/gi) || []).length > 1) && (
                    <button type="button" className="mt-2 text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4" onClick={() => setShowFullDesc(true)}>Show full description</button>
                  )}
                  {showFullDesc && <button type="button" className="mt-2 text-[14px] font-medium text-fetch-ink underline decoration-fetch-ink/20 underline-offset-4" onClick={() => setShowFullDesc(false)}>Show less</button>}
                </div>
              ) : (
                <p className={`mt-4 max-w-[52ch] whitespace-pre-wrap text-[15px] leading-[1.85] text-fetch-muted sm:text-[16px] ${showFullDesc ? '' : 'max-h-[320px] overflow-hidden'}`}>{product.description || 'No description provided.'}</p>
              )}
            </div>

            <div className="mt-10 border-t border-fetch-ink/[0.08] pt-7">
              <h2 className="text-[13px] font-semibold uppercase tracking-[0.14em] text-fetch-ink/55">Details</h2>
              <ul className="mt-4 space-y-2.5">
                {detailItems.map((d) => (
                  <li key={d} className="flex gap-3 text-[15px] leading-[1.65] text-fetch-muted"><span className="mt-[0.55rem] h-1 w-1 shrink-0 rounded-full bg-fetch-ink/25" aria-hidden="true" /><span>{d}</span></li>
                ))}
              </ul>
            </div>
            <div className="mt-7 border-t border-fetch-ink/[0.08] pt-6 text-[14px] leading-[1.75] text-fetch-muted">
              <p><span className="font-medium text-fetch-ink/80">Shipping & returns:</span> Final delivery estimates and return eligibility are confirmed at checkout and in policy pages.</p>
            </div>
          </div>
        </div>
      </div>

      <div className="pointer-events-none fixed inset-x-0 bottom-0 z-40 border-t border-fetch-ink/10 bg-[#FAF8F4]/95 backdrop-blur supports-[backdrop-filter]:bg-[#FAF8F4]/85">
        <div className="section-container pointer-events-auto flex items-center justify-center gap-3 py-3 sm:justify-between">
          <div className="hidden min-w-0 sm:block">
            <p className="truncate text-[13px] font-medium text-fetch-ink">{product.name}</p>
            <p className="text-[12px] tabular-nums text-fetch-muted">{selectedVariant?.title ? `${selectedVariant.title} · ` : ''}${'$'}{displayPrice.toFixed(2)}</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-[14px] font-medium tabular-nums text-fetch-ink sm:hidden">${displayPrice.toFixed(2)}</span>
            <Button onClick={onAdd} loading={adding} disabled={!canBuy} className="min-w-[180px]">
              {added ? (<><Check size={16} strokeWidth={2} /> Added</>) : canBuy ? 'Add to cart' : 'Unavailable'}
            </Button>
          </div>
        </div>
      </div>
    </StoreLayout>
  );
}
