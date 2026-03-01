import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi } from '../api/auth';
import type { User } from '../types/auth';

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, password: string, firstName?: string, lastName?: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Check for existing token on mount and set user if valid
    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                setUser({
                    id: payload.sub,
                    email: payload.email,
                    firstName: '',
                    lastName: '',
                    role: payload.role,
                });
            } catch {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
            }
        }
        setIsLoading(false);
    }, []);

    const login = async (email: string, password: string) => {
        const response = await authApi.login({ email, password });
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        setUser({
            id: response.id,
            email: response.email,
            firstName: response.firstName,
            lastName: response.lastName,
            role: response.role,
        });
    };

    const register = async (
        email: string,
        password: string,
        firstName?: string,
        lastName?: string
    ) => {
        const response = await authApi.register({ email, password, firstName, lastName });
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        setUser({
            id: response.id,
            email: response.email,
            firstName: response.firstName,
            lastName: response.lastName,
            role: response.role,
        });
    };

    const logout = () => {
        authApi.logout().catch(() => {});
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                isAuthenticated: !!user,
                isLoading,
                login,
                register,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
