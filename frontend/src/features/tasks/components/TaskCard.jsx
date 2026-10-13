import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  CheckSquare,
  Play,
  CheckCircle,
  XCircle,
  Clock,
  Droplets,
  Sprout,
  ShieldAlert,
  Eye,
  Scissors,
  Sun,
  Wrench,
  AlertTriangle,
  ChevronRight,
  Calendar,
  FastForward
} from 'lucide-react';
import { startTask, completeTask, cancelTask, skipTask } from '../api/taskApi';
import TaskDetailsModal from './TaskDetailsModal';
import RescheduleTaskModal from './RescheduleTaskModal';
import SkipTaskModal from './SkipTaskModal';
import StatusBadge from '../../../shared/components/StatusBadge';

const TYPE_ICONS = {
  IRRIGATION: Droplets,
  FERTILIZER_APPLICATION: Sprout,
  MANURE_APPLICATION: Sprout,
  WEEDING: Scissors,
  PEST_CONTROL: ShieldAlert,
  CROP_INSPECTION: Eye,
  PRUNING: Scissors,
  HARVESTING: Sun,
  DISEASE_TREATMENT: ShieldAlert,
  OTHER: Wrench,
};

export const TaskCard = ({ task, onStatusChange, showTreatmentContext = true }) => {
  const { t } = useTranslation();
  const [loadingAction, setLoadingAction] = useState(null);
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);
  const [isSkipOpen, setIsSkipOpen] = useState(false);

  const isOverdue = () => {
    if (task.status === 'COMPLETED' || task.status === 'CANCELLED' || task.status === 'SKIPPED') return false;
    if (task.status === 'OVERDUE') return true;
    if (!task.dueDate) return false;
    const d = new Date(task.dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    d.setHours(23, 59, 59, 999);
    return d < today;
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return {
          label: t('tasks.status.PENDING', 'Pending'),
          className: 'bg-amber-50 text-amber-800 border-amber-200'
        };
      case 'IN_PROGRESS':
        return {
          label: t('tasks.status.IN_PROGRESS', 'In Progress'),
          className: 'bg-blue-50 text-blue-800 border-blue-200'
        };
      case 'COMPLETED':
        return {
          label: t('tasks.status.COMPLETED', 'Completed'),
          className: 'bg-emerald-50 text-emerald-800 border-emerald-200'
        };
      case 'CANCELLED':
        return {
          label: t('tasks.status.CANCELLED', 'Cancelled'),
          className: 'bg-gray-100 text-gray-700 border-gray-200'
        };
      case 'SKIPPED':
        return {
          label: t('tasks.status.SKIPPED', 'Skipped'),
          className: 'bg-purple-50 text-purple-800 border-purple-200'
        };
      default:
        return {
          label: status,
          className: 'bg-gray-50 text-gray-700 border-gray-200'
        };
    }
  };

  const handleStart = async (e) => {
    e.stopPropagation();
    setLoadingAction('start');
    try {
      await startTask(task.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to start task:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleComplete = async (e) => {
    e.stopPropagation();
    setLoadingAction('complete');
    try {
      await completeTask(task.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to complete task:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleSkip = async (e) => {
    e.stopPropagation();
    setLoadingAction('skip');
    try {
      await skipTask(task.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to skip task:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleCancel = async (e) => {
    e.stopPropagation();
    setLoadingAction('cancel');
    try {
      await cancelTask(task.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to cancel task:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const statusInfo = getStatusBadge(task.status);
  const IconComponent = TYPE_ICONS[task.taskType] || CheckSquare;
  const overdue = isOverdue();

  // Format farmer-facing title (converting internal enum strings or default English preset titles)
  const getFormattedTitle = () => {
    if (!task.title) return t('tasks.defaultTitle', 'Work Task');
    if (task.title === 'INSPECT_CROP_RECOVERY') {
      return t('tasks.inspectCropRecovery', 'Inspect Crop Recovery');
    }

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

    const trimmed = task.title.trim();
    if (defaultTitleMap[trimmed]) {
      return t(defaultTitleMap[trimmed]);
    }
    if (task.taskType && defaultTitleMap[task.taskType]) {
      return t(defaultTitleMap[task.taskType]);
    }

    return task.title;
  };

  // Format farmer-facing description (parsing JSON if present)
  const getFormattedDescription = () => {
    if (!task.description) return null;
    if (task.description.startsWith('{')) {
      try {
        const parsed = JSON.parse(task.description);
        if (parsed.treatmentName) {
          return `${t('treatment.title', 'Treatment')}: ${parsed.treatmentName}`;
        }
      } catch (e) {
        // Fallback to raw text if not valid JSON
      }
    }
    return task.description;
  };

  const formattedDesc = getFormattedDescription();

  return (
    <>
      <div
        onClick={() => setIsDetailsOpen(true)}
        className={`rounded-2xl bg-white p-5 border shadow-xs hover:shadow-md transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-l-4 ${
          overdue
            ? 'border-l-red-500 border-red-200 bg-red-50/10'
            : task.status === 'COMPLETED'
            ? 'border-l-[#22C55E] border-[#C5DCC9]'
            : task.status === 'IN_PROGRESS'
            ? 'border-l-sky-500 border-[#C5DCC9]'
            : 'border-l-amber-500 border-[#C5DCC9]'
        }`}
      >
        {/* Left Content Area */}
        <div className="flex items-start space-x-3.5 min-w-0 flex-1">
          <div
            className={`rounded-xl p-3 mt-0.5 shrink-0 ${
              overdue
                ? 'bg-red-50 text-red-600'
                : task.status === 'COMPLETED'
                ? 'bg-[#E6F4E8] text-[#087A55]'
                : task.status === 'IN_PROGRESS'
                ? 'bg-sky-50 text-sky-600'
                : 'bg-amber-50 text-amber-700'
            }`}
          >
            <IconComponent className="h-6 w-6" />
          </div>

          <div className="min-w-0 space-y-1 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-base font-black text-[#222222] truncate leading-snug">{getFormattedTitle()}</h3>
              {task.source === 'PLAN' && (
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-[#E3F2E6] text-[#087A55] border border-[#C9DCCB] shrink-0">
                  {t('crops.plan.sectionTitle', 'Plan')}
                </span>
              )}
              {task.source === 'AUTOMATION' && (
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 border border-purple-100 shrink-0">
                  {t('tasks.source.automation', 'Auto Follow-up')}
                </span>
              )}
            </div>

            {showTreatmentContext && (
              <p className="text-xs font-semibold text-gray-500 truncate">
                {task.cropName || task.crop || 'Crop'} {task.cropVariety ? `(${task.cropVariety})` : ''} &bull; {task.farmName || 'Farm'}
              </p>
            )}

            {formattedDesc && (
              <p className="text-xs text-gray-600 font-semibold line-clamp-2 bg-gray-50 p-2 rounded-lg border border-gray-100">
                {formattedDesc}
              </p>
            )}

            <div className="flex flex-wrap items-center gap-3 text-xs font-bold text-gray-400 pt-0.5">
              <div className="flex items-center space-x-1.5">
                <Clock className="h-3.5 w-3.5 shrink-0 text-emerald-600" />
                <span>
                  {t('tasks.due', 'Due')}: {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : t('tasks.dueDate.today', 'Today')}
                </span>
              </div>

              {overdue && (
                <span className="inline-flex items-center text-red-650 font-black bg-red-50 px-2 py-0.5 rounded-md border border-red-150">
                  <AlertTriangle className="h-3 w-3 mr-1" />
                  {t('tasks.status.OVERDUE', 'OVERDUE')}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Right Status Badge & Primary Action Controls */}
        <div className="flex items-center justify-between sm:justify-end gap-2.5 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-gray-100">
          {/* Status Badge */}
          <StatusBadge status={overdue ? 'OVERDUE' : task.status} />

          {/* Action Workflow Controls */}
          <div className="flex items-center space-x-1.5">
            {(task.status === 'PENDING' || task.status === 'OVERDUE') && (
              <>
                <button
                  onClick={handleStart}
                  disabled={loadingAction === 'start'}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#087A55] hover:bg-[#066747] active:scale-95 text-white font-extrabold px-3.5 text-xs flex items-center space-x-1.5 transition-all cursor-pointer disabled:opacity-50 shadow-xs"
                >
                  <Play className="h-3.5 w-3.5 fill-current" />
                  <span>{loadingAction === 'start' ? 'Updating...' : t('tasks.actions.start', 'Start Work')}</span>
                </button>

                <button
                  onClick={handleComplete}
                  disabled={loadingAction === 'complete'}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#087A55] hover:bg-[#066747] active:scale-95 text-white font-extrabold px-3 text-xs flex items-center space-x-1 transition-all cursor-pointer disabled:opacity-50 shadow-xs"
                  title={t('tasks.actions.complete', 'Complete Work')}
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  <span className="hidden sm:inline">{t('tasks.actions.completeShort', 'Done')}</span>
                </button>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsRescheduleOpen(true);
                  }}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#F6FAF7] hover:bg-amber-50 text-[#57534e] hover:text-amber-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-[#D6E2D9] transition-all cursor-pointer"
                  title={t('tasks.actions.reschedule', 'Reschedule Activity')}
                >
                  <Calendar className="h-4 w-4" />
                </button>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsSkipOpen(true);
                  }}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#F6FAF7] hover:bg-purple-50 text-[#57534e] hover:text-purple-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-[#D6E2D9] transition-all cursor-pointer disabled:opacity-50"
                  title={t('tasks.actions.skip', 'Skip Activity')}
                >
                  <FastForward className="h-4 w-4" />
                </button>

                <button
                  onClick={handleCancel}
                  disabled={loadingAction === 'cancel'}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#F6FAF7] hover:bg-red-50 text-[#57534e] hover:text-red-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-[#D6E2D9] transition-all cursor-pointer disabled:opacity-50"
                  title={t('tasks.actions.cancel', 'Cancel Work')}
                >
                  <XCircle className="h-4 w-4" />
                </button>
              </>
            )}

            {task.status === 'IN_PROGRESS' && (
              <>
                <button
                  onClick={handleComplete}
                  disabled={loadingAction === 'complete'}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#087A55] hover:bg-[#066747] active:scale-95 text-white font-extrabold px-3.5 text-xs flex items-center space-x-1.5 transition-all cursor-pointer disabled:opacity-50 shadow-xs"
                >
                  <CheckCircle className="h-4 w-4" />
                  <span>{loadingAction === 'complete' ? 'Updating...' : t('tasks.actions.complete', 'Complete Work')}</span>
                </button>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsRescheduleOpen(true);
                  }}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#F6FAF7] hover:bg-amber-50 text-[#57534e] hover:text-amber-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-[#D6E2D9] transition-all cursor-pointer"
                  title={t('tasks.actions.reschedule', 'Reschedule Activity')}
                >
                  <Calendar className="h-4 w-4" />
                </button>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsSkipOpen(true);
                  }}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-[#F6FAF7] hover:bg-purple-50 text-[#57534e] hover:text-purple-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-[#D6E2D9] transition-all cursor-pointer disabled:opacity-50"
                  title={t('tasks.actions.skip', 'Skip Activity')}
                >
                  <FastForward className="h-4 w-4" />
                </button>

                <button
                  onClick={handleCancel}
                  disabled={loadingAction === 'cancel'}
                  style={{ minHeight: '44px' }}
                  className="rounded-xl bg-gray-50 hover:bg-red-50 text-gray-500 hover:text-red-700 font-bold px-2.5 text-xs flex items-center space-x-1 border border-gray-200 transition-all cursor-pointer disabled:opacity-50"
                  title={t('tasks.actions.cancel', 'Cancel Work')}
                >
                  <XCircle className="h-4 w-4" />
                </button>
              </>
            )}

            {(task.status === 'COMPLETED' || task.status === 'CANCELLED' || task.status === 'SKIPPED') && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setIsDetailsOpen(true);
                }}
                className="text-xs font-bold text-gray-400 hover:text-emerald-700 flex items-center space-x-1 p-1 cursor-pointer"
              >
                <span>{t('tasks.actions.viewDetails', 'View Details')}</span>
                <ChevronRight className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Task Details Drawer/Modal */}
      <TaskDetailsModal
        task={task}
        isOpen={isDetailsOpen}
        onClose={() => setIsDetailsOpen(false)}
        onStatusChange={onStatusChange}
      />

      {/* Reschedule Task Modal */}
      <RescheduleTaskModal
        task={task}
        isOpen={isRescheduleOpen}
        onClose={() => setIsRescheduleOpen(false)}
        onSuccess={onStatusChange}
      />

      {/* Skip Task Modal */}
      <SkipTaskModal
        task={task}
        isOpen={isSkipOpen}
        onClose={() => setIsSkipOpen(false)}
        onSuccess={onStatusChange}
      />
    </>
  );
};

export default TaskCard;
