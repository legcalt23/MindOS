# Firestore P0 hardening decisions — 2026-06-19

Scope: phase 0 prompt 7 (`P0-008` through `P0-011`). These decisions do not include work from later phases.

## P0-008 — block parity in profile discovery

Decision: every `profile_visuals` list result must satisfy the same bidirectional block check as a direct `get`. A blocked profile therefore cannot be returned by username search.

Firestore Rules cannot express “filter out documents for which a related block document exists”. If a bounded query could include a blocked profile, Firestore denies the complete query rather than returning a partial result. This is a deliberate fail-closed privacy trade-off. The client may surface a generic search failure; it must not retry through a less restrictive collection.

## P0-009 — DM read-receipt ownership

Decision: keep the existing `lastReadAtByUser` map, but constrain its keys to the two canonical participants. On create, a caller may populate only their own key. On update, `Map.diff().affectedKeys()` may contain only `request.auth.uid`, so one participant cannot create, overwrite, or delete the peer's receipt.

Values remain non-negative epoch milliseconds to preserve the current KMP DTO contract. No schema migration is required. A legacy document containing a non-participant key will fail closed on later client updates and requires privileged cleanup if one is discovered.

## P0-010 — authoritative post writes

Decision: direct client create, update, and delete operations on both `posts/{postId}` and `user_posts/{uid}/posts/{postId}` are denied. `createPost` and `deletePost` Cloud Functions are the only mutation authority and write through Admin SDK after enforcing App Check, authentication, email verification where applicable, suspension state, rate limits, ownership, strict payload validation, Storage metadata integrity, and mirror consistency.

The Android repository now uploads attachments, invokes `createPost`, and reads back the canonical server document. If the callable fails, it removes the uploaded objects best-effort. Text styling is validated and persisted by the callable so the authority change does not remove an existing product capability.

## P0-011 — canonical author identity

Decision: a post cannot be created without `profile_visuals/{uid}`. The backend derives `authorUsername` and `authorDisplayName` exclusively from that profile, validates them before its Admin SDK write, and rejects client-supplied author fields. Rules also require an existing canonical profile when post data is read.

The previous Android fallback from request fields was removed. This prevents a modified client or incomplete bootstrap from persisting an arbitrary author identity. Existing malformed or profile-orphaned posts become unreadable until repaired or deleted; no automatic migration is introduced in phase 0.

## Verification evidence

- Firestore Rules emulator: direct get and bounded username search deny blocked profiles.
- Firestore Rules emulator: own DM receipt updates succeed; peer and non-participant mutations fail.
- Firestore Rules emulator: modified clients cannot write either post collection, with or without a canonical profile.
- Functions tests: missing/malformed profiles and client-supplied author fields fail; canonical identity and text style are persisted.
- Android compilation: the production feed repository compiles against the callable path.
