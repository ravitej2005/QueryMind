import PropTypes from 'prop-types';

/** Skeleton loading state — design.md UX principle: never let the user wonder "is it thinking?" */
export default function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="flex h-full min-h-[200px] flex-col items-center justify-center gap-2 text-text-secondary">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent" />
      <span className="text-sm">{label}</span>
    </div>
  );
}
LoadingState.propTypes = { label: PropTypes.string };
