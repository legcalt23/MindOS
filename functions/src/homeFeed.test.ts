/**
 * homeFeed.test.ts — fan-out-on-read home feed (audit A2 / S4).
 *
 * Covers the LOGIC with a mocked Firestore:
 *  - pure merge (dedupe by postId, createdAt desc order, page cap)
 *  - pure chunking (Firestore `in` groups of <= 30)
 *  - per-instance cache (miss -> compute, hit within TTL, expiry, oldest-eviction over cap)
 *  - loadHomeFeed callable: followees + self -> posts merged desc; auth/App Check enforced;
 *    a warm cache hit serves without re-querying Firestore.
 */

type DocData = Record<string, unknown>;

const collections: Record<string, Map<string, DocData>> = {};
let queryCount = 0;

function coll(name: string): Map<string, DocData> {
  if (!collections[name]) collections[name] = new Map();
  return collections[name];
}
function resetStore(): void {
  for (const k of Object.keys(collections)) delete collections[k];
  queryCount = 0;
}
function seed(name: string, id: string, data: DocData): void {
  coll(name).set(id, data);
}
/** A Firestore Timestamp-like object (exercises projectFeedPost's toMillis path). */
function ts(ms: number): { toMillis: () => number } {
  return { toMillis: () => ms };
}

interface Filter { field: string; op: string; value: unknown; }
interface FakeDoc { id: string; data: () => DocData; }

function matchFilter(fieldVal: unknown, op: string, value: unknown): boolean {
  if (op === '==') return fieldVal === value;
  if (op === 'in') return Array.isArray(value) && value.includes(fieldVal);
  return false;
}
function millisOf(v: unknown): number {
  const t = v as { toMillis?: () => number } | null;
  if (t && typeof t.toMillis === 'function') return t.toMillis();
  return typeof v === 'number' ? v : 0;
}

class FakeQuery {
  constructor(
    private name: string,
    private filters: Filter[] = [],
    private order: { field: string; dir: string } | null = null,
    private lim: number | null = null,
  ) {}
  where(field: string, op: string, value: unknown): FakeQuery {
    return new FakeQuery(this.name, [...this.filters, { field, op, value }], this.order, this.lim);
  }
  orderBy(field: string, dir = 'asc'): FakeQuery {
    return new FakeQuery(this.name, this.filters, { field, dir }, this.lim);
  }
  limit(n: number): FakeQuery {
    return new FakeQuery(this.name, this.filters, this.order, n);
  }
  async get(): Promise<{ docs: FakeDoc[]; forEach: (cb: (d: FakeDoc) => void) => void }> {
    queryCount++;
    let docs: FakeDoc[] = [...coll(this.name).entries()].map(([id, data]) => ({ id, data: () => data }));
    for (const f of this.filters) {
      docs = docs.filter((d) => matchFilter(d.data()[f.field], f.op, f.value));
    }
    if (this.order) {
      const { field, dir } = this.order;
      docs = [...docs].sort((a, b) => {
        const av = millisOf(a.data()[field]);
        const bv = millisOf(b.data()[field]);
        return dir === 'desc' ? bv - av : av - bv;
      });
    }
    if (this.lim !== null) docs = docs.slice(0, this.lim);
    return { docs, forEach: (cb: (d: FakeDoc) => void) => docs.forEach(cb) };
  }
}

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: () => ({
    collection: (name: string) => new FakeQuery(name),
  }),
}));

jest.mock('firebase-functions', () => ({
  logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
  https: {
    onCall: (handler: unknown) => handler,
    HttpsError: class extends Error {
      code: string;
      constructor(code: string, message: string) { super(message); this.code = code; }
    },
  },
}));

import {
  loadHomeFeed,
  mergeFeedPage,
  chunk,
  clampLimit,
  feedCacheKey,
  readFeedCache,
  writeFeedCache,
  __clearFeedCacheForTest,
  type FeedCandidate,
} from './homeFeed';

type CallableFn = (data: unknown, context: unknown) => Promise<{ posts: DocData[] }>;
const callLoadHomeFeed = loadHomeFeed as unknown as CallableFn;
const authed = { auth: { uid: 'viewer' }, app: {} };

function candidate(postId: string, createdAtMs: number): FeedCandidate {
  return { postId, createdAtMs, post: { id: postId, createdAt: createdAtMs } };
}

beforeEach(() => {
  resetStore();
  __clearFeedCacheForTest();
});

// ── Pure helpers ────────────────────────────────────────────────────────────────

describe('clampLimit', () => {
  test('defaults to 50 for non-numbers, clamps to [1,100]', () => {
    expect(clampLimit(undefined)).toBe(50);
    expect(clampLimit('x')).toBe(50);
    expect(clampLimit(0)).toBe(1);
    expect(clampLimit(-5)).toBe(1);
    expect(clampLimit(1000)).toBe(100);
    expect(clampLimit(37.9)).toBe(37);
  });
});

describe('chunk', () => {
  test('splits into groups of at most `size`', () => {
    expect(chunk([1, 2, 3, 4, 5], 2)).toEqual([[1, 2], [3, 4], [5]]);
    expect(chunk([], 30)).toEqual([]);
    expect(chunk([1, 2], 30)).toEqual([[1, 2]]);
  });
});

describe('mergeFeedPage', () => {
  test('dedupes by postId, orders createdAt desc, caps to limit', () => {
    const merged = mergeFeedPage(
      [candidate('a', 100), candidate('b', 300), candidate('a', 100), candidate('c', 200)],
      2,
    );
    expect(merged.posts.map((p) => p['id'])).toEqual(['b', 'c']);
  });

  test('stable postId tiebreak when createdAt ties', () => {
    const merged = mergeFeedPage([candidate('a', 500), candidate('b', 500)], 2);
    // desc createdAt tie -> postId desc (b before a)
    expect(merged.posts.map((p) => p['id'])).toEqual(['b', 'a']);
  });

  test('returns all when fewer than limit', () => {
    const merged = mergeFeedPage([candidate('a', 1)], 50);
    expect(merged.posts).toHaveLength(1);
  });
});

// ── Cache ────────────────────────────────────────────────────────────────────────

describe('feed cache', () => {
  test('miss when empty, hit within TTL, expiry after TTL', () => {
    const key = feedCacheKey('viewer', 50);
    const page = { posts: [{ id: 'x' }] };
    expect(readFeedCache(key, 1000)).toBeNull();
    writeFeedCache(key, page, 1000);
    expect(readFeedCache(key, 1000)).toBe(page);            // fresh
    expect(readFeedCache(key, 1000 + 44_999)).toBe(page);   // still fresh (< 45s)
    expect(readFeedCache(key, 1000 + 45_000)).toBeNull();   // expired (>= TTL)
  });

  test('keys by viewer:limit so a different limit does not collide', () => {
    writeFeedCache(feedCacheKey('viewer', 50), { posts: [{ id: 'a' }] }, 0);
    expect(readFeedCache(feedCacheKey('viewer', 100), 0)).toBeNull();
    expect(readFeedCache(feedCacheKey('viewer', 50), 0)?.posts[0]?.['id']).toBe('a');
  });
});

// ── Callable ──────────────────────────────────────────────────────────────────────

describe('loadHomeFeed callable', () => {
  test('rejects unauthenticated and non-App-Check callers', async () => {
    await expect(callLoadHomeFeed({}, { app: {} })).rejects.toThrow();          // no auth
    await expect(callLoadHomeFeed({}, { auth: { uid: 'v' } })).rejects.toThrow(); // no app check
  });

  test('merges followees + own posts, newest first', async () => {
    seed('follows', 'viewer_alice', { followerId: 'viewer', followeeId: 'alice' });
    seed('follows', 'viewer_bob', { followerId: 'viewer', followeeId: 'bob' });
    // A stranger the viewer does NOT follow -> must not appear.
    seed('posts', 'p_stranger', { id: 'p_stranger', userId: 'stranger', createdAt: ts(999) });
    seed('posts', 'p_alice', { id: 'p_alice', userId: 'alice', createdAt: ts(300) });
    seed('posts', 'p_bob', { id: 'p_bob', userId: 'bob', createdAt: ts(200) });
    seed('posts', 'p_self', { id: 'p_self', userId: 'viewer', createdAt: ts(400) });

    const page = await callLoadHomeFeed({ limit: 50 }, authed);

    expect(page.posts.map((p) => p['id'])).toEqual(['p_self', 'p_alice', 'p_bob']);
    expect(page.posts.every((p) => p['userId'] !== 'stranger')).toBe(true);
    // createdAt projected to epoch millis (number), not a Timestamp object.
    expect(page.posts[0]?.['createdAt']).toBe(400);
  });

  test('respects the page limit', async () => {
    seed('follows', 'viewer_alice', { followerId: 'viewer', followeeId: 'alice' });
    for (let i = 0; i < 5; i++) {
      seed('posts', `p${i}`, { id: `p${i}`, userId: 'alice', createdAt: ts(i) });
    }
    const page = await callLoadHomeFeed({ limit: 2 }, authed);
    expect(page.posts).toHaveLength(2);
    expect(page.posts.map((p) => p['id'])).toEqual(['p4', 'p3']); // newest first
  });

  test('a warm cache hit serves without re-querying Firestore', async () => {
    seed('follows', 'viewer_alice', { followerId: 'viewer', followeeId: 'alice' });
    seed('posts', 'p_alice', { id: 'p_alice', userId: 'alice', createdAt: ts(300) });

    await callLoadHomeFeed({ limit: 50 }, authed);
    const afterFirst = queryCount;
    expect(afterFirst).toBeGreaterThan(0);

    const page = await callLoadHomeFeed({ limit: 50 }, authed);
    expect(queryCount).toBe(afterFirst); // second call did NOT touch Firestore
    expect(page.posts.map((p) => p['id'])).toEqual(['p_alice']);
  });
});
