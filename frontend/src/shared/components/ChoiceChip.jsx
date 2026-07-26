import React from 'react';

/**
 * ChoiceChip provides a large, touch-friendly chip for quick selections and suggestions.
 * Enforces minimum 48px height for accessibility on mobile screens.
 */
const ChoiceChip = ({
  label,
  icon: Icon = null,
  selected = false,
  onClick,
  className = '',
  size = 'md', // 'sm', 'md', 'lg'
}) => {
  const minHeight = size === 'sm' ? '40px' : '48px';

  return (
    <button
      type="button"
      onClick={onClick}
      style={{ minHeight }}
      className={`px-4 py-2.5 rounded-xl border text-xs font-black transition-all cursor-pointer flex items-center justify-center space-x-2 shrink-0 ${
        selected
          ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs ring-2 ring-emerald-500/20 font-black'
          : 'bg-white text-gray-750 border-gray-250 hover:bg-gray-50 hover:border-gray-300'
      } active:scale-95 ${className}`}
    >
      {Icon && <Icon className={`h-4 w-4 ${selected ? 'text-white' : 'text-emerald-600'}`} />}
      <span className="truncate">{label}</span>
    </button>
  );
};

export default ChoiceChip;
