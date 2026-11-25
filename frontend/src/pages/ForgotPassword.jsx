import { useState } from 'react';
import { Link } from 'react-router-dom';
import api, { ensureCsrf, errorMessage, fieldErrors } from '../api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setFieldError(''); setMessage('');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setFieldError('Enter a valid email address'); return; }
    setBusy(true);
    try {
      await ensureCsrf();
      const r = await api.post('/auth/forgot-password', { email });
      setMessage(r.data.message);
    } catch (err) {
      setFieldError(fieldErrors(err).email || '');
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-page">
      <form className="card" onSubmit={submit} noValidate>
        <h1>Forgot your password?</h1>
        <p className="muted">Enter your account email. If it matches an account, we send a link that lets you choose a new password. The link works once and expires in 30 minutes.</p>
        {message && <p className="ok" role="status">{message}</p>}
        {error && <p className="error" role="alert">{error}</p>}
        <label>Email
          <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} aria-invalid={!!fieldError} />
          {fieldError && <span className="field-error">{fieldError}</span>}
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Sending…' : 'Send reset link'}</button>
        <p className="muted"><Link to="/login">Back to sign in</Link></p>
      </form>
    </div>
  );
}
