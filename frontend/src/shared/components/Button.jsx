import React from 'react';
import { Loader2 } from 'lucide-react';

/**
 * Standardized Button primitive component for Krishi UI.
 * Enforces >= 48px touch targets for mobile accessibility.
 */
export const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  icon: Icon,
  iconPosition = 'left',
  fullWidth = false,
  className = '',
  style = {},
  type = 'button',
  onClick,
  ...props
}) => {
  const baseClasses =
    'inline-flex items-center justify-center font-extrabold rounded-xl transition-all duration-150 select-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]';

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-xs min-h-[38px]',
    md: 'px-4 py-2.5 text-sm min-h-[48px]',
    lg: 'px-6 py-3.5 text-base min-h-[52px]',
  };

  const variantClasses = {
    primary:
      'bg-[#087A55] hover:bg-[#066747] text-white focus:ring-[#087A55] shadow-xs border border-transparent',
    secondary:
      'bg-white hover:bg-[#E3F2E6] text-[#222222] border border-[#C9DCCB] focus:ring-[#087A55] shadow-2xs',
    danger:
      'bg-[#DC2626] hover:bg-[#B91C1C] text-white focus:ring-[#DC2626] shadow-xs border border-transparent',
    ghost:
      'bg-transparent hover:bg-[#E3F2E6] text-[#222222] focus:ring-[#087A55] border border-transparent',
  };

  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      style={{ minHeight: size === 'sm' ? '38px' : '48px', ...style }}
      className={`${baseClasses} ${sizeClasses[size] || sizeClasses.md} ${
        variantClasses[variant] || variantClasses.primary
      } ${widthClass} ${className}`}
      {...props}
    >
      {loading ? (
        <Loader2 className="h-4 w-4 animate-spin shrink-0 mr-2" />
      ) : (
        Icon && iconPosition === 'left' && <Icon className="h-4 w-4 shrink-0 mr-2" />
      )}

      <span>{children}</span>

      {!loading && Icon && iconPosition === 'right' && (
        <Icon className="h-4 w-4 shrink-0 ml-2" />
      )}
    </button>
  );
};

export const IconButton = ({
  icon: Icon,
  label,
  variant = 'secondary',
  size = 'md',
  className = '',
  ...props
}) => {
  return (
    <Button
      variant={variant}
      size={size}
      aria-label={label}
      title={label}
      className={`p-2.5 rounded-full !min-w-[44px] !min-h-[44px] ${className}`}
      {...props}
    >
      {Icon && <Icon className="h-5 w-5 shrink-0" />}
    </Button>
  );
};

export default Button;
