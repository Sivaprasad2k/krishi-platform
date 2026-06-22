import apiClient from '../../../core/api/axios';

/**
 * Registers a new farmer user.
 * @param {Object} data Registration payload
 * @param {string} data.fullName Farmer's full name
 * @param {string} data.phoneNumber Indian 10-digit phone number
 * @param {string} [data.email] Optional email address
 * @param {string} data.password Strong password
 * @returns {Promise<Object>} API Response
 */
export const registerUser = async ({ fullName, phoneNumber, email, password }) => {
  const payload = {
    fullName,
    phoneNumber,
    email: email && email.trim() !== '' ? email.trim() : null,
    password
  };
  
  const response = await apiClient.post('/auth/register', payload);
  return response.data;
};
