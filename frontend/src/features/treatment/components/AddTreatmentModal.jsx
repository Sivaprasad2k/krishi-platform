import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Check, Droplet } from 'lucide-react';
import { createTreatment } from '../api/treatmentApi';
import ChoiceChip from '../../../shared/components/ChoiceChip';

const METHOD_SUGGESTIONS = [
  'Foliar Spray',
  'Soil Drenching',
  'Seed Treatment',
  'Granular Application',
];

const TREATMENT_PRESETS = [
  'Neem Oil Spray (3%)',
  'Copper Fungicide',
  'Bio-Pesticide (Pseudomonas)',
  'NPK Foliar Spray',
  'Organic Compost',
];

/**
 * AddTreatmentModal allows farmers to create a treatment plan for a disease incident.
 */
export const AddTreatmentModal = ({
  isOpen,
  onClose,
  onSuccess,
  preselectedIncidentId = null,
  diseaseIncidents = [],
}) => {
  const { t } = useTranslation();

  const [diseaseIncidentId, setDiseaseIncidentId] = useState(preselectedIncidentId || '');
  const [treatmentName, setTreatmentName] = useState('');
  const [recommendedDosage, setRecommendedDosage] = useState('');
  const [applicationMethod, setApplicationMethod] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (preselectedIncidentId) {
      setDiseaseIncidentId(preselectedIncidentId);
    } else if (diseaseIncidents.length > 0 && !diseaseIncidentId) {
      setDiseaseIncidentId(diseaseIncidents[0].id);
    }
  }, [preselectedIncidentId, diseaseIncidents]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!diseaseIncidentId) {
      setError(t('treatment.selectProblemRequired', 'Please select a disease incident'));
      return;
    }
    if (!treatmentName.trim()) {
      setError(t('treatment.treatmentNameRequired', 'Treatment name is required'));
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await createTreatment({
        diseaseIncidentId,
        treatmentName: treatmentName.trim(),
        recommendedDosage: recommendedDosage.trim() || undefined,
        applicationMethod: applicationMethod.trim() || undefined,
        description: description.trim() || undefined,
      });

      setTreatmentName('');
      setRecommendedDosage('');
      setApplicationMethod('');
      setDescription('');
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error('Failed to create treatment plan:', err);
      setError(err.message || 'Failed to plan treatment. Please try again.');
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
            <div className="p-3 bg-blue-50 text-blue-600 rounded-2xl shrink-0">
              <Droplet className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-2xl font-black text-gray-850">
                {t('treatment.addModalTitle', 'Plan Treatment')}
              </h3>
              <p className="text-xs font-semibold text-gray-400 mt-0.5">
                {t('treatment.addModalSubtitle', 'How will you treat this crop problem?')}
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
          {/* Disease Incident Selector */}
          {!preselectedIncidentId && diseaseIncidents.length > 0 && (
            <div className="space-y-1.5">
              <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
                {t('treatment.selectProblem', 'Which problem are you treating?')}
              </label>
              <select
                value={diseaseIncidentId}
                onChange={(e) => setDiseaseIncidentId(e.target.value)}
                style={{ minHeight: '48px' }}
                className="w-full px-4 rounded-2xl border border-gray-250 font-bold text-base text-gray-850 bg-white focus:outline-hidden focus:border-blue-500 transition-all cursor-pointer"
              >
                {diseaseIncidents.map((incident) => (
                  <option key={incident.id} value={incident.id}>
                    {incident.diseaseName} &bull; {incident.cropName || 'Crop'} ({incident.farmName || 'Farm'})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Treatment Name Input */}
          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('treatment.treatmentName', 'What treatment will you apply?')} *
            </label>
            <input
              type="text"
              value={treatmentName}
              onChange={(e) => setTreatmentName(e.target.value)}
              placeholder={t('treatment.treatmentNamePlaceholder', 'e.g. Neem Oil Spray, Copper Fungicide')}
              style={{ minHeight: '48px' }}
              className="w-full px-4 rounded-2xl border border-gray-250 font-bold text-base text-gray-850 focus:outline-hidden focus:border-blue-500 transition-all"
              required
            />

            {/* Treatment Presets */}
            <div className="space-y-1 pt-1">
              <span className="text-xs font-bold text-gray-400 block">Recommended Treatments:</span>
              <div className="flex flex-wrap gap-1.5">
                {TREATMENT_PRESETS.map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setTreatmentName(preset)}
                    className={`text-xs font-black px-3 py-1.5 rounded-xl border transition-all cursor-pointer ${
                      treatmentName === preset
                        ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                        : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    {preset}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Recommended Dosage */}
          <div className="space-y-1.5">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('treatment.dosage', 'What is the dosage?')}
            </label>
            <input
              type="text"
              value={recommendedDosage}
              onChange={(e) => setRecommendedDosage(e.target.value)}
              placeholder={t('treatment.dosagePlaceholder', 'e.g. 5ml per liter of water, 200g per acre')}
              style={{ minHeight: '48px' }}
              className="w-full px-4 rounded-2xl border border-gray-250 font-semibold text-sm text-gray-850 focus:outline-hidden focus:border-blue-500 transition-all"
            />
          </div>

          {/* Application Method */}
          <div className="space-y-2">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('treatment.applicationMethod', 'How will you apply it?')}
            </label>
            <div className="flex flex-wrap gap-1.5">
              {METHOD_SUGGESTIONS.map((method) => (
                <ChoiceChip
                  key={method}
                  label={method}
                  selected={applicationMethod === method}
                  onClick={() => setApplicationMethod(method)}
                  size="sm"
                />
              ))}
            </div>
          </div>

          {/* Instructions Input */}
          <div className="space-y-1.5">
            <label className="text-xs font-black text-gray-500 block uppercase tracking-wider">
              {t('treatment.description', 'Application Notes / Safety Steps')}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('treatment.descriptionPlaceholder', 'e.g. Spray early morning before sun gets hot...')}
              rows={3}
              className="w-full p-3.5 rounded-2xl border border-gray-250 font-semibold text-sm text-gray-800 focus:outline-hidden focus:border-blue-500 transition-all resize-none"
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
              className="flex-1 py-3 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-black shadow-md active:scale-95 disabled:opacity-50 transition-all cursor-pointer flex items-center justify-center space-x-2"
            >
              <Check className="h-5 w-5" />
              <span>
                {submitting ? 'Saving...' : t('treatment.actions.submit', 'Save Treatment Plan')}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddTreatmentModal;
