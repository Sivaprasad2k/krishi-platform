import apiClient from '../../../core/api/axios';

/**
 * Creates a new Crop Cycle (Season).
 * @param {Object} cropCycleData { name: string, cropName: string, farmId: string }
 * @returns {Promise<Object>} API Response containing created Crop Cycle details
 */
export const createCropCycle = async (cropCycleData) => {
  const response = await apiClient.post('/crop-cycles', cropCycleData);
  return response.data;
};
