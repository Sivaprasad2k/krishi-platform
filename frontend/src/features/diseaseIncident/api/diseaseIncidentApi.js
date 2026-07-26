import apiClient from '../../../core/api/axios';

/**
 * Report a new crop disease incident.
 * @param {Object} data { cropPlantingId, diseaseName, description }
 */
export const reportDiseaseIncident = async (data) => {
  const response = await apiClient.post('/disease-incidents', data);
  return response.data;
};

/**
 * Fetch disease incidents for a specific crop planting.
 * @param {string} plantingId 
 */
export const getIncidentsByPlanting = async (plantingId) => {
  const response = await apiClient.get(`/crop-plantings/${plantingId}/disease-incidents`);
  return response.data;
};

/**
 * Fetch a specific disease incident by ID.
 * @param {string} id 
 */
export const getIncidentById = async (id) => {
  const response = await apiClient.get(`/disease-incidents/${id}`);
  return response.data;
};

/**
 * Mark a disease incident as UNDER_REVIEW.
 * @param {string} id 
 */
export const reviewIncident = async (id) => {
  const response = await apiClient.post(`/disease-incidents/${id}/review`);
  return response.data;
};

/**
 * Mark a disease incident as TREATED.
 * @param {string} id 
 */
export const treatIncident = async (id) => {
  const response = await apiClient.post(`/disease-incidents/${id}/treat`);
  return response.data;
};

/**
 * Mark a disease incident as CLOSED.
 * @param {string} id 
 */
export const closeIncident = async (id) => {
  const response = await apiClient.post(`/disease-incidents/${id}/close`);
  return response.data;
};

/**
 * Traverses farms, crop cycles, and plantings to collect all disease incidents across the farm.
 */
export const fetchAllDiseaseIncidents = async () => {
  try {
    const farmsRes = await apiClient.get('/farms');
    const farms = farmsRes.data?.data || [];

    let allIncidents = [];

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
                  allIncidents.push({
                    ...incident,
                    cropName: planting.cropName || cycle.cropName || 'Crop',
                    cropVariety: planting.cropVariety || 'Standard',
                    farmName: farm.name || 'Farm',
                    seasonName: cycle.name || 'Season'
                  });
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

    return allIncidents;
  } catch (error) {
    console.error('Failed to fetch all disease incidents:', error);
    throw error;
  }
};
