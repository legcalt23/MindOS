/**
 * admin-rbac.test.ts
 *
 * Covers:
 *  - hasPermission: each role has its expected permissions; non-role gets none
 *  - requireAdmin: rejects unauthenticated caller
 *  - requireAdmin: rejects caller with no admin_role custom claim
 *  - requireAdmin: rejects caller whose role lacks the requested permission
 *  - requireAdmin: accepts caller whose role has the permission (returns identity)
 *  - requireAdmin: each role tier (support/admin/super_admin) hits its boundaries
 *  - 2FA hook: no-op when disabled; rejects when enabled + missing/stale 2FA;
 *    accepts when enabled + recent 2FA
 *  - IP allowlist hook: no-op when disabled; rejects off-allowlist when enabled;
 *    accepts on-allowlist when enabled
 *  - admin.manage_roles: ONLY super_admin has it (deny escalation by lower tiers)
 */

jest.mock('firebase-functions', () => ({
  logger: { warn: jest.fn(), info: jest.fn(), error: jest.fn() },
  https: {
    HttpsError: class extends Error {
      code: string;
      constructor(code: string, message: string) {
        super(message);
        this.code = code;
      }
    },
  },
}));

import {
  hasPermission,
  requireAdmin,
  checkTwoFactorIfRequired,
  checkIpAllowlistIfRequired,
  ROLE_PERMISSIONS,
  ADMIN_SECURITY_CONFIG,
  shouldHardenAdminByDefault,
  AdminRole,
  AdminPermission,
} from './lib/adminAuth';

// ── Test helpers ──────────────────────────────────────────────────────────────

type Ctx = Parameters<typeof requireAdmin>[0];

function ctx(opts: {
  uid?: string;
  role?: string;
  mfa_verified_at?: number;
} = {}): Ctx {
  if (opts.uid === undefined && opts.role === undefined && opts.mfa_verified_at === undefined) {
    return { auth: undefined } as unknown as Ctx;
  }
  const token: Record<string, unknown> = {};
  if (opts.role !== undefined) token['admin_role'] = opts.role;
  if (opts.mfa_verified_at !== undefined) token['mfa_verified_at'] = opts.mfa_verified_at;
  return {
    auth: {
      uid: opts.uid ?? 'admin-user-1',
      token,
    },
  } as unknown as Ctx;
}

function resetConfig() {
  ADMIN_SECURITY_CONFIG.require2FA = false;
  ADMIN_SECURITY_CONFIG.requireIpAllowlist = false;
  ADMIN_SECURITY_CONFIG.ipAllowlist = [];
  ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs = 10 * 60 * 1000;
}

beforeEach(() => {
  jest.clearAllMocks();
  resetConfig();
});

// ── hasPermission ─────────────────────────────────────────────────────────────

describe('hasPermission', () => {
  it('super_admin has all permissions', () => {
    const all: AdminPermission[] = [
      'wallet.read', 'wallet.freeze', 'wallet.unfreeze',
      'wallet.adjustment.request', 'wallet.adjustment.approve',
      'wallet.adjustment.execute_low',
      'audit.read', 'fraud_signal.read', 'admin.manage_roles',
    ];
    for (const p of all) {
      expect(hasPermission('super_admin', p)).toBe(true);
    }
  });

  it('admin has all except admin.manage_roles', () => {
    expect(hasPermission('admin', 'wallet.freeze')).toBe(true);
    expect(hasPermission('admin', 'wallet.adjustment.approve')).toBe(true);
    expect(hasPermission('admin', 'admin.manage_roles')).toBe(false); // ESCALATION GUARD
  });

  it('support has only read permissions', () => {
    expect(hasPermission('support', 'wallet.read')).toBe(true);
    expect(hasPermission('support', 'audit.read')).toBe(true);
    expect(hasPermission('support', 'wallet.freeze')).toBe(false);
    expect(hasPermission('support', 'wallet.adjustment.request')).toBe(false);
    expect(hasPermission('support', 'wallet.adjustment.approve')).toBe(false);
    expect(hasPermission('support', 'admin.manage_roles')).toBe(false);
  });

  it('ROLE_PERMISSIONS catalog is non-empty for every role', () => {
    expect(ROLE_PERMISSIONS['super_admin'].size).toBeGreaterThan(0);
    expect(ROLE_PERMISSIONS['admin'].size).toBeGreaterThan(0);
    expect(ROLE_PERMISSIONS['support'].size).toBeGreaterThan(0);
  });
});

// ── requireAdmin: deny paths ──────────────────────────────────────────────────

describe('requireAdmin — deny by default', () => {
  it('rejects unauthenticated caller', () => {
    expect(() => requireAdmin(ctx(), 'wallet.read')).toThrow(/Administrative permission denied/);
  });

  it('rejects caller with no admin_role claim', () => {
    expect(() => requireAdmin(ctx({ uid: 'u' }), 'wallet.read'))
      .toThrow(/Administrative permission denied/);
  });

  it('rejects caller with invalid admin_role value', () => {
    expect(() => requireAdmin(ctx({ uid: 'u', role: 'pirate' }), 'wallet.read'))
      .toThrow(/Administrative permission denied/);
  });

  it('rejects support trying to freeze a wallet', () => {
    expect(() => requireAdmin(ctx({ role: 'support' }), 'wallet.freeze'))
      .toThrow(/Administrative permission denied/);
  });

  it('rejects admin trying to manage roles (escalation guard)', () => {
    expect(() => requireAdmin(ctx({ role: 'admin' }), 'admin.manage_roles'))
      .toThrow(/Administrative permission denied/);
  });
});

// ── requireAdmin: allow paths ─────────────────────────────────────────────────

describe('requireAdmin — allow paths', () => {
  it.each<[AdminRole, AdminPermission]>([
    ['support',     'wallet.read'],
    ['support',     'audit.read'],
    ['admin',       'wallet.freeze'],
    ['admin',       'wallet.adjustment.approve'],
    ['super_admin', 'admin.manage_roles'],
    ['super_admin', 'wallet.adjustment.execute_low'],
  ])('grants %s -> %s and returns identity', (role, permission) => {
    const identity = requireAdmin(ctx({ uid: 'op-1', role }), permission);
    expect(identity).toEqual({ uid: 'op-1', role });
  });
});

// ── 2FA hook ──────────────────────────────────────────────────────────────────

describe('checkTwoFactorIfRequired', () => {
  it('is a no-op when 2FA disabled', () => {
    ADMIN_SECURITY_CONFIG.require2FA = false;
    expect(() => checkTwoFactorIfRequired({ uid: 'u' })).not.toThrow();
  });

  it('rejects when 2FA required but mfa_verified_at missing', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    expect(() => checkTwoFactorIfRequired({ uid: 'u' })).toThrow(/Administrative/);
  });

  it('rejects when 2FA required but mfa_verified_at is stale', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs = 60_000; // 1 min
    const tooOld = Date.now() - 5 * 60_000;
    expect(() => checkTwoFactorIfRequired({ uid: 'u', mfa_verified_at: tooOld }))
      .toThrow(/Administrative/);
  });

  it('accepts when 2FA required and mfa_verified_at is recent', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs = 10 * 60_000;
    expect(() =>
      checkTwoFactorIfRequired({ uid: 'u', mfa_verified_at: Date.now() - 1000 }),
    ).not.toThrow();
  });

  it('rejects a future-dated mfa_verified_at beyond skew tolerance (M16)', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs = 10 * 60_000;
    const future = Date.now() + 10 * 60_000; // 10 min ahead — cannot be a real recent check
    expect(() =>
      checkTwoFactorIfRequired({ uid: 'u', mfa_verified_at: future }),
    ).toThrow(/Administrative/);
  });

  it('tolerates small clock skew on mfa_verified_at (M16)', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs = 10 * 60_000;
    const slightlyFuture = Date.now() + 5_000; // 5s ahead, within tolerance
    expect(() =>
      checkTwoFactorIfRequired({ uid: 'u', mfa_verified_at: slightlyFuture }),
    ).not.toThrow();
  });
});

// ── IP allowlist hook ─────────────────────────────────────────────────────────

describe('checkIpAllowlistIfRequired', () => {
  it('is a no-op when allowlist disabled', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = false;
    expect(() => checkIpAllowlistIfRequired('u', undefined)).not.toThrow();
  });

  it('rejects when allowlist required but no IP provided', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = ['10.0.0.1'];
    expect(() => checkIpAllowlistIfRequired('u', undefined)).toThrow(/Administrative/);
  });

  it('rejects when allowlist required and caller IP not on list', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = ['10.0.0.1'];
    expect(() => checkIpAllowlistIfRequired('u', '203.0.113.99')).toThrow(/Administrative/);
  });

  it('accepts when caller IP is on the allowlist', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = ['10.0.0.1', '10.0.0.2'];
    expect(() => checkIpAllowlistIfRequired('u', '10.0.0.2')).not.toThrow();
  });

  it('rejects when allowlist enabled but list is empty (true deny-by-default)', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = [];
    expect(() => checkIpAllowlistIfRequired('u', '10.0.0.1')).toThrow(/Administrative/);
  });

  it('denies on empty allowlist regardless of whether an IP is supplied (H15)', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = [];
    // Empty-allowlist misconfig is caught before the per-IP check, so it denies
    // even when no IP is provided.
    expect(() => checkIpAllowlistIfRequired('u', undefined)).toThrow(/Administrative/);
  });
});

describe('production hardening defaults', () => {
  it('requires admin hardening by default in the production Firebase project', () => {
    expect(shouldHardenAdminByDefault({
      NODE_ENV: 'production',
      GCLOUD_PROJECT: 'mindos-8452f',
    } as NodeJS.ProcessEnv)).toBe(true);
  });

  it('does not harden by default in tests or emulator', () => {
    expect(shouldHardenAdminByDefault({
      NODE_ENV: 'test',
      GCLOUD_PROJECT: 'mindos-8452f',
    } as NodeJS.ProcessEnv)).toBe(false);
    expect(shouldHardenAdminByDefault({
      NODE_ENV: 'production',
      FUNCTIONS_EMULATOR: 'true',
      GCLOUD_PROJECT: 'mindos-8452f',
    } as NodeJS.ProcessEnv)).toBe(false);
  });
});

// ── requireAdmin integrates 2FA and IP hooks ─────────────────────────────────

describe('requireAdmin — security hook integration', () => {
  it('rejects when 2FA enabled but token has no mfa_verified_at', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    expect(() =>
      requireAdmin(ctx({ uid: 'op', role: 'admin' }), 'wallet.freeze'),
    ).toThrow(/Administrative/);
  });

  it('accepts when 2FA enabled and token has recent mfa_verified_at', () => {
    ADMIN_SECURITY_CONFIG.require2FA = true;
    const id = requireAdmin(
      ctx({ uid: 'op', role: 'admin', mfa_verified_at: Date.now() - 1_000 }),
      'wallet.freeze',
    );
    expect(id).toEqual({ uid: 'op', role: 'admin' });
  });

  it('rejects when IP allowlist enabled and caller IP not provided', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = ['10.0.0.1'];
    expect(() =>
      requireAdmin(ctx({ uid: 'op', role: 'admin' }), 'wallet.freeze'),
    ).toThrow(/Administrative/);
  });

  it('accepts when IP allowlist enabled and caller IP on list', () => {
    ADMIN_SECURITY_CONFIG.requireIpAllowlist = true;
    ADMIN_SECURITY_CONFIG.ipAllowlist = ['10.0.0.1'];
    const id = requireAdmin(
      ctx({ uid: 'op', role: 'admin' }),
      'wallet.freeze',
      '10.0.0.1',
    );
    expect(id).toEqual({ uid: 'op', role: 'admin' });
  });
});
