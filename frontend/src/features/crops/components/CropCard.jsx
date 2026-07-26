import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ChevronRight, AlertTriangle, CheckSquare } from 'lucide-react';
import CropStatusBadge from './CropStatusBadge';

export const CropCard = ({ crop }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const getCropIcon = (name) => {
    if (!name) return '🌿';
    const clean = name.toLowerCase();
    if (clean.includes('paddy') || clean.includes('rice')) return '🌾';
    if (clean.includes('banana')) return '🌱';
    if (clean.includes('coconut')) return '🥥';
    if (clean.includes('tomato')) return '🍅';
    return '🌿';
  };

  const formatDaysPlanted = (dateStr) => {
    if (!dateStr) return '';
    const planted = new Date(dateStr);
    const now = new Date();
    planted.setHours(0, 0, 0, 0);
    now.setHours(0, 0, 0, 0);
    const diffTime = now - planted;
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    if (diffDays <= 0) {
      return t('crops.card.plantedToday', 'Planted today');
    }
    return t('crops.card.plantedDaysAgo', 'Planted {{days}} days ago', { days: diffDays });
  };

  let badgeType = 'healthy';
  if (crop.status === 'COMPLETED') {
    badgeType = 'completed';
  } else if (crop.problemCount > 0) {
    badgeType = 'problem';
  } else if (crop.pendingTaskCount > 0) {
    badgeType = 'attention';
  }

  return (
    <div
      onClick={() => navigate(`/crops/${crop.id}`)}
      className="rounded-2xl bg-white p-5 border border-[#C9DCCB] shadow-2xs flex flex-col justify-between hover:border-[#087A55] hover:shadow-xs transition-all cursor-pointer"
    >
      <div className="space-y-4">
        {/* Header: Crop Icon & Name */}
        <div className="flex items-center space-x-3 pb-3 border-b border-[#C9DCCB]">
          <div className="text-3xl p-1.5 bg-[#E3F2E6] rounded-xl shrink-0">
            {getCropIcon(crop.cropName)}
          </div>
          <div className="min-w-0">
            <h3 className="text-base md:text-lg font-black text-[#222222] truncate">
              {crop.cropName}
            </h3>
            <span className="text-xs font-semibold text-[#525252] block truncate">
              {crop.cropVariety || t('crops.card.noVariety', 'Standard')}
            </span>
          </div>
        </div>

        {/* Middle Info: Planting details */}
        <div className="space-y-2.5 text-sm">
          <div className="flex justify-between font-semibold text-[#525252]">
            <span>{t('crops.card.datePlanted', 'Date Planted')}:</span>
            <span className="font-black text-[#222222]">
              {crop.plantingDate ? new Date(crop.plantingDate).toLocaleDateString() : 'N/A'}
            </span>
          </div>
          
          <div className="flex justify-between font-semibold text-[#525252]">
            <span>{t('crops.card.duration', 'Timeline')}:</span>
            <span className="font-black text-[#222222]">
              {formatDaysPlanted(crop.plantingDate)}
            </span>
          </div>

          <div className="flex justify-between font-semibold text-[#525252]">
            <span>{t('crops.card.season', 'Season')}:</span>
            <span className="font-black text-[#087A55] truncate max-w-[150px]">
              {crop.seasonName || 'N/A'}
            </span>
          </div>
        </div>

        {/* Counter Indicators: Problems & Tasks */}
        <div className="grid grid-cols-2 gap-2 pt-1 border-t border-[#C9DCCB]">
          <div className="flex items-center space-x-1.5 text-xs font-bold text-[#525252]">
            <AlertTriangle className={`h-4 w-4 shrink-0 ${crop.problemCount > 0 ? 'text-[#DC2626]' : 'text-[#6B7280]'}`} />
            <span>{t('crops.card.problems', 'Problems')}: {crop.problemCount || 0}</span>
          </div>
          <div className="flex items-center space-x-1.5 text-xs font-bold text-[#525252]">
            <CheckSquare className={`h-4 w-4 shrink-0 ${crop.pendingTaskCount > 0 ? 'text-[#2563EB]' : 'text-[#6B7280]'}`} />
            <span>{t('crops.card.tasks', 'Tasks')}: {crop.pendingTaskCount || 0}</span>
          </div>
        </div>
      </div>

      {/* Footer: Status Badge & CTA */}
      <div className="mt-5 pt-3.5 border-t border-[#C9DCCB] flex items-center justify-between">
        <CropStatusBadge type={badgeType} />
        
        <div className="flex items-center text-[#087A55] font-black text-sm hover:underline">
          <span>{t('crops.card.viewDetails', 'Workspace')}</span>
          <ChevronRight className="h-4 w-4 ml-0.5" />
        </div>
      </div>
    </div>
  );
};

export default CropCard;
