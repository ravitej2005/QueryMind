import { create } from 'zustand';

/**
 * Client/UI auth state only. Access token lives in memory (never
 * localStorage/sessionStorage — memory.md §2). Refresh token is an
 * HttpOnly cookie the browser manages automatically, invisible to JS.
 */
export const useAuthStore = create((set) => ({
  accessToken: null,
  user: null,
  isInitializing: true,

  /** @param {{accessToken: string, userId: string, email: string, displayName: string}} authResponse */
  setAuth: (authResponse) =>
    set({
      accessToken: authResponse.accessToken,
      user: {
        id: authResponse.userId,
        email: authResponse.email,
        displayName: authResponse.displayName,
      },
    }),

  clearAuth: () => set({ accessToken: null, user: null }),

  setInitializing: (value) => set({ isInitializing: value }),
}));
