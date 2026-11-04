## Modulo: Lenguaje (razonamiento verbal)

I have a complete picture of the module. I've read: the catalog, all 6 banks, the item model + factories, both scoring engines, the adaptation engine, evidence engine, near-transfer verifier, all three gates, repair engine, claim engine, profile projector, the playable state holder, the root screen, the unused reducer, the taxonomy, and the persistence layer. I've confirmed which infrastructure is wired into the playable path (scoring → evidence → projector → claim → repair, all with default/empty contexts) and which is dead in production (adaptation, transfer verifier, fatigue/fairness/confounder gates, persistence, exposure, latency, confidence, multi-level laddering).

I now have everything needed to write the brutal audit.

# Auditoría cognitiva — Módulo "Lenguaje (razonamiento verbal / pensamiento crítico)" de MindBoost

> Auditoría sobre **código real** del directorio `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language`. Leídos: catálogo, los 6 bancos, modelo de ítem y factories, `ClosedScoringEngine`, `DefaultAdaptationEngine`, `DefaultEvidenceEngine`, `DefaultNearTransferVerifier`, `FatigueGate`/`FairnessGate`/`ConfounderEngine`, `DefaultRepairEngine`, `DefaultClaimEngine`, `ProfileProjector`, `LanguageSessionStateHolder` (el loop jugable real), `LanguageRootScreen`, `LanguageSessionReducer` (no usado), taxonomía y persistencia.

> **HALLAZGO TRANSVERSAL CRÍTICO (léelo antes que cualquier ejercicio).** El único camino jugable es `LanguageRootScreen` → `LanguageSessionStateHolder`. Ese holder:
> 1. Recorre los ítems **en orden de array fijo** (`currentItems[index]`, `LanguageSessionStateHolder.kt:99,119`). **No invoca `DefaultAdaptationEngine`** — la adaptación está construida y testeada pero **muerta en producción** (grep confirma: solo referenciada en su propio archivo y `SessionContext`).
> 2. Llama a scoring con `ScoringTiming()` y `ScoringContext(bankType = item.bankType)` (`:101`) — es decir, **sin latencia, sin confianza, fatiga=FRESH, confounders=NONE, isRepeatedItem=false** SIEMPRE. Por tanto `FatigueGate`, `FairnessGate`, `ConfounderEngine`, `NearTransferVerifier` y todos los `AttemptFlag` de §5.6 son **inalcanzables** en el juego real.
> 3. Mantiene los `EvidencePacket` en una `mutableListOf` en memoria (`:71`) y proyecta un perfil nuevo en `finish()`. **Nunca usa `LocalEvidenceRepository`/`ExposureRepository`/`SessionSummaryRepository`** (grep en `presentation` = 0 matches). **No hay persistencia, ni tracking de exposición, ni medición longitudinal.** Cada sesión es borrón y cuenta nueva.
> 4. Todos los bancos son `BankType.TRAINING` → `EvidenceEngine` solo puede emitir **E1_TRAINED_IMPROVEMENT** (`DefaultEvidenceEngine.kt:46`). **E2 (transferencia) es estructuralmente inalcanzable** desde la app. Toda la maquinaria de transferencia es decorado.
>
> Consecuencia: lo que el usuario JUEGA es **6 baterías de 6 ítems de opción múltiple, en orden fijo, sin adaptación, sin tiempo, sin memoria entre sesiones**. La arquitectura "elite" (gates, ladder de evidencia, adaptación por utilidad, IRT-ready) existe en el dominio pero **no toca el bucle jugable**. Esto es el riesgo #1 del módulo y reaparece en cada ejercicio.

---

## Ejercicio: Evidence Lock (evidence_lock)

### Diagnóstico rápido
Es el mejor ejercicio del módulo, y de lejos. La mecánica de "elige la conclusión soportada **y marca la evidencia exacta**" con penalización a 0.5 por acertar sin anclar (`ClosedScoringEngine.kt:79-84`) entrena algo cognitivamente real: no sobre-leer. Pero su banco es de **6 ítems** y la "evidencia requerida" se selecciona con tap sin orden ni distractores de evidencia plausibles, así que el techo llega rápido.

### Mecanismo cognitivo
Entrena **inferencia restringida + monitorización metacognitiva del salto inferencial** (control inhibitorio sobre la tentación de concluir de más). La parte que produce adaptación cognitiva real es el doble requisito: opción correcta Y `requiredEvidenceIds` completos; acertar la opción pero no anclar la evidencia se capa a 0.5 y se marca `INF_OVER` (`ClosedScoringEngine.kt:84-87`). Eso fuerza al usuario a justificar, no a adivinar — es el núcleo del pensamiento crítico textual y está **bien alineado** con el objetivo declarado (INF legítima).
- **Sobra / contamina:** que la selección de evidencia sea sin orden y `captured` solo cuente intersección/tamaño (`:77`) significa que **marcar TODAS las evidencias siempre satisface `containsAll`** → el usuario aprende a marcar todo y nunca es penalizado por sobre-marcar. Esto es un agujero de medición serio: no hay penalización por seleccionar evidencia irrelevante.
- **Solo práctica de tarea:** con 6 ítems y opciones donde el distractor `c` es casi siempre un absurdo ("Hará calor"), la dificultad real está en 2-3 ítems.

### Potencial de transferencia
**Baja → media** (potencial media si se arregla; hoy baja). El constructo (no afirmar más de lo que el texto prueba) es genuinamente transferible a lectura de noticias, contratos, papers. Pero: (a) banco finito de 6 → se memoriza; (b) todo es `TRAINING` → el sistema **nunca puede certificar ni near-transfer**; (c) sin material nuevo isomórfico, es mejora-en-la-tarea, no transferencia. Para subir a media-alta: banco de 60+ con plantillas isomórficas, evidencia con distractores (spans irrelevantes que NO deben marcarse), y un `TRANSFER_LAB`/`PARALLEL_FORM` real conectado.

### Problemas detectados
- **`EvidenceLockBank.kt` completo:** solo 6 ítems, niveles 1-3 con 2 ítems por nivel. Banco finito y memorizable.
- **`ClosedScoringEngine.kt:77`** `captured = (required intersect selected)/required.size` — **no penaliza evidencia sobrante**. Marcar todos los spans = anclaje perfecto. Bug de diseño de medición.
- **`ClosedScoringEngine.kt:79-83`**: `evidenceComplete = selected.containsAll(required)` → mismo problema: over-selection es invisible.
- Distractor `c` trivial en casi todos los ítems (`ev_lock_1..6`): infla el acierto, reduce la información del ítem.
- Sin orden ni justificación de por qué ESE span y no otro.

### Riesgos de falsa mejora
Altísimo por dos vías: (1) **memorización del banco** (6 ítems, mismo orden fijo cada run — `LanguageSessionStateHolder` no baraja); (2) **estrategia "marca todo"** que garantiza el bonus de evidencia sin entender nada. Un usuario puede llegar a 100% sin leer, marcando la opción no-absurda y todos los spans.

### Métricas recomendadas
- **Principal:** % de ítems con opción correcta **Y** evidencia exacta (ni de menos ni de más) en **material no visto** (`PARALLEL_FORM`).
- **Secundarias:** precisión de anclaje (F1 sobre spans: penaliza sobre-marcado, hoy ausente), tasa `INF_OVER` longitudinal, latencia (hoy no se mide → no se puede separar "rápido y bien" de "rápido y suerte"), recuperación tras `INF_OVER` (¿mejora al ítem siguiente del mismo patrón?).
- **Engañan hoy:** `accuracyPercent` (`LanguageSessionStateHolder.kt:157-162`) cuenta solo correctness≥1.0 sobre 6 ítems fijos → es una métrica de práctica memorizable, no de habilidad. El "skillLines: Mejora entrenada" del DONE es E1 garantizado por construcción, no evidencia.

### Progresión recomendada
Hoy la "progresión" son 3 niveles × 2 ítems jugados en orden fijo. Hay que: (1) baremar dificultad por nº de inferencias-puente y plausibilidad del distractor; (2) escala adaptativa real conectando `DefaultAdaptationEngine` (que YA scorea `information = 1 - reliability`); (3) zona óptima ~75-85% acierto subiendo nivel; (4) introducir spans-trampa para evitar techo por "marca todo".

### Rediseño propuesto
**Evidence Lock v2 — anclaje con coste.** Mecánica: elige conclusión + marca spans, **pero cada span irrelevante marcado resta** (F1 de anclaje, no recall puro). Añadir un 3er paso: "¿qué conclusión sería sobre-inferencia?" (elegir el `INF_OVER` activamente). Banco 60+ con generador de plantillas isomórficas (mismo esqueleto lógico, contenido nuevo) etiquetadas `PARALLEL_FORM`/`TRANSFER_LAB` para alimentar `NearTransferVerifier` (ya existe, `DefaultNearTransferVerifier`). Dificultad adaptativa vía `DefaultAdaptationEngine`. Métricas: F1 de anclaje + tasa INF_OVER + transferencia E2. Feedback: mostrar el span correcto resaltado cuando falla. Por qué es superior: cierra el agujero "marca todo", habilita E2 real, y convierte el banco finito en una familia infinita. Test: property-test que un response con spans extra puntúe < uno exacto (hoy puntúan igual → test rojo que expone el bug).

### Puntuación 0-10
1) Mejora cognitiva real: **6** | 2) Transferencia vida real: **4** | 3) Claridad mecanismo: **8** | 4) Progresión: **3** | 5) Medición: **4** (agujero over-selection) | 6) Personalización: **2** (adaptación no cableada) | 7) Resistencia a práctica: **2** (banco 6, orden fijo) | 8) Metacognición: **6** | 9) Emocional: **3** | 10) Riesgo fatiga (10=sin riesgo): **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **3** | 12) Adherencia: **4** | 13) Escalabilidad perfiles: **4** | 14) Robustez científica: **5** | 15) Prioridad de mejora: **9**
**Nota global del ejercicio: 5.5/10**
**Decisión final:** Mantener con cambios mayores (es el ancla del módulo; arreglar over-selection, ampliar banco, cablear adaptación/transfer).

---

## Ejercicio: Semantic Surgery (semantic_surgery)

### Diagnóstico rápido
Test de vocabulario de 3 alternativas disfrazado de "cirugía semántica". El distractor coloquial (`majo`, `chunga`, `regulera`, `torear`) es siempre obvio y el patrón "la opción `b` es siempre la correcta" se aprende en 4 ítems. Entrena reconocimiento léxico, no producción ni precisión activa.

### Mecanismo cognitivo
Pretende entrenar **precisión semántica (LEX_P)** eligiendo entre cuasi-sinónimos. En realidad entrena **reconocimiento de registro** (formal vs coloquial), porque el distractor `PRAG_TONE` es jerga evidente y el `PROD_VAGUE` es un genérico ("bueno", "pequeña", "mala"). El usuario no hace cirugía: descarta lo coloquial y lo genérico, y queda la palabra técnica. Eso es una tarea de 2-AFC encubierta, no de discriminación fina entre sinónimos cercanos.
- **Contamina la medición:** que la respuesta correcta sea SIEMPRE la opción `b` (en 5 de 6 ítems, `sem_surg_1..5`) es un sesgo de posición explotable.
- **Solo práctica de tarea:** elegir entre 3 palabras dadas ≠ producir la palabra precisa. La habilidad real (LEX_P) es de producción/recuperación, y aquí es de reconocimiento pasivo.

### Potencial de transferencia
**Muy baja.** La literatura sobre entrenamiento de vocabulario por reconocimiento múltiple es clara: mejora la tarea de reconocimiento, transfiere poco a comprensión/producción. Y aquí la mecánica ni siquiera discrimina sinónimos cercanos reales (concluyente vs categórico vs taxativo) — opone técnico vs coloquial. Para subir a baja-media: oponer sinónimos del MISMO registro con diferencia de matiz real ("leve" vs "menor" vs "ligera"), y exigir justificar el matiz, no solo elegir.

### Problemas detectados
- **`SemanticSurgeryBank.kt`: la correcta es `"b"` en `sem_surg_1,2,3,4,5`** → sesgo de posición masivo. `sem_surg_6` cambia a `b` también. Es `b` en los 6. **El usuario puede acertar todo eligiendo siempre `b`.** Fallo de diseño grave.
- Distractor `PRAG_TONE` siempre es jerga evidente (`majo/chunga/regulera/torear/"Bss, nos vemos"`) → no discrimina precisión, discrimina registro.
- Banco de 6, sin material nuevo, `TRAINING` → solo E1.
- `IDENTIFY_NUANCE` declarado, pero no hay matiz: hay correcto/vago/coloquial.

### Riesgos de falsa mejora
Máximo. **Heurística de posición ("siempre b")** + **heurística de registro ("descarta la jerga")** dan 100% sin tocar la habilidad diana. Efecto novedad alto, banco memorizable, puntuación inflada. Es el ejercicio con mayor ratio pseudoprogreso/aprendizaje del módulo.

### Métricas recomendadas
- **Principal:** discriminación de matiz entre sinónimos co-registro en material nuevo (% correcto con randomización de posición).
- **Secundarias:** calibración (confianza vs acierto — hoy no se captura), latencia, tasa de error por confusión de matiz específico.
- **Engañan hoy:** accuracy (explotable por posición), y el `skillLines` LEX_P "Mejora entrenada" que se promociona aun cuando el usuario usó la heurística de posición.

### Progresión recomendada
Inexistente hoy (3 niveles cosméticos). Necesita: randomización de posición de la respuesta correcta (imprescindible), pares mínimos de matiz por nivel, y oposición intra-registro creciente.

### Rediseño propuesto
**Semantic Surgery v2 — par mínimo de matiz.** Dar una frase con hueco y 3 sinónimos del MISMO registro que difieren en UN rasgo (intensidad, certeza, polaridad: "leve/ligero/menor"; "afirmó/sugirió/insinuó"). Segundo paso: el usuario marca QUÉ rasgo cambia ("intensidad"/"certeza"). Posición aleatoria obligatoria. Banco 80+ generado de tablas de rasgos. Métrica: acierto en material nuevo + identificación correcta del rasgo. Feedback: glosa del matiz. Variante avanzada: reescribir una frase imprecisa eligiendo el verbo exacto (acerca a producción). Por qué es superior: elimina la heurística de registro y de posición, mide matiz real, y el segundo paso fuerza metacognición léxica. Test: chi-cuadrado de que la posición de la correcta es uniforme; test de que la heurística "siempre b" baja a ~33%.

### Puntuación 0-10
1) Mejora cognitiva real: **3** | 2) Transferencia vida real: **2** | 3) Claridad mecanismo: **5** | 4) Progresión: **2** | 5) Medición: **2** (sesgo posición) | 6) Personalización: **2** | 7) Resistencia a práctica: **1** | 8) Metacognición: **2** | 9) Emocional: **3** | 10) Riesgo fatiga: **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **1** | 12) Adherencia: **3** | 13) Escalabilidad: **3** | 14) Robustez científica: **2** | 15) Prioridad de mejora: **9**
**Nota global del ejercicio: 3.0/10**
**Decisión final:** Rediseñar completamente (arreglar el sesgo de posición es urgente; la mecánica actual mide registro, no precisión).

---

## Ejercicio: Compression Ladder (compression_ladder)

### Diagnóstico rápido
"Elige el mejor resumen" entre 3, donde el distractor `a` siempre tira la tesis ("habla de cosas") y el `c` siempre la distorsiona. Una vez visto el patrón, es identificar "la opción del medio con dos cláusulas". No hay compresión activa: el usuario no comprime, reconoce un resumen ya hecho.

### Mecanismo cognitivo
Declara entrenar **síntesis bajo restricción (SUM)**. Pero la mecánica es **reconocimiento de resumen**, no producción de resumen. El nombre "Ladder" (escalera 5→3→1 palabras) sugiere compresión progresiva real; el código no la implementa: es opción múltiple estática. La discriminación cognitiva existe (distinguir "perdió la tesis" de "distorsionó la tesis" es real, y los `ErrorType` SUM_NO_THESIS / SUM_DISTORT son finos), pero el usuario lo hace por eliminación de los dos malos, no por construir.
- **Contamina:** la correcta es **siempre `"b"`** (`comp_lad_1..6`) → mismo sesgo de posición que Semantic Surgery.
- **Sobra:** la "escalera" prometida en doc/subtitle no existe en mecánica.

### Potencial de transferencia
**Muy baja → baja.** Reconocer un buen resumen no entrena producir uno (la habilidad funcional: resumir un email, un informe). La transferencia real exigiría que el usuario ESCRIBA la compresión, lo que requiere el Evaluator OS (existe `LocalStubEvaluatorClient`, no cableado). Hoy es mejora-en-la-tarea pura sobre 6 ítems.

### Problemas detectados
- **`CompressionLadderBank.kt`: correcta = `"b"` en los 6 ítems.** Sesgo de posición explotable.
- Distractores estructuralmente idénticos en todos los ítems (`a`=SUM_NO_THESIS genérico, `c`=SUM_DISTORT exagerado) → patrón aprendible sin leer el texto.
- "Compression Ladder" sin compresión: no hay budget de palabras, no hay 5→3→1. El subtitle y el KDoc (`CompressionLadderBank.kt:11-16`) prometen una mecánica que el código no tiene → **gap diseño-implementación**.
- Banco 6, `TRAINING`, sin material nuevo.

### Riesgos de falsa mejora
Muy alto: posición fija (`b`) + estructura de distractores fija (uno vacío, uno exagerado). El usuario aprende "elige la del medio con matiz", no a sintetizar. Pseudoprogreso garantizado.

### Métricas recomendadas
- **Principal (si se hace producción):** fidelidad de la tesis + densidad informativa del resumen escrito en material nuevo (rúbrica). Sin producción, esta tarea no mide SUM.
- **Secundarias:** tasa SUM_NO_THESIS vs SUM_DISTORT (qué tipo de error domina), recuperación tras error.
- **Engañan hoy:** accuracy (posición explotable). El crédito SUM en `skillLines`.

### Progresión recomendada
Reemplazar opción múltiple por **compresión escalonada producida**: resume en ≤25 palabras → ≤12 → ≤6, validando que la tesis sobreviva en cada escalón. Dificultad por longitud del texto fuente y nº de cláusulas a podar. Sin producción real, no hay progresión significativa posible.

### Rediseño propuesto
**Compression Ladder v2 — escalera real.** Paso 1 (selección, calentamiento): identificar la tesis en el texto. Paso 2 (producción): el usuario teclea un resumen bajo budget decreciente; el `LocalStubEvaluatorClient`/Evaluator OS valida que (a) la tesis está, (b) no se distorsiona, (c) cabe en el budget. Si no hay Evaluator, mecánica intermedia: **construir el resumen arrastrando cláusulas** (mantener tesis + podar ruido) — verificable de forma determinista sin LLM. Banco grande con plantillas. Métrica: supervivencia de tesis bajo compresión + densidad. Por qué es superior: hace al usuario COMPRIMIR (la habilidad), no reconocer; cumple la promesa del nombre; randomiza posición. Test: que arrastrar el ruido en vez de la tesis puntúe 0; chi-cuadrado de posición.

### Puntuación 0-10
1) Mejora cognitiva real: **3** | 2) Transferencia: **2** | 3) Claridad mecanismo: **5** (nombre engaña) | 4) Progresión: **2** | 5) Medición: **2** | 6) Personalización: **2** | 7) Resistencia a práctica: **1** | 8) Metacognición: **3** | 9) Emocional: **3** | 10) Riesgo fatiga: **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **1** | 12) Adherencia: **3** | 13) Escalabilidad: **3** | 14) Robustez científica: **2** | 15) Prioridad de mejora: **8**
**Nota global del ejercicio: 3.0/10**
**Decisión final:** Rediseñar completamente (convertir reconocimiento en producción/construcción; arreglar posición fija).

---

## Ejercicio: Argument X-Ray (argument_xray)

### Diagnóstico rápido
Localizar el eslabón débil de un argumento corto. Conceptualmente el ejercicio más valioso del módulo junto a Evidence Lock — detectar el salto inferencial es pensamiento crítico nuclear. Pero ejecutado como 3-AFC con distractores caricaturescos (la opción `c` es siempre un ad-hominem absurdo: "quien lo propone odia los parques") y la correcta siempre `"b"`.

### Mecanismo cognitivo
Entrena **identificación de falacia / evaluación de validez argumental (ARG/CRIT)** — un constructo real y transferible. La distinción entre `ARG_GAP` (señalar un hecho dado en vez del salto) y `ARG_STRAW` (atacar una caricatura) es una taxonomía fina y correcta (`ArgumentXRayBank.kt:14-16`). El problema: la opción `a` es siempre "un hecho dado" y la `c` siempre "una caricatura/ad-hominem evidente", así que el usuario detecta la estructura ("elige la que habla de la INFERENCIA, no del hecho ni de la persona") sin analizar el argumento concreto.
- **La parte que adapta:** discriminar salto-lógico de premisa-fáctica es genuino.
- **Lo que contamina:** posición fija `b` + distractores tipados de forma constante = patrón meta-aprendible.

### Potencial de transferencia
**Media** (la más alta del módulo junto a Evidence Lock, si se arregla). Detectar generalizaciones apresuradas, causa-correlación, ad-hominem es directamente útil en consumo de noticias y debate. Limitado hoy por: distractores caricaturescos (en la vida real las falacias no vienen etiquetadas), banco 6, posición fija, sin material nuevo. Para subir a alta: distractores plausibles (varios saltos candidatos, uno el más débil), y exigir **nombrar** la falacia tras localizarla.

### Problemas detectados
- **`ArgumentXRayBank.kt`: correcta = `"b"` en los 6 ítems.** Sesgo de posición.
- Distractor `c` siempre ad-hominem/strawman transparente (`arg_xray_1..6`): no es un candidato serio → reduce la dificultad a 2-AFC trivial.
- No se nombra la falacia (generalización, falsa causa, autoridad) — se localiza pero no se clasifica, perdiendo metacognición.
- Banco 6, `TRAINING`.

### Riesgos de falsa mejora
Alto: posición `b` + heurística "elige la opción que cuestiona la inferencia". El usuario no analiza el argumento; reconoce cuál de las 3 frases es del tipo correcto.

### Métricas recomendadas
- **Principal:** localización correcta del eslabón débil **entre varios candidatos plausibles** en material nuevo + clasificación de la falacia.
- **Secundarias:** falsos positivos (marcar un eslabón sano), confusión ARG_GAP↔ARG_STRAW, latencia.
- **Engañan hoy:** accuracy (posición + estructura), crédito ARG.

### Progresión recomendada
Niveles por nº de eslabones candidatos plausibles (2→3→4) y sutileza del salto (generalización obvia → causa-correlación → apelación cuantificadora). Cablear adaptación. Añadir el paso "nombra la falacia".

### Rediseño propuesto
**Argument X-Ray v2 — disección de dos pasos.** Paso 1: subrayar el eslabón más débil entre 3-4 candidatos TODOS plausibles (sin la caricatura regalada). Paso 2: clasificar el tipo (generalización apresurada / falsa causa / autoridad / ad-hominem / falso dilema), eligiendo de un set fijo. Banco 80+ con plantillas por tipo de falacia, material nuevo en `PARALLEL_FORM`. Posición aleatoria. Métrica: localización + clasificación en material no visto → habilita E2 vía `NearTransferVerifier`. Feedback: explicación de por qué los otros eslabones son sólidos. Por qué es superior: elimina el regalo del strawman, añade clasificación (metacognición + nomenclatura transferible), mide transferencia. Test: que la heurística de posición caiga a azar; que marcar un eslabón sano puntúe 0.

### Puntuación 0-10
1) Mejora cognitiva real: **5** | 2) Transferencia: **5** | 3) Claridad mecanismo: **7** | 4) Progresión: **3** | 5) Medición: **3** | 6) Personalización: **2** | 7) Resistencia a práctica: **2** | 8) Metacognición: **4** | 9) Emocional: **4** | 10) Riesgo fatiga: **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **2** | 12) Adherencia: **5** | 13) Escalabilidad: **4** | 14) Robustez científica: **4** | 15) Prioridad de mejora: **8**
**Nota global del ejercicio: 4.5/10**
**Decisión final:** Rediseñar parcialmente (concepto fuerte, ejecución débil; distractores plausibles + clasificación + arreglar posición).

---

## Ejercicio: Scope & Negation (scope_negation)

### Diagnóstico rápido
Resolver el alcance de "no/solo/todos/ni…ni/casi". El constructo (alcance de cuantificadores y negación) es real, frecuente y subestimado como fuente de errores de comprensión. Es el único ejercicio del módulo donde la posición de la correcta **sí varía** (`scope_neg_3` usa distractor `c`=SYN_SCOPE), señal de mejor diseño. Pero sigue siendo 3-AFC sobre 6 ítems con un distractor `c` casi siempre absurdo ("No aprobó nadie").

### Mecanismo cognitivo
Entrena **procesamiento sintáctico-semántico fino (SYN): resolución del alcance de operadores lógicos en lenguaje natural**. Es legítimo y bien dirigido: distinguir "no todos suspendieron" (≥1 aprobó) de "ninguno suspendió" es exactamente el error de alcance que comete la gente. `SYN_SCOPE` vs `LIT_DIST` separa "mal alcance" de "contradice el texto" — taxonomía correcta. La parte que produce adaptación es resolver el operador; lo que sobra es el distractor absurdo (`c`) que regala un 3-AFC → 2-AFC.

### Potencial de transferencia
**Baja → media.** El alcance de negación/cuantificadores aparece en contratos, instrucciones, condiciones legales, lógica. Si se entrena con material variado (condicionales "nadie...sin", dobles negaciones, "salvo/excepto", "a menos que") transfiere a comprensión precisa. Hoy limitado por banco 6 y distractor regalado. Es de los pocos con un constructo cuya transferencia tiene base (procesamiento sintáctico es un mecanismo, no un truco).

### Problemas detectados
- Banco 6, `TRAINING` → solo E1, sin material nuevo.
- Distractor `c` casi siempre absurdo/`LIT_DIST` (`scope_neg_1,2,4,5`): "No aprobó nadie", "Vinieron los dos", "Suspendieron todos" → 2-AFC efectivo.
- `scope_neg_3` invierte el mapeo de distractores (`c`→SYN_SCOPE, `a`→LIT_DIST) — buena variación, pero es la excepción.
- Sin tiempo: el alcance sintáctico es parcialmente un proceso de velocidad/automatización; medirlo sin latencia pierde la mitad de la señal (resolución correcta pero lenta ≠ automatizada).

### Riesgos de falsa mejora
Medio-alto: banco memorizable (6, orden fijo). Menos sesgo de posición que sus hermanos (positivo). El distractor absurdo infla acierto. Sin latencia, no se distingue automatización de cálculo laborioso.

### Métricas recomendadas
- **Principal:** acierto en resolución de alcance en material nuevo, **con latencia** (automatización = correcto + rápido).
- **Secundarias:** error por tipo de operador (negación simple vs doble vs cuantificador vs condicional), RT por tipo, calibración.
- **Engañan hoy:** accuracy sin latencia (no separa automatizado de laborioso); banco fijo.

### Progresión recomendada
Niveles por complejidad del operador: negación simple → cuantificador → "solo/salvo" → doble negación → condicional negado ("nadie…sin"). Presión de tiempo creciente en niveles altos para forzar automatización. Distractores ambos plausibles (dos lecturas de alcance defendibles, una correcta).

### Rediseño propuesto
**Scope & Negation v2 — paráfrasis de alcance bajo tiempo.** Dado el enunciado, elegir entre 3 paráfrasis TODAS gramaticales (dos lecturas de alcance + la correcta), sin el absurdo regalado. Añadir captura de **latencia** (ya hay `LatencyMs` en el modelo, `ScoringTiming.latencyMs`) y bonus por velocidad en niveles altos → mide automatización. Variante: marcar verdadero/falso sobre 3 implicaciones del enunciado (entrena la consecuencia del alcance, no solo la lectura). Banco grande por tipo de operador, material nuevo. Por qué es superior: mide automatización (velocidad), elimina el distractor regalado, cubre más operadores. Test: que las dos lecturas-distractoras sean igual de gramaticales (revisión humana); que la latencia entre en el score.

### Puntuación 0-10
1) Mejora cognitiva real: **5** | 2) Transferencia: **4** | 3) Claridad mecanismo: **7** | 4) Progresión: **3** | 5) Medición: **3** (sin latencia) | 6) Personalización: **2** | 7) Resistencia a práctica: **3** | 8) Metacognición: **3** | 9) Emocional: **3** | 10) Riesgo fatiga: **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **3** | 12) Adherencia: **4** | 13) Escalabilidad: **5** | 14) Robustez científica: **5** | 15) Prioridad de mejora: **7**
**Nota global del ejercicio: 4.2/10**
**Decisión final:** Rediseñar parcialmente (constructo sólido; añadir latencia, distractores plausibles, más operadores).

---

## Ejercicio: Source Triage (source_triage)

### Diagnóstico rápido
Clasificar afirmaciones como dato/opinión/especulación y detectar contexto faltante. Constructo de alfabetización mediática valioso, pero implementado como clasificación de 3 categorías sobre 6 ítems, donde los primeros son triviales ("El agua hierve a 100°C" = dato) y la categoría es adivinable por palabras clave ("el próximo año" = especulación; "la mejor" = opinión).

### Mecanismo cognitivo
Entrena **lectura crítica / estatus epistémico de una afirmación (CRIT)**: separar hecho verificable de juicio de valor y de predicción, y detectar reclamo con contexto ausente (`src_triage_6`: "9 de cada 10 dentistas"). Es un constructo real de pensamiento crítico. PERO la mayoría de los ítems se resuelven por **marcadores léxicos superficiales** ("el próximo año"→futuro→especulación; "la mejor"→valorativo→opinión), no por análisis epistémico. El usuario aprende a mapear palabras-señal a categorías, no a evaluar fuentes. Los ítems buenos son `src_triage_5` (separar "sin duda fracasará" del dato dentro de la misma frase) y `src_triage_6` (qué falta para fiarse) — esos sí entrenan el constructo.

### Potencial de transferencia
**Baja → media.** Distinguir dato de opinión y detectar "datos sin fuente" transfiere a consumo de noticias/publicidad — alto valor en la vida real. Pero la mecánica por marcadores léxicos transfiere poco (la realidad no rotula). El ítem de "qué falta para fiarte" (`src_triage_6`) es el de mayor transferencia y debería ser la mecánica central, no un caso aislado. Subir a media-alta: afirmaciones mixtas (dato+opinión en la misma frase, como `src_triage_5`), detección de fuente/interés, sin marcadores-regalo.

### Problemas detectados
- Mezcla dos tareas distintas en un banco: clasificación dato/opinión/especulación (`src_triage_1..4`) y detección de contexto faltante (`src_triage_5,6`). La segunda es mucho más valiosa y está infrarrepresentada.
- `src_triage_1` trivial (dato físico de libro de texto) → ítem-relleno que infla acierto.
- Marcadores léxicos regalan la categoría en `src_triage_2,3`.
- `src_triage_1` y `4`: `distractorErrors` mapea `b` y `c` ambos a `CRIT_SOURCE` (`SourceTriageBank.kt:49,86`) → no discrimina qué error específico cometió el usuario, perdiendo granularidad diagnóstica.
- Banco 6, `TRAINING`.

### Riesgos de falsa mejora
Medio-alto: heurística de marcadores léxicos ("futuro→especulación") da buen acierto sin evaluar fuentes; banco memorizable. El ítem trivial infla el % .

### Métricas recomendadas
- **Principal:** detección de afirmaciones con contexto/fuente ausente + clasificación correcta en **frases mixtas** (dato+juicio) de material nuevo.
- **Secundarias:** tasa de "credulidad" (aceptar opinión como dato) vs "cinismo" (rechazar dato verificado), error CRIT_SOURCE vs INF_OVER.
- **Engañan hoy:** accuracy (marcadores léxicos + ítem trivial); el granularidad diagnóstica está rota en 2 ítems (ambos distractores → mismo error).

### Progresión recomendada
Nivel 1: clasificar frases puras. Nivel 2: frases mixtas (separar el dato del juicio en la misma oración, como `src_triage_5`). Nivel 3: detectar qué falta para fiarse (fuente, muestra, conflicto de interés) — la habilidad cumbre. Eliminar ítems triviales y marcadores-regalo.

### Rediseño propuesto
**Source Triage v2 — escáner epistémico.** Sobre un fragmento de noticia/anuncio, el usuario (1) etiqueta cada cláusula como dato/opinión/predicción, (2) marca qué dato necesita fuente, (3) identifica el posible interés de quien lo afirma. Distractores sin marcadores léxicos-regalo (predicciones redactadas en presente, opiniones disfrazadas de dato). Banco grande con fragmentos reales reescritos, material nuevo `PARALLEL_FORM`. Granularidad diagnóstica arreglada (cada distractor → su `ErrorType` específico). Por qué es superior: entrena el constructo cumbre (detectar lo que falta), no el mapeo léxico; mide credulidad y cinismo por separado; alto valor de transferencia a vida real. Test: que afirmaciones sin marcadores léxicos sigan clasificándose bien (descarta la heurística); que cada distractor mapee a un error distinto.

### Puntuación 0-10
1) Mejora cognitiva real: **5** | 2) Transferencia: **5** | 3) Claridad mecanismo: **6** | 4) Progresión: **3** | 5) Medición: **3** (granularidad rota en 2 ítems) | 6) Personalización: **2** | 7) Resistencia a práctica: **2** | 8) Metacognición: **4** | 9) Emocional: **4** | 10) Riesgo fatiga: **8** | 11) Riesgo pseudoprogreso (10=sin riesgo): **3** | 12) Adherencia: **5** | 13) Escalabilidad: **4** | 14) Robustez científica: **4** | 15) Prioridad de mejora: **7**
**Nota global del ejercicio: 4.3/10**
**Decisión final:** Rediseñar parcialmente (centrar en "qué falta para fiarte" + frases mixtas; quitar triviales y marcadores-regalo; arreglar diagnóstico).

---

## Veredicto del módulo "Lenguaje (razonamiento verbal / pensamiento crítico)"

### Capacidades bien cubiertas / huecos / ausentes
- **Bien cubiertas (a nivel de constructo, no de ejecución):** inferencia anclada en evidencia (INF), evaluación argumental (ARG), alcance sintáctico (SYN), lectura crítica (CRIT). La **taxonomía** (`LanguageTaxonomy.kt`: 19 traits, 14 ErrorType con repair plans completos) es de calidad superior y el `EvidenceState` ladder + `ClaimEngine` (honesty contract) son **lo mejor del módulo**: el sistema está diseñado para NO mentir sobre la mejora. Ese rigor honesto es real y debe preservarse.
- **Huecos:** todo es **reconocimiento (opción múltiple)**, cero **producción** (escribir, resumir, reescribir). LEX_P y SUM —que son habilidades de producción— se entrenan por reconocimiento, lo que es científicamente débil. No hay **velocidad/automatización** (latencia no se captura nunca). No hay **calibración metacognitiva** real (confianza no se pide pese a existir `confidence` en `ScoringTiming` y un `META_MIS` repair plan).
- **Capacidades AUSENTES:** producción escrita guiada (PROD_W/EDIT existen en taxonomía, no en juego), paráfrasis (PAR), memoria verbal funcional (WM_V), mediación (MED), adaptación pragmática activa (PRAG solo aparece como distractor). El Evaluator OS (`LocalStubEvaluatorClient`) está stubbeado y desconectado → toda la rama de producción abierta es vaporware jugable.

### Ejercicios redundantes o a fusionar
- **Semantic Surgery + Compression Ladder** comparten el mismo defecto fatal (correcta siempre `b`, distractores estructuralmente fijos, reconocimiento en vez de producción) y el menor valor. **Fusionar/reconstruir como un único bloque de "producción precisa"** (elegir palabra exacta → comprimir conservando tesis), idealmente con producción real vía Evaluator.
- **Evidence Lock + Argument X-Ray + Source Triage** forman el núcleo de pensamiento crítico y son **complementarios, no redundantes** (anclar evidencia / detectar salto / juzgar fuente). Mantener los tres, reforzados.
- **Scope & Negation** es único (procesamiento sintáctico) y no se fusiona con nada.

### Riesgos sistémicos del módulo
1. **[Riesgo científico — CRÍTICO] La arquitectura "elite" no toca el juego.** Adaptación, gates de fatiga/fairness/confounder, near-transfer, persistencia, exposición, latencia y confianza están **construidos y testeados pero muertos en el bucle jugable** (`LanguageSessionStateHolder` usa `ScoringContext` por defecto y orden de array fijo). El módulo *parece* riguroso por su dominio, pero el usuario juega una versión naïve. Esto es un Potemkin: el rigor existe en el código, no en la experiencia.
2. **[Riesgo científico — CRÍTICO] E2+ inalcanzable.** Todos los bancos son `TRAINING` → `DefaultEvidenceEngine` solo puede dar **E1_TRAINED_IMPROVEMENT**. El sistema **estructuralmente nunca puede afirmar transferencia**, aunque el `ClaimEngine` esté listo para ello. El `skillLines: "Mejora entrenada"` del DONE es un resultado **predeterminado por construcción**, no una medición — bordea el pseudoprogreso que el propio módulo dice evitar.
3. **[Riesgo de medición] Sesgo de posición masivo.** En 4 de 6 ejercicios la respuesta correcta es **siempre `"b"`** (Semantic Surgery, Compression Ladder, Argument X-Ray; Evidence Lock varía por ser evidencia). Un usuario que pulse siempre la segunda opción saca ~67% del módulo sin leer. Es el fallo más vergonzoso y el más barato de arreglar.
4. **[Riesgo de medición] Banco común de 6 ítems × orden fijo, sin barajar, sin persistencia de exposición en el juego.** Memorización garantizada en pocas sesiones; el `ExposureRepository` que lo evitaría no está cableado.
5. **[Riesgo de medición] Agujero de over-selection en Evidence Lock** (`ClosedScoringEngine.kt:77`): marcar todos los spans = anclaje perfecto.
6. **`LanguageSessionReducer`** (la máquina de estados con repair branching real, §6.5) **no se usa**: el holder reimplementa una versión sin repair-retry. Dualidad de fuentes de verdad → deuda y confusión.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Cablear lo que ya existe al bucle jugable.** Conectar `DefaultAdaptationEngine` (selección por información/utilidad, ya scorea `1 - reliability`), capturar **latencia y confianza** en cada submit (campos ya en `ScoringTiming`), activar `FatigueGate`, y **persistir** vía `LocalEvidenceRepository`/`ExposureRepository` para medición longitudinal y anti-memorización. Esto convierte un quiz estático en el sistema adaptativo que el dominio promete — sin escribir motores nuevos, solo wiring. Es el cambio de mayor ROI con diferencia.
2. **Crear bancos de transferencia (`PARALLEL_FORM`/`TRANSFER_LAB`) con plantillas isomórficas** para Evidence Lock, Argument X-Ray, Scope & Negation y Source Triage, y conectar `DefaultNearTransferVerifier`. Sin material nuevo del mismo constructo, el módulo no puede demostrar nada más que práctica memorizada. Esto desbloquea E2 y hace honestamente reclamable la "transferencia cercana".
3. **Mover de reconocimiento a producción/construcción y matar los distractores-regalo.** Randomizar posición (urgente), distractores plausibles (no caricaturas ni absurdos), cerrar el over-selection de Evidence Lock, y convertir Compression Ladder/Semantic Surgery en tareas de construcción (arrastrar/podar/reescribir) verificables determinísticamente o vía Evaluator. Producción + distractores reales es lo que separa "entrenar la habilidad" de "aprender el formato del test".

**Nota global del módulo (experiencia jugable real, no el dominio sobre el papel): 4.0/10.** El diseño de dominio y el contrato de honestidad valdrían un 7-8; la desconexión entre ese dominio y lo que el usuario realmente juega, más el sesgo de posición y el banco finito memorizable, lo hunden a 4. La buena noticia: la mayor parte del valor está a un *wiring* de distancia, no a un rediseño desde cero.

---

