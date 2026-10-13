import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Sprout, BookOpen, Clock, MapPin, ChevronDown, ChevronUp, Layers, CheckCircle } from 'lucide-react';
import { fetchPublishedTemplates, fetchTemplateById } from '../api/cultivationPlanApi';

export const CatalogBrowser = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const [templateDetails, setTemplateDetails] = useState({});

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const list = await fetchPublishedTemplates();
        setTemplates(list);
      } catch (err) {
        console.error('Failed to load catalog:', err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleToggleExpand = async (tplId) => {
    if (expandedId === tplId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(tplId);
    if (!templateDetails[tplId]) {
      try {
        const full = await fetchTemplateById(tplId);
        setTemplateDetails((prev) => ({ ...prev, [tplId]: full }));
      } catch (err) {
        console.error('Failed to load template details:', err);
      }
    }
  };

  const getCropIcon = (name) => {
    if (!name) return '🌿';
    const clean = name.toLowerCase();
    if (clean.includes('paddy') || clean.includes('rice')) return '🌾';
    if (clean.includes('wheat')) return '🌾';
    if (clean.includes('tomato')) return '🍅';
    if (clean.includes('banana')) return '🌱';
    return '🌿';
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm font-bold text-gray-400 animate-pulse">
        {t('crops.catalog.loading', 'Loading agricultural cultivation catalog...')}
      </div>
    );
  }

  if (templates.length === 0) {
    return (
      <div className="p-8 text-center text-sm font-bold text-gray-400 bg-white rounded-2xl border border-gray-200">
        {t('crops.catalog.empty', 'No published crop cultivation packages found.')}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="p-4 bg-emerald-50/70 rounded-2xl border border-emerald-200 text-xs font-bold text-emerald-900 flex items-center space-x-2">
        <BookOpen className="h-4 w-4 text-emerald-700 shrink-0" />
        <span>
          {t('crops.catalog.banner', 'Scientific cultivation packages certified by ICAR (Indian Council of Agricultural Research) with automatic task schedules.')}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {templates.map((tpl) => {
          const isExpanded = expandedId === tpl.id;
          const details = templateDetails[tpl.id] || tpl;

          return (
            <div
              key={tpl.id}
              className="rounded-2xl bg-white border border-[#C9DCCB] p-5 shadow-2xs hover:shadow-xs transition-all flex flex-col justify-between"
            >
              <div className="space-y-3.5">
                {/* Header */}
                <div className="flex items-center space-x-3 pb-3 border-b border-[#C9DCCB]">
                  <span className="text-3xl p-1.5 bg-[#E3F2E6] rounded-xl shrink-0">
                    {getCropIcon(tpl.cropName)}
                  </span>
                  <div className="min-w-0">
                    <h3 className="text-base font-black text-[#222222] truncate">{tpl.cropName}</h3>
                    <div className="flex items-center space-x-1.5 text-xs font-bold text-[#087A55]">
                      <span>{tpl.sourceVersion}</span>
                      <span>&bull;</span>
                      <span className="truncate">{tpl.durationDays} days</span>
                    </div>
                  </div>
                </div>

                {/* Meta details */}
                <div className="space-y-2 text-xs font-semibold text-gray-600">
                  <div className="flex items-start space-x-1.5">
                    <BookOpen className="h-3.5 w-3.5 text-emerald-600 mt-0.5 shrink-0" />
                    <span className="text-gray-700 font-bold">{tpl.recommendationSource}</span>
                  </div>

                  {tpl.regionalApplicability && (
                    <div className="flex items-start space-x-1.5">
                      <MapPin className="h-3.5 w-3.5 text-emerald-600 mt-0.5 shrink-0" />
                      <span>{tpl.regionalApplicability}</span>
                    </div>
                  )}

                  {tpl.variety && (
                    <div className="p-2.5 bg-gray-50 rounded-xl border border-gray-150 space-y-1">
                      <span className="text-[10px] uppercase font-black text-gray-400 block tracking-wider">
                        {t('crops.catalog.varieties', 'Recommended Varieties')}
                      </span>
                      <p className="text-xs font-bold text-gray-800">
                        {tpl.variety}
                      </p>
                    </div>
                  )}
                </div>

                {/* Expanded Stages Preview */}
                {isExpanded && details.stages && details.stages.length > 0 && (
                  <div className="pt-2 border-t border-gray-150 space-y-2 text-xs">
                    <span className="text-[10px] uppercase font-black text-emerald-800 block tracking-wider flex items-center space-x-1">
                      <Layers className="h-3 w-3 inline mr-1 text-emerald-600" />
                      <span>{t('crops.catalog.growthStages', 'Growth Stages & Activities')}</span>
                    </span>
                    <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                      {details.stages.map((stage) => (
                        <div key={stage.id} className="p-2 bg-emerald-50/40 rounded-lg border border-emerald-100 space-y-1">
                          <div className="flex justify-between font-extrabold text-emerald-950">
                            <span>{stage.stageName}</span>
                            <span className="text-gray-400 text-[10px]">Day {stage.startDayOffset} - {stage.endDayOffset}</span>
                          </div>
                          {stage.tasks && stage.tasks.length > 0 && (
                            <ul className="text-[11px] text-gray-600 space-y-0.5 pl-3 list-disc">
                              {stage.tasks.map((task) => (
                                <li key={task.id}>
                                  <span className="font-bold text-gray-800">{task.title}</span> (Day {task.offsetDays})
                                </li>
                              ))}
                            </ul>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Actions Footer */}
              <div className="pt-4 mt-3 border-t border-gray-150 flex items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={() => handleToggleExpand(tpl.id)}
                  className="text-xs font-bold text-emerald-700 hover:text-emerald-850 flex items-center space-x-1 py-1 cursor-pointer"
                >
                  <span>{isExpanded ? t('crops.catalog.hideDetails', 'Hide Stages') : t('crops.catalog.viewDetails', 'View Stages')}</span>
                  {isExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                </button>

                <button
                  type="button"
                  onClick={() => navigate('/farms')}
                  className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-2xs active:scale-95 transition-all cursor-pointer flex items-center space-x-1"
                >
                  <Sprout className="h-3.5 w-3.5" />
                  <span>{t('crops.catalog.plantCrop', 'Plant Crop')}</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default CatalogBrowser;
