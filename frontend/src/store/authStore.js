import { create } from 'zustand';
import { tokenStorage } from '../core/storage/tokenStorage';
import { loginUser, getCurrentUserProfile } from '../features/auth/api/login';
import { registerUser } from '../features/auth/api/register';
import { logoutUser } from '../features/auth/api/logout';

export const useAuthStore = create((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  loading: true,
  error: null,

  /**
   * Initializes session auth state from local storage.
   * Performs an instant local claims layout restore, then verifies dynamically with the backend.
   */
  initializeAuth: async () => {
    const token = tokenStorage.getToken();
    if (token) {
      // Set initial state instantly to prevent flicker on authenticated routes
      set({
        token,
        isAuthenticated: true,
        loading: true,
        error: null
      });

      try {
        // Asynchronously check with backend if the token is valid and get fresh user profile
        const apiResponse = await getCurrentUserProfile();
        if (apiResponse.success && apiResponse.data) {
          set({
            user: apiResponse.data,
            loading: false
          });
        } else {
          throw new Error('Verification failed');
        }
      } catch (err) {
        console.warn('Cached session token validation failed, logging out...', err.message || err);
        // Clear expired/invalid token
        logoutUser();
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          loading: false,
          error: err.message || 'Session expired'
        });
      }
    } else {
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: null
      });
    }
  },

  /**
   * Authenticates user with phone and password, saves token, and fetches profile.
   */
  login: async (phoneNumber, password) => {
    set({ loading: true, error: null });
    try {
      const loginResponse = await loginUser(phoneNumber, password);
      
      if (loginResponse.success && loginResponse.data) {
        const { accessToken } = loginResponse.data;
        
        // Cache token locally
        tokenStorage.saveToken(accessToken);

        // Retrieve fresh user profile
        const profileResponse = await getCurrentUserProfile();
        
        if (profileResponse.success && profileResponse.data) {
          set({
            user: profileResponse.data,
            token: accessToken,
            isAuthenticated: true,
            loading: false,
            error: null
          });
          return { success: true };
        }
      }
      throw new Error(loginResponse.message || 'Authentication failed');
    } catch (err) {
      // Clean up token cache on login failure
      logoutUser();
      const errorMsg = err.message || 'Login failed';
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: errorMsg
      });
      return { success: false, error: errorMsg };
    }
  },

  /**
   * Registers a new user.
   */
  register: async (fullName, phoneNumber, email, password) => {
    set({ loading: true, error: null });
    try {
      const registerResponse = await registerUser({ fullName, phoneNumber, email, password });
      set({ loading: false });
      if (registerResponse.success) {
        return { success: true };
      }
      throw new Error(registerResponse.message || 'Registration failed');
    } catch (err) {
      set({ loading: false, error: err.message || 'Registration failed' });
      return { success: false, error: err.message || 'Registration failed' };
    }
  },

  /**
   * Logs out user by clearing storage cache and state.
   */
  logout: () => {
    logoutUser();
    set({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
      error: null
    });
  }
}));
