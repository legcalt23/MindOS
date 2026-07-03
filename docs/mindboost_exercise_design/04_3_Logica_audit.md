# Auditoría Logica

> Comité de auditoría MindBoost (Director científico cognitivo · Psicometría · Ingeniero KMP · Red-team anti-gaming · Diseñador de bancos · UX · Legal/safety · Arquitecto de producto · Competidor hostil · Usuario adversario). Lente: dura, no complaciente. Veredicto por ejercicio, no por catálogo.
>
> **Ground-truth del repo (no del diseño):** Lógica mide `timeSpentMs` con `EpochClock` (`Clock.System` epoch-millis, `LogicStateHolder.kt:105,113`) — wall-clock NO autoritativo, sin Choreographer/vsync → toda métrica RT/velocidad es no autoritativa y NO puede actualizar perfil sin gate `device_unreliable`. `DefaultReasoningScorer` (ICR process-first: 0.35 final + 0.20 regla + 0.15 premisas[Jaccard] + 0.15 justificación + 0.10 confianza + 0.05 independencia, penalty 0.10 impulsivo + 0.15 error conceptual). NO existe min-n en Lógica (solo en Focus `DefaultMetricsEngine`); `thetaEstimate`/`uncertainty` se computan pero NO alimentan selección (no hay IRT/CAT real). Banco = `DefaultLogicContentProvider`: 6 ítems autorados fijos, 1 solo `logicalStructureId` isomórfico (`conditional_refutation_p_and_not_q`), sin generador procedural, sin holdout, sin inter-rater, sin rotación → memorizable. Anti-gaming: SOLO detección de impulsividad cruda en scorer/adapter; CERO perfiles sintéticos adversarios, CERO tests adversariales/jank/golden.
>
> **Reglas no negociables aplicadas:** (1) RT sin timebase autoritativa NO actualiza perfil. (2) Anti-gaming solo cuenta si implementado+testeado con perfil sintético; ausente = BLOCK/REVISE crítico. (3) Métrica con n insuficiente prohibida. (4) Cero copy de far transfer ni para negarlo. (5) Banco de juicio: inter-rater ≥80%, banco grande, holdout, rotación, HIGH_PRIOR, anti-memo, control cultural/léxico. (6) UI honesta (proxy ≠ constructo). (7) null nunca 0. (9) sin redundancia. (10) pocos limpios > muchos ruidosos.

---

## E1 — Cadena Deductiva Progresiva (Modus Ponens/Tollens en escalera)

1. **Veredicto: REVISE.** El más sólido de los 10 sobre el ICR existente; bloqueado de APPROVE por banco autorado memorizable y falta de penalty de premisas irrelevantes.
2. **Constructo objetivo:** encadenamiento de inferencias condicionales válidas (modus ponens/tollens, silogismo hipotético) con justificación premisa-correcta; sub-skill `DEDUCTION_FROM_PREMISES`.
3. **Señal primaria válida (actualiza perfil si pasa gates):** ICR process-first agregado (no por sesión) + `premiseScore` Jaccard + accuracy de conclusión, solo con n≥umbral y `confident=true`.
4. **Señales secundarias (feedback/metacognitivo, NO perfil):** `ruleScore`, `confidenceScore` (calibración), tasa de falacia específica (afirmación-consecuente vs negación-antecedente), tiempo por inferencia (descriptivo).
5. **Contaminantes principales:** carga de lectura (densidad de texto mide comprensión, no deducción); memoria de trabajo en cadenas largas; familiaridad con 6 ítems fijos.
6. **Ataques de gaming:** marcar TODAS las premisas para inflar Jaccard; memorizar ítem→respuesta tras 2-3 sesiones; responder rápido tras memorizar (el impulsivo solo penaliza si además falla).
7. **Gates obligatorios:** generador procedural antes de `implemented`; penalty explícito por premisa irrelevante marcada; control de comprensión lectora o modo simbólico baseline; min-n para agregar ICR; gate `device_unreliable` para cualquier lectura temporal (descriptiva, nunca perfil).
8. **Métrica y scoring:** fórmula `DefaultReasoningScorer` (ya existe). **n_min:** ≥12 ítems del mismo skill cross-sesión antes de `confident=true`; bajo eso → `null`/calibrating. **Invalidaciones:** impulsividad ≥2, Jaccard≈0 con accuracy alta (adivina la conclusión). **Agregación inter-sesión:** media móvil de ICR sobre `LogicSession` summaries (capa de serie HOY INEXISTENTE — hueco). **Actualiza perfil:** solo con isomorfo correcto ICR≥70 + n_min alcanzado.
9. **Banco:** ≥40 estructuras seeded × ≥3 variantes superficie; `DeductiveChainGenerator` determinista; validación por construcción (la conclusión se sigue por forma); holdout ≥20% nunca entrenado; inter-rater N/A (verdad formal) pero requiere chequeo de unicidad de soporte; rotación por `surfaceVariantId`; HIGH_PRIOR no aplica (forma abstracta); anti-memo vía generación + rotación.
10. **UX/copy PERMITIDO:** "encadenar inferencias válidas contra tu baseline", "practicar deducción condicional", "—" cuando calibrando.
11. **UX/copy PROHIBIDO:** "inteligencia lógica", "pensamiento crítico", CI, mejora de matemáticas/código, "razonas mejor en la vida".
12. **Implementación KMP:** reusa renderer selección-múltiple actual (es el paradigma vigente). `DeductiveChainGenerator : LogicContentProvider` seeded; campo opcional `inferenceDepth`; añadir penalty de premisa irrelevante en `overlapScore` (hoy solo Jaccard simétrico). Persistencia summary namespace `Logic`.
13. **Tests obligatorios:** golden de scoring (input fijo→ICR esperado); adversarial "marca-todas-las-premisas" debe bajar score; perfil sintético memorizador detectado; determinismo de seed; min-n→`null`.
14. **Criterios de aceptación (→APPROVE):** generador procedural verde + penalty premisa irrelevante testeado + min-n/`null` + capa de agregación inter-sesión + golden tests.
15. **Rediseño propuesto:** generador seeded con escalera `chainLength` 1-5, modo simbólico (P/Q/R) como baseline anti-lectura, penalty asimétrico (premia premisa requerida, castiga irrelevante), agregación cross-sesión con IC.
16. **Impacto esperado (SOLO dentro del entrenamiento):** alto (núcleo deductivo limpio, reusa infra probada).
17. **Prioridad: P1.**
18. **Riesgo residual:** mide lectura si no se fuerza modo simbólico; agregación inter-sesión inexistente posterga "mejora honesta".

---

## E2 — Detector de Contradicción Latente (consistencia de un conjunto)

1. **Veredicto: REVISE.** Mecánica limpia; bloqueado por d′ no estimable por sesión listado como primario y núcleo mínimo potencialmente no único.
2. **Constructo objetivo:** juicio de consistencia de un conjunto e identificación del par/triple mínimo contradictorio; `CONTRADICTION_DETECTION`.
3. **Señal primaria válida:** accuracy del binario consistente/inconsistente + `premiseScore` Jaccard del núcleo mínimo, agregados; ICR process-first.
4. **Señales secundarias:** sesgo de respuesta (tendencia a "inconsistente"), `confidenceScore`, tasa de núcleo no-mínimo.
5. **Contaminantes principales:** memoria de trabajo con `statementCount=7`; lectura; familiaridad con ítems fijos.
6. **Ataques de gaming:** heurística "siempre inconsistente"; marcar todas las afirmaciones como núcleo; acertar el binario por suerte sin razonar el núcleo.
7. **Gates obligatorios:** `consistentRatio` balanceado; unicidad del núcleo mínimo garantizada por construcción O scoring que acepte cualquier núcleo válido; cap `statementCount≤5` hasta validar; **d′ NO por sesión** (solo agregado histórico o `null`); generador.
8. **Métrica y scoring:** ICR estándar; `correct`=binario; `premiseScore`=Jaccard(núcleo) solo si inconsistente (renormalizar en consistentes). **n_min:** d′ requiere ≥16 señal + ≥16 ruido cross-sesión → si no, `null`. **Invalidaciones:** falsa alarma repetida, Jaccard≈0 con binario correcto. **Agregación:** d′ SOLO sobre serie histórica (hueco). **Actualiza perfil:** ICR agregado + isomorfo; d′ nunca por sesión.
9. **Banco:** ≥40 conjuntos seeded; `ConsistencyGenerator` con `CounterexampleValidator` validando derivaciones internas; unicidad de núcleo por construcción; holdout; rotación; conjuntos consistentes balanceados como control.
10. **UX/copy PERMITIDO:** "detectar inconsistencias contra tu baseline", "—" para d′ calibrando.
11. **UX/copy PROHIBIDO:** "pensamiento crítico general", "detectas mentiras/falacias", far transfer.
12. **Implementación KMP:** selección-múltiple + tap binario (mapear a `selectedAnswerId` de 2 opciones o nuevo `binaryJudgment`). `ConsistencyGenerator : LogicContentProvider` seeded. d′ requiere capa de serie nueva.
13. **Tests obligatorios:** golden scoring; unicidad de núcleo por construcción; adversarial "siempre inconsistente" → no sube; "marca-todo-el-núcleo" penalizado; d′ con n bajo → `null`.
14. **Criterios de aceptación:** generador + unicidad de núcleo + d′ diferido a serie con n_min + cap `statementCount` + golden/adversarial verdes.
15. **Rediseño propuesto:** generador con núcleo mínimo único garantizado, `derivationDepth` 0-2, d′ reportado SOLO agregado con IC, consistentes balanceados.
16. **Impacto esperado:** medio-alto (consistencia es un sub-skill distinto y poco entrenado).
17. **Prioridad: P1.**
18. **Riesgo residual:** sin capa de serie, d′ permanece `null` indefinidamente; con statementCount alto mide WM.

---

## E3 — Necesario vs Suficiente (cuantificador de condiciones)

1. **Veredicto: REVISE.** Candidato más cercano a "cablear primero": núcleo valioso, render single-tap existente, P0 acotados (azar + sesgo de superficie).
2. **Constructo objetivo:** distinguir condición necesaria/suficiente/ambas/ninguna; combate confusión condicional→bicondicional; `NECESSARY_SUFFICIENT`.
3. **Señal primaria válida:** accuracy de clasificación (4-way) + ICR; tasa de la confusión canónica necesaria↔suficiente vía `errorTags`.
4. **Señales secundarias:** sesgo bicondicional ("ambas"), `confidenceScore` (clave: distinción contraintuitiva), `independenceScore`.
5. **Contaminantes principales:** comprensión precisa del enunciado (riesgo de medir lenguaje); 25% de azar con 4 opciones; longitud/posición del distractor (sesgo de superficie).
6. **Ataques de gaming:** detectar el distractor por longitud/posición; responder fijo "ambas"; adivinar el 25% con n bajo.
7. **Gates obligatorios:** aleatorizar orden y longitud de opciones (anti-superficie) testeado; `biconditionalRatio` balanceado; reportar accuracy con n y `confident=false` bajo umbral; contador de confusión persistente (plateau).
8. **Métrica y scoring:** ICR estándar; `conceptualError=1` cuando `distractorType`=confusión necesaria/suficiente. **n_min:** ≥16 clasificaciones cross-sesión (4-way, 25% azar). **Invalidaciones:** confusión repetida sin mejora tras micro-lección (plateau), impulsividad, sesgo "ambas". **Agregación:** accuracy y tasa-confusión sobre serie. **Actualiza perfil:** ICR + isomorfo, n_min alcanzado.
9. **Banco:** ≥40 condicionales seeded; `NecessarySufficientGenerator`; modo simbólico como control de lenguaje; holdout; rotación `surfaceVariantId`; HIGH_PRIOR bajo (forma); anti-memo por generación.
10. **UX/copy PERMITIDO:** "clasificar condiciones contra tu baseline", "distinguir necesaria de suficiente".
11. **UX/copy PROHIBIDO:** "razonas mejor", "tomas mejores decisiones", causalidad real, CI.
12. **Implementación KMP:** reusa single-tap/choice (4 opciones). `conditionType: NecessarySufficientType`→`correctAnswerId`. `NecessarySufficientGenerator` seeded; micro-lección = `AdaptationAction.MICRO_LESSON`.
13. **Tests obligatorios:** golden scoring; adversarial sesgo de superficie (longitud/posición no predicen correcta); "responde-fijo-ambas" no sube; n bajo→`confident=false`; flag confusión-persistente.
14. **Criterios de aceptación:** aleatorización testeada + reporte n/`confident` + generador + flag plateau.
15. **Rediseño propuesto:** generador con longitud/posición de opciones aleatorizada y verificada, modo simbólico baseline, detector de confusión persistente como readiness (no penaliza perfil).
16. **Impacto esperado:** medio-alto (distinción contraintuitiva, poco cubierta, render barato).
17. **Prioridad: P1 (candidato a cablear primero entre los 10).**
18. **Riesgo residual:** 25% de azar mantiene accuracy ruidosa con sesiones cortas; depende de comprensión verbal en modo concreto.

---

## E4 — Caza-Contraejemplo (refutar una regla con un caso)

1. **Veredicto: BLOCK (→REBUILD).** Validez del near-miss depende ENTERAMENTE de un generador inexistente; con pack autorado se memoriza qué caso es. AUDIT.md lo marca block.
2. **Constructo objetivo:** generar/seleccionar el caso que mantiene premisas verdaderas y vuelve falsa la conclusión (`CounterexampleValidator.invalidates`); `COUNTEREXAMPLE`.
3. **Señal primaria válida (post-rebuild):** accuracy de selección exacta de contraejemplos + Jaccard de casos validados por `CounterexampleValidator`; ICR.
4. **Señales secundarias:** tasa near-miss (romper premisa vs romper conclusión), falsas alarmas en ítems sin contraejemplo, `confidenceScore`.
5. **Contaminantes principales:** dimensionalidad alta de casos = carga perceptiva/comparación visual, no lógica; memorización del caso en pack fijo.
6. **Ataques de gaming:** marcar TODOS los casos (Jaccard premia cobertura); memorizar el índice del caso correcto en los 6 ítems fijos.
7. **Gates obligatorios:** generador con `CounterexampleValidator` aplicado EN CONSTRUCCIÓN + test de unicidad; penalty explícito por near-miss/caso inválido seleccionado; ítems sin contraejemplo balanceados; cap `caseDimensionality≤3`.
8. **Métrica y scoring:** ICR; `correct`=conjunto exacto (o juicio "no hay" correcto); `premiseScore`=Jaccard(seleccionados, validados); `conceptualError`=near-miss (`distractorType=BROKE_PREMISE`). **n_min:** ≥12 cross-sesión. **Invalidaciones:** marca-todo, near-miss repetido. **Actualiza perfil:** solo con generador validado y n_min.
9. **Banco:** ≥40 reglas seeded; `CounterexampleGenerator` que construye near-misses por diseño, cada caso etiquetado por `CounterexampleValidator`; holdout; unicidad testeada; ítems sin contraejemplo como control de sesgo de refutación.
10. **UX/copy PERMITIDO:** "refutar reglas con casos contra tu baseline".
11. **UX/copy PROHIBIDO:** "argumentas mejor", "debate", "pensamiento crítico", far transfer.
12. **Implementación KMP:** reusa selección-múltiple; `AnswerOption.counterexampleCase` ya existe en el modelo. `CounterexampleGenerator` seeded es el bloqueante real. Penalty por near-miss en scoring.
13. **Tests obligatorios:** golden; cada caso generado pasa `CounterexampleValidator` con la etiqueta esperada (unicidad); adversarial "marca-todo" penalizado; falsa alarma "hay contraejemplo" cuando no lo hay = `conceptualError`.
14. **Criterios de aceptación:** `CounterexampleGenerator` robusto + validador en construcción + penalty near-miss + cap dimensionalidad + tests verdes.
15. **Rediseño propuesto:** REBUILD sobre generador procedural validado por `CounterexampleValidator`; near-misses construidos (rompen exactamente una premisa); penalty asimétrico; dimensionalidad capada.
16. **Impacto esperado:** alto (genera, no reconoce — altísimo valor lógico) SI el generador es robusto.
17. **Prioridad: P0 (bloqueante: no cablear hasta generador validado).**
18. **Riesgo residual:** generador débil produce contraejemplos ambiguos que invalidan toda la métrica; carga perceptiva con dimensionalidad alta.

---

## E5 — Silogismo Categórico (anti belief-bias)

1. **Veredicto: REVISE.** Aísla forma de contenido (raro y valioso), pero el belief-bias index NO es honesto por sesión y el binario aporta 50% azar.
2. **Constructo objetivo:** validez de silogismos categóricos (todos/algunos/ningún) disociando validez formal de plausibilidad de contenido (sesgo de creencia).
3. **Señal primaria válida:** accuracy de validez + ICR; **belief-bias index** = accuracy(congruente)−accuracy(incongruente) SOLO agregado histórico con n/`confident`.
4. **Señales secundarias:** accuracy por `figure`, error en premisas particulares ("algunos"), `confidenceScore`.
5. **Contaminantes principales:** contenido familiar sesgado por cultura (mitigar con abstracto); 50% azar binario; sesgo de respuesta "inválido" si `invalidRatio` no se balancea.
6. **Ataques de gaming:** responder fijo "inválido" (acierta `invalidRatio`); memorizar ítems concretos.
7. **Gates obligatorios:** `contentAbstraction=2` (sin-sentido) elimina atajo de creencia; `invalidRatio` balanceado; paridad de contenido entre congruente/incongruente garantizada por generador; belief-bias index agregado, NUNCA por sesión; reportar accuracy con n.
8. **Métrica y scoring:** ICR; `correct`=validez; `conceptualError=1` si error belief-congruente (`distractorType=BELIEF_BIAS`); el index NO entra al score del trial. **n_min:** ≥10 congruentes + ≥10 incongruentes cross-sesión para index; bajo eso `null`. **Agregación:** index sobre serie (hueco). **Actualiza perfil:** ICR + isomorfo abstracto; index nunca por sesión.
9. **Banco:** ≥40 silogismos seeded cubriendo 4 figuras; `CategoricalSyllogismGenerator` controla `beliefValidityCongruence` y paridad de contenido; modo abstracto puro como ancla; holdout; rotación.
10. **UX/copy PERMITIDO:** "juzgar validez formal contra tu baseline", "separar lo válido de lo creíble".
11. **UX/copy PROHIBIDO:** "razonas sin sesgos en la vida real", "debiasing", "más racional", CI.
12. **Implementación KMP:** single-tap binario; `syllogismFigure`/`isValid`/`beliefCongruence`→`correctAnswerId` de 2; isomorfo abstracto vía `transferConsistencyFor`. Index requiere capa de serie.
13. **Tests obligatorios:** golden; paridad de contenido congruente/incongruente verificada; adversarial "responde-fijo-inválido"; index con celdas vacías → `null`.
14. **Criterios de aceptación:** generador con paridad + index diferido a serie con n_min + abstracto puro + reporte n.
15. **Rediseño propuesto:** generador con paridad de contenido y abstracto puro por defecto; belief-bias index solo agregado con IC; banda honesta cuando calibrando.
16. **Impacto esperado:** medio-alto (disociar forma/contenido es el activo único de este ejercicio).
17. **Prioridad: P1.**
18. **Riesgo residual:** index inestable sin serie; contenido concreto introduce sesgo cultural; binario ruidoso con n bajo.

---

## E6 — Inferencia Bayesiana Cualitativa (actualizar creencia con evidencia)

1. **Veredicto: BLOCK (→REBUILD o RETIRE).** "Magnitud cualitativa" no falsable mide la heurística del autor; alto riesgo de pseudociencia/claim de racionalidad. AUDIT.md block (claims high).
2. **Constructo objetivo:** dirección y magnitud cualitativa de actualización bayesiana (sin aritmética); combate base-rate neglect y confusión P(E|H)↔P(H|E).
3. **Señal primaria válida (post-rebuild):** accuracy de DIRECCIÓN (sube/baja/igual) — la señal robusta; SOLO esa actualiza perfil.
4. **Señales secundarias:** error ordinal de magnitud (descriptivo, NO perfil), tasa de base-rate neglect, `confidenceScore`.
5. **Contaminantes principales:** subjetividad de "mucho/poco" (mide al autor); comprensión verbal de probabilidad; saliencia de la evidencia.
6. **Ataques de gaming:** responder fijo "sube-poco" (modo conservador acierta dirección frecuente); ignorar la evidencia.
7. **Gates obligatorios:** magnitud derivada de una escala bayesiana interna determinista con ZONA MUERTA amplia (respuesta inequívoca por construcción) o magnitud NO puntúa; copy auditado por `review-cognitive-claims` (prohibido claim de debiasing/racionalidad); modo con bandas puramente visuales (anti-lenguaje); SIN números.
8. **Métrica y scoring:** ICR; `correct`=dirección; magnitud dentro de `justificationScore` SOLO si la zona muerta la hace inequívoca; `conceptualError`=neglect/condicional invertido. **n_min:** ≥16 cross-sesión. **Invalidaciones:** respuesta fija, neglect creciente. **Actualiza perfil:** SOLO dirección agregada; magnitud nunca por sí sola.
9. **Banco:** ≥40 ítems seeded con magnitud derivada por construcción (zona muerta); `QualitativeBayesGenerator`; modo visual; holdout; rotación; inter-rater ≥80% sobre la magnitud SI se autora a mano (preferible: derivar, no juzgar).
10. **UX/copy PERMITIDO:** "practicar actualización cualitativa con evidencia contra tu baseline".
11. **UX/copy PROHIBIDO:** "test de racionalidad", "debiasing", "menos sesgos en decisiones reales", "más racional", estadística clínica, CI.
12. **Implementación KMP:** single-tap (5 opciones ordinales) o renderer ordinal-slider; `priorBand`/`diagnosticity`/`correctDirection`/`correctMagnitude`. Generador con escala bayesiana interna + zona muerta.
13. **Tests obligatorios:** golden; toda magnitud generada cae fuera de la zona muerta (inequívoca); adversarial "fijo sube-poco" no sube; claims scan limpio; trampa de saliencia → cambio mínimo correcto.
14. **Criterios de aceptación:** magnitud por construcción con zona muerta + copy auditado + modo visual + solo-dirección al perfil. Si la magnitud no se puede hacer inequívoca → RETIRE.
15. **Rediseño propuesto:** REBUILD reduciendo el constructo a DIRECCIÓN (robusta) + magnitud opcional inequívoca por zona muerta; modo visual; claims cortados de raíz. Si no se logra inequívoco, RETIRE.
16. **Impacto esperado:** medio (núcleo real pero frágil; mucho del valor se pierde al recortar a dirección).
17. **Prioridad: P0 (bloqueante por claims y falsabilidad).**
18. **Riesgo residual:** dientes regulatorios por roce con "racionalidad/debiasing"; magnitud sigue siendo opinable si la zona muerta es estrecha.

---

## E7 — Transferencia Espejo (gemelo entrenado vs isomorfo)

1. **Veredicto: BLOCK.** UN solo `logicalStructureId` en el pack (`conditional_refutation_p_and_not_q`) → NO viable como está. Mejor alineado con el principio near-transfer del repo, pero bloqueado por banco. AUDIT.md block.
2. **Constructo objetivo:** medir explícitamente la transferencia isomorfa estructural (abstraer estructura vs memorizar superficie); meta-ejercicio sobre E1-E6.
3. **Señal primaria válida:** **transfer consistency** (1.0 sii gemelo entrenado correcto Y isomorfo correcto, regla 2026-06-15) + ICR del isomorfo condicionado a gemelo-correcto.
4. **Señales secundarias:** delta ICR gemelo→isomorfo (dependencia de superficie), falsas alarmas en decoys (sobre-generalización), `confidenceScore`.
5. **Contaminantes principales:** `interleaveGap` corto con pocos ítems (memoria reciente, no abstracción); banco con 1 estructura → no hay transferencia que medir.
6. **Ataques de gaming:** resolver el isomorfo por memoria del gemelo reciente (gap corto); con 1 estructura, todo es el mismo ítem.
7. **Gates obligatorios:** banco ≥8 estructuras isomórficas × ≥2 variantes de superficie cada una (BLOQUEANTE); interleaving Bjork ≥2 ítems intermedios; decoys validados por `CounterexampleValidator`/tests; transfer=0.0 inicial comunicado como "calibrando" (`null`/em-dash), NO como fallo.
8. **Métrica y scoring:** `transferScore`=`transferConsistencyFor` → parámetro `transferConsistencyScore` de `ReasoningScorer.score` (YA existe en la firma); modula progresión, no el ICR del trial. **n_min:** ≥6 pares completos cross-sesión antes de afirmar transfer; bajo eso calibrando. **Actualiza perfil:** transfer agregado con n_min; `highPerformanceDecision` ya consume isomorfos para gating de INCREASE.
9. **Banco:** ≥8 `logicalStructureId` × ≥2 `surfaceVariantId`; `IsomorphPairGenerator` produce gemelo+isomorfo con misma estructura; holdout; decoys estructurales validados; rotación.
10. **UX/copy PERMITIDO:** "aplicar la misma estructura en formas nuevas contra tu baseline", "transferencia en calibración".
11. **UX/copy PROHIBIDO:** "transferencia a la vida/estudios/trabajo", far transfer, CI, "generalizas mejor".
12. **Implementación KMP:** reusa renderers de E1-E6 (sin renderer nuevo); usa infra existente `transferConsistencyFor`/`isIsomorphicProbe`/`logicalStructureId`/`surfaceVariantId`/`TransferLevel`. `IsomorphPairGenerator` es el bloqueante.
13. **Tests obligatorios:** golden de `transferConsistencyFor` (1.0 sii ambos correctos — ya regresado 2026-06-15); pares comparten estructura difieren superficie; decoy estructural NO comparte estructura; transfer=0.0 inicial → `null` display.
14. **Criterios de aceptación:** banco ≥8 estructuras + `IsomorphPairGenerator` + decoys validados + comunicación honesta de calibrando.
15. **Rediseño propuesto:** construir el banco de pares isomorfos como precondición; mantener la regla ambos-correctos; mostrar transfer como banda "calibrando" hasta n_min de pares.
16. **Impacto esperado:** alto (el ejercicio que mejor encarna el principio near-transfer y reusa infra existente).
17. **Prioridad: P0 (bloqueante por banco; alto valor una vez desbloqueado).**
18. **Riesgo residual:** transfer=0.0 frecuente al inicio leído como regresión por el usuario; decoys mal construidos confunden en vez de discriminar.

---

## E8 — Ordenar la Derivación (reconstruir una prueba paso a paso)

1. **Veredicto: BLOCK.** Renderer drag nuevo introduce ruido motor; Kendall-tau debe validar contra el DAG (cualquier orden topológico), no contra una permutación fija. AUDIT.md block.
2. **Constructo objetivo:** reconstruir el orden lógico de una derivación (cada paso justificado por anteriores, no circular); dependencia lógica y no-circularidad.
3. **Señal primaria válida (post-rebuild):** ordering correctness = fracción de pares en orden de dependencia válido (Kendall-tau sobre el DAG) + exclusión de distractores; ICR.
4. **Señales secundarias:** errores de circularidad, distractores incluidos, `confidenceScore`.
5. **Contaminantes principales:** destreza táctil del drag (carga motora ≠ lógica); el `minimumReasoningTimeMs` no aplica igual (tiempo de manipulación vs razonamiento).
6. **Ataques de gaming:** reordenar al azar con muchos movimientos hasta acertar; soltar rápido sin razonar.
7. **Gates obligatorios:** `OrderingValidator` valida el DAG (acepta CUALQUIER orden topológico, no una permutación de referencia); separar tiempo-de-razonamiento (primera-decisión-firme) de tiempo-de-manipulación; gate de renderer (no `implemented` sin renderer + captura, aprendizaje 2026-06-18); gate `device_unreliable` para el tiempo.
8. **Métrica y scoring:** ICR; `correct`=orden topológicamente válido (cualquiera del DAG); `justificationScore`≈Kendall-tau de dependencia normalizada; `conceptualError`=circularidad o distractor incluido. **n_min:** ≥10 cross-sesión. **Invalidaciones:** muchos movimientos con Kendall≈azar, distractores repetidos. **Actualiza perfil:** ICR + validación por DAG.
9. **Banco:** ≥40 derivaciones seeded; `DerivationOrderingGenerator`; el DAG de dependencias es la autoridad; distractores plausibles por construcción; `circularTrapPresence`; holdout; rotación.
10. **UX/copy PERMITIDO:** "reconstruir el orden de una derivación contra tu baseline".
11. **UX/copy PROHIBIDO:** "demostración matemática", "programación", far transfer, CI.
12. **Implementación KMP:** **requiere renderer nuevo de drag/ordering** (único de los 10 que no reusa). `LogicResponse.orderedStepIds`; portar Kendall-tau de `ClosedScoringEngine.Ordering` (Lenguaje) a un `OrderingValidator` que valide el DAG. Gate de renderer.
13. **Tests obligatorios:** golden; `OrderingValidator` acepta todos los órdenes topológicos válidos y rechaza circulares; renderer↔clave; adversarial "reordena-al-azar"; separación tiempo motor/cognitivo.
14. **Criterios de aceptación:** renderer drag + captura testeados + `OrderingValidator` por DAG (no permutación) + separación de tiempos + generador.
15. **Rediseño propuesto:** REBUILD con `OrderingValidator` sobre DAG, separación tiempo-razonamiento/manipulación, gate de renderer; empezar por DAGs lineales (`validOrderingsCount=1`) antes de ramificados.
16. **Impacto esperado:** medio (estructura de prueba es única entre los 10, pero el coste/ruido del drag baja el ROI).
17. **Prioridad: P2 (alto coste de renderer; otros ejercicios primero).**
18. **Riesgo residual:** mide destreza táctil; feedback ambiguo con múltiples órdenes válidos.

---

## E9 — Inferencia Suprimida (cuando una premisa extra cambia la conclusión)

1. **Veredicto: BLOCK.** La supresión (Byrne) depende de conocimiento del mundo: abstraída desaparece el fenómeno; concreta mide cultura, no lógica. AUDIT.md block.
2. **Constructo objetivo:** reconocer cuándo una inferencia condicional válida deja de ser concluyente al añadir una condición habilitadora; deducción estricta (monótona) vs plausible (defeasible).
3. **Señal primaria válida (post-rebuild):** accuracy del juicio 3-way (se-sigue/ya-no/depende) SOLO si las condiciones habilitadoras son EXPLÍCITAS en el enunciado; ICR.
4. **Señales secundarias:** suppression discrimination = accuracy(habilitadora) y accuracy(control-estricto) por separado (descriptivo); tasa de supresión espuria; `confidenceScore`.
5. **Contaminantes principales:** conocimiento del mundo/pragmática (contaminante DOMINANTE); modo "depende" como imán de respuestas evasivas.
6. **Ataques de gaming:** responder fijo "depende"; usar conocimiento cultural en vez de la regla.
7. **Gates obligatorios:** condiciones habilitadoras DECLARADAS explícitamente en el ítem (lógica defeasible explícita, no sentido común implícito); penalizar/justificar el "depende"; ítems control (deducción pura) balanceados; discriminación agregada histórica.
8. **Métrica y scoring:** ICR; `correct`=juicio 3-way; `conceptualError`=suprimir deducción estricta válida o no suprimir habilitadora (`MONOTONICITY_ERROR`/`ENABLING_MISS`). **n_min:** ≥12 cross-sesión; discriminación ≥10 por celda. **Invalidaciones:** "depende" fijo, supresión espuria. **Actualiza perfil:** ICR + isomorfo; discriminación solo agregada.
9. **Banco:** ≥40 ítems seeded con condición habilitadora explícita en el texto; `SuppressionTaskGenerator`; control estricto balanceado; holdout; inter-rater ≥80% sobre la verdad del juicio (es banco de juicio); rotación; control cultural/léxico.
10. **UX/copy PERMITIDO:** "reconocer cuándo una conclusión deja de seguirse contra tu baseline".
11. **UX/copy PROHIBIDO:** "sentido común", "razonamiento del mundo real", far transfer, CI.
12. **Implementación KMP:** single-tap (3 opciones) + micro-secuencia de revelado (`LaunchedEffect` introduce la premisa extra, patrón focus 2026-06-18). `additionalPremiseType`/`suppressionCorrect`/`isStrictDeduction`. Generador seeded.
13. **Tests obligatorios:** golden; toda condición habilitadora es explícita (scan de banco); adversarial "depende-fijo"; control estricto nunca suprime; supresión espuria → `conceptualError`.
14. **Criterios de aceptación:** condiciones explícitas verificadas + control del "depende" + inter-rater ≥80% + generador + discriminación diferida a serie. Si no se logran explícitas → RETIRE.
15. **Rediseño propuesto:** REBUILD como lógica defeasible EXPLÍCITA (la condición habilitadora se declara en el texto), 3-way con justificación del "depende", control estricto balanceado.
16. **Impacto esperado:** medio (fenómeno profundo y único, pero estrecho margen entre "explícito y honesto" y "cultural y descartable").
17. **Prioridad: P0 (bloqueante por dependencia de conocimiento del mundo).**
18. **Riesgo residual:** al hacer explícitas las condiciones se pierde parte del efecto Byrne; "depende" sigue invitando a evasión.

---

## E10 — Puente de Analogía Estructural (mapear relaciones, no rasgos)

1. **Veredicto: BLOCK.** Dependencia cultural/léxica RIESGO ROJO; las analogías A:B::C:D rozan CI verbal salvo construcción sobre relaciones formales-abstractas. AUDIT.md block.
2. **Constructo objetivo:** mapear la estructura relacional entre dominios (A:B::C:?) eligiendo el término que preserva la RELACIÓN, no la similitud de rasgos; combate similitud superficial.
3. **Señal primaria válida (post-rebuild):** accuracy de mapeo relacional SOLO con relaciones formalmente definidas (contención, orden, oposición, causalidad simple) sobre estímulos abstractos/visuales; ICR.
4. **Señales secundarias:** surface-lure error rate (elegir similitud superficial), tasa de relación-invertida, `confidenceScore`.
5. **Contaminantes principales:** vocabulario/educación (analogías verbales = CI verbal); `relationCount=3` mide memoria de trabajo (mantener 3 relaciones); definición arbitraria de "superficial".
6. **Ataques de gaming:** usar conocimiento léxico en vez de mapeo; con banco fijo, memorizar la respuesta.
7. **Gates obligatorios:** TODAS las relaciones formalmente definidas sobre estímulos abstractos/visuales (CERO dependencia léxica) auditado por `review-cognitive-claims`; definición operacional de superficial vs relacional; cap `relationCount` (separar de WM); aleatorizar posición de correcta.
8. **Métrica y scoring:** ICR; `correct`=D relacionalmente correcto; `conceptualError`=similitud superficial o relación invertida (`SURFACE_LURE`/`INVERTED_RELATION`/`OTHER_RELATION`); relación nombrada correcta aporta a `justificationScore`. **n_min:** ≥16 cross-sesión. **Invalidaciones:** surface-lure creciente, impulsividad. **Actualiza perfil:** ICR + isomorfo, sin dependencia léxica verificada.
9. **Banco:** ≥40 analogías seeded de un BANCO DE RELACIONES FORMALES (no de palabras); `StructuralAnalogyGenerator`; superficial definido operacionalmente; holdout; rotación de dominios; control cultural/léxico estricto (mono-idioma sin claim translingüístico).
10. **UX/copy PERMITIDO:** "mapear relaciones estructurales contra tu baseline".
11. **UX/copy PROHIBIDO:** "creatividad", "inteligencia verbal", "vocabulario", CI, far transfer.
12. **Implementación KMP:** single-tap (4-5 opciones); `sourceRelation: RelationType`/`analogyMapping`/`distractorType`. `StructuralAnalogyGenerator` combina relaciones abstractas, NO palabras. Auditoría de claims obligatoria.
13. **Tests obligatorios:** golden; scan de banco verifica CERO dependencia léxica (relaciones formales); adversarial surface-lure; relación-invertida detectada; posición de correcta aleatorizada.
14. **Criterios de aceptación:** banco de relaciones formales-abstractas auditado (cero léxico) + definición operacional de superficial + cap `relationCount` + generador. Si una sola relación depende de significado de palabra → se descarta.
15. **Rediseño propuesto:** REBUILD sobre relaciones formales (contención/orden/oposición/causalidad) con estímulos abstractos o visuales; superficial definido por construcción; `relationCount` capado a 2.
16. **Impacto esperado:** medio (razonamiento relacional es núcleo lógico, pero el margen anti-cultural es estrecho).
17. **Prioridad: P0 (bloqueante por frontera cultural/CI verbal).**
18. **Riesgo residual:** "superficial" sigue siendo parcialmente del autor; relaciones formales abstractas pueden bajar validez ecológica y engagement.

---

## Síntesis transversal del módulo Lógica

- **Veredicto módulo:** **4 REVISE (E1, E2, E3, E5) · 6 BLOCK (E4, E6, E7, E8, E9, E10)**; E6/E9 candidatos a RETIRE si no cierran sus precondiciones. Cero APPROVE.
- **3 huecos estructurales que bloquean casi todo:** (1) NO hay generador procedural — los 6 ítems autorados se memorizan (false improvement no detectable, sin PlateauDetector). (2) NO hay min-n ni capa de agregación inter-sesión en Lógica → d′, belief-bias, suppression discrimination y transfer son no-estimables por sesión. (3) Anti-gaming NO implementado con perfiles sintéticos (solo impulsividad cruda) → ningún detector cuenta como existente.
- **Timebase:** todo el módulo usa `EpochClock` wall-clock; cualquier métrica RT es descriptiva y NO actualiza perfil sin gate `device_unreliable`. Lógica es proceso-primero, así que esto es menos letal que en Velocidad, pero `minimumReasoningTimeMs` (disparador de impulsividad) es wall-clock → heurístico, no preciso.
- **Cablear primero (al cerrar P0 acotados):** E3 (Necesario vs Suficiente) y E1 (Cadena Deductiva) — render barato, núcleo limpio, P0 verificables; E5 con su index diferido. NO cablear E4/E6/E7/E8/E9/E10 hasta cerrar generador/banco/claims.
