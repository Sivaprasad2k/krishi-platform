import apiClient from '../../../core/api/axios';

/**
 * Create a new operational task for a crop planting (optionally linked to a treatment plan).
 * @param {Object} data { cropPlantingId, treatmentId, taskType, title, description, dueDate }
 */
export const createTask = async (data) => {
  const response = await apiClient.post('/tasks', data);
  return response.data;
};

/**
 * Fetch all tasks for the authenticated farmer across all crop plantings.
 */
export const fetchAllTasks = async () => {
  const response = await apiClient.get('/tasks');
  return response.data?.data || [];
};

/**
 * Fetch tasks for a specific crop planting.
 * @param {string} cropPlantingId 
 */
export const getTasksByCropPlanting = async (cropPlantingId) => {
  const response = await apiClient.get(`/crop-plantings/${cropPlantingId}/tasks`);
  return response.data?.data || [];
};

/**
 * Fetch tasks for a specific treatment plan.
 * @param {string} treatmentId 
 */
export const getTasksByTreatment = async (treatmentId) => {
  const response = await apiClient.get(`/treatments/${treatmentId}/tasks`);
  return response.data?.data || [];
};

/**
 * Fetch a specific task by ID.
 * @param {string} id 
 */
export const getTaskById = async (id) => {
  const response = await apiClient.get(`/tasks/${id}`);
  return response.data;
};

/**
 * Mark a task as IN_PROGRESS.
 * @param {string} id 
 */
export const startTask = async (id) => {
  const response = await apiClient.post(`/tasks/${id}/start`);
  return response.data;
};

/**
 * Mark a task as COMPLETED.
 * @param {string} id 
 */
export const completeTask = async (id) => {
  const response = await apiClient.post(`/tasks/${id}/complete`);
  return response.data;
};

/**
 * Cancel a task.
 * @param {string} id 
 */
export const cancelTask = async (id) => {
  const response = await apiClient.post(`/tasks/${id}/cancel`);
  return response.data;
};

/**
 * Skip a task with an optional reason.
 * @param {string} id
 * @param {string} [reason]
 */
export const skipTask = async (id, reason) => {
  const payload = reason ? { reason: reason.trim() } : {};
  const response = await apiClient.post(`/tasks/${id}/skip`, payload);
  return response.data;
};

/**
 * Reschedule a task to a new due date.
 * @param {string} id
 * @param {string} dueDate (YYYY-MM-DD)
 */
export const rescheduleTask = async (id, dueDate) => {
  const response = await apiClient.post(`/tasks/${id}/reschedule`, { dueDate });
  return response.data;
};
