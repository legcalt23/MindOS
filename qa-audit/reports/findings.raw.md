# Findings (raw)

> `verified` = confirmado con build/ejecución · `resolved/clean` = investigado y sin riesgo · `not_reproduced` = hipótesis estática pendiente.

## architecture-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| ARCH-001 | **P3 latente** | build | shared | datetime 0.6.0 vs 0.7.1 transitivo (Clock.System) | verified: NO activo (suite verde) | regla anti-`Clock.System` fuera de SystemEpochClock |
| ARCH-002 | **info (resuelto)** | security | android | ML Kit face-detection: dato biométrico | **resolved**: on-device, transient, sin persistir/transmitir, opt-in | ver PRIV-001/002 (disclosure, no fuga) |
| ARCH-003 | P3 | platform | ios | iosMain no verificable en Windows | always (estructural) | job CI macOS + checklist expect/actual |
| ARCH-004 | P4 | build | common | materialIconsExtended 1.7.3 desalineado de Compose 1.10.3 | always | migrar a Material Symbols |

## navigation-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| NAV-001 | P3 | navigation | desktop | back físico no-op en desktop/iOS | not_reproduced | ui-agent: verificar back affordance visible |
| NAV-002 | P4 | navigation | common | añadir AppRoute toca rebinder+telemetry+stack | always | when exhaustivo sin else |
| NAV-003 | **clean** | ui | common | residual `isOnline` fabricado | **resolved**: solo en test fixture, producción limpia | — cerrado |

## build-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| BUILD-001 | P4 | build | shared | warnings deprecación Gradle (incompatible Gradle 10) | verified | `--warning-mode all` para localizar origen |

**Salud de build (verificada):** `:shared:desktopTest` ✅ 18s · `:shared:detekt` ✅ ratchet verde · `:shared:compileAndroidMain` ✅ 13s. Logs en `qa-audit/logs/`.

## security-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| PRIV-001 | **P3** | security | android | pide CÁMARA frontal sin rationale al activar pantalla de privacidad | verified (estático) | rationale + disclosure Data Safety |
| PRIV-002 | **P3** | security | android | protección de privacidad falla en silencio si se deniega cámara | verified (estático) | mostrar estado "protección inactiva" + acción |

**Limpio:** secretos hardcodeados (grep `*.kt`) → 0. Presencia fabricada → 0 en producción.

## api-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| API-001 | **P3** | data | common | `signOut()` no purga cachés locales sensibles (SecureKeyValueStore cognitivo) | not_reproduced | verificar namespacing por-uid o limpiar en logout |

**Limpio (api):** timeouts en toda op de auth ✅ · `CancellationException` re-lanzada ✅ · sin tokens en claro ✅ · enumeración mitigada ✅.

## ui-agent / performance-agent

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| NAV-001 | P3 | navigation | desktop | back no-op en desktop → necesita back visible por pantalla | not_reproduced | auditar desktop run (Lote C) |
| PERF-000 | P4 | performance | common | sin red flags estáticos; profiling pendiente | not_reproduced | Compose metrics + scroll feeds en device |

**Buena señal (ui):** cobertura de estados loading/error amplia (133 usos/25 archivos).

## Conteo

P0: 0 · P1: 0 · P2: 0 · P3: 6 (ARCH-001, ARCH-003, NAV-001, PRIV-001, PRIV-002, API-001) · P4: 4 (ARCH-004, NAV-002, BUILD-001, PERF-000) · resueltos/limpios: 2 (ARCH-002, NAV-003)

## Pendiente
- Lote C (device disponible): `:androidApp:assembleDebug`, install + adb logcat, lifecycle/rotación. **auth-agent live** BLOCKED (faltan credenciales).
- iOS: solo estático (Windows). Backend `/functions`: `scan:secrets` + jest (sin device, ejecutable).
- Lote D: triage + `final-report.md`.

---

## Lote B-extra (2026-06-13) — profundidad adicional: state, auth, navigation

### state-agent (focus / auth)

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| FOCUS-001 | **P3** | data-integrity | common | `FocusSessionStateHolder.abort()` puede cancelar `finishSession()` a mitad de escritura (history saved pero profile NOT updated) | verified estático | wrap `historyRepository.append` + `profileRepository.save` en `withContext(NonCancellable)` |
| AUTH-001 | P4 | coroutines | common | `runCatching { signIn() }.fold(...)` en `AuthScreen` atrapa `CancellationException`; `failedAttempts++` espurio | verified estático | usar `try/catch` con re-lanzamiento explícito de `CancellationException` |

Detalle FOCUS-001: `finishSession()` ejecuta dos writes secuenciales suspendibles: `historyRepository.append(session)` → `profileRepository.save(profile)`. Si `sessionJob.cancel()` se llama (hardware back durante COOLDOWN), la segunda write puede no ejecutarse. Ventana: ~50–200ms entre ambas writes. Impacto: sesión queda en historial pero la dificultad adaptativa NO se actualiza → próxima sesión empieza en dificultad obsoleta.

### navigation-agent extra

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| NAV-004 | P4 | navigation | common | `LaunchDestinationResolver.resolvePostLoginRoute()` NUNCA devuelve `AppRoute.ElementSelection` — flujo de onboarding no alcanzable vía login normal | verified estático (`LaunchDestinationResolver.kt:13-24`) | decidir quién pushea `AppRoute.ElementSelection` (AuthScreen, onboarding flag) o eliminar si obsoleta |
| NAV-005 | P4 | navigation | common | `GlobalFeedScreen.onOpenProfile` usa `stack.replace()` — el feed queda destruido; back desde perfil va a Home, no al feed | verified estático (`App.kt:153-170`) | documentar decisión de producto o cambiar a `stack.push()` si el feed debe ser recuperable |

### api-agent extra — SecureKeyValueStore aislamiento

| id | severity | category | target | título | estado | next action |
|----|----------|----------|--------|--------|--------|-------------|
| API-002 | info / clean | data | android | `SecureKeyValueStore` ya usa claves con UID como prefijo (`"profile:uid"`, `"session:uid:ts"`) — aislamiento por usuario confirmado | **resolved**: `LocalFocusProfileRepository.save()` usa `FocusKeys.PROFILE + profile.userId`. API-001 (datos post-logout) persiste pero un segundo usuario no accede a datos del primero. | PRIV concern: datos cifrados quedan en disco post-logout; issue de RGPD "derecho al olvido" latente si se implementa en producción en mercados UE |

## Conteo actualizado (post Lote B-extra)

P0: 0 · P1: 0 · P2: 0 · P3: 7 (+FOCUS-001) · P4: 7 (+AUTH-001, +NAV-004, +NAV-005, +API-002→info) · resueltos/limpios: 3 (+API-002)

Nota P2→P3: St-NEW-1 rebajado a P3 (ventana de corrupción muy estrecha, impacto acotado a dificultad adaptativa).
