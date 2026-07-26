import React, { useEffect } from 'react';
import { X } from 'lucide-react';

/**
 * Standardized Modal primitive component.
 * Prevents background body scrolling, handles z-index, and provides sticky action footers.
 */
export const Modal = ({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
  footer,
  maxWidth = 'max-w-xl',
  className = '',
}) => {
  // Lock body scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  // Handle ESC key press
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div
        className={`relative w-full ${maxWidth} bg-white rounded-3xl shadow-2xl border border-[#C9DCCB] overflow-hidden flex flex-col max-h-[90vh] ${className}`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Sticky Header */}
        <div className="flex items-center justify-between p-5 border-b border-[#C9DCCB] bg-white sticky top-0 z-10 shrink-0">
          <div>
            <h3 className="text-lg font-black text-[#222222]">{title}</h3>
            {subtitle && <p className="text-xs font-semibold text-[#6B7280] mt-0.5">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full text-[#6B7280] hover:text-[#222222] hover:bg-[#E3F2E6] transition-colors cursor-pointer min-h-[40px] min-w-[40px] flex items-center justify-center"
            aria-label="Close dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="p-5 overflow-y-auto flex-1 space-y-4 text-[#222222]">
          {children}
        </div>

        {/* Sticky Action Footer */}
        {footer && (
          <div className="p-4 border-t border-[#C9DCCB] bg-white sticky bottom-0 z-10 shrink-0">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};

export default Modal;
