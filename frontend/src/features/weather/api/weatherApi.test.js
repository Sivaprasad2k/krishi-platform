import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '../../../core/api/axios';
import {
  getFarmWeather,
  refreshFarmWeather,
  getFarmAdvisories,
  acknowledgeAdvisory,
  getTaskAdvisories
} from './weatherApi';

describe('WeatherApi Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('getFarmWeather calls GET /farms/{farmId}/weather', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          farmId: 'farm-1',
          coordinateSource: 'FARM_GPS',
          stale: false,
          current: { temperatureCelsius: 28 }
        }
      }
    });

    const result = await getFarmWeather('farm-1');

    expect(getSpy).toHaveBeenCalledWith('/farms/farm-1/weather');
    expect(result.success).toBe(true);
    expect(result.data.coordinateSource).toBe('FARM_GPS');
  });

  it('refreshFarmWeather calls POST /farms/{farmId}/weather/refresh', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          farmId: 'farm-1',
          stale: false,
          current: { temperatureCelsius: 29 }
        }
      }
    });

    const result = await refreshFarmWeather('farm-1');

    expect(postSpy).toHaveBeenCalledWith('/farms/farm-1/weather/refresh');
    expect(result.data.current.temperatureCelsius).toBe(29);
  });

  it('getFarmAdvisories calls GET /farms/{farmId}/advisories', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: [
          { id: 'adv-1', ruleCode: 'HEAVY_RAIN_IRRIGATION', severity: 'MEDIUM' }
        ]
      }
    });

    const result = await getFarmAdvisories('farm-1');

    expect(getSpy).toHaveBeenCalledWith('/farms/farm-1/advisories');
    expect(result.data.length).toBe(1);
    expect(result.data[0].ruleCode).toBe('HEAVY_RAIN_IRRIGATION');
  });

  it('acknowledgeAdvisory calls POST /advisories/{id}/acknowledge', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 'adv-1', status: 'ACKNOWLEDGED' }
      }
    });

    const result = await acknowledgeAdvisory('adv-1');

    expect(postSpy).toHaveBeenCalledWith('/advisories/adv-1/acknowledge');
    expect(result.data.status).toBe('ACKNOWLEDGED');
  });

  it('getTaskAdvisories calls GET /tasks/{id}/advisories', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: [
          { id: 'adv-2', ruleCode: 'HIGH_WIND_SPRAYING', severity: 'HIGH' }
        ]
      }
    });

    const result = await getTaskAdvisories('task-99');

    expect(getSpy).toHaveBeenCalledWith('/tasks/task-99/advisories');
    expect(result.data.length).toBe(1);
    expect(result.data[0].ruleCode).toBe('HIGH_WIND_SPRAYING');
  });
});
