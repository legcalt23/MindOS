---
id: coding-standard-adding-a-member-to-featureflags-requires-updating-every-impl
type: CODING_STANDARD
title: Adding a member to FeatureFlags requires updating every implementation
tags: [kmp, feature-flags, modules, compile]
source: CLAUDE.md
importance: high
created_at: 2026-06-30
updated_at: 2026-06-30
version: 1
encrypted: false
access_level: internal
related: []
supersedes: null
superseded_by: null
metadata: {}
content_hash: 41cd50ce3d65e57f22ab8b7c16689cd91dc606eb4f5b762373639d0493842ac4
---

## Rule
When you add a member to the FeatureFlags interface, update ALL implementations in the same change: DefaultFeatureFlags, StaticFeatureFlags (in :shared) and RemoteConfigFeatureFlags (in androidApp).

## Why
RemoteConfigFeatureFlags lives in androidApp, outside :shared. Compiling only :shared:compileAndroidMain will NOT catch the missing override — the gap appears only when compiling :androidApp.

## How to apply
Add the override with the existing boolOrDefault(KEY_*, *Enabled) pattern and verify with `:androidApp:compileDebugKotlin`, not just `:shared`.
