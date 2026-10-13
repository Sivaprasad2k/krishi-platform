import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import {
  createTask,
  fetchAllTasks,
  completeTask,
  cancelTask,
  skipTask,
  rescheduleTask
} from './taskApi';

describe('TaskApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('skipTask calls POST /tasks/{id}/skip with optional reason payload', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: { id: 'task-1', status: 'SKIPPED', skipReason: 'Heavy rain' } }
    });

    const result = await skipTask('task-1', 'Heavy rain');

    expect(postSpy).toHaveBeenCalledWith('/tasks/task-1/skip', { reason: 'Heavy rain' });
    expect(result.data.status).toBe('SKIPPED');
    expect(result.data.skipReason).toBe('Heavy rain');
  });

  it('skipTask calls POST /tasks/{id}/skip without body when reason omitted', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: { id: 'task-2', status: 'SKIPPED' } }
    });

    const result = await skipTask('task-2');

    expect(postSpy).toHaveBeenCalledWith('/tasks/task-2/skip', {});
    expect(result.data.status).toBe('SKIPPED');
  });

  it('rescheduleTask calls POST /tasks/{id}/reschedule with new due date', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: { id: 'task-1', dueDate: '2026-11-01T08:00:00' } }
    });

    const result = await rescheduleTask('task-1', '2026-11-01');

    expect(postSpy).toHaveBeenCalledWith('/tasks/task-1/reschedule', {
      dueDate: '2026-11-01'
    });
    expect(result.data.dueDate).toBe('2026-11-01T08:00:00');
  });
});
