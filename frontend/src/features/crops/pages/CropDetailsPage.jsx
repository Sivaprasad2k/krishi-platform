import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Edit2, Calendar, AlertTriangle, Compass, CheckSquare, Sprout, HeartPulse, Plus } from 'lucide-react';
import { fetchCropWorkspaceDetails } from '../api/getCrop';
import CropTimeline from '../components/CropTimeline';
import StateView from '../../../shared/components/StateView';
import ProblemCard from '../../diseaseIncident/components/ProblemCard';
import ReportProblemModal from '../../diseaseIncident/components/ReportProblemModal';
import TreatmentCard from '../../treatment/components/TreatmentCard';
import AddTreatmentModal from '../../treatment/components/AddTreatmentModal';
import TaskCard from '../../tasks/components/TaskCard';
import CultivationPlanCard from '../components/CultivationPlanCard';

export const CropDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [data, setData] = useState(null);
  const [viewState, setViewState] = useState('loading');
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isAddTreatmentModalOpen, setIsAddTreatmentModalOpen] = useState(false);

  const loadWorkspaceDetails = async () => {
    setViewState('loading');
    try {
      const details = await fetchCropWorkspaceDetails(id);
      setData(details);
      setViewState('success');
    } catch (err) {
      console.error('Failed to load crop workspace details:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    }
  };

  useEffect(() => {
    loadWorkspaceDetails();
  }, [id]);

  const getCropIcon = (name) => {
    if (!name) return '🌿';
    const clean = name.toLowerCase();
    if (clean.includes('paddy') || clean.includes('rice')) return '🌾';
    if (clean.includes('banana')) return '🌱';
    if (clean.includes('coconut')) return '🥥';
    if (clean.includes('tomato')) return '🍅';
    return '🌿';
  };

  const getSeverityBadge = (severity) => {
    const s = severity?.toUpperCase();
    if (s === 'HIGH' || s === 'CRITICAL') {
      return 'bg-red-50 text-red-750 border-red-100';
    }
    if (s === 'MEDIUM') {
      return 'bg-amber-50 text-amber-700 border-amber-100';
    }
    return 'bg-blue-50 text-blue-700 border-blue-100';
  };

  if (viewState === 'loading' || viewState === 'error' || viewState === 'offline') {
    return (
      <div className="max-w-3xl mx-auto p-4">
        <StateView state={viewState} onRetry={loadWorkspaceDetails} />
      </div>
    );
  }

  const { crop, season, farmName, openIncidents = [], incidents = openIncidents, treatments = [], tasks = [] } = data;
  const isEditable = season && season.status !== 'COMPLETED' && season.status !== 'CANCELLED';

  const pendingTasks = tasks.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS' || t.status === 'OVERDUE');

  return (
    <div className="space-y-6 max-w-3xl mx-auto pb-10">
      {/* 0. WORKSPACE HEADER CARD */}
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <button
            onClick={() => navigate('/crops')}
            className="p-2.5 rounded-xl border border-[#C9DCCB] hover:bg-[#E3F2E6] text-[#222222] transition-all cursor-pointer min-h-[44px] min-w-[44px] flex items-center justify-center"
            aria-label={t('common.actions.back', 'Back')}
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex items-center space-x-3">
            <span className="text-3xl md:text-4xl p-1.5 bg-[#E3F2E6] rounded-xl shrink-0" role="img" aria-label="Crop icon">
              {getCropIcon(crop.cropName)}
            </span>
            <div>
              <h1 className="text-lg md:text-xl font-black text-[#222222]">{crop.cropName}</h1>
              <p className="text-xs font-semibold text-[#525252] mt-0.5">
                {crop.cropVariety} &bull; {farmName}
              </p>
            </div>
          </div>
        </div>

        {isEditable && (
          <button
            onClick={() => navigate(`/crops/${crop.id}/edit`)}
            style={{ minHeight: '44px' }}
            className="flex items-center space-x-1.5 px-4 py-2 rounded-xl border border-[#C9DCCB] text-sm font-black text-[#222222] hover:bg-[#E3F2E6] active:scale-95 transition-all shadow-2xs cursor-pointer"
          >
            <Edit2 className="h-4 w-4" />
            <span className="hidden sm:inline">{t('crops.details.edit', 'Edit Crop')}</span>
          </button>
        )}
      </div>

      {/* 1. TODAY'S WORK */}
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
        <div className="flex items-center space-x-2 pb-3 border-b border-[#C9DCCB]">
          <CheckSquare className="h-5 w-5 text-[#2563EB]" />
          <h2 className="text-lg font-black text-[#222222]">{t('crops.details.todaysWork', "Today's Work")}</h2>
        </div>
        
        {pendingTasks.length > 0 ? (
          <div className="space-y-3">
            {pendingTasks.map((task) => (
              <TaskCard
                key={task.id}
                task={{ ...task, cropName: crop.cropName, cropVariety: crop.cropVariety, farmName }}
                onStatusChange={loadWorkspaceDetails}
                showTreatmentContext={false}
              />
            ))}
          </div>
        ) : (
          <div className="p-4 bg-[#F0F7F1] rounded-xl border border-[#C9DCCB] text-center flex flex-col items-center space-y-2">
            <HeartPulse className="h-7 w-7 text-[#16A34A]" />
            <span className="text-sm font-extrabold text-[#16A34A]">
              {t('crops.details.noTasks', 'Your field work is up to date!')}
            </span>
          </div>
        )}
      </div>

      {/* 2. CULTIVATION PLAN */}
      <CultivationPlanCard
        plan={data.cultivationPlan}
        plantingId={crop.id}
        cropName={crop.cropName}
        onPlanUpdated={loadWorkspaceDetails}
      />

      {/* 2. CURRENT PROBLEMS */}
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="h-5 w-5 text-[#DC2626]" />
            <h2 className="text-lg font-black text-[#222222]">{t('crops.details.problems', 'Current Problems')}</h2>
          </div>
          <button
            onClick={() => setIsReportModalOpen(true)}
            style={{ minHeight: '44px' }}
            className="flex items-center space-x-1 px-4 py-2 rounded-xl bg-[#DC2626] hover:bg-[#B91C1C] text-white text-xs font-black transition-all shadow-2xs active:scale-95 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>{t('diseaseIncident.reportButton', 'Report Problem')}</span>
          </button>
        </div>

        {openIncidents.length > 0 ? (
          <div className="space-y-3">
            {openIncidents.map((inc) => (
              <ProblemCard
                key={inc.id}
                incident={{ ...inc, cropName: crop.cropName, cropVariety: crop.cropVariety, farmName }}
                onStatusChange={loadWorkspaceDetails}
                showCropContext={false}
              />
            ))}
          </div>
        ) : (
          <div className="p-4 bg-[#F0F7F1] rounded-xl border border-[#C9DCCB] text-center flex flex-col items-center space-y-2">
            <Sprout className="h-7 w-7 text-[#16A34A]" />
            <span className="text-sm font-extrabold text-[#16A34A]">
              {t('crops.details.noProblems', 'No crop health problems reported!')}
            </span>
          </div>
        )}
      </div>

      <ReportProblemModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSuccess={loadWorkspaceDetails}
        preselectedPlantingId={crop.id}
      />

      {/* 3. TREATMENTS */}
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Compass className="h-5 w-5 text-[#D97706]" />
            <h2 className="text-lg font-black text-[#222222]">{t('crops.details.treatments', 'Treatments')}</h2>
          </div>
          {openIncidents.length > 0 && (
            <button
              onClick={() => setIsAddTreatmentModalOpen(true)}
              style={{ minHeight: '44px' }}
              className="flex items-center space-x-1 px-4 py-2 rounded-xl bg-white border border-[#C9DCCB] text-[#2563EB] hover:bg-blue-50 text-xs font-black transition-all cursor-pointer shadow-2xs"
            >
              <Plus className="h-3.5 w-3.5" />
              <span>{t('treatment.addButton', 'Plan Treatment')}</span>
            </button>
          )}
        </div>

        {treatments.length > 0 ? (
          <div className="space-y-3">
            {treatments.map((treat) => (
              <TreatmentCard
                key={treat.id}
                treatment={{ ...treat, cropName: crop.cropName, cropVariety: crop.cropVariety, farmName }}
                onStatusChange={loadWorkspaceDetails}
                showProblemContext={false}
              />
            ))}
          </div>
        ) : (
          <div className="text-center p-4 text-[#6B7280] font-semibold text-sm bg-[#F0F7F1] rounded-xl border border-[#C9DCCB]">
            {t('crops.details.noTreatments', 'No treatments applied yet.')}
          </div>
        )}
      </div>

      <AddTreatmentModal
        isOpen={isAddTreatmentModalOpen}
        onClose={() => setIsAddTreatmentModalOpen(false)}
        onSuccess={loadWorkspaceDetails}
        diseaseIncidents={openIncidents.map(inc => ({ ...inc, cropName: crop.cropName, farmName }))}
      />

      {/* 4. CROP INFORMATION */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center space-x-2 pb-3 border-b border-gray-100">
          <Sprout className="h-5 w-5 text-emerald-600" />
          <h2 className="text-lg font-black text-gray-800">{t('crops.details.info', 'Crop Information')}</h2>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 text-sm font-bold text-gray-600">
          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.cropType', 'Crop Type')}</span>
            <span className="text-gray-800 font-black">{crop.cropName}</span>
          </div>
          
          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.variety', 'Crop Variety')}</span>
            <span className="text-gray-800 font-black">{crop.cropVariety || 'Standard'}</span>
          </div>

          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.datePlanted', 'Date Planted')}</span>
            <span className="text-gray-800 font-black">
              {crop.plantingDate ? new Date(crop.plantingDate).toLocaleDateString() : 'N/A'}
            </span>
          </div>

          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.expectedHarvest', 'Expected Harvest')}</span>
            <span className="text-gray-800 font-black">
              {crop.expectedHarvestDate ? new Date(crop.expectedHarvestDate).toLocaleDateString() : t('crops.details.none', 'Not specified')}
            </span>
          </div>

          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.season', 'Season')}</span>
            <span className="text-emerald-850 font-black">{season?.name || 'N/A'}</span>
          </div>

          <div className="p-3 bg-gray-50/50 border border-gray-150 rounded-xl space-y-1">
            <span className="text-xs text-gray-400 block">{t('crops.details.farm', 'Farm')}</span>
            <span className="text-gray-800 font-black">{farmName}</span>
          </div>
        </div>
      </div>

      {/* 5. TIMELINE */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center space-x-2 pb-3 border-b border-gray-100">
          <Calendar className="h-5 w-5 text-gray-600" />
          <h2 className="text-lg font-black text-gray-800">{t('crops.details.timeline', 'Timeline')}</h2>
        </div>

        <CropTimeline crop={crop} incidents={incidents} treatments={treatments} tasks={tasks} />
      </div>
    </div>
  );
};

export default CropDetailsPage;
