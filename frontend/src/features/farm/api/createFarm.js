import apiClient from '../../../core/api/axios';

/**
 * Creates a new Farm.
 * @param {Object} farmData { name: string, sizeHectares: number, location: string }
 * @returns {Promise<Object>} API Response containing created Farm details
 */
export const createFarm = async (farmData) => {
  const response = await apiClient.post('/farms', farmData);
  return response.data;
};
