/**
 * Conversions:
 * 1 Acre = 0.404686 Hectares
 * 1 Cent = 0.00404686 Hectares
 * 1 Hectare = 1.0 Hectare
 */

export const toHectares = (value, unit) => {
  const val = parseFloat(value) || 0;
  const u = unit ? unit.toUpperCase() : 'HECTARE';
  if (u === 'ACRE') return val * 0.404686;
  if (u === 'CENT') return val * 0.00404686;
  return val;
};

export const fromHectares = (value, unit) => {
  const val = parseFloat(value) || 0;
  const u = unit ? unit.toUpperCase() : 'HECTARE';
  if (u === 'ACRE') return val / 0.404686;
  if (u === 'CENT') return val / 0.00404686;
  return val;
};

export const formatArea = (value, unit, t) => {
  const val = parseFloat(value) || 0;
  const u = unit ? unit.toUpperCase() : 'HECTARE';

  // Format: clean round values for whole numbers, otherwise limit to 2 decimal places
  const formattedVal = val % 1 === 0 ? val.toFixed(0) : val.toFixed(2);
  const localizedUnit = t ? t(`units.${u.toLowerCase()}`, u) : u.toLowerCase();
  
  return `${formattedVal} ${localizedUnit}`;
};
