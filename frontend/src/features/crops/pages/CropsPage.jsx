import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import StateView from '../../../shared/components/StateView';
import { Sprout, BookOpen, Layers } from 'lucide-react';
import CropCard from '../components/CropCard';
import CatalogBrowser from '../components/CatalogBrowser';
import { fetchCropsList } from '../api/getSeasonCrops';

const CropsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('active'); // 'active' | 'catalog'
  const [crops, setCrops] = useState([]);
  const [viewState, setViewState] = useState('loading');

  const loadCropsData = async () => {
    setViewState('loading');
    try {
      const list = await fetchCropsList();
      setCrops(list);

      if (list.length === 0) {
        setViewState('empty');
      } else {
        setViewState('success');
      }
    } catch (err) {
      console.error('Failed to load crops:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    }
  };

  useEffect(() => {
    loadCropsData();
  }, []);

  return (
    <div className="space-y-6">
      {/* Title Header */}
      <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-800">{t('nav.crops', 'Crops & Cultivation')}</h1>
          <p className="text-sm font-semibold text-gray-400 mt-1">
            {t('crops.page.subtitle', 'Cultivation inventory and automated agricultural planning')}
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex bg-gray-100 p-1.5 rounded-2xl border border-gray-200 self-start sm:self-auto">
          <button
            onClick={() => setActiveTab('active')}
            className={`px-4 py-2 rounded-xl text-xs font-black transition-all flex items-center space-x-1.5 cursor-pointer ${
              activeTab === 'active'
                ? 'bg-white text-emerald-800 shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <Sprout className="h-4 w-4" />
            <span>{t('crops.tabs.active', 'My Crops')}</span>
            {crops.length > 0 && (
              <span className="ml-1 px-1.5 py-0.2 rounded-full bg-emerald-100 text-emerald-800 text-[10px]">
                {crops.length}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab('catalog')}
            className={`px-4 py-2 rounded-xl text-xs font-black transition-all flex items-center space-x-1.5 cursor-pointer ${
              activeTab === 'catalog'
                ? 'bg-white text-emerald-800 shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <BookOpen className="h-4 w-4" />
            <span>{t('crops.tabs.catalog', 'Supported Crops')}</span>
          </button>
        </div>
      </div>

      {activeTab === 'active' ? (
        <StateView
          state={viewState}
          onRetry={loadCropsData}
          emptyTitle={t('crops.empty.title', 'No Crops Planted')}
          emptyMessage={t('crops.empty.desc', 'Record your first crop planting to start managing daily tasks, tracking health, and scheduling harvests.')}
          emptyActionText={t('crops.empty.action', 'Plant a Crop')}
          onEmptyAction={() => navigate('/farms')}
          emptyIcon={Sprout}
        >
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {crops.map((crop) => (
              <CropCard key={crop.id} crop={crop} />
            ))}
          </div>
        </StateView>
      ) : (
        <CatalogBrowser />
      )}
    </div>
  );
};

export default CropsPage;
