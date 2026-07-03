# Memoria

> Catálogo de diseño de 8 ejercicios de calidad suprema para el módulo **MindBoost · Memoria** (KMP).
> Anclado al código real: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/**`.
> Motores reusados por nombre: `ReliableChangeIndex` (Jacobson-Truax, `CRITICAL_Z = 1.96`),
> `RealImprovementCriterion` (delta en probe UNTRAINED), `TransferProtocol`/`TransferProtocols`,
> `PlateauDetector` + `InterventionPlanner`, `DefaultMemoryAdaptiveEngine` (vector de 8 ejes
> `MemoryDifficultyVector`, mueve UN eje por decisión), `MemoryMetricsEngine`/`MemoryMetricKey`,
> `MemorySessionSummary` (solo resúmenes), `SecureKeyValueStore` + `@Serializable` + `schemaVersion`,
> `EpochClock`, `RecentItemRegistry`/`TransferProbePartition`, wiring hub→`MemoryGraph.generators`
> (registry id→`MemoryTrialGenerator`)→`MemorySessionStateHolder`→`MemorySessionScreen`.
> Fronteras: producto NO clínico, NO diagnóstico, NO IQ garantizado. Lenguaje: "perfil cognitivo
> funcional", "práctica", "baseline personal", "dificultad adaptativa".

Los 3 ejercicios actuales jugables (EX1 Span Complejo, EX3 Lista con Estrategia, EX4 Pares Asociados)
permanecen; estos 8 son rediseños premium / nuevos que cubren memoria de trabajo verbal y visoespacial,
complex span con procesamiento concurrente, n-back adaptativo, binding asociativo, episódica con recall
diferido, orden serial, running memory (actualización) y una sonda de transferencia UNTRAINED. Ninguno
es una variación del mismo loop.

---

## Span Adaptativo Dual (rediseño premium de EX1)

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`MemorySubcapacity.WORKING_MEMORY`). Secundarios: M5_RESISTANCE
(`INTERFERENCE_RESISTANCE`), M6 (`METAMEMORY_CALIBRATION` vía overlay EX16).

### Objetivo cognitivo
Capacidad de mantenimiento activo de memoranda verbales bajo carga de una tarea de procesamiento
concurrente (paradigma complex-span clásico: operation span / reading span), forzando el componente
ejecutivo de la WM más allá del simple buffer fonológico.

### Hipótesis de transferencia  (qué SÍ podría transferir y qué NO se debe afirmar)
SÍ plausible: near-transfer a otras tareas de complex-span y a `WORKING_MEMORY_SPAN` medido en una
sonda UNTRAINED. POSIBLE far-transfer débil a comprensión lectora bajo carga (literatura mixta). NO se
afirma: aumento de IQ, mejora de "concentración general", ni beneficio clínico. La única prueba de
mejora real es el delta en la tarea UNTRAINED vía `RealImprovementCriterion.isGenuine`.

### Loop principal  (1 estímulo, 2 decisión, 3 feedback, 4 adaptación, 5 siguiente trial)
1. **Estímulo:** se presenta `Memorandum` (palabra) → `ProcessingTask` (verificación aritmética
   "a + b = shown", correcta ~50%) → siguiente memorandum, interleaved (`ComplexSpanTrial.steps`).
2. **Decisión:** el usuario juzga cada `ProcessingTask` (tap VERDADERO/FALSO con deadline) y, al final,
   recupera los memoranda en orden (`RecallType.SERIAL_FORWARD`/`SERIAL_BACKWARD`).
3. **Feedback:** inmediato por processing (✓/✗ con RT); diferido por recall (posiciones correctas,
   intrusiones, omisiones) al cerrar el trial.
4. **Adaptación:** `DefaultMemoryAdaptiveEngine.decide` mueve UN eje (gobernante `LOAD`).
5. **Siguiente trial:** nuevo plan con `excludeRecentWordIds` del `RecentItemRegistry`.

### Reglas
- Cada `ProcessingTask` debe responderse dentro de su deadline o cuenta como fallo de procesamiento
  (controla que el usuario no ignore la tarea concurrente para proteger los memoranda).
- El recall solo puntúa si la precisión de procesamiento ≥ 85% en el trial (umbral de validez): por
  debajo, el trial se marca de baja calidad y NO alimenta el staircase de span.
- Span gobernado por accuracy serial, no por RT.

### Inputs  (tap/swipe/drag/teclado/selección múltiple/voz opcional/texto libre)
Tap binario (VERDADERO/FALSO) en processing; recall por selección múltiple ordenada (tap secuencial
sobre un banco mezclado) o teclado opcional. Sin texto libre en V1 (scoring determinista).

### Variables de dificultad  (entre 5 y 10, con rangos concretos)
- `spanLength` (eje `LOAD`): **2–8** memoranda (`DifficultyDecode.decodeCount(load,2,8)`).
- `processingPerItem` (eje `CONCURRENCY`): **1–2** tareas por memorandum.
- `sameCategory` (eje `SIMILARITY`): false→true (≥0.5) ⇒ misma `WordCategory`, más confusable.
- `backwardRecall` (eje `RETRIEVAL`): false→true (≥0.5) ⇒ recall inverso.
- `processingDeadlineMs`: **3500→1800 ms** (presión de la tarea concurrente).
- `studyMsPerItem`: **1200→700 ms**.
- `interItemMaskMs` (eje `INTERFERENCE`): **0→400 ms** de máscara entre ítems.

### Algoritmo adaptativo  (accuracy, RT, errores, racha, fatiga, plateau, baseline personal)
Por bloque (no por trial), `MemoryPerformance.of(recentBlock,...)` → zona → `DefaultMemoryAdaptiveEngine`:
StepUp/StepDown sobre `LOAD` con histéresis (`cooldownBlocks`), pasos asimétricos (`stepBig` baja sin
histéresis como safety). `dimensionToRaise` desvía a `INTERFERENCE` si `INTRUSION_RATE` alto. Baseline
personal = `MemoryBaselineProtocol`. `PlateauDetector` + `InterventionPlanner` rotan estímulo/estrategia.

### Métricas primarias
`WORKING_MEMORY_SPAN`, `DUAL_TASK_COST`, `INTRUSION_RATE`, `RETRIEVAL_SPEED` (todas vía `MemoryMetricKey`).

### Métricas secundarias
`SERIAL_ORDER_ACCURACY`, `OMISSION_RATE`, consistencia (CV de span entre bloques), `LEARNING_CURVE_SLOPE`,
`CALIBRATION_INDEX` (overlay EX16).

### Señales de fatiga o mala calidad
RT de processing creciente con accuracy estable (drift de fatiga), span efectivo cayendo >1 punto vs
baseline de la sesión, processing accuracy <85% (trial inválido), racha de omisiones ≥3.

### Scoring sugerido  (fórmula o pseudofórmula con variables nombradas)
```
spanScore = sum_over_trials( serialPositionsCorrect / spanLength ) ponderado por validez
IES_proc  = meanRT_proc / (1 - errRate_proc)        // inverse efficiency del procesamiento
dualCost  = span_single - span_dual                  // DUAL_TASK_COST
intrusionRate = intrusions / (intrusions + correct)
qualityFlag = processingAccuracy >= 0.85
```
`confident=false` si `n` de trials válidos < umbral (`MemoryMetricValue.unavailable`).

### Antitrampa / calidad de datos
Processing con deadline impide ignorar la tarea concurrente. Trials con processing accuracy <85% no
cuentan para span. RT por debajo de `ANTICIPATION_FLOOR` (~150 ms) marca respuesta anticipada. Orden de
banco de recall aleatorizado para impedir patrón espacial.

### Variantes  (≥3 concretas)
1. **Operation span:** processing = verificación aritmética (default).
2. **Reading span:** processing = juicio semántico "¿frase plausible?" (requiere banco de frases V2).
3. **Symmetry span (visoespacial):** memoranda = posiciones en grid; processing = juicio de simetría
   (puente a Grid Espacial; comparte `COWAN_K`).

### UX  (pantalla, interacción, feedback, microanimaciones funcionales, duración ideal)
Pantalla limpia centrada; memorandum en grande, processing como tarjeta con dos botones. Microanimación
funcional: pulso del deadline (anillo que se vacía), no decorativo. Recall = banco inferior + zona de
orden. Duración ideal por trial 12–25 s; bloque de 6–8 trials; sesión 6–9 min.

### Implementación KMP  (modelo de datos, eventos, estado mínimo, motor a conectar)
```kotlin
// Reusa ComplexSpanTrial / SpanStep / Memorandum / ProcessingTask (MemoryTrialModels.kt).
// Generador ya existe: Ex1SpanGenerator : MemoryTrialGenerator (registrar en MemoryGraph.generators).
@Serializable
data class SpanTrialResult(           // efímero en sesión, NO persistido crudo
    val spanLength: Int, val serialCorrect: Int,
    val processingAccuracy: Double, val processingRtMs: List<Long>,
    val intrusions: Int, val omissions: Int, val valid: Boolean,
)
```
Eventos a registrar: `memory_span_trial_completed` (span, dualCost, valid), `memory_processing_lapse`.
Estado mínimo: `MemoryUiState` (fase Study/Process/Recall), índice de step, deadline activo. Persistir
solo `MemorySessionSummary` vía `LocalMemorySessionHistoryRepository` (`SecureKeyValueStore`,
`MindBoostStorageNamespaces.Memory`). Motores: `DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`,
`PlateauDetector`; tiempo vía `EpochClock`.

### Riesgos
Practice effect MEDIO (`PracticeEffectRisk.MEDIUM`): el processing aritmético se aprende; rotar a
reading/symmetry span (`STIMULUS_ROTATION`). Riesgo de que el usuario sacrifique processing por span
(mitigado por umbral de validez).

### Revisión crítica  (el Revisor Crítico intenta destruirlo)
"Es EX1 con maquillaje: el span verbal es el ejercicio más sobreusado y con mayor practice effect del
sector; un usuario memoriza la estructura del processing y el span sube sin WM real. Además 'aritmética'
es dependiente de habilidad numérica → confound cultural/educativo." **Correcciones exigidas:** (1) el
staircase de span DEBE alimentarse solo de trials con processing ≥85% (ya añadido); (2) reportar span
SOLO contra la sonda UNTRAINED para la claim de mejora; (3) ofrecer reading-span como default rotatorio
para neutralizar el confound numérico; (4) calibrar `processingDeadlineMs` al RT base del usuario, no fijo.

### Veredicto
**Aprobado con cambios** — premium solo si el span se valida contra processing y la mejora se afirma
únicamente sobre la sonda UNTRAINED.

---

## N-Back Adaptativo de Doble Modalidad

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`WORKING_MEMORY`). Secundarios: M4_SPATIAL_VISUAL (`SPATIAL`/`VISUAL`),
M5_RESISTANCE (`INTERFERENCE_RESISTANCE`, vía lures).

### Objetivo cognitivo
Actualización y matching continuo en WM (paradigma n-back): mantener una ventana deslizante de n ítems
y decidir si el actual coincide con el de hace n pasos. Entrena el componente de *updating* del modelo
de Miyake, distinto del mantenimiento puro del span.

### Hipótesis de transferencia
SÍ plausible: near-transfer a tareas de updating y a `WORKING_MEMORY_SPAN`. La literatura de far-transfer
del n-back a Gf es **controvertida y débil**; NO se afirma mejora de inteligencia ni de "foco". Solo se
reporta progreso contra baseline personal y delta UNTRAINED. NO afirmar beneficio académico directo.

### Loop principal
1. **Estímulo:** stream de ítems (posición en grid 3×3 + sílaba auditiva/visual), uno cada ISI.
2. **Decisión:** por ítem, "¿coincide con hace n?" en modalidad posición y/o sonido (single o dual n-back).
3. **Feedback:** inmediato por ítem (✓/✗ sutil) o diferido por bloque (configurable: el diferido reduce
   dependencia del feedback y mide aprendizaje real).
4. **Adaptación:** n sube/baja según accuracy del bloque (staircase sobre eje `LOAD` reinterpretado como n).
5. **Siguiente:** nuevo stream con tasa de targets controlada y lures n±1.

### Reglas
- Tasa de targets fija ~25–30% por modalidad (evita que "siempre no" sea ganador).
- Lures n−1/n+1 (`INTERFERENCE`) presentes para impedir matching por familiaridad vaga.
- Respuesta solo en ventana; sin respuesta = omisión (no "no-match" gratis).

### Inputs
Dos taps dedicados (botón Posición / botón Sonido) o swipe lateral para dual. Sin teclado. Voz no aplica.

### Variables de dificultad
- `nLevel` (eje `LOAD`): **1–5**.
- `dualModality` (eje `CONCURRENCY`): single→dual (≥0.5).
- `isiMs`: **3000→1500 ms** (presión temporal).
- `lureRate` (eje `INTERFERENCE`): **0.0→0.35** de ítems lure n±1.
- `targetRate`: **0.20→0.30** (mantener detectabilidad; no es palanca de "más=difícil").
- `stimulusSetSize` (eje `SIMILARITY`): **6→9** sonidos/posiciones (más confusables).
- `stimulusOnMs`: **800→400 ms**.

### Algoritmo adaptativo
Staircase 1-up/1-down asimétrico sobre `nLevel`: subir n con accuracy de bloque ≥80% en AMBAS
modalidades; bajar con <55%. Histéresis vía `cooldownBlocks`. `DefaultMemoryAdaptiveEngine` desvía a
`INTERFERENCE` (subir `lureRate`) cuando `FALSE_ALARM_RATE` alto en lugar de subir n (evita inflar carga
sin discriminación). Baseline = n máximo sostenido en `MemoryBaselineProtocol`.

### Métricas primarias
`WORKING_MEMORY_SPAN` (n efectivo), `RECOGNITION_DPRIME` (hits vs false alarms — honesto, no hit rate
crudo), `FALSE_ALARM_RATE`, `RETRIEVAL_SPEED`.

### Métricas secundarias
`RESPONSE_CRITERION` (sesgo), `DUAL_TASK_COST` (single vs dual), `BLOCK_STABILITY`, `LEARNING_CURVE_SLOPE`,
consistencia (CV de d').

### Señales de fatiga o mala calidad
d' descendiendo con criterio derivando a "todo no-match" (desistimiento), false alarms en lures subiendo
con RT alto (sobrecarga), accuracy <55% dos bloques seguidos (n demasiado alto), omisiones en racha.

### Scoring sugerido
```
dPrime = z(hitRate) - z(falseAlarmRate)              // RECOGNITION_DPRIME, sensibilidad real
criterion = -0.5 * (z(hitRate) + z(falseAlarmRate))  // RESPONSE_CRITERION, sesgo
nEffective = max nLevel con dPrime >= 1.0 sostenido
dualCost = dPrime_single - dPrime_dual
```
`confident=false` si targets/lures por bloque insuficientes para z estable.

### Antitrampa / calidad de datos
Reportar d' (no accuracy bruto) neutraliza la estrategia "responder siempre no". Lures impiden matching
laxo. Criterio extremo (siempre sí / siempre no) ⇒ bloque marcado de baja calidad. ISI con jitter para
impedir respuesta rítmica.

### Variantes  (≥3 concretas)
1. **Single n-back posición** (espacial puro).
2. **Single n-back auditivo** (verbal/fonológico puro).
3. **Dual n-back** (posición+sonido simultáneos, el clásico).
4. (V2) **n-back adaptativo continuo** sin bloques fijos.

### UX
Grid 3×3 central, dos botones grandes inferiores (Posición/Sonido). Microanimación funcional: el ítem
"hace n" se insinúa con un trail de memoria muy sutil solo en onboarding (luego off, para no andamiar).
ISI con barra de ritmo. Duración: bloque 60–90 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable enum class NBackModality { POSITION, SOUND }
data class NBackTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val stream: List<NBackItem>, val nLevel: Int,
    val modalities: Set<NBackModality>, val targetMask: List<Boolean>,
) : MemoryTrialPlan
data class NBackItem(val position: Int, val soundId: Int, val isLure: Boolean)
class NBackGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
Nuevo `MemoryExerciseId.EX_NBACK` + entrada en `MemoryExerciseCatalog` (governingMetric =
`WORKING_MEMORY_SPAN`). Eventos: `nback_item_response` (modality, hit/fa/miss, rt), `nback_block_dprime`.
Estado mínimo: ventana circular de n + índice. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine` (d'), `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO-ALTO en stream fijo; mitigar con `stimulusSetSize` rotatorio y jitter. Riesgo de
sobre-claim de far-transfer: prohibido en copy. Fatiga alta en dual n-back largo → cap de duración.

### Revisión crítica
"El n-back es el ejercicio con peor reputación científica para far-transfer; venderlo como 'entrena tu
inteligencia' sería pseudociencia. Y un d' alto puede venir de criterio, no de sensibilidad." **Correcciones
exigidas:** (1) copy DEBE limitarse a 'updating de memoria de trabajo' y progreso vs baseline, jamás Gf/IQ;
(2) reportar d' Y criterio, descartando bloques con criterio extremo; (3) la claim de mejora pasa por
`RealImprovementCriterion` sobre sonda UNTRAINED, no por n alcanzado.

### Veredicto
**Aprobado con cambios** — científicamente defendible solo con d' honesto y copy sin claims de inteligencia.

---

## Updating de Ranura (Running Memory / EX2 jugable)

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`WORKING_MEMORY`, paradigma `RUNNING_MEMORY`). Secundarios: M5_RESISTANCE
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria de actualización (*memory updating* puro): mantener el valor MÁS RECIENTE de k ranuras mientras
llega un flujo de actualizaciones, descartando activamente el valor antiguo (inhibición de lo obsoleto).
Distinto del n-back: aquí hay binding ranura↔valor explícito y la respuesta es el estado final.

### Hipótesis de transferencia
SÍ plausible: a `pending_tasks`/`real_instructions` (`TransferProtocols.PENDING_TASKS`, `INSTRUCTIONS`)
y a recordar "el último valor" en contexto real (precio actual, hueco de agenda). NO afirmar mejora
general de memoria ni clínica.

### Loop principal
1. **Estímulo:** se muestran k ranuras etiquetadas (A,B,C…) con un valor inicial; luego un stream de
   updates "ranura X ← nuevo valor", a veces "ranura X ← +/− operación" (running computation).
2. **Decisión:** al final, reportar el valor vigente de cada ranura (o de una ranura sondeada).
3. **Feedback:** diferido por ranura (correcto / valor antiguo intruido / valor de otra ranura).
4. **Adaptación:** sube k o longitud del stream (eje `LOAD`), o similitud de valores (`SIMILARITY`).
5. **Siguiente:** nuevo set con `excludeRecent` para evitar repetición.

### Reglas
- El número de updates por ranura varía (evita estrategia de "contar updates").
- Distingue dos errores: **intrusión de valor antiguo** (fallo de updating) vs **binding error**
  (valor correcto, ranura equivocada) — métricas separadas.
- Sonda puede ser de UNA ranura aleatoria (reduce carga de output, aísla la señal de updating).

### Inputs
Selección múltiple por ranura (banco de valores) o teclado numérico en variante de cómputo. Drag opcional
para asignar valor↔ranura.

### Variables de dificultad
- `slotCount` k (eje `LOAD`): **2–6** ranuras.
- `updateStreamLen` (eje `LOAD` secundario): **k → 4k** updates.
- `valueSimilarity` (eje `SIMILARITY`): valores de un set pequeño/parecido (0.0→1.0).
- `runningComputation` (eje `DEPTH`): reemplazo simple → operación (+/−) sobre el valor previo (≥0.5).
- `updateRateMs`: **2500→1200 ms** por update.
- `interferenceFillers` (eje `INTERFERENCE`): **0→3** updates a ranuras inexistentes (lures a ignorar).
- `probeAll` vs `probeOne` (eje `RETRIEVAL`): sondear todas (≥0.5) o una.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (k). `specificWeakAxis`: si `INTRUSION_RATE` (valor antiguo)
alto → subir `INTERFERENCE` (más fillers); si binding errors altos → subir `SIMILARITY`. Histéresis y
pasos asimétricos estándar. Baseline = k sostenido con accuracy ≥75%.

### Métricas primarias
`WORKING_MEMORY_SPAN` (k efectivo), `INTRUSION_RATE` (valor obsoleto), `BINDING_ERROR_RATE` (ranura
equivocada).

### Métricas secundarias
`RETRIEVAL_SPEED`, `OMISSION_RATE`, `RESPONSE_CRITERION` (en variante recognition), consistencia (CV de k),
`LEARNING_CURVE_SLOPE`.

### Señales de fatiga o mala calidad
Intrusiones de valor antiguo creciendo (updating fallando), binding errors creciendo con k estable,
respuestas a fillers (no ignora lures), RT de output muy alto.

### Scoring sugerido
```
kEffective = max k con (correctSlots/k) >= 0.75
intrusionRate = oldValueIntrusions / probes      // fallo de updating
bindingErrorRate = wrongSlotCorrectValue / probes
updatingScore = 1 - (intrusionRate + 0.5*bindingErrorRate)
```
`confident=false` si probes < umbral.

### Antitrampa / calidad de datos
Fillers a ranuras inexistentes detectan respuesta automática. Updates por ranura variables impiden conteo.
Sonda de una ranura aleatoria impide preparar solo algunas. Banco de respuesta mezclado.

### Variantes  (≥3 concretas)
1. **Keep-track (categorías):** ranuras = categorías, recordar el último ejemplar de cada una.
2. **Running computation:** ranura mantiene un número y los updates son operaciones (+/−).
3. **Spatial updating:** ranuras = posiciones, valor = objeto que se mueve (puente visoespacial).

### UX
Fila de "ranuras" como tarjetas etiquetadas; el update destaca brevemente la ranura afectada
(microanimación funcional: flash de reemplazo que comunica "lo viejo se va"). Output = tocar cada ranura
y elegir valor. Duración: trial 15–30 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class SlotUpdate(val slot: Int, val value: String, val isOp: Boolean)
data class UpdatingTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val slotCount: Int, val initial: List<String>,
    val updates: List<SlotUpdate>, val probedSlots: List<Int>,
    val expectedFinal: Map<Int, String>,   // scoring key embebida (patrón MemoryTrialPlan)
) : MemoryTrialPlan
class Ex2UpdatingGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX2_ACTUALIZACION_SET` ya existe en el catálogo (paradigma `RUNNING_MEMORY`); falta el
generador → añadir a `implementedGenerators()`. Eventos: `updating_probe` (intrusion/binding/correct),
`updating_filler_response`. Motores: `DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`,
`PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO. Riesgo de confusión con n-back en el usuario (UX debe dejar claro: "estado final",
no "matching"). Carga de output alta con probeAll → preferir probeOne para señal limpia.

### Revisión crítica
"¿Es esto distinto del n-back o es n-back con etiquetas? Si no separas intrusión-de-valor de
binding-error, mides una sopa." **Correcciones exigidas:** (1) separar formalmente `INTRUSION_RATE` y
`BINDING_ERROR_RATE` (hecho); (2) la sonda por defecto es de UNA ranura para aislar updating del coste de
output; (3) los fillers son obligatorios para impedir respuesta automática; (4) documentar la diferencia
paradigmática con n-back en la ficha del catálogo.

### Veredicto
**Aprobado** — paradigma distinto y bien aislado si se mantienen las dos métricas de error separadas.

---

## Grid Espacial con Binding (memoria de trabajo visoespacial)

### Módulo  (principal + secundarios)
Principal: M4_SPATIAL_VISUAL (`SPATIAL`). Secundarios: M1 (`WORKING_MEMORY`), M5
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria de trabajo visoespacial con binding objeto↔posición: recordar QUÉ objeto estaba en CADA celda
(no solo cuántos/dónde). Captura el binding feature-location, complementario al canal verbal del span.

### Hipótesis de transferencia
SÍ plausible: near-transfer a tareas espaciales (Corsi, change detection) y a `COWAN_K`. POSIBLE puente a
método de loci (`TransferProtocols.PRESENTATIONS`). NO afirmar mejora de "orientación" o navegación real.

### Loop principal
1. **Estímulo:** grid N×N; se iluminan c celdas, cada una con un objeto (color/forma/icono), durante un
   tiempo de estudio; luego se ocultan.
2. **Decisión:** reconstruir colocando cada objeto en su celda (recall) o juzgar si una sonda es correcta
   (recognition / change detection).
3. **Feedback:** diferido: celdas correctas, swaps (binding errors), posiciones acertadas con objeto malo.
4. **Adaptación:** sube c (eje `LOAD`), similitud de objetos (`SIMILARITY`) o demora (`DELAY`).
5. **Siguiente:** nuevo layout con `excludeRecent`.

### Reglas
- Distinguir **error de localización** (posición mal) de **error de binding** (posición bien, objeto mal)
  de **swap** (dos objetos intercambiados) — tres señales separadas.
- Recall por reconstrucción (no solo "¿cambió algo?") para forzar binding completo.
- En variante change-detection, usar d' (no hit rate).

### Inputs
Drag-and-drop de objetos a celdas (recall); tap en celda + selección de objeto; tap binario en
change-detection. Sin teclado.

### Variables de dificultad
- `itemCount` c (eje `LOAD`): **2–7** objetos-posición.
- `gridSize` N (acompaña a `LOAD`): **3×3 → 5×5**.
- `objectSimilarity` (eje `SIMILARITY`): colores muy distintos → tonos vecinos / formas parecidas (0→1).
- `delayMs` (eje `DELAY`): **0 → 8000 ms** con máscara.
- `studyMsPerItem`: **900→450 ms**.
- `interferenceGrid` (eje `INTERFERENCE`): **0→1** grid distractor intermedio.
- `recallMode` (eje `RETRIEVAL`): recognition (≥0 baja) → reconstrucción libre (alta).

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (c). `specificWeakAxis`: binding errors altos → subir
`SIMILARITY`; olvido tras demora → subir `DELAY`. d' guía la zona en change-detection. Baseline = `COWAN_K`
sostenido. `PlateauDetector` rota a `MODALITY_CHANGE` si mesetiza.

### Métricas primarias
`COWAN_K` (capacidad visoespacial), `BINDING_ERROR_RATE`, `IMMEDIATE_RECALL_ACCURACY`,
`DELAYED_RECALL_ACCURACY`.

### Métricas secundarias
`RECOGNITION_DPRIME` y `FALSE_ALARM_RATE` (change-detection), `RETRIEVAL_SPEED`, swap rate, consistencia
(CV de K).

### Señales de fatiga o mala calidad
K efectivo cayendo, binding errors subiendo con localización estable (binding fallando selectivamente),
random placement (accuracy ~ azar = desistimiento), RT de drag muy alto.

### Scoring sugerido
```
cowanK = c * (hitRate - falseAlarmRate)              // change-detection
locationAcc = correctCells / c
bindingAcc  = correctObjectGivenCorrectCell / correctCells
swapRate    = swappedPairs / c
visuospatialScore = locationAcc * bindingAcc
```
`confident=false` si trials < umbral o c demasiado bajo para K estable.

### Antitrampa / calidad de datos
Reconstrucción completa impide adivinar "algo cambió". Objetos similares impiden codificación verbal
trivial ("el rojo arriba"). Grid distractor controla rehearsal. Random-placement detectado por accuracy ≈
chance. Posiciones balanceadas para evitar sesgo de centro.

### Variantes  (≥3 concretas)
1. **Corsi-like (orden):** secuencia de celdas a reproducir en orden (puente a orden serial espacial).
2. **Change detection:** una celda cambia de objeto tras la demora → tap si detectas el cambio (d').
3. **What-where binding:** reconstrucción completa objeto↔celda (default, máximo binding).

### UX
Grid táctil grande; objetos en bandeja inferior; drag con snap a celda. Microanimación funcional: las
celdas con objeto pulsan una vez al estudiar (codificación), la máscara cubre con grano neutro. Duración:
trial 12–25 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class GridCell(val row: Int, val col: Int, val objectId: Int)
data class GridBindingTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val gridSize: Int, val items: List<GridCell>,
    val delayMs: Long, val recallMode: RecallType,
    val expected: List<GridCell>,                 // scoring key embebida
) : MemoryTrialPlan
class Ex5GridGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX5_GRID_ESPACIAL` ya en catálogo (governingMetric `COWAN_K`); falta generador. Eventos:
`grid_placement` (location/binding/swap), `grid_change_detection`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine` (Cowan's K, d'), `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO/BAJO (`PracticeEffectRisk.LOW` para change-detection). Riesgo de verbalización
(mitigado por similitud de objetos). Drag en pantallas pequeñas → asegurar hit-area amplia.

### Revisión crítica
"Un grid bonito que en realidad se resuelve verbalizando ('círculo rojo arriba-izquierda') no mide
memoria visoespacial, mide memoria verbal disfrazada." **Correcciones exigidas:** (1) objetos sin nombre
verbal fácil y alta similitud cromática/forma para bloquear recoding verbal; (2) separar location vs
binding vs swap (hecho); (3) en change-detection reportar `COWAN_K` con false alarms, no hit rate; (4)
grid distractor para impedir rehearsal sostenido.

### Veredicto
**Aprobado con cambios** — visoespacial genuino solo si el recoding verbal está activamente bloqueado.

---

## Pares Asociados con Interferencia A-B/A-C (rediseño premium de EX4)

### Módulo  (principal + secundarios)
Principal: M3_ASSOCIATIVE (`ASSOCIATIVE`). Secundarios: M5_RESISTANCE (`INTERFERENCE_RESISTANCE`), M2
(`DELAYED_RETRIEVAL`).

### Objetivo cognitivo
Memoria asociativa (binding relacional entre elementos no relacionados) y su resistencia a interferencia
proactiva/retroactiva mediante el paradigma A-B / A-C: aprender pares, luego re-emparejar el mismo cue
con un target nuevo, midiendo intrusiones del aprendizaje previo.

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.VOCABULARY` (pares L1-L2, `v1Available=true`) y `names_faces_context`.
La interferencia A-B/A-C es la más cercana a la realidad de "actualizar" una asociación (cambió el número
de alguien). NO afirmar mejora de "memoria fotográfica".

### Loop principal
1. **Estímulo:** estudiar lista A-B (cue→target). Tras demora, estudiar A-C (mismos cues, nuevos targets).
2. **Decisión:** recall cued: dado A, producir C (o B según la lista pedida), forward/backward.
3. **Feedback:** diferido: correcto, intrusión (dio B cuando pedían C), error de conjunción.
4. **Adaptación:** sube nº de pares (`LOAD`), similitud (`SIMILARITY`), demora (`DELAY`) o grado de
   solapamiento A-B/A-C (`INTERFERENCE`).
5. **Siguiente:** nueva lista con `excludeRecentPairIds`.

### Reglas
- El cue de A-C es el MISMO que en A-B (eso crea la interferencia proactiva controlada).
- Recall bidireccional opcional (A→C y C→A) para medir simetría del binding.
- Error de conjunción (mezcla cue de un par con target de otro) se mide aparte de la intrusión A-B.

### Inputs
Selección múltiple (banco de targets + distractores), teclado opcional para producción libre con matching
tolerante. Sin texto libre no validado.

### Variables de dificultad
- `pairCount` (eje `LOAD`): **3–10** pares.
- `overlap A-B/A-C` (eje `INTERFERENCE`): **0% → 100%** de cues compartidos.
- `semanticSimilarity` (eje `SIMILARITY`): targets de distinta vs misma categoría (0→1).
- `delayMs` (eje `DELAY`): **0 → 24 h** (vía `DeferredRecall`/`DeferredRecallScheduler`).
- `studyMsPerPair`: **2000→900 ms**.
- `recallDirection` (eje `RETRIEVAL`): forward → bidireccional (≥0.5).
- `encodingPrompt` (eje `DEPTH`): rote → elaborativo (sugerir imagen mental/keyword).

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD`. `specificWeakAxis`: intrusiones A-B altas → subir
`INTERFERENCE` (más overlap); olvido diferido → subir `DELAY`. Baseline = `ASSOCIATIVE_INDEX` sostenido.
`InterventionPlanner` → `ENCODING_STRATEGY_CHANGE` si meseta (rote→elaborativo).

### Métricas primarias
`ASSOCIATIVE_INDEX`, `CONJUNCTION_ERROR_RATE`, `INTRUSION_RATE` (A-B sobre A-C), `INTERFERENCE_INDEX`.

### Métricas secundarias
`DELAYED_RECALL_ACCURACY`, `RETRIEVAL_SPEED`, asimetría forward/backward, `LEARNING_CURVE_SLOPE`,
consistencia.

### Señales de fatiga o mala calidad
Intrusiones A-B disparándose (interferencia ganando), errores de conjunción altos (binding ruidoso),
accuracy ≈ azar en el banco, RT de selección creciente.

### Scoring sugerido
```
associativeIndex = correctTargets / pairCount
proactiveInterference = abIntrusions / (abIntrusions + correctC)
conjunctionRate = conjunctionErrors / probes
interferenceIndex = recall_AC_withOverlap / recall_AC_noOverlap   // <1 = interferencia
```
`confident=false` si pares < umbral.

### Antitrampa / calidad de datos
Banco con distractores que incluyen los targets B (detecta intrusión real, no azar). Cues idénticos A-B/A-C
verificados. Producción libre con normalización (lemmatize) y matching tolerante para no penalizar typos.
Demora real anclada a `EpochClock` (no se puede saltar).

### Variantes  (≥3 concretas)
1. **A-B/A-C clásico** (interferencia proactiva, default).
2. **Vocabulario L1-L2** (keyword method, `TransferProtocols.VOCABULARY`).
3. **Bidireccional simétrico** (mide si A→C y C→A son igual de fuertes).
4. (V2) **Caras-nombres** como caso asociativo de alto valor (`EX8_NOMBRES_ROSTROS`).

### UX
Estudio: tarjeta cue↑/target↓ con prompt de estrategia opcional. Recall: cue grande + banco inferior.
Microanimación funcional: al re-aprender A-C, el cue "parpadea" señalando "mismo cue, nuevo target".
Duración: estudio 30–60 s, recall 30–60 s, sesión 6–8 min (+ probe diferida 24 h).

### Implementación KMP
```kotlin
// Reusa PairedAssociatesTrial / CuedRecallProbe / RecallDirection (MemoryTrialModels.kt) + Ex4PairsGenerator.
// Añadir fase A-C: lista secundaria con cues compartidos.
@Serializable data class InterferenceProbeResult(
    val correct: Boolean, val abIntrusion: Boolean, val conjunction: Boolean, val rtMs: Long,
)
```
`MemoryExerciseId.EX4_PARES_ASOCIADOS` ya jugable; extender `Ex4PairsGenerator` con `overlap`. Demora 24 h
vía `DeferredRecallScheduler` + `LocalDeferredRecallRepository`. Eventos: `paired_probe`
(correct/abIntrusion/conjunction), `paired_deferred_due`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine`, `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect ALTO (`PracticeEffectRisk.HIGH`): pares se memorizan; obligatorio rotar contenido vía
`RecentItemRegistry` y medir solo sobre material nuevo. Demora 24 h depende de re-engagement (riesgo de
abandono de la probe diferida).

### Revisión crítica
"Pares asociados con HIGH practice-effect: si repites el mismo banco, el 'progreso' es memorización de
ítems, no mejora de capacidad asociativa." **Correcciones exigidas:** (1) contenido SIEMPRE nuevo por
sesión (excludeRecentPairIds estricto); (2) la mejora se afirma solo sobre pares UNTRAINED de la sonda;
(3) interferencia A-B/A-C obligatoria para que el ejercicio mida binding robusto y no calco; (4) probe
diferida real anclada a reloj, no simulada.

### Veredicto
**Aprobado con cambios** — premium solo con rotación estricta de contenido y A-B/A-C como núcleo.

---

## Episódica con Recall Diferido y Reconstrucción (rediseño premium de EX3)

### Módulo  (principal + secundarios)
Principal: M2_DEEP_ENCODING (`EPISODIC`). Secundarios: M6 (`METAMEMORY_CALIBRATION`), M5
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria episódica de lista con codificación profunda (elaborativa) y consolidación: estudiar material,
recuperar inmediato Y diferido (mismo día + 24 h), midiendo curva de olvido, organización (clustering) y
beneficio de la estrategia de codificación.

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.STUDY` (`study_efficient`, `v1Available=true`) y `ACTIVE_READING`.
Codificación elaborativa y testing-effect transfieren a estudio real. NO afirmar mejora clínica de
memoria ni prevención de declive.

### Loop principal
1. **Estímulo:** lista de palabras (categorizable) con prompt de estrategia (agrupar/historia/imagen).
2. **Decisión:** recall inmediato (libre o cued por categoría); recall diferido mismo-día y +24 h.
3. **Feedback:** diferido: aciertos, intrusiones, clustering (ARC), comparación inmediato vs diferido.
4. **Adaptación:** sube `LOAD`, `DELAY`, `SIMILARITY`, o exige codificación más profunda (`DEPTH`).
5. **Siguiente:** nueva lista; la probe +24 h se agenda con `DeferredRecallScheduler`.

### Reglas
- La estrategia de codificación se SUGIERE y se mide su uso vía clustering (ARC), no se impone.
- Recall diferido +24 h es parte del scoring de consolidación (no opcional para la métrica de mejora real).
- Intrusiones extralista penalizan (control de falsas memorias).

### Inputs
Selección múltiple o producción libre con matching tolerante (lemmatize). Voz opcional (V2, transcripción
local) para recall hablado. Texto libre normalizado.

### Variables de dificultad
- `listLength` (eje `LOAD`): **6–24** palabras.
- `categoryStructure` (eje `DEPTH`/`SIMILARITY`): aleatoria → categorizable (favorece clustering).
- `immediateDelayMs`: **0 → 60 s** (con tarea distractora para impedir rehearsal).
- `longDelay` (eje `DELAY`): mismo día (~20 min) y **24 h** (vía scheduler).
- `studyMsPerItem`: **2500→1000 ms**.
- `interferenceList` (eje `INTERFERENCE`): **0→1** lista B intermedia (interferencia retroactiva).
- `recallType` (eje `RETRIEVAL`): cued por categoría → libre.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD`. `delayedLags()`: si `DELAYED_RECALL_ACCURACY` <<
`IMMEDIATE_RECALL_ACCURACY` → subir `DELAY` no, sino reforzar consolidación (más espaciado); si
clustering bajo → `ENCODING_STRATEGY_CHANGE`. Baseline = `DELAYED_RECALL_ACCURACY`. `PlateauDetector`
distingue FalseImprovement (sube inmediato pero no diferido).

### Métricas primarias
`DELAYED_RECALL_ACCURACY` (gobernante), `IMMEDIATE_RECALL_ACCURACY`, `ARC_CLUSTERING`,
`CONSOLIDATION_INDEX`, `INTRUSION_RATE`.

### Métricas secundarias
`FORGETTING_RATE` (curva de olvido), `SPACED_REPETITION_BENEFIT`, `LEARNING_CURVE_SLOPE`,
`CALIBRATION_INDEX` (JOL: predice cuántas recordará), consistencia.

### Señales de fatiga o mala calidad
Inmediato sube pero diferido plano (FalseImprovement por `PlateauDetector`), intrusiones extralista
subiendo (falsas memorias), clustering bajo persistente (no codifica profundo), recall ≈ primacía/recencia
sin medio (rehearsal superficial).

### Scoring sugerido
```
immediateAcc = recalledImmediate / listLength
delayedAcc   = recalledDelayed24h / listLength
forgettingRate = (immediateAcc - delayedAcc) / immediateAcc
arcClustering = (observedCategoryRuns - expected) / (maxRuns - expected)   // ARC de Roenker
consolidationIndex = delayedAcc / immediateAcc
intrusionRate = extraListIntrusions / responses
```
`confident=false` si la probe +24 h no se completó (mejora real requiere ambos puntos).

### Antitrampa / calidad de datos
Tarea distractora antes del recall inmediato bloquea rehearsal (mide LTM, no buffer). Probe +24 h anclada
a `EpochClock` real. Matching tolerante con lemmatize; intrusiones extralista detectan invención. Lista B
controla interferencia retroactiva.

### Variantes  (≥3 concretas)
1. **Free recall categorizable** (mide ARC clustering, default).
2. **Cued recall por categoría** (aísla disponibilidad vs accesibilidad).
3. **Narrativa episódica** (`EX15_NARRATIVA_AUDITIVA`): recordar quién/qué/orden de una historia.
4. **Testing-effect:** recall repetido vs re-estudio, mide beneficio de recuperar.

### UX
Estudio: palabras una a una con prompt de estrategia y, opcional, "agrúpalas tú". Recall: campo + banco /
producción libre. Microanimación funcional: al estudiar, las de la misma categoría comparten un tinte
sutil solo si el usuario activa "ayuda de agrupado". Duración: estudio 30–90 s, recall 60 s; probe +24 h
2–3 min al día siguiente.

### Implementación KMP
```kotlin
// Reusa RecallListTrial (studyList, recallType, studyMsPerItem, delayMs, cueCategory, expected) + Ex3ListGenerator.
@Serializable data class EpisodicRecallResult(
    val immediateRecalled: List<String>, val delayedRecalled: List<String>,
    val extraListIntrusions: Int, val arcClustering: Double, val jolPrediction: Int?,
)
```
`MemoryExerciseId.EX3_LISTA_ESTRATEGIA` ya jugable. Probe +24 h: `DeferredRecall` + `DeferredRecallScheduler`
+ `LocalDeferredRecallRepository`. ARC vía `ArcClustering`. Eventos: `episodic_immediate_recall`,
`episodic_delayed_due`, `episodic_intrusion`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine`, `PlateauDetector` (FalseImprovement), `ReliableChangeIndex` (cambio fiable en
diferido). Persistir `MemorySessionSummary` (immediateAccuracy, delayedAccuracy).

### Riesgos
Practice effect ALTO (`PracticeEffectRisk.HIGH`): listas se memorizan; rotación obligatoria. La probe +24 h
puede no completarse → la claim de "mejora real" queda gated (correcto, pero reduce datos). Producción libre
con typos.

### Revisión crítica
"Es 'memoriza una lista', el ejercicio más viejo y más sujeto a practice effect del mundo. Sin recall
diferido real es solo un buffer test, y si reutilizas listas mides familiaridad." **Correcciones exigidas:**
(1) recall diferido +24 h ANCLADO a reloj es obligatorio para la métrica de mejora; (2) tarea distractora
antes del inmediato para no medir recencia; (3) contenido nuevo por sesión + sonda UNTRAINED;
(4) `PlateauDetector.FalseImprovement` debe vigilar el gap inmediato↔diferido.

### Veredicto
**Aprobado con cambios** — deja de ser "memoriza números" solo con diferido real + distractor + rotación.

---

## Orden Serial Temporal (secuencias forward/backward)

### Módulo  (principal + secundarios)
Principal: M4_SPATIAL_VISUAL (`SEQUENTIAL`, paradigma `SERIAL_ORDER`). Secundarios: M1
(`WORKING_MEMORY`), M2 (`EPISODIC`).

### Objetivo cognitivo
Memoria del ORDEN (no del contenido): reproducir una secuencia de elementos en la posición temporal
correcta. Aísla el componente de orden serial, disociable del item memory (puedes saber QUÉ ítems sin
saber su ORDEN).

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.PRESENTATIONS`, `INSTRUCTIONS`, `PENDING_TASKS` (secuencias de pasos,
método de loci). NO afirmar mejora general de "memoria" ni de planificación.

### Loop principal
1. **Estímulo:** secuencia de s elementos (iconos/posiciones/sílabas) presentados uno a uno.
2. **Decisión:** reproducir el orden (forward o backward) tocando los elementos en secuencia.
3. **Feedback:** diferido: posiciones correctas, errores de transposición (ítem correcto, posición ±1),
   distancia de edición.
4. **Adaptación:** sube longitud (`LOAD`), similitud (`SIMILARITY`), demora (`DELAY`) o backward (`RETRIEVAL`).
5. **Siguiente:** nueva secuencia.

### Reglas
- Puntuar por POSICIÓN (no por "ítems presentes"): un ítem en posición equivocada NO es acierto pleno.
- Medir errores de transposición (intercambios adyacentes) aparte de omisiones e intrusiones.
- Backward recall como palanca de `RETRIEVAL` (mayor demanda ejecutiva).

### Inputs
Tap secuencial sobre un banco de elementos; drag para reordenar una fila. Sin teclado.

### Variables de dificultad
- `sequenceLength` s (eje `LOAD`): **3–9** elementos.
- `direction` (eje `RETRIEVAL`): forward → backward (≥0.5).
- `itemSimilarity` (eje `SIMILARITY`): elementos distintos → confusables (0→1).
- `presentationRateMs`: **1000→500 ms** por elemento.
- `delayMs` (eje `DELAY`): **0 → 6000 ms** con máscara.
- `concurrentTap` (eje `CONCURRENCY`): **0→1** tarea de tap rítmico durante la presentación (suprime rehearsal).
- `grouping`: presentación agrupada (chunking visible) → continua.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (s). `specificWeakAxis`: transposiciones altas → subir
`SIMILARITY`; olvido con demora → subir `DELAY`. Backward solo se introduce con forward estable. Baseline =
`SERIAL_ORDER_ACCURACY` sostenido.

### Métricas primarias
`SERIAL_ORDER_ACCURACY`, `IMMEDIATE_RECALL_ACCURACY` (item memory, para disociar), `INTRUSION_RATE`.

### Métricas secundarias
Transposition rate (curva de gradiente posicional), `RETRIEVAL_SPEED`, span de orden vs span de ítem
(disociación), consistencia, `LEARNING_CURVE_SLOPE`.

### Señales de fatiga o mala calidad
Order accuracy cayendo con item accuracy estable (orden fallando selectivamente), transposiciones
adyacentes disparadas, respuesta aleatoria (gradiente plano = azar), RT alto.

### Scoring sugerido
```
serialOrderAcc = positionsCorrect / s
itemAcc        = itemsPresentRegardlessOfOrder / s
transpositionRate = adjacentSwaps / s
orderSpecificScore = serialOrderAcc - (itemAcc * chanceOrderGivenItems)  // orden por encima de item memory
```
`confident=false` si trials < umbral.

### Antitrampa / calidad de datos
Scoring por posición impide "los toqué todos" como acierto. Tap rítmico concurrente suprime rehearsal
articulatorio. Banco mezclado por trial. Gradiente posicional (errores deben concentrarse en medio):
gradiente plano = respuesta azarosa, marca baja calidad.

### Variantes  (≥3 concretas)
1. **Forward span de orden** (Corsi/Digit-span de orden, default).
2. **Backward span** (mayor demanda ejecutiva).
3. **Reconstrucción de orden:** se dan los ítems desordenados, ordenarlos (aísla orden de item recall).
4. **Orden con supresión articulatoria** (tap concurrente).

### UX
Presentación secuencial con resalte por elemento (microanimación funcional: "destello" por paso =
codificación temporal). Reproducción: banco + fila de orden con números de posición. Duración: trial
10–20 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class SerialItem(val itemId: Int, val position: Int)
data class SerialOrderTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val sequence: List<SerialItem>, val direction: RecallDirection,
    val delayMs: Long, val expectedOrder: List<Int>,    // scoring key embebida
) : MemoryTrialPlan
class Ex7SerialGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX7_SECUENCIAS` ya en catálogo (governingMetric `SERIAL_ORDER_ACCURACY`); falta generador.
Eventos: `serial_reproduction` (positionsCorrect, transpositions), `serial_item_vs_order`. Motores:
`DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`, `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO. Riesgo de que el usuario use chunking (mitigado por `grouping` continuo y tap
concurrente). Confusión con span de ítem si no se disocia (mitigado por reconstrucción de orden).

### Revisión crítica
"Sin disociar order memory de item memory, esto es indistinguible de un span más. Y si scoreas 'ítems
presentes' premias recall de ítem, no de orden." **Correcciones exigidas:** (1) scoring estrictamente por
posición (hecho); (2) variante de reconstrucción de orden que da los ítems para aislar orden puro; (3)
medir transposiciones y gradiente posicional para validar que la señal es de orden; (4) supresión
articulatoria opcional para bloquear rehearsal verbal del orden.

### Veredicto
**Aprobado** — aporta una dimensión cognitiva distinta (orden) si se disocia explícitamente del item memory.

---

## Sonda de Transferencia UNTRAINED (Probe de capacidad latente)

### Módulo  (principal + secundarios)
Transversal a todos los módulos. Mide capacidad, no entrena. Usa material y a veces paradigmas RESERVADOS
(`TransferProbePartition`), nunca vistos en entrenamiento.

### Objetivo cognitivo
Estimar la capacidad cognitiva REAL (no la familiaridad con un ejercicio entrenado) administrando tareas
con material/paradigma UNTRAINED. Es el patrón de oro de honestidad del sistema: la única evidencia
admisible de mejora genuina (`RealImprovementCriterion.untrainedDelta`).

### Hipótesis de transferencia
Por construcción mide transferencia: near (mismo paradigma, material nuevo) y far (paradigma relacionado
no entrenado, p. ej. complex span no practicado). SÍ se afirma: "tu capacidad funcional cambió de forma
fiable" SOLO si `ReliableChangeIndex.compute` da `reliable=true` (|RCI|>1.96) Y
`RealImprovementCriterion.isGenuine`. NO se afirma nada con una sola administración.

### Loop principal
1. **Estímulo:** mini-batería corta de ítems UNTRAINED (material reservado por `TransferProbePartition`),
   paradigmas estandarizados, dificultad FIJA (no adaptativa — es medición, no entrenamiento).
2. **Decisión:** el usuario responde igual que en el ejercicio base, pero sin feedback de rendimiento
   (no se entrena al medir).
3. **Feedback:** NINGUNO durante la probe (evita aprendizaje); solo al final, un resumen honesto de tendencia.
4. **Adaptación:** NO hay adaptación intra-probe (dificultad fija = comparabilidad pre/post).
5. **Siguiente:** se administra periódicamente (cada N sesiones / semanal), no cada sesión.

### Reglas
- Dificultad FIJA y estandarizada entre administraciones (comparabilidad).
- Material SIEMPRE reservado (`TransferProbePartition`): nunca aparece en entrenamiento.
- Sin feedback de acierto durante la probe (no entrena, mide).
- Requiere ≥2–3 administraciones antes de computar RCI (hasta entonces, solo delta crudo con caveat).

### Inputs
Idénticos al ejercicio base medido (span, recall, recognition) para validez de constructo. Sin inputs nuevos.

### Variables de dificultad
La probe es de dificultad FIJA por diseño; lo que varía es la COBERTURA de constructos, no la dificultad:
- `probeBatteryCoverage`: **1–4** subcapacidades medidas por administración.
- `itemsPerConstruct`: **8–20** ítems (más = `confident=true` antes; menos = más rápido).
- `fixedDifficultyLevel`: anclado al ~percentil 50 poblacional (estándar, NO adaptativo).
- `parallelFormId`: **A/B/C** formas paralelas equiparadas (rota para evitar memorización entre probes).
- `delayProbeMs`: incluye un punto diferido (+24 h) para `CONSOLIDATION_INDEX` UNTRAINED.

### Algoritmo adaptativo
NINGUNO intra-probe (la fijeza es el punto). El "adaptativo" del sistema solo decide CUÁNDO administrar:
tras un bloque de progreso aparente en entrenado, o si `PlateauDetector` reporta posible FalseImprovement,
se dispara una probe para verificar si la mejora es real o familiaridad. Formas paralelas (A/B/C) rotan por
administración.

### Métricas primarias
`NEAR_TRANSFER_INDEX`, el delta UNTRAINED que alimenta `RealImprovementCriterion.untrainedDelta`, y el
`ReliableChangeIndex` (RCI) sobre la métrica gobernante del constructo medido.

### Métricas secundarias
`effectSize` (Cohen's d para `RealImprovementCriterion.effectSize`), `sustainedWeeks`, `CONSOLIDATION_INDEX`
UNTRAINED, comparación trained-delta vs untrained-delta (clasifica `ImprovementPattern`).

### Señales de fatiga o mala calidad
Probe administrada con fatiga alta (invalida la medición — el sistema NO debe lanzar probe si
`MemoryFatigue` es HIGH), reliability aún no estimada (`RciResult.NotEnoughData`), n insuficiente
(`confident=false`), varianza pre anómala.

### Scoring sugerido
```
untrainedDelta = probePostScore - probePreScore                      // RealImprovementCriterion
effectSize     = untrainedDelta / pooledSD                           // Cohen's d
RCI = (post - pre) / (sqrt(2) * sdPre * sqrt(1 - reliability))       // ReliableChangeIndex.compute
pattern = RealImprovementCriterion.classify(ImprovementEvidence(
    trainedDelta, untrainedDelta, immediateDelta, delayedDelta, effectSize, sustainedWeeks))
genuine = isGenuine(...) && (RCI.reliable == true)
```
Claim de mejora SOLO si `genuine && RCI reliable`. Si no: "progreso en el ejercicio entrenado; aún sin
evidencia de cambio de capacidad" (honesto).

### Antitrampa / calidad de datos
Material reservado (`TransferProbePartition`) garantiza que no es lo entrenado. Formas paralelas A/B/C
impiden memorizar la propia probe. Sin feedback evita que la probe entrene. Gate de RCI (`NotEnoughData`
hasta tener reliability) impide claims prematuros. No se administra con fatiga alta.

### Variantes  (≥3 concretas)
1. **Near-transfer probe:** mismo paradigma que el entrenado, material reservado (p. ej. span con
   palabras nunca usadas).
2. **Far-transfer probe:** paradigma relacionado NO entrenado (entrenó span verbal → se mide updating o
   span visoespacial).
3. **Consolidation probe:** componente +24 h UNTRAINED para `CONSOLIDATION_INDEX` real.
4. **Calibration probe:** añade JOL (predicción de rendimiento) → `CALIBRATION_INDEX` UNTRAINED.

### UX
Marco visual distinto y honesto: "Esto es una MEDICIÓN, no entrenamiento. Sin pistas, sin feedback —
para ver tu capacidad real." Sin gamificación de puntos durante la probe. Resumen final sobrio con caveats
(`RciResult.NotEnoughData` → "necesitamos más mediciones para confirmar tendencias"). Duración: 3–6 min,
periódica.

### Implementación KMP
```kotlin
// TransferProbePartition ya reserva material UNTRAINED; ReliableChangeIndex + RealImprovementCriterion existen.
@Serializable data class TransferProbeSummary(
    val constructKey: MemoryMetricKey, val score: Double, val n: Int,
    val parallelForm: String, val administeredAtEpoch: Long, val fatigueWasLow: Boolean,
)
data class TransferProbePlan(
    override val exerciseId: MemoryExerciseId,         // EX_TRANSFER_PROBE
    val items: List<MemoryTrialPlan>, val fixedDifficulty: MemoryDifficultyVector,
    val parallelForm: String,
) : MemoryTrialPlan
```
Nuevo `MemoryExerciseId.EX_TRANSFER_PROBE` (NO en `implemented` como drill normal; orquestado por el
scheduler de sesión). Persistir solo `TransferProbeSummary` en `MindBoostStorageNamespaces.Memory` vía
`SecureKeyValueStore` (`@Serializable`, `schemaVersion`). Eventos: `transfer_probe_administered`,
`reliable_change_computed` (rci, reliable). Motores: `ReliableChangeIndex`, `RealImprovementCriterion`,
`PlateauDetector` (dispara la probe ante FalseImprovement). Tiempo vía `EpochClock`.

### Riesgos
Si las formas paralelas A/B/C no están equiparadas en dificultad, el delta se contamina (riesgo
psicométrico real → requiere equating). Coste de re-engagement de la componente +24 h. Usuarios pueden
percibir la probe como "examen" y abandonar (mitigar con framing positivo y brevedad).

### Revisión crítica
"Esta es la pieza más fácil de hacer mal: si las formas no están equiparadas, mides ruido y luego declaras
'mejora fiable' con un RCI basura. Y administrar la probe con el usuario cansado invalida todo." **Correcciones
exigidas:** (1) formas paralelas equiparadas (equating estadístico) ANTES de comparar; (2) NO administrar
con fatiga HIGH (gate explícito); (3) RCI gated por reliability estimada con ≥2–3 administraciones
(`RciResult.NotEnoughData` por defecto); (4) la claim de mejora requiere RCI reliable Y
`RealImprovementCriterion.isGenuine` simultáneamente; (5) cero feedback durante la probe.

### Veredicto
**Aprobado con cambios** — es el corazón de la honestidad del módulo; válido solo con formas equiparadas,
gate de fatiga y RCI gated por reliability.

---

## Resumen de cobertura cognitiva

| # | Ejercicio | Subcapacidad principal | Paradigma | Eje gobernante | Métrica gobernante |
|---|-----------|------------------------|-----------|----------------|--------------------|
| 1 | Span Adaptativo Dual | WORKING_MEMORY | COMPLEX_SPAN | LOAD | WORKING_MEMORY_SPAN |
| 2 | N-Back Doble Modalidad | WORKING_MEMORY | (n-back, nuevo) | LOAD (n) | WORKING_MEMORY_SPAN / RECOGNITION_DPRIME |
| 3 | Updating de Ranura | WORKING_MEMORY | RUNNING_MEMORY | LOAD (k) | WORKING_MEMORY_SPAN |
| 4 | Grid Espacial con Binding | SPATIAL | SPATIAL_GRID | LOAD (c) | COWAN_K |
| 5 | Pares A-B/A-C | ASSOCIATIVE | PAIRED_ASSOCIATES | LOAD | ASSOCIATIVE_INDEX |
| 6 | Episódica Diferida | EPISODIC | FREE_RECALL | LOAD | DELAYED_RECALL_ACCURACY |
| 7 | Orden Serial Temporal | SEQUENTIAL | SERIAL_ORDER | LOAD (s) | SERIAL_ORDER_ACCURACY |
| 8 | Sonda de Transferencia UNTRAINED | transversal | (medición fija) | — (fijo) | NEAR_TRANSFER_INDEX + RCI |

Los 8 cubren WM verbal (1,2,3), WM visoespacial (4), binding asociativo (5), episódica con diferido (6),
orden serial (7), updating/running memory (3) y la sonda UNTRAINED de transferencia (8) — sin solapar el
mismo loop. Toda claim de mejora real pasa por `RealImprovementCriterion` + `ReliableChangeIndex` sobre la
sonda UNTRAINED; `PlateauDetector`/`InterventionPlanner` gobiernan el largo plazo; el adaptativo mueve UN
eje del `MemoryDifficultyVector` por decisión.
