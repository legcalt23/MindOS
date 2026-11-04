## Modulo: Memoria

I now have a complete, verified picture. The decisive gap: the **live session path** (`MemorySessionStateHolder` → `MemorySessionScreen`) bypasses nearly all the scientific machinery. The generators emit timing/delay/serial/processing-task structure that the UI never renders; recall is a single untimed recognition multi-select; the assembler, deferred recall, recovery, partition-based probes, RCI, RealImprovement, plateau and fitness reporting are built and tested but never invoked from a real session. I'll now write the audit.

## Ejercicio: Span Complejo (EX1_SPAN_COMPLEJO)

### Diagnóstico rápido
El dominio genera una tarea de span complejo legítima (memoranda intercalados con verificación aritmética, recuerdo forward/backward); el **runtime la destruye**. `MemorySessionStateHolder.studyOf` (líneas 204-220) presenta los memoranda como `Text` estáticos sin tiempo, **descarta por completo los `ProcessingTask`** (nunca se muestran ni se responden), y el recall es un multi-select de reconocimiento sobre `studied + 6 distractores` (`candidatesFor`, líneas 222-237) que **ignora el orden serial**. Lo que el usuario hace de verdad no es span complejo: es reconocimiento sí/no de 2-8 palabras vistas hace segundos. No vale en su forma actual.

### Mecanismo cognitivo
El objetivo declarado es memoria de trabajo bajo carga con interferencia dual-task (`primaryMetrics = WORKING_MEMORY_SPAN, DUAL_TASK_COST`, catálogo líneas 38-43). El mecanismo que *debería* entrenar (mantenimiento activo + procesamiento concurrente + recuperación ordenada) es correcto y plausible **en el papel del generador** (`Ex1SpanGenerator`, líneas 77-88).
- **Lo que produce adaptación cognitiva:** mantener N ítems mientras un distractor consume recursos ejecutivos, en el límite del span. Eso es exactamente lo que NO ocurre en runtime.
- **Lo que SOBRA:** nada en el generador; el problema es por defecto.
- **Lo que CONTAMINA la medición:** (1) la presentación simultánea y sin límite temporal de todos los memoranda (`StudyContent`, `MemorySessionScreen` líneas 74-83) permite repaso/subvocalización ilimitada y elimina la carga; (2) el `ProcessingTask` no se ejecuta → no hay dual-task → `DUAL_TASK_COST` es incomputable y la tarea colapsa a span simple; (3) el reconocimiento con 6 distractores es trivial frente al recall serial: acertar por familiaridad infla `WORKING_MEMORY_SPAN`, que se computa como `trial.hits.toDouble()` (`MemoryMetricsEngine` líneas 77-86) — es decir, **número de reconocimientos correctos, no span**.
- **Lo que es solo práctica de la tarea:** todo, dado el banco finito y el reconocimiento (ver riesgos).

### Potencial de transferencia
**Nula** en el estado actual. La WM entrenada con span complejo *real* tiene transferencia cercana documentada y discutida (Gf es polémico), pero aquí no se entrena WM: se entrena reconocimiento episódico inmediato de palabras españolas. Mejora-en-la-tarea: alta (memorizar 6 palabras y reconocerlas es fácil y se satura). Transferencia cercana: nula (no hay carga ni dual-task). Funcional a la vida real: nula. Para que suba a "baja/media": **renderizar la presentación serial temporizada, ejecutar los `ProcessingTask` con su propia respuesta y latencia, y exigir recall por producción/reconstrucción de orden, no multi-select.**

### Problemas detectados
- `MemorySessionStateHolder.studyOf` (204-209): presenta `expectedRecall` como lista; **el `steps` con los `ProcessingTask` se ignora**. El dual-task no existe en producción.
- `MemorySessionScreen.StudyContent` (74-83): sin temporización; `studyMsPerItem`/orden de presentación no se usan. `ComplexSpanTrial` no tiene tiempos, pero el holder tampoco impone ninguno.
- `score()` (241-244): para EX1 construye `recalled` mapeando `expectedRecall` en su orden canónico y marcando hit si el id está `in selected`. **Esto evalúa pertenencia al conjunto, no posición serial** → `ORDER_ERROR` (definido en `MemoryScorer.scoreComplexSpan` líneas 76-81) es **inalcanzable** desde el runtime: nunca se pasa una palabra en posición equivocada.
- `WORKING_MEMORY_SPAN = trial.hits.toDouble()` (`MemoryMetricsEngine` 81): no es un span (ni partial-credit unit normalizado, ni longitud de secuencia perfecta). Es un conteo crudo de aciertos dependiente del `studiedCount` del día → no comparable entre dificultades.
- `candidatesFor` (222-237): los distractores se muestrean del mismo sampler con `excludeRecentIds = answerWords`, así que no colisionan con la respuesta, pero **el número de distractores es fijo (6)** independientemente del span (2-8) → la tasa base de acierto por azar varía con la dificultad y no se corrige (sin d-prime).

### Riesgos de falsa mejora
- **Banco finito + reconocimiento:** ≈142 palabras totales, menos el 20% reservado (`TransferProbePartition`, ratio 0.20) → ~113 de entrenamiento (`SpanishMemoryContentData` líneas 65-121). Con reconocimiento de 2-8 ítems el techo se alcanza en sesiones y el "acierto %" sube por familiaridad con el pool, no por capacidad.
- **Dificultad falsa:** subir `LOAD` añade memoranda, pero como la presentación es estática y sin tiempo, no añade carga real → la curva de "dificultad" no mapea a esfuerzo cognitivo.
- **Feedback inflado:** `FinishedContent` muestra "Acierto: X% (hits/studied)" tras una tarea de reconocimiento trivial → el usuario percibe alto rendimiento sin entrenar WM.
- **`delayedAccuracy = null` siempre** (`finish`, línea 164): la nota honesta promete "recuerdo diferido" que el runtime nunca ejecuta.

### Métricas recomendadas
- **Principal:** span con partial-credit load scoring (proporción de ítems en posición correcta ponderada por longitud) o longitud de secuencia con 50% de aciertos (umbral adaptativo), NO conteo de hits.
- **Procesamiento:** exactitud y RT medio del `ProcessingTask` (gate de validez: descartar trials con accuracy de procesamiento < ~85%, el control estándar anti-trade-off en complex span).
- **Secundarias:** `DUAL_TASK_COST` real (span solo vs span+procesamiento), curva de posición serial (primacía/recencia), tasa de errores de orden vs de ítem, intrusiones.
- **Consistencia:** varianza intra-bloque del span.
- **Engañan hoy:** `WORKING_MEMORY_SPAN` (es un conteo de reconocimiento), `IMMEDIATE_RECALL_ACCURACY` (saturada por reconocimiento), cualquier % de acierto mostrado.

### Progresión recomendada
Mantener la zona de desafío en el span umbral (≈50-75% de secuencias perfectas) con escalera 1-up/1-down sobre **longitud**, no sobre conteo. Procesamiento como eje ortogonal de carga (1→2→3 operaciones por ítem, con ventana de tiempo). Evitar techo bloqueando el reconocimiento (usar producción) y evitar fondo con warmup a span−1.

### Rediseño propuesto
Mecánica: presentación **serial temporizada** (cada memorandum 1000-1500 ms), intercalando el `ProcessingTask` como pantalla con respuesta verdadero/falso y latencia registrada (ya existe en el plan, solo hay que renderizarlo). Recall por **reconstrucción de orden** (arrastrar/teclear la secuencia) o, en V1 mínimo, recall por producción libre (teclado) clasificado por `MemoryScorer.scoreComplexSpan` con alineación posicional real. Dificultad adaptativa: escalera sobre longitud + nº de operaciones, gobernada por el `DefaultMemoryAdaptiveEngine` que ya existe (hoy se invoca pero su zona se calcula sobre métricas falsas). Métricas: partial-credit span + processing accuracy/RT + dual-task cost. Feedback: span estimado + "operaciones correctas" (penalizar trade-off). Variantes: verbal/espacial. Test: simulación con respondedores sintéticos de span conocido — el span estimado debe recuperar el parámetro. Superior porque restaura la carga, el dual-task y la sensibilidad posicional que definen el paradigma; sin eso, no es span complejo.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **2** | 2) Transferencia a la vida real: **1** | 3) Claridad del mecanismo: **4** (claro en dominio, roto en runtime) | 4) Calidad de la progresión: **3** | 5) Calidad de la medición: **2** | 6) Personalización: **4** | 7) Resistencia al efecto práctica: **2** | 8) Valor metacognitivo: **1** (EX16 no cableado, `confidenceBias=0.0`) | 9) Valor emocional: **2** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **8** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **2** | 12) Adherencia: **4** | 13) Escalabilidad a perfiles distintos: **4** | 14) Robustez científica: **2** | 15) Prioridad de mejora: **9**
**Nota global del ejercicio: 3/10**
**Decisión final:** Rediseñar parcialmente — el generador es bueno; hay que conectar presentación serial temporizada, ejecución real del processing-task y recall por producción/orden. Sin eso, **eliminar** del set hasta que el runtime esté a la altura del dominio.

---

## Ejercicio: Lista con Estrategia (EX3_LISTA_ESTRATEGIA)

### Diagnóstico rápido
Free recall episódico con clustering ARC — el ejercicio con mejor base científica del trío. Pero el runtime lo convierte otra vez en **reconocimiento multi-select sin demora**, lo que mata su métrica estrella (`ARC_CLUSTERING` requiere recall por producción, no reconocimiento) y la demora declarada (`delayMs` hasta 5 min) **nunca se aplica** (`StudyContent` no espera nada; `finish` guarda `delayedAccuracy = null`). Vale el diseño; no vale la ejecución.

### Mecanismo cognitivo
Objetivo: memoria episódica con codificación elaborativa y estrategias de organización (clustering semántico), medida tras demora (`governingMetric = DELAYED_RECALL_ACCURACY`, catálogo 88). El mecanismo correcto: estudiar una lista, **organizarla activamente por categorías** y recuperarla libremente; el ARC mide si el usuario impuso estructura (señal de estrategia de codificación profunda, el corazón del entrenamiento).
- **Produce adaptación:** la presión por organizar/elaborar bajo capacidad limitada y recuperar tras demora.
- **SOBRA:** nada en el generador.
- **CONTAMINA:** (1) reconocimiento en vez de recall → **el clustering es inmedible** (en un multi-select el "orden de recuperación" lo fija el orden de los chips en pantalla, no el usuario; `putArcClustering` lee `trial.responses` filtrando HIT y mapeando `category`, líneas 109-112 — pero esas responses provienen de `selectedWords` que filtra `studyList` en su **orden de estudio**, `Candidates.selectedWords` líneas 282-283, no en orden de recuperación). El ARC computado es por tanto **artefactual**: refleja el orden en que el generador colocó la lista, no la organización del usuario; (2) sin demora, `IMMEDIATE_RECALL_ACCURACY` es lo único que se mide y `DELAYED_RECALL_ACCURACY` (la métrica gobernante) jamás se produce.
- **Solo práctica:** reconocer palabras del pool.

### Potencial de transferencia
**Muy baja** ahora; **media** si se arregla. El free recall con estrategias de codificación tiene el puente funcional más fuerte del módulo (`TransferProtocols.STUDY`, `v1Available=true`): estudio eficiente, retención a 24h. Pero ese puente exige recall por producción + demora real, ninguno presente. Mejora-en-la-tarea: alta. Cercana: baja. Funcional (estudiar/retener): potencialmente media **solo** con el rediseño.

### Problemas detectados
- **ARC artefactual:** `Candidates.selectedWords` (282-283) reconstruye los recalled filtrando `studyList` → preserva el orden de estudio, no el de recuperación. `ArcClustering.arcIndex` (líneas 26-35) cuenta repeticiones de categoría adyacentes **sobre ese orden falso**. La métrica de clustering no mide clustering. [Riesgo científico]
- **Demora ignorada:** `RecallListTrial.delayMs` (generado 0-300_000 ms, `Ex3DifficultyMapping` línea 39) nunca se consume; `MemoryMetricsEngine.putCommon` decide la clave por `trial.delayMs > 0` (41-44), pero el `scoreList` propaga `plan.delayMs` (línea 109) — así que **si el adaptativo sube DELAY, la métrica cambia de IMMEDIATE a DELAYED sin que haya habido ninguna demora real**. Es una etiqueta de demora sin demora. [Riesgo científico grave]
- **Sin distinción primacía/recencia, sin curva de posición serial** (no se registra orden de recuperación en absoluto).
- **Banco finito:** listas de 8-20 palabras desde ~113 de entrenamiento → solapamiento alto entre sesiones pese a `excludeRecentWordIds`; con `verticalScroll` el usuario ve toda la lista a la vez sin presión temporal.

### Riesgos de falsa mejora
- **Reconocimiento sobre banco pequeño:** `IMMEDIATE_RECALL_ACCURACY` se satura; el "% acierto" sube por familiaridad.
- **DELAYED etiquetado sin demora:** el sistema puede reportar "recuerdo diferido" alto que en realidad es recuerdo inmediato → **pseudoprogreso de consolidación**, justo lo que `RealImprovementCriterion.POOR_CONSOLIDATION` pretende detectar, pero ese criterio nunca se alimenta con datos reales.
- **ARC inflado/aleatorio** presentado como evidencia de "estrategia".

### Métricas recomendadas
- **Principal:** `DELAYED_RECALL_ACCURACY` con demora **real** (intra-sesión ≥ varios minutos con tarea distractora, y entre-sesión a 24h vía `DeferredRecallScheduler` que ya existe pero no se ejecuta en el test diferido).
- **Secundarias:** ARC **real** (sobre orden de recuperación capturado), curva de posición serial, índice de primacía/recencia, intrusiones (ya crudo, correcto por aprendizaje 2026-06-04), `CONSOLIDATION_INDEX` (existe `consolidationIndex` pero nadie lo llama con dos medidas reales).
- **Engañan hoy:** `ARC_CLUSTERING` (artefactual), `DELAYED_RECALL_ACCURACY` (etiqueta sin demora), cualquier % inmediato.

### Progresión recomendada
LOAD (8→20) + DEPTH (menos tiempo de estudio fuerza elaboración, ya en `Ex3DifficultyMapping` 37) + DELAY real escalado. Zona óptima ~60-75% de recall diferido. Evitar techo con recall por producción (mucho más sensible que reconocimiento); evitar fondo con listas cortas categorizadas y cue de categoría (`CUED` ya existe).

### Rediseño propuesto
Recall por **producción libre** (teclado): el usuario escribe lo que recuerda, en su orden → captura el orden de recuperación real para ARC y curva serial. Demora real: insertar tarea distractora (el propio `ProcessingTask` de EX1 sirve) entre estudio y test; y **activar el flujo de recuerdo diferido a 24h** (`DeferredRecallScheduler.due` + `MemoryHomeStateHolder.dueDeferredCount` ya lo cuentan, pero no hay pantalla que ejecute el test diferido). Métricas: delayed real + ARC real + consolidación. Feedback: "recordaste X/Y, y tu organización por categorías fue alta/baja", más el resultado a 24h al día siguiente. Variantes: listas categorizables vs no categorizables (manipula la oportunidad de clustering). Test: respondedores sintéticos con clustering conocido → ARC debe recuperarlo; respondedores con olvido conocido → consolidación debe recuperarlo. Superior porque restaura las dos señales que justifican el ejercicio (organización estratégica y retención diferida), hoy ambas falsas.

### Puntuación 0-10 (los 15 ejes)
1) **3** | 2) **2** | 3) **5** | 4) **4** | 5) **2** (ARC y delayed falsos) | 6) **4** | 7) **2** | 8) **2** | 9) **3** | 10) **8** | 11) **2** | 12) **5** | 13) **4** | 14) **3** | 15) **9**
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente — el generador, el ARC y el delayed/consolidation son ciencia correcta; hay que (a) capturar recall por producción y orden, (b) aplicar demora real, (c) cablear el test diferido a 24h. Es el ejercicio con mayor retorno del módulo si se arregla.

---

## Ejercicio: Pares Asociados (EX4_PARES_ASOCIADOS)

### Diagnóstico rápido
Memoria asociativa con cued recall, errores de conjunción y dirección bidireccional — el paradigma con mayor utilidad declarada en la vida real (vocabulario, nombres). El generador es sólido (`Ex4PairsGenerator`), pero el runtime vuelve a colapsarlo: la prueba con **cue** desaparece, se convierte en reconocimiento multi-select de los **targets**, y el `CONJUNCTION_ERROR` se vuelve casi inalcanzable porque el usuario nunca elige un target equivocado dado un cue (no hay cue). Diseño bueno, ejecución que neutraliza su discriminación.

### Mecanismo cognitivo
Objetivo: binding asociativo cue→target y su resistencia a confusión (`governingMetric = ASSOCIATIVE_INDEX`, `CONJUNCTION_ERROR_RATE`, catálogo 116-123). El mecanismo correcto: estudiar pares arbitrarios y, dado el cue, recuperar el target — el binding relacional, distinto del memoria de ítem.
- **Produce adaptación:** formar y recuperar vínculos arbitrarios bajo interferencia (pares débiles, bidireccionalidad).
- **SOBRA:** nada en el generador.
- **CONTAMINA:** (1) `score()` para pares (líneas 249-254) mapea `plan.probes` y marca hit si `p.expectedTarget.id.value in selected` — **el cue nunca se presenta como tal**; el usuario ve `studied + distractores` y selecciona los que reconoce. Esto mide reconocimiento de ítems estudiados, **no recuperación dirigida por cue**; (2) por eso `CONJUNCTION_ERROR` (un target estudiado pero de otro par, `MemoryScorer.scorePairs` 128) es prácticamente inalcanzable desde el runtime: requiere que el usuario, *dado un cue concreto*, responda el target de *otro* par — imposible sin cue; (3) la bidireccionalidad (`RecallDirection.BACKWARD`) se genera pero es invisible en reconocimiento.
- **Solo práctica:** reconocer palabras-target del pool.

### Potencial de transferencia
**Muy baja** ahora; **media-alta** si se arregla — es el candidato con mejor puente funcional (`TransferProtocols.VOCABULARY`, `v1Available=true`: pares L1-L2, retención a 24h/7d). El cued-recall asociativo *real* transfiere a aprendizaje de vocabulario y, con caras, a nombres. Pero requiere presentar el cue y exigir el target por producción. Mejora-en-la-tarea: alta. Cercana: baja. Funcional (idiomas): media-alta **solo** con rediseño.

### Problemas detectados
- **Sin cue en la prueba:** `MemorySessionStateHolder.candidatesFor` (222-237) construye candidatos desde `answerWords` (= targets distintos, `studyOf` 218) + distractores; `RecallContent` los muestra todos juntos. El `CuedRecallProbe.cue` nunca llega a la UI. El paradigma cue→target no se ejerce. [Riesgo científico grave]
- **`ASSOCIATIVE_INDEX = hits/studiedCount`** (`MemoryMetricsEngine` 91-94) donde `studiedCount = plan.probes.size` (`scorePairs` 137) — mide reconocimiento de targets, etiquetado como índice asociativo.
- **Cap de 10 pares** (`Ex4DifficultyMapping` 38) por tamaño de pool; documentado, pero limita el techo de carga.
- **Fan-effect / interferencia diferida** (pares que comparten palabras) explícitamente no soportado (comentario líneas 26-30) → la métrica `CONJUNCTION_ERROR` carece de la condición que la haría informativa aun con cue.
- **`studyMsPerPair` y `delayMs` ignorados** por el runtime (mismo defecto que EX1/EX3).

### Riesgos de falsa mejora
- **Reconocimiento de targets sobre banco finito:** 56 pares totales − 20% reservado ≈ 44 de entrenamiento (`SpanishMemoryContentData` línea 124, `TransferProbePartition` ratio 0.20). Se memoriza el pool en pocas sesiones.
- **`CONJUNCTION_ERROR ≈ 0` siempre** → el sistema reporta "resistencia a la confusión perfecta" que es un artefacto de no presentar cue.
- **Bidireccionalidad fantasma:** subir RETRIEVAL genera probes B→A que no cambian nada en reconocimiento → dificultad falsa.

### Métricas recomendadas
- **Principal:** `ASSOCIATIVE_INDEX` como **cued recall accuracy real** (dado cue, producir target).
- **Secundarias:** `CONJUNCTION_ERROR_RATE` (con pares que comparten elementos para que sea informativa), asimetría forward/backward, RT de recuperación, retención diferida del par a 24h.
- **Engañan hoy:** `ASSOCIATIVE_INDEX` (reconocimiento, no asociación), `CONJUNCTION_ERROR_RATE` (≈0 artefactual), `RETRIEVAL_SPEED` (RT no se captura en el multi-select).

### Progresión recomendada
LOAD (4→10 pares) + SIMILARITY (`weakOnly`, ya en mapping 46) + DELAY real + bidireccionalidad. Introducir fan-effect (pares solapados) como eje de interferencia cuando crezca el pool. Zona óptima ~60-75% cued recall. Evitar techo eliminando reconocimiento; evitar fondo con pares fuertes y unidireccionales.

### Rediseño propuesto
Prueba de **cued recall**: mostrar el cue, el usuario produce el target (teclado o, en V1, selección de **una** opción por cue entre target + conjunción-distractores de otros pares estudiados — esto hace `CONJUNCTION_ERROR` medible sin teclado). Presentar cada probe por separado con su cue, capturando RT. Aplicar demora real y cablear retención a 24h. Crecer el pool con pares solapados para fan-effect. Métricas: cued recall + conjunción real + asimetría direccional. Feedback: "dado X recordaste Y" y, al día siguiente, retención del par. Variante directa: vocabulario L1-L2 (puente `VOCABULARY` ya definido). Test: respondedores sintéticos con tasa de binding conocida → el índice debe recuperarla; inyectar confusiones → conjunción debe subir. Superior porque restaura la recuperación dirigida por cue y la confusión inter-par, que son la definición operativa de memoria asociativa.

### Puntuación 0-10 (los 15 ejes)
1) **3** | 2) **2** | 3) **5** | 4) **4** | 5) **2** | 6) **4** | 7) **2** | 8) **1** | 9) **3** | 10) **8** | 11) **2** | 12) **5** | 13) **5** (mejor puente funcional) | 14) **3** | 15) **9**
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente — presentar el cue y exigir el target (selección 1-de-N con conjunción-distractores como mínimo viable). Con eso es el ejercicio de mayor utilidad real del módulo. En su forma actual, no mide memoria asociativa.

---

## Veredicto del módulo "Memoria (memoria de trabajo y episódica)"

**Capacidades bien cubiertas (en el DOMINIO, no en runtime):**
- WM bajo carga (EX1), episódica/clustering (EX3), asociativa (EX4) — las tres subcapacidades nucleares.
- Infraestructura científica **excelente y bien testeada pero desconectada**: `ReliableChangeIndex` (Jacobson-Truax correcto), `RealImprovementCriterion` (gate untrained-probe + effect size + 2 semanas — honesto), `TransferProbePartition` (reserva disjunta verificada en `init`), `DeferredRecallScheduler` (ventana 20-48h), `PlateauDetector`/`InterventionPlanner`, `MemoryRecoveryPolicy` (anti-overtraining), `MemoryFitnessCalculator` (bandas de incertidumbre por n), métricas con `confident`/`n` por construcción, intrusiones sobre datos crudos (aprendizaje 2026-06-04 aplicado bien).

**Huecos / capacidades AUSENTES en el producto vivo:**
- **El runtime no ejecuta nada de la ciencia.** `MemorySessionStateHolder.runSession` (95-130) corre **un único trial**, presentación **sin temporización**, recall por **reconocimiento multi-select** (`studied + 6 distractores`), `delayedAccuracy = null` (línea 164). `MemorySessionAssembler` (multi-bloque, warmup/encoding/interference/delayed), `DeferredRecallScheduler`, `MemoryRecoveryPolicy`, `RealImprovementCriterion`, `ReliableChangeIndex`, `PlateauDetector`, `MemoryFitnessCalculator` y los **probes reservados** (`MemoryProbeBank`, `MemoryBaselineProtocols.INITIAL`) **no se invocan desde ninguna sesión real**. Son código muerto operacional: construido, testeado, no cableado.
- **No hay recall por producción.** Las tres tareas colapsan a reconocimiento, lo que (a) destruye ARC (orden de recuperación falso), (b) hace `CONJUNCTION_ERROR`/`ORDER_ERROR` inalcanzables, (c) satura la precisión.
- **No hay demora real ni recuerdo diferido a 24h ejecutable.** El home cuenta `dueDeferredCount` pero no existe pantalla que administre el test diferido.
- **No hay baseline ni probes en el flujo.** `MemoryBaselineProtocols.INITIAL` y los `MemoryProbe` reservados existen; el usuario nunca los corre → **no hay medida untrained → el sistema no puede distinguir mejora real de efecto práctica**, que es exactamente su principio rector declarado.
- **Metacognición (EX16) sin cablear:** `confidenceBias = 0.0` fijo (`MemorySessionStateHolder` 145; `MemoryAdaptiveInput` 119) → `ActivateMetacognition` nunca dispara; `CALIBRATION_INDEX`/`BRIER_SCORE`/`GAMMA` no se computan.

**Ejercicios redundantes o a fusionar:** ninguno redundante (cubren subcapacidades distintas). El `ProcessingTask` de EX1 puede **reutilizarse como tarea distractora** para inducir demora real en EX3/EX4 (DRY, sin fusionar ejercicios). EX14 Dual-Task (catálogo, no implementado) es conceptualmente EX1 con la concurrencia explícita → cuando se implemente, derívalo del runtime de EX1, no como código nuevo.

**Riesgos sistémicos:**
1. **Medición común contaminada por reconocimiento:** todas las métricas de precisión heredan la inflación del multi-select sobre banco finito. El `confident`/`n` honesto no salva que el **constructo medido es el equivocado** (reconocimiento ≠ recall/span/asociación).
2. **Banco de contenido demasiado pequeño:** ~142 palabras / 56 pares, autoconfeso muy por debajo del ideal (>2000). Con reconocimiento, el efecto práctica/familiaridad domina la señal en semanas. `excludeRecentWordIds` mitiga repetición intra-corto-plazo, pero el pool se agota como universo de reconocimiento.
3. **Etiqueta de demora sin demora:** la métrica salta IMMEDIATE↔DELAYED por `delayMs` del plan aunque no transcurra tiempo real (`MemoryMetricsEngine.putCommon` 41-44 + `scoreList`/`scorePairs` propagando `plan.delayMs`). Riesgo de reportar consolidación inexistente. [Riesgo científico]
4. **Adaptación gobernada por métricas falsas:** `DefaultMemoryAdaptiveEngine` es correcto, pero su `MemoryZone` se calcula sobre accuracy de reconocimiento saturada → empuja dificultad por las razones equivocadas.
5. **El anti-engaño es estructural pero inerte:** la reserva de probes y los criterios RCI/RealImprovement están bien diseñados y desconectados → el producto **afirma** honestidad (nota "la mejora real se confirma con pruebas no entrenadas") sin **ejecutar** el mecanismo que la confirmaría.

**Las 3 cosas que más elevarían el poder cognitivo de este módulo (en orden):**

1. **Sustituir reconocimiento por recall por producción y conectar la demora real.** Es el cambio de mayor impacto: restaura simultáneamente span posicional (EX1), clustering ARC verdadero (EX3) y cued recall con conjunción (EX4), y desbloquea `DELAYED_RECALL_ACCURACY` legítimo. Sin esto, ninguna métrica del módulo mide lo que dice medir. (Para EX1, además, **renderizar y puntuar el `ProcessingTask`** para que exista el dual-task.)

2. **Cablear el ciclo de evaluación honesto que ya está construido:** baseline con `MemoryBaselineProtocols.INITIAL`, **probes reservados** (`MemoryProbeBank`) en pre/post, **test diferido a 24h** vía `DeferredRecallScheduler`, y alimentar `RealImprovementCriterion`/`ReliableChangeIndex`/`PlateauDetector` con datos reales. Es trabajo de *integración*, no de diseño nuevo — el código existe y está testeado. Esto convierte la honestidad declarada en honestidad ejecutada y es lo único que permite afirmar "mejora real vs efecto práctica".

3. **Multiplicar el banco de contenido (objetivo >2000 palabras / cientos de pares, con corpus de frecuencia como LEXESP/EsPal) y activar el flujo multi-bloque del `MemorySessionAssembler`** (warmup → encoding+immediate → interferencia → delayed). Esto ataca el efecto práctica de raíz, da varianza suficiente para `ReliableChangeIndex`, y hace que la dificultad (LOAD/DELAY/INTERFERENCE) module esfuerzo cognitivo real en una sesión estructurada, no en un trial aislado.

**Nota transversal de prioridad:** el módulo está en una situación peculiar — **el dominio está entre el mejor del repositorio (riguroso, honesto, testeado) y el runtime entre los más débiles (un solo trial, reconocimiento, sin tiempo, sin demora, sin probes).** El riesgo no es ciencia mala; es ciencia construida y **no conectada**, presentada al usuario con una "nota honesta" que el sistema no cumple. La prioridad 15 es máxima (9/10) en los tres ejercicios precisamente porque el coste de arreglarlo es integración, no investigación.

---

