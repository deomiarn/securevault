export type SecretType = 'PASSWORD' | 'API_KEY' | 'NOTE' | 'CERTIFICATE' | 'OTHER';

export interface SecretResponse {
    id: string;
    name: string;
    description: string;
    value: string;
    secretType: SecretType;
    folderId: string | null;
    folderName: string | null;
    createdAt: string;
    updatedAt: string;
    shared: boolean;
}

export interface SecretSummaryResponse {
    id: string;
    name: string;
    secretType: SecretType;
    folderName: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface CreateSecretRequest {
    name: string;
    value: string;
    description?: string;
    secretType?: SecretType;
    folderId?: string;
}

export interface UpdateSecretRequest {
    name?: string;
    value?: string;
    description?: string;
    secretType?: SecretType;
    folderId?: string;
}

export interface FolderResponse {
    id: string;
    name: string;
    parentFolderId: string | null;
    childFolders: FolderResponse[];
    secretCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateFolderRequest {
    name: string;
    parentFolderId?: string;
}

export interface UpdateFolderRequest {
    name?: string;
    parentFolderId?: string;
}

export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}
