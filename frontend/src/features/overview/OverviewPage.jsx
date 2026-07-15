import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Database, MessageSquare, ShieldCheck } from 'lucide-react';
import { listConnections } from '../connections/connectionsApi';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';
import { useAuthStore } from '../auth/useAuthStore';
import LoadingState from '../../components/states/LoadingState';

export default function OverviewPage() {
  const user = useAuthStore((s) => s.user);
  // Single source of truth: App.jsx hydrates currentWorkspaceId on every
  // auth path (register/login/silent-refresh). This page just reads it,
  // rather than independently re-fetching workspaces itself (that used to
  // duplicate the same state in two places).
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);

  const { data: connections, isLoading } = useQuery({
    queryKey: ['connections', workspaceId],
    queryFn: () => listConnections(workspaceId),
    enabled: !!workspaceId,
  });

  if (!workspaceId) {
    return <LoadingState label="Setting up your workspace…" />;
  }

  const hasConnections = connections && connections.length > 0;

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="mb-2 text-lg font-semibold text-text-primary">
        Welcome back, {user?.displayName}
      </h1>
      <p className="mb-6 flex items-start gap-2 text-sm text-text-secondary">
        <ShieldCheck size={16} className="mt-0.5 shrink-0 text-success" />
        QueryMind connects to databases you already have — it never stores
        or copies your business data. QueryMind only keeps your workspace,
        encrypted connection metadata, and chat history.
      </p>

      {isLoading ? (
        <LoadingState label="Checking your data sources…" />
      ) : hasConnections ? (
        <Link
          to="/ask"
          className="flex items-center gap-3 rounded border border-border bg-bg-surface p-4 hover:bg-bg-surface-raised"
        >
          <MessageSquare size={20} className="text-accent" />
          <div>
            <p className="text-sm font-medium text-text-primary">Ask a question</p>
            <p className="text-xs text-text-secondary">
              {connections.length} data source{connections.length > 1 ? 's' : ''} connected — start chatting with your data.
            </p>
          </div>
        </Link>
      ) : (
        <Link
          to="/connections"
          className="flex items-center gap-3 rounded border border-border bg-bg-surface p-4 hover:bg-bg-surface-raised"
        >
          <Database size={20} className="text-accent" />
          <div>
            <p className="text-sm font-medium text-text-primary">Connect your first database</p>
            <p className="text-xs text-text-secondary">
              No data source connected yet — add a read-only credential to get started.
            </p>
          </div>
        </Link>
      )}
    </div>
  );
}
