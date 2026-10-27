const TOKEN_KEY = 'shopstream.token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY));

export class ApiError extends Error {
  constructor(status, message, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

export async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });

  if (res.status === 401 && auth) {
    setToken(null);
    window.dispatchEvent(new Event('auth:expired'));
    throw new ApiError(401, 'Session expired - please sign in again');
  }
  const text = await res.text();
  const data = text ? safeJson(text) : null;
  if (!res.ok) {
    const msg = (data && (data.detail || data.error || data.message)) || `Request failed (${res.status})`;
    throw new ApiError(res.status, msg, data);
  }
  return data;
}

function safeJson(text) {
  try { return JSON.parse(text); } catch { return null; }
}

export const api = {
  login: (username, password) => request('/auth/login', { method: 'POST', body: { username, password }, auth: false }),
  products: () => request('/api/products'),
  createProduct: (p) => request('/api/products', { method: 'POST', body: p }),
  deleteProduct: (id) => request(`/api/products/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  stock: (id) => request(`/api/inventory/${encodeURIComponent(id)}`).catch((e) => (e.status === 404 ? { availableQuantity: 0 } : Promise.reject(e))),
  setStock: (id, quantity) => request(`/api/inventory/${encodeURIComponent(id)}/stock`, { method: 'PUT', body: { quantity } }),
  placeOrder: (items) => request('/api/orders', { method: 'POST', body: { items } }),
  orders: () => request('/api/orders'),
  order: (id) => request(`/api/orders/${id}`),
  orderEvents: (id) => request(`/api/orders/${id}/events`),
  cancelOrder: (id) => request(`/api/orders/${id}/cancel`, { method: 'POST' }),
  payment: (orderId) => request(`/api/payments/order/${orderId}`).catch((e) => (e.status === 404 ? null : Promise.reject(e))),
};
