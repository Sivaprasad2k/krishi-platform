import React from 'react';

/**
 * Standardized PageHeader component.
 */
export const PageHeader = ({
  title,
  subtitle,
  action,
  icon: Icon,
  className = '',
}) => {
  return (
    <div className={`flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-6 bg-white rounded-2xl border border-[#C9DCCB] shadow-2xs ${className}`}>
      <div className="flex items-start space-x-3.5 min-w-0">
        {Icon && (
          <div className="p-3 rounded-xl bg-[#E3F2E6] text-[#087A55] shrink-0 mt-0.5 border border-[#C9DCCB]">
            <Icon className="h-6 w-6" />
          </div>
        )}
        <div className="min-w-0">
          <h1 className="text-2xl md:text-3xl font-black text-[#222222] tracking-tight truncate">
            {title}
          </h1>
          {subtitle && (
            <p className="text-sm font-semibold text-[#525252] mt-0.5 truncate">
              {subtitle}
            </p>
          )}
        </div>
      </div>
      {action && <div className="shrink-0 flex items-center">{action}</div>}
    </div>
  );
};

/**
 * Standardized SectionHeader component.
 */
export const SectionHeader = ({
  title,
  subtitle,
  icon: Icon,
  action,
  className = '',
}) => {
  return (
    <div className={`flex items-center justify-between pb-2 border-b border-[#C9DCCB] ${className}`}>
      <div className="flex items-center space-x-2 min-w-0">
        {Icon && <Icon className="h-5 w-5 text-[#087A55] shrink-0" />}
        <h2 className="text-lg md:text-xl font-black text-[#222222] truncate">
          {title}
        </h2>
        {subtitle && <span className="text-xs font-semibold text-[#6B7280] hidden sm:inline ml-2">{subtitle}</span>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
};

export default PageHeader;
