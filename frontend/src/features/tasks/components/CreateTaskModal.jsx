import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Droplets, Sprout, ShieldAlert, Eye, Scissors, Sun, Wrench, Check, Edit2 } from 'lucide-react';
import { createTask } from '../api/taskApi';
import apiClient from '../../../core/api/axios';
import OptionCard from '../../../shared/components/OptionCard';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const CATEGORIZED_WORK_TYPES = [
  {
    categoryKey: 'tasks.categories.watering',
    categoryDefault: 'Watering',
    types: [
      {
        id: 'IRRIGATION',
        labelKey: 'tasks.types.IRRIGATION',
        defaultTitle: 'Water Crop',
        descKey: 'tasks.typeDesc.IRRIGATION',
        defaultDesc: 'Irrigate this crop with water',
        icon: Droplets,
        placeholderKey: 'tasks.placeholders.IRRIGATION',
        defaultPlaceholder: 'Example: Water each plant for 15 minutes',
      },
    ],
  },
  {
    categoryKey: 'tasks.categories.nutrition',
    categoryDefault: 'Crop Nutrition',
    types: [
      {
        id: 'FERTILIZER_APPLICATION',
        labelKey: 'tasks.types.FERTILIZER_APPLICATION',
        defaultTitle: 'Apply Fertilizer',
        descKey: 'tasks.typeDesc.FERTILIZER_APPLICATION',
        defaultDesc: 'Add fertilizer to nourish crop',
        icon: Sprout,
        placeholderKey: 'tasks.placeholders.FERTILIZER_APPLICATION',
        defaultPlaceholder: 'Example: Apply 50g NPK around each plant',
      },
      {
        id: 'MANURE_APPLICATION',
        labelKey: 'tasks.types.MANURE_APPLICATION',
        defaultTitle: 'Add Manure / Compost',
        descKey: 'tasks.typeDesc.MANURE_APPLICATION',
        defaultDesc: 'Add organic compost or manure',
        icon: Sprout,
        placeholderKey: 'tasks.placeholders.MANURE_APPLICATION',
        defaultPlaceholder: 'Example: Add 1 bucket compost per tree',
      },
    ],
  },
  {
    categoryKey: 'tasks.categories.maintenance',
    categoryDefault: 'Field Maintenance',
    types: [
      {
        id: 'WEEDING',
        labelKey: 'tasks.types.WEEDING',
        defaultTitle: 'Remove Weeds',
        descKey: 'tasks.typeDesc.WEEDING',
        defaultDesc: 'Clear unwanted wild plants',
        icon: Scissors,
        placeholderKey: 'tasks.placeholders.WEEDING',
        defaultPlaceholder: 'Example: Clear weeds around banana plants',
      },
      {
        id: 'PRUNING',
        labelKey: 'tasks.types.PRUNING',
        defaultTitle: 'Prune Plants',
        descKey: 'tasks.typeDesc.PRUNING',
        defaultDesc: 'Trim dead branches or leaves',
        icon: Scissors,
        placeholderKey: 'tasks.placeholders.PRUNING',
        defaultPlaceholder: 'Example: Trim lower yellow leaves',
      },
    ],
  },
  {
    categoryKey: 'tasks.categories.protection',
    categoryDefault: 'Crop Protection',
    types: [
      {
        id: 'PEST_CONTROL',
        labelKey: 'tasks.types.PEST_CONTROL',
        defaultTitle: 'Pest Control',
        descKey: 'tasks.typeDesc.PEST_CONTROL',
        defaultDesc: 'Control insects or pests',
        icon: ShieldAlert,
        placeholderKey: 'tasks.placeholders.PEST_CONTROL',
        defaultPlaceholder: 'Example: Spray neem oil mixture in morning',
      },
      {
        id: 'DISEASE_TREATMENT',
        labelKey: 'tasks.types.DISEASE_TREATMENT',
        defaultTitle: 'Disease Treatment',
        descKey: 'tasks.typeDesc.DISEASE_TREATMENT',
        defaultDesc: 'Treat fungal or bacterial disease',
        icon: ShieldAlert,
        placeholderKey: 'tasks.placeholders.DISEASE_TREATMENT',
        defaultPlaceholder: 'Example: Apply copper fungicide spray',
      },
    ],
  },
  {
    categoryKey: 'tasks.categories.monitoring',
    categoryDefault: 'Monitoring & Harvest',
    types: [
      {
        id: 'CROP_INSPECTION',
        labelKey: 'tasks.types.CROP_INSPECTION',
        defaultTitle: 'Inspect Crop Health',
        descKey: 'tasks.typeDesc.CROP_INSPECTION',
        defaultDesc: 'Check crop growth and leaf health',
        icon: Eye,
        placeholderKey: 'tasks.placeholders.CROP_INSPECTION',
        defaultPlaceholder: 'Example: Check under leaves for yellow spots',
      },
      {
        id: 'HARVESTING',
        labelKey: 'tasks.types.HARVESTING',
        defaultTitle: 'Harvest Crop',
        descKey: 'tasks.typeDesc.HARVESTING',
        defaultDesc: 'Harvest mature crop yield',
        icon: Sun,
        placeholderKey: 'tasks.placeholders.HARVESTING',
        defaultPlaceholder: 'Example: Harvest ripe fruits',
      },
    ],
  },
  {
    categoryKey: 'tasks.categories.other',
    categoryDefault: 'Other Activities',
    types: [
      {
        id: 'OTHER',
        labelKey: 'tasks.types.OTHER',
        defaultTitle: 'Field Work',
        descKey: 'tasks.typeDesc.OTHER',
        defaultDesc: 'General agricultural field work',
        icon: Wrench,
        placeholderKey: 'tasks.placeholders.OTHER',
        defaultPlaceholder: 'Example: General field maintenance',
      },
    ],
  },
];

export const CreateTaskModal = ({
  isOpen,
  onClose,
  onSuccess,
  preselectedPlantingId = null,
  preselectedTreatmentId = null,
}) => {
  const { t } = useTranslation();

  const [plantings, setPlantings] = useState([]);
  const [cropPlantingId, setCropPlantingId] = useState(preselectedPlantingId || '');
  const [treatmentId, setTreatmentId] = useState(preselectedTreatmentId || null);
  const [taskType, setTaskType] = useState(preselectedTreatmentId ? 'DISEASE_TREATMENT' : 'IRRIGATION');
  const [title, setTitle] = useState(t('tasks.types.IRRIGATION', 'Water Crop'));
  const [showCustomTitle, setShowCustomTitle] = useState(false);
  const [description, setDescription] = useState('');
  const [dueDateOption, setDueDateOption] = useState('TODAY'); // TODAY, TOMORROW, CUSTOM
  const [customDueDate, setCustomDueDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // Find active type object
  const activeTypeObj = CATEGORIZED_WORK_TYPES.flatMap((c) => c.types).find((t) => t.id === taskType) || CATEGORIZED_WORK_TYPES[0].types[0];

  const loadPlantings = async () => {
    try {
      const farmsRes = await apiClient.get('/farms');
      const farms = farmsRes.data?.data || [];
      let list = [];

      for (const farm of farms) {
        const cyclesRes = await apiClient.get(`/farms/${farm.id}/crop-cycles`);
        const cycles = cyclesRes.data?.data || [];
        for (const cycle of cycles) {
          const plantingsRes = await apiClient.get(`/crop-cycles/${cycle.id}/plantings`);
          const pList = plantingsRes.data?.data || [];
          for (const p of pList) {
            list.push({
              ...p,
              farmName: farm.name,
              cycleName: cycle.name,
            });
          }
        }
      }

      setPlantings(list);
      if (list.length > 0 && !cropPlantingId) {
        setCropPlantingId(list[0].id);
      }
    } catch (err) {
      console.error('Failed to load crop plantings for modal:', err);
    }
  };

  useEffect(() => {
    if (preselectedPlantingId) {
      setCropPlantingId(preselectedPlantingId);
    } else if (isOpen) {
      loadPlantings();
    }
    if (preselectedTreatmentId) {
      setTreatmentId(preselectedTreatmentId);
      setTaskType('DISEASE_TREATMENT');
      setTitle(t('tasks.types.DISEASE_TREATMENT', 'Disease Treatment'));
    }
  }, [isOpen, preselectedPlantingId, preselectedTreatmentId]);

  const handleSelectWorkType = (typeObj) => {
    setTaskType(typeObj.id);
    if (!showCustomTitle) {
      setTitle(t(typeObj.labelKey, typeObj.defaultTitle));
    }
  };

  const computeDueDate = () => {
    const now = new Date();
    if (dueDateOption === 'TODAY') {
      now.setHours(18, 0, 0, 0);
      return now.toISOString();
    }
    if (dueDateOption === 'TOMORROW') {
      now.setDate(now.getDate() + 1);
      now.setHours(18, 0, 0, 0);
      return now.toISOString();
    }
    if (dueDateOption === 'CUSTOM' && customDueDate) {
      return new Date(customDueDate).toISOString();
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!cropPlantingId) {
      setError(t('tasks.form.errors.cropRequired', 'Please select a crop'));
      return;
    }
    if (!title.trim()) {
      setError(t('tasks.form.errors.titleRequired', 'Work title is required'));
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await createTask({
        cropPlantingId,
        treatmentId: treatmentId || undefined,
        taskType,
        title: title.trim(),
        description: description.trim() || undefined,
        dueDate: computeDueDate(),
      });

      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to create task:', err);
      setError(err.response?.data?.message || err.message || t('common.errors.generic', 'Failed to create work task'));
    } finally {
      setSubmitting(false);
    }
  };

  const formatCustomDateLabel = () => {
    if (!customDueDate) return t('tasks.dueDate.pickDate', 'Pick Date');
    const d = new Date(customDueDate);
    if (isNaN(d.getTime())) return t('tasks.dueDate.pickDate', 'Pick Date');
    return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 overflow-y-auto">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl space-y-6 my-8 animate-in fade-in zoom-in-95 duration-150">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-gray-100 pb-4">
          <div>
            <h2 className="text-2xl font-black text-gray-850">{t('tasks.modal.title', 'Add Field Work')}</h2>
            <p className="text-sm font-semibold text-gray-400 mt-1">{t('tasks.modal.subtitle', 'What agricultural work needs to be done?')}</p>
          </div>
          <button
            onClick={onClose}
            style={{ minHeight: '44px', minWidth: '44px' }}
            className="p-2 rounded-xl text-gray-400 hover:bg-gray-100 transition-all cursor-pointer flex items-center justify-center"
            aria-label="Close Modal"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-sm font-bold text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 1. SELECT CROP (if not preselected) */}
          {!preselectedPlantingId && (
            <div className="space-y-2">
              <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('tasks.questions.whichCrop', 'Which crop is this work for?')}
              </label>
              <select
                value={cropPlantingId}
                onChange={(e) => setCropPlantingId(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full p-3 rounded-2xl border border-gray-250 font-bold text-base text-gray-800 focus:outline-hidden focus:border-emerald-500 bg-gray-50/50 cursor-pointer"
              >
                {plantings.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.cropName} ({p.cropVariety || 'Standard'}) &bull; {p.farmName}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* 2. CATEGORIZED ACTIVITY SELECTION (NO INNER SCROLLBAR) */}
          <div className="space-y-4">
            <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
              {t('tasks.questions.whatWork', 'What work needs to be done?')}
            </label>

            <div className="space-y-5">
              {CATEGORIZED_WORK_TYPES.map((cat) => (
                <div key={cat.categoryKey} className="space-y-2">
                  <h3 className="text-xs font-black text-emerald-800 uppercase tracking-wider bg-emerald-50/80 px-3 py-1 rounded-lg inline-block">
                    {t(cat.categoryKey, cat.categoryDefault)}
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {cat.types.map((wt) => (
                      <OptionCard
                        key={wt.id}
                        icon={wt.icon}
                        title={t(wt.labelKey, wt.defaultTitle)}
                        description={t(wt.descKey, wt.defaultDesc)}
                        selected={taskType === wt.id}
                        onClick={() => handleSelectWorkType(wt)}
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 3. CONTEXTUAL INSTRUCTIONS & AUTO TITLE */}
          <div className="space-y-4 pt-2 border-t border-gray-100">
            {/* Title display & customize toggle */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                  {t('tasks.form.titleLabel', 'Work Name')}
                </label>
                {!showCustomTitle && (
                  <button
                    type="button"
                    onClick={() => setShowCustomTitle(true)}
                    className="text-xs font-extrabold text-emerald-700 hover:text-emerald-800 flex items-center space-x-1 cursor-pointer"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                    <span>{t('tasks.form.customizeName', 'Change name')}</span>
                  </button>
                )}
              </div>

              {showCustomTitle ? (
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{ minHeight: '48px' }}
                  className="w-full p-3 rounded-2xl border border-gray-250 font-bold text-base text-gray-800 focus:outline-hidden focus:border-emerald-500"
                />
              ) : (
                <div className="p-3 bg-gray-50 border border-gray-200 rounded-2xl flex items-center justify-between">
                  <span className="font-extrabold text-base text-gray-850">{title}</span>
                  <span className="text-xs font-bold text-gray-400">{t('tasks.form.autoGenerated', 'Auto-set')}</span>
                </div>
              )}
            </div>

            {/* DUE DATE SELECTION */}
            <div className="space-y-2">
              <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('tasks.form.whenLabel', 'When should this work be done?')}
              </label>
              <div className="grid grid-cols-3 gap-2.5">
                <ChoiceChip
                  label={t('tasks.dueDate.today', 'Today')}
                  selected={dueDateOption === 'TODAY'}
                  onClick={() => setDueDateOption('TODAY')}
                />
                <ChoiceChip
                  label={t('tasks.dueDate.tomorrow', 'Tomorrow')}
                  selected={dueDateOption === 'TOMORROW'}
                  onClick={() => setDueDateOption('TOMORROW')}
                />
                <ChoiceChip
                  label={formatCustomDateLabel()}
                  selected={dueDateOption === 'CUSTOM'}
                  onClick={() => setDueDateOption('CUSTOM')}
                />
              </div>

              {dueDateOption === 'CUSTOM' && (
                <input
                  type="date"
                  value={customDueDate}
                  onChange={(e) => setCustomDueDate(e.target.value)}
                  style={{ minHeight: '48px' }}
                  className="mt-2 w-full p-3 rounded-2xl border border-gray-250 text-sm font-bold text-gray-800 bg-white"
                />
              )}
            </div>

            {/* CONTEXTUAL INSTRUCTIONS */}
            <div className="space-y-1.5">
              <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('tasks.form.notesLabel', 'Instructions or Notes')}
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t(activeTypeObj.placeholderKey, activeTypeObj.defaultPlaceholder)}
                className="w-full p-3.5 rounded-2xl border border-gray-250 text-sm font-semibold text-gray-800 focus:outline-hidden focus:border-emerald-500"
              />
            </div>
          </div>

          {/* ACTIONS STICKY FOOTER */}
          <div className="flex items-center space-x-3 pt-4 border-t border-[#C5DCC9] sticky bottom-0 bg-white z-10">
            <button
              type="button"
              onClick={onClose}
              style={{ minHeight: '48px' }}
              className="flex-1 py-3 rounded-2xl border border-[#C5DCC9] text-sm font-black text-[#525252] hover:bg-[#D7EDDB] active:scale-95 transition-all cursor-pointer"
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{ minHeight: '48px' }}
              className="flex-1 py-3 rounded-2xl bg-[#087A55] hover:bg-[#066747] active:scale-95 disabled:opacity-50 text-white text-sm font-black transition-all shadow-sm cursor-pointer flex items-center justify-center space-x-1.5"
            >
              {submitting ? (
                <span>{t('common.actions.saving', 'Scheduling...')}</span>
              ) : (
                <>
                  <Check className="h-5 w-5" />
                  <span>{t('tasks.form.saveButton', 'Schedule Work')}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateTaskModal;
