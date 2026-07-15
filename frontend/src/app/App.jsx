import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useUiStore } from '../features/ui/useUiStore';
import { useAuthStore } from '../features/auth/useAuthStore';
import { api } from '../lib/api';
import ProtectedRoute from '../features/auth/ProtectedRoute';
import AppLayout from '../components/layout/AppLayout';
import LoginPage from '../features/auth/LoginPage';
import RegisterPage from '../features/auth/RegisterPage';
import OverviewPage from '../features/overview/OverviewPage';
import ConnectionsPage from '../features/connections/ConnectionsPage';
import SqlEditorPage from '../features/query/SqlEditorPage';
import AskPage from '../features/chat/AskPage';
import ComingSoonPage from '../features/placeholder/ComingSoonPage';

export default function App() {
  const theme = useUiStore((s) => s.theme);
  const setInitializing = useAuthStore((s) => s.setInitializing);

  // Silent-refresh-on-load: try to exchange the HttpOnly refresh cookie for
  // a fresh access token before rendering protected routes (phases.md Phase1).
  useEffect(() => {
    api.tryRefresh().finally(() => setInitializing(false));
  }, [setInitializing]);

  return (
    <div className={theme}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/ask" element={<AskPage />} />
          <Route path="/connections" element={<ConnectionsPage />} />
          <Route path="/editor" element={<SqlEditorPage />} />
          <Route path="/dashboards" element={<ComingSoonPage title="Dashboards" />} />
          <Route path="/reports" element={<ComingSoonPage title="Reports" />} />
          <Route path="/prompts" element={<ComingSoonPage title="Saved Prompts" />} />
          <Route path="/settings" element={<ComingSoonPage title="Settings" />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
