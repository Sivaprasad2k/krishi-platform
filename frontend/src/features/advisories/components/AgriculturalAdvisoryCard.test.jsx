import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToString } from 'react-dom/server';
import '../../../i18n';
import AgriculturalAdvisoryCard from './AgriculturalAdvisoryCard';
import ResolveAdvisoryModal from './ResolveAdvisoryModal';

describe('AgriculturalAdvisoryCard & Modal Tests', () => {
  const mockActiveAdvisory = {
    id: 'adv-1',
    ruleCode: 'CHEMICAL_SPRAYING_WIND_RISK',
    severity: 'HIGH',
    title: 'High-Wind Spray Drift Risk',
    message: 'Forecast sustained wind of 28.0 km/h exceeds safe threshold of 20.0 km/h.',
    recommendedAction: 'Withhold chemical spraying and pest control applications.',
    targetDate: '2026-10-16',
    status: 'ACTIVE',
    taskTitle: 'Scheduled Neemark Spraying',
    provenance: 'FAO/CABI Agrochemical Spray Drift Guidelines',
    createdAt: '2026-10-15T10:00:00Z',
    lastEvaluatedAt: '2026-10-15T12:00:00Z'
  };

  it('renders active advisory with severity, explanation, action, task, and lifecycle buttons', () => {
    const html = renderToString(
      <AgriculturalAdvisoryCard
        advisory={mockActiveAdvisory}
        onAcknowledge={() => {}}
        onResolveClick={() => {}}
      />
    );

    expect(html).toContain('High-Wind Spray Drift Risk');
    expect(html).toContain('HIGH Severity');
    expect(html).toContain('Active');
    expect(html).toContain('Forecast sustained wind of 28.0 km/h');
    expect(html).toContain('Withhold chemical spraying and pest control applications');
    expect(html).toContain('Scheduled Neemark Spraying');
    expect(html).toContain('FAO/CABI Agrochemical Spray Drift Guidelines');
    expect(html).toContain('Acknowledge');
    expect(html).toContain('Resolve');
  });

  it('renders acknowledged advisory without Acknowledge button but with Resolve button', () => {
    const acknowledgedAdv = {
      ...mockActiveAdvisory,
      id: 'adv-2',
      status: 'ACKNOWLEDGED'
    };

    const html = renderToString(
      <AgriculturalAdvisoryCard
        advisory={acknowledgedAdv}
        onAcknowledge={() => {}}
        onResolveClick={() => {}}
      />
    );

    expect(html).toContain('Acknowledged');
    expect(html).not.toContain('>Acknowledge<');
    expect(html).toContain('Resolve');
  });

  it('renders resolved advisory with resolved badge and resolved date', () => {
    const resolvedAdv = {
      ...mockActiveAdvisory,
      id: 'adv-3',
      status: 'RESOLVED',
      resolvedAt: '2026-10-15T14:30:00Z'
    };

    const html = renderToString(
      <AgriculturalAdvisoryCard
        advisory={resolvedAdv}
        onAcknowledge={() => {}}
        onResolveClick={() => {}}
      />
    );

    expect(html).toContain('Resolved');
    expect(html).not.toContain('Acknowledge');
  });

  it('ResolveAdvisoryModal renders when isOpen is true', () => {
    const html = renderToString(
      <ResolveAdvisoryModal
        isOpen={true}
        advisory={mockActiveAdvisory}
        onClose={() => {}}
        onConfirm={() => {}}
      />
    );

    expect(html).toContain('Resolve Agricultural Advisory');
    expect(html).toContain('CHEMICAL_SPRAYING_WIND_RISK');
    expect(html).toContain('High-Wind Spray Drift Risk');
    expect(html).toContain('Mark Resolved');
    expect(html).toContain('Cancel');
  });

  it('ResolveAdvisoryModal renders null when isOpen is false', () => {
    const html = renderToString(
      <ResolveAdvisoryModal
        isOpen={false}
        advisory={mockActiveAdvisory}
        onClose={() => {}}
        onConfirm={() => {}}
      />
    );

    expect(html).toBe('');
  });
});
