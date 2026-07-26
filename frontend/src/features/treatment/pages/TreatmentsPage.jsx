import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Droplet, Plus, ShieldCheck } from 'lucide-react';
import StateView from '../../../shared/components/StateView';
import TreatmentCard from '../components/TreatmentCard';
import AddTreatmentModal from '../components/AddTreatmentModal';
import { fetchAllTreatments } from '../api/treatmentApi';
import { fetchAllDiseaseIncidents } from '../../diseaseIncident/api/diseaseIncidentApi';

export const TreatmentsPage = () => {
  const { t } = useTranslation();

  const [treatments, setTreatments] = useState([]);
  const [diseaseIncidents, setDiseaseIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewState, setViewState] = useState('loading');
  const [activeTab, setActiveTab] = useState('ALL'); // ALL, PLANNED, IN_PROGRESS, COMPLETED
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setViewState('loading');
    try {
      const list = await fetchAllTreatments();
      const incidentsList = await fetchAllDiseaseIncidents();

      setTreatments(list);
      setDiseaseIncidents(incidentsList);

      if (list.length === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load treatments:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const filteredTreatments = treatments.filter((item) => {
    if (activeTab === 'PLANNED') return item.status === 'PLANNED';
    if (activeTab === 'IN_PROGRESS') return item.status === 'IN_PROGRESS';
    if (activeTab === 'COMPLETED') return item.status === 'COMPLETED';
    return true;
  });

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Page Header */}
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[#222222] flex items-center space-x-2.5">
            <Droplet className="h-7 w-7 text-[#2563EB]" />
            <span>{t('treatment.title', 'Treatment Plans')}</span>
          </h1>
          <p className="text-sm font-semibold text-[#525252] mt-1">
            {t('treatment.subtitle', 'Manage spray schedules, organic applications, and curative treatments')}
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          style={{ minHeight: '48px' }}
          className="px-5 py-2.5 rounded-xl bg-[#087A55] hover:bg-[#066747] text-white font-black shadow-2xs active:scale-95 transition-all cursor-pointer flex items-center justify-center space-x-2 shrink-0"
        >
          <Plus className="h-5 w-5" />
          <span>{t('treatment.addButton', 'Plan Treatment')}</span>
        </button>
      </div>

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
          {t('treatment.filterAll', 'All Treatments')} ({treatments.length})
        </button>

        <button
          onClick={() => setActiveTab('PLANNED')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'PLANNED'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('treatment.filterPlanned', 'Planned')} ({treatments.filter(t => t.status === 'PLANNED').length})
        </button>

        <button
          onClick={() => setActiveTab('IN_PROGRESS')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'IN_PROGRESS'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('treatment.filterInProgress', 'In Progress')} ({treatments.filter(t => t.status === 'IN_PROGRESS').length})
        </button>

        <button
          onClick={() => setActiveTab('COMPLETED')}
          style={{ minHeight: '48px' }}
          className={`px-4 py-2 rounded-xl text-sm font-black transition-all cursor-pointer ${
            activeTab === 'COMPLETED'
              ? 'bg-[#DCFCE7] text-[#14532D] border-2 border-[#087A55] shadow-2xs'
              : 'bg-white text-[#525252] border border-[#C9DCCB] hover:bg-[#E3F2E6]'
          }`}
        >
          {t('treatment.filterCompleted', 'Completed')} ({treatments.filter(t => t.status === 'COMPLETED').length})
        </button>
      </div>

      {/* State View & Treatments List */}
      <StateView
        state={viewState}
        onRetry={loadData}
        emptyTitle={t('treatment.emptyState', 'No treatment plans recorded')}
        emptyMessage={t('treatment.emptyStateDesc', 'Active treatments will appear here once planned for reported crop problems.')}
        emptyIcon={ShieldCheck}
        emptyActionText={t('treatment.addButton', 'Plan Treatment')}
        onEmptyAction={() => setIsAddModalOpen(true)}
      >
        <div className="space-y-4">
          {filteredTreatments.map((treatment) => (
            <TreatmentCard key={treatment.id} treatment={treatment} onStatusChange={loadData} />
          ))}
        </div>
      </StateView>

      {/* Add Treatment Modal */}
      <AddTreatmentModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={loadData}
        diseaseIncidents={diseaseIncidents}
      />
    </div>
  );
};

export default TreatmentsPage;
