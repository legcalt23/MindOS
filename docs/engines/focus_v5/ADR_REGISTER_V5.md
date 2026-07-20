# ADR_REGISTER_V5 — Registro de ADRs del motor de Enfoque

```
DOCUMENT_STATUS = DRAFT · EXECUTION_AUTHORIZATION = NOT_GRANTED
BASE_SNAPSHOT = 27849450 · CANON = context_pack 2.0.2
Regla: un ADR aquí presenta OPCIONES y una recomendación; NINGUNO está decidido. La decisión se registra
en 09_DECISION_LOG.md (formato §22) por el owner indicado, ANTES del gate que lo bloquea. Ningún ADR de este
registro puede decidirse "por implementación". Mapeo al backlog canónico (09 §19) indicado por ADR.
```

## Índice y estado

| ADR | Pregunta | Estado | Owner de decisión | Bloquea | Canon relacionado |
|---|---|---|---|---|---|
| ADR-FOCUS-CONSTRUCT | modelo de constructo y qué subhabilidades muestran subscore | OPTIONS_READY | CEO (producto) + revisión científica | S2→S3 copy; S9 subscores | ADR-SCI-001; CAT-002 |
| ADR-FOCUS-PURPOSE | autoridad única de propósito, serialización, disposition | OPTIONS_READY (rec: Opción 3) | CEO (toca enum compartido del perfil) | S1 | — (grieta nueva) |
| ADR-FOCUS-EVIDENCE | ejes ortogonales de evidencia + gate de cobertura de faceta | OPTIONS_READY (estructura APPROVED-por-canon; umbrales ADR) | psychometrician propone / CEO ratifica | S1, S8 | ADR-SCI-003; SCO-012/013 |
| ADR-FOCUS-DATA | evento mínimo, raw/resumen, retención, privacidad (C-06) | OPTIONS_READY (i/ii/iii) | **CEO como propietario+responsable de datos** | S1 (toda escritura por-trial); outcomes replay | ADR-DATA-001/002; PRI-* |
| ADR-FOCUS-SESSION-STATE | estado runtime Focus vs `SessionStatus` global | OPTIONS_READY (rec: local, patrón cognitive/) | kmp-architect propone / CEO ratifica | S1, S3 | — |
| ADR-FOCUS-TIMING | clases T0-T3, derivación, RT eligibility | OPTIONS_READY (marco APPROVED; umbrales DESIGNED_NOT_RUN) | psychometrician + mobile-performance / CEO | S3 (captura), S5 (gate), S8 (RT oficial) | ADR-SCI-008; TEC-006 |
| ADR-FOCUS-MIGRATION | schema/versioning/old-new readers/rollback | OPTIONS_READY | kmp-architect / CEO | S1 y toda slice con persistencia | TEC-010/011 |
| ADR-FOCUS-SCORING | fórmula de score de faceta | **BLOCKED_BY_DESIGN** — solo tras simulación (S8-pre) + datos humanos repetidos (S8) + análisis (S9) | psychometrician propone / CEO ratifica; NUNCA pre-datos | exponer cualquier número de capacidad | ADR-SCI-002/003; ADR-MSR-002 |
| (aux) ADR-FOCUS-PLATFORM-MIN | resolver CONTRADICTED TEC-002 (API 26) vs repo (minSdk 29) | OPTIONS_READY | **CEO exclusivo** (decisión de producto) | S5 | TEC-002 |

---

## ADR-FOCUS-CONSTRUCT

**Pregunta:** ¿qué estructura de constructo gobierna Focus v2 y qué subhabilidades pueden mostrar subscore?
**Hechos gobernantes:** las CINCO subhabilidades del PRD §6.1 (sostenida, selectiva, inhibición de
distractores, control atencional, estabilidad bajo tiempo) son APPROVED como organización de producto
(CAT-002: "los motores visibles no implican factores independientes"). El dictamen de efficacy-guardian
(agent_outputs/02) las degrada COMO FACTORES: control atencional = CONTRADICTED como subscore (E001 vs
E002/E005, veredicto de existencia CONFLICTED); estabilidad bajo tiempo = propiedad/dinámica de SUSTAIN, no
capacidad. RESTRAIN (go/no-go, stop-signal) NO pertenece a las 5 (E051: freno motor ≠ inhibición de
interferencia). SUSTAIN/CONSTRAIN/RESTRAIN del pack = hipótesis rivales M1 dentro del harness M0-M5.
**Opciones:**
- (a) Navegación 5 / medición 3+propiedad — las 5 se muestran como ETIQUETAS de navegación y organización de
  ejercicios; los subscores futuros (post-V3) solo pueden existir para sostenida, selectiva e inhibición de
  distractores; control atencional y estabilidad se muestran como composite hipotético etiquetado o dinámica,
  jamás como subscore. **RECOMENDADA** (única compatible con el dictamen y con CAT-002 sin reabrir el PRD).
- (b) Navegación 5 / medición decidida por M0-M5 en beta — no fijar nada hasta datos; riesgo: la UI de S6+
  no puede diseñar jerarquía de información.
- (c) Reabrir el PRD a 3 facetas — RECHAZADA (contradice decisión cerrada del mandato: las 5 permanecen).
**Consecuencia inmediata (cualquier opción):** en C0 ningún subscore se muestra; el copy usa las 5 como
categorías de práctica. La estructura latente es 100% HYPOTHESIS hasta S9/beta (M0-M5, ≥2 métodos por faceta).
**Migración/rollback:** ninguna (decisión de contrato de copy/registro); el registro de constructo v2 lleva
`constructModelVersion` para poder re-etiquetar sin reescribir historia.

## ADR-FOCUS-PURPOSE

**Pregunta:** ¿cuál es la ÚNICA autoridad de propósito de sesión y cómo se representa "no aplicable al perfil"?
**Hecho gobernante (CONTRADICTED vivo, bloqueante de S1):** hoy hay DOS autoridades:
`SessionMeasurementPurpose` (8v, `ProfileMeasurementEligibility.kt:10-51`, TRAINING_FREE NO elegible) y
`CognitiveSessionEnums.SessionPurpose.mayContributeToProfile()` (`CognitiveSessionEnums.kt:25`, TRAINING_FREE
SÍ). Cualquier solución debe COLAPSAR a una y desactivar la otra opinión.
**Opciones (detalle en agent_outputs/03 §A):**
- (1) El 8v ES la autoridad; Focus usa subconjunto (FAMILIARIZATION colapsa a PRACTICE — lossy para washout).
- (2) Enum v2 propio + proyección many-to-one al 8v — **RECHAZADA** (P1-8: dos enums ≠ una autoridad;
  la V4 además enviaba el residuo a UNKNOWN, prohibido).
- (3) **RECOMENDADA** — extender el 8v con `TUTORIAL`, `FAMILIARIZATION`, `RESEARCH_ONLY` (consumer-inventory
  previo); UN enum, cero proyección; gate devuelve sealed
  `Eligible | NotEligibleByPurpose | Indeterminate(reason)`; UNKNOWN queda reservado a "no sellado/ausente/
  cambiado" (defecto → Indeterminate → deny); **retirar/renombrar `mayContributeToProfile()`** en el mismo
  cambio; deserialización de valor desconocido → Indeterminate (fail-closed).
**Serialización:** el enum ya persiste por nombre; añadir miembros es aditivo; reader antiguo ante valor nuevo
= Indeterminate (nunca crash, nunca elegible). **Migración:** ninguna fila histórica cambia de significado.
**Rollback:** los miembros nuevos dejan de emitirse; los persistidos caen a Indeterminate (fail-closed).
**Invariante de contrato:** el purpose se SELLA antes del primer trial; cambio mid-session ⇒ sesión
Indeterminate para evidencia (práctica conservada); "TRAINING nunca se eleva retroactivamente a oficial".

## ADR-FOCUS-EVIDENCE

**Pregunta:** ¿cómo se estructura la evidencia de Focus v2 sin fabricar señal?
**Estructura (anclada a canon y repo, APPROVED como forma):** cuatro ejes ORTOGONALES — elegibilidad
(purpose+lineage+timing), cobertura (conteos/días/span + variedad), confidence (estabilidad; JAMÁS derivada de
sufficiency — `ProfileUpdateGate.kt:40-58`, learning 2026-06-28), lineage (9 versiones + comparabilidad;
`ComparabilityNotEstablished` por defecto ⇒ prohibido promediar, impuesto por AUSENCIA del input — S6R3-001).
Reutiliza ÍNTEGRA la capa `core.session.cognitive.evidence` (facts-only, barrera evidenceBoundaryScanner).
**Queda a decisión (ADR_REQUIRED):** (a) el gate de cobertura de faceta (≥2 métodos por faceta; cap 50% de
información por tarea — umbral interino con revisión empírica, ADR-MSR-001/-020 del pack como referencia
EXTERNAL); (b) el modelo de error para confidence (SEM/split-half real; hasta entonces confidence = baja/
UNKNOWN, nunca fabricada; CV-confidence SUPERSEDED por Decision Log §18).
**Ausencia:** siempre `Missingness(reason, scope)` tipado; test en AMBOS sentidos (ausencia que fabrica mínimo
Y máximo — learning 2026-07-15).
**Migración/rollback:** tipos nuevos aditivos en el namespace evidence; rollback = no-emisión.

## ADR-FOCUS-DATA (resolución de C-06)

**Pregunta:** ¿qué se persiste por sesión de Focus v2 — resúmenes, journal replay-complete, o tiered — y con
qué retención?
**Hecho gobernante:** CLAUDE.md §5 = "solo resúmenes, nunca trials crudos" (POLICY viva). El pack pide raw
(EXTERNAL_UNVERIFIED). La reproducibilidad de FORMA ya existe sin raw (`ReproducibleSessionIdentity` +
`FormDescriptorV1`): re-genera estímulos, NO respuestas.
**Opciones (análisis completo en agent_outputs/04 §A):** (i) NO-RAW (statu quo APPROVED; outcomes
replay-dependientes BLOCKED explícitos: re-scoring retroactivo, auditoría por-trial, forense) ·
(ii) journal mínimo replay-complete (schema campo-a-campo justificado; cifrado AccountBound; retención
acotada; DoS-cap obligatorio; fail-closed WITHHELD_TECHNICAL si falla) · (iii) TIERED — resúmenes por defecto
+ journal SOLO en sesiones con purpose de medición (volumen capado por dosis diaria; purpose indeterminable ⇒
cae a (i)).
**Decisión = CEO como propietario y responsable de datos** (rol DPO efectivo en una organización unipersonal;
si se designa un DPO/asesor legal externo antes de alpha, co-firma). **ANTES de S1-exit; ninguna escritura
por-trial se diseña en firme sin ella.** Regla cerrada del mandato: si se deniega el dato mínimo
replay-complete ⇒ los outcomes dependientes quedan BLOCKED con razón explícita (no degradación silenciosa).
**Honestidad obligatoria en la decisión:** un journal local NO es evidencia anti-manipulación (dispositivo del
usuario, sin firma/sync); su valor es auditoría de coherencia interna y re-scoring.
**Retención:** la ventana concreta es parte de la decisión (PRD §18.3 hoy = objetivos no vinculantes);
requiere `retentionPolicyVersion` + purga por edad (GAP declarado).
**Anexo obligatorio (contrato de datos completo P2-22):** tabla de 14 ejes en agent_outputs/04 §B — cada eje
mapeado a mecanismo existente (file:line) o GAP asignado a slice (orden/`sequenceNo`→**S8** [corrección R6-V5
#25: el journal vive en S8, no en S4]; DoS-cap→S1 si se elige (ii)/(iii); retención→esta ADR; consentimiento
de scopes→S1; k-anonimato→pre-cualquier-sync).
**Protocolo de REAPERTURA [R6-V5 P1-4]:** esta decisión se toma ANTES de S1-exit y solo puede cambiar mediante
una NUEVA entrada de decisión (09 §22) con regla de migración explícita: (a) los datos ya recolectados
CONSERVAN el régimen bajo el que se recolectaron (etiqueta de régimen por fila — jamás re-etiquetado
retroactivo); (b) cambio (ii)/(iii)→(i): el journal existente se purga por su retención declarada y los
outcomes replay-dependientes vuelven a BLOCKED desde la fecha del cambio; (c) cambio (i)→(ii)/(iii): el
journal solo cubre sesiones POSTERIORES a la decisión (nunca reconstrucción retroactiva); (d) todo cambio
re-dispara la revisión de privacidad y el DPIA-equivalente.

## ADR-FOCUS-SESSION-STATE

**Pregunta:** ¿Focus v2 extiende `SessionStatus` global o define estado de runtime propio?
**Hechos:** `SessionStatus {STARTED, CHECKPOINT, FINALIZED, ABANDONED}` (`core/session/SessionRecord.kt:17`)
es transversal: 32 archivos, 7+ features (inventario completo en agent_outputs/01 §D). No tiene PAUSED.
El repo ya tiene el precedente de capa separada (`core.session.cognitive`) para no engordar el contrato global.
**Opciones:**
- (a) **RECOMENDADA** — estado de runtime Focus-LOCAL (sealed propio: Idle/Tutorial/Running/Paused/Resumed/
  Interrupted/Completed/Invalidated) en el paquete focus2, mapeado al `SessionStatus` global SOLO en los
  puntos de write-ahead (Started→STARTED, checkpoint→CHECKPOINT, terminal→FINALIZED/ABANDONED). El global no
  se toca. **Precisión [R6-V5 P1-3]: este sealed cubre el SUBCONJUNTO de eventos de CICLO DE VIDA DE SESIÓN
  del estándar de 16 (TutorialStarted/Completed, SessionStarted/Paused/Resumed/Interrupted/Completed/
  Invalidated) — NO es "1:1 con los 16": los eventos de trial (TrialPresented, ResponseStarted,
  ResponseSubmitted, TrialResolved, DifficultyChanged) y de infraestructura (ExerciseOpened, ResultPersisted,
  ResultSynced) son EVENTOS del journal/analítica local, no estados de sesión, y no generan tipo de estado.**
- (b) Extender el global con PAUSED — RECHAZADA salvo necesidad demostrada: blast radius = todos los `when`
  exhaustivos de 7 features (learning 2026-06-16), y la semántica de pausa es de RUNTIME, no de recovery.
**Rollback:** (a) es aditivo local; borrar el paquete lo revierte.

## ADR-FOCUS-TIMING

**Pregunta:** ¿cómo se clasifica el timing y cuándo un RT es elegible como evidencia?
**Marco (APPROVED como estructura; umbrales DESIGNED_NOT_RUN):** clases T0-T3 DERIVADAS de la medición de la
PROPIA sesión (frame stats + fuente de timestamps), jamás declaradas por modelo de dispositivo. Criticidad RT
declarada por-dossier, versionada, fail-closed a UNKNOWN (`RtCriticalityRegistry`). Regla: T0/T1 en métrica
RT-crítica ⇒ jugable, NO evidencia oficial; accuracy SE PRESERVA; UX → WITHHELD_TECHNICAL. Señal autoritativa
existente: CHOREOGRAPHER_FRAME+NANOS_NATIVE (precedente Speed; Android API≥34). iOS: probe = stub ⇒ techo T1
estructural hasta portar CADisplayLink (complejidad estimada por orden-2). T2 = INALCANZABLE hasta diseñar el
banco físico de latencia (0 €: banco casero descrito por orden-2 como estudio futuro, no presupuesto).
**Queda a decisión:** umbrales GO/NO-GO por clase (del protocolo de orden-2, DESIGNED_NOT_RUN + revisión
empírica), y la resolución de ADR-FOCUS-PLATFORM-MIN (API 26 vs 29) que cambia las bandas.
**Regla cerrada:** precisión de timestamp ≠ exactitud; nada RT-crítico entra a evidencia oficial en v1 de v2.

## ADR-FOCUS-MIGRATION

**Pregunta:** ¿cómo versiona y migra Focus v2 sus datos y cómo se revierte?
**Marco (APPROVED por patrón de repo):** todo DTO persistido lleva `schemaVersion` + reader fail-closed ante
versión futura (`readVersioned`); enum desconocido → disposition Indeterminate; claves subject-scoped desde el
día 1 (lección S6R3-002/003: JAMÁS claves sin sujeto); namespace nuevo → `AccountBound` + eraser +
`AccountBoundErasureCoverageTest` EN EL MISMO CAMBIO; old/new readers conviven una versión (N lee N y N-1;
escribe N); **dual-read/write solo si un consumidor vivo lo exige — hoy NINGUNO (v2 no reemplaza datos v1:
namespaces distintos, v1 se conserva RETIRED)**; prohibido last-write-wins para historia cognitiva (append u
outcome tipado de conflicto); toda migración destructiva exige sign-off de privacy (precedente S6R3-007).
**Rollback por slice:** flag fail-closed OFF + el paquete/namespace v2 es borrable sin tocar v1; el historial
v1 nunca se migra in-place (se preserva bajo su propio namespace RETIRED).
**Queda a decisión:** ventana de retención de versiones de reader (propuesta: N-1) y el criterio de retirada
del namespace legacy (cuando exista el reemplazo funcional completo + export de usuario).

## ADR-FOCUS-SCORING

**Pregunta:** ¿qué fórmula produce el score de faceta?
**Estado: BLOCKED_BY_DESIGN — este registro documenta que NO se decide ahora.** Precondiciones en orden:
(1) simulación + suite adversarial T-ADV verde + recuperación de parámetros (S8-pre); (2) datos humanos
repetidos reales (S8, post-alpha); (3) análisis de fiabilidad/error/práctica/dispositivo con N dimensionado
por simulación (S9); (4) entonces propuesta con ≥3 rivales evaluados en sombra (A descriptivo robusto ·
B faceta anclada · C jerárquico trial-level; F compuesto unitario PROHIBIDO exponer; D IRT / E DDM
RESEARCH_ONLY para Focus). Barrera mientras tanto: input-manifest allowlist (el scorer solo puede consumir
métricas permitidas por su manifiesto versionado) + el único scorer productivo es el descriptivo A con claim
C0 por-tarea. Cambios posteriores: dual/shadow scoring o migración defendible (TEC-011).

## Backlog de ADRs propuestos por el research pack (EXTERNAL — sin autoridad hasta adopción)

Tokens que la matriz de requisitos cita en sus gates y que NO son ADRs de este registro ni del backlog canónico
(09 §19). Se registran aquí para que NINGÚN token quede huérfano; cada uno es un ADR FUTURO propuesto por el
pack (HYPOTHESIS), que solo existirá si su slice lo alcanza y el CEO lo adopta con el formato 09 §22:
`ADR-MSR-001/002/005/007 del pack` (subsumidos en ADR-FOCUS-SCORING/EVIDENCE, ya mapeados en la matriz) ·
`ADR-ADP-001/-002` (pesos de adaptación y cambios de política adaptativa — bajo ADR-FOCUS-SCORING/S7) ·
`ADR-ACC-001` (puentes de accesibilidad↔constructo — S3/S5, con ADR-SCI-008 canónico) · `ADR-TRN-001`
(protocolo de transferencia — S11, con ADR-SCI-009 canónico) · `ADR-CLAIM-001` (claims por nivel — S10) ·
`DPIA` (evaluación de impacto de protección de datos — con ADR-FOCUS-DATA/ADR-DATA-002) · `Protocolo etico` /
`Threat model` / `R-24` (artefactos de estudio S6/S11, no ADRs — se producen en su slice).

## ADR-FOCUS-PLATFORM-MIN (auxiliar)

**Pregunta:** ¿API 26 (canon TEC-002 APPROVED) o API 29 (repo real)?
**Opciones:** (a) bajar minSdk a 26 — slice técnica con matriz de compatibilidad AGP 9.1/Compose 1.10.3/
Kotlin 2.3.10 en API 26 + coste QA en hardware antiguo + impacto en bandas de timing; (b) CEO registra
SUPERSEDED de TEC-002 a API 29 (formato 09 §22). **Decisión de producto exclusiva del CEO.** Hasta entonces:
el diseño v2 NO usa APIs >26 en rutas de lógica común sin fallback, y el protocolo de timing cubre ambas bandas.
