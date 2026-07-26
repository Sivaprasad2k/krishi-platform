import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LogIn, Eye, EyeOff, ShieldCheck, AlertCircle } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';

const REMEMBER_PHONE_KEY = 'krishi_remember_phone';

const LoginPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { login, loading: storeLoading } = useAuthStore();

  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const [validationError, setValidationError] = useState(null);
  const [apiError, setApiError] = useState(null);

  // Restore remembered phone number if exists
  useEffect(() => {
    const savedPhone = localStorage.getItem(REMEMBER_PHONE_KEY);
    if (savedPhone) {
      setPhoneNumber(savedPhone);
      setRememberMe(true);
    }
  }, []);

  const handlePhoneChange = (e) => {
    const val = e.target.value.replace(/\D/g, ''); // Digits only
    if (val.length <= 10) {
      setPhoneNumber(val);
    }
    setValidationError(null);
    setApiError(null);
  };

  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
    setValidationError(null);
    setApiError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setValidationError(null);
    setApiError(null);

    // 1. Phone number validation (10-digit Indian number)
    const phonePattern = /^[6-9]\d{9}$/;
    if (!phonePattern.test(phoneNumber)) {
      setValidationError(t('validation.invalidPhone', 'Invalid phone number. Must be a 10-digit Indian number starting with 6, 7, 8, or 9.'));
      return;
    }

    // 2. Password validation
    if (!password || password.trim() === '') {
      setValidationError(t('validation.passwordRequired', 'Password is required.'));
      return;
    }

    // Remember me persistence logic
    if (rememberMe) {
      localStorage.setItem(REMEMBER_PHONE_KEY, phoneNumber);
    } else {
      localStorage.removeItem(REMEMBER_PHONE_KEY);
    }

    // 3. Call auth store login action
    const result = await login(phoneNumber, password);

    if (result.success) {
      navigate('/dashboard');
    } else {
      setApiError(result.error || t('validation.loginFailed', 'Incorrect phone number or password. Please try again.'));
    }
  };

  const activeError = validationError || apiError;

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Title & Subtitle */}
      <div>
        <h2 className="text-2xl font-black text-stone-900 tracking-tight">
          {t('auth.welcomeBack', 'Welcome back!')}
        </h2>
        <p className="text-sm font-bold text-stone-500 mt-1">
          {t('auth.loginSubtitle', 'Login to continue managing your farm.')}
        </p>
      </div>

      {/* Error Alert */}
      {activeError && (
        <div
          role="alert"
          id="login-error-alert"
          className="rounded-2xl bg-red-50 p-4 border border-red-200 text-sm font-extrabold text-red-800 flex items-start space-x-3"
        >
          <AlertCircle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
          <div className="flex-1">{activeError}</div>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Phone Number Field */}
        <div>
          <label htmlFor="login-phone-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1.5">
            {t('auth.phoneNumber', 'Phone Number')}
          </label>
          <div className="relative flex items-center">
            {/* Visual India +91 Country Code Indicator */}
            <div className="absolute left-3 flex items-center space-x-1 pr-2 border-r border-stone-300 text-stone-600 font-extrabold text-sm select-none pointer-events-none">
              <span>🇮🇳</span>
              <span>+91</span>
            </div>

            <input
              id="login-phone-input"
              type="tel"
              autoComplete="tel"
              pattern="[6-9][0-9]{9}"
              maxLength={10}
              required
              disabled={storeLoading}
              value={phoneNumber}
              onChange={handlePhoneChange}
              className={`w-full rounded-xl border pl-20 pr-4 h-12 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 transition-all ${
                validationError ? 'border-red-500 focus:border-red-500' : 'border-stone-300 focus:border-emerald-600'
              }`}
              placeholder="9888888881"
            />
          </div>
        </div>

        {/* Password Field */}
        <div>
          <label htmlFor="login-password-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1.5">
            {t('auth.password', 'Password')}
          </label>
          <div className="relative flex items-center">
            <input
              id="login-password-input"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              required
              disabled={storeLoading}
              value={password}
              onChange={handlePasswordChange}
              className="w-full rounded-xl border border-stone-300 pl-4 pr-12 h-12 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-600 transition-all"
              placeholder="••••••••"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 p-1.5 rounded-lg text-stone-400 hover:text-stone-700 transition-colors cursor-pointer"
              aria-label={showPassword ? t('auth.hidePassword', 'Hide password') : t('auth.showPassword', 'Show password')}
            >
              {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </button>
          </div>
        </div>

        {/* Remember Me Checkbox */}
        <div className="flex items-center justify-between pt-1">
          <label className="flex items-center space-x-2.5 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="h-4.5 w-4.5 rounded-md border-stone-300 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
            />
            <span className="text-sm font-bold text-stone-650">
              {t('auth.rememberMe', 'Remember me')}
            </span>
          </label>
        </div>

        {/* Submit Button */}
        <button
          id="login-submit-btn"
          type="submit"
          disabled={storeLoading || !phoneNumber || !password}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-extrabold text-base h-12 px-6 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition-all cursor-pointer flex items-center justify-center space-x-2 min-h-[48px]"
        >
          {storeLoading ? (
            <span className="flex items-center space-x-2">
              <span className="h-5 w-5 rounded-full border-2 border-white border-t-transparent animate-spin" />
              <span>{t('states.loading', 'Loading...')}</span>
            </span>
          ) : (
            <span className="flex items-center space-x-2">
              <LogIn className="h-5 w-5" />
              <span>{t('auth.login', 'Login')}</span>
            </span>
          )}
        </button>
      </form>

      {/* Security Assurance Badge */}
      <div className="pt-3 border-t border-stone-100 flex items-center justify-center space-x-2 text-xs font-extrabold text-emerald-750 text-center">
        <ShieldCheck className="h-4.5 w-4.5 text-emerald-650 shrink-0" />
        <span>{t('auth.securityBadge', 'Your data is safe and secure with us.')}</span>
      </div>
    </div>
  );
};

export default LoginPage;
