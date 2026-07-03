# Logica

> Contrato de seguridad: todos los ejercicios entrenan un **perfil cognitivo funcional** mediante **practica contra el baseline personal** bajo **dificultad adaptativa**. Ninguna salida diagnostica, mide CI ni promete mejora clinica. Toda metrica sin soporte estadistico suficiente se reporta `null` (em-dash), nunca 0; todo score arranca `preliminary = true`. Motores reales conectados: `ReasoningScorer` (ICR process-first), `DefaultReasoningScorer`, `DefaultLogicDifficultyAdapter`/`DifficultyAdapter`, mas la infra `LogicStateHolder`/`LogicGraph`/`SecureKeyValueStore`/`LocalLogicTrainingRepository`. Persistencia local cifrada (namespace `MindBoostStorageNamespaces.Logic`), nunca Firebase ni trials crudos: solo `LogicSession`+`SkillState` summaries.

> Nota de diseno transversal: el repo NO tiene generador procedural en Logica (hueco reconocido: `DefaultLogicContentProvider` es pack autorado fijo de 6 items). Cada ejercicio declara su contrato de generador seeded determinista que se enchufa al MISMO pipeline (`LogicContentProvider.sessionItems()` -> `ReasoningScorer.score` -> `LogicAttempt` -> `DifficultyAdapter.decide`). Donde haga falta un renderer nuevo se indica explicitamente; donde reuse el render single-tap/seleccion-multiple actual tambien.

---

## 1. Cadena Deductiva Progresiva (Modus Ponens/Tollens en escalera)

### Modulo
Principal: MindBoost Logica (deduccion proposicional). Secundarios: Memoria de trabajo (mantener cadena de inferencias), Velocidad de proceso (solo via control de impulsividad, nunca sumando).

### Objetivo cognitivo
Entrenar encadenamiento de inferencias validas (modus ponens, modus tollens, silogismo hipotetico) sobre cadenas de longitud creciente, exigiendo seleccionar la conclusion que se sigue NECESARIAMENTE y las premisas exactas que la soportan. Ataca el sub-skill `DEDUCTION_FROM_PREMISES` y la justificacion premisa-correcta.

### Hipotesis de transferencia
SI podria transferir: a variantes isomorfas de razonamiento condicional encadenado (near transfer medible via items isomorfos con distinto `logicalStructureId`/`surfaceVariantId`). NO se debe afirmar: mejora de "inteligencia logica" general, far transfer a matematicas/codigo, ni CI. El copy dice "encadenar inferencias validas contra tu baseline".

### Loop principal
1. Estimulo: 2-5 premisas condicionales + un hecho, renderizadas como tarjetas de texto neutro (simbolos abstractos P/Q/R, no contenido cultural).
2. Decision: el usuario (a) selecciona la conclusion correcta entre 4 opciones y (b) marca via seleccion multiple las premisas exactas que uso (`selectedPremiseIds`) y (c) elige la regla aplicada (`reasoningRule`).
3. Feedback: `DefaultLogicFeedbackEngine` por `distractorType` sin revelar la solucion; muestra si la cadena de soporte estaba completa.
4. Adaptacion: tras la sesion `DifficultyAdapter.decide` ajusta longitud de cadena y densidad de distractores.
5. Siguiente trial: nuevo item del generador seeded con longitud objetivo del staircase.

### Reglas
La conclusion debe seguirse necesariamente (no plausiblemente). Marcar una premisa no usada penaliza `premiseScore` (Jaccard). Elegir una regla no presente en `validReasoningRules` anula `ruleScore`. Responder antes de `minimumReasoningTimeMs` (2000ms) marca impulsividad. Distractores incluyen falacias (afirmacion del consecuente, negacion del antecedente).

### Inputs
Seleccion (conclusion: tap sobre 1 de 4), seleccion multiple (premisas: checkboxes), tap (regla: chips). Confianza: 3 chips LOW/MEDIUM/HIGH (`LogicConfidence`). Sin voz ni texto libre.

### Variables de dificultad
1. `chainLength` (numero de inferencias): 1-6.
2. `premiseCount` total: 3-9.
3. `distractorTrapCount` (falacias plausibles entre opciones): 0-3.
4. `irrelevantPremiseCount` (ruido para Jaccard): 0-4.
5. `ruleDiversity` (numero de reglas distintas requeridas en la cadena): 1-3.
6. `negationDepth` (premisas con negacion): 0-3.
7. `surfaceAbstraction` (0=cotidiano neutro, 1=simbolico P/Q): 0-1.
8. `requirePremiseTagging` (exigir marcar premisas): bool.

### Algoritmo adaptativo
`DefaultLogicDifficultyAdapter.decide` sobre ventana 6: error conceptual repetido (>=2, excluye impulsive/overconfident) -> DECREASE + MICRO_LESSON (baja `chainLength`); `avgIcr<55` -> DECREASE; `avgIcr>=80 && avgJustification>=0.65` -> INCREASE solo si probes isomorfos correctos con ICR>=70, si no HOLD. Fatiga: `impulsiveCount>=2` marca `FatigueSignal.POSSIBLE_IMPULSIVITY` y bloquea INCREASE. Baseline: `thetaEstimate` por skill ancla `chainLength` inicial.

### Metricas primarias
ICR (`totalIcr` 0-100, process-first), `premiseScore` (Jaccard de soporte), accuracy de conclusion, `justificationScore`.

### Metricas secundarias
`ruleScore`, `confidenceScore` (calibracion), tiempo de razonamiento por inferencia (descriptivo, no suma score), tasa de falacias especificas (afirmacion-consecuente vs negacion-antecedente) via `errorTags`.

### Senales de fatiga o mala calidad
`timeSpentMs<minimumReasoningTimeMs` en >=2 items (impulsividad cruda, sobre `allErrors` sin distinct, aprendizaje 2026-06-15), deriva de ICR descendente intra-sesion, premisas marcadas al azar (Jaccard ~0 con accuracy alta sugiere adivinar la conclusion), overconfidence (prob>=0.85 en error).

### Scoring sugerido
`weightedBase = 0.35*correct + 0.20*ruleScore + 0.15*premiseScore + 0.15*justificationScore + 0.10*confidenceScore + 0.05*independenceScore`; `penalty = 100*(0.10*impulsive + 0.15*conceptualError)`; `totalIcr = (100*weightedBase - penalty).coerceIn(0,100)`. Exactamente la formula de `DefaultReasoningScorer`; la velocidad solo resta via `impulsive`.

### Antitrampa / calidad de datos
Premisas etiquetadas DEBEN coincidir con `requiredPremiseIds` para puntuar justificacion (no basta acertar la conclusion). `minimumReasoningTimeMs` filtra adivinacion rapida. Distractores con `distractorType` detectan error conceptual real. Seed determinista hace cada sesion auditable/reproducible.

### Variantes
1. **Solo Tollens**: cadenas que exigen contrapositiva (negar consecuente -> negar antecedente).
2. **Cadena con ruido**: 3-4 premisas irrelevantes elevan `irrelevantPremiseCount`, midiendo seleccion de soporte.
3. **Isomorfico probe**: mismo `logicalStructureId` con `surfaceForm` distinta (cotidiano vs simbolico) para `transferConsistencyFor`.

### UX
Pantalla: stack de tarjetas-premisa con `verticalScroll` (aprendizaje 2026-06-25), zona de conclusion abajo (4 opciones, `MinTouchTarget=48dp`), barra de chips de regla y confianza. Feedback inmediato no revela solucion. Microanimacion funcional: resaltado de las premisas que el usuario marco al dar feedback (no decorativa). Duracion ideal: 6-8 items, ~6 min. Tokens `AppColors`/`AppSpacing`.

### Implementacion KMP
Reusa renderer de **seleccion multiple** existente (es exactamente el paradigma actual de Logica). Modelo: extiende `LogicItem` (ya tiene `premises`, `requiredPremiseIds`, `validReasoningRules`, `chainLength` via nuevo campo opcional `inferenceDepth: Int`). Generador nuevo `DeductiveChainGenerator : LogicContentProvider` (seeded, determinista) reemplazable por el `DefaultLogicContentProvider` autorado. Eventos a registrar (solo summary): `LogicSession`{itemId, totalIcr, premiseScore, ruleScore, impulsive, conceptualError, transferLevel}, `SkillState`{thetaEstimate, uncertainty}. Motores: `ReasoningScorer`/`DefaultReasoningScorer` (scoring), `DifficultyAdapter`/`DefaultLogicDifficultyAdapter` (post-sesion). Persistencia `LocalLogicTrainingRepository(secureStore)` namespace `Logic`, DTO `@Serializable`+`schemaVersion`. Estado minimo: `LogicUiState` actual + `inferenceDepth`.

### Riesgos
La densidad de texto puede medir lectura mas que logica (mitigar con `surfaceAbstraction` simbolico). Cadenas largas saturan memoria de trabajo y contaminan la senal deductiva. El pack autorado limita variantes hasta tener generador procedural real.

### Revision critica
El Revisor exige: (1) demostrar que `premiseScore` Jaccard no se puede gamear marcando TODAS las premisas (mitigar: penalizar premisas irrelevantes marcadas, no solo premiar las correctas). (2) Separar carga de lectura de carga deductiva: requerir un control de comprension lectora o el modo simbolico como baseline. (3) Sin generador procedural, 6 items autorados se memorizan en pocas sesiones -> false improvement no detectado (Logica no tiene PlateauDetector). Correccion obligatoria: implementar `DeductiveChainGenerator` seeded antes de exponer en `implemented`, y registrar `surfaceVariantId` para rotar formas.

### Veredicto
Aprobado con cambios: solido sobre el ICR actual, pero exige generador procedural + penalizacion de premisas irrelevantes antes de `ExerciseCatalog.implemented`.

---

## 2. Detector de Contradiccion Latente (consistencia de un conjunto)

### Modulo
Principal: MindBoost Logica (`CONTRADICTION_DETECTION`). Secundarios: Memoria de trabajo (mantener el conjunto), Control inhibitorio (resistir aceptar el conjunto plausible pero inconsistente).

### Objetivo cognitivo
Entrenar la deteccion de si un conjunto de afirmaciones es logicamente consistente o contiene una contradiccion (directa o derivada en 1-2 pasos), e identificar el par minimo que la genera. Ataca razonamiento sobre consistencia, no sobre derivacion hacia adelante.

### Hipotesis de transferencia
SI: deteccion de inconsistencia en variantes isomorfas (near transfer). NO: "pensamiento critico" generico, deteccion de falacias retoricas en lenguaje natural rico, ni far transfer. Copy: "detectar inconsistencias contra tu baseline".

### Loop principal
1. Estimulo: conjunto de 3-7 afirmaciones (condicionales, disyunciones, negaciones).
2. Decision: (a) consistente / inconsistente (tap binario) y, si inconsistente, (b) seleccionar el par/triple minimo que choca (seleccion multiple) + (c) confianza.
3. Feedback: indica si el par minimo era correcto sin revelar la cadena.
4. Adaptacion: ajusta numero de afirmaciones y profundidad de derivacion de la contradiccion.
5. Siguiente trial.

### Reglas
Una contradiccion puede ser directa (P y no-P) o derivada (P->Q, P, no-Q). El par marcado debe ser MINIMO (marcar afirmaciones extra penaliza Jaccard). Conjuntos consistentes existen para evitar sesgo de "siempre hay trampa". Impulsividad si `timeSpentMs<minimumReasoningTimeMs`.

### Inputs
Tap binario (consistente/inconsistente), seleccion multiple (par minimo = `selectedPremiseIds`), chips confianza. Sin texto libre.

### Variables de dificultad
1. `statementCount`: 3-7.
2. `derivationDepth` (pasos para aflorar la contradiccion): 0-2.
3. `consistentRatio` (proporcion de conjuntos consistentes): 0.3-0.5.
4. `distractorPairCount` (pares plausibles pero no contradictorios): 0-3.
5. `connectiveMix` (variedad: solo condicional .. + disyuncion + negacion): 1-3.
6. `negationDensity`: 0-3.
7. `minimalSetSize` (tamano del nucleo contradictorio): 2-3.

### Algoritmo adaptativo
`decide`: `avgIcr<55` o error conceptual repetido -> DECREASE (baja `derivationDepth` y `statementCount`); `avgIcr>=80 && avgJustification>=0.65` -> INCREASE solo con probes isomorfos correctos. Penaliza falsa alarma (marcar inconsistente un conjunto consistente) como `conceptualError`. Baseline ancla `derivationDepth` inicial via `thetaEstimate`.

### Metricas primarias
ICR (`totalIcr`), accuracy del juicio consistente/inconsistente, `premiseScore` (Jaccard del nucleo minimo), discriminacion (d-prime aproximado: hits vs falsas alarmas entre consistentes/inconsistentes).

### Metricas secundarias
Sesgo de respuesta (tendencia a decir "inconsistente"), `confidenceScore` (calibracion), tiempo por afirmacion (descriptivo), tasa de seleccion de nucleo no-minimo.

### Senales de fatiga o mala calidad
Sesgo creciente hacia "inconsistente" sin soporte (falsas alarmas), Jaccard de nucleo ~0 con juicio correcto (adivina el binario), impulsividad cruda >=2, overconfidence en error.

### Scoring sugerido
Misma formula ICR de `DefaultReasoningScorer`. Aqui `correct` = juicio binario correcto; `premiseScore` = Jaccard(`selectedPremiseIds`, nucleo minimo) solo si inconsistente; en consistentes `premiseScore` se omite del peso (renormalizar). `conceptualError` = falsa alarma o nucleo no-minimo con `distractorType`.

### Antitrampa / calidad de datos
Conjuntos consistentes balanceados (`consistentRatio`) bloquean la heuristica "siempre inconsistente". Nucleo minimo exigido impide acertar el binario por suerte. `minimumReasoningTimeMs` filtra adivinacion. Seed determinista.

### Variantes
1. **Directa vs derivada**: `derivationDepth=0` (P, no-P) frente a `derivationDepth=2`.
2. **Disyuncion exclusiva**: introduce XOR para crear inconsistencias mas sutiles.
3. **Triple minimo**: `minimalSetSize=3` (tres afirmaciones que juntas chocan, ninguna par por si sola).

### UX
Pantalla: grid de tarjetas-afirmacion seleccionables, boton binario grande arriba (`MinTouchTarget`), chips de confianza. Feedback funcional: parpadeo del nucleo marcado al revelar acierto/error del par (sin mostrar la derivacion). `verticalScroll`. Duracion: 7-9 items, ~6-7 min.

### Implementacion KMP
Reusa renderer **seleccion multiple** + un tap binario (anadir un `binaryJudgment` al `LogicResponse` o mapearlo a `selectedAnswerId` de 2 opciones consistente/inconsistente). Generador `ConsistencyGenerator : LogicContentProvider` seeded. Modelo: `LogicItem` con `consistencyCorrect: Boolean` y `minimalConflictIds: List<String>` (mapeables a `requiredPremiseIds`). Motores: `ReasoningScorer`, `DifficultyAdapter`. Persistencia summary en namespace `Logic`. `CounterexampleValidator` reutilizable para validar derivaciones internas del generador.

### Riesgos
El d-prime exige n suficiente por categoria; con 7-9 items por sesion la discriminacion sale `confident=false` (correcto reportarla `null`). Conjuntos grandes desbordan memoria de trabajo y miden retencion, no consistencia logica.

### Revision critica
El Revisor ataca: (1) la metrica de discriminacion (hits/falsas alarmas) NO es estable con n bajo por sesion -> debe agregarse sobre la serie de `LogicSession` summaries, no por sesion (Logica no agrega cross-sesion hoy: hueco real). (2) "Nucleo minimo" es ambiguo si hay multiples pares contradictorios -> el generador DEBE garantizar unicidad del nucleo minimo o aceptar cualquier nucleo valido en el scoring. (3) Riesgo de medir memoria de trabajo en vez de logica con `statementCount=7`. Correcciones: cap `statementCount<=5` hasta validar, unicidad de nucleo garantizada por construccion, agregacion de d-prime diferida a una capa de serie historica.

### Veredicto
Aprobado con cambios: mecanica limpia y medible, pero la discriminacion y el nucleo minimo necesitan garantias del generador y agregacion historica antes de afirmar nada.

---

## 3. Necesario vs Suficiente (cuantificador de condiciones)

### Modulo
Principal: MindBoost Logica (`NECESSARY_SUFFICIENT`). Secundarios: Razonamiento condicional, Metacognicion (calibracion de confianza).

### Objetivo cognitivo
Entrenar la distincion entre condicion necesaria, suficiente, ambas o ninguna, dado un enunciado condicional y un escenario. Combate la confusion canonica (tratar "si A entonces B" como bicondicional). Sub-skill de relaciones condicionales.

### Hipotesis de transferencia
SI: clasificacion correcta de necesidad/suficiencia en isomorfos no entrenados (near). NO: razonamiento causal del mundo real, estadistica, ni CI. Copy: "clasificar condiciones contra tu baseline".

### Loop principal
1. Estimulo: un condicional + una pregunta ("¿es A necesaria para B? ¿suficiente?").
2. Decision: seleccionar entre {necesaria y no suficiente, suficiente y no necesaria, ambas (bicondicional), ninguna} + confianza + (opcional) marcar la premisa clave.
3. Feedback: corrige el tipo de error (p.ej. confundir necesaria con suficiente) sin dar la respuesta.
4. Adaptacion: ajusta complejidad del condicional y presencia de contraejemplos.
5. Siguiente trial.

### Reglas
4 opciones mutuamente excluyentes. La respuesta se deriva del condicional, nunca se muestra como affordance (render honesto, aprendizaje 2026-06-18). Distractores codifican la confusion necesaria<->suficiente como `distractorType`. Impulsividad estandar.

### Inputs
Tap (1 de 4 opciones), chips confianza, opcional seleccion de premisa clave.

### Variables de dificultad
1. `connectiveComplexity` (condicional simple .. anidado A->(B->C)): 1-3.
2. `negationInCondition`: 0-2.
3. `biconditionalRatio` (proporcion de items que SON bicondicionales): 0.1-0.4.
4. `contrapositiveRequired` (exige contrapositiva para decidir): bool.
5. `surfaceAbstraction` (cotidiano .. simbolico): 0-1.
6. `distractorSubtlety` (cuan plausible es el distractor necesario/suficiente): 1-3.
7. `quantifierPresence` (anadir "todos/algunos" para acoplar con silogismo): 0-1.

### Algoritmo adaptativo
`decide`: confusion necesaria/suficiente repetida (>=2) -> DECREASE + MICRO_LESSON especifica de la confusion; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`connectiveComplexity`). El `errorTag` del `distractorType` permite micro-leccion dirigida. Baseline ancla `connectiveComplexity`.

### Metricas primarias
ICR, accuracy de clasificacion, tasa de la confusion canonica (necesaria<->suficiente) via `errorTags`, `confidenceScore`.

### Metricas secundarias
Sesgo bicondicional (tendencia a marcar "ambas"), tiempo de razonamiento, `independenceScore` (hints usados), calibracion confianza-acierto.

### Senales de fatiga o mala calidad
Repeticion de la MISMA confusion sin mejora tras micro-leccion (posible plateau/false improvement), impulsividad, overconfidence en error, sesgo "ambas" creciente.

### Scoring sugerido
Formula ICR estandar. `conceptualError=1` cuando el `distractorType` es la confusion necesaria/suficiente (peso 0.15 en penalty). `confidenceScore` clave aqui: metacognicion = parte del entrenamiento (calibrar certeza en una distincion contraintuitiva).

### Antitrampa / calidad de datos
4 opciones excluyentes evitan adivinanza parcial. `biconditionalRatio` balanceado bloquea heuristica fija. La respuesta nunca es affordance visible. `minimumReasoningTimeMs`. Seed determinista; rotacion de `surfaceVariantId`.

### Variantes
1. **Contrapositiva obligatoria**: `contrapositiveRequired=true` (decidir necesidad via contrapositiva).
2. **Bicondicional camuflado**: enunciados que parecen condicional simple pero son "si y solo si".
3. **Acoplado a cuantificador**: "todos los A son B" -> ¿A suficiente para B? (puente hacia silogismo categorico).

### UX
Pantalla limpia: condicional destacado, pregunta, 4 opciones grandes excluyentes, chips confianza. Microanimacion funcional: al fallar, resalta el termino mal clasificado (necesaria vs suficiente) sin decir la respuesta. Duracion: 8-10 items, ~5-6 min (loop corto).

### Implementacion KMP
Reusa renderer **single-tap/choice** (4 opciones). Modelo: `LogicItem` con `conditionType: NecessarySufficientType` (mapeable a `correctAnswerId` de 4). Generador `NecessarySufficientGenerator` seeded. `errorTags` ya soporta `distractorType`->tag. Motores: `ReasoningScorer` (con `confidenceScore` ponderado), `DifficultyAdapter`. Micro-leccion = `AdaptationAction.MICRO_LESSON` ya existente. Persistencia summary namespace `Logic`.

### Riesgos
Es el ejercicio mas dependiente de comprension precisa del enunciado: riesgo de medir lenguaje. El modo simbolico mitiga pero reduce naturalidad. Solo 4 opciones -> 25% azar; con n bajo la accuracy es ruidosa (reportar `confident=false`).

### Revision critica
El Revisor exige: (1) controlar el 25% de azar: ICR process-first ayuda (la confianza y la premisa clave anaden senal), pero la accuracy binaria de clasificacion debe reportarse con su n y `confident=false` bajo umbral. (2) Demostrar que el distractor "necesaria/suficiente" no se distingue por longitud/posicion (sesgo de superficie) -> aleatorizar orden y longitud de opciones. (3) La micro-leccion repetida sin mejora indica plateau no detectado: exigir un contador de re-exposicion a la misma confusion. Correcciones: aleatorizacion de opciones, reporte de n/confident, flag de confusion-persistente.

### Veredicto
Aprobado con cambios: nucleo cognitivo valioso y poco entrenado, pero requiere control de azar/superficie y deteccion de confusion persistente.

---

## 4. Caza-Contraejemplo (refutar una regla con un caso)

### Modulo
Principal: MindBoost Logica (`COUNTEREXAMPLE`). Secundarios: Pensamiento divergente controlado, Razonamiento condicional.

### Objetivo cognitivo
Entrenar la generacion/seleccion del contraejemplo que invalida una regla universal o un condicional: un caso donde las premisas siguen verdaderas pero la conclusion se vuelve falsa. Usa `CounterexampleValidator.invalidates` (premisesRemainTrue && conclusionBecomesFalse).

### Hipotesis de transferencia
SI: refutacion por contraejemplo en isomorfos (near). NO: argumentacion retorica, debate, ni far transfer. Copy: "refutar reglas con casos contra tu baseline".

### Loop principal
1. Estimulo: una regla/condicional presentado como "verdadero" + 4-6 casos candidatos.
2. Decision: seleccionar el/los caso(s) que constituyen contraejemplo valido (seleccion multiple) + confianza.
3. Feedback: valida con `CounterexampleValidator` si el caso mantiene premisas y rompe conclusion; no revela cuales eran.
4. Adaptacion: ajusta cantidad de casos validos (0..k) y sutileza.
5. Siguiente trial.

### Reglas
Un contraejemplo valido EXIGE las dos condiciones (premisas verdaderas + conclusion falsa). Casos que rompen una premisa NO son contraejemplos (distractor `conceptualError`). Existen items SIN contraejemplo (la regla es valida) para evitar sesgo. Impulsividad estandar.

### Inputs
Seleccion multiple (casos = `selectedAnswerId`/multi), tap binario opcional "no hay contraejemplo", chips confianza.

### Variables de dificultad
1. `caseCount`: 4-8.
2. `validCounterexampleCount` (0..k): 0-3.
3. `nearMissCount` (casos que rompen una premisa, no la conclusion): 1-4.
4. `ruleQuantifier` ("todos" vs "si..entonces" vs "ningun"): 1-3.
5. `caseDimensionality` (atributos por caso): 2-4.
6. `noCounterexampleRatio`: 0.2-0.4.
7. `negationInRule`: 0-2.

### Algoritmo adaptativo
`decide`: marcar near-miss como contraejemplo repetidamente -> DECREASE + MICRO_LESSON ("rompiste una premisa, no la conclusion"); `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`caseDimensionality`, `nearMissCount`). Falsa alarma "hay contraejemplo" cuando no lo hay = `conceptualError`. Baseline ancla `caseCount`/`nearMissCount`.

### Metricas primarias
ICR, accuracy de seleccion de contraejemplo, `premiseScore`-analogo (Jaccard de casos validos via `CounterexampleValidator`), tasa near-miss (confundir romper-premisa con romper-conclusion).

### Metricas secundarias
Falsas alarmas en items sin contraejemplo, `confidenceScore`, tiempo, sensibilidad por `ruleQuantifier`.

### Senales de fatiga o mala calidad
Patron de marcar todos los casos (gaming Jaccard), near-miss repetido tras micro-leccion, impulsividad, overconfidence.

### Scoring sugerido
Formula ICR. `correct` = conjunto de contraejemplos exactamente correcto (o juicio "no hay" correcto). `premiseScore` = Jaccard(`selected`, casos validados por `CounterexampleValidator`). `conceptualError` = near-miss seleccionado (`distractorType=BROKE_PREMISE`).

### Antitrampa / calidad de datos
`CounterexampleValidator.invalidates` es la autoridad: ningun caso cuenta sin cumplir AMBAS condiciones. Penalizar marcar casos extra (near-miss) impide el "marca todo". Items sin contraejemplo balanceados. Seed determinista.

### Variantes
1. **Universal afirmativa**: refutar "todos los A son B" con un A-no-B.
2. **Condicional**: refutar "si A entonces B" con A-y-no-B manteniendo el resto.
3. **Sin contraejemplo**: la regla es valida; el acierto es decir "no hay" (controla sesgo de refutacion).

### UX
Pantalla: regla destacada arriba (cue persistente), grid de tarjetas-caso con atributos visibles, boton "no hay contraejemplo". Microanimacion funcional: al fallar near-miss, resalta la premisa que el caso rompio (mostrando por que NO es contraejemplo). `verticalScroll`. Duracion: 6-8 items, ~6 min.

### Implementacion KMP
Reusa **seleccion multiple**. Modelo: `LogicItem` con `answerOptions` = casos (cada `AnswerOption` con `counterexampleCase` ya existente en el modelo) + `CounterexampleValidator` para etiquetar validez en el generador. Generador `CounterexampleGenerator` seeded que construye near-misses por diseno. Motores: `ReasoningScorer`, `DifficultyAdapter`, `CounterexampleValidator` (validacion). Persistencia summary namespace `Logic`. `counterexampleCase` del `AnswerOption` ya soporta esto.

### Riesgos
Construir near-misses validos automaticamente es no trivial: un generador debil produce contraejemplos ambiguos. La dimensionalidad alta de casos puede medir comparacion visual mas que logica.

### Revision critica
El Revisor destruye: (1) la VALIDEZ del near-miss depende enteramente del generador; sin generador procedural robusto, con pack autorado fijo se memoriza cual es el caso. EXIGE generador con `CounterexampleValidator` aplicado en construccion + tests de unicidad. (2) "Marcar todos los casos" debe penalizarse explicitamente o el Jaccard premia cobertura -> confirmar penalty por casos invalidos seleccionados. (3) `caseDimensionality=4` con 8 casos = carga perceptiva, no logica: cap dimensionalidad. Correcciones: generador validado por `CounterexampleValidator`, penalty por near-miss seleccionado, cap perceptivo.

### Veredicto
Aprobado con cambios: ejercicio de altisimo valor logico (genera, no reconoce), pero totalmente dependiente de un generador robusto validado por `CounterexampleValidator`.

---

## 5. Silogismo Categorico con Diagrama Mental (todos/algunos/ningun)

### Modulo
Principal: MindBoost Logica (deduccion categorica, nuevo sub-skill). Secundarios: Razonamiento espacial (relaciones de conjunto), Memoria de trabajo.

### Objetivo cognitivo
Entrenar validez de silogismos categoricos (Barbara, Celarent, etc.) con cuantificadores "todos/algunos/ningun", distinguiendo validez de plausibilidad de contenido (combate el sesgo de creencia: una conclusion verdadera-en-el-mundo pero NO valida).

### Hipotesis de transferencia
SI: juicio de validez categorica en isomorfos con contenido distinto (near, clave para aislar forma de contenido). NO: razonamiento general ni CI. Copy: "juzgar validez formal contra tu baseline".

### Loop principal
1. Estimulo: 2 premisas categoricas + 1 conclusion candidata.
2. Decision: "¿la conclusion se sigue VALIDAMENTE?" (valido/invalido) + confianza + (opcional) seleccionar la figura/regla.
3. Feedback: separa validez de verdad-de-contenido (clave anti-sesgo-de-creencia).
4. Adaptacion: ajusta figura del silogismo y congruencia creencia-validez.
5. Siguiente trial.

### Reglas
Validez = la conclusion se sigue por forma, independiente de si es verdad en el mundo. Items donde creencia y validez DISOCIAN (conclusion creible pero invalida, o increible pero valida) son el nucleo. Impulsividad estandar.

### Inputs
Tap binario (valido/invalido), chips confianza, opcional tap de figura.

### Variables de dificultad
1. `figure` (1-4, figura del silogismo): 1-4.
2. `beliefValidityCongruence` (-1 increible-valido, 0 neutro, +1 creible-valido): -1..1.
3. `quantifierMix` (combinaciones todos/algunos/ningun): 1-4.
4. `particularPremiseCount` ("algunos", mas dificiles): 0-2.
5. `negativePremiseCount` ("ningun/no"): 0-2.
6. `contentAbstraction` (concreto familiar .. abstracto/sin-sentido): 0-2.
7. `invalidRatio`: 0.4-0.6.

### Algoritmo adaptativo
`decide`: errores sistematicos en items belief-incongruentes (creer que valido porque es creible) -> DECREASE + MICRO_LESSON anti-sesgo-de-creencia; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`figure` mas dificil, `particularPremiseCount`). Mide especificamente el delta de accuracy entre items congruentes vs incongruentes (efecto de sesgo de creencia). Baseline ancla `figure`.

### Metricas primarias
ICR, accuracy de validez, **belief-bias index** = accuracy(congruente) - accuracy(incongruente) (descriptivo, reportado con n/`confident`), `confidenceScore`.

### Metricas secundarias
Accuracy por `figure`, tasa de error en premisas particulares ("algunos"), tiempo, calibracion.

### Senales de fatiga o mala calidad
belief-bias index creciente intra-sesion (cae el control de contenido con fatiga), impulsividad, overconfidence, accuracy=invalidRatio (responde fijo "invalido").

### Scoring sugerido
Formula ICR. `correct` = juicio de validez correcto. `conceptualError=1` cuando el error es belief-congruente (cayo en el sesgo, `distractorType=BELIEF_BIAS`). El belief-bias index NO entra en el score del trial (es metrica de serie), solo el ICR del juicio.

### Antitrampa / calidad de datos
Contenido abstracto/sin-sentido (`contentAbstraction=2`) elimina el atajo de creencia. `invalidRatio` balanceado bloquea respuesta fija. Items incongruentes detectan sesgo real. Seed determinista, rotacion de contenido (`surfaceVariantId`).

### Variantes
1. **Abstracto puro**: terminos sin significado (X, Y, Z) -> validez pura sin creencia.
2. **Disociacion maxima**: conclusiones increibles-pero-validas + creibles-pero-invalidas.
3. **Cuatro figuras**: rota `figure` para cubrir formas validas/invalidas canonicas.

### UX
Pantalla: 2 premisas + conclusion en tarjetas, boton valido/invalido grande, chips confianza. Microanimacion funcional: al fallar por sesgo de creencia, muestra brevemente un item abstracto isomorfo (mismo `logicalStructureId`) para evidenciar que la FORMA es identica. Duracion: 8-10 items, ~5-6 min.

### Implementacion KMP
Reusa **single-tap/choice** binario. Modelo: `LogicItem` con `syllogismFigure`, `isValid`, `beliefCongruence` (mapeables; `correctAnswerId` de 2). Generador `CategoricalSyllogismGenerator` seeded que controla `beliefValidityCongruence`. Motores: `ReasoningScorer`, `DifficultyAdapter`. El isomorfo abstracto usa `transferConsistencyFor` (mismo `logicalStructureId`). Persistencia summary namespace `Logic`. belief-bias index = metrica agregada (requiere capa de serie, hueco reconocido).

### Riesgos
El belief-bias index necesita pares congruente/incongruente balanceados por sesion para ser interpretable; con n bajo sale `null`. El contenido familiar puede variar por cultura (mitigar con abstracto). Sesgo de respuesta "invalido" si `invalidRatio` no se balancea.

### Revision critica
El Revisor ataca: (1) el belief-bias index es la joya pero es inestable por sesion -> debe agregarse sobre summaries historicos, no afirmarse por sesion (Logica no agrega cross-sesion: hueco). (2) La validez categorica es culturalmente neutral SOLO en modo abstracto; el modo concreto introduce contenido potencialmente sesgado -> exigir paridad de contenido entre congruente/incongruente. (3) Binario = 50% azar; ICR process-first lo compensa parcialmente pero la accuracy debe reportar n/`confident`. Correcciones: agregacion historica del index, paridad de contenido controlada por el generador, reporte de n.

### Veredicto
Aprobado con cambios: aisla forma de contenido (raro y valioso), pero el belief-bias index solo es honesto agregado historicamente y con contenido balanceado.

---

## 6. Inferencia Bayesiana Cualitativa (actualizar creencia con evidencia)

### Modulo
Principal: MindBoost Logica (razonamiento probabilistico cualitativo, nuevo sub-skill). Secundarios: Metacognicion/calibracion, Toma de decisiones.

### Objetivo cognitivo
Entrenar la DIRECCION y MAGNITUD cualitativa de la actualizacion de creencia ante evidencia (sin aritmetica): dada una hipotesis, un prior y evidencia diagnostica, ¿sube, baja o no cambia la probabilidad, y cuanto relativamente? Combate base-rate neglect y confusion P(E|H) vs P(H|E).

### Hipotesis de transferencia
SI: actualizacion cualitativa correcta en isomorfos (near). NO: estadistica formal, calculo bayesiano numerico, ni CI. Copy: "actualizar creencias con evidencia contra tu baseline". Cero claims de "racionalidad" global.

### Loop principal
1. Estimulo: hipotesis + prior (alto/medio/bajo, mostrado como banda) + una pieza de evidencia con su diagnosticidad cualitativa.
2. Decision: (a) direccion (sube/baja/igual) y (b) magnitud relativa (mucho/poco) en un selector ordinal + confianza.
3. Feedback: corrige direccion/magnitud sin numeros; senala si ignoro el prior (base-rate neglect).
4. Adaptacion: ajusta diagnosticidad y conflicto prior-evidencia.
5. Siguiente trial.

### Reglas
La respuesta correcta es la direccion/magnitud CUALITATIVA coherente con Bayes (no un numero). Evidencia poco diagnostica -> cambio pequeno aunque sea llamativa. Prior fuerte modera el cambio. Distractores codifican base-rate neglect y confusion de condicional invertido (`distractorType`). Impulsividad estandar.

### Inputs
Selector ordinal (sube-mucho/sube-poco/igual/baja-poco/baja-mucho, 5 niveles = tap), chips confianza. Sin texto libre.

### Variables de dificultad
1. `diagnosticityLevel` (cuan informativa es la evidencia): 1-3.
2. `priorStrength` (extremo .. medio): 1-3.
3. `priorEvidenceConflict` (evidencia apunta contra el prior): 0-1.
4. `evidenceSalience` (llamativa pero poco diagnostica = trampa): 1-3.
5. `magnitudeGranularity` (3 vs 5 niveles ordinales): 3 o 5.
6. `multiEvidence` (1 vs 2 piezas a integrar): 1-2.
7. `baseRateExtremity` (prior muy raro: trampa de neglect): 0-2.

### Algoritmo adaptativo
`decide`: base-rate neglect repetido (ignora prior extremo) -> DECREASE + MICRO_LESSON de prior; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`multiEvidence`, `priorEvidenceConflict`). Detecta especificamente confusion direccion-correcta-magnitud-incorrecta vs direccion-incorrecta (errores cualitativamente distintos). Baseline ancla `diagnosticityLevel`.

### Metricas primarias
ICR, accuracy de direccion, error ordinal de magnitud (distancia en niveles, descriptivo), tasa de base-rate neglect via `errorTags`.

### Metricas secundarias
Confusion P(E|H)/P(H|E) (condicional invertido), `confidenceScore` (calibracion clave), tiempo, sensibilidad a `evidenceSalience`.

### Senales de fatiga o mala calidad
Respuesta fija "sube-poco" (modo conservador sin razonar), base-rate neglect creciente, overconfidence en error, impulsividad.

### Scoring sugerido
Formula ICR. `correct` = direccion correcta. Anadir un termino dentro de `justificationScore`-analogo que premie magnitud cualitativa correcta (error ordinal 0 = 1.0, distancia 1 = 0.5, >=2 = 0). `conceptualError` = base-rate neglect o condicional invertido (`distractorType`). La magnitud se pondera DENTRO del proceso, no anade velocidad.

### Antitrampa / calidad de datos
Evidencia saliente-pero-poco-diagnostica (`evidenceSalience` alto con `diagnosticityLevel` bajo) detecta el atajo de saliencia. Prior extremo detecta neglect. Selector ordinal (no binario) reduce azar. Seed determinista. SIN numeros para evitar que se convierta en aritmetica memorizada.

### Variantes
1. **Conflicto prior-evidencia**: prior fuerte + evidencia contraria moderada -> el cambio debe ser pequeno.
2. **Trampa de saliencia**: evidencia dramatica pero poco diagnostica -> cambio minimo.
3. **Doble evidencia**: integrar 2 piezas (una a favor, una en contra) -> direccion neta.

### UX
Pantalla: banda de prior visual (no numerica), tarjeta de evidencia, selector ordinal horizontal de 3/5 niveles (`MinTouchTarget`), chips confianza. Microanimacion funcional: la banda de creencia se desplaza al dar feedback mostrando la actualizacion correcta (direccion+magnitud), sin numeros. Duracion: 8-10 items, ~6 min.

### Implementacion KMP
Reusa **single-tap/choice** (5 opciones ordinales) — NO requiere renderer nuevo si se modela como 5 `answerOptions`; opcionalmente un renderer ordinal-slider nuevo mejora UX. Modelo: `LogicItem` con `priorBand`, `diagnosticity`, `correctDirection`, `correctMagnitude` (mapeable a `correctAnswerId` de 5). Generador `QualitativeBayesGenerator` seeded. Motores: `ReasoningScorer` (con peso de magnitud en justificacion), `DifficultyAdapter`. Persistencia summary namespace `Logic`. ATENCION: mantener cualitativo cumple frontera de seguridad (no es test de CI/estadistica clinica).

### Riesgos
Riesgo de pseudociencia si se presenta como "test de racionalidad" (PROHIBIDO): el copy debe ser estrictamente "practica de actualizacion cualitativa". La magnitud cualitativa ("mucho/poco") es inherentemente algo subjetiva: el generador DEBE construir items donde la magnitud correcta sea inequivoca por construccion.

### Revision critica
El Revisor destruye: (1) "magnitud cualitativa" es resbaladiza -> ¿quien decide que "sube mucho"? EXIGE que el generador derive la magnitud de una escala bayesiana interna determinista con margenes amplios (zona muerta entre niveles) para que la respuesta sea inequivoca; si no, la metrica mide la heuristica del autor, no la del usuario. (2) Roza el claim de "racionalidad/debiasing" -> debe pasar `review-cognitive-claims`; prohibido afirmar reduccion de sesgos en la vida real (far transfer). (3) Sin numeros pero con base-rates, riesgo de medir comprension verbal de probabilidad -> incluir modo con bandas puramente visuales. Correcciones: magnitud por construccion con zona muerta, copy auditado anti-claims, modo visual.

### Veredicto
Aprobado con cambios: entrena un nucleo de razonamiento real y poco cubierto, pero exige construccion inequivoca de magnitud y auditoria estricta de claims (alto riesgo de pseudociencia si se descuida).

---

## 7. Transferencia Espejo (gemelo entrenado vs isomorfo)

### Modulo
Principal: MindBoost Logica (consolidacion + medicion de near transfer). Secundarios: TODOS los sub-skills (es un meta-ejercicio de transferencia).

### Objetivo cognitivo
No entrena un paradigma nuevo: entrena y MIDE explicitamente la transferencia isomorfa. Presenta un item entrenado (`TRAINED`) y, mas tarde, su gemelo isomorfo (`T1_ISOMORPHIC`/`T2_NEAR`) con `surfaceForm` distinta pero mismo `logicalStructureId`, para verificar si el usuario abstrae la estructura o memoriza la superficie.

### Hipotesis de transferencia
ESTE ejercicio ES la medicion de transferencia. SI mide: near transfer estructural (1.0 solo si gemelo entrenado correcto Y isomorfo correcto, aprendizaje 2026-06-15). NO afirma: far transfer ni transferencia a dominios distintos. Copy: "aplicar la misma estructura en formas nuevas contra tu baseline".

### Loop principal
1. Estimulo: fase A entrena un item (cualquier paradigma E1-E6 anteriores). Fase B (mas tarde en la sesion, interleaved) presenta el isomorfo no entrenado.
2. Decision: resolver el isomorfo (paradigma heredado del gemelo).
3. Feedback: tras el isomorfo, indica si transfirio la estructura (sin revelar solucion).
4. Adaptacion: ajusta distancia superficie-estructura entre gemelo e isomorfo.
5. Siguiente par.

### Reglas
El isomorfo comparte `logicalStructureId` y difiere en `surfaceVariantId`. `isIsomorphicProbe=true`. `transferConsistencyFor` busca `trainedTwin` (ultimo attempt con mismo `logicalStructureId`) y da 1.0 SOLO si ambos correctos. Interleaving Bjork: gemelo e isomorfo separados por >=2 items intermedios (desirable difficulty). Impulsividad estandar.

### Inputs
Hereda del paradigma del gemelo (tap/seleccion multiple/binario). Chips confianza en ambas fases.

### Variables de dificultad
1. `surfaceDistance` (cuan distinta es la forma: rename .. dominio-distinto): 1-4.
2. `transferLevel` (`TRAINED`/`T1_ISOMORPHIC`/`T2_NEAR`): nivel.
3. `interleaveGap` (items entre gemelo e isomorfo): 2-6.
4. `structuralComplexity` (heredada del paradigma base): 1-5.
5. `decoyIsomorphCount` (isomorfos falsos: misma superficie, estructura distinta): 0-2.
6. `delayWithinSession` (intra-sesion vs final): 0-1.

### Algoritmo adaptativo
`decide`: el `highPerformanceDecision` de `DefaultLogicDifficultyAdapter` YA condiciona INCREASE a "probes isomorfos todos correctos con ICR>=70" — este ejercicio es su insumo directo. Transferencia fallida (gemelo OK, isomorfo mal) -> HOLD + MICRO_LESSON de abstraccion estructural; transferencia consistente -> INCREASE `surfaceDistance`. Baseline: `thetaEstimate` por estructura.

### Metricas primarias
ICR del isomorfo, **transfer consistency** (1.0/0.0 via `transferConsistencyFor`), accuracy del isomorfo condicionada a gemelo-correcto.

### Metricas secundarias
Delta ICR gemelo->isomorfo (caida = dependencia de superficie), `decoyIsomorph` falsas alarmas (transferir donde NO aplica = sobre-generalizacion), `confidenceScore`.

### Senales de fatiga o mala calidad
Transferencia que cae con `interleaveGap` alto (memoria, no abstraccion), sobre-generalizacion a decoys, impulsividad, overconfidence en isomorfo fallido.

### Scoring sugerido
`transferScore` = `transferConsistencyFor` (1.0 sii gemelo entrenado correcto && isomorfo correcto). Entra como `transferConsistencyScore` en `ReasoningScorer.score(item, response, transferConsistencyScore)` — el parametro YA EXISTE en la firma. ICR del isomorfo via formula estandar; el transfer score modula la senal de progresion (no el ICR del trial individual).

### Antitrampa / calidad de datos
Regla de 2026-06-15: transfer=1.0 exige AMBOS lados correctos (no premia acertar el isomorfo por suerte si el gemelo fallo, ni viceversa). Decoys isomorficos detectan sobre-generalizacion. `surfaceVariantId` distinto garantiza que no es el mismo item. Interleaving evita resolver por memoria reciente. Seed determinista.

### Variantes
1. **Rename minimo**: misma estructura, solo cambian etiquetas (P->A) — `surfaceDistance=1`.
2. **Cambio de dominio**: condicional sobre numeros -> condicional sobre reglas sociales neutras, misma forma — `surfaceDistance=3`.
3. **Decoy estructural**: isomorfo aparente (misma superficie) con estructura logica DISTINTA — castiga aplicar la regla a ciegas.

### UX
Pantalla: hereda el renderer del paradigma base; un indicador sutil (sin spoilear) marca la fase. Microanimacion funcional: tras el isomorfo, un overlay muestra el gemelo entrenado lado a lado destacando la estructura compartida (refuerza abstraccion). Duracion: 8-12 items (4-6 pares), ~7 min.

### Implementacion KMP
Reusa renderers de los paradigmas base (E1-E6) — NO renderer nuevo. Usa DIRECTAMENTE infraestructura existente: `LogicStateHolder.transferConsistencyFor`, `isIsomorphicProbe`, `logicalStructureId`/`surfaceVariantId`, `TransferLevel`, y el parametro `transferConsistencyScore` de `ReasoningScorer.score`. Motores: `ReasoningScorer` (consume transfer score), `DefaultLogicDifficultyAdapter.highPerformanceDecision` (gating de INCREASE por isomorfos). Generador `IsomorphPairGenerator` que produce gemelo+isomorfo con mismo `logicalStructureId`. Persistencia summary namespace `Logic` (registra `transferLevel` por attempt).

### Riesgos
Depende de un banco de pares isomorfos correctamente construidos (sin generador procedural, el pack autorado tiene 1 solo probe sobre `conditional_refutation_p_and_not_q`: insuficiente). Interleaving intra-sesion con pocos items deja `interleaveGap` corto.

### Revision critica
El Revisor ataca: (1) con UN solo `logicalStructureId` probe en el pack autorado actual, este ejercicio NO es viable: EXIGE un banco de >=8 estructuras isomorficas con >=2 variantes de superficie cada una. Bloqueante. (2) La regla AMBOS-correctos es correcta pero produce transfer=0.0 muy frecuente al inicio -> el progreso parecera estancado; debe comunicarse como "calibrando transferencia" (`null`/em-dash) hasta tener n suficiente de pares, no como fallo. (3) Decoys estructurales mal construidos confunden en vez de discriminar -> validacion por `CounterexampleValidator`/tests. Correcciones: banco minimo de estructuras, comunicacion honesta de transfer calibrando, decoys validados.

### Veredicto
Aprobado con cambios: es el ejercicio que mejor cumple el principio near-transfer del repo y reusa infra existente, pero esta BLOQUEADO hasta tener un banco real de pares isomorfos (>=8 estructuras).

---

## 8. Ordenar la Derivacion (reconstruir una prueba paso a paso)

### Modulo
Principal: MindBoost Logica (estructura de prueba, nuevo sub-skill). Secundarios: Memoria de trabajo, Planificacion secuencial.

### Objetivo cognitivo
Entrenar la reconstruccion del ORDEN logico de una derivacion: dadas premisas y pasos de inferencia desordenados, ordenarlos para que cada paso se justifique solo por pasos anteriores hasta llegar a la conclusion. Entrena dependencia logica y no-circularidad.

### Hipotesis de transferencia
SI: ordenar derivaciones isomorfas (near). NO: demostracion matematica formal, programacion, ni far transfer. Copy: "reconstruir el orden de una derivacion contra tu baseline".

### Loop principal
1. Estimulo: premisas + 3-7 pasos de inferencia desordenados (cada uno con su regla).
2. Decision: ordenar los pasos (drag-and-drop) de modo que cada uno dependa solo de anteriores.
3. Feedback: indica cuantos pasos estan en posicion logicamente valida (Kendall-tau de dependencia), sin dar el orden.
4. Adaptacion: ajusta numero de pasos y ramificacion de dependencias.
5. Siguiente trial.

### Reglas
Un orden es valido si cada paso solo usa premisas o pasos previos (no circular). Puede haber MULTIPLES ordenes validos (cualquier orden topologico del DAG de dependencias cuenta). Pasos "distractores" (no necesarios para la conclusion) deben excluirse o quedar al final. Impulsividad estandar.

### Inputs
Drag-and-drop (reordenar pasos), opcional descartar pasos irrelevantes (swipe out), chips confianza. **Requiere renderer nuevo de ordenamiento/drag.**

### Variables de dificultad
1. `stepCount`: 3-7.
2. `dependencyBranching` (cuantas dependencias por paso): 1-3.
3. `distractorStepCount` (pasos no necesarios): 0-3.
4. `validOrderingsCount` (1 = orden unico, >1 = varios topologicos): 1-k.
5. `ruleDiversity` (reglas distintas entre pasos): 1-4.
6. `circularTrapPresence` (un orden tentador pero circular): 0-1.
7. `premiseCount`: 2-5.

### Algoritmo adaptativo
`decide`: ordenes circulares repetidos -> DECREASE + MICRO_LESSON de dependencia; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`stepCount`, `dependencyBranching`). Penaliza incluir distractores en la cadena (`conceptualError`). Baseline ancla `stepCount`.

### Metricas primarias
ICR, **ordering correctness** = fraccion de pares en orden de dependencia valido (Kendall-tau sobre el DAG, espejando `Ordering` de Lenguaje), accuracy de exclusion de distractores.

### Metricas secundarias
Errores de circularidad (paso usado antes de su justificacion), `confidenceScore`, tiempo, distractores incluidos.

### Senales de fatiga o mala calidad
Reordenamientos aleatorios (Kendall-tau ~azar con muchos movimientos), inclusion repetida de distractores, impulsividad (soltar rapido sin razonar), overconfidence.

### Scoring sugerido
Formula ICR. `correct` = orden topologicamente valido (cualquiera del DAG). `justificationScore`-analogo = Kendall-tau de dependencia normalizada (fraccion de pares correctamente ordenados). `conceptualError` = circularidad o distractor incluido. Reusa la matematica Kendall-tau ya presente en `ClosedScoringEngine.Ordering` de Lenguaje (portada a Logica).

### Antitrampa / calidad de datos
Aceptar CUALQUIER orden topologico valido (no un orden canonico unico) evita penalizar ordenes correctos alternativos — critico para validez. Distractores detectan inclusion espuria. `circularTrapPresence` detecta razonamiento circular. Seed determinista; el DAG de dependencias es la autoridad.

### Variantes
1. **Orden unico**: DAG lineal (`validOrderingsCount=1`) — mas facil de puntuar.
2. **Multiples topologicos**: DAG ramificado, varios ordenes validos.
3. **Con distractores**: pasos plausibles pero innecesarios que deben excluirse.

### UX
Pantalla: lista vertical de tarjetas-paso arrastrables con `verticalScroll`/reorderable, zona de "descartados" abajo. Microanimacion funcional: al soltar, una linea conecta cada paso con sus dependencias satisfechas (verde) o violadas (rojo, no solo color — tambien icono, accesibilidad). Duracion: 5-7 items, ~7 min (loop algo mas largo por el drag).

### Implementacion KMP
**Requiere renderer nuevo de drag/ordering** (el unico de los 10 que no reusa los existentes). Modelo: `LogicItem` con `steps: List<DerivationStep>`, `dependencyDag: Map<StepId, Set<StepId>>`, `distractorStepIds`. `LogicResponse` gana `orderedStepIds: List<String>`. Generador `DerivationOrderingGenerator` seeded. Scoring: portar Kendall-tau de `ClosedScoringEngine.Ordering` a un `OrderingValidator` en Logica. Motores: `ReasoningScorer` (con justification=Kendall-tau), `DifficultyAdapter`. Persistencia summary namespace `Logic`.

### Riesgos
El drag-and-drop es la interaccion mas pesada (carga motora, no logica) — riesgo de medir destreza tactil. El renderer nuevo es coste de implementacion alto. Multiples ordenes validos complican el feedback claro.

### Revision critica
El Revisor destruye: (1) drag-and-drop introduce ruido motor que contamina RT e ICR -> el `minimumReasoningTimeMs` no aplica igual; EXIGE separar tiempo-de-razonamiento de tiempo-de-manipulacion (registrar timestamp de primera-decision-firme, no del ultimo drag). (2) Kendall-tau sobre multiples ordenes validos: ¿contra cual orden se compara? DEBE compararse contra la validez topologica del DAG, no contra un orden de referencia (si no, penaliza ordenes correctos). Confirmar `OrderingValidator` valida el DAG, no un permutacion fija. (3) Renderer nuevo = riesgo de que "implemented" se exponga sin renderer (aprendizaje 2026-06-18): bloquear hasta renderer + captura. Correcciones: separar tiempo motor/cognitivo, validacion por DAG no por permutacion, gate de renderer.

### Veredicto
Aprobado con cambios: entrena estructura de prueba (unico entre los 10), pero el coste del renderer drag y el ruido motor exigen separar tiempo cognitivo de manipulacion y validar por DAG.

---

## 9. Inferencia Suprimida (cuando una premisa extra cambia la conclusion)

### Modulo
Principal: MindBoost Logica (razonamiento defeasible vs deductivo monotono, nuevo sub-skill). Secundarios: Control inhibitorio, Flexibilidad cognitiva.

### Objetivo cognitivo
Entrenar el reconocimiento de cuando una inferencia condicional valida (modus ponens) deja de ser concluyente al anadir una premisa adicional que introduce una excepcion/condicion habilitadora — distinguiendo deduccion estricta (no se suprime) de inferencia plausible (se suprime). Nucleo del "suppression task" de Byrne.

### Hipotesis de transferencia
SI: reconocer supresion en isomorfos (near). NO: razonamiento causal del mundo real, "sentido comun" generico, ni far transfer. Copy: "reconocer cuando una conclusion deja de seguirse contra tu baseline".

### Loop principal
1. Estimulo: condicional + hecho (modus ponens valido) -> luego se anade una premisa adicional (condicion alternativa/habilitadora).
2. Decision: tras la premisa extra, ¿la conclusion AUN se sigue necesariamente? (se-sigue / ya-no-se-sigue / depende) + confianza.
3. Feedback: distingue deduccion estricta (no se suprime nunca) de inferencia con condicion habilitadora (se suprime).
4. Adaptacion: ajusta tipo de premisa adicional (alternativa vs habilitadora) y validez estricta.
5. Siguiente trial.

### Reglas
En modus ponens estricto, NINGUNA premisa adicional suprime la conclusion (monotonia). Con condicion habilitadora ausente, la inferencia plausible SE suprime. El error canonico es suprimir una deduccion valida (o no suprimir una habilitadora). Items control (deduccion pura) detectan supresion espuria. Impulsividad estandar.

### Inputs
Tap (3 opciones: se-sigue/ya-no/depende), chips confianza, opcional marcar la premisa que cambia el juicio.

### Variables de dificultad
1. `additionalPremiseType` (ninguna/alternativa/habilitadora): 0-2.
2. `strictnessLevel` (deduccion estricta vs plausible): 1-2.
3. `premiseChainLength`: 1-3.
4. `suppressionSubtlety` (cuan obvia es la condicion habilitadora): 1-3.
5. `controlItemRatio` (deducciones puras que NO deben suprimirse): 0.3-0.5.
6. `surfaceAbstraction`: 0-1.
7. `negationPresence`: 0-2.

### Algoritmo adaptativo
`decide`: suprimir deducciones validas repetidamente (error de monotonia) -> DECREASE + MICRO_LESSON "una deduccion valida no se suprime"; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`suppressionSubtlety`). Mide el delta entre items estrictos (no-suprimir) y habilitadores (suprimir): un buen razonador discrimina. Baseline ancla `strictnessLevel`.

### Metricas primarias
ICR, accuracy de juicio de supresion, **suppression discrimination** = accuracy(habilitadora) y accuracy(control-estricto) por separado (descriptivo, n/`confident`), `confidenceScore`.

### Metricas secundarias
Tasa de supresion espuria (suprimir deduccion valida), `errorTags` de monotonia, tiempo, sensibilidad a `additionalPremiseType`.

### Senales de fatiga o mala calidad
Respuesta fija "depende" (modo evasivo), supresion espuria creciente, impulsividad, overconfidence.

### Scoring sugerido
Formula ICR. `correct` = juicio de supresion correcto (3-way). `conceptualError=1` cuando suprime una deduccion estricta valida o no suprime una habilitadora (`distractorType=MONOTONICITY_ERROR`/`ENABLING_MISS`). El `confidenceScore` es relevante (la supresion es contraintuitiva, calibrar certeza importa).

### Antitrampa / calidad de datos
Items control (deduccion pura, no-suprimir) balanceados (`controlItemRatio`) bloquean la heuristica "anadir premisa siempre cambia algo". 3 opciones (no binario) reducen azar. La premisa adicional se presenta DESPUES (secuencia controlada). Seed determinista.

### Variantes
1. **Alternativa**: premisa que ofrece una via alternativa a la conclusion (no suprime modus ponens, puede afectar modus tollens).
2. **Habilitadora ausente**: premisa que revela una condicion necesaria no garantizada -> suprime la inferencia plausible.
3. **Control estricto**: deduccion pura donde NINGUNA premisa adicional debe suprimir (detecta supresion espuria).

### UX
Pantalla: secuencia animada funcional — primero condicional+hecho, luego "aparece" la premisa adicional (microanimacion que marca el cambio de informacion), 3 botones de juicio. Feedback: distingue visualmente deduccion estricta (candado, no se mueve) de inferencia plausible (se debilita). Duracion: 8-10 items, ~6 min.

### Implementacion KMP
Reusa **single-tap/choice** (3 opciones) + una micro-secuencia de revelado (LaunchedEffect que introduce la premisa extra, como las animaciones funcionales de focus E10/E8 — patron 2026-06-18). Modelo: `LogicItem` con `additionalPremiseType`, `suppressionCorrect: SuppressionVerdict`, `isStrictDeduction`. Generador `SuppressionTaskGenerator` seeded. Motores: `ReasoningScorer`, `DifficultyAdapter`. Persistencia summary namespace `Logic`. El revelado secuencial se hace en el holder/renderer, no en dominio.

### Riesgos
El "suppression task" depende fuertemente de pragmatica/contenido (Byrne usaba contenido cotidiano): el modo abstracto puede destruir el efecto (la supresion vive en el conocimiento de condiciones habilitadoras). Tension real: abstraer para neutralidad cultural vs preservar el efecto.

### Revision critica
El Revisor ataca duro: (1) la supresion DEPENDE de conocimiento del mundo (condiciones habilitadoras) — si se abstrae a P/Q puro, el fenomeno DESAPARECE; si se deja concreto, mide conocimiento cultural, no logica. Tension no resuelta: EXIGE items donde la condicion habilitadora se DECLARE explicitamente en el enunciado (no se asuma del mundo), convirtiendolo en logica defeasible explicita y no en sentido comun implicito. (2) 3-way "depende" es un imán de respuestas evasivas -> penalizar o exigir justificacion del "depende". (3) Discriminacion supresion necesita pares balanceados por sesion -> agregacion historica. Correcciones: condiciones habilitadoras EXPLICITAS en el item (no implicitas del mundo), control del "depende", agregacion.

### Veredicto
Aprobado con cambios: fenomeno logico profundo y unico, pero solo es honesto si las condiciones habilitadoras son EXPLICITAS en el enunciado (no conocimiento del mundo), de lo contrario mide cultura y se descarta.

---

## 10. Puente de Analogia Estructural (mapear relaciones, no rasgos)

### Modulo
Principal: MindBoost Logica (razonamiento analogico relacional, nuevo sub-skill). Secundarios: Razonamiento abstracto, Inhibicion de similitud superficial.

### Objetivo cognitivo
Entrenar el mapeo de la ESTRUCTURA relacional entre dos dominios (A:B :: C:?), eligiendo el termino que preserva la RELACION, no el que comparte rasgos superficiales con C. Combate el sesgo de similitud superficial (analogia atributiva vs relacional).

### Hipotesis de transferencia
SI: completar analogias relacionales isomorfas (near). NO: "creatividad" general, CI verbal, ni far transfer. Copy: "mapear relaciones estructurales contra tu baseline". (Analogias relacionales abstractas, no verbales-culturales tipo test de vocabulario.)

### Loop principal
1. Estimulo: par fuente A:B (con relacion explicita: p.ej. "contiene", "es-opuesto-de", "causa") + termino C + 4 opciones para D.
2. Decision: elegir D que replica la RELACION A:B en C:D + confianza + (opcional) nombrar la relacion.
3. Feedback: si fallo por similitud superficial (eligio rasgo de C, no relacion), lo senala.
4. Adaptacion: ajusta numero de relaciones simultaneas y fuerza del distractor superficial.
5. Siguiente trial.

### Reglas
La respuesta correcta preserva la relacion estructural, NUNCA la similitud de rasgos. Cada opcion distractora codifica un tipo de error: similitud-superficial-con-C, relacion-invertida, relacion-distinta (`distractorType`). Relaciones definidas abstractamente (no dependientes de vocabulario cultural). Impulsividad estandar.

### Inputs
Tap (1 de 4), chips confianza, opcional tap para nombrar la relacion (seleccion).

### Variables de dificultad
1. `relationCount` (relaciones simultaneas a preservar: 1=simple, 2-3=compuesta): 1-3.
2. `surfaceLureStrength` (cuan tentador es el distractor superficial): 1-3.
3. `relationAbstraction` (concreta-espacial .. abstracta-causal): 1-3.
4. `inversionDistractorPresence` (opcion con relacion invertida): 0-1.
5. `domainDistance` (fuente y objetivo en dominios cercanos vs lejanos): 1-3.
6. `optionCount`: 4-5.
7. `relationNamingRequired`: bool.

### Algoritmo adaptativo
`decide`: caer en similitud superficial repetidamente -> DECREASE + MICRO_LESSON "mapea la relacion, no el rasgo" + baja `surfaceLureStrength`; `avgIcr<55` -> DECREASE; alto rendimiento + isomorfos -> INCREASE (`relationCount`, `domainDistance`). El `errorTag` de tipo de distractor permite micro-leccion dirigida. Baseline ancla `relationCount`.

### Metricas primarias
ICR, accuracy de mapeo relacional, **surface-lure error rate** (elegir similitud superficial) via `errorTags`, `confidenceScore`.

### Metricas secundarias
Tasa de relacion-invertida, accuracy por `relationAbstraction` (concreto vs abstracto), tiempo, `independenceScore`.

### Senales de fatiga o mala calidad
surface-lure error rate creciente intra-sesion (cae el control relacional con fatiga), impulsividad, overconfidence, accuracy~azar con `relationCount=3` (desborde de mapeo).

### Scoring sugerido
Formula ICR. `correct` = D relacionalmente correcto. `conceptualError=1` cuando el error es similitud superficial o relacion invertida (`distractorType`, peso 0.15). Si `relationNamingRequired`, la relacion nombrada correctamente aporta a `justificationScore`. La velocidad solo resta via impulsividad.

### Antitrampa / calidad de datos
Distractor superficial fuerte (`surfaceLureStrength=3`) es el discriminador clave: distingue mapeo relacional de coincidencia de rasgos. Relaciones abstractas (no vocabulario) evitan medir conocimiento cultural. Aleatorizar posicion de la opcion correcta. Seed determinista; rotacion de dominios (`surfaceVariantId`).

### Variantes
1. **Relacion simple**: 1 relacion (A contiene B :: C contiene ?) con lure superficial.
2. **Relacion compuesta**: 2-3 relaciones a preservar simultaneamente (`relationCount=3`).
3. **Trampa de inversion**: incluye una opcion con la relacion correcta pero INVERTIDA (B:A en vez de A:B).

### UX
Pantalla: par fuente A:B con la relacion etiquetada visualmente (flecha/icono, no solo texto), C destacado, 4-5 opciones. Microanimacion funcional: al fallar por superficie, animacion breve que "tira" del rasgo superficial y lo descarta, mostrando la relacion como vinculo estructural (icono de relacion). Duracion: 8-10 items, ~5-6 min (loop corto).

### Implementacion KMP
Reusa **single-tap/choice** (4-5 opciones). Modelo: `LogicItem` con `sourceRelation: RelationType`, `analogyMapping`, `answerOptions` con `distractorType` (`SURFACE_LURE`/`INVERTED_RELATION`/`OTHER_RELATION`). Generador `StructuralAnalogyGenerator` seeded (combina relaciones abstractas de un banco de relaciones, no de palabras). Motores: `ReasoningScorer`, `DifficultyAdapter`. `errorTags` -> micro-leccion. Persistencia summary namespace `Logic`.

### Riesgos
Las analogias verbales clasicas son el ejemplo canonico de dependencia cultural/vocabulario (PROHIBIDO sin adaptacion). Debe construirse sobre relaciones ABSTRACTAS (espaciales, causales, ordinales) definidas formalmente, no sobre semantica de palabras, o medira vocabulario y no razonamiento.

### Revision critica
El Revisor destruye: (1) RIESGO ROJO de dependencia cultural — las analogias A:B::C:D son notoriamente sensibles a vocabulario/educacion. EXIGE que TODAS las relaciones sean formalmente definidas (contencion, orden, oposicion, causalidad simple) sobre estimulos abstractos/visuales, NUNCA sobre conocimiento lexico; si una sola relacion depende de saber el significado de una palabra, se descarta. (2) El distractor superficial debe ser objetivamente "superficial" — definir operacionalmente que rasgo es superficial vs relacional, o el autor decide arbitrariamente. (3) `relationCount=3` puede medir memoria de trabajo (mantener 3 relaciones), no analogia -> cap y separar. Correcciones: banco de relaciones formales-abstractas auditado por `review-cognitive-claims` (cero dependencia lexica), definicion operacional de superficial, cap de `relationCount`.

### Veredicto
Aprobado con cambios: el razonamiento relacional es nucleo logico de alto valor, pero solo sobrevive si se construye sobre relaciones formales-abstractas (no vocabulario); con dependencia lexica se descarta por frontera cultural.