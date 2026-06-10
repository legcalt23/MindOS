# 07 · Plan de Sesiones y Periodización

> **Fase 7.** Estructura temporal del entrenamiento: plantillas de sesión
> (5/10/20/30 min), ciclo de 8 semanas con deload, y `DailyFocusPlan` que
> selecciona qué entrenar cada día. Kotlin puro en `domain/session/`.

## 0. Estructura de una sesión

```kotlin
enum class SessionTemplate { MAINTENANCE_5, STANDARD_10, DEVELOPMENT_20, ELITE_30 }

/** Una fase dentro de la sesión. Las duraciones suman el total de la plantilla. */
enum class SessionPhase { WARMUP, CORE, PEAK, TRANSFER, COOLDOWN, REFLECTION }

data class SessionBlueprint(
    val template: SessionTemplate,
    val phases: List<PhaseSlot>,
)
data class PhaseSlot(
    val phase: SessionPhase,
    val durationMs: Long,
    val mode: Mode,                          // WARMUP/CORE/PEAK = TRAINING; probes = EVALUATION
    val exerciseSelection: ExerciseSelectionPolicy,
)
```

### Plantillas (duraciones del diseño funcional)

```kotlin
object SessionBlueprints {
    val MAINTENANCE_5 = blueprint(
        WARMUP to 30_000, CORE to 210_000, COOLDOWN to 30_000, REFLECTION to 30_000)
    val STANDARD_10 = blueprint(
        WARMUP to 60_000, CORE to 360_000, PEAK to 90_000, COOLDOWN_REFLECTION to 90_000)
    val DEVELOPMENT_20 = blueprint(
        WARMUP to 120_000, CORE to 600_000, PEAK to 180_000, TRANSFER to 180_000,
        COOLDOWN_REFLECTION to 120_000)
    val ELITE_30 = blueprint(
        WARMUP to 180_000, CORE to 840_000, PEAK to 240_000, TRANSFER to 300_000,
        COOLDOWN to 120_000, REFLECTION to 120_000)
}
```

| Plantilla | Calentamiento | Núcleo | Pico | Transferencia | Cooldown | Reflexión |
|-----------|--------------|--------|------|---------------|----------|-----------|
| 5 Mantenimiento | 30 s | 3.5 min | — | — | 30 s | breve |
| 10 Estándar | 1 min | 6 min | 1.5 min | — | 1.5 min (combinado) | |
| 20 Desarrollo | 2 min | 10 min | 3 min | 3 min | 2 min (combinado) | |
| 30 Élite | 3 min | 14 min | 4 min | 5 min | 2 min | 2 min |

**Semántica de fases:**
- **Warmup:** dificultad por debajo del nivel de trabajo; re-engancha sin frustrar.
- **Core:** trabajo adaptativo principal (TRAINING).
- **Peak:** breve empuje a dificultad alta del eje gobernante.
- **Transfer:** microprotocolos funcionales (doc 09/14) — etiquetados como
  transferencia **débil/subjetiva**, nunca como prueba de mejora.
- **Cooldown:** baja carga, cierre sin error en espiral.
- **Reflection:** cuestionario breve + feedback honesto (sin pseudociencia).

## 1. Ciclo de 8 semanas (periodización)

```kotlin
data class TrainingCycle(val weeks: List<WeekPlan> /* 8 */)
data class WeekPlan(
    val index: Int,                      // 1..8
    val focus: String,                   // descripción legible
    val emphasizedExercises: List<ExerciseId>,
    val volume: Volume,                  // LOW / MODERATE / HIGH
    val isDeload: Boolean,
    val evaluation: EvaluationRequirement,   // NONE / LIGHT / FULL
)
enum class Volume { LOW, MODERATE, HIGH }
enum class EvaluationRequirement { NONE, LIGHT, FULL }
```

| Sem | Foco | Ejercicios | Volumen | Evaluación |
|-----|------|-----------|---------|-----------|
| 1 | Base + diagnóstico | E1, E2, E3 | LOW | **FULL** (baseline) |
| 2 | Sostenida + selectiva | E1, E2, E7 | MODERATE | LIGHT |
| 3 | Inhibición + interferencia | E3, E4, E5 | MODERATE | LIGHT |
| 4 | **Descarga + evaluación** | (deload) | **LOW (deload)** | **FULL (retest)** |
| 5 | Flexibilidad + recuperación | E6, E12, E10 | MODERATE | LIGHT |
| 6 | Integración / doble tarea | E8, E9, E11 | HIGH | LIGHT |
| 7 | Pico bajo presión | E13 + módulo F | HIGH | LIGHT |
| 8 | **Descarga + evaluación final** | (deload) | **LOW (deload)** | **FULL (retest)** |

## 2. Deload (descarga)

```kotlin
fun applyDeload(base: DailyFocusPlan): DailyFocusPlan = base.copy(
    volumeMultiplier = 0.5,        // ~mitad de volumen
    capDifficulty = true,          // no subir dificultad esta semana
    prioritizeEvaluation = true,   // la semana de deload incluye retest completo
)
```
Propósito: consolidación, evitar sobreentrenamiento, ventana limpia para medir
(menos fatiga acumulada contamina el retest).

## 3. Reglas de recuperación (entre sesiones)

```kotlin
interface RecoveryPolicy {
    /** ¿Puede entrenar hoy a volumen normal? Lee historial reciente + fatiga. */
    fun gate(profile: UserCognitiveProfile, recent: List<SessionResult>): RecoveryGate
}
sealed interface RecoveryGate {
    data object FullGo : RecoveryGate
    data class Reduced(val volumeMultiplier: Double, val reason: String) : RecoveryGate
    data class RestRecommended(val reason: String) : RecoveryGate   // fatiga acumulada / mal estado
}
```
Señales: fatiga sostenida en sesiones recientes, RT-CV basal degradado varios
días, sueño bajo reportado. Anti-sobreentrenamiento (doc 08 §élite).

## 4. DailyFocusPlan (selección diaria)

```kotlin
data class DailyFocusPlan(
    val date: LocalDateContext,            // vía reloj inyectado, no Clock.System
    val template: SessionTemplate,         // según disponibilidad/preferencia del usuario
    val blocks: List<PlannedBlock>,
    val volumeMultiplier: Double = 1.0,
    val capDifficulty: Boolean = false,
    val includeProbe: Boolean,             // retest ligero del día
)
data class PlannedBlock(
    val exerciseId: ExerciseId,
    val mode: Mode,
    val startingDifficulty: DifficultyVector,   // del perfil calibrado
    val phase: SessionPhase,
)

interface DailyPlanGenerator {
    fun generate(
        profile: UserCognitiveProfile,
        cycle: TrainingCycle,
        availableMinutes: Int,             // el usuario elige 5/10/20/30
        recent: List<SessionResult>,
    ): DailyFocusPlan
}
```

### Algoritmo de selección (resumen)
1. Determinar `WeekPlan` actual (`profile.cycleWeek`).
2. Elegir plantilla por `availableMinutes`.
3. Sembrar el núcleo con `emphasizedExercises` de la semana, ponderados por
   debilidad del perfil (eje/capacidad más floja primero — doc 08).
4. Aplicar `RecoveryGate` (reducir volumen / recomendar descanso).
5. Si semana de deload → `applyDeload`.
6. Insertar probe ligero según cadencia (semanal) sin contaminar (doc 06).
7. Variedad: no repetir el mismo ejercicio dominante dos días seguidos (evita
   fatiga de tarea y aprendizaje ítem-específico).

## 5. Criterios de aceptación (Fase 7)

- [ ] Las 4 plantillas suman exactamente su duración objetivo (test).
- [ ] El ciclo de 8 semanas con deload en 4 y 8 está modelado.
- [ ] `DailyPlanGenerator` respeta semana, disponibilidad y RecoveryGate.
- [ ] Deload reduce volumen y capa dificultad.
- [ ] Probes insertados sin entrar en bancos de training.
- [ ] Fechas/tiempo vía reloj inyectado, nunca `Clock.System`.
