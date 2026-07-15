import { NavLink } from 'react-router-dom';
import { Home, MessageSquare, Database, Code2, LayoutDashboard, FolderOpen, Brain, Settings } from 'lucide-react';
import { useUiStore } from '../../features/ui/useUiStore';

const NAV_ITEMS = [
  { to: '/overview', label: 'Overview', icon: Home },
  { to: '/ask', label: 'Ask (AI Chat)', icon: MessageSquare },
  { to: '/connections', label: 'Connections', icon: Database },
  { to: '/editor', label: 'SQL Editor', icon: Code2 },
  { to: '/dashboards', label: 'Dashboards', icon: LayoutDashboard },
  { to: '/reports', label: 'Reports', icon: FolderOpen },
  { to: '/prompts', label: 'Saved Prompts', icon: Brain },
  { to: '/settings', label: 'Settings', icon: Settings },
];

export default function Sidebar() {
  const collapsed = useUiStore((s) => s.sidebarCollapsed);

  return (
    <nav
      className={`hidden md:flex flex-col border-r border-border bg-bg-surface transition-all ${
        collapsed ? 'w-16' : 'w-56'
      }`}
    >
      <div className="flex h-14 items-center px-4 font-semibold text-text-primary">
        {collapsed ? 'QM' : 'QueryMind'}
      </div>
      <ul className="flex-1 space-y-1 px-2 py-2">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-accent-soft text-accent'
                    : 'text-text-secondary hover:bg-bg-surface-raised hover:text-text-primary'
                }`
              }
            >
              <Icon size={18} />
              {!collapsed && <span>{label}</span>}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
