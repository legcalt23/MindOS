# S5_REOPEN_STATUS — Focus V5 · S5 «V1 técnico y paridad» (REOPEN)

```
ESTADO = REOPEN (contrato del CEO, 2026-07-21) · Este documento es el artefacto vivo de S5-01
(baseline reproducible + claim inventory) y el registro de progreso de la reapertura.
Sustituye operativamente a S5_EXECUTION_PROPOSAL.md (SUPERSEDED — conservado como evidencia histórica).
Fuentes: context_pack 2.0.2 (precedencia 00 §3) · FOCUS_ENGINE_MASTER_PLAN_V5 §5/S5 · GATE_CATALOG_V5 (T-S5)
· ADR_REGISTER_V5 · 09_DECISION_LOG §20.2 · MINDBOOST_AUDIT_LEDGER (checkpoint S4 + port iOS).
R3 (2026-07-21→2026-07-22, reapertura externa R2→R3): la R2 quedó FALSADA (contraejemplo Absent→Present
reproducido con el scorer real — ver §0, VERIFIED_PRIMARY, DOMINA cualquier APTO) y degradada a
REOPENED_AFTER_EXTERNAL_AUDIT. Paso 7 (2026-07-22) reconcilió docs (05/09/register/status) + resello del ledger.
El tracker de lentes `scratchpad/S5_R3_lens_integration.md` NO se conserva en este árbol ⇒ «gate 6/6+7/7 APTO» =
MISSING_PRIMARY_EVIDENCE, NO se afirma como hecho verificado. R3 = CANDIDATA para auditoría externa (la autoridad
que aprueba); NO congelada, NO montada. Autoridad vigente: `CURRENT_AUTHORITY_INDEX.md`.
```

## 0. Reapertura externa R2→R3 (2026-07-21) — R3 CANDIDATA, gates §8 BLOCKED

Segunda reapertura obligatoria (auditoría externa). Rol confirmado por el usuario = **Claude implementador**
produciendo la R3 candidata (diseño/pruebas/reconciliación documental; SIN implementación dependiente).

### 0.1 Falsación PROBADA de la R2 (R3-AUD-01 / R3-AUD-02) — domina cualquier APTO

Reproducida por la sesión principal con la fórmula EXACTA del scorer real
(`focus2/domain/scoring/DescriptiveScorer.kt:90,103-109,155-164` + `Probit.zScore`, `Z_95=1.959963984540054`):
`d′ = z((hits+0.5)/(signalTrials+1)) − z((FA+0.5)/(noiseTrials+1))`, corrección loglinear de Hautus.

| Escenario | Celda admisible | H,F corregidas | d′ (S_adm) | `S_adm ≤ S_full`? | Gate R2 §6.1 |
|---|---|---|---|---|---|
| **S_full** (todas las ventanas) | 30H/10M/10FA/30CR | 0.7439 / 0.2561 | **1.3107** | — (referencia) | — |
| **D1** = excluir 5 misses | 30H/5M/10FA/30CR | 0.8472 / 0.2561 | **1.6805** | **NO** (1.68 > 1.31) | **Absent** |
| **D2** = excluir 5 misses + 15 hits | 15H/5M/10FA/30CR | 0.7381 / 0.2561 | **1.2933** | **SÍ** (1.29 ≤ 1.31) | **Present(S_full)** |

D2 admisible: señal = 20 (= `MinSampleGate.DPRIME.nMinPerCell`), ruido = 40, exclusión = 20/80 = **25% < 50%
backstop**, no diferencial. **Más exclusión técnica (D1→D2) ABRE el gate que menos exclusión cerraba** ⇒
`monotonicGuard mg-1` y §10.3(c) **FALSADOS**. La cláusula `S_adm ≤ S_full` (el propio parche R2) crea la
no-monotonía; `CI-lower(S_full)>0` hace la elegibilidad dependiente del rendimiento (R3-AUD-02: dos usuarios con
trazas técnicas idénticas — uno discrimina, otro a nivel de azar — reciben Present vs Absent ⇒ sesgo de selección
longitudinal al alza). **El mecanismo se elimina como gate de elegibilidad.**

### 0.2 Decisiones cerradas por esta auditoría (D-R3-01..04), a codificar en la R3

- **D-R3-01** — La elegibilidad TEMPORAL es OUTCOME-INDEPENDENT: función solo de hechos técnicos pre-outcome
  (capability, coverage/report-loss, continuidad de reloj, cadencia esperada/observada, integridad de ventana,
  versión, blueprint/modalidad). NUNCA de H/M/FA/CR, accuracy, d′, `CI-lower>0` ni de si excluir ayuda/perjudica
  al score. La validez conductual/anti-automatización es un eje SEPARADO, no disfrazada de calidad temporal.
- **D-R3-02** — S5 NO certifica accuracy ni RT temporal. `S_full`/`S_adm`/excluded-fraction existen solo en
  SOMBRA; ningún diagnóstico temporal produce evidencia de medición; estado de evidencia temporal =
  `TRAINING_ONLY`/`NotCaptured`; `PROVISIONAL` no insinúa que estos datos actualicen un perfil; `OFFICIAL_ELIGIBLE`
  inconstructible. S6 monta e instrumenta pero NO desbloquea medición (5 gates separados posteriores).
- **D-R3-03** — Sin «commit real» por FrameMetrics: eliminar de ADR/plan/S6 «commit real»/«onset real»/«desbloquea
  rt−D por API»/«exactitud por NANOS_NATIVE». Nombres epistémicos (p. ej. `FRAME_ISSUED_PROXY`). La futura
  elegibilidad RT se basa en `validatedEndToEndErrorBound` + asociación correcta + validación física, no en la
  unidad/nivel de API del timestamp.
- **D-R3-04** — Retención coherente con drain-not-adopt: residuo write-ahead en cold start = intento abandonado ⇒
  purga-antes-de-leer, idempotente, SIN `MAX_ABANDONED_SESSION_AGE_HOURS` ni wall-clock; se documenta que una app
  que no corre no puede imponer un TTL físico; account-deletion y subject-revocation siguen siendo rutas inmediatas
  independientes.

### 0.3 Baseline (§1) y preservación

- HEAD `8a06b9a` · rama `product/focus2-v5`. Backup reproducible: `scratchpad/S5_R3_baseline_{tracked.patch (742
  líneas), status.txt (32 entradas), HEAD.txt}`.
- **Ajenos preservados, NO tocados/staged:** `DMListScreenIcon.ios.kt`, `AppMediaViewer.ios.kt` (port iOS).
- Código S5 propio (modificado + untracked) intacto; esta reapertura solo edita DOCS.

### 0.4 Estado del gate §8 y por qué la R3 sigue CANDIDATA (auditoría externa pendiente) — COMPLETO

**Actualización 2026-07-22 (Paso 7).** Lo que DOMINA es primario y está probado: la **falsación de la R2**
(§0.1, `VERIFIED_PRIMARY`, contraejemplo reproducido con el scorer real). Es la razón por la que el ADR está
`REOPENED_AFTER_EXTERNAL_AUDIT`, y no depende de ninguna lente. En cambio, los veredictos por-lente del gate de
entrada §8 y del gate de salida (7 lentes) **NO se conservan en este árbol**: el tracker
`scratchpad/S5_R3_lens_integration.md` es efímero y no sobrevivió. Por el guardarraíl de honestidad (no se
afirma un gate sin su registro), Paso 7 **NO declara «6/6 + 7/7 APTO»**: ese claim queda
`MISSING_PRIMARY_EVIDENCE`. La R3 no se congela por dos razones, ninguna de ellas «límite de sesión»: (1) el
contrato exige devolverla a **auditoría externa**, la autoridad que aprueba; (2) el registro interno del gate no
es reproducible aquí. Hasta la auditoría externa: **R3 = CANDIDATA**; la R2 está FALSADA; ninguna implementación
dependiente avanza. (La narrativa previa «gates BLOCKED por límite de sesión / reset 22:50» era cierta el
2026-07-21 pero quedó OBSOLETA: el límite se reseteó; el estado real es el de arriba.)

### 0.5 Checklist de la reapertura (estado a 2026-07-22, Paso 7)

1. **HECHO — falsación de la R2** (§0.1): R3-AUD-01/02 PROBADOS (`VERIFIED_PRIMARY`, contraejemplo con el scorer
   real). Domina cualquier APTO y motiva la reapertura.
2. **HECHO — R3 escrita en el ADR** (`ADR_FOCUS_TEMPORAL_QUALITY.md`) con `DECISION_STATUS`/`IMPLEMENTATION_STATUS`/
   `VALIDATION_STATUS` separados (steps 5–14 = `IMPLEMENTATION = PENDING`).
3. **HECHO — decisiones R3 registradas** (D-R3-01..04) en `09_DECISION_LOG.md §20.6`, + custodia D-P7-A y
   discoverability D-P7-B.
4. **NO CONSERVADO — gate interno 6/6+7/7:** el tracker `scratchpad/S5_R3_lens_integration.md` no sobrevivió ⇒
   `MISSING_PRIMARY_EVIDENCE`; Paso 7 NO afirma «7/7 APTO». La aprobación la da la auditoría externa.
5. **HECHO — Paso 7 (esta invocación):** docs `05/09/register/status` reconciliados + `CURRENT_AUTHORITY_INDEX` /
   `PROVENANCE_INCIDENT_V5_BUNDLE` / `S5_R3_EVIDENCE_CAPSULE` creados + **ledger resellado** (validador VERDE). NO
   se emite `FROZEN_FOR_S5_R3` (contrato: devolver la R3 a auditoría externa). NO se toca código (steps 5–14).
6. **SIGUIENTE — devolver la R3 CANDIDATA** (+ falsación + esta reconciliación + `S5_R3_P7_HANDOFF.md`) a la
   auditoría externa. Si aprueba, el cierre real de S5 (versionar ADR/register, re-sellar bundle) es otra
   invocación con su propio `T-DR`.

## 1. Estado real del repo (Paso 0)

- **Branch:** `product/focus2-v5` · **HEAD:** `8a06b9a` («docs(ios): record Kotlin/Native compat port»).
- **Working tree al arrancar la reapertura** (`git status --porcelain`, 2026-07-21):
  - ` M shared/src/androidMain/.../core/timing/FrameTimingProbe.android.kt` — S5·task 0 (Hz real por segmento vía `refreshHzProvider`; sin commitear).
  - ` M shared/src/desktopTest/.../core/timing/FrameStatsCalculatorTest.kt` — test REDTEAM #19 (mismo stream clasificado distinto a 60 vs 120 Hz; sin commitear).
  - `?? docs/engines/focus_v5/S5_EXECUTION_PROPOSAL.md` — propuesta previa, ahora SUPERSEDED.
- **Cambios añadidos por esta reapertura (Paso 0):** fixes detekt en 2 archivos `iosMain` (ver §3.1) + este documento.
- **Preservación de cambios ajenos:** no hay WIP ajeno en el working tree (inventario completo arriba).

### 1.1 Regla de Gradle y vía usada (contrato §4.5)

- **Regla exacta:** `CLAUDE.md` raíz, §4 «Reglas de entorno», punto 1: *«Daemon del IDE: si Android Studio
  posee el daemon Gradle de este worktree, NO invoques Gradle CLI…»* — regla **condicional**.
- **Comprobación de la condición (2026-07-21):** `Get-Process | Where-Object { $_.ProcessName -match
  'studio|java|gradle' }` → **cero procesos**. Android Studio NO posee el daemon ⇒ la condición no aplica.
- **Vía usada:** la sesión ejecuta los gates ella misma, **en serie, una sola invocación Gradle**
  (`:shared:desktopTest :shared:detekt :androidApp:compileDebugKotlin`), sin agentes corriendo Gradle
  (prohibición explícita en sus prompts; learning 2026-07-19). Precedente registrado: checkpoint R0 del
  ledger («no IDE/JVM process was running, so the session executed the gates itself»).
- Gate iOS: `ios-build.yml` (workflow_dispatch, runner macOS, Xcode 16) — se relanza sobre el diff FINAL.

## 2. Claim inventory (S5-01) — claim → evidencia → estado

| # | Claim | Evidencia | Estado |
|---|---|---|---|
| C1 | `T-S4` PASS (S4 cerrada) | Ledger checkpoint `TASK-2026-07-21-FOCUS-V5-S4-TRACER-B`: desktopTest 3964/3964 (559 suites, 0 stale XML), detekt 0, ambos compile lanes, exit-gate privacy/efficacy APTO + diff-auditor engineering-APTO; sello SHA-256 `8526b509…`; commits en `git log` | VERIFIED (para el árbol del sello S4) |
| C2 | Task 0 de S5 hecha (Hz real + REDTEAM #19) | Diff working-tree en los 2 archivos de §1; test `sameStream_isClassifiedDifferently_at60Hz_and120Hz` | VERIFIED como código; gate en re-ejecución (§3) |
| C3 | iOS CI verde (compile + sim-tests) | Runs `29786810391` (compile, success, ejecutado-no-cacheado 27/27) y `29788162586` (compile+test, success) sobre `ee65a55` | VERIFIED **para ee65a55**; NO válido para el diff final de S5 (S5-11) — se exigirá run fresco al cierre |
| C4 | «Gates verdes hoy» (baseline actual) | Run 1 (2026-07-21): `BUILD FAILED` — `:shared:detekt FAILED` con 4 findings en `iosMain` (`DMListScreenIcon.ios.kt` FunctionNaming; `AppMediaViewer.ios.kt` FunctionNaming + WildcardImport ×2); `desktopTest` ni llegó a ejecutarse | **CONTRADICTED → CORREGIDO Y RE-VERIFICADO**: run 2 exit 0, tareas gate EJECUTADAS (no cached), 559/559 XML frescos, **3965/0F/0E/0S** |
| C5 | `FocusRuntimeSnapshot` = solo counts (sin trials) | Auditoría directa `FocusRuntimeModel.kt:88-135`: campos = schemaVersion/sessionId/stateName/phaseName/trialIndex/2 timestamps/tallies(`ConditionTally`→`SdtCell`)/resumeCount/contaminatedCount — CERO estructura por-trial; KDoc :65-87 «COUNTS, never trials» | **VERIFIED** |
| C6 | Timestamps del motor = monotónicos | Únicos timestamps del snapshot: `presentedAtElapsedNanos`/`deadlineElapsedNanos` (elapsed monotónico, `FocusRuntimeModel.kt:97,99`); `FocusClock` separa `elapsedNanos()` de `wallClockMillis()` («labelling only», `FocusSessionRuntime.kt:16-22`); cero campos epoch en el snapshot | **VERIFIED** |
| C7 | Scoring focus2 NO modificado | `git status`: cero archivos `focus2/**` tocados | VERIFIED |
| C8 | Restore fail-closed de S3 existe | `FocusSessionRestore.kt`: `ignoreUnknownKeys=false`; schema futuro→`SCHEMA_UNSUPPORTED`; corrupto/ausente distinguidos; ventana abierta descartada cross-epoch; 13 tests nombrados en `FocusSessionRestoreTest.kt` incl. anti-vacuidad `every_rejection_reason_is_covered…` | **VERIFIED** |
| C9 | Android mínimo = API 26 (contrato §4.1) | **CONTRADICTED por decisión posterior explícitamente SUPERSEDED**: `ADR-FOCUS-PLATFORM-MIN` APPROVED 2026-07-20 (09_DECISION_LOG §20.2 + §15 TEC-002 + §18): «TEC-002 SUPERSEDED, mínimo canónico = API 29, no se baja minSdk»; repo `libs.versions.toml: android-minSdk = "29"` | Piso operativo = **API 29** (la propia cláusula de escape del contrato lo prevé). Capability tiers Android: **29–30** (FrameMetrics+VSYNC disponibles) y **31+** (DEADLINE) |
| C10 | iOS mínimo = 15 | `shared/build.gradle.kts:195` `ios.deploymentTarget = "15.0"`; ADR-PLATFORM-MIN: «iOS 15 sigue vigente» | VERIFIED |
| C11 | Tests skipped/quarantined | Run 2: agregado de 559 XML frescos = 3965 tests, **0 skipped**, 0 quarantined | **VERIFIED** |

## 3. Gates baseline (comandos, exit codes, scope)

| Run | Comando | Resultado | Evidencia |
|---|---|---|---|
| 1 (2026-07-21) | `gradlew :shared:desktopTest :shared:detekt :androidApp:compileDebugKotlin --console=plain` | **exit 1 · BUILD FAILED en 2m48s** — `:shared:detekt` 4 weighted issues (iosMain); `desktopTest` y `compileDebugKotlin` cancelados | output guardado (task bne48wfmz); hallazgo S5R-F1 |
| 2 (2026-07-21) | ídem, tras §3.1 | **exit 0 · BUILD SUCCESSFUL 47s** — 83 actionable: 24 executed / 59 up-to-date; `:shared:desktopTest`, `:shared:detekt`, `:androidApp:compileDebugKotlin` EJECUTADOS (sin sufijo) | 559/559 `TEST-*.xml` escritos en la ventana del run; agregado **tests=3965 failures=0 errors=0 skipped=0** (task bmxbwdq51) |

### 3.1 Hallazgo S5R-F1 — el port iOS dejó detekt en ROJO (candidato a ledger)

Los batches del port (71c6b69…c6d73b1) verificaron **compile en CI iOS** pero nunca re-corrieron
`:shared:detekt` (que analiza también `iosMain`); `kotlin-ci.yml` solo dispara en main ⇒ nada lo cazó.
El claim «quality-reviewed» del port era incompleto. Corrección (raíz, no baseline):
- `DMListScreenIcon.ios.kt`: `@file:Suppress("FunctionNaming")` (convención del repo para Composables).
- `AppMediaViewer.ios.kt`: ídem + wildcard → imports explícitos (`CMTimeGetSeconds`, `CMTimeMake`,
  `CATransaction` — únicos símbolos usados de CoreMedia/QuartzCore).
Riesgo iOS de los imports explícitos: bajo (símbolos enumerados del propio archivo); lo confirma el run
iOS final obligatorio del cierre.

Nota ambiental: el run 1 reportó «1 busy Daemon could not be reused» pese a cero procesos JVM visibles en
el check previo (entrada stale del registry de daemons). Sin firma de corrupción (no hubo cascada
`NoClassDefFoundError`); la invocación arrancó daemon propio y el fallo fue el finding real de detekt.

## 4. Decisiones cerradas aplicables (no se re-preguntan)

1. **Estados de evidencia de sesión** = charter científico §7: `TRAINING_ONLY / PROVISIONAL /
   OFFICIAL_ELIGIBLE / INVALIDATED`. `CALIBRATING` es estado de **calibración de perfil** (§11.4), jamás
   de sesión.
2. **`weight = 1 − frameDropFraction` RECHAZADA como default.** Sin conteos fraccionarios. La
   interpretación vigente de «exclusión proporcional» = la del orden-2 (agent_outputs/05 §C.7/§E):
   exclusión **por segmento y POR MÉTRICA** (segmento NO-GO excluye SOLO su RT con reason code; accuracy
   se conserva salvo discontinuidad de reloj; la sesión cae solo con >50 % de segmentos NO-GO o
   discontinuidad global). Cualquier fórmula alternativa: solo shadow tras ADR + simulación + validación
   física + gate psicométrico (charter §27).
3. **Peor calidad técnica JAMÁS mejora un resultado oficial** (invariante de monotonía, a probar por test).
4. **Clases temporales = T0–T3** (ADR-FOCUS-TIMING, marco APPROVED): derivadas de la PROPIA sesión
   (frame stats + fuente de timestamps), nunca del modelo de dispositivo; dimensión TEMPORAL separada de
   la validez global. Regla cerrada: **nada RT-crítico entra a evidencia oficial en v1 de v2**.
5. **Android mínimo API 29 / iOS 15** (C9/C10). Tiers de capacidad 29–30 y 31+; `MotionEvent.
   getEventTimeNanos()` (API 34) jamás como requisito base.
6. **iOS paridad honesta:** NoOp→«sin observación» NO satisface paridad; si el adapter iOS no es
   defendible, la evidencia temporal oficial queda mecánicamente desactivada en iOS y S5 cierra
   `CONDITIONAL` con slice/owner/gate para completarlo.
7. **Datos:** ADR-FOCUS-DATA (iii) TIERED vigente — resúmenes por defecto; nada de raw trials en S5;
   diseño preferido = sufficient statistics estratificados (condición × métrica × calidad) con
   numeradores/denominadores reales, reason codes y versiones (contrato §6 opción B).
8. **Claude ejecuta los gates** (06 §2.2) por la vía de §1.1; el CEO aporta solo dispositivos físicos y
   pasos manuales.
9. **ADR-FOCUS-SCORING sigue BLOCKED_BY_DESIGN**: S5 no toca fórmula de score; el trabajo es
   observación temporal + eligibility por métrica + shadow, con barrera mecánica ante perfil/rank.

## 5. Plan maestro (06 §4) — reapertura S5

```yaml
objective: "S5 V1 técnico y paridad bajo el contrato REOPEN: base temporal honesta multiplataforma"
user_outcome: "El usuario entrena en cualquier dispositivo soportado; el rendimiento del dispositivo
  jamás se interpreta como su capacidad; puede exportar y borrar todo"
scientific_outcome: "Elegibilidad temporal por métrica y ventana, fail-closed, determinista, versionada;
  cero caminos donde degradación técnica fabrique progreso"
current_repo_evidence: ["§1-§3 de este documento", "authority map (en curso)", "data-flow map (en curso)"]
assumptions: ["API 29 floor (C9)", "focus2 local-only sin sync (REDTEAM #24 N/A)"]
unknowns: ["forma exacta de segmentación runtime (espera authority map)", "adapter iOS defendible en
  iOS 15 + Compose MP (investigación S5-06)", "overhead real del probe (Paso 5)"]
out_of_scope: ["fórmula de score (BLOCKED_BY_DESIGN)", "journal per-trial (S8)", "sync/cloud",
  "normas/ranking", "claims"]
slices_pasos:
  - {id: P0, outcome: "baseline + claim inventory + authority map", gates: ["gates serie verdes reales"]}
  - {id: P1, outcome: "gate de entrada 4 lentes con criterios concretos", gates: ["criterios registrados"]}
  - {id: P2, outcome: "ADR calidad temporal congelado (8 alternativas, capability model, granularidad,
      sufficient stats B, versionado, relojes, estados)", gates: ["ADR registrado; thresholds=config
      versionada shadow"]}
  - {id: P3, outcome: "instrumentación vertical: adapter Android tiers 29-30/31+ + iOS + observación por
      ventana + reducer + persistencia/restore + eligibility + export/delete", gates: ["desktopTest/detekt/
      android verdes", "lifecycle suite S5-10"]}
  - {id: P4, outcome: "shadow eligibility + simulaciones adversarias + invariante monotonía", gates:
      ["suite adversaria verde con mitades discriminantes"]}
  - {id: P5, outcome: "red-team técnico + presupuestos", gates: ["REDTEAM S5 rows verdes"]}
  - {id: P6, outcome: "protocolo físico + device matrix diseñados; inventario pedido al CEO", gates:
      ["protocolo registrado; sin hardware => CONDITIONAL"]}
  - {id: P7, outcome: "docs + ledger + gate de salida 8 lentes sobre diff final + cierre 14§", gates:
      ["T-S5 o CONDITIONAL honesto"]}
release_impact: "ninguno (focus2 sin flag de exposición; sin wiring a perfil)"
documentation_updates: ["ADR_REGISTER_V5", "09_DECISION_LOG", "05_TECHNICAL_CURRENT_STATE",
  "GATE_CATALOG si procede", "MINDBOOST_AUDIT_LEDGER", "este documento"]
```

## 6. Data-flow map (S5-09/S5-07 · resultado del agente read-only, 2026-07-21)

**Hecho dominante:** focus2 está **construido pero NO montado** — fuera del paquete solo existe en
`MindBoostStorageNamespaces.kt` (reserva) y `AppGraph.kt:125,159` (store cableado únicamente al eraser).
Nada instancia el holder/stores en producción; `mindboost_focus2` está vacío en runtime.

- **Flujo:** UI→`FocusTracerStateHolder`→reducer puro→`FocusRuntimeSnapshot` (write-ahead: descriptor
  sellado pre-trial + checkpoint por submit, claves `focus2_session:<uid>:<sid>:descriptor|:snapshot`)
  →`complete()`→`FocusCompletion.Scored` **solo en memoria** + `store.discard()` purga el buffer. **NO
  existe registro sellado permanente**, ni escritura a SessionRuns/Sessions, ni adapter a perfil, ni
  SyncQueue, ni cloud (`RESULT_SYNCED = NOT_EMITTED`), ni flag propio (barrera = no-mount estructural).
- **Namespaces:** único `mindboost_focus2` (sesión + consent comparten namespace); AccountBound + blanket
  `clearNamespace` en eraser + `AccountBoundErasureCoverageTest.kt:44`; `mindboost_revocation` permanente
  con guard negativo (:63). focus2 no usa revocation (no subject-scoped) — sin tombstone anti-resurrección
  propio, borrado = blanket completo bajo el modelo single-user-per-device declarado.
- **Consent:** 4 estados (incl. `UNAVAILABLE` sin quarantine); revocar = tombstone reescrito; scope único
  `TRIAL_JOURNAL`; gate `FocusJournalAdmission.admit()` puro con caps — **sin call-site productivo** (el
  journal writer es S8 por diseño).
- **Estructura runtime (clave para el ADR):** NO existe subestructura de bloques/segmentos — una sesión ES
  un bloque sellado a dificultad fija; fases por trial `IDLE/AWAITING_RESPONSE/INTER_STIMULUS`. La
  «ventana cognitiva crítica» real es el trial: `presentNext()` (`FocusTracerStateHolder.kt:148`) abre,
  `onResponse()` (:161, captura `clock.elapsedNanos()` en el tap) / timeout cierran. **Cero instrumentación
  de frames en focus2 hoy** (grep Choreographer/FrameMetrics/withFrameNanos = 0).

### 6.1 GAPS factuales registrados (para hallazgos/ADR)

1. Feature no montado / sin escritor productivo (KDoc lo declara; MB-FOCUS-0026/0028/0035/0042 siguen OPEN).
2. Consent completo pero desconectado (writer S8).
3. Sin registro sellado permanente ni ruta a perfil (por ausencia, no por flag).
4. **Sin export de focus2 NI export de cuenta en TODO el repo** (greps exhaustivos; el KDoc de namespaces lo
   lista como precondición futura para retirar v1) — S5-09 exige cubrirlo o declararlo con owner/gate.
5. Retención por EDAD del journal ausente (gap declarado, bloquea S8).
6. Borrado asume single-user-per-device (decisión out-of-scope 2026-06-29).
7. `MB-FOCUS-0035` (instancia única por usuario) declarado, no forzado — seguro solo por no-mount.
8. Sin probe de frame-timing; puntos naturales de cableado = `presentNext()`→`onResponse()`.

### 6.2 Authority map (S5-02 · resultado del agente read-only, 2026-07-21)

Conclusiones operativas (mapa completo con file:line en el output del agente; se preserva íntegro en el
ADR de calidad temporal):

1. **focus2 NO tiene enum de estado de evidencia de sesión**: la admisibilidad son booleans
   (`evidenceAdmissible` `FocusTracerStateHolder.kt:77` = disposición-propósito ∧ writeAheadSealed ∧
   ¬checkpointLost, en `complete()` además `delivered==sealed`; `rtEvidenceAdmissible`
   `FocusSessionDescriptor.kt:118` = ¬screenReader ∧ TOUCH_DIRECT) + `Verdict{Valid,InsufficientData,
   Invalidated}` del scorer + `FocusSessionState.INVALIDATED`. El vocabulario del charter §7
   (`TRAINING_ONLY/PROVISIONAL/OFFICIAL_ELIGIBLE/INVALIDATED`) existe SOLO en la lane cognitiva
   (`core/session/cognitive/evidence/SessionEvidenceState.kt:10`) y focus2 no la referencia.
2. **`CALIBRATING` NO aparece como estado de sesión en focus2** (grep = 0; los hits son coach/core.scoring/
   memory/speed). Cumple ya la prohibición del contrato.
3. **Cero uso de `core/timing`** (FrameStats/Probe/TimingPolicy) y cero uso del modelo rival
   `TimingIntegritySnapshot` de la lane cognitiva ⇒ la dimensión temporal es NET-NEW en focus2; patrón
   establecido = bump de `SCHEMA_VERSION` + decode fail-closed.
4. **Eje de propósito = autoridad única compartida** (`ProfileMeasurementEligibility`); la calidad
   temporal debe entrar como AND-gate adicional, jamás como segunda autoridad de propósito/elegibilidad.
5. **Riesgo señalado:** ya hay DOS implementaciones independientes de «write-ahead ⇒ evidence» (booleans
   focus2 vs `SessionEligibilityPolicy` de la lane cognitiva); el gate temporal no debe ser la tercera.
6. **Perfil inalcanzable por construcción**: type gap (`SessionDescriptive` ≠ `ScoreResult`) + test de
   bytecode `Focus2DependencyBoundaryTest.no_focus2_class_reaches_the_profile_contribution_types` +
   wiring solo-eraser. El scanner Gradle `evidenceBoundaryScanner` NO cubre focus2 (solo
   core/session/cognitive/evidence); la barrera de focus2 es el test de bytecode.
7. **Duplicación genuina registrada** (no introducida por S5): `TrialOutcome` SDT (focus2) vs
   `TrialOutcomeKind` go/no-go (lane cognitiva), sin puente — se documenta, no se «arregla» en S5.
8. El reducer ya guarda cross-epoch (`atElapsedNanos < presentedAt` ⇒ contaminated,
   `FocusSessionReducer.kt:153`) y descarta trial abierto en pausa/interrupt (REDTEAM #21).

## 7. Registro de progreso

- 2026-07-21 · REOPEN recibido; propuesta previa marcada SUPERSEDED; tasks #22–#30 creadas.
- 2026-07-21 · Fuentes leídas: 00/03/06/07 context_pack íntegros; 09 §15/§18/§20.2 (decisiones Focus);
  plan §5/S5; GATE_CATALOG; ADR_REGISTER; REDTEAM matrix; REVIEWS_V5; agent_outputs/05 (orden-2).
- 2026-07-21 · Baseline run 1 → detekt ROJO (S5R-F1) → corregido → run 2 en curso.
- 2026-07-21 · Authority map (S5-02) + data-flow (S5-09) en curso vía 2 agentes read-only sin Gradle.
- 2026-07-21 · Run 2 de gates VERDE con ejecución real (24 executed; 3965/0/0/0 de 559 XML frescos).
- 2026-07-21 · Data-flow map recibido e integrado (§6); 8 gaps factuales registrados (§6.1).
- 2026-07-21 · Authority map recibido e integrado (§6.2). **Paso 0 COMPLETO.**
- 2026-07-21 · Paso 1 lanzado: gate de entrada con efficacy-guardian + psychometrician + kmp-architect +
  privacy-security (read-only, sin Gradle, criterios concretos exigidos).
- 2026-07-21 · **Hallazgo S5R-F2 (documental):** `05_TECHNICAL_CURRENT_STATE` §3/§4.1 seguía diciendo
  «Android 8/API 26» pese a ADR-FOCUS-PLATFORM-MIN (migración documental incompleta — probable origen del
  «API 26» del contrato de reapertura). Corregido citando 09 §20.2.
- 2026-07-21 · Fuentes temporales de plataforma verificadas para el ADR: FrameMetrics API 24 (mirror MS
  Learn); VSYNC/INTENDED_VSYNC 26, DEADLINE 31 (asertadas por contrato, concordantes); CADisplayLink
  timestamp/targetTimestamp = base CACurrentMediaTime/mach_absolute_time (Apple forums/docs) ⇒ regla de
  diseño: el adapter exporta SOLO duraciones/conteos intra-dominio, ningún timestamp crudo cruza la frontera.
- 2026-07-21 · **Paso 1 COMPLETO**: 4/4 dictámenes recibidos e integrados en §8 (eficacia APTO-COND ·
  privacy APTO-COND · psicómetra APTO-COND · arquitecto APTO). Cero discrepancias reales; 4 refinamientos
  de síntesis (§8.3: series prohibidas vs estratos; detectores vs P0-B; dos drenajes por trial; celda
  fundida condición×bucket).
- 2026-07-21 · **Paso 2 COMPLETO**: `ADR_FOCUS_TEMPORAL_QUALITY.md` escrito y CONGELADO (FROZEN_FOR_S5,
  18 secciones: 8 alternativas con veredicto, capacidades por plataforma, buckets de integridad con
  carve-out de truncación, sufficient statistics opción B, composición AND de estados, detectores +
  invariante de monotonía, umbrales sombra versionados, relojes/dominios, schema v2 + N-1 NotCaptured,
  privacidad/retención, no-wiring, plan de validación, 15 criterios de aceptación, riesgos con owner).
  Fila añadida a `ADR_REGISTER_V5.md`; decisión registrada en 09 §20.5 (formato §22, PROVISIONAL).
- 2026-07-21 · **Paso 3 · Fase A (primitivos `core/timing`) VERDE**: `TemporalVocabulary` (TemporalClass
  T0-T3 con T2/T3 sin productor, ClockDomain, ProbeCapability con expectedDomain, InputTimestampPrecision),
  `HzBand` (banding ±5%, @Suppress MagicNumber justificado — los valores del enum SON los datos),
  `TemporalWindowObservation` (dropFraction null si 0 frames — jamás 0.0 fabricado),
  `TemporalQualityThresholds` (config sombra tqt-1), `TemporalWindowVerdict` + `WindowExclusionReason`
  (taxonomía unificada §4.3), `TemporalClassPolicy` (sample-gate-first; codomain {T0,T1} pinneado por
  test exhaustivo; ramas marcador/conservadora del carve-out). Tests: 16 policy + 2 banding.
  Gates EN SERIE con ejecución real: `desktopTest` EJECUTADO (561 XML frescos · **3983/0F/0E/0S**;
  +18 sobre baseline 3965) · `detekt` VERDE tras 1 fix real (MagicNumber HzBand) · `compileAndroidMain`
  VERDE. Aviso config-cache «build.gradle.kts changed» investigado: limpio contra HEAD (cache viejo,
  no mutación paralela).
- 2026-07-21 · **Paso 3 · Fase B1–B3 (probe ventaneado) VERDE**: `WindowedTemporalProbe` (interfaz
  común start/markPresent/drainWindow/stop + capability/clockDomain de primera clase + NoOp explícito)
  + `@Composable expect rememberWindowedTemporalProbe` con 3 actuals: Android (un callback
  auto-reposteado por `start()` — 0 allocs/frame —, contador de GENERACIÓN anti-callback-tardío
  REDTEAM #22, marcador de render `System.nanoTime`↔`frameTimeNanos` mismo dominio uptime, drenaje
  con ancla retenida, buffer pre-dimensionado 1024, Hz real por drain vía View.display), desktop NoOp,
  iOS NoOp interino (port CADisplayLink en cambio propio verificado por CI, ADR §3.1). Gates EN SERIE:
  `desktopTest` EJECUTADO (562 XML frescos · **3985/0F/0E**; +2) · `detekt` VERDE · `compileAndroidMain`
  VERDE (compila el actual Android nuevo).
- 2026-07-21 · **Hallazgo S5R-F3 — la recomendación del arquitecto (importar el enum de estados de la
  lane cognitiva) violaba la barrera consumer-sweep**: `CognitiveSessionEvidenceBoundaryTest.
  no_commonMain_file_outside_the_evidence_package_references_its_public_types` (gemelo desktopTest del
  scanner Gradle) prohíbe que CUALQUIER archivo commonMain fuera del paquete NOMBRE sus tipos públicos
  (allowlist de exactamente 1 archivo, no editable por este agente). La barrera funcionó y cazó el
  primer intento en rojo. Resolución SIN debilitarla: enum LOCAL `FocusEvidenceState` con los 4
  nombres del vocabulario cerrado (la decisión fija los NOMBRES, no la instancia del tipo) + test de
  paridad de nombres en desktopTest (donde la barrera no aplica) para que los dos vocabularios no
  deriven en silencio. La firma de `evidenceOf` del ADR §8 se mantiene; solo cambia el tipo de retorno.
- 2026-07-21 · **Paso 3 · Fase B4 (núcleo focus2) VERDE**: snapshot v2 (`temporalCells` = celdas
  (condición × bucket) con SDT ENTERO en TODAS las filas — Σ buckets ≡ celda oficial, invariante
  testeada — + `excludedByReason` incl. descartes; `openConditionName` para atribuir descartes por
  condición, cerrando el canal vivo P1-C; `temporalSampledFrames`); descriptor v2 (capability/dominio/
  precisión/banda Hz/`temporalObservationVersion`, sellados con defaults «no capturado» fail-closed);
  reducer pliega el MISMO outcome en oficial Y sombra (un verdict EXCLUDED sigue contando oficialmente
  — en S5 nada por-ventana altera el camino oficial, ADR §15); `discardOpenTrial(reason)` unifica
  pausa/epoch en la taxonomía tipada con consistencia `contaminatedCount == Σ descartes`; holder posee
  el probe (dos drenajes por trial: ISI jamás clasifica ventana), slack por dossier derivado
  `W − ceiling`; `focus2/domain/evidence` NUEVO: `FocusTemporalEligibility` (NotCaptured como tercer
  veredicto — trampa 0/0; causas NON_AUTHORITATIVE_CLASS/COVERAGE_INCOMPLETE/SESSION_BACKSTOP) y
  `FocusEvidenceStatePolicy` (OFFICIAL_ELIGIBLE INCONSTRUCTIBLE por test exhaustivo; NotCaptured NO
  degrada accuracy; backstop ⇒ TRAINING_ONLY; `rtShadowAdmissible` = modalidad ∧ NANOS_NATIVE ∧
  Eligible). Restore N-1 verificado con fixture v1 real (⇒ NotCaptured, jamás Eligible/Excluded);
  futuro sigue rechazado. +23 tests. Gates EN SERIE: `desktopTest` EJECUTADO (566 XML frescos ·
  **4008/0F/0E**) · `detekt` VERDE · `compileAndroidMain` VERDE.
- 2026-07-21 · **Paso 3 · iOS CADisplayLink port ESCRITO** (`WindowedTemporalProbe.ios.kt`): NSObject
  listener con `@ObjCAction` + `NSSelectorFromString` (convenciones cinterop del learning 2026-07-21),
  `NSRunLoopCommonModes` (los frames siguen llegando durante touch-tracking), generación anti-stale,
  Hz AUTO-DETECTADO por mediana de deltas (clasificar contra el máximo del panel ProMotion leería el
  pacing normal a 60 Hz como drops ⇒ falso negativo prohibido), marcador SIEMPRE ausente (límite CMP
  declarado, rama conservadora). `detekt` VERDE (escanea iosMain). **Compilación iOS = CI macOS,
  pendiente para el gate de salida (Paso 7)** — el run verde actual del CI ancla en ee65a55, no en
  este diff.
- 2026-07-21 · **Pendientes declarados del Paso 3** (ninguno silencioso): (a) gate de resurrección
  `mindboost_focus2` (privacy P1-01): cablear `SubjectRevocationAuthority` o cierre CONDITIONAL con
  owner — SIGUIENTE sub-fase; (b) productor de `PRE_ONSET_RESPONSE` + piso de anticipación corregido
  por marcador (eficacia P2-E): exige plumbing del eventTime de input de plataforma hasta el punto de
  clasificación — declarado para Paso 5/6, el miembro de la taxonomía ya existe sin productor;
  (c) upgrade del adapter Android a listener `FrameMetrics` (report-loss `dropCountSinceLastInvocation`
  ≠ frames limpios, DEADLINE 31+): declarado para Paso 5 — el actual v1 mide cadencia Choreographer
  (T1 igualmente; el precedente Speed usa la misma señal); (d) captura real de capability/dominio en el
  SELLADO del descriptor exige el punto de montaje del composable (los defaults «no capturado» son
  honestos hasta entonces).
- 2026-07-21 · Paso 1: dictámenes recibidos de efficacy-guardian, psychometrician y privacy-security
  (los tres APTO CONDICIONAL); integrados en §8. kmp-architect pendiente. **[CORREGIDO abajo: kmp-architect
  entregó APTO; §8 entera SUPERSEDED por el gate R2.]**
- 2026-07-21 · **REAPERTURA «P3 A–B4» (contrato CEO).** El ADR pasó FROZEN_FOR_S5 → REOPENED_AFTER_AUDIT →
  **FROZEN_FOR_S5_R2**. §8 (síntesis del gate de entrada R1) queda SUPERSEDED por el gate de 7 lentes de la
  reapertura, reconciliado en **ADR R2 §20**; no citar §8 como vigente.
- 2026-07-21 · **Gate de entrada R2 (7 lentes read-only, sin Gradle, sin editar): COMPLETO.** eficacia ·
  psicómetra · kmp-architect · privacy-security · mobile-performance · assessment-psychometrics ·
  security-privacy. 12 hallazgos S5R-AUD-01..12 resueltos por decisión (ADR R2 §2–§20) + 4 nuevos
  (F2 borrado colateral · PRIV-D1 retención sin TTL · ND-1 D sub-slack · F4 orden de consent). DISCREPANCIA
  psicómetra (`S_full`-o-`Absent`) vs assessment (`d′_wc`) ADJUDICADA a `S_full`-o-`Absent` (charter §14).
- 2026-07-21 · **Monotonía PROBADA, no afirmada** (S5R-AUD-03): excluir 5 misses con el scorer real sube
  d′ 1.310708→1.679992 y CI-inf 0.716380→1.023001; la R1 §10.3 «menos n ⇒ CI más ancho» NO cerraba la
  no-inflación. R2 §6.1/§10.3 la sustituye por el gate `S_full`-o-`Absent`.
- 2026-07-21 · **Reclasificación (S5R-AUD-11):** capa de PLATAFORMA de P3 = `CONTRACT_INERT_NO_WIRING`
  (actuals solo compile-verified, sin caller de runtime); capa de DOMINIO se prueba vertical con harness
  `desktopTest`. Montaje productivo = slice **S6** (ADR R2 §19). `user_outcome` del plan corregido: DELETE
  local entregado, EXPORT diferido con owner+gate (S5R-AUD-09).
- 2026-07-21 · **ADR R2 escrito y RE-CONGELADO** (20 secciones, internamente consistente). PENDIENTE:
  implementación (contrato steps 5–14: marker→proxy+conservador · Hz finite-guard · buffer cap · gate
  `S_full`-o-`Absent`+`mg-1` · quitar PRE_ONSET/ONSET · validador de tallies · bins de tercio ·
  `TemporalCapture` requerido · persistir thresholdsVersion/modelVersion · cablear revocación+borrado
  subject-scoped · retención · fix F4 · harness de dominio · scanner de estado) + docs + gate de salida
  8 lentes + cierre 16§ + ledger reseal. Techo honesto = **CONDITIONAL** (Paso 6 físico + runtime iOS no
  ejecutables en este host).
- 2026-07-22 · **Paso 7 (reconciliación docs + reseal del ledger) HECHO.** La R2 sigue FALSADA
  (`VERIFIED_PRIMARY`, domina cualquier APTO). Reconciliados `05/09/register/status`; creados
  `CURRENT_AUTHORITY_INDEX.md`, `PROVENANCE_INCIDENT_V5_BUNDLE.md`, `S5_R3_EVIDENCE_CAPSULE.md/.json`, scanner de
  discoverability (`scripts/validate-authority-entrypoints.mjs`, `--self-test` OK); `MB-FOCUS-0047/0048` seeded
  OPEN + **ledger resellado** (validador VERDE). **T5 (barrido plan/GATE_CATALOG):** `GATE_CATALOG_V5.md` y
  `FOCUS_ENGINE_MASTER_PLAN_V5.md` en DENYLIST (custodia D, NO editados); los claims temporales inflados de la R2
  viven en el ADR temporal (editable, R3) y en el status, no en el plan/catálogo congelados — corregidos ahí, no
  en el bundle. Bundle 16/16: **13 fieles, 2 birth-defects** (`GATE_CATALOG`/`EXERCISE_PORTFOLIO`,
  `NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`), `INPUTS_SHA256.txt` recuperable en worktree. «Gate 6/6+7/7 APTO» =
  `MISSING_PRIMARY_EVIDENCE` (tracker no conservado). R3 = **CANDIDATA** para auditoría externa; NO congelada, NO
  montada; código intacto (Paso 7 = docs-only).
- 2026-07-22 · **Reaudit FULL_SCOPED (7 lentes) + correcciones.** Gate de salida adversarial sobre el árbol final:
  security APTO; efficacy/psychometrics/timing-kmp/privacy/completeness APTO_CON_OBSERVACIONES; integrator-diff
  NO_APTO (1 P0). **P0 corregido:** ledger/cápsula listaban `S5_R3_P7_HANDOFF.md` como creado antes de existir →
  handoff creado de verdad. **P1 corregidos:** (a) `D-R3-04` cualificada (account-deletion CABLEADO cubre el
  borrado inmediato; subject-revocation focus2 = PENDING, `MB-FOCUS-0048`); (b) residuo GATE_CATALOG registrado
  (abajo); (c) `S5_R3_PASO7_PLAN.md` anotado SUPERSEDED-BY-OUTCOME. **P2:** d′ canonicalizados a los valores del
  scorer real **1.310847/1.680016/1.292908** (triple-verificados por 3 probits independientes; antes 1.3107/1.6805/
  1.2933 mal-redondeados). **T5 resultado literal:** grep read-only sobre `FOCUS_ENGINE_MASTER_PLAN_V5` +
  `GATE_CATALOG_V5` = **0 hits de claim falso** (las 3 «exactitud» son la negación honesta «precisión ≠ exactitud»
  o descripción del scorer). **RESIDUO CONOCIDO:** `GATE_CATALOG_V5.md` L9 «todos los gates DESIGNED_NOT_RUN» es
  stale (snapshot `27849450`, pre-S0–S4; INFRA-declara los PASS del ledger) — NO editado (custodia D); estado LIVE
  en `CURRENT_AUTHORITY_INDEX §4/§4.2`.

## 8. Gate de entrada (Paso 1) — dictámenes y síntesis · **[SUPERSEDED 2026-07-21 por el gate R2 → ADR R2 §20; conservado como historia]**

Dictámenes completos en los output files de los agentes (read-only, sin Gradle). Aquí: veredicto,
bloqueantes y la síntesis que el ADR de Paso 2 DEBE incorporar. Síntesis por jerarquía de autoridad
(06 §15): un P0 demostrado no se neutraliza por consenso.

### 8.1 Veredictos

| Lente | Veredicto | Bloqueantes para congelar el ADR |
|---|---|---|
| efficacy-guardian | APTO CONDICIONAL | P0-A (accuracy no inmune al jank), P0-B (exclusión RT inerte en v1; accuracy solo vía carve-out+sesión), P1-C (cherry-picking por condición), P1-D (discontinuidad ⇒ INVALIDATED de sesión, monotónico-derivada) |
| psychometrician | APTO CONDICIONAL | P0-1 (estrechar «accuracy conservada» + 2 detectores), P0-2 (SdtCell condición×clase), P0-3 (UNAVAILABLE ≠ CLEAN) + arnés anti-vacuidad P2-1 |
| privacy-security | APTO CONDICIONADO | P0-01 (retención explícita por campo), P0-02 (prohibir series por-ventana persistidas) + P1-01 (resurrección `clearNamespace` vs `checkpoint()`) antes del cierre de S5 |
| kmp-architect | APTO | Sin bloqueantes; 6 decisiones de diseño obligatorias para el ADR (puntos 21–27 de §8.2) |

### 8.2 Síntesis convergente — obligaciones del ADR (Paso 2)

**Reencuadre dominante (eficacia ≡ psicómetra, independientes):** el runtime es tally-only (descarta la
latencia al clasificar) y ningún RT es oficial en v1 ⇒ la «exclusión de RT por ventana» es bookkeeping/
shadow; el riesgo psicométrico de primer orden es la **corrupción de ACCURACY mediada por timing**. La
premisa «accuracy conservada salvo discontinuidad» es FALSA tal como estaba enunciada.

1. **Carve-out de truncación.** Accuracy conservada SOLO en jitter-sin-truncación. Ventana con
   (i) pérdida de integridad del onset, (ii) ventana acortada por render tardío, o (iii) discontinuidad
   ⇒ trial completo `EXCLUDED` tipado (nunca MISS/OMISSION atribuido al usuario). Canales verificados en
   código: piso de anticipación reclasifica respuestas reales fuera de `signalTrials`
   (`DescriptiveModel.kt:72-73`) y onset tardío fabrica OMISSION espuria (`:80`; en vigilancia el efecto
   es máximo). Render marker (05 §C.3) = prerrequisito para distinguir; sin él, drops>umbral ⇒ exclusión
   de trial completo (conservador).
2. **Ventana de observación = commit→resolución.** El jank de ISI queda FUERA (CA-1.1: jank solo en ISI
   ⇒ resultado byte-idéntico). El inicio efectivo es el frame-commit del estímulo, no el dispatch de
   `PRESENT_TRIAL` (hoy `presentedAt` se captura en dispatch — defecto de anclaje).
3. **Sufficient statistics estratificados.** `SdtCell` por condición × clase-temporal, conteos ENTEROS;
   d′ por condición sobre la UNIÓN ADMISIBLE de clases (la clase es gate de admisibilidad, NO estrato de
   scoring — evita reventar `MinSampleGate.DPRIME` por estratificación, learning 2026-07-20).
4. **UNAVAILABLE ≠ CLEAN.** Clasificación gatea primero en `sampledFrames >= MIN_FRAMES`; probe
   NoOp/UNAVAILABLE ⇒ NON_AUTHORITATIVE (nunca CLEAN, nunca RT-elegible; accuracy conservada). Es la
   trampa ausencia-fabrica-máximo (learning 2026-07-15) y resuelve la paridad iOS honesta (§4.6).
5. **Discontinuidad de reloj.** Derivada SOLO de invariantes del monotónico (jamás comparación con
   wall-clock — falsificable por el usuario) ⇒ INVALIDATED de SESIÓN; nunca drop selectivo de accuracy
   por ventana. `FrameStats` no tiene el campo hoy — el ADR especifica dónde se detecta.
6. **Anti-cherry-picking.** Toda exclusión/descarte (incl. el pause-discard VIVO, hoy `contaminatedCount`
   global por sesión) se cuenta POR CONDICIÓN × MÉTRICA y contra `nMinPerCell`; `excludedFraction` como
   cantidad de primera clase; detector de exclusión diferencial (fracciones por condición, flag si
   `max−min > δ` ⇒ contraste `Absent(DIFFERENTIAL_TEMPORAL_EXCLUSION)`, niveles sostenibles); detector
   de tendencia temporal (thermal/degradación creciente ⇒ RT-sesión
   `Absent(NON_EXCHANGEABLE_TIME_TRUNCATION)`); tope de fracción excluida. REDTEAM #11 NO cubre la
   selección intra-sesión por condición — fila nueva para Paso 5.
7. **Invariante de monotonía (3 cláusulas, property-based).** (a) no-upgrade de estado de elegibilidad;
   (b) no cambio-fiable fabricado; (c) el CI-lower nunca SUBE por exclusión (conducta fija). Caso
   peligroso: excluir solo ventanas-error no puede subir el estimado (errores retenidos salvo outcome
   corrompido). Unificar `contaminated` (interrupción) y `temporally-excluded` (jank) bajo UNA taxonomía
   tipada de reason codes (sin doble conteo).
8. **`AbsenceReason` nuevos:** `INSUFFICIENT_ADMISSIBLE_N`, `DIFFERENTIAL_TEMPORAL_EXCLUSION`,
   `NON_EXCHANGEABLE_TIME_TRUNCATION`.
9. **Composición de alternativas ratificada (tabla psicómetra §P6):** núcleo = exclusión por ventana ×
   elegibilidad por métrica (3×4) + inflar incertidumbre como tier medio DEGRADED-usable (6) + backstop
   de sesión >umbral/clock-disc (1) + terminal `TRAINING_ONLY` (7). Alt 5 (down-weight fraccional)
   RECHAZADA con motivo estadístico: N fraccional destruye la distribución binomial que sostiene
   `var(d′)` (`DescriptiveScorer.dPrimeSe`) y hace indemostrable la monotonía. Alt 2 N/A (no existe
   subestructura de segmentos). Alt 8 fuera de alcance (cambio persistente de comparabilidad, no jank).
10. **Retención explícita por campo (privacy P0-01).** Cada campo declara: write-ahead efímero (purgado
    en `complete()`/`discard()`) o registro permanente con ventana de edad (patrón
    `retentionPolicyVersion`). «Indefinido por omisión» prohibido.
11. **Prohibición de series (privacy P0-02).** Nada ordenado por ventana/trial se PERSISTE; solo
    escalares/conteos por enum cerrado. Las estructuras por-ventana del cómputo viven en memoria durante
    la sesión y se descartan al agregar. (Compatible con la estratificación del punto 3: estratos =
    conteos por celda, no secuencias.)
12. **Minimización (privacy P1-02).** Hz detectado persistido → BANDA (60/90/120/144/240/OTHER), nunca
    el float; thermal → 3 niveles (NORMAL/THROTTLED/SEVERE), no los 7 nativos; battery-saver boolean ok.
13. **Dos structs (privacy P1-03).** Capabilities FIJAS selladas en `FocusSessionDescriptor` (patrón
    `inputModality`) ≠ observaciones AGREGADAS calculadas al `complete()`. «Hz nominal declarado» ≠
    «Hz detectado» (campos distintos).
14. **Consentimiento (privacy P2-01).** Tier resúmenes+observaciones técnicas = necesario-para-servicio
    (PRI-001), estructuralmente independiente de `TRIAL_JOURNAL`; test: revocar TRIAL_JOURNAL mid-sesión
    no altera los sufficient statistics. Finalidad nueva (analítica cross-usuario) exigiría su propio
    control — sin sink genérico legible.
15. **Resurrección (privacy P1-01).** Carrera `checkpoint()`-tras-`clearNamespace()`: cablear
    `SecureFocusSessionStore`/`SecureFocusConsentStore` a `SubjectRevocationAuthority` antes del cierre
    de S5, o declarar owner+gate explícito. Test de resurrección bloqueante. El coverage test de erasure
    NO cubre esta carrera (límite de alcance documentado).
16. **Piso de anticipación anclado al onset** (render marker) + invalidación tipada
    `PRE_ONSET_RESPONSE` (tap con eventTime < commit del estímulo jamás es HIT ni anticipación).
17. **Claims.** La clase T jamás se surfacea como badge de calidad cognitiva; sin RT en ms absolutos
    («precisión» acotada ≠ exactitud — sin banco de fotodiodo no hay exactitud); `WITHHELD_TECHNICAL`
    en lenguaje neutro; `mindboostCopySafetyScanner` sobre cadenas nuevas.
18. **10 simulaciones obligatorias con oráculo exacto** (tabla psicómetra §P5: MCAR/MNAR-errores/
    MNAR-aciertos/60-vs-120Hz/pequeño-limpio-vs-grande-degradado/thermal-creciente/UNAVAILABLE/
    todo-degradado/degenerate+jank/exclusión-diferencial) + arnés anti-vacuidad (`requirePresent` +
    aserciones de conteos + mutación «todo bajo piso» ⇒ contar rojos).
19. **Export (privacy P1-04).** No existe export de usuario en TODO el repo (único precedente:
    `CreativityExporter`, huérfano). S5 no lo construye; diseña los DTOs `@Serializable` nombrados para
    que un futuro `FocusTemporalExporter` sea formateador puro; gap declarado con owner+gate
    (PRI-004/005/006).
20. **Aritmética de tamaño pinchada.** Presentados ≥ (40/condición)/(1−e): flanker 2-cond ≈ ≥95 a
    e=0.15; vigilancia (20% objetivos) ≈ ≥118 (celda de señal = cuello de botella). El ADR pincha
    números y su derivación, no «20».
21. **Ubicación (arquitecto Q1).** Primitivos físicos en `core/timing` (feature-agnóstico: `TemporalClass`,
    `ClockDomain`, `ProbeCapability`, `TemporalWindowObservation`, `TemporalClassPolicy` + umbrales
    versionados, `WindowedTemporalProbe` + `@Composable expect`); composición con elegibilidad en
    `focus2/domain/evidence` (nuevo). Tercer vocabulario temporal (vs Speed y lane cognitiva) es
    DELIBERADO (ADR-FOCUS-TIMING lo ratificó); reconciliación futura = follow-up declarado con owner.
22. **Autoridad de estados (arquitecto Q2).** focus2 importa el ENUM `SessionEvidenceState` (tipo sin
    dependencias) y escribe su PROPIO mapeo `FocusEvidenceStatePolicy.evidenceOf(writeAheadAdmissible,
    doseComplete, rtModalityAdmissible, temporalEligibility, verdict)` llamado desde `complete()`;
    JAMÁS fabrica un `CognitiveSessionRun` sintético; `OFFICIAL_ELIGIBLE` es INCONSTRUCTIBLE (test
    exhaustivo).
23. **Probe (arquitecto Q3).** Interfaz plana + `@Composable expect fun remember...` (patrón Speed);
    inyectado por constructor al holder (misma disciplina de posesión que `clock`); registro 1×/sesión,
    DRENAJE por trial sin desregistrar (la deriva térmica/VRR de sesión necesita continuidad); contador
    de generación (Int) contra callbacks tardíos (REDTEAM #22); report-loss con motivo tipado explícito;
    la observación viaja como DATO en `FocusRuntimeInput.Respond`/`WindowExpired` — el reducer sigue puro.
24. **Dominios de reloj (arquitecto Q4).** `ClockDomain` sellado en el descriptor
    (`ANDROID_CHOREOGRAPHER_UPTIME` / `IOS_CADISPLAYLINK_MACH_TIME` / `NONE_MONOTONIC_FALLBACK` /
    `UNKNOWN` fail-closed); NINGÚN tipo del vertical expone un timestamp absoluto — la resta
    cross-dominio es imposible por construcción de tipos, no por convención.
25. **Schema (arquitecto Q5).** Descriptor `SCHEMA_VERSION` 1→2 (+capability/clockDomain) y snapshot 1→2
    (+`temporalTallies` default `emptyList()`); componentes = autoridad, agregado = función pura derivada
    JAMÁS persistida; N-1: lista vacía ⇒ tercer veredicto `NotCaptured` (ni `Eligible` ni `Excluded` —
    la trampa 0/0 fabrica máximo o mínimo); `temporalCoverageComplete` (= conteo de tallies ==
    deliveredTrials) como input del AND (resume cross-build con cobertura parcial nunca juzga RT en
    silencio).
26. **iOS (arquitecto Q6).** Port CADisplayLink EN S5 (CI macOS ya probado, cero pods nuevos) ⇒ techo
    estructural iOS sube T0→T1, mismo techo que Android (T2 exige banco físico, plataforma-agnóstico).
    Límite epistémico DECLARADO en el KDoc: CADisplayLink observa la cadencia de refresco del sistema,
    NO el frame que Compose/Skia efectivamente compositó (a diferencia de Android donde la View ES la
    superficie) — proxy de jank, no ancla de commit. Los probes NoOp reportan
    `ProbeCapability.NONE_MONOTONIC_FALLBACK` como HECHO de primera clase sellado — jamás inferido de
    stats vacías. `CONDITIONAL` se reserva para lo que exige hardware (Paso 6), no para código
    escribible hoy.
27. **«Segmento» reformulado.** El «por segmento» del orden-2 (C.7) se reformula SIN pérdida como
    histograma por-trial-clasificado; NO se introduce ninguna subestructura de segmento persistida
    (Tracer A segmenta por conteo, Tracer B por tiempo — el trial es el denominador común; la regla
    «>50% NO-GO» pasa a proporción sobre el histograma).

### 8.3 Discrepancias y refinamientos de síntesis

Ninguna discrepancia real; cuatro tensiones aparentes, resueltas:
- Estratificación por clase (psicómetra P0-2) vs prohibición de series (privacy P0-02): compatibles —
  los estratos son conteos agregados por celda cerrada; el cómputo por-ventana es efímero en memoria.
- «Ninguna señal por-ventana baja accuracy» (eficacia P0-B) vs detectores del psicómetra: compatibles —
  los detectores degradan ELEGIBILIDAD/CONTRASTE fail-closed (`Absent` tipado), nunca alteran
  selectivamente los conteos de accuracy supervivientes.
- Drenaje en resolución (arquitecto Q3) vs «el jank de ISI queda fuera» (eficacia CA-1.1): el ADR fija
  DOS drenajes por trial — al presentar (intervalo ISI → solo tendencia de sesión térmica/VRR) y al
  resolver (intervalo de ventana → clasificación del trial). El ISI jamás alimenta la clase de la
  ventana; la continuidad del probe se conserva.
- Histograma por clase (arquitecto) vs `SdtCell` por condición×clase (psicómetra): el ADR funde ambos —
  la celda persistida es (condición × bucket de integridad) con contadores SDT ENTEROS + conteos de
  exclusión por razón; el histograma puro por clase es una proyección derivada, nunca un campo.

### 8.4 Unknowns consolidados (→ Paso 4 simulación / Paso 6 físico)

1. D máximo real (retardo estado→frame-commit) por banda de dispositivo — protocolo físico 05 §C.
2. Tasa esperada de exclusión `e` por banda — gobierna pisos de sesión y el umbral de backstop.
3. Overhead real del probe en hot path (cero-alloc; un GC del propio probe fabricaría jank).
4. Paridad iOS: sin CADisplayLink portado, iOS ⇒ UNAVAILABLE ⇒ NON_AUTHORITATIVE (cierre CONDITIONAL
   honesto aceptable, §4.6).
5. Detección VRR/LTPO por ventana (C.1, riesgo top-1 del orden-2): sin Hz real por segmento, toda
   clase T>0 es ciega >60 Hz.
6. Holgura real del tracer B (vigilancia S4) — si su ventana es más ajustada que el flanker, el riesgo
   de truncación es mayor.
