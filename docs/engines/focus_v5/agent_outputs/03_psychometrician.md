# Orden 1 — psychometrician (2026-07-19) — output íntegro (gates S1/S9)

> Integrado en: ADR-FOCUS-PURPOSE/-EVIDENCE/-SCORING/-TIMING, GATE_CATALOG_V5, plan §5 S1/S8/S9, S0 oráculo.

## §0 HALLAZGO TRANSVERSAL (CONTRADICTED, bloqueante de S1-CONTRACTS)
El repo tiene DOS autoridades de propósito contradictorias VIVAS:
- `SessionMeasurementPurpose` (8v: MEASUREMENT, CALIBRATION, INITIAL_ASSESSMENT, TRAINING_FREE,
  TRAINING_RECOMMENDED, PRACTICE, DEBUG, UNKNOWN) — `ProfileMeasurementEligibility.kt:10-19`, gate fail-closed
  real (`measurementEligible` :38-51): TRAINING_FREE **NO** elegible.
- `CognitiveSessionEnums.SessionPurpose` (6v) — `CognitiveSessionEnums.kt:15-34` con
  `mayContributeToProfile()` (:25): TRAINING_FREE **SÍ** contribuye (`!= PRACTICE && != DEBUG`).
El KDoc del 8v lo admite ("Deliberately NOT the same as… too permissive"). Hoy no explota solo porque
`FocusProfileSource.kt:55-61` es fail-closed. Un consumidor v2 que lea `mayContributeToProfile()` puentearía el
gate. **La tarea NO es proyectar 6→8: es colapsar a UNA autoridad y retirar/renombrar la segunda función.**

## A) ADR-FOCUS-PURPOSE — opciones (ADR_REQUIRED; decisión antes de S1)
Marco: separar (1) gate de elegibilidad (pequeño, sellado, fail-closed) de (2) lane/taxonomía de sesión
(tutorial/familiarización/training/calibración/probe/research: gobierna adaptación/feedback/washout, NO elegibilidad).
La firma del gate acepta SOLO el tipo autoritativo.
**Regla "no aplicable ≠ UNKNOWN" (obligatoria):** UNKNOWN = solo "propósito no sellado/ausente/cambiado"
(defecto, fail-closed). El gate devuelve sealed de 3 vías:
`Eligible(purpose) | NotEligibleByPurpose(purpose) | Indeterminate(reason)` — "no aplicable" es un valor
EXPLÍCITO distinto de "desconocido".
- **Opción 1** — el 8v ES la autoridad; Focus usa subconjunto (TUTORIAL/FAMILIARIZATION→PRACTICE colapsado).
  Riesgo: pierde FAMILIARIZATION (relevante para washout/exposición). Lossy.
- **Opción 2** — enum v2 propio + proyección sellada al 8v (la de V4). **RECHAZADA por P1-8** (dos verdades,
  drift de tabla, residuo a UNKNOWN prohibido). Registrada solo para trazabilidad.
- **Opción 3 (RECOMENDADA)** — extender el 8v con TUTORIAL, FAMILIARIZATION, RESEARCH_ONLY (consumer-inventory);
  UN solo enum, CERO proyección; set elegible sin cambio {MEASUREMENT, CALIBRATION, INITIAL_ASSESSMENT};
  gate devuelve el disposition de 3 vías; **retirar/renombrar `mayContributeToProfile()`** en el mismo cambio;
  decoder: enum desconocido → Indeterminate (fail-closed), nunca crash ni elegible; migración aditiva.
  Blast radius: FocusProfileSource, LogicProfileSource + tests (manejable).
Justificación: P1-8 literal; preserva FAMILIARIZATION (charter §13.1); sellado pre-trial por tipo (charter
§7.1/§7.3: "el modo entrenamiento nunca se eleva retrospectivamente a oficial").

## B) CONTRATO DE 8 ESTADOS UX (presentación del Performance Score por faceta)
**Corrección al mandato (VERIFIED):** doc 06 §7 del pack YA lista los 8 estados INCLUYENDO RETIRED_VERSION.
**4 ejes de estado distintos, no conflables:** presentación (8, este contrato) · evidencia por-sesión
(`SessionEvidenceState`: TRAINING_ONLY/PROVISIONAL/OFFICIAL_ELIGIBLE/INVALIDATED) · calibración (charter §11.4,
7v) · Growth Index (charter §10.3, 9v, SCO-001 separado).
Los 8: CALIBRATING ("—", nunca 0) · INSUFFICIENT_DATA ("—"; bajo piso de muestra, distinto de "en progreso") ·
PROVISIONAL (banda amplia + "estimación preliminar", no-comparable-a-oficial) · CURRENT (estimación + cobertura/
confianza/frescura/versión; "cumple los gates de la versión, no significa verdad") · STALE (último valor +
"reevaluación breve"; NUNCA baja por inactividad) · NON_COMPARABLE (cambio versión/dispositivo/modalidad sin
puente; no promedia) · WITHHELD_TECHNICAL ("—"; T0/T1 en RT-crítico, integridad, lineage) · RETIRED_VERSION
(serie histórica preservada; nunca sobrescribe).
Transiciones por EVENTO (CalibrationComplete, VersionGatesPass [requiere V3+ADR], FreshnessExpired,
LineageBreak, TechnicalGateFail, VersionRetired, FreshEvidence, bridge-ADR). Invariantes: solo CURRENT/
PROVISIONAL renderizan número; CURRENT puede bajar (SCO-002); series nunca sobrescritas (charter §21.2).
**El estado v1-oculto de S0 NO es un 9º estado**: v1 nunca alcanzó CURRENT → WITHHELD_TECHNICAL o
no-renderizado bajo flag OFF; NO RETIRED_VERSION (reservado a versiones que estuvieron vivas).

## C) ADR-FOCUS-EVIDENCE — 4 ejes ORTOGONALES (ninguno deriva de otro)
- **Elegibilidad**: purpose (§A) + lineage completo + timing eligible. Reutiliza ProfileMeasurementEligibility
  (→disposition), SessionEvidenceState/EvidenceRouteEligibility, ExclusionReason (13). NUEVO: carrier de
  purpose+lineage en el modelo de sesión Focus (SessionResult hoy no lo transporta).
- **Cobertura**: conteos + días distintos + span (day-cap + span-floor) + ≥2 métodos/faceta + cap 50%/tarea.
  Reutiliza ExerciseScaleEvidence facts-only. NUEVO: gate de cobertura de faceta (hoy ABSENT).
- **Confidence**: estabilidad/error — JAMÁS de sufficiency (`ProfileUpdateGate.kt:40-42`). NUEVO: modelo de
  error real (SEM/split-half); hasta entonces UNKNOWN/baja, nunca inventada. CV-confidence = SUPERSEDED.
- **Lineage**: 9 versiones + ComparabilityVerdict (NotEstablished default ⇒ PROHIBIDO promediar, impuesto por
  AUSENCIA del input). Reutiliza MeasurementLineage/ScoreScaleKey. Hoy PARTIAL (solo E2 FormDescriptorV1).
Reglas: ausencia → Missingness(reason,scope) tipado, nunca 0 NI máximo (learning 2026-07-15 ambos sentidos);
VALID⇒sufficiency=1.0, confidence baja mantiene EMERGING (2026-06-28); evidence facts-only nunca importa perfil
(evidenceBoundaryScanner).

## D) ORDEN CIENTÍFICO S8-S9-S11 (P1-10 estricto)
simulación → alpha → recogida repetida → V3 → beta → RCT → transferencia → invariancia. Cero fórmula pre-datos.
(1) SIMULACIÓN pre-humanos: suite adversarial BLOQUEANTE (masher, esperar-al-final, periodicidad, side-bias,
cherry-picker con gate de tasa de respuesta, lower-is-better → null+reason) — cada adversario con MITAD
DISCRIMINANTE honesta; distribuciones sintéticas (floor/ceiling, colas RT, missing-by-design); golden de
invarianza (seed+versión ⇒ plan byte-igual; mutación ⇒ rechazo); recuperación de parámetros M0-M5.
(2) SHADOW RIVALES (≥3, nunca expuestos): A descriptivo robusto (mediana/d′/criterio por tarea — candidato
interino) · B faceta anclada con penalizaciones · C jerárquico trial-level/GLMM. **F compuesto unitario
PROHIBIDO exponer** (= anatomía del v1 FocusScore). **D IRT/E DDM = RESEARCH_ONLY para Focus** (timing
bloqueado B-F-04 + identificabilidad) — matiz explícito a la doctrina del skill.
(3) Con datos reales — computable a N≈20: split-half intra-sesión descriptivo, floor/ceiling, curvas de
práctica, distribuciones (SOLO QC). NO computable/prohibido a N≈20: α/ICC como validación, factorial, M0-M5,
normas, DIF, test-retest r (CI ±0.4), SEM/MDC/RCI individual, eficacia. Precisión utilizable → V3/beta con N
por simulación.
(4) Solo entonces ADR-FOCUS-SCORING propone fórmula (gate: datos + ADR-MSR-002; barrera: input-manifest
allowlist + "solo el descriptivo A es productivo hasta el ADR").
(5) V3 GO: ≥2 métodos/faceta estables + precisión por-uso + formas conectadas + práctica modelada +
invariancia suficiente. Si M0 gana → NO-GO subscore (queda C0). Tras V3 NO: transferencia (V4-V6),
percentiles/ranking (V7), causalidad, "mejora tu Focus".
Umbrales interinos DESIGNED_NOT_RUN: 5/3d/48h (revisión ADR-MSR-003), cap 50% (ADR-MSR-001), fiabilidad≥.70
RECHAZADO como gate universal (precisión por-uso), cap diario/washout ADR_REQUIRED, T2/T3 BLOCKED (banco físico).

## E) VOCABULARIO DE GATES (dos familias DISJUNTAS)
- Científica V0-V8 (doc 09 §2 verbatim, sin debilitar; ninguna verde por planificación).
- Técnica T-*: `T-PLAN` (artefacto) · `T-DR` (design review; precede ejecución) · `T-S0` (contención v1;
  precondición de EXPOSICIÓN) · `T-ADV` (suite adversarial BLOQUEANTE de slice tracer; **NO es V3** — sintético
  autoriza exposición/wiring; V3 es empírico humano) · `T-Sn` (por slice) · `T-SCI-n` (barrera mecánica del
  requisito; apunta al V-gate, no lo sustituye).
**Correcciones obligatorias a V4:** G-S2→V3 PROHIBIDO; G-V1/G-V2/G-V3 HUÉRFANOS confirmados (usados en CSV
FOC-SCI-004/FOC-MSR-009/012/FOC-TRN-006, indefinidos en catálogo) → sustituir por "requiere V_n PASSED".
Cierre de 5 campos de V4 ENDOSADO íntegro.

## F) RT/TIMING (marco para orden 2)
Clases DERIVADAS de medición (nunca declaradas): T0 desconocida (sin RT) · T1 monotónico no-frame (accuracy sí;
RT descriptivo local) · T2 dispositivo físicamente caracterizado (RT dentro de clase; requiere banco físico —
hoy inalcanzable) · T3 invariancia/bridge (V7-adyacente). Señal autoritativa existente:
CHOREOGRAPHER_FRAME+NANOS_NATIVE (`ScoreStateMachine.timebaseTrusted:95`, precedente Speed). CRITICIDAD por
dossier versionada fail-closed a UNKNOWN (`RtCriticalityRegistry.criticalityOf:59-62`).
Regla: T0/T1 en métrica RT-crítica ⇒ entrena, NO alimenta score oficial; accuracy SE PRESERVA; UX →
WITHHELD_TECHNICAL. Por faceta: SUSTAIN accuracy-based = omisiones/FA/d′/criterio/estabilidad accuracy;
RT-crítico = RT-CV/lapsos-PVT/pendiente RT. CONSTRAIN accuracy = por-condición/d′/intrusiones/congruencia en
ACCURACY; RT-crítico = congruencia en ms/RT-CV. RESTRAIN accuracy = comisiones/omisiones/criterio; RT-crítico
= SSRT/stop-RT. **⇒ el score honesto de E2 debe ser accuracy/sensibilidad-based; RT excluido hasta T2.**

## G) S0 MEASUREMENT-SIDE — oráculo adversarial (gate T-ADV, precondición de re-exponer números)
MB-ENG-0007/0008 verificados (ledger :135-136). ORACLE score_must_not_reward_gaming (E2 flanker, ventana
900-1500ms, n=55/perfil): HONEST (acc~0.92, RTs 400-650, lapsos "PVT", 0 azar) · MASHER (una tecla/metronómico,
acc~0.50, 0 MISS, RT~250 constante, 0 lapsos-500ms) · WAITER (fin de ventana, acc azar) · SIDE_BIAS.
Aserciones: A1 score(HONEST)>score(MASHER) [hoy FALLA: 44<70] · A2 masher no alcanza VALID/alto · A3
WAITER/SIDE_BIAS < HONEST · **A4 mitad discriminante: HONEST sigue ALTO y confident** (sin A4 un fix que clampa
todo a 0 pasaría) · A5 el score NO consume RT_CV ni lapsos-por-umbral (input-manifest allowlist) · A6
lower-is-better: congruencia con accuracy a azar → null+reason, jamás "0 perfecto".
Checklist antes de re-exponer CUALQUIER número: T-ADV verde · RT excluido salvo ≥T2 (⇒ accuracy-based) ·
MB-ENG-0008 cortado (ArchetypeInference sin RT_CV/LAPSES ni dificultad/gating bajo FIXED) · metadata completa
(cobertura/incertidumbre/reasons/versión + 1 de los 8 estados) · sin fórmula pre-datos (solo descriptivo
por-tarea C0; un 0-100 de capacidad exige V3).
Alcance S0: (i) hide reversible del compuesto v1 (flag OFF); (ii) cortar la 2ª puerta archetype; (iii) aterrizar
el oráculo como test ROJO documentando por qué v1 está oculto y VERDE-precondición de v2. Sin fórmula nueva.

## Clasificaciones clave
CONTRADICTED: doble autoridad de propósito (`ProfileMeasurementEligibility.kt:6-8,38` vs
`CognitiveSessionEnums.kt:25`); G-V1/2/3 huérfanos; proyección V4 a UNKNOWN. VERIFIED: 8v gate; D1 fail-closed;
capa evidence facts-only; sufficiency⊥confidence; G-S2→V3 en V4 (a corregir); doc 06 §7 = 8 estados.
REPORTED: masher ~70 vs ~44 (magnitud pendiente device data); pack = EXTERNAL_UNVERIFIED anclado a 2.0.0
(re-verificar vs 2.0.2 antes de claims >C0). Riesgos: grieta §0 (bloqueante S1) · confidence sin modelo de
error · timing B-F-04 · N≈20 no valida nada · gate de cobertura de faceta ausente.
