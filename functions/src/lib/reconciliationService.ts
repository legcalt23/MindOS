/**
 * reconciliationService.ts
 * Internal accounting reconciliation: recalculates wallet balance from ledger entries
 * and compares with the current snapshot. Detect-only by default.
 *
 * Security invariants:
 *  - Default mode: detect-only. No writes unless correctIfMismatch is explicitly true.
 *  - Every correction is double-audited (mismatch log + correction log).
 *  - Correction bypasses LedgerService (no fake ledger entries created for corrections).
 *  - Not callable by clients — internal/admin only.
 */

import * as admin from 'firebase-admin';
import { SnapshotRepository, LedgerEntryRepository, AuditLogRepository } from './walletRepository';
import { LedgerEntryDocument, LedgerBucket, COLLECTIONS } from './walletSchema';
import { walletLog } from './walletLogger';
import { generateCorrelationId } from './correlationId';

const db = () => admin.firestore();

// ── Types ─────────────────────────────────────────────────────────────────────

export interface BucketBalances {
  purchased:   number;
  promotional: number;
  earned:      number;
}

export interface ReconciliationResult {
  walletId:       string;
  snapshotValues: BucketBalances;
  computedValues: BucketBalances;
  hasMismatch:    boolean;
  corrected:      boolean;
  checkedAt:      number;
}

// ── Pure computation ──────────────────────────────────────────────────────────

/**
 * Pure function — sum all ledger entries per bucket.
 * Credits are positive, debits are negative. Testable without I/O.
 */
export function computeBalancesFromEntries(entries: LedgerEntryDocument[]): BucketBalances {
  const result: BucketBalances = { purchased: 0, promotional: 0, earned: 0 };
  for (const entry of entries) {
    // SECURITY: use hasOwnProperty.call to bypass the prototype chain.
    // The `in` operator would also accept '__proto__', 'constructor',
    // 'toString', etc., causing the next line to mutate Object.prototype
    // (silently corrupting the reconciliation result) if a bad bucket
    // value ever made it into ledger_entries. Defense-in-depth: even
    // though TypeScript types prevent this at write time, the consumer
    // must not trust read-time data.
    if (Object.prototype.hasOwnProperty.call(result, entry.bucket)) {
      result[entry.bucket as LedgerBucket] += entry.amount;
    }
  }
  return result;
}

// ── Reconciliation ────────────────────────────────────────────────────────────

/**
 * Reconcile a single wallet's snapshot against its ledger entries.
 *
 * @param walletId         - Wallet to reconcile.
 * @param options.correctIfMismatch - If true, overwrites snapshot with computed values
 *                                    (requires explicit caller intent; audited twice).
 * @param options.correlationId     - Trace ID for log correlation.
 */
export async function reconcileWallet(
  walletId: string,
  options: { correctIfMismatch?: boolean; correlationId?: string } = {},
): Promise<ReconciliationResult> {
  const correlationId = options.correlationId ?? generateCorrelationId();
  const correctIfMismatch = options.correctIfMismatch ?? false;

  const [snapshot, entries] = await Promise.all([
    SnapshotRepository.findByWalletId(walletId),
    LedgerEntryRepository.listByWalletId(walletId),
  ]);

  if (!snapshot) {
    throw new Error(`reconcileWallet: no snapshot found for wallet ${walletId}`);
  }

  const computed = computeBalancesFromEntries(entries);

  const snapshotValues: BucketBalances = {
    purchased:   snapshot.purchasedBalance,
    promotional: snapshot.promotionalBalance,
    earned:      snapshot.earnedBalance,
  };

  const hasMismatch =
    computed.purchased   !== snapshotValues.purchased   ||
    computed.promotional !== snapshotValues.promotional ||
    computed.earned      !== snapshotValues.earned;

  let corrected = false;

  if (hasMismatch) {
    const diff: BucketBalances = {
      purchased:   computed.purchased   - snapshotValues.purchased,
      promotional: computed.promotional - snapshotValues.promotional,
      earned:      computed.earned      - snapshotValues.earned,
    };

    walletLog.warn(
      { correlationId, action: 'reconcileWallet', walletId },
      'Reconciliation mismatch detected',
      { snapshotValues, computedValues: computed, diff },
    );

    await AuditLogRepository.append(
      'system', 'system', 'reconciliation_mismatch', walletId, correlationId,
      { snapshotValues, computedValues: computed, diff },
    );

    if (correctIfMismatch) {
      walletLog.warn(
        { correlationId, action: 'reconcileWallet', walletId },
        'Applying reconciliation correction (explicit mode)',
        { from: snapshotValues, to: computed },
      );

      await AuditLogRepository.append(
        'system', 'system', 'reconciliation_correction', walletId, correlationId,
        { from: snapshotValues, to: computed },
      );

      const available = computed.purchased + computed.promotional + computed.earned;
      await db().collection(COLLECTIONS.BALANCE_SNAPSHOTS).doc(walletId).update({
        purchasedBalance:   computed.purchased,
        promotionalBalance: computed.promotional,
        earnedBalance:      computed.earned,
        availableBalance:   available,
        updatedAt:          Date.now(),
      });

      corrected = true;
    }
  } else {
    walletLog.info(
      { correlationId, action: 'reconcileWallet', walletId },
      'Reconciliation passed — snapshot matches ledger',
    );
  }

  return {
    walletId,
    snapshotValues,
    computedValues: computed,
    hasMismatch,
    corrected,
    checkedAt: Date.now(),
  };
}
