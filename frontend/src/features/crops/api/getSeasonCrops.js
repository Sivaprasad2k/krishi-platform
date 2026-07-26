import apiClient from '../../../core/api/axios';

/**
 * Fetches all crop plantings under a specific Crop Cycle (Season).
 * @param {string} cropCycleId Crop Cycle UUID
 * @returns {Promise<Object>} API Response containing list of crop plantings
 */
export const getSeasonCrops = async (cropCycleId) => {
  const response = await apiClient.get(`/crop-cycles/${cropCycleId}/plantings`);
  return response.data;
};

/**
 * Fetches all crops across all active seasons for the farmer, resolving detailed operational metrics.
 * @returns {Promise<Array>} List of crops with problem and task counts
 */
export const fetchCropsList = async () => {
  const farmsRes = await apiClient.get('/farms');
  const farms = farmsRes.data?.data || [];
  
  const cropsList = [];

  for (const farm of farms) {
    if (!farm?.id) continue;
    try {
      const cyclesRes = await apiClient.get(`/farms/${farm.id}/crop-cycles`);
      const cycles = cyclesRes.data?.data || [];
      
      const activeCycles = cycles.filter(c => c.status === 'ACTIVE' || c.status === 'DRAFT');
      
      for (const cycle of activeCycles) {
        if (!cycle?.id) continue;
        try {
          const plantingsRes = await apiClient.get(`/crop-cycles/${cycle.id}/plantings`);
          const plantings = plantingsRes.data?.data || [];
          
          for (const planting of plantings) {
            if (!planting?.id) continue;
            try {
              const incidentsRes = await apiClient.get(`/crop-plantings/${planting.id}/disease-incidents`);
              const incidents = incidentsRes.data?.data || [];
              const openIncidents = incidents.filter(inc => inc.status !== 'CLOSED' && inc.status !== 'RESOLVED');
              
              let pendingTaskCount = 0;
              
              for (const inc of openIncidents) {
                if (!inc?.id) continue;
                try {
                  const treatmentsRes = await apiClient.get(`/disease-incidents/${inc.id}/treatments`);
                  const treatments = treatmentsRes.data?.data || [];
                  
                  for (const treat of treatments) {
                    if (!treat?.id) continue;
                    try {
                      const tasksRes = await apiClient.get(`/treatments/${treat.id}/tasks`);
                      const tasks = tasksRes.data?.data || [];
                      const openTasks = tasks.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS');
                      pendingTaskCount += openTasks.length;
                    } catch (err) {
                      console.warn(`Failed to fetch tasks for treatment ${treat.id}:`, err);
                    }
                  }
                } catch (err) {
                  console.warn(`Failed to fetch treatments for incident ${inc.id}:`, err);
                }
              }

              cropsList.push({
                id: planting.id,
                cropName: planting.cropName,
                cropVariety: planting.cropVariety,
                plantingDate: planting.plantingDate,
                expectedHarvestDate: planting.expectedHarvestDate,
                seasonId: cycle.id,
                seasonName: cycle.name,
                seasonStatus: cycle.status,
                farmId: farm.id,
                farmName: farm.name,
                problemCount: openIncidents.length,
                pendingTaskCount: pendingTaskCount,
                status: cycle.status
              });
            } catch (err) {
              console.warn(`Failed to fetch incidents for planting ${planting.id}:`, err);
            }
          }
        } catch (err) {
          console.warn(`Failed to fetch plantings for cycle ${cycle.id}:`, err);
        }
      }
    } catch (err) {
      console.warn(`Failed to fetch crop cycles for farm ${farm.id}:`, err);
    }
  }

  return cropsList;
};

/**
 * Fetches crops list details for a single crop cycle (season).
 * @param {string} seasonId Crop Cycle UUID
 * @returns {Promise<Array>} List of crops with counts
 */
export const fetchSeasonCropsWithDetails = async (seasonId) => {
  const plantingsRes = await getSeasonCrops(seasonId);
  const plantings = plantingsRes.data || [];
  
  const list = [];
  for (const planting of plantings) {
    if (!planting?.id) continue;
    try {
      const incidentsRes = await apiClient.get(`/crop-plantings/${planting.id}/disease-incidents`);
      const incidents = incidentsRes.data?.data || [];
      const openIncidents = incidents.filter(inc => inc.status !== 'CLOSED' && inc.status !== 'RESOLVED');
      
      let pendingTaskCount = 0;
      for (const inc of openIncidents) {
        if (!inc?.id) continue;
        try {
          const treatmentsRes = await apiClient.get(`/disease-incidents/${inc.id}/treatments`);
          const treatments = treatmentsRes.data?.data || [];
          for (const treat of treatments) {
            if (!treat?.id) continue;
            try {
              const tasksRes = await apiClient.get(`/treatments/${treat.id}/tasks`);
              const tasks = tasksRes.data?.data || [];
              const openTasks = tasks.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS');
              pendingTaskCount += openTasks.length;
            } catch (err) {
              console.warn(`Failed to fetch tasks for treatment ${treat.id}:`, err);
            }
          }
        } catch (err) {
          console.warn(`Failed to fetch treatments for incident ${inc.id}:`, err);
        }
      }
      
      list.push({
        ...planting,
        problemCount: openIncidents.length,
        pendingTaskCount: pendingTaskCount
      });
    } catch (err) {
      console.warn(`Failed to resolve details for planting ${planting.id}:`, err);
      list.push({
        ...planting,
        problemCount: 0,
        pendingTaskCount: 0
      });
    }
  }
  return list;
};
