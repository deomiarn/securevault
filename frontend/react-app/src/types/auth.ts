export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
}

export interface AuthResponse {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: 'ADMIN' | 'MANAGER' | 'USER';
    message: string;
    accessToken: string;
    refreshToken: string;
    totpRequired: boolean;
}

export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: 'ADMIN' | 'MANAGER' | 'USER';
}
