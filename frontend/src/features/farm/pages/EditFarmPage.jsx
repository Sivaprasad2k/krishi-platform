import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Save, X, AlertCircle } from 'lucide-react';
import { useFarms } from '../hooks/useFarms';
import FarmHeader from '../components/FarmHeader';
import StateView from '../../../shared/components/StateView';
import { toHectares, fromHectares } from '../../../shared/utils/areaConversion';

/**
 * EditFarmPage handles editing the Farm aggregated details.
 */
const EditFarmPage = () => {
  const { farmId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { selectedFarm, loading, error, fetchFarm, updateFarm } = useFarms();
  const [internalState, setInternalState] = useState('loading');

  const [formData, setFormData] = useState({
    name: '',
    location: '',
    status: 'ACTIVE'
  });
  const [sizeValue, setSizeValue] = useState('');
  const [sizeUnit, setSizeUnit] = useState('HECTARE');
  const [photo, setPhoto] = useState(null);

  const [validationError, setValidationError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setInternalState('loading');
    const res = await fetchFarm(farmId);
    if (!res.success) {
      setInternalState(navigator.onLine ? 'error' : 'offline');
    } else {
      setInternalState('success');
    }
  };

  useEffect(() => {
    loadData();
  }, [farmId]);

  useEffect(() => {
    if (selectedFarm && selectedFarm?.id === farmId) {
      // Load unit preference and photo from localStorage
      const pref = localStorage.getItem(`farm_pref_${selectedFarm.id}`);
      const { unit, photo: savedPhoto } = pref ? JSON.parse(pref) : { unit: 'HECTARE', photo: null };

      // Convert Hectares to display unit
      const convertedVal = fromHectares(selectedFarm.sizeHectares || 0, unit);
      const formattedVal = convertedVal % 1 === 0 ? convertedVal.toString() : convertedVal.toFixed(2);

      setFormData({
        name: selectedFarm?.name || '',
        location: selectedFarm?.location || '',
        status: selectedFarm?.status || 'ACTIVE'
      });
      setSizeValue(formattedVal);
      setSizeUnit(unit);
      setPhoto(savedPhoto);
      setInternalState('success');
    }
  }, [selectedFarm, farmId]);

  const handleInputChange = (field, value) => {
    setValidationError('');
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const validateForm = () => {
    if (!formData.name.trim()) {
      setValidationError(t('farm.wizard.validation.nameRequired', 'Farm name is required'));
      return false;
    }
    if (!formData.location.trim()) {
      setValidationError(t('farm.wizard.validation.locationRequired', 'Farm location is required'));
      return false;
    }
    const sizeStr = sizeValue.toString().trim();
    if (!sizeStr) {
      setValidationError(t('farm.wizard.validation.sizeRequired', 'Farm size is required'));
      return false;
    }
    const sizeNum = parseFloat(sizeStr);
    if (isNaN(sizeNum)) {
      setValidationError(t('farm.wizard.validation.sizeNumber', 'Farm size must be a valid number'));
      return false;
    }
    if (sizeNum <= 0) {
      setValidationError(t('farm.wizard.validation.sizePositive', 'Farm size must be greater than zero'));
      return false;
    }
    return true;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    setValidationError('');

    // Convert preferred unit back to Hectares for API payload
    const haVal = toHectares(sizeValue, sizeUnit);

    const result = await updateFarm(farmId, {
      name: formData.name.trim(),
      location: formData.location.trim(),
      sizeHectares: haVal,
      status: formData.status
    });
    setSubmitting(false);

    if (result.success) {
      // Save display unit preference and preserve the photo
      localStorage.setItem(`farm_pref_${farmId}`, JSON.stringify({
        unit: sizeUnit,
        photo: photo
      }));
      navigate(`/farms/${farmId}`);
    } else {
      setValidationError(result.error || t('states.errorDescription', 'Failed to update farm details.'));
    }
  };

  return (
    <div className="space-y-6">
      <FarmHeader
        title={t('farm.edit', 'Edit Farm')}
        subtitle={selectedFarm?.name || ''}
        onBack={() => navigate(`/farms/${farmId}`)}
      />

      <StateView state={internalState} onRetry={loadData}>
        <div className="max-w-2xl mx-auto">
          <form onSubmit={handleSave} className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
            {/* Farm Name Input */}
            <div className="space-y-2">
              <label htmlFor="editFarmName" className="block text-sm font-bold text-gray-700 uppercase tracking-wide">
                {t('farm.name', 'Farm Name')}
              </label>
              <input
                id="editFarmName"
                type="text"
                value={formData.name}
                onChange={(e) => handleInputChange('name', e.target.value)}
                placeholder="Farm Name"
                className="w-full h-12 px-4 rounded-xl border border-gray-250 font-semibold text-base focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 transition-all"
              />
            </div>

            {/* Farm Location Input */}
            <div className="space-y-2">
              <label htmlFor="editFarmLocation" className="block text-sm font-bold text-gray-700 uppercase tracking-wide">
                {t('farm.location', 'Farm Location')}
              </label>
              <input
                id="editFarmLocation"
                type="text"
                value={formData.location}
                onChange={(e) => handleInputChange('location', e.target.value)}
                placeholder="Location"
                className="w-full h-12 px-4 rounded-xl border border-gray-250 font-semibold text-base focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 transition-all"
              />
            </div>

            {/* Farm Size & Status Row */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
              {/* Size Value */}
              <div className="sm:col-span-2 space-y-2">
                <label htmlFor="editFarmSizeVal" className="block text-sm font-bold text-gray-700 uppercase tracking-wide">
                  {t('farm.wizard.areaLabel', 'Farm Area')}
                </label>
                <input
                  id="editFarmSizeVal"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={sizeValue}
                  onChange={(e) => {
                    setValidationError('');
                    setSizeValue(e.target.value);
                  }}
                  placeholder="e.g. 50"
                  className="w-full h-12 px-4 rounded-xl border border-gray-250 font-semibold text-base focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 transition-all"
                />
              </div>

              {/* Size Unit */}
              <div className="space-y-2">
                <label htmlFor="editFarmSizeUnit" className="block text-sm font-bold text-gray-700 uppercase tracking-wide">
                  {t('farm.wizard.unitLabel', 'Unit')}
                </label>
                <select
                  id="editFarmSizeUnit"
                  value={sizeUnit}
                  onChange={(e) => {
                    setValidationError('');
                    setSizeUnit(e.target.value);
                  }}
                  className="w-full h-12 px-4 rounded-xl border border-gray-250 font-bold text-base bg-white focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 transition-all cursor-pointer"
                >
                  <option value="CENT">{t('units.cent', 'Cent')}</option>
                  <option value="ACRE">{t('units.acre', 'Acre')}</option>
                  <option value="HECTARE">{t('units.hectare', 'Hectare')}</option>
                </select>
              </div>
            </div>

            {/* Farm Status */}
            <div className="space-y-2">
              <label htmlFor="editFarmStatus" className="block text-sm font-bold text-gray-700 uppercase tracking-wide">
                {t('farm.status', 'Status')}
              </label>
              <select
                id="editFarmStatus"
                value={formData.status}
                onChange={(e) => handleInputChange('status', e.target.value)}
                className="w-full h-12 px-4 rounded-xl border border-gray-250 font-bold text-base bg-white focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 transition-all cursor-pointer"
              >
                <option value="ACTIVE">{t('farm.active', 'Active')}</option>
                <option value="INACTIVE">{t('farm.inactive', 'Inactive')}</option>
              </select>
            </div>

            {/* Error Message */}
            {validationError && (
              <div className="rounded-xl border border-red-150 bg-red-50 p-4 flex items-start space-x-2.5 text-red-805 text-sm font-semibold leading-relaxed">
                <AlertCircle className="h-5 w-5 text-red-650 shrink-0 mt-0.5" />
                <span>{validationError}</span>
              </div>
            )}

            {/* Buttons */}
            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-150">
              <button
                type="button"
                onClick={() => navigate(`/farms/${farmId}`)}
                disabled={submitting}
                style={{ minHeight: '48px' }}
                className="flex items-center justify-center space-x-1 rounded-xl border border-gray-250 px-5 text-base font-bold text-gray-650 hover:bg-gray-50 active:scale-95 transition-all disabled:opacity-50 cursor-pointer"
              >
                <X className="h-4.5 w-4.5" />
                <span>{t('farm.actions.cancel', 'Cancel')}</span>
              </button>

              <button
                type="submit"
                disabled={submitting}
                style={{ minHeight: '48px' }}
                className="flex items-center justify-center space-x-1.5 rounded-xl bg-emerald-600 px-6 text-base font-bold text-white shadow-sm hover:bg-emerald-700 active:scale-95 transition-all disabled:opacity-50 cursor-pointer"
              >
                {submitting ? (
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                ) : (
                  <Save className="h-4.5 w-4.5" />
                )}
                <span>{t('farm.actions.save', 'Save Changes')}</span>
              </button>
            </div>
          </form>
        </div>
      </StateView>
    </div>
  );
};

export default EditFarmPage;
