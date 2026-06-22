import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import StateView from '../../shared/components/StateView';
import { Sprout, ChevronRight } from 'lucide-react';

const CropsPage = () => {
  const { t } = useTranslation();
  const [viewState, setViewState] = useState('success');

  // Mock data for crops
  const mockCrops = [
    { id: 1, name: 'Rice (Ponni)', area: '2.5 Acres', status: 'Growing', season: 'Samba 2026' },
    { id: 2, name: 'Tomato (F1 Hybrid)', area: '1.0 Acre', status: 'Flowering', season: 'Kharif 2026' },
    { id: 3, name: 'Coconut (Tall x Dwarf)', area: '5.0 Acres', status: 'Harvesting', season: 'Perennial' }
  ];

  return (
    <div className="space-y-6">
      {/* Dev State Controller */}
      <div className="rounded-xl border border-dashed border-emerald-300 bg-emerald-50/50 p-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <span className="text-sm font-bold text-emerald-800 uppercase tracking-wider">
            {t('dashboard.stateSelector', 'Review States (Dev Mode)')}
          </span>
          <div className="flex flex-wrap gap-2">
            {['success', 'loading', 'empty', 'error', 'offline'].map((stateName) => (
              <button
                key={stateName}
                onClick={() => setViewState(stateName)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all min-h-[36px] uppercase ${
                  viewState === stateName
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-white text-gray-600 border border-gray-250 hover:bg-gray-50'
                }`}
              >
                {stateName}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="rounded-xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800">{t('nav.crops', 'My Crops')}</h1>
        <p className="text-base text-gray-500 mt-1">
          {t('states.emptyCrops', 'No seasons or crops recorded yet.')}
        </p>
      </div>

      <StateView
        state={viewState}
        onRetry={() => setViewState('success')}
        emptyMessage={t('states.emptyCrops', 'No crops planted yet.')}
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {mockCrops.map((crop) => (
            <div
              key={crop.id}
              className="rounded-2xl bg-white p-5 shadow-sm border border-gray-200 hover:border-emerald-300 hover:shadow-md transition-all cursor-pointer flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center space-x-3 mb-3">
                  <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-600">
                    <Sprout className="h-6 w-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-gray-850">{crop.name}</h3>
                    <span className="text-sm font-semibold text-gray-500">{crop.season}</span>
                  </div>
                </div>
                
                <div className="space-y-2 mt-4 border-t border-gray-100 pt-3">
                  <div className="flex justify-between text-base">
                    <span className="text-gray-500">Area:</span>
                    <span className="font-semibold text-gray-800">{crop.area}</span>
                  </div>
                  <div className="flex justify-between text-base">
                    <span className="text-gray-500">Status:</span>
                    <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-sm font-semibold text-emerald-700">
                      {crop.status}
                    </span>
                  </div>
                </div>
              </div>

              <div className="mt-5 flex items-center justify-end text-emerald-700 font-bold text-base h-12">
                <span>View details</span>
                <ChevronRight className="h-5 w-5 ml-1" />
              </div>
            </div>
          ))}
        </div>
      </StateView>
    </div>
  );
};

export default CropsPage;
