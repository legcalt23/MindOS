# Regression Test Plan

Framework existente: `kotlin.test` (commonTest) + JVM `desktopTest` (verde). Patrón del repo: fakes
in-memory, sin device cuando es posible (ver `.claude/rules/kotlin/testing.md` y CLAUDE.md).

Por cada finding accionable, un test de regresión (preferentemente commonTest/desktopTest):

| Finding | Test propuesto | Ubicación sugerida |
|---------|----------------|--------------------|
| **API-001** | Tras `signOut()`, el store local sensible queda inaccesible para otro uid (o se limpia). Test del modelo de namespacing del `SecureKeyValueStore` (con fake in-memory). | `commonTest/.../core/session` o `.../data/local` |
| **PRIV-002** | Modelo de estado del overlay de privacidad: permiso denegado / sin cámara → estado "protección inactiva" (no "protegido"). Extraer la lógica de gating a una función pura testeable (hoy vive en el actual Android). | `commonTest/.../features/privacy` |
| **ARCH-001** | Regla detekt custom (o test de arquitectura) que falle si hay `Clock.System.now()` fuera de `SystemEpochClock`. | detekt config / test de convención |
| **NAV-002** | Test exhaustivo `when(route)` sobre todas las variantes de `AppRoute` (extender los 14 tests de `AppRouteRebinderTest`) para forzar actualización al añadir rutas. | `commonTest/.../core/navigation` |
| **NAV-001** | (runtime) checklist de affordance de back por pantalla en desktop — no automatizable en unit test; cubrir con UI test desktop (`compose.uiTest`). | `desktopTest` UI |
| **PRIV-001** | (parcial) test de que el flujo pide rationale antes de lanzar el permiso (si se extrae la decisión a modelo puro). | `commonTest/.../features/privacy` |

Findings sin test unitario natural (verificación manual/runtime): ARCH-003 (CI macOS), ARCH-004 (migración), BUILD-001 (`--warning-mode all`), PERF-000 (profiling device).

> Implementación de tests: pendiente de autorización + presupuesto (cada uno re-gate). Priorizar API-001 y PRIV-002 (P3 de seguridad/datos).
