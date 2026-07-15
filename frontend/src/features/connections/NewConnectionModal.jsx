import { useState } from 'react';
import PropTypes from 'prop-types';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { testConnection, createConnection } from './connectionsApi';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';

const EMPTY_FORM = { name: '', host: '', port: 3306, databaseName: '', username: '', password: '' };

export default function NewConnectionModal({ onClose }) {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const queryClient = useQueryClient();
  const [form, setForm] = useState(EMPTY_FORM);
  const [testResult, setTestResult] = useState(null);

  const testMutation = useMutation({
    mutationFn: () => testConnection(workspaceId, { ...form, port: Number(form.port) }),
    onSuccess: setTestResult,
  });

  const createMutation = useMutation({
    mutationFn: () => createConnection(workspaceId, { ...form, port: Number(form.port) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connections', workspaceId] });
      onClose();
    },
  });

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg border border-border bg-bg-surface-raised p-6">
        <h2 className="mb-4 text-lg font-semibold text-text-primary">Connect a MySQL database</h2>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Name" value={form.name} onChange={update('name')} />
          <Field label="Port" type="number" value={form.port} onChange={update('port')} />
          <Field label="Host" value={form.host} onChange={update('host')} className="col-span-2" />
          <Field label="Database" value={form.databaseName} onChange={update('databaseName')} className="col-span-2" />
          <Field label="Username (read-only user)" value={form.username} onChange={update('username')} className="col-span-2" />
          <Field label="Password" type="password" value={form.password} onChange={update('password')} className="col-span-2" />
        </div>

        {testResult && (
          <p className={`mt-3 text-sm ${testResult.readOnly ? 'text-success' : 'text-warning'}`}>
            {testResult.message}
          </p>
        )}
        {createMutation.isError && (
          <p className="mt-3 text-sm text-danger">{createMutation.error.message}</p>
        )}

        <div className="mt-5 flex justify-between gap-2">
          <button onClick={onClose} className="rounded px-3 py-1.5 text-sm text-text-secondary hover:bg-bg-surface">
            Cancel
          </button>
          <div className="flex gap-2">
            <button
              onClick={() => testMutation.mutate()}
              disabled={testMutation.isPending}
              className="rounded border border-border px-3 py-1.5 text-sm text-text-primary hover:bg-bg-surface disabled:opacity-50"
            >
              {testMutation.isPending ? 'Testing…' : 'Test connection'}
            </button>
            <button
              onClick={() => createMutation.mutate()}
              disabled={!testResult?.readOnly || createMutation.isPending}
              className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:opacity-90 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Saving…' : 'Save connection'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

NewConnectionModal.propTypes = { onClose: PropTypes.func.isRequired };

function Field({ label, className = '', ...props }) {
  return (
    <label className={`block text-sm text-text-secondary ${className}`}>
      {label}
      <input
        {...props}
        className="mt-1 w-full rounded border border-border bg-bg-surface px-2 py-1.5 text-text-primary outline-none focus:ring-2 focus:ring-accent"
      />
    </label>
  );
}
Field.propTypes = { label: PropTypes.string.isRequired, className: PropTypes.string };
