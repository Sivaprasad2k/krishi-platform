import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useFarms } from '../hooks/useFarms';
import FarmHeader from '../components/FarmHeader';
import FarmFormWizard from '../components/FarmFormWizard';

/**
 * CreateFarmPage mounts the creation wizard.
 */
const CreateFarmPage = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { createFarm } = useFarms();
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const handleSubmit = async (farmData) => {
    setSubmitting(true);
    setSubmitError('');
    
    // Construct backend-compatible payload
    const payload = {
      name: farmData.name,
      location: farmData.location,
      sizeHectares: farmData.sizeHectares
    };
    
    const result = await createFarm(payload);
    setSubmitting(false);

    if (result.success) {
      const createdFarm = result.data;
      if (createdFarm && createdFarm.id) {
        localStorage.setItem(`farm_pref_${createdFarm.id}`, JSON.stringify({
          unit: farmData.sizeUnit,
          photo: farmData.photo
        }));
      }
      navigate('/farms');
    } else {
      setSubmitError(result.error || t('states.errorDescription', 'Failed to create farm. Please verify input fields.'));
    }
  };

  return (
    <div className="space-y-6">
      <FarmHeader
        title={t('farm.create', 'Create Farm')}
        subtitle={t('farm.wizard.step1Desc', 'Register a new operational farm aggregate.')}
        onBack={() => navigate('/farms')}
      />

      <div className="max-w-2xl mx-auto">
        <FarmFormWizard
          onSubmit={handleSubmit}
          submitting={submitting}
          submitError={submitError}
        />
      </div>
    </div>
  );
};

export default CreateFarmPage;
