/**
 * profile-card-media-gc.test.ts
 *
 * Covers the pure reconciliation core and the I/O-injected orchestration:
 *  - card-media path parsing + owner extraction
 *  - over-inclusive reference extraction (any media-path string anywhere in the payload)
 *  - the SAFETY CONTRACT: delete only unreferenced + old + known-age; skip a user on an unparseable
 *    doc; protect recent/unknown-age objects; reclaim deleted-account media (no docs)
 *  - dry-run plans without deleting; user cap is reported, not silent
 */

import {
  CardMediaGcIo,
  CardMediaObject,
  DEFAULT_GRACE_MS,
  isCardMediaPath,
  ownerUidOfCardMedia,
  planUserOrphans,
  reconcileAllCardMedia,
  referencedMediaPathsFromPayload,
} from './lib/profileCardMediaGc';

const NOW = 1_800_000_000_000;
const OLD = NOW - DEFAULT_GRACE_MS - 1; // just past the grace window
const RECENT = NOW - 1_000; // well within grace

function obj(path: string, createdAtMs: number): CardMediaObject {
  return { path, createdAtMs };
}

function doc(payload: string | null) {
  return { exists: payload !== null, payload };
}

function payloadWithPaths(...paths: string[]): string {
  return JSON.stringify({
    appearance: { accentArgb: 123, screen: { type: 'SOLID' } },
    backgroundMedia: paths[0] ? { id: 'm0', storagePath: paths[0], downloadUrl: 'https://x/m0' } : null,
    canvas: paths.slice(1).map((p, i) => ({ id: i, type: 'MEDIA', media: { id: `c${i}`, storagePath: p } })),
  });
}

describe('card-media path parsing', () => {
  it('matches private and public media paths and extracts the uid', () => {
    expect(isCardMediaPath('profile_cards_public/u1/media/m1.jpg')).toBe(true);
    expect(isCardMediaPath('profile_cards_private/u1/media/m1.png')).toBe(true);
    expect(ownerUidOfCardMedia('profile_cards_public/abc-123/media/x.webp')).toBe('abc-123');
  });

  it('rejects non-media and folder-placeholder paths', () => {
    expect(isCardMediaPath('profile_cards_public/u1/media/')).toBe(false); // empty file segment
    expect(isCardMediaPath('profile_cards_public/u1/avatar/m1.jpg')).toBe(false);
    expect(isCardMediaPath('profile_visuals/u1/media/m1.jpg')).toBe(false);
    expect(isCardMediaPath('profile_cards_public/u1/media/sub/m1.jpg')).toBe(false); // nested
    expect(ownerUidOfCardMedia('profile_visuals/u1/x.jpg')).toBeNull();
  });
});

describe('referencedMediaPathsFromPayload', () => {
  it('collects media paths from background and canvas, filtered to the uid', () => {
    const refs = referencedMediaPathsFromPayload(
      payloadWithPaths(
        'profile_cards_public/u1/media/bg.jpg',
        'profile_cards_public/u1/media/c0.png',
        'profile_cards_private/u1/media/c1.gif',
      ),
      'u1',
    );
    expect(refs).not.toBeNull();
    expect([...refs!].sort()).toEqual([
      'profile_cards_private/u1/media/c1.gif',
      'profile_cards_public/u1/media/bg.jpg',
      'profile_cards_public/u1/media/c0.png',
    ]);
  });

  it('ignores another user\'s media paths', () => {
    const refs = referencedMediaPathsFromPayload(
      payloadWithPaths('profile_cards_public/u2/media/bg.jpg'),
      'u1',
    );
    expect(refs!.size).toBe(0);
  });

  it('finds a media path even in an unexpected (drifted) location', () => {
    const drifted = JSON.stringify({ weird: { nested: ['profile_cards_public/u1/media/x.jpg'] } });
    const refs = referencedMediaPathsFromPayload(drifted, 'u1');
    expect([...refs!]).toEqual(['profile_cards_public/u1/media/x.jpg']);
  });

  it('returns null on unparseable JSON so the caller skips deletion', () => {
    expect(referencedMediaPathsFromPayload('{not json', 'u1')).toBeNull();
  });
});

describe('planUserOrphans — SAFETY CONTRACT', () => {
  const refPath = 'profile_cards_public/u1/media/keep.jpg';
  const orphanPath = 'profile_cards_public/u1/media/orphan.jpg';

  it('deletes only unreferenced AND old objects; protects referenced and recent', () => {
    const objects = [
      obj(refPath, OLD), // referenced -> protected even though old
      obj(orphanPath, OLD), // unreferenced + old -> delete
      obj('profile_cards_public/u1/media/fresh.jpg', RECENT), // unreferenced but recent -> protect
    ];
    const plan = planUserOrphans('u1', objects, doc(payloadWithPaths(refPath)), doc(null), NOW, DEFAULT_GRACE_MS);
    expect(plan.skipped).toBe(false);
    expect(plan.toDelete).toEqual([orphanPath]);
    expect(plan.referencedCount).toBe(1);
    expect(plan.protectedRecent).toBe(1);
  });

  it('never deletes an object whose age is unknown', () => {
    const objects = [obj(orphanPath, Number.NaN)];
    const plan = planUserOrphans('u1', objects, doc(payloadWithPaths()), doc(null), NOW, DEFAULT_GRACE_MS);
    expect(plan.toDelete).toEqual([]);
    expect(plan.protectedUnknownAge).toBe(1);
  });

  it('skips the whole user when an existing doc is unparseable', () => {
    const objects = [obj(orphanPath, OLD)];
    const plan = planUserOrphans('u1', objects, doc('{broken'), doc(null), NOW, DEFAULT_GRACE_MS);
    expect(plan.skipped).toBe(true);
    expect(plan.skipReason).toBe('unparseable-doc');
    expect(plan.toDelete).toEqual([]);
  });

  it('skips the user when an existing doc has a non-string payload', () => {
    const objects = [obj(orphanPath, OLD)];
    const plan = planUserOrphans('u1', objects, { exists: true, payload: null }, doc(null), NOW, DEFAULT_GRACE_MS);
    expect(plan.skipped).toBe(true);
  });

  it('protects media referenced by EITHER the private or the public doc', () => {
    const objects = [obj(refPath, OLD)];
    // Public doc references it; private doc is empty.
    const plan = planUserOrphans('u1', objects, doc(payloadWithPaths()), doc(payloadWithPaths(refPath)), NOW, DEFAULT_GRACE_MS);
    expect(plan.toDelete).toEqual([]);
    expect(plan.referencedCount).toBe(1);
  });

  it('treats media of a deleted account (no docs) as deletable once old', () => {
    const objects = [obj(orphanPath, OLD), obj('profile_cards_public/u1/media/fresh.jpg', RECENT)];
    const plan = planUserOrphans('u1', objects, doc(null), doc(null), NOW, DEFAULT_GRACE_MS);
    expect(plan.toDelete).toEqual([orphanPath]); // fresh one still within grace
    expect(plan.protectedRecent).toBe(1);
  });
});

// ── Orchestration with a plain fake IO ────────────────────────────────────────

class FakeGcIo implements CardMediaGcIo {
  deleted: string[] = [];
  failOn = new Set<string>();
  constructor(
    private readonly objects: CardMediaObject[],
    private readonly docs: Record<string, { exists: boolean; payload: string | null }>,
  ) {}
  listAllObjects(): Promise<CardMediaObject[]> {
    return Promise.resolve(this.objects);
  }
  readDoc(collection: string, uid: string) {
    return Promise.resolve(this.docs[`${collection}/${uid}`] ?? { exists: false, payload: null });
  }
  deleteObject(path: string): Promise<void> {
    if (this.failOn.has(path)) return Promise.reject(new Error('storage down'));
    this.deleted.push(path);
    return Promise.resolve();
  }
  nowMs(): number {
    return NOW;
  }
}

describe('reconcileAllCardMedia', () => {
  it('deletes safe orphans across users and leaves referenced + recent media intact', async () => {
    const io = new FakeGcIo(
      [
        obj('profile_cards_public/u1/media/keep.jpg', OLD),
        obj('profile_cards_public/u1/media/orphan.jpg', OLD),
        obj('profile_cards_public/u2/media/fresh.jpg', RECENT),
        obj('profile_cards_public/u3/media/dead.jpg', OLD), // deleted account: no docs
      ],
      {
        'profile_cards_public/u1': doc(payloadWithPaths('profile_cards_public/u1/media/keep.jpg')),
      },
    );
    const summary = await reconcileAllCardMedia(io);
    expect(io.deleted.sort()).toEqual([
      'profile_cards_public/u1/media/orphan.jpg',
      'profile_cards_public/u3/media/dead.jpg',
    ]);
    expect(summary.deleted).toBe(2);
    expect(summary.usersScanned).toBe(3);
    expect(summary.objectsScanned).toBe(4);
    expect(summary.protectedRecent).toBe(1);
  });

  it('does not delete anything for a user whose doc is unparseable', async () => {
    const io = new FakeGcIo(
      [obj('profile_cards_public/u1/media/orphan.jpg', OLD)],
      { 'profile_cards_public/u1': doc('{broken') },
    );
    const summary = await reconcileAllCardMedia(io);
    expect(io.deleted).toEqual([]);
    expect(summary.skippedUsers).toBe(1);
    expect(summary.deleted).toBe(0);
  });

  it('dry-run plans deletions without deleting', async () => {
    const io = new FakeGcIo(
      [obj('profile_cards_public/u1/media/orphan.jpg', OLD)],
      {},
    );
    const summary = await reconcileAllCardMedia(io, { dryRun: true });
    expect(io.deleted).toEqual([]);
    expect(summary.plannedDeletions).toBe(1);
    expect(summary.deleted).toBe(0);
    expect(summary.dryRun).toBe(true);
  });

  it('counts delete failures without aborting the run', async () => {
    const io = new FakeGcIo(
      [
        obj('profile_cards_public/u1/media/a.jpg', OLD),
        obj('profile_cards_public/u1/media/b.jpg', OLD),
      ],
      {},
    );
    io.failOn.add('profile_cards_public/u1/media/a.jpg');
    const summary = await reconcileAllCardMedia(io);
    expect(summary.deleteFailures).toBe(1);
    expect(summary.deleted).toBe(1);
    expect(io.deleted).toEqual(['profile_cards_public/u1/media/b.jpg']);
  });

  it('ignores malformed paths and reports a user cap instead of silently truncating', async () => {
    const io = new FakeGcIo(
      [
        obj('profile_cards_public/u1/media/a.jpg', OLD),
        obj('profile_cards_public/u2/media/b.jpg', OLD),
        obj('garbage/not/media', OLD),
      ],
      {},
    );
    const summary = await reconcileAllCardMedia(io, { maxUsers: 1 });
    expect(summary.malformedPaths).toBe(1);
    expect(summary.usersScanned).toBe(1);
    expect(summary.cappedUsers).toBe(1);
  });
});
