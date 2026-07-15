import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useUiStore } from '../features/ui/useUiStore';
import { useAuthStore } from '../features/auth/useAuthStore';
import { useWorkspaceStore } from '../features/workspace/useWorkspaceStore';
import { listWorkspaces } from '../features/workspace/workspaceApi';
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
  const accessToken = useAuthStore((s) => s.accessToken);
  const setCurrentWorkspaceId = useWorkspaceStore((s) => s.setCurrentWorkspaceId);

  // Silent-refresh-on-load: try to exchange the HttpOnly refresh cookie for
  // a fresh access token before rendering protected routes (phases.md Phase1).
  useEffect(() => {
    api.tryRefresh().finally(() => setInitializing(false));
  }, [setInitializing]);

  // Workspace hydration: whenever the user becomes authenticated (register,
  // login, or silent refresh on page reload), fetch their workspace list and
  // populate the store. All three auth paths set accessToken in useAuthStore,
  // so watching it here covers every entry point in one place.
  useEffect(() => {
    if (!accessToken) return;
    listWorkspaces()
      .then((workspaces) => {
        if (workspaces && workspaces.length > 0) {
          setCurrentWorkspaceId(workspaces[0].id);
        }
      })
      .catch(() => {
        // Workspace fetch failed — currentWorkspaceId stays null.
        // Pages that need it show their empty/disabled state gracefully.
      });
  }, [accessToken, setCurrentWorkspaceId]);

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
