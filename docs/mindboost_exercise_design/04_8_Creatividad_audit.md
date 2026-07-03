# Auditoría Creatividad

ated## Auditoría del módulo Creatividad — Comité MindBoost (ground-truth del código, no del doc)

### Hallazgo transversal CRÍTICO (leer primero)

El doc `03_8_Creatividad.md` describe **8 ejercicios** con anti-gaming, rareza, switchCost, calibrationError, adaptación staircase, bancos pre-etiquetados y assemblers por ejercicio. **El código real tiene 2 ejercicios** (`CreativityExerciseCatalog.implemented = [EX01_SUPPRESS_FIRST_IDEA, EX02_CATEGORY_ROTATION]`) y **NINGUNA** de esa maquinaria:

- **Loop real = batch submit.** `CreativitySessionStateHolder.submit(ideas: List<String>)` envía la lista entera de una vez; `elapsed = now - startMs` es UN solo número de toda la ronda. NO hay feedback inmediato por idea, NO hay RT/IBI por idea, NO hay latencia de primera idea. Todo lo que el doc llama "feedback inmediato", "ritmo inter-idea", "firstResponseLatencyMs", "switchCost", "IBI", "RhythmMeter" **no existe**.
- **Validador = 3 checks** (`CreativityResponseValidator`): `TIMING_NOT_MONOTONIC` (elapsed<0), `EMPTY_RESPONSE`, `BELOW_MIN_IDEAS`. Nada más.
- **Scorer = 2 señales** (`CreativityRubricScorer`): `fluency` = `ideas.distinct().size`; `elaboration` = palabras/idea. `aiDerived=false`. NO hay `practiceScore`, NO hay `inhibitionSuccessRate`, `categorySpread`, `rarityScore`, `noveltyProxy`, `constraintSatisfactionRate` — ninguna métrica del doc.
- **Anti-gaming = BLOQUEADO por diseño.** `BlockedCreativityMetricIntegrityLayer.scrutinize()` siempre devuelve `Blocked`. El "Antitrampa" de los 8 ejercicios del doc (overlap, copy-paste <300ms, spam <400ms, keyword-stuffing, banco oculto, plausibilidad) **no está implementado en ningún sitio**. El único anti-cheat real es `elapsed<0`.
- **Dedup real = `List.distinct()`** sobre `normalizedIdeas`. NO hay overlap léxico, NO hay near-duplicate, NO hay rechazo de sinónimo trivial.
- **Banco** = 8 prompts EX01 + 6 EX02 = 14 strings estáticos. SIN prohibidos, SIN frecuencias, SIN distancia de dominio, SIN holdout, SIN rotación forzada, SIN inter-rater. Memorizable en 1-2 sesiones.
- **Adaptación = inexistente.** No hay `CreativityAdaptiveEngine`, no hay staircase, no hay `DifficultyVector`. La dificultad (minIdeas/targetIdeas) es fija por ejercicio.
- **Perfil global:** Creatividad queda CALIBRATING por diseño (no contribuye score numérico). Eso es lo único honesto y correcto hoy.

**Consecuencia de veredictos:** EX01 y EX02 reales son honestos y seguros (copy correcto, null≠0, perfil CALIBRATING) pero **miden fluidez/elaboración de tecleo, no el constructo que su título promete** (inhibición prepotente en EX01, flexibilidad/switch en EX02): ninguno implementa el mecanismo que los diferenciaría. EX03-EX08 son **solo diseño** → no pueden aprobarse como "implementados"; se evalúan como specs a REBUILD/BLOCK antes de codificar. La regla 2 (anti-gaming diseñado NO cuenta) y la regla 10 (mejores señales, no más ejercicios) gobiernan todo el módulo.

---

### EX01 — Supresión del Primer Impulso

1. **Veredicto:** REVISE (real, jugable, honesto, pero el mecanismo núcleo —prohibir el uso común— NO existe en código; hoy es "lista usos" genérico).
2. **Constructo objetivo:** Inhibición de la respuesta prepotente en pensamiento divergente (producir alternativas no-obvias). Real medido hoy: fluidez ideacional + elaboración.
3. **Señal primaria válida:** `fluency` = nº de ideas distintas (`distinct().size`). Puede actualizar una señal de PRÁCTICA de fluidez (no de inhibición) si pasa min-n; NUNCA actualiza un constructo de "inhibición" porque no hay set prohibido ni `inhibitionSuccessRate` implementado.
4. **Señales secundarias:** `elaboration` (palabras/idea) como feedback descriptivo interno; `CreativityPracticeLabel` (KEEP_PRACTICING/STEADY/STRONG) relativo al propio target. NO usar `noveltyProxy` (no existe).
5. **Contaminantes principales:** velocidad/destreza de tecleo, riqueza léxica del idioma, familiaridad con el banco de 8 objetos (memorización), longitud de respuesta inflando elaboration. Sin RT por idea no hay coste-de-inhibición medible. RedundantWith: EX02 (mismo loop batch, misma señal fluency).
6. **Ataques de gaming:** teclear 8 ideas basura cortas no-vacías y distintas (`distinct()` no detecta sinónimos ni casi-duplicados) → max fluency. Copy-paste de lista pre-escrita (no hay detector <300ms ni overlap). Memorizar los 8 objetos. Nada de esto se detecta: `MetricIntegrityLayer` está Blocked.
7. **Gates obligatorios:** min-n por sesión (≥minIdeas ya enforced); **falta**: detector de near-duplicate/overlap, detector de spam por timing (no hay timing por idea → imposible hoy), rotación/expansión obligatoria del banco, gate de no-actualizar-perfil (ya: CALIBRATING). Timebase: NO aplica RT (no se mide), así que no requiere timebase autoritativa — pero por eso tampoco puede afirmar coste de inhibición.
8. **Métrica y scoring:** hoy `fluency=distinct.size`, `elaboration=avg words`. n_min: ≥minIdeas(3) para validez de ronda; para señal de perfil exigir ≥5 rondas válidas inter-sesión. Invalidaciones: elapsed<0 (TIMING_NOT_MONOTONIC), <minIdeas, vacío. null/calibrating: si <n_min de rondas → CALIBRATING, nunca 0. Agregación inter-sesión: media de fluency válida ponderada por rondas. Actualiza perfil: solo señal "práctica de fluidez", nunca "inhibición".
9. **Banco de ítems:** HOY 8 objetos fijos, sin prohibidos, sin rotación, sin holdout, sin inter-rater → memorizable. Mínimo exigible: ≥40 objetos con lista de usos-comunes precomputada y validada (inter-rater ≥80% sobre "¿es uso común?"), rotación obligatoria por sesión, holdout de objetos no vistos para separar mejora real de familiaridad, HIGH_PRIOR para usos triviales.
10. **UX y copy PERMITIDO:** "práctica de generar alternativas contra tu baseline", "Fluidez (ideas distintas)", "Solo práctica · No es un diagnóstico · No es un test de inteligencia" (ya presente en `CreativityStrings.DISCLAIMER`).
11. **UX y copy PROHIBIDO:** "mide tu inhibición", "originalidad", "creatividad general", "tu capacidad creativa", cualquier número con decimales presentado como capacidad absoluta, far-transfer (vida real/innovación/IQ). Copy-scanner debe bloquear estos lexemas en el módulo.
12. **Implementación KMP:** existe `CreativityResponse(ideas, elapsedMonotonicMs)`, `CreativitySessionCoordinator`, `SecureCreativitySessionStore`, `EpochClock`. Falta para el rediseño: banco prohibido + matcher whole-word accent-insensitive (estilo `EmotionTerminologyScanner`), near-duplicate (Levenshtein/Jaccard local), `inhibitionSuccessRate`. Test hooks: `MonotonicMillis` inyectable ya existe. Serialización: `@Serializable` DTOs + `schemaVersion` ya presentes.
13. **Tests obligatorios:** unit del matcher de prohibidos; property (cualquier permutación de N ideas distintas válidas no rompe scorer); simulación adversarial con perfil "spam-basura" (debe NO inflar señal); golden-scoring lock de fluency/elaboration; copy-scanner sobre `CreativityStrings` y banco. NO jank (sin RT). Hoy SOLO existen unit deterministas — falta adversarial.
14. **Criterios de aceptación (→APPROVE):** (a) banco prohibido implementado+testeado con inter-rater; (b) near-duplicate/overlap implementado+testeado contra perfil sintético spam; (c) `inhibitionSuccessRate` = ideas_no_prohibidas_y_no_triviales / intentos, como señal primaria; (d) rotación de banco + holdout; (e) copy-scanner del módulo activo.
15. **Rediseño propuesto:** EX01 = "Usos Alternativos con Prohibición". Banco ≥40 objetos, cada uno con top-K usos comunes (precomputados+inter-rater). El usuario produce usos; se rechazan los prohibidos y los casi-duplicados (overlap). Señal primaria = `inhibitionSuccessRate` (no fluency cruda). Holdout de objetos para medir familiaridad vs mejora. Sigue siendo batch-submit (no necesita RT). Novelty = descriptivo, fuera de score.
16. **Impacto esperado (solo dentro del entrenamiento):** medio — práctica de fluidez bajo restricción es un paradigma divergente legítimo; con prohibición+holdout sube a señal limpia de inhibición ideacional.
17. **Prioridad:** P1.
18. **Riesgo residual:** sin IA, "trivial/sinónimo" se aproxima por overlap léxico → falsos negativos (sinonimia semántica no detectada); dependencia idiomática del banco; novelty sigue siendo proxy de rareza léxica, nunca originalidad.

---

### EX02 — Rotación Forzada de Categorías

1. **Veredicto:** BLOCK (como está, el título promete flexibilidad/switchCost y el código NO tiene clasificador de categorías, NO mide switchCost, NO captura RT por idea: es idéntico a EX01 con otro prompt → redundante y engañoso).
2. **Constructo objetivo:** Flexibilidad cognitiva / set-shifting semántico (abandonar categoría activa y saltar). Real medido hoy: fluidez (mismo `fluency` que EX01).
3. **Señal primaria válida:** ninguna válida para flexibilidad hoy. `fluency` distinct es la única que computa, y es la MISMA que EX01 → no aporta varianza nueva. switchCost/switchAccuracy/categorySpread NO existen.
4. **Señales secundarias:** `elaboration`. (Las del doc —perseverationErrors, withinCategoryDepth, flexibilityIndex— no existen.)
5. **Contaminantes principales:** disponibilidad léxica disfrazada de flexibilidad (crítica del propio doc), tecleo, idioma. **Redundancia total con EX01** (redundantWith: EX01): mismo loop, misma única señal.
6. **Ataques de gaming:** idénticos a EX01 (basura distinta corta). Además, sin clasificador de categoría, "pertenece a la categoría activa" no se verifica → el usuario teclea cualquier cosa.
7. **Gates obligatorios:** requeriría clasificador categoría↔léxico con ítems pre-clasificados (ítem fuera de banco = `unclassified`, NO error/no penaliza); switchCost SOLO con HIT-con-RT en ambos lados (regla congruencia) → **requiere RT por idea, que hoy no se captura** → si se mide RT ideacional (segundos), NO es switchCost ejecutivo: debe etiquetarse proxy explícito. min-n de switches válidos.
8. **Métrica y scoring:** rediseño: `switchAccuracy` = switches correctos/totales (primaria); `switchCost` = mediana(RT_post-switch)−mediana(RT_no-switch) solo ambos lados, `null` si falta lado, etiquetado PROXY ideacional. n_min: ≥8 switches válidos. Invalidaciones: switchCost no computable → null. CALIBRATING hasta n_min. Actualiza perfil solo `switchAccuracy` como práctica de flexibilidad, nunca como "control ejecutivo".
9. **Banco de ítems:** HOY 6 temas, sin categorías pre-clasificadas, sin cues, sin holdout. Necesita: banco de pertenencia categoría↔léxico pre-clasificado (inter-rater), cues de categoría, ítems-frontera controlados (`ambiguousItemRate`), rotación de temas, holdout.
10. **UX y copy PERMITIDO:** "práctica de cambio de enfoque", "cambiaste de categoría N veces".
11. **UX y copy PROHIBIDO:** "tu flexibilidad cognitiva", "switchCost ejecutivo", "adaptabilidad", far-transfer. switchCost nunca sin la etiqueta "proxy ideacional, no control ejecutivo".
12. **Implementación KMP:** requiere capturar RT por idea (cambiar el loop de batch a por-idea con `EpochClock`/`MonotonicMillis`), un `CreativitySessionAssembler` local como autoridad de switchCost (espejo `EmotionSessionAssembler`/`FocusSessionAssembler`), banco categorial + clasificador whole-word. Nada de esto existe.
13. **Tests obligatorios:** unit clasificador (ítem fuera de banco → unclassified, no penaliza); golden switchCost del assembler (no la UI); simulación adversarial "teclea-rápido-basura" (switchAccuracy no debe subir); property congruencia (switchCost null si falta un lado); copy-scanner.
14. **Criterios de aceptación (→APPROVE):** (a) clasificador categorial pre-etiquetado+inter-rater; (b) loop por-idea con RT y assembler autoritativo; (c) switchCost como proxy etiquetado con regla de ambos-lados; (d) demostrar varianza distinta de EX01 (si no, MERGE en EX01); (e) banco con holdout y rotación.
15. **Rediseño propuesto:** O bien (preferido) REBUILD como tarea de switching real: tema + cue de categoría visible, el usuario produce ideas de la categoría activa, cue cambia → se mide switchAccuracy (verificable por banco) + switchCost proxy; ítems no clasificables no penalizan. O bien, si el coste de construir clasificador+assembler no se asume, **MERGE en EX01** (es el mismo loop) y RETIRE como ejercicio separado para no inflar el catálogo con la misma varianza (regla 9).
16. **Impacto esperado (solo dentro del entrenamiento):** bajo hoy (redundante); medio si se construye el switching real verificable.
17. **Prioridad:** P2 (no construir hasta que EX01 tenga banco+anti-gaming; si no se va a construir el clasificador, MERGE/RETIRE ya).
18. **Riesgo residual:** clasificador local frágil en frontera contamina la métrica núcleo; switchCost ideacional ≠ ejecutivo (disponibilidad léxica); idioma. Si se queda como está = miente sobre lo que mide.

---

### EX03 — Síntesis Bajo Restricción (SOLO DISEÑO)

1. **Veredicto:** REBUILD (no implementado; spec con anti-gaming crítico —keyword-stuffing— que el propio doc reconoce y manda a CALIBRATING, pero sin él el ejercicio mide lo opuesto a creatividad).
2. **Constructo objetivo:** Combinatoria forzada / satisfacción de múltiples restricciones simultáneas.
3. **Señal primaria válida:** `constraintSatisfactionRate` = restricciones cumplidas/requeridas — PERO solo válida si "cumplir" exige tag + ventana de elaboración coherente, no el tag suelto. Sin esa ventana implementada+testeada, NO actualiza nada.
4. **Señales secundarias:** `viableSolutionCount`, `solutionVariety`, `calibrationError` (|cree cumplir − cumple|, metacognición — la mejor señal honesta aquí), `constraintIntegrationDepth`.
5. **Contaminantes principales:** keyword-stuffing (escribir el tag literal "cumple"), copiar el enunciado, verbosidad. RedundantWith: ninguno fuerte; la metacognición (calibrationError) lo diferencia.
6. **Ataques de gaming:** incluir los tags requeridos como palabras sueltas → satisfacción 100% sin resolver. Sin IA no hay comprensión semántica. Es el ataque que el doc admite.
7. **Gates obligatorios:** cumplimiento = presencia de tag MÁS ventana de elaboración mínima coherente (no tag suelto); banco de restricciones con tensión pre-modelada (no aleatoria) para garantizar resolubilidad; calidad semántica CALIBRATING hasta `isAiSurfaceUnlocked`; anti-copy del enunciado (overlap); min-n soluciones válidas.
8. **Métrica y scoring:** `practiceScore = 0.4·satisfRate + 0.3·norm(integrationDepth) + 0.2·variety − 0.1·calibErrorNorm`. n_min: ≥minN soluciones válidas. Invalidaciones: vacío, copia de enunciado, timing. CALIBRATING para calidad semántica siempre (sin IA). Solo `constraintSatisfactionRate` (presencia+elaboración) como señal de práctica.
9. **Banco de ítems:** objetivo + 2–6 restricciones con tags; tensión (`constraintConflict`) pre-modelada y validada para resolubilidad; holdout de objetivos; rotación; inter-rater sobre "¿esta solución cumple el tag?".
10. **UX y copy PERMITIDO:** "práctica de combinar requisitos", chips verde/gris por restricción.
11. **UX y copy PROHIBIDO:** "resolución de problemas reales", "innovación", "diseño", "tu capacidad de síntesis" como medida, far-transfer.
12. **Implementación KMP:** `SynthesisTrialDto` (en doc), matcher de tags whole-word accent-insensitive + ventana de elaboración, `CreativitySessionAssembler` autoritativo. Renderer texto + `ConstraintChipsRow` (no existe ningún renderer de texto creativity aún).
13. **Tests obligatorios:** adversarial keyword-stuffing (tag suelto NO debe contar); unit ventana-de-elaboración; golden satisfRate; property resolubilidad del banco; calibrationError unit; copy-scanner.
14. **Criterios de aceptación (→APPROVE):** (a) ventana-elaboración implementada+testeada que derrota keyword-stuffing; (b) banco con tensión validada y holdout; (c) calidad semántica visiblemente CALIBRATING en UI; (d) calibrationError funcionando.
15. **Rediseño propuesto:** el del doc con la ventana de elaboración como gate duro y la metacognición (`calibrationError`) como señal central honesta (no depende de comprensión semántica: solo compara auto-tag vs detección). Ese eje metacognitivo es lo que lo salva sin IA.
16. **Impacto esperado:** medio (combinatoria es creatividad aplicada real; pero la verificación sin IA es superficial).
17. **Prioridad:** P2.
18. **Riesgo residual:** presencia-de-tag+ventana sigue sin verificar el espíritu de la restricción; un absurdo elaborado pasa. Sin IA, techo de honestidad bajo.

---

### EX04 — Analogías Remotas (SOLO DISEÑO)

1. **Veredicto:** BLOCK (el más dependiente de IA; el propio doc lo admite. En modo libre, un puente sintáctico absurdo —"el sol es como un banco porque ambos abren por la mañana"— pasa el filtro estructural → riesgo reputacional máximo).
2. **Constructo objetivo:** Asociación remota / conexión de dominios distantes (componente del RAT/insight).
3. **Señal primaria válida:** NINGUNA en modo libre sin IA. La ÚNICA verificable localmente es la **variante proporcional A:B::C:?** (completar contra banco) → `proportionalAccuracy`. El modo "escribe el puente" no produce señal válida.
4. **Señales secundarias:** `relationVariety` (tipos de relación usados, autoevaluación → calibración), `bridgeLatencyMs` (no capturado hoy).
5. **Contaminantes principales:** forma sobre fondo (puente estructuralmente correcto pero absurdo); plantillas fijas ("es como… porque ambos existen"); idioma (conectores por locale). `meanDomainDistanceSolved` es circular ("resuelto" = pasó la heurística débil).
6. **Ataques de gaming:** rellenar una frase-plantilla con cualquier par → aceptado. Sin comprensión, "porque ambos…" siempre pasa.
7. **Gates obligatorios:** declarar `bridgeAcceptanceRate` como medida de ESTRUCTURA, no de calidad; calidad real CALIBRATING hasta IA; anti-plantilla agresivo; **solo la variante proporcional A:B::C:? puede puntuar** (verificable contra banco); holdout de pares.
8. **Métrica y scoring:** primaria = `proportionalAccuracy` (modo A:B::C:?, banco con respuesta canónica). Modo libre = NO puntúa, solo telemetría descriptiva CALIBRATING. n_min: ≥minN ítems proporcionales. Invalidaciones: tautología, plantilla repetida, vacío. CALIBRATING para todo lo no-proporcional.
9. **Banco de ítems:** pares pre-etiquetados por distancia semántica (inter-rater) para el modo libre descriptivo; banco de proporciones A:B::C:D con respuesta canónica + distractores para el modo puntuable; holdout; rotación dura.
10. **UX y copy PERMITIDO:** "práctica de conectar ideas", "completa la analogía".
11. **UX y copy PROHIBIDO:** "tu pensamiento lateral", "insight", "creatividad asociativa" como medida, "originalidad", far-transfer, IQ. NUNCA presentar `bridgeAcceptanceRate` como calidad.
12. **Implementación KMP:** `AnalogyTrialDto` (doc) + banco `EX04` proporcional con clave canónica; matcher de respuesta proporcional (verificable, determinista). El modo libre depende de `BlockedCreativityEvaluatorCascade` → CALIBRATING permanente sin IA.
13. **Tests obligatorios:** adversarial "absurdo gramatical" (debe NO aceptarse en modo puntuable; en modo libre debe quedar CALIBRATING, nunca puntuar); unit matcher proporcional; anti-plantilla; copy-scanner del titular de riesgo.
14. **Criterios de aceptación (→APPROVE):** solo el modo proporcional A:B::C:? como ejercicio puntuable; modo libre explícitamente descriptivo/CALIBRATING; banco con holdout. Sin esto: mantener BLOCK.
15. **Rediseño propuesto:** RETIRAR el modo libre como puntuable y **reconvertir EX04 en un ejercicio de analogía proporcional verificable** (mapeo estructural puro contra banco). Eso es honesto sin IA y mide relación estructural real. El modo "escribe el puente" se conserva como práctica descriptiva sin score hasta IA.
16. **Impacto esperado:** bajo sin IA (solo el modo proporcional es señal limpia; el libre es teatro).
17. **Prioridad:** P3 (esperar a IA para el modo libre; el proporcional solapa con Lenguaje/relaciones semánticas).
18. **Riesgo residual:** el modo libre, si se deja jugable, destruye credibilidad del módulo entero con un solo titular. El proporcional puede memorizarse sin holdout duro.

---

### EX05 — Elaboración Incremental (SOLO DISEÑO)

1. **Veredicto:** REBUILD (no implementado; entrena la 2ª señal real del scorer —elaboration— pero `elaboration=palabras/idea` premia verbosidad, no profundidad; necesita techo anti-verbosidad y coherencia por banda).
2. **Constructo objetivo:** Elaboración / profundización progresiva de una idea en capas (detalle/consecuencia/mejora/excepción).
3. **Señal primaria válida:** `depthReached` = capas válidas encadenadas (con techo de palabras por capa para que verbosidad no infle) + `elaboration` con cap. Actualiza señal de práctica de elaboración si pasa min-n.
4. **Señales secundarias:** `layerTypeSpread`, `redundancyRate`, `coherenceProxy` (overlap MODERADO con capa previa — banda, ni 0 ni 1), `depthLatencyMs`.
5. **Contaminantes principales:** logorrea (escribir largo y vacío); `coherenceProxy` por overlap léxico es burdo (capa profunda con vocabulario nuevo se marca "desconectada"). RedundantWith: ninguno fuerte (única que mide profundidad encadenada).
6. **Ataques de gaming:** escribir capas largas con relleno → infla elaboration; repetir con sinónimos para pasar redundancyThreshold; reiniciar idea encubiertamente (overlap nulo con la cadena).
7. **Gates obligatorios:** techo de `minWordsPerLayer` y cap de palabras-que-cuentan (anti-verbosidad); `coherenceProxy` con banda como EJE (no bonus); anti-redundancia (overlap alto = rechazo) y anti-desconexión (overlap nulo con TODA la cadena = flag de reinicio); min-n capas válidas.
8. **Métrica y scoring:** `practiceScore = 0.4·norm(depthReached) + 0.3·norm(elaborationCapped) + 0.2·layerSpread − 0.1·redundancyRateNorm`. n_min: ≥minN capas válidas. Invalidaciones: timing, reinicio, todas redundantes. CALIBRATING hasta n_min.
9. **Banco de ítems:** semillas + prompts de capa rotativos; banco de marcadores para la variante adversa (mejora/riesgo: un riesgo debe introducir término negativo/condicional del banco, verificable); holdout de semillas.
10. **UX y copy PERMITIDO:** "práctica de desarrollar una idea a fondo", "profundidad: N capas".
11. **UX y copy PROHIBIDO:** "tu profundidad de pensamiento", "rigor", far-transfer, presentar palabras/idea como "profundidad".
12. **Implementación KMP:** `ElaborationTrialDto`+`LayerDto` (doc); utilidad local de overlap (Jaccard/coseno léxico); `LayerStackField`. No existe.
13. **Tests obligatorios:** adversarial logorrea (capas largas vacías NO deben subir score por el cap); unit coherenceProxy-banda (overlap 0 y 1 ambos penalizan); anti-reinicio; golden depthReached; copy-scanner.
14. **Criterios de aceptación (→APPROVE):** (a) cap de palabras testeado contra perfil verboso; (b) coherenceProxy como banda y eje, no longitud; (c) variante adversa (mejora/riesgo) con verificación de término negativo; (d) holdout de semillas.
15. **Rediseño propuesto:** el del doc con la **variante adversa (alternar mejora/riesgo)** como modo principal: un "riesgo" válido debe contener un marcador negativo/condicional del banco → verificable localmente, fuerza pensamiento dialéctico real y no premia mera verbosidad.
16. **Impacto esperado:** medio (elaboration es señal real del scorer; con techo y banda deja de ser logorrea).
17. **Prioridad:** P2.
18. **Riesgo residual:** sin IA no se distingue profundidad real de palabrería estructuralmente correcta; coherencia léxica es burda; incentivo residual a longitud.

---

### EX06 — Detección de lo Inverosímil / Rareza (SOLO DISEÑO)

1. **Veredicto:** REBUILD (no implementado, pero es el ÚNICO diseño defendible sin IA: rareza relativa a banco × plausibilidad. Apruébese SOLO cuando el banco de frecuencias esté construido, auditado y con cobertura declarada).
2. **Constructo objetivo:** Originalidad operacionalizada honestamente como rareza relativa a un banco de referencia (cola de frecuencia), no originalidad absoluta.
3. **Señal primaria válida:** `rarityScore = media(1 − freq_rel) de ideas válidas` **multiplicado por `plausibilityRate`** (clave: el absurdo no gana). Actualiza señal de práctica de "exploración de cola" si banco tiene cobertura suficiente; si no → CALIBRATING.
4. **Señales secundarias:** `validRareCount`, `coverageOfTail`, `plausibilityRate`, `fluency`, `elaboration`.
5. **Contaminantes principales:** sesgo de quien curó el banco (frecuencia mal estimada → premia rareza falsa); plausibilidad heurística débil (absurdo bien formado pasa). Banco = punto único de fallo. RedundantWith: parcialmente EX01 (ambos producen ideas), pero la dimensión rareza-vs-banco es varianza nueva legítima.
6. **Ataques de gaming:** teclear respuestas raras-pero-absurdas (alta rareza, baja plausibilidad) → mitigado por `rarityScore·plausibilityRate`; memorizar la cola si se revela el banco (mitigado por banco oculto + feedback solo binario); variante 3 (anti-banco vivo) inflada con histórico corto.
7. **Gates obligatorios:** banco declara tamaño/cobertura; `rarityScore` CALIBRATING si cobertura baja; plausibilidad = pertenencia a banco-de-válidas PRIORITARIO sobre heurística (heurística solo da rareza provisional NO puntuable); banco oculto; rotación dura de prompts; variante 3 con mínimo de histórico antes de activar rareza relativa personal.
8. **Métrica y scoring:** `practiceScore = 0.5·rarityScore·plausibilityRate + 0.3·norm(validRareCount) + 0.2·coverageOfTail`. n_min: ≥minN válidas Y cobertura de banco ≥umbral. Invalidaciones: banco insuficiente → rarityScore=CALIBRATING; absurdo (rara+implausible) → flag, no cuenta. Agregación inter-sesión: media ponderada; variante 3 actualiza banco por-usuario cifrado.
9. **Banco de ítems:** prompts con distribución de respuestas pre-rankeada por frecuencia (común→raro), tamaño/cobertura auditados, banco-de-válidas para plausibilidad; holdout implícito (cola oculta); rotación; inter-rater sobre plausibilidad.
10. **UX y copy PERMITIDO:** "rareza respecto a respuestas comunes, no medida de originalidad absoluta" (copy explícito del doc — correcto); medidor común/raro.
11. **UX y copy PROHIBIDO:** "tu originalidad", "creatividad", "pensamiento original" como medida absoluta, far-transfer. NUNCA llamar `rarityScore` "originalidad".
12. **Implementación KMP:** `RarityTrialDto`+`RarityResponseDto` (doc); `EX06_FREQUENCY_BANK` con lookup determinista de freq_rel; filtro de plausibilidad (banco-válidas + heurística). Variante 3 = banco por-usuario cifrado en `SecureCreativitySessionStore`.
13. **Tests obligatorios:** adversarial "rara-basura" (alta rareza+baja plausibilidad NO debe puntuar); unit lookup de frecuencia; cobertura→CALIBRATING; property rarityScore∈[0,1]; golden; copy-scanner ("originalidad" prohibido).
14. **Criterios de aceptación (→APPROVE):** (a) banco de frecuencias construido con cobertura declarada y auditada; (b) plausibilidad prioriza banco-válidas; (c) rarityScore·plausibilityRate verificado contra perfil absurdo; (d) banco oculto + rotación; (e) variante 3 con mínimo de histórico.
15. **Rediseño propuesto:** implementar exactamente el diseño del doc (es el más honesto del set). Es el ejercicio que el módulo debería priorizar como su señal-insignia sin IA. Construir el banco de frecuencias es el trabajo crítico.
16. **Impacto esperado:** medio-alto dentro del entrenamiento (única operacionalización auditable de "rareza" sin IA).
17. **Prioridad:** P1 (el más defendible — debería construirse primero junto a EX01).
18. **Riesgo residual:** todo depende del banco (sesgo del curador); plausibilidad heurística sigue siendo débil; rareza léxica ≠ originalidad semántica (declarado, pero el usuario puede leerlo como capacidad).

---

### EX07 — Reencuadre del Problema (SOLO DISEÑO)

1. **Veredicto:** REBUILD (no implementado; problem-finding es genuinamente creativo, pero "detección de dimensión de marco" por marcadores léxicos es casi adivinanza y la tarea es conceptualmente difícil → riesgo de medir comprensión-de-instrucciones).
2. **Constructo objetivo:** Reencuadre / problem-finding (generar formulaciones alternativas del problema antes de resolver).
3. **Señal primaria válida:** `frameDiversity` = dimensiones de marco distintas alteradas / disponibles — declarada PROXY por marcadores; + `calibrationError` (|dimensión declarada − detectada|) como señal metacognitiva central honesta. Actualiza práctica si pasa min-n y tras descartar warm-up.
4. **Señales secundarias:** `validReframeCount`, `orthogonalityRate`, `reframeLatencyMs`.
5. **Contaminantes principales:** detección de dimensión por léxico = "usó vocabulario de N categorías", no reencuadre real; comprensión-de-instrucciones contamina early sessions; idioma/cultura del banco. RedundantWith: parcial con Decisiones (reformular) — diferenciar por foco en dimensiones de marco.
6. **Ataques de gaming:** parafrasear el original con palabras de otra dimensión → frameDiversity falso; declarar dimensiones que no cambió (mitigado por calibrationError que compara declarado vs detectado).
7. **Gates obligatorios:** onboarding/ejemplo obligatorio antes del primer trial (mitiga curva); descartar primeras sesiones del baseline (warm-up); `frameDiversity` declarado proxy, calidad CALIBRATING sin IA; rechazo de paráfrasis del enunciado (overlap alto); detección de dimensión por banco de marcadores, no por autoevaluación (autoeval solo alimenta calibración).
8. **Métrica y scoring:** `practiceScore = 0.4·frameDiversity + 0.3·norm(validReframeCount) + 0.2·orthogonalityRate − 0.1·calibErrorNorm`. n_min: ≥minN reformulaciones válidas, ≥1 sesión warm-up descartada. Invalidaciones: paráfrasis, vacío, timing. CALIBRATING para calidad de reencuadre siempre.
9. **Banco de ítems:** problemas con marco por defecto + marcadores de dimensión (sujeto/objetivo/escala/restricción/temporalidad) pre-etiquetados (inter-rater); holdout de problemas; rotación; la variante "escala" (individual/grupo/sistema) como modo de entrada verificable.
10. **UX y copy PERMITIDO:** "práctica de replantear problemas", "cambiaste N dimensiones del marco".
11. **UX y copy PROHIBIDO:** "tu capacidad de resolver problemas", "innovación", far-transfer, "creatividad". frameDiversity sin "proxy por marcadores".
12. **Implementación KMP:** `ReframeTrialDto`+`ReframeDto` (doc); matcher de marcadores de dimensión (banco `EX07`); onboarding screen; `FrameDimensionChips`. No existe.
13. **Tests obligatorios:** unit matcher de dimensión (paráfrasis → rechazo); adversarial "vocabulario-de-N-categorías-sin-reencuadrar"; calibrationError unit; golden; copy-scanner; test de que warm-up se descarta del baseline.
14. **Criterios de aceptación (→APPROVE):** (a) onboarding obligatorio; (b) variante escala como modo de entrada; (c) frameDiversity etiquetado proxy + calidad CALIBRATING; (d) warm-up descartado; (e) anti-paráfrasis.
15. **Rediseño propuesto:** el del doc con la **variante "escala" (individual/grupo/sistema)** como modo de entrada (la más verificable localmente) y `calibrationError` como señal metacognitiva central. La metacognición es lo que da una señal honesta sin depender de comprensión semántica.
16. **Impacto esperado:** medio (constructo valioso e infravalorado; pero verificación léxica débil y curva de aprendizaje alta).
17. **Prioridad:** P2.
18. **Riesgo residual:** "dimensión cambiada" por marcadores es aproximada (falsos +/−); en early sessions mide comprensión-de-instrucciones; alta dependencia cultural/idiomática.

---

### EX08 — Improvisación Restringida en Tiempo Real (SOLO DISEÑO)

1. **Veredicto:** BLOCK (no implementado; mide velocidad de tecleo, no fluidez ideacional —confound motor crítico, prohibido por charter—; y "creatividad bajo presión" es científicamente cuestionable: la presión mata la divergencia).
2. **Constructo objetivo:** REPOSICIONADO obligatorio: NO "creatividad bajo presión" sino **reducción de bloqueo / auto-censura** (fluidez sostenida sin congelarse).
3. **Señal primaria válida:** NINGUNA válida sin (a) normalización de IBI por baseline motora y (b) timebase de resolución suficiente. `validThroughput` e `rhythmStability` dependen de IBI por idea, que hoy NO se captura (loop batch) y que sin baseline motora = destreza de teclado.
4. **Señales secundarias:** `fluency`, `blockingEvents`, `validityRate`, `throughputDrift`.
5. **Contaminantes principales:** **velocidad/destreza de tecleo = el confound núcleo** (teclado rápido gana, lento "bloquea" por motricidad → "familiaridad con la UI", prohibido). Presión que premia ideas obvias, no creativas. RedundantWith: EX01/EX06 (mismo producir-ideas; la variante "solo-cola" es literalmente EX06 bajo presión).
6. **Ataques de gaming:** spam de ideas cortas distintas a alta cadencia → throughput inflado (mitigado por validityRate+elaboración SI se implementan); teclado mecánico/macro.
7. **Gates obligatorios:** **timebase:** IBI requiere resolución de input-event; el repo solo tiene timebase autoritativa frame-accurate en **Speed/Android** (`SpeedTimebase` Choreographer+MotionEvent); en desktop/iOS y en cualquier otra base = fallback monotónico NO autoritativo → **gate device_unreliable/UNTRUSTED_TIMEBASE: jugable, pero NO actualiza perfil**. Además: normalización de IBI por mini-calibrador motor no-creativo (HARD); validityRate y elaboración mínima como gates duros (throughput de basura no puntúa); presión leve-only techo HARD=3, `pressureCueIntensity` nunca auto-sube, check-in, exit visible; **NO penalizar abandono** (señal de seguridad).
8. **Métrica y scoring:** `practiceScore = 0.4·norm(validThroughput_normalizadoMotor) + 0.25·rhythmStability + 0.25·validityRate − 0.1·blockingRateNorm`. n_min: ≥minN válidas Y baseline motora capturada. Invalidaciones: sin baseline motora → métricas de ritmo = null; timebase no autoritativa → no actualiza perfil; IBI<350ms+baja elaboración = spam. CALIBRATING hasta calibrador motor. Eventos extremos (IBI) sobre datos crudos (regla §2.1.7).
9. **Banco de ítems:** prompts de fluidez; rotación; **mini-calibrador motor** (banco de tecleo no-creativo) como pre-requisito; holdout.
10. **UX y copy PERMITIDO:** "práctica de generar ideas sin bloquearte", "ritmo en banda/bajo". Exit siempre visible; check-in neutral si sobrecarga.
11. **UX y copy PROHIBIDO:** "creatividad bajo presión", "rendimiento bajo estrés", "tu fluidez creativa", "multitarea", far-transfer, cualquier inferencia de estado emocional. El nombre mismo no puede decir "creatividad bajo presión".
12. **Implementación KMP:** `ImprovTrialDto`+`TimedIdeaDto` (doc); IBI requiere capturar timestamp por idea con timebase de alta resolución (reusar patrón `SpeedTimebase`, hoy solo Android-autoritativo); `OverloadProxy` espejo `DefaultEmotionAdaptiveEngine`; `RhythmMeterField`. Nada existe.
13. **Tests obligatorios:** adversarial spam-rápido (validityRate debe anular throughput); unit normalización IBI por baseline; gate timebase no-autoritativa → no actualiza perfil; test abandono NO penaliza; copy-scanner ("creatividad bajo presión" prohibido); simulación perfil "teclado rápido vacío".
14. **Criterios de aceptación (→APPROVE):** (a) calibrador motor + normalización IBI implementados+testeados; (b) gate device_unreliable real (no hardcodeado a false como en Speed `SpeedSessionStateHolder:259-260`); (c) objetivo reposicionado a anti-bloqueo en nombre y copy; (d) presión leve techo 3; (e) abandono no penaliza. Sin (a)+(b)+(c): mantener BLOCK.
15. **Rediseño propuesto:** si se construye, **variante sprint-recovery** (alternar presión/ritmo libre, mide recuperación de fluidez, no pico) como modo principal + objetivo "anti-bloqueo". Si no se asume el coste del calibrador motor + timebase autoritativa multiplataforma → **RETIRE**: sin ellos mide destreza de teclado, que el charter prohíbe explícitamente.
16. **Impacto esperado:** bajo (confound motor domina; presión contraproducente para divergencia; redundante con EX01/EX06).
17. **Prioridad:** P3.
18. **Riesgo residual:** incluso con normalización, IBI mezcla cognición y motricidad; presión induce frustración (mitigado leve-only); claim de "fluidez" sigue siendo frágil. El gate device_unreliable hoy está hardcodeado a false en el módulo Speed que serviría de patrón → riesgo de copiar un gate muerto.

---

### Recomendaciones de comité (priorizadas)

- **P1 — construir primero las señales honestas:** EX06 (rareza×plausibilidad, banco auditado) y EX01 (usos con prohibición+holdout). Son las dos operacionalizaciones defendibles sin IA.
- **P2:** EX05 (elaboración con techo+banda, variante adversa), EX03 (síntesis con ventana-elaboración+calibrationError), EX07 (reencuadre con onboarding+escala+calibrationError).
- **MERGE/RETIRE:** EX02 → MERGE en EX01 salvo que se construya clasificador+assembler+RT (es el mismo loop hoy = varianza redundante).
- **BLOCK hasta IA o calibrador:** EX04 (solo modo proporcional puntuable; libre = CALIBRATING) y EX08 (calibrador motor + timebase autoritativa + reposicionamiento, o RETIRE).
- **Transversal obligatorio antes de cualquier "implemented":** (1) `CreativityMetricIntegrityLayer` deja de ser Blocked stub y gana detectores reales (near-duplicate/overlap, spam, copia de enunciado) testeados con perfiles sintéticos adversarios; (2) copy-scanner de lexemas prohibidos para el módulo creativity (hoy solo existen scanners para arena y emotions); (3) mantener Creatividad CALIBRATING en el perfil global (correcto hoy) hasta tener señales con holdout.