import apiClient from '../../../core/api/axios';

/**
 * Retrieves counts summary.
 * @returns {Promise<Object>} API Response containing counts
 */
export const getDashboardSummary = async () => {
  const response = await apiClient.get('/dashboard/summary');
  return response.data;
};

/**
 * Retrieves unread notifications.
 * @returns {Promise<Object>} API Response containing unread notifications list
 */
export const getAlerts = async () => {
  const response = await apiClient.get('/notifications/unread');
  return response.data;
};

/**
 * Recursively traverses all entity collections to aggregate:
 * 1. Unresolved Disease Incidents (Crop Problems)
 * 2. Pending and In-Progress Tasks (Work To Do)
 */
export const fetchFarmerOperations = async () => {
  try {
    const farmsRes = await apiClient.get('/farms');
    const farms = farmsRes.data?.data || [];
    
    let allProblems = [];
    let allTasks = [];

    // Traverse each farm
    for (const farm of farms) {
      if (!farm?.id) continue;
      try {
        const cyclesRes = await apiClient.get(`/farms/${farm.id}/crop-cycles`);
        const cycles = cyclesRes.data?.data || [];
        const activeCycles = cycles.filter(c => c?.status === 'ACTIVE');

        // Traverse each active cycle
        for (const cycle of activeCycles) {
          if (!cycle?.id) continue;
          try {
            const plantingsRes = await apiClient.get(`/crop-cycles/${cycle.id}/plantings`);
            const plantings = plantingsRes.data?.data || [];

            // Traverse each planting
            for (const planting of plantings) {
              if (!planting?.id) continue;
              try {
                const incidentsRes = await apiClient.get(`/crop-plantings/${planting.id}/disease-incidents`);
                const incidents = incidentsRes.data?.data || [];
                
                // Only collect unresolved incidents (REPORTED, UNDER_REVIEW, TREATED)
                const unresolvedIncidents = incidents.filter(
                  inc => inc?.status !== 'CLOSED' && inc?.status !== 'RESOLVED'
                );
                
                for (const incident of unresolvedIncidents) {
                  if (!incident?.id) continue;
                  allProblems.push({
                    id: incident.id,
                    title: incident.diseaseName || 'Reported Incident',
                    crop: planting.cropName || cycle.cropName || 'Crop',
                    severity: incident.severity || 'MEDIUM',
                    reportedDate: incident.createdAt ? new Date(incident.createdAt).toLocaleDateString() : 'Recently',
                    status: incident.status,
                    farmName: farm.name || 'Farm'
                  });

                  try {
                    const treatmentsRes = await apiClient.get(`/disease-incidents/${incident.id}/treatments`);
                    const treatments = treatmentsRes.data?.data || [];
                    
                    for (const treatment of treatments) {
                      if (!treatment?.id) continue;
                      try {
                        const tasksRes = await apiClient.get(`/treatments/${treatment.id}/tasks`);
                        const tasks = tasksRes.data?.data || [];
                        
                        // Filter for pending/in-progress tasks
                        const pendingTasks = tasks.filter(
                          t => t?.status === 'PENDING' || t?.status === 'IN_PROGRESS'
                        );

                        for (const task of pendingTasks) {
                          if (!task?.id) continue;
                          allTasks.push({
                            id: task.id,
                            title: task.title,
                            crop: planting.cropName || cycle.cropName || 'Crop',
                            time: task.dueDate ? new Date(task.dueDate).toLocaleDateString() : 'Today',
                            status: task.status,
                            farmName: farm.name || 'Farm'
                          });
                        }
                      } catch (err) {
                        console.warn(`Failed to fetch tasks for treatment ${treatment.id}:`, err);
                      }
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
        console.warn(`Failed to fetch crop cycles for farm ${farm.id}:`, err);
      }
    }

    return {
      problems: allProblems,
      tasks: allTasks
    };
  } catch (error) {
    console.error('Failed to fetch farmer operations:', error);
    throw error;
  }
};

/**
 * Traverses farms, crop cycles, and plantings to build a full workflow visualization mapping:
 * Farm -> Crop -> Disease -> Treatment -> Task
 */
export const fetchFarmerCropsWithWorkflows = async () => {
  try {
    const farmsRes = await apiClient.get('/farms');
    const farms = farmsRes.data?.data || [];
    
    let list = [];

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
              
              let unresolvedIncident = null;
              let activeTreatment = null;
              let pendingTask = null;

              try {
                const incidentsRes = await apiClient.get(`/crop-plantings/${planting.id}/disease-incidents`);
                const incidents = incidentsRes.data?.data || [];
                const openIncidents = incidents.filter(inc => inc?.status !== 'CLOSED' && inc?.status !== 'RESOLVED');
                
                if (openIncidents.length > 0) {
                  unresolvedIncident = openIncidents[0];

                  try {
                    const treatmentsRes = await apiClient.get(`/disease-incidents/${unresolvedIncident.id}/treatments`);
                    const treatments = treatmentsRes.data?.data || [];
                    
                    if (treatments.length > 0) {
                      activeTreatment = treatments[0];

                      try {
                        const tasksRes = await apiClient.get(`/treatments/${activeTreatment.id}/tasks`);
                        const tasks = tasksRes.data?.data || [];
                        const openTasks = tasks.filter(t => t?.status === 'PENDING' || t?.status === 'IN_PROGRESS');
                        
                        if (openTasks.length > 0) {
                          pendingTask = openTasks[0];
                        }
                      } catch (err) {
                        console.warn('Failed to fetch tasks for workflow mapping:', err);
                      }
                    }
                  } catch (err) {
                    console.warn('Failed to fetch treatments for workflow mapping:', err);
                  }
                }
              } catch (err) {
                console.warn('Failed to fetch incidents for workflow mapping:', err);
              }

              list.push({
                id: planting.id,
                plantingName: planting.cropName || cycle.cropName || 'Crop',
                farmName: farm.name || 'Farm',
                seasonName: cycle.name || 'Samba 2026',
                sizeHectares: planting.areaHectares || cycle.areaHectares || farm.sizeHectares || 0,
                status: planting.status || cycle.status || 'ACTIVE',
                workflow: {
                  disease: unresolvedIncident ? unresolvedIncident.diseaseName : null,
                  treatment: activeTreatment ? activeTreatment.name : null,
                  task: pendingTask ? pendingTask.title : null
                }
              });
            }
          } catch (err) {
            console.warn('Failed to fetch plantings for workflow mapping:', err);
          }
        }
      } catch (err) {
        console.warn('Failed to fetch crop cycles for workflow mapping:', err);
      }
    }

    return list;
  } catch (error) {
    console.error('Failed to resolve workflow mappings:', error);
    throw error;
  }
};
