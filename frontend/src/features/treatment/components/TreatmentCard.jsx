import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Droplet, Play, CheckCircle, ChevronRight, Beaker } from 'lucide-react';
import { startTreatment, completeTreatment } from '../api/treatmentApi';

export const TreatmentCard = ({ treatment, onStatusChange, showProblemContext = true }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [loadingAction, setLoadingAction] = useState(null);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PLANNED':
        return {
          label: t('treatment.status.PLANNED', 'Planned'),
          className: 'bg-amber-50 text-amber-800 border-amber-200'
        };
      case 'IN_PROGRESS':
        return {
          label: t('treatment.status.IN_PROGRESS', 'In Progress'),
          className: 'bg-blue-50 text-blue-750 border-blue-200'
        };
      case 'COMPLETED':
        return {
          label: t('treatment.status.COMPLETED', 'Completed'),
          className: 'bg-emerald-50 text-emerald-800 border-emerald-200'
        };
      default:
        return {
          label: status,
          className: 'bg-gray-50 text-gray-700 border-gray-200'
        };
    }
  };

  const handleStart = async (e) => {
    e.stopPropagation();
    setLoadingAction('start');
    try {
      await startTreatment(treatment.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to start treatment:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleComplete = async (e) => {
    e.stopPropagation();
    setLoadingAction('complete');
    try {
      await completeTreatment(treatment.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to complete treatment:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const statusInfo = getStatusBadge(treatment.status);

  const getFormattedMethod = (method) => {
    if (!method) return '';
    const trimmed = method.trim();
    const methodMap = {
      'Foliar Spray': 'treatment.methods.foliarSpray',
      'Soil Drenching': 'treatment.methods.soilDrenching',
      'Seed Treatment': 'treatment.methods.seedTreatment',
      'Pruning / Sanitation': 'treatment.methods.pruning'
    };
    if (methodMap[trimmed]) {
      return t(methodMap[trimmed], trimmed);
    }
    return method;
  };

  return (
    <div
      onClick={() => navigate(`/treatments/${treatment.id}`)}
      className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs hover:border-[#087A55] hover:shadow-xs transition-all cursor-pointer space-y-3.5 border-l-4 border-l-[#2563EB]"
    >
      {/* Top Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center space-x-3 min-w-0">
          <div className="p-2.5 bg-blue-50 text-[#2563EB] rounded-xl shrink-0">
            <Droplet className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <h3 className="text-base font-black text-[#222222] truncate">{treatment.treatmentName}</h3>
            {showProblemContext && (
              <p className="text-xs font-semibold text-[#525252] truncate mt-0.5">
                {t('treatment.targetLabel', 'Target')}: {treatment.diseaseName || 'Disease'} &bull; {treatment.cropName || 'Crop'} ({treatment.farmName || 'Farm'})
              </p>
            )}
          </div>
        </div>

        <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-black shrink-0 ${statusInfo.className}`}>
          {statusInfo.label}
        </span>
      </div>

      {/* Dosage & Application Method Tags */}
      {(treatment.recommendedDosage || treatment.applicationMethod) && (
        <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
          {treatment.recommendedDosage && (
            <span className="bg-blue-50 text-[#2563EB] px-3 py-1 rounded-xl border border-blue-200 flex items-center space-x-1 font-black">
              <Beaker className="h-3.5 w-3.5 text-[#2563EB]" />
              <span>{t('treatment.dosageLabel', 'Dosage')}: {treatment.recommendedDosage}</span>
            </span>
          )}
          {treatment.applicationMethod && (
            <span className="bg-[#F0F7F1] text-[#222222] px-3 py-1 rounded-xl border border-[#C9DCCB] font-semibold">
              {t('treatment.methodLabel', 'Method')}: {getFormattedMethod(treatment.applicationMethod)}
            </span>
          )}
        </div>
      )}

      {/* Description if present */}
      {treatment.description && (
        <p className="text-xs font-semibold text-[#525252] line-clamp-2 bg-[#F0F7F1] p-3 rounded-xl border border-[#C9DCCB]">
          {treatment.description}
        </p>
      )}

      {/* Footer & Transition Actions */}
      <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#C9DCCB] text-xs font-bold text-[#6B7280]">
        <span>
          {t('treatment.createdOn', 'Created')}: {treatment.createdAt ? new Date(treatment.createdAt).toLocaleDateString() : 'Recently'}
        </span>

        <div className="flex items-center space-x-2">
          {treatment.status === 'PLANNED' && (
            <button
              onClick={handleStart}
              disabled={loadingAction === 'start'}
              style={{ minHeight: '44px' }}
              className="px-3.5 py-2 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-200 text-[#2563EB] font-black transition-all cursor-pointer flex items-center space-x-1"
            >
              <Play className="h-3.5 w-3.5 fill-current" />
              <span>{loadingAction === 'start' ? 'Updating...' : t('treatment.actions.start', 'Start Treatment')}</span>
            </button>
          )}

          {treatment.status === 'IN_PROGRESS' && (
            <button
              onClick={handleComplete}
              disabled={loadingAction === 'complete'}
              style={{ minHeight: '44px' }}
              className="px-3.5 py-2 rounded-xl bg-[#F0F7F1] hover:bg-[#E3F2E6] border border-[#C9DCCB] text-[#16A34A] font-black transition-all cursor-pointer flex items-center space-x-1"
            >
              <CheckCircle className="h-3.5 w-3.5" />
              <span>{loadingAction === 'complete' ? 'Updating...' : t('treatment.actions.complete', 'Mark Completed')}</span>
            </button>
          )}

          <ChevronRight className="h-4 w-4 text-[#6B7280] ml-1" />
        </div>
      </div>
    </div>
  );
};

export default TreatmentCard;
