import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { AlertTriangle, Info, Bell, CheckCircle } from 'lucide-react';
import apiClient from '../../core/api/axios';

/**
 * AlertsPage acts as the live notifications panel for the farmer.
 * Permits reading alerts and marking them acknowledged.
 */
const AlertsPage = () => {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewState, setViewState] = useState('loading');
  const [actionLoadingId, setActionLoadingId] = useState(null);

  const loadAlerts = async () => {
    setLoading(true);
    setError(null);
    setViewState('loading');

    try {
      const res = await apiClient.get('/notifications');
      if (res.data?.success && Array.isArray(res.data?.data)) {
        const list = res.data.data;
        setAlerts(list);
        if (list.length === 0) {
          setViewState('empty');
        } else {
          setViewState('success');
        }
      } else {
        throw new Error(res.data?.message || 'Failed to retrieve alerts');
      }
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to load notifications');
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts();
  }, []);

  const handleAcknowledge = async (id) => {
    setActionLoadingId(id);
    try {
      const res = await apiClient.post(`/notifications/${id}/read`);
      if (res.data?.success) {
        await loadAlerts();
      }
    } catch (err) {
      console.error('Failed to acknowledge alert:', err);
    } finally {
      setActionLoadingId(null);
    }
  };

  const getCategoryConfig = (type) => {
    if (!type) return { icon: Info, bg: 'bg-[#F0F7F1]', text: 'text-[#6B7280]', labelKey: 'alerts.categories.general', defaultLabel: 'Info' };
    const clean = type.toUpperCase();
    if (clean.includes('DISEASE') || clean.includes('PROBLEM') || clean.includes('ALERT')) {
      return { icon: AlertTriangle, bg: 'bg-[#FEF2F2]', text: 'text-[#DC2626]', labelKey: 'alerts.categories.problem', defaultLabel: 'Crop Problem' };
    }
    if (clean.includes('TREATMENT')) {
      return { icon: Info, bg: 'bg-blue-50', text: 'text-[#2563EB]', labelKey: 'alerts.categories.treatment', defaultLabel: 'Treatment' };
    }
    if (clean.includes('TASK') || clean.includes('WORK') || clean.includes('REMINDER')) {
      return { icon: CheckCircle, bg: 'bg-blue-50', text: 'text-[#2563EB]', labelKey: 'alerts.categories.work', defaultLabel: 'Work Reminder' };
    }
    if (clean.includes('SEASON') || clean.includes('CYCLE')) {
      return { icon: Info, bg: 'bg-[#E3F2E6]', text: 'text-[#087A55]', labelKey: 'alerts.categories.season', defaultLabel: 'Season' };
    }
    if (clean.includes('HARVEST')) {
      return { icon: CheckCircle, bg: 'bg-[#E3F2E6]', text: 'text-[#16A34A]', labelKey: 'alerts.categories.harvest', defaultLabel: 'Harvest' };
    }
    return { icon: Info, bg: 'bg-[#F0F7F1]', text: 'text-[#6B7280]', labelKey: 'alerts.categories.general', defaultLabel: 'Info' };
  };

  const getTranslatedTitle = (alert) => {
    if (!alert.notificationType) return alert.title || t('alerts.defaultTitle', 'Notification');
    const rawType = alert.notificationType;
    const enumTitleMap = {
      'CROP_CYCLE_STARTED': 'season.events.started',
      'INSPECT_CROP_RECOVERY': 'tasks.types.CROP_INSPECTION',
      'DISEASE_REPORTED': 'diseaseIncident.events.reported',
      'TREATMENT_PLANNED': 'treatment.events.planned'
    };
    if (enumTitleMap[rawType]) {
      return t(enumTitleMap[rawType], alert.title);
    }
    const key = `notifications.types.${rawType}.title`;
    return t(key, alert.title);
  };

  const formatPresetTitle = (text) => {
    if (!text) return text;
    const defaultTitleMap = {
      'Water Crop': 'tasks.types.IRRIGATION',
      'Apply Fertilizer': 'tasks.types.FERTILIZER_APPLICATION',
      'Add Fertilizer': 'tasks.types.FERTILIZER_APPLICATION',
      'Add Manure / Compost': 'tasks.types.MANURE_APPLICATION',
      'Add Manure': 'tasks.types.MANURE_APPLICATION',
      'Remove Weeds': 'tasks.types.WEEDING',
      'Pest Control Operation': 'tasks.types.PEST_CONTROL',
      'Pest Control': 'tasks.types.PEST_CONTROL',
      'Inspect Crop Health': 'tasks.types.CROP_INSPECTION',
      'Prune Plants': 'tasks.types.PRUNING',
      'Harvest Crop': 'tasks.types.HARVESTING',
      'Disease Treatment': 'tasks.types.DISEASE_TREATMENT',
      'Field Work': 'tasks.types.OTHER'
    };

    let result = text;
    Object.entries(defaultTitleMap).forEach(([preset, key]) => {
      if (result.includes(preset)) {
        result = result.replaceAll(preset, t(key, preset));
      }
    });
    return result;
  };

  const getTranslatedMessage = (alert) => {
    if (!alert.message) return '';
    let parsed;
    try {
      parsed = JSON.parse(alert.message);
      if (parsed && parsed.title) {
        parsed.title = formatPresetTitle(parsed.title);
      }
      if (parsed && parsed.taskTitle) {
        parsed.taskTitle = formatPresetTitle(parsed.taskTitle);
      }
    } catch (e) {
      parsed = null;
    }
    const key = alert.notificationType ? `notifications.types.${alert.notificationType}.message` : null;
    let msg = alert.message;
    if (key) {
      if (parsed) {
        msg = t(key, { ...parsed, defaultValue: alert.message });
      } else {
        msg = t(key, { defaultValue: alert.message });
      }
    }
    return formatPresetTitle(msg);
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Page Header */}
      <div className="rounded-2xl bg-white p-6 shadow-2xs border border-[#C9DCCB] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[#222222] flex items-center space-x-2.5">
            <Bell className="h-7 w-7 text-[#087A55]" />
            <span>{t('nav.alerts', 'Alerts & Notifications')}</span>
          </h1>
          <p className="text-sm font-semibold text-[#525252] mt-1">
            {t('states.emptyAlertsDesc', "Stay informed about field activities, crop health, and scheduled work.")}
          </p>
        </div>
      </div>

      <StateView
        state={viewState}
        onRetry={loadAlerts}
        emptyTitle={t('states.emptyAlerts', 'No new alerts')}
        emptyMessage={t('states.emptyAlertsDesc', "You're up to date")}
        emptyIcon={Bell}
      >
        <div className="space-y-4">
          {alerts.map((alert) => {
            const config = getCategoryConfig(alert.notificationType);
            const CategoryIcon = config.icon;

            return (
              <div
                key={alert.id}
                className={`rounded-2xl p-5 border shadow-2xs transition-all flex items-start space-x-4 ${
                  alert.isRead
                    ? 'bg-white border-[#C9DCCB] opacity-75'
                    : 'bg-[#DCFCE7]/30 border-[#C9DCCB] border-l-4 border-l-[#087A55] shadow-xs'
                }`}
              >
                <div className={`rounded-xl p-3 ${config.bg} ${config.text} mt-0.5 shrink-0`}>
                  <CategoryIcon className="h-6 w-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <div className="flex items-center space-x-2 min-w-0">
                      <h3 className="text-base font-black text-[#222222] truncate">{getTranslatedTitle(alert)}</h3>
                      {!alert.isRead && (
                        <span className="h-2.5 w-2.5 rounded-full bg-[#087A55] shrink-0" title="Unread" />
                      )}
                    </div>
                    <span className="text-xs font-bold text-[#6B7280] shrink-0">
                      {alert.createdAt ? new Date(alert.createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' }) : ''}
                    </span>
                  </div>

                  <p className="text-sm text-[#525252] mt-2 font-semibold leading-relaxed">
                    {getTranslatedMessage(alert)}
                  </p>
                  
                  {!alert.isRead && (
                    <div className="mt-4 flex items-center justify-end border-t border-[#C9DCCB] pt-3">
                      <button
                        onClick={() => handleAcknowledge(alert.id)}
                        disabled={actionLoadingId === alert.id}
                        style={{ minHeight: '44px' }}
                        className="px-4 py-2 rounded-xl bg-[#087A55] hover:bg-[#066747] text-white text-xs font-black active:scale-95 disabled:opacity-50 transition-all cursor-pointer flex items-center space-x-1.5 shadow-2xs"
                      >
                        <CheckCircle className="h-4 w-4" />
                        <span>{t('alerts.actions.acknowledge', 'Acknowledge')}</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </StateView>
    </div>
  );
};

export default AlertsPage;
