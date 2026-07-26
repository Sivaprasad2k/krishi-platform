import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * FarmHeader is a clean header element that supports breadcrumb/back controls.
 */
const FarmHeader = ({ title, subtitle, onBack, rightElement }) => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl border border-gray-250 shadow-sm">
      <div className="flex items-center space-x-3.5">
        {onBack && (
          <button
            onClick={onBack}
            style={{ minHeight: '44px', minWidth: '44px' }}
            className="flex items-center justify-center rounded-xl border border-gray-200 hover:bg-gray-50 active:scale-95 transition-all text-gray-650 cursor-pointer"
            aria-label="Go Back"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
        )}
        <div>
          <h1 className="text-2xl font-black text-gray-905 leading-tight">{title}</h1>
          {subtitle && <p className="text-sm font-semibold text-gray-500 mt-0.5">{subtitle}</p>}
        </div>
      </div>
      {rightElement && <div className="flex items-center gap-2">{rightElement}</div>}
    </div>
  );
};

export default FarmHeader;
