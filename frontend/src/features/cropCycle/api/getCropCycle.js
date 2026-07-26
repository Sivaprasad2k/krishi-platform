import apiClient from '../../../core/api/axios';

/**
 * Retrieves details of a specific Crop Cycle.
 * @param {string} id The Crop Cycle UUID
 * @returns {Promise<Object>} API Response containing Crop Cycle details
 */
export const getCropCycle = async (id) => {
  const response = await apiClient.get(`/crop-cycles/${id}`);
  return response.data;
};
