import { useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { CheckSquare, AlertTriangle, AlertCircle, Play, ChevronRight, BarChart2 } from 'lucide-react';

const DashboardPage = () => {
  const { user } = useAuthStore();
  const { t } = useTranslation();
  const [viewState, setViewState] = useState('success');

  // Mock data representing the Action-First sections
  const mockTasks = [
    { id: 1, title: 'Spray Neem Oil Solution', crop: 'Tomato (F1 Hybrid)', status: 'Pending', time: 'Before 10:00 AM' },
    { id: 2, title: 'Weed the South-West field', crop: 'Rice (Ponni)', status: 'In Progress', time: 'Evening' }
  ];

  const mockProblems = [
    { id: 1, title: 'Leaf Spot Outbreak', crop: 'Tomato (F1 Hybrid)', severity: 'Medium', reportedDate: 'Yesterday' }
  ];

  const mockActions = [
    { id: 1, title: 'Apply Copper Fungicide', forProblem: 'Leaf Spot Outbreak', target: 'Tomato Block A' }
  ];

  const mockAlerts = [
    { id: 1, text: 'Heavy rainfall forecasted for tomorrow afternoon.' }
  ];

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

      {/* Welcome Banner Card */}
      <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-3xl font-extrabold text-gray-900 leading-tight">
          {t('dashboard.welcome', 'Welcome, {{name}}!', { name: user?.fullName || 'Farmer' })}
        </h1>
        <p className="text-base text-gray-500 mt-1 font-semibold">
          {t('dashboard.status', 'Farm Operations Overview')}
        </p>
      </div>

      {/* Reusable State View Container */}
      <StateView
        state={viewState}
        onRetry={() => setViewState('success')}
        emptyMessage={t('states.emptyWork', 'No pending work today!')}
      >
        <div className="space-y-6">
          {/* 1. TODAY'S TASKS (Action First) */}
          <section className="space-y-3">
            <h2 className="text-xl font-bold text-gray-800 flex items-center space-x-2">
              <CheckSquare className="h-5.5 w-5.5 text-emerald-600" />
              <span>{t('dashboard.todayTasks', "Work To Do Today")}</span>
            </h2>
            <div className="space-y-3">
              {mockTasks.map((task) => (
                <div
                  key={task.id}
                  className="rounded-2xl bg-white p-4 shadow-sm border border-gray-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-emerald-300 transition-all cursor-pointer"
                >
                  <div>
                    <h3 className="text-base font-bold text-gray-800">{task.title}</h3>
                    <p className="text-sm text-gray-500 font-semibold">{task.crop} • {task.time}</p>
                  </div>
                  <button
                    style={{ minHeight: '48px' }}
                    className="rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white text-base font-bold px-5 py-2 flex items-center justify-center space-x-1 min-w-[120px] self-end sm:self-auto cursor-pointer"
                  >
                    <Play className="h-4 w-4 fill-current" />
                    <span>Start</span>
                  </button>
                </div>
              ))}
            </div>
          </section>

          {/* 2. CROP PROBLEMS & 3. RECOMMENDED ACTIONS */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Crop Problems */}
            <section className="space-y-3">
              <h2 className="text-xl font-bold text-gray-800 flex items-center space-x-2">
                <AlertTriangle className="h-5.5 w-5.5 text-red-650" />
                <span>{t('dashboard.cropProblems', 'Crop Problems')}</span>
              </h2>
              <div className="space-y-3">
                {mockProblems.map((prob) => (
                  <div
                    key={prob.id}
                    className="rounded-2xl bg-white p-4 shadow-sm border border-red-150 bg-red-50/20 flex items-start justify-between hover:shadow-sm transition-all cursor-pointer"
                  >
                    <div>
                      <h3 className="text-base font-bold text-red-800">{prob.title}</h3>
                      <p className="text-sm text-gray-500 font-semibold">{prob.crop}</p>
                    </div>
                    <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-bold text-red-700">
                      {prob.severity}
                    </span>
                  </div>
                ))}
              </div>
            </section>

            {/* Recommended Actions */}
            <section className="space-y-3">
              <h2 className="text-xl font-bold text-gray-800 flex items-center space-x-2">
                <AlertCircle className="h-5.5 w-5.5 text-amber-500" />
                <span>{t('dashboard.recommendedActions', 'Recommended Actions')}</span>
              </h2>
              <div className="space-y-3">
                {mockActions.map((action) => (
                  <div
                    key={action.id}
                    className="rounded-2xl bg-white p-4 shadow-sm border border-amber-200 bg-amber-50/10 flex items-center justify-between hover:shadow-sm transition-all cursor-pointer"
                  >
                    <div>
                      <h3 className="text-base font-bold text-amber-800">{action.title}</h3>
                      <p className="text-sm text-gray-500 font-semibold">{action.forProblem} • {action.target}</p>
                    </div>
                    <ChevronRight className="h-5 w-5 text-amber-600" />
                  </div>
                ))}
              </div>
            </section>
          </div>

          {/* 4. ALERTS */}
          <section className="space-y-3">
            <h2 className="text-xl font-bold text-gray-800 flex items-center space-x-2">
              <AlertCircle className="h-5.5 w-5.5 text-blue-500" />
              <span>{t('dashboard.alerts', 'Alerts')}</span>
            </h2>
            <div className="space-y-2">
              {mockAlerts.map((alert) => (
                <div
                  key={alert.id}
                  className="rounded-2xl bg-white p-4 border border-blue-150 bg-blue-50/20 text-base font-semibold text-blue-700 shadow-sm"
                >
                  {alert.text}
                </div>
              ))}
            </div>
          </section>

          {/* 5. METRICS (Overview Metrics pushed to bottom as secondary support) */}
          <section className="space-y-3 pt-4 border-t border-gray-200">
            <h2 className="text-xl font-bold text-gray-800 flex items-center space-x-2">
              <BarChart2 className="h-5.5 w-5.5 text-gray-500" />
              <span>{t('dashboard.farmOverview', 'Farm Overview / Metrics')}</span>
            </h2>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div className="rounded-2xl bg-white p-4 shadow-sm border border-gray-250 flex flex-col justify-between">
                <span className="text-sm font-bold text-gray-400 uppercase tracking-wider">Crops Planted</span>
                <span className="text-2xl font-black text-emerald-700 mt-1">3</span>
              </div>
              <div className="rounded-2xl bg-white p-4 shadow-sm border border-gray-250 flex flex-col justify-between">
                <span className="text-sm font-bold text-gray-400 uppercase tracking-wider">Problems Active</span>
                <span className="text-2xl font-black text-red-650 mt-1">1</span>
              </div>
              <div className="rounded-2xl bg-white p-4 shadow-sm border border-gray-250 flex flex-col justify-between">
                <span className="text-sm font-bold text-gray-400 uppercase tracking-wider">Tasks Pending</span>
                <span className="text-2xl font-black text-emerald-700 mt-1">2</span>
              </div>
              <div className="rounded-2xl bg-white p-4 shadow-sm border border-gray-250 flex flex-col justify-between">
                <span className="text-sm font-bold text-gray-400 uppercase tracking-wider">Acres Active</span>
                <span className="text-2xl font-black text-emerald-750 mt-1">8.5</span>
              </div>
            </div>
          </section>
        </div>
      </StateView>
    </div>
  );
};

export default DashboardPage;
