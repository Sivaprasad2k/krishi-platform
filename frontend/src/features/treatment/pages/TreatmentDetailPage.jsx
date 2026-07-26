import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Droplet, Play, CheckCircle, Clock, Beaker, FileText, CheckSquare, Plus } from 'lucide-react';
import StateView from '../../../shared/components/StateView';
import { getTreatmentById, startTreatment, completeTreatment } from '../api/treatmentApi';
import { getTasksByTreatment } from '../../tasks/api/taskApi';
import TaskCard from '../../tasks/components/TaskCard';
import AddTaskModal from '../../tasks/components/AddTaskModal';

export const TreatmentDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [treatment, setTreatment] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [viewState, setViewState] = useState('loading');
  const [loadingAction, setLoadingAction] = useState(null);
  const [isAddTaskModalOpen, setIsAddTaskModalOpen] = useState(false);

  const loadDetails = async () => {
    setViewState('loading');
    try {
      const data = await getTreatmentById(id);
      if (data?.success && data?.data) {
        setTreatment(data.data);
        
        try {
          const tasksRes = await getTasksByTreatment(id);
          setTasks(tasksRes?.data || []);
        } catch (err) {
          console.warn('Failed to fetch tasks for treatment:', err);
        }

        setViewState('success');
      } else {
        throw new Error(data?.message || 'Failed to load treatment details');
      }
    } catch (err) {
      console.error('Failed to load treatment details:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    }
  };

  useEffect(() => {
    loadDetails();
  }, [id]);

  const handleAction = async (actionFn, actionName) => {
    setLoadingAction(actionName);
    try {
      await actionFn(id);
      await loadDetails();
    } catch (err) {
      console.error(`Failed action ${actionName}:`, err);
    } finally {
      setLoadingAction(null);
    }
  };

  if (viewState === 'loading' || viewState === 'error' || viewState === 'offline') {
    return (
      <div className="max-w-2xl mx-auto p-4">
        <StateView state={viewState} onRetry={loadDetails} />
      </div>
    );
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PLANNED': return 'bg-amber-50 text-amber-800 border-amber-200';
      case 'IN_PROGRESS': return 'bg-blue-50 text-blue-750 border-blue-200';
      case 'COMPLETED': return 'bg-emerald-50 text-emerald-800 border-emerald-200';
      default: return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto pb-12">
      {/* Header */}
      <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex items-center space-x-3">
        <button
          onClick={() => navigate('/treatments')}
          className="p-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-600 transition-all cursor-pointer"
          aria-label={t('common.actions.back', 'Back')}
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-xl font-black text-gray-850">
            {t('treatment.actions.viewDetails', 'Treatment Workspace')}
          </h1>
          <p className="text-xs font-semibold text-gray-400 mt-0.5">
            ID: {treatment.id}
          </p>
        </div>
      </div>

      {/* Main Card */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-6 border-l-4 border-l-blue-500">
        <div className="flex items-start justify-between gap-3 pb-4 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-blue-50 text-blue-650 rounded-2xl">
              <Droplet className="h-7 w-7" />
            </div>
            <div>
              <h2 className="text-2xl font-black text-gray-900">{treatment.treatmentName}</h2>
              <span className="text-xs font-bold text-gray-400 mt-0.5 block">
                Planned on {treatment.createdAt ? new Date(treatment.createdAt).toLocaleDateString() : 'Recently'}
              </span>
            </div>
          </div>

          <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ${getStatusBadgeClass(treatment.status)}`}>
            {t(`treatment.status.${treatment.status}`, treatment.status)}
          </span>
        </div>

        {/* Dosage & Application Method */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 bg-blue-50/50 rounded-xl border border-blue-150 space-y-1">
            <span className="text-xs font-bold text-blue-700 uppercase tracking-wider flex items-center space-x-1">
              <Beaker className="h-4 w-4" />
              <span>Recommended Dosage</span>
            </span>
            <p className="text-base font-extrabold text-gray-850">
              {treatment.recommendedDosage || 'Not specified'}
            </p>
          </div>

          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 space-y-1">
            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider block">
              Application Method
            </span>
            <p className="text-base font-extrabold text-gray-850">
              {treatment.applicationMethod || 'Standard Application'}
            </p>
          </div>
        </div>

        {/* Application Notes */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center space-x-1">
            <FileText className="h-4 w-4" />
            <span>{t('treatment.description', 'Application Notes / Instructions')}</span>
          </h3>
          <p className="text-base text-gray-800 font-semibold leading-relaxed bg-gray-50 p-4 rounded-xl border border-gray-150">
            {treatment.description || 'No special instructions provided.'}
          </p>
        </div>

        {/* Timeline Milestones */}
        <div className="space-y-3 pt-2">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Milestones</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-extrabold">
            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <Clock className="h-4 w-4 text-blue-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Started</span>
                <span className="text-gray-800">{treatment.startDate ? new Date(treatment.startDate).toLocaleString() : 'Not started yet'}</span>
              </div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <CheckCircle className="h-4 w-4 text-emerald-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Completed</span>
                <span className="text-gray-800">{treatment.completionDate ? new Date(treatment.completionDate).toLocaleString() : 'Pending completion'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        {treatment.status !== 'COMPLETED' && (
          <div className="pt-4 border-t border-gray-100 flex flex-wrap items-center gap-3">
            {treatment.status === 'PLANNED' && (
              <button
                onClick={() => handleAction(startTreatment, 'start')}
                disabled={loadingAction === 'start'}
                style={{ minHeight: '48px' }}
                className="flex-1 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-extrabold shadow-sm transition-all cursor-pointer flex items-center justify-center space-x-2"
              >
                <Play className="h-4 w-4 fill-current" />
                <span>{loadingAction === 'start' ? 'Updating...' : t('treatment.actions.start', 'Start Treatment')}</span>
              </button>
            )}

            {treatment.status === 'IN_PROGRESS' && (
              <button
                onClick={() => handleAction(completeTreatment, 'complete')}
                disabled={loadingAction === 'complete'}
                style={{ minHeight: '48px' }}
                className="flex-1 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold shadow-sm transition-all cursor-pointer flex items-center justify-center space-x-2"
              >
                <CheckCircle className="h-4 w-4" />
                <span>{loadingAction === 'complete' ? 'Updating...' : t('treatment.actions.complete', 'Mark Completed')}</span>
              </button>
            )}
          </div>
        )}
      </div>

      {/* Tasks To Do Section */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-2">
            <CheckSquare className="h-5 w-5 text-emerald-650" />
            <h2 className="text-lg font-black text-gray-800">Tasks To Do</h2>
          </div>
          <button
            onClick={() => setIsAddTaskModalOpen(true)}
            className="flex items-center space-x-1 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black transition-all shadow-sm active:scale-95 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>{t('taskModal.addButton', 'Add Task')}</span>
          </button>
        </div>

        {tasks.length > 0 ? (
          <div className="space-y-3">
            {tasks.map((task) => (
              <TaskCard
                key={task.id}
                task={{ ...task, treatmentName: treatment.treatmentName }}
                onStatusChange={loadDetails}
                showTreatmentContext={false}
              />
            ))}
          </div>
        ) : (
          <div className="p-4 bg-emerald-50/50 rounded-xl border border-emerald-100 text-center flex flex-col items-center space-y-2">
            <CheckSquare className="h-7 w-7 text-emerald-600" />
            <span className="text-sm font-extrabold text-emerald-800">
              No tasks scheduled for this treatment plan yet.
            </span>
          </div>
        )}
      </div>

      <AddTaskModal
        isOpen={isAddTaskModalOpen}
        onClose={() => setIsAddTaskModalOpen(false)}
        onSuccess={loadDetails}
        preselectedTreatmentId={treatment.id}
      />
    </div>
  );
};

export default TreatmentDetailPage;
