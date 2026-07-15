import PropTypes from 'prop-types';
import { useQuery } from '@tanstack/react-query';
import { Database, AlertTriangle } from 'lucide-react';
import { listConnections, getSchema } from '../../features/connections/connectionsApi';
import { useWorkspaceStore } from '../../features/workspace/useWorkspaceStore';

// Table names that only ever exist in QueryMind's OWN app database. If a
// connected data source's schema is made up entirely of these, the user has
// likely pointed QueryMind at its own database instead of a real business
// database — surface that clearly rather than silently returning confusing
// AI answers about "users" and "chat_messages".
const INTERNAL_TABLE_NAMES = new Set([
  'users', 'workspaces', 'workspace_members', 'connections',
  'query_history', 'chat_messages', 'flyway_schema_history',
]);

/**
 * Shows which data source is currently active (product-story requirement:
 * the interview demo should make "I can switch between connected databases"
 * obvious even with only one connected), and warns if the active source
 * looks like QueryMind's own internal database rather than a real one.
 */
export default function ActiveDataSourceBar({ connectionId }) {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);

  const { data: connections } = useQuery({
    queryKey: ['connections', workspaceId],
    queryFn: () => listConnections(workspaceId),
    enabled: !!workspaceId,
  });

  const { data: schema } = useQuery({
    queryKey: ['schema', workspaceId, connectionId],
    queryFn: () => getSchema(workspaceId, connectionId),
    enabled: !!workspaceId && !!connectionId,
    staleTime: 5 * 60 * 1000,
  });

  if (!connectionId) return null;

  const active = connections?.find((c) => c.id === connectionId);
  const tableNames = schema?.tables?.map((t) => t.name.toLowerCase()) ?? [];
  const looksInternal =
    tableNames.length > 0 && tableNames.every((name) => INTERNAL_TABLE_NAMES.has(name));

  return (
    <div className="border-b border-border bg-bg-surface px-4 py-1.5">
      <div className="flex items-center gap-1.5 text-xs text-text-secondary">
        <Database size={12} className="text-accent" />
        Connected to <span className="font-medium text-text-primary">{active?.name ?? '…'}</span>
        {schema && <span>· {schema.tables.length} tables</span>}
      </div>
      {looksInternal && (
        <div className="mt-1 flex items-start gap-1.5 text-xs text-warning">
          <AlertTriangle size={12} className="mt-0.5 shrink-0" />
          <span>
            This data source only has QueryMind&apos;s own application tables.
            Connect your business database on the Data Sources page to ask
            questions about real data.
          </span>
        </div>
      )}
    </div>
  );
}

ActiveDataSourceBar.propTypes = {
  connectionId: PropTypes.string,
};
