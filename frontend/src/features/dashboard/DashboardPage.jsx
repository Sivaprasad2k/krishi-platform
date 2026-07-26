import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import StateView from '../../shared/components/StateView';
import {
  CheckSquare,
  AlertTriangle,
  AlertCircle,
  PlusCircle,
  Calendar,
  Sprout,
  ArrowRight,
  Bell,
  Clock,
  Briefcase,
  Users,
  Compass,
  MapPin,
  RefreshCw,
  X
} from 'lucide-react';
import { getDashboardSummary, getAlerts, fetchFarmerOperations } from './api/dashboardApi';
import { fetchCropsList } from '../crops/api/getSeasonCrops';
import apiClient from '../../core/api/axios';

/**
 * Redesigned Operational Dashboard for Krishi.
 * Follows the visual workflow priorities and farmer-first layout.
 */
const DashboardPage = () => {
  const { user } = useAuthStore();
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewState, setViewState] = useState('loading');

  // Stats summary counts
  const [summary, setSummary] = useState({
    farmCount: 0,
    activeCropCycles: 0,
    pendingTasks: 0,
    openProblems: 0,
    unreadNotifications: 0
  });

  const [tasks, setTasks] = useState([]);
  const [problems, setProblems] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [crops, setCrops] = useState([]);
  const [upcomingHarvests, setUpcomingHarvests] = useState([]);
  
  // Custom dialog/toast overlay for future feature quick actions
  const [toastText, setToastText] = useState('');

  const loadDashboardData = async () => {
    setLoading(true);
    setError(null);
    setViewState('loading');

    try {
      // 1. Fetch count metrics
      const summaryRes = await getDashboardSummary();
      let activeSummary = {
        farmCount: 0,
        activeCropCycles: 0,
        pendingTasks: 0,
        openProblems: 0,
        unreadNotifications: 0
      };
      if (summaryRes.success && summaryRes.data) {
        activeSummary = summaryRes.data;
        setSummary(activeSummary);
      }

      // 2. Fetch unread alerts
      const alertsRes = await getAlerts();
      let activeAlerts = [];
      if (alertsRes.success && alertsRes.data) {
        activeAlerts = alertsRes.data;
        setAlerts(activeAlerts);
      }

      // 3. Traverse recursively
      const operations = await fetchFarmerOperations();
      setTasks(operations.tasks || []);
      setProblems(operations.problems || []);

      // 4. Fetch crops list for alerts and stats
      const cropsList = await fetchCropsList();
      setCrops(cropsList);

      const harvests = cropsList.filter(crop => {
        if (!crop.expectedHarvestDate || crop.status === 'COMPLETED') return false;
        const harvestDate = new Date(crop.expectedHarvestDate);
        const today = new Date();
        const timeDiff = harvestDate - today;
        const daysDiff = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));
        return daysDiff >= -5 && daysDiff <= 14;
      });
      setUpcomingHarvests(harvests);

      if (activeSummary.farmCount === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load dashboard:', err);
      setError(err.message || 'Error fetching data');
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Time-aware greeting
  const getGreeting = () => {
    const hrs = new Date().getHours();
    const name = user?.fullName || 'Farmer';
    if (hrs < 12) {
      return t('dashboard.greetings.morning', { name });
    } else if (hrs < 17) {
      return t('dashboard.greetings.afternoon', { name });
    } else {
      return t('dashboard.greetings.evening', { name });
    }
  };

  const triggerQuickActionPlaceholder = (actionKey, actionName) => {
    setToastText(`${t('farm.placeholders.futurePlaceholder', 'Future Phase')}: ${actionName}`);
  };

  return (
    <div className="space-y-8 relative pb-12">
      {/* Toast Alert overlay */}
      {toastText && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-50 bg-emerald-900 text-white text-base font-extrabold px-6 py-3.5 rounded-2xl shadow-xl flex items-center space-x-3.5 border border-emerald-700 animate-fade-in">
          <span>{toastText}</span>
          <button
            onClick={() => setToastText('')}
            className="hover:text-emerald-300 focus:outline-none p-1"
            aria-label="Close message"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      )}

      {/* SECTION 1: GREETING & PAGE HEADER */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-6 rounded-2xl bg-white border border-[#C9DCCB] shadow-2xs">
        <div>
          <h1 className="text-2xl md:text-3xl font-black text-[#222222] leading-tight">
            {getGreeting()}
          </h1>
          <p className="text-xs md:text-sm font-bold text-[#525252] mt-1 uppercase tracking-wider">
            {t('dashboard.status', 'Farm Operations Overview')}
          </p>
        </div>

        <button
          onClick={loadDashboardData}
          disabled={loading}
          style={{ minHeight: '48px' }}
          className="flex items-center justify-center px-4 py-2.5 rounded-xl border border-[#C9DCCB] hover:bg-[#E3F2E6] active:scale-95 transition-all text-[#222222] font-bold cursor-pointer disabled:opacity-50 min-w-[120px]"
          aria-label="Sync status"
        >
          <RefreshCw className={`h-5 w-5 mr-2 ${loading ? 'animate-spin' : ''}`} />
          <span>{t('common.actions.sync', 'Sync')}</span>
        </button>
      </div>

      <StateView
        state={viewState}
        onRetry={loadDashboardData}
        emptyTitle={t('farm.empty.welcome', 'Welcome to Krishi')}
        emptyMessage={t('farm.empty.description', "Let's create your first farm.")}
        emptyActionText={t('farm.create', 'Add My First Farm')}
        onEmptyAction={() => navigate('/farms/new')}
        emptyIcon={MapPin}
      >
        <div className="space-y-8">
          
          {/* SECTION 2: ATTENTION REQUIRED (Urgent Alerts) */}
          <section className="space-y-3.5">
            <h2 className="text-xs font-black text-[#525252] uppercase tracking-wider flex items-center space-x-2">
              <AlertTriangle className="h-4 w-4 text-[#D97706]" />
              <span>{t('dashboard.attention.title', 'Attention Required')}</span>
            </h2>

            {(problems.length === 0 && tasks.length === 0 && alerts.length === 0 && upcomingHarvests.length === 0) ? (
              <div className="rounded-2xl border border-[#C9DCCB] bg-white p-5 text-center text-[#16A34A] font-extrabold flex items-center justify-center space-x-2.5 shadow-2xs">
                <CheckSquare className="h-5 w-5 text-[#16A34A]" />
                <span>{t('dashboard.attention.allClear', 'All clear! No urgent attention required.')}</span>
              </div>
            ) : (
              <div className="space-y-3">
                {/* Upcoming Harvest Alerts */}
                {upcomingHarvests.map(crop => {
                  const daysLeft = Math.ceil((new Date(crop.expectedHarvestDate) - new Date()) / (1000 * 60 * 60 * 24));
                  return (
                    <div
                      key={`harvest-${crop.id}`}
                      onClick={() => navigate(`/crops/${crop.id}`)}
                      className="bg-white border border-[#C9DCCB] border-l-4 border-l-[#D97706] p-4.5 rounded-2xl flex items-start space-x-3.5 shadow-2xs cursor-pointer hover:bg-[#F0F7F1] transition-all"
                    >
                      <Sprout className="h-6 w-6 text-[#D97706] shrink-0 mt-0.5" />
                      <div>
                        <h4 className="text-base font-black text-[#222222]">
                          {daysLeft < 0
                            ? t('dashboard.attention.harvestOverdue', 'Harvest Overdue: {{crop}} ({{variety}})', { crop: crop.cropName, variety: crop.cropVariety })
                            : t('dashboard.attention.harvestApproaching', 'Harvest Approaching: {{crop}} ({{variety}}) in {{days}} days', { crop: crop.cropName, variety: crop.cropVariety, days: daysLeft })}
                        </h4>
                        <p className="text-xs font-semibold text-[#525252] mt-0.5">
                          {crop.farmName} • {t('crops.details.expectedHarvest', 'Expected')}: {new Date(crop.expectedHarvestDate).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                  );
                })}

                {/* Disease Incidents */}
                {problems.map(prob => (
                  <div
                    key={prob.id}
                    onClick={() => navigate(`/disease-incidents/${prob.id}`)}
                    className="bg-white border border-[#C9DCCB] border-l-4 border-l-[#DC2626] p-4.5 rounded-2xl flex items-start space-x-3.5 shadow-2xs cursor-pointer hover:bg-[#F0F7F1] transition-all"
                  >
                    <AlertTriangle className="h-6 w-6 text-[#DC2626] shrink-0 mt-0.5" />
                    <div>
                      <h4 className="text-base font-black text-[#222222]">
                        {t('dashboard.attention.diseaseReported', { name: prob.title, crop: prob.crop })}
                      </h4>
                      <p className="text-sm font-semibold text-gray-500 mt-0.5">
                        {prob.farmName} • {prob.reportedDate}
                      </p>
                    </div>
                  </div>
                ))}

                {/* 2. Tasks Overdue / Pending */}
                {tasks.map(task => (
                  <div
                    key={task.id}
                    className="bg-amber-50/20 border border-amber-200 border-l-4 border-l-amber-500 p-4.5 rounded-2xl flex items-start space-x-3.5 shadow-sm"
                  >
                    <Clock className="h-6 w-6 text-amber-600 shrink-0 mt-0.5" />
                    <div>
                      <h4 className="text-base font-extrabold text-amber-800">
                        {t('dashboard.attention.taskOverdue', { name: task.title, date: task.time })}
                      </h4>
                      <p className="text-sm font-semibold text-gray-500 mt-0.5">
                        {task.crop} • {task.farmName}
                      </p>
                    </div>
                  </div>
                ))}

                {/* 3. Alerts notifications */}
                {alerts.slice(0, 2).map(alert => (
                  <div
                    key={alert.id}
                    className="bg-blue-50/20 border border-blue-150 border-l-4 border-l-blue-500 p-4.5 rounded-2xl flex items-start space-x-3.5 shadow-sm"
                  >
                    <Bell className="h-6 w-6 text-blue-600 shrink-0 mt-0.5" />
                    <div>
                      <h4 className="text-base font-extrabold text-blue-800">
                        {t('dashboard.attention.newNotification', { text: alert.title })}
                      </h4>
                      <p className="text-sm text-gray-650 mt-0.5">{alert.message}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* SECTION 4: QUICK ACTIONS */}
          <section className="space-y-4">
            <h2 className="text-xs font-black text-[#525252] uppercase tracking-wider">
              {t('dashboard.actions.title', 'Quick Actions')}
            </h2>

            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
              {/* Add Farm */}
              <button
                onClick={() => navigate('/farms/new')}
                style={{ minHeight: '80px' }}
                className="bg-white hover:border-[#087A55] border border-[#C9DCCB] p-4.5 rounded-2xl shadow-2xs flex flex-col items-center justify-center space-y-1.5 transition-all text-[#222222] font-extrabold cursor-pointer text-center active:scale-95"
              >
                <PlusCircle className="h-6 w-6 text-[#087A55]" />
                <span className="text-sm font-bold truncate max-w-full">
                  {t('dashboard.actions.addFarm', 'Add Farm')}
                </span>
              </button>

              {/* Start Crop Season */}
              <button
                onClick={() => navigate('/farms')}
                style={{ minHeight: '80px' }}
                className="bg-white hover:border-[#087A55] border border-[#C9DCCB] p-4.5 rounded-2xl shadow-2xs flex flex-col items-center justify-center space-y-1.5 transition-all text-[#222222] font-extrabold cursor-pointer text-center active:scale-95"
              >
                <Calendar className="h-6 w-6 text-[#087A55]" />
                <span className="text-sm font-bold truncate max-w-full">
                  {t('dashboard.actions.startSeason', 'Start Season')}
                </span>
              </button>

              {/* Add Crop */}
              <button
                onClick={() => navigate('/farms')}
                style={{ minHeight: '80px' }}
                className="bg-white hover:border-[#087A55] border border-[#C9DCCB] p-4.5 rounded-2xl shadow-2xs flex flex-col items-center justify-center space-y-1.5 transition-all text-[#087A55] font-extrabold cursor-pointer text-center active:scale-95"
              >
                <Sprout className="h-6 w-6 text-[#087A55]" />
                <span className="text-sm font-bold truncate max-w-full">
                  {t('dashboard.actions.addCrop', 'Add Crop')}
                </span>
              </button>

              {/* Report Problem */}
              <button
                onClick={() => navigate('/disease-incidents')}
                style={{ minHeight: '80px' }}
                className="bg-white hover:border-[#DC2626] border border-[#C9DCCB] p-4.5 rounded-2xl shadow-2xs flex flex-col items-center justify-center space-y-1.5 transition-all text-[#DC2626] font-extrabold cursor-pointer text-center active:scale-95"
              >
                <AlertTriangle className="h-6 w-6 text-[#DC2626]" />
                <span className="text-sm font-bold truncate max-w-full">
                  {t('dashboard.actions.reportProblem', 'Report Issue')}
                </span>
              </button>

              {/* View Tasks */}
              <button
                onClick={() => navigate('/tasks')}
                style={{ minHeight: '80px' }}
                className="bg-white hover:border-[#087A55] border border-[#C9DCCB] p-4.5 rounded-2xl shadow-2xs flex flex-col items-center justify-center space-y-1.5 transition-all text-[#087A55] font-extrabold cursor-pointer text-center col-span-2 sm:col-span-1 active:scale-95"
              >
                <CheckSquare className="h-6 w-6 text-[#087A55]" />
                <span className="text-sm font-bold truncate max-w-full">
                  {t('dashboard.actions.viewTasks', 'View Tasks')}
                </span>
              </button>
            </div>
          </section>

          {/* SECTION 4b: RECENT CROPS */}
          {crops.length > 0 && (
            <section className="space-y-4 pt-4 border-t border-[#C9DCCB]">
              <div className="flex justify-between items-center">
                <h2 className="text-xs font-black text-[#525252] uppercase tracking-wider">
                  {t('dashboard.recentCrops.title', 'Recent Crops')}
                </h2>
                <button
                  onClick={() => navigate('/crops')}
                  className="text-xs font-black text-[#087A55] hover:underline flex items-center space-x-1 cursor-pointer"
                >
                  <span>{t('dashboard.recentCrops.viewAll', 'View All')}</span>
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
              
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {crops
                  .slice()
                  .sort((a, b) => new Date(b.plantingDate) - new Date(a.plantingDate))
                  .slice(0, 2)
                  .map(crop => {
                    const diffTime = new Date() - new Date(crop.plantingDate);
                    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
                    const daysAgo = diffDays <= 0 ? t('crops.card.plantedToday', 'Planted today') : t('crops.card.plantedDaysAgo', 'Planted {{days}} days ago', { days: diffDays });
                    
                    return (
                      <div
                        key={`recent-${crop.id}`}
                        onClick={() => navigate(`/crops/${crop.id}`)}
                        className="bg-white p-4.5 rounded-2xl border border-[#C9DCCB] shadow-2xs hover:border-[#087A55] transition-all cursor-pointer flex items-center justify-between"
                      >
                        <div className="flex items-center space-x-3">
                          <span className="text-2xl p-1 bg-[#E3F2E6] rounded-xl" role="img" aria-label="Crop icon">
                            {crop.cropName.toLowerCase().includes('paddy') || crop.cropName.toLowerCase().includes('rice') ? '🌾' :
                             crop.cropName.toLowerCase().includes('banana') ? '🌱' :
                             crop.cropName.toLowerCase().includes('coconut') ? '🥥' :
                             crop.cropName.toLowerCase().includes('tomato') ? '🍅' : '🌿'}
                          </span>
                          <div>
                            <h4 className="text-base font-black text-[#222222]">
                              {crop.cropName} ({crop.cropVariety})
                            </h4>
                            <p className="text-xs font-semibold text-[#525252]">
                              {crop.farmName} • {daysAgo}
                            </p>
                          </div>
                        </div>
                        <ArrowRight className="h-5 w-5 text-[#6B7280]" />
                      </div>
                    );
                  })}
              </div>
            </section>
          )}

          {/* SECTION 5: RECENT ACTIVITIES TIMELINE */}
          <section className="space-y-4 pt-4 border-t border-gray-200">
            <h2 className="text-lg font-black text-gray-800">
              {t('dashboard.timeline.title', 'Recent Activities')}
            </h2>

            {alerts.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-gray-250 p-6 text-center text-gray-450 font-semibold bg-white">
                {t('dashboard.timeline.empty', 'No recent operations logged.')}
              </div>
            ) : (
              <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm relative">
                {/* Timeline vertical bar */}
                <div className="absolute left-[33px] top-6 bottom-6 w-0.5 bg-gray-200" />

                <div className="space-y-6">
                  {alerts.slice(0, 4).map((alert) => (
                    <div key={alert.id} className="flex items-start space-x-4 relative z-10">
                      <div className="rounded-full bg-emerald-50 border border-emerald-150 p-2 text-emerald-600 shrink-0">
                        <Compass className="h-5 w-5" />
                      </div>
                      <div className="flex-1 min-w-0 pt-0.5">
                        <div className="flex justify-between items-center gap-2">
                          <h4 className="text-base font-bold text-gray-805 truncate">{alert.title}</h4>
                          <span className="text-xs font-semibold text-gray-400 shrink-0">
                            {alert.createdAt ? new Date(alert.createdAt).toLocaleTimeString() : 'Recently'}
                          </span>
                        </div>
                        <p className="text-sm text-gray-500 font-semibold mt-0.5 leading-relaxed">{alert.message}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>

        </div>
      </StateView>
    </div>
  );
};

export default DashboardPage;
