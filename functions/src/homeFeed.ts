/**
 * homeFeed.ts
 * Callable Cloud Function: loadHomeFeed — fan-out-on-read home feed (audit A2 / S4).
 *
 * The materialized collection feeds/{viewer}/posts has NO writer (createPost only writes
 * posts/ + user_posts/); it is always empty in production. Rather than fan-out-on-write
 * (celebrity write-storm), this callable computes the feed on demand:
 *
 *   1. authors = viewer's followees (cap FOLLOWING_CAP) + the viewer (own posts)
 *   2. chunk authors into groups of <= CHUNK_SIZE (Firestore `in` limit)
 *   3. per group: posts where userId in <group> orderBy createdAt desc limit PAGE
 *      (requires composite index posts(userId ASC, createdAt DESC))
 *   4. merge, dedupe by postId, sort by createdAt desc, take PAGE
 *
 * PRIVACY (Admin SDK bypasses Rules — this derivation IS the authorization):
 *   Iterating the viewer's FOLLOWEES means exists(follows/{viewer}_{author}) is true by
 *   construction, so postPrivacyAllowsRead (firestore.rules) holds for public AND private
 *   authors; blockUser deletes both follows docs, so a live follow guarantees no block.
 *   The viewer's own posts pass isOwner. Hence every returned post satisfies canReadPostData
 *   with no extra privacy/block reads.
 *
 * CACHE (DEC-2: no Memorystore, no new Firestore collection):
 *   Per-instance best-effort in-memory Map keyed by viewer:limit, TTL CACHE_TTL_MS. Ephemeral
 *   (evaporates on cold start, never persists cross-user data at rest → no GDPR erasure surface).
 *   Ceiling: a block/privacy change is reflected within <= CACHE_TTL_MS (eventual consistency).
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth } from './lib/guards';
import { logSafeRef } from './lib/privacyLogging';

const db = () => admin.firestore();

const FOLLOWING_CAP = 200;      // max followees fanned out (beyond this, subset by follow doc-id order)
const CHUNK_SIZE = 30;          // Firestore `in` operator limit
const DEFAULT_PAGE = 50;
const MAX_PAGE = 100;           // mirrors client GlobalFeedMaxPageSize
const CACHE_TTL_MS = 45_000;
const CACHE_MAX_ENTRIES = 500;

type FlatPost = Record<string, unknown>;
export interface HomeFeedPage { posts: FlatPost[]; }
export interface FeedCandidate { postId: string; createdAtMs: number; post: FlatPost; }

interface FeedDoc { id: string; data: () => Record<string, unknown> | undefined; }

// ── Pure helpers (unit-tested directly) ────────────────────────────────────────

export function clampLimit(raw: unknown): number {
  const n = typeof raw === 'number' && Number.isFinite(raw) ? Math.floor(raw) : DEFAULT_PAGE;
  return Math.min(Math.max(n, 1), MAX_PAGE);
}

export function chunk<T>(items: T[], size: number): T[][] {
  if (size <= 0) return items.length === 0 ? [] : [items];
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) out.push(items.slice(i, i + size));
  return out;
}

function toMillis(value: unknown): number | null {
  const ts = value as { toMillis?: () => number } | null;
  if (ts && typeof ts.toMillis === 'function') return ts.toMillis();
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  return null;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/** Projects a posts/ doc into the flat shape the client parses (createdAt as epoch-millis). */
export function projectFeedPost(doc: FeedDoc): FeedCandidate | null {
  const data = doc.data();
  if (!data) return null;
  const createdAtMs = toMillis(data['createdAt']);
  if (createdAtMs === null) return null;
  const postId = typeof data['id'] === 'string' && data['id'] ? (data['id'] as string) : doc.id;
  const post: FlatPost = {
    id: postId,
    userId: typeof data['userId'] === 'string' ? data['userId'] : null,
    authorUsername: typeof data['authorUsername'] === 'string' ? data['authorUsername'] : null,
    authorDisplayName: typeof data['authorDisplayName'] === 'string' ? data['authorDisplayName'] : null,
    postType: typeof data['postType'] === 'string' ? data['postType'] : null,
    text: typeof data['text'] === 'string' ? data['text'] : null,
    description: typeof data['description'] === 'string' ? data['description'] : null,
    textStyle: isPlainObject(data['textStyle']) ? data['textStyle'] : null,
    attachments: Array.isArray(data['attachments']) ? data['attachments'] : [],
    createdAt: createdAtMs,
  };
  return { postId, createdAtMs, post };
}

/** Flatten + dedupe by postId + sort createdAt desc (stable postId tiebreak) + cap. */
export function mergeFeedPage(candidates: FeedCandidate[], limit: number): HomeFeedPage {
  const seen = new Set<string>();
  const unique: FeedCandidate[] = [];
  for (const c of candidates) {
    if (seen.has(c.postId)) continue;
    seen.add(c.postId);
    unique.push(c);
  }
  unique.sort((a, b) => {
    if (b.createdAtMs !== a.createdAtMs) return b.createdAtMs - a.createdAtMs;
    if (a.postId === b.postId) return 0;
    return a.postId < b.postId ? 1 : -1;
  });
  return { posts: unique.slice(0, Math.max(0, limit)).map((c) => c.post) };
}

// ── Per-instance best-effort cache (DEC-2) ─────────────────────────────────────

interface FeedCacheEntry { expiresAtMs: number; page: HomeFeedPage; }
const feedCache = new Map<string, FeedCacheEntry>();

export function feedCacheKey(viewer: string, limit: number): string {
  return `${viewer}:${limit}`;
}

export function readFeedCache(key: string, nowMs: number): HomeFeedPage | null {
  const entry = feedCache.get(key);
  if (!entry) return null;
  if (entry.expiresAtMs <= nowMs) {
    feedCache.delete(key);
    return null;
  }
  return entry.page;
}

export function writeFeedCache(key: string, page: HomeFeedPage, nowMs: number): void {
  if (feedCache.size >= CACHE_MAX_ENTRIES && !feedCache.has(key)) {
    const oldest = feedCache.keys().next().value;
    if (oldest !== undefined) feedCache.delete(oldest);
  }
  feedCache.set(key, { expiresAtMs: nowMs + CACHE_TTL_MS, page });
}

/** Test-only: clear the module-level cache between cases. */
export function __clearFeedCacheForTest(): void {
  feedCache.clear();
}

// ── Firestore I/O ──────────────────────────────────────────────────────────────

async function resolveFeedAuthors(viewer: string): Promise<string[]> {
  const snap = await db().collection('follows')
    .where('followerId', '==', viewer)
    .limit(FOLLOWING_CAP)
    .get();
  const ids = new Set<string>([viewer]); // include the viewer's own posts
  snap.docs.forEach((d: FeedDoc) => {
    const followee = d.data()?.['followeeId'];
    if (typeof followee === 'string' && followee) ids.add(followee);
  });
  return [...ids];
}

async function fetchCandidateChunk(authorIds: string[], limit: number): Promise<FeedCandidate[]> {
  if (authorIds.length === 0) return [];
  const snap = await db().collection('posts')
    .where('userId', 'in', authorIds)
    .orderBy('createdAt', 'desc')
    .limit(limit)
    .get();
  const out: FeedCandidate[] = [];
  snap.docs.forEach((d: FeedDoc) => {
    const candidate = projectFeedPost(d);
    if (candidate) out.push(candidate);
  });
  return out;
}

async function fetchCandidatePosts(authorIds: string[], limit: number): Promise<FeedCandidate[]> {
  const groups = chunk(authorIds, CHUNK_SIZE);
  const perGroup = await Promise.all(groups.map((group) => fetchCandidateChunk(group, limit)));
  return perGroup.flat();
}

// ── Callable ────────────────────────────────────────────────────────────────────

export const loadHomeFeed = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewer = requireAuth(context);
  const limit = clampLimit(data?.limit);
  const nowMs = Date.now();
  const cacheKey = feedCacheKey(viewer, limit);

  const cached = readFeedCache(cacheKey, nowMs);
  if (cached) return cached;

  const authorIds = await resolveFeedAuthors(viewer);
  const candidates = await fetchCandidatePosts(authorIds, limit);
  const page = mergeFeedPage(candidates, limit);

  writeFeedCache(cacheKey, page, nowMs);
  functions.logger.info('loadHomeFeed: complete', {
    viewerRef: logSafeRef(viewer),
    authorCount: authorIds.length,
    postCount: page.posts.length,
  });
  return page;
});
