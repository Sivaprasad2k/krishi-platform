import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import { getCrop, fetchCropWorkspaceDetails } from './getCrop';

describe('getCrop and fetchCropWorkspaceDetails API Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('getCrop calls /crop-plantings/{id} and returns response data', async () => {
    const mockCropData = { id: 'crop-123', cropName: 'Paddy' };
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockCropData }
    });

    const result = await getCrop('crop-123');

    expect(getSpy).toHaveBeenCalledWith('/crop-plantings/crop-123');
    expect(result).toEqual({ success: true, data: mockCropData });
  });

  it('fetchCropWorkspaceDetails calls /crop-plantings/{id}/workspace and returns unwrapped workspace data', async () => {
    const mockWorkspace = {
      crop: { id: 'crop-123', cropName: 'Paddy', cropVariety: 'Uma' },
      season: { id: 'season-456', name: 'Monsoon 2026' },
      farm: { id: 'farm-789', name: 'Green Valley' },
      farmName: 'Green Valley',
      farmId: 'farm-789',
      tasks: [{ id: 'task-1', title: 'Weeding' }],
      incidents: [],
      openIncidents: [],
      treatments: []
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockWorkspace }
    });

    const result = await fetchCropWorkspaceDetails('crop-123');

    expect(getSpy).toHaveBeenCalledWith('/crop-plantings/crop-123/workspace');
    expect(result).toEqual(mockWorkspace);
  });

  it('fetchCropWorkspaceDetails throws error when data is not returned', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: false, data: null }
    });

    await expect(fetchCropWorkspaceDetails('crop-empty')).rejects.toThrow(
      'Crop workspace details not found'
    );
  });
});
