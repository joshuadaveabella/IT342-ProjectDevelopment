import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api, { ensureCsrf, errorMessage, fieldErrors } from '../api';

export const passwordProblem = (pw) => {
  if (pw.length < 8) return 'Password must be at least 8 characters';
  if (!/[A-Za-z]/.test(pw) || !/\d/.test(pw)) return 'Password must contain a letter and a number';
  return '';
};

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const navigate = useNavigate();
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (!token) {
    return (
      <div className="auth-page"><div className="card">
        <h1>Reset link problem</h1>
        <p className="error">This reset link is incomplete. Please request a new one.</p>
        <Link to="/forgot-password">Request a new link</Link>
      </div></div>
    );
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const v = {};
    const problem = passwordProblem(form.newPassword);
    if (problem) v.newPassword = problem;
    if (form.confirmPassword !== form.newPassword) v.confirmPassword = 'Passwords do not match';
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await ensureCsrf();
      await api.post('/auth/reset-password', { token, ...form });
      navigate('/login', { replace: true, state: { reset: true } });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-page">
      <form className="card" onSubmit={submit} noValidate>
        <h1>Choose a new password</h1>
        {error && <p className="error" role="alert">{error} {error.includes('invalid or has expired') && <Link to="/forgot-password">Request a new link</Link>}</p>}
        <label>New password
          <input type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} aria-invalid={!!errors.newPassword} />
          {errors.newPassword && <span className="field-error">{errors.newPassword}</span>}
        </label>
        <label>Confirm new password
          <input type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} aria-invalid={!!errors.confirmPassword} />
          {errors.confirmPassword && <span className="field-error">{errors.confirmPassword}</span>}
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Reset password'}</button>
      </form>
    </div>
  );
}
