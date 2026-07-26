import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSeasons } from '../hooks/useSeasons';
import { useFarms } from '../../farm/hooks/useFarms';
import FarmHeader from '../../farm/components/FarmHeader';
import StateView from '../../../shared/components/StateView';
import SeasonFormWizard from '../components/SeasonFormWizard';

/**
 * CreateSeasonPage coordinates wizard onboarding for a new season.
 */
const CreateSeasonPage = () => {
  const { farmId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const { createSeason } = useSeasons();
  const { selectedFarm, fetchFarm } = useFarms();
  const [internalState, setInternalState] = useState('loading');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

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

  const handleSubmit = async (seasonData) => {
    setSubmitting(true);
    setSubmitError('');
    const result = await createSeason(seasonData);
    setSubmitting(false);

    if (result.success) {
      navigate(`/farms/${farmId}`);
    } else {
      setSubmitError(result.error || t('states.errorDescription', 'Failed to create season.'));
    }
  };

  return (
    <div className="space-y-6">
      <FarmHeader
        title={t('season.create', 'Start Season')}
        subtitle={selectedFarm?.name || ''}
        onBack={() => navigate(`/farms/${farmId}`)}
      />

      <StateView state={internalState} onRetry={loadData}>
        {selectedFarm && (
          <div className="max-w-2xl mx-auto">
            <SeasonFormWizard
              farmName={selectedFarm.name}
              farmId={farmId}
              onSubmit={handleSubmit}
              submitting={submitting}
              submitError={submitError}
            />
          </div>
        )}
      </StateView>
    </div>
  );
};

export default CreateSeasonPage;
