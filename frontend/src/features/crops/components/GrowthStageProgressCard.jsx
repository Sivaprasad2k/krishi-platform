import React from 'react';
import { useTranslation } from 'react-i18next';
import { Sprout, Calendar, Clock, AlertCircle, Info, ChevronRight, Layers } from 'lucide-react';

/**
 * GrowthStageProgressCard
 * Renders the phenological growth stage timeline derived from applicable
 * cultivation templates and calendar elapsed days.
 *
 * Explicitly labels automated model estimations as estimates, distinguishing
 * them from actual field observations.
 */
export default function GrowthStageProgressCard({
  estimatedGrowthStage,
  crop = {},
  cultivationPlan,
  onGeneratePlanClick,
}) {
  const { t } = useTranslation();

  const formatDate = (dateStr) => {
    if (!dateStr) return null;
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return null;
      return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return null;
    }
  };

  const plantingDateFormatted = formatDate(crop.plantingDate);
  const expectedHarvestFormatted = formatDate(crop.expectedHarvestDate);

  // Fallback 1: No growth stage data provided or null
  if (!estimatedGrowthStage) {
    return (
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-3">
        <div className="flex items-center space-x-2 pb-2 border-b border-[#C9DCCB]">
          <Sprout className="h-5 w-5 text-[#16A34A]" />
          <h2 className="text-base md:text-lg font-black text-[#222222]">
            {t('crops.stage.title', 'Phenological Growth Stage')}
          </h2>
        </div>
        <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 text-center flex flex-col items-center space-y-1">
          <Info className="h-6 w-6 text-gray-500" />
          <p className="text-xs font-semibold text-gray-600">
            {t('crops.stage.unavailable', 'Growth stage estimation is currently unavailable for this planting.')}
          </p>
        </div>
      </div>
    );
  }

  const {
    status,
    stageName,
    stageOrder,
    totalStages,
    elapsedDays,
    stageStartDay,
    stageEndDay,
    stageProgressPercentage,
    overallProgressPercentage,
    templateName,
    templateVersion,
    message,
  } = estimatedGrowthStage;

  // Fallback 2: No active cultivation plan
  if (status === 'NO_ACTIVE_PLAN') {
    return (
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Sprout className="h-5 w-5 text-[#16A34A]" />
            <h2 className="text-base md:text-lg font-black text-[#222222]">
              {t('crops.stage.title', 'Phenological Growth Stage')}
            </h2>
          </div>
          <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">
            {t('crops.stage.noPlanBadge', 'Plan Required')}
          </span>
        </div>

        <div className="p-4 bg-amber-50/60 rounded-xl border border-amber-200 text-amber-900 space-y-2">
          <div className="flex items-start space-x-2.5">
            <AlertCircle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-bold">
                {t('crops.stage.noPlanMessage', 'No active cultivation plan is configured for this crop planting.')}
              </p>
              <p className="text-[11px] text-amber-700 mt-0.5">
                {t(
                  'crops.stage.noPlanHint',
                  'Dynamic growth stage estimation and operational advisories require an active cultivation protocol.'
                )}
              </p>
            </div>
          </div>
          {onGeneratePlanClick && (
            <div className="pt-1">
              <button
                type="button"
                onClick={onGeneratePlanClick}
                className="text-xs font-black text-white bg-[#16A34A] hover:bg-[#15803D] px-3.5 py-1.5 rounded-lg shadow-2xs transition-all cursor-pointer"
              >
                {t('crops.details.generatePlan', 'Generate Cultivation Plan')}
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Fallback 3: Missing planting date
  if (status === 'MISSING_PLANTING_DATE') {
    return (
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Sprout className="h-5 w-5 text-[#16A34A]" />
            <h2 className="text-base md:text-lg font-black text-[#222222]">
              {t('crops.stage.title', 'Phenological Growth Stage')}
            </h2>
          </div>
          <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 border border-rose-200">
            {t('crops.stage.missingDateBadge', 'Date Required')}
          </span>
        </div>

        <div className="p-4 bg-rose-50/60 rounded-xl border border-rose-200 text-rose-900 flex items-start space-x-2.5">
          <AlertCircle className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="text-xs font-bold">
            <p>{t('crops.stage.missingDate', 'Planting date is not set.')}</p>
            <p className="text-[11px] text-rose-700 font-medium mt-0.5">
              {t('crops.stage.missingDateHint', 'Please update the crop record with a valid sowing or transplanting date to activate stage tracking.')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Fallback 4: Pre-planting window
  if (status === 'PRE_PLANTING') {
    return (
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Sprout className="h-5 w-5 text-[#16A34A]" />
            <h2 className="text-base md:text-lg font-black text-[#222222]">
              {t('crops.stage.title', 'Phenological Growth Stage')}
            </h2>
          </div>
          <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 border border-blue-200">
            {t('crops.stage.prePlantingBadge', 'Pre-Planting')}
          </span>
        </div>

        <div className="p-4 bg-blue-50/60 rounded-xl border border-blue-200 text-blue-900 space-y-1">
          <div className="flex items-center space-x-2">
            <Calendar className="h-4 w-4 text-blue-600" />
            <span className="text-xs font-bold">
              {t('crops.stage.scheduledFor', 'Scheduled planting date')}: {plantingDateFormatted || 'Pending'}
            </span>
          </div>
          {elapsedDays != null && (
            <p className="text-[11px] text-blue-700 font-medium">
              {Math.abs(elapsedDays)} {t('crops.stage.daysUntil', 'days until scheduled planting.')}
            </p>
          )}
        </div>
      </div>
    );
  }

  // Fallback 5: Post-harvest cycle completed
  if (status === 'POST_HARVEST') {
    return (
      <div className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Sprout className="h-5 w-5 text-[#16A34A]" />
            <h2 className="text-base md:text-lg font-black text-[#222222]">
              {t('crops.stage.title', 'Phenological Growth Stage')}
            </h2>
          </div>
          <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
            {t('crops.stage.postHarvestBadge', 'Cycle Concluded')}
          </span>
        </div>

        <div className="p-4 bg-emerald-50/60 rounded-xl border border-emerald-200 text-emerald-900 space-y-1">
          <p className="text-xs font-bold">
            {t('crops.stage.postHarvestMessage', 'The planned cultivation template duration for this planting has elapsed.')}
          </p>
          {elapsedDays != null && (
            <p className="text-[11px] text-emerald-700 font-medium">
              {elapsedDays} {t('crops.stage.daysElapsed', 'total days elapsed since planting.')}
            </p>
          )}
        </div>
      </div>
    );
  }

  // Primary: Status === 'RESOLVED'
  const clampedStageProgress = Math.min(100, Math.max(0, stageProgressPercentage ?? 0));
  const clampedOverallProgress = Math.min(100, Math.max(0, overallProgressPercentage ?? 0));

  return (
    <div className="bg-white p-5 md:p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-3 border-b border-[#C9DCCB]">
        <div className="flex items-center space-x-2">
          <Sprout className="h-5 w-5 text-[#16A34A]" />
          <h2 className="text-base md:text-lg font-black text-[#222222]">
            {t('crops.stage.title', 'Phenological Growth Stage')}
          </h2>
        </div>

        {/* Clear automated estimate disclaimer badge */}
        <div className="flex items-center space-x-1.5 self-start sm:self-auto">
          <span
            className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200"
            title="Estimated growth stage calculated from elapsed calendar days and cultivation plan templates. Not a field observation."
          >
            <Clock className="w-3 h-3 text-emerald-600" />
            <span>{t('crops.stage.estimateBadge', 'Estimated Stage')}</span>
          </span>
        </div>
      </div>

      {/* Model vs Field Observation Clarification Banner */}
      <div className="flex items-start space-x-2 p-2.5 bg-blue-50/60 rounded-xl border border-blue-150 text-[11px] text-blue-900">
        <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
        <span className="leading-snug">
          {t(
            'crops.stage.estimateDisclaimer',
            'Estimated by automated phenological model based on days since planting and cultivation plan protocols. Field observations may vary based on microclimate and soil conditions.'
          )}
        </span>
      </div>

      {/* Current Stage Highlight */}
      <div className="p-4 bg-gradient-to-r from-[#F0F7F1] to-emerald-50/40 rounded-xl border border-[#C9DCCB] space-y-3">
        <div className="flex items-start justify-between gap-2">
          <div>
            <div className="flex items-center space-x-2 text-[11px] font-extrabold text-[#16A34A] uppercase tracking-wide">
              <Layers className="w-3.5 h-3.5" />
              <span>
                {`Stage ${stageOrder}${totalStages ? ` of ${totalStages}` : ''}`}
              </span>
            </div>
            <h3 className="text-lg md:text-xl font-black text-[#222222] mt-0.5">{stageName}</h3>
          </div>

          <div className="text-right shrink-0">
            <span className="text-xs font-bold text-[#525252] block">
              {t('crops.stage.daysElapsedLabel', 'Days Elapsed')}
            </span>
            <span className="text-base md:text-lg font-black text-[#16A34A]">
              {`Day ${elapsedDays ?? 0}`}
            </span>
          </div>
        </div>

        {/* Current Stage Progress Bar */}
        <div className="space-y-1.5">
          <div className="flex justify-between text-[11px] font-bold text-[#525252]">
            <span>
              {`Window: Day ${stageStartDay ?? 0} – ${stageEndDay ?? '—'}`}
            </span>
            <span>{Math.round(clampedStageProgress)}% {t('crops.stage.completed', 'completed')}</span>
          </div>
          <div className="w-full bg-emerald-100/70 rounded-full h-2.5 overflow-hidden">
            <div
              className="bg-[#16A34A] h-2.5 rounded-full transition-all duration-500 ease-out"
              style={{ width: `${clampedStageProgress}%` }}
              role="progressbar"
              aria-valuenow={Math.round(clampedStageProgress)}
              aria-valuemin="0"
              aria-valuemax="100"
              aria-label={`Progress within ${stageName}`}
            />
          </div>
        </div>
      </div>

      {/* Key Dates & Overall Cycle Progress */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
        {/* Dates Block */}
        <div className="p-3 bg-gray-50/70 border border-gray-200 rounded-xl space-y-1 text-xs">
          <div className="flex justify-between">
            <span className="text-gray-500 font-semibold">{t('crops.details.datePlanted', 'Planted')}:</span>
            <span className="font-bold text-gray-800">{plantingDateFormatted || 'Not recorded'}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500 font-semibold">{t('crops.details.expectedHarvest', 'Expected Harvest')}:</span>
            <span className="font-bold text-gray-800">{expectedHarvestFormatted || 'Not specified'}</span>
          </div>
        </div>

        {/* Overall Cycle Progress Block */}
        <div className="p-3 bg-gray-50/70 border border-gray-200 rounded-xl space-y-1.5 text-xs">
          <div className="flex justify-between font-bold text-gray-700">
            <span>{t('crops.stage.overallCycle', 'Overall Cultivation Progress')}</span>
            <span className="text-[#16A34A] font-black">{Math.round(clampedOverallProgress)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
            <div
              className="bg-[#2563EB] h-2 rounded-full transition-all duration-500 ease-out"
              style={{ width: `${clampedOverallProgress}%` }}
              role="progressbar"
              aria-valuenow={Math.round(clampedOverallProgress)}
              aria-valuemin="0"
              aria-valuemax="100"
              aria-label="Overall cultivation progress"
            />
          </div>
        </div>
      </div>

      {/* Provenance Footer */}
      {(templateName || templateVersion) && (
        <div className="text-[11px] text-gray-500 flex items-center justify-between pt-1 border-t border-gray-150">
          <span>
            {t('crops.stage.protocolBasis', 'Protocol Basis')}:{' '}
            <strong className="text-gray-700">{templateName || 'Cultivation Template'}</strong>
            {templateVersion ? ` (v${templateVersion})` : ''}
          </span>
          {message && <span className="hidden md:inline italic text-gray-400">{message}</span>}
        </div>
      )}
    </div>
  );
}
