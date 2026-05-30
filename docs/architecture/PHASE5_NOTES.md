# Fase 5 — Testing y QA: notas de ejecución y verificación

**Fecha:** 2026-05-29

Resumen de qué se verificó localmente y qué queda gated por entorno (sin dispositivo / sin macOS /
JDK del emulador) — diferido a CI, NO a "más tarde sin plan".

## Verificado localmente
- **Functions (T5.1):** `npm run typecheck` ✓ y `npm test` → **704 tests jest, 0 fallos**.
- **Kotlin (T5.4):** suite `:shared:desktopTest` → **323 tests, 0 fallos** (incluye los nuevos
  `LaunchDestinationResolverTest` + test offline de wallet).
- **Instrumentados (T5.3):** `:androidApp:assembleDebugAndroidTest` compila (runner + GMD + test) ✓.

## Gated por entorno (verificación en CI)
| Tarea | Bloqueo | Resuelto en |
|---|---|---|
| T5.1 reglas (emulador) | `firebase-tools` 15 exige **JDK 21+**; aquí hay JDK 17 (Gradle) | `backend-ci.yml` job `rules` usa temurin 21 |
| T5.2 iOS compile | requiere **macOS** (targets `if (isMacOs)`) | `ios-build.yml` (macos-14, `workflow_dispatch`) |
| T5.3 run instrumentado | requiere **emulador/dispositivo** (KVM) | `android-instrumented.yml` (GMD, `workflow_dispatch`) |

## Por qué algunos workflows son `workflow_dispatch` (manual)
- **iOS** (`ios-build.yml`): iOS está aplazado de v1 (Android-first, ver `IOS_STATUS.md`) y nunca ha
  compilado end-to-end → un trigger push/PR sería rojo y gastaría minutos macOS. Listo para activar
  (cambiar a `on: push/pull_request`) cuando iOS reentre en alcance.
- **Instrumentado** (`android-instrumented.yml`): GMD descarga imagen de sistema + necesita KVM
  (pesado). `SmokeInstrumentedTest` compila; los tests de flujos críticos (login/DM/wallet/borrado de
  cuenta) se añaden encima cuando el emulador de Firebase (T5.1) se integre en el entorno de
  dispositivo. Activable a push/PR cuando se quiera el coste de CI.

## Pendiente dentro de Fase 5 (NO implementado — requiere device + Firebase emulator en el device job)
- Tests instrumentados de **flujos críticos**: login, DM (envío/recepción), compra wallet, borrado de
  cuenta. Necesitan orquestar el emulador de Firebase contra el dispositivo y datos sembrados.
  `SmokeInstrumentedTest` deja el pipeline listo; estos tests son el siguiente incremento.

## Desbloqueos que esta fase habilita (para fases/tasks previas en cola)
- **T3.3 (fix límites DM):** el job `rules` valida ahora `firestore.rules` contra el cliente —
  confirma en CI el contrato `.limit()` que en Fase 3 solo se verificó por compilación.
- **T2.2 (persistencia waves):** ya puede implementarse con reglas nuevas validadas por el job `rules`.

## Adenda (2026-05-30) — Storage rules: gap del emulador con App Check
El emulador de Storage (cloud-storage-rules-runtime) **no popula `request.app`** (App Check), así
que `isAppChecked()` en `storage.rules` lanza "Property app is undefined" y toda ruta App-Check-gated
se deniega en el emulador. `@firebase/rules-unit-testing` no permite inyectar App Check en contextos
de Storage. Por eso `test:storage-rules` se **excluyó del chain `test:rules` de CI** (firestore +
username rules SÍ se validan: 81/81 + username). Las reglas de Storage **siguen activas/enforced en
producción** (App Check real). Revisar si una versión futura del emulador soporta App Check.
