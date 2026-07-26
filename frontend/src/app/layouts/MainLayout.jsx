import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useTranslation } from 'react-i18next';
import { Home, Map, Sprout, CheckSquare, Bell, User, Globe, MoreHorizontal, ShieldAlert, HeartPulse, X } from 'lucide-react';
import { useState, useEffect } from 'react';
import apiClient from '../../core/api/axios';

const MainLayout = () => {
  const { user } = useAuthStore();
  const { i18n, t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const [isMoreOpen, setIsMoreOpen] = useState(false);
  const [counts, setCounts] = useState({
    farms: 0,
    crops: 0,
    tasks: 0,
    alerts: 0
  });

  const [namesCache, setNamesCache] = useState({
    farms: {},
    seasons: {},
    crops: {},
    seasonToFarm: {},
    cropToSeason: {}
  });

  const fetchCounts = async () => {
    try {
      const res = await apiClient.get('/dashboard/summary');
      if (res.data?.success && res.data?.data) {
        const d = res.data.data;
        setCounts({
          farms: d.farmCount || 0,
          crops: d.activeCropCycles || 0,
          tasks: d.pendingTasks || 0,
          alerts: d.unreadNotifications || 0
        });
      }
    } catch (err) {
      console.warn('Failed to fetch dashboard counts:', err);
    }
  };

  useEffect(() => {
    fetchCounts();
    const interval = setInterval(fetchCounts, 15000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const resolveNames = async () => {
      const path = location.pathname;
      
      const cropMatch = path.match(/\/crops\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);
      const cropNewMatch = path.match(/\/crop-cycles\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/crops\/new/i);
      const seasonMatch = path.match(/\/seasons\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);
      const farmSeasonNewMatch = path.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/seasons\/new/i);
      const farmSeasonsMatch = path.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/seasons/i);
      const farmMatch = path.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);

      let cropId = cropMatch ? cropMatch[1] : null;
      let seasonId = seasonMatch ? seasonMatch[1] : (cropNewMatch ? cropNewMatch[1] : null);
      let farmId = farmMatch ? farmMatch[1] : (farmSeasonNewMatch ? farmSeasonNewMatch[1] : (farmSeasonsMatch ? farmSeasonsMatch[1] : null));

      try {
        if (cropId && !namesCache.crops[cropId]) {
          const res = await apiClient.get(`/crop-plantings/${cropId}`);
          if (res.data?.success && res.data?.data) {
            const cropData = res.data.data;
            const sId = cropData.cropCycleId?.toString();
            setNamesCache(prev => ({
              ...prev,
              crops: { ...prev.crops, [cropId]: cropData.cropName },
              cropToSeason: { ...prev.cropToSeason, [cropId]: sId }
            }));
            if (sId) {
              seasonId = sId;
            }
          }
        }

        if (seasonId && !namesCache.seasons[seasonId]) {
          const res = await apiClient.get(`/crop-cycles/${seasonId}`);
          if (res.data?.success && res.data?.data) {
            const seasonData = res.data.data;
            const fId = seasonData.farmId?.toString();
            setNamesCache(prev => ({
              ...prev,
              seasons: { ...prev.seasons, [seasonId]: seasonData.name },
              seasonToFarm: { ...prev.seasonToFarm, [seasonId]: fId }
            }));
            if (fId) {
              farmId = fId;
            }
          }
        }

        if (farmId && !namesCache.farms[farmId]) {
          const res = await apiClient.get(`/farms/${farmId}`);
          if (res.data?.success && res.data?.data) {
            const farmData = res.data.data;
            setNamesCache(prev => ({
              ...prev,
              farms: { ...prev.farms, [farmId]: farmData.name }
            }));
          }
        }
      } catch (err) {
        console.warn('Failed to resolve breadcrumb names:', err);
      }
    };

    resolveNames();
  }, [location.pathname]);

  const changeLanguage = (langCode) => {
    i18n.changeLanguage(langCode);
  };

  const currentPath = location.pathname;

  // Desktop Navigation Items
  const desktopNavItems = [
    { path: '/dashboard', label: t('nav.home', 'Home'), icon: Home },
    { path: '/farms', label: t('nav.farms', 'Farms'), icon: Map },
    { path: '/crops', label: t('nav.crops', 'Crops'), icon: Sprout },
    { path: '/tasks', label: t('nav.tasks', 'Work'), icon: CheckSquare },
    { path: '/disease-incidents', label: t('nav.problems', 'Crop Problems'), icon: ShieldAlert },
    { path: '/treatments', label: t('nav.treatments', 'Treatments'), icon: HeartPulse },
    { path: '/alerts', label: t('nav.alerts', 'Alerts'), icon: Bell },
    { path: '/profile', label: t('nav.profile', 'Profile'), icon: User }
  ];

  // Mobile Bottom Bar Primary Items (5 items: Home, Farms, Crops, Work, More)
  const mobileNavItems = [
    { path: '/dashboard', label: t('nav.home', 'Home'), icon: Home },
    { path: '/farms', label: t('nav.farms', 'Farms'), icon: Map },
    { path: '/crops', label: t('nav.crops', 'Crops'), icon: Sprout },
    { path: '/tasks', label: t('nav.tasks', 'Work'), icon: CheckSquare },
  ];

  const getBreadcrumbs = () => {
    const crumbs = [{ label: t('nav.home', 'Home'), path: '/dashboard' }];

    const cropMatch = currentPath.match(/\/crops\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);
    const cropNewMatch = currentPath.match(/\/crop-cycles\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/crops\/new/i);
    const seasonMatch = currentPath.match(/\/seasons\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);
    const farmSeasonNewMatch = currentPath.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/seasons\/new/i);
    const farmSeasonsMatch = currentPath.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/seasons/i);
    const farmMatch = currentPath.match(/\/farms\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);

    const cropId = cropMatch ? cropMatch[1] : null;
    const seasonId = seasonMatch ? seasonMatch[1] : (cropNewMatch ? cropNewMatch[1] : null);
    const farmId = farmMatch ? farmMatch[1] : (farmSeasonNewMatch ? farmSeasonNewMatch[1] : (farmSeasonsMatch ? farmSeasonsMatch[1] : null));

    if (currentPath.startsWith('/farms')) {
      crumbs.push({ label: t('nav.farms', 'Farms'), path: '/farms' });
      if (currentPath === '/farms/new') {
        crumbs.push({ label: t('farm.create', 'Create Farm'), path: '/farms/new' });
      } else if (farmId) {
        const farmName = namesCache.farms[farmId] || '...';
        crumbs.push({ label: farmName, path: `/farms/${farmId}` });
        
        if (currentPath.endsWith('/edit')) {
          crumbs.push({ label: t('farm.edit', 'Edit'), path: `/farms/${farmId}/edit` });
        } else if (currentPath.includes('/seasons')) {
          crumbs.push({ label: t('season.title', 'Seasons'), path: `/farms/${farmId}` });
          if (currentPath.endsWith('/new')) {
            crumbs.push({ label: t('season.create', 'Start Season'), path: `/farms/${farmId}/seasons/new` });
          }
        }
      }
    } else if (currentPath.startsWith('/seasons') && seasonId) {
      crumbs.push({ label: t('nav.farms', 'Farms'), path: '/farms' });
      const resolvedFarmId = namesCache.seasonToFarm?.[seasonId];
      if (resolvedFarmId) {
        const farmName = namesCache.farms[resolvedFarmId] || '...';
        crumbs.push({ label: farmName, path: `/farms/${resolvedFarmId}` });
      }
      const seasonName = namesCache.seasons[seasonId] || '...';
      crumbs.push({ label: seasonName, path: `/seasons/${seasonId}` });
    } else if (currentPath.startsWith('/crop-cycles') && cropNewMatch && seasonId) {
      crumbs.push({ label: t('nav.farms', 'Farms'), path: '/farms' });
      const resolvedFarmId = namesCache.seasonToFarm?.[seasonId];
      if (resolvedFarmId) {
        const farmName = namesCache.farms[resolvedFarmId] || '...';
        crumbs.push({ label: farmName, path: `/farms/${resolvedFarmId}` });
      }
      const seasonName = namesCache.seasons[seasonId] || '...';
      crumbs.push({ label: seasonName, path: `/seasons/${seasonId}` });
      crumbs.push({ label: t('crops.wizard.actions.planting', 'Plant Crop'), path: currentPath });
    } else if (currentPath.startsWith('/crops')) {
      if (currentPath === '/crops') {
        crumbs.push({ label: t('nav.crops', 'Crops'), path: '/crops' });
      } else if (cropId) {
        crumbs.push({ label: t('nav.farms', 'Farms'), path: '/farms' });
        
        const resolvedSeasonId = namesCache.cropToSeason?.[cropId];
        const resolvedFarmId = resolvedSeasonId ? namesCache.seasonToFarm?.[resolvedSeasonId] : null;
        
        if (resolvedFarmId) {
          const farmName = namesCache.farms[resolvedFarmId] || '...';
          crumbs.push({ label: farmName, path: `/farms/${resolvedFarmId}` });
        }
        if (resolvedSeasonId) {
          const seasonName = namesCache.seasons[resolvedSeasonId] || '...';
          crumbs.push({ label: seasonName, path: `/seasons/${resolvedSeasonId}` });
        }
        
        const cropName = namesCache.crops[cropId] || '...';
        crumbs.push({ label: cropName, path: `/crops/${cropId}` });
        
        if (currentPath.endsWith('/edit')) {
          crumbs.push({ label: t('farm.actions.editFarm', 'Edit'), path: `/crops/${cropId}/edit` });
        }
      }
    } else if (currentPath === '/tasks') {
      crumbs.push({ label: t('nav.tasks', 'Work'), path: '/tasks' });
    } else if (currentPath === '/disease-incidents') {
      crumbs.push({ label: t('nav.problems', 'Crop Problems'), path: '/disease-incidents' });
    } else if (currentPath === '/treatments') {
      crumbs.push({ label: t('nav.treatments', 'Treatments'), path: '/treatments' });
    } else if (currentPath === '/alerts') {
      crumbs.push({ label: t('nav.alerts', 'Alerts'), path: '/alerts' });
    } else if (currentPath === '/profile') {
      crumbs.push({ label: t('nav.profile', 'Profile'), path: '/profile' });
    }

    return crumbs;
  };

  const renderBreadcrumbs = () => {
    const crumbs = getBreadcrumbs();
    return (
      <nav className="flex items-center space-x-1 text-xs md:text-sm font-semibold text-gray-500 overflow-hidden select-none" aria-label="Breadcrumb">
        {crumbs.map((crumb, idx) => (
          <div key={idx} className="flex items-center min-w-0">
            {idx > 0 && <span className="mx-1 text-gray-400 shrink-0 font-bold">&gt;</span>}
            <button
              onClick={() => navigate(crumb.path)}
              className={`breadcrumb-truncate hover:text-emerald-700 hover:underline cursor-pointer truncate text-left shrink max-w-[60px] sm:max-w-[130px] md:max-w-[200px] ${
                idx === crumbs.length - 1 
                  ? 'text-gray-800 font-extrabold cursor-default no-underline pointer-events-none' 
                  : ''
              }`}
            >
              {crumb.label}
            </button>
          </div>
        ))}
      </nav>
    );
  };

  return (
    <div className="flex h-screen w-full flex-col bg-[#F0F7F1] md:flex-row font-sans">
      <style>{`
        @media (min-width: 360px) and (max-width: 430px) {
          .breadcrumb-truncate {
            max-width: 40px !important;
          }
        }
      `}</style>

      {/* Sidebar - Desktop Only (Deep Green #14532D) */}
      <aside className="hidden w-72 flex-col bg-[#14532D] text-[#ECFDF3] p-4 md:flex shadow-md">
        <div className="mb-8 flex items-center space-x-2 px-2">
          <span className="text-3xl font-black text-white tracking-tight">KRISHI</span>
        </div>
        
        <nav className="flex-1 space-y-1.5 overflow-y-auto">
          {desktopNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = item.path === '/dashboard'
              ? currentPath === '/dashboard'
              : item.path === '/farms'
                ? currentPath.startsWith('/farms') || currentPath.startsWith('/seasons')
                : item.path === '/crops'
                  ? currentPath.startsWith('/crops') || currentPath.startsWith('/crop-cycles')
                  : currentPath.startsWith(item.path);

            let count = 0;
            if (item.path === '/farms') count = counts.farms;
            else if (item.path === '/crops') count = counts.crops;
            else if (item.path === '/tasks') count = counts.tasks;
            else if (item.path === '/alerts') count = counts.alerts;

            return (
              <button
                key={item.path}
                onClick={() => navigate(item.path)}
                className={`flex w-full items-center justify-between rounded-xl px-4 transition-all h-12 cursor-pointer focus:outline-hidden focus:ring-2 focus:ring-white/40 ${
                  isActive
                    ? 'bg-[#DCFCE7] text-[#14532D] font-black shadow-xs'
                    : 'text-[#ECFDF3] hover:bg-white/10 font-bold'
                }`}
                style={{ minHeight: '48px' }}
              >
                <div className="flex items-center space-x-3.5 min-w-0">
                  <Icon className={`h-5.5 w-5.5 shrink-0 ${isActive ? 'text-[#14532D]' : 'text-[#ECFDF3]'}`} />
                  <span className="text-sm md:text-base leading-tight whitespace-normal text-left">{item.label}</span>
                </div>
                {count > 0 && (
                  <span className={`text-xs font-black px-2 py-0.5 rounded-full min-w-[20px] text-center shrink-0 ${
                    item.path === '/alerts' 
                      ? 'bg-rose-600 text-white' 
                      : isActive 
                        ? 'bg-[#14532D] text-white' 
                        : 'bg-white/20 text-[#ECFDF3]'
                  }`}>
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* User profile footer info - Desktop */}
        <div className="border-t border-white/15 pt-4">
          <div className="px-4 py-2">
            <p className="text-base font-bold text-[#ECFDF3] truncate">{user?.fullName || 'Farmer'}</p>
            <p className="text-sm text-white/70">{user?.phoneNumber || ''}</p>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top Header Bar */}
        <header className="flex h-16 items-center justify-between border-b border-[#C9DCCB] bg-white px-4 md:px-8 shadow-2xs">
          <div className="flex flex-1 items-center min-w-0 mr-2">
            {/* Logo visible on mobile only on dashboard */}
            {currentPath === '/dashboard' ? (
              <div className="flex items-center space-x-2 md:hidden">
                <span className="text-2xl font-black text-[#14532D] tracking-tight">KRISHI</span>
              </div>
            ) : (
              <div className="md:hidden min-w-0">
                {renderBreadcrumbs()}
              </div>
            )}

            <div className="hidden md:block">
              {renderBreadcrumbs()}
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {/* Quick Access to Alerts Notification icon */}
            <button
              onClick={() => navigate('/alerts')}
              className="relative p-2.5 rounded-full hover:bg-[#E3F2E6] text-[#525252] hover:text-[#222222] min-h-[40px] flex items-center justify-center cursor-pointer border border-transparent hover:border-[#C9DCCB]"
              aria-label="View Alerts"
            >
              <Bell className="h-5.5 w-5.5" />
              {counts.alerts > 0 && (
                <span className="absolute top-0.5 right-0.5 h-4.5 w-4.5 rounded-full bg-rose-600 text-white text-[10px] font-extrabold flex items-center justify-center border border-white shrink-0">
                  {counts.alerts}
                </span>
              )}
            </button>
          </div>
        </header>

        {/* Content Outlet - scrollable container */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 pb-24 md:pb-8 bg-[#F0F7F1]">
          <div className="max-w-5xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Bottom Nav Bar - Mobile Only (5 items) */}
      <nav className="fixed bottom-0 left-0 right-0 flex h-20 border-t border-[#C5DCC9] bg-white md:hidden z-40 shadow-lg px-1 justify-around">
        {mobileNavItems.map((item) => {
          const Icon = item.icon;
          const isActive = item.path === '/dashboard'
            ? currentPath === '/dashboard'
            : item.path === '/farms'
              ? currentPath.startsWith('/farms') || currentPath.startsWith('/seasons')
              : item.path === '/crops'
                ? currentPath.startsWith('/crops') || currentPath.startsWith('/crop-cycles')
                : currentPath.startsWith(item.path);

          let count = 0;
          if (item.path === '/farms') count = counts.farms;
          else if (item.path === '/crops') count = counts.crops;
          else if (item.path === '/tasks') count = counts.tasks;

          return (
            <button
              key={item.path}
              onClick={() => {
                setIsMoreOpen(false);
                navigate(item.path);
              }}
              className="relative flex flex-1 flex-col items-center justify-center space-y-1 transition-all h-full cursor-pointer min-w-0"
              style={{ minHeight: '48px' }}
            >
              <div 
                className={`flex items-center justify-center rounded-full px-4 py-1 transition-all ${
                  isActive ? 'bg-[#DCFCE7] text-[#14532D]' : 'text-stone-500'
                }`}
              >
                <Icon className="h-6 w-6 shrink-0" />
              </div>
              
              <span className={`text-[10px] tracking-normal leading-tight text-center whitespace-normal max-w-[68px] ${
                isActive ? 'text-[#14532D] font-black' : 'text-stone-500 font-bold'
              }`}>
                {item.label}
              </span>
              
              {count > 0 && (
                <span className="absolute top-1 right-2 text-[9px] font-black px-1.5 py-0.5 rounded-full border border-white shrink-0 min-w-[16px] text-center bg-[#087A55] text-white">
                  {count}
                </span>
              )}
            </button>
          );
        })}

        {/* 5th Mobile Item: MORE */}
        <button
          onClick={() => setIsMoreOpen(!isMoreOpen)}
          className={`relative flex flex-1 flex-col items-center justify-center space-y-1 transition-all h-full cursor-pointer min-w-0 ${
            isMoreOpen || currentPath === '/disease-incidents' || currentPath === '/treatments' || currentPath === '/alerts' || currentPath === '/profile'
              ? 'text-[#14532D]'
              : 'text-stone-500'
          }`}
          style={{ minHeight: '48px' }}
        >
          <div 
            className={`flex items-center justify-center rounded-full px-4 py-1 transition-all ${
              isMoreOpen || currentPath === '/disease-incidents' || currentPath === '/treatments' || currentPath === '/alerts' || currentPath === '/profile'
                ? 'bg-[#DCFCE7] text-[#14532D]'
                : 'text-stone-500'
            }`}
          >
            <MoreHorizontal className="h-6 w-6 shrink-0" />
          </div>
          <span className="text-[10px] tracking-normal leading-tight text-center font-black">
            {t('nav.more', 'More')}
          </span>
          {counts.alerts > 0 && (
            <span className="absolute top-1 right-2 text-[9px] font-black px-1.5 py-0.5 rounded-full border border-white shrink-0 min-w-[16px] text-center bg-rose-600 text-white">
              {counts.alerts}
            </span>
          )}
        </button>
      </nav>

      {/* Mobile "More" Bottom Sheet Overlay */}
      {isMoreOpen && (
        <div className="fixed inset-0 z-50 flex flex-col justify-end bg-black/50 backdrop-blur-xs md:hidden animate-in fade-in duration-150">
          <div className="bg-white rounded-t-3xl p-6 shadow-2xl space-y-4 animate-in slide-in-from-bottom duration-200">
            <div className="flex items-center justify-between border-b border-stone-100 pb-3">
              <h3 className="text-lg font-black text-stone-800">{t('nav.more', 'More Options')}</h3>
              <button
                onClick={() => setIsMoreOpen(false)}
                className="p-2 text-stone-400 hover:text-stone-600 cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <button
                onClick={() => { setIsMoreOpen(false); navigate('/disease-incidents'); }}
                className="flex items-center space-x-3 p-3.5 rounded-2xl bg-[#F0F7F1] border border-[#C9DCCB] text-left font-bold text-[#222222] cursor-pointer hover:bg-[#E3F2E6]"
              >
                <div className="p-2 rounded-xl bg-amber-100 text-[#D97706] shrink-0">
                  <ShieldAlert className="h-5 w-5" />
                </div>
                <span className="text-xs font-black">{t('nav.problems', 'Crop Problems')}</span>
              </button>

              <button
                onClick={() => { setIsMoreOpen(false); navigate('/treatments'); }}
                className="flex items-center space-x-3 p-3.5 rounded-2xl bg-[#F0F7F1] border border-[#C9DCCB] text-left font-bold text-[#222222] cursor-pointer hover:bg-[#E3F2E6]"
              >
                <div className="p-2 rounded-xl bg-blue-100 text-[#2563EB] shrink-0">
                  <HeartPulse className="h-5 w-5" />
                </div>
                <span className="text-xs font-black">{t('nav.treatments', 'Treatments')}</span>
              </button>

              <button
                onClick={() => { setIsMoreOpen(false); navigate('/alerts'); }}
                className="flex items-center space-x-3 p-3.5 rounded-2xl bg-[#F0F7F1] border border-[#C9DCCB] text-left font-bold text-[#222222] cursor-pointer hover:bg-[#E3F2E6]"
              >
                <div className="p-2 rounded-xl bg-rose-100 text-[#DC2626] shrink-0">
                  <Bell className="h-5 w-5" />
                </div>
                <span className="text-xs font-black">{t('nav.alerts', 'Alerts')}</span>
              </button>

              <button
                onClick={() => { setIsMoreOpen(false); navigate('/profile'); }}
                className="flex items-center space-x-3 p-3.5 rounded-2xl bg-[#F0F7F1] border border-[#C9DCCB] text-left font-bold text-[#222222] cursor-pointer hover:bg-[#E3F2E6]"
              >
                <div className="p-2 rounded-xl bg-[#E3F2E6] text-[#087A55] shrink-0">
                  <User className="h-5 w-5" />
                </div>
                <span className="text-xs font-black">{t('nav.profile', 'Profile')}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MainLayout;
