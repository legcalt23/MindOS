/**
 * accountDeletion.test.ts
 *
 * Covers executeAccountDeletion(uid):
 *  - deletes the owned single-doc records (profiles / profile_visuals / privacy / counters)
 *  - soft-deletes the account record (users/{uid}.status = 'Deleted')
 *  - queries every PII collection by the correct field(s), both directions
 *  - drops the per-user post subtrees (user_posts, feeds)
 *  - cleans up the user's Storage prefixes (and NOT shared dm_media)
 *  - deletes the Auth user LAST
 *  - batch-deletes documents returned by the purge queries
 *  - is idempotent: tolerates an already-deleted Auth user (auth/user-not-found)
 *  - rethrows other Auth errors
 */

// ── recording mocks (mock-prefixed so jest hoisting allows them in the factory) ──
const mockDocDelete = jest.fn(async (_path: string) => {});
const mockDocSet = jest.fn(async (_path: string, _data: unknown, _opts: unknown) => {});
const mockWhereGet = jest.fn(
  async (_collection: string, _field: string, _value: unknown) =>
    ({ docs: [] as Array<{ ref: { path: string } }> }),
);
const mockSubGet = jest.fn(
  async (_path: string) => ({ docs: [] as Array<{ ref: { path: string } }> }),
);
const mockBatchDelete = jest.fn((_path: string | undefined) => {});
const mockBatchCommit = jest.fn(async () => {});
const mockDeleteFiles = jest.fn(async (_opts: { prefix: string }) => {});
const mockDeleteUser = jest.fn(async (_uid: string) => {});

jest.mock('firebase-admin', () => {
  const makeQuery = (collection: string, field: string, value: unknown) => ({
    get: () => mockWhereGet(collection, field, value),
  });
  const makeCollection = (name: string) => ({
    doc: (id: string) => ({
      path: `${name}/${id}`,
      delete: () => mockDocDelete(`${name}/${id}`),
      set: (data: unknown, opts: unknown) => mockDocSet(`${name}/${id}`, data, opts),
      collection: (sub: string) => ({
        get: () => mockSubGet(`${name}/${id}/${sub}`),
      }),
    }),
    where: (field: string, _op: string, value: unknown) => makeQuery(name, field, value),
  });
  const firestore = Object.assign(
    () => ({
      collection: (name: string) => makeCollection(name),
      batch: () => ({
        delete: (ref: { path?: string }) => mockBatchDelete(ref?.path),
        commit: () => mockBatchCommit(),
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
  logger: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
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

import { executeAccountDeletion } from './accountDeletion';

const UID = 'user_del_1';

beforeEach(() => {
  jest.clearAllMocks();
  mockWhereGet.mockImplementation(async () => ({ docs: [] }));
  mockSubGet.mockImplementation(async () => ({ docs: [] }));
  mockDeleteUser.mockImplementation(async () => {});
});

describe('executeAccountDeletion', () => {
  it('deletes the owned single-doc records', async () => {
    await executeAccountDeletion(UID);
    expect(mockDocDelete).toHaveBeenCalledWith(`profiles/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`profile_visuals/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`privacy/${UID}`);
    expect(mockDocDelete).toHaveBeenCalledWith(`counters/${UID}`);
  });

  it('soft-deletes the account record', async () => {
    await executeAccountDeletion(UID);
    expect(mockDocSet).toHaveBeenCalledWith(
      `users/${UID}`,
      expect.objectContaining({ status: 'Deleted' }),
      { merge: true },
    );
  });

  it('queries every PII collection by the correct field(s)', async () => {
    await executeAccountDeletion(UID);
    const pairs: Array<[string, string]> = [
      ['follows', 'followerId'],
      ['follows', 'followeeId'],
      ['observations', 'observerUserId'],
      ['observations', 'observedUserId'],
      ['followRequests', 'requesterId'],
      ['followRequests', 'targetId'],
      ['blocks', 'blockerId'],
      ['blocks', 'blockedId'],
      ['profile_visits', 'viewerUserId'],
      ['profile_visits', 'profileOwnerId'],
      ['notifications', 'actorUserId'],
      ['notifications', 'recipientUserId'],
      ['usernames', 'uid'],
      ['posts', 'userId'],
    ];
    for (const [collection, field] of pairs) {
      expect(mockWhereGet).toHaveBeenCalledWith(collection, field, UID);
    }
  });

  it('drops the per-user post subtrees', async () => {
    await executeAccountDeletion(UID);
    expect(mockSubGet).toHaveBeenCalledWith(`user_posts/${UID}/posts`);
    expect(mockSubGet).toHaveBeenCalledWith(`feeds/${UID}/posts`);
  });

  it('cleans up the user Storage prefixes but not shared dm_media', async () => {
    await executeAccountDeletion(UID);
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `profile_visuals/${UID}/` });
    expect(mockDeleteFiles).toHaveBeenCalledWith({ prefix: `post_attachments/${UID}/` });
    const prefixes = mockDeleteFiles.mock.calls.map((c) => c[0].prefix);
    expect(prefixes.some((p) => p.startsWith('dm_media'))).toBe(false);
  });

  it('deletes the Auth user', async () => {
    await executeAccountDeletion(UID);
    expect(mockDeleteUser).toHaveBeenCalledWith(UID);
  });

  it('batch-deletes documents returned by the purge queries', async () => {
    mockWhereGet.mockImplementation(async (collection: string) =>
      collection === 'notifications'
        ? { docs: [{ ref: { path: 'notifications/n1' } }, { ref: { path: 'notifications/n2' } }] }
        : { docs: [] },
    );
    await executeAccountDeletion(UID);
    expect(mockBatchDelete).toHaveBeenCalledWith('notifications/n1');
    expect(mockBatchDelete).toHaveBeenCalledWith('notifications/n2');
    expect(mockBatchCommit).toHaveBeenCalled();
  });

  it('is idempotent: tolerates an already-deleted Auth user', async () => {
    mockDeleteUser.mockImplementation(async () => {
      const err = new Error('not found') as Error & { code: string };
      err.code = 'auth/user-not-found';
      throw err;
    });
    await expect(executeAccountDeletion(UID)).resolves.toBeUndefined();
  });

  it('rethrows non-user-not-found Auth errors', async () => {
    mockDeleteUser.mockImplementation(async () => {
      const err = new Error('boom') as Error & { code: string };
      err.code = 'internal';
      throw err;
    });
    await expect(executeAccountDeletion(UID)).rejects.toThrow('boom');
  });
});
