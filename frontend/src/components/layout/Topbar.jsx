import { PanelLeftClose, PanelLeft, Sun, Moon, LogOut } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { useUiStore } from '../../features/ui/useUiStore';
import { useAuthStore } from '../../features/auth/useAuthStore';
import { logoutUser } from '../../features/auth/authApi';
import { useNavigate } from 'react-router-dom';

export default function Topbar() {
  const { sidebarCollapsed, toggleSidebar, theme, toggleTheme } = useUiStore();
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const navigate = useNavigate();

  const logoutMutation = useMutation({
    mutationFn: logoutUser,
    onSettled: () => {
      clearAuth();
      navigate('/login');
    },
  });

  return (
    <header className="flex h-14 items-center justify-between border-b border-border bg-bg-surface px-4">
      <button
        onClick={toggleSidebar}
        className="hidden rounded p-1.5 text-text-secondary hover:bg-bg-surface-raised md:block"
        aria-label="Toggle sidebar"
      >
        {sidebarCollapsed ? <PanelLeft size={18} /> : <PanelLeftClose size={18} />}
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-3">
        <button
          onClick={toggleTheme}
          className="rounded p-1.5 text-text-secondary hover:bg-bg-surface-raised"
          aria-label="Toggle theme"
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>
        {user && <span className="text-sm text-text-secondary">{user.displayName}</span>}
        <button
          onClick={() => logoutMutation.mutate()}
          className="flex items-center gap-1 rounded p-1.5 text-text-secondary hover:bg-bg-surface-raised"
          aria-label="Log out"
        >
          <LogOut size={18} />
        </button>
      </div>
    </header>
  );
}
