import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  ShieldAlert,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  Calendar,
  CheckSquare,
  Compass,
  Clock,
  Sparkles,
} from 'lucide-react';

/**
 * AgriculturalAdvisoryCard
 * Renders an explainable, deterministic agricultural advisory with severity styling,
 * prominent recommended farmer action, linked task context, and lifecycle controls.
 */
export default function AgriculturalAdvisoryCard({
  advisory,
  onAcknowledge,
  onResolveClick,
  isActionLoading = false,
}) {
  const { t } = useTranslation();

  const isCriticalOrHigh = advisory.severity === 'CRITICAL' || advisory.severity === 'HIGH';
  const isMedium = advisory.severity === 'MEDIUM';

  const getSeverityStyle = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return {
          container: 'border-l-4 border-l-red-600 bg-red-50/30 border-red-200',
          badge: 'bg-red-100 text-red-800 border-red-200',
          icon: <ShieldAlert className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />,
        };
      case 'HIGH':
        return {
          container: 'border-l-4 border-l-rose-500 bg-rose-50/30 border-rose-200',
          badge: 'bg-rose-100 text-rose-800 border-rose-200',
          icon: <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />,
        };
      case 'MEDIUM':
        return {
          container: 'border-l-4 border-l-amber-500 bg-amber-50/30 border-amber-200',
          badge: 'bg-amber-100 text-amber-800 border-amber-200',
          icon: <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />,
        };
      default: // LOW
        return {
          container: 'border-l-4 border-l-blue-500 bg-blue-50/30 border-blue-200',
          badge: 'bg-blue-100 text-blue-800 border-blue-200',
          icon: <Compass className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />,
        };
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-amber-100 text-amber-800 border border-amber-200">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse" />
            <span>{t('advisories.status.ACTIVE', 'Active')}</span>
          </span>
        );
      case 'ACKNOWLEDGED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-blue-100 text-blue-800 border border-blue-200">
            <CheckCircle2 className="w-3 h-3 text-blue-600" />
            <span>{t('advisories.status.ACKNOWLEDGED', 'Acknowledged')}</span>
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>{t('advisories.status.RESOLVED', 'Resolved')}</span>
          </span>
        );
      case 'SUPERSEDED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-gray-100 text-gray-700 border border-gray-200">
            <span>{t('advisories.status.SUPERSEDED', 'Superseded')}</span>
          </span>
        );
      case 'EXPIRED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-gray-100 text-gray-600 border border-gray-200">
            <Clock className="w-3 h-3 text-gray-500" />
            <span>{t('advisories.status.EXPIRED', 'Expired')}</span>
          </span>
        );
      default:
        return null;
    }
  };

  const style = getSeverityStyle(advisory.severity);

  const formatDate = (dateStr) => {
    if (!dateStr) return null;
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  return (
    <div
      className={`p-4 md:p-5 rounded-2xl border transition-all shadow-2xs space-y-3.5 bg-white ${style.container}`}
    >
      {/* Header: Title, Severity, and Status */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start space-x-2.5">
          {style.icon}
          <div>
            <div className="flex items-center space-x-2 flex-wrap gap-y-1">
              <h4 className="text-sm md:text-base font-black text-[#222222]">{advisory.title}</h4>
              <span
                className={`text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full border ${style.badge}`}
              >
                {`${advisory.severity} Severity`}
              </span>
              {getStatusBadge(advisory.status)}
            </div>

            {/* Target Date Chip */}
            {advisory.targetDate && (
              <p className="text-[11px] text-gray-500 flex items-center space-x-1 mt-0.5">
                <Calendar className="w-3 h-3 text-gray-400" />
                <span>
                  {t('advisories.card.targetDate', 'Applicable Date')}:{' '}
                  <strong className="text-gray-700">{formatDate(advisory.targetDate)}</strong>
                </span>
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Advisory Explanation Message */}
      <p className="text-xs md:text-sm text-gray-700 leading-relaxed">{advisory.message}</p>

      {/* Recommended Action Highlight Box */}
      {advisory.recommendedAction && (
        <div className="p-3 bg-gradient-to-r from-emerald-50/80 to-green-50/40 rounded-xl border border-emerald-200 text-xs space-y-1">
          <div className="flex items-center space-x-1.5 text-emerald-800 font-extrabold uppercase text-[10px] tracking-wide">
            <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
            <span>{t('advisories.card.recommendedActionHeader', 'Recommended Operational Action')}</span>
          </div>
          <p className="text-gray-800 font-bold leading-normal">{advisory.recommendedAction}</p>
        </div>
      )}

      {/* Context Chips (Linked Task, Provenance) */}
      <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px] text-gray-600">
        {advisory.taskTitle && (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-gray-100 border border-gray-200 font-semibold text-gray-700">
            <CheckSquare className="w-3 h-3 text-blue-600" />
            <span>
              {t('advisories.card.linkedTask', 'Linked Task')}: <strong>{advisory.taskTitle}</strong>
            </span>
          </span>
        )}

        {advisory.provenance && (
          <span
            className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-gray-50 border border-gray-200 text-gray-500 text-[10px]"
            title={advisory.provenance}
          >
            <span>{t('advisories.card.basis', 'Basis')}:</span>
            <span className="truncate max-w-[240px] md:max-w-none">{advisory.provenance}</span>
          </span>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex items-center justify-between pt-2 border-t border-gray-150">
        <div className="text-[11px] text-gray-400">
          {advisory.lastEvaluatedAt && (
            <span>
              {t('advisories.card.lastEvaluated', 'Evaluated')}:{' '}
              {new Date(advisory.lastEvaluatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>

        <div className="flex items-center space-x-2">
          {advisory.status === 'ACTIVE' && onAcknowledge && (
            <button
              type="button"
              onClick={() => onAcknowledge(advisory.id)}
              disabled={isActionLoading}
              className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50 text-xs font-black transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <CheckCircle2 className="w-3.5 h-3.5 text-blue-600" />
              <span>{t('advisories.actions.acknowledge', 'Acknowledge')}</span>
            </button>
          )}

          {(advisory.status === 'ACTIVE' || advisory.status === 'ACKNOWLEDGED') && onResolveClick && (
            <button
              type="button"
              onClick={() => onResolveClick(advisory)}
              disabled={isActionLoading}
              className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-xl bg-[#16A34A] hover:bg-[#15803D] text-white text-xs font-black shadow-2xs transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{t('advisories.actions.resolve', 'Resolve')}</span>
            </button>
          )}

          {advisory.status === 'RESOLVED' && advisory.resolvedAt && (
            <span className="text-[11px] font-semibold text-emerald-700 flex items-center space-x-1">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>{t('advisories.card.resolvedOn', 'Resolved {{date}}', { date: formatDate(advisory.resolvedAt) })}</span>
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
