import axios from 'axios';

const apiClient = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request Interceptor: Add JWT token to headers if available
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 errors and attempt token refresh
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
    failedQueue.forEach((pending) => {
        if (token) {
            pending.resolve(token);
        } else {
            pending.reject(error);
        }
    });
    failedQueue = [];
};

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Only attempt refresh if it's a 401 error and we haven't already tried to refresh for this request
        if (
            error.response?.status !== 401 ||
            originalRequest._retry ||
            originalRequest.url === '/auth/refresh' ||
            originalRequest.url === '/auth/login'
        ) {
            return Promise.reject(error);
        }

        if (isRefreshing) {
            // Request in queue until token is refreshed
            return new Promise<string>((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            }).then((token) => {
                originalRequest.headers.Authorization = `Bearer ${token}`;
                return apiClient(originalRequest);
            });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            isRefreshing = false;
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return Promise.reject(error);
        }

        try {
            const response = await axios.post('/api/auth/refresh', { refreshToken });
            const { accessToken, refreshToken: newRefreshToken } = response.data;

            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', newRefreshToken);

            processQueue(null, accessToken);

            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return apiClient(originalRequest);
        } catch (refreshError) {
            processQueue(refreshError, null);
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    }
);

export default apiClient;
