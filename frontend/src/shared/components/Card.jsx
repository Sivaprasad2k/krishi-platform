import React from 'react';

/**
 * Standard Card primitive component.
 */
export const Card = ({
  children,
  className = '',
  onClick,
  interactive = false,
  selected = false,
  ...props
}) => {
  return (
    <div
      onClick={onClick}
      className={`bg-white rounded-2xl border p-5 shadow-2xs transition-all ${
        selected
          ? 'border-[#087A55] bg-[#DCFCE7] ring-2 ring-[#087A55]/20'
          : 'border-[#C9DCCB]'
      } ${
        interactive || onClick
          ? 'cursor-pointer hover:border-[#087A55] hover:shadow-xs active:scale-[0.99]'
          : ''
      } ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};

/**
 * MetricCard primitive for displaying operational counts and statistics.
 */
export const MetricCard = ({
  title,
  value,
  icon: Icon,
  trend,
  semanticColor = 'default',
  onClick,
  className = '',
}) => {
  const valueColorMap = {
    default: 'text-[#222222]',
    primary: 'text-[#087A55]',
    warning: 'text-[#D97706]',
    danger: 'text-[#DC2626]',
    info: 'text-[#2563EB]',
  };

  return (
    <Card
      onClick={onClick}
      interactive={!!onClick}
      className={`flex flex-col justify-between min-h-[110px] ${className}`}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-black text-[#525252] uppercase tracking-wider truncate">
          {title}
        </span>
        {Icon && <Icon className="h-5 w-5 text-[#6B7280] shrink-0" />}
      </div>
      <div className="mt-2 flex items-baseline justify-between">
        <span className={`text-3xl font-black ${valueColorMap[semanticColor] || valueColorMap.default}`}>
          {value}
        </span>
        {trend && <span className="text-xs font-bold text-[#6B7280]">{trend}</span>}
      </div>
    </Card>
  );
};

/**
 * OperationalCard primitive for actionable domain entities (Work, Problems, Treatments).
 */
export const OperationalCard = ({
  children,
  borderAccent = 'primary',
  className = '',
  onClick,
  ...props
}) => {
  const borderAccentMap = {
    primary: 'border-l-4 border-l-[#087A55]',
    success: 'border-l-4 border-l-[#16A34A]',
    warning: 'border-l-4 border-l-[#D97706]',
    danger: 'border-l-4 border-l-[#DC2626]',
    info: 'border-l-4 border-l-[#2563EB]',
  };

  return (
    <Card
      onClick={onClick}
      interactive={!!onClick}
      className={`${borderAccentMap[borderAccent] || borderAccentMap.primary} ${className}`}
      {...props}
    >
      {children}
    </Card>
  );
};

export default Card;
