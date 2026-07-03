# Creatividad

# MindBoost Creatividad — 8 ejercicios de calidad suprema

> Fronteras de seguridad vinculantes: perfil cognitivo funcional, práctica contra baseline personal, dificultad adaptativa. Sin diagnóstico, sin IQ, sin claims clínicos. Toda salida sin soporte estadístico = `null` (em-dash), nunca 0; `preliminary = true` por defecto. **AI bloqueado por `CreativityRuntimePolicy` (todos los flags false, G01..G10 sin pasar)**: por tanto, ningún ejercicio aquí depende de scoring de originalidad por IA. La "originalidad" se proxia LOCALMENTE por **rareza relativa al banco estático + diversidad intra-respuesta**, etiquetada honestamente como proxy de práctica, no medida de originalidad real. Cuando un ejercicio requiera evaluación semántica que solo la IA puede dar de forma robusta, su métrica de calidad arranca CALIBRATING hasta `isAiSurfaceUnlocked`.

Motores reales conectados: `CreativityRubricScorer` (fluency=#ideas distintas, elaboration=palabras/idea, `aiDerived=false` enforced en init, label `CreativityPracticeLabel`), `CreativityResponseValidator` (boundary: `TIMING_NOT_MONOTONIC`, `EMPTY_RESPONSE`, `BELOW_MIN_IDEAS`). Persistencia `SecureCreativitySessionStore` (AES-256-GCM+Keystore) + `CreativitySessionCoordinator`. Prompts de `CreativityPromptCatalog`. Adaptación: por el hueco de adaptación cross-módulo, cada ejercicio declara su `DifficultyVector` y se especifica el motor como **extensión determinista del patrón `DefaultAdaptiveEngine`** (a elevar a `mindboost/core` o reimplementar local `CreativityAdaptiveEngine`), nunca AI.

---

## 1. Supresión del Primer Impulso (Originalidad por Inhibición Prepotente)

### Módulo
Principal: Creatividad (pensamiento divergente / flexibilidad). Secundarios: Enfoque (control inhibitorio prepotente), Lógica (recombinación de restricciones).

### Objetivo cognitivo
Entrenar la inhibición de la primera respuesta dominante (la "respuesta de cima de mente") para forzar producción de alternativas no-obvias. Mide divergencia bajo la restricción de **prohibir la respuesta común**, atacando el principal cuello de botella del pensamiento divergente: la perseverancia en la categoría inicial.

### Hipótesis de transferencia
SÍ podría transferir (near): mayor fluidez ideacional y menor fijación funcional en variantes del mismo paradigma de usos alternativos. NO se debe afirmar: aumento de "creatividad general", innovación en dominios reales, IQ, ni far transfer. Copy: "práctica de generación de alternativas contra tu baseline".

### Loop principal
1. **Estímulo:** objeto cotidiano (de `CreativityPromptCatalog.EX01`, 8 objetos) + lista visible de N "usos prohibidos" (los más comunes, precomputados en el banco estático).
2. **Decisión:** el usuario teclea usos alternativos que NO estén en la lista prohibida ni sean sinónimos triviales.
3. **Feedback:** inmediato por idea — aceptada/rechazada-por-trivial/rechazada-por-prohibida (validación local, sin IA: match contra banco prohibido + heurística de solapamiento léxico).
4. **Adaptación:** ajusta nº de usos prohibidos y ventana temporal según fluency válida.
5. **Siguiente trial:** nuevo objeto, prohibiciones recalculadas.

### Reglas
Solo cuentan ideas que pasan `CreativityResponseValidator` (no vacías, ≥ minIdeas) y que no colisionan con el set prohibido (igualdad normalizada whole-word, accent-insensitive). Sinónimo trivial = solapamiento léxico ≥ umbral con una idea ya aceptada o prohibida → rechazo con feedback "ya cubierto". Sin penalización por intentar; solo no suma.

### Inputs
Texto libre (campo por idea, una idea por línea/enter). Tap para "enviar idea". Sin voz en v1.

### Variables de dificultad
1. `prohibitedCount` (usos prohibidos): 1–6.
2. `responseWindowMs`: 90000–30000 (más corto = más difícil).
3. `minIdeasTarget`: 3–10.
4. `trivialOverlapThreshold` (umbral solapamiento para rechazo): 0.7→0.4 (más estricto sube dificultad).
5. `objectAbstractness` (concreto→abstracto del banco): nivel 1–4.
6. `synonymBlockingOn`: bool (activa bloqueo de variantes morfológicas).
7. `categoryQuotaCap` (máx ideas por categoría semántica del banco antes de exigir saltar): 1–4.

### Algoritmo adaptativo
Banda objetivo: fluency válida ≈ 60–80% de `minIdeasTarget` dentro de la ventana. `DifficultyVector` ejes {INHIBITORY_PRESSURE=prohibitedCount, TEMPORAL_PRESSURE=responseWindowMs, CATEGORY_AMBIGUITY=categoryQuotaCap}. Staircase 1-up/2-down sobre eje principal `INHIBITORY_PRESSURE` con histéresis (subir cuesta 2 bloques buenos, bajar 1 malo). Gates de seguridad: si fatiga (caída de fluency intra-bloque > driftThreshold) o racha de 3 bloques bajo banda → STEP_DOWN/COOLDOWN antes de cualquier STEP_UP. Baseline personal ancla `prohibitedCount` inicial.

### Métricas primarias
`fluency` (vía `CreativityRubricScorer`: # ideas distintas válidas), `inhibitionSuccessRate` = ideas_no_prohibidas / intentos_totales.

### Métricas secundarias
`elaboration` (palabras/idea, `CreativityRubricScorer`), `categorySpread` = categorías distintas tocadas / categorías disponibles (proxy de flexibilidad), `noveltyProxy` = 1 − frecuencia_relativa_en_banco (CALIBRATING si banco insuficiente), `firstResponseLatencyMs` (latencia hasta primera idea válida = coste de inhibición).

### Señales de fatiga o mala calidad
Caída monotónica de fluency entre primer y último tercio del bloque; aumento de tasa de ideas triviales; `firstResponseLatencyMs` creciente sesión a sesión; ráfagas de envíos < 400ms (tecleo no-deliberado).

### Scoring sugerido
`practiceScore = w1*norm(fluency) + w2*inhibitionSuccessRate + w3*categorySpread`, con `w1=0.5,w2=0.3,w3=0.2`. `norm(fluency)=min(fluency/targetIdeas,1)`. Bar = `null` si n_ideas_validas < minN (confident=false). `noveltyProxy` NO entra al score total hasta tener banco suficiente (se reporta aparte, descriptivo).

### Antitrampa / calidad de datos
`CreativityResponseValidator.TIMING_NOT_MONOTONIC` (elapsed monotónico). Rechazo de duplicados/casi-duplicados (overlap). Detección de copy-paste masivo (varias ideas idénticas en banda <300ms). Banco prohibido evita que el "uso obvio" puntúe.

### Variantes
1. **Inversa:** dado un uso raro, generar el objeto que lo permite (constrain inverso).
2. **Cadena prohibida creciente:** cada idea aceptada se añade a las prohibidas del mismo objeto (presión inhibitoria acumulativa intra-trial).
3. **Multi-objeto:** combinar dos objetos para un uso conjunto (recombinación), prohibiendo el uso obvio de cada uno.

### UX
Pantalla: objeto centrado, lista de "prohibidos" en chip rojo (token semántico + icono candado, no solo color), campo de texto abajo (`MinTouchTarget=48dp`), contador de ideas válidas. Feedback: chip verde efímero al aceptar, shake breve (microanimación funcional) al rechazar trivial/prohibido. Duración ideal: 3–4 min/sesión, 4–6 trials. Sin animación decorativa.

### Implementación KMP
Reusa renderer **texto** (nuevo `CreativityTextEntryField`, no existe single-tap/choice aplicable). Modelo:
```
@Serializable data class SuppressTrialDto(val schemaVersion:Int=CreativitySchema.CURRENT, val objectId:String, val prohibited:List<String>, val accepted:List<String>, val rejectedTrivial:Int, val firstValidLatencyMs:Long, val elapsedMonotonicMs:Long, val difficulty:Map<String,Double>)
```
Eventos: `idea_submitted`, `idea_accepted/rejected_trivial/rejected_prohibited`, `trial_completed`. Estado mínimo: lista de ideas + set prohibido + clock (`EpochClock`, nunca `Clock.System`). Motores: `CreativityResponseValidator` (boundary), `CreativityRubricScorer` (fluency/elaboration). Persistencia `SecureCreativitySessionStore` (key `sess:<userId>::<sessionId>`), namespace `MindBoostStorageNamespaces.Creativity`. Adaptación: `CreativityAdaptiveEngine` local (patrón `DefaultAdaptiveEngine`). Registry→renderer: añadir a `CreativityPromptCatalog`/registry + `ExerciseCatalogConsistencyTest`. AI: NINGUNO (no toca `CreativityAiLayers`).

### Riesgos
`noveltyProxy` depende de calidad del banco estático → puede premiar rareza espuria. Solapamiento léxico no captura sinonimia semántica real (sin IA): falsos negativos en rechazo. Dependencia idiomática del banco (necesita banco por locale).

### Revisión crítica
Revisor Crítico: "Sin IA, tu `noveltyProxy` es teatro: mide rareza léxica contra un banco que TÚ controlas, no originalidad. Si el banco es pequeño (8 objetos), un usuario memoriza las prohibiciones y juega al banco, no a la creatividad — mides familiaridad con la UI/banco, justo lo prohibido. Además, `inhibitionSuccessRate` puede inflarse tecleando basura no-prohibida. EXIJO: (a) sacar `noveltyProxy` del score total (hecho), (b) rotación obligatoria de objetos y expansión del banco prohibido por sesión para impedir memorización, (c) que `inhibitionSuccessRate` solo cuente ideas que ADEMÁS pasen el filtro de trivialidad y aporten `elaboration` mínima, (d) holdout de objetos no vistos para detectar si la mejora es real o por familiaridad." Correcciones aceptadas e integradas en Reglas/Antitrampa/Variantes.

### Veredicto
Aprobado con cambios: sólido como inhibición divergente si se fuerza rotación de banco y novelty queda como señal descriptiva, no de score.

---

## 2. Rotación Forzada de Categorías (Flexibilidad / Switching Semántico)

### Módulo
Principal: Creatividad (flexibilidad cognitiva). Secundarios: Enfoque (set-shifting, espejo de E6 task-switch), Lógica.

### Objetivo cognitivo
Entrenar la flexibilidad: capacidad de abandonar una categoría semántica activa y saltar a otra distinta bajo demanda. Mide `switchCost` ideacional — el coste de cambiar de "marco" — y la profundidad por categoría antes del salto.

### Hipótesis de transferencia
SÍ (near): reducción de fijación categorial y mejor exploración del espacio de soluciones en variantes del paradigma. NO: flexibilidad de la personalidad, adaptabilidad vital, far transfer. Copy: "práctica de cambio de enfoque".

### Loop principal
1. **Estímulo:** tema semilla (`EX02_CATEGORY_ROTATION`, 6 temas) + un **cue de categoría activa** (p. ej. "objetos", "acciones", "lugares", "emociones-neutras") que cambia en trials de switch.
2. **Decisión:** generar ideas que pertenezcan a la categoría activa; al cambiar el cue, saltar de categoría.
3. **Feedback:** inmediato — idea válida-en-categoría / fuera-de-categoría; marca visual del switch.
4. **Adaptación:** frecuencia de switch y nº de categorías según `switchAccuracy` y `switchCost`.
5. **Siguiente trial:** nuevo tema o nuevo patrón de switch.

### Reglas
Idea válida = pertenece a la categoría activa (clasificación local por banco de pertenencia categoría↔léxico, no IA). `switchCost` solo se computa con **ideas válidas con latencia en AMBOS lados** del cambio (regla congruencia/switch, §2.1.4): si falta un lado → `null`. Cambiar de categoría con el cue antiguo = error de switch.

### Inputs
Texto libre + el cue de categoría es selección visible (no editable). Tap enviar.

### Variables de dificultad
1. `switchFrequency` (proporción trials con cambio): 0.2–0.7.
2. `categoryCount` (categorías en rotación): 2–5.
3. `responseWindowMs`: 80000–25000.
4. `minPerCategory` (ideas exigidas antes de permitir/forzar salto): 1–4.
5. `cuePersistenceMs` (cuánto se ve el cue antes de ocultarse): 4000–0 (0 = memorizar regla).
6. `categorySimilarity` (categorías cercanas vs lejanas): nivel 1–3 (cercanas = más interferencia).
7. `ambiguousItemRate` (proporción de ideas que caen en frontera de 2 categorías): 0–0.3.

### Algoritmo adaptativo
Banda: `switchAccuracy` ≈ 0.75–0.85. Eje principal `SET_SHIFT_DEMAND` (switchFrequency×categoryCount). Staircase 1-up/3-down (converge ~79%). Histéresis + dead-zone. Gate: `switchCost` creciente intra-sesión = fatiga de control → COOLDOWN/STEP_DOWN. Baseline ancla `categoryCount` inicial. Nunca sube con fatiga.

### Métricas primarias
`switchAccuracy` = switches correctos / switches totales. `switchCost` = mediana(RT_post-switch) − mediana(RT_no-switch), solo HIT-con-RT ambos lados (`null` si falta lado).

### Métricas secundarias
`fluency` total (`CreativityRubricScorer`), `categorySpread`, `perseverationErrors` = ideas en categoría anterior tras el cue nuevo, `withinCategoryDepth` = media de ideas válidas antes de salto, `flexibilityIndex` = categorySpread × (1 − perseverationRate).

### Señales de fatiga o mala calidad
`switchCost` y `perseverationErrors` crecientes; colapso de `categorySpread` (el usuario se ancla en 1 categoría); latencias post-switch con cola larga (lapsos).

### Scoring sugerido
`practiceScore = a*switchAccuracy + b*norm(1−switchCostNorm) + c*flexibilityIndex`, `a=0.45,b=0.25,c=0.30`. `switchCostNorm = clamp(switchCost/maxExpectedCost,0,1)`. Bar `null` si switches válidos < minN o `switchCost` no computable. preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`. Anti-spam (latencias <400ms → no cuentan). Detección de pegado de listas pre-categorizadas. `ambiguousItemRate` controla que el usuario no explote ítems-frontera para fingir switches.

### Variantes
1. **Switch impredecible:** cue cambia sin aviso a mitad de ventana (mide reactividad).
2. **Doble cue:** dos categorías activas simultáneas, alternar uno-y-otro (carga de working memory + switch).
3. **Switch por agotamiento:** forzar salto cuando la categoría actual se "agota" (no más ideas válidas), midiendo profundidad antes del bloqueo.

### UX
Pantalla: tema arriba, **cue de categoría persistente y visualmente distinto del estímulo** (color/forma/posición fija, no solo hue — accesibilidad), campo texto, indicador de switch (flash funcional). Microanimación: transición de cue marcada (200ms). Duración: 3–5 min. Reachability: lista de ideas con `LazyColumn`.

### Implementación KMP
Reusa renderer **texto** + un sub-componente cue (nuevo `CategoryCueBar`). Modelo:
```
@Serializable data class RotationTrialDto(val schemaVersion:Int, val themeId:String, val cueSequence:List<String>, val ideas:List<CategorizedIdeaDto>, val switchMarkers:List<Int>, val rtsMs:List<Long>, val difficulty:Map<String,Double>)
```
Eventos: `idea_in_category`, `idea_out_of_category`, `cue_switched`, `perseveration_detected`. Estado mínimo: categoría activa + buffer de ideas + RT por idea (`EpochClock`). Motores: `CreativityResponseValidator`, `CreativityRubricScorer`. `switchCost` lo computa un assembler local (autoridad de clasificación, espejo de `EmotionSessionAssembler`), NO la UI. Persistencia `SecureCreativitySessionStore`. AI: ninguno (clasificación categoría = banco local determinista).

### Riesgos
Clasificación categoría↔léxico sin IA es frágil para ítems ambiguos → ruido en `switchAccuracy`. Banco categorial dependiente de idioma. Si `categorySimilarity` alto, falsos errores.

### Revisión crítica
Revisor Crítico: "El `switchCost` ideacional NO es el switchCost de RT clásico — generar una idea tarda segundos y depende del léxico disponible, no del control ejecutivo puro. Estás midiendo disponibilidad léxica disfrazada de flexibilidad. Y tu clasificador de categorías local va a fallar en frontera, contaminando la métrica núcleo. EXIJO: (a) `switchCost` reportado como proxy explícito, no como medida ejecutiva; (b) banco categorial con ítems pre-clasificados y rechazo (no adivinanza) de ítems no en banco → ítem desconocido = no clasificado, no error; (c) `withinCategoryDepth` como métrica de respaldo cuando switchCost no es computable; (d) holdout de temas." Aceptado: ítem fuera de banco → `unclassified`, no penaliza; switchCost honesto como proxy.

### Veredicto
Aprobado con cambios: válido como flexibilidad ideacional si switchCost es proxy honesto y los ítems no clasificables no penalizan.

---

## 3. Síntesis Bajo Restricción (Combinatoria Forzada / Pensamiento Convergente-Divergente)

### Módulo
Principal: Creatividad (combinatoria, pensamiento divergente con constraint). Secundarios: Lógica (satisfacción de restricciones), Decisiones.

### Objetivo cognitivo
Entrenar la generación de soluciones que satisfagan **múltiples restricciones simultáneas** (combinatoria forzada), el motor de la creatividad aplicada real. Mide cuántas restricciones puede integrar el usuario manteniendo viabilidad y variedad.

### Hipótesis de transferencia
SÍ (near): mejor manejo de problemas con restricciones múltiples en variantes del paradigma. NO: capacidad de innovación profesional, diseño real, far transfer. Copy: "práctica de combinar requisitos".

### Loop principal
1. **Estímulo:** un objetivo (p. ej. "diseña un objeto que…") + 2–5 restricciones explícitas (tags del banco).
2. **Decisión:** proponer una solución textual que cumpla TODAS las restricciones.
3. **Feedback:** inmediato por restricción — cuáles cumple (check determinista por presencia de tags requeridos/prohibidos en la respuesta) + flag si trivial.
4. **Adaptación:** nº y dureza de restricciones según `constraintSatisfactionRate`.
5. **Siguiente trial:** nuevo objetivo + restricciones.

### Reglas
Solución válida = cumple todas las restricciones requeridas y ninguna prohibida (verificación local por presencia/ausencia de tags-clave del banco, anti-cheat por overlap). Cada restricción adicional satisfecha sube valor; soluciones que ignoran restricciones se rechazan. Variedad: soluciones casi-idénticas no recuentan.

### Inputs
Texto libre (solución elaborada). Selección múltiple opcional para taggear qué restricciones cree cubrir (autoevaluación → métrica de calibración). Tap enviar.

### Variables de dificultad
1. `constraintCount`: 2–6.
2. `constraintConflict` (grado de tensión entre restricciones): nivel 0–3 (conflictivas = más difícil).
3. `prohibitedTagCount`: 0–4.
4. `responseWindowMs`: 120000–40000.
5. `minElaborationWords`: 4–15.
6. `solutionsPerTrial` (cuántas soluciones distintas se piden): 1–4.
7. `abstractnessOfGoal`: nivel 1–4.

### Algoritmo adaptativo
Banda: `constraintSatisfactionRate` ≈ 0.75. Eje `CONSTRAINT_LOAD` (constraintCount×constraintConflict). Staircase 1-up/2-down. Histéresis. Gate fatiga: caída de elaboración + satisfacción → STEP_DOWN. Baseline ancla `constraintCount`. Deload tras plateau (sin slope N sesiones).

### Métricas primarias
`constraintSatisfactionRate` = restricciones_cumplidas / restricciones_requeridas (media sobre soluciones). `viableSolutionCount` = soluciones que cumplen todas.

### Métricas secundarias
`elaboration` (`CreativityRubricScorer`), `solutionVariety` = soluciones distintas / soluciones enviadas, `constraintIntegrationDepth` = máx restricciones cumplidas simultáneamente, `calibrationError` = |restricciones que cree cumplir − que realmente cumple| (metacognición, §2.2.11).

### Señales de fatiga o mala calidad
Soluciones que cumplen menos restricciones con el tiempo; aumento de `calibrationError`; soluciones repetidas; abandono (window timeout sin envío).

### Scoring sugerido
`practiceScore = p*constraintSatisfactionRate + q*norm(constraintIntegrationDepth) + r*solutionVariety − s*calibrationErrorNorm`, `p=0.4,q=0.3,r=0.2,s=0.1`. Bar `null` si soluciones válidas < minN. preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`, `EMPTY_RESPONSE`. Verificación de tags requeridos por presencia real (no por autoevaluación, que solo alimenta calibración). Rechazo de soluciones que copian el enunciado. Overlap inter-solución para variedad.

### Variantes
1. **Restricción oculta:** una restricción se revela solo tras el primer intento (adaptabilidad).
2. **Conflicto duro:** dos restricciones mutuamente tensionadas, midiendo soluciones de compromiso.
3. **Escalera de restricciones:** cada solución válida añade una restricción nueva al mismo objetivo (carga creciente intra-trial).

### UX
Pantalla: objetivo arriba, restricciones como chips (verde al cumplir en tiempo real, gris pendiente — icono + color), campo texto amplio, contador de restricciones cumplidas. Microanimación: chip que "se enciende" al detectar cumplimiento (funcional). Duración: 4–6 min. `verticalScroll`.

### Implementación KMP
Reusa renderer **texto** + chips de restricción (nuevo `ConstraintChipsRow`). Modelo:
```
@Serializable data class SynthesisTrialDto(val schemaVersion:Int, val goalId:String, val requiredTags:List<String>, val prohibitedTags:List<String>, val solutions:List<SolutionDto>, val selfTaggedCovered:List<List<String>>, val difficulty:Map<String,Double>)
```
Eventos: `solution_submitted`, `constraint_satisfied`, `constraint_violated`, `calibration_recorded`. Estado mínimo: solución actual + estado de cada restricción + clock. Motores: `CreativityResponseValidator`, `CreativityRubricScorer` (elaboration). Verificación de tags = matcher local determinista (whole-word, accent-insensitive, estilo `EmotionTerminologyScanner`). Persistencia `SecureCreativitySessionStore`. AI: ninguno.

### Riesgos
Verificación de cumplimiento por tags-clave es superficial: una solución puede mencionar el tag sin cumplir el espíritu de la restricción (sin IA no hay comprensión semántica). `constraintConflict` mal calibrado puede hacer trials imposibles.

### Revisión crítica
Revisor Crítico: "Tu 'verificación de restricción por presencia de tag' es trivialmente gameable: escribo el tag literal y 'cumplo'. Estás midiendo si el usuario incluye palabras-clave, no si resuelve. Eso es lo opuesto a creatividad: premia keyword-stuffing. Sin IA no puedes verificar satisfacción real. EXIJO: (a) que el cumplimiento exija el tag MÁS elaboración mínima coherente alrededor (ventana de palabras), no el tag suelto; (b) que `constraintConflict` se valide con un banco de restricciones cuya tensión esté pre-modelada (no generada al azar) para garantizar resolubilidad; (c) marcar la métrica de calidad como CALIBRATING hasta `isAiSurfaceUnlocked`, dejando solo `constraintSatisfactionRate` (presencia+elaboración) como señal de práctica honesta; (d) calibrationError es buena adición, mantenla." Aceptado: cumplimiento = tag + ventana de elaboración; calidad semántica CALIBRATING sin IA.

### Veredicto
Aprobado con cambios: válido como práctica de combinatoria si el cumplimiento exige elaboración (no keyword) y la calidad semántica queda CALIBRATING hasta IA.

---

## 4. Analogías Remotas (Conexión de Dominios Distantes)

### Módulo
Principal: Creatividad (pensamiento asociativo / conectar dominios remotos). Secundarios: Lógica (mapeo estructural), Lenguaje (relaciones semánticas).

### Objetivo cognitivo
Entrenar la asociación remota: conectar dos conceptos de dominios distantes mediante un puente explicado. Proxia el componente "asociativo" de la creatividad (base del RAT/insight), midiendo distancia del puente y su validez estructural.

### Hipótesis de transferencia
SÍ (near): mejor producción de analogías y conexiones en variantes del paradigma. NO: pensamiento lateral general, insight en problemas reales, far transfer, IQ. Copy: "práctica de conectar ideas".

### Loop principal
1. **Estímulo:** dos conceptos (A de dominio X, B de dominio Y, distancia controlada por banco).
2. **Decisión:** escribir el puente/analogía que los conecta ("A es como B porque…").
3. **Feedback:** inmediato — puente aceptado (tiene estructura mínima: relación + justificación, verificada localmente) / rechazado-trivial.
4. **Adaptación:** distancia entre dominios y exigencia estructural según calidad estructural.
5. **Siguiente trial:** nuevo par.

### Reglas
Puente válido = contiene una relación explícita y una justificación (heurística local: longitud mínima + presencia de conector relacional del banco + no-tautología). Distancia de dominios la fija el banco (pares pre-etiquetados por distancia semántica). Puentes triviales (sinonimia, misma categoría) rechazados.

### Inputs
Texto libre (el puente). Opcional: selección de "tipo de relación" (causal/funcional/estructural/perceptual) → métrica de variedad relacional. Tap enviar.

### Variables de dificultad
1. `domainDistance` (cercano→remoto, banco): nivel 1–5.
2. `responseWindowMs`: 90000–30000.
3. `minBridgeWords`: 5–20.
4. `forbiddenRelationType` (prohíbe el tipo de relación obvio): 0–2 tipos.
5. `pairsPerTrial`: 1–3.
6. `abstractnessBalance` (concreto-concreto → abstracto-abstracto): nivel 1–4.
7. `requireMultiHop` (exigir puente de 2 saltos A→C→B): bool.

### Algoritmo adaptativo
Banda: tasa de puentes aceptados ≈ 0.7–0.85. Eje `ASSOCIATIVE_DISTANCE` (domainDistance). Staircase 1-up/2-down con histéresis. Gate fatiga: caída de `bridgeAcceptanceRate` + latencia creciente → STEP_DOWN. Baseline ancla `domainDistance`. Deload por plateau.

### Métricas primarias
`bridgeAcceptanceRate` = puentes válidos / intentos. `meanDomainDistanceSolved` = distancia media de pares con puente válido (proxy de alcance asociativo).

### Métricas secundarias
`elaboration` (`CreativityRubricScorer`), `relationVariety` = tipos de relación distintos usados / disponibles, `bridgeLatencyMs` (tiempo a primer puente válido), `multiHopRate` (en variante).

### Señales de fatiga o mala calidad
`bridgeLatencyMs` creciente; colapso a un solo tipo de relación; puentes cada vez más cortos/triviales; timeouts.

### Scoring sugerido
`practiceScore = m*bridgeAcceptanceRate + n*norm(meanDomainDistanceSolved) + o*relationVariety`, `m=0.45,n=0.35,o=0.20`. Bar `null` si puentes válidos < minN. preliminary=true. La "calidad" del puente (más allá de estructura) = CALIBRATING sin IA.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`, `EMPTY_RESPONSE`. Detección de tautología (A y B aparecen sin relación nueva). Rechazo de plantillas repetidas ("es como… porque ambos existen"). Overlap entre puentes para evitar copia.

### Variantes
1. **Triple remoto:** tres conceptos, un puente que una a los tres.
2. **Puente prohibido:** se prohíbe la relación más obvia, forzando una conexión más remota.
3. **Cadena de analogías:** A:B :: C:? — completar la proporción manteniendo la relación (mapeo estructural puro).

### UX
Pantalla: dos conceptos en tarjetas separadas (distancia visual = distancia de dominio, metáfora espacial funcional), campo texto entre ellas, selector de tipo de relación (chips). Microanimación: "línea" que conecta las tarjetas al aceptar (funcional, 250ms). Duración: 4–5 min.

### Implementación KMP
Reusa renderer **texto** + sub-componente par-de-conceptos (nuevo `ConceptPairField`). Modelo:
```
@Serializable data class AnalogyTrialDto(val schemaVersion:Int, val conceptAId:String, val conceptBId:String, val domainDistance:Int, val bridgeText:String, val relationType:String?, val accepted:Boolean, val bridgeLatencyMs:Long, val difficulty:Map<String,Double>)
```
Eventos: `bridge_submitted`, `bridge_accepted/rejected`, `relation_selected`. Estado mínimo: par actual + puente + clock. Motores: `CreativityResponseValidator`, `CreativityRubricScorer`. Validación estructural del puente = heurística local (conectores relacionales del banco, longitud, no-tautología). Banco de pares en `CreativityPromptCatalog` (nuevo pool `EX04_REMOTE_ANALOGIES`, pre-etiquetado por distancia). Persistencia `SecureCreativitySessionStore`. AI: ninguno (calidad semántica deferida a `BlockedCreativityEvaluatorCascade`).

### Riesgos
Sin IA, "puente válido" = puente con estructura, no puente bueno: premia forma sobre fondo. Distancia de dominios depende totalmente del etiquetado del banco. Dependencia idiomática alta (conectores relacionales por locale).

### Revisión crítica
Revisor Crítico: "Esto es lo más dependiente de IA de todo el catálogo y lo sabes. Sin evaluación semántica, un puente sintácticamente correcto pero absurdo pasa tu filtro. Estás midiendo si el usuario sabe escribir 'porque ambos…', no si conecta dominios. El `meanDomainDistanceSolved` es circular: 'resuelto' = pasó tu heurística débil. EXIJO: (a) declarar explícitamente que `bridgeAcceptanceRate` mide ESTRUCTURA del puente, no calidad de la conexión, y que la calidad real es CALIBRATING hasta IA (hecho); (b) la variante 3 (analogía proporcional A:B::C:?) SÍ es verificable localmente contra el banco — priorízala como el sub-ejercicio honesto sin IA; (c) anti-plantilla agresivo o el ejercicio degenera en rellenar una frase fija; (d) holdout de pares." Aceptado: variante proporcional como núcleo verificable; resto CALIBRATING.

### Veredicto
Aprobado con cambios: honesto solo si la calidad del puente se declara CALIBRATING (sin IA) y la variante proporcional A:B::C:? es el modo verificable principal.

---

## 5. Elaboración Incremental (Profundización Progresiva de una Idea)

### Módulo
Principal: Creatividad (elaboración, la 2ª señal real del `CreativityRubricScorer`). Secundarios: Enfoque (persistencia atencional), Lenguaje.

### Objetivo cognitivo
Entrenar la elaboración: tomar una idea-semilla y profundizarla en capas sucesivas (detalle, consecuencia, mejora, excepción), en lugar de saltar a ideas nuevas. Ataca la superficialidad ideacional; mide profundidad sostenida.

### Hipótesis de transferencia
SÍ (near): mayor profundidad y persistencia en el desarrollo de ideas en variantes del paradigma. NO: capacidad de ejecución real de proyectos, far transfer. Copy: "práctica de desarrollar una idea a fondo".

### Loop principal
1. **Estímulo:** idea-semilla + un **prompt de capa** rotativo ("añade un detalle", "una consecuencia", "una mejora", "una excepción/riesgo").
2. **Decisión:** escribir la capa pedida, construyendo sobre lo anterior (no reiniciar).
3. **Feedback:** inmediato — capa aceptada (aporta contenido nuevo coherente con la anterior, verificado por no-solapamiento + longitud) / rechazada-redundante.
4. **Adaptación:** profundidad exigida y variedad de capas según `depthReached`.
5. **Siguiente trial:** nueva semilla.

### Reglas
Capa válida = aporta contenido nuevo (overlap bajo con capas previas), responde al tipo de capa pedido, y supera elaboración mínima. Reiniciar con idea distinta = rechazo (es elaboración, no fluidez). Profundidad = nº de capas válidas encadenadas.

### Inputs
Texto libre por capa. Tap "añadir capa". El prompt de capa es visible (no editable).

### Variables de dificultad
1. `targetDepth` (capas exigidas): 3–10.
2. `layerTypeVariety` (tipos de capa en rotación): 2–5.
3. `minWordsPerLayer`: 4–15.
4. `redundancyThreshold` (overlap permitido con capas previas): 0.5→0.25 (más estricto = difícil).
5. `responseWindowMs` por capa: 40000–12000.
6. `forbidRestart` (penaliza saltar a idea nueva): bool.
7. `crossLayerConstraint` (cada capa debe referenciar la anterior): bool.

### Algoritmo adaptativo
Banda: tasa de capas válidas ≈ 0.75–0.85. Eje `ELABORATION_DEPTH` (targetDepth). Staircase 1-up/2-down con histéresis. Gate fatiga: caída de calidad de capa + abandono → STEP_DOWN/COOLDOWN. Baseline ancla `targetDepth`. Plateau → deload (semillas más fáciles).

### Métricas primarias
`elaboration` (`CreativityRubricScorer`: palabras/idea, aquí palabras/capa) y `depthReached` = capas válidas encadenadas.

### Métricas secundarias
`layerTypeSpread` = tipos de capa completados / disponibles, `redundancyRate` = capas rechazadas por redundancia / totales, `coherenceProxy` = solapamiento moderado con capa previa (señal de "construir sobre", no copiar ni desconectar — banda óptima), `depthLatencyMs` por capa.

### Señales de fatiga o mala calidad
`depthReached` decreciente; `redundancyRate` creciente; capas cada vez más cortas; abandono antes de targetDepth.

### Scoring sugerido
`practiceScore = e*norm(depthReached) + f*norm(elaboration) + g*layerTypeSpread − h*redundancyRateNorm`, `e=0.4,f=0.3,g=0.2,h=0.1`. `coherenceProxy` fuera de banda (ni copia ni desconexión) bonifica levemente. Bar `null` si capas válidas < minN. preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`. Anti-redundancia (overlap alto = rechazo). Anti-desconexión (overlap nulo con TODA la cadena = posible reinicio encubierto → flag). Detección de relleno (capas largas sin contenido nuevo via novelty léxica intra-cadena).

### Variantes
1. **Profundización dirigida:** todas las capas del mismo tipo (p. ej. solo "consecuencias de consecuencias") = profundidad pura.
2. **Árbol:** ramificar la idea en 2 sub-ideas y elaborar ambas (elaboración + estructura).
3. **Capa adversa:** alternar "mejora" y "riesgo" forzando pensamiento dialéctico sobre la misma semilla.

### UX
Pantalla: idea-semilla fija arriba, capas previas apiladas (scroll, `LazyColumn`), prompt de capa actual destacado, campo texto abajo, indicador de profundidad (barra de capas). Microanimación: nueva capa "se acopla" a la pila (funcional). Duración: 4–6 min. Cada capa visible para construir encima.

### Implementación KMP
Reusa renderer **texto** + pila de capas (nuevo `LayerStackField`). Modelo:
```
@Serializable data class ElaborationTrialDto(val schemaVersion:Int, val seedId:String, val layers:List<LayerDto>, val layerPrompts:List<String>, val depthReached:Int, val rejectedRedundant:Int, val difficulty:Map<String,Double>)
data class LayerDto(val type:String, val text:String, val accepted:Boolean, val latencyMs:Long, val overlapPrev:Double)
```
Eventos: `layer_submitted`, `layer_accepted/rejected_redundant`, `restart_detected`. Estado mínimo: cadena de capas + tipo actual + clock. Motores: `CreativityRubricScorer` (elaboration central aquí), `CreativityResponseValidator`. Overlap/coherencia = utilidad local determinista. Persistencia `SecureCreativitySessionStore`. AI: ninguno.

### Riesgos
`elaboration`=palabras/capa premia verbosidad, no profundidad real (sin IA no se distingue). `coherenceProxy` por overlap léxico es burdo. Posible incentivo a escribir largo y vacío.

### Revisión crítica
Revisor Crítico: "`elaboration` = palabras/idea es una métrica de cantidad, no de profundidad. Premias al que teclea más, no al que piensa más hondo. Tu `coherenceProxy` por solapamiento léxico es ruido: una capa profunda puede usar vocabulario nuevo (overlap bajo) y tú la marcas 'desconectada'. EXIJO: (a) cap superior de `minWordsPerLayer` y techo de palabras que cuentan, para que verbosidad no infle el score; (b) `coherenceProxy` con banda (ni 0 ni 1), no monótona — ya lo tienes, pero hazlo el eje, no bonus; (c) la variante adversa (mejora/riesgo) es la más medible — un riesgo válido debe introducir un término negativo/condicional del banco, verificable; priorízala; (d) sin esto, mides logorrea." Aceptado: techo de palabras, coherenceProxy con banda, variante adversa priorizada.

### Veredicto
Aprobado con cambios: válido si elaboración tiene techo anti-verbosidad y la coherencia se mide por banda, no por longitud.

---

## 6. Detección de lo Inverosímil (Originalidad por Restricción Negativa)

### Módulo
Principal: Creatividad (originalidad medible localmente sin IA, vía rareza relativa al banco). Secundarios: Lógica, Enfoque.

### Objetivo cognitivo
Entrenar la producción de respuestas **raras-pero-válidas** mediante un mecanismo que SÍ es medible sin IA: el banco estático precomputa la distribución de respuestas comunes; el usuario es premiado por respuestas válidas de baja frecuencia. Convierte "originalidad" en un proxy honesto y auditable: rareza relativa a un banco de referencia.

### Hipótesis de transferencia
SÍ (near): mayor tendencia a explorar la cola de la distribución de ideas en variantes del paradigma. NO: originalidad creativa general, far transfer. Copy explícito: "rareza respecto a respuestas comunes, no medida de originalidad absoluta".

### Loop principal
1. **Estímulo:** prompt con un banco de respuestas pre-rankeadas por frecuencia (común→raro), oculto al usuario.
2. **Decisión:** dar respuestas válidas; el sistema las puntúa por su posición en la cola de frecuencia.
3. **Feedback:** inmediato — válida + indicador de rareza (común/poco común/rara), sin revelar el banco completo (anti-gaming).
4. **Adaptación:** umbral de rareza exigido y validación de plausibilidad según `rarityScore`.
5. **Siguiente trial:** nuevo prompt.

### Reglas
Respuesta válida = pasa filtro de plausibilidad (presente en banco de válidas O aprobada por heurística de no-absurdo local) Y no duplicada. Rareza = 1 − frecuencia_relativa_en_banco. Respuestas fuera de banco que pasan plausibilidad heurística → rareza máxima provisional (marcada para revisión, no infla score hasta confirmar banco). Anti-gaming: no se revela qué respuestas faltan.

### Inputs
Texto libre (respuestas cortas). Tap enviar.

### Variables de dificultad
1. `rarityTarget` (percentil de rareza exigido para "buena"): 50–90.
2. `responseWindowMs`: 70000–25000.
3. `minRareIdeas` (ideas raras exigidas): 1–6.
4. `plausibilityStrictness` (umbral heurística no-absurdo): nivel 1–4.
5. `commonResponsePenalty` (penalización a respuestas top-frecuencia): 0–0.5.
6. `bankExposureControl` (cuánto feedback de rareza se da): completo→solo binario.
7. `promptAbstractness`: nivel 1–4.

### Algoritmo adaptativo
Banda: proporción de ideas en el percentil objetivo ≈ 0.5–0.7. Eje `RARITY_DEMAND` (rarityTarget). Staircase 1-up/2-down con histéresis. Gate fatiga: caída de `rarityScore` + más respuestas comunes → STEP_DOWN. Baseline ancla `rarityTarget`. Plateau → rotación de prompts.

### Métricas primarias
`rarityScore` = media de (1 − freq_rel) de ideas válidas (proxy de originalidad, declarado). `validRareCount` = ideas válidas en percentil objetivo.

### Métricas secundarias
`fluency` (`CreativityRubricScorer`), `plausibilityRate` = válidas / enviadas (control de que rareza no es absurdo), `coverageOfTail` = ideas raras distintas / cola disponible, `elaboration`.

### Señales de fatiga o mala calidad
Deriva hacia respuestas comunes; aumento de respuestas implausibles (rareza por absurdo, no por creatividad); `plausibilityRate` cayendo; timeouts.

### Scoring sugerido
`practiceScore = u*rarityScore*plausibilityRate + v*norm(validRareCount) + w*coverageOfTail`, `u=0.5,v=0.3,w=0.2`. **Crucial:** `rarityScore` se multiplica por `plausibilityRate` para impedir que el absurdo gane. Bar `null` si válidas < minN o banco insuficiente (rarityScore=CALIBRATING). preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`, `EMPTY_RESPONSE`. Plausibilidad obligatoria (rareza sin plausibilidad = no cuenta). Banco oculto (no revelar la cola → no se puede memorizar el target). Detección de respuestas-basura raras (alta rareza + baja plausibilidad → flag). Rotación de prompts anti-memorización.

### Variantes
1. **Cola pura:** solo cuentan respuestas bajo cierto percentil de frecuencia (presión de rareza extrema).
2. **Rareza plausible justificada:** cada respuesta rara debe acompañarse de una justificación de plausibilidad (combina con elaboración).
3. **Anti-banco vivo:** las respuestas raras del propio usuario se añaden al banco común para siguientes sesiones (la rareza se vuelve relativa a su propio histórico = progresión contra baseline).

### UX
Pantalla: prompt arriba, campo texto, medidor de rareza por idea (común=gris, raro=destacado — icono + intensidad, no solo hue), contador de ideas raras válidas. Microanimación: "destello" en idea rara plausible (funcional). Duración: 3–5 min.

### Implementación KMP
Reusa renderer **texto** + medidor de rareza (nuevo `RarityMeterField`). Modelo:
```
@Serializable data class RarityTrialDto(val schemaVersion:Int, val promptId:String, val responses:List<RarityResponseDto>, val rarityTarget:Int, val difficulty:Map<String,Double>)
data class RarityResponseDto(val text:String, val freqRel:Double?, val plausible:Boolean, val rarityScore:Double?, val latencyMs:Long)
```
Eventos: `response_submitted`, `response_rare/common/implausible`, `tail_covered`. Estado mínimo: respuestas + banco de frecuencias del prompt (cargado de `CreativityPromptCatalog`, nuevo pool `EX06_FREQUENCY_BANK`) + clock. Motores: `CreativityResponseValidator`, `CreativityRubricScorer`. Frecuencia = lookup determinista en banco; plausibilidad = filtro heurístico local (en banco de válidas o heurística). Persistencia `SecureCreativitySessionStore` (variante 3 actualiza banco por-usuario cifrado). AI: ninguno — este es el ejercicio diseñado para ser honesto SIN IA.

### Riesgos
Calidad total depende del banco de frecuencias (sesgo de quien lo curó). "Plausibilidad heurística" sin IA es débil → absurdos podrían colarse. Variante 3 puede inflar rareza relativa artificialmente si el histórico es corto.

### Revisión crítica
Revisor Crítico: "Por fin un intento honesto de originalidad sin IA — pero tu banco es el punto único de fallo. Si la frecuencia está mal estimada, premias rareza falsa. Y tu 'plausibilidad heurística' es el mismo problema de siempre: sin comprensión, un absurdo bien formado pasa. El `rarityScore*plausibilityRate` mitiga, pero `plausibilityRate` sigue siendo débil. EXIJO: (a) que el banco de frecuencias declare su tamaño/cobertura y que `rarityScore` sea CALIBRATING si la cobertura es baja (hecho); (b) plausibilidad = pertenencia a banco de válidas PRIORITARIO sobre heurística — heurística solo da rareza provisional NO puntuable hasta confirmación humana/banco (hecho); (c) variante 3 con mínimo de histórico antes de activar rareza relativa personal; (d) rotación dura de prompts. Con esto, es el ejercicio más defendible del set." Aceptado todo.

### Veredicto
Aprobado: el ejercicio de originalidad más honesto sin IA — rareza relativa a banco auditado × plausibilidad, con CALIBRATING ante banco insuficiente.

---

## 7. Reencuadre del Problema (Flexibilidad de Marco / Problem-Finding)

### Módulo
Principal: Creatividad (problem-finding / reencuadre, fase pre-ideación de la creatividad real). Secundarios: Decisiones, Lógica, Lenguaje.

### Objetivo cognitivo
Entrenar el reencuadre: ante un problema dado, generar formulaciones alternativas del problema mismo ("¿y si el problema fuera realmente X?") antes de resolver. Mide diversidad de marcos y abstracción/concreción del replanteo. Es la habilidad creativa más infravalorada y medible por estructura.

### Hipótesis de transferencia
SÍ (near): mayor tendencia a cuestionar el enunciado y producir reformulaciones en variantes del paradigma. NO: capacidad de resolver problemas reales, innovación, far transfer. Copy: "práctica de replantear problemas".

### Loop principal
1. **Estímulo:** un problema/situación (banco) con un marco por defecto explícito.
2. **Decisión:** producir reformulaciones del problema en marcos distintos (cambiar el sujeto, el objetivo, la escala, la suposición oculta).
3. **Feedback:** inmediato — reformulación válida (cambia ≥1 dimensión del marco respecto al original y entre sí) / rechazada-mismo-marco.
4. **Adaptación:** nº de marcos exigidos y dimensiones a variar según `frameDiversity`.
5. **Siguiente trial:** nuevo problema.

### Reglas
Reformulación válida = altera al menos una **dimensión de marco** detectable (sujeto/objetivo/escala/restricción/temporalidad, verificada por banco de marcadores + no-solapamiento con reformulaciones previas). Reformulaciones del mismo marco rechazadas. La dificultad exige variar dimensiones específicas.

### Inputs
Texto libre por reformulación + selección de "qué dimensión cambié" (autoevaluación → calibración). Tap enviar.

### Variables de dificultad
1. `framesRequired` (reformulaciones exigidas): 2–7.
2. `dimensionsToVary` (dimensiones de marco distintas exigidas): 1–5.
3. `responseWindowMs`: 100000–35000.
4. `forbiddenDimension` (prohíbe variar la dimensión obvia): 0–2.
5. `minWordsPerFrame`: 5–15.
6. `problemAbstractness`: nivel 1–4.
7. `requireOrthogonal` (reformulaciones deben ser ortogonales entre sí, no solo del original): bool.

### Algoritmo adaptativo
Banda: tasa de reformulaciones válidas ≈ 0.7–0.85. Eje `FRAME_FLEXIBILITY` (framesRequired×dimensionsToVary). Staircase 1-up/2-down, histéresis. Gate fatiga: colapso de `frameDiversity` (todas en una dimensión) + latencia → STEP_DOWN. Baseline ancla `framesRequired`. Plateau → deload.

### Métricas primarias
`frameDiversity` = dimensiones de marco distintas alteradas / dimensiones disponibles. `validReframeCount` = reformulaciones válidas distintas.

### Métricas secundarias
`elaboration` (`CreativityRubricScorer`), `orthogonalityRate` = pares de reformulaciones mutuamente distintas / pares totales, `calibrationError` = |dimensión declarada − dimensión detectada| (metacognición), `reframeLatencyMs`.

### Señales de fatiga o mala calidad
Reformulaciones que repiten dimensión; `calibrationError` creciente; reformulaciones cada vez más cercanas al original; timeouts.

### Scoring sugerido
`practiceScore = x*frameDiversity + y*norm(validReframeCount) + z*orthogonalityRate − k*calibrationErrorNorm`, `x=0.4,y=0.3,z=0.2,k=0.1`. Bar `null` si válidas < minN. preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`, `EMPTY_RESPONSE`. Detección de dimensión por marcadores del banco (verificación, no autoevaluación; autoevaluación solo para calibración). Overlap entre reformulaciones (ortogonalidad). Rechazo de reformulación = paráfrasis del original (overlap alto con enunciado).

### Variantes
1. **Inversión:** reformular invirtiendo el objetivo ("cómo lograr lo contrario") como dimensión forzada.
2. **Escala:** reformular el mismo problema a 3 escalas (individual/grupo/sistema).
3. **Suposición oculta:** identificar y eliminar una suposición del enunciado, reformulando sin ella.

### UX
Pantalla: problema/enunciado fijo arriba (marco por defecto resaltado), reformulaciones previas apiladas (`LazyColumn`), selector de dimensión (chips), campo texto. Microanimación: reformulación válida "se separa" del original (metáfora de distancia de marco, funcional). Duración: 4–6 min.

### Implementación KMP
Reusa renderer **texto** + selector de dimensión (nuevo `FrameDimensionChips`). Modelo:
```
@Serializable data class ReframeTrialDto(val schemaVersion:Int, val problemId:String, val defaultFrame:String, val reframes:List<ReframeDto>, val difficulty:Map<String,Double>)
data class ReframeDto(val text:String, val detectedDimension:String?, val declaredDimension:String?, val accepted:Boolean, val latencyMs:Long, val overlapOriginal:Double)
```
Eventos: `reframe_submitted`, `reframe_accepted/rejected_same_frame`, `dimension_declared`. Estado mínimo: reformulaciones + dimensiones cubiertas + clock. Motores: `CreativityResponseValidator`, `CreativityRubricScorer`. Detección de dimensión = matcher local (marcadores de sujeto/objetivo/escala/etc. en banco `EX07_REFRAME_PROBLEMS`). Persistencia `SecureCreativitySessionStore`. AI: ninguno.

### Riesgos
Detección de "dimensión de marco" por marcadores léxicos es aproximada → falsos negativos/positivos. Banco de problemas dependiente de idioma/cultura. Concepto abstracto: riesgo de que el usuario no entienda la tarea (curva de aprendizaje).

### Revisión crítica
Revisor Crítico: "El reencuadre es genuinamente creativo, pero tu detección de 'dimensión de marco cambiada' por marcadores léxicos es casi adivinanza — sin comprensión, no sabes si el usuario realmente reformuló o solo parafraseó con palabras de otra dimensión. El `frameDiversity` se vuelve 'usó vocabulario de N categorías'. Además la tarea es conceptualmente difícil: riesgo de medir comprensión-de-instrucciones, no creatividad. EXIJO: (a) onboarding/ejemplo obligatorio antes del primer trial (mitiga curva); (b) `frameDiversity` declarado como proxy por marcadores, calidad real CALIBRATING sin IA; (c) la variante 'escala' (individual/grupo/sistema) es la más verificable localmente — priorízala como modo de entrada; (d) calibrationError es excelente aquí, mantenla como señal metacognitiva central." Aceptado: onboarding, escala como modo entrada, frameDiversity como proxy.

### Veredicto
Aprobado con cambios: potente como problem-finding si lleva onboarding, la variante escala es el modo de entrada y frameDiversity se declara proxy.

---

## 8. Improvisación Restringida en Tiempo Real (Fluidez Bajo Presión Temporal)

### Módulo
Principal: Creatividad (fluidez ideacional bajo presión, flow). Secundarios: Enfoque (sostenimiento atencional), Velocidad (procesamiento), Emociones (tolerancia a presión, leve).

### Objetivo cognitivo
Entrenar la fluidez ideacional sostenida bajo presión temporal: producir ideas válidas continuas sin congelarse, midiendo throughput ideacional y su estabilidad. Combina cantidad (fluency) con ritmo (inter-idea intervals), atacando el "bloqueo" y la auto-censura prematura.

### Hipótesis de transferencia
SÍ (near): mayor fluidez y menor bloqueo ideacional en variantes del paradigma. NO: creatividad bajo presión real laboral, far transfer. Copy: "práctica de generar ideas sin bloquearte". Restricción de seguridad: presión leve-only (espejo `InductionIntensity OPERATIONAL_CEILING=3`); nunca presión que induzca estrés clínico.

### Loop principal
1. **Estímulo:** prompt + temporizador continuo + objetivo de throughput (ideas/min) visible.
2. **Decisión:** teclear ideas válidas lo más continuamente posible; gaps largos penalizan ritmo, no validez.
3. **Feedback:** inmediato — idea válida/inválida + indicador de ritmo (en banda / bajo).
4. **Adaptación:** throughput objetivo y ventana según `validIdeaRate` y estabilidad de ritmo.
5. **Siguiente trial:** nuevo prompt.

### Reglas
Idea válida = pasa `CreativityResponseValidator` + no duplicada + elaboración mínima. Throughput cuenta ideas válidas/tiempo. Ritmo = consistencia de intervalos inter-idea (CV de IBI). Presión leve: si proxies de sobrecarga (racha de bloqueos, abandono) → check-in/reduce (NUNCA infiere estado emocional, solo rendimiento — espejo `OverloadProxy`).

### Inputs
Texto libre rápido (enter = enviar idea, loop muy corto). Tap opcional. Sin voz v1.

### Variables de dificultad
1. `targetThroughput` (ideas válidas/min): 4–15.
2. `sessionWindowMs`: 120000–45000.
3. `minIdeaWords`: 2–8.
4. `validityStrictness` (umbral plausibilidad/no-duplicado): nivel 1–4.
5. `pressureCueIntensity` (intensidad del feedback de presión, leve-only): 1–3 (techo 3, HARD).
6. `promptSwitchRate` (cambios de prompt intra-sesión): 0–0.5.
7. `gapPenaltyThresholdMs` (gap que marca "bloqueo"): 8000–3000.

### Algoritmo adaptativo
Banda: `validIdeaRate` cerca de `targetThroughput` con CV de IBI estable. Ejes {TEMPORAL_PRESSURE=targetThroughput, TIME_ON_TASK=sessionWindowMs}. Staircase 1-up/2-down sobre throughput. **Seguridad estrictamente primero** (espejo `DefaultEmotionAdaptiveEngine`): `safetyStop → reduceIntensity → checkIn → cooldown → stepDown → hold → stepUp`. `pressureCueIntensity` NUNCA auto-sube (solo dificultad); techo 3. Gate fatiga: deriva de IBI + caída de validez → COOLDOWN. Baseline ancla `targetThroughput`.

### Métricas primarias
`validThroughput` = ideas válidas / minuto. `rhythmStability` = 1 − CV(IBI) (consistencia del flujo).

### Métricas secundarias
`fluency` total (`CreativityRubricScorer`), `blockingEvents` = gaps > gapPenaltyThreshold, `validityRate` = válidas / enviadas (que la velocidad no degrade calidad), `elaboration`, `throughputDrift` (pendiente intra-sesión = fatiga).

### Señales de fatiga o mala calidad
`throughputDrift` negativo; `blockingEvents` creciente; `validityRate` cayendo (velocidad a costa de basura); IBI con cola larga; intentos de abandono repetidos (`OverloadProxy.REPEATED_EXIT_ATTEMPTS`).

### Scoring sugerido
`practiceScore = q1*norm(validThroughput) + q2*rhythmStability + q3*validityRate − q4*blockingRateNorm`, `q1=0.4,q2=0.25,q3=0.25,q4=0.1`. **validityRate como multiplicador implícito**: throughput de basura no puntúa. Bar `null` si válidas < minN. preliminary=true.

### Antitrampa / calidad de datos
`TIMING_NOT_MONOTONIC`. Anti-duplicado estricto (presión incentiva repetir). Detección de tecleo no-deliberado (IBI < 350ms con baja elaboración = spam, espejo `IMPULSIVE_FLOOR_MS`). `validityRate` impide ganar por volumen. RT crudos para IBI (eventos extremos sobre crudos, §2.1.7).

### Variantes
1. **Sprint-recovery:** alternar 30s de presión alta y 15s de ritmo libre (mide recuperación de fluidez).
2. **Prompt-shift:** el prompt cambia cada N segundos (fluidez + flexibilidad bajo presión).
3. **Solo-cola:** bajo presión, solo cuentan ideas raras (combina ejercicio 6 + presión — el más exigente).

### UX
Pantalla: prompt arriba, temporizador y medidor de ritmo (en banda = verde suave, bajo = ámbar — icono + posición, no solo hue), campo texto rápido, contador en vivo. Microanimación: pulso de ritmo funcional (no decorativo, low-motion, espejo micro-pausa neutral). Presión leve, exit siempre visible. Duración: 1.5–3 min (corto por presión). Check-in overlay neutral si sobrecarga.

### Implementación KMP
Reusa renderer **texto** rápido + medidor de ritmo (nuevo `RhythmMeterField`). Modelo:
```
@Serializable data class ImprovTrialDto(val schemaVersion:Int, val promptId:String, val ideas:List<TimedIdeaDto>, val targetThroughput:Double, val pressureLevel:Int, val difficulty:Map<String,Double>)
data class TimedIdeaDto(val text:String, val valid:Boolean, val ibiMs:Long, val elapsedMonotonicMs:Long)
```
Eventos: `idea_submitted`, `idea_valid/invalid`, `blocking_event`, `rhythm_in_band/below`, `check_in_armed`. Estado mínimo: ideas + último timestamp para IBI (`EpochClock`) + nivel de presión. Motores: `CreativityResponseValidator`, `CreativityRubricScorer`; safety por motor local espejo `DefaultEmotionAdaptiveEngine`/`OverloadProxy` (proxies de rendimiento, no estado emocional). Timebase de alta resolución (reusar patrón `SpeedTimebase` para IBI precisos). Persistencia `SecureCreativitySessionStore`. AI: ninguno.

### Riesgos
Presión temporal puede premiar impulsividad/spam si `validityRate` falla. Riesgo de inducir frustración (mitigado por leve-only + check-in). IBI depende de velocidad de tecleo (sesgo motor, no cognitivo) → confunde fluidez con destreza de teclado.

### Revisión crítica
Revisor Crítico: "Estás midiendo velocidad de tecleo, no fluidez ideacional. Un usuario con teclado rápido gana; uno lento 'bloquea' por motricidad. Eso es exactamente 'medir familiaridad con la UI', prohibido. Y la presión temporal en un ejercicio de creatividad es contradictoria: la presión MATA la divergencia (literatura de incubación). Premias cantidad bajo estrés, que correlaciona con ideas obvias, no creativas. EXIJO: (a) normalizar IBI por una baseline de velocidad de tecleo del usuario (medida en un mini-calibrador no-creativo) para descontar destreza motora; (b) `validityRate` y elaboración mínima como gates duros — si caen, el throughput NO puntúa (hecho); (c) presión estrictamente leve con techo 3 y check-in (hecho); (d) reposicionar el objetivo: NO 'creatividad bajo presión' sino 'reducción de bloqueo/auto-censura' — claim más honesto; (e) la variante sprint-recovery es la más defendible (mide recuperación, no pico). Sin (a) y (d), descartado." Aceptado: calibrador de tecleo para normalizar IBI, objetivo reposicionado a anti-bloqueo, presión leve.

### Veredicto
Aprobado con cambios: válido solo con normalización de IBI por baseline motora y objetivo reposicionado a reducción de bloqueo (no "creatividad bajo presión"); presión leve-only obligatoria.

---

## Notas transversales de implementación (las 8)

- **Renderer:** las 8 requieren un **renderer de texto nuevo** (`CreativityTextEntryField` + sub-componentes específicos) — no existe en el repo un renderer text/choice/stop aplicable a entrada libre. Es el principal trabajo de UI común; debe construirse una vez y parametrizarse.
- **Registry→renderer honesto:** cada ejercicio se añade a un `CreativityExerciseGenerators.registry` (espejo de `FocusExerciseGenerators.registry`) con `ExerciseCatalogConsistencyTest` que ata `implemented ⟺ registry.keys`. `domainReady` (lógica + tests) ≠ `implemented` (UI alcanzable).
- **Autoridad de clasificación:** cada uno necesita su `CreativitySessionAssembler` local (espejo `EmotionSessionAssembler`) como única autoridad de métrica; los contadores del holder son display-only.
- **AI fail-closed:** ninguno toca `CreativityAiLayers` activos; toda evaluación es heurística local determinista. Métricas que requieren semántica real (calidad de puente, satisfacción de restricción, profundidad) se reportan **CALIBRATING** hasta `CreativityRuntimePolicy.isAiSurfaceUnlocked` (G01..G10). `requireLocalOnly()` debe invocarse en el wiring de producción del root screen.
- **Persistencia:** todos vía `SecureCreativitySessionStore` (AES-256-GCM+Keystore), solo summaries `@Serializable`+`schemaVersion`, namespace `MindBoostStorageNamespaces.Creativity`, `EpochClock` nunca `Clock.System`.
- **Hueco honesto:** sin IA, la "creatividad" medida es mayormente **fluidez, flexibilidad y elaboración estructural + rareza relativa a banco** — proxies legítimos de práctica, NO originalidad semántica. El ejercicio 6 es el más defendible sin IA; el 4 el más dependiente de IA. Todo el catálogo debe declarar esto en copy (skill `review-cognitive-claims`).