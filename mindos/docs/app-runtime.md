# Mind App Runtime

`mind-app-runtime` is the public composition facade for application consumers. It builds the
canonical text runtime and optionally initializes the headless voice orchestrator.

## Public API

```kotlin
val result = MindCommandRuntimeBuilder
    .forProduction(productionDependencies)
    .build(MindCommandRuntimeConfig(enabled = true, voiceEnabled = false))

val runtime = (result as MindCommandRuntimeBuildResult.Ready).runtime
runtime.textGateway.routeAndExecute(TextCommandRequest("abre ajustes", requestId = "request-1"))
```

The built runtime exposes only `textGateway` and optional `voiceOrchestrator`. Expected build and
command failures are typed. Production construction requires a real `NavigationCoordinator` and
delegates to the secure router/executor production factories.

## Feature Flags

- `enabled=false` is the default and initializes no router, executor, or speech components.
- `voiceEnabled=false` is the default and never invokes the voice factory.
- Voice cannot be enabled while the command runtime is disabled.

## Boundaries

The facade adds no UI, platform APIs, audio provider, provider SDK, credentials, or app-root
wiring. See `docs/voice/phase-04-security-decisions.md` and the root ADR
`docs/adr/ADR-voice-002-mindos-consumption-strategy.md`.

## Verification

```powershell
.\gradlew.bat :mind-app-runtime:jvmTest --console=plain
.\gradlew.bat productionReady --console=plain
```
