import apiClient from './client';
import type {
    SecretResponse,
    SecretSummaryResponse,
    CreateSecretRequest,
    UpdateSecretRequest,
    FolderResponse,
    CreateFolderRequest,
    UpdateFolderRequest,
    PageResponse,
} from '../types/vault';

export const vaultApi = {
    // Secrets
    getSecrets(page = 0, size = 20): Promise<PageResponse<SecretSummaryResponse>> {
        return apiClient
            .get<PageResponse<SecretSummaryResponse>>('/secrets', { params: { page, size } })
            .then((res) => res.data);
    },

    getSecret(id: string): Promise<SecretResponse> {
        return apiClient.get<SecretResponse>(`/secrets/${id}`).then((res) => res.data);
    },

    createSecret(data: CreateSecretRequest): Promise<SecretResponse> {
        return apiClient.post<SecretResponse>('/secrets', data).then((res) => res.data);
    },

    updateSecret(id: string, data: UpdateSecretRequest): Promise<SecretResponse> {
        return apiClient.put<SecretResponse>(`/secrets/${id}`, data).then((res) => res.data);
    },

    deleteSecret(id: string): Promise<void> {
        return apiClient.delete(`/secrets/${id}`).then(() => undefined);
    },

    // Folders
    getFolders(): Promise<FolderResponse[]> {
        return apiClient.get<FolderResponse[]>('/folders').then((res) => res.data);
    },

    getFolder(id: string): Promise<FolderResponse> {
        return apiClient.get<FolderResponse>(`/folders/${id}`).then((res) => res.data);
    },

    createFolder(data: CreateFolderRequest): Promise<FolderResponse> {
        return apiClient.post<FolderResponse>('/folders', data).then((res) => res.data);
    },

    updateFolder(id: string, data: UpdateFolderRequest): Promise<FolderResponse> {
        return apiClient.put<FolderResponse>(`/folders/${id}`, data).then((res) => res.data);
    },

    deleteFolder(id: string): Promise<void> {
        return apiClient.delete(`/folders/${id}`).then(() => undefined);
    },
};
