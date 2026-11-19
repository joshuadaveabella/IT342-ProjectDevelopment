import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { dashboardPath, useAuth } from '../AuthContext';
import { errorMessage, fieldErrors } from '../api';

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirm: '', role: 'MEMBER' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={dashboardPath(user.role)} replace />;

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const validate = () => {
    const v = {};
    if (!form.fullName.trim()) v.fullName = 'Full name is required';
    if (!form.email.trim()) v.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) v.email = 'Enter a valid email address';
    if (form.password.length < 8) v.password = 'Password must be at least 8 characters';
    else if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) v.password = 'Password must contain a letter and a number';
    if (form.confirm !== form.password) v.confirm = 'Passwords do not match';
    return v;
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await register({ fullName: form.fullName, email: form.email, password: form.password, role: form.role });
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  const field = (k, label, type = 'text', auto) => (
    <label>{label}
      <input type={type} value={form[k]} onChange={set(k)} autoComplete={auto} aria-invalid={!!errors[k]} />
      {errors[k] && <span className="field-error">{errors[k]}</span>}
    </label>
  );

  return (
    <div className="auth-page">
      <form className="card" onSubmit={submit} noValidate>
        <h1>Create your account</h1>
        {error && <p className="error" role="alert">{error}</p>}
        {field('fullName', 'Full name', 'text', 'name')}
        {field('email', 'Email', 'email', 'email')}
        {field('password', 'Password', 'password', 'new-password')}
        {field('confirm', 'Confirm password', 'password', 'new-password')}
        <label>I will use Dila-Dila as
          <select value={form.role} onChange={set('role')}>
            <option value="MEMBER">Team Member</option>
            <option value="LEAD">Project Lead</option>
          </select>
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        <p className="muted">Already registered? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}
