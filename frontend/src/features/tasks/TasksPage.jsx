import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { CheckSquare, Clock } from 'lucide-react';

const TasksPage = () => {
  const { t } = useTranslation();
  const [viewState, setViewState] = useState('success');

  // Mock data for tasks
  const mockTasks = [
    { id: 1, title: 'Spray Neem Oil Solution', crop: 'Tomato (F1 Hybrid)', status: 'Pending', dueDate: 'Today' },
    { id: 2, title: 'Weed the South-West field', crop: 'Rice (Ponni)', status: 'In Progress', dueDate: 'Tomorrow' },
    { id: 3, title: 'Irrigate field block B', crop: 'Rice (Ponni)', status: 'Pending', dueDate: 'Jun 25' }
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

      <div className="rounded-xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800">{t('nav.tasks', 'Work To Do')}</h1>
        <p className="text-base text-gray-500 mt-1">
          {t('states.emptyWork', 'No pending work today! All tasks completed.')}
        </p>
      </div>

      <StateView
        state={viewState}
        onRetry={() => setViewState('success')}
        emptyMessage={t('states.emptyWork', 'No pending work today!')}
      >
        <div className="space-y-4">
          {mockTasks.map((task) => (
            <div
              key={task.id}
              className="rounded-2xl bg-white p-5 shadow-sm border border-gray-200 hover:border-emerald-300 hover:shadow-md transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
            >
              <div className="flex items-start space-x-3.5">
                <div className="rounded-xl bg-emerald-50 p-3 text-emerald-600 mt-0.5">
                  <CheckSquare className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-gray-850">{task.title}</h3>
                  <p className="text-base text-gray-500 mt-0.5">{task.crop}</p>
                  <div className="flex items-center space-x-2 mt-2 text-sm font-semibold text-gray-400">
                    <Clock className="h-4 w-4" />
                    <span>Due: {task.dueDate}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-3 self-end sm:self-center">
                <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-bold ${
                  task.status === 'In Progress'
                    ? 'bg-amber-50 text-amber-700'
                    : 'bg-emerald-50 text-emerald-700'
                }`}>
                  {task.status}
                </span>
                <button
                  style={{ minHeight: '48px' }}
                  className="rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white font-bold px-4 transition-all text-base min-w-[100px] cursor-pointer"
                >
                  Start
                </button>
              </div>
            </div>
          ))}
        </div>
      </StateView>
    </div>
  );
};

export default TasksPage;
