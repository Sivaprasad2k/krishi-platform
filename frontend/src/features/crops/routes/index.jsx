import React from 'react';
import CropsPage from '../pages/CropsPage';
import CropDetailsPage from '../pages/CropDetailsPage';
import CreateCropPage from '../pages/CreateCropPage';
import EditCropPage from '../pages/EditCropPage';

/**
 * Crop module route configuration array.
 */
export const cropsRoutes = [
  { path: '/crops', element: <CropsPage /> },
  { path: '/crops/:id', element: <CropDetailsPage /> },
  { path: '/crop-cycles/:id/crops/new', element: <CreateCropPage /> },
  { path: '/crops/:id/edit', element: <EditCropPage /> }
];
