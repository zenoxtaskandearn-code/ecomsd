import { api, apiEnabled } from './api.js';
import * as local from '../utils/catalog.js';

function mapSummary(p) {
  return {
    id: String(p.id),
    slug: p.slug,
    name: p.name,
    price: Number(p.priceAmount ?? p.basePriceAmount ?? 0),
    category: p.categorySlug || p.category?.slug || '',
    collection: p.categorySlug || p.category?.slug || '',
    variant: p.variantTitle || p.variants?.[0]?.title || '',
    description: p.description || '',
    details: [],
    inStock: p.status ? p.status === 'ACTIVE' : true,
    image: p.primaryImageUrl || p.images?.[0]?.url || local.products[0]?.image,
    currency: p.currency || 'USD',
    variants: (p.variants || []).map((v) => ({ id: v.id, title: v.title, price: Number(v.priceAmount), sku: v.sku, active: v.active })),
    images: p.images || [],
    _raw: p,
  };
}

export async function listCategories() {
  if (!apiEnabled) return local.categories;
  const { data } = await api.get('/categories');
  return data.map((c) => ({ slug: c.slug, name: c.name, id: c.id, description: c.description, active: c.active }));
}

export async function listProducts({ category, q, sort, page = 0, size = 48 } = {}) {
  if (!apiEnabled) {
    let list = [...local.products];
    if (category) list = list.filter((p) => p.category === category || p.collection === category);
    if (q) { const s = q.toLowerCase(); list = list.filter((p) => [p.name, p.description, p.category].some((v) => (v||'').toLowerCase().includes(s))); }
    if (sort === 'priceAsc' || sort === 'price-asc') list.sort((a,b)=>a.price-b.price);
    if (sort === 'priceDesc' || sort === 'price-desc') list.sort((a,b)=>b.price-a.price);
    if (sort === 'name') list.sort((a,b)=>a.name.localeCompare(b.name));
    return { content: list, total: list.length, local: true };
  }
  const params = { page, size };
  if (category) params.category = category;
  if (q) params.q = q;
  if (sort) params.sort = sort;
  const { data } = await api.get('/products', { params });
  const content = (data.content || []).map(mapSummary);
  return { content, total: data.totalElements ?? content.length, page: data.number ?? page, local: false };
}

export async function getProduct(slug) {
  if (!apiEnabled) return local.getProductBySlug(slug);
  const { data } = await api.get(`/products/${slug}`);
  const mapped = {
    id: String(data.id),
    slug: data.slug,
    name: data.name,
    price: Number(data.variants?.[0]?.priceAmount ?? data.basePriceAmount ?? 0),
    category: data.category?.slug || '',
    collection: data.category?.slug || '',
    categoryName: data.category?.name || '',
    categoryDescription: data.category?.description || '',
    variant: data.variants?.[0]?.title || '',
    description: data.description || '',
    estimatedDelivery: data.estimatedDelivery || '',
    shippingOptionsJson: data.shippingOptionsJson || '',
    seoTitle: data.seoTitle || '',
    seoDescription: data.seoDescription || '',
    status: data.status || '',
    currency: data.currency || 'USD',
    details: [],
    inStock: data.status === 'ACTIVE' && (data.variants?.length ? data.variants.some((v) => v.active) : true),
    image: data.images?.[0]?.url || local.products[0]?.image,
    images: data.images || [],
    variants: (data.variants || []).map((v) => ({
      id: v.id, title: v.title, price: Number(v.priceAmount), sku: v.sku, active: v.active,
      optionValuesJson: v.optionValuesJson, imageUrl: v.imageUrl || '',
      stockAvailability: v.stockAvailability || '', weightGrams: v.weightGrams ?? null, warehouseInfo: v.warehouseInfo || '',
      currency: v.currency || data.currency || 'USD',
    })),
    _raw: data,
  };
  if (mapped.variants[0]?.price) mapped.price = mapped.variants[0].price;
  return mapped;
}
