import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { askQuestion, getChatHistory } from './chatApi';
import ConnectionPicker from '../../components/shared/ConnectionPicker';
import ActiveDataSourceBar from '../../components/shared/ActiveDataSourceBar';
import EmptyState from '../../components/states/EmptyState';
import ErrorState from '../../components/states/ErrorState';
import { useWorkspaceStore } from '../workspace/useWorkspaceStore';

const EXAMPLE_QUESTIONS = [
  'What were our top 5 products last month?',
  'How many new customers signed up this week?',
  'Show me total revenue by region',
];

export default function AskPage() {
  const workspaceId = useWorkspaceStore((s) => s.currentWorkspaceId);
  const queryClient = useQueryClient();
  const [connectionId, setConnectionId] = useState('');
  const [question, setQuestion] = useState('');
  const [executeOnly, setExecuteOnly] = useState(false);
  const [messages, setMessages] = useState([]);

  // Hydrate the thread from persisted chat history whenever the active data
  // source changes (including on first load) — without this, refreshing the
  // page or switching sources silently lost every past question even though
  // the backend was persisting them all along.
  const historyQuery = useQuery({
    queryKey: ['chatHistory', workspaceId, connectionId],
    queryFn: () => getChatHistory(workspaceId, connectionId),
    enabled: !!workspaceId && !!connectionId,
  });

  useEffect(() => {
    if (!connectionId) {
      setMessages([]);
      return;
    }
    if (historyQuery.data) {
      // History comes back most-recent-first; the thread renders oldest-first.
      const hydrated = [...historyQuery.data].reverse().map((h) => ({
        question: h.question,
        success: h.status === 'ANSWERED',
        generatedSql: h.generatedSql,
        explanation: h.explanation,
        rejectionReason: h.rejectionReason,
        columns: [],
        rows: [],
        rowCount: 0,
        fromHistory: true,
      }));
      setMessages(hydrated);
    }
    // Switching data sources starts a fresh view of THAT source's history —
    // deliberately not merging threads across different databases.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connectionId, historyQuery.data]);

  const askMutation = useMutation({
    mutationFn: (q) => askQuestion(workspaceId, connectionId, q, executeOnly),
    onSuccess: (data, q) => {
      setMessages((m) => [...m, { question: q, ...data }]);
      queryClient.invalidateQueries({ queryKey: ['chatHistory', workspaceId, connectionId] });
    },
    onError: (err, q) => {
      // Every prompt must update the thread — including failures. Without
      // this, a failed request (network error, rate limit) silently did
      // nothing, leaving the user unsure whether their question was received.
      setMessages((m) => [
        ...m,
        { question: q, success: false, rejectionReason: err.message, columns: [], rows: [], rowCount: 0 },
      ]);
    },
  });

  function handleAsk(q) {
    // Hard guard against concurrent submissions: without this, clicking a
    // second example question (or double-submitting) before the first
    // request resolved could fire two mutate() calls on the same mutation,
    // and responses could land out of order — the results pane always shows
    // the LAST array entry, so a slower first response landing after a
    // faster second one would silently show the wrong answer.
    if (askMutation.isPending) return;
    if (!connectionId || !q.trim()) return;
    setQuestion('');
    askMutation.mutate(q);
  }

  return (
    <div className="flex h-full flex-col">
      <ActiveDataSourceBar connectionId={connectionId} />
      <div className="grid flex-1 grid-cols-1 overflow-hidden lg:grid-cols-2">
        {/* Left: chat thread */}
        <div className="flex flex-col overflow-hidden border-r border-border">
          <div className="flex items-center justify-between border-b border-border bg-bg-surface px-4 py-2">
            <ConnectionPicker value={connectionId} onChange={setConnectionId} />
            <label className="flex items-center gap-2 text-sm text-text-secondary">
              <input type="checkbox" checked={executeOnly} onChange={(e) => setExecuteOnly(e.target.checked)} />
              Explain only (don&apos;t run)
            </label>
          </div>

          <div className="flex-1 overflow-auto p-4">
            {!connectionId && (
              <EmptyState
                title="Pick a data source to start"
                description="Ask questions in plain English about a database you've connected. No data source connected yet? Head to Data Sources first."
              />
            )}

            {connectionId && historyQuery.isError && (
              <ErrorState message="Couldn't load past questions for this data source." />
            )}

            {connectionId && messages.length === 0 && !historyQuery.isLoading && (
              <EmptyState
                title="Ask a question about your data"
                description="Try one of these to get started:"
                action={
                  <div className="mt-3 flex flex-col gap-2">
                    {EXAMPLE_QUESTIONS.map((q) => (
                      <button
                        key={q}
                        onClick={() => handleAsk(q)}
                        disabled={askMutation.isPending}
                        className="rounded border border-border px-3 py-1.5 text-sm text-text-primary hover:bg-bg-surface disabled:opacity-50"
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
              disabled={!connectionId}
              className="flex-1 rounded border border-border bg-bg-surface px-3 py-2 text-sm text-text-primary outline-none focus:ring-2 focus:ring-accent disabled:opacity-50"
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
    </div>
  );
}

function LatestAnswer({ message }) {
  if (!message.success) {
    return (
      <div className="m-4 rounded border border-danger/40 bg-danger/10 p-3 text-sm text-danger">
        <p className="font-medium">This question couldn&apos;t be run</p>
        <p>{message.rejectionReason || 'Something went wrong generating or running this query.'}</p>
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

      {message.fromHistory ? (
        <p className="rounded border border-border bg-bg-surface p-3 text-xs text-text-secondary">
          This is a past question — its result table isn&apos;t kept in history, only the SQL and
          explanation. Ask it again to see live results.
        </p>
      ) : (
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
      )}

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
