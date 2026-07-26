import TreatmentsPage from '../pages/TreatmentsPage';
import TreatmentDetailPage from '../pages/TreatmentDetailPage';

export const treatmentRoutes = [
  {
    path: '/treatments',
    element: <TreatmentsPage />
  },
  {
    path: '/treatments/:id',
    element: <TreatmentDetailPage />
  }
];
