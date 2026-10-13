import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Sprout, X, Check, AlertTriangle, BookOpen } from 'lucide-react';
import { fetchPublishedTemplates, generateCultivationPlan } from '../api/cultivationPlanApi';

export const GeneratePlanModal = ({ plantingId, cropName, isOpen, onClose, onSuccess }) => {
  const { t } = useTranslation();
  const [templates, setTemplates] = useState([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isOpen) return;

    const loadTemplates = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const list = await fetchPublishedTemplates();
        setTemplates(list);
        // Preselect template matching cropName if available
        if (list.length > 0) {
          const match = list.find((tpl) =>
            cropName && tpl.cropName.toLowerCase().includes(cropName.toLowerCase())
          );
          setSelectedTemplateId(match ? match.id : list[0].id);
        }
      } catch (err) {
        console.error('Failed to load published templates:', err);
        setError(t('crops.plan.errLoadTemplates', 'Unable to load agricultural cultivation catalog.'));
      } finally {
        setIsLoading(false);
      }
    };

    loadTemplates();
  }, [isOpen, cropName]);

  if (!isOpen) return null;

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!plantingId) return;

    setIsSubmitting(true);
    setError(null);
    try {
      await generateCultivationPlan(plantingId, selectedTemplateId || undefined);
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to generate cultivation plan:', err);
      setError(err.response?.data?.message || err.message || t('crops.plan.errGenerate', 'Failed to generate cultivation plan.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-xs animate-fade-in">
      <div
        className="bg-white rounded-3xl shadow-2xl max-w-lg w-full overflow-hidden flex flex-col border border-gray-100 animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-5 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center space-x-2.5">
            <div className="p-2.5 rounded-2xl bg-emerald-50 text-emerald-700">
              <Sprout className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-black text-gray-900">
                {t('crops.plan.modalTitle', 'Generate Cultivation Plan')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.plan.modalSubtitle', 'Apply validated agronomic guidance to schedule field activities')}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-all cursor-pointer"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl flex items-center space-x-2 text-xs font-bold">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {isLoading ? (
            <div className="py-8 text-center text-xs font-bold text-gray-400 animate-pulse">
              {t('common.loading', 'Loading cultivation templates...')}
            </div>
          ) : templates.length === 0 ? (
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl text-xs font-bold text-amber-800">
              {t('crops.plan.noTemplates', 'No published cultivation templates found for this crop in the agricultural catalog.')}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-black text-gray-600 uppercase tracking-wider block">
                  {t('crops.plan.selectTemplate', 'Select Cultivation Protocol')}
                </label>
                <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                  {templates.map((tpl) => {
                    const isSelected = tpl.id === selectedTemplateId;
                    return (
                      <div
                        key={tpl.id}
                        onClick={() => setSelectedTemplateId(tpl.id)}
                        className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start justify-between ${
                          isSelected
                            ? 'border-emerald-600 bg-emerald-50/50 shadow-xs'
                            : 'border-gray-200 bg-white hover:border-emerald-300'
                        }`}
                      >
                        <div className="space-y-1 pr-2">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-black text-gray-850">{tpl.cropName}</span>
                            <span className="text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800">
                              {tpl.sourceVersion}
                            </span>
                          </div>
                          <p className="text-xs font-semibold text-gray-500">
                            {tpl.recommendationSource} &bull; {tpl.durationDays} {t('crops.plan.days', 'days')}
                          </p>
                          {tpl.regionalApplicability && (
                            <p className="text-[11px] font-bold text-gray-400">
                              {tpl.regionalApplicability}
                            </p>
                          )}
                        </div>
                        <div
                          className={`h-5 w-5 rounded-full border flex items-center justify-center shrink-0 mt-0.5 ${
                            isSelected
                              ? 'border-emerald-600 bg-emerald-600 text-white'
                              : 'border-gray-300'
                          }`}
                        >
                          {isSelected && <Check className="h-3 w-3 stroke-[3]" />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {selectedTemplate && (
                <div className="p-3.5 bg-gray-50 rounded-2xl border border-gray-200 text-xs space-y-1">
                  <div className="flex items-center space-x-1.5 text-gray-700 font-extrabold">
                    <BookOpen className="h-3.5 w-3.5 text-emerald-600" />
                    <span>{t('crops.plan.sourceTitle', 'Agronomic Foundation')}</span>
                  </div>
                  <p className="text-gray-500 font-semibold leading-relaxed">
                    {t('crops.plan.sourceNotice', 'This plan schedules automated tasks across crop growth stages according to certified package of practices.')}
                  </p>
                </div>
              )}
            </div>
          )}

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
              disabled={isSubmitting || !selectedTemplateId}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-black text-xs transition-all shadow-xs cursor-pointer flex items-center space-x-1.5"
            >
              <Sprout className="h-4 w-4" />
              <span>{isSubmitting ? t('crops.plan.generating', 'Generating...') : t('crops.plan.generateBtn', 'Generate Cultivation Plan')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default GeneratePlanModal;
