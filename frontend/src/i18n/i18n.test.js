import { describe, it, expect } from 'vitest';
import en from './locales/en.json';
import ml from './locales/ml.json';
import ta from './locales/ta.json';
import hi from './locales/hi.json';

function getKeys(obj, prefix = '') {
  let keys = [];
  for (const key in obj) {
    if (typeof obj[key] === 'object' && obj[key] !== null) {
      keys = keys.concat(getKeys(obj[key], `${prefix}${key}.`));
    } else {
      keys.push(`${prefix}${key}`);
    }
  }
  return keys;
}

describe('i18n Locale Dictionaries Integrity & Key Parity Tests', () => {
  it('contains valid navigation labels across all 4 V1 supported languages', () => {
    expect(en.nav.home).toBe('Home');
    expect(ml.nav.home).toBe('ഹോം');
    expect(ta.nav.home).toBe('முகப்பு');
    expect(hi.nav.home).toBe('मुख्य पृष्ठ');
  });

  it('contains valid error codes across all 4 V1 supported languages', () => {
    expect(en.errors.codes.VALIDATION_FAILED).toBeDefined();
    expect(ml.errors.codes.VALIDATION_FAILED).toBeDefined();
    expect(ta.errors.codes.VALIDATION_FAILED).toBeDefined();
    expect(hi.errors.codes.VALIDATION_FAILED).toBeDefined();
  });

  it('contains valid task type titles across all 4 V1 supported languages', () => {
    expect(en.tasks.types.IRRIGATION).toBe('Water Crop');
    expect(ml.tasks.types.IRRIGATION).toBe('വെള്ളമൊഴിക്കൽ');
    expect(ta.tasks.types.IRRIGATION).toBe('பயிருக்கு நீர் பாய்ச்சுதல்');
    expect(hi.tasks.types.IRRIGATION).toBe('फसल सिंचाई');
  });

  it('enforces 100% key parity between en.json and ml.json, ta.json, hi.json', () => {
    const enKeys = getKeys(en);
    const mlKeys = getKeys(ml);
    const taKeys = getKeys(ta);
    const hiKeys = getKeys(hi);

    const missingInMl = enKeys.filter(k => !mlKeys.includes(k));
    const missingInTa = enKeys.filter(k => !taKeys.includes(k));
    const missingInHi = enKeys.filter(k => !hiKeys.includes(k));

    expect(missingInMl).toEqual([]);
    expect(missingInTa).toEqual([]);
    expect(missingInHi).toEqual([]);
  });
});
