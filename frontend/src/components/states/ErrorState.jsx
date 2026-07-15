import PropTypes from 'prop-types';
import { AlertTriangle } from 'lucide-react';

/** Errors are actionable, not just descriptive (design.md UX principle #3). Never renders a raw error object. */
export default function ErrorState({ message, onRetry }) {
  return (
    <div className="flex h-full min-h-[200px] flex-col items-center justify-center gap-3 px-6 text-center">
      <AlertTriangle className="text-danger" size={24} />
      <p className="max-w-sm text-sm text-text-primary">{message || 'Something went wrong.'}</p>
      {onRetry && (
        <button onClick={onRetry} className="rounded bg-accent px-3 py-1.5 text-sm text-white hover:opacity-90">
          Retry
        </button>
      )}
    </div>
  );
}
ErrorState.propTypes = {
  message: PropTypes.string,
  onRetry: PropTypes.func,
};
