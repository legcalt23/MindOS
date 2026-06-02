/**
 * adminFunctions.ts
 * Callable Cloud Functions for administrative wallet operations.
 *
 * Security envelope for every callable:
 *   requireAppCheck → requireAdmin(<permission>) → validateInput → service call
 *
 * No callable accepts a balance or final-state field from the client.
 * All decisions about amounts and target wallets come from the verified
 * admin's request body + the server-side wallet state.
 */

import * as functions from 'firebase-functions';
import { requireAppCheck } from './lib/guards';
import { requireAdmin } from './lib/adminAuth';
import { checkRateLimitFailClosed } from './lib/rateLimiter';
import { resolveCorrelationId } from './lib/correlationId';
import { walletError } from './lib/walletErrors';
import { walletLog } from './lib/walletLogger';
import {
  AdminAdjustmentService,
  MAX_ADJUSTMENT_AMOUNT,
  MIN_JUSTIFICATION_LENGTH,
} from './lib/adminAdjustmentService';

function callerIp(context: functions.https.CallableContext): string | undefined {
  const raw = context.rawRequest;
  const forwarded = raw?.headers?.['x-forwarded-for'];
  if (typeof forwarded === 'string' && forwarded.length > 0) {
    return forwarded.split(',')[0].trim();
  }
  if (Array.isArray(forwarded) && forwarded.length > 0) {
    return forwarded[0].split(',')[0].trim();
  }
  return raw?.ip;
}

// ── Input validation ──────────────────────────────────────────────────────────

interface RequestAdjustmentInput {
  targetWalletId: string;
  amount: number;
  justification: string;
  correlationId?: string;
}

interface ApproveAdjustmentInput {
  actionId: string;
  allowSelfExecuteLow?: boolean;
  correlationId?: string;
}

function validateRequestAdjustment(data: unknown, correlationId: string): RequestAdjustmentInput {
  if (!data || typeof data !== 'object') {
    throw walletError('INVALID_WALLET_INPUT', correlationId);
  }
  const d = data as Record<string, unknown>;

  if (typeof d['targetWalletId'] !== 'string' || d['targetWalletId'].length === 0) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'targetWalletId');
  }
  if (typeof d['amount'] !== 'number' || !Number.isInteger(d['amount'])) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'amount');
  }
  if (d['amount'] === 0 || Math.abs(d['amount']) > MAX_ADJUSTMENT_AMOUNT) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'amount');
  }
  if (typeof d['justification'] !== 'string'
      || d['justification'].trim().length < MIN_JUSTIFICATION_LENGTH) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'justification');
  }

  return {
    targetWalletId: d['targetWalletId'] as string,
    amount:         d['amount'] as number,
    justification:  d['justification'] as string,
    correlationId:  typeof d['correlationId'] === 'string'
      ? d['correlationId'] as string
      : undefined,
  };
}

function validateApproveAdjustment(data: unknown, correlationId: string): ApproveAdjustmentInput {
  if (!data || typeof data !== 'object') {
    throw walletError('INVALID_WALLET_INPUT', correlationId);
  }
  const d = data as Record<string, unknown>;

  if (typeof d['actionId'] !== 'string' || d['actionId'].length === 0) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'actionId');
  }

  return {
    actionId:            d['actionId'] as string,
    allowSelfExecuteLow: d['allowSelfExecuteLow'] === true,
    correlationId:       typeof d['correlationId'] === 'string'
      ? d['correlationId'] as string
      : undefined,
  };
}

// ── adminRequestWalletAdjustment ──────────────────────────────────────────────

/**
 * Maker step. Records a pending adjustment for later approval by another admin.
 *
 * Required permission: `wallet.adjustment.request`
 *
 * Response: { actionId, status: 'pending', correlationId }
 *
 * Side effects:
 *  - Creates admin_actions/{actionId} (status='pending')
 *  - Emits 'admin_adjustment_requested' audit
 */
export const adminRequestWalletAdjustment = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const admin = requireAdmin(context, 'wallet.adjustment.request', callerIp(context));

  // Fail-closed rate limit. Treated as an economic action so infrastructure
  // failure denies the call rather than silently allowing it.
  await checkRateLimitFailClosed(admin.uid, 'adminRequestWalletAdjustment');

  const correlationId = resolveCorrelationId(data);
  const input = validateRequestAdjustment(data, correlationId);

  walletLog.info(
    { correlationId, action: 'adminRequestWalletAdjustment', userId: admin.uid },
    'Admin adjustment requested',
    { targetWalletId: input.targetWalletId, amount: input.amount },
  );

  const result = await AdminAdjustmentService.requestAdjustment({
    adminId:        admin.uid,
    targetWalletId: input.targetWalletId,
    amount:         input.amount,
    justification:  input.justification,
    correlationId,
  });

  return { ...result, correlationId };
});

// ── adminApproveWalletAdjustment ──────────────────────────────────────────────

/**
 * Checker step. Approves a pending adjustment.
 *
 * Required permission: `wallet.adjustment.approve`
 *
 * Approval flow:
 *  - If approver UID == maker UID AND amount ≤ AUTO_EXECUTE_THRESHOLD AND
 *    `allowSelfExecuteLow=true` AND caller also holds
 *    `wallet.adjustment.execute_low`, the adjustment is executed by the maker.
 *  - Otherwise, approver UID MUST differ from maker UID. Self-approval attempts
 *    are rejected with FRAUD_DETECTED, the action is marked 'rejected',
 *    and the attempt is audited.
 *
 * Response: { actionId, status: 'executed', ledgerTransactionId, correlationId }
 */
export const adminApproveWalletAdjustment = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const approverIdentity = requireAdmin(context, 'wallet.adjustment.approve', callerIp(context));

  // Fail-closed rate limit. Approval is a balance-mutating economic action.
  await checkRateLimitFailClosed(approverIdentity.uid, 'adminApproveWalletAdjustment');

  const correlationId = resolveCorrelationId(data);
  const input = validateApproveAdjustment(data, correlationId);

  // If the caller requested self-execute on a low-value adjustment, the caller
  // ALSO needs `wallet.adjustment.execute_low`. We re-check here to avoid
  // relying on the service-layer permission check alone.
  if (input.allowSelfExecuteLow) {
    // This will throw PERMISSION_DENIED if missing, before any state mutation.
    requireAdmin(context, 'wallet.adjustment.execute_low', callerIp(context));
  }

  walletLog.info(
    { correlationId, action: 'adminApproveWalletAdjustment', userId: approverIdentity.uid },
    'Admin adjustment approval requested',
    { actionId: input.actionId, allowSelfExecuteLow: input.allowSelfExecuteLow ?? false },
  );

  const result = await AdminAdjustmentService.approveAdjustment({
    adminId:             approverIdentity.uid,
    actionId:            input.actionId,
    allowSelfExecuteLow: input.allowSelfExecuteLow ?? false,
    correlationId,
  });

  return { ...result, correlationId };
});
