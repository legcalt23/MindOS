import * as admin from 'firebase-admin';
import * as functionsTest from 'firebase-functions-test';

const testEnv = functionsTest();

const mockTransactionGet = jest.fn();
const mockTransactionSet = jest.fn();
const mockFileDelete = jest.fn();

const mockRunTransaction = jest.fn((callback) => {
    const t = {
        get: mockTransactionGet,
        set: mockTransactionSet
    };
    return callback(t);
});

// Mock the admin SDK before importing the function
jest.mock('firebase-admin', () => {
    return {
        initializeApp: jest.fn(),
        firestore: Object.assign(jest.fn(() => ({
            collection: jest.fn(() => ({
                doc: jest.fn(() => ({
                    get: jest.fn(),
                    set: jest.fn(),
                }))
            })),
            runTransaction: mockRunTransaction
        })), {
            FieldValue: {
                serverTimestamp: jest.fn(() => 'MOCK_TIMESTAMP')
            }
        }),
        storage: jest.fn(() => ({
            bucket: jest.fn(() => ({
                file: jest.fn(() => ({
                    delete: mockFileDelete
                }))
            }))
        })),
    };
});

import { enforceStorageQuotas } from './index';

describe('enforceStorageQuotas', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        
        mockTransactionGet.mockImplementation(() => Promise.resolve({
            exists: false,
            data: () => undefined
        }));
        mockTransactionSet.mockImplementation(() => {});
        mockFileDelete.mockResolvedValue(undefined);
    });

    it('should delete file if no ownerUid metadata is present', async () => {
        const wrapped = testEnv.wrap(enforceStorageQuotas);
        
        const objectMetadata = {
            bucket: 'test-bucket',
            name: 'test-file.jpg',
            size: '1024',
            metadata: {} // missing ownerUid
        };

        await wrapped(objectMetadata as any);

        expect(mockFileDelete).toHaveBeenCalled();
        expect(admin.firestore().runTransaction).not.toHaveBeenCalled();
    });

    it('should update ledger on normal upload', async () => {
        const wrapped = testEnv.wrap(enforceStorageQuotas);
        
        const objectMetadata = {
            bucket: 'test-bucket',
            name: 'test-file.jpg',
            size: '1024',
            metadata: {
                ownerUid: 'user123'
            }
        };

        await wrapped(objectMetadata as any);

        expect(mockFileDelete).not.toHaveBeenCalled();
        expect(mockTransactionSet).toHaveBeenCalledWith(
            expect.anything(), // usageRef
            expect.objectContaining({
                totalBytes: 1024,
                totalFiles: 1,
                filesInWindow: 1
            }),
            { merge: true }
        );
    });

    it('should suspend user and delete file if storage quota exceeded', async () => {
        const wrapped = testEnv.wrap(enforceStorageQuotas);
        
        mockTransactionGet.mockImplementationOnce(() => Promise.resolve({
            exists: true,
            data: () => ({
                totalBytes: 500 * 1024 * 1024, // already at max
                totalFiles: 10,
                filesInWindow: 1,
                windowStart: Date.now() - 1000
            })
        }));

        const objectMetadata = {
            bucket: 'test-bucket',
            name: 'test-file-huge.jpg',
            size: '1024', // pushing over the limit
            metadata: {
                ownerUid: 'user123'
            }
        };

        await wrapped(objectMetadata as any);

        // Expect file to be deleted
        expect(mockFileDelete).toHaveBeenCalled();

        // Expect usageRef and userRef to be updated for suspension
        expect(mockTransactionSet).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                isSuspended: true,
                reason: 'STORAGE_QUOTA_EXCEEDED'
            }),
            { merge: true }
        );
    });

    it('should suspend user and delete file if rate limit exceeded', async () => {
        const wrapped = testEnv.wrap(enforceStorageQuotas);
        
        mockTransactionGet.mockImplementationOnce(() => Promise.resolve({
            exists: true,
            data: () => ({
                totalBytes: 1024,
                totalFiles: 10,
                filesInWindow: 20, // already at max files in window
                windowStart: Date.now() - 1000 // within the last minute
            })
        }));

        const objectMetadata = {
            bucket: 'test-bucket',
            name: 'test-file.jpg',
            size: '1024',
            metadata: {
                ownerUid: 'user123'
            }
        };

        await wrapped(objectMetadata as any);

        expect(mockFileDelete).toHaveBeenCalled();
        expect(mockTransactionSet).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                isSuspended: true,
                reason: 'RATE_LIMIT_EXCEEDED'
            }),
            { merge: true }
        );
    });
});
