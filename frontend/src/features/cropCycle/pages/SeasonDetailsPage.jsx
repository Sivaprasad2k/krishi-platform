import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Calendar, Play, CheckCircle2, AlertTriangle, Sprout, Ban, Clock, AlertCircle } from 'lucide-react';
import { useSeasons } from '../hooks/useSeasons';
import FarmHeader from '../../farm/components/FarmHeader';
import StateView from '../../../shared/components/StateView';
import SeasonStatusBadge from '../components/SeasonStatusBadge';
import { fetchSeasonCropsWithDetails } from '../../crops/api/getSeasonCrops';
import CropCard from '../../crops/components/CropCard';

/**
 * SeasonDetailsPage displays the full parameters of a crop season and allows workflow state transitions.
 */
const SeasonDetailsPage = () => {
  const { seasonId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const { selectedSeason, loading, error, fetchSeason, startSeason, completeSeason, cancelSeason, clearErrors } = useSeasons();
  const [internalState, setInternalState] = useState('loading');
  const [transitioning, setTransitioning] = useState(false);
  const [transitionError, setTransitionError] = useState('');
  const [seasonCrops, setSeasonCrops] = useState([]);

  const loadData = async () => {
    setInternalState('loading');
    setTransitionError('');
    clearErrors();
    const res = await fetchSeason(seasonId);
    if (!res.success) {
      setInternalState(navigator.onLine ? 'error' : 'offline');
    } else {
      try {
        const cropsList = await fetchSeasonCropsWithDetails(seasonId);
        setSeasonCrops(cropsList);
        setInternalState('success');
      } catch (err) {
        console.error('Failed to load season crops:', err);
        setInternalState('success');
      }
    }
  };

  useEffect(() => {
    loadData();
  }, [seasonId]);

  const handleStart = async () => {
    setTransitioning(true);
    setTransitionError('');
    const res = await startSeason(seasonId);
    setTransitioning(false);
    if (!res.success) {
      setTransitionError(res.error || t('states.errorOccurred', 'State transition failed'));
    }
  };

  const handleComplete = async () => {
    setTransitioning(true);
    setTransitionError('');
    const res = await completeSeason(seasonId);
    setTransitioning(false);
    if (!res.success) {
      setTransitionError(res.error || t('states.errorOccurred', 'State transition failed'));
    }
  };

  const handleCancel = async () => {
    setTransitioning(true);
    setTransitionError('');
    const res = await cancelSeason(seasonId);
    setTransitioning(false);
    if (!res.success) {
      setTransitionError(res.error || t('states.errorOccurred', 'State transition failed'));
    }
  };

  const formattedDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const status = selectedSeason?.status?.toUpperCase() || 'DRAFT';
  const isEditable = status === 'DRAFT' || status === 'ACTIVE';

  return (
    <div className="space-y-6">
      <FarmHeader
        title={selectedSeason?.name || t('season.details', 'Season Details')}
        subtitle={selectedSeason ? t('season.title', 'Season') : ''}
        onBack={() => selectedSeason && navigate(`/farms/${selectedSeason.farmId}`)}
      />

      <StateView state={internalState} onRetry={loadData}>
        {selectedSeason && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="bg-white rounded-2xl border border-[#C9DCCB] p-6 sm:p-8 shadow-2xs space-y-6">
              
              {/* Header Info */}
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-4 border-b border-[#C9DCCB]">
                <div>
                  <h2 className="text-2xl font-black text-[#222222] leading-tight">
                    {selectedSeason.name}
                  </h2>
                  <div className="flex items-center text-sm font-semibold text-[#525252] mt-1">
                    <Sprout className="h-4.5 w-4.5 text-[#087A55] mr-2 shrink-0" />
                    <span>{t(`season.types.${selectedSeason.seasonType}`, selectedSeason.seasonType)} {selectedSeason.year}</span>
                  </div>
                </div>
                <SeasonStatusBadge status={status} />
              </div>

              {/* Detail parameters */}
              <div className="space-y-4 text-base">
                <div className="flex justify-between items-center border-b border-[#C9DCCB] pb-2.5">
                  <span className="font-black text-[#525252] uppercase text-xs tracking-wider">
                    {t('season.createdAt', 'Created Date')}
                  </span>
                  <span className="font-black text-[#222222]">
                    {formattedDate(selectedSeason.createdAt) || 'N/A'}
                  </span>
                </div>

                <div className="flex justify-between items-center border-b border-[#C9DCCB] pb-2.5">
                  <span className="font-black text-[#525252] uppercase text-xs tracking-wider">
                    {t('season.startDate', 'Start Date')}
                  </span>
                  <span className="font-black text-[#222222]">
                    {formattedDate(selectedSeason.startDate) || (
                      <span className="text-[#6B7280] font-semibold italic">Not Started</span>
                    )}
                  </span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="font-black text-[#525252] uppercase text-xs tracking-wider">
                    {t('season.endDate', 'End Date')}
                  </span>
                  <span className="font-black text-[#222222]">
                    {formattedDate(selectedSeason.endDate) || (
                      <span className="text-[#6B7280] font-semibold italic">Ongoing</span>
                    )}
                  </span>
                </div>

                {selectedSeason.notes && (
                  <div className="flex flex-col pt-3 border-t border-[#C9DCCB] text-left">
                    <span className="font-black text-[#525252] uppercase text-xs tracking-wider mb-1">
                      {t('season.notes', 'Notes')}
                    </span>
                    <p className="text-sm font-semibold text-[#525252] italic bg-[#F0F7F1] p-3.5 rounded-xl border border-[#C9DCCB] leading-relaxed">
                      {selectedSeason.notes}
                    </p>
                  </div>
                )}
              </div>

              {/* Error messages */}
              {transitionError && (
                <div className="rounded-xl border border-[#DC2626] bg-[#FEF2F2] p-4 flex items-start space-x-2.5 text-[#DC2626] text-sm font-semibold leading-relaxed animate-fade-in">
                  <AlertCircle className="h-5 w-5 text-[#DC2626] shrink-0 mt-0.5" />
                  <span>{transitionError}</span>
                </div>
              )}

              {/* Workflow Actions */}
              {(status === 'DRAFT' || status === 'ACTIVE') && (
                <div className="flex flex-col sm:flex-row gap-4 pt-4 border-t border-[#C9DCCB]">
                  {status === 'DRAFT' && (
                    <button
                      onClick={handleStart}
                      disabled={transitioning}
                      style={{ minHeight: '48px' }}
                      className="flex-1 flex items-center justify-center space-x-2 rounded-xl bg-[#087A55] hover:bg-[#066747] active:scale-95 text-white font-black px-6 shadow-2xs transition-all cursor-pointer disabled:opacity-50"
                    >
                      {transitioning ? (
                        <span className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      ) : (
                        <Play className="h-5 w-5 fill-white" />
                      )}
                      <span>{t('season.actions.start', 'Start Growing')}</span>
                    </button>
                  )}

                  {status === 'ACTIVE' && (
                    <>
                      <button
                        onClick={handleComplete}
                        disabled={transitioning}
                        style={{ minHeight: '48px' }}
                        className="flex-1 flex items-center justify-center space-x-2 rounded-xl bg-[#087A55] hover:bg-[#066747] active:scale-95 text-white font-black px-6 shadow-2xs transition-all cursor-pointer disabled:opacity-50"
                      >
                        {transitioning ? (
                          <span className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                        ) : (
                          <CheckCircle2 className="h-5 w-5" />
                        )}
                        <span>{t('season.actions.complete', 'Mark Harvested')}</span>
                      </button>

                      <button
                        onClick={handleCancel}
                        disabled={transitioning}
                        style={{ minHeight: '48px' }}
                        className="flex-1 flex items-center justify-center space-x-2 rounded-xl border border-[#DC2626] bg-[#FEF2F2] hover:bg-red-100 text-[#DC2626] font-black px-6 shadow-2xs transition-all cursor-pointer disabled:opacity-50"
                      >
                        {transitioning ? (
                          <span className="h-5 w-5 animate-spin rounded-full border-2 border-[#DC2626] border-t-transparent" />
                        ) : (
                          <Ban className="h-5 w-5" />
                        )}
                        <span>{t('season.actions.cancel', 'Stop Season')}</span>
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Crops in this Season */}
            <div className="bg-white rounded-2xl border border-[#C9DCCB] p-6 sm:p-8 shadow-2xs space-y-4">
              <div className="flex justify-between items-center pb-3 border-b border-[#C9DCCB]">
                <div className="flex items-center space-x-2">
                  <Sprout className="h-5 w-5 text-[#087A55]" />
                  <h3 className="text-lg font-black text-[#222222]">
                    {t('season.crops.title', 'Crops Planted')}
                  </h3>
                </div>
                {isEditable && (
                  <button
                    onClick={() => navigate(`/crop-cycles/${seasonId}/crops/new`)}
                    style={{ minHeight: '44px' }}
                    className="rounded-xl bg-[#087A55] hover:bg-[#066747] px-4 py-2 text-xs font-black text-white active:scale-95 transition-all shadow-2xs cursor-pointer"
                  >
                    {t('season.crops.add', 'Add Crop')}
                  </button>
                )}
              </div>

              {seasonCrops.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {seasonCrops.map((c) => (
                    <CropCard key={c.id} crop={{ ...c, seasonName: selectedSeason.name }} />
                  ))}
                </div>
              ) : (
                <div className="text-center py-6 text-gray-400 font-bold text-sm bg-gray-50 rounded-xl border border-gray-100">
                  {t('season.crops.empty', 'No crops planted in this season yet.')}
                </div>
              )}
            </div>
          </div>
        )}
      </StateView>
    </div>
  );
};

export default SeasonDetailsPage;
