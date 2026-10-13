import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Sprout, CheckCircle2, RefreshCw, BookOpen, Layers } from 'lucide-react';
import GeneratePlanModal from './GeneratePlanModal';

export const CultivationPlanCard = ({ plan, plantingId, cropName, onPlanUpdated }) => {
  const { t } = useTranslation();
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <div className="bg-white p-6 rounded-2xl border border-[#C9DCCB] shadow-2xs space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[#C9DCCB]">
          <div className="flex items-center space-x-2">
            <Sprout className="h-5 w-5 text-[#087A55]" />
            <h2 className="text-lg font-black text-[#222222]">
              {t('crops.plan.sectionTitle', 'Cultivation Plan')}
            </h2>
          </div>
          {plan ? (
            <button
              onClick={() => setIsModalOpen(true)}
              style={{ minHeight: '38px' }}
              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl border border-[#C9DCCB] bg-white text-[#087A55] hover:bg-[#E3F2E6] text-xs font-black transition-all cursor-pointer shadow-2xs"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>{t('crops.plan.switchPlan', 'Revise Plan')}</span>
            </button>
          ) : (
            <button
              onClick={() => setIsModalOpen(true)}
              style={{ minHeight: '38px' }}
              className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl bg-[#087A55] hover:bg-[#066747] text-white text-xs font-black transition-all cursor-pointer shadow-2xs"
            >
              <Sprout className="h-3.5 w-3.5" />
              <span>{t('crops.plan.generateBtn', 'Generate Plan')}</span>
            </button>
          )}
        </div>

        {plan ? (
          <div className="space-y-3.5">
            <div className="p-4 bg-[#F0F7F1] rounded-xl border border-[#C9DCCB] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div className="space-y-1">
                <div className="flex items-center space-x-2">
                  <h3 className="text-base font-black text-[#222222]">
                    {plan.templateName || t('crops.plan.defaultTemplateName', 'Automated Cultivation Plan')}
                  </h3>
                  <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-[#E3F2E6] text-[#087A55] border border-[#C9DCCB]">
                    {plan.templateVersion || 'v1.0'}
                  </span>
                </div>
                <div className="flex items-center space-x-2 text-xs font-semibold text-[#525252]">
                  <BookOpen className="h-3.5 w-3.5 text-[#087A55] shrink-0" />
                  <span>{plan.recommendationSource || 'ICAR Package of Practices'}</span>
                </div>
              </div>

              <div className="flex items-center space-x-2 self-start sm:self-center">
                <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-black bg-[#E3F2E6] text-[#087A55] border border-[#C9DCCB]">
                  <CheckCircle2 className="h-3 w-3 mr-1" />
                  {plan.status || 'ACTIVE'}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 bg-gray-50/70 rounded-xl border border-gray-150 space-y-0.5">
                <span className="text-gray-400 font-bold uppercase text-[10px] tracking-wider block flex items-center space-x-1">
                  <Layers className="h-3 w-3 text-emerald-600 inline mr-1" />
                  {t('crops.plan.tasksCount', 'Generated Activities')}
                </span>
                <span className="text-gray-850 font-black text-sm">
                  {plan.generatedTasksCount ?? 'N/A'} {t('crops.plan.tasksUnit', 'operations scheduled')}
                </span>
              </div>

              <div className="p-3 bg-gray-50/70 rounded-xl border border-gray-150 space-y-0.5">
                <span className="text-gray-400 font-bold uppercase text-[10px] tracking-wider block">
                  {t('crops.plan.lastUpdated', 'Plan Initialized')}
                </span>
                <span className="text-gray-850 font-black text-sm">
                  {plan.createdAt ? new Date(plan.createdAt).toLocaleDateString() : 'Active'}
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className="p-4 bg-[#F0F7F1] rounded-xl border border-[#C9DCCB] text-center flex flex-col items-center space-y-2">
            <Sprout className="h-7 w-7 text-[#087A55]" />
            <div className="space-y-0.5">
              <span className="text-sm font-extrabold text-[#087A55] block">
                {t('crops.plan.noActivePlan', 'No automated cultivation plan active')}
              </span>
              <p className="text-xs font-semibold text-[#525252]">
                {t('crops.plan.noActivePlanDesc', 'Generate an initial cultivation plan to automatically schedule field activities from certified agronomic guidance.')}
              </p>
            </div>
          </div>
        )}
      </div>

      <GeneratePlanModal
        plantingId={plantingId}
        cropName={cropName}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={onPlanUpdated}
      />
    </>
  );
};

export default CultivationPlanCard;
