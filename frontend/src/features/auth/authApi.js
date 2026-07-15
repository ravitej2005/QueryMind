import { api } from '../../lib/api';

/** @param {{email: string, password: string, displayName: string}} data */
export const registerUser = (data) => api.post('/auth/register', data);

/** @param {{email: string, password: string}} data */
export const loginUser = (data) => api.post('/auth/login', data);

export const logoutUser = () => api.post('/auth/logout', {});
