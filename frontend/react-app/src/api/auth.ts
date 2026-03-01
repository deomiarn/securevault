import apiClient from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth';

export const authApi = {
    login(data: LoginRequest): Promise<AuthResponse> {
        return apiClient.post<AuthResponse>('/auth/login', data).then((res) => res.data);
    },

    register(data: RegisterRequest): Promise<AuthResponse> {
        return apiClient.post<AuthResponse>('/auth/register', data).then((res) => res.data);
    },

    refresh(refreshToken: string): Promise<AuthResponse> {
        return apiClient
            .post<AuthResponse>('/auth/refresh', { refreshToken })
            .then((res) => res.data);
    },

    logout(): Promise<void> {
        const refreshToken = localStorage.getItem('refreshToken');
        return apiClient.post('/auth/logout', { refreshToken }).then(() => undefined);
    },
};
