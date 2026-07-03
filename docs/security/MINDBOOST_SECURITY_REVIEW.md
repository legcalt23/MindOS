# MindBoost — Security Review

Fecha: 2026-06-20  
Scan sellado: `af2ea05c-e8bf-4750-a0e6-9f8c10376270`  
Cobertura: 519 archivos source-like más 51 artefactos locales bajo `validation/`.

## Executive summary

No se validaron hallazgos Critical, High ni Medium. Se confirmaron dos rutas Low/CWE-532 donde excepciones Firebase crudas llegaban a Crashlytics; ambas comparten la misma causa raíz y se corrigieron en la frontera central de observabilidad. También se cerraron dos gaps developer-local: permisos shell wildcard y ausencia de boundary explícita en agentes MindBoost con Bash/escritura. Las exportaciones debug de Velocidad ahora pseudonimizan identidad/dispositivo y usan nombres aleatorios.

Gate provisional: **CONDITIONAL SHIP para Android**. No publicar iOS/Desktop. Antes de lanzamiento se exige completar evidencia de release, canary de Crashlytics y limpieza/retención de evidencias locales.

## Alcance cubierto

- `shared/src/**/features/mindboost/**` y `com/mindverse/mindboost/**`.
- Integraciones Android, actuals iOS/Desktop y wiring común necesario.
- Storage local, serialización, scoring, timebase, exports y observabilidad.
- Firebase compartido alcanzable desde flujos revisados.
- Gradle, CI/CD, release y dependencias relevantes.
- Skills/agentes MindBoost y permisos locales de herramientas.
- Evidencias locales ignoradas por Git bajo `validation/`.

Fuera de alcance: pruebas ofensivas externas, mutaciones en producción y contenido/ACL real de consolas cloud.

## Hallazgos priorizados

| ID | Severidad | Confianza | Hallazgo | Evidencia | Estado |
|---|---:|---:|---|---|---|
| MB-S0006-C01 | Low | High | Borrado de cuenta enviaba excepción original a Crashlytics | `SettingsScreen` → `ObservabilityHub` → `MindOSApplication` | Fixed |
| MB-S0006-C02 | Low | High | Publicación social enviaba excepción Firebase original a Crashlytics | `safeErrorLog` → `ObservabilityHub` → `MindOSApplication` | Fixed |
| MB-S0001-C01 | Low/local | High | Permiso wildcard `xargs python3 -c` | `.claude/settings.local.json` | Fixed |
| MB-S0001-C02 | Low/local | High | Permiso wildcard `npm run *` | `.claude/settings.local.json` | Fixed |
| MB-S0097-C01 | Low/local | High | Agentes con herramientas consecuenciales sin boundary explícita | 16 prompts `.claude/agents/mindboost-*.md` | Fixed |
| MB-OPS-001 | Low/local | High | Evidencias QA contenían UID/email y telemetría cognitiva | `validation/` y exportador Speed | Partially fixed; retention manual |

## Cambios aplicados

- `ObservabilitySink` recibe `NonFatalTelemetryEvent`, no `Throwable`.
- Operación y tipo de error se normalizan a tokens ASCII acotados.
- Crashlytics construye `SanitizedNonFatalException` sin causa ni mensaje externo.
- `NonFatalReporter` también recibe el evento minimizado.
- Exports Android/Desktop usan UUID de archivo, contenido pseudonimizado y errores por tipo.
- Tests con canarios prueban que mensajes/causas/UID/modelo/OS no cruzan los sinks.
- Se eliminaron dos permisos shell wildcard y se añadió boundary anti prompt-injection/tool abuse a 16 agentes.

## Validación

- `:shared:desktopTest --tests com.mindos.app.core.logging.ObservabilityHubTest`: PASS.
- `:shared:desktopTest` focalizado en observabilidad y `DebugExportTest`: PASS.
- `:shared:desktopTest` completo: PASS.
- `:shared:detekt`: PASS.
- `:androidApp:compileDebugKotlin`: PASS.
- `scripts/verify-mindos.ps1 -Full`: PASS; incluye secret scan, Functions typecheck/Jest (799 tests), Command Router/Voice y composite Android.
- `functions/npm audit --omit=dev`: PASS, 0 vulnerabilidades; `npm ls uuid --all`: árbol válido en `11.1.1`.

## Riesgos restantes

- No se verificó en un proyecto Crashlytics no productivo la retención/ACL y representación final del evento.
- `validation/` conserva evidencias previas sensibles; no se borraron para preservar evidencia sin autorización específica.
- iOS/Desktop carecen de evidencia suficiente para release seguro equivalente a Android.
- Los 16 agentes conservan Bash/escritura por función; el boundary reduce riesgo, pero least privilege por rol sería superior.
- El árbol de trabajo contiene cambios previos extensos; esta revisión no atribuye ni revierte cambios ajenos.

## Criterios de aceptación pre-release

- [x] Sin Critical/High abiertos en el scope revisado.
- [x] Excepciones externas no cruzan por identidad a Crashlytics.
- [x] Exportación debug no usa UID ni session ID como nombre de archivo.
- [x] Guardrails de agentes y permisos wildcard corregidos.
- [ ] Canary Crashlytics no productivo verificado y archivado.
- [ ] Política de retención/limpieza de `validation/` ejecutada.
- [x] Gates locales completos KMP/Android/Functions en verde en el worktree revisado.
- [ ] Evidencia CI/release aprobada para el commit exacto que se vaya a publicar.
- [ ] Target de release explícito: Android sí; iOS/Desktop no.
