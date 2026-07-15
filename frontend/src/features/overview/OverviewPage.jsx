import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { listWorkspaces } from '../workspace/workspaceApi';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';
import { useAuthStore } from '../auth/useAuthStore';
import LoadingState from '../../components/states/LoadingState';
import ErrorState from '../../components/states/ErrorState';

export default function OverviewPage() {
  const user = useAuthStore((s) => s.user);
  const setCurrentWorkspaceId = useWorkspaceStore((s) => s.setCurrentWorkspaceId);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['workspaces'],
    queryFn: listWorkspaces,
  });

  useEffect(() => {
    if (data?.length > 0) setCurrentWorkspaceId(data[0].id);
  }, [data, setCurrentWorkspaceId]);

  if (isLoading) return <LoadingState label="Loading your workspace…" />;
  if (isError) return <ErrorState message={error.message} onRetry={refetch} />;

  return (
    <div className="p-6">
      <h1 className="mb-2 text-lg font-semibold text-text-primary">
        Welcome back, {user?.displayName}
      </h1>
      <p className="text-sm text-text-secondary">
        Workspace: {data?.[0]?.name ?? '—'}. Head to Connections to add a database, then Ask a question.
      </p>
    </div>
  );
}
