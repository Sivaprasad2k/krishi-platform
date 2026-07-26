import React from 'react';
import { useTranslation } from 'react-i18next';

export const CropTypeSelector = ({ value, onChange }) => {
  const { t } = useTranslation();

  const options = [
    { id: 'Paddy', icon: '🌾', label: t('crops.types.paddy', 'Paddy') },
    { id: 'Banana', icon: '🌱', label: t('crops.types.banana', 'Banana') },
    { id: 'Coconut', icon: '🥥', label: t('crops.types.coconut', 'Coconut') },
    { id: 'Tomato', icon: '🍅', label: t('crops.types.tomato', 'Tomato') },
    { id: 'Other', icon: '🌿', label: t('crops.types.other', 'Other') }
  ];

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
      {options.map((opt) => {
        const isSelected = value === opt.id;
        return (
          <button
            key={opt.id}
            type="button"
            onClick={() => onChange(opt.id)}
            style={{ minHeight: '80px' }}
            className={`flex flex-col items-center justify-center p-4 rounded-2xl border-2 transition-all text-center focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
              isSelected
                ? 'border-emerald-600 bg-emerald-50 text-emerald-800 shadow-sm'
                : 'border-gray-200 bg-white text-gray-700 hover:border-emerald-350'
            }`}
          >
            <span className="text-3xl mb-1.5" role="img" aria-label={opt.label}>
              {opt.icon}
            </span>
            <span className="text-sm font-extrabold">{opt.label}</span>
          </button>
        );
      })}
    </div>
  );
};

export default CropTypeSelector;
