/**
 * adminAuth.ts
 * Role-Based Access Control (RBAC) for administrative wallet operations.
 *
 * Security contract:
 *  - DENY BY DEFAULT. Any caller without an explicit admin role and the
 *    requested permission is rejected with PERMISSION_DENIED.
 *  - Roles are read from Firebase Auth custom claims (`context.auth.token`).
 *    Custom claims can only be set server-side (Admin SDK), so they cannot
 *    be forged by clients.
 *  - 2FA and IP-allowlist enforcement are CONFIGURABLE HOOKS:
 *      - They are NO-OPS by default (config disabled).
 *      - When enabled, missing 2FA or off-allowlist IP fails the check.
 *      - Real 2FA / IP-allowlist providers (e.g., Authy, Cloudflare WARP)
 *        are out of scope for this phase but the integration points exist.
 *  - Every admin auth failure is logged. Successes are NOT logged here
 *    (the calling admin endpoint emits its own audit log).
 */

import * as functions from 'firebase-functions';

// ── Roles ─────────────────────────────────────────────────────────────────────

/**
 * Closed set of admin roles. Custom claim is `admin_role`.
 * Tier ordering (most → least privileged):
 *   super_admin > admin > support > (no role)
 */
export type AdminRole = 'super_admin' | 'admin' | 'support';

// ── Permissions ───────────────────────────────────────────────────────────────

/**
 * Closed set of admin permissions. Each permission gates exactly one
 * administrative capability. Roles map to bundles of permissions.
 */
export type AdminPermission =
  | 'wallet.read'              // view any wallet's balance/transactions
  | 'wallet.freeze'            // freeze a wallet
  | 'wallet.unfreeze'          // unfreeze a wallet
  | 'wallet.adjustment.request'   // request a maker-side ledger adjustment
  | 'wallet.adjustment.approve'   // approve another admin's adjustment
  | 'wallet.adjustment.execute_low'  // execute a low-value pre-approved adjustment
  | 'audit.read'               // read audit logs
  | 'fraud_signal.read'        // read fraud signals
  | 'admin.manage_roles';      // grant/revoke admin roles (super_admin only)

// ── Role → Permissions catalog ────────────────────────────────────────────────

/**
 * Static role-to-permissions map. Used by `hasPermission`.
 *
 * Principle: each role grants the MINIMUM permissions needed for its tier.
 * Super-admin is the only role that can mutate the role catalog itself.
 */
export const ROLE_PERMISSIONS: Record<AdminRole, ReadonlySet<AdminPermission>> = {
  super_admin: new Set<AdminPermission>([
    'wallet.read',
    'wallet.freeze',
    'wallet.unfreeze',
    'wallet.adjustment.request',
    'wallet.adjustment.approve',
    'wallet.adjustment.execute_low',
    'audit.read',
    'fraud_signal.read',
    'admin.manage_roles',
  ]),
  admin: new Set<AdminPermission>([
    'wallet.read',
    'wallet.freeze',
    'wallet.unfreeze',
    'wallet.adjustment.request',
    'wallet.adjustment.approve',
    'wallet.adjustment.execute_low',
    'audit.read',
    'fraud_signal.read',
  ]),
  support: new Set<AdminPermission>([
    'wallet.read',
    'audit.read',
  ]),
};

// ── Config hooks (2FA / IP allowlist) ─────────────────────────────────────────

/**
 * Configuration for optional security enforcement layers.
 * In production, this should be loaded from Firebase Functions runtime config.
 * For now, defaults are disabled so existing flows continue to work.
 *
 * Exported as a mutable object so tests can flip flags.
 */
export const ADMIN_SECURITY_CONFIG = {
  /** When true, callers must present a recent-2FA claim in their token. */
  require2FA: false,
  /** When true, callers must originate from an allowlisted IP. */
  requireIpAllowlist: false,
  /** IP allowlist (CIDR strings or exact IPs). Empty = deny if requireIpAllowlist=true. */
  ipAllowlist: [] as string[],
  /**
   * Maximum age (ms) of a 2FA challenge to still be considered "recent".
   * Default: 10 minutes.
   */
  twoFactorMaxAgeMs: 10 * 60 * 1000,
};

// ── Types ─────────────────────────────────────────────────────────────────────

/**
 * Subset of the Firebase Auth token we care about for admin checks.
 * The `admin_role` claim is set by a separate role-management function
 * using the Admin SDK (never by clients).
 *
 * `auth_time` is standard Firebase Auth (seconds since epoch).
 * `mfa_verified_at` is a custom claim we expect from our 2FA pipeline.
 */
export interface AdminAuthToken {
  uid: string;
  admin_role?: AdminRole;
  /** When 2FA was last verified (ms since epoch). Set by 2FA service. */
  mfa_verified_at?: number;
}

/** Parsed admin identity, returned by requireAdmin. */
export interface AdminIdentity {
  uid: string;
  role: AdminRole;
}

// ── Internal helpers ──────────────────────────────────────────────────────────

function denied(reason: string): never {
  // Reason is intentionally vague to the client to prevent enumeration of policy.
  // The full reason is captured in logs for the admin audit pipeline.
  functions.logger.warn(`Admin RBAC denied: ${reason}`);
  throw new functions.https.HttpsError(
    'permission-denied',
    'Administrative permission denied.',
  );
}

function readAdminToken(context: functions.https.CallableContext): AdminAuthToken {
  if (!context.auth) denied('no_auth');
  const token = context.auth.token as Record<string, unknown>;
  return {
    uid:             context.auth.uid,
    admin_role:      typeof token['admin_role'] === 'string'
      ? token['admin_role'] as AdminRole
      : undefined,
    mfa_verified_at: typeof token['mfa_verified_at'] === 'number'
      ? token['mfa_verified_at'] as number
      : undefined,
  };
}

function isValidRole(r: unknown): r is AdminRole {
  return r === 'super_admin' || r === 'admin' || r === 'support';
}

// ── Public helpers (also exported for tests) ──────────────────────────────────

/**
 * Returns true if the given role has the requested permission.
 * Pure function — no side effects.
 */
export function hasPermission(role: AdminRole, permission: AdminPermission): boolean {
  const granted = ROLE_PERMISSIONS[role];
  return granted ? granted.has(permission) : false;
}

/**
 * Verifies 2FA hook if config requires it.
 * No-op when ADMIN_SECURITY_CONFIG.require2FA is false.
 * Throws PERMISSION_DENIED if 2FA is required but missing/stale.
 */
export function checkTwoFactorIfRequired(token: AdminAuthToken): void {
  if (!ADMIN_SECURITY_CONFIG.require2FA) return;
  const age = token.mfa_verified_at
    ? Date.now() - token.mfa_verified_at
    : Number.POSITIVE_INFINITY;
  if (!token.mfa_verified_at || age > ADMIN_SECURITY_CONFIG.twoFactorMaxAgeMs) {
    denied(`2fa_required_or_stale uid=${token.uid}`);
  }
}

/**
 * Verifies IP allowlist hook if config requires it.
 * No-op when ADMIN_SECURITY_CONFIG.requireIpAllowlist is false.
 * Throws PERMISSION_DENIED if allowlist is enabled but IP missing or not on list.
 *
 * IP is passed explicitly (not read from headers) because Firebase callable
 * context does not expose it portably across runtimes. The calling endpoint
 * must pull it from `context.rawRequest.ip` (gen-1) or `request.ip` (gen-2)
 * and forward it here.
 *
 * Allowlist matching is exact-string only for now. CIDR matching is a future
 * enhancement (would require an external dependency we deliberately avoid).
 */
export function checkIpAllowlistIfRequired(uid: string, ip: string | undefined): void {
  if (!ADMIN_SECURITY_CONFIG.requireIpAllowlist) return;
  if (!ip) denied(`ip_missing uid=${uid}`);
  if (!ADMIN_SECURITY_CONFIG.ipAllowlist.includes(ip)) {
    denied(`ip_not_allowlisted uid=${uid}`);
  }
}

// ── Main middleware ───────────────────────────────────────────────────────────

/**
 * Verifies that the caller has the requested admin permission.
 *
 * Order of checks (DENY BY DEFAULT — any failure short-circuits):
 *   1. Caller must be authenticated.
 *   2. Caller must have a valid admin_role custom claim.
 *   3. Role must include the requested permission.
 *   4. 2FA hook (if config requires it).
 *   5. IP allowlist hook (if config requires it AND caller passed ip).
 *
 * Returns the verified admin identity.
 */
export function requireAdmin(
  context: functions.https.CallableContext,
  permission: AdminPermission,
  callerIp?: string,
): AdminIdentity {
  const token = readAdminToken(context);

  if (!isValidRole(token.admin_role)) {
    denied(`no_admin_role uid=${token.uid}`);
  }

  if (!hasPermission(token.admin_role, permission)) {
    denied(`missing_permission uid=${token.uid} role=${token.admin_role} perm=${permission}`);
  }

  checkTwoFactorIfRequired(token);
  checkIpAllowlistIfRequired(token.uid, callerIp);

  return { uid: token.uid, role: token.admin_role };
}
