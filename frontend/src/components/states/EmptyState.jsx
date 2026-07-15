import PropTypes from 'prop-types';

/** Empty states teach (design.md UX principle #4) — never a blank box. */
export default function EmptyState({ title, description, action }) {
  return (
    <div className="flex h-full min-h-[200px] flex-col items-center justify-center gap-2 px-6 text-center">
      <h3 className="font-medium text-text-primary">{title}</h3>
      {description && <p className="max-w-sm text-sm text-text-secondary">{description}</p>}
      {action}
    </div>
  );
}
EmptyState.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
  action: PropTypes.node,
};
