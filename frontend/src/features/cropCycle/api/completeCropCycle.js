import apiClient from '../../../core/api/axios';

/**
 * Transitions a Crop Cycle status from ACTIVE to COMPLETED.
 * @param {string} id The Crop Cycle UUID
 * @returns {Promise<Object>} API Response containing updated Crop Cycle details
 */
export const completeCropCycle = async (id) => {
  const response = await apiClient.post(`/crop-cycles/${id}/complete`);
  return response.data;
};
