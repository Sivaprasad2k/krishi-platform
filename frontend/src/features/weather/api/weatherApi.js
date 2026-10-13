import apiClient from '../../../core/api/axios';

/**
 * Retrieves weather forecast and observations for a farm.
 * @param {string} farmId 
 * @returns {Promise<Object>} API response data containing WeatherForecastResponse
 */
export const getFarmWeather = async (farmId) => {
  const response = await apiClient.get(`/farms/${farmId}/weather`);
  return response.data;
};

/**
 * Manually refreshes weather forecast for a farm (respecting 15m cooldown).
 * @param {string} farmId 
 * @returns {Promise<Object>} API response data containing WeatherForecastResponse
 */
export const refreshFarmWeather = async (farmId) => {
  const response = await apiClient.post(`/farms/${farmId}/weather/refresh`);
  return response.data;
};

/**
 * Retrieves contextual weather advisories for a farm.
 * @param {string} farmId 
 * @returns {Promise<Object>} API response data containing list of WeatherAdvisoryResponse
 */
export const getFarmAdvisories = async (farmId) => {
  const response = await apiClient.get(`/farms/${farmId}/advisories`);
  return response.data;
};

/**
 * Acknowledges a weather advisory.
 * @param {string} advisoryId 
 * @returns {Promise<Object>} API response data containing acknowledged WeatherAdvisoryResponse
 */
export const acknowledgeAdvisory = async (advisoryId) => {
  const response = await apiClient.post(`/advisories/${advisoryId}/acknowledge`);
  return response.data;
};

/**
 * Retrieves weather advisories linked to a specific task.
 * @param {string} taskId 
 * @returns {Promise<Object>} API response data containing list of WeatherAdvisoryResponse
 */
export const getTaskAdvisories = async (taskId) => {
  const response = await apiClient.get(`/tasks/${taskId}/advisories`);
  return response.data;
};
