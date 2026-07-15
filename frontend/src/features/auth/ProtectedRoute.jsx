import PropTypes from 'prop-types';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from './useAuthStore';

/** Redirects to /login when unauthenticated (rules.md: rendering + primary interaction test required). */
export default function ProtectedRoute({ children }) {
  const { accessToken, isInitializing } = useAuthStore();

  if (isInitializing) {
    return <div className="flex h-screen items-center justify-center text-text-secondary">Loading…</div>;
  }
  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

ProtectedRoute.propTypes = {
  children: PropTypes.node.isRequired,
};
