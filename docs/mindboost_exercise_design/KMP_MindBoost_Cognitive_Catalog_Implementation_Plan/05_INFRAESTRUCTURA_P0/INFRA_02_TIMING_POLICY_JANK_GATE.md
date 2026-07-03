# INFRA_02 — `TimingPolicy` con signo + medición real de jank + gate `UNTRUSTED_TIMEBASE`

> **Issue P0 listo para Jira/GitHub.** Fuentes: `INFRA_P0.md` (PARTE A), `AUDIT_V2_TECNICO.md`
> (INFRA-2), `AUDIT_V2_GROUND_TRUTH.md` (§timing). Confianza: `[hecho]` / `[inferido]` /
> `POR_CONFIRMAR_EN_REPO`.

---

## Título
INFRA-2 · `TimingPolicy` (RT con signo, sin lanzar) + `FrameTimingProbe` que mide `frameDropFraction`
real + gate `DEVICE_UNRELIABLE/UNTRUSTED_TIMEBASE` portado de Speed-Android a Focus y otros

## Prioridad
**P0** — sin esto, ni Enfoque ni Velocidad pueden actualizar perfil (invariante 2), y E5/E12-Velocidad
(error temporal con signo) no son implementables.

## Objetivo
1. RT con signo: `computeRtNanos` hoy **LANZA** con rt<0 `[hecho, INFRA-2/AUDIT]`; la cadencia
   anticipatoria (E5-Velocidad) y las anticipaciones (E12) requieren error temporal **con signo** sin
   excepción.
2. Medir jank/frame-drop **real** vía `frameTimeNanos`: hoy `SpeedSessionStateHolder.kt:259-260` pasa
   `frameDropFraction=0.0` y `criticalFrameJitter=false` **hardcodeados** `[hecho, GROUND_TRUTH §timing]`
   → el gate `device_unreliable` es **código muerto** (nunca dispara por causa física).
3. Portar el patrón timebase-autoritativa de Speed-Android a Focus (que mide con `MonotonicClock` a
   nivel de corrutina, no de frame `[hecho]`) y exponer `UNTRUSTED_TIMEBASE` que NO actualiza perfil.

## Verdad de partida (no inventar) `[hecho, GROUND_TRUTH §timing]`

| Capa | Estado real | Veredicto |
|---|---|---|
| Aritmética RT pura + prohibición `elapsedRealtimeNanos` | `TimebasePolicy.kt` IMPLEMENTED+TESTED | OK |
| Captura frame-accurate Android | `SpeedTimebase.android.kt` (Choreographer + MotionEvent) | OK |
| Modelo de riesgo de dispositivo | `deviceTimingRisk()` + `SessionQualityEvaluator` | OK (modelo) |
| **Medición real de jank que alimente el gate** | `SpeedSessionStateHolder.kt:259-260` → `0.0`/`false` HARDCODED | **ABSENT → gate decorativo** |
| iOS frame-accurate (CADisplayLink) | `SpeedTimebase.ios.kt:14` delega a fallback | **ABSENT** |
| Focus frame-accurate | `MonotonicClock` corrutina, no vsync | downgrade (honesto, no autoritativo) |

## Archivos probables
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimebasePolicy.kt` (EXISTE — extender con signo)
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/TimingPolicy.kt` (NUEVO — superset compartido)
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/FrameTimingProbe.kt` (NUEVO expect)
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/SpeedTimebase.android.kt` (EXISTE — fuente del actual a portar)
- `ruta probable` `.../core/timing/FrameTimingProbe.{android,ios,desktop}.kt` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/SpeedSessionStateHolder.kt` (EXISTE — reemplazar el `0.0/false` hardcoded)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/**` (consumir gate)

## Cambios técnicos

### 1. RT con signo (no lanza en negativos)

```kotlin
// core/timing/TimingPolicy.kt
data class SignedTiming(val deltaNanos: Long, val sign: Int)  // sign = -1 adelanto / 0 / +1 retraso

object TimingPolicy {
    /** A diferencia de computeRtNanos (que LANZA con rt<0), preserva el signo del adelanto. */
    fun signedRt(stimulusNanos: Long, responseNanos: Long): SignedTiming {
        val d = responseNanos - stimulusNanos
        return SignedTiming(deltaNanos = d, sign = d.compareTo(0L))
    }
}
```

### 2. `FrameTimingProbe` — mide jank real (reemplaza el `0.0/false`)

```kotlin
expect class FrameTimingProbe { fun start(); fun stop(): FrameStats; fun markOnsetFrame(scheduledNanos: Long, actualNanos: Long) }

data class FrameStats(
    val sampledFrames: Int,
    val frameDropFraction: Double,    // frames con duración > 1.5×refresh / total
    val maxFrameJitterNanos: Long,
    val criticalFrameJitter: Boolean, // jitter en el frame del ONSET del estímulo (el único que corrompe el RT)
)
```

**Android** (`actual`): `Choreographer.postFrameCallback` acumula `frameTimeNanos` consecutivos
(misma base que `SpeedTimebase.android.kt:41`), cuenta drops vs período nominal (`Display.refreshRate`),
y `markOnsetFrame` marca `criticalFrameJitter` solo si el frame del onset llegó tarde.

> **Decisión psicométrica:** `criticalFrameJitter` gatea solo por el frame del **onset** (el que muestra
> el estímulo). Un drop a mitad de bloque no corrompe un RT ya capturado; gatear por jank global daría
> falsos `DEVICE_UNRELIABLE` por un GC puntual.

**iOS** (`actual`): contrato `CADisplayLink` (`POR_CONFIRMAR_EN_REPO` — hoy ABSENT, delega a fallback
`[hecho]`). Hasta existir, iOS devuelve garantía no-autoritativa → jugable, sin profile-update.
**Desktop** (`actual`): downgrade explícito (sin vsync portable) → `FrameStats(0, 0.0, 0L, false)` que
el clasificador traduce a no-autoritativo, **nunca** a autoritativo.

### 3. Clasificador puro (donde vive el gate, testeable sin device)

```kotlin
sealed interface TimebaseGuarantee {
    data class Authoritative(val source: TimestampSource, val frame: FrameStats) : TimebaseGuarantee
    data class NonAuthoritative(val reason: DowngradeReason) : TimebaseGuarantee      // → UNTRUSTED_TIMEBASE
    data class DeviceUnreliable(val reason: UnreliableReason, val frame: FrameStats) : TimebaseGuarantee
}

object TimingPolicyClassifier {
    fun classify(source: TimestampSource, precision: InputTimestampPrecision, frame: FrameStats,
                 clockDiscontinuity: Boolean, t: MetricThresholds): TimebaseGuarantee = when {
        clockDiscontinuity -> TimebaseGuarantee.DeviceUnreliable(UnreliableReason.CLOCK_DISCONTINUITY, frame)
        source != TimestampSource.CHOREOGRAPHER_FRAME -> TimebaseGuarantee.NonAuthoritative(DowngradeReason.SYSTEM_CLOCK_MONOTONIC)
        precision == InputTimestampPrecision.MILLIS_DERIVED -> TimebaseGuarantee.NonAuthoritative(DowngradeReason.MILLIS_DERIVED)
        frame.criticalFrameJitter -> TimebaseGuarantee.DeviceUnreliable(UnreliableReason.CRITICAL_FRAME_JITTER, frame)
        frame.frameDropFraction > t.contaminatedFrameDropThreshold -> TimebaseGuarantee.DeviceUnreliable(UnreliableReason.EXCESSIVE_FRAME_DROP, frame)
        else -> TimebaseGuarantee.Authoritative(source, frame)
    }
}
```

### 4. Conexión con `ScoreResult` (INFRA_01)

El clasificador alimenta `DataQuality.deviceTimingQuality` + `frameDropFraction` +
`criticalFrameJitter`. La state machine ya convierte `NonAuthoritative`/`DeviceUnreliable` en
`ScoreState.DEVICE_UNRELIABLE` con `updatesProfile=false` (invariante 2). **Focus RT-crítico, mientras
mida con `MonotonicClock` de corrutina, nunca será `AUTHORITATIVE` → no-update por diseño** hasta tener
captura frame-accurate.

## Criterios de aceptación
- `TimingPolicy.signedRt` acepta RT negativos (adelanto) **sin excepción**; signo preservado.
- Cualquier métrica RT-crítica en Focus/Emociones/Decisiones sin timebase autoritativa retorna
  `DEVICE_UNRELIABLE`/`UNTRUSTED_TIMEBASE`, **nunca** un RT crudo que actualice perfil.
- `frameDropFraction` proviene de medición real (`Choreographer`/`frameTimeNanos`), NO de constante;
  expuesto al gate. El `0.0/false` de `SpeedSessionStateHolder.kt:259-260` queda eliminado.
- Desktop/iOS NUNCA producen `Authoritative`.

## Tests obligatorios
- **Regresión del bug actual:** RT negativo NO lanza (hoy `computeRtNanos` lanza `[hecho]`).
- `UNTRUSTED_TIMEBASE` cuando la timebase no es Android-autoritativa (fallback/desktop/iOS/Focus-corrutina).
- **Jank simulado > umbral ⇒ `DeviceUnreliable`** (hoy imposible por el hardcoded `0.0`).
- `criticalFrameJitter` solo dispara con jitter en el frame del **onset**, no en uno intermedio.
- Property test: clasificador determinista sobre `FrameStats` arbitrario.

## Riesgos
- `FrameTimingProbe` es expect/actual: cada `actual` debe existir en TODOS los targets declarados
  (android/desktop/ios) o el build iOS falla `[hecho, convención repo expect/actual]`.
- iOS CADisplayLink es trabajo nativo no presente; hasta entonces iOS = no-autoritativo (riesgo aceptado).
- No ejecutar tareas Gradle en paralelo sobre el mismo worktree `[hecho, convención repo]`.

## Definition of Done
- [ ] `TimingPolicy.signedRt` + `FrameTimingProbe` (expect + 3 actuals) creados.
- [ ] `SpeedSessionStateHolder` alimenta `FrameStats` real; eliminado el `0.0/false`.
- [ ] Focus consume el gate; RT-crítico sin frame-accurate → `DEVICE_UNRELIABLE`.
- [ ] Suite de clasificador + regresión RT-negativo + jank-simulado verde.
- [ ] `:shared:detekt` + `:shared:desktopTest` + `:androidApp:compileDebugKotlin` verdes.

## Comando/check de validación
```
:shared:compileKotlinDesktop · :shared:desktopTest · :androidApp:compileDebugKotlin · :shared:detekt
```
