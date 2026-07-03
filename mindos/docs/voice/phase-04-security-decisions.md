# Phase 4 App Runtime Security Decisions

Date: 2026-06-10

## Status

Accepted for Phase 4.

## Risk

An app-facing builder can accidentally bypass the canonical gateway, initialize speech while voice
is disabled, accept unsafe production dependencies, expose internal components, or create a second
navigation owner.

## Decision

- `mind-app-runtime` is the only app-facing composition facade introduced in Phase 4.
- Runtime and voice flags are disabled by default.
- A disabled runtime creates only a typed disabled gateway. It does not initialize router,
  executor, or speech components.
- `voiceEnabled=false` never invokes the speech factory.
- Voice cannot be enabled while the command runtime is disabled. Voice enabled without a supplied
  factory returns a typed build error.
- Production construction delegates to `CommandRouterFactory.forProduction()` and
  `ActionExecutorFactory.forProduction()`; unsafe wiring returns a typed, sanitized build error and
  never falls back to development/test factories.
- A `NavigationCoordinator` is an explicit required dependency. The facade never creates,
  replaces, or bypasses it.
- The built runtime exposes only `TextCommandGateway` and an optional `VoiceCommandOrchestrator`.
  Router, executor, stores, registries, provider SDKs, credentials, and implementation components
  remain hidden.
- Expected initialization exceptions become typed build failures. Cancellation and unrecoverable
  runtime errors are not swallowed.
- No app-root wiring, UI, Android audio, confirmation UI, NVIDIA integration, or provider
  credentials are added in Phase 4.

## Verification

- Run `.\gradlew.bat :mind-app-runtime:jvmTest --console=plain`.
- Verify disabled flags do not initialize component/speech factories.
- Search `mind-app-runtime` for provider, platform, credential, and app-UI dependencies.
- Run `.\gradlew.bat productionReady --console=plain`.

## Rollback

Keep the app runtime absent from the main application build, remove `mind-app-runtime` from the
MindOS build, and remove this document. Existing text and headless voice modules remain disabled
and independently testable.
