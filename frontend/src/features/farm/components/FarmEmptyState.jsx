import { Plus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

/**
 * FarmEmptyState prompts the user to create their first Farm.
 */
const FarmEmptyState = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  return (
    <div className="flex min-h-[400px] w-full flex-col items-center justify-center rounded-2xl border border-dashed border-emerald-350 bg-white p-8 text-center shadow-sm">
      <div className="rounded-full bg-emerald-50 p-5 text-emerald-600 mb-5 animate-pulse">
        <Plus className="h-12 w-12" />
      </div>
      <h3 className="text-2xl font-black text-gray-800 mb-2 leading-tight">
        {t('farm.empty.welcome', 'Welcome to Krishi')}
      </h3>
      <p className="text-base font-semibold text-gray-500 max-w-sm mb-8">
        {t('farm.empty.description', "Let's create your first farm.")}
      </p>
      <button
        onClick={() => navigate('/farms/new')}
        style={{ minHeight: '48px' }}
        className="flex items-center justify-center rounded-xl bg-emerald-600 px-7 py-3 text-base font-bold text-white shadow-md hover:bg-emerald-700 active:scale-95 transition-all focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 cursor-pointer"
      >
        <Plus className="h-5 w-5 mr-1.5" />
        <span>{t('farm.create', 'Create Farm')}</span>
      </button>
    </div>
  );
};

export default FarmEmptyState;
