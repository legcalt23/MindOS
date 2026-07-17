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
const mockEnqueue = jest.fn(async (_task: unknown) => {});

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

type QuerySnap = { docs: DocSnap[]; size: number; empty: boolean };
type QueryLike = {
  limit: (n: number) => QueryLike;
  orderBy: (field?: unknown) => QueryLike;
  startAfter: (doc?: unknown) => QueryLike;
  get: () => Promise<QuerySnap>;
};
type CollectionRef = {
  doc: (id: string) => Ref;
  where: (field: string, op: string, value: unknown) => QueryLike;
  limit: (n: number) => QueryLike;
  orderBy: (field?: unknown) => QueryLike;
  startAfter: (doc?: unknown) => QueryLike;
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

// Query mock: `.limit(n)` slices the in-memory result; `.orderBy()/.startAfter()` are pass-throughs.
// Pagination loops re-run the query and rely on deleted/rewritten docs dropping out of the filter,
// so cursor ops need no state here — slicing to n plus the shrinking result set is enough.
function makeQuery(
  getDocs: () => DocSnap[],
  whereInfo?: { path: string; field: string; value: unknown },
): QueryLike {
  return {
    limit: (n: number) => makeQuery(() => getDocs().slice(0, n), whereInfo),
    orderBy: () => makeQuery(getDocs, whereInfo),
    startAfter: () => makeQuery(getDocs, whereInfo),
    get: async () => {
      if (whereInfo) mockWhereGet(whereInfo.path, whereInfo.field, whereInfo.value);
      const docs = getDocs();
      return { docs, size: docs.length, empty: docs.length === 0 };
    },
  };
}

function makeCollection(path: string): CollectionRef {
  const all = () => docsInCollection(path);
  return {
    doc: (id: string) => makeRef(`${path}/${id}`),
    where: (field: string, _op: string, value: unknown) =>
      makeQuery(() => all().filter((snap) => snap.data()?.[field] === value), { path, field, value }),
    limit: (n: number) => makeQuery(all).limit(n),
    orderBy: () => makeQuery(all),
    startAfter: () => makeQuery(all),
    get: async () => ({ docs: all() }),
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

jest.mock('firebase-functions', () => {
  // runWith(...).tasks.taskQueue(...).onDispatch(handler) → returns the raw handler for unit testing.
  const taskQueueBuilder = { onDispatch: (handler: unknown) => handler };
  const tasksNamespace = { taskQueue: (_opts?: unknown) => taskQueueBuilder };
  return {
    logger: {
      info: mockLoggerInfo,
      warn: mockLoggerWarn,
      error: mockLoggerError,
    },
    runWith: (_opts?: unknown) => ({ tasks: tasksNamespace }),
    tasks: tasksNamespace,
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
  };
});

// firebase-admin/functions: getFunctions().taskQueue(name).enqueue(task) — mock the enqueue only.
jest.mock('firebase-admin/functions', () => ({
  getFunctions: () => ({ taskQueue: () => ({ enqueue: mockEnqueue }) }),
}));

// Callable envelope guards no-op; requireAuth returns the caller uid. executeAccountDeletion does
// not use these, so the existing purge tests are unaffected.
jest.mock('./lib/guards', () => ({
  requireAppCheck: jest.fn(),
  requireAuth: (ctx: { auth?: { uid: string } }) => ctx.auth?.uid,
}));
jest.mock('./lib/rateLimiter', () => ({ checkRateLimit: jest.fn(async () => {}) }));

import {
  assertRecentAuthForAccountDeletion,
  deleteAccount,
  executeAccountDeletion,
  processAccountDeletion,
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
  mockEnqueue.mockResolvedValue(undefined);
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

describe('deletion pseudonym secret (fail-closed in production)', () => {
  const ENV_KEYS = [
    'GCLOUD_PROJECT',
    'FIREBASE_CONFIG',
    'ACCOUNT_DELETION_HMAC_SECRET',
    'PII_LOG_HMAC_SECRET',
    'LOG_REDACTION_SECRET',
  ];
  const saved: Record<string, string | undefined> = {};

  beforeEach(() => {
    ENV_KEYS.forEach((k) => { saved[k] = process.env[k]; delete process.env[k]; });
  });
  afterEach(() => {
    ENV_KEYS.forEach((k) => {
      if (saved[k] === undefined) delete process.env[k];
      else process.env[k] = saved[k];
    });
  });

  it('fails closed in a production project when no pseudonym secret is configured', async () => {
    process.env['GCLOUD_PROJECT'] = 'mindos-8452f';
    await expect(executeAccountDeletion(UID, 'corr-fc')).rejects.toThrow(/secret is not configured/i);
    // Refuses BEFORE any destructive work.
    expect(mockDeleteUser).not.toHaveBeenCalled();
    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it('proceeds in a production project when a pseudonym secret is configured', async () => {
    process.env['GCLOUD_PROJECT'] = 'mindos-8452f';
    process.env['ACCOUNT_DELETION_HMAC_SECRET'] = 'prod-pseudonym-secret';
    const result = await executeAccountDeletion(UID, 'corr-ok');
    expect(result.deleted).toBe(true);
    expect(mockDeleteUser).toHaveBeenCalledWith(UID);
  });

  it('uses the local fallback outside production without throwing', async () => {
    const result = await executeAccountDeletion(UID, 'corr-local');
    expect(result.deleted).toBe(true);
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
    expect(mockDocDelete).toHaveBeenCalledWith(`profile_cards_private/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`profile_cards_public/${UID}`);
    expect(mockWhereGet).toHaveBeenCalledWith('notifications', 'recipientUserId', UID);
    expect(mockWhereGet).toHaveBeenCalledWith('follows', 'followerId', UID);
    expect(mockDocs['notifications/n1']).toBeUndefined();
    expect(mockDocs['follows/f1']).toBeUndefined();
  });

  it('deletes the sharded counter subcollection with zero residue (A1 / GDPR)', async () => {
    seed(`counters/${UID}`, { followersCount: 12 });
    seed(`counters/${UID}/shards/0`, { followersCount: 7 });
    seed(`counters/${UID}/shards/1`, { followersCount: 5 });
    seed(`counters/${UID}/shards/_meta`, { migratedToShards: true });

    await executeAccountDeletion(UID, 'corr-shards');

    // Parent doc and every shard are gone — no residue that the aggregation trigger could revive.
    expect(mockDocs[`counters/${UID}`]).toBeUndefined();
    expect(Object.keys(mockDocs).some((p) => p.startsWith(`counters/${UID}/shards/`))).toBe(false);
  });

  it('cleans up user Storage prefixes but not shared dm_media', async () => {
    await executeAccountDeletion(UID, 'corr-3');
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `profile_visuals/${UID}/` });
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `post_attachments/${UID}/` });
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `profile_cards_private/${UID}/` });
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `profile_cards_public/${UID}/` });
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

// ── S2: paginated deletion + background job (A3 / GDPR) ─────────────────────────

type DeletionResult = { deleted: boolean; deletionJobId: string; correlationId: string };
type CallableHandler = (data: unknown, context: unknown) => Promise<DeletionResult>;
type TaskHandler = (data: { uid: string; correlationId: string }) => Promise<void>;

const recentCtx = { auth: { uid: UID, token: { auth_time: Math.floor(Date.now() / 1000) } } };

describe('deleteAccount callable (enqueues the durable purge job)', () => {
  it('enqueues a purge task and returns the job id without purging inline', async () => {
    const result = await (deleteAccount as unknown as CallableHandler)({}, recentCtx);

    expect(result).toMatchObject({ deleted: true });
    expect(result.deletionJobId).toMatch(/^deleted_[a-f0-9]{32}$/);
    expect(mockEnqueue).toHaveBeenCalledTimes(1);
    const [task] = mockEnqueue.mock.calls[0] as [{ uid: string; correlationId: string }];
    expect(task).toMatchObject({ uid: UID, correlationId: result.correlationId });
    // The heavy purge runs in the worker, not in the callable.
    expect(mockDeleteUser).not.toHaveBeenCalled();
    expect(mockDeleteFiles).not.toHaveBeenCalled();
  });

  it('rejects stale reauth before enqueuing anything', async () => {
    const staleCtx = { auth: { uid: UID, token: { auth_time: Math.floor((Date.now() - 20 * 60_000) / 1000) } } };
    await expect((deleteAccount as unknown as CallableHandler)({}, staleCtx)).rejects.toThrow(/Recent sign-in/);
    expect(mockEnqueue).not.toHaveBeenCalled();
  });

  it('propagates an enqueue failure so the callable never falsely reports deletion', async () => {
    mockEnqueue.mockRejectedValueOnce(new Error('cloud tasks unavailable'));
    await expect((deleteAccount as unknown as CallableHandler)({}, recentCtx)).rejects.toThrow('cloud tasks unavailable');
  });
});

describe('processAccountDeletion worker (durable purge with a long timeout)', () => {
  it('runs the purge to completion, deleting Auth last', async () => {
    seed(`follows/f1`, { followerId: UID, followeeId: 'other' });

    await (processAccountDeletion as unknown as TaskHandler)({ uid: UID, correlationId: 'corr-worker' });

    expect(mockDeleteUser).toHaveBeenCalledWith(UID);
    expect(mockDocs['follows/f1']).toBeUndefined();
    const jobPath = Object.keys(mockDocs).find((p) => p.startsWith('account_deletion_jobs/'));
    expect(mockDocs[jobPath!]).toEqual(expect.objectContaining({ status: 'completed' }));
  });
});

describe('paginated deletion (A3 — never loads a full collection into memory)', () => {
  it('deletes a large collection across multiple bounded batches with zero residue', async () => {
    // > 2 * DELETE_BATCH_SIZE (400) so pagination must run at least 3 pages.
    for (let i = 0; i < 950; i++) seed(`notifications/n${i}`, { recipientUserId: UID });

    await executeAccountDeletion(UID, 'corr-large');

    expect(Object.keys(mockDocs).some((p) => p.startsWith('notifications/'))).toBe(false);
    // Each page commits its own batch → paginated rather than one giant read.
    expect(mockBatchCommit.mock.calls.length).toBeGreaterThanOrEqual(3);
  });

  it('paginates pseudonymization of a large retained-record set with zero uid residue', async () => {
    for (let i = 0; i < 850; i++) seed(`ledger_transactions/tx${i}`, { walletId: UID, amount: i });

    const result = await executeAccountDeletion(UID, 'corr-large-pseudo');
    const pseudo = result.deletionJobId;

    const residual = Object.keys(mockDocs)
      .filter((p) => p.startsWith('ledger_transactions/'))
      .filter((p) => mockDocs[p]['walletId'] === UID);
    expect(residual).toHaveLength(0);
    expect(mockDocs['ledger_transactions/tx0']).toEqual(expect.objectContaining({ walletId: pseudo }));
  });
});

describe('resumable deletion (idempotent retry from the last checkpoint → zero residue)', () => {
  it('completes on retry after a first attempt fails partway through Storage', async () => {
    seed(`notifications/n1`, { recipientUserId: UID });
    seed(`wallets/${UID}`, { walletId: UID, userId: UID });

    // Attempt 1: Storage purge throws AFTER Firestore PII is deleted → job failed, Auth untouched.
    mockDeleteFiles.mockRejectedValueOnce(new Error('storage flake'));
    await expect(executeAccountDeletion(UID, 'corr-resume')).rejects.toThrow('storage flake');
    expect(mockDeleteUser).not.toHaveBeenCalled();
    expect(mockDocs['notifications/n1']).toBeUndefined(); // already gone from attempt 1

    // Attempt 2 (Cloud Tasks re-dispatch): resumes idempotently and completes with zero residue.
    const result = await executeAccountDeletion(UID, 'corr-resume');
    expect(result.deleted).toBe(true);
    expect(mockDeleteUser).toHaveBeenCalledWith(UID);
    expect(mockDocs['notifications/n1']).toBeUndefined();
    expect(mockDocs[`wallets/${UID}`]).toBeUndefined(); // pseudonymized on the resuming attempt
  });
});
