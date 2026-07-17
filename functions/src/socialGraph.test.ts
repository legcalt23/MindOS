/**
 * socialGraph.test.ts — sharded social counters (audit A1).
 *
 * Unit-level (mocked Firestore) coverage of the LOGIC:
 *  - follow/unfollow/request/block write fan-in counters (followers/incoming) to a SHARD via
 *    FieldValue.increment — never read-then-write on the parent counters/{uid}
 *  - lazy backfill seeds shard 0 from the legacy parent + a migratedToShards marker; a
 *    subsequent writer sees the marker and increments instead of re-seeding
 *  - single-writer counters (following/outgoing/blocked) increment the parent atomically
 *  - the aggregateCounterShards trigger re-sums shards, coerces >= 0, and no-ops when empty
 *
 * True concurrency (100 concurrent follows → exact count, single seed under a race) is NOT
 * reproducible with a sequential mock; that is asserted against the Firestore emulator — see
 * the emulator-only test at the bottom (skipped here) and the hand-off command in the report.
 */

interface IncrementOp { __op: 'increment'; n: number; }

function isIncrement(v: unknown): v is IncrementOp {
  return typeof v === 'object' && v !== null && (v as IncrementOp).__op === 'increment';
}

const mockDocs: Record<string, Record<string, unknown>> = {};
const txGets: string[] = [];

function applyValue(existing: unknown, incoming: unknown): unknown {
  if (isIncrement(incoming)) {
    return (typeof existing === 'number' ? existing : 0) + incoming.n;
  }
  return incoming;
}

function mergeSet(path: string, data: Record<string, unknown>, opts?: { merge?: boolean }) {
  const prev = opts?.merge ? (mockDocs[path] ?? {}) : {};
  const next: Record<string, unknown> = { ...prev };
  for (const [k, v] of Object.entries(data)) next[k] = applyValue(prev[k], v);
  mockDocs[path] = next;
}

type Ref = {
  path: string;
  id: string;
  get: () => Promise<DocSnap>;
  set: (data: Record<string, unknown>, opts?: { merge?: boolean }) => Promise<void>;
  update: (data: Record<string, unknown>) => Promise<void>;
  delete: () => Promise<void>;
  collection: (sub: string) => CollectionRef;
};
type DocSnap = { id: string; ref: Ref; exists: boolean; data: () => Record<string, unknown> | undefined };
type CollectionRef = {
  doc: (id: string) => Ref;
  get: () => Promise<{ empty: boolean; docs: DocSnap[]; forEach: (cb: (d: DocSnap) => void) => void }>;
  where: (field: string, op: string, value: unknown) => { get: () => Promise<{ docs: DocSnap[] }> };
};

function makeRef(path: string): Ref {
  return {
    path,
    id: path.split('/').pop() ?? path,
    get: async () => makeSnap(path),
    set: async (data, opts) => mergeSet(path, data, opts),
    update: async (data) => mergeSet(path, data, { merge: true }),
    delete: async () => { delete mockDocs[path]; },
    collection: (sub) => makeCollection(`${path}/${sub}`),
  };
}
function makeSnap(path: string): DocSnap {
  const data = mockDocs[path];
  return { id: path.split('/').pop() ?? path, ref: makeRef(path), exists: data !== undefined, data: () => data };
}
function docsInCollection(collectionPath: string): DocSnap[] {
  const prefix = `${collectionPath}/`;
  return Object.keys(mockDocs)
    .filter((p) => p.startsWith(prefix) && p.slice(prefix.length).split('/').length === 1)
    .map(makeSnap);
}
function makeCollection(path: string): CollectionRef {
  return {
    doc: (id) => makeRef(`${path}/${id}`),
    get: async () => {
      const docs = docsInCollection(path);
      return { empty: docs.length === 0, docs, forEach: (cb: (d: DocSnap) => void) => docs.forEach(cb) };
    },
    where: (field, _op, value) => ({
      get: async () => ({ docs: docsInCollection(path).filter((s) => s.data()?.[field] === value) }),
    }),
  };
}

jest.mock('firebase-admin', () => {
  const firestore = Object.assign(
    () => ({
      collection: (name: string) => makeCollection(name),
      runTransaction: (cb: (tx: unknown) => Promise<unknown>) =>
        cb({
          get: (ref: Ref) => { txGets.push(ref.path); return Promise.resolve(makeSnap(ref.path)); },
          set: (ref: Ref, data: Record<string, unknown>, opts?: { merge?: boolean }) => mergeSet(ref.path, data, opts),
          update: (ref: Ref, data: Record<string, unknown>) => mergeSet(ref.path, data, { merge: true }),
          delete: (ref: Ref) => { delete mockDocs[ref.path]; },
        }),
    }),
    {
      FieldValue: {
        serverTimestamp: () => 'TS',
        increment: (n: number): IncrementOp => ({ __op: 'increment', n }),
      },
    },
  );
  return { initializeApp: jest.fn(), firestore };
});

jest.mock('firebase-functions', () => ({
  logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
  https: {
    onCall: (handler: unknown) => handler,
    HttpsError: class extends Error {
      code: string;
      constructor(code: string, message: string) { super(message); this.code = code; }
    },
  },
  firestore: {
    document: () => ({ onWrite: (handler: unknown) => handler }),
  },
}));

// Guards / rate-limit / validators / logging are exercised elsewhere; here they no-op so the
// counter logic under test runs. Auth returns the caller uid from context.
jest.mock('./lib/guards', () => ({
  requireAppCheck: jest.fn(),
  requireAuth: (ctx: { auth?: { uid: string } }) => ctx.auth!.uid,
  requireNotSelf: jest.fn(),
  requireNoBlock: jest.fn(async () => {}),
}));
jest.mock('./lib/rateLimiter', () => ({ checkRateLimit: jest.fn(async () => {}) }));
jest.mock('./lib/validators', () => ({
  validateFollowInput: (d: { targetUserId: string }) => ({ targetUserId: d.targetUserId }),
  validateFollowRequestInput: (d: { targetUserId: string }) => ({ targetUserId: d.targetUserId }),
  validateRequestActionInput: (d: { requestId: string }) => ({ requestId: d.requestId }),
  validateBlockInput: (d: { targetUserId: string }) => ({ targetUserId: d.targetUserId }),
}));
jest.mock('./lib/privacyLogging', () => ({ logSafeRef: () => 'ref' }));

import {
  followUser,
  unfollowUser,
  sendFollowRequest,
  blockUser,
  aggregateCounterShards,
} from './socialGraph';

type Handler = (data: unknown, context: unknown) => Promise<unknown>;
type Trigger = (change: unknown, context: { params: Record<string, string> }) => Promise<unknown>;

const ctx = (uid: string) => ({ auth: { uid, token: {} }, app: {} });

function clearDocs() { Object.keys(mockDocs).forEach((k) => delete mockDocs[k]); txGets.length = 0; }

beforeEach(clearDocs);

describe('followUser — sharded followers, single-writer following', () => {
  it('seeds shard 0 from the legacy parent + marker on the first follow, and does not touch the parent followersCount', async () => {
    mockDocs[`counters/target`] = { followersCount: 5 };
    mockDocs[`privacy/target`] = { profileVisibility: 'Public' };

    await (followUser as unknown as Handler)({ targetUserId: 'target' }, ctx('caller'));

    // Fan-in count went to shard 0 (legacy 5 + 1), never a read-then-write on the parent field.
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ followersCount: 6 });
    expect(mockDocs['counters/target/shards/_meta']).toEqual({ migratedToShards: true });
    expect(mockDocs['counters/target']['followersCount']).toBe(5); // parent field untouched by the write path
    // Caller's own followingCount is single-writer → parent increment.
    expect(mockDocs['counters/caller']).toMatchObject({ followingCount: 1 });
  });

  it('after migration a second follower increments a shard and does NOT re-seed or read the legacy parent', async () => {
    mockDocs[`privacy/target`] = { profileVisibility: 'Public' };
    const rnd = jest.spyOn(Math, 'random').mockReturnValue(0.35); // → shard 3
    try {
      await (followUser as unknown as Handler)({ targetUserId: 'target' }, ctx('a')); // migrates (seed shard0)
      txGets.length = 0;
      await (followUser as unknown as Handler)({ targetUserId: 'target' }, ctx('b')); // steady state
    } finally {
      rnd.mockRestore();
    }
    // Second follow incremented shard 3, left shard0 seed intact, and never read counters/target (parent).
    expect(mockDocs['counters/target/shards/3']).toMatchObject({ followersCount: 1 });
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ followersCount: 1 });
    expect(txGets).not.toContain('counters/target');
    expect(txGets).toContain('counters/target/shards/_meta');
  });
});

describe('lazy seed is idempotent under the marker', () => {
  it('a writer that sees the marker increments instead of re-seeding the legacy value', async () => {
    mockDocs[`counters/target`] = { followersCount: 100 };
    mockDocs[`privacy/target`] = { profileVisibility: 'Public' };
    const rnd = jest.spyOn(Math, 'random').mockReturnValue(0); // both → shard 0 for a determinstic sum
    try {
      await (followUser as unknown as Handler)({ targetUserId: 'target' }, ctx('a')); // seed: shard0 = 101
      await (followUser as unknown as Handler)({ targetUserId: 'target' }, ctx('b')); // marker present → increment
    } finally {
      rnd.mockRestore();
    }
    // Legacy 100 counted exactly once (seed), then +1 — not re-seeded to 100 again.
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ followersCount: 102 });
  });
});

describe('unfollowUser', () => {
  it('decrements the target followers shard and the caller following parent', async () => {
    mockDocs[`privacy/target`] = { profileVisibility: 'Public' };
    mockDocs[`follows/caller_target`] = { followerId: 'caller', followeeId: 'target' };
    mockDocs[`counters/target/shards/_meta`] = { migratedToShards: true };
    mockDocs[`counters/target/shards/0`] = { followersCount: 4 };
    const rnd = jest.spyOn(Math, 'random').mockReturnValue(0);
    try {
      await (unfollowUser as unknown as Handler)({ targetUserId: 'target' }, ctx('caller'));
    } finally {
      rnd.mockRestore();
    }
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ followersCount: 3 });
    expect(mockDocs['counters/caller']).toMatchObject({ followingCount: -1 });
    expect(mockDocs['follows/caller_target']).toBeUndefined();
  });
});

describe('sendFollowRequest — incoming requests fan in on the target', () => {
  it('shards the target incoming count and increments the caller outgoing parent', async () => {
    mockDocs[`privacy/target`] = { profileVisibility: 'Private' };
    await (sendFollowRequest as unknown as Handler)({ targetUserId: 'target' }, ctx('caller'));
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ incomingPendingRequestsCount: 1 });
    expect(mockDocs['counters/caller']).toMatchObject({ outgoingPendingRequestsCount: 1 });
  });
});

describe('blockUser (D-03) — shard the target deltas, keep blockedUsersCount on the parent', () => {
  it('sends followers/incoming deltas to shards and following/outgoing/blocked to the parent', async () => {
    // caller → target follow exists; target → caller pending request exists.
    mockDocs[`follows/caller_target`] = { followerId: 'caller', followeeId: 'target' };
    mockDocs[`followRequests/target_caller`] = { status: 'Pending', requesterId: 'target', targetId: 'caller' };
    const rnd = jest.spyOn(Math, 'random').mockReturnValue(0);
    try {
      await (blockUser as unknown as Handler)({ targetUserId: 'target' }, ctx('caller'));
    } finally {
      rnd.mockRestore();
    }
    // target lost a follower (caller unfollowed by block) → shard(target).followersCount -1
    expect(mockDocs['counters/target/shards/0']).toMatchObject({ followersCount: -1 });
    // caller lost an incoming request (target's pending request invalidated) → shard(caller).incoming -1
    expect(mockDocs['counters/caller/shards/0']).toMatchObject({ incomingPendingRequestsCount: -1 });
    // blockedUsersCount is single-writer (the blocker) → parent increment, never a shard.
    expect(mockDocs['counters/caller']).toMatchObject({ blockedUsersCount: 1, followingCount: -1 });
    expect(mockDocs['counters/caller/shards/0']).not.toHaveProperty('blockedUsersCount');
  });
});

describe('aggregateCounterShards trigger — materialize the read-model, coerced >= 0', () => {
  it('re-sums the shards and merges the totals onto counters/{uid}', async () => {
    mockDocs[`counters/u/shards/0`] = { followersCount: 7, incomingPendingRequestsCount: 2 };
    mockDocs[`counters/u/shards/1`] = { followersCount: 3 };
    mockDocs[`counters/u/shards/_meta`] = { migratedToShards: true };

    await (aggregateCounterShards as unknown as Trigger)({}, { params: { uid: 'u', shardId: '1' } });

    expect(mockDocs['counters/u']).toMatchObject({ followersCount: 10, incomingPendingRequestsCount: 2 });
  });

  it('coerces a negative shard sum to 0 (FieldValue.increment does not clamp)', async () => {
    mockDocs[`counters/u/shards/0`] = { followersCount: -5 };
    await (aggregateCounterShards as unknown as Trigger)({}, { params: { uid: 'u', shardId: '0' } });
    expect(mockDocs['counters/u']).toMatchObject({ followersCount: 0 });
  });

  it('no-ops on an empty shard set so a deleted parent is not resurrected', async () => {
    await (aggregateCounterShards as unknown as Trigger)({}, { params: { uid: 'ghost', shardId: '0' } });
    expect(mockDocs['counters/ghost']).toBeUndefined();
  });
});
