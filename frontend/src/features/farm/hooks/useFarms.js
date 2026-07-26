import { useFarmStore } from '../../../store/farmStore';

/**
 * Custom hook to interface with the Farm Store.
 */
export const useFarms = () => {
  const {
    farms,
    selectedFarm,
    loading,
    error,
    fetchFarms,
    fetchFarm,
    createFarm,
    updateFarm,
    clearErrors
  } = useFarmStore();

  return {
    farms,
    selectedFarm,
    loading,
    error,
    fetchFarms,
    fetchFarm,
    createFarm,
    updateFarm,
    clearErrors
  };
};
