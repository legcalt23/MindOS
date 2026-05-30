/**
 * validators.ts
 * Strict input validation schemas for all Cloud Function callables.
 * Admin SDK bypasses Firestore Rules — every callable must validate here.
 */

import * as functions from 'firebase-functions';

/** Throws HttpsError('invalid-argument') with a descriptive message. */
function reject(msg: string): never {
  throw new functions.https.HttpsError('invalid-argument', msg);
}

const SAFE_ID = /^[A-Za-z0-9_-]{3,128}$/;
const HANDLE  = /^[a-z0-9._]{3,30}$/;
const CONTROL_CHARS = /[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/;
const STORAGE_PATH = /^[A-Za-z0-9_./-]{3,512}$/;

// Firestore is not SQL-backed from this code path, so classic SQL Injection is
// not the persistence risk here. We still reject control chars, unsafe IDs and
// path manipulation before Admin SDK writes because Rules are bypassed.

function assertString(v: unknown, field: string, max: number, allowNewlines = false): string {
  if (typeof v !== 'string' || v.length === 0 || v.length > max)
    reject(`"${field}" must be a non-empty string (max ${max} chars)`);
  const normalized = allowNewlines
    ? (v as string).replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim()
    : (v as string).replace(/\s+/g, ' ').trim();
  if (normalized.length === 0 || normalized.length > max)
    reject(`"${field}" must be a non-empty string (max ${max} chars)`);
  if (CONTROL_CHARS.test(normalized))
    reject(`"${field}" contains invalid characters`);
  return normalized;
}

function assertSafeId(v: unknown, field: string): string {
  const s = assertString(v, field, 128);
  if (!SAFE_ID.test(s)) reject(`"${field}" contains invalid characters`);
  return s;
}

function assertStoragePath(v: unknown, field: string): string {
  const s = assertString(v, field, 512);
  if (!STORAGE_PATH.test(s) || s.includes('..') || s.includes('//') || s.startsWith('/') || s.endsWith('/'))
    reject(`"${field}" contains invalid path characters`);
  return s;
}

function assertHttpUrl(v: unknown, field: string): string {
  const s = assertString(v, field, 2048);
  try {
    const url = new URL(s);
    if (url.protocol !== 'https:' && url.protocol !== 'http:') reject(`"${field}" must be http(s)`);
  } catch {
    reject(`"${field}" must be a valid URL`);
  }
  return s;
}

function assertSafeFileName(v: unknown, field: string): string {
  const s = assertString(v, field, 180);
  if (s.includes('/') || s.includes('\\') || s.includes('..'))
    reject(`"${field}" contains invalid path characters`);
  return s;
}

// ── Follows ────────────────────────────────────────────────────────────────

export interface FollowInput {
  targetUserId: string;
}

export function validateFollowInput(data: unknown): FollowInput {
  if (!data || typeof data !== 'object') reject('Payload must be an object');
  const d = data as Record<string, unknown>;
  return { targetUserId: assertSafeId(d['targetUserId'], 'targetUserId') };
}

// ── Follow Requests ────────────────────────────────────────────────────────

export interface FollowRequestInput {
  targetUserId: string;
}
export const validateFollowRequestInput = validateFollowInput as
  (d: unknown) => FollowRequestInput;

export interface RequestActionInput {
  requestId: string;
}
export function validateRequestActionInput(data: unknown): RequestActionInput {
  if (!data || typeof data !== 'object') reject('Payload must be an object');
  const d = data as Record<string, unknown>;
  return { requestId: assertSafeId(d['requestId'], 'requestId') };
}

// ── Blocks ─────────────────────────────────────────────────────────────────

export interface BlockInput {
  targetUserId: string;
}
export const validateBlockInput = validateFollowInput as (d: unknown) => BlockInput;

// ── Profile Visits ─────────────────────────────────────────────────────────

export interface ProfileVisitInput {
  profileOwnerId: string;
}
export function validateProfileVisitInput(data: unknown): ProfileVisitInput {
  if (!data || typeof data !== 'object') reject('Payload must be an object');
  const d = data as Record<string, unknown>;
  return { profileOwnerId: assertSafeId(d['profileOwnerId'], 'profileOwnerId') };
}

// ── Posts ──────────────────────────────────────────────────────────────────

export type PostType = 'TEXT' | 'PHOTO' | 'VIDEO' | 'AUDIO';
const POST_TYPES: PostType[] = ['TEXT', 'PHOTO', 'VIDEO', 'AUDIO'];

export interface PostAttachmentInput {
  id: string;
  type: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  storagePath: string;
  url: string;
  thumbnailUrl?: string | null;
  durationMs?: number | null;
  width?: number | null;
  height?: number | null;
}

export interface CreatePostInput {
  postId: string;
  postType: PostType;
  text?: string | null;
  description?: string | null;
  attachments: PostAttachmentInput[];
}

const ALLOWED_IMAGE_MIME = ['image/jpeg','image/png','image/webp','image/gif'];
const ALLOWED_VIDEO_MIME = ['video/mp4','video/webm'];
const ALLOWED_AUDIO_MIME = ['audio/mp4','audio/aac','audio/mpeg','audio/wav'];
const ALLOWED_FILE_MIME  = ['application/pdf','text/plain','application/json'];
const ALL_MIME = [...ALLOWED_IMAGE_MIME,...ALLOWED_VIDEO_MIME,...ALLOWED_AUDIO_MIME,...ALLOWED_FILE_MIME];

export function validateCreatePostInput(data: unknown): CreatePostInput {
  if (!data || typeof data !== 'object') reject('Payload must be an object');
  const d = data as Record<string, unknown>;

  const postId   = assertSafeId(d['postId'],   'postId');
  const postType = d['postType'];
  if (!POST_TYPES.includes(postType as PostType))
    reject(`"postType" must be one of ${POST_TYPES.join(', ')}`);

  const text        = d['text']        != null ? assertString(d['text'],        'text',        2000, true) : null;
  const description = d['description'] != null ? assertString(d['description'], 'description', 1000, true) : null;

  if (!Array.isArray(d['attachments'])) reject('"attachments" must be an array');
  const rawAtts = d['attachments'] as unknown[];
  if (rawAtts.length > 6) reject('Maximum 6 attachments');

  const attachments: PostAttachmentInput[] = rawAtts.map((a, i) => {
    if (!a || typeof a !== 'object') reject(`attachments[${i}] is not an object`);
    const att = a as Record<string, unknown>;

    const mimeType = assertString(att['mimeType'], `attachments[${i}].mimeType`, 128);
    if (!ALL_MIME.includes(mimeType)) reject(`attachments[${i}].mimeType not allowed`);

    const sizeBytes = att['sizeBytes'];
    if (typeof sizeBytes !== 'number' || sizeBytes <= 0)
      reject(`attachments[${i}].sizeBytes must be a positive number`);

    return {
      id:           assertSafeId(att['id'],           `attachments[${i}].id`),
      type:         assertString(att['type'],         `attachments[${i}].type`,         32),
      fileName:     assertSafeFileName(att['fileName'], `attachments[${i}].fileName`),
      mimeType,
      sizeBytes:    sizeBytes as number,
      storagePath:  assertStoragePath(att['storagePath'], `attachments[${i}].storagePath`),
      url:          assertHttpUrl(att['url'], `attachments[${i}].url`),
      thumbnailUrl: (att['thumbnailUrl']  != null ? assertHttpUrl(att['thumbnailUrl'], `attachments[${i}].thumbnailUrl`) : null),
      durationMs:   (typeof att['durationMs']  === 'number' ? att['durationMs']  : null),
      width:        (typeof att['width']        === 'number' ? att['width']        : null),
      height:       (typeof att['height']       === 'number' ? att['height']       : null),
    };
  });

  return {
    postId,
    postType: postType as PostType,
    text,
    description,
    attachments,
  };
}

// ── Username ───────────────────────────────────────────────────────────────

const RESERVED = ['admin','administrator','support','help','moderator','mod','neu','newera','official','system'];

export interface ClaimUsernameInput {
  username: string;
}
export function validateClaimUsernameInput(data: unknown): ClaimUsernameInput {
  if (!data || typeof data !== 'object') reject('Payload must be an object');
  const d = data as Record<string, unknown>;
  const username = assertString(d['username'], 'username', 30);
  if (!HANDLE.test(username))     reject('"username" must match ^[a-z0-9._]{3,30}$');
  if (username.startsWith('.'))   reject('"username" cannot start with a dot');
  if (username.endsWith('.'))     reject('"username" cannot end with a dot');
  if (username.includes('..'))    reject('"username" cannot contain consecutive dots');
  if (RESERVED.includes(username)) reject('"username" is reserved');
  return { username };
}
