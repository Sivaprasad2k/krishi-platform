import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LogIn, UserPlus } from 'lucide-react';

const AuthTabs = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();

  const isRegister = location.pathname.includes('/register');

  return (
    <div className="grid grid-cols-2 gap-1 p-1 bg-stone-100/90 rounded-2xl border border-stone-200/80 mb-6 w-full">
      <button
        type="button"
        onClick={() => navigate('/login')}
        className={`flex items-center justify-center space-x-2 py-3 px-4 rounded-xl text-base font-extrabold transition-all cursor-pointer min-h-[48px] ${
          !isRegister
            ? 'bg-emerald-600 text-white shadow-sm border border-emerald-700'
            : 'text-stone-600 hover:text-stone-900 hover:bg-stone-200/60'
        }`}
        aria-selected={!isRegister}
      >
        <LogIn className="h-5 w-5 shrink-0" />
        <span>{t('auth.login', 'Login')}</span>
      </button>

      <button
        type="button"
        onClick={() => navigate('/register')}
        className={`flex items-center justify-center space-x-2 py-3 px-4 rounded-xl text-base font-extrabold transition-all cursor-pointer min-h-[48px] ${
          isRegister
            ? 'bg-emerald-600 text-white shadow-sm border border-emerald-700'
            : 'text-stone-600 hover:text-stone-900 hover:bg-stone-200/60'
        }`}
        aria-selected={isRegister}
      >
        <UserPlus className="h-5 w-5 shrink-0" />
        <span>{t('auth.register', 'Register')}</span>
      </button>
    </div>
  );
};

export default AuthTabs;
