# Auditoría Memoria

## Auditoria del modulo Memoria — comite MindBoost

> GROUND-TRUTH verificado en `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/**`:
> - **Solo 3 generadores wired**: `Ex1SpanGenerator`, `Ex3ListGenerator`, `Ex4PairsGenerator` (`MemoryGraph.implementedGenerators()` = EX1/EX3/EX4). El catalogo declara EX1–EX13+, pero EX2/EX5/EX6/EX7/EX8 son **definiciones sin generador**; N-Back y la Sonda UNTRAINED **no existen como `MemoryExerciseId` ni generador** (solo diseño).
> - **RT sobre `EpochClock.nowMillis()` (wall-clock epoch, NO autoritativo)**: `MemorySessionStateHolder.kt:105,153`; `MemoryMetricsEngine` computa `RETRIEVAL_SPEED` desde `rtMs`. No hay Choreographer/MonotonicClock/anticipation-floor en Memoria.
> - **`MIN_N = 4`** (gate debil) en `MemoryMetricsEngine`. `MemoryMetricValue.unavailable()` devuelve `value=0.0, confident=false` (riesgo de 0-sentinela; el accessor respeta `confident`, pero el campo crudo es 0).
> - **`ReliableChangeIndex` / `RealImprovementCriterion` / `RciResult` NO tienen ningun caller** fuera de `domain/evaluation/`: los motores de honestidad existen pero estan **desconectados** (ningun probe/scheduler los invoca). La unica "evidencia de mejora real" del diseño no esta cableada.
> - `TransferProbePartition` reserva material UNTRAINED de forma determinista, pero su KDoc dice que se consume "solo por Phase-8" (**no implementada**) → el holdout nunca se sirve.
> - Banco de contenido Spanish: ~38 lineas de words/pairs autorados, **sin IRT/inter-rater/exposure/holdout activo**; idem patron del resto de MindBoost (cero validacion empirica).
> - Cero anti-gaming real en Memoria: no hay anticipation-floor, no hay deteccion de criterio extremo, no hay d' implementado en el engine para recognition, no hay cadencia.

---

### EX1 · Span Adaptativo Dual (complex-span; rediseño de EX1, GENERADOR EXISTE)

1. **Veredicto: REVISE.** Generador real (`Ex1SpanGenerator`), pero el diseño premium (validez por processing >=85%, RT de processing, dual-task cost, sonda UNTRAINED) NO esta cableado. Jugable; profile-update debe quedar gated.
2. **Constructo:** memoria de trabajo ejecutiva verbal (mantenimiento bajo carga de procesamiento concurrente; complex/operation span).
3. **Señal primaria valida:** `WORKING_MEMORY_SPAN` = aciertos seriales por longitud, **solo** en trials con processing-accuracy >=0.85. Es accuracy-based (no RT) → no depende de timebase, **puede** actualizar perfil si pasa min-n y validez.
4. **Señales secundarias:** `SERIAL_ORDER_ACCURACY`, `INTRUSION_RATE`, `OMISSION_RATE`, CV de span inter-bloque (feedback metacognitivo interno, no perfil).
5. **Contaminantes:** habilidad aritmetica/educativa del processing (confound cultural); sacrificio del processing para proteger memoranda; primacia/recencia; verbalizacion. **Redundante con** EX2-Updating y N-Back (los 3 cargan WM-ejecutiva).
6. **Ataques de gaming:** ignorar el processing y blindar memoranda; subvocalizar rapido el span y responder processing al azar; aprender la estructura aritmetica fija; usar dedos/notas externas para el orden.
7. **Gates obligatorios:** validez processing >=85% (trial invalido si no → NO alimenta staircase); min-n de trials validos por bloque; deteccion de criterio "todo processing al azar"; **NO** profile-update por RT (RETRIEVAL_SPEED es proxy bajo epoch-clock → marcar `device_unreliable`/UNTRUSTED_TIMEBASE); anti-memorizacion del banco aritmetico (rotacion).
8. **Metrica/scoring:** `spanScore = sum(serialCorrect/spanLength · validez)`; `dualCost = span_single − span_dual`; `IES_proc = meanRT/(1−err)` SOLO como secundaria interna (no perfil, RT no autoritativo). n_min: >=8 trials validos/bloque, >=2 bloques. Invalidaciones: processing<85%, RT<anticipation-floor (HOY ausente). `null`/calibrating si trials validos < umbral. Agregacion inter-sesion via `MemorySessionSummary` promediado ponderado por trials. Actualiza perfil solo cuando span estable y validez OK; la claim de **mejora** requiere sonda UNTRAINED (no implementada).
9. **Banco:** memoranda desde `WordPool` (autorado pequeño). Falta: tamaño min (>=200 palabras equilibradas por categoria/frecuencia/longitud), generacion procedural del processing, **inter-rater NO aplica** (scoring determinista), holdout via `TransferProbePartition` (existe reserve, no se sirve), rotacion via `RecentItemRegistry` (existe), HIGH_PRIOR n/a, anti-memorizacion = `excludeRecentWordIds` (existe). Banco aritmetico sin calibrar por dificultad numerica.
10. **Copy permitido:** "memoria de trabajo bajo carga", "manten palabras mientras resuelves", "progreso contra tu baseline", "estimacion preliminar", "datos insuficientes para confirmar".
11. **Copy prohibido (bloquear en tests):** "mejora tu inteligencia", "mas concentracion en el dia a dia", "transfiere a estudios/trabajo/multitarea", "test de IQ", cualquier far-transfer a vida real/decisiones.
12. **Impl KMP:** reutiliza `ComplexSpanTrial`/`SpanStep`/`Memorandum`/`ProcessingTask` (`MemoryTrialModels.kt`) + `Ex1SpanGenerator`. FALTA: captura de RT de processing con deadline + flag de validez por trial; persistir solo `MemorySessionSummary` (ya). Test hooks: generador determinista por seed (existe patron). Serializacion `@Serializable` + `schemaVersion` (patron del modulo).
13. **Tests obligatorios:** unit del generador determinista (seed→secuencia); **property** (validez monotona: trials <85% nunca suben staircase); **simulation** (perfil "abandona processing" → span no sube); **adversarial** (responder processing al azar → bloque baja calidad); **golden-scoring** del spanScore; copy-scanner (ver §11). Faltan TODOS los adversariales/property (no existen en Memoria).
14. **Aceptacion para APPROVE:** (a) staircase alimentado SOLO por trials validos, testeado; (b) RETRIEVAL_SPEED no actualiza perfil sin timebase autoritativa (gate); (c) reading-span rotatorio para neutralizar confound aritmetico; (d) claim de mejora ligada a sonda UNTRAINED implementada.
15. **Rediseño:** mantener complex-span como esta, pero (i) processing por defecto = **reading-span** (juicio de plausibilidad de frase) para matar el confound numerico, con operation-span como rotacion; (ii) deadline de processing **calibrado al RT base del usuario**, no fijo; (iii) anticipation-floor (~150 ms) sobre la respuesta de processing; (iv) span reportado al perfil solo con validez OK y solo accuracy; (v) la palabra "mejora" reservada a la sonda UNTRAINED.
16. **Impacto esperado:** alto (dentro del entrenamiento; complex-span es el paradigma WM mas defendible).
17. **Prioridad: P1** (ya jugable; cerrar gates de validez + timebase).
18. **Riesgo residual:** practice-effect medio del processing; sin sonda UNTRAINED cableada, "progreso" = familiaridad, no capacidad.

---

### EX_NBACK · N-Back Adaptativo de Doble Modalidad (DISEÑO-ONLY)

1. **Veredicto: BLOCK.** No existe `MemoryExerciseId.EX_NBACK`, ni generador, ni d' en el engine. Ademas el n-back es el paradigma con peor reputacion cientifica de far-transfer del sector. Bloqueado hasta tener d' honesto cableado y copy sin claims de inteligencia.
2. **Constructo:** *updating* de WM (matching deslizante n-back), distinto del mantenimiento del span.
3. **Señal primaria valida:** `RECOGNITION_DPRIME` = z(hit)−z(fa) (sensibilidad, NO hit-rate crudo) + `nEffective` (max n con d'>=1.0 sostenido). Accuracy-based → no depende de timebase para la señal de perfil.
4. **Señales secundarias:** `RESPONSE_CRITERION` (sesgo), `DUAL_TASK_COST` single vs dual, CV de d' (estabilidad).
5. **Contaminantes:** criterio de respuesta (un d' "alto" puede venir de sesgo); familiaridad vaga sin lures; ritmo/cadencia de ISI fija. **Redundante con** EX1-Span y EX2-Updating (varianza WM-ejecutiva compartida).
6. **Ataques de gaming:** "responder siempre no" (infla accuracy si target-rate baja → neutralizado por d'); matching laxo por familiaridad (neutralizado por lures n±1); respuesta ritmica al ISI fijo (neutralizado por jitter); criterio extremo.
7. **Gates obligatorios:** target-rate fija ~25–30%; lures n±1 presentes; ISI con jitter; **descartar bloques con criterio extremo** (siempre si/siempre no); min-n de targets/lures para z estable; RT no actualiza perfil (epoch-clock).
8. **Metrica/scoring:** `dPrime`, `criterion`, `nEffective`, `dualCost` (formulas del diseño). n_min: targets y FA suficientes para z (>=8 targets, >=8 no-targets/bloque); usar correccion log-linear de Hautus para hit/fa = 0/1. Invalidaciones: criterio extremo, target-rate fuera de rango. `null`/calibrating hasta z estable. Agregacion: n sostenido inter-sesion. Perfil: solo d' honesto, jamas n alcanzado como "nivel".
9. **Banco:** stream procedural (posiciones grid 3×3 + sonidos), `stimulusSetSize` 6→9. Falta generador entero. Holdout via partition. Anti-memorizacion: jitter + set rotatorio.
10. **Copy permitido:** "updating de memoria de trabajo", "decide si coincide con hace n", "progreso vs tu baseline".
11. **Copy prohibido:** "entrena tu inteligencia", "sube tu IQ", "mejora tu foco general", "transfiere a Gf/razonamiento/vida real". (El n-back es el caso clasico de over-claim pseudocientifico.)
12. **Impl KMP:** nuevo `MemoryExerciseId.EX_NBACK`, `NBackTrialPlan`/`NBackItem`/`NBackModality`, `NBackGenerator : MemoryTrialGenerator` en `implementedGenerators()`. Engine necesita **calculo de d' real** (hoy ausente en `MemoryMetricsEngine`). Persistir `MemorySessionSummary`.
13. **Tests:** unit (generador determinista, target-rate, lure placement); property (d' invariante a permutaciones de no-relevantes; criterio extremo → bloque invalido); simulation (perfil "siempre no" → d'≈0, no n alto); adversarial (waiting/lax-matching); golden de d'; copy-scanner duro contra léxico de inteligencia.
14. **Aceptacion:** (a) d' + criterio implementados y testeados; (b) descarte automatico de criterio extremo; (c) claim de mejora SOLO via sonda UNTRAINED; (d) copy-scanner que prohiba IQ/inteligencia/Gf en este modulo.
15. **Rediseño:** si se construye, **single n-back** (posicion o auditivo) por defecto, dual como opcion avanzada; d' obligatorio; feedback diferido por bloque (reduce andamiaje); ISI jitter. Si el equipo no puede sostener d' honesto + copy estricto → **RETIRE** (la redundancia con span/updating no justifica el riesgo cientifico).
16. **Impacto esperado:** medio (señal WM-updating limpia si d' honesto; cero far-transfer defendible).
17. **Prioridad: P2** (constructo cubierto parcialmente por span/updating; alto riesgo de claim).
18. **Riesgo residual:** alto riesgo reputacional/cientifico de over-claim; practice-effect medio-alto; fatiga en dual.

---

### EX2 · Updating de Ranura / Running Memory (CATALOGO SIN GENERADOR)

1. **Veredicto: REVISE.** `EX2_ACTUALIZACION_SET` existe en catalogo (`MemoryParadigm.RUNNING_MEMORY`) pero **falta el generador** (no en `implementedGenerators()`). Paradigma valido y bien aislado; necesita construirse con las dos metricas de error separadas.
2. **Constructo:** *memory updating* puro (mantener el valor MAS RECIENTE de k ranuras, inhibir lo obsoleto). Binding ranura↔valor explicito.
3. **Señal primaria valida:** `WORKING_MEMORY_SPAN` (k efectivo con accuracy >=0.75) + `INTRUSION_RATE` (valor obsoleto). Accuracy-based → apto para perfil.
4. **Señales secundarias:** `BINDING_ERROR_RATE` (ranura equivocada), `OMISSION_RATE`, CV de k.
5. **Contaminantes:** confusion del usuario con n-back (UX); carga de output con probeAll; conteo de updates si el numero es fijo. **Redundante con** EX1-Span y N-Back (WM-ejecutiva). Diferenciador clave: binding explicito + estado final.
6. **Ataques de gaming:** contar updates por ranura (neutralizado por numero variable de updates/ranura); responder al ultimo visto sin tracking (atrapado por fillers a ranuras inexistentes); preparar solo algunas ranuras (atrapado por sonda de UNA ranura aleatoria).
7. **Gates obligatorios:** fillers obligatorios (a ranuras inexistentes); updates/ranura variables; sonda por defecto de UNA ranura; banco de respuesta mezclado; min-n de probes; separar formalmente intrusion vs binding error.
8. **Metrica/scoring:** `kEffective`, `intrusionRate = oldValue/probes`, `bindingErrorRate = wrongSlot/probes`, `updatingScore = 1 − (intrusion + 0.5·binding)`. n_min: >=8 probes validos. Invalidaciones: respuesta a fillers (automatismo) → bloque baja calidad. `null`/calibrating si probes<umbral. Agregacion: k sostenido inter-sesion. Perfil: k + intrusionRate, ambos accuracy-based.
9. **Banco:** procedural (valores de set pequeño/parecido), sin contenido autorado. Anti-memorizacion: `excludeRecent`. Holdout via partition (no servido).
10. **Copy permitido:** "recuerda el ultimo valor de cada ranura", "actualiza y descarta lo viejo", "progreso vs baseline".
11. **Copy prohibido:** "mejora tu multitarea", "recuerda mejor en la vida real", "transfiere a tareas pendientes del trabajo" (far-transfer afirmativo).
12. **Impl KMP:** `SlotUpdate`/`UpdatingTrialPlan` (con `expectedFinal` como scoring-key embebida, patron `MemoryTrialPlan`), `Ex2UpdatingGenerator` en `implementedGenerators()`. Persistir `MemorySessionSummary`.
13. **Tests:** unit (generador determinista, fillers presentes, updates variables); property (intrusion y binding mutuamente exclusivos por probe); simulation (perfil "ultimo visto" → intrusionRate alto); adversarial (respuesta a fillers); golden de updatingScore; copy-scanner.
14. **Aceptacion:** (a) generador + dos metricas de error separadas y testeadas; (b) fillers + sonda-de-una obligatorios; (c) doc del catalogo que diferencie de n-back; (d) sin claim far-transfer.
15. **Rediseño:** construir tal cual el diseño, con `probeOne` por defecto (señal limpia) y `probeAll` solo como variante; running-computation (operaciones +/−) como `DEPTH` opcional. Mantener intrusion/binding separadas es el nucleo de su valor.
16. **Impacto esperado:** medio-alto (señal de updating limpia y disociable del span).
17. **Prioridad: P1** (constructo distinto, barato de construir sobre infra existente).
18. **Riesgo residual:** practice-effect medio; solapamiento perceptual con n-back si la UX no deja claro "estado final".

---

### EX5 · Grid Espacial con Binding (CATALOGO SIN GENERADOR)

1. **Veredicto: REVISE.** `EX5_GRID_ESPACIAL` en catalogo (`governingMetric = COWAN_K`) pero **sin generador** y **sin Cowan's K implementado** en el engine. Visoespacial genuino solo si se bloquea el recoding verbal.
2. **Constructo:** WM visoespacial con binding objeto↔posicion (que objeto en cada celda).
3. **Señal primaria valida:** `COWAN_K = c·(hit−fa)` en change-detection; `visuospatialScore = locationAcc · bindingAcc` en reconstruccion. Accuracy-based → apto para perfil.
4. **Señales secundarias:** `BINDING_ERROR_RATE`, swap-rate, `DELAYED_RECALL_ACCURACY`, CV de K.
5. **Contaminantes:** **verbalizacion** ("circulo rojo arriba-izquierda") → mide memoria verbal disfrazada; sesgo de centro del grid; rehearsal sin distractor. **Redundante con** EX6-Change-detection (comparten COWAN_K y misma varianza visoespacial) → candidato a MERGE.
6. **Ataques de gaming:** recoding verbal del layout (neutralizado por objetos sin nombre facil + alta similitud cromatica/forma); adivinar "algo cambio" sin binding (neutralizado por reconstruccion completa); rehearsal sostenido (neutralizado por grid distractor).
7. **Gates obligatorios:** objetos de baja verbalizabilidad + alta similitud; reconstruccion completa (no solo "¿cambio?"); d' (no hit-rate) en change-detection; deteccion de random-placement (accuracy≈chance → baja calidad); posiciones balanceadas; grid distractor para rehearsal.
8. **Metrica/scoring:** formulas del diseño (`cowanK`, `locationAcc`, `bindingAcc`, `swapRate`). n_min: trials suficientes para K estable y c>=3. Invalidaciones: accuracy≈chance, c demasiado bajo. `null`/calibrating si trials<umbral. Perfil: K + bindingAcc.
9. **Banco:** procedural (layouts), objetos visuales de baja verbalizabilidad (FALTAN: hoy el contenido es verbal Spanish). Anti-memorizacion: `excludeRecent`. Holdout via partition.
10. **Copy permitido:** "recuerda que objeto estaba en cada casilla", "memoria visoespacial", "progreso vs baseline".
11. **Copy prohibido:** "mejora tu orientacion", "navegas mejor en la vida real", "memoria fotografica", far-transfer espacial.
12. **Impl KMP:** `GridCell`/`GridBindingTrialPlan` (con `expected` scoring-key), `Ex5GridGenerator`. Engine necesita **Cowan's K + d'** (ausentes). Persistir `MemorySessionSummary`.
13. **Tests:** unit (generador determinista, balance posicional); property (location/binding/swap mutuamente exclusivos); simulation (perfil "random placement" → K≈0); adversarial (verbalizacion: objetos verbalizables vs no → diferencia de recoding); golden de K; copy-scanner.
14. **Aceptacion:** (a) Cowan's K + d' implementados y testeados; (b) objetos de baja verbalizabilidad obligatorios; (c) tres errores (location/binding/swap) separados; (d) sin claim de orientacion/navegacion.
15. **Rediseño:** construir con set de objetos abstractos no-nombrables (gabor/formas/tonos vecinos), change-detection con d' como variante de bajo practice-effect, reconstruccion como modo de maximo binding. **Considerar MERGE con EX6** (misma varianza): un solo ejercicio "Grid visoespacial" con dos modos (reconstruccion / change-detection).
16. **Impacto esperado:** alto (cubre el canal visoespacial, ausente en EX1-3).
17. **Prioridad: P1** (unica via para WM visoespacial en el modulo).
18. **Riesgo residual:** recoding verbal residual; drag en pantallas pequeñas; redundancia con EX6.

---

### EX6 · Deteccion de Cambio (CATALOGO SIN GENERADOR)

1. **Veredicto: MERGE.** `EX6_DETECCION_CAMBIO` (gobierna `COWAN_K`) mide la MISMA varianza visoespacial que EX5; no es un ejercicio aparte, es el **modo change-detection de EX5**. Fusionar como variante, no como entrada separada del catalogo.
2. **Constructo:** WM visoespacial (capacidad K) via deteccion de cambio (paradigma Luck & Vogel).
3. **Señal primaria valida:** `COWAN_K = c·(hit−fa)` con d' honesto. Accuracy-based.
4. **Señales secundarias:** `RECOGNITION_DPRIME`, `FALSE_ALARM_RATE`, `RESPONSE_CRITERION`.
5. **Contaminantes:** criterio de respuesta; verbalizacion; misma varianza que EX5 → **redundancia directa**. **redundantWith: EX5_GRID_ESPACIAL.**
6. **Ataques de gaming:** "siempre cambio"/"nunca cambio" (neutralizado por d'); verbalizacion del set.
7. **Gates obligatorios:** d' (no hit-rate); criterio extremo descartado; objetos de baja verbalizabilidad; min-n de probes change/no-change.
8. **Metrica/scoring:** `cowanK` con correccion log-linear; criterio. n_min: >=8 change + >=8 no-change. Invalidaciones: criterio extremo. Perfil: K. (Identico engine que EX5.)
9. **Banco:** mismo set visual que EX5. Anti-memorizacion via `excludeRecent`.
10. **Copy permitido:** "detecta si algo cambio", "memoria visoespacial", "progreso vs baseline".
11. **Copy prohibido:** identico a EX5 (orientacion/foto/far-transfer).
12. **Impl KMP:** NO crear un generador separado: añadir `recallMode = CHANGE_DETECTION` a `Ex5GridGenerator`. Retirar `EX6_DETECCION_CAMBIO` como `MemoryExerciseId` independiente o degradarlo a variante interna.
13. **Tests:** cubiertos por los de EX5 (modo change-detection); golden de d'/K compartido.
14. **Aceptacion (para no-RETIRE):** demostrar varianza unica vs EX5 (correlacion < 0.7 en datos sinteticos). Si no se puede → permanece como variante de EX5.
15. **Rediseño:** **MERGE en EX5** como segundo modo. Mantener dos entradas de catalogo solo si el producto necesita un drill corto de bajo practice-effect (change-detection) separado del de binding; aun asi comparten engine/banco.
16. **Impacto esperado:** bajo como ejercicio independiente (alto solapamiento); medio como modo de EX5.
17. **Prioridad: P2** (consolidar con EX5 antes de construir nada nuevo).
18. **Riesgo residual:** inflar el catalogo con varianza duplicada (viola "mejores señales, no mas ejercicios").

---

### EX4 · Pares Asociados con Interferencia A-B/A-C (rediseño de EX4; GENERADOR EXISTE)

1. **Veredicto: REVISE.** `Ex4PairsGenerator` wired y jugable, pero el nucleo premium (fase A-C de interferencia, demora 24h via `DeferredRecallScheduler`, separacion intrusion/conjunction) NO esta cableado. Practice-effect ALTO.
2. **Constructo:** memoria asociativa (binding relacional) y resistencia a interferencia proactiva/retroactiva (A-B/A-C).
3. **Señal primaria valida:** `ASSOCIATIVE_INDEX = correctTargets/pairCount` + `INTRUSION_RATE` (A-B sobre A-C). Accuracy-based → apto para perfil.
4. **Señales secundarias:** `CONJUNCTION_ERROR_RATE`, `INTERFERENCE_INDEX`, `DELAYED_RECALL_ACCURACY`, asimetria forward/backward.
5. **Contaminantes:** **memorizacion del banco fijo** (HIGH practice-effect → "progreso" = ítems aprendidos); typos en produccion libre; familiaridad sin binding. **Redundante con** EX8-Nombres-Rostros (mismo `ASSOCIATIVE_INDEX`).
6. **Ataques de gaming:** repetir el mismo banco hasta memorizar (neutralizado SOLO por `excludeRecentPairIds` estricto, que existe como infra); adivinar del banco (neutralizado por distractores que incluyen los targets B); saltar la demora (atrapado por anclaje a `EpochClock`).
7. **Gates obligatorios:** contenido nuevo por sesion (rotacion estricta); cue A-C identico al A-B (la interferencia es el punto); demora real anclada a reloj; min-n de pares; la **claim de mejora solo sobre pares UNTRAINED** (sonda no implementada).
8. **Metrica/scoring:** `associativeIndex`, `proactiveInterference = abIntrusions/(abIntrusions+correctC)`, `conjunctionRate`, `interferenceIndex` (<1 = interferencia). n_min: >=8 pares. Invalidaciones: accuracy≈chance. `null`/calibrating si pares<umbral o probe diferida no completada. Agregacion inter-sesion. Perfil: associativeIndex + intrusionRate.
9. **Banco:** `PairPool` autorado pequeño (~parte de las 38 lineas) → **insuficiente para HIGH practice-effect**. Necesita banco grande (>=300 pares), generacion o rotacion agresiva, holdout (`TransferProbePartition` reserve existe, no servido), matching tolerante (lemmatize) para typos.
10. **Copy permitido:** "aprende parejas y resiste la interferencia", "actualiza una asociacion (cambio el numero de alguien)", "progreso vs baseline".
11. **Copy prohibido:** "memoria fotografica", "nunca olvidas un nombre", far-transfer a vida real/estudios.
12. **Impl KMP:** reutiliza `PairedAssociatesTrial`/`CuedRecallProbe`/`RecallDirection` + `Ex4PairsGenerator`. FALTA: fase A-C (`overlap`), `DeferredRecall`/`DeferredRecallScheduler`/`LocalDeferredRecallRepository` (modelos existen en `domain/deferred/`, **no cableados al holder**). Persistir `MemorySessionSummary`.
13. **Tests:** unit (generador determinista, cues compartidos A-B/A-C); property (intrusion A-B vs conjunction mutuamente exclusivos); simulation (perfil "memoriza banco" → sin mejora UNTRAINED); adversarial (banco repetido); golden de associativeIndex; copy-scanner.
14. **Aceptacion:** (a) fase A-C cableada con overlap real; (b) rotacion estricta de contenido testeada; (c) demora 24h anclada a reloj y servida; (d) mejora afirmada solo sobre sonda UNTRAINED.
15. **Rediseño:** A-B/A-C como nucleo (no pares simples); banco grande + rotacion estricta + matching tolerante; demora diferida real; medir `interferenceIndex` como señal premium. Sin sonda UNTRAINED, capar el copy a "progreso en el ejercicio", nunca "tu memoria asociativa mejoro".
16. **Impacto esperado:** medio (asociativo es valioso pero el practice-effect alto limita la señal sin rotacion+UNTRAINED).
17. **Prioridad: P1** (jugable; cerrar A-C + demora + rotacion).
18. **Riesgo residual:** practice-effect ALTO domina si la rotacion/holdout fallan; abandono de la probe 24h reduce datos.

---

### EX8 · Nombres-Rostros (CATALOGO SIN GENERADOR)

1. **Veredicto: MERGE.** `EX8_NOMBRES_ROSTROS` (gobierna `ASSOCIATIVE_INDEX`) es un **caso particular de EX4** (asociativo cue↔target) con material caras-nombres. No aporta varianza nueva; es una variante de alto valor de EX4.
2. **Constructo:** memoria asociativa caras↔nombres (binding cara-nombre).
3. **Señal primaria valida:** `ASSOCIATIVE_INDEX` (correctos/pares). Accuracy-based.
4. **Señales secundarias:** `INTRUSION_RATE`, `CONJUNCTION_ERROR_RATE`, `DELAYED_RECALL_ACCURACY`.
5. **Contaminantes:** **sesgo cultural/etnico-lexico fuerte** (nombres y caras sesgan por cultura/raza del usuario); memorizacion del banco. **redundantWith: EX4_PARES_ASOCIADOS.**
6. **Ataques de gaming:** memorizar banco fijo de caras; verbalizar rasgo distintivo en vez de binding cara-nombre.
7. **Gates obligatorios:** banco grande y **balanceado por etnia/genero/edad** (control de sesgo cultural); rotacion estricta; demora real; min-n; matching tolerante de nombres.
8. **Metrica/scoring:** identico a EX4 (`associativeIndex`, intrusion, conjunction). n_min >=8 pares cara-nombre. Invalidaciones: sesgo de banco no balanceado. Perfil: associativeIndex.
9. **Banco:** requiere **assets de caras** (no existen) + nombres balanceados culturalmente → coste alto y riesgo de sesgo. Holdout/inter-rater n/a (scoring determinista), pero el **balance cultural ES la validacion critica** aqui.
10. **Copy permitido:** "asocia cada cara con su nombre", "progreso vs baseline".
11. **Copy prohibido:** "nunca olvidaras un nombre", "mejor en reuniones/networking" (far-transfer social).
12. **Impl KMP:** NO generador separado: añadir modo `material = FACE_NAME` a `Ex4PairsGenerator`. Requiere pipeline de assets de caras (licencias/diversidad) — fuera de alcance V1.
13. **Tests:** cubiertos por EX4 + test de **balance cultural del banco** (distribucion etnia/genero). copy-scanner.
14. **Aceptacion (para no-RETIRE):** banco de caras diverso y licenciado + control de sesgo cultural demostrado. Sin eso → permanece como variante teorica de EX4.
15. **Rediseño:** **MERGE en EX4** como variante de material de alto valor, aplazada hasta resolver assets+sesgo. No justifica entrada de catalogo independiente.
16. **Impacto esperado:** bajo (varianza duplicada de EX4; coste/riesgo de assets alto).
17. **Prioridad: P3** (aplazar; consolidar en EX4).
18. **Riesgo residual:** sesgo cultural del banco si no se balancea; coste de assets de caras con diversidad y licencias.

---

### EX3 · Episodica con Recall Diferido y Reconstruccion (rediseño de EX3; GENERADOR EXISTE)

1. **Veredicto: REVISE.** `Ex3ListGenerator` wired y jugable, pero la pieza que lo distingue de "memoriza una lista" (recall diferido +24h real + tarea distractora pre-inmediato + `PlateauDetector.FalseImprovement`) NO esta cableada. Practice-effect ALTO.
2. **Constructo:** memoria episodica de lista con codificacion elaborativa y consolidacion (curva de olvido).
3. **Señal primaria valida:** `DELAYED_RECALL_ACCURACY` (gobernante) — recall +24h sobre material nuevo. Accuracy-based → apto para perfil; pero **requiere la probe diferida**, hoy no servida.
4. **Señales secundarias:** `IMMEDIATE_RECALL_ACCURACY`, `ARC_CLUSTERING` (existe `ArcClustering.kt`), `CONSOLIDATION_INDEX`, `FORGETTING_RATE`, `CALIBRATION_INDEX` (JOL).
5. **Contaminantes:** **recencia** (sin distractor mide buffer, no LTM); memorizacion de listas (HIGH); intrusiones extralista (falsas memorias). **Redundante con** EX1 si se reporta solo el inmediato (ambos serian span verbal).
6. **Ataques de gaming:** subvocalizar la lista en el delay (neutralizado por tarea distractora — ausente); reusar listas memorizadas (neutralizado por rotacion); inventar respuestas (atrapado por intrusiones extralista).
7. **Gates obligatorios:** tarea distractora antes del recall inmediato (bloquea recencia); demora +24h anclada a `EpochClock`; contenido nuevo por sesion; `FalseImprovement` vigilando gap inmediato↔diferido; min-n de items.
8. **Metrica/scoring:** `immediateAcc`, `delayedAcc`, `forgettingRate`, `arcClustering` (ARC de Roenker), `consolidationIndex = delayed/immediate`, `intrusionRate`. n_min: lista >=8 + probe diferida completada. Invalidaciones: probe +24h no completada → `confident=false` para la claim de mejora. `null`/calibrating consistente. Agregacion: pre/post via `ReliableChangeIndex` (motor existe, **sin caller**).
9. **Banco:** `WordPool` Spanish autorado pequeño → insuficiente para HIGH practice-effect. Necesita banco grande categorizable + rotacion estricta + holdout. Matching tolerante (lemmatize) para produccion libre.
10. **Copy permitido:** "recuerda la lista, hoy y mañana", "consolidacion", "progreso vs baseline", "datos insuficientes hasta completar la prueba de mañana".
11. **Copy prohibido:** "mejora tu memoria", "previene el declive/olvido", "estudias mejor" (far-transfer a estudios), cualquier claim clinico.
12. **Impl KMP:** reutiliza `RecallListTrial` + `Ex3ListGenerator`. FALTA cablear: `DeferredRecall`/`DeferredRecallScheduler`/`LocalDeferredRecallRepository` (existen en `domain/deferred/`, sin caller), tarea distractora, `PlateauDetector.FalseImprovement` en el flujo. `ArcClustering` existe. Persistir `MemorySessionSummary` (immediate/delayed).
13. **Tests:** unit (generador determinista, listas categorizables); property (forgettingRate in [0,1]; consolidation = delayed/immediate); simulation (perfil "rehearsal" → recencia alta sin diferido); adversarial (reuso de listas); golden de ARC/consolidation; copy-scanner. (Existe `EvaluationComparator` test, pero el flujo diferido no.)
14. **Aceptacion:** (a) probe +24h cableada y anclada a reloj; (b) distractor pre-inmediato; (c) `FalseImprovement` vigilando el gap; (d) mejora solo sobre material UNTRAINED.
15. **Rediseño:** lista categorizable + prompt de estrategia (medido por ARC, no impuesto) + distractor obligatorio + diferido +24h real; reportar `DELAYED_RECALL_ACCURACY` como gobernante; gate de claim por `RciResult` (cuando se cablee). Sin diferido real → degradar a "drill de inmediato" con copy capado.
16. **Impacto esperado:** alto (episodica con consolidacion real es la señal mas honesta y diferenciada del modulo).
17. **Prioridad: P1** (jugable; el valor entero depende de cablear el diferido).
18. **Riesgo residual:** abandono de la probe +24h (re-engagement); practice-effect alto sin rotacion; produccion libre con typos.

---

### EX7 · Orden Serial Temporal (CATALOGO SIN GENERADOR)

1. **Veredicto: REVISE.** `EX7_SECUENCIAS` en catalogo (`governingMetric = SERIAL_ORDER_ACCURACY`) pero **sin generador**. Aporta una dimension cognitiva distinta (orden) SI se disocia explicitamente del item memory.
2. **Constructo:** memoria del ORDEN serial (posicion temporal), disociable del item memory.
3. **Señal primaria valida:** `SERIAL_ORDER_ACCURACY = positionsCorrect/s` (scoring por POSICION, no por "items presentes"). Accuracy-based → apto para perfil.
4. **Señales secundarias:** `IMMEDIATE_RECALL_ACCURACY` (item memory, para disociar), transposition-rate, gradiente posicional.
5. **Contaminantes:** **confusion con span de item** si no se disocia (seria otro span); chunking; rehearsal articulatorio. **Redundante con** EX1-Span si se puntua por items presentes en vez de posicion.
6. **Ataques de gaming:** "los toque todos" para inflar accuracy (neutralizado por scoring por posicion); chunking visible (neutralizado por `grouping` continuo + tap concurrente); respuesta azarosa (atrapada por gradiente posicional plano = baja calidad).
7. **Gates obligatorios:** scoring estricto por posicion; medir transposiciones + gradiente posicional (validar que la señal es de orden); supresion articulatoria opcional; variante de reconstruccion (items dados, ordenar) para aislar orden puro; min-n.
8. **Metrica/scoring:** `serialOrderAcc`, `itemAcc`, `transpositionRate`, `orderSpecificScore = serialOrderAcc − itemAcc·chanceOrderGivenItems`. n_min: >=6 trials. Invalidaciones: gradiente plano (azar). `null`/calibrating si trials<umbral. Perfil: serialOrderAcc disociado.
9. **Banco:** procedural (secuencias de iconos/posiciones/silabas). Anti-memorizacion via banco mezclado por trial. Holdout via partition.
10. **Copy permitido:** "reproduce el orden exacto", "memoria del orden", "progreso vs baseline".
11. **Copy prohibido:** "mejora tu planificacion", "sigues instrucciones mejor en el trabajo", far-transfer a pasos de la vida real.
12. **Impl KMP:** `SerialItem`/`SerialOrderTrialPlan` (con `expectedOrder` scoring-key), `Ex7SerialGenerator`. Engine: scoring por posicion + transposiciones. Persistir `MemorySessionSummary`.
13. **Tests:** unit (generador determinista, scoring por posicion); property (orderSpecificScore separa orden de item; gradiente posicional con forma U); simulation (perfil "toca todos" → itemAcc alto, orderAcc bajo); adversarial (respuesta azarosa → gradiente plano detectado); golden; copy-scanner.
14. **Aceptacion:** (a) scoring por posicion + transposiciones testeado; (b) variante de reconstruccion que aisla orden; (c) gradiente posicional como gate de calidad; (d) disociacion item/orden demostrada en datos sinteticos.
15. **Rediseño:** construir con scoring por posicion + reconstruccion de orden como modo de aislamiento + supresion articulatoria (tap concurrente) como `CONCURRENCY`. La disociacion item-vs-orden es lo que lo salva de ser "otro span".
16. **Impacto esperado:** medio (orden serial es una dimension genuina, complementaria al span).
17. **Prioridad: P2** (constructo distinto pero de menor prioridad que cerrar EX1/EX3/EX4/EX2/EX5).
18. **Riesgo residual:** si la disociacion falla, colapsa en redundancia con el span; practice-effect medio.

---

### EX_TRANSFER_PROBE · Sonda de Transferencia UNTRAINED (DISEÑO-ONLY, motores existen sin caller)

1. **Veredicto: BLOCK.** Es el **corazon de la honestidad del modulo** y, sin embargo, NO esta cableado: `ReliableChangeIndex`/`RealImprovementCriterion`/`RciResult` existen como motores puros **sin ningun caller**; `TransferProbePartition` reserva material pero "se consume solo por Phase-8" (no implementada). Toda claim de "mejora real" del diseño es hoy **no comprobable**. Bloqueante de modulo: sin esto, NINGUN ejercicio puede afirmar mejora de capacidad.
2. **Constructo:** capacidad latente REAL (no familiaridad con el drill entrenado) via material/paradigma UNTRAINED.
3. **Señal primaria valida:** `untrainedDelta` (post−pre sobre material reservado) + `ReliableChangeIndex` (|RCI|>1.96, reliable) sobre la metrica gobernante. Es la UNICA señal admisible para la claim de mejora.
4. **Señales secundarias:** `effectSize` (Cohen's d), `sustainedWeeks`, `CONSOLIDATION_INDEX` UNTRAINED, comparacion trained-delta vs untrained-delta (clasifica `ImprovementPattern`).
5. **Contaminantes:** **formas paralelas A/B/C no equiparadas** (mide ruido y declara "mejora fiable" basura); **fatiga** (probe con cansancio invalida); varianza pre anomala; n insuficiente. Sin redundancia (es transversal, no compite con drills).
6. **Ataques de gaming:** memorizar la propia probe (neutralizado por formas paralelas + material reservado); entrenar mientras se mide (neutralizado por cero feedback); inflar pre para fabricar delta.
7. **Gates obligatorios:** dificultad FIJA estandarizada; material SIEMPRE reservado (`TransferProbePartition`); **cero feedback** durante la probe; **NO administrar con fatiga HIGH** (gate explicito); RCI gated por reliability estimada con >=2–3 administraciones (`RciResult.NotEnoughData` por defecto); formas paralelas **equiparadas** (equating).
8. **Metrica/scoring:** `untrainedDelta`, `effectSize = delta/pooledSD`, `RCI = (post−pre)/(sqrt(2)·sdPre·sqrt(1−reliability))`, `pattern = classify(...)`. Claim de mejora SOLO si `isGenuine && RCI.reliable`. n_min: itemsPerConstruct 8–20 + >=2–3 administraciones. Invalidaciones: fatiga HIGH, formas no equiparadas, reliability no estimada → `NotEnoughData`. `null`/`NotEnoughData` por defecto (nunca bajo rendimiento). Es **el** mecanismo de actualizacion de la claim de perfil.
9. **Banco:** material reservado por `TransferProbePartition` (existe reserve determinista) + **formas paralelas A/B/C equiparadas** (NO existen; requieren equating estadistico — la pieza psicometrica mas dificil y ausente). Holdout = el propio diseño. Inter-rater n/a (scoring determinista). Anti-memorizacion = rotacion de formas.
10. **Copy permitido:** "Esto es una MEDICION, no entrenamiento. Sin pistas, sin feedback.", "necesitamos mas mediciones para confirmar tendencias", "tu capacidad funcional cambio de forma fiable" (SOLO con RCI reliable + isGenuine), "estimacion preliminar", "no actualizado por calidad de medicion".
11. **Copy prohibido:** afirmar mejora con UNA administracion; "tu memoria mejoro" sin RCI reliable; cualquier claim clinico/IQ/far-transfer a vida real.
12. **Impl KMP:** nuevo `MemoryExerciseId.EX_TRANSFER_PROBE` (NO en `implemented` como drill; orquestado por scheduler). `TransferProbePlan`/`TransferProbeSummary` (`@Serializable`, `schemaVersion`). Cablear `ReliableChangeIndex` + `RealImprovementCriterion` + `PlateauDetector` (dispara probe ante `FalseImprovement`). Gate de fatiga via `MemoryFatigue`. Persistir solo `TransferProbeSummary`.
13. **Tests:** unit (RCI/effectSize ya testeables — verificar cobertura); **golden-scoring** del RCI (locks numericos); property (NotEnoughData hasta reliability; sin claim con n<umbral); simulation (perfil "familiaridad" → trained-delta alto, untrained-delta ~0 → PRACTICE_EFFECT); adversarial (formas no equiparadas → no declarar reliable); gate de fatiga (probe no se lanza con HIGH); copy-scanner (prohibir claim con una administracion).
14. **Aceptacion:** (a) formas paralelas equiparadas (equating) ANTES de comparar; (b) gate de fatiga explicito; (c) RCI gated por reliability >=2–3 administraciones; (d) claim solo con `isGenuine && reliable`; (e) cero feedback; (f) motores efectivamente cableados a un scheduler.
15. **Rediseño:** construir el scheduler de probe (cada N sesiones / ante FalseImprovement), near-transfer probe por defecto (mismo paradigma, material reservado), far-transfer como variante; equating de formas A/B/C como prerequisito psicometrico; consolidation-probe +24h. Es **P0 de modulo**: sin esta pieza, todos los demas ejercicios estan capados a "progreso en el drill", nunca "mejora de capacidad".
16. **Impacto esperado:** alto (es el unico determinante de si el modulo puede afirmar algo honesto sobre capacidad).
17. **Prioridad: P0** (bloquea la claim de todo el modulo).
18. **Riesgo residual:** formas no equiparadas → RCI basura; percepcion de "examen" → abandono (mitigar con framing y brevedad); coste de re-engagement de la componente +24h; equating es estadisticamente exigente.

---

### Conclusion del comite (modulo Memoria)

- **Realidad vs diseño:** de 8 ejercicios diseñados, **3 generadores reales** (EX1/EX3/EX4) y ninguno con sus gates premium cableados (no hay anticipation-floor, no hay diferido real servido, no hay d'/Cowan-K, no hay sonda UNTRAINED). N-Back y la Sonda son **diseño-only**. EX5/EX6/EX7/EX8/EX2 son **catalogo sin generador**.
- **Bloqueante de modulo (P0):** la Sonda UNTRAINED + cableado de `ReliableChangeIndex`/`RealImprovementCriterion`. Sin ella NINGUN ejercicio puede afirmar mejora de capacidad; el copy queda capado a "progreso en el ejercicio".
- **Timebase:** `RETRIEVAL_SPEED` se computa sobre `EpochClock` (wall-clock, NO autoritativo) → **prohibido actualizar perfil con RT**; gate `device_unreliable`/UNTRUSTED_TIMEBASE. Las señales de perfil deben ser accuracy-based (span/K/associative/serial-order/delayed-recall), que SI son validas.
- **min-n:** `MIN_N=4` es demasiado bajo para metricas psicometricas; subir por celda (>=8 tipico) y exigir n_efectivo por bloque.
- **0-sentinela:** `MemoryMetricValue.unavailable()` guarda `value=0.0`; el accessor respeta `confident`, pero auditar que ningun consumidor lea `value` crudo (riesgo de tratar invalido como 0/bajo-rendimiento).
- **Redundancias:** EX6→MERGE en EX5; EX8→MERGE en EX4. EX1/EX2/N-Back comparten varianza WM-ejecutiva (diferenciar o capar a 2).
- **Banco:** todos los bancos son pequeños, autorados, sin validacion (cero IRT/inter-rater/exposure/holdout activo); practice-effect ALTO en EX3/EX4 exige rotacion estricta + holdout servido (hoy reservado pero nunca usado).
- **Copy-scanner:** Memoria NO tiene gate de terminologia (solo arena/emotions lo tienen). Añadir `memoryTerminologyScanner` contra léxico de IQ/inteligencia/clinico/far-transfer (vida real/estudios/multitarea), enganchado a `check`.
- **Anti-gaming:** practicamente inexistente en Memoria (sin anticipation-floor, sin criterio-extremo, sin gradiente posicional). Construir property/simulation/adversarial por ejercicio antes de aprobar.