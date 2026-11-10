import { api } from './api.js';

export async function preview(payload) { const { data } = await api.post('/orders/preview', payload); return data; }
export async function create(payload) { const { data } = await api.post('/orders', payload); return data; }
export async function listMine() { const { data } = await api.get('/orders'); return data; }
export async function getMine(orderNumber) { const { data } = await api.get(`/orders/${orderNumber}`); return data; }
