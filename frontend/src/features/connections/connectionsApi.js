import { api } from '../../lib/api';

export const listConnections = (workspaceId) => api.get(`/workspaces/${workspaceId}/connections`);

export const testConnection = (workspaceId, payload) =>
  api.post(`/workspaces/${workspaceId}/connections/test`, payload);

export const createConnection = (workspaceId, payload) =>
  api.post(`/workspaces/${workspaceId}/connections`, payload);

export const deleteConnection = (workspaceId, connectionId) =>
  api.delete(`/workspaces/${workspaceId}/connections/${connectionId}`);

export const getSchema = (workspaceId, connectionId) =>
  api.get(`/workspaces/${workspaceId}/connections/${connectionId}/schema`);
