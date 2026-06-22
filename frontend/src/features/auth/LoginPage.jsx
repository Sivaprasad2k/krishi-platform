import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useTranslation } from 'react-i18next';

const LoginPage = () => {
  const { t } = useTranslation();
  const { login, loading: storeLoading } = useAuthStore();
  const navigate = useNavigate();

  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [validationError, setValidationError] = useState(null);
  const [apiError, setApiError] = useState(null);

  const handlePhoneChange = (e) => {
    const val = e.target.value.replace(/\D/g, ''); // Allow digits only
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

    // 1. Local Validation checks
    const phonePattern = /^[6-9]\d{9}$/;
    if (!phonePattern.test(phoneNumber)) {
      setValidationError(t('validation.invalidPhone', 'Invalid phone number. Must be a 10-digit Indian number starting with 6, 7, 8, or 9.'));
      return;
    }

    if (!password || password.trim() === '') {
      setValidationError(t('validation.passwordRequired', 'Password is required.'));
      return;
    }

    // 2. Call authStore action
    const result = await login(phoneNumber, password);

    if (result.success) {
      navigate('/dashboard');
    } else {
      setApiError(result.error || t('validation.loginFailed', 'Incorrect phone number or password. Please try again.'));
    }
  };

  const activeError = validationError || apiError;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-center text-gray-800">{t('auth.login', 'Login')}</h2>
      
      {activeError && (
        <div className="rounded-xl bg-red-50 p-4 text-base font-semibold text-red-700 border border-red-200" id="login-error-alert">
          {activeError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="login-phone-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.phoneNumber', 'Phone Number')}
          </label>
          <input
            id="login-phone-input"
            type="tel"
            pattern="[6-9][0-9]{9}"
            required
            disabled={storeLoading}
            value={phoneNumber}
            onChange={handlePhoneChange}
            className={`w-full rounded-xl border px-4 h-12 text-base focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all ${
              validationError ? 'border-red-500 focus:border-red-500' : 'border-gray-300 focus:border-emerald-500'
            }`}
            placeholder="e.g. 9888888881"
          />
        </div>

        <div>
          <label htmlFor="login-password-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.password', 'Password')}
          </label>
          <input
            id="login-password-input"
            type="password"
            required
            disabled={storeLoading}
            value={password}
            onChange={handlePasswordChange}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="••••••••"
          />
        </div>

        <button
          id="login-submit-btn"
          type="submit"
          disabled={storeLoading}
          className="w-full rounded-xl bg-emerald-600 px-6 h-12 text-base font-bold text-white hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 disabled:opacity-55 transition-colors shadow-sm cursor-pointer flex items-center justify-center"
        >
          {storeLoading ? t('states.loading', 'Loading...') : t('auth.login', 'Login')}
        </button>
      </form>

      <div className="text-center text-base text-gray-500">
        <span>Don't have an account? </span>
        <Link to="/register" id="login-to-register-link" className="font-bold text-emerald-650 hover:text-emerald-750 focus:underline p-2">
          {t('auth.register', 'Register')}
        </Link>
      </div>
    </div>
  );
};

export default LoginPage;
