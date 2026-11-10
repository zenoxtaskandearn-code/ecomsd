import { api, apiEnabled } from './api.js';

export async function submit(payload) {
  if (!apiEnabled) { await new Promise((r)=>setTimeout(r,700)); return { status: 'received', demo: true }; }
  const { data } = await api.post('/contact', payload);
  return data;
}
