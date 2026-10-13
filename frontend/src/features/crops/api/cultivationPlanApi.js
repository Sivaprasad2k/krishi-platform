import apiClient from '../../../core/api/axios';

/**
 * Fetch published cultivation templates, optionally filtered by cropName.
 * @param {string} [cropName]
 * @returns {Promise<Array>} List of cultivation templates
 */
export const fetchPublishedTemplates = async (cropName) => {
  const params = { status: 'PUBLISHED' };
  if (cropName) {
    params.cropName = cropName;
  }
  const response = await apiClient.get('/cultivation-templates', { params });
  return response.data?.data || [];
};

/**
 * Fetch a single cultivation template with its full growth stages and activities.
 * @param {string} templateId
 * @returns {Promise<Object>} Template details
 */
export const fetchTemplateById = async (templateId) => {
  const response = await apiClient.get(`/cultivation-templates/${templateId}`);
  return response.data?.data;
};

/**
 * Generate or regenerate a cultivation plan for a crop planting.
 * @param {string} plantingId
 * @param {string} [templateId]
 * @returns {Promise<Object>} Cultivation plan details
 */
export const generateCultivationPlan = async (plantingId, templateId) => {
  const payload = templateId ? { templateId } : {};
  const response = await apiClient.post(`/crop-plantings/${plantingId}/cultivation-plan/generate`, payload);
  return response.data?.data;
};

/**
 * Fetch the current cultivation plan for a crop planting.
 * @param {string} plantingId
 * @returns {Promise<Object>} Cultivation plan details
 */
export const fetchCultivationPlan = async (plantingId) => {
  const response = await apiClient.get(`/crop-plantings/${plantingId}/cultivation-plan`);
  return response.data?.data;
};
