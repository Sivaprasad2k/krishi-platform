import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ArrowRight, Check, HelpCircle } from 'lucide-react';
import CropTypeSelector from './CropTypeSelector';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const VARIETY_SUGGESTIONS = {
  Paddy: ['Ponni', 'Sona Masoori', 'IR20', 'CR1009', 'ADT 37'],
  Banana: ['Grand Naine', 'Robusta', 'Nendran', 'Poovan', 'Red Banana'],
  Coconut: ['West Coast Tall', 'Chowghat Dwarf', 'Malayan Yellow', 'Local'],
  Tomato: ['PKM-1', 'Arka Rakshak', 'Vaishnavi', 'Local Hybrid'],
};

export const CropFormWizard = ({ onSubmit, isSubmitting, initialValues = {} }) => {
  const { t } = useTranslation();
  const [step, setStep] = useState(1);
  const [cropType, setCropType] = useState(initialValues.cropName || '');
  const [customCropName, setCustomCropName] = useState(
    initialValues.cropName && !['Paddy', 'Banana', 'Coconut', 'Tomato'].includes(initialValues.cropName)
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

  useEffect(() => {
    if (unknownVariety) {
      setCropVariety('Local/Traditional');
    } else if (cropVariety === 'Local/Traditional') {
      setCropVariety('');
    }
  }, [unknownVariety]);

  useEffect(() => {
    if (!initialValues.expectedHarvestDate) {
      const baseDate = plantingDate ? new Date(plantingDate) : new Date();
      if (cropType === 'Paddy') {
        const d = new Date(baseDate);
        d.setDate(d.getDate() + 120);
        setExpectedHarvestDate(d.toISOString().split('T')[0]);
      } else if (cropType === 'Banana') {
        const d = new Date(baseDate);
        d.setDate(d.getDate() + 300);
        setExpectedHarvestDate(d.toISOString().split('T')[0]);
      } else if (cropType === 'Tomato') {
        const d = new Date(baseDate);
        d.setDate(d.getDate() + 90);
        setExpectedHarvestDate(d.toISOString().split('T')[0]);
      } else {
        setExpectedHarvestDate('');
      }
    }
  }, [cropType, plantingDate]);

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
    });
  };

  const finalCropNameDisplay = cropType === 'Other' ? customCropName : cropType;
  const currentVarietySuggestions = VARIETY_SUGGESTIONS[cropType] || [];

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
                  placeholder={t('crops.wizard.step1.customPlaceholder', 'e.g. Mango, Carrot')}
                  value={customCropName}
                  onChange={(e) => setCustomCropName(e.target.value)}
                  style={{ minHeight: '48px' }}
                  className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 text-base"
                />
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
                placeholder={t('crops.wizard.step2.placeholder', 'e.g. Ponni, Basmati, Grand Naine')}
                value={cropVariety}
                onChange={(e) => setCropVariety(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold focus:outline-hidden focus:border-emerald-500 disabled:bg-gray-50 disabled:text-gray-400 text-base text-gray-850"
              />

              {/* Variety Suggestion Chips */}
              {currentVarietySuggestions.length > 0 && !unknownVariety && (
                <div className="space-y-1.5 pt-1">
                  <span className="text-xs font-bold text-gray-400">
                    {t('crops.wizard.step2.suggestionsLabel', 'Common Varieties:')}
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

            {['Paddy', 'Banana', 'Tomato'].includes(cropType) && (
              <div className="p-4 bg-amber-50/80 rounded-2xl border border-amber-200 flex items-start space-x-3 text-xs font-bold text-amber-800">
                <HelpCircle className="h-5 w-5 shrink-0 text-amber-600 mt-0.5" />
                <div className="space-y-1">
                  <span>
                    {cropType === 'Paddy' && t('crops.wizard.step4.suggestPaddy', 'Suggested Paddy cycle is ~120 days from planting.')}
                    {cropType === 'Banana' && t('crops.wizard.step4.suggestBanana', 'Suggested Banana cycle is ~300 days from planting.')}
                    {cropType === 'Tomato' && t('crops.wizard.step4.suggestTomato', 'Suggested Tomato cycle is ~90 days from planting.')}
                  </span>
                  <span className="block font-semibold text-amber-700">
                    {t('crops.wizard.step4.disclaimer', 'Timeline suggestions are estimate frameworks only.')}
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
              <div className="flex justify-between">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('crops.wizard.review.expectedHarvest', 'Expected Harvest')}:</span>
                <span className="font-black text-gray-850">
                  {expectedHarvestDate ? new Date(expectedHarvestDate).toLocaleDateString() : t('crops.wizard.review.none', 'Not specified')}
                </span>
              </div>
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
