import React from 'react';
import { Sprout, Plus } from 'lucide-react';

/**
 * EmptyState presents a friendly, actionable zero-data message.
 */
export const EmptyState = ({
  icon: Icon = Sprout,
  title,
  description,
  actionLabel,
  onAction,
  className = ''
}) => {
  return (
    <div className={`rounded-3xl bg-white p-8 text-center border border-stone-200 shadow-xs space-y-4 max-w-md mx-auto my-6 ${className}`}>
      <div className="mx-auto w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
        <Icon className="h-8 w-8" />
      </div>
      
      <div className="space-y-1">
        <h3 className="text-xl font-black text-stone-850">{title}</h3>
        {description && (
          <p className="text-sm font-semibold text-stone-500 max-w-xs mx-auto leading-relaxed">
            {description}
          </p>
        )}
      </div>

      {actionLabel && onAction && (
        <div className="pt-2">
          <button
            onClick={onAction}
            style={{ minHeight: '48px' }}
            className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black shadow-md active:scale-95 transition-all cursor-pointer inline-flex items-center justify-center space-x-2"
          >
            <Plus className="h-5 w-5" />
            <span>{actionLabel}</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default EmptyState;
