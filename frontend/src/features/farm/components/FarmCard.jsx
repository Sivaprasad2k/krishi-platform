import { MapPin, Scale, ChevronRight, CheckCircle2, AlertCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { fromHectares, formatArea } from '../../../shared/utils/areaConversion';

/**
 * FarmCard displays a summary of a Farm.
 * @param {Object} props.farm The Farm object from the API response
 */
const FarmCard = ({ farm }) => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const isStatusActive = farm?.status === 'ACTIVE';

  // Load preferences from localStorage
  const pref = localStorage.getItem(`farm_pref_${farm?.id}`);
  const { unit, photo } = pref ? JSON.parse(pref) : { unit: 'HECTARE', photo: null };

  const valueInPrefUnit = fromHectares(farm?.sizeHectares || 0, unit);
  const formattedArea = formatArea(valueInPrefUnit, unit, t);

  return (
    <div
      onClick={() => farm?.id && navigate(`/farms/${farm.id}`)}
      className="group relative rounded-2xl bg-white shadow-2xs border border-[#C9DCCB] hover:border-[#087A55] hover:shadow-xs transition-all cursor-pointer flex flex-col overflow-hidden justify-between"
    >
      {photo && (
        <div className="h-32 w-full overflow-hidden bg-[#F0F7F1] border-b border-[#C9DCCB]">
          <img
            src={photo}
            alt={farm?.name || ''}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        </div>
      )}
      <div className="p-5 flex-1 flex flex-col justify-between">
        <div>
          <div className="flex items-start justify-between mb-4">
            <h3 className="text-lg font-black text-[#222222] group-hover:text-[#087A55] transition-colors truncate pr-2">
              {farm?.name || ''}
            </h3>
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-black ${
                isStatusActive
                  ? 'bg-[#F0F7F1] text-[#16A34A] border border-[#C9DCCB]'
                  : 'bg-stone-100 text-stone-600 border border-stone-200'
              }`}
            >
              {isStatusActive ? (
                <CheckCircle2 className="h-3 w-3 mr-1" />
              ) : (
                <AlertCircle className="h-3 w-3 mr-1" />
              )}
              {isStatusActive ? t('farm.active', 'Active') : t('farm.inactive', 'Inactive')}
            </span>
          </div>

          <div className="space-y-2.5 border-t border-[#C9DCCB] pt-3">
            <div className="flex items-center text-sm font-semibold text-[#525252]">
              <MapPin className="h-4.5 w-4.5 text-[#6B7280] mr-2 shrink-0" />
              <span className="truncate text-[#222222] font-semibold">{farm?.location || ''}</span>
            </div>
            <div className="flex items-center text-sm font-semibold text-[#525252]">
              <Scale className="h-4.5 w-4.5 text-[#6B7280] mr-2 shrink-0" />
              <span className="text-[#222222] font-semibold">
                {formattedArea}
              </span>
            </div>
          </div>
        </div>

        <div className="mt-5 flex items-center justify-between text-[#087A55] font-black text-sm h-10 border-t border-[#C9DCCB] pt-3">
          <span className="group-hover:underline">{t('farm.actions.viewDetails', 'View Farm')}</span>
          <ChevronRight className="h-4 w-4 ml-1 transition-transform group-hover:translate-x-0.5" />
        </div>
      </div>
    </div>
  );
};

export default FarmCard;
