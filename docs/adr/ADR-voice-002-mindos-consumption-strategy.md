# ADR-voice-002: MindOS Consumption Strategy

Status: Accepted

Date: 2026-06-10

## Context

The main application and `mindos/` are separate Gradle builds in the same repository. The main
application is Kotlin Multiplatform with Android, Desktop, and optional iOS targets. `mindos/`
currently uses KMP modules with a JVM-only convention, has no stable Maven group/version, and does
not apply `maven-publish`.

The app needs a reproducible path to consume the command runtime without copying code, exposing
router/executor internals, or coupling the shared application to Android-only adapters. Phase 4
must decide that path before any UI or platform-audio integration.

Both builds currently align on Kotlin `2.3.10`, coroutines `1.10.2`, and serialization `1.9.0`.

## Decision

Use a Gradle composite build as the monorepo development and CI consumption strategy:

1. `mindos/` owns a dedicated public app-runtime facade with stable coordinates.
2. The main build will use `includeBuild("mindos")` with explicit dependency substitution from
   those coordinates to the facade project.
3. App modules depend only on the facade coordinate, never on individual `mind-*`,
   `command-router-*`, executor, or speech implementation modules.
4. The facade exposes typed app-facing runtime contracts and requires the real
   `NavigationCoordinator` at the composition root.
5. Actual app wiring remains disabled until the facade publishes a variant compatible with the
   consuming Android/KMP source set. The current JVM-only MindOS convention is an explicit blocker;
   Phase 4 does not hide it with copied sources or unsafe variant matching.

For versioned releases outside the monorepo, publish the same facade coordinate to an internal
Maven repository. Composite substitution and Maven publication must use the same public API and
coordinate so consumers do not change source code between development and release.

## Consequences

- Local and CI builds use source from the checked-out `mindos/` build and fail if it is absent or
  incompatible.
- Public app dependencies stay narrow and versionable; implementation modules remain internal.
- Android-first integration is viable once the facade has a compatible Android/JVM-consumable
  variant, without making Android the architectural owner.
- iOS and broader KMP consumption require adding supported targets to MindOS modules before app
  wiring. That work is not part of this phase.
- The main app remains unchanged and voice stays disabled during this spike.
- Internal Maven publication requires future release/versioning automation and repository
  credentials held by CI, never application clients.

## Alternatives Considered

### Internal or local Maven publication now

Rejected as the immediate development path. `mindos/` has no publication plugin, stable
coordinates, version policy, repository, or CI publication workflow. Adding those before the
public facade stabilizes would increase release complexity without solving the current target
compatibility gap. It remains the selected release-distribution path after those prerequisites.

### Android-specific adapter module

Rejected as the primary strategy because it makes Android own shared command-runtime composition,
duplicates future KMP wiring, and encourages platform types to leak into the public API. A thin
Android platform adapter may later implement platform ports, but it must consume the shared facade.

### Copying MindOS sources or binaries into the app build

Prohibited. Copies lose provenance and version clarity, bypass MindOS quality gates, and permit
security fixes to diverge.

### Direct composite dependencies on individual MindOS modules

Rejected because they expose internals, create a wide dependency surface, and make application
code sensitive to internal module refactors.

## Security Decision

- The facade is disabled by default and must require explicit typed configuration.
- The app supplies a real `NavigationCoordinator`; the facade must not create or bypass navigation.
- Speech providers, credentials, secrets, and provider SDKs are outside the facade.
- Composite substitution must target only the public facade coordinate.
- CI publication credentials belong to CI secret storage and are never embedded in app artifacts.

## Rollout And Verification

Phase 4 creates and tests the public facade inside `mindos/`, but does not add app UI, platform
audio, or root-app runtime wiring.

Before enabling consumption in the main build:

1. Add stable facade coordinates and explicit composite substitution.
2. Add an Android/KMP-compatible published variant and verify dependency resolution.
3. Keep runtime and voice feature flags disabled by default.
4. Run both the main-app verification gates and `mindos/productionReady`.

Rollback is to remove the composite include/substitution and facade dependency. The app remains
functional because no UI or app-root behavior is enabled by this decision.
