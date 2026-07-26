import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, AlertTriangle, Eye, CheckCircle, ShieldCheck, Clock, Sprout, Plus, Droplet } from 'lucide-react';
import StateView from '../../../shared/components/StateView';
import { getIncidentById, reviewIncident, treatIncident, closeIncident } from '../api/diseaseIncidentApi';
import { getTreatmentsByIncident } from '../../treatment/api/treatmentApi';
import TreatmentCard from '../../treatment/components/TreatmentCard';
import AddTreatmentModal from '../../treatment/components/AddTreatmentModal';

export const DiseaseIncidentDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [incident, setIncident] = useState(null);
  const [treatments, setTreatments] = useState([]);
  const [viewState, setViewState] = useState('loading');
  const [loadingAction, setLoadingAction] = useState(null);
  const [isAddTreatmentModalOpen, setIsAddTreatmentModalOpen] = useState(false);

  const loadDetails = async () => {
    setViewState('loading');
    try {
      const data = await getIncidentById(id);
      if (data?.success && data?.data) {
        setIncident(data.data);
        
        try {
          const treatRes = await getTreatmentsByIncident(id);
          setTreatments(treatRes?.data || []);
        } catch (err) {
          console.warn('Failed to fetch treatments for incident:', err);
        }

        setViewState('success');
      } else {
        throw new Error(data?.message || 'Failed to load problem details');
      }
    } catch (err) {
      console.error('Failed to load incident details:', err);
      setViewState(navigator.onLine ? 'error' : 'offline');
    }
  };

  useEffect(() => {
    loadDetails();
  }, [id]);

  const handleAction = async (actionFn, actionName) => {
    setLoadingAction(actionName);
    try {
      await actionFn(id);
      await loadDetails();
    } catch (err) {
      console.error(`Failed action ${actionName}:`, err);
    } finally {
      setLoadingAction(null);
    }
  };

  if (viewState === 'loading' || viewState === 'error' || viewState === 'offline') {
    return (
      <div className="max-w-2xl mx-auto p-4">
        <StateView state={viewState} onRetry={loadDetails} />
      </div>
    );
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'REPORTED': return 'bg-red-50 text-red-750 border-red-200';
      case 'UNDER_REVIEW': return 'bg-amber-50 text-amber-800 border-amber-200';
      case 'TREATED': return 'bg-blue-50 text-blue-750 border-blue-200';
      case 'CLOSED': return 'bg-emerald-50 text-emerald-800 border-emerald-200';
      default: return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto pb-12">
      {/* Header */}
      <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex items-center space-x-3">
        <button
          onClick={() => navigate('/disease-incidents')}
          className="p-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-600 transition-all cursor-pointer"
          aria-label={t('common.actions.back', 'Back')}
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-xl font-black text-gray-850">
            {t('diseaseIncident.actions.viewDetails', 'Problem Workspace')}
          </h1>
          <p className="text-xs font-semibold text-gray-400 mt-0.5">
            ID: {incident.id}
          </p>
        </div>
      </div>

      {/* Main Card */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-6 border-l-4 border-l-red-500">
        <div className="flex items-start justify-between gap-3 pb-4 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-red-50 text-red-650 rounded-2xl">
              <AlertTriangle className="h-7 w-7" />
            </div>
            <div>
              <h2 className="text-2xl font-black text-gray-900">{incident.diseaseName}</h2>
              <span className="text-xs font-bold text-gray-400 mt-0.5 block">
                Reported on {incident.reportedAt ? new Date(incident.reportedAt).toLocaleDateString() : 'Recently'}
              </span>
            </div>
          </div>

          <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ${getStatusBadgeClass(incident.status)}`}>
            {t(`diseaseIncident.status.${incident.status}`, incident.status)}
          </span>
        </div>

        {/* Symptoms / Notes */}
        <div className="space-y-2">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">
            {t('diseaseIncident.description', 'Symptoms / Notes')}
          </h3>
          <p className="text-base text-gray-800 font-semibold leading-relaxed bg-gray-50 p-4 rounded-xl border border-gray-150">
            {incident.description || 'No detailed symptoms noted.'}
          </p>
        </div>

        {/* Timeline Milestones */}
        <div className="space-y-3 pt-2">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Status History</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-extrabold">
            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <Clock className="h-4 w-4 text-red-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Reported</span>
                <span className="text-gray-800">{incident.reportedAt ? new Date(incident.reportedAt).toLocaleString() : 'N/A'}</span>
              </div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <Eye className="h-4 w-4 text-amber-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Reviewed</span>
                <span className="text-gray-800">{incident.reviewedAt ? new Date(incident.reviewedAt).toLocaleString() : 'Not reviewed yet'}</span>
              </div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <CheckCircle className="h-4 w-4 text-blue-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Treated</span>
                <span className="text-gray-800">{incident.treatedAt ? new Date(incident.treatedAt).toLocaleString() : 'Not treated yet'}</span>
              </div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl border border-gray-150 flex items-center space-x-2">
              <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
              <div>
                <span className="text-gray-400 block font-bold">Closed</span>
                <span className="text-gray-800">{incident.closedAt ? new Date(incident.closedAt).toLocaleString() : 'Active issue'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        {incident.status !== 'CLOSED' && (
          <div className="pt-4 border-t border-gray-100 flex flex-wrap items-center gap-3">
            {incident.status === 'REPORTED' && (
              <button
                onClick={() => handleAction(reviewIncident, 'review')}
                disabled={loadingAction === 'review'}
                style={{ minHeight: '48px' }}
                className="flex-1 px-4 py-2.5 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-800 font-extrabold transition-all cursor-pointer flex items-center justify-center space-x-2"
              >
                <Eye className="h-4 w-4" />
                <span>{loadingAction === 'review' ? 'Updating...' : t('diseaseIncident.actions.review', 'Mark Under Review')}</span>
              </button>
            )}

            {(incident.status === 'REPORTED' || incident.status === 'UNDER_REVIEW') && (
              <button
                onClick={() => handleAction(treatIncident, 'treat')}
                disabled={loadingAction === 'treat'}
                style={{ minHeight: '48px' }}
                className="flex-1 px-4 py-2.5 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-200 text-blue-800 font-extrabold transition-all cursor-pointer flex items-center justify-center space-x-2"
              >
                <CheckCircle className="h-4 w-4" />
                <span>{loadingAction === 'treat' ? 'Updating...' : t('diseaseIncident.actions.treat', 'Mark Treated')}</span>
              </button>
            )}

            <button
              onClick={() => handleAction(closeIncident, 'close')}
              disabled={loadingAction === 'close'}
              style={{ minHeight: '48px' }}
              className="flex-1 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold shadow-sm transition-all cursor-pointer flex items-center justify-center space-x-2"
            >
              <ShieldCheck className="h-4 w-4" />
              <span>{loadingAction === 'close' ? 'Updating...' : t('diseaseIncident.actions.close', 'Close Issue')}</span>
            </button>
          </div>
        )}
      </div>

      {/* Treatments Section */}
      <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-2">
            <Droplet className="h-5 w-5 text-blue-650" />
            <h2 className="text-lg font-black text-gray-800">{t('treatment.title', 'Treatment Plans')}</h2>
          </div>
          <button
            onClick={() => setIsAddTreatmentModalOpen(true)}
            className="flex items-center space-x-1 px-3.5 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-black transition-all shadow-sm active:scale-95 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>{t('treatment.addButton', 'Plan Treatment')}</span>
          </button>
        </div>

        {treatments.length > 0 ? (
          <div className="space-y-3">
            {treatments.map((tr) => (
              <TreatmentCard
                key={tr.id}
                treatment={{ ...tr, diseaseName: incident.diseaseName }}
                onStatusChange={loadDetails}
                showProblemContext={false}
              />
            ))}
          </div>
        ) : (
          <div className="p-4 bg-blue-50/50 rounded-xl border border-blue-100 text-center flex flex-col items-center space-y-2">
            <Droplet className="h-7 w-7 text-blue-500" />
            <span className="text-sm font-extrabold text-blue-800">
              {t('treatment.emptyStateDesc', 'Active treatments will appear here once planned for reported crop problems.')}
            </span>
          </div>
        )}
      </div>

      <AddTreatmentModal
        isOpen={isAddTreatmentModalOpen}
        onClose={() => setIsAddTreatmentModalOpen(false)}
        onSuccess={loadDetails}
        preselectedIncidentId={incident.id}
      />
    </div>
  );
};

export default DiseaseIncidentDetailPage;
