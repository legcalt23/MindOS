# Orden 2 — mindboost-mobile-performance (2026-07-19) — output íntegro (gate S5)

> Integrado en: ADR-FOCUS-TIMING, plan §5 S3/S5, REDTEAM matrix (timing), portfolio (reglas RT).
> HALLAZGO NUEVO candidato a ledger (single-writer = sesión principal): **el probe Android hardcodea
> DEFAULT_REFRESH_HZ=60 (`FrameTimingProbe.android.kt:11`) ⇒ en pantallas 120 Hz/VRR los drops son
> matemáticamente indetectables (deltas ~8.3ms nunca superan 1.5×16.7ms) ⇒ jank-gate ciego y clase T inflada.**
> Arreglo barato: `fromFrameTimes(refreshHz=)` ya acepta el parámetro (`FrameStatsCalculator.kt:26`);
> falta leer el Hz real por segmento. Prerequisito de cualquier clase T>0 honesta (C.1).
> Caveat de lectura: el agente no encontró focus_pack/07 (ruta sin el subdir FOCUS_ENGINE_RESEARCH_PACK);
> el entregable es 100% repo-driven (VERIFIED) + INFERRED marcado — impacto bajo (07 es UX/a11y).

## Estado del arte repo (VERIFIED)
`TimingPolicyClassifier` fail-closed en cadena (`speed/domain/TimebaseClassifier.kt:37-50`); probe Android =
Choreographer real con 60 Hz hardcodeado; iOS = NoOp stub; umbrales: drop >1.5× período, crítico >2.5×,
contaminación >5% (`FrameStatsCalculator.kt:18,21`; `MetricThresholds.kt:31`); NANOS_NATIVE exige API≥34
(`TimebasePolicy.kt:25`).

## A) Modelo de error por capa (precisión ≠ exactitud)
El stack de software solo acota PRECISIÓN; la EXACTITUD absoluta (latencia física display+touch) exige banco
con fotodiodo/actuador — inalcanzable a 0€. Android: recomposición hasta commit 0-1 frame (sesgo variable —
motiva el render marker); pipeline de frames 1-2 frames (~16-33ms, sesgo casi constante intra-sesión); panel
~5-20ms; digitalizador 60-240 Hz ⇒ 4-17ms cuantización; `MotionEvent.eventTime` se sella en el driver (el
jitter de entrega no contamina si se usa eventTime); resolución input: <API34 = 1ms, API34+ nanos; GC/JIT
1-10ms esporádico (⇒ warm-up + cero allocations en hot path); thermal = deriva sistemática en bloques 8-12min
(confound CIENTÍFICO, no UX); VRR/LTPO cambia el período nominal mid-sesión (baja a 10-24 Hz en estático —
riesgo específico del Tracer B con ISI largos sin animación). iOS: sin CADisplayLink cableado ⇒ sin ancla de
frame; UITouch.timestamp sub-ms; ProMotion = mismo problema VRR. **Sesgo total ~30-70ms, mayormente constante
intra-dispositivo ⇒ comparación intra-sujeto mismo-dispositivo defendible (T1); cross-device NO sin banco (T2).**
(Todo INFERRED salvo citas repo.)

## B) Estrategia por banda
| Banda | Clase T máx | Notas |
|---|---|---|
| Android 26-28 | T1 (frame-locked + input 1ms) | Choreographer existe desde API16; solo exige bajar minSdk (CEO) + probar Compose MP en emulador API26; peor banda de presupuestos |
| Android 29-33 | T1 fuerte | eventTime ms (MILLIS_DERIVED, `TimebasePolicy.kt:32-35`); cuantización 1ms despreciable vs sesgo 30-70ms; fail-closed la mantiene fuera de oficial igual |
| Android 34+ | T1+ (candidato a T2 SOLO con banco) | CHOREOGRAPHER_FRAME+NANOS_NATIVE (precedente Speed); ningún API level da T2 por sí solo |
| iOS 15+ HOY | T0-para-RT (monotónico sin frame-lock) | accuracy OK; RT ni descriptivo-comparable |
| iOS + port CADisplayLink | T1 | actual iosMain alimentando FrameStatsCalculator (patrón Android); est. 1-2 días; INVERIFICABLE en este host (Windows) — DESIGNED_NOT_RUN hasta iPhone físico |

## C) Protocolo 0€ (<10 min/dispositivo) — todos los umbrales DESIGNED_NOT_RUN, revisión tras ~20 sesiones/dispositivo
**C.1 (BLOQUEANTE): detección de Hz real** por segmento (Android `display.refreshRate`/`Display.getMode()`;
iOS `UIScreen.maximumFramesPerSecond`) pasado a `fromFrameTimes(refreshHz=)`; en VRR re-derivar por ventana o
clasificar el segmento como no-frame-locked.
1. Frame pacing en reposo (60s): GO = p99 delta <1.5× período y drops <2%.
2. Jank bajo carga (120s de Tracer A): GO = drops ≤5% y sin critical jitter en >1 segmento.
3. **Render marker**: timestamp del estímulo = frameTimeNanos del primer frame callback POSTERIOR al set de
   estado; RT = eventTime − frameCommit (elimina 0-1 frame de sesgo variable = la diferencia práctica T0→T1);
   verificable sin banco: p95 de (stateChange→frameCommit) < 1 período.
4. Tap latency descriptivo (30 taps frame-locked): firma estable del dispositivo para detectar deriva (>20ms
   ⇒ sospechar cambio de entorno). NO es exactitud absoluta.
5. Métricas por segmento: hzNominal, hzDetectedWindow[], sampledFrames, frameDropFraction, maxFrameJitter,
   criticalFrameJitter, clockDiscontinuity, inputPrecision, source, thermalStatus (API29+), batterySaverOn.
6. GO/NO-GO por clase: T1-frame-locked = fuente frame + Hz estable + drops ≤5% + sin critical + sin
   discontinuidad · T1-degradado (drops 5-15%) = RT solo práctica, accuracy intacta · T0 = sin frame-lock o
   critical o discontinuidad ⇒ cero RT, accuracy sí.
7. **Invalidación PROPORCIONAL por SEGMENTO** (~20 trials A / ventana 60-90s B), nunca cap binario de sesión:
   segmento NO-GO excluye SOLO su RT; accuracy se conserva salvo discontinuidad de reloj; la sesión cae solo
   si >50% de segmentos NO-GO o discontinuidad global. Thermal ≥MODERATE o deriva de mediana >10% primer→último
   segmento ⇒ segmentos tardíos thermal-confounded (RT excluido, accuracy conservada, confound citado —
   lentitud de hardware indistinguible de fatiga cognitiva: NO atribuir).
**Sin banco físico NO se mide**: latencia absoluta display+touch ⇒ ningún RT comparable cross-device ni
interpretable en ms absolutos; claim máximo = "RT relativo a tu propia línea base en este dispositivo y clase".

## D) Presupuestos (DESIGNED_NOT_RUN; herramientas gratis)
Frame budget: <10ms trabajo/frame @60, <6ms @120 (Perfetto/Macrobenchmark FrameTimingMetric; gate p95 ≤
período, jank <5%). Allocations hot path: 0 nuevas por frame en ventana de respuesta; pre-dimensionar
ArrayList(2000) del probe en bloques largos; gate = cero GC pauses >2ms en captura Perfetto. Arranque de
sesión: <1.5s gama baja; pre-generar TODO el bloque antes del primer trial (los generadores puros lo permiten).
Memoria: delta heap <50MB en sesión, <5MB post-GC pre/post. Batería/thermal 8-12min: thermalStatus nunca
>LIGHT en dispositivos de referencia; si MODERATE reproducible en gama media ⇒ recortar carga visual del
tracer. iOS: mismos presupuestos vía Instruments CUANDO haya Mac (DEFERRED-MAC, declarado no-verde).
Tracer B + LTPO: mantener animación imperceptible o fijar frame rate (`Surface.setFrameRate` /
`preferredFrameRateRange`) — INFERRED, verificar en dispositivo 120 Hz.

## E) Elegibilidad RT operativa (derivada EN SESIÓN, por segmento; fallo mid-session degrada solo el segmento)
| Plataforma/banda | Clase | Accuracy (A y B) | RT |
|---|---|---|---|
| Android 34+ 60Hz | T1+ | OFICIAL | descriptivo local (mismo dispositivo/Hz); NO oficial hasta T2 |
| Android 34+ 120/VRR | T1+ SOLO con C.1; sin C.1 → T0 para RT | OFICIAL | descriptivo local por modo Hz |
| Android 29-33 | T1 | OFICIAL | descriptivo local (precisión ms marcada) |
| Android 26-28 (si CEO) | T1 | OFICIAL (si presupuesto D pasa; si no, el clasificador degrada solo) | descriptivo local |
| iOS hoy | T0-para-RT | OFICIAL | NINGUNO |
| iOS + port | T1 | OFICIAL | descriptivo local |
| T2 (banco físico futuro) | T2 | — | oficial dentro de clase |
Invariantes: accuracy SIEMPRE elegible; NINGÚN RT alimenta evidencia oficial en ninguna celda actual; RT
descriptivo jamás entra en ModuleContribution (precedente SpeedProfileSource — y aun ese gate < T2).

## F) Inventario requerido al CEO
1. Android gama baja 60Hz API29-31 · 2. Android gama media/alta 120Hz/VRR API34+ · 3. (solo si ratifica API26)
un físico API26-28 — el emulador NO responde rendimiento de esa banda · 4. un iPhone ≥iOS15 (ideal ProMotion)
+ acceso puntual a un Mac. Emulador cubre: corrección funcional, flujo de exclusión, tests JVM del calculador.
NO cubre: pacing real, touch, thermal, VRR, GC en HW — **un GO de emulador no es un GO**. macos-14 CI:
compile-only.

**Riesgos top-3:** (1) 60Hz hardcodeado ⇒ jank-gate ciego >60Hz (bloqueante, barato). (2) iOS sin frame-lock ⇒
RT iOS no sale del dispositivo ni como descriptivo comparable hasta el port. (3) Thermal = confound científico
⇒ exclusión por segmento con deriva medida, nunca cap binario.
