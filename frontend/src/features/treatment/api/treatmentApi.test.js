import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import {
  fetchAllTreatments,
  createTreatment,
  getTreatmentsByIncident,
  startTreatment,
  completeTreatment
} from './treatmentApi';

describe('TreatmentApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('fetchAllTreatments calls GET /treatments and unwraps data', async () => {
    const mockTreatments = [
      {
        id: 't-1',
        treatmentName: 'Neem Oil Spray',
        diseaseName: 'Powdery Mildew',
        cropName: 'Tomato',
        farmName: 'Green Farm'
      }
    ];

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockTreatments }
    });

    const result = await fetchAllTreatments();

    expect(getSpy).toHaveBeenCalledWith('/treatments');
    expect(result).toEqual(mockTreatments);
    expect(result.length).toBe(1);
    expect(result[0].diseaseName).toBe('Powdery Mildew');
  });

  it('fetchAllTreatments returns empty array when response has no data', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: null }
    });

    const result = await fetchAllTreatments();

    expect(result).toEqual([]);
  });

  it('startTreatment calls POST /treatments/{id}/start', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: { id: 't-1', status: 'IN_PROGRESS' } }
    });

    const result = await startTreatment('t-1');

    expect(postSpy).toHaveBeenCalledWith('/treatments/t-1/start');
    expect(result.data.status).toBe('IN_PROGRESS');
  });

  it('completeTreatment calls POST /treatments/{id}/complete', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: { id: 't-1', status: 'COMPLETED' } }
    });

    const result = await completeTreatment('t-1');

    expect(postSpy).toHaveBeenCalledWith('/treatments/t-1/complete');
    expect(result.data.status).toBe('COMPLETED');
  });
});
