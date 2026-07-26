import apiClient from '../../../core/api/axios';

/**
 * Fetches details of a specific Crop Planting.
 * @param {string} id Crop Planting UUID
 * @returns {Promise<Object>} API Response containing Crop details
 */
export const getCrop = async (id) => {
  const response = await apiClient.get(`/crop-plantings/${id}`);
  return response.data;
};

/**
 * Aggregates all data required for the Crop Workspace (incidents, treatments, tasks, season details, farm).
 * @param {string} cropId Crop Planting UUID
 * @returns {Promise<Object>} Object containing compiled workspace details
 */
export const fetchCropWorkspaceDetails = async (cropId) => {
  const cropRes = await apiClient.get(`/crop-plantings/${cropId}`);
  const crop = cropRes.data?.data;
  
  if (!crop) throw new Error('Crop not found');
  
  const seasonRes = await apiClient.get(`/crop-cycles/${crop.cropCycleId}`);
  const season = seasonRes.data?.data;
  
  const farmName = season?.farm?.name || 'Farm';
  const farmId = season?.farm?.id;
  
  const incidentsRes = await apiClient.get(`/crop-plantings/${cropId}/disease-incidents`);
  const incidents = incidentsRes.data?.data || [];
  
  const openIncidents = incidents.filter(inc => inc.status !== 'CLOSED' && inc.status !== 'RESOLVED');
  
  const treatmentsList = [];
  const tasksList = [];
  
  for (const inc of incidents) {
    if (!inc?.id) continue;
    try {
      const treatmentsRes = await apiClient.get(`/disease-incidents/${inc.id}/treatments`);
      const treatments = treatmentsRes.data?.data || [];
      treatmentsList.push(...treatments);
      
      for (const treat of treatments) {
        if (!treat?.id) continue;
        try {
          const tasksRes = await apiClient.get(`/treatments/${treat.id}/tasks`);
          const tasks = tasksRes.data?.data || [];
          tasksList.push(...tasks);
        } catch (err) {
          console.warn(`Failed to fetch tasks for treatment ${treat.id} in workspace:`, err);
        }
      }
    } catch (err) {
      console.warn(`Failed to fetch treatments for incident ${inc.id} in workspace:`, err);
    }
  }

  return {
    crop,
    season,
    farmName,
    farmId,
    incidents,
    openIncidents,
    treatments: treatmentsList,
    tasks: tasksList
  };
};
