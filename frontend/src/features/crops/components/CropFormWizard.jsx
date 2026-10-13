import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ArrowRight, Check, BookOpen, Sparkles, Layers } from 'lucide-react';
import CropTypeSelector from './CropTypeSelector';
import ChoiceChip from '../../../shared/components/ChoiceChip';
import { fetchPublishedTemplates } from '../api/cultivationPlanApi';

export const CropFormWizard = ({ onSubmit, isSubmitting, initialValues = {} }) => {
  const { t } = useTranslation();
  const [step, setStep] = useState(1);
  const [cropType, setCropType] = useState(initialValues.cropName || '');
  const [customCropName, setCustomCropName] = useState(
    initialValues.cropName && !['Paddy', 'Wheat', 'Banana', 'Coconut', 'Tomato'].includes(initialValues.cropName)
      ? initialValues.cropName
      : ''
  );

  const [cropVariety, setCropVariety] = useState(initialValues.cropVariety || '');
  const [unknownVariety, setUnknownVariety] = useState(
    initialValues.cropVariety === 'Local/Traditional'
  );

  const [plantingDate, setPlantingDate] = useState(
    initialValues.plantingDate
      ? new Date(initialValues.plantingDate).toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0]
  );

  const [expectedHarvestDate, setExpectedHarvestDate] = useState(
    initialValues.expectedHarvestDate
      ? new Date(initialValues.expectedHarvestDate).toISOString().split('T')[0]
      : ''
  );

  // Dynamic Knowledge Catalog State
  const [templates, setTemplates] = useState([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState(initialValues.templateId || '');

  // Load published templates from agricultural catalog
  useEffect(() => {
    const loadTemplates = async () => {
      try {
        const list = await fetchPublishedTemplates();
        setTemplates(list);
      } catch (err) {
        console.error('Failed to load published templates:', err);
      }
    };
    loadTemplates();
  }, []);

  // Find templates matching the chosen crop
  const effectiveCropName = cropType === 'Other' ? customCropName.trim() : cropType;
  const matchingTemplates = templates.filter((tpl) => {
    if (!effectiveCropName) return false;
    const eff = effectiveCropName.toLowerCase();
    const tName = tpl.cropName.toLowerCase();
    return (
      tName.includes(eff) ||
      eff.includes(tName) ||
      (eff.includes('paddy') && tName.includes('rice')) ||
      (eff.includes('rice') && tName.includes('paddy'))
    );
  });

  // Automatically select best template when crop changes
  useEffect(() => {
    if (matchingTemplates.length > 0) {
      if (!selectedTemplateId || !matchingTemplates.some(t => t.id === selectedTemplateId)) {
        setSelectedTemplateId(matchingTemplates[0].id);
      }
    } else {
      setSelectedTemplateId('');
    }
  }, [cropType, customCropName, templates]);

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);

  // Derive dynamic variety suggestions from the chosen template (not hardcoded)
  const currentVarietySuggestions = React.useMemo(() => {
    if (!selectedTemplate?.variety) return [];
    return selectedTemplate.variety
      .split(/[,/]/)
      .map(s => s.trim())
      .filter(s => s.length > 0);
  }, [selectedTemplate]);

  useEffect(() => {
    if (unknownVariety) {
      setCropVariety('Local/Traditional');
    } else if (cropVariety === 'Local/Traditional') {
      setCropVariety('');
    }
  }, [unknownVariety]);

  // Dynamically calculate expected harvest date from agronomic template duration (not hardcoded)
  useEffect(() => {
    if (!initialValues.expectedHarvestDate && selectedTemplate?.durationDays) {
      const baseDate = plantingDate ? new Date(plantingDate) : new Date();
      const d = new Date(baseDate);
      d.setDate(d.getDate() + selectedTemplate.durationDays);
      setExpectedHarvestDate(d.toISOString().split('T')[0]);
    }
  }, [selectedTemplate, plantingDate]);

  const handleNext = () => {
    if (step === 1 && !cropType) return;
    if (step === 1 && cropType === 'Other' && !customCropName.trim()) return;
    if (step === 2 && !cropVariety.trim()) return;
    if (step === 3 && !plantingDate) return;
    setStep(step + 1);
  };

  const handleBack = () => {
    setStep(step - 1);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const finalCropName = cropType === 'Other' ? customCropName.trim() : cropType;
    const pDate = plantingDate ? `${plantingDate}T08:00:00` : null;
    const ehDate = expectedHarvestDate ? `${expectedHarvestDate}T18:00:00` : null;

    onSubmit({
      cropName: finalCropName,
      cropVariety: cropVariety.trim(),
      plantingDate: pDate,
      expectedHarvestDate: ehDate,
      templateId: selectedTemplateId || undefined
    });
  };

  const finalCropNameDisplay = cropType === 'Other' ? customCropName : cropType;

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Step Indicators */}
      <div className="flex items-center justify-between border-b border-gray-150 pb-4">
        {[1, 2, 3, 4, 5].map((s) => (
          <div key={s} className="flex items-center">
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-full border text-xs font-black transition-all ${
                s === step
                  ? 'border-emerald-600 bg-emerald-600 text-white shadow-xs'
                  : s < step
                  ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                  : 'border-gray-200 bg-white text-gray-400'
              }`}
            >
              {s < step ? <Check className="h-4.5 w-4.5" /> : s}
            </span>
            {s < 5 && (
              <div
                className={`h-0.5 w-6 sm:w-12 mx-1 rounded ${
                  s < step ? 'bg-emerald-500' : 'bg-gray-150'
                }`}
              />
            )}
          </div>
        ))}
      </div>

      {/* Step content */}
      <div className="min-h-[220px]">
        {/* STEP 1: Select Crop Type */}
        {step === 1 && (
          <div className="space-y-4">
            <div className="space-y-1">
              <h2 className="text-xl font-black text-gray-850">
                {t('crops.wizard.step1.title', 'What crop did you plant?')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.wizard.step1.desc', 'Select the crop currently cultivated in this section.')}
              </p>
            </div>
            <CropTypeSelector value={cropType} onChange={setCropType} />
            {cropType === 'Other' && (
              <div className="space-y-2 pt-2 animate-fade-in">
                <label className="text-xs font-black text-gray-500 uppercase tracking-wider block">
                  {t('crops.wizard.step1.customLabel', 'Specify Crop Name')}
                </label>
                <input
                  type="text"
                  required
                  placeholder={t('crops.wizard.step1.customPlaceholder', 'e.g. Mustard, Cotton')}
                  value={customCropName}
                  onChange={(e) => setCustomCropName(e.target.value)}
                  style={{ minHeight: '48px' }}
                  className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 text-base"
                />
              </div>
            )}

            {/* Cultivation Protocol Selection if matching templates exist */}
            {matchingTemplates.length > 0 && (
              <div className="pt-2 space-y-2">
                <label className="text-xs font-black text-emerald-800 uppercase tracking-wider block flex items-center space-x-1.5">
                  <Sparkles className="h-3.5 w-3.5 text-emerald-600 inline" />
                  <span>{t('crops.wizard.step1.templateAvailable', 'Agricultural Cultivation Plan Available')}</span>
                </label>
                <div className="space-y-2">
                  {matchingTemplates.map((tpl) => (
                    <div
                      key={tpl.id}
                      onClick={() => setSelectedTemplateId(tpl.id)}
                      className={`p-3 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                        selectedTemplateId === tpl.id
                          ? 'border-emerald-600 bg-emerald-50/70 shadow-xs'
                          : 'border-gray-200 bg-white hover:border-emerald-300'
                      }`}
                    >
                      <div className="space-y-0.5">
                        <div className="flex items-center space-x-2">
                          <span className="text-xs font-black text-gray-850">{tpl.cropName}</span>
                          <span className="text-[10px] font-extrabold uppercase px-1.5 py-0.2 rounded bg-emerald-100 text-emerald-800">
                            {tpl.sourceVersion}
                          </span>
                        </div>
                        <p className="text-[11px] font-semibold text-gray-500">
                          {tpl.recommendationSource} &bull; {tpl.durationDays} days
                        </p>
                      </div>
                      <div
                        className={`h-4 w-4 rounded-full border flex items-center justify-center shrink-0 ${
                          selectedTemplateId === tpl.id
                            ? 'border-emerald-600 bg-emerald-600 text-white'
                            : 'border-gray-300'
                        }`}
                      >
                        {selectedTemplateId === tpl.id && <Check className="h-2.5 w-2.5 stroke-[3]" />}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* STEP 2: Variety */}
        {step === 2 && (
          <div className="space-y-5">
            <div className="space-y-1">
              <h2 className="text-xl font-black text-gray-850">
                {t('crops.wizard.step2.title', 'What variety is this crop?')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.wizard.step2.desc', 'Enter or select the specific variety for accurate care recommendations.')}
              </p>
            </div>

            <div className="space-y-3">
              <label className="text-xs font-black text-gray-500 uppercase tracking-wider block">
                {t('crops.wizard.step2.label', 'Crop Variety')}
              </label>
              <input
                type="text"
                required
                disabled={unknownVariety}
                placeholder={t('crops.wizard.step2.placeholder', 'e.g. Samba Masoori, Arka Rakshak, HD-2967')}
                value={cropVariety}
                onChange={(e) => setCropVariety(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 disabled:bg-gray-50 disabled:text-gray-400 text-base text-gray-850"
              />

              {/* Dynamic Variety Suggestion Chips from Agricultural Catalog */}
              {currentVarietySuggestions.length > 0 && !unknownVariety && (
                <div className="space-y-1.5 pt-1">
                  <span className="text-xs font-bold text-gray-400">
                    {t('crops.wizard.step2.suggestionsLabel', 'Catalog Recommended Varieties:')}
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {currentVarietySuggestions.map((v) => (
                      <ChoiceChip
                        key={v}
                        label={v}
                        selected={cropVariety === v}
                        onClick={() => setCropVariety(v)}
                        size="sm"
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>

            <label className="flex items-center space-x-3 p-3.5 bg-gray-50/80 rounded-2xl border border-gray-200 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={unknownVariety}
                onChange={(e) => setUnknownVariety(e.target.checked)}
                className="h-5 w-5 rounded border-gray-300 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
              />
              <span className="text-sm font-extrabold text-gray-800">
                {t('crops.wizard.step2.unknownToggle', "I don't know / Local Variety")}
              </span>
            </label>
          </div>
        )}

        {/* STEP 3: Date Planted */}
        {step === 3 && (
          <div className="space-y-4">
            <div className="space-y-1">
              <h2 className="text-xl font-black text-gray-850">
                {t('crops.wizard.step3.title', 'When did you plant this crop?')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.wizard.step3.desc', 'Date sowed or transplanted in the soil.')}
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black text-gray-500 uppercase tracking-wider block">
                {t('crops.wizard.step3.label', 'Date Planted')}
              </label>
              <input
                type="date"
                required
                value={plantingDate}
                onChange={(e) => setPlantingDate(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 text-base text-gray-850"
              />
            </div>
          </div>
        )}

        {/* STEP 4: Expected Harvest */}
        {step === 4 && (
          <div className="space-y-4">
            <div className="space-y-1">
              <h2 className="text-xl font-black text-gray-850">
                {t('crops.wizard.step4.title', 'When do you expect to harvest?')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.wizard.step4.desc', 'Estimated date when harvest will begin.')}
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black text-gray-500 uppercase tracking-wider block">
                {t('crops.wizard.step4.label', 'Expected Harvest Date')}
              </label>
              <input
                type="date"
                value={expectedHarvestDate}
                onChange={(e) => setExpectedHarvestDate(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 text-base text-gray-850"
              />
            </div>

            {selectedTemplate && (
              <div className="p-4 bg-emerald-50/80 rounded-2xl border border-emerald-200 flex items-start space-x-3 text-xs font-bold text-emerald-900">
                <Sparkles className="h-5 w-5 shrink-0 text-emerald-600 mt-0.5" />
                <div className="space-y-1">
                  <span>
                    {t('crops.wizard.step4.suggestTemplate', 'Cultivation cycle: ~{{days}} days based on {{source}} ({{version}}).', {
                      days: selectedTemplate.durationDays,
                      source: selectedTemplate.recommendationSource,
                      version: selectedTemplate.sourceVersion
                    })}
                  </span>
                  <span className="block font-semibold text-emerald-700">
                    {t('crops.wizard.step4.autoPlanNotice', 'Automated field activities and stage tracking will be scheduled.')}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* STEP 5: Review */}
        {step === 5 && (
          <div className="space-y-4">
            <div className="space-y-1">
              <h2 className="text-xl font-black text-gray-850">
                {t('crops.wizard.step5.title', 'Review crop details')}
              </h2>
              <p className="text-xs font-semibold text-gray-400">
                {t('crops.wizard.step5.desc', 'Please review before recording this crop planting.')}
              </p>
            </div>

            <div className="rounded-2xl border border-gray-200 p-5 bg-gray-50/80 space-y-3.5 text-sm font-bold text-gray-700">
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('crops.wizard.review.cropType', 'Crop Name')}:</span>
                <span className="font-black text-gray-850">{finalCropNameDisplay}</span>
              </div>
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('crops.wizard.review.variety', 'Variety')}:</span>
                <span className="font-black text-gray-850">{cropVariety}</span>
              </div>
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('crops.wizard.review.datePlanted', 'Date Planted')}:</span>
                <span className="font-black text-gray-850">
                  {plantingDate ? new Date(plantingDate).toLocaleDateString() : ''}
                </span>
              </div>
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('crops.wizard.review.expectedHarvest', 'Expected Harvest')}:</span>
                <span className="font-black text-gray-850">
                  {expectedHarvestDate ? new Date(expectedHarvestDate).toLocaleDateString() : t('crops.wizard.review.none', 'Not specified')}
                </span>
              </div>
              {selectedTemplate && (
                <div className="flex justify-between items-center pt-1 text-emerald-900">
                  <span className="font-bold text-emerald-700 uppercase text-xs tracking-wider flex items-center space-x-1">
                    <BookOpen className="h-3.5 w-3.5 inline mr-1" />
                    <span>{t('crops.wizard.review.plan', 'Cultivation Plan')}:</span>
                  </span>
                  <span className="font-black text-xs text-right">
                    {selectedTemplate.cropName} &bull; {selectedTemplate.sourceVersion}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Wizard Actions Footer */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-150 gap-3">
        {step > 1 ? (
          <button
            type="button"
            onClick={handleBack}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl border border-gray-250 bg-white px-5 text-sm font-black text-gray-700 hover:bg-gray-50 active:scale-95 transition-all cursor-pointer"
          >
            <ArrowLeft className="h-5 w-5" />
            <span>{t('crops.wizard.actions.back', 'Back')}</span>
          </button>
        ) : (
          <div className="hidden sm:block" />
        )}

        {step < 5 ? (
          <button
            type="button"
            onClick={handleNext}
            disabled={
              (step === 1 && !cropType) ||
              (step === 1 && cropType === 'Other' && !customCropName.trim()) ||
              (step === 2 && !cropVariety.trim())
            }
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white px-6 text-sm font-black shadow-md active:scale-95 disabled:opacity-50 transition-all cursor-pointer"
          >
            <span>{t('crops.wizard.actions.next', 'Continue')}</span>
            <ArrowRight className="h-5 w-5" />
          </button>
        ) : (
          <button
            type="submit"
            disabled={isSubmitting}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white px-6 text-sm font-black shadow-md active:scale-95 transition-all cursor-pointer"
          >
            <Check className="h-5 w-5" />
            <span>{isSubmitting ? t('crops.wizard.actions.planting', 'Recording...') : t('crops.wizard.actions.submit', 'Add Crop')}</span>
          </button>
        )}
      </div>
    </form>
  );
};

export default CropFormWizard;
