import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import StateView from '../../../shared/components/StateView';
import { Sprout } from 'lucide-react';
import CropCard from '../components/CropCard';
import { fetchCropsList } from '../api/getSeasonCrops';

const CropsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
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
      <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-black text-gray-800">{t('nav.crops', 'My Crops')}</h1>
        <p className="text-sm font-semibold text-gray-400 mt-1">
          {t('crops.page.subtitle', 'Cultivation inventory and daily operations overview')}
        </p>
      </div>

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
    </div>
  );
};

export default CropsPage;
