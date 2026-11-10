import { api } from './api.js';

export async function listCategories() { const { data } = await api.get('/admin/categories'); return data; }
export async function saveCategory(payload, id) { const { data } = id ? await api.put(`/admin/categories/${id}`, payload) : await api.post('/admin/categories', payload); return data; }
export async function listProducts() { const { data } = await api.get('/admin/products'); return data; }
export async function saveProduct(payload, id) { const { data } = id ? await api.put(`/admin/products/${id}`, payload) : await api.post('/admin/products', payload); return data; }
export async function listOrders() { const { data } = await api.get('/admin/orders'); return data; }
export async function getOrder(orderNumber) { const { data } = await api.get(`/admin/orders/${orderNumber}`); return data; }
export async function updateOrderStatus(orderNumber, status) { const { data } = await api.patch(`/admin/orders/${orderNumber}/status`, { status }); return data; }
export async function addShipment(orderNumber, payload) { const { data } = await api.post(`/admin/orders/${orderNumber}/shipments`, payload); return data; }
export async function submitOrderToCj(orderNumber, options) { const { data } = await api.post(`/admin/orders/${orderNumber}/cj/submit`, options || {}); return data; }
export async function syncOrderFromCj(orderNumber) { const { data } = await api.post(`/admin/orders/${orderNumber}/cj/sync`); return data; }
export async function listSuppliers() { const { data } = await api.get('/admin/suppliers'); return data; }
export async function saveSupplier(payload, id) { const { data } = id ? await api.put(`/admin/suppliers/${id}`, payload) : await api.post('/admin/suppliers', payload); return data; }
export async function listCustomers() { const { data } = await api.get('/admin/customers'); return data; }
export async function getStats() { const { data } = await api.get('/admin/stats'); return data; }
export async function getCjImportStatus() { const { data } = await api.get('/admin/import/cj/status'); return data; }
export async function importCjManual(payload) { const { data } = await api.post('/admin/import/cj/manual', payload); return data; }
export async function importCjMyProducts(payload) { const { data } = await api.post('/admin/import/cj/my-products', payload); return data; }
export async function previewCjMyProducts(payload) { const { data } = await api.post('/admin/import/cj/my-products/preview', payload || {}); return data; }
export async function importCjSelected(payload) { const { data } = await api.post('/admin/import/cj/my-products/import-selected', payload); return data; }
export async function refreshCjProduct(productId) { const { data } = await api.post(`/admin/import/cj/products/${productId}/refresh`); return data; }
export async function syncCjProductStock(productId) { const { data } = await api.post(`/admin/import/cj/products/${productId}/sync-stock`); return data; }
export async function saveVariant(productId, payload, variantId) {
  const { data } = variantId
    ? await api.put(`/admin/variants/${variantId}`, payload)
    : await api.post(`/admin/products/${productId}/variants`, payload);
  return data;
}
export async function saveImage(productId, payload) { const { data } = await api.post(`/admin/products/${productId}/images`, payload); return data; }
export async function deleteImage(imageId) { await api.delete(`/admin/images/${imageId}`); }
