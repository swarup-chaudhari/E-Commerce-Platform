import React, { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAsync } from '../hooks.js';
import { money, shortId, when } from '../format.js';
import StatusBadge from '../components/StatusBadge.jsx';
import SagaTimeline from '../components/SagaTimeline.jsx';
import Alert from '../components/Alert.jsx';

async function loadOrder(id) {
  const [order, events, payment] = await Promise.all([api.order(id), api.orderEvents(id), api.payment(id)]);
  return { order, events, payment };
}

export default function OrderDetail() {
  const { id } = useParams();
  const { data, error, loading, reload } = useAsync(() => loadOrder(id), [id], {
    pollMs: 1500,
    shouldPoll: (d) => d.order.status === 'PENDING',
  });
  const [cancelError, setCancelError] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  if (loading) return <p className="muted">Loading order…</p>;
  if (error && !data) return <><Alert error={error} /><Link to="/orders">← Back to orders</Link></>;

  const { order, events, payment } = data;
  const cancellable = order.status === 'PENDING' || order.status === 'CONFIRMED';

  const cancel = async () => {
    if (!window.confirm('Cancel this order? Any payment will be refunded.')) return;
    setCancelling(true);
    setCancelError(null);
    try {
      await api.cancelOrder(id);
      await reload();
    } catch (err) {
      setCancelError(err);
    } finally {
      setCancelling(false);
    }
  };

  return (
    <>
      <p><Link to="/orders">← My orders</Link></p>
      <div className="row">
        <h1>Order #{shortId(order.id)}</h1>
        <StatusBadge status={order.status} />
      </div>
      {order.status === 'PENDING' && <Alert kind="info">Processing… this page updates automatically.</Alert>}
      {order.failureReason && <Alert kind={order.status === 'CANCELLED' ? 'info' : 'error'}>{order.failureReason}</Alert>}

      <div className="two-col">
        <div className="card">
          <h3>Items</h3>
          <table className="table">
            <tbody>
              {order.items.map((i) => (
                <tr key={i.productId}>
                  <td>{i.productId}</td>
                  <td>× {i.quantity}</td>
                  <td className="right">{money(i.unitPriceCents * i.quantity)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot><tr><td colSpan="2" className="right"><strong>Total</strong></td><td className="right"><strong>{money(order.totalAmountCents)}</strong></td></tr></tfoot>
          </table>
          <p className="muted small">Placed {when(order.createdAt)}</p>
          {payment && <p className="small">Payment: <StatusBadge status={payment.status} /></p>}
          <Alert error={cancelError} />
          {cancellable && <button className="btn btn-danger" disabled={cancelling} onClick={cancel}>{cancelling ? 'Cancelling…' : 'Cancel order'}</button>}
        </div>

        <div className="card">
          <h3>Progress</h3>
          <SagaTimeline events={events} />
          <details>
            <summary className="muted small">Event log ({events.length})</summary>
            <ul className="events">
              {events.map((e) => (
                <li key={e.sequence}><code>{e.eventType.replace('Event', '')}</code> <span className="muted small">{when(e.occurredAt)}</span></li>
              ))}
            </ul>
          </details>
        </div>
      </div>
    </>
  );
}
