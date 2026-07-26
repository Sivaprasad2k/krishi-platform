import { useTranslation } from 'react-i18next';
import { Sprout, Plus } from 'lucide-react';

/**
 * SeasonEmptyState displays when there are no seasons in the farm list.
 * @param {Function} props.onStart Callback when Start Season button is clicked
 */
const SeasonEmptyState = ({ onStart }) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center justify-center p-8 rounded-2xl border-2 border-dashed border-gray-250 bg-gray-50/30 text-center space-y-4 max-w-md mx-auto">
      <div className="rounded-2xl bg-emerald-50 p-4 text-emerald-600 shadow-inner">
        <Sprout className="h-10 w-10" />
      </div>
      <div className="space-y-1">
        <h3 className="text-lg font-bold text-gray-800">
          {t('season.currentSeasons', 'Current Seasons')}
        </h3>
        <p className="text-sm font-semibold text-gray-400">
          {t('season.emptyState', 'Start your first crop season')}
        </p>
      </div>
      <button
        onClick={onStart}
        style={{ minHeight: '48px' }}
        className="flex items-center justify-center space-x-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white font-bold px-6 shadow-sm cursor-pointer transition-all"
      >
        <Plus className="h-5 w-5" />
        <span>{t('season.create', 'Start Season')}</span>
      </button>
    </div>
  );
};

export default SeasonEmptyState;
