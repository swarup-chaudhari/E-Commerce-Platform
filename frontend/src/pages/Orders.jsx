import React from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAsync } from '../hooks.js';
import { money, shortId, when } from '../format.js';
import StatusBadge from '../components/StatusBadge.jsx';
import Alert from '../components/Alert.jsx';

export default function Orders() {
  const { data, error, loading } = useAsync(() => api.orders(), [], {
    pollMs: 3000,
    shouldPoll: (orders) => orders.some((o) => o.status === 'PENDING'),
  });

  if (loading) return <p className="muted">Loading orders…</p>;
  return (
    <>
      <h1>My orders</h1>
      <Alert error={error} />
      {data?.length === 0 && <p className="muted">No orders yet.</p>}
      {data?.length > 0 && (
        <div className="card">
          <table className="table">
            <thead><tr><th>Order</th><th>Placed</th><th>Items</th><th>Total</th><th>Status</th></tr></thead>
            <tbody>
              {data.map((o) => (
                <tr key={o.id}>
                  <td><Link to={`/orders/${o.id}`}>#{shortId(o.id)}</Link></td>
                  <td>{when(o.createdAt)}</td>
                  <td>{o.items.reduce((n, i) => n + i.quantity, 0)}</td>
                  <td>{money(o.totalAmountCents)}</td>
                  <td><StatusBadge status={o.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
