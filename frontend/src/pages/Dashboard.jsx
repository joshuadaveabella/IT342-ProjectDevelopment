import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api';

function useGet(url) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    api.get(url).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [url]);
  return { data, error };
}

function Caps({ list }) {
  return (<><h3>What you can do</h3><ul>{list.map((c) => <li key={c}>{c}</li>)}</ul></>);
}

export function MemberDashboard() {
  const { data, error } = useGet('/member/dashboard');
  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Loading…</p>;
  return (
    <section>
      <h1>{data.title}</h1><p>{data.welcome}</p>
      <div className="card"><Caps list={data.capabilities} /></div>
    </section>
  );
}

export function LeadDashboard() {
  const { data, error } = useGet('/lead/dashboard');
  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Loading…</p>;
  return (
    <section>
      <h1>{data.title}</h1><p>{data.welcome}</p>
      <div className="stats">
        <div className="card stat"><b>{data.totalAccounts}</b><span>Accounts</span></div>
        <div className="card stat"><b>{data.leads}</b><span>Project Leads</span></div>
        <div className="card stat"><b>{data.members}</b><span>Team Members</span></div>
      </div>
      <div className="card"><Caps list={data.capabilities} /></div>
    </section>
  );
}

export function TeamAccounts() {
  const { data, error } = useGet('/lead/users');
  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Loading…</p>;
  return (
    <section>
      <h1>Team Accounts</h1>
      <div className="card"><table>
        <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th></tr></thead>
        <tbody>{data.map((u) => (
          <tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td>
            <td><span className={`badge ${u.role}`}>{u.role}</span></td>
            <td>{new Date(u.createdAt).toLocaleDateString()}</td></tr>
        ))}</tbody>
      </table></div>
    </section>
  );
}

export function Forbidden() {
  return (<div className="auth-page"><div className="card"><h1>403 – Access denied</h1>
    <p>Your role does not allow you to open this page.</p></div></div>);
}
