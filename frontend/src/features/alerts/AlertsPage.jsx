import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { AlertTriangle, CloudRain, Info } from 'lucide-react';

const AlertsPage = () => {
  const { t } = useTranslation();
  const [viewState, setViewState] = useState('success');

  // Mock data for alerts
  const mockAlerts = [
    { id: 1, type: 'warning', title: 'Pest Outbreak Warning', message: 'Downy Mildew reported in farms 2 km away. Inspect tomato crops daily.', date: '2 hours ago' },
    { id: 2, type: 'weather', title: 'Heavy Rainfall Expected', message: 'Weather alerts forecast heavy rain tomorrow afternoon. Ensure field drainage channels are clear.', date: '5 hours ago' },
    { id: 3, type: 'info', title: 'Season Setup Complete', message: 'You have successfully set up your Samba 2026 growing season.', date: '1 day ago' }
  ];

  const getIcon = (type) => {
    switch (type) {
      case 'warning':
        return <AlertTriangle className="h-6 w-6 text-red-650" />;
      case 'weather':
        return <CloudRain className="h-6 w-6 text-blue-650" />;
      case 'info':
      default:
        return <Info className="h-6 w-6 text-emerald-650" />;
    }
  };

  const getBadgeColor = (type) => {
    switch (type) {
      case 'warning':
        return 'bg-red-50 border border-red-150';
      case 'weather':
        return 'bg-blue-50 border border-blue-150';
      case 'info':
      default:
        return 'bg-emerald-50 border border-emerald-150';
    }
  };

  return (
    <div className="space-y-6">
      {/* Dev State Controller */}
      <div className="rounded-xl border border-dashed border-emerald-300 bg-emerald-50/50 p-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <span className="text-sm font-bold text-emerald-800 uppercase tracking-wider">
            {t('dashboard.stateSelector', 'Review States (Dev Mode)')}
          </span>
          <div className="flex flex-wrap gap-2">
            {['success', 'loading', 'empty', 'error', 'offline'].map((stateName) => (
              <button
                key={stateName}
                onClick={() => setViewState(stateName)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all min-h-[36px] uppercase ${
                  viewState === stateName
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-white text-gray-600 border border-gray-250 hover:bg-gray-50'
                }`}
              >
                {stateName}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="rounded-xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800">{t('nav.alerts', 'Alerts')}</h1>
        <p className="text-base text-gray-500 mt-1">
          {t('states.emptyAlerts', 'No alerts at this moment.')}
        </p>
      </div>

      <StateView
        state={viewState}
        onRetry={() => setViewState('success')}
        emptyMessage={t('states.emptyAlerts', 'No alerts at this moment.')}
      >
        <div className="space-y-4">
          {mockAlerts.map((alert) => (
            <div
              key={alert.id}
              className={`rounded-2xl p-5 shadow-sm transition-all flex items-start space-x-3.5 border hover:shadow-md cursor-pointer bg-white`}
            >
              <div className={`rounded-xl p-3 ${getBadgeColor(alert.type)} mt-0.5`}>
                {getIcon(alert.type)}
              </div>
              <div className="flex-1">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
                  <h3 className="text-lg font-bold text-gray-850">{alert.title}</h3>
                  <span className="text-sm font-semibold text-gray-400">{alert.date}</span>
                </div>
                <p className="text-base text-gray-650 mt-1.5 leading-relaxed">{alert.message}</p>
                <div className="mt-4 flex items-center justify-end h-10">
                  <button
                    style={{ minHeight: '40px' }}
                    className="text-base font-bold text-emerald-700 hover:text-emerald-800 focus:underline"
                  >
                    Acknowledge
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </StateView>
    </div>
  );
};

export default AlertsPage;
