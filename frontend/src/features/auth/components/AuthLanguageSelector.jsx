import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';

const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'ta', label: 'தமிழ்' },
  { code: 'ml', label: 'മലയാളം' },
  { code: 'hi', label: 'हिन्दी' }
];

const AuthLanguageSelector = () => {
  const { i18n, t } = useTranslation();

  const currentLang = i18n.language || 'en';

  const handleLanguageChange = (langCode) => {
    i18n.changeLanguage(langCode);
  };

  return (
    <div className="flex flex-col items-center justify-center space-y-2.5 w-full">
      <div className="flex items-center space-x-1.5 text-xs sm:text-sm font-semibold text-gray-500">
        <Globe className="h-4 w-4 text-emerald-650 shrink-0" />
        <span>{t('auth.chooseLanguage', 'Choose your language / अपनी भाषा चुनें')}</span>
      </div>

      <div className="flex items-center justify-center flex-wrap gap-2 w-full max-w-lg">
        {LANGUAGES.map((lang) => {
          const isActive = currentLang === lang.code;
          return (
            <button
              key={lang.code}
              type="button"
              onClick={() => handleLanguageChange(lang.code)}
              className={`px-4 py-2 rounded-xl text-sm font-bold transition-all duration-200 cursor-pointer min-h-[44px] flex items-center justify-center border ${
                isActive
                  ? 'border-emerald-600 bg-emerald-50/70 text-emerald-800 shadow-xs font-black ring-1 ring-emerald-600'
                  : 'border-gray-250 bg-white text-gray-700 hover:border-emerald-400 hover:bg-stone-50'
              }`}
              aria-label={`Switch language to ${lang.label}`}
            >
              {lang.label}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default AuthLanguageSelector;
