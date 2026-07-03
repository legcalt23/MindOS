---
id: coding-standard-adding-a-sealed-uistate-variant-and-its-renderer-are-one-cha
type: CODING_STANDARD
title: Adding a sealed UiState variant and its renderer are one change
tags: [detekt, compose, kmp]
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
content_hash: 76b221541b716f06f23179db6e315b37c2dfea77fbd6cbab5288c028dc078c49
---

A sealed UiState forces an exhaustive when() in its renderer. Adding a variant without updating every renderer is a compile error, not a later phase. Update all when() blocks in the same change.
