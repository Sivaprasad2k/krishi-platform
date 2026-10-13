import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, X, AlertTriangle } from 'lucide-react';
import { rescheduleTask } from '../api/taskApi';

export const RescheduleTaskModal = ({ task, isOpen, onClose, onSuccess }) => {
  const { t } = useTranslation();
  const [dueDate, setDueDate] = useState(
    task?.dueDate ? task.dueDate.split('T')[0] : new Date().toISOString().split('T')[0]
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen || !task) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!dueDate) return;

    setIsSubmitting(true);
    setError(null);
    try {
      await rescheduleTask(task.id, dueDate);
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to reschedule task:', err);
      setError(err.response?.data?.message || err.message || t('tasks.errors.rescheduleFailed', 'Failed to reschedule task.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-xs animate-fade-in">
      <div
        className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden flex flex-col border border-gray-100 animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-5 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center space-x-2.5">
            <div className="p-2.5 rounded-2xl bg-amber-50 text-amber-700">
              <Calendar className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-black text-gray-900">
                {t('tasks.reschedule.title', 'Reschedule Activity')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {task.title}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-all"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl flex items-center space-x-2 text-xs font-bold">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-black text-gray-600 uppercase tracking-wider block">
              {t('tasks.reschedule.newDateLabel', 'New Due Date')}
            </label>
            <input
              type="date"
              required
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              style={{ minHeight: '44px' }}
              className="w-full px-4 rounded-xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 text-sm text-gray-850"
            />
            <p className="text-[11px] font-semibold text-gray-400">
              {t('tasks.reschedule.helper', 'Select the date when you plan to perform this cultivation activity.')}
            </p>
          </div>

          <div className="flex items-center justify-end space-x-2.5 pt-3 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl border border-gray-250 bg-white hover:bg-gray-50 text-gray-700 font-bold text-xs transition-all cursor-pointer"
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !dueDate}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-black text-xs transition-all shadow-xs cursor-pointer"
            >
              {isSubmitting ? t('tasks.reschedule.saving', 'Rescheduling...') : t('tasks.reschedule.confirm', 'Confirm Reschedule')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RescheduleTaskModal;
