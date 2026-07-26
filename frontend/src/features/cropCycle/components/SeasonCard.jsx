import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ChevronRight, Calendar, Sprout } from 'lucide-react';
import SeasonStatusBadge from './SeasonStatusBadge';

/**
 * SeasonCard displays a summary of a Crop Season.
 * @param {Object} props.season The Season object
 */
const SeasonCard = ({ season }) => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const formattedDate = season?.createdAt
    ? new Date(season.createdAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    : '';

  return (
    <div
      onClick={() => season?.id && navigate(`/seasons/${season.id}`)}
      className="group relative rounded-2xl bg-white p-5 shadow-sm border border-gray-200 hover:border-emerald-350 hover:shadow-md transition-all cursor-pointer flex flex-col justify-between"
    >
      <div>
        <div className="flex items-start justify-between mb-4">
          <h3 className="text-lg font-bold text-gray-850 group-hover:text-emerald-800 transition-colors truncate pr-2">
            {season?.name || ''}
          </h3>
          <SeasonStatusBadge status={season?.status} />
        </div>

        <div className="space-y-2.5 border-t border-gray-100 pt-3">
          <div className="flex items-center text-sm font-semibold text-gray-500">
            <Sprout className="h-4.5 w-4.5 text-gray-400 mr-2 shrink-0" />
            <span className="text-gray-400 mr-1.5 uppercase text-xs tracking-wider font-bold">
              {t('season.seasonType', 'Type')}:
            </span>
            <span className="truncate text-gray-750 font-bold">
              {t(`season.types.${season?.seasonType}`, season?.seasonType)} ({season?.year})
            </span>
          </div>

          <div className="flex items-center text-sm font-semibold text-gray-500">
            <Calendar className="h-4.5 w-4.5 text-gray-400 mr-2 shrink-0" />
            <span className="text-gray-400 mr-1.5 uppercase text-xs tracking-wider font-bold">
              {t('season.createdAt', 'Created Date')}:
            </span>
            <span className="text-gray-750 font-bold">{formattedDate || 'N/A'}</span>
          </div>
        </div>
      </div>

      <div className="mt-5 flex items-center justify-end text-emerald-700 font-bold text-sm h-10 border-t border-gray-55 pt-3">
        <span className="group-hover:underline">{t('farm.actions.viewDetails', 'View details')}</span>
        <ChevronRight className="h-4 w-4 ml-1 transition-transform group-hover:translate-x-0.5" />
      </div>
    </div>
  );
};

export default SeasonCard;
