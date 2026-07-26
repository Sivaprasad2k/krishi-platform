import apiClient from '../../../core/api/axios';

/**
 * Transitions a Crop Cycle status from DRAFT to ACTIVE.
 * @param {string} id The Crop Cycle UUID
 * @returns {Promise<Object>} API Response containing updated Crop Cycle details
 */
export const startCropCycle = async (id) => {
  const response = await apiClient.post(`/crop-cycles/${id}/start`);
  return response.data;
};
