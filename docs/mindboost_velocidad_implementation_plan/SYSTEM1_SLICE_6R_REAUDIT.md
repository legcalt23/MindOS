# SYSTEM-1 · Slice 6 REOPENED (6-R) — Adversarial re-audit of the ingestor contract

> ⚠️ **SUPERSEDED (2026-07-10) por SYSTEM1_SLICE_6R2_REOPEN.md.** El CEO reabrió de nuevo (verdicto independiente
> = REOPEN, no CONDITIONAL): 6-R protegía el RANGO pero NO el perfil provisional (el `ProfileEstimate` tenía la
> forma 1:1 de `ModuleContribution`; el agregador no era barrera). El contrato `profile/domain/ingestion/` descrito
> aquí (incl. `ProfileScorePolicy`/EWMA, `PerformanceTrendPolicy`, `ProfileEstimate`, `CalibrationState`) fue
> **BORRADO** y reemplazado por el contrato EVIDENCE-ONLY en `core.session.cognitive.evidence`. Léase solo como
> registro histórico. Estado vigente: `SYSTEM1_SLICE_6R2_REOPEN.md`.

**Fecha:** 2026-07-10 · **Estado:** SUPERSEDED por 6-R2 · **Alcance:** PURO, NO CABLEADO. Sin Firestore/SyncQueue/outbox/E9/perfil vivo/ranking vivo/cloud.

> Objetivo: intentar FALSAR el contrato de slice 6 y dejarlo incapaz de alimentar un perfil o rango engañoso.
> Cada riesgo "before wiring" capaz de alterar score/calibración/perfil-provisional/rango se resuelve en el
> contrato puro O se blinda con una barrera fail-closed que impida cablearlo por accidente. Ningún P0/P1
> disfrazado de follow-up.

---

## §1 · Inventario de evidencia (ground-truth verificado en el repo, no en el resumen)

Autoridades reales leídas (file:símbolo):

- `core/session/cognitive/CognitiveSessionEligibility.kt` → **`SessionEligibilityPolicy.evaluate`** — kill-switch
  HARD y EJECUTABLE que ya sella `profileEligible`/`calibrationEligible`/`rankingEligible` AND-gating: completed ·
  write-ahead · dataQuality · timing≠COMPROMISED · duración≥120 000 ms **o** `ApprovedProtocol` · purpose ·
  (kill-switch de medida) notPreliminary · VALID · confident · **authoritativeTimebase** · **antiGamingCoverage**.
  Además emite `EligibilityTrace` (por-gate + `rejectionReasons` + `officialUseEligible`) = provenance versionable.
- `profile/domain/calibration/CalibrationPolicy.kt` → `EMERGING_MIN_SESSIONS=2`, `ESTABLISHED_MIN_SESSIONS=5`,
  `ESTABLISHED_MIN_DISTINCT_DAYS=3`; `summarize` (dedup by id, distinct days) + `eligibleCeiling`.
- `profile/domain/calibration/ProfileSessionEligibility.kt` → `MIN_DURATION_MS=120_000`, duración+evidencia.
- `profile/domain/model/ModuleContribution.kt` → `normalizedScore:Double?`, `dataSufficiency`, `confidence`,
  `trend`, `preliminary=true` default, `sessionEvidence?`. Invariante `0..1`.
- `profile/domain/aggregation/MindBoostProfileAggregator.kt` → `AggregationConfig(minSufficiency=0.5,
  minConfidence=0.4, establishedConfidence=0.7, minEstablishedForComposite=3)`; `value` = media ponderada por
  `confidence*dataSufficiency` de `normalizedScore` ⇒ **el `normalizedScore` de la contribución ES el valor de
  la dimensión mostrado como estimación actual**.
- `core/scoring/normalization/InterSessionConfidence.kt` → confianza por **CV** (escala-sensible; penaliza al
  bajo-pero-estable — auto-documentado como limitación Fase C).
- `core/session/cognitive/CognitiveSessionRun.kt` → lleva `blockId,exerciseId,exerciseVersion,scoringVersion,
  sessionPurpose,started/completedAtEpochMs,durationActualMs,completionStatus,difficulty,score,eligibility,
  eligibilityTrace,anomalyFlags,...`. **NO lleva:** `userId` (namespace-bound), zona horaria IANA,
  `normalizationVersion`/`scaleId`, `subskillId`, `locale`.
- PRD `docs/MindBoost_PRD_Completo.md` §12.4/§12.6 (evaluación inicial → perfil PROVISIONAL; rango tras 5
  sesiones válidas/bloque) · §14.2 score sesión 0–100 · §14.3 `NewBlockScore=0.85·prev+0.15·session` (EWMA post) ·
  §14.4 `CalibrationBlockScore=average(last 5 valid)` · §14.9 calibración=5 válidas·no abandonadas·≥120s·sin
  anomalías extremas · §14.11 GlobalScore media 8 bloques, rango global ≥5/8 calibrados · §14.14 inactividad 1 mes
  ⇒ baja rango + recalibra · §14.15 **anti-volatilidad: tendencia, no ruido**.

Estado git: los archivos de slice 6 están **untracked** (`?? .../profile/domain/ingestion/`), diff aislado;
no toco WIP ajeno. Los 2 findings foráneos de detekt (`IndexedSecureKeyValueStore.kt`, `InterSessionConfidence.kt`)
NO se tocan.

### Matriz de trazabilidad (PRD / autoridad ↔ implementación actual ↔ test ↔ estado)

| # | Requisito | Fuente de verdad | Implementación ACTUAL (slice 6) | Test actual | Estado | Acción 6-R |
|---|---|---|---|---|---|---|
| R1 | Estimación de módulo = tendencia, no la última sesión | PRD §14.3/§14.4/§14.15 | `normalizedScore = latest.score.normalized` (línea 130) | test 13 fija "latest" | **CONFLICT** | Implementar `ProfileScorePolicy` (avg-last-5 en calibración, EWMA 0.85/0.15 después); exponer `latestSessionScore` aparte |
| R2 | Rango oficial tras 5 válidas/bloque, provisional antes | PRD §12.4/§14.9 | No hay lane de rango; `CalibrationState.calibrated=status==ESTABLISHED` suena a rango | tests 1,2,16 | **GAP** | `RankReadiness` fail-closed (NOT_RANK_ELIGIBLE); `calibrated` deja de existir como verdad almacenada |
| R3 | TRAINING_FREE no calibra rango solo por purpose | CEO §3 / PRD §14.9 | `qualifiesForCalibration` no consulta `rankingEligible` ni blueprint | test 9 | **GAP** | Rank lane exige `rankingEligible`+blueprint+48h+cap/día ⇒ fail-closed |
| R4 | Perfil provisional posible desde evaluación inicial | PRD §12.4/§12.6, CEO §3 | contribución 1-sesión ⇒ `dataSufficiency=0.2`; agregador `minSufficiency=0.5` la DESCARTA | — | **MECH-BLOCK** | Exponer `provisional` distinto + TEST que demuestra el descarte del agregador (bloqueante mecánico registrado) |
| R5 | Revalidación integridad vía oficial | CEO §4 | re-deriva completed/quality/timing/VALID/prelim/confident; confía `profileEligible` p/ timebase+antigaming | tests 3,7,18 | **PARTIAL-OK** | Consumir `eligibilityTrace` (provenance real); no reclamar "re-derived" de timebase/antigaming |
| R6 | Comparabilidad de escala | CEO §6 | mezcla todas las sesiones del bloque sin ScaleKey | — | **GAP** | `ScoreScaleKey(block,exercise,exerciseVersion,scoringVersion)`; solo combina misma key; rank fail-closed sin blueprint/normalizationVersion |
| R7 | `confidence` ≠ estabilidad CV | CEO §7 | `confidence = InterSessionConfidence` (CV) presentado como confianza | test 20 | **CONFLICT (nombre)** | Renombrar a `stabilityHeuristic` (versionado) en el output; `scoreConfidence=null`(UNKNOWN); separar `sessionProgress`/`temporalCoverage` |
| R8 | Tendencia fiable | CEO §8 | first-vs-last + ε0.05 fijo; ignora el medio | test 17 (documenta V-shape roto) | **CONFLICT** | `descriptivePerformanceTrend` (pendiente sobre todos los puntos, deadband declarado heurístico; nunca "cambio fiable") |
| R9 | Recencia/inactividad | PRD §14.14, CEO §9 | ausente | — | **GAP** | `lastQualifiedEvidenceAt` + `FreshnessStatus` (STALE ≥30d) con `nowMs` INYECTADO |
| R10 | Determinismo/identidad | CEO §10 | dedup by id; `sortedBy{orderKey}` **sin desempate**; sin conflicto de id; sin exclusión de futuros | test 12 | **GAP** | tie-break por sessionId; conflicto id→fail-closed; excluir timestamps futuros (con `nowMs`) |
| R11 | Doble verdad status+calibrated | CEO §2/§10 | `calibrated` almacenado además de `status` | test 1 | **CONFLICT** | Eliminar el bool almacenado; derivar `status.isAtLeast(ESTABLISHED)` |
| R12 | Escala tipada [0,1] vs [0,100] | CEO §5 | todo en `0..1`; PRD muestra 0–100 (display) | invariantes | **OK (documentar)** | El ingestor nunca toca 0–100; documentar frontera de display única; no introducir Doubles ambiguos nuevos |
| R13 | Política de calibración V1 (5+3días+48h+max2/día) formalizada | CEO §2 | `CalibrationPolicy` 5+3días; sin 48h/cap; PRD sin 3 días | tests 1,16 | **CONFLICT (PRD↔code)** | Ratificar V1 en PRD (cambio de producto versionado); rank policy nueva con 48h+cap; DC-3 maturity sigue para PERFIL, no rango |

**Contradicciones núcleo identificadas (gate §1 cumplido):** R1 (latest≠estimación), R2/R3/R11 (calibrated≈rango, doble verdad, sin lane de rango real), R4 (agregador descarta provisional), R6 (sin comparabilidad), R13 (PRD sin 3 días mientras code sí; CEO ratifica V1 más estricto).

---

## §2–§10 · Decisiones de diseño 6-R (resolución)

**Principio rector:** el ingestor NO puede producir hoy un rango ni una "estimación actual de módulo" autoritativa
para rango. Lo que SÍ produce: (a) un **perfil provisional** honesto (avg-last-5/EWMA, `preliminary=true` siempre);
(b) una **madurez DC-3** para el PERFIL (EMERGING/ESTABLISHED), explícitamente NO-rango; (c) un **veredicto de rango
fail-closed** (`NOT_RANK_ELIGIBLE`) con razones estructurales que un ingeniero de wiring no puede saltarse.

Lanes (§3), visibles en `EvidenceSummary`, no escondidas en dos booleanos:
- **PROVISIONAL** (incl. INITIAL_ASSESSMENT): `isMeasurementValid && profileEligible && purpose.mayContributeToProfile`.
- **CALIBRATION (madurez de perfil)**: `+ calibrationEligible && purpose.mayContributeToCalibration` (DC-3).
- **RANK (oficial)**: `+ rankingEligible && purpose.mayContributeToRanking && sameScaleKey && blueprintSatisfied && tzCaptured`
  → hoy **imposible** (`NO_CALIBRATION_BLUEPRINT`, `TIMEZONE_NOT_CAPTURED`) ⇒ barrera fail-closed.

Autoridades nuevas (todas puras, versionadas):
- `ProfileScorePolicy` (§5): `currentEstimate` = avg(last 5) en calibración; EWMA `0.85·prev+0.15·session` sembrado
  con la media de calibración, después. `latestSessionScore` separado. Reconstrucción determinista + goldens.
- `RankReadiness` + `RankPolicy` (§2/§6): MIN_RANK_SESSIONS=5, MIN_DISTINCT_DAYS=3, MIN_SPAN_MS=48h,
  MAX_SESSIONS_PER_DAY_ACCELERATE=2, fail-closed por blueprint+tz.
- `ScoreScaleKey` (§6): (block,exercise,exerciseVersion,scoringVersion). Solo combina misma key; distinta ⇒ usa la
  más reciente y cuenta la excluida; rank exige verificación de escala (no existe `normalizationVersion`) ⇒ fail-closed.
- `EvidenceSummary` (§4): totales, por-lane, exclusiones por `ExclusionReason` (enum), `usedScaleKey`, primera/última,
  días, `freshness`, `ruleVersion`. Sin PII (sin ids; conteos + tiempos coarse).
- `FreshnessStatus` (§9): FRESH/STALE/RECALIBRATING/UNKNOWN; `nowMs` inyectado; UNKNOWN si falta.

Honestidad de señales (§7/§8): output expone `sessionProgress`, `temporalCoverage`, `stabilityHeuristic`
(renombrado, versionado, "no es intervalo de confianza ni fiabilidad"), `scoreConfidence=null` (UNKNOWN),
`descriptivePerformanceTrend` (pendiente sobre todos los puntos; deadband declarado heurístico). `ModuleContribution`
(tipo compartido, 9 consumidores) NO se renombra: el mapeo documenta `confidence:=stabilityHeuristic`,
`dataSufficiency:=sessionProgress`, `preliminary=true`.

Determinismo (§10): sort `compareBy(orderKey, sessionId)`; conflicto de id (mismo id, payload distinto) ⇒ excluye
ambos + `IDENTITY_CONFLICT`; excluye `completedAt>nowMs` (si hay `nowMs`) ⇒ `FUTURE_TIMESTAMP`; input inmutable;
sin `TimeZone.currentSystemDefault()`; O(n log n). Precondición mismo-usuario documentada (run no lleva userId).

Doble verdad (§11): se elimina `CalibrationState.calibrated` almacenado; `status:DimensionStatus` es la única
verdad; helper `isAtLeast`.

---

## §12 · Gates a ejecutar (registro de resultados abajo, al cierre)
compile Desktop · desktopTest (batería nueva) · compileAndroidMain · detekt COMPLETO · copy-scanners (profile
fuera de scope, pero corrido) · iOS `compileKotlinIosSimulatorArm64` → **si no ejecutable en Windows: NOT VERIFIED**.

## §13 · Ciencia (sanity-check, no validación de MindBoost)
Goldberg2015 (practice) · Nicosia2023 (fiabilidad móvil / BYOD) · Gibbons2024 (MIRT/bancos) · Gavett2015 (reliable
change → justifica que `descriptivePerformanceTrend` NO es cambio fiable sin error de medida). Distinguir
ingeniería / heurística de producto / hipótesis / hallazgo / limitación en cada afirmación.

## §14 · Exit gate (contexto fresco): diff-auditor · efficacy-guardian · psychometrician.

---

## §15 · Exit gate — resultados (3 revisores frescos, adversariales)

- **efficacy-guardian: APTO** (sin P0). No pudo construir ningún set de runs que desbloquee rango, fabrique un valor bajo, o haga que la madurez de calibración se haga pasar por rango. Ruling CEO: la política V1 (3 días + 48h + max-2/día) es **honesta como heurística conservadora etiquetada** — ratificable como `rank-policy-v1` — solo porque el rango es fail-closed y nunca reclama validez.
- **psychometrician: APTO contingente** (sin P0). Auditó si cada cantidad mide su constructo (no solo recomputó). Confirmó las tres líneas fail-closed. Señaló que `currentEstimate` mide la tendencia del ejercicio más reciente (no un agregado de bloque equated), que `stabilityHeuristic` es un compuesto dispersión×saturación-muestral (no estabilidad pura), y que el agregador NO es una barrera universal (≥4 sesiones estables pasan).
- **diff-auditor: NO APTO → APTO (re-verificación)**. Su P0 (desempate ausente en `selectScale`) era lectura ESTÁLE: ya estaba corregido. Tras aplicar sus P1 (invariante de tipo, versión de trend) volvió a leer los 11 archivos y cerró sus 6 hallazgos + confirmó los fixes de conteo-de-conflictos y orden-de-exclusiones.

## §16 · Resolución de hallazgos (todo en el contrato puro o barrera fail-closed)

| ID | Hallazgo | Resolución (contrato puro / fail-closed) |
|---|---|---|
| E-F2 | max-2/día computado pero no gateado | `INSUFFICIENT_RANK_SESSIONS` ahora gatea sobre `acceleratingSessionCount` (capado por día) |
| E-F3 | `nowMs=null` desactivaba exclusión de futuros | `nowMs` REQUERIDO (sin default; doctrina 2026-07-08) |
| E-F4 | `?: 0.0` fabricaba 0 | `mapNotNull` + null si vacío (fail-closed `null≠0`) |
| D-F1 (P0) | `selectScale` sin desempate | `maxWithOrNull(compareBy(orderKey, sessionId))` + test de empate |
| D-F2 | tipos sin invariante | `RankReadiness.init{require(containsAll(STRUCTURAL_BLOCKERS))}` (eligible=true INCONSTRUIBLE) + `CalibrationState.init{status==eligibleCeiling(...)}` |
| D-F3 | `PerformanceTrendPolicy.VERSION` no expuesto | `ProfileEstimate.trendPolicyVersion` |
| P-F2 | `stabilityHeuristic` sobre-afirma ortogonalidad | KDoc corregido: compuesto dispersión×muestra, no ortogonal-puro; riesgo de doble-conteo marcado para wiring |
| P-F6 | `IDENTITY_CONFLICT` contaba ids | cuenta RUNS (`conflictRuns`) → audit trail cuadra |
| — | orden de `exclusions` no determinista | ordenado por `ordinal` (permutación-invariante) |
| P-F3 | agregador no es barrera universal | TEST ejecutable prueba que ≥4 estables PASAN → precondición de wiring visible |
| P-F1 | estimate exercise-scoped, no block-equated | fail-closed para RANK (`SCALE_KEY_UNVERIFIED_FOR_RANK`); documentado como límite del estimate + `scaleKey` expuesto |
| P-F4/F5, D-F4, E-F6, D-F6 | honestidad de KDoc | notas: practice-effect, ventana de calibración, n=1, mecanismo-de-bloqueo, confianza per-lane |

## §17 · Precondiciones de WIRING (BLOQUEANTES — no resolubles en un contrato puro; enumeradas, no "follow-ups blandos")

El contrato es correcto y fail-closed HOY (nada cableado; rango estructuralmente imposible; estimate preliminary). Cablearlo a un perfil/rango VIVO exige, con owner:

1. **Equating / anclaje de dificultad** para el estimate de bloque (P-F1): sin él, el estimate es la tendencia del ejercicio más reciente, y farmear el ejercicio fácil infla el provisional. Precondición del perfil provisional vivo.
2. **Vía de agregación provisional separada** (P-F3, R4): el agregador vivo (`minSufficiency=0.5`) descarta 1 sesión pero ACEPTA ≥4 estables — no es barrera universal. El wiring necesita su propio gate de vía provisional.
3. **Elegibilidad persistida autoritativa/no-forjable** (E-F5): el ingestor confía `profileEligible` para timebase+anti-gaming (no persistidos como booleanos). El sellado (SYSTEM-1 slices 1-4B) debe garantizar que esa flag es salida de `SessionEligibilityPolicy`, no un campo forjable por cliente.
4. **RANK (fail-closed hoy):** `CalibrationBlueprint` por bloque + captura de zona IANA por sesión + `normalizationVersion`/equating verificado. Mientras falten, `RankReadiness` permanece `NOT_RANK_ELIGIBLE` por invariante de tipo.
5. **Copy de display** (P-F4): la UI NUNCA debe renderizar `descriptiveTrend == IMPROVING` como mejora cognitiva validada (practice effects, Goldberg 2015).
6. **Migración de usuarios calibrados** (PRD §14.16): re-evaluar contra la distribución V1; analítica `calibration_policy_v1_reeval` definida, no producida.

## §18 · Ciencia (sanity-check, sin reclamar validación de MindBoost)
- Goldberg2015 (27239497): la subida temprana de una tarea repetida es aprendizaje de tarea → `descriptiveTrend` NO es mejora cognitiva.
- Gavett2015 (26234918): cambio fiable exige error de medida → `scoreConfidence=UNKNOWN`, sin RCI.
- Nicosia2023 (36062528/35953659): fiabilidad/latencias BYOD → timebase autoritativo es precondición (fail-closed vía eligibility).
- Gibbons2024 (39748843): MIRT/bancos calibrados → equating es el trabajo pendiente para el estimate de bloque (P-F1).
Distinción por afirmación: ingeniería (EWMA/avg-5 encoding), heurística de producto (V1 5/3d/48h/cap), hipótesis (transfer), hallazgo (ninguno reclamado), limitación no resuelta (equating, error de medida, reliability).

## §19 · Gates ejecutados (resultado real)

| Gate | Comando | Resultado |
|---|---|---|
| Compile Desktop + tests | `:shared:desktopTest --tests "…profile.domain.ingestion.*" --rerun-tasks` | ✅ verde (62 tests) |
| Regresión profile | `:shared:desktopTest --tests "…profile.*"` | ✅ verde |
| Compile Android | `:shared:compileAndroidMain` | ✅ verde |
| Detekt (completo) | `:shared:detekt --rerun-tasks` | ✅ 0 findings |
| Copy-scanners | `:shared:mindboostCopySafetyScanner(+DesktopTest)` | ✅ verde |
| **iOS** | `:shared:compileKotlinIosSimulatorArm64` | ⛔ **NOT VERIFIED** — tarea no registrada en Windows (sin toolchain Kotlin/Native iOS); código 100% commonMain puro; diferido a CI macOS |

## §20 · VEREDICTO: **CONDITIONAL**

El contrato puro y NO cableado está **completo, endurecido y 3/3 APTO** con todos los bloqueantes de exit-gate cerrados y todos los gates ejecutables en verde. NO se declara DONE porque quedan precondiciones de wiring P1 (§17) — no defectos del contrato, sino requisitos de datos/UI/upstream para cablearlo con seguridad. El rango es fail-closed por invariante de tipo, así que no puede desbloquearse por accidente. iOS: NOT VERIFIED. **No cablear a perfil/rango vivo, no pasar a slice 7, sin decisión CEO.**
