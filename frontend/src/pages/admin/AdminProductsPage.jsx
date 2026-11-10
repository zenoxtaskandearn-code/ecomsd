import { useEffect, useMemo, useState } from 'react';
import { Download, Plus, Search, Trash2 } from 'lucide-react';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminProducts, adminCategories } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

const inputClass = 'h-9 rounded-lg border border-[#d1d5db] bg-white px-3 text-[13px] focus:border-[#9ca3af] focus:outline-none';
const areaClass = 'rounded-lg border border-[#d1d5db] bg-white px-3 py-2 text-[13px] focus:border-[#9ca3af] focus:outline-none';

const emptyProduct = (categories = []) => ({
  name: '', slug: '', categoryId: categories[0]?.id || '', description: '', basePriceAmount: '', currency: 'USD', status: 'DRAFT',
  cjProductId: '', supplierSource: '', supplierId: '', estimatedDelivery: '', shippingOptionsJson: '', supplierShippingCostAmount: '', internalNotes: ''
});
const emptyVariant = (p) => {
  const base = p || {};
  return {
    sku: '', title: '', optionValuesJson: '', priceAmount: String(base.basePriceAmount ?? ''), currency: base.currency || 'USD', active: true,
    cjVariantId: '', supplierSku: '', supplierCostAmount: '', shippingCostAmount: '', weightGrams: '', stockAvailability: '', warehouseInfo: '', dimensionsJson: ''
  };
};
const emptyImportVariant = () => ({
  title: '', optionValuesJson: '', sku: '', cjVariantId: '', supplierSku: '', sellingPriceAmount: '', supplierCostAmount: '', shippingCostAmount: '',
  currency: 'USD', weightGrams: '', dimensionsJson: '', stockAvailability: '', warehouseInfo: '', active: true
});

function toProductForm(p, categories) {
  if (!p) return emptyProduct(categories);
  return {
    name: p.name || '', slug: p.slug || '', categoryId: p.categoryId || p.category?.id || categories[0]?.id || '',
    description: p.description || '', basePriceAmount: String(p.basePriceAmount ?? p.price ?? ''), currency: p.currency || 'USD',
    status: p.status || (p.inStock ? 'ACTIVE' : 'DRAFT'),
    cjProductId: p.cjProductId || '', supplierSource: p.supplierSource || '', supplierId: p.supplierId ?? '',
    estimatedDelivery: p.estimatedDelivery || '', shippingOptionsJson: p.shippingOptionsJson || '',
    supplierShippingCostAmount: p.supplierShippingCostAmount ?? '', internalNotes: p.internalNotes || ''
  };
}
function num(v) { if (v === '' || v === null || v === undefined) return null; const n = Number(v); return Number.isFinite(n) ? n : null; }
function money(v) { const n = Number(v); return Number.isFinite(n) ? `$${n.toFixed(2)}` : '—'; }
function profit(selling, supplierCost, shippingCost) {
  if (selling == null || selling === '') return null;
  const s = Number(selling), c = Number(supplierCost || 0), sh = Number(shippingCost || 0);
  if (!Number.isFinite(s)) return null;
  return s - c - sh;
}
function ProfitBadge({ selling, supplierCost, shippingCost }) {
  const p = profit(selling, supplierCost, shippingCost);
  if (p == null) return <span className="text-[12px] text-[#6b7280]">—</span>;
  const ok = p >= 0;
  return <span className={`rounded-full px-2 py-0.5 text-[12px] font-medium ${ok ? 'bg-[#ecfdf3] text-[#166534]' : 'bg-[#fef2f2] text-[#991b1b]'}`}>{ok ? '+' : ''}${p.toFixed(2)}</span>;
}

export default function AdminProductsPage() {
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [cjWorking, setCjWorking] = useState('');
  const [cjMsg, setCjMsg] = useState('');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [form, setForm] = useState(emptyProduct());
  const [variants, setVariants] = useState([]);
  const [images, setImages] = useState([]);
  const [newVariant, setNewVariant] = useState(emptyVariant());
  const [newImageUrl, setNewImageUrl] = useState('');
  const [showImport, setShowImport] = useState(false);
  const [cjStatus, setCjStatus] = useState(null);
  const [importForm, setImportForm] = useState({ categoryId: '', title: '', cjProductId: '', description: '', sellingPriceAmount: '', currency: 'USD', supplierId: '', estimatedDelivery: '', shippingOptionsJson: '', supplierShippingCostAmount: '', imageUrlsText: '', internalNotes: '' });
  const [importVariants, setImportVariants] = useState([emptyImportVariant()]);
  const [importSaving, setImportSaving] = useState(false);
  const [importError, setImportError] = useState('');
  const [autoCategoryId, setAutoCategoryId] = useState('');
  const [autoMax, setAutoMax] = useState('100');
  const [autoKeyword, setAutoKeyword] = useState('');
  const [autoSaving, setAutoSaving] = useState(false);
  const [autoError, setAutoError] = useState('');
  const [autoResult, setAutoResult] = useState(null);
  const [cjPreview, setCjPreview] = useState(null);
  const [cjPreviewLoading, setCjPreviewLoading] = useState(false);
  const [cjPreviewError, setCjPreviewError] = useState('');
  const [selectedCjIds, setSelectedCjIds] = useState([]);
  const [autoAddDone, setAutoAddDone] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      if (apiEnabled) {
        const [p, c] = await Promise.all([adminApi.listProducts(), adminApi.listCategories()]);
        setProducts(p || []); setCategories(c || []);
        try { setSuppliers(await adminApi.listSuppliers() || []); } catch { setSuppliers([]); }
      } else {
        setProducts(adminProducts); setCategories(adminCategories.map((c, i) => ({ ...c, id: i + 1 }))); setSuppliers([]);
      }
    } catch { setProducts(adminProducts); setCategories(adminCategories.map((c, i) => ({ ...c, id: i + 1 }))); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);
  useEffect(() => {
    if (editing !== null) {
      const p = editing === 'new' ? null : editing;
      const pf = toProductForm(p, categories);
      setForm(pf);
      setVariants(Array.isArray(p?.variants) ? p.variants.map((v) => ({ ...emptyVariant(pf), ...v, priceAmount: String(v.priceAmount ?? ''), supplierCostAmount: v.supplierCostAmount ?? '', shippingCostAmount: v.shippingCostAmount ?? '', weightGrams: v.weightGrams ?? '', supplierId: undefined })) : []);
      setImages(Array.isArray(p?.images) ? p.images : []);
      setNewVariant(emptyVariant(pf));
      setNewImageUrl('');
      setError('');
    }
  }, [editing, categories]);
  useEffect(() => {
    if (showImport && apiEnabled && !cjStatus) {
      adminApi.getCjImportStatus().then(setCjStatus).catch(() => setCjStatus({ apiConfigured: false, mode: 'MANUAL', message: 'Manual CJ import available.' }));
    }
    if (showImport) {
      setImportForm((f) => ({ ...f, categoryId: f.categoryId || categories[0]?.id || '' }));
      setAutoCategoryId((v) => v || categories[0]?.id || '');
    }
  }, [showImport, apiEnabled, cjStatus, categories]);

  const loadCjPreview = async (opts = {}) => {
    if (!apiEnabled) return;
    setCjPreviewLoading(true); setCjPreviewError(''); setAutoError(''); setAutoResult(null); setAutoAddDone(false);
    try {
      const payload = { maxProducts: Math.min(200, Math.max(1, Number(opts.maxProducts ?? autoMax) || 100)) };
      const kw = (opts.keyword ?? autoKeyword);
      if (kw && kw.trim()) payload.keyword = kw.trim();
      const res = await adminApi.previewCjMyProducts(payload);
      setCjPreview(res);
      const ids = (res?.products || []).filter((p) => p && p.cjProductId && !p.alreadyImported).map((p) => p.cjProductId);
      setSelectedCjIds(ids.length ? ids : (res?.products || []).map((p) => p.cjProductId).filter(Boolean));
    } catch (e2) {
      setCjPreview(null); setSelectedCjIds([]);
      setCjPreviewError(e2?.response?.data?.error?.message || 'Could not load CJ My Products. Check CJ API config and restart the backend if you recently updated it.');
    } finally { setCjPreviewLoading(false); }
  };

  useEffect(() => {
    if (showImport && apiEnabled && cjStatus?.apiConfigured && !cjPreview && !cjPreviewLoading && !cjPreviewError) {
      loadCjPreview();
    }
  }, [showImport, apiEnabled, cjStatus?.apiConfigured]);

  useEffect(() => {
    if (!showImport || autoAddDone || autoSaving || cjPreviewLoading || !cjPreview?.products?.length) return;
    if (cjStatus && !cjStatus.apiConfigured) return;
    const ids = (cjPreview.products || []).filter((p) => p && p.cjProductId && !p.alreadyImported).map((p) => p.cjProductId);
    if (!ids.length) return;
    if (!autoCategoryId) return;
    setAutoAddDone(true);
    addCjProducts(ids, 'auto-add');
  }, [showImport, autoAddDone, autoSaving, cjPreviewLoading, cjPreview, cjStatus, autoCategoryId]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return products;
    return products.filter((p) => [p.name, p.slug, p.categoryName, p.category, p.categorySlug, p.cjProductId].some((v) => (v || '').toLowerCase().includes(q)));
  }, [query, products]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const setI = (k) => (e) => setImportForm((f) => ({ ...f, [k]: e.target.value }));

  const save = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      if (apiEnabled) {
        const payload = {
          ...form,
          categoryId: Number(form.categoryId),
          basePriceAmount: num(form.basePriceAmount) ?? 0,
          supplierId: form.supplierId === '' ? null : Number(form.supplierId),
          supplierShippingCostAmount: num(form.supplierShippingCostAmount),
        };
        await adminApi.saveProduct(payload, editing?.id);
      }
      setEditing(null); await load();
    } catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not save product.'); }
    finally { setSaving(false); }
  };

  const saveVariantRow = async (v) => {
    if (!apiEnabled || !editing?.id) return;
    setError('');
    try {
      await adminApi.saveVariant(editing.id, {
        sku: v.sku, title: v.title, optionValuesJson: v.optionValuesJson || null,
        priceAmount: num(v.priceAmount) ?? 0, currency: v.currency || 'USD', active: v.active !== false,
        cjVariantId: v.cjVariantId || null, supplierSku: v.supplierSku || null,
        supplierCostAmount: num(v.supplierCostAmount), shippingCostAmount: num(v.shippingCostAmount),
        weightGrams: v.weightGrams === '' ? null : Number(v.weightGrams), stockAvailability: v.stockAvailability || null,
        warehouseInfo: v.warehouseInfo || null, dimensionsJson: v.dimensionsJson || null,
      }, v.id);
      await load();
      const fresh = (await adminApi.listProducts()).find((p) => p.id === editing.id);
      if (fresh) setEditing(fresh);
    } catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not save variant.'); }
  };

  const addVariant = async () => {
    if (!apiEnabled || !editing?.id) return;
    if (!newVariant.sku || !newVariant.priceAmount) { setError('Variant SKU and selling price are required.'); return; }
    await saveVariantRow(newVariant);
    setNewVariant(emptyVariant(form));
  };

  const addImage = async () => {
    if (!apiEnabled || !editing?.id || !newImageUrl.trim()) return;
    setError('');
    try {
      await adminApi.saveImage(editing.id, { url: newImageUrl.trim(), altText: form.name || 'Product image', sortOrder: images.length });
      setNewImageUrl(''); await load();
      const fresh = (await adminApi.listProducts()).find((p) => p.id === editing.id);
      if (fresh) setEditing(fresh);
    } catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not add image.'); }
  };

  const removeImage = async (id) => {
    if (!apiEnabled || !id) return;
    try { await adminApi.deleteImage(id); await load(); const fresh = (await adminApi.listProducts()).find((p) => p.id === editing?.id); if (fresh) setEditing(fresh); } catch {}
  };

  const addCjProducts = async (ids, label) => {
    if (!apiEnabled) { setAutoError('Connect backend to import products.'); return; }
    if (!autoCategoryId) { setAutoError('Select a target category for imported products.'); return; }
    if (cjStatus && !cjStatus.apiConfigured) { setAutoError('CJ API is not configured on the backend. Set CJ_API_ENABLED=true and CJ_API_KEY, restart the backend, then try again.'); return; }
    const cjProductIds = (ids || []).filter(Boolean);
    if (!cjProductIds.length) { setAutoError(`Select at least one CJ product to ${label || 'import'}.`); return; }
    setAutoSaving(true); setAutoError(''); setAutoResult(null);
    try {
      const result = await adminApi.importCjSelected({ categoryId: Number(autoCategoryId), cjProductIds });
      setAutoResult(result);
      await load();
      await loadCjPreview({ maxProducts: autoMax, keyword: autoKeyword });
      if (result?.importedProductIds?.length) {
        const fresh = (await adminApi.listProducts()).find((p) => p.id === result.importedProductIds[0]);
        if (fresh) setEditing(fresh);
      }
    } catch (e2) {
      setAutoError(e2?.response?.data?.error?.message || 'Could not import selected CJ products. Restart the backend if you recently updated it, then check CJ API config.');
    } finally { setAutoSaving(false); }
  };

  const submitImport = async (e) => {
    e.preventDefault();
    if (!apiEnabled) { setImportError('Connect backend to import products.'); return; }
    setImportSaving(true); setImportError('');
    try {
      const imageUrls = importForm.imageUrlsText.split(/\n+/).map((s) => s.trim()).filter(Boolean);
      const variantsPayload = importVariants
        .filter((v) => v.title || v.sku || v.cjVariantId || v.sellingPriceAmount || v.supplierCostAmount)
        .map((v) => ({
          cjVariantId: v.cjVariantId || null, sku: v.sku || null, supplierSku: v.supplierSku || null, title: v.title || null,
          optionValuesJson: v.optionValuesJson || null, sellingPriceAmount: num(v.sellingPriceAmount), supplierCostAmount: num(v.supplierCostAmount),
          shippingCostAmount: num(v.shippingCostAmount), currency: v.currency || 'USD',
          weightGrams: v.weightGrams === '' ? null : Number(v.weightGrams), dimensionsJson: v.dimensionsJson || null,
          stockAvailability: v.stockAvailability || null, warehouseInfo: v.warehouseInfo || null, active: v.active !== false,
        }));
      const payload = {
        categoryId: Number(importForm.categoryId), title: importForm.title, cjProductId: importForm.cjProductId || null,
        description: importForm.description || null, sellingPriceAmount: num(importForm.sellingPriceAmount), currency: importForm.currency || 'USD',
        supplierId: importForm.supplierId === '' ? null : Number(importForm.supplierId),
        estimatedDelivery: importForm.estimatedDelivery || null, shippingOptionsJson: importForm.shippingOptionsJson || null,
        supplierShippingCostAmount: num(importForm.supplierShippingCostAmount), status: 'DRAFT', imageUrls, variants: variantsPayload, internalNotes: importForm.internalNotes || null,
      };
      const created = await adminApi.importCjManual(payload);
      setShowImport(false);
      setImportForm({ categoryId: categories[0]?.id || '', title: '', cjProductId: '', description: '', sellingPriceAmount: '', currency: 'USD', supplierId: '', estimatedDelivery: '', shippingOptionsJson: '', supplierShippingCostAmount: '', imageUrlsText: '', internalNotes: '' });
      setImportVariants([emptyImportVariant()]);
      await load();
      const fresh = (await adminApi.listProducts()).find((p) => p.id === created.id);
      if (fresh) setEditing(fresh); else setEditing({ id: created.id, ...created });
    } catch (e2) { setImportError(e2?.response?.data?.error?.message || 'Could not import CJ product.'); }
    finally { setImportSaving(false); }
  };

  const updateImportVariant = (i, k, val) => setImportVariants((rows) => rows.map((r, idx) => idx === i ? { ...r, [k]: val } : r));
  const updateVariant = (i, k, val) => setVariants((rows) => rows.map((r, idx) => idx === i ? { ...r, [k]: val } : r));

  return (
    <AdminLayout title="Products" subtitle="Manage catalog, CJdropshipping imports, pricing, and visibility" actions={
      <div className="flex flex-wrap gap-2">
        <AdminButton variant="secondary" onClick={() => setShowImport((v) => { const next = !v; if (!next) { setCjPreview(null); setCjPreviewError(''); setSelectedCjIds([]); setAutoResult(null); setAutoError(''); setCjStatus(null); setAutoAddDone(false); } return next; })}><Download size={15} /> Import from CJdropshipping</AdminButton>
        <AdminButton onClick={() => setEditing('new')}><Plus size={15} /> Add product</AdminButton>
      </div>
    }>
      {showImport && (
        <AdminCard title="Import from CJdropshipping" description="Uses official CJdropshipping API when configured (no scraping/fake endpoints). Auto-import fetches your CJ My Products and adds them as drafts. Manual import is always available.">
          {cjStatus && <p className="mb-4 rounded-lg border border-[#e5e7eb] bg-[#f9fafb] px-3 py-2 text-[13px] text-[#374151]"><strong>{cjStatus.mode}:</strong> {cjStatus.message}</p>}

          <div className="mb-5 rounded-xl border border-[#dbeafe] bg-[#eff6ff] p-4">
            <h3 className="text-[14px] font-medium text-[#111827]">CJ My Products (official API)</h3>
            <p className="mt-1 text-[12px] text-[#374151]">Click <strong>Import from CJdropshipping</strong> and this list loads your CJ <strong>My Products</strong> automatically. Products are <strong>selected by default</strong> — use <strong>Add selected</strong> or <strong>Add all</strong> to upload/import them as <strong>drafts</strong> (deduped). Review prices/variants/images, then publish. Supplier/internal costs are never exposed publicly.</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <label className="block lg:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Target category *</span><select className={`${inputClass} mt-1.5 w-full`} value={autoCategoryId} onChange={(e)=>setAutoCategoryId(e.target.value)}><option value="">Select…</option>{categories.map((c) => <option key={c.id||c.slug} value={c.id || ''}>{c.name}</option>)}</select></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Max to load</span><input className={`${inputClass} mt-1.5 w-full`} value={autoMax} onChange={(e)=>setAutoMax(e.target.value)} placeholder="100" /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Keyword (optional)</span><input className={`${inputClass} mt-1.5 w-full`} value={autoKeyword} onChange={(e)=>setAutoKeyword(e.target.value)} placeholder="Filter CJ My Products" /></label>
            </div>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <AdminButton type="button" variant="secondary" onClick={() => loadCjPreview()} loading={cjPreviewLoading} disabled={!apiEnabled || cjPreviewLoading || (cjStatus ? !cjStatus.apiConfigured : false)}>Refresh list</AdminButton>
              <AdminButton type="button" onClick={() => addCjProducts(selectedCjIds, 'add')} loading={autoSaving} disabled={!apiEnabled || autoSaving || cjPreviewLoading || !selectedCjIds.length || (cjStatus ? !cjStatus.apiConfigured : false)}>Add selected ({selectedCjIds.length})</AdminButton>
              <AdminButton type="button" variant="secondary" onClick={() => addCjProducts((cjPreview?.products || []).map((p) => p.cjProductId).filter(Boolean), 'add')} loading={autoSaving} disabled={!apiEnabled || autoSaving || cjPreviewLoading || !(cjPreview?.products?.length) || (cjStatus ? !cjStatus.apiConfigured : false)}>Add all ({cjPreview?.products?.length || 0})</AdminButton>
              <button type="button" className="text-[12px] font-medium text-[#2563eb]" onClick={() => setSelectedCjIds((cjPreview?.products || []).map((p) => p.cjProductId).filter(Boolean))} disabled={cjPreviewLoading || autoSaving}>Select all</button>
              <button type="button" className="text-[12px] text-[#6b7280]" onClick={() => setSelectedCjIds([])} disabled={cjPreviewLoading || autoSaving}>Deselect all</button>
              <AdminButton type="button" variant="ghost" onClick={() => { setShowImport(false); setAutoResult(null); setAutoError(''); setCjPreviewError(''); setCjPreview(null); setSelectedCjIds([]); setCjStatus(null); }} disabled={autoSaving}>Close</AdminButton>
            </div>
            {cjStatus && !cjStatus.apiConfigured && <p className="mt-2 text-[12px] text-[#92400e]">CJ API not configured on the running backend. Set <code>CJ_API_ENABLED=true</code> and <code>CJ_API_KEY</code>, then <strong>restart the backend</strong> and click Refresh.</p>}
            {!apiEnabled && <p className="mt-2 text-[12px] text-[#6b7280]">Backend API is not connected. Set <code>VITE_API_BASE_URL</code> and ensure the backend is running.</p>}
            {cjPreviewError && <p className="mt-3 text-[13px] text-red-700">{cjPreviewError}</p>}
            {autoError && <p className="mt-3 text-[13px] text-red-700">{autoError}</p>}
            {autoResult && (
              <div className="mt-3 rounded-lg border border-[#bfdbfe] bg-white px-3 py-2 text-[13px] text-[#111827]">
                <p><strong>Imported:</strong> {autoResult.imported} · <strong>Skipped (existing):</strong> {autoResult.skippedExisting} · <strong>Failed:</strong> {autoResult.failed} · <strong>Scanned:</strong> {autoResult.scanned}/{autoResult.requestedMax}</p>
                {!!autoResult.messages?.length && <ul className="mt-1 list-disc pl-5 text-[12px] text-[#374151]">{autoResult.messages.map((m,i)=><li key={i}>{m}</li>)}</ul>}
              </div>
            )}
            <div className="mt-3 max-h-[420px] overflow-auto rounded-xl border border-[#e5e7eb] bg-white">
              {cjPreviewLoading ? <div className="p-4 text-[13px] text-[#6b7280]">Loading CJ My Products…</div> : !cjPreview?.products?.length ? <div className="p-4 text-[13px] text-[#6b7280]">{cjStatus?.apiConfigured ? (cjPreviewError ? 'Could not load products.' : 'No CJ My Products loaded yet. Click Refresh list.') : 'Configure CJ API and restart backend to auto-load your CJ My Products.'}</div> : (
                <table className="min-w-full text-left text-[13px]">
                  <thead className="sticky top-0 bg-[#f9fafb]"><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="px-3 py-2 font-medium"><input type="checkbox" aria-label="Select all" checked={selectedCjIds.length > 0 && selectedCjIds.length === cjPreview.products.length} onChange={(e)=>setSelectedCjIds(e.target.checked ? cjPreview.products.map((p)=>p.cjProductId).filter(Boolean) : [])} /></th><th className="px-3 py-2 font-medium">Product</th><th className="px-3 py-2 font-medium">CJ ID / SKU</th><th className="px-3 py-2 font-medium">CJ price</th><th className="px-3 py-2 font-medium">Status</th></tr></thead>
                  <tbody>
                    {cjPreview.products.map((p) => {
                      const id = p.cjProductId;
                      const checked = selectedCjIds.includes(id);
                      return (
                        <tr key={id} className={`border-b border-[#f3f4f6] last:border-0 ${checked ? 'bg-[#eff6ff]' : ''}`}>
                          <td className="px-3 py-2"><input type="checkbox" checked={checked} onChange={(e)=>setSelectedCjIds((prev)=> e.target.checked ? Array.from(new Set([...prev,id])) : prev.filter((x)=>x!==id))} /></td>
                          <td className="px-3 py-2"><div className="flex items-center gap-2">{p.imageUrl ? <img src={p.imageUrl} alt="" className="h-10 w-10 rounded-lg border border-[#e5e7eb] object-cover" /> : <div className="h-10 w-10 rounded-lg bg-[#f3f4f6]" />}<span className="font-medium text-[#111827]">{p.name || 'CJ product'}</span></div></td>
                          <td className="px-3 py-2 text-[#374151]"><div>{id}</div><div className="text-[12px] text-[#6b7280]">{p.sku || ''}</div></td>
                          <td className="px-3 py-2 tabular-nums text-[#111827]">{p.sellPrice != null ? `$${Number(p.sellPrice).toFixed(2)}` : '—'}</td>
                          <td className="px-3 py-2">{p.alreadyImported ? <span className="rounded-full bg-[#f3f4f6] px-2 py-0.5 text-[12px] text-[#374151]">Already imported</span> : <span className="rounded-full bg-[#ecfdf3] px-2 py-0.5 text-[12px] text-[#166534]">Ready</span>}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
            {cjPreview?.message && !cjPreviewLoading && <p className="mt-2 text-[12px] text-[#6b7280]">{cjPreview.message}</p>}
          </div>

          <details className="rounded-xl border border-[#e5e7eb] p-3">
            <summary className="cursor-pointer text-[13px] font-medium text-[#374151]">Manual CJ import (fallback)</summary>
            <form className="mt-3 space-y-4" onSubmit={submitImport}>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Product title *</span><input required className={`${inputClass} mt-1.5 w-full`} value={importForm.title} onChange={setI('title')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Category *</span><select required className={`${inputClass} mt-1.5 w-full`} value={importForm.categoryId} onChange={setI('categoryId')}><option value="">Select…</option>{categories.map((c) => <option key={c.id||c.slug} value={c.id || ''}>{c.name}</option>)}</select></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">CJ Product ID / Ref</span><input className={`${inputClass} mt-1.5 w-full`} value={importForm.cjProductId} onChange={setI('cjProductId')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Selling price (USD)</span><input className={`${inputClass} mt-1.5 w-full`} value={importForm.sellingPriceAmount} onChange={setI('sellingPriceAmount')} placeholder="38.00" /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Supplier shipping cost (internal)</span><input className={`${inputClass} mt-1.5 w-full`} value={importForm.supplierShippingCostAmount} onChange={setI('supplierShippingCostAmount')} placeholder="4.50" /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Supplier</span><select className={`${inputClass} mt-1.5 w-full`} value={importForm.supplierId} onChange={setI('supplierId')}><option value="">None / manual</option>{suppliers.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}</select></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Estimated delivery</span><input className={`${inputClass} mt-1.5 w-full`} value={importForm.estimatedDelivery} onChange={setI('estimatedDelivery')} placeholder="7-15 business days" /></label>
              <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Description</span><textarea className={`${areaClass} mt-1.5 min-h-[88px] w-full`} value={importForm.description} onChange={setI('description')} /></label>
              <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Product image URLs (one per line)</span><textarea className={`${areaClass} mt-1.5 min-h-[72px] w-full`} value={importForm.imageUrlsText} onChange={setI('imageUrlsText')} placeholder="https://..." /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Shipping options (text/JSON)</span><textarea className={`${areaClass} mt-1.5 min-h-[72px] w-full`} value={importForm.shippingOptionsJson} onChange={setI('shippingOptionsJson')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Internal notes (admin-only)</span><textarea className={`${areaClass} mt-1.5 min-h-[72px] w-full`} value={importForm.internalNotes} onChange={setI('internalNotes')} /></label>
            </div>
            <div>
              <div className="mb-2 flex items-center justify-between"><span className="text-[13px] font-medium text-[#374151]">Variants (size/color/SKU/supplier cost/stock/warehouse)</span><AdminButton type="button" variant="ghost" className="h-8 px-2.5" onClick={() => setImportVariants((v) => [...v, emptyImportVariant()])}>Add variant</AdminButton></div>
              <div className="space-y-3">
                {importVariants.map((v, i) => (
                  <div key={i} className="rounded-xl border border-[#e5e7eb] p-3">
                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                      <input className={inputClass} placeholder="Title (e.g. Beige / M)" value={v.title} onChange={(e)=>updateImportVariant(i,'title',e.target.value)} />
                      <input className={inputClass} placeholder={'Options JSON {"Size":"M"}'} value={v.optionValuesJson} onChange={(e)=>updateImportVariant(i,'optionValuesJson',e.target.value)} />
                      <input className={inputClass} placeholder="SKU" value={v.sku} onChange={(e)=>updateImportVariant(i,'sku',e.target.value)} />
                      <input className={inputClass} placeholder="CJ variant ID" value={v.cjVariantId} onChange={(e)=>updateImportVariant(i,'cjVariantId',e.target.value)} />
                      <input className={inputClass} placeholder="Selling price" value={v.sellingPriceAmount} onChange={(e)=>updateImportVariant(i,'sellingPriceAmount',e.target.value)} />
                      <input className={inputClass} placeholder="Supplier cost (internal)" value={v.supplierCostAmount} onChange={(e)=>updateImportVariant(i,'supplierCostAmount',e.target.value)} />
                      <input className={inputClass} placeholder="Shipping cost (internal)" value={v.shippingCostAmount} onChange={(e)=>updateImportVariant(i,'shippingCostAmount',e.target.value)} />
                      <div className="flex items-center gap-2 text-[13px]">Profit: <ProfitBadge selling={v.sellingPriceAmount || importForm.sellingPriceAmount} supplierCost={v.supplierCostAmount} shippingCost={v.shippingCostAmount || importForm.supplierShippingCostAmount} /></div>
                      <input className={inputClass} placeholder="Stock availability" value={v.stockAvailability} onChange={(e)=>updateImportVariant(i,'stockAvailability',e.target.value)} />
                      <input className={inputClass} placeholder="Warehouse" value={v.warehouseInfo} onChange={(e)=>updateImportVariant(i,'warehouseInfo',e.target.value)} />
                      <input className={inputClass} placeholder="Weight (g)" value={v.weightGrams} onChange={(e)=>updateImportVariant(i,'weightGrams',e.target.value)} />
                      <input className={inputClass} placeholder='Dimensions JSON {"l":10,"w":5}' value={v.dimensionsJson} onChange={(e)=>updateImportVariant(i,'dimensionsJson',e.target.value)} />
                    </div>
                    {importVariants.length > 1 && <button type="button" className="mt-2 text-[12px] text-red-700" onClick={() => setImportVariants((rows) => rows.filter((_, idx) => idx !== i))}>Remove variant</button>}
                  </div>
                ))}
              </div>
            </div>
            {importError && <p className="text-[13px] text-red-700">{importError}</p>}
            <div className="flex flex-wrap gap-2"><AdminButton type="submit" loading={importSaving}>Import as draft</AdminButton><AdminButton type="button" variant="secondary" onClick={() => setShowImport(false)} disabled={importSaving}>Cancel</AdminButton></div>
            <p className="text-[12px] text-[#6b7280]">Supplier costs and internal information are stored for admin use only and are never exposed by public product APIs. If official CJ API access is not configured, use manual import only.</p>
          </form>
          </details>
        </AdminCard>
      )}

      {editing && (
        <AdminCard title={editing === 'new' ? 'Add product' : `Edit: ${editing.name}`} description="Edit details, variants, images, and publish when ready. Drafts are not shown on the storefront.">
          <form className="space-y-4" onSubmit={save}>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Product name</span><input required className={`${inputClass} mt-1.5 w-full`} value={form.name} onChange={set('name')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Slug</span><input required className={`${inputClass} mt-1.5 w-full`} value={form.slug} onChange={set('slug')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Base selling price (USD)</span><input required className={`${inputClass} mt-1.5 w-full`} value={form.basePriceAmount} onChange={set('basePriceAmount')} placeholder="38.00" /></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Category</span><select required className={`${inputClass} mt-1.5 w-full`} value={form.categoryId} onChange={set('categoryId')}><option value="">Select…</option>{categories.map((c) => <option key={c.id||c.slug} value={c.id || ''}>{c.name}</option>)}</select></label>
              <label className="block"><span className="text-[13px] font-medium text-[#374151]">Status</span><select className={`${inputClass} mt-1.5 w-full`} value={form.status} onChange={set('status')}><option value="DRAFT">Draft</option><option value="ACTIVE">Active (published)</option><option value="ARCHIVED">Archived</option></select></label>
              <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Description</span><textarea className={`${areaClass} mt-1.5 min-h-[88px] w-full`} value={form.description||''} onChange={set('description')} /></label>
            </div>
            <details className="rounded-xl border border-[#e5e7eb] p-3" open={!!(form.cjProductId || form.supplierSource || form.supplierId)}>
              <summary className="cursor-pointer text-[13px] font-medium text-[#374151]">CJdropshipping / supplier (admin-only, never public)</summary>
              <div className="mt-3 grid gap-4 sm:grid-cols-2">
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">CJ Product ID</span><input className={`${inputClass} mt-1.5 w-full`} value={form.cjProductId||''} onChange={set('cjProductId')} /></label>
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">Supplier source</span><input className={`${inputClass} mt-1.5 w-full`} value={form.supplierSource||''} onChange={set('supplierSource')} placeholder="CJ_MANUAL" /></label>
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">Supplier</span><select className={`${inputClass} mt-1.5 w-full`} value={form.supplierId??''} onChange={set('supplierId')}><option value="">None</option>{suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">Supplier shipping cost (internal)</span><input className={`${inputClass} mt-1.5 w-full`} value={form.supplierShippingCostAmount??''} onChange={set('supplierShippingCostAmount')} /></label>
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">Estimated delivery</span><input className={`${inputClass} mt-1.5 w-full`} value={form.estimatedDelivery||''} onChange={set('estimatedDelivery')} /></label>
                <label className="block"><span className="text-[13px] font-medium text-[#374151]">Shipping options (text/JSON)</span><input className={`${inputClass} mt-1.5 w-full`} value={form.shippingOptionsJson||''} onChange={set('shippingOptionsJson')} /></label>
                <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Internal notes</span><textarea className={`${areaClass} mt-1.5 min-h-[72px] w-full`} value={form.internalNotes||''} onChange={set('internalNotes')} /></label>
              </div>
            </details>
            {cjMsg && <p className="text-[13px] text-[#374151]">{cjMsg}</p>}
            {error && <p className="text-[13px] text-red-700">{error}</p>}
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <AdminButton type="submit" loading={saving}>Save product</AdminButton>
              {form?.id && form.cjProductId && apiEnabled && (
                <>
                  <AdminButton type="button" variant="secondary" loading={cjWorking==='refresh'} disabled={saving || !!cjWorking} onClick={async ()=>{ setCjWorking('refresh'); setCjMsg(''); try { const r = await adminApi.refreshCjProduct(form.id); setCjMsg(r?.message || 'Refreshed from CJ.'); await load(); } catch(e){ setCjMsg(e?.response?.data?.error?.message || 'Refresh from CJ failed.'); } finally { setCjWorking(''); } }}>Refresh from CJ</AdminButton>
                  <AdminButton type="button" variant="secondary" loading={cjWorking==='stock'} disabled={saving || !!cjWorking} onClick={async ()=>{ setCjWorking('stock'); setCjMsg(''); try { const r = await adminApi.syncCjProductStock(form.id); setCjMsg(r?.message || 'CJ stock sync completed.'); await load(); } catch(e){ setCjMsg(e?.response?.data?.error?.message || 'CJ stock sync failed.'); } finally { setCjWorking(''); } }}>Sync CJ stock</AdminButton>
                </>
              )}
              <AdminButton type="button" variant="secondary" onClick={() => { setEditing(null); setCjMsg(''); }} disabled={saving || !!cjWorking}>Cancel</AdminButton>
            </div>
          </form>

          {apiEnabled && editing?.id && (
            <>
              <div className="mt-6 border-t border-[#eef0f2] pt-5">
                <h3 className="text-[14px] font-medium text-[#111827]">Variants & profit</h3>
                <p className="mt-1 text-[12px] text-[#6b7280]">Selling price minus supplier cost minus shipping cost. Supplier costs are admin-only.</p>
                <div className="mt-3 space-y-3">
                  {variants.map((v, i) => (
                    <div key={v.id || i} className="rounded-xl border border-[#e5e7eb] p-3">
                      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                        <input className={inputClass} placeholder="Title" value={v.title||''} onChange={(e)=>updateVariant(i,'title',e.target.value)} />
                        <input className={inputClass} placeholder="SKU" value={v.sku||''} onChange={(e)=>updateVariant(i,'sku',e.target.value)} />
                        <input className={inputClass} placeholder='Options JSON' value={v.optionValuesJson||''} onChange={(e)=>updateVariant(i,'optionValuesJson',e.target.value)} />
                        <input className={inputClass} placeholder="Selling price" value={v.priceAmount??''} onChange={(e)=>updateVariant(i,'priceAmount',e.target.value)} />
                        <input className={inputClass} placeholder="Supplier cost (internal)" value={v.supplierCostAmount??''} onChange={(e)=>updateVariant(i,'supplierCostAmount',e.target.value)} />
                        <input className={inputClass} placeholder="Shipping cost (internal)" value={v.shippingCostAmount??''} onChange={(e)=>updateVariant(i,'shippingCostAmount',e.target.value)} />
                        <input className={inputClass} placeholder="Stock" value={v.stockAvailability||''} onChange={(e)=>updateVariant(i,'stockAvailability',e.target.value)} />
                        <div className="flex items-center gap-2 text-[13px]">Profit <ProfitBadge selling={v.priceAmount} supplierCost={v.supplierCostAmount} shippingCost={v.shippingCostAmount ?? form.supplierShippingCostAmount} /></div>
                        <input className={inputClass} placeholder="CJ variant ID" value={v.cjVariantId||''} onChange={(e)=>updateVariant(i,'cjVariantId',e.target.value)} />
                        <input className={inputClass} placeholder="Warehouse" value={v.warehouseInfo||''} onChange={(e)=>updateVariant(i,'warehouseInfo',e.target.value)} />
                        <input className={inputClass} placeholder="Weight (g)" value={v.weightGrams??''} onChange={(e)=>updateVariant(i,'weightGrams',e.target.value)} />
                        <input className={inputClass} placeholder="Dimensions JSON" value={v.dimensionsJson||''} onChange={(e)=>updateVariant(i,'dimensionsJson',e.target.value)} />
                      </div>
                      <div className="mt-2 flex gap-2"><AdminButton type="button" variant="secondary" className="h-8 px-2.5" onClick={() => saveVariantRow(v)}>Save variant</AdminButton></div>
                    </div>
                  ))}
                  <div className="rounded-xl border border-dashed border-[#d1d5db] p-3">
                    <p className="mb-2 text-[13px] font-medium text-[#374151]">Add variant</p>
                    <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                      <input className={inputClass} placeholder="Title" value={newVariant.title} onChange={(e)=>setNewVariant(v=>({...v,title:e.target.value}))} />
                      <input className={inputClass} placeholder="SKU *" value={newVariant.sku} onChange={(e)=>setNewVariant(v=>({...v,sku:e.target.value}))} />
                      <input className={inputClass} placeholder='Options JSON' value={newVariant.optionValuesJson} onChange={(e)=>setNewVariant(v=>({...v,optionValuesJson:e.target.value}))} />
                      <input className={inputClass} placeholder="Selling price *" value={newVariant.priceAmount} onChange={(e)=>setNewVariant(v=>({...v,priceAmount:e.target.value}))} />
                      <input className={inputClass} placeholder="Supplier cost (internal)" value={newVariant.supplierCostAmount} onChange={(e)=>setNewVariant(v=>({...v,supplierCostAmount:e.target.value}))} />
                      <input className={inputClass} placeholder="Shipping cost (internal)" value={newVariant.shippingCostAmount} onChange={(e)=>setNewVariant(v=>({...v,shippingCostAmount:e.target.value}))} />
                      <input className={inputClass} placeholder="Stock / warehouse" value={newVariant.stockAvailability} onChange={(e)=>setNewVariant(v=>({...v,stockAvailability:e.target.value}))} />
                      <div className="flex items-center gap-2 text-[13px]">Profit <ProfitBadge selling={newVariant.priceAmount} supplierCost={newVariant.supplierCostAmount} shippingCost={newVariant.shippingCostAmount || form.supplierShippingCostAmount} /></div>
                    </div>
                    <div className="mt-2"><AdminButton type="button" variant="secondary" className="h-8 px-2.5" onClick={addVariant}>Add variant</AdminButton></div>
                  </div>
                </div>
              </div>
              <div className="mt-6 border-t border-[#eef0f2] pt-5">
                <h3 className="text-[14px] font-medium text-[#111827]">Images</h3>
                <div className="mt-3 flex flex-wrap gap-3">
                  {images.map((img) => (
                    <div key={img.id || img.url} className="relative">
                      <img src={img.url} alt={img.altText||''} className="h-20 w-20 rounded-lg border border-[#e5e7eb] object-cover" />
                      {img.id && <button type="button" className="absolute -right-1 -top-1 rounded-full bg-white p-1 shadow ring-1 ring-[#e5e7eb]" onClick={() => removeImage(img.id)} aria-label="Delete image"><Trash2 size={12} /></button>}
                    </div>
                  ))}
                </div>
                <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                  <input className={`${inputClass} w-full`} placeholder="https://image-url" value={newImageUrl} onChange={(e)=>setNewImageUrl(e.target.value)} />
                  <AdminButton type="button" variant="secondary" onClick={addImage}>Add image</AdminButton>
                </div>
              </div>
            </>
          )}
          {editing?.id && !apiEnabled && <p className="mt-4 text-[12px] text-[#6b7280]">Demo mode: connect backend to manage variants/images and CJ imports.</p>}
        </AdminCard>
      )}

      <AdminCard title="Product catalog" description={`${filtered.length} products`}>
        <div className="mb-4 flex items-center gap-3">
          <div className="relative w-full max-w-[320px]"><Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#9ca3af]" /><input className={`${inputClass} w-full pl-9`} placeholder="Search products..." value={query} onChange={(e) => setQuery(e.target.value)} /></div>
        </div>
        {loading ? <div className="space-y-2">{[1,2,3,4].map((i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Product</th><th className="py-2 pr-4 font-medium">Category</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 pr-4 font-medium">Price</th><th className="py-2 font-medium"></th></tr></thead>
            <tbody>{filtered.map((p) => (<tr key={p.id || p.slug} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4"><div className="flex items-center gap-3">{(p.images?.[0]?.url || p.image) ? <img src={p.images?.[0]?.url || p.image} alt="" className="h-10 w-10 rounded-lg bg-[#f3f4f6] object-cover" /> : <div className="h-10 w-10 rounded-lg bg-[#f3f4f6]" />}<div><p className="font-medium text-[#111827]">{p.name}</p><p className="text-[12px] text-[#6b7280]">{p.slug}{p.cjProductId ? ` · CJ ${p.cjProductId}` : ''}</p></div></div></td><td className="py-3 pr-4 text-[#374151]">{p.categoryName || p.category || p.categorySlug || '—'}</td><td className="py-3 pr-4"><span className={`rounded-full px-2 py-1 text-[12px] ${(p.status==='ACTIVE' || p.inStock) ? 'bg-[#ecfdf3] text-[#166534]' : 'bg-[#f3f4f6] text-[#374151]'}`}>{p.status || (p.inStock ? 'Active' : 'Draft')}</span></td><td className="py-3 pr-4 tabular-nums text-[#111827]">${Number(p.basePriceAmount ?? p.price ?? 0).toFixed(2)}</td><td className="py-3"><AdminButton variant="ghost" className="h-8 px-2.5" onClick={() => setEditing(p)}>Edit</AdminButton></td></tr>))}</tbody>
          </table></div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
