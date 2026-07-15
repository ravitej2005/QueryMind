import PropTypes from 'prop-types';
import { useQuery } from '@tanstack/react-query';
import { listConnections } from '../../features/connections/connectionsApi';
import { useWorkspaceStore } from '../../features/workspace/useWorkspaceStore';

/** Shared data-source dropdown used by SQL Editor and Ask pages. */
export default function ConnectionPicker({ value, onChange }) {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const { data, isLoading } = useQuery({
    queryKey: ['connections', workspaceId],
    queryFn: () => listConnections(workspaceId),
    enabled: !!workspaceId,
  });

  // Distinguish "workspace still hydrating" (workspaceId not set yet, right
  // after login) from "workspace ready but genuinely has zero data sources"
  // — these used to show the identical message, which was confusing right
  // after a fresh register/login.
  if (!workspaceId || isLoading) {
    return <span className="text-sm text-text-secondary">Loading your data sources…</span>;
  }
  if (!data || data.length === 0) {
    return <span className="text-sm text-warning">No data source connected — add one first.</span>;
  }

  return (
    <select
      value={value || ''}
      onChange={(e) => onChange(e.target.value)}
      className="rounded border border-border bg-bg-surface px-2 py-1.5 text-sm text-text-primary outline-none focus:ring-2 focus:ring-accent"
    >
      <option value="" disabled>Select a data source</option>
      {data.map((c) => (
        <option key={c.id} value={c.id}>{c.name}</option>
      ))}
    </select>
  );
}

ConnectionPicker.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};
