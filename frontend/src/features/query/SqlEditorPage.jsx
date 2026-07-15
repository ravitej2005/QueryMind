import { useState } from 'react';
import Editor from '@monaco-editor/react';
import { useMutation } from '@tanstack/react-query';
import { executeQuery } from './queryApi';
import ConnectionPicker from '../../components/shared/ConnectionPicker';
import ErrorState from '../../components/states/ErrorState';
import EmptyState from '../../components/states/EmptyState';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';

export default function SqlEditorPage() {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const [connectionId, setConnectionId] = useState('');
  const [sql, setSql] = useState('SELECT * FROM \n');

  const runMutation = useMutation({
    mutationFn: () => executeQuery(workspaceId, connectionId, sql),
  });

  function handleEditorMount(editor, monaco) {
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      if (connectionId) runMutation.mutate();
    });
  }

  const result = runMutation.data;

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-3 border-b border-border bg-bg-surface px-4 py-2">
        <ConnectionPicker value={connectionId} onChange={setConnectionId} />
        <button
          onClick={() => runMutation.mutate()}
          disabled={!connectionId || runMutation.isPending}
          className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:opacity-90 disabled:opacity-50"
        >
          {runMutation.isPending ? 'Running…' : 'Run (⌘/Ctrl+Enter)'}
        </button>
      </div>

      <div className="h-1/2 border-b border-border">
        <Editor
          height="100%"
          defaultLanguage="sql"
          theme="vs-dark"
          value={sql}
          onChange={(v) => setSql(v ?? '')}
          onMount={handleEditorMount}
          options={{ minimap: { enabled: false }, fontSize: 14 }}
        />
      </div>

      <div className="flex-1 overflow-auto p-4">
        {!result && !runMutation.isError && (
          <EmptyState title="Run a query to see results" description="Write SELECT SQL above and hit Run." />
        )}
        {runMutation.isError && <ErrorState message={runMutation.error.message} />}
        {result && !result.success && (
          <div className="rounded border border-danger/40 bg-danger/10 p-3 text-sm text-danger">
            <p className="font-medium">Query blocked</p>
            <p>{result.rejectionReason}</p>
          </div>
        )}
        {result?.success && <ResultsTable result={result} />}
      </div>
    </div>
  );
}

function ResultsTable({ result }) {
  if (result.rowCount === 0) {
    return <p className="text-sm text-text-secondary">Query ran successfully — 0 rows returned.</p>;
  }
  return (
    <div className="overflow-auto rounded border border-border">
      <table className="min-w-full text-sm">
        <thead className="bg-bg-surface">
          <tr>
            {result.columns.map((c) => (
              <th key={c} className="px-3 py-2 text-left font-medium text-text-secondary">{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {result.rows.map((row, i) => (
            <tr key={i} className="border-t border-border">
              {result.columns.map((c) => (
                <td key={c} className="px-3 py-2 text-text-primary">{String(row[c])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="border-t border-border px-3 py-1.5 text-xs text-text-secondary">
        {result.rowCount} rows · {result.durationMs}ms
      </p>
    </div>
  );
}
