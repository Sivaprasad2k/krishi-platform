import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Eye, CheckCircle, ShieldCheck, ChevronRight } from 'lucide-react';
import { reviewIncident, treatIncident, closeIncident } from '../api/diseaseIncidentApi';

export const ProblemCard = ({ incident, onStatusChange, showCropContext = true }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [loadingAction, setLoadingAction] = useState(null);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'REPORTED':
        return {
          label: t('diseaseIncident.status.REPORTED', 'Reported'),
          className: 'bg-red-50 text-red-750 border-red-200'
        };
      case 'UNDER_REVIEW':
        return {
          label: t('diseaseIncident.status.UNDER_REVIEW', 'Under Review'),
          className: 'bg-amber-50 text-amber-800 border-amber-200'
        };
      case 'TREATED':
        return {
          label: t('diseaseIncident.status.TREATED', 'Treated'),
          className: 'bg-blue-50 text-blue-750 border-blue-200'
        };
      case 'CLOSED':
        return {
          label: t('diseaseIncident.status.CLOSED', 'Closed'),
          className: 'bg-emerald-50 text-emerald-800 border-emerald-200'
        };
      default:
        return {
          label: status,
          className: 'bg-gray-50 text-gray-700 border-gray-200'
        };
    }
  };

  const handleReview = async (e) => {
    e.stopPropagation();
    setLoadingAction('review');
    try {
      await reviewIncident(incident.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to mark review:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleTreat = async (e) => {
    e.stopPropagation();
    setLoadingAction('treat');
    try {
      await treatIncident(incident.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to mark treated:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const handleClose = async (e) => {
    e.stopPropagation();
    setLoadingAction('close');
    try {
      await closeIncident(incident.id);
      if (onStatusChange) onStatusChange();
    } catch (err) {
      console.error('Failed to close incident:', err);
    } finally {
      setLoadingAction(null);
    }
  };

  const statusInfo = getStatusBadge(incident.status);

  return (
    <div
      onClick={() => navigate(`/disease-incidents/${incident.id}`)}
      className="bg-white p-5 rounded-2xl border border-[#C9DCCB] shadow-2xs hover:border-[#DC2626] hover:shadow-xs transition-all cursor-pointer space-y-3.5 border-l-4 border-l-[#DC2626]"
    >
      {/* Top Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center space-x-3 min-w-0">
          <div className="p-2.5 bg-[#FEF2F2] text-[#DC2626] rounded-xl shrink-0">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <h3 className="text-base font-black text-[#222222] truncate">{incident.diseaseName}</h3>
            {showCropContext && (
              <p className="text-xs font-semibold text-[#525252] truncate mt-0.5">
                {incident.cropName} ({incident.cropVariety || 'Standard'}) &bull; {incident.farmName || 'Farm'}
              </p>
            )}
          </div>
        </div>

        <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-black shrink-0 ${statusInfo.className}`}>
          {statusInfo.label}
        </span>
      </div>

      {/* Description if present */}
      {incident.description && (
        <p className="text-xs font-semibold text-[#525252] line-clamp-2 bg-[#F0F7F1] p-3 rounded-xl border border-[#C9DCCB]">
          {incident.description}
        </p>
      )}

      {/* Footer & Transition Actions */}
      <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#C9DCCB] text-xs font-bold text-[#6B7280]">
        <span>
          {t('diseaseIncident.reportedOn', 'Reported')}: {incident.createdAt ? new Date(incident.createdAt).toLocaleDateString() : 'Recently'}
        </span>

        <div className="flex items-center space-x-2">
          {incident.status === 'REPORTED' && (
            <button
              onClick={handleReview}
              disabled={loadingAction === 'review'}
              style={{ minHeight: '44px' }}
              className="px-3.5 py-2 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-[#D97706] font-black transition-all cursor-pointer flex items-center space-x-1"
            >
              <Eye className="h-3.5 w-3.5" />
              <span>{loadingAction === 'review' ? 'Updating...' : t('diseaseIncident.actions.review', 'Under Review')}</span>
            </button>
          )}

          {(incident.status === 'REPORTED' || incident.status === 'UNDER_REVIEW') && (
            <button
              onClick={handleTreat}
              disabled={loadingAction === 'treat'}
              style={{ minHeight: '44px' }}
              className="px-3.5 py-2 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-200 text-[#2563EB] font-black transition-all cursor-pointer flex items-center space-x-1"
            >
              <CheckCircle className="h-3.5 w-3.5" />
              <span>{loadingAction === 'treat' ? 'Updating...' : t('diseaseIncident.actions.treat', 'Mark Treated')}</span>
            </button>
          )}

          {incident.status !== 'CLOSED' && (
            <button
              onClick={handleClose}
              disabled={loadingAction === 'close'}
              style={{ minHeight: '44px' }}
              className="px-3.5 py-2 rounded-xl bg-[#F0F7F1] hover:bg-[#E3F2E6] border border-[#C9DCCB] text-[#16A34A] font-black transition-all cursor-pointer flex items-center space-x-1"
            >
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>{loadingAction === 'close' ? 'Updating...' : t('diseaseIncident.actions.close', 'Close Issue')}</span>
            </button>
          )}

          <ChevronRight className="h-4 w-4 text-[#6B7280] ml-1" />
        </div>
      </div>
    </div>
  );
};

export default ProblemCard;
