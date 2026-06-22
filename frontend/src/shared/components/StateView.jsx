import { Loader2, Inbox, AlertTriangle, WifiOff } from 'lucide-react';
import { useTranslation } from 'react-i18next';

/**
 * StateView is a reusable container component that handles:
 * - Loading states (with animated spinner and farmer-friendly text)
 * - Empty states (clean illustrative cards)
 * - Error states (with a visual alert and retry mechanism)
 * - Offline states (with visual network warnings)
 * - Success/Normal states (renders children)
 */
const StateView = ({
  state = 'success',
  onRetry,
  emptyMessage,
  errorMessage,
  loadingMessage,
  children
}) => {
  const { t } = useTranslation();

  const minTouchTargetStyle = { minHeight: '48px' };

  switch (state) {
    case 'loading':
      return (
        <div className="flex min-h-[300px] w-full flex-col items-center justify-center p-8 text-center animate-fade-in">
          <Loader2 className="h-12 w-12 animate-spin text-emerald-600 mb-4" />
          <p className="text-lg font-semibold text-gray-700">
            {loadingMessage || t('states.loading', 'Loading data, please wait...')}
          </p>
        </div>
      );

    case 'empty':
      return (
        <div className="flex min-h-[300px] w-full flex-col items-center justify-center rounded-2xl border-2 border-dashed border-gray-200 bg-white p-8 text-center animate-fade-in">
          <div className="rounded-full bg-emerald-50 p-4 text-emerald-600 mb-4">
            <Inbox className="h-10 w-10" />
          </div>
          <h3 className="text-lg font-bold text-gray-800 mb-2">
            {t('states.noItems', 'No information found')}
          </h3>
          <p className="text-base text-gray-500 max-w-sm mb-6">
            {emptyMessage || t('states.emptyDescription', 'There is no work or data recorded here at the moment.')}
          </p>
          {onRetry && (
            <button
              onClick={onRetry}
              style={minTouchTargetStyle}
              className="flex items-center justify-center rounded-xl bg-emerald-600 px-6 text-base font-bold text-white shadow-sm hover:bg-emerald-700 active:scale-95 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
            >
              {t('states.refresh', 'Refresh')}
            </button>
          )}
        </div>
      );

    case 'error':
      return (
        <div className="flex min-h-[300px] w-full flex-col items-center justify-center rounded-2xl border border-red-150 bg-red-50 p-8 text-center animate-fade-in">
          <div className="rounded-full bg-red-100 p-4 text-red-600 mb-4">
            <AlertTriangle className="h-10 w-10" />
          </div>
          <h3 className="text-lg font-bold text-red-800 mb-2">
            {t('states.errorOccurred', 'Something went wrong')}
          </h3>
          <p className="text-base text-red-600 max-w-sm mb-6">
            {errorMessage || t('states.errorDescription', 'Could not fetch information from the server. Please check your network and try again.')}
          </p>
          {onRetry && (
            <button
              onClick={onRetry}
              style={minTouchTargetStyle}
              className="flex items-center justify-center rounded-xl bg-red-600 px-6 text-base font-bold text-white shadow-sm hover:bg-red-700 active:scale-95 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
            >
              {t('states.tryAgain', 'Try Again')}
            </button>
          )}
        </div>
      );

    case 'offline':
      return (
        <div className="flex min-h-[300px] w-full flex-col items-center justify-center rounded-2xl border border-amber-150 bg-amber-50 p-8 text-center animate-fade-in">
          <div className="rounded-full bg-amber-100 p-4 text-amber-600 mb-4">
            <WifiOff className="h-10 w-10" />
          </div>
          <h3 className="text-lg font-bold text-amber-800 mb-2">
            {t('states.offlineTitle', 'No Internet Connection')}
          </h3>
          <p className="text-base text-amber-600 max-w-sm mb-6">
            {t('states.offlineDescription', 'You are currently offline. Krishi will sync details once you reconnect.')}
          </p>
          {onRetry && (
            <button
              onClick={onRetry}
              style={minTouchTargetStyle}
              className="flex items-center justify-center rounded-xl bg-amber-600 px-6 text-base font-bold text-white shadow-sm hover:bg-amber-700 active:scale-95 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
            >
              {t('states.reconnect', 'Check Connection')}
            </button>
          )}
        </div>
      );

    case 'success':
    default:
      return <div className="w-full h-full animate-fade-in">{children}</div>;
  }
};

export default StateView;
