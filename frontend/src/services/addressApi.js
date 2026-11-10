import { api } from './api.js';

export async function list() { const { data } = await api.get('/users/me/addresses'); return data; }
export async function create(payload) { const { data } = await api.post('/users/me/addresses', payload); return data; }
export async function update(id, payload) { const { data } = await api.put(`/users/me/addresses/${id}`, payload); return data; }
export async function remove(id) { await api.delete(`/users/me/addresses/${id}`); }
