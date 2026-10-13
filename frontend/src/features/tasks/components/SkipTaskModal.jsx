import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { X, FastForward, AlertTriangle } from 'lucide-react';
import { skipTask } from '../api/taskApi';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const SUGGESTED_REASONS = [
  'Adverse Weather / Rain',
  'Soil Moisture Adequate',
  'Crop Health Satisfactory',
  'Supplies / Labor Unavailable',
  'Completed Manually'
];

/**
 * SkipTaskModal captures and validates the agronomic reason for skipping a scheduled field task.
 */
const SkipTaskModal = ({ task, isOpen, onClose, onSuccess }) => {
  const { t } = useTranslation();
  const [selectedChip, setSelectedChip] = useState('');
  const [customReason, setCustomReason] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen || !task) return null;

  const handleChipClick = (reason) => {
    setSelectedChip(reason);
    setCustomReason(reason);
    setError('');
  };

  const handleCustomChange = (e) => {
    setCustomReason(e.target.value);
    setSelectedChip('');
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const finalReason = customReason.trim();

    if (!finalReason) {
      setError(t('tasks.validation.skipReasonRequired', 'Please provide a reason for skipping this agricultural activity.'));
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await skipTask(task.id, finalReason);
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to skip task:', err);
      setError(err.message || t('tasks.errors.skipFailed', 'Failed to skip task. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-xs animate-fade-in">
      <div
        className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden flex flex-col border border-gray-100 animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-purple-50 text-purple-700 rounded-2xl shrink-0">
              <FastForward className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-xl font-black text-gray-900 leading-tight">
                {t('tasks.actions.skip', 'Skip Activity')}
              </h2>
              <p className="text-xs font-bold text-gray-500 mt-0.5 truncate max-w-[240px]">
                {task.title}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-all cursor-pointer"
            aria-label="Close modal"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5 text-sm font-semibold">
          {error && (
            <div className="p-3.5 bg-red-50 border border-red-200 text-red-700 rounded-xl flex items-start space-x-2.5 text-xs">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span className="font-bold">{error}</span>
            </div>
          )}

          <div>
            <p className="text-xs font-black text-gray-700 uppercase tracking-wider mb-2">
              {t('tasks.skipPrompt', 'Why are you skipping this activity?')}
            </p>
            <p className="text-xs text-gray-500 mb-3">
              {t('tasks.skipNotice', 'Recording the agronomic reason preserves operational history for your farm records.')}
            </p>

            {/* Quick Reason Chips */}
            <div className="flex flex-wrap gap-2 mb-3">
              {SUGGESTED_REASONS.map((reason) => (
                <ChoiceChip
                  key={reason}
                  label={reason}
                  selected={selectedChip === reason}
                  onClick={() => handleChipClick(reason)}
                  size="sm"
                />
              ))}
            </div>

            {/* Custom/Selected Reason Text Input */}
            <label htmlFor="skipReasonInput" className="block text-xs font-bold text-gray-600 mb-1">
              {t('tasks.reasonDetails', 'Reason details / Notes:')}
            </label>
            <textarea
              id="skipReasonInput"
              rows={3}
              value={customReason}
              onChange={handleCustomChange}
              placeholder={t('tasks.reasonPlaceholder', 'e.g. Heavy rainfall made irrigation unnecessary')}
              className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-bold text-sm text-gray-900 focus:outline-none focus:border-purple-600 focus:ring-1 focus:ring-purple-600 resize-none"
            />
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end space-x-3 pt-2 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2.5 rounded-xl border border-gray-250 text-gray-700 font-bold text-xs hover:bg-gray-50 transition-all cursor-pointer"
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-black text-xs transition-all shadow-xs active:scale-95 cursor-pointer disabled:opacity-50 flex items-center space-x-1.5"
            >
              <FastForward className="h-4 w-4" />
              <span>{submitting ? 'Skipping...' : t('tasks.actions.confirmSkip', 'Confirm Skip')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SkipTaskModal;
