/**
 * accountDeletion.test.ts
 *
 * Covers executeAccountDeletion(uid):
 *  - creates a durable deletion job under a pseudonymized ID
 *  - marks the account DeletionPending before destructive work
 *  - blocks completion and Auth deletion when Storage purge fails
 *  - deletes Auth only after critical purge and pseudonymization
 *  - pseudonymizes retained wallet/ledger/purchase records
 *  - does not write raw UID to logs or returned payload
 *  - enforces recent reauth for the public callable envelope
 */

const mockDocs: Record<string, Record<string, unknown>> = {};

const mockDocSet = jest.fn();
const mockDocUpdate = jest.fn();
const mockDocDelete = jest.fn();
const mockWhereGet = jest.fn();
const mockBatchDelete = jest.fn();
const mockBatchUpdate = jest.fn();
const mockBatchCommit = jest.fn(async () => {});
const mockDeleteFiles = jest.fn(async (_opts: { prefix: string }) => {});
const mockDeleteUser = jest.fn(async (_uid: string) => {});
const mockLoggerInfo = jest.fn();
const mockLoggerWarn = jest.fn();
const mockLoggerError = jest.fn();

type Ref = {
  path: string;
  id: string;
  get: () => Promise<DocSnap>;
  set: (data: Record<string, unknown>, opts?: { merge?: boolean }) => Promise<void>;
  update: (data: Record<string, unknown>) => Promise<void>;
  delete: () => Promise<void>;
  collection: (sub: string) => CollectionRef;
};

type DocSnap = {
  id: string;
  ref: Ref;
  exists: boolean;
  data: () => Record<string, unknown> | undefined;
};

type CollectionRef = {
  doc: (id: string) => Ref;
  where: (field: string, op: string, value: unknown) => { get: () => Promise<{ docs: DocSnap[] }> };
  get: () => Promise<{ docs: DocSnap[] }>;
};

function mergeSet(path: string, data: Record<string, unknown>, opts?: { merge?: boolean }) {
  mockDocSet(path, data, opts);
  mockDocs[path] = opts?.merge
    ? { ...(mockDocs[path] ?? {}), ...data }
    : { ...data };
}

function makeRef(path: string): Ref {
  const id = path.split('/').pop() ?? path;
  return {
    path,
    id,
    get: async () => makeSnap(path),
    set: async (data, opts) => mergeSet(path, data, opts),
    update: async (data) => {
      mockDocUpdate(path, data);
      mockDocs[path] = { ...(mockDocs[path] ?? {}), ...data };
    },
    delete: async () => {
      mockDocDelete(path);
      delete mockDocs[path];
    },
    collection: (sub: string) => makeCollection(`${path}/${sub}`),
  };
}

function makeSnap(path: string): DocSnap {
  const ref = makeRef(path);
  const data = mockDocs[path];
  return {
    id: ref.id,
    ref,
    exists: data !== undefined,
    data: () => data,
  };
}

function docsInCollection(collectionPath: string): DocSnap[] {
  const prefix = `${collectionPath}/`;
  return Object.keys(mockDocs)
    .filter((path) => path.startsWith(prefix) && path.slice(prefix.length).split('/').length === 1)
    .map(makeSnap);
}

function makeCollection(path: string): CollectionRef {
  return {
    doc: (id: string) => makeRef(`${path}/${id}`),
    where: (field: string, _op: string, value: unknown) => ({
      get: async () => {
        mockWhereGet(path, field, value);
        return {
          docs: docsInCollection(path).filter((snap) => snap.data()?.[field] === value),
        };
      },
    }),
    get: async () => ({ docs: docsInCollection(path) }),
  };
}

jest.mock('firebase-admin', () => {
  const firestore = Object.assign(
    () => ({
      collection: (name: string) => makeCollection(name),
      batch: () => ({
        delete: (ref: Ref) => {
          mockBatchDelete(ref.path);
          delete mockDocs[ref.path];
        },
        update: (ref: Ref, data: Record<string, unknown>) => {
          mockBatchUpdate(ref.path, data);
          mockDocs[ref.path] = { ...(mockDocs[ref.path] ?? {}), ...data };
        },
        commit: () => mockBatchCommit(),
      }),
      runTransaction: (callback: (tx: unknown) => Promise<unknown>) =>
        callback({
          get: (ref: Ref) => Promise.resolve(makeSnap(ref.path)),
          set: (ref: Ref, data: Record<string, unknown>, opts?: { merge?: boolean }) =>
            mergeSet(ref.path, data, opts),
          update: (ref: Ref, data: Record<string, unknown>) => {
            mockDocUpdate(ref.path, data);
            mockDocs[ref.path] = { ...(mockDocs[ref.path] ?? {}), ...data };
          },
          delete: (ref: Ref) => {
            mockDocDelete(ref.path);
            delete mockDocs[ref.path];
          },
        }),
    }),
    { FieldValue: { serverTimestamp: () => 'MOCK_TS' } },
  );

  return {
    initializeApp: jest.fn(),
    firestore,
    storage: () => ({ bucket: () => ({ deleteFiles: (opts: { prefix: string }) => mockDeleteFiles(opts) }) }),
    auth: () => ({ deleteUser: (uid: string) => mockDeleteUser(uid) }),
  };
});

jest.mock('firebase-functions', () => ({
  logger: {
    info: mockLoggerInfo,
    warn: mockLoggerWarn,
    error: mockLoggerError,
  },
  https: {
    onCall: (handler: unknown) => handler,
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
  assertRecentAuthForAccountDeletion,
  executeAccountDeletion,
} from './accountDeletion';

const UID = 'user_del_1';

function clearDocs() {
  Object.keys(mockDocs).forEach((k) => delete mockDocs[k]);
}

function seed(path: string, data: Record<string, unknown>) {
  mockDocs[path] = data;
}

beforeEach(() => {
  clearDocs();
  jest.clearAllMocks();
  mockDeleteFiles.mockResolvedValue(undefined);
  mockDeleteUser.mockResolvedValue(undefined);
});

describe('assertRecentAuthForAccountDeletion', () => {
  const nowMs = 1_800_000_000_000;

  it('accepts recent Firebase auth_time in seconds', () => {
    expect(() =>
      assertRecentAuthForAccountDeletion({
        auth: { uid: UID, token: { auth_time: Math.floor((nowMs - 60_000) / 1000) } },
      } as never, nowMs),
    ).not.toThrow();
  });

  it('rejects stale auth_time', () => {
    expect(() =>
      assertRecentAuthForAccountDeletion({
        auth: { uid: UID, token: { auth_time: Math.floor((nowMs - 20 * 60_000) / 1000) } },
      } as never, nowMs),
    ).toThrow(/Recent sign-in/);
  });

  it('rejects missing auth_time', () => {
    expect(() =>
      assertRecentAuthForAccountDeletion({ auth: { uid: UID, token: {} } } as never, nowMs),
    ).toThrow(/Recent sign-in/);
  });
});

describe('executeAccountDeletion', () => {
  it('creates a durable job and marks the account DeletionPending', async () => {
    const result = await executeAccountDeletion(UID, 'corr-1');

    expect(result).toMatchObject({ deleted: true, correlationId: 'corr-1' });
    expect(result.deletionJobId).toMatch(/^deleted_[a-f0-9]{32}$/);
    expect(JSON.stringify(result)).not.toContain(UID);
    expect(mockDocSet).toHaveBeenCalledWith(
      `users/${UID}`,
      expect.objectContaining({ status: 'DeletionPending', deletionJobId: result.deletionJobId }),
      { merge: true },
    );
    expect(mockDocs[`account_deletion_jobs/${result.deletionJobId}`]).toEqual(
      expect.objectContaining({ status: 'completed', jobId: result.deletionJobId }),
    );
  });

  it('deletes owned single docs and query-based PII collections', async () => {
    seed('notifications/n1', { recipientUserId: UID, actorUserId: 'other' });
    seed('follows/f1', { followerId: UID, followeeId: 'other' });

    await executeAccountDeletion(UID, 'corr-2');

    expect(mockDocDelete).toHaveBeenCalledWith(`profiles/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`profile_visuals/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`privacy/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`counters/${UID}`);
    expect(mockWhereGet).toHaveBeenCalledWith('notifications', 'recipientUserId', UID);
    expect(mockWhereGet).toHaveBeenCalledWith('follows', 'followerId', UID);
    expect(mockDocs['notifications/n1']).toBeUndefined();
    expect(mockDocs['follows/f1']).toBeUndefined();
  });

  it('cleans up user Storage prefixes but not shared dm_media', async () => {
    await executeAccountDeletion(UID, 'corr-3');
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `profile_visuals/${UID}/` });
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `post_attachments/${UID}/` });
    const prefixes = mockDeleteFiles.mock.calls.map((c) => c[0].prefix);
    expect(prefixes.some((p) => p.startsWith('dm_media'))).toBe(false);
  });

  it('does not delete Auth or complete the job when Storage cleanup fails', async () => {
    mockDeleteFiles.mockRejectedValueOnce(new Error('storage down'));

    await expect(executeAccountDeletion(UID, 'corr-fail')).rejects.toThrow('storage down');

    expect(mockDeleteUser).not.toHaveBeenCalled();
    expect(mockDocSet).toHaveBeenCalledWith(
      `users/${UID}`,
      expect.objectContaining({ status: 'DeletionPending' }),
      { merge: true },
    );
    const jobPath = Object.keys(mockDocs).find((path) => path.startsWith('account_deletion_jobs/'));
    expect(jobPath).toBeDefined();
    expect(mockDocs[jobPath!]).toEqual(expect.objectContaining({ status: 'failed' }));
  });

  it('deletes Auth after both Storage prefixes are purged', async () => {
    await executeAccountDeletion(UID, 'corr-order');

    expect(mockDeleteUser).toHaveBeenCalledWith(UID);
    const authCall = mockDeleteUser.mock.invocationCallOrder[0];
    const storageCalls = mockDeleteFiles.mock.invocationCallOrder;
    expect(authCall).toBeGreaterThan(storageCalls[0]);
    expect(authCall).toBeGreaterThan(storageCalls[1]);
  });

  it('pseudonymizes retained wallet, ledger, purchases and audit records', async () => {
    seed(`wallets/${UID}`, { walletId: UID, userId: UID, status: 'active' });
    seed(`balance_snapshots/${UID}`, { walletId: UID, availableBalance: 10 });
    seed('ledger_transactions/tx1', { transactionId: 'tx1', walletId: UID, status: 'confirmed' });
    seed('ledger_entries/e1', { entryId: 'e1', walletId: UID, amount: 10 });
    seed('purchases/p1', { purchaseId: 'p1', walletId: UID, userId: UID });
    seed('fraud_signals/s1', { signalId: 's1', walletId: UID, userId: UID });
    seed('wallet_audit_logs/a1', { logId: 'a1', actorId: UID, targetWalletId: UID });
    seed('admin_actions/aa1', { actionId: 'aa1', targetWalletId: UID });
    seed(`idempotency_keys/${UID}_idem1`, { docId: `${UID}_idem1`, key: 'idem1', walletId: UID });

    const result = await executeAccountDeletion(UID, 'corr-retained');
    const pseudo = result.deletionJobId;

    expect(mockDocs[`wallets/${UID}`]).toBeUndefined();
    expect(mockDocs[`balance_snapshots/${UID}`]).toBeUndefined();
    expect(mockDocs[`wallets/${pseudo}`]).toEqual(expect.objectContaining({ walletId: pseudo, userId: pseudo }));
    expect(mockDocs[`balance_snapshots/${pseudo}`]).toEqual(expect.objectContaining({ walletId: pseudo }));
    expect(mockDocs['ledger_transactions/tx1']).toEqual(expect.objectContaining({ walletId: pseudo }));
    expect(mockDocs['ledger_entries/e1']).toEqual(expect.objectContaining({ walletId: pseudo }));
    expect(mockDocs['purchases/p1']).toEqual(expect.objectContaining({ walletId: pseudo, userId: pseudo }));
    expect(mockDocs['fraud_signals/s1']).toEqual(expect.objectContaining({ walletId: pseudo, userId: pseudo }));
    expect(mockDocs['wallet_audit_logs/a1']).toEqual(expect.objectContaining({ actorId: pseudo, targetWalletId: pseudo }));
    expect(mockDocs['admin_actions/aa1']).toEqual(expect.objectContaining({ targetWalletId: pseudo }));
    expect(mockDocs[`idempotency_keys/${UID}_idem1`]).toBeUndefined();
    expect(mockDocs[`idempotency_keys/${pseudo}_idem1`]).toEqual(expect.objectContaining({ walletId: pseudo }));
  });

  it('tolerates an already-deleted Auth user', async () => {
    mockDeleteUser.mockImplementation(async () => {
      const err = new Error('not found') as Error & { code: string };
      err.code = 'auth/user-not-found';
      throw err;
    });
    await expect(executeAccountDeletion(UID, 'corr-auth-missing')).resolves.toMatchObject({ deleted: true });
  });

  it('does not log raw UID', async () => {
    await executeAccountDeletion(UID, 'corr-log');
    const logs = JSON.stringify([
      mockLoggerInfo.mock.calls,
      mockLoggerWarn.mock.calls,
      mockLoggerError.mock.calls,
    ]);
    expect(logs).not.toContain(UID);
    expect(logs).toContain('corr-log');
  });
});
