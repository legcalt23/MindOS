# AUDITORÍA TOTAL, FALSACIONISTA Y READ-ONLY DEL MOTOR DE ENFOQUE DE MOLVEX

> **Modo:** READ-ONLY. Ningún fichero productivo modificado, ningún commit, ningún flag activado, ningún dato real alterado. Este documento y las filas del `MINDBOOST_AUDIT_LEDGER.md` que lo acompañan son las únicas escrituras de la tarea, y son actos de gobernanza, no de producto.
>
> **Nombre:** el producto es **Molvex** (`CEO-OVERRIDE-001`). Todo el repositorio, la copy y el ledger dicen «MindBoost». La discrepancia se registra como necesidad de migración documental (§ PARTE XXXI, decisión D-0), NO como contradicción de fondo.
>
> **Etiquetas de evidencia** (obligatorias en cada afirmación): `CEO_APPROVED` · `DOCUMENTED_POLICY` · `REPO_VERIFIED` · `TEST_VERIFIED` · `SCIENTIFICALLY_SUPPORTED` · `INFERRED` · `CLAIMED_NOT_VERIFIED` · `HYPOTHESIS` · `UNKNOWN` · `CONTRADICTED` · `RECOMMENDATION` · `REIMPLEMENTATION_REPRODUCED` (resultado del espejo Node del scoring Kotlin, validado contra 17 goldens Kotlin; nunca equivale a `TEST_VERIFIED`).
>
> **Cadena de código** (obligatoria al hablar de código): existe → está conectado → se ejecuta en producción → está testeado → produce outputs correctos → tiene interpretación científicamente válida. Un eslabón no implica el siguiente.

---

# PARTE 0 — Audit Snapshot

| Campo | Valor | Etiqueta |
|---|---|---|
| Producto | Molvex (legacy en docs/código: MindBoost) | `CEO_APPROVED` |
| Repo / rama | `C:\Users\curro\AndroidStudioProjects\Molvex` · `product/focus2-v5` | `REPO_VERIFIED` |
| HEAD | `bdc252f496384e52e3ac14e451a139df57b47792` (`feat(focus): FOC-P0-01 AppLifecycleProbe…`) | `REPO_VERIFIED` |
| Working tree | SUCIO: 360 entradas en `git status --porcelain`; huella SHA-256 del status `3b44e884…8a34e96c` **idéntica antes y después de todos los gates** (ningún fichero del repo tocado por esta auditoría) | `REPO_VERIFIED` |
| Fecha de la corrida de gates | 2026-09-14 (ventana de tests UTC 16:25:25–16:26:37); fecha de cierre del informe 2026-09-15 | `REPO_VERIFIED` |
| Plataformas | Android (compilado), Desktop JVM (compilado + tests), iOS **NOT VERIFIED** (sin host macOS; solo compila en CI) | `REPO_VERIFIED` / `UNKNOWN` (iOS) |
| Grafo Graphify | construido en `8a06b9ac` (un commit por detrás de HEAD); usado sólo para orientación, nunca como evidencia | `REPO_VERIFIED` |
| Ledger al inicio | 308 filas de registro; validador `node scripts/validate-audit-ledger.mjs` → `LEDGER OK — 325 findings`, hash `23af9d17b7b6…` | `REPO_VERIFIED` |
| Gobernanza | `npm run check:governance` **FALLA POR DISEÑO** (`CEO_FIELD_DISAGREEMENT EXECUTION_AUTHORIZATION`: `ADR_REGISTER_V5=NOT_GRANTED` vs `CURRENT_AUTHORITY_INDEX=GRANTED`) = `MB-FOCUS-0155`; no es un fallo de esta auditoría | `REPO_VERIFIED` |

## 0.1 Alcance auditado

- **Motor montado (`focus v1`)**: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/**` — catálogo `ExerciseCatalog` (E1–E13), generadores, `FocusSessionStateHolder`, `FocusSessionAssembler`, `SessionQualityGate`, `FocusAntiGaming`, `DefaultMetricsEngine`, `DefaultAdaptiveEngine`, `DefaultCalibrationEngine`, `BaselineAssembler`, `ProbeBank`, `ProbeReliabilityEstimator`, `ReliableChangeIndex`, `FocusEvaluationService`, `PersonalizedSessionScorer`, `ArchetypeInference`, `DailyPlanGenerator`, `TrainingCycle`, `RecoveryPolicy`, `OvertrainingGuard`, `MicroPauseGate`, persistencia (`FocusDtos`, `FocusPersistenceMappers`, `LocalFocusRepositories`, `SecureKeyValueStore`), DI (`FocusGraph`, `RememberFocusStateHolders`), UI (`FocusHomeScreen`, `FocusBaselineScreen`, `FocusSessionScreen`, `FocusSummaryScreen`, `FocusProgressScreen`, `FocusEvaluationScreen`, 8 campos de estímulo), `FeatureFlags`, `AppLifecycleProbe`, `MonotonicClock`.
- **Motor NO montado (`focus2`)**: `features/mindboost/focus2/**` (trazadores V5, `CONTRACT_INERT_NO_WIRING`) — auditado sólo donde difiere.
- **Transversal**: `core/timing/*` (FrameTimingProbe, WindowedTemporalProbe, TemporalClassPolicy), `core/scoring/MinSampleGate`, `core/antigaming/*`, `core/session/*` (SessionRecord, FormDescriptorV1), `profile/**` (FocusProfileSource), `speed/**` (comparación de timebase), `functions/src/accountDeletion.ts`, `FirestoreFocusSessionRemoteDataSource`.
- **Documentos**: `docs/context_pack/00…09`, `AGENTS.md`, `CLAUDE.md`, ADRs (`docs/engines/focus_v5/*`, `docs/audits/ADR_*`), `docs/mindboost_exercise_design/03_1_Enfoque.md`, `08_ALPHA_BETA_VALIDATION_PLAN.md`, `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§1–§28), `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md`, `docs/marketing/assets/aso-google-play-es.md`, `MINDBOOST_AUDIT_LEDGER.md` completo.
- **Fuera de alcance**: cualquier módulo no-Enfoque salvo como comparador (Velocidad, focus2); backend social; el catálogo definitivo (prohibido diseñarlo).

## 0.2 Inputs y método

1. Lectura directa de fuente Kotlin con cita `fichero:línea` para cada `REPO_VERIFIED`; Graphify sólo para orientar.
2. **Reimplementación Node del camino de scoring** (`scratchpad/reimpl/focus_reimpl.mjs`): `DefaultMetricsEngine`, `PersonalizedSessionScorer`, `FocusAntiGaming`/`SessionQualityGate`, `DefaultAdaptiveEngine`, `DefaultCalibrationEngine`, `Trends`, `ReliableChangeIndex`, `ArchetypeInference`; validada contra **17 goldens Kotlin extraídos de tests reales** (`goldens.mjs`, 17/17). Resultados etiquetados `REIMPLEMENTATION_REPRODUCED`.
3. **Simulación adversaria** S1–S15 (+S12b, variantes E3) sobre E1 con 8 sesiones × 2 bloques × 47 ensayos; perfiles SAT P1–P8 sobre 94 ensayos; ruido de estimación 200 semillas × 4 tamaños.
4. **12 lentes especialistas de solo lectura** (`workflow`): efficacy-guardian, psychometrician, assessment-psychometrics, cognitive-transfer, gameplay-lab, y emuladas por `architect` sin Bash: cognitive-science, personalization-engine, data-analytics, mobile-performance, experimentation, assessment-diagnostics, privacy-security. Cada lente recibió UNA pregunta falsable y devolvió veredicto `CONFIRMED / REFUTED / PARTIALLY_REFUTED / UNDETERMINED`. Estado de cada lente en PARTE II §2.3.
5. Refutación adversaria de los hallazgos portantes (3 votos `architect` sin Bash; un hallazgo con cero refutaciones se etiqueta **NO JUZGADO**, nunca refutado).
6. Gates ejecutados (todos `--rerun`, con evidencia de XML frescos, nunca `UP-TO-DATE`/`FROM-CACHE` en la tarea que es el gate).

## 0.3 Gates ejecutados

| Gate | Resultado | Evidencia |
|---|---|---|
| `:shared:desktopTest --rerun` | **4468 tests / 0 F / 0 E / 0 S** sobre **624 XML frescos** (0 rancios), ventana 16:25:25–16:26:37Z | `scratchpad/evidence/dt_run.log`, agregado XML |
| `:shared:detekt --rerun` | 0 errores, `detekt.xml` fresco | `scratchpad/evidence/detekt.log` |
| `:shared:compileKotlinDesktop --rerun` | verde (1m48s, ejecutada) | `scratchpad/evidence/compile_desktop.log` |
| `:shared:compileAndroidMain --rerun` | verde (1m39s, ejecutada) | `scratchpad/evidence/compile_android.log` |
| `:androidApp:compileDebugKotlin --rerun` | verde (82 actionable, 19 ejecutadas) | `scratchpad/evidence/compile_app.log` |
| 15 scanners (`adrAuthorityBoundary`, `arenaTerminology`×3, `emotionsTerminology`×3, `evidenceBoundary`, `evidenceConsumerSweep`, `evidenceTopLevelDeclarationAllowlist`, `focusQualityGate`, `mindboostCopySafety`×3, `sessionPurposeDerivation`) `--rerun` | 15/15 ejecutadas, verdes | `scratchpad/evidence/scanners.log` |
| `node scripts/validate-audit-ledger.mjs` | `LEDGER OK — 325 findings` | `scratchpad/evidence/ledger_validate.log` |
| `npm run check:governance` | FALLA POR DISEÑO (`MB-FOCUS-0155`) | `scratchpad/evidence/governance.log` |
| iOS | **NOT VERIFIED** | — |

## 0.4 Limitaciones declaradas

- **Sin datos reales de usuarios.** No existe ningún dataset de sesiones reales; todo resultado cuantitativo es simulación (`REIMPLEMENTATION_REPRODUCED`) o lectura estática. Ninguna afirmación de fiabilidad, validez o transferencia de este informe está respaldada por datos humanos.
- **Espejo Node ≠ Kotlin.** El espejo reproduce 17 goldens, pero no está garantizado bit-a-bit en ramas no cubiertas por goldens (p. ej. `ArchetypeInference` con `PRESSURE_DELTA`, `MicroPauseGate`). Donde el espejo es la única fuente, se dice.
- **iOS no verificado.** Ni compilación ni ejecución.
- **Lentes emuladas.** Seis lentes las ejecutó el agente `architect` emulando el rol (sin Bash por mecanismo `MB-FOCUS-0083`); documentado en cada una. Ninguna lente ejecutó Gradle.
- **Una corrida del workflow de lentes murió** (límite de uso) y se relanzó sólo para las seis lentes ausentes; el estado final de cada lente está en PARTE II §2.3. Toda lente sin veredicto se declara **NO JUZGADA**.
- **`CEO-OVERRIDE-002`** (función objetivo individual) vive sólo en el mandato; en el repo la única política documentada contra la sobre-reacción a datos escasos es la del charter §11.1 (≥5 sesiones válidas / ≥3 días / ≥48 h antes de `provisional→calibrated`, `03_SCIENTIFIC_MEASUREMENT_CHARTER.md:438-449`). La sobre-reacción se mide contra esa regla.

---

# PARTE I — Executive Verdict

## 1.1 Qué es hoy el motor de Enfoque de Molvex

`REPO_VERIFIED`. Un **andamiaje de medición honesta, sin motor de entrenamiento alcanzable**. Existen dos motores: `focus` (v1, montado en el hub por `MindBoostHubScreen.kt:117` → `FocusRootScreen`) y `focus2` (trazadores V5, `CONTRACT_INERT_NO_WIRING`, no montado). El montado tiene 13 ejercicios declarados (`ExerciseCatalog.implemented`, 13/13 con generador y renderizador), un ensamblador de sesión con puerta de calidad (`SessionQualityGate`) y detector de estrategia ciega, un motor de métricas con ausencia tipada, un motor adaptativo, un motor de calibración, un banco de sondas con 4 formas reservadas por sonda, un estimador de fiabilidad y un RCI. Todo ello compila, pasa 4468 tests y detekt en verde.

Pero con el flag embarcado `focusLifecycleProbeEnabled=false` (`FeatureFlags.kt:132`; `FocusScoreSurfacesEnabled` NO forma parte de la cadena: sólo oculta chip/progreso) **ningún usuario nuevo puede llegar a entrenar en ninguna plataforma**: la Home exige línea base (`FocusHomeStateHolder.kt:88 needsBaseline = profile.baseline == null`), la línea base sólo se persiste si la sonda es `Admitted` (`FocusBaselineStateHolder.kt:225-227`; la rama `NotMeasurable` :215-224 guarda `baseline = null` en :221), y sin la sonda de ciclo de vida la puerta devuelve `LIFECYCLE_NOT_OBSERVED` (`RememberFocusStateHolders.kt:86-90` → `AppLifecycleProbe.kt:72-76` → `SessionQualityGate.kt:55-64`). El bucle real de hoy es: Home → «Empezar mi base» → `NotMeasurable` → Home, indefinidamente. En desktop e iOS el `actual` es `NoOpAppLifecycleProbe` (`AppLifecycleProbe.desktop.kt:10`, `.ios.kt:12` → `NO_PLATFORM_SOURCE`), así que **ni con el flag ON** se desbloquean esos targets. La afirmación «Remote Config sólo enciende la sonda para `android_debug_app`» descansa en `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md:167-170`, no en fuente (no hay plantilla RC en el repo): `CLAIMED_NOT_VERIFIED`. La única ruta hacia la alcanzabilidad es operativa, no de código: un flip de consola de Remote Config sin condición encendería la sonda para TODOS los usuarios Android (`MB-FOCUS-0116`; `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md:40-45`) y ningún código impone la precondición «verificado en dispositivo». Refutación adversarial: sobrevive 3/3 (90–92 %; ver Anexo).

## 1.2 Qué está implementado (cadena existe → conectado → se ejecuta → testeado → outputs correctos → interpretación válida)

| Componente | Existe | Conectado | Se ejecuta en prod (flags embarcados) | Testeado | Outputs correctos | Interpretación válida | Etiqueta |
|---|---|---|---|---|---|---|---|
| 13 generadores E1–E13 + 8 renderizadores | ✅ | ✅ | ❌ (ruta inalcanzable) | ✅ | ✅ (goldens) | parcial (E9/E10/E4 anclaje RT, §XII) | `REPO_VERIFIED` |
| `SessionQualityGate` + `BlindStrategyCeilingDetector` | ✅ | ✅ (único call-site `FocusSessionAssembler.admit`) | ✅ (es lo que deniega la base) | ✅ red-team | ✅ | parcial: respondedor honesto lento = RESPONSE_PATTERN (`MB-FOCUS-0146`) | `REPO_VERIFIED` |
| `DefaultMetricsEngine` (26 `MetricKey`) | ✅ | ✅ | ❌ (sólo en la sonda denegada; métricas retiradas a `withdrawn()` antes de persistir) | ✅ | ✅ (17 goldens reproducidos) | parcial: `unavailable` escribe `value=0.0` | `REPO_VERIFIED` / `REIMPLEMENTATION_REPRODUCED` |
| `DefaultAdaptiveEngine` | ✅ | ✅ | ❌ | ✅ | ✅ | débil: con lifecycle ON honestos → `Hold(METRIC_UNAVAILABLE)`; `impulsivityDecision` precede a las guardas (`MB-FOCUS-0164`) | `REPO_VERIFIED` |
| `DefaultCalibrationEngine` + `BaselineAssembler` | ✅ | ✅ | ❌ (base nunca persiste) | ✅ | ✅ | débil: una sola administración fija umbrales personales; `deviceFingerprint` siempre `null` | `REPO_VERIFIED` |
| `ProbeBank` (3 sondas × 4 formas) + `ProbeReliabilityEstimator` + `ReliableChangeIndex` | ✅ | ✅ | ❌ | ✅ | ✅ | **no evaluable**: `ReliabilitySource.EMPTY` en producción (`FocusGraph.kt:156` → `FocusEvaluationService.kt:17`) ⇒ todo RCI = `NotEnoughData` | `REPO_VERIFIED` |
| `PersonalizedSessionScorer` (Performance Score) | ✅ | ✅ | ❌ (tras `scoreSurfacesEnabled()`, flag OFF) | ✅ | ✅ | débil: sesgo SAT y planitud (§XV) | `REPO_VERIFIED` / `REIMPLEMENTATION_REPRODUCED` |
| `ArchetypeInference`, `MicroPauseGate`, `PersonalizationProfiles` | ✅ | ✅ | ❌ (flag OFF) | ✅ | ✅ | débil: arquetipo no puede promocionarse sin fiabilidad | `REPO_VERIFIED` |
| `DailyPlanGenerator`, `RecoveryPolicy`, `OvertrainingGuard`, `TrainingCycle` | ✅ | ✅ | ✅ (en cada carga de Home, `FocusHomeStateHolder.kt:67-79`) | ✅ | ✅ | inerte: recomendación siempre E1 al no haber historial admitido | `REPO_VERIFIED` |
| Persistencia cifrada (`SecureKeyValueStore`, DTOs con `schemaVersion`) | ✅ | ✅ | ✅ | ✅ | parcial: trials descartados al escribir, `plannedTrials=1`/`completedTrials=1` literales en el `SessionRecord` de core (el DTO de Focus sí lleva `trialCount` real), sin versión de scorer/umbrales por fila | sólo resúmenes, comparabilidad entre filas no garantizada | `REPO_VERIFIED` |
| Telemetría / analítica de Enfoque | ❌ (0 eventos; único breadcrumb `FocusScoreContainmentBreadcrumb.kt:26-28`) | — | — | — | — | — | `REPO_VERIFIED` |
| Sincronización remota (`FirestoreFocusSessionRemoteDataSource`) | ✅ | ❌ (sin call-site de producción; `mindboostSyncFirestoreEnabled=false`) | ❌ | ✅ | — | — | `REPO_VERIFIED` |
| Perfil cerebral (`FocusProfileSource`) | ✅ | ✅ | ✅ (fail-closed → `CALIBRATING`) | ✅ | ✅ | correcta por ausencia | `REPO_VERIFIED` |

## 1.3 Qué es sólo objetivo (TARGET), no implementación

`DOCUMENTED_POLICY` / `CLAIMED_NOT_VERIFIED`: el estudio de fiabilidad test-retest (charter §11; `08_ALPHA_BETA_VALIDATION_PLAN.md`), la infraestructura A/B (08 §2.6/§8/§9.3/§10.3/§12.2/§17.1/§22/§25), la calibración poblacional, el `NEAR_TRANSFER_INDEX` (sin productor, `MB-FOCUS-0096`), los `FunctionalProtocols` (dormidos), la sincronización a nube, el sensor de ciclo de vida en producción (`MB-FOCUS-0101` OPEN), el Growth Index como cambio fiable (requiere fiabilidad ≥ 0.70/0.80 que nadie ha medido), la personalización por arquetipo (L4–L7), la dosis por fatiga (RecoveryPolicy/OvertrainingGuard sin historial), la transferencia (ni cercana ni lejana: no hay diseño que la pueda demostrar). Toda la V5 (`focus2`) es objetivo.

## 1.4 Qué mide hoy de verdad

Con los flags embarcados: **nada oficial**. La única medición que se ejecuta es el bloque de sonda de línea base (55 ensayos de E1/PVT), y su resultado se deniega y persiste con métricas retiradas (`admission = NotMeasurable(LIFECYCLE_NOT_OBSERVED)`; todas las métricas `withdrawn()` → `value=0.0`, `confident=false`). Con la sonda de ciclo de vida encendida (sólo `android_debug_app` hoy) mide: exactitud, RT mediano, CV del RT, lapsos, falsas alarmas e índice de fatiga por bloque y sesión (E1); efecto de congruencia (E2/E5); SSRT (E4, inflado por SSD codificada como cota inferior); coste de cambio (E6); coste dual (E8); slope de tamaño de set (E7). Ninguna con fiabilidad estimada, ninguna con error estándar mostrado, ninguna comparada con una población.

## 1.5 Qué no puede demostrar

- Que un usuario haya **mejorado**: ningún RCI puede ser `Reliable` (fuente de fiabilidad vacía), y con una fiabilidad hipotética de 0.80 el RCI marcaría como cambio fiable el ruido de S7/S9/S11/S12b (`REIMPLEMENTATION_REPRODUCED`).
- Que la mejora en E1 **transfiera** a otro ejercicio o a la vida: no hay diseño de transferencia, `NEAR_TRANSFER_INDEX` no tiene productor, los 13 `plainDescription` dicen «La sesión registra…» (correcto), pero la copy de marketing (`docs/marketing/assets/aso-google-play-es.md:15,17,19,29`) dice «MindBoost mide» / «para tu concentración».
- Que dos sesiones sean **comparables**: sin `deviceFingerprint` (siempre `null`), sin versión de scorer/umbrales/adaptativo por fila, con RT clase D (onset estampado antes de `presentAndAwait`, respuesta en pointer-UP) y con E9/E10 anclados al inicio del ensayo.
- Que la puerta de calidad **distinga** a un honesto lento de una estrategia ciega (`MB-FOCUS-0146`).

## 1.6 Mayor fortaleza

`REPO_VERIFIED`. La **disciplina de honestidad de medición**: ausencia tipada en vez de ceros fabricados (`MetricAbsenceReason`, `SessionAdmission.NotMeasurable`), puerta de calidad con precedencia declarada, detector de estrategia ciega con red-team (adversario dispara / honesto no dispara), formas de sonda reservadas y disjuntas del entrenamiento, `formIsFresh` como tercer gate del RCI, `MinSampleGate` como autoridad central de suficiencia, `measurementSufficient` ortogonal a `confident`, perfil cerebral fail-closed. Es una base poco frecuente en el sector y es lo que hace que el resto del informe pueda ser preciso.

## 1.7 Mayor debilidad

`REPO_VERIFIED`. **El sistema no puede ejercitarse**: el bucle nuclear `baseline → plan → sesión → feedback → progresión` está roto en el primer eslabón por una combinación de flag OFF + gate correcto + persistencia condicionada. Detrás de esa puerta, el segundo eslabón débil es la **identidad temporal y de dispositivo del RT** (clase D, onset pre-render, tap-UP, sin timebase por fila, sin fingerprint), que hace que cualquier futura afirmación de cambio herede un error de medida no acotado.

## 1.8 Principal riesgo de falso progreso

`REIMPLEMENTATION_REPRODUCED` + `REPO_VERIFIED`. El día que alguien pueble `ReliabilitySource` (una sola línea en `FocusGraph.kt:156`) **sin** un estudio de fiabilidad real, el RCI empezará a declarar «cambio fiable» sobre ruido: con rel=0.80 y sdPre≈60 ms un cambio de 40 ms exige rel ≥ 0.942 para ser detectable, y la simulación muestra RCI «fiables» en perfiles cuyo cambio verdadero es cero (S7) o negativo (S9). Riesgo agravado por: un único retest fresco elegible por sonda (4 formas − 3 administraciones mínimas), sin término de práctica en el RCI (`MB-FOCUS-0133`), y sin versionado de comparabilidad por fila.

## 1.9 Principal oportunidad

`RECOMMENDATION`. El andamiaje ya distingue evidencia de entrenamiento y evidencia de medición. Si se (a) desbloquea el entrenamiento (sonda de ciclo de vida en producción o rediseño de la precondición de línea base), (b) fija la identidad del RT (onset post-render, pointer-DOWN, timebase y fingerprint por fila, clases A–E declaradas), (c) cierra la cadena de versionado por fila y (d) ejecuta el estudio de fiabilidad de 8 semanas antes de poblar `ReliabilitySource`, Molvex tendría el único motor de atención del mercado con **medición oficial separada del entrenamiento** y RCI con incertidumbre declarada. El catálogo definitivo debe diseñarse sobre esa base, no antes.

## 1.10 Maturity: **3 / 10**

Justificación (detalle por dimensión en §58, media aritmética 2.5/10 sobre 24 dimensiones): las dimensiones más altas son accesibilidad (5) y cobertura de dominio (5); las más bajas son personalización individual (1), robustez de dispositivo (1), fiabilidad (1), validez (1) y transferencia (1). Construct definition 3, training quality 3, anti-false-progress 3 (10 invariantes sobreviven, 15 violados), measurement quality 2, uncertainty 2, adaptation 2. Se redondea la media 2.5 a **3** por dos hechos que la media no captura: (a) 13 generadores deterministas TEST_VERIFIED con 4468 tests en verde, y (b) la disciplina de ausencia tipada, gates de admisión y red-team ya existe y está probada por mutación —lo que sobrevive a la falsación (R9–R15) sobrevive por diseño, no por suerte—. No es un 4 porque con los flags embarcados **ningún usuario puede entrenar** (P0), no hay un solo dato de fiabilidad y el RT es de clase D en 13/13. No es un 2 porque nada de lo roto exige rehacer la arquitectura: los P0 son cableado, no diseño. Un 3 significa: **la arquitectura es correcta en dirección y la implementación es fiel a esa arquitectura, pero nada de lo que promete el bucle nuclear ha sido ejecutado ni medido con personas**.

---

# PARTE II — Source-of-Truth & Evidence Ledger

## 2.1 Jerarquía de fuentes aplicada

1. Ley / seguridad / privacidad · 2. Decisiones CEO del mandato (`CEO-OVERRIDE-001..004`) · 3. Fuente de verdad CEO vigente (`docs/context_pack/01_CEO_SOURCE_OF_TRUTH.md`; `CURRENT_AUTHORITY_INDEX.md` para V5) · 4. `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` · 5. PRD · 6. `09_DECISION_LOG.md` / ADRs · 7. plan/slice activo · 8. implementación real como **evidencia de lo que existe, nunca como requisito**.

Contradicciones internas detectadas entre fuentes (se resuelven por precedencia, no se ocultan):

| # | Fuente A | Fuente B | Contradicción | Resolución | Etiqueta |
|---|---|---|---|---|---|
| C1 | Mandato CEO: producto = **Molvex** | Repo, ledger, copy, `applicationId`, scanners: **MindBoost** | Nombre | Precedencia 2 > 8. Registrar migración documental (D-0, PARTE XXXI). No es contradicción de fondo. | `CEO_APPROVED` |
| C2 | `ADR_REGISTER_V5.md` `EXECUTION_AUTHORIZATION=NOT_GRANTED` | `CURRENT_AUTHORITY_INDEX.md` `=GRANTED` | Autorización de ejecución V5 | `MB-FOCUS-0155` (falla por diseño; sólo el CEO puede editar). Este informe trata V5 como NO autorizada hasta que el CEO reconcilie. | `REPO_VERIFIED` |
| C3 | Charter §11.1: `provisional→calibrated` exige ≥5 sesiones válidas / ≥3 días / ≥48 h | `DefaultCalibrationEngine`: umbrales personales y dificultad inicial de **una sola** administración de 55 ensayos | Dosis de calibración | El charter gobierna; la implementación queda como `provisional` estructuralmente. Hallazgo nuevo (PARTE XVII). | `DOCUMENTED_POLICY` vs `REPO_VERIFIED` |
| C4 | `ExerciseDefinition.governingMetric` KDoc (ya corregido R14): «lo lee NADIE» | 13 declaraciones en `ExerciseCatalog` | Campo declarado sin lector | KDoc corregido es la verdad; el campo es documental. | `REPO_VERIFIED` |
| C5 | `docs/marketing/assets/aso-google-play-es.md:15,17,19,29` «MindBoost **mide**…» «para tu concentración» | `ExerciseCatalog.plainDescription` ×13 «La sesión **registra**…» (nivel C0) | Nivel de claim | El catálogo tiene razón; marketing es una violación C0 fuera del alcance escaneado. Hallazgo nuevo. | `REPO_VERIFIED` |
| C6 | `FocusProgressStateHolder.kt:21-28,54` KDoc «near-transfer» | `MB-FOCUS-0089` (claims retirados), `NEAR_TRANSFER_INDEX` sin productor | Claim en comentario | El gate lexical `FocusClaimLevelGateTest` es ciego a KDoc. Actualizar `0089`. | `REPO_VERIFIED` |
| C7 | `08_ALPHA_BETA_VALIDATION_PLAN.md` asume infraestructura A/B (§2.6, §8, §9.3, §10.3, §12.2, §17.1, §22, §25) | Repo: cero infraestructura de asignación/experimento | Plan vs código | Plan es TARGET. Gate nuevo «08 §22 A/B infra». | `DOCUMENTED_POLICY` vs `REPO_VERIFIED` |
| C8 | `AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§1–§28) frases de estado global («los dos P0 siguen abiertos», ya corregida en §28) | Ledger de hoy | Frases envejecidas por fase | Se lee el ledger de hoy; el informe de agosto es histórico. | `REPO_VERIFIED` |
| C9 | Ledger `MB-FOCUS-0099` (autoridad de timing) describe captura RT como «monotónica»; `MB-FOCUS-0109` describe «captura duplicada» | Código: onset estampado **antes** de `presentAndAwait` y respuesta en `detectTapGestures{onTap}` (UP) | Descripción incompleta | Ambas filas se actualizan (clase D declarada). | `REPO_VERIFIED` |
| C10 | `FocusSessionCloudDto.scoringVersion = "focus-scoring-1"` | `SessionSummaryDto` sólo `schemaVersion`; ningún `scoringVersion` local; sync sin call-site | Versionado de scoring inerte | Charter §21 exige linaje; hallazgo nuevo (PARTE XXIV). | `REPO_VERIFIED` |

## 2.2 Ledger de claims estructurales

| # | Claim estructural | Estado | Evidencia | Confianza |
|---|---|---|---|---|
| S1 | El motor de Enfoque montado es `focus` v1; `focus2` no está montado | `REPO_VERIFIED` | `MindBoostHubScreen.kt:117`; `focus2` sin call-site desde el hub | 100% |
| S2 | 13 ejercicios con generador + renderizador + `implemented` = 13 | `TEST_VERIFIED` | `ExerciseCatalog.kt:274-299`; `ExerciseCatalogConsistencyTest` | 100% |
| S3 | Con flags embarcados el entrenamiento es inalcanzable en todas las plataformas | `REPO_VERIFIED` | `FeatureFlags.kt:131-132`; `FocusHomeScreen.kt:158-175`; `FocusHomeStateHolder.kt:88`; `FocusBaselineStateHolder.kt:215-224` (NotMeasurable) / `:225-227` (Admitted); `RememberFocusStateHolders.kt:86-90`; `AppLifecycleProbe.kt:72-76`; `AppLifecycleProbe.desktop.kt:10` / `.ios.kt:12` (NoOp); `SessionQualityGate.kt:55-64`. Bloqueador = `focusLifecycleProbeEnabled` solo | 100% |
| S4 | Remote Config enciende `focus_lifecycle_probe_enabled` sólo para `android_debug_app` | `REPO_VERIFIED` (plantilla v1 leída en corrida 2026-09-14) | `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md` §1.4 | 90% (la consola puede cambiar sin diff) |
| S5 | Precedencia de la puerta: INTERRUPTED > NO_RESPONSES > RESPONSE_PATTERN > LIFECYCLE_NOT_OBSERVED > Admitted | `REPO_VERIFIED` | `SessionQualityGate.kt:55-64` | 100% |
| S6 | Métricas de sesión no admitida se retiran (`withdrawn()`) **antes** de persistir | `REPO_VERIFIED` | `FocusSessionAssembler.kt:247-250`; `FocusSessionStateHolder.kt:419-424` | 100% |
| S7 | `MetricValue.unavailable` escribe `value=0.0`; la ausencia se pierde al persistir | `REPO_VERIFIED` | `MetricKey.kt:71-74, 82-88` | 100% |
| S8 | RT = `MonotonicClock` (`TimeSource.Monotonic`); onset antes de `presentAndAwait`; respuesta en pointer-UP; sin timebase por fila | `REPO_VERIFIED` | `MonotonicClock.kt:19-22`; `FocusSessionStateHolder.kt:266`; `detectTapGestures{onTap()}` en campos | 100% |
| S9 | E10 avanza por `delay(80)` en `LaunchedEffect`; E9 sondea a `progress>0.82` de un tween de 1800 ms dentro de ventana 2000 ms; RTs anclados al inicio del ensayo | `REPO_VERIFIED` | `FocusStreamField.kt`; `FocusTrackingField.kt`; `E9EnjambreGenerator` | 100% |
| S10 | E4: la SSD PEDIDA se registra como usada (`goPhaseMs = ssd.coerceIn(0, window)`, `awaitResponse(goPhaseMs)` = `withTimeoutOrNull`, sin sincronía de frame); la SSD visual efectiva = pedida + overshoot del timer + latencia de render STOP − latencia de render GO ⇒ sesgada al alza en expectativa (no cota estricta) ⇒ SSRT sesgado; único gate: banda post-hoc p(respond·señal) 0.25–0.75 sobre `confident` (`SsrtCalculator.kt:54` → `DefaultMetricsEngine.kt:489`), que caza la saturación de la escalera pero no el slowing intra-bloque; `WaitingStrategyDetector` (core) NO está cableado a Enfoque (`FocusAntiGaming.kt:62`). Discrepancia en sentido opuesto no cubierta en la primera pasada: `:592` acota `goPhaseMs` a la ventana pero `:594/:597` registran el `ssd` crudo; con `MAX_SSD_MS = 1000` (`StopSignalTracker.kt:33`) > ventana mínima 800 (`E4FrenoGenerator.kt:29`), una racha de stops exitosos hace que el SSD registrado supere la fase GO realizada y la señal STOP no llegue a dibujarse (`remainingMs = 0`) mientras el ensayo cuenta como stop exitoso. Radio hoy: E4 sólo corre en TRAINING (`:239/:398`) y ninguna sonda usa STOP_SIGNAL ⇒ el SSRT gameado llega al informe/historial/motor, no al RCI, y `measurementSufficient=false` a ≤60 ensayos (`MB-CATALOG-0016`): defecto de integridad latente. Refutación K5: sobrevive 3/3 (80–88 %) | `REPO_VERIFIED` | `FocusSessionStateHolder.kt:584-598`, `:537-543` | 95% |
| S11 | `plannedTrials = 1` y `completedTrials = 1` literales en el `SessionRecord` write-ahead de `core/session` (`recorder.onStart`/`onFinalize`), NO en `SessionSummaryDto` (que persiste `trialCount` real por bloque, `FocusPersistenceMappers.kt:130`); ningún consumidor de Focus lee esos campos (K6 3/3) | `REPO_VERIFIED` | `FocusSessionStateHolder.kt:340, 476` | 100% |
| S12 | `deviceFingerprint` nunca poblado (único escritor pasa `null`) | `REPO_VERIFIED` | `FocusBaselineStateHolder.kt:209` → `BaselineAssembler.kt:38/53` → `DefaultCalibrationEngine.kt:68/74` | 100% |
| S13 | Sólo E2 persiste `FormDescriptorV1` (identidad reproducible); id de recorder opaco ≠ id de resumen | `REPO_VERIFIED` | `FocusFormContract.kt:49-62`; `RememberFocusStateHolders.kt:109-119`; `FocusGraph.kt:206-212`; `SessionRecord.kt:53`; `FocusSessionStateHolder.kt:235, 383, 394` | 100% |
| S14 | `ReliabilitySource.EMPTY` en producción ⇒ RCI nunca `Reliable` | `REPO_VERIFIED` | `FocusGraph.kt:156` → `FocusEvaluationService.kt:17` → `ProbeReliabilityEstimator` | 100% |
| S15 | 4 formas reservadas + `MIN_ADMINISTRATIONS=3` + `formIsFresh` ⇒ un único retest fresco elegible por sonda | `TEST_VERIFIED` | `ProbeBank.kt:44,53,62`; `ProbeReliabilityEstimator.kt:50`; `ProbeBankTest` | 100% |
| S16 | Sonda de 55 ensayos deja ~11 celdas RT frente a piso 20 (110 necesarios) | `TEST_VERIFIED` | `ProbeMeasurementReachabilityTest` | 100% |
| S17 | `baselineRtCv` nunca llega al motor adaptativo (`cfg.fallbackBaselineRtCv`) | `REPO_VERIFIED` | `AdaptiveEngine.kt:96-98` | 100% |
| S18 | `impulsivityDecision` precede a las guardas de admisión/ausencia | `REPO_VERIFIED` | `AdaptiveEngine.kt`; `MB-FOCUS-0164` | 100% |
| S19 | Scorer/arquetipo/micro-pausa detrás de `scoreSurfacesEnabled()` (OFF) | `REPO_VERIFIED` | `FeatureFlags.kt:116,131`; `FocusSummary*`/`FocusProgress*` holders | 100% |
| S20 | Plan diario / recuperación / sobreentrenamiento se ejecutan en cada carga de Home | `REPO_VERIFIED` | `FocusHomeStateHolder.kt:67-79` | 100% |
| S21 | Cero eventos de analítica de Enfoque; único breadcrumb de contención | `REPO_VERIFIED` | `FocusScoreContainmentBreadcrumb.kt:26-28`; `MindBoostAnalyticsHub` consumidores sólo emotions/coach | 100% |
| S22 | Sync remoto sin call-site de producción; `scoringVersion` inerte; excluye `sessionMetrics` | `REPO_VERIFIED` | `FirestoreFocusSessionRemoteDataSource`; `FocusSessionCloudDto.kt:27-29` | 100% |
| S23 | Umbrales personales viven sólo en el DTO mutable del perfil (se sobrescriben) | `REPO_VERIFIED` | `FocusDtos.kt:118-129`; `LocalFocusRepositories.kt:46-48` | 100% |
| S24 | `readVersioned` cuarentena sólo versiones FUTURAS; `FocusSchema.CURRENT` compartido por 3 DTOs | `REPO_VERIFIED` | `SecureKeyValueStore.readVersioned:138-156`; `FocusSchema` | 100% |
| S25 | Todos los `plainDescription` en nivel C0 («La sesión registra…») | `TEST_VERIFIED` | `FocusClaimLevelGateTest` | 100% |
| S26 | Marketing ASO afirma «mide» / «para tu concentración» fuera del alcance escaneado | `REPO_VERIFIED` | `docs/marketing/assets/aso-google-play-es.md:15,17,19,29` | 100% |
| S27 | `ProbeExerciseMapping` sólo PVT/FLANKER/GO_NO_GO → E1/E2/E3; paradigmas no entrenados fallan cerrados | `TEST_VERIFIED` | `ProbeExerciseMappingTest` | 100% |
| S28 | El espejo Node reproduce 17 goldens Kotlin | `REIMPLEMENTATION_REPRODUCED` | `scratchpad/reimpl/goldens.mjs` 17/17 | 95% (ramas no cubiertas por goldens) |
| S29 | Gates verdes sobre XML frescos (4468/0/0/0; detekt 0) | `TEST_VERIFIED` | PARTE 0 §0.3 | 100% |
| S30 | iOS compila y ejecuta igual | `UNKNOWN` | sin host macOS | — |

## 2.3 Estado de las 12 lentes

| Lente | Tipo de agente | Pregunta falsable | Veredicto | Estado | Hallazgos |
|---|---|---|---|---|---|
| efficacy-guardian (**obligatoria**) | `efficacy-guardian` (sin Bash) | ¿Entrena y mide lo que afirma? | CONFIRMED | ENTREGADA (2.º intento del workflow; 1.º murió sin StructuredOutput) | 15: ningún número renderizado indexa un constructo (todo se reduce a ACCURACY de un bloque + resúmenes de RT clase D); SFI nunca entra en `sessionMetrics` ⇒ chip FOCO «—» y tendencia SFI vacía para siempre; FOCUS SCORE = ACCURACY×100 con confident=false por construcción; captions de tendencia direccionales sobre práctica/RTM/dispositivo; PS mezcla velocidad-precisión; «transferencia» = 1 línea de ACCURACY entre dos formas del PVT; dos carriles (progreso vs evaluación) pueden emitir veredictos distintos; INHIBITORY_CONTROL_INDEX sin productor |
| psychometrician (**obligatoria**) | `psychometrician` | ¿PS/GI/RCI son psicométricamente defendibles? | PARTIALLY_REFUTED | ENTREGADA | PS monótono en accuracy pero no en la variedad conductual; umbrales calibrados restauran monotonicidad; RCI sin ruta alcanzable; 40 ms a sdPre 60 exige rel ≥ 0.942; rel 0.8 marca falsamente S7/S9/S11/S12b; S11 ACC RCI 3.36 |
| assessment-psychometrics | `mindboost-assessment-psychometrics` | ¿Fiabilidad/validez/sesgos están arquitectónicamente cubiertos? | PARTIALLY_REFUTED | ENTREGADA | 10: `ReliabilitySource.EMPTY` es estructural pero por TRES barreras (población sin egreso; flags ⇒ ninguna fila admitida; esquema sin identidad de forma/n/SD); SEM intra-ocasión SÍ derivable (sd = RT_CV×RT_MEDIAN); 20/celda adecuado para mediana, marginal para CV, un orden de magnitud bajo para diferencias; SSRT 30 < consenso ~50; `MinSampleGate.RCI` nMin=2 sin consumidor; esquema aditivo mínimo propuesto |
| cognitive-science | `mindboost-cognitive-science` (2.º intento) | ¿Los 9 `Capacity` son constructos separables y bien nombrados? | PARTIALLY_REFUTED | ENTREGADA | 10: E1 es tarea de señal rara (6–30% objetivos + señuelos), no PVT; métricas base calculadas sin gate de paradigma (contradice Blueprint V3 §4); CONGRUENCY_EFFECT fusiona flanker+Stroop; PES dirección injustificada; membresía CORE E1/E3/E4/E2/E7, CROSS_DOMAIN E5/E12/E11/E13, EXCLUIR E6/E8/E9/E10; tabla de contaminación métrica×paradigma |
| assessment-diagnostics | `mindboost-assessment-diagnostics` (2.º intento) | ¿La línea base/calibración produce un punto de partida defendible? | PARTIALLY_REFUTED | ENTREGADA | 11: bloques de calibración E1+E2+E3 sin gate y umbrales AGRUPADOS entre paradigmas; umbrales se persisten aunque la sonda se deniegue (churn por reintento + quema de forma); lapso sólo sube (unidireccional); snapshot con umbrales PRE-calibración; covariables de estado nunca capturadas; anclaje de una sola administración; snapshot sin n/SE |
| cognitive-transfer | `mindboost-cognitive-transfer-agent` | ¿Existe una ruta honesta de transferencia? | PARTIALLY_REFUTED | ENTREGADA | Sin diseño de transferencia; KDoc «near-transfer»; `FunctionalProtocols` dormidos; marketing «mide»; `NEAR_TRANSFER_INDEX` sin productor |
| personalization-engine | `architect` (emulada) | ¿El motor personaliza por individuo o sólo por población? | PARTIALLY_REFUTED | ENTREGADA | Nivel embarcado L0; con lifecycle ON L1–L3; L7 nunca; `baselineRtCv` no cableado; arquetipo no promociona; recomendación siempre E1 |
| gameplay-lab | `mindboost-training-gameplay-lab` | ¿Los 13 ejercicios son entrenamiento real o skins? | PARTIALLY_REFUTED | ENTREGADA | 11: conjunto adaptativo alcanzable = 4 decisiones (Hold×3 + ContainImpulsivity); dificultad fija dentro de sesión; E3 calibración inerte; E1 entrenamiento resetea temporalUncertainty; escalera de exactitud inflada en tarea de detección; TIME_ON_TASK sin lectores; sin mecanismo de dificultad deseable; variante relajada parametrizable sólo por plantilla+ejes |
| mobile-performance | `architect` (emulada) | ¿El RT es interpretable en móvil? | PARTIALLY_REFUTED | ENTREGADA | Clase D; onset pre-render; tap-UP; E9/E10 anclaje; E4 SSD cota inferior; sin frame stats; Speed API≥34 clase C |
| data-analytics | `architect` (emulada) | ¿La telemetría permite auditar una sesión a posteriori? | PARTIALLY_REFUTED | ENTREGADA | 0 eventos; 9 hallazgos; tabla de telemetría (PARTE XXIV) |
| experimentation | `architect` (emulada) | ¿Existe capacidad de validar cambios experimentalmente? | PARTIALLY_REFUTED | ENTREGADA | Sin A/B; matriz de versionado 14 filas; roadmap PRE-ALPHA→TRANSFER; estudio de fiabilidad 8 semanas n≥100 |
| privacy-security | `privacy-security` (2.º intento) | ¿Los datos cognitivos están minimizados y borrables? | PARTIALLY_REFUTED | ENTREGADA | 9: ruta de egreso Firestore EXISTE pero no cableada + reglas deny + flag (tres barreras); `accountDeletion.ts` no borra nada de Enfoque (vacuamente cierto); dos namespaces (`mindboost_focus` + `mindboost_sessions`); borrado por `clearNamespace` blanket (sobre-borrado multi-cuenta); `SubjectiveStateDto` dormido; cifrado/backup OK; focus2 declara datos adyacentes a discapacidad |

Regla aplicada: una lente **sin veredicto** se declara **NO JUZGADA**; sus preguntas pasan a PARTE XXXIV (Unknowns). Todo hallazgo de lente que sostiene una conclusión de este informe fue **re-verificado en Kotlin** por la sesión principal (cita `fichero:línea`); los que no se pudieron verificar llevan `CLAIMED_NOT_VERIFIED`.

---

# PARTE III — Current Focus Architecture

Todo lo que sigue es `REPO_VERIFIED` salvo etiqueta contraria. El diagrama describe el motor **montado** (`focus` v1). `focus2` (V5) no aparece porque no tiene call-site desde el hub.

## 3.1 Diagrama de flujo (con estado de ejecución bajo flags embarcados)

```mermaid
flowchart TD
    HUB[MindBoostHubScreen.kt:117] --> ROOT[FocusRootScreen]
    ROOT --> HOME[FocusHomeScreen / FocusHomeStateHolder]
    HOME -->|needsBaseline = profile.baseline == null  :88| BASE[FocusBaselineScreen / FocusBaselineStateHolder]
    HOME -->|!needsBaseline  UI :158-175| SESS[FocusSessionScreen / FocusSessionStateHolder]
    HOME -->|cada carga :67-79| PLAN[DailyPlanGenerator + RecoveryPolicy + OvertrainingGuard + TrainingCycle]

    BASE --> CALB[3 bloques CALIBRATION sin gate<br/>E1 75 + E2 60 + E3 60 a 0.35/0.35<br/>umbrales = profile.thresholds PRE-calibracion]
    CALB --> PROBE[Bloque sonda VIGILANCE_PROBE 55 ensayos<br/>ProbeBankRepository.nextProbe -> forma reservada<br/>unico bloque con lifecycle + admit]
    PROBE --> HOLDER[FocusSessionStateHolder.presentAndAwait<br/>onset :266 antes de emitRunning<br/>respuesta detectTapGestures onTap = pointer UP]
    HOLDER --> ASM[FocusSessionAssembler.assembleBlock<br/>TrialClassifier classify/classifyChoice/classifyStop]
    ASM --> MET[DefaultMetricsEngine.blockMetrics/sessionMetrics<br/>26 MetricKey, ausencia tipada]
    ASM --> ADMIT[FocusSessionAssembler.admit :55-64<br/>gaming derivado -> SessionQualityGate.admit]
    ADMIT -->|NotMeasurable| WD[withdrawn() sobre toda metrica confident :247-250]
    ADMIT -->|Admitted| KEEP[metricas intactas]
    WD --> HIST[(SessionHistoryRepository<br/>SecureKeyValueStore cifrado)]
    KEEP --> HIST
    KEEP --> CAL[BaselineAssembler -> DefaultCalibrationEngine<br/>umbrales AGRUPADOS E1+E2+E3 (195 ensayos)<br/>initialDifficulty solo E1/E2/E3 - deviceFingerprint = null]
    CALB --> CAL
    CAL --> PROF[(FocusProfileRepository<br/>profile.baseline)]
    WD -.->|baseline = null pero thresholds + startingDifficulty SI se escriben :215-224 save en :221| PROF

    SESS --> HOLDER
    KEEP --> ADAPT[DefaultAdaptiveEngine.decideAdaptation<br/>impulsivityDecision precede guardas<br/>baselineCv = cfg.fallbackBaselineRtCv :96-98]
    ADAPT --> PROF
    KEEP --> SCORE{scoreSurfacesEnabled?<br/>FeatureFlags.kt:131 = false}
    SCORE -->|ON| PS[PersonalizedSessionScorer + ArchetypeInference + MicroPauseGate]
    SCORE -->|OFF| SUM[FocusSummaryScreen sin score]
    HIST --> PROG[FocusProgressScreen / FocusProgressStateHolder<br/>Trends + TransferReportBuilder]
    HIST --> EVAL[FocusEvaluationScreen / FocusEvaluationStateHolder]
    EVAL --> RCI[FocusEvaluationService.evaluate<br/>ProbeReliabilityEstimator MIN_ADMINISTRATIONS=3 + formIsFresh<br/>ReliabilitySource.EMPTY  FocusGraph.kt:156]
    RCI -->|siempre| NED[RCI = NotEnoughData]

    LIFE[AppLifecycleProbe<br/>focusLifecycleProbeEnabled=false -> DisabledAppLifecycleProbe :86-90<br/>NotObservable DISABLED_BY_FLAG :72-76] --> ADMIT
    HIST -.->|sin call-site prod| SYNC[SyncingFocusSessionHistoryRepository -> Firestore<br/>mindboostSyncFirestoreEnabled=false]
    HIST --> BRAIN[FocusProfileSource -> MindBoostProfileAggregator<br/>fail-closed CALIBRATING]
    E2ONLY[SessionRecorder.onStart plan.identity<br/>solo E2 FormDescriptorV1] -.-> HOLDER

    classDef dead fill:#f6d5d5,stroke:#b00
    classDef flag fill:#fff3c4,stroke:#b58900
    class SESS,KEEP,CAL,ADAPT,PS,SYNC dead
    class SCORE,LIFE flag
```

Leyenda: rojo = **no se ejecuta con los flags embarcados** (ruta inalcanzable o flag OFF); amarillo = nodo gobernado por flag.

## 3.2 Componentes por capa

| Capa | Fichero(s) | Rol | Estado |
|---|---|---|---|
| Composición / DI | `focus/di/FocusGraph.kt` (`generators`, `versionedGenerators`, `metricsEngine`, `adaptiveEngine`, `assembler`, `monotonicClock`, `epochClock`, `profileEvolution`, `overtrainingGuard`, `personalizedScorer`, `calibrationEngine`, `baselineProtocol`, `baselineAssembler`, `evaluationService`, 4 repositorios, `seedSource`, `focusEngine`); `presentation/RememberFocusStateHolders.kt` | Único punto de cableado; `rememberFocusGraph` instancia `DefaultMetricsEngine`, `DefaultAdaptiveEngine`, `DefaultCalibrationEngine`, `FocusEvaluationService()` (defaults ⇒ `ReliabilitySource.EMPTY`), `SecureKeyValueStore(Focus)` | Vivo |
| Catálogo / taxonomía | `domain/catalog/ExerciseCatalog.kt`, `domain/model/CognitiveTaxonomy.kt` (9 `Capacity`, 6 `Subdomain` A–F, `Paradigm` 14 valores, `DifficultyAxis` 9 ejes) | Declaración de constructos y ejercicios | Vivo (documental) |
| Generación | `domain/exercise/{E1..E13}*Generator.kt`, `TrialGenerator.conditionSchedule`, `FocusExerciseGenerators.registry`, `domain/engine/{FocusEngine,FocusFormContract,FormDescriptorV1}` | Formas deterministas (`DeterministicRng` splitmix64, `RngVersion`); sólo E2 con contrato de forma | Vivo |
| Sesión | `presentation/FocusSessionStateHolder.kt`, `domain/session/FocusSessionAssembler.kt`, `domain/exercise/TrialClassifier.kt`, `domain/quality/{SessionQualityGate,FocusAntiGaming}.kt`, `core/lifecycle/AppLifecycleProbe.kt`, `core/timing/MonotonicClock.kt` | Presentación, captura RT, clasificación, admisión | Vivo sólo para la sonda de base |
| Métricas | `domain/metrics/{DefaultMetricsEngine,MetricKey,MeasurementSufficiency,MeasurementReachability,RtStatistics}.kt`, `core/scoring/{MinSampleGate,SsrtCalculator,Probit}.kt` | 26 claves; pisos en sesión 3–6; piso central 20/celda | Vivo |
| Adaptación | `domain/adaptive/{AdaptiveEngine,AdaptiveModels,AdaptiveConfig}.kt` | accUp 0.85 / hold 0.72 / down 0.65; stepUp 0.08 / stepDown 0.12 | Inalcanzable |
| Calibración / base | `domain/evaluation/{BaselineAssembler,DefaultCalibrationEngine,BaselineProtocols,ProbeBank,ProbeDefinition,ProbeExerciseMapping}.kt` | Umbrales personales (lapse = max(500, mediana + 2.5·sMAD), MIN_SAMPLES 8), dificultad inicial en 4 niveles | Se ejecuta, resultado denegado |
| Evaluación longitudinal | `domain/evaluation/{FocusEvaluationService,ProbeReliabilityEstimator,ReliableChangeIndex,EvaluationComparator,EvaluationResult}.kt`, `domain/reporting/TransferReport.kt`, `domain/trend/Trends` | RCI Jacobson–Truax con `formIsFresh`; `withheldForSample`; `deltaText` por magnitud | Inalcanzable (EMPTY) |
| Personalización | `domain/personalization/{PersonalizedSessionScorer,ArchetypeInference,PersonalizationProfiles,MicroPauseGate}.kt`, `domain/session/{DailyPlanGenerator,TrainingCycle,RecoveryPolicy,OvertrainingGuard,ProfileEvolution}.kt` | Score 0–100 (W 0.5/0.2/0.3, PER_LAPSE_COST 0.03, MIN_N 20); arquetipos; micro-pausa; plan diario | Flag OFF / inerte |
| Persistencia | `data/dto/FocusDtos.kt`, `data/mapper/FocusPersistenceMappers.kt`, `data/local/{LocalFocusRepositories,SecureKeyValueStore,IndexedSecureKeyValueStore,CachingSecureKeyValueStore,FocusJson}.kt`, `core/session/{SessionRecord,SessionRecorder}` | 3 DTOs con `FocusSchema.CURRENT` compartido; resúmenes, sin trials; `readVersioned` cuarentena futuras | Vivo |
| Sync | `data/sync/*` (8 ficheros) | Cola cifrada + Firestore; `scoringVersion="focus-scoring-1"` | Sin call-site |
| Perfil cerebral | `profile/data/adapter/FocusProfileSource.kt` | `ModuleContribution` fail-closed | Vivo (CALIBRATING) |
| UI | `presentation/ui/{FocusRoot,FocusHome,FocusBaseline,FocusSession,FocusSummary,FocusProgress,FocusEvaluation}Screen.kt`; `components/{FocusStimulusField,FocusChoiceField,FocusRuleSwitchField,FocusTrackingField,FocusStreamField,FocusDualTaskField,FocusSearchField,FocusAnchorField}.kt` + `FocusFieldVisual`, `FocusGlyphButton`, `FocusMeasureRunningContent`, `FocusGauge`, `FocusAtmosphere` | 8 campos de estímulo, todos con `detectTapGestures`; E9/E10 con animación propia sobre la ventana | Vivo |
| Flags | `core/config/FeatureFlags.kt:116-117,131-132`; Remote Config (`RemoteConfigFeatureFlags` en `androidApp`) | 4 flags de Enfoque, todos OFF por defecto | Vivo |
| Telemetría | `presentation/FocusScoreContainmentBreadcrumb.kt` | 1 breadcrumb; 0 eventos | Vivo (mínimo) |

## 3.3 Contratos de frontera relevantes

- **Assembler ← Holder**: `RecordedTrial(stimulus, latencyMs, responseKey, stopSignalDelayMs)`; sin timestamp de onset absoluto, sin timebase, sin frame stats. `TrialResult.timestampMs` se rellena con `endedAtMs` del bloque para todos los ensayos (`FocusSessionAssembler.kt:108/146`).
- **Gate ← Assembler**: `admit(blocks, lifecycle, gaming)`; `gaming` obligatorio sin default (R15).
- **Persistencia ← Holder**: `SessionResult` (id `"$userId:$endedAtMs"`), `SessionSummaryDto` (sólo `schemaVersion`), trials recortados a conteo en escritura (`FocusPersistenceMappers.kt:130`) y devueltos vacíos en lectura (`:137`).
- **Evaluación ← Repos**: `evaluate(baseline, retest, repeatsEarlierForm: Boolean?)` obligatorio; `formIsFresh = (repeatsEarlierForm == false)`.
- **Perfil cerebral ← Historial**: `FocusProfileSource` exige `eligibleForProfile()` (≥120 s + evidencia) y devuelve `null` si nada elegible.

---

# PARTE IV — Current Construct Model

## 4.1 Qué significa hoy «Enfoque» en el código

`REPO_VERIFIED`. «Enfoque» no está definido como constructo en ningún símbolo. Lo que existe es una **taxonomía de tres niveles** en `domain/model/CognitiveTaxonomy.kt`: `CognitiveModule` (6 módulos A–F «del diseño funcional»), `Capacity` (9 subcapacidades «de control atencional») y `Paradigm` (14 linajes científicos). Cada `ExerciseDefinition` declara un módulo, una capacidad y un paradigma. El dominio canónico del perfil es `attention` (CLAUDE.md §1) y `FocusProfileSource` alimenta la dimensión ATTENTION del perfil cerebral con **una sola contribución agregada**: en el perfil, «Enfoque» colapsa a un número (hoy `CALIBRATING`).

Lectores reales de la taxonomía en producción (fuera del catálogo): `Capacity` la leen `ProbeBank` y `BaselineProtocol` (para elegir sonda); `CognitiveModule` **no la lee nadie** (documental); `Paradigm` la lee `ProbeExerciseMapping` (sólo PVT/FLANKER/GO_NO_GO; `MACKWORTH_CLOCK` y `SIMON` fallan cerrados, `TEST_VERIFIED`).

## 4.2 Taxonomía real actual (13 ejercicios × 9 capacidades × 6 módulos)

| Capacity | Módulo | Ejercicios | Paradigma | Métrica gobernante (documental) | Eje gobernante | Sonda reservada |
|---|---|---|---|---|---|---|
| SUSTAINED_VIGILANCE | A_VIGILANCE | E1 Vigía | PVT | RT_CV | TIME_ON_TASK | `probe_vigilance_v1` (PVT, formas 1001–1004) |
| SELECTIVE_FILTERING | B_SELECTIVE | E2 Filtro · E7 Rastreo · E9 Enjambre | FLANKER · VISUAL_SEARCH · MOT | CONGRUENCY_EFFECT · SET_SIZE_SLOPE · ACCURACY | INTERFERENCE · PERCEPTUAL_LOAD · PERCEPTUAL_LOAD | `probe_selective_v1` (FLANKER, 2001–2004) |
| RESPONSE_INHIBITION | C_INHIBITION | E3 Gatillo · E4 Freno | GO_NO_GO · STOP_SIGNAL | FALSE_ALARMS · SSRT | INHIBITORY_DEMAND ×2 | `probe_inhibition_v1` (GO_NO_GO, 3001–3004) |
| INTERFERENCE_CONTROL | D_INTERFERENCE | E5 Conflicto | STROOP | CONGRUENCY_EFFECT | INTERFERENCE | — |
| CONTROLLED_FLEXIBILITY | D_INTERFERENCE | E6 Cambio | TASK_SWITCHING | SWITCH_COST | INTERFERENCE | — |
| TEMPORAL_ATTENTION | E_RECOVERY | E10 Flujo · E12 Re-enganche | RSVP · PVT | ACCURACY · POST_ERROR_ACCURACY | TEMPORAL_UNCERTAINTY · PRESSURE | — |
| DUAL_TASK_INTEGRATION | F_INTEGRATION | E8 Dúo | DUAL_TASK | DUAL_TASK_COST | DUAL_TASK_LOAD | — |
| DISTRACTION_RESISTANCE | F_INTEGRATION | E11 Ancla | DIGITAL_DISTRACTION | DISTRACTION_RESISTANCE_INDEX | DISTRACTOR_SALIENCE | — |
| PRECISION_UNDER_PRESSURE | F_INTEGRATION | E13 Presión | DEADLINE_PRESSURE | PRESSURE_DELTA | PRESSURE | — |

Observaciones estructurales (`REPO_VERIFIED`):

1. **3 de 9 capacidades tienen sonda** de medición oficial; las otras 6 sólo tienen ejercicios de entrenamiento. Ninguna afirmación de cambio es posible hoy para INTERFERENCE_CONTROL, CONTROLLED_FLEXIBILITY, TEMPORAL_ATTENTION, DUAL_TASK_INTEGRATION, DISTRACTION_RESISTANCE ni PRECISION_UNDER_PRESSURE.
2. **La sonda de vigilancia y E1 son el mismo paradigma con la misma tarea** (PVT → `E1_VIGIA` en `ProbeExerciseMapping`), separados sólo por espacio de semillas reservado y dificultad fija. Lo mismo para FLANKER→E2 y GO_NO_GO→E3. La medición «oficial» es el entrenamiento con semillas distintas: **cero independencia de tarea** entre entrenamiento y medida (problema de práctica específica, §XIX).
3. `SELECTIVE_FILTERING` agrupa tres paradigmas con mecanismos distintos (supresión de flancos, búsqueda serial, seguimiento múltiple); E9 (MOT) usa `ACCURACY` como gobernante, lo que la lente psicométrica y el ledger (`MB-FOCUS-0139/0140`) ya marcaron como métrica sin techo defendible.
4. `TEMPORAL_ATTENTION` agrupa E10 (RSVP, atención temporal genuina) con E12 (PVT con recuperación post-lapso), que no es atención temporal sino vigilancia sostenida con etiqueta distinta; E12 es funcionalmente E1.
5. `INTERFERENCE_CONTROL` (E5) y `CONTROLLED_FLEXIBILITY` (E6) comparten módulo y eje (`INTERFERENCE`), pero E6 es un paradigma de cambio de tarea, no de control de interferencia.
6. Cinco ejercicios (E3, E5, E10, E11, E13) **no tienen entrada** en el documento de diseño `03_1_Enfoque.md` (que describe 8: Vigía, Filtro, Freno, Conmutador, MOT, Cazador, Reenganche, Doble Vía). Su justificación de constructo vive sólo en el catálogo Kotlin.
7. El modelo no declara **jerarquía** (qué capacidad es prerequisito de cuál), ni **pesos** para componer «Enfoque», ni **correlaciones esperadas** entre capacidades. `FocusProfileSource` agrega todo a un solo número sin declarar la fórmula de agregación por capacidad.

## 4.3 Tabla de contaminación (impureza de tarea)

`SCIENTIFICALLY_SUPPORTED` (Miyake et al. 2000, task impurity; Hedge, Powell & Sumner 2018, reliability paradox; Draheim et al. 2019/2021 attention control) + `REPO_VERIFIED` para el mecanismo concreto.

| Ejercicio | Constructo declarado | Contaminantes principales | Vía en el código | Severidad |
|---|---|---|---|---|
| E1 Vigía («PVT» declarado; **tarea de señal rara** real) | Vigilancia sostenida | Velocidad motora; latencia de captura (clase D); estrategia de anticipación; somnolencia/circadiano; **con 6–30 % de objetivos y señuelos, la exactitud premia «no responder» (techo ciego ≈ 0.79)** | `E1VigiaGenerator.kt:30-75`: `targetRarity = lerp(0.30, 0.06, PL)`, `lureProbability = lerp(0, 0.35, salience)`, ISI 1200–7000 ms; RT sin corrección; ventana 1400→950 ms penaliza lentos honestos (`MB-FOCUS-0146`) | Alta |
| E2 Filtro (Flanker) | Filtrado selectivo | Velocidad general (efecto de congruencia = diferencia de RT ⇒ hereda ruido ×2); aprendizaje del mapeo de teclas | `CONGRUENCY_EFFECT` como diferencia de medianas sobre celdas 11–13 | Alta |
| E3 Gatillo (Go/No-Go) | Inhibición de respuesta | Sesgo de respuesta (criterio), vigilancia; el techo de estrategia ciega es `max(p,1−p)` | `FALSE_ALARMS` sin d'/criterio separados | Media |
| E4 Freno (Stop-Signal) | Inhibición (SSRT) | SSD pedida registrada como usada; efectiva sesgada al alza en expectativa (timer + render) ⇒ SSRT sesgado; `goPhase` no espera al render; estrategia de esperar (slowing) sin detector cableado (sólo banda p(respond) post-hoc) | `presentStopTrial :584-598` | Alta |
| E5 Conflicto (Stroop) | Control de interferencia | Lectura/vocabulario visual; velocidad; congruencia como diferencia | Igual que E2 | Alta |
| E6 Cambio (Task-switching) | Flexibilidad | Memoria de trabajo (regla activa); aprendizaje del mapeo; discriminación de color (mitigada por luminancia, R10) | `SWITCH_COST` diferencia de RT | Alta |
| E7 Rastreo (Búsqueda visual) | Filtrado / búsqueda | Agudeza visual; tamaño de pantalla; estrategia de barrido | `SET_SIZE_SLOPE` con tamaños fijados por dificultad | Media |
| E8 Dúo (Dual-task) | Integración | Priorización estratégica; el contador secundario es visual (no cargas separadas) | `DUAL_TASK_COST` | Media |
| E9 Enjambre (MOT) | Seguimiento múltiple | Sonda única por ensayo (1 bit); RT anclado al inicio del tween; techo de estrategia ciega alto | `FocusTrackingField` PROBE_START=0.82 | Alta |
| E10 Flujo (RSVP) | Atención temporal | RT anclado al inicio de la ráfaga (80 ms/ítem); detección ≠ blink atencional | `FocusStreamField` `delay(80)` | Alta |
| E11 Ancla (distracción digital) | Resistencia a distracción | Sin dependencia de un constructo atencional específico; saliencia de distractor = parámetro visual | `DISTRACTION_RESISTANCE_INDEX` (1−x) | Media |
| E12 Re-enganche (PVT + recuperación) | Recuperación post-lapso | Es E1 con otra etiqueta; `POST_ERROR_*` exigen errores (MIN_N_POST_ERROR 3) que un buen usuario no comete | Redundante con E1 | Alta (redundancia) |
| E13 Presión (deadline) | Precisión bajo presión | Speed–accuracy trade-off inducido por diseño, no una capacidad; `PRESSURE_DELTA` sólo a nivel de bloque | Métrica sin nivel sesión | Media |

## 4.4 Qué constructo mide realmente la línea base

`REPO_VERIFIED` (`BaselineProtocol.kt:80-125`, `BaselineAssembler.kt:30-60`, `FocusBaselineStateHolder.kt:100-170,195-227`). El protocolo `INITIAL` corre **cuatro bloques en una sola sesión**: tres bloques `CALIBRATION` —E1 75 ensayos, E2 60, E3 60, todos a `calibrationDifficulty` (perceptualLoad 0.35 / temporalUncertainty 0.35)— y un bloque `EVALUATION` de 55 ensayos de `VIGILANCE_PROBE` (forma reservada, dificultad fija 0.4/0.4). De ahí se derivan tres cosas distintas, con tres calidades distintas:

1. **Umbrales personales** (`lapseRtMs = max(500, mediana + 2.5·sMAD)`, `baselineRtCv = SD/mediana`): calculados por `calibrationEngine.calibrate(calibrationBlocks.flatMap { it.trials })`, es decir sobre los **195 ensayos agrupados de tres paradigmas distintos** (RT simple E1 + RT de elección E2 + go/no-go E3). Los tres bloques de calibración corren **sin ningún gate** (sin sonda de ciclo de vida, sin anti-gaming, sin mínimo de esfuerzo: sólo el bloque de sonda pasa por `admitProbe`), y sus umbrales se persisten **aunque la sonda se deniegue** (`persist` escribe `thresholds` + `startingDifficulty` con `baseline = null` en la rama `NotMeasurable`).
2. **`startingDifficulty`**: sólo para E1/E2/E3, desde la exactitud de su bloque de calibración (`initialDifficulty`, 4 niveles; `valid == 0 → LEVEL_MIN`); los otros 10 ejercicios quedan en `EASIEST`. Para E3 es inerte (su mapeo no lee `PERCEPTUAL_LOAD`) y para E1 **resetea `temporalUncertainty` a 0**, es decir más fácil que la propia calibración.
3. **Snapshot `perMetric`**: sólo del bloque de sonda (`probeBlock.metrics`), calculado con los umbrales **pre-calibración** (`profile.thresholds` cargado en `:101`, default 500 ms en el primer intento) y aplanado a `Map<MetricKey, Double>` sin n ni SE.

Por tanto, el «perfil actual» con el que el sistema personaliza sigue siendo **de una sola administración y esencialmente de vigilancia**: el snapshot es sólo la sonda E1, y los umbrales son un promedio entre paradigmas que no describe a ningún ejercicio en particular. E2/E3 entran en la calibración, pero **ninguna métrica de filtrado o inhibición entra en el snapshot** contra el que se compara el progreso.

## 4.5 Definición operativa que este informe adopta (para las partes V–VI)

`RECOMMENDATION`. «Enfoque» = **control atencional funcional**: la capacidad de (a) mantener la disponibilidad de respuesta en el tiempo (vigilancia), (b) seleccionar la información relevante frente a la irrelevante (filtrado/interferencia) y (c) inhibir o reconfigurar la respuesta cuando la regla lo exige (inhibición/flexibilidad). Esta definición es compatible con la evidencia de un factor de «attention control» (Draheim/Unsworth) cuando se mide con tareas de exactitud y no sólo de diferencia de RT, y es la que permite que el catálogo no crezca por estética.

---

# PARTE V — Scientific Construct Audit

Todas las entradas de literatura son `SCIENTIFICALLY_SUPPORTED` con confianza declarada; ninguna se ha usado para inflar una afirmación del producto. Cada entrada declara qué cambiaría en este informe si fuera falsa.

## 5.1 Literatura de referencia

| Referencia | Evidencia | Aplicabilidad a Molvex | Limitaciones | Confianza | Qué cambiaría si fuera falsa |
|---|---|---|---|---|---|
| Owen et al. 2010 (BBC Brain Training, n≈11k) | Mejoras grandes en las tareas entrenadas; transferencia nula a tareas no entrenadas | Toda afirmación de mejora debe ser «en la tarea/paradigma»; ningún «mejora tu concentración» | Adultos sanos online, 6 semanas, sin dosis individualizada | Alta | Se podría afirmar transferencia; el marketing ASO dejaría de ser una violación |
| Simons et al. 2016 (revisión) | Evidencia de far-transfer «inadecuada»; diseños con control activo escasos | Exige control activo en cualquier estudio de eficacia (PARTE XXVII) | Revisión narrativa-crítica | Alta | Un control pasivo bastaría para las fases beta |
| Melby-Lervåg & Hulme 2013/2016; Sala & Gobet 2017/2019 (meta-análisis) | Near-transfer moderado, far-transfer ≈ 0 tras controlar por diseño | Justifica la taxonomía «CORE = lo que se puede medir con tarea de exactitud» y la separación entrenamiento/medida | Heterogeneidad de poblaciones | Alta | El GI podría interpretarse como capacidad general |
| Hedge, Powell & Sumner 2018 (reliability paradox) | Tareas cognitivas «robustas» (Stroop, Flanker, Stop-signal) tienen ICC test-retest bajos (0.3–0.7) porque la varianza entre sujetos es pequeña | Las métricas de diferencia (CONGRUENCY_EFFECT, SWITCH_COST, SSRT) son las peores candidatas a medir cambio individual; refuerza que E2/E5/E6 sean SECONDARY para medición | ICC depende del n de ensayos; con >100 ensayos por celda mejora | Alta | E2/E5/E6 podrían ser instrumentos oficiales con 55 ensayos |
| Rouder & Haaf 2019 | Los efectos de diferencia individuales requieren cientos de ensayos por condición para separar señal de ruido | Cuantifica la «reachability»: 55 ensayos deja ~11 celdas ⇒ inutilizable para diferencias | Modelos jerárquicos, no ICC clásico | Alta | El piso de 20/celda (`MinSampleGate`) sería suficiente para diferencias |
| Miyake et al. 2000 (task impurity) | Ninguna tarea mide un solo constructo; se necesita variable latente sobre ≥3 tareas | Un solo ejercicio nunca domina una subhabilidad (invariante §42); el catálogo necesita ≥2–3 tareas por constructo CORE | Modelo de 3 factores (updating, shifting, inhibition) | Alta | Un ejercicio por constructo sería aceptable |
| Dinges & Powell 1985; Basner & Dinges 2011 (PVT) | PVT de 10 min es sensible a privación de sueño; lapsos >500 ms y RT inverso (1/RT) son las métricas más sensibles; PVT-B (3 min) pierde sensibilidad | E1 se **declara** PVT (`Paradigm.PVT`) pero **no lo es** (`REPO_VERIFIED` `E1VigiaGenerator.kt:30-75`): sirve objetivos raros (6–30 %) entre no-objetivos y señuelos, con ISI 1.2–7 s. Un PVT es 100 % objetivos con foreperiodo aleatorio; las métricas canónicas de Dinges (lapsos > 500 ms sobre TODOS los ensayos, 1/RT) no se transportan a una tarea con 11–20 hits por bloque. El umbral 500 ms y el suelo 150 ms se importan de un paradigma que la tarea no implementa | PVT mide alerta/somnolencia, no «concentración»: el confound circadiano es el principal; en una tarea de señal rara el confound dominante es el **sesgo de respuesta** | Alta | Un bloque corto de detección rara tendría la sensibilidad del PVT completo |
| Mackworth 1948; Parasuraman 1979 (vigilance decrement) | El decremento de vigilancia requiere ≥20–30 min de tarea monótona; tasa de evento baja | `VIGILANCE_DECREMENT_SLOPE` con MIN_N_SLOPE 6 sobre 2–3 min mide ruido, no decremento | Tareas de laboratorio | Alta | La pendiente en 3 min sería interpretable |
| Robertson et al. 1997 (SART); Esterman et al. 2013 (gradCPT) | No-go infrecuente y variabilidad de RT como índices de lapsos atencionales; gradCPT mide fluctuaciones continuas | E3 y el `RT_CV` de E1 tienen base; E12 «re-enganche» carece de paradigma propio | SART es sensible a estrategia speed–accuracy | Media-Alta | E12 tendría justificación independiente |
| Verbruggen et al. 2019 (consenso SSRT) | Método de integración con reemplazo de omisiones; ≥50 stops; no usar media; excluir participantes con p(respond·signal) fuera de 0.25–0.75 o violación de independencia | E4 con SSD cota inferior y ~20 stops alcanzables en 60 ensayos viola varias recomendaciones: SSRT de E4 es RESEARCH | — | Alta | E4 sería instrumento oficial |
| Hautus 1995 (corrección loglineal) | Corrige tasas 0/1 pero sesga de forma desigual con celdas desbalanceadas | Aplicado en Speed; en Enfoque no hay d' (sólo FALSE_ALARMS), lo que evita el sesgo pero pierde la separación sensibilidad/criterio | — | Alta | Se podría añadir d' sin cuidado |
| Draheim et al. 2019, 2021; Unsworth et al. 2021 (attention control) | Las tareas de diferencia de RT tienen mala fiabilidad; las de exactitud bajo presión de tiempo (antisaccade, flanker adaptativo por exactitud) miden mejor el factor | La taxonomía recomendada prioriza medidas de **exactitud** con deadline adaptativo sobre diferencias de RT | Muestras universitarias | Alta | Las métricas de diferencia de RT serían buenas medidas oficiales |
| Nicosia et al. 2022; Passell et al. 2021 (RT en móvil; citados en charter `03:1011-1013`) | RT en smartphone es fiable y válido **si** el pipeline de captura está caracterizado; la variabilidad de dispositivo añade 20–60 ms de sesgo y varianza | Justifica la clasificación A–E y el requisito de `deviceFingerprint` + timebase por fila | Dispositivos concretos | Alta | La clase D sería aceptable como oficial |
| Jacobson & Truax 1991 (RCI); Chelune et al. 1993 (práctica) | RCI = (post − pre)/SEdiff; Chelune añade término de práctica esperado | El RCI del repo carece de término de práctica (`MB-FOCUS-0133`) y de fiabilidad medida | Requiere ICC y SD de una población de referencia | Alta | Sin práctica, el RCI sería insesgado |
| Enge et al. 2014; Karbach & Kray 2009 (entrenamiento de inhibición / cambio) | Entrenamiento de inhibición: near-transfer mínimo; task-switching con variabilidad de tareas: near-transfer moderado | El «switch» E6 tiene mejor base de entrenamiento que E3/E4 | Poblaciones específicas | Media | La prioridad E4 > E6 se invertiría |
| Eriksen & Eriksen 1974 (Flanker) | Efecto de compatibilidad de flancos robusto a nivel de grupo | Base paradigmática de E2 y de `probe_selective_v1` | Efecto de grupo ≠ diferencia individual fiable | Alta | — |
| Whelan 2008 (ex-Gaussian); Ratcliff & McKoon 2008 (DDM) | La distribución de RT porta más información que la mediana; τ (cola) correlaciona con lapsos atencionales | El motor sólo persiste mediana/CV; τ y la cola requieren RT por ensayo (no persistidos) | Requiere ≥100 RT por estimación estable | Alta | Persistir sólo resúmenes no perdería información |

## 5.2 Crítica del modelo de constructos actual

1. **«Enfoque» es una etiqueta de dominio, no un constructo medido.** `REPO_VERIFIED`: no hay fórmula de agregación por capacidad ni modelo latente; el perfil colapsa a un valor por `FocusProfileSource`. Literatura: sin ≥3 indicadores por constructo (Miyake), el número es impuro por construcción.
2. **La medición oficial es la misma tarea que el entrenamiento** (PVT→E1, Flanker→E2, Go/No-Go→E3). Owen 2010 garantiza que la mejora en la tarea entrenada es real y no transferible; medir con la tarea entrenada mide práctica específica (§XIX).
3. **Métricas de diferencia como gobernantes** (CONGRUENCY_EFFECT, SWITCH_COST, SSRT, DUAL_TASK_COST, SET_SIZE_SLOPE) chocan con Hedge 2018 y Rouder & Haaf 2019: baja fiabilidad individual, imposible de subsanar con 55–60 ensayos. Aunque el RCI tuviera fiabilidad medida, sería baja para estas métricas.
4. **E1 no es el paradigma que declara** (refutación K2: sobrevive 3/3, 92–95 %; la etiqueta `PVT` la llevan TAMBIÉN `E12_REENGANCHE` (`ExerciseCatalog.kt:224`) y `VIGILANCE_PROBE` (`ProbeBank.kt:41`); sin consecuencia en la regla adaptativa, porque E1 sí tiene demanda de retención y `GO_NO_GO_PARADIGMS` acierta por accidente de etiqueta — el daño se confina a constructo/transferencia). `ExerciseCatalog` y `ProbeExerciseMapping` lo etiquetan `PVT`, pero el generador produce una tarea de **detección de señal rara con señuelos** (Mackworth/CPT-like), donde `ACCURACY = (HIT + CR)/válidos` con ~78 % de no-objetivos premia la estrategia «no responder» (techo ciego ≈ 0.79, `FocusAntiGaming.blindCeiling`) y donde lapsos y RT_CV descansan sobre 11–20 hits. La etiqueta arrastra tres consecuencias: (a) los umbrales canónicos del PVT (500/150 ms) se aplican a una distribución que no es la del PVT; (b) la escalera de calibración por exactitud (≥.90/.80/.70) está inflada porque la exactitud es mayoritariamente correct-rejection; (c) `FocusAdaptiveRulePolicy.GO_NO_GO_PARADIGMS` incluye PVT, así que E1 recibe `ContainImpulsivity` sobre un eje sin lectores. Además, en 2–3 min la sensibilidad a fatiga/decremento (`FATIGUE_INDEX`, `VIGILANCE_DECREMENT_SLOPE`) no puede sostenerse (Basner & Dinges 2011; Mackworth): esas dos métricas son ruido estructural a esa dosis, sea PVT o señal rara.
5. **SSRT de E4** viola el consenso de 2019 (la SSD pedida se registra como usada y la efectiva queda sesgada al alza; pocos stops; sin comprobación de independencia ni de slowing intra-bloque — la banda p(respond·señal) 0.25–0.75 existe, pero post-hoc sobre `confident`, `SsrtCalculator.kt:54`; refutación K5: sobrevive, 88 %). Es RESEARCH, no medida.
6. **Confound circadiano/somnolencia**: el PVT es el instrumento canónico de somnolencia; sin hora del día ni sueño auto-reportado (existe `subjective state` en persistencia, `PERSISTED health-adjacent`, pero no entra en el scoring), el «progreso» en E1 puede ser sólo estar más descansado.
7. **Sin d'/criterio**: E3 con `FALSE_ALARMS` sin separar sensibilidad de sesgo confunde «inhibe mejor» con «responde menos».
8. **Sin distribución de RT**: persistir mediana/CV pierde la cola (τ), que es donde vive la señal de lapso atencional (Whelan 2008).

## 5.3 Lo que la literatura SÍ respalda del diseño actual

- Separar formas reservadas de medición de las de entrenamiento (`ProbeBank.allReservedSeeds` disjunto): correcto y raro en el sector.
- Umbral de lapso 500 ms y `DEFAULT_ANTICIPATION_FLOOR_MS = 150`: canónicos (PVT).
- Umbral personal de lapso robusto (`mediana + 2.5·sMAD`): defendible frente a un umbral fijo, siempre que se recalcule con ≥5 sesiones (charter §11.1), no con una.
- Exigir `formIsFresh` para el RCI: evita el efecto de forma repetida (práctica de ítems), aunque no el de práctica de tarea (Chelune).
- Precedencia de veredictos de calidad y ausencia tipada: coherente con la exigencia de reportar «no medible» en vez de un número (charter).

## 5.4 Integración de la lente cognitive-science

Veredicto de la lente: `PARTIALLY_REFUTED` (pregunta: «¿los 9 `Capacity` son constructos separables y bien nombrados?»). Lo que sobrevive tras re-verificación en Kotlin por la sesión principal:

| # | Hallazgo (re-verificado) | Evidencia | Consecuencia para el constructo |
|---|---|---|---|
| 1 | E1 es tarea de señal rara, no PVT | `E1VigiaGenerator.kt:30-75` (`REPO_VERIFIED`) | La capacidad `SUSTAINED_VIGILANCE` se mide con un instrumento cuya métrica dominante es sesgo de respuesta, no lapsos |
| 2 | Las 15 métricas base se calculan para los 13 ejercicios **sin gate de paradigma** (`DefaultMetricsEngine.kt:70-114`); sólo `addParadigmMetrics :312-327` filtra por `primaryMetrics` | `REPO_VERIFIED`; contradice `ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md` §4 (IMPLEMENTATION_OPEN B2/T7) | LAPSES/RT_CV/SFI/BLOCK_STABILITY existen para flanker, MOT, RSVP… donde no tienen interpretación; y las métricas con interpretación (CONGRUENCY_EFFECT, SSRT, SWITCH_COST) **no tienen consumidor vivo** |
| 3 | `CONGRUENCY_EFFECT` es una sola clave para flanker (E2) y Stroop (E5) | `ExerciseCatalog.kt`; `DefaultMetricsEngine.kt` | Dos mecanismos (supresión perceptiva vs conflicto semántico) colapsan en un número; Trends/TransferReport no podrían distinguirlos |
| 4 | `POST_ERROR_SLOWING` tratado como «menor es mejor» en `Trends.kt:16-28` | `REPO_VERIFIED` | La literatura no sostiene una dirección única (PES adaptativo vs orientación); la caption de tendencia puede invertir el signo |
| 5 | `SELECTIVE_FILTERING` agrupa flanker, búsqueda serial y MOT; `TEMPORAL_ATTENTION` agrupa RSVP y «PVT con recuperación» | `CognitiveTaxonomy.kt`; `ExerciseCatalog.kt` | Dos capacidades declaradas no son separables por sus propios ejercicios |
| 6 | Membresía propuesta por la lente: CORE E1 (con la salvedad de señal rara), E3, E4, E2, E7 (fuera del compuesto); CROSS_DOMAIN E5, E12, E11, E13; EXCLUIR de Enfoque E6, E8, E9, E10 | `RECOMMENDATION` | Integrada y reconciliada con las otras lentes en PARTE VI |
| 7 | Tabla de contaminación métrica × paradigma (V válida / C contaminada / X sin sentido / 0 no aplica): RT_CV, LAPSES, SFI válidas sólo en E1/E12; IES/RCS/LISAS X en E1; PES V en E3/E4/E12; CONGRUENCY_EFFECT V en E2/E5; SSRT V sólo E4; SWITCH_COST V sólo E6 | `SCIENTIFICALLY_SUPPORTED` + `REPO_VERIFIED` para el productor | Base de la matriz métrica × ejercicio de PARTE XI |

Lo que la sesión principal **no** acepta de la lente tal cual: su propuesta de excluir E6 (task-switching) de Enfoque choca con Karbach & Kray 2009 (mejor base de entrenamiento que E3/E4) y con Draheim 2021 (flexibilidad como faceta del control atencional); se reclasifica como SECONDARY (PARTE VI), no como EXCLUIR.

---

# PARTE VI — Recommended Focus Taxonomy

`RECOMMENDATION` salvo etiqueta contraria. Reconcilia las lentes cognitive-science, assessment-psychometrics, cognitive-transfer y gameplay-lab con la literatura de PARTE V y con lo que el código puede medir hoy (PARTE IV). No diseña el catálogo: fija **qué constructos existen, cuáles se miden oficialmente y cuáles sólo se entrenan**.

## 6.1 Constructo raíz

**Enfoque = control atencional funcional** (definición operativa de §4.5): mantener disponibilidad de respuesta (vigilancia), seleccionar lo relevante (filtrado/interferencia) e inhibir o reconfigurar la respuesta cuando la regla lo exige (inhibición/flexibilidad). No es un número único: es un **perfil de 3–4 subhabilidades** con un compuesto opcional que sólo puede existir cuando ≥2 subhabilidades tienen estimación calibrada.

## 6.2 Clasificación de subhabilidades

| Subhabilidad | Clase | Definición operativa | Paradigmas de medida aceptables | Paradigmas sólo-entrenamiento | Ejercicios actuales | Justificación |
|---|---|---|---|---|---|---|
| **Vigilancia sostenida** | CORE | Mantener velocidad y disponibilidad de respuesta ante estímulos frecuentes en bloques ≥3 min; indicadores: lapsos, cola de RT, RT_CV | PVT real (100 % objetivos, foreperiodo aleatorio) o CPT/gradCPT con tasa de señal ≥50 % | Señal rara (Mackworth) como entrenamiento de mantenimiento | E1 (declarado PVT; es señal rara), E12 (E1 con etiqueta) | Dinges 1985; Basner 2011; Esterman 2013 |
| **Inhibición de respuesta** | CORE | Retener una respuesta prepotente; indicadores: d'/criterio go/no-go, SSRT sólo con ≥50 stops y SSD real | Go/No-Go con d' + criterio; Stop-signal (RESEARCH hasta cumplir Verbruggen 2019) | Stop-signal corto | E3, E4 | Verbruggen 2019; Miyake 2000 |
| **Filtrado / control de interferencia** | CORE | Responder al objetivo ignorando distractores espaciales o semánticos; indicador oficial: **exactitud con deadline adaptativo**, no diferencia de RT | Flanker/Stroop por exactitud bajo deadline (Draheim 2021); antisacada-like | Flanker/Stroop por RT (efecto de congruencia) | E2, E5 | Draheim 2019/2021; Hedge 2018 |
| **Flexibilidad (cambio de set)** | SECONDARY | Reconfigurar la regla activa; indicador: exactitud en switch bajo deadline; SWITCH_COST sólo descriptivo | Task-switching por exactitud | Task-switching por RT | E6 | Karbach & Kray 2009; Rouder & Haaf 2019 |
| **Búsqueda / selección espacial** | SECONDARY | Localizar objetivo entre distractores; SET_SIZE_SLOPE descriptivo | Búsqueda visual por exactitud con set size fijo | Búsqueda por RT | E7 | Miyake 2000 (impureza); lente cs |
| **Atención temporal (RSVP)** | CROSS_DOMAIN | Detección en flujo rápido; comparte varianza con memoria de trabajo y velocidad perceptiva | — (no oficial) | RSVP | E10 | Lente cs: detección ≠ blink |
| **Seguimiento múltiple (MOT)** | CROSS_DOMAIN | Seguir varios objetivos en movimiento; carga espacial + memoria | — (no oficial) | MOT | E9 | `MB-FOCUS-0139/0140`: techo ciego alto, 1 bit/ensayo |
| **Integración dual** | CROSS_DOMAIN | Mantener dos tareas; mide priorización estratégica | — | Dual-task | E8 | DUAL_TASK_COST = acc(single) − acc(dual) sobre la tarea PRIMARIA (`DefaultMetricsEngine.kt:375-384`; `ParadigmMetricsTest.kt:59` da 0,4): NO es ≡ 0 —refutado por K8 3/3—; el defecto real es que la demanda secundaria (contador) nunca se puntúa, así que ignorarla da el mejor valor 0,0 (`MB-FOCUS-0141`, `FocusBlindStrategyTest.kt:214-262`) |
| **Resistencia a distracción digital** | CROSS_DOMAIN | Ignorar distractores salientes tipo notificación; validez ecológica, no constructo | — | Anchor | E11 | Sin paradigma validado |
| **Precisión bajo presión** | EXCLUDE (como subhabilidad) → **dimensión de dificultad** | Es un eje (PRESSURE/deadline) aplicable a cualquier tarea, no una capacidad | — | — | E13 | Speed–accuracy trade-off inducido (§4.3) |
| **Recuperación post-error** | RESEARCH | PES/POST_ERROR_ACCURACY; dirección no consensuada | — | — | E12 (redundante) | Lente cs #4; literatura PES ambigua |

Resumen de membresía: **CORE** = vigilancia, inhibición, filtrado/interferencia. **SECONDARY** = flexibilidad, búsqueda. **CROSS_DOMAIN** = RSVP, MOT, dual, distracción digital. **EXCLUDE como constructo** = presión (pasa a eje). **RESEARCH** = recuperación post-error.

## 6.3 Reglas de la taxonomía

1. **Ningún ejercicio domina una subhabilidad** (invariante §42): cada CORE necesita ≥2 tareas de medida distintas (paradigma distinto, no skin) antes de emitir estimación calibrada. Hoy: 0 de 3 lo cumplen.
2. **Medida ≠ entrenamiento**: la tarea oficial de medida de una subhabilidad no puede ser la tarea entrenada con otras semillas (§4.2 obs. 2). Hoy: 3 de 3 lo violan.
3. **Indicadores oficiales = exactitud bajo deadline o tasas de señal** (d', lapsos sobre distribución completa); las diferencias de RT (CONGRUENCY_EFFECT, SWITCH_COST, SET_SIZE_SLOPE, DUAL_TASK_COST, PRESSURE_DELTA) son **descriptivas de entrenamiento**, nunca oficiales (Hedge 2018; Rouder & Haaf 2019).
4. **El compuesto «Enfoque» no se publica** mientras <2 CORE estén calibrados; y cuando se publique, declara sus pesos y su n por subhabilidad.
5. **CROSS_DOMAIN** se registra en el perfil bajo su dominio principal (memoria de trabajo, velocidad) con etiqueta de contribución secundaria a atención; nunca alimenta el compuesto de Enfoque.
6. **Presión/deadline** se modela como `DifficultyAxis.PRESSURE` sobre cualquier tarea CORE, no como ejercicio propio.

## 6.4 Mapa de cambios respecto a la taxonomía actual (`CognitiveTaxonomy.kt`)

| Actual (`Capacity`) | Propuesto | Acción |
|---|---|---|
| SUSTAINED_VIGILANCE | Vigilancia sostenida (CORE) | Mantener; corregir instrumento (E1 no es PVT) |
| SELECTIVE_FILTERING | Filtrado/interferencia (CORE) + Búsqueda (SECONDARY) + MOT (CROSS) | **Dividir**: hoy agrupa 3 mecanismos |
| RESPONSE_INHIBITION | Inhibición (CORE) | Mantener; SSRT → RESEARCH |
| INTERFERENCE_CONTROL | Fusionar con filtrado (CORE) | Stroop y Flanker comparten indicador oficial (exactitud bajo deadline); distinguir por `Paradigm`, no por `Capacity` |
| CONTROLLED_FLEXIBILITY | Flexibilidad (SECONDARY) | Mantener; sacar del módulo D_INTERFERENCE |
| TEMPORAL_ATTENTION | RSVP (CROSS) | E12 sale de aquí (es vigilancia) |
| DUAL_TASK_INTEGRATION | CROSS | Mantener como CROSS |
| DISTRACTION_RESISTANCE | CROSS | Mantener como CROSS; sin medida oficial |
| PRECISION_UNDER_PRESSURE | Eje de dificultad | **Eliminar como Capacity** |

`CognitiveModule` (6 módulos A–F) **no tiene lector** (`REPO_VERIFIED`, §4.1): se recomienda retirarlo o convertirlo en etiqueta de presentación; no aporta estructura de medición.

---

# PARTE VII — Exercise Inventory

`REPO_VERIFIED` para mecánica, generador, métricas y renderer (`ExerciseCatalog.kt`, `FocusExerciseGenerators.registry`, `E*Generator.kt`, `presentation/ui/components/*`). Los 13 están en `implemented` y montados vía `FocusHomeScreen` (menú por `implemented`), pero **ninguno es alcanzable como sesión de entrenamiento con los flags embarcados** (P0, PARTE I). Nivel de readiness según `EXERCISE_QUALITY_CONTRACT.md`: **L1** (jugable local) para todos; ninguno es L3/L4.

Leyenda de columnas: *Entrena* = lo que la mecánica ejercita de verdad; *Mide* = lo que el motor puede sostener como medida (no lo que declara); *Contaminación* = §4.3; *Prob. gaming* = techo de estrategia ciega o exploit conocido; *Veredicto* = clase de PARTE VI + acción.

## 7.1 Dossier por ejercicio

### E1 Vigía — declarado `PVT` / real: detección de señal rara con señuelos
- **Mecánica** (`E1VigiaGenerator.kt:30-75`): objetivos con `targetRarity = lerp(0.30, 0.06, perceptualLoad)`, señuelos `lureProbability = lerp(0, 0.35, salience)`, ISI 1200–7000 ms según `temporalUncertainty`, ventana 1400→950 ms, contraste 1.0→0.4. Single-tap.
- **Entrena**: mantenimiento de vigilancia ante evento raro + discriminación objetivo/señuelo. **No** entrena la respuesta rápida sostenida del PVT.
- **Mide (real)**: ACCURACY (dominada por correct-rejection, ~78 % no-objetivos), RT sobre 11–20 hits, lapsos sobre esos hits, FALSE_ALARMS a señuelos. Techo ciego «no responder» ≈ 0.79.
- **Contaminación**: alta (§4.3). **Gaming**: S15 «repetidor» a 190 ms pasa como honesto (AG=NONE) sólo si rinde por encima del techo ciego —D3 caza rápido-y-ciego (mediana < 250 y accuracy ≈ techo, `GamingDetectors.kt:105-108`)—; no responder sale bajo techo (`BlindStrategyCeiling` lo caza sólo si accuracy ≤ techo).
- **Adaptación**: `governingAxis = TIME_ON_TASK` sin lectores; `ContainImpulsivity` aplicable (PVT ∈ GO_NO_GO_PARADIGMS) sobre eje inerte.
- **Veredicto**: CORE (vigilancia) **con relabel obligatorio** (`Paradigm.PVT` → `RARE_SIGNAL_DETECTION` o rediseño a PVT real). Instrumento de medida actual: **no válido como PVT**.

### E2 Filtro — `FLANKER`
- **Mecánica**: elección izquierda/derecha; `incongruentProportion` por `interference` vía `conditionSchedule` (cuota, no Bernoulli); único con `FormDescriptorV1`.
- **Entrena**: supresión de flancos + mapeo de teclas. **Mide**: ACCURACY, RT; CONGRUENCY_EFFECT con celdas 11–13 a 55–60 ensayos (piso 20: no alcanzable, `MB-FOCUS-0092/0130`).
- **Contaminación**: alta (diferencia de RT). **Gaming**: baja (elección forzada); estrategia lenta-y-precisa infla exactitud sin deadline.
- **Adaptación**: `FIXED_DIFFICULTY_MODE` (Hold) — E2 no adapta nunca.
- **Veredicto**: CORE (filtrado) **como entrenamiento**; como medida exige rediseño a exactitud bajo deadline. Mejor base técnica del catálogo (contrato de forma, golden).

### E3 Gatillo — `GO_NO_GO`
- **Mecánica**: go frecuente / no-go raro; single-tap; `noGoProportion` por `inhibitoryDemand`.
- **Entrena**: retención de respuesta prepotente. **Mide**: ACCURACY (pooled), FALSE_ALARMS; sin d'/criterio; POST_ERROR_* con MIN_N 3.
- **Contaminación**: media. **Gaming**: techo ciego `max(p,1−p)`; sim E3_S2b «rápido sin machacar» pasa.
- **Adaptación**: `ContainImpulsivity` ratchet 0.08 sobre INHIBITORY_DEMAND (único eje vivo).
- **Veredicto**: CORE (inhibición). Añadir d'/criterio antes de cualquier medida oficial.

### E4 Freno — `STOP_SIGNAL`
- **Mecánica**: go en todos; stop con SSD online (`StopSignalTracker`, 1-up/1-down); `presentStopTrial :584-598` registra la SSD PEDIDA como usada (`awaitResponse` = `withTimeoutOrNull`, sin sincronía de frame; la efectiva queda sesgada al alza en expectativa).
- **Entrena**: cancelación de respuesta iniciada. **Mide**: SSRT sesgado (SSD inferior), ~20 stops en 60 ensayos frente a ≥50 (Verbruggen 2019).
- **Contaminación**: alta. **Gaming**: estrategia de esperar (slowing) infla p(stop); sin comprobación de independencia.
- **Veredicto**: CORE (inhibición) como entrenamiento; SSRT **RESEARCH**.

### E5 Conflicto — `STROOP`
- **Mecánica**: elección; conflicto semántico color/palabra; misma clave `CONGRUENCY_EFFECT` que E2.
- **Entrena/Mide**: como E2, con contaminación de lectura. **Veredicto**: CORE (filtrado/interferencia) entrenamiento; métrica compartida con E2 debe separarse (`CONGRUENCY_EFFECT_SEMANTIC`).

### E6 Cambio — `TASK_SWITCHING`
- **Mecánica**: regla color/forma; estímulo con `colorIsRed`/`shapeIsCircle`; `switchProbability` por `interference`; par de color con contraste de luminancia (R10).
- **Entrena**: reconfiguración de set; memoria de la regla activa. **Mide**: SWITCH_COST (diferencia de RT, celdas pequeñas).
- **Veredicto**: SECONDARY (flexibilidad). Mejor evidencia de entrenamiento del catálogo (Karbach & Kray).

### E7 Rastreo — `VISUAL_SEARCH`
- **Mecánica**: campo `FocusSearchField`; set size por `perceptualLoad`; single-tap sobre objetivo.
- **Mide**: SET_SIZE_SLOPE con tamaños fijos por dificultad (pendiente sobre 2–3 puntos). **Veredicto**: SECONDARY (búsqueda); fuera del compuesto.

### E8 Dúo — `DUAL_TASK`
- **Mecánica**: go/no-go + contador visual concurrente; flag `dual`.
- **Mide**: DUAL_TASK_COST = acc(single) − acc(dual) sobre la tarea primaria; las dos celdas existen en la misma sesión (`E8DuoGenerator.kt:37-45` sirve cuota dual/single, `Stimulus.kt:147` marca `dualTask`) y el valor es no nulo (`ParadigmMetricsTest.kt:59` = 0,4). **Corrección K8 (3/3)**: la afirmación «≡ 0» de la primera pasada era falsa; el defecto es más estrecho y sigue vivo —la demanda secundaria (contador) NUNCA se puntúa, así que quien la ignora obtiene el mejor valor 0,0 (`MB-FOCUS-0141`, pinneado por `FocusBlindStrategyTest.kt:214-262`). **Veredicto**: CROSS_DOMAIN; métrica gobernante que premia ignorar la segunda tarea.

### E9 Enjambre — `MOT`
- **Mecánica**: `FocusTrackingField` TRIAL_MS 1800, PROBE_START 0.82 ⇒ RT ≥ ~1476 ms ⇒ lapseRate 1.0, SFI de bloque ≡ 0 (en sesión: LAPSES ≈ aciertos); 1 bit por ensayo.
- **Mide**: ACCURACY con techo ciego alto (`MB-FOCUS-0139/0140`, guard invertido pinnea la limitación). **Veredicto**: CROSS_DOMAIN; excluir de medida.

### E10 Flujo — `RSVP`
- **Mecánica**: `FocusStreamField` FRAME_MS 80; RT anclado al inicio de la ráfaga; forma degenerada de una sola clase posible (DEGENERATE_CEILING idle).
- **Veredicto**: CROSS_DOMAIN; excluir de medida.

### E11 Ancla — `DIGITAL_DISTRACTION`
- **Mecánica**: `FocusAnchorField`; distractores tipo notificación con `distractorSalience`.
- **Mide**: DISTRACTION_RESISTANCE_INDEX = 1 − x (sin constructo). **Veredicto**: CROSS_DOMAIN; valor ecológico/motivacional, no de medida.

### E12 Re-enganche — `PVT` (+ recuperación)
- **Mecánica**: E1 con énfasis en POST_ERROR_*. **Redundante** con E1 (§4.3). **Veredicto**: fusionar con E1 o convertir en variante de presentación (CEO-OVERRIDE-003 lo permite); RESEARCH para recuperación.

### E13 Presión — `DEADLINE_PRESSURE`
- **Mecánica**: deadline variable por `pressure`; PRESSURE_DELTA sólo a nivel de bloque.
- **Veredicto**: eliminar como ejercicio; convertir en eje `PRESSURE` aplicable a E2/E3/E5.

## 7.2 Tabla resumen

| Ej. | Paradigma (código) | Entrena | Mide (real) | Cont. | Gaming | Adapt. viva | Clase | Acción |
|---|---|---|---|---|---|---|---|---|
| E1 | PVT (mal etiquetado) | Vigilancia a señal rara | ACC (CR-dominada), RT/lapsos sobre 11–20 hits | Alta | Repetidor pasa; no-responder ≈ techo | Contain (eje inerte) | CORE | Relabel / rediseñar a PVT real |
| E2 | FLANKER | Filtrado | ACC, RT; CE no alcanzable | Alta | Baja | Ninguna (FIXED) | CORE | Medida por exactitud+deadline |
| E3 | GO_NO_GO | Inhibición | ACC pooled, FA | Media | Techo max(p,1−p) | Contain (INHIB) | CORE | Añadir d'/criterio |
| E4 | STOP_SIGNAL | Cancelación | SSRT sesgado | Alta | Slowing | Contain (INHIB) | CORE/RESEARCH | SSD real, ≥50 stops |
| E5 | STROOP | Interferencia | como E2 | Alta | Baja | Ninguna | CORE | Separar clave CE |
| E6 | TASK_SWITCHING | Flexibilidad | SWITCH_COST | Alta | Baja | Ninguna | SECONDARY | Exactitud+deadline |
| E7 | VISUAL_SEARCH | Búsqueda | SET_SIZE_SLOPE | Media | Baja | Ninguna | SECONDARY | Descriptivo |
| E8 | DUAL_TASK | Priorización | DTC ≡ 0 | Media | Media | Ninguna | CROSS | Métrica muerta |
| E9 | MOT | Seguimiento | ACC techo alto; SFI ≡ 0 | Alta | Alta | Ninguna | CROSS | Excluir medida |
| E10 | RSVP | Detección temporal | ACC; RT inválido | Alta | Media | Ninguna | CROSS | Excluir medida |
| E11 | DIGITAL_DISTRACTION | Ignorar salientes | DRI = 1−x | Media | Media | Ninguna | CROSS | Sin medida |
| E12 | PVT+recuperación | = E1 | POST_ERROR_* (MIN_N 3) | Alta | = E1 | Contain | RESEARCH | Fusionar con E1 |
| E13 | DEADLINE_PRESSURE | SAT inducido | PRESSURE_DELTA bloque | Media | Media | Ninguna | EXCLUDE | Convertir en eje |

## 7.3 Lo que ningún ejercicio tiene hoy (`REPO_VERIFIED`)

- Eventos de analítica (0 eventos; sólo `FocusScoreContainmentBreadcrumb`).
- Persistencia de identidad de forma por fila (`probeId`, `formSeed`, `rngVersion`) salvo el contador del cursor.
- Timebase/plataforma/`deviceFingerprint` por fila.
- Nivel L3/L4 del contrato de calidad.

---

# PARTE VIII — Scientific Family Map

`SCIENTIFICALLY_SUPPORTED` para la familia; `REPO_VERIFIED` para la asignación de ejercicios. Una *familia* es un linaje experimental con una demanda cognitiva y un conjunto de indicadores canónicos; un *skin* es la misma familia con otra estética. El catálogo declara 14 `Paradigm`; 12 tienen ejercicio; `SIMON` y `MACKWORTH_CLOCK` fallan cerrados (`TEST_VERIFIED`, R11).

| Familia | Demanda cognitiva | Indicadores canónicos | Ejercicios Molvex | Skins (mismo mecanismo) | Estado de medida |
|---|---|---|---|---|---|
| **Vigilancia psicomotora (PVT)** | Mantener respuesta rápida a estímulo frecuente con foreperiodo aleatorio | Lapsos >500 ms sobre TODOS los ensayos; 1/RT media; cola de RT (τ) | Ninguno real (E1/E12 lo declaran) | — | No implementada |
| **Detección de señal rara (Mackworth/CPT-X)** | Sostener discriminación ante evento infrecuente y señuelos | d', criterio, decremento temporal en ≥20 min | E1, E12 | E12 = skin de E1 | Implementada como E1; mal etiquetada |
| **Go/No-Go** | Retener respuesta prepotente | d', criterio, FA rate, PES | E3, E8 (dual) | — | Sin d'/criterio |
| **Stop-signal** | Cancelar respuesta iniciada | SSRT (integración), p(respond signal) 0.25–0.75, independencia | E4 | — | RESEARCH |
| **Flanker (Eriksen)** | Suprimir información espacialmente adyacente | Efecto de congruencia RT/exactitud; exactitud bajo deadline | E2 | — | CE no alcanzable |
| **Stroop / conflicto semántico** | Suprimir respuesta automática (lectura) | Efecto Stroop; exactitud bajo deadline | E5 | Comparte clave con E2 | Idem |
| **Task-switching** | Reconfigurar set de reglas | Switch cost; mixing cost; exactitud en switch | E6 | — | Descriptivo |
| **Búsqueda visual** | Selección espacial serial/paralela | Pendiente por set size; exactitud | E7 | — | Descriptivo |
| **MOT** | Seguimiento espacial múltiple | Capacidad (n objetos), exactitud de sonda | E9 | — | Techo ciego |
| **RSVP / blink atencional** | Selección temporal | T2 dado T1 por lag; aquí sólo detección | E10 | — | Detección ≠ blink |
| **Dual-task** | Coordinación / priorización | Coste dual vs single | E8 | — | Sin single de referencia |
| **Distracción ecológica** | Ignorar distractor saliente no relacionado | Sin canon | E11 | — | Sin constructo |
| **Deadline / SAT** | Eje de dificultad, no familia | Curva SAT | E13 | Aplica a cualquiera | Convertir en eje |

Familias **ausentes** relevantes para CORE: PVT real; antisacada (attention control, Draheim); CPT con d'/criterio explícito. Familias **sobre-representadas**: vigilancia con señal rara (E1 + E12).

---

# PARTE IX — Coverage & Redundancy Matrix

## 9.1 Cobertura subhabilidad × función

Función: **T** = entrena, **M** = puede medir oficialmente hoy, **m** = podría medir con cambios menores (mismo ejercicio), **—** = no.

| Subhabilidad (PARTE VI) | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 | E11 | E12 | E13 | Nº tareas T | Nº tareas M | Cumple «≥2 tareas de medida» |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Vigilancia sostenida (CORE) | T/m | — | — | — | — | — | — | — | — | — | — | T/m (=E1) | — | 2 (1 real) | 0 | **No** |
| Inhibición (CORE) | — | — | T/m | T | — | — | — | T | — | — | — | — | — | 3 | 0 | **No** |
| Filtrado/interferencia (CORE) | — | T/m | — | — | T/m | — | T | — | — | — | T | — | T | 5 | 0 | **No** |
| Flexibilidad (SEC) | — | — | — | — | — | T | — | — | — | — | — | — | — | 1 | 0 | No |
| Búsqueda (SEC) | — | — | — | — | — | — | T | — | — | — | — | — | — | 1 | 0 | No |
| RSVP (CROSS) | — | — | — | — | — | — | — | — | — | T | — | — | — | 1 | — | n/a |
| MOT (CROSS) | — | — | — | — | — | — | — | — | T | — | — | — | — | 1 | — | n/a |
| Dual (CROSS) | — | — | — | — | — | — | — | T | — | — | — | — | — | 1 | — | n/a |
| Distracción (CROSS) | — | — | — | — | — | — | — | — | — | — | T | — | — | 1 | — | n/a |

**Lectura**: la columna M es cero en todas las filas porque (a) la única sonda que corre (VIGILANCE_PROBE) es E1 con semillas reservadas (misma tarea que el entrenamiento), (b) el RCI está estructuralmente en `NotEnoughData` (`ReliabilitySource.EMPTY`), y (c) sólo ACCURACY cruza el piso de 20/celda a 55 ensayos. «m» marca los ejercicios cuyo instrumento podría volverse oficial con cambios de dosis/métrica (PARTE XXVII).

## 9.2 Redundancia (mismo mecanismo)

| Par | Tipo | Evidencia | Recomendación |
|---|---|---|---|
| E1 ↔ E12 | Redundancia total (mismo generador de vigilancia; E12 añade recuperación post-lapso) | `ExerciseCatalog` paradigma PVT ambos; `POST_ERROR_*` con MIN_N 3 | Fusionar: E12 = variante de feedback de E1 (CEO-OVERRIDE-003) |
| E2 ↔ E5 | Redundancia parcial (misma clave `CONGRUENCY_EFFECT`, mismo eje `INTERFERENCE`, mismo renderer de elección) | `DefaultMetricsEngine` produce una clave para ambos | Mantener ambos (mecanismos distintos) pero separar claves y contar como 2 tareas de filtrado |
| E3 ↔ E8 | E8 = E3 + contador | flag `dual` | E8 no cuenta como segunda tarea de inhibición |
| E1 ↔ E3 | Comparten single-tap + umbrales de lapso pooled en calibración | `BaselineAssembler` pool 195 | Separar umbrales por paradigma |
| E13 ↔ cualquiera | E13 es un eje | `pressure` | Eliminar como ejercicio |

**Variedad procedimental real** (mecanismos distintos, no skins): 9 de 13 (E1, E2, E3, E4, E5, E6, E7, E9, E10, E11 menos redundancias E12/E8/E13 ⇒ 10 mecanismos, 3 redundantes).

## 9.3 Huecos

| Hueco | Por qué importa | Riesgo | Cómo resolver | Evidencia necesaria | Orden |
|---|---|---|---|---|---|
| Sin PVT real | La subhabilidad CORE «vigilancia» se mide con un instrumento de señal rara cuyas métricas canónicas no aplican | Falso progreso por sesgo de respuesta | Nuevo instrumento (100 % objetivos, foreperiodo 2–10 s, ≥3 min) o relabel de E1 | Test-retest ICC ≥0.7 en 2 administraciones | P1 |
| Sin segunda tarea de medida por CORE | Invariante «un ejercicio no domina» | Impureza de tarea | 1 instrumento adicional por CORE (antisacada-like para filtrado; CPT con d' para inhibición) | ≥2 instrumentos por CORE con correlación >0.4 | P1 |
| Sin d'/criterio | E3 no separa sensibilidad de sesgo | «Inhibe mejor» = «responde menos» | Productor d'/criterio con Hautus + n por celda | Sim + test | P1 |
| Sin exactitud bajo deadline | Filtrado sólo por RT | Diferencias de RT no fiables | Deadline adaptativo (Draheim) en E2/E5/E6 | Reliability paradox mitigado | P1 |
| Sin medida de fatiga/decremento válida | 2–3 min no permiten decremento | Ruido como señal | Retirar `FATIGUE_INDEX`/`VIGILANCE_DECREMENT_SLOPE` de superficies o exigir ≥10 min | Literatura (Mackworth) | P2 |

---

# PARTE X — Measurement Pipeline Audit (Trial → score)

`REPO_VERIFIED` salvo etiqueta. Cadena evaluada con el estándar «existe → conectado → se ejecuta → testeado → outputs correctos → interpretación válida».

## 10.1 Etapas

| # | Etapa | Símbolo | Existe | Conectado | Se ejecuta (flags embarcados) | Testeado | Output correcto | Interpretación válida |
|---|---|---|---|---|---|---|---|---|
| 1 | Generación de forma | `E*Generator.generate(difficulty, spec, seed)`; `DeterministicRng` splitmix64 + `RngVersion` | Sí | Sí | Sí (base y sesión) | Golden (E2), determinismo (13) | Sí | Sí (sólo E2 con contrato de forma) |
| 2 | Presentación + onset | `FocusSessionStateHolder.presentAndAwait :266` (onset ANTES de `emitRunning`) | Sí | Sí | Sí | Parcial (virtual clock) | **Onset ≠ render**: clase D | No como RT-crítico |
| 3 | Captura de respuesta | `detectTapGestures { onTap() }` = pointer UP; `MonotonicClock` | Sí | Sí | Sí | No (UI) | Latencia = UP − onset (incluye hold del dedo) | Sesgo sistemático +30–80 ms |
| 4 | Clasificación | `TrialClassifier.classify/classifyChoice/classifyStop`; suelo 150 ms | Sí | Sí | Sí | Sí | Sí | Sí |
| 5 | Ensamblado de bloque | `FocusSessionAssembler.assembleBlock`; `TrialResult.timestampMs = endedAtMs` para todos (`:108/146`) | Sí | Sí | Sí | Sí | Timestamps por ensayo **fabricados** (iguales) | Cualquier análisis temporal intra-bloque es inválido |
| 6 | Métricas de bloque | `DefaultMetricsEngine.blockMetrics :70-114` (15 base incondicionales) + `addParadigmMetrics :312-327` (gated por `primaryMetrics`) | Sí | Sí | Sí | Sí (goldens, honestidad) | Sí para lo que calcula | **No** para métricas base en paradigmas donde no aplican (§11) |
| 7 | Métricas de sesión | `sessionMetrics :123-152` → sólo ACCURACY, RT_MEDIAN, RT_CV, LAPSES, FALSE_ALARMS, FATIGUE_INDEX; `stampMeasurementSufficiency(samples = emptyMap())` | Sí | Sí | Sí | Sí | **SFI/BLOCK_STABILITY/paradigma nunca llegan a sesión** | Consumidores (chip FOCO, FocusScore, tendencias SFI) estructuralmente vacíos |
| 8 | Anti-gaming | `FocusAntiGaming.telemetryFrom` → D3, D4, `BlindStrategyCeiling`; COVERAGE PARTIAL; MIN_TRIALS 12 | Sí | Sí (único call-site `admit`) | Sí | Red-team | Sí | Parcial (no cubre repetidor lento-consistente ni rítmico) |
| 9 | Admisión | `SessionQualityGate.admit :55-64` INTERRUPTED > NO_RESPONSES > RESPONSE_PATTERN > LIFECYCLE_NOT_OBSERVED > Admitted | Sí | Sí | Sí | Sí | Sí | Sí; **con flag OFF siempre LIFECYCLE_NOT_OBSERVED** |
| 10 | Retirada de métricas | `withdrawn() :247-250` (value 0.0, confident false) antes de persistir | Sí | Sí | Sí | Sí | Sí en memoria | **Persistencia pierde la ausencia** (`MetricValueDto` sin absence) ⇒ re-lectura devuelve 0.0 |
| 11 | Persistencia | `SessionSummaryDto` (sin trials, `deviceFingerprint` null; lleva `trialCount` real por bloque). Los literales `plannedTrials=1`/`completedTrials=1` van al `SessionRecord` de `core/session` (`FocusSessionStateHolder.kt:340/:476`), que ningún consumidor de Focus lee | Sí | Sí | Sí | Sí | Campos placeholder | No auditable por fila |
| 12 | Calibración | `DefaultCalibrationEngine` (lapse = max(500, med + 2.5·sMAD), MIN_SAMPLES 8, pooled 195) | Sí | Sí | Sí (aunque la sonda se deniegue) | Sí | Sí matemáticamente | **No**: pool entre paradigmas; unidireccional (nunca < 500) |
| 13 | Snapshot | `BaselineAssembler.assemble` → `perMetric: Map<MetricKey, Double>` (sin n/SD) | Sí | Sí | Sólo si Admitted (nunca con flags OFF) | Sí | Sí | Sin SE ⇒ RCI sin SEdiff propio |
| 14 | Adaptación | `DefaultAdaptiveEngine.decideAdaptation` | Sí | Sí | Sólo con flag ON | Sí | 4 decisiones alcanzables | Difficulty ≠ ability (no lee `baselineRtCv`) |
| 15 | Score personalizado | `PersonalizedSessionScorer` (W 0.5/0.2/0.3) | Sí | Sí | Flag OFF | Sí | Sí | Blends SAT; no es estimación |
| 16 | FocusScore (UI) | `FocusScore.collectComponents :38-62` MIN_COMPONENTS ≥2 | Sí | Sí | Flag OFF; con ON `confident=false` **por construcción** (1 componente) | Fixtures hand-built | No producible | n/a |
| 17 | Evaluación (RCI) | `FocusEvaluationService.evaluate` → `ProbeReliabilityEstimator` (MIN_ADMINISTRATIONS 3, `formIsFresh`) | Sí | Sí | Sí | Sí | `NotEnoughData` siempre (`ReliabilitySource.EMPTY` `FocusGraph.kt:156`) | Correcto (fail-closed) |
| 18 | Perfil cerebral | `FocusProfileSource` → `MindBoostProfileAggregator` | Sí | Sí | Sí | Sí | CALIBRATING | Correcto (fail-closed) |

## 10.2 Roturas de cadena (ordenadas por consecuencia)

1. **Sesión inalcanzable** (etapas 9→13 con flags embarcados): ningún dato de entrenamiento llega jamás a 11–18. P0.
2. **`sessionMetrics` whitelist** (7): tres superficies (chip FOCO, tendencia SFI, FocusScore) dependen de claves que nunca se emiten a nivel sesión. Los tests que las cubren construyen a mano un `sessionMetrics` que el motor no produce. P1 con flag ON.
3. **Ausencia no persistida** (10→11): `withdrawn()` (`FocusSessionAssembler.kt:248-250`) re-etiqueta y `MetricValue.unavailable` (`MetricKey.kt:88`) escribe 0.0 —para toda ausencia, no sólo la retirada—; `MetricValueDto` no lleva `absence` ni `measurementSufficient`; toda re-lectura de una sesión NotMeasurable ve ceros con `confident=false` y la admisión persistida. Ningún consumidor de focus v1 llama a `rawValueOf` (sólo su definición `MetricKey.kt:131-132`): hoy es pérdida de la razón tipada, no fabricación viva; deuda declarada (aprendizaje 2026-07-15). P2 (K6 3/3).
4. **Timestamps por ensayo fabricados** (5): todo análisis intra-bloque (decremento, PES temporal, fatiga) parte de tiempos idénticos. `VIGILANCE_DECREMENT_SLOPE` se calcula por índice de ensayo, no por tiempo — sobrevive, pero cualquier futuro que use `timestampMs` fabrica. P2.
5. **Calibración pooled y ungated** (12): umbrales de E1 contaminados por E2/E3; sandbag posible sin gate. P1.
6. **Clase D de RT** (2–3): ninguna métrica de RT es oficial. P1 (bloquea medida, no entrenamiento).

## 10.3 Dos carriles con veredictos distintos (`REPO_VERIFIED`)

`FocusProgressStateHolder.kt:62-71` (el `null` en `:70`) compara `retest.sessionMetrics` con `repeatsEarlierForm = null` (retiene por desconocido); `FocusEvaluationStateHolder.kt:235-241` compara `blocks.single().metrics` con el hecho real. Para el mismo retest, la pantalla de progreso y la de evaluación pueden disentir en `withheldForSample` y en la cláusula de forma repetida. Hoy ambos acaban en `NotEnoughData`, así que el disenso es latente; queda **armado** para el día que se pueble `ReliabilitySource`.

---

# PARTE XI — Metric-by-Metric Audit

Claves: 26 `MetricKey`. Columnas: *Productor* (`REPO_VERIFIED`), *Nivel* (B = bloque, S = sesión), *n mínimo* (en sesión / central `MinSampleGate`), *Validez por paradigma* (V válida, C contaminada, X sin sentido, 0 no aplica; de la lente cognitive-science, re-verificada), *Consumidor vivo*, *Veredicto*.

| Métrica | Productor | Nivel | n mín (sesión / central) | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 | E11 | E12 | E13 | Consumidor vivo | Veredicto |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ACCURACY | base, todos | B+S | 5 / 20 | C (CR-dominada) | V | C (pooled go+nogo) | C | V | V | V | C | C (techo) | C | C | C | V | Home, Progress, Adaptive, PS, FocusScore, Calibration ladder | Única que cruza pisos; semántica distinta por paradigma |
| RT_MEDIAN | base | B+S | 5 / 20 | V (clase D) | V | V | V | V | V | V | V | X (≥1476) | X (ráfaga) | V | V | V | Trends, PS, Calibration | Clase D ⇒ no oficial |
| RT_CV | base | B+S | 5 / 20 | V | C | C | C | C | C | C | C | X | X | C | V | C | Adaptive (perf), PS, Trends «Estabilidad» | Válida sólo E1/E12 |
| LAPSES | base | B+S | 5 / 20 (celda lapso) | V (sobre 11–20 hits) | X | C | C | X | X | X | C | X (todos) | X | C | V | X | PS (PER_LAPSE_COST), Report | Umbral 500 unidireccional; pool de calibración |
| FALSE_ALARMS | base (count) | B+S | — | V (señuelos) | 0 | V | V | 0 | 0 | 0 | V | 0 | 0 | V | V | 0 | Adaptive Contain, Report | Sin d'/criterio |
| FATIGUE_INDEX | base | B+S | 6 | X (2–3 min) | X | X | X | X | X | X | X | X | X | X | X | X | Adaptive (null con flags) | Ruido estructural a esta dosis |
| ANTICIPATIONS | base | B | — | V | V | V | V | V | V | V | V | 0 | 0 | V | V | V | AntiGaming D4 | OK |
| OMISSIONS | base | B | — | V | V | V | V | V | V | V | V | V | V | V | V | V | — | Sin consumidor |
| SUSTAINED_FOCUS_INDEX | base (`addFocusIndices`) | **B sólo** | 5 | V | X | C | C | X | X | X | C | X (≡0) | X | C | V | X | Home chip (lee S ⇒ nunca), Trends SFI (nunca), FocusScore (nunca) | Nunca llega a sesión |
| BLOCK_STABILITY | base | **B sólo** | 5 | V | C | C | C | C | C | C | C | X | X | C | V | C | FocusScore (nunca) | Idem |
| VIGILANCE_DECREMENT_SLOPE | base | B | 6 | X (duración) | X | X | X | X | X | X | X | X | X | X | X | X | — | Retirar de superficies |
| IES / RCS / LISAS | base | B | 5 | X (señal rara) | V | C | C | V | V | V | C | X | X | C | X | V | TransferReport labels | Sin interpretación en E1 |
| POST_ERROR_SLOWING | paradigm | B | 3 | C | C | V | V | C | C | C | V | X | X | C | V | C | Trends (LOWER-is-better, no justificado) | Dirección no consensuada |
| POST_ERROR_ACCURACY | paradigm | B | 3 | C | C | V | V | C | C | C | V | X | X | C | V | C | — | MIN_N 3 |
| CONGRUENCY_EFFECT | paradigm | B | 3 (/20 central) | 0 | V (celda 11–13) | 0 | 0 | V | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Report | Una clave para 2 mecanismos; no alcanzable |
| SWITCH_COST | paradigm | B | 3 | 0 | 0 | 0 | 0 | 0 | V | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Report | Descriptivo |
| SSRT | paradigm (`SsrtCalculator`) | B | 30 central | 0 | 0 | 0 | C (SSD inferior) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Report | RESEARCH |
| SET_SIZE_SLOPE | paradigm | B | — | 0 | 0 | 0 | 0 | 0 | 0 | V | 0 | 0 | 0 | 0 | 0 | 0 | Report | Descriptivo |
| DUAL_TASK_COST | paradigm | B | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | X (secundaria no puntuada) | 0 | 0 | 0 | 0 | 0 | Report | acc(single) − acc(dual) real (no ≡ 0, K8); ignorar el contador da 0,0 = mejor valor (`MB-FOCUS-0141`) |
| DISTRACTION_RESISTANCE_INDEX | paradigm | B | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | C | 0 | 0 | Report | 1−x sin constructo |
| PRESSURE_DELTA | paradigm | B | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | C | Report | Sólo bloque |
| INHIBITORY_CONTROL_INDEX | **ninguno** | — | — | — | — | — | — | — | — | — | — | — | — | — | — | — | Labels en Trends:21, TransferReport:102, FocusScore:58 | Orphan |
| NEAR_TRANSFER_INDEX | **ninguno** | — | — | — | — | — | — | — | — | — | — | — | — | — | — | — | — | Orphan |
| RCI (score metric) | `ReliableChangeIndex` | Admin. | 2 (`MinSampleGate.RCI`, sin consumidor) / 3 (`MIN_ADMINISTRATIONS`) | — | — | — | — | — | — | — | — | — | — | — | — | — | Evaluation | Piso muerto vs piso vivo |

Nota sobre pisos: MIN_N_ACCURACY 5, MIN_N_RT 5, MIN_N_POST_ERROR 3, MIN_N_SLOPE 6, MIN_N_CONGRUENCY 3 (en sesión, para `confident`); central `MinSampleGate` DPRIME 20 / ACCURACY_RT_BASE 20 / SSRT 30 / BRIER 40 / RCI 2 (para `measurementSufficient`). `MetricValue.unavailable` sigue codificando 0.0 (`MB-FOCUS` follow-up declarado 2026-07-15).

**Contradicción documental** (refutación K3: sobrevive 3/3, 85–95 %, con precisión de autoridad): `ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md` §4 (:67-77) especifica para **E2** una allow-list («E2 must neither produce nor consume … `LAPSES`, `RT_CV`, `BLOCK_STABILITY`, `FATIGUE_INDEX`, `IES`, `LISAS`, `SUSTAINED_FOCUS_INDEX`»); «per-family» sólo aparece en la celda de resolución de T7 (:45) y no existe lista para MOT/RSVP. El Blueprint es `Status: CANDIDATE` con `DESIGN_REVIEW_GATE = PENDING` y dos `NO APTO` (`MB-ENG-0001`/`0004`), ausente del `CURRENT_AUTHORITY_INDEX`: la redacción honesta es «el código incumple el criterio de salida del blueprint candidato», no «contradice un mandato». Lo que SÍ es `REPO_VERIFIED` sin matiz: las ramas prohibidas están vivas — `RT_CV` dirige `StepDown` para E2 (`AdaptiveEngine.kt:92,:112`) y el `MetricSet` de bloque completo se persiste (`FocusPersistenceMappers.kt:129`). `CONTRADICTED`, registrar.

---

# PARTE XII — RT & Mobile Timing Audit

## 12.1 Clases de validez de RT (definición adoptada)

| Clase | Definición | Uso permitido |
|---|---|---|
| A | Onset = vsync del frame que pinta el estímulo (Choreographer/CADisplayLink) + input con timestamp nativo en ns + cadencia verificada + frame stats limpios | Oficial, RT-crítico |
| B | Onset en frame callback + input nativo, sin verificación de cadencia | Oficial con corrección declarada |
| C | Onset en frame callback o input nativo (uno de los dos) | Entrenamiento; oficial sólo para métricas de exactitud |
| D | Onset en código de aplicación antes de componer; input en pointer UP de Compose; reloj monotónico | Entrenamiento; **nunca** oficial para RT |
| E | Reloj de pared o sin monotónico | Nada |

## 12.2 Estado por plataforma (`REPO_VERIFIED`)

| Plataforma | Onset | Input | Reloj | Clase | Timebase persistido |
|---|---|---|---|---|---|
| Android (Focus v1) | `presentAndAwait :266` antes de `emitRunning` (pre-composición) | `detectTapGestures onTap` (UP) | `MonotonicClock` (`TimeSource.Monotonic`) | **D** | No (`RecordedTrial` sin timebase) |
| Android (Speed, API ≥34) | Choreographer frame | `MotionEvent` nanos | nativo | C (según `TimebaseClassifier`) | Sí (`SessionMetrics.timestampSource`) |
| Desktop | igual que Android v1 | Compose UP | monotónico | D | No |
| iOS | igual | Compose UP | monotónico | D | No |
| focus2 (V5, no montado) | `WindowedTemporalProbe` con cadencia declarada/detectada | — | — | Diseñado para A/B; INERT | Sí (diseño) |

**Sesgo esperado clase D** (refutación K5: sobrevive, 95 %; el sello de onset `:266` precede incluso a la asignación de `uiState`, y `detectTapGestures` (UP) gobierna los TRES carriles: `FocusSessionScreen.kt:242,247` y `FocusMeasureRunningContent.kt:116,130`): onset adelantado (código → frame ≈ 8–33 ms a 60/120 Hz) y respuesta retrasada (DOWN → UP ≈ 30–80 ms) ⇒ RT medido = RT real + (30…110) ms con varianza añadida. Nicosia 2022 / Passell 2021: sesgo de dispositivo 20–60 ms. Sin `deviceFingerprint` por fila, un cambio de teléfono se lee como cambio de capacidad (S9 sim: PS 89.8 → 72.3 sin cambio de habilidad).

## 12.3 Casos paradigma-específicos

| Ejercicio | Problema | Consecuencia | Clase efectiva |
|---|---|---|---|
| E9 MOT | `TRIAL_MS 1800`, `PROBE_START 0.82f` ⇒ la sonda aparece a ~1476 ms del onset registrado (`FocusSessionStateHolder.kt:266-270` sella el onset ANTES de `presentAndAwait`) | Para todo usuario que espere a la sonda (no adivine): RT ≥ 1476 ⇒ `lapseRate = 1.0` y `SFI = 0.0` real (no `unavailable`) siempre que `lapseRtMs < ~1476`, cierto para el default 500 y para toda calibración realista (`max(500, mediana+2.5·sMAD)` sobre E1/E2/E3 con ventanas ≤1500 ms; no acotado por constante); además la ventana cierra a 2000 ms ⇒ <524 ms útiles ⇒ muchas respuestas legítimas son MISS. Alcance preciso (K2, 3/3, 85–90 %): `SUSTAINED_FOCUS_INDEX` es SÓLO de bloque — `sessionMetrics` (`:123-152`) nunca lo re-deriva, así que `FocusScore` y `Trends` no ven el 0 de E9; la fuga a sesión es `LAPSES`, que suma los lapsos de bloque (`:135-142`), se renderiza como «Lapsos» (`FocusSessionAssembler.kt:187,202`) y la leen `PersonalizedSessionScorer.kt:50` y `ArchetypeInference.kt:116` ⇒ «lapsos ≈ nº de aciertos a nivel sesión; SFI de bloque ≈ 0». El fixture de E9 del ensamblador fija 500 ms (`FocusAdvancedParadigmsAssemblerTest.kt:94`), nunca ejercita la latencia real | X (RT sin sentido) |
| E10 RSVP | `FRAME_MS 80`; onset al inicio de la ráfaga, no del target | RT = posición del target × 80 + RT real | X |
| E4 stop | `presentStopTrial :584-598`: SSD pedida registrada como usada; GO y STOP son emisiones de estado que se dibujan un frame después | SSD efectiva = pedida + overshoot del timer + render STOP − render GO (sesgo al alza en expectativa, no cota estricta) ⇒ SSRT = go − SSD **sesgado** | C→D |
| E1/E3 | Ventana 1400→950 ms; respondedor honesto lento (~1 s) ⇒ RESPONSE_PATTERN | Denegación de honestos (`MB-FOCUS-0146`) | D |

## 12.4 Lo que existe pero no está conectado

- `core/timing/{FrameStatsCalculator, FrameTimingProbe, TemporalClassPolicy, WindowedTemporalProbe}` (fail-closed, cadencia declarada vs detectada) — usados por Speed y focus2; **cero call-sites en focus v1**.
- `TimebaseClassifier` (Speed) — no aplica a focus.

## 12.5 Requisitos mínimos para RT oficial (`RECOMMENDATION`)

1. Onset desde frame callback (Choreographer/CADisplayLink) y respuesta en DOWN con timestamp nativo.
2. `timebaseSource`, `platform`, `deviceClass`, `refreshHz` por fila persistida.
3. Frame stats por bloque con `sampledFrames` obligatorio (patrón Speed 2026-08-29).
4. Clase declarada por sesión; sólo A/B alimentan estimadores de RT; C/D alimentan exactitud.
5. Validación física (protocolo `S5_PASO6_PHYSICAL_PROTOCOL.md`): dispersión de D acotada, no su media.

---

# PARTE XIII — Difficulty Model Audit

`REPO_VERIFIED` (`DifficultyVector`, `DifficultyAxis` 9 ejes, `ExerciseCatalog.governingAxis`, `E*Generator` lerps, `DefaultCalibrationEngine.initialDifficulty`, `SessionTemplate`).

## 13.1 Modelo

- `DifficultyVector` = 9 ejes en [0,1]: PERCEPTUAL_LOAD, TEMPORAL_UNCERTAINTY, INTERFERENCE, INHIBITORY_DEMAND, DUAL_TASK_LOAD, DISTRACTOR_SALIENCE, PRESSURE, TIME_ON_TASK, (+1). `EASIEST` = todos 0. Cada generador lee 1–3 ejes con `lerp` fijos (E1: PL → rareza/ventana/contraste; TU → ISI; salience → señuelos).
- **Dificultad fija dentro de la sesión**: el vector se decide al inicio (`profile.startingDifficulty`) y no cambia entre bloques.
- **Dificultad ≠ capacidad**: no existe mapeo dificultad→habilidad (sin IRT, sin staircase de convergencia, sin curva psicométrica). Un usuario en PL 0.5 no es «mejor» que uno en 0.35: es el que la escalera de exactitud puso ahí.

## 13.2 Escalera inicial (`initialDifficulty`)

| Exactitud del bloque de calibración | PERCEPTUAL_LOAD |
|---|---|
| ≥ .90 | 0.50 |
| ≥ .80 | 0.35 |
| ≥ .70 | 0.20 |
| < .70 o `valid == 0` | 0 (LEVEL_MIN) |

Defectos (`REPO_VERIFIED`): (a) **inflada en E1** porque la exactitud es ~78 % correct-rejection (un usuario que no responde saca ≈.79 ⇒ 0.20); (b) **inerte en E3** (su generador no lee PL); (c) **resetea TEMPORAL_UNCERTAINTY a 0** para E1 (la calibración corrió a 0.35) ⇒ la primera sesión es más fácil que la calibración; (d) sólo E1/E2/E3; los otros 10 en `EASIEST`.

## 13.3 Rango adaptativo real

| Ejercicio | Eje gobernante | Lectores del eje en el generador | Rango efectivo |
|---|---|---|---|
| E1 | TIME_ON_TASK | **ninguno** | 0 (eje inerte) |
| E2 | INTERFERENCE | `incongruentProportion` | FIXED_DIFFICULTY_MODE ⇒ nunca cambia |
| E3/E4 | INHIBITORY_DEMAND | `noGoProportion` / stop rate | 0.08 por ContainImpulsivity, ratchet |
| E5/E6 | INTERFERENCE | sí | sin decisión que lo mueva (StepUp inalcanzable) |
| E7/E9 | PERCEPTUAL_LOAD | sí | idem |
| E8 | DUAL_TASK_LOAD | sí | idem |
| E10 | TEMPORAL_UNCERTAINTY | sí | idem |
| E11 | DISTRACTOR_SALIENCE | sí | idem |
| E12/E13 | PRESSURE | sí | idem |

**Desirable difficulty**: no existe ningún objetivo de tasa de acierto (p. ej. 80–85 %) ni staircase de convergencia; `AdaptiveConfig` tiene umbrales (accUp 0.85 / hold 0.72 / down 0.65) pero las decisiones StepUp/StepDown no son alcanzables (PARTE XIV).

## 13.4 Difficulty vs Ability (invariante §42)

Estado: **separados por omisión, no por diseño**. Nada convierte dificultad en capacidad (bien), pero tampoco nada estima capacidad (mal): el «nivel» que ve el usuario (si se mostrara) sería la dificultad. Recomendación: modelo de dificultad como **parámetro de ítem** (b) y capacidad como **parámetro de persona** (θ) estimados por separado (1PL/2PL sobre exactitud con deadline), con la escalera adaptativa como política de selección, no como estimador.

---

# PARTE XIV — Adaptive Engine Audit

`REPO_VERIFIED` (`DefaultAdaptiveEngine.decideAdaptation`, `AdaptiveConfig`, `AdaptiveModels`, `FocusAdaptiveRulePolicy`, `AdaptiveModeGate`).

## 14.1 Espacio de decisiones

| Decisión | Alcanzable con flags embarcados | Alcanzable con lifecycle ON | Condición | Efecto |
|---|---|---|---|---|
| Hold(SESSION_NOT_MEASURABLE) | Sí (única) | Sí | admisión ≠ Admitted | ninguno |
| Hold(FIXED_DIFFICULTY_MODE) | — | Sí (E2) | `exerciseId == E2` | ninguno |
| Hold(METRIC_UNAVAILABLE) | — | Sí (default) | `fatigue`/`perAxisMastery`/`blocksSinceChange` null (los call-sites de producción no los pueblan) | ninguno |
| ContainImpulsivity | — | Sí | ≥2 FALSE_ALARM <250 ms y FALSE_ALARMS confident ≥2; paradigma ∈ {GO_NO_GO, STOP_SIGNAL, PVT} | stepUp 0.08 en INHIBITORY_DEMAND (E3/E4/E12 —E12 es PVT, está en `implemented` y `provokedErrorRate` lee `inhibitoryDemand`, `E12ReengancheGenerator.kt:21`: vivo, no inerte—) o eje gobernante (E1 ⇒ TIME_ON_TASK inerte); **ratchet** (nunca baja) |
| StepUp / StepDown / SwitchAxis / Cooldown / StopSession | — | **No** | requieren señales que nadie produce | — |
| NOT_APPLICABLE_TO_FAMILY | — | No (sin productor) | — | — |
| `AdaptiveModeGate` | — | **Dead** (0 call-sites) | — | — |

`impulsivityDecision` precede a `performanceDecision` sin leer las tres señales (`MB-FOCUS-0164`). `baselineCv = cfg.fallbackBaselineRtCv` (`AdaptiveEngine.kt:96-98`, clase `DefaultAdaptiveEngine`; no existe `DefaultAdaptiveEngine.kt`): el `baselineRtCv` calibrado **nunca llega** al motor adaptativo. Persistencia (K7 3/3): `FocusSessionStateHolder.kt:424-448` escribe historial y perfil sin guardar por `admitted`; para una sesión no medible `nextDifficulty == setup.difficulty` (`:500-505`), así que la dificultad NO se mueve —lo que se escribe pase lo que pase es `totalSessions+1`, `updatedAtMs`, la fila de historial y, en la primera sesión, la materialización de `startingDifficulty[id]` (EASIEST, o el default por arquetipo si `focusScoreSurfacesEnabled`), pinneado como diseño en `FocusQualityGateWiringTest.kt:139-140`. Hold adicionales inalcanzables: HYSTERESIS_AFTER_CHANGE, MAINTENANCE_BAND y NOT_APPLICABLE_TO_FAMILY (este último sin productor en commonMain; el KDoc de `FocusAdaptiveRulePolicy.kt:19-20` sobreafirma).

## 14.2 Propiedades falsadas

| Invariante | Resultado | Evidencia |
|---|---|---|
| Adaptation no optimiza el score oficial | **Cumple** (no hay score oficial; PS no entra en el motor) | `decideAdaptation` no lee PS |
| Más errores no suben dificultad | **Viola en E1 sim**: una sola pulsación por bloque en E9 y no-responder en E1 dejan ACC alta ⇒ escalera de calibración sube PL (`MB-FOCUS-0137/0138` pinnean la limitación) | sim + guards invertidos |
| Ratchet controlado | **Viola**: ContainImpulsivity sólo sube; sin StepDown alcanzable, la dificultad inhibitoria de un impulsivo crece 0.08/sesión hasta 1.0 | `AdaptiveModels` |
| Dificultad persiste sólo si medible | **Viola**: `finishSession` escribe `decision.nextDifficulty` en `profile.startingDifficulty` incondicionalmente (aunque Hold devuelva la misma) | `FocusSessionStateHolder.finishSession` |
| Intra-sesión | No hay adaptación intra-sesión (bloques a dificultad fija) | `runSession` |

## 14.3 Parámetros

`AdaptiveConfig`: accUp 0.85, hold 0.72, down 0.65, stepUp 0.08, stepDown 0.12, fallbackBaselineRtCv. `FEEDBACK_MS 320`, warmup 4000 / cooldown 2500 constantes. Ninguno derivado de datos; ninguno versionado como `AdaptiveConfigVersion` (sólo `scoringVersion="focus-scoring-1"` en sync, sin consumidor).

## 14.4 Veredicto

El motor adaptativo es, con los flags embarcados, **un no-op**, y con lifecycle ON un **ratchet unidireccional de un solo eje** (INHIBITORY_DEMAND) para dos ejercicios. No cumple el gate 4 de CLAUDE.md §1 («adaptación de dificultad o razón explícita de dificultad estática») para 11 de 13 ejercicios. Recomendación vNext (PARTE XXIX, capa 6): política de selección con objetivo de exactitud (staircase 3-down/1-up hacia 79 %, o 1PL) por eje, desacoplada del estimador, con StepDown garantizado y versión.

---

# PARTE XV — Performance Score Audit

Hay **tres** «scores» distintos en el código, ninguno visible con los flags embarcados (`focusScoreSurfacesEnabled = false`), y ninguno es una estimación de capacidad.

## 15.1 `PersonalizedSessionScorer` (PS, 0–100) — `REPO_VERIFIED` + `REIMPLEMENTATION_REPRODUCED`

```
speed     = clamp((800 − RT_MEDIAN) / (800 − 250), 0, 1)
stability = clamp(1 − RT_CV / 0.5, 0, 1)
base      = 0.5·ACCURACY + 0.2·speed + 0.3·stability
lapsePen  = min(0.3, 0.03 · LAPSES)
PS        = 100 · (base − lapsePen)          ; gated: ACCURACY confident && n ≥ 20
```
Modificadores de arquetipo (`ArchetypeInference`, escalas fijas RT 250/550; defaults acc 0.5/RT 400/CV 0.25; `pressureDelta` siempre 0.0; CHANGE_THRESHOLD 0.30 ⇒ ELITE/ADVANCED inalcanzables) alteran el peso sin que el arquetipo se muestre ni se valide.

**Falsación (sim SAT P1–P8, E1 94 ensayos)**: la misma capacidad discriminativa colocada en distintos puntos de la curva speed–accuracy produce PS de 49.8 (P1, cauto, 600 ms) a 81.7 (P5) y 71 (P8, 240 ms). PS **premia un punto concreto de la SAT** (~340 ms, 88 % hit) y penaliza al cauto por lapsos (30 lapsos con umbral 500 en P1; con umbral calibrado 803 ⇒ 79.8). Conclusión: PS mide **estrategia + velocidad + estabilidad de esa estrategia**, no Enfoque. Cumple `dataSufficiency ⊥ confidence` (gate n ≥ 20 aparte de confident) pero no separa velocidad de capacidad.

**Invariantes**: dispositivo más rápido ⇒ PS sube (S9: 89.8 → 72.3 al cambiar a dispositivo +90 ms; o al revés). Repetidor (S15, 190 ms): PS 96. XP no existe ⇒ invariante «XP no cambia capacidad» cumplido por ausencia.

## 15.2 `FocusScore` (UI, `FocusScore.kt:38-62`) — `REPO_VERIFIED`

Componentes: SFI, INHIBITORY_CONTROL_INDEX (sin productor), ACCURACY·100, BLOCK_STABILITY·100, leídos de `sessionMetrics` confident. Como `sessionMetrics` sólo lleva ACCURACY entre esas cuatro, `components.size = 1 < MIN_COMPONENTS = 2` ⇒ `confident = false` **siempre**. Margen = MIN_MARGIN + MAX_EXTRA_MARGIN·(1−maturity): heurístico, no un SE. Veredicto: score no producible; sus tests construyen `sessionMetrics` a mano.

## 15.3 `SUSTAINED_FOCUS_INDEX` (0–100, por bloque)

`100·(1−RT_CV_clamp)·(1−lapseRate)` con guard `cleanRtCount == 0 ⇒ unavailable` (2026-07-15). Sólo bloque. En E9 ≡ 0; en E1 depende de 11–20 hits (sd ≈ 8–12 puntos entre semillas, sim). Presentado en Home como «FOCO» leyendo sesión ⇒ «—» siempre.

## 15.4 Veredicto

| Criterio | PS | FocusScore | SFI |
|---|---|---|---|
| Visible hoy | No | No | No (chip vacío) |
| Es estimación de capacidad | No (mezcla SAT) | No | No (índice descriptivo) |
| Lleva incertidumbre | No | Margen heurístico | No |
| Precisión mostrada ≤ precisión estimada | n/a | Viola si se muestra (0–100 entero sin SE) | Viola |
| Comparable entre versiones | No (sin versión) | No | No |

Recomendación: PS puede quedarse como **feedback de sesión** (capa 17) con etiqueta «rendimiento de la sesión», nunca como «Enfoque»; el score de capacidad debe venir del estimador (capa 8) con banda.

---

# PARTE XVI — Growth Index Audit

No existe un «Growth Index» como símbolo. Lo más cercano: (a) `Trends` (pendiente sobre las últimas sesiones por métrica, con captions direccionales), (b) `TransferReportBuilder` (delta vs baseline por métrica con `deltaText` por magnitud y `withheldForSample`), (c) `EvaluationResult.hasReliableChange` (RCI). `REPO_VERIFIED`.

## 16.1 Qué produce cada uno

| Componente | Entrada | Salida | Corrige práctica | Corrige RTM | Corrige dispositivo | Exige n | Estado |
|---|---|---|---|---|---|---|---|
| `Trends` | últimas k `sessionMetrics` | dirección + caption («Mejorando/Estable/…») | No | No | No | k ≥ 3 | Vivo (flag OFF para SFI; ACC/RT visibles con Home) |
| `TransferReportBuilder` | baseline.perMetric vs retest | `TransferLine(delta, strong/preliminary)` | No | No | No | `measurementSufficient == true` | Vivo; `strong` nunca (EMPTY) |
| `ReliableChangeIndex` | pre, post, SEdiff | RCI + `hasReliableChange` | **No** (sin término Chelune, `MB-FOCUS-0133`) | No | No | MIN_ADMINISTRATIONS 3 + `formIsFresh` | `NotEnoughData` siempre |

## 16.2 Falsación por simulación (`REIMPLEMENTATION_REPRODUCED`, sim_digest)

| Escenario | ¿Debería contar como crecimiento? | Lo que `Trends`/PS dirían | Lo que RCI diría | Veredicto |
|---|---|---|---|---|
| S7 PracticeOnly (aprende la tarea, capacidad constante) | No | PS 60 → 86; Trends «Mejorando» | NotEnoughData | Trends fabrica crecimiento |
| S8 RealImprovement | Sí | PS 36 → 86 | NotEnoughData | Indistinguible de S7 |
| S11 RegressionToMean (día 0 malo) | No | PS 36 → 87 | NotEnoughData | Indistinguible de S8 |
| S9 DeviceSwitch (mismo usuario, +90 ms) | No | PS 90 → 72; Trends «Empeorando» | — | Fabrica declive |
| S12b SubtleSandbag (calibra mal, luego juega bien) | No | Umbral lapso 1188; PS 33 → 89 | — | Fabrica crecimiento y arruina umbrales |
| S10 Fatigue | No | SFI 88 → 60 en bloque 2 | — | Señal intra-sesión, no crecimiento |
| S14 LowExposure (1 bloque, n<20) | n/a | PS `?` (gated) | — | Cumple `null ≠ 0` |

Conclusión: **ninguna superficie longitudinal distingue práctica, regresión a la media, dispositivo y cambio real**. El único componente diseñado para ello (RCI) no puede ejecutarse y, cuando pueda, carece de término de práctica y de SD/ICC poblacional.

## 16.3 Requisitos para un Growth Index honesto (`RECOMMENDATION`)

1. Estimación con banda por administración (capa 8) — no puntos.
2. RCI con término de práctica esperado (Chelune) **o** control por forma alterna + intervalo ≥48 h.
3. Ventana mínima: ≥2 administraciones oficiales con `formIsFresh` y misma `deviceClass`; nunca un solo día.
4. Publicar sólo dirección + confianza («cambio fiable / sin evidencia de cambio»), nunca magnitud sin SE.
5. Versionar la métrica (`growthVersion`) y no comparar entre versiones.

---

# PARTE XVII — Calibration & Baseline Audit

`REPO_VERIFIED` (`BaselineProtocol.kt:80-125`, `BaselineAssembler.kt:30-60`, `DefaultCalibrationEngine.kt:27-36`, `FocusBaselineStateHolder.kt:100-227`, `FocusBaselineScreen.kt:54`, `LocalFocusRepositories.kt:108-109`).

## 17.1 Máquina de estados derivada (no existe como tipo; se reconstruye del código)

```
NO_PROFILE ──(Home load)──▶ NEEDS_BASELINE (profile.baseline == null)
NEEDS_BASELINE ──"Empezar mi base"──▶ RUNNING_CALIBRATION [E1 75, E2 60, E3 60 @0.35/0.35, sin gate]
RUNNING_CALIBRATION ──▶ RUNNING_PROBE [VIGILANCE_PROBE 55 @0.4/0.4; nextProbe consume forma; lifecycle.start()]
RUNNING_PROBE ──admitProbe──▶ Admitted  ──persist──▶ BASELINED (baseline + thresholds + startingDifficulty)
                           └─▶ NotMeasurable ──persist──▶ NEEDS_BASELINE' (baseline = null, thresholds + startingDifficulty ESCRITOS)
BASELINED ──▶ (nunca) RE_BASELINE      ; no hay staleness, no hay re-baseline, no hay invalidación
```
Con flags embarcados: `NotMeasurable(LIFECYCLE_NOT_OBSERVED)` **siempre** ⇒ bucle NEEDS_BASELINE → NEEDS_BASELINE' con umbrales que cambian en cada intento y una forma reservada quemada por intento (4 formas; `unspentProbeId = null` una vez vista; el carril de base nunca lee `repeatsEarlierForm`). Tras 4 intentos, la 5.ª administración repite la forma 1001 sin que nadie lo declare. Y como el cursor avanza por intento completado mientras `priorAdministrations` sólo cuenta filas `Admitted`, cada intento denegado desplaza en uno la forma que dibujará el primer retest con puerta de fiabilidad abierta: con ≥1 intento denegado ese retest cae en forma repetida y la fiabilidad queda retenida en adelante (`ProbeReliabilityEstimator.kt:44`; rompe la garantía de `ProbeBank.kt:20-27`).

## 17.2 Auditoría por propiedad

| Propiedad | Estado | Evidencia | Severidad |
|---|---|---|---|
| Dueño único del estado de base | **Dos dueños**: `profile.baseline` (repo perfil) + fila `probe:<userId>:<endedAtMs>` (historial); pueden divergir (persist parcial) | `FocusBaselineStateHolder.persist :195-227`; `recordProbe` | P2 |
| Gate de calibración | **Ninguno** para los 3 bloques CALIBRATION (sin lifecycle, sin anti-gaming, sin mínimo de esfuerzo) | `runBaseline :100-170` sólo `admitProbe` para el probe | P1 (sandbag) |
| Umbrales | Pool de los HIT con RT ≥150 ms de los 195 ensayos E1+E2+E3 (RT simple + elección + go/no-go; las CR de go/no-go y los no-objetivos de E1 no aportan RT), sin filtro por `exerciseId` (`DefaultCalibrationEngine.kt:26-36`); suelo `max(500, mediana+2.5·sMAD)` ⇒ el pooling sólo puede SUBIR el umbral (dirección favorable: menos lapsos contados después); el único «gate» es el fallback `MIN_SAMPLES = 8` a defaults poblacionales — ni anti-gaming ni ciclo de vida: los bloques de calibración nunca pasan por `admit` (`FocusBaselineStateHolder.kt:150-159`: sólo `block.isProbe`; el escáner `focusQualityGateScanner` sólo pinnea carriles de sonda) y el ensamblador consume los `trials` crudos, no sus métricas. El repo lo trata como diseño DELIBERADO (comentario :117-118; test `FocusBaselineStateHolderTest.kt:147-148` «the calibration work is untouched»): decisión de diseño bajo revisión, no defecto inadvertido. Consumidores aguas abajo sin comprobar admisión: `FocusSessionStateHolder.kt:223,:320`, `OvertrainingGuard.kt:50`, `RecoveryPolicy.kt:49`, `DailyFocusPlan.kt:115`, `ArchetypePersonalization.kt:22`. Refutación K4: sobrevive 3/3 (92–95 %) | `BaselineAssembler.kt:42-43` `calibrate(calibrationBlocks.flatMap { it.trials })`; persist `:195-228`; `deviceFingerprint = null` literal en `:209` es propiedad del SNAPSHOT (`BaselineAssembler.kt:48-54`), que sólo persiste en la vía admitida (`:226`); umbrales y `startingDifficulty` no llevan huella alguna | P1 |
| Umbral de lapso | `max(500, mediana + 2.5·sMAD)`: **unidireccional** (nunca < 500; un usuario rápido no gana umbral menor) sobre hits ≥150 ms, MIN_SAMPLES 8 | `DefaultCalibrationEngine.kt:27-36` | P2 |
| Consistencia pre/post | Snapshot del probe calculado con umbrales **pre**-calibración (default 500 en 1.er intento); el retest usa umbrales **post** ⇒ lapsos no comparables | `profile` cargado en `:101` | P1 |
| Persistencia bajo denegación | thresholds + startingDifficulty se escriben aunque `baseline = null` ⇒ **churn por intento** | `persist` rama NotMeasurable | P1 |
| Consumo de formas | 1 forma reservada por intento, denegada o no | `nextProbe` avanza cursor antes de administrar; `releaseProbe` sólo en abort | P1 |
| Covariables de estado | `subjectiveState = null` siempre; `SubjectiveStateDto` (sleep, caffeine, mood, localHour) **nunca poblado** | `FocusBaselineScreen.kt:54` | P2 (para vigilancia) |
| Dispositivo | `deviceFingerprint = null` (único escritor; propiedad del snapshot — umbrales y `startingDifficulty` no llevan huella) | `:209`; `BaselineAssembler.kt:48-54` | P1 |
| Número de administraciones | **1** (single-administration anchor); charter §11.1 exige ≥5 sesiones / ≥3 días / ≥48 h para provisional→calibrada | `03_SCIENTIFIC_MEASUREMENT_CHARTER.md:438-449` (DOCUMENTED_POLICY) vs código | P1 (`MB-FOCUS-0169` propuesto) |
| Snapshot con incertidumbre | `perMetric: Map<MetricKey, Double>` sin n/SD/SE | `BaselineSnapshotDto` | P1 |
| Staleness / re-baseline | Inexistente; una base de hace 6 meses en otro teléfono sigue siendo «tu base» | — | P2 |
| Copy | La pantalla presenta umbral de lapso y RT-CV como «tu base» aunque el comparador sólo usa ACCURACY (única que cruza 20/celda) | `FocusBaselineScreen` + `EvaluationComparator` | P2 (`MB-FOCUS-0092/0130`) |

## 17.3 Contra el charter §11.1 (CEO-OVERRIDE-002: personalización útil vs sobre-reacción)

El código **sobre-reacciona a un solo dato** en tres sitios: umbrales personales (1 sesión), `startingDifficulty` (1 bloque), snapshot (1 probe). El charter exige ≥5 sesiones válidas, ≥3 días, ≥48 h antes de pasar de provisional a calibrada. No existe el estado «provisional» en el modelo (`BaselineSnapshot` no lleva `maturity`). `EvidenceQuantityMaturity` existe en `core/session/cognitive/evidence` (day-cap + span-floor 48 h) y **no lo consume focus**.

## 17.4 Recomendación mínima (capas 7 y 9 de vNext)

1. Gate único (`admit`) para TODOS los bloques de base, no sólo el probe; sin anti-gaming, la calibración no persiste.
2. Umbrales **por paradigma** (E1 no comparte lapso con E2); bidireccional con suelo físico (p. ej. 300 ms) y techo.
3. Estado `BaselineMaturity { NONE, PROVISIONAL(n, days, span), CALIBRATED }` derivado de `EvidenceQuantityMaturity`; snapshot con `{value, n, sd}`.
4. No persistir umbrales ni dificultad si el probe se deniega; `releaseProbe` en denegación por sensor.
5. `deviceFingerprint`, `timebaseSource`, `platform` y `subjectiveState` (opt-in) por administración.
6. Re-baseline por staleness (>90 d) o cambio de `deviceClass`; nunca silencioso.

---

# PARTE XVIII — Uncertainty & Reliability

## 18.1 Qué incertidumbre existe hoy (`REPO_VERIFIED`)

| Nivel | Representación | Es un SE/intervalo | Se propaga | Se muestra |
|---|---|---|---|---|
| Ensayo | ninguna | — | — | — |
| Bloque | `MetricValue(value, n, confident, measurementSufficient: Boolean?)` | No (booleano de piso) | A sesión (n = suma de celdas, R12) | No |
| Sesión | idem | No | A Trends/Report | «Aún sin tendencia» / «(prueba)» |
| Baseline | `Map<MetricKey, Double>` | **No** (sin n, sin SD) | No | «tu base» |
| RCI | `SEdiff` desde `ReliabilitySource` (EMPTY) | Sí, si existiera | A `hasReliableChange` | «Aún sin cambio fiable» |
| PS | ninguna | — | — | (flag OFF) |
| FocusScore | `margin` heurístico | No | — | (flag OFF) |
| Perfil cerebral | `ModuleContribution(confidence, dataSufficiency)` | No (heurísticos ortogonales) | Aggregator | CALIBRATING |

**Ausencia tipada**: correcta en memoria (`unavailable`, `withdrawn`, `NotMeasurable`, `withheldForSample`), **perdida en persistencia** (`MetricValueDto` sin absence ⇒ 0.0 en re-lectura).

## 18.2 Ruido de estimación (sim, S1 honesto, 200 semillas; `REIMPLEMENTATION_REPRODUCED`)

| Dosis | RT mediana sd | RT_CV sd | ACC sd | PS sd |
|---|---|---|---|---|
| 47×2 (=94) | 10.1 ms | 0.023 | 0.015 | 1.79 |
| 55×1 (probe) | **14.0 ms** | **0.031** | 0.020 | 2.17 |
| 94×2 | 7.6 ms | 0.015 | 0.009 | 1.58 |
| 160×2 | 5.7 ms | 0.012 | 0.008 | 1.52 |

Lectura: con la sonda de 55 ensayos, el SE intra-sujeto de RT_CV (~0.03) es del orden del cambio que el producto querría detectar (S8 mejora real: CV 0.29 → 0.12; S7 práctica: 0.10 → 0.11). Un ΔCV de 0.03 entre dos sondas es **ruido a 1 SE**. Para RT mediana, SEdiff ≈ 14·√2 ≈ 20 ms ⇒ RCI 1.96 exige Δ ≥ 39 ms sólo por ruido de muestreo, antes de añadir varianza de dispositivo (20–60 ms) y test-retest.

## 18.3 Tres barreras a la fiabilidad (por qué el RCI no puede ser honesto hoy)

1. **Sin fiabilidad medida**: `ReliabilitySource.EMPTY` en `FocusGraph.kt:156` → `FocusEvaluationService.kt:17` → `ProbeReliabilityEstimator` (tres capas de default; ningún pin de producción exige EMPTY — `MB-FOCUS-0102`). Poblarla sin datos poblacionales fabricaría SEdiff.
2. **Sin n por celda en el snapshot**: aunque hubiera ICC, el lado pre no lleva su SE.
3. **Sin comparabilidad de forma/dispositivo**: `repeatsEarlierForm` sólo en un carril; `deviceFingerprint` null; sin versión de generador por fila (salvo E2).

## 18.4 SEM derivable ya (`RECOMMENDATION`, coste S)

Sin población: SE intra-sujeto por bootstrap de ensayos dentro del bloque (mediana, CV, ACC) es calculable en `blockMetrics` con los RT crudos que ya están en memoria antes de persistir. Persistir `{value, n, se_boot}` por métrica y bloque permite (a) bandas honestas por administración, (b) `SEdiff` mínimo = √(se₁²+se₂²) como cota inferior, (c) declarar «no distinguible del ruido» en vez de «estable».

## 18.5 Reliability readiness

| Requisito | Estado |
|---|---|
| ICC test-retest por métrica y sonda | `UNKNOWN` (nunca medido) |
| SD poblacional | `UNKNOWN` |
| Formas alternas equivalentes (equating) | `UNKNOWN` (4 formas por sonda, sin estudio) |
| Término de práctica | Ausente (`MB-FOCUS-0133`) |
| Intervalo mínimo entre administraciones | Sin aplicar (charter pide 48 h) |

---

# PARTE XIX — Practice / Exposure / Transfer

## 19.1 Control de práctica (`REPO_VERIFIED`)

| Mecanismo | Existe | Conectado | Efecto real |
|---|---|---|---|
| Semillas reservadas para sondas (`ProbeBank.allReservedSeeds` disjunto de entrenamiento) | Sí | Sí | Evita **práctica de ítem** entre entrenamiento y sonda |
| 4 formas por sonda + `repeatsEarlierForm` + `formIsFresh` | Sí | Sí (evaluación); progreso pasa `null` | Evita repetir forma en el 1.er retest fiable; el suministro **envuelve** en la 5.ª administración |
| `ExposureTracker` (`core/bank`) | Sí | **0 call-sites** (R9) | Ninguno |
| Persistencia de `(seed, rngVersion, difficulty, spec)` por sesión | Parcial (sólo `SessionRecorder` E2 `FormDescriptorV1`) | — | Exposición no computable para 12 ejercicios |
| Término de práctica en RCI | No | — | Práctica de tarea = «mejora» |
| Control de práctica de **tarea** (misma mecánica) | **No**: la sonda es la tarea entrenada con otras semillas | — | Owen 2010: la mejora en la tarea entrenada es real y no transferible |

## 19.2 Exposición

Sin `ExposureTracker` cableado ni semilla persistida, **no se puede calcular** cuántas veces un usuario ha visto una forma ni una familia. La única cuenta es el cursor de formas por sonda (`userId:probeId`), tri-estado desde R13.

## 19.3 Transferencia

| Tipo | Instrumento | Estado |
|---|---|---|
| Near-transfer (misma familia, otra forma) | Sonda con semillas reservadas | Medible en principio; bloqueado por fiabilidad (PARTE XVIII) |
| Near-transfer (familia distinta, mismo constructo) | **No existe**: la sonda de vigilancia es E1, la de filtrado es E2, la de inhibición es E3 | Ninguna |
| Far-transfer / vida diaria | `NEAR_TRANSFER_INDEX` sin productor; `TransferReport` lo etiqueta | Ninguna; cualquier claim sería violación (Owen/Simons) |
| Ecológico (E11 distracción digital) | Sin medida | Sólo narrativo |

**Arquitectura de transferencia recomendada** (capa 14): por cada CORE, ≥1 *probe de transferencia* con paradigma distinto al entrenado (p. ej. entrenar flanker → medir con antisacada-like por exactitud; entrenar go/no-go → medir con CPT-d'), semillas reservadas, dosis ≥ la de fiabilidad, administrado cada N sesiones con `formIsFresh`. Claim máximo publicable: «mejora en tareas de [familia] no entrenadas».

---

# PARTE XX — Personalization Audit

## 20.1 Escala de niveles (definición adoptada)

| Nivel | Significado |
|---|---|
| L0 | Sin personalización: misma tarea/dosis para todos |
| L1 | Dificultad inicial por bloque de calibración |
| L2 | Umbrales personales (lapso, CV) |
| L3 | Adaptación entre sesiones por rendimiento |
| L4 | Adaptación intra-sesión |
| L5 | Selección de ejercicio por perfil de subhabilidades (prescripción) |
| L6 | Dosis/horario por respuesta al entrenamiento y estado (fatiga, sueño) |
| L7 | Modelo individual con incertidumbre (Bayesiano/IRT) que gobierna todo lo anterior |

## 20.2 Nivel actual

| Condición | Nivel efectivo | Evidencia |
|---|---|---|
| Flags embarcados | **L0** (la sesión no es alcanzable; sólo se personalizan umbrales que nadie usa) | PARTE I P0 |
| Lifecycle ON | **L1–L3 parcial**: L1 (E1/E2/E3, ladder inflada), L2 (pooled, unidireccional, no llega al motor adaptativo), L3 sólo ContainImpulsivity en E3/E4 | PARTES XIII–XIV, XVII |
| L5 | `DailyPlanGenerator` elige por rotación/recency, no por perfil de subhabilidad; recomendación por defecto = E1 | `DailyPlanGenerator`, `FocusHomeStateHolder` |
| L6 | `RecoveryPolicy`/`OvertrainingGuard`/`TrainingCycle` corren en cada Home load (OvertrainingGuard no gated por flag; pool ACCURACY entre ejercicios); `MicroPauseGate` flag OFF; sin estado subjetivo | `FocusHomeStateHolder.kt:67-79` |
| L7 | Inexistente | — |

## 20.3 Sobre-reacción vs personalización útil (CEO-OVERRIDE-002)

| Decisión personalizada | Datos que la sostienen | Charter §11.1 | Veredicto |
|---|---|---|---|
| Umbral de lapso | 1 sesión, 195 ensayos pooled | ≥5 sesiones / 3 días / 48 h | **Sobre-reacción** |
| `startingDifficulty` | 1 bloque (60–75 ensayos), exactitud CR-dominada | idem | Sobre-reacción |
| `ArchetypeInference` | snapshot de 1 probe + defaults | — | Sobre-reacción (y etiqueta no validada; `MB-FOCUS-0164`) |
| ContainImpulsivity ratchet | ≥2 FA rápidas en 1 sesión | — | Sobre-reacción sin retorno |
| OvertrainingGuard | ACCURACY pooled entre ejercicios | — | Ruido (compara flanker con MOT) |
| Recomendación E1 por defecto | ninguna | — | L0 |

**Robustez poblacional**: ninguna constante (0.85/0.72/0.65, 800/250, 0.5, 500/150) tiene procedencia empírica declarada; no hay tabla de priors poblacionales; nada impide que un individuo con 1 sesión sobrescriba cualquier prior (porque no hay prior).

## 20.4 Dimensiones de personalización que sí existen como parámetros (para el diseñador)

`DifficultyVector` (9 ejes), `SessionTemplate` (5/10/20/30 min → 55/94/157/160 ensayos), `thresholds.lapseRtMs`, `baselineRtCv`, `startingDifficulty` por ejercicio, arquetipo (4 modificadores), `MicroPauseGate`, plan diario (recency/rotación). **Ninguna** se condiciona a un objetivo del usuario (no existe `UserGoal`), ni a preferencias, ni a contexto (hora, dispositivo), ni a respuesta al entrenamiento (no hay estimador de respuesta).

---

# PARTE XXI — Dose, Fatigue & Scheduling

## 21.1 Dosis

| Parámetro | Valor | Procedencia | Evidencia de adecuación |
|---|---|---|---|
| `SessionTemplate` | 5/10/20/30 min → 55/94/157/160 ensayos (`coreMs/3800`, clamp 24–160); 2 bloques | Constante | Ninguna; 55 no cruza pisos de RT (110 necesarios para RT_CV en E1); 160 sí para ACC/RT |
| Bloques de base | 75+60+60+55 = 250 ensayos ≈ 12–15 min | Constante | Sin justificación; la sonda (55) es la parte más corta y la única que cuenta |
| Warmup / cooldown | 4000 / 2500 ms | Constante | — |
| `FEEDBACK_MS` | 320 | Constante | — |
| ISI E1 | 1.2–7 s | lerp | Foreperiodo PVT canónico 2–10 s |
| Frecuencia | Plan diario (1 sesión/día sugerida) | `DailyPlanGenerator` | Sin dosis-respuesta |
| Ciclo | `TrainingCycle` (semana N de 8 retirado, R1) | — | Sin evidencia |

**Fatiga**: `FATIGUE_INDEX` (bloque 2 vs 1) inválido a 2–3 min (PARTE V); `OvertrainingGuard` pool ACCURACY; `RecoveryPolicy` sin estado subjetivo; `MicroPauseGate` flag OFF. Ningún mecanismo lee sueño/hora/estado (dormido).

## 21.2 Scheduling

- `RecoveryPolicy`/`OvertrainingGuard`/`TrainingCycle` corren en **cada carga de Home** (`FocusHomeStateHolder.kt:67-79`) sobre `recent` — coste O(k) trivial, pero decisiones sin dueño de estado (no se persisten, se recomputan).
- No hay intervalo mínimo entre sondas (charter: 48 h); un usuario puede administrar las 4 formas en 10 minutos y quemar el suministro.
- No hay ventana circadiana ni registro de hora local (`SubjectiveStateDto.localHour` dormido).

## 21.3 Recomendación (capa 13)

Dosis de **medida** derivada del piso (`MinSampleGate` × tasa de señal × margen de HIT ⇒ ≥110 ensayos E1; ≥98–120 para diferencias, ver 2026-08-27), separada de la dosis de **entrenamiento** (objetivo de tasa de acierto). Intervalo ≥48 h entre sondas; máximo 1 sonda/semana por CORE; `localHour` + `sleepHours` opt-in como covariables de vigilancia; fatiga sólo con bloques ≥10 min.

---

# PARTE XXII — Relaxed Experience Compatibility

Marco: CEO-OVERRIDE-003 (`CEO_APPROVED`): la experiencia relajada es una **variante dentro del mismo sistema cognitivo**; puede cambiar fricción/duración/presentación/intensidad/feedback/estética/presión/complejidad de entrada; **no** puede crear taxonomía paralela, fragmentar el perfil, introducir métricas incompatibles sin etiquetar ni sustituir entrenamiento por gamificación vacía.

## 22.1 Qué existe hoy que pueda servir de variante (`REPO_VERIFIED`)

| Palanca | Símbolo existente | ¿Afecta a la medida? | ¿Está etiquetado en la fila persistida? |
|---|---|---|---|
| Duración | `SessionTemplate` 5/10/20/30 min | Sí (n; 55 no cruza pisos de RT) | `plannedTrials`/`completedTrials` = literales `1` ⇒ **no** |
| Presión | `DifficultyAxis.PRESSURE` (E12/E13 lo leen) | Sí (SAT) | Vector no persistido por fila (salvo E2 `FormDescriptorV1`) ⇒ **no** |
| Feedback | `FEEDBACK_MS 320` constante | Sí (post-error) | no |
| Estética/ambiente | `FocusAtmosphere` | Desconocido (sin medida) | no |
| Intensidad (dificultad) | `startingDifficulty` | Sí | no |
| Complejidad de entrada | single-tap / choice / stop | Sí (paradigma) | `exerciseId` sí |

**Riesgo principal (P1, `INFERRED` desde persistencia)**: una variante relajada que cambie duración, presión o feedback produce filas **indistinguibles** de las oficiales en `SessionResult`/`probeHistory`, porque la fila no lleva ni `sessionMode`, ni `template`, ni el vector de dificultad, ni la versión del generador. Consecuencia científica: `Trends`, `OvertrainingGuard`, `RecoveryPolicy`, `TransferReportBuilder` mezclarían modos ⇒ contaminación del comparador y falso declive/mejora.

## 22.2 Hazards concretos frente al override

| Hazard | Violaría | Estado hoy |
|---|---|---|
| Sesión relajada con `PRESSURE=0` y `FEEDBACK_MS` largo comparada contra base con `PRESSURE` alto | «métricas incompatibles sin etiquetar» | Posible: nada etiqueta |
| Sesión relajada de 5 min (55) contando como administración de sonda | «no fragmentar perfil» | Posible: el probe siempre tiene 55; una «sonda relajada» sería idéntica en n |
| Variante con estímulos más grandes/contraste alto (E1 `contrast = lerp(1.0, 0.4)`) | Cambia el umbral perceptual ⇒ umbral de lapso distinto | Posible: umbrales pooled por usuario, no por variante |
| Sustituir ejercicio por «respiración/atmósfera» sin ensayo | «gamificación vacía» | No existe; `FocusAtmosphere` es decorativo dentro de sesión |
| Taxonomía paralela («Enfoque suave») | «taxonomía paralela» | No existe |
| E12 (recuperación post-error, redundante con E1) presentado como «modo relajado» | ninguna, si se etiqueta | Recomendado: E12 → **variante** de E1 con eje PRESSURE, no ejercicio |

## 22.3 Compatibilidad: qué debe existir antes de una variante (`RECOMMENDATION`, capas 2/3/16)

1. `SessionMode { OFFICIAL, TRAINING, RELAXED }` persistido por fila, con `template`, `difficultyVector`, `generatorVersion`, `feedbackProfile`.
2. Regla: RELAXED **nunca** entra en `probeHistory`, `Trends` oficiales, `OvertrainingGuard` ni `TransferReport`; sí entra en exposición y adherencia.
3. Umbrales personales por `(paradigma, variante)`, o variante que no toque PL/contraste.
4. Un solo `ExerciseId` por mecánica; la variante es un `VariantSpec` del mismo id (evita la taxonomía paralela por construcción).
5. Métrica de la variante etiquetada `metricVersion` distinta si cambia la ventana de respuesta (RT no comparable).

Veredicto: **compatible por diseño futuro, incompatible con la persistencia actual** — introducirla hoy contaminaría la única medida longitudinal.

---

# PARTE XXIII — Accessibility & Construct Invariance

## 23.1 Accesibilidad (estado `REPO_VERIFIED`)

| Dimensión | Estado | Evidencia |
|---|---|---|
| Objetivos táctiles | 48 dp / 44 pt vía `core/a11y/A11yTargets` (movido desde focus2 el 2026-09-03); `FocusControlTargetTest` prohíbe literales | R10 |
| Visión del color | E6 par de regla con canal de **luminancia** (`FocusRuleColorDark` #C4143C, 3.89:1 vs claro, 3.36:1 vs lienzo), `FocusFieldAchromaticTest` con `SizeBand`; 12/13 renderizadores contestables sin tono (E10 identidad de letra, E9 anillo 1.8×, E8 alfa 0.4, E2/E5 glifo+posición) | `MB-FOCUS-0112` CLOSED |
| Nodos semánticos en estímulos | **Deliberadamente ausentes** en los 13 campos (la descripción revelaría la respuesta o se anunciaría dentro de la ventana de RT); `FocusStimulusSemanticsTest` pinnea la limitación con polaridad invertida (`MB-FOCUS-0110`) | 2026-09-07 |
| Lectores de pantalla en flujo (Home/Baseline/Summary) | Controles con rol y descripción; estímulos no | idem |
| Movimiento reducido | `FocusAtmosphere` y animaciones E9/E10 son **la tarea** (funcionales); sin respeto a `prefers-reduced-motion` porque reduciría el paradigma | INFERRED |
| Idioma | Copy sólo en castellano; instrucciones textuales (E6 regla, E13) | `FocusStrings` |
| Tamaño de fuente del sistema | RSVP E10 letras y E6 glifos escalan con `sp`? **No verificado** | UNKNOWN |
| Latencia motora / temblor | Ninguna adaptación (ventana 950–1400 ms E1) | — |

## 23.2 Invariancia del constructo (lo que la accesibilidad NO garantiza)

Un instrumento es invariante si mide lo mismo en subgrupos (DIF/MI). Nada lo ha comprobado. Fuentes de **no invariancia plausibles** (todas `HYPOTHESIS` hasta medir):

| Fuente | Mecanismo | Métrica afectada | Mitigación en vNext |
|---|---|---|---|
| Velocidad motora (edad, tremor, dispositivo) | RT absoluto y ventana 950 ms | RT_MEDIAN, LAPSES, ACCURACY (timeouts) | Exactitud bajo deadline **personal** (percentil de la propia RT), no ventana fija |
| Agudeza/contraste | E1 `contrast` a 0.4 en PL alto | HIT rate ⇒ ACCURACY, PS | Calibración perceptual separada (umbral de contraste) antes de la sonda |
| Visión del color | E6 (resuelto por luminancia); E2/E5 glifo | — | Mantener discriminadores no cromáticos como regla del catálogo |
| Comprensión de instrucciones | E6 reglas, E13 presión | SWITCH_COST, PRESSURE_EFFECT | Bloque de práctica hasta criterio (≥90 %) antes de contar |
| Táctil vs ratón (desktop) | pointer-up vs click | RT +20–60 ms | `deviceClass` por fila; no comparar entre clases |
| Refresh 60/90/120 Hz | onset cuantizado 8–16 ms | RT_CV, lapsos en borde | timebase de frame (core/timing) o clase D declarada |
| Sueño/cafeína/hora | vigilancia | LAPSES, RT slope | `subjectiveState` opt-in como covariable, nunca corrección silenciosa |

**Regla de catálogo derivada (§49)**: todo ejercicio futuro declara qué canal sensorial y motor exige, su discriminador no cromático, y si su métrica oficial depende de RT absoluto (⇒ clase de timing requerida) o de exactitud (⇒ robusta a dispositivo).

---

# PARTE XXIV — Technical Architecture Audit

## 24.1 Dos motores, uno montado (`REPO_VERIFIED`)

| | `features/mindboost/focus/` (v1) | `features/mindboost/focus2/` (V5) |
|---|---|---|
| Montado | Sí (`MindBoostHubScreen.kt:117` → `FocusRootScreen`) | No (`CONTRACT_INERT_NO_WIRING`) |
| Ejercicios | 13 | tracers |
| Timing | `MonotonicClock`, onset pre-`presentAndAwait`, tap UP (clase D) | `WindowedTemporalProbe` + `TemporalClassPolicy` + `FocusTemporalEligibility` (T0–T2) |
| Consentimiento | ninguno específico | `SecureFocusConsentStore` (4 estados) |
| Elegibilidad | `SessionQualityGate` + `FocusAntiGaming` | `FocusEvidenceStatePolicy` (sombra) |
| Persistencia | `mindboost_focus` + `mindboost_sessions` | — |

La infraestructura de calidad temporal (`core/timing`: `FrameTimingProbe`, `FrameStatsCalculator`, `TemporalQualityThresholds`, `WindowedTemporalProbe`) **existe, está testeada y no la consume focus v1**. `ProductionFocusGraphWiringTest` fija `focusEngine`/`seedSource` pero no las costuras de fiabilidad (`MB-FOCUS-0102`).

## 24.2 Telemetría

| Evento | Existe | Emitido | Consumidor |
|---|---|---|---|
| Sesión iniciada/terminada, bloque, admisión, sonda, calibración, adaptación, abandono | **0 eventos Focus** en `MindBoostAnalytics` | — | — |
| Breadcrumbs de navegación (`AppRoute.telemetryName`) | Sí (app) | Sí | Crashlytics |
| `PORTFOLIO_RUNTIME_EVIDENCE.json` | Sí (untracked; 2 corridas EMULADOR) | manual | validador de portfolio |

Gate 6 de CLAUDE.md §1 («eventos de analítica») **no se cumple** para ninguno de los 13. Sin telemetría no hay tasa de abandono por ejercicio, ni distribución de admisiones, ni tasa de `NotMeasurable` por causa — es decir, no se puede saber en producción si el P0 (bucle de base) le ocurre a alguien.

## 24.3 Versionado y comparabilidad

| Artefacto | Versión | Consumidor | Veredicto |
|---|---|---|---|
| `FocusSchema.CURRENT` | 1, **compartida por 3 DTOs** (perfil, sesión, probe) | `readVersioned` (cuarentena de futuro) | Un bump borra las tres; sin migración por DTO (`MB-FOCUS-0157` KDoc) |
| `scoringVersion = "focus-scoring-1"` | en payload de sync | **ninguno** | inerte |
| `RngVersion` + `SessionSeed` | sí | generadores | no persistido por fila (salvo E2) |
| `FormDescriptorV1` (seed, rngVersion, difficulty, spec) | sí | `SessionRecorder` **sólo E2** | 12/13 sin reproducibilidad |
| `generatorVersion` | por generador (E2 = 3) | `FocusFormContract` cross-check | no persistido |
| `AdaptiveConfig` / `MetricThresholds` / `MinSampleGate` | sin versión | — | un cambio de constante rompe comparabilidad en silencio |
| `probeId` en `recordProbe` | ausente (clave `probe:<userId>:<endedAtMs>`) | — | `MB-FOCUS-0121` |

Invariante §42 «nueva versión no hereda comparabilidad»: **no verificable** — no hay versión que comparar.

## 24.4 Persistencia (`REPO_VERIFIED`, `LocalFocusRepositories.kt`, `FocusPersistenceMappers.kt`)

- `SecureKeyValueStore` cifrado (Android Keystore AES-GCM; desktop in-memory; iOS Keychain) ✔; sólo resúmenes ✔ (trials descartados).
- **Ausencia perdida**: `withdrawn()` (`FocusSessionAssembler.kt:248-250`) y toda `MetricValue.unavailable` (`MetricKey.kt:88`) escriben `value = 0.0` antes de append; `MetricValueDto` sin `absence` ⇒ re-lectura conserva `confident=false` y la admisión pero pierde la razón (`MB-FOCUS-0091` familia; deuda declarada 2026-07-15).
- `plannedTrials = 1`, `completedTrials = 1` literales en el `SessionRecord` de `core/session` (no en `SessionSummaryDto`, que lleva `trialCount` real); `deviceFingerprint = null` (único escritor, `FocusBaselineStateHolder.kt:209`); `SubjectiveStateDto` dormido (ninguna pantalla pasa `SubjectiveState`).
- Dos namespaces (`mindboost_focus`, `mindboost_sessions`) sin invariante que los relacione; `AccountDeletionCoordinator.kt:24-40,64-72` borra con `clearNamespace()` blanket (correcto para erasure, sin verificación por clave).
- `readCursor` tri-estado sin cuarentena (R13) ✔.

## 24.5 Privacidad y datos

| Aspecto | Estado |
|---|---|
| Datos cognitivos en reposo | Cifrados; sin trials crudos ✔ |
| Egreso | `mindboostSyncFirestoreEnabled = false` (legible por Remote Config en Android, `MindOSApplication.kt:139-142`, pero el decorador nunca se construye: `FocusGraph.kt:160-161` cablea `LocalSessionHistoryRepository` directo ⇒ «inerte por construcción», `MB-FOCUS-0062`); `firestore.rules` deniega `users/{uid}/sessions` sólo por el catch-all `/{document=**}` (`:1840-1842`), sin regla específica —el KDoc del gateway (`GitLiveFocusSessionFirestoreGateway.kt:17-18`) afirma una regla `allow update: if false` que no existe—; `accountDeletion.ts` no purga `users/{uid}/sessions` y borrar `users/{uid}` (`:458`) no cascada ⇒ hueco de borrado LATENTE el día que se cablee (CLAUDE.md §5). K6 3/3 |
| Borrado de cuenta (cliente) | `clearNamespace` sobre AccountBound ✔ (`AccountBoundErasureCoverageTest`) |
| Borrado de cuenta (servidor) | `accountDeletion.ts` **no toca datos Focus** — correcto hoy (no hay egreso); precondición de cualquier sync |
| Consentimiento | ninguno para Focus v1; focus2 tiene el store pero no está montado |
| `AccessibilityContext` (focus2) | podría inferir discapacidad ⇒ dato sensible si se persiste (P3, no montado) |
| Telemetría PII | 0 eventos ⇒ 0 riesgo, 0 observabilidad |
| Exportación debug | ninguna para Focus |

## 24.6 Tests y gates (2026-09-14, `TEST_VERIFIED`)

`desktopTest --rerun` 4468/0F/0E sobre 624 XML frescos; `detekt --rerun` 0; `compileKotlinDesktop`, `compileAndroidMain`, `androidApp:compileDebugKotlin` OK; 15 scanners 15/15; ledger OK 325 hallazgos; `check:governance` **falla por diseño** (`MB-FOCUS-0155`); iOS **NO VERIFICADO** en este árbol. Cobertura de Focus: alta en dominio (generadores, métricas, gates, sondas), **nula en la ruta que el usuario recorre** (Home → Baseline → NotMeasurable → Home no tiene test que la declare como bucle; los tests de holder usan `AppLifecycleProbe` real o fixtures que admiten).

## 24.7 Auditabilidad técnica

| Requisito | Estado |
|---|---|
| Reproducir una sesión desde la fila persistida | **No** (12/13 sin forma; sin trials) |
| Reproducir una métrica desde la fila | Parcial (valor + n; sin RT crudos) |
| Reproducir la decisión adaptativa | No (no se persiste `decision`) |
| Reproducir el veredicto de admisión | Sí (`admission` tipado en la fila de sonda) |
| Reproducir umbrales | Sí (perfil) pero sin historial de versiones |
| Trazar un score visible a su fila | n/a (flags OFF) |

---

# PARTE XXV — Adversarial Simulation Results

**Método** (`REIMPLEMENTATION_REPRODUCED`): reimplementación en Node (`reimpl/focus_reimpl.mjs`, goldens contra `DefaultMetricsEngineTest`/`PersonalizedSessionScorerTest`/`FocusAntiGamingTest` reproducidos) de: generador E1/E3, clasificación, métricas de bloque/sesión, calibración (`max(500, med+2.5·sMAD)`), anti-gaming (D3, D4, BlindStrategyCeiling), `SessionQualityGate` con flags embarcados y con lifecycle ON, PS, FocusScore, SFI, motor adaptativo, `Trends`, RCI con `ReliabilitySource.EMPTY` («prod») y con fiabilidad hipotética 0.8 («what-if»), `ArchetypeInference`. 8 sesiones × 94 ensayos (2×47) por perfil; sesión 0 = calibración. Limitaciones: sólo E1/E3; sin ruido de dispositivo salvo S9; sin abandono; el RCI «what-if» usa SEdiff de una fiabilidad inventada (0.8) y SD del propio perfil — sirve para mostrar **qué diría el RCI si se poblara sin datos**, no un valor real.

Leyenda: PS = `PersonalizedSessionScorer`; GI = crecimiento (Trends / RCI); «oficial» = `probeHistory`/`baseline`/perfil. «Esperado» = lo que un motor honesto debería hacer.

## 25.1 Perfiles S1–S15 (+S12b) — E1

| # | Perfil | Esperado | Observado (sim) | ¿Debe cambiar PS? | ¿Debe cambiar GI? | ¿Debe actualizar evidencia oficial? | Fallo | Sev. |
|---|---|---|---|---|---|---|---|---|
| S1 | Skilled (hit .97, 330 ms, CV .13) | Admitido; PS alto estable; sin crecimiento | AG NONE; embarcado `LIFECYCLE_NOT_OBSERVED` (nunca oficial); flag ON ADMITTED; PS 82.7–90.2 (sd 1.8); Trends STABLE; RCI NED; arquetipo BEGINNER siempre | No | No | Sí (flag ON) / **No** (embarcado) | Con flags embarcados el usuario ideal **nunca** produce evidencia; arquetipo no puede promocionar | P0 / P2 |
| S2 | Reckless (responde a todo, 236 ms) | Invalidado; sin PS; sin adaptación | AG INVALIDATE[D3, BLIND]; gate `RESPONSE_PATTERN`; la reimpl computó PS 57–64 porque `PersonalizedSessionScorer.score(metrics, modifiers)` no tiene entrada de admisión — **REFUTADO en Kotlin** (K4, 88 %): el único call-site (`FocusSessionStateHolder.kt:464-469`) exige `admitted && scoreSurfacesEnabled()`, el PS nunca se persiste (`FocusReport` en memoria) y `FocusQualityGateWiringTest.kt:178-185` lo pinnea; motor propondría ContainImpulsivity sobre TIME_ON_TASK (inerte en E1) | No ✔ (el guard vive en el caller, no en el tipo) | No | No ✔ | Invariante impuesto por un solo caller + test de wiring (estilo `MB-FOCUS-0071`): un segundo call-site futuro podría omitir la puerta; ContainImpulsivity apunta a eje inerte en E1 | P3 |
| S3 | Conservative (hit .95, 700 ms) | Admitido; PS moderado; **sin** crecimiento | s0 lapsos **27** (umbral 500 pre-calibración), s1+ lapsos 1–5 (umbral 849 post); PS 43.9 → 65–72; what-if RCI LAPSES **−13.18\* «fiable»**; SFI s0 = **0** | No (misma capacidad) | **No** — el what-if diría sí | Sí | **Cambio de umbral pre/post fabrica una «mejora fiable» en LAPSES** y un SFI 0 en la primera sesión | P1 |
| S4 | Random (50 % respuesta al azar) | Invalidado | INVALIDATE[BLIND]; PS 16–46 computado sólo en la reimpl (en Kotlin el caller lo gatea por admisión, ver S2); RT_MEDIAN/CV/LAPSES **retenidos** (`withheld`) por muestra | No | No | No ✔ | idem S2 (refutado como defecto vivo) | P3 |
| S5 | Anticipator (30 % anticipaciones, 290 ms) | Retenido (WITHHOLD) | WITHHOLD[D4]; gate `RESPONSE_PATTERN`; PS 84–91 computado | No | No | No ✔ | idem | P2 |
| S6 | Lapser (hit .95, 8 lapsos/sesión) | Admitido; PS penalizado; sin crecimiento | umbral 541; PS 60–77 (variable por lapsos); Trends STABLE | No | No | Sí | PS oscila ±17 con la misma capacidad por conteo de lapsos (`0.03·L`, cap 0.3) | P2 |
| S7 | PracticeOnly (capacidad fija; RT baja 450→365 por aprender la tarea) | Sin crecimiento oficial | umbral 571; PS **60 → 87**; Trends RT «IMPROVING»; what-if RCI RT −2.25\*, LAPSES −4.22\* «fiable» | **No** (sube 27) | **No** (diría sí) | Sí, pero como misma capacidad | **Práctica indistinguible de mejora**; sin término de práctica | P1 |
| S8 | RealImprovement (hit .86→1.0, 514→345 ms, CV .29→.10) | Crecimiento | umbral 1027 (!); PS 36 → 91; Trends IMPROVING ×3; what-if RCI las 4 «fiables» | Sí | Sí | Sí | Umbral de lapso 1027 ms (calibrado sobre la peor sesión) inutiliza LAPSES después; RCI real NED | P1 |
| S9 | DeviceSwitch (mismo usuario; s2+ en dispositivo +90 ms) | Sin cambio | PS 90 → 69; Trends RT/LAPSES «DECLINING»; what-if RCI RT 2.04\*, LAPSES 2.64\* «fiable» | **No** (baja 21) | **No** (diría declive) | Sí, pero comparable | **Fabrica declive**; `deviceFingerprint` null | P1 |
| S10 | Fatigue (bloque 2 más lento/variable) | Admitido; sin crecimiento; señal de fatiga intra-sesión | SFI 88.6 / 56.5 (b1/b2); FATIGUE_INDEX 0.58; PS 71–79; Trends STABLE | No | No | Sí | FATIGUE_INDEX sí se mueve (0.58 vs ≈0.5), pero no lo lee ningún consumidor (`OvertrainingGuard` lee ACCURACY) | P3 |
| S11 | RegressionToMean (día 0 malo, luego normal) | Sin crecimiento | umbral 877; PS 36 → 88; Trends IMPROVING ×3; what-if RCI 4/4 «fiables» | **No** | **No** | Sí | **RTM indistinguible de S8** | P1 |
| S12 | DifficultyGaming (s0 aleatorio para calibrar bajo; luego bien) | s0 invalidado; base no persiste; luego normal | s0 INVALIDATE[BLIND] ⇒ base no admitida ✔ **pero umbral 716 sí calibrado y persistido**; PS 31 → 89 | No | No | s0 No ✔ / umbrales **sí** ✗ | Umbrales pooled de bloques **sin gate** persisten aunque el probe se deniegue | P1 |
| S12b | SubtleSandbag (calibra lento pero honesto-plausible; luego bien) | Base honesta o rechazada | AG NONE (acc .86, no cruza techo ciego); umbral **1188 ms**; PS 33 → 89; what-if RCI 3/4 «fiables» | **No** | **No** | Sí (indistinguible de S8) | **Sandbag no detectable**: sin mínimo de esfuerzo, sin comparación intra-usuario; umbrales arruinados | P1 |
| S13 | HighVariance (CV .24–.46) | Admitido; PS bajo-estable; sin crecimiento | umbral 691; PS 46–56; what-if RCI RT_CV 3.75\* «declive fiable» | No | No (diría declive) | Sí | Varianza intra-sujeto alta ⇒ RCI what-if dispara por ruido | P2 |
| S14 | LowExposure (1 bloque, 4–12 ensayos) | Todo retenido | AG NONE (<12 ensayos ⇒ no juzga) ✔; PS `?` ✔; RCI `withheld` ×4 ✔; ACC=1 con n<20 no cuenta ✔ | n/a | n/a | No ✔ | Ninguno (`null ≠ 0` cumplido) | — |
| S15 | Repeater (190 ms constante, hit .99) | Sospechoso (tiempos imposibles para E1 con ISI 1.2–7 s no lo son, pero 190 ms < percentil 1 poblacional de PVT) | AG NONE (techo ciego .74; D4 = fracción <150 ms = 0); PS **96**; SFI 94 | No debería premiarse | No | Sí (admitido sólo con el sensor de ciclo de vida ON; con flags de fábrica `NotMeasurable(LIFECYCLE_NOT_OBSERVED)` y AG NONE) | **Tiempos implausibles (<200 ms) y precisos no retienen**: D4 mira anticipaciones (<150), no plausibilidad; invariante «tiempos imposibles no mejoran score» **viola** | P2 |

## 25.2 Variantes E3 (go/no-go, `noGoProportion` 0.35)

| Perfil | Observado | Fallo |
|---|---|---|
| E3_S1 Skilled | AG NONE; PS 69–82; ContainImpulsivity **no** (FA<2 rápidas) | — |
| E3_S2 Reckless | INVALIDATE[D3, BLIND] (acc .61 = techo .61 exacto); PS 75–77 computado sólo en la reimpl (Kotlin lo gatea, ver S2); motor ContainImpulsivity(INHIBITORY_DEMAND) | Acc del respondedor ciego **coincide** con el techo ⇒ D3 decide por RT<250 y acc≤techo+0.05: frontera; un masher a 260 ms no cae en D3 (banda 250–chance, `MB-FOCUS-0147`) |
| E3_S2b FastButNotMasher (242 ms, hit .84, FA 14→10) | **AG NONE**; ADMITTED; ContainImpulsivity ratchet INHIBITORY_DEMAND 0.08/sesión (0.16 tras 2) ⇒ 1.0 en 13 sesiones; PS 85–86 | Ratchet sin retorno sobre un usuario admitido; PS lo premia |

## 25.3 Perfiles SAT (misma d′, distinto criterio; E1 94 ensayos)

| P | hit | RT obj. | ACC | RT | lapsos (umbral 500) | PS | PS con umbral calibrado | IES | RCS | LISAS | SFI |
|---|---|---|---|---|---|---|---|---|---|---|---|
| P1 | .99 | 600 | .989 | 594 | 30 | **49.8** | 79.8 (803) | 562 | 1.76 | 578 | 10 |
| P2 | .97 | 500 | .989 | 495 | 10 | 55.7 | 82.7 (582) | 482 | 2.10 | 476 | 65 |
| P3 | .95 | 430 | .957 | 422 | 5 | 67.6 | 79.6 (571) | 430 | 2.24 | 459 | 56 |
| P4 | .92 | 380 | .979 | 376 | 2 | 75.2 | 81.2 (633) | 392 | 2.59 | 403 | 74 |
| P5 | .88 | 340 | .936 | 334 | 1 | **81.7** | 81.7 | 372 | 2.83 | 366 | 84 |
| P6 | .82 | 300 | .809 | 321 | 0 | 78.5 | 78.5 | 403 | 3.12 | 343 | 86 |
| P7 | .75 | 270 | .851 | 282 | 0 | 77.7 | 77.7 | 388 | 3.37 | 325 | 82 |
| P8 | .65 | 240 | .713 | 246 | 0 | 71.0 | 71.0 | 378 | 3.97 | 294 | 76 |

Lecturas: (1) PS varía **32 puntos** con capacidad constante; con umbral calibrado, 8 puntos — sigue premiando P5. (2) RCS (aciertos/s) crece **monótonamente con la velocidad** (1.76 → 3.97) premiando al más impulsivo (P8, 65 % hit): no es una métrica de Enfoque. (3) IES/LISAS son más planos (372–562) pero siguen penalizando la cautela. (4) SFI 10 en P1 por 30 «lapsos» que son la estrategia del sujeto. (5) Ninguna métrica emitida es invariante al criterio; d′/criterio (SDT) no se computan (`MinSampleGate.DPRIME` existe sin productor en Focus).

## 25.4 Ruido de estimación (S1, 200 semillas)

| Dosis | RT med sd | RT_CV sd | ACC sd | PS sd |
|---|---|---|---|---|
| 47×2 | 10.1 ms | 0.023 | 0.015 | 1.79 |
| 55×1 (sonda) | 14.0 ms | 0.031 | 0.020 | 2.17 |
| 94×2 | 7.6 ms | 0.015 | 0.009 | 1.58 |
| 160×2 | 5.7 ms | 0.012 | 0.008 | 1.52 |

Un PS entero 0–100 sin banda muestra ~50× más precisión de la que tiene (sd ≈ 2 puntos por muestreo, antes de dispositivo y estado).

## 25.5 Resolución de la aparente contradicción S15 vs S5

S15 (190 ms constante) no dispara D4 y S5 (290 ms, 30 % anticipaciones) sí: D4 mide la **fracción de respuestas < 150 ms** (`anticipationRate > 0.30`), no la mediana. S15 nunca baja de 150. Es consistente con el código; el hueco es la ausencia de un detector de **plausibilidad** de RT (percentil poblacional / límite fisiológico por paradigma), no una inconsistencia. Precisiones K8 (3/3, 80–92 %): D3 (`core/antigaming/GamingDetectors.kt:105-108`; no existe `focus/domain/quality/GamingDetectors.kt`) exige mediana < 250 **y** accuracy ≤ techo ciego + 0,05, así que un repetidor a 190 ms con accuracy en el techo SÍ cae (D3 y BlindStrategyCeiling): el hueco exacto es «rápido (≥150 ms) **y** por encima del techo». D4 divide anticipaciones por TODOS los ensayos (`FocusAntiGaming.kt:89`, no-respondidos incluidos), no por respuestas; los ensayos stop de E4 nunca clasifican ANTICIPATION (`TrialClassifier.kt:93-100`), invisibles para D4; bloques < 12 ensayos devuelven NONE (`FocusAntiGaming.kt:51,66`). «Admitido» exige además ciclo de vida observado sin interrupciones: con flags de fábrica S15 acaba en `NotMeasurable(LIFECYCLE_NOT_OBSERVED)` con AG NONE; sólo con `focusLifecycleProbeEnabled=true` (Remote Config, Android) pasa a `Admitted`.

## 25.6 Invariantes §42 puestos a prueba por simulación

| Invariante | Resultado |
|---|---|
| Más errores no mejoran capacidad | Cumple en métricas; **viola** en escalera de calibración (no responder ⇒ ACC .79 ⇒ PL 0.20) |
| Tiempos imposibles no mejoran score | **Viola** (S15) |
| Repetir indefinidamente no produce crecimiento oficial indefinido | Cumple por ausencia (RCI NED); **viola en Trends/PS** (S7) |
| Dispositivo más rápido no parece mejora | **Viola** (S9, en ambas direcciones) |
| XP no cambia capacidad | Cumple (no hay XP) |
| Más sesiones ≠ capacidad | Cumple (no hay conteo que puntúe) |
| Missing ≠ zero | Cumple en memoria (S14); **viola en persistencia** (`withdrawn` → 0.0) |
| Un bloque no evaluado no reduce otro | Cumple (bloques independientes); sesión pondera por celdas |
| Nueva versión no hereda comparabilidad | No verificable (sin versión) |
| Training difficulty ≠ ability | Cumple por omisión |
| UI 0–100 no más precisión que la estimación | **Violaría** si se mostrara (PS entero, sd 2) |
| Un único ejercicio no domina una subhabilidad | **Viola** (sonda = 1 ejercicio por subhabilidad) |
| Un único día no define cambio longitudinal | **Viola** (Trends con k≥3 sesiones del mismo día) |
| Adaptation no optimiza el score oficial | Cumple |
| Probe degradado no entra como oficial | Cumple para admisión; **viola** para umbrales (S12) |
| Enfoque: la sonda no debe repetir forma sin declararlo | Cumple en evaluación; **viola** en base (4 intentos → 5.º repite) |
| Enfoque: la exactitud de una tarea de señal rara no es capacidad | **Viola** (techo ciego .79) |

---

# PARTE XXVI — Red-Team Exploit Catalogue

Cada exploit indica qué se **demostró** (sim o código) y qué queda **a verificar**. Ninguno se ejecutó contra datos reales.

## X1 — Calibración sandbagged + sonda honesta (`REIMPLEMENTATION_REPRODUCED`, S12b)
- **Exploit**: responder lento y algo peor en E1/E2/E3 de calibración (sin cruzar el techo ciego), luego bien en la sonda y en sesiones.
- **Precondición**: lifecycle ON (o cualquier vía que admita el probe).
- **Ruta**: los 3 bloques CALIBRATION no pasan `admit`; `calibrate` pool ⇒ `lapseRtMs` = 1188; `initialDifficulty` = PL 0.20; el probe se admite con acc .86.
- **Consecuencia observada**: umbral de lapso inútil (nadie «lapsa» a 1188); PS 33 → 89; Trends «Mejorando» ×3; what-if RCI «fiable».
- **Consecuencia científica**: base y umbrales personales corrompidos de forma **permanente** (no hay re-baseline); todo cambio posterior es «mejora».
- **Severidad**: P1 (Impacto 5 · Verosimilitud 3 · Confianza 90 % · Fix M · Dep. capa 7).
- **Detección**: comparar RT de calibración vs RT de la sonda del mismo día (Δ > 2 SD intra-sujeto); mínimo de esfuerzo por bloque; umbrales sólo del probe admitido.
- **Mitigación**: gate único para todos los bloques; umbrales por paradigma desde bloques admitidos; `BaselineMaturity.PROVISIONAL` hasta ≥3 administraciones.

## X2 — Repetidor de reflejo (S15)
- **Exploit**: responder a cada onset con RT ~190 ms (posible con foreperiodo predecible o toque sostenido en otro dispositivo).
- **Precondición**: ninguna.
- **Ruta**: D4 mide fracción < 150 ms (0 %); techo ciego .74; ACC .99.
- **Consecuencia**: PS 96, SFI 94, admitido, oficial.
- **Científica**: RT por debajo del límite fisiológico de una PVT (~200 ms mínimo poblacional) contado como rendimiento excepcional.
- **Severidad**: P2 (4·2·85 %·S).
- **Detección**: banda de plausibilidad por paradigma (p. ej. E1: retener si mediana < 200 ms o p10 < 170 ms); ISI aleatorio ya existe (1.2–7 s) ⇒ 190 ms constante es implausible.
- **Mitigación**: detector `ImplausibleSpeed` con `COVERAGE` declarada; retener, no acusar.

## X3 — No-respondedor en la escalera de calibración (`REPO_VERIFIED` + sim)
- **Exploit**: no tocar nada durante E1 de calibración.
- **Ruta**: ACC ≈ 0.79 (CR-dominada) ⇒ `initialDifficulty` PL 0.20 (¡sube!); R15 devuelve `NO_RESPONSES` sólo en el probe.
- **Consecuencia**: dificultad inicial mayor que la de un usuario que responde mal; umbral de lapso = 500 (sin hits).
- **Científica**: la exactitud de una tarea de señal rara no es capacidad.
- **Severidad**: P2 (3·3·95 %·S).
- **Mitigación**: escalera sobre HIT rate (no ACCURACY) o sobre d′; calibración gated por `NO_RESPONSES`.

## X4 — Quema del suministro de formas (`REPO_VERIFIED`)
- **Exploit** (o accidente): con flags embarcados, pulsar «Empezar mi base» 5 veces.
- **Ruta**: cada intento `nextProbe` avanza el cursor; denegación no libera (`releaseProbe` sólo en abort); carril de base no lee `repeatsEarlierForm`.
- **Consecuencia**: 5.ª administración repite la forma 1001 sin declararlo; umbrales churn por intento; con ≥1 intento denegado-completado el primer retest con puerta de fiabilidad abierta ya dibuja forma repetida (cursor desplazado, `priorAdministrations` no) ⇒ fiabilidad retenida en todos los retests posteriores del usuario.
- **Científica**: la primera base «admitida» (cuando el flag se encienda) puede ser una forma ya vista ⇒ práctica de ítem en la base.
- **Severidad**: P1 (4·5·100 %·S). Ocurre a **todo** usuario con flags embarcados.
- **Mitigación**: `releaseProbe` en denegación por sensor/no-respuesta; no consumir forma hasta bloque completado; declarar repetición en base.

## X5 — Cambio de dispositivo/entrada (S9)
- **Exploit** (o accidente): calibrar en tablet táctil, retest en desktop con ratón (o al revés); o pantalla 60 Hz vs 120 Hz.
- **Ruta**: `deviceFingerprint = null`; sin `deviceClass`; RT clase D (+30–110 ms de sesgo entre plataformas).
- **Consecuencia**: PS ±20; Trends «Empeorando/Mejorando»; what-if RCI «fiable».
- **Severidad**: P1 (5·4·95 %·S).
- **Mitigación**: persistir `deviceClass`, `inputKind`, `refreshHz`, `platform`; comparar sólo intra-clase; exactitud bajo deadline como oficial.

## X6 — Ralentización estratégica en E4 (stop-signal) (`REPO_VERIFIED` + `INFERRED`)
- **Exploit**: esperar ~600 ms antes de responder en todos los go ⇒ casi todos los stop se «inhiben».
- **Ruta**: `StopSignalTracker` 1-up/1-down sube la SSD; SSRT = RT_go − SSD; con RT_go inflada, SSRT ≈ constante; `presentStopTrial` registra la SSD pedida como usada (efectiva sesgada al alza en expectativa); el único gate es la banda post-hoc p(respond·señal) 0.25–0.75 sobre `confident` (`SsrtCalculator.kt:54`), que caza la saturación de la escalera pero no el slowing intra-bloque mientras la escalera sigue en p≈0.5; `WaitingStrategyDetector` existe en `core/antigaming` y no está cableado a Enfoque (`FocusAntiGaming.kt:62`; `telemetryFrom` nunca rellena `stopRespondRate`). Verbruggen 2019 recomienda excluir p(respond·stop) fuera de 0.25–0.75 y RT_go creciente.
- **Consecuencia**: p(stop) alto, SSRT no interpretable, PS puede subir (acc alta).
- **Severidad**: P2 (3·3·70 %·M). A verificar con sim de E4 (no reimplementado).
- **Mitigación**: gate de integración de carrera (p(respond|stop) en banda, RT_go no creciente), SSRT sólo con ≥30 stops (`MinSampleGate.SSRT` existe).

## X7 — Un solo toque por bloque en E9 (MOT) (`REPO_VERIFIED`, `MB-FOCUS-0137/0138`)
- **Exploit**: responder una sola vez en todo el bloque.
- **Ruta**: evita `NO_RESPONSES`; probedIsTarget ~ p; ACC ≈ max(p, 1−p); PL máximo hace más rentable no responder; PROBE_START 0.82 ⇒ RT ≥ 1476 ⇒ lapseRate 1.0 ⇒ SFI 0 (para todos).
- **Consecuencia**: exactitud sobre techo hold-up; el motor **sube** PL (`MB-FOCUS-0138`).
- **Severidad**: P2 (pinneado como limitación; no oficial porque E9 no es sonda).
- **Mitigación**: E9 fuera de la medida hasta rediseño (respuesta forzada por ensayo, d′).

## X8 — Sesión de 5 min como evidencia (`REPO_VERIFIED`)
- **Exploit** (accidente): elegir siempre la plantilla de 5 min (55 ensayos).
- **Ruta**: 55 no cruza pisos de RT (110 necesarios); ACCURACY sí cruza 20 ⇒ `Trends`/`TransferReport` sólo con ACCURACY; RT retenido ⇒ el usuario ve «sin datos» en la mitad de las métricas y `OvertrainingGuard` decide con ACCURACY.
- **Consecuencia**: no fabrica, pero **sesga**: usuarios de 5 min nunca tendrán tendencia de RT; un cambio de plantilla cambia qué métricas existen ⇒ comparación entre plantillas es comparación entre instrumentos.
- **Severidad**: P2 (3·4·90 %·S).
- **Mitigación**: `plannedTrials` real por fila; dosis mínima para oficial; etiquetar plantilla.

## X9 — Repetir el probe el mismo día (`REPO_VERIFIED`)
- **Exploit**: administrar las 4 formas de la sonda en una hora.
- **Ruta**: sin intervalo mínimo (charter: 48 h); `MIN_ADMINISTRATIONS 3` + `formIsFresh` ⇒ la 4.ª administración del día ya es «fiable» (si `ReliabilitySource` se poblara).
- **Consecuencia**: test-retest intra-día = práctica pura + estado del día.
- **Severidad**: P2 (hoy inerte por EMPTY; se arma al poblar).
- **Mitigación**: `EvidenceQuantityMaturity` (48 h span-floor) aplicado a probes; máximo 1 sonda/48 h.

## X10 — Sobre-selección en E2/E5 por sesgo de lado (`INFERRED`)
- **Exploit**: responder siempre LEFT.
- **Ruta**: `classifyChoice`; con 50/50 de lados ⇒ ACC .5 ⇒ BlindStrategyCeiling (techo .5) ⇒ INVALIDATE ✔ (R15, `max(p,1−p)` sobre proporción servida).
- **Consecuencia**: **sobrevive** — el detector cubre este caso.
- **Severidad**: — (verificar que la proporción de lados se sirve 50/50 con cuota: `conditionSchedule` reparte congruencia, no lado — a verificar).

## X11 — Persistencia como vector: `withdrawn` → 0.0 (`REPO_VERIFIED`)
- **No es exploit de usuario, es de sistema**: una sesión honesta con RT retenido por muestra se persiste con `value 0.0`; cualquier consumidor futuro que lea `SessionResult` histórico (Trends ya lo hace) ve un 0 real.
- **Consecuencia**: `Trends` de RT_MEDIAN con un 0 en la serie ⇒ pendiente absurda; SFI 0.
- **Severidad**: P1 (5·3·95 %·S).
- **Mitigación**: `MetricValueDto.value: Double?` + `absenceReason`; migración aditiva (`ignoreUnknownKeys`).

## X12 — Trends con k sesiones del mismo día
- **Ruta**: `Trends` toma las últimas k `sessionMetrics` sin exigir días distintos.
- **Consecuencia**: 3 sesiones en una tarde con fatiga ⇒ «Empeorando»; 3 con calentamiento ⇒ «Mejorando».
- **Severidad**: P2. **Mitigación**: 1 punto por día (mediana), k días distintos, sin caption direccional sin SE (`MB-FOCUS-0089`).

## Resumen

| Sobrevive al red-team | No sobrevive |
|---|---|
| BlindStrategyCeiling (S2, S4, X10), D3, D4 (S5), `NO_RESPONSES`, MIN_TRIALS 12 (S14), `null ≠ 0` en memoria, `formIsFresh` en evaluación, `readCursor` tri-estado, cifrado en reposo | Calibración sin gate (X1, X3, X12-sim), plausibilidad de RT (X2), quema de formas (X4), dispositivo (X5), E4 slowing (X6, a verificar), E9 (X7), plantilla (X8), intervalo (X9), persistencia de ausencia (X11), Trends intra-día (X12) |

Acciones primero: X4 (afecta a todos hoy), X1/X3 (calibración gated), X11 (persistencia), X5 (deviceClass).

---

# PARTE XXVII — Validation Roadmap

Orden obligatorio: no se puede validar fiabilidad sin instrumento estable; no se puede validar cambio sin fiabilidad; no se puede validar transferencia sin cambio. Cada etapa nombra la evidencia que la cierra y el umbral mínimo defendible (`RECOMMENDATION`; umbrales `SCIENTIFICALLY_SUPPORTED` donde se cita).

| Etapa | Pregunta | Diseño mínimo | Evidencia de cierre | Umbral | Bloqueado por |
|---|---|---|---|---|---|
| V0 Instrumento | ¿Se puede administrar la sonda y obtener una fila oficial en producción? | 20 usuarios internos, dispositivos reales, flag lifecycle ON | ≥90 % administraciones `Admitted`; distribución de `NotMeasurable` por causa | 0 usuarios en bucle NEEDS_BASELINE | P0 (PARTE I), telemetría (0 eventos) |
| V1 Timing | ¿Cuál es la clase de timing real por plataforma? | Protocolo físico `S5_PASO6` (cámara 240 fps, N=30 por plataforma) | dispersión de D por plataforma; sesgo tap-UP vs DOWN | sd(D) < 15 ms para clase C; declarar D si no | infra `core/timing` sin cablear a focus v1 |
| V2 Fiabilidad | ¿Test-retest de cada métrica oficial a 48 h–7 d? | N≥60, 2 administraciones, formas alternas, misma `deviceClass` | ICC(2,1) por métrica y sonda; SEM; efecto de práctica medio | ICC ≥ .70 para oficial; ≥ .80 para RCI individual (Nunnally) | V0, V1, `deviceClass`, `repeatsEarlierForm` en ambos carriles |
| V3 Formas | ¿Las 4 formas son equivalentes? | Diseño contrabalanceado, N≥40/forma | medias/SD por forma; DIF por forma | Δ media < 0.2 SD | V2 |
| V4 Validez de constructo | ¿Las sondas miden vigilancia/inhibición/filtrado y no velocidad? | Batería de referencia (PVT 10 min, SART, flanker de referencia) en N≥60 | matriz multirasgo-multimétodo; r con RT simple | r(sonda, referencia) > r(sonda, RT simple) | V2 |
| V5 Invariancia | ¿Mide lo mismo por edad/dispositivo/visión del color? | Subgrupos ≥30 | DIF / MI configural-métrica | sin DIF grande (ΔR² < .035) | V4 |
| V6 Sensibilidad al cambio | ¿El RCI detecta cambio real y no práctica? | Grupo control activo (otra tarea) vs entrenamiento, 4 semanas | RCI con término de práctica del control; % «cambio fiable» en control (falsos positivos) | FP en control ≤ 10 % | V2, V3 |
| V7 Dosis-respuesta | ¿Cuánta dosis mueve la sonda? | 3 brazos de dosis | pendiente dosis-Δ | Δ > SEM en el brazo alto | V6 |
| V8 Transferencia cercana | ¿Mejora en tareas no entrenadas de la misma familia? | Probe de transferencia (paradigma no entrenado) pre/post | d de Cohen con IC | d > 0.2 con IC excluyendo 0 | V6, capa 14 |
| V9 Ecológica | ¿Correlaciona con autoinforme/rendimiento diario? | Diario opt-in | r | informativo, no claim | V8 |

Sin V0–V2 **ninguna** afirmación de progreso individual es defendible; sin V6, ninguna afirmación de entrenamiento. Hoy: V0 no ejecutado (2 corridas en emulador, `MB-FOCUS-0165`), V1–V9 `UNKNOWN`.

---

# PARTE XXVIII — Gap Analysis

Severidad: P0 bloquea el producto; P1 bloquea medida oficial; P2 bloquea validación; P3 deuda. Stage: NOW / BEFORE_OFFICIAL_MEASUREMENT / BEFORE_VALIDATION / FUTURE.

| Current | Target | Gap | Severity | Evidence | Recommendation | Stage |
|---|---|---|---|---|---|---|
| Sesión de entrenamiento inalcanzable con flags embarcados (bucle de base) | Todo usuario llega a entrenar; la base es opcional o admisible | Gate de lifecycle exige un sensor apagado por flag; Start oculto sin base | **P0** | `FocusHomeScreen.kt:158-175`, `RememberFocusStateHolders.kt:86-90`, `SessionQualityGate.kt:55-64` | Desacoplar entrenamiento de base; sensor ON o gate relajado para base; `releaseProbe` en denegación | NOW |
| Sonda = tarea entrenada (E1/E2/E3 con otras semillas) | ≥2 paradigmas por CORE, sonda ≠ entrenamiento | Sin control de práctica de tarea | P1 | `ProbeBank`, `ProbeExerciseMapping` | Capa 14; antisacada-like/CPT-d′ como sondas | BEFORE_OFFICIAL |
| E1 declara PVT y es tarea de señal rara con techo ciego .79 | Paradigma declarado = paradigma implementado | Métricas de PVT (lapsos, slope) sobre una tarea que no es PVT | P1 | `E1VigiaGenerator.kt:30-75` | Relabel (GO_NO_GO_RARE) **o** rediseño a PVT real (rareza 1.0, sin señuelos) — decisión D-3 | BEFORE_OFFICIAL |
| Métricas base incondicionales para 13 paradigmas | Allow-list por familia (Blueprint V3 §4) | Código contradice el diseño | P1 | `DefaultMetricsEngine.kt:70-114`, Blueprint V3 §4 | `MetricPolicy(paradigm) → allowed` en capa 4 | BEFORE_OFFICIAL |
| `sessionMetrics` sin SFI/BLOCK_STABILITY ⇒ FocusScore nunca confident; chip FOCO oculto (flag OFF) o «—» (flag ON); tendencia SFI nunca confident | Superficies que se pueden poblar o se retiran | Superficie muerta | P1 (UI honesta) / P3 (código) | `DefaultMetricsEngine.kt:123-152`, `FocusScore.kt:38-62` | Retirar chip/score hasta capa 10, o promover SFI a sesión con n | NOW (retirar) |
| Calibración sin gate; umbrales pooled 3 paradigmas; unidireccional; persisten en denegación | Umbrales por paradigma desde bloques admitidos; bidireccional; no persistir sin admisión | Sandbag y churn | P1 | `FocusBaselineStateHolder.kt:100-227`, `DefaultCalibrationEngine.kt:27-36` | Capa 7/9 (PARTE XVII §17.4) | BEFORE_OFFICIAL |
| Pre/post con umbrales distintos | Umbral fijado por administración y persistido con la fila | LAPSES no comparable | P1 | S3 sim | Persistir `thresholdsUsed` por fila | BEFORE_OFFICIAL |
| Base de 1 administración | `PROVISIONAL` hasta ≥5 sesiones/3 días/48 h | Sobre-reacción | P1 | charter §11.1 vs código | `BaselineMaturity` desde `EvidenceQuantityMaturity` | BEFORE_OFFICIAL |
| Snapshot sin n/SD | `{value, n, se}` | Sin incertidumbre | P1 | `BaselineSnapshotDto` | Bootstrap intra-bloque (capa 15) | BEFORE_OFFICIAL |
| `deviceFingerprint` null; sin `deviceClass`/`inputKind`/`refreshHz` | Por fila | Comparaciones entre dispositivos | P1 | `:209`; S9 | `DeviceContext` obligatorio | BEFORE_OFFICIAL |
| RT clase D (tap UP, onset pre-await) | Clase C (frame) para RT oficial; o exactitud bajo deadline | Sesgo +30–110 ms, no comparable | P1 | `FocusSessionStateHolder.kt:266`, `detectTapGestures` | Cablear `core/timing`; onset en frame commit; pointer DOWN; o métrica oficial de exactitud | BEFORE_OFFICIAL |
| `withdrawn` → 0.0 en persistencia (el 0.0 lo escribe `MetricValue.unavailable`, `MetricKey.kt:88`, para TODA ausencia: NOT_ADMITTED, INTERRUPTED, INSUFFICIENT_SAMPLE, NO_RESPONSES) | Ausencia tipada persistida | Se persiste `confident=false` y la admisión de sesión, así que un lector puede distinguir; lo que se pierde en disco es `absence` y `measurementSufficient`. Deuda ya declarada (aprendizaje 2026-07-15: `value: Double?` follow-up abierto). K6 3/3 | P2 | `FocusSessionAssembler.kt:248-250`, `MetricKey.kt:88`, `FocusPersistenceMappers.kt:87-88` | `value: Double?` + reason | BEFORE_OFFICIAL |
| `plannedTrials`/`completedTrials` = 1 (en el `SessionRecord` de core; el DTO de Focus lleva `trialCount` real) | Reales | Dosis de core no auditable | P3 | `FocusSessionStateHolder.kt:340/:476` | Poblar desde bloque | BEFORE_OFFICIAL |
| Sin forma/versión por fila (12/13) | `FormDescriptorV1` para todos | No reproducible | P2 | `SessionRecorder` E2 | Extender a 13 | BEFORE_VALIDATION |
| PS mezcla SAT; premia ~340 ms | Score de capacidad con banda | PS ≠ Enfoque | P1 (si se muestra) | SAT P1–P8 | PS = feedback de sesión; estimador (capa 8) | BEFORE_OFFICIAL |
| Sin estimador de subhabilidad | θ por subhabilidad con SE | Nada estima capacidad | P1 | — | 1PL/2PL sobre exactitud bajo deadline; o SDT d′ por bloque con IC | BEFORE_OFFICIAL |
| RCI NED siempre; sin práctica; sin SD | RCI con SEM medido + práctica | Sin crecimiento honesto | P1 | `ReliabilitySource.EMPTY` | V2 primero; Chelune después | BEFORE_VALIDATION |
| Trends direccionales sin SE, intra-día | Dirección + confianza; 1/día | Fabrica tendencia | P2 | `MB-FOCUS-0089`, X12 | Capa 11 | NOW (copy) / BEFORE_OFFICIAL |
| Motor adaptativo no-op / ratchet | Staircase con objetivo de exactitud, StepDown, versión | 11/13 sin adaptación | P2 | PARTE XIV | Capa 6 | BEFORE_OFFICIAL |
| Escalera inicial sobre ACCURACY CR-dominada | Sobre HIT/d′ | Infla dificultad | P2 | X3 | idem | BEFORE_OFFICIAL |
| Personalización L0/L1–L3; sin `UserGoal`; sin prescripción | L5 mínimo (prescripción por perfil) | Cadena §62 rota en el primer eslabón | P1 | PARTE XX | Capa 12 | BEFORE_OFFICIAL |
| Dosis sin justificación; 55 no cruza pisos; sin intervalo | Dosis de medida derivada del piso; ≥48 h entre sondas | Sonda subpotente | P1 | `MB-FOCUS-0092/0130` | 110 ensayos o `interference` 0.75 (E2); capa 13 | BEFORE_OFFICIAL |
| 0 eventos de analítica | Eventos por sesión/bloque/admisión/sonda | Ciego en producción | P1 | `MindBoostAnalytics` | 8 eventos PII-free | NOW |
| 3 DTOs comparten `FocusSchema` | Versión por DTO + migración | Un bump borra todo | P2 | `MB-FOCUS-0157` | Split | BEFORE_VALIDATION |
| Sin `SessionMode` | OFFICIAL/TRAINING/RELAXED por fila | Variante relajada contaminaría | P1 (antes de variante) | PARTE XXII | Capa 2/3 | BEFORE_OFFICIAL |
| Estímulos sin nodo semántico (decisión) | Alternativa accesible o declaración | Limitación declarada | P3 | `MB-FOCUS-0110` | Mantener; documentar en copy | FUTURE |
| E13 PRESSURE como ejercicio; E12 duplica E1 | PRESSURE = eje; E12 = variante | Falsa variedad | P2 | PARTE VII/IX | Retirar E13 del catálogo; E12 → variante | BEFORE_OFFICIAL |
| E9 métricas sin sentido (RT ≥1476, SFI 0), E10 RSVP 80 ms frame-bound | Rediseño o exclusión de medida | Métricas fabricadas | P2 | PARTE XII | Excluir de oficial; d′ por bloque | BEFORE_OFFICIAL |
| `INHIBITORY_CONTROL_INDEX`, `NEAR_TRANSFER_INDEX` sin productor (`FocusScore.kt:58` consume una clave que nada produce); `MinSampleGate.RCI nMin=2` sin consumidor (piso vivo = `MIN_ADMINISTRATIONS = 3`). `DUAL_TASK_COST` SÍ se produce (acc(single) − acc(dual)); su defecto es la secundaria no puntuada (`MB-FOCUS-0141`), no «≡ 0» (K8 refuta esa frase) | Sin claves huérfanas | Etiquetas sin dato | P3 | `MetricKey`, `MinSampleGate.kt:56` | Retirar o producir | BEFORE_VALIDATION |
| Documentación MindBoost (nombre) | Molvex | Migración doc | P3 | CEO-OVERRIDE-001 | Lista de docs a migrar (PARTE XXXI D-0) | FUTURE |

---

# PARTE XXIX — Molvex Focus Engine vNext

Principios: (a) todo determinista (CEO-OVERRIDE-004); (b) una sola taxonomía, una sola fila por administración con `SessionMode`; (c) medida ≠ entrenamiento; (d) toda estimación con banda; (e) versión por capa; (f) ausencia tipada de extremo a extremo. Cada capa: **Propósito · Entradas · Salidas · Estado · Versionado · Modos de fallo · Hoy · Hueco · Recomendación**.

## L1 Construct Registry
- **Propósito**: única fuente de verdad de subhabilidades (PARTE VI) y de qué paradigmas las miden/entrenan.
- **Entradas**: taxonomía §6.2. **Salidas**: `Subskill { id, class(CORE/SECONDARY/CROSS), measureParadigms, trainParadigms, officialMetric, invariantsRequired }`.
- **Estado**: estático, versionado. **Versionado**: `constructRegistryVersion`.
- **Fallos**: subhabilidad con 1 solo paradigma de medida (dominio por ejercicio); paradigma en ambas listas sin declarar.
- **Hoy**: `Capacity` (9) + `CognitiveModule` (6, sin lector) + `Paradigm` (14) + `ExerciseCatalog.capacity`; sin clase, sin métrica oficial declarada.
- **Hueco**: no existe registro; el mapeo vive en el catálogo de ejercicios.
- **Recomendación**: `ConstructRegistry` en `focus/domain/construct`; test que exige ≥2 `measureParadigms` por CORE y disjunción medida/entrenamiento por sonda.

## L2 Exercise / Family Registry
- **Propósito**: ejercicios como instancias de familias con `VariantSpec` (relajado, presión) y contrato de forma.
- **Entradas**: L1. **Salidas**: `ExerciseDefinition { id, family, paradigm, subskillsTrained, variants, formContract, timingClassRequired, officialEligible }`.
- **Estado**: estático. **Versionado**: `generatorVersion` por generador (existe), `catalogVersion`.
- **Fallos**: ejercicio que declara paradigma que no implementa (E1); skins como ejercicios (E12/E13).
- **Hoy**: `ExerciseCatalog` 13, `FocusExerciseGenerators.registry`, `FormDescriptorV1` (E2), `FocusFormContract`.
- **Hueco**: sin `VariantSpec`, sin `timingClassRequired`, sin `officialEligible`; paradigma declarado no verificado contra generador.
- **Recomendación**: test `everyExerciseImplementsItsDeclaredParadigm` (rareza, señuelos, respuesta forzada) — espejo de `everyProbeRendersTheParadigmItDeclares`.

## L3 Trial Evidence Layer
- **Propósito**: registro por ensayo en memoria (nunca persistido crudo) con todo lo que la clasificación y la calidad necesitan.
- **Entradas**: runtime. **Salidas**: `TrialEvidence { index, onsetFrameNs, responseDownNs, responseUpNs, key, condition, ssd, timingClass }`.
- **Estado**: efímero. **Versionado**: `trialSchemaVersion`.
- **Fallos**: onset antes del frame; UP en vez de DOWN; `timestampMs` = fin de bloque (hoy).
- **Hoy**: `RecordedTrial(stimulus, latencyMs, responseKey, ssd)`; `TrialResult.timestampMs` = endedAtMs.
- **Hueco**: sin timebase de frame; sin DOWN; sin clase de timing por ensayo.
- **Recomendación**: cablear `FrameTimingProbe`/`WindowedTemporalProbe` de `core/timing`; onset en `withFrameNanos`; `PointerEventType.Press`.

## L4 Metric Layer
- **Propósito**: métricas por bloque y sesión con `MetricPolicy(paradigm)` allow-list, n por celda, ausencia tipada, SE bootstrap.
- **Entradas**: L3, `thresholdsUsed`. **Salidas**: `MetricValue { value: Double?, n, se, absence?, sufficient? }`.
- **Estado**: efímero → persistido. **Versionado**: `metricVersion` por clave.
- **Fallos**: métrica de RT sin celda; SFI sobre 11 hits; ACC de señal rara como capacidad.
- **Hoy**: `DefaultMetricsEngine` (26 claves, base incondicional, `sessionMetrics` 6 claves); `MeasurementSufficiency`; `withheld` → 0.0 en disco.
- **Hueco**: allow-list (Blueprint V3 §4), SE, `Double?` en DTO, SFI/BLOCK_STABILITY a sesión con n, d′/criterio para tareas de señal.
- **Recomendación**: `MetricPolicy` + bootstrap intra-bloque + DTO nullable; retirar claves huérfanas.

## L5 Difficulty Model
- **Propósito**: dificultad como parámetro de ítem (b) separado de capacidad (θ); ejes con rango efectivo declarado.
- **Entradas**: `DifficultyVector`. **Salidas**: `ItemDifficulty(b)` por forma, mapeo eje→b calibrado.
- **Estado**: estático + tabla calibrada. **Versionado**: `difficultyModelVersion`.
- **Fallos**: eje inerte (E1 TIME_ON_TASK); escalera sobre CR-accuracy; reset de TU.
- **Hoy**: 9 ejes, lerps fijos, `initialDifficulty` por bins de ACCURACY.
- **Hueco**: sin b; sin curva psicométrica; sin rango efectivo por ejercicio.
- **Recomendación**: pinnear rango efectivo por ejercicio (test); escalera sobre HIT/d′; 1PL cuando haya datos (V2).

## L6 Training Adaptation Layer
- **Propósito**: política de selección de dificultad **para entrenar** (objetivo ~80 % acierto), desacoplada de la medida; StepDown garantizado; sin ratchet.
- **Entradas**: L4 (bloque), L5, historial. **Salidas**: `AdaptationDecision` con razón y versión, persistida.
- **Estado**: por usuario × ejercicio. **Versionado**: `adaptiveConfigVersion`.
- **Fallos**: subir por no responder; ratchet; decisión persistida sin medida.
- **Hoy**: `DefaultAdaptiveEngine` (Hold/ContainImpulsivity alcanzables), `AdaptiveModeGate` muerto, `baselineCv` nunca llega.
- **Hueco**: StepUp/Down inalcanzables; señales no pobladas; sin versión.
- **Recomendación**: staircase 3-down/1-up sobre HIT con deadline; `SESSION_NOT_MEASURABLE ⇒ no persistir`; test «más errores nunca suben».

## L7 Measurement Eligibility Layer
- **Propósito**: un solo `admit` para **todo** bloque (calibración, sonda, entrenamiento) con causas tipadas, coberturas declaradas, plausibilidad de RT, dosis mínima, `SessionMode`.
- **Entradas**: L3, sensores (lifecycle, timing), `DeviceContext`. **Salidas**: `Admission { Admitted(timingClass) | NotMeasurable(reason) }`.
- **Estado**: por fila. **Versionado**: `eligibilityPolicyVersion`.
- **Fallos**: gate sólo en sonda; sensor apagado por flag = denegación universal; jerarquía que enmascara.
- **Hoy**: `SessionQualityGate` (INTERRUPTED > NO_RESPONSES > RESPONSE_PATTERN > LIFECYCLE), `FocusAntiGaming` (D3, D4, Blind; COVERAGE PARTIAL), `AppLifecycleProbe` (flag OFF).
- **Hueco**: calibración sin gate; sin `ImplausibleSpeed`; sin dosis mínima; sin `deviceClass`; sin timing.
- **Recomendación**: aplicar a calibración; `NotObservable` del sensor ⇒ `Admitted(timingClass=D, lifecycle=UNOBSERVED)` **para entrenamiento** y denegación **sólo para oficial**; `releaseProbe` en denegación.

## L8 Subskill Estimator
- **Propósito**: θ por subhabilidad con SE, actualizado sólo por administraciones oficiales; prior poblacional débil; nunca de un solo ejercicio.
- **Entradas**: L4 oficiales, L5. **Salidas**: `SubskillEstimate { theta, se, nAdmin, lastOfficialAt, maturity }`.
- **Estado**: por usuario. **Versionado**: `estimatorVersion`.
- **Fallos**: estimación desde 1 sonda de 55; PS como θ; mezclar modos.
- **Hoy**: **inexistente** (PS, FocusScore y SFI no son estimadores).
- **Hueco**: todo.
- **Recomendación**: fase 1 = SDT (d′, criterio) por sonda con IC bootstrap y `maturity`; fase 2 = 1PL con b calibrado (V2+). Prior = media poblacional con SE grande; shrinkage explícito (CEO-OVERRIDE-002).

## L9 Focus Profile
- **Propósito**: agregado de L8 por subhabilidad con `maturity` y comparabilidad (`deviceClass`, versión).
- **Entradas**: L8. **Salidas**: `FocusProfile { subskills, baselineMaturity, thresholds(paradigm), deviceClass, versions }`.
- **Estado**: por usuario. **Versionado**: `profileSchemaVersion` (separado de sesión/probe).
- **Fallos**: dos dueños de base; umbrales pooled; sin staleness.
- **Hoy**: `FocusProfile { baseline, thresholds, startingDifficulty }` + `probeHistory`.
- **Hueco**: maturity, per-paradigm thresholds, deviceClass, staleness, re-baseline.
- **Recomendación**: PARTE XVII §17.4.

## L10 Performance Score Presentation
- **Propósito**: lo que ve el usuario tras una sesión: rendimiento **de la sesión** (PS) etiquetado como tal; nunca «tu Enfoque».
- **Entradas**: L4 sesión, `SessionMode`. **Salidas**: `SessionFeedback { ps?, band, mode, whatItMeans }`.
- **Estado**: efímero. **Versionado**: `psVersion`.
- **Fallos**: PS como capacidad; entero sin banda; premiar SAT.
- **Hoy**: PS/FocusScore/SFI detrás de flag OFF; chip FOCO oculto (o «—» si el flag se enciende).
- **Hueco**: copy, banda, separación de L8.
- **Recomendación**: PS con `±sd` (bootstrap) y copy «rendimiento de esta sesión»; retirar chip hasta L8.

## L11 Growth / Reliable Change Layer
- **Propósito**: cambio fiable por subhabilidad entre administraciones oficiales con `formIsFresh`, misma `deviceClass`, ≥48 h, término de práctica.
- **Entradas**: L8 (dos estimaciones con SE), fiabilidad (V2). **Salidas**: `ChangeVerdict { RELIABLE_UP | RELIABLE_DOWN | NO_EVIDENCE | NOT_COMPARABLE(reason) }`.
- **Estado**: derivado. **Versionado**: `growthVersion`.
- **Fallos**: Trends direccionales; RCI sin práctica; intra-día.
- **Hoy**: `Trends`, `TransferReportBuilder`, `ReliableChangeIndex` (NED).
- **Hueco**: SEM, práctica, comparabilidad, copy.
- **Recomendación**: hasta V2, publicar sólo `NO_EVIDENCE`/`NOT_COMPARABLE` con causa; SEdiff mínimo desde bootstrap como cota.

## L12 Personalization Policy
- **Propósito**: prescripción determinista: `UserGoal` × perfil × respuesta × preferencias × contexto → subhabilidad prioritaria → familia → parámetros; con robustez poblacional (prior, shrinkage, mínimos de evidencia).
- **Entradas**: L9, L11, `UserGoal`, historial de exposición, `SessionMode`. **Salidas**: `Prescription { subskill, family, exercise, difficulty, dose, why, evidenceLevel }`.
- **Estado**: por usuario. **Versionado**: `policyVersion`.
- **Fallos**: sobre-reacción a 1 sesión; recomendar por rotación; arquetipos no validados.
- **Hoy**: `DailyPlanGenerator` (rotación/recency), `ArchetypeInference` (no puede promocionar), recomendación E1 por defecto.
- **Hueco**: `UserGoal`, prescripción por perfil, `why`, mínimos de evidencia.
- **Recomendación**: política por reglas explícitas y testeables (tabla), con `evidenceLevel` en la salida; L7 nivel (PARTE XX) alcanzable = L5 en 1 fase.

## L13 Dose / Scheduling Layer
- **Propósito**: dosis de entrenamiento (objetivo de acierto, duración) separada de dosis de medida (derivada de pisos); intervalo entre sondas; fatiga con bloques ≥10 min; covariables opt-in.
- **Entradas**: L12, `MinSampleGate`, tasa de señal, `subjectiveState?`. **Salidas**: `DosePlan { template, trials, probeDue?, minIntervalOk }`.
- **Estado**: por usuario. **Versionado**: `doseVersion`.
- **Fallos**: 55 como sonda; 4 formas en 10 min; OvertrainingGuard con ACCURACY pooled.
- **Hoy**: `SessionTemplate`, `RecoveryPolicy`, `OvertrainingGuard`, `TrainingCycle`, `MicroPauseGate` (OFF).
- **Hueco**: dosis de medida derivada; intervalo 48 h; `plannedTrials` real.
- **Recomendación**: PARTE XXI §21.3; `EvidenceQuantityMaturity` para sondas.

## L14 Transfer Probe Layer
- **Propósito**: por CORE, sonda con paradigma **no entrenado**, semillas reservadas, administrada cada N sesiones; base de la única claim de transferencia (cercana).
- **Entradas**: L1, L2. **Salidas**: `TransferProbe { subskill, paradigm ∉ trained, forms(≥4), dose ≥ reliability }`.
- **Estado**: estático + cursor. **Versionado**: `transferProbeVersion`.
- **Fallos**: sonda = tarea entrenada (hoy); `NEAR_TRANSFER_INDEX` sin productor.
- **Hoy**: `ProbeBank` 3 sondas, todas paradigma entrenado; `ProbeExerciseMapping` fail-closed para no entrenados (R11).
- **Hueco**: renderizadores de sonda no entrenados (antisacada-like, CPT-d′); `ExerciseCatalogConsistencyTest` fuerza `implemented == wired` ⇒ una sonda renderizable exige ejercicio entrenado — **decisión de arquitectura** (D-6).
- **Recomendación**: `ProbeRenderer` separado de `ExerciseRenderer`; claim máximo «tareas de [familia] no entrenadas».

## L15 Uncertainty / Coverage Layer
- **Propósito**: SE en cada nivel; cobertura de sensores/detectores declarada; ausencia tipada de extremo a extremo.
- **Entradas**: todas. **Salidas**: `Uncertainty { se, method, coverage }` adjunto a cada valor.
- **Estado**: derivado. **Versionado**: `uncertaintyVersion`.
- **Fallos**: booleanos de piso como «confianza»; margen heurístico; 0.0 en disco.
- **Hoy**: `confident`, `measurementSufficient`, `DetectorCoverage`, `margin` heurístico.
- **Hueco**: SE real; DTO nullable; cobertura de timing.
- **Recomendación**: bootstrap intra-bloque (coste S); `MetricValueDto.value: Double?`.

## L16 Versioning / Comparability Layer
- **Propósito**: cada fila lleva `{schemaVersion por DTO, generatorVersion, metricVersion, thresholdsUsed, adaptiveConfigVersion, eligibilityPolicyVersion, deviceClass, sessionMode}`; comparador rechaza filas no comparables con razón.
- **Entradas**: todas. **Salidas**: `Comparability { ok | NOT_COMPARABLE(reason) }`.
- **Fallos**: `FocusSchema` compartido; `scoringVersion` inerte; constantes sin versión.
- **Hoy**: `FocusSchema.CURRENT` ×3, `RngVersion`, `FormDescriptorV1` (E2), `scoringVersion` (sin consumidor).
- **Hueco**: casi todo.
- **Recomendación**: `RowProvenance` obligatorio en mappers (sin defaults); split de `FocusSchema`; test de comparabilidad.

## L17 Feedback Layer
- **Propósito**: copy honesta por estado: `NO_EVIDENCE` ≠ «estable»; rendimiento de sesión ≠ capacidad; repetición de forma declarada; límite del instrumento declarado (no atributivo).
- **Entradas**: L10, L11, L7. **Salidas**: strings pinneadas por gate de copy.
- **Fallos**: «Estabilidad» para RT_CV; captions direccionales; «tu base» con métricas que no comparan; disclaimers que nombran lexemas.
- **Hoy**: `FocusStrings`, `TransferReport.honestNote`, `REPEATED_FORM_CLAUSE`, `withheldForSample`, scanners.
- **Hueco**: copy de PS/Trends; «tu base» (`MB-FOCUS-0092/0130`); «Estabilidad».
- **Recomendación**: tabla estado→copy con test de emparejamiento (precedente `retestClaimText`).

## Orden de construcción (dependencias)

L7 (gate universal + release) → L3/L15 (timing + SE) → L4 (policy + DTO nullable) → L16 (provenance) → L9 (maturity/thresholds) → L8 (SDT) → L13 (dosis) → L6 (staircase) → L12 (prescripción) → L11 (RCI tras V2) → L14 (sondas de transferencia) → L10/L17 (superficies). L1/L2/L5 son registros y se escriben primero pero no bloquean código.

---

# PARTE XXX — Prioritized Roadmap

Formato por ítem: `Impacto 1–5 · Verosimilitud 1–5 · Confianza % · Fix S/M/L/XL · Dependencia`. Ningún ítem se implementa en esta auditoría (READ-ONLY).

### P0 / BEFORE NEXT EXERCISE DESIGN
1. **Desbloquear el entrenamiento** (`Impacto 5 · Veros. 5 · 100 % · S · —`): decidir D-2 (PARTE XXXI). Sin esto ningún usuario con flags embarcados entrena ni mide. `FocusHomeScreen.kt:158-175` + `RememberFocusStateHolders.kt:86-90`.
2. **`releaseProbe` en denegación por sensor/no-respuesta** (`4·5·100 %·S·—`): X4 quema 4 formas por usuario hoy. `FocusBaselineStateHolder.kt:138` (gasto) / `:163-169` (única liberación, sólo forma no vista) / `:215-224` (denegación). Consecuencia latente (K1): los intentos denegados-pero-completados desplazan el cursor sin mover `priorAdministrations` (sólo `Admitted`), así que el día que el sensor se encienda, todo usuario con ≥1 intento denegado tendrá su primer retest con puerta abierta sobre forma repetida y la fiabilidad retenida para siempre (`ProbeReliabilityEstimator.kt:44`).
3. **Retirar superficies muertas** (`3·5·100 %·S·—`): chip FOCO (oculto con flag OFF; «—» permanente en cuanto se encienda), `FocusScore` nunca confident, tendencia SFI siempre «Aún sin tendencia.», captions direccionales de `Trends` (`MB-FOCUS-0089`). Retirar, no arreglar, hasta L8/L10.
4. **Fijar la taxonomía (PARTE VI) y el registro de constructos (L1)** (`5·—·—·M·—`): CORE vigilancia/inhibición/filtrado; PRESSURE como eje; E12 como variante. Es la única salida de esta auditoría que el diseñador de ejercicios consume directamente.
5. **Decidir el paradigma de E1** (`5·5·100 %·S (relabel) / L (rediseño)·D-3`): PVT real o GO_NO_GO_RARE. Todo lo de vigilancia cuelga de esto.
6. **`MetricPolicy(paradigm)` allow-list** (`4·5·100 %·M·L1`): el código contradice Blueprint V3 §4; sin esto cada ejercicio nuevo hereda 26 claves.
7. **Persistir `SessionMode` + `thresholdsUsed` + `DeviceContext` + `Double?`** (`5·5·95 %·M·—`): 4 campos aditivos (`ignoreUnknownKeys`), sin bump de `FocusSchema`. Precondición de toda medida y de la variante relajada.
8. **8 eventos de analítica PII-free** (`4·5·100 %·S·—`): `session_started/finished`, `block_admitted/denied(reason)`, `probe_administered(form,probeId)`, `baseline_persisted(maturity)`, `adaptation_decision`, `template_selected`.

### P1 / BEFORE MEASUREMENT WIRING
9. **Gate universal (L7) sobre calibración** + umbrales por paradigma sólo desde bloques admitidos + `BaselineMaturity` (`5·4·95 %·M·1,7`): cierra X1, X3, S12, churn.
10. **Detector `ImplausibleSpeed`** con `COVERAGE` (`4·2·85 %·S·L7`): S15/X2.
11. **Timing clase C** para RT oficial: cablear `core/timing` a focus v1, onset en `withFrameNanos`, pointer DOWN (`5·5·90 %·M·V1`) — o D-9 (exactitud bajo deadline como oficial).
12. **Dosis de medida derivada de pisos** (`4·5·100 %·S·—`): E1 ≥110; E2 `interference` 0.75 o 98+; `plannedTrials` real; ≥48 h entre sondas (`EvidenceQuantityMaturity`).
13. **Bootstrap intra-bloque → SE por métrica** (`4·5·95 %·S·—`): base de L15/L8.
14. **Estimador SDT por sonda (L8 fase 1)**: d′/criterio con IC, `maturity` (`5·4·80 %·M·13`).
15. **Staircase de entrenamiento (L6)** sobre HIT con deadline, StepDown, sin ratchet, `SESSION_NOT_MEASURABLE ⇒ no persistir` (`4·5·95 %·M·6`).
16. **`UserGoal` + política de prescripción (L12)** por reglas explícitas con `evidenceLevel` (`5·—·—·M·L1,L9`).
17. **Copy honesta (L17)**: «Estabilidad»→«Variabilidad de RT», «tu base» sólo con comparador válido, `NO_EVIDENCE` ≠ «estable» (`3·5·100 %·S·—`).
18. **Split de `FocusSchema`** en 3 versiones + `RowProvenance` (`4·3·90 %·M·7`).
19. **E13 fuera del catálogo; E12 → variante de E1** (`3·—·—·S·4`).
20. **E9/E10 fuera de la medida oficial** hasta rediseño (respuesta forzada, d′) (`4·5·100 %·S·—`).

### P2 / BEFORE BETA VALIDATION
21. **V0 instrumento** (20 usuarios internos, dispositivos reales) (`5·—·—·M·1–8`).
22. **V1 timing físico** (`S5_PASO6`, cámara 240 fps) por plataforma (`4·—·—·M·11`).
23. **`ProbeRenderer` separado + 1 sonda no entrenada por CORE (L14)** — decisión D-6 (`5·—·—·L·L1,L2`).
24. **`FormDescriptorV1` para 13 ejercicios** (`3·4·90 %·M·18`).
25. **Gate de integración de carrera en E4** (p(respond|stop) 0.25–0.75, RT_go no creciente, SSRT ≥30 stops) (`3·3·70 %·M·—`). Verificar X6 con sim de E4 antes.
26. **`Trends` 1 punto/día, k días distintos** (`3·4·90 %·S·17`).
27. **Prior poblacional débil + shrinkage explícito** (CEO-OVERRIDE-002) sobre L8 (`4·—·—·M·14, V0`).
28. **Dosis de entrenamiento con objetivo de acierto** y fatiga sólo ≥10 min (`3·—·—·S·15`).

### P3 / LATER RESEARCH
29. V2 fiabilidad (N≥60) → RCI con SEM y término de práctica (Chelune) (`5·—·—·XL·21,22`).
30. V3 equivalencia de formas; V4 validez de constructo (MTMM vs PVT/SART/flanker); V5 invariancia.
31. V6 sensibilidad al cambio con control activo; V7 dosis-respuesta; V8 transferencia cercana.
32. 1PL/2PL con b calibrado (L5/L8 fase 2).
33. Nodo semántico alternativo para estímulos (`MB-FOCUS-0110`); fuente escalable; input motor adaptado.
34. Migración documental MindBoost → Molvex (D-0).
35. Coaching interpretativo con IA (Elite) — nunca autoridad sobre score/elegibilidad/perfil (CEO-OVERRIDE-004).

---

# PARTE XXXI — CEO Decisions Required

## CEO DECISION REQUIRED — D-0 Naming y migración documental
- **Current decision**: CEO-OVERRIDE-001 = Molvex. Código, ledger, docs y scanners dicen MindBoost/MindOS.
- **Problem**: 0 archivos del repo usan «Molvex» salvo `CLAUDE.md`/ruta del proyecto; el ledger, `docs/context_pack/*`, `FocusStrings`, `MindBoostHubScreen`, namespaces `mindboost_*`.
- **Evidence**: `REPO_VERIFIED` (grep).
- **Option A**: migración documental sólo (docs + copy de UI), código intacto.
- **Option B**: A + renombrar paquetes/namespaces (rompe persistencia: namespaces `mindboost_focus` son claves de cifrado en disco).
- **Option C**: no migrar; alias en docs.
- **Recommended**: A, por fases; namespaces de almacenamiento NUNCA (B).
- **Why**: los namespaces son identidad de datos cifrados; renombrarlos = borrar la base de todos.
- **Benefits/Costs/Risks/Reversibility**: coherencia de marca / S-M / ninguno si sólo docs / reversible.
- **What changes if approved**: lista de 9 docs `context_pack` + `FocusStrings` + ledger header.
- **If current remains**: fricción cognitiva en cada auditoría; sin riesgo técnico.
- **Confidence**: 95 %.

## CEO DECISION REQUIRED — D-1 Política de re-baseline y madurez (charter §11.1)
- **Current**: 1 administración de 55 ensayos = base permanente; no hay re-baseline; charter §11.1 exige ≥5 sesiones/3 días/48 h para «calibrado».
- **Problem**: base corruptible por 1 mal día (S11, X1); código y charter contradicen (`CONTRADICTED`).
- **Evidence**: `BaselineAssembler.kt:30-60`, `03_SCIENTIFIC_MEASUREMENT_CHARTER.md:438-449`.
- **A**: `BaselineMaturity {PROVISIONAL, CALIBRATED}` derivada de `EvidenceQuantityMaturity`; base = mediana de ≥3 administraciones admitidas en ≥48 h; hasta entonces sólo entrenamiento.
- **B**: mantener 1 administración pero permitir re-baseline manual cada 30 días.
- **C**: mantener tal cual.
- **Recommended**: A. **Why**: es lo que el charter ya dice; la mediana de 3 mata X1/S11 sin nuevo código de detección.
- **Costs**: 3 sondas antes de la primera comparación (≈15 min repartidos en ≥2 días). **Risks**: abandono si se percibe como examen — mitigar con entrenamiento desbloqueado desde el día 1 (D-2).
- **Reversibility**: alta (campo aditivo). **If current remains**: toda afirmación de cambio es sobre una base de N=1.
- **Confidence**: 90 %.

## CEO DECISION REQUIRED — D-2 Desbloquear el entrenamiento sin base admitida
- **Current**: Start oculto hasta `baseline != null`; base exige lifecycle sensor ON; flag OFF en producción.
- **Problem**: P0 — nadie entrena.
- **Evidence**: `FocusHomeScreen.kt:158-175`, `RememberFocusStateHolders.kt:86-90`, `AppLifecycleProbe.kt:72-76`.
- **A**: entrenamiento SIEMPRE disponible; base opcional y etiquetada `PROVISIONAL`; gate de lifecycle sólo para filas OFFICIAL.
- **B**: encender `focusLifecycleProbeEnabled` en producción (hoy sólo android_debug_app) y mantener el bloqueo.
- **C**: mantener.
- **Recommended**: A (+ B cuando el sensor esté verificado en dispositivo, `MB-FOCUS-0101`).
- **Why**: entrenar no necesita sensor; medir sí. B sola sigue exigiendo un sensor no verificado en iOS/desktop.
- **Costs**: S. **Risks**: sesiones de entrenamiento sin `SessionMode` contaminan — por eso ítem 7 va antes.
- **Reversibility**: alta. **If current remains**: producto inoperante.
- **Confidence**: 100 %.

## CEO DECISION REQUIRED — D-3 E1: relabel o rediseño a PVT
- **Current**: E1 declara `PVT`, implementa señal rara (6–30 %) con señuelos y ventana 950–1400 ms; métricas de PVT (lapsos, slope) aplicadas.
- **Problem**: paradigma declarado ≠ implementado; techo ciego .79; ACC no es capacidad.
- **Evidence**: `E1VigiaGenerator.kt:30-75`; `GO_NO_GO_PARADIGMS` incluye PVT.
- **A (relabel)**: `Paradigm.GO_NO_GO_RARE`; métricas de SDT (d′, criterio) + HIT RT; sin lapsos PVT.
- **B (rediseño)**: E1 = PVT real (rareza 1.0, ISI 2–10 s, sin señuelos, ≥5 min); mantener GO_NO_GO como E3.
- **C**: mantener.
- **Recommended**: **B para la sonda de vigilancia, A para el entrenamiento** (E1 entrena; una PVT de 5 min mide). Es la separación medida/entrenamiento de L14.
- **Why**: la PVT es el único instrumento de vigilancia con norma publicada (Basner & Dinges 2011); una PVT no se entrena bien (aburrida), pero mide bien.
- **Costs**: M (nuevo renderer de sonda). **Risks**: PVT en móvil con timing clase D — vincular a D-9.
- **Reversibility**: media. **If current remains**: «lapsos» sin significado; comparaciones con literatura imposibles.
- **Confidence**: 85 %.

## CEO DECISION REQUIRED — D-4 Allow-list de métricas por familia
- **Current**: 26 `MetricKey`, base incondicional para 13 paradigmas; Blueprint V3 §4 exige allow-list.
- **Problem**: `CONTRADICTED`; RSVP con RT_CV, MOT con lapsos, etc.
- **A**: `MetricPolicy(paradigm)` en L4; claves fuera de la lista ⇒ no se calculan ni persisten; retirar 3 claves huérfanas.
- **B**: calcular todo pero etiquetar `interpretable=false`.
- **C**: mantener.
- **Recommended**: A. **Why**: B persiste basura que `Trends` ya lee.
- **Costs**: S-M; algunos goldens cambian (declarados). **Reversibility**: alta.
- **Confidence**: 95 %.

## CEO DECISION REQUIRED — D-5 Exportación/consentimiento de sondas
- **Current**: `SecureFocusConsentStore` existe (focus2); focus v1 no pide consentimiento; sync unwired; `accountDeletion.ts` no borra Focus en nube (no hay nada que borrar hoy).
- **Problem**: antes de cualquier egreso (V2 necesita datos de N≥60) hay que decidir consentimiento y minimización.
- **A**: consentimiento explícito opt-in para «contribuir a validación», exportación pseudonimizada, sin RT crudos.
- **B**: sin egreso; validación sólo con usuarios internos.
- **Recommended**: A para V2+; B hasta V1.
- **Costs**: M. **Risks**: legal (precedencia 1). **Confidence**: 80 %.

## CEO DECISION REQUIRED — D-6 Sondas no entrenadas vs `implemented == wired`
- **Current**: `ExerciseCatalogConsistencyTest` fuerza que todo lo renderizable esté en `implemented`; sondas usan generadores de ejercicios entrenados.
- **Problem**: no puede existir una sonda no entrenada sin ser también un ejercicio.
- **A**: `ProbeRenderer` separado del catálogo de ejercicios; test de consistencia aparte para sondas.
- **B**: mantener sonda = ejercicio entrenado y declarar «cambio en tarea entrenada» (sin claim de transferencia).
- **Recommended**: A a medio plazo; B como copy inmediata.
- **Why**: sin A, la única afirmación posible es «mejoraste en E1».
- **Costs**: L. **Confidence**: 85 %.

## CEO DECISION REQUIRED — D-7 PRESSURE como eje; E13 fuera
- **Current**: E13 «Presión» es E1 con ventana corta; `DifficultyAxis.PRESSURE` no existe.
- **A**: eje PRESSURE (deadline, feedback, penalización) aplicable a E1/E2/E3; E13 retirado.
- **B**: mantener E13.
- **Recommended**: A. **Why**: falsa variedad; un eje sirve a la variante relajada (menos presión) y a la exigente.
- **Costs**: S. **Confidence**: 90 %.

## CEO DECISION REQUIRED — D-8 E12 como variante de E1
- **Current**: E12 «recuperación post-error» = E1 con feedback y PES.
- **A**: `VariantSpec(feedback=ON)` de E1; PES como métrica opcional.
- **B**: mantener.
- **Recommended**: A. **Confidence**: 85 %.

## CEO DECISION REQUIRED — D-9 Métrica oficial: RT clase C o exactitud bajo deadline
- **Current**: RT clase D en 13/13; Speed tiene clase C sólo Android ≥34.
- **Problem**: RT no comparable entre dispositivos (X5); cablear timing es M y sólo mejora Android.
- **A**: oficial = exactitud/d′ bajo deadline (independiente de ±50 ms); RT sólo descriptivo.
- **B**: cablear `core/timing`, RT oficial sólo con clase ≥C, resto excluido del perfil (como Speed).
- **C**: RT clase D como oficial (hoy).
- **Recommended**: A ahora, B para vigilancia cuando V1 confirme clase C.
- **Why**: la PVT necesita RT; el resto de CORE se mide bien con exactitud bajo deadline.
- **Costs**: A = S; B = M + excluye iOS/desktop del perfil. **Confidence**: 80 %.

## CEO DECISION REQUIRED — D-10 `SessionMode` antes de cualquier variante relajada
- **Current**: sin modo por fila; toda sesión es igual para `Trends`/`OvertrainingGuard`/`TransferReport`.
- **A**: `SessionMode {OFFICIAL, TRAINING, RELAXED}` persistido + filtros en consumidores, ANTES de diseñar la variante.
- **B**: diseñar la variante y filtrar después.
- **Recommended**: A. **Why**: CEO-OVERRIDE-003 exige no fragmentar el perfil; sin modo, la variante lo fragmenta invisiblemente.
- **Costs**: S. **Confidence**: 95 %.

---

# PARTE XXXII — Exercise Design Handoff

Este es el documento que el siguiente diseñador de ejercicios consume. Todo lo no etiquetado como `REPO_VERIFIED`/`TEST_VERIFIED` es `RECOMMENDATION`. Nada de aquí es requisito hasta que el CEO apruebe D-1…D-10.

## 51.1 Taxonomía definitiva propuesta

| Clase | Subhabilidad | Definición operativa (medible) | Paradigma de medida | Paradigmas de entrenamiento |
|---|---|---|---|---|
| CORE | **Vigilancia sostenida** | Mantener la disposición a responder a señales infrecuentes y no predecibles a lo largo de ≥5 min; medida por RT (PVT) o d′ por bloque temporal (CPT) y su pendiente | PVT 5–10 min (D-3B) | E1 (señal rara + señuelos), E11 (con distracción), E12 (con feedback) como variantes |
| CORE | **Inhibición de respuesta** | Retener una respuesta prepotente ante una señal de no-go o de parada; medida por criterio (c) en go/no-go y SSRT en stop-signal | Go/no-go de alta frecuencia go (≥70 %) con d′/c; stop-signal con integración de carrera | E3, E4 |
| CORE | **Filtrado de interferencia** | Responder al objetivo ignorando distractores concurrentes; medida por efecto de congruencia (RT y exactitud) con ≥20 ensayos por celda | Flanker de referencia (Eriksen) | E2 (flanker), E5 (Stroop/Simon) |
| SECONDARY | **Flexibilidad (cambio de regla)** | Coste de cambio en tareas alternantes | Task-switch con cue explícito | E6 |
| SECONDARY | **Búsqueda atencional** | Pendiente RT × set size en búsqueda conjuntiva | Visual search | E7 |
| CROSS | Atención temporal (RSVP) | AB lag-2 | — hasta rediseño (frame-bound) | E10 |
| CROSS | Seguimiento múltiple | Capacidad MOT | — hasta rediseño (respuesta forzada) | E9 |
| CROSS | Coordinación dual | Coste dual | — | E8 |
| EJE (no subhabilidad) | Presión temporal | Deadline/penalización | — | eje PRESSURE (D-7) |

Regla: **una subhabilidad CORE no puede medirse con un solo paradigma ni con el paradigma que la entrena** (L1/L14). Hoy 3/3 CORE violan la regla (`REPO_VERIFIED`).

## 51.2 Coverage map (subhabilidad × ejercicio × rol)

| | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 | E11 | E12 | E13 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Vigilancia | **T+P** | | | | | | | | | | T | T(var) | T(var) |
| Inhibición | | | **T+P** | T | | | | T | | | | | |
| Filtrado | | **T+P** | | | T | | | | | | T | | |
| Flexibilidad | | | | | | T | | | | | | | |
| Búsqueda | | | | | | | T | | | | | | |
| RSVP | | | | | | | | | | T | | | |
| MOT | | | | | | | | | T | | | | |
| Dual | | | | | | | | T | | | | | |

T = entrena; P = sonda (hoy = misma tarea). Ningún CORE tiene P independiente.

## 51.3 Existing coverage (`REPO_VERIFIED`)

- 13 ejercicios renderizables (`implemented == wired`, `ExerciseCatalogConsistencyTest`).
- 3 sondas (`ProbeBank`: VIGILANCE_PROBE→E1, SELECTIVE_PROBE→E2, INHIBITION_PROBE→E3), 4 formas cada una, semillas reservadas disjuntas del entrenamiento (`ProbeSeedSpace`).
- Clasificación por modalidad: go/no-go, choice, stop (`TrialClassifier`); cuota por condición (`conditionSchedule`) en E2/E4/E5/E6/E8/E13.
- Anti-gaming: BlindStrategyCeiling, D3, D4, NO_RESPONSES, MIN_TRIALS 12.
- Timing: `core/timing` (FrameTimingProbe, TemporalClassPolicy) existe y está testeado; **no** consumido por focus v1.
- Persistencia cifrada de resúmenes; `null≠0` en memoria; `readCursor` tri-estado.

## 51.4 Gaps (para diseño)

1. Sonda de vigilancia real (PVT) — no existe.
2. Sonda de inhibición con go ≥70 % y d′/c — E3 hoy tiene rareza variable.
3. Sonda de filtrado con ≥20/celda a 55 ensayos — E2 a `interference` MIN da 11.
4. Ningún ejercicio expone `VariantSpec`; ninguna fila persiste modo.
5. Eje PRESSURE inexistente (E13 lo simula).
6. E9/E10: métricas no interpretables (RT ≥1476 ms; frame 80 ms).
7. Sin feedback intra-ensayo en 12/13 (sólo E12); FEEDBACK_MS 320 fijo.
8. Sin escalera de entrenamiento operativa (Hold/ContainImpulsivity sólo).

## 51.5 Redundancy

| Redundante | Con | Acción |
|---|---|---|
| E13 Presión | E1 + eje | retirar (D-7) |
| E12 Recuperación | E1 + feedback | variante (D-8) |
| E11 Distracción | E1 + distractor | variante de E1 o E2 según el distractor |
| E5 Stroop/Simon | E2 flanker | mantener ambos (paradigmas distintos de interferencia); sonda = flanker |
| E8 Dual | E3 + carga | mantener como CROSS; sin medida oficial |

Regla §61: no forzar 15 ejercicios. Con la taxonomía propuesta el catálogo de entrenamiento mínimo son **5 familias** (E1, E2/E5, E3/E4, E6, E7) con variantes por eje; el resto es CROSS o retirable.

## 51.6 Training requirements (por familia, para ~80 % de acierto)

| Familia | Parámetro que gobierna la dificultad | Rango útil (hoy) | Objetivo de entrenamiento | Dosis mínima por sesión |
|---|---|---|---|---|
| Vigilancia (E1) | ventana de respuesta, rareza, contraste | ventana 950–1400; rareza .06–.30 | HIT ≥ .80 con FA ≤ .10 | 10 min (≥94 ensayos) |
| Inhibición (E3/E4) | proporción no-go / SSD | E3 rareza; E4 SSD 1-up/1-down | p(stop) ≈ .50 (E4); FA ≤ .15 (E3) | 60 ensayos con ≥20 stop |
| Filtrado (E2/E5) | proporción incongruente, separación espacial | `interference` 0–1 | congruencia RT < 60 ms sostenido | ≥40/celda |
| Flexibilidad (E6) | proporción switch, cue lead time | switch .2–.5 | coste < 150 ms | ≥30 switch |
| Búsqueda (E7) | set size, similitud | 4–24 | pendiente < 30 ms/ítem | ≥3 set sizes × 15 |

`RECOMMENDATION`; los rangos «hoy» son `REPO_VERIFIED` (lerps de los generadores).

## 51.7 Measurement requirements

| Requisito | Estado | Fuente |
|---|---|---|
| Piso por celda ≥20 (RT, d′), ≥30 (SSRT), ≥40 (Brier) | `REPO_VERIFIED` (`MinSampleGate`) | `core/scoring/MinSampleGate.kt` |
| SE por métrica (bootstrap) | no existe | L15 |
| Admisión universal (todo bloque) | sólo sonda | L7 |
| `SessionMode` por fila | no existe | D-10 |
| `deviceClass`/`inputKind`/`refreshHz` por fila | no existe | L16 |
| Timing clase ≥C para RT oficial | no (clase D) | D-9 |
| Intervalo ≥48 h entre administraciones oficiales | no existe | L13 |
| Forma fresca (`formIsFresh == true`) en ambos carriles | sólo evaluación | `FocusProgressStateHolder.kt:56-75` |
| Umbrales por paradigma desde bloques admitidos | pooled, sin gate | L9 |
| Base = mediana de ≥3 administraciones | 1 | D-1 |

## 51.8 Adaptation dimensions

Ejes existentes (`DifficultyAxis`, 9): PERCEPTUAL_LOAD, TEMPORAL_UNCERTAINTY, SALIENCE, INHIBITORY_DEMAND, INTERFERENCE, SWITCH_LOAD, SET_SIZE, DUAL_LOAD, TIME_ON_TASK. Propuesto: + PRESSURE. Requisitos: cada ejercicio declara `governingAxis` con rango efectivo pinneado por test (E1 TIME_ON_TASK es inerte hoy); la escalera opera sobre HIT/d′, nunca sobre ACCURACY CR-dominada; StepDown alcanzable; sin ratchet; decisión persistida con versión.

## 51.9 Personalization dimensions

Entradas legítimas (L12): `UserGoal` (estudio/trabajo/conducción-no: evitar claims), perfil L9 (θ+SE por CORE), respuesta al entrenamiento (pendiente de HIT por familia), preferencias (duración, presión, estética), contexto opt-in (hora local, sueño), historial de exposición por familia. Salidas: subhabilidad prioritaria (menor θ con mayor SE ⇒ medir; menor θ con SE aceptable ⇒ entrenar), familia, parámetros, dosis, `why`, `evidenceLevel`. Prohibido: cambiar prescripción por 1 sesión (charter §11.1); arquetipos no validados como autoridad (`ArchetypeInference` hoy no puede promocionar).

## 51.10 Anti-practice requirements

- Semillas reservadas para sondas ✔ (`REPO_VERIFIED`).
- ≥4 formas por sonda ✔; `formIsFresh` en los 2 carriles ✘.
- Sonda con paradigma no entrenado ✘ (D-6).
- Término de práctica en RCI (Chelune) ✘ — requiere V2/V6.
- Ítems paramétricos (no banco fijo) ✔ para 13/13.
- Registro de exposición por familia ✘ (`ExposureTracker` sin call-sites).
- Intervalo mínimo 48 h ✘.

## 51.11 Transfer architecture

Única claim permitida hasta V8: **«mejora en tareas no entrenadas de la misma familia»** (cercana), y sólo con sonda de paradigma no entrenado (L14) + RCI con práctica (L11). Claims prohibidas: transferencia lejana, «Enfoque» global, rendimiento en vida diaria (V9 informativo). Copy hoy (`TransferReport.honestNote`) ya acota; `NEAR_TRANSFER_INDEX` sin productor debe retirarse hasta L14.

## 51.12 Technical constraints (`REPO_VERIFIED`)

- KMP `commonMain`; una respuesta por ensayo en el runtime (`presentAndAwait`); animaciones en el composable keyed por `stimulus.index`.
- RT: `MonotonicClock`, onset pre-await, pointer UP ⇒ clase D; frame 80 ms mínimo práctico en RSVP.
- Persistencia: `@Serializable` + `SecureKeyValueStore`; sólo resúmenes; `FocusSchema.CURRENT` compartido por 3 DTOs (un bump cuarentena todo); `ignoreUnknownKeys` permite campos aditivos.
- 48 dp/44 pt (`core/a11y/A11yTargets`); sin nodos semánticos en estímulos por decisión (`MB-FOCUS-0110`); luminancia ≥3:1 para discriminantes de color.
- Copy: scanner de léxico prohibido lee archivos completos; no nombrar lexemas ni para negarlos.
- `ExerciseCatalogConsistencyTest` fuerza `implemented == wired` (D-6).
- Sin analítica Focus; sin sync; sin consentimiento en focus v1.

## 51.13 Evidence status (qué está demostrado)

| Afirmación | Estado |
|---|---|
| Los 13 generadores son deterministas y testeados | `TEST_VERIFIED` |
| El motor de métricas produce 26 claves para cualquier paradigma | `REPO_VERIFIED` (`CONTRADICTED` con Blueprint V3 §4) |
| Sesión de entrenamiento alcanzable en producción | `CONTRADICTED` (P0) |
| E1 es una PVT | `CONTRADICTED` |
| PS mide Enfoque | `CONTRADICTED` (SAT, sim) |
| RCI puede afirmar cambio | `REPO_VERIFIED` que no (EMPTY) |
| Fiabilidad de cualquier sonda | `UNKNOWN` |
| Timing clase real por plataforma | `UNKNOWN` |
| Efecto de entrenamiento de cualquier ejercicio | `UNKNOWN` |
| Transferencia | `UNKNOWN` (sin instrumento) |
| Personalización individual | `INFERRED` L0 (shipped) |

---

# PARTE XXXIII — Future Exercise Rubric (0–100)

Uso: cada ejercicio futuro se puntúa ANTES de implementarse (diseño) y DESPUÉS (con evidencia). Puntuación < 60 ⇒ no entra en el catálogo; 60–79 ⇒ entrenamiento sólo (`TRAINING`), nunca sonda; ≥ 80 ⇒ candidato a sonda tras V2. Los pesos suman 100 y se justifican por lo que esta auditoría encontró roto con más frecuencia.

| # | Criterio | Peso | Por qué este peso | 0 | 5 | 10 (escala interna) |
|---|---|---|---|---|---|---|
| 1 | Fidelidad de constructo (paradigma declarado = implementado; mecanismo nombrado; literatura con norma) | **12** | El fallo más grave encontrado (E1≠PVT) invalida todo lo demás | paradigma no existe en literatura | paradigma existe, parámetros fuera de norma | parámetros dentro de rango publicado, cita con `Confianza` |
| 2 | Calidad de medida (celda ≥ piso a la dosis prevista; SE calculable; ausencia tipada) | **11** | 55 ensayos no cruzan pisos hoy; sin SE no hay estimación | celdas < piso a cualquier dosis | cruza sólo a dosis máxima | cruza a dosis estándar con SE < 0.15 SD |
| 3 | Impureza de tarea (motor, percepción, lectura, color, memoria) | **9** | 12/13 dependen de discriminación visual fina; E6 dependía del tono | rendimiento dominado por habilidad irrelevante | impureza acotada (luminancia, tamaño) | invariante demostrada (V5) o diseño que la elimina |
| 4 | Intensidad de entrenamiento (desirable difficulty; ~80 % acierto alcanzable; sin techo temprano) | **8** | Motor adaptativo hoy no-op; techo ciego .79 en E1 | sin escalera / techo < 20 sesiones | escalera sobre exactitud | escalera sobre HIT/d′ con rango pinneado, sin techo a 40 sesiones (sim) |
| 5 | Resistencia a la práctica (ítems paramétricos; formas; sin estrategia aprendible) | **8** | Sonda = tarea entrenada; sin término de práctica | banco fijo | paramétrico, 1 forma | paramétrico + ≥4 formas + techo de estrategia-barata medido |
| 6 | Resistencia a exploits (BlindStrategyCeiling, plausibilidad, sandbag, single-tap) | **8** | 8 de 12 exploits sobreviven hoy | ≥1 exploit con PS > honesto | exploits detectados con COVERAGE PARTIAL | suite red-team (dispara/no dispara) por cada estrategia enumerada |
| 7 | Controlabilidad de dificultad (eje gobernante único, monótono, rango efectivo) | **7** | E1 TIME_ON_TASK inerte; E5 no monótono a 55 | eje inerte o no monótono | monótono sin rango declarado | monótono con rango pinneado por test |
| 8 | Calidad de telemetría (por ensayo: onset frame, DOWN/UP, condición, SSD; por bloque: forma, versión, modo) | **7** | `timestampMs` = fin de bloque; `plannedTrials` = 1 | resumen sin forma | forma + versión | `RowProvenance` completa + timing class |
| 9 | Robustez de timing móvil (RT no crítico o clase ≥C; frame-bound declarado) | **6** | Clase D en 13/13; RSVP 80 ms | RT crítico con clase D | exactitud bajo deadline como oficial | clase C verificada (V1) o RT no oficial |
| 10 | Accesibilidad (48 dp; sin dependencia de tono; alternativa declarada) | **5** | Resuelto parcialmente (luminancia E6) | depende de tono o < 44 pt | luminancia ≥3:1 | + fuente escalable + input alternativo |
| 11 | Variedad procedimental (no skin; mecanismo distinto de los ya cubiertos) | **5** | E12/E13 son skins de E1 | skin | variante declarada de una familia | familia nueva con subhabilidad no cubierta |
| 12 | Potencial de transferencia cercana (claim acotada; sonda no entrenada disponible) | **5** | Ninguna claim es posible hoy | sin sonda | sonda = entrenada | sonda de paradigma distinto |
| 13 | Complementariedad (cubre un hueco de 51.4; no duplica) | **4** | Catálogo inflado por estética | duplica | parcial | cubre hueco CORE |
| 14 | Engagement sin contaminación (la estética no cambia la demanda; feedback no revela estrategia) | **3** | Variante relajada pendiente (D-10) | estética altera demanda | `VariantSpec` sin efecto medido | efecto de variante medido ≈ 0 |
| 15 | Valor de personalización (parámetros que un perfil puede prescribir; `why` expresable) | **2** | L12 inexistente | ninguno | 1 parámetro | ≥2 parámetros prescribibles |

**Penalizaciones** (se restan del total): skin de ejercicio existente −20; falsa variedad (mismo mecanismo, distinta estética) −15; score explotable con estrategia enumerable −15; techo temprano (< 20 sesiones en sim) −10; práctica específica de tarea sin forma alterna −10; dependencia de habilidad irrelevante no mitigada −10; sin progresión (eje inerte) −10.

**Justificación de los pesos**: 1+2+3 = 32 puntos son «¿mide lo que dice?» (los tres fallos P1 más repetidos); 4+5+6+7 = 31 son «¿entrena y resiste?»; 8+9 = 13 son «¿se puede auditar?»; 10–15 = 24 son producto. Un ejercicio perfecto en producto y nulo en medida saca 24: fuera. Un ejercicio perfecto en medida y nulo en producto saca 76: entra como TRAINING.

**Aplicación retroactiva al catálogo actual** (estimación `INFERRED`, sin evidencia V1–V8; sirve de calibración de la rúbrica, no de veredicto): E1 ≈ 41 (fidelidad 2, medida 4, exploit 4, telemetría 3, timing 2); E2 ≈ 52; E3 ≈ 48; E4 ≈ 50 (SSRT no computable a 60); E5 ≈ 50; E6 ≈ 47; E7 ≈ 45; E8 ≈ 35; E9 ≈ 22 (−15 exploit, −10 eje); E10 ≈ 28; E11 ≈ 35 (−15 variedad); E12 ≈ 26 (−20 skin); E13 ≈ 21 (−20 skin). Ninguno alcanza 60: coherente con «entrenamiento sí, medida no» de PARTE I.

---

# PARTE XXXIV — Unknowns

Consolidado de las 12 lentes + sesión principal. Sólo entran preguntas que **no** se resolvieron en esta auditoría; las que una lente marcó `UNKNOWN` y la sesión principal verificó se omiten (p. ej. `FocusJson.ignoreUnknownKeys = true` — `REPO_VERIFIED`; `deviceFingerprint` sin lector — `REPO_VERIFIED`).

| # | Question | Why unknown | Where checked | Evidence required | Impact | Who/what can resolve it |
|---|---|---|---|---|---|---|
| U1 | ¿Algún build de producción tiene `focusLifecycleProbeEnabled`/`focusScoreSurfacesEnabled` = true vía Remote Config? | RC v1 sólo muestra android_debug_app; consola no leída | `MindOSApplication.kt:145-154`, `FeatureFlags.kt:131-132`, RC template | Export de la plantilla RC publicada por app.id | Decide si el P0 aplica al 100 % de usuarios o a un subconjunto | Fundador (consola Firebase) |
| U2 | ¿Existen en dispositivos reales perfiles legacy con `baseline != null` (builds pre-FOC-P0-05)? | 0 telemetría; sin egreso | `LocalFocusRepositories`, analítica (0 eventos) | Evento `profile_loaded(baselineMaturity)` | Un usuario legacy sí puede entrenar; cambia la población afectada por P0 | Instrumentación (ítem 8 del roadmap) |
| U3 | ¿Tasa real de `Admitted` en dispositivo con lifecycle ON? | Sólo 2 corridas en emulador con drivers guionizados (`MB-FOCUS-0165`) | `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md` | V0 (20 usuarios) | Si un respondedor honesto lento cae por `RESPONSE_PATTERN` (`MB-FOCUS-0146`), la sonda excluye a quien más la necesita | V0 |
| U4 | ¿Clase de timing real (A–E) por plataforma para RT en focus v1? | `core/timing` no consumido; protocolo físico no ejecutado | `S5_PASO6_PHYSICAL_PROTOCOL.md`, `FocusSessionStateHolder.kt:266` | V1: cámara 240 fps, N≥30 por plataforma, sd(D) | Decide D-9 (RT oficial o exactitud bajo deadline) | V1 |
| U5 | ¿Semántica exacta del `onTap` de `detectTapGestures` en la versión de Compose del repo (UP con dwell)? | Contrato de librería recordado, no leído del fuente empaquetado | `FocusStimulusField`, Compose foundation | Lectura del fuente de `TapGestureDetector.kt` de la versión pinneada o medición V1 | Magnitud del sesgo +50–150 ms | Lectura de fuente (barata) |
| U6 | ¿Cuánto infla el pooling E1+E2+E3 el umbral de lapso respecto a E1 solo? | La sim fue E1-only; producción pool 195 ensayos de 3 paradigmas | `BaselineAssembler.kt:42-43`, `sim_digest.txt` | Sim con 3 paradigmas o datos V0 | Dirección conocida (infla), magnitud no | Extender `focus_reimpl.mjs` a E2/E3 (temp space) |
| U7 | ¿E4 cumple mínimos Verbruggen 2019 (p(respond·stop) 0.25–0.75, ≥50 stops, RT_go no creciente) a 60/94 ensayos? | E4 no reimplementado; sólo verificado que la SSD pedida se registra como usada (K5) y que el único gate es la banda p(respond) post-hoc | `E4FrenoGenerator`, `StopSignalTracker`, `MinSampleGate.SSRT=30` | Sim de E4 con escalera | X6 y validez de SSRT | Sim (temp space) |
| U8 | ¿Sobreviven SSRT/SWITCH_COST/CONGRUENCY_EFFECT en el DTO persistido o sólo en memoria? | `SessionSummaryDto` no leído por la lente cs; sesión principal verificó `sessionMetrics` (6 claves) y `blockMetrics` no persistidos | `FocusPersistenceMappers`, `FocusDtos.kt` | Lectura de mappers (barata) | Si no se persisten, `Trends`/`TransferReport` nunca los verán | Lectura de fuente |
| U9 | ¿El veredicto `ADMITTED` de la sim para P8 (acc .713) coincide con Kotlin al techo ciego real de la sonda? | Reimplementación, no test Kotlin | `sim_results.json`, `FocusAntiGaming.kt` | Test Kotlin con el mismo stream | Si P8 se rechaza, el hallazgo SAT se acota a P1–P7 | Test (temp) |
| U10 | ¿ICC test-retest de PVT/flanker/go-no-go en móvil no supervisado? | Literatura recordada (Hedge 2018, Rouder & Haaf 2019: ~.5–.8), no recuperada | — | Búsqueda bibliográfica con cita | Umbral V2 y factibilidad del RCI individual | psychometrician con WebSearch |
| U11 | ¿Compose iOS pointer events y `CADisplayLink` en focus v1? | iOS diferido en Windows; nunca compilado localmente | `iosMain` | Build en macOS + V1 iOS | iOS puede ser clase E | CI macOS (existe workflow) |
| U12 | ¿Colisión práctica de `startedAtMs` como clave entre `SessionRecord` y `SessionSummaryDto`? | No medido | `FocusPersistenceMappers`, `SessionRecorder` | Test de 2 sesiones mismo ms | Baja; corrupción de historial | Test (temp) |
| U13 | ¿`SecureSessionRecorder` de focus v1 puebla `reproducibleIdentity` (seed/versiones)? | No leído por la lente privacy; sesión principal verificó `FormDescriptorV1` sólo E2 | `SessionRecorder.kt` | Lectura | Reproducibilidad 12/13 | Lectura de fuente |
| U14 | ¿Estado de `mindboost_sync_firestore_enabled` en consola RC? | Irrelevante para egreso (decorador unwired) pero no verificado | RC | Export RC | Ninguno hoy | Fundador |
| U15 | ¿Los rangos de ISI/rareza/ventana de E1 al `fixedProbeDifficulty` (0.4/0.4) dan exactamente 35 targets/94? | La sim usó 35/94; producción a 0.4 da rareza .204 ⇒ ~19/94 esperados | `E1VigiaGenerator.kt:30-75`, `ProbeBank.fixedProbeDifficulty` | Cálculo directo del generador (barato) | Cambia n de celda RT en la sonda (11–13 verificados a 55) | Cálculo |
| U16 | ¿Qué docs consumen `CognitiveModule` (0 lectores en código)? | Grep en `shared/src` = 0; docs no exhaustivamente | `Capacity.kt` | Grep en `docs/` | Taxonomía muerta vs viva | Grep |

Ninguna de las 16 bloquea el veredicto de PARTE I; U1–U4 bloquean cualquier afirmación cuantitativa sobre población afectada y timing.

---

# PARTE XXXV — Final Truth Matrix

| Claim | Status | Evidence | Confidence | Implication |
|---|---|---|---|---|
| Con flags embarcados, ningún usuario nuevo puede entrenar en Enfoque | `REPO_VERIFIED` | `FocusHomeScreen.kt:158-175` · `FocusHomeStateHolder.kt:88` · `RememberFocusStateHolders.kt:86-90` · `AppLifecycleProbe.kt:72-76` · `SessionQualityGate.kt:55-64` | 100 % | P0; toda medida de producción es hipotética |
| Cada intento de base que COMPLETA el bloque de sonda y es denegado quema una forma reservada y re-persiste umbrales + startingDifficulty | `REPO_VERIFIED` (refutación K1: sobrevive, 88 %) | `FocusBaselineStateHolder.kt:138` (forma «vista» ⇒ gastada) → `:151` admit → `:213` recordProbe → `:221` save; `releaseProbe` sólo desde el `finally` del run (:163-169) y sólo para forma NO vista; `abort()` :91-98 no lo llama | 100 % | X4; con flags embarcados TODA base completada quema una forma; tras 4 el cursor envuelve (`LocalFocusRepositories.kt:105,121`) y la base re-sirve 1001 sin declararlo. Consecuencias añadidas por la refutación (3/3, 85–88 %): (a) un intento ABORTADO no quema ni churnea (`finally` :163-169 bajo `NonCancellable`; pin `abortInsideTheProbeBlock_givesTheReservedFormBack` :83-109) — el corte «vista» del carril de base es «bloque terminado», el del carril de evaluación es «primer estímulo» (`FocusEvaluationStateHolder.kt:153-164`), asimetría entre carriles; (b) cada intento completado desplaza el cursor de formas pero NO `priorAdministrations`, que sólo cuenta filas `Admitted` (`FocusEvaluationStateHolder.kt:144-146`), así que con k≥1 intentos denegados-pero-completados el primer retest con puerta de fiabilidad abierta dibuja el índice k+3 ≥ 4 ⇒ `repeatsEarlierForm = true` ⇒ `formIsFresh = false` (`ProbeReliabilityEstimator.kt:44`) ⇒ fiabilidad RETENIDA en todos los retests posteriores de ese usuario — la garantía de `ProbeBank.kt:20-27` («el primer retest con puerta abierta nunca cae en forma repetida») deja de valer; (c) cada intento completado añade una fila a `probeHistory` (:213), crecimiento persistente |
| Existen dos motores de Enfoque; sólo v1 está montado | `REPO_VERIFIED` | `MindBoostHubScreen.kt:117`; focus2 CONTRACT_INERT | 100 % | Auditar v1; focus2 es diseño |
| 13 ejercicios renderizables con generadores deterministas testeados | `TEST_VERIFIED` | `ExerciseCatalogConsistencyTest`, `DeterministicRngGoldenTest`, 4468/0F | 100 % | Base sólida para entrenamiento |
| E1 no es una PVT | `REPO_VERIFIED` + `CONTRADICTED` (declaración); refutación K2: sobrevive 3/3 (92–95 %) | `E1VigiaGenerator.kt:30-48` (mapa: rareza .06–.30, señuelos, ventana 950–1400) y `:57-93` (cuerpo); la sonda reservada corre a `perceptualLoad 0.4` ⇒ rareza ≈ 0.20, no ~100 %; `E12_REENGANCHE` lleva la MISMA etiqueta (`ExerciseCatalog.kt:224`); `MACKWORTH_CLOCK` existe en la taxonomía (`CognitiveTaxonomy.kt:54`) sin rama de render | 100 % | Métricas de PVT sin significado; D-3 |
| La exactitud de E1 es CR-dominada (techo ciego ≈ .79) | `REIMPLEMENTATION_REPRODUCED` + `TEST_VERIFIED` (BlindStrategyCeiling) | sim S2/S4; `FocusBlindStrategyTest` | 95 % | ACCURACY ≠ capacidad; escalera inicial inflada (X3) |
| Métricas base se calculan para los 13 paradigmas sin allow-list | `REPO_VERIFIED` · `CONTRADICTED` (Blueprint V3 §4) | `DefaultMetricsEngine.kt:70-114`, `:312-327` | 100 % | D-4 |
| `sessionMetrics` nunca contiene SFI/BLOCK_STABILITY (y `INHIBITORY_CONTROL_INDEX` no tiene productor a ningún nivel) ⇒ `FocusScore.confident` estructuralmente false (≤1 componente < `MIN_COMPONENTS=2`; el VALOR sí se emite: accuracy×100 con banda ancha y caption «preliminar»); chip FOCO no se renderiza con flag OFF y es «—» permanente en cuanto el flag se encienda; tendencia SFI (`Trends.kt:55`, `FocusProgressStateHolder.kt:89`, `FocusProfileSource.kt:107`) siempre no-confident | `REPO_VERIFIED` (refutación K3: sobrevive 3/3) | `DefaultMetricsEngine.kt:123-152`, `focus/domain/reporting/FocusScore.kt:43,:55-61,:76`, `FocusHomeStateHolder.kt:50-55`, `FocusHomeScreen.kt:249-260` | 100 % | Superficie muerta; retirar. 3 tests fabrican SFI en `sessionMetrics` (forma que producción nunca emite) |
| Calibración sin gate; umbrales pooled de 3 paradigmas; persisten aunque el probe se deniegue | `REPO_VERIFIED` + sim S12 | `FocusBaselineStateHolder.kt:100-227`, `BaselineAssembler.kt:30-60` | 100 % | X1, X3; base corruptible |
| Umbral de lapso pre/post distinto fabrica «cambio fiable» en LAPSES | `REIMPLEMENTATION_REPRODUCED` | sim S3 (−13.18*) | 90 % | RCI de LAPSES no interpretable hasta persistir `thresholdsUsed` |
| PS mezcla velocidad y exactitud y premia ~340 ms | `REIMPLEMENTATION_REPRODUCED` | SAT P1–P8: PS 49.8→81.7→71.0 | 90 % | PS ≠ Enfoque; feedback de sesión sólo |
| PS se computa para sesiones INVALIDADAS | `CONTRADICTED` (refutación K4: refutado 3/3, 88–93 %; residuo: la garantía es por disciplina de call-site, no por tipo — `focusQualityGateScanner` sólo pinnea call-sites de `admit`, no de `personalizedScorer.score`, y `FocusReport.personalizedScore` es un opcional sin vínculo de tipo con la admisión): la firma del scorer es ciega a la admisión, pero su único call-site (`FocusSessionStateHolder.kt:464-469`) gatea por `session.admission == Admitted` (que ya incluye el veredicto anti-gaming vía `SessionQualityGate.kt:59-60`) y por el flag; PS nunca persistido; pin `FocusQualityGateWiringTest.kt:178-185` | sim S2 (artefacto de la reimpl, no del árbol) | 88 % | Sin defecto vivo; queda como smell de diseño P3 (guard en el caller, no en el tipo) |
| RT es clase D (tap UP, onset pre-await) en 13/13 y en los TRES carriles | `REPO_VERIFIED` (K5: 3/3, 92–95 %; U5 pendiente para magnitud) | `FocusSessionStateHolder.kt:266` y `FocusFixedBlockRunner.kt:93-96` (línea base y sonda: mismo patrón, `.coerceAtLeast(0)` colapsa un delta negativo en 0 en vez de ausencia), `detectTapGestures` en `FocusSessionScreen.kt:242/:247` y `FocusMeasureRunningContent.kt:116/:130`; `core/timing` sin consumidor en `focus/` | 95 % | RT no comparable entre dispositivos; afecta también al pre/post del RCI; en taxonomía del repo = `TemporalClass.T0_NON_AUTHORITATIVE` / `ClockDomain.NONE_MONOTONIC_FALLBACK`; techo YA declarado y con dueño (`MB-FOCUS-0099`, `SessionQualityGate.kt:13-15`); no llega al perfil (`FocusProfileSource.kt:55-61` fail-closed) pero sí al informe, al historial y a `AdaptiveEngine.kt:92` (RT_CV); D-9 |
| E9 produce RT ≥ ~1476 ms ⇒ lapseRate 1, SFI de bloque 0 (para quien espera a la sonda); en sesión la fuga es LAPSES ≈ aciertos (SFI no se re-deriva en `sessionMetrics`) | `REPO_VERIFIED` (K2: 3/3) | PROBE_START 0.82 × 1800; `DefaultMetricsEngine.kt:87,:135-142` | 100 % | Métricas de E9 no interpretables |
| `withdrawn` se persiste como 0.0 (origen: `MetricValue.unavailable`, común a toda ausencia) | `REPO_VERIFIED` (K6: 3/3, 85–92 %) | `FocusSessionAssembler.kt:248-250` → `MetricKey.kt:88` → `FocusPersistenceMappers.kt:87-88` (`MetricValueDto.value: Double` no nulo, sin `absence`) | 100 % | Se persiste `confident=false` y la admisión: no es fabricación viva sino pérdida de la razón tipada (deuda declarada 2026-07-15); ningún lector de focus v1 usa `rawValueOf`; X11 |
| `deviceFingerprint` nunca poblado; sin `deviceClass` | `REPO_VERIFIED` | `:209` | 100 % | X5 |
| RCI siempre NotEnoughData (`ReliabilitySource.EMPTY` por 3 defaults) | `REPO_VERIFIED` | `FocusGraph.kt:156` → `FocusEvaluationService.kt:17` → `ProbeReliabilityEstimator` | 100 % | Ninguna claim de cambio hoy; correcto |
| Carril de progreso pasa `repeatsEarlierForm = null` | `REPO_VERIFIED` (K7: 3/3, 92–97 %) | `FocusProgressStateHolder.kt:62-71` (el `null` en `:70`); `FocusEvaluationStateHolder.kt:235-241` pasa el hecho real | 100 % | `formIsFresh` retiene ✔ pero SIN efecto diferencial hoy (ambos carriles reciben `ReliabilitySource.EMPTY`): tercer gate latente. La consecuencia viva es la divulgación: la pantalla de progreso no lleva ningún texto de forma (ni repetida ni de identidad DESCONOCIDA); la cláusula `REPEATED_FORM_CLAUSE` sólo existe en `FocusEvaluationScreen.kt:230-239` |
| Motor adaptativo: sólo Hold/ContainImpulsivity alcanzables; StepUp/Down no | `REPO_VERIFIED` (K7: 3/3, 85–95 %) + `REIMPLEMENTATION_REPRODUCED` | `AdaptiveEngine.kt` (clase `DefaultAdaptiveEngine`), `baselineCv` nunca llega (`:96-98`), `fatigue=null` mata StopSession/Cooldown (`:33`) y `holdForAbsence` (`:106`) precede a todo Step; también inalcanzables HYSTERESIS_AFTER_CHANGE, MAINTENANCE_BAND y NOT_APPLICABLE_TO_FAMILY (sin productor; el KDoc de `FocusAdaptiveRulePolicy.kt:19-20` sobreafirma); sim E3_S2b ratchet | 95 % | 10/13 sin adaptación; ratchet en E3/E4/E12 (E12 es PVT con INHIBITORY_DEMAND en `relevantAxes`, `ExerciseCatalog.kt:224-228`, y `E12ReengancheGenerator.kt:21` lo consume: NO inerte); en E1 el eje es inerte |
| Personalización individual: L0 embarcado | `REPO_VERIFIED` | PARTE XX | 95 % | Cadena §62 rota en el primer eslabón |
| 0 eventos de analítica de Focus (`MindBoostAnalyticsEvent`); existe un único breadcrumb Crashlytics PII-free (`FocusScoreContainmentBreadcrumb.kt:26-28`) y el stream local de focus2 sin montar | `REPO_VERIFIED` (K6: 3/3, 90–95 %) | `MindBoostAnalytics` grep; `FocusGraph.kt:160-161`; `firestore.rules:1840-1842`; `accountDeletion.ts` | 100 % | Ciego en producción; egreso inerte por construcción (no sólo por flag); hueco de borrado latente si se cablea el sync |
| Sync/egreso inerte por construcción; rules deny | `REPO_VERIFIED` | `firestore.rules`, decorador unwired | 100 % | Sin riesgo de egreso hoy |
| `accountDeletion.ts` no borra Focus en nube (no hay nada) | `REPO_VERIFIED` | grep | 100 % | Correcto hoy; deuda si se cablea sync |
| Sonda = tarea entrenada para las 3 CORE | `REPO_VERIFIED` | `ProbeBank`, `ProbeExerciseMapping` | 100 % | Ninguna claim de transferencia posible |
| 55 ensayos no cruzan pisos de RT (110 necesarios) | `TEST_VERIFIED` | `ProbeMeasurementReachabilityTest` (110/109) | 100 % | Sonda subpotente por diseño |
| Base de 1 administración contradice charter §11.1 | `DOCUMENTED_POLICY` vs `REPO_VERIFIED` | charter :438-449 vs `BaselineAssembler` | 100 % | D-1 |
| El sensor de ciclo de vida funciona en dispositivo | `CLAIMED_NOT_VERIFIED` | sólo emulador; `MB-FOCUS-0101` OPEN | — | U3 |
| Fiabilidad, validez, sensibilidad al cambio, transferencia | `UNKNOWN` | ningún estudio | — | V2–V8 |
| Los gates 2026-09-14 son verdes | `TEST_VERIFIED` | 4468/0F/0E sobre 624 XML frescos; detekt 0 | 100 % | El código que existe hace lo que sus tests dicen |
| `MinSampleGate.RCI nMin = 2` no tiene consumidor | `REPO_VERIFIED` | grep; piso vivo `MIN_ADMINISTRATIONS = 3` | 100 % | Constante muerta (P3) |
| `INHIBITORY_CONTROL_INDEX`, `NEAR_TRANSFER_INDEX` sin productor; `MinSampleGate.RCI nMin=2` muerto | `REPO_VERIFIED` (K8: sub-claims (a)(b) 3/3) | `MetricKey.kt:22/24`, `DefaultMetricsEngine` (0 ocurrencias), `FocusScore.kt:58`, `MinSampleGate.kt:56` | 100 % | Etiquetas huérfanas; consumidor que nunca se alimenta |
| `DUAL_TASK_COST ≡ 0` (afirmación de la primera pasada) | `CONTRADICTED` (K8: refutado 3/3, 70–85 %) | `DefaultMetricsEngine.kt:375-384` resta dos celdas distintas; `ParadigmMetricsTest.kt:59` = 0,4; `FocusBlindStrategyTest.kt:263-267` | 100 % | Se sustituye por el hallazgo estrecho y vivo: la demanda secundaria no se puntúa, ignorarla da el mejor valor 0,0 (`MB-FOCUS-0141`) |
| Molvex = nombre vigente; repo dice MindBoost | `CEO_APPROVED` vs `REPO_VERIFIED` | CEO-OVERRIDE-001; grep | 100 % | D-0 |

---

# ANEXO — Refutación adversarial (Fase 4)

Dieciséis hallazgos portantes de la auditoría (C1–C16) se sometieron a refutación adversarial por tres lentes independientes (K = mecanismo, K' = bypass/implicación, K'' = lectura literal del código) con la instrucción explícita de **refutar por defecto ante la duda**. 24 agentes de solo lectura, 0 errores, 8,48 M tokens; ningún agente tenía Bash ni Gradle (mecanismo `MB-FOCUS-0083`). Regla de lectura: un hallazgo con **cero** refutaciones emitidas es NO JUZGADO, jamás refutado (aprendizaje 2026-09-06). Aquí todos los hallazgos recibieron ≥ 2 votos: **ninguno queda NO JUZGADO**. C1 recibió 2 votos (la tercera lente no emitió sobre C1) y se reporta con ese techo.

## Tabla de veredictos

| Claim | Votos | Refutados | Sobrevive | Confianza | Corrección de precisión aplicada al informe |
|---|---|---|---|---|---|
| C1 — Entrenamiento inalcanzable con flags de fábrica (`focusLifecycleProbeEnabled=false`) | 2 | 0 | **Sí** (2/2) | 90–92 % | Acotado al flag de ciclo de vida (no a `focusScoreSurfacesEnabled`, que sólo oculta chip/enlace); desktop/iOS `NoOp` ⇒ inalcanzable incluso con flag ON; la vía «RC sólo debug» es `CLAIMED_NOT_VERIFIED` |
| C2 — Quema de formas en intentos de línea base completados pero denegados | 3 | 0 | **Sí** (3/3) | 85–88 % | Los abortos NO queman (`finally` libera lo no visto); sólo los intentos que corren hasta el final y salen NotMeasurable; desajuste cursor vs `priorAdministrations` documentado |
| C3 — E1 y E12 etiquetados PVT sin serlo (mapeo de dificultad, rareza ≈ 0,204) | 3 | 0 | **Sí** (3/3) | 92–95 % | Ninguna |
| C4 — Métricas de sesión emitidas sin allow-list frente a Blueprint V3 §4 | 3 | 0 | **Sí** (3/3) | 85–95 % | Ninguna |
| C5 — `sessionMetrics` sin SFI/BLOCK_STABILITY ⇒ FocusScore nunca confident | 3 | 0 | **Sí** (3/3) | 88–92 % | Ninguna |
| C6 — Bloques de calibración sin gate; umbrales agrupados persistidos aunque se deniegue | 3 | 0 | **Sí** (3/3) | 92–95 % | Ninguna |
| C7 — «El PS ignora la admisión» | 3 | **3** | **No** (refutado 3/3) | 88–93 % | Retirado del informe: el único call-site (`FocusSessionStateHolder.kt:463-469`) gatea con `admitted && scoreSurfacesEnabled()`; la reimplementación reprodujo la FIRMA, no el CALL-SITE. Residuo registrado: el escáner sólo pinnea `admit`, no este gate |
| C8 — RT clase D (tap UP, onset antes de `presentAndAwait`) | 3 | 0 | **Sí** (3/3) | 92–95 % | Alcance AMPLIADO al carril de medición (`FocusFixedBlockRunner.kt:93-96`), `.coerceAtLeast(0)` colapsa delta negativo en 0, taxonomía T0/`NONE_MONOTONIC_FALLBACK`, techo ya con dueño `MB-FOCUS-0099`; no llega al perfil pero sí a informe/historial/motor |
| C9 — Persistencia: `withdrawn` → 0,0; `deviceFingerprint` null; `SubjectiveState` nunca poblado; literales 1 | 3 | 0 | **Sí** (3/3) | 85–92 % | Atribución corregida (`FocusSessionAssembler.kt:248-250`, no `LocalFocusRepositories`); el 0,0 nace en `MetricValue.unavailable` para TODA ausencia; `confident=false` y admisión SÍ se persisten ⇒ deuda declarada, P1→P2; `rawValueOf` sin llamadores; literales 1 van al `SessionRecord` de core, no al DTO de Focus |
| C10 — Motor adaptativo: `baselineCv` nunca llega; sólo Hold/ContainImpulsivity alcanzables | 3 | 0 | **Sí** (3/3) | 85–95 % | Fichero real `AdaptiveEngine.kt` (clase `DefaultAdaptiveEngine`); E12 es PVT con INHIBITORY_DEMAND consumido ⇒ ratchet en E3/E4/E12 (no inerte); persistencia sin guardar por admisión acotada: la dificultad NO se mueve, sí `totalSessions`/historial/materialización |
| C11 — Cero egreso analítico; sync inerte por construcción; reglas por catch-all | 3 | 0 | **Sí** (3/3) | 90–95 % | Un breadcrumb Crashlytics existe; flag legible por RC pero decorador nunca construido; KDoc del gateway cita una regla inexistente; `accountDeletion.ts` sin purga de `users/{uid}/sessions` ⇒ hueco LATENTE |
| C12 — E9: piso de RT y fuga de LAPSES | 3 | 0 | **Sí** (3/3) | 85–90 % | Ninguna |
| C13 — Carril de progreso pasa `repeatsEarlierForm = null` | 3 | 0 | **Sí** (3/3) | 92–97 % | Líneas `:62-71` (null en `:70`); sin efecto diferencial hoy (`ReliabilitySource.EMPTY` en ambos carriles): tercer gate latente; consecuencia viva = ausencia de divulgación de forma en progreso |
| C14 — Claves huérfanas: `INHIBITORY_CONTROL_INDEX`, `NEAR_TRANSFER_INDEX`, `RCI nMin=2`, «DUAL_TASK_COST ≡ 0» | 3 | **3** (parcial) | **Parcial** | 70–85 % | Sub-claims (a) claves sin productor y (b) `MinSampleGate.RCI` sin consumidor SOBREVIVEN 3/3. Sub-claim (c) «≡ 0» REFUTADO 3/3: `dualTaskCost = acc(single) − acc(dual)` sobre celdas distintas (`ParadigmMetricsTest.kt:59` = 0,4). Sustituido por el defecto estrecho y vivo: la secundaria no se puntúa, ignorarla da 0,0 (`MB-FOCUS-0141`) |
| C15 — E4: SSD pedida registrada como usada; sin detector de ralentización | 3 | 0 | **Sí** (3/3) | 80–88 % | Sesgo del SSRT es CONSERVADOR (no halaga); NUEVA discrepancia opuesta: `MAX_SSD_MS=1000` > ventana mínima 800 ⇒ SSD registrada excede la fase GO realizada; radio hoy = TRAINING (no RCI), `measurementSufficient=false` a ≤ 60 ensayos ⇒ latente |
| C16 — Anti-gaming: S15 (190 ms constante) pasa como honesto | 3 | 0 | **Sí** (3/3) | 80–92 % | D3 exige mediana < 250 **y** accuracy ≈ techo ciego ⇒ un repetidor rápido y preciso es el hueco exacto; D4 divide por TODOS los ensayos; stops de E4 nunca ANTICIPATION; n < 12 ⇒ NONE; «admitido» condicionado al sensor de ciclo de vida ON |

Totales: 16 claims · 47 votos emitidos · 13 sobreviven íntegros · 1 sobrevive parcialmente (C14) · 1 refutado (C7) · 0 NO JUZGADOS.

## Por qué sobreviven los que sobreviven (§60)

Cada superviviente pasó por al menos una lente que intentó el **bypass** (¿hay otra ruta que anule la consecuencia?) y una que intentó la **lectura literal** (¿dice el código lo que la auditoría afirma?). Sobreviven porque:

- **C1, C2, C6, C13**: la consecuencia se sostiene por **estructura**, no por interpretación — un único productor de `FocusRoute.Session`, un único sitio que nulifica `unspentProbeId`, un `filterValues { it.confident }` que nunca gatea, un literal `null` en una llamada. Los refutadores buscaron rutas alternativas (deep link, `FocusTracerScreen`, otro carril) y midieron que no existen.
- **C3, C4, C5, C12**: son hechos de **catálogo y de conjunto** verificables por grep exhaustivo (paradigma declarado vs generador real; claves emitidas vs allow-list; claves requeridas vs claves producidas). Ningún refutador encontró un productor o un filtro que la auditoría hubiera pasado por alto.
- **C8, C9, C10, C11, C15, C16**: sobreviven **con corrección de precisión**. Las lentes refutaron ATRIBUCIONES (fichero equivocado, línea desplazada, conjunto incompleto) y SEVERIDADES (P1 → P2 en C9; «inerte» → «vivo» para E12 en C10; «halaga» → «conservador» en C15), pero no el MECANISMO. Cuando una lente corrigió el enunciado, la corrección se incorporó al informe y el hallazgo quedó **más estrecho y más verdadero**, que es el resultado que la refutación existe para producir.

## Por qué cae lo que cae

- **C7** cayó porque la reimplementación reproducía la firma de `PersonalizedSessionScorer` y no su call-site: el gate `if (admitted && scoreSurfacesEnabled())` vive en el llamador, no en la función. Lección para la Fase 5 (§XXXIV): una reimplementación en espacio temporal demuestra propiedades de una FUNCIÓN, nunca de un WIRING; el wiring se lee, no se reimplementa.
- **C14(c)** cayó porque «≡ 0» era una inferencia sobre la ausencia de un bloque single de referencia que sí existe (`E8DuoGenerator.kt:37-45` sirve cuota dual/single dentro de la misma sesión). El defecto real —la demanda secundaria no puntuada— ya tenía fila dueña (`MB-FOCUS-0141`) y el informe pasa a citarla en vez de abrir un hallazgo falso.

## Techos de esta fase

- 2 votos sobre C1 (no 3): la tercera lente no emitió sobre C1. Se reporta como sobreviviente con 2/2 y confianza 90–92 %, no como 3/3.
- Los agentes de refutación son de solo lectura sin Gradle: ningún veredicto de esta fase se apoya en una corrida de tests nueva; las citas `TEST_VERIFIED` provienen del gate de 2026-09-14 (4468/0F/0E sobre 624 XML frescos).
- La refutación cubre los 16 claims portantes, no las 300+ afirmaciones del informe. Las restantes conservan su etiqueta epistémica original (`REPO_VERIFIED`, `INFERRED`, `HYPOTHESIS`…) sin el sello de esta fase.

# §58 — Scoring de madurez del motor (0–10 por dimensión)

Escala: 0 = no existe; 3 = existe sin verificar; 5 = existe, verificado, con fallo conocido; 7 = correcto y testeado sin validación humana; 10 = validado con humanos y auditado. `N/D` si no hay evidencia.

**1. Construct definition — 3/10**
Current: «Enfoque» = 9 `Capacity` + 6 `CognitiveModule` sin lector + 14 `Paradigm`; sin definición operativa medible por subhabilidad.
Evidence: `Capacity.kt`, `ExerciseCatalog`, PARTE IV.
Why not higher: E1 declara PVT y no lo es; no hay registro de constructos; docs y código divergen.
10/10 requires: L1 `ConstructRegistry` con definición operativa, paradigma de medida y de entrenamiento por subhabilidad, test que exige ≥2 paradigmas de medida por CORE.

**2. Construct separation — 2/10**
Current: 26 métricas calculadas para 13 paradigmas; PS mezcla SAT; sonda = tarea entrenada.
Evidence: `DefaultMetricsEngine.kt:70-114`, SAT P1–P8.
Why not higher: ninguna métrica se atribuye a una sola subhabilidad; sin MTMM.
10/10 requires: `MetricPolicy`, V4 (validez discriminante).

**3. Domain coverage — 5/10**
Current: 3 CORE cubiertas por entrenamiento (E1/E2-E5/E3-E4), 2 SECONDARY (E6/E7), 3 CROSS.
Evidence: 51.2.
Why not higher: 0 CORE con sonda independiente; PRESSURE como ejercicio.
10/10 requires: sonda no entrenada por CORE (L14).

**4. Task diversity — 4/10**
Current: 13 ejercicios, 5 familias reales; E11/E12/E13 son variantes de E1.
Evidence: PARTE VII/IX.
Why not higher: falsa variedad; RSVP/MOT no interpretables.
10/10 requires: catálogo por familia con `VariantSpec`; sin skins.

**5. Training quality — 3/10**
Current: generadores deterministas ✔; escalera no operativa (Hold/ContainImpulsivity); sin objetivo de acierto; E1 TIME_ON_TASK inerte.
Evidence: PARTE XIII/XIV, sim E3_S2b.
Why not higher: 11/13 sin adaptación; ratchet en E3/E4.
10/10 requires: L6 staircase sobre HIT con deadline + V7 dosis-respuesta.

**6. Measurement quality — 2/10**
Current: sonda 55 ensayos (RT n=11–13 < 20); RT clase D; sin SE; calibración sin gate.
Evidence: `ProbeMeasurementReachabilityTest`, PARTE X/XII/XVII.
Why not higher: ninguna administración alcanza los pisos que el propio repo declara.
10/10 requires: dosis derivada de pisos, timing clase C, SE, gate universal, V2.

**7. Metric validity — 2/10**
Current: `null≠0` en memoria ✔; ACCURACY CR-dominada; lapsos de PVT sobre no-PVT; 3 claves huérfanas; 0.0 en disco.
Evidence: PARTE XI, X11.
Why not higher: la mitad de las claves no tiene interpretación para el paradigma que las produce.
10/10 requires: D-4 + V4.

**8. Difficulty modeling — 3/10**
Current: 9 ejes con lerps fijos; sin parámetro de ítem; escalera inicial sobre ACCURACY.
Evidence: PARTE XIII.
Why not higher: dificultad ≠ capacidad no está separado (ninguna b).
10/10 requires: rango efectivo pinneado por ejercicio; 1PL cuando V2.

**9. Adaptation — 2/10**
Current: `DefaultAdaptiveEngine` (`AdaptiveEngine.kt`) con 4 decisiones alcanzables de 8; `baselineCv` nunca llega; `AdaptiveModeGate` muerto; perfil e historial se escriben sin guardar por admisión (la dificultad NO se mueve en una sesión no medible: se materializa la entrada inicial y `totalSessions+1`, pinneado como diseño en `FocusQualityGateWiringTest.kt:139-140`).
Evidence: `AdaptiveEngine.kt:96-98` (clase `DefaultAdaptiveEngine`), PARTE XIV; K7 3/3.
Why not higher: no adapta.
10/10 requires: L6.

**10. Individual personalization — 1/10**
Current: L0 embarcado; L1–L3 parcial con flag; `DailyPlanGenerator` por recencia; sin `UserGoal`.
Evidence: PARTE XX.
Why not higher: nada del perfil cambia la prescripción.
10/10 requires: L12 + prior/shrinkage + V6.

**11. Dose optimization — 2/10**
Current: plantillas 5/10/20/30 min → 55/94/157/160 sin justificación; base 250 ensayos; sin intervalo.
Evidence: `SessionTemplate`, PARTE XXI.
Why not higher: 55 no cruza pisos.
10/10 requires: L13 + V7.

**12. Practice control — 3/10**
Current: semillas reservadas ✔, 4 formas ✔, `formIsFresh` en evaluación ✔; carril de progreso `null`; sonda = entrenada; sin término de práctica.
Evidence: PARTE XIX.
Why not higher: práctica de tarea no separable de mejora (S7 ≡ S8).
10/10 requires: L14 + Chelune tras V2/V6.

**13. Device robustness — 1/10**
Current: `deviceFingerprint` null; sin `deviceClass`; RT clase D; `core/timing` sin cablear.
Evidence: S9, X5.
Why not higher: un cambio de dispositivo fabrica tendencia.
10/10 requires: `DeviceContext` + V1 + comparación intra-clase.

**14. Temporal integrity — 2/10**
Current: `MonotonicClock` ✔; onset pre-await; UP; `timestampMs` = fin de bloque; frame 80 ms en RSVP.
Evidence: PARTE XII.
Why not higher: ningún ensayo lleva onset de frame.
10/10 requires: L3 + `WindowedTemporalProbe` en focus v1.

**15. Reliability readiness — 1/10**
Current: `ReliabilitySource.EMPTY`; ICC desconocido; sin egreso para estudio.
Evidence: `FocusGraph.kt:156`.
Why not higher: no hay datos ni vía para obtenerlos.
10/10 requires: D-5 + V2.

**16. Validity readiness — 1/10**
Current: ningún instrumento de referencia; ningún estudio.
Evidence: PARTE V.
10/10 requires: V4–V5.

**17. Uncertainty — 2/10**
Current: `confident`/`measurementSufficient` booleanos; sin SE; margen heurístico.
Evidence: PARTE XVIII.
10/10 requires: L15 bootstrap + SE en DTO.

**18. Longitudinal change — 2/10**
Current: RCI existe pero NED; `Trends` direccional intra-día; pre/post con umbrales distintos.
Evidence: PARTE XVI, S3, X12.
Why not higher: fabrica tendencia en 3 escenarios de sim.
10/10 requires: L11 + V6.

**19. Transfer architecture — 1/10**
Current: `TransferReport` con copy acotada ✔; `NEAR_TRANSFER_INDEX` sin productor; 0 sondas no entrenadas.
Evidence: PARTE XIX.
10/10 requires: L14 + V8.

**20. Accessibility — 5/10**
Current: 48 dp ✔; luminancia E6 ✔; 12/13 sin dependencia de tono ✔; sin nodos semánticos (decisión pinneada); sin fuente escalable verificada; sin input alternativo.
Evidence: PARTE XXIII.
10/10 requires: V5 invariancia + alternativas declaradas.

**21. Technical auditability — 4/10**
Current: gates verdes 4468/0F; ledger 308 filas; `FormDescriptorV1` sólo E2; `plannedTrials` = 1; 0 analítica; decisión adaptativa no persistida.
Evidence: PARTE XXIV.
Why not higher: 12/13 sesiones no reproducibles.
10/10 requires: `RowProvenance` + analítica.

**22. Versioning — 3/10**
Current: `RngVersion` ✔, `generatorVersion` ✔; `FocusSchema.CURRENT` compartido ×3; `scoringVersion` inerte; constantes sin versión.
Evidence: PARTE XXIV.
10/10 requires: L16.

**23. Anti-false-progress — 3/10**
Current: BlindStrategyCeiling, D3, D4, NO_RESPONSES, RCI NED ✔ (10 invariantes sobreviven); 10 violados (S3, S9, S12, S15, X11, X12, …).
Evidence: §42 abajo.
10/10 requires: L7 universal + plausibilidad + `thresholdsUsed` + `deviceClass`.

**24. Readiness for exercise expansion — 2/10**
Current: taxonomía propuesta (PARTE VI) pero no aprobada; ningún registro de constructos; rúbrica nueva; `implemented == wired` impide sondas separadas.
Evidence: PARTE XXXII/XXXIII.
Why not higher: diseñar hoy repetiría los 3 fallos P1 (constructo, medida, práctica).
10/10 requires: P0 ítems 4–7 aprobados y cerrados.

**Media aritmética: 2.5/10.** No es un promedio útil (las dimensiones no son conmensurables); se reporta porque §58 lo pide. La dimensión más alta (accesibilidad 5) no compensa la más baja (personalización 1) porque el objetivo (CEO-OVERRIDE-002) es la segunda.

---

# §59 — Las 30 preguntas

1. **¿Qué significa «Enfoque» en Molvex?** Hoy: la unión de lo que 13 ejercicios entrenan (`REPO_VERIFIED`, PARTE IV). Propuesto: tres subhabilidades CORE medibles —vigilancia sostenida, inhibición de respuesta, filtrado de interferencia— más dos SECONDARY (flexibilidad, búsqueda) (PARTE VI, `RECOMMENDATION`).
2. **Componentes funcionalmente relevantes**: los 5 anteriores; RSVP/MOT/dual son CROSS sin medida oficial.
3. **Qué entrena hoy**: las 3 CORE + 2 SECONDARY + 3 CROSS, si el usuario llega a entrenar (P0: no llega con flags embarcados).
4. **Qué mide realmente**: ninguna subhabilidad. Mide exactitud CR-dominada, RT clase D y 24 derivadas sobre 3 sondas que son las tareas entrenadas, sin SE, sin fiabilidad, sin base madura.
5. **Con qué pureza**: baja. E1 acc ≈ .79 sin responder; PS ± 32 puntos por SAT; E6 dependía de tono hasta R10; 12/13 dependen de discriminación visual fina (PARTE XXIII).
6. **Métricas con señal útil**: HIT RT mediana (E1/E3), FALSE_ALARMS (E1/E3), CONGRUENCY_EFFECT (E2/E5 con ≥20/celda), SWITCH_COST (E6), SSRT (E4 con ≥30 stops, hoy no alcanzable). El resto: descriptivas o huérfanas.
7. **Cuánto es velocidad/motor/dispositivo/práctica/estrategia**: no separable hoy. Sim: dispositivo ±20 PS (S9); práctica ≡ mejora (S7 = S8); estrategia hasta PS 96 (S15); SAT 32 puntos.
8. **Cómo estima capacidad**: no la estima. PS = 0.5·acc + 0.2·speed + 0.3·stability − lapsos (rendimiento de sesión); FocusScore nunca confident; no hay θ.
9. **¿Es válida la agregación 0–100?** No como capacidad. Como feedback de sesión con banda, aceptable si se etiqueta (L10).
10. **Nivel vs crecimiento**: no distingue. `Trends` = pendiente sobre k sesiones; RCI = NED.
11. **Crecimiento vs regresión a la media**: no. Base de N=1 (S11: PS 36→88 «reliable» en what-if).
12. **Mejora vs task learning**: no. Sonda = tarea entrenada; sin práctica en RCI.
13. **Incertidumbre**: no cuantificada. Sim: sd(PS) 1.5–2.2 por ruido de muestreo a 94–160 ensayos; sd(RT) 6–14 ms; ninguna se calcula en producción.
14. **Cómo controla dificultad**: 9 ejes con lerps; escalera inicial por bins de ACCURACY; ContainImpulsivity ratchet; sin StepDown alcanzable.
15. **Cómo adapta training**: en la práctica, no (PARTE XIV).
16. **Qué personaliza realmente**: L0 (nada) con flags embarcados; L1–L3 parcial con lifecycle ON (dificultad inicial por 1 bloque, umbral de lapso pooled, arquetipo que no promociona).
17. **Qué debería personalizar**: subhabilidad prioritaria, familia, parámetros por eje, dosis, modo, presión, con `why` y `evidenceLevel` (51.9).
18. **Cómo decidir qué entrena cada usuario después**: L12 por reglas: (a) CORE con θ más bajo y SE aceptable ⇒ entrenar; (b) SE alto ⇒ medir primero; (c) respuesta al entrenamiento plana en 6 sesiones ⇒ cambiar familia/eje; (d) nunca por 1 sesión.
19. **¿Puede aprender respuesta diferencial sin sobreajustar?** Hoy no (no hay estimador). Con L8 + prior poblacional + shrinkage + mínimos de charter §11.1, sí para la pregunta «¿responde a esta familia?» (pendiente en 6+ sesiones); no para «¿qué familia le va mejor?» sin V7.
20. **Ejercicios redundantes**: E13 (E1+presión), E12 (E1+feedback), E11 (E1/E2+distractor) — 51.5.
21. **Mecanismos infracubiertos**: sonda de vigilancia real (PVT), inhibición con go ≥70 %, filtrado con ≥20/celda, PRESSURE como eje, feedback intra-ensayo.
22. **Tareas sólo para training**: E8, E9, E10, E11, E12, E13 (hasta rediseño); y todo ejercicio en modo RELAXED.
23. **Familias que pueden aspirar a measurement**: vigilancia (PVT), inhibición (go/no-go d′/c; stop-signal con carrera), filtrado (flanker) — tras V2.
24. **Probes necesarios**: 1 no entrenada por CORE (L14): PVT 5 min; go/no-go 80/20 con d′; flanker de referencia con 40/celda.
25. **Datos a guardar** (resúmenes, nunca ensayos): por fila `SessionMode`, `thresholdsUsed`, `DeviceContext`, `formSeed+rngVersion`, `probeId`, `administrationIndex`, `repeatsEarlierForm`, `plannedTrials` real, `metricVersion`, `value: Double?` + `absenceReason` + `n` + `se`, `timingClass`, `adaptationDecision+version`.
26. **Sesgos del hardware móvil**: +30–110 ms de pipeline táctil, dwell UP 50–150 ms (`INFERRED`), refresh 60/120 Hz, ratón vs táctil, frame 80 ms mínimo en RSVP, clase D universal.
27. **False-progress paths**: 12 (PARTE XXVI): X1 sandbag, X2 reflejo, X3 no-responder, X4 quema de formas, X5 dispositivo, X6 slowing E4, X7 single-tap E9, X8 plantilla, X9 intra-día, X11 0.0 en disco, X12 Trends; + S3 (umbral pre/post), S11 (RTM).
28. **Qué arreglar antes de más ejercicios**: P0 ítems 1–8 (PARTE XXX).
29. **Qué validar con humanos**: V0 (instrumento), V1 (timing), V2 (fiabilidad) como mínimo antes de cualquier claim; V4–V6 antes de «entrena».
30. **Información exacta para el siguiente diseñador**: PARTE XXXII completa + rúbrica XXXIII + decisiones D-1…D-10 resueltas.

---

# §49 — Criterios para el futuro catálogo (20 preguntas por ejercicio)

El diseñador responde las 20 en una ficha por ejercicio; la rúbrica (XXXIII) puntúa las respuestas. Restricción derivada de esta auditoría por pregunta:

1. **Operación cognitiva**: nombrada en términos de la taxonomía (51.1); «atención» no vale.
2. **Subhabilidad primaria**: una sola CORE/SECONDARY.
3. **Secundarias**: lista con dirección esperada de contaminación.
4. **Constante**: rareza de señal, ISI, tamaño de estímulo, contraste — todo lo que no sea el eje gobernante.
5. **Manipulable**: exactamente un eje gobernante + ejes de presentación declarados inertes para la medida.
6. **Mayor dificultad**: definida sobre el eje gobernante con rango efectivo pinneado (E1 hoy falla).
7. **Adaptable individualmente**: eje gobernante y dosis; nunca la rareza de señal en una sonda.
8. **Telemetría**: por ensayo (onset frame, DOWN, condición), por bloque (forma, versión, modo, `thresholdsUsed`).
9. **Contaminantes**: motor (RT), percepción (contraste/tamaño), color, lectura, memoria, dispositivo.
10. **Trucos**: techo de estrategia barata calculado (`max(p,1−p)`, single-tap, ralentización) y detector con `COVERAGE`.
11. **Memorización**: paramétrico + ≥4 formas + semillas reservadas.
12. **Escala principiante→élite**: sim de 40 sesiones sin techo; rango de eje que lo permita.
13. **Forma de training**: objetivo de acierto ~80 %, feedback declarado, `VariantSpec` relajado.
14. **¿Produce probes?**: sólo si existe una versión con paradigma distinto del entrenado; si no, «no».
15. **¿Familia paralela para medición?**: sí para toda CORE (L14).
16. **Dependencia motora**: RT crítico (clase ≥C requerida) o no.
17. **Dependencia perceptiva**: luminancia ≥3:1, tamaño ≥48 dp, sin tono.
18. **Riesgo de práctica**: alto si estrategia aprendible; medir techo por prefijo/posición como en Emociones.
19. **Riesgo de device bias**: alto si RT oficial; bajo si exactitud bajo deadline.
20. **Transferencia hipotetizada**: sólo cercana, a familia nombrada; «vida diaria» prohibido.

---

# §42 — Invariantes falsados (consolidado)

| Invariante | Resultado | Evidencia | Qué lo rompe |
|---|---|---|---|
| Más errores no mejoran capacidad | **VIOLADO** (escalera inicial) | X3: no responder ⇒ ACC .79 ⇒ PL 0.20 | ACCURACY CR-dominada |
| Tiempos imposibles no mejoran score | **VIOLADO** (condicionado) | S15: 190 ms y accuracy sobre el techo ⇒ AG NONE, PS 96 (reimpl); admitido sólo con el sensor de ciclo de vida ON | D4 mide fracción <150 ms sobre todos los ensayos; D3 exige además accuracy ≈ techo ciego; sin plausibilidad de RT (K8 3/3) |
| Repetir indefinidamente no produce crecimiento oficial | sobrevive (RCI NED) / **VIOLADO** en what-if | S7 | sin práctica en RCI; sonda = entrenada |
| Dispositivo más rápido ≠ mejora | **VIOLADO** | S9: Trends DECLINING/IMPROVING | sin `deviceClass` |
| XP no cambia capacidad | sobrevive | no hay XP en Focus v1 | — |
| Más sesiones ≠ capacidad | sobrevive (RCI) / **VIOLADO** (`OvertrainingGuard`, Trends leen conteo) | PARTE XX | pooled ACCURACY |
| missing ≠ zero | sobrevive en memoria / **VIOLADO** en disco | X11 | `withdrawn` → 0.0 |
| Un bloque no evaluado no reduce otro | sobrevive | `weightedAverage` por celda (R12) | — |
| Nueva versión no hereda comparabilidad | **VIOLADO** | `scoringVersion` inerte; `FocusSchema` compartido | sin comparador |
| Training difficulty ≠ ability | **VIOLADO** (escalera + arquetipo) | `initialDifficulty`, `ArchetypeInference` | dificultad inicial = bins de acc |
| UI 0–100 sin más precisión que la estimación | **VIOLADO** (si flag ON) | PS entero sin banda; sd 1.5–2.2 | sin SE |
| Un ejercicio no domina una subhabilidad | **VIOLADO** | 1 sonda por CORE = 1 ejercicio | `ProbeBank` |
| Un día no define cambio longitudinal | **VIOLADO** | X12 | `Trends` sin días distintos |
| Adaptation no optimiza el score oficial | sobrevive (no optimiza nada) / riesgo latente | ContainImpulsivity sube dificultad de E3/E4 y PS baja | — |
| Probe degradado no entra como oficial | sobrevive para el probe / **VIOLADO** para calibración | S12: umbrales persisten con probe denegado | calibración sin gate |
| **Enfoque-específico**: accuracy de señal rara ≠ vigilancia | **VIOLADO** | E1 techo .79 | CR-dominada |
| **Enfoque-específico**: forma de base no se repite | **VIOLADO** | X4 | `releaseProbe` sólo en abort |
| **Enfoque-específico**: umbral de lapso fijo entre pre y post | **VIOLADO** | S3 | recalibración por sesión |
| **Enfoque-específico**: SSRT sólo con carrera integrada | **VIOLADO** (`INFERRED`) | X6 | sin gate de slowing |
| **Enfoque-específico**: una sesión no medible no persiste dificultad | **VIOLADO** | `finishSession` escribe `nextDifficulty` incondicional | — |
| **Enfoque-específico**: estrategia ciega detectada | sobrevive | BlindStrategyCeiling (S2, S4, X10) | — |
| **Enfoque-específico**: sin respuestas ⇒ ausencia tipada | sobrevive (probe) | `NO_RESPONSES` | — |

Balance: 8 sobreviven, 15 violados (contando dobles). Lo que sobrevive lo hace por **R9–R15** (gates recientes con mutación): la dirección del trabajo previo es correcta; el alcance no cubre calibración, persistencia ni dispositivo.

---

# §62 — La cadena, eslabón por eslabón

| Eslabón | ¿Puede Molvex responderlo hoy? | Qué falta | Por qué importa | Riesgo | Cómo resolver | Evidencia necesaria | Orden |
|---|---|---|---|---|---|---|---|
| OBJETIVO DEL USUARIO | **No** | `UserGoal` no existe | Sin objetivo no hay prescripción, sólo rotación | Recomendar E1 a todos | Campo + 3–5 valores + copy | ninguna (diseño) | 1 |
| PERFIL ACTUAL | **No** | θ+SE por subhabilidad; base madura; `deviceClass` | Todo lo demás se compara contra esto | Base de N=1 corrupta define al usuario | L8 SDT + D-1 + L16 | V0, V2 | 2 |
| SUBHABILIDAD PRIORITARIA | **No** | Regla de prioridad sobre θ/SE | Sin ella «prioritario» = «último no jugado» | Entrenar lo que ya va bien | L12 tabla de reglas | sim + V0 | 3 |
| MECANISMO COGNITIVO OBJETIVO | **Parcial** | `ConstructRegistry` | Sin mecanismo, la familia se elige por estética | Skins | L1 | ninguna | 1 |
| FAMILIA DE ENTRENAMIENTO | **Parcial** (13 ejercicios existen) | mapa familia→subhabilidad aprobado; E12/E13 retirados | Falsa variedad | Catálogo inflado | D-7, D-8, 51.5 | ninguna | 1 |
| PARÁMETROS PERSONALIZADOS | **No** | eje gobernante con rango; escalera | Dificultad = capacidad hoy | Ratchet, techo | L5/L6 | sim 40 sesiones | 4 |
| DOSIS | **Parcial** (plantillas) | dosis derivada de pisos; intervalo 48 h | 55 no mide | Sonda subpotente | L13 | V7 | 4 |
| TELEMETRÍA | **Parcial** (resúmenes cifrados) | `RowProvenance`, `SessionMode`, `Double?`, timing por ensayo, analítica | Sin esto nada es auditable ni comparable | Ciego en producción; 0.0 en disco | L3/L16 + 8 eventos | ninguna | 1 |
| ESTIMACIÓN | **No** | estimador (d′/θ) | PS ≠ capacidad | Feedback de sesión leído como nivel | L8 | V2 | 5 |
| INCERTIDUMBRE | **No** | SE por métrica | Sin banda todo cambio parece real | RCI/Trends fabricados | L15 bootstrap | ninguna | 2 |
| RESPUESTA AL ENTRENAMIENTO | **No** | pendiente por familia sobre ≥6 sesiones con SE | Sin ella no hay «este estímulo le sirve» | Sobre-reacción a 1 sesión | L11/L12 | V6 | 6 |
| REEVALUACIÓN | **Parcial** (sondas, formas, RCI NED) | sonda no entrenada; ≥48 h; `formIsFresh` ambos carriles; SEM | Práctica ≡ mejora | S7 | L14 + L11 | V2, V3 | 5 |
| SIGUIENTE PRESCRIPCIÓN | **No** | todo lo anterior | Cierre del bucle | Rotación | L12 | V6 | 7 |

**Respuesta honesta a la pregunta definitiva** («¿Qué debería entrenar este usuario ahora, mediante qué demanda, a qué dificultad, cuánto tiempo, por qué y qué evidencia demostraría mejora?»): Molvex no puede responderla hoy para ningún usuario. Puede responder «qué ejercicio sigue» (rotación por recencia) y «cuánto dura» (plantilla elegida por el usuario). El orden para poder responderla: (1) eslabones 1 — registro de constructos, familia aprobada, telemetría, `UserGoal` (P0, sin humanos); (2) perfil + incertidumbre (L8/L15 + V0/V2); (3) prioridad (L12); (4) parámetros + dosis (L5/L6/L13); (5) estimación + reevaluación (L14, V3); (6) respuesta al entrenamiento (V6); (7) prescripción cerrada. El catálogo definitivo se diseña después de (1) y antes de (2); se valida después de (6).

---
