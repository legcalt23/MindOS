/**
 * storage.ts
 * Storage triggers: enforceStorageQuotas (onFinalize) + reduceStorageQuotasOnDelete (onDelete)
 * Moved from index.ts to keep the entry point clean.
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { logSafeError, logSafeRef, logSafeStoragePrefix } from './lib/privacyLogging';

const db = () => admin.firestore();

// Limits (keep in sync with SECURITY_REMEDIATION_ALTO_03_STORAGE_FLOOD.md)
const MAX_STORAGE_BYTES    = 500 * 1024 * 1024; // 500 MB
const MAX_FILES_COUNT      = 2000;
const RATE_LIMIT_WINDOW_MS = 60 * 1000;          // 1 minute
const MAX_FILES_PER_WINDOW = 20;

export const enforceStorageQuotas = functions.storage.object().onFinalize(async (object) => {
  const { bucket, name, size, metadata } = object;

  if (!metadata || !metadata['ownerUid']) {
    functions.logger.warn('Storage upload missing owner metadata. Deleting.', {
      fileRef: name ? logSafeStoragePrefix(name) : '<redacted:storage:empty>',
    });
    if (name && bucket) {
      await admin.storage().bucket(bucket).file(name).delete();
    }
    return;
  }

  const uid      = metadata['ownerUid'];
  const fileSize = parseInt(size || '0', 10);
  const usageRef = db().collection('storage_usage').doc(uid);
  const userRef  = db().collection('users').doc(uid);

  try {
    await db().runTransaction(async (transaction) => {
      const usageDoc = await transaction.get(usageRef);
      const now      = Date.now();

      let currentBytes    = 0;
      let currentFiles    = 0;
      let windowStart     = now;
      let filesInWindow   = 0;
      let isSuspended     = false;

      if (usageDoc.exists) {
        const data      = usageDoc.data()!;
        currentBytes    = data['totalBytes']    || 0;
        currentFiles    = data['totalFiles']    || 0;
        isSuspended     = data['isSuspended']   || false;
        windowStart     = data['windowStart']   || now;
        filesInWindow   = data['filesInWindow'] || 0;

        if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
          windowStart   = now;
          filesInWindow = 0;
        }
      }

      if (isSuspended) throw new Error('USER_SUSPENDED');

      const newBytes        = currentBytes    + fileSize;
      const newFiles        = currentFiles    + 1;
      const newFilesInWindow = filesInWindow  + 1;

      if (newBytes > MAX_STORAGE_BYTES || newFiles > MAX_FILES_COUNT || newFilesInWindow > MAX_FILES_PER_WINDOW) {
        transaction.set(usageRef, {
          totalBytes:    currentBytes,
          totalFiles:    currentFiles,
          windowStart,
          filesInWindow,
          isSuspended:   true,
          suspendedAt:   admin.firestore.FieldValue.serverTimestamp(),
          reason: newBytes > MAX_STORAGE_BYTES  ? 'STORAGE_QUOTA_EXCEEDED'
                : newFiles > MAX_FILES_COUNT     ? 'FILES_QUOTA_EXCEEDED'
                : 'RATE_LIMIT_EXCEEDED',
        }, { merge: true });

        transaction.set(userRef, { isStorageSuspended: true }, { merge: true });
        throw new Error('LIMIT_EXCEEDED');
      }

      transaction.set(usageRef, {
        totalBytes:    newBytes,
        totalFiles:    newFiles,
        windowStart,
        filesInWindow: newFilesInWindow,
        lastUploadAt:  admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    });

  } catch (error: unknown) {
    const msg = (error as Error).message;
    if (msg === 'LIMIT_EXCEEDED' || msg === 'USER_SUSPENDED') {
      functions.logger.warn('Storage quota exceeded or user suspended. Deleting file.', {
        uidRef: logSafeRef(uid, 'uid'),
        fileRef: name ? logSafeStoragePrefix(name) : '<redacted:storage:empty>',
      });
      if (name && bucket) {
        await admin.storage().bucket(bucket).file(name).delete().catch((e) => {
          functions.logger.error('Failed to delete quota-violating file.', {
            uidRef: logSafeRef(uid, 'uid'),
            fileRef: logSafeStoragePrefix(name),
            error: logSafeError(e),
          });
        });
      }
    } else {
      functions.logger.error('Error in enforceStorageQuotas transaction', {
        uidRef: logSafeRef(uid, 'uid'),
        error: logSafeError(error),
      });
      throw error;
    }
  }
});

export const reduceStorageQuotasOnDelete = functions.storage.object().onDelete(async (object) => {
  const { size, metadata } = object;
  if (!metadata || !metadata['ownerUid']) return;

  const uid      = metadata['ownerUid'];
  const fileSize = parseInt(size || '0', 10);
  const usageRef = db().collection('storage_usage').doc(uid);

  try {
    await db().runTransaction(async (transaction) => {
      const usageDoc = await transaction.get(usageRef);
      if (usageDoc.exists) {
        const data         = usageDoc.data()!;
        const currentBytes = data['totalBytes'] || 0;
        const currentFiles = data['totalFiles'] || 0;

        transaction.set(usageRef, {
          totalBytes: Math.max(0, currentBytes - fileSize),
          totalFiles: Math.max(0, currentFiles - 1),
        }, { merge: true });
      }
    });
  } catch (error) {
    functions.logger.error('Error reducing storage quotas', {
      uidRef: logSafeRef(uid, 'uid'),
      error: logSafeError(error),
    });
  }
});
