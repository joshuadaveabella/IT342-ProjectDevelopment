import { Navigate, useLocation } from 'react-router-dom';
import { dashboardPath, useAuth } from '../AuthContext';

/** Frontend RBAC guard. The backend enforces the same rules; this only improves the UX. */
export default function ProtectedRoute({ roles, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <p className="center muted">Loading…</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/forbidden" replace />;
  return children;
}

export function HomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <p className="center muted">Loading…</p>;
  return <Navigate to={user ? dashboardPath(user.role) : '/login'} replace />;
}
