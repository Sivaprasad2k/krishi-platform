import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Edit3, Calendar, MapPin, Scale, HelpCircle, Layers, Sprout, AlertTriangle, CheckSquare, Plus } from 'lucide-react';
import { useFarms } from '../hooks/useFarms';
import FarmHeader from '../components/FarmHeader';
import StateView from '../../../shared/components/StateView';
import { fromHectares, formatArea } from '../../../shared/utils/areaConversion';
import { useSeasons } from '../../cropCycle/hooks/useSeasons';
import SeasonStatusBadge from '../../cropCycle/components/SeasonStatusBadge';
import WeatherSummaryWidget from '../../weather/components/WeatherSummaryWidget';
import WeatherAdvisoryBanner from '../../weather/components/WeatherAdvisoryBanner';
import { getFarmAdvisories } from '../../weather/api/weatherApi';

/**
 * FarmDetailsPage displays full operational stats for a specific farm
 * and shows placeholders for pending sub-feature phases.
 */
const FarmDetailsPage = () => {
  const { farmId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { selectedFarm, loading, error, fetchFarm } = useFarms();
  const { seasons, fetchFarmSeasons } = useSeasons();
  const [internalState, setInternalState] = useState('loading');
  const [farmAdvisories, setFarmAdvisories] = useState([]);

  const loadData = async () => {
    setInternalState('loading');
    const [farmRes, seasonsRes] = await Promise.all([
      fetchFarm(farmId),
      fetchFarmSeasons(farmId)
    ]);

    try {
      const advRes = await getFarmAdvisories(farmId);
      if (advRes && advRes.data) {
        setFarmAdvisories(advRes.data);
      }
    } catch (e) {
      console.warn('Failed to load farm advisories:', e);
    }

    if (!farmRes.success || !seasonsRes.success) {
      setInternalState(navigator.onLine ? 'error' : 'offline');
    } else {
      setInternalState('success');
    }
  };

  useEffect(() => {
    loadData();
  }, [farmId]);

  useEffect(() => {
    if (!loading && selectedFarm && selectedFarm?.id === farmId) {
      setInternalState('success');
    }
  }, [loading, selectedFarm, farmId]);

  const handleEditButton = (
    <button
      onClick={() => navigate(`/farms/${farmId}/edit`)}
      style={{ minHeight: '48px' }}
      className="flex items-center justify-center space-x-1.5 rounded-xl border border-[#C9DCCB] bg-white px-5 text-sm font-black text-[#222222] hover:bg-[#E3F2E6] active:scale-95 transition-all cursor-pointer shadow-2xs"
    >
      <Edit3 className="h-4.5 w-4.5" />
      <span>{t('farm.actions.editFarm', 'Edit Farm')}</span>
    </button>
  );

  const formattedDate = selectedFarm?.createdAt
    ? new Date(selectedFarm.createdAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    : '';

  // Load preferences from localStorage
  const pref = localStorage.getItem(`farm_pref_${selectedFarm?.id}`);
  const { unit, photo } = pref ? JSON.parse(pref) : { unit: 'HECTARE', photo: null };

  const valueInPrefUnit = selectedFarm ? fromHectares(selectedFarm.sizeHectares, unit) : 0;
  const formattedArea = selectedFarm ? formatArea(valueInPrefUnit, unit, t) : '';

  return (
    <div className="space-y-6">
      <FarmHeader
        title={selectedFarm?.name || t('farm.details', 'Farm Details')}
        subtitle={t('farm.details', 'Farm Details')}
        onBack={() => navigate('/farms')}
        rightElement={selectedFarm ? handleEditButton : null}
      />

      <StateView state={internalState} onRetry={loadData}>
        {selectedFarm && (
          <div className="space-y-8">
            {/* Optional Farm Photo Banner */}
            {photo && (
              <div className="w-full h-64 md:h-80 rounded-2xl overflow-hidden border border-[#C9DCCB] bg-[#F0F7F1] shadow-2xs relative animate-fade-in">
                <img
                  src={photo}
                  alt={selectedFarm?.name || ''}
                  className="w-full h-full object-cover"
                />
              </div>
            )}

            {/* Core Info Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {/* Location Card */}
              <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs flex items-center space-x-4">
                <div className="rounded-xl bg-[#E3F2E6] p-3 text-[#087A55]">
                  <MapPin className="h-6 w-6" />
                </div>
                <div className="min-w-0">
                  <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                    {t('farm.location', 'Location')}
                  </span>
                  <span className="text-base font-black text-[#222222] mt-0.5 block truncate">
                    {selectedFarm?.location || ''}
                  </span>
                  {(selectedFarm?.district || selectedFarm?.state) && (
                    <span className="text-xs font-semibold text-gray-500 block truncate mt-0.5">
                      {[selectedFarm.district, selectedFarm.state].filter(Boolean).join(', ')}
                    </span>
                  )}
                  {selectedFarm?.latitude != null && selectedFarm?.longitude != null && (
                    <span className="text-[11px] font-mono text-gray-400 block mt-0.5">
                      GPS: {Number(selectedFarm.latitude).toFixed(4)}°, {Number(selectedFarm.longitude).toFixed(4)}°
                    </span>
                  )}
                </div>
              </div>

              {/* Size Card */}
              <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs flex items-center space-x-4">
                <div className="rounded-xl bg-[#E3F2E6] p-3 text-[#087A55]">
                  <Scale className="h-6 w-6" />
                </div>
                <div>
                  <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                    {t('farm.size', 'Size')}
                  </span>
                  <span className="text-base font-black text-[#222222] mt-0.5 block">
                    {formattedArea}
                  </span>
                </div>
              </div>

              {/* Status / Created At Card */}
              <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs flex items-center space-x-4">
                <div className="rounded-xl bg-[#E3F2E6] p-3 text-[#087A55]">
                  <Calendar className="h-6 w-6" />
                </div>
                <div>
                  <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                    {t('farm.createdAt', 'Created Date')}
                  </span>
                  <span className="text-base font-black text-[#222222] mt-0.5 block">
                    {formattedDate || 'N/A'}
                  </span>
                </div>
              </div>
            </div>

            {/* Weather Advisories & Intelligence */}
            <div className="space-y-4 pt-4 border-t border-gray-200">
              <h2 className="text-xl font-black text-gray-800">
                {t('farm.weather', 'Weather Intelligence & Advisories')}
              </h2>
              <WeatherAdvisoryBanner
                advisories={farmAdvisories}
                onAcknowledge={(id) => setFarmAdvisories(prev => prev.filter(a => a.id !== id))}
              />
              <WeatherSummaryWidget farmId={farmId} farmName={selectedFarm?.name} />
            </div>

            {/* Operational Workflow Panels */}
            <div className="space-y-6 pt-4 border-t border-gray-200">
              <h2 className="text-xl font-black text-gray-800">
                {t('farm.operations', 'Farm Operations & Workflows')}
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* 1. Growing Seasons */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200 flex flex-col justify-between min-h-[220px]">
                  <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                    <div className="flex items-center space-x-2 text-gray-800">
                      <Layers className="h-5.5 w-5.5 text-emerald-600" />
                      <h3 className="text-lg font-black">{t('season.currentSeasons', 'Current Seasons')}</h3>
                    </div>
                    {seasons.length > 0 && (
                      <button
                        onClick={() => navigate(`/farms/${farmId}/seasons/new`)}
                        style={{ minHeight: '36px' }}
                        className="rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-bold text-xs px-3 py-1 flex items-center space-x-1 cursor-pointer transition-all border border-emerald-150"
                      >
                        <Plus className="h-3.5 w-3.5" />
                        <span>{t('season.create', 'Start Season')}</span>
                      </button>
                    )}
                  </div>

                  {seasons.length === 0 ? (
                    <div className="flex-1 flex flex-col items-center justify-center py-4 space-y-3">
                      <p className="text-sm font-semibold text-gray-400">
                        {t('season.emptyState', 'Start your first crop season')}
                      </p>
                      <button
                        onClick={() => navigate(`/farms/${farmId}/seasons/new`)}
                        style={{ minHeight: '40px' }}
                        className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm px-4 py-2 flex items-center space-x-1.5 cursor-pointer shadow-sm active:scale-95 transition-all"
                      >
                        <Plus className="h-4.5 w-4.5" />
                        <span>{t('season.create', 'Start Season')}</span>
                      </button>
                    </div>
                  ) : (
                    <div className="flex-1 mt-3 space-y-2 max-h-[180px] overflow-y-auto pr-1">
                      {seasons.slice(0, 3).map((season) => (
                        <div
                          key={season.id}
                          onClick={() => navigate(`/seasons/${season.id}`)}
                          className="flex items-center justify-between p-3 rounded-xl border border-gray-150 hover:border-emerald-350 bg-gray-50/20 hover:bg-emerald-50/5 cursor-pointer transition-all"
                        >
                          <div className="min-w-0 flex-1 pr-2">
                            <p className="font-extrabold text-sm text-gray-850 truncate">{season.name}</p>
                            <p className="font-semibold text-xs text-gray-400 truncate mt-0.5">{season.cropName}</p>
                          </div>
                          <SeasonStatusBadge status={season.status} />
                        </div>
                      ))}
                      {seasons.length > 3 && (
                        <button
                          onClick={() => navigate(`/farms/${farmId}/seasons`)}
                          className="w-full text-center text-xs font-bold text-emerald-700 hover:underline pt-1.5"
                        >
                          {t('farm.actions.viewDetails', 'View details')} ({seasons.length})
                        </button>
                      )}
                    </div>
                  )}
                </div>

                {/* 2. Crop Plantings */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200 flex flex-col justify-between min-h-[180px] hover:border-emerald-300 transition-all shadow-2xs">
                  <div>
                    <div className="flex items-center space-x-3 text-gray-800">
                      <div className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                        <Sprout className="h-5.5 w-5.5" />
                      </div>
                      <h3 className="text-lg font-black">{t('nav.crops', 'Crops & Plantings')}</h3>
                    </div>
                    <p className="text-sm font-semibold text-gray-500 mt-2.5">
                      {t('farm.cropsDesc', 'Track active crop cycles, plantings, growth stages, and cultivation plans for this farm.')}
                    </p>
                  </div>
                  <div className="mt-4 flex justify-end">
                    <button
                      onClick={() => navigate(`/crops?farmId=${farmId}`)}
                      style={{ minHeight: '40px' }}
                      className="px-4 py-2 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-750 font-black text-sm transition-all cursor-pointer flex items-center space-x-1 border border-emerald-200 shadow-2xs"
                    >
                      <span>{t('farm.actions.viewFarmCrops', 'View Farm Crops')}</span>
                    </button>
                  </div>
                </div>

                {/* 3. Crop Problems */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200 flex flex-col justify-between min-h-[180px] hover:border-red-300 transition-all shadow-2xs">
                  <div>
                    <div className="flex items-center space-x-3 text-gray-800">
                      <div className="rounded-xl bg-red-50 p-2 text-red-600">
                        <AlertTriangle className="h-5.5 w-5.5" />
                      </div>
                      <h3 className="text-lg font-black">{t('diseaseIncident.title', 'Crop Problems')}</h3>
                    </div>
                    <p className="text-sm font-semibold text-gray-500 mt-2.5">
                      {t('farm.problemsDesc', 'Monitor and resolve disease incidents, pest threats, and treatment regimens on this farm.')}
                    </p>
                  </div>
                  <div className="mt-4 flex justify-end">
                    <button
                      onClick={() => navigate(`/disease-incidents?farmId=${farmId}`)}
                      style={{ minHeight: '40px' }}
                      className="px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-750 font-black text-sm transition-all cursor-pointer flex items-center space-x-1 border border-red-200 shadow-2xs"
                    >
                      <span>{t('farm.actions.viewFarmProblems', 'View Crop Problems')}</span>
                    </button>
                  </div>
                </div>

                {/* 4. Tasks */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200 flex flex-col justify-between min-h-[180px] hover:border-emerald-300 transition-all shadow-2xs">
                  <div>
                    <div className="flex items-center space-x-3 text-gray-800">
                      <div className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                        <CheckSquare className="h-5.5 w-5.5" />
                      </div>
                      <h3 className="text-lg font-black">{t('nav.tasks', 'Farm Tasks')}</h3>
                    </div>
                    <p className="text-sm font-semibold text-gray-500 mt-2.5">
                      {t('farm.tasksDesc', 'Log and execute operational farming tasks, irrigation, sprays, and field activities.')}
                    </p>
                  </div>
                  <div className="mt-4 flex justify-end">
                    <button
                      onClick={() => navigate(`/tasks?farmId=${farmId}`)}
                      style={{ minHeight: '40px' }}
                      className="px-4 py-2 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-750 font-black text-sm transition-all cursor-pointer flex items-center space-x-1 border border-emerald-200 shadow-2xs"
                    >
                      <span>{t('farm.actions.viewFarmTasks', 'View Farm Tasks')}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </StateView>
    </div>
  );
};

export default FarmDetailsPage;
