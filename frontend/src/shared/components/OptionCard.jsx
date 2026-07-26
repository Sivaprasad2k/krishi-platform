import React from 'react';
import { Check } from 'lucide-react';

/**
 * OptionCard provides a prominent, high-contrast, touch-friendly visual choice card.
 * Designed for color-vision deficiency with border, background, icon treatment, and check indicator.
 */
const OptionCard = ({
  icon: Icon,
  title,
  description,
  selected = false,
  onClick,
  badgeText = null,
  className = '',
  disabled = false,
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      role="radio"
      aria-checked={selected}
      style={{ minHeight: '56px' }}
      className={`w-full text-left p-3.5 rounded-2xl border-2 transition-all cursor-pointer flex items-start space-x-3.5 relative ${
        selected
          ? 'border-[#087A55] bg-[#CDEBD6] ring-2 ring-[#087A55]/20 shadow-2xs'
          : 'border-[#C5DCC9] hover:border-[#A8CBB0] bg-white hover:bg-[#D7EDDB]'
      } ${disabled ? 'opacity-50 cursor-not-allowed' : 'active:scale-[0.98]'} ${className}`}
    >
      {Icon && (
        <div
          className={`p-2.5 rounded-xl shrink-0 transition-colors ${
            selected ? 'bg-[#087A55] text-white' : 'bg-[#D7EDDB] text-[#222222] border border-[#C5DCC9]'
          }`}
        >
          <Icon className="h-5 w-5" />
        </div>
      )}

      <div className="flex-1 min-w-0 pr-6">
        <div className="flex items-center space-x-2">
          <span className={`text-sm font-black truncate leading-snug ${selected ? 'text-[#14532D]' : 'text-[#222222]'}`}>
            {title}
          </span>
          {badgeText && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-[#087A55] text-white">
              {badgeText}
            </span>
          )}
        </div>
        {description && (
          <p className="text-xs font-semibold text-[#525252] mt-0.5 leading-normal">
            {description}
          </p>
        )}
      </div>

      {/* Selected indicator badge */}
      {selected && (
        <div className="absolute top-3 right-3 h-5 w-5 rounded-full bg-[#087A55] text-white flex items-center justify-center shadow-xs shrink-0">
          <Check className="h-3.5 w-3.5 stroke-[3]" />
        </div>
      )}
    </button>
  );
};

export default OptionCard;
