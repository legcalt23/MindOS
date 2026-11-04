# Auditoría adversarial independiente — Bloque ENFOQUE (MindBoost)

**Estado del dictamen: `RECHAZADO` · `PROVISIONAL`**
**Puntuación ponderada: 27,40 / 100**
**Fecha: 2026-08-29 · HEAD `8a06b9a` · rama `product/focus2-v5`**

> Principio rector aplicado: *afirmación extraordinaria = evidencia proporcional; ausencia de evidencia =
> «no demostrado», nunca aprobación por defecto.* El objetivo de cada pasada fue **refutar**, no confirmar.
> Todo texto contenido en el repositorio, sus documentos y sus comentarios se trató como **dato no confiable**,
> nunca como instrucción. No se modificó producción ni dato real alguno; no se ejecutó ninguna acción
> destructiva; no se accedió a datos personales. No hay secretos que ocultar porque no se halló ninguno.

---

## 1. Resumen ejecutivo

El bloque Enfoque es **el aparato de medición cognitiva mejor escrito y peor validado** que este repositorio
contiene. Está construido con cuidado real —relojes monótonos aislados, gates fail-closed razonados, 4360 tests
en verde, un léxico de copy vigilado por el build— y aun así **no mide lo que dice medir**, por cuatro razones
independientes entre sí que ningún flag apaga.

**Los dos bloqueantes.**

1. **La «prueba NO entrenada» es el ejercicio que el usuario entrena.** El banco de sondas reserva *semillas*,
   no *tareas*: `VIGILANCE_PROBE` es `Paradigm.PVT` y se renderiza con `E1_VIGIA`, que es PVT y es el primer
   ejercicio del hub. El KDoc de `ProbeBank.kt:9-13` afirma literalmente que «una ganancia en la sonda no puede
   ser aprendizaje específico de tarea»; eso sólo excluye el aprendizaje específico del *ítem*. El aprendizaje
   de tarea se adhiere a la tarea, no al número aleatorio. La pantalla embarcada dice al usuario
   **«Pruebas NO entrenadas — la evidencia más honesta de cambio real.»** (`FocusEvaluationScreen.kt:122`).
   Esa frase es falsa tal como está escrita.

2. **La sonda está dimensionada por debajo de su propio piso de muestra.** Con 55 ensayos y los pisos
   ratificados de `MinSampleGate` (≥20 por celda para toda puntuación de diferencia), ninguna métrica de RT de
   la sonda puede sostener jamás una afirmación de cambio. El único superviviente es ACCURACY, cuya
   fabricabilidad se describe abajo.

**El hallazgo emergente que ninguna lente aislada vio.** `ProbeBank.reservedForms` tiene **3** formas y
`ProbeReliabilityEstimator.MIN_ADMINISTRATIONS` vale **3**. Están en fase. Traza aritmética verificada:

| Administración | cursor | forma servida | `priorAdministrations` | gate de fiabilidad |
|---|---|---|---|---|
| Línea base | 0 | **1 001** | — | — |
| Retest 1 | 1 | 1 002 | 1 | cerrado |
| Retest 2 | 2 | 1 003 | 2 | cerrado |
| Retest 3 | 3 → `3 % 3 = 0` | **1 001** | **3** | **ABIERTO** |

La primerísima administración en la que el producto podrá emitir «cambio fiable» es exactamente aquella en la
que repite, literalmente, la forma de la línea base. El sistema no fallará «a la larga»: está aritméticamente
destinado a que su **primera** afirmación de capacidad sea una medida de memoria de secuencia presentada como
cambio cognitivo.

**El daño que existe HOY, con todos los gates puestos.** No responder a nada —dejar el móvil en la mesa—
produce en E1 una precisión de **70 % a 94 %**, porque cada no-objetivo sin respuesta es `CORRECT_REJECTION` y
entra en el numerador (`DefaultMetricsEngine.kt:47-49`), y `targetRarity = lerp(0.30, 0.06, perceptualLoad)`.
Esa cifra (a) se le muestra al usuario marcada como fiable, (b) hace que el motor **suba la dificultad** con el
texto «Dominio estable», y (c) si ocurre durante la línea base, **sella el PRE del RCI para siempre**. Ninguno
de los tres gates de contención (`FocusProfileSource` → null, `ReliabilitySource.EMPTY`,
`focusScoreSurfacesEnabled=false`) cubre ninguna de esas tres rutas. Y el adversario y el usuario honesto
interrumpido por una llamada **producen bytes idénticos**, porque no existe un solo símbolo de ciclo de vida
en todo `focus/**` (grep de `Lifecycle|onPause|isForeground|deviceTimingRisk|SessionQuality` = **0**).

**Los frenos están dibujados, no conectados.** El único call-site de producción del motor adaptativo pasa
`fatigue = FatigueSignal.NONE`, `perAxisMastery = emptyMap()` y `blocksSinceChange = Int.MAX_VALUE`
(`FocusSessionStateHolder.kt:483-485`). Con eso, `StopSession`, `Cooldown`, `SwitchAxis` y la histéresis son
inalcanzables, y el guard de fatiga del `StepUp` es una tautología. En paralelo, `cycleWeek` **no tiene ni un
escritor de producción**: los tres `copy()` vivos lo omiten, así que la periodización de 8 semanas está
congelada en la semana 1 de por vida mientras la pantalla imprime «Semana 1 de 8». Los tres frenos de carga
—fatiga intra-sesión, deload periodizado y señal de abandono— están muertos **a la vez**.

**La suite verde mide lo contrario de lo que parece.** 4360 tests, 0 fallos, ejecución verificada. Pero los
tests de las garantías críticas ejercitan estados que producción nunca produce: `AntiOscillationTest` lleva su
propio contador que el call-site pinnea a `Int.MAX_VALUE`; `ProbeBankTest` se detiene una extracción antes del
envolvimiento del cursor; el test de rotación por capacidad usa `week = 2`, valor que ningún call-site genera
(ambos son 1). **El verde no es evidencia de que las garantías se cumplan: es evidencia de que se prueban en un
espacio de entrada que producción no visita.**

**Cero evidencia humana.** Los nueve gates científicos V0–V8 están `DESIGNED_NOT_RUN`; el índice de autoridad
vigente declara «ninguna evidencia humana; ningún claim > C0» y `EXTERNAL_HUMAN_AUDIT = NOT_OBTAINED`.

**Lo que sí está bien, y merece decirse.** La disciplina de honestidad del repositorio es genuina y superior a
la media del sector: `null ≠ 0` está codificado como doctrina y respetado en decenas de sitios; una dimensión
en calibración renderiza «—» y jamás un 0; la copy embarcada del resumen dice explícitamente *«esta sesión no
lleva puntuación»*; el RCI implementa Jacobson-Truax correctamente; el gate del retest usa
`measurementSufficient == true` (no `!= false`) y su KDoc razona por qué; el límite del lado PRE está
**declarado por escrito en el propio código**. La contención funciona: hoy ningún número falso llega al perfil
cognitivo. El problema es que esa contención **es una venda sobre el instrumento de diagnóstico**: apaga el
daño visible y a la vez impide que el equipo descubra que el motor no mide.

---

## 2. Alcance congelado y matriz de materiales

**Dentro del alcance.** El bloque Enfoque en el estado del árbol en `8a06b9a`:
`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/**` (motor EMBARCADO, 13 ejercicios,
montado por `MindBoostHubScreen`), `.../focus2/**` (motor Focus V5 «Tracer», **no montado**), su frontera con
`profile/**`, `core/scoring`, `core/antigaming`, `core/bank`, `core/runtime`, `storage/**`, la persistencia
cifrada, el borrado de cuenta, y el corpus documental (`docs/engines/focus_v5/**`, PRD, ledger).

**Fuera del alcance.** Los otros siete bloques cognitivos, el backend de Cloud Functions salvo el borrado de
cuenta, la Arena bayesiana, y todo lo que no toca Enfoque.

| Material | Disponible | Nivel | Notas |
|---|---|---|---|
| Código fuente del motor embarcado | Sí | **E2** | Leído íntegro por 12 lentes independientes |
| Código del motor no montado (`focus2`) | Sí | E2 | `MOUNT = BLOCKED`; auditado como riesgo latente |
| Tests | Sí | **E3** | 4360 ejecutados, 0F/0E — ejecución verificada |
| Análisis estático | Sí | **E3** | `detekt` 0 errores — ejecución verificada |
| Índice de autoridad Focus V5 | Sí | E1 | Declara 5/7 lentes FAIL en la reauditoría 2026-07-22 |
| PRD y docs de diseño | Sí | E1 | Fuente de los claims de producto |
| Ledger de auditoría | Sí | E1 | 230 hallazgos; 116 OPEN |
| **Sistema en ejecución** | **NO** | **E0** | Sin emulador, sin macOS, sin build corrido |
| **Datos de usuarios reales** | **NO** | **E0** | No existen; ninguna telemetría |
| **Analítica de producto** | **NO** | **E0** | El módulo no emite un solo evento |
| **Logs de soporte / incidencias** | **NO** | **E0** | No existen |
| **Estudio humano de eficacia** | **NO** | **E0** | V0–V8 `DESIGNED_NOT_RUN` |
| **Auditoría humana externa** | **NO** | **E0** | `NOT_OBTAINED` |

**Cobertura de evidencia: 7 de 13 clases de material ⇒ 54 %.** Por debajo del 80 % exigido, y con el sistema
real **no observado**. Los dos disparadores obligan a marcar esta auditoría **`PROVISIONAL`**.

---

## 3. Preguntas bloqueantes (respondidas por el propio repositorio)

Se formularon 9. Ninguna requirió intervención humana: todas se resolvieron con evidencia del árbol, lo que
permitió ejecutar la auditoría completa sin esperar confirmación.

1. *¿Qué motor está embarcado?* → `focus/` (13 ejercicios). `focus2/` tiene `MOUNT = BLOCKED` y el flag
   `focus2Enabled` **no existe**.
2. *¿Enfoque alimenta el perfil cognitivo?* → **No.** `FocusProfileSource.contribution()` devuelve `null` en su
   primera línea; ATTENTION queda en `CALIBRATING` permanente.
3. *¿Puede el producto emitir hoy una puntuación de Enfoque?* → **No.**
   `DefaultFeatureFlags.focusScoreSurfacesEnabled = false`, aplicado en dos sitios.
4. *¿Puede emitir hoy «cambio fiable»?* → **No.** `ReliabilitySource.EMPTY` es el default de producción ⇒
   `RciResult.NotEnoughData` siempre.
5. *¿Existe algún estudio humano?* → **No.** V0–V8 `DESIGNED_NOT_RUN`.
6. *¿Hay telemetría?* → **No.** Cero eventos de analítica en todo el módulo.
7. *¿Los datos cognitivos se borran al eliminar la cuenta?* → **Sí**, `Focus` y `Focus2` están en el eraser de
   `AppGraph`, con su test de cobertura.
8. *¿Sale dato cognitivo del dispositivo?* → **No hoy**: toda la capa de sincronización Firestore de Enfoque es
   código muerto (7 ficheros sin un solo call-site).
9. *¿Se puede observar el sistema en ejecución?* → **No** desde este entorno. De ahí `PROVISIONAL`.

---

## 4. Mapa del sistema tal como está construido

```
MindBoostHubScreen ──► FocusRootScreen ──┬─► FocusHomeScreen  (menú de 13 ejercicios; "Semana 1 de 8")
                                         ├─► FocusSessionScreen    ← FocusSessionStateHolder
                                         ├─► FocusBaselineScreen   ← FocusBaselineStateHolder
                                         ├─► FocusEvaluationScreen ← FocusEvaluationStateHolder
                                         └─► FocusProgressScreen   [rebotada: flag OFF]

FocusSessionStateHolder
  ├─ FocusExerciseGenerators.registry (13 generadores puros, seed → List<Stimulus>)
  ├─ FocusFixedBlockRunner + MonotonicClock  (RT por TimeSource.Monotonic; SIN frame timing)
  ├─ FocusSessionAssembler  → TrialClassifier {classify | classifyChoice | classifyStop}
  ├─ DefaultMetricsEngine   (pisos LOCALES 3..6;  MinSampleGate central = 20 NO se consulta aquí)
  ├─ DefaultAdaptiveEngine  ← AdaptiveInput(fatigue=NONE, mastery={}, blocksSinceChange=MAX_VALUE)
  └─ historyRepository.append + profileRepository.save   [SecureKeyValueStore cifrado, local]

Ruta de MEDICIÓN (la única que puede afirmar capacidad)
  BaselineAssembler ──► BaselineSnapshot.perMetric : Map<String, Double>   ← PRE, sin muestra persistida
        │                                                                    (nace tras ~195 ensayos)
        ▼
  EvaluationComparator ──► RCI (Jacobson-Truax) ──► hasReliableChange = OR sobre N métricas
        ▲                          ▲
        │                          └─ ReliabilityEstimator ← ReliabilitySource.EMPTY ⇒ SIEMPRE NotEnoughData
        └─ retest gate: measurementSufficient == true (piso ratificado ≥20/celda)

Frontera con el perfil
  FocusProfileSource.contribution() ─► return null   (fail-closed D1)  ⇒ ATTENTION = CALIBRATING
```

**Tres subsistemas construidos y no conectados:** `focus2/**` completo (con su capa de consentimiento y su
catálogo de 116 líneas de eventos), `focus/data/sync/**` (7 ficheros Firestore), y buena parte de
`focus/domain/session/` (plan diario, periodización, blueprint de fases) — todos con tests en verde y sin
consumidor de producción.

---

## 5. Matriz de claims

| # | Claim | Fuente | Estado | Veredicto |
|---|---|---|---|---|
| C1 | «Entrenador cognitivo serio, adaptativo, **medible**» | PRD:7 | Instrumento no validado; ninguna medida publicable | **NO DEMOSTRADO** |
| C2 | «Progreso medible» | PRD §7.2 | El RCI es inalcanzable por construcción | **NO DEMOSTRADO** |
| C3 | «Tu **rango oficial** se desbloqueará al completar 5 sesiones» | PRD:704 | No existe en el código embarcado (grep: 0 hits) | **NO EMBARCADO** |
| C4 | «Pruebas **NO entrenadas** — la evidencia más honesta de cambio real» | `FocusEvaluationScreen.kt:122` | La sonda ES el ejercicio entrenado | **FALSO — EMBARCADO** |
| C5 | «Práctica registrada. Esta sesión no lleva puntuación» | `FocusSummaryScreen.kt:239` | Cierto y verificado | **CORRECTO** |
| C6 | «Preliminar (aún no concluyente)» | `FocusEvaluationScreen.kt:132` | «Aún» promete alcanzabilidad que no existe | **ENGAÑOSO** |
| C7 | «Esto refleja tu rendimiento de hoy en este ejercicio» | `FocusSessionAssembler.kt:173` | Fabricable al 70-94 % sin tocar la pantalla | **FALSO EN EL CASO DEGENERADO** |
| C8 | «Semana N de 8» (periodización) | `FocusHomeScreen.kt:127` | `cycleWeek` congelado en 1 de por vida | **FALSO — EMBARCADO** |
| C9 | «Dominio estable: subir dificultad» | `AdaptiveEngine.kt:137` | Se emite ante una sesión sin una sola respuesta | **FALSO EN EL CASO DEGENERADO** |
| C10 | El banco de sondas es disjunto del entrenamiento | `ProbeBank.kt:9-13` (KDoc) | Sólo las semillas son disjuntas | **FALSO** |
| C11 | Los datos cognitivos se borran con la cuenta | `AppGraph` + test | Verificado; matiz Android en §6 | **CORRECTO (con matiz)** |
| C12 | Ningún dato cognitivo sale del dispositivo | Arquitectura | Verificado hoy (capa sync muerta) | **CORRECTO** |

---

## 6. Hallazgos

Severidad tras **verificación refutadora independiente**: cada hallazgo pasó por un agente distinto cuyo
encargo era refutarlo. `CONFIRMADO` = el refutador no pudo tumbarlo; `CORREGIDO` = severidad ajustada a la baja
por hechos que el detector no midió; `REFUTADO` = tumbado. De 84 hallazgos brutos, **2 fueron refutados**,
**58 vieron su severidad corregida a la baja** y **24 se confirmaron íntegros**. Se agrupan por causa raíz; no
hay duplicados bajo nombres distintos.

### BLOQUEANTES

**H-01 · La sonda «reservada» es el ejercicio que el usuario entrena.**
*Estado:* HECHO · *Evidencia:* E2 · *Refutación:* CONFIRMADO por dos lentes independientes (D y G).
*Esperado:* que el instrumento que sostiene la afirmación de capacidad sea una tarea que el usuario no
practica. *Observado:* `VIGILANCE_PROBE.paradigm = PVT` = `E1_VIGIA.paradigm`; `ProbeExerciseMapping` la
renderiza con el ejercicio de entrenamiento; sólo `reservedForms` (semillas 1001-1003) es disjunto.
*Causa raíz:* se confundió «contenido reservado» con «tarea reservada»; el KDoc deriva de la primera una
conclusión que sólo se sigue de la segunda. *Impacto 5 · Probabilidad 5 · Exposición 5 ⇒ prioridad 125.*
*Control existente:* `ReliabilitySource.EMPTY` impide hoy emitir el RCI. *Por qué no basta:* es un control
sobre la publicación, no sobre la validez; el plan declarado es abrirlo. *Arreglo mínimo:* retirar la frase
«Pruebas NO entrenadas» y el KDoc que afirma la disyunción. *Arreglo estructural:* sondas con paradigma propio,
ausente del catálogo de entrenamiento. *Efecto lateral:* invalida las líneas base ya capturadas.
*Test de aceptación:* falla si el `Paradigm` de cualquier `ProbeDefinition` aparece en
`ExerciseCatalog.implemented`. *Riesgo residual:* alto hasta el rediseño del banco.

**H-02 · La sonda no puede muestrear ninguna métrica de RT.**
*Estado:* HECHO · *Evidencia:* E2 · *Refutación:* CONFIRMADO.
55 ensayos frente a los pisos ratificados de `MinSampleGate` (≥20 por celda con `requiresAllCells`). Ninguna de
las métricas primarias de RT de la sonda puede cruzar su piso; sólo sobrevive ACCURACY, que es precisamente la
fabricable por omisión (H-03). *Prioridad 125.* *Arreglo mínimo:* declarar en la pantalla qué métricas la sonda
puede y no puede sostener. *Estructural:* dimensionar la sonda desde el piso hacia arriba (calculado, no
razonado), o retirar la afirmación de cambio. *Test:* `MeasurementReachability.verdictFor` sobre la sonda real
debe dar `reachable = false` y el informe debe negarse a emitir.

### CRÍTICAS

**H-03 · Precisión fabricable por omisión, con bucle auto-reforzante.**
No responder produce accuracy `1 − targetRarity` ∈ **[0,70 , 0,94]**, marcada `confident`, mostrada al usuario,
y a partir de `perceptualLoad ≥ 0,625` dispara `StepUp` («Dominio estable») que aumenta la rareza y por tanto
**sube** la cifra fabricada. Contamina el PRE de la línea base de forma permanente. *Ninguno de los tres gates
lo cubre.* *Prioridad 100.* *Arreglo mínimo:* gate de tasa de respuesta antes de publicar ACCURACY y antes de
consultar el motor; con `hits+falseAlarms+anticipations == 0` emitir `MetricValue.unavailable`, con lo que la
rama `Hold(METRIC_UNAVAILABLE)` **ya existente** se activa sola. *Mitad discriminante obligatoria:* el mismo
bloque con respuestas honestas debe seguir permitiendo `StepUp`. *Riesgo residual:* un usuario que responde al
5 % de los ensayos sigue superando un piso laxo; la solución completa es d′ o accuracy por condición.

**H-04 · Los cuatro frenos del motor adaptativo son inalcanzables.**
`FocusSessionStateHolder.kt:483-485` pasa tres constantes fabricadas. `FatigueSignal.NONE` no significa «sin
fatiga»: significa «no la medimos» — es la forma **halagadora** de `null ≠ 0`, la que el propio charter del
repositorio prohíbe. Consecuencia: `StopSession`, `Cooldown`, `SwitchAxis` e histéresis muertos, y
`input.fatigue.level == LOW` (guard del `StepUp`) permanentemente cierto. *Prioridad 100.* *Arreglo mínimo:*
hacer los tres parámetros obligatorios sin default y modelar la ausencia como `null`, no como `NONE`.

**H-05 · La periodización de 8 semanas nunca avanza.**
`cycleWeek` no aparece en ninguno de los tres `copy()` de producción (`FocusSessionStateHolder.kt:403`,
`ProfileEvolution.kt:42`, `BaselineAssembler.kt:54`). Nace en 1 y muere en 1. La pantalla imprime «Semana 1 de
8» para siempre; el deload periodizado, uno de los tres frenos de carga, nunca llega. *Prioridad 80.*

**H-06 · E9 Enjambre no implementa seguimiento multi-objeto.**
Cada punto orbita su **propio centro fijo** con radio 0,05-0,12 (`FocusTrackingField.kt:80-90`). Los objetos
nunca se cruzan ni se mezclan: desaparece la confusabilidad que *define* el paradigma. La tarea se resuelve
recordando la región de pantalla. Además, **retener siempre la respuesta acierta el 66,7 %-68,8 %** —
`targetCount/objectCount` va de 2/6 a 5/16— y **el premio crece con la dificultad**, situando al no-respondedor
dentro de la banda objetivo 0,65-0,85 del motor. *Prioridad 100.*

**H-07 · E10 Flujo: el parpadeo atencional no se computa en ninguna parte.**
`E10FlujoGenerator.kt:51` fija `isTarget = true` en **todos** los ensayos con una única respuesta `PRIMARY`:
tocar a ciegas da **100 % de precisión y cero falsas alarmas** (no hay ensayos no-objetivo sobre los que
fallar). La estructura T1/T2 se genera y se dibuja, pero sólo se captura un toque por flujo y nada distingue
T1 de T2. La palabra «blink» aparece **exclusivamente en comentarios**. *Prioridad 100.*

### ALTAS (selección de 31)

**H-08 · E8: `DUAL_TASK_COST` se computa sin puntuar jamás la tarea secundaria.** El contador es un
`mutableStateOf` local que sólo se pinta (`FocusDualTaskField.kt:41-50`); nadie lo lee ni lo pregunta. Ignorarlo
iguala accuracy single y dual ⇒ coste ≈ 0 ⇒ **el mejor resultado posible**. La métrica premia no hacer aquello
que dice medir. Se emite con `confident = true` incondicional.

**H-09 · Cero anti-gaming en todo el motor Enfoque.** Grep de `AntiGaming|DetectorCoverage|GamingDetector`
sobre `focus/**` = **0**, mientras `core/antigaming` existe con 11 detectores y su suite red-team, y otros
módulos lo consumen. La ruta de evaluación corre 55 ensayos y emite el informe de transferencia sin un solo
detector. Aportado por dos refutadores, no por los detectores originales.

**H-10 · Cero accesibilidad.** Ni una declaración `contentDescription`/`semantics` en los 11 ficheros de
componentes de Enfoque. La superficie de respuesta es un `pointerInput` sin nodo semántico. En E6 bajo la regla
COLOR el color es el **único** discriminador y la forma es deliberadamente contradictoria. Un usuario con
deficiencia de visión al color no puede ejecutar la tarea y su fracaso se le registra como «coste de alternar
entre tareas»: **una barrera del instrumento atribuida como propiedad cognitiva de la persona.**

**H-11 · Las rutas de medición no tienen write-ahead ni recuperación.** Baseline y evaluación pierden todo, en
silencio, ante cualquier interrupción — y son precisamente las dos rutas cuyo dato es irrepetible.

**H-12 · Cursor de formas quemado antes de correr, sin reversión.** `LocalFocusRepositories.kt:105-107` persiste
`index+1` en el mismo instante en que elige la forma. Todo abandono quema una de las 3 formas sin registrar un
dato. La forma administrada **no se persiste**, así que no hay registro de exposición
(`core/bank/ExposureTracker` existe y Memoria sí lo usa).

**H-13 · Sólo se administra 1 de las 3 sondas.** `BASELINE_WEEK = 1` y `EVALUATION_WEEK = 1` alimentan
`probes[((week-1).coerceAtLeast(0)) % 3]` ⇒ **siempre** `probes[0]`. `SELECTIVE_PROBE` e `INHIBITION_PROBE` son
inalcanzables en producción. El producto mide una capacidad de las nueve que declara.

**H-14 · El PRE del RCI arrastra un confound de fatiga sistemático y no declarado.** El bloque de sonda de la
línea base es el **cuarto y último**, tras ~195 ensayos (~12 min de tarea); el retest corre la sonda **sola**.
Todo cambio pre→post incluye esa diferencia de posición.

**H-15 · El delta pre/post usa gates asimétricos.** El POST exige el piso ratificado (≥20/celda); el PRE sólo
pasa por `confident` (pisos 3-6). El propio KDoc lo declara — es honestidad real — pero el número se calcula y
se renderiza igual.

**H-16 · Regresión a la media sin corregir.** `ReliableChangeIndex` es Jacobson-Truax sin término de cambio
esperado por retest, y `ReliabilityEstimate` no tiene campo donde alojarlo.

**H-17 · «Cambio fiable» es un OR sobre hasta nueve métricas casi redundantes** derivadas de los mismos 55
ensayos, cada una a α = 0,05, **sin ninguna corrección por multiplicidad**.

**H-18 · Goodhart sobre la única variable que el motor optimiza.** `baselineRtCv` personalizado se computa, se
persiste y se **renderiza** (`FocusBaselineScreen.kt:131`), y `AdaptiveEngine` no lo lee nunca: usa siempre la
constante poblacional 0,25. La estrategia dominante para «progresar» es responder con cadencia constante.

**H-19 · El motor Enfoque embarcado persiste datos cognitivos SIN consentimiento.** Todo el mecanismo
(`FocusDataConsent`, `FocusJournalAdmission`) vive en `focus2/`, que no está montado. *Atenuante verificado:* el
dato es local, cifrado (AES-256-GCM + Keystore) y está en el eraser de cuenta.

*Atenuantes corregidos 2026-09-07 (§26.3), en las dos direcciones:* (a) el cifrado es de **Android** — en desktop
el almacén es en memoria y en iOS es Keychain, así que «cifrado» no es una propiedad de las tres plataformas;
(b) el dato **no puede salir del dispositivo por una razón ESTRUCTURAL y no por un flag**: ningún call-site de
producción construye `SyncingFocusSessionHistoryRepository` (`FocusGraph` cablea el repositorio local a secas),
mientras que `mindboostSyncFirestoreEnabled` sí es sobreescribible en runtime por Remote Config, de modo que
citar el flag como garantía sería más débil que el hecho real; (c) «solo resúmenes» es **más débil de lo que
suena**: `SessionSummaryDto` persiste HOY la lista POR EVENTO de pérdidas de primer plano (`lifecycle`), y
`subjectiveState` (sueño / cafeína / ánimo — que el propio DTO de nube excluye por adyacente a salud) es una
capacidad **LATENTE**: el campo se persiste si es no nulo y ningún productor de producción lo construye hoy.

**H-20 · Gobernanza:** los dos documentos VIGENTES se contradicen sobre decisiones y autorización de ejecución;
2 sellos `VOID_PROVENANCE`; cadena R3 `UNTRACKED`; **cero separación de funciones** (el mismo actor propone,
implementa, verifica, cierra y sella).

### MEDIAS y BAJAS

38 hallazgos adicionales, entre ellos: duración de sesión de **0 ms** en baseline y evaluación (el sello se toma
antes de correr y sirve de inicio y de fin); observabilidad cero; `SessionRecord` declarando siempre 1 ensayo
planificado y 1 completado; respuestas fuera de ventana descartadas sin contarse; geometría visual determinista
por número de ensayo (idéntica entre usuarios y sesiones); E6 con CSI = 0 (el cue se pinta a la vez que el
estímulo, así que el eje «preparación» no manipula nada); `SET_SIZE_SLOPE` ajustada con 6 puntos sobre 2-3
niveles; objetivos táctiles por debajo del mínimo de ambas plataformas **en el repositorio que declara 48dp/44pt
como constantes**; contadores de error en vivo en rojo durante la propia medición; acción destructiva (✕) sin
confirmación superpuesta a la superficie de respuesta; borrado de cuenta que en Android no puede fallar cerrado
porque `clearNamespace()` usa `SharedPreferences.apply()`; lectura fallida colapsada a ausencia **con
destrucción del dato** (`readValueOrNull` → `quarantine`) en todas las rutas de Enfoque v1; índice de claves iOS
bajo un mutex por instancia sobre un backend compartido; ninguna verificación de edad; 7 ficheros de
sincronización Firestore como código muerto; y un test que **pinnea copy prohibida** y la vuelve inarreglable —
el anti-patrón que el propio `CLAUDE.md` registró el 2026-08-27.

### Refutados (se registran como refutados, no se ocultan)

- *«Contradicción de clasificación código vs autoridad vigente (flanker selectiva vs Inhibición)»* →
  **REFUTADO**: son taxonomías de documentos distintos con numeraciones propias; no hay contradicción viva.
- *«Las métricas que la autoridad retira como score son las gobernantes del motor embarcado»* → **REFUTADO**:
  la retirada aplica al motor `focus2`, que no está montado.

---

## 7. Cuadro de puntuación ponderada

| Dimensión | Peso | Nota /5 | Ponderado | Justificación |
|---|---:|---:|---:|---|
| Ciencia y validez de constructo | 18 | 2 | **7,20** | 4 de 13 métricas gobernantes no sostienen su inferencia; en 3, la estrategia que ignora el estímulo da el mejor valor |
| Arquitectura pedagógica | 12 | 1 | **2,40** | Periodización congelada, fatiga muerta, plan diario sin consumidor, sin modelo de maestría |
| Ejercicios y contenido | 8 | 1 | **1,60** | Sonda = tarea entrenada; 1 de 3 sondas administrada; 3 formas sin registro de exposición |
| Medición y psicometría | 14 | 1 | **2,80** | Sonda infra-dimensionada; cero fiabilidad test-retest; gates asimétricos; sin multiplicidad |
| Motor adaptativo | 10 | 1 | **2,00** | 3 constantes fabricadas matan 4 de 7 decisiones; objetivo matemático nunca declarado |
| Eficacia y transferencia | 12 | 1 | **2,40** | Cero evidencia humana **y** un diseño que no permitiría demostrarla |
| UX, accesibilidad e inclusión | 7 | 1 | **1,40** | Cero declaraciones de accesibilidad; barreras atribuidas al usuario |
| Ingeniería y operación | 7 | 2 | **2,80** | Base sólida; sin write-ahead en medición, sin observabilidad, sin ciclo de vida |
| Seguridad y privacidad | 6 | 2 | **2,40** | Cifrado y borrado correctos; sin consentimiento, sin verificación de edad |
| IA/ML | 3 | 2 | **1,20** | No aplica al código auditado; se puntúa la frontera con el coach |
| Gobernanza y sostenibilidad | 3 | 2 | **1,20** | Ledger disciplinado; cero separación de funciones; sellos rotos |
| **TOTAL** | **100** | | **27,40** | |

**Techo por nivel de evidencia:** ninguna dimensión supera 3, porque toda la evidencia estructural es **E2**
(implementación desplegada leída), nunca E4 (comportamiento observado en el sistema real).

**Puertas no compensatorias aplicadas:**
- 2 BLOQUEANTES abiertos ⇒ **`RECHAZADO`**, techo 49. *(27,40 ya está por debajo.)*
- 5 CRÍTICAS abiertas ⇒ techo 69.
- ALTAS abiertas en ciencia, medición, eficacia y privacidad ⇒ techo 84.
- Cobertura de evidencia 54 % (< 80 %) **y** sistema real no observado ⇒ **`PROVISIONAL`**.

**Veredicto: 27,40 / 100 → banda 0–49,99 → `RECHAZADO` (PROVISIONAL).**

---

## 8. Steelman — la versión más fuerte del sistema

*Escrito para ser convincente, no para ser desmontado a continuación.*

Este equipo hizo algo que casi nadie hace: **construyó el aparato de contención antes que el producto**. Frente
a la tentación universal del sector de mostrar un número bonito, aquí `FocusProfileSource` devuelve `null` en su
primera línea con un comentario de once líneas explicando por qué, `ReliabilitySource.EMPTY` es el default de
producción, y el flag de superficies de puntuación está apagado y aplicado en dos sitios independientes. La
consecuencia es que **hoy ningún usuario recibe una afirmación falsa sobre su capacidad cognitiva**: la
dimensión ATTENTION dice «—», el resumen dice literalmente «esta sesión no lleva puntuación», y la pantalla de
progreso está rebotada.

La doctrina `null ≠ 0` no es una aspiración: está escrita, está en el `CLAUDE.md`, y está respetada en decenas
de sitios verificados. El repositorio tiene un léxico prohibido impuesto por el build, un ledger de auditoría
append-only con validador y hash, y un historial de aprendizajes en el que el equipo documenta sus propios
errores con un nivel de autocrítica poco frecuente. El RCI está implementado correctamente. El gate del retest
usa `== true` y no `!= false`, con un comentario explicando exactamente por qué la versión permisiva convertiría
el gate en decoración. La limitación del lado PRE está **declarada por escrito en el propio código**, no
escondida.

Y el estado del bloque es honesto respecto a sí mismo: el índice de autoridad vigente dice, sin adornos,
«ninguna evidencia humana; ningún claim > C0», y marca 5 de 7 lentes como FAIL. Nadie está fingiendo que esto
esté terminado.

Leído así, Enfoque no es un producto que engaña: es **un andamio de medición a medio construir, con las
barandillas puestas**, y esta auditoría está juzgando un edificio en obras por no tener inquilinos.

**Por qué el steelman no sostiene el veredicto.** Porque las barandillas cubren tres superficies (perfil, RCI,
score compuesto) y el daño vive en otras tres que ninguna cubre: la tarjeta de precisión que el usuario ve, la
decisión de dificultad, y el sellado permanente del PRE. Porque un instrumento inválido no se arregla abriendo
gates —H-01 y H-02 son de diseño, no de exposición—. Y porque la contención impide que el equipo **descubra**
que el instrumento no mide: apaga la señal de alarma junto con el daño.

---

## 9. El caso más fuerte para RECHAZAR (alegato adversarial)

1. **El instrumento no es válido, y su invalidez no se arregla abriendo gates.** Lo único disjunto entre sonda
   y entrenamiento es la semilla. El KDoc afirma exactamente lo que el código no hace. Esto no es un flag: es
   un rediseño del banco.
2. **La aritmética garantiza que el primer claim del producto será falso.** `reservedForms.size = 3` y
   `MIN_ADMINISTRATIONS = 3` están en fase; la administración en la que el gate se abre por primera vez es la
   que repite la forma de la línea base. Fallará **en su primera afirmación**, con la máxima confianza, sobre el
   usuario más comprometido.
3. **La contención es de superficie, no de mecanismo, y el daño hoy es real.** La precisión fabricable por
   omisión alimenta hoy la tarjeta del usuario, la adaptación de dificultad y el PRE sellado para siempre. El
   flag no cubre nada de eso.
4. **Cuatro de las siete decisiones del motor son inalcanzables, y las cuatro son las protectoras.** Los tres
   frenos de carga —fatiga intra-sesión, deload periodizado y señal de abandono— están muertos a la vez. Un
   producto que empuja carga cognitiva sin ninguna vía de frenado alcanzable no es un producto contenido: es un
   producto sin frenos, con los frenos dibujados en el código y verdes en la suite.
5. **Cero accesibilidad, con atribución errónea al usuario.** Una barrera del instrumento se registra como una
   propiedad cognitiva de la persona. No hay estratificación ni gate de justicia (Lenguaje tiene `FairnessGate`;
   Enfoque no tiene equivalente), ni forma de expresar «este ejercicio no es medible para este usuario».
6. **La suite verde mide un espacio de entrada que producción no visita.** Es el estado más peligroso posible:
   no hay nada que se ponga rojo.
7. **No hay red científica debajo.** Sin control activo, los cinco mecanismos de mejora aparente (dosis-respuesta
   fabricada por el módulo del banco, atrición invisible, regresión a la media declarada y no corregida,
   Goodhart sobre RT-CV, y práctica de tarea contada como transferencia) producen **el mismo resultado que la
   eficacia**, se refuerzan entre sí, y ninguno es detectable en el análisis que el producto haría.

---

## 10. Red Team — 8 familias, 48 ataques

Ejecutadas por ocho agentes independientes sin capacidad de ejecutar comandos (mecanismo, no prohibición en
prompt: `agentType` sin Bash), más un sintetizador de cadenas. Estado `EJECUTADO_POR_LECTURA` = la ruta se
trazó línea a línea sobre el código real; `ARMADO_NO_ALCANZABLE` = el defecto existe pero un gate vivo lo
contiene hoy.

| Familia | Ataques | Máx. severidad | Resultado destacado |
|---|---:|---|---|
| F1 Gaming y estrategia de respuesta | 6 | ALTA | No responder → 70-94 % de precisión «fiable» + subida de dificultad |
| F2 Memorización, exposición y contaminación | 6 | CRÍTICA | Banco de 3 formas en fase con el umbral de 3 administraciones |
| F3 Perfiles atípicos, subgrupos y equidad | 6 | ALTA | Sin estratificación ni gate de justicia; barreras leídas como déficit |
| F4 Fatiga, interrupción, abandono y retorno | 6 | ALTA | Los tres frenos de carga muertos simultáneamente |
| F5 Entorno técnico hostil | 6 | ALTA | Cero conciencia de ciclo de vida: llamada entrante = «buen bloque» |
| F6 Bucle adaptativo | 6 | ALTA | Goodhart sobre RT-CV; histéresis defeateada por `Int.MAX_VALUE` |
| F7 Mejora aparente sin transferencia | 6 | CRÍTICA | Dosis-respuesta fabricada por el módulo del banco |
| F8 Cuentas, privacidad, borrado, insider | 6 | ALTA | Persistencia cognitiva sin consentimiento; borrado no fail-closed en Android |

### El fallo más barato y más dañino
**No responder. Coste cero: dejar el móvil en la mesa.** Produce 70-94 % de precisión marcada fiable, sube la
dificultad, y sella el PRE del RCI de forma permanente. Lo que lo hace el más barato no es que un adversario lo
ejecute: es que **un usuario honesto lo ejecuta sin querer** cada vez que entra una llamada.

### El fallo silencioso más probable
**Un bloque jugado en segundo plano persistido como una sesión legítima y buena.** No existe un solo símbolo de
ciclo de vida en `focus/**`. Los ensayos consumidos son mayoritariamente no-objetivo, y se cuentan como
correctos: el resultado observable de una llamada telefónica es una precisión alta marcada fiable y, en
dificultad alta, un `StepUp` con el texto «Dominio estable». Build verde, cero log, cero crash. Además
**envenena el guard de sobreentrenamiento**: la interrupción no dispara la protección, la desactiva.

### Cadena de tres fallos menores que produce un crítico
1. `BASELINE_WEEK = 1` y `EVALUATION_WEEK = 1` ⇒ siempre `probes[0]`. *Aislado: «medimos una capacidad de tres».*
2. `formSeed = reservedForms[index % 3]` con cursor monótono y sin techo. *Aislado: «las formas se reciclan».*
3. `MIN_ADMINISTRATIONS = 3`. *Aislado: prudencia.*

**Juntos:** el periodo del banco (3) y el umbral de fiabilidad (3) quedan en fase, y el primer duplicado exacto
de la forma de la línea base cae **precisamente** en la administración en que el gate se abre.

### La condición en que el sistema parece excelente y falla
**Un piloto de 8 semanas en el que los usuarios más comprometidos muestran las mayores mejoras, con
dosis-respuesta limpia y p pequeña.** Ese resultado, en este código, es exactamente lo que produciría un sistema
sin ningún efecto: más sesiones ⇒ más repeticiones de las 3 formas; el abandono no se registra en la ventana que
consumen las políticas; la regresión a la media está declarada y no corregida; el motor optimiza RT-CV; y sin
control activo nada resta.
**El indicador que lo delataría, y que un análisis normal no mira:** graficar la mejora del RCI contra
`cursor % 3`, no contra las semanas. Si el efecto es real, la curva es plana; si es memoria de forma, hay un
diente de sierra de período 3 con cresta en `cursor % 3 == 0`. El dato está persistido y nadie lo consulta.

### Evidencia que retiraría cada claim
| Claim | Evidencia que lo retiraría |
|---|---|
| «Medible» / «progreso medible» | Un solo caso en que el RCI se emita — hoy imposible: `ReliabilitySource.EMPTY` |
| «Pruebas no entrenadas» | Un grep: `VIGILANCE_PROBE.paradigm == E1_VIGIA.paradigm`. **Ya retirado.** |
| «Semana N de 8» | La ausencia de `cycleWeek` en los tres `copy()`. **Ya retirado.** |
| «Dominio estable» | Un bloque sin una sola respuesta que dispare `StepUp`. **Ya retirado.** |
| Validez de E9/E10/E8 | La estrategia degenerada que maximiza su métrica gobernante. **Ya retirado.** |
| Eficacia / transferencia | Un ensayo con control activo. **No existe.** |

---

## 11. Pruebas ejecutadas — resultado exacto

Ejecutadas desde la sesión principal con el árbol demostrablemente quieto (sin JVM concurrentes, sin
`in-progress-results-*.bin`), aplicando la disciplina de gates del repositorio: informe borrado antes de
invocar, y las tres señales exigidas (tarea ejecutada · línea de tarea **sin** sufijo · artefactos dentro de la
ventana).

```
PWD=<repo-root>          (ruta absoluta redactada por privacidad, MB-LEDGER-0003)
HEAD=8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5
FP_BEFORE=e989a383d65c          T0=2026-08-29T22:05:01Z

=== DETEKT ===   ./gradlew :shared:detekt --rerun
> Task :shared:detekt
BUILD SUCCESSFUL in 28s
1 actionable task: 1 executed
DETEKT_EXIT=0
    → detekt.xml borrado ANTES de invocar; regenerado con 0 <error>

=== DESKTOPTEST ===   ./gradlew :shared:desktopTest --rerun
> Task :shared:desktopTest
BUILD SUCCESSFUL in 1m 12s
11 actionable tasks: 2 executed, 9 up-to-date
TEST_EXIT=0

T1=2026-08-29T22:06:44Z        FP_AFTER=e989a383d65c   (huella idéntica ⇒ árbol inalterado)
```

**Agregado de los XML, filtrado por la ventana de esta corrida:**

```
XML frescos: 610 / 610
tests=4360  failures=0  errors=0  skipped=0
de los cuales mindboost.focus*: 113 suites / 726 tests
```

El extractor lleva un guard anti-vacuidad obligatorio (`if (fresh > 0 && t === 0) → EXTRACTOR ROTO, exit 2`).
**Ese guard se disparó realmente** en el primer intento: un `\d` dentro de una cadena JS escrita por heredoc se
había convertido en `d`, y el agregado devolvía `tests: 0` sobre 610 ficheros. Sin el guard habría reportado un
cero silencioso como si fuera un resultado. Se corrigió a `[0-9]` y se re-ejecutó.

**Lo que este verde SÍ demuestra:** que la suite existente pasa íntegra sobre este árbol, y que el análisis
estático está limpio.
**Lo que NO demuestra, y es el núcleo de esta auditoría:** que las garantías declaradas se cumplan en
producción. Verificado por lectura: al menos tres suites críticas ejercitan estados que el call-site de
producción nunca genera.

**Ningún test de esta auditoría se marca `SUPERADA`.** Los tests propuestos en §14 son `PROPUESTA`.

---

## 12. Plan de remediación

### 0–72 h — detener el daño alcanzable hoy (no requiere rediseño)

| # | Acción | Hallazgo | Coste |
|---|---|---|---|
| R1 | Retirar la copy «Pruebas NO entrenadas — la evidencia más honesta de cambio real» y el KDoc de `ProbeBank` que afirma la disyunción | H-01 | 2 líneas |
| R2 | Gate de tasa de respuesta antes de publicar ACCURACY: `hits+falseAlarms+anticipations == 0` ⇒ `MetricValue.unavailable`. Activa sola la rama `Hold(METRIC_UNAVAILABLE)` **ya existente** | H-03 | ~6 líneas + test con mitad discriminante |
| R3 | Retirar «Semana N de 8» de `FocusHomeScreen` hasta que `cycleWeek` tenga escritor | H-05 | 1 línea |
| R4 | Cambiar «Preliminar (aún no concluyente)» por una fórmula que no prometa alcanzabilidad | H-06 | 1 línea |
| R5 | Registrar en el ledger los 2 BLOQUEANTES y las 5 CRÍTICAS con dueño | todos | — |

### 30 días — hacer visible lo invisible

- **R6** Ciclo de vida: detectar segundo plano y marcar el bloque `INTERRUPTED`; excluirlo de métricas y de la
  decisión adaptativa (H-03/F5). *Sin esto, ningún otro arreglo distingue al interrumpido del adversario.*
- **R7** Los tres parámetros fabricados del `AdaptiveInput` pasan a **obligatorios sin default**, y la ausencia
  se modela `null`, no `NONE` (H-04).
- **R8** Write-ahead en baseline y evaluación (H-11); revertir el cursor de forma si la sonda no completa (H-12).
- **R9** Persistir la identidad de la forma administrada y un registro de exposición reutilizando
  `core/bank/ExposureTracker`, que ya existe (H-12).
- **R10** Accesibilidad mínima: nodo semántico en la superficie de respuesta, `contentDescription` en los 13
  renderizadores, y un discriminador no cromático en E6 (H-10).

### 60 días — reparar el instrumento

- **R11** Rediseñar el banco de sondas con **paradigma propio**, ausente del catálogo de entrenamiento (H-01).
- **R12** Dimensionar la sonda desde `MinSampleGate` hacia arriba, **calculando** la alcanzabilidad con
  `MeasurementReachability` —que existe y no tiene ningún call-site de producción— y negarse a emitir informe si
  el veredicto es `reachable = false` (H-02, H-13).
- **R13** Desacoplar el número de formas del umbral de administraciones y romper la fase (H-01 emergente).
- **R14** Corregir E9 (trayectorias que se cruzan), E10 (ensayos no-objetivo y puntuación T1/T2 separada) y E8
  (puntuar la tarea secundaria) o **retirar su métrica gobernante** (H-06, H-07, H-08).
- **R15** Cablear `core/antigaming` a la ruta de evaluación, con su suite red-team (H-09).

### 90 días y más — poder afirmar algo

- **R16** Corrección por multiplicidad y término de regresión a la media en el RCI (H-16, H-17).
- **R17** Simetrizar los gates PRE/POST persistiendo los hechos de muestra en el snapshot (H-15).
- **R18** Consentimiento explícito antes de persistir dato cognitivo, y régimen para menores (H-19).
- **R19** Ejecutar V0–V8 **con control activo**. Sin él, los cinco mecanismos de mejora aparente son
  indistinguibles de eficacia.
- **R20** Separación de funciones en la gobernanza: quien implementa no cierra (H-20).

---

## 13. Plan de pruebas que faltan (todas `PROPUESTA`)

| ID | Prueba | Qué mata |
|---|---|---|
| T1 | Bloque de 60 ensayos de E1 sin una sola respuesta ⇒ ACCURACY ausente **y** `Hold(METRIC_UNAVAILABLE)`. Mitad discriminante: el mismo bloque con 40 respuestas honestas sigue permitiendo `StepUp` | H-03 |
| T2 | El `Paradigm` de todo `ProbeDefinition` NO aparece en `ExerciseCatalog.implemented` | H-01 |
| T3 | `MeasurementReachability.verdictFor` sobre la sonda real: si `reachable = false`, el informe se niega a emitir | H-02 |
| T4 | Cuatro administraciones consecutivas: la forma servida en la 4.ª ≠ la de la línea base | cadena emergente |
| T5 | Estrategia «retener siempre» en E9 ⇒ la métrica gobernante NO puede caer en la banda 0,65-0,85 | H-06 |
| T6 | Tocar en todos los ensayos de E10 ⇒ la métrica gobernante NO puede ser 1,0 | H-07 |
| T7 | Ignorar la tarea secundaria de E8 ⇒ `DUAL_TASK_COST` no puede ser el valor óptimo | H-08 |
| T8 | `AdaptiveInput` no compila sin fatiga, maestría e histéresis reales (sin default) | H-04 |
| T9 | Tras N sesiones, `cycleWeek` avanza; el día de deload no recomienda ejercicio | H-05 |
| T10 | Los 13 renderizadores exponen nodo semántico; E6 tiene discriminador no cromático | H-10 |
| T11 | Un bloque interrumpido en segundo plano no entra en métricas ni en la decisión adaptativa | F5 |
| T12 | Abortar la sonda no consume forma reservada | H-12 |

Cada test debe llevar su **mitad discriminante** (el caso honesto que debe seguir pasando) y verificarse
**por mutación** —reintroducir el defecto y exigir ROJO—, según la disciplina que el propio repositorio
documenta.

---

## 14. Checklist de re-auditoría

**Condición de entrada:** los 12 tests de §13 existen, están en verde y **cada uno se ha verificado por
mutación**.

1. ¿Los 2 BLOQUEANTES están cerrados con evidencia y gate, no con narrativa?
2. ¿Se ha rediseñado el banco de sondas, o se ha retirado la afirmación de cambio?
3. ¿La alcanzabilidad de la sonda se **calcula** y bloquea la emisión?
4. ¿Los tres parámetros del `AdaptiveInput` son obligatorios?
5. ¿Existe conciencia de ciclo de vida y una marca de sesión interrumpida?
6. ¿Existe algún dato humano? ¿Con control activo?
7. ¿Existe una declaración de accesibilidad y una forma de expresar «no medible para este usuario»?
8. ¿Se ha ejecutado el diagnóstico `mejora vs cursor % 3`?
9. ¿Hay separación de funciones: quien implementó no cerró?
10. ¿Se ha observado el sistema **en ejecución**? Sin esto, la re-auditoría vuelve a ser `PROVISIONAL`.

---

## 15. Riesgo residual tras la remediación propuesta

Aun ejecutando R1–R20 completo, quedan tres riesgos que **ninguna cantidad de ingeniería cierra**:

1. **Riesgo de constructo.** Que «Enfoque» sea un factor unitario entrenable no está demostrado en la
   literatura ni en este producto. El propio portfolio vigente lo enumera entre lo que el motor no demuestra.
   Ninguna corrección de código lo resuelve.
2. **Riesgo de transferencia.** Aunque la sonda se rediseñe correctamente, medir mejora en una sonda de
   laboratorio no autoriza a afirmar mejora en la vida del usuario. El repositorio ya tiene el vocabulario para
   decirlo (`FunctionalProtocols`) y ningún consumidor que lo renderice.
3. **Riesgo de instrumento.** El RT se mide con un reloj monótono de corrutina, sin frame timing ni gate de
   jank. Los efectos que se pretende medir (~40 ms) están en o por debajo del error del instrumento en un
   dispositivo bajo carga. El repositorio **ya tiene** `FrameStatsCalculator` y `TemporalClassPolicy`
   construidos y probados para Velocidad; Enfoque no los usa.

---

## 16. Trazabilidad y método

- **12 lentes de dominio** (A ontología · B ciencia · C pedagogía · D contenido · E psicometría · F adaptación ·
  G eficacia · H UX/a11y · I ingeniería · J IA/ML · K seguridad/privacidad · L gobernanza), cada una simulada
  **independientemente**, sin ver las demás.
- **12 refutadores**, uno por dominio, cuyo único encargo era tumbar los hallazgos de su lente. Corrigieron 58
  severidades a la baja, refutaron 2 hallazgos, y **aportaron 12 hallazgos que su detector no había visto** —
  incluido el «cero anti-gaming», encontrado por dos refutadores y por ningún detector.
- **8 familias de Red Team** + 1 sintetizador de cadenas, que produjo el hallazgo emergente de la fase 3↔3.
- **Verificación independiente del director** sobre las 14 afirmaciones que sostienen el veredicto, leyendo el
  código directamente. Todas confirmadas; dos resultaron **más fuertes** de lo enunciado (E10: no es que el
  parpadeo no se compute, es que *todos* los ensayos son objetivo; E9: el premio del no-respondedor *crece* con
  la dificultad).
- **Mecanismo, no prohibición.** Todos los agentes corrieron con `agentType: 'architect'` (Read/Grep/Glob, **sin
  Bash**), de modo que era estructuralmente imposible que invocaran Gradle y destruyeran la evidencia de la
  corrida — el incidente `MB-FOCUS-0083` del propio ledger, en el que un revisor borró 609 XML de evidencia.
  Una prohibición en el prompt no es un control.
- **Coste:** 33 agentes, 11,03 M tokens de subagente, 1008 usos de herramienta, ~38 min de reloj.

---

## 17. Reconciliación con el registro previo

El ledger contiene 230 hallazgos (116 OPEN). Esta auditoría **no los duplica**: donde un hallazgo nuevo coincide
con uno registrado, se referencia. Contribución neta: **7 hallazgos nuevos** de causa raíz no registrada
(H-01, H-02, H-03, la cadena emergente 3↔3, H-06, H-07, H-08) y la **elevación** de varios ya registrados al
descubrirse su alcance real.

Observación de gobernanza, dirigida al proceso y no a las personas: más de la mitad del registro está OPEN y el
cierre se concentra en defectos propios y pequeños, mientras los defectos de validez del instrumento —los que
deciden si el producto puede afirmar algo— llevaban sin registrarse. Un ledger disciplinado puede, sin
mala fe, convertirse en un mecanismo que documenta con enorme rigor lo que ya se sabe mirar.

---

## 18. Lo que esta auditoría NO puede afirmar

- **Nada sobre el comportamiento real en dispositivo.** No se observó el sistema en ejecución. Todas las
  afirmaciones de comportamiento son inferencias sobre código leído (E2), no observaciones (E4).
- **Nada sobre iOS.** El estado es `BLOCKED_IOS`; no hay macOS en este entorno.
- **Nada sobre usuarios reales.** No existen. No hay telemetría, ni logs, ni cohortes.
- **Nada cuantitativo sobre la magnitud del daño.** Sin exposición no hay incidencia.
- **Nada sobre los otros siete bloques cognitivos.** Fuera de alcance, aunque `DefaultMetricsEngine`,
  `MinSampleGate` y `core/scoring` son compartidos y algunos hallazgos podrían aplicar.

---

## 19. Conclusión

**`RECHAZADO` (`PROVISIONAL`) · 27,40 / 100.**

El bloque Enfoque no debe desplegarse con ninguna afirmación de medición, y **no debe abrir ninguno de sus tres
gates de contención** hasta cerrar H-01 y H-02. La razón no es que esté a medio hacer: es que sus gates están
apagando el daño **y al mismo tiempo ocultando que el instrumento no mide**. El día que se abran —que es el plan
declarado en el propio código, cuyo KDoc llama a la ruta contenida «retained wiring point»— el defecto llegará
al usuario con toda la autoridad de un producto ya en producción, y su primera afirmación de capacidad estará
aritméticamente destinada a ser una medida de memoria de secuencia.

Hay dos decisiones legítimas, y ambas son mejores que la actual:

- **Reparar el instrumento** (R11–R15) antes de tocar un solo gate; o
- **Reclasificar Enfoque como práctica**, no como medición: retirar las afirmaciones de cambio, quedarse con la
  copy honesta que ya está embarcada («esta sesión no lleva puntuación»), y dejar de mantener un aparato de
  medida que no puede medir.

Lo que no es legítimo es la tercera vía, que es la que la inercia produce: mantener la contención, seguir
construyendo encima, y abrir los gates cuando la presión de producto lo pida.

### Tres razones por las que esta auditoría podría estar equivocada

1. **No observé el sistema en ejecución.** Todo mi razonamiento es sobre código leído. Un solo hecho de runtime
   —un guard en una capa que no leí, un valor que la plataforma sustituye, un `LaunchedEffect` que reordena algo—
   podría invalidar una cadena entera. El ataque de la precisión por omisión, en particular, asume que el
   `FocusFixedBlockRunner` avanza los ensayos exactamente como su código sugiere; nunca lo vi hacerlo.
   **Confianza: alta en los hechos estáticos, media en las consecuencias dinámicas.**
2. **Juzgo con criterios de instrumento psicométrico algo que quizá nunca pretendió serlo.** Si la intención
   real de Enfoque es entrenamiento con feedback y no medición —y la copy embarcada del resumen sugiere
   exactamente eso—, entonces la mitad de mis hallazgos críticos son irrelevantes y el veredicto correcto sería
   «producto de práctica razonable con documentación que promete de más». La ambigüedad no la introduzco yo: la
   introduce un producto que dice «no lleva puntuación» en una pantalla y «la evidencia más honesta de cambio
   real» en otra. Pero la lectura estricta es una **elección** mía, y podría ser la equivocada.
3. **Mis hallazgos y sus refutadores comparten método, sesgo y modelo.** Los 33 agentes leyeron los mismos
   ficheros con el mismo estilo de razonamiento. Un refutador que comparte el punto ciego de su detector no es
   independiente: es un eco con otro nombre. El hecho de que 58 severidades bajaran y 2 hallazgos cayeran indica
   que el mecanismo mordió — pero un sesgo compartido produciría exactamente este patrón, y también un
   **falso negativo sistemático**: aquello que ninguno de los 33 estaba equipado para ver sigue sin verse, y yo
   no puedo enumerarlo. La única corrección real es una auditoría humana externa, que el propio índice de
   autoridad marca como `NOT_OBTAINED`.

---

## 20. Anexo — inventario de evidencia

- **Corrida de gates:** `scratchpad/audit/gate.log` (fuera del árbol del repositorio, por la lección
  `MB-FOCUS-0083`: la evidencia dentro del alcance de un revisor es evidencia que un revisor puede borrar).
- **33 resultados de agente** en bruto: `scratchpad/audit/agents/*.json`.
- **Journals de workflow:** `wf_e38bef27-11e` (24 agentes: 12 lentes + 12 refutadores) y `wf_7c5970a4-0d7`
  (9 agentes: 8 familias + sintetizador).
- **Registro canónico:** entrada correspondiente en `MINDBOOST_AUDIT_LEDGER.md`.

---

## 21. Nota de vigencia — 2026-09-03

> **Esta sección se AÑADE; nada de lo anterior se reescribe.** Un informe fechado describe el árbol que
> auditó, y falsearlo a posteriori destruye justamente lo que un registro sirve para probar. Lo que sigue
> es el delta entre el árbol auditado y el de hoy.

### 21.1 Los dos árboles

| | Auditado | Hoy |
|---|---|---|
| `HEAD` | `8a06b9a` | `bdc252f` |
| Instante del gate | 2026-08-29T22:05:01Z (`gate.log`, `T0`) | 2026-09-03T08:14:55Z |
| Árbol de trabajo | el de esa fecha | + `FOC-P0-00`…`FOC-P0-05` sin commitear |

La cita `HEAD 8a06b9a` de la portada **era correcta cuando se midió**. Entre el 2026-08-30 y el
2026-09-02 una sesión paralela leyó este informe y el registro, y ejecutó seis fases de remediación. El
informe no está equivocado: está **fechado**. Las verificaciones de §5 y §11 no se han repetido contra el
árbol nuevo, así que **ningún hallazgo de este informe puede darse por vigente sin re-medirlo**, en
ninguna de las dos direcciones.

### 21.2 Afirmaciones de este informe que HOY son falsas

Se listan porque una afirmación caducada dentro de un documento que alguien volverá a leer no es
documentación obsoleta: es una instrucción.

1. **§3 (línea 59) y §10 F5 — «no existe un solo símbolo de ciclo de vida en `focus/**`».** Falso desde
   `FOC-P0-01`/`FOC-P0-03` (2026-08-31 / 2026-09-02). Existen `AppLifecycleProbe`,
   `LifecycleObservation` y su cableado; una sesión interrumpida ya NO produce bytes idénticos a una
   limpia. La consecuencia que el informe derivaba de esa ausencia queda retirada.
2. **§10 (línea 471) — el ataque de «bloque sin respuestas puntúa alto».** Cerrado en el productor por
   `FOC-P0-04` (`DefaultMetricsEngine` emite `MetricAbsence.NO_RESPONSES`) y en la sesión por
   `FOC-P0-05` (`SessionQualityGate.admit` ⇒ `NotMeasurable(NO_RESPONSES)`), con
   `focusQualityGateScanner` en `check` como gate de build.
3. **§12 R1 (mitad de copy) — «la copy embarcada afirma una prueba no entrenada».** Cerrado por
   `FOC-P0-00`: la escalera de afirmaciones está congelada en C0 y el hecho está pinneado por
   `FocusClaimLevelGateTest.everyProbeParadigmIsAlsoATrainedParadigm`, que se pone rojo el día que una
   sonda corra un paradigma que nadie entrena.

### 21.3 Qué se ejecutó hoy del bucket 0–72 h (§12 R1–R5)

Verificar cada enunciado **antes** de editar rescató la mitad del bucket:

| Ítem | Veredicto de la premisa | Acción |
|---|---|---|
| R1 | cierta pero mal dirigida | **Ejecutado en lo que quedaba.** «material reservado» es cierto (las SEMILLAS lo son). Lo falso vivía en 9 KDoc y comentarios que la congelación C0 no escanea, porque solo cubre literales de producción. |
| R2 | **falsa** | **No ejecutado.** Ya estaba implementado, y mejor que mi prescripción: gate de sesión + gate por métrica + segundo canal por celda + tarea Gradle. Reimplementarlo habría creado dos dueños del mismo invariante. |
| R3 | cierta e **infravalorada** | **Ejecutado y ampliado.** No es solo una etiqueta falsa: la periodización entera está congelada y su salida es inerte. |
| R4 | cierta pero exagerada | **Ejecutado.** |
| R5 | **falsa** | **No ejecutado.** Ya existe `focusQualityGateScanner` en `check`. |

Cambios aplicados (**cero cambio de comportamiento**: copy, KDoc y una etiqueta retirada):

- **R1** — `ProbeBank`, `ProbeDefinition` (×2), `ProbeExerciseMapping`, `BaselineAssembler` (×2),
  `BaselineProtocol`, `FunctionalProtocols`, `FocusReport`, `TransferReport` (×2) y 3 comentarios de
  test. Además un **error factual en el KDoc que retiraba la afirmación**: decía que PVT se entrena en
  E1/E11/E12, y `E11_ANCLA` es `DIGITAL_DISTRACTION` (los únicos PVT son E1 y E12).
- **R3** — retirada de «Semana N de 8» (`FocusHomeScreen`) y corrección del comentario que afirmaba que
  la tarjeta sigue el ciclo del usuario.
- **R4** — «Preliminar (aún no concluyente)» → «Preliminar: sin margen de error establecido» en las dos
  pantallas.

### 21.4 Hallazgos nuevos, tres de ellos no contenidos en el enunciado que los originó

- `MB-FOCUS-0102` (P2) — la sección de evidencia fuerte es **inalcanzable** en producción: sin fuente de
  fiabilidad no hay RCI `reliable`, luego `strongEvidence` está siempre vacía. El rótulo «aún no
  concluyente» prometía una conclusión que no puede llegar.
- `MB-FOCUS-0103` (P2) — `cycleWeek` **no tiene escritor de producción**: la periodización resuelve
  siempre la semana 1, las semanas de descarga 4 y 8 son inalcanzables y `DELOAD_VOLUME = 0.5` es código
  muerto.
- `MB-FOCUS-0104` (P2) — `volumeMultiplier`, `capDifficulty` e `includeProbe` tienen **cero lectores
  de producción**: aunque el ciclo avanzara, su salida seguiría sin efecto.
- `MB-FOCUS-0105` (P3) — el mismo ciclo muerto existe en MEMORIA; se registra con dueño, no se arregla,
  por estar fuera del alcance auditado.

### 21.5 Evidencia de esta sesión

- Gate de escáneres y análisis estático: `scratchpad/audit/gate4.log` — `focusQualityGateScanner`,
  `mindboostCopySafetyScanner` (+ variantes `CommonTest` y `DesktopTest`) y `detekt` ejecutados **sin
  sufijo**; `detekt.xml` borrado antes de invocar y reescrito con **0 `<error>`**.
- Suite: `scratchpad/audit/gate5.log` — `:shared:desktopTest --rerun` (la corrida anterior salió
  `UP-TO-DATE`, que es «no corrió»): **619 suites / 4415 tests / 0F / 0E**, con las 619 XML escritas a las
  `2026-09-03T08:14:55Z`.
- Huella `git status --porcelain` estable en `32666e17…` a ambos lados de las corridas, con el árbol
  quieto (sin JVM ajenas, sin `in-progress-*.bin`).
- Contraste adversarial: workflow de 16 agentes `architect` **sin Bash** (`wf_9332fbf6-ab6`), elegido así
  por el mecanismo de `MB-FOCUS-0083` — una prohibición en el prompt no es un control. Refutó la premisa
  de R2, marcó R1 como mal dirigida y R3 como infravalorada, y aportó cinco de los sitios de KDoc y el
  error factual del E11 que yo no había visto.

### 21.6 Lo que esta nota NO afirma

El veredicto de la portada — `RECHAZADO` · `PROVISIONAL` · 27,40/100 — **no se recalcula aquí**. Cuatro
hallazgos han mejorado su estado y cuatro han nacido, pero recomputar la puntuación exigiría repetir §5 y
§11 completas contra el árbol nuevo, y eso es una re-auditoría, no una nota. El bloqueo sigue en pie: los
dos `P0` (`MB-FOCUS-0089`, `MB-FOCUS-0090`) siguen sin cerrarse, y `MB-FOCUS-0101` bloquea
explícitamente cualquier release hasta que la sonda de ciclo de vida se verifique en dispositivo real.


## 22. Nota de vigencia — 2026-09-06 (bucket 60 d, R12)

> Se AÑADE, como la §21. R6–R11 dejaron su veredicto en el registro (`MINDBOOST_AUDIT_LEDGER.md`) y en
> `CLAUDE.md`; esta nota vuelve al informe porque R12 cambia el ESTADO de un hallazgo de este documento
> (H-02) y retira una prescripción suya, y eso debe leerse aquí, donde alguien volverá a abrirla.

### 22.1 R12 contra su prescripción, punto por punto

| Punto | Veredicto | Acción |
|---|---|---|
| Enunciado de H-02: «la sonda no puede muestrear ninguna métrica de RT» | **CIERTO, y más fuerte de lo enunciado.** No es una expectativa sobre un sorteo: la sonda sirve una población CERRADA de tres formas reservadas, y medidas sobre el productor real (`FocusGraph.fixedBlockForm`) llevan **11, 11 y 13** señales en 55 ensayos frente a un piso de 20. Y ninguna carga perceptual lo arregla: el eje que gobierna la tasa sólo hace las señales MÁS raras (máximo 0,30 a carga 0), así que a 55 ensayos el piso es inalcanzable en todo el eje. | Elevado de HECHO esperado a HECHO medido sobre la población completa (`MB-FOCUS-0092`). |
| Mecanismo prescrito: «calcular la alcanzabilidad con `MeasurementReachability.verdictFor`» | **REFUTADO.** `verdictFor(E1_VIGIA, 55, fixedProbeDifficulty)` devuelve `null`, no `reachable = false`: su tabla `cells` cubre SÓLO las seis comparativas con cuota (E2/E4/E5/E6/E8/E13) y `RT_CV` es una métrica de una celda que la autoridad declina. El test T3 tal como está escrito es insatisfacible, y el gate de cierre de `MB-FOCUS-0092` lo nombraba literalmente. | No se cablea `verdictFor` a la sonda. El gate de cierre se reescribe. |
| «Negarse a emitir informe si el veredicto es negativo» | **YA TIENE DUEÑO, y mejor.** `EvaluationComparator` gatea por `measurementSufficient == true` sobre la muestra REALIZADA y divulga la retención en `withheldForSample` (R12 del catálogo, `MB-CATALOG-0020`). Un límite superior planificado sólo puede negar; la muestra realizada niega Y declara. | No se añade un segundo dueño. |
| «Dimensionar la sonda desde `MinSampleGate` hacia arriba» | **Decisión de producto, no de fase.** Calculado sobre el productor real: la longitud más pequeña a la que las TRES formas cruzan el piso es **110** (98 y 109 fallan por la forma 1003, con 18); no es monótona porque `nextInt(count)` re-siembra el flujo y cada longitud es una forma distinta, no un prefijo. Alargar la sonda invalida toda línea base capturada contra la forma de 55 (`MB-FOCUS-0126`). | Registrado con dueño (`MB-FOCUS-0130`), superficie pinneada en test, NO ejecutado. Precedente: R11, `MB-CATALOG-0018`. |

### 22.2 Lo que se ejecutó, y salió de medir el terreno, no de la prescripción

1. **Defecto VIVO en el productor** (`MB-FOCUS-0128`, P1, cerrado): `DefaultMetricsEngine.sessionMetrics`
   sellaba cada métrica ponderada por ensayos —y `LAPSES`— con el CONTEO DE ENSAYOS como celda, así que un
   bloque que había negado honestamente la suficiencia (`RT_CV` con celda 11) volvía a nivel de sesión con
   `n = 55` y `measurementSufficient = true`. No era latente: la pantalla de progreso alimenta el comparador con
   `retest.sessionMetrics` (`FocusProgressStateHolder.kt:64`) mientras la de evaluación usa
   `blocks.single().metrics`, de modo que **dos pantallas daban dos veredictos sobre el mismo retest**, y el
   fabricado era el de progreso. Arreglo: el peso de un bloque es su conteo de ensayos y su muestra es la
   celda de la métrica; dos números que la función mezclaba. Mutaciones M1 y M2 en ROJO 1 de 12, sólo el test
   nuevo.
2. **El guard que afirmaba lo contrario de H-02** (`MB-FOCUS-0129`, cerrado): `ProbeMeasurementReachabilityTest`
   «demostraba» que la sonda cableada cruza su piso pasando el conteo de ensayos como celda de `RT_CV` — la misma
   sustitución que el motor, desde el otro lado. Reescrito para CONSULTAR al productor sobre las tres formas
   reservadas, con su mitad discriminante (ACCURACY a 55 sí cruza) y la superficie de remedio pinneada. Mutación
   M3 (tasa de señal del productor 0,30 → 0,60) pone en ROJO exactamente los dos guards nuevos y ningún otro:
   leen al generador real, no a un fixture.
3. **Registrados con dueño, sin implementar:** el histórico persistido contaminado por (1) (`MB-FOCUS-0131`; las
   filas ya grabadas conservan `n = 55`, y `BlockSummaryDto.metrics` permite recomputar en lectura) y la muestra
   de la sonda dejada a la semilla (`MB-FOCUS-0132`: E1 sortea señales Bernoulli por ensayo mientras las seis
   comparativas recibieron cuota en `MB-CATALOG-0014`; una cuota igualaría las celdas pre/post en 11, sin
   alcanzar el piso — arregla la comparabilidad, no la alcanzabilidad).

### 22.3 Evidencia

- `:shared:desktopTest --rerun`: **621 XML todos frescos / 4446 tests / 0F / 0E / 0S**; R11 cerró en 4444 sobre
  621, y el +2 es exactamente los dos tests nuevos. Huella `git status --porcelain` idéntica a ambos lados
  (`d1003cc2…`), 0 JVM ajenas, sin `in-progress-*.bin`.
- `:shared:detekt` 0 errores con `detekt.xml` borrado antes de invocar y regenerado dentro de la ventana;
  `:shared:compileKotlinDesktop`, `:shared:compileAndroidMain`, los tres `mindboostCopySafetyScanner*` y
  `focusQualityGateScanner` con `--rerun`, todos verdes.
- M1/M2/M3 concluyentes con atribución 1-a-1; restauraciones byte-idénticas verificadas por SHA-256
  (`c0794d69…` motor, `ab8f2a20…` generador).

### 22.4 Lo que esta nota NO afirma

Nada de §21.6 cambia: la puntuación de portada no se recalcula, los dos `P0` siguen abiertos y
`MB-FOCUS-0101` sigue bloqueando el release. H-13 (`EVALUATION_WEEK = 1`) queda intacto; sigue siendo la razón
de que el defecto de la sonda selectiva sea latente y no vivo.

## 23. Nota de vigencia — 2026-09-06 (bucket 60 d, R13)

> Se AÑADE, como la §21 y la §22. R13 cambia el ESTADO de la cadena emergente 3↔3 que este informe registró
> (§19, "la cadena emergente 3↔3") y deja por escrito qué mitad de H-01 sigue abierta, así que debe leerse
> aquí, donde alguien volverá a abrir el informe.

### 23.1 R13 contra su prescripción, punto por punto

La prescripción de R13 es una línea con dos cláusulas: «desacoplar el número de formas del umbral de
administraciones **y** romper la fase (H-01 emergente)».

| Cláusula | Veredicto | Acción |
|---|---|---|
| Enunciado de la cadena 3↔3 | **CIERTO, y verificado contra el árbol de HOY, no contra el del informe.** Tres formas reservadas por sonda frente a `MIN_ADMINISTRATIONS = 3`; los dos carriles (evaluación y progreso) piden `week = 1` sobre un cursor con clave común `userId:probeId`; y la propia línea base consume una administración. La primera administración con la puerta de fiabilidad abierta es la número MIN más 1, que dibuja el índice de forma MIN — es decir, exactamente la forma que el usuario ya vio en su línea base. | Ejecutado. |
| «Desacoplar el número de formas del umbral» | **EJECUTADO, con un techo que se declara y no se esconde.** `ProbeBank.reservedForms` pasa de 3 a 4 formas por sonda, así que la administración que abre la puerta cae sobre material fresco. El desacoplamiento se pinnea con un guard DERIVADO de las dos autoridades a la vez, no de una tabla escrita a mano: mover `reservedForms` o mover `MIN_ADMINISTRATIONS` por separado lo pone en ROJO, y las mutaciones M1 y M2 lo confirman. **Techo:** la puerta cuenta filas `Admitted` y el cursor quema una forma en CADA pick, así que son contadores distintos; fuera del camino todo-admitido una corrida retenida gasta suministro sin acercar la puerta y el primer retest con puerta abierta puede caer igualmente sobre material repetido. Sigue siendo honesto, pero por la mitad ESTRUCTURAL, no por esta aritmética. | Ejecutado, con el guard derivado como cierre mecánico y el techo declarado en su KDoc (`MB-FOCUS-0135`). |
| «Romper la fase» | **EJECUTADO, y por la vía estructural en vez de la aritmética.** Subir el suministro sólo mueve la coincidencia; lo que la rompe de verdad es que una administración que repite material **no pueda sostener una afirmación de cambio fiable**, pase lo que pase con los números. `ProbeReliabilityEstimator` gana un tercer gate, `formIsFresh = (repeatsEarlierForm == false)`. El hecho NO se persiste de nuevo: se DERIVA del que `nextProbe` ya declaraba desde R9, así que no nace un segundo dueño del mismo hecho. | Ejecutado. Cierra `MB-FOCUS-0122`. |
| Lo que la prescripción NO decía y la fase encontró al medir | El desacoplamiento es de **UNA unidad**: con 4 formas y un umbral de 3, un usuario tiene **exactamente un** retest con la puerta abierta y material fresco por sonda. Cada forma adicional compra exactamente uno más, y ninguna cifra de producto declara cuántos hacen falta. | Registrado con dueño (`MB-FOCUS-0133`), NO decidido. Precedente: R11 (`MB-CATALOG-0018`) y R12 (`MB-FOCUS-0130`). |

### 23.2 Lo que se ejecutó, y por qué la forma elegida es la conservadora

1. **El tercer gate es tri-estado a propósito.** `repeatsEarlierForm: Boolean?` se compara `== false`, nunca
   `!= true`: `null` significa «esta fila no lleva la identidad de la forma» (`MB-FOCUS-0121`) y **también
   retiene**. Con `!= true` la ausencia habría pasado por fresca, que es la respuesta favorable fabricada
   desde el desconocimiento — la dirección cara de `null != 0`.
2. **El parámetro es OBLIGATORIO y sin default.** Un default en `FocusEvaluationService.evaluate` habría
   dejado que un llamante futuro afirmara material nuevo exactamente en las corridas en que el suministro
   envuelve, y ningún guard textual por fichero puede ver un valor por defecto (precedente: `MB-FOCUS-0071`).
3. **Consecuencia declarada, no escondida:** el carril de PROGRESO pasa `null` porque sus filas vienen de
   `probeHistory`, que no lleva la forma. Ese carril **no publicará jamás un cambio fiable** hasta que la
   fila lleve la identidad. Es la dirección correcta y a la vez una capacidad que la pantalla de progreso
   pierde; queda dicho en `MB-FOCUS-0121`, junto con una segunda asimetría medida entre carriles: evaluación
   cuenta como administraciones previas sólo las filas `Admitted`, y progreso cuenta TODAS.
4. **Lo que NO se ejecuta:** negarse a administrar cuando el suministro se agota. El sistema sigue sirviendo
   material repetido y declarándolo (`REPEATED_FORM_CLAUSE`, R9), así que `MB-FOCUS-0119` sigue
   **MITIGATED y no CLOSED**: la envoltura se retrasa de la cuarta administración a la quinta y deja de poder
   sostener una afirmación, pero sigue ocurriendo.

### 23.2 bis El P1 que el gate de salida encontró, y que esta fase paró a arreglar

El gate adversarial devolvió, convergente en dos lentes independientes, un defecto **vivo hoy** que no estaba
en ninguna prescripción y que yo no había visto: `LocalProbeBankRepository.readCursor` resolvía el cursor de
formas con `readParsed`, que ante `SecureReadResult.Failure` devuelve `null` **y además pone la clave en
CUARENTENA, es decir la BORRA**; el repositorio colapsaba ese `null` en `0` y a continuación escribía `1`
de forma incondicional, con lo que el reinicio quedaba permanente.

La cadena medida: `formSeed` vuelve a `reservedForms[0]` —la forma que el usuario **ya hizo en su línea
base**— y `repeatsEarlierForm` sale `false`, de modo que la copy **retira su cláusula de divulgación** y
afirma material distinto, y el gate de frescura que R13 acababa de instalar pasa sobre un hecho que nadie
midió. El cursor es el **dueño único** de qué formas se sirvieron y no se puede re-derivar de nada, así que la
cuarentena lo destruía de forma irreversible ante un fallo transitorio de backend.

Arreglo: `readCursor` lee el `SecureReadResult` **crudo** y devuelve `Int?` con TRES respuestas (0 para
una primera administración genuina, el conteo almacenado, y `null` para «no puedo decirlo»), no cuarentena
nunca, sólo avanza un cursor que pudo LEER, y falla **cerrado**: una identidad desconocida declara la
repetición y retiene, exactamente como una envoltura real. `releaseProbe` deja de rebobinar una posición que
no conoce. Registrado y cerrado como `MB-FOCUS-0134`.

### 23.3 Evidencia

- Mutación x4 con atribución 1-a-1 y restauración byte-idéntica verificada por SHA-256 (`900058df` banco,
  `03cc3489` estimador, `e8a16c04` holder): **M1** (retirar la cuarta forma) 1 ROJO, el guard derivado;
  **M2** (`MIN_ADMINISTRATIONS` 3 → 4) 1 ROJO, el MISMO guard — la prueba de que está derivado de ambas
  autoridades y no de una; **M3** (`formIsFresh` a `true`) 2 ROJOS, el test de exclusión del servicio y el
  del holder; **M4** (el holder pasa un literal `false` en vez del hecho del planificador) 1 ROJO, sólo el
  del holder.
- Mutación del arreglo del P1, x2, con restauración byte-idéntica verificada por SHA-256 (`c71f6d70`):
  **M5** (fail-open restaurado, `Failure` a `0`) deja 2 ROJOS de 13 por las aserciones de **frescura**;
  **M6** (cuarentena restaurada) deja 2 ROJOS de 13 por la aserción de **supervivencia de la entrada**. Dos
  mutaciones, dos mitades distintas del mismo guard — que es la prueba de que ninguna de las dos sobra.
- Gate completo en serie: `:shared:desktopTest --rerun` **4450 tests / 0F / 0E / 0S** sobre 621 XML todos
  frescos (R12 cerró en 4446 sobre 621; el delta de +4 son dos del gate de R13 —el guard derivado y el test de
  exclusión del servicio— más dos del arreglo del P1; el gemelo fresco del holder es un RENOMBRADO, no un test
  nuevo). `:shared:detekt` 0 errores con el informe BORRADO antes de invocar. `compileKotlinDesktop`,
  `compileAndroidMain`, los tres `mindboostCopySafetyScanner` y `focusQualityGateScanner`, verdes con
  `--rerun`. Huella `git status --porcelain` idéntica antes y después de la tanda (`a7f26206d15e`).

### 23.4 Lo que esta nota NO afirma

H-01 **no queda cerrado**: R13 rompe su cadena emergente (la coincidencia 3↔3), no su mitad de diseño — que la
sonda comparta paradigma con el catálogo de entrenamiento sigue siendo cierto, y R11 ya midió que la
prescripción de rediseñarla es estructuralmente insatisfacible mientras el CI imponga
`implemented == wiredExerciseIds`. El suministro **sigue siendo finito**: envuelve una administración más
tarde, no deja de envolver. Y nada de §21.6 ni de §22.4 cambia: los dos `P0` siguen abiertos y
`MB-FOCUS-0101` sigue bloqueando el release.

Y una afirmación que esta nota **retira explícitamente**: R13 no hace alcanzable la afirmación de cambio
fiable en la app embarcada. `DefaultFeatureFlags.focusLifecycleProbeEnabled` vale `false`, así que
`RememberFocusStateHolders` instala `DisabledAppLifecycleProbe`, cuyo `stop()` devuelve
`NotObservable(DISABLED_BY_FLAG)`, y `SessionQualityGate` lo convierte en
`NotMeasurable(LIFECYCLE_NOT_OBSERVED)`: **ninguna administración es nunca `Admitted`**, así que
`priorAdministrations` se queda en 0 de forma permanente y la puerta de fiabilidad no se abre jamás en el
carril de evaluación con los flags de hoy. La dirección es fail-closed y honesta —nadie recibe una afirmación
fabricada— pero significa que el suministro se consume sin que la propiedad que R13 compra sea alcanzable.
Registrado como `MB-FOCUS-0135`, no resuelto aquí: cambiar un flag de producto no es un paso de R13.

### 23.5 El gate de salida, y qué hacer con lo que no llegó a juzgarse

El gate adversarial se corrió con agentes SIN Bash (el mecanismo de `MB-FOCUS-0083`: una prohibición en el
prompt no es un control) y produjo el P1 de §23.2 bis, más dos correcciones a mi propio trabajo (el techo de
la mitad aritmética, §23.1; y un KDoc que prometía cubrir el salto del estado a la pantalla y sólo pinnea el
contrato de copy, `MB-FOCUS-0136`, corregido en el mismo cambio).

Corrección metodológica que se declara en vez de esconderse: el arnés del gate marcaba un hallazgo como
muerto cuando **todos** sus refutadores morían, y el límite de uso mató 18 de 28 agentes, así que seis
hallazgos volvieron con cero refutaciones. **Cero refutaciones no es refutado: es no juzgado**, y se
dispusieron uno a uno a mano. Sólo dos llegaron con refutación real 3 de 3 —la copy duplicada de la pantalla
de progreso y el KDoc de su holder—, ambos refutados porque `ReliabilitySource.EMPTY` ya hacía inalcanzable
esa rama **antes** de R13; y uno lo refuté yo contra el TECHO DECLARADO de R9 en `FocusRepositories.kt` (el
corte de `releaseProbe` es «el bloque terminó», no «se dibujó el primer estímulo»).


## 24. Nota de vigencia — 2026-09-07 (bucket 60 d, R14)

> Se AÑADE, como la §21, la §22 y la §23. R14 corrige el MECANISMO de tres enunciados de este informe
> (H-06, H-07, H-08) que resultaron CIERTOS en su efecto y equivocados en su explicación, y REFUTA con
> medición la mitad B de su propia prescripción. Debe leerse aquí, donde alguien volverá a abrir el informe.

### 24.1 R14 contra su prescripción, punto por punto

La prescripción de R14 es una línea con dos mitades: «corregir E9 (trayectorias que se cruzan), E10 (ensayos
no-objetivo y puntuación T1/T2 separada) y E8 (puntuar la tarea secundaria) **o** retirar su métrica
gobernante».

| Cláusula | Veredicto | Acción |
|---|---|---|
| Enunciado de H-06 (E9) | **CIERTO en el efecto, EQUIVOCADO en el mecanismo.** El informe dice que «retener siempre acierta el 66,7 %-68,8 %». Medido contra el productor real, retener en TODOS los ensayos ya **no compra nada**: desde `MB-FOCUS-0091` / `FOC-P0-04`, cero respuestas produce una **ausencia tipada** (`SessionQualityGate` → `NotMeasurable(NO_RESPONSES)` y `DefaultMetricsEngine` → `unavailable(ACCURACY, …, NO_RESPONSES)`), no una precisión alta. El exploit que SÍ es alcanzable es **una sola pulsación por bloque**: basta un token para que la métrica salga confiada, y entonces la mediana cae dentro de la banda de mantenimiento sin haber seguido un solo objeto. La segunda mitad del enunciado —«el premio crece con la dificultad»— es cierta y **peor de lo escrito**: el único eje que el bucle adaptativo puede mover en E9 (`perceptualLoad`, su `governingAxis`) paga MÁS al no-respondedor, y en carga máxima la mediana cruza `accUpThreshold`, es decir, el bucle le SUBE la dificultad. | Enunciado corregido y mecanizado (`MB-FOCUS-0137`, P1, OPEN). El rediseño del paradigma NO se ejecuta: ver 24.3. |
| Enunciado de H-07 (E10) | **CIERTO y EXACTO.** Barrido de 3 × 3 niveles de eje × 7 semillas = **3465 ensayos, cero no-objetivos**; `isTarget = true` y `correctResponseKey = PRIMARY` en todos. `FALSE_ALARM` y `CORRECT_REJECTION` son inalcanzables por construcción, así que la métrica gobernante sólo puede leer omisiones: pulsar en todo es un **1,0 confiado**, y el silencio es una ausencia honesta. | Enunciado confirmado y mecanizado (`MB-FOCUS-0138`, P1, OPEN). |
| Enunciado de H-08 (E8) | **CIERTO en su mecanismo, con una cláusula CORREGIDA.** Es cierto que ignorar el contador iguala las celdas y lleva `DUAL_TASK_COST` a `0.0`, su mejor valor posible; y peor: `FocusDualTaskField` no tiene callback, así que **incumplir la instrucción es inobservable**. Pero la cláusula «se emite con `confident = true` incondicional» sobre-afirma: el valor **sí** lleva su segundo sello de suficiencia (`measurementSufficient` no nulo), de modo que el defecto es de PARADIGMA (nadie puntúa la tarea secundaria), no de sellado. | Enunciado corregido y mecanizado (`MB-FOCUS-0141`, P1, OPEN). |
| Mitad A: «retirar la afirmación de la copy» | **EJECUTADA.** Tres descripciones embarcadas afirmaban una medición que ningún mecanismo produce (E8 decía registrar cuánto te cuesta repartir la atención; E9, cuántos objetos sigues a la vez; E10, cómo respondes al siguiente objetivo tras cada acierto). Reescritas a lo que el ejercicio hace de verdad; **las otras diez quedan byte-idénticas**. | Ejecutado (`MB-FOCUS-0139`, P2, CLOSED). |
| Mitad B: «retirar su métrica gobernante» | **REFUTADA por medición, antes de implementarla.** En Enfoque `ExerciseDefinition.governingMetric` **no tiene un solo lector de producción**: `DefaultAdaptiveEngine.performanceDecision` nombra sus tres claves directamente (`ACCURACY`, `RT_CV`, `FALSE_ALARMS`) y jamás consulta el campo; el único miembro vivo de la definición en este eje es `governingAxis`. Retirarlo **no cambia ningún comportamiento**, luego no es remedio de nada. Efecto lateral del hallazgo: el KDoc del campo afirmaba justo lo contrario, y se corrigió. | NO ejecutada. Refutación registrada (`MB-FOCUS-0143`, P3, CLOSED) y KDoc corregido (`MB-FOCUS-0140`, P3, MITIGATED). Precedente: R11 y R12 — una prescripción con premisa falsa se registra refutada, no se ejecuta al pie. |

### 24.2 Lo que R14 deja mecanizado

Los tres tests de aceptación que el propio informe pide para este bucket —**T5** (E9: la estrategia ciega no
puede caer en la banda 0,65-0,85), **T6** (E10: pulsar en todo no puede ser 1,0) y **T7** (E8: ignorar la
tarea secundaria no puede dar el valor óptimo)— **no se pueden satisfacer hoy**, porque los tres describen la
propiedad que el rediseño traería y el rediseño no se ejecuta. Se invierte su polaridad y se **pinnea la
limitación**, que es el patrón que este repo ya usa para una limitación que se levantará más tarde
(precedente 2026-08-19): el guard afirma que la estrategia ciega SÍ compra la banda, que pulsar en todo SÍ es
1,0 y que ignorar el contador SÍ da el óptimo, de modo que el día que alguien arregle el paradigma el guard
se pondrá ROJO y la aserción se **invertirá con evidencia** en vez de borrarse en silencio.

`FocusBlindStrategyTest` (5 tests, `desktopTest`). Ninguna cifra está escrita a mano: cada número se le pide
al productor real (`E9EnjambreGenerator`, `E10FlujoGenerator`, `E8DuoGenerator`) y se empuja por
`DefaultMetricsEngine`, con anti-vacuidad en cada bloque y su mitad discriminante (el silencio debe seguir
siendo una ausencia tipada). E9 se pinnea por **DISTRIBUCIÓN** —mediana sobre 7 semillas más la proporción de
semillas dentro de banda— y no por una semilla: el sondeo es un sorteo por ensayo, así que a 55 ensayos la
desviación típica del explotador es ≈ 0,064 frente a un borde de banda a 0,035, y un pin de una sola semilla
habría sido intermitente por diseño. Verificado por **cuatro mutaciones** con atribución 1-a-1 y restauración
comprobada por SHA-256: E10 con ensayos trampa, E9 con objetivos que escalan con los objetos,
`DUAL_TASK_COST` constante y `MB-FOCUS-0091` reintroducido.

### 24.3 Lo que R14 NO ejecuta, y por qué

El rediseño del paradigma —trayectorias que se cruzan en E9, ensayos no-objetivo y puntuación T1/T2 separada
en E10, superficie de entrada y puntuación de la tarea secundaria en E8— **cambia lo que la sonda mide**, así
que invalida toda línea base ya capturada con la forma actual. Es una decisión de producto con la misma
dependencia que `MB-FOCUS-0130`, no un paso de remediación: se registra con dueño (`MB-FOCUS-0137`, `0138`,
`0141`, los tres P1 y OPEN) en vez de ejecutarse dentro de esta fase.

### 24.4 Hallazgo que la prescripción no contenía

`E10FlujoGenerator` sortea `t2Index` con `hasT2Rate = 0,7`, de modo que aproximadamente el 30 % de los
ensayos **no llevan segundo objetivo**. Ese es exactamente el contraste que el paradigma existe para medir, y
el generador lo produce y lo descarta antes de puntuar: como todos los ensayos son objetivo (H-07), ninguno
cambia de clase por llevarlo o no, y la distinción no llega a ninguna métrica. Registrado como
`MB-FOCUS-0142` (P2, OPEN).

### 24.5 Techo declarado

El guard **pinnea el exploit, no lo cierra**. Mientras `MB-FOCUS-0137`, `0138` y `0141` sigan abiertos, las
métricas gobernantes de E8, E9 y E10 siguen siendo satisfacibles sin hacer la tarea, y el bucle adaptativo
sigue moviéndose sobre ellas. Lo que R14 garantiza es que ese hecho ya no es invisible: está mecanizado, está
registrado con dueño, y la copy embarcada ha dejado de afirmarlo.


## 25. Nota de vigencia — 2026-09-07 (bucket 60 d, R15)

Ejecutado `TASK-2026-09-07-ENFOQUE-REMEDIATION-60D-R15` contra la **prescripción** de R15 (§12, línea 590):
«Cablear `core/antigaming` a la ruta de evaluación, con su suite red-team (H-09)». El enunciado de H-09 se
verificó **antes** de tocar producción y resultó **CIERTO**: el grep de `AntiGaming`, `DetectorCoverage` y
`GamingDetector` sobre `focus/**` devolvía **cero**. La ubicación que la prescripción nombra resultó **más
estrecha que la correcta**, y esa diferencia se corrigió en vez de heredarse.

### 25.1 Dónde se cablea, y por qué no en «la ruta de evaluación»

La prescripción dice *evaluación*; el punto honesto es `FocusSessionAssembler.admit`, que la tarea Gradle
`focusQualityGateScanner` ya fija como **único** call-site de producción de `SessionQualityGate.admit` y que
cubre los **tres** carriles (entrenamiento, línea base y evaluación). Cablear sólo evaluación habría dejado
las otras dos lanes sin control mientras el informe las daba por cubiertas.

El veredicto **se DERIVA allí** —`FocusAntiGaming.evaluate(session.blocks.flatMap { it.trials })`— y el
parámetro `gaming` de `admit` es **obligatorio, sin default**: un default limpio reinstauraría en silencio la
lane sin puerta que R15 cierra, y derivarlo en el propio call-site impide que nadie entregue un veredicto
limpio para una sesión cuyos ensayos digan lo contrario. Una sesión no admitida por este motivo se sella como
`NotMeasurable(RESPONSE_PATTERN_NOT_INTERPRETABLE)` y pierde toda métrica confiada, igual que las otras causas.

### 25.2 Qué detector, y por qué el techo ciego no es 0,5

De los once detectores de `core/antigaming`, se cablean **dos** por telemetría (D3 `SpeedAccuracyHackDetector`
y D4 `TooFastToProcessDetector`) más uno **nuevo y específico de Enfoque**, `BlindStrategyCeilingDetector`.

En una tarea de detección, la mejor estrategia ciega no acierta el 50 %: acierta `max(p, 1-p)` sobre la
proporción de objetivos **servida** (en E1 a carga alta, responder a todo ronda 0,20 mientras el techo ciego
está en 0,80). Ése es exactamente el defecto que `MB-FOCUS-0097` registraba en el default plano de
`TrialTelemetry.chanceAccuracy`, y por eso el adapter lo pasa **explícito**. La mutación M4 (techo devuelto a
0,5) deja 4 tests en rojo.

Corolario que la prescripción no contenía: una planificación **degenerada** de una sola clase —exactamente la
forma de E10 según `MB-FOCUS-0138`— tiene techo ciego 1,0, así que el detector **IDLE** en vez de disparar.
Leerlo como un techo alcanzable acusaría a **todo** jugador de un defecto que pertenece al ejercicio. La
mutación M3 retira esa guarda y deja rojo exactamente ese test.

### 25.3 Hallazgo nuevo destapado por el propio cableado

Al cablear la puerta se pusieron **rojos nueve tests** que se creían honestos. Medido: `driveWithResponses` y
los bucles propios de `FocusQualityGateWiringTest` y `FocusSessionStateHolderTest` **respondían a todo campo
visible**, que en un carril go/no-go **es** la estrategia ciega. La salida correcta no era ablandar la puerta
sino hacer que el arnés **jugara** la tarea: `honestResponseTo` es ahora el dueño único de «qué contesta un
jugador honesto» y los tres arneses lo consultan. Registrado y cerrado como `MB-FOCUS-0145`.

Hay una segunda razón, menos visible, por la que dejar el arnés ciego habría sido inaceptable: como el veredicto
de patrón de respuesta **precede en la jerarquía** a `LIFECYCLE_NOT_OBSERVED`, los tests del sensor no observado
habrían pasado a cumplirse por la rama equivocada del mismo enum sellado — la vacuidad de 2026-09-03 otra vez, y
esta vez introducida por el arreglo.

### 25.4 Techo declarado

- La cobertura es **`DetectorCoverage.PARTIAL`, nunca `ADEQUATE`**, y el detector la **declara** mientras el
  consumidor la consulta (sin literal en el call-site, regla 2026-06-30). El motivo está **medido**, no supuesto:
  D1 necesita intervalos entre respuestas y el único tiempo que Enfoque persiste es `timestampMs` de reloj de
  pared, cuyo propio KDoc prohíbe derivar temporización de él; D6 necesita la tecla elegida, que `TrialResult`
  no conserva. Registrado como `MB-FOCUS-0147` (P2, OPEN).
- **R15 NO cierra `MB-FOCUS-0137`, `0138` ni `0141`.** La puerta retiene la sesión de quien responde a ciegas;
  no arregla que las métricas gobernantes de E8, E9 y E10 sean satisfacibles sin hacer la tarea.
- Una sesión **honesta** que de verdad rinde al nivel del techo ciego se retiene igual que una ciega: con lo
  que Enfoque persiste hoy nada las distingue. La dirección es fail-closed y **jamás acusatoria** —se retiene
  la lectura, no se emite ningún juicio sobre la persona—, y la consecuencia queda registrada como
  `MB-FOCUS-0146` (P3, OPEN) en vez de omitida.
- `MB-FOCUS-0097` pasa a **MITIGATED, no CLOSED**: R15 construye el adapter con consumidor vivo que pedía, pero
  la otra mitad de su gate —hacer `chanceAccuracy` obligatorio en `core/antigaming`— arrastra al adapter de
  Memoria y excede el alcance de esta fase.


---

## 26. Nota de vigencia — 2026-09-07 (bucket 90 d, R16–R20)

Ejecución del bucket 90 d (§12 R16–R20), un ítem por vez, con **FASE-0 que verifica ENUNCIADO y
PRESCRIPCIÓN por separado** contra el árbol de hoy. Resultado global: **tres de los cinco ítems se
ejecutan como corrección DOCUMENTAL con cero cambio de comportamiento**, uno se refuta y produce un
arreglo de copy embarcada que la prescripción no contenía, y uno se ejecuta en una forma **más
estrecha** que la escrita, porque la escrita era inejecutable como regla de personas. Ninguna
prescripción del bucket sobrevivió intacta a su medición.

### 26.1 · R16 — enunciado PARCIALMENTE CIERTO, prescripción NO EJECUTABLE

H-16 («el RCI no corrige por el cambio esperado en el retest») y H-17a («no corrige por juzgar varias
métricas a la vez») son **ciertos**. Lo que la prescripción no dice y la medición sí: (a) toda la ruta
del comparador está **INERTE** en producción —`ReliabilitySource.EMPTY`, y el carril de progreso además
pasa `repeatsEarlierForm = null`, que deja `formIsFresh = false`—, así que hoy todo resultado es
`NotEnoughData`; (b) el tamaño de la familia de métricas en la sonda servida es **1**, no nueve — el
«nueve» procede de un KDoc rancio de `TransferReportBuilder.label`, cierto cuando se escribió y falso
desde que cada métrica sella SU celda (`MB-FOCUS-0128`); (c) añadir un término de cambio esperado con
default `0.0` **AFIRMARÍA** que repetir la tarea no mueve nada, que es fabricar la respuesta halagadora
desde una ausencia, en la dirección que INFLA.

Ejecutado: se corrige el KDoc rancio y se DECLARA EL TECHO en `ReliableChangeIndex` (las dos
correcciones que la fórmula no aplica, con dueño: quien cablee el primer `ReliabilitySource` no vacío).
**Cero guard nuevo**: `ProbeMeasurementReachabilityTest` ya es dueño del invariante y un segundo guard
serían dos dueños.

### 26.2 · R17 — enunciado PARCIALMENTE CIERTO, prescripción con su mitad de guard REFUTADA

El hueco es real (el lado `pre` del RCI no pasa por el gate central de muestra), pero el párrafo del
propio código que generó la prescripción daba **dos razones y las dos son falsas**, medidas:

1. «el hecho de la muestra no es recuperable» — **falso**: `DefaultMetricsEngine.blockMetrics` sella
   `measurementSufficient` en cada valor y `DefaultCalibrationEngine` lo **DESCARTA** al mapear a un
   `Double` pelado. Se tira, no falta.
2. «es un cambio de schema con migración» — **falso**: `FocusJson` lleva `ignoreUnknownKeys` +
   `encodeDefaults`, y el precedente en el repo (`SessionSummaryDto.lifecycle` / `.admission`) es
   exactamente un campo opcional aditivo sin bump. Peor: un bump sería **activamente dañino**, porque
   `FocusSchema.CURRENT` es COMPARTIDO con el perfil y los resúmenes de sesión y `readVersioned`
   **cuarentena — borra —** toda clave con versión más nueva.

Lo que sí se sostiene es que estrechar el productor no es equivalente (`perMetric` tiene otros dos
consumidores vivos). Y el hueco está **ARMADO, no disparando**: en la sonda servida ACCURACY es la única
métrica que puede cruzar el gate de retest y su celda son los ensayos. La mitad de guard de la
prescripción **no se ejecuta**: `ProbeMeasurementReachabilityTest` ya pinnea ese invariante derivándolo
de `BaselineProtocols.INITIAL` y `MinSampleGate`, y ya se pone ROJO el día que la sonda se redimensione.

### 26.3 · R18 — enunciado CIERTO, prescripción NO EJECUTABLE · **riesgo declarado, no cerrado**

Verificado en fuente: el mecanismo de consentimiento (`FocusDataConsent`, `FocusJournalAdmission`) vive
solo en `focus2/`, sin un solo call-site de producción, mientras el carril V1 —el único que el hub
monta— tiene **seis** call-sites de persistencia sin gate. Los atenuantes de H-19 se corrigen en las dos
direcciones (§ corrección aplicada al propio H-19).

La prescripción **no se ejecuta**, por tres medidas: (a) **ADR-FOCUS-DATA está acotada a Focus v2** por su
propio `problem:`/`migration:`, así que no puede certificar las escrituras de V1 — las autoridades
correctas y agnósticas del motor son **PRI-001 / PRI-002 / PRI-008**, a las que la propia ADR apunta en
sus consecuencias; (b) la mitad de menores **no tiene base en el repo**: el único modelo de edad es
`MatchCandidate.isMinor`, en Arena, bajo un KDoc «Phase 10 (SIMULATION ONLY)» y sin productor de
producción; (c) `MB-FOCUS-0099` ya es dueño del hueco **y de su menú de disposición**, y
`GAP-FOCUS-RETENTION` (Decision Log, OPEN, BLOQUEA S8) ya es dueño de la retención — adjudicar aquí
crearía un segundo dueño de una decisión de producto.

**La decisión se documenta y no se toma**: el consentimiento del carril montado es una decisión de
producto/CEO, no del operador.

### 26.4 · R19 — enunciado CIERTO, prescripción FUERA DEL ALCANCE DEL REPO, defecto vivo encontrado

V0–V8 son gates de investigación con sujetos humanos; `GATE_CATALOG_V5.md` prohíbe explícitamente
sustituir un gate científico por uno técnico, así que la prescripción no es ejecutable desde el repo.
Pero medirla destapó copy **embarcada** que nombra lexemas prohibidos —negarlos no exime—, y en la
superficie más visible que existe: `BrainProfileUiModelBuilder.DISCLAIMER` se renderiza en el perfil de
la app **sin entrar en MindBoost**, y el hub (`MindBoostHubScreen`) afirmaba además que cada ejercicio
«mide», la afirmación que el propio catálogo de Enfoque acaba de retirar.

Causa raíz medida: `profile/presentation` y `presentation` estaban **fuera** de
`mindboostCopyFeatureModules`, y el test que pinneaba el disclaimer **EXIGÍA** el lexema
(`assertContains(disclaimer, "No es un diagnóstico")`) — un test que vuelve la copy inarreglable, la
clase de defecto de 2026-08-27.

Ejecutado: las dos cadenas se reescriben acotando positivamente (precedente `CreativityStrings`), el test
pasa a consumir `MindBoostCopySafetyScanner.scanAll` sin nombrar ningún lexema, y `presentation` entra en
el gate. **Medido antes de ampliar** (89 términos de la lista de paridad, auto-comprobación en ambas
direcciones): `presentation` 2 ficheros / 1 línea ofensora → 0 tras el arreglo. Quedan FUERA con su
recuento: **`profile`** (su etiqueta de dimensión «Regulación emocional» ES un literal de transferencia
lejana prohibido — colisión que es decisión de producto) y **`coach`** (33 líneas; su política de
seguridad debe NOMBRAR los términos que bloquea).

### 26.5 · R20 — enunciado SOBREACTUADO, prescripción con MECANISMO EQUIVOCADO

«CERO separación de funciones» es falso: el ledger registra a un revisor externo bloqueando un cierre
(:565), lleva evidencia `EXTERNAL_INDEPENDENT_AUDIT` (:524/:538) y GOV-007/008/009/012 asignan segundo
revisor. Y «quien implementa no cierra» es **inejecutable como regla de personas**: GOV-001 registra que
el CEO es el único humano, y un check sobre la columna Actor es falsificable — teatro de gobernanza.
Medido: los 66 eventos CLOSED llevan la sesión implementadora como actor y `validate-audit-ledger.mjs`
**nunca lee esa columna**.

Ejecutado, en la forma estrecha que sí es un mecanismo: una regla nueva
`ceo_owned_field_agreement` en `validate-authority-entrypoints.mjs` que compara el campo
**EXECUTION_AUTHORIZATION —propiedad exclusiva del CEO por `GATE_CATALOG_V5`—** entre las autoridades
VIVAS (entrypoints + índice actual; los documentos en cuarentena quedan fuera a propósito: su valor es
historia). **Falla en el árbol de HOY**, y ese fallo es el objetivo:

```
[CEO_FIELD_DISAGREEMENT] ADR_REGISTER_V5.md, CURRENT_AUTHORITY_INDEX.md
  NOT_GRANTED: ADR_REGISTER_V5.md | GRANTED: CURRENT_AUTHORITY_INDEX.md
```

**No se resuelve editando ninguno de los dos documentos**: el valor no es del operador. Se escala.
Además, ambos validadores de gobernanza eran **inalcanzables** desde `package.json`, `.github/**` y
Gradle; ahora son ejecutables (`check:governance`) y **deliberadamente NO se cablean a ningún workflow**,
porque bloquear cada PR sobre una decisión que no es del operador convierte una pregunta de gobernanza en
presión de build para editar un campo del CEO.

Dos hallazgos que la prescripción no contenía, confirmados y registrados sin arreglar: el índice llama
**VIGENTE** a `FOCUS_ENGINE_MASTER_PLAN_V5.md` mientras `authority-registry.json` lo marca
`LEGACY/QUARANTINED`; y de los dos documentos con sello `VOID_PROVENANCE`, uno está registrado y
hash-pinneado (`GATE_CATALOG_V5.md`) y el otro (`EXERCISE_PORTFOLIO_V5.md`) no está en el registro en
absoluto.


---

## 27. Nota de vigencia — 2026-09-07 (§13, plan de pruebas T1–T12)

Los doce ítems de §13 nacieron `PROPUESTA`. Se ejecutaron **en orden, uno por vez**, cada uno con su FASE-0
que verifica el **enunciado** y la **prescripción** por separado contra el árbol de HOY. El resultado es que
**nueve de los doce ya tenían dueño vivo o son insatisfacibles/dañinos tal como están escritos**, y sólo tres
correspondían a un hueco real. Implementar los nueve no habría añadido cobertura: habría creado un **segundo
dueño** de nueve invariantes que este repositorio ya trata como defecto.

### 27.1 Disposición ítem a ítem

| ID | Disposición | Dueño vivo / razón medida |
|---|---|---|
| T1 | YA CUBIERTO | Sus dos mitades viven en **capas distintas** y cada una ya tiene su dueño: la ausencia de ACCURACY con razón tipada en `FocusIndexHonestyTest.a_block_with_no_responses_does_not_publish_a_confident_accuracy` (`MetricAbsence.NO_RESPONSES`), y el `Hold` del motor en `AdaptiveAbsenceHoldTest.an_absent_accuracy_holds_and_never_steps_down`. Su mitad discriminante también: `a_measured_high_accuracy_still_steps_up`. Escribirlo como UN test habría duplicado los dos. |
| T2 | REFUTADO | Su prescripción es la **negación lógica exacta** de `FocusClaimLevelGateTest.everyProbeParadigmIsAlsoATrainedParadigm`, vivo y verde. Ya medido en R11: el enunciado es cierto y la prescripción es **estructuralmente insatisfacible** (`ExerciseCatalogConsistencyTest` fuerza `implemented == wiredExerciseIds`, así que toda sonda renderizable tiene por fuerza paradigma entrenado). Registrado en `MB-FOCUS-0124`. |
| T3 | REFUTADO | El mecanismo no existe para la sonda servida: `MeasurementReachability.verdictFor(E1_VIGIA, …)` devuelve **`null`**, no `reachable = false` —su tabla `cells` cubre sólo las seis comparativas con cuota y `RT_CV` es una métrica de una celda—, y el símbolo **no tiene ningún call-site de producción**. La mitad útil («negarse a emitir») ya tiene dueño en `EvaluationComparator` sobre la muestra REALIZADA. Medido en R12; el gate de cierre de `MB-FOCUS-0092` se reescribió por esta misma razón. |
| T4 | YA CUBIERTO | R13 ejecutó su contenido y más: la cuarta forma reservada (`ProbeBank.reservedForms` 3 → 4) y el tercer gate `formIsFresh` del estimador, con el guard **derivado de las DOS autoridades** en `ProbeBankTest` y el tri-estado del cursor en `LocalFocusRepositoriesTest`. |
| T5 | YA CUBIERTO, con polaridad INVERTIDA | `FocusBlindStrategyTest.e9_aSingleTokenResponseBuysTheMaintenanceBandWithoutTrackingAnything`. R14 midió que el enunciado ya no describe el árbol: «retener siempre» produce hoy una ausencia tipada, así que el exploit alcanzable es **una sola pulsación por bloque**, y el guard pinnea la LIMITACIÓN para que el día del rediseño se ponga ROJO y se invierta con evidencia. |
| T6 | YA CUBIERTO | `FocusBlindStrategyTest` fija que pulsar en todo SÍ da 1,0 (limitación pinneada), y R15 cableó el control real: `BlindStrategyCeilingDetector` en `FocusSessionAssembler.admit`, con su suite red-team `FocusAntiGamingTest` —incluida la guarda de planificación degenerada, que hace IDLE en vez de acusar—. |
| T7 | YA CUBIERTO, con polaridad INVERTIDA | `FocusBlindStrategyTest.e8_ignoringTheSecondDemandScoresTheBestPossibleCost` (R14). |
| T8 | **EJECUTADO** | Ver §27.2. |
| T9 | REFUTADO | Sus dos mitades son insatisfacibles: `cycleWeek` **no tiene ningún escritor de producción**, así que «tras N sesiones avanza» no puede probarse sin implementar antes la periodización, y el día de deload no existe como estado. El defecto real ya está registrado como `MB-FOCUS-0103` (P2, OPEN) — y esta fase **corrige su recuento**: los `copy(` de producción sobre el perfil que omiten `cycleWeek` son **CUATRO**, no tres (falta `FocusBaselineStateHolder.kt:221`, cuyo `outcome.profile` es un `UserCognitiveProfile` según `BaselineAssembler.kt:29`). |
| T10 | MITAD YA CUBIERTA, MITAD REFUTADA Y SUSTITUIDA | Ver §27.3. |
| T11 | YA CUBIERTO | Seis ficheros del carril montado lo poseen: `SessionQualityGateTest`, `FocusSessionAdmissionTest`, `FocusLifecycleWiringTest`, `FocusQualityGateWiringTest`, `FocusBaselineStateHolderTest` y `FocusEvaluationStateHolderTest`. La mitad discriminante también está viva (`FocusLifecycleWiringTest:109`, `assertNotEquals(MetricAbsence.INTERRUPTED, …)`). |
| T12 | YA CUBIERTO + ENDURECIDO EN SITIO | Ver §27.4. |

### 27.2 T8 — el hueco real: un `= null` que ningún guard textual podía ver

El enunciado («`AdaptiveInput` no compila sin fatiga, maestría e histéresis reales, sin default») resultó
**CIERTO como propiedad vigente y sin dueño mecánico**. `AdaptiveContractAbsenceGuardTest` ya vigilaba el
CALL-SITE con cuatro expresiones regulares (centinela de fatiga, histéresis saturada, mapa de maestría vacío,
señal construida en línea), pero **ninguna mira la DECLARACIÓN**: un `= null` en el parámetro deja los cuatro
patrones verdes y devuelve exactamente el vector que `MB-FOCUS-0071` nombra —un call-site futuro que SÍ produce
la señal puede omitirla en silencio, y ningún escaneo textual por fichero distingue «no se pasó» de «no se
midió»—.

Se añadió `theThreeSessionSignalsCarryNoDefault`, que lee la lista de parámetros de `AdaptiveInput` con los
comentarios ya retirados, exige que ninguna de las tres señales lleve `=` y **conserva su mitad discriminante**:
el default de `config` es legítimo, así que el guard no puede prohibir todo `=`. Lleva además su anti-vacuidad
(si `AdaptiveInput` deja de declarar alguna de las tres, el test dice que hay que mover el guard, no pasa) y su
mitad de «no leer de más» (un parámetro ausente devuelve `null`, no una cadena vacía).

**Mutación M1** (`val fatigue: FatigueSignal?` → `= null`): ROJO **sólo** en el test nuevo, 1 de 4, con el
mensaje que nombra el vector. Los tres guards preexistentes quedaron VERDES bajo la misma mutación — que es la
prueba de que eran ciegos a este defecto y de que el guard nuevo no duplica a ninguno. Restauración verificada
por SHA-256 (`a475f2ed5106`).

### 27.3 T10 — una mitad ya cubierta y otra que sería PEOR que la ausencia

La mitad de E6 («discriminador no cromático») **ya está cerrada** desde R10: la dimensión de color del campo de
alternancia ganó un segundo canal —la LUMINANCIA—, con `FocusFieldAchromaticTest` pinneando el par en un solo
sitio y su contra-presión de contraste contra el lienzo.

La mitad de los renderizadores («los 13 exponen nodo semántico») está **medida como dañina** y registrada en
`MB-FOCUS-0110`: en una tarea de discriminación visual una descripción o REVELA la respuesta —un disco go/no-go
anunciado por su color ES la respuesta— o se anuncia de forma asíncrona respecto al onset y el anuncio cae
dentro de la ventana de la que se deriva el tiempo de reacción, que es la entrada de la única afirmación de
capacidad del producto. No es un arreglo de una línea: lo que falta es una decisión de elegibilidad declarada, o
la modalidad secuencial que el carril V5 ya modela. Las dos son decisiones de producto, y ninguna es un test.

Lo que SÍ faltaba era el **dueño mecánico de la ausencia**: nada ponía en rojo el día que alguien añadiera un
nodo semántico, así que la decisión de `MB-FOCUS-0110` podía tomarse en silencio. `FocusStimulusSemanticsTest`
pinnea la LIMITACIÓN con polaridad invertida —un ROJO aquí **no es una regresión**, significa que la decisión se
tomó, y la aserción se invierte con esa decisión como evidencia, nunca se borra— y lleva su nota de que el
cambio debe aterrizar en las DOS copias de la capa de captura (`MB-FOCUS-0109`). Su mitad discriminante es que
el único componente del paquete que ES un control, `FocusGlyphButton`, sigue llevando su descripción y su rol de
botón (`MB-FOCUS-0108`); sin ella el guard lo satisfaría también un carril sin semántica en ninguna parte.

Autocorrección dentro de la fase: la **primera** versión del guard casaba contra el fichero entero, así que un
KDoc que mencionara `contentDescription` habría contado como violación — el defecto de «un extractor que lee de
MÁS informa falsos positivos que parecen rigor». Se corrigió antes de mutar, retirando comentarios de bloque y
de línea, y se añadió el tercer test que prueba **las dos direcciones**.

**Mutación M2** (nodo semántico añadido a `FocusStimulusField`): ROJO 1 de 3, en la aserción de ausencia.
**Mutación M3** (`Role.Button` retirado de `FocusGlyphButton`): ROJO 1 de 3, en la mitad discriminante. Cada una
cae en su propia aserción y en ninguna otra. Restauraciones verificadas por SHA-256 (`7e9fa8daf66e`,
`338e60bf4cca`).

### 27.4 T12 — cuatro dueños, y una aserción que se cumplía sin haber quemado nada

Devolver la forma reservada al abortar ya tiene cuatro dueños vivos (la primitiva `releaseProbe` en su
repositorio local y en el fake, y su invocación desde el `finally` bajo `NonCancellable` en los dos carriles de
medición). No se añadió ningún test.

El hueco real era de **vacuidad**: en el carril de evaluación, el test que afirma que la forma vuelve no
distinguía «la forma volvió» de «la forma nunca se tomó». La quema (`nextProbe`) precede a `administerProbe`, y
`emitRunning` es la única fuente de `FocusEvaluationUiState.Running`, así que afirmar `Running` **prueba que la
reserva ocurrió**. Se añadió esa aserción de anti-vacuidad; el gemelo del carril de línea base obtiene la misma
prueba de su bucle de espera.

**Mutación M4** (retirar el `runCurrent()` que hace arrancar la sesión, de modo que no se queme nada): ROJO
**sólo** en la aserción nueva, 1 de 8, con su mensaje —y las dos aserciones preexistentes del mismo test
quedaron VERDES, que es exactamente la prueba de que eran satisfacibles sin quemar—.

### 27.5 Techos declarados

- El guard de T8 es **textual sobre la declaración**: ve un default escrito, no ve un default introducido por
  un constructor secundario ni por una fábrica. La garantía dura es la del compilador sobre los call-sites
  existentes; el guard sólo impide que alguien la afloje editando la declaración.
- El guard de T10 mira los `*Field.kt` del paquete de componentes del carril montado. Un renderizador que
  naciera fuera de ese paquete o sin ese sufijo quedaría fuera; su anti-vacuidad (piso de 8 ficheros) detecta la
  desaparición del conjunto, no su desplazamiento.
- T9 queda **sin cubrir a propósito**: su test exige una periodización que no existe. La fila `MB-FOCUS-0103`
  sigue OPEN y es su dueño, ahora con el recuento corregido.
- Los nueve ítems refutados o ya cubiertos **no se marcan como implementados**: se marcan con su dueño, que es
  una afirmación más pequeña y verdadera en vez de una grande y falsa.


---

## 28. Nota de vigencia — 2026-09-13 (§14, checklist de re-auditoría)

**Condición de entrada, corregida.** §14 exige «los 12 tests de §13 existen, están en verde y cada uno se ha
verificado por mutación». Esa condición es **falsa por construcción** desde §27: nueve de los doce ítems de §13
NO deben existir como tests (tienen dueño vivo, o son insatisfacibles, o su remedio está medido como dañino), y
sólo T8, T10 y T12 produjeron código. La condición honesta —y la que se comprobó— es que **cada ítem de §13 tiene
dueño citado por nombre de test o refutación registrada en el ledger**, y se cumple (§27.1).

**Método.** Cada pregunta se trató como un prompt independiente: FASE-0 contra el árbol de HOY (símbolos,
call-sites y estados del ledger verificados uno a uno), ejecución de los tests dueños de lo afirmado, y después
una refutación adversarial con **quince agentes de sólo lectura** (`architect`: Read/Grep/Glob, sin Bash — el
mecanismo de `MB-FOCUS-0083`), una lente por pregunta y dos lentes para Q1, Q2, Q3, Q7 y Q8. Resultado: **doce
lentes CONFIRMAN** la respuesta de FASE-0 y **tres la REFUTAN PARCIALMENTE** (Q5, Q6, Q9); las tres correcciones
están integradas abajo, y ninguna invierte una respuesta. Evidencia de tests: 14 clases dueñas re-ejecutadas
(`--rerun`, 14 XML frescos, **92 tests / 0F / 0E**) y la suite completa `:shared:desktopTest --rerun` (**624 XML
frescos, 4468 / 0F / 0E**, `> Task :shared:desktopTest` sin sufijo, 46 s, delta 0 frente al checkpoint de §27),
con huella `git status` idéntica antes y después (`2c283a25cf3d3bc2`). **Ningún fichero Kotlin se tocó** en esta
fase; lo que cambia es el ledger y este informe.

### 28.1 Respuesta pregunta a pregunta

| # | Pregunta | Respuesta | Evidencia y corrección del refutador |
|---|---|---|---|
| 1 | ¿Los 2 BLOQUEANTES cerrados con evidencia y gate? | **NO** | `MB-FOCUS-0089` (H-01) es P0 **MITIGATED**: las mitades de copy y documento están mecanizadas, la del INSTRUMENTO sigue abierta y el rediseño quedó refutado en `MB-FOCUS-0124`. `MB-FOCUS-0092` (H-02) es **P1** en el ledger, no P0 —«BLOQUEANTE» es encuadre de este informe (§21.6/§23.4)—, MITIGATED hasta la decisión de `MB-FOCUS-0130`. El segundo P0 real es **`MB-FOCUS-0090`**, y ahí está el hallazgo de la fase: ver §28.2. |
| 2 | ¿Rediseñado el banco de sondas, o retirada la afirmación de cambio? | **NI LO UNO NI LO OTRO** | Rediseño: NO, refutado como estructuralmente insatisfacible en R11 (`MB-FOCUS-0124`; `ExerciseCatalogConsistencyTest` fuerza `implemented == wiredExerciseIds`). Afirmación: **retenida, no retirada** — el bloque `hasReliableChange` y la cabecera «Cambio observado» (`FocusEvaluationScreen.kt:137`) siguen en el código; son léxicamente C0-compatibles (`EFFICACY_CLAIMS_C0`, `ForbiddenLexicon.kt:243-250`) y están **muertos** detrás de `ReliabilitySource.EMPTY`. `retestClaimText` no contiene ninguna afirmación de cambio; el copy de sonda se acota a la práctica. |
| 3 | ¿La alcanzabilidad de la sonda se **calcula** y bloquea la emisión? | **NO se calcula; la emisión SÍ se bloquea, por otra vía** | `MeasurementReachability.verdictFor` tiene **cero call-sites de producción** y devuelve `null` para E1 (`MeasurementReachability.kt:147`: su tabla `cells` cubre sólo los seis ejercicios con cuota). Lo que bloquea es el gate sobre la muestra **REALIZADA**: `EvaluationResult.kt:92` (`measurementSufficient == true`) y `:103` (`== false` → `withheldForSample`), sellado por `DefaultMetricsEngine.kt:119/150/162-171` vía `MeasurementSufficiency.kt:70-72` → `MinSampleGate`. Es la refutación de T3 (§27.1) confirmada contra el árbol. |
| 4 | ¿Los tres parámetros de `AdaptiveInput` son obligatorios? | **SÍ** | `AdaptiveModels.kt:125/127/129` sin default (`:130` es el default legítimo de `config`); guard de la declaración `AdaptiveContractAbsenceGuardTest.kt:58-74` (T8, §27.2); único call-site de producción `FocusSessionStateHolder.kt:521-533`, que pasa **`null` explícito** por contención C0 declarada. Matiz confirmado: `DefaultAdaptiveEngine.decide` evalúa `impulsivityDecision` **antes** que `performanceDecision` y sin leer las tres señales, así que en go/no-go puede emitir `ContainImpulsivity` con las tres a `null`; el KDoc de `AdaptiveModels.kt:113-116` sobreafirma un `Hold` universal y `AdaptiveAbsenceHoldTest` no pinnea el paso previo. Registrado como `MB-FOCUS-0164`. |
| 5 | ¿Conciencia de ciclo de vida y marca de sesión interrumpida? | **SÍ, con techo declarado** | Sonda tipada (`AppLifecycleProbe`), `actual` Android real, cableada a los tres holders y persistida (`Results.kt:69/74`, `FocusDtos.kt:78/85`). **Corrección del refutador:** en el árbol embarcado el flag `focusLifecycleProbeEnabled` es `false` (`FeatureFlags.kt:132`), así que `RememberFocusStateHolders.kt:89` instala `DisabledAppLifecycleProbe` en **TODAS** las plataformas (no sólo NoOp en desktop/iOS) y toda sesión sale `NotMeasurable(LIFECYCLE_NOT_OBSERVED)` — lo que ya poseen `MB-FOCUS-0101` (P1) y `MB-FOCUS-0116` (P3); el techo desktop/iOS está registrado (`0101`/`0116`/`0114`), no es hallazgo nuevo. Precedencia de denegación `INTERRUPTED > NO_RESPONSES > RESPONSE_PATTERN_NOT_INTERPRETABLE > LIFECYCLE_NOT_OBSERVED` (`SessionQualityGate.kt:56-62`); `MetricAbsence.INTERRUPTED` no se persiste (techo declarado en FOC-P0-04). Dueños de la razón tipada: `SessionQualityGateTest.kt:42`, `FocusSessionAdmissionTest.kt:113`, `FocusQualityGateWiringTest.kt:133`, `FocusBaselineStateHolderTest.kt:181`, `FocusEvaluationStateHolderTest.kt:182`. El gate de cierre de `0101` ES una observación en ejecución: Q5 cuelga de Q10. |
| 6 | ¿Existe algún dato humano? ¿Con control activo? | **NO** (en Enfoque y en el árbol commiteado) | Sin dato humano de Enfoque, sin control, sin salida a la nube: `SyncingFocusSessionHistoryRepository` **no tiene ninguna instanciación de producción** y `FocusGraph.kt:160-161` cablea `LocalSessionHistoryRepository`. **Corrección del refutador:** los dos call-sites de `ObservabilityHub.breadcrumb` en `focus/` (`FocusScoreContainmentBreadcrumb.kt:26-28`, `SecureKeyValueStore.kt:159`) existen pero no transportan datos de sesión; las constantes normativas viven en `ReliableChangeIndex.kt:33-37` y `ProbeReliabilityEstimator.kt:6-8`, no en `EvaluationResult.kt`; y SÍ existen CSV humanos reales del carril de **Velocidad** con UID crudos en `validation/` (gitignored; `MB-OPS-001`/`MB-R-006`) — fuera del alcance de esta pregunta. El flag `mindboostSyncFirestoreEnabled` (default `false`, leído de Remote Config en `MindOSApplication.kt:139-142`) NO puede cablear egreso por sí solo: la ruta es inerte por construcción, y eso se anota en `MB-FOCUS-0062` en vez de abrir un ID. |
| 7 | ¿Declaración de accesibilidad y forma de expresar «no medible para este usuario»? | **PARCIAL** | Existe vocabulario de ausencia **por sesión** y elegibilidad **por modo** (`DefaultFocusEngine.kt:290-296`), pero **ninguna declaración por usuario**: `SessionQualityGate` (KDoc `:13-15`) declara la configuración de accesibilidad como TECHO, y `MB-FOCUS-0110`/`MB-FOCUS-0099` siguen OPEN. El modelo portable existe para `focus2` (`docs/engines/focus_v5/reaudit/v2/S5_R3_ACCESSIBILITY_DECISION_RECORD.md`), carril NO montado por el hub. Guards vivos verificados: `FocusFieldAchromaticTest` (luminancia sobre `ruleColorFill`), `FocusControlTargetTest` (`actual` sólo desktop), `FocusStimulusSemanticsTest` (regex `SEMANTIC_NODE` estrecho — techo ya declarado en §27.5). |
| 8 | ¿Ejecutado el diagnóstico `mejora vs cursor % 3`? | **NO, y su premisa es FALSA** | Ver §28.3. |
| 9 | ¿Separación de funciones: quien implementó no cerró? | **NO como regla de personas; SÍ en su forma ejecutable, que FALLA a propósito** | GOV-001 (`docs/context_pack/09_DECISION_LOG.md:282`): el CEO es el único humano. El validador **nunca lee la columna de actor** (`validate-audit-ledger.mjs:35-44`, `:59-63`). **Corrección del refutador:** de los 74 eventos CLOSED de §3, 61 llevan actor `primary session` y 13 `claude-code` — la columna no es uniforme (y el «66 eventos CLOSED» de la regla del 2026-09-07 en `CLAUDE.md` está rancio: hoy son 74); ambos actores son la sesión implementadora, así que la sustancia se mantiene. La forma ejecutable es `validate-authority-entrypoints.mjs:148-170` (`ceo_owned_field_agreement`): `npm run check:governance` → ledger `OK, 321 findings` y **`AUTHORITY CHAIN FAILED [CEO_FIELD_DISAGREEMENT]`, exit 1** (`ADR_REGISTER_V5.md:4` = `NOT_GRANTED` frente a `CURRENT_AUTHORITY_INDEX.md:30` = `GRANTED`). Es `MB-FOCUS-0155` (P1, dueño CEO, «no se cierra editando ninguno de los dos documentos»), deliberadamente **no cableado** a CI ni Gradle (`MB-FOCUS-0156`). Hechos nuevos anotados en `0155`: el vocabulario de actor no está normalizado, y `GATE_CATALOG_V5.md:4` (también `NOT_GRANTED`) queda fuera del check sólo porque `authority-registry.json:20` lo marca LEGACY — el alcance de la regla lo decide el registro. |
| 10 | ¿Se ha observado el sistema **en ejecución**? | **NO** | `adb devices` no lista ningún dispositivo; toda mención de ejecución en el ledger y en este informe es una **negación** (`MINDBOOST_AUDIT_LEDGER.md:31/251/766/953/954`; aquí `:111/:142/:708/:741`). El refutador añadió dos hechos: (a) `MINDBOOST_AUDIT_LEDGER.md:251` designa `PORTFOLIO_RUNTIME_EVIDENCE.json` como la autoridad donde una corrida Android debe registrarse, y **ese fichero no existe en el árbol** (ni trackeado ni ignorado; el checkpoint de 2026-07-15 lo declaró «nuevo») — registrado como `MB-FOCUS-0165`; (b) las corridas de emulador de Velocidad de 2026-06-14/15 (`PHYSICAL_VALIDATION_GATE.md:10-12`) atravesaron por fuerza `MindBoostHubScreen` y nadie anotó la tarjeta de Enfoque: evidencia barata de Q10 que no se capturó. |

### 28.2 Q1 — el segundo P0 llevaba quince días cerrado bajo otro nombre

`MB-FOCUS-0090` (P0, OPEN, sin un solo evento desde su alta el 2026-08-29) tiene un gate de cierre de tres partes,
y **las tres tienen evidencia en el árbol de hoy**:

1. *«desacoplar el número de formas del umbral de administraciones»* — `ProbeBank.kt:44/53/62` reserva **cuatro**
   formas por sonda frente a `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS = 3` (`:50`), con el guard DERIVADO
   de las dos autoridades (R13, `MB-FOCUS-0122`).
2. *«revertir el cursor si la sonda no completa»* — `releaseProbe` desde el `finally` bajo `NonCancellable`
   en los dos carriles (`FocusEvaluationStateHolder.kt:152-164`, `FocusBaselineStateHolder.kt:164-167`;
   `MB-FOCUS-0117`).
3. *«test de 4 administraciones consecutivas (la 4ª forma distinta de la de baseline)»* —
   `ProbeBankTest.theFirstGatedRetestNeverLandsOnTheBaselineForm` (`:119-137`): sirve `MIN + 1 = 4`
   administraciones sobre `ProbeBank.all` y exige `served.last().formSeed != served.first().formSeed` y
   `!firstGated.repeatsEarlierForm`.

El trabajo se contabilizó contra `MB-FOCUS-0122`, registrado **NEW en R9** (evento `:811`) con el mismo
enunciado que `0090` («`MIN_ADMINISTRATIONS` vale 3 y `reservedForms` tiene 3 entradas … se registra porque el
acoplamiento tres-igual-a-tres no está declarado en ningún sitio») y **sin ninguna referencia a `0090`** en sus
dependencias — mientras `0090` ya lo declaraba desde el 2026-08-29. Es un duplicado bajo otro nombre, la clase de
defecto que este ledger prohíbe en su §1 y que ya tuvo precedente en `MB-FOCUS-0113`.

**Disposición.** `MB-FOCUS-0090` pasa a **CLOSED** citando `0117` + `0122` + las re-ejecuciones frescas de
`ProbeBankTest`, `LocalFocusRepositoriesTest` y `FocusEvaluationServiceTest`, **y citando su techo**: el KDoc de
`ProbeBankTest.kt:112-116` declara que la aritmética corre sobre el cursor de quemado de formas mientras
producción abre la puerta sobre el recuento de filas **admitidas**, así que fuera del camino todo-admitido el
primer retest con puerta abierta puede seguir cayendo en material repetido y sólo lo retiene el gate
`formIsFresh == false` (`MB-FOCUS-0135`, OPEN, que sigue siendo el dueño de ese resto). `0122` gana la referencia
cruzada que le faltaba. El defecto de proceso se registra como `MB-FOCUS-0163` (P3) y se cierra en la misma
escritura, porque su gate es exactamente esta reconciliación. De paso, dos correcciones de metadatos: el
`Last update` de `0089` decía 2026-08-30 con su transición OPEN→MITIGATED fechada el 2026-09-03T08:20Z
(`:787`) — el validador no comprueba que `Last update` ≥ fecha del último evento —, y la frase de este informe
en la línea 1008 («los dos P0 siguen abiertos») sólo es cierta leyendo «abiertos» como «no CLOSED».

**Consecuencia sobre el RCI, integrada en `MB-FOCUS-0102` (no ID nuevo).** El refutador de código confirmó que la
retención del `strongEvidence` la producen **tres** capas de defaults anidados, no dos: `FocusGraph.kt:156`
(`FocusEvaluationService()` sin argumentos) → `FocusEvaluationService.kt:17` (estimador por defecto) →
`ProbeReliabilityEstimator.kt:35` (`ReliabilitySource.EMPTY`), más una cuarta latente en
`FocusProgressStateHolder.kt:34` (hoy sobreescrita por `RememberFocusStateHolders.kt:36`). **Ningún test pinnea
que producción cablee `EMPTY`**: `FocusEvaluationServiceTest.kt:26-42` fija el default del constructor, y
`ProductionFocusGraphWiringTest.kt:38-53` sólo escanea `focusEngine =` y `seedSource =` en `rememberFocusGraph`
— una línea más en ese escáner es su dueño natural. El día que alguien cablee una fuente de fiabilidad, el literal
fuerte se embarca sin ningún gate adicional: la retención por defaults es **contención**, no gate.

### 28.3 Q8 — el diagnóstico no se ejecutó porque su premisa no se sostiene

§13 pedía correlacionar la mejora reportada con `cursor % 3` sobre «el dato ya persistido». Medido en el árbol:
**no existe identidad de forma por fila**. `SessionResult`, sus DTOs, los mappers y el DTO de nube no llevan
`formSeed` ni índice de forma; lo único que persiste es el **contador** del cursor (`userId:probeId → Int`), y
`recordProbe` indexa por `endedAtMs` sin `probeId`, así que reconstruir el ordinal de cada administración desde
el historial es **insostenible** (dos sondas distintas en la misma ventana serían indistinguibles). Además el
periodo ya no es 3 sino `reservedForms.size = 4`, y la propia pregunta «¿la mejora reportada correlaciona con la
forma repetida?» está pre-vaciada por `formIsFresh == false` (R13): una administración repetida **no puede**
sostener una afirmación de cambio fiable, y con `EMPTY` cableado ninguna administración la sostiene hoy, así que
el diagnóstico sería ciego incluso con el dato. Todo esto se integra en `MB-FOCUS-0121` (P3, OPEN), que ya
pedía «volver con su CONSUMIDOR»: el consumidor nombrado es este diagnóstico, y su gate se reescribe con el
periodo real. No se abre ID nuevo.

### 28.4 Techos declarados

- Q1: `0090` se cierra por sus tres partes, no por el invariante completo — el resto (apertura de la puerta por
  filas admitidas frente a cursor de quemado) sigue en `MB-FOCUS-0135`.
- Q3 y Q8: la alcanzabilidad **planificada** de la sonda no se calcula en ningún sitio de producción, y el
  diagnóstico de forma repetida no tiene dato; ambos quedan con dueño (`MB-FOCUS-0130`, `MB-FOCUS-0121`) en vez
  de implementarse en una fase de re-auditoría.
- Q5 y Q10 cuelgan del mismo hecho: la observación en ejecución no es de esta sesión (entrar credenciales en el
  emulador o el dispositivo es del fundador), y el gate de `MB-FOCUS-0101` es exactamente esa observación.
- Q9: la regla ejecutable falla por diseño y **debe seguir fallando** hasta que el CEO unifique el campo; no se
  cablea a ningún gate de build.
- Los refutadores no ejecutan Gradle (sin Bash): todo conteo de tests de esta nota proviene de la sesión
  principal, con sus XML copiados fuera del árbol antes de lanzar los revisores.

### 28.5 Veredicto de la re-auditoría

`PROVISIONAL`, por el gatillo duro de Q10. No se recalcula la puntuación ponderada de §7: **ninguno** de los
dos ejes que la mueven (dato humano con control, sistema observado en ejecución) ha cambiado de estado, y una
cifra recomputada sin ellos sería la falsa precisión que §7 advierte. Lo que sí cambia es el registro: un P0 que
llevaba quince días cerrado bajo otro nombre deja de figurar como abierto, y las cuatro filas que lo rodean
(`0102`, `0121`, `0122`, `0089`) dicen ahora lo que el árbol dice.

---

*Fin del informe.*
