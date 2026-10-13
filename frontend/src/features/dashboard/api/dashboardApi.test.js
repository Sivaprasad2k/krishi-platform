import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import { fetchFarmerOperations } from './dashboardApi';

describe('DashboardApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('fetchFarmerOperations consolidates tasks from all sources, prioritizing overdue work', async () => {
    const yesterday = new Date(Date.now() - 86400000).toISOString();
    const tomorrow = new Date(Date.now() + 86400000).toISOString();
    const nextWeek = new Date(Date.now() + 7 * 86400000).toISOString();

    const mockTasks = [
      {
        id: 'task-plan-1',
        title: 'Stage 1 Irrigation',
        status: 'PENDING',
        source: 'AUTOMATION', // Cultivation plan task
        dueDate: nextWeek,
        cropName: 'Rice',
        farmName: 'North Farm'
      },
      {
        id: 'task-manual-2',
        title: 'Weeding Bed A',
        status: 'PENDING',
        source: 'MANUAL', // Manual task
        dueDate: yesterday, // Overdue!
        cropName: 'Tomato',
        farmName: 'South Farm'
      },
      {
        id: 'task-treat-3',
        title: 'Fungicide Spray',
        status: 'IN_PROGRESS',
        source: 'TREATMENT', // Treatment task
        dueDate: tomorrow,
        cropName: 'Corn',
        farmName: 'North Farm'
      },
      {
        id: 'task-done-4',
        title: 'Old Harvest Task',
        status: 'COMPLETED',
        source: 'MANUAL',
        dueDate: yesterday
      }
    ];

    vi.spyOn(apiClient, 'get').mockImplementation((url) => {
      if (url === '/tasks') {
        return Promise.resolve({ data: { success: true, data: mockTasks } });
      }
      if (url === '/farms') {
        return Promise.resolve({ data: { success: true, data: [] } });
      }
      return Promise.resolve({ data: { success: true, data: [] } });
    });

    const result = await fetchFarmerOperations();

    expect(result).toHaveProperty('tasks');
    expect(result).toHaveProperty('problems');

    // Completed task is excluded from active work
    expect(result.tasks.length).toBe(3);

    // Overdue task (manual-2) is sorted first
    expect(result.tasks[0].id).toBe('task-manual-2');
    expect(result.tasks[0].isOverdue).toBe(true);
    expect(result.tasks[0].source).toBe('MANUAL');

    // Next is tomorrow's treatment task
    expect(result.tasks[1].id).toBe('task-treat-3');
    expect(result.tasks[1].source).toBe('TREATMENT');

    // Cultivation plan task is included
    expect(result.tasks[2].id).toBe('task-plan-1');
    expect(result.tasks[2].source).toBe('AUTOMATION');
    expect(result.tasks[2].cropName).toBe('Rice');
  });
});
