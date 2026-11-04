# 7. Motor adaptativo recomendado

# Sección 7 — Motor adaptativo recomendado (diseño unificado)

## 7.0 Veredicto: qué hay realmente

Auditados los 7 motores **y sus call-sites de producción**. El hallazgo transversal no es que los motores sean malos: es que **cuatro de los siete están total o parcialmente desconectados**, y la mayor parte de las ramas de seguridad que sí existen reciben constantes hardcodeadas.

| # | Motor | Diseño | ¿Cableado? | Veredicto |
|---|---|---|---|---|
| 1 | `DefaultEmotionAdaptiveEngine` (`emotions/domain/adaptive/EmotionAdaptiveEngine.kt:25-165`) | Robusto | **Sí, completo** (`EmotionSessionStateHolder.kt:473-487`) | **REUTILIZAR COMO ESPINA** |
| 2 | `MultiSignalStaircaseEngine` (`speed/domain/StaircaseEngine.kt:169-224`) | Robusto en control, **contaminado en señal** | Sí (`SpeedExerciseScreen.kt:140`) | Reutilizar el lazo, arreglar la señal |
| 3 | `DefaultAdaptiveEngine` (`focus/domain/adaptive/AdaptiveEngine.kt:23-181`) | Robusto | **Sí, pero con 4 entradas muertas** | Arreglar el call-site, no el motor |
| 4 | `DefaultMemoryAdaptiveEngine` (`memory/domain/adaptive/MemoryAdaptiveEngine.kt:32-139`) | Rico pero con **escalar roto** | Sí, con toda la seguridad en defaults | Arreglar escalar + call-site |
| 5 | `DefaultAdaptationEngine` (`language/domain/adaptation/DefaultAdaptationEngine.kt:19-94`) | La mejor política de **selección** del repo | **MUERTO** (0 call-sites) | Cablear |
| 6 | `DefaultLogicDifficultyAdapter` (`logic/domain/adaptive/LogicDifficultyAdapter.kt:28-128`) | Ingenuo | **Decorativo** (`nextDifficulty` sin consumidores) | Reescribir o borrar |
| 7 | `DifficultyAdapter` (`com/mindverse/mindboost/core/adaptive/DifficultyAdapter.kt:5-55`) | Ingenuo (bang-bang) | **MUERTO** (0 call-sites) | Borrar |

### Los 6 hallazgos que invalidan la personalización actual

**[Mejora crítica] H1 — Focus: la mitad del motor es inalcanzable.** `FocusSessionStateHolder.kt:477-487` construye el `AdaptiveInput` con `perAxisMastery = emptyMap()`, `fatigue = FatigueSignal.NONE`, `blocksSinceChange = Int.MAX_VALUE`. Consecuencias verificables sobre `AdaptiveEngine.kt`:
- `fatigueDecision` (l.29-43): `StopSession` y `Cooldown` **nunca disparan**.
- Histéresis (l.114): `Int.MAX_VALUE < 1` es false → **nunca dispara**.
- `SwitchAxis` (l.122): `isSaturated` lee `perAxisMastery` vacío → `return false` → **nunca dispara**.
- `StepUp` (l.132-133): la condición `fatigue.level == LOW` es **trivialmente cierta siempre** → el paso arriba no tiene puerta de fatiga.
Y `grep` confirma que **`FocusSessionStateHolder` no usa `AdaptiveModeGate`** (`focus/domain/evaluation/AdaptiveModeGate.kt:20`, 0 call-sites en producción): la invariante "solo se adapta en TRAINING" **no está enforzada en la ruta viva**; lo único que la cubre parcialmente es la rama `FocusAdaptationPolicy.FIXED` (l.472-476).

**[Mejora crítica] H2 — Memory: la zona depende de si existe una métrica de RT.** `MemoryPerformance.of` (`MemoryAdaptiveModels.kt:134-148`) hace `accuracy = immediate ?: delayed`, `delayedAccuracy = delayed ?: immediate`. En un bloque sin recuerdo diferido —el caso normal— **la misma precisión se cuenta dos veces con peso 0.40+0.40**, y `speedNorm` devuelve 0.5 neutro si no hay RT (l.152). El escalar colapsa a `P = 0.8·acc + 0.1`:

| acc | sin RT (`speedNorm=0.5`) | con RT rápido (`speedNorm=1.0`) |
|---|---|---|
| 1.00 | 0.90 → SUBOPTIMAL_HIGH | 1.00 → TOO_EASY |
| 0.75 | 0.70 → OPTIMAL | 0.80 → OPTIMAL (borde) |
| 0.55 | 0.54 → SUBOPTIMAL_LOW | 0.64 → OPTIMAL |

Es decir: **la misma precisión cae en zonas distintas según si el ejercicio registró RT o no**, y `TOO_EASY` es prácticamente inalcanzable sin RT. La banda óptima efectiva (acc ∈ 0.5625–0.875) no es una decisión de diseño: es el subproducto de un doble conteo y de una constante neutra. Además el escalar **premia velocidad con peso 0.20 dentro de un módulo de memoria**, mientras el propio motor tiene una rama `WorkSpeed` (l.81-82) cuyo propósito declarado es justo lo contrario: desacoplar velocidad de dificultad. Contradicción interna.

**[Mejora crítica] H3 — Memory: toda la capa de seguridad está en defaults.** `MemorySessionStateHolder.kt:264-270` pasa solo `(exercise, current, recentBlock)`. Por tanto `fatigue = NONE`, `consecutiveFrustrationBlocks = 0`, `blocksSinceChange = Int.MAX_VALUE`, `lastRaisedAxis = null`, `confidenceBias = 0.0` → `StopSession`, `ProposeRest`, `ActivateMetacognition` y la histéresis **están muertas**. Y `confidenceBias = 0.0` hardcodeado (también en l.263) significa que **el gap confianza↔rendimiento no existe en producción** pese a estar modelado.

**[Mejora crítica] H4 — Logic: la adaptación no toca nada.** `DifficultyDecision.nextDifficulty` (`LogicDifficultyAdapter.kt:18`) **no tiene ningún consumidor** en `commonMain`: `LogicStateHolder.kt:211-212` usa solo `.reason` como texto de recomendación. `thetaEstimate` (l.66) se escribe y se persiste pero **ningún selector lo lee**. La dificultad servida sale de `question.item.difficultyLevel` (`LogicStateHolder.kt:135`) sobre una lista fija. Además el "theta" no es IRT: `theta + (avgIcr − 65)/100` es un integrador de ganancia fija sin dificultad ni discriminación de ítem, y `uncertainty *= 0.92` (l.68) **decae aunque el intento no aporte información** — contabilidad bayesiana falsa. Y `transferConfirmed` con n=1 (l.96-97) hace que **un acierto por azar (1/3) suba dificultad**.

**[Riesgo científico] H5 — Speed: dificultad ≡ deadline, y la señal que la gobierna mezcla lentitud con error.** `StaircaseConfig.deadlineNanos` (`StaircaseEngine.kt:58-62`) mapea CCE→deadline y **ese es el único eje**. El gobernante es `pc`, que cuenta correcto-pero-tardío como incorrecto: el staircase no puede distinguir "se equivoca" de "es lento". El lazo es formalmente impecable (puertas null-safe l.205-214, `FatigueIndex` corrido desde el bloque 3) pero **converge a un punto de precisión no especificado**: no hay `targetP`, hay una banda 0.70–0.85 con pasos asimétricos fijos (`deltaUp` proporcional, `deltaDown` constante). Eso no es un staircase psicofísico; es un termostato sin consigna.

**[Dato necesario] H6 — Señales recolectadas y jamás usadas.** `SubjectiveState` (sueño/cafeína/estado de ánimo, `focus/domain/model/Results.kt:60`) se captura, se persiste y se sincroniza (`FocusSessionCloudDto.kt:28` la marca *health-adjacent*) pero **`AdaptiveInput` no tiene campo para ella**. `PlateauDetector` e `InterventionPlanner` se **construyen** en `MemoryGraph.kt:107-108` y **nunca se invocan** (0 llamadas a `.detect(`/`.plan(`). Recolectar datos sensibles que no alimentan ninguna decisión es coste de privacidad sin beneficio.

---

## 7.1 Data inputs

El kernel unificado consume **un solo tipo de entrada**, y cada campo es `null` cuando no se midió — nunca un centinela. Marcado: `[HOY]` = ya existe en el repo con esa semántica; `[NUEVO]` = hay que producirlo.

```kotlin
/** Todo lo que una decisión adaptativa necesita. Inmutable, sin I/O, sin reloj propio. */
data class ChallengeInput(
    // ── Identidad de la tarea (determina QUÉ política aplica) ──────────────
    val target: ChallengeTarget,          // [NUEVO] ACCURACY_BAND | STAIRCASE | ERROR_RATE_BAND
    val axes: AxisSpec,                   // [HOY] ejes relevantes + eje gobernante + cuáles son temporales
    val mode: Mode,                       // [HOY] TRAINING | EVALUATION | CALIBRATION

    // ── Rendimiento del bloque (null = no medido, NUNCA 0.0) ──────────────
    val accuracy: Measured?,              // [HOY] MetricValue confident-gated
    val commissionRate: Measured?,        // [HOY] emotions/focus
    val omissionRate: Measured?,          // [HOY]
    val rtMedianMs: Measured?,            // [HOY]
    val rtCv: Measured?,                  // [HOY]
    val impulsivityIndex: Measured?,      // [HOY] speed impIdx
    val errorTypeMix: Map<ErrorKind, Int>,// [HOY parcial] logic errorTags, memory intrusions
    val trailingConsecutiveErrors: Int,   // [HOY] focus/emotions
    val postErrorAccuracyDelta: Measured?,// [NUEVO] recuperación tras fallo (acc n+1 tras error − acc base)
    val nTrials: Int,                     // [HOY] gobierna todo gate de muestra

    // ── Estado del usuario ────────────────────────────────────────────────
    val fatigue: FatigueSignal,           // [HOY solo speed/emotions] focus y memory lo pasan NONE
    val overloadProxies: Set<OverloadProxy>, // [HOY solo emotions]
    val selfReportIntensity: Int?,        // [HOY solo emotions] 1..5
    val subjectiveState: SubjectiveState?,// [HOY capturado, NUNCA usado] sueño/cafeína/ánimo
    val abandonmentEvents: Int,           // [HOY emotions ABANDONMENT] no-respuestas del bloque
    val consecutiveFrustrationBlocks: Int,// [HOY memory, siempre 0 en producción]

    // ── Metacognición ─────────────────────────────────────────────────────
    val confidenceBias: Measured?,        // [HOY memory param, hardcodeado 0.0; logic lo calcula y lo tira]

    // ── Historia ──────────────────────────────────────────────────────────
    val blocksSinceChange: Int,           // [HOY, siempre Int.MAX_VALUE en focus/memory]
    val perAxisMastery: Map<Axis, Mastery>,// [HOY, siempre vacío en focus]
    val recentImprovement: TrendSignal?,  // [NUEVO] pendiente sobre N sesiones vs línea base propia
    val holdoutDelta: Measured?,          // [NUEVO*] ganancia en ítems nunca entrenados (*emotions ya lo tiene)

    val config: ChallengeConfig,
)

/** Un valor medido con su n. Absence-safe por construcción: no existe la variante "0.0 sin datos". */
data class Measured(val value: Double, val n: Int, val confident: Boolean)
```

**Reglas duras sobre las entradas** (heredadas del charter, hoy violadas por omisión):
1. Ninguna entrada tiene default en el constructor. Si el call-site no la puede producir, pasa `null` **explícitamente** y el kernel lo registra en la traza. Los defaults de `MemoryAdaptiveInput` (`MemoryAdaptiveModels.kt:112-121`) son el mecanismo exacto por el que se murieron cuatro reglas de seguridad sin que ningún test lo notara.
2. `Measured?` nunca se colapsa a `Double` con elvis. `confidentValueOf` (focus) y `valueIfConfident` (emotions) son el patrón correcto; `rawValueOf(key, 0.0)` (`MemoryAdaptiveEngine.kt:120,139`) es el patrón prohibido.
3. Ninguna señal que el módulo no capture puede *inferirse*. Si Focus no tiene estimador de fatiga (verificado: `FatigueSignal(` solo aparece en `AdaptiveModels.kt`), la respuesta correcta es `fatigue = null` → rama `Hold(SIGNAL_UNAVAILABLE)`, **no** `FatigueSignal.NONE`, que es una afirmación positiva de "no hay fatiga".

---

## 7.2 Rule-based algorithm

### 7.2.1 Zona óptima de desafío POR TIPO de ejercicio

El error de diseño transversal es que **cada módulo inventó su propia banda sin declarar una consigna**. Hoy coexisten: 0.85/0.65 (focus, `AdaptiveConfig.kt:83,85`), 0.85/0.70 (speed, `StaircaseConfig:45-46`), 0.80/0.55 zona (memory), 0.85/0.78 + 0.80/0.70 + 0.10/0.25 (emotions, `EmotionAdaptiveEngine.kt:149-154`), 0.82 (`DifficultyAdapter.kt:43`, muerto). Cinco bandas, cero justificación, cero consigna.

El kernel declara **tres familias** y cada ejercicio declara la suya. Emotions ya lo hace bien con `AdaptiveZone` (GENERAL / TOLERANCE / COMMISSION) — es la abstracción a generalizar.

| Familia | Consigna | Regla de movimiento | Ejercicios |
|---|---|---|---|
| **A. ACCURACY_BAND** | `targetP` explícito (recomendado 0.80; banda muerta ±0.06) | Paso proporcional al error `p − targetP`; nunca por un solo bloque bajo el `nMin` | Memory span, Logic, Language, Emotions GENERAL/TOLERANCE, Focus E2/E5/E6, Creatividad |
| **B. PSYCHOPHYSICAL_STAIRCASE** | `targetP` **derivado de la regla**: 1-arriba/1-abajo → 50%; 1-arriba/2-abajo → 70.7%; 1-arriba/3-abajo → 79.4% | Movimiento por trial sobre un eje continuo; el umbral es la **media de las reversiones post-burn-in**, no el valor final | Focus E4 (`StopSignalTracker` ya lo hace bien), Speed deadline, cualquier SOA/ISI |
| **C. ERROR_RATE_BAND** | Banda de **tasa de comisión** (recomendado 0.10–0.25; fuera de banda por arriba = bajar, por abajo = subir) | Igual que A pero con el signo invertido y `nMin` mayor (una tasa de 0.15 exige ≥24 trials para tener ±0.07 de EE) | Focus E1/E3, Emotions EM1/EM8, go/no-go en general |

**Por qué esto importa y no es burocracia:** hoy nadie puede responder "¿a qué precisión pretende converger este ejercicio?". Con la consigna declarada, (a) el `nMin` se deriva del error estándar necesario para distinguir el rendimiento observado de la consigna, (b) el tamaño de paso se puede hacer proporcional al error en vez de constante, y (c) **se puede testear que el motor converge**: un respondedor sintético con habilidad conocida debe estabilizarse en la consigna. Ningún motor actual tiene ese test porque ninguno tiene consigna.

### 7.2.2 Orden de precedencia (una sola tabla, no siete)

El orden viene de Emotions (`EmotionAdaptiveEngine.kt:27-35`), que es el único que lo tiene explícito como lista de reglas ordenada. Se extiende con lo que Focus y Memory aportan:

```
 0. MODE_GATE          — mode != TRAINING          → Hold(FIXED_DIFFICULTY_MODE)      [hoy no enforzado]
 1. SAFETY_STOP        — autoinforme 4-5 / fatiga alta sostenida / 3 bloques frustración → StopSession
 2. REDUCE_INTENSITY   — solo módulos con escala de intensidad (emotions)            [nunca auto-sube]
 3. CHECK_IN           — proxies de sobrecarga disparados y sin resolver             → pregunta, no adapta
 4. COOLDOWN           — fatiga emergente / enlentecimiento sostenido                → baja TIEMPO-EN-TAREA
 5. ABANDONMENT_GUARD  — ≥N no-respuestas en el bloque                               → StepDown + no puntúa  [NUEVO]
 6. METACOGNITION      — |confidenceBias| > umbral                                   → overlay, NUNCA dificultad
 7. CONTAIN_IMPULSIVITY— comisiones rápidas, SOLO si la familia tiene el constructo   → sube demanda inhibitoria
 8. STEP_DOWN          — fuera de banda por abajo / N errores consecutivos / CV disparado
 9. HYSTERESIS         — blocksSinceChange < cooldown                                → Hold(HYSTERESIS)
10. INSUFFICIENT_N     — nTrials < nMin(target)                                      → Hold(SIGNAL_UNAVAILABLE)
11. SWITCH_AXIS        — eje gobernante saturado y existe otro eje relevante más débil
12. STEP_UP            — en/sobre consigna Y todas las puertas de compromiso pasan
13. HOLD               — banda de mantenimiento
```

Cinco cosas no negociables en ese orden:

- **La seguridad precede a la evidencia** (regla 1-5 antes que 8-12). Focus y Emotions ya lo hacen; Memory lo tiene diseñado y muerto.
- **La metacognición (6) nunca mueve dificultad.** Un usuario sobreconfiado tiene un fallo de calibración, no de capacidad; subirle la carga castiga lo equivocado. `DefaultMemoryAdaptiveEngine` ya acierta aquí (`ActivateMetacognition` no toca `current`, l.55).
- **`CONTAIN_IMPULSIVITY` (7) exige gating por familia.** `FocusAdaptiveRulePolicy.allowsImpulsivityContainment` (`FocusAdaptiveRulePolicy.kt:35-36`) es la única implementación correcta de esto en el repo y debe generalizarse: una regla cuyo constructo no existe en la tarea devuelve `NOT_APPLICABLE_TO_FAMILY`, no "no disparó".
- **`INSUFFICIENT_N` (10) va ANTES de subir y DESPUÉS de bajar.** Bajar por seguridad con poca muestra es aceptable (fail-closed); subir con poca muestra es fabricar progreso. Hoy Speed sube con n=12 y puertas de `cv`/`impIdx` calculadas con MAD sobre ≥4 valores — estadística de ruido gobernando dificultad.
- **Pasos asimétricos:** bajar más rápido que subir (focus 0.12/0.08, memory 0.18/0.08, emotions 0.12/0.07 — los tres coinciden y están bien). **Bajar salta la histéresis; subir no.**

### 7.2.3 Cómo se evita medir dificultad-como-velocidad

Este es el fallo de diseño con más consecuencias: apretar un deadline sube la dificultad *percibida* sin subir la carga cognitiva. Speed lo hace estructuralmente (CCE→deadline es su único eje), Memory lo hace accidentalmente (peso 0.20 a `speedNorm` en el escalar de zona) y Focus lo evita por el lado del score pero no del motor.

**Regla del kernel — invariante `NO_TIME_FOR_ABILITY`:**

```
Un STEP_UP puede mover un eje TEMPORAL (ventana, deadline, ISI, SOA) sólo si:
  (a) el constructo declarado del ejercicio ES velocidad de procesamiento, Y
  (b) la exactitud en el nivel actual está en/sobre consigna medida con una regla
      de puntuación que NO trate la tardanza como error, Y
  (c) existe al menos UN eje no-temporal ya saturado (masteryLevel >= saturación).
En cualquier otro caso el STEP_UP mueve un eje de CARGA (nº de elementos, similitud
de distractores, proporción de incongruentes, demora, interferencia, nº de alternativas).
```

La condición (b) obliga a partir `pc`: el staircase de Speed necesita **dos señales separadas** — `judgementAccuracy` (correcto/incorrecto ignorando el deadline) y `deadlineCompliance` (dentro de plazo). Hoy `SpeedSessionStateHolder.kt:193` las colapsa y por eso el motor no puede distinguir "se equivoca" de "es lento", que es precisamente la distinción que la condición (b) necesita. Es un cambio de una línea en la captura y desbloquea todo lo demás.

Complementariamente, **la puerta de compromiso de Emotions es el mejor código de personalización del repo** y debe ser universal: `engagementSufficientForStepUp` (`EmotionAdaptiveEngine.kt:125-131`) veta la subida por RT mediano excesivo, CV excesivo, hit-rate bajo u omisiones altas, y **cada veto solo actúa cuando la señal está confiablemente presente** (una señal ausente no puede probar desenganche). Eso resuelve simultáneamente "correcto pero lentísimo" y "abandonó a medias" sin sobre-bloquear.

### 7.2.4 Detección de meseta y de sobrecarga de entrenamiento

Ambas son **cross-sesión**, no intra-bloque, y hoy ninguna corre.

**Meseta / mejora falsa** — `PlateauDetector` (`memory/domain/antiplateau/PlateauDetector.kt:46-68`) ya tiene la lógica correcta y **nunca se invoca**. Su regla de mejora falsa (`trainedImprovement > 0.15 && untrainedImprovement < 0.05`) es exactamente el discriminante que separa aprendizaje de familiaridad. Para computarla hace falta `untrainedImprovement`, y **hoy sólo Emotions puede producirlo**: `EmotionHoldoutProbe` administra una sonda sobre ítems nunca entrenados antes del entrenamiento de la sesión (`EmotionSessionStateHolder.kt:411-425`), con la disciplina correcta (si no se puede saber qué ítems ya se quemaron, no se sondea). Esa es la pieza a generalizar a todos los módulos con banco.

```
Meseta = ≥7 días observados ∧ |Δaccuracy| < 0.03 ∧ Δvelocidad ≤ 0 ∧ Δrecuerdo-diferido ≤ 0 ∧ dificultad NO cambió
Mejora falsa = Δ_entrenado > 0.15 ∧ Δ_holdout < 0.05      ← la única prueba de que no es memorización
Regla de oro = ≥2 semanas sin mejora → replanteo de objetivo (bajar frecuencia, redirigir tiempo)
```

Sin `holdoutDelta`, un plateau detector **no puede** distinguir meseta de techo de práctica, y toda "mejora" reportada es indistinguible de familiaridad con el banco. Es el mismo agujero que las auditorías de Language (banco de 6 ítems en orden fijo), Logic (los mismos 6 ítems siempre) y Memory (banco pequeño) señalan por separado.

**Sobrecarga de entrenamiento** — `DefaultOvertrainingGuard` (`focus/domain/personalization/OvertrainingGuard.kt:25-75`) es el único detector que existe y **sí está cableado** (`FocusHomeStateHolder.kt:30`). Su lógica es defendible y se generaliza tal cual:

```
ForceRest  = fatiga alta en las 3 sesiones más recientes
           ∨ (CV_RT > 1.5·CV_base_del_usuario ∧ precisión en tendencia descendente)
Caution    = precisión en tendencia descendente sola
```

Dos correcciones necesarias: (1) `baselineRtCv` nulo → `cvDegraded = false` (l.50), lo que en un usuario sin calibración desactiva la mitad del guard en silencio — debe reportar `UNKNOWN`, no `Ok`; (2) el lenguaje de salida debe ser de rendimiento frente a la propia línea base, nunca de estado de salud.

---

## 7.3 Future ML version

No antes de tener (a) consigna declarada por ejercicio, (b) holdout rotativo en todos los bancos, (c) ≥300 sesiones/ejercicio con la señal separada de exactitud y latencia. Antes de eso, un modelo aprendería el ruido de los defaults hardcodeados actuales.

**Fase 1 — IRT/CAT de 2 parámetros por dominio (la más rentable).** Es lo que `DefaultAdaptationEngine` (Language) *casi* hace: su `information = 1.0 − reliability` (`DefaultAdaptationEngine.kt:34-35`) es un proxy de fiabilidad de rasgo, **no información de Fisher**: no depende de la dificultad del ítem relativa a θ, así que prefiere rasgos poco medidos en lugar del ítem que más informa sobre ESTE usuario. Sustituir por `I(θ) = a²·P(θ)·(1−P(θ))` con `a`,`b` calibrados offline convierte esa política de selección (que ya pondera reparación, transferencia, exposición, fatiga y confusores) en un CAT real. `LogicDifficultyAdapter.thetaEstimate` debe **borrarse**, no migrarse: es un integrador de ganancia fija disfrazado de θ, y su `uncertainty *= 0.92` decae aunque el intento no informe.

**Fase 2 — Modelo de fatiga/abandono personalizado.** Regresión logística sobre `(bloque, tiempo-en-tarea, ΔRT, ΔCV, Δlapsos, hora del día, autoinforme)` → `P(abandono | continuar)`. Alimenta las reglas 4-5, no las sustituye: el ML propone el umbral, la regla decide. Etiqueta = evento de abandono real, que **hoy no se emite** (ver 7.5).

**Fase 3 — Curva de aprendizaje jerárquica (bayesiana, parcialmente compartida).** `y_it = a_i·(1 − exp(−b_i·t)) + c_i` con priors poblacionales por dominio. Da tres cosas que las reglas no pueden: intervalo de credibilidad sobre "¿ha mejorado?", detección de meseta como `b_i·t >> 1` en lugar de un umbral de 7 días, y un contrafactual honesto para el efecto práctica.

**Prohibido en cualquier fase:** que el modelo prediga transferencia lejana, que infiera rasgos de personalidad o estado clínico, o que sustituya las reglas 0-6 (seguridad). El ML modula magnitud de paso y selección de ítem; **la dirección de seguridad siempre es determinista y fail-closed**.

---

## 7.4 Pseudocode

```kotlin
/** Kernel único. Puro, determinista, sin I/O, sin reloj. Un módulo = una ChallengePolicy. */
object AdaptiveKernel {

    fun decide(input: ChallengeInput): ChallengeDecision =
        RULES.firstNotNullOfOrNull { it(input) } ?: hold(input, HoldReason.MAINTENANCE_BAND)

    private val RULES: List<(ChallengeInput) -> ChallengeDecision?> = listOf(
        ::modeGate,            // 0  invariante estructural, no puede saltarse en el call-site
        ::safetyStop,          // 1
        ::reduceIntensity,     // 2  solo módulos con escala de intensidad; NUNCA auto-sube
        ::checkIn,             // 3
        ::cooldown,            // 4
        ::abandonmentGuard,    // 5
        ::metacognition,       // 6  overlay, jamás dificultad
        ::containImpulsivity,  // 7  gated por familia (FocusAdaptiveRulePolicy generalizado)
        ::stepDown,            // 8  salta histéresis: bajar es seguridad
        ::hysteresis,          // 9
        ::insufficientSample,  // 10 solo bloquea la SUBIDA; stepDown ya pasó
        ::switchAxis,          // 11
        ::stepUp,              // 12
    )

    // ── 0. La invariante que hoy no se enforza (AdaptiveModeGate tiene 0 call-sites) ──
    private fun modeGate(i: ChallengeInput) =
        if (i.mode == Mode.TRAINING) null
        else hold(i, HoldReason.FIXED_DIFFICULTY_MODE)

    // ── 10. Sin muestra no se sube. Con muestra insuficiente se puede BAJAR (fail-closed). ──
    private fun insufficientSample(i: ChallengeInput): ChallengeDecision? =
        if (i.nTrials >= nMinFor(i.target, i.config)) null
        else hold(i, HoldReason.SIGNAL_UNAVAILABLE)

    // ── 12. Subir: consigna alcanzada Y ninguna señal de compromiso vetando ──
    private fun stepUp(i: ChallengeInput): ChallengeDecision? {
        val p = governingSignal(i) ?: return hold(i, HoldReason.SIGNAL_UNAVAILABLE)
        if (!onOrAboveTarget(p, i.target, i.config)) return null
        if (!engagementOk(i)) return hold(i, HoldReason.ENGAGEMENT_VETO)   // ← puerta de Emotions
        if (i.trailingConsecutiveErrors > 0) return null
        if (i.fatigue.level != FatigueLevel.LOW) return null               // ← hoy trivialmente cierto en Focus
        val axis = axisToRaise(i)                                          // ← NO_TIME_FOR_ABILITY se aplica aquí
        val step = proportionalStep(p, i.target, i.config.stepUpBase)
        return ChallengeDecision(StepUp(axis), i.axes.current.stepUp(axis, step), rationale = "...")
    }

    /**
     * INVARIANTE NO_TIME_FOR_ABILITY: un eje temporal solo se sube si el constructo ES velocidad,
     * la exactitud está en consigna bajo una regla que NO penaliza la tardanza, y ya hay un eje
     * de carga saturado. En cualquier otro caso se sube CARGA, no presión temporal.
     */
    private fun axisToRaise(i: ChallengeInput): Axis {
        val loadAxes = i.axes.relevant.filterNot { it.isTemporal }
        val timeAllowed = i.axes.constructIsSpeed &&
            i.accuracy?.takeIf { it.confident }?.let { it.value >= i.config.targetP } == true &&
            loadAxes.any { saturated(i, it) }
        val weakestLoad = loadAxes.minByOrNull { masteryOf(i, it) }
        return weakestLoad?.takeIf { !timeAllowed || masteryOf(i, it) < i.config.saturation }
            ?: i.axes.governing
    }

    /** Puerta de compromiso — puerto directo de EmotionAdaptiveEngine.engagementSufficientForStepUp. */
    private fun engagementOk(i: ChallengeInput): Boolean {
        val rtOk        = i.rtMedianMs.vetoIfConfident { it <= i.config.maxStepUpRtMs }
        val cvOk        = i.rtCv.vetoIfConfident      { it <= i.config.maxStepUpRtCv }
        val omissionOk  = i.omissionRate.vetoIfConfident { it <= i.config.maxStepUpOmission }
        val recoveryOk  = i.postErrorAccuracyDelta.vetoIfConfident { it >= i.config.minPostErrorRecovery }
        return rtOk && cvOk && omissionOk && recoveryOk
    }
    /** Una señal ausente NO puede probar desenganche: sin dato → no veta. */
    private inline fun Measured?.vetoIfConfident(ok: (Double) -> Boolean): Boolean =
        this?.takeIf { it.confident }?.let { ok(it.value) } ?: true

    /** La señal gobernante depende del TIPO, no del módulo. */
    private fun governingSignal(i: ChallengeInput): Double? = when (i.target.family) {
        ACCURACY_BAND   -> i.accuracy?.takeIf { it.confident }?.value
        ERROR_RATE_BAND -> i.commissionRate?.takeIf { it.confident }?.value
        STAIRCASE       -> null   // el staircase no usa banda: reversiones, ver StaircaseTracker
    }
}

/** Familia B: staircase psicofísico de verdad — consigna DERIVADA de la regla, umbral por reversiones. */
class StaircaseTracker(private val rule: UpDownRule, private val step: Double) {
    private val reversals = mutableListOf<Double>()
    private var runOfCorrect = 0
    private var lastDirection: Int? = null

    fun onTrial(correct: Boolean, level: Double): Double {
        runOfCorrect = if (correct) runOfCorrect + 1 else 0
        val dir = when {
            !correct -> -1                             // fallo → siempre baja (1-arriba)
            runOfCorrect >= rule.downAfter -> +1        // N aciertos → sube; 2 → 70.7%, 3 → 79.4%
            else -> 0
        }
        if (dir != 0) {
            if (lastDirection != null && dir != lastDirection) reversals += level
            lastDirection = dir
            if (dir > 0) runOfCorrect = 0
        }
        return (level + dir * step).coerceIn(0.0, 1.0)
    }

    /** Umbral = media de reversiones EXCLUYENDO el burn-in. Null si no convergió: nunca un número inventado. */
    fun threshold(): Double? =
        reversals.drop(BURN_IN_REVERSALS)
            .takeIf { it.size >= MIN_REVERSALS }        // ≥6 tras descartar las 2-4 primeras
            ?.average()
    // targetP se lee de la regla, no se configura: rule.targetP = 1-up/1-down→0.50, 1-up/2-down→0.707
}
```

---

## 7.5 Events to track

Esquema plano, sin contenido de trial, sin texto libre, ML-ready (una fila por decisión ⇒ el dataset de entrenamiento de la Fase 2/3 sale gratis).

```
adaptive_decision            // UNA por bloque. La fila que hoy no existe y sin la cual nada es auditable.
  exercise_id, target_family, target_p, mode
  decision, hold_reason?, axis_moved?, axis_is_temporal, step_size, from_level, to_level
  governing_signal_value?, governing_signal_n, governing_signal_confident
  blocks_since_change, n_trials
  gates_evaluated[]          // qué reglas se evaluaron y cuáles devolvieron null
  inputs_missing[]           // ← CRÍTICO: qué señales llegaron null. Hoy detectaría H1/H3 en 1 día.
  engine_version, config_version

challenge_zone_sample        // para verificar convergencia a la consigna
  exercise_id, target_p, observed_p, n, distance_to_target, blocks_at_level

step_up_vetoed               // por qué NO se subió (la señal más informativa para tuning)
  exercise_id, veto_reason   // ENGAGEMENT_RT | ENGAGEMENT_CV | OMISSIONS | POST_ERROR | FATIGUE | SAMPLE

safety_action_fired
  exercise_id, action        // SAFETY_STOP | COOLDOWN | CHECK_IN | REDUCE_INTENSITY | ABANDONMENT_GUARD
  trigger                    // qué señal lo disparó
  self_report_present         // bool, NUNCA el valor 1..5 fuera del almacén cifrado local

session_abandoned            // NO EXISTE HOY. Es la etiqueta de la Fase 2 de ML.
  exercise_id, block_index, elapsed_ms, last_decision, trials_completed, trials_planned

plateau_evaluated            // hoy imposible: PlateauDetector nunca se invoca
  exercise_id, status        // NONE | PLATEAU | FALSE_IMPROVEMENT
  days_observed, trained_delta, holdout_delta, holdout_available  // holdout_available=false ⇒ el status NO es concluyente
  intervention_protocol?, golden_rule_triggered

overtraining_evaluated
  verdict                    // OK | CAUTION | FORCE_REST | UNKNOWN(sin baseline)
  trigger                    // CHRONIC_FATIGUE | CV_AND_ACCURACY | ACCURACY_TREND
```

`inputs_missing[]` es el evento de mayor valor inmediato: los hallazgos H1 y H3 (cuatro reglas muertas en Focus, cuatro en Memory) habrían aparecido en el primer día de telemetría como `fatigue`, `per_axis_mastery`, `blocks_since_change` y `confidence_bias` ausentes en el 100% de las decisiones.

---

## 7.6 Adaptation metrics

Métricas de **la personalización**, no del usuario. Sin ellas no se puede afirmar que el motor adapta.

| Métrica | Definición | Objetivo | Qué detecta cuando falla |
|---|---|---|---|
| **Convergencia a consigna** | `mediana(\|p_observada − targetP\|)` sobre los últimos 5 bloques | < 0.05 | El motor no converge (Speed: no puede — no tiene consigna) |
| **Tiempo a la zona** | Bloques hasta entrar en banda desde el arranque | ≤ 3 | Nivel inicial mal elegido; pasos demasiado pequeños |
| **Tasa de oscilación** | Fracción de decisiones que invierten la anterior | < 0.15 | Histéresis rota o muestra insuficiente. **`DifficultyAdapter.kt` daría ~1.0**: bang-bang ±1 sin banda muerta |
| **Cobertura de reglas** | Fracción de reglas con ≥1 disparo en 30 días | 1.0 | **Hoy ~0.5 en Focus y ~0.4 en Memory** (H1/H3). El detector de código muerto adaptativo |
| **Ratio de vetos** | `step_up_vetoed / (step_up + step_up_vetoed)` | 0.10–0.35 | ≈0 → la puerta de compromiso no muerde; ≈1 → nadie progresa |
| **Deriva de eje temporal** | Fracción de STEP_UP que movieron un eje temporal | < 0.30 fuera de Speed | Dificultad-como-velocidad. **Speed = 1.00 hoy** |
| **Δ holdout / Δ entrenado** | Ganancia en ítems nunca vistos ÷ ganancia entrenada | > 0.4 | < 0.2 ⇒ memorización de banco, no aprendizaje. **Hoy incomputable salvo en Emotions** |
| **Gap calibración** | `mediana(confianza_declarada − exactitud)` | \|·\| < 0.15 | Sobreconfianza. Dispara metacognición, **nunca dificultad** |
| **Supervivencia por bloque** | `P(completar bloque n+1 \| completó n)` | > 0.90 | Frustración/fatiga mal gestionadas |
| **Recuperación tras fallo** | Exactitud en n+1 tras error − exactitud base | ≥ −0.10 | Perseveración de error; el bloque va demasiado difícil |
| **Estabilidad de arquetipo** | Cambios de arquetipo por 10 sesiones | < 1 | La inferencia es ruido. `DefaultProfileEvolution` ya lo mitiga (histéresis + un rango por vez) |

---

## 7.7 Edge cases

Cada uno con su comportamiento obligatorio. Los marcados **[HOY MAL]** están rotos en el repo.

1. **Bloque sin evidencia** (`accuracy == null`) → `Hold(SIGNAL_UNAVAILABLE)`, jamás un paso. *Bien*: `AdaptiveEngine.kt:96-101`. **[HOY MAL]** en Memory: `rawValueOf(key, 0.0)` (`MemoryAdaptiveEngine.kt:120`) convierte ausencia en 0.0 y `MemoryPerformance.of` usa `?: 0.0` (l.137) → un bloque no medido cae en FRUSTRATION y **baja la dificultad de un usuario que no falló nada**.
2. **Muestra por debajo del mínimo** → puede bajar (fail-closed), no puede subir. **[HOY MAL]** Speed sube con n=12 y puertas `cv`/`impIdx` sobre MAD de ≥4 valores.
3. **Precisión perfecta con n diminuto** (3/3) → no es dominio. Con `targetP=0.80`, `n=3` da un IC del 95% que va de 0.29 a 1.0. `nMin` debe derivarse del EE, no ser una constante por módulo.
4. **Techo** (100% en el nivel máximo) → `SwitchAxis` o rotación de contenido; nunca "subir" un eje ya en 1.0 y reportar progreso. **[HOY MAL]** `SwitchAxis` es inalcanzable en Focus (H1).
5. **Suelo** (0% en el mínimo) → `StopSession` + explicación, no bucle infinito de step-down.
6. **Zona inalcanzable por diseño**: la consigna cae fuera del rango representable por los ejes (p.ej. `TOO_EASY` de Memory con `speedNorm=0.5`, H2) → el motor debe **detectarlo y declararlo**, no fingir que adapta.
7. **Métrica presente pero de otro constructo** → la regla devuelve `NOT_APPLICABLE_TO_FAMILY`. `FocusAdaptiveRulePolicy` es el modelo. Sin esto, `ContainImpulsivity` **subía** la dificultad a un usuario de flanker que simplemente erraba rápido (deuda declarada en `AdaptiveEngine.kt:48-51`).
8. **Umbral absoluto sobre bloques de longitud variable**: `falseAlarms >= 2.0` (`AdaptiveEngine.kt:167`) es un **conteo**, no una tasa → sesgo por longitud de bloque. Toda puerta debe ser tasa o estar normalizada por n.
9. **Sesión de un solo trial**: Focus y Memory finalizan con `completedTrials = 1` — la decisión "por bloque" se toma sobre **un** trial. Con `nMin` real, esas sesiones devuelven `Hold(SIGNAL_UNAVAILABLE)` y **la dificultad deja de moverse**, que es el resultado honesto. Exige rediseñar la sesión, no relajar el gate.
10. **Primera sesión sin línea base**: `baselineRtCv == null` → `OvertrainingGuard` devuelve `Ok` en silencio (`OvertrainingGuard.kt:50`). Debe devolver `UNKNOWN`. Y sin baseline no hay `rtCvSpikeFactor` válido: el fallback `0.25` (`AdaptiveConfig:94`) es una constante poblacional aplicada como si fuera personal.
11. **Autoinforme contradictorio** (autoinforme calmado + proxies de sobrecarga disparados) → gana el proxy conductual para BAJAR, gana el autoinforme para NO SUBIR. Emotions ya lo hace (`canStepUp` exige `reportCalm` **y** `!escalationLocked`).
12. **Reanudación tras interrupción**: `blocksSinceChange` debe persistir. Con `Int.MAX_VALUE` hardcodeado (H1/H3) la histéresis nunca existió, así que hoy ni siquiera es un caso límite: es el caso normal.
13. **Modo evaluación/calibración**: dificultad congelada. `AdaptiveModeGate` existe y **no se usa** — la invariante debe moverse DENTRO del kernel (regla 0) para que sea imposible saltársela desde un call-site.
14. **Cambio de banco/versión de contenido** a mitad de progresión: los niveles no son comparables entre versiones. La decisión debe marcarse `NON_COMPARABLE` y el `masteryLevel` reiniciarse, no arrastrarse.
15. **Reversiones insuficientes en staircase** → `threshold() == null`. Nunca devolver el último nivel como si fuera el umbral (el defecto que la auditoría de Focus señala en `meanSsd` con el burn-in incluido, `focus.md:140`).

---

## 7.8 Privacy risks

1. **`SubjectiveState` (sueño, cafeína, estado de ánimo, hora)** — `focus/domain/model/Results.kt:60`, marcado *health-adjacent* por el propio código (`FocusSessionCloudDto.kt:28`). **Se captura, se persiste y se sincroniza, y NO alimenta ninguna decisión adaptativa** (no existe campo en `AdaptiveInput`). Recolección sin propósito. Dos salidas honestas: (a) cablearla como entrada de las reglas 1/4 y declararla en el consentimiento, o (b) **dejar de recolectarla**. La tercera —seguir guardándola por si acaso— es la que hay hoy y es la peor.
2. **Autoinforme de intensidad 1..5 (Emotions)** — es una autoevaluación de estado afectivo. Debe permanecer en el almacén cifrado por-usuario (`SecureKeyValueStore`), **nunca** en un evento analítico con su valor, y nunca en un sink remoto. El evento `safety_action_fired` debe llevar `self_report_present: Boolean`, no el valor.
3. **Series de fatiga, frustración consecutiva y abandono** — la traza temporal de "cuándo se rinde este usuario" es un perfil conductual inferido. Riesgo dual: privacidad y **manipulación** (un motor optimizado a retención aprendería a mantener al usuario justo por debajo del punto de abandono, que es engagement, no entrenamiento). Mitigación estructural: la función objetivo del motor es la **convergencia a consigna**, nunca el tiempo en sesión; y el modelo de abandono de la Fase 2 solo puede **bajar** carga, jamás subirla.
4. **`OvertrainingVerdict.ForceRest`** — sale de fatiga crónica + degradación. El texto **no puede** sugerir una condición de salud; el vocabulario permitido es "rendimiento frente a tu propia línea base", "reduce carga", "descanso". El copy-scanner (`mindboostCopySafetyScanner`) ya cubre el léxico prohibido; toda cadena nueva del motor entra en su alcance, **comentarios incluidos**.
5. **`errorTypeMix` y matriz de confusión** — especialmente en Emotions (confundir impulso con hecho) y Logic (errores conceptuales repetidos). Son inferencias sobre patrones de pensamiento. Se persisten como resúmenes agregados, **nunca trials crudos**, en el namespace por-cuenta, y su namespace debe estar en `MindBoostStorageNamespaces.AccountBound` **con su entrada en el eraser de `AppGraph` en el mismo cambio** (la regla ya documentada tras el hueco de `mindboost_profile`).
6. **Eventos ML-ready** — `adaptive_decision` es una fila por bloque y por usuario: un flujo de alta resolución. Regla: solo IDs de ejercicio y valores de decisión; **cero** contenido de ítem, cero texto libre, cero timestamps de precisión de reloj de pared (bloque relativo, no hora exacta), y sin identificador estable si el sink es remoto.
7. **Inferencia de arquetipo** (`UserArchetype`: IMPULSIVE, LOW_CONCENTRATION…) — son etiquetas de personalidad-adyacentes. `PersonalizedSessionScorer` ya acierta en no nombrarlas al usuario (`PersonalizedSessionScorer.kt:96-100`). La regla debe ser explícita en el kernel: **el arquetipo modula pesos internos; nunca se muestra, nunca se exporta, nunca se sincroniza**.

---

## 7.9 Qué reutilizar y qué arreglar — orden de ejecución

**Reutilizar tal cual (es el mejor código adaptativo del repo):**
- `DefaultEmotionAdaptiveEngine` — lista de reglas ordenada, seguridad primero, zonas por tipo de ejercicio, e `engagementSufficientForStepUp` (`:125-131`), que es la única defensa contra dificultad-como-velocidad escrita en este repo. **Es la espina del kernel.**
- `FocusAdaptiveRulePolicy` (`:35-36`) — gating de reglas por familia de paradigma. Generalizar a las 13 reglas.
- `StopSignalTracker` + `SsrtCalculator` (Focus E4) — el único staircase psicofísico correcto. Es el patrón de la familia B.
- `FatigueIndex` (`speed/domain/metrics/FatigueIndex.kt`) — tres señales estandarizadas sobre la propia serie, null con <3 bloques. Es la señal de fatiga a usar en todos los módulos.
- `PlateauDetector` + `InterventionPlanner` (Memory) — lógica correcta, solo hay que invocarla.
- `DefaultOvertrainingGuard` (Focus) — generalizar, arreglando el `UNKNOWN` sin baseline.
- `DefaultAdaptationEngine` (Language) — como capa de **selección de ítem** por encima del kernel de dificultad (son problemas distintos y hoy están confundidos). Sustituir `information` por información de Fisher cuando haya calibración.

**Arreglar, en este orden (coste creciente, valor decreciente):**

1. **Los call-sites, no los motores** (1 día). `FocusSessionStateHolder.kt:477-487` y `MemorySessionStateHolder.kt:264-270`: dejar de pasar `FatigueSignal.NONE`, `emptyMap()`, `Int.MAX_VALUE`, `0.0`. Donde el módulo no pueda producir la señal, pasar `null` y que el kernel emita `Hold(SIGNAL_UNAVAILABLE)`. **Resucita 8 reglas de seguridad de golpe.** Quitar los defaults de `MemoryAdaptiveInput` para que el compilador impida que vuelva a pasar.
2. **Separar exactitud de latencia en Speed** (1 día). `SpeedSessionStateHolder.kt:193`: emitir `judgementCorrect` y `withinDeadline` como campos distintos. Desbloquea el invariante `NO_TIME_FOR_ABILITY` y hace diagnosticable E3/E7.
3. **Arreglar `MemoryPerformance.of`** (medio día). Eliminar el doble conteo (`delayed ?: immediate`), sacar `speedNorm` del escalar de zona (ya existe la rama `WorkSpeed` para eso), y declarar la consigna en vez de dejar que emerja de las constantes. Añadir un test de convergencia con respondedor sintético.
4. **Meter `AdaptiveModeGate` DENTRO del kernel** como regla 0 (medio día). Una invariante que vive en un objeto que nadie llama no es una invariante.
5. **Emitir `adaptive_decision` con `inputs_missing[]`** (1 día). Sin este evento, cada uno de los seis hallazgos de arriba se descubre leyendo código; con él, se descubren en un dashboard.
6. **Cablear `DefaultAdaptationEngine` en Language** (2-3 días) y `PlateauDetector` en Memory/Emotions (1 día). El motor de meseta **necesita `holdoutDelta`**: sin holdout el estado devuelto es no concluyente y debe marcarse como tal.
7. **Logic: decidir.** O se cablea `nextDifficulty` a un selector de ítems con banco generativo (el módulo hoy sirve **los mismos 6 ítems siempre**), o se borra el adapter y se declara Logic como no-adaptativo. Lo que no puede seguir es calcular una decisión, persistir un θ que nadie lee y mostrar el texto de la decisión como si algo hubiera cambiado. Y `transferConfirmed` con n=1 (`:96-97`) debe exigir ≥3 sondas antes de mover nada.
8. **Borrar `com/mindverse/mindboost/core/adaptive/DifficultyAdapter.kt`.** Cero call-sites, controlador bang-bang sin banda muerta ni histéresis. Su única aportación —`TARGET_ICD = 0.82`, la única consigna explícita del repo— se conserva como concepto en `ChallengeConfig.targetP` y el archivo se va.

**El test que hoy no existe y que define si esto funciona:** respondedor sintético con habilidad conocida `θ` → el motor debe converger a la consigna declarada en ≤5 bloques, con tasa de oscilación <0.15, y el umbral estimado debe recuperar `θ`. Ningún motor de los siete tiene ese test, porque **ninguno tiene consigna que recuperar**. Esa es, en una frase, la diferencia entre los siete motores actuales y un motor adaptativo.