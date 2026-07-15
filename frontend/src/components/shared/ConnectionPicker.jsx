import PropTypes from 'prop-types';
import { useQuery } from '@tanstack/react-query';
import { listConnections } from '../../features/connections/connectionsApi';
import { useWorkspaceStore } from '../../features/workspace/useWorkspaceStore';

/** Shared connection dropdown used by SQL Editor and Ask pages. */
export default function ConnectionPicker({ value, onChange }) {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const { data, isLoading } = useQuery({
    queryKey: ['connections', workspaceId],
    queryFn: () => listConnections(workspaceId),
    enabled: !!workspaceId,
  });

  if (isLoading) return <span className="text-sm text-text-secondary">Loading connections…</span>;
  if (!data || data.length === 0) {
    return <span className="text-sm text-warning">No connections yet — add one first.</span>;
  }

  return (
    <select
      value={value || ''}
      onChange={(e) => onChange(e.target.value)}
      className="rounded border border-border bg-bg-surface px-2 py-1.5 text-sm text-text-primary outline-none focus:ring-2 focus:ring-accent"
    >
      <option value="" disabled>Select a connection</option>
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
