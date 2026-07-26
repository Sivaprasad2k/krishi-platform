import apiClient from '../../../core/api/axios';

/**
 * Transitions a Crop Cycle status from ACTIVE to CANCELLED.
 * @param {string} id The Crop Cycle UUID
 * @returns {Promise<Object>} API Response containing updated Crop Cycle details
 */
export const cancelCropCycle = async (id) => {
  const response = await apiClient.post(`/crop-cycles/${id}/cancel`);
  return response.data;
};
