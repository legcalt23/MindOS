# 11 · Plan de Testing y Validación

> Tests con `kotlin-test` + `kotlinx-coroutines-test` (`runTest`), patrón de fakes
> y `cancelChildren()` del repo. Cobertura objetivo ≥80% en dominio/métricas/
> adaptación (lógica pura — la más crítica y la más fácil de testear bien).

## 0. Pirámide para este sistema

```
            ┌─────────────────────────┐
            │ Validación manual (device)│  timing RT real, sensación
            ├─────────────────────────┤
            │ Simuladores de usuario   │  end-to-end del lazo (puro)
            ├─────────────────────────┤
            │ Integración (holders+repo)│  flujos de sesión con fakes
            ├─────────────────────────┤
            │ Unit (dominio/métricas/  │  ← el grueso; vectores conocidos
            │ adaptación/ejercicios)   │
            └─────────────────────────┘
```

## 1. Unit — Métricas (doc 03)

- **Vectores conocidos:** datasets sintéticos con el resultado calculado a mano
  (accuracy, RT mediano, RT-CV, lapsos, FA, omisiones, PES).
- **SSRT:** replicar el ejemplo del paper de Verbruggen (2019) y exigir igualdad.
- **IES/LISAS/RCS:** casos con números redondos verificables.
- **MAD/outliers:** inyectar RT extremos → comprobar exclusión y `n` resultante.
- **Property-based** (manual, sin lib): accuracy ∈ [0,1]; RT-CV ≥ 0; mediana ≤
  max; `confident=false` cuando n bajo.
- **Edge cases tabla doc 03 §8:** 0 hits, accuracy 0, MAD 0, reliability ausente.

```kotlin
class MetricsEngineTest {
    @Test fun rtMedian_ignoresOutliers_viaMad() = runTest {
        val trials = buildTrials(rts = listOf(300,310,305,295,9000)) // 9000 = outlier
        val m = engine.blockMetrics(trials, e1Def, defaults)
        assertEquals(304.0, m.value(RT_MEDIAN), absoluteTolerance = 5.0)
    }
}
```

## 2. Unit — Motor adaptativo (doc 04)

Una prueba por rama de la tabla de decisión:
- accuracy ≥ 0.85 estable → `StepUp(governingAxis)`.
- accuracy < 0.65 → `StepDown`.
- 3 errores consecutivos → `StepDown` aunque accuracy media sea decente.
- RT-CV > 1.3×baseline → `StepDown`.
- comisiones rápidas → `ContainImpulsivity` (NO `StepUp`).
- eje saturado + eje débil → `SwitchAxis`.
- fatiga sostenida → `StopSession`.
- **Anti-oscilación:** secuencia alternante alta/baja → la histéresis evita
  ping-pong (assert: no más de 1 cambio en K bloques tras un cambio).
- **Solo TRAINING:** invocar en EVALUATION debe ser imposible/no-op (test que
  falla si adapta).

## 3. Unit — Ejercicios (doc 05)

- **Determinismo:** `generate(d, spec, seed)` con misma seed → secuencia idéntica.
- **Mapping:** `DifficultyMapping` solo usa `relevantAxes`; cambiar un eje
  irrelevante no altera la salida.
- **Clasificación de outcomes:** dado (stimulus, response, ventana) → outcome
  correcto (HIT/MISS/FA/CORRECT_REJECTION/ANTICIPATION/TIMEOUT).
- **SSD adaptativo E4:** tracking 1-up/1-down converge a p(stop)≈0.5.

## 4. Tests de timing (críticos, riesgo A1)

- `TrialClock` fake monótono inyectable: avanzar nanos manualmente y verificar
  que RT = response − onset exacto.
- Verificar que **nunca** se usa reloj de pared para RT (test de arquitectura:
  grep/asserts de que `ExerciseRuntime` no llama a `EpochClock` para RT).
- En device (manual): medir RT contra un estímulo conocido y caracterizar la
  latencia de display; documentar el offset.

```kotlin
class TimingTest {
    @Test fun rt_isMonotonicDelta() = runTest {
        val clock = FakeMonotonicClock()
        clock.advanceNanos(0); val onset = clock.markNow()
        clock.advanceNanos(420_000_000); val resp = clock.markNow()
        assertEquals(420L, (resp - onset) / 1_000_000)
    }
}
```

## 5. Integración — StateHolders + repos fake

Patrón del repo (fakes, `scope = this`, `cancelChildren()` al final):

```kotlin
class SessionRunnerStateHolderTest {
    @Test fun fullSession_appendsSummary_andAdaptsInTraining() = runTest {
        val history = InMemorySessionHistoryRepository()
        val holder = SessionRunnerStateHolder(
            runtime = FakeExerciseRuntime(scriptedOutcomes),
            metrics = realMetricsEngine, adaptive = realAdaptiveEngine,
            historyRepo = history, clock = FixedEpochClock(), scope = this)
        holder.startSession(SessionTemplate.STANDARD_10)
        advanceUntilIdle()
        assertTrue(history.all().isNotEmpty())
        coroutineContext.cancelChildren()   // patrón obligatorio del repo
    }
}
```

## 6. Simuladores de usuario (validación end-to-end del lazo)

`UserSimulator` = modelo que genera respuestas con un perfil de comportamiento;
se corre contra el lazo completo (generator→runtime→metrics→adaptive) y se
verifica que el sistema reacciona correctamente.

```kotlin
interface UserSimulator { fun respond(s: Stimulus, difficulty: DifficultyVector): SimResponse }

object Simulators {
    val IMPULSIVE      // RT corto, muchas comisiones en no-go → debe disparar ContainImpulsivity
    val SLOW_ACCURATE  // RT alto, accuracy alta → debe poder subir presión sin perder precisión
    val FATIGUED       // RT-CV creciente, lapsos al final → debe disparar Cooldown/StopSession
    val ADVANCED       // accuracy alta estable → debe subir dificultad / cambiar eje
    val GAMER_E4       // ralentiza a propósito en stop-signal → SSRT confident=false (gaming)
}
```

Asserts esperados por simulador:
- IMPULSIVE → el sistema sube `inhibitoryDemand`, baja incentivo de velocidad,
  NO sube dificultad por "velocidad".
- FATIGUED → aparece `Cooldown` y, si persiste, `StopSession`.
- ADVANCED → progresión sostenida o `SwitchAxis` al saturar.
- GAMER_E4 → métrica SSRT marcada no fiable + flag.
- SLOW_ACCURATE → progresión por eje `pressure` con accuracy preservada.

## 7. Tests de regresión

- Snapshot de métricas para datasets fijos (si cambia el cálculo, el test avisa).
- `DailyPlanGenerator`: misma entrada → mismo plan (determinista con seed).
- Banco de probes disjunto de training (intersección vacía) — regresión de
  contaminación.
- Plantillas de sesión suman su duración exacta.

## 8. Validación científica (revisión, no automatizable)

- Revisar que cada fórmula cita su fuente (PVT, SSRT-integration, flanker…).
- Revisar que ninguna afirmación de UI promete transferencia lejana.
- Revisar que probes nunca se entrenan.
- (Futuro) estudio interno N pequeño: ¿mejoran los probes? RCI real.

## 9. Cobertura y CI

- Kover (si se integra) o conteo manual: ≥80% en `domain/` (puro).
- Tests corren en `desktopTest`/`commonTest` (JVM) — rápido, sin device.
- Respetar el ratchet de Detekt: código nuevo limpio.

## 10. Criterios de aceptación (testing)

- [ ] Métricas: vectores conocidos + SSRT del paper + edge cases.
- [ ] Adaptación: una prueba por rama + anti-oscilación + solo-TRAINING.
- [ ] Ejercicios: determinismo + clasificación de outcomes.
- [ ] Timing: RT = delta monótono; nunca reloj de pared.
- [ ] Simuladores: los 5 perfiles con asserts de comportamiento del sistema.
- [ ] Regresión: probes disjuntos, planes deterministas, plantillas exactas.
- [ ] ≥80% en dominio puro.
