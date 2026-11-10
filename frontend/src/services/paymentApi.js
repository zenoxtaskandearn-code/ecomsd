import { api } from './api.js';

export async function getPayPalConfig() { const { data } = await api.get('/payments/paypal/config'); return data; }
export async function createPayPalOrder(orderNumber) { const { data } = await api.post('/payments/paypal/create', { orderNumber }); return data; }
export async function capturePayPalOrder(orderNumber, paypalOrderId) { const { data } = await api.post('/payments/paypal/capture', { orderNumber, paypalOrderId }); return data; }
