# Data Inventory

**Date:** 2026-05-26
**Project:** MindOS

---

## 1. Firestore Collections

### 1.1 `users/{userId}`

| Property | Value |
|---|---|
| **Path** | `/users/{userId}` |
| **Purpose** | Core user account record (status, handle) |
| **Sensitivity** | HIGH — account status, identity |
| **Doc ID** | Firebase Auth UID |
| **Who creates** | Backend (Cloud Functions / Admin SDK) |
| **Who reads (get)** | Owner only |
| **Who reads (list)** | Nobody |
| **Who updates** | Backend only |
| **Who deletes** | Nobody (soft-delete via status) |
| **Queries allowed** | Owner get only |
| **Queries prohibited** | All list queries |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `id` | string (safeId) | yes | no | no |
| `handle` | string (handle) | yes | yes | no (server-only) |
| `status` | string enum (Active/Suspended/Deleted) | yes | yes | no (server-only) |
| `createdAt` | temporal | yes | no | no |
| `updatedAt` | temporal | yes | yes | no (server-only) |

**Rule:** Owner get with schema validation. All writes denied.

---

### 1.2 `profiles/{userId}`

| Property | Value |
|---|---|
| **Path** | `/profiles/{userId}` |
| **Purpose** | User profile (display name, bio, avatar, verification) |
| **Sensitivity** | MEDIUM — contains PII (name, bio) |
| **Doc ID** | Firebase Auth UID |
| **Who creates** | Backend only |
| **Who reads (get)** | Privacy-gated (owner, followers if private, all if public) |
| **Who reads (list)** | Bounded (12) for DM profile discovery |
| **Who updates** | Backend only |
| **Who deletes** | Nobody |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `id` | string (safeId) | yes | no | no |
| `userId` | string (safeId) | yes | no | no |
| `handle` | string (handle) | yes | yes | no |
| `displayName` | string (1-80) | yes | yes | no |
| `bio` | string? (0-300) | no | yes | no |
| `avatarUrl` | https URL? (0-2048) | no | yes | no |
| `isVerified` | boolean | no | yes | no (server-only) |
| `createdAt` | temporal | yes | no | no |
| `updatedAt` | temporal | yes | yes | no |
| `usernameHistory` | list (0-10 entries) | no | yes | no |

**Rule:** Privacy-gated get. Bounded list for DM discovery. All writes denied.

---

### 1.3 `privacy/{userId}`

| Property | Value |
|---|---|
| **Path** | `/privacy/{userId}` |
| **Purpose** | Per-user privacy settings |
| **Sensitivity** | MEDIUM — privacy preferences |
| **Doc ID** | Firebase Auth UID |
| **Who creates** | Owner |
| **Who reads (get)** | Owner only |
| **Who reads (list)** | Nobody |
| **Who updates** | Owner |
| **Who deletes** | Nobody |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `userId` | string | yes | no | yes (create only, must match auth.uid) |
| `profileVisibility` | enum (Public/Private) | yes | yes | yes |
| `followersVisibility` | enum (audience) | yes | yes | yes |
| `followingVisibility` | enum (audience) | yes | yes | yes |
| `messagePermission` | enum (audience) | yes | yes | yes |
| `allowMentionsFrom` | enum (audience) | yes | yes | yes |
| `discoverable` | boolean | yes | yes | yes |
| `showActivityStatus` | boolean | yes | yes | yes |
| `updatedAt` | temporal | yes | yes | yes (must be >= previous) |

**Rule:** Owner create/update with full schema validation. `userId` immutable after create.

---

### 1.4 `usernames/{username}`

| Property | Value |
|---|---|
| **Path** | `/usernames/{username}` |
| **Purpose** | Username reservation / uniqueness enforcement |
| **Sensitivity** | LOW — public handle mapping |
| **Doc ID** | Normalized username handle |
| **Who creates** | Authenticated user (owner) |
| **Who reads (get)** | Any authenticated user |
| **Who reads (list)** | Nobody |
| **Who updates** | Owner of claim |
| **Who deletes** | Owner of claim (only if not actively used by profile_visual) |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `uid` | string (safeId) | yes | no | yes (must match auth.uid) |
| `username` | string (handle) | yes | no | yes (must match doc ID) |
| `createdAt` | temporal | yes | no | yes (create only) |
| `updatedAt` | temporal | yes | yes | yes (must be >= previous) |

**Rule:** Validated handle format, reserved word check, atomic with profile_visual.

---

### 1.5 `profile_visuals/{userId}`

| Property | Value |
|---|---|
| **Path** | `/profile_visuals/{userId}` |
| **Purpose** | Rich profile configuration (layout, media, modules) |
| **Sensitivity** | MEDIUM — profile content, media references |
| **Doc ID** | Firebase Auth UID |
| **Who creates** | Owner |
| **Who reads (get)** | Privacy-gated (owner, followers, public if searchable) |
| **Who reads (list)** | Bounded search (12) if searchable + directoryVisible |
| **Who updates** | Owner (profile fields) + other users (observersCount atomic increment) |
| **Who deletes** | Nobody |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `schemaVersion` | number (==1) | yes | no | yes (create), immutable on update |
| `userId` | string (safeId) | yes | no | yes (must match auth.uid), immutable |
| `displayName` | string (1-80) | yes | yes | yes |
| `username` | string (handle) | yes | yes | yes (must match valid username claim) |
| `userphrase` | string? (0-180) | no | yes | yes |
| `mainProfilePhotoUrl` | string? (0-512) | no | yes | yes |
| `searchable` | boolean | yes | yes | yes |
| `directoryVisible` | boolean | yes | yes | yes (must match privacy.profileVisibility) |
| `usernameSearchTokens` | list | yes | no | **no** (must be empty on client write) |
| `observersCount` | number (>=0) | yes | yes | **partial** (only +1 with atomic observation) |
| `identityPosition` | enum (Top/Bottom) | yes | yes | yes |
| `primaryColor` | number (ARGB) | yes | yes | yes |
| `secondaryColor` | number (ARGB) | yes | yes | yes |
| `categoryIconsConfig` | list (0-3 enums) | yes | yes | yes |
| `layoutVersion` | number (==1) | yes | no | yes (create), immutable |
| `layoutPreset` | enum (LargeLeft/LargeRight) | yes | yes | yes |
| `modules` | list (3 validated modules) | yes | yes | yes |
| `createdAt` | temporal | yes | no | yes (create), immutable |
| `updatedAt` | temporal | yes | yes | yes (must be >= previous) |

**Immutable on update:** `schemaVersion`, `userId`, `usernameSearchTokens`, `observersCount` (except atomic +1), `layoutVersion`, `createdAt`.

---

### 1.6 `profile_visits/{visitId}`

| Property | Value |
|---|---|
| **Path** | `/profile_visits/{visitId}` |
| **Purpose** | Track profile view events (deduplicated) |
| **Sensitivity** | LOW — behavioral data |
| **Doc ID** | `{viewerUserId}_{profileOwnerId}` |
| **Who creates** | Viewer |
| **Who reads (get)** | Viewer only |
| **Who updates** | Viewer (lastVisitedAt only) |
| **Who deletes** | Nobody |

| Field | Type | Required | Mutable | Client-writable |
|---|---|---|---|---|
| `viewerUserId` | string (safeId) | yes | no | yes (must match auth.uid) |
| `profileOwnerId` | string (safeId) | yes | no | yes (immutable) |
| `hasVisitedProfile` | boolean (==true) | yes | no | yes |
| `firstVisitedAt` | temporal | yes | no | yes (create), immutable |
| `lastVisitedAt` | temporal | yes | yes | yes (must be >= previous) |

---

### 1.7 `follows/{relationId}`

| Property | Value |
|---|---|
| **Path** | `/follows/{relationId}` |
| **Purpose** | Follow relationships |
| **Sensitivity** | LOW — social graph edges |
| **Doc ID** | `{followerId}_{followeeId}` |
| **Who creates** | Backend (Cloud Functions) |
| **Who reads (get)** | Follower or followee |
| **Who writes** | Backend only — all client writes denied |

---

### 1.8 `observations/{relationId}`

| Property | Value |
|---|---|
| **Path** | `/observations/{relationId}` |
| **Purpose** | Observer (one-click follow) relationships |
| **Doc ID** | `{observerUserId}_{observedUserId}` |
| **Who creates** | Observer (client, atomic with counter + notification) |
| **Who reads (get)** | Any authenticated user |
| **Who updates/deletes** | Nobody |

---

### 1.9 `notifications/{notificationId}`

| Property | Value |
|---|---|
| **Path** | `/notifications/{notificationId}` |
| **Purpose** | In-app notification delivery |
| **Sensitivity** | LOW — notification metadata |
| **Doc ID** | `{actorUserId}_{recipientUserId}` |
| **Who creates** | Actor (client, atomic with observation) |
| **Who reads (get)** | Actor or recipient |
| **Who reads (list)** | Recipient only, bounded (100) |
| **Who updates** | Recipient (isRead field only) |
| **Who deletes** | Nobody |

---

### 1.10 `followRequests/{requestId}`

Server-only writes. Participant get only. No list.

### 1.11 `blocks/{relationId}`

Server-only writes. Blocker get only. No list.

### 1.12 `counters/{userId}`

Server-only writes. Privacy-gated get. No list.

### 1.13 `posts/{postId}`

| Property | Value |
|---|---|
| **Path** | `/posts/{postId}` |
| **Purpose** | Global post index |
| **Who creates** | Owner (client-side validated + CF createPost) |
| **Who reads (get)** | Privacy-gated |
| **Who reads (list)** | Bounded (100), own posts only |
| **Who updates** | Nobody |
| **Who deletes** | Owner |

### 1.14 `user_posts/{userId}/posts/{postId}`

| Property | Value |
|---|---|
| **Path** | `/user_posts/{userId}/posts/{postId}` |
| **Purpose** | Per-user post collection |
| **Who reads (list)** | Privacy-gated, bounded (100) |

### 1.15 `feeds/{viewerId}/posts/{postId}`

Server-only writes. Owner get/list (bounded 100).

### 1.16 `dm_chats/{chatId}`

Participant-only access. List bounded (50). Create/update validated.

### 1.17 `dm_chats/{chatId}/messages/{messageId}`

Participant-only access. List bounded (100). Create/update validated.

### 1.18-1.29 Server-only collections

All wallet, payment, fraud, audit, and admin collections deny all client reads and writes:
`wallets`, `balance_snapshots`, `ledger_transactions`, `ledger_entries`, `idempotency_keys`, `purchases`, `purchase_token_index`, `payment_provider_events`, `fraud_signals`, `wallet_audit_logs`, `admin_actions`, `auditEvents`.

Exception: `wallets/{walletId}` and `balance_snapshots/{snapshotId}` allow owner `get`.

---

## 2. Firebase Storage Paths

### 2.1 `profile_visuals/{userId}/{moduleId}/{fileName}`

| Property | Value |
|---|---|
| **Purpose** | Profile media (photos, videos, thumbnails) |
| **Allowed content types** | image/jpeg, image/png, image/webp, video/mp4, video/quicktime |
| **Max size (image)** | 15 MB (main), 10 MB (small modules) |
| **Max size (video)** | 15 MB (main), 75 MB (small modules) |
| **Max size (thumbnail)** | 1 MB |
| **Who uploads** | Owner (App Check required) |
| **Who reads** | Public/private metadata-gated |
| **Who deletes** | Owner |
| **Required metadata** | `ownerUid`, `visibility` |

### 2.2 `post_attachments/{userId}/{postId}/{attachmentId}`

| Property | Value |
|---|---|
| **Purpose** | Post media attachments |
| **Allowed content types** | image/*, video/mp4, video/webm, audio/*, application/pdf, text/plain, application/json |
| **Max size** | 10 MB (images), 75 MB (video/audio), 32 MB (PDF), 2 MB (text/json) |
| **Who uploads** | Owner (App Check required) |
| **Who reads** | Visibility-gated via metadata |
| **Who deletes** | Owner |
| **Required metadata** | `ownerUid`, `postId`, `visibility` |

### 2.3 `dm_media/{chatId}/{fileName}`

| Property | Value |
|---|---|
| **Purpose** | DM media attachments |
| **Allowed content types** | image/jpeg, image/png, image/webp, video/mp4, video/webm, audio/mp4, audio/mpeg, audio/wav |
| **Max size** | 10 MB (images), 50 MB (video), 25 MB (audio), 1 MB (thumbnails) |
| **Who uploads** | Participant (create-only, no overwrite) |
| **Who reads** | Participant (via metadata) |
| **Who deletes** | Nobody |
| **Required metadata** | `visibility=private`, `chatId`, `ownerUid`, `participantA`, `participantB` |

---

## 3. Indices

| Collection | Fields | Query scope |
|---|---|---|
| `profile_visuals` | directoryVisible ASC, searchable ASC, username ASC | COLLECTION |
| `dm_chats` | participants CONTAINS, updatedAt DESC | COLLECTION |
