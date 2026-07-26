import axios from 'axios';
import { tokenStorage } from '../storage/tokenStorage';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

// Request Interceptor: Inject JWT token into headers
apiClient.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Standard error handling
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Check if error is authentication-related (unauthorized/expired token)
    if (error.response && error.response.status === 401) {
      console.warn('Unauthorized request (401). Evicting session and redirecting...');
      // Lazily import useAuthStore to prevent evaluation circular dependencies
      import('../../store/authStore').then(({ useAuthStore }) => {
        useAuthStore.getState().logout();
      });
    }
    
    // Format error response so callers receive readable details
    const apiError = {
      message: error.response?.data?.message || error.message || 'An unexpected error occurred',
      errorCode: error.response?.data?.errorCode || 'INTERNAL_SERVER_ERROR',
      status: error.response?.status || 500,
      data: error.response?.data?.data || null,
      success: false,
    };
    
    return Promise.reject(apiError);
  }
);

export default apiClient;
