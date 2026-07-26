import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Sprout } from 'lucide-react';
import AuthLanguageSelector from '../../features/auth/components/AuthLanguageSelector';
import AuthBrandPanel from '../../features/auth/components/AuthBrandPanel';
import AuthTabs from '../../features/auth/components/AuthTabs';

/**
 * Responsive AuthLayout shell supporting two-zone composition on desktop
 * and single-column composition on mobile with a pre-auth language bar.
 */
const AuthLayout = () => {
  const { t } = useTranslation();

  return (
    <div className="min-h-screen w-full bg-stone-50/90 text-stone-900 flex flex-col justify-between relative selection:bg-emerald-200 selection:text-emerald-900">
      {/* Subtle background decoration */}
      <div className="absolute inset-0 bg-[radial-gradient(#059669_1px,transparent_1px)] [background-size:24px_24px] opacity-[0.03] pointer-events-none" />

      {/* Top Header: Pre-Auth Language Selector */}
      <header className="w-full pt-6 pb-2 px-4 relative z-10">
        <AuthLanguageSelector />
      </header>

      {/* Central Content Area */}
      <main className="w-full max-w-6xl mx-auto px-4 py-6 sm:py-8 flex-1 flex items-center justify-center relative z-10">
        <div className="w-full flex flex-col lg:flex-row items-center lg:items-stretch justify-between gap-8 lg:gap-10">
          
          {/* Desktop Left Brand Panel */}
          <AuthBrandPanel />

          {/* Mobile Top Brand Header */}
          <div className="lg:hidden flex flex-col items-center text-center space-y-2 select-none">
            <div className="flex items-center space-x-2.5">
              <div className="h-10 w-10 rounded-xl bg-emerald-600 flex items-center justify-center text-white shadow-xs">
                <Sprout className="h-6 w-6" />
              </div>
              <span className="text-3xl font-black tracking-tight text-emerald-800">
                KRISHI
              </span>
            </div>
            <p className="text-sm font-bold text-stone-600 max-w-xs">
              {t('auth.tagline', 'Manage your farm, grow with confidence.')}
            </p>
          </div>

          {/* Primary Authentication Card (Right / Central Area) */}
          <div className="w-full max-w-md lg:w-7/12 xl:w-1/2 bg-white rounded-3xl p-6 sm:p-8 shadow-md border border-stone-200/90 flex flex-col justify-between">
            <div>
              {/* Segmented Auth Tabs */}
              <AuthTabs />

              {/* Dynamic Auth Form Outlet (LoginPage or RegisterPage) */}
              <Outlet />
            </div>
          </div>
        </div>
      </main>

      {/* Footer Disclaimer */}
      <footer className="w-full py-4 text-center text-xs font-semibold text-stone-500 relative z-10">
        <p>{t('auth.termsDisclaimer', 'By continuing, you agree to our Terms of Service and Privacy Policy.')}</p>
      </footer>
    </div>
  );
};

export default AuthLayout;
