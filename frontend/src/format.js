export const money = (cents) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format((cents || 0) / 100);

export const shortId = (id) => (id ? id.slice(0, 8) : '');

export const when = (iso) => (iso ? new Date(iso).toLocaleString() : '');
