const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const apiEnabled = Boolean(baseURL);

let accessToken = null;
let refreshToken = localStorage.getItem('fetch_refresh_token');
let refreshPromise = null;

export function setTokens(tokens) {
  accessToken = tokens?.accessToken || null;
  refreshToken = tokens?.refreshToken || null;
  if (refreshToken) localStorage.setItem('fetch_refresh_token', refreshToken);
  else localStorage.removeItem('fetch_refresh_token');
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('fetch_refresh_token');
}

export function getAccessToken() { return accessToken; }
export function getRefreshToken() { return refreshToken; }

function makeError(response, data) {
  const err = new Error(data?.error?.message || response.statusText || 'Request failed');
  err.response = { status: response.status, data };
  return err;
}

async function doRefresh() {
  const tokenToUse = refreshToken || localStorage.getItem('fetch_refresh_token');
  if (!tokenToUse) throw new Error('No refresh token');
  const res = await fetch(`${baseURL}/auth/refresh`, {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: tokenToUse }),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) throw makeError(res, data);
  setTokens(data);
  return data;
}

export async function refreshSession() {
  if (!refreshPromise) refreshPromise = doRefresh().finally(() => { refreshPromise = null; });
  return refreshPromise;
}

async function request(method, url, body, config = {}, retry = true) {
  const headers = { Accept: 'application/json', ...(config.headers || {}) };
  if (body !== undefined) headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const res = await fetch(`${baseURL}${url}${config.params ? `?${new URLSearchParams(Object.entries(config.params).filter(([,v])=>v!==undefined&&v!==null&&v!=='')).toString()}` : ''}`, {
    method,
    headers,
    body: body === undefined ? undefined : (headers['Content-Type']?.includes('json') ? JSON.stringify(body) : body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (res.ok) return { data, status: res.status };
  if (apiEnabled && res.status === 401 && retry && (refreshToken || localStorage.getItem('fetch_refresh_token')) && !String(url).includes('/auth/')) {
    try {
      await refreshSession();
      return request(method, url, body, config, false);
    } catch (e) {
      clearTokens();
      throw makeError(res, data);
    }
  }
  throw makeError(res, data);
}

export const api = {
  get: (url, config) => request('GET', url, undefined, config),
  post: (url, body, config) => request('POST', url, body, config),
  put: (url, body, config) => request('PUT', url, body, config),
  patch: (url, body, config) => request('PATCH', url, body, config),
  delete: (url, config) => request('DELETE', url, undefined, config),
};
