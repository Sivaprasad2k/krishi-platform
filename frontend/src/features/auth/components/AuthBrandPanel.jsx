import { useTranslation } from 'react-i18next';
import { Sprout, ShieldCheck, BarChart3, CalendarDays } from 'lucide-react';

const AuthBrandPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="hidden lg:flex flex-col justify-center w-full lg:w-5/12 xl:w-1/2 pr-6 xl:pr-12 space-y-4 select-none">
      {/* Brand Header */}
      <div className="space-y-2.5">
        <div className="flex items-center space-x-3">
          <div className="h-12 w-12 rounded-2xl bg-emerald-600 flex items-center justify-center text-white shadow-md">
            <Sprout className="h-7 w-7" />
          </div>
          <span className="text-4xl font-black tracking-tight text-emerald-800">
            KRISHI
          </span>
        </div>
        <p className="text-xl sm:text-2xl font-extrabold text-stone-800 leading-snug max-w-md">
          {t('auth.tagline', 'Manage your farm, grow with confidence.')}
        </p>
      </div>

      {/* 3 Value Propositions */}
      <div className="space-y-3 max-w-md">
        {/* Value Prop 1 */}
        <div className="flex items-start space-x-4 bg-white/70 backdrop-blur-xs border border-stone-200/80 p-3.5 rounded-2xl shadow-xs transition-all hover:border-emerald-300">
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-700 shrink-0 border border-emerald-150">
            <CalendarDays className="h-5.5 w-5.5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-stone-850">
              {t('auth.features.planAndTrack', 'Plan & Track')}
            </h3>
            <p className="text-sm font-semibold text-stone-600 mt-0.5 leading-relaxed">
              {t('auth.features.planAndTrackDesc', 'Plan seasons, crops, and daily farm work.')}
            </p>
          </div>
        </div>

        {/* Value Prop 2 */}
        <div className="flex items-start space-x-4 bg-white/70 backdrop-blur-xs border border-stone-200/80 p-3.5 rounded-2xl shadow-xs transition-all hover:border-emerald-300">
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-700 shrink-0 border border-emerald-150">
            <ShieldCheck className="h-5.5 w-5.5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-stone-850">
              {t('auth.features.protectCrops', 'Protect Crops')}
            </h3>
            <p className="text-sm font-semibold text-stone-600 mt-0.5 leading-relaxed">
              {t('auth.features.protectCropsDesc', 'Detect problems early and take the right action.')}
            </p>
          </div>
        </div>

        {/* Value Prop 3 */}
        <div className="flex items-start space-x-4 bg-white/70 backdrop-blur-xs border border-stone-200/80 p-3.5 rounded-2xl shadow-xs transition-all hover:border-emerald-300">
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-700 shrink-0 border border-emerald-150">
            <BarChart3 className="h-5.5 w-5.5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-stone-850">
              {t('auth.features.betterDecisions', 'Better Decisions')}
            </h3>
            <p className="text-sm font-semibold text-stone-600 mt-0.5 leading-relaxed">
              {t('auth.features.betterDecisionsDesc', 'Get insights and reminders to improve your yield.')}
            </p>
          </div>
        </div>
      </div>

      {/* Calm Agricultural Imagery Banner / Footer Accent */}
      <div className="relative rounded-2xl overflow-hidden border border-stone-200/80 bg-gradient-to-br from-emerald-800 to-stone-900 p-4 text-white shadow-sm max-w-md mt-1">
        <div className="relative z-10 space-y-1">
          <div className="flex items-center space-x-2 text-emerald-300 text-xs font-bold uppercase tracking-wider">
            <Sprout className="h-4 w-4" />
            <span>Operational Farm Platform</span>
          </div>
          <p className="text-sm font-semibold text-stone-200">
            Empowering Indian farmers with digital tools for sustainable agriculture.
          </p>
        </div>
        <div className="absolute -right-6 -bottom-6 w-32 h-32 bg-emerald-500/20 rounded-full blur-xl pointer-events-none" />
      </div>
    </div>
  );
};

export default AuthBrandPanel;
