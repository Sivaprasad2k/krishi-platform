import FarmsPage from '../pages/FarmsPage';
import CreateFarmPage from '../pages/CreateFarmPage';
import FarmDetailsPage from '../pages/FarmDetailsPage';
import EditFarmPage from '../pages/EditFarmPage';

/**
 * Farm module route configurations.
 */
export const farmRoutes = [
  { path: '/farms', element: <FarmsPage /> },
  { path: '/farms/new', element: <CreateFarmPage /> },
  { path: '/farms/:farmId', element: <FarmDetailsPage /> },
  { path: '/farms/:farmId/edit', element: <EditFarmPage /> }
];
