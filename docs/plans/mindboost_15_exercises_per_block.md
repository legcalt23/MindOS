# MindBoost — 15 Exercises Per Cognitive Block

**Versión:** 1.0 · **Fecha:** 2026-07-02 · **Alcance:** sistema de entrenamiento cognitivo.
**Documento hermano de** [mindboost_cognitive_training_master_plan.md](mindboost_cognitive_training_master_plan.md) (§8 = rediseño de motor por bloque; este doc = las fichas de ejercicio) · [scoring](mindboost_scoring_system_v1.md) · [guardrails](mindboost_science_and_claims_guardrails.md).

Este documento contiene **123 ejercicios diseñados** (15+ por bloque) con la plantilla estándar de 27 campos. El subconjunto **MVP = 41** (Enfoque 6 · Decisiones 5 · Lógica 5 · Creatividad 4 · Lenguaje 6 · Emociones 5 · Velocidad 5 · Memoria 5). Cada ejercicio distingue **diseñado** (spec completa aquí) de **implementado** (código en el repo): 15/bloque es la meta de diseño, no de implementación en el MVP (decisión CEO DC-19).

## 1. Design Criteria

Todo ejercicio de este documento cumple los criterios del panel /mb (memo de decisiones, secciones D1–D14 + A1–A18):

1. **Hipótesis cognitiva anclada a un paradigma reconocible** de la literatura (PVT, flanker, stop-signal/SSRT, Stroop/Simon, task-switching, complex span, n-back/updating, recuerdo diferido, Posner cueing, SART, matrices/series, silogismos, muestreo de información/parada óptima, Brier/calibración, usos alternativos, RAT/asociación remota, decisión léxica, DSST…), adaptado sin copiar tests con copyright ni afirmar localizaciones cerebrales.
2. **10 elementos obligatorios por ejercicio:** hipótesis cognitiva, habilidad entrenada, métrica observable, riesgo de medición, señales de gaming, criterio de progresión, criterio de fallo, feedback, nivel de confianza y claim permitido — materializados en la plantilla de 27 campos.
3. **Scoring honesto (DC-1):** `normalizedScore` 0–1 **o null** — nunca 0 fabricado; el score de usuario 0–100 es vista derivada gateada (DC-2). Emociones y Creatividad sin número absoluto en MVP.
4. **Anti-gaming concreto (DC-13/§13):** cada ficha nombra la señal de gaming y el detector/gate que la neutraliza (referencia `core/antigaming` D1–D11 o detector nuevo especificado con su par red-team); nunca se reclama `DetectorCoverage.ADEQUATE` sin suite adversarial.
5. **Anti-memorización de banco:** generación paramétrica (structureId × surfaceVariant), ventana de no-repetición, holdout de retest, `BankValidator`/`BankIntegrity` antes de alimentar el perfil; ítems versionados (`bankVersion`).
6. **Claim único (DC-20/A18):** formato "Entrenas y mides X dentro de MindBoost contra tu propia línea base"; prohibidos transferencia a la vida real, IQ, rasgos, lecturas clínicas y percentiles poblacionales; el copy pasa el lexicon prohibido del scanner de build.
7. **Persistencia + versionado:** cada ejercicio persiste su resumen cifrado + `SessionRecord` normalizado (STARTED→CHECKPOINT→FINALIZED/ABANDONED, DC-16); un abandono jamás alimenta el perfil.
8. **Duración MVP** en el rango 120 s–10 min; dificultad usuario 1–10; feedback serio (informe de rendimiento, sin XP ni celebraciones vacías).

## 2. Exercise Matrix

Matriz de los 123 ejercicios (bloque · # · ejercicio · id · habilidad · estado · apto MVP · prioridad). El detalle completo de cada uno está en su ficha en las secciones 3–10.

| Bloque | # | Ejercicio | ID | Habilidad | Estado | Apto MVP | Prioridad |
|---|---:|---|---|---|---|---|---|
| Enfoque | 1 | Vigía | `E1_VIGIA` | Vigilancia sostenida (SUSTAINED_VIGILANCE) | Mejorar | Sí | P1 |
| Enfoque | 2 | Filtro | `E2_FILTRO` | Filtrado selectivo (SELECTIVE_FILTERING) | Mejorar | Sí | P1 |
| Enfoque | 3 | Gatillo | `E3_GATILLO` | Inhibición de respuesta (RESPONSE_INHIBITION, vía prepotencia) | Mejorar | Sí | P1 |
| Enfoque | 4 | Freno | `E4_FRENO` | Inhibición reactiva (RESPONSE_INHIBITION, cancelación) | Mejorar | Sí | P1 |
| Enfoque | 5 | Conflicto | `E5_CONFLICTO` | Control de interferencia (INTERFERENCE_CONTROL) | Mejorar | Sí | P1 |
| Enfoque | 6 | Cambio | `E6_CAMBIO` | Flexibilidad controlada (CONTROLLED_FLEXIBILITY) | Mejorar | Sí | P1 |
| Enfoque | 7 | Rastreo | `E7_RASTREO` | Búsqueda visual eficiente (SELECTIVE_FILTERING espacial) | Mantener | Con cambios (queda Post-MVP en modo libre) | P2 |
| Enfoque | 8 | Dúo | `E8_DUO` | Integración dual-task (DUAL_TASK_INTEGRATION) | Rehacer | No (Post-MVP) | P1 |
| Enfoque | 9 | Enjambre | `E9_ENJAMBRE` | Atención dividida espacial dinámica (MOT) | Mejorar | Con cambios (queda Post-MVP en modo libre) | P1 |
| Enfoque | 10 | Flujo | `E10_FLUJO` | Atención temporal (TEMPORAL_ATTENTION) | Mantener | Con cambios (queda Post-MVP en modo libre) | P2 |
| Enfoque | 11 | Ancla | `E11_ANCLA` | Resistencia a la distracción (DISTRACTION_RESISTANCE) | Mantener | Con cambios (queda Post-MVP en modo libre) | P2 |
| Enfoque | 12 | Re-enganche | `E12_REENGANCHE` | Recuperación post-error (TEMPORAL_ATTENTION / control dinámico) | Mejorar | Con cambios (queda Post-MVP en modo libre) | P2 |
| Enfoque | 13 | Presión | `E13_PRESION` | Precisión bajo presión temporal (PRECISION_UNDER_PRESSURE) | Mantener | Con cambios (queda Post-MVP en modo libre) | P2 |
| Enfoque | 14 | Faro | `ENF-14` | Orientación atencional encubierta (SELECTIVE_FILTERING espacial anticipatoria) | Nuevo (spec de diseño, sin código; propuesto por panel science) | No (Post-MVP) | P3 |
| Enfoque | 15 | Corriente | `ENF-15` | Atención sostenida a la respuesta (SUSTAINED_VIGILANCE bajo automatismo) | Nuevo (spec de diseño, sin código; propuesto por panel science) | No (Post-MVP) | P3 |
| Decisiones | 1 | Reversibility Triage — Puerta de Una Vía vs Dos Vías | `` | Discriminación riesgo/recompensa — clasificación reversible (Type-2) vs irreversible (Type-1) | Mejorar (`E2_REVERSIBILIDAD`, código completo oculto por flag; cambio concreto: validar banco κ≥0.75 → cablear `ReversibilityScenarioBank` al generador → retirar `SMALL_UNVALIDATED` → `includeReversibility=true`) | Con cambios (gate humano de validación) | P0 |
| Decisiones | 2 | Bayes Arena — Calibrador de Posterior | `` | Actualización bayesiana de creencias / juicio bajo incertidumbre | Mejorar (`decisiones.bayesArena`; cambio concreto: `BayesArenaProfileAdapter`→`ScoreResult`, exclusión priori≈posterior del BUR, `movedFromDefault`+entropía de dial reales, 3 niveles de dificultad reales, min-n en `finish()`) | Con cambios | P0 |
| Decisiones | 3 | DEC-03 — Muestreo de Información con Coste | `` | Parada óptima de recolección de información (valor de la información) | Nuevo (fuente: rediseño de E8 "Resolución bajo Ambigüedad" de `03_6_Decisiones.md`, con las 5 correcciones de su auditoría) | Sí (destino MVP) | P1 |
| Decisiones | 4 | DEC-04 — Priorización por Dominancia | `` | Priorización / integración multiatributo con criterio objetivo | Nuevo (fuente: rediseño de E5 "Escáner de Alternativas" de `03_6_Decisiones.md`, reencuadrado a frontera objetiva por su auditoría: elección dominada = error) | Sí (destino MVP) | P1 |
| Decisiones | 5 | DEC-05 — Aceptar/Rechazar con Deadline | `` | Colocación de criterio de decisión bajo costes asimétricos y presión temporal | Nuevo (fijado por panel A10-D5; reutiliza `ChoiceResponseLayer` extraído, A11) | Sí (destino MVP) | P1 |
| Decisiones | 6 | DEC-06 — Calibración de Confianza | `` | Calibración metacognitiva confianza↔acierto | Post-MVP (Nuevo — diseño sin código; fuente: E3 "Forecast Calibrado" de `03_6_Decisiones.md`, renombrado y purgado de léxico prohibido según su auditoría) | No | P2 |
| Decisiones | 7 | DEC-07 — Resistencia a Framing | `` | Consistencia de elección ante marcos ganancia/pérdida equivalentes | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) | No | P2 |
| Decisiones | 8 | DEC-08 — Resistencia a Anclaje | `` | Independencia de la estimación respecto de anclas irrelevantes | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) | No | P2 |
| Decisiones | 9 | DEC-09 — Descuento Temporal | `` | Consistencia en elecciones intertemporales (antes↔después) | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science con reserva fuerte) | No | P3 |
| Decisiones | 10 | DEC-10 — Resistencia a Coste Hundido | `` | Decidir por valor futuro esperado ignorando inversión irrecuperable | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) | No | P2 |
| Decisiones | 11 | DEC-11 — Exploración-Explotación | `` | Equilibrio explorar↔explotar bajo retornos inciertos | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science: bandit simple) | No | P2 |
| Decisiones | 12 | DEC-12 — Juicio Proceso-vs-Resultado | `` | Evaluar decisiones por su proceso ex-ante, no por su resultado | Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) | No | P3 |
| Decisiones | 13 | DEC-13 — Presupuesto de Decisión | `` | Optimización de asignación bajo restricciones múltiples | Post-MVP (Nuevo — diseño sin código; fuente: E7 de `03_6_Decisiones.md` con las 5 correcciones de su auditoría) | No | P3 |
| Decisiones | 14 | DEC-14 — Caza-Sesgos | `` | Identificación metacognitiva del patrón de sesgo plantado en un ítem | Post-MVP (Nuevo — diseño sin código; fuente: E6 de `03_6_Decisiones.md`; su auditoría lo deja en BLOCK con caso débil — se conserva como diseño, candidato a retirada) | No | P3 |
| Decisiones | 15 | DEC-15 — Riesgo-Recompensa con Valor Esperado | `` | Elección bajo riesgo descrito con criterio de dominancia estocástica | Post-MVP (Nuevo — diseño sin código; cubre la subhabilidad riesgo/recompensa del PRD con criterio objetivo) | No | P2 |
| Lógica | 1 | Motor Deductivo Adaptativo | `wizard` | Razonamiento deductivo proposicional (inferencia necesaria) | Mejorar (banco paramétrico ≥30 ítems/familia con no-repetición y holdout; dificultad calculada APLICADA al contenido servido; ≥2 probes isomorfos/sesión; adapter a `ScoreResult`) | Con cambios | P0 |
| Lógica | 2 | Matrices y Series Paramétricas | `` | Razonamiento inductivo-abstracto (inducción y extrapolación de reglas sobre patrones) | Nuevo (LOG-02; memo A9-L2; panel science: reglas componibles tipo Carpenter con ítems PROPIOS, jamás Raven/CRT publicados) | Sí | P0 |
| Lógica | 3 | Ordena la Derivación | `tap-order` | Estructura de prueba (dependencia lógica secuencial, no-circularidad) | Nuevo (LOG-03; memo A9-L3; reutiliza diseño 03_3 §8 "Ordenar la Derivación" con las correcciones que su audit exigía: TAP-ORDER en lugar de drag [elimina ruido motor], validación contra el DAG y no contra una permutación fija) | Sí | P0 |
| Lógica | 4 | Rejilla de Restricciones | `` | Deducción multi-restricción (integración de constraints para determinar asignaciones únicas) | Nuevo (LOG-04; memo A9-L4; panel science "deducción por rejilla de restricciones"; GridField compartido NUEVO en `core/paradigm/ui` — A11, reutilizable por Memoria/Corsi) | Sí | P1 |
| Lógica | 5 | Caza-Contraejemplo Bajo Presión | `` | Falsación (construir/reconocer el caso que invalida una regla) | Nuevo (LOG-05; memo A9-L5; reutiliza diseño 03_3 §4 "Caza-Contraejemplo" [audit BLOCK] corrigiendo su bloqueante: generador validado por `CounterexampleValidator` en construcción + penalización explícita de near-miss; deadline reusa el patrón E13 de presión temporal de Enfoque sobre ChoiceResponseLayer) | Sí | P1 |
| Lógica | 6 | Tarea de Selección Condicional | `Wason paramétrico` | Comprobación de reglas condicionales (qué evidencia es necesaria para testear "si P entonces Q") | Post-MVP (LOG-06; panel science; ítems propios paramétricos, nunca la formulación publicada literal) | No | P2 |
| Lógica | 7 | Silogismo Categórico anti Sesgo de Creencia | `` | Juicio de validez formal independiente del contenido (control del sesgo de creencia) | Post-MVP (LOG-07; reutiliza diseño 03_3 §5 [audit REVISE]: belief-bias index solo agregado históricamente, paridad de contenido congruente/incongruente por generador, n/confident reportados) | No | P2 |
| Lógica | 8 | Detector de Contradicción Latente | `conjuntos` | Detección de consistencia de un conjunto de afirmaciones | Post-MVP (LOG-08; reutiliza diseño 03_3 §2 [audit REVISE]: cap 5 afirmaciones, unicidad del núcleo mínimo garantizada por generador, d′ solo agregado histórico; evoluciona la familia CONTRADICTION_DETECTION del motor L1 a ejercicio autónomo de conjuntos) | No | P2 |
| Lógica | 9 | Inducción de Reglas | `2-4-6` | Inducción activa de reglas (generar y testear hipótesis) | Post-MVP (LOG-09; panel science "inducción de reglas, paradigma 2-4-6"; diseño nuevo) | No | P2 |
| Lógica | 10 | Inferencia Transitiva Encadenada | `` | Inferencia relacional transitiva (integrar relaciones de orden A>B, B>C → A>C) | Post-MVP (LOG-10; panel science "inferencia transitiva/ordenación"; diseño nuevo) | No | P2 |
| Lógica | 11 | Transferencia Espejo | `gemelo/isomorfo` | Transferencia estructural near (abstraer la forma lógica de la superficie) | Post-MVP (LOG-11; reutiliza diseño 03_3 §7 [audit BLOCK por banco: exige ≥8 estructuras isomórficas ×≥2 superficies]; se desbloquea SOLO cuando el banco paramétrico L1 alcance ese piso; mientras, los probes isomorfos siguen viviendo DENTRO de L1 vía `transferConsistencyFor`) | No | P2 |
| Lógica | 12 | Inferencia Suprimida | `defeasible explícito` | Distinguir deducción monótona de inferencia revisable con condiciones explícitas | Post-MVP (LOG-12; reutiliza diseño 03_3 §9 [audit BLOCK] con SU corrección obligatoria: condiciones habilitadoras DECLARADAS en el enunciado — lógica defeasible explícita, nunca conocimiento del mundo) | No | P3 |
| Lógica | 13 | Analogía Estructural Formal | `` | Mapeo relacional (preservar la relación, no el rasgo superficial) | Post-MVP (LOG-13; reutiliza diseño 03_3 §10 [audit BLOCK riesgo cultural ROJO] bajo su condición de supervivencia: relaciones EXCLUSIVAMENTE formales-abstractas — contención, orden, oposición, causalidad simple — sobre estímulos visuales, cero dependencia léxica) | No | P3 |
| Lógica | 14 | Reparación Lógica | `premisa mínima` | Reparación de argumentos inválidos (identificar la premisa mínima que restaura la validez) | Post-MVP (LOG-14; diseño nuevo sobre la subhabilidad LOGICAL_REPAIR de la taxonomía existente; sustituye a la Inferencia Bayesiana Cualitativa del catálogo previo [03_3 §6, BLOCK→RETIRE], cuyo constructo pasa a Decisiones D2) | No | P3 |
| Lógica | 15 | Suposición Oculta | `entimema` | Identificación de la premisa tácita que un argumento necesita para ser válido | Post-MVP (LOG-15; diseño nuevo sobre HIDDEN_ASSUMPTION/ARGUMENT_ANATOMY de la taxonomía existente) | No | P3 |
| Creatividad | 1 | Usos Alternativos Estructurado | `EX01_SUPPRESS_FIRST_IDEA` | Fluidez ideacional divergente con inhibición de la respuesta prepotente (proxy) | Mejorar | Con cambios | P0 |
| Creatividad | 2 | Rotación Forzada de Categorías en Vivo | `EX02_CATEGORY_ROTATION` | Flexibilidad cognitiva / switching semántico (proxy ideacional) | Rehacer | Con cambios | P0 |
| Creatividad | 3 | Asociación Remota Convergente | `CRE-03` | Asociación remota convergente | Nuevo | Sí | P1 |
| Creatividad | 4 | Consecuencias en Cadena | `CRE-04` | Fluidez de implicaciones (pensamiento hipotético divergente) | Nuevo | Sí | P1 |
| Creatividad | 5 | Rareza Relativa al Banco | `CRE-05` | Exploración de la cola de la distribución de ideas (originalidad APROXIMADA, operacionalizada honestamente) | Nuevo | No (V1.1 — primer post-MVP a construir) | P2 |
| Creatividad | 6 | Elaboración Dialéctica por Capas | `CRE-06` | Elaboración / profundización sostenida de una idea | Nuevo | No | P2 |
| Creatividad | 7 | Síntesis Bajo Restricciones | `CRE-07` | Combinatoria forzada / satisfacción de restricciones múltiples | Nuevo | No | P2 |
| Creatividad | 8 | Reencuadre por Escalas | `CRE-08` | Problem-finding / reencuadre del problema | Nuevo | No | P2 |
| Creatividad | 9 | Mejora de Producto con Restricción Rotativa | `CRE-09` | Ideación aplicada bajo restricción cambiante (mejora iterativa) | Nuevo | No | P2 |
| Creatividad | 10 | Combinación Conceptual Forzada | `CRE-10` | Combinación conceptual (integración de dominios) | Nuevo | No | P2 |
| Creatividad | 11 | Fluidez con Restricción Formal | `CRE-11` | Producción divergente bajo restricción formal dura | Nuevo | No | P2 |
| Creatividad | 12 | Títulos Alternativos | `CRE-12` | Reetiquetado conceptual (síntesis divergente breve) | Nuevo | No | P3 |
| Creatividad | 13 | Analogía Proporcional Estructural | `CRE-13` | Mapeo estructural de relaciones (A:B::C:?) | Nuevo | No | P3 |
| Creatividad | 14 | Anti-Bloqueo Sprint-Recovery | `CRE-14` | Reducción de bloqueo/auto-censura en producción ideacional (reposicionado: NUNCA "creatividad bajo presión" — veredicto BLOCK del comité, `04_8` EX08) | Nuevo | No (BLOQUEADO hasta prerrequisitos) | P3 |
| Creatividad | 15 | Cadena Prohibida Creciente | `CRE-15` | Inhibición ideacional acumulativa (presión inhibitoria creciente intra-trial) | Nuevo | No (modo avanzado de EX01) | P3 |
| Lenguaje | 1 | Evidence Lock (Anclaje evidencial) — `evidence_lock` | `` | Inferencia legítima anclada en evidencia (INF) | Mejorar (wiring persistencia + banco ≥40 posición-balanceado + prior-flag + RT-lectura; mecánica intacta) | Con cambios | P1 |
| Lenguaje | 2 | Semantic Surgery (Disección de significado) — `semantic_surgery` | `` | Precisión léxico-semántica (LEX_P) | Mejorar (banco ≥50 balanceado por `changeType`; claim restringido a equivalencia — sin localización de token en v1) | Con cambios | P1 |
| Lenguaje | 3 | Compression Ladder (Escalera de compresión) — `compression_ladder` | `` | Síntesis por reconocimiento (SUM) | Mejorar (forzar `candidateCount≥4` en niveles altos; banco ≥30 balanceado por `distortionType`; claim de reconocimiento) | Con cambios | P1 |
| Lenguaje | 4 | Argument X-Ray (Radiografía argumental) — `argument_xray` | `` | Análisis argumental verbal (ARG) | Rehacer (banco completo: la correcta es "b" en los 6 ítems — verificado; mecánica single-choice "eslabón débil" se CONSERVA y se re-claima; opción B del audit 04_4 por coste/riesgo) | Con cambios | P1 |
| Lenguaje | 5 | Scope & Negation (Alcance y negación) — `scope_negation` | `` | Procesamiento sintáctico de alcance (SYN) | Mejorar (cuota ≥20% ítems cero-válidas; banco ≥50 balanceado por operador; delimitación frente a evidence_lock/Lógica) | Con cambios | P1 |
| Lenguaje | 6 | Source Triage (Triaje de fuentes) — `source_triage` | `` | Lectura crítica estructural (CRIT) | Mejorar (claim blindado a "criterios dados en el ítem"; decoy de autoridad bidireccional; banco ≥40 con atributos explícitos) | Con cambios | P1 |
| Lenguaje | 7 | LEN-07 · Fluidez semántica cronometrada | `` | Acceso y fluidez léxica (LEX_A) | Nuevo (cubre "rapidez verbal/asociación" del PRD §10.5; sugerido por panel science: "fluidez verbal por letra/categoría (conteo)") | No (Post-MVP) | P2 |
| Lenguaje | 8 | LEN-08 · Vocabulario en contexto | `cloze` | Profundidad léxica en contexto (LEX_D) | Nuevo (cubre "vocabulario" del PRD; panel science: "cloze de completamiento / profundidad de vocabulario") | No (Post-MVP) | P1 |
| Lenguaje | 9 | LEN-09 · Precisión gramatical | `juicio de forma` | Procesamiento sintáctico normativo (SYN) | Nuevo (paradigma de juicio de gramaticalidad, psicolingüística estándar) | No (Post-MVP) | P2 |
| Lenguaje | 10 | LEN-10 · Paráfrasis con equivalencia de conjunto | `` | Paráfrasis receptiva (PAR) | Nuevo (extiende el paradigma de equivalencia a conjuntos; delimitado de semantic_surgery) | No (Post-MVP) | P2 |
| Lenguaje | 11 | LEN-11 · Detección de ambigüedad | `` | Monitorización sintáctico-semántica (SYN/INF) | Nuevo (monitorización de doble lectura; paradigma de ambigüedad estructural/léxica offline) | No (Post-MVP) | P2 |
| Lenguaje | 12 | LEN-12 · Registro y tono | `` | Adecuación pragmática (PRAG) | Nuevo (adecuación pragmática; usa el trait PRAG ya definido en la taxonomía) | No (Post-MVP) | P3 |
| Lenguaje | 13 | LEN-13 · Analogías verbales | `` | Razonamiento relacional verbal (ARG/LEX_D) | Nuevo (ítems propios paramétricos; disciplina estricta de claims — nada de razonamiento general) | No (Post-MVP) | P2 |
| Lenguaje | 14 | LEN-14 · Comprensión con inferencia | `multi-frase` | Modelo de situación / inferencia integrativa (SIT/INF) | Nuevo (inferencia puente a nivel de pasaje; complementa evidence_lock sin anclaje explícito) | No (Post-MVP) | P1 |
| Lenguaje | 15 | LEN-15 · Definiciones precisas | `` | Profundidad léxica definicional (LEX_D) | Nuevo (profundidad definicional con rúbrica género-diferencia) | No (Post-MVP) | P3 |
| Emociones | 1 | Pausa antes de actuar | `` | Inhibición de respuesta bajo distractor afectivo leve (`PAUSE_BEFORE_ACTION`). ID técnico vivo: `EM1_PAUSA` (`EM1PausaGenerator.kt`) | Mejorar | Sí | P2 |
| Emociones | 2 | Recuperación tras error | `` | Recuperación funcional post-error (`POST_ERROR_RECOVERY`). ID vivo: `EM2_RECUPERACION` (`EM2RecuperacionGenerator.kt`) | Mejorar | Sí | P2 |
| Emociones | 3 | Separador de registros | `` | Separación categorial de registros internos (`EMOTION_THOUGHT_SEPARATION`, `GRANULARITY`). ID vivo: `EM3_SEPARADOR` (`EM3SeparadorGenerator.kt`) | Mejorar | Sí | P2 |
| Emociones | 4 | Anti-impulso por comisión | `` | Inhibición pura por comisión (`PAUSE_BEFORE_ACTION`). ID vivo: `EM8_ANTIIMPULSO` (`EM8AntiImpulsoGenerator.kt`) | Mantener | Sí (jugable) | P3 |
| Emociones | 5 | Separación bajo presión leve | `` | Separación categorial con presión temporal leve (`EMOTION_THOUGHT_SEPARATION`). ID vivo: `EM10_SEP_PRESION` (`EM10SepPresionGenerator.kt`) | Mantener | Sí | P2 |
| Emociones | 6 | Continuidad tras feedback negativo neutro | `` | Recuperación post-error con densidad garantizada (`POST_ERROR_RECOVERY`). ID vivo: `EM11_FEEDBACK_NEG` (`EM11ContinuidadGenerator.kt`) | Mantener | Sí | P2 |
| Emociones | 7 | Calibrador intensidad-rendimiento | `` | Calibración metacognitiva prospectiva (`CALIBRATION_ACCURACY`) | Nuevo (catálogo in-code `EM4_CALIBRADOR` + diseño EM-E de 03_7, adaptado a calibración prospectiva por bloque) | No (post-MVP; primero de la cola) | P2 |
| Emociones | 8 | Decisiones bajo contexto afectivo | `` | Decisión basada en evidencia bajo señuelo afectivo leve (`AFFECTIVE_DECISION_QUALITY`) | Nuevo (catálogo in-code `EM5_SESGO_AFECTIVO`; decisión del panel: banco texto 50, reusa renderers propios) | No | P3 |
| Emociones | 9 | Persistencia bajo demora | `` | Persistencia segura ante demora (código: `FRUSTRATION_TOLERANCE`; nombre UI allowlist: "persistencia bajo demora") | Nuevo (catálogo in-code `EM6_TOLERANCIA` + diseño EM-D "Tolerance Hold" de 03_7) | No | P3 |
| Emociones | 10 | Reentrada tras pausa programada | `` | Estabilidad al retomar la tarea (`REGULATION_STABILITY`; nombre UI: "estabilidad al retomar") | Nuevo (catálogo in-code `EM7_REENTRADA`, rediseñado para resolver el problema de n del audit 04_7) | No | P3 |
| Emociones | 11 | Etiquetado funcional rápido | `` | Granularidad de etiquetado funcional (`GRANULARITY`) | Nuevo (catálogo in-code `EM9_ETIQUETADO` + diseño EM-F de 03_7, eje vocabulario funcional) | No | P2 |
| Emociones | 12 | Espera activa | `` | Retención de respuesta durante la espera (`PAUSE_BEFORE_ACTION`) | Nuevo (catálogo in-code `EM12_ESPERA`, rediseñado: gobernante pasa de ANTICIPATIONS RT-críticas a comisión de foreperiodo, robusta sin timebase) | No | P3 |
| Emociones | 13 | Estabilidad bajo interferencia | `` | Control de interferencia afectiva leve (`REGULATION_STABILITY`; nombre UI: "estabilidad de respuesta") | Nuevo (catálogo in-code `EM13_VARIABILIDAD`, rehecho: gobernante pasa de RT_CV a coste de interferencia accuracy-based; fuente diseño EM-G de 03_7) | No | P3 |
| Emociones | 14 | Calibración de confianza por ítem | `` | Calibración confianza-acierto por ítem (`CALIBRATION_ACCURACY`) | Nuevo (catálogo in-code `EM14_CONFIANZA` + diseño EM-E de 03_7; diferenciado de EM4: retrospectivo por ítem con ECE) | No | P3 |
| Emociones | 15 | Recuperación encadenada | `` | Recuperación tras errores consecutivos (`POST_ERROR_RECOVERY`) | Nuevo (catálogo in-code `EM15_RECUP_CADENA`, rediseñado como recuperación tras racha con techo de seguridad) | No | P3 |
| Velocidad mental | 1 | E1 · Destello | `` | tiempo de reacción simple (velocidad de detección-respuesta) | Mejorar | Sí | P0 |
| Velocidad mental | 2 | E6 · Multivía | `` | velocidad de decisión bajo elección forzada (CRT 2-AFC) | Mejorar | Sí | P0 |
| Velocidad mental | 3 | E3 · Cifrado | `` | velocidad de verificación símbolo-valor con carga de memoria de trabajo ligera | Mejorar | Con cambios | P0 |
| Velocidad mental | 4 | E7 · Compuerta | `` | velocidad bajo regla condicional con inversión de respuesta prepotente | Mantener | Sí | P0 |
| Velocidad mental | 5 | VEL-01 · Conmutador | `Cambio de Regla Veloz` | velocidad de reconfiguración de regla (throughput bajo cambio) | Nuevo | Sí (5º MVP) | P0 |
| Velocidad mental | 6 | VEL-02 · Rastreo de Símbolos | `` | velocidad de búsqueda visual (presente/ausente) | Nuevo | No (Post-MVP) | P1 |
| Velocidad mental | 7 | VEL-03 · Gemelos | `` | velocidad de comparación perceptiva (mismo/distinto) | Nuevo | No (Post-MVP) | P1 |
| Velocidad mental | 8 | VEL-04 · Cifrado Paramétrico | `DSST propio` | throughput perceptivo-asociativo bajo mapeo arbitrario | Nuevo | No (Post-MVP) | P1 |
| Velocidad mental | 9 | VEL-05 · Abanico de Hick | `` | escalado de RT con incertidumbre de elección (ley de Hick-Hyman) | Nuevo | No (Post-MVP) | P2 |
| Velocidad mental | 10 | VEL-06 · Palabra Real | `` | velocidad de acceso léxico (decisión léxica) | Nuevo | No (Post-MVP) | P2 |
| Velocidad mental | 11 | VEL-07 · Recuento Relámpago | `` | velocidad de enumeración (subitización/conteo rápido) | Nuevo | No (Post-MVP) | P2 |
| Velocidad mental | 12 | VEL-08 · Patrones Espejo | `` | velocidad de matching de patrones espaciales | Nuevo | No (Post-MVP) | P2 |
| Velocidad mental | 13 | VEL-09 · Cálculo Contrarreloj | `` | velocidad de verificación aritmética simple | Nuevo | No (Post-MVP) | P2 |
| Velocidad mental | 14 | VEL-10 · Freno Exprés | `` | velocidad de go con freno selectivo (go/no-go veloz asimétrico) | Nuevo | No (Post-MVP / avanzado) | P3 |
| Velocidad mental | 15 | VEL-11 · Umbral de Luminancia | `` | velocidad de decisión perceptiva pura (discriminación de luminancia 2-AFC) | Nuevo | No (Post-MVP / avanzado) | P3 |
| Velocidad mental | 16 | VEL-12 · Cadencia | `` | precisión temporal predictiva (coincidence timing), no RT reactivo | Nuevo | No (Post-MVP / avanzado) | P3 |
| Memoria | 1 | Span Complejo | `EX1_SPAN_COMPLEJO` | Memoria de trabajo bajo carga concurrente (`WORKING_MEMORY`) | Mejorar | Con cambios (menores) | P2 |
| Memoria | 2 | Actualización — Running Memory | `EX2_ACTUALIZACION_SET` | Actualización de memoria de trabajo (`WORKING_MEMORY`, paradigma RUNNING_MEMORY) | Nuevo (taxonomía in-code completa, sin generador; fuente: catálogo `MemoryExerciseCatalog.EX2` + diseño "Updating de Ranura" de 03_5_Memoria) | Con cambios (construcción completa) | P1 |
| Memoria | 3 | Lista con Estrategia + Recuerdo Diferido 24 h | `EX3_LISTA_ESTRATEGIA` | Memoria episódica con consolidación (`EPISODIC_MEMORY`, dimensión propia) | Mejorar | Con cambios (recordatorio activo) | P1 |
| Memoria | 4 | Pares Asociados | `EX4_PARES_ASOCIADOS` | Memoria asociativa (`WORKING_MEMORY` blend documentado; SignalKey asociativo propio) | Mantener | Sí | P3 |
| Memoria | 5 | Detección de Cambio — Grid Visoespacial | `EX6_DETECCION_CAMBIO` | Memoria de trabajo visoespacial (capacidad K; contribuye a WORKING_MEMORY como blend documentado; `SignalKey("VISUOSPATIAL_K")` propio para abrir subdimensión en V1.1 con nMin — A18) | Nuevo (taxonomía in-code EX6; fuente: catálogo + diseño "Grid Espacial" 03_5 en su modo change-detection, merge recomendado por 04_5) | Con cambios (construcción; **corte designado del suelo A16 si falta holgura**) | P1 |
| Memoria | 6 | Grid Espacial con Binding | `EX5_GRID_ESPACIAL` | WM visoespacial con binding objeto↔posición (subdimensión visual-espacial cuando exista) | Post-MVP (Nuevo; fuente: catálogo EX5 + diseño 03_5 "Grid Espacial con Binding") | No (V1.1) | P2 |
| Memoria | 7 | Secuencias — Orden Serial | `EX7_SECUENCIAS` | Memoria del orden serial (disociada del item memory) | Post-MVP (Nuevo; fuente: catálogo EX7 + diseño 03_5 "Orden Serial Temporal") | No | P2 |
| Memoria | 8 | Instrucciones Complejas | `EX9_INSTRUCCIONES` | WM ejecutiva aplicada a secuencias de acción (following instructions) | Post-MVP (Nuevo; fuente: catálogo EX9) | No | P3 |
| Memoria | 9 | Prospectiva Embebida | `EX10_PROSPECTIVA` | Memoria prospectiva (recordar ejecutar una intención futura) | Post-MVP (Nuevo; fuente: catálogo EX10) | No | P2 |
| Memoria | 10 | Listas con Interferencia A-B/A-C | `EX11_INTERFERENCIA` | Resistencia a interferencia proactiva/retroactiva | Post-MVP (Nuevo; fuente: catálogo EX11 + núcleo A-B/A-C del diseño premium de EX4 en 03_5) | No | P2 |
| Memoria | 11 | Discriminación Fina | `EX12_DISCRIMINACION_FINA` | Discriminación mnemónica (rechazar señuelos muy similares) | Post-MVP (Nuevo; fuente: catálogo EX12) | No | P2 |
| Memoria | 12 | Reconstrucción Parcial | `EX13_RECONSTRUCCION` | Recuperación constructiva con calibración metamnemónica | Post-MVP (Nuevo; fuente: catálogo EX13) | No (avanzado/modo libre en V1.1+) | P3 |
| Memoria | 13 | Dual-Task Recall | `EX14_DUAL_TASK` | Recuperación bajo carga concurrente (`DUAL_TASK_COST`) | Post-MVP (Nuevo; fuente: catálogo EX14) | No (avanzado/modo libre — solapa varianza con EX1) | P3 |
| Memoria | 14 | Memoria Auditiva Narrativa | `EX15_NARRATIVA_AUDITIVA` | Memoria episódica de material estructurado (quién/qué/orden) | Post-MVP (Nuevo; fuente: catálogo EX15) | No | P3 |
| Memoria | 15 | Nombres y Rostros — Avatares Sintéticos | `EX8_NOMBRES_ROSTROS` | Memoria asociativa cara↔nombre (variante de alto valor de EX4 — merge recomendado por 04_5) | Post-MVP (Nuevo; fuente: catálogo EX8; condición dura de science: avatares sintéticos, NUNCA datasets de caras reales) | No | P3 |
| Memoria | 16 | Calibración de Confianza — Overlay | `EX16_CALIBRACION_CONFIANZA` | Metamemoria (calibración confianza↔acierto) | Post-MVP (Nuevo; fuente: catálogo EX16 — modelado in-code como overlay transversal, no drill) | No | P2 |
| Memoria | 17 | Sonda de Transferencia UNTRAINED | `MEM-17` | Medición de capacidad (no entrena): near-transfer sobre material reservado | Post-MVP (Nuevo; fuente: diseño 03_5 "Sonda UNTRAINED"; motores `ReliableChangeIndex`/`RealImprovementCriterion`/`MemoryBaselineProtocol`/`TransferProbePartition` YA implementados y testeados, DORMANT por decisión — destino V1.1) | No | P1 |

---

## 3. Enfoque — 15 Exercises

**Propósito.** Control atencional ejecutivo medido contra la propia línea base: vigilancia, filtrado, inhibición, interferencia, flexibilidad y regulación bajo carga. Es el bloque más maduro del producto (13 jugables, motor propio con MAD/RT-mediana/SSRT-Verbruggen/RCI): la política aquí es curar, no construir.

**Política de scoring del bloque.** Motor nativo (`DefaultMetricsEngine` + `FocusScore`) INTACTO y congelado con goldens (A5). La homogeneización vive SOLO en frontera: `FocusScoreAdapter` (nuevo, aditivo) lee el `MetricSet` congelado y produce `ScoreResult` con `primarySignal = 0.30·Accuracy + 0.25·Consistencia + 0.15·ControlErrores + 0.15·Dificultad + 0.10·VelocidadGated(acc≥0.80) + 0.05·Progreso` (renormalización si faltan componentes; null si faltan Accuracy o Consistencia — nunca 0 fabricado). Difference-scores solo secundarias (A2). RT absoluto nunca al perfil sin timebase autoritativo. `DetectorCoverage` declarado por detector (E4 PARTIAL; resto NONE→PARTIAL cuando DF1 pase red-team). Usuario: banda cualitativa hasta EMERGING con confidence≥0.55 (A1); solo los 6 MVP cuentan para calibración y score del bloque en v1.

**Selección MVP (6).**

| Ejercicio | Justificación (1 línea) |
|---|---|
| E1 Vigía | Vigilancia PVT: paradigma núcleo del bloque, consenso unánime del panel, sin bloqueantes. |
| E2 Filtro | Filtrado selectivo flanker: cubre la elección L/R y el filtrado con motor de congruencia ya honesto (null si falta un lado). |
| E3 Gatillo | Go/no-go: señal accuracy-based (no RT-crítica) apta para perfil, y su generador alimenta la evaluación inicial (A17). |
| E4 Freno | Stop-signal SSRT: el ejercicio con mejor testing adversarial del proyecto (staircase + gate trustworthy). |
| E5 Conflicto | Interferencia Stroop/Simon: exigido por product; entra con regla anti-doble-conteo de señal con E2. |
| E6 Cambio | Task-switching: flexibilidad controlada, payload que deriva la respuesta (no la expone), consenso unánime. |

**Requisitos de banco de contenido.** Todos los ejercicios son generadores procedurales seed-deterministas (sin banco de ítems textual → sin riesgo de memorización de contenido): requisitos = seed no repetido por usuario-sesión, `exerciseVersion`/`scoringVersion` persistidos en el resumen (D11), y holdout de medición vía `ProbeBank` (probes reservadas, jamás entrenadas) para retest/RCI y la métrica de mejora a 30 días. No aplica `BankValidator` (no hay banco de ítems); la integridad del generador se fija con tests de determinismo + property tests de invariantes (balanceo, jitter mínimo, ≥1 señal por bloque).

---

### Vigía (E1_VIGIA)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Vigilancia sostenida (SUSTAINED_VIGILANCE).
**Subhabilidades:** Estabilidad de la respuesta, resistencia al decremento temporal, control de anticipación.
**Hipótesis cognitiva:** Paradigma PVT/Mackworth: mantener respuesta a una señal rara e impredecible durante periodos largos de "nada" expone lapsos y variabilidad del RT; la práctica reduce lapsos y estabiliza el RT dentro de la tarea (estado intra-sesión, no rasgo).
**Mecánica UX:** Pantalla completa táctil; tocar ante el destello verde (señal), ignorar el violeta (lure); ISI impredecible con jitter; feedback flash 180–320 ms + HUD en vivo.
**Duración recomendada:** 5–10 min (plantillas 5/10; la vigilancia necesita bloques largos).
**Dificultad mínima:** 1. **Dificultad máxima:** 10 (escala = 1+round(9·media de ejes)).
**Variables de dificultad:** Rareza de target 30%→6%, ISI 1200–2600→2200–7000 ms, ventana 1400→950 ms, contraste 1.0→0.4, probabilidad de lure 0→35% (`E1DifficultyMapping`).
**Métricas capturadas:** RT_CV, LAPSES (sobre RT crudos), FALSE_ALARMS, OMISSIONS, ANTICIPATIONS, RT_MEDIAN, VIGILANCE_DECREMENT_SLOPE, SUSTAINED_FOCUS_INDEX, BLOCK_STABILITY — todas con n+confident.
**Scoring específico:** SUSTAINED_FOCUS_INDEX = 100·(1−RT_CV)·(1−lapseRate); componentes confident al FocusScore; slope de decremento null si n_targets<20 (regla nueva en adapter).
**Cómo afecta al score del bloque:** Alimenta Accuracy/Consistencia/ControlErrores del composite del adapter; es la fuente principal de la señal dominante Consistencia.
**Criterio de progresión:** Banda de accuracy 0.78–0.85 sobre targets; StepUp del `DefaultAdaptiveEngine` (histéresis + anti-oscilación) baja rareza/sube monotonía.
**Criterio de fallo:** Racha de omisiones (desenganche), FA desbordadas → StepDown/Cooldown; sesión <120 s o abandono → inválida (tombstone ABANDONED).
**Anti-gaming:** Señal: tap rítmico equiespaciado desacoplado del onset infla el índice. Neutraliza: **DF1** detector de cadencia (CV de inter-tap-intervals vs inter-onset → CONTAMINATED; red-team obligatorio) + filtro de anticipación existente + jitter mínimo ≥150 ms verificado por property test.
**Persistencia necesaria:** `SessionSummaryDto` cifrado (namespace `mindboost_focus`, existente) + SessionRecord write-ahead A12 (STARTED→CHECKPOINT por bloque→FINALIZED; tombstone en abort) + exerciseVersion/scoringVersion.
**Feedback inmediato:** Flash semántico por trial + HUD aciertos/lapsos/fallos.
**Feedback post-sesión:** FocusSummaryScreen: tarjetas RT_CV/lapsos con banda de incertidumbre y caption honesto.
**Feedback histórico:** FocusProgressScreen + retest RCI sobre probe reservada (mejora 30 d validada).
**Riesgo científico:** Decremento de vigilancia confundido con fatiga; n_targets bajo a dificultad alta hace el slope ruido (→null).
**Riesgo de UX:** Monotonía deliberada puede leerse como aburrimiento; bloques >10 min elevan abandono.
**Riesgo técnico:** Onset por `MonotonicClock` (no vsync): RT absoluto no comparable inter-dispositivo (se mantiene descriptivo).
**Claim permitido:** "Entrenas y mides la estabilidad de tu atención sostenida en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Goldens del generador/métricas verdes; property jitter≥150 ms; DF1 red-team (rítmico dispara/honesto no); slope null con n_targets<20; abandono persiste tombstone.

### Filtro (E2_FILTRO)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Filtrado selectivo (SELECTIVE_FILTERING).
**Subhabilidades:** Supresión de distractores, resolución de conflicto estímulo-respuesta, velocidad con precisión.
**Hipótesis cognitiva:** Paradigma flanker (Eriksen): responder al target central ignorando flancos congruentes/incongruentes; el efecto de congruencia (RT incong−cong) indexa el control de interferencia espacial.
**Mecánica UX:** Mitades táctiles LEFT/RIGHT (`FocusChoiceField`+ChoiceResponseLayer); target central + 2–6 flancos; feedback inmediato.
**Duración recomendada:** 3–5 min (plantilla 5 min).
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Proporción incongruente 20%→60%, nº flancos 2→6, ventana 1500→900 ms (`E2FiltroGenerator`).
**Métricas capturadas:** ACCURACY (por condición), CONGRUENCY_EFFECT (solo HIT-con-RT ambos lados, null honesto), RT_MEDIAN, RT_CV, POST_ERROR_*.
**Scoring específico:** Accuracy en incongruentes gobierna; CONGRUENCY_EFFECT solo se muestra con ≥20/celda (gate en frontera; el MIN_N_CONGRUENCY=3 interno queda congelado y su subida es tarea deliberada V1.1 con goldens).
**Cómo afecta al score del bloque:** Accuracy/Consistencia al composite; la señal de interferencia (compartida con E5) cuenta UNA vez.
**Criterio de progresión:** Banda accuracy ≈0.80; StepUp sube ratio incongruente y flancos.
**Criterio de fallo:** Comisiones impulsivas → ContainImpulsivity; sesión inválida → tombstone.
**Anti-gaming:** Señal: sesgo motor de lado (responder siempre un lado ≈50% gratis) y pre-onset. Neutraliza: balanceo LEFT/RIGHT del generador verificado por property test (nuevo) + detector de sesgo de lado en frontera (ratio de respuestas por lado vs mezcla real → CONTAMINATED, familia D6 entropía de respuesta de `core/antigaming`) + filtro de anticipación.
**Persistencia necesaria:** Igual que E1 (summary cifrado + SessionRecord + versionado).
**Feedback inmediato:** Flash correcto/incorrecto + HUD.
**Feedback post-sesión:** Tarjeta de accuracy por condición; congruencia solo si confident (badge de incertidumbre).
**Feedback histórico:** Progreso de accuracy incongruente; RCI en retest.
**Riesgo científico:** Efecto (~40 ms) por debajo del jitter de frame → el difference-score es descriptivo, jamás primario.
**Riesgo de UX:** Flancos pequeños pueden medir agudeza visual; mantener tamaños >16 dp.
**Riesgo técnico:** Ninguno nuevo (renderer choice probado).
**Claim permitido:** "Entrenas y mides cuánto te frena la información irrelevante en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Property de balanceo L/R; detector de sesgo de lado con par adversario/honesto; congruencia null con <20/celda testeado; goldens verdes.

### Gatillo (E3_GATILLO)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Inhibición de respuesta (RESPONSE_INHIBITION, vía prepotencia).
**Subhabilidades:** Retención de respuesta prepotente, control de impulsividad, recuperación post-error.
**Hipótesis cognitiva:** Go/no-go: alta proporción de "go" crea prepotencia; retener ante el no-go raro mide inhibición de acción. Señal accuracy-based (comisiones/omisiones), robusta sin timebase fino.
**Mecánica UX:** Single-tap pantalla completa; go frecuente / no-go raro; garantiza ≥1 no-go por bloque; feedback inmediato.
**Duración recomendada:** 3–5 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Proporción go 70%→90% (más prepotencia), ventana 900→600 ms (`E3GatilloGenerator`).
**Métricas capturadas:** FALSE_ALARMS, OMISSIONS, RT_MEDIAN, POST_ERROR_SLOWING/ACCURACY, ACCURACY.
**Scoring específico:** ControlErrores del composite = 1−tasa de comisiones normalizada; accuracy con IC Wilson n≥20. (Computar INHIBITORY_CONTROL_INDEX — declarado en MetricKey, hoy no computado — es extensión ADITIVA post-goldens, P2.)
**Cómo afecta al score del bloque:** Fuente principal de ControlErrores; accuracy al componente Precisión. Por ser accuracy-based es la señal de Enfoque menos dependiente de timebase → la más apta para perfil.
**Criterio de progresión:** FA bajas con go-accuracy alta → StepUp (sube prepotencia, acorta ventana).
**Criterio de fallo:** FA en racha → StepDown; abandono → tombstone.
**Anti-gaming:** Señal: "responder a todo" (hit-rate≈1 sin inhibir jamás). Neutraliza: detector nuevo en frontera (omisiones≈0 + FA>umbral → bloque CONTAMINATED, no solo ajuste adaptativo) + gate de impulsividad fast-commission existente (testeado en `DefaultAdaptiveEngineTest`) + DF1 cadencia.
**Persistencia necesaria:** Igual que E1; además su generador se reutiliza en Mode.EVALUATION para la probe de evaluación inicial (A17) — la probe NO persiste como sesión de entrenamiento.
**Feedback inmediato:** Flash por trial; el fallo de inhibición se marca sin culpa.
**Feedback post-sesión:** Comisiones vs omisiones separadas (no mezclar inatención con impulsividad).
**Feedback histórico:** Tendencia de tasa de comisiones; RCI en retest.
**Riesgo científico:** Prepotencia alta convierte omisiones en ruido de lapso; separar conteos (ya se hace).
**Riesgo de UX:** Ventanas de 600 ms a dificultad 10 pueden frustrar; el motor adaptativo ya contiene.
**Riesgo técnico:** Ninguno nuevo.
**Claim permitido:** "Entrenas y mides tu control para no actuar de más en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Detector responder-a-todo con par adversario/honesto; accuracy Wilson n≥20 en frontera; goldens verdes; probe de evaluación no cuenta como sesión.

### Freno (E4_FRENO)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Inhibición reactiva (RESPONSE_INHIBITION, cancelación).
**Subhabilidades:** Cancelación de respuesta iniciada, latencia de freno (SSRT), inhibición proactiva.
**Hipótesis cognitiva:** Stop-signal (Logan/Verbruggen): cancelar una respuesta ya iniciada cuando aparece la señal de stop tras una SSD variable; SSRT por método de integración con staircase online 1-up/1-down → p(respond|signal)≈0.5.
**Mecánica UX:** Single-tap dos fases: GO (responder por defecto); si no respondió dentro de la SSD, cue STOP rojo (color+forma). `StopSignalTracker` en vivo.
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** INHIBITORY_DEMAND/PRESSURE → ratio de stops, SSD inicial y salience del go (`E4FrenoGenerator`).
**Métricas capturadas:** SSRT (SsrtCalculator, integración), p(respond|signal), trustworthy, go-RT mediana/CV, stop-failure rate, SSD usada por trial (`stopSignalDelayMs`).
**Scoring específico:** SSRT solo con `trustworthy=true` (p∈[0.25,0.75]) Y ≥30 stops + convergencia del staircase (gate nuevo en frontera, A2); si no → null con caption "calibrando freno". SSRT es métrica SECUNDARIA (RT-crítica): jamás entra en primarySignal.
**Cómo afecta al score del bloque:** Go-accuracy y estabilidad al composite; SSRT es lectura secundaria mostrada solo confident.
**Criterio de progresión:** SSRT estable + go-RT en banda → StepUp (más stops, menos salience).
**Criterio de fallo:** Staircase roto (p fuera de banda) → bloque no fiable; abandono → tombstone.
**Anti-gaming:** Señal: waiting strategy (ralentizar el GO homogéneamente para esperar el stop mantiene p≈0.5 con SSRT inflado). Neutraliza: gate trustworthy existente (testeado: `ssrtMetric_flagsGaming`, `SimulatorLoopTest.gamerOnStopSignal`) + **DF2** nuevo `goRtInflation = median(goRT bloques con stop) − median(goRT sin stop)` > umbral → trustworthy=false, con par red-team. DetectorCoverage: PARTIAL (declarado por el detector, consultado por el adapter — nunca hardcodeado en el consumidor).
**Persistencia necesaria:** Igual que E1 + p(respond|signal) y nº stops en el summary (auditar convergencia).
**Feedback inmediato:** Freno exitoso reforzado positivamente (no culpa); GO correcto/lento.
**Feedback post-sesión:** SSRT con banda solo si trustworthy; si no, explicación de calibración.
**Feedback histórico:** Tendencia SSRT solo entre sesiones trustworthy del mismo dispositivo.
**Riesgo científico:** SSRT muy sensible a timebase y a estrategia; el valor absoluto no es comparable inter-dispositivo.
**Riesgo de UX:** El stop debe ser inequívoco multicanal (color+forma; añadir posición es mejora P3 por daltonismo).
**Riesgo técnico:** Staircase online depende del holder (dos fases) — ya probado.
**Claim permitido:** "Entrenas y mides la rapidez de tu freno mental en esta tarea contra tu propia línea base."
**Criterios de aceptación:** DF2 implementado+red-team (ralentizador homogéneo dispara/honesto no); gate ≥30 stops testeado; goldens de SsrtCalculator verdes; SSRT null sin excepción cuando !trustworthy.

### Conflicto (E5_CONFLICTO)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Control de interferencia (INTERFERENCE_CONTROL).
**Subhabilidades:** Supresión de dimensión irrelevante, resolución de conflicto dimensional.
**Hipótesis cognitiva:** Conflicto tipo Stroop/Simon: responder a la dimensión relevante cuando la irrelevante empuja la respuesta opuesta; el conflicto es DIMENSIONAL (dentro del estímulo), a diferencia del espacial de E2 (flancos).
**Mecánica UX:** Mitades táctiles LEFT/RIGHT; payload `Conflict` sin flancos; mezcla congruente/incongruente entrelazada.
**Duración recomendada:** 3–5 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Proporción incongruente 25%→65% (`E5ConflictoGenerator`).
**Métricas capturadas:** ACCURACY por condición, CONGRUENCY_EFFECT (null honesto), RT_MEDIAN, IES.
**Scoring específico:** Igual que E2 (mismo motor de congruencia); su señal de interferencia se AGREGA con la de E2 (media) en el adapter — cuenta UNA vez en el bloque hasta demostrar ortogonalidad empírica E2↔E5 (correlación < umbral en datos reales).
**Cómo afecta al score del bloque:** Accuracy/Consistencia al composite; interferencia compartida con E2 (regla anti-doble-conteo, riesgo r4).
**Criterio de progresión:** Banda accuracy ≈0.80; StepUp sube ratio incongruente.
**Criterio de fallo:** Congruencia negativa con accuracy degradada → bloque de mala calidad; abandono → tombstone.
**Anti-gaming:** Señal: sesgo de lado y "responder a la dimensión irrelevante consistente". Neutraliza: mismo detector de sesgo de lado que E2 (familia D6) + accuracy condicionada por congruencia (si acc_incong≈0 con acc_cong alta → estrategia de dimensión irrelevante → CONTAMINATED).
**Persistencia necesaria:** Igual que E1.
**Feedback inmediato:** Flash por trial.
**Feedback post-sesión:** Accuracy por condición; efecto de interferencia solo confident.
**Feedback histórico:** Tendencia compartida "control de interferencia" (E2+E5 agregada).
**Riesgo científico:** Redundancia con E2 (auditoría: posible MERGE); mitigado por regla de señal compartida + validación de ortogonalidad como criterio de separación futura.
**Riesgo de UX:** Distinguibilidad visual del conflicto dimensional debe ser obvia en onboarding del ejercicio.
**Riesgo técnico:** Ninguno nuevo.
**Claim permitido:** "Entrenas y mides tu resistencia a la interferencia en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Regla de agregación E2+E5 testeada en el adapter (la señal cuenta una vez); detector de dimensión irrelevante con par adversario/honesto; goldens verdes.

### Cambio (E6_CAMBIO)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Flexibilidad controlada (CONTROLLED_FLEXIBILITY).
**Subhabilidades:** Reconfiguración de set mental, resistencia a perseveración, aplicación de regla activa.
**Hipótesis cognitiva:** Task-switching con cue: alternar entre regla de color y de forma; el coste de cambio (RT switch−repeat) indexa la reconfiguración. El payload deriva `correctResponse` de (regla, dimensión) — nunca expone la respuesta (aprendizaje 2026-06-18).
**Mecánica UX:** `FocusRuleSwitchField`: forma coloreada + cue de regla visible; mitades táctiles LEFT/RIGHT.
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Frecuencia de cambio de regla 30%→60% con INTERFERENCE (`E6CambioGenerator`).
**Métricas capturadas:** SWITCH_COST (solo HIT-RT ambas condiciones, null honesto), ACCURACY en switch, RT_MEDIAN; nuevo: perseverationRate (requiere `previousRule` aditivo en el trial).
**Scoring específico:** Accuracy en switch gobierna el aporte; SWITCH_COST secundaria con ≥20/celda (frontera); perseverationRate como diagnóstico (error que coincide con la regla anterior ≠ error perceptual).
**Cómo afecta al score del bloque:** Accuracy/Consistencia al composite; switch cost descriptivo.
**Criterio de progresión:** Coste estable + accuracy ≈0.80 → StepUp (más switches).
**Criterio de fallo:** Perseveración creciente → StepDown/Cooldown; abandono → tombstone.
**Anti-gaming:** Señal: ignorar el cue y responder a UNA regla fija (≈50% gratis en la otra). Neutraliza: accuracy condicionada por regla (accuracy≈azar en una regla con alta en la otra → CONTAMINATED, detector en frontera) + balanceo de lado por regla verificado por property test.
**Persistencia necesaria:** Igual que E1 + `previousRule` en el summary por bloque (agregado, no por trial).
**Feedback inmediato:** Flash por trial; cambio de cue marcado visualmente.
**Feedback post-sesión:** Accuracy switch vs repeat; coste solo confident.
**Feedback histórico:** Tendencia de accuracy en switch; RCI en retest.
**Riesgo científico:** CSI implícito largo → mide lectura de cue, no reconfiguración; coste ruidoso en sesiones cortas.
**Riesgo de UX:** El cue debe ser inequívocamente distinto del estímulo (ya diseñado así).
**Riesgo técnico:** `previousRule` es cambio aditivo al `RecordedTrial` — tras goldens, con test de no-regresión.
**Claim permitido:** "Entrenas y mides el coste de alternar entre reglas en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Detector ignorar-cue con par adversario/honesto; property balanceo por regla; `previousRule`/perseverationRate aditivos con goldens intactos; switch cost null con <20/celda.

### Rastreo (E7_RASTREO)

**Bloque:** Enfoque · **Estado:** Mantener · **Prioridad:** P2 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Búsqueda visual eficiente (SELECTIVE_FILTERING espacial).
**Subhabilidades:** Despliegue atencional, discriminación target/distractor, decisión presente/ausente.
**Hipótesis cognitiva:** Búsqueda visual (Treisman/Wolfe): la pendiente RT×set-size distingue búsqueda eficiente (plana) de serial; el generador ya varía setSize intra-bloque (4/mitad/máximo) para ajustar pendiente — técnica correcta.
**Mecánica UX:** Single-tap presente (withhold ausente); array de 8→32 ítems.
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Set size máximo 8→32 con PERCEPTUAL_LOAD; salience del target (`E7RastreoGenerator`).
**Métricas capturadas:** SET_SIZE_SLOPE (≥2 tamaños, n≥6), ACCURACY, RT_MEDIAN por set size.
**Scoring específico:** Accuracy gobierna; pendiente descriptiva. Graduación a métrica mostrada exige: ≥3 set sizes con n≥20, regresión con R² mínimo (`linearSlope`→(slope,R²)), pendientes presente/ausente separadas.
**Cómo afecta al score del bloque:** NO cuenta para calibración/score en v1 (modo libre); al graduar, accuracy al composite.
**Criterio de progresión:** Accuracy ≈0.85 → StepUp (más set size / más similitud).
**Criterio de fallo:** "Ausente" sistemático rápido → bloque de mala calidad.
**Anti-gaming:** Señal: responder "ausente" sin buscar (RT_ausente << esperado por set size aplana la pendiente). Neutraliza: detector "ausente sin buscar" (RT ausente vs banda esperada → CONTAMINATED) — especificado, se implementa al graduar el ejercicio; mientras, la métrica es descriptiva y el riesgo no toca el perfil.
**Persistencia necesaria:** Igual que E1 (summary+SessionRecord).
**Feedback inmediato:** Flash + resaltado breve del target tras responder.
**Feedback post-sesión:** Accuracy y RT por set size (pendiente solo si confident).
**Feedback histórico:** Tendencia de accuracy.
**Riesgo científico:** Pendiente exige varios niveles × n → sesiones largas; ítems pequeños miden agudeza.
**Riesgo de UX:** Arrays densos en pantallas pequeñas; mínimo 16 dp por ítem.
**Riesgo técnico:** Medir jank del Canvas con `FrameTimingProbe` antes de graduar (guidance mobile).
**Claim permitido:** "Entrenas y mides la eficiencia de tu búsqueda visual en esta tarea contra tu propia línea base."
**Criterios de aceptación:** (Para graduar) R²+split presente/ausente+detector ausente-sin-buscar testeados; mientras: sigue alcanzable en registry (test de curación) y jugable sin regresión.

### Dúo (E8_DUO)

**Bloque:** Enfoque · **Estado:** Rehacer · **Prioridad:** P1 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Integración dual-task (DUAL_TASK_INTEGRATION).
**Subhabilidades:** Reparto atencional, mantenimiento concurrente, priorización.
**Hipótesis cognitiva:** Dual-task/PRP: una demanda concurrente degrada la tarea primaria (coste dual). HOY el dominio no evalúa la secundaria: `E8DuoGenerator` emite un flag `dual` y el renderer muestra un contador que NADIE pregunta → ignorarlo es gratis y el "coste" mide distracción pasiva, no coordinación (revisión fijada por CEO).
**Mecánica UX (rehecha):** Trials dual muestran el contador (450 ms/tick, ya existe en `FocusDualTaskField`); al cierre de cada racha dual se inserta un TRIAL DE SONDA del contador (par/impar → mitades táctiles, reusa ChoiceResponseLayer y respeta el runtime una-respuesta-por-trial) → accSecondary real por bloque.
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Proporción dual 40%→75% con DUAL_TASK_LOAD; ventana 1100→800 ms; (nuevo) velocidad del contador.
**Métricas capturadas:** ACCURACY primaria single vs dual, DUAL_TASK_COST, accSecondary (nuevo), taskBalance (nuevo) = min/max de ambas accuracies.
**Scoring específico:** DUAL_TASK_COST solo válido si accSecondary ≥ umbral (>azar 0.5 con margen, p.ej. ≥0.65) — si no, coste=null + bloque CONTAMINATED; ≥20/celda; siempre secundaria (A2). Hasta el rebuild: la UI re-etiqueta la métrica como "precisión con distractor concurrente" y no alimenta score.
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre); tras rebuild, accuracy primaria al composite.
**Criterio de progresión:** Coste bajo CON ambas accuracies sobre umbral → StepUp.
**Criterio de fallo:** Secundaria a azar → bloque inválido (no se premia abandonarla).
**Anti-gaming:** Señal: abandonar la secundaria (ignorar el contador) hace el "coste" trivialmente bajo. Neutraliza: gate de accSecondary + taskBalance (detector nuevo DF-E8, par red-team: perfil que ignora el contador dispara / honesto no). Señal 2: sonda adivinada (50% azar) → umbral con margen + n de sondas mínimo.
**Persistencia necesaria:** Summary + accSecondary/taskBalance nuevos campos versionados (schemaVersion+1).
**Feedback inmediato:** Flash primaria; resultado de la sonda del contador al momento.
**Feedback post-sesión:** Coste dual solo si válido; si secundaria ignorada, explicación honesta.
**Feedback histórico:** Tendencia del coste solo entre bloques válidos.
**Riesgo científico:** "Secundaria evaluada por sonda" NO es dual-task psicofísico estricto — el copy lo declara (proxy).
**Riesgo de UX:** La sonda no debe romper el ritmo; máx. 1 sonda por racha dual.
**Riesgo técnico:** Nuevo tipo de trial de sonda en generador+holder (aditivo, tras goldens; renderer reusa Choice).
**Claim permitido:** "Entrenas y mides cuánto te cuesta repartir la atención en esta tarea (proxy; la multitarea perfecta no existe) contra tu propia línea base."
**Criterios de aceptación:** Sonda implementada (generador+holder+renderer reusado); gate accSecondary con par adversario/honesto; re-etiquetado UI previo al rebuild; goldens del resto del motor intactos.

### Enjambre (E9_ENJAMBRE)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Atención dividida espacial dinámica (MOT).
**Subhabilidades:** Indexación de múltiples objetivos, mantenimiento bajo movimiento, discriminación de sonda.
**Hipótesis cognitiva:** Multiple Object Tracking (Pylyshyn): seguir N targets entre objetos idénticos en movimiento; sonda binaria final ("¿era target?"). HOY p(sonda=target)=targetCount/objectCount≈0.31 → responder "no" siempre rinde ~69% — sesgo de respuesta explotable (revisión fijada por CEO).
**Mecánica UX:** `FocusTrackingField` (Canvas, trayectorias seed-deterministas); marcado inicial de targets, movimiento, sonda final single-tap sí.
**Duración recomendada:** 4–6 min (bloques cortos, tarea exigente).
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** objectCount 6→16 (PERCEPTUAL_LOAD), targetCount 2→5 (DUAL_TASK_LOAD), velocidad 1→3 (DISTRACTOR_SALIENCE) (`E9DifficultyMapping`).
**Métricas capturadas:** ACCURACY de sonda; NUEVO: D_PRIME y criterio c con corrección log-lineal (Hautus): HR_c=(hits+0.5)/(n_señal+1), FAR_c=(fa+0.5)/(n_ruido+1), d′=z(HR_c)−z(FAR_c), c=−0.5·(z(HR_c)+z(FAR_c)); null si n_señal<20 O n_ruido<20 (A2).
**Scoring específico:** (1) Generador: probe BALANCEADO 50/50 estratificado por bloque (mitad de trials sondean target, mitad distractor) — cambio de contenido sancionado, con goldens de determinismo actualizados deliberadamente. (2) D_PRIME como métrica PARALELA con test A/B contra el scoring ACCURACY actual (QA: no reemplazo directo); migración de gobernante a d′ solo tras comparar en goldens.
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre); al graduar, d′ normalizado alimentaría Precisión con guard de frame.
**Criterio de progresión:** Accuracy/d′ alto → StepUp (más targets/velocidad).
**Criterio de fallo:** Accuracy≈azar sostenida → sobrecarga → StepDown.
**Anti-gaming:** Señal: sesgo "responder siempre sí/no" infla accuracy con probe desbalanceado. Neutraliza: **DF3** = probe 50/50 enforced por property test + criterio c fuera de banda → CONTAMINATED + RT de sonda < suelo (adivinar) filtrado (familia D3 fast-guessing). Par red-team: responder-siempre-no (hoy ~69%) debe caer a ≈50% y disparar c.
**Persistencia necesaria:** Summary + d′/c/n_señal/n_ruido versionados (schemaVersion+1).
**Feedback inmediato:** Flash de sonda.
**Feedback post-sesión:** d′ con banda solo confident; accuracy como referencia durante el A/B.
**Feedback histórico:** Tendencia d′ entre bloques con frame limpio.
**Riesgo científico:** Una sonda por trial → convergencia lenta del umbral; d′ inestable con pocos trials (→null).
**Riesgo de UX:** Dispositivos lentos: saltos de animación destruyen la tarea.
**Riesgo técnico:** Frame-rate del Canvas no medido — medir con `FrameTimingProbe` (jank → bloque CONTAMINATED) antes de graduar.
**Claim permitido:** "Entrenas y mides cuántos objetivos puedes seguir a la vez en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Property probe 50/50; d′ log-lineal con golden numérico + null con n<20/20; test A/B d′ vs ACCURACY; red-team responder-siempre-no; goldens del resto intactos.

### Flujo (E10_FLUJO)

**Bloque:** Enfoque · **Estado:** Mantener · **Prioridad:** P2 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Atención temporal (TEMPORAL_ATTENTION).
**Subhabilidades:** Detección en ráfaga rápida, recuperación entre estímulos (régimen T1→T2).
**Hipótesis cognitiva:** RSVP con estructura de attentional blink (T1 precede a T2 con lag corto a dificultad alta — invariante testeado); la detección en ráfaga mide selección temporal. Claim degradado a "detección en ráfaga" hasta medir el SOA real por frame.
**Mecánica UX:** `FocusStreamField` (ráfaga animada por LaunchedEffect); single-tap al detectar el target.
**Duración recomendada:** 3–5 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** TEMPORAL_UNCERTAINTY → lag T1–T2 (1–3 vs 5–8) y velocidad de ráfaga (`E10FlujoGenerator`).
**Métricas capturadas:** ACCURACY de detección, RT_MEDIAN.
**Scoring específico:** Accuracy gobierna; sin métrica de blink dedicada hasta registrar SOA efectivo por frame (graduación).
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre).
**Criterio de progresión:** Accuracy alta → lag más corto/ráfaga más rápida.
**Criterio de fallo:** Accuracy a azar → StepDown.
**Anti-gaming:** Señal: tap rítmico a la cadencia de la ráfaga y adivinación ultra-rápida. Neutraliza: DF1 cadencia + filtro de anticipación + suelo de RT de detección (familia D3).
**Persistencia necesaria:** Summary + SessionRecord.
**Feedback inmediato:** Flash al detectar.
**Feedback post-sesión:** Accuracy de detección con incertidumbre.
**Feedback histórico:** Tendencia de accuracy.
**Riesgo científico:** SOA nominal ≠ SOA real con jank → la dificultad temporal puede ser ilusoria en gama baja.
**Riesgo de UX:** Ráfagas rápidas en fontScale grande pueden recortar estímulos.
**Riesgo técnico:** Instrumentar el composable con FrameTimingProbe para SOA efectivo (criterio de graduación).
**Claim permitido:** "Entrenas y mides la detección de objetivos en un flujo rápido en esta tarea contra tu propia línea base."
**Criterios de aceptación:** (Graduación) SOA efectivo medido + gate de jank; mientras: alcanzable en registry, copy sin "recuperación de atención".

### Ancla (E11_ANCLA)

**Bloque:** Enfoque · **Estado:** Mantener · **Prioridad:** P2 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Resistencia a la distracción (DISTRACTION_RESISTANCE).
**Subhabilidades:** Mantenimiento de tarea principal, supresión de intrusos salientes.
**Hipótesis cognitiva:** Tarea continua L/R con intrusos tipo notificación que deben ignorarse; el índice accuracy(intrusión)/accuracy(baseline) mide la degradación por intrusión — condiciones REALES (`duringIntrusion`), diseño sólido.
**Mecánica UX:** `FocusAnchorField`; elección LEFT/RIGHT continua; intrusos visuales salientes 20%→55%.
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** DISTRACTOR_SALIENCE → tasa y saliencia de intrusos (`E11AnclaGenerator`).
**Métricas capturadas:** DISTRACTION_RESISTANCE_INDEX (ratio coerce[0,1], null si baseline=0 o subset pequeño), ACCURACY por condición, RT_MEDIAN.
**Scoring específico:** Índice con ≥20/celda para mostrarse (frontera); accuracy gobierna.
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre); al graduar, accuracy al composite.
**Criterio de progresión:** Índice estable alto → StepUp (más saliencia).
**Criterio de fallo:** Colapso de accuracy baseline → StepDown.
**Anti-gaming:** Señal: sesgo de lado (misma que E2/E5). Neutraliza: detector de sesgo de lado compartido (familia D6) + balanceo baseline/intrusión verificado por property test.
**Persistencia necesaria:** Summary + SessionRecord.
**Feedback inmediato:** Flash por trial.
**Feedback post-sesión:** Índice de resistencia solo confident, con nota "proxy de práctica".
**Feedback histórico:** Tendencia del índice.
**Riesgo científico:** La intrusión es perceptual, no ecológica — el punto de fuga es el copy ("te distraes menos con el móvil" PROHIBIDO).
**Riesgo de UX:** Intrusos demasiado realistas (iconos de apps) invitarían al claim prohibido; mantener abstractos.
**Riesgo técnico:** Ninguno nuevo.
**Claim permitido:** "Entrenas y mides tu resistencia a intrusos visuales en esta tarea contra tu propia línea base."
**Criterios de aceptación:** (Graduación) nMin≥20/celda + balanceo testeado; copy auditado sin transferencia a dispositivos.

### Re-enganche (E12_REENGANCHE)

**Bloque:** Enfoque · **Estado:** Mejorar · **Prioridad:** P2 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Recuperación post-error (TEMPORAL_ATTENTION / control dinámico).
**Subhabilidades:** Detección del propio lapso, restauración del ritmo, estabilidad post-error.
**Hipótesis cognitiva:** PVT con provocación deliberada de errores (10%→30%): el patrón post-error (slowing/accuracy) mide la recuperación tras el fallo, no el fallo mismo. Producto lo marcó opcional para MVP; queda en libre hasta validar el constructo (recuperación ≠ tolerancia al tedio).
**Mecánica UX:** Single-tap; trials de recuperación marcados tras error provocado.
**Duración recomendada:** 5–8 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** INHIBITORY_DEMAND → tasa de error provocado (`E12ReengancheGenerator`).
**Métricas capturadas:** POST_ERROR_SLOWING, POST_ERROR_ACCURACY (min-n 3 interno; mostrar exige ≥20 por A2); nuevo (graduación): reengagementLatency = nº trials hasta recuperar RT_CV basal, null si <N lapsos.
**Scoring específico:** Post-error como difference-scores → SIEMPRE secundarias; accuracy global gobierna el aporte.
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre).
**Criterio de progresión:** Recuperación rápida estable → StepUp.
**Criterio de fallo:** Racha de omisiones = desenganche total (separado del lapso puntual) → CONTAMINATED, no penaliza score.
**Anti-gaming:** Señal: tap rítmico y "responder a todo" para no generar errores medibles. Neutraliza: DF1 cadencia + detector responder-a-todo compartido con E3; fatiga solo ADAPTA, jamás penaliza (A14).
**Persistencia necesaria:** Summary + SessionRecord.
**Feedback inmediato:** Flash; el error provocado no se culpabiliza.
**Feedback post-sesión:** Recuperación post-error con incertidumbre; copy de estado intra-sesión.
**Feedback histórico:** Tendencia de POST_ERROR_ACCURACY.
**Riesgo científico:** Riesgo de constructo: medir tolerancia al aburrimiento en vez de reenganche; validar latencia↔recuperación de RT_CV antes de graduar.
**Riesgo de UX:** Provocar errores frustra si el copy no lo explica.
**Riesgo técnico:** reengagementLatency requiere ventana post-error en el assembler (aditivo, post-goldens).
**Claim permitido:** "Entrenas y mides cómo te recuperas tras un error en esta tarea contra tu propia línea base."
**Criterios de aceptación:** (Graduación) reengagementLatency null con <N lapsos + separación desenganche/lapso testeadas + evidencia de constructo; mientras: jugable sin regresión.

### Presión (E13_PRESION)

**Bloque:** Enfoque · **Estado:** Mantener · **Prioridad:** P2 · **Apto MVP:** Con cambios (queda Post-MVP en modo libre)

**Habilidad cognitiva principal:** Precisión bajo presión temporal (PRECISION_UNDER_PRESSURE).
**Subhabilidades:** Mantenimiento de precisión con deadline, regulación del trade-off velocidad-error.
**Hipótesis cognitiva:** Interleaving de trials calmados y bajo deadline (800→450 ms): PRESSURE_DELTA = accuracy(calma)−accuracy(deadline) aísla el efecto de la presión temporal con condiciones reales (`underPressure`).
**Mecánica UX:** Elección LEFT/RIGHT; el holder aplica el deadline como ventana efectiva solo en trials bajo presión.
**Duración recomendada:** 3–5 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** PRESSURE → proporción de deadline 40%→60% y deadline 800→450 ms (`E13PresionGenerator`).
**Métricas capturadas:** PRESSURE_DELTA (null si falta condición), ACCURACY por condición, RT_MEDIAN, IES.
**Scoring específico:** Accuracy por condición gobierna; delta secundaria con ≥20/celda (frontera).
**Cómo afecta al score del bloque:** NO cuenta en v1 (modo libre).
**Criterio de progresión:** Delta pequeña con accuracy alta → StepUp (deadline más corto).
**Criterio de fallo:** Colapso bajo deadline → StepDown; abandono → tombstone.
**Anti-gaming:** Señal: sacrificar todos los trials bajo presión (omitir) para proteger la accuracy calmada. Neutraliza: omisiones bajo presión contadas como error (ya, ventana efectiva) + detector de omisión selectiva por condición (ratio omisiones presión/calma > umbral → CONTAMINATED).
**Persistencia necesaria:** Summary + SessionRecord.
**Feedback inmediato:** Flash + indicador de deadline.
**Feedback post-sesión:** Delta de presión con incertidumbre.
**Feedback histórico:** Tendencia de la delta.
**Riesgo científico:** El deadline depende del frame (presión nominal); el copy roza "rendir bajo presión en la vida" (prohibido).
**Riesgo de UX:** Deadline 450 ms a dificultad 10 exige onboarding claro.
**Riesgo técnico:** Deadline efectivo por frame como criterio de graduación (FrameTimingProbe).
**Claim permitido:** "Entrenas y mides cuánto te afecta el deadline en esta tarea contra tu propia línea base."
**Criterios de aceptación:** (Graduación) nMin≥20/celda + deadline efectivo medido + detector de omisión selectiva; mientras: jugable sin regresión.

### Faro (ENF-14)

**Bloque:** Enfoque · **Estado:** Nuevo (spec de diseño, sin código; propuesto por panel science) · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Orientación atencional encubierta (SELECTIVE_FILTERING espacial anticipatoria).
**Subhabilidades:** Orientación por pista, desenganche de ubicación inválida, mantenimiento de fijación.
**Hipótesis cognitiva:** Cueing espacial tipo Posner: una pista central (flecha) predice la ubicación del target con validez 80% (o 50% neutra); el beneficio/coste de validez (respuesta más lenta tras pista inválida) indexa la orientación encubierta sin mover los ojos de la fijación.
**Mecánica UX:** Fijación central; pista 100–300 ms; SOA variable; target aparece a izquierda o derecha; respuesta = mitad táctil del LADO donde apareció (reusa ChoiceResponseLayer + overlay de pista: variante menor de renderer con payload nuevo `Cueing`).
**Duración recomendada:** 3–5 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Validez de la pista 80%→60%, SOA 400→100 ms, saliencia del target, proporción de pistas neutras.
**Métricas capturadas:** ACCURACY por validez (válido/inválido/neutro), RT_MEDIAN por validez; validity effect (RT inválido−válido) como difference-score.
**Scoring específico:** Accuracy gobierna (normalizedScore del ejercicio = accuracy Wilson-gated n≥20 o null); validity effect SIEMPRE secundaria con ≥20/celda (A2) — nunca primaria (RT-crítica sin timebase autoritativo).
**Cómo afecta al score del bloque:** NO cuenta en v1; al graduar, accuracy al composite.
**Criterio de progresión:** Accuracy alta con coste de invalidez estable → SOA más corto/validez menor.
**Criterio de fallo:** Accuracy a azar o sesgo de pista → StepDown/CONTAMINATED.
**Anti-gaming:** Señal: cue-following (responder al lado de la PISTA, no del target — acierta el 80% gratis con validez alta). Neutraliza: **DF4** detector cue-following (accuracy condicionada: acc_inválido≈0 con acc_válido alta → CONTAMINATED; par red-team obligatorio) + balanceo de lado del target + filtro de anticipación (respuestas pre-target).
**Persistencia necesaria:** Summary versionado nuevo + SessionRecord + registro por bloque de n por celda de validez.
**Feedback inmediato:** Flash por trial.
**Feedback post-sesión:** Accuracy por tipo de pista; coste de invalidez solo confident.
**Feedback histórico:** Tendencia de accuracy; RCI cuando exista probe propia.
**Riesgo científico:** En detección simple la accuracy va a techo y TODA la señal es RT (débil sin timebase) — mitigación: respuesta de localización L/R (no detección) mantiene accuracy informativa.
**Riesgo de UX:** El usuario debe entender "no muevas los ojos de la cruz" sin eye-tracking real (instrucción, no verificable — declararlo).
**Riesgo técnico:** Payload+overlay de pista nuevos (aditivos); riesgo bajo-medio; sin renderer nuevo completo.
**Claim permitido:** "Entrenas y mides cómo orientas tu atención con pistas en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Generador determinista con property de validez/balanceo; DF4 con par adversario/honesto; accuracy null con n<20; registry+catálogo+test de consistencia actualizados en el mismo cambio.

### Corriente (ENF-15)

**Bloque:** Enfoque · **Estado:** Nuevo (spec de diseño, sin código; propuesto por panel science) · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Atención sostenida a la respuesta (SUSTAINED_VIGILANCE bajo automatismo).
**Subhabilidades:** Inhibición bajo ritmo automático, detección del deslizamiento pre-error, consistencia de cadencia.
**Hipótesis cognitiva:** SART (Robertson): responder a TODOS los estímulos frecuentes (~89% go) con ISI FIJO y rítmico que induce automatismo; retener SOLO ante el ítem raro fijo. La comisión en el no-go y la aceleración del RT pre-error son la firma del deslizamiento atencional. Se diferencia de E3 (prepotencia variable, jitter, ventana corta) por el ritmo constante y el énfasis en el patrón PRE-error, y de E12 (que mide el patrón POST-error).
**Mecánica UX:** Single-tap sobre símbolo central (dígitos/glifos neutrales); ISI constante (p.ej. 1150 ms); no-go = símbolo raro fijo; reusa `FocusStimulusField` con payload de símbolo (aditivo).
**Duración recomendada:** 4–6 min.
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Tasa de go 85%→92%, ISI 1300→900 ms, similitud perceptual del no-go con los go, longitud del bloque.
**Métricas capturadas:** FALSE_ALARMS (comisiones en no-go — señal núcleo), OMISSIONS, go-ACCURACY, RT_CV, RT_MEDIAN; nuevo: aceleración pre-error (pendiente de RT en los 4 trials previos a comisión) como secundaria.
**Scoring específico:** Señal primaria = 1−commission_rate combinada con go-accuracy≥0.90 (gate: sin go-accuracy alta la retención no vale); accuracy-based → apta para perfil con n_nogo≥20; aceleración pre-error secundaria (RT-crítica).
**Cómo afecta al score del bloque:** NO cuenta en v1; al graduar, ControlErrores/Precisión al composite.
**Criterio de progresión:** Comisiones bajas con go-accuracy alta → sube tasa go/baja ISI.
**Criterio de fallo:** Comisiones en racha (desenganche) → Cooldown; abandono → tombstone.
**Anti-gaming:** Señal 1: estrategia conservadora "ir lento a todo" protege el no-go a costa del go (trade-off clásico del SART). Neutraliza: gate go-accuracy≥0.90 + banda de RT esperada (RT mediana > umbral con omisiones → bloque marcado estrategia, no premiado). Señal 2: tap 100% rítmico (la tarea ES rítmica) → aquí DF1 se INVIERTE: la comisión en no-go ya delata el automatismo; responder a todo (comisión≈100%) → CONTAMINATED trivial (detector responder-a-todo compartido con E3).
**Persistencia necesaria:** Summary versionado nuevo + SessionRecord; n_nogo por bloque.
**Feedback inmediato:** Flash sutil (no romper el ritmo); retención exitosa reforzada.
**Feedback post-sesión:** Comisiones + go-accuracy + nota sobre la firma pre-error si confident.
**Feedback histórico:** Tendencia de commission rate.
**Riesgo científico:** Speed-accuracy trade-off inherente al SART (la literatura lo critica): el gate dual (go-accuracy + banda RT) es obligatorio o la métrica premia estrategia.
**Riesgo de UX:** Monotonía por diseño; bloques >6 min elevan abandono.
**Riesgo técnico:** Payload de símbolo aditivo; n_nogo bajo por bloque (≈11% de trials) → varios bloques para n≥20 (comunicar "X de N").
**Claim permitido:** "Entrenas y mides tu control para no responder en automático en esta tarea contra tu propia línea base."
**Criterios de aceptación:** Generador determinista (≥1 no-go/bloque garantizado, property); gate dual go-accuracy+banda RT testeado con adversario conservador y honesto; registry+catálogo+consistencia en el mismo cambio; copy-scanner verde.

---

## 4. Decisiones — 15 Exercises

**Propósito:** medir y entrenar el proceso de decisión (actualización con evidencia, parada de búsqueda, priorización, criterio bajo costes asimétricos, control del impulso) contra criterios normativos computables — nunca el outcome, nunca la suerte. **Política de scoring:** todo ejercicio emite `ScoreResult` (normalizedScore 0–1 **o null**, jamás 0 fabricado) vía adapter a `core/scoring`, con `MinSampleGate`, `BankIntegrity` y anti-gaming testeado con adversarios sintéticos; el motor legacy `com.mindverse.mindboost.core` queda detrás de los adapters. RT nunca alimenta perfil (timebase epoch). **Check-In (CRI Go/No-Go)** se re-etiqueta **"Calibración"**: es un semáforo de estado pre-sesión (se Mantiene, con seed aleatorizada y RT-CV marcada no fiable), NO cuenta como ejercicio ni alimenta el perfil.

**Selección MVP (5):**

| Ejercicio | Justificación (1 línea) |
|---|---|
| Reversibility Triage (`E2_REVERSIBILIDAD`) | Habilidad núcleo del bloque; scorer/store/adapter ya construidos y honestos — solo falta el gate humano (κ≥0.75) y el flag. |
| Bayes Arena (`decisiones.bayesArena`) | Única señal de calidad decisional jugable hoy; constructo real; requiere adapter a core/scoring + dificultad real. |
| DEC-03 Muestreo de Información con Coste | Cubre parada óptima/valor de información — constructo distinto de Bayes (cuándo parar vs cómo actualizar), verificable por VOI. |
| DEC-04 Priorización por Dominancia | Priorización con error normativo inequívoco (elección dominada), sin banco textual ni jueces — barato de validar. |
| DEC-05 Aceptar/Rechazar con Deadline | Costes asimétricos + presión cubren impulso/trade-offs reutilizando `ChoiceResponseLayer` (A11) — el nuevo más barato. |

**Requisitos de banco:** Reversibility = 30 escenarios balanceados 15/15 en 6 dominios cotidianos no sensibles (`ReversibilityScenarioBank`), validación 2 jueces κ≥0.75, ítems en desacuerdo se reescriben o descartan, `bankVersion` + registro en `ProfileUpdatingBanks`. Bayes/DEC-03/04/05/11/15 = bancos PROCEDURALES paramétricos (verdad computada por motor: posterior/VOI/dominancia/criterio), seed por sesión, pool ≥40 configuraciones por celda de dificultad, no-repetición hasta agotar pool. DEC-12 = banco textual con κ≥0.75 (mismo protocolo que Reversibility). DEC-06 = ítems evidence-controlled perceptuales/numéricos (verdad fijada por diseño, nunca trivia cultural).

### Reversibility Triage — Puerta de Una Vía vs Dos Vías

**Bloque:** Decisiones · **Estado:** Mejorar (`E2_REVERSIBILIDAD`, código completo oculto por flag; cambio concreto: validar banco κ≥0.75 → cablear `ReversibilityScenarioBank` al generador → retirar `SMALL_UNVALIDATED` → `includeReversibility=true`) · **Prioridad:** P0 · **Apto MVP:** Con cambios (gate humano de validación)
**Habilidad cognitiva principal:** Discriminación riesgo/recompensa — clasificación reversible (Type-2) vs irreversible (Type-1).
**Subhabilidades:** Control inhibitorio del impulso (lock de confirmación), estimación de coste de error, velocidad calibrada al coste.
**Hipótesis cognitiva:** Discriminación de categorías por coste de deshacer (decisiones "one-way vs two-way door"), medida con marco de detección de señal: balanced accuracy = (sensibilidad+especificidad)/2, inmune a base-rate guessing. Fuente: diseño E2 de `03_6_Decisiones.md` + código real.
**Mecánica UX:** Tarjeta con escenario cotidiano real (banco de 30) → elección binaria reversible/irreversible; en irreversibles, lock de confirmación (anillo de progreso 2–8 s) que penaliza la prisa; en reversibles, la demora acumula penalización de proceso (feedback, no perfil).
**Duración recomendada:** 5 min (8–12 tarjetas/bloque).
**Dificultad mínima:** 1 (escenarios inequívocos, sin presión). **Dificultad máxima:** 10 (ambigüedad de marcadores alta, mezcla 50/50, deadline visible).
**Variables de dificultad:** ambigüedad del escenario, proporción de tipos, duración de lock, presión temporal, dominio rotado.
**Métricas capturadas:** clasificación por tarjeta (`ReversibilityTrial`), balanced accuracy, tasa de violación de lock, omisión/comisión separadas, RT (secundario, no perfil).
**Scoring específico:** `ReversibilityScorer.scoreBlock` (EXISTE): VALID → discriminationIndex = balanced accuracy; responder-constante → INVALIDATED; sin cobertura de ambas clases → CALIBRATING (null, nunca 0); ≥8 trials/bloque + ≥20 válidos en el adapter. normalizedScore = índice de sesión ponderado por trials válidos, o null.
**Cómo afecta al score del bloque:** Calidad 0.45 (índice de discriminación) + Control de errores (violaciones de lock) + Dificultad (nivel afrontado); vía `DecisionsProfileSource` → DECISION_MAKING.
**Criterio de progresión:** balanced accuracy ≥0.80 estable en el nivel → sube ambigüedad (staircase-lite con histéresis).
**Criterio de fallo:** <0.65 en bloque o ≥2 violaciones de lock → deload de nivel; nunca "suspenso" visible, informe de proceso.
**Anti-gaming:** base-rate guessing colapsa a azar por construcción (balanced accuracy, testeado); responder-constante → INVALIDATED (DEC-AG2, testeado); respuesta antes del lock = violación, jamás acierto rápido; entropía de respuesta real al AGRS vía D6 (sustituye `spatialEntropy=1.0` hardcoded, `ReversibilityTriageStateHolder.kt:73`); seed por sesión.
**Persistencia necesaria:** `LocalReversibilityResultStore` (EXISTE, cifrado, namespace `mindboost_decisiones`, AccountBound) + `SessionRecord` write-ahead (A12).
**Feedback inmediato:** correcto/incorrecto por tarjeta + lock respetado (nunca "demasiado lento" en irreversibles).
**Feedback post-sesión:** índice de discriminación, violaciones de lock, patrón (prisa vs parálisis), estado de calibración.
**Feedback histórico:** tendencia del índice vs línea base propia; "X de 5 sesiones" durante calibración.
**Riesgo científico:** etiquetas de reversibilidad discutibles → mitigado por validación inter-rater κ≥0.75 y descarte de ítems en desacuerdo.
**Riesgo de UX:** el lock puede sentirse fricción arbitraria → explicarlo como medida ("las decisiones difíciles de deshacer merecen una pausa").
**Riesgo técnico:** activar el flag sin banco validado expondría el generador coin-flip — gate flag↔banco obligatorio en `BankValidationGateTest`.
**Claim permitido:** "Entrenas a distinguir decisiones reversibles de irreversibles dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** banco κ≥0.75 registrado; `SMALL_UNVALIDATED` retirado solo entonces (`ReversibilityScore.kt:54`); flag activo; primera contribución VALID llega al perfil end-to-end vía aggregator; suite adversarial verde; no-repetición de escenario hasta agotar pool.

### Bayes Arena — Calibrador de Posterior

**Bloque:** Decisiones · **Estado:** Mejorar (`decisiones.bayesArena`; cambio concreto: `BayesArenaProfileAdapter`→`ScoreResult`, exclusión priori≈posterior del BUR, `movedFromDefault`+entropía de dial reales, 3 niveles de dificultad reales, min-n en `finish()`) · **Prioridad:** P0 · **Apto MVP:** Con cambios
**Habilidad cognitiva principal:** Actualización bayesiana de creencias / juicio bajo incertidumbre.
**Subhabilidades:** Uso de tasas base, integración sensibilidad/falsos-positivos, dirección del ajuste (anti over/under-updating), calibración de confianza.
**Hipótesis cognitiva:** Paradigma de revisión de creencias con tasas base (base-rate neglect, Kahneman-Tversky; formato de frecuencias naturales, Gigerenzer-Hoffrage). La señal es proximidad al posterior normativo (BUR en espacio logit vía `BayesEngine.evaluateBayesianUpdate`) — calidad de proceso, no acierto de outcome. Fuente: diseño E1 de `03_6_Decisiones.md` + código real.
**Mecánica UX:** Consola de evidencia (prevalencia/sensibilidad/FA como barras) → slider de posterior 0–100% + slider de certeza 50–100% → confirmar; 6–8 escenarios/sesión; feedback "tu estimación vs el cálculo".
**Duración recomendada:** 5–7 min.
**Dificultad mínima:** 1 (base-rate moderada, separación TP-FP amplia, 1 evidencia). **Dificultad máxima:** 10 (base-rate extrema, separación mínima, evidencias encadenadas, deadline suave).
**Variables de dificultad:** separación TP-FP, extremidad de base-rate, nº de evidencias encadenadas, ruido del generador, formato (frecuencias naturales↔probabilidades), deadline. 3 niveles reales (memo A10) mapeados a 1–3/4–6/7–10.
**Métricas capturadas:** posterior estimado, posterior normativo, BUR, error absoluto, confianza, `movedFromDefault`, RT (secundario).
**Scoring específico:** normalizedScore = mediana de f(BUR)=exp(−(ln BUR)²/k) sobre trials VÁLIDOS: se excluyen priori≈posterior (|logitMath−logitPrior|≤ε — hoy inflan BUR=1.0), dial-default y confirmaciones <500 ms; null si n<12 válidos (hoy `meanOrZero` colapsa a 0 — se corrige). Versionado `bayes-arena-v2`.
**Cómo afecta al score del bloque:** Calidad 0.45 vía `BayesArenaProfileAdapter`→`ScoreResult`→`ProfileUpdateGate` (nueva 2ª fuente de `DecisionsProfileSource`); confianza solo covariable.
**Criterio de progresión:** |BUR|∈[0.85,1.15] en ≥75% de trials válidos → sube nivel; histéresis al bajar.
**Criterio de fallo:** <50% en banda o FRS>0.20 → deload (más separación TP-FP, sin deadline).
**Anti-gaming:** DEC-AG1 dial-default real (`movedFromDefault`; hoy `isDialDefault=false` hardcoded en `BayesArenaStateHolder.kt:72` — se rehace) + entropía de recorrido del dial al AGRS vía D6; confirmación <500 ms invalida; seed por sesión (ya via `DecisionsRootScreen.kt:46`; test de variabilidad fija que `DEFAULT_SEED=0L` no llegue a producción); suite red-team: perfil dial-default, perfil anclado-a-base-rate, honesto.
**Persistencia necesaria:** `SecureDecisionSessionRepository` (EXISTE, sin wiring — P0 onSealed) + `SessionRecord` (A12).
**Feedback inmediato:** gap firmado vs posterior normativo por escenario.
**Feedback post-sesión:** BUR mediano, dirección del sesgo (infra/sobre-actualización), estado de calibración.
**Feedback histórico:** tendencia BUR vs línea base; distribución over/under.
**Riesgo científico:** destreza motora del slider como confound → granularidad gruesa (20 pasos) en condición de medida + banda de acierto ≥1 paso.
**Riesgo de UX:** texto de escenario introduce lectura/cultura → plantillas neutras con glyphs numéricos.
**Riesgo técnico:** el "posterior mostrado" debe salir del MISMO `BayesEngine` que puntúa, o el feedback miente (test de identidad de motor).
**Claim permitido:** "Entrenas y medimos cómo actualizas tus estimaciones con la evidencia dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** adapter con adversarios verde y registrado en `AdequateCoverageInvariantTest`; exclusión priori≈posterior testeada; 3 niveles cambian el generador de verdad; sesión persiste en onSealed (test rojo→verde); golden del scoring.

### DEC-03 — Muestreo de Información con Coste

**Bloque:** Decisiones · **Estado:** Nuevo (fuente: rediseño de E8 "Resolución bajo Ambigüedad" de `03_6_Decisiones.md`, con las 5 correcciones de su auditoría) · **Prioridad:** P1 · **Apto MVP:** Sí (destino MVP)
**Habilidad cognitiva principal:** Parada óptima de recolección de información (valor de la información).
**Subhabilidades:** Trade-off coste↔certeza, integración secuencial de evidencia, anti-parálisis y anti-decisión-ciega.
**Hipótesis cognitiva:** Paradigma de information sampling con coste por muestra: cada evidencia comprada actualiza el posterior (`BayesEngine.calculatePosterior`) y el óptimo de parada se deriva de un cálculo explícito de valor esperado de la información (VOI). Diferencia nítida con Bayes Arena: mide CUÁNDO parar, no CÓMO actualizar.
**Mecánica UX:** Hipótesis A/B con prior débil; botón "pedir otra evidencia" (coste en puntos de recurso neutro) vs "decidir ya"; barra de posterior se mueve al integrar; al decidir, elegir A/B. 8–10 rondas/sesión.
**Duración recomendada:** 6–8 min.
**Dificultad mínima:** 1 (evidencia barata y potente, prior claro). **Dificultad máxima:** 10 (coste alto, evidencias poco informativas, payoff asimétrico que desplaza el umbral).
**Variables de dificultad:** coste por evidencia, poder informativo (likelihood ratio), fuerza del prior, asimetría de payoff, máximo de evidencias.
**Métricas capturadas:** evidencias compradas vs óptimas (stopping error), acierto A/B, payoff neto, tasa over/under-collection, RT (secundario).
**Scoring específico:** normalizedScore = 0.45·(1−stoppingErrorNorm) + 0.35·accuracy + 0.20·payoffNorm sobre rondas válidas; banda óptimo±1 tolerante (la cautela leve con payoff simétrico NO es error); null si n<10 rondas válidas.
**Cómo afecta al score del bloque:** Calidad (stopping+accuracy) + Control de errores (decisiones <400 ms, política degenerada); adapter propio → `ScoreResult`.
**Criterio de progresión:** stopping error ≤1 en ≥75% → sube nivel (payoff asimétrico).
**Criterio de fallo:** política degenerada detectada o error medio >2 → deload y explicación de proceso.
**Anti-gaming:** DEC-AG3 política degenerada (siempre-comprar-max / nunca-comprar): correlación compras↔incertidumbre del ítem ≈ 0 → WITHHOLD, con par adversario/honesto en la suite red-team; decidir <400 ms invalida ronda; seed por sesión; D6 entropía de secuencia de acciones.
**Persistencia necesaria:** summaries por sesión en `SecureDecisionSessionRepository` + `SessionRecord` write-ahead desde el día 1.
**Feedback inmediato:** al decidir, umbral óptimo vs tu parada (línea en la barra).
**Feedback post-sesión:** perfil over/under-collector + stopping error medio + payoff.
**Feedback histórico:** tendencia del stopping error vs línea base.
**Riesgo científico:** confunde aversión al riesgo (rasgo) con habilidad → banda tolerante + copy que no culpa la cautela; el rasgo se reporta como observación, no score.
**Riesgo de UX:** ronda lenta si el usuario duda → cap de evidencias visible.
**Riesgo técnico:** VOI incorrecto convierte la señal en ruido → solver VOI determinista en commonMain con golden tests exhaustivos (incluida asimetría), misma fuente para feedback y score.
**Claim permitido:** "Entrenas a decidir cuánta información necesitas antes de decidir dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** golden del VOI (con payoff asimétrico); adversario degenerado detectado y honesto limpio; banda ±1 testeada; null con n<10; SessionRecord activo.

### DEC-04 — Priorización por Dominancia

**Bloque:** Decisiones · **Estado:** Nuevo (fuente: rediseño de E5 "Escáner de Alternativas" de `03_6_Decisiones.md`, reencuadrado a frontera objetiva por su auditoría: elección dominada = error) · **Prioridad:** P1 · **Apto MVP:** Sí (destino MVP)
**Habilidad cognitiva principal:** Priorización / integración multiatributo con criterio objetivo.
**Subhabilidades:** Detección de dominancia, resistencia a la saliencia (la opción vistosa no es la mejor), consistencia transitiva.
**Hipótesis cognitiva:** Dominancia estricta de Pareto: elegir una opción dominada (peor o igual en todos los atributos y peor en uno) es un error normativo inequívoco según los axiomas de elección racional — la tarea mide detección de dominancia, no gusto, eliminando toda subjetividad de "la respuesta correcta".
**Mecánica UX:** 3–6 opciones como tarjetas con 2–4 atributos numéricos/icónicos (sin texto cultural); exactamente una no-dominada; tap para elegirla; variante alta: ordenar top-3 (tap-order, τ de Kendall — patrón L3 de Lógica). Deadline suave por trial.
**Duración recomendada:** 5 min (14–20 trials).
**Dificultad mínima:** 1 (2 atributos, margen de dominancia amplio). **Dificultad máxima:** 10 (4 atributos, margen mínimo, señuelo casi-dominante, saliencia invertida, deadline).
**Variables de dificultad:** nº opciones, nº atributos, margen de dominancia, mismatch de saliencia, señuelo casi-dominante, deadline.
**Métricas capturadas:** elección (dominada/no-dominada), margen del error, RT (secundario), consistencia entre pares espejo, orden top-3 (variante).
**Scoring específico:** normalizedScore = 1 − tasa de elección dominada ponderada por margen (equivocarse con margen grande pesa más); trials <400 ms inválidos; null si n<12 válidos; violaciones de transitividad alimentan la componente Consistencia, no anulan el bloque.
**Cómo afecta al score del bloque:** Calidad (tasa no-dominada) + Consistencia (transitividad) + Dificultad; adapter propio → `ScoreResult`.
**Criterio de progresión:** ≥0.85 no-dominada estable → sube margen/atributos.
**Criterio de fallo:** ≤ azar en el nivel → deload; informe "te ancla la opción llamativa".
**Anti-gaming:** DEC-AG4 respuesta-por-posición: la no-dominada rota de posición y cada sesión incluye pares espejo (mismo set con posiciones permutadas) — inconsistencia espejo + entropía posicional baja (D6) → WITHHOLD; selección <400 ms invalida; generación paramétrica structureId×surfaceVariant con ventana de no-repetición (patrón A9).
**Persistencia necesaria:** summaries + `SessionRecord`; sin banco humano (dominancia computada).
**Feedback inmediato:** al fallar, resalta el atributo en que la elegida pierde.
**Feedback post-sesión:** tasa de dominadas, sensibilidad al margen, efecto saliencia.
**Feedback histórico:** tendencia vs línea base; nivel máximo estable.
**Riesgo científico:** con atributos en conflicto la "mejor" es opinable → V1 SOLO dominancia estricta (sin trade-offs subjetivos); la variante suma-ponderada muestra los pesos.
**Riesgo de UX:** atributos numéricos áridos → iconografía consistente con magnitud redundante (forma+número, no solo color).
**Riesgo técnico:** renderer de grid/lista nuevo — reutilizar `GridField` compartido (A11) en cuanto exista; fallback: tarjetas apiladas + tap.
**Claim permitido:** "Entrenas a priorizar opciones comparando sus atributos dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** generador con dominancia única garantizada (property test); pares espejo activos y testeados; adversario posicional detectado; null con n<12; no-repetición inter-sesión.

### DEC-05 — Aceptar/Rechazar con Deadline

**Bloque:** Decisiones · **Estado:** Nuevo (fijado por panel A10-D5; reutiliza `ChoiceResponseLayer` extraído, A11) · **Prioridad:** P1 · **Apto MVP:** Sí (destino MVP)
**Habilidad cognitiva principal:** Colocación de criterio de decisión bajo costes asimétricos y presión temporal.
**Subhabilidades:** Control del impulso (no aceptar todo), tolerancia a dejar pasar, adaptación del criterio a la matriz de costes.
**Hipótesis cognitiva:** Teoría de detección de señal con matriz de pagos asimétrica (criterion placement, Green & Swets): el criterio óptimo se desplaza con los costes relativos de aceptar-malo vs rechazar-bueno; el deadline por ítem impide deliberación ilimitada y hace observable la política. Recursos SIEMPRE neutros (puntos de energía/tiempo), nunca dinero.
**Mecánica UX:** Ofertas secuenciales (tarjeta con calidad y coste observables, valor computable); mitades táctiles LEFT=rechazar / RIGHT=aceptar (`ChoiceResponseLayer`); deadline visible 2–6 s por ítem; la matriz de costes del bloque se muestra al inicio y cambia entre bloques.
**Duración recomendada:** 5 min (2 bloques × 20 ítems).
**Dificultad mínima:** 1 (valores muy separados, deadline 6 s, costes simétricos). **Dificultad máxima:** 10 (valores solapados, deadline 2 s, asimetría fuerte que exige criterio desplazado).
**Variables de dificultad:** solapamiento de distribuciones de valor, deadline, asimetría de costes, proporción de ítems buenos.
**Métricas capturadas:** decisión por ítem, FA (aceptar malo) y miss (rechazar bueno) separadas, expiraciones, payoff neto, criterio empírico estimado, RT (secundario).
**Scoring específico:** normalizedScore = 0.6·(1−|criterioEmpírico−criterioÓptimo|norm) + 0.4·payoffNorm sobre ítems respondidos; expiración = no-decisión (reduce n, no puntúa como error doble); null si n<20 respondidos (≥10 por clase).
**Cómo afecta al score del bloque:** Calidad (criterio) + Control de errores (expiraciones, respuestas <300 ms) + Dificultad; adapter propio → `ScoreResult`.
**Criterio de progresión:** distancia al criterio óptimo estable y decreciente → sube solapamiento/asimetría.
**Criterio de fallo:** criterio degenerado o >30% expiraciones → deload (deadline más largo).
**Anti-gaming:** DEC-AG5 criterio degenerado aceptar-todo/rechazar-todo (FA o miss ≈100% de su clase) → WITHHOLD (espejo del gate responder-constante de `ReversibilityScorer`, con par adversario/honesto); ritmo mecánico → D1 cadencia/IRI de `core/antigaming`; respuestas <300 ms invalidan; seed por sesión.
**Persistencia necesaria:** summaries + `SessionRecord`; nace con analytics started/completed/abandoned.
**Feedback inmediato:** resultado del ítem (coste incurrido/evitado), sin interrumpir el flujo.
**Feedback post-sesión:** tu criterio vs el óptimo del bloque, FA/miss, expiraciones.
**Feedback histórico:** adaptación del criterio a matrices distintas vs línea base.
**Riesgo científico:** deadline con reloj epoch → usar ventanas gruesas (≥2 s) donde el jitter del scheduler es despreciable; RT jamás al perfil.
**Riesgo de UX:** frustración por deadline corto → deload automático y copy no culpabilizante.
**Riesgo técnico:** parametrización del criterio óptimo — derivarlo de las distribuciones generadoras (cerrado, testeable), no de heurística.
**Claim permitido:** "Entrenas a aceptar o descartar opciones con tiempo limitado dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** criterio óptimo con golden test; adversarios degenerados detectados; expiración tratada como no-decisión (test); reuso real de `ChoiceResponseLayer`; null con n<20.

### DEC-06 — Calibración de Confianza

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; fuente: E3 "Forecast Calibrado" de `03_6_Decisiones.md`, renombrado y purgado de léxico prohibido según su auditoría) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Calibración metacognitiva confianza↔acierto.
**Subhabilidades:** Anti-sobreconfianza, anti-infraconfianza, uso del rango completo de confianza.
**Hipótesis cognitiva:** Paradigma de calibración de juicio (Lichtenstein-Fischhoff): la confianza declarada debe igualar la frecuencia real de acierto; puntuación proper (Brier) que hace óptimo reportar la creencia verdadera. Ítems perceptuales/numéricos con verdad fijada por diseño (p. ej. proporción de una mezcla visual) — nunca trivia cultural.
**Mecánica UX:** Juicio binario A/B sobre estímulo perceptual + slider de confianza 50–100%; 20–30 ítems.
**Duración recomendada:** 6–8 min.
**Dificultad mínima:** 1 (evidencia fuerte). **Dificultad máxima:** 10 (evidencia al borde del umbral perceptual).
**Variables de dificultad:** fuerza de la evidencia (separación perceptual), granularidad del slider, deadline suave.
**Métricas capturadas:** elección, confianza, acierto, Brier, calibration error (5 bins), sd de confianzas.
**Scoring específico:** normalizedScore = 0.50·(1−Brier) + 0.35·(1−calErr) + 0.15·clamp(sdConfianzas); null si n<40 para Brier (memo A2) o <25 para calErr; confianza-constante no puntúa alto.
**Cómo afecta al score del bloque:** Calidad; segunda señal metacognitiva (complementa la confianza covariable de Bayes).
**Criterio de progresión:** calErr decreciente estable → evidencia más fina.
**Criterio de fallo:** colapso al 50% persistente → intervención de coach, no castigo.
**Anti-gaming:** confianza-constante (sd≈0) detectada como degenerado (detector nuevo con par adversario/honesto); siempre-misma-opción → D6 entropía; <400 ms invalida.
**Persistencia necesaria:** summaries + serie cross-day para curva de calibración.
**Feedback inmediato:** acierto + Brier del ítem.
**Feedback post-sesión:** curva confianza-vs-acierto por bins.
**Feedback histórico:** deriva de la sobre/infraconfianza vs línea base.
**Riesgo científico:** Brier premia conservadurismo si la dificultad no se calibra → staircase de evidencia obligatoria.
**Riesgo de UX:** doble input (elección+slider) alarga el trial.
**Riesgo técnico:** generador perceptual con verdad controlada — nuevo pero simple (proporciones).
**Claim permitido:** "Entrenas a ajustar tu confianza a tus aciertos dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** ítems 100% evidence-controlled; property de propiedad proper del Brier; adversario confianza-constante; null bajo n mínimo; cero léxico prohibido (scanner).

### DEC-07 — Resistencia a Framing

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Consistencia de elección ante marcos ganancia/pérdida equivalentes.
**Subhabilidades:** Detección de equivalencia, estabilidad de preferencia, resistencia a presión de presentación.
**Hipótesis cognitiva:** Efecto de framing (Tversky-Kahneman): pares de decisiones formalmente idénticas presentadas como ganancia o como pérdida de recursos neutros; la señal es la tasa de INVERSIÓN de preferencia intra-sujeto entre marcos equivalentes — consistencia, no "acertar".
**Mecánica UX:** Elecciones binarias con cantidades y probabilidades explícitas de recursos neutros; los pares espejo se separan dentro de la sesión con variantes superficiales (structureId×surfaceVariant).
**Duración recomendada:** 6 min (≥16 pares).
**Dificultad mínima:** 1 (equivalencia evidente). **Dificultad máxima:** 10 (equivalencia oculta tras redondeos y estructura superficial distinta).
**Variables de dificultad:** opacidad de la equivalencia, distancia entre los dos miembros del par, deadline suave.
**Métricas capturadas:** elección por miembro del par, inversiones entre marcos, RT (secundario).
**Scoring específico:** normalizedScore = 1 − tasa de inversión sobre pares completos válidos; null si n<16 pares; pares con un miembro inválido (<400 ms) se descartan enteros.
**Cómo afecta al score del bloque:** Calidad + Consistencia.
**Criterio de progresión:** inversiones <15% estable → sube opacidad.
**Criterio de fallo:** inversiones ≥50% (azar) → deload + explicación de equivalencia.
**Anti-gaming:** memorizar pares → variantes superficiales paramétricas + ventana de no-repetición de structureId (patrón A9); respuesta por posición → D6 + rotación; responder siempre la opción segura es una PREFERENCIA legítima (no gaming) — solo puntúa la consistencia.
**Persistencia necesaria:** summaries + registro de pares servidos (no-repetición).
**Feedback inmediato:** ninguno por miembro (evitaría medir el segundo); al cerrar el par, nada visible.
**Feedback post-sesión:** tasa de inversión + ejemplos de pares equivalentes explicados.
**Feedback histórico:** tendencia de consistencia vs línea base.
**Riesgo científico:** los marcos requieren texto → plantillas numéricas mínimas; la actitud al riesgo del sujeto NO se puntúa (solo su estabilidad).
**Riesgo de UX:** sensación de "trampa" al revelar pares → copy pedagógico serio.
**Riesgo técnico:** generador de pares equivalentes verificado por property test de equivalencia formal.
**Claim permitido:** "Entrenas la consistencia de tus elecciones ante presentaciones distintas del mismo problema dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** property de equivalencia formal de cada par; descarte de pares incompletos; n≥16; no-repetición testeada.

### DEC-08 — Resistencia a Anclaje

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Independencia de la estimación respecto de anclas irrelevantes.
**Subhabilidades:** Estimación de cantidades, supresión de números salientes irrelevantes.
**Hipótesis cognitiva:** Anclaje (Tversky-Kahneman): un número irrelevante mostrado antes de estimar sesga la estimación hacia él; la señal es el desplazamiento sistemático hacia el ancla en pares con ancla alta vs baja sobre estímulos cuya verdad es computable EN la tarea (conteo aproximado de elementos visuales), nunca trivia.
**Mecánica UX:** Se muestra un número irrelevante ("código de la ronda"), luego un estímulo visual breve (nube de puntos/objetos); el usuario estima la cantidad con stepper/slider.
**Duración recomendada:** 5–6 min (≥20 estimaciones, 10 por brazo de ancla).
**Dificultad mínima:** 1 (exposición larga, cantidades pequeñas). **Dificultad máxima:** 10 (exposición breve, cantidades grandes, ancla extrema).
**Variables de dificultad:** duración de exposición, magnitud, extremidad del ancla.
**Métricas capturadas:** estimación, verdad, ancla, error firmado, pull = correlación estimación↔ancla controlando la verdad.
**Scoring específico:** normalizedScore = 1 − pullNorm sobre pares de anclas; la exactitud de estimación se reporta aparte (covariable); null si n<20; estimaciones <400 ms tras el estímulo invalidan.
**Cómo afecta al score del bloque:** Calidad (independencia del ancla) + Consistencia.
**Criterio de progresión:** pull bajo estable → exposición más breve.
**Criterio de fallo:** pull ≥ umbral alto → deload + explicación del efecto.
**Anti-gaming:** estimación constante (sd≈0) → degenerado detectado; ignorar el estímulo y responder el ancla → pull≈1 es la propia señal (no confundir con gaming: se reporta como efecto, WITHHOLD solo si sd≈0); D6 entropía de respuesta.
**Persistencia necesaria:** summaries por sesión.
**Feedback inmediato:** verdad revelada tras estimar.
**Feedback post-sesión:** tu desplazamiento medio hacia el ancla, por brazo.
**Feedback histórico:** tendencia del pull vs línea base.
**Riesgo científico:** el anclaje es robusto incluso sabiéndolo → expectativas honestas: se entrena la tarea, no se promete inmunidad.
**Riesgo de UX:** presentar el ancla sin que parezca relevante requiere cuidado de copy.
**Riesgo técnico:** estimación perceptual necesita render estable del estímulo (conteo verificable por generador).
**Claim permitido:** "Entrenas a estimar cantidades sin dejarte llevar por números irrelevantes dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** diseño de dos brazos de ancla balanceados; pull computado con control de la verdad (property); null con n<20.

### DEC-09 — Descuento Temporal

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science con reserva fuerte) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Consistencia en elecciones intertemporales (antes↔después).
**Subhabilidades:** Estabilidad de preferencia temporal, monotonía, no-impulsividad procedimental.
**Hipótesis cognitiva:** Elección intertemporal con titración (estilo Kirby) sobre recursos neutros de la app: "X puntos ahora vs Y puntos en Z sesiones". La TASA de descuento es una preferencia (rasgo), no una habilidad con respuesta correcta → SOLO se puntúa la CONSISTENCIA (violaciones de monotonía/transitividad en la titración); la tasa se reporta como observación sin score.
**Mecánica UX:** Elecciones binarias rápidas en escalera de titración; 24–30 elecciones.
**Duración recomendada:** 4–5 min.
**Dificultad mínima:** 1 (diferencias groseras). **Dificultad máxima:** 10 (puntos de indiferencia finos).
**Variables de dificultad:** granularidad de la titración, rango de demoras.
**Métricas capturadas:** elecciones, punto de indiferencia estimado, violaciones de monotonía, RT (secundario).
**Scoring específico:** normalizedScore = 1 − tasa de violaciones de monotonía; null si n<24; la tasa de descuento JAMÁS entra al score.
**Cómo afecta al score del bloque:** Consistencia (peso reducido); nunca Calidad (no hay respuesta correcta).
**Criterio de progresión:** consistencia alta → titración más fina.
**Criterio de fallo:** violaciones ≥ azar → deload.
**Anti-gaming:** patrón fijo (siempre-ahora/siempre-después) es una preferencia extrema legítima, NO gaming — pero produce cero información de titración → CALIBRATING honesto; alternancia mecánica → D6/D1.
**Persistencia necesaria:** summaries.
**Feedback inmediato:** ninguno (evita moldear preferencias).
**Feedback post-sesión:** tu consistencia + observación neutral del patrón temporal (sin juicio).
**Feedback histórico:** estabilidad del punto de indiferencia.
**Riesgo científico:** ALTO — preferencia ≠ habilidad; cualquier copy que valore "paciencia" sería juicio de rasgo prohibido. Por eso P3 y solo consistencia.
**Riesgo de UX:** monótono → sesiones cortas.
**Riesgo técnico:** bajo (elección binaria + titración).
**Claim permitido:** "Entrenas la consistencia de tus elecciones entre ahora y después dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** score solo-consistencia (test que fija que la tasa no afecta normalizedScore); copy sin juicio de rasgo; CALIBRATING ante preferencia extrema.

### DEC-10 — Resistencia a Coste Hundido

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Decidir por valor futuro esperado ignorando inversión irrecuperable.
**Subhabilidades:** Cálculo de EV restante, desapego de inversión previa, cambio de curso.
**Hipótesis cognitiva:** Efecto de coste hundido (Arkes-Blumer): tendencia a continuar un curso de acción por recursos ya gastados; ítems con EV futuro COMPUTABLE en tarea (proyecto A lleva X puntos invertidos pero B domina en retorno restante) donde la elección normativa ignora lo hundido. Diseño cruzado: trials-trampa (hundido empuja a lo subóptimo) + trials-control (continuar ES óptimo).
**Mecánica UX:** Tarjeta con dos cursos de acción, inversión previa visible y retornos futuros explícitos; elección binaria continuar/cambiar; 16–20 trials.
**Duración recomendada:** 5–6 min.
**Dificultad mínima:** 1 (diferencia de EV grande). **Dificultad máxima:** 10 (diferencia fina + inversión previa muy saliente).
**Variables de dificultad:** margen de EV, magnitud/saliencia del hundido, deadline suave.
**Métricas capturadas:** elección EV-óptima por tipo (trampa/control), diferencial trampa−control (susceptibilidad), RT.
**Scoring específico:** normalizedScore = tasa EV-óptima en trampa, VÁLIDA solo si el control ≥0.75 (si falla los control no entendió la tarea → CALIBRATING); null si n<16 (≥8/tipo).
**Cómo afecta al score del bloque:** Calidad + Control de errores.
**Criterio de progresión:** diferencial trampa−control → 0 estable → margen más fino.
**Criterio de fallo:** control <0.75 → deload con tutorial de EV.
**Anti-gaming:** heurística "siempre cambiar" ganaría los trampa sin razonar → los trials-control la castigan por construcción (espejo del gate de discriminación de EX3/EX4 de Memoria, testeado con adversario siempre-cambiar); <400 ms invalida; D6.
**Persistencia necesaria:** summaries.
**Feedback inmediato:** EV revelado tras elegir.
**Feedback post-sesión:** diferencial trampa/control explicado.
**Feedback histórico:** tendencia de susceptibilidad.
**Riesgo científico:** escenarios textuales → plantillas numéricas neutras; nunca inversiones monetarias reales.
**Riesgo de UX:** repetitivo → rotación de superficies.
**Riesgo técnico:** bajo (EV cerrado por generador).
**Claim permitido:** "Entrenas a decidir por el valor futuro y no por lo ya invertido dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** diseño cruzado trampa/control con gate control≥0.75 testeado; adversario siempre-cambiar detectado; null con n<16.

### DEC-11 — Exploración-Explotación

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science: bandit simple) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Equilibrio explorar↔explotar bajo retornos inciertos.
**Subhabilidades:** Estimación de valor por muestreo, adaptación a cambio de contingencias.
**Hipótesis cognitiva:** Multi-armed bandit de 2–3 opciones con retornos estocásticos (estacionarios en niveles bajos, con switch en altos): la señal es el regret normalizado frente a una política de referencia computable y la latencia de adaptación tras el cambio — paradigma estándar de aprendizaje por refuerzo conductual.
**Mecánica UX:** 2–3 "fuentes" abstractas; tap repetido para elegir; retorno inmediato en puntos neutros; 60–80 elecciones por bloque.
**Duración recomendada:** 5–7 min.
**Dificultad mínima:** 1 (medias muy separadas, estacionario). **Dificultad máxima:** 10 (medias próximas, 1–2 switches no anunciados).
**Variables de dificultad:** separación de medias, varianza, presencia/frecuencia de switch.
**Métricas capturadas:** secuencia de elecciones, regret normalizado, latencia de re-adaptación post-switch, tasa de exploración.
**Scoring específico:** normalizedScore = 1 − regretNorm sobre el horizonte; null si <60 elecciones o política degenerada; la latencia post-switch alimenta Consistencia.
**Cómo afecta al score del bloque:** Calidad + Progreso (adaptación).
**Criterio de progresión:** regret bajo estable → introduce switch.
**Criterio de fallo:** regret ≈ política aleatoria → deload.
**Anti-gaming:** políticas degeneradas (pegarse a un brazo desde el inicio / alternancia mecánica) detectadas contra el óptimo (variante DEC-AG3, correlación elección↔evidencia acumulada); seed por sesión con retornos pseudoaleatorios controlados; D1 cadencia.
**Persistencia necesaria:** summaries por bloque (nunca el stream crudo).
**Feedback inmediato:** retorno del tap (es la tarea).
**Feedback post-sesión:** tu política vs referencia, latencia de adaptación.
**Feedback histórico:** tendencia de regret vs línea base.
**Riesgo científico:** varianza estocástica mete ruido en sesiones cortas → bloques largos + semilla controlada.
**Riesgo de UX:** puede parecer azar puro → explicar que hay estructura aprendible.
**Riesgo técnico:** el generador de retornos y la política de referencia deben ser deterministas por seed (golden).
**Claim permitido:** "Entrenas a equilibrar probar opciones nuevas y aprovechar lo que funciona dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** política de referencia con golden; adversarios degenerados; null bajo n; sin léxico de juego de azar en copy (scanner).

### DEC-12 — Juicio Proceso-vs-Resultado

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; recomendado por /mb science) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Evaluar decisiones por su proceso ex-ante, no por su resultado.
**Subhabilidades:** Separación información-disponible↔desenlace, resistencia al sesgo retrospectivo.
**Hipótesis cognitiva:** Outcome bias (Baron-Hershey): juzgar la calidad de una decisión descrita según la información disponible al decidir, con desenlaces buenos/malos CRUZADOS experimentalmente — la señal es la insensibilidad del juicio al desenlace (mismo proceso, distinto resultado → mismo juicio).
**Mecánica UX:** Escenario breve (decisión + información ex-ante) → juicio binario "decisión razonable/no razonable" → después se revela el desenlace y se re-pregunta; 12–16 ítems con diseño cruzado.
**Duración recomendada:** 7–9 min.
**Dificultad mínima:** 1 (procesos claramente buenos/malos). **Dificultad máxima:** 10 (procesos ambiguos + desenlaces extremos).
**Variables de dificultad:** ambigüedad del proceso, extremidad del desenlace.
**Métricas capturadas:** juicio pre y post desenlace, flip-rate por condición, accuracy vs etiqueta validada.
**Scoring específico:** normalizedScore = balanced accuracy vs etiqueta κ≥0.75 (reusa patrón `ReversibilityScorer`) ponderada por insensibilidad al desenlace (flip-rate baja); null si n<12 o banco no validado (BankIntegrity gate).
**Cómo afecta al score del bloque:** Calidad + Consistencia.
**Criterio de progresión:** flip-rate baja estable → procesos más ambiguos.
**Criterio de fallo:** flips ≥50% → deload.
**Anti-gaming:** responder-constante → INVALIDATED (patrón DEC-AG2); base-rate → balanced accuracy; ítems con `excludeRecent` anti-memorización.
**Persistencia necesaria:** summaries + registro de ítems servidos.
**Feedback inmediato:** ninguno hasta cerrar el par pre/post.
**Feedback post-sesión:** tu sensibilidad al desenlace explicada con un par ejemplo.
**Feedback histórico:** tendencia de insensibilidad.
**Riesgo científico:** banco textual = validación inter-rater obligatoria (mismo protocolo que Reversibility) — bloqueante para activarlo.
**Riesgo de UX:** doble juicio alarga el ítem.
**Riesgo técnico:** bajo (choice + banco).
**Claim permitido:** "Entrenas a juzgar decisiones por cómo se tomaron y no por cómo salieron dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** banco κ≥0.75 registrado en `BankValidationGateTest` ANTES de alimentar perfil; diseño cruzado balanceado; adversario constante.

### DEC-13 — Presupuesto de Decisión

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; fuente: E7 de `03_6_Decisiones.md` con las 5 correcciones de su auditoría) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Optimización de asignación bajo restricciones múltiples.
**Subhabilidades:** Integración multiatributo, mantenimiento de restricciones en memoria de trabajo, eficiencia de búsqueda.
**Hipótesis cognitiva:** Asignación de recurso limitado entre ítems con coste/retorno (knapsack pequeño con solver exacto como autoridad): la señal es el ratio retorno-obtenido/óptimo y el respeto de restricciones — decisión optimizadora con verdad computable.
**Mecánica UX:** Barra de presupuesto + ítems con STEPPERS discretos (no drag, por confound motor) + panel de restricciones; confirmar; resets limitados a 2.
**Duración recomendada:** 7–10 min (4–6 problemas).
**Dificultad mínima:** 1 (3 ítems, 1 restricción, holgura amplia). **Dificultad máxima:** 10 (8 ítems, 3 restricciones, holgura mínima, restricción oculta en memoria).
**Variables de dificultad:** nº ítems, nº restricciones, holgura, varianza de retornos, restricción memorizada.
**Métricas capturadas:** asignación final, optimality ratio, violaciones, nº reasignaciones, RT total.
**Scoring específico:** normalizedScore = 0.8·optimalityRatio·(1−violationRate) + 0.2·eficiencia; violación anula el retorno del problema; null si n<10 problemas cross-sesión.
**Cómo afecta al score del bloque:** Calidad + Dificultad (CDE describe complejidad afrontada, NUNCA es la métrica de habilidad).
**Criterio de progresión:** ratio ≥0.85 estable → más restricciones.
**Criterio de fallo:** violaciones recurrentes → deload de carga.
**Anti-gaming:** fuerza bruta vía resets → límite 2 + detector de entropía de asignación baja (patrón fijo asignar-todo-al-primero) con par adversario/honesto; confirmar <500 ms invalida.
**Persistencia necesaria:** summaries.
**Feedback inmediato:** restricciones violadas resaltadas al confirmar.
**Feedback post-sesión:** tu asignación vs óptima del solver.
**Feedback histórico:** ratio medio vs línea base.
**Riesgo científico:** mide paciencia/perfeccionismo si el deadline no acota; solapa con DEC-04 → activar solo si se diferencia (optimización con restricciones vs detección de dominancia).
**Riesgo de UX:** renderer más caro del bloque.
**Riesgo técnico:** ALTO — bug del solver hace mentir feedback y score → knapsack exacto con golden exhaustivo, misma fuente para ambos.
**Claim permitido:** "Entrenas a repartir recursos limitados respetando restricciones dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** solver exacto con golden; steppers (no drag) en medida primaria; límite de resets; adversario patrón-fijo.

### DEC-14 — Caza-Sesgos

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; fuente: E6 de `03_6_Decisiones.md`; su auditoría lo deja en BLOCK con caso débil — se conserva como diseño, candidato a retirada) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Identificación metacognitiva del patrón de sesgo plantado en un ítem.
**Subhabilidades:** Veto de la respuesta intuitiva, discriminación entre familias de sesgo.
**Hipótesis cognitiva:** Doble juicio sobre ítems con UN sesgo inequívoco plantado (anclaje, tasas base, framing, coste hundido, confirmación): (a) resistir la respuesta-trampa, (b) etiquetar el patrón. La evidencia de transferencia de debiasing es débil → el claim se limita estrictamente al reconocimiento del patrón EN la tarea.
**Mecánica UX:** Escenario breve → choice de respuesta → choice de etiqueta (5 familias); explicación concisa.
**Duración recomendada:** 7–10 min.
**Dificultad mínima:** 1 (trampa evidente). **Dificultad máxima:** 10 (sesgo sutil + señuelos de etiqueta plausibles).
**Variables de dificultad:** sutileza del sesgo, atractivo de la trampa, nº de señuelos de etiqueta.
**Métricas capturadas:** accuracy de respuesta, accuracy de etiquetado, jointCorrectRate, perfil de vulnerabilidad por familia.
**Scoring específico:** normalizedScore = 0.4·accResp + 0.4·accEtiqueta + 0.2·jointCorrect; null si n<8 por familia; jointCorrect baja con accuracy alta → sospecha de azar en etiquetado (no puntúa alto).
**Cómo afecta al score del bloque:** Calidad (peso reducido; solapa con DEC-07/08/10 conductuales — si la varianza compartida es alta, se retira).
**Criterio de progresión:** joint alto → sutileza arriba, rotación weakness-first de familias.
**Criterio de fallo:** etiquetado ≈ azar → deload.
**Anti-gaming:** memorización de ítems → `excludeRecent` obligatorio (patrón `excludeRecentWordIds` de Memoria) + banco grande; RT<400 ms invalida; etiquetado al azar detectado por jointCorrectRate.
**Persistencia necesaria:** summaries + ítems servidos.
**Feedback inmediato:** dos pasos (respuesta, luego etiqueta) con explicación.
**Feedback post-sesión:** perfil de vulnerabilidad por familia (lenguaje funcional, sin juicio de rasgo).
**Feedback histórico:** tendencia por familia.
**Riesgo científico:** ALTO — overclaim de debiasing casi inevitable; banco textual cultural; requiere κ≥0.75 en familias narrativas o formato numérico.
**Riesgo de UX:** puede sentirse examen de teoría.
**Riesgo técnico:** banco grande validado = caro.
**Claim permitido:** "Entrenas a reconocer patrones de razonamiento engañoso en estos ejercicios dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** sesgo único inequívoco por ítem (validado); excludeRecent testeado; copy limitado al patrón-en-tarea (scanner); decisión explícita de retirada si solapa con DEC-07/08/10.

### DEC-15 — Riesgo-Recompensa con Valor Esperado

**Bloque:** Decisiones · **Estado:** Post-MVP (Nuevo — diseño sin código; cubre la subhabilidad riesgo/recompensa del PRD con criterio objetivo) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Elección bajo riesgo descrito con criterio de dominancia estocástica.
**Subhabilidades:** Cálculo aproximado de valor esperado, detección de opciones estocásticamente dominadas, consistencia.
**Hipótesis cognitiva:** Decisiones desde descripción (probabilidad × magnitud explícitas, recursos neutros): elegir una opción dominada estocásticamente en primer orden es un error normativo inequívoco; la actitud al riesgo entre opciones no-dominadas es preferencia legítima (covariable, jamás puntuada) — así se separa habilidad (no elegir dominadas, consistencia) de rasgo.
**Mecánica UX:** Dos opciones con barras de probabilidad y magnitud de puntos neutros; tap para elegir; 20–24 trials, mezcla de pares con dominada y pares de preferencia pura.
**Duración recomendada:** 5 min.
**Dificultad mínima:** 1 (dominancia obvia). **Dificultad máxima:** 10 (dominancia fina que exige computación, deadline suave).
**Variables de dificultad:** margen de dominancia, complejidad (2→3 desenlaces por opción), deadline.
**Métricas capturadas:** elección, tasa de dominadas, consistencia test-retest intra-sesión en pares repetidos con superficie distinta, RT.
**Scoring específico:** normalizedScore = 1 − tasa de elección dominada (solo pares con dominada); consistencia alimenta la componente Consistencia; null si n<12 pares con dominada; la actitud al riesgo NO afecta al score (test que lo fija).
**Cómo afecta al score del bloque:** Calidad + Consistencia.
**Criterio de progresión:** dominadas <10% → margen más fino y 3 desenlaces.
**Criterio de fallo:** dominadas ≥ azar → deload con tutorial visual de EV.
**Anti-gaming:** respuesta por posición → rotación + pares espejo (DEC-AG4); heurística "mayor magnitud siempre" castigada por pares donde domina la de menor magnitud (adversario sintético en la suite); <400 ms invalida; D6.
**Persistencia necesaria:** summaries.
**Feedback inmediato:** en pares con dominada, marca del atributo decisivo.
**Feedback post-sesión:** tasa de dominadas + consistencia; actitud al riesgo como observación neutral sin valoración.
**Feedback histórico:** tendencia vs línea base.
**Riesgo científico:** cuidar que el copy jamás valore la aversión/búsqueda de riesgo (rasgo).
**Riesgo de UX:** barras de probabilidad deben ser legibles sin números (redundancia forma+número).
**Riesgo técnico:** bajo (choice + generador con dominancia verificable).
**Claim permitido:** "Entrenas a comparar opciones por su probabilidad y su retorno dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** generador con dominancia estocástica verificada (property); actitud al riesgo excluida del score (test); pares espejo activos; copy sin léxico de juego de azar (scanner).

---

## 5. Lógica — 15 Exercises

**Propósito:** razonamiento deductivo/abstracto de proceso contra la propia línea base, con premisas siempre visibles (cero contaminación por memoria). **Política de scoring del bloque:** ICR process-first por ítem (motor `DefaultReasoningScorer` intocable, congelado con goldens); sesión = 0.40 Accuracy + 0.25 Difficulty + 0.15 Consistency + 0.10 Progress + 0.10 ErrorControl + 0.00 Speed (la velocidad solo resta vía impulsividad); perfil vía `LogicScoreAdapter`→`ScoreResult` con gates MinSampleGate n≥20 únicos, `BankIntegrity`, anti-gaming limpio; normalizedScore 0–1 o null, nunca fabricado. **Requisitos de banco:** generación paramétrica `structureId×surfaceVariant`, ≥30 ítems/familia, `bankVersion`+`itemVersion` persistidos, ventana de no-repetición (ítem: pool completo; estructura: ≥3 sesiones), holdout ≥20% de estructuras para retest, `BankValidator` (tamaño efectivo, balance posicional, unicidad de solución, no-trivialidad) antes de alimentar perfil, p-value/discriminación empírica registrados localmente desde el día 1.

| MVP | Ejercicio | Justificación (1 línea) |
|---|---|---|
| 1 | L1 Motor Deductivo (`logic`) | Único jugable end-to-end hoy; con banco paramétrico y dificultad aplicada es el ancla del bloque. |
| 2 | LOG-02 Matrices y Series | Cubre "patrones/series" del PRD §10.3 (0% hoy) con la 2ª mecánica de captura (visual 1-de-N). |
| 3 | LOG-03 Ordena la Derivación | 3ª mecánica (tap-order + τ Kendall) que desbloquea presentar el bloque como multi-ejercicio. |
| 4 | LOG-04 Rejilla de Restricciones | Deducción multi-restricción operativa sobre GridField compartido (A11), reutilizable por Memoria. |
| 5 | LOG-05 Caza-Contraejemplo Bajo Presión | Falsación (habilidad insignia) sobre ChoiceResponseLayer + patrón E13, coste de render mínimo. |

---

### Motor Deductivo Adaptativo (wizard)

**Bloque:** Lógica · **Estado:** Mejorar (banco paramétrico ≥30 ítems/familia con no-repetición y holdout; dificultad calculada APLICADA al contenido servido; ≥2 probes isomorfos/sesión; adapter a `ScoreResult`) · **Prioridad:** P0 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Razonamiento deductivo proposicional (inferencia necesaria).
**Subhabilidades:** NECESSARY_DEDUCTION, CONDITIONAL_REASONING, NECESSARY_SUFFICIENT, CONSISTENCY, COUNTEREXAMPLE, CONFIDENCE_CALIBRATION.
**Hipótesis cognitiva:** Paradigmas clásicos de razonamiento condicional/silogístico (modus ponens/tollens, clasificación necesario/suficiente, consistencia, refutación): la exigencia de justificar premisas+regla (process-first) mide competencia inferencial y no adivinación; near-transfer verificado con isomorfos `logicalStructureId` compartido (fuente: diseño 03_3 §1 "Cadena Deductiva", audit REVISE→correcciones integradas).
**Mecánica UX:** Wizard de 3 pasos por ítem (tarea: premisas visibles + 1 de 3-4 opciones; razonamiento: regla + premisas usadas por checkbox + justificación; confianza: LOW/MEDIUM/HIGH). Pistas acumulativas con coste. Existente en `LogicRootScreen`/`LogicStateHolder`.
**Duración recomendada:** 5 min (6 ítems) o 10 min (10 ítems); seleccionable (nuevo).
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** longitud de cadena inferencial 1–4; nº premisas 2–5 (cap anti-WM); premisas irrelevantes 0–3; densidad de falacias entre opciones 1–3; abstracción de superficie (cotidiano→simbólico); profundidad de negación 0–2.
**Métricas capturadas:** ICR y sus 6 componentes (`ReasoningScore`), accuracy, `errorTags` por `distractorType`, confianza declarada vs acierto, tiempo por ítem, hints usados, `transferTag`/probe isomorfo, `itemVersion`/`bankVersion`.
**Scoring específico:** Fórmula ICR intacta de `DefaultReasoningScorer` (0.35/0.20/0.15/0.15/0.10/0.05, penalizaciones 0.10 impulsividad + 0.15 error conceptual). Sin cambios de matemática (A5); golden tests previos.
**Cómo afecta al score del bloque:** aporta Accuracy (ICR/100 de ítems únicos), ErrorControl (errores conceptuales/overconfidence) y Difficulty (nivel servido); es la fuente principal de n para MinSampleGate.
**Criterio de progresión:** `decide` existente: ICR≥80 ∧ justificación≥0.65 ∧ TODOS los probes isomorfos correctos con ICR≥70 → INCREASE (aplicado al contenido, fix P0).
**Criterio de fallo:** ICR<55 en ventana 6 → DECREASE; error conceptual repetido ≥2 → DECREASE + MICRO_LESSON.
**Anti-gaming:** memorización del banco → D5_MEMORIZATION + D7_BANK_EXPLOIT + ventana de no-repetición + holdout; sesgo posicional → D6_POSITION_BIAS + aleatorización de posición de la correcta en el generador; marcar todas las premisas → Jaccard sobre unión (penaliza, test adversarial dedicado); adivinación rápida → penalización de impulsividad ICR + D3_SPEED_ACCURACY_HACK; farmeo de pistas → HINT_COST 0.25 en `independenceScore`.
**Persistencia necesaria:** `LogicSession`+`SkillState` actuales + `itemVersion`/`bankVersion`/`exerciseVersion`/`scoringVersion`, duración real, RT p10/p90 agregados, estado COMPLETED/ABANDONED (`SessionRecord` A12), estado de `ExposureTracker` (estructuras servidas + timestamps). Namespace `Logic` cifrado (existente).
**Feedback inmediato:** diagnóstico por `distractorType` sin revelar solución (existente, se conserva).
**Feedback post-sesión:** informe ICR desglosado + errores dominantes + recomendación (existente) + estado de calibración "X de 20 ítems / X de 5 sesiones".
**Feedback histórico:** `LogicProgressScreen` por subcapacidad, null→"—", tendencia contra línea base propia.
**Riesgo científico:** densidad de texto puede medir lectura (mitigar con modo simbólico + cap de premisas); 25–33% azar por ítem exige n alto (el ICR process-first lo amortigua).
**Riesgo de UX:** wizard de 3 pasos × N ítems es largo en taps; duración seleccionable y resumen compacto lo mitigan.
**Riesgo técnico:** generador paramétrico débil → ítems ambiguos; mitigación: validación por construcción + `BankValidator` + tests de unicidad de solución.
**Claim permitido:** "Entrenas y mides deducción justificada dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** banco ≥30 ítems/familia ×5 familias versionado; test de no-repetición inter-sesión verde; INCREASE/DECREASE cambia `difficultyLevel` servido (test); ≥2 probes isomorfos/sesión; goldens del ICR congelados; adapter emite `ScoreResult` con `BankIntegrity` correcto.

---

### Matrices y Series Paramétricas

**Bloque:** Lógica · **Estado:** Nuevo (LOG-02; memo A9-L2; panel science: reglas componibles tipo Carpenter con ítems PROPIOS, jamás Raven/CRT publicados) · **Prioridad:** P0 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Razonamiento inductivo-abstracto (inducción y extrapolación de reglas sobre patrones).
**Subhabilidades:** GENERATIVE_RULE, RELATIONAL_ANALOGY (relaciones formales), STRATEGIC_FLEXIBILITY.
**Hipótesis cognitiva:** La inducción de reglas en matrices/series con reglas componibles (progresión, distribución, XOR de atributos, alternancia) es el paradigma canónico de razonamiento abstracto; generar ítems por composición paramétrica de 1→3 reglas permite dificultad objetiva por construcción y elimina el copyright/contaminación de bancos publicados.
**Mecánica UX:** Matriz 2×2/3×3 o serie lineal de celdas con figuras generadas (forma/número/relleno/rotación); elegir la celda que completa el patrón entre 4–6 opciones (1 tap, `ChoiceResponseLayer` extendido a N opciones en grid). Confianza opcional a dificultad ≥5.
**Duración recomendada:** 5 min (10–12 ítems); opción 120 s (4 ítems, práctica).
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** nº de reglas simultáneas 1–3; tipo de regla (progresión<distribución<XOR); nº de atributos variantes 1–3; nº de opciones 4–6; sutileza de distractores (reglas parcialmente correctas); serie vs matriz.
**Métricas capturadas:** accuracy, RT por ítem (descriptivo), tipo de distractor elegido (regla-parcial vs superficial), nivel de reglas alcanzado, `itemVersion`/`bankVersion`.
**Scoring específico:** correcto/incorrecto ponderado por nº de reglas del ítem (`weightedAccuracy = Σ correct·rules / Σ rules`); mapea a ICR-compatible vía adapter (respuesta 0.7 + distractor-diagnóstico 0.3; sin premisas/regla declaradas → pesos renormalizados y documentados en `scoringVersion`). Velocidad no suma; RT < suelo por complejidad marca impulsividad.
**Cómo afecta al score del bloque:** Accuracy y Difficulty (nivel de composición de reglas sostenido); alimenta n del MinSampleGate como ítems únicos.
**Criterio de progresión:** ≥80% aciertos en el nivel con ≥8 ítems → +1 regla/atributo (staircase con histéresis, patrón Enfoque).
**Criterio de fallo:** <50% en ventana de 8 → −1 nivel; 2 niveles seguidos de caída → MICRO_LESSON del tipo de regla dominante en errores.
**Anti-gaming:** respuesta posicional fija → D6_POSITION_BIAS + posición de la correcta uniforme por generador; adivinación rápida a chance → D3_SPEED_ACCURACY_HACK (mediana RT ultra-baja + accuracy≈1/opciones) con guard de muestra; memorización → generación paramétrica seed-por-sesión + D5/D7; eliminación superficial de opciones → distractores construidos como reglas parciales (cada uno satisface k−1 de k reglas; test adversarial "greedy por 1 regla" no supera azar+ε).
**Persistencia necesaria:** resumen por sesión (accuracy por nivel de reglas, distribución de distractores) + `itemVersion`; mismo repositorio/namespace `Logic`; `SessionRecord` A12.
**Feedback inmediato:** correcto/incorrecto + qué regla faltó capturar (sin revelar la celda correcta hasta fin de sesión).
**Feedback post-sesión:** nivel de composición alcanzado, precisión por tipo de regla, comparación con propia línea base.
**Feedback histórico:** curva de nivel de reglas sostenido; "—" durante calibración.
**Riesgo científico:** practice effects fuertes en matrices (documentar `practiceEffectRisk=HIGH`; el RCI de 30 días usa SOLO probes holdout); riesgo de medir percepción visual a atributos altos (cap 3 atributos).
**Riesgo de UX:** figuras generadas deben ser legibles en pantallas pequeñas (contraste AA, tamaño celda ≥48dp).
**Riesgo técnico:** renderer de matriz nuevo (Canvas figuras paramétricas); generador debe garantizar UNICIDAD de solución (validador por construcción: ninguna opción distractora satisface todas las reglas).
**Claim permitido:** "Entrenas y mides inducción de patrones abstractos dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** generador con test de unicidad de solución y balance posicional; consistency test registry⟺catálogo ANTES de exponer; suite red-team D3/D6 con par adversario/honesto; 0 ítems de bancos publicados (revisión de contenido).

---

### Ordena la Derivación (tap-order)

**Bloque:** Lógica · **Estado:** Nuevo (LOG-03; memo A9-L3; reutiliza diseño 03_3 §8 "Ordenar la Derivación" con las correcciones que su audit exigía: TAP-ORDER en lugar de drag [elimina ruido motor], validación contra el DAG y no contra una permutación fija) · **Prioridad:** P0 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Estructura de prueba (dependencia lógica secuencial, no-circularidad).
**Subhabilidades:** PROOF_REFUTATION, NECESSARY_DEDUCTION, planificación secuencial.
**Hipótesis cognitiva:** Reconstruir el orden de una derivación (cada paso justificado solo por premisas o pasos anteriores) mide comprensión de dependencia inferencial, un constructo distinto de acertar conclusiones; τ de Kendall sobre el DAG de dependencias cuantifica orden parcial correcto aceptando CUALQUIER orden topológico válido.
**Mecánica UX:** Premisas fijas arriba (visibles siempre); 3–7 tarjetas-paso desordenadas; el usuario las TOCA en el orden que cree correcto (cada tap asigna el siguiente número de secuencia; tocar de nuevo des-asigna). Botón confirmar. Sin drag (decisión A9: menos ruido motor, renderer más barato). Señuelos descartables (paso innecesario: se deja sin numerar).
**Duración recomendada:** 5–7 min (5–7 ítems).
**Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** nº pasos 3–7; ramificación del DAG 1–3; pasos señuelo 0–2; trampa circular 0–1; diversidad de reglas 1–3.
**Métricas capturadas:** τ de Kendall normalizado contra el DAG (fracción de pares en orden de dependencia válido), orden topológico exacto sí/no, señuelos incluidos, errores de circularidad, tiempo hasta primer tap y total, `itemVersion`.
**Scoring específico:** `orderScore = τ_DAG` (1.0 = orden topológico válido; cualquier topológico puntúa 1.0); señuelo incluido → error conceptual (−0.15 análogo ICR); circularidad → error conceptual etiquetado. Adapter mapea a ICR-compatible (orden 0.6 + exclusión de señuelos 0.25 + confianza 0.15).
**Cómo afecta al score del bloque:** Accuracy (τ medio) + Difficulty (nº pasos/ramificación sostenidos); ítems únicos cuentan para n.
**Criterio de progresión:** τ≥0.85 medio con ≥5 ítems en el nivel → +1 paso o +señuelo.
**Criterio de fallo:** τ<0.5 medio en ventana → −1 nivel + MICRO_LESSON de dependencia ("un paso solo puede usar lo anterior").
**Anti-gaming:** permutación aleatoria rápida → detector nuevo **LOG-D_RANDOM_ORDER** (especificación: τ medio ≈ E[τ_azar]±banda con tiempo-por-decisión bajo el suelo → verdict WITHHOLD; par adversario "taps aleatorios en <800ms/paso" / honesto en suite red-team); orden fijo de lectura (tocar siempre en el orden mostrado) → aleatorizar orden de presentación de tarjetas por seed y comparar τ del orden-mostrado (si el orden mostrado puntuara alto por azar sería trivial: el generador GARANTIZA que la presentación nunca es un orden topológico válido); memorización → paramétrico + D5/D7.
**Persistencia necesaria:** resumen (τ medio, exactos, señuelos, circularidades) + `orderedStepIds` en el attempt; `SessionRecord` A12.
**Feedback inmediato:** nº de pasos en posición válida (sin revelar el orden); si circularidad: qué paso se usó antes de justificarse.
**Feedback post-sesión:** τ medio, exactos/total, tipo de error dominante.
**Feedback histórico:** curva de τ por nivel de pasos; "—" en calibración.
**Riesgo científico:** con pasos ≤3 el azar produce τ alto (mínimo 4 pasos para ítems que alimentan perfil; 3 pasos solo tutorial).
**Riesgo de UX:** tap-order exige affordance clara de "numerado" (badge visible por tarjeta); deshacer debe ser obvio.
**Riesgo técnico:** renderer de secuencia nuevo (lista numerable, sin drag — coste bajo); `OrderingValidator` debe validar contra DAG (portar τ de `ClosedScoringEngine.Ordering` de Lenguaje y extender a orden parcial).
**Claim permitido:** "Entrenas y mides la reconstrucción del orden de una derivación dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** `OrderingValidator` acepta todo orden topológico válido (test con DAG ramificado y ≥2 órdenes válidos); presentación nunca-topológica garantizada por generador (test); detector LOG-D_RANDOM_ORDER con par adversario/honesto; consistency test antes de exponer.

---

### Rejilla de Restricciones

**Bloque:** Lógica · **Estado:** Nuevo (LOG-04; memo A9-L4; panel science "deducción por rejilla de restricciones"; GridField compartido NUEVO en `core/paradigm/ui` — A11, reutilizable por Memoria/Corsi) · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Deducción multi-restricción (integración de constraints para determinar asignaciones únicas).
**Subhabilidades:** MULTISTEP_CONSTRAINTS, CONSISTENCY, EXTERNAL_REPRESENTATION.
**Hipótesis cognitiva:** Los puzles de asignación con restricciones (entidad×atributo, estilo lógica de cuadrícula) exigen encadenar eliminaciones deductivas; la rejilla externaliza el estado (premisas y tablero visibles → mide deducción, no memoria), y la solución única por construcción hace la corrección objetiva.
**Mecánica UX:** 3–6 restricciones visibles (texto corto neutro: "A no está en la fila 2", "B está junto a C"); rejilla N×M (entidades 2–5) donde cada celda se marca por tap cíclico (vacío→sí→no→vacío) o se asigna entidad por selector; botón confirmar solución. GridField nuevo compartido.
**Duración recomendada:** 5–10 min (2–4 puzles).
**Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** entidades 2–5; nº restricciones 3–8; profundidad de encadenamiento (pasos de eliminación necesarios) 1–4; restricciones negativas/relacionales ratio; restricción irrelevante 0–1.
**Métricas capturadas:** solución exacta sí/no, accuracy de celdas (celdas correctas/total), nº de correcciones (taps de rectificación), tiempo por puzle, uso de restricción irrelevante, `itemVersion`.
**Scoring específico:** `puzzleScore = 1.0` si solución exacta; parcial = accuracy de celdas × 0.6 (cap: sin solución exacta nunca >0.6); correcciones no penalizan (rectificar es deducción legítima); impulsividad si confirma bajo el suelo de tiempo por complejidad. Adapter → ICR-compatible.
**Cómo afecta al score del bloque:** Accuracy + Difficulty (entidades×profundidad sostenidas); cada puzle único cuenta para n (nMin en puzles: ≥12 para EMERGING de esta familia, agregado al n global del bloque).
**Criterio de progresión:** 2 puzles exactos consecutivos en el nivel → +1 entidad o +1 profundidad.
**Criterio de fallo:** 2 puzles <60% de celdas → −1 nivel + MICRO_LESSON ("empieza por la restricción más determinante").
**Anti-gaming:** relleno aleatorio → banda de azar por tamaño de rejilla (accuracy de celdas ≈ chance + tiempo bajo → D3_SPEED_ACCURACY_HACK adaptado con chanceAccuracy por rejilla); ensayo-error masivo → feedback SOLO al confirmar (sin validación por celda, gate de diseño) + nº de confirmaciones limitado (1 por puzle a dificultad ≥5); memorización → generador paramétrico (permutación de entidades/atributos por seed) + D5/D7; abandono selectivo de puzles difíciles → D11_ABANDONMENT_GAMING.
**Persistencia necesaria:** resumen por puzle (exacto, accuracy celdas, nivel, tiempo) + `gridCells` en attempt; `SessionRecord` A12 con checkpoint por puzle.
**Feedback inmediato:** al confirmar: exacto o nº de celdas correctas (sin señalar cuáles a dificultad ≥5).
**Feedback post-sesión:** puzles exactos/total, profundidad alcanzada, patrón de error (restricciones relacionales vs negativas).
**Feedback histórico:** nivel de entidades×profundidad sostenido; "—" en calibración.
**Riesgo científico:** a entidades altas puede medir estrategia de anotación más que deducción (la rejilla ES la anotación externa: se acepta y documenta como parte del constructo).
**Riesgo de UX:** rejilla 5×5 en móvil pequeño (celdas ≥44dp, scroll/zoom; validar con fontScale ampliado).
**Riesgo técnico:** GridField compartido nuevo (mayor coste de render del bloque); generador con solución única por construcción (solver interno que verifica unicidad — test).
**Claim permitido:** "Entrenas y mides deducción con restricciones múltiples dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** solver de unicidad en el generador (test: todo puzle emitido tiene exactamente 1 solución); GridField con contrato en `core/paradigm/ui` + test de captura; banda de azar calibrada por tamaño (test adversarial de relleno aleatorio → WITHHOLD); consistency test antes de exponer.

---

### Caza-Contraejemplo Bajo Presión

**Bloque:** Lógica · **Estado:** Nuevo (LOG-05; memo A9-L5; reutiliza diseño 03_3 §4 "Caza-Contraejemplo" [audit BLOCK] corrigiendo su bloqueante: generador validado por `CounterexampleValidator` en construcción + penalización explícita de near-miss; deadline reusa el patrón E13 de presión temporal de Enfoque sobre ChoiceResponseLayer) · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Falsación (construir/reconocer el caso que invalida una regla).
**Subhabilidades:** COUNTEREXAMPLE, FALSIFYING_INDUCTION, CONDITIONAL_REASONING, ADVERSARIAL_RESISTANCE (presión temporal).
**Hipótesis cognitiva:** Reconocer el contraejemplo válido (premisas verdaderas ∧ conclusión falsa, autoridad: `CounterexampleValidator.invalidates`) discrimina falsación de refutación superficial (romper una premisa = near-miss); el deadline decreciente añade resistencia a la presión SIN premiar velocidad (la ventana solo acota, el acierto puntúa igual a 8 s que a 3 s).
**Mecánica UX:** Regla visible arriba (cue persistente); 4–6 tarjetas-caso con 2–3 atributos; elegir EL caso contraejemplo o "no hay contraejemplo" (1 tap, ChoiceResponseLayer); countdown visible 8 s→3 s según dificultad. Sin timeout-castigo: timeout = omisión (no error conceptual).
**Duración recomendada:** 120 s (rápida, 8–10 ítems) o 5 min (18–22 ítems).
**Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline 8000→3000 ms; nº casos 4–6; near-misses 1–3; ratio sin-contraejemplo 0.2–0.4; cuantificador de la regla (todos/si-entonces/ningún); dimensionalidad de caso 2–3 (cap perceptivo).
**Métricas capturadas:** accuracy, tasa de near-miss (romper-premisa elegido), falsas alarmas en ítems sin contraejemplo, omisiones por timeout, RT (descriptivo), `itemVersion`.
**Scoring específico:** acierto = contraejemplo válido según `CounterexampleValidator` o "no hay" correcto; near-miss = error conceptual (etiqueta BROKE_PREMISE, −0.15 análogo); omisión ≠ error conceptual (no penaliza doble). Velocidad JAMÁS suma: deadline es condición de captura, no multiplicador.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl (near-miss/falsas alarmas); Difficulty por deadline+casos sostenidos.
**Criterio de progresión:** ≥80% aciertos con ≤10% omisiones en el nivel → deadline −1 s o +1 near-miss.
**Criterio de fallo:** omisiones >30% → deadline +1 s (la presión no debe convertirse en azar); accuracy <50% → −1 nivel + MICRO_LESSON ("un contraejemplo mantiene las premisas y rompe la conclusión").
**Anti-gaming:** responder siempre "no hay contraejemplo" → ratio sin-contraejemplo balanceado (0.2–0.4) + sesgo de respuesta monitorizado (entropía de elección, D6_POSITION_BIAS adaptado a categoría); esperar siempre el timeout para no arriesgar → tasa de omisión como señal (espejo de D2_WAITING_STRATEGY: omisiones>50% con RT de aciertos normal → WITHHOLD de la sesión para perfil); tap ultra-rápido a chance → D3_SPEED_ACCURACY_HACK + D4_TOO_FAST (floor 300 ms para elección con lectura); memorización → paramétrico + D5/D7.
**Persistencia necesaria:** resumen (accuracy, near-miss rate, omisiones, deadline medio) + `SessionRecord` A12.
**Feedback inmediato:** si near-miss: "ese caso rompe una premisa, no la conclusión" (diagnóstico, sin revelar el correcto).
**Feedback post-sesión:** precisión, tasa near-miss, omisiones, deadline sostenido.
**Feedback histórico:** accuracy por deadline; "—" en calibración.
**Riesgo científico:** presión temporal puede contaminar la señal de falsación con velocidad de lectura (mitigar: deadline mínimo 3 s + casos de ≤3 atributos + la métrica de perfil usa solo ítems respondidos).
**Riesgo de UX:** countdown genera ansiedad en algunos usuarios (copy neutro, sin rojo agresivo hasta el último segundo; abandono seguro nunca penaliza).
**Riesgo técnico:** bajo (ChoiceResponseLayer + countdown existente en patrón E13); el generador de near-misses exige `CounterexampleValidator` aplicado en construcción (test de que cada near-miss rompe exactamente una premisa).
**Claim permitido:** "Entrenas y mides la detección de contraejemplos bajo presión de tiempo dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** generador etiqueta cada caso vía `CounterexampleValidator` (test de validez + unicidad del contraejemplo); balance sin-contraejemplo (test); detector de waiting/omisión con par adversario/honesto; omisión nunca cuenta como error conceptual (test).

---

### Tarea de Selección Condicional (Wason paramétrico)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-06; panel science; ítems propios paramétricos, nunca la formulación publicada literal) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Comprobación de reglas condicionales (qué evidencia es necesaria para testear "si P entonces Q").
**Subhabilidades:** CONDITIONAL_REASONING, FALSIFYING_INDUCTION, VALUE_OF_INFORMATION.
**Hipótesis cognitiva:** El paradigma de selección (elegir qué tarjetas voltear para verificar una regla condicional) mide la estrategia de falsación P∧¬Q frente al sesgo de confirmación; versiones paramétricas con contenido neutro y variantes deónticas/abstractas cruzadas separan facilitación de contenido de competencia formal.
**Mecánica UX:** Regla condicional + 4 tarjetas (P, ¬P, Q, ¬Q con superficies paramétricas); selección múltiple de las tarjetas a comprobar; confirmar.
**Duración recomendada:** 5 min (8–10 ítems). **Dificultad mínima:** 3. **Dificultad máxima:** 9.
**Variables de dificultad:** abstracto vs temático neutro; negaciones en la regla 0–2; nº tarjetas 4–6; regla bicondicional camuflada 0–1.
**Métricas capturadas:** selección exacta {P,¬Q}, sobre-selección (Q incluida = confirmación), sub-selección, `errorTags`.
**Scoring específico:** exacto = 1.0; Jaccard con {P,¬Q} como parcial; Q seleccionada = error conceptual CONFIRMATION_BIAS (etiqueta existente).
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** ≥3 exactos consecutivos → +negación/+bicondicional.
**Criterio de fallo:** CONFIRMATION_BIAS repetido ≥2 → MICRO_LESSON de falsación.
**Anti-gaming:** patrón fijo "marcar P y Q siempre" → entropía de selección (D6 adaptado); memorización de la plantilla → rotación paramétrica de superficies + D5/D7; marcar todo → Jaccard sobre unión penaliza (test adversarial).
**Persistencia necesaria:** resumen estándar + `itemVersion`; `SessionRecord`.
**Feedback inmediato:** qué tarjeta seleccionada no aporta información (diagnóstico).
**Feedback post-sesión:** tasa de estrategia falsadora vs confirmatoria.
**Feedback histórico:** tendencia de selección exacta; "—" en calibración.
**Riesgo científico:** el efecto de contenido es enorme en la literatura (versión temática mucho más fácil): reportar abstracto/temático por separado, jamás mezclados.
**Riesgo de UX:** la tarea es contraintuitiva; requiere tutorial de 1 ítem guiado.
**Riesgo técnico:** bajo (selección múltiple existente).
**Claim permitido:** "Entrenas y mides qué evidencia comprueba una regla condicional dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** formulaciones propias (0 ítems publicados); separación abstracto/temático en métricas; test adversarial de marcar-todo.

---

### Silogismo Categórico anti Sesgo de Creencia

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-07; reutiliza diseño 03_3 §5 [audit REVISE]: belief-bias index solo agregado históricamente, paridad de contenido congruente/incongruente por generador, n/confident reportados) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Juicio de validez formal independiente del contenido (control del sesgo de creencia).
**Subhabilidades:** QUANTIFIERS, NECESSARY_DEDUCTION, ADVERSARIAL_RESISTANCE.
**Hipótesis cognitiva:** Cruzar validez×credibilidad (conclusión creíble-inválida / increíble-válida) mide la capacidad de juzgar la FORMA aislando el contenido; el delta congruente-incongruente (belief-bias index) es la métrica de serie, nunca por sesión.
**Mecánica UX:** 2 premisas categóricas + conclusión; tap binario válido/inválido + confianza; a dificultad alta, modo abstracto (X/Y/Z).
**Duración recomendada:** 5 min (10 ítems). **Dificultad mínima:** 3. **Dificultad máxima:** 9.
**Variables de dificultad:** figura silogística 1–4; congruencia creencia-validez −1..+1; premisas particulares ("algunos") 0–2; abstracción de contenido 0–2; ratio inválidos 0.4–0.6.
**Métricas capturadas:** accuracy de validez, accuracy por congruencia (separadas, con n), BELIEF_BIAS en `errorTags`, confianza.
**Scoring específico:** ICR estándar con `correct`=juicio de validez; error belief-congruente = error conceptual (DistractorType.BELIEF_BIAS ya existe).
**Cómo afecta al score del bloque:** Accuracy + ErrorControl; el belief-bias index es SOLO informativo (nunca entra al score).
**Criterio de progresión:** accuracy≥80% + isomorfos → figuras difíciles/particulares.
**Criterio de fallo:** errores belief-congruentes ≥2 → MICRO_LESSON con isomorfo abstracto de la MISMA estructura.
**Anti-gaming:** respuesta fija "inválido" → ratio balanceado + entropía de respuesta (D6 adaptado a categoría); binario 50% azar → n alto exigido + process-scoring; memorización → paramétrico + D5/D7.
**Persistencia necesaria:** resumen + accuracy por congruencia con n; `SessionRecord`.
**Feedback inmediato:** separa validez de verdad-en-el-mundo (núcleo pedagógico).
**Feedback post-sesión:** accuracy congruente vs incongruente (con n y "provisional").
**Feedback histórico:** belief-bias index SOLO agregado cross-sesión con n≥20/celda (A2 difference-scores), si no "—".
**Riesgo científico:** contenido creíble varía culturalmente → paridad de contenido controlada por generador + modo abstracto como ancla.
**Riesgo de UX:** monotonía del binario (sesiones cortas).
**Riesgo técnico:** bajo (choice binario existente).
**Claim permitido:** "Entrenas y mides el juicio de validez formal separado del contenido dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** generador cruza validez×credibilidad balanceado (test); index solo con ≥20/celda agregado; par adversario respuesta-fija/honesto.

---

### Detector de Contradicción Latente (conjuntos)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-08; reutiliza diseño 03_3 §2 [audit REVISE]: cap 5 afirmaciones, unicidad del núcleo mínimo garantizada por generador, d′ solo agregado histórico; evoluciona la familia CONTRADICTION_DETECTION del motor L1 a ejercicio autónomo de conjuntos) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Detección de consistencia de un conjunto de afirmaciones.
**Subhabilidades:** CONSISTENCY, CONDITIONAL_REASONING, control inhibitorio (resistir el conjunto plausible-inconsistente).
**Hipótesis cognitiva:** Juzgar si un conjunto es consistente y aislar el núcleo mínimo contradictorio (directo o derivado en 1–2 pasos) mide razonamiento sobre consistencia, distinto de derivar hacia delante; los conjuntos consistentes balanceados controlan el sesgo "siempre hay trampa".
**Mecánica UX:** 3–5 tarjetas-afirmación; tap binario consistente/inconsistente; si inconsistente, marcar el par/triple mínimo; confianza.
**Duración recomendada:** 5–7 min (7–9 ítems). **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** nº afirmaciones 3–5 (cap audit); profundidad de derivación 0–2; ratio consistentes 0.3–0.5; pares señuelo 0–3; tamaño de núcleo 2–3.
**Métricas capturadas:** accuracy del binario, Jaccard del núcleo, falsas alarmas, hits/FA para d′ agregado, confianza.
**Scoring específico:** ICR con `correct`=binario; núcleo vía Jaccard (solo en inconsistentes, renormalizado en consistentes); falsa alarma = error conceptual (FALSE_CONTRADICTION ya existe).
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** accuracy≥80% + núcleos correctos → +profundidad de derivación.
**Criterio de fallo:** falsas alarmas ≥2 → MICRO_LESSON ("plausible no es inconsistente").
**Anti-gaming:** decir siempre "inconsistente" → ratio balanceado + d′/sesgo de respuesta monitorizado (agregado); acertar el binario adivinando → núcleo mínimo exigido para puntuar completo; memorización → paramétrico + D5/D7.
**Persistencia necesaria:** resumen + hits/FA por categoría para agregación histórica; `SessionRecord`.
**Feedback inmediato:** si el par marcado no choca: "esas dos pueden ser verdad a la vez".
**Feedback post-sesión:** aciertos, falsas alarmas, núcleos exactos.
**Feedback histórico:** d′ SOLO agregado con ≥20 señal + ≥20 ruido (corrección log-lineal, A2); antes "—".
**Riesgo científico:** conjuntos grandes miden retención (cap 5 + siempre visibles).
**Riesgo de UX:** doble respuesta (binario+núcleo) alarga el ítem.
**Riesgo técnico:** generador debe garantizar unicidad del núcleo mínimo o aceptar cualquier núcleo válido (test).
**Claim permitido:** "Entrenas y mides la detección de inconsistencias dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** unicidad/aceptación-múltiple del núcleo resuelta por generador con test; d′ agregado gated por n; balance de consistentes.

---

### Inducción de Reglas (2-4-6)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-09; panel science "inducción de reglas, paradigma 2-4-6"; diseño nuevo) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Inducción activa de reglas (generar y testear hipótesis).
**Subhabilidades:** GENERATIVE_RULE, FALSIFYING_INDUCTION, VALUE_OF_INFORMATION.
**Hipótesis cognitiva:** Descubrir una regla oculta proponiendo tripletas y recibiendo sí/no mide estrategia de test de hipótesis; la métrica clave es la proporción de tests NEGATIVOS (intentos de falsar la propia hipótesis) antes de declarar la regla — combate la confirmación.
**Mecánica UX:** El sistema tiene una regla oculta paramétrica sobre secuencias numéricas cortas; el usuario compone tripletas con steppers (sin texto libre), recibe sí/no cumple, y cuando cree saber la regla la declara eligiendo entre 4–6 formulaciones candidatas; presupuesto de 6–10 tests por ronda.
**Duración recomendada:** 5–10 min (2–4 rondas). **Dificultad mínima:** 3. **Dificultad máxima:** 9.
**Variables de dificultad:** generalidad de la regla (específica→amplia); presupuesto de tests; nº de formulaciones candidatas; señuelos de regla-más-específica.
**Métricas capturadas:** regla correcta sí/no, nº de tests usados, proporción de tests negativos (hipótesis-falsadores), declaración prematura, tiempo.
**Scoring específico:** acierto de regla 0.6 + calidad de estrategia 0.4 (proporción de tests negativos con zona muerta amplia: ≥0.25 = pleno); declarar sin ningún test negativo con regla errónea = error conceptual CONFIRMATION_BIAS.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl; Difficulty por generalidad de regla.
**Criterio de progresión:** 2 rondas correctas con estrategia mixta → reglas más amplias/candidatas más próximas.
**Criterio de fallo:** declaraciones prematuras repetidas → MICRO_LESSON ("intenta romper tu propia regla").
**Anti-gaming:** farmear tests triviales (repetir tripletas casi idénticas) → penalización de tests duplicados (no cuentan al presupuesto ni a la estrategia, test adversarial); declarar al azar entre candidatas → 4–6 candidatas + n de rondas para el gate; ritmo mecánico → D1_RHYTHM_TAPPING.
**Persistencia necesaria:** resumen por ronda (regla, tests, negativos, resultado); `SessionRecord`.
**Feedback inmediato:** sí/no por tripleta (es la mecánica).
**Feedback post-sesión:** estrategia (tests negativos) + resultado por ronda.
**Feedback histórico:** proporción de estrategia falsadora en el tiempo; "—" en calibración.
**Riesgo científico:** pocas rondas por sesión → n bajo (agregación multi-sesión obligatoria antes de perfil).
**Riesgo de UX:** frustración si la regla amplia se resiste (presupuesto visible + salida segura sin penalización).
**Riesgo técnico:** renderer de composición de tripletas nuevo (steppers, coste bajo); banco de reglas paramétricas con verificador determinista.
**Claim permitido:** "Entrenas y mides tu estrategia para descubrir reglas dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** verificador determinista de regla (test); tests duplicados neutralizados (test adversarial); zona muerta de estrategia documentada en `scoringVersion`.

---

### Inferencia Transitiva Encadenada

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-10; panel science "inferencia transitiva/ordenación"; diseño nuevo) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Inferencia relacional transitiva (integrar relaciones de orden A>B, B>C → A>C).
**Subhabilidades:** RELATIONAL_ANALOGY (relaciones formales de orden), NECESSARY_DEDUCTION, INDETERMINACY.
**Hipótesis cognitiva:** Responder sobre pares NO adyacentes tras ver premisas relacionales mide integración transitiva; el caso crítico es reconocer INDETERMINACIÓN (pares no conectados por la cadena: "no se puede saber"), que discrimina inferencia real de heurística posicional.
**Mecánica UX:** 3–5 premisas relacionales visibles (barras/entidades neutras); pregunta sobre un par (¿X mayor que Z?) con 3 opciones: sí/no/no-determinable (ChoiceResponseLayer).
**Duración recomendada:** 120 s–5 min (8–14 ítems). **Dificultad mínima:** 2. **Dificultad máxima:** 8.
**Variables de dificultad:** longitud de cadena 3–5; pares no determinables ratio 0.2–0.4; relaciones mixtas (mayor/menor entremezcladas); premisas en orden no canónico.
**Métricas capturadas:** accuracy por distancia del par, accuracy en no-determinables, errores de sobre-inferencia (OVERINFERENCE existente), RT descriptivo.
**Scoring específico:** ICR-compatible: correcto 0.7 + no-determinable correcto ponderado ×1.2 dentro del cap (señal más diagnóstica) + confianza 0.3; sobre-inferir un par no conectado = error conceptual OVERINFERENCE.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl (sobre-inferencia).
**Criterio de progresión:** ≥80% con no-determinables ≥70% → +longitud/orden no canónico.
**Criterio de fallo:** sobre-inferencia repetida → MICRO_LESSON ("la cadena no conecta esos dos").
**Anti-gaming:** responder siempre "sí" o nunca "no-determinable" → balance de ratio + entropía de categoría (D6 adaptado); memorización del orden lineal → premisas en orden no canónico rotadas por seed + D5; rapidez a chance → D3.
**Persistencia necesaria:** resumen (accuracy por distancia y por determinabilidad); `SessionRecord`.
**Feedback inmediato:** si sobre-inferencia: "ninguna cadena conecta esos elementos".
**Feedback post-sesión:** accuracy por tipo; capacidad de reconocer indeterminación.
**Feedback histórico:** tendencia por longitud de cadena; "—" en calibración.
**Riesgo científico:** con premisas visibles la carga de memoria desaparece (intencional) pero el efecto de distancia simbólica se atenúa — se mide integración, no memoria (documentar).
**Riesgo de UX:** enunciados relacionales repetitivos (variar superficies neutras).
**Riesgo técnico:** bajo (choice de 3 + generador de cadenas con verificador de conectividad).
**Claim permitido:** "Entrenas y mides la integración de relaciones de orden dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** verificador de determinabilidad en generador (test); balance de no-determinables; par adversario "nunca-indeterminado"/honesto.

---

### Transferencia Espejo (gemelo/isomorfo)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-11; reutiliza diseño 03_3 §7 [audit BLOCK por banco: exige ≥8 estructuras isomórficas ×≥2 superficies]; se desbloquea SOLO cuando el banco paramétrico L1 alcance ese piso; mientras, los probes isomorfos siguen viviendo DENTRO de L1 vía `transferConsistencyFor`) · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Transferencia estructural near (abstraer la forma lógica de la superficie).
**Subhabilidades:** FUNCTIONAL_TRANSFER, todos los sub-skills entrenados (meta-ejercicio).
**Hipótesis cognitiva:** Presentar un ítem entrenado y más tarde su gemelo isomorfo (mismo `logicalStructureId`, distinto `surfaceVariantId`, interleaving ≥2 ítems) distingue abstracción estructural de memorización de superficie; transfer=1.0 exige AMBOS lados correctos (regla 2026-06-15, ya implementada).
**Mecánica UX:** Hereda el renderer del paradigma base (wizard/choice); fase A entrena, fase B interleaved presenta el isomorfo; overlay post-isomorfo muestra la estructura compartida.
**Duración recomendada:** 7–10 min (4–6 pares). **Dificultad mínima:** 3. **Dificultad máxima:** 9.
**Variables de dificultad:** distancia de superficie 1–4; gap de interleaving 2–6; decoys isomórficos (misma superficie, otra estructura) 0–2.
**Métricas capturadas:** `transferConsistencyFor` (1.0/0.0), delta ICR gemelo→isomorfo, falsas alarmas en decoys, confianza.
**Scoring específico:** el ICR del isomorfo puntúa estándar; el transfer score NO entra al ICR del trial — modula la progresión (insumo directo de `highPerformanceDecision`).
**Cómo afecta al score del bloque:** no suma directo; gatea INCREASE y alimenta Progress (RCI sobre probes).
**Criterio de progresión:** transferencia consistente → +distancia de superficie.
**Criterio de fallo:** gemelo OK + isomorfo mal repetido → HOLD + MICRO_LESSON de abstracción.
**Anti-gaming:** es EL antídoto contra D5_MEMORIZATION (memorización sin transferencia queda expuesta); decoys castigan sobre-generalización; el propio D5 usa esta señal ("no isomorph transfer").
**Persistencia necesaria:** `transferLevel` por attempt (existente) + pares servidos en `ExposureTracker`.
**Feedback inmediato:** "transferido"/"la estructura se te escapó" sin revelar solución.
**Feedback post-sesión:** tasa de transferencia con n; "calibrando transferencia" bajo n.
**Feedback histórico:** transfer rate agregado; "—" hasta n suficiente de pares.
**Riesgo científico:** transfer=0 frecuente al inicio parece estancamiento (comunicación "en calibración" obligatoria, corrección del audit).
**Riesgo de UX:** el usuario puede percibir repetición ("esto ya lo vi") — copy explica que es la fase de aplicación.
**Riesgo técnico:** BLOQUEADO por banco (≥8 estructuras ×2 superficies); decoys validados por tests.
**Claim permitido:** "Entrenas y mides la aplicación de una misma estructura lógica en formas nuevas dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** banco ≥8 estructuras ×≥2 variantes (gate de activación); transfer mostrado solo con n suficiente; decoys con test de estructura distinta verificada.

---

### Inferencia Suprimida (defeasible explícito)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-12; reutiliza diseño 03_3 §9 [audit BLOCK] con SU corrección obligatoria: condiciones habilitadoras DECLARADAS en el enunciado — lógica defeasible explícita, nunca conocimiento del mundo) · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Distinguir deducción monótona de inferencia revisable con condiciones explícitas.
**Subhabilidades:** BELIEF_REVISION, CONDITIONAL_REASONING, INDETERMINACY.
**Hipótesis cognitiva:** Cuando una premisa adicional EXPLÍCITA introduce una condición habilitadora no garantizada, la inferencia plausible se suprime pero la deducción estricta no; juzgar cuál es cuál mide monotonicidad, con TODO el conocimiento necesario dentro del ítem (corrección del audit: cero dependencia cultural).
**Mecánica UX:** Secuencia de revelado (condicional+hecho → aparece premisa extra, micro-secuencia LaunchedEffect patrón E10/E8 de Enfoque); 3 opciones: se-sigue / ya-no-se-sigue / la premisa extra es irrelevante; confianza.
**Duración recomendada:** 5 min (8–10 ítems). **Dificultad mínima:** 3. **Dificultad máxima:** 8.
**Variables de dificultad:** tipo de premisa extra (irrelevante/alternativa/habilitadora); sutileza; ratio de controles estrictos 0.3–0.5; negaciones.
**Métricas capturadas:** accuracy 3-way, supresión espuria (suprimir deducción válida), habilitadora ignorada, confianza, `errorTags` (MONOTONICITY como nuevo tag).
**Scoring específico:** ICR-compatible; supresión espuria y habilitadora-ignorada = errores conceptuales tipados distintos.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** discriminación estricto/habilitadora ≥80% → +sutileza.
**Criterio de fallo:** supresión espuria repetida → MICRO_LESSON ("una deducción válida no se suprime").
**Anti-gaming:** respuesta evasiva fija ("irrelevante" siempre) → balance de tipos + entropía de categoría; memorización → paramétrico + D5/D7; ritmo → D1.
**Persistencia necesaria:** resumen por tipo de premisa extra; `SessionRecord`.
**Feedback inmediato:** distingue "candado" (estricta) de "condición no garantizada".
**Feedback post-sesión:** accuracy por tipo con n.
**Feedback histórico:** discriminación agregada ≥20/celda; antes "—".
**Riesgo científico:** aún con habilitadoras explícitas la pragmática del lenguaje pesa (validar ítems con pilotos internos; si no discrimina, RETIRE).
**Riesgo de UX:** revelado secuencial no debe poder saltarse (gate de lectura mínima).
**Riesgo técnico:** micro-secuencia de revelado en renderer (patrón existente en Enfoque).
**Claim permitido:** "Entrenas y mides cuándo una conclusión deja de seguirse al añadir condiciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** toda condición habilitadora declarada en el enunciado (revisión de banco); controles estrictos balanceados; discriminación con n gate.

---

### Analogía Estructural Formal

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-13; reutiliza diseño 03_3 §10 [audit BLOCK riesgo cultural ROJO] bajo su condición de supervivencia: relaciones EXCLUSIVAMENTE formales-abstractas — contención, orden, oposición, causalidad simple — sobre estímulos visuales, cero dependencia léxica) · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Mapeo relacional (preservar la relación, no el rasgo superficial).
**Subhabilidades:** RELATIONAL_ANALOGY, ABDUCTION, inhibición de similitud superficial.
**Hipótesis cognitiva:** Completar A:B::C:D sobre relaciones formales definidas operacionalmente mide mapeo estructural; el distractor superficial fuerte (rasgo compartido con C) discrimina mapeo de coincidencia.
**Mecánica UX:** Par fuente con relación etiquetada visualmente (icono/flecha, estímulos geométricos generados); C + 4–5 opciones (choice); confianza.
**Duración recomendada:** 5 min (8–10 ítems). **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** nº relaciones simultáneas 1–2 (cap audit: 3 mide WM); fuerza del lure superficial 1–3; relación invertida presente 0–1; distancia de dominio visual.
**Métricas capturadas:** accuracy, tasa de lure superficial (SURFACE_MATCH existente), relación invertida elegida, confianza.
**Scoring específico:** ICR-compatible; lure superficial e inversión = errores conceptuales tipados.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** accuracy≥80% con lure fuerte → +relación compuesta (máx 2).
**Criterio de fallo:** SURFACE_MATCH repetido → MICRO_LESSON ("mapea el vínculo, no el parecido").
**Anti-gaming:** posición fija → D6 + aleatorización; memorización → generación por composición de relaciones formales + D5/D7; rapidez a chance → D3.
**Persistencia necesaria:** resumen estándar; `SessionRecord`.
**Feedback inmediato:** al fallar por lure: señala el rasgo que sedujo vs la relación pedida.
**Feedback post-sesión:** accuracy + tasa de lure.
**Feedback histórico:** tendencia; "—" en calibración.
**Riesgo científico:** ROJO cultural si cualquier relación exige vocabulario (gate de contenido: revisión de que toda relación es formal y visual; si una depende de léxico, se descarta el ítem).
**Riesgo de UX:** iconografía de relaciones debe aprenderse (tutorial 2 ítems).
**Riesgo técnico:** renderer de estímulos geométricos (comparte generador visual con LOG-02).
**Claim permitido:** "Entrenas y mides el mapeo de relaciones estructurales dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** banco 100% relaciones formales-visuales (auditoría de contenido); definición operacional de "superficial" documentada; cap de 2 relaciones.

---

### Reparación Lógica (premisa mínima)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-14; diseño nuevo sobre la subhabilidad LOGICAL_REPAIR de la taxonomía existente; sustituye a la Inferencia Bayesiana Cualitativa del catálogo previo [03_3 §6, BLOCK→RETIRE], cuyo constructo pasa a Decisiones D2) · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Reparación de argumentos inválidos (identificar la premisa mínima que restaura la validez).
**Subhabilidades:** LOGICAL_REPAIR, MISSING_PREMISE, HIDDEN_ASSUMPTION.
**Hipótesis cognitiva:** Dado un argumento inválido por premisa ausente, elegir la premisa que lo hace válido (y rechazar las que solo lo hacen persuasivo) mide comprensión constructiva de la validez — complementa la detección (L1) con reparación.
**Mecánica UX:** Argumento con hueco visible; 4 premisas candidatas (1 repara la validez, señuelos: repara-plausibilidad, redundante, contradictoria); choice + confianza.
**Duración recomendada:** 5 min (8 ítems). **Dificultad mínima:** 3. **Dificultad máxima:** 8.
**Variables de dificultad:** profundidad del hueco (1 paso vs encadenado); sutileza del señuelo persuasivo; negaciones; abstracción.
**Métricas capturadas:** accuracy, elección de señuelo persuasivo (BELIEF_BIAS/OVERINFERENCE), confianza, RT descriptivo.
**Scoring específico:** ICR-compatible; señuelo persuasivo = error conceptual tipado.
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** ≥80% → huecos encadenados.
**Criterio de fallo:** persuasivo repetido → MICRO_LESSON ("válida ≠ convincente").
**Anti-gaming:** posición → D6 + aleatorización; memorización → paramétrico + D5/D7; rapidez → D3 + impulsividad ICR.
**Persistencia necesaria:** resumen estándar; `SessionRecord`.
**Feedback inmediato:** por qué la candidata elegida no cierra el hueco lógico.
**Feedback post-sesión:** accuracy + patrón de señuelos.
**Feedback histórico:** tendencia; "—" en calibración.
**Riesgo científico:** frontera con "argumentación retórica" (mantener argumentos formales neutros; dedup con Argument X-Ray de Lenguaje documentado: Lenguaje analiza TEXTOS reales, Lógica repara FORMAS con contenido neutro).
**Riesgo de UX:** enunciados con hueco requieren tipografía clara del hueco.
**Riesgo técnico:** bajo (choice existente); generador valida que exactamente 1 candidata repara (test de unicidad).
**Claim permitido:** "Entrenas y mides la reparación de argumentos incompletos dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** unicidad de la reparación por construcción (test); dedup con Lenguaje documentado; señuelos tipados.

---

### Suposición Oculta (entimema)

**Bloque:** Lógica · **Estado:** Post-MVP (LOG-15; diseño nuevo sobre HIDDEN_ASSUMPTION/ARGUMENT_ANATOMY de la taxonomía existente) · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Identificación de la premisa tácita que un argumento necesita para ser válido.
**Subhabilidades:** HIDDEN_ASSUMPTION, ARGUMENT_ANATOMY, NECESSARY_SUFFICIENT.
**Hipótesis cognitiva:** Todo entimema (argumento con premisa implícita) se completa con una suposición específica; distinguir la suposición NECESARIA de afirmaciones compatibles-pero-no-necesarias mide análisis de estructura argumental en su forma lógica.
**Mecánica UX:** Argumento breve de 2 pasos con premisa tácita; 4 opciones (1 necesaria; señuelos: compatible-no-necesaria, inversa, irrelevante); choice + confianza; opcionalmente marcar qué premisa explícita conecta con la tácita.
**Duración recomendada:** 5 min (8–10 ítems). **Dificultad mínima:** 3. **Dificultad máxima:** 8.
**Variables de dificultad:** distancia de la tácita (directa vs puente encadenado); fuerza del señuelo compatible; abstracción; negaciones.
**Métricas capturadas:** accuracy, señuelo compatible elegido (OVERINFERENCE), confianza, RT descriptivo.
**Scoring específico:** ICR-compatible; compatible-no-necesaria = error conceptual (distinción necesidad/compatibilidad, espejo del NECESSARY_SUFFICIENT del motor).
**Cómo afecta al score del bloque:** Accuracy + ErrorControl.
**Criterio de progresión:** ≥80% → puentes encadenados.
**Criterio de fallo:** compatible repetido → MICRO_LESSON ("compatible no es necesaria").
**Anti-gaming:** posición → D6 + aleatorización; memorización → paramétrico + D5/D7; rapidez → impulsividad ICR + D3.
**Persistencia necesaria:** resumen estándar; `SessionRecord`.
**Feedback inmediato:** diagnóstico del tipo de señuelo sin revelar la necesaria.
**Feedback post-sesión:** accuracy + patrón.
**Feedback histórico:** tendencia; "—" en calibración.
**Riesgo científico:** solapamiento con Argument X-Ray (Lenguaje): se separa por contenido (formas neutras abstractas aquí, textos naturales allí) — dedup documentado y revisado antes de activar.
**Riesgo de UX:** distinción necesaria/compatible es sutil (tutorial guiado).
**Riesgo técnico:** bajo (choice existente); generador con test de necesidad (negar la candidata correcta debe invalidar el argumento — verificable con `CounterexampleValidator` extendido).
**Claim permitido:** "Entrenas y mides la identificación de suposiciones necesarias dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** test de necesidad por construcción (negación invalida); dedup con Lenguaje; señuelos tipados y balanceados.

---

## 6. Creatividad — 15 Exercises

**Propósito:** producción divergente y asociación convergente como habilidades de proceso, contra la línea base propia, 100% offline y sin IA. **Política de scoring del bloque:** MVP sin score 0–100 ni rango; métricas de proceso visibles bajo "Retroalimentación funcional — sin nota ni rango"; `normalizedScore=null` hacia el perfil hasta `CreativityProfileSource` (V1.1, ClaimLevel máx PRACTICE_SIGNAL); toda métrica sin soporte = null/"—", nunca 0.

**Selección MVP (4 de 15):**

| Ejercicio | Justificación |
|---|---|
| EX01 Usos Alternativos Estructurado | Ya jugable end-to-end; paradigma divergente público; la mejora (prohibición+banco+near-dup) es barata y convierte la instrucción no enforzada en restricción verificable |
| EX02 Rotación Forzada en Vivo | Rehecho: única vía honesta a flexibilidad — mecánica de captura distinta real (cue + timestamps por idea), exigida por auditoría y panel |
| CRE-03 Asociación Remota Convergente | Única señal con accuracy real verificable offline (tríadas propias + opciones): lo único genuinamente puntuable del bloque sin IA |
| CRE-04 Consecuencias en Cadena | Segunda mecánica divergente estructurada (profundidad 1º/2º orden verificable por encadenamiento); banco barato de curar |

**Requisitos de banco de contenido:** EX01 ≥40 objetos es-ES con top-5 usos comunes precomputados (acuerdo ≥80% entre 2 revisores) + holdout ≥8; EX02 ≥12 temas con banco de pertenencia categoría↔léxico pre-clasificado (≥30 ítems/categoría, inter-rater; frontera controlada); CRE-03 ≥60 tríadas propias es-ES con clave canónica + 3 distractores plausibles, graduadas en 3 niveles de distancia, holdout 20%; CRE-04 ≥30 escenarios con lista de consecuencias-genéricas por escenario. Todos: `bankVersion` versionado, no repetir ítem hasta agotar pool, registro en `BankValidator`/`ProfileUpdatingBanks` ANTES de alimentar perfil (V1.1). Prohibido usar ítems TTCT/RAT publicados (copyright/contaminación): todos los ítems son propios.

### Usos Alternativos Estructurado (EX01_SUPPRESS_FIRST_IDEA)

**Bloque:** Creatividad · **Estado:** Mejorar · **Prioridad:** P0 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Fluidez ideacional divergente con inhibición de la respuesta prepotente (proxy).
**Subhabilidades:** Generación de alternativas; evitación de usos dominantes; variedad categorial; elaboración.
**Hipótesis cognitiva:** Familia de usos alternativos (paradigma público tipo Guilford AUT, ítems propios): producir usos no-obvios de un objeto ejercita la búsqueda más allá de la respuesta dominante. Prohibir explícitamente los top-K usos comunes convierte la instrucción actual "salta la primera idea" (hoy NO enforzada — auditoría §9.4) en una restricción verificable localmente. Fuente: `03_8_Creatividad.md` §1 + rediseño del comité (`04_8_Creatividad_audit.md` EX01.15).
**Mecánica UX:** Objeto del banco + chips de "usos prohibidos" visibles; campo de texto + "Añadir"; rechazo inmediato motivado (prohibida/duplicada/trivial); 2–3 rondas de 90–120 s; lista con scroll (`LazyColumn`, fix del riesgo de overflow señalado en auditoría).
**Duración recomendada:** 5 min (2–3 rondas). **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** `prohibitedCount` 1→6; `minIdeas` 3→8; umbral de solapamiento trivial 0.8→0.5; abstracción del objeto 1→4; nº de rondas 2→3.
**Métricas capturadas:** validFluency; `inhibitionSuccessRate` (ideas no-prohibidas-y-no-triviales / intentos); elaboración capada; categorySpread; rechazos por tipo; `firstIdeaLatencyMs` cuando exista loop por-idea.
**Scoring específico:** `sessionSignal = 0.6·min(validFluency/meta,1) + 0.4·inhibitionSuccessRate` ∈[0,1] interno; ronda con <minIdeas válidas → inválida (sin señal, nunca 0); `normalizedScore=null` hacia perfil en MVP.
**Cómo afecta al score del bloque:** MVP: solo informe de proceso. V1.1: alimenta Precisión/Validez + Consistencia + Dificultad del composite (ancla divergente del bloque).
**Criterio de progresión:** Banda 60–80% de meta 2 sesiones seguidas → +1 prohibido; histéresis a la baja con racha <40%.
**Criterio de fallo:** Ronda <minIdeas válidas o validityRate<0.60 → INVALIDATED (comunicado sin culpa, no cuenta para calibración).
**Anti-gaming:** Señal: spam de ideas cortas/sinónimos, copy-paste de listas, memorización de los 8 objetos actuales (auditoría EX01.6). Gates: `MIN_IDEA_CHARS` existente + **CRE-D1** near-duplicate (Jaccard token-set ≥0.8 → no cuenta) + **CRE-D2** burst (<350 ms/idea) + **CRE-D3** holdout de objetos (familiaridad vs mejora); red-team obligatoria: perfil sintético "spam-basura" NO infla validFluency.
**Persistencia necesaria:** `PersistedCreativitySessionDto` existente extendido (prohibited, rechazos por tipo, `bankVersion`, difficulty) con `schemaVersion`; `SecureCreativitySessionStore` (namespace `mindboost_creativity`).
**Feedback inmediato:** Contador de válidas + motivo de rechazo por idea.
**Feedback post-sesión:** validFluency + inhibitionSuccessRate vs media de las últimas 5 sesiones, bajo "Retroalimentación funcional — sin nota ni rango" + disclaimer existente.
**Feedback histórico:** Tendencia de validFluency y nivel de prohibición alcanzado (línea sobria).
**Riesgo científico:** Mide fluidez léxica+tecleo, no "creatividad"; la sinonimia semántica real no es detectable sin IA (falsos negativos del near-dup); dependencia idiomática del banco.
**Riesgo de UX:** Frustración por rechazos; IME cubre ~40% de pantalla (`imePadding`, fontScale); títulos hoy en inglés (corregir a es-ES).
**Riesgo técnico:** Curación editorial del banco de usos comunes; compatibilidad de DTOs versionados.
**Claim permitido:** "Entrenas la generación de alternativas evitando los usos más comunes, medido dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Banco ≥40 objetos es-ES con top-5 prohibidos (inter-rater ≥80%) + rotación sin repetición + holdout ≥8; CRE-D1 verde contra adversario sintético; copy es-ES; golden de fluency/elaboration congelado antes de tocar el scorer.

### Rotación Forzada de Categorías en Vivo (EX02_CATEGORY_ROTATION)

**Bloque:** Creatividad · **Estado:** Rehacer · **Prioridad:** P0 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Flexibilidad cognitiva / switching semántico (proxy ideacional).
**Subhabilidades:** Desenganche del cluster semántico activo; fluidez sostenida; control de perseveración.
**Hipótesis cognitiva:** Fluidez semántica con switching (métrica clustering/switching de Troyer, paradigma público): alternar entre categorías bajo un cue exige abandonar el marco activo. La tasa de cambios logrados y la latencia post-cambio son señales de flexibilidad de PROCESO, declaradas proxy ideacional — nunca switch-cost ejecutivo (mandato del comité, `04_8` EX02.7/11). Motivo del rehacer: el código actual es un formulario batch idéntico a EX01 (misma señal, veredicto BLOCK del comité; el panel gameplay fija el cambio de categoría FORZADO en vivo con señal de UI + timestamp por idea).
**Mecánica UX:** Tema + `CategoryCueBar` con la categoría ACTIVA (icono+posición, no solo color); cada idea se envía individualmente con timestamp (loop por-idea, adiós batch); el cue cambia forzado cada N ideas válidas o T segundos con flash funcional; idea de la categoría anterior tras el cambio = perseveración (marcada, no puntúa); ítem fuera del banco léxico = `unclassified` (no penaliza).
**Duración recomendada:** 5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** `switchFrequency` 0.2→0.6; `categoryCount` 2→5; `minPerCategory` 1→3; similitud entre categorías 1→3; persistencia del cue visible→breve.
**Métricas capturadas:** `switchRate` (cambios logrados/solicitados); `switchLatencyMs` mediana (primera idea válida post-cue; null si falta un lado — regla ambos-lados); validFluency; perseverationErrors; categorySpread; unclassifiedRate.
**Scoring específico:** `sessionSignal = 0.5·switchRate + 0.3·min(validFluency/meta,1) + 0.2·(1−perseverationRate)`; switchLatency SOLO descriptiva; null si <8 cambios válidos (CALIBRATING).
**Cómo afecta al score del bloque:** MVP: informe. V1.1: componente Dificultad+Consistencia; jamás como "control ejecutivo".
**Criterio de progresión:** switchRate ≥0.75 dos sesiones → +switchFrequency o +categoría (histéresis).
**Criterio de fallo:** <8 switches válidos → sin señal; validityRate<0.60 → INVALIDATED.
**Anti-gaming:** Señal: teclear cualquier cosa (hoy la pertenencia NO se verifica — auditoría EX02.6), spam rápido. Gates: clasificación por banco léxico pre-clasificado (pertenencia verificable; `unclassified` no suma ni resta), **CRE-D1**, **CRE-D2** (los timestamps por idea ya existen en este loop); red-team "teclea-rápido-basura": switchRate NO debe subir.
**Persistencia necesaria:** `RotationTrialDto` nuevo (cueSequence, ideas con timestamp+clasificación, `bankVersion`, difficulty) vía `SecureCreativitySessionStore`; resumen por sesión, jamás escritura por keystroke.
**Feedback inmediato:** Válida-en-categoría / fuera / unclassified; señal de cambio de cue accesible.
**Feedback post-sesión:** Cambios logrados, latencia mediana de cambio, perseveraciones vs baseline propio.
**Feedback histórico:** Tendencia de switchRate y categorySpread.
**Riesgo científico:** Disponibilidad léxica disfrazada de flexibilidad; latencias ideacionales (segundos) → solo proxy, etiquetado obligatorio.
**Riesgo de UX:** El cambio de cue puede pasar desapercibido (mitigar con icono+posición+flash); más instrucciones que EX01.
**Riesgo técnico:** Banco categorial es-ES con frontera controlada; `CreativityMetricsAssembler` como única autoridad de switchRate/latencia (no la UI).
**Claim permitido:** "Entrenas el cambio de categoría al generar ideas, medido dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Clasificador con inter-rater y regla unclassified-no-penaliza testeada; loop por-idea con timestamps + assembler autoritativo (golden); demostrar varianza distinta de EX01 (si no, MERGE en EX01 — comité EX02.14d); red-team verde.

### Asociación Remota Convergente (CRE-03)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Asociación remota convergente.
**Subhabilidades:** Recuperación de asociaciones débiles; integración de restricciones múltiples; decisión entre alternativas.
**Hipótesis cognitiva:** Paradigma de asociados remotos (familia RAT de Mednick, paradigma público; tríadas PROPIAS en español, jamás ítems publicados): hallar el concepto que conecta tres pistas exige recuperar asociaciones no dominantes. Con respuesta por selección (1 de 4) la señal es accuracy REAL verificable offline — la reconversión honesta que el comité exigió para lo asociativo (`04_8` EX04: solo el modo verificable puntúa; panel gameplay: "3 pistas → 1 conexión, respuesta por opciones para ser puntuable").
**Mecánica UX:** 3 pistas arriba + 4 opciones (1 canónica + 3 distractores plausibles); tap para elegir (reusa `ChoiceResponseLayer` extraído, A11); 12–16 tríadas por sesión; temporizador suave de 30 s/ítem con omisión válida; sin presión dura.
**Duración recomendada:** 5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Distancia asociativa del banco (3 niveles → mapeo 1–10); nº de opciones 4→6; tiempo por ítem 30→12 s; proporción de distractores cercanos.
**Métricas capturadas:** `convergentAccuracy` (acumulada n≥20 con IC Wilson, A2); RT por ítem (descriptivo; NUNCA al perfil sin timebase autoritativo); omisiones; posición de la opción elegida (alimenta D6).
**Scoring específico:** Accuracy corregida por azar: `(acc − 1/k)/(1 − 1/k)` clamp [0,1] sobre ítems respondidos; sesión válida si ≥10 respondidos; señal null si <12 acumulados (CALIBRATING, nunca 0).
**Cómo afecta al score del bloque:** MVP: informe. V1.1: la señal MÁS fiable del bloque (accuracy con IC) — ancla del componente Precisión del composite.
**Criterio de progresión:** Accuracy corregida ≥0.6 en el nivel actual 2 sesiones → sube distancia asociativa.
**Criterio de fallo:** <10 respondidos, o veredicto anti-gaming → sin señal.
**Anti-gaming:** Señal: fast-guessing (responder al azar rápido para acabar) y sesgo de posición (siempre la misma opción). Gates: `core/antigaming` **D6** (entropía de posición de respuesta) + banda de azar con guard de muestra (patrón A8: accuracy≈1/k con RT mediana baja → WITHHOLD) + corrección por adivinación en el propio score; red-team: perfil "azar rápido" NO produce señal.
**Persistencia necesaria:** `TriadTrialDto` (itemId, `bankVersion`, opción, correcta, rtMs, posición) resumido por sesión en `SecureCreativitySessionStore`.
**Feedback inmediato:** Correcto/incorrecto sobrio por ítem (sin rachas gamificadas).
**Feedback post-sesión:** Aciertos por nivel de distancia vs baseline; IC de la estimación cuando n≥20.
**Feedback histórico:** Accuracy corregida por nivel de distancia.
**Riesgo científico:** Tríadas propias sin validación normativa → dificultad estimada mal calibrada (mitigar: registrar p-value/discriminación empírica por ítem desde el día 1, patrón Lógica A9).
**Riesgo de UX:** Ítems demasiado difíciles frustran al arrancar (empezar siempre en nivel 1).
**Riesgo técnico:** Curación editorial de ≥60 tríadas es-ES con distractores plausibles; registro en `BankValidator`/`ProfileUpdatingBanks` antes de alimentar perfil (V1.1).
**Claim permitido:** "Entrenas la búsqueda de la conexión entre ideas dentro de MindBoost, medido contra tu propia línea base".
**Criterios de aceptación:** Banco ≥60 tríadas + holdout 20% + `bankVersion`; D6+banda de azar con par adversario/honesto verde; `ChoiceResponseLayer` reutilizado sin fork; informe con Wilson.

### Consecuencias en Cadena (CRE-04)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P1 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** Fluidez de implicaciones (pensamiento hipotético divergente).
**Subhabilidades:** Exploración de consecuencias directas; encadenamiento de 2º orden; variedad de dominios.
**Hipótesis cognitiva:** Tarea de consecuencias (familia Guilford Consequences, paradigma público; escenarios propios): generar implicaciones de un supuesto hipotético ("¿qué pasaría si X?") ejercita la exploración sistemática del espacio de efectos. Separar 1º orden de 2º orden (consecuencias de consecuencias, con referencia explícita a una de 1º) da una señal de PROFUNDIDAD estructural verificable localmente. Fuente: panel science (lista MVP: "consecuencias").
**Mecánica UX:** Escenario hipotético; fase 1 (90 s): listar consecuencias directas; fase 2 (90 s): seleccionar una y encadenar segundas consecuencias con plantilla "si eso → entonces…" que fija el enlace; onboarding de 1 pantalla la primera vez.
**Duración recomendada:** 5–10 min (2 escenarios). **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Abstracción del escenario 1→4; exigencia de 2º orden 0→3 encadenadas; minIdeas por fase; variedad de dominios exigida.
**Métricas capturadas:** validFluency de 1º orden; `chainDepth` (2ᵒˢ órdenes válidas encadenadas); elaboración capada; variedad de dominios (lista local de dominios comunes).
**Scoring específico:** `sessionSignal = 0.5·min(fluency1º/meta,1) + 0.5·min(chainDepth/meta,1)`; null si <3 consecuencias válidas; nunca 0 fabricado.
**Cómo afecta al score del bloque:** MVP: informe. V1.1: componentes Precisión/Validez + Mejora (profundidad contra baseline).
**Criterio de progresión:** Meta de profundidad alcanzada 2 sesiones → +exigencia de 2º orden/abstracción.
**Criterio de fallo:** <3 válidas o validityRate<0.60 → INVALIDATED.
**Anti-gaming:** Señal: relleno genérico ("todo cambiaría", "sería distinto") y duplicados entre fases. Gates: **CRE-D1** near-duplicate + lista local de respuestas-genéricas por escenario (no cuentan; espejo del banco de prohibidos de EX01) + **CRE-D4** anti-plantilla inter-sesión + techo de palabras; red-team: perfil "genéricas" NO infla fluency ni chainDepth.
**Persistencia necesaria:** `ConsequencesTrialDto` (escenario, ideas fase1/fase2 con enlaces, `bankVersion`, difficulty) en `SecureCreativitySessionStore`.
**Feedback inmediato:** Contador por fase + motivo de rechazo (genérica/duplicada/sin enlace).
**Feedback post-sesión:** Fluidez de 1º orden y profundidad de cadena vs baseline propio.
**Feedback histórico:** Tendencia de chainDepth.
**Riesgo científico:** Verbosidad ≠ profundidad; el encadenamiento por referencia es aproximado sin semántica.
**Riesgo de UX:** Dos fases suben la carga de instrucciones (onboarding obligatorio); IME.
**Riesgo técnico:** Banco ≥30 escenarios + listas de genéricas curadas (editorial).
**Claim permitido:** "Entrenas la exploración de consecuencias de una idea dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Banco ≥30 escenarios con genéricas; test del marcador de encadenamiento (2º orden sin enlace → no cuenta); techo anti-verbosidad testeado.

### Rareza Relativa al Banco (CRE-05)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (V1.1 — primer post-MVP a construir)

**Habilidad cognitiva principal:** Exploración de la cola de la distribución de ideas (originalidad APROXIMADA, operacionalizada honestamente).
**Subhabilidades:** Producción divergente; supresión de respuestas dominantes; plausibilidad.
**Hipótesis cognitiva:** Puntuación por infrecuencia estadística (tradición Wallach-Kogan de uniqueness scoring): la rareza de una respuesta VÁLIDA relativa a un banco de frecuencias auditado es el único proxy de originalidad defendible sin IA. Fuente: `03_8` §6 + comité (`04_8` EX06: "el más defendible del set; construir el banco es el trabajo crítico").
**Mecánica UX:** Prompt; respuestas cortas; medidor común/poco común/raro por idea (icono+intensidad); el banco NUNCA se revela.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Percentil de rareza objetivo 50→90; mínimo de raras exigidas 1→6; rigor de plausibilidad 1→4.
**Métricas capturadas:** `rarityScore` = media(1−freq_rel) de válidas; `plausibilityRate`; validRareCount; coverageOfTail; fluency.
**Scoring específico:** `0.5·rarityScore·plausibilityRate + 0.3·norm(validRareCount) + 0.2·coverageOfTail`; CLAVE: rareza multiplicada por plausibilidad (el absurdo no gana); CALIBRATING (null) si cobertura del banco < umbral declarado.
**Cómo afecta al score del bloque:** V1.1: única entrada de "originalidad aproximada" al composite, etiquetada UncertaintyLabel HIGH; jamás en MVP.
**Criterio de progresión:** Proporción en percentil objetivo 0.5–0.7 sostenida → +rarityTarget.
**Criterio de fallo:** Banco insuficiente → CALIBRATING; plausibilityRate<0.5 → sesión sin señal.
**Anti-gaming:** Señal: respuestas raras-pero-absurdas; memorizar la cola. Gates: plausibilidad = pertenencia a banco-de-válidas PRIORITARIA sobre heurística (heurística → rareza provisional NO puntuable); banco oculto + feedback solo ordinal; rotación dura de prompts; red-team "rara-basura" NO puntúa.
**Persistencia necesaria:** `RarityTrialDto` + banco por-usuario cifrado para la variante contra-histórico-propio.
**Feedback inmediato:** Medidor ordinal de rareza por idea válida.
**Feedback post-sesión:** Ideas raras plausibles vs baseline; cobertura del banco declarada.
**Feedback histórico:** Tendencia de rarityScore·plausibility.
**Riesgo científico:** El banco es punto único de fallo (sesgo del curador); rareza léxica ≠ originalidad semántica (declarado en copy).
**Riesgo de UX:** El usuario puede leer "raro" como nota de capacidad (copy explícito: "rareza respecto a respuestas comunes, no medida de originalidad").
**Riesgo técnico:** Construcción/auditoría del banco de frecuencias por prompt (el mayor trabajo editorial del bloque).
**Claim permitido:** "Entrenas la búsqueda de respuestas poco comunes dentro de MindBoost, medido como rareza frente a respuestas típicas contra tu propia línea base".
**Criterios de aceptación:** Banco con tamaño/cobertura declarados y auditados; rarityScore·plausibilityRate verificado contra perfil absurdo; banco oculto + rotación; lexema "originalidad" ausente del copy (scanner).

### Elaboración Dialéctica por Capas (CRE-06)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Elaboración / profundización sostenida de una idea.
**Subhabilidades:** Pensamiento dialéctico (mejora↔riesgo); coherencia de cadena; persistencia atencional.
**Hipótesis cognitiva:** Profundizar una idea-semilla en capas alternas de mejora y riesgo (modo adverso priorizado por el comité, `04_8` EX05.15): un "riesgo" válido debe introducir un marcador negativo/condicional del banco → verificable localmente; ataca la superficialidad sin premiar verbosidad. Fuente: `03_8` §5.
**Mecánica UX:** Semilla fija arriba; capas previas apiladas (`LazyColumn`); prompt de capa alterno (mejora/riesgo); campo texto; barra de profundidad.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** targetDepth 3→10; techo de palabras/capa; umbral de redundancia 0.5→0.25; exigencia de referencia a capa previa.
**Métricas capturadas:** `depthReached`; elaboración CAPADA; layerTypeSpread; redundancyRate; coherenceProxy en banda (ni 0 ni 1).
**Scoring específico:** `0.4·norm(depthReached) + 0.3·norm(elabCapped) + 0.2·layerSpread − 0.1·redundancyNorm`; null si <minN capas válidas.
**Cómo afecta al score del bloque:** V1.1: componente Resistencia+Mejora (profundidad sostenida).
**Criterio de progresión:** targetDepth alcanzado 2 sesiones → +profundidad.
**Criterio de fallo:** Cadena rota (reinicio encubierto) o todas redundantes → INVALIDATED.
**Anti-gaming:** Señal: logorrea (capas largas vacías), sinónimos para esquivar redundancia, reinicio encubierto. Gates: techo de palabras-que-cuentan (anti-verbosidad, exigencia del comité); coherenceProxy en BANDA como eje; anti-desconexión (overlap 0 con toda la cadena → flag); **CRE-D1**; red-team "capas largas vacías" NO sube score.
**Persistencia necesaria:** `ElaborationTrialDto`+`LayerDto` con overlaps y `bankVersion`.
**Feedback inmediato:** Capa aceptada/redundante/desconectada con motivo.
**Feedback post-sesión:** Profundidad y equilibrio mejora/riesgo vs baseline.
**Feedback histórico:** Tendencia de depthReached.
**Riesgo científico:** Palabras/capa sigue siendo cantidad; coherencia léxica burda (capa profunda con vocabulario nuevo puede marcarse desconectada).
**Riesgo de UX:** Escritura larga en móvil (IME); fatiga de tarea.
**Riesgo técnico:** Utilidad de overlap local (Jaccard) nueva; banco de marcadores negativos/condicionales.
**Claim permitido:** "Entrenas el desarrollo de una idea en profundidad dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Cap de palabras testeado contra perfil verboso; banda de coherencia testeada (0 y 1 penalizan); marcador negativo verificado en modo riesgo; holdout de semillas.

### Síntesis Bajo Restricciones (CRE-07)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Combinatoria forzada / satisfacción de restricciones múltiples.
**Subhabilidades:** Integración de requisitos; variedad de soluciones; metacognición (calibración).
**Hipótesis cognitiva:** La generación bajo restricciones explícitas (literatura de constraints en cognición creativa, p. ej. Stokes; invención de Finke) fuerza salir de la solución prototípica. La señal metacognitiva `calibrationError` (|restricciones que cree cumplir − que cumple|) es la más honesta sin IA porque no depende de comprensión semántica. Fuente: `03_8` §3 + comité (`04_8` EX03.15).
**Mecánica UX:** Objetivo + chips de restricción (pendiente/cumplida); solución textual; auto-tag de qué restricciones cree cubrir; verificación local = tag + ventana de elaboración coherente alrededor (no el keyword suelto).
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** constraintCount 2→6; conflicto entre restricciones 0→3 (pre-modelado, resoluble); tags prohibidos 0→4; soluciones pedidas 1→4.
**Métricas capturadas:** constraintSatisfactionRate; viableSolutionCount; solutionVariety; `calibrationError`; constraintIntegrationDepth.
**Scoring específico:** `0.4·satisfRate + 0.3·norm(integrationDepth) + 0.2·variety − 0.1·calibErrorNorm`; calidad semántica SIEMPRE CALIBRATING sin IA; null si <minN soluciones válidas.
**Cómo afecta al score del bloque:** V1.1: componente Dificultad; calibrationError alimenta la subseñal metacognitiva.
**Criterio de progresión:** satisfRate ~0.75 sostenida → +constraintCount/conflicto.
**Criterio de fallo:** Copia del enunciado (overlap alto) o vacío → rechazo; <minN → sin señal.
**Anti-gaming:** Señal: keyword-stuffing (escribir el tag literal y "cumplir" — el ataque que el propio diseño admite). Gate: cumplimiento = tag + ventana de elaboración coherente, testeado con red-team "tag suelto NO cuenta"; anti-copy del enunciado; **CRE-D1**.
**Persistencia necesaria:** `SynthesisTrialDto` (tags requeridos/prohibidos, soluciones, auto-tags, `bankVersion`).
**Feedback inmediato:** Chips que se encienden al detectar cumplimiento real.
**Feedback post-sesión:** Satisfacción media + error de calibración vs baseline.
**Feedback histórico:** Tendencia de integrationDepth y calibración.
**Riesgo científico:** La presencia-de-tag+ventana no verifica el espíritu de la restricción; techo de honestidad bajo sin IA (declarado).
**Riesgo de UX:** Instrucciones densas; auto-tag añade fricción.
**Riesgo técnico:** Banco de restricciones con tensión pre-modelada (resolubilidad garantizada).
**Claim permitido:** "Entrenas la combinación de requisitos en una misma solución dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Red-team keyword-stuffing verde; banco con tensión validada + holdout; UI muestra calidad semántica como CALIBRATING; calibrationError con test unitario.

### Reencuadre por Escalas (CRE-08)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Problem-finding / reencuadre del problema.
**Subhabilidades:** Cambio de dimensión de marco; ortogonalidad de reformulaciones; metacognición.
**Hipótesis cognitiva:** La construcción/redefinición del problema (tradición de problem-finding: Getzels-Csikszentmihalyi; problem construction de Mumford) precede a la ideación de calidad. El modo de entrada verificable es la variante de ESCALA (reformular a nivel individuo/grupo/sistema — marcadores detectables), exigida por el comité (`04_8` EX07.15). Fuente: `03_8` §7.
**Mecánica UX:** Problema con marco por defecto resaltado; onboarding con ejemplo OBLIGATORIO; reformulaciones apiladas; selector de dimensión (chips) como declaración; detección real por banco de marcadores.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** framesRequired 2→7; dimensiones exigidas 1→5; dimensión prohibida 0→2; ortogonalidad exigida.
**Métricas capturadas:** frameDiversity (PROXY por marcadores, etiquetado); validReframeCount; orthogonalityRate; `calibrationError`; reframeLatency.
**Scoring específico:** `0.4·frameDiversity + 0.3·norm(validReframeCount) + 0.2·orthogonality − 0.1·calibErrorNorm`; primeras sesiones descartadas del baseline (warm-up); null si <minN.
**Cómo afecta al score del bloque:** V1.1: componente Mejora + subseñal metacognitiva; nunca "capacidad de resolver problemas".
**Criterio de progresión:** Tasa de reformulaciones válidas 0.7–0.85 → +dimensiones exigidas.
**Criterio de fallo:** Paráfrasis del enunciado (overlap alto) → rechazo; <minN → sin señal.
**Anti-gaming:** Señal: paráfrasis con vocabulario de otra dimensión (frameDiversity falso); declarar dimensiones no cambiadas. Gates: anti-paráfrasis (overlap alto con enunciado → rechazo); detección por banco de marcadores (la autoevaluación SOLO alimenta calibración); red-team "vocabulario-de-N-categorías-sin-reencuadrar".
**Persistencia necesaria:** `ReframeTrialDto` (dimensión declarada vs detectada, overlaps, `bankVersion`).
**Feedback inmediato:** Reformulación válida/mismo-marco/paráfrasis con motivo.
**Feedback post-sesión:** Dimensiones cubiertas + calibración vs baseline.
**Feedback histórico:** Tendencia de frameDiversity.
**Riesgo científico:** Detección léxica de dimensión ≈ aproximada; early sessions miden comprensión de instrucciones (mitigado por warm-up descartado).
**Riesgo de UX:** Tarea conceptualmente difícil (onboarding obligatorio); abstracta para móvil.
**Riesgo técnico:** Banco de problemas + marcadores por dimensión con inter-rater.
**Claim permitido:** "Entrenas el replanteamiento de un problema desde distintos ángulos dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Onboarding obligatorio implementado; variante escala como modo de entrada; frameDiversity etiquetado proxy; warm-up descartado con test; anti-paráfrasis verde.

### Mejora de Producto con Restricción Rotativa (CRE-09)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Ideación aplicada bajo restricción cambiante (mejora iterativa).
**Subhabilidades:** Fluidez aplicada; re-orientación ante nueva restricción; variedad funcional.
**Hipótesis cognitiva:** Paradigma de mejora de producto (familia Guilford/Torrance "product improvement", ítems propios) con giro de restricción en vivo: cada 2 mejoras válidas la restricción cambia ("coste cero", "sin electricidad", "para una mano") forzando re-orientación — mezcla verificable de fluidez aplicada y flexibilidad. Fuente: panel science ("mejora de producto") + prompt del bloque ("mejora iterativa con restricciones").
**Mecánica UX:** Objeto/servicio cotidiano; campo de texto; banner de restricción ACTIVA que rota con señal funcional; mejoras que violan la restricción activa se marcan (no puntúan).
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Frecuencia de rotación; dureza de restricciones (verificables por marcadores); mejoras mínimas por restricción.
**Métricas capturadas:** validFluency por restricción; adaptationRate (mejoras válidas post-rotación / rotaciones); variedad funcional; violaciones.
**Scoring específico:** `0.5·norm(validFluency) + 0.5·adaptationRate`; null si <minN mejoras válidas.
**Cómo afecta al score del bloque:** V1.1: Dificultad + Consistencia.
**Criterio de progresión:** adaptationRate ≥0.7 dos sesiones → +frecuencia de rotación.
**Criterio de fallo:** validityRate<0.60 → INVALIDATED.
**Anti-gaming:** Señal: mejoras genéricas reutilizadas entre objetos ("hacerlo más barato/bonito"). Gates: lista local de mejoras-genéricas por categoría (no cuentan) + **CRE-D4** anti-plantilla inter-sesión + **CRE-D1**; red-team "genéricas recicladas".
**Persistencia necesaria:** `ImprovementTrialDto` (objeto, secuencia de restricciones, mejoras con clasificación, `bankVersion`).
**Feedback inmediato:** Válida/violación/genérica con motivo; señal de rotación.
**Feedback post-sesión:** Adaptación a la rotación vs baseline.
**Feedback histórico:** Tendencia de adaptationRate.
**Riesgo científico:** "Mejora válida" por marcadores es superficial; contaminación léxica.
**Riesgo de UX:** Rotación puede sentirse arbitraria (explicar la regla).
**Riesgo técnico:** Banco de objetos + restricciones con marcadores verificables.
**Claim permitido:** "Entrenas la generación de mejoras bajo requisitos cambiantes dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Marcadores de restricción testeados; lista de genéricas con red-team; rotación con señal accesible.

### Combinación Conceptual Forzada (CRE-10)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Combinación conceptual (integración de dominios).
**Subhabilidades:** Mapeo de propiedades; integración; elaboración.
**Hipótesis cognitiva:** La combinación conceptual (paradigma de conceptual combination, p. ej. Mobley-Doares-Mumford; invención creativa de Finke) exige integrar propiedades de dos conceptos distantes en una propuesta única. Verificación local honesta: la propuesta debe referenciar ≥1 propiedad de CADA concepto de su lista precomputada + ventana de elaboración (lección anti-keyword de CRE-07). Fuente: panel science ("combinación forzada de conceptos") + `03_8` §1 variante 3.
**Mecánica UX:** Dos tarjetas-concepto con propiedades ocultas; campo texto para la propuesta combinada; check por concepto al detectar propiedad+elaboración.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** Distancia entre dominios (banco, 3 niveles); nº de propiedades exigidas por concepto 1→2; propuestas por par 1→3.
**Métricas capturadas:** combinationRate (propuestas que integran ambos); propiedades cubiertas; elaboración capada; variedad entre propuestas.
**Scoring específico:** `0.6·combinationRate + 0.4·norm(propuestasVálidas/meta)`; null si <minN.
**Cómo afecta al score del bloque:** V1.1: Precisión/Validez + Dificultad (distancia).
**Criterio de progresión:** combinationRate ≥0.7 → +distancia de dominios.
**Criterio de fallo:** Propuestas que solo mencionan un concepto → no cuentan; <minN → sin señal.
**Anti-gaming:** Señal: citar propiedades sin combinarlas (lista de palabras). Gates: propiedad de AMBOS conceptos + ventana de elaboración coherente + **CRE-D1**; red-team "lista de propiedades sin propuesta" NO cuenta.
**Persistencia necesaria:** `CombinationTrialDto` (par, propiedades detectadas, `bankVersion`).
**Feedback inmediato:** Check por concepto integrado.
**Feedback post-sesión:** Tasa de combinación y distancia alcanzada vs baseline.
**Feedback histórico:** Tendencia por distancia.
**Riesgo científico:** Integración real no verificable semánticamente (proxy estructural declarado).
**Riesgo de UX:** Tarea exigente; necesita ejemplo.
**Riesgo técnico:** Banco de pares con listas de propiedades (editorial).
**Claim permitido:** "Entrenas la combinación de dos conceptos en una sola propuesta dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Verificador ambos-conceptos+ventana con red-team; banco por distancia con holdout.

### Fluidez con Restricción Formal (CRE-11)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No

**Habilidad cognitiva principal:** Producción divergente bajo restricción formal dura.
**Subhabilidades:** Fluidez; control de forma; supresión de la formulación fácil.
**Hipótesis cognitiva:** La generación restringida (controlled generation; fluidez con constraint formal — "exactamente N palabras", "empieza por X", "sin usar la palabra Y") mantiene la demanda divergente mientras hace la validez 100% verificable por regla determinista — el gate perfecto sin IA. Fuente: panel science ("fluidez con restricción (exactamente N palabras)").
**Mecánica UX:** Prompt + restricción formal visible; el validador marca cumplimiento formal en vivo (verde/rojo antes de enviar); rondas de 90 s.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Dureza de la restricción (N exacto, letra rara, palabra prohibida frecuente); combinación de 2 restricciones; minIdeas.
**Métricas capturadas:** validFluency formal (cumplen la regla + sustantivas + no duplicadas); formalErrorRate; elaboración capada.
**Scoring específico:** `0.7·min(validFluency/meta,1) + 0.3·(1−formalErrorRate)`; null si <minN.
**Cómo afecta al score del bloque:** V1.1: Precisión/Validez (la validez formal es objetiva).
**Criterio de progresión:** Meta cumplida 2 sesiones → +dureza/combinación.
**Criterio de fallo:** <minN válidas → sin señal.
**Anti-gaming:** Señal: cumplir la forma con contenido basura ("casa casa casa" de N palabras). Gates: **CRE-D1** near-duplicate + `MIN_IDEA_CHARS` + exigencia de tokens distintos dentro de la idea (regla formal adicional determinista); claim limitado a "producción válida bajo restricción formal" (no semántica); red-team "forma-sin-contenido".
**Persistencia necesaria:** `FormalFluencyTrialDto` (regla, ideas, cumplimiento, `bankVersion`).
**Feedback inmediato:** Semáforo de cumplimiento formal en vivo.
**Feedback post-sesión:** Fluidez válida bajo restricción vs baseline.
**Feedback histórico:** Tendencia por dureza de regla.
**Riesgo científico:** Mide fluidez bajo regla, no calidad de ideas (declararlo).
**Riesgo de UX:** Restricciones duras frustran (arrancar suave).
**Riesgo técnico:** Bajo — validador determinista; el más barato de los nuevos.
**Claim permitido:** "Entrenas la generación de ideas cumpliendo una regla formal dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Validador formal con property tests; anti-basura formal con red-team; feedback en vivo accesible.

### Títulos Alternativos (CRE-12)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Reetiquetado conceptual (síntesis divergente breve).
**Subhabilidades:** Abstracción del contenido; variedad de ángulo; concisión.
**Hipótesis cognitiva:** Paradigma de títulos alternativos (familia Guilford "plot titles", textos propios): generar títulos distintos para un micro-relato exige abstraer y re-sintetizar. Verificación local: título válido = >50% de sus palabras de contenido fuera del texto (no copia, regla determinista) + no duplicado.
**Mecánica UX:** Micro-relato de 3–4 líneas (banco propio); campo corto; contador de títulos válidos.
**Duración recomendada:** 3–4 min. **Dificultad mínima:** 1. **Dificultad máxima:** 8.
**Variables de dificultad:** Longitud máx del título 6→3 palabras; títulos exigidos 3→8; abstracción del texto.
**Métricas capturadas:** validFluency de títulos; copyRate (títulos-copia rechazados); variedad léxica entre títulos.
**Scoring específico:** `0.7·min(títulosVálidos/meta,1) + 0.3·(1−copyRate)`; null si <3 válidos.
**Cómo afecta al score del bloque:** V1.1: aporte menor a Validez; principalmente variedad de práctica.
**Criterio de progresión:** Meta 2 sesiones → títulos más cortos (mayor compresión).
**Criterio de fallo:** <3 válidos → sin señal.
**Anti-gaming:** Señal: títulos que copian el texto o variaciones mínimas entre sí. Gates: regla >50% fuera-del-texto (determinista) + **CRE-D1** entre títulos; red-team "copias del texto".
**Persistencia necesaria:** `TitlesTrialDto` (textId, títulos, `bankVersion`).
**Feedback inmediato:** Válido/copia/duplicado.
**Feedback post-sesión:** Títulos válidos y compresión vs baseline.
**Feedback histórico:** Tendencia de validFluency.
**Riesgo científico:** Señal débil (fluidez breve); solapa con CRE-11.
**Riesgo de UX:** Leve; lectura previa añade tiempo.
**Riesgo técnico:** Banco de micro-relatos propios (editorial menor).
**Claim permitido:** "Entrenas la síntesis de una idea en títulos distintos dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Regla fuera-del-texto testeada; banco ≥25 textos con rotación.

### Analogía Proporcional Estructural (CRE-13)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No

**Habilidad cognitiva principal:** Mapeo estructural de relaciones (A:B::C:?).
**Subhabilidades:** Abstracción de la relación; transferencia estructural; discriminación de distractores.
**Hipótesis cognitiva:** El mapeo de estructuras (structure-mapping de Gentner) es la base verificable de la analogía: completar A:B::C:? contra banco con clave canónica y distractores mide relación estructural real. Es la ÚNICA parte puntuable del antiguo EX04 según el comité (`04_8` EX04.15: "reconvertir en analogía proporcional verificable"); el modo "escribe el puente libre" queda como práctica descriptiva SIN score hasta IA.
**Mecánica UX:** A:B::C:? con 4 opciones (reusa `ChoiceResponseLayer`); 12–16 ítems/sesión.
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** Abstracción de la relación; distractores relacional-cercanos; tiempo suave por ítem.
**Métricas capturadas:** proportionalAccuracy corregida por azar; RT descriptivo; posición elegida.
**Scoring específico:** Igual patrón que CRE-03: accuracy corregida sobre respondidos; null si <12 acumulados.
**Cómo afecta al score del bloque:** V1.1: refuerza Precisión (segunda señal convergente); riesgo declarado de solapamiento con Lenguaje (relaciones semánticas) — por eso P3.
**Criterio de progresión:** Accuracy corregida ≥0.6 → +abstracción.
**Criterio de fallo:** <10 respondidos → sin señal.
**Anti-gaming:** Señal: memorización del banco; sesgo de posición; azar rápido. Gates: holdout + rotación + ventana de no-repetición (patrón A9) + **D6** entropía de posición + banda de azar (A8); red-team azar.
**Persistencia necesaria:** `AnalogyItemDto` por sesión (itemId, opción, correcta, `bankVersion`).
**Feedback inmediato:** Correcto/incorrecto sobrio.
**Feedback post-sesión:** Accuracy por nivel de abstracción.
**Feedback histórico:** Tendencia por nivel.
**Riesgo científico:** Solapa con Lenguaje/Lógica (razonamiento relacional) — puede no aportar varianza propia al bloque.
**Riesgo de UX:** Sabor "de examen" (mantener tono de práctica).
**Riesgo técnico:** Banco ≥60 proporciones con clave + distractores (editorial).
**Claim permitido:** "Entrenas el reconocimiento de relaciones entre pares de ideas dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Banco con clave canónica + holdout; D6/banda de azar verdes; decisión explícita de varianza vs Lenguaje antes de alimentar perfil.

### Anti-Bloqueo Sprint-Recovery (CRE-14)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No (BLOQUEADO hasta prerrequisitos)

**Habilidad cognitiva principal:** Reducción de bloqueo/auto-censura en producción ideacional (reposicionado: NUNCA "creatividad bajo presión" — veredicto BLOCK del comité, `04_8` EX08).
**Subhabilidades:** Fluidez sostenida; recuperación del ritmo tras pausa; tolerancia a demanda leve.
**Hipótesis cognitiva:** Alternar sprints suaves de generación y ventanas libres (variante sprint-recovery, la única defendible según el comité) mide la RECUPERACIÓN de la fluidez, no el pico bajo estrés. Requiere timestamps por idea normalizados por una baseline motora de tecleo (mini-calibrador no-creativo) para no medir destreza de teclado (confound prohibido por charter).
**Mecánica UX:** Prompt; 30 s sprint / 15 s libre alternos; medidor de ritmo (icono+posición); exit siempre visible; check-in neutral si sobrecarga (espejo OverloadProxy de Emociones); presión leve-only techo 3, nunca auto-sube.
**Duración recomendada:** 2–3 min. **Dificultad mínima:** 1. **Dificultad máxima:** 6 (techo duro de presión).
**Variables de dificultad:** Objetivo de throughput; duración de sprint; rigor de validez.
**Métricas capturadas:** validThroughput normalizado por baseline motora; rhythmStability (1−CV de IBI); blockingEvents; validityRate.
**Scoring específico:** `0.4·norm(throughputNorm) + 0.25·rhythmStability + 0.25·validityRate − 0.1·blockingNorm`; SIN baseline motora → métricas de ritmo = null; timebase no autoritativo → jugable pero JAMÁS actualiza perfil (gate UNTRUSTED_TIMEBASE).
**Cómo afecta al score del bloque:** V1.1+: solo si supera sus gates; hasta entonces práctica sin señal.
**Criterio de progresión:** Recuperación estable 2 sesiones → +throughput objetivo.
**Criterio de fallo:** Abandono = ABANDONED_SAFE, NUNCA penaliza (patrón Emociones).
**Anti-gaming:** Señal: spam de tecleo/macros a alta cadencia. Gates: **CRE-D2** burst + validityRate como multiplicador (throughput de basura no puntúa) + calibrador motor; red-team "teclado rápido vacío".
**Persistencia necesaria:** `ImprovTrialDto` con IBI + baseline motora por usuario cifrada.
**Feedback inmediato:** Medidor de ritmo en banda/bajo (funcional).
**Feedback post-sesión:** Recuperación post-pausa vs baseline; sin lenguaje de estrés.
**Feedback histórico:** Tendencia de rhythmStability.
**Riesgo científico:** IBI mezcla cognición y motricidad incluso normalizado; presión contraproducente para divergencia (por eso leve-only y reposicionado).
**Riesgo de UX:** Sensación de presión (check-in + exit visible obligatorios).
**Riesgo técnico:** Calibrador motor + timebase por idea multiplataforma — coste alto; si no se asume → RETIRE (comité).
**Claim permitido:** "Entrenas la generación de ideas sin bloquearte dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Calibrador motor + normalización testeados; gate de timebase real (no hardcodeado); nombre/copy sin "presión/estrés"; abandono no penaliza (test permanente).

### Cadena Prohibida Creciente (CRE-15)

**Bloque:** Creatividad · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No (modo avanzado de EX01)

**Habilidad cognitiva principal:** Inhibición ideacional acumulativa (presión inhibitoria creciente intra-trial).
**Subhabilidades:** Fluidez bajo restricción creciente; monitorización del propio historial de respuestas.
**Hipótesis cognitiva:** Extensión del paradigma de usos alternativos donde cada idea aceptada se añade al set prohibido del MISMO objeto (`03_8` §1 variante 2): la demanda inhibitoria crece dentro del trial, midiendo cuántas alternativas produce el usuario antes de agotarse — profundidad de exploración bajo auto-restricción.
**Mecánica UX:** Igual que EX01 pero los chips prohibidos CRECEN con cada aceptación; fin de trial al agotar ventana o 3 rechazos seguidos.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 3. **Dificultad máxima:** 10.
**Variables de dificultad:** Prohibidos iniciales; umbral de trivialidad; ventana por trial.
**Métricas capturadas:** exhaustionDepth (ideas válidas antes del agotamiento); validFluency; rechazos por tipo.
**Scoring específico:** `0.7·norm(exhaustionDepth/meta) + 0.3·inhibitionSuccessRate`; null si <minN.
**Cómo afecta al score del bloque:** V1.1: refina la señal de EX01 (Dificultad); se presenta como nivel avanzado, no ejercicio separado en el menú.
**Criterio de progresión:** Se desbloquea al alcanzar nivel ≥6 en EX01.
**Criterio de fallo:** <minN válidas → sin señal.
**Anti-gaming:** Igual que EX01 (**CRE-D1/D2/D3**); señal adicional: reformular la misma idea con sinónimos para esquivar la cadena → CRE-D1 con umbral más estricto (0.7); red-team "sinónimos en cadena".
**Persistencia necesaria:** Reusa el DTO de EX01 con flag de modo y set prohibido dinámico.
**Feedback inmediato:** Chip nuevo al aceptar (la restricción crece, visible).
**Feedback post-sesión:** Profundidad de agotamiento vs baseline.
**Feedback histórico:** Tendencia de exhaustionDepth.
**Riesgo científico:** Igual que EX01 + sesgo de estrategia (ideas cortas para alargar cadena — mitigado por sustantividad).
**Riesgo de UX:** Frustración al agotarse (encuadrar como "hasta dónde llegaste", sin fallo).
**Riesgo técnico:** Mínimo (reusa EX01).
**Claim permitido:** "Entrenas la producción de alternativas cuando las opciones fáciles se agotan dentro de MindBoost contra tu propia línea base".
**Criterios de aceptación:** Set prohibido dinámico testeado; CRE-D1 estricto verde; desbloqueo por nivel de EX01.

---

## 7. Lenguaje — 15 Exercises

**Propósito:** precisión verbal receptiva medida contra la propia línea base (comprensión anclada, semántica fina, síntesis, argumento, alcance, criterios estructurales; expansión a fluidez, vocabulario, gramática, paráfrasis, ambigüedad, registro, analogías, inferencia y definición). **Política de scoring del bloque:** accuracy ponderada por evidencia con gates a peso 0 (`ClosedScoringEngine`), `normalizedScore` 0–1 o **null** (nunca fabricado), RT jamás al perfil (sin timebase), banco TRAINING ·0.5, min-n 20/trait (A2), salida vía `LanguageProfileSource → ModuleContribution(VERBAL)`, `preliminary=true`. **Requisitos de banco (transversal):** tamaño mínimo por ejercicio (tabla abajo), posición de la correcta balanceada + shuffle en render, no repetir ítem hasta agotar pool (`LocalExposureRepository`), `bankVersion` versionado, holdout de re-test reservado (patrón BaselineProtocol), unicidad de clave validada por ≥2 autores, ≥20% temas neutros sin prior, registro en `ProfileUpdatingBanks` → `BankValidator` al alimentar perfil.

**Selección MVP (6, cuota A16):**

| Ejercicio | Justificación (1 línea) |
|---|---|
| Evidence Lock | La señal más limpia del módulo: diseño anti-adivinación por anclaje de evidencia, scoring testeado |
| Semantic Surgery | Única cobertura MVP de vocabulario/matiz semántico (LEX_P), mecánica simple y probada |
| Compression Ladder | Síntesis con crédito parcial honesto (Kendall-tau real en código) |
| Argument X-Ray | Análisis argumental verbal ya jugable; requiere rehacer banco (bug posicional verificado) |
| Scope & Negation | Fuente sistemática de errores de comprensión; encaje directo con PRD "comprensión" |
| Source Triage | Diferenciador epistémico-estructural; entra solo con claim blindado y decoy bidireccional |

---

### Evidence Lock (Anclaje evidencial) — `evidence_lock`

**Bloque:** Lenguaje · **Estado:** Mejorar (wiring persistencia + banco ≥40 posición-balanceado + prior-flag + RT-lectura; mecánica intacta) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Inferencia legítima anclada en evidencia (INF).
**Subhabilidades:** Control de sobre-inferencia (over-claiming), comprensión literal (LIT), relevancia del soporte, autorregulación evidencial.
**Hipótesis cognitiva:** Paradigma de inferencia basada en texto con justificación evidencial (familia text-based inference de la investigación en comprensión): exigir marcar el soporte exacto separa inferencia soportada de adivinación y de conocimiento previo; el 0.5 con `INF_OVER` modela el coste de sobre-leer. Near-transfer solo intra-paradigma; nada más se afirma.
**Mecánica UX:** Párrafo (40–90 palabras) → elegir 1 de 4 conclusiones → marcar chips de evidencia (multi-select) → confirmar → feedback + micro-lección; 6–10 ítems.
**Duración recomendada:** 5–7 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `sourceLengthWords` 40–90; `requiredEvidenceCount` 1–3; `inferenceDepth` 1–3; `distractorOverlap` 0–0.6; `overClaimTrapStrength`; abstracción léxica; complejidad sintáctica.
**Métricas capturadas:** correctness {0, 0.5, 1}; Jaccard de captura evidencial; tasa `INF_OVER`; latencia coarse (solo gate); flags; `evidenceWeight`.
**Scoring específico:** `ClosedScoringEngine.EvidenceSelection` (existente): correcta+evidencia completa=1.0; correcta sin evidencia completa=0.5+`INF_OVER`; incorrecta=0. `INF_score=Σ(score·weight)/Σweight`, null si Σweight<umbral.
**Cómo afecta al score del bloque:** Trait INF — señal gobernante (peso pleno en Precisión 0.45; su escalera de evidencia E0–E5 alimenta el `normalizedScore` del `LanguageProfileSource`).
**Criterio de progresión:** Staircase 1-up/2-down sobre `inferenceDepth`+`requiredEvidenceCount`, banda accuracy 0.70–0.80 (al reactivar `DefaultAdaptationEngine`).
**Criterio de fallo:** Accuracy<0.5 en ventana de 6 ⇒ DECREASE + repair; `INF_OVER`≥0.4 ⇒ bajar `overClaimTrapStrength` + micro-lección dirigida.
**Anti-gaming:** "Marca-todo" ⇒ cap 0.5 vía Jaccard exacto + `INF_OVER` (ya testeado); "siempre-b" ⇒ balance posicional del banco + LEN-D1 (entropía de posición, espejo D6 core/antigaming, red-team obligatoria); respuesta-sin-lectura ⇒ LEN-D2 (latencia < suelo paramétrico por longitud ⇒ `LOW_EVALUATOR_CONFIDENCE` ⇒ peso 0); re-exposición ⇒ `REPEATED_ITEM` vía `LocalExposureRepository` ⇒ peso 0.
**Persistencia necesaria:** `EvidencePacket.append` por ítem + `SessionSummary.save` en `finish()` + `ExposureRepository.record`; namespace `mindboost_language` (AccountBound, ya en eraser).
**Feedback inmediato:** Correcto / "conclusión correcta, soporte incompleto o excesivo" / incorrecto + tipo de error; sin revelar el set exacto al primer intento.
**Feedback post-sesión:** Accuracy ponderada, captura evidencial media, tasa de sobre-lectura, estado del trait.
**Feedback histórico:** Escalera E0–E5 de INF, slope de captura inter-sesión, "X de 5 sesiones para calibrar".
**Riesgo científico:** Conocimiento previo contamina (mitigado con `HIGH_PRIOR_KNOWLEDGE` por tema); el 0.5 puede premiar adivinar la conclusión si la evidencia es trivial.
**Riesgo de UX:** El botón Confirmar no exige evidencia (pedagógico intencional) — explicarlo en la primera instrucción para no frustrar.
**Riesgo técnico:** Bajo — todo existe; el riesgo real es de wiring (tests de wiring nuevos obligatorios).
**Claim permitido:** "Entrenas y mides inferencia anclada en evidencia dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Persistencia rojo→verde; banco ≥40 balanceado con prior-flag; adversarios marca-todo⇒0.5 y siempre-b≤chance+10; `REPEATED_ITEM` activo; copy-scanner verde.

### Semantic Surgery (Disección de significado) — `semantic_surgery`

**Bloque:** Lenguaje · **Estado:** Mejorar (banco ≥50 balanceado por `changeType`; claim restringido a equivalencia — sin localización de token en v1) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Precisión léxico-semántica (LEX_P).
**Subhabilidades:** Discriminación de cuasi-sinónimos, sensibilidad a polaridad/cuantificador/alcance/conector/aspecto, detección de tergiversación.
**Hipótesis cognitiva:** Juicio de equivalencia de paráfrasis (paradigma de similitud/equivalencia semántica): detectar cuál reformulación preserva exactamente el significado exige representar matices, no superficie léxica; los distractores diagnósticos por `changeType` producen un perfil de sensibilidad por operador.
**Mecánica UX:** Oración fuente destacada + 3–4 paráfrasis-tarjeta → tap a la equivalente → feedback con el tipo de cambio del distractor elegido; 8–12 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `changeSubtlety` 1–5; `sentenceLengthWords` 8–28; `lexicalDistance`; `decoyFluency`; `changeType` ∈ {polaridad, cuantificador, alcance, conector, aspecto}.
**Métricas capturadas:** Accuracy de equivalencia; perfil de error por `changeType` (banco etiquetado — hoy no agregable, se añade); falsos positivos por tipo; latencia coarse (gate).
**Scoring específico:** `ClosedScoringEngine.SingleChoice` binario; `typeProfile[changeType]=HIT/(HIT+MISS)`, null si n<8 por tipo; `LEX_score=Σ(score·weight)/Σweight`, null si insuficiente.
**Cómo afecta al score del bloque:** Trait LEX_P a Precisión 0.45; el perfil por tipo es feedback interno (no barra de perfil hasta n≥8/tipo acumulado).
**Criterio de progresión:** Staircase 1-up/3-down sobre `changeSubtlety` (~79% objetivo); error repetido del mismo tipo (≥2 en ventana 6) ⇒ repair de ese tipo.
**Criterio de fallo:** Accuracy<0.5 en ventana 6 ⇒ DECREASE solo en el eje fallado.
**Anti-gaming:** Heurística "la más corta/natural es la equivalente" ⇒ test estructural de no-correlación longitud/naturalidad↔clave en el banco; siempre-misma-posición ⇒ LEN-D1 + shuffle; memorización ⇒ `REPEATED_ITEM`; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Idéntica al bloque (packet/summary/exposure sobre `mindboost_language`).
**Feedback inmediato:** Correcto/incorrecto + qué operador cambió el sentido en el distractor elegido.
**Feedback post-sesión:** Accuracy ponderada + tipo de cambio más fallado (metacognitivo, no punitivo).
**Feedback histórico:** Escalera del trait LEX_P + evolución del perfil por `changeType` cuando n≥8/tipo.
**Riesgo científico:** "Matiz semántico" resbaladizo sin rúbrica cerrada por tipo validada; mono-idioma (español) debe declararse — sin claim translingüístico.
**Riesgo de UX:** Frustración en `changeSubtlety` alto si el feedback no explica el pivote; mantener explicación por tipo.
**Riesgo técnico:** Bajo; el etiquetado `changeType` agregable exige un campo nuevo en el ítem del banco (aditivo).
**Claim permitido:** "Entrenas y mides discriminación de significado entre reformulaciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥50 (≥10/tipo) con test de balanceo anti-bias; min-n por tipo con null honesto; adversario más-corta-siempre ≤ chance+10; persistencia verde.

### Compression Ladder (Escalera de compresión) — `compression_ladder`

**Bloque:** Lenguaje · **Estado:** Mejorar (forzar `candidateCount≥4` en niveles altos; banco ≥30 balanceado por `distortionType`; claim de reconocimiento) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Síntesis por reconocimiento (SUM).
**Subhabilidades:** Jerarquía idea principal/secundaria, detección de adición (inventar) vs omisión, economía semántica.
**Hipótesis cognitiva:** Discriminación de fidelidad de resumen: reconocer el resumen que conserva la tesis sin distorsión exige un modelo de la macroestructura del texto (macrorreglas de la comprensión de discurso); es reconocimiento, NO producción — el claim se limita a ello.
**Mecánica UX:** Texto (60–120 palabras) + nivel de compresión → seleccionar el resumen óptimo o reordenar candidatos del más al menos fiel (drag, `Ordering`); 5–8 ítems.
**Duración recomendada:** 5–7 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `compressionLevel` 1–4; `candidateCount` 4–5; `distortionType` ∈ {omisión, adición, distorsión, exacto}; `nuanceLoad`; `decoyFidelity`.
**Métricas capturadas:** Kendall-tau normalizado (ordering) o accuracy (choice); tasa de adición vs omisión; latencia coarse (gate).
**Scoring específico:** `ClosedScoringEngine.Ordering` (τ normalizado, set incorrecto ⇒ 0 — ya en código) / `SingleChoice`; `COMP_score=fidelityIndex·(1−w·additionRate)`, null si Σweight<umbral.
**Cómo afecta al score del bloque:** Trait SUM a Precisión 0.45; `compressionLevel` alcanzado alimenta Dificultad 0.20.
**Criterio de progresión:** Sube `compressionLevel`+`nuanceLoad` solo con fidelidad alta sostenida (1-up/2-down).
**Criterio de fallo:** τ medio < banda de azar en ventana 6 ⇒ DECREASE; dominancia de adición ⇒ repair anti-invención.
**Anti-gaming:** Orden-aleatorio rinde τ≈0.5 esperado ⇒ min-n alto + agregación inter-sesión + flag si τ≈azar con latencia mínima (LEN-D2); descarte trivial ⇒ `decoyFidelity` alto; memorización de orden ⇒ `REPEATED_ITEM` + shuffle de candidatos.
**Persistencia necesaria:** Estándar del bloque (packet/summary/exposure).
**Feedback inmediato:** Orden correcto revelado con el par invertido clave resaltado; en choice, el defecto del elegido (añade/omite/distorsiona).
**Feedback post-sesión:** Fidelidad media, tasa de adición (inventar) como señal metacognitiva.
**Feedback histórico:** Escalera SUM + nivel de compresión máximo sostenido.
**Riesgo científico:** Reconocer ≠ producir: jamás afirmar "sabes resumir"; τ grueso con pocos candidatos (por eso ≥4).
**Riesgo de UX:** Drag-reorder en pantallas pequeñas (destreza motora como confound) — ofrecer modo selección.
**Riesgo técnico:** Lista reordenable accesible (`LazyColumn` drag) ya resuelta en el repo para otros casos; bajo.
**Claim permitido:** "Entrenas y mides reconocimiento de resúmenes fieles dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** `candidateCount≥4` forzado por el banco en niveles altos (test estructural); banco ≥30 balanceado; property test orden-inverso⇒τ→0; claim de reconocimiento en todo el copy.

### Argument X-Ray (Radiografía argumental) — `argument_xray`

**Bloque:** Lenguaje · **Estado:** Rehacer (banco completo: la correcta es "b" en los 6 ítems — verificado; mecánica single-choice "eslabón débil" se CONSERVA y se re-claima; opción B del audit 04_4 por coste/riesgo) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Análisis argumental verbal (ARG).
**Subhabilidades:** Localización del salto no garantizado (non-sequitur), distinción hecho-dado vs caricatura (hombre de paja), sensibilidad a garantías implícitas.
**Hipótesis cognitiva:** Evaluación de argumentos informales: localizar el eslabón inferencial más débil usando SOLO la estructura interna del texto (no verdad-en-el-mundo) mide sensibilidad a la relación premisa→conclusión con vehículo verbal; se delimita formalmente de Lógica (forma proposicional) para no duplicar varianza en el perfil.
**Mecánica UX:** Párrafo argumentativo (50–110 palabras) → "¿dónde está el salto que el texto no justifica?" → 3–4 opciones (débil real, hecho dado, caricatura) → feedback por tipo de error; 5–8 ítems.
**Duración recomendada:** 6–8 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `sentenceCount` 3–7; `connectorExplicitness` (sin "por tanto" como pista); `premiseChainDepth` 1–3; `topicAbstractness`; conclusión implícita (niveles altos).
**Métricas capturadas:** Accuracy; `distractorErrors` ARG_GAP vs ARG_STRAW (perfil del tipo de confusión); latencia coarse (gate).
**Scoring específico:** `SingleChoice` binario (1 bit/ítem — IC ancho: agregación inter-sesión obligatoria); `ARG_score=Σ(score·weight)/Σweight`, null si insuficiente.
**Cómo afecta al score del bloque:** Trait ARG a Precisión 0.45; señal declarada "gruesa" (1-bit) — nunca gobierna sola la calibración del bloque.
**Criterio de progresión:** 1-up/2-down sobre `sentenceCount`+conclusión implícita.
**Criterio de fallo:** Confusión sistemática hacia ARG_STRAW (≥2 en ventana 6) ⇒ repair "caricatura vs eslabón" + DECREASE.
**Anti-gaming:** **Bug real hoy: "elige-siempre-b" rinde 100% en este banco** ⇒ rehacer banco con posición balanceada + shuffle de opciones en render + test estructural de distribución + adversario siempre-b en red-team (LEN-D1); memorización ⇒ `REPEATED_ITEM`; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Correcto/incorrecto + si elegiste un hecho dado o una caricatura (etiquetas funcionales: "afirmación principal/razón/no aporta" — sin jerga).
**Feedback post-sesión:** Accuracy ponderada + tipo de confusión dominante.
**Feedback histórico:** Escalera ARG + tendencia del perfil de confusión.
**Riesgo científico:** Solape con Lógica en el brain-profile (delimitar en la definición del ejercicio y validar ortogonalidad empírica); familiaridad con falacias como prior.
**Riesgo de UX:** Textos argumentativos densos en móvil — mantener ≤110 palabras y tipografía generosa.
**Riesgo técnico:** Bajo (mecánica existente); el coste es de autoría del banco rehecho.
**Claim permitido:** "Entrenas y mides localización de saltos no justificados en argumentos dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥40 rehecho con posición balanceada (test); adversario siempre-b ≤ chance+10; delimitación frente a Lógica documentada en la definición; persistencia verde.

### Scope & Negation (Alcance y negación) — `scope_negation`

**Bloque:** Lenguaje · **Estado:** Mejorar (cuota ≥20% ítems cero-válidas; banco ≥50 balanceado por operador; delimitación frente a evidence_lock/Lógica) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Procesamiento sintáctico de alcance (SYN).
**Subhabilidades:** Negación simple/doble, cuantificadores universal↔existencial, "solo/salvo/a menos que", condicional vs bicondicional, control de over/under-claim.
**Hipótesis cognitiva:** Verificación de inferencias bajo operadores (paradigma clásico de procesamiento de negación y alcance de cuantificadores en psicolingüística): decidir qué se sigue NECESARIAMENTE de un enunciado con operadores mide el cálculo de alcance, fuente sistemática de errores de comprensión; los casos sin inferencia válida rompen el sesgo "siempre hay una".
**Mecánica UX:** Enunciado destacado + 3–5 inferencias-chip → multi-select cerrada de las que se siguen (a veces ninguna) → confirmar → feedback por operador; 8–12 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `operatorType` ∈ {negación, universal, existencial, solo, a-menos-que, condicional}; `nestedScopeDepth` 1–3; `validInferenceCount` 0–3; `surfaceSimilarity`; doble negación.
**Métricas capturadas:** Jaccard de selección; over-claim (inválidas marcadas) vs under-claim (válidas omitidas); perfil por `operatorType`; latencia coarse (gate).
**Scoring específico:** `ClosedScoringEngine.EvidenceSelection` (Jaccard exacto ya en código); `SCOPE_score=Σ(score·weight)/Σweight`, null si insuficiente; `operatorProfile[op]` null si n<8.
**Cómo afecta al score del bloque:** Trait SYN a Precisión 0.45; over/under-claim alimenta el mismo eje metacognitivo interno que `INF_OVER`.
**Criterio de progresión:** 1-up/3-down sobre `nestedScopeDepth`+`operatorType` (~79%).
**Criterio de fallo:** Confusión sistemática de un operador (≥2 en ventana 6) ⇒ repair de ese operador + DECREASE en ese eje.
**Anti-gaming:** "Marca-algo-siempre" ⇒ roto por cuota ≥20% cero-válidas (test estructural del banco); "marca-todo" ⇒ Jaccard exacto castiga; posición ⇒ LEN-D1 + shuffle; sin-lectura ⇒ LEN-D2; memorización ⇒ `REPEATED_ITEM`.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Por qué cada distractor no se sigue (inversión/negación/cuantificador) tras confirmar.
**Feedback post-sesión:** Accuracy ponderada + operador más débil.
**Feedback histórico:** Escalera SYN + perfil por operador cuando n≥8/op.
**Riesgo científico:** Solape con evidence_lock (mismo Jaccard) y con Lógica NECESSARY_SUFFICIENT — exige ortogonalidad empírica o MERGE (audit 04_4); dependencia del español (mono-idioma declarado).
**Riesgo de UX:** Jaccard exacto frustra a dificultad alta — considerar crédito parcial controlado documentado.
**Riesgo de técnico:** Bajo; cuota cero-válidas es política de banco (test estructural nuevo).
**Claim permitido:** "Entrenas y mides el cálculo del alcance de negaciones y cuantificadores dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Cuota cero-válidas ≥20% (test); banco ≥50 con ≥8/operador; adversario marca-algo-siempre ≤ chance+10; delimitación E1/Lógica documentada; persistencia verde.

### Source Triage (Triaje de fuentes) — `source_triage`

**Bloque:** Lenguaje · **Estado:** Mejorar (claim blindado a "criterios dados en el ítem"; decoy de autoridad bidireccional; banco ≥40 con atributos explícitos) · **Prioridad:** P1 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** Lectura crítica estructural (CRIT).
**Subhabilidades:** Distinción dato/opinión/especulación, relevancia vs prestigio, detección de contradicción interna, identificación de información faltante.
**Hipótesis cognitiva:** Aplicación de criterios epistémicos estructurales dados (relevancia, especificidad, consistencia interna) a afirmaciones presentadas: mide la aplicación disciplinada de criterios explícitos del ítem, NO juicio de credibilidad del mundo real — el sesgo de autoridad (elegir prestigio irrelevante) es el error diagnóstico objetivo.
**Mecánica UX:** Pregunta + 3–5 afirmaciones/fuentes con atributos visibles como chips (relevancia, especificidad, contradicción) → elegir la que mejor sustenta / detectar qué falta → feedback con el criterio decisivo; 5–8 ítems.
**Duración recomendada:** 6–8 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** `sourceCount` 3–5 (≥4 en alto); `criteriaInTension` 1–3; `authorityDecoyStrength` bidireccional; presencia de contradicción; mezcla dato/opinión/inferencia.
**Métricas capturadas:** Accuracy; `authorityBiasRate` (decoy elegido / trials con decoy); discriminación dato/opinión; latencia coarse (gate).
**Scoring específico:** `SingleChoice` binario; `SRC_score=Σ(score·weight)/Σweight`, null si insuficiente; `authorityBiasRate` como sub-señal interna.
**Cómo afecta al score del bloque:** Trait CRIT a Precisión 0.45; nunca gobierna solo la calibración (señal 1-bit).
**Criterio de progresión:** 1-up/2-down sobre `sourceCount`+`criteriaInTension`.
**Criterio de fallo:** Caer en `authorityDecoy` ≥2 en ventana 6 ⇒ repair anti-autoridad + bajar decoy.
**Anti-gaming:** Regla inversa "ignora siempre la fuente seria" ⇒ decoy BIDIRECCIONAL (a veces la prestigiosa SÍ es la mejor; test estructural de balance); posición ⇒ LEN-D1 + shuffle; memorización ⇒ `REPEATED_ITEM`; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Criterio que falló ("prestigiosa pero irrelevante aquí", "contradice el dato 2").
**Feedback post-sesión:** Accuracy ponderada + tasa de sesgo de autoridad (contra tu propia línea base).
**Feedback histórico:** Escalera CRIT + evolución del sesgo de autoridad.
**Riesgo científico:** El mayor riesgo del bloque es de CLAIM, no de validez: el usuario interpretará "fuentes" como mundo real — copy blindado obligatorio (el copy-scanner ya cubre language; añadir los lexemas mediáticos sensibles a la revisión de copy externo A18).
**Riesgo de UX:** Chips de atributos densos — jerarquizar visualmente y limitar a 3 atributos visibles.
**Riesgo técnico:** Bajo (single-choice existente).
**Claim permitido:** "Entrenas y mides la aplicación de criterios estructurales dados para jerarquizar afirmaciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Copy blindado verde en scanner (3 variantes) y revisión A18 del copy externo; decoy bidireccional balanceado (test); banco ≥40 con atributos en el ítem; adversarios elige-la-seria-siempre e ignora-la-seria-siempre ≤ chance+10.

### LEN-07 · Fluidez semántica cronometrada

**Bloque:** Lenguaje · **Estado:** Nuevo (cubre "rapidez verbal/asociación" del PRD §10.5; sugerido por panel science: "fluidez verbal por letra/categoría (conteo)") · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Acceso y fluidez léxica (LEX_A).
**Subhabilidades:** Recuperación estratégica por categoría/letra, cambio de subcategoría (switching), inhibición de repeticiones e intrusiones.
**Hipótesis cognitiva:** Fluidez verbal semántica/fonológica (paradigma público de category/letter fluency; métricas de clustering/switching de Troyer como secundarias): el conteo de miembros válidos únicos generados en ventana fija indexa el acceso léxico estratégico; ítems propios (categorías/letras), sin material con copyright.
**Mecánica UX:** Categoría o letra en pantalla → 60–90 s de teclado para escribir tantos miembros válidos como pueda (lista visible, borrado permitido) → cierre automático → informe. Validación offline contra lexicón local ES por categoría.
**Duración recomendada:** 2–4 min (2 rondas de 60–90 s) · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Amplitud de la categoría (animales → instrumentos de cuerda), letra frecuente→rara, restricción añadida (sin repetir inicial), alternancia forzada de dos categorías (nivel alto).
**Métricas capturadas:** Nº válidas únicas (primaria); switching de subcategorías; perseveraciones (repetición/flexión); intrusiones (fuera de categoría/lexicón); cadencia de tecleo (solo gate).
**Scoring específico:** Conteo válido normalizado contra la banda propia (baseline personal por tipo de ronda); `normalizedScore=null` hasta ≥2 administraciones comparables (patrón span A2: banda SEM); nunca conteo bruto como score absoluto.
**Cómo afecta al score del bloque:** Trait LEX_A: entra en Precisión con peso reducido (señal de generación, más ruidosa) y en Amplitud 0.10; jamás gobierna la calibración.
**Criterio de progresión:** Sube amplitud/restricción cuando el conteo válido supera la banda propia 2 rondas seguidas.
**Criterio de fallo:** Conteo bajo la banda propia con intrusiones altas ⇒ bajar restricción; nunca mensaje punitivo.
**Anti-gaming:** Entradas fuera de lexicón no cuentan (intrusión); duplicados/flexiones normalizados por stemming ⇒ 1; ráfaga de tecleo humanamente imposible o pegado (>N chars/s) ⇒ **LEN-D3** detector de cadencia de entrada (espejo del D1 de core/antigaming; red-team con perfil "pegado de lista") ⇒ INVALIDATED; repetir la misma categoría ⇒ exposure.
**Persistencia necesaria:** Summary agregado (conteos, switching, categoría-id) — JAMÁS las palabras crudas (minimización D11); exposure de categorías.
**Feedback inmediato:** Contador de válidas en vivo (sin juicio por entrada para no interrumpir).
**Feedback post-sesión:** Válidas, cambios de subcategoría, repeticiones — contra tu propia banda.
**Feedback histórico:** Tendencia del conteo válido por tipo de ronda, banda SEM.
**Riesgo científico:** Fluidez inflable con miembros triviales (mitigado: solo válidos únicos cuentan y la banda es personal); efecto de práctica alto ⇒ `practiceEffectRisk` alto en el trait (down-weighting del ProfileProjector, patrón ya modelado).
**Riesgo de UX:** IME ocupa ~40% de pantalla (imePadding, layout validado); tecleo en móvil penaliza a usuarios lentos de teclado — declarar como confound y no comparar entre usuarios.
**Riesgo técnico:** Lexicón ES embebido por categoría (tamaño APK; empaquetar comprimido); guardado por ronda, jamás por keystroke (regla mobile).
**Claim permitido:** "Entrenas y mides fluidez de recuperación de palabras por categoría dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Validador de lexicón con tests (flexiones/duplicados); LEN-D3 con red-team (pegado, ráfaga); persistencia sin palabras crudas (test de minimización); banda personal con ≥2 administraciones antes de cualquier score.

### LEN-08 · Vocabulario en contexto (cloze)

**Bloque:** Lenguaje · **Estado:** Nuevo (cubre "vocabulario" del PRD; panel science: "cloze de completamiento / profundidad de vocabulario") · **Prioridad:** P1 (primer candidato post-MVP) · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Profundidad léxica en contexto (LEX_D).
**Subhabilidades:** Selección léxica por restricción contextual, sensibilidad a colocaciones, registro léxico, descarte de cuasi-sinónimos inadecuados.
**Hipótesis cognitiva:** Técnica cloze de opción cerrada (paradigma público de completamiento): elegir la palabra que satisface TODAS las restricciones del contexto (semántica, colocacional, de registro) mide conocimiento léxico en profundidad, no mera familiaridad; los distractores diagnósticos separan el tipo de fallo.
**Mecánica UX:** Frase/párrafo con un hueco + 4–5 opciones (correcta + cuasi-sinónimo de registro incorrecto + colocación inválida + sentido opuesto/intrusión plausible) → tap → feedback por tipo de distractor; 10–14 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Frecuencia léxica de la clave (frecuente→rara), distancia semántica de los distractores, longitud/densidad del contexto, nº de restricciones activas (semántica+colocación+registro).
**Métricas capturadas:** Accuracy; perfil de error por tipo de distractor; latencia coarse (gate).
**Scoring específico:** `SingleChoice`; `VOC_score=Σ(score·weight)/Σweight`, null si insuficiente; perfil por tipo de distractor null si n<8/tipo.
**Cómo afecta al score del bloque:** Trait LEX_D a Precisión 0.45 y Amplitud 0.10; con LEN-15 comparte trait (sub-señales distintas: contexto vs definición).
**Criterio de progresión:** 1-up/2-down sobre frecuencia de la clave + distancia de distractores.
**Criterio de fallo:** Error dominante en colocaciones (≥2 ventana 6) ⇒ repair de colocación + DECREASE.
**Anti-gaming:** Posición ⇒ LEN-D1 + shuffle; sin-lectura ⇒ LEN-D2 (suelo por longitud de contexto); memorización ⇒ `REPEATED_ITEM` + banco ≥60; "la rara es la mala" ⇒ balance de frecuencia entre clave y distractores (test estructural).
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Por qué el distractor no encaja ("colocación inválida en español", "registro demasiado formal aquí").
**Feedback post-sesión:** Accuracy ponderada + tipo de fallo léxico dominante.
**Feedback histórico:** Escalera LEX_D + banda de frecuencia léxica dominada.
**Riesgo científico:** Mide conocimiento cristalizado con fuerte efecto de nivel educativo — declarar como perfil funcional propio, nunca comparar entre usuarios; sesgo cultural de ítems (temas neutros ≥20%, revisión de fairness del banco).
**Riesgo de UX:** Bajo (mecánica de 1 tap).
**Riesgo técnico:** Bajo — `SingleChoice` existente; el coste es autoría del banco etiquetado por frecuencia.
**Claim permitido:** "Entrenas y mides precisión de vocabulario en contexto dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥60 etiquetado (frecuencia, tipo de distractor) con validación de unicidad por 2 autores; test de balance frecuencia↔clave; min-n por tipo; copy-scanner verde.

### LEN-09 · Precisión gramatical (juicio de forma)

**Bloque:** Lenguaje · **Estado:** Nuevo (paradigma de juicio de gramaticalidad, psicolingüística estándar) · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Procesamiento sintáctico normativo (SYN).
**Subhabilidades:** Concordancia (género/número/persona), régimen preposicional, correlación temporal, anacoluto, detección del punto de ruptura.
**Hipótesis cognitiva:** Juicio de gramaticalidad (GJT): decidir si un enunciado está bien formado activa el monitoreo sintáctico; con balance 50/50 gramatical/agramatical la señal correcta es d′ (sensibilidad) separada del criterio c (sesgo de respuesta) — el sesgo "siempre-no" queda expuesto por el propio modelo, no premiado.
**Mecánica UX:** Enunciado central → juicio SÍ/NO en mitades táctiles L/R (reutiliza `ChoiceResponseLayer` extraído, A11); nivel alto: elegir de 4 versiones la única bien formada; 16–24 juicios.
**Duración recomendada:** 3–5 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Sutileza de la violación (concordancia flagrante → régimen preposicional sutil), longitud/anidamiento, distancia entre el error y su controlador (dependencias largas).
**Métricas capturadas:** Hits/FA/misses/CR por tipo de violación; d′ y criterio c (corrección log-lineal); latencia coarse (gate).
**Scoring específico:** d′ log-lineal con nMin A2 (≥20 señal + ≥20 ruido) ⇒ `confident`; `GRAM_score` = d′ normalizado a banda propia; null hasta nMin; el criterio c se reporta solo como feedback interno.
**Cómo afecta al score del bloque:** Trait SYN a Precisión 0.45 (sub-señal normativa, complementa scope_negation que es semántico-lógica).
**Criterio de progresión:** Sube sutileza/distancia cuando d′ sostenido sobre banda propia.
**Criterio de fallo:** d′≈0 con criterio extremo ⇒ mensaje de estrategia (estás respondiendo casi siempre lo mismo) + DECREASE.
**Anti-gaming:** "Siempre-no"/"siempre-sí" ⇒ absorbido por d′/criterio (no infla sensibilidad) + aviso; respuestas a cadencia fija sin lectura ⇒ LEN-D2 + entropía de respuesta LEN-D1; memorización ⇒ banco ≥80 paramétrico (violación×superficie) + `REPEATED_ITEM`.
**Persistencia necesaria:** Estándar del bloque + tabla señal/ruido por tipo en el summary.
**Feedback inmediato:** Correcto/incorrecto + dónde estaba la ruptura (tras responder).
**Feedback post-sesión:** d′ propio (como "sensibilidad"), tipo de violación más difícil.
**Feedback histórico:** Tendencia de d′ por tipo, banda propia.
**Riesgo científico:** Norma vs variedad dialectal del español (ítems limitados a violaciones no dialectales, validadas por 2 autores); conocimiento escolar como prior.
**Riesgo de UX:** Juicios binarios largos aburren — bloques de 16–24 con cierre limpio.
**Riesgo técnico:** Reusa ChoiceResponseLayer; cálculo d′ ya existe como patrón en el repo (corrección log-lineal, memo A2/Memoria) — portar a commonMain del módulo.
**Claim permitido:** "Entrenas y mides sensibilidad a la buena formación de oraciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** d′ con corrección log-lineal y nMin 20+20 (golden); balance 50/50 del banco (test estructural); adversarios siempre-no/siempre-sí ⇒ d′≈0 (red-team); banco ≥80 sin ítems dialectales.

### LEN-10 · Paráfrasis con equivalencia de conjunto

**Bloque:** Lenguaje · **Estado:** Nuevo (extiende el paradigma de equivalencia a conjuntos; delimitado de semantic_surgery) · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Paráfrasis receptiva (PAR).
**Subhabilidades:** Preservación de significado bajo reformulación múltiple, detección de pérdida/adición de matiz, tolerancia a variación superficial legítima.
**Hipótesis cognitiva:** Clasificación de equivalencia sobre conjuntos: marcar TODAS las reformulaciones que preservan el significado (0–3 válidas de 4–5) exige representar el contenido proposicional independiente de la superficie; se diferencia de semantic_surgery (detectar EL cambio único entre cuasi-equivalentes) — si la ortogonalidad empírica es baja (r>0.7), MERGE como familia única.
**Mecánica UX:** Oración fuente + 4–5 reformulaciones-chip → multi-select de las equivalentes (a veces ninguna) → confirmar → feedback por chip; 8–10 ítems.
**Duración recomendada:** 5–7 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Distancia superficial de las válidas (misma sintaxis → reestructuración completa), sutileza del cambio en las inválidas, nº de candidatas, `validCount` 0–3.
**Métricas capturadas:** Jaccard de selección; over-claim (aceptar no-equivalentes) vs under-claim (rechazar equivalentes lejanas); latencia coarse (gate).
**Scoring específico:** `EvidenceSelection` (Jaccard exacto, reuso directo); `PAR_score=Σ(score·weight)/Σweight`, null si insuficiente.
**Cómo afecta al score del bloque:** Trait PAR a Precisión 0.45 y Amplitud 0.10.
**Criterio de progresión:** 1-up/2-down sobre distancia superficial de válidas + sutileza de inválidas.
**Criterio de fallo:** Under-claim dominante (rechaza equivalentes lejanas) ⇒ repair "misma proposición, otra superficie" + DECREASE.
**Anti-gaming:** "Marca-algo-siempre" ⇒ cuota ≥20% cero-válidas (test estructural); "marca-todo" ⇒ Jaccard exacto; posición ⇒ LEN-D1 + shuffle; sin-lectura ⇒ LEN-D2; memorización ⇒ `REPEATED_ITEM`.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Por chip: qué matiz se perdió/añadió en las inválidas marcadas.
**Feedback post-sesión:** Accuracy ponderada + sesgo over/under-claim propio.
**Feedback histórico:** Escalera PAR + evolución del sesgo.
**Riesgo científico:** Frontera equivalencia/no-equivalencia tiene zona gris — rúbrica cerrada por tipo de cambio validada por 2 autores; ortogonalidad con semantic_surgery pendiente de datos.
**Riesgo de UX:** Leer 5 reformulaciones cansa — fuente corta (≤20 palabras).
**Riesgo técnico:** Cero código nuevo de scoring (reuso EvidenceSelection).
**Claim permitido:** "Entrenas y mides reconocimiento de paráfrasis que preservan el significado dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥50 con cuota cero-válidas y rúbrica validada; test de ortogonalidad con semantic_surgery planificado (criterio MERGE documentado); adversarios marca-todo/marca-algo ≤ chance+10.

### LEN-11 · Detección de ambigüedad

**Bloque:** Lenguaje · **Estado:** Nuevo (monitorización de doble lectura; paradigma de ambigüedad estructural/léxica offline) · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Monitorización sintáctico-semántica (SYN/INF).
**Subhabilidades:** Ambigüedad de adjunción ("vi al hombre con el telescopio"), de alcance, léxica (polisemia), detección de NO-ambigüedad (control).
**Hipótesis cognitiva:** Detección y resolución de ambigüedad: notar que un enunciado admite más de una lectura exige mantener representaciones alternativas en vez de comprometerse con la primera (fenómeno garden-path, versión offline sin RT); el balance 50/50 ambiguo/no-ambiguo convierte la fase 1 en señal d′.
**Mecánica UX:** Fase 1: enunciado → ¿admite más de una lectura? SÍ/NO (mitades L/R). Fase 2 (solo si es ambiguo y acertó): seleccionar las 2 lecturas correctas de 4 propuestas (multi-select). 10–14 ítems.
**Duración recomendada:** 5–7 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Tipo de ambigüedad (léxica obvia → adjunción sutil → alcance), plausibilidad de la lectura secundaria (muy plausible → requiere esfuerzo), longitud.
**Métricas capturadas:** d′ de detección (fase 1, balance 50/50); accuracy de resolución (fase 2); latencia coarse (gate).
**Scoring específico:** Fase 1: d′ log-lineal (nMin 20+20); fase 2: Jaccard sobre {2 lecturas} (`EvidenceSelection`); `AMB_score` combina 0.6·d′norm + 0.4·resolución, null hasta nMin de ambas.
**Cómo afecta al score del bloque:** Traits SYN+INF (sub-señal de monitorización) a Precisión 0.45; nunca gobierna sola.
**Criterio de progresión:** Sube sutileza del tipo cuando d′ sostenido; fase 2 se activa desde dificultad 3.
**Criterio de fallo:** FA altas (ve ambigüedad donde no la hay) ⇒ repair con contraejemplos + DECREASE.
**Anti-gaming:** "Siempre-sí/siempre-no" ⇒ absorbido por d′; adivinanza en fase 2 ⇒ Jaccard exacto de 2-de-4 (azar 1/6); posición ⇒ LEN-D1; memorización ⇒ banco ≥60 + `REPEATED_ITEM`; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Estándar del bloque + tabla señal/ruido fase 1.
**Feedback inmediato:** Las dos lecturas explicitadas tras responder (aha didáctico controlado, serio).
**Feedback post-sesión:** Sensibilidad de detección + accuracy de resolución.
**Feedback histórico:** Tendencia d′ por tipo de ambigüedad.
**Riesgo científico:** La "no-ambigüedad" absoluta es difícil de garantizar — validación de ítems control por 2 autores (κ sobre etiqueta ambiguo/no-ambiguo ≥0.75, patrón D14).
**Riesgo de UX:** Fase 2 condicional puede confundir — transición explícita ("este era ambiguo: elige las 2 lecturas").
**Riesgo técnico:** Flujo de 2 fases en el holder (patrón processing-fase de Memoria EX1: añadir variante + rama exhaustiva del renderer EN EL MISMO cambio, regla 2026-06-28).
**Claim permitido:** "Entrenas y mides detección de dobles lecturas en enunciados dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥60 balanceado 50/50 con κ≥0.75 en etiquetas; d′ golden; flujo 2 fases con test de UiState exhaustivo; red-team siempre-sí/no.

### LEN-12 · Registro y tono

**Bloque:** Lenguaje · **Estado:** Nuevo (adecuación pragmática; usa el trait PRAG ya definido en la taxonomía) · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Adecuación pragmática (PRAG).
**Subhabilidades:** Ajuste de registro (formal/informal), tono (directo/atenuado), adecuación al destinatario y al canal, detección de desajuste.
**Hipótesis cognitiva:** Juicio de adecuación de registro con rúbrica cerrada (variación de registro/diseño para la audiencia, sociolingüística aplicada): elegir la reformulación adecuada a un destinatario+propósito dados mide el control pragmático receptivo; la pauta es explícita en el ítem para medir aplicación, no convención social del usuario.
**Mecánica UX:** Situación dada (destinatario, propósito, canal) + mensaje base + 4 reformulaciones → elegir la adecuada → feedback por defecto del distractor (demasiado informal / rígido / ambiguo de tono / inadecuado al canal); 8–12 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Distancia entre registros de las candidatas (polos claros → matices), nº de restricciones simultáneas (destinatario+canal+propósito), presión de cortesía vs claridad.
**Métricas capturadas:** Accuracy; perfil de error por tipo de desajuste; latencia coarse (gate).
**Scoring específico:** `SingleChoice`; `PRAG_score=Σ(score·weight)/Σweight`, null si insuficiente; perfil por tipo null si n<8.
**Cómo afecta al score del bloque:** Trait PRAG a Precisión 0.45 y Amplitud 0.10 (amplía la cobertura del constructo verbal).
**Criterio de progresión:** 1-up/2-down sobre distancia de registros + restricciones simultáneas.
**Criterio de fallo:** Error dominante en un polo (siempre elige lo más formal) ⇒ repair de calibración de registro.
**Anti-gaming:** "Elige-lo-más-formal-siempre" ⇒ balance del banco donde lo formal NO siempre es correcto (test estructural bidireccional, análogo al decoy de source_triage); posición ⇒ LEN-D1; memorización ⇒ `REPEATED_ITEM`; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Qué restricción de la pauta violó el distractor elegido.
**Feedback post-sesión:** Accuracy + polo de desajuste dominante propio.
**Feedback histórico:** Escalera PRAG.
**Riesgo científico:** El MÁS sensible a convención cultural del catálogo: la "adecuación" es normativa — rúbrica cerrada validada por ≥2 autores (κ≥0.75) y claim restringido a "según la pauta dada"; sin esa validación el ejercicio NO se publica.
**Riesgo de UX:** Situaciones artificiosas — redactar escenarios laborales/cotidianos neutros.
**Riesgo técnico:** Bajo (single-choice existente).
**Claim permitido:** "Entrenas y mides la elección del tono adecuado según la pauta dada dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥50 con κ≥0.75 en la etiqueta de adecuación; balance bidireccional formal/informal (test); copy sin juicios de personalidad; scanner verde.

### LEN-13 · Analogías verbales

**Bloque:** Lenguaje · **Estado:** Nuevo (ítems propios paramétricos; disciplina estricta de claims — nada de razonamiento general) · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Razonamiento relacional verbal (ARG/LEX_D).
**Subhabilidades:** Extracción de la relación A:B, mapeo a C:?, inhibición del asociado léxico fuerte no-análogo, taxonomía de relaciones (sinonimia, antonimia, parte-todo, causa-efecto, función, grado).
**Hipótesis cognitiva:** Analogía proporcional A:B::C:? — la resolución exige representar la RELACIÓN y proyectarla, no la asociación superficial; el distractor diagnóstico clásico (asociado fuerte de C con relación incorrecta) separa mapeo relacional de asociación léxica. Ítems 100% propios generados por parámetros (relación×superficie), jamás material publicado (contaminación/copyright).
**Mecánica UX:** "A es a B como C es a ___" + 4–5 opciones → tap → feedback nombrando la relación; 10–14 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Abstracción de la relación (parte-todo concreto → grado abstracto), fuerza del asociado-señuelo, frecuencia léxica de los términos, relaciones cruzadas (nivel alto: elegir el par que NO cumple la relación).
**Métricas capturadas:** Accuracy; perfil de error por tipo de relación y por señuelo-asociativo elegido; latencia coarse (gate).
**Scoring específico:** `SingleChoice`; `ANA_score=Σ(score·weight)/Σweight`, null si insuficiente; `associativeCaptureRate` (señuelos asociativos elegidos / trials con señuelo) como sub-señal.
**Cómo afecta al score del bloque:** Traits ARG/LEX_D a Precisión 0.45; delimitación frente a Lógica documentada (contenido léxico-semántico vs forma proposicional) para no duplicar varianza.
**Criterio de progresión:** 1-up/2-down sobre abstracción de la relación + fuerza del señuelo.
**Criterio de fallo:** `associativeCaptureRate`≥0.4 en ventana 6 ⇒ repair "relación, no asociación" + DECREASE.
**Anti-gaming:** Memorización ⇒ generación paramétrica relationId×surfaceVariant + ventana de no-repetición de relationId (patrón A9 de Lógica) + `REPEATED_ITEM`; posición ⇒ LEN-D1 + shuffle; sin-lectura ⇒ LEN-D2; heurística "el término más raro" ⇒ balance de frecuencia (test estructural).
**Persistencia necesaria:** Estándar del bloque + relationId servidos (exposure por familia).
**Feedback inmediato:** La relación correcta nombrada ("parte-todo") y por qué el señuelo era solo asociación.
**Feedback post-sesión:** Accuracy + relación más débil + tasa de captura asociativa.
**Feedback histórico:** Perfil por tipo de relación cuando n≥8/tipo.
**Riesgo científico:** Es el formato más asociado culturalmente a tests de capacidad — disciplina de claims máxima (el copy-scanner ya bloquea los lexemas; además prohibir "razonamiento general" en el copy del ejercicio); conocimiento léxico como prior (balance de frecuencia).
**Riesgo de UX:** Percepción de "examen" — encuadre de práctica contra tu línea base.
**Riesgo técnico:** Generador paramétrico de pares por relación (nuevo, commonMain puro, testeable).
**Claim permitido:** "Entrenas y mides el mapeo de relaciones entre palabras dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Generador paramétrico con tests de no-repetición y balance; banco/familias ≥8 relaciones × ≥10 superficies; adversario asociativo (elige-el-asociado) documentado en red-team; delimitación frente a Lógica escrita en la definición.

### LEN-14 · Comprensión con inferencia (multi-frase)

**Bloque:** Lenguaje · **Estado:** Nuevo (inferencia puente a nivel de pasaje; complementa evidence_lock sin anclaje explícito) · **Prioridad:** P1 (segundo candidato post-MVP) · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Modelo de situación / inferencia integrativa (SIT/INF).
**Subhabilidades:** Inferencia puente entre frases no adyacentes, integración de referencia (anáforas — cubre la sugerencia del panel science), distinción dicho/no-dicho, control epistémico ("no se puede saber").
**Hipótesis cognitiva:** Inferencias puente e integración multi-frase (marco de construcción-integración en comprensión de discurso): responder preguntas cuya respuesta exige combinar ≥2 frases separadas mide la construcción del modelo de situación, no la búsqueda literal; la opción "el texto no lo dice" con cuota controlada mide control de sobre-inferencia a nivel de pasaje (espejo del `INF_OVER` de evidence_lock).
**Mecánica UX:** Pasaje (80–150 palabras) → 2–3 preguntas de 4 opciones, SIEMPRE con la opción "el texto no lo dice" → tap → feedback; 4–6 pasajes.
**Duración recomendada:** 6–9 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Distancia entre las frases a integrar, densidad de referencia (anáforas encadenadas), plausibilidad del contenido no-dicho, longitud del pasaje.
**Métricas capturadas:** Accuracy; tasa de sobre-inferencia (elegir contenido plausible no dicho); tasa de infra-inferencia (elegir "no lo dice" cuando sí se sigue); latencia de lectura coarse (gate).
**Scoring específico:** `SingleChoice`; `SIT_score=Σ(score·weight)/Σweight`, null si insuficiente; sub-señales over/under-inference con cuota de banco conocida.
**Cómo afecta al score del bloque:** Traits SIT/INF a Precisión 0.45; con evidence_lock forma la familia de inferencia (anclada vs integrativa) — sub-señales distintas documentadas.
**Criterio de progresión:** 1-up/2-down sobre distancia de integración + densidad de referencia.
**Criterio de fallo:** Sobre-inferencia ≥0.4 ⇒ repair epistémico ("plausible no es lo mismo que dicho") + subir cuota de "no-lo-dice".
**Anti-gaming:** "Elige-siempre-no-lo-dice" ⇒ cuota controlada ~25% donde es correcta (test estructural; el adversario cae al 25%); posición ⇒ LEN-D1 + shuffle; sin-lectura ⇒ LEN-D2 con suelo por longitud de pasaje; memorización ⇒ banco ≥40 pasajes + `REPEATED_ITEM`.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** Las 2 frases que había que integrar, señaladas tras responder.
**Feedback post-sesión:** Accuracy + sesgo epistémico propio (sobre/infra-inferencia).
**Feedback histórico:** Escalera SIT + tendencia del sesgo epistémico.
**Riesgo científico:** Conocimiento previo del tema (temas neutros + `HIGH_PRIOR_KNOWLEDGE`); pasajes largos introducen fatiga de lectura como confound (FatigueGate activo).
**Riesgo de UX:** Pasajes de 150 palabras en móvil — scroll cómodo y pregunta visible sin perder el texto.
**Riesgo técnico:** Bajo (single-choice existente + banco).
**Claim permitido:** "Entrenas y mides integración de información entre frases de un texto dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥40 pasajes con cuota "no-lo-dice" ~25% (test estructural); adversario elige-no-lo-dice-siempre ≤ 25%+10; validación de unicidad de clave por 2 autores; persistencia verde.

### LEN-15 · Definiciones precisas

**Bloque:** Lenguaje · **Estado:** Nuevo (profundidad definicional con rúbrica género-diferencia) · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** Profundidad léxica definicional (LEX_D).
**Subhabilidades:** Discriminación exacto/demasiado-amplio/demasiado-estrecho/circular, sensibilidad al rasgo distintivo, precisión conceptual.
**Hipótesis cognitiva:** Selección de definición por estructura género-diferencia: reconocer la definición que delimita exactamente el concepto (ni sobre-incluye ni sub-incluye ni es circular) mide representación léxica profunda, más allá de la familiaridad; los defectos de las falsas son la rúbrica cerrada.
**Mecánica UX:** Término en contexto mínimo → 4 definiciones candidatas → elegir la exacta; nivel alto: además clasificar el defecto de una señalada (amplia/estrecha/circular); 10–12 ítems.
**Duración recomendada:** 4–6 min · **Dificultad mínima:** 1 · **Dificultad máxima:** 10
**Variables de dificultad:** Abstracción del término (concreto → abstracto), sutileza del defecto (circularidad obvia → sobre-amplitud fina), cercanía semántica de las candidatas.
**Métricas capturadas:** Accuracy; perfil de error por tipo de defecto elegido; accuracy de clasificación del defecto (nivel alto); latencia coarse (gate).
**Scoring específico:** `SingleChoice`; nivel alto compuesto 0.7·selección + 0.3·clasificación; `DEF_score=Σ(score·weight)/Σweight`, null si insuficiente.
**Cómo afecta al score del bloque:** Trait LEX_D a Precisión 0.45 (sub-señal definicional, complementa LEN-08 contextual).
**Criterio de progresión:** 1-up/2-down sobre abstracción + sutileza del defecto.
**Criterio de fallo:** Error dominante hacia "demasiado-amplia" ⇒ repair del rasgo distintivo.
**Anti-gaming:** "La más larga es la correcta" ⇒ balance de longitud entre candidatas (test estructural de no-correlación longitud↔clave); posición ⇒ LEN-D1 + shuffle; memorización ⇒ `REPEATED_ITEM` + banco ≥50; sin-lectura ⇒ LEN-D2.
**Persistencia necesaria:** Estándar del bloque.
**Feedback inmediato:** El defecto de la elegida nombrado ("incluye casos que no son X").
**Feedback post-sesión:** Accuracy + tipo de defecto que más se te escapa.
**Feedback histórico:** Escalera LEX_D (sub-señal definicional).
**Riesgo científico:** Fuerte carga de conocimiento cristalizado/educativo — solo banda propia, jamás comparación; términos técnicos excluidos (solo léxico general).
**Riesgo de UX:** Leer 4 definiciones densas — candidatas ≤25 palabras.
**Riesgo técnico:** Bajo (single-choice existente).
**Claim permitido:** "Entrenas y mides precisión al delimitar significados dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥50 con rúbrica de defectos validada por 2 autores; test de no-correlación longitud↔clave; min-n por tipo de defecto; scanner verde.

---

## 8. Emociones — 15 Exercises

Propósito: control funcional de la respuesta bajo carga afectiva leve, medido como proceso (consistencia, reactividad, recuperación, interpretación) contra la propia línea base. Política de scoring del bloque: los índices internos por ejercicio son 0–1 con gates (null si n<nMin, jamás 0 fabricado); `normalizedScore` del bloque permanece **null** y la dimensión EMOTIONAL_REGULATION muestra "—" hasta holdout/baseline (Ola 4, memo A8); hasta entonces el usuario ve bandas cualitativas de proceso en el informe. Velocidad tiene peso 0 en el composite (timebase no autoritativa). Fuentes reutilizadas: catálogo in-code `EmotionExerciseCatalog` (EM1–EM15) y diseños EM-A…EM-H de `docs/mindboost_exercise_design/03_7_Emociones.md`, reconciliados con `04_7_Emociones_audit.md`.

**Selección MVP (exactamente 5):**

| Ejercicio | Justificación (1 línea) |
|---|---|
| EM1 Pausa antes de actuar | Go/no-go afectivo vivo y sano; ancla de consistencia y reactividad (congruencia). |
| EM2 Recuperación tras error | Composite post-error honesto ya codificado; ancla de la banda recuperación. |
| EM3 Separador | Único ejercicio de interpretación; banco 40→80 es el único cambio (P2). |
| EM10 Separación bajo presión leve | Única manipulación de dificultad real del paradigma de separación; alimenta reactividad. |
| EM11 Continuidad tras feedback negativo neutro | Garantiza la suficiencia de eventos post-error (BUR≥12) que EM2 base no asegura. |

(EM8 sigue jugable en modo libre/avanzado; fuera de cuota por ser clon de señal de EM1.)

**Requisitos de banco de contenido:** `PAUSA_WORDS` 24→48 (distractor, leve-only, escaneado); `SEPARATOR_ITEMS` 40→80 con inter-rater ≥80%/κ≥0.75 por ítem, pools train/eval disjuntos y holdout SERVIDO en modo evaluación, `bankVersion` + `ExposureTracker` (no repetir ítem hasta agotar pool); EM5 banco nuevo de 50 escenarios texto (2 opciones, evidencia computable, señuelo afectivo no correlacionado con la correcta, inter-rater); EM9 banco nuevo de 80 ítems de vocabulario funcional (4 opciones, inter-rater, disjunto de SEPARATOR_ITEMS). Todo banco que alimente perfil pasa `BankValidator`/`BankIntegrity`; sin validar → el gate bloquea (estado honesto).

### Pausa antes de actuar

**Bloque:** Emociones · **Estado:** Mejorar · **Prioridad:** P2 · **Apto MVP:** Sí · **Destino:** MVP
**Habilidad cognitiva principal:** Inhibición de respuesta bajo distractor afectivo leve (`PAUSE_BEFORE_ACTION`). ID técnico vivo: `EM1_PAUSA` (`EM1PausaGenerator.kt`).
**Subhabilidades:** Discriminación go/no-go, resistencia a interferencia por congruencia, mantenimiento de criterio de respuesta.
**Hipótesis cognitiva:** Paradigma go/no-go afectivo clásico: el marco (abierto/cerrado) decide la respuesta y la palabra afectiva es SOLO distractor ("tone never decides"); la tasa de comisión indexa el control inhibitorio y el delta congruente/incongruente la interferencia (literatura go/no-go emocional; sin ítems con copyright).
**Mecánica UX:** Self-report inicial → marco abierto=tocar / cerrado=retener, un tap, 2 bloques de ~10 trials, ventana 2.5 s; barra de seguridad siempre visible (Pausar/Reducir/Salir ≥48dp).
**Duración recomendada:** 4–5 min (2 bloques). **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** `INHIBITORY_DEMAND` (proporción no-go 0.20–0.40), ratio incongruente (0.0–0.6), ventana (2.5 s → x1.0), ISI jitter; intensidad del banco NUNCA sube automáticamente (techo 3).
**Métricas capturadas:** `COMMISSION_RATE` (gobernante, no RT-crítica, MIN_NOGO=6), `AFFECTIVE_INTERFERENCE_COST` (MIN_CELL=5), `RT_MEDIAN`/`ANTICIPATIONS` (display-only).
**Scoring específico:** `commissionScore = 1 − COMMISSION_RATE`, válido solo con n no-go ≥ nMin Y go-accuracy ≥ 0.6 (gate nuevo GA-1); null si `!confident`; nunca 0 fabricado.
**Cómo afecta al score del bloque:** Hoy solo `dataSufficiency/confidence/trend` vía `EmotionProfileSource` (normalizedScore null). Post-Ola4: componente Precisión (0.30) y Consistencia (0.30, estabilidad inter-sesión de comisión); congruencia → banda reactividad.
**Criterio de progresión:** Mastery de zona COMMISSION + 0 errores consecutivos + self-report ≤2 → +0.07 en el eje; plateau → cambia a ratio incongruente.
**Criterio de fallo:** Comisión sobre banda objetivo → −0.12; go-accuracy <0.6 → bloque INVÁLIDO (no puntúa, no cuenta para calibración).
**Anti-gaming:** "No responder nunca" (comisión 0 trivial) → suelo go-accuracy invalidante con par adversario/honesto (hoy AUSENTE, el fix del ejercicio); tap rítmico → detector de cadencia IRI (patrón D1); anticipaciones <350 ms excluidas de HIT; COMMISSION_SPIKE (0.30) fuerza check-in.
**Persistencia necesaria:** Resúmenes `EmotionSessionSummaryDto`/`EmotionAggregateDto` cifrados (namespace Emotions, ya vivo); + `SessionRecord` write-ahead (A12) con `exerciseVersion`.
**Feedback inmediato:** Borde verde/rojo + glifo neutro ~200 ms, sin culpa.
**Feedback post-sesión:** Informe 4 partes + banda de comisión y delta de congruencia con nota de confianza ("qué no se puede concluir aún").
**Feedback histórico:** Tendencia de comisión contra línea base propia; "—" sin datos.
**Riesgo científico:** Vender interferencia afectiva con celdas insuficientes → gate MIN_CELL. **Riesgo de UX:** monotonía del tap único → variar banco/ISI. **Riesgo técnico:** tocar `goNoGoMetrics` sin golden previo (A5) — golden-first obligatorio.
**Claim permitido:** "Entrenas la pausa antes de responder y mides tus respuestas de más en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Golden congela el scorer actual; suelo go-accuracy implementado+testeado (adversario "nunca responde" → bloque inválido); test de paridad tono↔respuesta; scanners 3 variantes verdes; salida de seguridad no altera ninguna métrica.

### Recuperación tras error

**Bloque:** Emociones · **Estado:** Mejorar · **Prioridad:** P2 · **Apto MVP:** Sí · **Destino:** MVP
**Habilidad cognitiva principal:** Recuperación funcional post-error (`POST_ERROR_RECOVERY`). ID vivo: `EM2_RECUPERACION` (`EM2RecuperacionGenerator.kt`).
**Subhabilidades:** Estabilidad de accuracy tras fallo, control de sobre-frenado (post-error slowing) y de impulsividad compensatoria.
**Hipótesis cognitiva:** Literatura de post-error adjustments (slowing/accuracy): host perceptual 2-alternativas NEUTRO con trials difíciles plantados determinísticamente (cada 5) que garantizan errores normales sin inducción injusta; el composite penaliza slowing SOLO si la accuracy no se recuperó.
**Mecánica UX:** Elección izquierda/derecha entre magnitudes en mitades táctiles full-screen; feedback neutro; misma barra de seguridad.
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** `POST_ERROR_LOAD` (densidad de trials difíciles 0.15–0.45), discriminabilidad base 0.90→0.45, ventana, ISI.
**Métricas capturadas:** `POST_ERROR_RECOVERY` (gobernante, MIN_POST_ERROR=3 por bloque; ≥12 eventos para banda inter-sesión), `POST_ERROR_ACCURACY`, `ACCURACY`, `RT_CV`/`POST_ERROR_SLOWING` (display-only), `ABANDONMENT` (seguridad).
**Scoring específico:** Composite condicional vivo: penaliza slowing solo si `postErrAcc < baseAcc − ACC_TOL`; penaliza impulsividad post-error (<250 ms) por separado; `unavailable` si pares <3 — jamás 0.
**Cómo afecta al score del bloque:** Banda recuperación; post-Ola4 componente Control de errores (0.20). Hoy solo sufficiency/confidence/trend.
**Criterio de progresión:** Recuperación estable en banda TOLERANCE (0.70–0.80 accuracy) → sube densidad/discriminabilidad.
**Criterio de fallo:** Accuracy bajo banda o proxy `POST_ERROR_ACCURACY_DROP` → step-down; <3 pares → métrica `unavailable` (no penaliza).
**Anti-gaming:** Fabricar errores baratos respondiendo rápido-aleatorio → excluir anticipaciones del set de errores genuinos (fix concreto en `postErrorRecovery`, hoy cuenta cualquier INCORRECT) + patrón D3 (fast-guessing) + la impulsividad post-error ya penaliza; trials difíciles plantados por seed no predecibles.
**Persistencia necesaria:** Igual que EM1 (resúmenes cifrados + SessionRecord A12).
**Feedback inmediato:** Pulso neutro en error, transición suave (anti-rumiación).
**Feedback post-sesión:** Informe 4 partes + banda de recuperación con n de pares y nota de confianza.
**Feedback histórico:** Tendencia de recuperación vs línea base; "—" sin n suficiente.
**Riesgo científico:** Composite ruidoso con pocos pares → `unavailable` honesto + EM11 aporta densidad. **Riesgo de UX:** sensación de fallo frecuente → OverloadDetector/check-in ya contienen. **Riesgo técnico:** cambiar la definición de error sin golden → regresión del composite.
**Claim permitido:** "Entrenas volver a tu rendimiento después de un error y mides tu recuperación en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Golden del composite; test adversarial anticipación-como-error (rápido-incorrecto NO abre ventana post-error); suficiencia ≥3 pares probada en el peor seed; abandono no altera la banda.

### Separador de registros

**Bloque:** Emociones · **Estado:** Mejorar · **Prioridad:** P2 · **Apto MVP:** Sí · **Destino:** MVP
**Habilidad cognitiva principal:** Separación categorial de registros internos (`EMOTION_THOUGHT_SEPARATION`, `GRANULARITY`). ID vivo: `EM3_SEPARADOR` (`EM3SeparadorGenerator.kt`).
**Subhabilidades:** Clasificación 4 vías (estado interno/pensamiento/impulso/hecho), granularidad, lectura precisa bajo neutralidad.
**Hipótesis cognitiva:** Clasificación categorial con matriz de confusión completa y corrección por azar (chance=0.25) sobre frases neutras domésticas — mide discriminación de registros SIN contenido de vida cargado (leve-only §10.6); `TransferLabel.NOT_DEMONSTRATED` pinneado (honesto).
**Mecánica UX:** Frase central → 1 de 4 botones en grid 2x2 estable; feedback resalta la categoría correcta.
**Duración recomendada:** 4–6 min (menos trials, más lectura). **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** `CATEGORY_AMBIGUITY` (proporción de ítems ambiguos del banco ampliado; hoy el generador ignora difficulty — la ampliación a 80 con etiquetas de ambigüedad ACTIVA este eje), longitud de frase, densidad de cuasi-sinónimos.
**Métricas capturadas:** `CATEGORY_ACCURACY`, `EMOTION_THOUGHT_SEPARATION` (ajustada por azar), `GRANULARITY_INDEX` (gated por n por categoría), `RT_MEDIAN` (display-only), matriz 4x4.
**Scoring específico:** `separationScore = 0.6·adjAcc + 0.4·granularity`, `adjAcc=(acc−0.25)/0.75` clamp 0..1; null si `!confident`; la "verdad" de ítem solo cuenta para graduación cuando el banco tenga inter-rater documentado.
**Cómo afecta al score del bloque:** Banda interpretación (la señal dominante §14.7); post-Ola4 componente Precisión/Interpretación (0.30).
**Criterio de progresión:** Mastery en zona GENERAL → sube ambigüedad; plateau → más pares cuasi-sinónimos.
**Criterio de fallo:** Accuracy < banda → step-down; matriz degenerada (colapso a 1 categoría) → bloque inválido.
**Anti-gaming:** Botón-spam → detector de entropía de respuesta (patrón D6; hoy NO implementado — se añade con par adversario/honesto) + `GRANULARITY_INDEX` degenerado; respuestas sin lectura → RT-floor proporcional a longitud; memorización → holdout eval SERVIDO en modo evaluación (hoy fantasma) + ExposureTracker con banco 80.
**Persistencia necesaria:** Resúmenes cifrados + versión de banco (`bankVersion`) en el DTO (bump de `schemaVersion`).
**Feedback inmediato:** Categoría correcta resaltada, neutro.
**Feedback post-sesión:** Informe 4 partes + banda de interpretación + confusiones más frecuentes (descriptivo, sin lectura de la persona).
**Feedback histórico:** Tendencia de separación ajustada por azar vs línea base.
**Riesgo científico:** Sin inter-rater mide concordancia con el autor (BLOCK del audit 04_7 para graduación) → precondición de Ola4, el ejercicio sigue jugable como práctica. **Riesgo de UX:** ambigüedad pensamiento/impulso frustra → descartar ítems sin acuerdo. **Riesgo técnico:** ampliar banco rompe scanner → pasar 3 variantes antes de cerrar (regla 2026-07-01).
**Claim permitido:** "Entrenas distinguir estado interno, pensamiento, impulso y hecho, y mides tu precisión al separarlos en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco 80 con inter-rater ≥80% por ítem y desacuerdos descartados; holdout activo servido; detector de entropía + RT-floor testeados; coherencia comentario↔cálculo del índice revisada; scanners verdes.

### Anti-impulso por comisión

**Bloque:** Emociones · **Estado:** Mantener · **Prioridad:** P3 · **Apto MVP:** Sí (jugable) · **Destino:** Post-MVP (modo libre/avanzado)
**Habilidad cognitiva principal:** Inhibición pura por comisión (`PAUSE_BEFORE_ACTION`). ID vivo: `EM8_ANTIIMPULSO` (`EM8AntiImpulsoGenerator.kt`).
**Subhabilidades:** Retención de respuesta con alta demanda de no-go (0.40), sin balanceo de congruencia.
**Hipótesis cognitiva:** Variante paramétrica del go/no-go de EM1 con mayor proporción de no-go: misma señal (comisión), mayor carga inhibitoria — es un preset de dificultad, no un constructo nuevo (audit 04_7: MERGE lógico en EM1).
**Mecánica UX:** Idéntica a EM1 (mismo renderer/clasificador, reutilización deliberada documentada).
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 6 (arranca alto por diseño). **Dificultad máxima:** 10.
**Variables de dificultad:** No-go ratio 0.40 fijo alto; ventana; ISI.
**Métricas capturadas:** `COMMISSION_RATE` (gobernante), `RT_MEDIAN`, `IMPULSIVITY` (display-only).
**Scoring específico:** Igual que EM1 (mismo motor `goNoGoMetrics`); hereda el suelo go-accuracy cuando se implemente.
**Cómo afecta al score del bloque:** Igual que EM1 (misma varianza); en el menú se presenta como "variante avanzada de Pausa" para no inflar la variedad percibida (honestidad de catálogo).
**Criterio de progresión:** El de EM1 sobre el mismo eje.
**Criterio de fallo:** El de EM1; go-accuracy <0.6 invalida.
**Anti-gaming:** Los de EM1 (suelo go-accuracy, cadencia D1, floor 350 ms).
**Persistencia necesaria:** La de EM1.
**Feedback inmediato / post-sesión / histórico:** Los de EM1, etiquetado como variante.
**Riesgo científico:** Cero señal incremental — presentarlo como ejercicio distinto reduce la honestidad del catálogo (por eso sale de la cuota MVP). **Riesgo de UX:** duplicidad percibida. **Riesgo técnico:** ninguno (vivo y verde).
**Claim permitido:** "Entrenas retener el impulso cuando la regla lo pide y mides tus respuestas de más en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Sigue en registry y alcanzable en modo libre (test fija los ids alcanzables); golden que fija EM8 = EM1 con no-go ratio mayor; menú lo etiqueta como variante.

### Separación bajo presión leve

**Bloque:** Emociones · **Estado:** Mantener · **Prioridad:** P2 (hereda banco EM3) · **Apto MVP:** Sí · **Destino:** MVP
**Habilidad cognitiva principal:** Separación categorial con presión temporal leve (`EMOTION_THOUGHT_SEPARATION`). ID vivo: `EM10_SEP_PRESION` (`EM10SepPresionGenerator.kt`).
**Subhabilidades:** Interpretación rápida, mantenimiento de precisión bajo ventana reducida.
**Hipótesis cognitiva:** Manipulación speed-accuracy sobre el paradigma de EM3: ventana ×0.65 (mínimo 900 ms) — presión REAL de tiempo, no fabricada; el delta de accuracy vs EM3 a igual dificultad base es la medida de reactividad del bloque.
**Mecánica UX:** Idéntica a EM3 con ventana corta visible.
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 3. **Dificultad máxima:** 10.
**Variables de dificultad:** `TEMPORAL_PRESSURE` (factor de ventana 0.65→ y suelo 900 ms), + las de EM3 (banco 80).
**Métricas capturadas:** `CATEGORY_ACCURACY`, `EMOTION_THOUGHT_SEPARATION`, `RT_MEDIAN` (display-only); delta vs EM3 → banda reactividad (≥20/celda para reporte).
**Scoring específico:** El de EM3; el delta de presión se computa SOLO con n suficiente en ambas condiciones (difference-score secundario, A2).
**Cómo afecta al score del bloque:** Banda reactividad + interpretación; post-Ola4 componente Dificultad (0.15) como "precisión sostenida bajo presión".
**Criterio de progresión:** Mantener accuracy en banda con ventana decreciente.
**Criterio de fallo:** Accuracy < banda → ampliar ventana (step-down); omisiones masivas → check-in.
**Anti-gaming:** Los de EM3 (entropía D6, RT-floor por longitud, holdout servido); la ventana corta agrava el riesgo de respuesta sin lectura → RT-floor obligatorio.
**Persistencia necesaria:** La de EM3 + flag de condición (presión) en el resumen.
**Feedback inmediato / post-sesión / histórico:** Los de EM3 + delta bajo presión como banda con nota de confianza.
**Riesgo científico:** La latencia es load-bearing sin timebase autoritativa → el delta usa ACCURACY (no RT) como numerador; RT solo display. **Riesgo de UX:** presión percibida como agobio → suelo 900 ms + barra de seguridad. **Riesgo técnico:** ninguno nuevo.
**Claim permitido:** "Entrenas separar estado, pensamiento, impulso y hecho con presión temporal leve y mides tu precisión bajo presión en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Golden fija EM10 = EM3 con ventana ×0.65; delta de reactividad solo se reporta con ≥20/celda; hereda criterios de banco de EM3.

### Continuidad tras feedback negativo neutro

**Bloque:** Emociones · **Estado:** Mantener · **Prioridad:** P2 · **Apto MVP:** Sí · **Destino:** MVP
**Habilidad cognitiva principal:** Recuperación post-error con densidad garantizada (`POST_ERROR_RECOVERY`). ID vivo: `EM11_FEEDBACK_NEG` (`EM11ContinuidadGenerator.kt`).
**Subhabilidades:** Continuidad de rendimiento tras avisos de error frecuentes, estabilidad de criterio.
**Hipótesis cognitiva:** Mismo paradigma post-error que EM2 con trials difíciles plantados cada 3 (casi el doble): garantiza `MIN_POST_ERROR` y el volumen de eventos (BUR=12) que la banda de recuperación necesita — es la configuración de suficiencia estadística del paradigma (audit 04_7: su `plantIndices` es la solución que EM2 base necesita).
**Mecánica UX:** Idéntica a EM2; el feedback de error es un aviso neutro (sin carga).
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 3. **Dificultad máxima:** 10.
**Variables de dificultad:** Densidad de plantado (cada 3), discriminabilidad, ventana.
**Métricas capturadas:** `POST_ERROR_RECOVERY` (gobernante), `POST_ERROR_ACCURACY`, `ABANDONMENT` (seguridad).
**Scoring específico:** El de EM2 (mismo composite); la mayor densidad NO cambia la fórmula, solo el n.
**Cómo afecta al score del bloque:** Principal alimentador de la banda recuperación (volumen de pares); post-Ola4 componente Control de errores (0.20).
**Criterio de progresión:** El de EM2 en zona TOLERANCE.
**Criterio de fallo:** El de EM2; el `OverloadDetector`/`PauseGovernor` contienen la mayor densidad de error (mecanismo vivo).
**Anti-gaming:** Los de EM2 (exclusión de anticipaciones, D3, impulsividad post-error penalizada).
**Persistencia necesaria:** La de EM2.
**Feedback inmediato:** Aviso neutro de error, sin culpa, transición suave.
**Feedback post-sesión / histórico:** Los de EM2, con n de pares mayor y nota de confianza más alta.
**Riesgo científico:** Densidad de error alta roza carga acumulada → techo leve + check-in intermedio ya cablados (orden safety-first hardcodeado). **Riesgo de UX:** sensación de fallar mucho → copy de encuadre neutro escaneado. **Riesgo técnico:** ninguno (vivo y verde).
**Claim permitido:** "Entrenas continuar con control tras un aviso neutro de error y mides tu continuidad en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Golden fija EM11 = EM2 con densidad mayor; suficiencia ≥3 pares por bloque garantizada por seed (test peor-seed); abandono ABANDONED_SAFE sin efecto en bandas.

### Calibrador intensidad-rendimiento

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM4_CALIBRADOR` + diseño EM-E de 03_7, adaptado a calibración prospectiva por bloque) · **Prioridad:** P2 · **Apto MVP:** No (post-MVP; primero de la cola) · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Calibración metacognitiva prospectiva (`CALIBRATION_ACCURACY`).
**Subhabilidades:** Estimación del propio rendimiento, ajuste estimación↔dato real, monitoreo de estado funcional.
**Hipótesis cognitiva:** Paradigma de juicio de rendimiento (judgments of performance): antes de cada bloque el usuario estima su banda de acierto esperada (3 niveles) usando la maquinaria de `SelfReport` existente; el error de calibración = |estimación − accuracy real del bloque|. Metacognición medible sin contenido sanitario (lente science: "confianza vs rendimiento, metacognición medible").
**Mecánica UX:** Self-report + estimación de banda (bajo/medio/alto) → bloque 2-alt estándar (reusa `EmotionTwoAltField` y `EM2RecuperacionGenerator` como host) → comparación diferida estimación vs dato al final del bloque.
**Duración recomendada:** 5–6 min (2 bloques + estimaciones). **Dificultad mínima:** 2. **Dificultad máxima:** 8.
**Variables de dificultad:** Discriminabilidad mantenida en banda media (accuracy ~0.75 para que haya varianza que calibrar), granularidad de la estimación (2–3 niveles).
**Métricas capturadas:** `CALIBRATION_ERROR` (gobernante; hoy catalogada NO producida — se activa honestamente), `ACCURACY`.
**Scoring específico:** `calibrationScore = 1 − |estimación − accuracy|` por bloque, agregado solo con ≥4 bloques estimados (n mínimo); null si n insuficiente; nunca 0.
**Cómo afecta al score del bloque:** Banda interpretación (vertiente metacognitiva); post-Ola4 entra en Consistencia (0.30) como estabilidad de calibración. Hoy nada numérico.
**Criterio de progresión:** Error de calibración decreciente contra línea base → dificultad del host sube dentro de la banda.
**Criterio de fallo:** Estimación colapsada (siempre el mismo nivel) → bloque no computa calibración (entropía nula).
**Anti-gaming:** Colapso de estimación → detector de entropía de confianza (nuevo, patrón D6, con par adversario/honesto); feedback DIFERIDO por bloque (por trial permitiría ajuste por ensayo-error); estimación PRE-bloque no manipulable por el resultado.
**Persistencia necesaria:** Campo nuevo `estimatedBand` en el resumen de bloque (bump `schemaVersion`); resto igual.
**Feedback inmediato:** El del host 2-alt (neutro).
**Feedback post-sesión:** Comparación estimación vs dato por bloque + banda de calibración, con "qué no se puede concluir aún".
**Feedback histórico:** Tendencia del error de calibración vs línea base propia.
**Riesgo científico:** n bajo por sesión (2 bloques = 2 puntos) → agregación multi-sesión con nMin=4 antes de banda. **Riesgo de UX:** estimar antes de cada bloque alarga el loop → una sola pantalla, 3 botones. **Riesgo técnico:** requiere agregación nueva en `EmotionSessionAssembler` — aditiva, golden-first.
**Claim permitido:** "Entrenas ajustar tu estimación a tu acierto real y mides tu calibración en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** `CALIBRATION_ERROR` producida con gate de n; adversario colapso-de-estimación → no computa; feedback diferido testeado; scanners verdes; sin efecto en perfil (normalizedScore sigue null).

### Decisiones bajo contexto afectivo

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM5_SESGO_AFECTIVO`; decisión del panel: banco texto 50, reusa renderers propios) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Decisión basada en evidencia bajo señuelo afectivo leve (`AFFECTIVE_DECISION_QUALITY`).
**Subhabilidades:** Ponderación de evidencia textual, resistencia al atractivo superficial de una opción.
**Hipótesis cognitiva:** Paradigma de elección con señuelo (attraction/salience decoy adaptado a texto): 2 opciones donde la evidencia enunciada determina la correcta y una carga afectiva leve hace más atractiva a veces la incorrecta; acertar exige leer la evidencia, no el tono. La exactitud basada en evidencia (accuracy) es la señal — distinto del módulo Decisiones (aquí el manipulador es el señuelo afectivo del ítem, no probabilidad/reversibilidad).
**Mecánica UX:** Escenario breve neutro (2–3 líneas) → 2 opciones (reusa `EmotionTwoAltField` con textos); banco de 50 ítems escritos, balanceado (señuelo alineado/contrario a la correcta al 50%).
**Duración recomendada:** 5–7 min (lectura). **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** Sutileza de la evidencia, saliencia del señuelo, longitud del escenario; sin presión temporal fuerte (leve-only).
**Métricas capturadas:** Accuracy basada en evidencia (gobernante), delta señuelo-contrario vs señuelo-alineado (≥20/celda), `IMPULSIVITY` (display-only).
**Scoring específico:** `decisionScore = accuracy` ajustada por azar (chance 0.5 → `(acc−0.5)/0.5`); delta de señuelo solo con celdas suficientes; null si n<nMin.
**Cómo afecta al score del bloque:** Banda interpretación + reactividad (delta de señuelo); post-Ola4 Precisión (0.30). Nunca claim de "mejores decisiones".
**Criterio de progresión:** Accuracy en banda con señuelos más sutiles.
**Criterio de fallo:** Accuracy ~ azar → dificultad baja; colapso a "siempre la opción atractiva" → banda de reactividad lo refleja, sin culpa.
**Anti-gaming:** El señuelo NUNCA correlaciona con la correcta (test de paridad sobre el banco, 50/50 forzado) → elegir siempre lo atractivo rinde 50% (azar); spam de lado → entropía D6 + balance L/R; memorización → ExposureTracker + `bankVersion`.
**Persistencia necesaria:** Resumen con `bankVersion` e ids de ítem NO persistidos como contenido (solo agregados).
**Feedback inmediato:** Correcta resaltada + la evidencia clave subrayada (didáctico, neutro).
**Feedback post-sesión:** Banda de acierto por condición de señuelo; "qué no se puede concluir aún".
**Feedback histórico:** Tendencia de acierto vs línea base.
**Riesgo científico:** Roza far-transfer ("decidir mejor") → claim estrictamente intra-tarea; el audit 04_7 recomendaba RETIRE por solape — la reconciliación del panel lo mantiene SOLO como elección con señuelo textual, sin solapar con el módulo Decisiones (sin probabilidades, sin coste/beneficio). **Riesgo de UX:** carga de lectura → escenarios ≤3 líneas. **Riesgo técnico:** banco nuevo de 50 con inter-rater + scanner (los escenarios son texto: superficie de riesgo léxico máxima).
**Claim permitido:** "Entrenas decidir con la evidencia del ejercicio cuando una opción resulta más llamativa y mides tu acierto en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco 50 con inter-rater y test de paridad señuelo↔respuesta; adversario "siempre la atractiva" → ~azar y detectado; scanners 3 variantes verdes; sin solape mecánico con Decisiones (revisión cruzada).

### Persistencia bajo demora

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM6_TOLERANCIA` + diseño EM-D "Tolerance Hold" de 03_7) · **Prioridad:** P3 (frontera de seguridad) · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Persistencia segura ante demora (código: `FRUSTRATION_TOLERANCE`; nombre UI allowlist: "persistencia bajo demora").
**Subhabilidades:** Espera calibrada, inhibición de respuesta temprana, decisión de continuar/salir segura.
**Hipótesis cognitiva:** Paradigma de espera con ventana variable (delay-of-response): un objetivo "madura" en tiempo variable con varianza; tocar solo en la ventana final. La ÚNICA carga es la demora temporal (prueba de no-inducción documentada obligatoria); el abandono es SIEMPRE una salida válida y jamás puntúa en contra (`PLACEBO_RISK` → copy ultra-conservador).
**Mecánica UX:** Renderer NUEVO (timed-tap con barra de maduración + catch-trials); sin countdown numérico; Exit grande visible; 12–22 trials.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 1. **Dificultad máxima:** 8 (techo por seguridad).
**Variables de dificultad:** Tiempo de maduración 1.5–9 s, varianza (CV 0.1–0.5), anchura de ventana 900→300 ms, catch-trials 0–0.15.
**Métricas capturadas:** `SAFE_PERSISTENCE_INDEX` (gobernante = hits maduros / (hits+tempranas), abandonos EXCLUIDOS del denominador), `ANTICIPATIONS` tempranas, `ABANDONMENT` (señal de seguridad, jamás de fallo).
**Scoring específico:** `persistScore = matureHits/(matureHits+earlyTaps)`; null si denominador < nMin; `FRUSTRATION_TOLERANCE_INDEX` NO computable sin baseline previo (se difiere hasta tener línea base por usuario).
**Cómo afecta al score del bloque:** Banda consistencia (estabilidad de espera); post-Ola4 Dificultad (0.15). Nada hoy.
**Criterio de progresión:** Zona TOLERANCE estricta: sube maduración solo con mastery Y self-report ≤2; plateau → sube varianza, no el tiempo medio.
**Criterio de fallo:** `REPEATED_EXIT_ATTEMPTS`/`REPEATED_PAUSE` → check-in → reduce; nunca sube con intensidad >2.
**Anti-gaming:** Tap-spam → catch-trials que nunca maduran (responder a uno invalida el bloque, con par adversario/honesto); aprender el timer → varianza de maduración obligatoria; auto-clicker → variante hold (mantener y soltar).
**Persistencia necesaria:** Payload/DTO nuevos (`ToleranceHold`), rama `classifyTolerance` en el assembler, resumen cifrado estándar.
**Feedback inmediato:** Maduro/temprano por color+forma; abandono con mensaje neutro de validez ("salir es una opción válida").
**Feedback post-sesión:** Banda de persistencia + tempranas; sin lectura de estado de la persona.
**Feedback histórico:** Tendencia del índice vs línea base; "—" sin baseline.
**Riesgo científico:** El MÁS alto del bloque: prolongar espera para medir "aguante" ES inducción si se escala contenido → prohibido escalar contenido afectivo; solo demora; si la prueba de no-inducción no se puede documentar, se RETIRA. **Riesgo de UX:** aburrimiento con esperas largas → maduración ≤9 s, pocos trials. **Riesgo técnico:** renderer + rama de clasificación nuevos (la mayor deuda técnica de los 15).
**Claim permitido:** "Entrenas sostener la espera hasta la señal correcta y mides tus respuestas tempranas en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Prueba de no-inducción documentada; abandono excluido del denominador y sin culpa (test permanente); catch-trials + adversario tap-spam testeados; zona TOLERANCE con intensidad nunca auto-subida; nombre UI de la allowlist; scanners verdes.

### Reentrada tras pausa programada

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM7_REENTRADA`, rediseñado para resolver el problema de n del audit 04_7) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Estabilidad al retomar la tarea (`REGULATION_STABILITY`; nombre UI: "estabilidad al retomar").
**Subhabilidades:** Re-enganche atencional, restablecimiento de criterio tras interrupción.
**Hipótesis cognitiva:** Task-resumption tras interrupciones PROGRAMADAS: micro-pausas anunciadas (2–3 por sesión, 15–20 s) insertadas entre mini-bloques 2-alt; se mide accuracy en los K=5 trials post-reanudación vs pre-pausa. CLAVE de rediseño: solo las pausas programadas son ventanas de medición; las pausas de SEGURIDAD del usuario JAMÁS se miden ni puntúan (Regla #8) — así el n sube a 6–9 ventanas/sesión y ≥12 en 2 sesiones sin tocar la seguridad.
**Mecánica UX:** Mini-bloques de 2-alt (reusa `EmotionTwoAltField`) separados por pausas anunciadas con cuenta neutra; reanudación con un tap.
**Duración recomendada:** 5–6 min. **Dificultad mínima:** 2. **Dificultad máxima:** 8.
**Variables de dificultad:** Duración de pausa, nº de pausas, discriminabilidad del host.
**Métricas capturadas:** Delta accuracy post-reanudación (gobernante, accuracy-based), `RT_CV` (display-only).
**Scoring específico:** `reentryScore = 1 − max(0, baseAcc − postResumeAcc)/margen`, agregado sobre ≥6 ventanas; null bajo nMin.
**Cómo afecta al score del bloque:** Banda consistencia; post-Ola4 Consistencia (0.30).
**Criterio de progresión:** Delta estable ~0 → pausas más largas o host más difícil.
**Criterio de fallo:** Delta creciente → step-down del host; nunca se culpa la pausa.
**Anti-gaming:** Pausa estratégica para "resetear" → solo cuentan las programadas (las de usuario ni miden ni penalizan); spam → D6 entropía del host.
**Persistencia necesaria:** Ventanas de reanudación en el resumen (agregados, no timestamps crudos).
**Feedback inmediato:** El del host; reanudación neutra.
**Feedback post-sesión:** Banda de estabilidad al retomar; nota explícita de que las pausas de seguridad no se evalúan.
**Feedback histórico:** Tendencia del delta vs línea base.
**Riesgo científico:** Solape con EM13 (misma capacity) → criterio de aceptación exige señal propia (delta post-pausa ≠ coste de interferencia) o se fusiona como preset. **Riesgo de UX:** pausas forzadas percibidas como fricción → anunciarlas como parte del ejercicio. **Riesgo técnico:** orquestación de fases en el holder (aditiva).
**Claim permitido:** "Entrenas retomar la tarea con control tras una pausa y mides tu estabilidad al volver en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Pausas de seguridad excluidas de la medición (test permanente); ≥6 ventanas/sesión garantizadas por diseño; señal incremental demostrada vs EM13 o colapsa a preset; scanners verdes.

### Etiquetado funcional rápido

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM9_ETIQUETADO` + diseño EM-F de 03_7, eje vocabulario funcional) · **Prioridad:** P2 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Granularidad de etiquetado funcional (`GRANULARITY`).
**Subhabilidades:** Discriminación fina entre etiquetas cercanas, precisión léxica funcional, decisión rápida sin perder exactitud.
**Hipótesis cognitiva:** Categorización con vocabulario funcional graduado (paradigma de discriminación semántica de proximidad): elegir entre 4 etiquetas funcionales CERCANAS la más precisa para una frase neutra (p. ej. "inquietud" vs "prisa" vs "duda" vs "cansancio" como descriptores funcionales de una situación de tarea). Distinto de EM3 (registros lógicos): aquí el eje es la PRECISIÓN del descriptor. Banco propio disjunto con inter-rater; jamás caras, jamás lectura de otras personas.
**Mecánica UX:** Reusa `EmotionFourCategoryField` (grid 2x2) con etiquetas variables por trial; ventana moderada.
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** Proximidad semántica de los señuelos, longitud de frase, `TEMPORAL_PRESSURE` leve.
**Métricas capturadas:** `GRANULARITY_INDEX` + accuracy ajustada por azar (gobernantes), `RT_MEDIAN` (display-only; IES NO gobierna sin timebase — desviación deliberada del diseño EM-F).
**Scoring específico:** `labelScore = (acc−0.25)/0.75` sobre ítems con acuerdo inter-rater; null si `!confident`.
**Cómo afecta al score del bloque:** Banda interpretación; post-Ola4 Precisión/Interpretación (0.30).
**Criterio de progresión:** Accuracy en banda con señuelos más próximos.
**Criterio de fallo:** Colapso a una etiqueta → inválido por entropía; accuracy ~azar → baja proximidad.
**Anti-gaming:** Botón-spam → entropía D6 + matriz degenerada; respuesta sin lectura → RT-floor por longitud; memorización → banco 80 + holdout + ExposureTracker.
**Persistencia necesaria:** Resumen estándar + `bankVersion`.
**Feedback inmediato:** Etiqueta correcta resaltada con 1 línea de porqué (neutra).
**Feedback post-sesión:** Banda de precisión de etiquetado + confusiones frecuentes.
**Feedback histórico:** Tendencia vs línea base.
**Riesgo científico:** Arrastra el claim prohibido de "reconocer estados" → claim estrictamente de precisión de etiqueta en la tarea; banco cotidiano-neutro. **Riesgo de UX:** etiquetas ambiguas frustran → solo ítems con acuerdo ≥80%. **Riesgo técnico:** banco nuevo de 80 con inter-rater y scanner (esfuerzo de contenido, no de código).
**Claim permitido:** "Entrenas elegir la etiqueta funcional más precisa entre opciones cercanas y mides tu precisión en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco 80 disjunto de SEPARATOR_ITEMS con inter-rater documentado; adversario spam → inválido; RT-floor testeado; claim y copy escaneados en 3 variantes.

### Espera activa

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM12_ESPERA`, rediseñado: gobernante pasa de ANTICIPATIONS RT-críticas a comisión de foreperiodo, robusta sin timebase) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Retención de respuesta durante la espera (`PAUSE_BEFORE_ACTION`).
**Subhabilidades:** Espera con foreperiodo variable, inhibición de la respuesta preparada.
**Hipótesis cognitiva:** Foreperiodo variable (1–6 s) tipo tarea de preparación: responder al aparecer la señal; cualquier tap DURANTE el foreperiodo (pantalla en fase de espera explícita) es una comisión de espera — un evento de ESTADO (no de latencia), medible honestamente sin timebase frame-accurate. Las `ANTICIPATIONS` por RT (<350 ms) quedan como secundarias display-only (resuelve el bloqueo del audit 04_7).
**Mecánica UX:** Reusa `EmotionGoNoGoField` con fase de espera visible → señal → tap; foreperiodo aleatorio (anti-ritmo).
**Duración recomendada:** 3–4 min. **Dificultad mínima:** 1. **Dificultad máxima:** 8.
**Variables de dificultad:** Rango del foreperiodo y su varianza, densidad de trials, distractor leve durante la espera.
**Métricas capturadas:** Tasa de comisión de foreperiodo (gobernante, state-based), `ANTICIPATIONS`/`RT_MEDIAN` (display-only).
**Scoring específico:** `waitScore = 1 − foreperiodCommissions/trials`, con nMin de trials; null bajo nMin; go-accuracy mínima aplica (no premiar "no responder nunca").
**Cómo afecta al score del bloque:** Banda consistencia; post-Ola4 Precisión (0.30) parcial.
**Criterio de progresión:** Comisión de espera baja estable → foreperiodos más largos/variables.
**Criterio de fallo:** Omisiones masivas → inválido (suelo go-accuracy, mismo gate GA-1).
**Anti-gaming:** Ritmo fijo → varianza de foreperiodo + cadencia D1; "no responder nunca" → suelo go-accuracy invalidante; el evento medido (tap en espera) no es fabricable a favor.
**Persistencia necesaria:** Resumen estándar (nuevo campo comisiones de foreperiodo, bump `schemaVersion`).
**Feedback inmediato:** Temprano/correcto por color+forma, neutro.
**Feedback post-sesión:** Banda de espera + omisiones; sin lecturas de la persona.
**Feedback histórico:** Tendencia vs línea base.
**Riesgo científico:** Solape con EM1 (misma capacity) y con EM6 (espera) → criterio de aceptación exige señal incremental o colapsa a preset de EM1. **Riesgo de UX:** esperas percibidas como vacío → foreperiodo ≤6 s. **Riesgo técnico:** bajo (reusa renderer y clasificador con fase nueva).
**Claim permitido:** "Entrenas esperar la señal correcta sin adelantarte y mides tus respuestas tempranas en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Gobernante state-based (no RT) implementada; suelo go-accuracy compartido con EM1; señal incremental vs EM1/EM6 demostrada o preset; scanners verdes.

### Estabilidad bajo interferencia

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM13_VARIABILIDAD`, rehecho: gobernante pasa de RT_CV a coste de interferencia accuracy-based; fuente diseño EM-G de 03_7) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Control de interferencia afectiva leve (`REGULATION_STABILITY`; nombre UI: "estabilidad de respuesta").
**Subhabilidades:** Selección bajo conflicto congruente/incongruente, mantenimiento de criterio con distractor.
**Hipótesis cognitiva:** Paradigma de conflicto tipo flanker/Simon con distractor afectivo leve: responder a la dimensión objetivo (dirección/forma) ignorando un distractor cuyo tono apunta al mismo lado (congruente) o al contrario (incongruente); el coste de interferencia sobre ACCURACY (incongruente − congruente) es la señal (RT_CV y coste RT quedan display-only por timebase). Cubre "interferencia afectiva leve" pedida por la lente science.
**Mecánica UX:** Reusa `EmotionTwoAltField` + capa de distractor (patrón E5 de Enfoque); daltónico-safe (forma+posición).
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** Ratio incongruente 0.2–0.6, saliencia del distractor, ventana.
**Métricas capturadas:** Coste de interferencia accuracy-based (gobernante, MIN_CELL=5 por celda; ≥20/celda para banda inter-sesión), `ACCURACY`, `RT_CV` (display-only).
**Scoring específico:** `stabilityScore = 1 − clamp(costAcc/COST_MAX, 0, 1)` SOLO con ambas celdas suficientes; `unavailable` si cualquier celda < MIN_CELL (jamás rellenar con 0).
**Cómo afecta al score del bloque:** Banda reactividad; post-Ola4 Dificultad (0.15) + Consistencia parcial.
**Criterio de progresión:** Coste estable/bajo → sube ratio incongruente; plateau → saliencia.
**Criterio de fallo:** Accuracy < banda o variabilidad extrema (proxy) → step-down.
**Anti-gaming:** Responder solo al lado frecuente → balance L/R + entropía D6; el distractor NUNCA predice la respuesta (test de paridad, anti render deshonesto); anticipaciones excluidas.
**Persistencia necesaria:** Resumen con celdas congruente/incongruente agregadas.
**Feedback inmediato:** Neutro estándar.
**Feedback post-sesión:** Banda de estabilidad bajo interferencia con n por celda.
**Feedback histórico:** Tendencia del coste vs línea base.
**Riesgo científico:** El nombre de capacidad promete demasiado → UI "estabilidad de respuesta", jamás términos de manejo de estados (allowlist). **Riesgo de UX:** conflicto sutil difícil de comunicar → cue de regla persistente. **Riesgo técnico:** capa de distractor en renderer (aditiva) + agregación por celda en el assembler.
**Claim permitido:** "Entrenas responder a la señal objetivo ignorando un distractor y mides tu estabilidad de respuesta en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Coste solo con MIN_CELL en ambas celdas (test); paridad distractor↔respuesta; nombre UI de la allowlist; absorbe/fusiona EM7 si este no demuestra señal propia; scanners verdes.

### Calibración de confianza por ítem

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM14_CONFIANZA` + diseño EM-E de 03_7; diferenciado de EM4: retrospectivo por ítem con ECE) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Calibración confianza-acierto por ítem (`CALIBRATION_ACCURACY`).
**Subhabilidades:** Monitoreo post-decisión, discriminación de la propia certeza (resolution).
**Hipótesis cognitiva:** Paradigma clásico de confidence rating post-respuesta: decisión 2-alt → reporte de confianza (3 niveles) → ECE (Expected Calibration Error) con binning por nivel. Diferencia con EM4 (prospectivo por bloque): aquí la calibración es retrospectiva por ítem — si la diferenciación empírica no aparece, EM14 se fusiona en EM4 (duplicado señalado por el audit 04_7).
**Mecánica UX:** Reusa `EmotionTwoAltField` + fila de 3 botones de confianza pre-renderizada (sin pantalla nueva, loop corto); feedback de calibración DIFERIDO por bloque (curva honesta).
**Duración recomendada:** 5–6 min (2 taps/trial, 24–40 trials). **Dificultad mínima:** 2. **Dificultad máxima:** 8.
**Variables de dificultad:** Discriminabilidad mantenida en banda media (~0.75), granularidad de confianza (2–3), ratio de trials ambiguos.
**Métricas capturadas:** `CALIBRATION_ERROR` (ECE, gobernante, gated n_k≥8 por nivel), resolution (var de accuracy entre niveles), `ACCURACY`.
**Scoring específico:** `calibScore = 1 − ECE`, `ECE = Σ(n_k/N)·|conf_k − acc_k|`; null si algún nivel usado tiene n_k < 8; nunca 0.
**Cómo afecta al score del bloque:** Banda interpretación (metacognición); post-Ola4 Consistencia (0.30) parcial.
**Criterio de progresión:** ECE decreciente vs línea base → más trials ambiguos.
**Criterio de fallo:** Confianza colapsada a un nivel → ECE no interpretable → no computa (check-in armado).
**Anti-gaming:** Colapso de confianza → entropía de confianza (detector nuevo con par adversario/honesto); feedback diferido (por trial = gaming por ensayo-error); confianza POST-respuesta (no sesga la decisión).
**Persistencia necesaria:** `RecordedEmotionTrial.reportedConfidence: Int?` + agregación ECE en assembler + resumen (bump `schemaVersion`).
**Feedback inmediato:** Correcto/incorrecto del host; confianza sin feedback inmediato.
**Feedback post-sesión:** Curva de calibración por bloque (diagonal = calibrado) + banda.
**Feedback histórico:** Tendencia de ECE vs línea base.
**Riesgo científico:** ECE con celdas vacías = ruido con precisión falsa → gate n_k innegociable. **Riesgo de UX:** dos taps alargan el loop → botones pre-renderizados. **Riesgo técnico:** campo nuevo en el trial + agregación (aditivo, golden-first).
**Claim permitido:** "Entrenas ajustar tu confianza a tu acierto real y mides tu calibración por respuesta en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** ECE gated por n_k testeado; adversario colapso-de-confianza → no computa; feedback diferido fijado por test; diferenciación empírica vs EM4 o fusión documentada.

### Recuperación encadenada

**Bloque:** Emociones · **Estado:** Nuevo (catálogo in-code `EM15_RECUP_CADENA`, rediseñado como recuperación tras racha con techo de seguridad) · **Prioridad:** P3 · **Apto MVP:** No · **Destino:** Post-MVP
**Habilidad cognitiva principal:** Recuperación tras errores consecutivos (`POST_ERROR_RECOVERY`).
**Subhabilidades:** Continuidad tras racha de fallos, no-escalada del deterioro.
**Hipótesis cognitiva:** Extensión del paradigma post-error a CLUSTERS: se plantan determinísticamente 2 errores probables consecutivos (nunca 3+; techo leve) y se mide la accuracy en los 3 trials posteriores al cluster vs post-error simple — señal propia = recuperación tras racha, distinta del par aislado de EM2. Si la señal incremental no aparece, colapsa como preset ALTA de EM2 (audit 04_7).
**Mecánica UX:** Idéntica a EM2/EM11 (reusa todo); densidad de clusters limitada (máx 3 por bloque).
**Duración recomendada:** 4–5 min. **Dificultad mínima:** 4. **Dificultad máxima:** 9.
**Variables de dificultad:** Nº de clusters, discriminabilidad, ventana.
**Métricas capturadas:** Accuracy post-cluster (gobernante, n≥6 ventanas), `POST_ERROR_RECOVERY`, `POST_ERROR_SLOWING` (display-only).
**Scoring específico:** `chainScore = postClusterAcc` ajustada contra baseAcc; null bajo nMin; abandono jamás penaliza.
**Cómo afecta al score del bloque:** Banda recuperación (vertiente racha); post-Ola4 Control de errores (0.20).
**Criterio de progresión:** Recuperación post-cluster estable → discriminabilidad baja (más difícil).
**Criterio de fallo:** Proxies de sobrecarga (racha de errores real + pausa repetida) → check-in ANTES de decisión adaptativa (orden vivo).
**Anti-gaming:** Igual que EM2 (anticipaciones excluidas de errores genuinos, D3, impulsividad post-error penalizada); clusters plantados por seed no predecibles.
**Persistencia necesaria:** La de EM2 + agregado post-cluster en el resumen.
**Feedback inmediato:** Neutro, transición suave anti-rumiación.
**Feedback post-sesión:** Banda de recuperación tras racha con n; "qué no se puede concluir aún".
**Feedback histórico:** Tendencia vs línea base.
**Riesgo científico:** Densidad de error encadenado roza inducción → techo 2 errores/cluster + OverloadDetector armado (frontera dura). **Riesgo de UX:** frustración acumulada → los mecanismos de contención vivos (check-in, deload). **Riesgo técnico:** bajo (parametrización de EM11).
**Claim permitido:** "Entrenas recuperar tu rendimiento después de varios errores seguidos y mides tu continuidad en esta tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Techo de cluster (≤2) fijado por test; señal incremental vs EM2 demostrada o preset; check-in dispara antes de adaptación (test de orden); abandono ABANDONED_SAFE.

---

## 9. Velocidad mental — 15 Exercises

**Propósito:** velocidad de procesamiento funcional (detección, decisión, verificación, reconfiguración) medida con timebase autoritativo y gates de honestidad; velocidad sin precisión JAMÁS suma. **Política de scoring del bloque:** motor nativo `SpeedScoreEngine` (usefulSpeed/control/stability + techo pc<0.80||cv>0.30 ⇒ ×0.5) intocable; composición PRD §14.2 adaptada (Velocidad 30 · Precisión 25 · Consistencia 20 · Dificultad 10 · Control errores 10 · Progreso 5) en `SpeedScoreAdapter → ScoreResult`; `normalizedScore` 0–1 o `null`, nunca fabricado; PROCESSING_SPEED solo con `DetectorCoverage.ADEQUATE` + timebase autoritativo + verdict limpio.

**Selección MVP (5):**

| Ejercicio | Estado | Justificación (1 línea) |
|---|---|---|
| E1 · Destello | Mejorar | SRT ancla del bloque, ya construido y honesto; sube a 24 trials para nMin=20 + catch-trials |
| E6 · Multivía | Mejorar | CRT 2-AFC con azar real (gate binomial ya operativo); solo necesita KDoc + chosenSide + catch |
| E3 · Cifrado | Mejorar | Única carga de memoria de trabajo del MVP; rotación de clave por bloque le devuelve el constructo |
| E7 · Compuerta | Mantener | Inversión de respuesta prepotente: la mecánica más defendible del bloque (auditoría) |
| VEL-01 · Conmutador | Nuevo | 5º del piso PRD; cubre "cambio de regla" §10.7 reutilizando RuleSwitchField de Enfoque (coste mínimo) |

**Requisitos de banco de contenido:** el bloque es ~todo procedural seeded (`XorShiftSpeedRng` determinista, re-simulable) — sin banco textual salvo VEL-06 (decisión léxica: ≥300 palabras + ≥300 pseudopalabras, `bankVersion`, no repetición hasta agotar pool, `BankValidator` antes de alimentar perfil) y VEL-09 (plantillas aritméticas paramétricas, sin banco fijo). Claves/leyendas de E3/VEL-04 se regeneran por bloque (anti-memorización estructural, sin holdout necesario).

---

### E1 · Destello

**Bloque:** Velocidad mental · **Estado:** Mejorar · **Prioridad:** P0 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** tiempo de reacción simple (velocidad de detección-respuesta).
**Subhabilidades:** alerta fásica, preparación motora, supresión de anticipación.
**Hipótesis cognitiva:** paradigma SRT clásico con foreperiodo variable (familia Donders-A/PVT breve): la latencia mínima de detección con respuesta única aísla el componente sensorio-motor de la velocidad, línea base contra la que se interpretan los RT de elección.
**Mecánica UX:** pantalla completa táctil; foreperiodo aleatorio 800–2200 ms → destello (cambio de fondo) → tap en cualquier punto → feedback RT 650 ms. NUEVO: 2 bloques × 12 trials (24 total) y ~10% catch-trials (foreperiodo sin destello: responder = falso arranque contable, la ventana termina sola).
**Duración recomendada:** 120 s–3 min. **Dificultad mínima:** 1. **Dificultad máxima:** 3 (sin adaptación por diseño: SRT puro, `fixedDifficulty=0.5`).
**Variables de dificultad:** solo rango de foreperiodo (jitter) y densidad de catch; el deadline no aplica (respuesta obligatoria).
**Métricas capturadas:** rtMedian/rtMean/SD (MAD-limpias), anticipationRate, lapseRate (sobre crudos), catchCommissionRate, p10/p90 (nuevo, agregados A11-D11), timebase/quality flags.
**Scoring específico:** `usefulSpeed` desde VCE; `control=(1−impIdx)·pc` con pc=1.0 estructural (single-tap) ⇒ el control real viene de anticipaciones+catch; `stability=(1−cv)·(1−lapseRate)`; total con techo cv>0.30. `chanceAccuracy=1.0` mantiene D3 idle (documentado en `SpeedAntiGaming.evaluate`).
**Cómo afecta al score del bloque:** sesión E1 alimenta la composición del adapter con peso pleno en Velocidad/Consistencia; al perfil solo bajo los 3 gates del bloque (timebase, coverage, verdict).
**Criterio de progresión:** tendencia de rtMedian contra línea base propia (ProfileTrend); no hay niveles.
**Criterio de fallo:** anticipationRate>0.15 o catchCommission>0.20 ⇒ calidad LOW (QualityReason existentes); sesión <20 RT válidos ⇒ sin score (MinSampleGate).
**Anti-gaming:** señal = tap rítmico anticipatorio y spam; neutraliza D1 `RhythmTappingDetector` (cadencia IRI casi constante; el foreperiodo aleatorio protege al honesto), D4 `TooFastToProcessDetector` (anticipación de bloque >30%), catch-trials (falso arranque observable) + filtro de anticipación <100 ms ya existente. `FastGuessingCafDetector` con suelo SRT 140–180 ms (§15) para PARTIAL→ADEQUATE.
**Persistencia necesaria:** `MeasurementRun` (SecureSpeedMeasurementStore, namespace Speed) + `isCatch` en TrialRecord (schemaVersion bump) + SessionRecord ABANDONED en abandono (A12).
**Feedback inmediato:** RT en ms por trial; "Demasiado pronto" en anticipación (neutro).
**Feedback post-sesión:** informe con RT mediana/p10/p90, anticipaciones, estabilidad, calidad de medición; disclaimer preliminar.
**Feedback histórico:** tendencia de RT-mediana vs línea base, "X de 5 sesiones" de calibración.
**Riesgo científico:** un SRT aislado mide sobre todo tiempo motor+arousal; mitigado porque coexiste con 4 tareas de decisión y se rotula como línea base.
**Riesgo de UX:** foreperiodo puede sentirse "lento"; catch-trials pueden frustrar si el copy no explica que no responder ahí es correcto.
**Riesgo técnico:** ninguno nuevo relevante; cero allocations en frame path al añadir catch.
**Claim permitido:** "Entrenas y mides tu tiempo de reacción simple dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** 24 trials/2 bloques verdes en `SpeedSessionStateHolderTest`; catch no cuenta en rtMedian; adversario rítmico sintético dispara D1 (test); p10/p90 en informe; copy-scanner verde.

---

### E6 · Multivía

**Bloque:** Velocidad mental · **Estado:** Mejorar · **Prioridad:** P0 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** velocidad de decisión bajo elección forzada (CRT 2-AFC).
**Subhabilidades:** discriminación perceptiva simple, selección de respuesta, adaptación a presión temporal.
**Hipótesis cognitiva:** choice-RT de 2 alternativas (Donders-B / Hick k=2): añade el estadio de selección de respuesta al SRT; la diferencia E6−E1 aproxima el coste de decisión, y la accuracy objetiva habilita el gate de azar binomial.
**Mecánica UX:** pantalla dividida en 2 mitades táctiles; flecha central (◀/▶) tras foreperiodo; 4 bloques adaptativos × 12 trials; deadline visible en header (1200→450 ms por CCE); respuesta tardía = incorrecta. NUEVO: ~10% catch (marco de retención: no responder), persistir lado elegido.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10 (CCE 0→1 vía `MultiSignalStaircaseEngine`).
**Variables de dificultad:** deadline (gobernado por CCE), ISI jitter; densidad de catch fija.
**Métricas capturadas:** rtMedian HIT MAD-limpia, pc, cv, vce, impIdx, omissionRate, anticipationRate, CCE por bloque (persistida en `BlockRecord`), fatIdx, catchCommissionRate, chosenSide por trial (nuevo).
**Scoring específico:** barras estándar del motor; `chanceAccuracy=0.5` activa D3 y `ChanceLevelGuessingDetector` (accuracy ≤ límite binomial superior ⇒ INVALIDATE).
**Cómo afecta al score del bloque:** contribución plena a la composición; es el ejercicio de referencia para la señal CRT del perfil.
**Criterio de progresión:** RAISE del staircase solo con pc≥banda y sin flags (impulsividad/anticipación/omisión); COOLDOWN por fatIdx termina la corrida mostrando la razón.
**Criterio de fallo:** pc<0.80 ⇒ techo ×0.5 y SpeedScore→0 en el adapter; verdict INVALIDATE ⇒ sesión jugable pero excluida del perfil.
**Anti-gaming:** señal = adivinar rápido a azar (uniforme ~260 ms, alternancia rápido/lento, banda 100–250 ms) y sesgo posicional (siempre un lado); neutraliza `ChanceLevelGuessingDetector` (ya en código), D3, D4, D1, y NUEVOS `FastGuessingCafDetector` (CAF con suelo CRT 250–300 ms, mixture parcial ⇒ WITHHOLD) + D6 `PositionBiasDetector` (habilitado al persistir chosenSide) + catchCommission.
**Persistencia necesaria:** MeasurementRun + `chosenSide`/`isCatch` en TrialRecord (schemaVersion bump); CCE por bloque (ya existe).
**Feedback inmediato:** acierto/error/tardía por trial; deadline visible.
**Feedback post-sesión:** informe con RT mediana, precisión, estabilidad, nivel alcanzado (CCE→1–10), calidad; sin culpa en COOLDOWN (razón explicada).
**Feedback histórico:** tendencia RT/precisión y nivel máximo sostenido vs línea base.
**Riesgo científico:** señal de flecha sin carga semántica discrimina RT de elección "puro" — correcto pero estrecho; el staircase que acorta deadline puede inducir trade-off velocidad-precisión (controlado por gates).
**Riesgo de UX:** presión creciente sin explicación visible del porqué (mitigar: micro-copy "el ritmo se ajusta a ti").
**Riesgo técnico:** desfase doc↔código del KDoc `E6MultiviaScreen.kt` (P2 auditoría) — corregir a "adaptativo multi-señal MVP-2".
**Claim permitido:** "Entrenas y mides tu velocidad de decisión entre dos opciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** KDoc corregido; chosenSide persistido y D6 disparando con sintético sesgado (test); adversarios de azar → INVALIDATE (tests existentes + nuevos CAF); staircase respeta LOWER-prioritario (tests `MultiSignalStaircaseEngineTest` verdes).

---

### E3 · Cifrado

**Bloque:** Velocidad mental · **Estado:** Mejorar · **Prioridad:** P0 · **Apto MVP:** Con cambios

**Habilidad cognitiva principal:** velocidad de verificación símbolo-valor con carga de memoria de trabajo ligera.
**Subhabilidades:** mantenimiento de mapeo arbitrario, comparación, decisión match/no-match.
**Hipótesis cognitiva:** verificación de asociación símbolo↔código (familia DSST-verificación): el RT refleja consulta del mapeo + comparación; la carga es real SOLO si la clave no es trivialmente memorizable ni está siempre visible — de ahí las mejoras.
**Mecánica UX:** fila de clave arriba + prompt "▲ → 3" centrado; LEFT=Coincide / RIGHT=No coincide; 4 bloques adaptativos × 12. CAMBIOS: clave REGENERADA por bloque (seeded), keySize 4→6 progresivo con dificultad, fila de clave OCULTA a dificultad ≥7 (tras 3 s de exposición inicial por bloque); si la ocultación no llega al corte del 25-jul, re-etiquetar como "verificación visual bajo presión" (fallback honesto aprobado por science).
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline (CCE), keySize 4→6, visibilidad de clave (visible→oculta), ISI.
**Métricas capturadas:** estándar 2-AFC + keySize activo por bloque + catchCommissionRate + chosenSide.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`; pendiente RT vs keySize como métrica secundaria descriptiva (nunca gatea; A2 difference-scores).
**Cómo afecta al score del bloque:** contribución plena; etiqueta interna de subseñal "verificación con carga" separada de la CRT pura de E6 (el perfil las agrega, el informe las distingue).
**Criterio de progresión:** igual que E6 (staircase multi-señal) + subida de keySize solo con pc≥0.85 sostenida.
**Criterio de fallo:** pc<0.80 ⇒ techo; omissionRate>0.20 ⇒ LOW.
**Anti-gaming:** señal = memorizar la clave fija (colapsa la carga: hoy real con DEFAULT_KEY estática) y adivinar a azar; neutraliza rotación de clave por bloque (mata la memorización estructuralmente, complementa D5 `MemorizationDetector`), `ChanceLevelGuessingDetector` + `FastGuessingCafDetector` + D1/D3/D4/D6 + catch.
**Persistencia necesaria:** MeasurementRun + keySize/keyVisibility por BlockRecord + chosenSide/isCatch (schemaVersion bump).
**Feedback inmediato:** acierto/error/tardía; al ocultarse la clave, aviso neutro "clave memorizada: ahora sin ayuda".
**Feedback post-sesión:** informe estándar + keySize máximo con precisión.
**Feedback histórico:** tendencia por nivel de carga vs línea base.
**Riesgo científico:** con clave visible y pequeña degenera en discriminación visual (la auditoría lo marcó); la rotación+ocultación restauran el constructo — validar con `mindboost-cognitive-science` antes de reclamar "carga de memoria".
**Riesgo de UX:** fila de clave a 13.sp puede fallar con fuente ampliada (verificar accesibilidad); la ocultación puede frustrar sin el aviso.
**Riesgo técnico:** `CifradoGenerator` debe ganar regeneración por bloque sin romper determinismo seeded (tests de determinismo existentes se actualizan).
**Claim permitido:** "Entrenas y mides tu velocidad verificando un código dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** clave distinta por bloque (test determinista); a dificultad ≥7 la clave no está en pantalla durante trials (test de estado UI); adversario memorizador (clave fija simulada) detectado o estructuralmente imposible; pendiente RT/keySize reportada como secundaria.

---

### E7 · Compuerta

**Bloque:** Velocidad mental · **Estado:** Mantener · **Prioridad:** P0 · **Apto MVP:** Sí

**Habilidad cognitiva principal:** velocidad bajo regla condicional con inversión de respuesta prepotente.
**Subhabilidades:** control inhibitorio, aplicación de regla, flexibilidad ligera.
**Hipótesis cognitiva:** mapeo compatible/incompatible condicional (familia reversed-mapping/Simon-inversión): con puerta cerrada el usuario debe inhibir la respuesta espacialmente compatible con la flecha y emitir la opuesta; el coste de inversión (RT incompatible − compatible) es la firma del control a velocidad.
**Mecánica UX:** cue "◀ · puerta abierta/cerrada"; abierta ⇒ lado de la flecha, cerrada ⇒ opuesto; 4 bloques adaptativos × 12; mitades táctiles. Cambios transversales solamente (catch + chosenSide del runtime común).
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline (CCE), proporción puerta-cerrada (0.5 fija en MVP; variable Post-MVP), ISI.
**Métricas capturadas:** estándar 2-AFC + coste de inversión (RT cerrada−abierta, HITs ambos lados, ≥20/celda) como secundaria + catch/chosenSide.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`; coste de inversión `null` no penaliza jamás.
**Cómo afecta al score del bloque:** contribución plena; subseñal "velocidad con control" del informe.
**Criterio de progresión:** staircase estándar; LOWER prioritario si errores en puerta-cerrada suben (impulsividad direccional).
**Criterio de fallo:** pc<0.80 ⇒ techo; verdict sucio ⇒ excluida del perfil.
**Anti-gaming:** señal = ignorar la puerta y responder siempre a la flecha (rinde ~50% en cerrada ⇒ cae bajo la banda binomial) o adivinar rápido; neutraliza `ChanceLevelGuessingDetector` (la estrategia degenerada queda a azar en la mitad de trials), `FastGuessingCafDetector`, D1/D3/D4/D6 + catch.
**Persistencia necesaria:** MeasurementRun + gateOpen por trial (derivable del prompt; persistir flag para el coste por celda) + chosenSide/isCatch.
**Feedback inmediato:** acierto/error/tardía.
**Feedback post-sesión:** informe estándar + coste de inversión si computable (con nota "orientativo").
**Feedback histórico:** tendencia estándar + coste de inversión inter-sesión cuando haya celdas suficientes.
**Riesgo científico:** bajo (auditoría: mecánica más defendible); el cue textual añade lectura — vigilar que el coste no sea de lectura (cue icónico Post-MVP).
**Riesgo de UX:** doble información en una línea (flecha+puerta): carga de lectura ligeramente mayor, aceptable.
**Riesgo técnico:** ninguno nuevo; la lógica vive en `CompuertaGenerator` (puro, testeado).
**Claim permitido:** "Entrenas y mides tu velocidad aplicando una regla que a veces se invierte, dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** los 12 tests de generador/staircase siguen verdes; estrategia "siempre flecha" sintética ⇒ INVALIDATE por azar (test); coste de inversión null bajo n<20/celda (test).

---

### VEL-01 · Conmutador (Cambio de Regla Veloz)

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P0 · **Apto MVP:** Sí (5º MVP)

**Habilidad cognitiva principal:** velocidad de reconfiguración de regla (throughput bajo cambio).
**Subhabilidades:** mantenimiento de set, cambio de set, resistencia a interferencia dimensional.
**Hipótesis cognitiva:** task-switching con cue explícito (paradigma cued switching de Rogers & Monsell, sin ítems con copyright): un estímulo bidimensional (color rojo/azul × forma círculo/cuadrado) se juzga por la dimensión que dicta el cue; alternar regla impone un coste de reconfiguración; bajo deadline mide throughput-bajo-cambio. Fuente: diseño 03_2 #3 (Conmutación Perceptiva) CORREGIDO según su auditoría: la señal primaria es RT-mediana global + pc bajo alternancia (medible en una sesión), y SWITCH_COST pasa a secundaria (memo A2: difference-scores solo secundarias, ≥20/celda, null honesto).
**Mecánica UX:** cue persistente arriba (icono+texto "COLOR"/"FORMA"), figura central, mitades táctiles LEFT/RIGHT (RULE_COLOR: rojo→izq, azul→der; RULE_SHAPE: círculo→izq, cuadrado→der); reutiliza el patrón `FocusRuleSwitchField` de Enfoque E6 portado al runtime Speed (payload transporta las features, la respuesta se DERIVA, nunca se muestra — regla 2026-06-18); 4 bloques × 16; switchRate 0.3–0.5 seeded balanceado.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline (CCE), switchRate 0.2→0.5, CSI cue→estímulo 600→150 ms, proporción de estímulos incongruentes (la otra dimensión empuja al lado opuesto) 0→0.5.
**Métricas capturadas:** rtMedian global, pc, cv, vce, impIdx, SWITCH_COST (HITs ambas celdas, ≥20/celda, null si no), efecto de congruencia (separado, secundario), catch/chosenSide, fatIdx.
**Scoring específico:** barras estándar del motor con `chanceAccuracy=0.5`; SWITCH_COST y congruencia son descriptivas (jamás penalizan si null; jamás gatean).
**Cómo afecta al score del bloque:** contribución plena; cubre la subhabilidad "cambio de regla" del PRD §10.7 que E7 solo roza.
**Criterio de progresión:** staircase estándar + subida de switchRate solo con pc≥0.85 sostenida.
**Criterio de fallo:** pc<0.80 ⇒ techo; responder por una dimensión fija (ver anti-gaming) ⇒ INVALIDATE.
**Anti-gaming:** señal = ignorar el cue y responder siempre por una dimensión (con incongruencia 0.5, esa estrategia cae a azar en incongruentes ⇒ banda binomial la caza), adivinar rápido, tap rítmico; neutraliza `ChanceLevelGuessingDetector`, `FastGuessingCafDetector` (suelo CRT), D1/D3/D4/D6, catch-trials de retención.
**Persistencia necesaria:** MeasurementRun + isSwitch/ruleId/congruent por trial + chosenSide/isCatch (mismo bump de esquema); registry: añadir `VEL01_CONMUTADOR` a `SpeedExerciseId` + generador en `SpeedPromptGenerators` + card en `SpeedRootScreen` (test de consistencia catálogo↔menú).
**Feedback inmediato:** acierto/error/tardía; el cue parpadea suave SOLO en trial switch (señal funcional, no decorativa).
**Feedback post-sesión:** informe estándar + "coste de cambio" si computable con nota orientativa.
**Feedback histórico:** tendencia estándar; coste de cambio inter-sesión al acumular celdas.
**Riesgo científico:** con CSI largo mide lectura de cue, no reconfiguración (capar CSI); switch-cost casi siempre null en sesión única (aceptado: es secundaria).
**Riesgo de UX:** doble regla exige onboarding claro (pantalla de práctica de 6 trials sin registro antes del bloque 1).
**Riesgo técnico:** portar RuleSwitchField sin acoplar módulos (copiar patrón, no depender de focus); derivar correctChoice de (rule×dimensión) en generador puro seeded.
**Claim permitido:** "Entrenas y mides tu velocidad cambiando de regla dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** generador determinista con balance switch/repeat seeded (test); correctChoice derivado nunca expuesto (test de payload); estrategia dimensión-fija ⇒ INVALIDATE (adversario sintético); SWITCH_COST null bajo n<20/celda; jugable end-to-end desde el hub.

---

### VEL-02 · Rastreo de Símbolos

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P1 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de búsqueda visual (presente/ausente).
**Subhabilidades:** escaneo serial, matching perceptivo, decisión terminación-de-búsqueda.
**Hipótesis cognitiva:** symbol search estilo Salthouse (símbolos propios, sin ítems WAIS): 2 símbolos objetivo + fila de 5 candidatos; decidir si ALGÚN objetivo aparece. El RT crece con set-size; la pendiente RT×set es la firma de búsqueda (secundaria); la señal primaria es rtMedian+pc a set fijo por bloque.
**Mecánica UX:** fila objetivo arriba, fila de búsqueda centrada; LEFT=Presente / RIGHT=Ausente; 4 bloques × 14; renderer 2-AFC existente + slot de filas de símbolos (componente pequeño).
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline, set-size 3→7, similitud objetivo-distractor, presencia 50% fija.
**Métricas capturadas:** estándar 2-AFC + set-size por bloque + pendiente RT×set (secundaria).
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena cuando se construya; subseñal "búsqueda".
**Criterio de progresión:** staircase + set-size sube con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo; sesgo "siempre presente" cae a azar ⇒ banda binomial.
**Anti-gaming:** señal = responder "presente" siempre (50% estructural ⇒ `ChanceLevelGuessingDetector`), fast-guessing (`FastGuessingCafDetector`), rítmico (D1), posicional (D6).
**Persistencia necesaria:** MeasurementRun + setSize por bloque; símbolos proceduralmente muestreados (sin banco).
**Feedback inmediato:** acierto/error/tardía. **Feedback post-sesión:** informe estándar + set máximo con precisión. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** mide búsqueda, no velocidad pura — declarado como subseñal distinta (ortogonalidad con E6 documentada en el informe).
**Riesgo de UX:** símbolos pequeños con fuente ampliada (verificar accesibilidad).
**Riesgo técnico:** slot de filas nuevo; sin renderer nuevo de paradigma.
**Claim permitido:** "Entrenas y mides tu velocidad de búsqueda visual dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** generador determinista con presencia 50%; sintético "siempre presente" ⇒ INVALIDATE; pendiente RT×set secundaria null-safe.

---

### VEL-03 · Gemelos

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P1 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de comparación perceptiva (mismo/distinto).
**Subhabilidades:** codificación paralela, detección de discrepancia, decisión same/different.
**Hipótesis cognitiva:** perceptual comparison de Salthouse (letter/pattern comparison, con cadenas de símbolos propios): dos cadenas lado a lado, ¿idénticas?; la longitud de cadena parametriza la carga; es uno de los marcadores conductuales de processing speed más replicados — aquí solo intra-producto contra línea base propia.
**Mecánica UX:** dos cadenas de 3–9 símbolos en dos tarjetas; LEFT=Iguales / RIGHT=Distintas (diferencia siempre de exactamente 1 símbolo cuando existe, posición aleatoria); 4 bloques × 14; renderer 2-AFC + slot de tarjetas.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline, longitud 3→9, similitud del símbolo cambiado, igualdad 50% fija.
**Métricas capturadas:** estándar + longitud por bloque + pendiente RT×longitud (secundaria).
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena; subseñal "comparación".
**Criterio de progresión:** staircase + longitud sube con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo.
**Anti-gaming:** "siempre iguales" ⇒ azar ⇒ `ChanceLevelGuessingDetector`; fast-guessing ⇒ CAF; D1/D4/D6; catch de retención.
**Persistencia necesaria:** MeasurementRun + longitud por bloque; generación procedural.
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe + longitud máxima con precisión. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** bajo (paradigma muy establecido); solapa con VEL-08 — diferenciar por estímulo (cadenas lineales vs rejillas) y retirar uno si la correlación intra-usuario es alta.
**Riesgo de UX:** longitudes altas fuerzan sacadas; layout responsive con fuente ampliada.
**Riesgo técnico:** trivial sobre el runtime existente.
**Claim permitido:** "Entrenas y mides tu velocidad comparando patrones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** diferencia exactamente-1 garantizada por generador (property test); igualdad 50% seeded; sintéticos de sesgo ⇒ INVALIDATE.

---

### VEL-04 · Cifrado Paramétrico (DSST propio)

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P1 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** throughput perceptivo-asociativo bajo mapeo arbitrario.
**Subhabilidades:** consulta de leyenda, codificación símbolo→respuesta, ritmo sostenido.
**Hipótesis cognitiva:** sustitución símbolo-dígito paramétrica (familia DSST con símbolos PROPIOS, jamás ítems publicados): responder qué lado asigna la leyenda al símbolo central; leyenda visible que ROTA por bloque; el throughput (correctos/min) es la señal. Fuente: 03_2 #2 con las correcciones de su auditoría: pendiente RT/keySize elevada a métrica de control (separa búsqueda de velocidad), keySize capado a 6 en runtime, `legendVisibleMs` para forzar codificación.
**Mecánica UX:** leyenda compacta arriba (2–6 pares símbolo↔lado), símbolo grande central, mitades táctiles; ISI corto 250–500 ms; 5 bloques × 16.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** keySize 2→6, deadline, legendVisibleMs ∞→2 s, similitud entre símbolos.
**Métricas capturadas:** throughput/min, rtMedian, pc, cv, pendiente RT×keySize (control de constructo), estándar.
**Scoring específico:** throughput escalado modula `usefulSpeed`; barras estándar; techo estándar.
**Cómo afecta al score del bloque:** sustituye a E3 como señal de throughput en V1.1 SI la pendiente RT/keySize demuestra que no mide búsqueda; hasta entonces coexisten y solo uno alimenta el perfil (anti-doble-conteo).
**Criterio de progresión:** keySize sube con pc≥0.85; staircase estándar.
**Criterio de fallo:** pc<0.80 ⇒ techo; pendiente RT/keySize fuera de rango esperado ⇒ señal marcada "búsqueda dominante" (no puntúa como velocidad).
**Anti-gaming:** memorizar leyenda (rotación por bloque + `legendShufflePerTrial` ante sospecha, D5), respuesta-por-posición (D6, símbolos barajados), azar (`ChanceLevelGuessingDetector`), fast-guessing (CAF), rítmico (D1).
**Persistencia necesaria:** MeasurementRun + keySize/legend snapshot por bloque (schemaVersion).
**Feedback inmediato:** estándar + highlight funcional del par acertado en la leyenda.
**Feedback post-sesión:** informe + throughput y carga máxima. **Feedback histórico:** tendencia de throughput vs línea base.
**Riesgo científico:** a keySize alto con leyenda visible el RT lo dominan las sacadas (auditoría 03_2/E2) — el cap + pendiente lo controlan.
**Riesgo de UX:** leyenda densa en pantallas pequeñas.
**Riesgo técnico:** slot de leyenda nuevo; regeneración determinista por bloque.
**Claim permitido:** "Entrenas y mides tu ritmo de codificación con una clave dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** leyenda rotada por bloque (test); keySize≤6 enforced; pendiente RT/keySize computada y reportada; adversario memorizador neutralizado estructuralmente.

---

### VEL-05 · Abanico de Hick

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** escalado de RT con incertidumbre de elección (ley de Hick-Hyman).
**Subhabilidades:** decisión multi-alternativa, mapeo estímulo-posición, preparación dividida.
**Hipótesis cognitiva:** CRT con 2/4/8 alternativas por bloques: el RT crece ~lineal con log2(n); la PENDIENTE individual (bits/ms) contra la propia línea base es la señal distintiva (science: "la pendiente es la señal") — describe eficiencia de decisión más allá del RT bruto.
**Mecánica UX:** 2/4/8 zonas táctiles dispuestas en rejilla (reutiliza el `GridField` compartido de A11); un destello ilumina una zona ⇒ tocarla; 3 bloques por set-size × 14; deadline holgado (la presión viene de n).
**Duración recomendada:** 5–7 min. **Dificultad mínima:** 2. **Dificultad máxima:** 8.
**Variables de dificultad:** nº alternativas 2→8, deadline, ISI jitter.
**Métricas capturadas:** rtMedian y pc por set-size, pendiente Hick (regresión RT×log2 n; secundaria hasta ≥20 HITs/celda), estándar.
**Scoring específico:** barras estándar sobre el agregado; la pendiente es descriptiva (A2), null-safe.
**Cómo afecta al score del bloque:** contribución plena; la pendiente enriquece el informe, no el score.
**Criterio de progresión:** por set-size con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo; celdas incompletas ⇒ pendiente null (no penaliza).
**Anti-gaming:** azar cae a 1/n (banda binomial paramétrica por chanceAccuracy=1/n — `ChanceLevelGuessingDetector` ya es genérico en chance); fast-guessing (CAF con suelo por n); D1/D4; motor: tocar el centro siempre ⇒ D6-espacial (entropía de zona, extensión menor del detector).
**Persistencia necesaria:** MeasurementRun + setSize por bloque + zona tocada.
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe + RT por nº de opciones. **Feedback histórico:** pendiente inter-sesión cuando computable.
**Riesgo científico:** bajo; requiere n alto por celda para la pendiente (aceptado como secundaria).
**Riesgo de UX:** 8 zonas en pantallas pequeñas (targets ≥48 dp obligatorios).
**Riesgo técnico:** primer uso de GridField en Speed; captura de zona con timebase táctil intacta.
**Claim permitido:** "Entrenas y mides cómo escala tu velocidad al elegir entre más opciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** chanceAccuracy=1/n propagada al gate binomial (test); pendiente null-safe; targets táctiles verificados.

---

### VEL-06 · Palabra Real

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de acceso léxico (decisión léxica).
**Subhabilidades:** reconocimiento ortográfico, discriminación palabra/pseudopalabra.
**Hipótesis cognitiva:** lexical decision task clásica: decidir si una cadena es palabra real del español o pseudopalabra fonotácticamente legal; el RT indexa acceso léxico rápido. Cubre "rapidez verbal" (PRD §10.5) desde Velocidad con timebase autoritativo.
**Mecánica UX:** cadena central; LEFT=Palabra / RIGHT=No; 4 bloques × 16; deadline adaptativo.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 2. **Dificultad máxima:** 9.
**Variables de dificultad:** frecuencia léxica de las palabras (alta→baja), similitud de pseudopalabras (legalidad/vecindad), deadline.
**Métricas capturadas:** estándar 2-AFC + frecuencia media del bloque; d′ opcional descriptivo.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena; FRONTERA declarada: alimenta SOLO PROCESSING_SPEED (nunca VERBAL); si Lenguaje construye su decisión léxica propia (su panel la lista), UNO de los dos se retira (anti-doble-conteo cross-bloque, coordinación obligatoria).
**Criterio de progresión:** baja frecuencia + pseudopalabras más legales con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo.
**Anti-gaming:** sesgo "siempre palabra" (50% estructural ⇒ banda binomial), fast-guessing (CAF), memorizar banco (D7 `RepeatedBankExploitDetector` + no-repetición hasta agotar pool + `bankVersion`), D1/D6.
**Persistencia necesaria:** MeasurementRun + itemId/bankVersion por trial; banco ≥300 palabras + ≥300 pseudopalabras con `BankValidator` y registro en `ProfileUpdatingBanks` ANTES de alimentar perfil (regla INFRA-04).
**Feedback inmediato:** estándar (sin mostrar la palabra correcta en error, evita aprendizaje del banco).
**Feedback post-sesión:** informe estándar. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** dependencia de idioma/vocabulario individual (declarar covariable); sesgo cultural en selección de palabras (curación del banco).
**Riesgo de UX:** lectura con fuente ampliada; longitudes controladas 4–8 letras.
**Riesgo técnico:** primer ejercicio de banco del bloque: integrar `core/bank/ItemBank` + ExposureTracker.
**Claim permitido:** "Entrenas y mides tu rapidez reconociendo palabras dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** banco validado por `BankValidator` (tamaño, balance, no-trivialidad) y versionado; no-repetición inter-sesión testeada; frontera con Lenguaje documentada en el plan maestro.

---

### VEL-07 · Recuento Relámpago

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de enumeración (subitización/conteo rápido).
**Subhabilidades:** aprehensión numérica inmediata, conteo bajo presión, verificación.
**Hipótesis cognitiva:** enumeración rápida: N puntos (1–9) se muestran 200–500 ms y se enmascaran; verificar un candidato ("¿eran 6?"). El quiebro de la función RT×N en ~4 (subitizing→conteo) es una firma clásica; aquí, señal primaria = rtMedian+pc de verificación; el quiebro es descriptivo.
**Mecánica UX:** nube de puntos breve + máscara + candidato numérico; LEFT=Sí / RIGHT=No (mantiene renderer 2-AFC y evita teclado); 4 bloques × 14.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 9.
**Variables de dificultad:** N 1–4 (subitizing) → 5–9 (conteo), duración de exposición 500→200 ms, distancia del candidato (±1 vs ±3), densidad/agrupación de puntos.
**Métricas capturadas:** estándar + N por trial + exposición; pendiente RT×N descriptiva.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena; subseñal "enumeración".
**Criterio de progresión:** N y exposición se endurecen con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo.
**Anti-gaming:** sesgo sí/no (banda binomial), fast-guessing (CAF), rítmico (D1); la exposición breve + máscara impiden contar fuera de ventana (estructural).
**Persistencia necesaria:** MeasurementRun + N/exposición por trial; layout de puntos seeded (`layoutSeed=stimulus.index`, patrón 2026-06-18).
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe + precisión por rango de N. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** la exposición nominal depende del frame real — usar `FrameTimingProbe` para marcar CONTAMINATED si el jank distorsiona la ventana.
**Riesgo de UX:** máscara agresiva puede sentirse injusta; onboarding con 4 trials de práctica.
**Riesgo técnico:** presentación breve requiere onset por Choreographer (ya disponible) y duración medida en frames reales.
**Claim permitido:** "Entrenas y mides tu rapidez estimando cantidades de un vistazo dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** exposición efectiva registrada en frames reales; CONTAMINATED bajo jank (test con probe fake); generador determinista de nubes.

---

### VEL-08 · Patrones Espejo

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de matching de patrones espaciales.
**Subhabilidades:** codificación de configuración, comparación estructural, decisión same/different.
**Hipótesis cognitiva:** comparación de matrices (pattern matching paramétrico): dos mini-rejillas 3×3 con 3–7 celdas activas; ¿idénticas? (cuando difieren, exactamente 1 celda). Extiende VEL-03 a configuraciones 2D con carga espacial graduable — se mantiene solo si demuestra señal no redundante con VEL-03.
**Mecánica UX:** dos rejillas lado a lado; LEFT=Iguales / RIGHT=Distintas; 4 bloques × 14; renderer 2-AFC + slot de rejillas (reusa GridField como display estático).
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** celdas activas 3→7, deadline, distancia visual de la celda cambiada, igualdad 50%.
**Métricas capturadas:** estándar + carga (celdas) por bloque + pendiente RT×carga descriptiva.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena; subseñal "comparación espacial".
**Criterio de progresión:** carga sube con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo.
**Anti-gaming:** sesgo de respuesta (banda binomial), fast-guessing (CAF), D1/D6; generación procedural sin banco memorizable.
**Persistencia necesaria:** MeasurementRun + carga por bloque; rejillas seeded.
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe + carga máxima. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** redundancia con VEL-03 (medir correlación intra-usuario en beta y retirar uno si >0.8 — regla anti-redundancia).
**Riesgo de UX:** rejillas pequeñas en teléfonos compactos.
**Riesgo técnico:** trivial sobre GridField.
**Claim permitido:** "Entrenas y mides tu rapidez comparando configuraciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** diferencia exactamente-1 (property test); plan de evaluación de redundancia vs VEL-03 documentado.

---

### VEL-09 · Cálculo Contrarreloj

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P2 · **Apto MVP:** No (Post-MVP)

**Habilidad cognitiva principal:** velocidad de verificación aritmética simple.
**Subhabilidades:** recuperación de hechos aritméticos, verificación, resistencia a señuelos plausibles.
**Hipótesis cognitiva:** verificación aritmética con tiempo (paradigma de arithmetic verification): "7+6=13" ¿correcto?; los errores señuelo son plausibles (±1, ±10, tabla vecina); el RT indexa acceso a hechos numéricos automatizados — velocidad de procesamiento con contenido simbólico, sin exigir cálculo complejo (no es Lógica).
**Mecánica UX:** expresión central; LEFT=Correcto / RIGHT=Incorrecto; 4 bloques × 16; operaciones 1 dígito → 2 dígitos sin llevada → con llevada.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 1. **Dificultad máxima:** 9.
**Variables de dificultad:** tamaño de operandos, tipo de operación (+,−,×), plausibilidad del señuelo, deadline.
**Métricas capturadas:** estándar 2-AFC + nivel aritmético por bloque.
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`.
**Cómo afecta al score del bloque:** contribución plena; subseñal "verificación simbólica".
**Criterio de progresión:** nivel sube con pc≥0.85.
**Criterio de fallo:** pc<0.80 ⇒ techo.
**Anti-gaming:** sesgo correcto/incorrecto (banda binomial, 50% seeded), fast-guessing (CAF), D1; generación paramétrica infinita (sin banco memorizable, señuelos rotados).
**Persistencia necesaria:** MeasurementRun + nivel por bloque; plantillas paramétricas versionadas.
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe estándar. **Feedback histórico:** tendencia estándar.
**Riesgo científico:** confundido con destreza aritmética educacional (declarar covariable; nunca claims de "matemáticas").
**Riesgo de UX:** ansiedad numérica en parte de usuarios (copy neutro, dificultad mínima amable).
**Riesgo técnico:** trivial.
**Claim permitido:** "Entrenas y mides tu rapidez verificando operaciones sencillas dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** señuelos plausibles balanceados (property test de distancia); 50% correcto/incorrecto seeded; sin léxico prohibido en copy.

---

### VEL-10 · Freno Exprés

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP / avanzado)

**Habilidad cognitiva principal:** velocidad de go con freno selectivo (go/no-go veloz asimétrico).
**Subhabilidades:** respuesta prepotente, inhibición selectiva, control de comisión.
**Hipótesis cognitiva:** go/no-go con go frecuente (~80%) y no-go raro bajo ISI corto: mide el equilibrio velocidad/control (curva velocidad-comisión), no solo RT. Fuente: 03_2 #4 con sus correcciones de auditoría (techo por faRate>0.30, gate de comisión LOWER prioritario testeado con sintético impulsivo, faRate null bajo MIN_N de no-go). CONDICIÓN de entrada al catálogo activo: demostrar ortogonalidad con Enfoque E3 (go/no-go) — aquí la presión es deadline+ISI corto y la señal primaria es GO-RT; si la correlación cross-bloque es alta, se queda en modo libre sin alimentar perfil.
**Mecánica UX:** símbolo central verde (GO, tap) / rojo+forma distinta (NO-GO, retener — doble canal daltonismo-safe); renderer single-tap de E1; 5 bloques × 24.
**Duración recomendada:** 4–6 min. **Dificultad mínima:** 3. **Dificultad máxima:** 10.
**Variables de dificultad:** deadline GO, noGoRate 0.30→0.15, similitud go/no-go (lures), ISI 600→300 ms.
**Métricas capturadas:** GO-rtMedian, pc, faRate (comisión), omissionRate, post-error slowing (descriptivo), estándar.
**Scoring específico:** `control=(1−faRate)·pc`; techo ampliado: pc<0.80||cv>0.30||faRate>0.30 ⇒ ×0.5; macro FAST_IMPULSIVE si VCE alta+faRate alta; faRate null bajo n de no-go insuficiente.
**Cómo afecta al score del bloque:** contribución solo si supera la condición de ortogonalidad; subseñal "velocidad con freno".
**Criterio de progresión:** RAISE jamás si comisión alta (LOWER prioritario — test obligatorio con sintético impulsivo).
**Criterio de fallo:** faRate>0.30 ⇒ techo; comisión+omisión subiendo juntas ⇒ COOLDOWN.
**Anti-gaming:** tap-a-todo (comisión observable — el no-go ES el catch estructural), fast-guessing (CAF sobre GO-RT), rítmico (D1); premiar velocidad sin penalizar comisión PROHIBIDO (test).
**Persistencia necesaria:** MeasurementRun + isGo/isLure por trial.
**Feedback inmediato:** neutro en comisión (sin culpa). **Feedback post-sesión:** informe con comisiones separadas de omisiones. **Feedback histórico:** curva velocidad-control inter-sesión.
**Riesgo científico:** redundancia cross-bloque con Enfoque E3/E4 (condición explícita arriba).
**Riesgo de UX:** frustración por no-go raros fallados; copy neutro.
**Riesgo técnico:** bajo (runtime single-tap existente).
**Claim permitido:** "Entrenas y mides tu velocidad de respuesta manteniendo el freno dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** sintético impulsivo ⇒ el staircase BAJA (test); techo por faRate (test); estudio de ortogonalidad vs Enfoque E3 planificado antes de alimentar perfil.

---

### VEL-11 · Umbral de Luminancia

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP / avanzado)

**Habilidad cognitiva principal:** velocidad de decisión perceptiva pura (discriminación de luminancia 2-AFC).
**Subhabilidades:** codificación sensorial temprana, decisión con evidencia degradada.
**Hipótesis cognitiva:** discriminación claro/oscuro con separación paramétrica (fuente: 03_2 #1 Tinte Crítico, corregido por su auditoría: eje LUMINANCIA por defecto — daltonismo-safe — en vez de tono; rotación de eje ≥30% de trials contra el aprendizaje de mapeo; anclaje ESTRICTO al baseline del propio dispositivo, jamás cross-device): velocidad de transducción estímulo→categoría con evidencia nítida→degradada.
**Mecánica UX:** parche central; LEFT=Más oscuro / RIGHT=Más claro; 4 bloques × 20; delta de luminancia adaptativo.
**Duración recomendada:** 3–4 min. **Dificultad mínima:** 2. **Dificultad máxima:** 10.
**Variables de dificultad:** delta de luminancia, deadline, tamaño del parche, ruido.
**Métricas capturadas:** estándar + delta alcanzado (umbral 79% descriptivo).
**Scoring específico:** barras estándar; `chanceAccuracy=0.5`; umbral descriptivo, no puntuable cross-device.
**Cómo afecta al score del bloque:** contribución solo device-anchored (misma pantalla); subseñal "perceptiva pura".
**Criterio de progresión:** staircase 1-up/3-down sobre delta (objetivo ~0.79).
**Criterio de fallo:** pc<0.80 a delta máximo ⇒ LOW (posible problema de pantalla/brillo — copy neutro sugiere revisar brillo).
**Anti-gaming:** azar (banda binomial), fast-guessing (CAF), rítmico (D1), mapeo fijo (rotación de eje).
**Persistencia necesaria:** MeasurementRun + delta por trial + deviceProfile (anclaje).
**Feedback inmediato:** estándar. **Feedback post-sesión:** informe + delta mínimo discriminado (orientativo, "en este dispositivo"). **Feedback histórico:** tendencia device-anchored.
**Riesgo científico:** brillo/pantalla/entorno contaminan (por eso P3 y device-anchored estricto).
**Riesgo de UX:** puede percibirse "difícil de ver" — es la tarea; expectativas claras.
**Riesgo técnico:** control de brillo no disponible: registrar covariables imposible ⇒ aceptar ruido y marcar preliminar siempre.
**Claim permitido:** "Entrenas y mides tu velocidad distinguiendo diferencias visuales sutiles dentro de MindBoost, en este dispositivo, contra tu propia línea base."
**Criterios de aceptación:** rotación de eje ≥30% (test); anclaje device-local probado; sintético azar ⇒ INVALIDATE.

---

### VEL-12 · Cadencia

**Bloque:** Velocidad mental · **Estado:** Nuevo · **Prioridad:** P3 · **Apto MVP:** No (Post-MVP / avanzado)

**Habilidad cognitiva principal:** precisión temporal predictiva (coincidence timing), no RT reactivo.
**Subhabilidades:** extrapolación de trayectoria, sincronización motora, control de impulsividad temporal.
**Hipótesis cognitiva:** tiempo de coincidencia (fuente: 03_2 #5): un objeto viaja hacia una meta con tramo final OCLUIDO (oclusión obligatoria: fuerza extrapolación, no reacción al borde); tocar EN el instante de coincidencia; el error con signo (adelanto/retraso) y su variabilidad son la señal. Nota de infraestructura: el helper puro `signedRt` YA existe en `core/timing/TimingPolicy.kt` (INFRA-02, 2026-06-28) — el bloqueo histórico ("computeRtNanos lanza con negativos") está parcialmente resuelto; queda el cómputo de error con signo en métricas y el renderer dinámico.
**Mecánica UX:** pista con meta, objeto animado (animación en composable keyed por `layoutSeed`, patrón MOT/RSVP 2026-06-18; una pulsación por trial); barra de error con signo <150 ms; 5 bloques × 16.
**Duración recomendada:** 3–5 min. **Dificultad mínima:** 3. **Dificultad máxima:** 10.
**Variables de dificultad:** velocidad del objeto, ventana de tolerancia ±200→±60 ms, fracción de oclusión 0.2→0.6, jitter de velocidad.
**Métricas capturadas:** timingError mediano con signo, |error| mediano, tol-hit rate, variabilidad temporal, sesgo direccional.
**Scoring específico:** `stability=1−clamp(var/ref)`, `control=1−clamp(|sesgoAdelanto|/ref)`, `usefulSpeed=tol-hit ponderado por estrechez`; techo tol-hit<0.80; siempre preliminar y device-anchored (latencia táctil = sesgo constante).
**Cómo afecta al score del bloque:** subseñal "timing predictivo" única; solo Android autoritativo + jank-gate; CONTAMINATED en fallback.
**Criterio de progresión:** tolerancia se estrecha con tol-hit≥0.80.
**Criterio de fallo:** sesgo de adelanto creciente ⇒ LOWER (impulsividad temporal), jamás RAISE.
**Anti-gaming:** "tocar pronto siempre" penalizado por el error con signo (estructural); tap-spam ⇒ D1/anticipación; distractores castigan tap-a-movimiento.
**Persistencia necesaria:** MeasurementRun + coincidenceUptime/tapUptime/signedError por trial (campos nuevos, schemaVersion).
**Feedback inmediato:** barra de error con signo y magnitud. **Feedback post-sesión:** informe con sesgo y variabilidad. **Feedback histórico:** tendencia de variabilidad device-anchored.
**Riesgo científico:** latencia táctil del hardware domina si no se ancla al baseline del mismo dispositivo.
**Riesgo de UX:** exigente; frustración con tolerancias estrechas.
**Riesgo técnico:** renderer dinámico nuevo + métricas de error con signo (deuda real declarada); `cos/sin`→`.toFloat()` en Canvas.
**Claim permitido:** "Entrenas y mides tu precisión anticipando un instante dentro de MindBoost, en este dispositivo, contra tu propia línea base."
**Criterios de aceptación:** signedError negativo sin lanzar (property test sobre `signedRt`); oclusión obligatoria activa; CONTAMINATED fuera de Android autoritativo (test); adversario "toco pronto" ⇒ score bajo.

---

## 10. Memoria — 15 Exercises

**Propósito:** memoria de trabajo, episódica y asociativa medidas contra la línea base personal, con la única señal de retención a 24 h real del producto. **Política de scoring del bloque:** accuracy-based puro (Velocidad peso 0.00 por timebase no autoritativo); pesos sesión Precisión 0.40 / Dificultad 0.30 / Consistencia 0.15 / Control de errores 0.10 / Progreso 0.05; gates processing≥0.85 → discriminación FAR → criterio extremo → `MinSampleGate` n≥20 → anti-gaming; `normalizedScore` 0–1 o null, jamás fabricado; blend usuario WM 0.7 / EPISÓDICA 0.3 (D5).

**Selección MVP (5):**

| Ejercicio | Estado | Justificación (1 línea) |
|---|---|---|
| EX1_SPAN_COMPLEJO | Mejorar | Ancla de WM ya jugable con el gate de validez más sólido del proyecto; solo documentar/interleaving. |
| EX2_ACTUALIZACION_SET | Nuevo | Paradigma distinto (updating) con taxonomía in-code completa; reusa StreamField (A11); el 4º más barato. |
| EX3_LISTA_ESTRATEGIA | Mejorar | Única medida episódica a 24 h del producto; el P1 es el recordatorio activo (A13), no la mecánica. |
| EX4_PARES_ASOCIADOS | Mantener | Jugable, gates probados adversarialmente, cero cambios urgentes — coste marginal nulo. |
| EX6_DETECCION_CAMBIO | Nuevo | Cubre el canal visoespacial con practice-effect LOW y captura binaria sobre GridField; corte designado del suelo A16. |

**Requisitos de banco de contenido:** `WordPool` español ≥200 palabras balanceadas por categoría/frecuencia/longitud (hoy ~38 líneas autoradas — ampliar antes de subir dificultad alta); `PairPool` ≥300 pares con fuerza asociativa etiquetada; set de ≥24 objetos visuales de baja verbalizabilidad para EX6 (formas abstractas/tonos vecinos, generación paramétrica); política de no-repetición hasta agotar pool vía `RecentItemRegistry` (existente) + `bankVersion` persistido en el blob; regla dura heredada: si no se pueden muestrear TODOS los señuelos requeridos, el ítem se salta, nunca se presenta degradado; holdout `TransferProbePartition` reservado (se sirve en V1.1 con la sonda).

### Span Complejo (EX1_SPAN_COMPLEJO)

**Bloque:** Memoria · **Estado:** Mejorar · **Prioridad:** P2 · **Apto MVP:** Con cambios (menores)
**Habilidad cognitiva principal:** Memoria de trabajo bajo carga concurrente (`WORKING_MEMORY`).
**Subhabilidades:** Mantenimiento serial, control ejecutivo, resistencia a intrusiones.
**Hipótesis cognitiva:** Paradigma complex/operation span (Turner & Engle; Conway et al.): mantener memoranda mientras se resuelve una tarea de procesamiento carga el ejecutivo central, no el buffer fonológico simple; el gate de processing ≥0.85 es lo que impide que degenere en simple span.
**Mecánica UX:** Intro → Study (palabras) → Processing (juicios aritméticos V/F) → Recall multi-select (memoranda + 6 distractores) → Finished. Mejora V1: documentar en producto que el processing va en bloque (desviación V1 ya anotada en código); V1.1: intercalarlo entre memoranda (fidelidad dual-task).
**Duración recomendada:** 5–8 min (≥8 trials válidos/bloque).
**Dificultad mínima:** 1 = span 2, 1 processing/ítem, categorías distintas, recall directo.
**Dificultad máxima:** 10 = span 8, 2 processing/ítem, misma categoría (confusable), recall inverso.
**Variables de dificultad:** `spanLength` 2–8 (LOAD), `processingPerItem` 1–2 (CONCURRENCY), `sameCategory` (SIMILARITY), `backwardRecall` (RETRIEVAL) — `Ex1DifficultyMapping` existente.
**Métricas capturadas:** span serial-correct, processingAccuracy, serialOrderAccuracy, intrusionRate, validTrials; hits/omissions/intrusions por trial.
**Scoring específico:** `ComplexSpanScorer.scoreBlock` (existente): processing <0.85 → INVALIDATED; <8 válidos → CALIBRATING; span = media serial-correct sobre válidos; `sessionSpan` ponderado por trials; `MemorySpanScore` → `ScoreResult` con normalizedScore = span/8 o null.
**Cómo afecta al score del bloque:** Señal principal de WORKING_MEMORY (0.7 del blend de usuario); alimenta Precisión y Dificultad (span alcanzado).
**Criterio de progresión:** `DefaultMemoryAdaptiveEngine` sube LOAD con zona OPTIMAL sostenida (histéresis); desvía a INTERFERENCE/SIMILARITY si intrusiones altas.
**Criterio de fallo:** Bloque INVALIDATED (processing <0.85) no puntúa ni castiga; FRUSTRATION → step-down asimétrico inmediato.
**Anti-gaming:** Señal: spam/azar en processing para blindar memoranda. Neutraliza: `MemorySpanAntiGaming` (`PositionBiasDetector` INFRA-03, red-team verde) + gate 0.85 del scorer → `DetectorCoverage.ADEQUATE` legítimo (ya en código).
**Persistencia necesaria:** `LocalSpanResultStore` (namespace `mindboost_memory`, cifrado) — existente; + `SessionRecord` write-ahead (A12) al integrarse.
**Feedback inmediato:** Accuracy del recall + validez del processing + `HONEST_NOTE` existente.
**Feedback post-sesión:** Informe: span de sesión, % trials válidos, intrusiones, estado de calibración.
**Feedback histórico:** Tendencia de span en `MemoryProgressScreen` (hoy solo conteo — ampliar); "X de 5 sesiones".
**Riesgo científico:** Processing en bloque reduce fidelidad dual-task (documentado); confound aritmético (V1.1: rotar a juicio de plausibilidad).
**Riesgo de UX:** El usuario no anticipa la fase aritmética; estudio autoguiado reduce presión.
**Riesgo técnico:** El interleaving V1.1 toca `runSession` + sealed UiState (regla: variante y renderer en el mismo cambio).
**Claim permitido:** "Entrenas y mides tu memoria de trabajo bajo carga dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Desviación V1 documentada en catálogo/PRD; adversario "ignora processing" → INVALIDATED (test existente sigue verde); n≥20 → VALID vía aggregator end-to-end; abandono → ABANDONED sin tocar perfil.

### Actualización — Running Memory (EX2_ACTUALIZACION_SET)

**Bloque:** Memoria · **Estado:** Nuevo (taxonomía in-code completa, sin generador; fuente: catálogo `MemoryExerciseCatalog.EX2` + diseño "Updating de Ranura" de 03_5_Memoria) · **Prioridad:** P1 · **Apto MVP:** Con cambios (construcción completa)
**Habilidad cognitiva principal:** Actualización de memoria de trabajo (`WORKING_MEMORY`, paradigma RUNNING_MEMORY).
**Subhabilidades:** Inhibición de lo obsoleto, binding ranura↔valor, resistencia a interferencia.
**Hipótesis cognitiva:** Memory updating / keep-track (Yntema; Morris & Jones): mantener el valor MÁS RECIENTE de k ranuras ante un flujo de actualizaciones exige descartar activamente lo viejo — componente de updating de Miyake, disociable del mantenimiento del span (EX1).
**Mecánica UX:** k ranuras etiquetadas (A,B,C…) con valor inicial → stream de updates "ranura X ← valor" sobre **StreamField compartido** (A11) → al final, sonda de UNA ranura aleatoria: multi-select del valor vigente entre candidatos mezclados. Runtime de una respuesta por trial (sin cambios al esqueleto).
**Duración recomendada:** 5–8 min (bloques de ≥8 probes).
**Dificultad mínima:** 1 = k=2, stream corto (2k), valores muy distintos, ritmo 2500 ms.
**Dificultad máxima:** 10 = k=6, stream 4k, valores confusables, 1200 ms, 3 fillers.
**Variables de dificultad:** `slotCount` 2–6 (LOAD), `updateStreamLen` k–4k, `valueSimilarity` (SIMILARITY), `updateRateMs` 2500→1200, `interferenceFillers` 0–3 (INTERFERENCE), `probeOne`/`probeAll` (RETRIEVAL).
**Métricas capturadas:** correctProbes, `INTRUSION_RATE` (respondió el valor viejo), `BINDING_ERROR_RATE` (valor correcto, ranura equivocada), respuestas a fillers, omisiones, k por bloque.
**Scoring específico:** NUEVO `Ex2UpdatingScorer` (patrón `ComplexSpanScorer`): `updatingScore = 1 − (intrusionRate + 0.5·bindingErrorRate)` sobre probes válidas; `kEffective` = máximo k con accuracy ≥0.75; bloque con respuesta-a-filler → baja calidad (INVALIDATED); <8 probes → CALIBRATING; normalizedScore = updatingScore·(kEffective/6) o null.
**Cómo afecta al score del bloque:** Contribuye a WORKING_MEMORY (mismo SignalKey de familia WM, blend documentado con EX1/EX4); intrusiones alimentan Control de errores.
**Criterio de progresión:** Sube k (LOAD) con zona OPTIMAL; si intrusiones altas sube INTERFERENCE (fillers) en vez de k; si binding errors altos sube SIMILARITY.
**Criterio de fallo:** accuracy <0.55 dos bloques → step-down; bloque con fillers respondidos no puntúa.
**Anti-gaming:** Señales: "responder al último visto sin tracking", conteo de updates, preparar solo algunas ranuras. Neutraliza: fillers a ranuras inexistentes (responderlos = automatismo → `FillerResponseGate` NUEVO, gate del scorer), nº de updates variable por ranura, sonda de UNA ranura aleatoria, `PositionBiasDetector` sobre la secuencia de respuestas. `DetectorCoverage.ADEQUATE` SOLO tras suite red-team (adversario "último visto" dispara, honesto no); hasta entonces NONE → CALIBRATING.
**Persistencia necesaria:** NUEVO `UpdatingResultStore` sobre `SecureKeyValueStore` (namespace `mindboost_memory`) + entrada en eraser AccountBound + test en el MISMO cambio; `SessionRecord` A12 desde el día 1.
**Feedback inmediato:** Por probe: correcto / valor antiguo / ranura equivocada (feedback de TIPO de error, no solo ✓/✗).
**Feedback post-sesión:** Informe: k alcanzado, tasa de intrusión vs binding (las dos curvas separadas), validez.
**Feedback histórico:** k sostenido inter-sesión + tendencia de intrusiones.
**Riesgo científico:** Solapamiento perceptual con n-back si la UX no comunica "estado final" (mitigado por diseño de sonda y copy del catálogo).
**Riesgo de UX:** Carga de output con probeAll → probeOne por defecto; stream rápido en fontScale grande.
**Riesgo técnico:** Depende de la extracción de StreamField (A11, donante Enfoque); patrón "flujo de captura vivo": verificar que toda probe sea contestable (incidente EX4 previo).
**Claim permitido:** "Entrenas y mides tu capacidad de actualizar información y descartar lo obsoleto dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Generador determinista por seed; intrusion/binding mutuamente exclusivos por probe (property test); adversario "último visto" → intrusionRate alto/INVALIDATED y honesto limpio; end-to-end vía aggregator; test consistencia catálogo↔registry actualizado; copy-scanner verde.

### Lista con Estrategia + Recuerdo Diferido 24 h (EX3_LISTA_ESTRATEGIA)

**Bloque:** Memoria · **Estado:** Mejorar · **Prioridad:** P1 · **Apto MVP:** Con cambios (recordatorio activo)
**Habilidad cognitiva principal:** Memoria episódica con consolidación (`EPISODIC_MEMORY`, dimensión propia).
**Subhabilidades:** Codificación estratégica, retención a 24 h, discriminación de señuelos.
**Hipótesis cognitiva:** Free recall + reconocimiento diferido (Ebbinghaus/curva de olvido; testing effect de Roediger & Karpicke): la retención medida tras ~24 h SIN re-estudio separa consolidación de familiaridad inmediata; la métrica gobernante `DELAYED_RECALL_ACCURACY` es la señal episódica honesta.
**Mecánica UX:** Sesión inmediata: Study (lista) → Recall multi-select con señuelos → Finished + se agenda el ítem diferido. Repaso: tarjeta "Repaso pendiente" en `MemoryHomeScreen` (ventana 20–48 h) → `DeferredRecallScreen`. MEJORA P1: recordatorio activo — notificación local en la ventana válida + badge; deep link a `DeferredReview`.
**Duración recomendada:** Inmediata 4–6 min; repaso diferido 2–3 min.
**Dificultad mínima:** 1 = 8 palabras, estudio holgado, recall inmediato sin demora.
**Dificultad máxima:** 10 = 20 palabras, estudio breve, demora intra-sesión 5 min, recall cued.
**Variables de dificultad:** `listLength` 8–20 (LOAD), tiempo de estudio inverso (DEPTH), `delay` 0–5 min (DELAY), free/cued (RETRIEVAL) — `Ex3DifficultyMapping` existente. El componente 24 h NO adapta (es prueba de retención — correcto por diseño).
**Métricas capturadas:** Inmediata: hits/omissions/intrusions. Diferida: `DeferredRecallOutcome` (studiedCount, hits, distractorCount, falseAlarms) + **intervalo real 20–48 h como covariable** (nuevo) + `source=notification|organic`.
**Scoring específico:** Inmediata: `MemoryScorer.scoreList`. Diferida: `DeferredRecallScorer` → `RecognitionDiscriminationScorer` (gate FAR; "seleccionar todo" no da 100% — verificado); `MemoryDeferredScore` → ScoreResult ADEQUATE; ítem sin señuelos completos → se salta (NothingDue), nunca test degradado.
**Cómo afecta al score del bloque:** Única fuente de `EPISODIC_MEMORY` (0.3 del blend de usuario); solo alimenta perfil si la ventana 20–48 h se confirma (fuera → EXPIRED, no puntúa).
**Criterio de progresión:** Motor adaptativo sobre la sesión inmediata; el diferido progresa solo en volumen (n outcomes).
**Criterio de fallo:** Ítem EXPIRED = pérdida de dato (no castigo); FAR sobre techo → outcome descartado.
**Anti-gaming:** Señal: sobre-selección en reconocimiento; re-estudio encubierto. Neutraliza: gate de discriminación FAR (adversarial test existente), señuelos completos o skip, demora anclada a `EpochClock` real (no saltable), ítem consumido al puntuarse.
**Persistencia necesaria:** `LocalDeferredRecallRepository` + `LocalDeferredResultStore` (existentes) + NUEVO registro de programación del recordatorio (id opaco) en el mismo namespace.
**Feedback inmediato:** "Recordados X/Y" + nota honesta existente ("mide cuánto conservaste de la sesión anterior, no lo de hoy").
**Feedback post-sesión:** Comparativa inmediato vs diferido (índice de consolidación) cuando ambos existen.
**Feedback histórico:** Curva de retención a 24 h por sesión; % repasos completados a tiempo.
**Riesgo científico:** La sesión inmediata es reconocimiento (constructo más fácil que recall libre) — documentado; volumen episódico depende del retorno del usuario.
**Riesgo de UX:** Notificación percibida como spam → opt-in contextual al registrar el PRIMER ítem diferido, quiet hours, cancelación al consumir/expirar.
**Riesgo técnico:** WorkManager + Doze/OEM: ventana inexacta 20–48 h absorbe el retraso (A13); iOS stub documentado (badge pasivo); suite JVM del scheduler + camino permiso-denegado.
**Claim permitido:** "Entrenas y mides cuánto retienes a 24 horas de lo que estudiaste dentro de MindBoost, contra tu propia línea base."
**Criterios de aceptación:** A13 literal (contenido genérico sin lexemas de memoria/recuerdo/palabras, visibility PRIVATE, payload id opaco, permiso contextual vía `NotificationPermissionGate`); notificación disparada dentro de [20h,48h] y cancelada al consumir/expirar; `source=notification` en analytics; covariable de intervalo persistida; degradación honesta (badge + copy) si permiso denegado; tests del scheduler en desktopTest.

### Pares Asociados (EX4_PARES_ASOCIADOS)

**Bloque:** Memoria · **Estado:** Mantener · **Prioridad:** P3 · **Apto MVP:** Sí
**Habilidad cognitiva principal:** Memoria asociativa (`WORKING_MEMORY` blend documentado; SignalKey asociativo propio).
**Subhabilidades:** Binding relacional cue↔target, resistencia a confusión, recall bidireccional.
**Hipótesis cognitiva:** Paired associates (Calkins; literatura de cued recall): aprender vínculos arbitrarios y recuperar uno dado el otro mide binding relacional, no familiaridad — el gate de discriminación garantiza que no sea adivinación por eliminación.
**Mecánica UX:** Intro → Study (pares cue→target) → Recall multi-select (targets + 6 señuelos) → Finished. Sin cambios MVP.
**Duración recomendada:** 5–8 min.
**Dificultad mínima:** 1 = 4 pares de asociación fuerte, unidireccional.
**Dificultad máxima:** 10 = 10 pares de asociación débil, bidireccional, misma categoría.
**Variables de dificultad:** `pairCount` 4–10 (LOAD), DEPTH, DELAY, bidireccional (RETRIEVAL), asociación débil (SIMILARITY) — `Ex4DifficultyMapping`.
**Métricas capturadas:** targetCount (solo probes CONTESTABLES — fix del silent-halving testeado), hits, distractorCount, falseAlarms; `ASSOCIATIVE_INDEX`, `CONJUNCTION_ERROR_RATE`.
**Scoring específico:** `AssociativePairsScorer` → `RecognitionDiscriminationScorer`: trial cuenta solo si discrimina (FAR ≤ 0.34); bloque con <85% discriminantes → INVALIDATED; `MemoryPairsScore` → ScoreResult ADEQUATE.
**Cómo afecta al score del bloque:** Contribuye a WORKING_MEMORY (coarse blend deliberado y documentado con EX1); precisión + control de errores.
**Criterio de progresión:** Motor adaptativo compartido; sube LOAD, desvía a SIMILARITY si conjunción alta.
**Criterio de fallo:** Bloque INVALIDATED por sobre-selección; FRUSTRATION → step-down.
**Anti-gaming:** Señal: "seleccionar todo" (FAR=1) y probes no contestables inflando denominador. Neutraliza: gate de discriminación (adversario sintético testeado) + filtro answerable probes (test explícito del bug histórico 2026-06-28).
**Persistencia necesaria:** `LocalPairsResultStore` (existente, cifrado).
**Feedback inmediato:** Aciertos/pares + validez de discriminación.
**Feedback post-sesión:** Índice asociativo + errores de conjunción.
**Feedback histórico:** Tendencia del índice asociativo (misma pantalla de progreso).
**Riesgo científico:** Practice effect ALTO si el pool no rota → `RecentItemRegistry` + ampliar `PairPool` ≥300.
**Riesgo de UX:** Ninguno relevante (patrón visual probado).
**Riesgo técnico:** Ninguno urgente; V1.1 fase A-C reutiliza `Ex4PairsGenerator` con overlap (ver EX11).
**Claim permitido:** "Entrenas y mides tu memoria de asociaciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Tests existentes verdes (discriminación, bidireccionalidad, answerable); pool ampliado con `bankVersion`; sin regresión al integrar SessionRecord.

### Detección de Cambio — Grid Visoespacial (EX6_DETECCION_CAMBIO)

**Bloque:** Memoria · **Estado:** Nuevo (taxonomía in-code EX6; fuente: catálogo + diseño "Grid Espacial" 03_5 en su modo change-detection, merge recomendado por 04_5) · **Prioridad:** P1 · **Apto MVP:** Con cambios (construcción; **corte designado del suelo A16 si falta holgura**)
**Habilidad cognitiva principal:** Memoria de trabajo visoespacial (capacidad K; contribuye a WORKING_MEMORY como blend documentado; `SignalKey("VISUOSPATIAL_K")` propio para abrir subdimensión en V1.1 con nMin — A18).
**Subhabilidades:** Codificación visual, retención breve con máscara, discriminación de cambio.
**Hipótesis cognitiva:** Change detection (Luck & Vogel): recordar un array breve de objetos-posición y juzgar si la sonda cambió estima la capacidad K de WM visual; K = c·(hit−FA) corrige el sesgo de respuesta, y el paradigma tiene el menor practice-effect del catálogo (LOW).
**Mecánica UX:** GridField compartido (A11): estudio del array (c objetos abstractos en celdas, 500–900 ms) → máscara/demora → array de test (una celda puede haber cambiado) → juicio binario "Igual / Cambió" (dos zonas táctiles, patrón ChoiceResponseLayer). Una respuesta por trial — encaja en el runtime existente.
**Duración recomendada:** 4–6 min (≥40 trials: ≥20 change + ≥20 no-change por sesión de calibración).
**Dificultad mínima:** 1 = c=2, objetos muy distintos, demora 500 ms.
**Dificultad máxima:** 10 = c=7, objetos de similitud alta (tonos vecinos), demora 4000 ms con máscara.
**Variables de dificultad:** `itemCount` c 2–7 (LOAD), `objectSimilarity` (SIMILARITY), `delayMs` 500–4000 (DELAY), `studyMsPerItem` 900→500; grid 3×3→4×4.
**Métricas capturadas:** hits, falseAlarms, misses, correctRejections por bloque; c del bloque; `COWAN_K`, `RECOGNITION_DPRIME`, `RESPONSE_CRITERION`.
**Scoring específico:** NUEVO `Ex6ChangeDetectionScorer`: K = c·(hitRate−faRate) y d′ con **corrección log-lineal** (evita z(0)/z(1)); bloque con criterio extremo (|c|>1.5 o siempre-sí/siempre-no) → INVALIDATED; <20+20 probes → CALIBRATING; normalizedScore = K/c o null. Golden tests numéricos de K/d′ obligatorios (matemática nueva en el engine).
**Cómo afecta al score del bloque:** Contribuye a WORKING_MEMORY como blend documentado (cap de peso mientras no exista subdimensión); Dificultad = K alcanzado.
**Criterio de progresión:** Sube c con K estable; desvía a SIMILARITY antes que a c cuando FAR alta (evita inflar carga sin discriminación).
**Criterio de fallo:** Criterio extremo o accuracy≈azar → bloque baja calidad, no puntúa.
**Anti-gaming:** Señales: "siempre cambió"/"nunca cambió" (sesgo de criterio) y verbalización del layout. Neutraliza: K/d′ (nunca hit-rate crudo) + `ExtremeCriterionGate` NUEVO (descarta criterio extremo; par adversario/honesto obligatorio) + objetos de baja verbalizabilidad y alta similitud. ADEQUATE solo con la suite red-team; antes NONE → CALIBRATING.
**Persistencia necesaria:** NUEVO `GridResultStore` (`mindboost_memory`) + eraser AccountBound + test en el MISMO cambio; `SessionRecord` A12 desde el día 1.
**Feedback inmediato:** ✓/✗ sobrio por trial (sin revelar qué celda cambió durante el bloque).
**Feedback post-sesión:** K de sesión + sesgo de respuesta explicado en lenguaje llano ("tiendes a responder 'cambió'").
**Feedback histórico:** K sostenido inter-sesión; se abre barra visoespacial solo con nMin (A18).
**Riesgo científico:** Recoding verbal residual (mitigado por set de objetos no nombrables); K inestable con c bajo.
**Riesgo de UX:** Demoras con máscara pueden sentirse lentas → sesiones cortas; contraste/accesibilidad del set visual.
**Riesgo técnico:** GridField compartido es NUEVO (A11, compartido con Lógica L4) — dependencia cruzada de calendario; d′/K nuevos en engine sin goldens previos.
**Claim permitido:** "Entrenas y mides tu memoria visual inmediata dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Golden numérico de K/d′ (incluida corrección log-lineal en bordes 0/1); adversario "siempre cambió" → INVALIDATED, honesto limpio; balance change/no-change 50% testeado; end-to-end vía aggregator con SignalKey propio; consistency test actualizado; si se corta por A16, el catálogo lo marca Post-MVP sin dejar tarjeta muerta en el hub.

### Grid Espacial con Binding (EX5_GRID_ESPACIAL)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX5 + diseño 03_5 "Grid Espacial con Binding") · **Prioridad:** P2 · **Apto MVP:** No (V1.1)
**Habilidad cognitiva principal:** WM visoespacial con binding objeto↔posición (subdimensión visual-espacial cuando exista).
**Subhabilidades:** Localización, binding feature-location, detección de swaps.
**Hipótesis cognitiva:** What-where binding (Treisman; literatura de binding en WM visual): reconstruir QUÉ objeto estaba en CADA celda separa binding de mera localización — complementa el modo change-detection de EX6 (comparten engine/banco; construir solo si demuestra varianza única, correlación <0.7 en sintéticos — veredicto MERGE de 04_5).
**Mecánica UX:** GridField: estudio → máscara → reconstrucción (tap celda + selección de objeto; sin drag en V1 por riesgo táctil).
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = 2 objetos, grid 3×3. · **Dificultad máxima:** 10 = 7 objetos, 5×5, similitud alta, demora 8 s.
**Variables de dificultad:** c 2–7 (LOAD), gridSize 3×3–5×5, objectSimilarity, delayMs 0–8000, grid distractor (INTERFERENCE).
**Métricas capturadas:** locationAcc, bindingAcc, swapRate, K.
**Scoring específico:** `visuospatialScore = locationAcc·bindingAcc`; errores location/binding/swap mutuamente exclusivos (property test); null si trials < umbral.
**Cómo afecta al score del bloque:** Segunda fuente visoespacial; solo suma si aporta varianza única vs EX6.
**Criterio de progresión:** LOAD; SIMILARITY si binding errors dominan. · **Criterio de fallo:** random placement (accuracy≈azar) → baja calidad.
**Anti-gaming:** Señal: verbalización y colocación aleatoria. Neutraliza: objetos no nombrables, detección de azar por accuracy≈chance, reconstrucción completa (no "¿cambió?").
**Persistencia necesaria:** Reusa `GridResultStore` de EX6.
**Feedback inmediato:** Celdas correctas/swaps. · **Feedback post-sesión:** Location vs binding por separado. · **Feedback histórico:** Tendencia binding.
**Riesgo científico:** Redundancia con EX6 (gate de varianza única). · **Riesgo de UX:** Interacción de dos pasos más lenta. · **Riesgo técnico:** Bajo (hereda EX6).
**Claim permitido:** "Entrenas y mides el vínculo objeto-posición de tu memoria visual dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Correlación sintética <0.7 vs EX6 antes de exponer; property tests de exclusividad de errores; goldens compartidos con EX6.

### Secuencias — Orden Serial (EX7_SECUENCIAS)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX7 + diseño 03_5 "Orden Serial Temporal") · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Memoria del orden serial (disociada del item memory).
**Subhabilidades:** Orden temporal, reproducción inversa, resistencia a transposiciones.
**Hipótesis cognitiva:** Serial order (Corsi-like / efecto de gradiente posicional): reproducir posiciones temporales exactas mide orden, disociable de saber QUÉ ítems aparecieron; el scoring por posición es lo que lo salva de ser "otro span".
**Mecánica UX:** Secuencia de elementos resaltados uno a uno (GridField o banco) → reproducir tocando en orden (forward/backward).
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = 3 elementos, forward, ritmo 1000 ms. · **Dificultad máxima:** 10 = 9 elementos, backward, 500 ms, confusables.
**Variables de dificultad:** sequenceLength 3–9 (LOAD), forward/backward (RETRIEVAL), itemSimilarity, presentationRateMs 1000→500, delayMs 0–6000.
**Métricas capturadas:** positionsCorrect, itemAcc, transposiciones adyacentes, gradiente posicional.
**Scoring específico:** `serialOrderAcc = positionsCorrect/s`; `orderSpecificScore = serialOrderAcc − itemAcc·chanceOrderGivenItems` (disociación); gradiente plano (azar) → baja calidad; null si <6 trials.
**Cómo afecta al score del bloque:** WM blend (componente secuencial); Dificultad = longitud sostenida.
**Criterio de progresión:** LOAD; backward solo con forward estable. · **Criterio de fallo:** Gradiente plano.
**Anti-gaming:** Señal: "los toqué todos" y respuesta azarosa. Neutraliza: scoring estricto por posición + gate de gradiente posicional (nuevo, especificado en 04_5).
**Persistencia necesaria:** Store propio sobre `mindboost_memory` + eraser.
**Feedback inmediato:** Posiciones correctas. · **Feedback post-sesión:** Orden vs ítems disociados. · **Feedback histórico:** Longitud sostenida.
**Riesgo científico:** Colapso en redundancia con span si la disociación falla. · **Riesgo de UX:** Tap secuencial exige precisión táctil. · **Riesgo técnico:** Captura tap-en-orden nueva (patrón L3 de Lógica reutilizable).
**Claim permitido:** "Entrenas y mides tu memoria del orden dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Disociación item/orden demostrada en sintéticos; gradiente como gate testeado.

### Instrucciones Complejas (EX9_INSTRUCCIONES)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX9) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** WM ejecutiva aplicada a secuencias de acción (following instructions).
**Subhabilidades:** Retención de pasos, ejecución ordenada, resistencia a demora.
**Hipótesis cognitiva:** Paradigma de instruction following (Gathercole): retener y ejecutar series de instrucciones carga WM verbal+acción; el orden de ejecución es la señal.
**Mecánica UX:** Set de instrucciones simples sobre objetos en pantalla ("toca el círculo, luego arrastra…" — solo taps en V1) → ejecutarlas en orden, a veces tras demora.
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = 2 pasos. · **Dificultad máxima:** 10 = 6 pasos + demora + distractor.
**Variables de dificultad:** nº pasos (LOAD), delay, concurrencia, similitud de objetos.
**Métricas capturadas:** pasos correctos en orden, omisiones, `SERIAL_ORDER_ACCURACY`.
**Scoring específico:** Por posición (patrón EX7); null si trials < umbral.
**Cómo afecta al score del bloque:** WM blend menor. · **Criterio de progresión:** LOAD. · **Criterio de fallo:** Omisiones en racha.
**Anti-gaming:** Señal: ejecutar todo lo visible sin orden. Neutraliza: scoring por posición + objetos señuelo no instruidos (tocar señuelo = false alarm).
**Persistencia necesaria:** Store propio + eraser. · **Feedback inmediato:** Pasos OK. · **Feedback post-sesión:** Orden vs omisión. · **Feedback histórico:** Pasos sostenidos.
**Riesgo científico:** Solapa con EX7 (orden). · **Riesgo de UX:** Instrucciones largas de leer. · **Riesgo técnico:** Renderer de escena nuevo (por eso P3).
**Claim permitido:** "Entrenas y mides cuántos pasos retienes y ejecutas en orden dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Señuelos no instruidos como FA testeados; disociación con EX7 documentada.

### Prospectiva Embebida (EX10_PROSPECTIVA)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX10) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Memoria prospectiva (recordar ejecutar una intención futura).
**Subhabilidades:** Detección de señal prospectiva bajo tarea en curso, coste dual.
**Hipótesis cognitiva:** Event-based prospective memory (Einstein & McDaniel): mantener la intención "cuando aparezca X, haz Y" mientras se ejecuta otra tarea mide la memoria de intenciones, con `PROSPECTIVE_SUCCESS_RATE` y coste sobre la tarea base.
**Mecánica UX:** Tarea base (p. ej. mini-clasificación tap) + regla prospectiva rara (~10% de trials); reusa campo tap existente.
**Duración recomendada:** 6–10 min (la señal necesita eventos raros suficientes). · **Dificultad mínima:** 1 = señal saliente. · **Dificultad máxima:** 10 = señal no saliente + demora + carga base alta.
**Variables de dificultad:** saliencia de la señal, tasa de eventos, carga de la tarea base (CONCURRENCY), demora.
**Métricas capturadas:** `PROSPECTIVE_SUCCESS_RATE`, `DUAL_TASK_COST`, `FALSE_ALARM_RATE`.
**Scoring específico:** successRate corregido por FA; nMin de eventos prospectivos ≥12 antes de puntuar (eventos raros = muestras lentas); null si menos.
**Cómo afecta al score del bloque:** WM/prospectiva blend menor. · **Criterio de progresión:** Saliencia baja. · **Criterio de fallo:** FA altas (responder prospectivo a todo).
**Anti-gaming:** Señal: responder la acción prospectiva indiscriminadamente. Neutraliza: corrección por FA + gate de discriminación (patrón compartido).
**Persistencia necesaria:** Store propio + eraser. · **Feedback inmediato:** Sobrio (no revelar próximas señales). · **Feedback post-sesión:** Éxito prospectivo + coste dual. · **Feedback histórico:** Tasa sostenida.
**Riesgo científico:** Pocos eventos por sesión → calibración lenta. · **Riesgo de UX:** Frustración por señales raras. · **Riesgo técnico:** Bajo (runtime de una respuesta).
**Claim permitido:** "Entrenas y mides tu memoria de intenciones dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** nMin de eventos gateado; adversario "todo prospectivo" → FA gate; honesto limpio.

### Listas con Interferencia A-B/A-C (EX11_INTERFERENCIA)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX11 + núcleo A-B/A-C del diseño premium de EX4 en 03_5) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Resistencia a interferencia proactiva/retroactiva.
**Subhabilidades:** Supresión de asociaciones obsoletas, discriminación de lista fuente.
**Hipótesis cognitiva:** Paradigma A-B/A-C (Underwood; interferencia asociativa): re-emparejar el MISMO cue con un target nuevo mide si el aprendizaje viejo intruye — la intrusión A-B sobre A-C es la señal, no el recall bruto.
**Mecánica UX:** Estudiar lista A-B → estudiar A-C (mismos cues) → cued recall de la lista pedida (multi-select con señuelos que INCLUYEN los targets B).
**Duración recomendada:** 6–10 min. · **Dificultad mínima:** 1 = overlap 25%. · **Dificultad máxima:** 10 = overlap 100% + similitud alta + demora.
**Variables de dificultad:** overlap A-B/A-C 0–100% (INTERFERENCE), pairCount, similitud, delay.
**Métricas capturadas:** `INTERFERENCE_INDEX` (recall con overlap / sin overlap), intrusiones A-B, conjunciones.
**Scoring específico:** proactiveInterference = abIntrusions/(abIntrusions+correctC); reusa `RecognitionDiscriminationScorer` para el gate; null si pares < umbral.
**Cómo afecta al score del bloque:** Control de errores + WM blend; abre la subhabilidad interferencia SOLO con señal propia (A18).
**Criterio de progresión:** Overlap. · **Criterio de fallo:** Accuracy≈azar.
**Anti-gaming:** Señal: sobre-selección. Neutraliza: señuelos = targets B (la intrusión es medible, no adivinable) + gate FAR compartido.
**Persistencia necesaria:** Reusa pares stores + eraser. · **Feedback inmediato:** Correcto/intrusión-vieja. · **Feedback post-sesión:** Índice de interferencia. · **Feedback histórico:** Resistencia sostenida.
**Riesgo científico:** Practice effect alto (pares) → rotación estricta. · **Riesgo de UX:** Dos fases de estudio alargan la sesión. · **Riesgo técnico:** Extensión de `Ex4PairsGenerator` con overlap (bajo).
**Claim permitido:** "Entrenas y mides tu resistencia a la confusión entre asociaciones viejas y nuevas dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Cues A-C idénticos a A-B verificados por property test; intrusión vs conjunción exclusivas; adversario sobre-selección rojo.

### Discriminación Fina (EX12_DISCRIMINACION_FINA)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX12) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Discriminación mnemónica (rechazar señuelos muy similares).
**Subhabilidades:** Separación de patrones, control de falsas memorias.
**Hipótesis cognitiva:** Mnemonic similarity (paradigma de lure discrimination, Stark): distinguir lo visto de señuelos parecidos-pero-nuevos mide precisión de la traza, con d′ target/lure como señal (nunca hit-rate).
**Mecánica UX:** Study de ítems → test viejo/parecido/nuevo (3 opciones, ChoiceResponseLayer extendido o binario en V1).
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = señuelos poco similares. · **Dificultad máxima:** 10 = señuelos casi idénticos + demora.
**Variables de dificultad:** similitud del señuelo (SIMILARITY, eje gobernante), listLength, delay.
**Métricas capturadas:** `RECOGNITION_DPRIME`, `FALSE_ALARM_RATE` sobre lures, criterio.
**Scoring específico:** d′ con corrección log-lineal (reusa la matemática de EX6); criterio extremo → INVALIDATED; null bajo nMin (≥20 señal + ≥20 ruido, A2).
**Cómo afecta al score del bloque:** Control de errores + precisión episódica fina.
**Criterio de progresión:** SIMILARITY. · **Criterio de fallo:** Criterio extremo.
**Anti-gaming:** Señal: "todo viejo"/"todo nuevo". Neutraliza: d′ + `ExtremeCriterionGate` (compartido con EX6).
**Persistencia necesaria:** Store propio + eraser. · **Feedback inmediato:** ✓/✗ sobrio. · **Feedback post-sesión:** Sensibilidad vs sesgo. · **Feedback histórico:** d′ sostenido.
**Riesgo científico:** Banco de señuelos graduados por similitud es difícil de autorar en texto (mejor visual/paramétrico). · **Riesgo de UX:** Sensación de "trampa" con señuelos casi idénticos. · **Riesgo técnico:** Depende del banco graduado.
**Claim permitido:** "Entrenas y mides tu precisión para distinguir lo visto de lo casi-visto dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco con similitud graduada validada; goldens de d′ compartidos; criterio extremo testeado.

### Reconstrucción Parcial (EX13_RECONSTRUCCION)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX13) · **Prioridad:** P3 · **Apto MVP:** No (avanzado/modo libre en V1.1+)
**Habilidad cognitiva principal:** Recuperación constructiva con calibración metamnemónica.
**Subhabilidades:** Completamiento desde fragmentos, monitoreo de certeza.
**Hipótesis cognitiva:** Cued/fragment completion + judgments of learning (Koriat): completar material estudiado desde huecos y declarar confianza mide recuperación constructiva y calibración (acierto vs certeza).
**Mecánica UX:** Study → completar fragmentos con huecos (multi-select) + slider de confianza por respuesta (overlay EX16).
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = huecos pocos y salientes. · **Dificultad máxima:** 10 = fragmentos mínimos + demora + interferencia.
**Variables de dificultad:** proporción de hueco (LOAD), delay, interferencia, profundidad.
**Métricas capturadas:** accuracy de completamiento, `CALIBRATION_INDEX`, intrusiones.
**Scoring específico:** Accuracy con gate de discriminación; calibración solo como métrica secundaria (Brier de memoria, nMin ≥40 — A2).
**Cómo afecta al score del bloque:** Menor; su valor real es alimentar el overlay EX16.
**Criterio de progresión:** LOAD. · **Criterio de fallo:** Intrusiones altas.
**Anti-gaming:** Señal: confianza siempre-máxima para inflar calibración aparente. Neutraliza: la calibración puntúa alineación (sobreconfianza penaliza el índice, no el score del bloque).
**Persistencia necesaria:** Store propio + eraser. · **Feedback inmediato:** Acierto + gap de confianza. · **Feedback post-sesión:** Curva de calibración. · **Feedback histórico:** Sesgo de confianza.
**Riesgo científico:** Doble constructo (memoria+metacognición) — mantener señales separadas. · **Riesgo de UX:** Slider por respuesta añade fricción. · **Riesgo técnico:** Bajo.
**Claim permitido:** "Entrenas y mides tu recuperación desde pistas parciales y cómo se alinea tu certeza con tu acierto, dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Señales memoria/calibración separadas; Brier nMin gateado.

### Dual-Task Recall (EX14_DUAL_TASK)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX14) · **Prioridad:** P3 · **Apto MVP:** No (avanzado/modo libre — solapa varianza con EX1)
**Habilidad cognitiva principal:** Recuperación bajo carga concurrente (`DUAL_TASK_COST`).
**Subhabilidades:** Recall bajo distracción, gestión de recursos.
**Hipótesis cognitiva:** Dual-task methodology (Baddeley): comparar recall con y sin tarea concurrente estima el coste dual; el difference-score exige ≥20/celda y SOLO como métrica secundaria (A2).
**Mecánica UX:** Recall estándar intercalando bloques single/dual (tap rítmico concurrente).
**Duración recomendada:** 6–10 min. · **Dificultad mínima:** 1 = single. · **Dificultad máxima:** 10 = dual denso.
**Variables de dificultad:** carga concurrente (CONCURRENCY, gobernante), LOAD, similitud, delay.
**Métricas capturadas:** recall single vs dual, `DUAL_TASK_COST`, adherencia al tap.
**Scoring específico:** Coste dual = span_single − span_dual, secundaria; primaria = recall dual accuracy-based; null bajo nMin/celda.
**Cómo afecta al score del bloque:** Marginal (secundarias); nunca abre dimensión propia.
**Criterio de progresión:** CONCURRENCY. · **Criterio de fallo:** Abandono del tap concurrente (adherencia <umbral → inválido, patrón processing-gate).
**Anti-gaming:** Señal: ignorar la tarea concurrente. Neutraliza: gate de adherencia al tap (espejo del 0.85 de EX1).
**Persistencia necesaria:** Reusa span store + eraser. · **Feedback inmediato:** Validez de la concurrente. · **Feedback post-sesión:** Coste dual. · **Feedback histórico:** Tendencia del coste.
**Riesgo científico:** Difference-scores poco fiables (por eso secundaria). · **Riesgo de UX:** Frustración alta. · **Riesgo técnico:** Tap concurrente = timing sensible sin timebase (solo adherencia, no RT).
**Claim permitido:** "Entrenas y mides tu recuperación mientras atiendes otra tarea dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Gate de adherencia testeado; coste dual nunca primaria.

### Memoria Auditiva Narrativa (EX15_NARRATIVA_AUDITIVA)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX15) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Memoria episódica de material estructurado (quién/qué/orden).
**Subhabilidades:** Escucha/lectura profunda, memoria de detalles y orden narrativo.
**Hipótesis cognitiva:** Story recall (paradigma de recuerdo lógico): retener elementos y orden de una narración mide episódica estructurada; en V1 la "narración" es TEXTO leído (audio requiere assets TTS — diferido).
**Mecánica UX:** Leer mini-historia → preguntas de contenido/orden (multi-select con señuelos plausibles).
**Duración recomendada:** 6–10 min. · **Dificultad mínima:** 1 = historia corta, preguntas literales. · **Dificultad máxima:** 10 = historia larga + demora + preguntas de orden.
**Variables de dificultad:** longitud (LOAD), delay, profundidad de pregunta, interferencia entre historias.
**Métricas capturadas:** accuracy de contenido, accuracy de orden, intrusiones plausibles.
**Scoring específico:** Accuracy con señuelos plausibles obligatorios (gate de discriminación); null bajo nMin.
**Cómo afecta al score del bloque:** EPISODIC blend secundario (nunca sustituye al diferido 24 h).
**Criterio de progresión:** LOAD/DEPTH. · **Criterio de fallo:** FAR alta sobre señuelos plausibles.
**Anti-gaming:** Señal: responder por plausibilidad general sin leer. Neutraliza: señuelos plausibles-pero-falsos (responder por sentido común = FA medible).
**Persistencia necesaria:** Store propio + eraser; banco de historias versionado.
**Feedback inmediato:** ✓/✗. · **Feedback post-sesión:** Contenido vs orden. · **Feedback histórico:** Tendencia episódica estructurada.
**Riesgo científico:** Sesgo cultural del contenido narrativo; confound de comprensión lectora (frontera con Lenguaje — documentar blend). · **Riesgo de UX:** Lectura larga en móvil. · **Riesgo técnico:** Banco autorado grande (coste contenido).
**Claim permitido:** "Entrenas y mides cuántos detalles y en qué orden retienes de lo que acabas de leer dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Banco ≥30 historias con señuelos plausibles validados; blend con Lenguaje documentado.

### Nombres y Rostros — Avatares Sintéticos (EX8_NOMBRES_ROSTROS)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX8; condición dura de science: avatares sintéticos, NUNCA datasets de caras reales) · **Prioridad:** P3 · **Apto MVP:** No
**Habilidad cognitiva principal:** Memoria asociativa cara↔nombre (variante de alto valor de EX4 — merge recomendado por 04_5).
**Subhabilidades:** Binding percepto-verbal, recuperación por cue visual.
**Hipótesis cognitiva:** Face-name paired associates: asociar un identificador verbal a un estímulo visual complejo es el caso asociativo ecológicamente más citado; misma matemática que EX4 (`ASSOCIATIVE_INDEX` + discriminación).
**Mecánica UX:** Study avatar+nombre → recall cued (avatar → nombre entre señuelos). Modo material de `Ex4PairsGenerator`, no generador nuevo.
**Duración recomendada:** 5–8 min. · **Dificultad mínima:** 1 = 4 avatares distintos. · **Dificultad máxima:** 10 = 10 avatares similares + demora + nombres parecidos.
**Variables de dificultad:** pairCount, similitud de avatar y de nombre, delay, bidireccional.
**Métricas capturadas:** associativeIndex, conjunciones, FAR.
**Scoring específico:** Idéntico a EX4 (delegación a `RecognitionDiscriminationScorer`).
**Cómo afecta al score del bloque:** Asociativo blend (no dimensión nueva).
**Criterio de progresión:** LOAD/SIMILARITY. · **Criterio de fallo:** FAR alta.
**Anti-gaming:** Igual que EX4 (gate de discriminación); señuelos completos o skip.
**Persistencia necesaria:** Reusa pairs store + registro de avatares servidos (rotación).
**Feedback inmediato:** Acierto por par. · **Feedback post-sesión:** Índice asociativo. · **Feedback histórico:** Tendencia.
**Riesgo científico:** Sesgo del set de avatares (balancear atributos; validación del banco ES el gate). · **Riesgo de UX:** Avatares poco distinguibles. · **Riesgo técnico:** Pipeline de assets sintéticos (coste; por eso P3).
**Claim permitido:** "Entrenas y mides tu memoria de asociaciones cara-nombre dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Set sintético balanceado y versionado; cero imágenes de personas reales; tests de EX4 reutilizados sobre el modo material.

### Calibración de Confianza — Overlay (EX16_CALIBRACION_CONFIANZA)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: catálogo EX16 — modelado in-code como overlay transversal, no drill) · **Prioridad:** P2 · **Apto MVP:** No
**Habilidad cognitiva principal:** Metamemoria (calibración confianza↔acierto).
**Subhabilidades:** Monitoreo, resolución metacognitiva.
**Hipótesis cognitiva:** Judgments of learning / calibration (Koriat; Brier): declarar certeza por respuesta y compararla con el acierto real mide calibración; se aplica SOBRE los drills existentes (EX1/EX3/EX4), no como ejercicio aparte.
**Mecánica UX:** Slider/3-botones de certeza tras cada respuesta en los ejercicios que lo activen (opt-in por sesión).
**Duración recomendada:** +30–60 s sobre el drill anfitrión. · **Dificultad mínima/máxima:** hereda del anfitrión (1–10).
**Variables de dificultad:** Hereda; el overlay no adapta.
**Métricas capturadas:** `CALIBRATION_INDEX`, `BRIER_SCORE`, `CONFIDENCE_BIAS`, resolución.
**Scoring específico:** Brier de memoria con nMin ≥40 juicios (A2); NUNCA modifica el score del drill anfitrión; señal propia secundaria.
**Cómo afecta al score del bloque:** No afecta en MVP/V1.1 inicial; informe aparte ("tu certeza vs tu acierto").
**Criterio de progresión:** n/a (overlay). · **Criterio de fallo:** n/a.
**Anti-gaming:** Señal: certeza constante para no pensar. Neutraliza: varianza de confianza mínima requerida para computar resolución (si es constante → índice no computable, null).
**Persistencia necesaria:** Campo adicional en los stores anfitriones (versionado de schema).
**Feedback inmediato:** Ninguno por juicio (no condicionar). · **Feedback post-sesión:** Curva calibración. · **Feedback histórico:** Sesgo sostenido.
**Riesgo científico:** Reactividad del juicio (declarar confianza altera el recall — documentar). · **Riesgo de UX:** Fricción por respuesta. · **Riesgo técnico:** Toca todos los UiState anfitriones (regla sealed+renderer).
**Claim permitido:** "Mides cuánto se alinea tu certeza con tu acierto real dentro de MindBoost contra tu propia línea base."
**Criterios de aceptación:** Overlay no altera el scoring anfitrión (test); null si confianza sin varianza.

### Sonda de Transferencia UNTRAINED (MEM-17)

**Bloque:** Memoria · **Estado:** Post-MVP (Nuevo; fuente: diseño 03_5 "Sonda UNTRAINED"; motores `ReliableChangeIndex`/`RealImprovementCriterion`/`MemoryBaselineProtocol`/`TransferProbePartition` YA implementados y testeados, DORMANT por decisión — destino V1.1) · **Prioridad:** P1 (de V1.1: gatea toda claim de "mejora real" y la métrica 30 días A6) · **Apto MVP:** No
**Habilidad cognitiva principal:** Medición de capacidad (no entrena): near-transfer sobre material reservado.
**Subhabilidades:** n/a (transversal a WM/episódica/asociativa).
**Hipótesis cognitiva:** Jacobson-Truax (RCI |z|>1.96) sobre delta pre/post en material UNTRAINED con dificultad FIJA: la única evidencia admisible de cambio de capacidad frente a familiaridad con el drill; base del evento `mb_block_improvement_30d` con flag validated (A6).
**Mecánica UX:** Mini-batería 3–6 min, dificultad fija, SIN feedback por ítem, material de `TransferProbePartition` (nunca visto en entrenamiento), formas paralelas A/B/C rotadas; framing honesto "esto es una medición".
**Duración recomendada:** 3–6 min, periódica (cada N sesiones / ante meseta).
**Dificultad mínima/máxima:** FIJA por diseño (comparabilidad pre/post) — no aplica 1–10.
**Variables de dificultad:** Ninguna intra-probe; varían cobertura (1–4 constructos) e itemsPerConstruct 8–20.
**Métricas capturadas:** untrainedDelta, effectSize, RCI, `CONSOLIDATION_INDEX` untrained.
**Scoring específico:** `RciResult.NotEnoughData` por defecto hasta ≥2–3 administraciones con reliability estimada; claim solo si `isGenuine && RCI.reliable`; jamás puntúa "bajo" — solo confirma o no confirma cambio.
**Cómo afecta al score del bloque:** No al score de sesión; gatea el copy de "mejora confirmada" y la tendencia histórica.
**Criterio de progresión:** n/a. · **Criterio de fallo:** No se administra con fatiga alta (gate explícito).
**Anti-gaming:** Señal: memorizar la propia sonda / inflar el pre. Neutraliza: formas paralelas equiparadas + material reservado + cero feedback + RCI gated por reliability.
**Persistencia necesaria:** `TransferProbeSummary` (@Serializable, schemaVersion) en `mindboost_memory` + eraser.
**Feedback inmediato:** Ninguno durante la probe. · **Feedback post-sesión:** Resumen sobrio con caveats ("necesitamos más mediciones"). · **Feedback histórico:** Tendencia confirmada/no confirmada.
**Riesgo científico:** Equating de formas A/B/C es la pieza psicométrica más exigente (sin él, RCI = ruido — bloquea el claim, no el producto). · **Riesgo de UX:** Percepción de examen → abandono. · **Riesgo técnico:** Scheduler de probes nuevo; motores ya verdes.
**Claim permitido:** "Mide si tu capacidad cambió de forma fiable dentro de MindBoost respecto a tu propia línea base" (solo con RCI fiable; si no: "progreso en el ejercicio; aún sin evidencia de cambio de capacidad").
**Criterios de aceptación:** Formas equiparadas antes de comparar; gate de fatiga; `NotEnoughData` por defecto testeado; claim doble-gated (`isGenuine && reliable`); cero feedback intra-probe; DORMANT documentado hasta su fase.
---

## 11. MVP Selection

Subconjunto MVP = **41 ejercicios** (cuota por bloque DC-19). Regla: exactamente la cuota; el resto es Post-MVP. Suelo de contingencia 25-jul entre paréntesis (si el calendario aprieta, se recorta a este suelo con etiquetado honesto — BETA / "Motor de Razonamiento" / "PRÁCTICA").

| Bloque | Ejercicio MVP | Mantener/Nuevo/Rehacer | Por qué entra en MVP | Scoring listo | Persistencia lista | Riesgo |
|---|---|---|---|---|---|---|
| Enfoque (6) | E1 Vigía | Mejorar | Vigilancia PVT, paradigma núcleo, consenso unánime | Sí (adapter) | Sí | Bajo |
| Enfoque | E2 Filtro | Mejorar | Filtrado selectivo flanker; motor de congruencia honesto | Sí | Sí | Bajo (doble-conteo con E5) |
| Enfoque | E3 Gatillo | Mejorar | Go/no-go accuracy-based; alimenta la evaluación inicial | Sí | Sí | Bajo |
| Enfoque | E4 Freno | Mejorar | Stop-signal SSRT, mejor testing adversarial del proyecto | Sí | Sí | Bajo |
| Enfoque | E5 Conflicto | Mejorar | Interferencia Stroop/Simon; regla anti-doble-conteo con E2 | Sí | Sí | Bajo |
| Enfoque | E6 Cambio | Mejorar | Task-switching; payload deriva la respuesta | Sí | Sí | Bajo |
| Decisiones (5) | D1 Reversibility Triage | Mejorar (activar) | Habilidad núcleo; todo construido, falta gate humano | Sí (bloqueado hasta validar banco) | Sí | Alto (P0 — validación inter-rater) |
| Decisiones | D2 Bayes Arena | Mejorar | Única señal jugable hoy; adapter + dificultad real | Parcial→adapter | Parcial (P0 diario) | Alto |
| Decisiones | D3 Muestreo con coste | Nuevo | Parada óptima; constructo distinto de Bayes | Nuevo | Nuevo (SessionRecord) | Medio |
| Decisiones | D4 Dominancia | Nuevo | Priorización con error normativo inequívoco (sin jueces) | Nuevo | Nuevo | Medio |
| Decisiones | D5 Aceptar/Rechazar | Nuevo | Impulso + costes asimétricos; reusa ChoiceResponseLayer | Nuevo | Nuevo | Bajo |
| Lógica (5) | L1 Motor Wizard | Mantener | Única pieza jugable; máximo ROI con banco ≥30/familia | Sí (ReasoningScorer→adapter) | Sí | Medio (memorización→paramétrico) |
| Lógica | LOG-02 Matrices/Series | Nuevo | Cubre "patrones/series" del PRD, 0% cubierto hoy | Nuevo | Nuevo | Medio |
| Lógica | LOG-03 Ordenación deductiva | Nuevo | 3ª mecánica; desbloquea el re-etiquetado a "5 ejercicios" | Nuevo | Nuevo | Medio |
| Lógica | LOG-04 Rejilla de restricciones | Nuevo | Deducción multi-paso; GridField compartido con Memoria | Nuevo | Nuevo | Medio |
| Lógica | LOG-05 Contraejemplo con deadline | Nuevo | Falsación bajo presión; reusa ChoiceResponseLayer + E13 | Nuevo | Nuevo | Medio |
| Creatividad (4) | EX01 Suprimir 1ª idea | Mejorar | Paradigma divergente ya jugable; mejora barata | Proceso (sin nota) | Sí | Bajo |
| Creatividad | EX02 Rotación categorías | Rehacer | Única vía honesta a flexibilidad: mecánica de captura distinta | Proceso | Sí | Medio |
| Creatividad | CRE-03 Convergente | Nuevo | Única señal con accuracy verificable offline (sin IA) | Proceso/accuracy | Nuevo | Bajo |
| Creatividad | CRE-04 Divergente estructurado | Nuevo | 2ª mecánica divergente con señal de profundidad; banco barato | Proceso | Nuevo | Bajo |
| Lenguaje (6) | evidence_lock | Mantener | Contenido excelente ya jugable; gap 100% integración | Sí (in-session) | **P1: cablear** | Bajo (fix barato) |
| Lenguaje | semantic_surgery | Mantener | idem | Sí | P1 | Bajo |
| Lenguaje | compression_ladder | Mantener | idem | Sí | P1 | Bajo |
| Lenguaje | argument_xray | Mantener | idem | Sí | P1 | Bajo |
| Lenguaje | scope_negation | Mantener | idem | Sí | P1 | Bajo |
| Lenguaje | source_triage | Mantener | idem (candidato a suelo de contingencia si aprieta) | Sí | P1 | Bajo |
| Emociones (5) | EM1 Pausa | Mantener | Paradigma base, jugable, sin P0/P1 | Proceso (null by design) | Sí | Bajo |
| Emociones | EM2 Recuperación | Mantener | Recuperación post-error | Proceso | Sí | Bajo |
| Emociones | EM3 Separador | Mantener | Interpretación 4 categorías (ampliar banco) | Proceso | Sí | Bajo (banco) |
| Emociones | EM10 Separación bajo presión | Mantener | Reactividad bajo presión leve | Proceso | Sí | Bajo |
| Emociones | EM11 Continuidad | Mantener | Continuidad tras feedback negativo | Proceso | Sí | Bajo |
| Velocidad (5) | E1 Destello | Mejorar | SRT, ancla del bloque (12→24 trials) | Parcial (detector) | Sí | Medio (timebase API≥34) |
| Velocidad | E6 Multivía | Mejorar | CRT, gate de azar operativo | Parcial | Sí | Medio |
| Velocidad | E3 Cifrado | Mejorar | Verificación con carga (clave rota por bloque) | Parcial | Sí | Bajo |
| Velocidad | E7 Compuerta | Mantener | Inversión de regla, el más defendible | Parcial | Sí | Bajo |
| Velocidad | VEL-01 Conmutador | Nuevo | Cambio de regla veloz; reusa RuleSwitchField | Nuevo | Nuevo | Bajo |
| Memoria (5) | EX1 Span Complejo | Mantener | Ancla de memoria de trabajo | Sí | Sí | Bajo |
| Memoria | EX3 Lista con Estrategia | Mejorar | Única señal episódica real (P1: notificación 24h) | Sí | Sí | Medio (reenganche) |
| Memoria | EX4 Pares Asociados | Mantener | Asociativa, cero cambios | Sí | Sí | Bajo |
| Memoria | EX2 Actualización (updating) | Nuevo | Paradigma distinto; reusa StreamField; el más barato | Nuevo | Nuevo | Bajo |
| Memoria | EX6 Grid Espacial | Nuevo | Canal visoespacial ausente; GridField compartido; practice-effect bajo | Nuevo | Nuevo | Medio |

## 12. Post-MVP Selection

Los **82 ejercicios restantes** quedan diseñados y priorizados para V1.1/V2 (ver cada ficha, campo Estado = Post-MVP y su Prioridad). Resumen: Enfoque E7–E13 + ENF-14/15 (modo libre/avanzado, además donantes de renderers compartidos); Decisiones D6–D15 (marcos, anclaje, encuadre, coste hundido, probabilístico avanzado…); Lógica L6–L15 (deducción abstracta, series numéricas/figurativas, condicionales, analogías estructurales — con dedup documentado vs Lenguaje); Creatividad CRE-05–CRE-15 (asociación remota validada, mejora iterativa, combinación forzada…); Lenguaje LEN-07–LEN-15 (fluidez semántica cronometrada, vocabulario en contexto, parafraseo, ambigüedad, registros, analogías verbales, inferencia — todos requieren bancos nuevos, autoría cara); Emociones EM4–EM9/EM12–EM15 (calibrador no-mostrado, decisiones con banco 50, cadenas de recuperación…); Velocidad VEL-02–VEL-12 (DSST paramétrico, decisión léxica, Hick, conteo, espejos…); Memoria EX5/EX7–EX16 (secuencial, verbal, n-back, retención tras interferencia, confianza metacognitiva).

## 13. Content Bank Requirements

- **Generadores procedurales (Enfoque, Velocidad, la mayoría de Lógica):** sin banco textual → sin riesgo de memorización de contenido; requisito = seed no repetido por usuario-sesión, invariantes verificadas por property tests (balanceo, jitter mínimo, ≥1 señal por bloque), holdout de medición vía `ProbeBank`.
- **Bancos de ítems (Lógica L1 premisas, Decisiones escenarios, Lenguaje los 6+, Emociones interpretación):** tamaño mínimo declarado por ejercicio (Lógica ≥30 ítems/familia; Decisiones Reversibility 30 validados inter-rater κ≥0.75; Emociones ampliar EM3); política de no-repetición hasta agotar pool; versionado `bankVersion`; `BankValidator` + `BankIntegrity` (PROCEDURAL/VALIDATED/SMALL_UNVALIDATED) como gate antes de alimentar el perfil; contenido sin dominios sensibles (dinero real, legal, salud, autolesión) en Decisiones/Emociones.
- **Rúbricas offline (Lenguaje semántico, Creatividad convergente):** scoring por reglas deterministas verificables sin LLM (equivalencia semántica por listas de sinónimos/antónimos curadas, no por modelo generativo — el bloqueo de IA generativa permanece).

## 14. Scientific Risks

- **Practice effects / regresión a la media:** toda "mejora" se mide probe-a-probe sobre probes fijos con RCI (Jacobson-Truax), descartando la sesión 1; nunca sobre scores de entrenamiento adaptativo.
- **Difference-scores poco fiables** (congruencia, switch cost, dual cost, post-error): siempre secundarios (split-half típicamente <0.6), nunca en `primarySignal`.
- **Memorización de banco** (Lógica, Decisiones, Lenguaje): paramétrico + holdout + no-repetición; sin esto un banco pequeño se agota en la 1ª sesión (auditoría Lógica).
- **Señal fabricada por composición** (Velocidad fast-guessing, Creatividad "originalidad", Emociones "competencia"): detectores calibrados + normalizedScore null donde el flujo no captura el constructo (Emociones/Creatividad sin número en MVP).
- **Timebase no autoritativo** (Velocidad, Enfoque RT absoluto): RT crítico solo alimenta perfil con Choreographer + nanos nativos (Android API≥34); resto = feedback intra-sesión.
- **Contaminación cross-bloque** (interferencia contada dos veces en Enfoque E2/E5; Lógica bayesiana vs Decisiones D2; Lógica analogía vs Lenguaje argument_xray): dedup documentado, la señal cuenta una vez.
- **Confianza inflada por evaluación inicial:** siembra dificultad + bandas ordinales, jamás EMERGING/score/rango (DC-14).

## 15. Implementation Priority

Orden recomendado (alineado con las fases del plan maestro §17):

1. **Fase 0 (P0):** Decisiones D1 (validación inter-rater) + persistencia diario/sesión; commit del árbol; copy de Creatividad/Velocidad.
2. **Fase 1 (perfil):** adapters de Enfoque/Lógica/Bayes; Lenguaje persistencia + `LanguageProfileSource`; detector fast-guessing de Velocidad; `UserScoreMapper`.
3. **Fase 3 (cobertura MVP):** Decisiones D3/D4/D5; Lógica L2/L3(+L4); Memoria EX2 + EX6 + notificación; Creatividad EX02 rehecho + CRE-03/04; Velocidad VEL-01.
4. **Fase 5 (hacia 15):** el resto por prioridad de ficha (P2 antes que P3), bancos paramétricos primero.

Cada ejercicio nuevo nace con: generador + scorer + tests adversariales + consistency test catálogo↔registry + ficha en el catálogo in-code, y pasa el gate de entrada (`efficacy-guardian` + `psychometrician`) antes de exponerse.
