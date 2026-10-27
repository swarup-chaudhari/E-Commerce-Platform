import React from 'react';
import { api } from '../api.js';
import { useAsync } from '../hooks.js';
import { useCart } from '../cart.jsx';
import { money } from '../format.js';
import Alert from '../components/Alert.jsx';

async function loadCatalog() {
  const products = await api.products();
  const stocks = await Promise.all(products.map((p) => api.stock(p.id)));
  return products.map((p, i) => ({ ...p, available: stocks[i].availableQuantity }));
}

export default function Catalog() {
  const { data, error, loading } = useAsync(loadCatalog, []);
  const cart = useCart();

  if (loading) return <p className="muted">Loading catalog…</p>;
  return (
    <>
      <h1>Shop</h1>
      <Alert error={error} />
      {data?.length === 0 && <p className="muted">No products yet. An admin can add some under “Admin”.</p>}
      <div className="grid">
        {data?.map((p) => {
          const inCart = cart.lines.find((l) => l.product.id === p.id)?.quantity || 0;
          const soldOut = p.available <= 0;
          return (
            <div className="card product" key={p.id}>
              <div className="tag">{p.category || 'general'}</div>
              <h3>{p.name}</h3>
              <p className="muted small">{p.description || Object.entries(p.attributes || {}).map(([k, v]) => `${k}: ${v}`).join(' · ')}</p>
              <div className="row">
                <strong>{money(p.priceCents)}</strong>
                <span className={soldOut ? 'stock out' : p.available < 10 ? 'stock low' : 'stock'}>
                  {soldOut ? 'Out of stock' : `${p.available} in stock`}
                </span>
              </div>
              <button className="btn" disabled={soldOut || inCart >= p.available} onClick={() => cart.add(p)}>
                {inCart ? `Add another (${inCart} in cart)` : 'Add to cart'}
              </button>
            </div>
          );
        })}
      </div>
    </>
  );
}
