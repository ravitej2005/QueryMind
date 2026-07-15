/**
 * Single API client wrapping fetch (memory.md §6 — no scattered fetch calls
 * in components). Access token is read from the auth store in memory only,
 * never localStorage (memory.md §2).
 */
import { useAuthStore } from '../features/auth/useAuthStore';

const BASE = '/api';

/**
 * @param {string} path
 * @param {RequestInit} [options]
 * @returns {Promise<any>}
 */
async function request(path, options = {}) {
  const accessToken = useAuthStore.getState().accessToken;
  const headers = {
    'Content-Type': 'application/json',
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...(options.headers || {}),
  };

  let res = await fetch(BASE + path, { ...options, headers, credentials: 'include' });

  if (res.status === 401 && accessToken) {
    // Silent-refresh-on-401 once, then retry the original request.
    const refreshed = await tryRefresh();
    if (refreshed) {
      const retryHeaders = { ...headers, Authorization: `Bearer ${useAuthStore.getState().accessToken}` };
      res = await fetch(BASE + path, { ...options, headers: retryHeaders, credentials: 'include' });
    }
  }

  if (!res.ok) {
    let body;
    try {
      body = await res.json();
    } catch {
      body = null;
    }
    const message = body?.error?.message || `Request failed (${res.status})`;
    const code = body?.error?.code || 'UNKNOWN_ERROR';
    const error = new Error(message);
    error.code = code;
    error.status = res.status;
    throw error;
  }

  if (res.status === 204) return null;
  return res.json();
}

let refreshPromise = null;

async function tryRefresh() {
  if (!refreshPromise) {
    refreshPromise = fetch(BASE + '/auth/refresh', { method: 'POST', credentials: 'include' })
      .then(async (res) => {
        if (!res.ok) {
          useAuthStore.getState().clearAuth();
          return false;
        }
        const body = await res.json();
        useAuthStore.getState().setAuth(body);
        return true;
      })
      .catch(() => {
        useAuthStore.getState().clearAuth();
        return false;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
  tryRefresh,
};
