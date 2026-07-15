import { create } from 'zustand';

/** Genuine client/UI state only (design.md §2, memory.md §11): theme + sidebar collapse. */
export const useUiStore = create((set) => ({
  theme: 'dark',
  sidebarCollapsed: false,
  toggleTheme: () => set((s) => ({ theme: s.theme === 'dark' ? 'light' : 'dark' })),
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
}));
