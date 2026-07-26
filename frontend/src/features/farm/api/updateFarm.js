import apiClient from '../../../core/api/axios';

/**
 * Updates an existing Farm details.
 * @param {string} farmId UUID of the Farm
 * @param {Object} farmData { name: string, sizeHectares: number, location: string, status: string }
 * @returns {Promise<Object>} API Response containing updated Farm details
 */
export const updateFarm = async (farmId, farmData) => {
  const response = await apiClient.put(`/farms/${farmId}`, farmData);
  return response.data;
};
