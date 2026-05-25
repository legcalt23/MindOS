/**
 * wallet-fraud-signals.test.ts
 *
 * Covers:
 *  - sanitizeMetadata: PII fields are hashed, non-PII fields pass through
 *  - clampScore: values outside [0,100] are clamped; decimals rounded
 *  - FraudSignalService.record: calls repository with sanitized metadata
 *  - Schema guards: isValidFraudSignalSeverity / isValidFraudSignalSource
 *  - Repository.append: called with correct severity, source, and score
 *  - Insertion of signals with each severity level
 */

// ── Mocks ─────────────────────────────────────────────────────────────────────

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
  })),
}));

jest.mock('firebase-functions', () => ({
  logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

const mockAppend = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  FraudSignalRepository: {
    append: (...args: unknown[]) => mockAppend(...args),
  },
}));

// ── Imports ───────────────────────────────────────────────────────────────────

import {
  sanitizeMetadata,
  clampScore,
  FraudSignalService,
} from './lib/fraudSignalService';
import {
  isValidFraudSignalSeverity,
  isValidFraudSignalSource,
} from './lib/walletSchema';

// ── sanitizeMetadata ──────────────────────────────────────────────────────────

describe('sanitizeMetadata', () => {
  it('hashes "ip" field and removes the raw value', () => {
    const result = sanitizeMetadata({ ip: '203.0.113.42' });
    expect(result['ip']).toBeUndefined();
    expect(typeof result['ipHash']).toBe('string');
    expect((result['ipHash'] as string)).toMatch(/^[a-f0-9]{64}$/);
  });

  it('hashes "deviceId" and replaces with deviceIdHash', () => {
    const result = sanitizeMetadata({ deviceId: 'ABC123' });
    expect(result['deviceId']).toBeUndefined();
    expect(result['deviceIdHash']).toBeDefined();
  });

  it('hashes "deviceFingerprint" field', () => {
    const result = sanitizeMetadata({ deviceFingerprint: 'fp-xyz' });
    expect(result['deviceFingerprintHash']).toBeDefined();
    expect(result['deviceFingerprint']).toBeUndefined();
  });

  it('hashes "userAgent" field', () => {
    const result = sanitizeMetadata({ userAgent: 'Mozilla/5.0' });
    expect(result['userAgentHash']).toBeDefined();
    expect(result['userAgent']).toBeUndefined();
  });

  it('passes through non-PII fields unchanged', () => {
    const result = sanitizeMetadata({ packId: 'coins_10', count: 3 });
    expect(result['packId']).toBe('coins_10');
    expect(result['count']).toBe(3);
  });

  it('handles mixed PII and non-PII fields correctly', () => {
    const result = sanitizeMetadata({
      ip: '1.2.3.4',
      packId: 'coins_100',
      purchaseCount: 5,
    });
    expect(result['ip']).toBeUndefined();
    expect(result['ipHash']).toBeDefined();
    expect(result['packId']).toBe('coins_100');
    expect(result['purchaseCount']).toBe(5);
  });

  it('produces deterministic hashes for the same PII value', () => {
    const r1 = sanitizeMetadata({ ip: '10.0.0.1' });
    const r2 = sanitizeMetadata({ ip: '10.0.0.1' });
    expect(r1['ipHash']).toBe(r2['ipHash']);
  });

  it('produces different hashes for different PII values', () => {
    const r1 = sanitizeMetadata({ ip: '10.0.0.1' });
    const r2 = sanitizeMetadata({ ip: '10.0.0.2' });
    expect(r1['ipHash']).not.toBe(r2['ipHash']);
  });

  it('returns empty object for empty input', () => {
    expect(sanitizeMetadata({})).toEqual({});
  });
});

// ── clampScore ────────────────────────────────────────────────────────────────

describe('clampScore', () => {
  it('clamps negative values to 0', () => {
    expect(clampScore(-10)).toBe(0);
    expect(clampScore(-999)).toBe(0);
  });

  it('clamps values above 100 to 100', () => {
    expect(clampScore(101)).toBe(100);
    expect(clampScore(9999)).toBe(100);
  });

  it('rounds decimal values to nearest integer', () => {
    expect(clampScore(49.9)).toBe(50);
    expect(clampScore(50.4)).toBe(50);
    expect(clampScore(70.6)).toBe(71);
  });

  it('passes through values within [0, 100]', () => {
    expect(clampScore(0)).toBe(0);
    expect(clampScore(50)).toBe(50);
    expect(clampScore(100)).toBe(100);
  });
});

// ── Schema type guards ────────────────────────────────────────────────────────

describe('isValidFraudSignalSeverity', () => {
  it('accepts all valid severities', () => {
    for (const s of ['low', 'medium', 'high', 'critical']) {
      expect(isValidFraudSignalSeverity(s)).toBe(true);
    }
  });

  it('rejects invalid values', () => {
    for (const s of ['', 'LOW', 'none', null, undefined, 0]) {
      expect(isValidFraudSignalSeverity(s)).toBe(false);
    }
  });
});

describe('isValidFraudSignalSource', () => {
  it('accepts all valid sources', () => {
    for (const s of ['velocity', 'device', 'ip', 'behavior', 'attestation', 'manual']) {
      expect(isValidFraudSignalSource(s)).toBe(true);
    }
  });

  it('rejects invalid values', () => {
    for (const s of ['', 'VELOCITY', 'unknown', null, 42]) {
      expect(isValidFraudSignalSource(s)).toBe(false);
    }
  });
});

// ── FraudSignalService.record ─────────────────────────────────────────────────

describe('FraudSignalService.record', () => {
  beforeEach(() => {
    mockAppend.mockReset();
    mockAppend.mockResolvedValue('signal-001');
  });

  it('calls repository with correct arguments', async () => {
    await FraudSignalService.record({
      walletId:     'user-abc',
      userId:       'user-abc',
      signalType:   'velocity_purchases',
      severity:     'medium',
      riskScore:    40,
      source:       'velocity',
      metadata:     { purchaseCount: 6 },
      correlationId: 'corr-1',
    });

    expect(mockAppend).toHaveBeenCalledWith(
      'user-abc',         // walletId
      'user-abc',         // userId
      'velocity_purchases', // signalType
      'medium',           // severity
      40,                 // riskScore (already clamped)
      'velocity',         // source
      { purchaseCount: 6 }, // sanitized metadata (no PII here)
    );
  });

  it('clamps out-of-range riskScore before storing', async () => {
    await FraudSignalService.record({
      walletId: 'u1', userId: 'u1',
      signalType: 'test', severity: 'low',
      riskScore: 150,   // out of range
      source: 'manual', metadata: {}, correlationId: 'c1',
    });
    const [,,,,score] = mockAppend.mock.calls[0];
    expect(score).toBe(100);
  });

  it('sanitizes PII in metadata before calling repository', async () => {
    await FraudSignalService.record({
      walletId: 'u1', userId: 'u1',
      signalType: 'ip_anomaly', severity: 'high',
      riskScore: 60, source: 'ip',
      metadata: { ip: '1.2.3.4', requestCount: 10 },
      correlationId: 'c2',
    });
    const [,,,,, , metadataArg] = mockAppend.mock.calls[0];
    expect(metadataArg['ip']).toBeUndefined();
    expect(metadataArg['ipHash']).toBeDefined();
    expect(metadataArg['requestCount']).toBe(10);
  });

  it('returns the signalId from repository', async () => {
    mockAppend.mockResolvedValue('sig-xyz');
    const id = await FraudSignalService.record({
      walletId: 'u1', userId: 'u1',
      signalType: 'test', severity: 'low',
      riskScore: 5, source: 'behavior',
      metadata: {}, correlationId: 'c3',
    });
    expect(id).toBe('sig-xyz');
  });

  // Severity levels — one test per tier to verify enum acceptance

  it.each([
    ['low',      5,  'behavior'],
    ['medium',   40, 'velocity'],
    ['high',     65, 'ip'],
    ['critical', 90, 'device'],
  ] as const)('records a %s severity signal (score=%i, source=%s)', async (severity, score, source) => {
    mockAppend.mockResolvedValue('sid');
    await FraudSignalService.record({
      walletId: 'wx', userId: 'wx',
      signalType: `test_${severity}`,
      severity, riskScore: score, source,
      metadata: {}, correlationId: 'c',
    });
    const [,,,severityArg, scoreArg, sourceArg] = mockAppend.mock.calls[0];
    expect(severityArg).toBe(severity);
    expect(scoreArg).toBe(score);
    expect(sourceArg).toBe(source);
  });
});
