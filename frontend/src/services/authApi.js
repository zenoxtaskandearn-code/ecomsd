import { api } from './api.js';

export async function login(payload) { const { data } = await api.post('/auth/login', payload); return data; }
export async function register(payload) { const { data } = await api.post('/auth/register', payload); return data; }
export async function refresh(payload) { const { data } = await api.post('/auth/refresh', payload); return data; }
export async function logout(payload) { await api.post('/auth/logout', payload || {}); }
export async function me() { const { data } = await api.get('/users/me'); return data; }
export async function updateMe(payload) { const { data } = await api.patch('/users/me', payload); return data; }
export async function verifyEmail(payload) { const { data } = await api.post('/auth/verify-email', payload); return data; }
export async function resendVerification(payload) { const { data } = await api.post('/auth/resend-verification', payload); return data; }
export async function forgotPassword(payload) { const { data } = await api.post('/auth/forgot-password', payload); return data; }
export async function resetPassword(payload) { const { data } = await api.post('/auth/reset-password', payload); return data; }
