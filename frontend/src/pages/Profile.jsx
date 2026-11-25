import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api';
import { useAuth } from '../AuthContext';
import { passwordProblem } from './ResetPassword';

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const [form, setForm] = useState({
    fullName: user.fullName || '', phone: user.phone || '', bio: user.bio || '', avatarUrl: user.avatarUrl || '',
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => { setSaved(false); setForm({ ...form, [k]: e.target.value }); };

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSaved(false);
    const v = {};
    if (!form.fullName.trim()) v.fullName = 'Full name is required';
    if (form.phone && !/^\+?[0-9 ()-]{7,20}$/.test(form.phone)) v.phone = 'Enter a valid phone number';
    if (form.bio.length > 255) v.bio = 'Bio must be at most 255 characters';
    if (form.avatarUrl && !/^https:\/\/\S+$/.test(form.avatarUrl)) v.avatarUrl = 'Photo link must start with https://';
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const r = await api.put('/profile', form);
      updateUser(r.data);
      setSaved(true);
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  const field = (k, label, props = {}) => (
    <label>{label}
      <input value={form[k]} onChange={set(k)} aria-invalid={!!errors[k]} {...props} />
      {errors[k] && <span className="field-error">{errors[k]}</span>}
    </label>
  );

  return (
    <form className="card form" onSubmit={submit} noValidate>
      <h2>Profile</h2>
      <div className="profile-head">
        {user.avatarUrl
          ? <img className="avatar" src={user.avatarUrl} alt="Your profile" referrerPolicy="no-referrer" />
          : <div className="avatar placeholder" aria-hidden="true">{user.fullName?.[0]?.toUpperCase()}</div>}
        <div><strong>{user.email}</strong><br /><span className={`badge ${user.role}`}>{user.role}</span>
          <div className="muted small">Email and role cannot be changed here.</div></div>
      </div>
      {saved && <p className="ok" role="status">Profile saved.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      {field('fullName', 'Full name', { autoComplete: 'name' })}
      {field('phone', 'Phone (optional)', { autoComplete: 'tel', placeholder: '+63 912 345 6789' })}
      <label>Bio (optional)
        <textarea rows={3} value={form.bio} onChange={set('bio')} aria-invalid={!!errors.bio} />
        {errors.bio && <span className="field-error">{errors.bio}</span>}
      </label>
      {field('avatarUrl', 'Photo link (optional, https)', { placeholder: 'https://…' })}
      <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button>
    </form>
  );
}

function ChangePasswordForm() {
  const blank = { currentPassword: '', newPassword: '', confirmPassword: '' };
  const [form, setForm] = useState(blank);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => { setDone(false); setForm({ ...form, [k]: e.target.value }); };

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setDone(false);
    const v = {};
    if (!form.currentPassword) v.currentPassword = 'Current password is required';
    const problem = passwordProblem(form.newPassword);
    if (problem) v.newPassword = problem;
    else if (form.newPassword === form.currentPassword) v.newPassword = 'New password must be different from the current password';
    if (form.confirmPassword !== form.newPassword) v.confirmPassword = 'Passwords do not match';
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await api.post('/account/change-password', form);
      setForm(blank);
      setDone(true);
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  };

  const field = (k, label, auto) => (
    <label>{label}
      <input type="password" autoComplete={auto} value={form[k]} onChange={set(k)} aria-invalid={!!errors[k]} />
      {errors[k] && <span className="field-error">{errors[k]}</span>}
    </label>
  );

  return (
    <form className="card form" onSubmit={submit} noValidate>
      <h2>Change password</h2>
      {done && <p className="ok" role="status">Password changed. Use the new password next time you sign in.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      {field('currentPassword', 'Current password', 'current-password')}
      {field('newPassword', 'New password', 'new-password')}
      {field('confirmPassword', 'Confirm new password', 'new-password')}
      <button className="btn" disabled={busy}>{busy ? 'Saving…' : 'Change password'}</button>
    </form>
  );
}

export default function Profile() {
  return (
    <section>
      <h1>My Profile</h1>
      <div className="two-col">
        <ProfileForm />
        <ChangePasswordForm />
      </div>
    </section>
  );
}
