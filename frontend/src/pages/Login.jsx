import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { dashboardPath, useAuth } from '../AuthContext';
import { errorMessage } from '../api';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={dashboardPath(user.role)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!email.trim() || !password) { setError('Email and password are required.'); return; }
    setBusy(true);
    try {
      const u = await login(email, password);
      const from = location.state?.from;
      const allowed = from && (u.role === 'LEAD' || !from.startsWith('/lead'));
      navigate(allowed ? from : dashboardPath(u.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-page">
      <form className="card" onSubmit={submit} noValidate>
        <h1>Sign in to Dila-Dila</h1>
        {location.state?.registered && <p className="ok" role="status">Account created. You can sign in now.</p>}
        {error && <p className="error" role="alert">{error}</p>}
        <label>Email
          <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>Password
          <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <p className="muted">No account? <Link to="/register">Create one</Link></p>
      </form>
    </div>
  );
}
