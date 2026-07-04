# Decisión: NO cablear BayesArena al perfil cognitivo en Fase 1 (diferido)

- **Fecha:** 2026-07-04
- **Estado:** ACEPTADA (diferir)
- **Ámbito:** MindBoost · Decisiones · BayesArena (E07/E08) → dimensión DECISION_MAKING del brain-profile
- **Tipo:** decisión de eficacia/honestidad + integridad de banco (MVP Fase 1, slice 1.4)

## Contexto

La Fase 1 (slice 1.4) proponía cablear BayesArena para que la dimensión DECISION_MAKING dejara de
estar permanentemente en `CALIBRATING`. La FASE-0 trazó el flujo real y encontró el **mismo muro** que
Lenguaje y que Reversibility: contenido no validado + sin persistencia + sin provenance.

## Estado actual (verdad del repo)

1. **BayesArena computa una señal honesta, pero solo en memoria.**
   `BayesArenaStateHolder` (`decisiones/presentation/BayesArenaStateHolder.kt`) es PURO y **clock-free**:
   por escenario captura el posterior del usuario y produce feedback — `bayesianUpdateRatio` (BUR,
   proximidad al posterior normativo en espacio logit), `absoluteError` y `confidence` — más un resumen
   (medias). Su propio KDoc advierte: *"No Brier / calibration / CAG, which are undefined without a
   resolved binary outcome."* Los `rounds` se acumulan en `DecisionsFlowStateHolder` **en memoria** y van
   al Result screen; nada persiste una señal de Bayes por sesión.

2. **El contenido es un generador paramétrico, no un banco validado.**
   `BayesEngine.generateScenario(difficulty, random)` (`com.mindverse.mindboost.core.mdp.bayes`) deriva
   `baseRate`/`sensitivity`/`falsePositiveRate` de una escala de dificultad + `jittered()` (ruido
   aleatorio) y computa `expectedPosterior` de forma normativa. Es exactamente la clase "generador
   paramétrico" que la doctrina de integridad de banco marca **`SMALL_UNVALIDATED`** (cf. aprendizaje
   2026-06-30 sobre Reversibility: un generador placeholder degenerado se marca `SMALL_UNVALIDATED`, no
   `PROCEDURAL`, y el `ScoreStateMachine` lo BLOQUEA del perfil).

3. **No existe persistencia de Bayes.** Hay `ReversibilityResultStore` (E2) y
   `SecureDecisionSessionRepository` (B1: `DecisionSession` + diario), pero **NO** un `BayesResultStore`
   análogo. Sin store, cablear solo el lado de lectura leería nada → wiring muerto (el antipatrón ya
   rechazado en Lenguaje).

4. **DECISION_MAKING ya tiene un contribuyente (bloqueado).** `DecisionsProfileSource` ya lee
   `ReversibilityResultStore` → DECISION_MAKING, pero queda bloqueada porque el contenido de Reversibility
   es un "template prompt + coin-flip label", `SMALL_UNVALIDATED` (`includeReversibility=false` en el flow
   lo confirma). Añadir BayesArena sería un **segundo** contribuyente a la misma dimensión.

## Razón de bloqueo (resumen)

- Generador paramétrico → **`SMALL_UNVALIDATED`** (muro de banco).
- **Sin `BayesResultStore`** → nada que leer (no-op).
- **Sin provenance/version** (`ScoringVersionRegistry` solo tiene `DECISIONS_REVERSIBILITY`).
- **Sin timestamp real de evento** persistido (holder clock-free) → `updatedAtMs` sería fabricado.
- **Sin `sessionEvidence`** (no hay sesiones válidas persistidas con día/id/duración).
- **Semántica pendiente** de multi-contribuyente en DECISION_MAKING (Reversibility + Bayes) — el
  follow-up multi-módulo por dimensión de `CalibrationPolicy`.

## Decisión

**Se DIFIERE el cableado de BayesArena al perfil.** No se hace en Fase 1.

- **No** cablear a perfil oficial (`ModuleContribution`/`ScoreResultFactory`/`DECISION_MAKING`).
- **No** cablear un path preliminar no observable (sería wiring muerto: sin store no hay señal que leer).
- Diferir hasta cumplir los prerrequisitos.

Cualquiera de las razones basta; juntas, cablear ahora **fabricaría una madurez que el sistema no tiene**.

Matiz honesto: BayesArena SÍ tiene una señal computable y defendible (BUR = distancia al óptimo
bayesiano), mejor base que el coin-flip de Reversibility — el camino futuro es claro, pero es una
**feature**, no una tarea de "wiring" de Fase 1.

## Prerrequisitos para cablearlo (fase futura)

1. **Banco validado** de escenarios de Bayes (o política explícita que saque el contenido de
   `SMALL_UNVALIDATED`), que pase el contrato de integridad de banco / `BankValidationGateTest`.
2. **`BayesResultStore` cifrado** (namespace propio, minimizando: solo abstracciones/resúmenes por
   sesión, nunca datos crudos), persistiendo la señal por sesión.
3. **Timestamp real de evento por sesión** (no la semilla `resultTimestampMs`; ver honestidad temporal,
   Fase 1.7 — evento real >0 o `null`, jamás 0L).
4. **Scorer versionado** que produzca `ScoreResult` vía `ScoreResultFactory` (nuevo
   `ScoringVersionRegistry.DECISIONS_BAYES`), gateado por `BankIntegrity.blocksProfileEligibility()`.
5. **`sessionEvidence`** con elegibilidad de sesión (duración/evidencia/identidad — Fase 1.6/1.8) para
   que DC-3 no se cumpla con tiempo abierto.
6. **Resolución de la semántica multi-módulo por dimensión** cuando dos fuentes (Reversibility + Bayes)
   alimenten DECISION_MAKING (techo/mezcla; follow-up de `CalibrationPolicy`).
7. **Reviews obligatorias** de `efficacy-guardian` (la señal persistida es honesta y gated) y
   `privacy-security` (nueva superficie de persistencia de datos de decisión) antes de cablear.

## Verificación

Sin cambios de código de producción en esta fase → sin tests asociados (N/A). Esta decisión formaliza
el cierre operativo de Fase 1 con 1.4 explícitamente diferido, consistente con los deferrals de Lenguaje
(2026-07-01) y con el bloqueo `SMALL_UNVALIDATED` de Reversibility (2026-06-30).
