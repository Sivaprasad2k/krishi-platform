import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../../store/authStore';
import { useNavigate } from 'react-router-dom';
import { User, Phone, Mail, Globe, LogOut } from 'lucide-react';
import OptionCard from '../../shared/components/OptionCard';

const LANGUAGES = [
  { code: 'en', nativeName: 'English', subtitle: 'English language' },
  { code: 'ta', nativeName: 'தமிழ்', subtitle: 'Tamil language' },
  { code: 'ml', nativeName: 'മലയാളം', subtitle: 'Malayalam language' },
  { code: 'hi', nativeName: 'हिंदी', subtitle: 'Hindi language' },
];

/**
 * ProfilePage lists farmer details and preference controls (Language switching, Logout).
 */
const ProfilePage = () => {
  const { t, i18n } = useTranslation();
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const setLang = (code) => {
    i18n.changeLanguage(code);
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Page Header */}
      <div className="rounded-2xl bg-white p-6 shadow-2xs border border-[#C9DCCB]">
        <h1 className="text-2xl font-black text-[#222222]">{t('profile.title', 'Profile & Settings')}</h1>
        <p className="text-sm font-semibold text-[#525252] mt-1">
          {t('profile.subtitle', 'Manage settings and language preference')}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        {/* Profile Card */}
        <div className="md:col-span-2 rounded-2xl bg-white p-6 sm:p-8 border border-[#C9DCCB] shadow-2xs space-y-6">
          <h2 className="text-xl font-black text-[#222222] border-b border-[#C9DCCB] pb-3">
            {t('profile.detailsHeader', 'Personal Details')}
          </h2>

          <div className="space-y-5">
            <div className="flex items-center space-x-4">
              <div className="rounded-2xl bg-[#E3F2E6] text-[#087A55] p-3.5 shrink-0">
                <User className="h-6 w-6" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                  {t('profile.fullName', 'Full Name')}
                </span>
                <span className="text-lg font-black text-[#222222] truncate block">
                  {user?.fullName || 'Farmer'}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="rounded-2xl bg-[#E3F2E6] text-[#087A55] p-3.5 shrink-0">
                <Phone className="h-6 w-6" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                  {t('profile.phoneNumber', 'Phone Number')}
                </span>
                <span className="text-lg font-black text-[#222222] block">
                  {user?.phoneNumber || ''}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="rounded-2xl bg-[#E3F2E6] text-[#087A55] p-3.5 shrink-0">
                <Mail className="h-6 w-6" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-black text-[#525252] uppercase tracking-wider">
                  {t('profile.emailAddress', 'Email Address')}
                </span>
                <span className="text-lg font-black text-[#222222] truncate block">
                  {user?.email || 'N/A'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Settings Panel */}
        <div className="rounded-2xl bg-white p-6 sm:p-8 border border-[#C9DCCB] shadow-2xs space-y-6">
          <h2 className="text-xl font-black text-[#222222] border-b border-[#C9DCCB] pb-3">
            {t('profile.settingsHeader', 'Language')}
          </h2>

          {/* Native Script Language Selection */}
          <div className="space-y-2.5">
            <label className="flex items-center text-sm font-black text-[#222222] space-x-2">
              <Globe className="h-5 w-5 text-[#087A55]" />
              <span>{t('profile.languageLabel', 'Select Language')}</span>
            </label>
            <div className="space-y-2.5">
              {LANGUAGES.map((lang) => (
                <OptionCard
                  key={lang.code}
                  title={lang.nativeName}
                  description={lang.subtitle}
                  selected={i18n.language === lang.code}
                  onClick={() => setLang(lang.code)}
                />
              ))}
            </div>
          </div>

          {/* Logout Action */}
          <div className="pt-4 border-t border-[#C9DCCB]">
            <button
              id="profile-logout-btn"
              onClick={handleLogout}
              className="w-full rounded-xl bg-[#FEF2F2] hover:bg-red-100 border border-[#DC2626] text-[#DC2626] font-black transition-all text-base flex items-center justify-center space-x-2 cursor-pointer shadow-2xs"
              style={{ minHeight: '48px' }}
            >
              <LogOut className="h-5 w-5" />
              <span>{t('profile.logoutButton', 'Logout')}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
