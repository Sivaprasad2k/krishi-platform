import apiClient from '../../../core/api/axios';

/**
 * Plan a new treatment for a disease incident.
 * @param {Object} data { diseaseIncidentId, treatmentName, description, recommendedDosage, applicationMethod }
 */
export const createTreatment = async (data) => {
  const response = await apiClient.post('/treatments', data);
  return response.data;
};

/**
 * Fetch treatments for a specific disease incident.
 * @param {string} incidentId 
 */
export const getTreatmentsByIncident = async (incidentId) => {
  const response = await apiClient.get(`/disease-incidents/${incidentId}/treatments`);
  return response.data;
};

/**
 * Fetch a specific treatment by ID.
 * @param {string} id 
 */
export const getTreatmentById = async (id) => {
  const response = await apiClient.get(`/treatments/${id}`);
  return response.data;
};

/**
 * Mark a treatment as IN_PROGRESS.
 * @param {string} id 
 */
export const startTreatment = async (id) => {
  const response = await apiClient.post(`/treatments/${id}/start`);
  return response.data;
};

/**
 * Mark a treatment as COMPLETED.
 * @param {string} id 
 */
export const completeTreatment = async (id) => {
  const response = await apiClient.post(`/treatments/${id}/complete`);
  return response.data;
};

/**
 * Fetches all treatments for the authenticated farmer directly from GET /treatments.
 */
export const fetchAllTreatments = async () => {
  const response = await apiClient.get('/treatments');
  return response.data?.data || [];
};
