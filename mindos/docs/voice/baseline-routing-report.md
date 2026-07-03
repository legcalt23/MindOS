# Voice Foundation Routing Baseline

Date: 2026-06-10

## Scope

This report freezes the command-router behavior before speech integration. It covers the public
`KmpCommandRouter` only; no speech provider, app-root integration, streaming, wake word, or TTS is
part of this phase.

## Canonical stack

- Public router: `mind-kmp-core` over contracts from `mind-kmp-contracts`.
- Execution: `mind-action-model`, `mind-action-validation`, `mind-reference-resolver`,
  `mind-action-execution`, and `mind-navigation`.
- Legacy: `command-router-*` and legacy `action-executor` are compatibility/foundation modules.
- Runtime bridge: `mind-command-runtime`.

## Frozen routing behavior

Golden coverage records:

- English: settings, messages, and parametric profile navigation.
- Spanish: settings and story-of-user navigation.
- Typed failures: no match, ambiguity, and unsafe input.
- Terminal ASR punctuation (`.`, `?`, `!`) currently returns typed `NoMatchFailureDto`.

The punctuation limitation is documented rather than changed in Phase 0. A later normalization
change must be separately reviewed and update the golden expectation.

## Baseline gate

Before Phase 1 changes, `.\gradlew.bat productionReady --console=plain` passed with 143 tasks.

## Rollback

The runtime is disabled by default. Operational rollback is to keep
`CommandRuntimeConfig.enabled=false` or remove application wiring. Repository rollback is limited
to removing `mind-command-runtime`, its Gradle registration, new tests, and these documents; no
existing public router contract was modified.
