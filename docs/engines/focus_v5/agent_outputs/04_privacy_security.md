# Orden 1 — privacy-security (2026-07-19) — output íntegro (gate S1)

> Integrado en: ADR-FOCUS-DATA (opciones C-06), contrato de datos P2-22, eventos, amenazas, plan §5 S1/S4.
> CORRECCIÓN de la sesión principal a este output: el estándar canónico de eventos SÍ existe —
> `04_EXERCISE_QUALITY_STANDARD.md` §18 (líneas 697-718), 16 eventos, VERIFICADO directamente por la sesión
> principal y por kmp-architect. La premisa "no existe catálogo centralizado" de la sección C queda
> CONTRADICTED; las reglas de minimización/consentimiento de esa sección permanecen válidas.

## A) Resolución C-06 — opciones ADR-FOCUS-DATA (decide propietario+DPO; este doc NO decide)
Conflicto: pack pide raw por-trial vs CLAUDE.md §5 "persistir solo RESÚMENES, nunca trials crudos" (POLICY).
**Hallazgo re-encuadrante:** el repo YA tiene reproducibilidad de FORMA sin raw:
`ReproducibleSessionIdentity` (:19-25 "FORM_REPRODUCTION, not SESSION_REPLAY") + `FormDescriptorV1` (:56-82:
seedHex, rngVersionId, 7 versiones, resolvedDifficulty, plannedTrialCount; digest recomputado :104-184).
Qué recupera (seed+versiones+dificultad): secuencia potencial de estímulos exacta; no-deriva del generador
(generatedPlanDigest); coherencia del descriptor. Qué NO recupera: estímulo real presentado si hubo desviación;
respuestas/latencias del usuario; re-scoring con fórmula nueva; forense trial-a-trial de gaming.
⇒ "reduce pero no cierra": sustituye el raw para "¿el generador hizo lo que decía?", no para "¿qué hizo el usuario?".

### Opción (i) NO-RAW (statu quo, APPROVED hoy)
SessionRecord + ReproducibleSessionIdentity + evidencia agregada facts-only. HABILITA: entrenamiento completo,
auditoría de generador, recovery. BLOQUEA (explícito): forense por-trial, re-scoring retroactivo, auditoría de
anomalía puntual, manifest exacto del pack §12. Storage O(1)/sesión. Erasure ya operativa. Fail-closed ya
implementado (SessionRecordUpsertOutcome). Riesgo residual: resolvedDifficulty ya es vector de inferencia
sensible (KDoc lo admite).

### Opción (ii) JOURNAL MÍNIMO REPLAY-COMPLETE (ADR_REQUIRED)
Schema mínimo campo a campo: trialId+sequenceNo (idempotencia+orden, no wall-clock) · conditionHash ·
plannedOnsetMono · observedOnsetMono: Long? (null=no observable, no inventar) · inputEventMono: Long? +
responseClass enum (nunca coordenadas) · correctnessDerived + qualityFlags.
HABILITA: re-scoring retroactivo (bridge old/new), auditoría por-trial, verificación manifest.
NO da (honesto): forense anti-gaming a prueba de manipulación — journal LOCAL en dispositivo que el usuario
controla, sin firma/sync ⇒ valor real = auditoría de coherencia interna, NO evidencia adversarial-resistente.
Amenazas: huella conductual longitudinal (re-identificación cuasi-biométrica) · tampering · **DoS storage: SIN
cap de claves/tamaño en Secure/IndexedSecureKeyValueStore hoy (GAP real)** · export accidental.
Coste O(trials); recomendación de diseño: UN evento trial_outcome fusionado (~4x menos volumen).
Erasure: namespace nuevo AccountBound + eraser + SubjectRevocationAuthority (mismo patrón stores existentes).
Fail-closed: escritura fallida ⇒ outcome tipado + sesión marcada WITHHELD_TECHNICAL para outcomes
replay-dependientes (nunca "parcialmente journaled = replay-complete").

### Opción (iii) TIERED (ADR_REQUIRED, variante de ii)
Resúmenes por defecto; journal SOLO purpose de medición (fracción acotada por dosis diaria del charter §11.3),
retención corta + purga tras materializar evidencia + ventana de auditoría. Si purpose indeterminable pre-trial
⇒ cae a (i) — fail-closed hacia MENOS recolección.
**Transversal a las 3:** namespace AccountBound + eraser EN EL MISMO CAMBIO (no opcional).

## B) Contrato de datos (P2-22) — mecanismo existente o GAP
| Ítem | Estado |
|---|---|
| Idempotencia | VERIFIED — upsert por sessionId (`SecureSessionRecordStore.kt:113-123`); enqueue por entryId (PayloadJournal) |
| Orden/out-of-order | GAP → sequenceNo en S4 (patrón requireOrderedUnique `FormDescriptorV1.kt:118-123`) |
| Corrupción | VERIFIED — SecureReadResult.Corrupt/UnsupportedVersion + quarantine tipado |
| Cuota/disk-full | **GAP real** — cero caps; imitar MAX_PLANNED_TRIALS=1000 (rechazo tipado, nunca truncar) |
| Backpressure | N/A local; si sync → ADR-DATA-001 |
| Retención | POLICY no-final (PRD §18.3 "≤30d activos/≤90d backups, requiere revisión legal") + GAP mecanismo (sin retentionPolicyVersion ni purga por edad) |
| Cifrado reposo | VERIFIED Android (AES-256-GCM+Keystore); iOS IMPLEMENTED_NOT_VERIFIED (Keychain real, CEO HOLD gates macOS); desktop in-memory |
| Key loss | VERIFIED — KeyInvalidated → quarantine tipado |
| Exportación | VERIFIED patrón DebugExport pseudonimizado; export de USUARIO (PRD §18.3) = GAP preexistente fuera de Focus v2 |
| Borrado | VERIFIED mecanismo (eraser + best-effort-total + revocación triádica + accountDeletion.ts); **namespace Focus v2 NO EXISTE → crear con AccountBound+eraser+CoverageTest en el MISMO cambio** |
| Backup exclusions | VERIFIED hoy (sharedpref dominio completo); re-verificar en S1/S2 que v2 no introduce file-domain nuevo |
| Sync | GAP declarado — local-only hasta ADR-DATA-001; NO reutilizar SyncQueue (dormido, otro dominio) |
| Migración schema | VERIFIED — schemaVersion + readVersioned fail-closed ante versión futura |

## C) Eventos (reglas de minimización; premisa del catálogo corregida arriba)
Puerta única de salida del dispositivo: `ObservabilityHub` — breadcrumb/customKey/NonFatalTelemetryEvent
(operation+errorType sanitizados por toTelemetryToken; sin Throwable/mensaje/causa por diseño).
NUNCA sale del dispositivo: RT/accuracy/contenido por-trial, seed, estímulos, quality-flag detallado, razones
de adaptación, contexto subjetivo (PRD §18.2), scores. Puede salir AGREGADO con consentimiento NUEVO (scope
propio, patrón `AICoachConsent.kt` versionado/revocable/durable): breadcrumbs neutrales de operación, tasas
agregadas de jank/quarantine, NonFatal técnicos.
Los eventos por-trial del pack (trial_planned/stimulus_presented/input_received/trial_evaluated) = núcleo C-06:
solo bajo opción ii/iii; 100% LOCAL, jamás vía ObservabilityHub. `consent_changed` = el primero a implementar.
`trial_planned` posiblemente redundante (el seed re-deriva el plan — no persistir dos veces). Falta en el pack:
breadcrumb `revocation_status_unavailable` (patrón ya existente).

## D) Top-10 amenazas → mitigación (con learnings del repo)
1 éxito silencioso de escritura (2026-07-14) → outcome tipado siempre. 2 revocación fail-open (2026-07-14) →
SubjectRevocationAuthority triádica, test con store de revocación fallando y payload OK. 3 forEach{remove}
abortante (2026-07-14) → bucle removeCatching+firstFailure; test falla clave que PRECEDE. 4 quarantine-DELETE
por read fail-open (2026-07-05) → read fail-closed simétrico con delete. 5 centinela 0L (2026-07-04) → Long?
nullable. 6 namespace sin eraser (2026-06-28/07-11) → mismo commit. 7 **DoS storage sin cap (GAP)** → cap con
rechazo tipado; test N+1. 8 export filtrando trials (2026-06-20) → redactStableIdentifiers + redacción de
contenido. 9 sobre-recolección resolvedDifficulty → escrutar cada campo nuevo. 10 re-identificación por huella
conductual longitudinal (INFERRED) — bajo mientras 100% local; CRÍTICO si se aprueba sync/export → k-anonimato
ANTES de cualquier ADR-DATA-001.

## E) Observabilidad minimizada
Permitido: breadcrumbs neutrales de operación ("secure_storage_quarantined:<code>",
"$operation.missingRecord"), customKey bool/int/enum estructural, NonFatalTelemetryEvent sanitizado — reutilizar
VERBATIM. Prohibido: Throwable crudo, RT/accuracy/contenido, seed/userId/sessionId en claro, modelo/OS como id
estable. Depuración profunda local = patrón DebugExport (debug-gated, pseudonimizado, nombre aleatorio); nunca
enrutar contenido cognitivo por ObservabilityHub.

## Bloqueantes de seguridad
1 CRÍTICO C-06 sin decidir (nada por-trial se construye antes). 2 CRÍTICO namespace v2 + eraser mismo cambio.
3 ALTO sin cap de storage (pre-S4-journal). 4 ALTO ADR-DATA-001 retención/base legal. 5 MEDIO-ALTO modelo de
consentimiento para scopes nuevos (patrón AICoachConsent). 6 MEDIO iOS Keychain CEO HOLD hasta gates macOS.
7 MEDIO k-anonimato antes de sync. 8 BAJO-MEDIO re-verificar backup exclusions en DoD S1/S2. 9 MEDIO no
sobrevender journal local como forense anti-manipulación.
