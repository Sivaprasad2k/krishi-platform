const TOKEN_KEY = 'krishi_auth_token';

export const tokenStorage = {
  /**
   * Retrieves the stored auth token.
   * @returns {string|null} JWT token
   */
  getToken: () => {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch (error) {
      console.error('Failed to get token from storage:', error);
      return null;
    }
  },

  /**
   * Stores the auth token.
   * @param {string} token JWT token
   */
  saveToken: (token) => {
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      }
    } catch (error) {
      console.error('Failed to save token to storage:', error);
    }
  },

  /**
   * Removes the auth token from storage.
   */
  removeToken: () => {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch (error) {
      console.error('Failed to remove token from storage:', error);
    }
  }
};
