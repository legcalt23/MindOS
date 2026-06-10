# 05 · Plan del Motor de Ejercicios

> Arquitectura común para los 13 ejercicios. Separa **generación de estímulos
> (puro, testeable)** del **runtime de presentación/timing (plataforma)**. El
> núcleo (TrialGenerator) es Kotlin puro y determinista con semilla; el render y
> el timing monótono son la única parte que toca Compose/plataforma.

## 0. Capas

```
domain/exercise/                         (PURO, commonMain)
  TrialGenerator.kt        # interfaz: produce List<Stimulus> determinista (seed)
  StimulusPayload.kt       # sealed por familia de paradigma (datos de render)
  DifficultyMapping.kt     # DifficultyVector -> parámetros concretos del ejercicio
  generators/E1VigiaGenerator.kt ... E13PresionGenerator.kt
presentation/exercise/                   (Compose, commonMain)
  ExerciseRuntime.kt       # orquesta: presenta stimulus, captura Response, sella tiempos
  TrialClock.kt            # interfaz para timing monótono
  renderers/<Exercise>Renderer.kt
{android,desktop,ios}Main/.../exercise/
  MonotonicTimeSource.<plat>.kt   # expect/actual (kotlin.time cubre casi todo)
```

## 1. TrialGenerator (puro, sembrado, determinista)

```kotlin
interface TrialGenerator {
    val exerciseId: ExerciseId
    /**
     * Genera la secuencia de estímulos para UN bloque, determinista dada la
     * semilla. Determinismo = tests reproducibles + replays.
     */
    fun generate(difficulty: DifficultyVector, blockSpec: BlockSpec, seed: Long): List<Stimulus>
}

data class BlockSpec(
    val targetTrialCount: Int,
    val mode: Mode,
    val durationBudgetMs: Long? = null,   // p. ej. E1 se rige por tiempo, no por nº trials
)
```
Usa un PRNG **propio sembrado** (`kotlin.random.Random(seed)`), nunca
`Math.random()` ni `Random.Default` (la regla del repo prohíbe fuentes no
deterministas en lógica; además rompen replays/tests).

## 2. StimulusPayload (sealed por familia)

```kotlin
sealed interface StimulusPayload {
    data class SignalDetection(val present: Boolean, val contrast: Double) : StimulusPayload // E1
    data class Flanker(val center: Symbol, val flankers: List<Symbol>, val congruent: Boolean) : StimulusPayload // E2/E5
    data class GoNoGo(val go: Boolean, val symbol: Symbol) : StimulusPayload  // E3
    data class StopSignal(val baseGo: Symbol, val stopAfterMs: Long?) : StimulusPayload // E4
    data class Conflict(val word: Symbol, val dimension: Dimension, val relevant: Dimension) : StimulusPayload // E5
    data class TaskCue(val rule: Rule, val isSwitch: Boolean) : StimulusPayload // E6
    data class Search(val items: List<SearchItem>, val targetIndex: Int?) : StimulusPayload // E7
    data class DualTask(val primary: StimulusPayload, val secondary: StimulusPayload, val priority: Priority) : StimulusPayload // E8
    data class MultiObjectTracking(val objects: List<MovingObject>, val targets: Set<Int>) : StimulusPayload // E9
    data class RsvpStream(val items: List<Symbol>, val t1: Int, val t2: Int?) : StimulusPayload // E10
    data class AnchorTask(val main: StimulusPayload, val intruders: List<Intruder>) : StimulusPayload // E11
    data class Reengagement(val provoked: Boolean, val interruption: InterruptionType) : StimulusPayload // E12
    data class Pressure(val base: StimulusPayload, val deadlineMs: Long, val errorPenalty: Double) : StimulusPayload // E13
}
```

## 3. DifficultyMapping (vector → parámetros concretos)

Cada ejercicio traduce los ejes **relevantes** del `DifficultyVector` a sus
parámetros. Ejemplo E1 Vigía:

```kotlin
object E1DifficultyMapping {
    fun map(d: DifficultyVector): E1Params = E1Params(
        durationMs        = lerp(60_000, 300_000, d.timeOnTask.value),           // 1..5 min
        targetRarity      = lerp(0.30, 0.05, d.perceptualLoad.value),            // raro = difícil
        temporalJitterMs  = lerp(800, 4000, d.temporalUncertainty.value),        // incertidumbre
        contrast          = lerp(1.0, 0.35, d.perceptualLoad.value),             // degradación
        lureSimilarity    = lerp(0.0, 0.8, d.distractorSalience.value),          // no-objetivos similares
    )
}
fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
```
**Regla:** un ejercicio solo lee los ejes de su `ExerciseDefinition.relevantAxes`.
Los demás ejes se ignoran (no afectan).

## 4. ExerciseRuntime y timing (la única parte de plataforma)

```kotlin
/** Reloj monótono de alta resolución para RT. Multiplataforma vía kotlin.time. */
interface TrialClock {
    fun markNow(): Long   // nanos monótonos; en común: TimeSource.Monotonic.markNow().elapsedNow()
}

/**
 * Orquesta un bloque: presenta cada Stimulus, sella onset, espera input/ventana,
 * sella response, clasifica TrialOutcome, emite TrialResult. NO calcula métricas
 * (eso es del MetricsEngine). NO decide dificultad (eso es del AdaptiveEngine).
 */
class ExerciseRuntime(
    private val generator: TrialGenerator,
    private val clock: TrialClock,
    private val scope: CoroutineScope,
) {
    fun run(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): Flow<TrialResult> = flow {
        val stimuli = generator.generate(difficulty, spec, seed)
        for (s in stimuli) {
            val onset = clock.markNow()
            val response = awaitResponseOrTimeout(s, window = ...)  // UI feeds input
            emit(classify(s, response, onset))
        }
    }
}
```

### Sobre el timing en móvil (riesgo A1, doc 14)
- RT = `response.responseMonotonicNs − stimulus.onsetMonotonicNs`, **ambos
  monótonos**, nunca reloj de pared.
- Sellar el onset lo más cerca posible del frame real de pintado (usar
  `withFrameNanos` para registrar cuándo el estímulo se presentó realmente).
- Restar/registrar latencia de display conocida; documentar el error residual.
- **No comparar RT absolutos entre dispositivos** sin calibración; comparar
  cambios intra-usuario (baseline vs retest en el mismo device).

## 5. Catálogo: tabla ejercicio ↔ capacidad ↔ ejes ↔ métricas ↔ módulo

| Ej | Nombre | Capacidad | Paradigma | Ejes relevantes (vector) | Métricas primarias | Métrica gobernante | Módulo |
|----|--------|-----------|-----------|--------------------------|--------------------|--------------------|--------|
| E1 | Vigía | Vigilancia sostenida | PVT/Mackworth | timeOnTask, perceptualLoad, temporalUncertainty, distractorSalience | lapses, RT-CV, decrementSlope, falseAlarms, anticipations | RT-CV/lapses | A |
| E2 | Filtro | Selectiva + interferencia | Flanker | perceptualLoad, interference, distractorSalience, pressure | congruencyEffect, accuracy, captureErrors | congruencyEffect | B |
| E3 | Gatillo | Inhibición proactiva | Go/No-Go | inhibitoryDemand, perceptualLoad, pressure | commissions(FA), omissions, RT_go, PES | falseAlarms | C |
| E4 | Freno | Inhibición reactiva | Stop-signal | inhibitoryDemand, pressure | SSRT, stopRate, RT_go | SSRT | C |
| E5 | Conflicto | Control interferencia | Stroop/Simon | interference, pressure, perceptualLoad | congruencyEffect (por paradigma), transfer | congruencyEffect | D |
| E6 | Cambio | Flexibilidad controlada | Task switching | interference, temporalUncertainty, pressure | switchCost, perseveration, mixingCost | switchCost | D |
| E7 | Rastreo | Búsqueda/filtrado | Visual search | perceptualLoad, distractorSalience, pressure | setSizeSlope, accuracy, FA, RT | setSizeSlope | B |
| E8 | Dúo | Doble tarea | Dual-task | dualTaskLoad, perceptualLoad | dualTaskCost, balance | dualTaskCost | F |
| E9 | Enjambre | Seguimiento múltiple | MOT | perceptualLoad, dualTaskLoad, distractorSalience | trackingThreshold, accuracy, speedEffect | trackingThreshold | B/F |
| E10 | Flujo | Atención temporal | RSVP | temporalUncertainty, perceptualLoad | detection, attentionalBlink, RT | blinkMagnitude | E |
| E11 | Ancla | Resistencia a distracción | Digital distraction | distractorSalience, dualTaskLoad, pressure | resistanceIndex, intrusionInteractions, recovery | resistanceIndex | F |
| E12 | Re-enganche | Recuperación post-error | (mixto) | temporalUncertainty, pressure, inhibitoryDemand | postErrorAccuracy, PES, timeToBaseline, negStreak | postErrorAccuracy | E |
| E13 | Presión | Precisión bajo deadline | Deadline | pressure, perceptualLoad, interference | pressureDelta, efficiencyUnderDeadline, hastyResponses | pressureDelta | F |

## 6. Reglas por ejercicio (resumen ejecutable)

- **E1 Vigía:** responder solo a objetivo raro/impredecible. Castigar falsas
  alarmas. Gobierna el tiempo (duración), no nº de trials.
- **E2 Filtro:** responder al centro ignorando flancos. Proporción de
  incongruentes = `interference`.
- **E3 Gatillo:** go frecuente, no-go raro. Detectar comisiones (FA).
- **E4 Freno:** responder por defecto, abortar en stop-signal. **SSD adaptativo**
  (tracking 1-up/1-down hacia p(stop)=0.5). Detectar gaming (ralentizar).
- **E5 Conflicto:** dimensión relevante vs conflictiva; medir transferencia
  entre Stroop/Simon/Flanker.
- **E6 Cambio:** aplicar regla activa, cambiar al cue. switchCost + perseveración.
- **E7 Rastreo:** localizar objetivo entre N distractores; pendiente set-size.
- **E8 Dúo:** dos tareas con prioridad; mantener ambas sobre umbral.
- **E9 Enjambre:** seguir N objetivos entre idénticos en movimiento; umbral.
- **E10 Flujo:** detectar T1/T2 en RSVP; medir attentional blink (lag T1-T2).
- **E11 Ancla:** tarea principal + intrusos tipo banner; índice de resistencia.
- **E12 Re-enganche:** continuar tras error provocado sin espiral; accuracy
  post-error, tiempo a baseline.
- **E13 Presión:** comparar calma vs deadline con coste por error; delta precisión.

## 7. Orden de implementación de ejercicios (y por qué)

**Vertical slice primero = E1 Vigía** (Fase 5). Razón: es el más simple de
renderizar (un estímulo, una respuesta), ejercita el camino completo
generator→runtime→métricas→adaptación→sesión→persistencia, y sus métricas
(RT-CV, lapsos, decremento) son las más representativas del sistema.

Luego **núcleo** (Fase 6): E2 Filtro, E3 Gatillo, E4 Freno, E5 Conflicto — cubren
selectiva, inhibición e interferencia con paradigmas bien validados y render
sencillo (símbolos estáticos). E4 introduce el SSD adaptativo (más complejo).

**Avanzados al final** (Fase 9): E6, E7, E9 (movimiento/MOT — render exigente),
E8 (doble tarea), E10 (RSVP timing fino), E11 (intrusos), E12, E13.

Justificación: los avanzados requieren render con movimiento/animación o timing
sub-frame estricto; no deben bloquear el lazo de medición/adaptación que se
valida con los simples.

## 8. Criterios de aceptación (motor de ejercicios)

- [ ] `TrialGenerator` puro y determinista (misma seed → misma secuencia), testeado.
- [ ] `DifficultyMapping` por ejercicio solo lee `relevantAxes`.
- [ ] RT siempre con reloj monótono; onset sellado en frame real.
- [ ] `ExerciseRuntime` no calcula métricas ni decide dificultad (separación).
- [ ] Cada ejercicio: test de generación + test de clasificación de outcomes.
- [ ] E1 completo antes de cualquier otro ejercicio (vertical slice).
