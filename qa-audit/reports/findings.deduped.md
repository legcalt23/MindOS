# Findings (deduped)

> Triage-agent — 2026-06-13. Hallazgos deduplicados, causa raíz vs síntoma, severidad final.
> Fuentes: architecture-agent, build-agent, navigation-agent, security-agent, api-agent, ui-agent, performance-agent, state-agent (Lote B-extra).

## P0 / P1 / P2
Ninguno. El repo tiene una base de seguridad sólida (AES-GCM, App Check, Firestore limits, account-enumeration mitigado, timeouts en toda API Firebase).

## P3 — Requieren acción antes de producción en escala

| # | ID | Título (causa raíz) | Área | Fix recomendado |
|---|----|---------------------|------|-----------------|
| 1 | PRIV-002 | Overlay anti-mirones **falla en silencio** si se deniega la cámara → UX de falsa seguridad | android / privacy | Mostrar estado "protección inactiva" visible + acción de re-activar |
| 2 | PRIV-001 | **Cámara frontal sin rationale** al activar privacidad → riesgo Play Data Safety + RGPD | android / privacy | Diálogo rationale previo + disclosure en política de datos |
| 3 | API-001 | `signOut()` no borra `SecureKeyValueStore` → datos cognitivos cifrados persisten en disco post-logout (RGPD "derecho al olvido") | common / data | Al logout, `clearNamespace()` para todos los namespaces del usuario activo; o documentar decisión en política de datos |
| 4 | FOCUS-001 | `abort()` durante COOLDOWN puede cancelar `finishSession()` a mitad → historial OK pero perfil adaptativo NO actualizado | common / state | Wrap `historyRepository.append() + profileRepository.save()` en `withContext(NonCancellable)` |
| 5 | ARCH-001 | `kotlinx-datetime` 0.6.0 pineado vs 0.7.1 transitivo (`Clock.System` removido) — latente, mitigado por EpochClock pero frágil | shared / build | Regla detekt `ForbiddenMethodCall` sobre `Clock.System.now()` fuera de `SystemEpochClock`; forzar pin explícito en `shared/build.gradle.kts` |
| 6 | NAV-001 | Back físico **no-op en desktop** (PlatformBackHandler.desktop es no-op) → pantallas profundas sin back visible dejan atrapado al usuario | desktop / ux | Auditar en runtime que TODA pantalla pusheada incluye back/close visible; checklist en PR template |
| 7 | ARCH-003 | `iosMain` no compilable en Windows → actuals rotos pueden quedar sin detectar hasta intento de build iOS | ios / platform | Job CI en macOS (`macos-latest`) que compile `iosArm64` + `iosSimulatorArm64` |

## P4 — Deuda técnica y mejoras de mantenibilidad

| # | ID | Título | Fix |
|---|----|---------|----|
| 8 | BUILD-001 | Warnings de deprecación Gradle (incompatible con Gradle 10) | `--warning-mode all` + migrar APIs deprecadas |
| 9 | NAV-002 | Añadir `AppRoute` obliga a actualizar rebinder + telemetryName + stack — acoplamiento latente | Usar `when` exhaustivo sin `else` para que el compilador fuerce los 3 updates |
| 10 | AUTH-001 | `runCatching { signIn() }.fold(...)` en `AuthScreen` atrapa `CancellationException` → `failedAttempts++` espurio en cancelación de scope | Sustituir por `try/catch` re-lanzando `CancellationException` explícitamente |
| 11 | NAV-004 | `LaunchDestinationResolver.resolvePostLoginRoute()` nunca devuelve `AppRoute.ElementSelection` → pantalla de onboarding no alcanzable | Decidir: quién pushea ElementSelection (¿AuthScreen al crear cuenta nueva? ¿flag en perfil?). Si se elimina, limpiar el sealed AppRoute. |
| 12 | NAV-005 | `GlobalFeedScreen.onOpenProfile` usa `stack.replace()` → feed destruido; back desde perfil va a Home | Decisión de producto: si es intencional, documentarlo; si no, cambiar a `stack.push()` |
| 13 | ARCH-004 | `materialIconsExtended` 1.7.3 desalineado de Compose 1.10.3 | Migrar a Material Symbols vector resources |
| 14 | PERF-000 | Sin red flags estáticos; profiling en device pendiente | Compose metrics + scroll en GlobalFeed/DMList en device |

## Resueltos / limpios

| ID | Hallazgo inicial | Resolución |
|----|-----------------|------------|
| ARCH-002 | Datos faciales ML Kit: riesgo biométrico | **Clean**: on-device, transient, opt-in, sin persistir/transmitir |
| NAV-003 | `isOnline` fabricado | **Clean**: solo en test fixture, producción usa default `false` |
| API-002 | Aislamiento por-uid en SecureKeyValueStore | **Clean**: claves son `"profile:uid"`, aisladas; cifrado AES-256-GCM |

## Árbol de causa raíz (deduplication notes)

- PRIV-001 y PRIV-002 son **síntomas distintos** de la misma feature (overlay de privacidad): no fusionar. PRIV-001 = ausencia de rationale (UX/compliance); PRIV-002 = fallo silencioso (funcional).
- API-001 y FOCUS-001 son distintos: API-001 = datos persisten post-logout para el mismo dispositivo (privacidad); FOCUS-001 = datos parcialmente escritos por abort (integridad).
- NAV-004 y NAV-005 son ortogonales: NAV-004 = ruta muerta en resolver; NAV-005 = comportamiento de replace vs push en feed.
- AUTH-001 no es duplicado de PRIV-001 (son layers distintos: coroutine hygiene vs UX).

## Orden de fix recomendado

1. FOCUS-001 (P3, corrección trivial: `NonCancellable`, 1 línea)
2. PRIV-002 (P3, estado "inactiva" visible)
3. PRIV-001 (P3, rationale + disclosure)
4. API-001 (P3, decisión de `clearNamespace()` en logout o documentar política RGPD)
5. ARCH-001 (P3, regla detekt)
6. NAV-001 (P3, auditoría runtime desktop)
7. ARCH-003 (P3, job CI macOS)
8. P4 en orden: BUILD-001 → NAV-002 → AUTH-001 → NAV-004 → NAV-005 → ARCH-004 → PERF-000
