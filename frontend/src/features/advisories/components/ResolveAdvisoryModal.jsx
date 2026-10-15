import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle2, X, AlertTriangle } from 'lucide-react';

/**
 * ResolveAdvisoryModal
 * Prompts the farmer to confirm manual resolution of an agricultural advisory.
 */
export default function ResolveAdvisoryModal({
  isOpen,
  onClose,
  onConfirm,
  advisory,
  isResolving = false,
}) {
  const { t } = useTranslation();

  // Keyboard accessibility: Escape closes modal
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen && !isResolving) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isResolving, onClose]);

  if (!isOpen || !advisory) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
      aria-labelledby="resolve-modal-title"
    >
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-[#C9DCCB] space-y-4">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-emerald-50 text-[#16A34A] rounded-xl border border-emerald-200">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <div>
              <h3 id="resolve-modal-title" className="text-base font-black text-[#222222]">
                {t('advisories.resolve.title', 'Resolve Agricultural Advisory')}
              </h3>
              <p className="text-xs text-gray-500">{advisory.ruleCode}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isResolving}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition cursor-pointer"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Advisory Context Summary */}
        <div className="p-3.5 bg-gray-50 rounded-xl border border-gray-200 text-xs space-y-1.5">
          <span className="font-bold text-gray-800 block">{advisory.title}</span>
          <p className="text-gray-600 leading-relaxed">{advisory.message}</p>
          {advisory.recommendedAction && (
            <div className="pt-1 text-[11px] font-semibold text-emerald-800">
              <span>{t('advisories.card.action', 'Action')}: </span>
              <span>{advisory.recommendedAction}</span>
            </div>
          )}
        </div>

        <p className="text-xs text-gray-600 leading-normal">
          {t(
            'advisories.resolve.prompt',
            'Confirming resolution marks this advisory as addressed. If field conditions reoccur or new weather alerts emerge, the advisory engine will re-evaluate on the next cycle.'
          )}
        </p>

        {/* Buttons */}
        <div className="flex items-center justify-end space-x-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={isResolving}
            className="px-4 py-2 rounded-xl border border-gray-300 text-xs font-black text-gray-700 hover:bg-gray-50 transition cursor-pointer"
          >
            {t('common.actions.cancel', 'Cancel')}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isResolving}
            className="flex items-center space-x-1.5 px-4 py-2 rounded-xl bg-[#16A34A] hover:bg-[#15803D] text-white text-xs font-black shadow-2xs transition active:scale-95 disabled:opacity-50 cursor-pointer"
          >
            {isResolving ? (
              <span>{t('advisories.resolve.resolving', 'Resolving...')}</span>
            ) : (
              <>
                <CheckCircle2 className="w-4 h-4" />
                <span>{t('advisories.resolve.confirm', 'Mark Resolved')}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
