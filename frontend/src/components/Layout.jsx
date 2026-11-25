import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';

// Role-based navigation: links appear only for roles allowed to use them.
const NAV = [
  { to: '/member', label: 'My Dashboard', roles: ['MEMBER', 'LEAD'] },
  { to: '/lead', label: 'Lead Dashboard', roles: ['LEAD'] },
  { to: '/lead/team', label: 'Team Accounts', roles: ['LEAD'] },
  { to: '/profile', label: 'My Profile', roles: ['MEMBER', 'LEAD'] },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const onLogout = async () => { await logout(); navigate('/login', { replace: true }); };

  return (
    <div className="shell">
      <header className="topbar">
        <strong className="brand">Dila-Dila</strong>
        <nav aria-label="Main">
          {NAV.filter((n) => n.roles.includes(user.role)).map((n) => (
            <NavLink key={n.to} to={n.to} end>{n.label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <span>{user.fullName} <span className={`badge ${user.role}`}>{user.role}</span></span>
          <button className="btn ghost" onClick={onLogout}>Sign out</button>
        </div>
      </header>
      <main className="content"><Outlet /></main>
    </div>
  );
}
