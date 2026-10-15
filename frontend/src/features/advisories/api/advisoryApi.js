import apiClient from '../../../core/api/axios';

/**
 * Triggers deterministic rules evaluation for a crop planting and returns active advisories.
 * @param {string} plantingId Crop Planting UUID
 * @returns {Promise<Object>} Advisory evaluation summary response
 */
export const evaluateAdvisories = async (plantingId) => {
  const response = await apiClient.post(`/crop-plantings/${plantingId}/advisories/evaluate`);
  return response.data?.data;
};

/**
 * Lists agricultural advisories for a crop planting, optionally filtered by status.
 * @param {string} plantingId Crop Planting UUID
 * @param {string} [status] Optional AdvisoryStatus filter ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', etc.)
 * @returns {Promise<Array>} List of agricultural advisories
 */
export const fetchAdvisories = async (plantingId, status) => {
  const params = status ? { status } : {};
  const response = await apiClient.get(`/crop-plantings/${plantingId}/advisories`, { params });
  return response.data?.data || [];
};

/**
 * Acknowledges an active agricultural advisory.
 * @param {string} advisoryId Advisory UUID
 * @returns {Promise<Object>} Updated agricultural advisory
 */
export const acknowledgeAdvisory = async (advisoryId) => {
  const response = await apiClient.patch(`/advisories/${advisoryId}/acknowledge`);
  return response.data?.data;
};

/**
 * Manually resolves an active or acknowledged agricultural advisory.
 * @param {string} advisoryId Advisory UUID
 * @returns {Promise<Object>} Updated agricultural advisory
 */
export const resolveAdvisory = async (advisoryId) => {
  const response = await apiClient.patch(`/advisories/${advisoryId}/resolve`);
  return response.data?.data;
};
