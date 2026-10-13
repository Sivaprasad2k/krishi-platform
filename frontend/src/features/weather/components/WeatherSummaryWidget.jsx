import React, { useState, useEffect } from 'react';
import { 
  Cloud, 
  CloudRain, 
  Sun, 
  Wind, 
  Droplets, 
  RefreshCw, 
  MapPin, 
  AlertTriangle,
  Compass,
  CheckCircle2
} from 'lucide-react';
import { getFarmWeather, refreshFarmWeather } from '../api/weatherApi';

const getWeatherIcon = (code) => {
  if (code === null || code === undefined) return <Sun className="w-8 h-8 text-amber-500" />;
  if (code === 0 || code === 1) return <Sun className="w-8 h-8 text-amber-500" />;
  if (code === 2 || code === 3) return <Cloud className="w-8 h-8 text-slate-500" />;
  if (code >= 51 && code <= 67) return <CloudRain className="w-8 h-8 text-blue-500" />;
  if (code >= 80 && code <= 82) return <CloudRain className="w-8 h-8 text-blue-600" />;
  if (code >= 95) return <CloudRain className="w-8 h-8 text-purple-600" />;
  return <Cloud className="w-8 h-8 text-slate-500" />;
};

export default function WeatherSummaryWidget({ farmId, farmName }) {
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchWeather = async () => {
    if (!farmId) return;
    try {
      setLoading(true);
      setError(null);
      const res = await getFarmWeather(farmId);
      if (res && res.data) {
        setWeather(res.data);
      }
    } catch (err) {
      console.error('Failed to load farm weather:', err);
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    if (!farmId || refreshing) return;
    try {
      setRefreshing(true);
      setError(null);
      const res = await refreshFarmWeather(farmId);
      if (res && res.data) {
        setWeather(res.data);
      }
    } catch (err) {
      console.error('Failed to refresh farm weather:', err);
      setError(err);
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchWeather();
  }, [farmId]);

  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm animate-pulse">
        <div className="flex justify-between items-center mb-4">
          <div className="h-4 bg-slate-200 rounded w-1/3"></div>
          <div className="h-4 bg-slate-200 rounded w-16"></div>
        </div>
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-slate-200 rounded-full"></div>
          <div className="space-y-2 flex-1">
            <div className="h-6 bg-slate-200 rounded w-1/4"></div>
            <div className="h-3 bg-slate-200 rounded w-1/2"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    const isLocationUnavailable = error?.response?.data?.errorCode === 'LOCATION_UNAVAILABLE';
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
          <div className="flex-1 text-sm">
            <h4 className="font-semibold text-slate-900 mb-1">
              {isLocationUnavailable ? 'Location Coordinates Unavailable' : 'Weather Forecast Unavailable'}
            </h4>
            <p className="text-slate-600 text-xs">
              {isLocationUnavailable 
                ? 'Farm coordinates or verified district are not configured. Please set farm GPS coordinates or a recognized district in Farm Details to activate automated weather intelligence.'
                : (error?.response?.data?.message || 'Unable to contact meteorological service. Please try again later.')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (!weather || !weather.current) {
    return null;
  }

  const { current, daily, coordinateSource, locationLabel, stale, attribution, attributionUrl } = weather;

  return (
    <div className="bg-gradient-to-br from-white to-slate-50 rounded-xl border border-slate-200 p-5 shadow-sm">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-2 mb-4 pb-3 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <MapPin className="w-4 h-4 text-emerald-600" />
          <span className="font-semibold text-slate-900 text-sm">{locationLabel || farmName || 'Farm Location'}</span>
          <span className={`text-[11px] font-medium px-2 py-0.5 rounded-full ${
            coordinateSource === 'FARM_GPS' 
              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' 
              : 'bg-blue-50 text-blue-700 border border-blue-200'
          }`}
          title={coordinateSource === 'FARM_GPS' ? 'Exact farm GPS coordinates' : 'District headquarters collectorate reference coordinates'}
          >
            {coordinateSource === 'FARM_GPS' ? 'GPS Accurate' : 'District HQ Reference'}
          </span>
        </div>

        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-emerald-700 p-1.5 rounded-lg hover:bg-slate-100 transition disabled:opacity-50"
          title="Refresh forecast (15-min cooldown)"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin text-emerald-600' : ''}`} />
          <span>{refreshing ? 'Updating...' : 'Refresh'}</span>
        </button>
      </div>

      {/* Stale cache indicator banner */}
      {stale && (
        <div className="mb-3 px-3 py-1.5 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-2 text-xs text-amber-800">
          <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
          <span>Showing cached forecast. Weather provider connection temporarily offline.</span>
        </div>
      )}

      {/* Current Conditions Display */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center mb-4">
        <div className="flex items-center gap-3.5">
          {getWeatherIcon(current.weatherCode)}
          <div>
            <div className="text-3xl font-bold text-slate-900 tracking-tight">
              {current.temperatureCelsius !== null ? `${Math.round(current.temperatureCelsius)}°C` : '--'}
            </div>
            <div className="text-xs font-medium text-slate-600">
              {current.weatherCondition || 'Observed'}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-2 bg-slate-50 p-2.5 rounded-lg border border-slate-100 text-center">
          <div>
            <div className="flex items-center justify-center gap-1 text-[11px] text-slate-500 mb-0.5">
              <Droplets className="w-3 h-3 text-blue-500" />
              <span>Humidity</span>
            </div>
            <div className="text-xs font-semibold text-slate-800">
              {current.relativeHumidity !== null ? `${current.relativeHumidity}%` : '--'}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-center gap-1 text-[11px] text-slate-500 mb-0.5">
              <CloudRain className="w-3 h-3 text-cyan-500" />
              <span>Rain Prob</span>
            </div>
            <div className="text-xs font-semibold text-slate-800">
              {current.precipitationProbability !== null ? `${current.precipitationProbability}%` : '--'}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-center gap-1 text-[11px] text-slate-500 mb-0.5">
              <Wind className="w-3 h-3 text-emerald-500" />
              <span>Wind</span>
            </div>
            <div className="text-xs font-semibold text-slate-800">
              {current.windSpeedKmh !== null ? `${Math.round(current.windSpeedKmh)} km/h` : '--'}
            </div>
          </div>
        </div>
      </div>

      {/* 7-Day Mini Forecast Strip */}
      {daily && daily.length > 0 && (
        <div className="mt-3 pt-3 border-t border-slate-100">
          <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">
            7-Day Outlook
          </div>
          <div className="grid grid-cols-7 gap-1 text-center">
            {daily.slice(0, 7).map((day, idx) => {
              const dateObj = new Date(day.date);
              const dayName = idx === 0 ? 'Today' : dateObj.toLocaleDateString('en-US', { weekday: 'short' });
              return (
                <div key={day.date} className="p-1 rounded hover:bg-slate-50 transition">
                  <div className="text-[10px] text-slate-500 font-medium">{dayName}</div>
                  <div className="my-1 flex justify-center">
                    {getWeatherIcon(day.weatherCode)}
                  </div>
                  <div className="text-xs font-bold text-slate-800">
                    {day.maxTempCelsius !== null ? `${Math.round(day.maxTempCelsius)}°` : '--'}
                  </div>
                  <div className="text-[10px] text-slate-400">
                    {day.minTempCelsius !== null ? `${Math.round(day.minTempCelsius)}°` : '--'}
                  </div>
                  {day.precipitationMm > 0 && (
                    <div className="text-[9px] font-medium text-blue-600 mt-0.5">
                      {Math.round(day.precipitationMm)}mm
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Mandatory Open-Meteo Attribution */}
      <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-between text-[10px] text-slate-400">
        <span>Updated: {weather.fetchedAt ? new Date(weather.fetchedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent'}</span>
        <span>
          Weather data by{' '}
          <a
            href={attributionUrl || 'https://open-meteo.com/'}
            target="_blank"
            rel="noopener noreferrer"
            className="text-emerald-700 underline hover:text-emerald-900 font-medium"
          >
            Open-Meteo.com
          </a>
        </span>
      </div>
    </div>
  );
}
