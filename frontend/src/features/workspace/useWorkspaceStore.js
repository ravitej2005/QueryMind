import { create } from 'zustand';

/** Current workspace id — client/UI state (memory.md §11), not server data. */
export const useWorkspaceStore = create((set) => ({
  currentWorkspaceId: null,
  setCurrentWorkspaceId: (id) => set({ currentWorkspaceId: id }),
}));
