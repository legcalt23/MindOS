# Query Security Review

**Date:** 2026-05-26

---

## 1. Firestore Queries Audit

### Q1 — Username search (`profile_visuals`)
| Property | Value |
|---|---|
| **File** | `FirebaseUsernameSearchRepository.kt` |
| **Path** | `profile_visuals` |
| **Filters** | `searchable == true`, `directoryVisible == true`, `username >= query`, `username <= query + ""` |
| **Limit** | 12 (enforced by rules) |
| **Index** | `directoryVisible ASC, searchable ASC, username ASC` |
| **Rule** | `boundedUsernameSearch()` — signedIn, limit <= 12 |
| **Risk** | LOW — only exposes opt-in searchable profiles |

### Q2 — DM chat list (`dm_chats`)
| Property | Value |
|---|---|
| **File** | `FirebaseDMRepository.kt` |
| **Path** | `dm_chats` |
| **Filters** | `participants` array-contains `uid` |
| **OrderBy** | `updatedAt DESC` |
| **Limit** | 50 (enforced by rules after fix) |
| **Index** | `participants CONTAINS, updatedAt DESC` |
| **Rule** | signedIn, limit <= 50, uid in resource.data.participants |
| **Risk** | LOW — participant-only access |

### Q3 — DM messages (`dm_chats/{chatId}/messages`)
| Property | Value |
|---|---|
| **File** | `FirebaseDMRepository.kt` |
| **Path** | `dm_chats/{chatId}/messages` |
| **OrderBy** | `createdAt ASC` |
| **Limit** | 100 (enforced by rules after fix) |
| **Rule** | isDMParticipant, limit <= 100 |
| **Risk** | LOW — participant-only access |

### Q4 — DM profile discovery (`profiles`)
| Property | Value |
|---|---|
| **File** | `FirebaseDMRepository.kt` |
| **Path** | `profiles` |
| **Filters** | Discovery query (searchable, directoryVisible) |
| **Limit** | 12 (enforced by rules) |
| **Rule** | `boundedDMProfileDiscovery()` |
| **Risk** | LOW — only opt-in profiles |

### Q5 — User's own posts (`posts`)
| Property | Value |
|---|---|
| **File** | Publishing/feed repositories |
| **Path** | `posts` |
| **Filters** | `userId == uid()` |
| **Limit** | 100 (enforced by rules) |
| **Rule** | signedIn, limit <= 100, resource.data.userId == uid() |
| **Risk** | LOW — own data only |

### Q6 — User posts by owner (`user_posts/{userId}/posts`)
| Property | Value |
|---|---|
| **File** | Feed/profile repositories |
| **Path** | `user_posts/{userId}/posts` |
| **Filters** | Privacy-gated |
| **Limit** | 100 (enforced by rules after fix) |
| **Rule** | limit <= 100, privacy + block check |
| **Risk** | LOW — privacy-gated |

### Q7 — Feed entries (`feeds/{viewerId}/posts`)
| Property | Value |
|---|---|
| **File** | Feed repositories |
| **Path** | `feeds/{viewerId}/posts` |
| **Limit** | 100 (enforced by rules after fix) |
| **Rule** | isOwner(viewerId), limit <= 100 |
| **Risk** | LOW — owner-only |

### Q8 — Notifications list (`notifications`)
| Property | Value |
|---|---|
| **File** | Social repositories |
| **Path** | `notifications` |
| **Filters** | `recipientUserId == uid()` |
| **Limit** | 100 (enforced by rules) |
| **Rule** | signedIn, limit <= 100, recipientUserId == uid() |
| **Risk** | LOW — recipient-only |

---

## 2. Prohibited Query Patterns

| Pattern | Status |
|---|---|
| Global queries on private data | NOT FOUND |
| Queries without limit on non-trivial data | FIXED (4 queries bounded) |
| Cross-tenant queries | NOT APPLICABLE (no tenants) |
| Queries requiring overly broad rules | NOT FOUND |
| Client-side post-filtering of private data | NOT FOUND |

---

## 3. Indices Review

### Existing indices (`firestore.indexes.json`)

| Collection | Fields | Needed | Status |
|---|---|---|---|
| `profile_visuals` | directoryVisible, searchable, username | Yes (username search) | ✅ |
| `dm_chats` | participants (contains), updatedAt (desc) | Yes (chat listing) | ✅ |

### Missing indices

No missing indices identified. All queries use single-field or composite indices that are either auto-created or explicitly defined.

---

## 4. Summary

All Firestore queries in the codebase are:
- ✅ Bounded by `limit`
- ✅ Scoped to authorized data (owner, participant, or privacy-gated)
- ✅ Covered by appropriate indices
- ✅ Validated by matching Firestore rules
