import apiClient from '../../../core/api/axios';

/**
 * Retrieves details of a specific Farm by ID.
 * @param {string} farmId UUID of the Farm
 * @returns {Promise<Object>} API Response containing Farm details
 */
export const getFarm = async (farmId) => {
  const response = await apiClient.get(`/farms/${farmId}`);
  return response.data;
};
