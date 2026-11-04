# Molvex — Plan maestro: eliminación de la evaluación inicial obligatoria y calibración progresiva por bloque

**Tipo:** Plan maestro READ-ONLY (fase de investigación y planificación; cero mutaciones de código/canon en esta fase)
**Fecha:** 2026-07-24
**Autor:** Sesión principal Claude (lead orchestrator, protocolo `/mb`) + lentes especialistas read-only
**Estado:** PROPUESTA COMPLETA — lista para ratificación CEO y fase de implementación
**Producto:** Molvex (el repo y el canon todavía usan `MindBoost`; se tratan como el mismo producto — ver §2-N1)
**Decisión ejecutiva que implementa:** eliminar por completo la evaluación inicial obligatoria; acceso inmediato al
entrenamiento; rango por bloque desbloqueado solo por evidencia; ausencia nunca es cero; global solo con los 8 bloques.

> **Nota de gobernanza de esta sesión.** Este archivo es NUEVO y aditivo; ninguna fuente de verdad
> (`docs/context_pack/00–09`), código, test ni configuración fue modificada. El checkpoint obligatorio del
> `MINDBOOST_AUDIT_LEDGER.md` queda DIFERIDO por el mandato read-only del encargo y debe ejecutarse como primer
> paso de la fase de implementación (ver §14-U0). `/mb` está marcado `disable-model-invocation`
> (`.claude/commands/mb.md:3`): no puede autoinvocarse; esta sesión aplicó su protocolo manualmente
> (sesión principal = orquestador; routing por especialidad; gate de eficacia).

---

## 1. Recomendación ejecutiva

**Arquitectura elegida: D — Híbrida (uso inmediato + carriles separados + probes transparentes + calibración opcional), montada casi íntegramente sobre maquinaria que el repo YA tiene.**

El hallazgo central del baseline cambia el carácter del proyecto: **la "evaluación inicial obligatoria de ~16 minutos"
no existe como flujo app-wide en el código**. Existe (a) como mandato documental (PRD §8, Charter científico §6.4,
SES-003) y (b) como un soft-gate real únicamente en el track Enfoque (`BaselineProtocols.INITIAL`, 4 bloques,
≈16,8 min, que sustituye el botón de entrenar por "Empezar mi base"). Los otros 7 bloques ya se entrenan sin gate, toda
dimensión del perfil ya nace `CALIBRATING` con `value=null` (nunca 0), el global ya está triplemente bloqueado, y la
escalera de elegibilidad por ejercicio (`TRAINING_ONLY → … → RANK_ELIGIBLE`) ya está definida en el Exercise Quality
Standard. Por tanto este plan NO es una demolición: es (1) una **supersesión documental quirúrgica** (PRD §8 /
Charter §6.4 / SES-003), (2) la **retirada de un único soft-gate** (Enfoque) con su cold start sustituto, y (3) la
**construcción del contrato de desbloqueo de rango por bloque** — la única pieza mayor genuinamente nueva — sobre los
símbolos existentes (`CalibrationPolicy`, `SessionMeasurementPurpose`, `SessionEvidenceState`,
`CognitiveSessionEvidenceExtractor`, `DimensionStatus`), en modo sombra primero y fail-closed siempre.

Por qué D y no A/B/C puras: el uso natural puro (A) no puede producir evidencia comparable (autoselección de
ejercicios, dificultad adaptativa no comparable, práctica, repetición — exactamente los vetos del Charter §3/§13);
los probes solos (B) resuelven la comparabilidad pero sin la ruta opcional (C) castigan al usuario que QUIERE
clasificarse rápido, y C sola reintroduce por la puerta de atrás un examen para los que la eligen y deja sin
evidencia a los que no. D compone: entrenar alimenta dificultad y señal provisional; los probes transparentes,
breves y programados por cobertura producen la evidencia oficial; la ruta "Acelerar mi calibración" es solo un
atajo voluntario hacia los mismos probes con la misma dispersión temporal mínima (no puede saltarse 3 días/48 h);
y el rango por bloque se desbloquea de forma independiente cuando su evidencia cumple el contrato de §6.

Tres decisiones de encuadre que este plan fija: (i) durante calibración **no se muestra ninguna cifra longitudinal
de capacidad** — se muestra resultado de sesión, progresión de entrenamiento (XP/nivel de reto) y cobertura de
calibración por criterios, nunca una barra "3 de 5" que prometa desbloqueo por conteo; (ii) el desbloqueo de rango
se construye y valida **en sombra** (shadow) detrás de flags por bloque fail-closed + una constante de código para
la aprobación científica (patrón `ADR_SCI_006_APPROVED` ya existente), y no se activa mientras sigan abiertos
ADR-SCI-003/-005/-007/-008 y las precondiciones W1–W5; (iii) el protocolo de baseline de Enfoque **no se borra**:
se degrada a "calibración opcional" (arquitectura C) sin gate, conservando su valor científico sin coste de fricción.

El coste principal no es técnico sino científico-operativo: definir "evidencia oficial válida" y el contrato de
desbloqueo (§6) con simulación previa, y validarlo en alpha/beta con shadow scoring (§12). El riesgo dominante
(falso rango por volumen/repetición) queda cerrado por diseño: propósito sellado + dosis diaria + cobertura ≥2
ejercicios por faceta + dispersión temporal + incertidumbre máxima + histéresis anti-flipping, todo fail-closed.

---

## 2. Evidencia del estado actual

Clasificación: `VERIFIED` = leído literal en repo/docs con ruta; `REPORTED` = afirmado por un doc, no re-verificado
en código; `INFERRED` = deducción; `UNKNOWN` = sin evidencia; `CONTRADICTED` = dos fuentes en conflicto.

| # | Área | Evidencia del repo | Estado | Riesgo |
|---|---|---|---|---|
| N1 | Nombre del producto | Todo el repo/canon usa `MindBoost` (p. ej. `docs/context_pack/00:6`); "Molvex" no aparece. Se tratan como el mismo producto; NO se inicia migración de nombre | VERIFIED | Bajo — registrar; renombrar es otra iniciativa |
| E1 | Evaluación obligatoria app-wide | `AppRoute` (sealed) no tiene ruta onboarding/assessment; Auth → `resolvePostLoginRoute()` → `Main` (HomeScreen) directo (`App.kt:156-195`; `core/navigation/AppRoute.kt:5-37`); `LaunchDestinationResolver.kt:8-24` declara el onboarding como "lógica futura" | VERIFIED | — (no existe; nada que desmontar app-wide) |
| E2 | Los 8 bloques jugables sin gate | `MindBoostHubScreen.kt:96-256,301-342`: 8 TrackCards montan su root directamente; grep `needsBaseline` fuera de `focus/` = 0 gates | VERIFIED | — |
| E3 | La "evaluación de ~16 min" real | SOLO Enfoque: `BaselineProtocols.INITIAL` = 4 bloques (E1/E2/E3 calibración + probe vigilancia), ~250 trials × 3,8 s + 60 s ≈ 16,8 min (`focus/domain/evaluation/BaselineProtocol.kt:53-121`, "fixed 15–20 minute initial baseline"); soft-gate `needsBaseline = profile.baseline == null` (`FocusHomeStateHolder.kt:85`) sustituye el botón de entrenar por "Empezar mi base" (`FocusHomeScreen.kt:162-179`) | VERIFIED | Medio — es EL gate a retirar; su plan diario (`DailyFocusPlan`) asume baseline |
| E4 | Duración "16 min" vs "6–10 min" | PRD §8.3 declara "6–10 minutos" para la evaluación rápida; el brief CEO dice ~16; el único ~16 real es E3 (Enfoque). Discrepancia registrada | CONTRADICTED | Bajo — ambos textos quedan SUPERSEDED a la vez |
| E5 | Mandato documental de la evaluación | PRD `01:§8.1` pasos 8-9 + `§8.3` Etapa 1; Charter `03:§6.4` viñeta 1 ("Evaluación inicial rápida para un perfil provisional"); Decision Log `09:SES-003` | VERIFIED | Alto si no se supersede: cualquier slice futura podría "implementar el PRD" y construir el examen |
| E6 | Retest de Enfoque | `FocusEvaluationStateHolder` (≈3 min, 55 trials) muestra "Aún no tienes una base. Haz primero tu evaluación inicial…" si falta baseline (`FocusEvaluationStateHolder.kt:56-58`) | VERIFIED | Bajo — copy a sustituir en la misma slice que E3 |
| E7 | Perfil nace calibrando, nunca 0 | `MindBoostProfileAggregator.kt:28-31,89-94`: sin contribución cualificada ⇒ `DimensionState.calibrating` con `value=null`; invariante `require((status==CALIBRATING)==(value==null))` (`MindBoostProfile.kt`); UI "—" nunca 0 (`BrainProfileUiModelBuilder.kt:47-95`) | VERIFIED | — (implementa ya "SIN RANGO/CALIBRANDO") |
| E8 | Global bloqueado | Flag `mindboostGlobalScoreEnabled` OFF en las 3 impls + const `ADR_SCI_006_APPROVED=false` + `minEstablishedForComposite=3`; `compositeIndex` estructuralmente null; `ProvisionalGlobalIndex` sin escalar (solo cobertura) | VERIFIED | Medio — el gate real es "≥3 ESTABLISHED", no "8 calibrados"; el KDoc lo admite pendiente. Debe corregirse al activar global (§6.7) |
| E9 | Política de calibración en código | `CalibrationPolicy.kt:41-82`: EMERGING≥2 sesiones; ESTABLISHED≥5 sesiones + ≥3 días distintos. **Falta ≥48 h de span** (`ExerciseScaleEvidence.spanMs` existe pero ningún consumidor lo evalúa) | VERIFIED | Alto — la política mínima CEO exige 48 h; hueco a cerrar en §6.2 |
| E10 | Autoridad de propósito | `SessionMeasurementPurpose` (11 valores) + `ProfileMeasurementEligibility.disposition()` sealed 3-vías fail-closed (`UNKNOWN → Indeterminate`); `INITIAL_ASSESSMENT` es provisional-only y SIN productor en producción | VERIFIED | — (la palanca exacta para carriles del plan) |
| E11 | Carriles y kill-switch | `SessionEligibilityPolicy` (`CognitiveSessionEligibility.kt:164-264`): 3 carriles (profile/calibration/ranking) + kill-switch de 5 condiciones; cableado SOLO en Velocidad E8 con `lineageComplete=false` hardcoded | VERIFIED | Medio — carril ranking permite `TRAINING_FREE`; el contrato §6 lo endurece |
| E12 | "Ingestor" de evidencia | `CognitiveSessionEvidenceExtractor` PURO, **cero call-sites de producción**; emite solo hechos (observationCount, distinctDayCount, spanMs); `SessionEvidenceState.OFFICIAL_ELIGIBLE` estructuralmente inconstructible; vocabulario de rango RETIRADO y prohibido por shape-scanner (`EvidenceShapeScanRules.kt:32-50`) | VERIFIED | — (confirma el claim documental "contrato no cableado"; es la base del puente §8) |
| E13 | Por qué cada dimensión está CALIBRATING hoy | Focus/Lógica: gate hardcodeado `measurementEligible(UNKNOWN, lineageComplete=false)` ⇒ null (`FocusProfileSource.kt:47-99`, `LogicProfileSource.kt:37-90`); Velocidad: `SpeedAntiGaming.COVERAGE=PARTIAL` no adequateFor(RT_CRITICAL) + timebase gate; Lenguaje: banco `SMALL_UNVALIDATED` + fuente no instanciada; Emociones: `normalizedScore=null` por diseño; SOLO Memoria (span/pairs/episodic) y Decisiones alcanzan EMERGING vía `ScoreResult VALID → ProfileUpdateGate` (≥20 trials), nunca ESTABLISHED | VERIFIED | — (mapa exacto de qué desbloquea cada bloque, §6.6) |
| E14 | Precondiciones de wiring | W1–W5 declaradas (sellar purpose/lineage; purga sign-out; decisión producto TRAINING_FREE; registro ejercicio∈bloque; CalibrationBlueprint+equating) + ADR-SCI-003/-004/-005/-006/-007/-008 ABIERTOS (`05:§10.4`, `09:§19`) | VERIFIED | Alto — gates duros del plan; el rango NO se activa mientras sigan abiertos |
| E15 | Superficie numérica viva 1 | Speed Dashboard: tras UNA corrida muestra 3 barras + "TOTAL %" preliminar con banner, sin flag (`SpeedExerciseScreen.kt:184-185`; `SpeedScoreEngine.kt:28-44` `preliminary=true` siempre) | VERIFIED | Medio — número tipo-capacidad inmediato; §9/§13 lo re-etiquetan como resultado de sesión |
| E16 | Superficie numérica viva 2 | ProfileCard social: rank 10-tier COSMÉTICO hardcodeado (DIAMANTE III, nivel 47, "742 victorias"), editable por el dueño; KDoc dice que rank/level "no son client-writable" pero lo son (`ProfileCardModels.kt:133-168` vs `ProfileCardAppearance.kt:3-46`) | CONTRADICTED | Alto para la percepción: un usuario nuevo ve "Diamante III" y puede leerlo como rango cognitivo. §13-R7 |
| E17 | Analytics del funnel | Eventos PII-free definidos (EmotionAnalytics 14, AICoach 15) pero el sink NO se registra en `MindOSApplication.onCreate` (el KDoc de `MindBoostAnalytics.kt:14-16` afirma lo contrario) ⇒ funnel INERTE en producción | CONTRADICTED | Alto para §12: sin sink no hay métricas de validación. Slice propia (§14-S9) |
| E18 | Offline/sync | `SyncQueue` (fingerprint, `PENDING/SYNCED/FAILED/CONFLICT`) motor-agnóstica, `mindboostSyncFirestoreEnabled` OFF ⇒ nada sube; sin coordinador montado; `SessionRecoveryScanner` marca huérfanas ABANDONED (grace 120 s); local = fuente de verdad | VERIFIED | Medio — "provisional hasta integridad server-side" es hoy vacuo por ausencia de servidor (INFERRED); el contrato §8.5 lo hace explícito |
| E19 | Flags de scoring/rango | Inventario completo: `mindboostGlobalScore`, `focusScoreSurfaces`, `arena*` (5), `mindboostSyncFirestore` — TODOS OFF por defecto en las 3 impls; RemoteConfig omite claves arena/focus pero `boolOrDefault` cae al default compilado (sin gap) | VERIFIED | — (patrón a replicar para flags por bloque) |
| E20 | Rutas legacy de score prematuro | Focus v1 `PersonalizedScoreCard`/`FocusProgressScreen` INERTES (flag OFF, bounce a Home, `FocusRootScreen.kt:107-124`); Arena headless sin UI y 5 flags OFF | VERIFIED | Bajo — contención ya probada por test adversarial |
| E21 | Docs 04/05/06/07/08 | NINGUNO prescribe evaluación inicial obligatoria (verificado archivo completo por lentes dedicadas); 08 §12.1 ya codifica 5/3/48h; 04 ya define escalera y carriles con exposición limitada; 06/07 son proceso puro y NO requieren cambios | VERIFIED | — (la supersesión queda acotada a 01/03/09) |
| E22 | Estados del brief CEO (`UNASSESSED`, `QUICK_PROVISIONAL`…) | 0 apariciones en canon y en código; el vocabulario real es `DimensionStatus {CALIBRATING, EMERGING, ESTABLISHED}` + `SessionEvidenceState` + escalera 04 §3.2 | VERIFIED | Medio — el plan NO diseña desde nombres supuestos: mapea el vocabulario CEO al real (§7) |
| E23 | Ledger de auditoría | `MINDBOOST_AUDIT_LEDGER.md` leído al inicio (protocolo); checkpoint de esta tarea DIFERIDO por mandato read-only (conflicto de reglas resuelto a favor de la instrucción nueva del usuario, CLAUDE.md) | VERIFIED | Bajo — U0 de la fase de implementación lo salda |
| E24 | Worktrees paralelos | Existen `.claude/worktrees/*` con árboles divergentes; este plan audita SOLO el árbol raíz (`com.mindos.app`) | VERIFIED | Bajo — la implementación debe partir del árbol raíz |

---

## 3. Decisiones cerradas, abiertas y prohibidas

### 3.1 Cerradas por el CEO (no se re-litigan; este plan las implementa)

1. Cero evaluación cognitiva inicial obligatoria; ningún examen sustituto de ninguna duración.
2. Acceso inmediato: tras requisitos legales/privacidad/configuración esencial, se puede entrenar.
3. Ningún bloque/ejercicio/plan bloqueado por no evaluarse.
4. Arranque por bloque en estado sin rango/calibrando; desbloqueo INDEPENDIENTE por bloque.
5. Bloques no entrenados = sin evidencia; jamás cero (SCO-004 vigente).
6. Global oficial solo con los 8 bloques con sus gates (SCO-015 vigente; corrige el gate real de 3-ESTABLISHED, §6.7).
7. Política mínima de calibración: ≥5 evidencias oficiales válidas, ≥3 días, ≥48 h, cobertura, integridad,
   sin dependencia de una sola tarea/forma, más evidencia adaptativa (SCO-012/013 + Charter §11 vigentes).
8. No convertir cualquier partida normal en medición oficial (SES-001/SES-008 vigentes).
9. Sin lenguaje de IQ/inteligencia general/diagnóstico (Charter §2/§19.3 + lexicon de build vigentes).
10. Rangos provisionales durante alpha/beta; World Class y ranking público bloqueados (SCO-017/018/019 vigentes).
11. El flujo antiguo NO se reabre por A/B test (solo benchmark histórico).

### 3.2 Abiertas — decididas por ESTE plan (con recomendación inequívoca, §4–§11)

- Arquitectura de calibración progresiva: **D híbrida** (§4).
- Mecánica exacta de probes transparentes + calibración opcional (§5/§6).
- Terminología visible y estados por bloque (§7).
- Cifra longitudinal durante calibración: **no se muestra** (§9.3).
- Destino del protocolo baseline de Enfoque: **degradar a calibración opcional, sin gate** (§13-R2).
- Contrato interino de desbloqueo en sombra (§6) con sus flags/consts (§8.6).

### 3.3 Abiertas — NO resolubles sin datos/ADR (unknowns científicos; ver §14-U*)

- Estimador longitudinal robusto + incertidumbre (ADR-SCI-003); trend/cambio fiable (ADR-SCI-004);
  dosis oficial diaria por familia (ADR-SCI-005); modelo global (ADR-SCI-006); normas/rangos y n° de
  tramos de rango (ADR-SCI-007); puentes dispositivo/modalidad/equating (ADR-SCI-008); umbrales de
  histéresis exactos (nuevo ADR-SCI-010, §6.4); blueprint de cobertura por bloque (W5).

### 3.4 Prohibidas (barreras fail-closed exigidas)

- Conceder rango por volumen/XP/racha/uso/pago (SCO-003; test §11).
- Rango automático por haber completado la evaluación antigua (§10 migración).
- Probes encubiertos (Charter §6.3; requisitos §5.3).
- Percentil/comparativa poblacional sin normas representativas (SCO-019).
- Cifra de capacidad fabricada durante calibración (invariante `CALIBRATING ⇔ value==null` ya en código).
- Activar desbloqueo real con ADR-SCI-003/-005/-007/-008 o W1–W5 abiertos (const de código, §8.6).

---

## 4. Matriz de alternativas y selección

| Criterio | A · Uso natural puro | B · Probes integrados | C · Calibración opcional | **D · Híbrida (elegida)** |
|---|---|---|---|---|
| Fricción inicial | Nula | Nula | Nula (opcional) | **Nula** |
| Tiempo hasta primer valor (entrenar) | Inmediato | Inmediato | Inmediato | **Inmediato** |
| Tiempo hasta rango | Rápido pero inválido | Medio (días, por diseño) | Rápido solo para quien la elige | **Medio; atajo voluntario sin saltarse dispersión** |
| Validez de la evidencia | **Inaceptable**: autoselección, dificultad adaptativa no comparable, práctica, repetición (vetos Charter §3.5-§3.7, §13.1) | Alta: formas controladas, exposición limitada (04 §8.3) | Alta pero cobertura sesgada (solo voluntarios) | **Alta: misma evidencia de probes para todos los caminos** |
| Precisión alcanzable | Baja (ruido no modelable) | Alta con n suficiente | Alta en subpoblación | **Alta** |
| Riesgo de práctica/exposición | Máximo | Controlado (bancos reservados) | Controlado | **Controlado** |
| Transparencia / riesgo de evaluación encubierta | Máximo (todo mide sin decirlo) | Requiere disclosure explícito (§5.3) | Máxima (el usuario la pide) | **Alta con disclosure §5.3** |
| Retención ética | Buena | Buena si los probes son breves | Buena | **Buena; probes con cadencia limitada** |
| Complejidad técnica | Baja | Media (scheduler + bancos) | Baja (reusa E3) | **Media (B + C, mismo motor)** |
| Offline | OK | OK (bancos locales deterministas) | OK | **OK** |
| Accesibilidad | Hereda sesgos sin control | Modalidad declarada por probe | Ídem | **Ídem (modalidad = eje de comparabilidad)** |
| Riesgo de manipulación | Máximo (elige su mejor ejercicio) | Bajo (el sistema elige forma/momento) | Medio (repetir la ruta) | **Bajo: dosis + cooldown + formas reservadas** |
| Reversibilidad | — | Alta (flag por bloque) | Alta | **Alta (todo tras flags fail-closed)** |
| Facilidad de validación | Mala (sin condiciones controladas) | Buena (shadow + test-retest) | Media | **Buena (shadow §12)** |

**Selección: D.** A queda descartada como fuente de evidencia oficial (no como fuente de adaptación: el uso natural
SÍ alimenta dificultad y señal provisional). B es el motor de evidencia; C es una envoltura voluntaria sobre B
(mismos probes, misma dispersión temporal mínima, cero penalización por rechazarla); D los compone y es la única
que satisface simultáneamente "entrena ya", "rango solo con evidencia comparable" y "sin examen obligatorio ni
encubierto". La eliminación de la evaluación obligatoria no estaba en votación; lo que esta matriz decide es el
mecanismo de calibración progresiva.

## 5. Experiencia objetivo

Journey completo (dictamen `mindboost-product-ux` + `mindboost-personalization-engine`, decidido por la sesión
principal). Convención de carriles en TODO momento: **dificultad** se actualiza desde cualquier sesión (carril
entrenamiento, agnóstica al propósito); **perfil** solo desde `ScoreResult` VALID con propósito elegible
(`ProfileUpdateGate`); **rango** solo desde el contrato §6 (hoy: nunca). Microcopy en español de España; el lexicon
prohibido del repo aplica a todo el copy.

### 5.1 Los 15 momentos

**(1) Primera apertura.** Ve: bienvenida única, cero decisiones. Molvex no sabe nada; no puede inferir nada (todo
nace CALIBRATING). Guarda: nada. Copy: **"Entrena tu mente. Hoy."** / "Ocho tipos de entrenamiento. Empiezas ya; tu
clasificación llega sola, con la práctica." / CTA **"Empezar a entrenar"**.

**(2) Configuración mínima** (<60 s; sin ninguna pregunta de capacidad). Edad 18+ ("Confirmo que tengo 18 años o
más"), alias ("¿Cómo te llamamos?" · "Puedes cambiarlo cuando quieras"), privacidad ("Tus datos de entrenamiento se
guardan cifrados en este dispositivo. No se comparten."), objetivo ("¿Qué te apetece entrenar primero?" + chips de
los 8 + "Aún no lo sé" · **"Esto solo ordena tu pantalla. Todo está desbloqueado desde el primer día."**), tiempo
("¿Cuánto tiempo sueles tener?" · "Orientativo."). Guarda: preferencias. Puede actualizar dificultad: no. Perfil:
no. Rango: no. El objetivo/tiempo solo sesga la FRECUENCIA de recomendación (jamás el nivel de arranque —
anti-estereotipado PER-007 con barrera mecánica: la firma de `dificultadInicial(ejercicio, historialPropio)` no
admite parámetros demográficos).

**(3) Primera Home.** Ve: hub con los 8 bloques jugables, chip "Calibrando" por bloque, TodayPlanCard con UNA
sugerencia, cero números. Copy cabecera: **"Todo abierto. Elige y entrena."** Sheet del chip: "Cada bloque se
calibra solo, con tus sesiones. Cuando haya evidencia suficiente y de calidad, verás tu rango de ese bloque. No hay
nada que aprobar antes."

**(4) Selección del primer entrenamiento.** Lista de ejercicios con el recomendado marcado "Para empezar" y
duración visible; tutorial OFRECIDO, nunca obligatorio ("¿Primera vez? Te lo enseñamos en 30 segundos." · "Ver cómo
va / Ir directo"). La sesión sale sellada como carril entrenamiento.

**(5) Cold start de dificultad.** Regla (ya viva en Enfoque, se generaliza): `startingDifficulty[ejercicio] ?:
EASIEST` (`DailyFocusPlan.kt:115`) — piso del ejercicio, sin prior fabricado; staircase rápida converge en 2–4
trials (§5.4). Copy pre-sesión: **"Empezamos suave a propósito."** / "El nivel de reto se ajusta a ti en pocas
rondas. No mide lo que vales: regula el ejercicio."

**(6) Primera sesión.** El ejercicio, sin números longitudinales. Salir a mitad: "¿Salir de la sesión? Lo jugado no
se pierde como práctica, pero la sesión quedará incompleta." (huérfanas ≥120 s → ABANDONED vía
`SessionRecoveryScanner`; jamás evidencia). Molvex captura observables + telemetría de calidad; no puede inferir
capacidad (una sesión no pasa `MinSampleGate`).

**(7) Primer resultado post-sesión.** SOLO observables: "Aciertos: 14/18 · Nivel de reto: 3 · 5 min 12 s" + "+40 XP
de entrenamiento" + nota: **"El XP premia entrenar. Tu rango, cuando llegue, saldrá solo de la evidencia de juego —
no del XP."** Enlace "¿Dónde está mi puntuación?" → "Una sesión no basta para decir nada fiable, y preferimos no
inventarlo. Sigue entrenando: el bloque se calibra con tus próximas sesiones."

**(8) Primeros 7 días.** Racha suave ("3 días entrenando. Bien." — nunca culpa), plan del día, panel Calibrando por
bloque tocado. Consejo día 2–3: "¿Solo te apetece Memoria? Perfecto: puedes tener rango de Memoria sin tocar nada
más." Push opt-in máx. 1/día: "Tu sesión de hoy: Lógica, ~6 min. Cuando quieras."

**(9) Progreso de calibración por bloque.** Panel por criterios (§5.2), NUNCA "3 de 5".

**(10) Desbloqueo del primer rango.** Sobrio y ganado: **"Memoria: rango desbloqueado"** / "Bronce II · Provisional
(alpha)" / "Este rango sale de tu evidencia de juego en Molvex y se compara contigo, no con la población. Mientras
estemos en alpha puede recalibrarse." Sin percentil; sin compartir (ranking público bloqueado).

**(11) Perfil parcialmente calibrado.** Radar honesto: dimensiones con dato → valor+banda; resto "—" tocable →
panel Calibrando + CTA "Entrenar [bloque]". Leyenda fija: **"— significa 'aún sin evidencia suficiente'. Nunca
significa cero."**

**(12) Perfil 8/8.** Radar completo; el global sigue sin mostrarse (candados §6.7). Si pregunta: "Una cifra única
exige que cada pieza sea sólida y comparable. Estamos en ello; hasta entonces, tu perfil son tus 8 bloques." Nota
interna: 8/8 es hoy inalcanzable (E13) — este momento se diseña como estado objetivo y NINGÚN copy lo promete.

**(13) Rango STALE / recalibración.** Rango atenuado con fecha, nunca borrado ni bajado (SCO-010): "Rango en pausa
· última evidencia hace 6 semanas" / "Tu Bronce II de Memoria sigue aquí, pero es una foto antigua. Un par de
sesiones lo ponen al día. Nada se pierde por haber parado." CTA "Poner al día Memoria".

**(14) Cambio de dispositivo.** Sin sync montado (estado actual): "Molvex guarda tu entrenamiento cifrado en cada
dispositivo. En este empiezas calibración nueva. (La sincronización entre dispositivos está en desarrollo.)" Si el
timebase no es autoritativo: "En este dispositivo la precisión temporal no basta para medición oficial de Velocidad
mental. Puedes entrenarla igual; el rango de Velocidad requiere un dispositivo compatible." (una vez, dentro del
bloque).

**(15) Bloque sin método de rango (Creatividad, Emociones).** Chip **"Sin clasificación"** (distinto de
"Calibrando" — no promete lo que más sesiones no traerán): "Creatividad se entrena, no se clasifica (por ahora). No
existe todavía una forma de medirla que nos parezca honesta, así que preferimos no ponerle nota. Tus sesiones y tu
progreso de entrenamiento cuentan igual."

### 5.2 Patrón "Calibrando" (anti-promesa de conteo)

Cobertura por **criterios cumplidos**, no conteo. Panel por bloque con 4 criterios en 3 estados (✓/◐/○), en
lenguaje de hábito: "✓ Sesiones completas y válidas · ◐ Días distintos de práctica · ○ Práctica repartida en el
tiempo · ◐ Variedad de ejercicios del bloque". Reglas: (1) sin números en los criterios ("Días distintos ◐", no
"2/3"); (2) el quinto criterio (calidad) es invisible hasta que es lo único que retiene; (3) cada criterio tocable
→ una acción ("Entrena Memoria otro día distinto para avanzar este punto."); (4) fórmula fija, nunca promesa:
**"Cuando la evidencia sea suficiente y de calidad, tu rango aparece solo."**

Copy del caso duro (mínimo cumplido, evidencia insuficiente): **"Base mínima: hecha. Aún afinando."** / "Ya tienes
suficientes sesiones, días y variedad. Lo que falta no depende de jugar más: tus últimas sesiones variaron bastante
entre sí, y antes de fijar un rango queremos estar seguros de que refleja tu nivel y no un mal día (ni uno bueno)."
Variante por calidad técnica: "Algunas sesiones no pudieron medirse con la precisión necesaria y no las contamos
para tu rango — sí para tu entrenamiento."

### 5.3 Antes del rango: qué se muestra (decisión)

Siempre: resultado de sesión (observables), XP/progresión de entrenamiento (separada de capacidad, con su nota),
nivel de reto ("regula el ejercicio, no te mide"), UNA próxima acción, y el panel de criterios en el detalle.
**Decisión: NINGUNA cifra longitudinal numérica durante calibración.** Razones sobre la alternativa (score
provisional 0–100): con n bajo el número es ruido y rebota — enseña al usuario que los números de Molvex no son
fiables, que es el activo central; contradice el display-gate existente (`DISPLAY_CONFIDENCE_GATE=0.55` ya retiene)
y exigiría construir una segunda vía numérica más laxa que la que el repo prohíbe; invita a optimizar el número
antes de que el anti-gaming tenga cobertura adecuada; y duplica vocabulario frente al rango. El primer número que
el usuario ve de sí mismo debe ser uno que Molvex pueda defender. El hueco emocional lo cubren los observables +
XP + criterios (progreso real y visible sin afirmar capacidad).

**Transparencia de probes (requisitos del `efficacy-guardian`, vinculantes):** (a) disclosure up-front en
onboarding/Ajustes, lenguaje llano: "algunas rondas del entrenamiento se usan para medir tu nivel del bloque";
(b) señalización AGREGADA en el resumen de sesión ("esta sesión incluyó rondas de calibración") — preferida a
marcar el probe en vivo, que induce reactividad y contamina la medición; lo prohibido es que no exista NINGUNA
señal; (c) opt-out no punitivo y tan visible como el opt-in, con consecuencia declarada de forma factual: "tu
bloque permanecerá en Calibrando y no obtendrá rango"; el entrenamiento no se degrada jamás; (d) nunca prometer
desbloqueo-por-conteo.

### 5.4 Cold start de dificultad (contrato del carril entrenamiento)

- Arranque: `startingDifficulty[ejercicio] ?: EASIEST` — generalizar la regla viva de Enfoque a los 8 bloques.
- Primer contacto por ejercicio: sellado `TUTORIAL` (demo guiada 3–5 trials) o `FAMILIARIZATION` — actualiza SOLO
  dificultad, jamás perfil (el sealed 3-vías ya lo excluye).
- Intra-sesión: staircase 1-up/1-down los primeros ~6 trials (convergencia), luego 1-up/2-down; límites: ±1
  paso/trial, techo de subida por sesión (~N/3), suelo EASIEST siempre alcanzable. Patrón ya existente
  (`StopSignalTracker`).
- Inter-sesión (histéresis): la próxima sesión arranca en el nivel persistido **menos un paso** (warm-up); una
  subida solo se consolida sostenida en ≥2 sesiones; las bajadas se consolidan de inmediato (asimetría
  conservadora).
- Señales tempranas que SÍ ajustan dificultad desde el trial 1 (nunca perfil): acierto/fallo, RT crudo como señal
  relativa (sin gate de timebase — medir sí lo exige, regular no), abandono/ABANDONED (arranca un paso más abajo),
  degradación intra-sesión (`RecoveryPolicy` con fallback honesto sin baseline). La salida de seguridad emocional
  jamás penaliza (Regla #8).
- Primer plan: `TransversalDailyPlanner` ya ordena CALIBRATING primero con `GATHER_DATA` — para un usuario nuevo
  degrada naturalmente a rotación de cobertura. Se añade solo el sesgo de frecuencia por objetivo (General =
  round-robin; Estudiante prioriza Memoria/Enfoque/Lógica; Emprendedor prioriza Decisiones/Enfoque/Emociones — los
  8 siempre jugables) y la etiqueta **"Plan exploratorio"** ("Estamos conociendo tu forma de entrenar; este plan
  reparte los bloques para calibrar tu perfil") que se retira por ESTADO (primera dimensión EMERGING), no por días.
  Prohibido: "reforzamos tus puntos débiles" o cualquier claim de fortaleza no observada.
- Usuario ocasional: la calibración es función del recuento de evidencia, no del calendario (ya es así en
  `CalibrationPolicy`); prohibido el decay de evidencia sin ADR; la dificultad persiste con warm-up de un paso;
  invariante anti-atrapamiento: para cualquier historial (incluido gap de 90 días) el plan devuelve algo jugable y
  la evidencia antigua no se revoca por antigüedad.

### 5.5 Calibración opcional (ruta C — sustituto del CTA "Empezar mi base")

El botón "Entrenar" NUNCA se sustituye. La calibración exprés es una tarjeta secundaria dentro del bloque:

> ⚡ **Calibración exprés · opcional** — "Unas rondas variadas y seguidas que aceleran tu clasificación de Enfoque.
> ~8–10 min. **Entrenando normal también llegas.**" · [Probar] [Ahora no]

Reglas anti-examen: la frase "Entrenando normal también llegas" es obligatoria y literal (y verdadera: las
sesiones de entrenamiento son calibración-elegibles vía `SessionEligibilityPolicy`); "Ahora no" la colapsa y solo
reaparece una vez más (~3 sesiones después), nunca modal ni badge; dentro, copy de sesión, no de prueba ("Rondas
variadas de Enfoque. Juega como siempre."), y al terminar se ve el post-sesión estándar + el panel de criterios
actualizado; la duración se declara antes y se respeta; la ruta corre sellada `CALIBRATION` y NO puede saltarse la
dispersión temporal (≥3 días/≥48 h siguen aplicando — acelera la cobertura, no el calendario). El protocolo
`BaselineProtocols.INITIAL` (~16,8 min) se conserva como contenido pero se re-empaqueta en tramos ~8–10 min
(rediseño menor; el monolito actual es demasiado largo para "exprés"). El retest de Enfoque cambia su copy a: "Para
medir tu cambio necesitamos una referencia tuya. La consigues entrenando Enfoque o con la calibración exprés." —
dos rutas, ninguna obligatoria.

### 5.6 Superficies numéricas existentes (coherencia)

1. **Speed "TOTAL %"** (única "nota" que un usuario nuevo ve hoy en toda la app — la incoherencia más visible):
   renombrar a "Resultado de la sesión", eliminar la palabra "TOTAL" (o el compuesto entero), banner: "Resumen de
   esta sesión. No es tu puntuación de Velocidad mental: esa se calibra con la práctica."
2. **ProfileCard**: la superficie deja de llamarse "rango" → **"Insignia de tarjeta · personalizada"**, gramática
   visual divergente (sin numerales romanos, reservados al rango de bloque), defaults fabricados ("742 victorias",
   "nivel 47") eliminados u ocultos hasta que el dueño los edite (UI honesta: placeholders no se renderizan como
   hechos), tooltip "Decorativa. Elegida por el usuario. No refleja rendimiento en Molvex.", y fix de la
   contradicción KDoc↔código (§13-R7).
3. **Brain profile "—"**: se conserva; se añade leyenda persistente + "—" tocable con salida a entrenar; chip
   distinto para "Sin clasificación" (Creatividad/Emociones) vs "Calibrando".

---


---

## 6. Contrato de calibración y rango

Contrato formal (dictamen `psychometrician`, integrado y decidido por la sesión principal). Todo umbral marcado
`[INTERINO]` es política conservadora fijable ya; todo `[ADR]` exige simulación previa — prohibido improvisar la
fórmula (Charter §27).

### 6.1 Escalera de evidencia — qué significa de verdad "X sesiones"

Cada nivel es un superconjunto de gates del anterior; el símbolo del repo que lo implementa (o implementará) va anclado.

| Nivel | Definición formal | Símbolo | Cuenta para |
|---|---|---|---|
| Partida | Cualquier ejecución de un ejercicio | `ExerciseAttempt` | Nada oficial |
| Sesión de entrenamiento | Partida con propósito de entrenamiento sellado | `SessionMeasurementPurpose.{TRAINING_FREE, TRAINING_RECOMMENDED, PRACTICE}` | Adaptación de dificultad y feedback; nunca rango |
| Probe | Partida con propósito medición-elegible (`disposition()==Eligible`) que supera `MinSampleGate` de la métrica canónica y produce `ScoreResult` VALID | `ProfileMeasurementEligibility`; `MinSampleGate` | 1 observación candidata |
| Evidencia provisional | Probe con propósito provisional-only o comparabilidad degradada | `SessionEvidenceState.PROVISIONAL` | Perfil EMERGING etiquetado; nunca rango |
| **Evidencia oficial válida** | Probe con además: (a) calidad técnica `CLEAN/MINOR` (timebase autoritativo si RT-crítico); (b) banco no bloqueante (`BankIntegrity` válido); (c) anti-gaming limpio con cobertura `adequateFor`; (d) comparabilidad no-`Incompatible` | `ExerciseScaleEvidence.observationCount`; `SessionEvidenceState.OFFICIAL_ELIGIBLE` (hoy inconstructible a propósito) | Madurez de calibración del bloque |
| Evidencia RANK_ELIGIBLE | Conjunto de evidencia oficial válida que satisface el blueprint del bloque (§6.3) | Puente no construido (W1–W5) | Asignación de rango de bloque |

Ejes ortogonales: **día independiente** = `distinctDayCount` con `ClockProvenance` fiable (reloj no fiable no
aporta días); **forma-familia** = linaje de generador/banco distinto; **cobertura de subhabilidades** = facetas del
`CalibrationBlueprint` del bloque (W5); **cobertura de dificultad** = ≥2 niveles muestreados alrededor del punto de
operación; **comparabilidad de dispositivo** = puente ADR-SCI-008 (sin equating ⇒ el rango se restringe a linaje
homogéneo); **incertidumbre** = SE/intervalo del estimador (ADR-SCI-003 — el blocker duro de todo rango).

**Sesión multibloque:** produce evidencia por separado en cada bloque tocado (las `*ProfileSource` ya son
independientes); cada bloque cuenta la suya. Sin doble conteo.

### 6.2 Suficiencia — la política 5/3/48h es necesaria, NO suficiente

Hallazgo de código: `CalibrationPolicy.kt:44-50` implementa 5 sesiones + 3 días distintos, pero **la cláusula de
48 h no tiene consumidor** (`spanMs` existe en `ExerciseScaleEvidence.kt:23` y nadie la evalúa) ⇒ hoy la
"distinta-días" es comprimible. Primer cierre `[INTERINO]`: threadear `spanMs` → `SessionEvidence` y exigir
`spanMs >= 48h` como 4.º AND de `ESTABLISHED`.

Criterios adicionales para RANK_ELIGIBLE:

1. `[ADR-SCI-003]` **Incertidumbre máxima**: el IC 90% del estimado debe caber en una banda de rango. Sin modelo de
   error ⇒ rango bloqueado (hold honesto).
2. `[INTERINO]` **Cobertura ≥2 ejercicios/generadores por faceta** requerida por blueprint (patrón ADR-FOCUS-EVIDENCE).
3. `[INTERINO]` **≥2 form-families** con ≥1 observación válida cada una (anti-memorización de banco).
4. `[ADR]` **Acuerdo inter-faceta** acotado; desacuerdo alto ⇒ pedir más evidencia, no promediar a ciegas.
5. `[INTERINO]` **Evidencia extra cerca de umbral / baja calidad / alta variabilidad**: +2 observaciones válidas si el
   punto estimado cae a menos de media banda de un límite o si la integridad fue `MINOR`.

**Dosis diaria `[INTERINO hasta ADR-SCI-005]`:** máximo **1 evidencia oficial válida por linaje por día** hacia
calibración/rango; las repeticiones del día siguen alimentando entrenamiento y progreso, jamás madurez (contar 10
repeticiones de un día como 10 evidencias fabricaría madurez vía efecto de práctica intra-día). Una sesión
multibloque cuenta 1 en cada bloque (el cupo es por-linaje, no por-sesión).

**Comunicación cuando "el mínimo se cumple pero la evidencia no basta":** el estado visible sigue siendo
"Calibrando", con el criterio pendiente nombrado ("necesitamos medidas de otro ejercicio de este bloque", "tus
medidas varían mucho; hacen falta más") — nunca una barra de conteo que prometa desbloqueo (§5.2).

### 6.3 Blueprint por bloque (W5 / ADR-EVIDENCE-BLUEPRINT)

Cada bloque declara: facetas requeridas y sus ejercicios medidores (≥2 por faceta), formas reservadas de probe,
cobertura de dificultad, y techo de aportación por tarea (≤50% de la información de una faceta, regla interina de
ADR-FOCUS-EVIDENCE). Sin blueprint no hay RANK_ELIGIBLE para ese bloque — y eso deja el desbloqueo estructuralmente
cerrado hasta W5, que es el comportamiento correcto.

### 6.4 Umbrales y anti-flipping `[ADR-SCI-010 nuevo]`

Modelo CI-containment + histéresis (parámetros exactos por simulación de tasa de flip, no a ojo):

1. **Asignación inicial**: rango R solo si el IC 90% cae **entero** dentro de la banda R. IC cruzando dos bandas ⇒
   no se asigna; se muestra "Calibrando — cerca del límite" y se pide evidencia extra (§6.2.5).
2. **Cambio R→R′**: exige (a) IC entero en R′, (b) el punto cruza el límite con margen de histéresis `h`
   (`[INTERINO] h = 0.5·SE`), y (c) ≥`K=2` evidencias oficiales válidas nuevas desde la última asignación.
3. **IC a caballo tras tener rango**: se retiene el rango actual (sticky); jamás degrada una sola sesión ruidosa.
4. Consecuencia honesta: el ancho de banda debe ser ≥ ancho típico del IC o ningún rango es asignable — que es
   exactamente por qué el rango queda bloqueado hasta ADR-SCI-003. No es un defecto: es el diseño diciendo la verdad.

### 6.5 Baseline del Growth Index sin pre-test `[ADR-SCI-011 nuevo + ADR-SCI-003/004]`

- La **primera serie de probes ES la base**: `base_0` = mediana de las primeras K=3 evidencias oficiales válidas en
  ≥2 días distintos. La 1.ª sesión oficial del bloque se excluye como warm-up (o corre como `FAMILIARIZATION`, no oficial).
- `base_0` se congela al salir de `CALIBRATING` (no ventana deslizante: perseguiría al presente y haría el progreso ≈0).
- El progreso se reporta **solo como cambio fiable** (RCI; `MinSampleGate.RCI` ya existe): sin significación ⇒
  "sin cambio fiable", nunca un número. Etiqueta: "cambio frente a tus primeras mediciones", jamás "vs pre-entrenamiento".
- La ruta opcional "Acelerar mi calibración" da una base one-shot más limpia a quien la quiera — el reemplazo
  honesto del soft-gate de Enfoque.

### 6.6 Disponibilidad de método por bloque (estado real, hoy)

| Bloque | ¿Método RANK_ELIGIBLE plausible? | Qué lo bloquea hoy (VERIFIED) |
|---|---|---|
| Memoria | Sí (el más cercano) | Único (con Decisiones) que alcanza EMERGING (`ProfileUpdateGate`, ≥20 trials); falta multi-sesión + blueprint + modelo de error |
| Decisiones | Sí | Ídem (Brier/proceso); mismo faltante |
| Enfoque | Sí a futuro | Fail-closed W1 (`measurementEligible(UNKNOWN,false)` ⇒ null); motor v2 (focus2) construido pero inerte |
| Lógica | Sí a futuro | Mismo fail-closed W1 |
| Velocidad | Sí a futuro | `SpeedAntiGaming.COVERAGE=PARTIAL` + timebase (API ≥34); correcto que retenga |
| Lenguaje | No hoy | Banco `SMALL_UNVALIDATED` + fuente no instanciada; "pendiente de banco" |
| Emociones | **No — "sin método de rango"** | `normalizedScore=null` por diseño; separar regulación de supresión no está modelado |
| Creatividad | **No — "sin método de rango"** | Scoring no validado, AI bloqueada; sin instrumento |

**Ningún bloque es RANK_ELIGIBLE hoy.** El rango es un contrato forward que se construye en sombra. Creatividad y
Emociones muestran el estado terminal honesto "sin método de rango disponible" (distinto de "Calibrando": más
sesiones NO lo cambian), coherente con SCO-020.

### 6.7 Global

La decisión CEO ("global solo con los 8") NO está codificada: el gate real es `minEstablishedForComposite=3`
(`MindBoostProfileAggregator`; su propio KDoc lo admite pendiente). ADR-SCI-006 debe redefinir el gate a "los 8
bloques presentes y comparables" antes de cualquier flip de `mindboostGlobalScoreEnabled`; con 2 bloques sin método,
el global queda estructuralmente bloqueado — consistente con el triple candado ya existente. Mientras tanto no hay
breach vivo (flag OFF + const false + composite estructuralmente null).

---

## 7. Máquina de estados

### 7.1 Terminología canónica final (corrección obligatoria §3 del encargo)

| Concepto | Nombre canónico (interno) | Presentación UI (es-ES) | Regla dura |
|---|---|---|---|
| Resultado de una sesión | Resultado de sesión | "Resultado de la sesión" | Carril entrenamiento; observable por partida; jamás con forma de capacidad acumulada |
| Dificultad de entrenamiento | Nivel de reto | "Nivel de reto" | Explícitamente NO capacidad (Charter §8.4); puede moverse rápido |
| Estimación longitudinal | Performance Score (canon §10.1) | "Puntuación de bloque" con banda de confianza | **Nunca un 0–100 desnudo como titular**: siempre banda + "frente a tu propio recorrido"; `null ⇒ '—'`. No emisible hasta ADR-SCI-003. ("Nivel…" queda reservado a "Nivel de reto" para no colisionar) |
| Progreso personal | Growth Index (canon §10.2) | "Progreso propio" ("frente a tu propia línea base") | Solo cambio fiable (RCI); "Index"/"Performance" no aparecen en UI (voz es-ES; "Index" suena a instrumento validado) |
| Etiqueta calibrada | Rango de bloque | "Rango de [bloque] (provisional)" en alpha/beta | Etiqueta interna Molvex SIN referente poblacional y la UI lo dice; vocabulario DISJUNTO del social |
| Percentil normativo | — | No existe en UI | Bloqueado hasta normas representativas (SCO-019); nada con forma de percentil |
| Ranking competitivo | Arena (aparte) | No visible | Flags arena* OFF hasta validación |
| Insignia social | ProfileCard tier (BRONCE…LEYENDA) | "Insignia" / "Tarjeta" | Cosmética; PROHIBIDO llamarla "rango" junto a superficies cognitivas; nombres disjuntos de los tramos cognitivos (§13-R7) |

### 7.2 Estados visibles por bloque

Decisión de arquitectura (dictamen `kmp-architect`): **no tocar `DimensionStatus`** (su invariante
`CALIBRATING ⇔ value==null` es correcto y los estados nuevos son cualificadores de frescura/comparabilidad, no de
ausencia). Se añade un refinamiento de presentación puro, `sealed interface BlockRangeStatus` en
`profile/presentation/model/` (patrón `BrainProfileUiModelBuilder`):

```
SinRangoCalibrando   ← DimensionStatus.CALIBRATING                     (visible: "Calibrando")
RangoProvisional     ← DimensionStatus.EMERGING (preliminary=true)     (visible: "Rango provisional")
RangoCalibrado       ← DimensionStatus.ESTABLISHED && fresco           (visible: "Rango de bloque")
Stale                ← ESTABLISHED && (now-updatedAt) > staleWindow    (visible: "Rango en revisión")
NonComparable        ← linaje/versión discontinua (MeasurementLineage) (visible: "No comparable — recalibrando")
SinMetodoDeRango     ← OfficialScoringReadiness.isWired(dim)==false    (visible: "Sin rango disponible")
```

Transiciones: entrada única por evidencia vía aggregator (nunca UI); `Stale` no degrada el valor, degrada la
frescura (SCO-010: inactividad jamás baja el rango, activa reevaluación); `NonComparable` se declara hoy pero es
inalcanzable hasta que exista mezcla de linajes; salidas de `Stale`/`NonComparable` = probes de reactivación
(Charter §9.4). `SinMetodoDeRango` es el hecho que `DimensionStatus` no puede expresar: la diferencia honesta entre
"necesita más sesiones" (Memoria) y "no hay camino oficial cableado" (Enfoque hoy) — colapsarlos miente.

**Estado global derivado:** cobertura sobre las 8 claves fijas (reusa `OverallSummary.establishedDimensionCount`);
sin agregado nuevo. La UI solo pinta el sealed — cero cálculo de calibración en Compose.

### 7.3 Mapeo del vocabulario del encargo al real

`UNASSESSED` → no existe como estado propio: un bloque sin evidencia es `SinRangoCalibrando` con 0 observaciones
(la distinción "nunca jugado" vs "jugado sin evidencia" es presentacional, derivable del conteo). `QUICK_PROVISIONAL`
→ se RETIRA del canon (nacía de la evaluación rápida; matriz §10). `CALIBRATING/CALIBRATED` → `CALIBRATING`/
`ESTABLISHED`+frescura. `TRAINING_ONLY/PROFILE_ELIGIBLE/RANK_ELIGIBLE` → ya existen como elegibilidad por-ejercicio
(04 §3.2) y por-sesión (`SessionEvidenceState`), NO como estados de bloque — el estado de bloque emerge de la
evidencia acumulada, nunca de una etiqueta asignada.

---

## 8. Arquitectura de datos y dominio

Principio rector (verificado símbolo a símbolo): **mínimo diff — la mayor parte ya existe.** Cero namespace nuevo,
cero bump de schema, cero eraser nuevo para retirar el gate; el único campo de contrato genuinamente nuevo es el
consumidor de 48 h.

### 8.1 Autoridades (la UI nunca computa)

- Elegibilidad de sesión: `ProfileMeasurementEligibility.disposition()` (sealed 3-vías fail-closed) + carriles de
  `SessionEligibilityPolicy`. El propósito se sella al inicio de sesión (ADR-FOCUS-PURPOSE); fila sin propósito ⇒
  `UNKNOWN → Indeterminate` (jamás reinterpretación retroactiva — esto RESUELVE la etiqueta de régimen por fila).
- Estado de evidencia: `SessionEvidenceStatePolicy` (+ `ExclusionReason` tipado como motivos de invalidación).
- Madurez de bloque: `CalibrationPolicy` (5/3 + **48 h nuevo**) alimentada por `CognitiveSessionEvidenceExtractor`
  (hoy inerte; el puente se cablea en S6 tras W1).
- Rango: `BlockRangeStatus` (presentación) + `OfficialScoringReadiness` (const de código por dimensión, espejo de
  `ADR_SCI_006_APPROVED`) + flags por bloque (§8.4). Estimador/IC: ADR-SCI-003 (cuando exista, ahí nace
  `estimatorVersion` — no antes, YAGNI).

### 8.2 Persistencia y versionado

- Namespace afectado: solo `mindboost_profile` (ya en `AccountBound`, ya cubierto por
  `AccountBoundErasureCoverageTest`). Retirar el gate de Enfoque no persiste ningún dato nuevo.
- Versiones ya modeladas y reutilizadas: identidad de forma = `ReproducibleSessionIdentity` (digest, fail-closed
  ante schema futuro); banco = `BankIntegrityAuthority`/`BankValidator`; propósito+linaje por fila =
  `SessionMeasurementPurpose`+`lineageComplete` (el "régimen" exigido por GAP-FOCUS-PERSISTED-FLAG-REGIME ya viaja
  sellado por fila); reader N-1 fail-closed (`readVersioned` cuarentena versiones futuras).
- `deviceClass`: NO se modela hasta ADR-SCI-008 (YAGNI declarado).

### 8.3 Scheduler de probes (motor de la arquitectura B/D)

`ProbeScheduler` puro en `profile/domain/calibration/` (sibling de `CalibrationPolicy`):
`nextProbe(dimension, evidence, status, exposure, now) → Recommend(protocol) | None(reason)`. Inputs 100% locales
(conteos del extractor, exposición de banco ya trackeada en `core/bank`, cooldowns derivados de `distinctDayCount`)
⇒ **sin backend**. El resultado alimenta un banner recomendador no-bloqueante en el hub/track-home (el mismo seam
donde hoy vive `needsBaseline`), jamás un gate. `ApprovedProtocol` ya existe para expresar "protocolo de duración
fija que cuenta como calibración" — `BaselineProtocols.INITIAL` pasa a ser exactamente eso, recomendado y opcional,
bajo propósito `CALIBRATION`.

### 8.4 Flags y shadow scoring

- Flags nuevos (patrón cuádruple exacto del repo: interface + Default + Static + RemoteConfig, o no compila
  `:androidApp`): `focusOfficialRangeEnabled`, `logicOfficialRangeEnabled`, `speedOfficialRangeEnabled`,
  `languageOfficialRangeEnabled`, `emotionsOfficialRangeEnabled`, `creativityOfficialRangeEnabled` — default OFF en
  las 4 impls. Memoria/Decisiones gatean por `OfficialScoringReadiness` + ADRs (su vía EMERGING ya es legal).
- Const científica: `OfficialScoringReadiness.isWired(dimension)` — un único punto, cambio de CÓDIGO revisado
  citando el ADR del bloque (jamás un flag remoto), patrón `ADR_SCI_006_APPROVED`.
- **Sin flag para retirar el gate de baseline de Enfoque**: decisión CEO cerrada, diff de UI puro, reversible por
  revert (la convención del repo reserva flags para capacidades/claims, no para rollback de UX).
- Shadow: `BlockRangeStatus` y el veredicto de desbloqueo se computan SIEMPRE (baratos y puros; mismo patrón que
  `ProvisionalGlobalIndex`, que computa cobertura con el flag OFF); el flag solo decide si se PINTA. El período de
  sombra (§12) audita los veredictos computados-no-mostrados contra test-retest antes de encender nada.

### 8.5 Offline, idempotencia y eventos

Garantizado hoy: write-ahead + `SessionRecoveryScanner` (huérfanas → ABANDONED, jamás evidencia), `SyncQueue` con
fingerprint dedup (montaje OFF), local = fuente de verdad. La calibración por bloque es una **proyección pura del
CONJUNTO** de evidencias (conteos conmutativos: mismo conjunto ⇒ mismo estado, independiente del orden de llegada) —
la propiedad de determinismo se prueba en §11. Único hueco real detectado: dedup por `sessionId` en el call-site del
extractor (un replay duplicado inflaría `validSessions`) — una línea (`distinctBy { sessionId }`) + test. La
"oficialidad diferida hasta integridad server-side" (04 §14.2) se declara explícitamente VACUA hoy (no hay servidor
montado): las evidencias offline son válidas localmente y el contrato de re-validación server-side queda documentado
como futuro, no fingido.

### 8.6 Consumers a migrar y legacy

| Consumer | Cambio | Riesgo |
|---|---|---|
| `FocusHomeStateHolder.needsBaseline` + `FocusUiState.needsBaseline` + `FocusHomeScreen` (gate) | Borrar el gate; CTA de entrenar siempre visible; "Empezar mi base" → CTA secundario "Acelerar mi calibración (~16 min, opcional)" bajo propósito `CALIBRATION` | **Auditar antes**: ~42 archivos de Focus tocan "baseline" (`AdaptiveEngine`, `RecoveryPolicy`, `ProfileEvolution`, `OvertrainingGuard`); si el motor adaptativo exige perfil no-null, el cambio real es un default conservador en el motor (`AdaptiveModeGate.Mode.TRAINING` ya adapta sin baseline — seam correcto) |
| `FocusEvaluationStateHolder` (retest) | Copy no-bloqueante: sin base, el retest se ofrece como "primera medición" | Bajo |
| `DailyFocusPlan` / generadores | Si alguno branchea en `baseline==null` para degradar, sustituir por startingRule conservador | Medio |
| Speed Dashboard | Retirar el compuesto "TOTAL %" o re-etiquetar estricto a "Resultado de la sesión" (carril entrenamiento); nunca score de bloque mientras Velocidad esté reteniendo su contribución | Bajo (UI) |
| ProfileCard rank | Firewall terminológico ("Insignia", nombres disjuntos) + fix contradicción KDoc↔código (client-writable) | Bajo (copy/docs) |
| Focus v1 score surfaces / arena | Sin cambios: ya inertes tras flags OFF | — |

Rollback: cada flag por bloque revierte solo; la retirada del gate es un diff de UI revertible por revert; el puente
de evidencia (S6) es aditivo tras flag; `BlockRangeStatus` debe aterrizar antes de que las pantallas lo referencien
(única dependencia de orden dura).

## 9. Plan maestro en slices verticales

Convenciones: gates del repo en serie por slice (`:shared:desktopTest` + `:shared:detekt` + `:shared:compileKotlinDesktop` +
`:shared:compileAndroidMain`/`:androidApp:compileDebugKotlin` + scanners de copy; iOS vía CI `compileKotlinIosSimulatorArm64` +
`iosSimulatorArm64Test`); ledger actualizado al cierre de CADA slice; routing `/mb` (gate de salida siempre:
`efficacy-guardian` + `diff-auditor` en contexto fresco; `psychometrician` si toca medición; `privacy-security` si toca datos).
Owner por defecto: sesión principal como orquestador. Macrofases del encargo → slices:

**S0 · Fuentes de verdad y gobernanza** *(macrofase 1; SOLO docs — primera y bloqueante del resto)*
Outcome usuario: ninguno directo; científico: el canon deja de mandar un examen que ya nadie debe construir.
Entrada: este plan ratificado por CEO. Cambios: `09` (+SES-012, SES-003→SUPERSEDED, SES-002 parcial, +ADR nuevos §14),
`01 §8` reescrito, `03 §6.4` viñeta 1 + §11.4 (`QUICK_PROVISIONAL` fuera), `00 §6` viñeta nueva + packRelease bump,
`08` adiciones §12 de este plan, `05` re-conceptualización "assessment→calibration". **U0 primero: checkpoint diferido
del ledger de esta sesión read-only.** Sin flag; rollback = revert doc; sin código. Agentes: `mindboost-docs-knowledge`
+ `diff-auditor`. Gate de salida: matriz §10 aplicada al 100%, cero silencios (validador de ledger verde).
Aceptación: grep del canon no contiene ninguna evaluación inicial obligatoria vigente y SES-012 existe.

**S1 · Retirada del gate de Enfoque + cold start** *(macrofases 2-3)*
Outcome: un usuario nuevo entra a Enfoque y el botón de entrenar está SIEMPRE; científico: cero pérdida de honestidad
(el rango de Enfoque sigue cerrado por W1 — retirar el gate desbloquea entrenar, no medir).
Símbolos: `FocusHomeStateHolder.kt:85` (`needsBaseline` — borrar campo y sus 5 ramas en `FocusHomeScreen.kt:154,162,195,359,376`),
`FocusEvaluationStateHolder.kt:56-58` (copy retest → "referencia por entrenamiento o calibración exprés"),
`DailyFocusPlan.kt:96-116` (ya cold-start-compatible: `startingDifficulty[id] ?: EASIEST` — sin cambio),
`RecoveryPolicy.kt:49` (fallback sin baseline ya honesto — sin cambio). **Auditoría previa obligatoria**: ~42 archivos
de Focus tocan "baseline" (`AdaptiveEngine`, `ProfileEvolution`, `OvertrainingGuard`, `TrainingCycle`) — si alguno exige
perfil no-null, el cambio real es un default conservador en el motor (seam: `AdaptiveModeGate.Mode.TRAINING`).
Dependencias: S0. Sin flag (decisión cerrada; rollback = revert del diff de UI). Tests: gate ausente (state-holder),
plan jugable con perfil vacío, retest ofrece dos rutas; paridad iOS por compilación. Agentes:
`mindboost-personalization-engine` (diseño ya hecho) + `compose-ui-dev` + `test-engineer`; gate `efficacy` + `diff`.
Aceptación: usuario nuevo → Enfoque → entrenar en <10 s sin CTA bloqueante; `desktopTest` verde.

**S2 · Coherencia de superficies numéricas** *(macrofase 7 parcial — lo VIVO hoy)*
Outcome: ninguna cifra tipo-capacidad prematura visible; científico: coherencia con display-gate y `null≠0`.
Símbolos: `SpeedExerciseScreen.kt:184-185` + `SpeedDashboardScreen` (relabel "Resultado de la sesión", fuera "TOTAL",
banner §5.6), `ProfileCardModels.kt:133-168` + `ProfileCardAppearance.kt` ("Insignia · personalizada", defaults
fabricados fuera, gramática visual divergente, KDoc↔código reconciliado), `BrainProfileUiModelBuilder` (leyenda "—",
chip "Sin clasificación" para Creatividad/Emociones vía `OfficialScoringReadiness` de S3 o constante local temporal).
Dependencias: S0 (terminología). Sin flag; rollback revert. Tests: copy-scanner verde (¡el lexicon aplica!), snapshot
de state-holders. Agentes: `compose-ui-dev` + `mindboost-product-ux` + gate `efficacy` (dictaminó BLOQUEA el TOTAL %).
Aceptación: un usuario nuevo no ve NINGÚN número con forma de capacidad en su primer día.

**S3 · Estado por bloque visible (`BlockRangeStatus`) + plan exploratorio** *(macrofases 4 y 7)*
Outcome: hub y perfil muestran "Calibrando"/"Sin clasificación" por bloque + panel de criterios §5.2 + plan etiquetado
exploratorio; científico: el estado visible deriva SOLO del dominio (UI no computa).
Símbolos nuevos: `profile/presentation/model/BlockRangeStatus.kt` (sealed §7.2),
`profile/domain/aggregation/OfficialScoringReadiness.kt` (const por dimensión); cambios: `MindBoostHubScreen` chips,
panel Calibrando (criterios ✓/◐/○ sin números), `TransversalDailyPlanner` (comparator por objetivo declarado) +
etiqueta "Plan exploratorio" retirada por estado. Dependencias: S0 (nombres), S2 recomendada antes.
Sin flag (presentación honesta de estados ya existentes). Tests: mapeo DimensionStatus→BlockRangeStatus exhaustivo,
"Sin clasificación" para Emociones/Creatividad discriminante, planner con perfil vacío = rotación.
Agentes: `kmp-architect` + `compose-ui-dev` + `test-engineer`; gate `efficacy` + `diff`.
Aceptación: los 6 estados renderizan; ninguna promesa de conteo en copy (revisión UX + scanner).

**S4 · Contrato interino de calibración en dominio** *(macrofase 6 parcial)*
Outcome: invisible; científico: la política 5/3/**48h** completa + dosis diaria + cobertura mínima quedan codificadas
y computadas EN SOMBRA (nada se muestra ni desbloquea).
Símbolos: `CalibrationPolicy.kt` (4.º AND `spanMs>=48h`; threadear `spanMs` a `SessionEvidence`), dosis 1
evidencia/linaje/día `[INTERINO]`, cobertura ≥2 formas para techo ESTABLISHED; dedup `distinctBy{sessionId}` en el
call-site del extractor (hueco §8.5). Dependencias: S0. Flag: no (computación en sombra, sin superficie). Tests §11
(casos 5-en-un-día, un-solo-ejercicio, mitades discriminantes, monotonía, determinismo por permutación).
Agentes: `psychometrician` (ya dictaminó) + `cognitive-engine-dev` + `test-engineer`; gate `efficacy` + `psy` + `diff`.
Aceptación: mutación de cada regla nueva pone en rojo ≥1 test; la suite honesta pasa.

**S5 · W1 — carrier de propósito+lineage** *(macrofase 5 núcleo; la precondición cara)*
Outcome: cada sesión sella su propósito; científico: desbloquea que Focus/Lógica dejen el CALIBRATING forzado
(manteniendo `preliminary=true` hasta sus ADR) y habilita probes reales.
Símbolos: descriptores de sesión por módulo (patrón ya cableado en Velocidad E8:
`GoNoGoCognitiveSessionAdapter`), `FocusProfileSource.kt:47-99`/`LogicProfileSource.kt:37-90` (sustituir el
hardcode `(UNKNOWN,false)` por el propósito sellado real), decisión de producto W3 (`TRAINING_FREE` en carril
calibración: mantener allowlist actual de `SessionEligibilityPolicy` — las sesiones de entrenamiento SÍ son
calibración-elegibles, sostiene "entrenando normal también llegas"). Dependencias: S4. Flag: no para el sellado
(aditivo, fail-closed por diseño: fila sin propósito ⇒ Indeterminate). Migración: filas legacy sin propósito caen a
`UNKNOWN→Indeterminate` (jamás reinterpretadas). Tests: sellado por módulo, legacy fail-closed, TUTORIAL nunca
evidencia. Agentes: `kmp-architect` + `cognitive-engine-dev` + `data-sync-dev`; gate completo (+`privacy-security`).
Aceptación: 0 productores de `UNKNOWN` en sesiones nuevas de los 8 módulos.

**S6 · Puente evidencia→calibración + scheduler de probes + calibración exprés** *(macrofases 5-6)*
Outcome: panel de criterios alimentado por evidencia real; tarjeta "Calibración exprés · opcional" (§5.5);
científico: el extractor gana su primer call-site de producción; probes con exposición controlada.
Símbolos: call-site de `CognitiveSessionEvidenceExtractor` (proyección por bloque), `ProbeScheduler` nuevo
(`profile/domain/calibration/`), re-empaquetado de `BaselineProtocols.INITIAL` en tramos ~8-10 min bajo propósito
`CALIBRATION`, transparencia §5.3 (disclosure en Ajustes + señalización agregada + opt-out). Dependencias: S5.
Flag: no para el panel (hechos); el scheduler emite RECOMENDACIONES (no gates). Tests: scheduler puro
(cooldown/exposición), opt-out deja todo entrenable, disclosure presente. Agentes: `cognitive-engine-dev` +
`compose-ui-dev` + `privacy-security` + `efficacy` (transparencia vinculante). Aceptación: un usuario puede
calibrar un bloque SOLO entrenando (sin tocar la exprés) — la mitad discriminante de la opcionalidad.

**S7 · Instrumentación** *(precondición de validación; E17)*
Outcome: invisible; científico: el funnel §12 emite (sink registrado en `MindOSApplication.onCreate` como el KDoc
ya promete), eventos `calibration_progress`, `evidence_disposition`, `shadow_unlock_eval` PII-free.
Dependencias: S0; puede correr en paralelo a S1-S4. Flag: consentimiento de analítica (PRI-009); rollback = sink
no-op. Tests: canarios PII (patrón `ObservabilityHubTest`), guard de release. Agentes: `mindboost-data-analytics` +
`privacy-security`. Aceptación: eventos visibles en debug con consentimiento ON; cero payload cognitivo.

**S8 · Shadow del desbloqueo** *(macrofase 8)*
Outcome: invisible; científico: `wouldUnlock` por bloque computado log-only (fase S1 de §12.4) + simulación offline
previa con trazas sintéticas (fase S0 de §12.4). Dependencias: S4+S5+S6+S7. Flag: los 6 `*OfficialRangeEnabled`
existen y quedan OFF (§8.4). Tests: simulación adversarial (grinding no acelera, B9 en sombra). Agentes:
`psychometrician` + `test-engineer` + `mindboost-experimentation`. Aceptación: log de sombra estable 2 ventanas.

**S9 · Migración de usuarios existentes** *(macrofase 9; ver §"Migración" abajo)*
Outcome: nadie pierde nada; científico: el baseline antiguo jamás se convierte en rango.
Símbolos: `BaselineProtocolMigrationTest` (nuevo); el `FocusBaselineProfile` persistido se conserva como referencia
histórica del retest y como semilla de `startingDifficulty` (placement de dificultad SÍ; evidencia NO — era
pre-contrato, sin propósito sellado). Dependencias: S1. Aceptación: usuario con baseline viejo y 0 evidencia nueva
⇒ `CALIBRATING`/null (test discriminante §11).

**S10 · Validación alpha → beta → activación gradual** *(macrofases 10-11; §12)*
Alpha A1-A6 → hardening → beta B1-B10 con shadow → criterios S2 de activación (6 AND preregistrados) → flip
gradual por bloque (previsiblemente Memoria primero) 5%→25%→100% con B5 en vivo; rollback = flag OFF (el rango
vuelve a "Calibrando", nunca a otro valor). NO se activa ningún rango mientras ADR-SCI-003/-005/-007/-008 o W1-W5
sigan abiertos (§3.4).

### Migración (macrofase 9, detalle)

| Población | Tratamiento |
|---|---|
| Usuarios nuevos | Flujo §5 directo; nada que migrar |
| Abandonaron la evaluación antigua (baseline null) | Idénticos a nuevos: el gate desaparece, entrenan ya |
| Completaron el baseline de Enfoque (~16 min) | El `FocusBaselineProfile` se CONSERVA: (a) dato histórico visible, (b) placement de dificultad (`startingDifficulty` sembrado), (c) referencia del retest. **Jamás** evidencia oficial ni rango automático (pre-contrato: sin propósito sellado, sin linaje) |
| Perfil provisional (Memoria/Decisiones EMERGING) | Sin cambio: su vía `ProfileUpdateGate` ya cumple el contrato; se re-etiqueta "Rango provisional" en UI |
| Sesiones históricas elegibles | No se reinterpretan (regla del repo: jamás reetiquetado retroactivo); filas sin propósito ⇒ `Indeterminate` |
| Datos antiguos no comparables | Permanecen bajo su linaje; `NonComparable` cuando se mezcle (§7.2) |
| Offline durante el despliegue | Sin efecto: todo es local-first; el contrato nuevo aplica a sesiones nuevas |
| Reinstalación/duplicados/sync fuera de orden | Proyección determinista del conjunto + dedup por `sessionId` (§8.5, test §11) |

---


---

## 10. Matriz de actualización documental

Regla de ejecución: NINGÚN documento se reescribe en silencio. Cada cambio incrementa `documentVersion`, se registra
en `09 §21` (registro de cambios) y la decisión antigua queda `SUPERSEDED` apuntando a la nueva (patrón `09 §18`).
Los docs `06` y `07` NO se tocan (verificado: son proceso puro, agnósticos al onboarding — E21).

| Documento | Sección | Contradicción con la decisión | Cambio requerido | Decisión que queda SUPERSEDED |
|---|---|---|---|---|
| `09_DECISION_LOG.md` | §6 tabla SES | SES-003 "Evaluación inicial en dos fases: rápida provisional + calibración progresiva" | Nueva decisión **SES-012** (id siguiente libre de la tabla §6): "Cero evaluación inicial obligatoria. Calibración progresiva por bloque con desbloqueo independiente de rango; probes transparentes + calibración explícita opcional; el acceso al entrenamiento es inmediato". Fila en §18: SES-003 → SES-012 | SES-003 |
| `09_DECISION_LOG.md` | §6 SES-002 | "Evaluación mediante prueba explícita + probes embebidos controlados" — la "prueba explícita" deja de ser parte del flujo por defecto | Enmienda: la prueba explícita pasa a OPCIONAL (ruta C); los probes embebidos siguen. SES-002 se marca parcialmente SUPERSEDED por SES-012 en su mitad "prueba explícita [obligatoria]" | SES-002 (parcial) |
| `09_DECISION_LOG.md` | §19 backlog ADR | Faltan los ADR nuevos de este plan | Añadir ADR-SCI-010 (histéresis/anti-flipping de rango), ADR-SCI-011 (baseline móvil de Growth Index sin pre-test), ADR-PER-002 (cold start de dificultad por bloque) — ver §14 | — (aditivo) |
| `01_MINDBOOST_PRODUCT_PRD.md` | §8.1 pasos 8-9 | "8. Evaluación rápida de los ocho bloques. 9. Perfil y confianza provisionales" como pasos del onboarding | Reescribir el flujo: onboarding mínimo (edad, alias, privacidad, objetivo, tiempo, preferencias) → Home → primer entrenamiento inmediato. El perfil provisional nace del USO, no de un examen | PRD §8.1 (pasos 8-9) vía SES-012 |
| `01_MINDBOOST_PRODUCT_PRD.md` | §8.3 | "Etapa 1: evaluación rápida que configura dificultad y perfil provisional… 6–10 minutos" | Sustituir por "Etapa 1: cold start conservador + adaptación temprana (sin examen). Etapa 2: calibración progresiva por bloque (probes transparentes + ruta opcional 'Acelerar mi calibración')" | PRD §8.3 Etapa 1 vía SES-012 |
| `01_MINDBOOST_PRODUCT_PRD.md` | §8.2, §10.1 (input "Evaluación") | El plan usa "Evaluación" como input de personalización | Renombrar el input a "evidencia de calibración acumulada" (opcionalidad explícita) | — (consecuencia de SES-012) |
| `01_MINDBOOST_PRODUCT_PRD.md` | §23 Onboarding | "Perfil provisional comprensible" como criterio de aceptación (asume evaluación previa) | Reformular: "Arranque sin rango comprensible; primer entrenamiento inmediato; ningún rango oficial prematuro" (la última cláusula ya existe) | — |
| `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` | §6.4 viñeta 1 | "Evaluación inicial rápida para un perfil provisional" | Sustituir por: "Calibración progresiva por bloque desde el primer uso (sin evaluación inicial obligatoria); reevaluación estructurada y probes continuos se conservan; existe una calibración explícita OPCIONAL que usa el mismo contrato de integridad" — el resto de §6.4 (calibración progresiva, reevaluación mensual, probes) queda INTACTO | Charter §6.4 viñeta 1 vía SES-012 |
| `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` | §11.4 | Estado `QUICK_PROVISIONAL` en el vocabulario de calibración | Retirar `QUICK_PROVISIONAL` del set (nace de la evaluación rápida); documentar el mapeo al vocabulario real (§7 de este plan) | — (consecuencia) |
| `05_TECHNICAL_CURRENT_STATE.md` | §6/§7 (módulo "assessment"), §10, §15 | "assessment" como pantalla/módulo del target; estado del ingestor; flags | Re-conceptualizar "assessment" → "calibration" (probes + ruta opcional); registrar el contrato de desbloqueo en sombra y los flags por bloque (`§15` ya prevé `OFFICIAL_SCORING_ENABLED` por bloque/versión); actualizar §10 cuando la fase de implementación cablee el puente | — (doc VIVO; se actualiza al implementar) |
| `08_ALPHA_BETA_VALIDATION_PLAN.md` | §7.4, §17.2, §20.3, §22 | Ninguna contradicción dura (E21); "onboarding completo" como gate podría leerse como examen | Añadir las métricas/experimentos de §12 de este plan (tiempo-hasta-primer-entrenamiento como métrica primaria de activación; D1 guardrail; instrumento de evaluación-encubierta; embudo de calibración por bloque); redefinir operativamente "onboarding completo" = configuración esencial SIN evaluación | — (aditivo) |
| `04_EXERCISE_QUALITY_STANDARD.md` | — | Ninguna (E21): ya modela escalera, carriles, startingRule conservador | SIN CAMBIOS de contrato. Única vigilancia: que `startingRule` jamás se interprete como test inicial | — |
| `00_CONTEXT_INDEX.md` | §6 resumen ejecutivo | "Calibración inicial: al menos 5 evidencias…" ya es compatible; falta reflejar la nueva decisión | Añadir viñeta: "Sin evaluación inicial obligatoria; acceso inmediato; rango por bloque solo con evidencia (SES-012)"; bump packRelease | — (espejo del CEO en su master canónico) |
| `CLAUDE.md` / ledger | Aprendizajes | — | Al implementar: checkpoint del ledger (U0) + aprendizaje si surge regla reutilizable | — |

---

## 11. Estrategia de pruebas

Principios (dictamen `test-engineer`, alineados con los aprendizajes del repo): (1) `Absent/CALIBRATING/null` no es
un valor bajo — toda aserción de orden envuelve ambos operandos en `requirePresent` o es vacua; (2) toda regla que
endurece un gate lleva su **mitad discriminante en el mismo cambio** (el honesto DEBE pasar); (3) propiedad rectora
= **monotonía** (más evidencia válida nunca retrocede un estado, salvo los dos escapes declarados: STALE por vejez,
INVALIDATED por integridad); (4) **determinismo de proyección** probado por permutaciones, no por un orden feliz;
(5) los fixtures construyen estado vía las autoridades puras (`CalibrationPolicy`, `SessionEligibilityPolicy`,
extractor, `MinSampleGate`), no vía flujo end-to-end aún no cableado; (6) verificación de mutación para cada regla
nueva (la regla se rompe a propósito y ≥1 test cae).

Casos adversarios mínimos (tipo · suite destino · aserción discriminante):

| Caso | Test |
|---|---|
| Cero sesiones | unit · `CalibrationPolicyTest` · `CALIBRATING && value==null` (usa el invariante, no un assert débil) |
| 1 sesión | unit · ídem · `status != EMERGING` (fija `EMERGING_MIN_SESSIONS=2` contra bajadas silenciosas) |
| 5 partidas ≠ 5 evidencias | unit · `CognitiveSessionEvidenceExtractorTest` · 5 runs `PRACTICE/TUTORIAL/FAMILIARIZATION` ⇒ `observationCount<5` |
| 5 evidencias en un día | unit · `CalibrationPolicyTest` · bloqueado por AMBAS causas (días Y span<48h), asserts separados |
| 5 bien distribuidas | unit (mitad discriminante) · count=5, días≥3, span≥48h, ≥2 métodos ⇒ `ESTABLISHED` |
| Un único ejercicio repetido | unit · bloqueado por `distinctMethodCount<2` |
| Solo un bloque entrenado | integración · `MindBoostProfileAggregatorTest` · esa dimensión avanza, las otras 7 CALIBRATING/null, `compositeIndex==null` |
| Sesión multibloque | integración · cada dimensión solo por su propia contribución (sin filtrado cruzado) |
| Duraciones 2/5/10/20/30 | unit parametrizado · `SessionEligibilityPolicyTest` · umbral 120 s discriminado |
| Tutorial abandonado | unit · `TUTORIAL` jamás evidencia, con cualquier duración |
| Process death durante probe | integración · `SessionRecoveryScannerTest` · huérfana⇒ABANDONED⇒cero conteo en el extractor |
| Offline prolongado | integración · la evidencia cuenta con su timestamp de EVENTO, no el de sync |
| Sync duplicado/desordenado | property · `EvidenceProjectionDeterminismTest` · mismo multiset en N permutaciones+duplicado ⇒ mismo `ExerciseScaleEvidence` |
| Reloj/DST/manipulado | unit · timestamps futuros ⇒ cuarentena (regla ya existente); `endedAt<startedAt` ⇒ no contribuye |
| Cambio de dispositivo | unit · lineages separados, jamás fusionados por recencia; `NonComparable` explícito |
| Latencia no validada | unit · patrón `SpeedProfileSourceTest` ya existente (timebase gate excluye) |
| Accesibilidad cambia modalidad | unit · modalidad distinta ⇒ lineage propio, nunca mezcla ciega |
| Rápido-por-azar vs lento-perfecto | adversarial · patrón `AntiGamingRedTeamTest` (ambas mitades) |
| Sesión extrema única | unit · el agregado no salta por un outlier (sigue exigiendo count/días/span) |
| Evidencia cerca de umbral | unit · límites inclusivos explícitos (`>=`) + sin flapping con evidencia marginal (histéresis §6.4) |
| Rango → STALE | unit · valor histórico conservado y marcado no-fresco, jamás reseteado en silencio |
| Bloque sin método (Creatividad) | unit · permanece CALIBRATING/null indefinidamente **por diseño** (el test falla si alguien lo "arregla" sin decisión CEO) |
| Sin consentimiento de analytics | integración · pipeline de calibración idéntico con sink no-op |
| Migración desde evaluación antigua | migración · `BaselineProtocolMigrationTest` · baseline viejo + 0 evidencia nueva ⇒ CALIBRATING/null; el histórico sigue legible |
| XP alto, cero evidencia | unit · perfil intacto ante cualquier valor cosmético |
| 8 bloques parciales | integración · `compositeIndex==null`; el test FIJA el número del gate para que un cambio silencioso lo rompa |

Property-based clave: monotonía sobre prefijos crecientes de evidencia (shrinking a par mínimo) y determinismo de
proyección con semilla pinneada (patrón `DeterministicRng`). Paridad iOS: los tests nuevos de
calibración/evidencia/aggregator van a `commonTest` (heredan `iosSimulatorArm64Test` gratis); los de timebase de
Velocidad se quedan como lógica pura (la paridad real del actual se verifica por compilación
`compileKotlinIosSimulatorArm64`); nada usa `Clock.System` directo (inyectar `EpochClock`/`EvaluationAsOf`); en CI
verificar ejecución REAL (conteo de XML, no `UP-TO-DATE` — aprendizaje 2026-07-19).

---

## 12. Alpha/beta y rollout

Plan del dictamen `mindboost-experimentation` sobre la estructura real de `08`. **Todo umbral numérico es
HIPÓTESIS pendiente de baseline** (se fija en alpha, se congela en el preregistro de beta — 08 §10.3). El flujo
antiguo NO se re-testea por A/B (decisión cerrada); sirve solo como descripción histórica.

### 12.1 Instrumentación mínima (precondición dura — hoy el funnel está INERTE, E17)

Eventos PII-free con `appVersion/scoringVersion/deviceClass/platform`: funnel de entrada (`app_first_open`,
`hub_shown`, `block_card_tap`, `session_start(blockId, exerciseId, purpose)`, `session_first_response`,
`session_end(reason, durationMs, trialCount)`); snapshot diario `calibration_progress(blockId, officialEvidenceCount,
distinctDays, spanMs, coverage)`; `evidence_disposition(blockId, VALID|WITHHELD|INVALIDATED, reason)` (el denominador
de media docena de experimentos); superficie (`calibrating_badge_shown`, `rank_unlocked` post-shadow,
`speed_dashboard_shown`); y `shadow_unlock_eval(blockId, wouldUnlock, blockingReasons[])` (log local). Dependencias
duras pre-alpha: sink registrado (S7), soft-gate de Enfoque retirado (S1), ProfileCard resuelta (S2 — contamina
cualquier medición de comprensión), W1+W4+consumidor 48h antes del shadow con datos reales.

### 12.2 Alpha (~20 adultos; detección + comprensión, sin potencia — denominadores e IC reportados)

- **A1 TTFT**: mediana `hub_shown→session_first_response`; parada si ≥3 usuarios no inician sesión en su primera
  visita; éxito = 0 gates residuales.
- **A2 Primera sesión**: % completadas por bloque; un bloque con 3+ abandonos consecutivos sale de la matriz hasta
  diagnóstico; éxito = 100% de abandonos con `reason` clasificable.
- **A3 Abandono de onboarding**: funnel + think-aloud; cualquier abandono en un paso de consentimiento por confusión
  se revisa antes del siguiente participante.
- **A4 Comprensión de "Calibrando"/"—"**: entrevista cognitiva (pantallas reales, doble codificación ciega); fallo =
  ≥3 usuarios lo leen como "nivel bajo" o "roto" ⇒ rediseño de copy obligatorio. Sondear explícitamente rango de
  bloque vs insignia social: UN solo usuario creyendo que "Diamante III" es su nivel cognitivo = hallazgo CRITICAL.
- **A5 Evaluación encubierta**: wording neutral que no planta la idea ("Cuéntame qué crees que hace la app con tus
  partidas" → codificación transparente/incomoda/no-menciona); ≥2 "incomoda" = fallo y se revisa la comunicación
  antes de beta; un usuario reportando que "toda partida cuenta" = incidente de claims.
- **A6 Techo/suelo temprano**: distribuciones por ejercicio×clase de dispositivo; cuantificar cuántos usuarios caen
  en el bucket sin timebase autoritativo (alimenta B7).

### 12.3 Beta (preregistrada; potencia con varianzas de alpha)

B1 activación 1.ª visita (estimación con IC de Wilson, n≈1.070 por cohorte para ±3%; análisis intermedio a n=300
solo para guardrails); B2 D1/D7 como guardrails con límites de control (dos cohortes semanales fuera del límite ⇒
freeze de cambios de onboarding); B3 días-hasta-calibrar por bloque (Kaplan-Meier con censura) + **% atrapados
desglosado por `blockingReasons[]`** (distingue atrapado-por-diseño / por-comportamiento / por-bug; mientras 6 de 8
bloques no PUEDAN salir de calibración, su UI no insinúa desbloqueo — verificado con A4); B4 tasa de evidencia
válida/inválida por motivo (monitorización; un motivo saltando >3σ en una release = rollback de esa release); B5
rank flips post-desbloqueo (test-retest; ICC con n≈100-150 pares/bloque; el ancho de banda debe ser ≥ cambio mínimo
detectable o los flips son ruido garantizado); B6 concordancia rango inicial vs 30 días (Spearman + Bland-Altman;
sesgo sistemático ⇒ task-learning ⇒ iterar familiarización, no el conteo); B7 brechas dispositivo/accesibilidad
(elegibilidad por clase + DIF; brecha estructural en clase mayoritaria ⇒ decisión de producto antes de activar); B8
probes vs adherencia (ÚNICO aleatorizado: cadencia propuesta vs mínima — jamás un brazo "probes encubiertos";
no-inferioridad de sesiones/semana con parada secuencial); B9 gaming oportunista (correlación grinding↔velocidad de
desbloqueo debe ser ~0; si el shadow muestra que grinding acelera, NO se activa); B10 ceiling/floor a escala.

### 12.4 Shadow rollout del desbloqueo

- **S0 simulación offline** (sin datos reales): trazas sintéticas de los arquetipos (perfecto, rápido-azar,
  grinder, ocasional, cambio de dispositivo, extremo, duplicados) contra el contrato exacto §6 — incluida la regla
  48h (implementarla es prerequisito del shadow, no un detalle).
- **S1 shadow en beta** (flag OFF, log-only): `shadow_unlock_eval` nocturno; produce la D de B3, la Z de B5 y la
  sensibilidad a grinding/dispositivo sin exponer a nadie a un rango falso.
- **S2 criterios de activación (preregistrados, todos AND):** flips en sombra ≤Z en dos ventanas de 2 semanas;
  atrapados-por-causa-desconocida ~0; insensibilidad a grinding; invariancia mínima o brecha declarada; ADR
  aprobados (mínimo -003/-007) o excepción CEO explícita; copy de rango pasa scanner + entrevistas (A4 repetida).
- **S3 activación gradual por bloque:** empezar por el de mejor fiabilidad en sombra (previsiblemente Memoria),
  5%→25%→100% con B5 en vivo; **rollback = flag OFF y el rango vuelve a "Calibrando"** — un rango mostrado y luego
  cambiado en silencio es incidente de score falso (08 §21).

---

## 13. Registro de riesgos

| ID | P | Riesgo | Mitigación | Barrera fail-closed | Owner |
|---|---|---|---|---|---|
| R1 | P0 | Falso rango por volumen/grinding/autoselección | Contrato §6 (propósito sellado, dosis 1/linaje/día, 48h, cobertura ≥2 formas, anti-gaming) + B9 en sombra | El desbloqueo NO se activa si grinding acelera en sombra; `OFFICIAL_ELIGIBLE` inconstructible hasta el puente; flags OFF + `OfficialScoringReadiness=false` | psychometrician + efficacy |
| R2 | P0 | Cifra de capacidad prematura visible (hoy: Speed "TOTAL %") | S2 relabel inmediato (primera slice de producto) | Display-gate 0.55 + invariante `CALIBRATING⇔null` ya en código | efficacy (dictaminó BLOQUEA) |
| R3 | P1 | Evaluación encubierta (probes sin disclosure) | Requisitos §5.3 vinculantes + A5 con wording neutral | Sin disclosure implementado, el scheduler de probes no se monta (gate de S6) | privacy-security + efficacy |
| R4 | P1 | El canon sigue mandando el examen y una slice futura lo construye | S0 PRIMERO (SES-012 + supersesiones §10) | Regla del repo: decisión posterior solo sustituye si queda `SUPERSEDED` explícito — se ejecuta en S0 | docs-knowledge |
| R5 | P1 | Growth Index contaminado sin baseline pre-test (práctica + regresión a la media) | §6.5: warm-up exclusion, base_0 = mediana K=3 en ≥2 días, RCI-only, ruta C opcional | Growth retenido ('—') hasta base_0 establecida + ADR-SCI-003/004/011 | psychometrician |
| R6 | P1 | "Calibrando" leído como "nivel bajo" / usuarios atrapados sin explicación | Patrón §5.2 (criterios, no conteo) + "Sin clasificación" para bloques sin método + B3 blockingReasons | El estado visible deriva solo del dominio (`BlockRangeStatus`); A4 con umbral de fallo | product-ux |
| R7 | P1 | Colisión insignia social "DIAMANTE III" vs rango cognitivo (+ KDoc↔código contradictorio, client-writable) | S2: "Insignia · personalizada", gramática divergente, defaults fabricados fuera, KDoc reconciliado | Numerales romanos reservados al rango de bloque; A4 sondea la confusión (1 caso = CRITICAL) | product-ux + efficacy |
| R8 | P1 | Validación imposible: funnel de analytics INERTE (sink sin registrar, E17) | S7 antes de alpha | Guard de release (`requireRealSinkInRelease`) pasa a ejecutarse de verdad | data-analytics |
| R9 | P2 | Gate global real (≥3 ESTABLISHED) ≠ decisión CEO ("los 8") | ADR-SCI-006 redefine el gate antes de cualquier flip | Triple candado vigente (flag OFF + const false + composite null); no hay breach vivo | psychometrician |
| R10 | P2 | Rank flipping tras desbloqueo | §6.4 CI-containment + histéresis + K=2 evidencias nuevas + sticky | Parámetros por ADR-SCI-010 con simulación; sin modelo de error no hay asignación posible | psychometrician |
| R11 | P2 | El motor adaptativo de Focus exige baseline no-null (~42 archivos lo tocan) | Auditoría previa en S1; default conservador en el seam `AdaptiveModeGate.Mode.TRAINING` | El plan contenido YA funciona con perfil vacío (`?: EASIEST`, verificado) | kmp-architect |
| R12 | P2 | Usuarios estructuralmente incapaces de calibrar por dispositivo (timebase API<34, iOS actual) | Copy honesto (momento 14) + B7 cuantifica el bucket | Timebase gate ya fail-closed; jamás se degrada a medición dudosa | mobile-performance |
| R13 | P2 | Sesiones duplicadas por replay inflan `validSessions` | Dedup `distinctBy{sessionId}` en el call-site (S4) + property test de §11 | Proyección pura del conjunto (conmutativa) | data-sync-dev |
| R14 | P3 | Incoherencia de nombre Molvex vs MindBoost en repo/canon | Registrada (E2-N1); la migración de nombre es otra iniciativa | — | CEO |
| R15 | P3 | "Calibración exprés" percibida como examen | Reglas anti-examen §5.5 ("Entrenando normal también llegas" literal y verdadera) | La exprés no puede saltarse dispersión temporal (mismo contrato §6) | product-ux |

---

## 14. Unknowns y ADR

**U0 (operativo, primero de la implementación):** checkpoint del `MINDBOOST_AUDIT_LEDGER.md` de esta sesión
read-only (hallazgos E15/E16/E17 + este plan), diferido aquí por el mandato de cero mutaciones.

**Unknowns resolubles por inspección (no preguntar al CEO — se resuelven en S1/S4):**
- ¿`TrainingCycle` avanza por sesiones o por calendario? (si calendario, único fix para el usuario ocasional).
- ¿Algún componente de los ~42 archivos "baseline" de Focus exige perfil no-null? (auditoría S1; default conservador).
- ¿`PairsProfileSource` replica exactamente el patrón de `MemoryProfileSource`? (inferido de su gemelo; verificar al tocar).
- Exportación local de métricas de alpha sin sync montado (bastan logs locales + exportación pseudonimizada ya normada).

**ADR abiertos que BLOQUEAN activaciones (no este plan):** ADR-SCI-003 (modelo de error/estimador — bloquea todo
rango y el Growth-RCI), -004 (trend/cambio fiable), -005 (dosis diaria definitiva y exposición), -006 (global: gate
"los 8" + pesos), -007 (cutpoints de bandas; sin normas, bandas internas provisionales o hold), -008
(equating dispositivo/modalidad).

**ADR nuevos que este plan registra (owner: psychometrician salvo indicación; evidencia que los cierra):**
- **ADR-SCI-010 · Anti-flipping**: valores de histéresis `h`, evidencia extra `K`, número/ancho de bandas `B` —
  simulación de tasa de flip sobre distribución real (minimizar cambios espurios sin bloquear reales).
- **ADR-SCI-011 · Baseline móvil del Growth**: K exacto, ventana, exclusión de warm-up — simulación con curva de
  práctica + regresión a la media; validación RCI.
- **ADR-EVIDENCE-BLUEPRINT (W5)**: blueprint por bloque (facetas, ≥2 métodos, cobertura de dificultad, techo 50%
  por tarea) — diseño de ejercicios (04 §8.3) + simulación de cobertura.
- **ADR-PER-002 · Cold start** (ratifica el interino §5.4: EASIEST + staircase + histéresis asimétrica) — datos de
  alpha (A6: techo/suelo del arranque conservador en expertos).
- **Decisión de producto W3 (`TRAINING_FREE` en carril calibración)**: este plan RECOMIENDA mantener la allowlist
  vigente (entrenar normal ES calibración-elegible — sostiene la promesa "entrenando normal también llegas");
  ratificación CEO en S0 (es la única pregunta de producto genuina que este plan deja abierta, porque fija qué
  fracción del uso natural puede ser evidencia).

**Políticas interinas fijadas YA por este plan (reversibles por su ADR):** dosis 1/linaje/día; consumidor 48h;
≥2 form-families; "sin método de rango" para Creatividad/Emociones; hold de todo rango numérico hasta ADR-SCI-003;
cold start EASIEST.

---

## 15. Prompt de ejecución posterior (macrofase 1 — lista para pegar)

> El prompt implementa SOLO la primera macrofase aprobable (S0+S1+S2: fuentes de verdad, retirada del gate,
> coherencia de superficies). No toca contrato de calibración, probes ni shadow (S3+ requieren este plan ratificado
> y sus ADR). Formato según `07_PHASE_REVIEW_PROTOCOL.md` §21.

```markdown
# EJECUCIÓN — Molvex/MindBoost · Macrofase 1: acceso inmediato sin evaluación obligatoria (S0+S1+S2)

## 1. Objetivo y outcome de usuario
Eliminar del canon y del producto todo rastro de evaluación inicial obligatoria. Al cerrar: (a) el canon registra
SES-012 y supersede PRD §8.1(8-9)/§8.3-Etapa-1, Charter §6.4-viñeta-1 y SES-003 (+SES-002 parcial); (b) un usuario
nuevo entra en Enfoque y el botón de entrenar está SIEMPRE visible (hoy `needsBaseline` lo sustituye por "Empezar
mi base"); (c) ninguna superficie muestra una cifra tipo-capacidad prematura (Speed "TOTAL %" re-etiquetado;
ProfileCard pasa a "Insignia · personalizada" sin defaults fabricados; chips "Sin clasificación" en
Creatividad/Emociones). El rango de bloque NO se activa en esta fase (sigue todo CALIBRATING — correcto).

## 2. Contexto verificado (no re-derivar; citas en el plan maestro §2)
- El examen ~16 min NO es app-wide: es `BaselineProtocols.INITIAL` + soft-gate `needsBaseline`
  (`FocusHomeStateHolder.kt:85`; ramas UI en `FocusHomeScreen.kt:154,162,195,359,376`).
- El cold start YA existe: `startingDifficulty[id] ?: EASIEST` (`DailyFocusPlan.kt:115`);
  `RecoveryPolicy.kt:49` ya degrada honesto sin baseline; el plan contenido funciona con perfil vacío.
- Focus/Lógica siguen CALIBRATING por W1 (hardcode `measurementEligible(UNKNOWN,false)`) — NO tocar en esta fase.
- Leer OBLIGATORIO antes: docs/mindboost_calibracion_progresiva/PLAN_MAESTRO_SIN_EVALUACION_OBLIGATORIA.md
  (este plan) + 00_CONTEXT_INDEX + MINDBOOST_AUDIT_LEDGER.md (protocolo de arranque).

## 3. Tareas (en orden; gates en serie tras cada una)
U0. Checkpoint diferido del ledger (sesión read-only 2026-07-24): registrar hallazgos E15 (Speed TOTAL %),
    E16 (ProfileCard rank cosmético + KDoc contradictorio), E17 (sink analytics sin registrar) y este plan.
    Validador: node scripts/validate-audit-ledger.mjs.
S0. Docs: aplicar la matriz §10 del plan maestro COMPLETA (SES-012 en 09 §6 + fila §18; PRD §8 reescrito;
    Charter §6.4 viñeta 1 + §11.4 sin QUICK_PROVISIONAL; 00 §6 + packRelease; adiciones a 08; 05 assessment→calibration).
    documentVersion bump por doc; nada se reescribe en silencio.
S1. Código: eliminar `needsBaseline` (campo + 5 ramas UI); "Empezar mi base" → tarjeta secundaria "Calibración
    exprés · opcional" (copy EXACTO del plan §5.5, con "Entrenando normal también llegas" literal); retest copy
    (`FocusEvaluationStateHolder.kt:56-58`) → "referencia por entrenamiento o calibración exprés"; AUDITAR los ~42
    archivos de Focus que tocan "baseline" y, si el motor adaptativo exige perfil no-null, default conservador en
    `AdaptiveModeGate.Mode.TRAINING` (cambio mínimo, documentado).
S2. Código: Speed dashboard → "Resultado de la sesión" (fuera "TOTAL"; banner del plan §5.6); ProfileCard →
    "Insignia de tarjeta · personalizada", defaults fabricados (nivel 47, 742 victorias, DIAMANTE III) vacíos u
    ocultos hasta edición del dueño, KDoc↔código reconciliado; BrainProfile → leyenda "—" + chip "Sin clasificación"
    (Creatividad/Emociones) distinto de "Calibrando".

## 4. Invariantes que NINGÚN cambio puede romper
- `require((status==CALIBRATING)==(value==null))`; '—' nunca 0; global triple-bloqueado intacto.
- Ningún camino nuevo a perfil/rango: esta fase NO desbloquea medición (W1-W5 y ADR-SCI-* siguen abiertos).
- Copy: lexicon prohibido del build (no nombrar términos vetados NI para negarlos); scanners de copy verdes.
- Ninguna sesión cambia de carril: propósito/elegibilidad intactos.

## 5. Tests exigidos (mitad discriminante SIEMPRE)
- Gate ausente: state-holder de Focus Home sin `needsBaseline`; usuario nuevo ⇒ CTA entrenar presente.
- `BaselineProtocolMigrationTest`: baseline viejo persistido + 0 evidencia nueva ⇒ CALIBRATING/null; el histórico
  sigue legible como referencia de retest y semilla de dificultad.
- Speed: el compuesto "TOTAL %" no se renderiza; los observables por-barra sí (mitad discriminante).
- ProfileCard: sin edición del dueño ⇒ sin insignia/estadísticas fabricadas; con edición ⇒ marcadas "personalizado".
- Chips: Creatividad/Emociones ⇒ "Sin clasificación" (test que falla si alguien las pasa a "Calibrando" sin decisión).

## 6. Gates de salida
`:shared:desktopTest` + `:shared:detekt` + `:shared:compileKotlinDesktop` + `:androidApp:compileDebugKotlin` +
`mindboostCopySafetyScanner` (las 3 variantes de los source-sets tocados) — ejecución REAL verificada (N executed,
conteo de XML frescos; jamás aceptar UP-TO-DATE). iOS por CI si hay runner (compile + sim-test); si no, declarar
BLOCKED_IOS honesto. Gate de salida /mb: `efficacy-guardian` + `diff-auditor` en contexto fresco (+
`mindboost-product-ux` sobre el copy); los tres sobre el diff FINAL. Ledger actualizado ANTES de declarar DONE.

## 7. Fuera de alcance (prohibido en esta fase)
Contrato 48h/dosis/cobertura (S4), carrier purpose+lineage (S5), puente de evidencia/ProbeScheduler (S6), sink de
analytics (S7 — slice propia), shadow (S8), cualquier flip de flag de scoring/rango, y cualquier renombrado
Molvex↔MindBoost.

## 8. Definition of Done
Matriz §10 aplicada al 100% · usuario nuevo entrena en Enfoque sin CTA bloqueante · cero cifras tipo-capacidad en
el primer día · todos los gates verdes con ejecución real · ledger sellado (SHA recomputado, validador verde) ·
cierre con: qué cambió, archivos, tests, cómo verificar, riesgos y follow-ups (S3+ pendientes de este plan).
```

---

## Cierre

Definition of Done del encargo, verificada contra este documento: elimina totalmente la evaluación obligatoria
(§1/§10/S0-S1) · entrenar es inmediato (§5) · el rango jamás llega por volumen (§6, R1) · desbloqueo independiente
por bloque (§6.6/§7.2) · ausencia ≠ cero (invariante viva, E7) · training y measurement separados (carriles §5/§6)
· cold start + baseline + dashboard sin rango resueltos (§5.4/§6.5/§5.2-5.3) · "X sesiones" definido formalmente
(§6.1-6.2) · incertidumbre y cobertura además del conteo (§6.2/§6.4) · offline y paridad (§8.5/§11) · migración
(§9) · contradicciones documentales identificadas (§2/§10) · validación+flags+rollback (§12/§8.4) · rangos no
respaldados bloqueados (§3.4/§6.6) · sin lenguaje de IQ (§7.1 + lexicon) · sin decisiones técnicas sin recomendación
(§3.2) · implementable sin reconstruir la intención del CEO (§15).

