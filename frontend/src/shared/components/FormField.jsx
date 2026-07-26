import React from 'react';
import { AlertCircle } from 'lucide-react';

/**
 * Standardized Input field primitive.
 */
export const Input = React.forwardRef(
  ({ error = false, className = '', style = {}, ...props }, ref) => {
    return (
      <input
        ref={ref}
        style={{ minHeight: '48px', ...style }}
        className={`w-full px-4 py-3 rounded-xl border text-sm font-semibold transition-all outline-hidden bg-white text-[#222222] placeholder:text-[#9CA3AF] ${
          error
            ? 'border-[#DC2626] focus:border-[#DC2626] focus:ring-2 focus:ring-[#DC2626]/20'
            : 'border-[#C9DCCB] focus:border-[#087A55] focus:ring-2 focus:ring-[#087A55]/20'
        } disabled:bg-[#F0F7F1] disabled:text-[#9CA3AF] disabled:cursor-not-allowed ${className}`}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';

/**
 * Standardized Textarea primitive.
 */
export const Textarea = React.forwardRef(
  ({ error = false, rows = 3, className = '', ...props }, ref) => {
    return (
      <textarea
        ref={ref}
        rows={rows}
        className={`w-full px-4 py-3 rounded-xl border text-sm font-semibold transition-all outline-hidden bg-white text-[#222222] placeholder:text-[#9CA3AF] ${
          error
            ? 'border-[#DC2626] focus:border-[#DC2626] focus:ring-2 focus:ring-[#DC2626]/20'
            : 'border-[#C9DCCB] focus:border-[#087A55] focus:ring-2 focus:ring-[#087A55]/20'
        } disabled:bg-[#F0F7F1] disabled:text-[#9CA3AF] disabled:cursor-not-allowed ${className}`}
        {...props}
      />
    );
  }
);

Textarea.displayName = 'Textarea';

/**
 * Standardized Select / Dropdown primitive.
 * Designed to handle long Tamil, Malayalam, and Hindi labels gracefully.
 */
export const Select = React.forwardRef(
  ({ error = false, options = [], children, className = '', style = {}, ...props }, ref) => {
    return (
      <select
        ref={ref}
        style={{ minHeight: '48px', ...style }}
        className={`w-full px-4 py-3 rounded-xl border text-sm font-bold transition-all outline-hidden bg-white text-[#222222] cursor-pointer appearance-none ${
          error
            ? 'border-[#DC2626] focus:border-[#DC2626] focus:ring-2 focus:ring-[#DC2626]/20'
            : 'border-[#C9DCCB] focus:border-[#087A55] focus:ring-2 focus:ring-[#087A55]/20'
        } disabled:bg-[#F0F7F1] disabled:text-[#9CA3AF] disabled:cursor-not-allowed ${className}`}
        {...props}
      >
        {children ||
          options.map((opt) => (
            <option key={opt.value} value={opt.value} className="py-2 text-[#222222] font-semibold">
              {opt.label}
            </option>
          ))}
      </select>
    );
  }
);

Select.displayName = 'Select';

/**
 * Standardized ErrorMessage primitive.
 */
export const ErrorMessage = ({ message, className = '' }) => {
  if (!message) return null;
  return (
    <div className={`flex items-center space-x-1.5 text-xs font-bold text-[#DC2626] mt-1.5 ${className}`}>
      <AlertCircle className="h-3.5 w-3.5 shrink-0" />
      <span>{message}</span>
    </div>
  );
};

/**
 * FormField wrapper binding label, control, helper text, and validation error.
 */
export const FormField = ({
  label,
  required = false,
  error,
  helperText,
  children,
  className = '',
}) => {
  return (
    <div className={`space-y-1.5 ${className}`}>
      {label && (
        <label className="block text-xs font-black text-[#222222] uppercase tracking-wider">
          {label} {required && <span className="text-[#DC2626]">*</span>}
        </label>
      )}
      {children}
      {helperText && !error && (
        <p className="text-xs font-semibold text-[#6B7280] mt-1">{helperText}</p>
      )}
      <ErrorMessage message={error} />
    </div>
  );
};

export default FormField;
