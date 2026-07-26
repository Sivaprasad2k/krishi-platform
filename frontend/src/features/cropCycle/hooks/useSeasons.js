import { useCropCycleStore } from '../../../store/cropCycleStore';

/**
 * Custom hook to interface with the CropCycle (Season) store.
 */
export const useSeasons = () => {
  const {
    seasons,
    selectedSeason,
    loading,
    error,
    fetchFarmSeasons,
    fetchSeason,
    createSeason,
    startSeason,
    completeSeason,
    cancelSeason,
    clearErrors
  } = useCropCycleStore();

  return {
    seasons,
    selectedSeason,
    loading,
    error,
    fetchFarmSeasons,
    fetchSeason,
    createSeason,
    startSeason,
    completeSeason,
    cancelSeason,
    clearErrors
  };
};
