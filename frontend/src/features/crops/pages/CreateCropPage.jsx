import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import CropFormWizard from '../components/CropFormWizard';
import { createCrop } from '../api/createCrop';
import apiClient from '../../../core/api/axios';
import StateView from '../../../shared/components/StateView';

export const CreateCropPage = () => {
  const { id: seasonId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const [season, setSeason] = useState(null);
  const [viewState, setViewState] = useState('loading');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const loadSeasonDetails = async () => {
    setViewState('loading');
    try {
      const response = await apiClient.get(`/crop-cycles/${seasonId}`);
      const cycle = response.data?.data;
      setSeason(cycle);
      
      if (cycle.status === 'COMPLETED' || cycle.status === 'CANCELLED') {
        setError(t('crops.create.errClosedSeason', 'Cannot sow a crop on a completed or cancelled season.'));
        setViewState('error');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load season details:', err);
      setViewState('error');
    }
  };

  useEffect(() => {
    loadSeasonDetails();
  }, [seasonId]);

  const handleCreateCropSubmit = async (formData) => {
    setIsSubmitting(true);
    try {
      const payload = {
        cropCycleId: seasonId,
        ...formData
      };
      const response = await createCrop(payload);
      const createdCrop = response.data;
      
      navigate(`/crops/${createdCrop.id}`);
    } catch (err) {
      console.error('Failed to plant crop:', err);
      alert(err.message || t('crops.create.failed', 'Failed to plant crop. Please check inputs.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-xl mx-auto">
      {/* Header */}
      <div className="flex items-center space-x-3 bg-white p-4 rounded-2xl border border-gray-200 shadow-sm">
        <button
          onClick={() => navigate(`/seasons/${seasonId}`)}
          className="p-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-600 transition-all"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-xl font-black text-gray-800">
            {t('crops.create.title', 'Plant a Crop')}
          </h1>
          {season && (
            <p className="text-xs font-semibold text-gray-400">
              {t('crops.create.subtitle', 'Sowing inside season: {{seasonName}}', { seasonName: season.name })}
            </p>
          )}
        </div>
      </div>

      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
        <StateView state={viewState} onRetry={loadSeasonDetails} errorTitle={error}>
          <CropFormWizard onSubmit={handleCreateCropSubmit} isSubmitting={isSubmitting} />
        </StateView>
      </div>
    </div>
  );
};

export default CreateCropPage;
