# Profile Card — Production Notes (Olas 1–4)

The MindBoost **app profile card** (`features/profile/card/`, design `Perfil.dc.html`). This is the
social/identity surface other authenticated users can see — NOT the cognitive "brain" profile.

## Architecture

```
Edit (owner)                          View (any authed user)
ProfileCardStateHolder  ── save ──►   Firestore profile_cards_private/{uid}   (owner RW)
                                  └►   Firestore profile_cards_public/{uid}    (authed read, owner write)
ProfileCardMediaRepository ─ upload ► Storage profile_cards_public/{uid}/media/{id}.{ext}
                                       (private folder reserved for future draft-only media)
```

- **Auto-publish-on-save**: every committed `save` writes the private draft AND the public snapshot in a
  single Firestore transaction (both or neither). The public doc never lags or shows partial state.
- **Firestore stores metadata only** — a structured `ProfileCardDoc` serialized to a JSON `payload`
  string (appearance + `backgroundMedia` ref + `canvas` items). **Never bytes/base64.**
- **Storage stores binaries.** Media objects are immutable (`allow update: false`); replace = new id + delete old.

## Schema & migration

- Firestore envelope: `{ schemaVersion, [ownerUid for public], payload, updatedAt }`.
- `schemaVersion 2` = structured doc (`ProfileCardDocCodec`). `schemaVersion 1` = legacy appearance-only
  blob, migrated on decode (`ProfileCardDocCodec.decode(1, …)` → `ProfileCardDoc(appearance)`).
- Backward compatible: a v2 doc without `canvas`/`backgroundMedia` decodes to empty/none.

## Limits (defense in depth)

| Limit | Client (`ProfileCardMediaPolicy`) | Storage rule | Firestore rule |
|---|---|---|---|
| Media MIME | image/jpeg,png,webp,gif | same allowlist | n/a |
| Media size | ≤ 10 MiB | ≤ 10 MiB | n/a |
| Doc payload | n/a | n/a | ≤ 16000 chars (private & public) |

The client policy fails fast with a clear message before any upload; the Storage/Firestore rules are the
authoritative backstop. The media picker also caps bytes at pick time.

## State machine (background media)

`ProfileCardLoadStatus`: `LoadingRemoteProfile → Loaded` (loading overlay covers the default-content flash).

`ProfileCardMediaStatus`: `Idle → Uploading → Saving → Saved`, with `UploadFailed` / `SaveFailed`.
Product-state mapping:
- *PickingMedia / PreviewingLocalMedia* — local bytes are shown immediately while `Uploading` (preview is
  NEVER dropped on failure).
- *UploadError / SaveError* — `UploadFailed` / `SaveFailed`, both retryable (`retry*` re-runs from the failed
  step; a save-retry does NOT re-upload).
- *OfflinePending* — there is no separate connectivity probe; **offline is handled by the retryable failed
  states** (upload/save fails while offline → user retries when connectivity returns). Canvas items track
  failures per item in `canvasUploadFailures` with a Reintentar/Descartar affordance.

## Orphan-media strategy

- **Primary (implemented):** delete-on-mutation, best-effort, only AFTER a successful commit:
  - replace background / remove canvas item / reset background → delete the previous object;
  - **discard** a failed-but-already-uploaded background → delete the orphaned upload.
  - Failures here are logged (`safeErrorLog`), never block UX (an undeleted object is an orphan, not a crash).
- **Crash orphans (server-side, IMPLEMENTED):** an upload that succeeds but whose commit/cleanup never runs
  (process killed mid-flow) leaves an unreferenced object. **Automated reclamation is intentionally NOT done
  client-side** (a client list-and-delete sweep risks deleting *referenced* media on an incomplete/transient
  doc read — data-loss). The safe owner is the scheduled Cloud Function **`reconcileProfileCardMedia`**
  (`functions/src/profileCardGc.ts`), backed by the pure, unit-tested core `functions/src/lib/profileCardMediaGc.ts`.

### Scheduled GC — `reconcileProfileCardMedia`

Runs daily (`functions.pubsub.schedule('every 24 hours')`). Enumerates users from **Storage** (not from a
Firestore collection), then per user reads both card docs and deletes only the objects that satisfy the
**safety contract** — an object is deleted ONLY if all hold:

1. it is a card-media object (`profile_cards_{private|public}/{uid}/media/{file}`);
2. its path is **not referenced** by the owner's private OR public doc — reference extraction is
   *over-inclusive* (every media-path string anywhere in the parsed payload counts), so schema drift can
   never strand a referenced object;
3. it is **older than the grace window** (default 48h — covers the upload→commit race and clock skew);
4. its **age is known** (unknown age ⇒ never deleted).

If a doc **exists but its payload is missing/unparseable**, the whole user is **skipped** (references can't be
enumerated → nothing deleted). Admin-SDK reads are strongly consistent, so a present doc is authoritative.

- **Deleted-account media is reclaimed as a property:** `accountDeletion.ts` purges `profile_visuals/` and
  `post_attachments/` but **not** `profile_cards_*/{uid}/`. Because the GC enumerates from Storage, a deleted
  user's media (whose docs are gone → zero references) is collected once past the grace window. Trade-off: it
  is reclaimed *lazily* (next scheduled run + grace), not immediately on deletion. An optional future
  enhancement is an immediate `deleteStoragePrefix('profile_cards_private|public/{uid}/')` inside
  `executeAccountDeletion`; the GC is the durable backstop regardless.
- **Operational switches (safe first deploys):** `PROFILE_CARD_GC_DRY_RUN=1|true` plans and logs without
  deleting; `PROFILE_CARD_GC_GRACE_HOURS=<int>` overrides the grace window. Logs are counts only (PII-free).
- **Scale backstop:** at most `DEFAULT_MAX_USERS` (5000) users per run; any overflow is logged as `cappedUsers`,
  never silently truncated. Per-user/per-object delete failures are tolerated and counted, not fatal.

## Security

- `profile_cards_private/{uid}`: owner-only get/create/update/delete.
- `profile_cards_public/{uid}`: any authenticated user reads; owner-only write; `ownerUid == uid` enforced.
- Storage media mirrors this (private owner-only; public authed-read, owner-write, App Check, immutable,
  content-type + size validated). No unauthenticated access. Verified by emulator rules tests
  (`firestore.security.rules.test.mjs` T95–T109, `storage.dm.rules.test.mjs` card matrix).

## Known limitations / deferred

- **Animated GIF/WebP** now play on **Android**: the media preview (`ProfileMediaPlatform.android.kt`) renders
  through a process-wide Coil `ImageLoader` that adds the platform animated-image decoder
  (`ImageDecoderDecoder` on API 28+, else `GifDecoder`) on top of Coil's defaults. Verified by Android compile +
  Detekt; on-device playback still needs visual confirmation.
- **iOS media preview rendering is a stub** — `ProfilePlatformMediaPreview` falls back to the placeholder on iOS
  (Coil is Android-only here). NOTE: iOS *picking* DOES work (`PHPickerViewController` / `UIDocumentPickerViewController`).
  Rendering media on iOS needs a Coil 3 (multiplatform) migration or a native `UIImageView` bridge — a macOS-verified task.
- **Immediate purge of card media on account deletion** — optional; the scheduled GC reclaims it (see above).
- Visual pixel-fidelity to the prototype requires on-device confirmation (no emulator in CI for the screen).
