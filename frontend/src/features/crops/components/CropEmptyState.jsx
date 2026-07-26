import React from 'react';
import { useTranslation } from 'react-i18next';
import { Sprout } from 'lucide-react';

export const CropEmptyState = ({ onAction, showAction = true }) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center rounded-2xl bg-white border border-gray-200 shadow-sm space-y-4">
      <div className="rounded-2xl bg-emerald-50 p-4 text-emerald-600 animate-pulse">
        <Sprout className="h-10 w-10" />
      </div>
      <div className="space-y-1">
        <h3 className="text-lg font-black text-gray-800">
          {t('crops.empty.title', 'No Crops Planted')}
        </h3>
        <p className="text-sm font-semibold text-gray-400 max-w-sm">
          {t('crops.empty.desc', 'Record your first crop planting to start managing daily tasks, tracking health, and scheduling harvests.')}
        </p>
      </div>
      {showAction && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-black text-white hover:bg-emerald-700 shadow-sm active:scale-95 transition-all"
        >
          {t('crops.empty.action', 'Plant a Crop')}
        </button>
      )}
    </div>
  );
};

export default CropEmptyState;
