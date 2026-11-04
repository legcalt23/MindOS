# ADR — Honestidad de la medición de frames en la lane de Velocidad (`MB-FOCUS-0071`)

**Fecha:** 2026-08-29 · **Estado:** DECIDIDO · **Alcance:** `speed/` + `core/timing` (Android)
**Regla aplicada:** los criterios de aborto se declaran ANTES de escribir código (precedente R12).

---

## 1. Qué dice el hallazgo y qué es cierto

`MB-FOCUS-0071` (P1, OPEN): *«La sonda de la lane de VELOCIDAD conserva el fallback de cadencia que
focus2 retira. `FrameTimingProbe.android.kt:55-56` sigue haciendo `?: DEFAULT_REFRESH_HZ`, el mismo
patrón que `MB-FOCUS-0069` acaba de eliminar de focus2. Ahí alimenta la clasificación de calidad de
Velocidad, no la de Focus.»* Re-severizado P3→P1 el 2026-08-19: *«esa sonda ESTÁ cableada a producción,
mientras el guard de honestidad de P7-F1 está acotado por nombre de fichero y no puede verla.»*

Verificado mitad por mitad sobre el árbol:

| Mitad del enunciado | Veredicto | Evidencia |
|---|---|---|
| `?: DEFAULT_REFRESH_HZ` en :55-56 | **CIERTA**, y son **DOS** fallbacks | `?:` en :55 y `if (hz > 0.0) … else DEFAULT_REFRESH_HZ` en :56 |
| «el mismo patrón que 0069 eliminó de focus2» | **CIERTA pero INCOMPLETA** | ver §2 |
| «alimenta la calidad de Velocidad, no la de Focus» | **CIERTA** | `SpeedExerciseScreen.kt:112` → holder :109/:156/:325 → `QualitySignals` :327 |
| «el guard P7-F1 está acotado por nombre de fichero» | **CIERTA** | `WindowedTemporalProbeCadenceHonestyTest.kt:97,100` hard-codean los dos nombres |
| Severidad P1 | **MATIZADA** — ver §4 | el consumidor de perfil es fail-closed en su primera línea |

## 2. Lo que el enunciado NO contiene, y es lo que gobierna el arreglo

`MB-FOCUS-0069` **no** se limitó a hacer nullable el proveedor de cadencia. Hizo DOS cosas, y la
segunda es la que da la honestidad:

1. `refreshHzProvider: () -> Double?` — la ausencia se declara (`WindowedTemporalProbe.android.kt:34`),
   y sin cadencia declarada **no se computa ningún conteo de drops** (`drainWindow`: `expectedHz?.let {…}`).
2. El CONSUMIDOR modela la ausencia: `TemporalWindowObservation` conserva `sampledFrames` y
   `expectedCadenceHz`, y `TemporalClassPolicy.windowIntegrity` la enruta a un veredicto DISTINTO
   (`NoObservation`) mediante un **sample gate + `cadenceResolved`**.

El KDoc de `TemporalClassPolicy.kt:6-10` nombra literalmente la trampa como guarda portante nº 1:

> *«Sample gate FIRST (psicómetra P0-3): an absent/empty observation must never read as CLEAN —
> `FrameStats.UNAVAILABLE` carries `frameDropFraction = 0.0`, so any threshold check that runs before
> the sample gate fabricates perfection from absence.»*

**Velocidad no tiene ninguna de las dos guardas.** `QualitySignals` (`SessionQualityEvaluator.kt:23-32`)
**descarta `sampledFrames`**, y `evaluateContamination` corre los umbrales directamente. Cadena viva:

```
frameStats sin medición  ->  criticalFrameJitter = false  ->  deviceTimingRisk = LOW (con NANOS_NATIVE)
                         ->  frameDropFraction  = 0.0     ->  contaminated = false
                         ->  source = CHOREOGRAPHER_FRAME ->  authoritativeSource = true
                         =>  SessionQuality.HIGH
```

**La ausencia de medición produce la calificación MÁXIMA.** Es `null != 0` en su dirección que fabrica
el máximo (aprendizaje 2026-07-15), y el propio repo ya había escrito la advertencia — en `core/timing`,
el módulo que Velocidad comparte.

### 2.1 Qué disparador es realmente alcanzable

- **Android — la cadencia.** `start()` precede SIEMPRE a `stop()` (`finishSession` solo es alcanzable
  desde el flujo de ensayos), y una sesión acumula miles de frames, así que `sampledFrames < 2` **no es
  alcanzable** ahí. Lo alcanzable es `view.display == null` (vista no adjunta) o `refreshRate <= 0` ⇒
  nominal fabricado de 60 Hz. En un panel de 90/120 Hz eso convierte jank REAL en falso negativo — el
  mismo defecto que el KDoc de la propia sonda dice haber arreglado, reintroducido por su fallback.
  El precedente de alcanzabilidad ya está sentado: `MB-FOCUS-0069` fue P1 por exactamente
  «cuando la vista no daba display», sobre el mismo `LocalView` y el mismo `View.getDisplay()`.
- **desktop/iOS — la ausencia total.** `rememberFrameTimingProbe()` devuelve `NoOpFrameTimingProbe`, cuyo
  `stop()` es `FrameStats.UNAVAILABLE`. Hoy no llega a HIGH, pero **por el `source`, no por ningún
  razonamiento sobre frames**: la protección es incidental, no diseñada.

### 2.2 Una afirmación falsa encontrada de camino

`FrameTimingProbe.kt:29-31` afirma que `NoOpFrameTimingProbe` devuelve `UNAVAILABLE`, *«which the
classifier treats as NON-authoritative (never authoritative)»*. **Falso por dos motivos**: (a)
`TimingPolicyClassifier.classify` nunca inspecciona `sampledFrames` — con `source`+`precision` buenos,
`UNAVAILABLE` cae en la rama `Authoritative(source, frame)`; lo que salva a desktop/iOS es el `source`;
(b) `TimingPolicyClassifier` tiene **CERO call-sites de producción** (solo un test y dos KDoc que dicen
«espeja»): es una costura muerta, patrón `MB-CATALOG-0022`. Se registra, no se construye sobre ella.

## 3. Decisión

Se replica el **contrato honesto completo**, no solo el `?:`:

1. `refreshHzProvider: () -> Double?` — se borran los DOS fallbacks. Sin cadencia declarada no se
   computa ningún conteo de drops (espeja `WindowedTemporalProbe.android.kt`).
2. `QualitySignals` gana `sampledFrames: Int` **obligatorio, sin default** (un default es el hardcode
   esperando volver, 2026-08-18). Los 3 sitios de construcción quedan forzados por el compilador.
3. `SessionQualityEvaluator` obtiene su **sample gate**: las señales de frame solo son portantes cuando
   `sampledFrames > 0`. Sin medición: (a) no se puede afirmar contaminación — la ausencia **no acusa**;
   (b) no se puede conceder `HIGH` — `HIGH` afirma un dispositivo medido y limpio. Cae a `ACCEPTABLE`,
   con el motivo `FRAME_TIMING_UNMEASURED` en el log.

Dirección fail-closed elegida deliberadamente: **retener la afirmación superior, jamás acusar**. Marcar
`CONTAMINATED` una sesión no medida sería fabricar un hecho sobre el usuario en la dirección contraria.

**Rechazado — la prescripción literal del hallazgo.** «Espejar el `?:`» a secas dejaría `stats == null`
sin sitio honesto donde aterrizar en Velocidad: se convertiría en `UNAVAILABLE`, que es exactamente lo
que el consumidor lee como perfecto. Sin la guarda (2) el arreglo no arregla nada.

## 4. Consecuencia medida en LAS DOS direcciones

- **Hacia el perfil cognitivo: NULA hoy.** `SpeedProfileSource.contribution()` es fail-closed en su
  primera línea: `SpeedAntiGaming.COVERAGE = PARTIAL` (:63) y `PARTIAL.adequateFor(RT_CRITICAL) = false`
  (`ScoreTypes.kt:74`), luego devuelve `null` incondicionalmente. **El defecto está ARMADO, no muerto**:
  se activa el día que esa constante pase a `ADEQUATE` tras §15, y `eligibleContribution` «resume
  unchanged» según su propio KDoc. Misma forma que `MB-CATALOG-0011`.
- **Hacia el usuario: VIVA hoy.** `SpeedExerciseScreen.kt:463` renderiza
  `state.qualityLog?.sessionQuality?.name` como línea de resultado. Una sesión cuya medición de frames
  nunca ocurrió muestra su etiqueta de calidad al usuario como un hecho.

Severidad: se mantiene **P1** — no por el perfil (fail-closed), sino por la afirmación viva al usuario
más el armado del perfil detrás de una constante de una línea.

## 5. Criterios de aborto (declarados ANTES de escribir código)

Si cualquiera se incumple, el cambio se revierte y se re-diseña:

- **A1.** Ninguna sesión con `sampledFrames > 0` cambia su `SessionQuality`. La clasificación de toda
  sesión REALMENTE medida es byte-idéntica a la de hoy.
- **A2.** Ninguna sesión de desktop/iOS cambia su `SessionQuality` (ya eran `ACCEPTABLE` por `source`).
- **A3.** La ausencia de medición **nunca** produce `CONTAMINATED` ni `LOW`. Solo retiene `HIGH`.
- **A4.** `deviceTimingRisk` conserva su semántica para toda sesión medida.
- **A5.** El delta de comportamiento queda confinado a «sesión Android sin medición de frames», que es
  exactamente el defecto.

## 6. Techo declarado

- El proveedor de cadencia se consulta **una sola vez, al final de la sesión**, y se aplica retroactivamente
  a todos los frames. Con refresco adaptativo (120→60 Hz en reposo) la lectura final no describe toda la
  sesión. **No se arregla aquí.** Se registra como `MB-FOCUS-0087`.
  **CORREGIDO por el gate (§9, C8):** la primera redacción de este techo decía que focus2 «lo consulta por
  ventana, y eso es lo que hace comparables sus ventanas». La mitad factual es cierta —`drainWindow()`
  llama al proveedor en `WindowedTemporalProbe.android.kt:79`— pero **la mitad causal era falsa**, y era la
  que sostenía la prescripción: `drainWindow()` **drena los frames en `:71`, ANTES de leer el proveedor en
  `:79`**, así que leer por ventana es la MISMA estructura retroactiva con grano más fino, y un drenaje que
  cruza un cambio de cadencia se clasifica igual de mal. Lo que de verdad protege a focus2 es
  `TemporalClassPolicy.cadenceResolved` (`:71-80`), que compara la cadencia DECLARADA con la DETECTADA y se
  **niega a clasificar** si discrepan más que `cadenceMatchTolerance`. iOS lo zanja: `WindowedTemporalProbe.ios.kt:88`
  fija `expectedCadenceHz = null` incondicionalmente —cero lecturas por drenaje— y aun así se maneja
  honestamente. La prescripción correcta NO es «leer por ventana» sino replicar esa negativa a clasificar.
- `DeviceProfileFactory.create` lleva `criticalFrameJitter: Boolean = false` **por default** y su único
  call-site no lo pasa. Se registra como `MB-FOCUS-0088`.
  **CORREGIDO por el gate (§9, C6).** Este bullet declaraba originalmente DOS hechos, y el segundo —que
  `TimebasePolicy.deviceTimingRisk` derivaba `LOW` con `criticalFrameJitter = false` «venga de una medición
  o de su ausencia»— **no era un techo aceptable: era el MISMO defecto que esta fase existe para cerrar**,
  en la misma función, y en el canal que SÍ se renderiza al usuario. Se ha **CERRADO en esta fase** (§9.1),
  no registrado. Lo que queda registrado como `0088` es solo la copia de `DeviceProfile`, que no tiene lector.
- `frameTimesNanos` es un `ArrayList<Long>` **sin cota** (boxing por frame). focus2 lo sustituyó por un
  `FrameTimeRing` de 8 KiB por los bloqueadores B1/B2 de Paso 5. Velocidad conserva el patrón viejo: en una
  sesión larga a 120 Hz son decenas de miles de `Long` boxeados, y el instrumento contribuye al jank que mide.
  Se registra; no se toca en esta fase.
- La sonda de Velocidad tampoco tiene el **contador de GENERACIÓN** que focus2 añadió por REDTEAM #22
  (recomposición que relanza `start` sin que el `stop` previo haya aterrizado). Se registra.

- **Asimetría deliberada, declarada para que nadie la «unifique»:** `QualitySignals.sampledFrames` es
  OBLIGATORIO sin default —un default reinstauraría en silencio la fabricación que el campo existe para
  impedir— pero `SyntheticUserRunner.SyntheticRunConfig.sampledFrames` SÍ lleva default
  (`DEFAULT_SIMULATED_FRAMES = 600`). No es incoherencia: una config de simulación existe justamente para
  DESCRIBIR el dispositivo modelado, y el fichero ya tenía `criticalFrameJitter: Boolean = false` en ese
  mismo papel. El default modela «dispositivo medido y limpio», que es lo que `frameDropFraction = 0.0`
  afirma en ese mismo objeto; pasar `0` simula explícitamente «no se midió nada». La regla que se aplica es
  «sin default en el contrato de producción», no «sin default en ninguna parte».

## 7. Correcciones aplicadas a este mismo documento

Gate adversarial independiente: **20 agentes**, 5 lentes + verificación por refutación, `agentType:
architect` (Read/Grep/Glob, **sin Bash** — el mecanismo de `MB-FOCUS-0083`, para que un revisor no
pueda invocar Gradle ni destruir evidencia). Veredictos: **espejo CONFIRMADA**, las otras cuatro
`PARCIALMENTE_FALSA`; 14 afirmaciones sobrevivieron a su verificador y **1 fue refutada**. Cuatro
cosas corrigen o amplían §1–§6, y tres de ellas van contra mi propio trabajo.

### C1 — §1: la mitad (b) del enunciado es LITERALMENTE CIERTA; lo incompleto es la PRESCRIPCIÓN

Escribí «CIERTA pero INCOMPLETA». Una lente fue más lejos y la declaró «MITAD FALSA» argumentando que
`MB-FOCUS-0069` no retiró el operador ni la constante. **Su verificador la refutó con cuatro
contraejemplos**: el ledger (`MINDBOOST_AUDIT_LEDGER.md:344`) dice literal *«El provider pasa a
`() -> Double?` sin constante de reserva»*; `WindowedTemporalProbe.android.kt` tiene **cero**
ocurrencias de `DEFAULT_REFRESH_HZ`; existe un test dedicado a pinnear esa ausencia
(`theAndroidAdapterNeverNamesTheNominalFallback`); y `MB-FOCUS-0072` nació precisamente porque la
primera versión de ese guard sobrevivía verde a re-introducir el `?:` por mutación.

Corrección: el enunciado de `MB-FOCUS-0071` es **cierto en sus dos mitades**. Lo que estaba incompleto
es su **prescripción implícita** — y esa distinción es exactamente la asimetría que esta fase ya
practica (verificar enunciado y prescripción por separado). §1 y §3 se leen con esa precisión.

### C2 — §4: HIGH frente a ACCEPTABLE **no** decide la elegibilidad de perfil, solo la confianza

Mi §4 daba a entender que la calidad fabricada sería la puerta al perfil. Medido: `qualifyingSessions`
excluye **solo** `CONTAMINATED` y `LOW`, así que HIGH y ACCEPTABLE **entran ambos**; la única
diferencia es el multiplicador de `confidence(…)`, que además está acotado por debajo de ESTABLISHED
mientras §15 siga abierta. Consecuencia honesta, corregida: incluso con la cobertura elevada a
ADEQUATE, el daño de este defecto hacia el perfil sería una **confianza inflada**, no una sesión
inelegible colada. La frontera con consecuencia de inclusión es `CONTAMINATED`/`LOW`, no HIGH.
El veredicto de §4 (consecuencia de perfil NULA hoy) no cambia: lo corta `contribution()`.

### C3 — un vector de re-fabricación que yo no había visto, y que se CIERRA aquí

`FrameStatsCalculator.fromFrameTimes` tenía `refreshHz: Double = DEFAULT_REFRESH_HZ` **por default en
las dos sobrecargas** (`:26` y `:36`). Los 3 call-sites de producción lo pasaban explícito, así que hoy
no fabricaba nada — pero un caller futuro podía re-fabricar 60 Hz **sin nombrar la constante**, y un
guard textual por fichero es estructuralmente incapaz de verlo. Es el mismo patrón que esta fase está
retirando dos ficheros más allá, y dejarlo habría repetido el fallo de «escribir la regla y no barrer
el árbol». El default se retira; los 6 call-sites de test pasan la cadencia explícita.

### C4 — el guard tenía que ampliarse, porque «no puede verla» ERA la razón de la P1

La re-severización P3→P1 se justificó en que el guard de honestidad *«está acotado por nombre de
fichero y no puede verla»*. Verificado literal: `WindowedTemporalProbeCadenceHonestyTest.kt:97,100`
hard-codea los dos nombres. Cerrar el hallazgo arreglando solo el código habría dejado esa razón
intacta y cero protección contra la reintroducción. El guard se amplía **en el fichero que ya era
dueño del invariante** (no se crea un segundo dueño) para cubrir la sonda de Velocidad y el
calculador compartido: la regla pasa a estar acotada por el INVARIANTE, no por los dos nombres de
fichero que lo albergaron primero.

### C5 — las dos mitades del arreglo son conjuntamente necesarias (autocrítica)

El red team probó que, **antes** de esta fase, el conjunto {`FrameStats.UNAVAILABLE`} ∩
{`SessionQuality.HIGH`} era **vacío**: donde la ausencia estaba garantizada (desktop/iOS) HIGH era
inalcanzable por el `source`, y donde HIGH era alcanzable (Android) `sampledFrames < 2` no lo era.
De ahí se sigue algo incómodo y cierto: **el sample gate por sí solo no habría cambiado nada en
ninguna plataforma.** Lo que lo vuelve portante es la otra mitad — el proveedor nullable CREA la
ausencia honesta en Android (`display == null` ⇒ `UNAVAILABLE`), y el gate es lo que impide que esa
ausencia recién declarada se lea como un dispositivo impecable. Ninguna de las dos mitades funciona
sola, y la mutación de la fase debe demostrar exactamente eso.

Esto también precisa el defecto que había ANTES: en Android no era «cero frames leídos como perfectos»
sino **miles de frames medidos contra un nominal fabricado de 60 Hz**, que en un panel de 90/120 Hz
esconde jank real. El sample gate no lo habría cazado; solo el proveedor nullable lo caza.

---

## 8. Hallazgos registrados al medir el alcance (NO implementados)

Salen de leer la sonda de Velocidad y su gemela de focus2 **en pareja** —la disciplina que
`MB-FOCUS-0069` dejó escrita: «adapters que fallan distinto no son comparables»— y ninguno pertenece
a esta fase: el hallazgo que se cierra aquí es de HONESTIDAD DE MEDIDA, y estos son de ciclo de vida,
de costura muerta y de segunda fuente de verdad. Se registran con dueño, alcance medido y techo
declarado; el código queda **deliberadamente intacto**, igual que `MB-FOCUS-0071` quedó intacto en el
Paso 7 de focus2 por la misma razón.

> **CORRECCIÓN del gate (§9, C5 y C7).** El título de esta sección decía «Hallazgos **NUEVOS**
> descubiertos al medir el alcance» y el preámbulo afirmaba que **los cuatro** llevan «alcance medido y
> techo declarado». Ambas cosas eran falsas en parte: `MB-FOCUS-0085` **no es nuevo** —un gate C4 anterior
> ya lo registró, y a **P1**, en `docs/engines/focus_v5/reaudit/c4/C4_LENS_DICTAMENES.json`— y el techo de
> `8.2` dice literalmente «no se midió si las tres definiciones coinciden hoy», que es lo contrario de
> «alcance medido». Corregidos el título y el preámbulo; la severidad de `8.1` se reconcilia abajo.

### 8.1 `MB-FOCUS-0085` (P1) — la sonda de Velocidad fuga un callback de Choreographer que se re-postea solo

> **SEVERIDAD RECONCILIADA (§9, C5): P2 → P1.** Registré P2 sin reconciliar contra el propio repo. El
> dictamen C4 (`C4_LENS_DICTAMENES.json`, finding «B1/B2/B3 observer-effect fixes applied to only one of
> the two Android frame probes — the unfixed sibling is the wired one») ya lo fijó en **P1**, con
> `when_wired: "ALREADY WIRED — this is the exception to the zero-blast-radius rule"`, y describe la ruta
> que mi techo declaraba como no demostrada: la presión de asignación del callback fugado **eleva los
> deltas de frame que mide la sesión SIGUIENTE**, lo que llega a `criticalFrameJitter` →
> `DeviceTimingRisk.HIGH` y **degrada la calidad de la sesión del usuario**. Es decir, la fuga no solo
> gasta batería: acusa. Un P1 previo no se baja sin medir; se sostiene.

`AndroidFrameTimingProbe.start()` postea un `Choreographer.FrameCallback` que **se re-postea a sí
mismo en cada frame** mientras `running`. El único `stop()` de producción está en
`SpeedSessionStateHolder:325`, dentro del cierre de sesión: abandonar la pantalla a mitad
(`onExit`, back, proceso en background) **nunca lo llama**. `running` se queda en `true`, el callback
sigue despertando en cada vsync durante toda la vida del proceso, y el `ArrayList<Long>` crece sin
cota. Medido: **cero `DisposableEffect` en toda la feature `speed`**.

La sonda gemela de focus2 lleva las **tres** protecciones que a ésta le faltan, y su propio KDoc las
llama portantes: `DisposableEffect(probe) { onDispose { probe.stop() } }` («load-bearing, Paso 5,
observer-effect B3»), un **contador de generación** (REDTEAM #22: un callback de una generación rancia
ni graba ni se re-postea) y un `FrameTimeRing` **acotado** de 8 KiB sin boxing por frame
(observer-effect B2). La de Velocidad usa `ArrayList<Long>`, que **caja un `Long` por vsync** — la
asignación que el ring de focus2 existe para eliminar, y que aquí ocurre durante la medición.

Prescripción exacta (una línea + el guard): `DisposableEffect(probe) { onDispose { probe.stop() } }`
en `rememberFrameTimingProbe()`, con el contador de generación espejando la implementación de
referencia, que vive en **otro fichero del mismo paquete** (`WindowedTemporalProbe.android.kt:41`,
`:48-49`, `:56`, `:96`).

**Techo, CORREGIDO por el gate (§9, C5).** Mi redacción decía «no se demostró corrupción de la medida
de la sesión SIGUIENTE (cada sonda tiene su propia lista)». La premisa es cierta y la conclusión NO se
sigue: la ruta de contaminación no pasa por compartir la lista, sino por el **coste de mantenerla**. El
dictamen C4 ya la había descrito: el callback fugado sigue despertando en cada vsync, añadiendo un
`Long` **boxeado** a una lista sin cota **en el hilo principal**, justo en la ventana que la sonda
SIGUIENTE está midiendo; esa presión de asignación eleva los deltas de frame → `criticalFrameJitter` →
`DeviceTimingRisk.HIGH` → degrada la calidad de la sesión del usuario. Y es **acumulativo**: N
abandonos dejan N cadenas vivas. El techo honesto es otro: no se ha medido la MAGNITUD del efecto en un
dispositivo real (cuántos abandonos hacen falta para cruzar el umbral), no su existencia. Corolario que
esta fase tampoco tocó: `FrameTimeRing.kt:6` afirma que reemplaza «the `ArrayList<Long>` the probes used
to hold» —en plural— mientras `FrameTimingProbe.android.kt:35` sigue teniendo uno; es una afirmación
falsa viva en KDoc de producción, de la familia «documentado como arreglado y no arreglado».

### 8.2 `MB-FOCUS-0086` (P3) — `TimingPolicyClassifier` es una costura muerta con DOS sombras que se documentan como su espejo

`TimingPolicyClassifier.classify` tiene **cero call-sites de producción**. Sus únicas referencias en
el árbol son su propio test (`TimebaseClassifierTest.kt:23`) y **dos KDoc de producción que declaran
espejarlo**: `SpeedProfileSource.kt:133` («Mirrors `TimingPolicyClassifier`'s `Authoritative`
definition») y `SessionQualityEvaluator.kt:85` («espeja TimingPolicyClassifier»). No es sólo código
muerto: es una **autoridad fantasma** con dos re-implementaciones que se justifican citándola, así que
la deriva entre las tres es posible y nadie la vería. Es la forma de `MB-CATALOG-0022` (costura muerta)
agravada por el anti-patrón de 2026-08-18: lo conservado para una fase que no llegó se LEE como
capacidad existente. Salida por defecto según el precedente del repo: **borrarla** y que las dos
sombras dejen de citarla, o darle su consumidor. **Techo:** no se midió si las tres definiciones
coinciden hoy; el hallazgo es la ausencia de mecanismo, no una divergencia demostrada.

### 8.3 `MB-FOCUS-0087` (P2) — una sola lectura de cadencia al final de la sesión, aplicada retroactivamente

`AndroidFrameTimingProbe.stop()` llama `refreshHzProvider()` **una vez, al cerrar**, y mide contra ese
valor **todos** los frames de la sesión entera. La gemela de focus2 lo lee **por drenaje**
(`drainWindow()`), que es la razón de que sus ventanas sean comparables. En un panel con refresco
adaptativo (120→60 Hz) las dos direcciones del error son alcanzables: si la sesión corrió a 120 y se
lee 60 al cerrar, el periodo nominal es 16,67 ms y frames reales de 8,33 ms **no marcan ni un drop**
(falso negativo, la dirección que fabrica perfección); si corrió a 60 y se lee 120, **todo** frame
supera 1,5 × 8,33 ms y la sesión sale `CONTAMINATED` (acusación falsa). **Techo:** no se midió la
frecuencia real con que un dispositivo cambia de cadencia a mitad de sesión; lo medido es que la
estructura permite ambos errores.

> **PRESCRIPCIÓN CORREGIDA por el gate (§9, C8).** La primera versión prescribía «leer la cadencia por
> ventana, como focus2». Es **la prescripción equivocada**: `drainWindow()` drena los frames (`:71`) y
> DESPUÉS lee el proveedor (`:79`), así que por ventana es la misma estructura retroactiva con grano más
> fino, y un drenaje que cruza el cambio se clasifica igual de mal. Lo que hace honesta a la lane hermana
> es `TemporalClassPolicy.cadenceResolved` (`:71-80`): compara la cadencia **declarada** con la
> **detectada** y se NIEGA a clasificar si discrepan más que `cadenceMatchTolerance` — la regla de
> 2026-08-18, «un instrumento nunca debe clasificar una traza contra un referente derivado de esa misma
> traza», resuelta con dos hechos separados en vez de uno. Velocidad ya tiene la materia prima
> (`FrameStatsCalculator.detectedRefreshHz`), así que la prescripción correcta es replicar esa negativa,
> no cambiar la frecuencia de lectura.

### 8.4 `MB-FOCUS-0088` (P3) — un default `= false` fabrica un `deviceTimingRisk` persistido y compartido entre usuarios

`DeviceProfileFactory.create(..., criticalFrameJitter: Boolean = false)` lleva un default que afirma
«sin jitter crítico» cuando quien llama no lo sabe — exactamente lo que la regla de 2026-08-18 prohíbe
(«todo parámetro de un control de calidad que pueda ser no-medible se declara obligatorio: un default
restaura el hardcode en silencio»). El único call-site de producción, `buildDeviceProfile()`
(`SpeedSessionStateHolder:394`), **no lo pasa**, y no puede: se invoca en la línea 318, **siete líneas
ANTES** de `frameProbe.stop()` (325), así que en ese punto las estadísticas de frames todavía no
existen. La omisión es estructural, no un argumento olvidado.

Resultado: cada corrida persiste **dos** `deviceTimingRisk` que pueden contradecirse — el de
`QualityDecision` y el fabricado dentro de `DeviceProfile`, que además es el **durable y compartido
entre usuarios del mismo dispositivo** (§16 de su propio KDoc).

> **CORRECCIÓN del gate (§9, C6).** Este párrafo decía que el de `QualityDecision` era «el honesto,
> calculado con el jitter real». **Era falso cuando lo escribí**: en ese momento `QualityDecision`
> derivaba su `deviceTimingRisk` ANTES del sample gate, con el `criticalFrameJitter = false` que viaja
> dentro de `FrameStats.UNAVAILABLE`. Es honesto **ahora, y solo porque esta fase lo arregló** (§9.1). La
> frase original no solo era incorrecta: enterraba un techo de §6 declarándolo resuelto.

**Techo declarado, y es lo que baja la severidad a P3:** medido, `DeviceProfile.deviceTimingRisk` no
tiene ningún lector **de decisión** — la UI (`SpeedExerciseScreen:464` y `:624`) lee el de `qualityLog`.
Matiz que el gate exigió precisar: el campo **sí se serializa** en el modelo durable y sale por el export
de depuración, así que «sin lectores» significa «nada decide con él», no «nadie lo escribe ni lo lee
jamás». Hoy el daño de decisión es nulo; el riesgo es que el campo es durable y compartido, así que el
primer lector futuro heredaría un hecho fabricado sin nada que se lo advierta.

Precisión de citas (§9, C9): el default está en `SessionQualityEvaluator.kt:153`; `buildDeviceProfile()`
se **invoca** en `SpeedSessionStateHolder:318` y se **declara** alrededor de `:394` — la primera versión
citaba `:394` como si fuera el call-site.

## 9. Segunda pasada del gate adversarial (9 agentes read-only) y lo que refutó

Ejecutado **después** de dar la fase por terminada y **antes** de sellar el ledger, con `agentType:
architect` (Read/Grep/Glob, **sin Bash**) — el mecanismo de `MB-FOCUS-0083`: un revisor con Bash puede
invocar Gradle y destruir la evidencia que audita. 8 lentes (2 por hallazgo, cada una instruida para
REFUTAR, con veredicto por DEFECTO «refutado») + 1 crítico de completitud. Resultado: **ninguno de los
cuatro enunciados sobrevivió intacto como yo los escribí**, y el crítico encontró un defecto que la
sección 8 no contenía — dentro del fichero que esta misma fase arregló.

### 9.1 C4 — el QUINTO defecto: la ausencia seguía fabricando el `deviceTimingRisk` que SÍ ve el usuario

**Bloqueante, y el más caro de la fase.** El sample gate que añadí protegía `sessionQuality`, pero
`deviceTimingRisk` se derivaba **arriba, antes del gate**. Cadena reproducida con los bytes: frames sin
medir → `FrameStats.UNAVAILABLE` → `criticalFrameJitter = false` → `TimebasePolicy.kt:70` no toma la rama
HIGH → `:71` con `NANOS_NATIVE` cae directo en **`DeviceTimingRisk.LOW`**, el riesgo MÍNIMO, fabricado
desde la ausencia. Se devuelve en `QualityDecision`, se persiste (`SpeedSessionStateHolder:363`) y **se
renderiza al usuario** en `SpeedExerciseScreen:464`, una línea por debajo de la que esta fase arregló
(`:463`).

Es el defecto de `MB-FOCUS-0071` **en su segundo canal**: misma ausencia, misma función, misma dirección
halagadora, y el canal más visible de los dos (la copia de `DeviceProfile` de `8.4` no tiene lector de
decisión; ésta se pinta en pantalla). El crítico lo probó además con mis propios tests: yo pinneaba
`deviceTimingRisk` para el caso MEDIDO (`SessionQualityEvaluatorTest.kt:40`) y **nunca** para el no
medido. Cerrar `0071` dejando esto vivo habría sido un cierre falso — exactamente la forma «documentado
como arreglado y no arreglado».

**Arreglado en esta fase** (no registrado): la derivación baja por debajo del sample gate y sin frames
medidos ya no afirma `LOW`. Dirección fail-closed idéntica al resto: **no afirmar** (nada de LOW sin
medida) y **jamás acusar** (nada de HIGH con un jitter que nadie observó); `MODERATE` es el bucket «no
afirmado limpio», y ningún consumidor penaliza por él —se verificó enumerando los 5 consumidores de
`QualityDecision.deviceTimingRisk`: render, persistencia, export, simulación y la rama HIGH que ya estaba
gateada—. Evidencia RED→GREEN sobre código de producción real: antes del arreglo el guard nuevo falla con
`AssertionError: sin un solo frame observado no se puede AFIRMAR el riesgo de timing mínimo. Actual: LOW`;
después pasa, con su mitad discriminante (la misma señal CON frames sí afirma `LOW`) en verde.

**Techo declarado:** `MODERATE` queda sobrecargado — «millis-derived» y «sin medir» comparten valor. Lo
que los desambigua es `FRAME_TIMING_UNMEASURED` en `reasons`, que se persiste junto a la decisión.
Modelarlo en el propio tipo (`UNKNOWN`, o nullable) exige tocar un enum **serializado** dentro de
`QualityDecisionLog`: decisión de esquema durable, y por tanto de otra fase.

### 9.2 Correcciones aplicadas a este documento

| # | Qué decía yo | Qué dicen los bytes |
|---|---|---|
| C5 | `MB-FOCUS-0085` es **nuevo** y va a **P2**; «no se demostró corrupción de la sesión siguiente» | Un dictamen C4 anterior ya lo registró a **P1** (`C4_LENS_DICTAMENES.json`), marcado `ALREADY WIRED`, y describe la ruta de contaminación de la sesión siguiente que yo daba por no demostrada. **P2 → P1**, novedad retirada, techo corregido |
| C6 | «el honesto de `QualityDecision`, calculado con el jitter real» (§8.4) | Falso al escribirlo: derivaba antes del sample gate. Y enterraba un techo de §6 declarándolo resuelto. Arreglado en §9.1; §6 y §8.4 corregidos |
| C7 | «los cuatro con alcance medido y techo declarado» | El techo de §8.2 dice literalmente «no se midió si las tres definiciones coinciden hoy». Preámbulo corregido |
| C8 | focus2 «lo lee por drenaje, y eso es lo que hace comparables sus ventanas»; prescripción = leer por ventana | `drainWindow()` drena en `:71` y lee el proveedor en `:79`: igual de retroactivo. La protección real es `TemporalClassPolicy.cadenceResolved` (`:71-80`). **Prescripción de `0087` sustituida** |
| C9 | `buildDeviceProfile()` (`SpeedSessionStateHolder:394`) como call-site | `:394` es la declaración; la invocación es `:318`. Citas precisadas |

Las cinco salieron del gate, no de mí. Es la segunda vez en esta fase que el patrón se repite: **el
enunciado de un hallazgo puede ser cierto y su prescripción llevar al sitio equivocado**, y un hallazgo
escrito por uno mismo no tiene revisor — por eso el gate corre antes de sellar, no después.
