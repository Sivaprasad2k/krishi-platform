import apiClient from '../../../core/api/axios';

/**
 * Lists all Crop Cycles associated with a specific Farm.
 * @param {string} farmId The Farm UUID
 * @returns {Promise<Object>} API Response containing a list of Crop Cycles
 */
export const getFarmCropCycles = async (farmId) => {
  const response = await apiClient.get(`/farms/${farmId}/crop-cycles`);
  return response.data;
};
