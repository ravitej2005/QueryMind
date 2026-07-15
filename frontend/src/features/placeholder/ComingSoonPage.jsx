import PropTypes from 'prop-types';
import EmptyState from '../../components/states/EmptyState';

/** Honest placeholder for Phase 5+ screens (dashboards/reports/prompts) not yet built. */
export default function ComingSoonPage({ title }) {
  return (
    <div className="p-6">
      <EmptyState title={title} description="This screen is planned for a later phase and isn't built yet." />
    </div>
  );
}
ComingSoonPage.propTypes = { title: PropTypes.string.isRequired };
