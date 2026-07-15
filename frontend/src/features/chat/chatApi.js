import { api } from '../../lib/api';

export const askQuestion = (workspaceId, connectionId, question, executeOnly) =>
  api.post(`/workspaces/${workspaceId}/connections/${connectionId}/chat`, { question, executeOnly });
