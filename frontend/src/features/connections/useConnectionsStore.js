import { create } from 'zustand';

/** In-progress "new connection" form state — genuine client/UI state. */
export const useSelectedConnectionStore = create((set) => ({
  selectedConnectionId: null,
  setSelectedConnectionId: (id) => set({ selectedConnectionId: id }),
}));
