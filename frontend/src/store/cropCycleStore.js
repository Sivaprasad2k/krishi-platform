import { create } from 'zustand';
import { getFarmCropCycles } from '../features/cropCycle/api/getFarmCropCycles';
import { getCropCycle } from '../features/cropCycle/api/getCropCycle';
import { createCropCycle } from '../features/cropCycle/api/createCropCycle';
import { startCropCycle } from '../features/cropCycle/api/startCropCycle';
import { completeCropCycle } from '../features/cropCycle/api/completeCropCycle';
import { cancelCropCycle } from '../features/cropCycle/api/cancelCropCycle';

export const useCropCycleStore = create((set, get) => ({
  seasons: [],
  selectedSeason: null,
  loading: false,
  error: null,

  fetchFarmSeasons: async (farmId) => {
    set({ loading: true, error: null });
    try {
      const response = await getFarmCropCycles(farmId);
      if (response.success) {
        set({ seasons: response.data || [], loading: false });
        return { success: true };
      }
      throw new Error(response.message || 'Failed to fetch seasons');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to fetch seasons' });
      return { success: false, error: err.message };
    }
  },

  fetchSeason: async (seasonId) => {
    set({ loading: true, error: null });
    try {
      const response = await getCropCycle(seasonId);
      if (response.success) {
        set({ selectedSeason: response.data, loading: false });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to fetch season details');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to fetch season details' });
      return { success: false, error: err.message };
    }
  },

  createSeason: async (seasonData) => {
    set({ loading: true, error: null });
    try {
      const response = await createCropCycle(seasonData);
      if (response.success) {
        // Refresh list
        const freshResponse = await getFarmCropCycles(seasonData.farmId);
        set({ 
          seasons: freshResponse.success ? (freshResponse.data || []) : [...get().seasons, response.data],
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to create season');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to create season' });
      return { success: false, error: err.message };
    }
  },

  startSeason: async (seasonId) => {
    set({ loading: true, error: null });
    try {
      const response = await startCropCycle(seasonId);
      if (response.success) {
        set({ 
          selectedSeason: response.data,
          seasons: get().seasons.map(s => s.id === seasonId ? response.data : s),
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to start season');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to start season' });
      return { success: false, error: err.message };
    }
  },

  completeSeason: async (seasonId) => {
    set({ loading: true, error: null });
    try {
      const response = await completeCropCycle(seasonId);
      if (response.success) {
        set({ 
          selectedSeason: response.data,
          seasons: get().seasons.map(s => s.id === seasonId ? response.data : s),
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to complete season');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to complete season' });
      return { success: false, error: err.message };
    }
  },

  cancelSeason: async (seasonId) => {
    set({ loading: true, error: null });
    try {
      const response = await cancelCropCycle(seasonId);
      if (response.success) {
        set({ 
          selectedSeason: response.data,
          seasons: get().seasons.map(s => s.id === seasonId ? response.data : s),
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to cancel season');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to cancel season' });
      return { success: false, error: err.message };
    }
  },

  clearErrors: () => set({ error: null })
}));
