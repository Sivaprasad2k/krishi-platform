import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { UserPlus, Eye, EyeOff, ShieldCheck, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';

const RegisterPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { register, loading: storeLoading } = useAuthStore();

  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [validationError, setValidationError] = useState(null);
  const [apiError, setApiError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const handlePhoneChange = (e) => {
    const val = e.target.value.replace(/\D/g, ''); // Digits only
    if (val.length <= 10) {
      setPhoneNumber(val);
    }
    clearErrors();
  };

  const clearErrors = () => {
    setValidationError(null);
    setApiError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    clearErrors();
    setSuccessMsg(null);

    // 1. Full Name check
    if (!fullName || fullName.trim() === '') {
      setValidationError(t('validation.nameRequired', 'Full Name is required.'));
      return;
    }

    if (fullName.length > 150) {
      setValidationError(t('validation.nameTooLong', 'Full Name cannot exceed 150 characters.'));
      return;
    }

    // 2. Phone number check (10-digit Indian number)
    const phonePattern = /^[6-9]\d{9}$/;
    if (!phonePattern.test(phoneNumber)) {
      setValidationError(t('validation.invalidPhone', 'Invalid phone number. Must be a 10-digit Indian number starting with 6-9.'));
      return;
    }

    // 3. Optional Email check
    if (email && email.trim() !== '') {
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailPattern.test(email)) {
        setValidationError(t('validation.invalidEmail', 'Invalid email format.'));
        return;
      }
    }

    // 4. Password backend requirement pattern check
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
    if (!passwordPattern.test(password)) {
      setValidationError(t('validation.weakPassword', 'Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*?&).'));
      return;
    }

    // 5. Password confirm check
    if (password !== confirmPassword) {
      setValidationError(t('validation.passwordMismatch', 'Passwords do not match.'));
      return;
    }

    // Call register action from authStore
    const result = await register(fullName, phoneNumber, email, password);

    if (result.success) {
      setSuccessMsg(t('validation.registerSuccess', 'Registration successful! Redirecting to login...'));
      setTimeout(() => {
        navigate('/login');
      }, 1500);
    } else {
      setApiError(result.error || t('validation.registerFailed', 'Registration failed. Please try again.'));
    }
  };

  const activeError = validationError || apiError;
  const passwordsMatch = confirmPassword.length > 0 && password === confirmPassword;
  const passwordsMismatch = confirmPassword.length > 0 && password !== confirmPassword;

  return (
    <div className="space-y-4 animate-fade-in">
      {/* Title & Subtitle */}
      <div>
        <h2 className="text-2xl font-black text-stone-900 tracking-tight">
          {t('auth.newToKrishi', 'New to Krishi?')}
        </h2>
        <p className="text-sm font-bold text-stone-500 mt-1">
          {t('auth.registerSubtitle', 'Create an account to get started.')}
        </p>
      </div>

      {/* Error Alert */}
      {activeError && (
        <div
          role="alert"
          id="register-error-alert"
          className="rounded-2xl bg-red-50 p-3.5 border border-red-200 text-sm font-extrabold text-red-800 flex items-start space-x-3"
        >
          <AlertCircle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
          <div className="flex-1">{activeError}</div>
        </div>
      )}

      {/* Success Alert */}
      {successMsg && (
        <div
          role="status"
          id="register-success-alert"
          className="rounded-2xl bg-emerald-50 p-3.5 border border-emerald-200 text-sm font-extrabold text-emerald-800 flex items-start space-x-3"
        >
          <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
          <div className="flex-1">{successMsg}</div>
        </div>
      )}

      {/* Registration Form */}
      <form onSubmit={handleSubmit} className="space-y-3.5">
        {/* Full Name */}
        <div>
          <label htmlFor="register-name-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1">
            {t('auth.fullName', 'Full Name')}
          </label>
          <input
            id="register-name-input"
            type="text"
            autoComplete="name"
            required
            disabled={storeLoading || !!successMsg}
            value={fullName}
            onChange={(e) => { setFullName(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-stone-300 px-4 h-11 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-600 transition-all"
            placeholder="e.g. Ramesh Kumar"
          />
        </div>

        {/* Phone Number */}
        <div>
          <label htmlFor="register-phone-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1">
            {t('auth.phoneNumber', 'Phone Number')}
          </label>
          <div className="relative flex items-center">
            <div className="absolute left-3 flex items-center space-x-1 pr-2 border-r border-stone-300 text-stone-600 font-extrabold text-sm select-none pointer-events-none">
              <span>🇮🇳</span>
              <span>+91</span>
            </div>
            <input
              id="register-phone-input"
              type="tel"
              autoComplete="tel"
              pattern="[6-9][0-9]{9}"
              maxLength={10}
              required
              disabled={storeLoading || !!successMsg}
              value={phoneNumber}
              onChange={handlePhoneChange}
              className="w-full rounded-xl border border-stone-300 pl-20 pr-4 h-11 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-600 transition-all"
              placeholder="9888888881"
            />
          </div>
        </div>

        {/* Email (Optional) */}
        <div>
          <div className="flex items-center justify-between mb-1">
            <label htmlFor="register-email-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider">
              {t('auth.email', 'Email Address')}
            </label>
            <span className="text-xs font-bold text-stone-400">
              ({t('auth.optional', 'Optional')})
            </span>
          </div>
          <input
            id="register-email-input"
            type="email"
            autoComplete="email"
            disabled={storeLoading || !!successMsg}
            value={email}
            onChange={(e) => { setEmail(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-stone-300 px-4 h-11 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-600 transition-all"
            placeholder="farmer@krishi.com"
          />
        </div>

        {/* Password */}
        <div>
          <label htmlFor="register-password-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1">
            {t('auth.password', 'Password')}
          </label>
          <div className="relative flex items-center">
            <input
              id="register-password-input"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              required
              disabled={storeLoading || !!successMsg}
              value={password}
              onChange={(e) => { setPassword(e.target.value); clearErrors(); }}
              className="w-full rounded-xl border border-stone-300 pl-4 pr-12 h-11 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-600 transition-all"
              placeholder="••••••••"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 p-1 rounded-lg text-stone-400 hover:text-stone-700 transition-colors cursor-pointer"
              aria-label={showPassword ? t('auth.hidePassword', 'Hide password') : t('auth.showPassword', 'Show password')}
            >
              {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </button>
          </div>
        </div>

        {/* Confirm Password */}
        <div>
          <label htmlFor="register-confirm-password-input" className="block text-xs font-black text-stone-700 uppercase tracking-wider mb-1">
            {t('auth.confirmPassword', 'Confirm Password')}
          </label>
          <div className="relative flex items-center">
            <input
              id="register-confirm-password-input"
              type={showConfirmPassword ? 'text' : 'password'}
              autoComplete="new-password"
              required
              disabled={storeLoading || !!successMsg}
              value={confirmPassword}
              onChange={(e) => { setConfirmPassword(e.target.value); clearErrors(); }}
              className={`w-full rounded-xl border pl-4 pr-12 h-11 text-base font-bold bg-stone-50/50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-500 transition-all ${
                passwordsMismatch ? 'border-red-500' : passwordsMatch ? 'border-emerald-500' : 'border-stone-300'
              }`}
              placeholder="••••••••"
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 p-1 rounded-lg text-stone-400 hover:text-stone-700 transition-colors cursor-pointer"
              aria-label={showConfirmPassword ? t('auth.hidePassword', 'Hide password') : t('auth.showPassword', 'Show password')}
            >
              {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </button>
          </div>
          {passwordsMismatch && (
            <p className="text-xs font-extrabold text-red-600 mt-1">
              {t('validation.passwordMismatch', 'Passwords do not match.')}
            </p>
          )}
          {passwordsMatch && (
            <p className="text-xs font-extrabold text-emerald-650 mt-1 flex items-center space-x-1">
              <CheckCircle2 className="h-3.5 w-3.5" />
              <span>Passwords match</span>
            </p>
          )}
        </div>

        {/* Submit CTA Button */}
        <button
          id="register-submit-btn"
          type="submit"
          disabled={storeLoading || !!successMsg || !fullName || !phoneNumber || !password || !confirmPassword || passwordsMismatch}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-extrabold text-base h-12 px-6 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition-all cursor-pointer flex items-center justify-center space-x-2 mt-2 min-h-[48px]"
        >
          {storeLoading ? (
            <span className="flex items-center space-x-2">
              <span className="h-5 w-5 rounded-full border-2 border-white border-t-transparent animate-spin" />
              <span>{t('states.loading', 'Loading...')}</span>
            </span>
          ) : (
            <span className="flex items-center space-x-2">
              <UserPlus className="h-5 w-5" />
              <span>{t('auth.createAccount', 'Create Account')}</span>
            </span>
          )}
        </button>
      </form>

      {/* Security Assurance Badge */}
      <div className="pt-2 border-t border-stone-100 flex items-center justify-center space-x-2 text-xs font-extrabold text-emerald-750 text-center">
        <ShieldCheck className="h-4.5 w-4.5 text-emerald-650 shrink-0" />
        <span>{t('auth.securityBadge', 'Your data is safe and secure with us.')}</span>
      </div>
    </div>
  );
};

export default RegisterPage;
