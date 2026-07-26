import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './i18n/index.js'
import './styles/main.css'
import AppRouter from './app/router/AppRouter.jsx'
import { useAuthStore } from './store/authStore'
import ErrorBoundary from './shared/components/ErrorBoundary.jsx'

// Initialize session from local storage cache
useAuthStore.getState().initializeAuth();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
      <AppRouter />
    </ErrorBoundary>
  </StrictMode>,
)
