import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { askQuestion } from './chatApi';
import ConnectionPicker from '../../components/shared/ConnectionPicker';
import EmptyState from '../../components/states/EmptyState';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';

const EXAMPLE_QUESTIONS = [
  'What were our top 5 products last month?',
  'How many new users signed up this week?',
  'Show me total revenue by region',
];

export default function AskPage() {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const [connectionId, setConnectionId] = useState('');
  const [question, setQuestion] = useState('');
  const [executeOnly, setExecuteOnly] = useState(false);
  const [messages, setMessages] = useState([]);

  const askMutation = useMutation({
    mutationFn: (q) => askQuestion(workspaceId, connectionId, q, executeOnly),
    onSuccess: (data, q) => {
      setMessages((m) => [...m, { question: q, ...data }]);
    },
  });

  function handleAsk(q) {
    if (!connectionId || !q.trim()) return;
    setQuestion('');
    askMutation.mutate(q);
  }

  return (
    <div className="grid h-full grid-cols-1 lg:grid-cols-2">
      {/* Left: chat thread */}
      <div className="flex flex-col border-r border-border">
        <div className="flex items-center justify-between border-b border-border bg-bg-surface px-4 py-2">
          <ConnectionPicker value={connectionId} onChange={setConnectionId} />
          <label className="flex items-center gap-2 text-sm text-text-secondary">
            <input type="checkbox" checked={executeOnly} onChange={(e) => setExecuteOnly(e.target.checked)} />
            Explain only (don&apos;t run)
          </label>
        </div>

        <div className="flex-1 overflow-auto p-4">
          {messages.length === 0 && (
            <EmptyState
              title="Ask a question about your data"
              description="Try one of these to get started:"
              action={
                <div className="mt-3 flex flex-col gap-2">
                  {EXAMPLE_QUESTIONS.map((q) => (
                    <button
                      key={q}
                      onClick={() => handleAsk(q)}
                      className="rounded border border-border px-3 py-1.5 text-sm text-text-primary hover:bg-bg-surface"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              }
            />
          )}

          <div className="space-y-4">
            {messages.map((m, i) => (
              <div key={i} className="ml-auto max-w-[85%] rounded-lg bg-accent-soft px-3 py-2 text-sm text-text-primary">
                {m.question}
              </div>
            ))}
            {askMutation.isPending && (
              <p className="text-sm text-text-secondary">Generating SQL…</p>
            )}
          </div>
        </div>

        <form
          onSubmit={(e) => { e.preventDefault(); handleAsk(question); }}
          className="flex gap-2 border-t border-border p-3"
        >
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask a question about your data…"
            className="flex-1 rounded border border-border bg-bg-surface px-3 py-2 text-sm text-text-primary outline-none focus:ring-2 focus:ring-accent"
          />
          <button
            type="submit"
            disabled={!connectionId || askMutation.isPending}
            className="flex items-center gap-1 rounded bg-accent px-3 py-2 text-sm text-white disabled:opacity-50"
          >
            <Send size={16} />
          </button>
        </form>
      </div>

      {/* Right: SQL + results + explanation for the latest message */}
      <div className="flex flex-col overflow-auto">
        {messages.length === 0 ? (
          <EmptyState title="SQL, results, and explanation appear here" />
        ) : (
          <LatestAnswer message={messages[messages.length - 1]} />
        )}
      </div>
    </div>
  );
}

function LatestAnswer({ message }) {
  if (!message.success) {
    return (
      <div className="m-4 rounded border border-danger/40 bg-danger/10 p-3 text-sm text-danger">
        <p className="font-medium">This question couldn&apos;t be run</p>
        <p>{message.rejectionReason}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <div>
        <h3 className="mb-1 text-xs font-medium uppercase tracking-wide text-text-secondary">Generated SQL</h3>
        <pre className="overflow-auto rounded border border-border bg-bg-surface p-3 text-xs text-text-primary">
          {message.generatedSql}
        </pre>
      </div>

      <div>
        <h3 className="mb-1 text-xs font-medium uppercase tracking-wide text-text-secondary">
          Results ({message.rowCount} rows)
        </h3>
        <div className="overflow-auto rounded border border-border">
          <table className="min-w-full text-sm">
            <thead className="bg-bg-surface">
              <tr>
                {message.columns.map((c) => (
                  <th key={c} className="px-3 py-2 text-left font-medium text-text-secondary">{c}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {message.rows.map((row, i) => (
                <tr key={i} className="border-t border-border">
                  {message.columns.map((c) => (
                    <td key={c} className="px-3 py-2 text-text-primary">{String(row[c])}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {message.explanation && (
        <div>
          <h3 className="mb-1 text-xs font-medium uppercase tracking-wide text-text-secondary">Explanation</h3>
          <p className="rounded border border-border bg-bg-surface p-3 text-sm text-text-primary">
            {message.explanation}
          </p>
        </div>
      )}
    </div>
  );
}
