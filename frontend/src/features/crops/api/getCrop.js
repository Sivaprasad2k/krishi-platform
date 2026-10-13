import apiClient from '../../../core/api/axios';

/**
 * Fetches details of a specific Crop Planting.
 * @param {string} id Crop Planting UUID
 * @returns {Promise<Object>} API Response containing Crop details
 */
export const getCrop = async (id) => {
  const response = await apiClient.get(`/crop-plantings/${id}`);
  return response.data;
};

/**
 * Fetches compiled details for the Crop Workspace via the unified endpoint.
 * Eliminates the sequential multi-hop request waterfall by querying the backend aggregator.
 * @param {string} cropId Crop Planting UUID
 * @returns {Promise<Object>} Object containing compiled workspace details
 */
export const fetchCropWorkspaceDetails = async (cropId) => {
  const response = await apiClient.get(`/crop-plantings/${cropId}/workspace`);
  const data = response.data?.data;
  if (!data) throw new Error('Crop workspace details not found');
  return data;
};
