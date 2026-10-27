import React, { useState } from 'react';
import { api } from '../api.js';
import { useAsync } from '../hooks.js';
import { money } from '../format.js';
import Alert from '../components/Alert.jsx';

async function loadAll() {
  const products = await api.products();
  const stocks = await Promise.all(products.map((p) => api.stock(p.id)));
  return products.map((p, i) => ({ ...p, available: stocks[i].availableQuantity }));
}

const EMPTY = { id: '', name: '', description: '', category: '', price: '', quantity: '' };

export default function Admin() {
  const { data, error, loading, reload } = useAsync(loadAll, []);
  const [form, setForm] = useState(EMPTY);
  const [formError, setFormError] = useState(null);
  const [edits, setEdits] = useState({});
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const addProduct = async (e) => {
    e.preventDefault();
    setFormError(null);
    try {
      await api.createProduct({
        id: form.id.trim(),
        name: form.name.trim(),
        description: form.description,
        category: form.category,
        priceCents: Math.round(parseFloat(form.price) * 100),
        attributes: {},
      });
      await api.setStock(form.id.trim(), parseInt(form.quantity || '0', 10));
      setForm(EMPTY);
      reload();
    } catch (err) {
      setFormError(err.status === 409 ? new Error('A product with that id already exists') : err);
    }
  };

  const saveStock = async (id) => {
    try {
      await api.setStock(id, parseInt(edits[id], 10));
      setEdits(({ [id]: _, ...rest }) => rest);
      reload();
    } catch (err) {
      setFormError(err);
    }
  };

  const remove = async (id) => {
    if (!window.confirm(`Delete product ${id}?`)) return;
    try { await api.deleteProduct(id); reload(); } catch (err) { setFormError(err); }
  };

  return (
    <>
      <h1>Admin · catalog & stock</h1>
      <Alert error={error} />
      <Alert error={formError} />

      <div className="card">
        <h3>Add product</h3>
        <form className="form inline" onSubmit={addProduct}>
          <input placeholder="Id (e.g. P-2001)" value={form.id} onChange={set('id')} required />
          <input placeholder="Name" value={form.name} onChange={set('name')} required />
          <input placeholder="Category" value={form.category} onChange={set('category')} />
          <input placeholder="Description" value={form.description} onChange={set('description')} />
          <input placeholder="Price (USD)" type="number" min="0" step="0.01" value={form.price} onChange={set('price')} required />
          <input placeholder="Initial stock" type="number" min="0" value={form.quantity} onChange={set('quantity')} required />
          <button className="btn">Add</button>
        </form>
      </div>

      <div className="card">
        {loading ? <p className="muted">Loading…</p> : (
          <table className="table">
            <thead><tr><th>Id</th><th>Name</th><th>Price</th><th>Stock</th><th /></tr></thead>
            <tbody>
              {data?.map((p) => (
                <tr key={p.id}>
                  <td><code>{p.id}</code></td>
                  <td>{p.name}</td>
                  <td>{money(p.priceCents)}</td>
                  <td>
                    <input
                      className="stock-input" type="number" min="0"
                      value={edits[p.id] ?? p.available}
                      onChange={(e) => setEdits({ ...edits, [p.id]: e.target.value })}
                    />
                    {edits[p.id] !== undefined && <button className="btn btn-small" onClick={() => saveStock(p.id)}>Save</button>}
                  </td>
                  <td className="right"><button className="btn btn-ghost btn-small" onClick={() => remove(p.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
