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
 * Traverses farms, crop cycles, plantings, and disease incidents to collect all treatments across the farm.
 */
export const fetchAllTreatments = async () => {
  try {
    const farmsRes = await apiClient.get('/farms');
    const farms = farmsRes.data?.data || [];

    let allTreatments = [];

    for (const farm of farms) {
      if (!farm?.id) continue;
      try {
        const cyclesRes = await apiClient.get(`/farms/${farm.id}/crop-cycles`);
        const cycles = cyclesRes.data?.data || [];

        for (const cycle of cycles) {
          if (!cycle?.id) continue;
          try {
            const plantingsRes = await apiClient.get(`/crop-cycles/${cycle.id}/plantings`);
            const plantings = plantingsRes.data?.data || [];

            for (const planting of plantings) {
              if (!planting?.id) continue;
              try {
                const incidentsRes = await apiClient.get(`/crop-plantings/${planting.id}/disease-incidents`);
                const incidents = incidentsRes.data?.data || [];

                for (const incident of incidents) {
                  if (!incident?.id) continue;
                  try {
                    const treatmentsRes = await apiClient.get(`/disease-incidents/${incident.id}/treatments`);
                    const treatments = treatmentsRes.data?.data || [];

                    for (const treatment of treatments) {
                      allTreatments.push({
                        ...treatment,
                        diseaseName: incident.diseaseName || 'Disease Incident',
                        cropName: planting.cropName || cycle.cropName || 'Crop',
                        cropVariety: planting.cropVariety || 'Standard',
                        farmName: farm.name || 'Farm'
                      });
                    }
                  } catch (err) {
                    console.warn(`Failed to fetch treatments for incident ${incident.id}:`, err);
                  }
                }
              } catch (err) {
                console.warn(`Failed to fetch incidents for planting ${planting.id}:`, err);
              }
            }
          } catch (err) {
            console.warn(`Failed to fetch plantings for cycle ${cycle.id}:`, err);
          }
        }
      } catch (err) {
        console.warn(`Failed to fetch cycles for farm ${farm.id}:`, err);
      }
    }

    return allTreatments;
  } catch (error) {
    console.error('Failed to fetch all treatments:', error);
    throw error;
  }
};
