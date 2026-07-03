# CEREBRO CENTRAL — Plan de auditoría multiagente (adaptado a MindOS)

Rol: orquestación, deduplicación, control de contexto, priorización. El cerebro **no** lee todo el
repo en profundidad: delega y recibe `AGENT_RESULT` (≤12 líneas) + rutas.

## Protocolo de retorno de cada agente

Cada agente escribe su detalle en `qa-audit/agents/<agente>.md` y devuelve solo el bloque
`AGENT_RESULT` definido en el prompt maestro (agent_id, scope, status, confidence, findings con
severidad P0–P4, evidencia por ruta, summary_for_brain). Logs largos → `qa-audit/logs/`.

## Roster adaptado al stack real

| Agente | Foco en este repo | Fuente principal |
|--------|-------------------|------------------|
| architecture-agent | Módulos `:shared/:androidApp/:desktopApp`, source sets, AppGraph DI, AppRouteStack nav | settings.gradle, libs.versions.toml, core/ |
| build-agent | `:shared:desktopTest`, `:shared:detekt` (ratchet), `:shared:compileAndroidMain`, `:androidApp:assembleDebug` | Gradle |
| navigation-agent | `AppRoute` (sealed) + `AppRouteStack` + 37 `*Screen.kt`; back físico; deep links | core/navigation, features/**/presentation |
| auth-agent | `FirebaseSessionRepository`, `AuthDiagnostics`, verificación email, timeouts auth | core/session |
| api-agent | Repos gitlive Firebase (DM/social/search), `WalletApiClient` (callables), `FirestoreDefaults` timeouts | features/**/data, core/network |
| state-agent | StateHolders + Flows, `SecureKeyValueStore`, offline/persistencia, EpochClock | features/**/presentation, **/data/local |
| security-agent | Secretos, `firestore.rules`/`storage.rules`, App Check, tokens, backup rules | rules + CLAUDE.md (mucho ya endurecido) |
| ui-agent | Estados vacío/loading/error, dark mode forzado, accesibilidad, consistencia cross-target | design/, StateComponents |
| performance-agent | Recomposición Compose, trabajo en main thread, listas/paginación | presentation/** |
| test-automation-agent | Cobertura commonTest/desktopTest existente + plan de regresión por finding | **/commonTest, **/desktopTest |
| android-agent | install + `adb logcat` + lifecycle/rotación/back/offline en device | device (disponible) |
| ios-agent | **Estático** de `iosMain` (Windows: sin build) | iosMain |
| triage-agent | Dedupe, severidad, causa raíz vs síntoma, matriz final | reports/ |

## Lotes de ejecución (cada uno re-pasa el budget gate)

- **Lote A — Recon + build (más barato, más señal):** architecture, build, navigation.
- **Lote B — Estático profundo:** api, state, security, ui, performance (máx 3-5 en paralelo si el entorno lo permite).
- **Lote C — En vivo (requiere inputs/presupuesto):** android-agent (device ✓), auth-agent (necesita credenciales), ios-agent (solo estático).
- **Lote D — Cierre:** test-automation-agent + triage-agent → `reports/final-report.md` + `reports/regression-plan.md`.

## Mapa de bloqueos actuales

- auth-agent **live login**: BLOCKED (faltan `APP_TEST_EMAIL`/`APP_TEST_PASSWORD`). Estático OK.
- ios-agent **build/run**: BLOCKED (Windows, sin Xcode). Estático OK.
- android-agent: device disponible → ejecutable, pendiente de presupuesto.

## Control de presupuesto (CLAUDE.md)

- Antes de CADA lote: `node .claude/scripts/budget-gate.mjs --task "<lote>" --remaining N`.
- Decisión `deny`/`plan_only`/`split` ⇒ no fan-out; dividir o diferir.
- Con ~14% restante: solo esta fase de documentación está autorizada. Los lotes A–D requieren
  recargar presupuesto o ejecutarse repartidos en varios bloques de uso.
- **No** lanzar Workflow ni fan-out de subagentes hasta tener presupuesto claro y decisión del gate.

## Severidades

P0 app inutilizable / pérdida de datos / seguridad crítica / login imposible en entorno válido ·
P1 función core rota / crash reproducible · P2 fallo con workaround · P3 UX/accesibilidad/menores ·
P4 deuda técnica / test faltante.
