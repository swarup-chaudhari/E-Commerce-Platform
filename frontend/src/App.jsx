import React from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import { useCart } from './cart.jsx';
import Login from './pages/Login.jsx';
import Catalog from './pages/Catalog.jsx';
import Cart from './pages/Cart.jsx';
import Orders from './pages/Orders.jsx';
import OrderDetail from './pages/OrderDetail.jsx';
import Admin from './pages/Admin.jsx';

function RequireAuth({ children, admin = false }) {
  const { user, isAdmin } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (admin && !isAdmin) return <Navigate to="/" replace />;
  return children;
}

function Header() {
  const { user, isAdmin, logout } = useAuth();
  const { count } = useCart();
  return (
    <header className="header">
      <Link to="/" className="brand">ShopStream</Link>
      {user && (
        <nav>
          <NavLink to="/" end>Shop</NavLink>
          <NavLink to="/cart">Cart{count > 0 && <span className="pill">{count}</span>}</NavLink>
          <NavLink to="/orders">My orders</NavLink>
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
        </nav>
      )}
      <div className="spacer" />
      {user && (
        <div className="user">
          <span>{user.username}{isAdmin ? ' (admin)' : ''}</span>
          <button className="btn btn-ghost" onClick={logout}>Sign out</button>
        </div>
      )}
    </header>
  );
}

export default function App() {
  return (
    <>
      <Header />
      <main className="container">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<RequireAuth><Catalog /></RequireAuth>} />
          <Route path="/cart" element={<RequireAuth><Cart /></RequireAuth>} />
          <Route path="/orders" element={<RequireAuth><Orders /></RequireAuth>} />
          <Route path="/orders/:id" element={<RequireAuth><OrderDetail /></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth admin><Admin /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
