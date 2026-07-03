# Storage Rules dynamic harness decision

Date: 2026-06-19  
Scope: Fase 0, P0-005

## App Check in the emulator

Production `storage.rules` keeps `isAppChecked()` as `request.app != null`. The Firebase Storage emulator does not expose `request.app`, so the dynamic harness reads the production rules and replaces exactly one canonical guard with `true` in memory. It fails before loading rules if that canonical guard is absent or duplicated. No generated test ruleset is deployed or written over the production file.

Static checks continue to require App Check on owner-scoped reads, DM reads and DM uploads. Dynamic tests exercise authentication, authorization, private metadata, object names, MIME types, sizes, immutability and cross-user denial with the documented App Check substitute.

## Security finding: thumbnail size bypass

The first dynamic run showed that `1700000000006_thumb.jpg` larger than 1 MiB was accepted. `validThumb()` rejected it, but the same name also matched `validImage()` and inherited the 15 MiB main-image allowance.

Decision: thumbnail and generic-image branches must be mutually exclusive. Any name matching the canonical thumbnail pattern can only pass `validThumb()` and its 1 MiB limit. A static regression check and a dynamic oversized-thumbnail denial pin this invariant.

## Security finding: create branch accepted an overwrite

The dynamic suite also observed an overwrite of an existing post attachment succeeding even though `allow update` is false. The profile and post create branches did not carry the explicit `resource == null` precondition already used by DM media.

Decision: every media `allow create` branch now requires `resource == null`. This makes immutability an explicit invariant independent of emulator operation classification, and a static check requires all three media paths to retain it.

## CI requirement

`npm run test:rules` now requires both the static Storage checks and all 27 dynamic assertions. The Storage suite starts Firestore as well because production Storage Rules use `firestore.exists/get`.
