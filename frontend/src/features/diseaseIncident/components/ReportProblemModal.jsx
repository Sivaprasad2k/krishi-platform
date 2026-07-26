import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, X, Check } from 'lucide-react';
import { reportDiseaseIncident } from '../api/diseaseIncidentApi';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const COMMON_SUGGESTIONS = [
  'Rice Blast',
  'Sheath Blight',
  'Stem Borer',
  'Leaf Spot',
  'Bacterial Blight',
  'Root Rot',
  'Mosaic Virus',
];

const SYMPTOM_CHIPS = [
  'Yellow Leaves',
  'Black Spots',
  'Wilting Stems',
  'Insect Damage',
  'Fungal Powdery Coating',
  'Stunted Growth',
];

/**
 * ReportProblemModal allows farmers to log a crop health problem/disease incident.
 */
export const ReportProblemModal = ({
  isOpen,
  onClose,
  onSuccess,
  preselectedPlantingId = null,
  cropPlantings = [],
}) => {
  const { t } = useTranslation();

  const [plantingId, setPlantingId] = useState(preselectedPlantingId || '');
  const [diseaseName, setDiseaseName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (preselectedPlantingId) {
      setPlantingId(preselectedPlantingId);
    } else if (cropPlantings.length > 0 && !plantingId) {
      setPlantingId(cropPlantings[0].id);
    }
  }, [preselectedPlantingId, cropPlantings]);

  if (!isOpen) return null;

  const handleSymptomChipClick = (symptom) => {
    if (!description.includes(symptom)) {
      setDescription((prev) => (prev ? `${prev}, ${symptom}` : symptom));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!plantingId) {
      setError(t('diseaseIncident.selectCropRequired', 'Please select a crop'));
      return;
    }
    if (!diseaseName.trim()) {
      setError(t('diseaseIncident.diseaseNameRequired', 'Problem name is required'));
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await reportDiseaseIncident({
        cropPlantingId: plantingId,
        diseaseName: diseaseName.trim(),
        description: description.trim() || undefined,
      });

      setDiseaseName('');
      setDescription('');
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to report disease incident:', err);
      setError(err.message || 'Failed to report crop problem. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between pb-4 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-red-50 text-red-600 rounded-2xl shrink-0">
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-2xl font-black text-gray-850">
                {t('diseaseIncident.reportModalTitle', 'Report Crop Problem')}
              </h3>
              <p className="text-xs font-semibold text-gray-400 mt-0.5">
                {t('diseaseIncident.reportModalSubtitle', 'Log a disease, pest, or field issue for your crop')}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ minHeight: '44px', minWidth: '44px' }}
            className="p-2 text-gray-400 hover:text-gray-600 rounded-xl hover:bg-gray-100 transition-all cursor-pointer flex items-center justify-center"
            aria-label="Close"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 text-red-700 text-sm font-bold rounded-2xl">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Crop Selector */}
          {!preselectedPlantingId && cropPlantings.length > 0 && (
            <div className="space-y-1.5">
              <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
                {t('diseaseIncident.selectCrop', 'Which crop has this problem?')}
              </label>
              <select
                value={plantingId}
                onChange={(e) => setPlantingId(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold text-base text-gray-850 bg-white focus:outline-hidden focus:border-emerald-500 transition-all cursor-pointer"
              >
                {cropPlantings.map((crop) => (
                  <option key={crop.id} value={crop.id}>
                    {crop.cropName} ({crop.cropVariety || 'Standard'}) &bull; {crop.farmName || 'Farm'}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Disease Name Input */}
          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('diseaseIncident.diseaseName', 'What is the problem or disease name?')} *
            </label>
            <input
              type="text"
              value={diseaseName}
              onChange={(e) => setDiseaseName(e.target.value)}
              placeholder={t('diseaseIncident.diseaseNamePlaceholder', 'e.g. Rice Blast, Leaf Spot, Root Rot')}
              style={{ minHeight: '48px' }}
              className="w-full px-4 rounded-2xl border border-gray-250 font-bold text-base text-gray-850 focus:outline-hidden focus:border-emerald-500 transition-all"
              required
            />
            {/* Quick Suggestions */}
            <div className="space-y-1 pt-1">
              <span className="text-xs font-bold text-gray-400 block">Common Problems:</span>
              <div className="flex flex-wrap gap-1.5">
                {COMMON_SUGGESTIONS.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    onClick={() => setDiseaseName(suggestion)}
                    className={`text-xs font-black px-3 py-1.5 rounded-xl border transition-all cursor-pointer ${
                      diseaseName === suggestion
                        ? 'bg-red-600 text-white border-red-600 shadow-xs'
                        : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Symptoms / Notes Input */}
          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('diseaseIncident.description', 'What symptoms do you observe?')}
            </label>
            <div className="flex flex-wrap gap-1.5 mb-2">
              {SYMPTOM_CHIPS.map((sym) => (
                <ChoiceChip
                  key={sym}
                  label={sym}
                  selected={description.includes(sym)}
                  onClick={() => handleSymptomChipClick(sym)}
                  size="sm"
                />
              ))}
            </div>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('diseaseIncident.descriptionPlaceholder', 'Describe symptoms observed on leaves, stems, or fruit...')}
              rows={3}
              className="w-full p-3.5 rounded-2xl border border-gray-250 font-semibold text-sm text-gray-800 focus:outline-hidden focus:border-emerald-500 transition-all resize-none"
            />
          </div>

          {/* Buttons */}
          <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-150">
            <button
              type="button"
              onClick={onClose}
              style={{ minHeight: '48px' }}
              className="flex-1 py-3 rounded-2xl border border-gray-250 font-black text-gray-700 hover:bg-gray-50 active:scale-95 transition-all cursor-pointer"
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{ minHeight: '48px' }}
              className="flex-1 py-3 rounded-2xl bg-red-600 hover:bg-red-700 text-white font-black shadow-md active:scale-95 disabled:opacity-50 transition-all cursor-pointer flex items-center justify-center space-x-2"
            >
              <Check className="h-5 w-5" />
              <span>
                {submitting ? 'Submitting...' : t('diseaseIncident.actions.submit', 'Report Problem')}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReportProblemModal;
