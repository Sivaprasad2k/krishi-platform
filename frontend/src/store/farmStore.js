import { create } from 'zustand';
import { getFarms } from '../features/farm/api/getFarms';
import { getFarm } from '../features/farm/api/getFarm';
import { createFarm } from '../features/farm/api/createFarm';
import { updateFarm } from '../features/farm/api/updateFarm';

export const useFarmStore = create((set, get) => ({
  farms: [],
  selectedFarm: null,
  loading: false,
  error: null,

  fetchFarms: async () => {
    set({ loading: true, error: null });
    try {
      const response = await getFarms();
      if (response.success) {
        set({ farms: response.data || [], loading: false });
        return { success: true };
      }
      throw new Error(response.message || 'Failed to fetch farms');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to fetch farms' });
      return { success: false, error: err.message };
    }
  },

  fetchFarm: async (farmId) => {
    set({ loading: true, error: null });
    try {
      const response = await getFarm(farmId);
      if (response.success) {
        set({ selectedFarm: response.data, loading: false });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to fetch farm details');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to fetch farm details' });
      return { success: false, error: err.message };
    }
  },

  createFarm: async (farmData) => {
    set({ loading: true, error: null });
    try {
      const response = await createFarm(farmData);
      if (response.success) {
        // Refresh farms list
        const freshFarmsResponse = await getFarms();
        set({ 
          farms: freshFarmsResponse.success ? (freshFarmsResponse.data || []) : [...get().farms, response.data],
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to create farm');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to create farm' });
      return { success: false, error: err.message };
    }
  },

  updateFarm: async (farmId, farmData) => {
    set({ loading: true, error: null });
    try {
      const response = await updateFarm(farmId, farmData);
      if (response.success) {
        // Refresh farms list
        const freshFarmsResponse = await getFarms();
        set({ 
          farms: freshFarmsResponse.success ? (freshFarmsResponse.data || []) : get().farms.map(f => f.id === farmId ? response.data : f),
          selectedFarm: response.data,
          loading: false 
        });
        return { success: true, data: response.data };
      }
      throw new Error(response.message || 'Failed to update farm');
    } catch (err) {
      set({ loading: false, error: err.message || 'Failed to update farm' });
      return { success: false, error: err.message };
    }
  },

  clearErrors: () => set({ error: null })
}));
