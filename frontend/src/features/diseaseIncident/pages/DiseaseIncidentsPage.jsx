import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Plus, ShieldCheck, Filter, X } from 'lucide-react';
import StateView from '../../../shared/components/StateView';
import ProblemCard from '../components/ProblemCard';
import ReportProblemModal from '../components/ReportProblemModal';
import { fetchAllDiseaseIncidents } from '../api/diseaseIncidentApi';
import { fetchCropsList } from '../../crops/api/getSeasonCrops';

export const DiseaseIncidentsPage = () => {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const farmFilterId = searchParams.get('farmId');

  const [incidents, setIncidents] = useState([]);
  const [crops, setCrops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewState, setViewState] = useState('loading');
  const [activeTab, setActiveTab] = useState('ALL'); // ALL, ACTIVE, CLOSED
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setViewState('loading');
    try {
      const list = await fetchAllDiseaseIncidents();
      const cropsList = await fetchCropsList();

      setIncidents(list);
      setCrops(cropsList);

      if (list.length === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load disease incidents:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const farmCrops = farmFilterId
    ? crops.filter((c) => c.farmId === farmFilterId)
    : crops;
  const farmPlantingIds = new Set(farmCrops.map((c) => c.id));

  const scopedIncidents = farmFilterId
    ? incidents.filter((item) => farmPlantingIds.has(item.cropPlantingId))
    : incidents;

  const filteredIncidents = scopedIncidents.filter((item) => {
    if (activeTab === 'ACTIVE') return item.status !== 'CLOSED';
    if (activeTab === 'CLOSED') return item.status === 'CLOSED';
    return true;
  });

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Page Header */}
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[#222222] flex items-center space-x-2.5">
            <AlertTriangle className="h-7 w-7 text-[#DC2626]" />
            <span>{t('diseaseIncident.title', 'Crop Problems')}</span>
          </h1>
          <p className="text-sm font-semibold text-[#525252] mt-1">
            {t('diseaseIncident.subtitle', 'Track and manage field issues, pests, and crop diseases')}
          </p>
        </div>

        <button
          onClick={() => setIsReportModalOpen(true)}
          style={{ minHeight: '48px' }}
          className="px-5 py-2.5 rounded-xl bg-[#DC2626] hover:bg-[#B91C1C] text-white font-black shadow-2xs active:scale-95 transition-all cursor-pointer flex items-center justify-center space-x-2 shrink-0"
        >
          <Plus className="h-5 w-5" />
          <span>{t('diseaseIncident.reportButton', 'Report Problem')}</span>
        </button>
      </div>

      {/* Farm filter banner */}
      {farmFilterId && (
        <div className="flex items-center justify-between rounded-xl bg-red-50 border border-red-200 px-4 py-2.5 text-xs sm:text-sm font-bold text-red-800 animate-fade-in">
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-red-650 shrink-0" />
            <span>
              {t('diseaseIncident.filteredByFarm', 'Filtering crop problems by farm')}
              {farmCrops[0]?.farmName ? `: ${farmCrops[0].farmName}` : ''}
            </span>
          </div>
          <button
            onClick={() => {
              searchParams.delete('farmId');
              setSearchParams(searchParams);
            }}
            className="flex items-center space-x-1 text-red-700 hover:text-red-900 font-extrabold hover:underline cursor-pointer"
          >
            <X className="h-4 w-4" />
            <span>{t('common.clearFilter', 'Clear Filter')}</span>
          </button>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex items-center space-x-2 border-b border-[#C9DCCB] pb-3 overflow-x-auto select-none">
        <button
          onClick={() => setActiveTab('ALL')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'ALL'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('diseaseIncident.filterAll', 'All Issues')} ({scopedIncidents.length})
        </button>

        <button
          onClick={() => setActiveTab('ACTIVE')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'ACTIVE'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('diseaseIncident.filterOpen', 'Active Issues')} ({scopedIncidents.filter(i => i.status !== 'CLOSED').length})
        </button>

        <button
          onClick={() => setActiveTab('CLOSED')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'CLOSED'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('diseaseIncident.filterClosed', 'Closed Issues')} ({scopedIncidents.filter(i => i.status === 'CLOSED').length})
        </button>
      </div>

      {/* State View & Problems List */}
      <StateView
        state={viewState}
        onRetry={loadData}
        emptyTitle={t('diseaseIncident.emptyState', 'No crop health problems reported!')}
        emptyMessage={t('diseaseIncident.emptyStateDesc', 'Your fields look healthy and disease-free.')}
        emptyIcon={ShieldCheck}
        emptyActionText={t('diseaseIncident.reportButton', 'Report Problem')}
        onEmptyAction={() => setIsReportModalOpen(true)}
      >
        <div className="space-y-4">
          {filteredIncidents.map((incident) => (
            <ProblemCard key={incident.id} incident={incident} onStatusChange={loadData} />
          ))}
        </div>
      </StateView>

      {/* Report Problem Modal */}
      <ReportProblemModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSuccess={loadData}
        cropPlantings={farmCrops}
      />
    </div>
  );
};

export default DiseaseIncidentsPage;
