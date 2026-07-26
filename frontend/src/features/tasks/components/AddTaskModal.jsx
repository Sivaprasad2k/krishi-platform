import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckSquare, X, Check, Calendar } from 'lucide-react';
import { createTask } from '../api/taskApi';

const TASK_PRESETS = [
  'Prepare Foliar Spray Solution',
  'Apply Spray Application',
  'Soil Drenching / Root Application',
  'Inspect Crop Symptoms after 48 Hours',
  'Re-apply Follow-up Treatment'
];

/**
 * AddTaskModal allows farmers to schedule a action item for a treatment plan.
 */
export const AddTaskModal = ({
  isOpen,
  onClose,
  onSuccess,
  preselectedTreatmentId = null,
  treatments = []
}) => {
  const { t } = useTranslation();

  const [treatmentId, setTreatmentId] = useState(preselectedTreatmentId || '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (preselectedTreatmentId) {
      setTreatmentId(preselectedTreatmentId);
    } else if (treatments.length > 0 && !treatmentId) {
      setTreatmentId(treatments[0].id);
    }
  }, [preselectedTreatmentId, treatments]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!treatmentId) {
      setError(t('taskModal.selectTreatmentRequired', 'Please select a treatment plan'));
      return;
    }
    if (!title.trim()) {
      setError(t('taskModal.titleRequired', 'Task title is required'));
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      // Format ISO LocalDateTime String if dueDate is set
      let formattedDueDate = null;
      if (dueDate) {
        formattedDueDate = new Date(dueDate).toISOString();
      }

      await createTask({
        treatmentId,
        title: title.trim(),
        description: description.trim() || undefined,
        dueDate: formattedDueDate
      });

      setTitle('');
      setDescription('');
      setDueDate('');
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to create task:', err);
      setError(err.message || 'Failed to create task. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-fade-in">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-200 space-y-5 max-h-[90vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-start justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-emerald-50 text-emerald-650 rounded-xl">
              <CheckSquare className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-xl font-black text-gray-850">
                {t('taskModal.addModalTitle', 'Add Field Task')}
              </h3>
              <p className="text-xs font-semibold text-gray-400 mt-0.5">
                {t('taskModal.addModalSubtitle', 'Create a scheduled action item for a treatment plan')}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-600 rounded-xl hover:bg-gray-100 transition-all cursor-pointer"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="p-3.5 bg-red-50 border border-red-200 text-red-700 text-sm font-bold rounded-xl">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Treatment Selector (if not preselected) */}
          {!preselectedTreatmentId && treatments.length > 0 && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-600 block uppercase tracking-wider">
                {t('taskModal.selectTreatment', 'Select Treatment Plan')}
              </label>
              <select
                value={treatmentId}
                onChange={(e) => setTreatmentId(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-extrabold text-gray-800 bg-white focus:outline-none focus:border-emerald-500 transition-all"
              >
                {treatments.map((tr) => (
                  <option key={tr.id} value={tr.id}>
                    {tr.treatmentName} &bull; {tr.diseaseName || 'Disease'} ({tr.cropName || 'Crop'})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Task Title Input */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-gray-600 block uppercase tracking-wider">
              {t('taskModal.taskTitle', 'Task Title')} *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={t('taskModal.taskTitlePlaceholder', 'e.g. Prepare Foliar Spray Solution, Apply Spray')}
              style={{ minHeight: '48px' }}
              className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-extrabold text-gray-800 focus:outline-none focus:border-emerald-500 transition-all"
              required
            />

            {/* Quick Presets */}
            <div className="pt-1">
              <span className="text-xs font-bold text-gray-400 block mb-1.5">{t('taskModal.presetsLabel', 'Quick Task Presets:')}</span>
              <div className="flex flex-wrap gap-1.5">
                {TASK_PRESETS.map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setTitle(preset)}
                    className={`text-xs font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
                      title === preset
                        ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                        : 'bg-gray-50 text-gray-650 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    {preset}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Due Date */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-gray-600 block uppercase tracking-wider flex items-center space-x-1">
              <Calendar className="h-4 w-4 text-emerald-600" />
              <span>{t('taskModal.dueDate', 'Due Date')}</span>
            </label>
            <input
              type="datetime-local"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              style={{ minHeight: '48px' }}
              className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-semibold text-gray-800 focus:outline-none focus:border-emerald-500 transition-all"
            />
          </div>

          {/* Description / Notes */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-gray-600 block uppercase tracking-wider">
              {t('taskModal.description', 'Task Notes / Details')}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('taskModal.descriptionPlaceholder', 'Detailed instructions for field worker...')}
              rows={3}
              className="w-full p-3 rounded-xl border border-gray-300 font-semibold text-gray-800 focus:outline-none focus:border-emerald-500 transition-all"
            />
          </div>

          {/* Buttons */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              style={{ minHeight: '48px' }}
              className="px-5 py-2.5 rounded-xl border border-gray-250 font-extrabold text-gray-600 hover:bg-gray-50 active:scale-95 transition-all cursor-pointer"
            >
              {t('taskModal.actions.cancel', 'Cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{ minHeight: '48px' }}
              className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold shadow-md active:scale-95 disabled:opacity-50 transition-all cursor-pointer flex items-center space-x-2"
            >
              <Check className="h-5 w-5" />
              <span>
                {submitting ? 'Creating...' : t('taskModal.actions.submit', 'Create Task')}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddTaskModal;
