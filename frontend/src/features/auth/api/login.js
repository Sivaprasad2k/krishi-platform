import apiClient from '../../../core/api/axios';

/**
 * Logins user using credentials.
 * @param {string} phoneNumber 10-digit Indian phone number
 * @param {string} password Password string
 * @returns {Promise<Object>} API Response containing access token
 */
export const loginUser = async (phoneNumber, password) => {
  const response = await apiClient.post('/auth/login', { phoneNumber, password });
  return response.data;
};

/**
 * Fetches the currently logged in user profile details.
 * @returns {Promise<Object>} API Response containing user profile
 */
export const getCurrentUserProfile = async () => {
  const response = await apiClient.get('/auth/me');
  return response.data;
};
