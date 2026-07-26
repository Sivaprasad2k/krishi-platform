import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Plus, Map } from 'lucide-react';
import { useFarms } from '../hooks/useFarms';
import FarmHeader from '../components/FarmHeader';
import FarmCard from '../components/FarmCard';
import FarmEmptyState from '../components/FarmEmptyState';
import StateView from '../../../shared/components/StateView';

/**
 * FarmsPage acts as the main dashboard for a farmer's registered farms.
 */
const FarmsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { farms, loading, error, fetchFarms } = useFarms();
  const [internalState, setInternalState] = useState('loading');

  const loadData = async () => {
    setInternalState('loading');
    const res = await fetchFarms();
    if (res.success) {
      if (!res.data || res.data.length === 0) {
        // Wait, fetchFarms in the store returns { success: true } but puts the data in the store state. Let's check `farms` from store.
        // So we evaluate the store's state after calling fetchFarms.
      }
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (!loading) {
      if (error) {
        setInternalState(navigator.onLine ? 'error' : 'offline');
      } else if (farms.length === 0) {
        setInternalState('empty');
      } else {
        setInternalState('success');
      }
    }
  }, [loading, error, farms]);

  const handleCreateCTA = (
    <button
      onClick={() => navigate('/farms/new')}
      style={{ minHeight: '48px' }}
      className="flex items-center justify-center space-x-1.5 rounded-xl bg-emerald-600 px-5 text-base font-bold text-white shadow-sm hover:bg-emerald-700 active:scale-95 transition-all cursor-pointer"
    >
      <Plus className="h-5 w-5" />
      <span>{t('farm.create', 'Create Farm')}</span>
    </button>
  );

  return (
    <div className="space-y-6">
      <FarmHeader
        title={t('farm.title', 'My Farms')}
        subtitle={t('dashboard.status', 'Farm Operations Overview')}
        rightElement={farms.length > 0 ? handleCreateCTA : null}
      />

      <StateView
        state={internalState}
        onRetry={loadData}
        emptyTitle={t('states.noFarmsTitle', 'No Farms Found')}
        emptyMessage={t('states.noFarmsDesc', 'You have not added a farm yet.')}
        emptyActionText={t('farm.create', 'Add My First Farm')}
        onEmptyAction={() => navigate('/farms/new')}
        emptyIcon={Map}
      >
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {farms.map((farm) => (
            <FarmCard key={farm.id} farm={farm} />
          ))}
        </div>
      </StateView>
    </div>
  );
};

export default FarmsPage;
