# Firebase Storage Security Rules — Design Document

**Date:** 2026-05-26

---

## 1. Architecture

The `storage.rules` file (382 lines) enforces:

1. **App Check enforcement** — `isAppChecked()` required for all write operations
2. **Owner metadata validation** — `ownerUid` must match `auth.uid`
3. **Visibility metadata** — public/private tagging for read access control
4. **MIME type whitelisting** — extension-MIME pairing enforced
5. **Size limits per file type** — prevents storage abuse
6. **Catch-all deny** — `match /{path=**} { allow read, write: if false; }`

## 2. Path Security Matrix

| Path | Auth | AppCheck | MIME | Size | Metadata | Overwrite |
|---|---|---|---|---|---|---|
| `profile_visuals/{uid}/{mod}/{file}` | ✅ | ✅ | ✅ | ✅ | ownerUid, visibility | create-only |
| `post_attachments/{uid}/{post}/{id}` | ✅ | ✅ | ✅ | ✅ | ownerUid, postId, visibility | create-only |
| `dm_media/{chatId}/{file}` | ✅ | — | ✅ | ✅ | chatId, ownerUid, participantA/B, visibility=private | create-only (resource==null) |

## 3. MIME Type Allowlist

### Profile visuals
- `image/jpeg`, `image/png`, `image/webp` (photos)
- `video/mp4`, `video/quicktime` (videos)

### Post attachments
- `image/jpeg`, `image/png`, `image/webp`, `image/gif`
- `video/mp4`, `video/webm`
- `audio/mp4`, `audio/aac`, `audio/mpeg`, `audio/wav` (+ variants)
- `application/pdf`, `text/plain`, `application/json`

### DM media
- `image/jpeg`, `image/png`, `image/webp`
- `video/mp4`, `video/webm`
- `audio/mp4`, `audio/mpeg`, `audio/wav` (+ variants)

## 4. Blocked Types (Enforced by Allowlist)

The following are implicitly blocked by the allowlist approach:
- `text/html` — XSS vector
- `image/svg+xml` — script injection vector
- `application/javascript`, `text/javascript` — code execution
- `application/x-msdownload` — Windows executables
- `application/x-sh`, `application/x-bat` — shell scripts
- `application/java-archive` — Java archives
- `application/zip`, `application/x-zip-compressed` — archives
- `application/octet-stream` — generic binary (except WAV variants)
- APK, IPA, EXE, DLL, JAR — all blocked

## 5. Size Limits

| Type | Path | Max size |
|---|---|---|
| Thumbnail | profile_visuals | 1 MB |
| Photo (main module) | profile_visuals | 15 MB |
| Photo (small module) | profile_visuals | 10 MB |
| Video (main module) | profile_visuals | 15 MB |
| Video (small module) | profile_visuals | 75 MB |
| Image attachment | post_attachments | 10 MB |
| Video attachment | post_attachments | 75 MB |
| Audio attachment | post_attachments | 75 MB |
| PDF attachment | post_attachments | 32 MB |
| Text/JSON attachment | post_attachments | 2 MB |
| DM thumbnail | dm_media | 1 MB |
| DM image | dm_media | 10 MB |
| DM video | dm_media | 50 MB |
| DM audio | dm_media | 25 MB |

## 6. Server-Side Enforcement

Storage rules are complemented by Cloud Functions:

- **`enforceStorageQuotas`** (onFinalize trigger): Per-user quota (500 MB, 2000 files), rate limiting (20 files/min), auto-suspension on breach
- **`reduceStorageQuotasOnDelete`** (onDelete trigger): Decrements usage ledger
- **`onPostDeleted`** (Firestore trigger): Purges orphaned post attachments

## 7. Known Limitations

1. **No content scanning** — Storage rules validate declared MIME type, not actual file content. A file declaring `image/jpeg` but containing non-image data will be accepted.
2. **Download URL tokens** — Firebase Storage download URLs include long-lived access tokens that bypass rules. Private file URLs should not be shared.
3. **No quarantine flow** — Files go directly to final path. A quarantine → scan → promote flow is recommended for production if malware risk is significant.

## 8. Hardening Status

| Control | Status |
|---|---|
| Deny-by-default | ✅ |
| Catch-all deny | ✅ |
| App Check on writes | ✅ (profile_visuals, post_attachments) |
| MIME validation | ✅ |
| Size validation | ✅ |
| Owner metadata | ✅ |
| No overwrite (DM) | ✅ |
| No list operations | ✅ (all list: if false) |
| No HTML/JS/SVG | ✅ |
| No executables | ✅ |
