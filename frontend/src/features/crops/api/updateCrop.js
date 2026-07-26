import apiClient from '../../../core/api/axios';

/**
 * Updates an existing Crop Planting details.
 * @param {string} id Crop Planting UUID
 * @param {Object} cropData { cropName: string, cropVariety: string, plantingDate: string, expectedHarvestDate: string }
 * @returns {Promise<Object>} API Response containing updated Crop details
 */
export const updateCrop = async (id, cropData) => {
  const response = await apiClient.put(`/crop-plantings/${id}`, cropData);
  return response.data;
};
