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

function assertSafeFileName(v: unknown, field: string): string {
  const s = assertString(v, field, 180);
  if (s.includes('/') || s.includes('\\') || s.includes('..'))
    reject(`"${field}" contains invalid path characters`);
  return s;
}

// ── Numeric bounds ───────────────────────────────────────────────────────────
// Number.isInteger() is false for NaN, Infinity and any non-integer float, so a
// single guard rejects NaN/Infinity/negative/decimal in one place. Persisting
// any of those to Firestore corrupts the document and bypasses the numeric
// contract the security rules assume.
const MAX_ATTACHMENT_BYTES_ABSOLUTE = 78_643_200; // largest per-MIME cap; per-MIME check tightens
const MAX_DURATION_MS = 86_400_000;               // 24h — generous upper bound for any clip
const MAX_PIXEL_DIMENSION = 100_000;              // px — generous upper bound for width/height

/** Asserts a finite, non-negative safe integer within [0, max]. */
function assertNonNegativeInt(v: unknown, field: string, max: number): number {
  if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > max)
    reject(`"${field}" must be an integer between 0 and ${max}`);
  return v as number;
}

/** Asserts a finite, strictly-positive safe integer within [1, max]. */
function assertPositiveInt(v: unknown, field: string, max: number): number {
  const n = assertNonNegativeInt(v, field, max);
  if (n === 0) reject(`"${field}" must be greater than 0`);
  return n;
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

const MAX_ATTACHMENT_BYTES: Record<string, number> = {
  'image/jpeg': 10 * 1024 * 1024,
  'image/png': 10 * 1024 * 1024,
  'image/webp': 10 * 1024 * 1024,
  'image/gif': 10 * 1024 * 1024,
  'video/mp4': 78_643_200,
  'video/webm': 78_643_200,
  'audio/mp4': 78_643_200,
  'audio/aac': 78_643_200,
  'audio/mpeg': 78_643_200,
  'audio/wav': 78_643_200,
  'application/pdf': 33_554_432,
  'text/plain': 2_097_152,
  'application/json': 2_097_152,
};

const MIME_EXTENSION: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
  'video/mp4': 'mp4',
  'video/webm': 'webm',
  'audio/mp4': 'm4a',
  'audio/aac': 'aac',
  'audio/mpeg': 'mp3',
  'audio/wav': 'wav',
  'application/pdf': 'pdf',
  'text/plain': 'txt',
  'application/json': 'json',
};

function expectedAttachmentPath(ownerUid: string, postId: string, attachmentId: string, mimeType: string): string {
  const extension = MIME_EXTENSION[mimeType];
  if (!extension) reject(`attachments[].mimeType not allowed`);
  return `post_attachments/${ownerUid}/${postId}/${attachmentId}.${extension}`;
}

function expectedThumbnailPath(ownerUid: string, postId: string, attachmentId: string): string {
  return `post_attachments/${ownerUid}/${postId}/${attachmentId}_thumb.jpg`;
}

function validateAttachmentSemantics(
  attachment: PostAttachmentInput,
  ownerUid: string | null,
  postId: string,
  index: number,
): void {
  const prefix = `attachments[${index}]`;
  const type = attachment.type;
  const mimeType = attachment.mimeType;

  const validTypeMime =
    (type === 'Image' && ALLOWED_IMAGE_MIME.includes(mimeType)) ||
    (type === 'Video' && ALLOWED_VIDEO_MIME.includes(mimeType)) ||
    (type === 'Audio' && ALLOWED_AUDIO_MIME.includes(mimeType)) ||
    (type === 'File' && ALLOWED_FILE_MIME.includes(mimeType));
  if (!validTypeMime) reject(`${prefix}.type does not match mimeType`);

  const maxBytes = MAX_ATTACHMENT_BYTES[mimeType];
  if (!maxBytes || attachment.sizeBytes > maxBytes) {
    reject(`${prefix}.sizeBytes exceeds the allowed limit for ${mimeType}`);
  }

  if (ownerUid) {
    const expectedPath = expectedAttachmentPath(ownerUid, postId, attachment.id, mimeType);
    if (attachment.storagePath !== expectedPath) {
      reject(`${prefix}.storagePath must be ${expectedPath}`);
    }
    if (attachment.url !== attachment.storagePath) {
      reject(`${prefix}.url must equal storagePath; persistent download URLs are not allowed`);
    }
    if (attachment.thumbnailUrl != null && attachment.thumbnailUrl !== expectedThumbnailPath(ownerUid, postId, attachment.id)) {
      reject(`${prefix}.thumbnailUrl must be the expected Storage thumbnail path`);
    }
  } else if (attachment.url.startsWith('http://')) {
    reject(`${prefix}.url must not be http`);
  }

  if (type === 'Audio' && (attachment.durationMs == null || attachment.durationMs <= 0)) {
    reject(`${prefix}.durationMs is required for audio`);
  }
  if (type === 'Video' && attachment.durationMs != null && attachment.durationMs <= 0) {
    reject(`${prefix}.durationMs must be positive when provided`);
  }
  if ((type === 'Image' || type === 'File') && attachment.durationMs != null) {
    reject(`${prefix}.durationMs must be null for image/file attachments`);
  }
}

function validatePostPayloadForType(postType: PostType, text: string | null, attachments: PostAttachmentInput[]): void {
  if (postType === 'TEXT') {
    if (!text) reject('TEXT posts require text');
    if (attachments.length !== 0) reject('TEXT posts cannot contain attachments');
    return;
  }
  if (text) reject('Only TEXT posts may contain text');

  if (postType === 'PHOTO') {
    if (attachments.length !== 1 || attachments[0].type !== 'Image') reject('PHOTO posts require exactly one image');
    return;
  }
  if (postType === 'VIDEO') {
    if (attachments.length !== 1 || attachments[0].type !== 'Video') reject('VIDEO posts require exactly one video');
    return;
  }
  if (postType === 'AUDIO') {
    if (attachments.length < 1 || attachments.length > 5) reject('AUDIO posts require 1 to 5 audio clips');
    if (attachments.some((attachment) => attachment.type !== 'Audio')) reject('AUDIO posts only allow audio clips');
  }
}

export function validateCreatePostInput(data: unknown, ownerUid: string | null = null): CreatePostInput {
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

    const sizeBytes = assertPositiveInt(att['sizeBytes'], `attachments[${i}].sizeBytes`, MAX_ATTACHMENT_BYTES_ABSOLUTE);

    const parsed = {
      id:           assertSafeId(att['id'],           `attachments[${i}].id`),
      type:         assertString(att['type'],         `attachments[${i}].type`,         32),
      fileName:     assertSafeFileName(att['fileName'], `attachments[${i}].fileName`),
      mimeType,
      sizeBytes,
      storagePath:  assertStoragePath(att['storagePath'], `attachments[${i}].storagePath`),
      url:          assertStoragePath(att['url'], `attachments[${i}].url`),
      thumbnailUrl: (att['thumbnailUrl']  != null ? assertStoragePath(att['thumbnailUrl'], `attachments[${i}].thumbnailUrl`) : null),
      durationMs:   (att['durationMs'] != null ? assertNonNegativeInt(att['durationMs'], `attachments[${i}].durationMs`, MAX_DURATION_MS) : null),
      width:        (att['width']      != null ? assertNonNegativeInt(att['width'],      `attachments[${i}].width`,      MAX_PIXEL_DIMENSION) : null),
      height:       (att['height']     != null ? assertNonNegativeInt(att['height'],     `attachments[${i}].height`,     MAX_PIXEL_DIMENSION) : null),
    };
    validateAttachmentSemantics(parsed, ownerUid, postId, i);
    return parsed;
  });

  validatePostPayloadForType(postType as PostType, text, attachments);

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
