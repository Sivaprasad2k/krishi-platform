import apiClient from '../../../core/api/axios';

/**
 * Creates a new Crop Planting.
 * @param {Object} cropData { cropCycleId: string, cropName: string, cropVariety: string, plantingDate: string, expectedHarvestDate: string }
 * @returns {Promise<Object>} API Response containing created Crop details
 */
export const createCrop = async (cropData) => {
  const response = await apiClient.post('/crop-plantings', cropData);
  return response.data;
};
