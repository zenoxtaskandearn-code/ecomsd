import { Navigate, useLocation } from 'react-router-dom';
import { useAdminAuth } from '../../context/AdminAuthContext.jsx';

export default function AdminProtectedRoute({ children }) {
  const { isAuthenticated, hydrating } = useAdminAuth();
  const location = useLocation();
  if (hydrating) return <div className="min-h-screen bg-[#f1f1f1]" />;
  if (!isAuthenticated) return <Navigate to="/admin/login" replace state={{ from: location }} />;
  return children;
}
