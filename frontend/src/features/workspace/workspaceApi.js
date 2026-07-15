import { api } from '../../lib/api';

export const listWorkspaces = () => api.get('/workspaces');
