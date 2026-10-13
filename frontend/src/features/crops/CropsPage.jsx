import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import StateView from '../../shared/components/StateView';
import { Sprout, ChevronRight, HelpCircle, Layers, AlertTriangle, CheckSquare, Compass, Filter, X } from 'lucide-react';
import { fetchFarmerCropsWithWorkflows } from '../dashboard/api/dashboardApi';

/**
 * CropsPage displays all crop plantings in the farmer's cycles,
 * detailing the active workflow pipeline (Farm -> Crop -> Disease -> Treatment -> Task).
 */
const CropsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const farmFilterId = searchParams.get('farmId');

  const [crops, setCrops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewState, setViewState] = useState('loading');

  const loadCropsData = async () => {
    setLoading(true);
    setError(null);
    setViewState('loading');

    try {
      const list = await fetchFarmerCropsWithWorkflows();
      setCrops(list);

      if (list.length === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load crops workflows:', err);
      setError(err.message || 'Failed to retrieve crops');
      setViewState(navigator.onLine ? 'error' : 'offline');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCropsData();
  }, []);

  const getLocalizedCropName = (name) => {
    if (!name) return '';
    const clean = name.toLowerCase();
    if (clean.includes('tomato')) return t('workflow.names.tomato', 'Tomato');
    if (clean.includes('rice') || clean.includes('ponni')) return t('workflow.names.rice', 'Rice (Ponni)');
    if (clean.includes('coconut')) return t('workflow.names.coconut', 'Coconut');
    return name;
  };

  const getLocalizedStatus = (status) => {
    if (!status) return '';
    const clean = status.toLowerCase();
    if (clean === 'growing') return t('workflow.status.growing', 'Growing');
    if (clean === 'flowering') return t('workflow.status.flowering', 'Flowering');
    if (clean === 'harvesting') return t('workflow.status.harvesting', 'Harvesting');
    if (clean === 'perennial') return t('workflow.status.perennial', 'Perennial');
    if (clean === 'active') return t('workflow.status.active', 'Active');
    if (clean === 'completed') return t('workflow.status.completed', 'Completed');
    return status;
  };

  const displayedCrops = farmFilterId ? crops.filter((c) => c.farmId === farmFilterId) : crops;

  return (
    <div className="space-y-6">
      {/* Title Header */}
      <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-black text-gray-800">{t('nav.crops', 'My Crops')}</h1>
        <p className="text-sm font-semibold text-gray-400 mt-1">
          {t('dashboard.status', 'Farm Operations Overview')}
        </p>
      </div>

      {/* Farm filter banner */}
      {farmFilterId && (
        <div className="flex items-center justify-between rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-2.5 text-xs sm:text-sm font-bold text-emerald-800 animate-fade-in">
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>
              {t('crops.filteredByFarm', 'Filtering crops by farm')}
              {displayedCrops[0]?.farmName ? `: ${displayedCrops[0].farmName}` : ''}
            </span>
          </div>
          <button
            onClick={() => {
              searchParams.delete('farmId');
              setSearchParams(searchParams);
            }}
            className="flex items-center space-x-1 text-emerald-700 hover:text-emerald-900 font-extrabold hover:underline cursor-pointer"
          >
            <X className="h-4 w-4" />
            <span>{t('common.clearFilter', 'Clear Filter')}</span>
          </button>
        </div>
      )}

      <StateView
        state={viewState}
        onRetry={loadCropsData}
        emptyTitle={t('states.noCropsTitle', 'No Crops Found')}
        emptyMessage={t('states.noCropsDesc', 'No crops added yet.')}
        emptyActionText={t('dashboard.actions.addCrop', 'Add Crop')}
        onEmptyAction={() => navigate('/farms')}
        emptyIcon={Sprout}
      >
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {displayedCrops.map((crop) => (
            <div
              key={crop.id}
              className="rounded-2xl bg-white p-5 border border-gray-200 shadow-sm flex flex-col justify-between hover:border-emerald-350 transition-all hover:shadow-md"
            >
              <div className="space-y-4">
                {/* Crop Name & Season Header */}
                <div className="flex items-center space-x-3 pb-3 border-b border-gray-100">
                  <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-600">
                    <Sprout className="h-6 w-6" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="text-lg font-black text-gray-850 truncate">
                      {getLocalizedCropName(crop.plantingName)}
                    </h3>
                    <span className="text-xs font-bold text-gray-400 block truncate">
                      {crop.seasonName}
                    </span>
                  </div>
                </div>

                {/* Crop Details summary */}
                <div className="grid grid-cols-2 gap-2 text-sm bg-gray-50/50 p-2.5 rounded-xl border border-gray-100">
                  <div>
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Farm</span>
                    <span className="font-extrabold text-gray-700 truncate block">{crop.farmName}</span>
                  </div>
                  <div>
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Area</span>
                    <span className="font-extrabold text-gray-700 block">{crop.sizeHectares} Ha</span>
                  </div>
                </div>

                {/* WORKFLOW PIPELINE: Farm -> Crop -> Disease -> Treatment -> Task */}
                <div className="space-y-2.5 pt-2">
                  <h4 className="text-xs font-black text-gray-400 uppercase tracking-wider">
                    {t('dashboard.timeline.title', 'Operational Workflow')}
                  </h4>
                  
                  <div className="space-y-2">
                    {/* 1. Disease indicator */}
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center space-x-2 font-bold text-gray-500">
                        <AlertTriangle className="h-4.5 w-4.5 shrink-0" />
                        <span>{t('workflow.disease', 'Disease')}:</span>
                      </div>
                      <span className={`font-extrabold ${crop.workflow.disease ? 'text-red-650' : 'text-gray-400'}`}>
                        {crop.workflow.disease || t('workflow.none', 'None')}
                      </span>
                    </div>

                    {/* 2. Treatment indicator */}
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center space-x-2 font-bold text-gray-500">
                        <Compass className="h-4.5 w-4.5 shrink-0" />
                        <span>{t('workflow.treatment', 'Treatment')}:</span>
                      </div>
                      <span className={`font-extrabold ${crop.workflow.treatment ? 'text-amber-600' : 'text-gray-400'}`}>
                        {crop.workflow.treatment || t('workflow.none', 'None')}
                      </span>
                    </div>

                    {/* 3. Task indicator */}
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center space-x-2 font-bold text-gray-500">
                        <CheckSquare className="h-4.5 w-4.5 shrink-0" />
                        <span>{t('workflow.task', 'Task')}:</span>
                      </div>
                      <span className={`font-extrabold ${crop.workflow.task ? 'text-emerald-700' : 'text-gray-400'}`}>
                        {crop.workflow.task || t('workflow.none', 'None')}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Status Badge & details CTA */}
              <div className="mt-5 pt-3.5 border-t border-gray-100 flex items-center justify-between">
                <span className="inline-flex items-center rounded-full bg-emerald-50 border border-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-700">
                  {getLocalizedStatus(crop.status)}
                </span>
                
                <button
                  type="button"
                  onClick={() => navigate(`/crops/${crop.id}`)}
                  className="flex items-center text-emerald-700 font-extrabold text-sm hover:underline cursor-pointer bg-transparent border-0 p-0"
                >
                  <span>{t('farm.actions.viewDetails', 'View details')}</span>
                  <ChevronRight className="h-4 w-4 ml-0.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </StateView>
    </div>
  );
};

export default CropsPage;
