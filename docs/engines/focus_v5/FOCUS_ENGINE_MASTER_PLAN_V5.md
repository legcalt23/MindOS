# FOCUS_ENGINE_MASTER_PLAN_V5 — Plan maestro reconstruido del motor de Enfoque

```
DOCUMENT_STATUS = DRAFT (candidato; pide aprobación, no la presupone)
PLAN_VERSION = V5 (reconstrucción íntegra ordenada por el CEO; V4 = REOPEN)
EXECUTION_AUTHORIZATION = NOT_GRANTED
IMPLEMENTATION = NOT_STARTED (esta tarea no implementó ni modificó el repositorio)
BASE_SNAPSHOT = 27849450345ef57f96b02f1900afe1251b456292 (Commit F; branch product/mindboost-wip-consolidation-20260717)
CANON = docs/context_pack packRelease 2.0.2 (único canon vivo; adjudicación en BASELINE_EVIDENCE_V5.md §2)
RESEARCH_PACK = FOCUS_ENGINE_RESEARCH_PACK.zip sha256 5fc8e617ef455ea1aa414f40656a43488f988b6808141e6b886600d2d0100cdb (EXTERNAL_UNVERIFIED, anclado a 2.0.0)
INTEGRIDAD = manifiesto SHA-256 portable del bundle en SHA256_MANIFEST_V5.txt (rutas relativas, line endings LF declarados)
PRECEDENCIA = ley/seguridad/privacidad > CEO > 03_Charter > 01_PRD > 09_DecisionLog/ADR > este plan > código existente
NOTA TIPOGRÁFICA [R6-V5 P3-1]: "V0".."V8" a secas SIEMPRE denotan gates científicos; la versión de este
documento se escribe "Plan V5"/"V5 (plan)" — nunca "V5" a secas en contexto de gates.
```

Artefactos del bundle V5 (autocontenidos, coherentes entre sí):
`FOCUS_ENGINE_MASTER_PLAN_V5.md` (este) · `BASELINE_EVIDENCE_V5.md` · `REQUIREMENTS_MATRIX_V5.csv` ·
`EXERCISE_PORTFOLIO_V5.md` · `ADR_REGISTER_V5.md` · `GATE_CATALOG_V5.md` · `REDTEAM_TEST_MATRIX_V5.md` ·
`REVIEWS_V5.md` · `INPUTS_SHA256.txt` · `SHA256_MANIFEST_V5.txt`

---

## §0 — Veredicto del plan V4 (artefacto 1)

**`REOPEN` — aceptado sin reservas.** El V4 (hash-set H3, 7/7 SHA-256 reconciliados con el bundle del CEO,
ver BASELINE §3) era un candidato honesto en sus límites pero NO ejecutable ni promovible, por los defectos
que el mandato enumera y esta reconstrucción corrige (tabla §2). Los más graves en mi autocrítica: (a) trató
la agrupación SUSTAIN/CONSTRAIN/RESTRAIN del research pack como organización de facto del constructo cuando
las cinco subhabilidades del PRD son lo aprobado; (b) dio autoridad efectiva a requisitos del pack por el mero
hecho de estar en el pack (61 `RECOMMENDED` tratados operativamente como plan); (c) su vocabulario de gates
mezclaba escalera científica y gates técnicos (crosswalk G-S2→V3) y contenía referencias huérfanas
(G-V1/G-V2/G-V3); (d) su integridad descansaba en CRC32 + una ruta absoluta no portable; (e) H3 modificó
artefactos DESPUÉS de la última lente psicométrica y del diff-audit, dejando la última revisión antes del
último cambio; (f) el purpose 6→8 vía proyección a UNKNOWN creaba doble autoridad y usaba UNKNOWN para "no
aplicable"; (g) S2 se llamaba end-to-end difiriendo lifecycle/recovery/a11y/paridad; (h) delegaba al CEO la
elección sobre el score v1 en vez de fijar la acción segura por defecto. V4 queda como insumo trazado
(hashes en INPUTS_SHA256.txt), sin autoridad; V3/ENG-01 no se marca SUPERSEDED por este documento.

## §1 — Baseline y evidencia real del repositorio (artefacto 2)

Evidencia completa y comandos en **`BASELINE_EVIDENCE_V5.md`** (parte inseparable de este plan). Hechos
gobernantes:

1. **Canon real = 2.0.2** en `docs/context_pack/` (commit `81e263c9`); 2.0.0 no byte-recuperable; diff de
   autoridad declarado con clasificación (VERIFIED/REPORTED/NOT_VERIFIED) en BASELINE §2. Caveat CAVEAT-CANON:
   el research pack cita un snapshot 2.0.0 no verificable ⇒ toda dependencia científica del pack se re-verifica
   contra 2.0.2 antes de promover claims (>C0).
2. **P0 operativo CONFIRMADO**: el score v1 manipulable está VISIBLE (`FocusSummaryScreen.kt:94-96`,
   `FocusProgressScreen` vía `FocusRootScreen.kt:107-110`); NO alimenta perfil (gate D1 fail-closed
   `FocusProfileSource.kt:55-61`) ni claims. MB-ENG-0007/0008 = P0 OPEN en el ledger. Contención: slice S0.
3. **CONTRADICTED de plataforma**: TEC-002 (canon APPROVED) exige Android 8/API 26; el repo compila
   `minSdk=29`. Disposición en S5 (decisión CEO: bajar minSdk o SUPERSEDED). iOS 15.0 alineado.
4. **focus2 NO existe en HEAD**; solo en el commit rechazado `89325271` (no-base, no-autoridad).
5. **Gates Gradle NOT_RUN** en esta sesión (directiva de entorno: daemon del IDE); el baseline verde NO está
   demostrado en el HEAD actual (ledger `integration-baseline` = fallo no atribuido en HEAD anterior).
   Primer exit-gate de R0: el CEO ejecuta y aporta outputs.
6. **iOS = BLOCKED_IOS**; lane 0 € existente `ios-build.yml` (macos-14, compile-only, ejecución no verificada).
7. Ledger LEÍDO; su actualización = mutación ⇒ PENDING_BY_OWNER_DIRECTIVE.

## §2 — Hallazgo → corrección incorporada → ubicación (artefacto 3)

| # | Hallazgo del mandato | Corrección incorporada | Ubicación |
|---|---|---|---|
| P0 | Score v1 manipulable visible | P0 operativo confirmado por inspección; especificación de ocultación REVERSIBLE vía FeatureFlags existente, historial preservado retirado; NO implementada en esta tarea | BASELINE §6 · §5/S0 |
| P1-1 | Procedencia: el pack no da autoridad | CSV V5: columnas `canonical_source` (doc§/decisión del canon), `normative_status` re-derivado (8 estados), `secondary_evidence`; `pack_status` conservado aparte como dato del pack; ningún RECOMMENDED tratado como aprobado | REQUIREMENTS_MATRIX_V5.csv · §6 |
| P1-2 | H3 cambió artefactos tras las lentes | Pipeline V5: lentes de orden 1-2 → integración → test-engineer → correcciones → FREEZE con manifiesto → diff-auditor sobre el bundle FINAL exacto; si corrige algo, se re-hash y se repite la lente afectada — la última revisión SIEMPRE posterior a la última modificación | REVIEWS_V5.md · §14 |
| P1-3 | CRC32/ruta absoluta no portable | Manifiesto SHA-256 con rutas RELATIVAS + política de line endings (LF, declarada y verificada); sin scripts con rutas absolutas | SHA256_MANIFEST_V5.txt |
| P1-4 | Gates huérfanos; G-S2→V3 | Catálogo único: escalera científica V0–V8 (semántica del pack sin debilitar) + gates técnicos `T-*` por slice, vocabularios DISJUNTOS sin crosswalk; cero referencias sin definición (validado mecánicamente antes del freeze) | GATE_CATALOG_V5.md |
| P1-5 | Exposición C0 sin V1/V2 | Tracers = `INTERNAL_TECHNICAL_TRACER` (internos, sin usuarios externos) hasta V1 técnico (S5) y V2 humano (S6); ninguna exposición externa antes | §5/S3-S6 · catálogo |
| P1-6 | Raw vs "solo resúmenes" | ADR-FOCUS-DATA con opciones cerradas y decisión propietario+DPO ANTES de cualquier escritura; si se deniega el mínimo replay-complete ⇒ outcomes dependientes BLOCKED explícitos | ADR_REGISTER_V5.md · §5/S1 |
| P1-7 | Constructos: 5 aprobadas, no facetas | Registro de constructo = las CINCO subhabilidades del PRD; SUSTAIN/CONSTRAIN/RESTRAIN = hipótesis rivales del harness (M-modelos), sin tipo ni UI propios | §3 · EXERCISE_PORTFOLIO_V5.md · ADR-FOCUS-CONSTRUCT |
| P1-8 | Purpose 6→8 no es autoridad única | ADR-FOCUS-PURPOSE: UNA autoridad; "no aplicable al perfil" = valor EXPLÍCITO, jamás UNKNOWN | ADR_REGISTER_V5.md · §5/S1 |
| P1-9 | 7 vs 8 estados | Contrato UX = EXACTAMENTE 8 estados incl. `RETIRED_VERSION`; toda referencia a "7" eliminada del bundle | §5/S1 · catálogo |
| P1-10 | Orden científico mezclado | Slices separadas: simulación (S8-pre), alpha (S6), recogida repetida (S8), V3+ADR psicométrico (S9), beta (S10), RCT/transfer/invariancia (S11); NINGUNA fórmula elegida antes de datos+ADR; scorers rivales solo en sombra | §5/S8-S11 |
| P1-11 | Catálogo insuficiente (E04/E05) | Matriz de ≥15 ejercicios con dossier por fila (subhabilidad, mecanismo, confounds, dificultad, métricas, falsificador, modalidad…) | EXERCISE_PORTFOLIO_V5.md · §5/S2,S7 |
| P1-12 | No delegar el score v1 al CEO | Acción por defecto FIJADA: ocultar reversiblemente + preservar historial RETIRADO; no se relabela, no se "arregla la cifra"; el plan no pregunta | §5/S0 |
| P1-13 | Ejecutabilidad | Cada slice con los 15 campos (owner único, agentes, archivos, comandos, entry/exit gates, rollback…); baseline reproducible con comandos y estado NOT_RUN honesto | §5 · BASELINE §7 |
| P1-14 | "End-to-end" falso | El vertical del Tracer A (S3) INCLUYE tutorial, práctica, sesión, cierre, offline, pause/resume/exit, process death y a11y; la paridad se verifica en S5 y hasta entonces el estado máximo es INTERNAL_TECHNICAL_TRACER | §5/S3,S5 |
| P1-15 | iOS bloquea | Regla de catálogo: `BLOCKED_IOS` impide TRAINING_READY, alpha externa y lanzamiento; vía 0 € = `ios-build.yml` (compile) + Mac para el resto | GATE_CATALOG_V5.md · §5/S5 |
| P1-16 | A11y por plataforma | Requisitos NATIVOS: Android targets interactivos ≥48dp (Material/Accessibility scanner); iOS ≥44pt (HIG); nunca un "44pt universal aspiracional" | §5/S3 · REDTEAM matrix |
| P1-17 | Timing API26/iOS15 | Programa de timing del orden-2 (dispositivos reales, 60/120 Hz, frame pacing, jank, render marker, modalidad); precisión de timestamp ≠ exactitud; RT no fiable EXCLUIDO de evidencia oficial; CONTRADICTED TEC-002/minSdk29 planteado a decisión | ADR-FOCUS-TIMING · §5/S5 · BASELINE §4 |
| P2-18 | Scanner de imports insuficiente | Frontera v2 = graph/signature/API tests contra dependencias legacy directas Y transitivas (inventario en output kmp-architect), no un scanner de texto | §4 · §5/S1 |
| P2-19 | SessionStatus global | Consumer inventory completo + ADR-FOCUS-SESSION-STATE (runtime local vs status global) antes de tocar el enum | ADR_REGISTER_V5.md |
| P2-20 | 13 vs 16 eventos | Tabla de reconciliación evento a evento (aplica/no aplica/por qué) contra el estándar canónico | §5/S1 · ADR-FOCUS-DATA anexo |
| P2-21 | CSV desnormalizado | `pack_priority` separado de `severity` propia; gates/owners/deps/slices normalizados al vocabulario V5 | REQUIREMENTS_MATRIX_V5.csv |
| P2-22 | Contrato de datos incompleto | Contrato completo (idempotencia…migración, 14 ejes) mapeado a mecanismos existentes del repo o GAP+slice | ADR-FOCUS-DATA anexo · §5/S1,S5 |
| P2-23 | Programa de rendimiento | Protocolo medible por dispositivo/Hz/modalidad con presupuestos y gates | §5/S5 · orden-2 |
| P2-24 | Un solo tracer sesga | DOS tracers con mecánica y confounds DISTINTOS (S3/S4), seleccionados por criterios re-derivados | EXERCISE_PORTFOLIO_V5.md §C · §5/S3-S4 |
| P3-25 | Fences/hashes/refs rotos | Saneo integral + checks mecánicos pre-freeze (paridad de fences, enlaces internos, hashes vigentes) | freeze §14 · manifiesto |

## §3 — Objetivo, decisiones cerradas y constructo aprobado

Objetivo (del mandato, íntegro): el mejor motor de entrenamiento de enfoque posible para Android e iOS como
aspiración interna, jamás como claim público; prioridades 1-11 del mandato asumidas en ese orden (eficacia
plausible → validez → transferencia → seguridad/privacidad → honestidad → paridad → a11y/equidad →
personalización ética → mantenibilidad/reversibilidad → 0 € → velocidad).

**Decisiones cerradas aplicadas sin re-preguntar** (ancla canónica): 5 subhabilidades del PRD vigentes
[01_PRD + CAT-002]; ≥15 ejercicios excelentes por motor [CAT-003]; training ≠ measurement [SES-001];
tutorial/familiarización sin score [SES-001/CAT-006]; `null != 0` [SCO-004]; sin score público v2 hasta datos
humanos+ADR+gate [SCO-006/TEC-011 + mandato]; score v1 se oculta reversible, no se relabela [mandato P1-12];
8 estados UX incl. RETIRED_VERSION [mandato]; alpha ≈20 sin claims de eficacia/normas [LCH-001/LCH-004];
Free offline sin IA generativa [BUS-002/TEC-004]; contexto subjetivo/pago/XP/racha/volumen jamás inflan
capacidad [SCO-003/CTX-003]; Android+iOS [VIS-003/TEC-003]; 0 € [BUS-010]; raw denegado ⇒ outcomes replay
BLOCKED [mandato]; iOS no verificable ⇒ BLOCKED_IOS [mandato].

**Constructo aprobado (ADR-FOCUS-CONSTRUCT resume — leer AMBOS párrafos, no solo este):** las cinco
subhabilidades canónicas del motor Enfoque son **atención sostenida, atención selectiva, inhibición de
distractores, control atencional y estabilidad bajo tiempo** (fuente: 01_PRD §Enfoque; organización pedagógica
per CAT-002 — no cinco factores latentes demostrados). **Precisión obligatoria (R6-V5 P1-5): las cinco son
categorías de NAVEGACIÓN/producto. Solo TRES (sostenida, selectiva, inhibición de distractores) son candidatas
HYPOTHESIS a subscore futuro post-V3; control atencional (existencia CONFLICTED) y estabilidad bajo tiempo
(propiedad de sostenida) NO tendrán subscore en ninguna versión salvo evidencia nueva que reabra el ADR —
detalle en ADR_REGISTER_V5 §ADR-FOCUS-CONSTRUCT.** SUSTAIN/CONSTRAIN/RESTRAIN (research pack, EXTERNAL_UNVERIFIED) quedan como **hipótesis rivales
de estructura latente** dentro del harness de modelos rivales de S9: no generan tipos, ni UI, ni score, ni
copy; solo análisis. Falsación estructural (qué datos distinguirían 5-aprobadas vs 3-facetas vs 1-factor) =
S9 con datos repetidos reales; hasta entonces TODA estructura es HYPOTHESIS.

## §4 — Arquitectura objetivo y frontera legacy

Principios fijos (el detalle por componente se apoya en el evidence map del kmp-architect, REVIEWS_V5.md):

1. **`focus2` clean-room en commonMain** (`features/mindboost/focus2/`), vertical, determinista, versionado y
   recuperable. Reutiliza SOLO autoridades transversales aprobadas del repo (core/scoring: ScoreStateMachine ·
   MinSampleGate · BankIntegrityAuthority · DetectorCoverage; core/session + evidence facts-only S6R3-001;
   core/timing; storage seguro; DeterministicRng/SessionSeed) — jamás los tipos v1 de focus contaminados.
2. **Frontera legacy MECÁNICA por análisis de dependencias, no por scanner de texto** (P2-18): un test JVM de
   frontera que recorre el grafo de dependencias (directas Y transitivas) de `focus2.*` y FALLA si alcanza
   símbolos del conjunto legacy prohibido (v1 `focus.domain.personalization.*`, `ArchetypeInference`,
   `PersonalizedSessionScorer`, `FocusScore*`, …inventario exacto del kmp-architect), complementado por
   signature-tests de API pública (la superficie de focus2 no puede exponer tipos con la forma de la moneda
   del agregador — lección evidenceBoundary). El grep CI queda como defensa secundaria, nunca la garantía.
3. **Score v1: contención, no reforma.** v1 permanece jugable TRAINING_ONLY con su score OCULTO tras flag
   (S0); ninguna slice de v2 "arregla" la fórmula v1 (RETIRED, historial preservado).
4. **Determinismo y reproducibilidad**: toda sesión v2 sella `(SessionSeed(value, RngVersion), difficulty,
   spec/forma versionada, purpose)` ANTES del primer trial (patrón DeterministicRng existente); dificultad
   online (staircase) emite esquema + tracker con registro por-trial del parámetro resuelto.
5. **Paridad de constructo**: una mecánica solo entra al carril de medición si su timing class y su modalidad
   de respuesta son equivalentes en Android e iOS o el puente está aprobado por ADR-SCI-008 (hasta entonces:
   entrenable ambas, medible ninguna o una sola con etiqueta de no-comparabilidad).
6. **Fail-closed en cadena**: purpose ausente ⇒ sesión inválida para evidencia (práctica conservada); journal
   denegado ⇒ outcomes replay BLOCKED; timing no fiable ⇒ RT excluido de evidencia oficial; iOS no verificado
   ⇒ BLOCKED_IOS; flag mal configurada ⇒ deniega (default seguro).

## §5 — Slices R0–S11 (artefacto 5; cada slice con los 15 campos)

Convenciones: gates definidos en GATE_CATALOG_V5.md; ADRs en ADR_REGISTER_V5.md; tests/adversarios en
REDTEAM_TEST_MATRIX_V5.md; comandos de gate = tabla §3 de CLAUDE.md del repo (los ejecuta el CEO mientras el
IDE posea el daemon). "Observabilidad" siempre = local minimizada (breadcrumbs neutrales, patrón
ObservabilityHub; jamás datos cognitivos). Ninguna slice se inicia sin `T-DR` global + autorización expresa.

---
### R0 — Freeze de autoridad y baseline
- **Outcome:** canon y estado real del repo reproducibles; toda afirmación posterior anclada a hash.
- **Estado alcanzable:** BASELINE_FROZEN.
- **Entry gates:** ninguno (primera slice).
- **Dependencias:** acceso read-only al repo; CEO disponible para correr gates.
- **Archivos/componentes:** NINGUNO nuevo en el repo. Artefactos: BASELINE_EVIDENCE_V5.md + INPUTS_SHA256.txt
  (ya producidos en esta reconstrucción) + outputs de gates aportados por el CEO.
- **Owner único:** sesión `/mb`.
- **Agent routing:** kmp-architect (evidence map — HECHO, agent_outputs/01).
- **Tareas exactas:** (1) snapshot worktree/HEAD/submódulos [HECHO]; (2) adjudicar canon 2.0.0/2.0.2 [HECHO];
  (3) hash de fuentes y bundle [HECHO]; (4) inspección P0 score v1 [HECHO — P0 operativo]; (5) **CEO ejecuta**:
  `.\gradlew.bat :shared:desktopTest`, `:shared:detekt`, `:shared:compileKotlinDesktop`,
  `:shared:compileAndroidMain`, `:androidApp:compileDebugKotlin` y aporta outputs; (6) opcional 0€: dispatch
  de `ios-build.yml` si el remoto está configurado.
- **Tests/simulaciones:** N/A (slice de evidencia).
- **Evidencia humana:** N/A.
- **Exit gates:** `T-BASE` (los 5 outputs reales; si desktopTest/detekt no están verdes, se atribuye y
  resuelve ANTES de S0 — no se construye sobre baseline rojo no atribuido).
- **Observabilidad:** N/A.
- **Migración:** N/A. — **Rollback:** N/A (read-only).
- **Failure behavior:** cualquier gate irreproducible ⇒ estado BLOCKED_REPO_BASELINE y el plan se detiene ahí.

---
### S0 — Contención del legado (score v1)
- **Outcome:** el score manipulable v1 deja de ser visible (reversible) y deja de influir en sesiones futuras;
  historial intacto; NADA de esto ejecutado sin T-DR.
- **Estado alcanzable:** V1_CONTAINED.
- **Entry gates:** `T-BASE` + `T-DR`.
- **Dependencias:** MB-ENG-0007/0008 (ledger); mecanismo FeatureFlags existente.
- **Archivos/componentes esperados:** `core/config/FeatureFlags.kt` (+`focusScoreSurfacesEnabled=false` en las
  TRES impls: Default/Static/RemoteConfig — learning 2026-06-22); gateo de las TRES superficies:
  `FocusHomeScreen.kt` chip FOCO (:246-253) y link "Ver progreso" (:184-189), `FocusProgressScreen` completa,
  `FocusSummaryScreen.kt` PersonalizedScoreCard (:94-99); corte de la 2ª puerta: `FocusSessionStateHolder.kt:198`
  (startingDifficulty por arquetipo → EASIEST/última calibrada legítima) y `:203-205` (MicroPauseGate neutro);
  test-oráculo `FocusV1ScoreAdversarialTest` (ROJO documentado — G del psychometrician); ledger actualizado.
- **Owner único:** cognitive-engine-dev (implementación futura); spec = esta slice.
- **Agent routing:** psychometrician (oráculo — HECHO) · kmp-architect (wiring — HECHO) · diff-auditor al cierre.
- **Tareas exactas:** (1) añadir flag OFF a las 3 impls; (2) gatear los 3 composables (el resto de
  FocusSummaryScreen — métricas por-sesión con caption — NO se toca); (3) neutralizar las rutas (b) sesgo
  arquetipo y (c) micro-pausa y (d) ponderación del score del inventario de 5 rutas (agent_outputs/01 §C) —
  la ruta (a) startingDifficulty ya persistido se conserva (dato legítimo del usuario) y la (e) AdaptiveEngine
  legacy queda como está (TRAINING_ONLY); (4) aterrizar el oráculo A1-A6 como test que HOY falla A1 contra el
  scorer v1 (documenta el porqué del hide) — se marca `@Ignore`-with-reason o se invierte como regresión
  "v1 sigue oculto"; (5) copy neutral en el hueco del score ("práctica registrada"); (6) ledger: MB-ENG-0007/
  0008 → MITIGATED (no CLOSED: la fórmula sigue existiendo oculta).
- **Tests/simulaciones:** oráculo A1-A6; test "flag OFF ⇒ 0 superficies numéricas Focus"; test "flag ON ⇒
  restaura las 3"; test "persistencia intacta con flag OFF" (round-trip de SessionResult/perfil).
- **Evidencia humana:** N/A.
- **Exit gates:** `T-S0` + `:shared:desktopTest`+`detekt`+`compileAndroidMain` verdes (CEO).
- **Observabilidad:** breadcrumb neutro `focus_score_surfaces_hidden` una vez por sesión de app.
- **Migración:** NINGUNA (no se toca ningún dato persistido; el flag es de solo-render).
- **Rollback:** flip del flag a ON restaura todo; el corte de rutas arquetipo va tras el MISMO flag.
- **Failure behavior:** si el gateo rompe otra pantalla, el flag OFF es inerte por diseño (default seguro);
  cualquier fallo ⇒ no ship, v1 sigue visible y el ledger lo registra como intento fallido.

---
### S1 — Contratos y ADR
- **Outcome:** purpose, estados, evidencia, datos, lifecycle y migración con UNA autoridad cada uno,
  inequívocos y fail-closed; C-06 DECIDIDO.
- **Estado alcanzable:** CONTRACTS_SEALED.
- **Entry gates:** `T-S0` (spec) + `T-DR`.
- **Dependencias:** decisiones CEO de: ADR-FOCUS-PURPOSE (opción), ADR-FOCUS-DATA (i/ii/iii + retención),
  ADR-FOCUS-SESSION-STATE, ADR-FOCUS-MIGRATION, ADR-FOCUS-PLATFORM-MIN; consumer-inventory ya hecho
  (agent_outputs/01 §D, 03 §0).
- **Archivos/componentes esperados (si se ratifica la recomendación):** extensión de
  `SessionMeasurementPurpose` (+TUTORIAL/FAMILIARIZATION/RESEARCH_ONLY) + `ProfileEligibilityDisposition`
  sealed 3-vías en `ProfileMeasurementEligibility`; retirada/renombrado de
  `CognitiveSessionEnums.SessionPurpose.mayContributeToProfile()` (cerrar la grieta §0); estado runtime
  focus2-local (sealed 8 alineado al estándar de 16 eventos); namespace `mindboost_focus2` en
  `MindBoostStorageNamespaces.AccountBound` + eraser en `AppGraph` + `AccountBoundErasureCoverageTest`
  (MISMO cambio — learning 2026-06-28); esquema de consentimiento de scopes (patrón `AICoachConsent`);
  tabla de reconciliación de eventos 13-pack↔16-canon (sellada en un objeto versionado).
- **Owner único:** kmp-architect.
- **Agent routing:** psychometrician + privacy-security (co-revisión de contratos) · diff-auditor al cierre.
- **Tareas exactas:** (1) presentar los 5+1 ADRs al CEO con las opciones del registro (UNA pregunta por ADR);
  (2) registrar decisiones en 09 §22; (3) implementar los contratos ratificados (tipos + tests, SIN UI);
  (4) reconciliación de eventos: session_created→SessionStarted · session_committed→ResultPersisted ·
  tutorial_result→TutorialCompleted · stimulus_presented→TrialPresented · input_received→ResponseSubmitted
  (+ResponseStarted que el pack omite) · trial_evaluated→TrialResolved · adaptation_decided→DifficultyChanged ·
  lifecycle_changed→SessionPaused/Resumed/Interrupted (el canon los separa) · quality_flagged→
  SessionInvalidated (parcial: flag≠invalidación, ambos) · block_started/ended→granularidad extra local
  permitida · trial_planned→REDUNDANTE (el seed re-deriva el plan; no persistir dos veces) ·
  score_materialized→local-only D1 · consent_changed→PRIMERO en implementarse · ExerciseOpened/TutorialStarted/
  ResultSynced→añadidos del canon (ResultSynced N/A mientras local-only); (5) DoS-cap del storage (rechazo
  tipado) si ADR-DATA eligió (ii)/(iii).
- **Tests/simulaciones:** disposition 3-vías exhaustivo (incl. enum desconocido→Indeterminate); purpose sellado
  pre-trial (cambio mid-session ⇒ Indeterminate, práctica conservada); coverage-test del eraser; round-trip de
  consentimiento; propiedad: NINGÚN camino de código responde elegibilidad desde el 6v.
- **Evidencia humana:** N/A.
- **Exit gates:** `T-S1` + gates Gradle verdes (CEO).
- **Observabilidad:** breadcrumbs de quarantine/revocación ya estándar.
- **Migración:** aditiva (miembros de enum nuevos; reader antiguo→Indeterminate). Registrada en ADR-FOCUS-MIGRATION.
- **Rollback:** dejar de emitir los miembros nuevos; tipos v2 borrables; namespace v2 vacío se elimina con su eraser.
- **Failure behavior:** ADR sin decidir ⇒ la slice NO avanza en ese eje (p. ej. sin ADR-DATA no se diseña
  NINGUNA escritura por-trial); jamás un default implícito.

---
### S2 — Modelo científico y portfolio
- **Outcome:** operacionalización de las 5 subhabilidades + matriz de ≥15 ejercicios + par de tracers,
  ratificados.
- **Estado alcanzable:** PORTFOLIO_APPROVED.
- **Entry gates:** `T-S1`.
- **Dependencias:** dictamen efficacy (HECHO, agent_outputs/02); decisión CEO de ADR-FOCUS-CONSTRUCT.
- **Archivos/componentes esperados:** EXERCISE_PORTFOLIO_V5.md (producido); registro de constructo v2 en código
  (S3 lo materializa): 5 subhabilidades como etiquetas + `constructModelVersion`; dossiers por ejercicio
  (docs/mindboost_exercise_design/ formato existente) para los 2 tracers como mínimo.
- **Owner único:** efficacy-guardian (dictamen) / CEO (ratificación).
- **Agent routing:** efficacy-guardian [HECHO] · psychometrician (métricas por ejercicio) · CEO decide.
- **Tareas exactas:** (1) ratificar portfolio de 18 con estados C0 (ACCEPT/PROTOTYPE/RESEARCH_ONLY);
  (2) ratificar tracers FOC2-E07 + FOC2-E01; (3) ratificar reglas transversales (difference-scores retirados,
  RT secundario, no-léxico, sondas retenidas pre-entrenamiento); (4) escribir dossier completo de los 2 tracers
  (formato Quality Standard §20); **(5) [R6-V5 P1-2] SIMULACIÓN PREVIA DEL SCORER CANDIDATO: antes de
  comprometer el motor en S3, implementar en JVM puro (sin app) el scorer descriptivo candidato
  (exactitud/d′/criterio POR CONDICIÓN, CON corrección de azar — el defecto v1 era exactamente accuracy SIN
  chance-floor, DefaultMetricsEngine.kt:43-57) y correrlo contra los 5 perfiles del oráculo (HONEST, EXPERT,
  MASHER, WAITER, SIDE_BIAS): si A1-A6 no pasan EN SIMULACIÓN, la fórmula candidata se corrige AQUÍ, no en S3.**
- **Tests/simulaciones:** la simulación (5) con sus 6 aserciones; los falsificadores por ejercicio en
  REDTEAM_TEST_MATRIX_V5.md.
- **Evidencia humana:** N/A (alpha NO valida constructos — LCH-004).
- **Exit gates:** revisión científica registrada + selección de tracers en 09 §22.
- **Observabilidad/Migración/Rollback:** N/A (documental). **Failure behavior:** sin ratificación ⇒ S3 no arranca.

---
### S3 — Tracer A interno (FOC2-E07 Filtro vectorial)
- **Outcome:** vertical training-only COMPLETO de una mecánica de elección: tutorial → familiarización →
  práctica → sesión → cierre, offline, pause/resume/exit, process death, a11y, feedback C0 neutral.
- **Estado alcanzable (máximo):** `INTERNAL_TECHNICAL_TRACER` (sin usuarios externos).
- **Entry gates:** S2 ratificada + `T-DR` de la slice.
- **Dependencias:** contratos S1 (purpose sellado, estado runtime, namespace); generador E2 v1 reutilizable
  SOLO como generación (H6); frontera de dependencias.
- **Archivos/componentes esperados:** paquete `features/mindboost/focus2/` nuevo: `domain/` (motor de sesión
  determinista: SessionSeed+RngVersion sellados pre-trial; FormDescriptor propio; clasificación por condición;
  staircase de dificultad con registro por-trial del parámetro resuelto), `presentation/` (state holder NO
  god-object: reducer puro + efectos; pantalla Compose con targets ≥48dp Android/≥44pt iOS, TalkBack/VoiceOver
  labels, sin animación decorativa), `data/` (repos sobre `mindboost_focus2`; write-ahead vía
  SessionRecorder/RecoveryScanner transversales), tests de frontera (`Focus2DependencyBoundaryTest` por grafo
  de deps + signature-test) y golden de determinismo; eventos locales de los 16 aplicables.
- **Owner único:** cognitive-engine-dev.
- **Agent routing:** cognitive-engine-dev (motor) → compose-ui-dev (pantallas) → test-engineer (suite) →
  efficacy-guardian + diff-auditor (gate de salida); privacy-security si toca datos.
- **Tareas exactas:** (1) tipos de dominio del tracer (trial/condición/bloque/forma); (2) generador v2 (puede
  delegar en E2FiltroGenerator para la GENERACIÓN de estímulos, con corrección del isTarget incondicional si
  aplica al render); (3) scoring DESCRIPTIVO por condición (scorer A; exactitud/intrusiones/d′ — CERO
  difference-score, CERO RT en el feedback); (4) runtime con purpose sellado + write-ahead + recovery +
  process-death restore fail-closed (9 razones tipadas, patrón DefaultFocusEngine); (5) tutorial +
  familiarización (purpose propios, jamás evidencia); (6) UI a11y-first — incluye [R6-V5]: captura del flag
  de screen-reader activo (RT-crítico excluido de oficial con a11y activa; accuracy y práctica ÍNTEGRAS —
  REDTEAM #33) y modalidad secuencial para switch-access en toda mecánica choice (modalidad DISTINTA
  registrada — REDTEAM #34); (7) suite T-ADV del tracer (con perfil EXPERT además de HONEST — REDTEAM #36);
  (8) eventos locales; (9) [R6-V5] property tests de reloj: wall-clock ±13h no altera nada (todo monotónico),
  seed SIEMPRE de SeedSource persistida en el descriptor pre-trial (REDTEAM #26), y round-trip del estado del
  reducer a mitad de ventana (rotación/recomposición — REDTEAM #22).
- **Tests/simulaciones:** golden de determinismo (misma seed ⇒ mismo plan byte-igual; mutación de versión ⇒
  restore rechazado); T-ADV A1-A6 sobre el scorer descriptivo; process-death en cada fase (STARTED/mid-block/
  pre-seal); offline completo; a11y semántica (labels, targets); frontera (grafo+firmas); property: purpose
  no sellado ⇒ sesión inválida para evidencia y práctica conservada.
- **Evidencia humana:** SOLO el CEO/entorno interno (dogfooding técnico); NADA externo.
- **Exit gates:** `T-S3` + `T-ADV` + gates Gradle verdes.
- **Observabilidad:** breadcrumbs neutros de lifecycle (started/interrupted/recovered) sin payload.
- **Migración:** namespace nuevo v2 (S1); schemaVersion en todo DTO; sin tocar datos v1.
- **Rollback:** flag `focus2Enabled` OFF (default) — el hub no muestra el tracer; paquete borrable.
- **Failure behavior:** cualquier gate rojo ⇒ el tracer no se monta en el hub; si el write-ahead falla ⇒
  outcome tipado y la sesión no cuenta como evidencia (práctica local visible con disclaimer).

---
### S4 — Tracer B interno (FOC2-E01 Centinela)
- **Outcome:** segunda mecánica (vigilancia continua) sobre EL MISMO motor/eventos/dificultad, demostrando
  que nada está acoplado al Tracer A.
- **Estado alcanzable:** `INTERNAL_TECHNICAL_TRACER` ×2 + ENGINE_GENERALIZED.
- **Entry gates:** `T-S3`.
- **Dependencias:** motor v2 de S3; generador E1 v1 (generación).
- **Archivos/componentes esperados:** SOLO módulos del ejercicio B (definición, generador-wrapper, scorer
  descriptivo de vigilancia d′/criterio/FA, renderer single-tap); CERO cambios estructurales al motor — si el
  motor NECESITA cambios, se registra cada uno como hallazgo de acoplamiento (ese es el punto del tracer B).
- **Owner único:** cognitive-engine-dev.
- **Agent routing:** como S3; test-engineer audita el DIFF del motor (idealmente vacío).
- **Tareas exactas:** (1) definición+forma del tracer B (ISI impredecibles, targets raros, ventana de
  respuesta opcional); (2) scorer descriptivo SUSTAIN (d′/criterio/omisiones/anticipaciones; sin RT en
  feedback); (3) T-ADV B (adversarios PROPIOS: rítmico/periodicidad, esperar, criterio-colapsado);
  (4) informe de acoplamiento (lista de cambios que B exigió al motor, cada uno justificado).
- **Tests/simulaciones:** los de S3 aplicados a B + informe de acoplamiento con presupuesto (si >N cambios
  estructurales, el motor se revisa antes de S5); T-ADV B.
- **Evidencia humana:** interna.
- **Exit gates:** `T-S4` + `T-ADV`(B) + gates verdes.
- **Observabilidad/Migración/Rollback/Failure:** como S3 (mismo flag, mismo namespace).

---
### S5 — V1 técnico y paridad
- **Outcome:** los dos tracers verificados en Android E iOS: datos, rendimiento, lifecycle, migración,
  export/delete, timing, red-team técnico.
- **Estado alcanzable:** V1_PASSED → los tracers pueden aspirar a `TRAINING_READY`.
- **Entry gates:** `T-S4`.
- **Dependencias:** decisión ADR-FOCUS-PLATFORM-MIN (API 26 vs 29); protocolo de timing del orden-2 (pendiente
  de relanzamiento — ver REVIEWS_V5); acceso a Mac o CI para iOS; inventario de dispositivos del CEO.
- **Archivos/componentes esperados:** derivación de clase T en sesión (FrameStats + fuente de timestamps →
  clase persistida con la sesión); presupuestos de rendimiento como tests/benchmarks donde sea posible;
  fixes de paridad iOS que surjan; `ios-build.yml` en verde + tests en simulador.
- **Owner único:** mindboost-mobile-performance (protocolo) / kmp-architect (paridad).
- **Agent routing:** mobile-performance → kmp-architect → test-engineer → diff-auditor + privacy-security
  (export/delete) al cierre.
- **Tareas exactas:** (0) **[R6-V5 P2-2, PRERREQUISITO] arreglar C.1: detección de Hz real por segmento
  (`fromFrameTimes(refreshHz=)` ya existe; falta leer el Hz del display) ANTES de correr el protocolo en
  cualquier dispositivo ≠60 Hz — con test del mismo stream clasificado distinto a 60 vs 120 (REDTEAM #19)**;
  (1) ejecutar el protocolo de timing por dispositivo del inventario; (2) derivación de
  clase T en sesión + exclusión de segmentos degradados (proporcional, no binaria); (3) paridad: iOS compila
  (CI) + suite en simulador + pasada manual en iPhone del CEO; (4) verificación de export/delete (eraser +
  coverage + revocación); (5) red-team técnico (matriz REDTEAM: process death, reloj civil/DST, schema
  antiguo, permiso revocado, flag mal configurada, disk-full…); (6) presupuestos de rendimiento medidos.
- **Tests/simulaciones:** los de la REDTEAM matrix marcados S5; benchmarks de frame pacing.
- **Evidencia humana:** N/A (técnica).
- **Exit gates:** `T-S5`. **Sin iOS: `BLOCKED_IOS`** — y con BLOCKED_IOS no hay TRAINING_READY, ni alpha, ni
  lanzamiento (regla de catálogo).
- **Observabilidad:** métricas técnicas agregadas locales (jank rate por sesión, sin identificadores).
- **Migración:** la que la decisión PLATFORM-MIN exija (minSdk) — slice propia si baja a 26.
- **Rollback:** flag OFF global de focus2.
- **Failure behavior:** cualquier eje rojo (datos/lifecycle/paridad) ⇒ V1 FAIL, tracers siguen internos.

---
### S6 — V2 formativo humano (alpha privada)
- **Outcome:** evidencia humana FORMATIVA: comprensión, usabilidad, accesibilidad, bugs, seguridad subjetiva,
  distribuciones preliminares (techo/suelo, tiempos), device-effects gruesos.
- **Estado alcanzable:** V2_EVIDENCE_COLLECTED (alpha ≈20, LCH-001).
- **Entry gates:** `T-S5` PASS (iOS incluido) + reclutamiento/consentimiento aprobados por CEO + ADR-DATA-002
  equivalente (retención para datos alpha) decidido.
- **Dependencias:** copy neutral de investigación; canal de reclutamiento (Discord ~240, adultos — LCH-003).
- **Archivos/componentes esperados:** build alpha con flags; cuestionarios formativos; consentimiento por
  finalidad (PRI-001/002).
- **Owner único:** CEO (única persona que recluta y trata con humanos).
- **Agent routing:** mindboost-product-ux (guiones formativos) · privacy-security (consentimiento) ·
  efficacy-guardian (que el copy no infle claims).
- **Tareas exactas:** (1) protocolo formativo (qué se pregunta, qué se observa); (2) consentimiento;
  (3) sesiones alpha; (4) análisis DESCRIPTIVO (lo permitido a N≈20: QC, techo/suelo, curvas de práctica
  descriptivas, usabilidad — JAMÁS fiabilidad/validez/eficacia); (5) informe V2.
- **Tests/simulaciones:** N/A (humano formativo).
- **Evidencia humana:** SÍ — la primera; copy de investigación neutral; **cero claims C0 de producto antes de
  superar V2** (el tracer sigue interno hasta este gate).
- **Exit gates:** V2 (evidencia registrada + issues P0/P1 de usabilidad resueltos o aceptados).
- **Observabilidad:** feedback cualitativo estructurado; nada automático nuevo.
- **Migración:** N/A. **Rollback:** retirar build alpha.
- **Failure behavior:** incomprensión/abandono sistemático ⇒ REOPEN de S3/S4 (diseño), no parche de copy.

---
### S7 — Escalado del catálogo (≥15)
- **Outcome:** el bloque Enfoque alcanza ≥15 ejercicios excelentes jugables (CAT-003) con dossiers,
  generadores deterministas, diversidad real y adaptación training-only.
- **Estado alcanzable:** CATALOG_15_PLUS (cada ejercicio individualmente TRAINING_READY tras su T-S7).
- **Entry gates:** `T-S5` (el motor está probado en 2 mecánicas).
- **Dependencias:** portfolio ratificado (S2); pipeline por-ejercicio = mini-S3 (dossier→generador→scorer
  descriptivo→T-ADV propio→a11y→paridad).
- **Archivos/componentes esperados:** por ejercicio: definición+generador+scorer+renderer+tests+dossier;
  reuso v1 según columna "reuse" del portfolio; los PROTOTYPE (E03/E14/E17/E18) resuelven su pregunta abierta
  antes de ACCEPT; E16 permanece RESEARCH_ONLY (sin score).
- **Owner único:** cognitive-engine-dev.
- **Agent routing:** por ejercicio: cognitive-engine-dev → compose-ui-dev → test-engineer → efficacy-guardian
  (dossier fidelity) → diff-auditor. Máx 2-3 en vuelo.
- **Tareas exactas:** orden de construcción propuesto (cubre subhabilidades pronto y deja PROTOTYPE al final):
  E02, E04, E05, E06, E08, E09, E15, E12, E10, E11, E13, E03, E14, E17, E18.
- **Tests/simulaciones:** por ejercicio: golden determinismo + T-ADV propio + a11y + frontera.
- **Evidencia humana:** opcional formativa continua (mismo protocolo V2).
- **Exit gates:** `T-S7` por ejercicio; el bloque declara ≥15 SOLO contando ejercicios con T-S7 PASS.
- **Observabilidad/Migración/Rollback:** como S3 (flag por-ejercicio en el catálogo v2, no flags globales nuevos).
- **Failure behavior:** un ejercicio que no pasa su T-ADV entra al bloque SOLO sin feedback numérico (práctica
  pura) o no entra; jamás "provisional con número".

---
### S8 — Measurement y shadow
- **Outcome:** carril de medición separado (probes explícitos + formas retenidas), journal según ADR-FOCUS-DATA,
  y scorers rivales computando EN SOMBRA.
- **Estado alcanzable:** SHADOW_MEASUREMENT_RUNNING.
- **Entry gates:** `T-S1` (ADR-DATA decidido) + `T-S5`.
- **Dependencias:** sondas retenidas DISEÑADAS ANTES de que el usuario entrene las formas de práctica (R5/B-6
  del dictamen — esto obliga a diseñar los bancos held-out EN S3/S7, esta slice solo los ACTIVA); dosis de
  evidencia oficial (ADR-SCI-005 canon, interino conservador).
- **Archivos/componentes esperados:** probe runner (purpose=MEASUREMENT sellado); bancos held-out versionados;
  journal (solo si ADR (ii)/(iii)) con DoS-cap + retención + erasure; scorers B y C en sombra (sin superficie);
  export de auditoría interna pseudonimizado.
- **Owner único:** psychometrician (diseño) / data-sync-dev (persistencia).
- **Agent routing:** psychometrician → cognitive-engine-dev + data-sync-dev → test-engineer →
  privacy-security (journal) → diff-auditor.
- **Tareas exactas:** DIVISIÓN OBLIGATORIA (P1-10): **(fase pre-humana)** simulación completa + property/golden
  de los scorers sombra + recuperación de parámetros M0-M5 sintética; **(fase con datos)** SOLO recogida:
  probes reales de usuarios beta acumulan datos; NADIE analiza para seleccionar fórmula aquí (eso es S9).
- **Tests/simulaciones:** suite de simulación del pipeline completo; invariantes de no-exposición (ningún
  valor de scorer sombra alcanza UI/analytics — test de frontera); journal fail-closed (escritura fallida ⇒
  WITHHELD_TECHNICAL).
- **Evidencia humana:** recogida pasiva bajo consentimiento; sin feedback de score al usuario.
- **Exit gates:** `T-S8`.
- **Observabilidad:** conteos agregados de probes completados (sin valores).
- **Migración:** journal con schemaVersion; retención enforced con purga por edad.
- **Rollback:** desactivar probes (flag); journal purgable por retención o borrado total.
- **Failure behavior:** si ADR-DATA = (i) NO-RAW ⇒ los outcomes replay-dependientes quedan BLOCKED declarados
  (re-scoring retroactivo, forense por-trial) y S9 se diseña solo sobre resúmenes+forma.

---
### S9 — V3 y ADR psicométrico
- **Outcome:** con datos repetidos REALES (beta): práctica, dispositivo, versión, fiabilidad, error, cobertura
  e incertidumbre analizados; harness M0-M5 ejecutado; SOLO ENTONCES se propone fórmula (ADR-FOCUS-SCORING).
- **Estado alcanzable:** V3_ANALYZED (+ eventualmente V3 PASSED por faceta).
- **Entry gates:** `T-S8` + N de beta dimensionado por simulación (no un N universal).
- **Dependencias:** ADR-SCI-001/002/003/008 del canon (backlog 09 §19) — esta slice los alimenta.
- **Archivos/componentes esperados:** análisis reproducibles (notebooks/scripts versionados fuera del app);
  propuesta de ADR-FOCUS-SCORING con ≥3 rivales comparados; decisión de subscores por faceta (o NO-GO honesto
  si gana M0/M4/M5).
- **Owner único:** psychometrician.
- **Agent routing:** psychometrician → efficacy-guardian (revisión adversarial del análisis) → CEO (ADR).
- **Tareas exactas:** (1) fiabilidad/SEM por tarea y faceta; (2) efectos de práctica modelados; (3) invariancia
  por dispositivo/clase T; (4) M0-M5 con validación cruzada; (5) propuesta de fórmula + migración
  (dual/shadow TEC-011); (6) si V3 PASS por faceta: activar PROVISIONAL→CURRENT según contrato de 8 estados.
- **Tests/simulaciones:** recuperación de parámetros pre-registrada; análisis pre-especificado (evitar garden
  of forking paths).
- **Evidencia humana:** los datos de S8; sin recogida nueva específica salvo diseño explícito.
- **Exit gates:** V3 (por faceta) + ADR-FOCUS-SCORING registrado. Continúa SIN publicación hasta el ADR.
- **Observabilidad/Migración:** dual/shadow scoring en cualquier cambio de fórmula; series antiguas preservadas.
- **Rollback:** la fórmula nueva vive tras flag + shadow hasta su propio gate; revert = flag.
- **Failure behavior:** M0 gana ⇒ NO-GO de subscores (los ejercicios siguen C0 training + feedback descriptivo)
  — declarado como resultado VÁLIDO del programa, no como fracaso a esconder.

---
### S10 — Beta y release hardening
- **Outcome:** estabilidad, observabilidad, rollback operativo, soporte y claims permitidos auditados para
  beta abierta (LCH-002).
- **Estado alcanzable:** BETA_READY.
- **Entry gates:** V2 + `T-S5`; V3 NO es prerequisito (beta puede correr con feedback C0 sin scores).
- **Dependencias:** gates de release del repo (RELEASE_EVIDENCE, firma fail-closed 2026-06-19); revisión de
  claims (review-cognitive-claims) sobre TODO el copy.
- **Owner único:** mindboost-devops-release (rol) / CEO (publicación — autorización separada GOV-013).
- **Agent routing:** qa-safety + security-privacy + docs-knowledge → diff-auditor.
- **Tareas exactas:** (1) auditoría de claims del copy completo; (2) crash-free y presupuesto de ANRs;
  (3) runbook de rollback (flags + store rollback); (4) soporte (FAQ, contacto); (5) checklist store.
- **Tests/simulaciones:** REDTEAM matrix filas S10 (upgrade path, flag misconfig, analytics sin consentimiento).
- **Evidencia humana:** beta abierta con telemetría consentida.
- **Exit gates:** `T-REL`.
- **Observabilidad:** Crashlytics PII-free (patrón existente) + eventos agregados consentidos.
- **Migración:** upgrade-path v1→coexistencia (v1 RETIRED oculto, v2 activo) probado.
- **Rollback:** flags + release anterior; datos v2 forward-compatible (reader N-1).
- **Failure behavior:** gate rojo ⇒ no release; sin excepciones por fecha (LCH-007).

---
### S11 — Programas posteriores (separados, preregistrados)
- **Outcome:** eficacia causal (RCT), near/far/everyday transfer (V5/V6), invariancia y normas (V7) como
  ESTUDIOS separados con preregistro (LCH-006) y gates propios; VEPR-30/outcome cotidiano según ADR-SCI-009.
- **Estado alcanzable:** por estudio.
- **Entry gates:** V3 PASSED (por faceta implicada) + preregistro + potencia calculada (ADR-EXP-001).
- **Dependencias:** S9 cerrada; reclutamiento y recursos del CEO; ADR-SCI-009 (VEPR-30) para el outcome
  cotidiano.
- **Archivos/componentes esperados:** por estudio: protocolo preregistrado (público o timestamped), plan de
  análisis pre-especificado, dataset separado del producto, informe con claims permitidos/denegados.
- **Owner único:** CEO (recursos/reclutamiento); diseño = psychometrician + experimentation.
- **Agent routing:** psychometrician (diseño/análisis) · efficacy-guardian (revisión adversarial) · CEO.
- **Tareas exactas (a nivel de programa; cada estudio detalla su protocolo ANTES de reclutar — el protocolo
  ES el entry-gate, no un sustituto de estos campos [R6-V5 P2-4]):** (1) preregistro; (2) potencia; (3)
  recogida; (4) análisis pre-especificado; (5) veredicto de claim (conceder/denegar/retirar).
- **Tests/simulaciones:** recuperación de parámetros del diseño ANTES de reclutar; análisis ciego a condición
  donde aplique.
- **Evidencia humana:** la del estudio, bajo consentimiento de investigación separado (PRI-001/002).
- **Exit gates:** el V-gate del estudio (V4/V5/V6/V7) PASSED o claim DENEGADO registrado (resultado válido).
- **Observabilidad:** la del protocolo; nada nuevo en producto.
- **Migración:** N/A (datasets de estudio separados). — **Rollback:** retirar el claim si V8 detecta drift.
- **Failure behavior:** resultado nulo/negativo se PUBLICA en el registro interno y el claim se deniega — un
  estudio fallido es un resultado válido del programa, jamás se reintenta en silencio con otro análisis.
  World Class bloqueado hasta normas representativas (SCO-019; LCH-009).

---

## §6 — Matriz de requisitos (artefacto 6): `REQUIREMENTS_MATRIX_V5.csv`

102 requisitos FOC-* × 30 columnas. Validada mecánicamente: 102 ids únicos, 0 celdas vacías, 0 tokens de gate
huérfanos (todo gate ∈ GATE_CATALOG_V5 — incl. su §2.1 de hitos no-gate — y todo ADR ∈ ADR_REGISTER_V5 incl.
§backlog-pack). **Regla de precedencia [R6-V5 P0-3]: las columnas entry/exit_gate del CSV heredan la
granularidad por-requisito del pack; la autoridad de gates POR SLICE es la tabla §4 del catálogo — en caso de
divergencia entre una celda del CSV y el catálogo/plan §5, ganan el catálogo y el plan.** Cambios V4→V5 por
mandato: `pack_priority` (dato del pack: 92×P0) SEPARADO de `severity_v5` propia (P0:1 · P1:39 · P2:62);
`pack_status` SEPARADO de `normative_status_v5` re-derivado del canon: **HYPOTHESIS 44 · UNVERIFIED 28 ·
ADR_REQUIRED 22 · APPROVED 8** — solo 8 filas tienen ancla canónica confirmada; **ninguno de los 61
RECOMMENDED del pack se trata como aprobado**; las APPROVED_EXISTING sin ancla confirmada quedan UNVERIFIED
(el pack no otorga autoridad); 7 filas llevan marcador CONTRADICTED. `canonical_source` por fila (específica u
honestamente de familia), `secondary_evidence` → agent_outputs/, `slice_v5` → R0–S11, `owner_v5` único.

## §7 — Portfolio (artefacto 7): `EXERCISE_PORTFOLIO_V5.md`

18 ejercicios (≥15), 5 subhabilidades cubiertas (≥2 c/u), 15 mecánicas distintas, dossier-lite por fila,
tracers A=FOC2-E07 (flanker) y B=FOC2-E01 (vigilancia) re-derivados por criterios; E16 stop-signal
RESEARCH_ONLY; difference-scores y SSRT retirados como scores.

## §8 — ADRs (artefacto 8): `ADR_REGISTER_V5.md`

Los 8 ADR-FOCUS-* del mandato + ADR-FOCUS-PLATFORM-MIN + backlog-pack. Ninguno decidido; opciones con
recomendación, owner, serialización, migración, rollback y fail-closed sin decisión. ADR-FOCUS-SCORING =
BLOCKED_BY_DESIGN hasta simulación+datos+análisis (P1-10).

## §9 — Gates (artefacto 9): `GATE_CATALOG_V5.md`

Vocabularios DISJUNTOS: científico V0–V8 (verbatim) y técnico T-* (12 gates). T-ADV jamás equivale a V3.
Códigos V4 RETIRADOS con tabla de reemplazo. Regla BLOCKED_IOS cerrada. Cierre de 5 campos conservado.
Todos DESIGNED_NOT_RUN.

## §10 — Risk register (artefacto 10)

| # | Riesgo | Trigger observable | Owner | Mitigación | Gate |
|---|---|---|---|---|---|
| R-01 | Score v1 sigue visible; un usuario real aprende a mashear | cualquier build sin S0 | CEO | S0 ocultación reversible; hasta entonces el riesgo CONTINÚA (declarado) | T-S0 |
| R-02 | La doble autoridad de propósito se cablea en v2 | un consumidor lee `mayContributeToProfile()` | kmp-architect | ADR-FOCUS-PURPOSE opción 3 + retirada de la función + test de propiedad | T-S1 |
| R-03 | Journal/raw construido sin decisión propietario+DPO | escritura por-trial en un diff | privacy-security | C-06 = gate de entrada de S1 | T-S1 |
| R-04 | Jank-gate ciego en >60 Hz infla la clase T | dispositivo 120 Hz clasifica T1 con drops reales | mobile-performance | C.1 detección de Hz real ANTES de derivar clase; candidato a ledger | T-S5 |
| R-05 | iOS dado por verde por compilar | ios-build.yml verde leído como paridad | kmp-architect | niveles COMPILE/UNIT_SIM/MANUAL_DEVICE separados; BLOCKED_IOS por defecto | T-S5 |
| R-06 | Un scorer sombra filtra a UI/analytics | valor de rival B/C en cualquier superficie | psychometrician | test de frontera de no-exposición + input-manifest allowlist | T-S8 |
| R-07 | Fórmula elegida antes de datos (presión de producto) | propuesta de fórmula sin dataset S8 | CEO | ADR-FOCUS-SCORING BLOCKED_BY_DESIGN; solo descriptivo A productivo | V3 |
| R-08 | Alpha ≈20 sobre-interpretada como validación | claims de fiabilidad/validez en informe V2 | efficacy-guardian | límites N≈20 tabulados; revisión adversarial del informe | V2 |
| R-09 | Thermal/práctica/estado leídos como señal cognitiva | deriva de frame-time o advertencia motivacional mueve el "rasgo" | psychometrician | exclusión por segmento thermal-confounded; instrucciones estandarizadas | T-S5/V3 |
| R-10 | SecureKeyValueStore (39 consumers) rompe el aislamiento de frontera | el boundary-test debe excepcionar un archivo del paquete prohibido | kmp-architect | decisión S1: extraer a core/ o excepción explícita documentada | T-S1 |
| R-11 | Baseline del repo no verde y se construye encima | desktopTest/detekt rojos no atribuidos en R0 | CEO | T-BASE exige outputs; BLOCKED_REPO_BASELINE detiene el plan | T-BASE |
| R-12 | TEC-002/minSdk resuelto en silencio "arreglando el doc" | cambio de canon o toml sin decisión registrada | CEO | ADR-FOCUS-PLATFORM-MIN; 06_ §13 | T-S1 |
| R-13 | Deriva del bundle (revisión anterior al último cambio, como V4-H3) | edición post-review sin re-lente | sesión /mb | freeze: manifiesto SHA-256 + diff-auditor SIEMPRE último | T-PLAN |
| R-14 | Coste multi-agente sin techo | sesiones largas repetidas | CEO | GOV-011; slices pequeñas con gate | — |
| R-15 | Alpha con menores o consentimiento inválido | participante <18 o consent genérico | CEO | LCH-003 (adultos) + PRI-001/002 (por finalidad) | V2 |

## §11 — Fuera de alcance con owner y gate futuro (artefacto 11)

| Ítem | Owner | Gate futuro |
|---|---|---|
| Actualización del MINDBOOST_AUDIT_LEDGER (cierre de esta replanificación + hallazgos nuevos: probe 60 Hz hardcodeado; doble autoridad de propósito) | sesión /mb en la primera fase autorizada a escribir | autorización de escritura del CEO |
| Extracción de SecureKeyValueStore de focus/ a core/ (39 consumers) | kmp-architect | decisión en T-S1 (o excepción documentada) |
| Port CADisplayLink iOS (FrameTimingProbe real) | mobile-performance | T-S5; requiere Mac+iPhone |
| Banco físico de latencia (T2) | CEO (equipamiento) | revisión ADR-FOCUS-TIMING; sin fecha |
| Bajada de minSdk a 26 (si TEC-002 se ratifica) | CEO | ADR-FOCUS-PLATFORM-MIN |
| Export de datos orientado a usuario (PRD §18.3, gap de TODO MindBoost) | privacy-security | ADR-DATA-002 |
| k-anonimato/re-identificación antes de CUALQUIER sync | privacy-security | pre-ADR-DATA-001 |
| Sync cloud de Focus v2 | data-sync-dev | ADR-FOCUS-DATA + ADR-DATA-001; hoy local-only |
| Subscores de faceta, Growth Index de Enfoque, ranking | psychometrician | V3+ADR; V7 para ranking |
| Estudios V4–V8 (novel-form, transfer, normas) | CEO + psychometrician | preregistro por estudio (S11) |
| Retirada del árbol v1 legacy | kmp-architect | post-S7 + export de usuario |

## §12 — Diferencias V4 → plan corregido (artefacto 12)

1. **Constructo:** V4 organizaba por SUSTAIN/CONSTRAIN/RESTRAIN (pack); V5 por las 5 subhabilidades del PRD,
facetas degradadas a hipótesis rivales M1. 2. **Procedencia:** V4 daba autoridad operativa al pack (61
RECOMMENDED como plan); V5 re-deriva del canon (solo 8 APPROVED confirmados). 3. **Gates:** de crosswalk
G-S2→V3 y huérfanos G-V1/2/3 a dos vocabularios disjuntos completos. 4. **Purpose:** de enum propio +
proyección a UNKNOWN a UNA autoridad (opción 3) + disposition 3-vías + cierre de la grieta
`mayContributeToProfile` (hallazgo NUEVO que V4 no vio). 5. **Estados:** de "7+1" a los 8 de doc 06 §7 con
los 4 ejes de estado separados. 6. **Orden científico:** de comprimido a simulación/alpha/recogida/V3/beta/
estudios con límites explícitos de N≈20. 7. **Catálogo:** de 2 ejercicios nominales a matriz de 18 con
dossier y par de tracers re-derivado. 8. **Score v1:** de "opciones al CEO" a acción segura fijada (ocultar
reversible, historial retirado) + corte de la 2ª puerta arquetipo con inventario de 5 rutas. 9. **Integridad:**
de CRC32+ruta absoluta a SHA-256 portable + line endings declarados. 10. **Verticalidad:** el "end-to-end" de
V4-S2 pasa a S3 con lifecycle/recovery/a11y DENTRO del vertical y paridad en S5. 11. **iOS:** de caveat a
regla dura (BLOCKED_IOS bloquea TRAINING_READY/alpha/lanzamiento) con niveles de evidencia. 12. **Timing:**
de gate genérico a clases T0-T3 derivadas por segmento + hallazgo del Hz hardcodeado + límite honesto (T2
inalcanzable sin banco). 13. **Datos:** C-06 pasa de conflicto registrado a ADR con 3 opciones analizadas +
contrato de 14 ejes mapeado a mecanismos reales. 14. **Frontera:** de scanner de texto a grafo de
dependencias + firmas + goldens de comportamiento, con la excepción SecureKeyValueStore descubierta.
15. **Proceso:** la última revisión SIEMPRE posterior a la última modificación (corrige el defecto H3 de V4).

## §13 — Autocrítica (artefacto 13)

1. **El baseline Gradle sigue NOT_RUN** (prohibición de entorno: daemon del IDE); el plan arranca con T-BASE
ejecutado por el CEO, pero HOY no sé si desktopTest/detekt están verdes en este HEAD — la mayor incertidumbre
operativa. 2. **28 filas UNVERIFIED** en la matriz: preferí honestidad a fabricar anclas; T-DR debe revisarlas
una a una. 3. **Anclas de familia** donde no hay específica: correcto pero grueso. 4. **El protocolo de
timing** depende de un inventario de dispositivos que no existe aún y sus umbrales son DESIGNED_NOT_RUN.
5. **iOS**: todo es diseño sin verificación posible desde este host; riesgo real de sorpresas al primer
contacto con Mac. 6. **El pack 07 (UX/a11y)** no fue releído por orden 2 (ruta errónea); las reglas a11y
vienen del mandato+canon — impacto bajo, declarado. 7. **Densidad**: 13 slices × 15 campos es mucho que
auditar; mitigación: artefactos por-tema autocontenidos. 8. **Sesgo de autor**: el mismo orquestador escribió
V4 y V5; las lentes adversariales mitigan pero no eliminan — la revisión del CEO/ChatGPT (07_PHASE_REVIEW)
es la barrera real.

## §14 — Cierre del pipeline y no-implementación

Pipeline: R0 baseline → orden 1 (4 lentes) → orden 2 (timing) → integración → orden 3 (test-engineer,
falsación) → correcciones → FREEZE (manifiesto SHA-256 portable, LF) → orden 4 (diff-auditor sobre el bundle
final EXACTO) → si corrige, re-hash + re-lente afectada → entrega. La última revisión es SIEMPRE posterior a
la última modificación. **Esta tarea NO implementó nada: cero mutaciones del repositorio (worktree idéntico
verificado al inicio y al cierre), cero dependencias, cero wiring, cero schemas, cero despliegues, cero
reclutamiento, cero modificación de datos. El ledger queda PENDING_BY_OWNER_DIRECTIVE.**


