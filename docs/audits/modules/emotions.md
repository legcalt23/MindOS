## Modulo: Emociones

I now have everything needed. Let me confirm one final critical inference — whether the EM3 separation metric uses the confusion matrix or just an "ET accuracy" subset, and the catalog consistency. I've already seen the metrics engine uses `etTrials` (INTERNAL_STATE + THOUGHT only) for separation, ignoring IMPULSE/FACT. Let me verify the `EmotionTrend`/trend computation is genuinely "UNKNOWN" always (no real longitudinal trend), which I saw hardcoded in `aggregateOf`. That confirms a systemic issue. 

I have read the catalog, all 6 wired generators, content bank, classifier, assembler, metrics engine, RT stats, adaptive engine, difficulty model, session holder, renderers, safety, feedback, strings, and profile. I have enough to write the brutal audit.

## Ejercicio: Pausa antes de actuar (EM1_PAUSA)

### Diagnóstico rápido
Es un go/no-go afectivo estándar: el usuario toca cuando el marco está ABIERTO y retiene cuando está CERRADO; la palabra emocional (`fallo`, `calma`) es solo distractor y nunca decide la respuesta (`EM1PausaGenerator.kt:48`, `EmotionTrialClassifier.kt:20-31`). Es honesto y mide control inhibitorio de verdad (tasa de comisión sobre el denominador no-go). Pero NO entrena regulación emocional: entrena inhibición motora con decorado afectivo superficial.

### Mecanismo cognitivo
Control inhibitorio (supresión de respuesta prepotente) más, en segundo plano, interferencia afectiva tipo emotional-Stroop vía `AFFECTIVE_INTERFERENCE_COST` (`DefaultEmotionMetricsEngine.kt:39-47`). El proceso REAL entrenado es la inhibición de comisión; eso está bien alineado con la capacidad declarada `PAUSE_BEFORE_ACTION`.
- **Qué produce adaptación:** elevar la proporción de no-go vía `INHIBITORY_DEMAND` (`EM1PausaGenerator.kt:30`). Es el único motor real de dificultad.
- **Qué SOBRA:** la palabra afectiva no añade carga de regulación; añade ruido perceptivo. El marco visual (`ABIERTO`/`CERRADO`) ya es la tarea completa; el "afecto" es cosmético.
- **Qué CONTAMINA la medición:** la tasa de no-go base (0.28) es baja; con `TRIALS_PER_BLOCK = 10` y dos bloques (`EmotionSessionStateHolder.kt:410-411`), salen ~5-6 trials no-go por sesión, justo en el filo de `MIN_NOGO = 6`. La métrica gobernante (`COMMISSION_RATE`) llegará frecuentemente a `confident = false` (datos insuficientes). Mecanismo entrenado real, medición subdimensionada.
- **Qué es solo práctica de tarea:** reconocer el patrón borde-grueso = ABIERTO. Mejora rapidísima por aprendizaje perceptivo, no por mejor regulación.

### Potencial de transferencia
**Muy baja.** El propio catálogo lo etiqueta `TASK_ONLY` (`EmotionExerciseCatalog.kt:30`), lo cual es honesto. Go/no-go entrena inhibición específica de la tarea; la evidencia de transferencia de inhibición a control de impulsos en la vida real es débil/nula. Es mejora-en-la-tarea. Para subir a transferencia cercana habría que: (1) variar el mapeo de regla entre bloques (a veces ABIERTO=retener) para forzar flexibilidad, no aprendizaje S-R fijo; (2) introducir coste real por comisión (no solo conteo).

### Problemas detectados
- **Subpotencia de la métrica gobernante:** `NO_GO_RATIO = 0.28` × 20 trials ≈ 5-6 no-go < `MIN_NOGO = 6` (`EM1PausaGenerator.kt:79`, `DefaultEmotionMetricsEngine.kt:181`). La métrica primaria del ejercicio nace casi siempre no-confiable. Fallo de diseño de medición grave.
- **Interferencia afectiva casi nunca medible:** requiere `MIN_CELL = 5` celdas congruentes E incongruentes de HITs (`DefaultEmotionMetricsEngine.kt:41,184`). Con ~14 go-trials divididos, raramente se alcanzan 5+5 con RT válido. La métrica "estrella afectiva" es decorativa.
- **Aprendizaje perceptivo del marco:** el borde de 6dp vs 2dp (`EmotionGoNoGoField.kt:48`) se aprende en una sesión; la "mejora" es discriminación visual, no inhibición.
- **`congruent` en no-go mal definido:** marco cerrado + tono negativo se marca `congruent = negative` (`EM1PausaGenerator.kt:71-75`), pero el coste de interferencia solo se computa sobre HITs (go-trials), así que esa congruencia de no-go nunca entra en la métrica. Código muerto semántico.

### Riesgos de falsa mejora
- **Efecto práctica perceptiva:** alto. La comisión baja rápido por familiaridad con el marco, no por control.
- **Banco finito de 24 palabras** (`EmotionContent.kt:16-41`): memorización trivial; pero como el tono no es la respuesta, esto no infla el score directamente — solo elimina cualquier valor afectivo tras 2 sesiones.
- **Dificultad falsa:** `INHIB_SPAN = 0.12` mueve la ratio no-go de 0.28 a 0.40 como máximo. El techo de dificultad es bajísimo; un usuario competente toca el techo en 2-3 sesiones.

### Métricas recomendadas
- **Principal:** d-prime (sensibilidad de señal) en vez de comisión cruda — separa criterio de discriminabilidad. La comisión sola confunde "impulsivo" con "sesgo de respuesta liberal".
- **Secundarias:** SSRT-like si se añade stop-signal; coste de interferencia afectiva (ya existe, pero necesita ≥30 trials para ser medible).
- **Consistencia:** RT-CV de go-trials (ya disponible en otros paradigmas, ausente aquí).
- **Engañan hoy:** `COMMISSION_RATE` con n<6 mostrada como número, y `ANTICIPATIONS` con `IMPULSIVE_FLOOR_MS = 350ms` (`DefaultEmotionMetricsEngine.kt:187`) — un umbral arbitrario sin calibración por usuario.

### Progresión recomendada
Subir trials/bloque a ≥24 para que la comisión sea medible. Curva: no-go de 0.25→0.50, ventana de respuesta decreciente (1500→700ms), y a partir de dominio, introducir reversiones de regla. Zona óptima: comisión 0.10-0.25. Hoy el techo se alcanza demasiado pronto (fondo de tarea).

### Rediseño propuesto
Convertir en **stop-signal afectivo** real: estímulo go que a veces dispara una señal de stop con SOA adaptativo (staircase 1-up/1-down → SSRT). El afecto entra como modulador: señales de stop tras palabra negativa miden si la carga afectiva degrada la inhibición (interacción real, medible con suficientes trials). Métricas: SSRT, SSRT-bajo-carga-afectiva vs neutra. Progresión: SOA adaptativo por trial. Testeable: SSRT debe estabilizarse y el delta afectivo debe ser positivo y replicable. Superior porque mide un constructo inhibitorio validado (SSRT) y la interacción afecto×inhibición que el diseño actual finge medir pero no logra.

### Puntuación 0-10 (los 15 ejes)
1) Mejora cognitiva real: 3 | 2) Transferencia: 2 | 3) Claridad mecanismo: 8 | 4) Progresión: 3 | 5) Medición: 3 | 6) Personalización: 3 | 7) Resistencia a práctica: 2 | 8) Metacognición: 1 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin riesgo): 8 | 11) Riesgo pseudoprogreso (10=sin riesgo): 3 | 12) Adherencia: 4 | 13) Escalabilidad perfiles: 4 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 8
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente (a stop-signal afectivo) o fusionar con EM8 (son el mismo paradigma con distinta ratio).

---

## Ejercicio: Recuperación tras error (EM2_RECUPERACION)

### Diagnóstico rápido
Es una discriminación perceptiva 2-alternativa ("¿qué barra es más alta?", `EmotionTwoAltField.kt:45`) donde la dificultad baja la discriminabilidad para forzar errores y medir recuperación post-error. El constructo (recuperación tras error) es el más interesante del módulo, PERO el host es perceptivo-neutro: no hay nada emocional. El "error" es perceptual, no afectivo. Vale como tarea de control post-error; es engañoso llamarlo "emocional".

### Mecanismo cognitivo
Monitorización de errores y recuperación de rendimiento tras fallo (post-error slowing/accuracy, un constructo de control cognitivo real con base en ERN/Pe). El compuesto condicional está bien pensado: penaliza el enlentecimiento SOLO si la precisión no se recuperó, y penaliza impulsividad post-error (`DefaultEmotionMetricsEngine.kt:160-166`). Eso es ciencia decente.
- **Qué produce adaptación:** bajar discriminabilidad vía `POST_ERROR_LOAD` (`EM2RecuperacionGenerator.kt:28-29`) hasta que aparecen errores normales.
- **Qué SOBRA:** nada del host es emocional. La "recuperación emocional tras error" es una analogía, no el constructo medido.
- **Qué CONTAMINA:** la discriminabilidad determinista (`EmotionSessionStateHolder.kt:53-59`) significa que un usuario con buena visión y atención NUNCA falla los trials fáciles; los errores solo ocurren en los `harder` (disc=0.45). El "error" depende de agudeza perceptiva, no de regulación. Un usuario puede tener 0 errores y la métrica gobernante sale `unavailable` (`DefaultEmotionMetricsEngine.kt:90-93`).
- **Qué es práctica de tarea:** comparar dos alturas de barra. Cero valor de regulación emocional.

### Potencial de transferencia
**Muy baja a la vida real como "regulación emocional".** El catálogo dice `PLAUSIBLE` (`EmotionExerciseCatalog.kt:48`), lo cual es OPTIMISTA y discutible: la recuperación post-error en una tarea perceptiva no se ha demostrado que transfiera a recuperación emocional tras un fallo real (despido, discusión). Es transferencia cercana, como mucho, dentro de tareas de rendimiento. La etiqueta debería ser `NOT_DEMONSTRATED`. Para aumentar transferencia: el "feedback negativo" tras error tendría que tener carga afectiva real (no un perceptual neutro), y medir si el usuario mantiene estrategia tras un fallo *significativo*.

### Problemas detectados
- **EM2 ≈ EM11 idénticos:** `EM11ContinuidadGenerator.kt` es EM2 con más trials plantados (`HARD_EVERY=3` vs `5`, `MIN_HARD=4` vs `3`). Mismo payload, mismo renderer, misma métrica `POST_ERROR_RECOVERY`. Redundancia casi total (ver veredicto).
- **Errores no garantizados:** los trials `harder` tienen disc=0.45 pero la respuesta correcta sigue siendo determinista (barra mayor); un usuario atento acierta igualmente. El generador "planta trials difíciles" pero NO planta errores. La premisa "post-error recovery measurable" (`EM2RecuperacionGenerator.kt:14`) depende de que el usuario falle, lo cual no está garantizado por diseño.
- **`MIN_POST_ERROR = 3` pares con ~20 trials:** si el usuario comete <3 errores (probable en usuarios competentes), la métrica gobernante del ejercicio sale no-confiable. El ejercicio puede "no medir nada" en sus mejores usuarios — un fallo de medición que castiga al competente.
- **`discriminability` mapeada a altura de barra** (`leftMagnitude`/`rightMagnitude`, `EmotionSessionStateHolder.kt:54-59`): a disc=0.45, gap=0.18, las barras difieren ~18% de altura — perfectamente discriminable visualmente. La "near-threshold" no es near-threshold en absoluto.

### Riesgos de falsa mejora
- **Inflado del score gobernante por ausencia de errores:** si no hay 3+ errores, `unavailable` (correcto), pero el dashboard puede interpretar "sin datos" como neutro; el usuario percibe que "le va bien" sin medición.
- **Práctica perceptiva:** comparar barras se automatiza; los errores bajan, la recuperación deja de medirse → falsa sensación de dominio.
- **Banco infinito (generado), sin memorización** — el único punto fuerte anti-práctica.

### Métricas recomendadas
- **Principal:** post-error accuracy adjustment (PEA) condicionado, ya implementado decentemente; mantener pero exigir ≥6 pares.
- **Secundarias:** post-error slowing (existe), recuperación de RT-CV tras error.
- **Engaña hoy:** la métrica gobernante sale `unavailable` justo en usuarios buenos; eso no es "honesto", es "no mide". Y `POST_ERROR_RECOVERY` con solo 3 pares es estadísticamente ruidoso.

### Progresión recomendada
Forzar una tasa de error objetivo (~25-30%) con staircase psicofísico real (no plantado fijo), de modo que SIEMPRE haya suficientes errores para medir recuperación independientemente de la habilidad. Esto resuelve el "techo del competente".

### Rediseño propuesto
Sustituir el host perceptivo por una tarea con **error afectivamente cargado pero leve**: p.ej. una clasificación rápida donde el feedback de error es un aviso neutro pero la siguiente decisión es bajo presión temporal. Medir: ¿mantiene precisión y estrategia en el trial post-error? Staircase para fijar tasa de error en 30%. Métricas: PEA, post-error strategy persistence. Superior porque (a) garantiza datos de recuperación en todos los niveles, (b) acerca el constructo a "recuperarme tras un fallo que importa" en vez de "tras no ver bien dos barras".

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 2 | 3) Claridad mecanismo: 7 | 4) Progresión: 4 | 5) Medición: 4 | 6) Personalización: 4 | 7) Resistencia práctica: 3 | 8) Metacognición: 2 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 4 | 13) Escalabilidad: 5 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 4.0/10**
**Decisión final:** Rediseñar parcialmente (staircase + carga afectiva real) y fusionar EM11 dentro de EM2.

---

## Ejercicio: Separar emoción, pensamiento, impulso y hecho (EM3_SEPARADOR)

### Diagnóstico rápido
Clasificación de 4 categorías: el usuario lee una frase ("Siento tensión en los hombros") y la etiqueta como Estado interno / Pensamiento / Impulso / Hecho (`EmotionFourCategoryField.kt`, `EmotionContent.kt:50-95`). ESTE es el único ejercicio del módulo con un mecanismo cognitivo-emocional genuino (defusión / granularidad emocional). Es el más valioso del módulo y, a la vez, el peor medido por un banco finito de 40 ítems.

### Mecanismo cognitivo
Defusión cognitiva y granularidad emocional: distinguir estado interno de pensamiento de impulso de hecho es un componente real de la regulación (reappraisal se apoya en ello; la granularidad emocional predice mejor regulación en la literatura de Feldman Barrett). La mecánica está alineada con el objetivo.
- **Qué produce adaptación:** declaradamente, "ambigüedad de categoría" vía ventana de respuesta — pero el generador NO aplica dificultad (`EM3SeparadorGenerator.kt:19-20`, `@Suppress("UnusedParameter")`). El banco es fijo y pre-balanceado; no hay progresión real de dificultad de contenido.
- **Qué SOBRA:** nada estructural; el problema es escasez, no exceso.
- **Qué CONTAMINA:** el banco de entrenamiento son 28 ítems (40 menos 12 eval, `EmotionContent.kt:50-95`). Con 20 trials/sesión, en la sesión 2 el usuario ya ha visto casi todo el banco. Se mide memorización frase→categoría, no la habilidad de separar.
- **Qué es práctica de tarea:** memorizar que "Siento tensión" = Estado interno. Tras 2 sesiones es recall, no clasificación.

### Potencial de transferencia
**Baja-media (la más alta del módulo), pero solo con banco grande y generativo.** El catálogo lo marca `NOT_DEMONSTRATED` (`EmotionExerciseCatalog.kt:61`), honesto. La granularidad/defusión tiene la base teórica más sólida para transferir a regulación real. Pero con 28 ítems memorizables, la transferencia tiende a NULA por overfitting al banco. Para subir: banco de 200+ ítems generados composicionalmente, ítems ambiguos deliberados en el eje pensamiento↔hecho, e items autorreferenciales del propio estado del usuario.

### Problemas detectados
- **Banco finito y minúsculo:** 28 ítems de entrenamiento (`EmotionContent.kt`). El comentario del propio código admite "target = 80; v1 ships ≥30" (`EmotionContent.kt:8`). Memorización garantizada en 2-3 sesiones. Es el fallo más grave del ejercicio más valioso.
- **Métrica de separación ignora 2 de 4 categorías:** `EMOTION_THOUGHT_SEPARATION` solo usa INTERNAL_STATE + THOUGHT (`DefaultEmotionMetricsEngine.kt:109-115`). IMPULSE y FACT no entran en la métrica estrella. Un usuario que confunde sistemáticamente impulso↔hecho (el error más común y más importante para la regulación) saca separación = 1.0 si acierta estado↔pensamiento. La métrica nombra mal lo que mide.
- **Matriz de confusión construida pero no explotada:** `EmotionSessionAssembler.kt:37-49` calcula la confusión 4×4 completa, pero el `EmotionBlockResult` la guarda y nada la convierte en métrica de separación. El dato más rico (qué confunde con qué) se descarta para la puntuación.
- **`GRANULARITY_INDEX` trivial:** = categorías-distintas-acertadas / 4 (`DefaultEmotionMetricsEngine.kt:117-119`). Acertar 1 de cada categoría = 1.0. No mide granularidad; mide cobertura de categorías. Engañoso.
- **Sin dificultad real:** el `@Suppress("UnusedParameter")` (`EM3SeparadorGenerator.kt:19`) confirma que la dificultad declarada (ambigüedad) no existe en generación. La progresión es ficticia.

### Riesgos de falsa mejora
- **Memorización del banco:** riesgo MÁXIMO. 28 ítems, mapeo fijo frase→categoría. La precisión sube a ~100% por recall en sesión 3. Pseudoprogreso puro.
- **Score inflado por la métrica de 2 categorías:** separación = 1.0 fácil ignorando impulso/hecho.
- **Eval holdout no usado:** los 12 ítems `isEval=true` se reservan "para Ola-4 holdout" (`EmotionContent.kt:44`), pero el holdout no existe; no hay ninguna defensa activa contra el overfitting hoy.

### Métricas recomendadas
- **Principal:** precisión sobre ítems NUNCA vistos (holdout rotatorio) — la única defensa contra memorización. Implementarla YA, no en "Ola 4".
- **Secundarias:** confusión específica impulso↔hecho y pensamiento↔hecho (de la matriz que ya se calcula); d-prime por categoría.
- **Granularidad real:** entropía/diversidad de uso correcto de categorías ponderada por dificultad del ítem, no conteo de categorías tocadas.
- **Engañan hoy:** `EMOTION_THOUGHT_SEPARATION` (ignora mitad de categorías) y `GRANULARITY_INDEX` (cobertura disfrazada de granularidad).

### Progresión recomendada
Banco generativo composicional (plantilla + sujeto + objeto → cientos de frases). Dificultad por ambigüedad real: ítems frontera (p.ej. "Siento que voy a fallar" = pensamiento disfrazado de estado). Zona óptima 75-85% en ítems nuevos. Holdout rotatorio para que la precisión refleje habilidad, no memoria.

### Rediseño propuesto
**Defusión adaptativa con banco generativo y holdout obligatorio.** Generar ítems composicionalmente; etiquetar cada uno con su eje de confusión esperado; el adaptativo sube la proporción de ítems frontera según dominio. Métrica gobernante = precisión en holdout (ítems nunca presentados a ESTE usuario). Explotar la matriz de confusión para feedback específico ("confundes impulso con hecho"). Superior porque ataca el único riesgo letal (memorización), mide separación en las 4 categorías, y convierte el dato más rico (confusión) en señal accionable. Testear: precisión-holdout debe correlacionar con precisión-entrenamiento solo si hay aprendizaje real; si diverge, es memorización.

### Puntuación 0-10
1) Mejora cognitiva: 5 | 2) Transferencia: 4 | 3) Claridad mecanismo: 8 | 4) Progresión: 2 | 5) Medición: 3 | 6) Personalización: 2 | 7) Resistencia práctica: 1 | 8) Metacognición: 6 | 9) Valor emocional: 7 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 2 | 12) Adherencia: 5 | 13) Escalabilidad: 3 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 10
**Nota global del ejercicio: 4.5/10** (potencial 8/10 con rediseño)
**Decisión final:** Rediseñar parcialmente — es la joya del módulo, pero con banco finito es pseudoprogreso. Prioridad máxima de mejora.

---

## Ejercicio: Anti-impulso por comisión (EM8_ANTIIMPULSO)

### Diagnóstico rápido
Es EM1 con más no-go (0.40 vs 0.28) y sin balanceo de congruencia afectiva (`EM8AntiImpulsoGenerator.kt:49`). Mismo renderer, mismo clasificador, misma métrica de comisión. Es una variante de parámetro de EM1, no un ejercicio distinto. Su única ventaja sobre EM1: la métrica de comisión SÍ alcanza confianza (más no-go).

### Mecanismo cognitivo
Idéntico a EM1: control inhibitorio de comisión. El único cambio funcional es que `NO_GO_RATIO = 0.40` × 20 ≈ 8 no-go ≥ `MIN_NOGO = 6` → la métrica gobernante es confiable, a diferencia de EM1.
- **Qué produce adaptación:** ratio no-go vía `INHIBITORY_DEMAND` (0.40→0.55).
- **Qué SOBRA:** la palabra afectiva sigue presente (`EM8AntiImpulsoGenerator.kt:33`) pero su congruencia ya no se mide (no hay `AFFECTIVE_INTERFERENCE_COST` en sus métricas, `EmotionExerciseCatalog.kt:124`). Es decorado puro, ahora reconocido como tal.
- **Qué CONTAMINA:** mismo aprendizaje perceptivo del marco que EM1.
- **Qué es práctica de tarea:** igual que EM1.

### Potencial de transferencia
**Muy baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:126`) — de nuevo optimista para un go/no-go. Misma crítica que EM1: inhibición específica de tarea, sin transferencia demostrada a control de impulsos vital. La etiqueta debería ser `TASK_ONLY` como EM1, dado que es el MISMO paradigma.

### Problemas detectados
- **Redundancia con EM1:** mismo payload (`AffectiveGoNoGo`), mismo renderer (`EmotionGoNoGoField`), mismo clasificador, mismo eje gobernante (`INHIBITORY_DEMAND`). Difieren en 3 constantes. No justifica ser un "ejercicio" separado en un menú de 6.
- **Inconsistencia de etiqueta de transferencia:** EM1=`TASK_ONLY`, EM8=`PLAUSIBLE` para el mismo paradigma (`EmotionExerciseCatalog.kt:30` vs `:126`). Una de las dos es deshonesta.
- **Mismo techo de dificultad bajo** que EM1; `INHIB_SPAN=0.15` → tope 0.55 no-go.
- **Word `congruent` calculado pero nunca usado** (`EM8AntiImpulsoGenerator.kt:40`): `congruent = frameOpen != word.negative` se guarda pero EM8 no computa interferencia. Código semánticamente muerto.

### Riesgos de falsa mejora
- **Práctica perceptiva del marco:** alto, igual que EM1.
- **Sensación de "dos ejercicios"** cuando es uno: infla la percepción de cobertura del módulo sin añadir entrenamiento.
- Banco de 24 palabras irrelevante (no afecta score).

### Métricas recomendadas
- **Principal:** d-prime, no comisión cruda (separar criterio de sensibilidad).
- **Secundarias:** SSRT si se migra a stop-signal; RT-CV de go.
- **Engaña hoy:** comisión cruda confunde sesgo de respuesta con impulsividad; un usuario "lento y cuidadoso" puntúa igual que uno con inhibición real fuerte.

### Progresión recomendada
Si se mantiene separado de EM1, debe ser el "nivel avanzado" del paradigma go/no-go: stop-signal con SOA adaptativo. Si no, fusionar.

### Rediseño propuesto
Fusionar con EM1 en un único ejercicio "Inhibición" con dificultad continua (ratio no-go + eventual señal de stop). EM8 desaparece como entrada de menú y se convierte en el tramo alto de la curva de dificultad de EM1. Superior porque elimina redundancia, concentra los trials en una sola tarea bien medida, y libera un slot del menú para una capacidad ausente (reappraisal/flexibilidad).

### Puntuación 0-10
1) Mejora cognitiva: 3 | 2) Transferencia: 2 | 3) Claridad mecanismo: 8 | 4) Progresión: 3 | 5) Medición: 5 (comisión sí confiable) | 6) Personalización: 3 | 7) Resistencia práctica: 2 | 8) Metacognición: 1 | 9) Valor emocional: 1 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 3 | 13) Escalabilidad: 4 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Fusionar con EM1 (mismo paradigma; EM8 = tramo de dificultad alta).

---

## Ejercicio: Separación bajo presión leve (EM10_SEP_PRESION)

### Diagnóstico rápido
Es EM3 con la ventana de respuesta multiplicada por 0.65 (`EM10SepPresionGenerator.kt:41`). Mismo banco, mismo renderer, misma métrica. Variante de parámetro de EM3 (presión temporal en lugar de ventana normal). Hereda TODOS los problemas de EM3 (banco finito, métrica de 2 categorías) y añade el suyo: presión temporal aplicada a una tarea de clasificación que NO debería premiar velocidad.

### Mecanismo cognitivo
Igual que EM3 (defusión/granularidad) bajo presión temporal. La idea —separar bajo presión— es razonable conceptualmente (regular bajo tiempo es más vital que en calma). Pero:
- **Qué produce adaptación:** la ventana recortada (`PRESSURE_FACTOR=0.65`, mínimo 900ms). Fijo, no adaptativo dentro del generador.
- **Qué SOBRA:** nada nuevo respecto a EM3; misma escasez de banco.
- **Qué CONTAMINA:** la presión temporal sobre una tarea conceptual convierte la métrica de precisión en una mezcla velocidad×conocimiento. Un usuario que CONOCE las categorías pero es lento sale "peor en separación", cuando su separación es perfecta. La presión contamina el constructo que dice medir.
- **Qué es práctica de tarea:** memorización del banco + velocidad de pulsación. Doble pseudoprogreso.

### Potencial de transferencia
**Baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:150`). Mismo banco memorizable que EM3 → con práctica, la transferencia tiende a nula. La presión temporal no añade transferencia a regulación real; añade un confound de velocidad. Para mejorar: presión solo como modulador medido por separado (precisión-bajo-presión vs precisión-en-calma como índice de robustez), no mezclada en la precisión principal.

### Problemas detectados
- **Redundancia con EM3:** mismo payload `Classification`, mismo banco (`EmotionContent.SEPARATOR_ITEMS`), mismo renderer, mismo clasificador. Difiere en un factor de ventana.
- **Hereda el banco de 28 ítems** → memorización (ver EM3).
- **Hereda la métrica de separación de 2 categorías** (ignora impulso/hecho).
- **Presión confunde precisión:** `CATEGORY_ACCURACY` bajo ventana de 900ms penaliza al lento-pero-correcto. La métrica gobernante mezcla dos constructos.
- **Mínimo de ventana 900ms** puede ser demasiado corto para leer una frase de 5-7 palabras Y decidir entre 4 botones — riesgo de medir velocidad lectora, no separación.

### Riesgos de falsa mejora
- **Memorización del banco** (idéntico a EM3): máximo.
- **Mejora por velocidad de pulsación:** el usuario aprende posiciones de botón (mapeo fijo 2×2, `EmotionFourCategoryField.kt:47-54`) y responde por motor, no por análisis. Pseudoprogreso de velocidad.
- **Dificultad falsa:** "presión" = ventana más corta, que es velocidad disfrazada de habilidad de regulación.

### Métricas recomendadas
- **Principal:** precisión en holdout bajo presión.
- **Secundarias:** índice de robustez = precisión-bajo-presión / precisión-en-calma (mide si la separación aguanta la presión, que ES el constructo interesante).
- **Engaña hoy:** `CATEGORY_ACCURACY` con presión, porque confunde velocidad con conocimiento.

### Progresión recomendada
Mantener separación y presión como ejes ortogonales: medir precisión sin presión primero (conocimiento), luego presión como reto adicional con su propia métrica de robustez. Banco generativo + holdout.

### Rediseño propuesto
Integrar como un MODO de EM3 ("modo presión"), no un ejercicio separado: tras dominar EM3 sin presión, desbloquear presión adaptativa que reduce la ventana mientras la precisión-holdout se mantenga ≥80%. Métrica = índice de robustez bajo presión. Superior porque desacopla conocimiento de velocidad, reutiliza el banco generativo de EM3, y mide algo real (mantener la separación cuando hay prisa) en vez de premiar dedos rápidos.

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 3 | 3) Claridad mecanismo: 6 | 4) Progresión: 2 | 5) Medición: 2 | 6) Personalización: 2 | 7) Resistencia práctica: 1 | 8) Metacognición: 5 | 9) Valor emocional: 6 | 10) Riesgo fatiga (10=sin): 7 | 11) Riesgo pseudoprogreso (10=sin): 1 | 12) Adherencia: 4 | 13) Escalabilidad: 3 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 8
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Fusionar con EM3 (como "modo presión" adaptativo). No merece slot propio.

---

## Ejercicio: Continuidad tras feedback negativo neutro (EM11_FEEDBACK_NEG)

### Diagnóstico rápido
Es EM2 con más trials difíciles plantados (`HARD_EVERY=3`, `MIN_HARD=4`, `EM11ContinuidadGenerator.kt:55-59`). Mismo payload perceptivo de barras, mismo renderer, misma métrica `POST_ERROR_RECOVERY`. Variante de parámetro de EM2. El nombre promete "feedback negativo neutro" pero el feedback es el mismo perceptual de EM2 — no hay aviso de error diferenciado en el código que leí.

### Mecanismo cognitivo
Idéntico a EM2: recuperación post-error en tarea perceptiva 2-AFC. La diferencia es densidad de errores potenciales (más trials near-threshold). Esto SÍ mejora la medición respecto a EM2 (más pares post-error → métrica más probablemente confiable), que es su única ventaja real.
- **Qué produce adaptación:** discriminabilidad vía `POST_ERROR_LOAD` + más trials plantados.
- **Qué SOBRA:** la promesa de "feedback negativo neutro" no está implementada como algo distinto del feedback estándar de EM2. El `displayName` sobre-vende.
- **Qué CONTAMINA:** igual que EM2, errores dependen de agudeza perceptiva, no de regulación.
- **Qué es práctica de tarea:** comparar barras.

### Potencial de transferencia
**Muy baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:162`). Misma crítica que EM2: recuperación post-error perceptiva no transfiere demostradamente a recuperación emocional vital. Para mejorar habría que añadir el "feedback negativo" prometido: un aviso de error explícito (neutro pero salience) y medir continuidad de estrategia tras él.

### Problemas detectados
- **Redundancia casi total con EM2:** mismo payload `TwoAltPerceptual`, mismo renderer `EmotionTwoAltField`, mismo clasificador, misma métrica gobernante, mismo eje `POST_ERROR_LOAD`. Difieren en 3 constantes de plantado (`EM11ContinuidadGenerator.kt:55-59` vs `EM2RecuperacionGenerator.kt:58-63`).
- **Promesa incumplida:** "tras un aviso neutro de error" (`EmotionExerciseCatalog.kt:164`) — no existe un aviso de error distinto en el flujo. El feedback inmediato (`EmotionStrings.NEUTRAL_NEGATIVE="No esa vez"`) ni siquiera se ve cableado en el holder que leí.
- **Hereda "errores no garantizados"** de EM2 (la respuesta correcta sigue siendo determinista).
- **Hereda `MIN_POST_ERROR=3`** ruidoso.

### Riesgos de falsa mejora
- **Doble conteo de cobertura:** EM2 y EM11 cuentan como 2 de 6 ejercicios, siendo el mismo paradigma con distinta densidad de plantado. El módulo aparenta más variedad de la que tiene.
- **Práctica perceptiva** baja errores → la recuperación deja de medirse.

### Métricas recomendadas
- **Principal:** post-error accuracy adjustment con ≥6 pares.
- **Secundarias:** continuidad de estrategia (¿cambia el patrón de respuesta tras error?), abandono post-error.
- **Engaña hoy:** misma que EM2; además el nombre sugiere medir reacción a feedback negativo que no se mide.

### Progresión recomendada
Si se mantiene, debe implementar de verdad el feedback negativo salience y medir continuidad tras él. Si no, fusionar en EM2.

### Rediseño propuesto
Fusionar en EM2 como el tramo de "alta densidad de error". Si se quiere un ejercicio de "continuidad tras feedback negativo" real, hay que construirlo: tarea con feedback de error explícito (animación/sonido neutro pero notable) y métrica de cuánto se degrada el rendimiento en los 2-3 trials siguientes. Superior porque (a) elimina la redundancia, (b) si se conserva como ejercicio propio, al menos cumpliría su nombre.

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 2 | 3) Claridad mecanismo: 6 | 4) Progresión: 4 | 5) Medición: 5 (más pares que EM2) | 6) Personalización: 4 | 7) Resistencia práctica: 3 | 8) Metacognición: 2 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin): 7 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 4 | 13) Escalabilidad: 5 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 6
**Nota global del ejercicio: 4.0/10**
**Decisión final:** Fusionar con EM2 (mismo paradigma; EM11 = tramo de alta densidad de error) O implementar de verdad el feedback negativo prometido.

---

## Ejercicios de catálogo (no cableados): EM4, EM5, EM6, EM7, EM9, EM12, EM13, EM14, EM15

No tienen generador ni renderer (`EmotionExerciseGenerators.kt:18-25` registra solo 6; el resto vive como `EmotionExerciseDefinition` sin código ejecutable). No son auditables como ejercicios porque NO existe la mecánica, el clasificador de aciertos ni la presentación — solo una ficha declarativa. Evaluación breve del DISEÑO declarado (lo único auditable):

- **EM4 Calibrador / EM14 Confianza (`CALIBRATION_ERROR`):** son los DOS ejercicios con mayor valor metacognitivo potencial (calibración confianza↔acierto es un constructo medible y entrenable de verdad). Que estén sin cablear es el mayor hueco del módulo. `EmotionMetricKey.CALIBRATION_ERROR` existe pero NINGÚN generador wired produce datos de calibración → la métrica nunca se computa. **Prioridad de implementación: máxima.**
- **EM5 Sesgo afectivo (`AFFECTIVE_DECISION_QUALITY`):** el único con potencial de transferencia a decisiones reales bajo carga afectiva. Requiere renderer de texto + banco de 50 escenarios (no existe). Alto valor, alto coste.
- **EM6 Tolerancia (`FRUSTRATION_TOLERANCE_INDEX`):** catálogo lo marca `PLACEBO_RISK` (`EmotionExerciseCatalog.kt:101`) — honestidad notable; el propio diseño admite riesgo de placebo. Bien etiquetado.
- **EM7 Reentrada / EM13 Variabilidad (`REGULATION_STABILITY`, `RT_CV`):** miden estabilidad/variabilidad de RT. Constructo débil como "regulación"; RT-CV es ruido de atención, no regulación emocional. Riesgo de medir fatiga disfrazada de "estabilidad".
- **EM9 Etiquetado (`GRANULARITY_INDEX`):** solapa con EM3; la granularidad ya se (mal)mide en EM3. Redundante con el ejercicio que sí está cableado.
- **EM12 Espera (`ANTICIPATIONS`):** go/no-go temporal; otra variante inhibitoria. Tercera redundancia del paradigma EM1/EM8.

**Riesgo de catálogo:** 9 de 15 ejercicios son fichas sin código. El menú v1 NO los muestra (`implemented` se deriva del registry, `EmotionExerciseCatalog.kt:231`), así que no engañan al usuario. Pero el diseño concentra los 6 cableados en SOLO 3 paradigmas (go/no-go, 2-AFC perceptivo, clasificación), dejando fuera justo los 2 más valiosos (calibración, decisión afectiva).

---

## Veredicto del módulo "Emociones (regulación cognitivo-emocional) [PRIORIDAD]"

### Capacidades bien cubiertas / huecos / capacidades AUSENTES
- **Cubiertas (con reservas):** inhibición de comisión (EM1/EM8, sobre-representada, 2 ejercicios para lo mismo) y monitorización post-error (EM2/EM11, también duplicada). Defusión/separación (EM3/EM10) está cubierta conceptualmente pero rota por banco finito.
- **Huecos:** la separación (EM3) tiene el banco demasiado pequeño (28 ítems) y métrica que ignora 2 de 4 categorías.
- **AUSENTES por completo (no cableadas):** calibración metacognitiva (EM4/EM14 — la capacidad de MAYOR valor entrenable y demostrable), calidad de decisión bajo sesgo afectivo (EM5), reappraisal/reinterpretación (no existe en ningún ejercicio), y flexibilidad cognitiva (cambio de regla — ningún ejercicio cambia la regla entre bloques). **El módulo entrena 3 paradigmas y llama a eso "regulación cognitivo-emocional".**

### Ejercicios redundantes o a fusionar
- **EM1 + EM8 + (EM12) → un ejercicio "Inhibición"** con dificultad continua (ratio no-go → stop-signal). Hoy son 2-3 entradas para el mismo go/no-go.
- **EM2 + EM11 → un ejercicio "Recuperación"** con densidad de error adaptativa. Hoy son 2 entradas idénticas salvo 3 constantes.
- **EM3 + EM10 + (EM9) → un ejercicio "Separación"** con presión como modo opcional. Hoy 2-3 entradas para la misma clasificación.
- **Resultado real:** los "6 ejercicios cableados" son **3 paradigmas**. El menú aparenta el doble de variedad de la que existe.

### Riesgos sistémicos del módulo
1. **Medición común sub-dimensionada:** `BLOCKS=2 × TRIALS_PER_BLOCK=10 = 20 trials/sesión` (`EmotionSessionStateHolder.kt:410-411`). Casi todas las métricas gobernantes rozan o caen bajo su `MIN_*` (comisión n<6 en EM1; post-error pares <3 en usuarios buenos). El módulo frecuentemente devuelve `unavailable`/`confident=false` — es honesto pero significa "no mide". 20 trials es insuficiente para estimación estable de cualquiera de estos constructos.
2. **Banco de contenido finito (EM3/EM10):** 28 ítems memorizables → el ejercicio más valioso produce pseudoprogreso por recall en 2-3 sesiones. El holdout que lo defendería está "diferido a Ola 4" y no existe.
3. **Trend/longitudinal fabricado:** `aggregateOf` fija `trend = ProfileTrend.UNKNOWN` y `transferConfidence = NOT_DEMONSTRATED` SIEMPRE (`EmotionSessionStateHolder.kt:368,370`). No hay cálculo de mejora longitudinal real — el snippet de perfil no puede mostrar progreso porque no se computa. Honesto, pero el módulo no tiene forma de demostrar que mejora nada.
4. **Adaptación que no progresa la habilidad real:** el adaptativo mueve un eje (`STEP_UP=0.07`) sobre dificultad de TAREA (ratio no-go, discriminabilidad, ventana), no sobre el constructo. Más no-go o barras más parecidas no entrenan mejor regulación; entrenan la tarea.
5. **El "afecto" es decorativo en 4 de 6 ejercicios cableados:** go/no-go (palabra distractora cuyo efecto casi nunca se mide), 2-AFC (barras perceptivas neutras). Solo EM3/EM10 tienen contenido genuinamente emocional-cognitivo. El módulo es, en su mayoría, tareas de control cognitivo con etiqueta emocional.
6. **Seguridad sobre-ingenierizada vs. medición sub-ingenierizada:** hay un sistema elaborado de proxies de overload, pause governor, induction intensity 1-5, check-in (`OverloadDetector`, `PauseGovernor`, `EmotionSafetyLimits`). Es buen trabajo de seguridad. Pero se ha invertido más rigor en NO dañar que en MEDIR/ENTRENAR algo real. El módulo es seguro y honesto, pero su eficacia cognitiva es débil.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Cablear calibración metacognitiva (EM4/EM14) y explotar `CALIBRATION_ERROR`.** Es la capacidad de mayor valor entrenable y demostrable que YA tiene clave de métrica pero ningún generador. Pedir confianza (1-5) tras cada respuesta en CUALQUIER paradigma existente y medir Brier/calibración convierte 20 trials inertes en señal metacognitiva real, transversal a todos los ejercicios, sin banco nuevo. Coste bajo, impacto máximo.
2. **Arreglar EM3 (la joya) con banco generativo + holdout obligatorio + métrica de separación sobre las 4 categorías.** Es el único ejercicio con base teórica sólida de transferencia (granularidad/defusión). Hoy lo mata el banco de 28 ítems y la métrica que ignora impulso/hecho. Banco composicional de 200+ ítems, precisión-en-holdout como métrica gobernante, y explotar la matriz de confusión (ya calculada, hoy descartada) para feedback específico.
3. **Subir la potencia de medición y reducir a 3 paradigmas distintos.** Fusionar EM1/EM8, EM2/EM11, EM3/EM10 en 3 ejercicios con dificultad continua, y subir trials/sesión (≥30, idealmente con staircase que fije tasa de error/no-go target) para que las métricas gobernantes alcancen confianza de forma fiable. Con los slots liberados, añadir un paradigma genuinamente ausente (reappraisal o flexibilidad/cambio-de-regla). Esto convierte "6 ejercicios que aparentan variedad y rara vez miden" en "menos ejercicios, bien medidos, con una capacidad nueva real".

---

