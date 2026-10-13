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
 * Consolidates farmer operational priorities for the Home dashboard:
 * 1. Actionable Tasks (Cultivation Plans, Manual work, Treatment-related work) via GET /tasks
 * 2. Unresolved Disease Incidents (Crop Problems)
 */
export const fetchFarmerOperations = async () => {
  try {
    let allTasks = [];
    let allProblems = [];

    // 1. Fetch all actionable tasks directly via authenticated backend GET /tasks
    try {
      const tasksRes = await apiClient.get('/tasks');
      const rawTasks = tasksRes.data?.data || [];

      const today = new Date();
      today.setHours(0, 0, 0, 0);

      const activeTasks = rawTasks.filter(
        t => t?.status !== 'COMPLETED' && t?.status !== 'CANCELLED' && t?.status !== 'SKIPPED'
      );

      allTasks = activeTasks.map(task => {
        let isOverdue = task.status === 'OVERDUE';
        if (task.dueDate && !isOverdue) {
          const d = new Date(task.dueDate);
          d.setHours(23, 59, 59, 999);
          if (d < today) {
            isOverdue = true;
          }
        }

        return {
          id: task.id,
          title: task.title,
          crop: task.cropName || 'Crop',
          cropName: task.cropName || 'Crop',
          cropVariety: task.cropVariety || '',
          time: task.dueDate ? new Date(task.dueDate).toLocaleDateString() : 'Today',
          status: task.status,
          isOverdue: isOverdue,
          dueDate: task.dueDate,
          taskType: task.taskType,
          source: task.source,
          farmName: task.farmName || 'Farm',
          farmId: task.farmId
        };
      });

      // Sort by urgency: overdue first, then by earliest due date
      allTasks.sort((a, b) => {
        if (a.isOverdue && !b.isOverdue) return -1;
        if (!a.isOverdue && b.isOverdue) return 1;
        if (a.dueDate && b.dueDate) return new Date(a.dueDate) - new Date(b.dueDate);
        if (a.dueDate && !b.dueDate) return -1;
        if (!a.dueDate && b.dueDate) return 1;
        return 0;
      });
    } catch (taskErr) {
      console.warn('Failed to fetch tasks for dashboard operations:', taskErr);
    }

    // 2. Fetch unresolved disease incidents
    try {
      const farmsRes = await apiClient.get('/farms');
      const farms = farmsRes.data?.data || [];

      for (const farm of farms) {
        if (!farm?.id) continue;
        try {
          const cyclesRes = await apiClient.get(`/farms/${farm.id}/crop-cycles`);
          const cycles = cyclesRes.data?.data || [];
          const activeCycles = cycles.filter(c => c?.status === 'ACTIVE' || c?.status === 'DRAFT');

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

                  const unresolvedIncidents = incidents.filter(
                    inc => inc?.status !== 'CLOSED' && inc?.status !== 'RESOLVED'
                  );

                  for (const incident of unresolvedIncidents) {
                    if (!incident?.id) continue;
                    allProblems.push({
                      id: incident.id,
                      title: incident.diseaseName || 'Reported Incident',
                      crop: planting.cropName || cycle.cropName || 'Crop',
                      cropVariety: planting.cropVariety || '',
                      severity: incident.severity || 'MEDIUM',
                      reportedDate: incident.createdAt ? new Date(incident.createdAt).toLocaleDateString() : 'Recently',
                      status: incident.status,
                      farmName: farm.name || 'Farm',
                      farmId: farm.id
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
          console.warn(`Failed to fetch crop cycles for farm ${farm.id}:`, err);
        }
      }
    } catch (farmErr) {
      console.warn('Failed to fetch farms for incident operations:', farmErr);
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
                farmId: farm.id,
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
