import { CheckCircle, AlertTriangle, Clock } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export const CropStatusBadge = ({ type }) => {
  const { t } = useTranslation();

  const config = {
    healthy: {
      bg: 'bg-emerald-50 border-emerald-100 text-emerald-700',
      icon: CheckCircle,
      text: t('crops.status.healthy', 'Healthy')
    },
    attention: {
      bg: 'bg-amber-50 border-amber-100 text-amber-700',
      icon: Clock,
      text: t('crops.status.attention', 'Attention')
    },
    problem: {
      bg: 'bg-red-50 border-red-100 text-red-750',
      icon: AlertTriangle,
      text: t('crops.status.problem', 'Problem')
    },
    completed: {
      bg: 'bg-gray-50 border-gray-200 text-gray-600',
      icon: CheckCircle,
      text: t('crops.status.completed', 'Harvested')
    }
  };

  const current = config[type] || config.healthy;
  const Icon = current.icon;

  return (
    <span className={`inline-flex items-center space-x-1.5 rounded-full border px-2.5 py-0.5 text-xs font-black ${current.bg}`}>
      <Icon className="h-3.5 w-3.5 shrink-0" />
      <span>{current.text}</span>
    </span>
  );
};

export default CropStatusBadge;
