import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import {
  evaluateAdvisories,
  fetchAdvisories,
  acknowledgeAdvisory,
  resolveAdvisory
} from './advisoryApi';

describe('AdvisoryApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('evaluateAdvisories calls POST /crop-plantings/{plantingId}/advisories/evaluate', async () => {
    const mockSummary = {
      cropPlantingId: 'planting-1',
      activeAdvisoriesCount: 2,
      activeAdvisories: []
    };
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: mockSummary }
    });

    const result = await evaluateAdvisories('planting-1');

    expect(postSpy).toHaveBeenCalledWith('/crop-plantings/planting-1/advisories/evaluate');
    expect(result).toEqual(mockSummary);
  });

  it('fetchAdvisories calls GET /crop-plantings/{plantingId}/advisories without status when not provided', async () => {
    const mockList = [{ id: 'adv-1', title: 'Rain warning' }];
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockList }
    });

    const result = await fetchAdvisories('planting-1');

    expect(getSpy).toHaveBeenCalledWith('/crop-plantings/planting-1/advisories', {
      params: {}
    });
    expect(result).toEqual(mockList);
  });

  it('fetchAdvisories calls GET /crop-plantings/{plantingId}/advisories with status filter', async () => {
    const mockList = [{ id: 'adv-1', status: 'ACTIVE' }];
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockList }
    });

    const result = await fetchAdvisories('planting-1', 'ACTIVE');

    expect(getSpy).toHaveBeenCalledWith('/crop-plantings/planting-1/advisories', {
      params: { status: 'ACTIVE' }
    });
    expect(result).toEqual(mockList);
  });

  it('acknowledgeAdvisory calls PATCH /advisories/{id}/acknowledge', async () => {
    const mockAdv = { id: 'adv-1', status: 'ACKNOWLEDGED' };
    const patchSpy = vi.spyOn(apiClient, 'patch').mockResolvedValueOnce({
      data: { success: true, data: mockAdv }
    });

    const result = await acknowledgeAdvisory('adv-1');

    expect(patchSpy).toHaveBeenCalledWith('/advisories/adv-1/acknowledge');
    expect(result).toEqual(mockAdv);
  });

  it('resolveAdvisory calls PATCH /advisories/{id}/resolve', async () => {
    const mockAdv = { id: 'adv-1', status: 'RESOLVED' };
    const patchSpy = vi.spyOn(apiClient, 'patch').mockResolvedValueOnce({
      data: { success: true, data: mockAdv }
    });

    const result = await resolveAdvisory('adv-1');

    expect(patchSpy).toHaveBeenCalledWith('/advisories/adv-1/resolve');
    expect(result).toEqual(mockAdv);
  });
});
