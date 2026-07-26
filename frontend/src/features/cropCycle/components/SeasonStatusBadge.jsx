import { useTranslation } from 'react-i18next';
import { Calendar, Sprout, Wheat, Octagon } from 'lucide-react';

/**
 * SeasonStatusBadge renders a localized badge for a Season's status.
 * @param {string} props.status DRAFT, ACTIVE, COMPLETED, CANCELLED
 */
const SeasonStatusBadge = ({ status }) => {
  const { t } = useTranslation();
  const normalizedStatus = status ? status.toUpperCase() : 'DRAFT';

  let bgClass = 'bg-gray-50 text-gray-600 border-gray-200';
  let Icon = Calendar;
  let labelKey = 'season.statusMap.draft';
  let defaultLabel = 'Planned';

  if (normalizedStatus === 'ACTIVE') {
    bgClass = 'bg-emerald-50 text-emerald-700 border-emerald-100';
    Icon = Sprout;
    labelKey = 'season.statusMap.active';
    defaultLabel = 'Growing';
  } else if (normalizedStatus === 'COMPLETED') {
    bgClass = 'bg-emerald-55 text-emerald-750 border-emerald-150'; // Completed is also Green (Healthy/Completed)
    Icon = Wheat;
    labelKey = 'season.statusMap.completed';
    defaultLabel = 'Harvested';
  } else if (normalizedStatus === 'CANCELLED') {
    bgClass = 'bg-red-50 text-red-700 border-red-100';
    Icon = Octagon;
    labelKey = 'season.statusMap.cancelled';
    defaultLabel = 'Stopped';
  }

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold border ${bgClass}`}
    >
      <Icon className="h-3.5 w-3.5 mr-1 shrink-0" />
      <span>{t(labelKey, defaultLabel)}</span>
    </span>
  );
};

export default SeasonStatusBadge;
