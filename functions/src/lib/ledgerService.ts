/**
 * ledgerService.ts
 * Core accounting engine — all economic mutations pass through here.
 *
 * Security invariants:
 *  - No method mutates existing ledger_transactions or ledger_entries (append-only).
 *  - Reversals create NEW transactions with negated amounts; originals are untouched.
 *  - SnapshotRepository.updateInTx enforces non-negative balance invariant.
 *  - Zero-amount entries are rejected before reaching the repository layer.
 *  - All writes happen inside a Firestore transaction (optimistic locking via version).
 */

import * as crypto from 'crypto';
import * as admin from 'firebase-admin';
import {
  SnapshotRepository,
  LedgerTransactionRepository,
  LedgerEntryRepository,
} from './walletRepository';
import {
  BalanceSnapshotDocument,
  LedgerBucket,
  LedgerTransactionType,
} from './walletSchema';
import { walletError } from './walletErrors';

const db = () => admin.firestore();

// ── Types ─────────────────────────────────────────────────────────────────────

export interface LedgerEntrySpec {
  bucket: LedgerBucket;
  /** Non-zero integer. Positive = credit, negative = debit. */
  amount: number;
}

export interface CreateLedgerParams {
  transactionId: string;
  type: LedgerTransactionType;
  idempotencyKey: string;
  entries: LedgerEntrySpec[];
  metadata: Record<string, unknown>;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function bucketSum(entries: LedgerEntrySpec[], bucket: LedgerBucket): number {
  return entries
    .filter((e) => e.bucket === bucket)
    .reduce((sum, e) => sum + e.amount, 0);
}

// ── LedgerService ─────────────────────────────────────────────────────────────

export const LedgerService = {

  /**
   * Apply a ledger transaction inside an existing Firestore transaction.
   *
   * Call this AFTER all tx.get() reads in the enclosing transaction.
   * The caller owns the runTransaction() wrapper.
   *
   * Writes performed (in order):
   *  1. SnapshotRepository.updateInTx — validates balances, increments version
   *  2. LedgerTransactionRepository.createInTx — append-only transaction record
   *  3. LedgerEntryRepository.createInTx × N — append-only entries with running balances
   */
  applyInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    currentSnapshot: BalanceSnapshotDocument,
    params: CreateLedgerParams,
  ): void {
    if (params.entries.length === 0) {
      throw new Error('LedgerService: entries must not be empty');
    }
    if (params.entries.some((e) => e.amount === 0)) {
      throw new Error('LedgerService: entry amount must not be zero');
    }

    const purchasedDelta   = bucketSum(params.entries, 'purchased');
    const promotionalDelta = bucketSum(params.entries, 'promotional');
    const earnedDelta      = bucketSum(params.entries, 'earned');

    // Step 1 — snapshot (validates non-negative, increments version)
    SnapshotRepository.updateInTx(
      tx, walletId,
      { purchasedDelta, promotionalDelta, earnedDelta },
      currentSnapshot,
      params.transactionId,
    );

    // Step 2 — ledger transaction record
    LedgerTransactionRepository.createInTx(tx, {
      transactionId: params.transactionId,
      walletId,
      type:          params.type,
      idempotencyKey: params.idempotencyKey,
      status:        'confirmed',
      metadata:      params.metadata,
    });

    // Step 3 — ledger entries with per-bucket running balances
    const running: Record<LedgerBucket, number> = {
      purchased:   currentSnapshot.purchasedBalance,
      promotional: currentSnapshot.promotionalBalance,
      earned:      currentSnapshot.earnedBalance,
    };

    for (const entry of params.entries) {
      running[entry.bucket] += entry.amount;
      LedgerEntryRepository.createInTx(tx, {
        entryId:        crypto.randomUUID(),
        transactionId:  params.transactionId,
        walletId,
        entryType:      entry.amount > 0 ? 'credit' : 'debit',
        bucket:         entry.bucket,
        amount:         entry.amount,
        runningBalance: running[entry.bucket],
      });
    }
  },

  /**
   * Create a reversal for an existing ledger transaction.
   *
   * Fetches original entries (immutable — safe to read outside a transaction),
   * then atomically applies the reversal in a new Firestore transaction.
   * The original transaction and its entries are never modified.
   */
  async reverse(
    walletId: string,
    originalTransactionId: string,
    reversalId: string,
    idempotencyKey: string,
    correlationId: string,
    metadata: Record<string, unknown>,
  ): Promise<void> {
    const originalEntries = await LedgerEntryRepository.listByTransactionId(originalTransactionId);
    if (originalEntries.length === 0) {
      throw walletError('PURCHASE_NOT_FOUND', correlationId);
    }

    const reversalEntries: LedgerEntrySpec[] = originalEntries.map((e) => ({
      bucket: e.bucket,
      amount: -e.amount,
    }));

    await db().runTransaction(async (tx) => {
      const snapshot = await SnapshotRepository.findByWalletIdInTx(tx, walletId);
      if (!snapshot) throw walletError('WALLET_NOT_FOUND', correlationId);

      LedgerService.applyInTx(tx, walletId, snapshot, {
        transactionId: reversalId,
        type:          'refund',
        idempotencyKey,
        entries:       reversalEntries,
        metadata:      { ...metadata, reversesTransactionId: originalTransactionId },
      });
    });
  },
};
