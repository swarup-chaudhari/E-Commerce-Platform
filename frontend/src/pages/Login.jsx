import React, { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import Alert from '../components/Alert.jsx';

const DEMO = [
  ['alice', 'alice123', 'customer'],
  ['bob', 'bob123', 'customer'],
  ['admin', 'admin123', 'admin'],
];

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('alice');
  const [password, setPassword] = useState('alice123');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username, password);
      navigate(location.state?.from?.pathname || '/', { replace: true });
    } catch (err) {
      setError(err.status === 401 ? new Error('Invalid username or password') : err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card narrow">
      <h1>Sign in</h1>
      <form onSubmit={submit} className="form">
        <label>Username<input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus /></label>
        <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} /></label>
        <Alert error={error} />
        <button className="btn" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="muted small">Demo accounts (click to fill):</p>
      <div className="chips">
        {DEMO.map(([u, p, role]) => (
          <button key={u} type="button" className="chip" onClick={() => { setUsername(u); setPassword(p); }}>
            {u} · {role}
          </button>
        ))}
      </div>
    </div>
  );
}
