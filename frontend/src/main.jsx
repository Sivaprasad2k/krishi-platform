import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './i18n/index.js'
import './styles/main.css'
import AppRouter from './app/router/AppRouter.jsx'
import { useAuthStore } from './store/authStore'

// Initialize session from local storage cache
useAuthStore.getState().initializeAuth();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppRouter />
  </StrictMode>,
)
