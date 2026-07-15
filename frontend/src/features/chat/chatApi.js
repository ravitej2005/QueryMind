import { api } from '../../lib/api';

export const askQuestion = (workspaceId, connectionId, question, executeOnly) =>
  api.post(`/workspaces/${workspaceId}/connections/${connectionId}/chat`, { question, executeOnly });

export const getChatHistory = (workspaceId, connectionId, page = 0, size = 20) =>
  api.get(`/workspaces/${workspaceId}/connections/${connectionId}/chat/history?page=${page}&size=${size}`);
