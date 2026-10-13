import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import {
  fetchPublishedTemplates,
  fetchTemplateById,
  generateCultivationPlan,
  fetchCultivationPlan
} from './cultivationPlanApi';

describe('CultivationPlanApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('fetchPublishedTemplates calls GET /cultivation-templates with query parameters', async () => {
    const mockTemplates = [
      { id: 'tpl-1', cropName: 'Rice (Paddy)', sourceVersion: 'v2024.1' }
    ];
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockTemplates }
    });

    const result = await fetchPublishedTemplates('Rice');

    expect(getSpy).toHaveBeenCalledWith('/cultivation-templates', {
      params: { status: 'PUBLISHED', cropName: 'Rice' }
    });
    expect(result).toEqual(mockTemplates);
  });

  it('fetchTemplateById calls GET /cultivation-templates/{id}', async () => {
    const mockTemplate = { id: 'tpl-1', cropName: 'Rice (Paddy)', stages: [] };
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockTemplate }
    });

    const result = await fetchTemplateById('tpl-1');

    expect(getSpy).toHaveBeenCalledWith('/cultivation-templates/tpl-1');
    expect(result).toEqual(mockTemplate);
  });

  it('generateCultivationPlan calls POST /crop-plantings/{plantingId}/cultivation-plan/generate', async () => {
    const mockPlan = { id: 'plan-1', templateName: 'Rice Protocol', status: 'ACTIVE' };
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: { success: true, data: mockPlan }
    });

    const result = await generateCultivationPlan('planting-123', 'tpl-1');

    expect(postSpy).toHaveBeenCalledWith(
      '/crop-plantings/planting-123/cultivation-plan/generate',
      { templateId: 'tpl-1' }
    );
    expect(result).toEqual(mockPlan);
  });

  it('fetchCultivationPlan calls GET /crop-plantings/{plantingId}/cultivation-plan', async () => {
    const mockPlan = { id: 'plan-1', templateName: 'Rice Protocol', status: 'ACTIVE' };
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: { success: true, data: mockPlan }
    });

    const result = await fetchCultivationPlan('planting-123');

    expect(getSpy).toHaveBeenCalledWith('/crop-plantings/planting-123/cultivation-plan');
    expect(result).toEqual(mockPlan);
  });
});
