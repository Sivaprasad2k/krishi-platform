import DiseaseIncidentsPage from '../pages/DiseaseIncidentsPage';
import DiseaseIncidentDetailPage from '../pages/DiseaseIncidentDetailPage';

export const diseaseIncidentRoutes = [
  {
    path: '/disease-incidents',
    element: <DiseaseIncidentsPage />
  },
  {
    path: '/disease-incidents/:id',
    element: <DiseaseIncidentDetailPage />
  }
];
