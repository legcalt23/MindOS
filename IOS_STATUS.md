# Estado real de iOS — fuente de verdad

**Fecha:** 2026-05-29
**Decisión de alcance:** **Android-first. iOS APLAZADO (fuera de v1).**

> Este documento prevalece sobre cualquier afirmación de paridad iOS en
> `*_PARITY_*.md`. Esos reports fueron escritos antes de verificar el wiring real.

## Realidad verificada en código

iOS es actualmente un **shell de preview no funcional**, NO una app conectada al backend:

| Área | Estado iOS real | Evidencia |
|---|---|---|
| Sesión / Auth | **Bloqueada**: `MainViewController` no monta `App()` hasta tener sesión real | `MainViewController.kt` |
| App Check | **No instalado** en iOS | sin provider App Attest/DeviceCheck |
| Feed global | **Fail-closed** (`DisabledGlobalFeedRepository`) hasta tener sesión/App Check reales | `GlobalFeedRepositoryProvider.ios.kt` |
| Privacy overlay | **Stub** (`content()` sin protección) | `PrivacyScreenOverlay.ios.kt` |
| Observabilidad | **Nula** (sink solo en Android) | `MindOSApplication.kt` |
| Compilación | **Gate de CI macOS añadido** para compilar frameworks shared; app iOS runtime sigue fuera de v1 | `.github/workflows/ios-build.yml`, `shared/build.gradle.kts:41` |

Consecuencia: aunque se cableara la sesión real, las reglas con `isAppChecked()`
(DM media, profile visuals, posts) rechazarían a iOS por falta de App Check.

## Qué se necesita para activar iOS (cuando entre en alcance)

1. Wirear `FirebaseSessionRepository` real en `MainViewController` (no `App()` preview).
2. Instalar App Check iOS (App Attest / DeviceCheck) en el arranque Swift.
3. Mover el feed (y otros repos hoy stub) a `commonMain` sobre gitlive Firestore.
4. Implementar privacy overlay nativo iOS (sin cámara).
5. Instalar sink de observabilidad iOS.
6. Compilar en runner macOS en CI (`:shared:iosSimulatorArm64Test`) para evitar drift.
7. Si la moneda virtual se vende por dinero real: StoreKit IAP (Guideline 3.1.1).

Hasta completar 1–7, **los `*_PARITY_*.md` describen una paridad que no existe en runtime.**
