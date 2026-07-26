import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import CropFormWizard from '../components/CropFormWizard';
import { getCrop } from '../api/getCrop';
import { updateCrop } from '../api/updateCrop';
import apiClient from '../../../core/api/axios';
import StateView from '../../../shared/components/StateView';

export const EditCropPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const [crop, setCrop] = useState(null);
  const [viewState, setViewState] = useState('loading');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const loadCropAndSeason = async () => {
    setViewState('loading');
    setError(null);
    try {
      const cropRes = await getCrop(id);
      const cropData = cropRes.data;
      setCrop(cropData);

      const seasonRes = await apiClient.get(`/crop-cycles/${cropData.cropCycleId}`);
      const seasonData = seasonRes.data?.data;

      if (seasonData.status === 'COMPLETED' || seasonData.status === 'CANCELLED') {
        setError(t('crops.edit.errClosedSeason', 'Cannot edit crop planting details because the parent season is completed or cancelled.'));
        setViewState('error');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load crop edit details:', err);
      setError(err.message || t('crops.edit.errLoad', 'Failed to retrieve crop details.'));
      setViewState('error');
    }
  };

  useEffect(() => {
    loadCropAndSeason();
  }, [id]);

  const handleEditCropSubmit = async (formData) => {
    setIsSubmitting(true);
    try {
      await updateCrop(id, formData);
      navigate(`/crops/${id}`);
    } catch (err) {
      console.error('Failed to update crop:', err);
      alert(err.message || t('crops.edit.failed', 'Failed to update crop. Please check inputs.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-xl mx-auto">
      {/* Header */}
      <div className="flex items-center space-x-3 bg-white p-4 rounded-2xl border border-gray-200 shadow-sm">
        <button
          onClick={() => navigate(`/crops/${id}`)}
          className="p-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-600 transition-all"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-xl font-black text-gray-800">
            {t('crops.edit.title', 'Edit Crop Details')}
          </h1>
          {crop && (
            <p className="text-xs font-semibold text-gray-400">
              {crop.cropName} ({crop.cropVariety})
            </p>
          )}
        </div>
      </div>

      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
        <StateView state={viewState} onRetry={loadCropAndSeason} errorTitle={error}>
          {crop && (
            <CropFormWizard
              onSubmit={handleEditCropSubmit}
              isSubmitting={isSubmitting}
              initialValues={{
                cropName: crop.cropName,
                cropVariety: crop.cropVariety,
                plantingDate: crop.plantingDate,
                expectedHarvestDate: crop.expectedHarvestDate
              }}
            />
          )}
        </StateView>
      </div>
    </div>
  );
};

export default EditCropPage;
