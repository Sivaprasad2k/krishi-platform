import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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

    // 1. Local Validations
    if (!fullName || fullName.trim() === '') {
      setValidationError(t('validation.nameRequired', 'Full Name is required.'));
      return;
    }

    if (fullName.length > 150) {
      setValidationError(t('validation.nameTooLong', 'Full Name cannot exceed 150 characters.'));
      return;
    }

    const phonePattern = /^[6-9]\d{9}$/;
    if (!phonePattern.test(phoneNumber)) {
      setValidationError(t('validation.invalidPhone', 'Invalid phone number. Must be a 10-digit Indian number starting with 6-9.'));
      return;
    }

    if (email && email.trim() !== '') {
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailPattern.test(email)) {
        setValidationError(t('validation.invalidEmail', 'Invalid email format.'));
        return;
      }
    }

    // Password strong checks
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
    if (!passwordPattern.test(password)) {
      setValidationError(t('validation.weakPassword', 'Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*?&).'));
      return;
    }

    if (password !== confirmPassword) {
      setValidationError(t('validation.passwordMismatch', 'Passwords do not match.'));
      return;
    }

    // 2. Trigger register action
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

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-center text-gray-800">{t('auth.register', 'Register')}</h2>

      {activeError && (
        <div className="rounded-xl bg-red-50 p-4 text-base font-semibold text-red-700 border border-red-200" id="register-error-alert">
          {activeError}
        </div>
      )}

      {successMsg && (
        <div className="rounded-xl bg-green-50 p-4 text-base font-semibold text-green-700 border border-green-200" id="register-success-alert">
          {successMsg}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="register-name-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.fullName', 'Full Name')}
          </label>
          <input
            id="register-name-input"
            type="text"
            required
            disabled={storeLoading || successMsg}
            value={fullName}
            onChange={(e) => { setFullName(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="e.g. Farmer Alice"
          />
        </div>

        <div>
          <label htmlFor="register-phone-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.phoneNumber', 'Phone Number')}
          </label>
          <input
            id="register-phone-input"
            type="tel"
            pattern="[6-9][0-9]{9}"
            required
            disabled={storeLoading || successMsg}
            value={phoneNumber}
            onChange={handlePhoneChange}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="e.g. 9888888881"
          />
        </div>

        <div>
          <label htmlFor="register-email-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.email', 'Email Address') + ' (' + t('auth.optional', 'Optional') + ')'}
          </label>
          <input
            id="register-email-input"
            type="email"
            disabled={storeLoading || successMsg}
            value={email}
            onChange={(e) => { setEmail(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="e.g. alice@krishi.com"
          />
        </div>

        <div>
          <label htmlFor="register-password-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.password', 'Password')}
          </label>
          <input
            id="register-password-input"
            type="password"
            required
            disabled={storeLoading || successMsg}
            value={password}
            onChange={(e) => { setPassword(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="••••••••"
          />
        </div>

        <div>
          <label htmlFor="register-confirm-password-input" className="block text-base font-bold text-gray-750 mb-2">
            {t('auth.confirmPassword', 'Confirm Password')}
          </label>
          <input
            id="register-confirm-password-input"
            type="password"
            required
            disabled={storeLoading || successMsg}
            value={confirmPassword}
            onChange={(e) => { setConfirmPassword(e.target.value); clearErrors(); }}
            className="w-full rounded-xl border border-gray-300 px-4 h-12 text-base focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 bg-white transition-all"
            placeholder="••••••••"
          />
        </div>

        <button
          id="register-submit-btn"
          type="submit"
          disabled={storeLoading || successMsg}
          className="w-full rounded-xl bg-emerald-600 px-6 h-12 text-base font-bold text-white hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 disabled:opacity-55 transition-colors shadow-sm cursor-pointer flex items-center justify-center"
        >
          {storeLoading ? t('states.loading', 'Loading...') : t('auth.register', 'Register')}
        </button>
      </form>

      <div className="text-center text-base text-gray-500">
        <span>Already have an account? </span>
        <Link to="/login" id="register-to-login-link" className="font-bold text-emerald-650 hover:text-emerald-750 focus:underline p-2">
          {t('auth.login', 'Login')}
        </Link>
      </div>
    </div>
  );
};

export default RegisterPage;
