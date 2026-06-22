import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../../store/authStore';
import { useNavigate } from 'react-router-dom';
import StateView from '../../shared/components/StateView';
import { User, Phone, Mail, Globe, LogOut } from 'lucide-react';

const ProfilePage = () => {
  const { t, i18n } = useTranslation();
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [viewState, setViewState] = useState('success');

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const changeLanguage = (e) => {
    i18n.changeLanguage(e.target.value);
  };

  return (
    <div className="space-y-6">
      {/* Dev State Controller */}
      <div className="rounded-xl border border-dashed border-emerald-300 bg-emerald-50/50 p-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <span className="text-sm font-bold text-emerald-800 uppercase tracking-wider">
            {t('dashboard.stateSelector', 'Review States (Dev Mode)')}
          </span>
          <div className="flex flex-wrap gap-2">
            {['success', 'loading', 'empty', 'error', 'offline'].map((stateName) => (
              <button
                key={stateName}
                onClick={() => setViewState(stateName)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all min-h-[36px] uppercase ${
                  viewState === stateName
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-white text-gray-600 border border-gray-250 hover:bg-gray-50'
                }`}
              >
                {stateName}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="rounded-xl bg-white p-6 shadow-sm border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800">{t('nav.profile', 'Profile')}</h1>
        <p className="text-base text-gray-500 mt-1">Manage settings and language preference</p>
      </div>

      <StateView
        state={viewState}
        onRetry={() => setViewState('success')}
      >
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {/* Profile Card */}
          <div className="md:col-span-2 rounded-2xl bg-white p-6 shadow-sm border border-gray-200 space-y-6">
            <h2 className="text-xl font-bold text-gray-800 border-b border-gray-100 pb-3">Personal Details</h2>
            
            <div className="space-y-4">
              <div className="flex items-center space-x-3.5">
                <div className="rounded-xl bg-gray-55 p-3 text-gray-500">
                  <User className="h-6 w-6" />
                </div>
                <div>
                  <span className="block text-sm font-semibold text-gray-400">Full Name</span>
                  <span className="text-lg font-bold text-gray-800">{user?.fullName || 'Farmer'}</span>
                </div>
              </div>

              <div className="flex items-center space-x-3.5">
                <div className="rounded-xl bg-gray-55 p-3 text-gray-500">
                  <Phone className="h-6 w-6" />
                </div>
                <div>
                  <span className="block text-sm font-semibold text-gray-400">Phone Number</span>
                  <span className="text-lg font-bold text-gray-800">{user?.phoneNumber || ''}</span>
                </div>
              </div>

              <div className="flex items-center space-x-3.5">
                <div className="rounded-xl bg-gray-55 p-3 text-gray-500">
                  <Mail className="h-6 w-6" />
                </div>
                <div>
                  <span className="block text-sm font-semibold text-gray-400">Email Address</span>
                  <span className="text-lg font-bold text-gray-800">{user?.email || 'N/A'}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Settings / Actions */}
          <div className="rounded-2xl bg-white p-6 shadow-sm border border-gray-200 space-y-6">
            <h2 className="text-xl font-bold text-gray-800 border-b border-gray-100 pb-3">Settings</h2>

            {/* Language Selection */}
            <div className="space-y-2">
              <label htmlFor="settings-language-select" className="flex items-center text-base font-bold text-gray-700 space-x-2">
                <Globe className="h-5 w-5 text-gray-500" />
                <span>Preferred Language</span>
              </label>
              <select
                id="settings-language-select"
                value={i18n.language}
                onChange={changeLanguage}
                className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base font-bold text-gray-700 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white cursor-pointer"
                style={{ minHeight: '48px' }}
              >
                <option value="en">English</option>
                <option value="ta">தமிழ்</option>
                <option value="ml">മലയാളം</option>
              </select>
            </div>

            {/* Logout Action */}
            <div className="pt-4 border-t border-gray-100">
              <button
                id="profile-logout-btn"
                onClick={handleLogout}
                className="w-full rounded-xl bg-red-50 hover:bg-red-100 border border-red-200 text-red-650 h-12 font-bold transition-all text-base flex items-center justify-center space-x-2 cursor-pointer"
                style={{ minHeight: '48px' }}
              >
                <LogOut className="h-5 w-5" />
                <span>{t('auth.logout', 'Logout')}</span>
              </button>
            </div>
          </div>
        </div>
      </StateView>
    </div>
  );
};

export default ProfilePage;
