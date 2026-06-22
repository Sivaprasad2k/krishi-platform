import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useTranslation } from 'react-i18next';
import { Home, Sprout, CheckSquare, Bell, User, Globe } from 'lucide-react';

const MainLayout = () => {
  const { user } = useAuthStore();
  const { i18n, t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const changeLanguage = (e) => {
    i18n.changeLanguage(e.target.value);
  };

  const currentPath = location.pathname;

  // Nav Items Configuration (Hiding backend technical jargon)
  const navItems = [
    { path: '/dashboard', label: t('nav.home', 'Home'), icon: Home },
    { path: '/crops', label: t('nav.crops', 'My Crops'), icon: Sprout },
    { path: '/tasks', label: t('nav.tasks', 'Work To Do'), icon: CheckSquare },
    { path: '/alerts', label: t('nav.alerts', 'Alerts'), icon: Bell },
    { path: '/profile', label: t('nav.profile', 'Profile'), icon: User }
  ];

  const getPageTitle = () => {
    const matched = navItems.find((item) => item.path === currentPath);
    return matched ? matched.label : '';
  };

  return (
    <div className="flex h-screen w-full flex-col bg-gray-50 md:flex-row font-sans">
      {/* Sidebar - Desktop Only */}
      <aside className="hidden w-64 flex-col border-r border-gray-250 bg-white p-4 md:flex">
        <div className="mb-8 flex items-center space-x-2 px-2">
          <span className="text-3xl font-black text-emerald-700 tracking-tight">KRISHI</span>
        </div>
        
        <nav className="flex-1 space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentPath === item.path;
            return (
              <button
                key={item.path}
                onClick={() => navigate(item.path)}
                className={`flex w-full items-center space-x-3.5 rounded-xl px-4 text-base font-bold transition-all h-12 cursor-pointer ${
                  isActive
                    ? 'bg-emerald-50 text-emerald-750'
                    : 'text-gray-600 hover:bg-gray-55 hover:text-gray-900'
                }`}
                style={{ minHeight: '48px' }}
              >
                <Icon className="h-5.5 w-5.5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* User profile footer info - Desktop */}
        <div className="border-t border-gray-200 pt-4">
          <div className="px-4 py-2">
            <p className="text-base font-bold text-gray-800 truncate">{user?.fullName || 'Farmer'}</p>
            <p className="text-sm text-gray-500">{user?.phoneNumber || ''}</p>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top Header Bar */}
        <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-4 md:px-8">
          {/* Logo visible on mobile */}
          <div className="flex items-center space-x-2 md:hidden">
            <span className="text-2xl font-black text-emerald-700 tracking-tight">KRISHI</span>
          </div>

          <div className="hidden md:block">
            <h2 className="text-xl font-bold text-gray-800">
              {getPageTitle()}
            </h2>
          </div>

          <div className="flex items-center space-x-4">
            {/* Quick Header Language Switcher */}
            <div className="flex items-center space-x-1 border border-gray-200 rounded-xl bg-gray-50 px-2 h-10">
              <Globe className="h-4 w-4 text-gray-500" />
              <select
                value={i18n.language}
                onChange={changeLanguage}
                className="bg-transparent text-sm font-bold text-gray-700 focus:outline-none cursor-pointer"
                aria-label="Select Language"
              >
                <option value="en">EN</option>
                <option value="ta">தமிழ்</option>
                <option value="ml">മല</option>
              </select>
            </div>

            {/* Quick Access to Alerts Notification icon */}
            <button
              onClick={() => navigate('/alerts')}
              className="relative p-2.5 rounded-full hover:bg-gray-100 text-gray-500 hover:text-gray-700 min-h-[40px] flex items-center justify-center cursor-pointer"
              aria-label="View Alerts"
            >
              <Bell className="h-5.5 w-5.5" />
              <span className="absolute top-1 right-1 h-2.5 w-2.5 rounded-full bg-red-500 border border-white"></span>
            </button>
          </div>
        </header>

        {/* Content Outlet - scrollable container */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 pb-24 md:pb-8">
          <div className="max-w-5xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Bottom Nav Bar - Mobile Only */}
      <nav className="fixed bottom-0 left-0 right-0 flex h-18 border-t border-gray-200 bg-white md:hidden z-50 shadow-lg">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentPath === item.path;
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`flex flex-1 flex-col items-center justify-center space-y-1 transition-all h-full cursor-pointer ${
                isActive ? 'text-emerald-700 font-bold' : 'text-gray-500'
              }`}
              style={{ minHeight: '48px' }}
            >
              <Icon className="h-5.5 w-5.5" />
              <span className="text-2xs font-semibold tracking-wide">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};

export default MainLayout;
