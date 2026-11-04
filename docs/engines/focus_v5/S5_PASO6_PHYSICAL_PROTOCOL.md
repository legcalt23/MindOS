# S5 · Paso 6 — Protocolo físico + matriz de dispositivos

```
ESTADO = DESIGNED_NOT_RUN / CONDITIONAL (2026-08-18)
Fase ejecutada: la SIGUIENTE tras Paso 5 (S5_REOPEN_STATUS.md §10).
Gate del plan (§5, P6): «protocolo registrado; sin hardware => CONDITIONAL».
Este documento REGISTRA el protocolo. NO lo ejecuta: no hay hardware en este host, y un protocolo
ejecutado en un emulador no es un GO (orden-2 §F). Cada medición se declara NOT_RUN por su nombre.
Sin montaje, sin wiring, sin flag. R3 sigue CANDIDATA. Paso 6 NO la promueve.
```

## 0. Qué es esta fase y qué NO es

Paso 6 diseña **el instrumento que decidiría** si la lane temporal de Focus V5 puede algún día sostener
una afirmación de medición, y **pide el inventario** de hardware al CEO. No decide nada sobre el producto:
mientras no haya hardware, todo veredicto físico es `NOT_RUN` y el techo se queda donde `D-R3-02` lo dejó.

Tres cosas que este documento se niega a hacer, porque cada una ya causó una reapertura en este motor:

1. **No convierte «diseñado» en «hecho».** Todo umbral de aquí es `DESIGNED_NOT_RUN`. El propio ADR §11
   lo dice de los umbrales sombra: congelan ANTES de la evidencia confirmatoria.
2. **No ejecuta el protocolo de 2026-07-19 tal cual está escrito.** Ese protocolo (orden-2 §C/§D/§E)
   precede a R2, a R3 y a Paso 5, y varias de sus cláusulas fueron FALSADAS o SUPERSEDIDAS por decisiones
   posteriores. Ejecutarlo literalmente re-introduciría afirmaciones retiradas. La reconciliación es §2.
3. **No promete que la validación física desbloquee RT.** `D-R3-03` es explícita: la elegibilidad futura
   de RT se apoya en un `validatedEndToEndErrorBound` **más** asociación estímulo-frame correcta **más**
   validación física — tres condiciones, no una. Medir `D` es necesario y no suficiente.

## 1. Inventario de obligaciones (qué debe medirse y qué descarga)

Cada fila existe porque una autoridad viva la exige. La última columna es lo que la medición DESCARGA —
si una medición no descarga nada, no debería estar en el protocolo.

| # | Magnitud a medir | Por qué no se puede saber desde el código | Autoridad que la exige | Descarga |
|---|---|---|---|---|
| **M1** | `D` = retardo state-set → **fotones** en pantalla, y su dispersión | El stack de software solo acota PRECISIÓN; la exactitud absoluta (display + digitalizador) es física | ADR §16; orden-2 §A | `validatedEndToEndErrorBound` (1 de las 3 condiciones de `D-R3-03`); unknown §8.4-1 |
| **M2** | Cadencia REALMENTE presentada vs `refreshRate` REPORTADO por la plataforma | Paso 5 separó los dos hechos pero solo puede comprobar que CONCUERDAN, no cuál es cierto si discrepan | ADR §16; §R3-19 B4 | Valida el modelo de cadencia de dos hechos (`F-R3-E`); unknown §8.4-5 |
| **M3** | Comportamiento VRR/LTPO real: cuándo baja el panel, a cuánto, y si el probe lo ve | El SO no notifica el cambio de modo de forma fiable; en estático LTPO cae a 10-24 Hz | ADR §18 (riesgo **top-1**); orden-2 §A | Riesgo top-1; unknown §8.4-5; holgura del Tracer B (§8.4-6) |
| **M4** | Curvas térmicas en bloques de 8-12 min y la deriva de la mediana de RT que inducen | La deriva térmica es indistinguible en código de la fatiga cognitiva del usuario | ADR §16; orden-2 §A/§C.7 | El confound científico que prohíbe atribuir lentitud tardía al usuario |
| **M5** | **Efecto observador del probe**: allocations/GC, coste por frame, delta de calor y batería con probe ON vs OFF | Paso 5 eliminó la causa dominante por construcción; la MAGNITUD residual no es inspeccionable | `MB-FOCUS-0066`; ADR §R3-19 **B5** | El único bloqueante de `§R3-19` que sigue abierto ⇒ desbloquea el gate de montaje |
| **M6** | Tasa de exclusión `e` observada por banda de dispositivo | Gobierna los pisos de sesión y el backstop, hoy `DESIGNED_NOT_RUN` | ADR §11; unknown §8.4-2 | Los valores de `TemporalQualityThresholds` (versión `tqt-1` → `tqt-2` si cambian) |
| **M7** | `getEventTimeNanos` vs latencia real del callback de input | Sellado en el driver; su relación con el frame no es derivable en proceso | ADR §16 | Que un timestamp en nanos NO es por sí solo exactitud (`D-R3-03`) |
| **M8** | `dropCountSinceLastInvocation` por OEM (report-loss) | Cada OEM reporta distinto; el número no es comparable sin caracterizarlo | ADR §16; §R3-19 B4 | Que report-loss ≠ drops, ya tipado en Paso 5 pero sin caracterizar |
| **M9** | Paridad iOS: que `CADisplayLink` observe lo mismo que el bucle Skia/Metal compuso | Es el límite epistémico DECLARADO del adapter iOS; solo un fotodiodo lo dirime | `MB-FOCUS-0065`; ADR §18 | Si iOS puede pasar de `expectedCadenceHz = null` a un valor defendible |

**Cobertura declarada:** M1-M9 cubren las 6 incógnitas de `S5_REOPEN_STATUS §8.4` (M1↔1, M6↔2, M5↔3,
M9↔4, M2/M3↔5, M3↔6). Ninguna incógnita queda sin instrumento, y ningún instrumento sobra.

**Lo que este protocolo NO puede medir, a ningún precio de esta fase:** `T2` (clase «externamente
calibrada») exige un banco de latencia con actuador, no solo un fotodiodo pasivo. ADR §18 ya lo registra
como «sin constructor». M1 acota el error; no construye T2.

## 2. Reconciliación: qué del protocolo original NO se ejecuta

El protocolo físico de este motor nació en el output orden-2 de `mobile-performance`
([`agent_outputs/05_mobile_performance.md`](agent_outputs/05_mobile_performance.md), 2026-07-19). Ese
documento es anterior a la R2, a la R3 y a Paso 5. **Ocho de sus cláusulas están superseded**, y varias
de ellas no fallan de forma visible: se ejecutarían sin error y producirían un veredicto que este motor ya
retiró. Enumerarlas es el trabajo, no un apéndice.

| # | Cláusula original | Estado | Autoridad que la supera |
|---|---|---|---|
| R1 | «el probe Android hardcodea `DEFAULT_REFRESH_HZ=60` ⇒ jank-gate ciego >60 Hz» (hallazgo cabecera) | **CERRADA** | S5 task 0: `refreshHzProvider` lee el Hz real; `REDTEAM #19` pinneado en test |
| R2 | «iOS = NoOp stub» (§Estado del arte, §B) | **SUPERSEDED** | Port `CADisplayLink` escrito en S5 P3 — pero **nunca compilado**; sigue `NOT_EXECUTED` |
| R3 | Banda **Android 26-28** (§B fila 1, §F item 3, `REDTEAM #46`) | **VOID** | `ADR-FOCUS-PLATFORM-MIN` APPROVED: mínimo canónico **API 29**, no se baja `minSdk`. La banda no existe ⇒ no se pide hardware para ella |
| R4 | §C.3 «RT = `eventTime` − `frameCommit`; elimina el sesgo variable = la diferencia práctica T0→T1» | **SUPERSEDED** | `D-R3-03` (sin «commit real» por FrameMetrics; nombre epistémico `FRAME_ISSUED_PROXY`) + Paso 5 B6 (el marcador **no clasifica**). **Sobrevive** su mitad verificable —medir p95 de `stateChange→frameCommit`— como dato DESCRIPTIVO, jamás como derivación de RT |
| R5 | §C.5/§C.7 métricas e invalidación «por SEGMENTO» | **REFORMULADA** | ADR R2 §27: no hay subestructura de segmento persistida; el trial es el denominador y la proporción se lee del histograma por-trial. La MEDICIÓN puede ser por ventana; lo que no existe es un segmento almacenado |
| R6 | §D «pre-dimensionar `ArrayList(2000)` del probe» | **SUPERSEDED** | Paso 5 B1/B2: `FrameTimeRing` (`LongArray`, cap duro, overflow como hecho). **Sobrevive** el presupuesto «0 allocations nuevas por frame en la ventana de respuesta» — es exactamente **M5** |
| R7 | §E columna «Accuracy: **OFICIAL**» en TODAS las filas; `REDTEAM #47` «accuracy OFICIAL» | **FALSA HOY** | `D-R3-02`: S5 no certifica accuracy ni RT; la lane de medición es inconstructible; techo `TRAINING_ONLY`/`NotCaptured`; `OFFICIAL_ELIGIBLE` no construible. **Es la cláusula más peligrosa de la lista**: se lee como una autorización vigente |
| R8 | §E «RT descriptivo local» como salida del dispositivo | **SUPERSEDED** | `D-R3-02` + `rtShadowAdmissible`: el RT existe solo en SOMBRA, sin consumidor. Ningún RT sale como evidencia, ni siquiera «descriptivo» |

**Cláusulas que SÍ sobreviven intactas y son la base de §3-§5:** el modelo de error por capa (§A: sesgo
total ~30-70 ms, mayormente constante intra-dispositivo ⇒ la comparación intra-sujeto mismo-dispositivo es
lo único defendible sin banco); los umbrales GO/NO-GO de §C.6 (drops ≤5% limpio, 5-15% degradado), que son
literalmente el origen de `windowDropCleanMax`/`windowDropExcludeMin`; los presupuestos de §D (frame budget,
allocations, arranque, memoria, térmico); y la regla de §F de que **un GO de emulador no es un GO**.

**Consecuencia operativa:** el protocolo ejecutable es §3-§5 de ESTE documento. El orden-2 se conserva como
evidencia histórica y como fuente del modelo de error, no como instrucción de ejecución.

## 3. Protocolo de medición física

### 3.1 Qué se necesita de `D` (y qué no)

`D` es el retardo entre el instante en que la app fija el estado del estímulo y el instante en que los
fotones llegan al ojo. La intuición dice «hay que medir su valor». Es la mitad equivocada del problema.

Un **sesgo constante** en `D` se cancela en la única comparación que este motor declara defendible: el
sujeto contra su propia línea base, en el mismo dispositivo (orden-2 §A). Si cada trial se retrasa 45 ms,
todos los RT se desplazan 45 ms y ninguna diferencia intra-sujeto cambia. Lo que **no** se cancela es la
**dispersión**: si `D` varía trial a trial, esa varianza entra directa en la varianza del RT y es
indistinguible de variabilidad del usuario — que es justo la señal que un test de atención pretende leer.

Por tanto el objetivo primario de M1 no es la media de `D` sino su **dispersión y su estabilidad**:

- `IQR(D)` y `p95(D) − p05(D)` por celda (dispositivo x modo de refresco x estado térmico).
- Si esa dispersión **cambia** entre celdas (p. ej. crece con el calor), el sesgo deja de ser constante y
  la comparación intra-sujeto pierde su justificación DENTRO de la sesión, no solo cross-device.
- La media de `D` se reporta igualmente, pero su papel es documental: es el número que hace que un RT en
  milisegundos absolutos sea una afirmación sin fundamento (§18 del ADR ya prohíbe surfacearlo).

**Consecuencia para `validatedEndToEndErrorBound`:** el bound que `D-R3-03` exige no es `mean(D)`; es una
cota superior sobre la **dispersión** de `D` con su intervalo de confianza. Un bound sobre la media no
autoriza nada.

### 3.2 Instrumentación: dos niveles, y por qué un fotodiodo solo NO basta

El ADR §16 nombra «FOTODIODO/cámara de alta velocidad» como si fueran intercambiables. No lo son, y la
diferencia es la que decide si el protocolo produce `D` o solo la mitad de `D`:

**un fotodiodo mide el flanco de la PANTALLA; no ve el evento de entrada.** Para obtener `D` end-to-end
hace falta un segundo canal en el MISMO dominio temporal que capture el instante del toque. Sin él, un
fotodiodo entrega el instante de encendido en el reloj del osciloscopio y la app entrega el state-set en el
reloj del dispositivo, y la diferencia entre dos relojes no sincronizados no es una latencia.

| Nivel | Aparato | Reloj común | Cuantización | Qué mide | Coste |
|---|---|---|---|---|---|
| **A** | Cámara de alta velocidad (240 fps mínimo; 960 fps si el móvil de captura lo permite) filmando **dedo y pantalla en el mismo encuadre** | **la propia cámara** | 1 frame de cámara: 4.17 ms @240, **1.04 ms @960** | `D` end-to-end tal como lo vive el usuario, contando frames entre contacto y transición | ~0 EUR (segundo móvil) |
| **B** | Fotodiodo + microcontrolador/osciloscopio **+ actuador** (solenoide o pin capacitivo conmutado) | el disparo del actuador | menos de 0.1 ms | `D` end-to-end automatizable, n grande, repetible | ~50-100 EUR |

**Nivel A es el protocolo por defecto** (es 0 EUR y su cuantización de 1.04 ms es despreciable frente a la
dispersión que se busca acotar, del orden de un período de frame). **Nivel B es la única vía hacia `T2`**:
`T2` («externamente calibrada») no la da un fotodiodo pasivo, la da un banco con actuador que cierra el
lazo — que es exactamente lo que ADR §18 registra como «sin constructor». M1 acota el error; **no**
construye `T2`, y ningún resultado de este protocolo puede reclamarla.

**Cuantización declarada, no ignorada:** a 240 fps la cuantización (4.17 ms) es del orden de un cuarto de
período a 60 Hz. Si la dispersión medida resulta comparable a la cuantización, el resultado es
`INCONCLUSIVE`, no «dispersión baja». Ese es el modo de fallo que convierte un instrumento pobre en una
falsa confirmación.

### 3.3 Arnés de captura (especificado aquí; construido por quien ejecute)

Se especifica y **no se construye en esta fase**: no es verificable sin dispositivo, y un arnés sin
hardware es código muerto con aspecto de progreso.

- Pantalla **debug-only**, alcanzable solo desde un build de depuración, **nunca** una superficie de
  producción ni una ruta de `AppRoute`. Motivo: el §18 del ADR prohíbe que una clase temporal se surface
  como calidad cognitiva; una pantalla de calibración visible sería exactamente eso.
- Contenido: transición de pantalla **completa** negro-blanco al fijar el estado (contraste máximo para el
  fotodiodo y para el umbral de la cámara), sin animación ni fundido — un fundido convierte el flanco en
  una rampa y destruye la medición.
- Registra por repetición: `stateSetNanos`, `firstVsyncAfterStateSetNanos` (el marcador, **descriptivo**),
  `expectedCadenceHz`, `detectedCadenceHz`, `thermalStatus` (API 29+), `batterySaverOn`, y el modo de
  refresco activo.
- **No registra ningún dato de usuario**: el arnés no abre una sesión, no toca `mindboost_focus2`, no crea
  `SessionDescriptive`. Es un instrumento, no una sesión.

### 3.4 Procedimiento por dispositivo (~25 min)

Orden fijo; alterarlo invalida la comparación térmica.

1. **Frío.** Dispositivo en reposo 10 min o más, pantalla al 50% de brillo, modo avión, sin otras apps.
2. **Cadencia declarada (M2).** Leer y anotar el `refreshRate` reportado y el modo activo. Filmar 60 s de
   la pantalla de arnés con el estímulo pulsando a cadencia fija conocida. Contar frames presentados en el
   vídeo. **GO** si la cadencia contada coincide con la reportada dentro de `cadenceMatchTolerance`.
3. **`D` en frío (M1).** n de 100 repeticiones o más. Reportar mediana, IQR, p05, p95 **con IC bootstrap**
   — un p95 de 100 muestras es un estimador ruidoso, y presentarlo sin intervalo es el mismo error de
   estimador puntual que ya falsó una versión anterior de este motor.
4. **VRR/LTPO (M3).** Repetir el paso 3 en dos regímenes: (a) pantalla estática 5 s o más antes de cada
   estímulo, que fuerza al panel LTPO a bajar de tasa; (b) con animación continua que mantenga la tasa
   alta. **La diferencia entre ambos ES la medida.** Anotar si el probe DETECTA el cambio de modo o lo
   atraviesa en silencio: si no lo detecta, el riesgo top-1 del ADR §18 queda CONFIRMADO.
5. **Soak térmico (M4).** 12 min de carga continua del tracer. Registrar `thermalStatus` cada 30 s.
6. **`D` en caliente (M1 + M4).** Repetir el paso 3 inmediatamente después del soak. **La comparación
   frio-caliente de la DISPERSIÓN, no de la media, es el resultado que decide.**
7. **Input (M7).** 30 toques con el arnés registrando `eventTime` y el instante de entrega del callback.
   Reportar la distribución de la diferencia. No se deriva ninguna exactitud de aquí: se documenta que un
   timestamp en nanos no es una afirmación sobre el mundo físico.
8. **Report-loss (M8).** Anotar `dropCountSinceLastInvocation` y el modelo/OEM. Es caracterización, no un
   umbral: el número solo es interpretable dentro del mismo OEM.

**Invalidan la corrida:** llegada de una notificación, cambio de brillo automático, batería por debajo del
30% (activa gobernadores), o una discontinuidad de reloj registrada por el arnés.

### 3.5 iOS (M9)

Mismo procedimiento con una pregunta adicional, que es la única que importa en iOS: filmar simultáneamente
la transición de pantalla y registrar la cadencia que `CADisplayLink` reporta. **Si divergen, el límite
epistémico declarado en `MB-FOCUS-0065` queda CONFIRMADO y `expectedCadenceHz = null` es permanente en iOS,
no provisional.** Si coinciden dentro de la tolerancia en TODOS los regímenes, y solo entonces, se abre la
discusión de si iOS puede declarar una cadencia esperada — discusión que pertenece a S6, no a esta fase.

Precondición no negociable: **el actual iOS de este motor nunca ha compilado**. Antes de cualquier medición
física en iOS hay que cerrar el CI macOS. Medir con un binario que no se sabe si compila no mide nada.

## 4. Presupuestos y efecto observador (M5) — el único bloqueante vivo de §R3-19

Paso 5 cerró B1, B2, B3, B4 y B6. **B5 sigue abierto** (`MB-FOCUS-0066`): el probe induce GC, calor y
consumo que después clasifica. La causa dominante está eliminada por construcción —el ring es `LongArray`
y la ruta caliente no crea un solo objeto por frame— pero **la magnitud residual no está medida**, y
«eliminado por inspección del tipo» no es «medido».

### 4.1 La trampa que invalidaría esta medición

**No se puede medir el probe con el probe.** Si el instrumento que juzga si hay jank es el mismo cuyo coste
se está evaluando, un probe que induce jank se lo atribuye al dispositivo y sale limpio: el estadístico es
auto-consistente por construcción. Es literalmente el defecto que Paso 5 corrigió en el adapter iOS
(`MB-FOCUS-0065`), aplicado ahora al eje del rendimiento.

Por tanto la métrica de aceptación la produce **la plataforma, no nosotros**: `FrameTimingMetric` de
Macrobenchmark y la traza de Perfetto. `FrameStats` no participa en el veredicto de M5 ni como referencia.

### 4.2 Diseño de la comparación

Un A/B donde el probe es **la única diferencia**, y **intercalado (ABABAB), nunca secuencial (AAABBB)**:
la deriva térmica a lo largo de la sesión de benchmark es monótona, así que en un diseño secuencial el
brazo que corra segundo hereda el calor del primero y la deriva se atribuiría al probe. Intercalar convierte
la deriva en ruido común a ambos brazos en vez de en un sesgo a favor de uno.

- **Brazo A:** tracer con `NoOpWindowedTemporalProbe`.
- **Brazo B:** tracer con el probe real de la plataforma.
- Mismo dispositivo, mismo brillo, mismo estado inicial, ≥10 iteraciones por brazo, orden intercalado.

### 4.3 Presupuestos (heredados de orden-2 §D, que sobreviven intactos)

| Magnitud | Presupuesto | Instrumento | Qué significa fallarlo |
|---|---|---|---|
| Allocations en la ventana de respuesta | **0 nuevas por frame** | Microbenchmark `allocationCount` | La afirmación estructural de Paso 5 es falsa; el ring no es el único camino caliente |
| Pausas de GC | **cero pausas > 2 ms** durante la ventana | Perfetto | El probe puede fabricar el jank que clasifica |
| Coste por frame | **< 10 ms @60 Hz, < 6 ms @120 Hz** de trabajo | Macrobenchmark `FrameTimingMetric` p95 | El probe consume el presupuesto que el propio tracer necesita |
| Delta de jank B vs A | **no significativo** con IC | `FrameTimingMetric` | El observador altera lo observado: magnitud del efecto observador |
| `thermalStatus` en 8-12 min | **nunca por encima de LIGHT** en dispositivos de referencia | API 29+ | Recortar carga visual del tracer (orden-2 §D) |
| Delta de heap en sesión | **< 50 MB**, y **< 5 MB** post-GC pre/post | Perfetto / Studio Profiler | Fuga en el instrumento |

### 4.4 Criterio de cierre de B5 (fail-closed)

`§R3-19` pasa a `PASS` **solo si** los seis presupuestos se cumplen **en la banda más pobre del inventario**
(no en el buque insignia: el gate existe para proteger al usuario de gama baja). Cualquier otro resultado:

- **Un presupuesto fallado** ⇒ `§R3-19` sigue `FAIL`, `MB-FOCUS-0066` sigue OPEN, el montaje S6 sigue
  bloqueado, y el probe se rediseña. **No** se «ajusta el presupuesto» para que pase: un umbral que se
  mueve hasta que el sujeto lo cumple no es un gate.
- **Medición no concluyente** (delta comparable al ruido del instrumento) ⇒ `INCONCLUSIVE`, que es un
  tercer veredicto y **no** un aprobado. Mismo principio que la cuantización de §3.2.

**Estado hoy: `NOT_RUN`.** Ninguna de las seis filas tiene número. Este host no tiene dispositivo, y un
Macrobenchmark en emulador no responde por el pacing real (orden-2 §F).

## 5. Matriz de dispositivos

El ADR §16 enumera los ejes (API 29/30/31/33/34+; 60 LCD/OLED, 90, 120 fijo, 120 LTPO/VRR, 144;
low/mid/flagship; battery-saver; frío vs soak; iOS 60 y 120 ProMotion). Tomado como factorial completo son
~300 celdas, que no es una matriz sino una forma elegante de no medir nunca. La reducción se justifica
separando factores **decisivos** (cambian lo que el código puede leer o el régimen físico del panel) de
factores **intra-dispositivo** (se miden en TODOS los dispositivos y no multiplican su número).

**Decisivos (definen cuántos dispositivos hacen falta):**
1. **Régimen de refresco** — 60 fijo · alto fijo (90/120) · **VRR/LTPO**. LTPO no es «120 con otro nombre»:
   es un comportamiento físico distinto (baja a 10-24 Hz en estático) y es el riesgo top-1 del ADR §18.
2. **Nivel de capacidad de API** — 29-30 (`MILLIS_DERIVED`) · 31+ (`DEADLINE`) · 34+ (`NANOS_NATIVE`).
3. **Gama de rendimiento** — el presupuesto de §4 debe pasar en la banda MÁS POBRE, no en el buque insignia.

**Intra-dispositivo (nunca añaden un dispositivo):** frío vs soak de 12 min, battery-saver on/off,
estático vs animado (el par que mide LTPO).

### 5.1 Conjunto mínimo defendible

| ID | Dispositivo | API | Panel | Gama | Por qué es irreemplazable |
|---|---|---|---|---|---|
| **D1** | Android gama baja | 29-30 | 60 Hz LCD | baja | La banda donde el presupuesto de §4 puede fallar, y la única con input `MILLIS_DERIVED` |
| **D2** | Android gama media | 31-33 | 90 o 120 **fijo** | media | Tier `DEADLINE`; refresco alto SIN el confound de VRR |
| **D3** | Android gama alta | 34+ | 120 **LTPO/VRR** | alta | `NANOS_NATIVE` + el riesgo top-1 (M3). Sin este dispositivo, M3 no existe |
| **D4** | Android gama media/baja | 34+ | 60 Hz OLED | media/baja | **Desconfunde API de hardware.** Sin D4, toda conclusión sobre `NANOS_NATIVE` sale de un flagship y no se puede distinguir «lo hace la API 34» de «lo hace el silicio caro» |
| **I1** | iPhone sin ProMotion | iOS 15+ | 60 Hz | — | M9 en el régimen simple |
| **I2** | iPhone ProMotion | iOS 15+ | 120 adaptativo | — | M9 donde el límite epistémico de `CADisplayLink` muerde |

**D4 es la fila que la matriz original no tenía y que decide si sus conclusiones son atribuibles.** Un
diseño con solo D1/D2/D3 confunde perfectamente nivel de API con gama de hardware: son la misma variable
en ese conjunto.

**Fuera del mínimo, declarado:** 144 Hz (raro fuera de handhelds de juego; su ausencia solo limita las
afirmaciones a paneles ≤120 Hz) y cualquier banda API 26-28 (**VOID**: `ADR-FOCUS-PLATFORM-MIN` fija el
mínimo canónico en **API 29**, así que pedir hardware para ella sería pedirlo para código que no existe).

### 5.2 Inventario que se pide al CEO

El CEO aporta **inventario**, no ejecución (ADR §16). Lo que se pide, con lo que se pierde si falta:

| Petición | Si falta |
|---|---|
| D1, D2, D3, D4 (propios o prestados, ~25 min de uso cada uno) | Sin **D1**: el presupuesto de §4 queda sin probar donde importa ⇒ B5 no cierra. Sin **D3**: M3 no se ejecuta y el riesgo top-1 sigue sin caracterizar. Sin **D4**: las conclusiones de API 34 quedan confundidas con la gama. Sin **D2**: no hay refresco alto sin VRR con el que separar los dos efectos |
| I1 e I2 + **acceso puntual a un Mac** | Sin Mac no hay ni CI de iOS ni Instruments ⇒ iOS entero sigue `NOT_EXECUTED`, y `MB-FOCUS-0065` no se puede confirmar ni refutar |
| Un segundo móvil con cámara ≥240 fps (960 ideal) | Sin él no hay Nivel A ⇒ **M1 no se mide en absoluto** y `validatedEndToEndErrorBound` sigue sin existir |
| (Opcional) ~50-100 EUR de banco fotodiodo + actuador | Sin él, `T2` sigue estructuralmente fuera de alcance. No bloquea nada de S5/S6 |

**Cobertura parcial ⇒ afirmaciones parciales, no un veredicto parcial.** Si llegan D1 y D3 pero no D4, el
protocolo se ejecuta y produce resultados válidos **para esas celdas**, y las conclusiones sobre las celdas
ausentes quedan `NOT_RUN` por su nombre. Lo que no se hace jamás es extrapolar de un dispositivo a una banda.

**Y el recordatorio que la fase entera necesita (orden-2 §F):** el emulador cubre corrección funcional,
flujo de exclusión y los tests JVM del calculador. **No** cubre pacing real, táctil, térmico, VRR ni GC en
hardware. **Un GO de emulador no es un GO.**

## 6. Paridad protocolo-código (mecánica, no prometida)

Los umbrales GO/NO-GO contra los que se mide en §3 y §4 **son** los que clasifican en producción. Si el
documento dice «drops ≤ 5%» y `TemporalQualityThresholds` dice otra cosa, la validación física se ejecuta
contra un instrumento que el producto no usa, y el resultado no vale para nada — sin que nada falle de
forma visible. Es la clase de divergencia que solo se descubre meses después.

Por eso los números viven en un bloque que **un test lee y compara contra la config versionada**
(`TemporalQualityThresholdsParityTest`, `desktopTest`). Cambiar un valor en el código sin actualizar aquí
—o al revés— pone el gate en rojo. La versión también está pinneada: estos números pertenecen a `tqt-1`, y
un bump de versión obliga a revisar el protocolo, que es exactamente lo que un cambio de umbrales debería
obligar.

<!-- TQT_PARITY_BLOCK_START -->
| key | value |
|---|---|
| version | tqt-1 |
| minFramesWindow | 3 |
| minFramesSession | 120 |
| windowDropCleanMax | 0.05 |
| windowDropExcludeMin | 0.15 |
| sessionExcludedMax | 0.5 |
| cadenceMatchTolerance | 0.05 |
<!-- TQT_PARITY_BLOCK_END -->

Lectura de esos números en el protocolo:

- `windowDropCleanMax` **0.05** y `windowDropExcludeMin` **0.15** son literalmente el GO/NO-GO de orden-2
  §C.6 («drops ≤5% limpio, 5-15% degradado»): la fila que sobrevivió a la reconciliación de §2.
- `cadenceMatchTolerance` **0.05** es la tolerancia con la que el paso 2 de §3.4 declara GO al comparar la
  cadencia contada en vídeo contra la reportada.
- `sessionExcludedMax` **0.5** es el backstop de sesión: no se mide con la cámara, pero **M6** existe para
  estimarlo con datos reales, y hasta entonces sigue `DESIGNED_NOT_RUN`.
- **Corrección del gate de salida (Paso 7, `MB-FOCUS-0054`):** este bloque pinneaba además
  `differentialExclusionDelta` **0.15** describiéndolo como «el detector de exclusión diferencial». Ese
  detector NO EXISTE — la R2 lo definió, la R3 lo retiró y ninguna fase posterior lo reconstruyó, así que la
  constante llevaba tres fases sin un solo consumidor. Pinnearla aquí convertía un hueco de diseño en una
  cifra con aspecto de umbral vigente, que es precisamente el error que este documento denuncia en §2.
  La constante se ELIMINÓ de `TemporalQualityThresholds` y de este bloque; vuelve cuando vuelva su detector,
  con su test. La política de missingness que la justificaría es `MB-FOCUS-0053`, todavía abierta.
- `minFramesSession` **120** es el piso que separa T0 de T1: una sesión que no lo alcanza no puede afirmar
  cadencia autoritativa, y el protocolo debe comprobar que las sesiones de referencia lo superan de sobra.

## 7. Qué autoriza cada resultado (y qué no autoriza ninguno)

### 7.1 Tabla de disposición

| Medición | Un PASS autoriza | Un FAIL fuerza |
|---|---|---|
| **M1** `D` | Registrar `validatedEndToEndErrorBound` como **uno** de los tres requisitos de `D-R3-03`. **No** habilita RT por sí solo | El RT sigue estructuralmente fuera de la lane de evidencia, sin fecha |
| **M2** cadencia | Confirmar que el modelo de dos hechos de Paso 5 describe la realidad | El `expectedCadenceHz` de Android es tan poco fiable como el de iOS ⇒ `null` en ambas plataformas |
| **M3** VRR/LTPO | Cerrar el riesgo top-1 del ADR §18 con datos | El probe atraviesa cambios de modo en silencio ⇒ toda clase T>0 es ciega en paneles adaptativos y hay que fijar la tasa (`Surface.setFrameRate`) antes de clasificar |
| **M4** térmico | Cuantificar la deriva y fijar la regla de exclusión con un número, no con una intuición | Los bloques largos quedan confounded y el tracer recorta duración o carga visual |
| **M5** efecto observador | **`§R3-19` → PASS**, que es la precondición de montaje que S6 necesita | `MB-FOCUS-0066` sigue OPEN, el montaje sigue bloqueado y el probe se rediseña |
| **M6** tasa `e` | Sustituir los valores `DESIGNED_NOT_RUN` de `tqt-1` por valores derivados de datos ⇒ `tqt-2` | Los umbrales siguen siendo una conjetura congelada, declarada como tal |
| **M7** input | Documentar que un timestamp en nanos no es exactitud | Igual, pero con evidencia de cuánto se equivoca |
| **M8** report-loss | Caracterizar el número por OEM | El campo no es interpretable cross-OEM y no se usa para nada comparativo |
| **M9** iOS | Abrir la discusión (en S6) de si iOS puede declarar cadencia esperada | `expectedCadenceHz = null` en iOS es **permanente**, no provisional |

### 7.2 Lo que ningún resultado de esta fase autoriza

- **Montar `focus2`.** El montaje es S6 y tiene su propia lista de precondiciones (ADR §19). Un M5 verde
  quita UN bloqueante, no abre la puerta.
- **Producir evidencia de medición.** `D-R3-02` sigue en pie: S5 no certifica accuracy ni RT, y ningún
  diagnóstico temporal produce evidencia. Un protocolo físico ejecutado no cambia esa decisión; cambiarla
  exige una decisión de producto registrada, no una medición.
- **Reclamar `T2`.** §3.2: hace falta un banco con actuador, que este protocolo no construye.
- **Comparar entre dispositivos.** El sesgo `D` no es igual entre modelos; la única comparación defendible
  sigue siendo intra-sujeto en el mismo dispositivo (orden-2 §A).

### 7.3 Riesgo de privacidad de la propia campaña — decisión declarada

**El protocolo recoge exactamente lo que la producción tiene prohibido persistir.** `§R3-17` minimiza
campo a campo: Hz **banded**, sin identificador de dispositivo, sin modelo, sin OEM. La campaña, en cambio,
necesita Hz exacto, modelo, OEM, estado térmico y firma de latencia — que juntos son una **huella de
dispositivo**. Si esos registros entraran en el almacén de la app, la minimización que Paso 5 y R3
sostienen quedaría deshecha por su propio instrumento de validación.

**Decisión (fail-closed, no negociable en la ejecución):**

1. Los datos de campaña viven **fuera del almacenamiento de la app**: no tocan `mindboost_focus2`, no crean
   `SessionDescriptive`, no pasan por `SecureKeyValueStore`. Salen del dispositivo por el canal del operador
   (fichero/ADB), no por un sink de la app.
2. **Ningún dato de campaña vuelve a entrar** en un registro de sesión, ni siquiera agregado: el resultado
   de la campaña se consume escribiendo umbrales en `TemporalQualityThresholds` (una constante versionada,
   revisable en diff), nunca cableando una tabla de dispositivos al runtime.
3. Los dispositivos son **del operador o prestados**, sin cuenta de usuario real y sin datos de usuario.
   Un dispositivo de un participante convertiría una calibración en una recogida de datos personales.
4. Si alguna vez se quisiera una calibración por dispositivo **en producción**, es una decisión de producto
   con su propio ADR y su propia DPIA. Esta fase la declara fuera de alcance en vez de habilitarla de facto.

## 8. Estado de ejecución (todo NOT_RUN, por su nombre)

| Medición | Estado | Bloqueado por |
|---|---|---|
| M1 `D` end-to-end | `NOT_RUN` | sin dispositivos y sin cámara ≥240 fps |
| M2 cadencia declarada vs presentada | `NOT_RUN` | ídem |
| M3 VRR/LTPO | `NOT_RUN` | sin D3 (panel LTPO) |
| M4 térmico | `NOT_RUN` | sin dispositivos |
| M5 efecto observador (**B5**) | `NOT_RUN` | sin dispositivo de gama baja (D1); emulador no vale |
| M6 tasa de exclusión `e` | `NOT_RUN` | exige sesiones reales, no solo hardware |
| M7 input | `NOT_RUN` | sin dispositivos |
| M8 report-loss por OEM | `NOT_RUN` | sin dispositivos de varios OEM |
| M9 paridad iOS | `NOT_RUN` | **y además** el actual iOS nunca ha compilado (CI macOS pendiente) |

**Veredicto de la fase: `CONDITIONAL`**, que es exactamente lo que el gate del plan prevé («sin hardware
=> CONDITIONAL»). Lo entregado es el instrumento y la petición de inventario. Lo NO entregado es cualquier
número físico, y no se disfraza de otra cosa.

---

**Custodia.** `REDTEAM_TEST_MATRIX_V5.md` está SELLADO en `SHA256_MANIFEST_V5.txt` y **no se edita**: las
filas #46 (banda API 26-28, VOID) y #47 («accuracy OFICIAL», falsa bajo `D-R3-02`) quedan superadas AQUÍ y
en el ledger, no en el fichero congelado — misma disciplina que Paso 7 aplicó al plan y al catálogo de gates.
El estado vigente se lee en `CURRENT_AUTHORITY_INDEX.md`.
