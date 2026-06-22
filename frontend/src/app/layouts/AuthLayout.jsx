import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';

/**
 * Layout wrapper for public authentication pages (Login, Register).
 * Adapts screen space, centering content, and provides a language selector.
 */
const AuthLayout = () => {
  const { i18n } = useTranslation();

  const changeLanguage = (e) => {
    i18n.changeLanguage(e.target.value);
  };

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center bg-gray-50 px-4 py-8 relative">
      {/* Top right language selector for accessibility before login */}
      <div className="absolute top-4 right-4 flex items-center space-x-2 rounded-xl border border-gray-250 bg-white px-3 shadow-sm h-12">
        <Globe className="h-5 w-5 text-gray-500" />
        <select
          value={i18n.language}
          onChange={changeLanguage}
          className="bg-transparent text-base font-bold text-gray-700 focus:outline-none cursor-pointer h-full"
          style={{ minHeight: '48px' }}
          aria-label="Select Language"
        >
          <option value="en">English</option>
          <option value="ta">தமிழ்</option>
          <option value="ml">മലയാളം</option>
        </select>
      </div>

      <div className="w-full max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-sm border border-gray-200 mt-12 animate-fade-in">
        <div className="flex flex-col items-center text-center">
          <h1 className="text-4xl font-black text-emerald-700 tracking-tight">KRISHI</h1>
          <p className="text-base font-medium text-gray-500 mt-1">Operational Farm Management Platform</p>
        </div>
        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout;
