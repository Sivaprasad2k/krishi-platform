import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import MainLayout from '../layouts/MainLayout';
import LoginPage from '../../features/auth/LoginPage';
import RegisterPage from '../../features/auth/RegisterPage';
import DashboardPage from '../../features/dashboard/DashboardPage';
import CropsPage from '../../features/crops/CropsPage';
import TasksPage from '../../features/tasks/TasksPage';
import AlertsPage from '../../features/alerts/AlertsPage';
import ProfilePage from '../../features/profile/ProfilePage';
import ProtectedRoute from '../../core/auth/ProtectedRoute';

/**
 * AppRouter handles navigation matching.
 * Redirects fallback requests directly to the authenticated Dashboard.
 */
const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public authentication paths */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        {/* Protected operational paths */}
        <Route
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/crops" element={<CropsPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        {/* Fallbacks */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
