import React, { useState } from 'react';
import { AlertTriangle, AlertCircle, CheckCircle2, ShieldAlert } from 'lucide-react';
import { acknowledgeAdvisory } from '../api/weatherApi';

export default function WeatherAdvisoryBanner({ advisories = [], onAcknowledge }) {
  const [acknowledgingId, setAcknowledgingId] = useState(null);

  if (!advisories || advisories.length === 0) {
    return null;
  }

  const handleAcknowledge = async (id) => {
    try {
      setAcknowledgingId(id);
      await acknowledgeAdvisory(id);
      if (onAcknowledge) {
        onAcknowledge(id);
      }
    } catch (err) {
      console.error('Failed to acknowledge advisory:', err);
    } finally {
      setAcknowledgingId(null);
    }
  };

  return (
    <div className="space-y-3 mb-4">
      {advisories.map((adv) => {
        const isHigh = adv.severity === 'HIGH' || adv.severity === 'CRITICAL';
        return (
          <div
            key={adv.id}
            className={`p-4 rounded-xl border transition ${
              isHigh
                ? 'bg-rose-50 border-rose-200 text-rose-900'
                : 'bg-amber-50 border-amber-200 text-amber-900'
            }`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-2.5">
                {isHigh ? (
                  <ShieldAlert className="w-5 h-5 text-rose-600 mt-0.5 flex-shrink-0" />
                ) : (
                  <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
                )}
                <div>
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <span className="font-semibold text-sm">{adv.title}</span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${
                        isHigh
                          ? 'bg-rose-200 text-rose-800'
                          : 'bg-amber-200 text-amber-800'
                      }`}
                    >
                      {adv.severity} Severity
                    </span>
                    {adv.cropName && (
                      <span className="text-[11px] text-slate-600 font-medium bg-white/70 px-2 py-0.5 rounded border border-slate-200">
                        {adv.cropName}
                      </span>
                    )}
                    {adv.taskTitle && (
                      <span className="text-[11px] text-slate-600 font-medium bg-white/70 px-2 py-0.5 rounded border border-slate-200">
                        Task: {adv.taskTitle}
                      </span>
                    )}
                  </div>

                  <p className="text-xs leading-relaxed mb-2 opacity-90">{adv.message}</p>

                  <div className="text-xs font-medium bg-white/80 p-2.5 rounded-lg border border-slate-200/60">
                    <span className="font-semibold text-slate-900">Recommended Farmer Action: </span>
                    <span className="text-slate-800">{adv.recommendedAction}</span>
                  </div>
                </div>
              </div>

              {adv.status === 'ACTIVE' && (
                <button
                  onClick={() => handleAcknowledge(adv.id)}
                  disabled={acknowledgingId === adv.id}
                  className="flex-shrink-0 flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 transition shadow-sm disabled:opacity-50"
                  title="Acknowledge advisory"
                >
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>{acknowledgingId === adv.id ? 'Saving...' : 'Acknowledge'}</span>
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
