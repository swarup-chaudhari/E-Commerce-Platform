import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useCart } from '../cart.jsx';
import { money } from '../format.js';
import Alert from '../components/Alert.jsx';

export default function Cart() {
  const cart = useCart();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (cart.lines.length === 0) {
    return (
      <div className="card narrow">
        <h1>Your cart is empty</h1>
        <Link className="btn" to="/">Browse products</Link>
      </div>
    );
  }

  const checkout = async () => {
    setBusy(true);
    setError(null);
    try {
      const order = await api.placeOrder(
        cart.lines.map((l) => ({ productId: l.product.id, quantity: l.quantity, unitPriceCents: l.product.priceCents }))
      );
      cart.clear();
      navigate(`/orders/${order.id}`);
    } catch (err) {
      if (err.status === 409 && err.body?.unavailableProductIds) {
        setError(new Error(`Not enough stock for: ${err.body.unavailableProductIds.join(', ')}`));
      } else {
        setError(err);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h1>Cart</h1>
      <div className="card">
        <table className="table">
          <thead><tr><th>Product</th><th>Price</th><th>Qty</th><th className="right">Subtotal</th></tr></thead>
          <tbody>
            {cart.lines.map(({ product, quantity }) => (
              <tr key={product.id}>
                <td>{product.name}</td>
                <td>{money(product.priceCents)}</td>
                <td>
                  <div className="qty">
                    <button onClick={() => cart.setQuantity(product.id, quantity - 1)}>−</button>
                    <span>{quantity}</span>
                    <button onClick={() => cart.setQuantity(product.id, quantity + 1)}>+</button>
                  </div>
                </td>
                <td className="right">{money(product.priceCents * quantity)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot><tr><td colSpan="3" className="right"><strong>Total</strong></td><td className="right"><strong>{money(cart.totalCents)}</strong></td></tr></tfoot>
        </table>
        <Alert error={error} />
        <div className="row end">
          <button className="btn btn-ghost" onClick={cart.clear}>Clear cart</button>
          <button className="btn" disabled={busy} onClick={checkout}>{busy ? 'Placing order…' : 'Place order'}</button>
        </div>
        <p className="muted small">Orders are processed asynchronously. You'll see live progress on the next screen.</p>
      </div>
    </>
  );
}
