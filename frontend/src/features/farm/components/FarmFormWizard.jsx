import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ArrowRight, Check, AlertCircle, Camera, Upload, Trash2 } from 'lucide-react';
import { toHectares, formatArea } from '../../../shared/utils/areaConversion';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const FARM_NAME_SUGGESTIONS = [
  { labelKey: 'farm.suggestions.coconutGarden', defaultText: 'Coconut Garden' },
  { labelKey: 'farm.suggestions.paddyField', defaultText: 'Paddy Field' },
  { labelKey: 'farm.suggestions.riverSidePlot', defaultText: 'River Side Plot' },
  { labelKey: 'farm.suggestions.homeFarm', defaultText: 'Home Farm' },
];

const AREA_UNITS = [
  { id: 'CENT', labelKey: 'units.cent', defaultText: 'Cent' },
  { id: 'ACRE', labelKey: 'units.acre', defaultText: 'Acre' },
  { id: 'HECTARE', labelKey: 'units.hectare', defaultText: 'Hectare' },
];

/**
 * FarmFormWizard guides the farmer step-by-step through creating a Farm.
 */
const FarmFormWizard = ({ onSubmit, submitting, submitError }) => {
  const { t } = useTranslation();
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    sizeValue: '',
    sizeUnit: 'ACRE', // Default to Acre for Indian farming context
    photo: null,
  });

  const [validationError, setValidationError] = useState('');
  const [showCamera, setShowCamera] = useState(false);
  const [cameraStream, setCameraStream] = useState(null);

  const videoRef = useRef(null);
  const fileInputRef = useRef(null);

  const stepsTotal = 5; // 1: Name, 2: Location, 3: Area, 4: Photo, 5: Review

  useEffect(() => {
    return () => {
      if (cameraStream) {
        cameraStream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [cameraStream]);

  useEffect(() => {
    if (showCamera && videoRef.current && cameraStream) {
      videoRef.current.srcObject = cameraStream;
    }
  }, [showCamera, cameraStream]);

  const handleInputChange = (field, value) => {
    setValidationError('');
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const validateCurrentStep = () => {
    if (step === 1) {
      if (!formData.name.trim()) {
        setValidationError(t('farm.wizard.validation.nameRequired', 'Please enter a farm name'));
        return false;
      }
    }
    if (step === 2) {
      if (!formData.location.trim()) {
        setValidationError(t('farm.wizard.validation.locationRequired', 'Please enter your farm location'));
        return false;
      }
    }
    if (step === 3) {
      const sizeStr = formData.sizeValue.toString().trim();
      if (!sizeStr) {
        setValidationError(t('farm.wizard.validation.sizeRequired', 'Please enter farm size'));
        return false;
      }
      const sizeNum = parseFloat(sizeStr);
      if (isNaN(sizeNum) || sizeNum <= 0) {
        setValidationError(t('farm.wizard.validation.sizePositive', 'Farm size must be a number greater than zero'));
        return false;
      }
    }
    return true;
  };

  const handleNext = () => {
    if (validateCurrentStep()) {
      if (step === 4) stopCamera();
      setStep((prev) => prev + 1);
    }
  };

  const handlePrevious = () => {
    setValidationError('');
    if (step === 4) stopCamera();
    setStep((prev) => prev - 1);
  };

  const startCamera = async () => {
    setValidationError('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });
      setCameraStream(stream);
      setShowCamera(true);
    } catch (err) {
      console.error('Camera access failed:', err);
      setValidationError(t('farm.wizard.cameraError', 'Could not access camera. Please choose a photo from your device.'));
    }
  };

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
    setShowCamera(false);
  };

  const capturePhoto = () => {
    if (!videoRef.current) return;
    const canvas = document.createElement('canvas');
    const video = videoRef.current;

    const maxDim = 400;
    let w = video.videoWidth || 640;
    let h = video.videoHeight || 480;
    if (w > maxDim || h > maxDim) {
      if (w > h) {
        h = Math.round((h * maxDim) / w);
        w = maxDim;
      } else {
        w = Math.round((w * maxDim) / h);
        h = maxDim;
      }
    }
    canvas.width = w;
    canvas.height = h;

    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, w, h);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.7);

    setFormData((prev) => ({ ...prev, photo: dataUrl }));
    stopCamera();
  };

  const triggerFileInput = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const maxDim = 400;
        let w = img.width;
        let h = img.height;
        if (w > maxDim || h > maxDim) {
          if (w > h) {
            h = Math.round((h * maxDim) / w);
            w = maxDim;
          } else {
            w = Math.round((w * maxDim) / h);
            h = maxDim;
          }
        }
        canvas.width = w;
        canvas.height = h;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, w, h);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.7);

        setFormData((prev) => ({ ...prev, photo: dataUrl }));
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  };

  const removePhoto = () => {
    setFormData((prev) => ({ ...prev, photo: null }));
  };

  const handleSubmit = () => {
    if (validateCurrentStep()) {
      const ha = toHectares(formData.sizeValue, formData.sizeUnit);
      onSubmit({
        name: formData.name.trim(),
        location: formData.location.trim(),
        sizeHectares: ha,
        sizeValue: parseFloat(formData.sizeValue),
        sizeUnit: formData.sizeUnit,
        photo: formData.photo,
      });
    }
  };

  const progressPercent = (step / stepsTotal) * 100;

  return (
    <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden p-6 sm:p-8 space-y-6">
      {/* Progress Header */}
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

      {/* Step Container */}
      <div className="min-h-[220px] flex flex-col justify-center">
        {/* Step 1: Farm Name */}
        {step === 1 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('farm.wizard.step1Title', 'What do you call this farm?')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('farm.wizard.step1Desc', 'Give your farm a name you can easily recognize.')}
              </p>
            </div>

            <div className="space-y-3">
              <label htmlFor="farmName" className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('farm.name', 'Farm Name')}
              </label>
              <input
                id="farmName"
                type="text"
                value={formData.name}
                onChange={(e) => handleInputChange('name', e.target.value)}
                placeholder={t('farm.wizard.placeholderName', 'e.g. Coconut Garden')}
                style={{ minHeight: '48px' }}
                className={`w-full px-4 rounded-2xl border font-extrabold text-base text-gray-850 transition-all focus:outline-hidden ${
                  validationError
                    ? 'border-red-500 focus:ring-2 focus:ring-red-200'
                    : 'border-gray-250 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'
                }`}
                aria-invalid={!!validationError}
              />

              {/* Tappable suggestion chips */}
              <div className="space-y-1.5 pt-1">
                <span className="text-xs font-bold text-gray-400">
                  {t('farm.wizard.suggestionsLabel', 'Quick Suggestions:')}
                </span>
                <div className="flex flex-wrap gap-2">
                  {FARM_NAME_SUGGESTIONS.map((sug) => {
                    const txt = t(sug.labelKey, sug.defaultText);
                    return (
                      <ChoiceChip
                        key={sug.labelKey}
                        label={txt}
                        selected={formData.name === txt}
                        onClick={() => handleInputChange('name', txt)}
                        size="sm"
                      />
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 2: Location */}
        {step === 2 && (
          <div className="space-y-4 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('farm.wizard.step2Title', 'Where is this farm?')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('farm.wizard.step2Desc', 'Enter village, town, or district name.')}
              </p>
            </div>

            <div className="space-y-2">
              <label htmlFor="farmLocation" className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                {t('farm.location', 'Farm Location')}
              </label>
              <input
                id="farmLocation"
                type="text"
                value={formData.location}
                onChange={(e) => handleInputChange('location', e.target.value)}
                placeholder={t('farm.wizard.placeholderLocation', 'e.g. Madurai, Tamil Nadu')}
                style={{ minHeight: '48px' }}
                className={`w-full px-4 rounded-2xl border font-extrabold text-base text-gray-850 transition-all focus:outline-hidden ${
                  validationError
                    ? 'border-red-500 focus:ring-2 focus:ring-red-200'
                    : 'border-gray-250 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'
                }`}
                aria-invalid={!!validationError}
              />
            </div>
          </div>
        )}

        {/* Step 3: Area & Unit Chips */}
        {step === 3 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('farm.wizard.step3Title', 'How large is this farm?')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('farm.wizard.step3Desc', 'Enter the size and select your measurement unit.')}
              </p>
            </div>

            <div className="space-y-4">
              {/* Size input */}
              <div className="space-y-2">
                <label htmlFor="farmAreaVal" className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                  {t('farm.wizard.areaLabel', 'Farm Size')}
                </label>
                <input
                  id="farmAreaVal"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={formData.sizeValue}
                  onChange={(e) => handleInputChange('sizeValue', e.target.value)}
                  placeholder={t('farm.wizard.placeholderArea', 'e.g. 5')}
                  style={{ minHeight: '48px' }}
                  className={`w-full px-4 rounded-2xl border font-extrabold text-base text-gray-850 transition-all focus:outline-hidden ${
                    validationError
                      ? 'border-red-500 focus:ring-2 focus:ring-red-200'
                      : 'border-gray-250 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'
                  }`}
                  aria-invalid={!!validationError}
                />
              </div>

              {/* Large Unit Choice Buttons */}
              <div className="space-y-2">
                <label className="block text-xs font-black text-gray-500 uppercase tracking-wider">
                  {t('farm.wizard.unitLabel', 'Select Unit')}
                </label>
                <div className="grid grid-cols-3 gap-2.5">
                  {AREA_UNITS.map((unit) => {
                    const txt = t(unit.labelKey, unit.defaultText);
                    const isSelected = formData.sizeUnit === unit.id;
                    return (
                      <ChoiceChip
                        key={unit.id}
                        label={txt}
                        selected={isSelected}
                        onClick={() => handleInputChange('sizeUnit', unit.id)}
                      />
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Photo Options */}
        {step === 4 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('farm.wizard.step4Title', 'Add a farm photo')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('farm.wizard.step4Desc', 'Optional photo to recognize your farm visually.')}
              </p>
            </div>

            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              accept="image/*"
              onChange={handleFileChange}
            />

            {showCamera && (
              <div className="rounded-2xl border-2 border-emerald-500 overflow-hidden relative bg-black flex flex-col items-center">
                <video ref={videoRef} autoPlay playsInline className="w-full max-h-[300px] object-cover" />
                <div className="p-3 bg-gray-900 w-full flex items-center justify-around">
                  <button
                    onClick={stopCamera}
                    style={{ minHeight: '44px' }}
                    className="rounded-xl px-4 text-sm font-bold text-white border border-gray-600 hover:bg-gray-800"
                  >
                    {t('common.actions.cancel', 'Cancel')}
                  </button>
                  <button
                    onClick={capturePhoto}
                    style={{ minHeight: '44px' }}
                    className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold px-6 flex items-center space-x-1"
                  >
                    <Camera className="h-4.5 w-4.5" />
                    <span>{t('farm.actions.capture', 'Capture')}</span>
                  </button>
                </div>
              </div>
            )}

            {!showCamera && !formData.photo && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <button
                  type="button"
                  onClick={startCamera}
                  style={{ minHeight: '80px' }}
                  className="rounded-2xl border-2 border-dashed border-gray-250 p-5 flex flex-col items-center justify-center space-y-2 bg-white hover:border-emerald-500 cursor-pointer hover:bg-emerald-50/10 transition-all"
                >
                  <Camera className="h-7 w-7 text-emerald-600" />
                  <span className="text-sm font-black text-gray-800">{t('farm.wizard.takePhoto', 'Take Photo')}</span>
                </button>

                <button
                  type="button"
                  onClick={triggerFileInput}
                  style={{ minHeight: '80px' }}
                  className="rounded-2xl border-2 border-dashed border-gray-250 p-5 flex flex-col items-center justify-center space-y-2 bg-white hover:border-emerald-500 cursor-pointer hover:bg-emerald-50/10 transition-all"
                >
                  <Upload className="h-7 w-7 text-emerald-600" />
                  <span className="text-sm font-black text-gray-800">{t('farm.wizard.chooseFile', 'Choose from Phone')}</span>
                </button>
              </div>
            )}

            {formData.photo && (
              <div className="rounded-2xl border border-gray-200 overflow-hidden relative shadow-sm max-w-xs mx-auto bg-white">
                <img src={formData.photo} alt="Preview" className="w-full h-44 object-cover" />
                <div className="absolute top-2.5 right-2.5">
                  <button
                    type="button"
                    onClick={removePhoto}
                    style={{ minHeight: '40px', minWidth: '40px' }}
                    className="bg-red-50 hover:bg-red-100 text-red-600 p-2 rounded-full border border-red-200 shadow-sm flex items-center justify-center cursor-pointer"
                  >
                    <Trash2 className="h-4.5 w-4.5" />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 5: Review & Submit */}
        {step === 5 && (
          <div className="space-y-5 animate-fade-in">
            <div>
              <h2 className="text-2xl font-black text-gray-850 leading-tight">
                {t('farm.wizard.step5Title', 'Review farm details')}
              </h2>
              <p className="text-sm text-gray-400 font-semibold mt-1">
                {t('farm.wizard.step5Desc', 'Confirm your farm details before creating.')}
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-5 bg-gray-50/80 p-5 rounded-2xl border border-gray-200">
              {formData.photo && (
                <div className="w-full sm:w-32 h-28 rounded-xl overflow-hidden border border-gray-200 shrink-0 bg-white">
                  <img src={formData.photo} alt="Preview" className="w-full h-full object-cover" />
                </div>
              )}

              <div className="flex-1 space-y-3 text-sm">
                <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                  <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('farm.name', 'Farm Name')}</span>
                  <span className="font-black text-gray-850 truncate max-w-[200px]">{formData.name}</span>
                </div>
                <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                  <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('farm.location', 'Location')}</span>
                  <span className="font-bold text-gray-700 truncate max-w-[200px]">{formData.location}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="font-bold text-gray-400 uppercase text-xs tracking-wider">{t('farm.size', 'Size')}</span>
                  <span className="font-black text-emerald-700">
                    {formatArea(formData.sizeValue, formData.sizeUnit, t)}
                  </span>
                </div>
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

      {/* Control Buttons Footer */}
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

        {step === 4 && !formData.photo && (
          <button
            type="button"
            onClick={handleNext}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1 rounded-2xl border border-emerald-200 bg-emerald-50/50 px-5 text-sm font-black text-emerald-800 hover:bg-emerald-100 active:scale-95 transition-all cursor-pointer"
          >
            <span>{t('farm.wizard.skip', 'Skip')}</span>
            <ArrowRight className="h-5 w-5 text-emerald-700" />
          </button>
        )}

        {step < stepsTotal && step !== 4 && (
          <button
            type="button"
            onClick={handleNext}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white px-6 text-sm font-black shadow-md active:scale-95 transition-all cursor-pointer"
          >
            <span>{t('farm.actions.next', 'Continue')}</span>
            <ArrowRight className="h-5 w-5" />
          </button>
        )}

        {step === 4 && formData.photo && (
          <button
            type="button"
            onClick={handleNext}
            style={{ minHeight: '48px' }}
            className="flex-1 sm:flex-none flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white px-6 text-sm font-black shadow-md active:scale-95 transition-all cursor-pointer"
          >
            <span>{t('farm.actions.next', 'Continue')}</span>
            <ArrowRight className="h-5 w-5" />
          </button>
        )}

        {step === stepsTotal && (
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
                <span>{t('states.loading', 'Creating...')}</span>
              </span>
            ) : (
              <>
                <Check className="h-5 w-5" />
                <span>{t('farm.actions.submit', 'Create Farm')}</span>
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
};

export default FarmFormWizard;
