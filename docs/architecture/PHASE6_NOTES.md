# Fase 6 — Observabilidad, analytics y operación: notas

**Fecha:** 2026-05-29

## Entregado y verificado
- **T6.1 Performance Monitoring:** SDK `firebase-perf` añadido (vía BOM). `assembleDebug` OK.
  Da automáticamente traza de arranque de app, foreground/background y la API de trazas custom.
- **T6.3 Remote Config + feature flags (kill switches):**
  - `core/config/FeatureFlags.kt` (commonMain): `FeatureFlags` + `DefaultFeatureFlags` (todo ON) +
    `StaticFeatureFlags` + `FeatureFlagsHub` (registro global, patrón `ObservabilityHub`).
  - Proveedor Android `RemoteConfigFeatureFlags` en `MindOSApplication` (lee live del cache activado;
    fallback a defaults cuando la fuente es STATIC → seguro durante el fetch async).
  - **Kill switch cableado:** el overlay de privacidad (cámara continua) ahora se gatea por
    `privacyEnabledState && FeatureFlagsHub.current.privacyOverlayEnabled` → ops puede desactivarlo
    remotamente sin actualizar la app. `desktopTest` (FeatureFlagsTest) + `assembleDebug` OK.

## Deferidos (con motivo)

### T6.1 — Plugin Gradle de firebase-perf (auto-instrumentación HTTP) — DIFERIDO
Solo se añadió el SDK, NO el plugin `com.google.firebase.firebase-perf`. El plugin instrumenta
bytecode (trazas HTTP automáticas) y su compatibilidad con **AGP 9.1.0** (bleeding-edge) no está
verificada; añadirlo a ciegas arriesga romper el build. El SDK ya cubre arranque + trazas custom.
Activar el plugin cuando se confirme compat con AGP 9.x (añadir classpath + `apply` + medir build).

### T6.2 — Sink de observabilidad iOS — GATED por iOS
`ObservabilityHub.sink` solo se instala en Android (`MindOSApplication`). iOS usa el sink no-op
(documentado en `Observability.kt`). iOS está aplazado de v1 (`IOS_STATUS.md`) y la app iOS es un
shell de preview sin Crashlytics pod cableado → wirear el sink iOS (Crashlytics iOS vía el
`MainViewController` real) se hace cuando iOS reentre en alcance. Mismo patrón que el sink Android.

### Kill switches NO cableados aún (flags definidos, gating pendiente)
`wavesEnabled` y `walletEnabled` existen en `FeatureFlags` y se leen desde Remote Config, pero su
gating en UI vive en god-screens (DM / wallet) sin tests UI → cablearlos se agrupa con el troceado
de esas pantallas (Fase 5 T5.3 / Fase 2 T2.5) para no editar a ciegas. Hoy: definidos y operables
vía consola, sin punto de corte en UI.

## Setup operativo requerido (consola Firebase, fuera del repo)
Crear en **Remote Config** los parámetros booleanos (default `true`), para poder usarlos como
kill switch:
- `privacy_overlay_enabled`
- `waves_enabled`
- `wallet_enabled`

Mientras no existan en consola, el cliente usa los defaults compilados (`DefaultFeatureFlags`, todo
ON) — comportamiento idéntico al actual.
