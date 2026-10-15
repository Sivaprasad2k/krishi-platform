import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToString } from 'react-dom/server';
import '../../../i18n';
import GrowthStageProgressCard from './GrowthStageProgressCard';

describe('GrowthStageProgressCard Component Tests', () => {
  it('renders gracefully when estimatedGrowthStage is null', () => {
    const html = renderToString(
      <GrowthStageProgressCard estimatedGrowthStage={null} crop={{}} />
    );

    expect(html).toContain('Phenological Growth Stage');
    expect(html).toContain('Growth stage estimation is currently unavailable');
  });

  it('renders resolved growth stage with model estimate disclaimer, days, and progress', () => {
    const mockStage = {
      status: 'RESOLVED',
      stageName: 'Tillering & Vegetative Phase',
      stageOrder: 2,
      totalStages: 5,
      elapsedDays: 24,
      stageStartDay: 15,
      stageEndDay: 35,
      stageProgressPercentage: 45.0,
      overallProgressPercentage: 20.0,
      templateName: 'Rice (Paddy) standard package',
      templateVersion: '2024.1',
      message: 'Active vegetative tillering'
    };

    const mockCrop = {
      plantingDate: '2026-09-20T00:00:00Z',
      expectedHarvestDate: '2026-12-25T00:00:00Z'
    };

    const html = renderToString(
      <GrowthStageProgressCard
        estimatedGrowthStage={mockStage}
        crop={mockCrop}
      />
    );

    // Checks stage title & order
    expect(html).toContain('Tillering &amp; Vegetative Phase');
    expect(html).toContain('Stage 2 of 5');
    // Checks elapsed days
    expect(html).toContain('Day 24');
    // Checks progress percentages
    expect(html).toContain('45%');
    expect(html).toContain('20%');
    // Checks explicit estimate disclaimers
    expect(html).toContain('Estimated Stage');
    expect(html).toContain('Not a field observation');
    // Checks template basis
    expect(html).toContain('Rice (Paddy) standard package');
    expect(html).toContain('v2024.1');
  });

  it('renders informative banner when status is NO_ACTIVE_PLAN', () => {
    const mockStage = {
      status: 'NO_ACTIVE_PLAN',
      message: 'No active cultivation plan'
    };

    const html = renderToString(
      <GrowthStageProgressCard
        estimatedGrowthStage={mockStage}
        crop={{}}
        onGeneratePlanClick={() => {}}
      />
    );

    expect(html).toContain('Plan Required');
    expect(html).toContain('No active cultivation plan is configured');
    expect(html).toContain('Generate Cultivation Plan');
  });

  it('renders notice when status is MISSING_PLANTING_DATE', () => {
    const mockStage = {
      status: 'MISSING_PLANTING_DATE'
    };

    const html = renderToString(
      <GrowthStageProgressCard estimatedGrowthStage={mockStage} crop={{}} />
    );

    expect(html).toContain('Date Required');
    expect(html).toContain('Planting date is not set');
  });

  it('renders pre-planting window when status is PRE_PLANTING', () => {
    const mockStage = {
      status: 'PRE_PLANTING',
      elapsedDays: -5
    };

    const mockCrop = {
      plantingDate: '2026-11-01T00:00:00Z'
    };

    const html = renderToString(
      <GrowthStageProgressCard estimatedGrowthStage={mockStage} crop={mockCrop} />
    );

    expect(html).toContain('Pre-Planting');
    expect(html).toContain('Scheduled planting date');
    expect(html).toContain('5');
    expect(html).toContain('days until scheduled planting');
  });

  it('renders post-harvest notification when status is POST_HARVEST', () => {
    const mockStage = {
      status: 'POST_HARVEST',
      elapsedDays: 130
    };

    const html = renderToString(
      <GrowthStageProgressCard estimatedGrowthStage={mockStage} crop={{}} />
    );

    expect(html).toContain('Cycle Concluded');
    expect(html).toContain('planned cultivation template duration for this planting has elapsed');
    expect(html).toContain('130');
  });
});
