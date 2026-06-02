/**
 * audit-remediation.test.ts
 *
 * Regression tests for findings from docs/security/final-audit-report.md.
 *
 *   REM-H1: admin callables must call checkRateLimitFailClosed.
 *   REM-M1: reconciliationService must reject prototype-chain bucket values.
 *
 * Failing a test here means the audit finding has REGRESSED.
 */

// ── REM-M1 — reconciliationService prototype-chain bypass ────────────────────

describe('REM-M1 — computeBalancesFromEntries rejects prototype-chain buckets', () => {
  // Pure function — no mocks needed.
  /* eslint-disable @typescript-eslint/no-var-requires */
  const { computeBalancesFromEntries } = require('./lib/reconciliationService');

  function entry(bucket: string, amount: number) {
    return {
      entryId: 'e', transactionId: 't', walletId: 'w',
      entryType: 'credit', bucket, amount, runningBalance: 0,
      createdAt: 0, schemaVersion: 1,
    };
  }

  it('correctly sums valid buckets', () => {
    const out = computeBalancesFromEntries([
      entry('purchased',   10),
      entry('promotional',  5),
      entry('earned',       3),
      entry('purchased',   -2),
    ]);
    expect(out).toEqual({ purchased: 8, promotional: 5, earned: 3 });
  });

  it.each([
    '__proto__',
    'constructor',
    'toString',
    'hasOwnProperty',
    'valueOf',
    'isPrototypeOf',
    'propertyIsEnumerable',
  ])('SILENTLY IGNORES bucket "%s" without corrupting result or prototype', (badBucket) => {
    const before = JSON.stringify(Object.prototype);
    const out = computeBalancesFromEntries([
      entry('purchased', 10),
      entry(badBucket,  999), // attacker-controlled bucket via corrupted data
      entry('earned',     5),
    ]);
    // Bad bucket is ignored; valid ones still sum.
    expect(out).toEqual({ purchased: 10, promotional: 0, earned: 5 });
    // Object.prototype must NOT have been mutated.
    expect(JSON.stringify(Object.prototype)).toBe(before);
  });

  it('does NOT throw when an entry has a numeric-string bucket like "0"', () => {
    expect(() => computeBalancesFromEntries([entry('0' as string, 100)])).not.toThrow();
  });
});

// ── REM-H1 — admin callables must check rate limit ──────────────────────────

describe('REM-H1 — admin callables are rate-limited', () => {
  it('rateLimiter.RATE_LIMITS defines the admin actions', () => {
    /* eslint-disable @typescript-eslint/no-var-requires */
    const { RATE_LIMITS } = require('./lib/rateLimiter');
    expect(RATE_LIMITS).toHaveProperty('adminRequestWalletAdjustment');
    expect(RATE_LIMITS).toHaveProperty('adminApproveWalletAdjustment');
    expect(RATE_LIMITS['adminRequestWalletAdjustment'].maxCalls).toBeGreaterThan(0);
    expect(RATE_LIMITS['adminApproveWalletAdjustment'].maxCalls).toBeGreaterThan(0);
  });

  it('rateLimiter source classifies admin actions as ECONOMIC_ACTIONS (fail-closed)', () => {
    const fs   = require('fs') as typeof import('fs');
    const path = require('path') as typeof import('path');
    const src  = fs.readFileSync(path.join(__dirname, 'lib', 'rateLimiter.ts'), 'utf8');

    // ECONOMIC_ACTIONS set must include both admin actions
    const setBlock = src.match(/ECONOMIC_ACTIONS\s*=\s*new\s+Set\(\[([\s\S]*?)\]\)/);
    expect(setBlock).not.toBeNull();
    const setContent = setBlock![1];
    expect(setContent).toContain("'adminRequestWalletAdjustment'");
    expect(setContent).toContain("'adminApproveWalletAdjustment'");
  });

  it('adminFunctions.ts calls checkRateLimitFailClosed for both callables', () => {
    const fs   = require('fs') as typeof import('fs');
    const path = require('path') as typeof import('path');
    const src  = fs.readFileSync(path.join(__dirname, 'adminFunctions.ts'), 'utf8');

    // checkRateLimitFailClosed is imported
    expect(src).toMatch(/import.*checkRateLimitFailClosed.*from\s+['"]\.\/lib\/rateLimiter['"]/);

    // Both admin actions invoke it with their own name
    expect(src).toContain("checkRateLimitFailClosed(admin.uid, 'adminRequestWalletAdjustment')");
    expect(src).toContain("checkRateLimitFailClosed(approverIdentity.uid, 'adminApproveWalletAdjustment')");
  });

  it('the rate-limit call sits BEFORE validation in both callables', () => {
    // Pulling the call site after validation would allow attackers to
    // burn validator CPU before the rate limit gates the call.
    const fs   = require('fs') as typeof import('fs');
    const path = require('path') as typeof import('path');
    const src  = fs.readFileSync(path.join(__dirname, 'adminFunctions.ts'), 'utf8');

    // For each callable, the order must be: requireAdmin → checkRateLimitFailClosed → validate
    const requestBlock = src.match(/adminRequestWalletAdjustment[\s\S]*?validateRequestAdjustment/);
    expect(requestBlock).not.toBeNull();
    const requestSegment = requestBlock![0];
    const idxAdmin   = requestSegment.indexOf("requireAdmin(context, 'wallet.adjustment.request', callerIp(context))");
    const idxLimit   = requestSegment.indexOf("checkRateLimitFailClosed");
    const idxValid   = requestSegment.indexOf("validateRequestAdjustment");
    expect(idxAdmin).toBeGreaterThanOrEqual(0);
    expect(idxLimit).toBeGreaterThan(idxAdmin);
    expect(idxValid).toBeGreaterThan(idxLimit);

    const approveBlock = src.match(/adminApproveWalletAdjustment[\s\S]*?validateApproveAdjustment/);
    expect(approveBlock).not.toBeNull();
    const approveSegment = approveBlock![0];
    const idxAdmin2 = approveSegment.indexOf("requireAdmin(context, 'wallet.adjustment.approve', callerIp(context))");
    const idxLimit2 = approveSegment.indexOf("checkRateLimitFailClosed");
    const idxValid2 = approveSegment.indexOf("validateApproveAdjustment");
    expect(idxAdmin2).toBeGreaterThanOrEqual(0);
    expect(idxLimit2).toBeGreaterThan(idxAdmin2);
    expect(idxValid2).toBeGreaterThan(idxLimit2);
  });
});
