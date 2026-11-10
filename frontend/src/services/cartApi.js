import { api, apiEnabled } from './api.js';

export async function getCart() { const { data } = await api.get('/cart'); return data; }
export async function addItem(variantId, quantity = 1) { const { data } = await api.post('/cart/items', { variantId, quantity }); return data; }
export async function updateItem(itemId, quantity) { const { data } = await api.patch(`/cart/items/${itemId}`, { quantity }); return data; }
export async function removeItem(itemId) { const { data } = await api.delete(`/cart/items/${itemId}`); return data; }
export async function clearCart() { await api.delete('/cart'); }
export { apiEnabled };
