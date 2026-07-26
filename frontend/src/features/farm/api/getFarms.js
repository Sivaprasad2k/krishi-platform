import apiClient from '../../../core/api/axios';

/**
 * Lists all Farms belonging to the authenticated farmer.
 * @returns {Promise<Object>} API Response containing array of Farms
 */
export const getFarms = async () => {
  const response = await apiClient.get('/farms');
  return response.data;
};
