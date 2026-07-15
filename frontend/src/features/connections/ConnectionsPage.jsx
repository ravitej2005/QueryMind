import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Database } from 'lucide-react';
import { listConnections, deleteConnection } from './connectionsApi';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';
import NewConnectionModal from './NewConnectionModal';
import LoadingState from '../../components/states/LoadingState';
import EmptyState from '../../components/states/EmptyState';
import ErrorState from '../../components/states/ErrorState';

export default function ConnectionsPage() {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['connections', workspaceId],
    queryFn: () => listConnections(workspaceId),
    enabled: !!workspaceId,
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => deleteConnection(workspaceId, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['connections', workspaceId] }),
  });

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-text-primary">Connections</h1>
        <button
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-1.5 rounded bg-accent px-3 py-1.5 text-sm text-white hover:opacity-90"
        >
          <Plus size={16} /> New connection
        </button>
      </div>

      {isLoading && <LoadingState label="Loading connections…" />}
      {isError && <ErrorState message={error.message} onRetry={refetch} />}
      {!isLoading && !isError && data?.length === 0 && (
        <EmptyState
          title="No connections yet"
          description="Connect a read-only MySQL credential to start asking questions about your data."
          action={
            <button onClick={() => setModalOpen(true)} className="mt-2 rounded bg-accent px-3 py-1.5 text-sm text-white">
              Connect a database
            </button>
          }
        />
      )}
      {!isLoading && !isError && data?.length > 0 && (
        <ul className="space-y-2">
          {data.map((conn) => (
            <li
              key={conn.id}
              className="flex items-center justify-between rounded border border-border bg-bg-surface px-4 py-3"
            >
              <div className="flex items-center gap-3">
                <Database size={18} className="text-accent" />
                <div>
                  <p className="text-sm font-medium text-text-primary">{conn.name}</p>
                  <p className="text-xs text-text-secondary">
                    {conn.host}:{conn.port}/{conn.databaseName}
                  </p>
                </div>
              </div>
              <button
                onClick={() => deleteMutation.mutate(conn.id)}
                className="rounded p-1.5 text-text-secondary hover:bg-bg-surface-raised hover:text-danger"
                aria-label="Delete connection"
              >
                <Trash2 size={16} />
              </button>
            </li>
          ))}
        </ul>
      )}

      {modalOpen && <NewConnectionModal onClose={() => setModalOpen(false)} />}
    </div>
  );
}
