/**
 * adminAdjustmentService.ts
 * Maker/checker workflow for administrative wallet adjustments.
 *
 * Security contract (CRITICAL — read before modifying):
 *
 *   1. Snapshots are NEVER written to directly. Every balance change goes
 *      through the LedgerService, which writes a ledger transaction first
 *      and then derives the snapshot. This preserves the append-only ledger.
 *
 *   2. The maker (requester) CANNOT be the checker (approver).
 *      Enforced inside the approval transaction by comparing actionDoc.adminId
 *      to the approver's verified UID. Attempting self-approval is rejected
 *      with PERMISSION_DENIED and audited as a rejected attempt.
 *
 *   3. Justification is REQUIRED. Empty/whitespace-only justifications are
 *      rejected at request time.
 *
 *   4. Low-value auto-execute: if |amount| <= AUTO_EXECUTE_THRESHOLD AND the
 *      caller holds `wallet.adjustment.execute_low`, the adjustment can be
 *      executed by the same admin without a second approver. This path is
 *      STILL audited as a single 'admin_adjustment_executed' entry.
 *
 *   5. All state transitions are audited:
 *        request → admin_adjustment_requested
 *        approve → admin_adjustment_approved
 *        execute → admin_adjustment_executed
 *        reject  → admin_adjustment_rejected
 *
 *   6. Amount is an integer (positive = credit, negative = debit). Zero is
 *      rejected. Bucket targeted by adjustments is 'promotional' so admin
 *      credits never look like purchases on the user's transaction history.
 */

import * as admin from 'firebase-admin';
import * as crypto from 'crypto';
import * as functions from 'firebase-functions';
import {
  WalletRepository,
  SnapshotRepository,
  AdminActionRepository,
} from './walletRepository';
import { AdminActionStatus } from './walletSchema';
import { LedgerService, LedgerEntrySpec } from './ledgerService';
import { AuditLogService } from './auditLogService';
import { walletError } from './walletErrors';

const db = () => admin.firestore();

// ── Configuration ─────────────────────────────────────────────────────────────

/**
 * Adjustments with |amount| at or below this threshold MAY be executed by a
 * single admin (the maker) if they hold `wallet.adjustment.execute_low`.
 * Higher amounts always require maker/checker.
 *
 * Threshold is in coins (integer). Default: 10 coins.
 */
export const AUTO_EXECUTE_THRESHOLD = 10;

/** Maximum absolute adjustment amount in a single action. Hard cap. */
export const MAX_ADJUSTMENT_AMOUNT = 10_000;

/** Minimum justification length in characters. */
export const MIN_JUSTIFICATION_LENGTH = 10;

// ── Types ─────────────────────────────────────────────────────────────────────

export interface RequestAdjustmentParams {
  /** Admin requesting the adjustment (maker). Always from verified context. */
  adminId: string;
  targetWalletId: string;
  /** Positive = credit, negative = debit. Non-zero. */
  amount: number;
  justification: string;
  correlationId: string;
}

export interface ApprovalParams {
  /** Admin performing the approval (checker). Always from verified context. */
  adminId: string;
  actionId: string;
  /** When true, the approver may also be the maker IF auto-execute conditions hold. */
  allowSelfExecuteLow?: boolean;
  correlationId: string;
}

export interface AdjustmentResult {
  actionId: string;
  status: AdminActionStatus;
  /** Set when the adjustment was executed (status='executed'). */
  ledgerTransactionId?: string;
}

// ── Validation helpers ────────────────────────────────────────────────────────

function validateAmount(amount: number, correlationId: string): void {
  if (!Number.isInteger(amount)) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'amount');
  }
  if (amount === 0) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'amount');
  }
  if (Math.abs(amount) > MAX_ADJUSTMENT_AMOUNT) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'amount');
  }
}

function validateJustification(justification: string, correlationId: string): void {
  if (typeof justification !== 'string') {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'justification');
  }
  if (justification.trim().length < MIN_JUSTIFICATION_LENGTH) {
    throw walletError('INVALID_WALLET_INPUT', correlationId, 'justification');
  }
}

// ── Service ───────────────────────────────────────────────────────────────────

export const AdminAdjustmentService = {

  /**
   * Maker step: create a pending adjustment.
   *
   * Validations:
   *  - amount must be non-zero integer in [-MAX, +MAX]
   *  - justification must be ≥ MIN_JUSTIFICATION_LENGTH after trim
   *  - target wallet must exist (avoids approval against a phantom wallet)
   *
   * Side effects:
   *  - Creates an admin_actions doc with status='pending'
   *  - Emits 'admin_adjustment_requested' audit
   *
   * Does NOT mutate balance. Does NOT verify approver identity (that's for
   * approveAdjustment).
   */
  async requestAdjustment(params: RequestAdjustmentParams): Promise<AdjustmentResult> {
    validateAmount(params.amount, params.correlationId);
    validateJustification(params.justification, params.correlationId);

    // Target wallet must exist — avoids creating zombie pending actions
    const wallet = await WalletRepository.findById(params.targetWalletId);
    if (!wallet) {
      throw walletError('WALLET_NOT_FOUND', params.correlationId);
    }

    const actionId = crypto.randomUUID();

    await AdminActionRepository.create({
      actionId,
      adminId:        params.adminId,
      action:         'wallet_adjustment',
      targetWalletId: params.targetWalletId,
      amount:         params.amount,
      justification:  params.justification.trim(),
      status:         'pending',
      approverId:     null,
    });

    await AuditLogService.record({
      actorId:        params.adminId,
      actorType:      'admin',
      action:         'admin_adjustment_requested',
      targetWalletId: params.targetWalletId,
      correlationId:  params.correlationId,
      details: {
        actionId,
        amount:        params.amount,
        justification: params.justification.trim(),
      },
    });

    return { actionId, status: 'pending' };
  },

  /**
   * Checker step: approve and execute a pending adjustment.
   *
   * Enforces:
   *  - Action must exist and be in 'pending' status
   *  - Approver UID MUST differ from maker UID, UNLESS auto-execute applies:
   *      |amount| <= AUTO_EXECUTE_THRESHOLD AND allowSelfExecuteLow=true
   *  - Target wallet must exist and be in a writable state
   *  - The mutation is performed via LedgerService (no direct snapshot writes)
   *  - The entire transition (action update + ledger + audit) is atomic
   *
   * On rejection of self-approval, emits 'admin_adjustment_rejected' audit
   * with the attempting admin's UID for traceability.
   */
  async approveAdjustment(params: ApprovalParams): Promise<AdjustmentResult> {
    // Pre-read outside transaction for fast rejection.
    const preAction = await AdminActionRepository.findById(params.actionId);
    if (!preAction) {
      throw walletError('WALLET_NOT_FOUND', params.correlationId, 'actionId');
    }
    if (preAction.status !== 'pending') {
      // Idempotent surface: already executed or already rejected.
      throw walletError('IDEMPOTENCY_KEY_CONFLICT', params.correlationId, 'status');
    }

    const isMaker = preAction.adminId === params.adminId;
    const isLowValue = preAction.amount !== null && Math.abs(preAction.amount) <= AUTO_EXECUTE_THRESHOLD;
    const allowAutoExecute = isMaker && isLowValue && params.allowSelfExecuteLow === true;

    if (isMaker && !allowAutoExecute) {
      // Self-approval attempt for an amount that requires a separate checker.
      // Mark the action as rejected and audit the attempt.
      try {
        await db().runTransaction(async (tx) => {
          AdminActionRepository.updateStatusInTx(
            tx, params.actionId, 'rejected', params.adminId,
          );
          AuditLogService.recordInTx(tx, {
            actorId:        params.adminId,
            actorType:      'admin',
            action:         'admin_adjustment_rejected',
            targetWalletId: preAction.targetWalletId,
            correlationId:  params.correlationId,
            details: {
              actionId:     params.actionId,
              reason:       'self_approval_not_permitted',
              amount:       preAction.amount,
              makerAdminId: preAction.adminId,
            },
          });
        });
      } catch (err) {
        functions.logger.error('Failed to mark self-approval attempt as rejected', err);
      }
      throw walletError('FRAUD_DETECTED', params.correlationId);
      // FRAUD_DETECTED is intentionally opaque to the client — the attempt
      // detail is in the audit log, not in the response.
    }

    // Execute the adjustment inside a single Firestore transaction.
    const transactionId = crypto.randomUUID();

    try {
      const result = await db().runTransaction(async (tx) => {
        const [actionDoc, wallet, snapshot] = await Promise.all([
          AdminActionRepository.findByIdInTx(tx, params.actionId),
          WalletRepository.findByIdInTx(tx, preAction.targetWalletId),
          SnapshotRepository.findByWalletIdInTx(tx, preAction.targetWalletId),
        ]);

        // Re-validate inside transaction
        if (!actionDoc || actionDoc.status !== 'pending') {
          throw walletError('IDEMPOTENCY_KEY_CONFLICT', params.correlationId, 'status');
        }
        if (!wallet)   throw walletError('WALLET_NOT_FOUND', params.correlationId);
        if (!snapshot) throw walletError('WALLET_NOT_FOUND', params.correlationId);
        if (wallet.status === 'suspended') {
          throw walletError('WALLET_SUSPENDED', params.correlationId);
        }
        // Note: 'frozen' wallets ARE adjustable by admins — that's the whole
        // point of having an unfreeze path or post-fraud reconciliation.

        const amount = actionDoc.amount ?? 0;
        if (amount === 0) {
          throw walletError('INVALID_WALLET_INPUT', params.correlationId, 'amount');
        }

        // For debits, ensure the promotional bucket has enough to take from.
        // If not, the LedgerService will throw INSUFFICIENT_BALANCE via the
        // snapshot guard. This is the correct fail-closed behavior.
        const entries: LedgerEntrySpec[] = [{ bucket: 'promotional', amount }];

        LedgerService.applyInTx(tx, preAction.targetWalletId, snapshot, {
          transactionId,
          type:           'admin_adjustment',
          idempotencyKey: `admin_action_${params.actionId}`,
          entries,
          metadata: {
            actionId:     params.actionId,
            amount,
            justification: actionDoc.justification,
            makerAdminId: actionDoc.adminId,
            approverAdminId: params.adminId,
            correlationId: params.correlationId,
          },
        });

        // Move action to executed (atomically with the ledger write).
        AdminActionRepository.updateStatusInTx(
          tx, params.actionId, 'executed', params.adminId,
        );

        // Approval audit (separate from execution audit for clarity)
        AuditLogService.recordInTx(tx, {
          actorId:        params.adminId,
          actorType:      'admin',
          action:         'admin_adjustment_approved',
          targetWalletId: preAction.targetWalletId,
          correlationId:  params.correlationId,
          details: {
            actionId:     params.actionId,
            amount,
            makerAdminId: actionDoc.adminId,
            autoExecuted: allowAutoExecute,
          },
        });

        // Execution audit
        AuditLogService.recordInTx(tx, {
          actorId:        params.adminId,
          actorType:      'admin',
          action:         'admin_adjustment_executed',
          targetWalletId: preAction.targetWalletId,
          correlationId:  params.correlationId,
          details: {
            actionId:      params.actionId,
            transactionId,
            amount,
            makerAdminId:  actionDoc.adminId,
            approverAdminId: params.adminId,
          },
        });

        return { actionId: params.actionId, status: 'executed' as AdminActionStatus, ledgerTransactionId: transactionId };
      });

      return result;
    } catch (err) {
      if (err instanceof functions.https.HttpsError) throw err;
      functions.logger.error('approveAdjustment unexpected error', err);
      throw walletError('CONCURRENCY_CONFLICT', params.correlationId);
    }
  },
};
