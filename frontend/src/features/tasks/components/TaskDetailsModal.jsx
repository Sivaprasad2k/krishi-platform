import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  X,
  Play,
  CheckCircle,
  XCircle,
  Calendar,
  Clock,
  Sprout,
  MapPin,
  FileText,
  AlertTriangle,
  Beaker,
  Bot,
  User,
  Check,
  FastForward
} from 'lucide-react';
import { startTask, completeTask, cancelTask, skipTask } from '../api/taskApi';
import RescheduleTaskModal from './RescheduleTaskModal';
import SkipTaskModal from './SkipTaskModal';

const WORK_TYPES_MAP = {
  IRRIGATION: { labelKey: 'tasks.types.IRRIGATION', defaultTitle: 'Water Crop', color: 'text-blue-600 bg-blue-50' },
  FERTILIZER_APPLICATION: { labelKey: 'tasks.types.FERTILIZER_APPLICATION', defaultTitle: 'Apply Fertilizer', color: 'text-emerald-600 bg-emerald-50' },
  MANURE_APPLICATION: { labelKey: 'tasks.types.MANURE_APPLICATION', defaultTitle: 'Add Manure / Compost', color: 'text-amber-700 bg-amber-50' },
  WEEDING: { labelKey: 'tasks.types.WEEDING', defaultTitle: 'Remove Weeds', color: 'text-lime-700 bg-lime-50' },
  PEST_CONTROL: { labelKey: 'tasks.types.PEST_CONTROL', defaultTitle: 'Pest Control Operation', color: 'text-rose-600 bg-rose-50' },
  CROP_INSPECTION: { labelKey: 'tasks.types.CROP_INSPECTION', defaultTitle: 'Inspect Crop Health', color: 'text-teal-600 bg-teal-50' },
  PRUNING: { labelKey: 'tasks.types.PRUNING', defaultTitle: 'Prune Plants', color: 'text-indigo-600 bg-indigo-50' },
  HARVESTING: { labelKey: 'tasks.types.HARVESTING', defaultTitle: 'Harvest Crop', color: 'text-amber-600 bg-amber-50' },
  DISEASE_TREATMENT: { labelKey: 'tasks.types.DISEASE_TREATMENT', defaultTitle: 'Disease Treatment', color: 'text-red-600 bg-red-50' },
  OTHER: { labelKey: 'tasks.types.OTHER', defaultTitle: 'Field Work', color: 'text-gray-600 bg-gray-50' }
};

export const TaskDetailsModal = ({ task, isOpen, onClose, onStatusChange }) => {
  const { t } = useTranslation();
  const [loadingAction, setLoadingAction] = useState(null);
  const [showConfirmComplete, setShowConfirmComplete] = useState(false);
  const [showConfirmCancel, setShowConfirmCancel] = useState(false);
  const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);
  const [isSkipOpen, setIsSkipOpen] = useState(false);
  const [actionError, setActionError] = useState('');

  if (!isOpen || !task) return null;

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
        return { label: t('tasks.status.PENDING', 'Pending'), className: 'bg-amber-50 text-amber-800 border-amber-200' };
      case 'IN_PROGRESS':
        return { label: t('tasks.status.IN_PROGRESS', 'In Progress'), className: 'bg-blue-50 text-blue-800 border-blue-200' };
      case 'COMPLETED':
        return { label: t('tasks.status.COMPLETED', 'Completed'), className: 'bg-emerald-50 text-emerald-800 border-emerald-200' };
      case 'CANCELLED':
        return { label: t('tasks.status.CANCELLED', 'Cancelled'), className: 'bg-gray-100 text-gray-700 border-gray-200' };
      case 'SKIPPED':
        return { label: t('tasks.status.SKIPPED', 'Skipped'), className: 'bg-purple-50 text-purple-800 border-purple-200' };
      default:
        return { label: status, className: 'bg-gray-50 text-gray-700 border-gray-200' };
    }
  };

  const getFormattedTitle = () => {
    if (!task.title) return t('tasks.defaultTitle', 'Work Task');
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
    return task.title;
  };

  const handleStart = async () => {
    setActionError('');
    setLoadingAction('start');
    try {
      await startTask(task.id);
      if (onStatusChange) onStatusChange();
      onClose();
    } catch (err) {
      setActionError(err.message || t('tasks.errors.updateFailed', 'Unable to update work status'));
    } finally {
      setLoadingAction(null);
    }
  };

  const handleComplete = async () => {
    setActionError('');
    setLoadingAction('complete');
    try {
      await completeTask(task.id);
      if (onStatusChange) onStatusChange();
      setShowConfirmComplete(false);
      onClose();
    } catch (err) {
      setActionError(err.message || t('tasks.errors.updateFailed', 'Unable to update work status'));
    } finally {
      setLoadingAction(null);
    }
  };

  const handleCancel = async () => {
    setActionError('');
    setLoadingAction('cancel');
    try {
      await cancelTask(task.id);
      if (onStatusChange) onStatusChange();
      setShowConfirmCancel(false);
      onClose();
    } catch (err) {
      setActionError(err.message || t('tasks.errors.updateFailed', 'Unable to update work status'));
    } finally {
      setLoadingAction(null);
    }
  };

  const handleSkip = async () => {
    setActionError('');
    setLoadingAction('skip');
    try {
      await skipTask(task.id);
      if (onStatusChange) onStatusChange();
      onClose();
    } catch (err) {
      setActionError(err.message || t('tasks.errors.updateFailed', 'Unable to update work status'));
    } finally {
      setLoadingAction(null);
    }
  };

  const typeConfig = WORK_TYPES_MAP[task.taskType] || WORK_TYPES_MAP.OTHER;
  const statusInfo = getStatusBadge(task.status);
  const overdue = isOverdue();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-xs animate-fade-in">
      <div
        className="bg-white rounded-3xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-hidden flex flex-col border border-gray-100 animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center space-x-3">
            <div className={`p-3 rounded-2xl ${typeConfig.color}`}>
              <Sprout className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-xl font-black text-gray-900 leading-tight">{getFormattedTitle()}</h2>
              <p className="text-xs font-bold text-gray-400 mt-0.5">
                {t(typeConfig.labelKey, typeConfig.defaultTitle)}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-all"
            aria-label="Close modal"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-sm font-semibold">
          {actionError && (
            <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-start space-x-2.5">
              <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
              <span className="text-xs font-bold">{actionError}</span>
            </div>
          )}

          {/* Status & Overdue Badges */}
          <div className="flex items-center space-x-3">
            <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-extrabold ${statusInfo.className}`}>
              {statusInfo.label}
            </span>
            {overdue && (
              <span className="inline-flex items-center rounded-full border border-red-200 bg-red-50 text-red-700 px-3 py-1 text-xs font-extrabold animate-pulse">
                <AlertTriangle className="h-3.5 w-3.5 mr-1" />
                {t('tasks.status.OVERDUE', 'OVERDUE')}
              </span>
            )}
            {task.source === 'AUTOMATION' && (
              <span className="inline-flex items-center rounded-full border border-purple-200 bg-purple-50 text-purple-700 px-3 py-1 text-xs font-extrabold">
                <Bot className="h-3.5 w-3.5 mr-1" />
                {t('tasks.source.automation', 'Auto Follow-up')}
              </span>
            )}
          </div>

          {/* Crop & Farm Context */}
          <div className="bg-emerald-50/40 rounded-2xl p-4 border border-emerald-100 space-y-2">
            <div className="flex items-center space-x-2 text-emerald-900">
              <Sprout className="h-4.5 w-4.5 text-emerald-600 shrink-0" />
              <span className="font-extrabold text-base">{task.cropName || task.crop || 'Crop'}</span>
              {task.cropVariety && (
                <span className="text-xs font-bold text-emerald-700">({task.cropVariety})</span>
              )}
            </div>
            {task.farmName && (
              <div className="flex items-center space-x-2 text-gray-500 text-xs font-bold pl-0.5">
                <MapPin className="h-4 w-4 text-emerald-600 shrink-0" />
                <span>{task.farmName}</span>
              </div>
            )}
          </div>

          {/* Due & Completed Timestamps */}
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-3.5 bg-gray-50 rounded-2xl border border-gray-150">
              <div className="flex items-center space-x-1.5 text-gray-400 font-bold mb-1">
                <Clock className="h-4 w-4 text-emerald-600" />
                <span>{t('tasks.details.dueDateLabel', 'Scheduled Date')}</span>
              </div>
              <span className="font-black text-gray-800 text-sm">
                {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : t('tasks.dueDate.today', 'Today')}
              </span>
            </div>

            <div className="p-3.5 bg-gray-50 rounded-2xl border border-gray-150">
              <div className="flex items-center space-x-1.5 text-gray-400 font-bold mb-1">
                <Calendar className="h-4 w-4 text-blue-600" />
                <span>{t('tasks.details.createdDateLabel', 'Created Date')}</span>
              </div>
              <span className="font-black text-gray-800 text-sm">
                {task.createdAt ? new Date(task.createdAt).toLocaleDateString() : 'Recently'}
              </span>
            </div>
          </div>

          {/* Description & Instructions */}
          {task.description && (
            <div className="space-y-2">
              <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center space-x-1.5">
                <FileText className="h-4 w-4 text-emerald-600" />
                <span>{t('tasks.details.instructions', 'Instructions / Notes')}</span>
              </h4>
              <div className="p-4 bg-gray-50 rounded-2xl border border-gray-150 text-gray-700 leading-relaxed font-semibold">
                {task.description}
              </div>
            </div>
          )}

          {/* Completed Timestamp */}
          {task.completedAt && (
            <div className="p-4 bg-emerald-50/60 rounded-2xl border border-emerald-200 text-emerald-900 flex items-center space-x-3">
              <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0" />
              <div>
                <span className="text-xs font-bold text-emerald-700 block">
                  {task.status === 'COMPLETED'
                    ? t('tasks.details.completedOn', 'Work Completed On')
                    : t('tasks.details.cancelledOn', 'Work Cancelled On')}
                </span>
                <span className="text-sm font-black">{new Date(task.completedAt).toLocaleString()}</span>
              </div>
            </div>
          )}
        </div>

        {/* Modal Actions Footer */}
        <div className="p-5 border-t border-gray-100 bg-gray-50/40 flex flex-col space-y-3">
          {/* Confirmation Views */}
          {showConfirmComplete && (
            <div className="p-4 bg-emerald-50 rounded-2xl border border-emerald-200 space-y-3">
              <p className="text-xs font-black text-emerald-900 text-center">
                {t('tasks.confirm.completePrompt', 'Mark this work as completed?')}
              </p>
              <div className="flex space-x-2">
                <button
                  onClick={handleComplete}
                  disabled={loadingAction === 'complete'}
                  className="flex-1 py-2.5 rounded-xl bg-emerald-650 hover:bg-emerald-750 text-white font-black text-xs transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center space-x-1"
                >
                  <Check className="h-4 w-4" />
                  <span>{loadingAction === 'complete' ? 'Updating...' : t('tasks.actions.confirmComplete', 'Yes, Mark Complete')}</span>
                </button>
                <button
                  onClick={() => setShowConfirmComplete(false)}
                  className="px-4 py-2.5 rounded-xl bg-white border border-gray-250 text-gray-700 font-bold text-xs hover:bg-gray-50 transition-all cursor-pointer"
                >
                  {t('tasks.actions.cancelModal', 'Back')}
                </button>
              </div>
            </div>
          )}

          {showConfirmCancel && (
            <div className="p-4 bg-red-50 rounded-2xl border border-red-200 space-y-3">
              <p className="text-xs font-black text-red-900 text-center">
                {t('tasks.confirm.cancelPrompt', 'Are you sure you want to cancel this field work?')}
              </p>
              <div className="flex space-x-2">
                <button
                  onClick={handleCancel}
                  disabled={loadingAction === 'cancel'}
                  className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-black text-xs transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center space-x-1"
                >
                  <XCircle className="h-4 w-4" />
                  <span>{loadingAction === 'cancel' ? 'Updating...' : t('tasks.actions.confirmCancel', 'Yes, Cancel Work')}</span>
                </button>
                <button
                  onClick={() => setShowConfirmCancel(false)}
                  className="px-4 py-2.5 rounded-xl bg-white border border-gray-250 text-gray-700 font-bold text-xs hover:bg-gray-50 transition-all cursor-pointer"
                >
                  {t('tasks.actions.cancelModal', 'Back')}
                </button>
              </div>
            </div>
          )}

          {/* Regular Action Buttons */}
          {!showConfirmComplete && !showConfirmCancel && (
            <div className="flex flex-wrap items-center justify-end gap-2.5">
              {(task.status === 'PENDING' || task.status === 'OVERDUE') && (
                <>
                  <button
                    onClick={() => setShowConfirmCancel(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-red-50 text-gray-700 hover:text-red-700 font-bold text-xs transition-all cursor-pointer"
                  >
                    {t('tasks.actions.cancel', 'Cancel Work')}
                  </button>
                  <button
                    onClick={() => setIsRescheduleOpen(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-amber-50 text-gray-700 hover:text-amber-700 font-bold text-xs flex items-center space-x-1 transition-all cursor-pointer"
                  >
                    <Calendar className="h-4 w-4" />
                    <span>{t('tasks.actions.reschedule', 'Reschedule')}</span>
                  </button>
                  <button
                    onClick={() => setIsSkipOpen(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-purple-50 text-gray-700 hover:text-purple-700 font-bold text-xs flex items-center space-x-1 transition-all cursor-pointer"
                  >
                    <FastForward className="h-4 w-4" />
                    <span>{t('tasks.actions.skip', 'Skip')}</span>
                  </button>
                  <button
                    onClick={handleStart}
                    disabled={loadingAction === 'start'}
                    className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs flex items-center space-x-1.5 transition-all cursor-pointer disabled:opacity-50"
                  >
                    <Play className="h-4 w-4 fill-current" />
                    <span>{loadingAction === 'start' ? 'Updating...' : t('tasks.actions.start', 'Start Work')}</span>
                  </button>
                  <button
                    onClick={() => setShowConfirmComplete(true)}
                    className="px-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white font-extrabold text-xs flex items-center space-x-1.5 transition-all cursor-pointer"
                  >
                    <CheckCircle className="h-4 w-4" />
                    <span>{t('tasks.actions.complete', 'Mark Complete')}</span>
                  </button>
                </>
              )}

              {task.status === 'IN_PROGRESS' && (
                <>
                  <button
                    onClick={() => setShowConfirmCancel(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-red-50 text-gray-700 hover:text-red-700 font-bold text-xs transition-all cursor-pointer"
                  >
                    {t('tasks.actions.cancel', 'Cancel Work')}
                  </button>
                  <button
                    onClick={() => setIsRescheduleOpen(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-amber-50 text-gray-700 hover:text-amber-700 font-bold text-xs flex items-center space-x-1 transition-all cursor-pointer"
                  >
                    <Calendar className="h-4 w-4" />
                    <span>{t('tasks.actions.reschedule', 'Reschedule')}</span>
                  </button>
                  <button
                    onClick={() => setIsSkipOpen(true)}
                    className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-250 hover:bg-purple-50 text-gray-700 hover:text-purple-700 font-bold text-xs flex items-center space-x-1 transition-all cursor-pointer"
                  >
                    <FastForward className="h-4 w-4" />
                    <span>{t('tasks.actions.skip', 'Skip')}</span>
                  </button>
                  <button
                    onClick={() => setShowConfirmComplete(true)}
                    className="px-4 py-2.5 rounded-xl bg-emerald-650 hover:bg-emerald-750 text-white font-extrabold text-xs flex items-center space-x-1.5 transition-all cursor-pointer disabled:opacity-50"
                  >
                    <CheckCircle className="h-4 w-4" />
                    <span>{t('tasks.actions.complete', 'Mark Complete')}</span>
                  </button>
                </>
              )}

              {(task.status === 'COMPLETED' || task.status === 'CANCELLED' || task.status === 'SKIPPED') && (
                <button
                  onClick={onClose}
                  className="px-5 py-2.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-800 font-extrabold text-xs transition-all cursor-pointer"
                >
                  {t('tasks.actions.closeModal', 'Close')}
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      <RescheduleTaskModal
        task={task}
        isOpen={isRescheduleOpen}
        onClose={() => setIsRescheduleOpen(false)}
        onSuccess={() => {
          if (onStatusChange) onStatusChange();
          onClose();
        }}
      />

      <SkipTaskModal
        task={task}
        isOpen={isSkipOpen}
        onClose={() => setIsSkipOpen(false)}
        onSuccess={() => {
          if (onStatusChange) onStatusChange();
          onClose();
        }}
      />
    </div>
  );
};

export default TaskDetailsModal;
