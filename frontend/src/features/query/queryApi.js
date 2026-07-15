import { api } from '../../lib/api';

export const executeQuery = (workspaceId, connectionId, sql) =>
  api.post(`/workspaces/${workspaceId}/connections/${connectionId}/query`, { sql });

export const getQueryHistory = (workspaceId, connectionId, page = 0, size = 20) =>
  api.get(`/workspaces/${workspaceId}/connections/${connectionId}/query/history?page=${page}&size=${size}`);
