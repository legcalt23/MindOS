# Auditoría Lenguaje

＃ Auditoría MindBoost — Módulo Lenguaje (rediseño quirúrgico)

> **Ground-truth anclado en código (no en el doc de diseño):** el módulo Lenguaje tiene HOY **6 ejercicios** en `LanguageExerciseCatalog` (Evidence Lock, Semantic Surgery, Compression Ladder, Argument X-Ray, Scope & Negation, Source Triage) + 1 "Sesión mixta". Los dos del doc — **E7 Lexical Access Sprint y E8 Calibrated Comprehension — NO EXISTEN en código** (cero `deadline`, `Brier`, selector de confianza, timebase). El scoring tiene **solo 3 `AnswerKey`**: `SingleChoice`, `EvidenceSelection`, `Ordering` (no existe `SpanLocalization` ni `RoleLabeling`). El `LanguageSessionStateHolder` **NO invoca `AdaptationEngine.selectNext` ni `NearTransferVerifier.verify`** (ni siquiera importados), usa `ScoringTiming()` por defecto (**sin RT real**, `tick` es un contador monótono de orden), `itemInformation=1.0` hardcodeado y `ScoringContext(bankType=…)` con **todos los confounders/fatiga en default** (nunca se elevan flags). Es un **cuestionario estático de 6 ítems fijos por ejercicio, sin barajar, sin reposición, sin holdout activo, sin inter-rater, sin exposure, sin adaptación, sin anti-gaming conductual, sin copy-scanner de build**. Lo único sólido y honesto: el `ClosedScoringEngine` (Jaccard exacto, Kendall-tau, gate de evidencia a peso 0 ante cualquier flag, `bankFactor=0.5` TRAINING) y la arquitectura `null≠0`/`preliminary=true`. Pero esa arquitectura **no recibe señal**: las flags nunca se activan porque el holder no las alimenta.

**Recuento de veredictos:** 2 REVISE · 2 REBUILD · 2 BLOCK · 2 RETIRE (los inexistentes E7/E8) + 1 BLOCK estructural (Sesión mixta). Prefiero bloquear que aprobar débil: ningún ejercicio del módulo está hoy en condiciones de actualizar perfil, porque **ninguno mide bajo gates reales** — el holder los neutraliza todos.

---

## E1 · Evidence Lock (`evidence_lock`) — **REVISE**

1. **Veredicto:** REVISE (es lo más fuerte del módulo; jugable y mecánica honesta, pero hoy no actualiza perfil por wiring muerto).
2. **Constructo:** Inferencia mínima suficiente (`INF`) — elegir la conclusión soportada Y anclar el subconjunto exacto de evidencia, penalizando el over-claiming.
3. **Señal primaria válida:** `captureRate = Jaccard(selectedEvidence, requiredEvidenceIds)` + correctness {0/0.5/1.0} del `ClosedScoringEngine.EvidenceSelection`. Implementado y testeado (marcar-todo o evidencia incompleta ⇒ 0.5 + `INF_OVER`). **Actualiza perfil solo si**: flags vacías + `Σ evidenceWeight ≥ umbral` + banco no agotado.
4. **Señales secundarias:** Tasa `INF_OVER`/trials (over-reading), alineamiento confianza↔soporte (no capturado: `ScoringTiming.confidence` siempre null), slope inter-sesión de captura.
5. **Contaminantes:** **Conocimiento previo** (item ev_lock_5 "el pan sabe igual"/ev_lock_6 "instituto" tocan temas con prior; no se eleva `HIGH_PRIOR_KNOWLEDGE`), legibilidad/longitud como proxy de dificultad, ausencia de RT de lectura mínimo (el doc lo pide, el código no lo aplica). `redundantWith`: Scope&Negation (ambos usan Jaccard sobre selección).
6. **Gaming:** Con **6 ítems fijos en orden fijo**, memorización total en 1–2 sesiones (la conclusión correcta es siempre "b" en 4 de 6). Estrategia "marca toda la evidencia" cap a 0.5 (bien penalizada), pero "marca la evidencia más corta/literal" funciona por la baja variación del banco. Sin detector de respuesta-sin-lectura.
7. **Gates obligatorios:** min-n por trait; `REPEATED_ITEM` (re-ver ítem ⇒ peso 0) — **modelado pero nunca activado**; `BANK_SEPARATION` ·0.5 TRAINING (sí funciona); `HIGH_PRIOR_KNOWLEDGE` obligatorio (ausente); RT-mínimo-de-lectura (ausente); fatiga (`FatigueGate` existe, no se llama).
8. **Métrica/scoring:** `INF_score = Σ(itemScore·weight)/Σweight`, null si Σweight<umbral; `overClaimIndex = nINF_OVER/nValid`. **n_min:** ≥12 trials válidos limpios por trait para `confident=true`; **agregación inter-sesión** obligatoria (1 sesión = 6 ítems es insuficiente). **null/calibrating** cuando Σweight bajo o banco repetido.
9. **Banco:** HOY 6 ítems fijos. **Mínimo exigible:** ≥40 ítems con `requiredEvidenceIds` auditados, rotación con `exposure tracking`, ≥20% temas neutros sin prior, **holdout de ítems isomorfos T2_NEAR** para `NearTransferVerifier`. Inter-rater no aplica (clave cerrada) pero sí **validación de unicidad de la evidencia requerida** (≥2 autores acuerdan el set mínimo).
10. **Copy permitido:** "Infiere solo lo que el texto prueba", "señal de práctica contra tu baseline", "evidencia incompleta", "estimación preliminar", "—" en CALIBRATING.
11. **Copy prohibido (bloquear en tests):** "mejora tu comprensión lectora", "detecta fake news", "pensamiento crítico", "lees mejor en la vida real", cualquier afirmación de transferencia amplia o de inteligencia.
12. **KMP:** Reusa `ClosedScoringEngine.EvidenceSelection`, `DefaultEvidenceEngine`, `DefaultAdaptationEngine` (existe, **wirear `selectNext`**), `NearTransferVerifier` (existe, **wirear `.verify`**). Test hooks: alimentar `ScoringContext` con confounders reales (prior, repetición, fatiga). Persistencia `SecureKeyValueStore` namespace `Language`, summaries-only.
13. **Tests obligatorios:** unit del scoring (marca-todo⇒0.5 ✓ ya existe); **property** (cualquier set ⊋ required ⇒ INF_OVER); **adversarial** (perfil "marca-todo", "elige-b-siempre", "respuesta<RTmin"); **golden-scoring** del `INF_score` agregado; **copy-scanner de build** para Lenguaje (no existe).
14. **Aceptación → APPROVE:** (a) `selectNext`+transfer wirados y testeados; (b) flag `HIGH_PRIOR_KNOWLEDGE` activado por banco-tema; (c) banco ≥40 con rotación/exposure; (d) min-n + agregación inter-sesión; (e) copy-scanner enganchado a `check`.
15. **Rediseño:** Mantener mecánica (es buena). Añadir RT-de-lectura mínimo como gate de calidad (no como score), banco grande rotado, prior-flag por tema, y wiring de adaptación. Reportar a `CognitiveDimension` verbal como señal *coarse*, nunca como "comprensión".
16. **Impacto (solo entrenamiento):** Medio-alto — es la señal más limpia del módulo si se le da banco y gates.
17. **Prioridad:** P1.
18. **Riesgo residual:** Sin banco grande sigue midiendo memorización; el 0.5 puede premiar adivinar la conclusión si la evidencia es trivial de marcar.

---

## E2 · Semantic Surgery (`semantic_surgery`) — **REVISE**

1. **Veredicto:** REVISE — mecánica de discriminación semántica defendible, pero **la "localización del token" del diseño NO existe** (no hay `AnswerKey.SpanLocalization`); hoy es single-choice puro.
2. **Constructo:** Discriminación semántica fina (`LEX_A/LEX_B`) — detectar la paráfrasis equivalente y aislar el operador que cambia el sentido (polaridad, cuantificador, alcance, conector, aspecto).
3. **Señal primaria válida:** Accuracy de equivalencia (`SingleChoice`). Implementado. Localización: **no implementada** → no puede ser señal primaria hoy.
4. **Señales secundarias:** Perfil de error por `changeType` (diseñado, no capturado: el banco no etiqueta `changeType` en una estructura agregable), falsos positivos por tipo, RT (ausente).
5. **Contaminantes:** **Variante-bias** (la equivalente siempre la más corta/natural — el doc lo advierte, no hay test de balanceo); dependencia del español (banco mono-idioma sin declararlo); legibilidad. `redundantWith`: Scope&Negation (cuantificador/alcance se solapan).
6. **Gaming:** Heurística "la candidata más rara es la mala" / "la más corta es la equivalente". Con 6 ítems fijos, memorización inmediata. Sin `decoyFluency` real en el banco.
7. **Gates obligatorios:** min-n por `changeType`; balanceo anti-bias (longitud/posición del pivote) — ausente; RT-mínimo; repetición; fatiga. Ninguno activo.
8. **Métrica/scoring:** `itemScore = equivalenceCorrect` (1.0/0.0); cuando se implemente localización: `0.6·equiv + 0.4·loc`. `typeProfile[changeType] = HIT/(HIT+MISS)`, **null si n<8 por tipo**. Agregación inter-sesión obligatoria por `changeType` (5 tipos × n_min ⇒ banco grande imprescindible).
9. **Banco:** 6 fijos. **Mínimo:** ≥50 ítems balanceados en los 5 `changeType` (≥10/tipo) con longitudes/posiciones de pivote balanceadas, exposure, holdout. Sin balanceo, el perfil por tipo es ruido.
10. **Copy permitido:** "¿Qué reformulación conserva exactamente el significado?", "cambió el cuantificador/la polaridad", "estimación preliminar", "solo español".
11. **Copy prohibido:** "vocabulario más rico", "mejora tu comprensión", "lees con más precisión en general", transferencia translingüística, inteligencia verbal.
12. **KMP:** Si se quiere localización: añadir `AnswerKey.SpanLocalization(correctOptionId, pivotTokenIndex)` al `when` exhaustivo (riesgo de rama olvidada → test de scoring obligatorio) + renderer token-tap. Si no, **declarar honestamente que mide solo equivalencia**, no localización.
13. **Tests obligatorios:** unit scoring por `changeType`; **balanceo anti-bias** (la equivalente no correlaciona con longitud); adversarial "más-corta-siempre"; golden por tipo; copy-scanner.
14. **Aceptación → APPROVE:** (a) banco ≥50 balanceado por tipo y longitud; (b) decisión explícita localización (impl+test) o claim restringido a equivalencia; (c) min-n por tipo + null honesto; (d) wiring adaptación.
15. **Rediseño:** Recortar a **discriminación de equivalencia pura** (sin localización en v1 — reduce superficie no probada), con banco grande balanceado por `changeType`, y perfil de debilidad por operador como feedback metacognitivo *interno* (no barra de perfil hasta n suficiente acumulado).
16. **Impacto:** Medio.
17. **Prioridad:** P2.
18. **Riesgo residual:** "matiz semántico" es resbaladizo; sin rúbrica cerrada por tipo validada, mide astucia de test. Solapamiento con E5 si no se delimita (E2=preservación de significado, E5=alcance del operador).

---

## E3 · Compression Ladder (`compression_ladder`) — **REVISE**

1. **Veredicto:** REVISE — mecánica honesta (reconocimiento de fidelidad, no producción), pero con `candidateCount` bajo la señal Kendall-tau es ruidosa y el banco es minúsculo.
2. **Constructo:** Compresión fiel ascendente (`COMP/SUM`) — reconocer el resumen que conserva la idea nuclear sin distorsión ni adición.
3. **Señal primaria válida:** `kendallTauNormalized(userOrder, correctOrder)` (`Ordering`) o accuracy de selección. Implementado y testeado (Kendall-tau parcial real).
4. **Señales secundarias:** Tasa de adición (inventar) vs omisión, perfil de `distortionType` confundido (no capturado en agregable), slope.
5. **Contaminantes:** **Resolución discreta de Kendall-tau con candidateCount=3** (pocos valores posibles ⇒ señal gruesa, el doc exige ≥4 en alto nivel, el código no lo fuerza); destreza motora del drag como confound; cultura del texto.
6. **Gaming:** Con 3 candidatos, adivinar tiene rendimiento alto (Kendall-tau de orden aleatorio ≈ 0.5 esperado). 6 ítems fijos ⇒ memorización del orden. Sin `decoyFidelity` calibrado.
7. **Gates obligatorios:** `candidateCount≥4` en niveles altos (ausente); min-n; repetición; fatiga; RT informativo. Ninguno activo.
8. **Métrica/scoring:** `fidelityIndex = Σ(τ_i·weight)/Σweight`, null si insuficiente; `COMP_score = fidelity·(1−additionPenalty·additionRate)`. **n_min:** ≥10 ítems de ordering válidos; con candidateCount<4 **subir n_min o marcar baja-resolución**.
9. **Banco:** 6 fijos. **Mínimo:** ≥30 textos neutros con ≥4 candidatos balanceados en `distortionType` (omission/addition/distortion/accurate), exposure, holdout.
10. **Copy permitido:** "Reconoce el resumen más fiel", "este resumen añade algo que el texto no dice", "discriminas resúmenes fieles" (NO "sabes resumir").
11. **Copy prohibido:** "sabes resumir/sintetizar", "escribes mejor", "comunicación más clara", transferencia a redacción libre, inteligencia.
12. **KMP:** Reusa `ClosedScoringEngine.Ordering` (existe). Forzar `candidateCount≥4` en el generador de banco para niveles altos. Wirear adaptación.
13. **Tests obligatorios:** unit Kendall-tau (ya existe); property (orden inverso ⇒ τ→0); adversarial "orden-aleatorio rinde X"; golden del `COMP_score`; copy-scanner.
14. **Aceptación → APPROVE:** (a) candidateCount≥4 forzado; (b) banco ≥30 balanceado; (c) claim restringido a "reconocimiento de fidelidad"; (d) min-n ajustado a resolución de τ; (e) wiring.
15. **Rediseño:** Mantener pero **mínimo 4 candidatos** y claim estrictamente de reconocimiento. Penalización explícita por adición (inventar) separada de omisión, reportada como sub-señal metacognitiva interna.
16. **Impacto:** Medio (limitado por ser cerrado: no entrena producción).
17. **Prioridad:** P2.
18. **Riesgo residual:** El drag introduce destreza motora; Kendall-tau con pocos candidatos sigue siendo gruesa aunque suba a 4. No confundir nunca con "habilidad de síntesis".

---

## E4 · Argument X-Ray (`argument_xray`) — **REBUILD**

1. **Veredicto:** REBUILD — **el mecanismo del diseño (etiquetar cada oración por rol PREMISA/CONCLUSIÓN/IRRELEVANTE + juicio de suficiencia) NO existe**: en código es `single(...)` = un single-choice "¿cuál es el eslabón más débil?". Mide otra cosa (detección de gap falaz), más estrecho y fácilmente memorizable.
2. **Constructo (real hoy):** Detección del eslabón inferencial débil en un argumento corto (`ARG`). **Constructo diseñado (ausente):** mapeo completo de estructura argumental por oración.
3. **Señal primaria válida:** Accuracy de single-choice (correcto = "b" identifica el salto no garantizado). Implementado, pero es 1 bit por ítem (gruesa) y la respuesta correcta es la opción "b" en los 6 ítems vistos → **patrón posicional explotable**.
4. **Señales secundarias:** `distractorErrors` (ARG_GAP vs ARG_STRAW) — distingue el tipo de error elegido; no agregado a perfil.
5. **Contaminantes:** **Respuesta por posición** (correcto siempre "b"); familiaridad con falacias como conocimiento previo; 1-bit ⇒ alta varianza. `redundantWith`: módulo **Lógica** (`ReasoningScorer.justificationScore`) — riesgo de medir lo mismo en el brain-profile.
6. **Gaming:** "Elige siempre la opción del medio/b" rinde 100% en el banco actual. 6 ítems fijos memorizables. Sin balanceo de posición de la respuesta correcta.
7. **Gates obligatorios:** balanceo de posición de la opción correcta (ausente, crítico); min-n; repetición; fatiga; etiquetas funcionales no técnicas.
8. **Métrica/scoring:** Hoy `correct?1:0`. Diseñado: `0.7·roleScore + 0.3·sufficiency` con `roleScore = aciertos/oraciones`. **n_min** ≥12 single-choice; con 1-bit por ítem el IC es ancho ⇒ agregación inter-sesión obligatoria.
9. **Banco:** 6 fijos, correcto siempre "b". **Mínimo:** ≥40 con posición de respuesta balanceada, ≥20% temas neutros, exposure, holdout; si se rebuildea a labeling, **rúbrica que prohíba ítems con rol ambiguo** (una oración no puede ser premisa-y-conclusión) validada por ≥2 autores (inter-rater de unicidad de rol ≥80%).
10. **Copy permitido:** "¿Dónde está el salto que el texto no justifica?", "afirmación principal" / "razón" / "no aporta" (etiquetas funcionales), "estructura interna del texto, no verdad del mundo".
11. **Copy prohibido:** "pensamiento crítico", "detectas falacias en la vida real", "argumentas mejor", "premisa/conclusión" como jerga obligatoria (medir metalenguaje), inteligencia, far transfer.
12. **KMP:** Para el diseño real haría falta `AnswerKey.RoleLabeling(Map<SentenceId,Role>, sufficiency)` (sellada, **no existe**) + renderer sentence-labeling + `ExerciseCatalogConsistencyTest` que ate renderer↔clave. Sin eso, **renombrar y reclaimar honestamente** como "caza del eslabón débil" (single-choice), no como "radiografía argumental".
13. **Tests obligatorios:** balanceo de posición (la correcta no es siempre "b"); adversarial "siempre-b"; unit por `distractorError`; si labeling: test renderer↔clave + rúbrica de no-ambigüedad; golden; copy-scanner.
14. **Aceptación → APPROVE:** o (A) rebuild a labeling con clave nueva+renderer+test+banco sin ambigüedad; o (B) aceptar versión single-choice "eslabón débil" con banco ≥40 posición-balanceada y claim recortado — y **delimitar formalmente frente a Lógica** en `ExerciseDefinition` para no duplicar varianza.
15. **Rediseño (preferido B sobre A por coste/riesgo):** Quedarse con single-choice "eslabón débil" (renderer probado), banco grande con posición balanceada, distractores ARG_GAP/ARG_STRAW como perfil de error interno. La versión labeling (A) solo si se asume el coste de renderer nuevo + banco sin ambigüedad de rol validado.
16. **Impacto:** Medio (estrecho, 1-bit), bajo si se solapa con Lógica.
17. **Prioridad:** P1 (el patrón posicional "b" es un bug de validez explotable hoy).
18. **Riesgo residual:** Solape con Lógica en brain-profile; etiquetar roles tiene respuestas ambiguas si no hay rúbrica dura; 1-bit es ruidoso.

---

## E5 · Scope & Negation Drill (`scope_negation`) — **REBUILD**

1. **Veredicto:** REBUILD — constructo valioso (alcance de operadores) pero **triple solape** con E1 (Jaccard de selección) y con módulo Lógica (forma lógica), banco fijo de 6, y sin la cuota de ítems "ninguna válida" que el diseño exige para romper el sesgo de adivinanza.
2. **Constructo:** Cálculo del alcance de negación/cuantificador/conector (`SCOPE/NEG`) — qué se sigue *necesariamente* bajo "no/todos/algunos/solo/a menos que".
3. **Señal primaria válida:** `jaccard(selectedValid, requiredValid)` (multi-select cerrada, `EvidenceSelection`/`SingleChoice`). Implementado.
4. **Señales secundarias:** Over-claim (marcar inválidas) vs under-claim (omitir válidas), perfil por `operatorType` (no agregable hoy).
5. **Contaminantes:** **Solape de varianza** con E1 (mismo paradigma Jaccard) y con Lógica NECESSARY_SUFFICIENT; sesgo "siempre hay ≥1 válida" si el banco no incluye casos `validInferenceCount=0`; dependencia fuerte del español. `redundantWith`: **Evidence Lock (E1), módulo Lógica**.
6. **Gaming:** "Marca algo siempre" rinde si nunca hay casos de cero válidas; con 6 ítems fijos, memorización. Heurística por parecido léxico si `surfaceSimilarity` no está alto.
7. **Gates obligatorios:** **cuota ≥20% ítems sin inferencia válida** (rompe la heurística — ausente); min-n por `operatorType`; balance de operadores; RT-mínimo; repetición. Ninguno activo.
8. **Métrica/scoring:** `itemScore = jaccard(selectedValid, requiredValid)` (1.0 solo exacto); `operatorProfile[op]` null si n<8. Considerar **crédito parcial controlado** en alta dificultad (Jaccard exacto frustra). n_min ≥12; agregación inter-sesión por operador.
9. **Banco:** 6 fijos. **Mínimo:** ≥50 balanceado en `operatorType` (≥8/operador), **≥20% con cero válidas**, `surfaceSimilarity` alto en distractores, exposure, holdout, mono-idioma declarado.
10. **Copy permitido:** "¿Qué se sigue necesariamente?", "ninguna de estas se sigue", "alcance de la negación / del cuantificador", "solo español".
11. **Copy prohibido:** "razonamiento lógico", "piensas con más rigor", inteligencia, transferencia a decisiones/vida real, far transfer.
12. **KMP:** Reusa `EvidenceSelection`. **Diferenciar en `ExerciseDefinition`**: E5=alcance del operador, E1=soporte evidencial, Lógica=forma. Validar ortogonalidad EMPÍRICA (no conceptual) antes de contar E1/E5/Lógica como sub-señales separadas en el perfil.
13. **Tests obligatorios:** cuota de "cero válidas" en el banco (test estructural); adversarial "marca-algo-siempre"; unit Jaccard con set vacío requerido; golden; copy-scanner; **test de ortogonalidad** E1↔E5.
14. **Aceptación → APPROVE:** (a) cuota ≥20% sin-válida; (b) banco ≥50 balanceado por operador; (c) delimitación formal vs E1/Lógica + evidencia de ortogonalidad; (d) min-n por operador; (e) mono-idioma declarado.
15. **Rediseño:** Si la ortogonalidad empírica con E1 es baja ⇒ **MERGE con E1** en una sola familia "inferencia soportada y bajo alcance" con sub-perfiles, en vez de dos barras. Si es alta ⇒ mantener con cuota de cero-válidas y banco grande.
16. **Impacto:** Medio; bajo si redunda con E1.
17. **Prioridad:** P1.
18. **Riesgo residual:** Medir lógica formal en vez de procesamiento verbal si los enunciados se notacionalizan; redundancia triple no resuelta sin datos de ortogonalidad.

---

## E6 · Source Triage (`source_triage`) — **BLOCK**

1. **Veredicto:** BLOCK — **riesgo de claim de far-transfer** ("detectar fuentes fiables / fake news") + el mecanismo de ordering/descarte del diseño **no está implementado** (es `single(...)` single-choice) + frontera ética sensible.
2. **Constructo:** Triaje estructural de fuentes (`SRC/CRED`) — aplicar criterios *dados en el ítem* (relevancia, especificidad, contradicción), NO juicio de credibilidad del mundo real.
3. **Señal primaria válida:** Accuracy de selección (single-choice). El Kendall-tau de ordenación que pedía el diseño no se usa aquí.
4. **Señales secundarias:** Tasa de sesgo de autoridad (elegir prestigio irrelevante), discriminación hecho/opinión/inferencia (no capturada).
5. **Contaminantes:** **Conocimiento previo/prestigio real** de las fuentes; "leer la rúbrica del test" en vez de razonar; 1-bit; cultura. `redundantWith`: Evidence Lock (jerarquización de soporte).
6. **Gaming:** "Elige la fuente más específica/relevante" si el `authorityDecoy` no se balancea en ambos sentidos (a veces la prestigiosa SÍ es la mejor — ausente). 6 ítems fijos memorizables.
7. **Gates obligatorios:** `sourceCount≥4` en alto nivel (ausente); decoy de autoridad **bidireccional** (ausente); atributos verificables en el ítem (sin conocimiento externo); min-n; repetición.
8. **Métrica/scoring:** `itemScore = correctSelection?1:0` (o τ si se hace ordering); `authorityBiasRate = nDecoyChosen/nDecoyTrials`. n_min ≥12; agregación inter-sesión.
9. **Banco:** 6 fijos. **Mínimo:** ≥40 con atributos explícitos en el ítem, decoy de autoridad balanceado en ambos sentidos, ≥4 fuentes en alto nivel, exposure, holdout, temas neutros.
10. **Copy permitido:** "Aplica los criterios dados: relevancia, especificidad, contradicción", "según los datos del ítem", "esta fuente es prestigiosa pero irrelevante aquí".
11. **Copy prohibido (bloquear en tests — P0):** "detecta fake news", "alfabetización mediática", "fuentes fiables en internet", "no te engañan", "pensamiento crítico", cualquier transferencia a desinformación del mundo real, inteligencia.
12. **KMP:** Reusa `Ordering`+`SingleChoice`. **Obligatorio copy-scanner de build para Lenguaje** que bloquee el lexema "fake news"/"desinformación"/"medios" incluso en negaciones. Hoy NO existe scanner para este módulo (solo arena/emotions).
13. **Tests obligatorios:** copy-scanner P0 (far-transfer mediático); decoy bidireccional (test estructural); adversarial "elige-la-seria-siempre" y "ignora-la-seria-siempre"; golden.
14. **Aceptación → APPROVE:** (a) copy-scanner que bloquee far-transfer mediático enganchado a `check`; (b) decoy bidireccional + sourceCount≥4; (c) banco ≥40 atributos-en-ítem; (d) claim restringido a "aplicación de criterios estructurales dados".
15. **Rediseño:** Mantener como **aplicación de criterios estructurales explícitos en el ítem** (no credibilidad real), con decoy de autoridad bidireccional y copy blindado. Si el copy no puede separarse honestamente de "fake news" en producto ⇒ RETIRE (no vale el riesgo regulatorio).
16. **Impacto:** Medio dentro del entrenamiento; **alto riesgo** fuera de él (claim).
17. **Prioridad:** P0 (riesgo de claim, no solo de validez).
18. **Riesgo residual:** Aunque el copy sea perfecto, el usuario interpreta "fuentes" como mundo real; banco artificial enseña a leer la rúbrica.

---

## Sesión mixta (`mixed_session`) — **BLOCK** (estructural)

1. **Veredicto:** BLOCK como contribuyente de perfil; aceptable solo como variedad de práctica sin actualizar perfil.
2. **Constructo:** Ninguno propio — toma `items.first()` de cada ejercicio (`primaryTrait = TRANS`). No mide un constructo, mezcla 6.
3. **Señal primaria válida:** Ninguna agregable: 1 ítem/ejercicio ⇒ n=1 por trait ⇒ siempre por debajo de cualquier min-n.
4. **Señales secundarias:** Variedad/desirable-difficulty (Bjork) — beneficio pedagógico real, no métrico.
5. **Contaminantes:** Memorización trivial (siempre el mismo primer ítem de cada banco).
6. **Gaming:** Trivial: 6 respuestas fijas memorizadas.
7. **Gates:** Debe forzar `isStrongUpdateAllowed=false` siempre (n=1 por celda).
8. **Métrica:** No produce score de perfil; solo feedback de práctica.
9. **Banco:** Hereda los 6 primeros ítems fijos — el más memorizable de todos.
10. **Copy permitido:** "Ronda corta que alterna habilidades", "práctica mixta".
11. **Copy prohibido:** Cualquier "perfil"/"medición"/score derivado de la mixta.
12. **KMP:** Marcar explícitamente como no-profile-updating en el holder.
13. **Tests:** test de que la mixta nunca eleva un trait a EMERGING/ESTABLISHED.
14. **Aceptación:** Que rote ítems (no `.first()`) y nunca actualice perfil.
15. **Rediseño:** Rotar 1 ítem aleatorio no-visto por ejercicio; etiquetar como práctica pura.
16. **Impacto:** Bajo (variedad).
17. **Prioridad:** P3.
18. **Riesgo residual:** Que su accuracy se filtre al perfil global.

---

## E7 · Lexical Access Sprint — **RETIRE** (no existe en código; RT-gated sin timebase)

1. **Veredicto:** RETIRE del catálogo de Lenguaje hasta tener timebase autoritativa. **No existe en código** (cero `deadline`, anti-anticipación, `ContainImpulsivity`, choice LEFT/RIGHT en Lenguaje). El módulo Lenguaje **no mide RT físico** (ground-truth: `language` no captura RT por reloj; el holder usa `tick` de orden).
2. **Constructo:** Acceso léxico-semántico rápido bajo deadline — pero es RT-gated, y sin timebase frame-accurate todo RT es ruido cross-device.
3. **Señal primaria válida:** Ninguna válida hoy: IES/RT requieren `ANDROID_UI_UPTIME_NANOS` (Choreographer+MotionEvent), que solo existe en **Speed/Android**, no en Lenguaje. Desktop/iOS de Speed ya son fallback no-autoritativo; Lenguaje no tiene ni eso.
4. **Señales secundarias:** N/A.
5. **Contaminantes:** Jitter de frame, jank no medido (`frameDropFraction=0.0`/`criticalFrameJitter=false` están **hardcodeados** en todo el sistema), wall-clock. `redundantWith`: **módulo Speed** (sería RT genérico repintado de verbal).
6. **Gaming:** Tapping rápido sin leer (sin anti-anticipación real), machacar la mitad.
7. **Gates obligatorios (todos ausentes):** **UNTRUSTED_TIMEBASE/device_unreliable** ⇒ no actualizar perfil; anti-anticipación (RT<antMin); `ContainImpulsivity`; RT solo sobre HITs; MAD solo para tendencia central.
8. **Métrica/scoring:** `IES=RTmed/acc` (null, nunca 0), `confident=(n≥nMin && timebaseTrusted)`. **Sin timebase confiable ⇒ siempre null/preliminary, jamás actualiza perfil.**
9. **Banco:** Inexistente. Requeriría palabras/pseudopalabras con frecuencia léxica del español **calibrada empíricamente** (no subjetiva), `semanticDistance` validado.
10. **Copy permitido (si alguna vez):** "acceso a significados entrenados", "velocidad útil, no impulsividad", "no actualizado por calidad de medición" cuando el dispositivo no es fiable.
11. **Copy prohibido:** "velocidad lectora", "lees más rápido", "procesas más rápido en general", "vocabulario más amplio", inteligencia.
12. **KMP:** Exigiría portar el timebase de Speed (expect/actual Choreographer) + medición REAL de jank que alimente el gate (hoy inexistente en todo MindBoost) + mapear a `CognitiveDimension` **verbal**, no speed.
13. **Tests:** timebase trusted/untrusted ⇒ profile-update on/off; anti-anticipación; adversarial tapping; jank real (requiere device).
14. **Aceptación → reconsiderar:** Integrar timebase autoritativa + medición de jank real + gate device_unreliable + banco de frecuencia calibrado. Sin los cuatro, permanece RETIRE.
15. **Rediseño:** No reconstruir dentro de Lenguaje hasta que exista timebase compartida; si el objetivo es velocidad, **vive en Speed**, no en Lenguaje (no duplicar varianza RT en el perfil).
16. **Impacto:** N/A (no existe).
17. **Prioridad:** P3 (no construir ahora).
18. **Riesgo residual:** Si se construye sin gate, premia tapping y miente con RT cross-device.

---

## E8 · Calibrated Comprehension — **RETIRE** (no existe en código; Brier no estimable con n de sesión)

1. **Veredicto:** RETIRE como ejercicio independiente hasta tener agregación inter-sesión de Brier. **No existe en código** (cero `Brier`, cero selector de confianza en Lenguaje). El selector de confianza solo existe conceptualmente en Lógica (`LogicConfidence`).
2. **Constructo:** Metacognición de la comprensión — calibración confianza↔acierto (Brier).
3. **Señal primaria válida:** Accuracy de inferencia (comprensión, sí estimable). **Calibración (Brier): NO estimable con 6–10 ítems/sesión** — IC ancho, reportar ruido como metacognición es deshonesto.
4. **Señales secundarias:** Over/under-confidence index, resolución metacognitiva — todas requieren n alto acumulado.
5. **Contaminantes:** Confianza degenerada (siempre MED) gamea el Brier; doble objetivo (comprensión+calibración) confunde qué se mide; prior knowledge.
6. **Gaming:** Marcar siempre MED minimiza el Brier sin metacognición real (necesita penalización de confianza degenerada — inexistente).
7. **Gates obligatorios:** **n_min agregado inter-sesión** para `META_score` (no por sesión); anti-degeneración de confianza (varianza mínima); confianza obligatoria pre-feedback; staircase NO controlado por Brier.
8. **Métrica/scoring:** `brier_i=(conf−correct)²`, `calibrationScore=1−mean(brier)`, **null si n acumulado < umbral alto (≥40–60 juicios)**; `COMPREHENSION_score=acc` separado de `META_score`. Dos barras null honestas, nunca mezcladas en un número.
9. **Banco:** Inexistente. Requeriría textos+preguntas de inferencia con `distractorPlausibility` controlado y casos de ambigüedad legítima (confianza calibrada = MED).
10. **Copy permitido:** "acertaste con baja confianza / fallaste con alta confianza", "datos insuficientes para tu calibración", "contra tu baseline" (nunca culpabilizante).
11. **Copy prohibido:** "sabes lo que sabes", "metacognición general", "mejor juicio en la vida", "mejora tu comprensión", inteligencia, far transfer.
12. **KMP:** Reusaría `SingleChoice` + selector de confianza (patrón Lógica) + agregación Brier propia en commonMain con persistencia inter-sesión. Brain-profile reporta META coarse separada, CALIBRATING→em-dash.
13. **Tests:** anti-degeneración (siempre-MED ⇒ META anulado); n_min agregado ⇒ barra null honesta; separación comprensión/calibración; golden Brier; copy-scanner.
14. **Aceptación → reconsiderar:** Agregación inter-sesión de Brier con umbral n alto + test anti-degeneración + separación de barras. Sin agregación, RETIRE (Brier de sesión es ruido).
15. **Rediseño:** En vez de ejercicio independiente, **MERGE la calibración como capa metacognitiva opcional sobre E1/E8-comprensión** (declarar confianza tras responder), agregada inter-sesión, sin barra hasta n suficiente. Reduce superficie y evita "doble objetivo confuso".
16. **Impacto:** Medio si se acumula bien; N/A hoy.
17. **Prioridad:** P3 (no construir como standalone; considerar capa).
18. **Riesgo residual:** Reportar Brier prematuro como "metacognición"; entrenar a jugar el Brier.

---

## Hallazgos transversales del módulo (vinculantes)

- **Wiring muerto:** `LanguageSessionStateHolder` no invoca `AdaptationEngine.selectNext` ni `NearTransferVerifier.verify` (existen, sin usar), usa `ScoringTiming()` sin RT y `ScoringContext` sin confounders/fatiga ⇒ **las flags `HIGH_PRIOR_KNOWLEDGE/REPEATED_ITEM/HIGH_FATIGUE/VARIANT_BIAS_RISK` nunca se elevan** y el peso de evidencia nunca se anula por calidad. La arquitectura de honestidad está, pero ciega. **Bloqueante para todo el módulo.**
- **Bancos:** 36 ítems fijos (6×6), orden fijo, sin shuffle, sin reposición, sin `exposure`, sin holdout activo, sin inter-rater de unicidad de clave, sin temas neutros controlados. Memorizables en 1–2 sesiones. **Sin banco grande rotado, ningún ejercicio puede actualizar perfil honestamente.**
- **Copy-scanner ausente:** no hay gate de lexemas prohibidos para Lenguaje (solo arena/emotions). Source Triage especialmente necesita bloquear "fake news"/"desinformación"/"medios" incluso en negaciones. **P0.**
- **Redundancia:** E1↔E5 (Jaccard de selección) y E4↔Lógica (estructura argumental) deben validarse por **ortogonalidad empírica** antes de contarse como sub-señales separadas en el brain-profile; si no, MERGE.
- **Mecánicas degradadas vs diseño:** E4 (single-choice, no labeling), E6 (single-choice, no ordering), E2 (sin SpanLocalization). El catálogo vende 8 mecánicas; el código ejecuta 6 reducidas. Honestidad exige reclaimar o reconstruir, no mantener el doc.