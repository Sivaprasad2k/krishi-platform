import React from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle2, Clock, AlertTriangle, XCircle, AlertCircle, Sparkles } from 'lucide-react';

const STATUS_CONFIGS = {
  // Task / Work statuses
  PENDING: {
    bg: 'bg-[#FFFBEB] text-[#D97706] border-amber-200',
    icon: Clock,
    translationKey: 'tasks.status.PENDING',
    defaultText: 'Pending'
  },
  IN_PROGRESS: {
    bg: 'bg-[#EFF6FF] text-[#2563EB] border-blue-200',
    icon: Clock,
    translationKey: 'tasks.status.IN_PROGRESS',
    defaultText: 'In Progress'
  },
  COMPLETED: {
    bg: 'bg-[#F0F7F1] text-[#16A34A] border-[#C9DCCB]',
    icon: CheckCircle2,
    translationKey: 'tasks.status.COMPLETED',
    defaultText: 'Completed'
  },
  CANCELLED: {
    bg: 'bg-stone-100 text-stone-700 border-stone-250',
    icon: XCircle,
    translationKey: 'tasks.status.CANCELLED',
    defaultText: 'Cancelled'
  },
  SKIPPED: {
    bg: 'bg-purple-50 text-purple-700 border-purple-200',
    icon: XCircle,
    translationKey: 'tasks.status.SKIPPED',
    defaultText: 'Skipped'
  },
  OVERDUE: {
    bg: 'bg-[#FEF2F2] text-[#DC2626] border-red-200 animate-pulse',
    icon: AlertTriangle,
    translationKey: 'tasks.status.OVERDUE',
    defaultText: 'Overdue'
  },

  // Health statuses
  HEALTHY: {
    bg: 'bg-[#F0F7F1] text-[#16A34A] border-[#C9DCCB]',
    icon: Sparkles,
    translationKey: 'crops.status.healthy',
    defaultText: 'Healthy'
  },
  ATTENTION: {
    bg: 'bg-[#FFFBEB] text-[#D97706] border-amber-200',
    icon: AlertCircle,
    translationKey: 'crops.status.attention',
    defaultText: 'Attention Needed'
  },
  PROBLEM: {
    bg: 'bg-[#FEF2F2] text-[#DC2626] border-red-200',
    icon: AlertTriangle,
    translationKey: 'crops.status.problem',
    defaultText: 'Problem Reported'
  },

  // Season & Farm statuses
  ACTIVE: {
    bg: 'bg-[#F0F7F1] text-[#16A34A] border-[#C9DCCB]',
    icon: Sparkles,
    translationKey: 'season.statusMap.active',
    defaultText: 'Active'
  },
  DRAFT: {
    bg: 'bg-stone-100 text-stone-700 border-stone-250',
    icon: Clock,
    translationKey: 'season.statusMap.draft',
    defaultText: 'Planned'
  }
};

/**
 * StatusBadge presents standardized semantic visual indicators for domain statuses.
 */
export const StatusBadge = ({ status, customLabel = null, className = '' }) => {
  const { t } = useTranslation();
  const normalizedStatus = status ? status.toString().toUpperCase() : 'PENDING';
  const config = STATUS_CONFIGS[normalizedStatus] || STATUS_CONFIGS.PENDING;
  const Icon = config.icon;

  const label = customLabel || t(config.translationKey, config.defaultText);

  return (
    <span className={`inline-flex items-center space-x-1.5 px-3 py-1 rounded-full border text-xs font-black shrink-0 ${config.bg} ${className}`}>
      <Icon className="h-3.5 w-3.5 shrink-0" />
      <span className="truncate max-w-[120px] sm:max-w-none">{label}</span>
    </span>
  );
};

export default StatusBadge;
