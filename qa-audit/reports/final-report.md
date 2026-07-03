# Final QA Audit Report — MindOS / Mindverse (KMP)

Fecha: 2026-06-13 · Auditor: Windows 11 · Modo: estático + build de verificación (sin login en vivo).

## Executive Summary

Auditoría estática + verificación de build de la app KMP MindOS. **La salud de compilación es buena**:
`:shared:desktopTest`, `:shared:detekt` (ratchet) y `:shared:compileAndroidMain` pasan en verde. **No hay
findings P0/P1/P2 abiertos.** El hallazgo de mayor calado inicial (datos faciales biométricos, ARCH-002)
quedó **resuelto**: el overlay anti-mirones procesa rostro 100% on-device, sin persistir ni transmitir, y es
opt-in. Quedan **7 findings P3** (integridad de escritura en aborto de sesión, consentimiento/fallo silencioso
de cámara de privacidad, aislamiento de cachés locales en logout, y deuda de plataforma) y **7 P4**. Áreas en
vivo (login, device, iOS) y backend `/functions` quedan **pendientes/bloqueadas** por inputs y presupuesto.

> **Actualización 2026-06-13 (Lote B-extra):** Profundización en state, auth y navegación añadió FOCUS-001 (P3),
> AUTH-001 (P4), NAV-004 (P4), NAV-005 (P4) y cerró API-002 (aislamiento multi-usuario confirmado).

## Environment

- Auditor OS: Windows 11. Android device/emulador: disponible (no ejercitado aún). macOS/Xcode: no.
- Toolchain: Kotlin 2.3.10 · AGP 9.1.0 · Compose Multiplatform 1.10.3 · JVM 17 · gitlive Firebase 2.4.0.
- Módulos: `:shared` (KMP, plugin `com.android.kotlin.multiplatform.library`), `:androidApp`, `:desktopApp` (+ build `mindos/` aparte).

## Platforms Tested

| Platform | Status | Notes |
|----------|--------|-------|
| Android  | compila ✅ / live pending | `:shared:compileAndroidMain` verde; assembleDebug + device pendiente |
| Desktop  | tests ✅ | `:shared:desktopTest` verde; affordances de back en runtime pendientes |
| iOS      | static-only | sin macOS/Xcode → no compilable aquí |

## Function Coverage

| Área (agente) | Cobertura | Estado |
|---------------|-----------|--------|
| architecture | estático | ✅ done |
| build | ejecutado | ✅ done (3 tareas verdes) |
| navigation | estático (37 pantallas inventariadas) | ✅ done; dinámico (deep links, dead ends) pendiente |
| security | estático enfocado | ✅ done (ARCH-002 resuelto) |
| api | estático (sesión) | ◑ parcial — DM/wallet/social repos pendientes |
| state | vía security/nav | ◑ parcial (isOnline limpio) |
| ui | estático | ◑ parcial — estados vacío/dark/accesibilidad → runtime |
| performance | estático | ◑ parcial — profiling en device pendiente |
| android (live) | — | ⏳ pending (device disponible) |
| ios (live) | — | ⛔ blocked (Windows) |
| auth (live) | — | ⛔ blocked (faltan credenciales) |
| backend /functions | — | ⏳ deferred (gate-blocked; requiere sesión dedicada) |

## Findings by Severity

### P0 / P1 / P2
Ninguno abierto.

### P3
- **FOCUS-001** — `FocusSessionStateHolder.abort()` puede cancelar `finishSession()` **entre los dos writes sequenciales** (`historyRepository.append` → `profileRepository.save`): la sesión queda en historial pero la dificultad adaptativa NO se actualiza → próxima sesión empieza en dificultad obsoleta. Fix: `withContext(NonCancellable)` alrededor de ambos writes. *(data-integrity/common)*
- **PRIV-002** — La protección de privacidad (overlay anti-mirones) **falla en silencio** si se deniega la cámara o no hay cámara frontal: el usuario cree estar protegido y no lo está. *(security/android)*
- **PRIV-001** — Se solicita la **cámara frontal sin rationale** al activar la pantalla de privacidad; falta disclosure (Play Data Safety + política). *(security/android)*
- **API-001** — `signOut()` limpia auth de Firebase pero **no purga cachés locales sensibles** (`SecureKeyValueStore` cognitivo); datos cifrados quedan en disco post-logout. Riesgo RGPD "derecho al olvido". Las claves están aisladas por uid (API-002 resuelto), pero otro usuario del mismo dispositivo sí los hereda en disco cifrado. Fix: `clearNamespace()` para todos los namespaces del usuario activo en logout, o documentar en política de datos. *(data/common)*
- **ARCH-001** — `kotlinx-datetime` 0.6.0 pineado vs 0.7.1 transitivo (quita `Clock.System`); **latente** (suite pasa por la mitigación EpochClock). *(build/shared)*
- **NAV-001** — En **desktop** el back físico es no-op → cada pantalla profunda necesita back/close visible (verificar en runtime). *(navigation/desktop)*
- **ARCH-003** — `iosMain` no compilable en Windows → riesgo de actuals rotos sin detectar. *(platform/ios)*

### P4
- **BUILD-001** — Warnings de deprecación Gradle (incompatible con Gradle 10).
- **NAV-002** — Añadir un `AppRoute` exige tocar rebinder+telemetry+stack (usar `when` exhaustivo sin `else`).
- **AUTH-001** — `runCatching { signIn() }.fold(...)` en `AuthScreen` atrapa `CancellationException` → `failedAttempts++` espurio en cancelación de scope. Fix: `try/catch` re-lanzando `CancellationException` explícitamente. *(coroutines/common)*
- **NAV-004** — `LaunchDestinationResolver.resolvePostLoginRoute()` nunca devuelve `AppRoute.ElementSelection` → flujo de onboarding no alcanzable vía login normal. Decidir: quién pushea `ElementSelection` o eliminar del sealed si es obsoleta. *(navigation/common)*
- **NAV-005** — `GlobalFeedScreen.onOpenProfile` usa `stack.replace()` → el feed queda destruido; back desde perfil va a Home, no al feed. Documentar si es intencional o cambiar a `stack.push()`. *(navigation/common)*
- **ARCH-004** — `materialIconsExtended` 1.7.3 desalineado de Compose 1.10.3.
- **PERF-000** — Sin red flags estáticos; profiling en device pendiente.

### Resueltos / limpios
- **ARCH-002** ✅ Datos faciales: on-device, transient, sin persistir/transmitir, opt-in → no hay fuga biométrica.
- **NAV-003** ✅ `isOnline` fabricado: solo en un test fixture; producción limpia.
- **API-002** ✅ Aislamiento multi-usuario en `SecureKeyValueStore` confirmado: claves con prefijo uid (`"profile:uid123"`), un segundo usuario del dispositivo no accede a datos del primero. El concern residual (datos post-logout en disco) se mantiene como API-001.
- Secretos hardcodeados en `*.kt`: 0.

## Cross-Agent Conflicts
Ninguno. ARCH-002 (architecture) fue investigado y cerrado por security-agent sin contradicción.

## Untested / Blocked Areas
- **Login en vivo** y todos los flujos de auth dinámicos: faltan credenciales de prueba (entorno dev).
- **iOS**: build/run imposible en Windows; solo revisión estática.
- **Android en vivo**: `assembleDebug` + install + `adb logcat` + lifecycle/rotación → device disponible, pendiente de ejecutar.
- **Backend `/functions`**: `scan:secrets` + jest (713 tests) → diferido (budget gate lo clasificó broad; sesión dedicada).
- **Navegación dinámica**: deep links, botones sin acción, pantallas muertas → requiere ejecución.

## Recommended Fix Order

1. **FOCUS-001** (P3, trivial: 1 línea `withContext(NonCancellable)`) — integridad de datos adaptativa.
2. **PRIV-002** — la protección de privacidad no debe fallar en silencio. Mostrar estado "protección inactiva" + acción.
3. **PRIV-001** — añadir rationale + disclosure de cámara frontal (riesgo de política Play).
4. **API-001** — `clearNamespace()` en logout por todos los namespaces del usuario activo, o documentar decisión RGPD.
5. **ARCH-001** — regla detekt/lint que prohíba `Clock.System.now()` fuera de `SystemEpochClock`; pin explícito en `shared/build.gradle.kts`.
6. **NAV-001** — auditar back/close visible por pantalla en desktop (runtime).
7. **ARCH-003** — job CI en macOS que compile `iosArm64` + `iosSimulatorArm64`.
8. P4 en orden: **BUILD-001** → **NAV-002** → **AUTH-001** → **NAV-004** → **NAV-005** → **ARCH-004** → **PERF-000**.

## Regression Test Plan
Ver `qa-audit/reports/regression-plan.md`.

## Next Sprint Plan
- Ejecutar Lote C en device (assembleDebug + logcat) y backend `/functions` en sesiones dedicadas (re-gate).
- Conseguir credenciales dev → auth-agent live (14 flujos de login).
- Implementar fixes en orden recomendado con su test de regresión.
