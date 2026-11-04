# Anexos — lentes adversariales y de diseno (material de origen)

## Anexo A. Red Team cientifico (origen de la Seccion 5)
# SECCIÓN 5 — Fallos estructurales del sistema

> Método: cada fallo verificado contra el código, no contra los KDoc ni los planes. Los hallazgos por ejercicio de las 8 auditorías **no** se repiten: aquí solo va lo que es **sistémico** (atraviesa módulos o compromete la capa común). Toda afirmación optimista se trata como falsa hasta que el código la sostiene.

---

## CRÍTICO

### C-1 · No existe control del efecto práctica EJECUTADO en ningún módulo. Los mecanismos están construidos, testeados y desconectados

**Evidencia (verificada por grep/lectura, no por doc):**

| Módulo | Mecanismo construido | Estado real |
|---|---|---|
| Enfoque | `ProbeReliabilityEstimator` | `FocusEvaluationService.kt:17` usa el default `ProbeReliabilityEstimator()` → `source = ReliabilitySource.EMPTY` → `estimate()` devuelve `emptyMap()` **siempre**. Único call-site no-EMPTY: `FocusEvaluationServiceTest.kt:43,59`. **RCI no puede disparar jamás en producción.** |
| Memoria | `MemoryProbeBank`, `MemoryBaselineProtocols`, `ReliableChangeIndex`, `RealImprovementCriterion` | **0 referencias** en `memory/presentation/` y `memory/di/`. Código muerto operacional. |
| Lenguaje | `BankType` con 9 valores (`LanguageTaxonomy.kt:90-100`) | Los 36 ítems de los 6 bancos usan el default `TRAINING`; **ningún ítem declara `bankType =`**. `PARALLEL_FORM`, `TRANSFER_LAB`, `EVALUATION`, `GOLD_SET`, `RED_TEAM` son entradas de enum vacías → `DefaultEvidenceEngine` solo puede emitir `E1_TRAINED_IMPROVEMENT`. **E2 estructuralmente inalcanzable.** |
| Emociones | 12 ítems `isEval=true` reservados | El holdout está "diferido a Ola-4"; no existe. |
| Creatividad | `historyForUser` persistido | Cero consumidores. |

**Por qué reduce la mejora cognitiva real:** la promesa de marca es *"progreso frente a tu propia línea base"*. Sin un solo ítem no entrenado administrado en producción, **es imposible por construcción separar aprendizaje del formato de mejora de capacidad**. Toda curva longitudinal del producto está confundida con el efecto práctica, y ningún módulo puede detectarlo. Esto no es una laguna de investigación: es *wiring* pendiente sobre código ya escrito y testeado. **[Riesgo científico]**

---

### C-2 · Bancos finitos y minúsculos son la norma: la "mejora" medida es recuerdo del banco

**Evidencia:**

| Módulo | Tamaño real del banco | Archivo |
|---|---|---|
| Lógica | **6 ítems fijos hardcodeados**, 5 estructuras | `LogicContentProvider.kt:29-36` |
| Lenguaje | **36 ítems** (6 × 6 ejercicios), orden de array fijo, sin barajar | `*/exercises/*Bank.kt`; `LanguageSessionStateHolder.kt:99,119` |
| Creatividad | **14 prompts** (8 + 6) | `CreativityPromptCatalog.kt:12-30` |
| Emociones | **28 ítems** de entrenamiento (EM3/EM10) | `EmotionContent.kt:50-95` (el propio comentario admite "target = 80") |
| Velocidad | clave de **4 pares** → 16 estados (E3); **4 reglas** (E7) | `SpeedPromptGenerators.kt:36-67, 73-89` |
| Memoria | ~113 palabras / 44 pares tras reserva | `SpanishMemoryContentData.kt:65-124` |
| Enfoque | E6: 2 colores × 2 formas × 2 reglas = **8 situaciones** | `E6CambioGenerator` |

**Por qué:** en 2-3 sesiones la precisión refleja recuperación de memoria del set, no la capacidad nombrada. Peor: los motores adaptativos consumen esa precisión inflada y **suben la dificultad por la razón equivocada**, sacando al usuario de la zona de desafío real. Combinado con C-1 (sin holdout), el sistema **no tiene forma de notar que esto está pasando**. **[Riesgo científico]**

---

### C-3 · Sesgo de posición explotable en el módulo entero de Lenguaje: 32 de 36 ítems tienen la correcta = `"b"`

**Evidencia (conteo directo sobre el código):**

```
SemanticSurgeryBank.kt    6/6 → "b"
CompressionLadderBank.kt  6/6 → "b"
ArgumentXRayBank.kt       6/6 → "b"
ScopeNegationBank.kt      6/6 → "b"
SourceTriageBank.kt       3/6 → "b"  (a=2, c=1)
EvidenceLockBank.kt       5/6 → "b"  (a=1)
                         -----
                         32/36 = 88.9%
```

**Por qué:** la métrica gobernante del módulo es `accuracyPercent` (`LanguageSessionStateHolder.kt:157-162`). **Un usuario que pulse siempre la segunda opción, sin leer nada, saca 88.9% en toda la dimensión de razonamiento verbal.** No es un "riesgo de pseudoprogreso": es un exploit abierto que además convierte la etiqueta `E1_TRAINED_IMPROVEMENT` en un resultado predeterminado. Es el fallo más barato de arreglar y el más vergonzoso de dejar. **[Mejora crítica]**

---

### C-4 · Disonancia sistemática entre lo que el runtime PRESENTA y lo que el scorer MIDE

No es un defecto de un ejercicio; es un patrón arquitectónico que se repite en 4 módulos.

**Evidencia:**

- **Enfoque E8 (dual-task):** `FocusDualTaskField.kt:41-56` — el "contador" es un `LaunchedEffect` que incrementa un `Int` cada 450 ms. No se captura, no entra en `RecordedTrial`, no se puntúa. El propio KDoc lo confiesa: *"the secondary load is not separately scored"*. `DUAL_TASK_COST` = precisión(single) − precisión(dual) **sobre una manipulación que no existe** → mide ruido alrededor de 0.
- **Enfoque E9 (MOT):** `FocusTrackingField.kt:82-93` — las trayectorias son **órbitas circulares independientes** (`cx/cy` aleatorios, `orbit` 0.05-0.12, sin cruces ni oclusión) y el dominio no conoce ninguna de ellas. `speed` solo mueve `turns` en el render (`:89`). El MOT degenera en **memoria de localización espacial**: basta recordar "arriba-izquierda y abajo-derecha estaban marcados". La identidad-bajo-cruce, que es *lo que define el paradigma*, no está modelada. `pickProbe` (`:96-97`) re-deriva post-hoc el punto sondeado para casar con `probedIsTarget` del dominio.
- **Enfoque E10 (RSVP):** `isTarget = true` siempre y un único tap para todo el stream → `ACCURACY` trivialmente ~100%. El *attentional blink* (p(T2|T1) por lag) es **inmedible** con un solo juicio, pese a que el generador calcula `t1Index`, `t2Index` y `lag`.
- **Memoria (los 3 ejercicios):** los generadores emiten `ProcessingTask`, tiempos de presentación serial y `delayMs`; el runtime presenta `Text` estáticos sin temporización y resuelve el recall con un multi-select de reconocimiento. Span, ARC y binding asociativo **miden reconocimiento**.
- **Creatividad EX02:** declara rotación de categorías; el scorer es compartido y agnóstico al ejercicio (`CreativitySessionCoordinator.kt:78`). El constructo declarado **no existe en el código**.

**Por qué:** validez de constructo cero. La cifra que sube no es la habilidad que el nombre promete. Cuando el usuario mejora en E9, mejora en memoria espacial; cuando "mejora" en E8, no mejora en nada — la métrica oscila. **Tres paradigmas prestigiosos (dual-task, MOT, attentional blink) están en el nombre y no en la medición.** **[Riesgo científico]**

---

### C-5 · Contenido cuya respuesta correcta no es derivable de lo presentado — o está filtrada en el enunciado

**Evidencia:**

- **Reversibilidad (Decisiones):** `ReversibilityEngine.kt:18` → `isReversible = random.nextDouble() >= irreversibility`; `:33-35` → `description = "Reversibility triage level N"`. **La respuesta correcta es una moneda y la carta no contiene ninguna decisión.** Además, `lockedDurationMs > 0` solo en irreversibles (`:25-30`) y la pantalla consulta `currentLockDurationMs()` antes de la elección → **fuga de respuesta explotable al ~100%**.
- **Lógica necesario/suficiente:** `LogicContentProvider.kt:153` — el prompt verbaliza la conclusión correcta antes de preguntar. El proceso entrenado es emparejamiento texto↔opción.

**Por qué:** un tercio del módulo de Decisiones **mide suerte** y la reporta como juicio en el eje de un radar. El repo ya documentó exactamente esta regla para E6 task-switching (`CLAUDE.md` 2026-06-18: *el payload debe transportar las features; nunca exponer la respuesta*) y aquí se viola en las dos direcciones posibles: respuesta no derivable, y respuesta regalada. **[Mejora crítica]**

---

### C-6 · La adaptación está muerta en 4 de 8 módulos: no existe zona óptima de desafío

**Evidencia:**

- **Lógica:** `LogicStateHolder.startSession()` llama `graph.contentProvider.sessionItems()` **sin pasar dificultad ni theta** (verificado en el código); el `DifficultyDecision` y el `thetaEstimate` por skill se calculan y se guardan como string de recomendación. ~150 líneas de maquinaria adaptativa sin efecto sobre el contenido.
- **Decisiones:** `core/adaptive/DifficultyAdapter` existe y **nunca se invoca** en el flujo; todo es `DEFAULT_DIFFICULTY` hardcodeada por state holder. Corolario: el eje "Reto enfrentado" del polígono es **constante sesión a sesión**.
- **Lenguaje:** `DefaultAdaptationEngine` no se llama desde `LanguageSessionStateHolder`; recorrido por índice de array.
- **Creatividad:** no hay motor adaptativo; `minIdeas`/`targetIdeas` son constantes (3/8 y 4/8) y el comentario "Difficulty bounds are per-exercise domain data" es falso.

**Por qué:** sin dificultad que siga a la habilidad, el usuario entrena en techo (adherencia, cero adaptación) o en suelo. Y donde SÍ hay adaptación (Velocidad, Enfoque, Emociones), el eje que se mueve es la **presión temporal**, no la carga cognitiva (ver A-1).

---

## ALTO

### A-1 · La dificultad se sube apretando el reloj, no la carga: velocidad disfrazada de habilidad

**Evidencia:** Velocidad — `MultiSignalStaircaseEngine` mueve **solo el deadline** sobre contenido trivial (flecha→lado; clave de 4 pares). Enfoque — el eje `PRESSURE` recorta `responseWindowMs` en E2 (1500→900), E3 (900→600), E5 y E6. Emociones EM10 — `PRESSURE_FACTOR=0.65` (mínimo 900 ms) aplicado a una tarea de **clasificación conceptual** de frases de 5-7 palabras.

**Por qué:** tolerar un deadline más corto en una tarea sobreaprendida no es más capacidad de procesamiento. En EM10 es peor: convierte una métrica de conocimiento (`CATEGORY_ACCURACY`) en una mezcla velocidad×conocimiento, penalizando al usuario que **sí** sabe separar pero lee despacio. En E2/E5 el `CONGRUENCY_EFFECT` absorbe parte del colapso por presión.

### A-2 · Ningún RT del producto está corregido por la latencia sensoriomotora basal

**Evidencia:** `SpeedSessionStateHolder.kt:312-317` calcula `simpleReactionMedianMs` a partir del primer bloque del usuario — y **no se resta en ningún sitio**. Enfoque, Emociones y Decisiones reportan RT crudo. `SpeedSessionStateHolder.kt:189` (`?: true`) hace además que la `pc` de E1 sea trivialmente 1.0.

**Por qué:** el RT crudo mezcla capacidad, motricidad y hardware (tasa de refresco, latencia táctil). Un teléfono nuevo produce "mejora cognitiva". La comparación entre usuarios es inválida y la longitudinal está contaminada. Es la corrección más barata del repo (el SRT ya se computa) y multiplicaría la validez de 3 módulos. **[Dato necesario]** calibración de latencia de pantalla/touch por dispositivo.

### A-3 · Ausencia de teoría de detección de señal donde hay hits y falsas alarmas

**Evidencia:** Enfoque E3 — `FALSE_ALARMS` es un **conteo absoluto** (`addErrorTallies`, l.168) con umbral fijo `falseAlarms >= 2.0`, no escalado por longitud de bloque. Emociones EM1/EM8 — `COMMISSION_RATE` cruda. Decisiones check-in — accuracy agregada que **funde omisión y comisión** en `!isCorrect`. Lógica — el banco de detección de contradicción tiene base-rate 100% "consistente" (ningún `correctAnswerId` apunta a "hay contradicción").

**Por qué:** sin d′ y criterio, **responder más lento baja las comisiones y el sistema lo lee como mejor inhibición**. Es la estrategia dominante para el usuario y el producto la premia. En Lógica, la heurística "responde siempre consistente" saca 100%.

### A-4 · Potencia estadística estructuralmente insuficiente, en contradicción con el propio `MinSampleGate` del repo

**Evidencia:** Emociones — `BLOCKS=2 × TRIALS_PER_BLOCK=10 = 20` (`EmotionSessionStateHolder.kt:410-411`) con `MIN_NOGO=6`, `MIN_POST_ERROR=3`, `MIN_CELL=5`; con `NO_GO_RATIO=0.28` salen ~5-6 no-go → la métrica gobernante de EM1 **nace no-confiable**. Enfoque — `MIN_N_CONGRUENCY=3`, `MIN_N_POST_ERROR=3`, **1** no-go/stop garantizado. Velocidad — 12 trials/bloque alimentando `cv`/`impIdx` con MAD (que exige ≥4 tras filtrar). Lógica — 6 ítems. Frente a esto, `core/scoring/MinSampleGate` exige ≥20 y marca `requiresConfidenceInterval` para DPRIME.

**Por qué:** el sistema entrega o bien `unavailable` (no mide) o bien un número sobre n=3 (ruido con barra de error gigante) — y los motores adaptativos **consumen ese ruido** para mover la dificultad, produciendo una oscilación que el usuario percibe como progreso/regreso. La honestidad del flag `confident` no compensa que la muestra sea inviable por diseño.

### A-5 · Redundancia de paradigmas: ~50 entradas de menú, ~12 paradigmas reales

**Evidencia (cruce entre módulos):**

| Paradigma | Instancias |
|---|---|
| Go/no-go | Enfoque E3, Enfoque E8 (primaria), Emociones EM1, EM8, (EM12 catálogo), Decisiones check-in |
| Conflicto E-R (flanker/Simon) | Enfoque E2, Enfoque E5, Velocidad E7, base de Enfoque E13 |
| Post-error | Enfoque E12, Emociones EM2, EM11 |
| TR de elección | Velocidad E6, Enfoque E2/E5/E13 |
| Reconocimiento multi-select | Memoria EX1, EX3, EX4 |

**Por qué:** la cobertura aparente del catálogo es ~4× la real. El usuario reparte su tiempo re-entrenando el mismo proceso estrecho mientras **faltan por completo**: memoria de trabajo bien medida (n-back / span real), reappraisal, calibración metacognitiva ejecutada, razonamiento analógico, y la red de alerta/orientación de Posner. La ausencia de WM es la más cara: es la capacidad con más evidencia de transferencia documentada, y el módulo que dice cubrirla la mide por reconocimiento.

### A-6 · Índices compuestos inventados presentados como capacidad, algunos con puntos regalados o gameables

**Evidencia:**

- `SUSTAINED_FOCUS_INDEX = 100·(1−rtCv)·(1−lapseRate)` (`DefaultMetricsEngine`, l.418) — fórmula ad-hoc sin validación, escala 0-100 que se leerá como "tu foco".
- `CRI = 50%·accuracy + 30%·estabilidad + 20%·fatiga **autorreportada**` (`ScoringEquations.calculateCRI`, l.99-112). Tocar "Descansado" (0.9) sube el índice ~18 puntos **sin ningún cambio real**.
- `ICD`: `biasDetected=false` y `alternativesScannedRatio=1.0` se pasan **fijos** en todos los rounds → **35 de 100 puntos son regalo constante**. El ICD no puede bajar de ~35 aunque el usuario falle todo lo medible.
- "Integridad" del polígono = `(1−AGRS)·100`, y AGRS depende solo de `RT<400 ms` en Bayes/Reversibilidad → **clavado a ~100** siempre. Eje decorativo.
- `GRANULARITY_INDEX` = categorías-distintas-acertadas / 4 (`DefaultEmotionMetricsEngine.kt:117-119`) — es cobertura, no granularidad.
- `averageIcr` funde 6 dimensiones heterogéneas: quien acierta y no justifica y quien justifica y adivina obtienen el mismo número.

**Por qué:** aritmética no validada con autoridad visual de radar. Un eje clavado y otro gameable por autoinforme conviven con ejes que vienen de azar (Reversibilidad): **el polígono es el vehículo perfecto de pseudoprogreso**, y el coach dispara perfiles psicológicos ("sobreconfiado", "impulsivo") sobre esas señales.

### A-7 · Bug real de precedencia en la penalización de Lógica — reincidencia documentada

**Evidencia (`ReasoningScorer.kt:47-51`, verbatim en el código actual):**

```kotlin
val penaltyPoints = MAX_SCORE * (
    IMPULSIVITY_WEIGHT * if (impulsive) 1.0 else 0.0 +
        CONCEPTUAL_ERROR_WEIGHT * if (conceptualError) 1.0 else 0.0
    )
```

Kotlin lo parsea como `IMPULSIVITY_WEIGHT * (if (impulsive) 1.0 else (0.0 + CONCEPTUAL_ERROR_WEIGHT * (if (conceptualError) 1.0 else 0.0)))`. Con `impulsive=true` **el término de error conceptual desaparece**; con `impulsive=false` se pondera por `IMPULSIVITY_WEIGHT` (0.10) en vez de `CONCEPTUAL_ERROR_WEIGHT` (0.15).

**Por qué:** la penalización por elegir un distractor diagnóstico —la razón entera de tiparlos (`OVERINFERENCE`, `CONFIRMATION_BIAS`, `FALSE_CONTRADICTION`)— casi nunca se aplica con su peso. Y `CLAUDE.md` (2026-06-15) documenta exactamente esta clase de bug **en este mismo archivo**: la regla se escribió y el defecto reapareció 10 líneas más abajo. **[Mejora crítica]**

### A-8 · Agujero de sobre-selección: marcar todo puntúa perfecto

**Evidencia (`ClosedScoringEngine.kt:77-78`):**

```kotlin
val captured = if (required.isEmpty()) 1.0 else (required intersect selected).size.toDouble() / required.size
val evidenceComplete = selected.containsAll(required)
```

Solo recall, cero precisión. Seleccionar **todos** los spans ⇒ `captured = 1.0` y `evidenceComplete = true`. El mismo patrón en Memoria: el recall es multi-select y el hit es pertenencia al conjunto seleccionado (solo EX4 tiene gate de discriminación por tasa de falsas alarmas).

**Por qué:** Evidence Lock existe precisamente para castigar la sobre-inferencia (`INF_OVER`); su scorer premia la sobre-marcación. El mejor ejercicio del módulo verbal es superable al 100% sin leer: opción no-absurda + marcar todo.

### A-9 · El módulo "Creatividad / Inteligencia" no mide creatividad ni contiene inteligencia

**Evidencia:** `CreativityRubricScorer.kt:21-37` computa **dos** señales: `fluency` = `ideas.distinct()` (igualdad exacta de strings solo `trim`-eados → "arma"/"Arma" y "pisapapeles"/"pisa papeles" cuentan por separado) y `elaboration` = palabras/idea. **Originalidad y flexibilidad no se miden en ninguna parte.** No hay razonamiento fluido, abstracción ni convergencia. Las 3 etiquetas de feedback (`CreativityPracticeLabel.kt:13-17`) son todas positivas: no existe etiqueta de estancamiento. Los `Double` se pintan crudos (`CreativityRootScreen.kt:211-212`).

**Por qué:** la fluidez se infla con variantes ortográficas y la "elaboración" premia verbosidad; el feedback nunca puede comunicar que no mejoras. El nombre "Inteligencia" **no tiene ningún sustrato en el código**.

---

## MEDIO

### M-1 · La capa de honestidad se ha convertido en un tapón permanente que oculta que la medición está rota

**Evidencia:** `MindBoostProfileAggregator.kt:137,147` — `compositeIndex = null` estructuralmente; `ADR_SCI_006_APPROVED = false`, `PROVISIONAL_GLOBAL_PROXY_APPROVED = false`; el índice provisional solo lleva **cobertura, sin número**. RCI nunca dispara (C-1). E2 inalcanzable (C-1). `EmotionSessionStateHolder.kt:368,370` fija `trend = UNKNOWN` y `transferConfidence = NOT_DEMONSTRATED` **siempre**.

**Por qué:** el diseño de reporte es genuinamente honesto y hay que preservarlo. Pero el efecto neto es que, tras N semanas de entrenamiento, **el sistema no puede decirle nada al usuario**. Y —esto es lo peligroso— desde fuera **un scorer roto y uno honestamente incalculable son indistinguibles**: el `CALIBRATING` universal camufla que debajo hay métricas que miden reconocimiento, azar o ruido. El repo ha invertido mucho más rigor en *no mentir sobre lo que sabe* que en *saber algo*. **[Riesgo científico]**

### M-2 · Fatiga sin retorno de medición

**Evidencia:** E10 lanza un stream a 80 ms/ítem y captura un tap; E9 corre trials de 1800 ms cuyo constructo no se mide; E8 hace parpadear un contador que nadie puntúa; EM2/EM11 exigen comparación de barras cerca del umbral (trabajo de agudeza visual, no de regulación). El `FatigueGate` de Lenguaje existe y **nunca se invoca** (`LanguageSessionStateHolder` pasa `ScoringTiming()` con fatiga `FRESH` fija).

**Por qué:** coste ocular y atencional con señal cero. Además degrada los bloques posteriores de la misma sesión sin que ningún gate lo detecte, metiendo ruido no modelado en las métricas que sí funcionan.

### M-3 · Confounds de estado no controlados en ningún módulo

**Evidencia:** no se registra hora del día, sueño ni contexto en ningún módulo. El único autoinforme de estado (fatiga en Decisiones) **entra directamente al score** con peso 20% en vez de usarse como covariable.

**Por qué:** el RT varía ±30-50 ms entre días por sueño/cafeína/hora. Sin covariables, todo delta sesión-a-sesión es ruido presentado como progreso — y el único dato de estado disponible se usa de la forma exacta que lo hace gameable. **[Dato necesario]** timestamp de sesión + hora local como covariable, no como término del score.

### M-4 · Cero datos normativos y cero fiabilidad test-retest

**Evidencia:** la única instancia de `ReliabilitySource` en producción es `EMPTY`. No hay normas en ningún módulo.

**Por qué:** sin fiabilidad no hay RCI, sin RCI no hay "cambio fiable"; sin normas no hay percentiles. La única comparación posible es contra la propia línea base — que a su vez no está controlada por práctica (C-1). El circuito de evidencia está cerrado sobre sí mismo y vacío.

### M-5 · Duplicidad de fuentes de verdad en el runtime

**Evidencia:** `LanguageSessionReducer` (con branching de reparo real) existe y **no se usa**; el holder reimplementa una versión sin repair-retry. `CounterexampleValidator` (Lógica) está definido y testeado pero **no se cablea al scoring** — la puntuación decide por el `id` de la opción, no por la semántica del contraejemplo. `calculatePosterior(baseRate, List<SensorEvidence>)` (Bayes multi-fuente) está implementado y muerto. `calculateCAG`/`calculateBrierScore`/`calculateCalibrationError` implementados y desconectados; la certeza se recoge en Bayes y **se tira**.

**Por qué:** el repo tiene ya escrito el material para medir calibración metacognitiva e integración de evidencia —los dos constructos de mayor valor funcional del producto— y no lo ejecuta. Cada motor huérfano es además deuda que hace parecer que la capacidad existe.

---

## Los 5 ataques que más duelen

### 1. «Pulso siempre la segunda opción» → 88.9% en todo el módulo de Lenguaje
32 de 36 ítems tienen la correcta = `"b"`. Sin leer una palabra, el usuario maximiza `accuracyPercent`, la métrica gobernante, y el sistema le emite `E1_TRAINED_IMPROVEMENT` — una etiqueta que, además, es el único veredicto que el módulo puede producir (todos los bancos son `TRAINING`). **El razonamiento verbal completo se supera con una heurística motora.**

### 2. «Toco "Irreversible" primero y miro si aparece el lock» → ~100% sobre un ground truth que es una moneda
`ReversibilityEngine.kt:18`: la etiqueta correcta es `random.nextDouble()`. `:25-30`: solo las irreversibles tienen `lockedDurationMs > 0`, y la pantalla lo consulta antes de la elección. Un tercio del módulo de Decisiones **mide suerte**, es explotable hasta el 100%, y su accuracy alimenta un eje del polígono y los perfiles del coach. El KDoc afirma que clasificar "sigue siendo una decisión honesta": es falso, verificable en 4 líneas.

### 3. «Respondo más despacio» → el sistema lo lee como mejor control inhibitorio
Sin d′ ni criterio en E3, EM1, EM8 y el check-in, bajar la tasa de comisión ralentizándose es la estrategia dominante. El producto no puede distinguir "inhibe mejor" de "adoptó un criterio conservador", y en E3 el umbral es un **conteo absoluto** (`falseAlarms >= 2.0`) que ni siquiera escala con la longitud del bloque. **La app entrena cautela y la reporta como control.**

### 4. «Me aprendo el banco» → indistinguible de aprender, porque no hay holdout ejecutado
6 ítems en Lógica, 36 en orden fijo en Lenguaje, 28 frases en Emociones, clave de 4 pares en Velocidad, 14 prompts en Creatividad. Y los cinco mecanismos que lo detectarían —`ProbeReliabilityEstimator` (`ReliabilitySource.EMPTY`), `MemoryProbeBank`, `PARALLEL_FORM`/`TRANSFER_LAB` vacíos, los 12 ítems `isEval` diferidos, `historyForUser` sin consumidor— **están construidos, testeados y desconectados**. La app no puede notar que el usuario memorizó; solo puede felicitarle.

### 5. «Los tres paradigmas más prestigiosos del catálogo no están implementados, y el radar los presenta con autoridad científica»
Dual-task (`FocusDualTaskField.kt:41-56`: contador decorativo, KDoc lo admite), MOT (`FocusTrackingField.kt:82-97`: órbitas independientes sin cruces → memoria espacial; `speed` solo afecta al render) y attentional blink (`isTarget=true` + un tap → precisión ~100%). Súmese `Paradigm.STROOP` etiquetando un Simon espacial y "Cifrado/digit-symbol" etiquetando un matching binario con clave de 4. **En un producto cuyo primer límite no negociable es "no usar neurociencia vaga ni claims no respaldados", el mayor pasivo de credibilidad no está en el copy —que es cuidadoso— sino en los nombres de los paradigmas: el sistema es honesto sobre lo que sabe y deshonesto sobre lo que mide.**

---

**Conclusión del red team.** La ingeniería de medición temporal, la seguridad y la capa de reporte están por encima de la media del sector. La capa que decide **si el producto entrena inteligencia** —contenido generativo, control del efecto práctica, validez de constructo y adaptación cableada— está sistemáticamente sin conectar. Hoy MindBoost no puede demostrar que mejora nada, y en 5 vectores independientes puede ser superado sin ejercer la capacidad que dice entrenar. Los arreglos de mayor retorno no son investigación: son **wiring de código ya escrito** (C-1, C-6, M-5), **randomización de posición** (C-3, coste horas) y **derivar las respuestas correctas de features del estímulo** (C-5).

---

## Anexo B. Transferencia y cobertura cognitiva (origen de la Seccion 6)
# SECCIÓN 6 — Mejoras estructurales del sistema

## 6.0 Diagnóstico de sistema (antes de proponer nada)

**Qué es MindBoost hoy, arquitectónicamente:** ocho verticales aisladas + un selector. No es un sistema de entrenamiento; es un menú de baterías.

Evidencia en el código, no en la impresión:

- **Acoplamiento cero entre módulos.** El único import cruzado en `features/mindboost/**` es `focus.data.local.SecureKeyValueStore` (+ `MonotonicClock`), es decir, **almacenamiento y reloj**. Ningún módulo lee el estado, el rendimiento ni el contenido de otro. Verificado por barrido de imports sobre los 8 paquetes.
- **El hub es un `sealed interface` de 9 destinos y un `when`** (`presentation/ui/MindBoostHubScreen.kt:96-256`, `:461-470`). Elegir un track lo lanza **sin payload**: `onEnfoque = { track = MindBoostTrack.Enfoque }` (`:100`). No se transporta dificultad, objetivo, historial ni plan.
- **El "plan de hoy" es una sugerencia de navegación, no un plan.** `TransversalDailyPlanner.plan()` (`profile/domain/orchestration/TransversalDailyPlanner.kt:24-37`) ordena 5 módulos por *estado del perfil* (CALIBRATING → EMERGING → ESTABLISHED) y devuelve 3 nombres. `TodayPlanStateHolder.DEFAULT_PLANNABLE` excluye Velocidad, Creatividad y Lenguaje. El resultado se pinta como tarjetas (`MindBoostHubScreen.kt:377`) y **no altera nada del ejercicio que se ejecuta después**. Es un ordenador de iconos.
- **La periodización existe, pero solo dentro de Enfoque y desactivada.** `TrainingCycle` (semanas con `isDeload`, `focus/domain/session/TrainingCycle.kt:27-75`) y `DefaultDailyPlanGenerator` (`focus/domain/session/DailyFocusPlan.kt:56-134`) son un microciclo real con deload y gate de recuperación — el mejor artefacto de progresión del repositorio. Su personalización está **contenida a NEUTRAL** porque `archetypePersonalizationEnabled` lee `focusScoreSurfacesEnabled`, que es `false` (`core/config/FeatureFlags.kt:123`). Y ningún otro módulo tiene equivalente.
- **El único mecanismo de espaciado real de todo el producto es el recuerdo diferido de Memoria** (`memory/domain/deferred/DeferredRecall.kt:73`, ruta viva en `MemoryRootScreen.kt:66` → `DeferredRecallSessionStateHolder.kt:114`, ventana 20-48 h). Uno de ocho módulos. Y en su forma actual es **reconocimiento**, no recuperación.
- **La transferencia está declarada como metadatos, no como runtime.** `memory/domain/transfer/TransferProtocol.kt` define 10 "micro-protocolos de transferencia funcional" con `bridge` y `measurement` en prosa; **ninguno tiene ejecución**. Es un documento de diseño compilado como Kotlin.
- **El índice global es estructuralmente `null`** (`MindBoostProfileAggregator.kt:46-64`, `:137`, `:147`), con dos constantes de aprobación científica en `false`. Esto es **lo más honesto del sistema** y hay que preservarlo intacto.
- **Taxonomía 1:1 módulo→dimensión** (`profile/domain/model/CognitiveTaxonomy.kt:34-44`): cada puerta del hub alimenta exactamente una dimensión. La arquitectura **presupone** que la capacidad se descompone en 9 casillas independientes y que entrenar una casilla la mueve. Eso es un modelo, no un hecho, y está cableado en el tipo.

### Respuesta directa a la pregunta central

**MindBoost hoy produce mejora-en-la-tarea, con techo de banco, y no tiene la instrumentación para saberlo.**

Justificación por mecanismo, no por opinión:

| Mecanismo con evidencia | ¿Existe a nivel de sistema? | Estado real |
|---|---|---|
| Recuperación activa (testing effect) | **No** | Los 8 módulos usan reconocimiento/opción múltiple. Memoria colapsa span/free-recall/pares a multi-select (audit memoria). Lenguaje es 100 % opción múltiple con la correcta siempre en `b` en 4 de 6 ejercicios. |
| Espaciado | **Casi no** | Solo `DeferredRecallScheduler` en Memoria. Cero repaso espaciado en Enfoque, Lógica, Lenguaje, Decisiones, Emociones, Creatividad. |
| Intercalado | **No** | Un módulo por sesión, un ejercicio por sesión. Ni intra-módulo (Memoria corre **un solo trial**), ni inter-módulo (el hub es exclusivo). |
| Dificultad deseable | **Parcial y falsa** | Enfoque y Memoria tienen escaleras reales; Lógica calcula theta y **no lo consume** (`LogicStateHolder.startSession()`); Decisiones tiene `DifficultyAdapter` muerto; Creatividad no tiene adaptación. Donde existe, opera sobre métricas contaminadas (reconocimiento saturado). |
| Variabilidad de estímulos | **No** | Bancos: Lógica 6 ítems, Lenguaje 6, Creatividad 14 prompts, Decisiones 1 plantilla, Emociones 28 ítems, Memoria ~142 palabras/56 pares, Velocidad 4 pares/4 reglas. Todos memorizables en días. |
| Contextos múltiples | **No** | Un solo contexto por ejercicio: pantalla, modalidad, formato de respuesta y banco fijos. |
| Transfer probes | **Diseñados, casi no ejecutados** | Enfoque tiene probes para 3 de 13 ejercicios; Memoria reserva el 20 % del banco (`TransferProbePartition`) y **no lo usa en el flujo**; Lenguaje tiene `NearTransferVerifier` pero todos los bancos son `TRAINING`, luego E2 es **estructuralmente inalcanzable**. |

**[Riesgo científico — sistémico]** La arquitectura apuesta íntegramente por el paradigma con **peor** evidencia de transferencia (práctica repetida de una tarea para elevar una capacidad latente; Owen 2010, Melby-Lervåg & Hulme, Simons et al. 2016) y **no explota** los tres mecanismos con mejor evidencia (espaciado, recuperación activa, instrucción explícita de estrategia). Se ha invertido un rigor extraordinario en *no mentir sobre la medición* y prácticamente nada en *diseñar un currículo que pudiera producir algo que medir*.

---

## 6.1 El giro estructural (la decisión de arquitectura número 1)

Antes de las mejoras: hay una decisión que las precede y sin la cual todas son cosmética.

> **MindBoost debe dejar de ser una plataforma de "entrenamiento de capacidad" y convertirse en un motor de estrategia + calibración + retención, con la práctica de tarea como sustrato, no como producto.**

**Razón funcional:** el volumen de práctica sobre tareas de laboratorio produce mejora en la tarea y transferencia cercana estrecha; lo que sí transfiere con evidencia razonable es (a) **estrategia enseñada explícitamente y practicada en contextos variados**, (b) **calibración metacognitiva** (saber cuándo sabes), y (c) **retención de material real** vía espaciado + recuperación. Los tres son mecánicas de sistema, no de ejercicio — y ninguno de los tres tiene hoy dueño arquitectónico.

**Consecuencia de diseño:** las mejoras S1-S8 siguientes viven **por encima** de los módulos (en `core/` o en un `curriculum/` nuevo), no dentro de ellos. Si se implementan dentro de cada módulo, se replicarán ocho veces y divergirán ocho veces, exactamente como ya ocurrió con la adaptación (Enfoque, Memoria, Lógica y Decisiones tienen cuatro motores adaptativos distintos, dos de ellos muertos).

**Claim permitido tras el giro:** "entrena estrategias, calibración y retención, y mide tu progreso frente a tu propia línea base con pruebas que no has entrenado".
**Claim que sigue prohibido:** cualquier afirmación de mejora de capacidad general o de rendimiento fuera de lo medido.

---

## 6.2 Las ocho mejoras estructurales

Formato por mejora: **capacidad objetivo · mecanismo · nivel de evidencia · mecánica · medición · adaptación · métrica que verifica que funciona · riesgo**.

---

### S1 — Capa de sesión transversal (`MindBoostSession`): el contenedor que hoy no existe

**Capacidad objetivo:** ninguna en particular — es el habilitador del intercalado, la variabilidad y la mezcla de contextos.
**Mecanismo psicológico:** interferencia contextual (Shea & Morgan). Alternar tareas dentro de una sesión degrada el rendimiento inmediato y mejora la retención y la flexibilidad de aplicación, porque fuerza reconstruir el set de tarea en cada bloque en vez de reutilizar el del bloque anterior.
**Nivel de evidencia:** fuerte para aprendizaje motor y de categorías; **moderado** para tareas cognitivas de laboratorio; **no demostrado** para "capacidad". Se adopta por su efecto sobre *retención y flexibilidad de lo entrenado*, no como promesa de transferencia lejana.

**Mecánica.** Un tipo nuevo por encima del hub:

```
core/curriculum/
  MindBoostSession(
    sessionId, blocks: List<SessionBlock>, mixPolicy: MixPolicy,
    stateGate: ReadinessGate, probeSlot: ProbeSlot?
  )
  SessionBlock(moduleId, exerciseId, targetErrorRate, minTrials, contextTag)
  MixPolicy = BLOCKED | INTERLEAVED_WITHIN_MODULE | INTERLEAVED_CROSS_MODULE
```

El hub deja de ser un `when` sobre 9 destinos exclusivos y pasa a ser **el runner de una sesión compuesta**: `MindBoostHubScreen.kt:96` navega a bloques consecutivos y devuelve al contenedor entre bloques, en vez de entregar la pantalla completa a un módulo hasta que el usuario sale.

**Coste real y honesto:** cada `*RootScreen` (Focus, Memory, Speed, Logic, Language, Decisions, Emotions, Creativity) posee hoy su propia navegación interna y su propio ciclo de vida de grabación (`recorder.onStart/onAbandon`). El contenedor debe **componer bloques, no secuestrar módulos**: cada módulo expone `runBlock(spec): BlockOutcome` y conserva su runtime. Esto es refactor de superficie, no reescritura.

**Medición:** `MindBoostSession` emite un `SessionComposition` (módulos, orden, tags de contexto, política de mezcla) por sesión.
**Adaptación:** `MixPolicy` se elige por fase del microciclo (§6.3), no por preferencia del usuario.

**Métrica que verifica que funciona:**
- **Efecto de interferencia contextual medido en el propio producto:** asignar aleatoriamente usuarios a BLOCKED vs INTERLEAVED_CROSS_MODULE durante 3 semanas. Se espera **peor rendimiento intra-sesión en interleaved** (esa es la señal de que funciona) y **mejor retención a 7 días y mejor rendimiento en probes no entrenados**. Si interleaved gana también intra-sesión, la manipulación no está funcionando (bloques demasiado largos ⇒ no hay switching real).
- Contraste crítico: `retención_7d(interleaved) − retención_7d(blocked)`, con IC.

**[Riesgo científico]** Intercalar tareas *incompatibles en su timebase* (Enfoque/Velocidad son RT-críticos y exigen `CHOREOGRAPHER_FRAME + NANOS_NATIVE`) puede introducir efectos de arrastre (fatiga, warm-up) que contaminen justo las métricas más frágiles del producto. Ver la matriz de §6.4: no todo se mezcla con todo.

---

### S2 — Motor de espaciado transversal (`core/curriculum/SpacedRetrievalScheduler`)

**Capacidad objetivo:** retención y consolidación de lo entrenado (todas las dimensiones).
**Mecanismo:** efecto de espaciado + curva de olvido; el intervalo expandido fuerza recuperación en el punto de dificultad deseable.
**Nivel de evidencia:** **fuerte** (uno de los efectos más replicados del aprendizaje humano; Cepeda et al. 2006 para la función intervalo-retención). Fuerte para *material*; **no** demuestra por sí mismo cambio de capacidad.

**Mecánica.** Promover `DeferredRecallScheduler` (hoy privado de Memoria, `memory/domain/deferred/DeferredRecall.kt:73`) a `core/`, generalizándolo de "ítem de memoria" a `ReviewItem`:

```
ReviewItem(id, moduleId, constructTag, lastSeenAtMs, lastOutcome, intervalIndex)
intervals = [24h, 3d, 7d, 21d]   // expandido, no fijo
due(now) → List<ReviewItem> ordenado por urgencia y por diversidad de módulo
```

Productores de `ReviewItem` por módulo, todos triviales de emitir porque los datos ya existen:
- **Memoria:** palabras/pares estudiados (ya lo hace).
- **Lenguaje:** ítem fallado + su `ErrorType` (la taxonomía tiene 14 tipos con *repair plans* completos — hoy sin repaso).
- **Lógica:** estructura de razonamiento fallada (`LogicModels.kt` declara 28 subhabilidades).
- **Decisiones:** el escenario resuelto, para re-preguntar la estimación pasado un tiempo (test de estabilidad de la actualización bayesiana).
- **Emociones:** ítem de separación (EM3) fallado.

**Medición:** precisión en el repaso por intervalo (24 h / 3 d / 7 d / 21 d) y por módulo ⇒ **curva de retención observada por usuario**.
**Adaptación:** el intervalo siguiente sube si acierta con confianza alta, se contrae si falla (retroalimentación clásica de sistema de repaso). Nunca sube por rendimiento en el mismo día (evita el atajo del sobre-entrenamiento masivo).

**Métrica que verifica que funciona:**
- **Pendiente de olvido por constructo:** ajustar retención vs log(intervalo). El sistema funciona si la pendiente **se aplana** a lo largo de las semanas *para los ítems programados* y no para un grupo de control de ítems no programados (control interno intra-sujeto: la mitad de los ítems fallados entra en la cola, la otra mitad no). Este contraste es barato, ético y decisivo.
- **Dato necesario:** hoy nadie persiste "ítem visto en la sesión N" fuera de Memoria; hay que emitir `ReviewItem` desde cada módulo. **[Dato necesario]**

**[Mejora crítica]** Esta es la mejora con mejor relación evidencia/coste de toda la sección. El mecanismo está probado, el scheduler ya existe y está testeado, y falta exclusivamente la generalización del tipo y siete productores de eventos.

---

### S3 — Invariante de plataforma: recuperación activa por producción, no reconocimiento

**Capacidad objetivo:** todas. Es una restricción de plataforma sobre el *formato de respuesta*.
**Mecanismo:** el testing effect exige **recuperación**, no reexposición. Reconocer un ítem entre distractores no ejerce el proceso que produce la consolidación, y además satura por familiaridad con el banco.
**Nivel de evidencia:** **fuerte** (Roediger & Karpicke; ventaja de recall sobre reconocimiento en retención diferida).

**Mecánica.** Un contrato en `core/` con gate de build, siguiendo el patrón ya probado en el repositorio (`mindboostCopySafetyScanner`, `evidenceBoundaryScanner`):

```
enum class ResponseMode { PRODUCTION, CONSTRUCTED, FORCED_CHOICE, DETECTION }
```

Regla: un ejercicio **que actualiza el perfil** y cuyo constructo declarado sea de memoria, razonamiento o lenguaje **no puede** usar `FORCED_CHOICE` como único modo. `DETECTION` sigue siendo legítimo para paradigmas de RT (Enfoque/Velocidad), donde la respuesta binaria *es* el paradigma. El gate se implementa como test en `desktopTest` (que ya está en `check`), no como scanner de texto: es validación de datos estructurados, exactamente como `BankValidationGateTest`.

Impacto por módulo (todos ya identificados en las auditorías):
- Memoria: recall por producción restaura span posicional, ARC real y cued recall con conjunción **de una vez**.
- Lenguaje: mata el sesgo de posición (correcta siempre `b`) por construcción.
- Lógica: elimina el ~50 % de acierto por azar que contamina el 35 % del peso del ICR.
- Creatividad: ya es producción (es lo único que hace bien); el problema ahí es la puntuación.

**Medición:** `accuracy` bajará. Eso es la señal esperada, no una regresión.
**Adaptación:** las escaleras deben re-anclarse a la nueva tasa de error objetivo (§6.3), porque los umbrales actuales están calibrados sobre precisión de reconocimiento saturada.

**Métrica que verifica que funciona:**
- **Caída inmediata de accuracy** (≥15 puntos en Memoria, esperada) **con aumento de retención a 24 h/7 d** en los mismos ítems. Si la accuracy cae y la retención no sube, la producción se implementó como puro aumento de dificultad sin beneficio de codificación ⇒ revisar el feedback correctivo (la recuperación fallida solo ayuda si va seguida de la respuesta correcta).
- **Reducción de la correlación entre "sesiones acumuladas" y "accuracy"** dentro del mismo nivel de dificultad — es el indicador directo de menos pseudoprogreso por familiaridad.

---

### S4 — Contenido como servicio compartido: `ItemBank` + holdout obligatorio + presupuesto de exposición

**Capacidad objetivo:** todas. Es el suelo bajo el que ninguna medición longitudinal es interpretable.
**Mecanismo:** con banco finito, la curva de mejora es una curva de familiarización. La transferencia solo es medible contra ítems **nunca vistos** del mismo constructo.
**Nivel de evidencia:** no es una hipótesis psicológica; es una precondición de validez de medida.

**Mecánica.** Ya existe la mitad: `core/bank/BankIntegrityAuthority` deriva la integridad del banco desde `BankValidator` y es fail-closed con registro opt-in vacío (aprendizaje 2026-07-05). Falta:

1. **Registrar los bancos reales** de los 8 módulos en `ProfileUpdatingBanks` — hoy vacío, luego **ninguna** de las fuentes tiene contenido validado y todas deberían estar en `SMALL_UNVALIDATED`. Ese es el estado honesto y ya se está aplicando en Memoria; hay que extenderlo o los módulos que sí publican perfil lo hacen sobre bancos no validados.
2. **Holdout disjunto obligatorio** por banco (el patrón ya existe: `TransferProbePartition` de Memoria reserva 20 % con verificación en `init`). Generalizar a `core/bank`.
3. **Ledger de exposición transversal** (`ExposureLedger`): cuántas veces ha visto este usuario este ítem. Lenguaje tiene `ExposureRepository` sin cablear; Memoria tiene `excludeRecentIds`; los demás no tienen nada.
4. **Generadores paramétricos donde el banco es inviable de escribir a mano:** Lógica (estructuras con plantillas ⇒ banco efectivamente infinito), Decisiones (escenarios de reversibilidad con features derivadas), Emociones (banco composicional de separación), Velocidad (clave digit-symbol rotativa).

**Medición:** `exposureCount` por ítem y por usuario; tamaño efectivo del banco; ratio holdout/entrenado servido.
**Adaptación:** un ítem con `exposureCount ≥ k` deja de ser elegible para medición del perfil (puede seguir siendo elegible para práctica, marcado como tal).

**Métrica que verifica que funciona:**
- **Brecha entrenado − holdout** por constructo. Si el usuario mejora 20 puntos en ítems entrenados y 2 en holdout, el sistema aprendió la brecha correctamente y debe reportar "práctica", no "mejora". La existencia misma de una brecha medible es lo que hace honesto el resto del sistema.
- **Correlación exposición↔accuracy** debe ser ≈0 en los ítems que informan el perfil.

---

### S5 — Currículo y periodización global (promover `TrainingCycle` de Enfoque a sistema)

**Capacidad objetivo:** gestión de carga, adherencia y consolidación.
**Mecanismo:** la adaptación cognitiva requiere alternancia carga/recuperación; el sobreentrenamiento sin deload produce meseta y abandono. Además, la dosis distribuida bate la masiva (mismo mecanismo que S2, en escala de semanas).
**Nivel de evidencia:** **fuerte** para el espaciado a escala de semanas; **[Inferencia]** para el deload cognitivo importado del entrenamiento físico — es una heurística razonable, no un hallazgo establecido. Debe etiquetarse como tal y probarse (§6.6).

**Mecánica.** `TrainingCycle` (`focus/domain/session/TrainingCycle.kt:27-75`) es exactamente el artefacto correcto en el sitio equivocado. Promoverlo a `core/curriculum/` con un microciclo transversal de 4 semanas:

| Semana | Énfasis | MixPolicy | Objetivo de error | Probes |
|---|---|---|---|---|
| 1 (adquisición) | 2 módulos, bloques largos | BLOCKED | 20-25 % | baseline holdout |
| 2 (consolidación) | 3 módulos | INTERLEAVED_WITHIN_MODULE | 25-30 % | repaso 24 h/3 d |
| 3 (variación) | 3-4 módulos + contextos alternos | INTERLEAVED_CROSS_MODULE | 30-35 % | repaso 7 d |
| 4 (deload + evaluación) | volumen ×0.5 | BLOCKED corto | 15-20 % | **batería de probes no entrenados** |

**Medición:** posición en el ciclo, volumen realizado/planificado, adherencia por fase.
**Adaptación:** el gate de recuperación ya existe (`RecoveryGate.RestRecommended` / `Reduced`, `DailyFocusPlan.kt:79-93`), y ya sabe devolver día de descanso. Generalizarlo con la señal de estado de S8.

**Métrica que verifica que funciona:**
- **Rendimiento en la semana 4 (evaluación) vs semana 1 (baseline) sobre holdout**, que es el único contraste con significado.
- **Adherencia y tasa de abandono por fase** — si la semana 3 (máxima interferencia contextual) dispara el abandono, la dificultad deseable se convirtió en dificultad indeseable y hay que bajar el ratio de switching, no aumentar la gamificación.
- **[Dato necesario]** No existe ninguna telemetría de adherencia longitudinal a nivel de sistema. Sin ella la periodización es un modelo sin realimentación.

---

### S6 — Calibración metacognitiva transversal (la mejora de mayor retorno del sistema)

**Capacidad objetivo:** metacognición — monitorización y control.
**Mecanismo:** juicio de confianza post-respuesta + feedback de calibración. La discrepancia entre confianza y acierto es la señal de aprendizaje; el usuario aprende a discriminar *saber* de *sentir que sabe*, lo que gobierna las decisiones de estudio, de parada y de riesgo.
**Nivel de evidencia:** **moderado** — la calibración es entrenable con feedback, y a diferencia del entrenamiento de capacidad tiene una vía de aplicación directa (regula qué haces cuando no sabes). No es una promesa de transferencia lejana; es una habilidad de decisión con dominio propio.

**Mecánica.** Es la única mejora que se puede desplegar **en los ocho módulos con una sola pieza** y sin contenido nuevo:

```
core/metacognition/
  ConfidenceProbe(scale = 1..4, askedOn: ProbeSchedule)   // no en cada trial: en un 25-30 % muestreado
  CalibrationScorer → Brier, calibration curve (slope/intercept), resolution (gamma), over/underconfidence bias
```

El repositorio ya tiene **tres implementaciones abandonadas del mismo constructo** — la prueba más clara de que es un concern de sistema y no de módulo:
- `mindverse/core/scoring/ScoringEquations`: `calculateBrierScore`, `calculateCalibrationError`, `calculateCAG` — implementados, **nunca invocados**.
- Emociones: existe la clave `CALIBRATION_ERROR` sin generador (EM4/EM14 no cableados).
- Memoria: `confidenceBias = 0.0` fijo (`MemorySessionStateHolder.kt:145`) ⇒ `ActivateMetacognition` no dispara nunca.
- Lenguaje: `confidence` existe en `ScoringTiming`, no se pide.
- Decisiones: la certeza **se recoge en Bayes Arena y se descarta**.

**Medición:** Brier decompuesto (calibración / resolución / incertidumbre), pendiente de calibración, sesgo de sobreconfianza, todo por constructo y por módulo.
**Adaptación:** el feedback es la intervención. Tras cada bloque: "acertaste el 62 % de las respuestas en las que dijiste 'seguro'". Y la dificultad se ajusta a la zona donde la calibración es peor (que es donde el aprendizaje metacognitivo es máximo), no solo donde la precisión es media.

**Métrica que verifica que funciona:**
- **Reducción del sesgo de sobreconfianza** (|confianza media − acierto medio|) a lo largo de 4 semanas, **medida en un módulo que no dio feedback de calibración**. Ese es el test de transferencia: si la calibración solo mejora donde se entrenó, es aprendizaje del formato; si mejora en un dominio no entrenado, es la única transferencia que este producto puede reclamar con base.
- Resolución (gamma) creciente: distingue *estar calibrado* de *discriminar*. Un usuario que dice "50 %" a todo está calibrado y es inútil.

**[Mejora crítica]** Coste bajísimo (un componente de UI + un scorer puro + un almacén), impacto transversal, y es el único constructo del sistema que se puede medir de forma comparable entre los 8 módulos — es decir, **es el candidato natural a índice global honesto** el día que se quiera uno, muy por delante del compuesto ponderado de 9 dimensiones que `ADR_SCI_006_APPROVED = false` bloquea con razón.

---

### S7 — Batería de transfer probes de sistema (la única forma de responder la pregunta central)

**Capacidad objetivo:** ninguna — es el instrumento de verificación de todo lo demás.
**Mecanismo:** medir en material del mismo constructo pero **nunca entrenado**, y contrastar contra el material entrenado.
**Nivel de evidencia:** precondición metodológica. Sin ella, "mejora" y "familiarización" son indistinguibles por construcción.

**Mecánica.** El repositorio ya tiene las tres piezas y **ninguna está conectada extremo a extremo**:
- `ReliableChangeIndex` (Jacobson-Truax, correcto y testeado, en Memoria).
- `RealImprovementCriterion` (gate de probe no entrenado + tamaño de efecto + 2 semanas — honesto).
- `ProbeReliabilityEstimator` de Enfoque, correctamente fail-closed: **vacío ⇒ nunca afirma cambio fiable**. Esto es lo mejor del sistema y no debe tocarse; debe **alimentarse**.

Lo que falta es de sistema:
1. Promover RCI + RealImprovementCriterion a `core/measurement/` (hoy son privados de Memoria y de Enfoque, duplicados conceptualmente).
2. **Un `ProbeSlot` en la sesión** (S1) que administre probes en semana 1 y semana 4 del microciclo (S5), sobre el holdout (S4), en modo producción (S3).
3. Cobertura de probes por constructo, no por ejercicio. Enfoque tiene probes para 3 de 13 ejercicios (`ProbeExerciseMapping`); el módulo entrena 13 cosas y solo puede evaluar transferencia de 3.
4. **Estimación de fiabilidad test-retest antes de reclamar nada**: el RCI necesita la fiabilidad del instrumento; sin ella el índice es incalculable, y el sistema hoy lo sabe (por eso está fail-closed). **[Dato necesario]** Requiere una campaña de test-retest (n≥60, dos administraciones separadas 7-14 días, sin entrenamiento intermedio) por cada probe que se quiera usar.

**Métrica que verifica que funciona:**
- **`Δ_entrenado − Δ_holdout` con IC del 95 %**, por constructo. Es *la* métrica de esta sección entera.
- **% de dimensiones con probe válido disponible.** Hoy: 0 de 9 con la cadena completa. Objetivo mínimo antes de cualquier claim de progreso: 3 de 9.
- **Tasa de "cambio fiable" declarado**: debe ser baja. Un sistema que declara mejora fiable en el 80 % de los usuarios a las 4 semanas está roto.

---

### S8 — El estado (emoción / activación / fatiga) como covariable de primera clase

**Capacidad objetivo:** regulación emocional aplicada al rendimiento + validez de todas las demás medidas.
**Mecanismo:** el estado afectivo y la fatiga modulan la atención sostenida y el control ejecutivo dentro del sujeto y del día. Ignorarlo mete varianza de estado dentro de la estimación de rasgo — es decir, el ruido del día se lee como cambio de capacidad.
**Nivel de evidencia:** **fuerte** para el efecto de estado sobre rendimiento momentáneo; **débil/no demostrado** para "entrenar regulación mejora la capacidad cognitiva basal". La mejora es de **validez de medida** primero y de habilidad después.

**Mecánica.** Decisiones ya tiene un check-in con CRI (`decisiones/presentation/DecisionCheckInStateHolder.kt:46,95`: auto-reporte de disposición + go/no-go, produce `CognitiveReadinessState.criValue`) y Emociones tiene `OverloadDetector`, `PauseGovernor` y `EmotionSafetyLimits`. **Nada de esto sale de su módulo.** El CRI se calcula, se pinta en un polígono (`DecisionResultStateHolder.kt:84`) y muere ahí.

Promover a `core/state/`:

```
ReadinessSnapshot(selfReportedReadiness, criValue, timeOfDay, sessionsLast24h, source)
ReadinessGate: decide volumen/dificultad/permiso de medición ANTES del primer bloque
```

Tres usos, en orden de valor:
1. **Covariable de scoring** (el mayor valor, y el menos glamuroso): registrar el `ReadinessSnapshot` junto a cada `SessionRecord` para poder **modelar y descontar la varianza de estado** al estimar tendencia. Sin esto, cualquier "estás mejorando" incluye "hoy dormiste bien".
2. **Gate de elegibilidad de medición:** readiness muy baja ⇒ la sesión es válida para práctica pero **no actualiza el perfil**. Es exactamente el mismo patrón fail-closed que ya se usa con el timebase no autoritativo (`SpeedProfileSource`) y con `BankIntegrity`. Coherente con la arquitectura existente.
3. **Gate de carga:** readiness baja ⇒ menos volumen, menos presión temporal, bloques más cortos. `RecoveryGate` (Enfoque) ya sabe hacerlo; le falta la señal.

**Métrica que verifica que funciona:**
- **Reducción de la varianza residual intra-sujeto** del score una vez incluida readiness como covariable. Si readiness explica ≥10 % de la varianza intra-sujeto, el sistema estaba atribuyendo a "capacidad" algo que era "estado", y el gate está justificado.
- **Correlación readiness↔rendimiento** por módulo: cuantifica qué medidas son frágiles al estado (predicción: Enfoque y Velocidad altísima; Lógica y Lenguaje menor). Eso, por sí solo, es un hallazgo de producto: informa qué se puede medir en un día malo.

---

## 6.3 Progresión global: de "8 escaleras independientes" a una política de dificultad única

**Problema estructural actual:** cuatro motores adaptativos incompatibles y dos muertos.

| Módulo | Motor | Estado |
|---|---|---|
| Enfoque | escalera por vector de dificultad + `TrainingCycle` + `RecoveryPolicy` | **vivo**, personalización contenida a NEUTRAL (`DailyFocusPlan.kt:60-66`) |
| Memoria | `DefaultMemoryAdaptiveEngine` | vivo, gobernado por métricas de reconocimiento saturadas |
| Velocidad | staircase multi-señal sobre 12 trials/bloque | vivo, estadística ruidosa |
| Lógica | `LogicDifficultyAdapter` calcula theta | **muerto** — `startSession()` nunca lo consume |
| Decisiones | `mindverse/core/adaptive/DifficultyAdapter` | **muerto** — nunca invocado |
| Lenguaje | `DefaultAdaptationEngine` | **muerto** — el holder usa `ScoringContext` por defecto y orden fijo |
| Emociones | un eje, `STEP_UP=0.07` | vivo, mueve dificultad de tarea, no de constructo |
| Creatividad | ninguno | inexistente |

**Mejora estructural:** una política única en `core/adaptive/`, con dos parámetros y una regla:

- **Objetivo por tasa de error, no por precisión.** Zona de dificultad deseable: **25-30 % de error** en fase de consolidación (donde la recuperación es esforzada pero exitosa), 15-20 % en deload, 30-35 % en fase de variación. Hoy cada módulo tiene su propio objetivo implícito y ninguno está justificado por escrito.
- **Dos ejes ortogonales obligatorios por ejercicio:** carga (cuánto hay que mantener/procesar) y **presión temporal**. Los módulos que solo aprietan el reloj (Velocidad) entrenan tolerancia a deadlines, no procesamiento; los que solo suben carga sin tiempo (Memoria en runtime) permiten repaso ilimitado y anulan la carga. Exigir ambos ejes por contrato de tipo.
- **La adaptación se prohíbe sobre métricas no confiables.** Regla fail-closed idéntica a la del perfil: si la métrica que gobierna la escalera tiene `confident = false` o `n < MinSampleGate`, la dificultad **no se mueve**. Hoy Velocidad adapta sobre 12 trials y Emociones sobre 20 — adaptación errática que el usuario lee como progreso/regresión aleatoria, que es adherencia destruida por ruido.

**Métrica que verifica que funciona:**
- **Distribución de la tasa de error observada** por usuario y sesión: debe concentrarse en la banda objetivo. Si la moda está en 5 % o en 60 %, la escalera no converge.
- **Volatilidad de la dificultad** (desviación típica del nivel entre sesiones consecutivas): debe caer tras aplicar el gate de confianza. Es la métrica directa de "dejó de oscilar por ruido".

---

## 6.4 Mezcla de módulos: qué se intercala con qué (y qué no)

Intercalar por intercalar destruye la medición de los módulos RT-críticos. Matriz de decisión:

| Par | ¿Mezclar? | Razón funcional |
|---|---|---|
| Enfoque ↔ Memoria | **Sí, alto valor** | Complementarios y con demanda ejecutiva compartida; además Enfoque provee la **tarea distractora** que Memoria necesita para inducir demora real (el `ProcessingTask` de EX1 y los bloques de Enfoque sirven para lo mismo). Mata dos pájaros: intercalado + demora real. |
| Memoria ↔ Lenguaje | **Sí** | El repaso espaciado de vocabulario/errores de Lenguaje es literalmente el protocolo `VOCABULARY` que Memoria declara y no ejecuta. Mismo scheduler (S2), contenido distinto. |
| Lógica ↔ Lenguaje | **Sí** | Comparten el andamiaje de justificación estructurada y evidencia; alternarlos varía el contexto superficial manteniendo el constructo (variabilidad de estímulos con constructo constante = el patrón que mejor transfiere). |
| Decisiones ↔ Emociones | **Sí, prioritario** | Es el par con más sentido funcional del producto: decidir bajo estado afectivo activado. Emociones EM8 (anti-impulso) precede a Bayes Arena. |
| Enfoque ↔ Velocidad | **No** | Miden la misma latencia con el mismo timebase; intercalarlos añade arrastre motor y no varía el constructo. Además Velocidad debería degradarse a **baseline SRT** que corrige los RT del resto (recomendación del audit de Velocidad), no competir como track. |
| Creatividad ↔ cualquiera | **Solo al final** | Producción divergente con fatiga previa colapsa fluidez; y la fluidez es hoy su única métrica. Colocarlo siempre en la misma posición del bloque o su medida es incomparable. |
| Cualquier RT-crítico tras bloque largo | **Prohibido sin marca** | Enfoque/Velocidad requieren `CHOREOGRAPHER_FRAME + NANOS_NATIVE` y son sensibles a fatiga. Si van en posición ≠ 1, el `SessionBlock` debe registrar `positionInSession` y el scoring debe **excluir o modelar** ese efecto. **[Riesgo científico]** sin esto, el intercalado introduce un sesgo sistemático en las medidas más frágiles. |

**Consolidación previa obligatoria:** hoy el hub muestra 8 puertas que aparentan 8 capacidades. Realmente son menos: Emociones son 3 paradigmas vendidos como 6; Velocidad son 2 constructos en 4 pantallas; Enfoque tiene 3 ejercicios (E8/E9/E10) que no miden lo que su nombre dice. **Antes de mezclar hay que fusionar**, o el intercalado alternará entre variantes del mismo paradigma creyendo que varía el contexto — el peor de los dos mundos: coste de switching sin variación de constructo.

**Métrica que verifica que funciona:** coste de cambio medido (RT/accuracy del primer trial tras cambio de módulo vs trials 2-n). Si el coste de cambio es ≈0, no se está variando nada; si es enorme y persistente, los bloques son demasiado cortos y solo se mide re-orientación.

---

## 6.5 El bucle emoción ↔ cognición ↔ motivación ↔ atención ↔ metacognición

Hoy estos cinco están en cinco sitios distintos y **no hay un solo camino de datos entre ellos**. Emociones no lee nada; Decisiones calcula CRI y lo tira; la motivación no existe como concepto en el código (no hay adherencia, ni objetivos, ni contrato de compromiso); la metacognición tiene tres implementaciones abandonadas.

**Arquitectura del bucle — cuatro aristas, todas construibles con piezas existentes:**

```
                    ReadinessSnapshot (S8)
   [EMOCIÓN/ESTADO] ─────────────①────────────▶ [ATENCIÓN / CARGA]
        ▲                                              │
        │⑤ regulación                              ② rendimiento + error
        │  aplicada                                     ▼
   [MOTIVACIÓN] ◀────────④─────── [METACOGNICIÓN: confianza vs acierto (S6)]
        │                                              ▲
        └──────────── ③ objetivo/dificultad elegida ───┘
```

**① Emoción → Atención (gate de carga y de medición).** `ReadinessSnapshot` (CRI + auto-reporte + sesiones últimas 24 h) entra en `ReadinessGate` **antes** del primer bloque y fija volumen, presión temporal y **si la sesión es elegible para actualizar el perfil**. Piezas existentes: `DecisionCheckInStateHolder` (cálculo del CRI), `RecoveryGate` (sabe reducir volumen y recomendar descanso), `SessionEligibility` (sabe excluir sesiones del perfil).
*Métrica:* % de varianza intra-sujeto del rendimiento explicada por readiness; falsa-alarma del gate (sesiones excluidas que luego resultan de rendimiento normal).

**② Atención/Error → Metacognición (la señal de aprendizaje).** Cada bloque emite `(confianza, acierto, latencia)`. `CalibrationScorer` devuelve calibración y resolución. Aquí es donde el error deja de ser un castigo y se convierte en información: el feedback no es "fallaste", es "fallaste **donde te sentías seguro**", que es la única forma de feedback de error que produce cambio de estrategia.
*Métrica:* Brier decompuesto; % de trials de alta confianza fallados (el objetivo de la intervención).

**③ Metacognición → Motivación (elección de dificultad).** Dar al usuario la elección **calibrada** del reto siguiente: el sistema propone la banda de dificultad deseable (25-30 % error) y el usuario elige dentro de ella. Autonomía dentro de un rango científicamente correcto — no autonomía total (que lleva a elegir demasiado fácil) ni imposición total (que destruye la motivación autónoma).
*Métrica:* dificultad elegida vs dificultad óptima estimada; adherencia por condición (elección vs imposición) en A/B.

**④ Motivación → Metacognición (contrato de progreso honesto).** Esta es la arista más delicada y la que el producto ya tiene medio construida y desperdiciada. El feedback longitudinal debe ser **el cambio en el holdout no entrenado (S7)**, no la accuracy de la sesión. Es menos halagador y es lo único no fabricable. El sistema ya rechaza el compuesto global (`compositeIndex = null`) y ya sabe decir `CALIBRATING` en vez de 0 — la infraestructura de honestidad existe; falta el dato al que apuntarla.
*Métrica:* retención a 7 y 30 días (adherencia) bajo feedback honesto vs feedback de accuracy. **[Dato necesario]** hipótesis abierta: el feedback honesto puede costar adherencia a corto plazo. Hay que medirlo, no asumirlo en ninguna de las dos direcciones.

**⑤ Metacognición/Cognición → Emoción (regulación aplicada, no genérica).** El módulo de Emociones deja de ser una puerta separada y pasa a ser **un modo de los demás**: los bloques de separación/defusión (EM3, el único ejercicio con base teórica sólida del módulo) se insertan tras un bloque de alto error en Lógica o Decisiones — que es cuando la frustración es real y la regulación tiene objeto. Regular una emoción inducida por un banco de 28 frases es un simulacro; regular la frustración por haber fallado 8 de 10 problemas es el constructo.
*Métrica:* rendimiento y persistencia en el bloque **posterior** al bloque de alto error, con vs sin bloque de regulación intercalado. Es un contraste limpio, intra-sujeto y directamente interpretable.

**Restricción no negociable:** este bucle mueve datos de estado afectivo. Nada de él puede describirse ni medirse en términos de condición de salud, y la inducción de estado debe seguir siendo leve y con salida garantizada (`PauseGovernor`, `EmotionSafetyLimits` ya lo hacen bien). El bucle es sobre **rendimiento aplicado**, y su métrica es rendimiento posterior, no estado reportado.

---

## 6.6 Cuadro de mando de sistema (lo que verifica que todo lo anterior funciona)

Seis números. Si estos seis no se pueden calcular, no hay sistema.

| # | Métrica | Definición | Umbral de éxito | Existe hoy |
|---|---|---|---|---|
| M1 | **Brecha entrenado−holdout** | Δaccuracy ítems entrenados − Δaccuracy holdout, por constructo, IC 95 % | Brecha reportada siempre; mejora de holdout > 0 con IC que excluye 0 en ≥1 constructo a las 8 semanas | **No** (holdout existe, no se sirve) |
| M2 | **Curva de retención** | accuracy vs log(intervalo) por constructo | Pendiente se aplana en cola programada vs no programada | Solo Memoria, parcial |
| M3 | **Calibración** | Brier decompuesto + pendiente + sobreconfianza | Sobreconfianza ↓ en un módulo **sin** feedback de calibración | **No** (3 impls muertas) |
| M4 | **Convergencia de dificultad** | distribución de tasa de error; volatilidad del nivel | moda en banda objetivo; volatilidad ↓ tras gate de confianza | Parcial (Enfoque/Memoria) |
| M5 | **Exposición** | veces vista cada ítem por usuario; correlación exposición↔accuracy | correlación ≈0 en ítems que informan el perfil | **No** |
| M6 | **Adherencia por fase** | sesiones completadas/planificadas, retención 7/30 d, abandono por fase del microciclo | sin caída >20 % en la fase de máxima interferencia | **No** |

**El hallazgo incómodo:** 4 de 6 no existen. Y las que más pesan (M1, M3) llevan meses de infraestructura construida, testeada y no conectada — `RealImprovementCriterion`, `ReliableChangeIndex`, `TransferProbePartition`, `ScoringEquations.calculateBrierScore`, `NearTransferVerifier`, `ProbeReliabilityEstimator`. **El problema del sistema no es que falte ciencia; es que la ciencia construida no tiene por dónde correr, porque no hay una capa de sesión ni un currículo que la invoque.** S1 (contenedor) y S7 (probes) son, literalmente, el cableado que hace ejecutable lo ya escrito.

---

## 6.7 Orden de ejecución (y lo que NO hay que hacer)

**Orden, por dependencia real y no por atractivo:**

1. **S6 (calibración transversal)** — coste mínimo, transversal, tres implementaciones ya escritas esperando un consumidor, y es el único constructo comparable entre módulos. Empieza a generar M3 desde el día 1.
2. **S4 (bancos + holdout + exposición)** — sin esto, ninguna medición longitudinal posterior es interpretable. Bloquea a S7. Incluye los generadores paramétricos de Lógica y Decisiones, que son los dos bancos más críticos (6 ítems y 1 plantilla).
3. **S3 (producción sobre reconocimiento)** — cambia el techo de todo el sistema. Debe ir **después** de S4 (banco grande) o el usuario agota el contenido produciendo en vez de reconociendo.
4. **S2 (espaciado transversal)** — mecanismo con mejor evidencia, scheduler ya escrito, siete productores de eventos por añadir.
5. **S1 (capa de sesión)** — habilita intercalado, contextos y probes. Refactor de superficie sobre 8 `RootScreen`.
6. **S7 (probes de sistema)** + campaña de fiabilidad test-retest. Depende de 2, 3 y 5.
7. **S8 (estado como covariable)** — mejora la validez de todo lo anterior; se puede adelantar si la campaña de S7 se retrasa.
8. **S5 (periodización global)** — la última, porque solo tiene sentido cuando hay algo que periodizar y una métrica de adherencia que la valide.

**Lo que NO hay que hacer, explícitamente:**

- **No desbloquear el índice global.** `ADR_SCI_006_APPROVED = false` y `compositeIndex = null` son correctos. Un número único sobre 9 dimensiones sin modelo de ponderación, sin equating entre módulos y sin calibración por motor es exactamente el tipo de cifra que un usuario leerá como una puntuación de capacidad general. Si algún día hace falta un número transversal, **el candidato honesto es la calibración (S6)**, no el compuesto ponderado.
- **No añadir ejercicios nuevos antes de fusionar los redundantes.** Hoy: Emociones 6 pantallas = 3 paradigmas; Velocidad 4 = 2 constructos; Enfoque E3/E4, E2/E5, E1/E12 duplicados; Creatividad EX01/EX02 comparten scorer. Añadir contenido sobre esa base multiplica la ilusión de variedad, que es exactamente el fallo de producto que el intercalado necesita evitar.
- **No gamificar la adherencia antes de tener M1.** Rachas, puntos y niveles sobre un sistema que hoy solo puede medir familiarización producen adherencia a la familiarización. La motivación tiene que engancharse a la señal correcta o cementa la equivocada.
- **No cablear ninguna fuente al perfil sin banco validado + timebase autoritativo + cobertura anti-gaming declarada.** Esa disciplina ya existe (`BankIntegrityAuthority`, `DetectorCoverage`, gate de timebase) y es el activo más valioso del repositorio. Todas las mejoras de esta sección deben pasar por ella, no rodearla.

---

## 6.8 Diseño experimental que decide si el sistema funciona

Un solo estudio, cuatro brazos, y responde a la pregunta central en 8 semanas.

**Diseño:** aleatorizado, intra-sujeto para la medida de resultado, n≥200 (50/brazo) para detectar d≈0.3 en el contraste entrenado−holdout con potencia 0.8. **[Dato necesario]** el n exacto depende de la fiabilidad test-retest de los probes, que hay que estimar primero (n≥60, dos administraciones a 7-14 días).

| Brazo | Manipulación |
|---|---|
| A (control activo) | Sistema actual: bloqueado, sin espaciado, reconocimiento, sin calibración |
| B | + S2 espaciado + S3 producción |
| C | + S1/S5 intercalado y periodización |
| D | Sistema completo (B + C + S6 calibración + S8 gate de estado) |

**Medida primaria:** Δ en **probes no entrenados** (S7), semana 0 vs semana 8, con RCI por sujeto.
**Medidas secundarias:** retención a 7 y 30 días; calibración (Brier, sobreconfianza) **en un módulo sin feedback de calibración**; adherencia; brecha entrenado−holdout.
**Control de expectativa:** los cuatro brazos reciben el mismo relato de producto y el mismo feedback en formato; solo cambia la mecánica. Sin esto, la diferencia observada mide la expectativa, que es el defecto metodológico que hunde la mayor parte de la literatura de entrenamiento cognitivo (Simons et al. 2016).
**Predicción honesta y falsable:** B > A en retención; D > A en calibración con transferencia dentro del dominio metacognitivo; **y probablemente A ≈ B ≈ C ≈ D en probes de capacidad no entrenada**. Si ese último resultado se confirma, es el hallazgo más valioso posible: define el claim máximo defendible del producto y lo separa para siempre del marketing de "entrenamiento cerebral".

**Claims que este diseño podría autorizar:** mejora de retención de lo practicado; mejora de calibración; progreso frente a la propia línea base en constructos con probe válido.
**Claims que este diseño no autorizará nunca:** mejora de capacidad general, transferencia lejana, rendimiento fuera de lo medido, o cualquier lectura del perfil como valoración de una condición.

**[Riesgo científico residual]** Aun ejecutando las ocho mejoras, la posición honesta por defecto sigue siendo: *el producto entrena estrategias, calibración y retención, y mide con rigor el progreso contra la propia línea base*. Cualquier arquitectura que necesite prometer más que eso para ser vendible está mal diseñada, no mal comunicada.

---

## Anexo C. Metacognicion
# Auditoría metacognitiva transversal — MindBoost

**Veredicto: NO. MindBoost mide el pensamiento del usuario; no le enseña a pensar sobre su pensamiento.**

Existe andamiaje metacognitivo real, disperso, y en su mayoría **desconectado, muerto o con el incentivo invertido**. Hay exactamente **un** motor que enseña *cómo* mejorar (`DefaultLogicFeedbackEngine`) y su propio scorer, en el mismo módulo, **premia mentir sobre la confianza**. El perfil cognitivo declara `metacognition` como dominio canónico (CLAUDE.md §1) pero **`CognitiveDimension` no lo contiene** (`profile/domain/model/CognitiveTaxonomy.kt:14-24`, 9 entradas): no hay ni siquiera dónde representarla.

---

## 1. Inventario transversal: quién captura, quién computa, quién devuelve

| Módulo | ¿Captura confianza subjetiva? | ¿Computa calibración? | ¿La devuelve al usuario? | Estado real |
|---|---|---|---|---|
| **Logic** | **Sí** — `LogicConfidence{LOW .35/MED .65/HIGH .9}`, `LogicStateHolder.kt:98,117,125` | Un sustituto **impropio** (`ReasoningScorer.kt:33-37`) | Sí, 2 frases (`LogicFeedbackEngine.kt:19-31,79-87`) + `averageConfidenceCalibration` | **Incentivo invertido** (H1) |
| **Decisiones (Bayes)** | **Sí** — slider `confidence: Double` `BayesArenaStateHolder.kt:55,77` | **No** (documentado y honesto, `BayesArenaStateHolder.kt:23-25`) | Solo `meanConfidence` en el summary (`:110`) | **Dato recogido y tirado** (H4) |
| **Decisiones (Coach)** | — | `calculateBrierScore`/`calculateCAG` existen (`ScoringEquations.kt:114,152`) | **Nunca**: 0 callers de producción | **Matemática muerta** (H2) |
| **Memoria** | **No** — `ConfidenceJudgment` (`MemoryValueObjects.kt:100`) solo se construye en tests | `confidenceBias` **hardcodeado a 0.0** (`MemorySessionStateHolder.kt:263`) | No | **Slot muerto** (H3) |
| **Language** | **No** — `ScoringTiming.confidence: Proportion? = null` (`ScoringEngine.kt:52-57`), invocado siempre como `ScoringTiming()` (`LanguageSessionStateHolder.kt:101`) | No | No | **Slot muerto** |
| **Focus** | `SubjectiveState(sleep, caffeine, mood, hour)` — persistido | `POST_ERROR_SLOWING`/`_ACCURACY` **sí se computan** (`DefaultMetricsEngine.kt:177-195`) | **No** — `buildReport` solo emite ACCURACY/RT_MEDIAN/RT_CV/LAPSES (`FocusSessionAssembler.kt:128-151`) | **Computado y descartado** (H5) |
| **Speed / E8** | No | No | `GoNoGoFeedbackKind` (8 casos conductuales, honesto) | Sin capa metacognitiva |
| **Emotions** | No | `CALIBRATION_ERROR` declarado (`EmotionMetricKey.kt:17`) y **nunca computado** | "Calibración" = madurez del perfil, no confianza↔acierto | **Etiqueta engañosa** (H8) |

**Conclusión del inventario:** de 8 módulos, **2 capturan confianza**, **0 computan calibración honesta**, **0 la devuelven como enseñanza**.

---

## 2. Hallazgos

### H1 — [Mejora crítica] El scorer de Lógica usa una regla de puntuación IMPROPIA: enseña activamente a sobreconfiar

`ReasoningScorer.kt:33-37,46,95`:

```kotlin
val confidenceScore = if (isCorrect) response.confidence.probability
                      else 1.0 - response.confidence.probability
// ...
CONFIDENCE_WEIGHT * confidenceScore   // CONFIDENCE_WEIGHT = 0.10 de 100 pts
```

Esto es el **score lineal**, y es matemáticamente impropio. Si el usuario cree con probabilidad `q` que acertó y reporta `p`:

```
E[score] = q·p + (1−q)(1−p) = (1−q) + p·(2q−1)
```

Es **lineal en `p`** ⇒ el óptimo está siempre en un extremo: `p = 0.9` si `q > 0.5`, `p = 0.35` si `q < 0.5`. **Reportar la creencia honesta nunca es estrictamente óptimo.** Y como el usuario acaba de elegir su respuesta (casi siempre cree `q > 0.5`), la estrategia dominante es **marcar HIGH siempre**: 10 puntos gratis sobre 100 en el ICR.

Peor: la etiqueta miente. `ReasoningScore.confidenceCalibrationScore` (`LogicModels.kt:195`) y su agregado `averageConfidenceCalibration` (`LogicStateHolder.kt:227-229`) **no miden calibración** — miden `1 − |acierto − p|`, que mezcla calibración con **exactitud**. Un usuario que acierta el 95% y siempre marca HIGH saca 0.86; un usuario perfectamente calibrado al 60% saca 0.53. El "score de calibración" **premia al más preciso, no al mejor calibrado**. Son ortogonales y están fusionados.

El único guardarraíl real es `LogicErrorTag.OVERCONFIDENT_ERROR` (`ReasoningScorer.kt:56-58`), que solo dispara con `p ≥ 0.85` **y** fallo — no toca la ganancia sistemática del que siempre marca HIGH y acierta.

[Riesgo científico] Se está entrenando el sesgo exacto que se pretende corregir. Un producto que cobra por "pensar mejor" y cuya única métrica de confianza recompensa la sobreconfianza no es neutro: es iatrogénico a nivel de hábito.

---

### H2 — [Mejora crítica] La mitad metacognitiva del CoachEngine es estructuralmente inalcanzable

`CoachEngine` declara 15 plantillas; **6 (T04–T09, el 40%)** son los perfiles `sobreconfiado` y `subconfiante`, y dependen de `cag`, `brierScore`, `confidence100Failure`, `systematicFloorConfidence`, `lowConfidenceHighReliability`, `decisionalCourage`, `likelihoodRatio`, `averageConfidence`, `actualAccuracy`.

El **único call-site de producción** (`DecisionSessionOrchestrator.kt:172-180`) construye:

```kotlin
CoachSignals(
    reactionTimeMs = meanReactionMs, icd = icd, ied = ied,
    agrs = gamingScore, frs = fatigueScore, reactionTimeCv = readiness.reactionTimeCV,
)
```

**6 de 22 campos.** Los nueve campos metacognitivos quedan `null`/`false`, así que `selectTemplate` (`:118-136`) nunca alcanza T04–T09: `(signals.cag ?: 0.0) > 0.15` es siempre falso, `confidence100Failure` siempre false. `calculateBrierScore` y `calculateCAG` **tienen cero callers de producción** — solo `AlphaCoreTest.kt:41-62`.

Es exactamente el patrón que CLAUDE.md ya documentó (2026-07-04): *"un detector testeado que nadie invoca protege cero"*. Aquí: **un coach metacognitivo testeado que nadie puede recibir**.

---

### H3 — [Mejora crítica] `confidenceBias = 0.0` hardcodeado mata toda la metacognición de Memoria

`MemorySessionStateHolder.kt:263`:

```kotlin
val performance = MemoryPerformance.of(metrics, confidenceBias = 0.0, config = MemoryAdaptiveConfig())
```

Consecuencias en cadena, todas verificadas:

1. `MemoryAdaptiveEngine.kt:54-55` — `abs(input.confidenceBias) > cfg.confidenceBiasThreshold (0.20)` es **siempre falso** ⇒ `ActivateMetacognition` ("Calibración pobre: metacognición") es **inalcanzable**. Es una rama de seguridad diseñada, testeada (`MemoryAdaptiveEngineTest.kt:63`) e imposible de disparar en producción.
2. `ArchetypeInference.kt:77-78` — los arquetipos `OVERCONFIDENT`/`UNDERCONFIDENT` reciben siempre `0.0` ⇒ **inalcanzables**.
3. `MemoryPerformance.of` (`MemoryAdaptiveModels.kt:146`) — el término `wConfidenceBias · bias²` es siempre 0 ⇒ la penalización por mala calibración no existe.
4. `MemoryMetricKey.METACOGNITIVE_RESOLUTION` (`:35`) y `MemoryParadigm.METACOGNITIVE_CALIBRATION` (`MemoryExerciseCatalog.kt:404`, Ej.16) están declarados y **nunca computados**.

`ConfidenceJudgment` (`MemoryValueObjects.kt:100`) con su `ConfidenceScale{BINARY/FOUR_POINT/SIX_POINT}` y su KDoc *"the scale widens as metacognition improves"* es un diseño correcto y completamente **no cableado**: `RecalledWord.confidence` y `ScoredResponse.confidence` (`MemoryScoring.kt:21,28`) son `null` por defecto y ninguna ruta de producción los rellena.

---

### H4 — Bayes Arena: certeza pedida al usuario y descartada (teatro cognitivo)

`BayesArenaStateHolder.kt:55` captura `confidence: Double` y su KDoc (`:23-25`) es honesto: *"No Brier / calibration / CAG, which are undefined without a resolved binary outcome"*. La honestidad es correcta; la **decisión de producto no**: se le impone al usuario una carga cognitiva por trial cuyo dato termina en `meanConfidence` (`:110`), un número que no alimenta feedback, ni score, ni perfil.

[Inferencia] El outcome binario **sí existe y es derivable**: `absoluteError ≤ τ` respecto a `scenario.expectedPosterior` (ya calculado, `:77`) es un resultado resuelto. Con τ declarado, Brier y CAG son computables hoy sin recoger un solo dato nuevo. La afirmación "undefined" es cierta para el posterior continuo, falsa para el juicio "¿estuve dentro de tolerancia?".

---

### H5 — Focus: la señal metacognitiva más valiosa se computa y se tira en la frontera del informe

`DefaultMetricsEngine.kt:177-195` computa `POST_ERROR_SLOWING` y `POST_ERROR_ACCURACY` correctamente (comparación RT post-error vs post-correcto). `FocusSessionAssembler.buildReport` (`:128-151`) construye el headline con **exactamente cuatro claves**: `ACCURACY`, `RT_MEDIAN`, `RT_CV`, `LAPSES`. El post-error **no aparece**.

Es decir: MindBoost sabe si te recuperas bien de tus errores y **no te lo dice nunca**. Recuperación post-error es el único constructo de esta lista con vínculo directo a autorregulación real, y está a una línea de `buildList` de ser visible.

Segundo: `SubjectiveState(sleepHours, caffeine, mood1to5, localHour)` (`FocusPersistenceMappers.kt:71-75`) se captura y se persiste (dato *health-adjacent*, con su coste de privacidad ya pagado) y **nunca se correlaciona con el rendimiento medido**. La pregunta metacognitiva de mayor valor del producto — *"¿sabes leer tu propio estado?"* — tiene los dos operandos en disco y nadie los cruza.

---

### H6 — El feedback dice QUÉ pasó, casi nunca CÓMO mejorar

- **`DefaultLogicFeedbackEngine` es la excepción y el estándar a copiar** (`:38-88`): 8 tipos de distractor → diagnóstico del patrón + `nextStep` accionable *sin revelar la solución* ("Para refutar P → Q, busca P verdadero y Q falso"). Esto **sí es enseñanza metacognitiva**.
- **`CoachUiBuilder.resultLine`** (`:48-52`) devuelve tres cadenas fijas: `"Buen desempeño; conservaste el núcleo."` / `"Sesión registrada; hay margen de mejora."` — cero contenido estratégico. El único `nextStep` real llega si hay `repairPlan` (`:34`); si no, es `EvidenceCopy.nextEvidenceNeeded` (qué evidencia falta para el sistema, no qué hacer el usuario).
- **`FocusReport.honestNote`** (`FocusSessionAssembler.kt:168-177`) es honestidad epistémica ejemplar y **cero estrategia**: dice que no puedes concluir nada, no dice qué probar mañana.
- **`CoachEngine`** entrega mensajes con métricas crudas (`"IED penalizado a 0.0"`, `"BUR: {BUR}"`) — jerga de motor, no instrucción.
- **`GoNoGoFeedback`** (8 `Kind`) es lo mejor de la familia conductual: clasifica conducta, no rasgo ("respondiste antes de confirmar"), y la comisión se evalúa **antes** que la velocidad (`:45`) para que la velocidad nunca produzca refuerzo positivo si falla la inhibición. Correcto — pero sigue siendo *qué*, no *cómo*.

---

### H7 — "Calibración" significa tres cosas distintas en la misma app

1. **Madurez del perfil**: `EmotionCalibrationProgressBuilder` — 0/5 → 5/5 sesiones válidas; `GoNoGoFeedbackKind.CALIBRATING`; `MindBoostProfile` CALIBRATING.
2. **Confianza↔acierto**: `confidenceCalibrationScore` (Logic), `CALIBRATION_ERROR` (Emotions), `calculateCAG` (Alpha core).
3. **Ajuste de dificultad**: `ProfileSessionEligibility`/`CalibrationPolicy`.

El usuario lee "calibración" y no puede saber si se habla de él o del sistema. Además `EM4_CALIBRADOR` (`EmotionExerciseCatalog.kt:194-206`) declara `governingMetric = CALIBRATION_ERROR` y su `plainDescription` dice *"Mantén un ritmo estable bajo carga. Mide la variabilidad de tu tiempo de respuesta"* — **la métrica gobernante y la descripción son cosas distintas**. Es un desajuste métrica↔claim, no un problema de copy.

---

### H8 — Detección de errores: el usuario nunca declara si cree haberse equivocado

Cero ocurrencias en todo `mindboost` de un juicio de tipo *"¿crees que acertaste?"* posterior a la respuesta (postdicción / error awareness). Lo que hay es la confianza **prospectiva** de Logic y nada más. Sin postdicción no hay medida de **conciencia de error**, que es distinta de la calibración prospectiva y es el constructo metacognitivo con mejor rendimiento aplicado (saber cuándo revisar). [Dato necesario] no se recoge, luego no se puede afirmar nada sobre detección de errores.

---

## 3. Evaluación por eje

| Eje metacognitivo | Estado | Nota /10 |
|---|---|---|
| **Calibración confianza↔acierto** | Capturada en 2/8 módulos; computada con regla impropia en 1; el motor honesto (Brier/CAG) está muerto | **2** |
| **Detección de errores** | No existe postdicción. `OVERCONFIDENT_ERROR` es la única traza y es un tag, no un juicio del usuario | **1** |
| **Autoevaluación** | `SubjectiveState` capturado y nunca contrastado; `ConfidenceJudgment` no cableado | **1** |
| **Estrategias enseñadas** | Solo `DefaultLogicFeedbackEngine` (8 patrones, `nextStep` real). El resto: 0 | **3** |
| **Manejo post-error** | Métricas computadas en Focus/Emotions, **nunca devueltas**; sin manipulación experimental que provoque errores | **2** |
| **Representación en el perfil** | `METACOGNITION` no existe como `CognitiveDimension` | **0** |

**Global: 1.5/10.** No es que la metacognición esté mal implementada: está **implementada y desconectada**, que es peor, porque el código existente crea la ilusión de cobertura.

---

## 4. Propuesta: `core/metacognition/` — capa metacognitiva transversal

**Ubicación:** `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/metacognition/`, junto a `core/scoring`, `core/antigaming`, `core/timing`. Pura, sin dependencias de feature (regla de neutralidad de `core`, CLAUDE.md §3).

### 4.1 Principio arquitectónico no negociable

> **La confianza NUNCA suma al score de rendimiento.**

Es la causa raíz de H1. Mientras `CONFIDENCE_WEIGHT = 0.10` alimente el ICR, cualquier regla —propia o impropia— crea presión para reportar estratégicamente. La calibración es un **eje separado** que se **reporta**, no que se **puntúa** dentro del rendimiento. Primer cambio a hacer: sacar `CONFIDENCE_WEIGHT` de `weightedBase` (`ReasoningScorer.kt:46`) y redistribuir su 0.10 a `RULE_WEIGHT`/`PREMISE_WEIGHT`.

### 4.2 Contrato

```kotlin
// core/metacognition/MetacognitiveJudgment.kt
/** Juicio prospectivo: ANTES de conocer el resultado. probability ∈ [chance, 1.0]. */
data class ConfidenceForecast(val probability: Double, val scaleId: ScaleId, val chanceLevel: Double)

/** Juicio retrospectivo (postdicción): "¿crees que acertaste?" — mide CONCIENCIA DE ERROR. */
enum class ErrorAwareness { BELIEVED_CORRECT, BELIEVED_WRONG, UNSURE }

/** Un par juicio↔resultado. El outcome es binario RESUELTO; nunca se infiere. */
data class CalibrationPair(
    val forecast: ConfidenceForecast,
    val outcome: Boolean,
    val awareness: ErrorAwareness? = null,
)
```

### 4.3 Métricas — tres ejes ORTOGONALES, nunca uno derivado de otro

```kotlin
object MetacognitionMetrics {
    /** Regla PROPIA. Máximo estricto en p = creencia real. Uso: incentivo, no puntuación. */
    fun brier(pairs: List<CalibrationPair>): Double?          // null si n < MinSampleGate

    /** SESGO con signo: mean(p) − accuracy. >0 sobreconfianza, <0 subconfianza. Es lo que el usuario debe VER. */
    fun calibrationGap(pairs: List<CalibrationPair>): Double?

    /** RESOLUCIÓN metacognitiva (Goodman-Kruskal gamma o AUC). Independiente de la exactitud:
     *  "¿distingues tus aciertos de tus fallos?" Es el eje que HOY no existe en ningún módulo. */
    fun resolution(pairs: List<CalibrationPair>): Double?

    /** Conciencia de error: P(BELIEVED_WRONG | error). Requiere postdicción. */
    fun errorAwareness(pairs: List<CalibrationPair>): Double?
}
```

**Gates obligatorios (charter `null ≠ 0`):**
- `n < MinSampleGate` para el estadístico ⇒ **`null`/`Absent`**, jamás `0.0`. Reusar `core/scoring/MinSampleGate` (autoridad existente), no inventar umbral.
- `calibrationGap` y `resolution` exigen **intervalo de confianza**, no punto (mismo criterio que d′, aprendizaje 2026-07-20). Un sesgo de +0.08 con IC \[−0.15, +0.31] no es sobreconfianza: es ruido.
- **Corrección por azar obligatoria**: `chanceLevel = 1/k` para k opciones. Hoy `LogicConfidence.LOW = 0.35` está sobre el azar de un 4-AFC (0.25) y bajo el de un 2-AFC (0.50) — la misma escala mide cosas distintas según el ítem. La escala debe anclarse al azar del ítem, no ser un literal global.
- **Anti-gaming**: un usuario que reporta la misma confianza en todos los trials tiene `resolution` indefinida (no 0.0) y `calibrationGap` degenerado. Detector `ConstantConfidenceDetector` con `DetectorCoverage` declarada por el propio detector (patrón 2026-06-30), fail-closed a "no inferible", **nunca** acusar.

### 4.4 Feedback que enseña CÓMO (`MetacognitiveCoach`)

Copiar la forma de `DefaultLogicFeedbackEngine`: **patrón detectado → diagnóstico → siguiente movimiento concreto**, sin revelar la solución, sin etiquetar al usuario como rasgo.

| Patrón (derivado de las 3 métricas) | Diagnóstico | Siguiente movimiento |
|---|---|---|
| gap > 0, resolution alta | "Distingues bien tus aciertos de tus fallos, pero apuntas alto de forma sistemática." | "Baja un nivel tu certeza por defecto y comprueba si el desajuste se cierra." |
| gap ≈ 0, resolution baja | "Tu certeza media cuadra, pero no separa aciertos de fallos: aciertas de media por compensación." | "Antes de responder, nombra la señal concreta que te hace estar seguro. Si no la encuentras, marca certeza baja." |
| gap < 0, exactitud alta | "Aciertas más de lo que crees." | "En el próximo bloque sube un nivel cuando encuentres la regla explícita." |
| `errorAwareness` baja | "Fallaste sin notarlo." | "Tras cada respuesta, pregunta: ¿qué me haría cambiar de opinión?" |
| `POST_ERROR_ACCURACY` baja | "Tras fallar, el siguiente también falló." | "Después de un error, tómate un beat antes de responder — no aceleres para compensar." |
| n insuficiente | "Aún no hay datos para leer tu calibración." | "—" (nunca fabricar) |

### 4.5 Representación en el perfil

Añadir `CognitiveDimension.METACOGNITION` (`CognitiveTaxonomy.kt:14`). **No** derivar su `normalizedScore` del gap crudo: el mapeo natural es `1 − |gap|`, y ese es un `1 − x` alimentado por un estadístico que devuelve 0.0 con muestra vacía ⇒ **fabricaría un 100 perfecto para quien no reportó nada** (patrón exacto del aprendizaje 2026-07-15). El productor debe devolver `unavailable` con `pairCount == 0`, y `pairCount` debe ser **parámetro obligatorio**, no derivado.

### 4.6 Cableado por módulo (orden por impacto/coste)

| # | Cambio | Archivo | Coste |
|---|---|---|---|
| 1 | Sacar `CONFIDENCE_WEIGHT` del `weightedBase`; sustituir `confidenceCalibrationScore` por `CalibrationPair` emitido a la capa | `ReasoningScorer.kt:33-37,46,68` | S |
| 2 | Devolver post-error en el informe de Focus (2 `ReportLine` más) + captions honestos | `FocusSessionAssembler.kt:128-151` | XS |
| 3 | Poblar `CoachSignals` con `averageConfidence`/`actualAccuracy`/`brierScore`/`cag`; **o borrar T04–T09** si no se van a alimentar | `DecisionSessionOrchestrator.kt:172-180` | S |
| 4 | Derivar outcome binario de Bayes (`absoluteError ≤ τ`, τ declarado) y computar Brier/gap reales | `BayesArenaStateHolder.kt:77-86` | S |
| 5 | Sustituir `confidenceBias = 0.0` por el bias real de la capa; desbloquea `ActivateMetacognition` y 2 arquetipos | `MemorySessionStateHolder.kt:263` | M (exige capturar el juicio) |
| 6 | Postdicción de una pulsación tras el feedback (`ErrorAwareness`) en Logic y Language | holders + `ScoringTiming.confidence` (`LanguageSessionStateHolder.kt:101`) | M |
| 7 | Correlacionar `SubjectiveState` con el rendimiento medido y devolverlo ("¿aciertas al leer tu estado?") | `focus/domain/reporting/` | M |
| 8 | Renombrar los tres sentidos de "calibración"; alinear `EM4_CALIBRADOR` con su métrica real | `EmotionCalibrationProgressBuilder`, `EmotionExerciseCatalog.kt:194-206` | XS |
| 9 | `CognitiveDimension.METACOGNITION` + fuente fail-closed | `CognitiveTaxonomy.kt` | M |

Los pasos 1–4 no requieren **ningún dato nuevo del usuario** y cierran los tres hallazgos críticos.

### 4.7 Tests de falsación (sin ellos, la capa no cuenta)

1. **Impropiedad**: simular un usuario con creencia real `q` que reporta estratégicamente; con la regla actual su score es **mayor** que reportando honestamente; con Brier debe ser **estrictamente menor**. Si el test no se pone rojo con la regla vieja, no prueba nada.
2. **Ortogonalidad**: dos usuarios con la **misma** exactitud y calibración opuesta deben producir `calibrationGap` de signo opuesto y **el mismo** score de rendimiento. Hoy este test falla.
3. **Vacuidad** (aprendizaje 2026-07-20): forzar todas las métricas a `Absent` y contar cuántos tests caen. Si caen pocos, el suite mide poco. Cada operando de cada comparación pasa por `requirePresent`.
4. **Alcanzabilidad**: para cada plantilla/rama metacognitiva (T04–T09, `ActivateMetacognition`, cada arquetipo), un fixture que la dispare **desde el call-site de producción**, no desde el motor aislado. Es el test que hoy no existe y que habría cazado H2 y H3.
5. **Mitad discriminante**: un usuario honesto y bien calibrado NO debe disparar ningún detector anti-gaming ni recibir feedback correctivo.

---

## 5. Lo que NO hay que hacer

- **No añadir un slider de confianza a los 8 módulos.** Un juicio metacognitivo cuesta ~1–2 s por trial y **destruye la medida de TR** en todo lo RT-crítico (Focus E1–E13, Speed, Go/No-Go). La capa se cablea solo donde el TR **no** es la señal primaria: Lógica, Lenguaje, Memoria (recall), Decisiones. En RT-crítico, el juicio va **por bloque**, nunca por trial.
- **No mostrar el Brier al usuario.** Es una métrica de ingeniería. Lo que se muestra es el **sesgo con signo** ("apuntas ~12 puntos por encima de tu acierto real") y, cuando la muestra lo permite, la **resolución**.
- **No reciclar `averageConfidenceCalibration` como si ya fuera calibración.** Es 1 − MAE, contaminado por exactitud. Renombrarlo o borrarlo; mantenerlo con ese nombre es un claim falso persistido.
- **No prometer transferencia.** [Riesgo científico] La evidencia de que entrenar calibración en un dominio transfiere a decisiones vitales es **débil y específica de dominio**. El claim máximo defendible es: *"mides y ajustas tu certeza en estas tareas"*. Cualquier copy que sugiera "tomarás mejores decisiones en tu vida" excede la evidencia y viola CLAUDE.md §1.
- **No cablear al perfil antes de que la capa tenga sus gates.** Una dimensión METACOGNITION alimentada por un `1 − |gap|` sin gate de muestra fabrica un 100 para el usuario que no reportó nada. Fail-closed a `CALIBRATING` hasta que `pairCount ≥ MinSampleGate` y el IC excluya el cero.

---

**Resumen en una línea:** las piezas metacognitivas ya están escritas y testeadas en cuatro módulos distintos; el trabajo no es diseñarlas, es **conectar tres literales muertos (`confidenceBias = 0.0`, `ScoringTiming()`, 6 campos de `CoachSignals`), corregir una regla de puntuación impropia y devolver al usuario dos métricas que ya se computan y se tiran.**

---

## Anexo D. Etica, seguridad psicologica y auditoria de claims
## Auditoría de claims y seguridad psicológica — MindBoost
**Lente:** ética y seguridad psicológica · **Base:** código real + `docs/audits/modules/*.md` · **Fecha:** 2026-08-18

---

## 1. Veredicto

**BLOQUEADO para cualquier comunicación externa de "perfil cognitivo".** No por lo que la copy dice hoy —que es, en promedio, la copy más honesta que se ve en esta categoría— sino por **dónde no hay gate y qué no hay debajo**:

1. Las **tres superficies con los claims más fuertes** (hub, perfil cognitivo, coach) están **fuera del `mindboostCopySafetyScanner`**. Nada impide que la próxima frase de marketing entre sin control.
2. El módulo con mayor riesgo de claim (**emotions**) corre un **lexicón distinto y más débil**, sin ninguna cobertura de far-transfer.
3. El claim central del producto —*"progreso frente a tu propia línea base"*— **no tiene línea base implementada**.
4. La **única detección de crisis del producto vive en el coach, que no se envía**, mientras sí se envían dos superficies de texto libre sin clasificar.
5. El **aparato de seguridad psicológica existe solo en emotions**, mientras Enfoque envía inducción explícita de presión con coste por error y de espiral de fallo, sin gating alguno.

Lo bueno es real y hay que decirlo con precisión: `null ≠ 0`, `MinSampleGate`, gate de timebase, `ClaimEngine`, `SAFETY_STOP`, cifrado local. **El producto es honesto por construcción en la capa de medición y desprotegido en la capa de comunicación y de exposición.**

---

## 2. Problemas críticos

### C1 · El gate de copy no cubre las superficies que más claim emiten
`shared/build.gradle.kts:446-447` — `mindboostCopyFeatureModules = focus, focus2, memory, speed, logic, decisiones, language, creativity` (+`design`). El comentario de `:380` lo declara: **excluye `profile/presentation`**; y por omisión también `features/mindboost/presentation/` (el hub) y `features/mindboost/coach/` (todo el coach y el prompt del LLM).

Prueba de que el hueco está vivo — estos cuatro literales contienen lexemas prohibidos y **el build pasa en verde**:

| Archivo:línea | Literal | Lexema |
|---|---|---|
| `presentation/ui/MindBoostHubScreen.kt:369` | "…ni es un **diagnóstico**" | CLINICAL |
| `profile/presentation/BrainProfileUiModelBuilder.kt:35` | "No es un **diagnóstico** ni una medida **clínica**" | CLINICAL ×2 |
| `coach/safety/AICoachSafetyPolicy.kt:37` | "no promete subir tu **IQ**" | FAR_TRANSFER |
| `coach/prompt/AICoachPromptBuilder.kt:25` | "No prometas mejoras de **IQ**" | FAR_TRANSFER |

El invariante #c de `ForbiddenLexicon.kt:10-12` prohíbe nombrarlos **incluso para negarlos**, y `ForbiddenLexicon.kt:30-33` ya documentó el conflicto ("Proxy interno, no diagnostico") declarando que **gana el invariante**. Esa decisión nunca se aplicó a las superficies que no se escanean.

[Riesgo científico] El daño no es esa copy defensiva —es legalmente sensata—. Es que **el hub y la pantalla de perfil son exactamente donde nacerá el primer claim comercial** ("mejora tu concentración un 30%") y ahí no hay ninguna barrera mecánica.

**[Mejora crítica]** Meter los 3 dirs en el scanner **con una allowlist explícita de disclaimers** (literal exacto + archivo), no dejar el dir fuera. Un carve-out enumerado es revisable; un dir sin gate no.

---

### C2 · Dos lexicones divergentes, y el módulo más expuesto tiene el débil
`EmotionsTerminologyScannerTask` (`build.gradle.kts:312-316`) prohíbe 22 términos clínicos y **cero términos de far-transfer**. Pasan hoy sin fallo dentro de `features/mindboost/emotions/`:

> `inteligencia emocional` · `autocontrol` · `controlas tus emociones` · `regulacion emocional` · `en la vida real` · `IQ` · `coeficiente intelectual` · `productividad` · `multitarea` · `salud mental` · `clinico` · `sintoma` · `demencia` · `deterioro cognitivo` · `reconfigura tu cerebro`

De hecho ya hay un hit: `emotions/presentation/EmotionStrings.kt:24` dice *"…no tu **salud mental**"* — prohibido por el lexicón global, permitido por el de emotions.

[Riesgo científico] Emociones es, comercialmente, el módulo donde más tienta prometer transferencia ("controla tus emociones en el trabajo"), y es el único sin ninguna defensa contra ese lexema. El KDoc de `ForbiddenLexicon.kt:6-8` dice "keep both in sync" — **nada lo enforce**.

**[Mejora crítica]** Emotions debe correr `ForbiddenLexicon.ALL` **más** su lista clínica extendida. Un solo lexicón base, extensiones aditivas por módulo, y un test de paridad.

---

### C3 · "Progreso frente a tu propia línea base" — no hay línea base
`profile/presentation/BrainProfileUiModelBuilder.kt:32-33`:
```
TITLE    = "Tu perfil cognitivo"
SUBTITLE = "Tu progreso funcional frente a tu propia línea base."
```
Lo que existe realmente:
- `FocusProfileSource.kt:106-108` → dirección de tendencia sobre sesiones **recientes**, y solo si `trend.confident`.
- `LogicProfileSource.kt:103`, `LanguageProfileSource.kt:112`, `SpeedProfileSource.kt:166` → mismo patrón.
- Emotions: `trend = ProfileTrend.UNKNOWN` **siempre** (`emotions.md:305`) — el módulo no computa mejora longitudinal en absoluto.

[Riesgo científico] **"Línea base" es un término psicométrico con significado exacto**: una medida de referencia fijada antes del entrenamiento, contra la que se compara un post-test, idealmente con un índice de cambio fiable (RCI) que separe mejora real de ruido de medición. Lo que hay es una **pendiente móvil sobre las últimas N sesiones**, que confunde efecto de práctica, aprendizaje perceptivo de la tarea y variación de día. La copy promete un diseño pre/post que el código no implementa.

Agravante: los propios audits documentan que la "mejora" que esa pendiente capturaría es en su mayor parte efecto práctica —`focus.md`, `logic.md:269` ("entrena reconocimiento de 6 reactivos fijos"), `memory.md`, `emotions.md:304` (banco de 28 ítems memorizable)—. Es decir: **la pendiente subirá, y no querrá decir lo que la copy dice que quiere decir.**

**[Dato necesario]** Ninguna sesión almacena un baseline sellado ni un retest diferido. Para sostener el claim hace falta: primeras K sesiones como baseline inmutable + retest a 7/30 días + banda de cambio fiable. `ClaimType.MAINTAINED`/`E4_MAINTAINED` de Lenguaje ya modela exactamente eso y **nadie más lo usa**.

---

### C4 · La única detección de crisis vive en el módulo que no se envía
`coach/safety/AICoachSafetyPolicy.kt:53-59` es trabajo de primera: clasifica auto-lesión/suicidio en ES+EN con priorización sobre las demás reglas, y `CRISIS_RESPONSE:27-30` deriva a emergencias sin fingir competencia clínica. También filtra la **salida** del modelo (`OUTPUT_RED_FLAGS:68-77`: "tienes tdah", "garantiz", "como tu terapeuta", "aumentara tu iq").

El problema: **el coach está bloqueado** (sin provider ni billing). Mientras tanto **sí se envían** dos superficies de texto libre sin ninguna clasificación:
- Diario reflexivo de Decisiones — `decisiones/data/SecureDiaryRepository.kt`, sellado desde `DecisionsRootScreen`. Un diario de decisiones personales es, por diseño, donde alguien escribe lo que le pesa.
- Ideas de Creatividad — `CreativityStrings.IDEA_PLACEHOLDER = "Escribe una idea…"`.

[Inferencia] El producto invita a escribir contenido personal, lo cifra, lo persiste y **no mira nada**. No pido moderación ni análisis de contenido (violaría minimización): pido un **clasificador local, en dispositivo, sin persistencia del match**, que ante señal de crisis muestre recursos de ayuda. Cero datos salen del dispositivo.

**[Mejora crítica]** Extraer `classifyRequest`+`CRISIS_RESPONSE` a `core/safety/CrisisRouting.kt` y llamarlo en **toda** entrada de texto libre. Coste: mover ~40 líneas ya escritas y testeadas.

---

### C5 · Inducción de presión y de fallo sin ningún gating de seguridad
Verificado por grep: `OverloadDetector`, `PauseGovernor`, `EmotionSafetyLimits`, `SafetyEvent`, `SafetyProxySignals`, `SAFETY_STOP`, `safetyLoad` existen **exclusivamente** en `emotions/domain/safety/`. Fuera de emotions: cero.

Y sin embargo Enfoque envía end-to-end (`ExerciseCatalog.implemented`, 13/13):

| Ejercicio | Copy real | Qué induce |
|---|---|---|
| `E13_PRESION` (`ExerciseCatalog.kt:238-253`) | "Compara tu precisión con calma y **bajo deadline con coste por error**. Mide **cuánto te afecta la presión**." | Presión de rendimiento con castigo explícito |
| `E12_REENGANCHE` (`:225-236`) | "Continúa tras un error **sin entrar en espiral**." | Fallo repetido; el ejercicio necesita que falles |
| Speed `E8` (`SpeedStrings.kt:E8_INSTRUCTIONS`) | "La velocidad **solo cuenta** si mantienes **cero** respuestas en las señales de freno." | Todo-o-nada |

[Riesgo científico] La inducción deliberada de presión de rendimiento con coste por error es el vector canónico de **ansiedad de rendimiento**; y un paradigma diseñado para producir errores repetidos en un usuario con baja tolerancia a la frustración es una experiencia de fracaso administrada. **El equipo ya reconoció este riesgo y construyó el gobierno completo para él — en emotions.** Enfoque, con inducción más agresiva, no tiene: detector de sobrecarga, pausa forzada, salida de seguridad, ni límite de bloques consecutivos de presión.

Agrava: `focus/presentation/ui/FocusHomeScreen.kt:251` muestra **"RACHA"** — presión de adherencia diaria, en el módulo sin `PauseGovernor`, junto al ejercicio de "coste por error".

**[Mejora crítica]** Promover `emotions/domain/safety/` a `core/safety/` y aplicarlo a todo ejercicio cuyo `governingAxis == DifficultyAxis.PRESSURE` o cuyo paradigma sea `DEADLINE_PRESSURE`. Es reubicación + wiring, no diseño nuevo.

---

## 3. Auditoría de claims (a) — clasificación de lo que el código dice REALMENTE

| # | Claim real (archivo:línea) | Clase | Por qué |
|---|---|---|---|
| 1 | `SpeedStrings.HUB_SUBTITLE:20-22` "Beta: resultados orientativos; la precisión depende del dispositivo. **Sin puntuación validada aún**." | **Seguro** | Estándar de oro del repo. Declara el límite antes de que el usuario lo descubra. |
| 2 | `SpeedStrings.MVP_NOTE:15-17` "la velocidad se interpreta junto con precisión y estabilidad" | **Seguro** | Previene la lectura ingenua "más rápido = mejor". |
| 3 | Focus `plainDescription` E1-E13 ("Mide tus lapsos y cuán estable es tu tiempo de reacción") | **Seguro** | Describen la tarea y la métrica, no un beneficio vital. Modelo a copiar. |
| 4 | `EmotionFeedbackCopy.kt:112` "Esto describe tu rendimiento **en el ejercicio**, no una conclusión sobre ti ni sobre tu vida emocional real" | **Seguro** | La mejor frase de todo el producto. |
| 5 | `EmotionFeedbackCopy.kt:108-109` "no tiene muestra suficiente… Cuenta como práctica, **no como medida**" | **Seguro** | Separa práctica de medición. Debería ser transversal. |
| 6 | `CreativityStrings.HUB_SUBTITLE:15` "**Pensamiento divergente** offline. Retroalimentación funcional, sin nota ni rango." | **Exagerado** | `creativity.md:88,128`: EX02 "vende un constructo (flexibilidad) que el código no mide"; no hay originalidad ni flexibilidad, solo conteo de ideas. Honesto sobre el estatus, falso sobre el constructo. |
| 7 | `MindBoostHubScreen.kt:324` Decisiones: "**Calibración bayesiana**, triaje de reversibilidad y registro reflexivo" | **Débil / necesita-validación** | `decisiones.md:163` triaje = 1.7/10, transferencia **nula**, "mide azar"; la calibración se recoge y se tira (`decisiones.md:109`). |
| 8 | `BrainProfileUiModelBuilder.kt:33` "progreso funcional frente a tu propia **línea base**" | **Necesita-validación** | C3: no hay baseline ni retest ni RCI. |
| 9 | `EmotionStrings.kt:99` "**Rango oficial** desbloqueado" | **Débil** | "Oficial" implica autoridad sobre 20 trials/sesión cuyas métricas gobernantes caen bajo su n mínimo (`emotions.md:303`). |
| 10 | `ClaimEngine.kt:27` "Mejoras en este tipo de ejercicio." | **Seguro (excepcional)** | Único claim del repo **gateado por evidencia** (`DefaultClaimEngine.kt:20-33`): permitido iff estado ≥ mínimo; en E0 ningún claim de mejora; bloqueado devuelve la alternativa honesta más fuerte. Además acota el alcance ("en este tipo de ejercicio"). |
| 11 | `AICoachStrings.ELITE_ENTRY:13` "**analiza tu progreso** y te sugiere qué practicar. Incluido en Elite." | **Necesita-validación** | Se vende, mientras `RECOMMENDATION_PENDING:42` admite que aplicar la sugerencia "todavía no está disponible". Desalineación expectativa/entrega en la superficie de pago. |
| 12 | `AICoachSafetyPolicy.IQ_REFUSAL:37`, `CRISIS_RESPONSE:27` | **Seguro pero prohibido por invariante** | Nombran `IQ`; sobreviven solo porque el dir no se escanea (C1). |
| 13 | `EmotionStrings.kt:21-24` "entrenamiento funcional, no evaluación **clínica**… no tu **salud mental**" | **Prohibido (pasa por gate débil)** | C2. |
| 14 | `MindBoostHubScreen.kt:368-370` "no promete subir tu nota ni es un **diagnóstico**" | **Prohibido (fuera de gate)** | C1. La primera mitad ("no promete subir tu nota") es excelente. |

**Nada clasificado como "peligroso".** Ese es el logro real: no hay un solo claim de curación, de IQ garantizado ni de neurociencia falsa en la copy enviada.

### Reescrituras — más fuertes comercialmente, sin mentir ni nombrar lexemas prohibidos

```diff
# profile/presentation/BrainProfileUiModelBuilder.kt:33
- "Tu progreso funcional frente a tu propia línea base."
+ "Tu rendimiento en las tareas de MindBoost, sesión a sesión.
+  Solo mostramos lo que hemos medido lo suficiente."

# profile/presentation/BrainProfileUiModelBuilder.kt:35
- "Perfil funcional de práctica, orientativo y preliminar. No es un diagnóstico ni una medida clínica."
+ "Perfil funcional de práctica, orientativo y preliminar. Describe tu rendimiento
+  en estas tareas, no tus capacidades fuera de la app."

# presentation/ui/MindBoostHubScreen.kt:368-370
- "…frente a tu propia línea base — no promete subir tu nota ni es un diagnóstico."
+ "…frente a tus propias sesiones anteriores. Medimos lo que pasa dentro de la app
+  y no prometemos resultados fuera de ella."

# coach/safety/AICoachSafetyPolicy.kt:37
- "MindBoost no promete subir tu IQ ni garantiza mejoras…"
+ "Ningún entrenamiento cognitivo puede prometerte eso con honestidad, y nosotros
+  no lo hacemos. Lo que sí te doy: cómo evoluciona tu rendimiento en cada tarea que practicas."

# creativity/presentation/CreativityStrings.kt:15  (hasta que se mida originalidad/flexibilidad)
- "Pensamiento divergente offline. Retroalimentación funcional, sin nota ni rango."
+ "Práctica de fluidez de ideas, offline. Contamos cuántas ideas distintas produces;
+  todavía no medimos su originalidad."

# emotions/presentation/EmotionStrings.kt:99
- "Rango oficial desbloqueado"
+ "Ya tenemos muestra suficiente para una lectura estable"

# emotions/presentation/EmotionStrings.kt:24
- "…no tu salud mental."
+ "…no cómo estás fuera de la app."
```

Todas son **más vendibles**, no menos: "solo mostramos lo que hemos medido lo suficiente" es un diferenciador; "línea base" es una palabra que el usuario no entiende y que además no es cierta.

---

## 4. Problemas medios

- **M1 · `ClaimEngine` confinado a Lenguaje.** `language/domain/claim/DefaultClaimEngine.kt` es el gobierno de claims que el producto entero necesita (escalera E0→E4, caveats automáticos `"Aún no validado tras una pausa (retest 7/30 días)"`). Fuera de Lenguaje, cualquier copy de progreso se emite sin pasar por ningún juez. **Debe subir a `core/claims/`.**
- **M2 · Racha sin contrapeso** (`FocusHomeScreen.kt:251`): presión de adherencia en el módulo sin `PauseGovernor`. Mínimo: que una pausa deliberada no rompa la racha (`EmotionStrings.kt:23` ya dice *"Una pausa estratégica también cuenta como regulación"* — aplicar la misma doctrina).
- **M3 · Emotions: 6 ejercicios etiquetados de emoción, 4 con afecto decorativo** (`emotions.md:307`). El módulo se presenta como entrenamiento de regulación aplicado al rendimiento y en su mayoría es control cognitivo con etiqueta afectiva. No es un claim prohibido; es un claim **débil** que la copy del hub (`MindBoostHubScreen.kt:329`) sostiene sin matizar.
- **M4 · "Perfil cognitivo" con dimensiones en `—`.** El diseño es correcto (`ProductMotorUiMapper.kt:56` fila segura, `EM_DASH` nunca `0`), pero el usuario ve un radar mayoritariamente vacío llamado "**Tu perfil cognitivo**". [Inferencia] Un perfil incompleto y etiquetado como cognitivo invita a la autointerpretación deficitaria ("estoy vacío en memoria"). El texto de progreso (`:82-84`) mitiga; el título no.

---

## 5. Problemas menores

- `AICoachStrings.MEMORY_LOCAL_ONLY:47` "**Por ahora** tu resumen se guarda solo en este dispositivo" — el "por ahora" anticipa un cambio de alcance de datos sin consentimiento renovado. Quitarlo o convertirlo en compromiso.
- `AICoachSafetyPolicy.fold()` solo pliega acentos: `su1cid`, `s-u-i-c-i-d-i-o` o separaciones con emoji evaden el clasificador de crisis.
- `EmotionContent.kt:73` "Esto va a salir mal" como estímulo — correcto dentro del marco de inducción leve documentado; solo asegurar que nunca aparezca fuera de un ejercicio con `SAFETY_STOP` disponible.

---

## 6. Tests que faltan

1. **Paridad de lexicones** — `ForbiddenLexicon.ALL` ⊆ lista del task Gradle (`build.gradle.kts:399+`). Hoy el KDoc dice "keep both in sync" y **nada falla si divergen**.
2. **Cobertura del scanner** — test que enumere todo dir bajo `features/mindboost/` con literales en español visibles al usuario y falle si no está en `mindboostCopyFeatureModules` ni en una allowlist justificada. Cierra C1 de forma permanente.
3. **Allowlist de disclaimers** — los N literales con lexema prohibido permitidos son **exactamente** estos; cualquier otro rompe.
4. **Red-team de `classifyRequest`** — variantes ofuscadas de crisis (leet, espaciado, sin acentos, emoji intercalado) deben clasificar CRISIS. Mitad discriminante obligatoria: un mensaje honesto de entrenamiento NO debe clasificar crisis.
5. **Gating de presión** — una vez exista `core/safety`, test que falle si un `ExerciseDefinition` con `governingAxis == PRESSURE` o `paradigm == DEADLINE_PRESSURE` está en `implemented` sin overload gate.
6. **Claim/implementación** — test que falle si la copy del perfil contiene "línea base" mientras no exista un baseline sellado y un retest.

---

## 7. Seguridad psicológica (b) — resumen de riesgos

| Riesgo | Estado | Evidencia |
|---|---|---|
| Sobreexigencia / no poder parar | **Cubierto solo en emotions** | `PauseGovernor`, `EmotionSafetyLimits`, `SAFETY_STOP`, `"Puedes pausar o salir en cualquier momento"` (`EmotionStrings.kt:22`). Cero equivalente en focus/speed/memory/logic. |
| Ansiedad de rendimiento (deadline + castigo) | **Descubierto** | E13 con "coste por error", Speed E8 todo-o-nada, sin overload detection. |
| Frustración por fallo inducido | **Descubierto** | E12 requiere errores; sin límite de bloques consecutivos ni salida. |
| Inducción emocional | **Bien gobernado** | Intensidad 1-5, check-in, solo inducción leve, `SAFETY_STOP` no cuenta como sesión (`EmotionTerminalSemantics.kt:74`). |
| Límite no-clínico | **Correcto en copy, sin gate en 3 dirs** | C1/C2. |
| Crisis / auto-lesión | **Solo en módulo no enviado** | C4. |
| Autointerpretación deficitaria | **Parcial** | `null≠0` y `—` protegen; el título "Tu perfil cognitivo" sobre un radar vacío no. |
| Presión de adherencia (racha) | **Descubierto** | M2. |
| Sobrecarga de datos personales | **Bien** | Cifrado local, `AccountBound` eraser, consentimiento revocable del coach. |

**Conclusión (b):** el equipo sabe exactamente cómo se construye la seguridad psicológica —lo demostró en emotions— y **no la ha extendido al módulo que más presión induce**. Es un problema de alcance, no de capacidad.

---

## 8. (c) Claims que MindBoost PODRÍA hacer HOY con honestidad y no hace

Estos son verificables en el código de hoy, no requieren investigación nueva, y son diferenciadores brutales frente a la categoría:

1. **"Si tu móvil no puede medir tu tiempo de reacción con fiabilidad, te lo decimos y esa sesión no cuenta para tu perfil."**
   Respaldo: `TimebaseClassifier`, gate `CHOREOGRAPHER_FRAME + NANOS_NATIVE` en `SpeedProfileSource`, `FrameStatsCalculator`, `FRAME_JITTER_DETECTED`. **Ninguna app de esta categoría hace esto.** Hoy solo se insinúa ("la precisión depende del dispositivo").

2. **"Nunca verás un número que no podamos sostener. Si no hay datos suficientes, verás un guion, no un cero."**
   Respaldo: `MinSampleGate`, invariante `null != 0` (`CognitiveSessionScore.kt:28`), `EM_DASH` en el perfil. Es el compromiso más vendible del producto y **no aparece en ninguna superficie de usuario**.

3. **"Detectamos cuando alguien intenta engañar al sistema — y en vez de premiarlo, retenemos el resultado."**
   Respaldo: `core/antigaming` con 11 detectores y suite red-team; `SpeedAntiGaming`; `SMALL_UNVALIDATED` bloqueando el perfil.

4. **"Te decimos lo que todavía NO podemos afirmar."**
   Respaldo: `DefaultClaimEngine` — escalera E0→E4 con caveats explícitos ("Aún no validado tras una pausa"). Convertir la escalera en **feature visible** ("Nivel de evidencia de esta afirmación: 2/4") es un diferenciador de categoría entera. Hoy está enterrada en un módulo.

5. **"Tus datos cognitivos se cifran en tu dispositivo, y cuando borras tu cuenta se borran de verdad — o el borrado falla en vez de mentirte."**
   Respaldo: `SecureKeyValueStore` AES-256-GCM + Keystore, `AccountBound` eraser fail-closed, `LocalEraseBlocked`.

6. **"Una pausa cuenta como entrenamiento."**
   Respaldo: `EmotionStrings.kt:23`. Generalizarlo al producto: antídoto directo contra la presión de racha (M2), y **verdadero**.

7. **"Medimos tu rendimiento en cada tarea sesión a sesión, y solo lo llamamos mejora cuando el dato lo aguanta."**
   Sustituye honestamente al claim de "línea base" hasta que exista baseline + retest.

---

## 9. Fixes requeridos (orden de ejecución)

| # | Fix | Bloquea |
|---|---|---|
| **F1** | Añadir `presentation/`, `profile/presentation/`, `coach/` al `mindboostCopySafetyScanner` con allowlist enumerada de disclaimers. Aplicar las reescrituras de §3. | Release |
| **F2** | Unificar lexicones: emotions corre `ForbiddenLexicon.ALL` + su extensión clínica. Test de paridad. | Release |
| **F3** | Reescribir el subtítulo del perfil: eliminar "línea base" hasta que exista baseline sellado + retest. | Release |
| **F4** | `core/safety/CrisisRouting.kt` (extraer de `AICoachSafetyPolicy`) aplicado a diario de Decisiones y a Creatividad. + red-team de ofuscación. | Release |
| **F5** | Promover `emotions/domain/safety/` a `core/safety/`; aplicar a E12/E13 y Speed E8. | Release |
| **F6** | Reescribir copy de Creatividad y de Decisiones al constructo realmente medido (§3 #6, #7). | Release |
| **F7** | Promover `ClaimEngine` a `core/claims/`; toda copy de progreso pasa por él. | Siguiente ciclo |
| **F8** | Racha: pausa deliberada no rompe racha. | Siguiente ciclo |
| **F9** | Superficializar los 7 claims honestos de §8 en onboarding y store listing. | Siguiente ciclo (valor comercial alto) |

---

## 10. Criterios de aprobación

Levanto el bloqueo cuando **todo** lo siguiente sea cierto y verificable con un gate, no con una narrativa:

1. `mindboostCopySafetyScanner` cubre **toda** superficie de copy de usuario de MindBoost; los carve-outs son una allowlist enumerada, no dirs ausentes. Test de cobertura verde.
2. Un solo lexicón base para todos los módulos; test de paridad verde.
3. La copy del perfil **no** afirma comparación con una línea base, o existe baseline sellado + retest + banda de cambio fiable.
4. Enrutamiento de crisis vivo en toda entrada de texto libre enviada, con red-team de ofuscación verde.
5. Todo ejercicio con `governingAxis == PRESSURE` o `DEADLINE_PRESSURE` tiene overload gate + salida de seguridad, con test que lo enforce.
6. Creatividad y Decisiones nombran el constructo que **miden**, no el que aspiran a medir.
7. Ningún claim de progreso fuera de un `ClaimEngine` con estado de evidencia.

**Asunciones declaradas:** (i) el coach no se envía en el build actual — si se envía, F4 pasa de crítico a resuelto para esa superficie pero el hueco de diario/creatividad sigue; (ii) las notas de los audits por módulo (`docs/audits/modules/*.md`) se toman como dadas y no se re-verifican aquí; (iii) no he verificado la copy de `androidApp/` ni de la store listing — fuera del alcance de esta lente y **fuera de todo scanner**, lo cual es un hueco a auditar por separado.

---

## Anexo E. Adherencia y UX cognitiva
# Auditoría de adherencia y UX cognitiva — MindBoost
> Lente J (comportamiento/adherencia) + K (UX cognitiva). Base: código real + auditorías por módulo ya producidas. Todo lo que sigue está verificado en fuente; las citas son archivo:línea.

**Respuesta directa a la pregunta:** hoy **no**. La forma del ejercicio interfiere con el entrenamiento en cuatro puntos verificables: (1) el contrato de tiempo está roto, (2) la sesión es un bloque monolítico de hasta 160 trials del mismo ejercicio, (3) toda la maquinaria de recuperación/periodización es texto, no conducta, y (4) la única recompensa cableada (racha) premia aparecer, no medir. La variedad percibida (8 pistas) es ~3× mayor que la variedad real de paradigmas, así que la novedad colapsa antes de que exista serie temporal suficiente para decir nada.

---

## 1. Problema de usuario

El usuario quiere saber si está mejorando y quiere que entrenar cueste menos que abrir Instagram. Hoy se encuentra con:

- **Coste de decisión antes del primer trial: 4–5 elecciones.** Hub con 8 `TrackCard` + tarjeta de plan (`MindBoostHubScreen.kt:288-342`) → raíz del módulo → menú de 13 ejercicios en Enfoque → intro → plantilla de duración → sesión. Cada elección es una oportunidad de abandono y ninguna la puede tomar mejor el usuario que el sistema (el sistema tiene el perfil; el usuario no).
- **Contrato de tiempo falso.** `SessionTemplate.estimatedTrialCount` usa `averageTrialMs = 3_800` calibrado para E1 y el propio código lo confiesa: *"the trial count it yields for E2 does not match the template's advertised minutes"* (`SessionTemplate.kt:28-41`). "Estándar 10 min" no dura 10 minutos en 12 de los 13 ejercicios. **[Mejora crítica]** Romper el contrato de tiempo es el predictor de abandono más barato de arreglar y el más caro de ignorar.
- **Monotonía intra-sesión estructural.** El runner ejecuta **un** bloque: `blocks = listOf(block)` (`FocusSessionStateHolder.kt:394`), con `trialCount` entre 24 y 160 (`SessionTemplate.kt:44-45`). Es decir: hasta 160 pulsaciones consecutivas del MISMO paradigma. En E1 eso es correcto como medida (el decremento de vigilancia es la señal) y letal como producto.
- **El planificador ya sabe hacerlo mejor y nadie lo escucha.** `DefaultDailyPlanGenerator` produce 1–4 bloques según plantilla (`DailyFocusPlan.kt:153-158`), pero `FocusHomeStateHolder` solo lee `plan.blocks.firstOrNull()` como recomendación (`FocusHomeStateHolder.kt:70`). Los bloques 2–4 se calculan y se tiran.
- **La fatiga no existe como conducta.** `volumeMultiplier`, `capDifficulty` e `includeProbe` (`DailyFocusPlan.kt:27-37`) **no tienen ni un solo consumidor en `commonMain`** (grep: 0 call-sites fuera de su propia declaración). El gate de recuperación produce un banner (`FocusHomeScreen.kt:136-138, 157`) y nada más: si el sistema dice "descanso recomendado", el usuario puede entrar igual, a volumen completo, sin cap de dificultad. **[Mejora crítica]**
- **Sin check-in de estado.** `SubjectiveState` (sueño/cafeína/ánimo/hora) existe en modelo, DTO y persistencia, pero **ninguna UI lo recoge**: `start(template, subjectiveState = null)` (`FocusSessionStateHolder.kt:164`) se invoca siempre con el default. Consecuencia conductual: un mal día se le devuelve al usuario como "has empeorado". Consecuencia científica: el confounder más grande de RT (hora + sueño) queda sin registrar. **[Dato necesario]**

---

## 2. Promesa de producto

> **"Diez minutos al día. Un número al mes."**

El compromiso operativo, en tres cláusulas que el sistema debe poder cumplir:

1. **Cada sesión dura lo que dice** (±10 %), y se puede abandonar sin perder lo hecho.
2. **La sesión diaria no te evalúa.** Te entrena y recoge datos. Nunca verás un número de capacidad al terminar un día.
3. **El progreso frente a tu propia línea base se dice una vez al mes**, con la misma prueba, y solo si el cambio supera el ruido de tu propia medición. Si no lo supera, se dice: *"todavía no distinguible de la práctica"*.

La cláusula 3 es la única defensa contra pseudoprogreso que sobrevive al contacto con un producto de consumo. Y es *vendible*: la escasez del número lo hace creíble.

---

## 3. Flujo

**Regla dura: cero decisiones antes del primer trial en la ruta diaria.**

```
Home ─┬─► [ENTRENAR HOY · 8 min]  ← 1 tap. Sin menú. Sin elegir módulo.
      │        │
      │        ├─ Check-in 8 s (3 taps, saltable): sueño ↓/=/↑ · energía 1-5 · (hora auto)
      │        ├─ Bloque ANCLA  (~2,5 min)  ← fijo 14 días, alimenta la serie temporal
      │        ├─ Bloque ROTATIVO A (~2,5 min) ← módulo distinto, elegido por el planner
      │        ├─ Bloque ROTATIVO B (~2,5 min) ← módulo distinto de A
      │        └─ Cierre: qué entrenaste + calidad de datos. CERO número de capacidad.
      │
      └─► "Explorar" (menú actual de 8 pistas)  ← ruta secundaria, para quien quiere elegir
```

- El menú de 13 ejercicios de Enfoque y las 8 `TrackCard` **dejan de ser la puerta de entrada** y pasan a ser modo exploración. No se borran: se despriorizan.
- Los bloques rotativos los elige `TransversalDailyPlanner` (`TransversalDailyPlanner.kt:44-55`), que ya prioriza CALIBRATING → EMERGING → ESTABLISHED. Está bien diseñado y hoy solo pinta una tarjeta clicable (`MindBoostHubScreen.kt:288-300`); debe pasar a **componer la sesión**, no a sugerir un destino.
- **El ancla no rota.** Es la concesión no negociable a la medición: sin un bloque estable durante ≥14 días no hay N por constructo y el perfil se queda en CALIBRATING para siempre. La rotación se aplica a los 2 bloques variables, que es donde el aburrimiento muerde.

**Tensión explícita y su resolución.** Rotación (adherencia) y N por constructo (medición) tiran en direcciones opuestas. Con 20 trials/sesión, Emociones ya devuelve `unavailable` casi siempre (`EmotionSessionStateHolder.kt:410-411`). Si rotamos dentro de la sesión sin presupuesto, el problema se generaliza a todos los módulos. Resolución: **presupuesto semanal de N por constructo**, no por sesión. El ancla aporta ~40 trials/día × 5 días = 200/semana a un constructo; los rotativos aportan variedad y datos de baja resolución. Ningún constructo se declara medido con lo que aportan los rotativos.

---

## 4. Bucle de retención

```
Check-in (8 s) → 3 bloques cortos → Cierre de PROCESO → señal de constancia
      ▲                                                        │
      └──────── recordatorio anclado a la hora del check-in ◄──┘
                          │
        (semanal) sesión de evaluación con probes NO entrenados
                          │
        (mensual) batería fija → único momento donde aparece un número
```

Refuerzos permitidos (todos de **proceso**, ninguno de capacidad):

| Refuerzo | Qué mide | Por qué no infla pseudoprogreso |
|---|---|---|
| **Días válidos** | sesión elegible para perfil **o** descanso recomendado por el gate | No se puede farmear con sesiones basura: una sesión no elegible no cuenta |
| **Cobertura de dominios** (anillo de 8) | dominios con datos frescos | Premia amplitud, no puntuación |
| **Calidad de datos** | % de bloques con métricas `confident` | Premia atender de verdad; alinea usuario y medición |
| **Racha que sobrevive al descanso** | constancia | Ver abajo — es un bug de diseño hoy |

**[Mejora crítica] La racha actual pelea contra la política de recuperación.** `estimateStreak` cuenta días distintos con sesión (`FocusHomeStateHolder.kt:103-111`) y se pinta junto al banner "Descanso recomendado" (`FocusHomeStateHolder.kt:81` vs `:92`). El sistema le pide al usuario que descanse y le castiga por hacerlo. Un día de descanso emitido por `RecoveryGate.RestRecommended` (`RecoveryPolicy.kt:38-41`) **debe contar como día válido**, y decirlo: *"Descanso registrado. Tu racha sigue."*

**Prohibiciones de refuerzo (regla dura):**
- Nada de trofeos por subir de nivel de dificultad. El staircase de Velocidad aprieta el deadline; premiar eso es premiar tolerancia al cronómetro sobre tarea trivial (riesgo ya identificado en la auditoría de Velocidad). **[Riesgo científico]**
- Nada de "% de acierto" al terminar. En Memoria el cierre muestra acierto sobre un reconocimiento trivial (`MemorySessionStateHolder`: recall multi-select, un solo trial, `delayedAccuracy = null` en `:164`): es un número alto que no significa nada y que el usuario leerá como capacidad.
- Nada de índices compuestos en el cierre diario. `SUSTAINED_FOCUS_INDEX` ya está contenido tras flag (`FocusHomeStateHolder.kt:39-55`, MB-ENG-0007). **Mantener esa contención y extender el criterio al resto de módulos.**

---

## 5. MVP

Cinco cambios. Ninguno requiere investigación nueva; todos son cableado de cosas que ya existen.

| # | Cambio | Archivo | Esfuerzo |
|---|---|---|---|
| M1 | **Botón único "Entrenar hoy"** que compone 3 bloques desde `TransversalDailyPlanner` y los ejecuta seguidos | `MindBoostHubScreen.kt` + runner multi-bloque | M |
| M2 | **Runner multi-bloque**: consumir `plan.blocks` en vez de `listOf(block)` | `FocusSessionStateHolder.kt:394`, `:226-227` | M |
| M3 | **Cablear volumen y cap**: `trialCount = (base × plan.volumeMultiplier)`, `capDifficulty` respetado; `RestRecommended` ⇒ ruta de recuperación, no la normal | `FocusSessionStateHolder.kt:226`, `DailyFocusPlan.kt:27-37` | S |
| M4 | **`averageTrialMs` por ejercicio** (no la constante E1) para que la duración anunciada sea la real | `SessionTemplate.kt:28`, `ExerciseCatalog` | S |
| M5 | **Check-in de 8 s** que rellena `SubjectiveState` y se pasa a `start(...)` | `FocusSessionStateHolder.kt:164` + pantalla nueva | S |

M3 y M4 son los de mayor retorno por línea: convierten "la app dice que descanses" en "la app te da una sesión de recuperación" y "10 min" en 10 min.

**Fuera del MVP a propósito:** arreglar E8/E9/E10 (métricas que no son función de la conducta), banco generativo de Lógica, recall por producción en Memoria. Son correcciones de validez, no de adherencia — pero **bloquean la sección 7**: hasta que existan, ningún número mensual puede publicarse para esos módulos.

---

## 6. Versión premium

Frontera: **gratis todo lo que protege al usuario; de pago todo lo que profundiza.**

| Gratis | Premium |
|---|---|
| Sesión diaria (3 bloques, ~8 min) | **Sesión profunda** 20–25 min con bloques largos y adaptación fina |
| Gate de recuperación y descansos (**nunca de pago**) | **Evaluación mensual completa** (batería fija + intervalo de cambio fiable) |
| Perfil en CALIBRATING/EMERGING | **Protocolos de transferencia** (vocabulario, estudio) — ya definidos en Memoria, sin cablear |
| Historial 30 días | Historial completo + exportación |
| — | Ventana horaria personalizada (a qué hora rindes mejor, derivado del check-in de M5) |

**Regla dura:** la seguridad (fatiga, sobreentrenamiento, descanso) jamás se monetiza. Vender el freno es vender el daño.
**Regla dura 2:** premium no compra un número mejor. Compra más resolución y más profundidad. Si premium desbloqueara "tu puntuación real", el producto se convierte en el problema.

---

## 7. Métrica principal

**Adherencia Medible 4×4: % de usuarios que completan ≥4 sesiones *válidas* en cada una de 4 semanas consecutivas.**

"Válida" = sesión que produce al menos un bloque con métricas `confident` y elegible para perfil (el pipeline ya distingue elegible/no elegible; `ProfileSessionEligibility` y el gate de timebase existen). No cuenta abrir la app, ni una sesión abandonada, ni una sesión con timebase no autoritativo.

Por qué esta y no retención D30: retención se puede subir con notificaciones agresivas y confeti, y ese movimiento **destruye** la métrica principal (más sesiones basura ⇒ menos sesiones válidas por semana). Es la única definición que hace imposible ganar adherencia sacrificando medición.

---

## 8. Métricas secundarias

**Fricción**
- Mediana de tiempo desde abrir la app hasta el primer trial. Objetivo < 20 s. [Hoy: no instrumentado — **[Dato necesario]**]
- Taps hasta el primer trial. Objetivo ≤ 2. Hoy 4–5.
- Tasa de abandono en la pantalla de intro/plantilla.

**Fatiga**
- Abandono por posición de bloque (¿cae en el 2.º o en el 3.º?) y por posición de trial dentro del bloque — la curva dice dónde cortar de verdad.
- Delta intra-sesión bloque 3 vs bloque 1 (precisión y RT-CV). Si es negativo sistemáticamente, la sesión es demasiado larga, sin importar lo que digan los minutos.
- Cumplimiento de la recomendación de descanso (hoy inobservable: el gate no bloquea nada).

**Aburrimiento / rotación**
- Entropía de rotación de módulos por usuario/semana.
- **Paradigmas distintos vistos**, no ejercicios distintos. Emociones: 6 entradas = 3 paradigmas; Velocidad: 4 ejercicios ≈ 1 constructo. El menú promete el doble de variedad de la que hay.
- Curva de novedad: sesiones hasta la primera racha de 3 días sin abrir.

**Falsa mejora (las que más importan)**
- **Tasa de mejora reclamada vs confirmada por probe no entrenado.** Objetivo: no reclamar nunca sin confirmar. Hoy `includeProbe` no se consume ⇒ el denominador no existe.
- % de sesiones con métricas gobernantes `unavailable` (potencia de medición). En Emociones es alto por diseño (20 trials/sesión).
- Días hasta salir de CALIBRATING por dimensión — mide si la rotación está matando el N.
- % de contenido repetido por usuario (bancos finitos: 6 ítems en Lógica, 28 en separación emocional, ~113 palabras en Memoria). Es el termómetro directo del pseudoprogreso por memorización.

---

## 9. Modos de fallo

| # | Fallo | Mecanismo | Mitigación |
|---|---|---|---|
| F1 | **Racha vs descanso** | El refuerzo castiga la conducta que el gate recomienda (`FocusHomeStateHolder.kt:81` vs `:92`) | Descanso recomendado = día válido. Test que lo fije |
| F2 | **Rotación mata el N** | Variedad ⇒ pocos trials por constructo ⇒ todo `unavailable` (caso Emociones) | Bloque ancla fijo 14 días + presupuesto semanal de N por constructo |
| F3 | **Contrato de tiempo roto** | `averageTrialMs` E1 para 13 ejercicios (`SessionTemplate.kt:36-41`) | M4 + barra de progreso por trials restantes, no por reloj |
| F4 | **Fatiga invisible entre módulos** | `DefaultRecoveryPolicy` solo ve historia de Enfoque (`RecoveryPolicy.kt:29-35`); `MemoryRecoveryPolicy`/`PlateauDetector` solo se inyectan en `MemoryGraph.kt:104,107` y nunca se llaman; el `FatigueGate` de Lenguaje tiene 0 call-sites | Registro transversal de carga (§11 I3) |
| F5 | **Pseudoprogreso por banco finito** | 6 ítems fijos en Lógica (`LogicContentProvider.kt:29`), 28 en separación, ~113 palabras en Memoria | Ningún módulo con banco finito entra en el número mensual hasta tener holdout + generación |
| F6 | **Falsa mejora por dificultad** | El usuario sube de nivel de deadline y lo lee como capacidad | El nivel de dificultad nunca es recompensa visible; solo instrumento |
| F7 | **Culpar al usuario por un mal día** | Sin `SubjectiveState`, el sistema no puede decir "esto es sueño, no capacidad" | M5 + copy de atribución (§10) |
| F8 | **Notificaciones que generan sesiones basura** | Empuja a abrir, no a entrenar | La notificación solo existe si el día es entrenable según el gate; y la métrica principal (§7) penaliza el atajo |
| F9 | **Sesión profunda comida por la retención** | Presión de producto por acortar todo a 5 min | Regla dura: la sesión profunda no se acorta. Si no cabe, se salta ese día |
| F10 | **Métricas espurias en el cierre** | E8/E9/E10 reportan números que no son función de la conducta | Ocultas hasta rediseño. Ya existe el precedente de contención por flag (MB-ENG-0007) |

---

## 10. Copy de UX

**Entrada (una sola frase, un solo botón)**
> **Entrenar hoy · 8 min**
> Tres bloques cortos. Puedes parar cuando quieras.

**Check-in (8 s, saltable)**
> ¿Cómo llegas hoy?
> Sueño: menos de lo normal · lo normal · más de lo normal
> Energía: 1 — 5
> *Esto no puntúa. Sirve para que tus resultados se comparen entre días parecidos.*

**Transición entre bloques**
> Bloque 2 de 3 · Cambio de tarea. Respira una vez.

**Cierre diario (proceso, nunca capacidad)**
> Sesión completa. Hoy entrenaste **atención sostenida** y **memoria de trabajo**.
> Datos utilizables: **3 de 3 bloques**.
> Tu progreso frente a tu propia línea base se revisa el **día 30**.

**Cierre con datos insuficientes**
> Sesión registrada. Esta vez no hubo datos suficientes para medir — pasa, y no resta.

**Descanso recomendado**
> **Hoy toca descansar.** Tus últimos registros muestran más variabilidad de lo habitual.
> Descansar cuenta como día de entrenamiento: tu constancia sigue intacta.
> [ Registrar descanso ] · [ Sesión de recuperación · 4 min ]

**Día flojo con check-in bajo**
> Hoy has dormido menos de lo habitual. Tus tiempos suelen subir en esos días — es información, no un retroceso.

**Evaluación semanal**
> **Revisión semanal · 6 min.** Ejercicios que *no* has entrenado esta semana.
> Es la única forma de distinguir "he mejorado" de "me he acostumbrado a este ejercicio".

**Evaluación mensual — cambio confirmado**
> Frente a tu línea base de hace 30 días, tu **estabilidad de atención** ha cambiado más de lo que explica tu propia variabilidad. Es un cambio medible en esta práctica. Orientativo.

**Evaluación mensual — sin cambio confirmado**
> Este mes no hay cambio distinguible de la práctica. Tus datos siguen acumulándose; el perfil necesita más semanas para separar señal de ruido.

**Ejercicio con medición pendiente (E8/E9/E10, Lógica, Memoria hoy)**
> Ejercicio en **modo práctica**: entrena, pero todavía no alimenta tu perfil.

---

## 11. Notas de implementación

### I1 · Runner multi-bloque (desbloquea todo lo demás)
```kotlin
// FocusSessionStateHolder.kt:226 — hoy
val trialCount = template.estimatedTrialCount()
val spec = BlockSpec(targetTrialCount = trialCount, mode = Mode.TRAINING)
// ... :394
blocks = listOf(block)

// propuesto
val budget = plan.blocks.map { b ->
    BlockSpec(
        targetTrialCount = (template.trialsFor(b.exerciseId) * plan.volumeMultiplier).toInt(),
        mode = b.mode,
    )
}
// ejecuta secuencialmente, con transición explícita entre bloques y
// abandono parcial permitido: los bloques completados se sellan igual.
blocks = completedBlocks   // no listOf(block)
```
Criterios de aceptación: (a) abandonar en el bloque 2 sella el bloque 1 como evidencia válida; (b) `volumeMultiplier = 0.7` produce ~70 % de trials; (c) `capDifficulty = true` impide que el motor adaptativo suba de nivel en ese día.

### I2 · Duración honesta por ejercicio
```kotlin
// SessionTemplate.kt:28 — averageTrialMs deja de ser una constante E1
fun trialsFor(id: ExerciseId): Int =
    (coreMs / ExerciseCatalog.definition(id).averageTrialMs).toInt().coerceIn(MIN_TRIALS, MAX_TRIALS)
```
Bump de `TEMPLATE_VERSION` (`SessionTemplate.kt:41`) — el KDoc ya obliga a ello. Test: para cada ejercicio implementado, duración simulada ∈ [0,9, 1,1] × minutos anunciados.

### I3 · Registro transversal de carga (`core/session/TrainingLoadLedger`)
Hoy la fatiga es ciega entre módulos: `DefaultRecoveryPolicy` solo lee historia de Enfoque (`RecoveryPolicy.kt:29-35`); `MemoryRecoveryPolicy` y `PlateauDetector` se construyen en `MemoryGraph.kt:104,107` y **no se invocan desde ninguna sesión**; el `FatigueGate` de Lenguaje no tiene call-site.

```kotlin
data class DayLoad(val dayEpoch: Long, val blocks: Int, val trials: Int, val minutes: Int)
// SOLO conteos y timestamps. Ni scores, ni normalizados, ni nada con forma de contribución de perfil.
```
**Frontera obligatoria:** este registro vive fuera de `profile.*` y no puede emitir ningún campo con forma de `normalizedScore`/`confidence`/`dataSufficiency` — el escáner de frontera de evidencia existente lo verificaría. Es carga, no capacidad.
Regla de fatiga cruzada: >6 días consecutivos con sesión ⇒ día 7 forzado a recuperación (volumen 0,5, sin adaptación al alza).

### I4 · Semántica de racha
```kotlin
// FocusHomeStateHolder.kt:103 — estimateStreak cuenta días con sesión
// propuesto: un día es válido si (sesión elegible) O (RecoveryGate.RestRecommended registrado)
```
Test discriminante obligatorio: un usuario con 3 días de entrenamiento + 1 descanso recomendado + 2 días de entrenamiento tiene racha 6, **y** un usuario con 6 sesiones abandonadas sin bloque completo tiene racha 0. Sin la segunda mitad, el arreglo solo infla la racha.

### I5 · Check-in y `SubjectiveState`
Pantalla de 3 taps → `start(template, SubjectiveState(sleepHours, caffeine, mood1to5, localHour))`. El modelo, el DTO y el mapper ya existen (`FocusDtos.kt:39`, `FocusPersistenceMappers.kt:71-75`); solo falta el productor. Con ~20 sesiones se puede estimar la ventana horaria del usuario **[Inferencia]** y usarla para el recordatorio; antes de ese N, no afirmar nada sobre "tu mejor hora".

### I6 · Estructura de sesión (parámetros concretos)

| Tipo | Frecuencia | Duración | Composición | Adaptación | ¿Muestra número? |
|---|---|---|---|---|---|
| **Diaria corta** | 5 días/sem | 7–9 min | 1 ancla (≈2,5 min, fijo 14 d) + 2 rotativos de módulos distintos | Sí, dentro del bloque | **No** |
| **Profunda** | 2×/sem (no consecutivos, ≥24 h) | 20–25 min | 1 ejercicio, 3–4 bloques con descanso de 45 s | Sí, rango completo | **No** |
| **Recuperación** | Cuando el gate dispara | 4 min | 1 bloque baja carga, dificultad congelada | **No** (cap duro) | **No**, y explícitamente no elegible para perfil |
| **Semanal** | 1×/sem | 6 min | **Probes no entrenados** (`includeProbe = true`) | **No** (dificultad fija) | Solo "medición registrada" |
| **Mensual** | 1×/30 d | 12–15 min | Batería fija, misma dificultad, ventana horaria similar a la del baseline | **No** | **Sí — único punto** |

Reglas duras que protegen la eficacia:
- **El ancla no rota antes de 14 días** aunque el usuario se aburra. El aburrimiento se combate en los 2 bloques rotativos, no sacrificando la serie temporal.
- **Semanal y mensual con dificultad congelada.** Una evaluación adaptativa no es comparable consigo misma.
- **La mensual usa la misma ventana horaria** que el baseline (±2 h). Sin eso, el "cambio" mide hora del día. **[Riesgo científico]**
- **Ningún módulo con banco finito o métrica no cableada entra en la mensual.** Hoy eso excluye Lógica (6 ítems fijos, `LogicContentProvider.kt:29`), Memoria en su forma actual (reconocimiento, un trial, `delayedAccuracy = null`), E8/E9/E10 de Enfoque y la separación emocional (28 ítems). Excluirlos no es cobardía: incluirlos es fabricar el número.
- **Nunca acortar la profunda por retención.** Si el usuario no tiene 20 min, ese día no hay profunda — no hay "profunda exprés".

### I7 · Orden de ejecución sugerido
`M4 (duración honesta)` → `M3 (volumen/cap cableados)` → `M2 (multi-bloque)` → `M1 (un tap)` → `M5 (check-in)` → `I3 (carga transversal)` → `I4 (racha)`.
M4 y M3 son días, no semanas, y son los que convierten dos mentiras del producto (duración, descanso) en conducta real. M1 sin M2 es peor que nada: un botón único que lanza 160 trials del mismo ejercicio empeora el abandono.

### I8 · Lo que esta propuesta **no** arregla
La adherencia no repara la validez. Aunque se implemente todo lo anterior, siguen abiertos: métricas que no son función de la conducta (E8/E9/E10), reconocimiento en lugar de recall en Memoria, adaptación desconectada del contenido en Lógica, ausencia de corrección por SRT en Velocidad, y calibración metacognitiva sin cablear en Emociones. **Un producto con adherencia perfecta sobre medición falsa es peor que uno que nadie usa: escala el error.** El calendario correcto es cablear el ciclo de evaluación honesta (probes semanales + batería mensual) **antes** de escalar la adquisición.

---

## Anexo F. Evaluacion inicial y perfiles cognitivos
# Evaluación Inicial MindBoost — `CALIBRACIÓN-3` (baseline multi-sesión)

> Metodología: leídas las 6 auditorías de módulo y verificado el código real de runtime, generadores, gates de muestra y persistencia. **Hallazgo previo que cambia el encargo:** ya existe un baseline REAL, cableado y alcanzable para Enfoque — `FocusBaselineStateHolder` (250 líneas) + `BaselineProtocols.INITIAL` + `FocusFixedBlockRunner` + `BaselineAssembler`, con entrada desde `FocusRootScreen.kt:81-86` (`onStartBaseline`). No hay que construir un motor de evaluación: **hay que corregir sus parámetros, generalizarlo a 2 módulos más y ponerle un gate de honestidad.** Todo lo que sigue es incremental sobre eso.

---

## 0. Veredicto brutal antes del diseño

1. **El baseline existente NO pasa el gate de muestra del propio repo.** `BaselineProtocols.INITIAL` (`focus/domain/evaluation/BaselineProtocol.kt:76-125`) usa `calibrationDifficulty = DifficultyVector(perceptualLoad = 0.35, temporalUncertainty = 0.35)` y deja `interference` e `inhibitoryDemand` en `MIN`. Consecuencia aritmética, no opinión:
   - **E1 (75 trials):** `targetRarity = lerp(0.30, 0.06, 0.35) = 0.216` (`E1VigiaGenerator.kt:36`) → **≈16 señales** → 16 RTs de acierto. `MinSampleGate.ACCURACY_RT_BASE = 20` (`core/scoring/MinSampleGate.kt:57`). **FALLA.**
   - **E2 (60 trials):** `incongruentProportion = lerp(0.20, 0.60, 0.0) = 0.20` (`E2FiltroGenerator.kt:28`) → **12 incongruentes** vs `CONGRUENCY_EFFECT` = 20/celda + `requiresAllCells` (`MinSampleGate.kt:47`). **FALLA.**
   - **E3 (60 trials):** `goProportion = lerp(0.70, 0.90, 0.0) = 0.70` (`E3GatilloGenerator.kt:26`) → **18 no-go** vs `DPRIME = 20` (`MinSampleGate.kt:53`). **FALLA** (y además d′ ni siquiera se computa: no hay SDT en el módulo).
   [Mejora crítica] Tres números en un archivo de datos y una función pura de SDT arreglan las tres. Es el ROI más alto de todo este documento.
2. **De los 13 constructos pedidos, hoy son honestamente medibles 6.** Parciales 4. Imposibles 3 (**memoria de trabajo, regulación emocional, tolerancia a la frustración como constructo**). Cualquier perfil que dependa de esos 3 **no es asignable** y decirlo es obligatorio.
3. **Ningún perfil puede asignarse con una sola sesión.** El RT varía ±30-50 ms día a día (auditoría Velocidad §Métricas) y la primera exposición mide calentamiento motor, no capacidad. Un baseline de una sesión es un artefacto de práctica con nombre de diagnóstico. [Riesgo científico]
4. **El baseline solo es autoritativo en Android con timebase `CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`** (regla ya vigente en `SpeedProfileSource`). En desktop/iOS la sesión es jugable pero **no puede producir perfil**. Fail-closed, sin excepciones.

---

## 1. Nombre de la evaluación

**`CALIBRACIÓN-3`** — calibración inicial en 3 sesiones cortas (días 1, 2-3, 4-6).
Copy de usuario: *"Calibración inicial: 3 sesiones cortas para ajustar tu entrenamiento a tu propia línea base."* Nunca "test", nunca "nivel", nunca "puntuación".

## 2. Objetivo

Producir un **perfil cognitivo funcional preliminar** con dos usos y ninguno más:
- **(a) Anclar la línea base propia** (`BaselineSnapshot` + `IndividualThresholds`), que es el único referente contra el que se reportará progreso.
- **(b) Sembrar la personalización**: dificultad inicial por ejercicio y priorización de ejercicios.

**No** produce dimensión de `CognitiveProfile`, **no** entra al `MindBoostProfileAggregator`, **no** genera número 0-100. El baseline es *entrada de personalización*, no *afirmación cognitiva* — mientras no pase `ScoreStateMachine` + `MinSampleGate` + integridad de banco, su contribución al perfil es `null` (`CALIBRANDO`), no un valor bajo.

## 3. Duración

| Sesión | Contenido | Duración medida (params reales) |
|---|---|---|
| **S1** (día 1) | Estado + SRT + Vigilancia | **≈6.5 min** |
| **S2** (día 2-3) | SRT-retest + Conflicto + Flexibilidad | **≈6 min** |
| **S3** (día 4-6) | Inhibición + curva velocidad-precisión + razonamiento/confianza | **≈7 min** |
| **Total** | | **≈20 min repartidos en 3 días** |

Restricción dura: **S1 nunca es el ancla**. Si el delta S1→S2 en SRT supera el criterio de cambio fiable, S1 se marca `WARMUP_DISCARDED` y el ancla es S2 (ver §7).

## 4. Mecánica

Todas las sondas corren a **dificultad FIJA** (sin escalera, sin adaptación) salvo la sonda SAT de S3, que es la única donde el deadline se mueve por diseño. El invariante ya está codificado: `BaselineBlock.init` exige `mode != Mode.TRAINING` y `BaselineProtocol.init` exige `blocks.none { it.mode == TRAINING }` (`BaselineProtocol.kt:36-38, 62-65`). Respetarlo.

### S1 — Estado + velocidad basal + vigilancia (≈6.5 min)

| Bloque | Ejercicio reutilizado | Parámetros exactos | Constructos |
|---|---|---|---|
| **S1.0** (20 s) | `StateQuestionnaire` (`BaselineProtocol.kt:15-21`) — sueño, cafeína, ánimo, hora local | tal cual, ya existe | Covariables. **Nunca input de score** (el CRI de Decisiones mete 20% de autoinforme en un índice presentado como objetivo → `ScoringEquations.calculateCRI`; no repetir ese error) |
| **S1.1** (≈75 s) | **Velocidad · E1 Destello** vía `SpeedSessionStateHolder(exerciseId = E1_DESTELLO, trialsPerBlock = 24, blockCount = 1, staircase = null)` — parámetros ya son de constructor (`SpeedSessionStateHolder.kt:101-103`; default 12 en `:419`) | 24 trials, foreperiodo aleatorio 800-2200 ms (`:449-450`) | **SRT** (constante de corrección), **consistencia** (RT-CV), **impulsividad** (anticipaciones <100 ms) |
| **S1.2** (≈5 min) | **Enfoque · E1 Vigía**, bloque ya existente del protocolo | `targetTrialCount = 75`, **`perceptualLoad = 0.0`** (rarity 0.30 → ≥22 señales), `temporalUncertainty = 0.35` (ISI 1550-4140 ms) | **Atención sostenida**, **consistencia**, **fatiga intra-sesión** (pendiente de decremento), **impulsividad** (FA a lure) |

### S2 — Retest + conflicto + flexibilidad (≈6 min)

| Bloque | Ejercicio | Parámetros exactos | Constructos |
|---|---|---|---|
| **S2.1** (≈75 s) | **Velocidad · E1 Destello** idéntico a S1.1 (mismos params, semilla distinta) | 24 trials | **Test-retest**, **consistencia día-a-día** |
| **S2.2** (≈110 s) | **Enfoque · E2 Filtro** | `targetTrialCount = 60`, **`interference = 0.50`** → 40% incongruente = **24 incong / 36 cong** ✓, `pressure = 0.0` (ventana 1500 ms FIJA — jamás mezclar presión con filtrado), `perceptualLoad = 0.35` (3 flancos) | **Control de interferencia**, **precisión** |
| **S2.3** (≈120 s) | **Enfoque · E6 Cambio** *(bloque nuevo en el protocolo)* | `targetTrialCount = 60`, `switchFrequency ≈ 0.45` → 27 switch / 33 repeat ✓ `SWITCH_COST` (20/celda), `pressure = 0.0` | **Flexibilidad cognitiva** |

[Mejora crítica] S2.3 **no es un cambio de datos**: `FocusFixedBlockRunner.fieldFor` (`presentation/exercise/FocusFixedBlockRunner.kt:138-154`) soporta Vigilance / Flanker / Conflict / go-no-go y hace `error(...)` con cualquier otro payload. Falta una rama `RuleSwitch` (~5 líneas, espejando `FocusSessionStateHolder`). Coste real: pequeño, pero declarado.

### S3 — Inhibición + presión + razonamiento (≈7 min)

| Bloque | Ejercicio | Parámetros exactos | Constructos |
|---|---|---|---|
| **S3.1** (≈100 s) | **Enfoque · E3 Gatillo** | **`targetTrialCount = 70`** (60 da 18 no-go, insuficiente), `inhibitoryDemand = 0.0` → 30% no-go = **21** ✓ `DPRIME`, `pressure = 0.0` (ventana 900 ms) | **Control inhibitorio** (d′), **sesgo de respuesta** (criterio c) |
| **S3.2** (≈120 s) | **Velocidad · E6 Multivía adaptativo** (`ADAPTIVE_BLOCKS = 4` × 12 = 48 trials, `SpeedExerciseScreen.kt:96,139`) | tal cual, con `MultiSignalStaircaseEngine` | **Precisión bajo presión** (curva SAT por bloque), **impulsividad** (`impIdx = 0.6·antic + 0.4·fastErrorShare`, `MetricEdgeCasePolicy.kt:109-111`), **fatiga** (`FatigueIndex` requiere ≥3 bloques — 4 ✓, `FatigueIndex.kt:23-25`) |
| **S3.3** (≈150 s) | **Lógica**: 3 ítems válidos (`logic-e1-audit-01`, `logic-e3-access-01`, `logic-e23-chef-01`) + confianza por ítem | contenido fijo existente | **Razonamiento** (exploratorio, n=3), **metacognición** (3 juicios de confianza) |

**Exclusiones explícitas del baseline y por qué** (no negociable):
- `logic-e4-publish-01`: el prompt **contiene la respuesta verbatim** (`LogicContentProvider.kt:153`). Mide lectura.
- `logic-e5-reservation-01`: base-rate 100% "consistente" → responder siempre "no hay contradicción" da 100%.
- **Reversibilidad (Decisiones)**: `isReversible = random.nextDouble() >= irreversibility` (`ReversibilityEngine.kt:18`) → mide suerte, y es explotable al 100% vía la fuga del lock.
- **Check-in de Decisiones**: sin ISI aleatorio (`DecisionCheckInScreen.kt:158-162`) → RT contaminado por anticipación rítmica. Su autoinforme de fatiga sí se reutiliza como covariable.
- **Enfoque E7/E8/E9/E10**: métricas que no son función de la conducta del usuario (contador decorativo, probe predeterminado por `pickProbe`, un solo tap para un blink). Cero valor diagnóstico.
- **Velocidad E3 Cifrado**: banco de 4 pares con la clave siempre visible → memorización en minutos.
- **Memoria EX1/EX3/EX4**: el runtime colapsa todo a reconocimiento multi-select sin tiempo ni demora; `WORKING_MEMORY_SPAN = trial.hits` (`MemoryMetricsEngine.kt:81`) es un conteo de reconocimientos. **Ningún probe de MT en el baseline hasta que se arregle.**
- **Emociones**: 6 ejercicios cableados = 3 paradigmas (go/no-go, 2-AFC perceptivo, clasificación con banco de 28 ítems). No añade constructo que E1/E3 no midan mejor, y el banco se memoriza. Fuera del baseline v1.

## 5. Inputs

**Por trial** (ya capturados): `onsetNanos`, `responseNanos`, `responseKey`, `isTarget`/`correctResponseKey`, `outcome` (`HIT`/`MISS`/`FALSE_ALARM`/`CORRECT_REJECTION`/`ANTICIPATION`), `interStimulusDelayMs`, `responseWindowMs`, `congruent`/`isSwitch` cuando aplica.

**Por bloque:** conteos por celda, `n` efectivo, `timestampSource`, `inputTimestampPrecision`, semilla + `RngVersion` (`SessionSeed`), versión de generador.

**Por sesión:** `SubjectiveState` (sueño/cafeína/ánimo/hora), pausas usadas, intentos de salida, `SessionStatus` (`STARTED`/`CHECKPOINT`/`FINALIZED`/`ABANDONED`, `core/session/SessionRecord.kt:17`).

**Falta capturar** [Dato necesario]: (a) **confianza post-bloque** (un deslizador 0-100 tras cada bloque, reutilizable en todos los módulos — es la vía barata a calibración transversal); (b) **omisiones reales en Velocidad** — hoy no hay timeout en fase `Stimulus` (`SpeedExerciseScreen.kt:109-121`), un no-toque cuelga el trial y `omissionRate` recibe siempre 0; (c) **latencia de input del dispositivo**, sin la cual el SRT mezcla persona y hardware.

## 6. Scoring

Ningún índice se computa si no pasa su gate. Ausencia = `null` tipado, **jamás 0** ni "bajo".

| Índice | Fórmula | Fuente | Gate | ¿Hoy? |
|---|---|---|---|---|
| `srtMedianMs` | mediana de RT de aciertos | S1.1/S2.1 | `ACCURACY_RT_BASE` n≥20 (24 ✓) | ✅ |
| `srtCv` | SD/mediana intra-bloque | S1.1 | n≥20 | ✅ |
| `decisionTimeMs` | `RT_med(E6) − srtMedianMs` | S3.2 − S1.1 | ambos gates | ❌ la resta no existe |
| `rtCvVigilance` | CV de RTs de señal | S1.2 | ≥20 señales (con `perceptualLoad=0.0` → 22 ✓) | ✅ tras el fix |
| `lapseRate` | RT > k·mediana sobre **RTs crudos** (no filtrados por MAD) | S1.2 | n≥20 | ✅ |
| `vigilanceSlope` | pendiente RT/error contra tiempo-en-tarea intra-bloque | S1.2 | `MIN_N_SLOPE` | ✅ |
| `congruencyEffectMs` | `RT_med(incong) − RT_med(cong)` | S2.2 | `CONGRUENCY_EFFECT` 20/celda + todas las celdas (24/36 ✓) | ✅ tras el fix |
| `switchCostMs` | `RT_med(switch) − RT_med(repeat)` | S2.3 | `SWITCH_COST` 20/celda (27/33 ✓) | ⚠️ falta rama del runner |
| `dPrime`, `criterionC` | `d′ = z(H) − z(FA)`; `c = −0.5·(z(H) + z(FA))`, corrección loglineal | S3.1 | `DPRIME` n≥20 (21 ✓) + IC 95% | ❌ **no existe SDT** |
| `impIdx` | `0.6·anticipationRate + 0.4·fastErrorShare` | S1.1, S3.2 | `fastErrorShare != null` | ✅ ya implementado |
| `satContrast` | `pc(tercil RT rápido propio) − pc(tercil lento propio)` | S3.2 | ≥8 trials/tercil | ❌ trivial de calcular |
| `fatIdx` | `−Δvce + Δτ + Δlapse` estandarizados | S3.2 | ≥3 bloques (4 ✓) | ✅ ya implementado |
| `retestDelta` | criterio de cambio fiable S1↔S2 sobre `srtMedianMs` | S1.1↔S2.1 | `RCI` n≥2 sesiones | ⚠️ `ReliableChangeIndex` existe en Memoria, **sin cablear** |
| `brierScore` | calibración confianza↔acierto | todos | `BRIER` n≥**40** juicios | ❌ hoy n≤11 |
| `frustrationProxies` | conjunto de `OverloadProxy` disparados + `ABANDONED` | todas | conteo, sin umbral | ⚠️ existe en Emociones (`SafetyProxySignals.kt:9-18`), no transversal |

**Prohibiciones de scoring:**
- Nada de índices compuestos inventados (`SUSTAINED_FOCUS_INDEX = 100·(1−rtCv)·(1−lapseRate)`, `CRI = 50%acc + 30%estab + 20%fatiga-autoinformada`). Un compuesto de 3 constructos no es interpretable y ya mordió en dos módulos.
- **Toda fórmula del tipo `1 − x` alimentada por un estadístico que devuelve 0.0 con lista vacía fabrica perfección**, no penalización. Verificado como defecto real en este repo. Cada índice del baseline debe devolver `Absent(INSUFFICIENT_N)` tipado, no un `Double` centinela.
- La precisión global nunca se reporta sola: sin d′/c, "precisión alta" confunde capacidad con criterio conservador (responder lento y poco).

## 7. Normalización

**No hay normas poblacionales y no se van a fabricar.** Toda la normalización es intra-sujeto y de dos tipos:

1. **Contrastes sin normas (los que asignan perfil).** Son signados o de razón, interpretables sin percentiles:
   - `criterionC` (c>0 conservador, c<0 liberal) — anclado en 0.
   - `impIdx` — anclado en 0 (sin anticipaciones ni errores rápidos).
   - `satContrast` — diferencia intra-persona entre sus propios terciles de RT.
   - Costes proporcionales: `congruencyEffectMs / srtCorrectedRT`, `switchCostMs / RT_repeat` — unidad-libres, comparables consigo mismo.
   - `vigilanceSlope`, `fatIdx` — pendientes, el signo es la información.
2. **Ancla longitudinal.** `BaselineSnapshot` = **S2 y S3** (S1 se usa solo para estimar la pendiente de práctica). Todo progreso posterior se reporta como delta contra ese ancla, con criterio de cambio fiable — nunca como "percentil" ni "nivel".

**Regla de estabilidad (la que hace que esto sea evaluación y no adivinanza):**
```
si |srt(S2) − srt(S1)| > RCI_crit(SE_within):
    baselineStatus = WARMUP_DETECTED      // curva de práctica aún activa
    ancla = S2..S3, S1 descartado como línea base
si además |srt(S3) − srt(S2)| > RCI_crit:
    baselineStatus = UNSTABLE
    perfil = SIN_PERFIL_DETERMINADO       // se re-mide en la sesión 4
```
[Dato necesario] `SE_within` no existe: hay que estimarlo con las 3 sesiones del propio usuario (bootstrap sobre sus trials) hasta tener datos agregados. Mientras tanto, umbral conservador y explícito en el código, no oculto.

**Comparación entre usuarios: no disponible.** Requeriría recolección de normas con consentimiento explícito y no está en alcance. Cualquier copy tipo "por encima de la media" está prohibido hoy.

## 8. Riesgos de sesgo

| # | Sesgo | Mecanismo real | Mitigación |
|---|---|---|---|
| B1 | **Efecto práctica / calentamiento motor** | El SRT mejora S1→S2 por familiaridad con la mecánica. Confundir eso con "consistencia" o con "mejora" es el error nº1. | 3 sesiones + descarte de S1 como ancla + descartar los 4 primeros trials de cada bloque [Mejora crítica: hoy no se descartan]. |
| B2 | **Hardware y timebase** | El SRT mezcla latencia de pantalla/táctil con latencia humana. En desktop/iOS el timebase es fallback. | Baseline autoritativo solo con `CHOREOGRAPHER_FRAME + NANOS_NATIVE`; si no, sesión jugable pero `profileEligible = false`. Registrar modelo de dispositivo pseudonimizado. |
| B3 | **Estado (sueño/hora/cafeína)** | ±30-50 ms de variación diaria en RT. | Covariables capturadas; comparaciones entre días exigen la covariable; y sirven además como ítem de calibración (¿tu autoinforme predice tu RT-CV real?). |
| B4 | **Sesgo de respuesta confundido con capacidad** | Sin SDT, un usuario cauto (lento, pocos errores) puntúa igual que uno con inhibición fuerte. | `d′` + `c` obligatorios en S3.1. Sin ellos, **no se asignan** los perfiles impulsivo / rápido-impreciso / lento-preciso. |
| B5 | **Fluidez lectora** | Toda sonda con regla textual mide velocidad de lectura (la regla "puerta abierta/cerrada" de Velocidad E7 es texto; los ítems de Lógica son párrafos). | E7 excluido del baseline; el razonamiento de S3.3 se marca **exploratorio** y no entra en la asignación de perfil. |
| B6 | **Banco finito** | E6 tiene 8 estados S-R; Lógica 6 ítems fijos; Memoria ~113 palabras. | En el **baseline** (primera exposición) la memorización aún no contamina — ese es justo el momento de medir. Pero prohíbe re-usar el mismo banco como probe de progreso: reservar holdout. |
| B7 | **Gaming / key-mashing** | Tocar todo, o responder por ritmo. | Anticipaciones + `SpeedAntiGaming` (cobertura declarada `PARTIAL`) → **invalida** la sesión (fail-closed), nunca acusa ni penaliza al usuario. |
| B8 | **Sobreajuste del perfil** | 10 etiquetas sobre ~6 constructos medibles con n modesto = clasificación de ruido. | Los perfiles son **flags no exclusivos** con umbrales explícitos, y `SIN_PERFIL_DETERMINADO` debe ser el resultado **más frecuente** tras S1. Test de aceptación explícito (§13). |
| B9 | **Orden y fatiga entre bloques** | El último bloque siempre se mide con más fatiga. | Orden fijo (comparabilidad longitudinal) + `fatIdx` reportado como covariable, no como capacidad. Contrabalancear rompería el retest; se elige comparabilidad y se declara el sesgo. |

## 9. Control de frustración

- **Dificultad fija en todas las sondas de medición.** Sin escalera no hay espiral de fallo. La única sonda adaptativa (S3.2) va al final y tiene suelo.
- **Sin ms por trial en pantalla.** Mostrar el RT incentiva anticipación y persecución de número (defecto activo hoy en `SpeedExerciseScreen.kt:313`). Resumen solo al final, con banda de variabilidad.
- **Sin veredictos ni puntuaciones durante la calibración.** Copy: "calibrando", "—" cuando no hay dato. Prohibido mostrar 0.
- **Pausa siempre disponible**, y su uso es covariable, no penalización. Reutilizar `OverloadThresholds` (`SafetyProxySignals.kt:25-35`: `errorStreakLength = 3`, `exitAttemptsPerBlock = 2`) para **ofrecer** una pausa, nunca para inferir un estado emocional.
- **Abandonar es un dato válido, no un fallo.** `SessionRecorder` ya marca `ABANDONED`. Al reabrir: "retomamos donde lo dejaste", cero culpa, cero racha perdida.
- **Sesiones de ≤7 min**, 3 días. Un baseline de 20 min de una tacada es donde se pierde a los usuarios con menor tolerancia — exactamente el perfil que más necesitas identificar y menos puedes permitirte expulsar.
- **Ninguna sonda emocionalmente cargada en el baseline.** Sin inducción, sin contenido afectivo, sin ítems personales.

## 10. Uso en personalización

El resultado se persiste como `BaselineSnapshot` + `IndividualThresholds` (ya existen) más un `FunctionalProfileFlags` nuevo, en el namespace cifrado del módulo, `AccountBound` + eraser + test de cobertura en el mismo cambio.

Consume tres cosas y solo tres:
1. **Dificultad inicial por ejercicio** (`BaselineAssembler.assemble` ya lo produce, `BaselineAssembler.kt:43-46`).
2. **Prioridad de ejercicios** (§11).
3. **Forma de sesión**: duración, densidad de pausas, si se permite escalera.

Prohibido: que el baseline escriba una dimensión de `CognitiveProfile`, que alimente ranking, o que genere un número 0-100 mostrado como capacidad. Es `preliminary = true` por construcción.

---

## 11. Los 10 perfiles

Reglas operativas con umbrales sobre índices intra-sujeto. **Flags, no etiquetas exclusivas.** `SIN_PERFIL_DETERMINADO` es un resultado legítimo y esperado.

| # | Perfil | Regla operativa (índices §6) | ¿Asignable hoy? | Prioriza | Por qué |
|---|---|---|---|---|---|
| 1 | **Impulsivo** | `impIdx ≥ 0.20` **y** (`criterionC < −0.3` **o** FA a lure elevada en S1.2) | ⚠️ falta `criterionC` | **Enfoque E4 Freno** (SSRT, staircase real y anti-gaming propio) → **E3 Gatillo con SDT** → E2 Filtro con ventana FIJA | El impulsivo se define por respuesta prematura, no por error de juicio; E4 es el único paradigma del repo que mide freno reactivo con validez. **Nunca** darle sondas con deadline decreciente: refuerza el sesgo que hay que reducir. |
| 2 | **Lento-pero-preciso** | `RT` en su tercil alto propio, `pc ≥ 0.95`, `criterionC > +0.3`, `satContrast ≈ 0` | ⚠️ falta `criterionC` | **Enfoque E13 Presión** → **Velocidad E6 adaptativo** (deadline) → E2 con ventana progresivamente menor | No le falta precisión: le sobra criterio conservador. El objetivo es mover el punto de operación en la curva velocidad-precisión, no "entrenar exactitud". |
| 3 | **Rápido-pero-impreciso** | `RT` en tercil bajo, `pc ≤ 0.85`, `criterionC < 0`, **`anticipationRate` bajo** (esto lo separa del impulsivo) | ⚠️ falta `criterionC` | **Enfoque E2 Filtro** (ventana fija) → **E3 Gatillo** → E13 Presión | Su fallo es criterio liberal tras el estímulo, no disparo prematuro. E2 con ventana fija fuerza a resolver conflicto sin poder canjear precisión por velocidad. |
| 4 | **Baja tolerancia a la frustración** | ≥1 `ABANDONED` en la calibración, **o** `REPEATED_EXIT_ATTEMPTS` + `POST_ERROR_ACCURACY_DROP` | ⚠️ proxies existen, no transversales | Sesiones **`MAINTENANCE_5`**, dificultad fija, **sin escalera**, E1 Vigía y E2 a nivel cómodo | Prioridad = permanencia, no carga. Cualquier staircase aquí produce abandono. Es un perfil **conductual** (abandono, salidas), jamás una inferencia emocional. |
| 5 | **Buena memoria / mala flexibilidad** | `switchCost` proporcional alto **y** span de MT alto | ❌ **BLOQUEADO** | — | La mitad "flexibilidad" es medible (E6). La mitad "memoria" **no existe**: el runtime de Memoria es reconocimiento multi-select sin tiempo. Asignar este perfil hoy sería inventar la mitad del criterio. Se desbloquea con el probe de span real. |
| 6 | **Buena lógica / mala regulación** | `ICR` alto **y** índice de regulación bajo | ❌ **BLOQUEADO** | — | No hay medida de regulación emocional en el producto: los 6 ejercicios cableados de Emociones son inhibición y discriminación perceptiva con decorado afectivo. Y la mitad "lógica" son 6 ítems fijos, 2 inválidos. **Doble bloqueo.** |
| 7 | **Baja metacognición** | `brierScore` alto **o** correlación confianza↔acierto ≈ 0 | ❌ **BLOQUEADO por n** | — | `MinSampleGate.BRIER = 40` juicios. Hoy se capturan ≤11 (6 Lógica + 5 Bayes, y los de Bayes se **descartan**). Con confianza post-bloque en todas las sondas: ~6 juicios/sesión → asignable hacia la sesión 7. **La calibración no es un resultado del baseline; es un resultado de la semana 2.** Decirlo así. |
| 8 | **Inconsistente** | `rtCv` alto intra-sesión **y** `retestDelta` > criterio de cambio fiable entre S2 y S3 | ⚠️ falta cablear `ReliableChangeIndex` | **Enfoque E1 Vigía** (bloques cortos, RT-CV como métrica gobernante) → E3 → sesiones más frecuentes y cortas | El RT-CV es el marcador con más valor ecológico del módulo. Ojo: inconsistencia puede ser estado (sueño), no rasgo — por eso exige **dos** deltas, no uno. |
| 9 | **Avanzado** | `pc ≥ 0.95` **con** RT bajo **y** `rtCv` bajo en S1.2/S2.2 (techo en las sondas fáciles) | ✅ | **E4 Freno** → **E6 Cambio** → E13 Presión, todos con dificultad inicial elevada | Techo en las sondas fáciles significa que el baseline no discrimina más arriba: hay que subir la dificultad inicial o no se mide nada. Copy honesto: "avanzado **en estas tareas**", no "cognición superior". |
| 10 | **Principiante** | Mejora S1→S2 **mayor** que el criterio de cambio fiable, con `rtCv` alto | ✅ | E1 Vigía y E1 Destello a dificultad mínima; **re-calibrar en la sesión 4** | La señal que lo define es la **pendiente de práctica**, no el nivel. Su baseline es inválido por construcción: fijarlo en S1 congelaría un artefacto de calentamiento como "su capacidad". Este perfil es la razón de que el baseline sean 3 sesiones. |

**Lista negra de priorización (ningún perfil puede recibirlos como recomendación):** Enfoque E8/E9/E10 (métricas que no son función de la conducta), Reversibilidad de Decisiones (respuesta aleatoria + gameable al 100%), Velocidad E3 Cifrado (banco de 4 con clave visible), Memoria EX1/EX3/EX4 en su forma actual, Emociones EM10/EM11 (duplicados de EM3/EM2). [Riesgo científico] Recomendar un ejercicio cuya métrica es espuria convierte el motor de personalización en un generador de pseudoprogreso: el usuario "mejora" en un número que no mide nada, y el sistema lo refuerza.

---

## 12. Hoy vs. lo que falta construir

### Medible HOY (con los 3 arreglos de parámetros de §0)
✅ Velocidad de procesamiento (SRT) · ✅ Atención sostenida · ✅ Consistencia (RT-CV + retest) · ✅ Fatiga (intra-bloque y `fatIdx`) · ✅ Impulsividad (`impIdx` + anticipaciones) · ✅ Control de interferencia (efecto de congruencia)

### Parcial (medible pero con salvedad declarada)
⚠️ **Flexibilidad** — E6 mide switch cost real, pero con 8 estados S-R; válido en primera exposición, contaminado longitudinalmente.
⚠️ **Precisión bajo presión** — curva SAT derivable de S3.2, pero la resta del SRT no existe → mezcla latencia motora con decisión.
⚠️ **Razonamiento** — 3 ítems válidos, n insuficiente; exploratorio, no diagnóstico.
⚠️ **Tolerancia a la frustración** — solo proxies conductuales (abandono, salidas, caída post-error). Nunca presentarlo como constructo emocional.

### NO medible hoy — huecos duros
❌ **Memoria de trabajo.** El hueco más caro. Es el constructo con más evidencia de transferencia y el producto no tiene ningún probe válido: el runtime presenta los memoranda sin tiempo, descarta los `ProcessingTask` y evalúa por reconocimiento.
❌ **Control inhibitorio con validez** — d′/criterio no existen en ningún módulo; el SSRT de E4 necesita ≥30 stops y el baseline actual no llega.
❌ **Regulación emocional** — no hay paradigma de reappraisal ni de flexibilidad de regla; lo que hay es inhibición con etiqueta.
❌ **Metacognición reportable** — dato capturado, n insuficiente, y en Bayes literalmente descartado.

### Backlog ordenado por ROI (todos son cambios pequeños salvo el 6)

| # | Cambio | Archivo | Coste | Desbloquea |
|---|---|---|---|---|
| 1 | `perceptualLoad = 0.0` (E1), `interference = 0.5` (E2), `targetTrialCount = 70` (E3) en `BaselineProtocols.INITIAL` | `focus/domain/evaluation/BaselineProtocol.kt:76-125` | 3 líneas | Que el baseline existente **pase su propio gate de muestra** |
| 2 | `dPrime` + `criterionC` puros (corrección loglineal, IC 95%) | nuevo en `core/scoring/` | ~40 líneas + tests | Perfiles 1, 2, 3 |
| 3 | Timeout de omisión en fase `Stimulus` | `speed/presentation/ui/SpeedExerciseScreen.kt:109-121` | ~10 líneas | Omisiones reales; hoy un no-toque **cuelga el trial** |
| 4 | `decisionTime = RT − SRT` persistido | `speed/domain/metrics/` | ~20 líneas | Separar procesamiento de latencia motora en toda la batería |
| 5 | Confianza post-bloque (un deslizador, transversal) | runners de Enfoque/Velocidad | ~30 líneas | Perfil 7 hacia la sesión 7 |
| 6 | **Probe de span real**: presentación serial temporizada + `ProcessingTask` puntuado + recall por producción | `memory/presentation/MemorySessionStateHolder.kt` | **grande** | Memoria de trabajo, perfil 5 |
| 7 | Cablear `ReliableChangeIndex` (ya implementado y testeado en Memoria) al baseline | `memory/domain/evaluation/` → core | integración | Estabilidad de línea base, perfiles 8 y 10 |
| 8 | Rama `RuleSwitch` en `FocusFixedBlockRunner.fieldFor` | `focus/presentation/exercise/FocusFixedBlockRunner.kt:138-154` | ~5 líneas | Bloque S2.3 (flexibilidad) |
| 9 | Namespace `mindboost_assessment` + `AccountBound` + eraser + test de cobertura + `accountDeletion.ts` | `storage/`, `core/di/AppGraph.kt` | pequeño | Persistir el baseline sin hueco de borrado |

---

## 13. Criterios de aceptación y tests

**Gates de honestidad (fallan cerrado):**
1. `assert` que ningún índice del baseline devuelve un `Double` cuando su `SampleVerdict` es `InsufficientData` → debe ser `Absent` tipado.
2. `assert` que el baseline **no** produce ninguna `ModuleContribution` no nula con timebase no autoritativo.
3. `assert` que `BaselineProtocol` no contiene ningún bloque en `Mode.TRAINING` (ya existe, `BaselineProtocol.kt:64`) y ninguno con escalera.

**Tests de potencia de muestra (los que impiden repetir el defecto de §0):**
4. Para cada bloque del protocolo, un test que **simule el generador con su dificultad fija** y afirme los **conteos por celda** (no solo los valores) contra `MinSampleGate`. Con los parámetros actuales debe estar **ROJO** hoy; verde tras el arreglo 1. Sin afirmar conteos, un test de ordenamiento entre ausencias pasa trivialmente.

**Tests adversariales (perfiles sintéticos):**
5. `key-masher` → `impIdx` alto, sesión **invalidada**, sin perfil asignado, sin acusación en el copy.
6. `siempre-lento-y-cauto` → `criterionC > 0`, clasificado *lento-pero-preciso*, **no** "impreciso".
7. `honesto-competente` → **no** dispara ningún detector anti-gaming (mitad discriminante obligatoria).
8. `principiante` (mejora S1→S2 > criterio) → `WARMUP_DETECTED`, ancla = S2, **no** se congela S1 como línea base.

**Anti-sobreajuste:**
9. Con 500 usuarios sintéticos generados desde una **única** distribución (sin perfiles reales), la proporción de asignaciones distintas de `SIN_PERFIL_DETERMINADO` tras S1 debe ser **< 20%**. Si el clasificador etiqueta ruido, este test lo delata.

**Test-retest:**
10. Usuario sintético con parámetros estables entre sesiones → `retestDelta` **no** supera el criterio de cambio fiable. Usuario con deriva inyectada → sí lo supera. Ambas mitades obligatorias.

---

## Anexo G. Critica de completitud
## VEREDICTO DEL CRÍTICO DE COMPLETITUD

Las 8 auditorías de módulo cubren **44 ejercicios**; el producto expone **48 alcanzables**. El delta completo está en Velocidad, y no es cosmético: los 4 ejercicios omitidos incluyen los **dos mejor instrumentados de todo el repo**. Todas las lentes transversales (redundancia, cobertura, menú, motores adaptativos) calcularon sus números sobre ese módulo incompleto. Además hay **un motor entero (`focus2/` Focus V5) y dos módulos (`coach/`, `profile/`) sin ninguna lente propietaria**.

---

## HUECOS PRIORIZADOS

### G1 · [BLOQUEANTE] Cuatro ejercicios VIVOS de Velocidad sin auditar — y son los de mayor calidad del producto
`speed/presentation/ui/SpeedRootScreen.kt:114-156` expone **8 tarjetas** (E1,E2,E4,E5,E3,E6,E7,E8). `speed.md` audita **4** (E1,E3,E6,E7). Grep de `E8|Inhibici|GoNoGo` en `speed.md` → **0 hits**; ídem `E2|E4|E5|Compara|Cambio|Busqueda` → **0 hits**.

Lo omitido no es relleno:
- **E8_INHIBICION** es el único ejercicio del producto con contrato de calidad completo: `GoNoGoQualityDiagnostics`, `TimingIntegrityLevel` escalonado (CLEAN/MINOR/COMPROMISED, no binario), `missingOnsetTaps` contado, elegibilidad **por capa** (`profileEligible`/`calibrationEligible`/`rankingEligible`), `GoNoGoOfficialRunAdapter` → `CognitiveSessionRun` persistido, y write-ahead con `PayloadReplayScanner` (`SpeedRootScreen.kt:93`).
- **E5_BUSQUEDA**: `VisualScanQualityDiagnostics` + `VisualScanTelemetrySummaryV1` + mapper propio.
- **E4_CAMBIO**: `RuleSwitchAnalyzer` (task-switching real, con analizador dedicado).

**[Riesgo científico] Contamina cinco lentes.** §7-H5 afirma "Velocidad: dificultad ≡ deadline, **ese es el único eje**" — falso para E8 y E5. §5-A-1 repite lo mismo. La lente de adherencia cuenta "Velocidad 4 ≈ 2 constructos"; son 8. La §6.4 propone "degradar Velocidad a baseline SRT" sin saber que contiene el pipeline de referencia. Y §9 reinventa el gate V0/C0 cuando **`docs/mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md` ya lo define con niveles L1-L4, escrito a partir de E8**.

### G2 · [BLOQUEANTE] `focus2/` (Focus V5): motor completo, sin auditar y sin call-site de producción
`focus2/` tiene `runtime/`, `generator/`, `scoring/`, `evidence/`, `journal/`, `telemetry/`, `consent/`, `a11y/` + `FocusTracerScreen.kt` + `FocusTracerStateHolder.kt`. Grep de call-sites: **0 en producción** (único hit, un test de a11y). §8 lo consagra como *"el mejor artefacto psicométrico del repo"* (`DescriptiveScorer`) y correctamente dice que la capa viva no lo consume — pero **ninguna lente audita su ejercicio** (flanker, según `FocusTracerA11yLeakTest`) **ni pregunta si V5 sustituye a los 13 de `focus/` o convive con ellos**. La rama activa es `product/focus2-v5` con ADR register y S5 en ejecución: §8 propone "promover DescriptiveScorer a `core/`" mientras existe un plan paralelo que puede reemplazar el módulo entero. **[Dato necesario]** disposición de V5 (sustituye / convive / se cancela) antes de aceptar el orden de ejecución de §8.

### G3 · [ALTO] Dos módulos del árbol sin lente propietaria: `coach/` y `profile/`
- `coach/` contiene la **única detección de crisis del producto** (`AICoachSafetyPolicy`), 15 plantillas, y se vende en copy ("Incluido en Elite"). Grep en `presentation/`: **no está montado**. Aparece troceado en ética (F4) y metacognición (H2) pero sin dueño ni puntuación por ejes.
- `profile/` emite el claim más fuerte del producto ("frente a tu propia línea base") y solo se audita de refilón.

### G4 · [ALTO] Dominios canónicos que ninguna lente reclama como hueco
CLAUDE.md §1 fija **11 dominios**; `CognitiveDimension` tiene **9**.
- **`metacognition`** no existe como dimensión (lo caza la lente metacog, bien).
- **`spatial_reasoning`**: `EX5_GRID_ESPACIAL` está **declarado en el catálogo de Memoria y sin construir**; el único candidato vivo (Focus E9/MOT) mide memoria de localización. **Ninguna lente lo declara hueco.**
- **`learning_strategy`**: único vehículo `EX3_LISTA_ESTRATEGIA`, cuyo `ARC_CLUSTERING` es artefactual (lo dice §8). Nadie lo eleva a hueco de dominio.
- **Updating de MT**: `EX2_ACTUALIZACION_SET` está declarado. §5-A-5 escribe "falta memoria de trabajo bien medida (n-back/span real)" **como si hubiera que diseñarla de cero**, ignorando el diseño existente.

### G5 · [ALTO] Memoria: 13 de 16 ejercicios diseñados y no mencionados — incluida la calibración que tres lentes reinventan
`MemoryExerciseCatalog.implemented` = 3 (`EX1`, `EX3`, `EX4`); `memory.md` audita esos 3 y **no dedica una sección al catálogo no cableado** (Emociones sí lo hace, correctamente). Entre los 13 omitidos está **`EX16_CALIBRACION_CONFIANZA`**. Consecuencia: la lente de Emociones propone "N1 Calibración", la de metacognición propone `core/metacognition/`, y la Evaluación Inicial propone "confianza post-bloque" — **tres diseños independientes del mismo constructo, ninguno sabe que el catálogo ya lo declara**, y ninguno referencia `EX14_DUAL_TASK` ni `EX10_PROSPECTIVA` frente a los huecos que sí nombran.

### G6 · [MEDIO] Corpus de auditoría previo no reconciliado + cero trazabilidad en el ledger
`docs/mindboost_exercise_design/` contiene `04_1..04_8_*_audit.md`, `MASTER_AUDIT_V2.md`, `AUDIT_V2_GROUND_TRUTH.md`, `AUDIT_SYNTHESIS.md`, `EXERCISE_QUALITY_CONTRACT.md`, `INFRA_P0.md`. **Ninguna auditoría nueva ni ninguna lente cita ese corpus ni declara qué supersede.** Y ninguna registra un ID de `MINDBOOST_AUDIT_LEDGER.md`, que es obligatorio por gobernanza (`MITIGATED`≠`CLOSED`, no reutilizar IDs, `SUPERSEDED` apunta al nuevo). Riesgo directo: hallazgos duplicados con IDs nuevos y hallazgos previos que desaparecen sin nota de supersesión.

### G7 · [MEDIO] Afirmaciones que el integrador heredará como hechos y NO están verificadas
- **§5-A-7 (bug de precedencia en `ReasoningScorer.kt:47-51`)** se presenta como verbatim del código actual. **CLAUDE.md 2026-06-15 declara ese mismo bug CORREGIDO en ese mismo archivo.** O la corrección se revirtió, o la lente cita una versión antigua. **Dirimir antes de "arreglarlo" otra vez** — si ya está parcheado, el fix propuesto introduce el bug.
- **§5-C-3 ("32 de 36 correctas = `b`")** es el hallazgo más citado del red team y ningún documento muestra el comando de conteo. Si el banco cambió, cae el ataque nº1.
- **§5-A-2 / Evaluación Inicial**: "`simpleReactionMedianMs` no se resta en ningún sitio" — no verificado contra E8/E5, que tienen mappers de métricas propios (G1).
- **§4-S3 vs Evaluación Inicial**: §4 parte de `TRIALS_PER_BLOCK=10`; la Evaluación Inicial asume subir a ≥24 sin comprobar el impacto en `EmotionSafetyLimits`/`PauseGovernor`. **[Dato necesario]**

### G8 · [MEDIO] Redundancia real mayor que la señalada (y la tabla A-5 hay que recalcularla)
Omisiones cruzadas por G1:
- **go/no-go × 6**: Focus E3, Focus E8(dual), **Speed E8**, EM1, EM8, check-in Decisiones. La tabla §5-A-5 lista "Enfoque E8" pero **no Speed E8**, que es el mejor construido de los seis.
- **task-switching × 3**: Focus E6, **Speed E4** (`RuleSwitchAnalyzer`), + N3 propuesto en §4 "reusando Enfoque E6" — ignorando que Velocidad ya tiene el analizador.
- **visual search × 2**: Focus E7 (Rastreo) y **Speed E5**, cada uno con su propio pipeline de diagnóstico. Ninguna lente los cruza.
El ratio "~50 entradas / ~12 paradigmas" **empeora** al incorporar Velocidad completa; hay que recalcularlo antes de usarlo como argumento de fusión.

### G9 · [BAJO] "Creatividad / Inteligencia": se diagnostica el nombre y no se decide nada
§5-A-9 concluye que "Inteligencia no tiene sustrato en el código" y ahí termina. Ninguna lente propone (a) renombrar el track o (b) qué construiría el lado de inteligencia (razonamiento fluido / analogías / matrices), pese a que `logical_reasoning` es dominio canónico y el módulo lo reclama en el nombre. Decisión pendiente, no hallazgo pendiente.

---

## LAS 3 CONTRADICCIONES QUE EL INTEGRADOR **DEBE** RESOLVER

### ⚔️ CONTRA-1 · `DefaultEmotionAdaptiveEngine`: ¿espina del kernel unificado o motor estructuralmente muerto?
- **§7 (Motor adaptativo):** lo clasifica *"Robusto / **Sí, cableado completo** / **REUTILIZAR COMO ESPINA**"*, y construye el kernel de los 8 módulos sobre su lista de reglas ordenada y su `engagementSufficientForStepUp`.
- **§4 (Diseño Emociones):** demuestra con aritmética que ese mismo motor **no puede subir dificultad JAMÁS, en ningún ejercicio, en ninguna sesión**: `BLOCKS=2` vs `COOLDOWN_AFTER_DECISIONS=2` ⇒ `stepUp` inalcanzable (S1); `ACCURACY` vs `CATEGORY_ACCURACY` ⇒ EM3/EM10 ciegos en ambos sentidos (S2); `MIN_NOGO=6` vs 4 no-go máximos ⇒ EM1 en bloqueo circular (S3); la dificultad no se persiste entre sesiones.

No es matiz de énfasis: §7 dice "reutilizar tal cual" en su orden de ejecución; §4 dice "Ola 0 de reparación **antes de cualquier ejercicio nuevo**". Si se toma §7 literalmente, se generaliza a los 8 módulos un motor cuyo `stepUp` está **probado inalcanzable**.
**Decisión requerida:** ¿se promueve el *diseño* (orden de reglas + puerta de compromiso) reparando los parámetros del call-site, o el motor tal cual? ¿Quién es dueño de S1-S3 y en qué orden respecto al kernel?

### ⚔️ CONTRA-2 · RCI / `ReliabilitySource`: ¿desbloqueable local-first o cuello de botella que exige estudio?
- **§8 (Psicometría) §8.5:** *"**No hace falta población.**"* SEM intra-individual con k≥3 administraciones en fase de estabilización, alimentando **el mismo seam** sin cambiar la firma de `ReliableChangeIndex.compute`. *"**Coste: un productor. La maquinaria ya existe.**"*
- **§9 (Validación) §9.11:** poblar `ReliabilitySource` *"**Exige backend o estudio supervisado**"*, ≥60 pares pre/pre agregados, y lo declara *"**el cuello de botella único de toda la §9**"*.

Mismo objeto (`ProbeReliabilityEstimator` / `ReliabilitySource.EMPTY`), veredictos opuestos. Y §9 diseña EXP-3 N-of-1 asumiendo que **no** hay `r` ("delta crudo etiquetado preliminar") mientras §8 dice que N-of-1 **sí** lo produce. De esto cuelga el claim central del producto y **dos entregables que ya lo dan por hecho**: la Evaluación Inicial usa `retestDelta`/`WARMUP_DETECTED`/`UNSTABLE` como gates, y §7 asume "criterio de cambio fiable" en su detección de meseta.
**Decisión requerida:** ¿el RCI se alimenta con SEM intra-sujeto —y con qué garantía estadística declarada por escrito— o queda fail-closed hasta datos agregados? Sin esto, la mitad de las métricas longitudinales propuestas no tienen denominador.

### ⚔️ CONTRA-3 · Velocidad: ¿track de entrenamiento, instrumento de calibración, o prohibido mezclar?
Tres documentos, tres productos distintos:
- **§6 §6.4 (matriz de mezcla):** *"Enfoque ↔ Velocidad: **No**"* (mismo timebase, arrastre motor, no varía el constructo) y *"Velocidad **debería degradarse a baseline SRT**… no competir como track"*.
- **Evaluación Inicial:** `CALIBRACIÓN-3` administra **Velocidad E1 + Enfoque E1 en la misma sesión S1**, y **Velocidad E6 + Lógica en S3** — exactamente la mezcla que §6.4 prohíbe. Y §5-A-2 **exige** restar el SRT a los RT de Enfoque/Emociones/Decisiones, o sea, requiere administrar Velocidad junto al resto.
- **Adherencia (§I6):** `MindBoostSession` = 1 ancla + 2 rotativos elegidos por `TransversalDailyPlanner`, cuyo `DEFAULT_PLANNABLE` **excluye Velocidad** ⇒ el bloque de corrección de SRT nunca entraría en la sesión diaria.
- **Y G1 lo agrava:** Velocidad no son 4 pantallas de un constructo — son **8, con dos pipelines de calidad completos (E8, E5)** que ninguna de las tres posturas contempló.

**Decisión requerida:** Velocidad es (a) track de entrenamiento con 8 ejercicios, (b) instrumento de baseline administrado dentro de otras sesiones, o (c) ambas con reglas por ejercicio (E1 = calibración obligatoria; E8/E5 = entrenamiento con contrato de calidad). Sin esta decisión, §6, la Evaluación Inicial, la lente de adherencia y el motor adaptativo están diseñando cuatro productos incompatibles sobre el mismo módulo.

---

## LO QUE NO VERIFIQUÉ (declarado, no asumido)
- No releí el contenido completo de las 8 auditorías: skim dirigido de encabezados + greps de cobertura. Los veredictos por eje (0-10) los tomo como dados.
- No verifiqué el conteo `32/36 = "b"` de §5-C-3 ni el estado actual de `ReasoningScorer.kt:47-51` (G7): ambos requieren lectura de código y **son precondición** para aceptar dos hallazgos citados como críticos.
- No audité `features/arena/` (motor bayesiano de ranking): está fuera de los 8 módulos, pero **emite señal cognitiva y ninguna lente lo toca**. Marcar como hueco de alcance a decidir, no como omisión de esta auditoría.