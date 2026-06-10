/**
 * security.test.ts
 * Unit tests for the hardened Cloud Functions security layer.
 *
 * Tests cover:
 *  - Auth enforcement (unauthenticated calls rejected)
 *  - App Check enforcement
 *  - Input validation (bad payloads rejected)
 *  - Self-targeting prevention
 *  - Rate limiting
 *  - Block cascade on blockUser
 *  - Idempotency of follow/block/visit
 *  - Cross-user write prevention on deletePost / cancelFollowRequest / unblockUser
 *  - profileVisit: no-op on self / block / dedup window
 *  - createPost: ownership enforced, author identity from DB
 *  - Storage quota / rate-limit / suspend flow (existing tests preserved)
 */

// ─── Shared mock state ───────────────────────────────────────────────────────

const mockDocs: Record<string, Record<string, unknown>> = {};
const mockStorageObjects: Record<string, Record<string, unknown>> = {};

const mockGet = jest.fn((ref: { path: string }) => {
  const data = mockDocs[ref.path];
  return Promise.resolve({ exists: !!data, data: () => data ?? undefined, ref });
});

const mockSet = jest.fn(
  (ref: { path: string }, data: Record<string, unknown>, opts?: { merge?: boolean }) => {
    if (opts?.merge) {
      mockDocs[ref.path] = { ...(mockDocs[ref.path] ?? {}), ...data };
    } else {
      mockDocs[ref.path] = { ...data };
    }
  },
);

const mockUpdate = jest.fn((ref: { path: string }, data: Record<string, unknown>) => {
  mockDocs[ref.path] = { ...(mockDocs[ref.path] ?? {}), ...data };
});

const mockDelete = jest.fn((ref: { path: string }) => {
  delete mockDocs[ref.path];
});

const mockTransactionGet = jest.fn((ref: { path: string }) => mockGet(ref));
const mockTransactionSet = mockSet;
const mockTransactionUpdate = mockUpdate;
const mockTransactionDelete = mockDelete;

const mockRunTransaction = jest.fn(
  (callback: (tx: unknown) => Promise<unknown>) =>
    callback({
      get:    mockTransactionGet,
      set:    mockTransactionSet,
      update: mockTransactionUpdate,
      delete: mockTransactionDelete,
    }),
);

const mockFileDelete = jest.fn().mockResolvedValue(undefined);
const mockFileGetMetadata = jest.fn((path: string) => {
  const metadata = mockStorageObjects[path];
  if (!metadata) {
    return Promise.reject(Object.assign(new Error('missing storage object'), { code: 'storage/object-not-found' }));
  }
  return Promise.resolve([metadata]);
});

// ─── Mock firebase-admin ─────────────────────────────────────────────────────

jest.mock('firebase-admin', () => {
  const docBuilder = (path: string) => ({
    path,
    get: () => mockGet({ path }),
    set: (d: Record<string, unknown>, o?: { merge?: boolean }) => mockSet({ path }, d, o),
    update: (d: Record<string, unknown>) => mockUpdate({ path }, d),
    delete: () => mockDelete({ path }),
  });

  const collectionBuilder = (col: string) => ({
    doc: (id: string) => {
      const path = `${col}/${id}`;
      return {
        ...docBuilder(path),
        collection: (sub: string) => collectionBuilder(`${path}/${sub}`),
      };
    },
  });

  const firestoreFn = jest.fn(() => ({
    collection: collectionBuilder,
    runTransaction: mockRunTransaction,
    collectionGroup: (_col: string) => ({
      where: jest.fn().mockReturnThis(),
      limit: jest.fn().mockReturnThis(),
      get: jest.fn().mockResolvedValue({ empty: true, docs: [] }),
    }),
    batch: jest.fn(() => ({
      delete: jest.fn(),
      commit: jest.fn().mockResolvedValue(undefined),
    })),
  }));

  (firestoreFn as unknown as Record<string, unknown>)['FieldValue'] = {
    serverTimestamp: jest.fn(() => 'MOCK_TS'),
    increment: jest.fn((n: number) => n),
  };

  return {
    initializeApp: jest.fn(),
    firestore: firestoreFn,
    storage: jest.fn(() => ({
      bucket: jest.fn(() => ({
        file: jest.fn((path: string) => ({
          delete: mockFileDelete,
          getMetadata: () => mockFileGetMetadata(path),
        })),
        getFiles: jest.fn().mockResolvedValue([[]]),
      })),
    })),
  };
});

// ─── Helpers ─────────────────────────────────────────────────────────────────

type CallableContext = {
  auth?: { uid: string; token?: Record<string, unknown> };
  app?: object;
};

function authedCtx(uid = 'userA'): CallableContext {
  return { auth: { uid, token: { email_verified: true } }, app: {} };
}

function anonCtx(): CallableContext {
  return {};
}

function noAppCheckCtx(uid = 'userA'): CallableContext {
  return { auth: { uid, token: { email_verified: true } } }; // app === undefined
}

function unverifiedCtx(uid = 'userA'): CallableContext {
  return { auth: { uid, token: { email_verified: false } }, app: {} };
}

// ─── Import functions AFTER mocks ────────────────────────────────────────────

import * as functions from 'firebase-functions-test';
const testEnv = functions();

// We import through index so everything is wired up
import {
  followUser,
  blockUser,
  unblockUser,
  sendFollowRequest,
  cancelFollowRequest,
  acceptFollowRequest,
  recordProfileVisit,
  createPost,
  deletePost,
  enforceStorageQuotas,
} from './index';

// ─── Utility ─────────────────────────────────────────────────────────────────

function setDoc(collection: string, id: string, data: Record<string, unknown>) {
  mockDocs[`${collection}/${id}`] = data;
}

function clearDocs() {
  Object.keys(mockDocs).forEach((k) => delete mockDocs[k]);
  Object.keys(mockStorageObjects).forEach((k) => delete mockStorageObjects[k]);
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth & App Check guards
// ─────────────────────────────────────────────────────────────────────────────

describe('Auth & App Check guards', () => {
  const wrapped = {
    follow:   testEnv.wrap(followUser),
    block:    testEnv.wrap(blockUser),
    visit:    testEnv.wrap(recordProfileVisit),
    post:     testEnv.wrap(createPost),
  };

  it('rejects unauthenticated callers with unauthenticated error', async () => {
    await expect(wrapped.follow({ targetUserId: 'userB' }, anonCtx() as never))
      .rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('rejects missing App Check with unauthenticated error', async () => {
    await expect(wrapped.follow({ targetUserId: 'userB' }, noAppCheckCtx() as never))
      .rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('rejects unauthenticated blockUser', async () => {
    await expect(wrapped.block({ targetUserId: 'userB' }, anonCtx() as never))
      .rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('rejects unauthenticated recordProfileVisit', async () => {
    await expect(wrapped.visit({ profileOwnerId: 'userB' }, anonCtx() as never))
      .rejects.toMatchObject({ code: 'unauthenticated' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Input validation
// ─────────────────────────────────────────────────────────────────────────────

describe('Input validation', () => {
  const wrapped = {
    follow:  testEnv.wrap(followUser),
    block:   testEnv.wrap(blockUser),
    visit:   testEnv.wrap(recordProfileVisit),
    post:    testEnv.wrap(createPost),
  };

  it('rejects missing targetUserId for followUser', async () => {
    await expect(wrapped.follow({}, authedCtx() as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects invalid targetUserId characters for blockUser', async () => {
    await expect(wrapped.block({ targetUserId: '../../etc' }, authedCtx() as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects createPost with too many attachments', async () => {
    const manyAtts = Array(7).fill({
      id: 'att1', type: 'Image', fileName: 'a.jpg', mimeType: 'image/jpeg',
      sizeBytes: 1000, storagePath: 's', url: 'https://x.com/a',
    });
    await expect(
      wrapped.post(
        { postId: 'post1', postType: 'PHOTO', attachments: manyAtts },
        authedCtx() as never,
      ),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects createPost with unknown postType', async () => {
    await expect(
      wrapped.post(
        { postId: 'post1', postType: 'MEME', attachments: [] },
        authedCtx() as never,
      ),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it.each([
    "' OR '1'='1",
    "'; DROP TABLE users; --",
    '" OR ""="',
    "admin'--",
    '../../etc',
    'abc\u0000def',
  ])('rejects unsafe document ids: %s', async (payload) => {
    await expect(wrapped.visit({ profileOwnerId: payload }, authedCtx() as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects createPost text with control characters', async () => {
    await expect(
      wrapped.post(
        { postId: 'post1', postType: 'TEXT', text: 'hola\u0000mundo', attachments: [] },
        authedCtx() as never,
      ),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects createPost when caller email is not verified', async () => {
    await expect(
      wrapped.post(
        { postId: 'post1', postType: 'TEXT', text: 'hola mundo', attachments: [] },
        unverifiedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('allows SQL-like post text as inert Firestore data', async () => {
    clearDocs();
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });

    const result = await wrapped.post(
      {
        postId: 'post1',
        postType: 'TEXT',
        text: "' OR '1'='1\n%_%_%",
        attachments: [],
      },
      authedCtx('userA') as never,
    );

    expect(result).toMatchObject({ success: true, postId: 'post1' });
    expect(mockDocs['posts/post1']?.['text']).toBe("' OR '1'='1\n%_%_%");
  });

  it('rejects attachment paths with traversal', async () => {
    await expect(
      wrapped.post(
        {
          postId: 'post1',
          postType: 'PHOTO',
          attachments: [{
            id: 'att1',
            type: 'Image',
            fileName: '../a.jpg',
            mimeType: 'image/jpeg',
            sizeBytes: 1000,
            storagePath: '../post_attachments/userA/post1/att1.jpg',
            url: 'https://x.com/a',
          }],
        },
        authedCtx() as never,
      ),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
function validPhotoAttachment(ownerUid = 'userA', postId = 'postPhoto1', attachmentId = 'att1') {
  const storagePath = `post_attachments/${ownerUid}/${postId}/${attachmentId}.jpg`;
  return {
    id: attachmentId,
    type: 'Image',
    fileName: `${attachmentId}.jpg`,
    mimeType: 'image/jpeg',
    sizeBytes: 1000,
    storagePath,
    url: storagePath,
    thumbnailUrl: `post_attachments/${ownerUid}/${postId}/${attachmentId}_thumb.jpg`,
  };
}

function seedAttachmentStorage(
  attachment: ReturnType<typeof validPhotoAttachment>,
  ownerUid = 'userA',
  postId = 'postPhoto1',
  overrides: Record<string, unknown> = {},
) {
  mockStorageObjects[attachment.storagePath] = {
    contentType: attachment.mimeType,
    size: String(attachment.sizeBytes),
    metadata: {
      ownerUid,
      postId,
      visibility: 'private',
    },
    ...overrides,
  };
}

function seedAttachmentThumbnail(
  attachment: ReturnType<typeof validPhotoAttachment>,
  ownerUid = 'userA',
  postId = 'postPhoto1',
  overrides: Record<string, unknown> = {},
) {
  if (!attachment.thumbnailUrl) throw new Error('test attachment has no thumbnailUrl');
  mockStorageObjects[attachment.thumbnailUrl] = {
    contentType: 'image/jpeg',
    size: String(256 * 1024),
    metadata: {
      ownerUid,
      postId,
      visibility: 'private',
    },
    ...overrides,
  };
}

describe('createPost attachment validation parity', () => {
  const wrapped = testEnv.wrap(createPost);

  beforeEach(() => {
    clearDocs();
    jest.clearAllMocks();
  });

  it('rejects persistent external download URLs', async () => {
    const attachment = {
      ...validPhotoAttachment(),
      url: 'http://example.test/bearer-token.jpg',
    };

    await expect(
      wrapped({ postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects attachment paths owned by another user', async () => {
    const attachment = validPhotoAttachment('userB', 'postPhoto1');

    await expect(
      wrapped({ postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects size excessive for MIME type', async () => {
    const attachment = {
      ...validPhotoAttachment(),
      sizeBytes: 11 * 1024 * 1024,
    };

    await expect(
      wrapped({ postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  // ── P0.3: numeric field hardening (reject NaN / Infinity / negative / float) ──
  it.each([
    ['NaN sizeBytes', { sizeBytes: Number.NaN }],
    ['Infinity sizeBytes', { sizeBytes: Number.POSITIVE_INFINITY }],
    ['fractional sizeBytes', { sizeBytes: 1024.5 }],
    ['zero sizeBytes', { sizeBytes: 0 }],
    ['NaN width', { width: Number.NaN }],
    ['Infinity height', { height: Number.POSITIVE_INFINITY }],
    ['negative height', { height: -10 }],
    ['fractional width', { width: 12.7 }],
  ])('rejects %s', async (_label, override) => {
    const attachment = { ...validPhotoAttachment(), ...override };
    await expect(
      wrapped({ postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('accepts valid integer width/height', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachment = { ...validPhotoAttachment(), width: 1080, height: 1920 };
    seedAttachmentStorage(attachment);
    seedAttachmentThumbnail(attachment);

    const result = await wrapped(
      { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ success: true, postId: 'postPhoto1' });
  });

  it('rejects TEXT posts with attachments', async () => {
    await expect(
      wrapped(
        { postId: 'postText1', postType: 'TEXT', text: 'hello', attachments: [validPhotoAttachment('userA', 'postText1')] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('rejects MIME/type mismatch', async () => {
    const attachment = {
      ...validPhotoAttachment(),
      mimeType: 'audio/mpeg',
      storagePath: 'post_attachments/userA/postPhoto1/att1.mp3',
      url: 'post_attachments/userA/postPhoto1/att1.mp3',
    };

    await expect(
      wrapped({ postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('accepts a valid photo attachment stored as a revocable Storage path', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachmentInput = validPhotoAttachment();
    seedAttachmentStorage(attachmentInput);
    seedAttachmentThumbnail(attachmentInput);

    const result = await wrapped(
      { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachmentInput] },
      authedCtx('userA') as never,
    );

    expect(result).toMatchObject({ success: true, postId: 'postPhoto1' });
    const attachment = (mockDocs['posts/postPhoto1']?.['attachments'] as Array<Record<string, unknown>>)[0];
    expect(attachment['url']).toBe('post_attachments/userA/postPhoto1/att1.jpg');
    expect(attachment['url']).toBe(attachment['storagePath']);
  });

  it('rejects a valid-looking attachment when the Storage object is missing', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });

    await expect(
      wrapped(
        { postId: 'postPhoto1', postType: 'PHOTO', attachments: [validPhotoAttachment()] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('rejects attachment Storage metadata owned by another user', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachment = validPhotoAttachment();
    seedAttachmentStorage(attachment, 'userB', 'postPhoto1');
    seedAttachmentThumbnail(attachment);

    await expect(
      wrapped(
        { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('rejects attachment Storage payload metadata mismatch', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachment = validPhotoAttachment();
    seedAttachmentStorage(attachment, 'userA', 'postPhoto1', { contentType: 'image/png' });
    seedAttachmentThumbnail(attachment);

    await expect(
      wrapped(
        { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('rejects a valid-looking attachment when the thumbnail object is missing', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachment = validPhotoAttachment();
    seedAttachmentStorage(attachment);

    await expect(
      wrapped(
        { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('rejects thumbnail Storage metadata mismatch', async () => {
    setDoc('profile_visuals', 'userA', { username: 'alice', displayName: 'Alice' });
    const attachment = validPhotoAttachment();
    seedAttachmentStorage(attachment);
    seedAttachmentThumbnail(attachment, 'userA', 'postPhoto1', { size: String(2 * 1024 * 1024) });

    await expect(
      wrapped(
        { postId: 'postPhoto1', postType: 'PHOTO', attachments: [attachment] },
        authedCtx('userA') as never,
      ),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });
});

describe('sendFollowRequest target privacy validation', () => {
  const wrapped = testEnv.wrap(sendFollowRequest);

  beforeEach(() => {
    clearDocs();
    jest.clearAllMocks();
  });

  it('rejects when the target privacy document does not exist', async () => {
    await expect(
      wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  it('rejects public targets because they must be followed directly', async () => {
    setDoc('privacy', 'userB', { profileVisibility: 'Public' });

    await expect(
      wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('creates a pending request for private targets', async () => {
    setDoc('privacy', 'userB', { profileVisibility: 'Private' });

    const result = await wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never);

    expect(result).toMatchObject({ success: true });
    expect(mockDocs['followRequests/userA_userB']).toEqual(
      expect.objectContaining({ requesterId: 'userA', targetId: 'userB', status: 'Pending' }),
    );
  });
});

// Self-targeting prevention
// ─────────────────────────────────────────────────────────────────────────────

describe('Self-targeting prevention', () => {
  it('followUser rejects self-follow', async () => {
    const wrapped = testEnv.wrap(followUser);
    await expect(wrapped({ targetUserId: 'userA' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('blockUser rejects self-block', async () => {
    const wrapped = testEnv.wrap(blockUser);
    await expect(wrapped({ targetUserId: 'userA' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('sendFollowRequest rejects self-request', async () => {
    const wrapped = testEnv.wrap(sendFollowRequest);
    await expect(wrapped({ targetUserId: 'userA' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// profileVisit: self, block, dedup
// ─────────────────────────────────────────────────────────────────────────────

describe('recordProfileVisit', () => {
  beforeEach(() => {
    clearDocs();
    jest.clearAllMocks();
    // Clear rate_limit docs so rate limit doesn't fire
  });

  it('is a no-op for self-visit', async () => {
    const wrapped = testEnv.wrap(recordProfileVisit);
    const result  = await wrapped(
      { profileOwnerId: 'userA' },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ recorded: false });
    expect(mockRunTransaction).not.toHaveBeenCalled();
  });

  it('is a no-op when a block exists (blocker → blocked)', async () => {
    setDoc('blocks', 'userA_userB', { blockerId: 'userA', blockedId: 'userB', createdAt: 0 });
    const wrapped = testEnv.wrap(recordProfileVisit);
    const result  = await wrapped(
      { profileOwnerId: 'userB' },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ recorded: false });
  });

  it('is a no-op when a block exists (blocked → blocker direction)', async () => {
    setDoc('blocks', 'userB_userA', { blockerId: 'userB', blockedId: 'userA', createdAt: 0 });
    const wrapped = testEnv.wrap(recordProfileVisit);
    const result  = await wrapped(
      { profileOwnerId: 'userB' },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ recorded: false });
  });

  it('records first visit', async () => {
    const wrapped = testEnv.wrap(recordProfileVisit);
    const result  = await wrapped(
      { profileOwnerId: 'userB' },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ recorded: true });
  });

  it('deduplicates visits within 10-minute window', async () => {
    const recentTs = Date.now() - 60_000; // 1 min ago — within 10-min window
    setDoc('profile_visits', 'userA_userB', {
      viewerUserId:    'userA',
      profileOwnerId:  'userB',
      hasVisitedProfile: true,
      firstVisitedAt:  recentTs,
      lastVisitedAt:   recentTs,
    });

    const wrapped = testEnv.wrap(recordProfileVisit);
    // Transaction runs but does NOT call update because within dedup window
    const result  = await wrapped(
      { profileOwnerId: 'userB' },
      authedCtx('userA') as never,
    );
    expect(result).toMatchObject({ success: true });
    // mockTransactionUpdate should NOT have been called
    expect(mockTransactionUpdate).not.toHaveBeenCalled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// blockUser cascade
// ─────────────────────────────────────────────────────────────────────────────

describe('blockUser cascade', () => {
  beforeEach(() => {
    clearDocs();
    jest.clearAllMocks();
  });

  it('removes follow A→B when blocking B', async () => {
    setDoc('follows', 'userA_userB', { followerId: 'userA', followeeId: 'userB', createdAt: 0 });
    setDoc('counters', 'userA', { followingCount: 1, followersCount: 0, blockedUsersCount: 0, outgoingPendingRequestsCount: 0, incomingPendingRequestsCount: 0 });
    setDoc('counters', 'userB', { followingCount: 0, followersCount: 1, blockedUsersCount: 0, outgoingPendingRequestsCount: 0, incomingPendingRequestsCount: 0 });

    const wrapped = testEnv.wrap(blockUser);
    await wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never);

    expect(mockDocs['follows/userA_userB']).toBeUndefined();
    expect(mockDocs['blocks/userA_userB']).toBeDefined();
    expect(mockDocs['counters/userA']?.['followingCount']).toBe(0);
    expect(mockDocs['counters/userB']?.['followersCount']).toBe(0);
  });

  it('removes follow B→A when blocking B', async () => {
    setDoc('follows', 'userB_userA', { followerId: 'userB', followeeId: 'userA', createdAt: 0 });
    setDoc('counters', 'userA', { followingCount: 0, followersCount: 1, blockedUsersCount: 0, outgoingPendingRequestsCount: 0, incomingPendingRequestsCount: 0 });
    setDoc('counters', 'userB', { followingCount: 1, followersCount: 0, blockedUsersCount: 0, outgoingPendingRequestsCount: 0, incomingPendingRequestsCount: 0 });

    const wrapped = testEnv.wrap(blockUser);
    await wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never);

    expect(mockDocs['follows/userB_userA']).toBeUndefined();
    expect(mockDocs['counters/userA']?.['followersCount']).toBe(0);
  });

  it('invalidates pending follow request when blocking', async () => {
    setDoc('followRequests', 'userB_userA', {
      id: 'userB_userA', requesterId: 'userB', targetId: 'userA',
      status: 'Pending', createdAt: 0, resolvedAt: null,
    });
    setDoc('counters', 'userA', { incomingPendingRequestsCount: 1, followingCount: 0, followersCount: 0, blockedUsersCount: 0, outgoingPendingRequestsCount: 0 });
    setDoc('counters', 'userB', { outgoingPendingRequestsCount: 1, followingCount: 0, followersCount: 0, blockedUsersCount: 0, incomingPendingRequestsCount: 0 });

    const wrapped = testEnv.wrap(blockUser);
    await wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never);

    expect(mockDocs['followRequests/userB_userA']?.['status']).toBe('InvalidatedByBlock');
    expect(mockDocs['counters/userA']?.['incomingPendingRequestsCount']).toBe(0);
    expect(mockDocs['counters/userB']?.['outgoingPendingRequestsCount']).toBe(0);
  });

  it('is idempotent: double-blocking is a no-op', async () => {
    setDoc('blocks', 'userA_userB', { blockerId: 'userA', blockedId: 'userB', createdAt: 0 });
    // Pre-set a counter doc so we can verify it wasn't mutated
    setDoc('counters', 'userA', { blockedUsersCount: 1, followingCount: 0, followersCount: 0, outgoingPendingRequestsCount: 0, incomingPendingRequestsCount: 0 });

    const wrapped = testEnv.wrap(blockUser);
    await wrapped({ targetUserId: 'userB' }, authedCtx('userA') as never);

    // Block doc still exists unchanged, counter NOT incremented again
    expect(mockDocs['blocks/userA_userB']).toBeDefined();
    expect(mockDocs['counters/userA']?.['blockedUsersCount']).toBe(1);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// unblockUser: cross-user protection
// ─────────────────────────────────────────────────────────────────────────────

describe('unblockUser cross-user protection', () => {
  beforeEach(() => { clearDocs(); jest.clearAllMocks(); });

  it('prevents removing another user\'s block (blockerId mismatch)', async () => {
    // userA_userC block where blockerId is 'userB' (tampered / wrong caller)
    setDoc('blocks', 'userA_userC', { blockerId: 'userB', blockedId: 'userC', createdAt: 0 });

    // userA tries to remove blocks/userA_userC but that block belongs to userB
    const wrapped = testEnv.wrap(unblockUser);
    await expect(wrapped({ targetUserId: 'userC' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'permission-denied' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// cancelFollowRequest: only requester can cancel
// ─────────────────────────────────────────────────────────────────────────────

describe('cancelFollowRequest', () => {
  beforeEach(() => { clearDocs(); jest.clearAllMocks(); });

  it('rejects cancellation by non-requester', async () => {
    setDoc('followRequests', 'userB_userC', {
      id: 'userB_userC', requesterId: 'userB', targetId: 'userC',
      status: 'Pending', createdAt: 0, resolvedAt: null,
    });

    const wrapped = testEnv.wrap(cancelFollowRequest);
    await expect(wrapped({ requestId: 'userB_userC' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'permission-denied' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// acceptFollowRequest: only target can accept
// ─────────────────────────────────────────────────────────────────────────────

describe('acceptFollowRequest', () => {
  beforeEach(() => { clearDocs(); jest.clearAllMocks(); });

  it('rejects acceptance by non-target', async () => {
    setDoc('followRequests', 'userB_userC', {
      id: 'userB_userC', requesterId: 'userB', targetId: 'userC',
      status: 'Pending', createdAt: 0, resolvedAt: null,
    });

    const wrapped = testEnv.wrap(acceptFollowRequest);
    await expect(wrapped({ requestId: 'userB_userC' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('rejects acceptance when a block appeared after request was sent', async () => {
    setDoc('followRequests', 'userB_userA', {
      id: 'userB_userA', requesterId: 'userB', targetId: 'userA',
      status: 'Pending', createdAt: 0, resolvedAt: null,
    });
    // Block appeared after the request
    setDoc('blocks', 'userA_userB', { blockerId: 'userA', blockedId: 'userB', createdAt: 1 });

    const wrapped = testEnv.wrap(acceptFollowRequest);
    await expect(wrapped({ requestId: 'userB_userA' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'permission-denied' });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// deletePost: cross-user protection
// ─────────────────────────────────────────────────────────────────────────────

describe('deletePost', () => {
  beforeEach(() => { clearDocs(); jest.clearAllMocks(); });

  it('prevents deleting another user\'s post', async () => {
    setDoc('posts', 'post123', {
      id: 'post123', userId: 'userB', postType: 'TEXT',
      attachments: [], createdAt: 0, updatedAt: 0,
      authorUsername: 'bob', authorDisplayName: 'Bob',
    });

    const wrapped = testEnv.wrap(deletePost);
    await expect(wrapped({ requestId: 'post123' }, authedCtx('userA') as never))
      .rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('allows owner to delete their own post', async () => {
    setDoc('posts', 'post123', {
      id: 'post123', userId: 'userA', postType: 'TEXT',
      attachments: [], createdAt: 0, updatedAt: 0,
      authorUsername: 'alice', authorDisplayName: 'Alice',
    });

    const wrapped = testEnv.wrap(deletePost);
    const result  = await wrapped({ requestId: 'post123' }, authedCtx('userA') as never);
    expect(result).toMatchObject({ success: true });
    expect(mockDocs['posts/post123']).toBeUndefined();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Storage quota tests (existing — preserved)
// ─────────────────────────────────────────────────────────────────────────────

describe('enforceStorageQuotas (existing)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    clearDocs();
    mockTransactionGet.mockImplementation(mockGet as jest.Mock);
    mockFileDelete.mockResolvedValue(undefined);
  });

  it('deletes file if no ownerUid metadata', async () => {
    const wrapped = testEnv.wrap(enforceStorageQuotas);
    await wrapped({ bucket: 'b', name: 'f.jpg', size: '100', metadata: {} } as never);
    expect(mockFileDelete).toHaveBeenCalled();
  });

  it('updates ledger on normal upload', async () => {
    const wrapped = testEnv.wrap(enforceStorageQuotas);
    await wrapped({
      bucket: 'b', name: 'f.jpg', size: '1024',
      metadata: { ownerUid: 'user123' },
    } as never);
    expect(mockFileDelete).not.toHaveBeenCalled();
  });

  it('suspends user and deletes file when storage quota exceeded', async () => {
    setDoc('storage_usage', 'user123', {
      totalBytes: 500 * 1024 * 1024, totalFiles: 10,
      filesInWindow: 1, windowStart: Date.now() - 1000,
      isSuspended: false,
    });
    const wrapped = testEnv.wrap(enforceStorageQuotas);
    await wrapped({
      bucket: 'b', name: 'f.jpg', size: '1024',
      metadata: { ownerUid: 'user123' },
    } as never);
    expect(mockFileDelete).toHaveBeenCalled();
  });

  it('suspends user and deletes file when rate limit exceeded', async () => {
    setDoc('storage_usage', 'user123', {
      totalBytes: 1024, totalFiles: 5,
      filesInWindow: 20, windowStart: Date.now() - 1000,
      isSuspended: false,
    });
    const wrapped = testEnv.wrap(enforceStorageQuotas);
    await wrapped({
      bucket: 'b', name: 'f.jpg', size: '1024',
      metadata: { ownerUid: 'user123' },
    } as never);
    expect(mockFileDelete).toHaveBeenCalled();
  });

  it('redacts raw ownerUid and file path in quota logs', async () => {
    const fs = require('fs') as typeof import('fs');
    const path = require('path') as typeof import('path');
    const source = fs.readFileSync(path.join(__dirname, 'storage.ts'), 'utf8');

    expect(source).not.toContain('User ${uid}');
    expect(source).not.toContain('File ${name}');
    expect(source).not.toContain('Failed to delete file ${name}');
    expect(source).toContain("logSafeRef(uid, 'uid')");
    expect(source).toContain('logSafeStoragePrefix(name)');
  });
});
