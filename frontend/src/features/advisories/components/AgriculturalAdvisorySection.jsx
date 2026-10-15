import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ShieldAlert,
  RefreshCw,
  Filter,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Sparkles,
} from 'lucide-react';
import {
  evaluateAdvisories,
  fetchAdvisories,
  acknowledgeAdvisory,
  resolveAdvisory,
} from '../api/advisoryApi';
import AgriculturalAdvisoryCard from './AgriculturalAdvisoryCard';
import ResolveAdvisoryModal from './ResolveAdvisoryModal';

/**
 * AgriculturalAdvisorySection
 * Manages the evaluation, listing, filtering, and lifecycle actions of agricultural advisories
 * for a specific crop planting workspace.
 */
export default function AgriculturalAdvisorySection({ plantingId, onAdvisoryUpdated }) {
  const { t } = useTranslation();

  const [advisories, setAdvisories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [evaluating, setEvaluating] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('ACTIVE'); // default to ACTIVE for operational focus
  const [selectedAdvisoryForResolve, setSelectedAdvisoryForResolve] = useState(null);
  const [isResolving, setIsResolving] = useState(false);
  const [lastEvaluationSummary, setLastEvaluationSummary] = useState(null);

  const loadAdvisories = useCallback(async () => {
    if (!plantingId) return;
    setLoading(true);
    setError(null);
    try {
      const statusParam = filter === 'ALL' ? undefined : filter;
      const data = await fetchAdvisories(plantingId, statusParam);
      setAdvisories(data);
    } catch (err) {
      console.error('Failed to load agricultural advisories:', err);
      setError(err?.message || 'Failed to retrieve agricultural advisories.');
    } finally {
      setLoading(false);
    }
  }, [plantingId, filter]);

  useEffect(() => {
    loadAdvisories();
  }, [loadAdvisories]);

  // Handle on-demand evaluation
  const handleEvaluate = async () => {
    if (evaluating || !plantingId) return;
    setEvaluating(true);
    setError(null);
    try {
      const summary = await evaluateAdvisories(plantingId);
      setLastEvaluationSummary(summary);
      await loadAdvisories();
      if (onAdvisoryUpdated) {
        onAdvisoryUpdated();
      }
    } catch (err) {
      console.error('Advisories evaluation failed:', err);
      setError(err?.message || 'Failed to evaluate field rules. Please try again.');
    } finally {
      setEvaluating(false);
    }
  };

  // Handle acknowledge action
  const handleAcknowledge = async (id) => {
    setActionLoadingId(id);
    try {
      await acknowledgeAdvisory(id);
      await loadAdvisories();
      if (onAdvisoryUpdated) {
        onAdvisoryUpdated();
      }
    } catch (err) {
      console.error('Failed to acknowledge advisory:', err);
      setError(err?.message || 'Failed to acknowledge advisory.');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Handle manual resolve action
  const handleConfirmResolve = async () => {
    if (!selectedAdvisoryForResolve) return;
    setIsResolving(true);
    try {
      await resolveAdvisory(selectedAdvisoryForResolve.id);
      setSelectedAdvisoryForResolve(null);
      await loadAdvisories();
      if (onAdvisoryUpdated) {
        onAdvisoryUpdated();
      }
    } catch (err) {
      console.error('Failed to resolve advisory:', err);
      setError(err?.message || 'Failed to resolve advisory.');
    } finally {
      setIsResolving(false);
    }
  };

  const activeCount = advisories.filter((a) => a.status === 'ACTIVE').length;

  return (
    <div className="bg-white p-5 md:p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-[#C9DCCB]">
        <div className="flex items-center space-x-2.5">
          <div className="p-2 bg-emerald-50 text-[#16A34A] rounded-xl border border-[#C9DCCB]">
            <ShieldAlert className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-base md:text-lg font-black text-[#222222]">
                {t('advisories.section.title', 'Agricultural Advisories & Field Alerts')}
              </h2>
              {filter === 'ACTIVE' && activeCount > 0 && (
                <span className="px-2 py-0.5 rounded-full text-xs font-black bg-rose-100 text-rose-800 border border-rose-200">
                  {activeCount}
                </span>
              )}
            </div>
            <p className="text-xs text-gray-500 font-medium mt-0.5">
              {t(
                'advisories.section.subtitle',
                'Deterministic recommendations combining weather forecasts, growth stage, and scheduled cultivation tasks.'
              )}
            </p>
          </div>
        </div>

        {/* Evaluate Button (debounced with spinner) */}
        <button
          type="button"
          onClick={handleEvaluate}
          disabled={evaluating}
          style={{ minHeight: '44px' }}
          className="flex items-center justify-center space-x-1.5 px-4 py-2 rounded-xl bg-[#16A34A] hover:bg-[#15803D] active:scale-95 text-white text-xs font-black shadow-2xs transition disabled:opacity-50 cursor-pointer self-start sm:self-auto shrink-0"
        >
          <RefreshCw className={`w-4 h-4 ${evaluating ? 'animate-spin' : ''}`} />
          <span>{evaluating ? t('advisories.actions.evaluating', 'Evaluating Rules...') : t('advisories.actions.evaluate', 'Evaluate Advisories')}</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center justify-between flex-wrap gap-2 pt-1">
        <div className="flex items-center space-x-1.5 p-1 bg-gray-100/80 rounded-xl border border-gray-200 text-xs font-bold text-gray-600">
          {['ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'ALL'].map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              className={`px-3 py-1.5 rounded-lg transition cursor-pointer text-xs font-black ${
                filter === f
                  ? 'bg-white text-[#222222] shadow-2xs'
                  : 'hover:text-[#222222] hover:bg-gray-200/50'
              }`}
            >
              {f === 'ALL' ? t('advisories.filter.all', 'All') : t(`advisories.status.${f}`, f)}
            </button>
          ))}
        </div>

        {lastEvaluationSummary && (
          <span className="text-[11px] text-gray-400 italic">
            {t('advisories.section.lastChecked', 'Evaluated {{count}} rules', {
              count: lastEvaluationSummary.totalRulesEvaluated ?? 4,
            })}
          </span>
        )}
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={loadAdvisories}
            className="text-xs font-black underline hover:text-rose-900 cursor-pointer"
          >
            {t('common.actions.retry', 'Retry')}
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading ? (
        <div className="p-8 text-center flex flex-col items-center justify-center space-y-2 bg-gray-50/50 rounded-xl border border-gray-150">
          <RefreshCw className="w-6 h-6 text-gray-400 animate-spin" />
          <p className="text-xs font-bold text-gray-500">
            {t('advisories.section.loading', 'Loading advisories...')}
          </p>
        </div>
      ) : advisories.length > 0 ? (
        /* List of Advisories */
        <div className="space-y-3">
          {advisories.map((advisory) => (
            <AgriculturalAdvisoryCard
              key={advisory.id}
              advisory={advisory}
              onAcknowledge={handleAcknowledge}
              onResolveClick={setSelectedAdvisoryForResolve}
              isActionLoading={actionLoadingId === advisory.id}
            />
          ))}
        </div>
      ) : (
        /* Empty State */
        <div className="p-6 bg-[#F0F7F1] rounded-xl border border-[#C9DCCB] text-center flex flex-col items-center space-y-2">
          <div className="p-3 bg-white rounded-full border border-[#C9DCCB] shadow-2xs">
            <CheckCircle2 className="h-7 w-7 text-[#16A34A]" />
          </div>
          <p className="text-sm font-extrabold text-[#16A34A]">
            {filter === 'ACTIVE'
              ? t('advisories.empty.active', 'All clear! No active risks or operational conflicts detected.')
              : t('advisories.empty.general', 'No advisories found for the selected filter.')}
          </p>
          <p className="text-xs text-[#525252] max-w-md">
            {t(
              'advisories.empty.hint',
              'Click "Evaluate Advisories" anytime to re-check weather conditions, growth stage, and scheduled tasks.'
            )}
          </p>
        </div>
      )}

      {/* Manual Resolve Modal */}
      <ResolveAdvisoryModal
        isOpen={Boolean(selectedAdvisoryForResolve)}
        onClose={() => setSelectedAdvisoryForResolve(null)}
        onConfirm={handleConfirmResolve}
        advisory={selectedAdvisoryForResolve}
        isResolving={isResolving}
      />
    </div>
  );
}
