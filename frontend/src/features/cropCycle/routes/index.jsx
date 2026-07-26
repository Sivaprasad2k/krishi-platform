import SeasonsPage from '../pages/SeasonsPage';
import CreateSeasonPage from '../pages/CreateSeasonPage';
import SeasonDetailsPage from '../pages/SeasonDetailsPage';

/**
 * CropCycle (Season) module route configurations.
 */
export const cropCycleRoutes = [
  { path: '/farms/:farmId/seasons', element: <SeasonsPage /> },
  { path: '/farms/:farmId/seasons/new', element: <CreateSeasonPage /> },
  { path: '/seasons/:seasonId', element: <SeasonDetailsPage /> }
];
