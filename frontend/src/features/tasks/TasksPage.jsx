import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { CheckSquare, Plus, CheckCircle } from 'lucide-react';
import TaskCard from './components/TaskCard';
import CreateTaskModal from './components/CreateTaskModal';
import { fetchAllTasks } from './api/taskApi';

/**
 * TasksPage acts as the primary agricultural operational queue for the farmer.
 * Organizes field tasks into Today's Work, Upcoming Work, and Completed Work.
 */
const TasksPage = () => {
  const { t } = useTranslation();

  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewState, setViewState] = useState('loading');
  const [activeTab, setActiveTab] = useState('TODAY'); // TODAY, UPCOMING, COMPLETED, ALL
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const loadTasksData = async () => {
    setLoading(true);
    setViewState('loading');

    try {
      const list = await fetchAllTasks();
      setTasks(list);

      if (list.length === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load tasks:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasksData();
  }, []);

  const isToday = (dateStr) => {
    if (!dateStr) return true;
    const d = new Date(dateStr);
    const today = new Date();
    return (
      d.getDate() === today.getDate() &&
      d.getMonth() === today.getMonth() &&
      d.getFullYear() === today.getFullYear()
    );
  };

  const isOverdue = (dateStr, status) => {
    if (!dateStr || status === 'COMPLETED' || status === 'CANCELLED') return false;
    const d = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    d.setHours(23, 59, 59, 999);
    return d < today;
  };

  const isFuture = (dateStr) => {
    if (!dateStr) return false;
    const d = new Date(dateStr);
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    return d > today;
  };

  const isActionableToday = (item) => {
    if (item.status !== 'PENDING' && item.status !== 'IN_PROGRESS') return false;
    return isToday(item.dueDate) || isOverdue(item.dueDate, item.status);
  };

  const filteredTasks = tasks.filter((item) => {
    if (activeTab === 'TODAY') {
      return isActionableToday(item);
    }
    if (activeTab === 'UPCOMING') {
      return (item.status === 'PENDING' || item.status === 'IN_PROGRESS') && isFuture(item.dueDate);
    }
    if (activeTab === 'COMPLETED') return item.status === 'COMPLETED';
    return true;
  });

  const todayCount = tasks.filter(isActionableToday).length;
  const upcomingCount = tasks.filter(t => (t.status === 'PENDING' || t.status === 'IN_PROGRESS') && isFuture(t.dueDate)).length;
  const completedCount = tasks.filter(t => t.status === 'COMPLETED').length;

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Title Header */}
      <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-800 flex items-center space-x-2.5">
            <CheckSquare className="h-7 w-7 text-emerald-600" />
            <span>{t('nav.tasks', "Today's Work")}</span>
          </h1>
          <p className="text-sm font-semibold text-gray-400 mt-1">
            {t('tasks.headerDesc', 'Manage irrigation, fertilizing, weeding, and crop care activities')}
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          style={{ minHeight: '48px' }}
          className="flex items-center justify-center space-x-1.5 rounded-xl bg-[#087A55] hover:bg-[#066747] px-5 text-sm font-black text-white shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <Plus className="h-5 w-5" />
          <span>{t('tasks.addWorkButton', 'Add Work')}</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center space-x-2 border-b border-[#C9DCCB] pb-3 overflow-x-auto select-none">
        <button
          onClick={() => setActiveTab('TODAY')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'TODAY'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('tasks.tabs.today', "Today's Work")} ({todayCount})
        </button>

        <button
          onClick={() => setActiveTab('UPCOMING')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'UPCOMING'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('tasks.tabs.upcoming', 'Upcoming Work')} ({upcomingCount})
        </button>

        <button
          onClick={() => setActiveTab('COMPLETED')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'COMPLETED'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('tasks.tabs.completed', 'Completed Work')} ({completedCount})
        </button>

        <button
          onClick={() => setActiveTab('ALL')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'ALL'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('tasks.tabs.all', 'All Work')} ({tasks.length})
        </button>
      </div>

      {/* State View & Tasks List */}
      <StateView
        state={viewState}
        onRetry={loadTasksData}
        emptyTitle={t('states.emptyWork', "Today's work completed")}
        emptyMessage={t('states.emptyWorkDesc', '✓ All field activities are up to date!')}
        emptyIcon={CheckCircle}
        emptyActionText={t('tasks.addWorkButton', 'Add Work')}
        onEmptyAction={() => setIsAddModalOpen(true)}
      >
        <div className="space-y-4">
          {filteredTasks.map((task) => (
            <TaskCard key={task.id} task={task} onStatusChange={loadTasksData} />
          ))}
        </div>
      </StateView>

      {/* Create Task Modal */}
      <CreateTaskModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={loadTasksData}
      />
    </div>
  );
};

export default TasksPage;
