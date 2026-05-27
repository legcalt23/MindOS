# Firestore Security Rules — Design Document

**Date:** 2026-05-26

---

## 1. Architecture

The `firestore.rules` file (1720+ lines) follows these structural conventions:

1. **Helper functions first** — reusable validators at the top
2. **Collection-specific match blocks** — one per collection
3. **Catch-all deny** — `match /{document=**} { allow read, write: if false; }` at bottom

## 2. Helper Function Categories

| Category | Functions | Purpose |
|---|---|---|
| Auth | `signedIn()`, `uid()`, `isOwner()` | Authentication checks |
| Identity | `isSafeId()`, `validHandle()`, `reservedUsername()` | ID/handle validation |
| Strings | `nonEmptyString()`, `nullableString()`, `httpsUrlOrNull()` | Text validation |
| Temporal | `millis()`, `ts()`, `temporal()`, `temporalOrder()`, `temporalNotBefore()` | Timestamp validation |
| Privacy | `privacyAllowsProfileRead()`, `canReadProfile()`, `canReadProfileVisual()` | Privacy-gated access |
| Social | `noBlockBetween()`, `followPath()`, `blockPath()` | Social graph queries |
| DM | `isDMParticipant()`, `validDMChatCandidateForUser()` | DM access control |
| Schema | `visualKeys()`, `validModule()`, `validPostAttachment()` | Document schema |

## 3. Hardening Applied in This Audit

### 3.1 Unbounded list queries fixed

| Collection | Before | After | Risk mitigated |
|---|---|---|---|
| `user_posts/{uid}/posts` | No limit | `limit <= 100` | Cost amplification |
| `feeds/{viewerId}/posts` | No limit | `limit <= 100` | Cost amplification |
| `dm_chats` | No limit | `limit <= 50` | Cost amplification |
| `dm_chats/.../messages` | No limit | `limit <= 100` | Cost amplification |

### 3.2 Existing protections verified

- All creates validate `keys().hasOnly()` and `keys().hasAll()` ✅
- All updates validate `diff().affectedKeys().hasOnly()` ✅
- Immutable fields checked on update ✅
- Server-only collections deny all ✅
- Catch-all deny present ✅
- No `allow read, write: if true` anywhere ✅
- No broad `allow read: if request.auth != null` ✅

## 4. Patterns Enforced

### Create pattern
```
allow create: if
  signedIn() &&
  isOwner(userId) &&
  data.keys().hasOnly(allowedKeys) &&
  data.keys().hasAll(requiredKeys) &&
  // type validation for each field
  // server-only fields absent or constrained
  // createdAt/updatedAt valid
```

### Update pattern
```
allow update: if
  signedIn() &&
  isOwner(userId) &&
  resource.data.userId == userId &&
  // full schema validation on new data
  diff().affectedKeys().hasOnly(mutableKeys) &&
  // immutable fields unchanged
  // updatedAt >= previous
```

### Read pattern
```
allow get: if ownership_or_privacy_check;
allow list: if limit_enforced && access_check;
```

## 5. Missing Functions (Not Needed)

The following functions from the generic template are **not applicable** to this project:

- `isAdmin()` — No admin role in Firestore rules (admin ops are CF-only)
- `isModerator()` — No moderator role
- `isOrgMember()`, `isOrgAdmin()`, `isOrgOwner()`, `hasOrgRole()` — No organization model
- `hasVerifiedEmail()` — Email verification not enforced at rules level
- `notSuspended()` — Suspension is enforced in Cloud Functions, not rules
- `validSchemaVersion()` — Validated inline per collection
- `noSensitiveClientFields()` — No sensitive fields exist in client-writable schemas

These omissions are **by design**, not oversights. The project's security model does not require these controls at the rules layer.
