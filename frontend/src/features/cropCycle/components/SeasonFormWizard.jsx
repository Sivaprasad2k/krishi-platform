import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ArrowRight, Check, AlertCircle, CloudRain, Sun, Snowflake, Calendar, FileText } from 'lucide-react';
import OptionCard from '../../../shared/components/OptionCard';

/**
 * SeasonFormWizard guides the farmer step-by-step through starting a new crop season.
 */
const SeasonFormWizard = ({ farmName, farmId, onSubmit, submitting, submitError }) => {
  const { t } = useTranslation();
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    seasonType: 'MONSOON',
    year: new Date().getFullYear(),
    notes: '',
  });
  const [validationError, setValidationError] = useState('');

  const stepsTotal = 3; // Step 1: Season Type & Year, Step 2: Notes, Step 3: Review & Submit

  const handleSeasonTypeChange = (type) => {
    setValidationError('');
    setFormData((prev) => ({
      ...prev,
      seasonType: type,
    }));
  };

  const handleYearChange = (val) => {
    setValidationError('');
    setFormData((prev) => ({
      ...prev,
      year: val,
    }));
  };

  const handleNotesChange = (val) => {
    setValidationError('');
    setFormData((prev) => ({
      ...prev,
      notes: val,
    }));
  };

  const getEffectiveName = () => {
    const typeLabel = t(`season.types.${formData.seasonType}`, formData.seasonType);
    return `${typeLabel} ${formData.year}`;
  };

  const validateCurrentStep = () => {
    if (step === 1) {
      if (!formData.seasonType) {
        setValidationError(t('season.validation.typeRequired', 'Season type is required'));
        return false;
      }
      if (!formData.year) {
        setValidationError(t('season.validation.yearRequired', 'Year is required'));
        return false;
      }
      const yearNum = parseInt(formData.year, 10);
      if (isNaN(yearNum) || yearNum < 2000 || yearNum > 2100) {
        setValidationError(t('season.validation.yearInvalid', 'Please enter a valid year (e.g. 2026)'));
        return false;
      }
    }
    return true;
  };

  const handleNext = () => {
    if (validateCurrentStep()) {
      setStep((prev) => prev + 1);
    }
  };

  const handlePrevious = () => {
    setValidationError('');
    setStep((prev) => prev - 1);
  };

  const handleSubmit = () => {
    if (validateCurrentStep()) {
      onSubmit({
        name: getEffectiveName(),
        seasonType: formData.seasonType,
        year: parseInt(formData.year, 10),
        notes: formData.notes,
        farmId: farmId,
      });
    }
  };

  const progressPercent = (step / stepsTotal) * 100;

  const seasonsList = [
    { type: 'MONSOON', labelKey: 'season.types.MONSOON', defaultText: 'Monsoon (Kharif)', descKey: 'season.typeDesc.MONSOON', defaultDesc: 'Rainy season cultivation', icon: CloudRain },
    { type: 'SUMMER', labelKey: 'season.types.SUMMER', defaultText: 'Summer (Zaid)', descKey: 'season.typeDesc.SUMMER', defaultDesc: 'Hot season crop cycle', icon: Sun },
    { type: 'WINTER', labelKey: 'season.types.WINTER', defaultText: 'Winter (Rabi)', descKey: 'season.typeDesc.WINTER', defaultDesc: 'Cool weather cultivation', icon: Snowflake },
    { type: 'PERENNIAL', labelKey: 'season.types.PERENNIAL', defaultText: 'Perennial / All Year', descKey: 'season.typeDesc.PERENNIAL', defaultDesc: 'Long term tree or garden crops', icon: Calendar },
  ];

  return (
    <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden p-6 sm:p-8 space-y-6">
      {/* Progress Bar */}
      <div className="space-y-2">
        <div className="flex justify-between items-center text-xs font-black text-gray-500">
          <span className="uppercase tracking-wider">
            {t('farm.wizard.progress', { current: step, total: stepsTotal })}
          </span>
          <span className="text-emerald-700 font-black">{Math.round(progressPercent)}%</span>
        </div>
        <div className="h-2.5 w-full bg-gray-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-emerald-600 rounded-full transition-all duration-300 ease-out"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </div>

      {/* Step Area */}
      <div className="min-h-[240px] flex flex-col justify-center">
        {/* Step 1 */}
        {step === 1 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('season.wizard.step1Title', 'What crop season are you starting?')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('season.wizard.step1Desc', 'Choose the planting season and cultivation year.')}
              </p>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {seasonsList.map((s) => (
                  <OptionCard
                    key={s.type}
                    icon={s.icon}
                    title={t(s.labelKey, s.defaultText)}
                    description={t(s.descKey, s.defaultDesc)}
                    selected={formData.seasonType === s.type}
                    onClick={() => handleSeasonTypeChange(s.type)}
                  />
                ))}
              </div>

              <div className="space-y-1.5 pt-2">
                <label htmlFor="seasonYear" className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                  {t('season.year', 'Cultivation Year')}
                </label>
                <div className="relative">
                  <Calendar className="absolute left-4 top-3.5 h-5 w-5 text-gray-400" />
                  <input
                    id="seasonYear"
                    type="number"
                    min="2000"
                    max="2100"
                    value={formData.year}
                    onChange={(e) => handleYearChange(e.target.value)}
                    style={{ minHeight: '48px' }}
                    className="w-full pl-12 pr-4 rounded-2xl border border-gray-250 font-extrabold text-base bg-white focus:outline-hidden focus:border-emerald-500"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 2 */}
        {step === 2 && (
          <div className="space-y-4 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('season.wizard.step2Title', 'Season Notes (Optional)')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('season.wizard.step2Desc', 'Add notes about crop plans or soil preparation.')}
              </p>
            </div>

            <div className="space-y-1.5">
              <label htmlFor="seasonNotes" className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('season.notes', 'Notes / Plan')}
              </label>
              <div className="relative">
                <FileText className="absolute left-4 top-3.5 h-5 w-5 text-gray-400" />
                <textarea
                  id="seasonNotes"
                  rows={4}
                  value={formData.notes}
                  onChange={(e) => handleNotesChange(e.target.value)}
                  placeholder={t('season.wizard.placeholderNotes', 'e.g. Rotate paddy with pulses. Apply organic manure.')}
                  className="w-full pl-12 pr-4 py-3 rounded-2xl border border-gray-250 font-semibold text-sm text-gray-800 focus:outline-hidden focus:border-emerald-500 resize-none"
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 3 */}
        {step === 3 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('season.wizard.step3Title', 'Review & Start Season')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('season.wizard.step3Desc', 'Confirm details before launching this crop season.')}
              </p>
            </div>

            <div className="bg-gray-50/80 p-5 rounded-2xl border border-gray-200 space-y-3 text-sm">
              <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('nav.farms', 'Farm')}</span>
                <span className="font-black text-gray-850 truncate max-w-[200px]">{farmName}</span>
              </div>
              <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('season.name', 'Season Name')}</span>
                <span className="font-black text-emerald-700 truncate max-w-[200px]">{getEffectiveName()}</span>
              </div>
              <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('season.cropType', 'Season Type')}</span>
                <span className="font-bold text-gray-800 truncate max-w-[200px]">
                  {t(`season.types.${formData.seasonType}`, formData.seasonType)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('season.year', 'Year')}</span>
                <span className="font-black text-gray-850">{formData.year}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Errors */}
      {(validationError || submitError) && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 flex items-center space-x-2 text-red-700 text-sm font-bold animate-fade-in">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{validationError || submitError}</span>
        </div>
      )}

      {/* Control Buttons */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-150 gap-3">
        {step > 1 ? (
          <button
            type="button"
            onClick={handlePrevious}
            disabled={submitting}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl border border-gray-250 bg-white px-5 text-sm font-black text-gray-700 hover:bg-gray-50 active:scale-95 transition-all cursor-pointer"
          >
            <ArrowLeft className="h-5 w-5" />
            <span>{t('farm.actions.previous', 'Back')}</span>
          </button>
        ) : (
          <div className="hidden sm:block" />
        )}

        {step < stepsTotal ? (
          <button
            type="button"
            onClick={handleNext}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white px-6 text-sm font-black shadow-md active:scale-95 transition-all cursor-pointer"
          >
            <span>{t('farm.actions.next', 'Continue')}</span>
            <ArrowRight className="h-5 w-5" />
          </button>
        ) : (
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white px-6 text-sm font-black shadow-md active:scale-95 transition-all cursor-pointer"
          >
            {submitting ? (
              <span className="flex items-center space-x-2">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                <span>{t('states.loading', 'Starting...')}</span>
              </span>
            ) : (
              <>
                <Check className="h-5 w-5" />
                <span>{t('season.create', 'Start Season')}</span>
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
};

export default SeasonFormWizard;
