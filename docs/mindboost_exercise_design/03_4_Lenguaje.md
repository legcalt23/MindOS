# Lenguaje

> **Fronteras de seguridad (vinculantes en todo el catálogo):** MindBoost Lenguaje entrena un **perfil cognitivo funcional** de comprensión, inferencia, control semántico, calibración evidencial y razonamiento verbal. No diagnostica dislexia, TDAH, afasia ni trastorno alguno; no mide CI; no promete mejora clínica. Toda métrica es señal de **práctica contra el baseline personal** bajo **dificultad adaptativa**. Salida con soporte estadístico insuficiente ⇒ `null` (em-dash en UI), **nunca 0**; todo score arranca `preliminary = true`. Cada ejercicio cumple el honesty-contract del módulo (ladder E0..E5, gates de fatiga/fairness, evidencia ponderada por banco) antes de exponerse en `LanguageExerciseCatalog.menu`.

---

## 1. Evidence Lock Pro (Anclaje Evidencial Forzado)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `INF` — inferencia soportada). Secundarios: control de sobre-lectura (`LIT`/literalidad), metacognición evidencial (calibración confianza↔soporte). Cross-módulo: alimenta `CognitiveDimension` verbal/razonamiento vía `LanguageProfileSource`.

### Objetivo cognitivo
Entrenar la **inferencia mínima suficiente**: seleccionar la conclusión correcta *y* el conjunto exacto de evidencias que la soportan, penalizando la inferencia que excede el texto (over-claiming) tanto como la que ignora evidencia disponible. Ataca el sub-componente "no sobre-leer" que el `ClosedScoringEngine.EvidenceSelection` ya modela (0.5 con `INF_OVER` cuando hay opción correcta sin evidencia completa).

### Hipótesis de transferencia
SÍ podría transferir (near): a otras tareas de comprensión donde haya que distinguir lo afirmado de lo implicado dentro del mismo paradigma evidence-selection (lectura crítica de párrafos, fact-checking estructurado). NO se debe afirmar: mejora general de "comprensión lectora", far transfer a razonamiento numérico, ni reducción de sesgos del mundo real. Solo se afirma near transfer si `NearTransferVerifier` lo demuestra con ítems isomorfos T2_NEAR.

### Loop principal
1. **Estímulo:** párrafo fuente (40–90 palabras) + pregunta de inferencia + 4 opciones de conclusión + panel de 4–7 fragmentos de evidencia citables del texto.
2. **Decisión:** el usuario toca 1 conclusión y marca (multi-select) el subconjunto de fragmentos que la soportan.
3. **Feedback:** correcto/parcial/incorrecto; si parcial → mensaje "conclusión correcta pero soporte incompleto/excesivo" sin revelar el set exacto la primera vez (repair lo expone en el segundo intento diferido).
4. **Adaptación:** `AdaptationEngine.selectNext` elige siguiente ítem por utilidad (info=1−reliability, +repair si hubo `INF_OVER`, −exposure).
5. **Siguiente trial.**

### Reglas
- Conclusión correcta + evidencia completa (Jaccard=1.0 sobre `requiredEvidenceIds`) ⇒ 1.0.
- Conclusión correcta + evidencia incompleta/excesiva ⇒ 0.5 + `ErrorType.INF_OVER`, `unsupported=true`.
- Conclusión incorrecta ⇒ 0.0 + `distractorErrors[selected]` (INF_OVER vs LIT_DIST).
- Prohibido inferir más allá del texto: una opción "verdadera en el mundo pero no soportada" se clasifica como error.

### Inputs
Tap (conclusión, single-choice) + selección múltiple (fragmentos de evidencia). Sin teclado/voz. Renderer = `MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION` (existente, sin renderer nuevo).

### Variables de dificultad (5–10)
1. `sourceLengthWords` 40–90 (paso 10).
2. `evidenceCandidates` 4–7.
3. `requiredEvidenceCount` 1–3.
4. `distractorOverlap` 0.0–0.6 (solapamiento léxico evidencia-distractor).
5. `inferenceDepth` 1–3 (saltos lógicos entre evidencia y conclusión).
6. `overClaimTrapStrength` 0.0–1.0 (atractivo de la opción mundo-verdadera-no-soportada).
7. `lexicalAbstraction` 1–4 (concreto→abstracto del vocabulario).
8. `syntacticComplexity` 1–4 (subordinación, anidamiento).

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `inferenceDepth`+`requiredEvidenceCount` (banda accuracy 0.70–0.80). Señales: accuracy de captura evidencial (Jaccard), tasa `INF_OVER`, RT mediano de HITs (informativo, no sube dificultad). Si `INF_OVER` rate ≥0.4 en ventana 6 ⇒ DECREASE `overClaimTrapStrength` + REPAIR-block dirigido. `FatigueGate.isHigh` ⇒ STABILIZE (no StepUp). Plateau (sin slope 5 sesiones) ⇒ SwitchAxis a `syntacticComplexity`. Baseline ancla `inferenceDepth` inicial.

### Métricas primarias
- Accuracy de conclusión (sobre trials válidos).
- Captura evidencial (Jaccard medio vs `requiredEvidenceIds`).
- Tasa de over-claiming (`INF_OVER` / trials).

### Métricas secundarias
- RT mediano de HITs (proceso), RT-CV.
- Alineamiento confianza↔soporte (calibración metacognitiva).
- Slope de captura evidencial inter-sesión.

### Señales de fatiga o mala calidad
`HIGH_FATIGUE` flag, latencia creciente (mediana 1er vs último tercio ·1.15), `LOW_EVALUATOR_CONFIDENCE`<0.6, `REPEATED_ITEM`, `VARIANT_BIAS_RISK`. Cualquier flag ⇒ `evidenceWeight=0.0`, `isStrongUpdateAllowed=false`.

### Scoring sugerido
```
itemScore = scoreFromEngine        // {0.0, 0.5, 1.0}
evidenceWeight = itemInformation · evaluatorConfidence · bankFactor
                 · (allFlagsClear ? 1.0 : 0.0)
INF_score(session) = Σ(itemScore_i · evidenceWeight_i) / Σ(evidenceWeight_i)   // null si Σweight<umbral
overClaimIndex = nINF_OVER / nValidTrials
captureRate = mean(Jaccard_i)
```
Barra `null` si `Σweight` bajo umbral; `preliminary=true`.

### Antitrampa / calidad de datos
- Banco TRAINING → `bankFactor=0.5` (no infla evidencia con ítems de práctica).
- Gate `REPEATED_ITEM` anula peso al re-ver un ítem.
- Evidencia debe seleccionarse antes de revelar feedback (no se puede ajustar post-hoc).
- Detección de respuesta sin lectura: RT<umbral mínimo de lectura para `sourceLengthWords` ⇒ `LOW_EVALUATOR_CONFIDENCE`.

### Variantes (≥3)
1. **Refutación:** seleccionar la conclusión *no soportada* + la evidencia que la contradice (invierte la operación).
2. **Evidencia distractora textual:** fragmentos verdaderos del texto pero irrelevantes a la conclusión (entrena relevancia, no veracidad).
3. **Inferencia multi-párrafo:** evidencia repartida en 2 fuentes, requiere integración (`inferenceDepth`≥3).

### UX
Pantalla: fuente arriba (scrolleable, `verticalScroll`), conclusión 4 botones grandes (`MinTouchTarget`), panel evidencia con chips toggle (estado seleccionado por `AppColors` semántico + checkmark, no solo color). Feedback inmediato bajo el botón "Confirmar". Microanimación funcional: resalte del/los fragmento(s) correcto(s) en el segundo intento de repair. Duración ideal: 6–10 ítems / 5–7 min.

### Implementación KMP
Reusa `ClosedScoringEngine.EvidenceSelection` (clave existente), `DefaultEvidenceEngine` (E1/E2 ladder), `DefaultAdaptationEngine` (utility), `NearTransferVerifier` para probes isomorfos, `DefaultFatigueGate`/`DefaultFairnessGate`. Banco = nuevo `EvidenceLockProBank` extendiendo el patrón de `EvidenceLockBank` (más niveles + over-claim traps). Persistencia: `SessionSummaryRepository` (solo summaries) + `EvidenceRepository` event-sourced, vía `SecureKeyValueStore` namespace `Language`, DTO `@Serializable`+`schemaVersion`. **Wiring requerido:** invocar realmente `AdaptationEngine.selectNext` y `NearTransferVerifier.verify` en el holder (hoy hueco). Eventos: `ItemPresented`, `ConclusionSelected`, `EvidenceSelected{ids}`, `Scored{flag,weight}`, `RepairTriggered`. Renderer: existente (evidence-selection).

### Riesgos
- El holder demo no persiste packets ni invoca adaptation/transfer (hueco documentado): sin fix, el ejercicio mide pero no progresa.
- Autoría costosa de bancos con `requiredEvidenceIds` correctos.
- `overClaimTrap` mal calibrado puede medir astucia de test en vez de inferencia.

### Revisión crítica
El Revisor exige: (1) demostrar que el ítem distingue inferencia de conocimiento previo del usuario → obligatorio el flag `HIGH_PRIOR_KNOWLEDGE` y banco con temas neutros; sin esto, mide cultura, no inferencia. (2) El 0.5 de "correcto sin evidencia" puede premiar adivinar la conclusión y marcar todo → corrección: penalizar evidencia *excesiva* explícitamente (Jaccard, no recall), ya cubierto, pero debe testearse que marcar-todo ⇒ 0.5, no 1.0. (3) Sin wiring de adaptación es un cuestionario estático: bloqueante. Correcciones aceptadas.

### Veredicto
Aprobado con cambios — premium y medible, pero condicionado a wirear adaptación/transferencia/persistencia y al flag de conocimiento previo.

---

## 2. Semantic Surgery (Disección de Significado)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `LEX_A`/`LEX_B` — precisión léxica y matiz semántico). Secundarios: control de paráfrasis (¿mismo significado?), detección de cambio de sentido por una palabra.

### Objetivo cognitivo
Entrenar **discriminación semántica fina**: detectar cuál de varias reformulaciones preserva exactamente el significado de una oración fuente, y aislar la palabra/operador que altera el sentido (negación, cuantificador, conector, matiz aspectual). Ataca el componente que `SemanticSurgeryBank` ya cubre, en versión premium con localización del cambio.

### Hipótesis de transferencia
SÍ (near): paráfrasis fiel, resumen sin distorsión, detección de tergiversación dentro del mismo formato. NO: comprensión global de textos largos, traducción, ni "vocabulario más rico" como claim. Solo near transfer verificado.

### Loop principal
1. **Estímulo:** oración fuente + 3–4 paráfrasis candidatas (una equivalente, resto con un cambio sutil).
2. **Decisión:** seleccionar la paráfrasis equivalente; en niveles altos, también **tocar la palabra** que rompe el sentido en una candidata marcada.
3. **Feedback:** correcto/incorrecto + tipo de cambio (`ErrorType` mapeado: cuantificador, polaridad, alcance).
4. **Adaptación:** utility-based, repair dirigido al tipo de cambio más fallado.
5. **Siguiente trial.**

### Reglas
- Una sola candidata es semánticamente equivalente.
- Cambios admisibles: polaridad (no), cuantificador (todos↔algunos), alcance, conector causal↔temporal, aspecto.
- En modo "localización": acierto pleno = paráfrasis correcta + palabra-pivote correcta (clave compuesta).

### Inputs
Tap (paráfrasis, single-choice) + tap sobre token (localización, en niveles ≥2). Renderer: single-choice existente + **renderer nuevo ligero de "token-tap"** (resaltado de palabra dentro de una frase; reusa zonas táctiles sobre spans).

### Variables de dificultad (5–10)
1. `paraphraseCount` 3–4.
2. `changeSubtlety` 1–5 (polaridad obvia → matiz aspectual).
3. `sentenceLengthWords` 8–28.
4. `lexicalDistance` 0.0–1.0 (sinonimia cercana ↔ lejana entre fuente y candidata).
5. `changeType` ∈ {polarity, quantifier, scope, connector, aspect}.
6. `decoyFluency` 0–1 (qué tan "natural" suena la candidata errónea).
7. `requireLocalization` bool (niveles ≥2).

### Algoritmo adaptativo
Staircase 1-up/3-down sobre `changeSubtlety` (objetivo ~79% correcto). Error repetido del mismo `changeType` (≥2 ventana 6) ⇒ REPAIR-block de ese tipo + DECREASE subtlety solo en ese eje. RT mediano informativo. Fatiga ⇒ COOLDOWN. Plateau ⇒ activar `requireLocalization` (fricción deseable, no regresión).

### Métricas primarias
- Accuracy de equivalencia.
- Accuracy de localización (cuando aplica).
- Sensibilidad por `changeType` (perfil de debilidad).

### Métricas secundarias
- RT mediano HITs, RT-CV.
- Tasa de falsos positivos por `changeType` (confunde equivalencia con cambio).
- Slope inter-sesión.

### Señales de fatiga o mala calidad
Latencia creciente, accuracy drift intra-sesión, `VARIANT_BIAS_RISK` si una variante superficial domina, `LOW_EVALUATOR_CONFIDENCE`.

### Scoring sugerido
```
itemScore = requireLocalization
            ? 0.6·equivalenceCorrect + 0.4·localizationCorrect
            : 1.0·equivalenceCorrect
typeProfile[changeType] = HIT/(HIT+MISS) por tipo        // null si n<umbral
LEX_score = weightedMean(itemScore · evidenceWeight)      // gates aplican
```

### Antitrampa / calidad de datos
- `decoyFluency` evita que se gane por "la rara es la mala".
- Banco con `changeType` balanceado para que no haya estrategia de adivinanza por frecuencia.
- TRAINING bank `·0.5`. RT mínimo de lectura por longitud.

### Variantes (≥3)
1. **Localización pura:** todas las candidatas cambian el sentido; marcar la palabra-pivote en cada una.
2. **Doble cambio:** una candidata con dos micro-cambios que se cancelan (¿neto equivalente?).
3. **Cuantificador/alcance avanzado:** foco en "solo/también/incluso" y negación de alcance.

### UX
Fuente destacada arriba; candidatas como tarjetas tappables; en localización, la frase se vuelve fila de tokens tappables (highlight por `AppColors` + subrayado). Feedback: subraya el pivote real tras responder. Microanimación: pulso del token correcto. Duración: 8–12 ítems / 4–6 min.

### Implementación KMP
Reusa `ClosedScoringEngine.SingleChoice`; clave compuesta para localización = nueva `AnswerKey.SpanLocalization(correctOptionId, pivotTokenIndex)` (sellada, junto a las 3 existentes). `DefaultAdaptationEngine`, gates, `NearTransferVerifier`. Banco `SemanticSurgeryProBank`. Renderer: single-choice existente + **token-tap nuevo** (reutilizable también por E5 Scope/Negation). Persistencia `SecureKeyValueStore` namespace `Language`. Eventos: `ParaphraseSelected`, `PivotTokenTapped{index}`, `ChangeTypeScored`. **Wiring:** integrar adaptación real en holder.

### Riesgos
- Dependencia del idioma: cambios de matiz son específicos del español ⇒ banco debe declararse mono-idioma y no afirmar transferencia translingüística.
- `AnswerKey` nueva obliga a tocar el `when` exhaustivo del scoring (riesgo de rama olvidada → `ExerciseCatalogConsistencyTest` no la cubre; añadir test del scoring).

### Revisión crítica
Revisor: (1) "matiz semántico" es resbaladizo → exige rúbrica cerrada por `changeType` con ítems validados, no juicio abierto; aceptado (todo cerrado, sin Evaluator OS). (2) La localización puede medir lectura visual, no semántica → exige que el pivote NO sea siempre la palabra rara/larga; balancear posición y longitud del pivote. (3) Riesgo de variante-bias si la equivalente siempre es la más corta → balancear longitudes. Correcciones reales exigidas.

### Veredicto
Aprobado con cambios — añade clave `SpanLocalization`, test de scoring y balanceo anti-bias antes de exponer.

---

## 3. Compression Ladder (Escalera de Compresión)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `COMP`/`SUM` — síntesis sin pérdida). Secundarios: jerarquía de ideas (principal vs secundaria), economía verbal.

### Objetivo cognitivo
Entrenar **compresión fiel ascendente**: dado un texto, elegir/ordenar el resumen que conserva la idea nuclear descartando lo accesorio, en niveles crecientes de compresión (frase → titular → 3 palabras). Mide la capacidad de preservar significado bajo presión de brevedad, sin distorsión ni omisión de lo esencial.

### Hipótesis de transferencia
SÍ (near): resumen fiel, extracción de tesis, escritura de titulares dentro del paradigma. NO: redacción libre de calidad, comprensión de dominios técnicos no entrenados. Cerrado: no usa producción abierta (Evaluator OS sigue stub).

### Loop principal
1. **Estímulo:** texto (60–120 palabras) + nivel de compresión objetivo.
2. **Decisión:** ordenar (drag) los candidatos de resumen del más fiel al menos fiel, o seleccionar el resumen óptimo para el nivel.
3. **Feedback:** Kendall-tau del orden vs orden correcto; o correcto/incorrecto.
4. **Adaptación:** sube nivel de compresión solo si fidelidad alta.
5. **Siguiente trial.**

### Reglas
- "Fiel" = conserva idea nuclear + sin afirmaciones no presentes.
- Penaliza tanto omisión de lo esencial como inclusión de lo accesorio o inventado.
- Orden correcto definido por rúbrica cerrada de fidelidad (no estilo).

### Inputs
Drag-reorder (ordering) + tap (selección). Renderer: `ORDERING` existente (Kendall-tau ya implementado en `ClosedScoringEngine.Ordering`) + single-choice.

### Variables de dificultad (5–10)
1. `compressionLevel` 1–4 (frase/titular/3-palabras/1-palabra).
2. `sourceLengthWords` 60–120.
3. `candidateCount` 3–5.
4. `distortionType` ∈ {omission, addition, distortion, accurate}.
5. `nuanceLoad` 1–4 (cuántas ideas secundarias compiten).
6. `decoyFidelity` 0.0–1.0 (qué tan cercano es el resumen casi-correcto).
7. `abstractnessGap` 1–4 (distancia de abstracción texto↔resumen).

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `compressionLevel` + `nuanceLoad`. Señal: Kendall-tau medio (ordering) / accuracy (choice), tasa de `addition` (inventar) vs `omission`. Si `addition` domina ⇒ REPAIR anti-invención + bajar `decoyFidelity`. Fatiga ⇒ STABILIZE. Plateau ⇒ subir `abstractnessGap`. Baseline ancla nivel inicial.

### Métricas primarias
- Fidelidad de orden (Kendall-tau normalizado).
- Tasa de distorsión por adición (inventar contenido).
- Tasa de omisión de idea nuclear.

### Métricas secundarias
- RT por nivel de compresión.
- Slope de fidelidad inter-sesión.
- Perfil de `distortionType` más confundido.

### Señales de fatiga o mala calidad
Drift de Kendall-tau intra-sesión, latencia creciente, `REPEATED_ITEM`, `LOW_EVALUATOR_CONFIDENCE`.

### Scoring sugerido
```
itemScore_ordering = kendallTauNormalized(userOrder, correctOrder)   // 0..1, fracción pares correctos
itemScore_choice   = correct ? 1.0 : 0.0
fidelityIndex = Σ(itemScore_i · evidenceWeight_i)/Σweight            // null si insuficiente
additionRate = nAddition / nValidTrials
COMP_score = fidelityIndex · (1 − additionPenaltyWeight·additionRate)
```

### Antitrampa / calidad de datos
- `decoyFidelity` alto evita ganar por descarte trivial.
- Ordering con Kendall-tau parcial (no todo-o-nada) reduce premio a la suerte pero penaliza adivinar.
- Banco balanceado en `distortionType`. TRAINING `·0.5`.

### Variantes (≥3)
1. **Escalera ascendente forzada:** mismo texto comprimido en 3 niveles sucesivos (mide pérdida controlada).
2. **Caza de la adición:** todos los resúmenes omiten algo; elegir el que NO añade nada falso.
3. **Titular:** comprimir a ≤8 palabras eligiendo entre titulares (uno fiel, resto clickbait/distorsión).

### UX
Texto arriba scrolleable; candidatos como tarjetas arrastrables (lista reordenable con `LazyColumn`), drag handle, MinTouchTarget. Feedback: muestra el orden correcto y resalta el par invertido clave. Microanimación funcional: snap del reordenado. Duración: 5–8 ítems / 5–7 min.

### Implementación KMP
Reusa `ClosedScoringEngine.Ordering` (Kendall-tau ya existe), `DefaultAdaptationEngine`, gates, `EvidenceEngine`. Banco `CompressionLadderProBank` (extiende `CompressionLadderBank`). Renderer: ordering existente (drag) + single-choice; sin renderer nuevo. Persistencia `SecureKeyValueStore` namespace `Language`, summaries-only. Eventos: `OrderSubmitted{permutation}`, `KendallScored`, `DistortionTypeScored`. **Wiring:** activar adaptación.

### Riesgos
- "Fidelidad" sin Evaluator abierto se reduce a ordering cerrado ⇒ no entrena producción real; honesto pero limitado (no afirmar "sabes resumir", solo "discriminas resúmenes fieles").
- Kendall-tau parcial puede dar señal ruidosa con `candidateCount` bajo.

### Revisión crítica
Revisor: (1) elegir un resumen ≠ producir uno; el claim debe ser estrictamente "reconocimiento de fidelidad", no "habilidad de síntesis" → aceptado, copy restringido. (2) Con 3 candidatos, Kendall-tau toma pocos valores discretos → exige `candidateCount`≥4 para señal métrica decente en niveles altos. (3) Riesgo cultural en textos → banco temáticamente neutro. Correcciones exigidas.

### Veredicto
Aprobado con cambios — limitar claim a reconocimiento y forzar `candidateCount`≥4 en niveles altos.

---

## 4. Argument X-Ray (Radiografía Argumental)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `ARG`/`STRUCT` — estructura argumental). Secundarios: distinción premisa/conclusión/garantía, detección de falacia estructural. Puente conceptual con módulo Lógica (`ReasoningScorer`), pero implementación en Lenguaje (cerrado, verbal).

### Objetivo cognitivo
Entrenar el **mapeo de la estructura de un argumento**: identificar la conclusión, las premisas que la soportan y el material irrelevante; y reconocer cuándo el soporte es insuficiente (non-sequitur) sin recurrir a conocimiento del dominio. Análogo verbal del `JustificationClaim` de Lógica.

### Hipótesis de transferencia
SÍ (near): análisis de argumentos escritos del mismo formato, separar afirmación de prueba. NO: lógica formal cuantificada (eso es módulo Lógica), ni "pensamiento crítico" general. Posible sinergia medida vía brain-profile, no afirmada como transferencia.

### Loop principal
1. **Estímulo:** párrafo argumentativo (50–110 palabras) con conclusión + premisas + ruido.
2. **Decisión:** etiquetar cada oración como CONCLUSIÓN / PREMISA / IRRELEVANTE (drag a buckets o tap cíclico), y juzgar si el soporte es suficiente.
3. **Feedback:** matriz de aciertos por rol + veredicto de suficiencia.
4. **Adaptación:** repair sobre el rol peor discriminado.
5. **Siguiente trial.**

### Reglas
- Exactamente una conclusión por argumento (en niveles básicos).
- Suficiencia ∈ {soportada, insuficiente, irrelevante}.
- No se permite usar verdad-en-el-mundo: solo estructura interna del texto.

### Inputs
Drag a buckets (rol por oración) o tap cíclico de etiqueta + tap (suficiencia). Renderer: **renderer nuevo de "labeling por oración"** (lista de oraciones con selector de rol) — reusable; o adaptación del evidence-selection a multi-categoría.

### Variables de dificultad (5–10)
1. `sentenceCount` 3–7.
2. `noiseSentences` 0–3 (irrelevantes).
3. `implicitConclusion` bool (conclusión no marcada con conector).
4. `premiseChainDepth` 1–3 (premisas que soportan sub-premisas).
5. `fallacyType` ∈ {none, non_sequitur, circular, irrelevant_appeal}.
6. `connectorExplicitness` 0–1 (presencia de "porque/por tanto").
7. `topicAbstractness` 1–4.

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `sentenceCount`+`implicitConclusion`. Confusión sistemática premisa↔conclusión (≥2) ⇒ REPAIR + bajar `premiseChainDepth`. RT informativo. Fatiga ⇒ COOLDOWN. Plateau ⇒ activar `fallacyType≠none` (fricción deseable). Baseline ancla complejidad.

### Métricas primarias
- Accuracy de etiquetado por rol (matriz de confusión rol-a-rol).
- Accuracy de juicio de suficiencia.
- Tasa de confusión premisa↔conclusión.

### Métricas secundarias
- RT mediano HITs.
- Sensibilidad a `fallacyType` (detección estructural).
- Slope inter-sesión por rol.

### Señales de fatiga o mala calidad
Drift de accuracy, latencia creciente, etiquetado aleatorio (entropía alta), `LOW_EVALUATOR_CONFIDENCE`, `REPEATED_ITEM`.

### Scoring sugerido
```
roleScore = (#oraciones con rol correcto)/(#oraciones)           // exact-match por oración
sufficiencyScore = correct ? 1.0 : 0.0
itemScore = 0.7·roleScore + 0.3·sufficiencyScore
confusionPC = #(premisa↔conclusión invertidos)/#oraciones
ARG_score = Σ(itemScore·evidenceWeight)/Σweight                  // null si insuficiente
```

### Antitrampa / calidad de datos
- `connectorExplicitness` bajo evita ganar por palabras-pista ("por tanto").
- Posición de la conclusión balanceada (no siempre última).
- Banco con `fallacyType` balanceado. TRAINING `·0.5`.

### Variantes (≥3)
1. **Conclusión implícita:** ninguna oración la enuncia; seleccionarla de un menú (la que el argumento sostiene).
2. **Caza de falacia estructural:** marcar la oración que rompe el soporte (non-sequitur).
3. **Cadena profunda:** premisas que soportan premisas; ordenar la jerarquía de soporte (ordering).

### UX
Párrafo segmentado en oraciones-tarjeta; cada una con selector de rol (3 estados, color+icono distintos por rol, no solo color). Buckets visibles. Panel de suficiencia. Feedback: matriz coloreada de aciertos/errores por rol. Microanimación: ensamblaje del árbol de soporte correcto. Duración: 5–8 ítems / 6–8 min.

### Implementación KMP
Reusa `ClosedScoringEngine` con nueva `AnswerKey.RoleLabeling(correctRoles: Map<SentenceId,Role>, sufficiency)` (sellada). `DefaultAdaptationEngine`, gates, `NearTransferVerifier`. Conceptualmente espeja `ReasoningScorer.justificationScore` de Lógica pero cerrado/verbal. Banco `ArgumentXRayProBank`. **Renderer nuevo:** "sentence-labeling" (lista + role selector + sufficiency). Persistencia `SecureKeyValueStore` namespace `Language`. Eventos: `RoleAssigned{sentence,role}`, `SufficiencyJudged`, `RoleScored`. **Wiring:** adaptación + transfer.

### Riesgos
- Mayor coste de autoría (roles correctos por oración) y de renderer nuevo.
- Solapamiento con módulo Lógica: debe diferenciarse claramente (verbal/estructural vs lógico-formal) para no medir lo mismo dos veces en el brain-profile.

### Revisión crítica
Revisor: (1) etiquetar roles puede tener respuestas ambiguas (una oración premisa-y-conclusión) → exige rúbrica que prohíba ítems ambiguos y validación de que el rol "correcto" es único; bloqueante hasta banco validado. (2) El renderer nuevo añade superficie de UI no probada → exige `ExerciseCatalogConsistencyTest` que ate renderer↔clave. (3) Riesgo de medir familiaridad con metalenguaje ("premisa") más que la habilidad → usar etiquetas funcionales ("afirmación principal" / "razón" / "no aporta"). Correcciones reales.

### Veredicto
Aprobado con cambios — requiere banco sin ambigüedad de rol, renderer probado y etiquetas funcionales no técnicas.

---

## 5. Scope & Negation Drill (Alcance y Negación)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `SCOPE`/`NEG` — alcance de cuantificadores y negación). Secundarios: control inferencial bajo polaridad, desambiguación de alcance.

### Objetivo cognitivo
Entrenar el **cálculo correcto del alcance** de negación, cuantificadores y conectores: dado un enunciado, decidir qué se sigue y qué no bajo "no", "todos/algunos/ninguno", "solo", "a menos que". Es el sub-componente más sensible a errores de over-/under-claiming, ya cubierto parcialmente por `ScopeNegationBank`.

### Hipótesis de transferencia
SÍ (near): inferencia bajo negación/cuantificación en el mismo formato; lectura precisa de cláusulas condicionales y excluyentes. NO: razonamiento probabilístico, comprensión de textos largos. Cerrado.

### Loop principal
1. **Estímulo:** enunciado con operador de alcance + 3–4 inferencias candidatas.
2. **Decisión:** marcar todas las que se siguen necesariamente (multi-select cerrado) o la única que se sigue.
3. **Feedback:** correcto/incorrecto + por qué cada distractor no se sigue (sin revelar antes del intento).
4. **Adaptación:** repair por tipo de operador fallado.
5. **Siguiente trial.**

### Reglas
- "Se sigue" = verdadero en todo modelo que satisface el enunciado (necesidad, no plausibilidad).
- Distractores típicos: inversión de condicional, negación de alcance, cuantificador existencial↔universal.
- Multi-select penaliza tanto marcar de más como de menos (Jaccard).

### Inputs
Selección múltiple cerrada o tap single. Renderer: evidence-selection/multiple-choice existente (la multi-select cerrada ya está soportada) + token-tap (de E2) para señalar el operador.

### Variables de dificultad (5–10)
1. `operatorType` ∈ {negation, universal, existential, only, unless, conditional}.
2. `nestedScopeDepth` 1–3 (operadores anidados).
3. `candidateCount` 3–5.
4. `validInferenceCount` 0–3 (a veces ninguna se sigue).
5. `surfaceSimilarity` 0.0–1.0 (parecido léxico entre válida e inválida).
6. `doubleNegation` bool.
7. `quantifierScopeAmbiguity` 0–1.

### Algoritmo adaptativo
Staircase 1-up/3-down sobre `nestedScopeDepth`+`operatorType` (objetivo ~79%). Confusión sistemática por `operatorType` ⇒ REPAIR-block de ese operador. Casos `validInferenceCount=0` previenen sesgo de "siempre hay una válida". Fatiga ⇒ STABILIZE. Plateau ⇒ `doubleNegation`/`ambiguity` on.

### Métricas primarias
- Accuracy global de inferencia bajo alcance.
- Jaccard de selección (captura+precisión).
- Perfil de error por `operatorType`.

### Métricas secundarias
- RT mediano HITs, RT-CV.
- Tasa de over-claim (marcar inválidas) vs under-claim (omitir válidas).
- Slope inter-sesión por operador.

### Señales de fatiga o mala calidad
`HIGH_FATIGUE`, latencia creciente, accuracy drift, `LOW_EVALUATOR_CONFIDENCE`, `VARIANT_BIAS_RISK`.

### Scoring sugerido
```
itemScore = jaccard(selectedValid, requiredValid)        // 1.0 sólo si exacto; 0 si set incorrecto
overClaim = #(inválidas marcadas)/#candidatas
underClaim = #(válidas omitidas)/#requeridas
SCOPE_score = Σ(itemScore·evidenceWeight)/Σweight        // null si insuficiente
operatorProfile[op] = HIT/(HIT+MISS)                     // null si n<umbral
```

### Antitrampa / calidad de datos
- `validInferenceCount=0` y casos "ninguna" rompen heurística de adivinanza.
- `surfaceSimilarity` alto evita ganar por parecido léxico.
- Balance de `operatorType`. TRAINING `·0.5`. RT mínimo de lectura.

### Variantes (≥3)
1. **Doble negación:** desactivar "no es falso que no…".
2. **Condicional vs bicondicional:** distinguir "si" de "solo si" y "a menos que".
3. **Cuantificador anidado:** "todos…algunos…" con alcance cruzado.

### UX
Enunciado destacado; candidatas como chips multi-select (estado por color+forma). Botón "Confirmar". Feedback: marca cada distractor con el motivo (inversión/negación/cuantificador). Microanimación: tachado del operador mal interpretado. Duración: 8–12 ítems / 4–6 min.

### Implementación KMP
Reusa `ClosedScoringEngine.EvidenceSelection`/`SingleChoice` (multi-select cerrada Jaccard ya implementada), `DefaultAdaptationEngine`, gates, `NearTransferVerifier`. Banco `ScopeNegationProBank` (extiende `ScopeNegationBank`). Renderer: evidence-selection existente + token-tap opcional (compartido con E2). Persistencia `SecureKeyValueStore` namespace `Language`. Eventos: `InferenceSelected{ids}`, `OperatorScored`, `OverUnderClaimLogged`. **Wiring:** adaptación real.

### Riesgos
- Fuerte dependencia del idioma (operadores del español): banco mono-idioma, sin claim translingüístico.
- Riesgo de medir lógica formal en vez de procesamiento verbal del alcance → mantener enunciados en lenguaje natural, no notación.

### Revisión crítica
Revisor: (1) se solapa con E1 (over-claiming) y con Lógica (NECESSARY_SUFFICIENT) → exige diferenciación: E5 ataca *alcance del operador*, E1 *soporte evidencial*, Lógica *forma lógica*; documentar en `ExerciseDefinition` para no triplicar la misma señal en el brain-profile. (2) Casos "ninguna válida" deben ser ~20% del banco o el usuario aprende a marcar siempre algo; exigible cuota. (3) Jaccard exacto puede ser frustrante en alta dificultad → considerar crédito parcial controlado. Correcciones reales.

### Veredicto
Aprobado con cambios — exige delimitación frente a E1/Lógica, cuota de ítems sin inferencia válida, y banco mono-idioma declarado.

---

## 6. Source Triage (Triaje de Fuentes)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `SRC`/`CRED` — evaluación de credibilidad/relevancia de fuentes). Secundarios: distinción hecho/opinión/inferencia, jerarquización de evidencia.

### Objetivo cognitivo
Entrenar el **triaje de fuentes y afirmaciones**: ante varias fuentes sobre una pregunta, ordenar/seleccionar cuál sustenta mejor una conclusión según criterios estructurales (relevancia, especificidad, ausencia de contradicción), sin recurrir a confianza pre-existente en la fuente. Premiumiza `SourceTriageBank`.

### Hipótesis de transferencia
SÍ (near): jerarquizar evidencia y detectar irrelevancia/contradicción en el mismo formato. NO: alfabetización mediática real, detección de desinformación en el mundo (requiere conocimiento de dominio), ni "pensamiento crítico" general. Cerrado.

### Loop principal
1. **Estímulo:** pregunta + 3–5 fuentes/afirmaciones con atributos (especificidad, relevancia, fecha relativa, contradicción).
2. **Decisión:** ordenar por valor de soporte (drag) o seleccionar la fuente decisiva + descartar la irrelevante/contradictoria.
3. **Feedback:** Kendall-tau / acierto + criterio que falló.
4. **Adaptación:** repair sobre el criterio peor aplicado.
5. **Siguiente trial.**

### Reglas
- "Mejor fuente" definida por criterios estructurales cerrados, no por prestigio.
- Penaliza elegir la fuente más "autoritaria" cuando es irrelevante (anti-sesgo de autoridad).
- Contradicción interna ⇒ fuente descartable.

### Inputs
Drag-reorder (ordering) + tap (selección/descarte). Renderer: ordering existente + single/multi-select; sin renderer nuevo.

### Variables de dificultad (5–10)
1. `sourceCount` 3–5.
2. `criteriaInTension` 1–3 (relevancia vs especificidad vs recencia compiten).
3. `authorityDecoyStrength` 0.0–1.0 (fuente prestigiosa pero irrelevante).
4. `contradictionPresence` bool.
5. `relevanceSpread` 0.0–1.0.
6. `claimTypeMix` ∈ {fact, opinion, inference} por fuente.
7. `topicAbstractness` 1–4.

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `sourceCount`+`criteriaInTension`. Caer en `authorityDecoy` (≥2) ⇒ REPAIR anti-autoridad + bajar decoy. RT informativo. Fatiga ⇒ COOLDOWN. Plateau ⇒ subir `criteriaInTension`/añadir contradicción. Baseline ancla.

### Métricas primarias
- Fidelidad de ordenación (Kendall-tau).
- Tasa de captura por sesgo de autoridad (elegir prestigio irrelevante).
- Accuracy de descarte de contradictoria/irrelevante.

### Métricas secundarias
- RT mediano HITs.
- Discriminación hecho/opinión/inferencia.
- Slope inter-sesión.

### Señales de fatiga o mala calidad
Drift de Kendall-tau, latencia creciente, ordenación aleatoria (baja correlación), `REPEATED_ITEM`, `LOW_EVALUATOR_CONFIDENCE`.

### Scoring sugerido
```
itemScore = ordering ? kendallTauNormalized : (correctSelection ? 1.0 : 0.0)
authorityBiasRate = nAuthorityDecoyChosen / nDecoyTrials
SRC_score = Σ(itemScore·evidenceWeight)/Σweight            // null si insuficiente
criterionProfile[c] = aplicación correcta del criterio c   // null si n<umbral
```

### Antitrampa / calidad de datos
- `authorityDecoyStrength` evita estrategia "elige la fuente seria".
- Atributos de fuente explícitos y verificables en el ítem (no requieren conocimiento externo).
- Balance de `claimTypeMix`. TRAINING `·0.5`.

### Variantes (≥3)
1. **Anti-autoridad:** la fuente prestigiosa es irrelevante; descartarla.
2. **Resolución de contradicción:** dos fuentes se contradicen; elegir cuál descartar por criterio estructural.
3. **Hecho vs opinión:** clasificar cada afirmación antes de jerarquizar.

### UX
Pregunta arriba; fuentes como tarjetas con atributos visibles (relevancia/especificidad/fecha como chips), reordenables (`LazyColumn` drag). Zona de descarte. Feedback: muestra el orden óptimo y el criterio decisivo. Microanimación: reordenado snap + marca de la fuente decisiva. Duración: 5–8 ítems / 6–8 min.

### Implementación KMP
Reusa `ClosedScoringEngine.Ordering` + `SingleChoice`, `DefaultAdaptationEngine`, gates, `EvidenceEngine`. Banco `SourceTriageProBank` (extiende `SourceTriageBank`). Renderer: ordering existente; sin renderer nuevo. Persistencia `SecureKeyValueStore` namespace `Language`, summaries-only. Eventos: `SourceOrderSubmitted`, `SourceDiscarded{id}`, `AuthorityBiasLogged`, `ClaimTypeScored`. **Wiring:** adaptación.

### Riesgos
- Riesgo de enseñar a "leer la rúbrica del test" en vez del razonamiento de fuentes → criterios deben variar y no ser predecibles.
- Frontera ética: no debe leerse como "te enseña a detectar fake news" (claim no soportado).

### Revisión crítica
Revisor: (1) "credibilidad" sin contenido del mundo real es artificial; debe quedar claro que mide *aplicación de criterios estructurales dados en el ítem*, no juicio de credibilidad real → copy estricto, aceptado. (2) Kendall-tau con `sourceCount`=3 es de baja resolución → exigir ≥4 en niveles altos (igual que E3). (3) `authorityDecoy` puede inducir la regla inversa "ignora siempre la fuente seria" → balancear ítems donde la prestigiosa SÍ es la mejor. Correcciones reales.

### Veredicto
Aprobado con cambios — restringir claim, `sourceCount`≥4 en alto nivel y balancear el decoy de autoridad en ambos sentidos.

---

## 7. Lexical Access Sprint (Sprint de Acceso Léxico)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `LEX_A` — velocidad/precisión de acceso léxico-semántico). Secundarios: control inhibitorio verbal (rechazar pseudopalabras/cuasi-sinónimos), procesamiento bajo presión temporal. Puente con módulo Speed (paradigma RT-gated).

### Objetivo cognitivo
Entrenar el **acceso léxico-semántico rápido y preciso**: decisiones binarias veloces (¿son sinónimos? ¿pertenece a la categoría? ¿es palabra real?) bajo deadline adaptativo, midiendo velocidad útil sin premiar la impulsividad. Es el ÚNICO ejercicio RT-gated del módulo, modelado sobre el patrón de Speed (`Staircase` CCE→deadline) pero con contenido verbal.

### Hipótesis de transferencia
SÍ (near): fluidez de recuperación léxica en tareas del mismo tipo; reducción de tiempo de acceso a significados entrenados. NO: vocabulario más amplio (no enseña palabras nuevas), comprensión profunda, ni velocidad lectora global. Cerrado, RT-gated.

### Loop principal
1. **Estímulo:** par/ítem léxico (palabra + objetivo: sinónimo/categoría/lexicalidad) con deadline visible-implícito.
2. **Decisión:** tap SÍ/NO (o LEFT/RIGHT) dentro de la ventana.
3. **Feedback:** correcto/incorrecto/lento; RT registrado en timebase de alta resolución.
4. **Adaptación:** deadline se acorta/alarga por staircase (CCE 0..1) según accuracy efectiva.
5. **Siguiente trial.**

### Reglas
- Respuesta antes del deadline; respuesta fuera de ventana = omission.
- Pseudopalabras y cuasi-sinónimos como distractores controlados.
- Velocidad solo cuenta sobre HITs; errores no aportan RT de velocidad (espeja `ReasoningScorer`: velocidad nunca suma, la impulsividad resta).

### Inputs
Tap binario / LEFT-RIGHT. Renderer: **choice (LEFT/RIGHT) reusado del patrón focus** (mitades táctiles) + timebase de alta resolución (Android Choreographer/monotónico, patrón Speed). Renderer existente choice; añade gate de deadline.

### Variables de dificultad (5–10)
1. `deadlineMs` 600–2500 (staircase, paso ~100).
2. `taskType` ∈ {synonym, category, lexicality, antonym}.
3. `semanticDistance` 0.0–1.0 (cuasi-sinónimo vs lejano).
4. `pseudowordLikeness` 0.0–1.0 (qué tan plausible la pseudopalabra).
5. `wordFrequency` 1–5 (frecuente→raro).
6. `interferenceLoad` 0–2 (distractor semántico previo, priming negativo).
7. `responseMappingSwitch` bool (invertir SÍ/NO, mide flexibilidad).

### Algoritmo adaptativo
`DefaultStaircaseEngine`/`MultiSignalStaircaseEngine` estilo Speed: CCE→`deadlineMs`, objetivo accuracy ~80% sobre trials en ventana. Gates: `impulsivityIndex` (anticipaciones < RT mínimo) ⇒ `ContainImpulsivity` (no acortar deadline); `RT-CV` alto ⇒ STABILIZE; `FatigueIndex` ⇒ COOLDOWN. Plateau ⇒ subir `semanticDistance`/`pseudowordLikeness`. Baseline ancla `deadlineMs` inicial (anti-trivial).

### Métricas primarias
- Accuracy bajo deadline (sobre trials válidos).
- RT mediano de HITs (velocidad útil), sobre `cleanRts` post-MAD.
- IES = RT_hits / accuracy (anti trade-off).

### Métricas secundarias
- RT-CV (consistencia).
- Omission vs commission separadas (inatención vs impulsividad).
- Anticipaciones (RT < umbral) contadas sobre crudos (aprendizaje 2026-06-04: no filtrar el evento extremo con MAD).
- Slope/fatigue drift.

### Señales de fatiga o mala calidad
Anticipaciones (RT ultra-rápido) → no son HIT; lapsos (RT extremo lento) contados sobre crudos; `FatigueIndex` por drift de RT; RT-CV alto; commission alta = impulsividad. `LOW_EVALUATOR_CONFIDENCE` si n insuficiente.

### Scoring sugerido
```
validTrial = RT >= antMinMs && RT <= deadlineMs
cleanRts = MAD_filter(hitRts)                              // solo tendencia central
RTmed = median(cleanRts)
acc = nHitsValid / nValidTrials
IES = (acc>0 && n>=nMin) ? RTmed/acc : null                // null, nunca 0
commissionRate = nCommission/nValidTrials
LEX_speed_score = f(IES, RT-CV) con confident=(n>=nMin)    // preliminary=true
```

### Antitrampa / calidad de datos
- Anti-anticipación: RT < `antMinMs` ⇒ no cuenta como HIT (no se gana machacando).
- `ContainImpulsivity` impide acortar deadline cuando commission/anticipación alta.
- `pseudowordLikeness` evita ganar por "la rara es falsa".
- TRAINING `·0.5`. Timebase de alta resolución (no `delay` de UI).

### Variantes (≥3)
1. **Lexicalidad:** palabra real vs pseudopalabra (clásico LDT, RT-gated).
2. **Sinonimia graduada:** SÍ/NO sinónimo con `semanticDistance` adaptativo.
3. **Switch de mapeo:** invertir respuesta cada N trials (flexibilidad bajo presión, mide switch cost verbal — solo con HIT-RT en ambos lados).

### UX
Estímulo central grande; mitades LEFT/RIGHT (SÍ/NO) cubriendo la pantalla (`MinTouchTarget` masivo). Feedback inmediato <200 ms (flash de borde verde/rojo + forma, no solo color). Sin transiciones. Microanimación funcional: barra de deadline que se vacía (la propia tarea). Duración: 30–60 trials / 3–4 min (loop corto, anti-fatiga por bloques cortos).

### Implementación KMP
Reusa el patrón Speed: `DefaultStaircaseEngine`/`MultiSignalStaircaseEngine` (CCE→deadline), `FatigueIndex`, `SpeedTimebase`/timebase de alta resolución (expect/actual; Android Choreographer, desktop/iOS fallback monotónico). Scoring estilo `CognitiveProfileScore` (nullable, `preliminary` siempre true, macro-categoría descriptiva). Para encajar en el spine cerrado de Lenguaje: `ClosedScoringEngine.SingleChoice` por-trial + agregación RT propia. Banco `LexicalAccessBank` (`@Serializable`, palabras/pseudopalabras/pares). Persistencia `SecureKeyValueStore` namespace `Language`, summaries-only. **Renderer:** choice LEFT/RIGHT existente + gate de deadline (nuevo wiring de timebase, no renderer visual nuevo). Eventos: `TrialPresented{deadlineMs}`, `Responded{key,rtMs}`, `Anticipation`, `Omission`, `StaircaseAdjusted`. Motores: `StaircaseEngine` + `FatigueGate`.

### Riesgos
- Importa complejidad de Speed (timebase, anti-anticipación) al módulo Lenguaje, que hoy usa contador monótono (no reloj real): requiere wirear timebase de alta resolución, hueco real.
- Riesgo de premiar impulsividad si los gates no se activan: bloqueante sin `ContainImpulsivity` real.
- Dependencia fuerte de idioma (frecuencia léxica del español): banco mono-idioma calibrado.

### Revisión crítica
Revisor: (1) un LDT/sinonimia rápida roza "velocidad de proceso" que es dominio de Speed → debe justificar que el contenido verbal aporta señal distinta (acceso léxico-semántico, no RT genérico) y mapear a `CognitiveDimension` verbal, no a speed, para no duplicar en brain-profile. (2) El módulo Lenguaje no tiene timebase real ⇒ sin él, los RT son ruido: bloqueante, exige integrar timebase de alta resolución antes de exponer. (3) Anti-anticipación y `ContainImpulsivity` son obligatorios o el ejercicio premia tapping rápido. (4) Frecuencia léxica debe estar calibrada empíricamente o `semanticDistance` es subjetivo. Correcciones duras.

### Veredicto
Aprobado con cambios — depende de integrar timebase de alta resolución y gates anti-impulsividad; sin ellos, descartado. Mapear a dimensión verbal, no speed.

---

## 8. Calibrated Comprehension (Comprensión Calibrada)

### Módulo (principal + secundarios)
Principal: Lenguaje (trait `INF`+`META` — comprensión + calibración metacognitiva). Secundarios: alineamiento confianza↔exactitud, control de over/under-confidence. Espeja el `confidenceScore` de `ReasoningScorer` (Lógica) en clave verbal.

### Objetivo cognitivo
Entrenar la **metacognición de la comprensión**: responder preguntas de inferencia y *además* declarar confianza calibrada, recompensando la correspondencia entre confianza y acierto (regla de puntuación propia tipo Brier). Penaliza tanto el exceso de confianza en errores como la duda en aciertos. Convierte la calibración en señal entrenable, no solo medida.

### Hipótesis de transferencia
SÍ (near): mejor calibración de juicio de comprensión en el mismo paradigma; reducción de over-claiming consciente. NO: metacognición general, "saber lo que sabes" en cualquier dominio, ni mejora de comprensión per se. Cerrado.

### Loop principal
1. **Estímulo:** texto + pregunta de inferencia (4 opciones).
2. **Decisión:** seleccionar respuesta + declarar confianza (LOW/MEDIUM/HIGH, mapeada a prob como en Lógica: 0.35/0.65/0.9).
3. **Feedback:** correcto/incorrecto + puntuación de calibración (Brier) + mensaje metacognitivo ("acertaste pero con baja confianza" / "fallaste con alta confianza").
4. **Adaptación:** dificultad de comprensión por staircase; el componente de calibración modula el feedback diferido, no la dificultad.
5. **Siguiente trial.**

### Reglas
- Confianza obligatoria antes de feedback (no ajustable post-hoc).
- Calibración recompensa correspondencia, no confianza alta per se.
- Over-confident error y under-confident hit ambos penalizados (Brier).

### Inputs
Tap (respuesta single-choice) + tap (nivel de confianza, 3 estados). Renderer: single-choice existente + **selector de confianza** (3 botones, ya conceptualmente presente en Lógica `LogicConfidence`); sin renderer pesado nuevo.

### Variables de dificultad (5–10)
1. `inferenceDepth` 1–3.
2. `sourceLengthWords` 40–100.
3. `distractorPlausibility` 0.0–1.0 (atractivo del distractor → induce over-confidence).
4. `confidenceGranularity` 3 fija (LOW/MED/HIGH) o 5 (avanzado).
5. `ambiguityLevel` 0–2 (texto con grado de indeterminación legítima).
6. `lexicalAbstraction` 1–4.
7. `trapForOverconfidence` bool (distractor muy plausible).

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `inferenceDepth`+`distractorPlausibility` (banda accuracy 0.70–0.80). El **calibration error** (Brier medio) NO controla la dificultad (evita penalizar doblemente), pero alimenta `repair` metacognitivo: over-confidence sistemática ⇒ feedback de cautela + ítems con `trapForOverconfidence`. Fatiga ⇒ STABILIZE. Plateau de comprensión ⇒ subir `ambiguityLevel`. Baseline ancla.

### Métricas primarias
- Accuracy de inferencia (comprensión).
- Calibration error (Brier medio): `mean((conf − correct)²)`.
- Over/under-confidence index (asimetría del error de calibración).

### Métricas secundarias
- RT mediano HITs.
- Resolución metacognitiva (gap de confianza entre aciertos y errores).
- Slope de calibración inter-sesión.

### Señales de fatiga o mala calidad
Confianza degenerada (siempre HIGH o siempre LOW = no discrimina) ⇒ `LOW_EVALUATOR_CONFIDENCE`/flag; latencia creciente; accuracy drift; `REPEATED_ITEM`.

### Scoring sugerido
```
conf = {LOW:0.35, MED:0.65, HIGH:0.9}[declared]
correct ∈ {0,1}
brier_i = (conf − correct)²                               // 0 = calibración perfecta
calibrationScore = 1 − mean(brier_i)                      // 0..1, null si n<umbral
overconfIndex = mean(conf − correct | error)              // asimetría
COMPREHENSION_score = acc                                 // comprensión pura
META_score = calibrationScore                             // calibración, separada
```
Ambas barras `null` si insuficiente; `preliminary=true`. Comprensión y metacognición se reportan separadas (no se mezclan en un número).

### Antitrampa / calidad de datos
- Confianza degenerada (sin varianza) anula `META_score` (no se gana marcando siempre MED).
- Confianza obligatoria pre-feedback impide ajuste tras ver resultado.
- `distractorPlausibility` controlado evita que over-confidence sea ruido del ítem.
- TRAINING `·0.5`. Brier solo sobre trials válidos.

### Variantes (≥3)
1. **Apuesta evidencial:** asignar "peso de evidencia" además de confianza (¿cuánto del texto lo soporta?).
2. **Comprensión bajo ambigüedad:** textos con indeterminación legítima donde la confianza calibrada correcta es MEDIA (no HIGH).
3. **Calibración temporal:** confianza pre-respuesta vs post-respuesta (mide ajuste).

### UX
Texto arriba scrolleable; pregunta + 4 opciones; tras seleccionar, selector de confianza (3 botones grandes, color+forma distintos). Feedback: tarjeta de calibración (matriz acierto×confianza) con mensaje metacognitivo no culpabilizante ("contra tu baseline", nunca "deberías saberlo"). Microanimación: marcador de calibración que se mueve hacia el ideal. Duración: 6–10 ítems / 6–8 min.

### Implementación KMP
Reusa `ClosedScoringEngine.SingleChoice` para corrección; el componente de confianza espeja `LogicConfidence`/`confidenceScore` de Lógica (mapeo 0.35/0.65/0.9). Agregación Brier propia en `commonMain`. `DefaultAdaptationEngine` (comprensión), gates, `EvidenceEngine`, `NearTransferVerifier`. Banco `CalibratedComprehensionBank` (`@Serializable`). **Renderer:** single-choice + selector de confianza (ligero, 3 estados; reusa el patrón de Lógica). Persistencia `SecureKeyValueStore` namespace `Language`, summaries-only (incluye Brier agregado, nunca respuestas crudas). Eventos: `AnswerSelected`, `ConfidenceDeclared{level}`, `BrierScored`, `CalibrationDrift`. Motores: `ScoringEngine`+`AdaptationEngine`+`FatigueGate`. Brain-profile: reporta META como señal coarse separada, `CALIBRATING`→em-dash.

### Riesgos
- Calibración requiere n alto para `confident=true` (Brier es ruidoso con pocos trials) ⇒ riesgo de barras `null` frecuentes; honesto pero puede frustrar expectativas.
- Doble objetivo (comprensión + calibración) puede confundir al usuario sobre "qué se mide".
- No afirmar que mejora la metacognición en la vida real (solo en el paradigma).

### Revisión crítica
Revisor: (1) Brier con 6–10 ítems es estadísticamente débil → exige umbral n explícito para `META_score` (probablemente acumulado inter-sesión, no por sesión) y barra `null` honesta; sin esto, reporta ruido como metacognición: bloqueante. (2) Riesgo de entrenar al usuario a "jugar" el Brier (siempre MED) → la penalización de confianza degenerada es obligatoria y debe testearse. (3) Mezclar comprensión y calibración en una sola dificultad confunde la señal → correcto separarlas, pero el staircase NO debe usar Brier (ya corregido). (4) El selector de confianza añade fricción al loop → mantenerlo a 1 tap. Correcciones reales.

### Veredicto
Aprobado con cambios — separar comprensión/calibración, umbral n agregado para Brier (barra null honesta) y test anti-degeneración de confianza.