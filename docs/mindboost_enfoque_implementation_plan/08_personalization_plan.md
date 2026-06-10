# 08 · Plan de Personalización

> **Fase 8.** Inferir el arquetipo del usuario desde sus métricas, ajustar pesos
> de selección, plantillas y dificultad, y permitir que el perfil **evolucione**.
> Con seguridad anti-sobreentrenamiento. Kotlin puro en `domain/profile/`.

## 0. Arquetipos

```kotlin
enum class UserArchetype { BEGINNER, LOW_CONCENTRATION, IMPULSIVE,
    SLOW_BUT_ACCURATE, FAST_BUT_INACCURATE, ADVANCED, ELITE }
```

| Arquetipo | Foco | Ejercicios | Sesión | Ajustes clave |
|-----------|------|-----------|--------|---------------|
| Principiante | vigilancia/selectiva | E1, E2 | 5–10 min | cambios suaves, paso pequeño, refuerzo |
| Baja concentración | sostenida/re-enganche | E1, E12 | bloques cortos | microdescansos, penalizar lapsos, `timeOnTask` gradual |
| Impulsivo | inhibición | E3, E4 | 10 min | penalizar comisiones, micro-pausa pre-respuesta, NO premiar RT corto |
| Lento pero preciso | presión/flexibilidad | E13, E6 | 10–20 min | deadlines graduales, eficiencia sin sacrificar precisión |
| Rápido pero impreciso | presión/interferencia | E13, E5 | 10–20 min | penalización fuerte por error, precisión ponderada |
| Avanzado | integración/doble tarea | E6, E8, E9 | 20 min | mezcla de ejes, mayor volumen |
| Élite | integración bajo presión | E11, E13, F | 20–30 min | intrusos personalizados, presión/ranking, **vigilancia de sobreentrenamiento** |

## 1. Inferencia del arquetipo desde métricas

```kotlin
interface ArchetypeInference {
    fun infer(baseline: BaselineSnapshot, recent: List<SessionResult>): UserArchetype
}
```

Heurística (umbrales relativos a percentiles propios + poblacionales):

```
IMPULSIVE        ← falseAlarms altas + RT mediano bajo + comisiones con RT<250ms
FAST_BUT_INACCURATE ← RT bajo + accuracy baja (no solo comisiones de inhibición)
SLOW_BUT_ACCURATE   ← RT alto + accuracy alta + pressureDelta grande
LOW_CONCENTRATION   ← RT-CV alto + lapses altos + vigilanceDecrement pronunciado
BEGINNER         ← varianza alta en todo + sin baseline estable (primeras sesiones)
ADVANCED         ← accuracy alta + RT-CV bajo + maneja varios ejes
ELITE            ← ADVANCED + buen rendimiento bajo presión + dual-task cost bajo
```

**Regla:** el arquetipo es **probabilístico y revisable**, no una etiqueta fija.
Guardar también la confianza de la inferencia.

```kotlin
data class ArchetypeInferenceResult(
    val archetype: UserArchetype,
    val confidence: Double,        // baja al principio; sube con datos
    val secondGuess: UserArchetype?, // perfil mixto frecuente
)
```

## 2. Cómo se aplica la personalización

El arquetipo modula tres palancas, vía pesos (no ramas hardcodeadas por todos
lados):

```kotlin
data class PersonalizationProfile(
    val archetype: UserArchetype,
    /** Peso de selección por ejercicio (sesgo hacia debilidades del usuario). */
    val exerciseWeights: Map<ExerciseId, Double>,
    /** Sesgo de dificultad inicial por eje (p. ej. impulsivo empieza con inhibitoryDemand+). */
    val axisBias: Map<DifficultyAxis, Double>,
    /** Modificadores del scoring (anti-velocidad para impulsivos, etc.). */
    val scoringModifiers: ScoringModifiers,
    /** Plantilla de sesión preferida/recomendada. */
    val preferredTemplate: SessionTemplate,
)

data class ScoringModifiers(
    val speedIncentive: Double = 1.0,     // <1.0 para impulsivos (no premiar prisa)
    val errorPenalty: Double = 1.0,       // >1.0 para rápido-impreciso
    val lapsePenalty: Double = 1.0,       // >1.0 para baja concentración
    val prePressureMicroPause: Boolean = false, // gate de input para impulsivos
)
```

**Selección de ejercicios = debilidad primero.** El generador diario (doc 07)
multiplica el peso base de la semana por `exerciseWeights`, sesgando hacia la
capacidad más floja del usuario, sin abandonar el resto (variedad mínima).

## 3. Evolución del perfil en el tiempo

```kotlin
interface ProfileEvolution {
    /** Recalcula arquetipo, umbrales y pesos tras N sesiones / cada retest. */
    fun evolve(current: UserCognitiveProfile, newData: List<SessionResult>,
               retest: EvaluationResult?): UserCognitiveProfile
}
```
- Re-inferir arquetipo en cada retest completo (semana 4, 8) y tras rachas
  consistentes.
- Transición suave: no saltar de BEGINNER a ELITE; pasar por estados intermedios
  (evita cambios bruscos de experiencia).
- Histéresis de arquetipo: requerir evidencia sostenida para cambiar (igual
  filosofía que la anti-oscilación de dificultad, doc 04).

## 4. Seguridad anti-sobreentrenamiento (especialmente Élite)

```kotlin
interface OvertrainingGuard {
    fun check(profile: UserCognitiveProfile, recent: List<SessionResult>): OvertrainingVerdict
}
sealed interface OvertrainingVerdict {
    data object Ok : OvertrainingVerdict
    data class Caution(val reason: String) : OvertrainingVerdict     // reducir volumen
    data class ForceRest(val reason: String) : OvertrainingVerdict   // bloquear sesión hoy
}
```
Señales de sobreentrenamiento / degradación:
- Tendencia descendente sostenida en métricas pese a más volumen.
- RT-CV basal subiendo varios días (no recupera entre sesiones).
- Fatiga subjetiva alta repetida + sueño bajo.
- Élite empujando presión/ranking sin recuperación.

Respuesta: forzar deload no programado, recomendar descanso, **nunca** premiar
"streaks" que incentiven entrenar fatigado (anti-patrón de gamificación, doc 14).

## 5. Privacidad del perfil

El `UserCognitiveProfile` y el arquetipo son **datos sensibles** (pueden inferir
rasgos de salud/neurodivergencia). Reglas (detalle doc 10/14):
- Local-first; el arquetipo **nunca** se muestra como diagnóstico clínico.
- No compartir con terceros, no usar para ranking público sin consentimiento.
- Lenguaje no estigmatizante en UI ("hoy tu atención está más dispersa", no
  "tienes déficit de atención").

## 6. Criterios de aceptación (Fase 8)

- [ ] `ArchetypeInference` testeada con perfiles sintéticos (doc 11 simuladores).
- [ ] Personalización vía pesos/modificadores, no ramas dispersas.
- [ ] Impulsivo: `speedIncentive<1`, `prePressureMicroPause`, no premia RT corto.
- [ ] Perfil evoluciona con histéresis (sin saltos bruscos), confianza registrada.
- [ ] `OvertrainingGuard` puede forzar descanso; tests de degradación sostenida.
- [ ] Lenguaje de UI no diagnóstico (revisión en doc 09).
