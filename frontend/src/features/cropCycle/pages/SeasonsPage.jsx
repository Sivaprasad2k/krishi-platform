import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { useSeasons } from '../hooks/useSeasons';
import { useFarms } from '../../farm/hooks/useFarms';
import FarmHeader from '../../farm/components/FarmHeader';
import StateView from '../../../shared/components/StateView';
import SeasonCard from '../components/SeasonCard';
import SeasonEmptyState from '../components/SeasonEmptyState';

/**
 * SeasonsPage lists all crop seasons under a specific Farm aggregate.
 */
const SeasonsPage = () => {
  const { farmId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const { seasons, loading, error, fetchFarmSeasons } = useSeasons();
  const { selectedFarm, fetchFarm } = useFarms();
  const [internalState, setInternalState] = useState('loading');

  const loadData = async () => {
    setInternalState('loading');
    const [farmRes, seasonsRes] = await Promise.all([
      fetchFarm(farmId),
      fetchFarmSeasons(farmId)
    ]);
    
    if (!farmRes.success || !seasonsRes.success) {
      setInternalState(navigator.onLine ? 'error' : 'offline');
    } else {
      setInternalState('success');
    }
  };

  useEffect(() => {
    loadData();
  }, [farmId]);

  const handleStartSeason = () => {
    navigate(`/farms/${farmId}/seasons/new`);
  };

  const handleStartSeasonButton = (
    <button
      onClick={handleStartSeason}
      style={{ minHeight: '44px' }}
      className="flex items-center justify-center space-x-1 rounded-xl bg-emerald-600 px-4 text-base font-bold text-white shadow-sm hover:bg-emerald-700 active:scale-95 transition-all cursor-pointer"
    >
      <Plus className="h-4.5 w-4.5" />
      <span>{t('season.create', 'Start Season')}</span>
    </button>
  );

  return (
    <div className="space-y-6">
      <FarmHeader
        title={t('season.title', 'Seasons')}
        subtitle={selectedFarm?.name || ''}
        onBack={() => navigate(`/farms/${farmId}`)}
        rightElement={seasons.length > 0 ? handleStartSeasonButton : null}
      />

      <StateView state={internalState} onRetry={loadData}>
        {seasons.length === 0 ? (
          <div className="py-12">
            <SeasonEmptyState onStart={handleStartSeason} />
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {seasons.map((season) => (
              <SeasonCard key={season.id} season={season} />
            ))}
          </div>
        )}
      </StateView>
    </div>
  );
};

export default SeasonsPage;
