# 1. Diagnóstico estratégico

| Módulo | Capacidad principal | Capacidad secundaria | Riesgo de mala medición | Potencial de mejora | Recomendación estratégica |
|--------|--------------------|--------------------|------------------------|---------------------|---------------------------|
| **Enfoque** | Control atencional sostenido e inhibitorio (atención sostenida, inhibición de respuesta, resistencia a interferencia, control de cambio de tarea) | Velocidad de procesamiento bajo conflicto + monitorización del error (post-error slowing/accuracy) | **Bajo-Medio**: pipeline maduro (RT mediana, RT-CV, MAD, anticipación filtrada, `n`+`confident` por métrica, SSRT con `trustworthy`). Riesgo residual: MOT/RSVP/Dual son aproximaciones de una sola pulsación (no captura sub-trial real); adaptación inter-bloque NO cableada en runtime (`perAxisMastery=emptyMap`, un bloque/sesión) | **Alto**: señal limpia + dificultad paramétrica multi-eje + 13 paradigmas con primaryMetrics gated. Margen real de cambio medible en inhibición/control de conflicto | Módulo de referencia. Cablear adaptación inter-bloque real (multi-bloque por sesión) y baseline test-retest con `ReliableChangeIndex`+`ProbeBank` ya presentes. No tocar el motor de clasificación |
| **Velocidad** | Velocidad de procesamiento útil (RT simple y de elección, control velocidad-precisión) | Estabilidad temporal (VCE/CV) + control de impulsividad (anticipaciones, impIdx) | **Alto**: timebase autoritativo SOLO Android API34+ (Choreographer+MotionEvent); desktop/iOS fallback NO-autoritativo. `CognitiveProfileScore.preliminary=true` SIEMPRE; dificultad FIJA en runtime MVP-0 (staircase MVP-2 existe en dominio, no cableado al gate físico §3/§15 ABIERTO) | **Medio-Alto** condicionado a hardware: VCE/lapsos/CV son señales válidas; el techo (pc<0.80 o cv>0.30 → ·0.5) protege bien | Cerrar el gate de validación física Android antes de declarar el score autoritativo. Cablear `MultiSignalStaircaseEngine` al runtime. NUNCA mostrar score como autoritativo en desktop/iOS |
| **Lógica** | Calidad del razonamiento deductivo/condicional (coherencia proceso-resultado, ICR) | Calibración de confianza + transferencia isomorfa (estructura lógica vs forma superficial) | **Medio-Alto**: contenido = pack autorado FIJO de 6 items (1 probe isomórfico); sin generador procedural → riesgo de familiaridad de item alto en uso repetido. `thetaEstimate`/`uncertainty` se computan pero NO alimentan selección (no IRT/CAT real) | **Medio**: el ICR process-first (velocidad solo resta vía impulsividad) y la transferencia "ambos lados aciertan" son sólidos, pero el banco minúsculo limita el rango medible | Prioridad #1 de ampliación de contenido: generador procedural con `logicalStructureId`/`surfaceVariantId` y banco de probes isomórficos reservados. Cablear theta→selección |
| **Lenguaje** | Razonamiento verbal evidencial (inferencia soportada, no sobre-lectura, separación literal/inferido) | Precisión semántica + triaje de fuentes + alcance/negación | **Alto en flujo real**: engines world-class (gates fatiga/fairness/evidencia E0-E5, near-transfer, adaptación por utilidad), pero el `LanguageSessionStateHolder` es **provisional/demo**: bankType siempre TRAINING, contador monótono no reloj, NearTransfer/Adaptation NO invocados, packets en memoria (no persiste cifrado), gates NO ejercitados | **Alto** (potencial), **bajo** (realizado): la arquitectura de honestidad es la mejor del repo pero está desconectada del loop de UI | Cablear los engines existentes al holder de producción: pasar fatiga/confounders/bankType reales, invocar adaptación y near-transfer, persistir al sellar. El dominio ya existe — es trabajo de integración, no de diseño |
| **Memoria** | Memoria de trabajo (span complejo dual-task) + memoria episódica/asociativa | Estrategia de recuperación (clustering, cued recall, consolidación inmediata vs diferida) | **Medio**: solo 3/16 ejercicios jugables (EX1/EX3/EX4); `practiceEffectRisk` etiquetado por ejercicio. RCI **GATED a NotEnoughData** hasta reliability test-retest (nadie la persiste aún) → el criterio de cambio real no se dispara en producción | **Alto**: ÚNICO módulo con `ReliableChangeIndex` (Jacobson-Truax, z=1.96), `RealImprovementCriterion` (delta UNTRAINED como única prueba), `PlateauDetector` y delayed recall como métrica de primera clase | Cablear el cómputo/persistencia de reliability (≥2-3 administraciones de probe) para activar el RCI. Es el módulo psicométricamente más honesto: completarlo da el mayor crédito científico |
| **Decisiones** | Calidad de la decisión bajo incertidumbre (actualización bayesiana, BUR en espacio logit) | Calibración (Brier, CAG), triaje de reversibilidad, anti-gaming (AGRS) | **Medio-Alto**: anti-cheat fuerte (AGRS>0.75 anula sesión, RT<400ms), pero duplicidad de persistencia (SecureKeyValueStore blobs **y** SQLDelight coexisten → fuente de verdad ambigua); orquestador no persiste ni genera escenarios (delegado al caller, wiring de `save()` no confirmado) | **Medio**: formulario psicométrico denso (CDE/ICD/IED/CRI/PHD/Brier/BUR) con base normativa propia; el potencial de cambio medible depende de baseline longitudinal aún no cableado | Resolver la duplicidad de persistencia (eliminar SQLDelight si el pivote a blobs es definitivo). Confirmar generación de escenarios y `save()` en el sellado. Unificar convención de paquetes |
| **Emociones** | Control inhibitorio afectivo + recuperación post-error bajo carga emocional | Separación emoción/pensamiento (matriz de confusión, granularidad), tolerancia bajo presión | **Medio** (medición) / **crítico** (seguridad): 6/15 cableados; los proxies de sobrecarga **ARMAN check-in, NUNCA infieren estado interior** (diseño correcto); inducción leve-only con techo HARD=3, intensity nunca auto-sube. Riesgo: métricas catalogadas no producidas (CALIBRATION_ERROR, etc.); transferencia NOT_DEMONSTRATED (honesto) | **Medio**: señales de rendimiento limpias (commission rate, post-error recovery condicional), pero NO mide "regulación emocional" como tal — mide control inhibitorio en contexto afectivo | Mantener intacta la frontera de seguridad (escáner de terminología estricto, leve-only). Ampliar solo dentro del techo. NUNCA reetiquetar como medición de salud emocional |
| **Creatividad** | Fluidez ideacional (proxy de práctica, #ideas distintas) | Elaboración (palabras-promedio por idea) | **Alto por diseño deliberado**: AI 100% BLOQUEADO (`AI_RUNTIME_ENABLED=false`, `EVIDENCE_GATES_PASSED=false`, G01-G10 sin pasar); rúbrica determinista NO mide originalidad/flexibilidad reales — solo fluidez/elaboración. `aiDerived` siempre false con `require` en init | **Bajo (actual)**: sin evaluación de originalidad, 2 ejercicios (EX01/EX02), prompts estáticos (8+6). El potencial real está bloqueado tras los evidence gates | NO desbloquear AI sin pasar G01-G10 y privacy review. Etiquetar honestamente como "práctica de fluidez local", nunca como medida de creatividad. Ampliar prompts antes que paradigmas |

---

**(1) Capacidades cognitivas por módulo.** Los 8 módulos cubren un mapa funcional coherente y mayormente no-solapado: **Enfoque** = control atencional/ejecutivo (sostenido, inhibitorio, conflicto, switching) con 13 paradigmas; **Velocidad** = velocidad de procesamiento útil con control velocidad-precisión; **Lógica** = razonamiento deductivo/condicional con calibración; **Lenguaje** = razonamiento verbal evidencial (inferencia soportada, separación literal/inferido); **Memoria** = memoria de trabajo (span dual-task) + episódica/asociativa con consolidación; **Decisiones** = juicio bajo incertidumbre (bayesiano, reversibilidad, calibración); **Emociones** = control inhibitorio afectivo y recuperación post-error (NO regulación emocional clínica); **Creatividad** = fluidez ideacional como proxy de práctica. El solapamiento real y deseable es entre Enfoque-Velocidad (inhibición/RT) y Lógica-Lenguaje-Decisiones (razonamiento/calibración), pero cada uno aísla una capacidad gobernante distinta.

**(2) Huecos.** Tres clases. **Hueco de contenido**: Lógica (6 items fijos, sin generador), Memoria (3/16), Emociones (6/15), Creatividad (2 ejercicios) — riesgo directo de familiaridad de item en uso repetido. **Hueco de cableado** (dominio listo, loop desconectado): Lenguaje es el caso extremo (engines world-class no invocados, holder demo, sin persistencia al sellar), Velocidad (staircase MVP-2 y score MVP-3 en dominio, runtime fijo, gate físico abierto), Enfoque (adaptación inter-bloque no cableada), Memoria (RCI gated por falta de reliability persistida). **Hueco de infraestructura**: no existe contrato de ejercicio cross-módulo (cada módulo reimplementa Trial/Classifier/Assembler/MetricsEngine/AdaptiveEngine), `SecureKeyValueStore` vive dentro de `focus.data.local` acoplando todos los módulos a Enfoque, el hub hardcodea cada track sin registry, y Decisiones tiene doble persistencia (blobs + SQLDelight).

**(3) Qué ejercicios ampliar primero.** Orden por relación crédito-medible/coste: (a) **Lógica** — generador procedural con `logicalStructureId`/`surfaceVariantId` y banco de probes isomórficos reservados; el contrato `LogicContentProvider` ya anticipa un "future generative provider", es la inversión con mejor retorno psicométrico (rompe familiaridad y habilita rango de dificultad real). (b) **Lenguaje** — no son ejercicios nuevos sino cablear los 6 existentes a los engines (fatiga/fairness/near-transfer/adaptación/persistencia); el diseño ya es de primera clase. (c) **Memoria** — completar EX2 (sin generador pese a estar en catálogo) y el cómputo de reliability para activar el RCI. (d) **Emociones** — ampliar dentro del techo leve-only reutilizando los 3 renderers existentes (EM4/EM9 calibración/etiquetado). Creatividad se queda en último lugar: bloqueada por evidence gates por diseño.

**(4) Módulos con mayor potencial de progreso medible.** **Memoria** lidera: es el único con `ReliableChangeIndex` (Jacobson-Truax), `RealImprovementCriterion` que exige ganancia en probe UNTRAINED + effect size ≥0.30 + 2 semanas sostenidas, `PlateauDetector` y delayed recall como métrica de primera clase — el aparato para *probar* cambio real ya está construido. **Enfoque** le sigue por señal limpia y dificultad paramétrica multi-eje (inhibición y control de conflicto tienen margen de cambio genuino con SSRT/CONGRUENCY_EFFECT/SWITCH_COST). **Velocidad** tiene alto potencial pero condicionado al cierre del gate físico Android. Lógica y Decisiones son medios (limitados por contenido/baseline longitudinal); Lenguaje es alto en potencial pero no realizado hasta el cableado.

**(5) Módulos que más necesitan protección contra falso progreso.** **Lógica** y **Creatividad** son los más expuestos: banco de items minúsculo → mejora atribuible a familiaridad, no a capacidad. Lógica ya mitiga con la transferencia isomórfica (1.0 solo si gemelo entrenado Y probe aciertan) y `PRACTICE_EFFECT` debe vigilarse; Creatividad mide solo fluidez (riesgo de confundir "más palabras" con creatividad). **Memoria** está bien protegida en diseño (la única delta que prueba cambio es `untrainedDelta`, RCI gated) pero el RCI no se dispara aún → falso progreso silencioso si se muestra mejora sin reliability. **Velocidad** necesita el techo pc<0.80/cv>0.30 y el `preliminary=true` permanente hasta el gate físico. La defensa transversal correcta ya existe por convención: barras `null` = no computable (nunca 0 silencioso), `confident` por `n`, MAD para tendencia central y conteo de eventos extremos sobre datos crudos.

**(6) Métricas primarias/secundarias/de seguridad.** **Primarias** (gobernantes, deben mostrar el cambio): Enfoque → SSRT, CONGRUENCY_EFFECT, SWITCH_COST, SUSTAINED_FOCUS_INDEX; Velocidad → VCE/velocidad útil (NUNCA RT crudo sin precisión); Lógica → ICR + transfer isomórfico; Lenguaje → evidencia capturada (Jaccard) + estado E0-E5; Memoria → delayed recall + RCI sobre probe untrained; Decisiones → BUR + Brier/CAG; Emociones → commission rate + post-error recovery; Creatividad → fluidez (proxy). **Secundarias** (contextualizan, no autorizan avance solo): RT mediana, RT-CV/estabilidad, clustering, elaboración, granularidad. **De seguridad** (gatean honestidad y bienestar, nunca premian): `confident`/`n` por métrica, anticipaciones e impIdx (impulsividad), AGRS (anti-gaming, anula sesión), fatiga (FatigueIndex/FatigueGate → bloquea strong updates), techo velocidad-precisión, `OverloadProxy`/induction leve-only en Emociones, y `preliminary=true`/barras `null`. Regla rectora: la velocidad y la racha **nunca suman** crédito por sí solas; solo restan vía impulsividad o se reportan como contexto — coherente con las fronteras de "perfil cognitivo funcional, práctica, baseline personal, dificultad adaptativa".

---

## 2. Principios maestros de diseño

> **Fronteras de seguridad (vinculantes en toda esta sección):** MindBoost entrena un **perfil cognitivo funcional**; no diagnostica, no mide CI ni promete mejora clínica. Toda métrica es señal de **práctica contra el baseline personal** bajo **dificultad adaptativa**. Cualquier salida cuyo soporte estadístico no alcance el umbral se reporta como `null` (em-dash en UI), **nunca como 0**, y todo resultado de score arranca `preliminary = true`. Estos principios son el contrato de diseño que todo ejercicio nuevo debe cumplir antes de exponerse en `ExerciseCatalog.implemented`.

---

### 2.1 Principios cognitivos

Cada principio se define operativamente (qué es, en términos manipulables) y se instrumenta (qué símbolo del repo lo materializa). El sustrato común es: un `TrialGenerator` determinista-seeded produce estímulos sobre un `DifficultyVector` multi-eje; un `*SessionAssembler` es la **autoridad de clasificación y métrica** (los contadores en vivo del holder son secundarios); un `MetricsEngine` reduce trials a un `MetricSet`; y un motor adaptativo decide el siguiente `DifficultyVector`.

#### 2.1.1 Adaptive challenge (reto en zona de ajuste)
- **Definición operativa:** mantener la probabilidad de acierto del usuario dentro de una banda objetivo (típicamente accuracy ≈ 0.75–0.85 en tareas de exactitud, o un percentil-correcto fijado en tareas RT-gated), de modo que la tarea nunca sea trivial ni saturante.
- **Instrumentación:** el motor adaptativo (`DefaultAdaptiveEngine.decide(AdaptiveInput): AdaptiveResult` en focus; `DefaultStaircaseEngine`/`MultiSignalStaircaseEngine` con `CCE 0..1 → deadline` en speed) ajusta el `DifficultyVector`/deadline tras cada `BlockResult`. La banda objetivo es un parámetro de `StaircaseConfig`/config versionada, no un literal disperso. **Regla:** ningún ejercicio expone dificultad fija; debe declarar al menos un `DifficultyAxis` continuo o cuasi-continuo recorrido por el staircase.

#### 2.1.2 Deliberate practice (práctica deliberada)
- **Definición operativa:** repetición orientada a un sub-componente débil, con feedback inmediato y metas por encima del rendimiento actual, no práctica difusa.
- **Instrumentación:** el planificador (`DefaultDailyPlanGenerator → DailyFocusPlan` con `PlannedBlock` weakness-first) prioriza el eje con peor `MetricValue` reciente; el `*SessionAssembler` aísla la métrica del sub-componente (p. ej. `MetricKey` de inhibición vs. velocidad base) para que la práctica ataque la señal débil. **Regla:** todo bloque debe poder atribuirse a un sub-componente medible; no se permiten bloques "genéricos".

#### 2.1.3 Desirable difficulty (dificultad deseable)
- **Definición operativa:** introducir fricción que perjudica el rendimiento inmediato pero mejora la retención/transferencia (p. ej. recuperación espaciada, generación frente a reconocimiento).
- **Instrumentación:** se modela como ejes de `DifficultyVector` que el staircase puede activar **sin** que el motor los confunda con regresión: el motor adaptativo distingue *step-down por seguridad/fatiga* de *fricción deseable programada*. **Regla:** la fricción deseable se introduce vía `DifficultyAxis` explícito y se documenta en `ExerciseDefinition`; nunca como ruido no medido.

#### 2.1.4 Interleaving (entrelazado)
- **Definición operativa:** alternar tipos de trial/sub-tareas dentro de una sesión en lugar de bloquear por tipo, forzando selección de regla en cada trial.
- **Instrumentación:** el `BlockSpec` que recibe `TrialGenerator.generate(...)` define la mezcla y el orden (seed determinista → reproducible/auditable). En paradigmas de cambio de regla (E6 task-switch), el coste de entrelazado se mide como `SWITCH_COST` y se calcula **solo con HIT-con-RT en ambos lados** de la comparación. **Regla:** una métrica comparativa (switch cost, congruencia) exige RT válido en las dos condiciones; si falta un lado → `null`.

#### 2.1.5 Spacing (espaciado)
- **Definición operativa:** distribuir la práctica en el tiempo (intra e inter-sesión) en lugar de masificarla, para favorecer consolidación.
- **Instrumentación:** a nivel inter-sesión lo gobierna `TrainingCycle`/`RecoveryPolicy`/`SessionTemplate` (focus) sobre la serie histórica de `SessionSummary` leída desde `LocalSessionHistoryRepository`; el espaciado se mide con `EpochClock.nowMillis()` (clave user-scoped `userId:endedAtMs`), nunca con `Clock.System` directo. **Regla:** las decisiones de espaciado se calculan sobre timestamps persistidos de summaries, no sobre estado en memoria.

#### 2.1.6 Feedback inmediato + diferido
- **Definición operativa:** feedback inmediato por trial (correcto/incorrecto/latencia) para corrección motora-cognitiva; feedback diferido por sesión/serie (tendencia, slope, percentil personal) para metacognición.
- **Instrumentación:** inmediato lo emite el renderer por trial (`FocusStimulusField`/`FocusChoiceField`); diferido lo emite el `*ScoreCalculator → *Score` y el agregador (`BrainProfileUiModelBuilder`/`BrainProfilePolygon`). **Regla:** el feedback diferido nunca afirma una capacidad que el dato no soporta: `CALIBRATING → em-dash`, `preliminary=true` visible.

#### 2.1.7 Reducción de ruido métrico
- **Definición operativa:** separar señal cognitiva de artefactos (RT prematuros = anticipación; RT extremos lentos = lapsos/distracción; outliers de medición).
- **Instrumentación:** el `MetricsEngine` aplica filtrado **diferenciado por propósito** (aprendizaje 2026-06-04): el rechazo de outliers MAD/SD se aplica **solo** a métricas de tendencia central (mediana RT, RT-CV, IES); los eventos extremos *por definición* (lapsos, anticipaciones) se cuentan sobre datos crudos tras filtrar únicamente la categoría opuesta. **Regla:** nunca contar un evento extremo sobre una lista de la que se han eliminado los extremos.

#### 2.1.8 Near vs far transfer
- **Definición operativa:** *near* = mejora en variantes del mismo paradigma; *far* = transferencia a un dominio distinto. MindBoost **solo afirma near transfer medible**; far transfer se reporta como hipótesis no garantizada.
- **Instrumentación:** la transferencia se mide con ítems isomorfos (gemelo entrenado ↔ isomorfo no entrenado) y **solo vale 1.0 cuando ambos lados son correctos** (aprendizaje 2026-06-15). El brain-profile (`ModuleContribution` one-way → `MindBoostProfileAggregator`) expone señal coarse por `CognitiveDimension`, no claims de transferencia general. **Regla:** prohibido afirmar far transfer/IQ; el copy usa "perfil cognitivo funcional".

#### 2.1.9 Control de fatiga
- **Definición operativa:** detectar degradación intra-sesión (deriva de RT/accuracy en función del tiempo en tarea) y por subjetividad, y abortar/enfriar antes de contaminar la medida.
- **Instrumentación:** `FatigueIndex`/`FatigueLevel` alimenta gates en el motor adaptativo (`AdaptiveDecision.StopSession`/`Cooldown`/`StepDown`) y en `MultiSignalStaircaseEngine` (directiva `COOLDOWN`/`STABILIZE`); `SubjectiveState` aporta la vía auto-reportada. La tabla de prioridad pone **seguridad/fatiga antes que progresión**. **Regla:** ningún staircase sube dificultad con fatiga alta; el control de fatiga precede a cualquier `StepUp`/`SwitchAxis`.

---

### 2.2 Principios de scoring

Convención transversal (por convención, no por tipo común aún): **barra `null` = no computable** (nunca 0 silencioso), `preliminary=true` hasta alcanzar suficiencia, macro-categoría **descriptiva no clínica**. La unidad mínima de evidencia es `MetricValue(clave → valor + n + confident)` (ya presente como `MetricValueDto` en focus): `confident=false` cuando `n` cae bajo el umbral del `MetricsEngine` (aprendizaje 2026-06-04 sobre fixtures coherentes).

#### 2.2.1 Accuracy
- **Operacionalización:** proporción de respuestas correctas sobre trials válidos (excluye anticipaciones y trials abortados). En paradigmas de elección/go-no-go, "correcto" depende de la **clave/lado** (`classifyChoice`), no solo de responder en ventana.

#### 2.2.2 Reaction time (RT)
- **Operacionalización:** latencia estímulo→respuesta **solo de HITs** (los errores no aportan RT informativo de velocidad de proceso). Estadístico de tendencia = **mediana** sobre `cleanRts` (post-MAD), no media. Timebase de alta resolución vía `SpeedTimebase` (Android Choreographer/monotónico).

#### 2.2.3 Inverse Efficiency Score (IES)
- **Operacionalización:** `IES = RT_medio_de_hits / accuracy` (penaliza compensar velocidad con error). Se computa sobre `cleanRts` y accuracy de trials válidos; `null` si accuracy=0 o n insuficiente. Integra velocidad y exactitud en una sola señal anti-trade-off.

#### 2.2.4 Omission vs commission errors
- **Operacionalización:** *omission* = no responder a un target (señal de inatención/lapsus); *commission* = responder a un no-target o lado erróneo (señal de impulsividad/control inhibitorio). Se cuentan **separadas** porque informan sub-componentes distintos; el motor adaptativo trata commission alta como gate de impulsividad (`ContainImpulsivity`).

#### 2.2.5 Consistency (RT-CV)
- **Operacionalización:** `RT-CV = SD(cleanRts) / mediana(cleanRts)`, coeficiente de variación de los HITs (estabilidad atencional). Constantes de umbral nombradas (`CV_VERY_STABLE`, etc.), no mágicas. Alta CV = atención inestable aunque la media sea buena.

#### 2.2.6 Learning slope
- **Operacionalización:** pendiente de regresión de la métrica primaria (score/IES/accuracy) sobre la serie de `SessionSummary` (índice de sesión o tiempo). Positiva = aprendizaje; se reporta con su intervalo, no como punto.

#### 2.2.7 Fatigue drift
- **Operacionalización:** pendiente intra-sesión de RT/accuracy vs. posición de trial/bloque. Drift adverso significativo marca la sesión como contaminada y alimenta `FatigueIndex` (degrada `confident`).

#### 2.2.8 Plateau
- **Operacionalización:** ausencia de slope significativo sostenido N sesiones, distinguiendo *plateau real* de *false improvement* (mejora por familiaridad de UI, no cognitiva). Único módulo con detección formal hoy: `PlateauDetector → PlateauStatus(None/Plateau/FalseImprovement)` + `InterventionPlanner` (9 `InterventionProtocol`, golden rule 2 semanas). **Recomendación de diseño:** elevar este detector a core para alimentar cualquier serie de summaries.

#### 2.2.9 Reliable improvement (RCI)
- **Operacionalización:** Reliable Change Index = `(score_post − score_pre) / SE_diff`, con `SE_diff` derivado de la fiabilidad test-retest y la SD del baseline. **Solo** un `|RCI|` por encima del umbral (≈1.96) se reporta como cambio real; por debajo → "dentro de la variación normal". Esto bloquea el claim de mejora hasta que supere el ruido.

#### 2.2.10 Reversibility
- **Operacionalización:** verificación de que una mejora se mantiene tras descanso/condición distinta (no fue un pico transitorio). Toda métrica comparativa se calcula con señal de **ambos lados** (no solo el intento entrenado), espejando el principio de transferencia isomorfa.

#### 2.2.11 Calibration
- **Operacionalización:** ajuste entre dificultad entregada y rendimiento obtenido: el staircase está bien calibrado si mantiene al usuario en la banda objetivo (§2.1.1). Se monitoriza con la tasa de aciertos efectiva vs. la banda; desviación sistemática → recalibrar baseline.

#### 2.2.12 Confidence/error alignment
- **Operacionalización:** correspondencia entre confianza subjetiva declarada (`SubjectiveState`) y exactitud real (metacognición). Buen alineamiento = alta confianza en aciertos, baja en errores. Se reporta como señal metacognitiva descriptiva, nunca como rasgo clínico.

---

### 2.3 Principios de progresión

#### 2.3.1 Niveles de dificultad
- Dificultad **paramétrica** sobre `DifficultyVector` multi-eje (`DifficultyAxis`), no niveles discretos hardcodeados. Cada eje tiene rango, paso y semántica documentada en `ExerciseDefinition`. Los "niveles" visibles al usuario son una proyección legible del vector, no la fuente de verdad.

#### 2.3.2 Escalado (staircase 1-up / N-down)
- Procedimiento adaptativo psicofísico: **1-up / N-down** converge a un umbral de rendimiento conocido (p. ej. 1-up/3-down ≈ 79% correcto). Implementado por `DefaultStaircaseEngine`/`AdaptiveStaircaseEngine` con **anti-oscilación obligatoria**: histéresis + pasos asimétricos + dead-zone (subir cuesta más que bajar). **Regla:** todo staircase declara su regla (X-up/Y-down) y su objetivo de convergencia en `StaircaseConfig` versionada.

#### 2.3.3 Desbloqueos
- Un paradigma se expone (`ExerciseCatalog.implemented`) **solo** cuando hay generador en `registry` + renderer compatible (aprendizaje 2026-06-18: dominio listo ≠ UI alcanzable). `domainReady` vs `implemented` vs `pendingUiIntegration` separan ambas cosas; `ExerciseCatalogConsistencyTest` rompe el build si `implemented != registry.keys`. Desbloqueos al usuario se condicionan a suficiencia de baseline, no a gamificación arbitraria.

#### 2.3.4 Personalización por baseline
- El baseline personal (`BaselineSnapshotDto` persistido en namespace del módulo) ancla la dificultad inicial y el cero de referencia del progreso ("contra tu propio baseline"). El planificador arranca el staircase cerca del umbral estimado del baseline, no en un default global, minimizando trials desperdiciados en zona trivial.

#### 2.3.5 Regresión inteligente (deload)
- Reducción **deliberada y temporal** de dificultad/volumen para recuperar tras fatiga, plateau o caída de rendimiento, gobernada por `RecoveryPolicy` y por los gates de seguridad del motor adaptativo (`StepDown`/`Cooldown`/`StopSession`). El deload se distingue semánticamente de la fricción deseable (§2.1.3): el motor **nunca** confunde regresión por seguridad con dificultad programada. **Regla:** el deload prioriza siempre sobre la progresión en la tabla de decisión.

---

### 2.4 Principios de UX

Tokens **obligatorios y centralizados** (`design/`): cero literales `dp`/`Color` en UI; todo vía `AppColors`/`AppSpacing`/`AppShapes`/`AppElevation`/`AppAnimation`/`MindOSTheme`.

#### 2.4.1 Claridad
- Un trial = una decisión inequívoca. El estímulo objetivo, la regla activa y la zona de respuesta deben ser legibles en <1 s. En paradigmas de elección, las mitades táctiles LEFT/RIGHT cubren toda la pantalla; en cambio de regla, el cue de regla es persistente y distinto del estímulo. **Regla:** la pantalla nunca muestra la respuesta correcta como affordance (render deshonesto): el usuario la **deriva** del estímulo (aprendizaje 2026-06-18, E6).

#### 2.4.2 Rapidez (loop corto)
- Ciclo estímulo→respuesta→feedback→siguiente trial en pocos cientos de ms de feedback; sin pantallas de transición innecesarias. Las animaciones solo existen cuando son **funcionales** (la propia tarea: MOT/RSVP/stop-signal), nunca decorativas por defecto (memoria de usuario "animation on-demand only"). El timebase usa alta resolución, no `delay` de UI.

#### 2.4.3 Atractivo sin fricción artificial
- Estética premium con `MindOSTheme`, pero **prohibida la dificultad artificial** (timers cosméticos, ruido visual que mida familiaridad con la UI en lugar de cognición). Toda fricción debe ser una `DifficultyAxis` medible.

#### 2.4.4 No frustrante / motivación sana
- Banda adaptativa (§2.1.1) evita el muro de dificultad; feedback diferido enmarca el progreso "contra tu baseline", **nunca por culpa** ni comparación social punitiva. El deload se presenta como recuperación inteligente, no castigo. Mensajes sin claims clínicos/IQ (skill `review-cognitive-claims`).

#### 2.4.5 Reglas concretas (tiempos, affordances, color/contraste, accesibilidad)
- **Tiempos:** ventana de respuesta y feedback derivados del deadline adaptativo, no fijos arbitrarios; ráfagas (RSVP) keyed por `layoutSeed = stimulus.index` para estabilidad + variación por trial.
- **Affordances:** `MinTouchTarget = 48dp` (token), zonas de respuesta grandes y consistentes entre trials del mismo paradigma.
- **Color/contraste:** estados (correcto/incorrecto/stop) por tokens semánticos de `AppColors` con contraste suficiente; el rojo de `FocusFieldState.Stop` es distinguible para daltonismo (no depender solo del hue: añadir forma/posición).
- **Accesibilidad:** texto escalable, no depender de color como único canal, latencia táctil mínima. En `Canvas`, las trig de `kotlin.math` (`cos/sin`) devuelven `Double` → `.toFloat()` antes de alimentar `Offset`/radios (aprendizaje 2026-06-04/18).
- **Reachability:** toda lista vertical de selección usa `verticalScroll`/`LazyColumn` — un Composable que compila no equivale a una UI alcanzable (aprendizaje 2026-06-25).

---

### 2.5 Principios de implementación KMP

Cada ejercicio se modela como una tubería determinista y testeable en `commonMain`, con la plataforma aislada solo en captura de timebase/input. Estado actual del repo: **cada módulo reimplementa su contrato** (no hay `Trial`/`TrialResult`/`SessionSummary` común cross-módulo); estos principios definen los **roles canónicos** que todo ejercicio debe cubrir, mapeados a los símbolos existentes de `focus` (referencia más completa) y recomendando su elevación a un `mindboost/core/exercise` neutral.

| Rol canónico | Símbolo de referencia (focus) | Responsabilidad |
|---|---|---|
| **ExerciseDefinition** | `ExerciseDefinition` / `ExerciseId` (en `registry`) | Identidad, ejes de dificultad declarados, paradigma, renderer compatible, criterios de suficiencia. Fuente de verdad de lo "wired" = `FocusExerciseGenerators.registry`. |
| **TrialGenerator** | `TrialGenerator.generate(DifficultyVector, BlockSpec, seed): List<Stimulus>` | **Determinista y seeded** (reproducible/auditable). Deriva `correctResponse` del estímulo, nunca la expone. Para dificultad online (stop-signal) emite esquema + tracker. |
| **ExerciseState** | `FocusUiState` / `FocusFieldState` (+ `FocusSessionStateHolder`) | Estado de presentación por trial. Holder parametrizado por `ExerciseId`; captura una respuesta (o LEFT/RIGHT) y delega clasificación al assembler. No llama `Clock.System` (usa `EpochClock`). |
| **UserResponse** | `RecordedTrial` (con `responseKey`, `latencyMs`, SSD usada) | Transporta **todo** lo que la clasificación necesita; no asumir que la latencia basta (aprendizaje E2/E4/E6). |
| **TrialResult** | `TrialResult` (vía `TrialClassifier.classify/classifyChoice/classifyStop`) | Outcome tipado (HIT/MISS/FALSE_ALARM/CORRECT_REJECTION…) producido por la **autoridad única** (assembler), no por los contadores del holder. |
| **ScoreResult** | `BlockResult`/`SessionResult` → `MetricSet` → `FocusScore` (`FocusScoreCalculator`) | Reduce trials a métricas normalizadas `(clave→valor+n+confident)`; barras `null` si no confident; `preliminary=true`. |
| **AdaptationSignal** | `AdaptiveInput` → `DefaultAdaptiveEngine.decide` → `AdaptiveResult`/`AdaptiveDecision` | Decisión del siguiente `DifficultyVector` con tabla de prioridad seguridad>progresión, histéresis y gates de fatiga/impulsividad. |
| **SessionSummary** | `SessionSummaryDto` (persistido vía `LocalSessionHistoryRepository`) | **Solo resúmenes**, nunca trials crudos. `@Serializable` + `schemaVersion`, `Json{ignoreUnknownKeys; encodeDefaults}`, clave user-scoped `userId:endedAtMs`. Alimenta slope/plateau/spacing. |

**Reglas de implementación vinculantes:**
1. **Persistencia:** patrón `SecureKeyValueStore` (AES-256-GCM+Keystore en Android; `InMemory` en desktop/iOS), DTO `@Serializable`+`schemaVersion`, `readVersioned(...).toDomain()`. **Nunca** Room/SQLDelight; solo se persisten summaries. Namespace propio en `MindBoostStorageNamespaces` (+ `AccountBound` para borrado de cuenta).
2. **Determinismo:** generador seeded ⇒ toda sesión es reproducible para test/auditoría; la dificultad online se resuelve por-trial en el runtime y se registra en `RecordedTrial` para que el assembler la propague.
3. **Autoridad de clasificación:** el `*SessionAssembler` es la única fuente de outcome/métrica; el holder solo presenta y captura. Holder y assembler comparten **exactamente** la misma vía de despacho de clasificación.
4. **Wiring honesto:** exponer en `registry` + `implemented` a la vez; un `ExerciseCatalogConsistencyTest` ata `implemented ⟺ registry.keys` y rompe el build ante divergencia. Distinguir `domainReady` de `implemented`.
5. **DI:** patrón `XGraph` + `@Composable rememberXGraph()` por módulo; el hub (`MindBoostHubScreen`, sealed `MindBoostTrack` + `when` + `TrackCard`) monta cada `XRootScreen` self-contained. **Recomendación:** introducir un `TrackRegistry` (análogo a `FocusExerciseGenerators.registry`) para que añadir un módulo no toque tres sitios.
6. **Brain-profile:** cada módulo expone señal coarse one-way vía adaptador `CognitiveModuleProfileSource → ModuleContribution` (consumiendo solo superficies públicas), que `MindBoostProfileAggregator` pliega a `MindBoostProfile` (estados `CALIBRATING/EMERGING/ESTABLISHED`, composite ≥3 established, nunca fabrica valores).

**Huecos arquitectónicos reconocidos (deuda a resolver para escalar estos principios):** no existe contrato de ejercicio común cross-módulo (`Trial<TStimulus,TResponse>`/`TrialOutcome`/`SessionSummary` genéricos + `Assembler` neutral); no hay `MetricsEngine`/`MetricKey` común (elevar `MetricValueDto` a core como métrica normalizada); no hay `AdaptiveEngine<Difficulty>` ni `StaircaseConfig` compartidos (políticas de histéresis/asimetría/fatiga reutilizables); `PlateauDetector`+`InterventionPlanner` viven solo en memory (subir a core); `SecureKeyValueStore` vive en `focus.data.local` (extraer a `mindboost/storage`); falta `TrackRegistry`; coexisten dos `CognitiveTaxonomy` (focus vs profile) sin unificar; la periodización (`DefaultDailyPlanGenerator`/`TrainingCycle`/`RecoveryPolicy`) es exclusiva de focus (generalizar a core). Estos huecos no bloquean implementar un ejercicio hoy contra el patrón `focus`, pero todo ejercicio nuevo debe diseñarse para **migrar sin fricción** a los contratos neutrales cuando se eleven.

---

# 4. Arquitectura de scoring y adaptación

> **Ámbito y honestidad de tipos.** Esta sección especifica la capa transversal de scoring y adaptación que opera sobre los 8 módulos (focus, memory, speed, logic, language, creativity, decisiones, emotions) y sus 58 ejercicios. Reconoce el estado real del repo: **no existe un contrato de ejercicio común** (cada módulo reimplementa `Trial/Classifier/Assembler/MetricsEngine`), y la única superficie compartida hoy es `ModuleContribution`/`CognitiveModuleProfileSource` (one-way hacia brain-profile). Por tanto, la arquitectura aquí descrita se introduce como un **núcleo neutral nuevo** `mindboost/core/scoring` + `mindboost/core/adaptive`, alimentado por **`SessionSummary` normalizados** que cada módulo emite a través de un adaptador (extensión del patrón `CognitiveModuleProfileSource`), **sin reescribir** los motores por módulo. Los engines por-módulo (`DefaultMetricsEngine`, `DefaultAdaptiveEngine`, `DefaultStaircaseEngine`, `MultiSignalStaircaseEngine`, `PlateauDetector`) permanecen como productores autoritativos de métricas crudas; el núcleo solo **normaliza, compone y decide a nivel meta**. Regla de oro mantenida en todo el núcleo: **una barra/valor `null` significa "no computable", nunca 0 silencioso**; todo score nuevo nace `preliminary=true` hasta superar los gates de fiabilidad.

## 4.2 Scoring común

### 4.2.1 Contrato de entrada normalizado

El núcleo no consume `MetricKey`/`EmotionMetricSet`/`SessionMetrics` directamente (tipos divergentes por módulo). Cada módulo proyecta su resultado de sesión a una métrica normalizada elevada a core, espejo del `MetricValueDto` ya presente en focus:

```
data class NormalizedMetric(
  val key: MetricId,        // enum neutral: ACCURACY, RT_MEDIAN, RT_CV, LAPSES,
                            //   CALIBRATION_ERROR, SWITCH_COST, IMPULSIVITY_IDX, FALSE_ALARM, ...
  val value: Double?,       // null = no computable (n insuficiente o no confident)
  val n: Int,               // tamaño muestral efectivo del estimador
  val confident: Boolean,   // gate del MetricsEngine de origen (n>=umbral)
  val direction: Direction  // HIGHER_IS_BETTER | LOWER_IS_BETTER | NEUTRAL
)

data class SessionSummary(
  val moduleId: CognitiveModuleId,
  val exerciseId: String,
  val endedAtMs: Long,           // EpochClock.nowMillis(), nunca Clock.System directo
  val difficulty: NormalizedDifficulty, // escalar 0..1 derivado del DifficultyVector/CCE/staircase del módulo
  val metrics: List<NormalizedMetric>,
  val subjective: SubjectiveState?,      // fatiga/esfuerzo reportado (0..1), reusa el de focus
  val durationMs: Long
)
```

`NormalizedDifficulty` colapsa el eje de dificultad nativo (multi-eje `DifficultyVector` en focus, CCE 1D en speed, nivel discreto en logic/language) a un escalar `d∈[0,1]` mediante una función de proyección declarada **por módulo** (cada adaptador la provee; el núcleo nunca asume la forma). Esto permite que `difficulty-adjusted performance` sea comparable cross-módulo sin imponer una taxonomía de dificultad común.

### 4.2.2 Los ocho scores y sus fórmulas

Todos los scores se calculan **por (módulo, ejercicio)** y se agregan opcionalmente a nivel módulo. Notación: una sesión `s` con métricas normalizadas; `clamp01(x)=max(0,min(1,x))`.

**1) Raw score `R` (0..1).** Combinación lineal de las métricas de la sesión orientadas a "mejor=alto" tras invertir las `LOWER_IS_BETTER`. Para una métrica `m`:

```
oriented(m) = if m.direction == LOWER_IS_BETTER then 1 - norm(m.value) else norm(m.value)
```

donde `norm` mapea el valor crudo a `[0,1]` con anclas empíricas por `MetricId` (p. ej. `RT_MEDIAN` se normaliza contra `[floorRtMs, ceilRtMs]` del ejercicio; `ACCURACY` ya es `[0,1]`). El raw es la media ponderada sobre las métricas **confident** de la sesión:

```
R = Σ_m (w_m · oriented(m)) / Σ_m w_m      (solo m con m.confident == true)
R = null   si no hay ninguna métrica confident      // honestidad: no fabricar
```

Los pesos `w_m` son del **perfil del ejercicio** (no globales): un ejercicio go/no-go pondera `FALSE_ALARM`/`IMPULSIVITY_IDX`; uno de calibración pondera `CALIBRATION_ERROR`. Esto respeta que cada paradigma define qué es rendimiento.

**2) Normalized score `Z` / percentil personal (0..100).** Estandarización **contra el propio baseline** del usuario para ese ejercicio, nunca contra una norma poblacional (boundary no-clínico). Sea `μ_b, σ_b` media y desviación del baseline rolling (ventana de las últimas `K` sesiones, mín. `n_baseline`):

```
z = (R - μ_b) / max(σ_b, σ_floor)          // σ_floor evita explosión con baseline plano
Z = 50 + 15 · clamp(z, -3.3, +3.3) / 3.3 · (50/15)   // mapea ±3.3σ → [0,100], 50 = tu baseline
Z = null   si n_baseline < umbral          // CALIBRATING, em-dash en UI
```

`Z=50` es **siempre** "tu nivel base", no un percentil de población. Esta es la semántica honesta heredada de `SpeedMacroCategory.CALIBRATING`.

**3) Reliability-adjusted score `R̂` (0..1).** Penaliza scores construidos sobre poca evidencia mediante un **factor de fiabilidad** `ρ∈[0,1]` derivado del tamaño muestral agregado y la consistencia interna (split-half sobre los trials del ejercicio, expuesta por el `MetricsEngine` del módulo cuando exista; si no, proxy por `n`):

```
ρ = ρ_n · ρ_consistency
ρ_n = n_eff / (n_eff + n_half)             // saturación: n_half = n al que ρ_n=0.5
ρ_consistency = clamp01(splitHalf_corrected)  // Spearman-Brown sobre split-half; 1.0 si no disponible
R̂ = μ_prior + ρ · (R - μ_prior)           // shrinkage Bayesiano hacia el prior del usuario
```

`μ_prior` = media del baseline (o 0.5 si frío). Con poca evidencia `R̂→μ_prior` (no afirma cambios); con mucha `R̂→R`.

**4) Improvement estimate `ΔÎ`.** Pendiente robusta de la serie de `R̂` recientes vs. baseline temprano, **no** delta de dos puntos (ruidoso). Regresión de Theil-Sen (mediana de pendientes pareadas, robusta a outliers) sobre `(t_i, R̂_i)` de las últimas `W` sesiones:

```
ΔÎ = median_{i<j} ( (R̂_j - R̂_i) / (t_j - t_i) )   · scaleToSessionUnits
ΔÎ = null   si W < 4   // se necesita serie, no dos puntos
```

Se reporta como "progreso contra tu propio baseline" con su intervalo (abajo), nunca como mejora clínica/IQ.

**5) Confidence interval del improvement.** Bootstrap por remuestreo de las sesiones de la ventana (`B≈1000` resamples de la serie `(t_i,R̂_i)`, recalculando Theil-Sen):

```
CI_95(ΔÎ) = [ percentile(boot, 2.5), percentile(boot, 97.5) ]
```

Si `CI_95` cruza 0 → la UI muestra "estable / sin cambio significativo", no una flecha de mejora. Esto evita afirmar progreso que el ruido no respalda.

**6) Fatigue penalty `φ` (0..1, multiplicativo).** El rendimiento bajo fatiga alta no debe inflar ni el score ni la dificultad. Derivado de señales objetivas (`RT_CV` creciente intra-sesión, deriva de `LAPSES`, caída de `ACCURACY` en el último tercio) combinadas con el `subjective` reportado, alineado con `FatigueIndex`/`FatigueLevel` de speed/focus:

```
fatigueLevel = wObj · objFatigue + wSubj · (subjective?.fatigue ?: objFatigue)   // ∈[0,1]
φ = 1 - λ_f · fatigueLevel               // λ_f ≈ 0.3: la penalización es acotada, no anula
R_session_reported = R̂ · φ
```

`φ` solo **modula el score reportado y la decisión adaptativa** (§4.3); el `R` crudo se conserva para el baseline (no contaminamos la serie histórica con la penalización, solo el snapshot mostrado).

**7) Consistency modifier `κ` (0..1).** Recompensa estabilidad inter-sesión (baja varianza de `R̂` reciente) frente a rendimiento errático:

```
κ = clamp01( 1 - σ(R̂_recent) / σ_ref )   // σ_ref = varianza esperable del paradigma
```

Se usa como **modificador de confianza**, no del score: alta `κ` sube `confidence` del `ModuleContribution` (acelera CALIBRATING→ESTABLISHED); baja `κ` mantiene `preliminary=true` más tiempo.

**8) Difficulty-adjusted performance `P_d` (0..1).** Acierto a mayor dificultad vale más. Rendimiento ajustado por la `NormalizedDifficulty` `d` a la que se logró:

```
P_d = R̂ · (1 + γ · (d - d_ref))          // d_ref = dificultad de baseline; γ ≈ 0.5
P_d = clamp01(P_d)
```

`P_d` es la señal que alimenta el `Elo-like skill estimate` (§4.3) y el `normalizedScore` del `ModuleContribution` hacia brain-profile, porque integra "qué tan difícil estaba" — evita que bajar la dificultad infle el score percibido.

### 4.2.3 Composición y flujo

```
trials crudos
  └─(MetricsEngine del módulo, autoritativo)→ métricas crudas + confident + split-half
       └─(adaptador del módulo)→ SessionSummary{NormalizedMetric[]}      [entra al core]
            └─ R → Z (vs baseline) ; R → R̂ (shrinkage por ρ) ; serie R̂ → ΔÎ → CI
                 └─ φ (fatiga) · R̂ → score reportado ; κ → confidence ; P_d → skill/brain-profile
```

**Gates de publicación de score** (todos deben cumplirse para salir de `preliminary`):

| Gate | Condición | Si falla |
|---|---|---|
| Confident | ≥1 métrica `confident` | score `null` (no se muestra barra) |
| Baseline | `n_baseline ≥ umbral` | `Z=null`, estado CALIBRATING |
| Reliability | `ρ ≥ ρ_min` | score visible pero `preliminary=true` |
| Improvement | `W ≥ 4` y `CI` no cruza 0 | "estable", sin claim de mejora |
| Fatigue | `fatigueLevel < f_cap` | score se reporta con asterisco "bajo fatiga" |

El `NormalizedMetric` con `confident=false` **nunca** entra en `R` (lección focus: no contar sobre listas de las que se filtró la señal).

## 4.3 Adaptación común

### 4.3.1 Estimadores de habilidad (capa de creencia)

El núcleo mantiene, por (usuario, ejercicio), **tres estimadores complementarios** que conviven; la máquina de decisión elige cuál domina según contexto:

- **Staircase (control local rápido).** Reusa el motor del módulo (`DefaultStaircaseEngine` CCE-driven en speed, `DefaultAdaptiveEngine` multi-eje en focus, `LogicDifficultyAdapter`, etc.) vía la interfaz neutral nueva `AdaptiveEngine<D>`:

```
interface AdaptiveEngine<D> { fun decide(input: AdaptiveInput): AdaptiveResult<D> }
```

  Política compartida elevada a core (hoy duplicada por módulo): **histéresis** (umbrales de subida ≠ bajada), **pasos asimétricos** (bajar más rápido que subir, asimetría tipo 1-up/2-down), **dead-zone** anti-oscilación, **gates de seguridad** (impulsividad/fatiga) con prioridad sobre la progresión.

- **Bayesian skill posterior (creencia global suave).** Posterior `θ ~ Beta(α,β)` sobre la probabilidad de acierto a dificultad de referencia, actualizado con cada trial: `α += hit, β += miss`, con **olvido exponencial** (`α,β *= decay`) para seguir no-estacionariedad (el usuario cambia). La media `θ̄=α/(α+β)` y su varianza dan dificultad objetivo + incertidumbre.

- **Elo-like rating (transferencia y matchmaking de ítems).** Cada **ítem/dificultad** tiene un rating `r_item`; el usuario tiene `r_user`. Tras un trial: `r_user += k·(outcome - E)`, `E = 1/(1+10^((r_item-r_user)/400))`. El `k` se modula por fiabilidad (`k = k0·(1-ρ)` no — al revés: `k` alto cuando incierto, decae al consolidar). Esto da una escala continua y comparable entre ejercicios del mismo módulo y alimenta `P_d`.

### 4.3.2 Ajuste consciente de volatilidad

Cuando la varianza de la creencia es alta (usuario nuevo, post-pausa, serie errática), el sistema **aprende rápido pero ajusta dificultad con prudencia**: paso de staircase reducido + `k` de Elo elevado + olvido Bayesiano mayor. Cuando la creencia está consolidada (baja varianza, alta `κ`), pasa a explotación: pasos normales, `k` bajo, olvido mínimo. Formalmente, un **factor de volatilidad** `v∈[0,1]` (de la varianza del posterior y de `σ(R̂_recent)`) escala simultáneamente:

```
stepSize     = step_base · (1 - β_v · v)      // menos salto si volátil
k_elo        = k_base · (1 + β_k · v)         // aprende más rápido si volátil
decay_bayes  = decay_base - β_d · v           // olvida más si volátil (sigue cambios)
```

### 4.3.3 Máquina de decisión meta (cuándo usar cada cosa)

Tabla de prioridad, evaluada en orden (la primera que dispara gana), espejo y generalización de la tabla de `DefaultAdaptiveEngine` (safety antes que progresión):

| Prio | Disparador | Decisión meta | Mecanismo |
|---|---|---|---|
| 1 | **Seguridad**: impulsividad alta / fatiga crítica / abandono | `StopSession` o `Cooldown` | gate duro; ignora progresión |
| 2 | **Fatiga moderada** sostenida (φ bajo varias sesiones) | `DeloadSession` | dificultad −1 nivel, volumen reducido, sin presión de tiempo |
| 3 | **Volatilidad alta** (usuario frío/post-pausa) | `Recalibrate` | staircase paso pequeño + Bayes olvido alto + Elo k alto |
| 4 | **Plateau** detectado (§4.3.4) | `Intervention` (challenge / axis-switch / transfer) | rompe el estancamiento |
| 5 | **Progreso sano** (`ΔÎ>0`, CI sin cruzar 0, no fatiga) | `Progress` (StepUp / SwitchAxis) | staircase normal + explotación |
| 6 | Estable dentro de dead-zone | `Hold` | mantiene dificultad, sigue midiendo |

La decisión meta selecciona **qué estimador domina**: `Recalibrate`→Bayes+Elo lideran; `Progress`/`Hold`→staircase local lidera; `Intervention`→planner (§4.3.4).

### 4.3.4 Detección de plateau y planificación de intervención (elevada de memory a core)

El único detector formal existente (`PlateauDetector` + `InterventionPlanner` + 9 `InterventionProtocol` + "golden rule" de 2 semanas) vive en memory; se **eleva a core** para alimentarse de la serie de `SessionSummary`/`R̂` de **cualquier** módulo:

- **`PlateauStatus`**: `None` | `Plateau` (pendiente `ΔÎ≈0` con CI estrecho ≥2 semanas / `N` sesiones) | `FalseImprovement` (sube `R` pero **baja `P_d`**: progresa por dificultad estancada, señal de mejora ilusoria). La distinción `FalseImprovement` exige el `difficulty-adjusted` `P_d` — sin él, un plateau enmascarado parece progreso.
- **Golden rule**: no declarar plateau antes de 2 semanas de datos estables (evita intervenir sobre ruido).
- **Intervención** (planner): elige entre **challenge session** (subir dificultad por encima del nivel cómodo para romper techo), **axis-switch** (cambiar el eje del `DifficultyVector` que está saturado), **transfer probe** (ítem isomorfo/transferencia para verificar si la habilidad generaliza o es item-specific), o **deload** si el plateau coincide con fatiga acumulada.

### 4.3.5 Transfer probes

Sondas insertadas periódicamente (no entrenadas, no cuentan para staircase) que miden si la habilidad **generaliza**: un ítem isomorfo del mismo constructo pero superficie distinta (regla focus 2026-06-15: la consistencia de transferencia solo vale `1.0` si **ambos** lados —entrenado e isomorfo— son correctos; cualquier fallo → `0.0`). Su resultado alimenta `FalseImprovement` (alto `R` + baja transferencia = mejora item-specific, no transferida) y el brain-profile (`confidence` de la `ModuleContribution` baja si no hay transferencia).

## 4.4 Sesiones inteligentes

Cada plantilla es un `SessionTemplate` (generalizado desde focus a core) compuesto de bloques `PlannedBlock`. Composición = lista de bloques (ejercicio + nº trials + dificultad objetivo); objetivo = qué señal optimiza; gates = condiciones de entrada/salida.

| Sesión | Duración | Composición | Objetivo | Gates |
|---|---|---|---|---|
| **Corta** | ~5 min | 1–2 bloques, 1 módulo, dificultad ≈ baseline | Adherencia / mantener racha | Entrada: siempre. Salida temprana si fatiga. No avanza staircase agresivo |
| **Estándar** | ~12 min | 3–4 bloques, 1–2 módulos, dificultad adaptativa | Entrenamiento principal con progresión | Entrada: `fatigueLevel<f_cap`. Aplica decisión meta completa |
| **Profunda** | ~25 min | 5–6 bloques, 2–3 módulos interleaved, incluye 1 transfer probe | Carga alta + medición de transferencia | Entrada: readiness OK (no fatiga base, racha estable). Aborta si φ cae |
| **Diagnóstico** | variable | barrido de dificultad por ejercicio, sin adaptación (staircase OFF) | Construir/refrescar baseline (`μ_b,σ_b`) | Entrada: baseline ausente/obsoleto. Output marca `preliminary` hasta `n_baseline` |
| **Transferencia** | ~10 min | bloques de ítems isomorfos no entrenados | Verificar generalización (alimenta `FalseImprovement`) | Entrada: solo si hay base entrenada del constructo. No cuenta para staircase |
| **Recuperación/fatiga** | ~6 min | 1–2 bloques baja demanda, dificultad −1, sin presión temporal | Deload activo, romper acumulación de fatiga | Entrada: disparada por Prio-2 (fatiga sostenida). Bloquea sesiones profundas el mismo día |
| **Creatividad** | ~10–15 min | bloques divergentes (rotación de categorías, analogías remotas) | Fluidez/flexibilidad; scoring por proceso, no por RT | Entrada: sin gate de fatiga RT (paradigma no cronométrico). Scoring honesto: sin "correcto" binario |
| **Mixta interleaved** | ~12–18 min | bloques alternados de ≥2 módulos/constructos (no bloqueado por tipo) | Interleaving para transferencia y anti-rutina | Entrada: requiere ≥2 módulos con baseline. Evita 2 bloques consecutivos del mismo constructo |

Notas de gating transversales: ninguna sesión avanza dificultad si la métrica que la justifica no es `confident`; las creativas usan scoring de proceso (no penalizan tiempo) — el motor de scoring las trata con `direction=NEUTRAL` en RT y pondera fluidez/originalidad/flexibilidad.

## 4.5 Motor de recomendación diaria

Generaliza `DefaultDailyPlanGenerator`/`TrainingCycle`/`RecoveryPolicy` de focus a un **`DailyRecommender`** de core que puntúa **candidatos** (cada candidato = (ejercicio, plantilla de sesión)) y devuelve un plan ordenado.

### 4.5.1 Señales de entrada (por candidato)

`recentProgress` (`ΔÎ`, CI), `fatigue` (`fatigueLevel` global y por módulo), `cognitiveGap` (déficit del `MindBoostProfile`: dimensión con `DimensionState` más débil/CALIBRATING), `preference` (frecuencia histórica de elección del usuario), `adherence` (riesgo de churn: días desde última sesión, longitud de racha), `plateau` (`PlateauStatus` del ejercicio), `transferNeed` (sesiones desde la última transfer probe), `frustrationRisk` (ratio de fallos recientes + dificultad relativa al skill).

### 4.5.2 Scoring de candidatos (pseudocódigo)

```
fun scoreCandidate(c: Candidate, ctx: UserContext): Double {
    // 1. Necesidad cognitiva: prioriza dimensiones débiles / en calibración (weakness-first)
    val gap = ctx.profile.gapFor(c.dimension)            // 0..1, alto = más débil
    val gapTerm = W_GAP * gap

    // 2. Plateau: un ejercicio estancado necesita intervención -> sube prioridad si hay margen
    val plateauTerm = when (c.plateau) {
        Plateau          -> W_PLATEAU                     // empuja a challenge/axis-switch
        FalseImprovement -> W_PLATEAU * 1.3               // urgente: mejora ilusoria
        None             -> 0.0
    }

    // 3. Transferencia: deuda de transfer probes
    val transferTerm = W_TRANSFER * clamp01(ctx.sessionsSinceProbe(c) / PROBE_PERIOD)

    // 4. Adherencia / anti-churn: si racha en riesgo, favorece sesión CORTA y preferida
    val adherenceTerm = if (ctx.streakAtRisk()) {
        W_ADHERENCE * (if (c.template == SHORT) 1.0 else 0.3) * ctx.preference(c)
    } else 0.0

    // 5. Preferencia explícita/implícita (suave, no domina)
    val prefTerm = W_PREF * ctx.preference(c)

    // ---- Penalizaciones (multiplicativas / restas) ----

    // 6. Fatiga: bloquea/penaliza sesiones demandantes si fatiga alta
    val fatiguePenalty = W_FATIGUE * ctx.fatigueLevel * demand(c.template)

    // 7. Riesgo de frustración: dificultad muy por encima del skill o racha de fallos
    val frustrationPenalty = W_FRUST * frustrationRisk(c, ctx)

    // 8. Saciedad/recencia: penaliza repetir el mismo constructo hoy (favorece interleaving)
    val recencyPenalty = W_RECENCY * ctx.timesPlayedToday(c.construct)

    val base = gapTerm + plateauTerm + transferTerm + adherenceTerm + prefTerm
    return base - fatiguePenalty - frustrationPenalty - recencyPenalty
}

fun recommendDaily(ctx: UserContext): DailyPlan {
    // Hard gates (orden de la máquina de decisión meta §4.3.3):
    if (ctx.safetyStop())   return DailyPlan.restDay(reason = SAFETY)         // Prio 1
    if (ctx.fatigueSustained()) return DailyPlan.recoveryOnly()              // Prio 2 -> deload

    val candidates = enumerate(exercises × eligibleTemplates(ctx))
        .filter { it.baselineReadyOrDiagnostic() }       // no adaptar sin baseline
        .map { it to scoreCandidate(it, ctx) }
        .sortedByDescending { it.second }

    // Construye el plan respetando presupuesto temporal y diversidad (interleaving)
    val plan = mutableListOf<Candidate>()
    var budget = ctx.timeBudgetMin
    for ((cand, _) in candidates) {
        if (budget <= 0) break
        if (plan.violatesInterleaving(cand)) continue      // no 2 mismo constructo seguidos
        if (cand.durationMin > budget) continue
        plan.add(cand); budget -= cand.durationMin
    }
    // Garantiza al menos 1 transfer probe si hay deuda y presupuesto profundo
    plan.ensureTransferProbeIfDue(ctx)
    return DailyPlan(plan, periodization = ctx.cycle.phase)  // deload/challenge según ciclo
}
```

### 4.5.3 Periodización envolvente

El recommender opera dentro de un `TrainingCycle` (microciclo): alterna fases de **acumulación** (volumen/dificultad creciente), **challenge** (semana de techo, más sesiones profundas + probes) y **deload** (recuperación programada cada N microciclos, independientemente de la fatiga puntual). La fase del ciclo modula los pesos `W_*`: en deload sube `W_FATIGUE` y baja `W_PLATEAU`; en challenge sube `W_PLATEAU`/`W_TRANSFER`. Así la recomendación diaria es localmente reactiva (fatiga/frustración del día) pero globalmente periodizada (evita sobreentrenamiento y mesetas estructurales).

---

**Notas de integración con el repo (no normativas):** el núcleo `mindboost/core/scoring`+`mindboost/core/adaptive` es **nuevo** y aditivo; no toca los engines por-módulo. Requiere (a) extraer `SecureKeyValueStore` de `focus.data.local` a `mindboost/storage` para que el núcleo persista series de `SessionSummary` sin acoplarse a focus; (b) un adaptador por módulo `SessionSummarySource` análogo a `CognitiveModuleProfileSource` (one-way, solo superficies públicas); (c) `NormalizedDifficulty`/`NormalizedMetric` como única frontera de tipos cross-módulo; (d) todo timestamp vía `EpochClock`. Persistencia de baselines/series en namespace propio (extensión de `MindBoostStorageNamespaces`, `AccountBound` para borrado de cuenta). Honestidad de tipos preservada: `null`=no computable, `preliminary=true` por defecto, scoring vs baseline propio (no poblacional), copy no-clínico.

---

# 5. Modelos KMP sugeridos

> **Objetivo:** un módulo **neutral** `com.mindos.app.features.mindboost.core.exercise` (+ `core.scoring`, `core.adaptive`) que provea el contrato común que hoy falta, **sin reescribir** los motores por módulo. Los tipos específicos (`focus.TrialGenerator`, `emotions.EmotionTrialGenerator`, `speed.TrialEngine`, `ReasoningScorer`, `BayesEngine`, `DefaultMetricsEngine`…) **siguen siendo la autoridad**; el core solo define genéricos parametrizados (`Stimulus`/`Response`) y _adaptadores one-way_ análogos a `CognitiveModuleProfileSource`. Convención de convivencia: **el core no calcula nada que un motor de módulo ya calcula** — lo normaliza.

## 5.1 Núcleo de ejercicio: `core.exercise`

```kotlin
package com.mindos.app.features.mindboost.core.exercise

import com.mindos.app.core.time.EpochClock

/** Identidad estable de un ejercicio. Reusa el patrón ExerciseId de focus,
 *  elevado a core para que el registry/renderer de cualquier módulo lo comparta. */
@JvmInline
value class ExerciseId(val raw: String) {
    init { require(raw.isNotBlank()) { "ExerciseId must not be blank" } }
}

@JvmInline
value class ModuleId(val raw: String) {
    init { require(raw.isNotBlank()) { "ModuleId must not be blank" } }
}

/** Paradigma de respuesta. El renderer y el classifier despachan sobre esto,
 *  igual que focus despacha go-no-go / choice / stop hoy. NO añade paradigmas
 *  que un módulo no soporte: es el conjunto unión observado en el repo. */
enum class ResponseParadigm {
    SINGLE_TAP,      // go/no-go, detección (focus E1/E3/E10)
    TWO_CHOICE,      // flanker, Stroop, task-switch (focus E2/E5/E6)
    STOP_SIGNAL,     // freno (focus E4)
    SPATIAL_PROBE,   // MOT (focus E9)
    TEXT_CHOICE,     // banco de ítems (logic, language, emotions EM5)
    FREEFORM_TEXT,   // creativity
    SELF_REPORT      // check-in subjetivo (decisiones, emotions)
}

/**
 * Definición declarativa de un ejercicio. Es el "schema" estático: un módulo lo
 * registra una vez. NO contiene lógica de generación (eso lo hace TrialGenerator).
 * Análogo a focus.ExerciseDefinition, elevado y parametrizado por módulo.
 */
data class ExerciseDefinition(
    val id: ExerciseId,
    val moduleId: ModuleId,
    val displayKey: String,            // clave AppStrings, NUNCA copy hardcoded
    val paradigm: ResponseParadigm,
    val primaryDimension: CognitiveDimension, // reusa profile.domain.CognitiveDimension
    val difficultyAxes: List<DifficultyAxisId>,
    val domainReady: Boolean,          // dominio testeado
    val implemented: Boolean,          // alcanzable end-to-end (registry+renderer)
) {
    init {
        require(displayKey.isNotBlank())
        // invariante: no se puede exponer (implemented) lo que no está domain-ready.
        require(!implemented || domainReady) {
            "Exercise ${id.raw} cannot be implemented without domainReady"
        }
    }
}
```

## 5.2 Estímulo, respuesta y trial genérico

```kotlin
package com.mindos.app.features.mindboost.core.exercise

/**
 * Marcadores de estímulo. Cada módulo define su payload concreto (focus.Stimulus,
 * speed.SpeedTrialPrompt, emotions.*). Stimulus<T> envuelve ese payload con los
 * metadatos comunes que el assembler/clasificador genérico necesita SIN conocer T.
 */
data class Stimulus<out TPayload>(
    val index: Int,                    // orden en el bloque (seed estable, focus pattern)
    val isTarget: Boolean,             // semántica go/no-go cuando aplica; default por paradigma
    val expectedResponse: Response?,   // null si la corrección depende del payload (task-switch)
    val payload: TPayload,
)

/** Respuesta esperada/correcta normalizada. Es la cara "neutra" de ResponseKey de focus. */
sealed interface Response {
    data object Withhold : Response          // no responder (no-go, stop exitoso)
    data object Primary : Response           // tap único
    data object Left : Response
    data object Right : Response
    data class Choice(val optionId: String) : Response   // banco de ítems / TEXT_CHOICE
    data class Text(val value: String) : Response        // freeform (creativity)
    data class SelfReport(val scaleValue: Int) : Response
}

/**
 * Respuesta cruda del usuario (lo que el renderer capturó). Sealed para que el
 * `when` sea exhaustivo y el Detekt ratchet no se rompa por ramas implícitas.
 * Convive con focus.UserResponse: cada módulo mapea su captura a esto en el borde.
 */
sealed interface UserResponse {
    /** ms relativos al onset del estímulo (EpochClock-derived); null = sin respuesta. */
    val latencyMs: Long?

    data class Tapped(override val latencyMs: Long, val response: Response) : UserResponse
    data object NoResponse : UserResponse { override val latencyMs: Long? = null }
    data class Submitted(override val latencyMs: Long, val response: Response) : UserResponse
}

/** Un trial = estímulo + lo que el usuario hizo. NO contiene el veredicto (eso es TrialResult). */
data class Trial<out TPayload>(
    val stimulus: Stimulus<TPayload>,
    val userResponse: UserResponse,
    val onsetEpochMs: Long,            // de EpochClock.nowMillis(), nunca Clock.System directo
)

/** Veredicto de un trial. La AUTORIDAD de clasificación sigue siendo el assembler del
 *  módulo (FocusSessionAssembler, EmotionSessionAssembler…); este tipo es solo el
 *  resultado normalizado que esos assemblers pueden EMITIR para alimentar al core. */
data class TrialResult(
    val outcome: TrialOutcome,
    val latencyMs: Long?,              // RT del acierto, si lo hubo
    val markers: Set<TrialMarker> = emptySet(),
)

enum class TrialOutcome {
    HIT, CORRECT_REJECTION,            // aciertos
    MISS, FALSE_ALARM, WRONG_CHOICE,  // errores
    ANTICIPATION, LAPSE,              // extremos (se cuentan sobre crudos, ver aprendizaje 2026-06-04)
    NO_RESPONSE,
}

/** Eventos cualitativos por trial (impulsividad, switch, congruencia…). */
enum class TrialMarker {
    IMPULSIVE, SWITCH_TRIAL, CONGRUENT, INCONGRUENT, STOP_TRIAL, ISOMORPH_TRANSFER
}
```

## 5.3 Generación: `TrialGenerator` común (determinista, seeded)

```kotlin
package com.mindos.app.features.mindboost.core.exercise

/**
 * Contrato de generación común. focus.TrialGenerator ya tiene esta forma exacta
 * (exerciseId + generate(DifficultyVector, BlockSpec, seed) -> List<Stimulus>);
 * lo elevamos a core para que speed/memory/etc puedan compartir el registry+test
 * de consistencia. Determinista: misma (spec, seed) => misma lista (regla del repo).
 */
interface TrialGenerator<out TPayload> {
    val exerciseId: ExerciseId
    fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus<TPayload>>
}

/** Parámetros estructurales de un bloque (longitud, proporción de targets…). */
data class BlockSpec(
    val trialCount: Int,
    val targetRatio: Double,           // 0..1
    val responseWindowMs: Long,
    val interTrialIntervalMs: Long,
) {
    init {
        require(trialCount > 0)
        require(targetRatio in 0.0..1.0)
        require(responseWindowMs > 0)
    }
}

/**
 * Registry id->generador, ELEVADO del patrón focus.FocusExerciseGenerators.registry.
 * Única fuente de verdad de "lo wired" por módulo. Un ConsistencyTest por módulo
 * (análogo a ExerciseCatalogConsistencyTest) ata implemented <=> registry.keys.
 */
interface ExerciseRegistry {
    val moduleId: ModuleId
    val generators: Map<ExerciseId, TrialGenerator<*>>
    val definitions: Map<ExerciseId, ExerciseDefinition>

    fun generatorFor(id: ExerciseId): TrialGenerator<*>? = generators[id]
    fun definitionFor(id: ExerciseId): ExerciseDefinition? = definitions[id]

    /** Invariante verificable en test: lo implementado debe estar wired y al revés. */
    fun implementedMatchesRegistry(): Boolean =
        definitions.filterValues { it.implemented }.keys == generators.keys
}
```

## 5.4 Estado de UI y sesión

```kotlin
package com.mindos.app.features.mindboost.core.exercise

/**
 * Estado observable del runner de ejercicio. Sealed => `when` exhaustivo en el
 * renderer (regla del repo: nuevo estado obliga a actualizar todos los `when`).
 * NO es el FocusUiState completo: es el contrato mínimo común; cada módulo puede
 * envolverlo o extenderlo con su propio FocusFieldState/EmotionFieldState.
 */
sealed interface ExerciseState<out TPayload> {
    data object Idle : ExerciseState<Nothing>
    data class Instructions(val exerciseId: ExerciseId) : ExerciseState<Nothing>
    data class Presenting<TPayload>(
        val trialIndex: Int,
        val total: Int,
        val stimulus: Stimulus<TPayload>,
    ) : ExerciseState<TPayload>
    data class AwaitingResponse<TPayload>(
        val stimulus: Stimulus<TPayload>,
        val onsetEpochMs: Long,
    ) : ExerciseState<TPayload>
    data class BlockBreak(val completed: Int, val remaining: Int) : ExerciseState<Nothing>
    data class Finished(val summary: SessionSummary) : ExerciseState<Nothing>
}

/**
 * Resumen de sesión NORMALIZADO y persistible. Es lo único que el core sabe agregar
 * de forma cross-módulo. SOLO summaries (regla del repo: nunca trials crudos).
 * Cada módulo emite su SessionResult rico (focus.SessionResult, speed.SessionMetrics)
 * y produce ESTE summary plano en el borde, vía su assembler.
 */
data class SessionSummary(
    val schemaVersion: Int,            // == CoreExerciseSchema.CURRENT al persistir
    val moduleId: ModuleId,
    val exerciseId: ExerciseId,
    val userId: String,
    val startedAtMs: Long,
    val endedAtMs: Long,               // key user-scoped = "userId:endedAtMs" (patrón Local* repo)
    val difficulty: DifficultyVector,
    val metrics: NormalizedMetricSet,  // ver 5.6
    val score: ScoreResult?,           // null si no confident (nunca 0 silencioso)
    val subjective: SubjectiveState?,
)

/** Estado subjetivo común (esfuerzo, fatiga percibida…). Reusa la forma de focus.SubjectiveState. */
data class SubjectiveState(
    val perceivedEffort: Int?,         // 1..5, null si no preguntado
    val perceivedFatigue: Int?,
)

/** Snapshot de una sesión, agnóstico del módulo, para el runner genérico. */
data class ExerciseSession<out TPayload>(
    val exerciseId: ExerciseId,
    val moduleId: ModuleId,
    val spec: BlockSpec,
    val trials: List<Trial<TPayload>>,
    val startedAtMs: Long,
)
```

## 5.5 Dificultad multi-eje común

```kotlin
package com.mindos.app.features.mindboost.core.adaptive

/** Identidad de eje. Eleva focus.DifficultyAxis a core; cada módulo declara SOLO
 *  los ejes que usa (speed: 1 eje deadline; focus: varios). */
@JvmInline
value class DifficultyAxisId(val raw: String) {
    init { require(raw.isNotBlank()) }
}

/**
 * Vector de dificultad multi-eje normalizado (cada eje 0..1). Generaliza
 * focus.DifficultyVector; speed lo usa con un único eje (CCE/deadline) sin perder
 * generalidad. Inmutable: las transiciones devuelven copia (regla de inmutabilidad).
 */
data class DifficultyVector(
    val axes: Map<DifficultyAxisId, Double>,
) {
    init {
        require(axes.values.all { it in 0.0..1.0 }) { "All axes must be normalized 0..1" }
    }

    fun stepped(axis: DifficultyAxisId, delta: Double): DifficultyVector {
        val current = axes[axis] ?: 0.0
        val next = (current + delta).coerceIn(0.0, 1.0)
        return copy(axes = axes + (axis to next))
    }

    fun valueOf(axis: DifficultyAxisId): Double = axes[axis] ?: 0.0

    companion object { val EMPTY = DifficultyVector(emptyMap()) }
}
```

## 5.6 Métricas normalizadas (puente común)

```kotlin
package com.mindos.app.features.mindboost.core.exercise

/** Clave de métrica neutra. NO sustituye a focus.MetricKey/emotions.EmotionMetricSet:
 *  es el denominador común que los assemblers EMITEN para que el core agregue.
 *  El nombre crudo permite a cada módulo aportar métricas propias sin un enum cerrado. */
@JvmInline
value class MetricKey(val raw: String) {
    init { require(raw.isNotBlank()) }
}

/**
 * Valor de métrica con incertidumbre explícita. Modela MetricValueDto de focus
 * (value + n + confident) elevado a core: 'confident=false' => no se puntúa ni se
 * agrega (aprendizaje 2026-06-04: gating por confident, no por n directamente).
 */
data class MetricValue(
    val value: Double?,                // null => no computable
    val n: Int,                        // tamaño de muestra
    val confident: Boolean,            // gate de scoring/agregación
)

/** Bolsa de métricas normalizadas de una sesión. */
data class NormalizedMetricSet(
    val values: Map<MetricKey, MetricValue>,
) {
    fun confidentValue(key: MetricKey): Double? =
        values[key]?.takeIf { it.confident }?.value

    companion object { val EMPTY = NormalizedMetricSet(emptyMap()) }
}
```

## 5.7 Scoring común (interfaz, no implementación)

```kotlin
package com.mindos.app.features.mindboost.core.scoring

import com.mindos.app.features.mindboost.core.exercise.NormalizedMetricSet

/**
 * Resultado de score NORMALIZADO y HONESTO. Eleva la convención ya compartida
 * (focus.FocusScore, speed.CognitiveProfileScore): barras null = no computable
 * (nunca 0 silencioso); preliminary=true por defecto; macro descriptiva no clínica.
 */
data class ScoreResult(
    val value: Int?,                   // 0..100 o null si !confident
    val macroCategory: String,         // descriptivo no-clínico (incl. "CALIBRATING")
    val preliminary: Boolean = true,
    val contributing: Map<String, Double?> = emptyMap(), // sub-barras, nullable
) {
    init { require(value == null || value in 0..100) }
}

/**
 * Motor de scoring común. Los motores reales (ReasoningScorer, FocusScoreCalculator,
 * SpeedScoreEngine, BayesEngine…) NO implementan esto directamente para no perder su
 * API rica; se exponen al core mediante un thin ADAPTER por módulo (patrón
 * CognitiveModuleProfileSource). Así el core puntúa de forma uniforme cuando hace
 * falta una vista común, y el motor del módulo sigue siendo la autoridad interna.
 */
fun interface ScoringEngine {
    /** Debe devolver value=null cuando las métricas clave no son confident. */
    fun score(metrics: NormalizedMetricSet, difficulty: com.mindos.app.features.mindboost.core.adaptive.DifficultyVector): ScoreResult
}
```

## 5.8 Adaptación común (interfaz + señales)

```kotlin
package com.mindos.app.features.mindboost.core.adaptive

import com.mindos.app.features.mindboost.core.exercise.NormalizedMetricSet
import com.mindos.app.features.mindboost.core.exercise.SessionSummary

/**
 * Política de adaptación común. Generaliza focus.DefaultAdaptiveEngine (multi-eje)
 * y speed.StaircaseEngine (1D) bajo una firma única decide(input)->decision.
 * Las implementaciones por módulo siguen existiendo; pueden implementar esta
 * interfaz O ser envueltas por un adapter. La PRIORIDAD seguridad-antes-de-progresión
 * y la anti-oscilación (histéresis, pasos asimétricos, dead-zone) son contrato, no opción.
 */
fun interface AdaptationPolicy {
    fun decide(input: AdaptiveInput): AdaptiveDecision
}

/** Entrada de decisión: estado actual + señales normalizadas. */
data class AdaptiveInput(
    val current: DifficultyVector,
    val lastSummary: SessionSummary,
    val recentMetrics: NormalizedMetricSet,
    val fatigue: FatigueSignal,
    val transfer: TransferSignal?,
    val history: List<AdaptationSignal> = emptyList(),  // serie para histéresis/plateau
)

/**
 * Decisión sellada (exhaustiva). Une la tabla de focus (StepUp/StepDown/Hold/SwitchAxis/
 * Cooldown/StopSession/ContainImpulsivity) con las directivas de speed (RAISE/LOWER/HOLD/
 * STABILIZE/COOLDOWN) en un vocabulario común. SAFETY antes que PROGRESSION por orden de ramas.
 */
sealed interface AdaptiveDecision {
    // --- safety (prioridad alta) ---
    data class StopSession(val reason: String) : AdaptiveDecision
    data class Cooldown(val blocks: Int) : AdaptiveDecision
    data object ContainImpulsivity : AdaptiveDecision
    data class StepDown(val axis: DifficultyAxisId, val delta: Double) : AdaptiveDecision
    // --- progresión ---
    data object Hold : AdaptiveDecision
    data class Stabilize(val axis: DifficultyAxisId) : AdaptiveDecision
    data class StepUp(val axis: DifficultyAxisId, val delta: Double) : AdaptiveDecision
    data class SwitchAxis(val from: DifficultyAxisId, val to: DifficultyAxisId) : AdaptiveDecision
}

/** Señal de adaptación serializable para reconstruir la serie (histéresis/plateau). */
data class AdaptationSignal(
    val atMs: Long,
    val axis: DifficultyAxisId,
    val direction: Int,                // -1 down / 0 hold / +1 up
    val confident: Boolean,
)

/** Fatiga normalizada (generaliza speed.FatigueIndex / focus.FatigueLevel). */
data class FatigueSignal(
    val level: FatigueLevel,
    val index: Double?,                // 0..1, null si no estimable
) {
    init { require(index == null || index in 0.0..1.0) }
}

enum class FatigueLevel { LOW, MODERATE, HIGH, UNKNOWN }

/**
 * Señal de transferencia (isomorfos / cross-task). Eleva la lógica de logic:
 * solo vale 1.0 cuando AMBOS lados (entrenado + isomorfo) son correctos
 * (aprendizaje 2026-06-15). El core NO la calcula: la recibe del módulo.
 */
data class TransferSignal(
    val consistency: Double,           // 0..1
    val bothSidesObserved: Boolean,
) {
    init { require(consistency in 0.0..1.0) }
}
```

## 5.9 Persistencia común: schema + namespace

```kotlin
package com.mindos.app.features.mindboost.core.exercise

/**
 * Schema versionado del core, espejo de focus.FocusSchema{CURRENT=1}. Cada
 * SessionSummary persistido lleva schemaVersion = CoreExerciseSchema.CURRENT.
 * (Json compartido reusa FocusJson: ignoreUnknownKeys=true; encodeDefaults=true.)
 */
object CoreExerciseSchema { const val CURRENT: Int = 1 }
```

> **Nota de persistencia:** la superficie cripto (`SecureKeyValueStore`, `readVersioned`, `InMemorySecureKeyValueStore`) hoy vive en `focus.data.local`. **Hueco identificado:** debería extraerse a `mindboost/storage` (o este `core`) para que `SessionSummary` se persista sin que cada módulo dependa de `focus`. Mientras no se extraiga, los repos `Local*` siguen importándola desde `focus.data.local` y persisten **solo** `SessionSummary` (DTO `@Serializable` con `schemaVersion`), key user-scoped `"userId:endedAtMs"`, en su propio `MindBoostStorageNamespaces.X`.

---

## 5.10 Convivencia con los motores específicos (sin romper el ratchet Detekt)

| Tipo del core | Motor de módulo (autoridad real) | Cómo conviven |
|---|---|---|
| `TrialGenerator<TPayload>` | `focus.TrialGenerator`, `speed.TrialEngine`, `emotions.EmotionTrialGenerator` | focus ya tiene la firma; los demás implementan el core o se envuelven con un adapter delgado. El `ExerciseRegistry` por módulo reemplaza al `FocusExerciseGenerators.registry` hardcoded. |
| `TrialResult` / `TrialOutcome` | `FocusSessionAssembler`, `EmotionSessionAssembler`, `TrialClassifier`/`classifyStop`/`classifyChoice` | El assembler **sigue siendo la autoridad de clasificación**; solo **emite** `TrialResult` normalizado al final, además de su `BlockResult` rico interno. |
| `NormalizedMetricSet` / `MetricValue` | `DefaultMetricsEngine`+`MetricKey`, `EmotionMetricSet`, `speed.SessionMetrics`/`BlockMetrics` | Cada `MetricsEngine` calcula igual que hoy; un mapper `data/mapper/` aplana su resultado a `NormalizedMetricSet`. El core nunca recalcula métricas. |
| `ScoringEngine` / `ScoreResult` | `ReasoningScorer`, `FocusScoreCalculator`, `SpeedScoreEngine`, `BayesEngine` | Motores intactos; un `*ScoreAdapter : ScoringEngine` por módulo traduce. `BayesEngine` (decisiones) expone su salida vía adapter, sin que el core conozca Bayes. |
| `AdaptationPolicy` / `AdaptiveDecision` | `DefaultAdaptiveEngine`, `DefaultStaircaseEngine`/`MultiSignalStaircaseEngine`, `LogicDifficultyAdapter`, `AdaptationEngine` | Cada motor implementa `AdaptationPolicy` **o** se envuelve. El `sealed AdaptiveDecision` unifica el vocabulario focus+speed; las políticas (histéresis, gates de fatiga/impulsividad) se vuelven reutilizables. |
| `PlateauDetector` (hoy solo `memory`) | `memory.PlateauDetector` + `InterventionPlanner` | Se eleva a `core.adaptive` alimentándose de la serie de `SessionSummary`/`AdaptationSignal` de cualquier módulo; `memory` deja de ser el único con plateau formal. |
| `ModuleContribution` (ya compartido) | adapters en `profile/data/adapter/` | **Sin cambios**: el core de ejercicio alimenta hacia el brain-profile por el mismo contrato one-way `CognitiveModuleProfileSource`. |

**Reglas anti-ratchet (Detekt) para los archivos nuevos del core:**

- **`MagicNumber`:** los umbrales (deltas de staircase, dead-zone, ratios) se declaran como `const val`/`companion object` con nombre de dominio (`STEP_UP_DELTA`, `DEAD_ZONE`), nunca literales sueltos.
- **`ComplexCondition` (≥4 operandos):** factorizar a pertenencia en colección (`axis in PROGRESSION_AXES`) o a funciones predicado — patrón ya forzado por el repo (`DASH_POSITIONS`).
- **`MatchingDeclarationName`:** todo archivo con una sola declaración top-level se nombra igual que ella (`AdaptationPolicy.kt`, `ScoringEngine.kt`); si agrupa varias (`Stimulus.kt` con `Stimulus`/`Response`/`UserResponse`), no aplica.
- **`ReturnCount` (≤2):** las ramas de `decide(...)` se parten en funciones (`safetyDecision()` antes de `progressionDecision()`), nunca múltiples `return` en un método — igual que el split de `TrialClassifier`.
- **`FunctionNaming` / `LongMethod` / `MagicNumber` en Composables:** los renderers del runner común usan `@file:Suppress(...)` **localizado** (no baseline regenerado).
- **Tests de consistencia (`commonTest`):** un `CoreExerciseRegistryConsistencyTest` por módulo ata `implemented ⇔ registry.keys` (espejo de `ExerciseCatalogConsistencyTest`); los tests IO/pesados van a `desktopTest` (no lo escanea Detekt).
- **Terminología:** ningún símbolo del core usa lexemas prohibidos por `arenaTerminologyScanner` (clinical/diagnostic/IQ…), ni en compuestos con guion — `macroCategory` es descriptivo funcional.

---

# 6. Ranking brutal de prioridad

**Metodologia de puntuacion compuesta.** Score = (Impacto x 2.5) + (Robustez x 2.0) + (Rejugabilidad x 1.5) + (Facilidad x 1.0) - (Fatiga x 1.0). Se prioriza el valor cognitivo demostrable (impacto + robustez) por encima de la conveniencia de ingenieria; la rejugabilidad protege la curva de retencion; la fatiga penaliza por encima de la facilidad porque un ejercicio fatigante contamina la medida y degrada la adherencia. Empates se rompen por prioridad MVP declarada y luego por robustez.

| Rank | Ejercicio | Modulo | Impacto esperado | Facilidad de implementacion | Robustez metrica | Rejugabilidad | Riesgo de fatiga | Prioridad MVP | Motor principal conectado |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Evidence Lock Pro | MindBoost Lenguaje | 5 | 5 | 5 | 4 | 2 | P0 | ScoringEngine |
| 2 | Scope & Negation Drill | MindBoost Lenguaje | 5 | 4 | 5 | 4 | 2 | P0 | ScoringEngine |
| 3 | Necesario vs Suficiente | MindBoost Logica | 5 | 5 | 4 | 3 | 2 | P0 | ReasoningScorer/ICR |
| 4 | Transferencia Espejo | MindBoost Logica | 5 | 2 | 5 | 4 | 2 | P1 | ReasoningScorer (transferConsistencyScore) |
| 5 | Cadena Deductiva Progresiva | MindBoost Logica | 5 | 4 | 4 | 3 | 3 | P0 | ReasoningScorer/ICR |
| 6 | E3 — Forecast Calibrado: Apuesta tu Confianza | MindBoost Decisiones | 5 | 3 | 4 | 4 | 2 | P0 | ScoringEquations |
| 7 | E1 — Bayes Arena: Calibrador de Posterior Continuo | MindBoost Decisiones | 5 | 3 | 4 | 4 | 3 | P0 | BayesEngine |
| 8 | Silogismo Categorico (anti sesgo de creencia) | MindBoost Logica | 5 | 4 | 3 | 4 | 2 | P1 | ReasoningScorer/ICR |
| 9 | Calibrated Comprehension | MindBoost Lenguaje | 5 | 3 | 3 | 4 | 2 | P1 | ScoringEngine |
| 10 | Vigía Probabilístico | MindBoost Enfoque | 4 | 5 | 4 | 4 | 4 | P0 | DefaultMetricsEngine |
| 11 | Filtro de Distracción | MindBoost Enfoque | 4 | 5 | 4 | 4 | 2 | P0 | DefaultMetricsEngine |
| 12 | Detección de lo Inverosímil | MindBoost Creatividad | 5 | 4 | 4 | 4 | 2 | P0 | CreativityRubricScorer |
| 13 | EM-B Reentrada tras Error | MindBoost Emociones | 4 | 5 | 4 | 4 | 2 | P0 | DefaultEmotionMetricsEngine |
| 14 | Caza-Contraejemplo | MindBoost Logica | 5 | 3 | 4 | 3 | 3 | P1 | ReasoningScorer/ICR + CounterexampleValidator |
| 15 | Sustitución Símbolo-Dígito Rápida (Cifrado Veloz) | MindBoost Velocidad | 4 | 4 | 4 | 4 | 2 | P0 | MultiSignalStaircaseEngine |
| 16 | Compuerta Lógica Express (Go/No-Go Veloz Asimétrico) | MindBoost Velocidad | 4 | 4 | 4 | 4 | 3 | P0 | MultiSignalStaircaseEngine |
| 17 | Freno Adaptativo | MindBoost Enfoque | 5 | 4 | 4 | 3 | 3 | P0 | SsrtCalculator |
| 18 | Conmutador de Reglas | MindBoost Enfoque | 4 | 5 | 4 | 4 | 3 | P1 | DefaultMetricsEngine |
| 19 | Cazador Visual | MindBoost Enfoque | 4 | 4 | 4 | 4 | 3 | P1 | DefaultMetricsEngine |
| 20 | Discriminación Cromática Binaria (Tinte Crítico) | MindBoost Velocidad | 3 | 5 | 4 | 4 | 2 | P0 | MultiSignalStaircaseEngine |
| 21 | E2 — Triaje de Reversibilidad | MindBoost Decisiones | 4 | 4 | 4 | 4 | 2 | P0 | ReversibilityEngine |
| 22 | Resolución de Conflicto Veloz (Flanker Express) | MindBoost Velocidad | 4 | 4 | 4 | 3 | 3 | P1 | MultiSignalStaircaseEngine |
| 23 | EM-G Estabilidad bajo Interferencia Afectiva | MindBoost Emociones | 4 | 4 | 4 | 4 | 3 | P1 | DefaultEmotionMetricsEngine |
| 24 | Semantic Surgery | MindBoost Lenguaje | 4 | 3 | 4 | 4 | 2 | P1 | ScoringEngine |
| 25 | E8 — Resolucion bajo Ambiguedad: Valor de la Informacion | MindBoost Decisiones | 5 | 2 | 4 | 4 | 3 | P1 | BayesEngine |

## Top 5 para implementar primero

1. **Evidence Lock Pro (Lenguaje).** El unico ejercicio con 5/5 en impacto, robustez Y facilidad. El `ScoringEngine` con `NearTransferVerifier` y penalizacion de over-claiming ya esta cableado; el scoring de seleccion-exacta-de-evidencias es discreto, auditable y libre de jueces de IA. Coste de fatiga minimo (2). Es el ancla MVP: maxima señal por unidad de esfuerzo de ingenieria.
2. **Scope & Negation Drill (Lenguaje).** Robustez 5/5: las inferencias bajo negacion/cuantificadores/conectores tienen verdad logica formal, no opinion, asi que la clave es deterministica y el ratchet anti-regresion es trivial. Mide razonamiento composicional, nucleo de transfer lejano. Reusa la infra de Evidence Lock (mismo motor, mismos gates de fairness/fatiga).
3. **Necesario vs Suficiente (Logica).** Facilidad 5/5 con impacto 5/5: combate la confusion bicondicional canonica, un error de razonamiento universal y medible con 4 clases discretas (necesaria/suficiente/ambas/ninguna). El `ReasoningScorer/ICR` ya existe; el generador es combinatorio barato. Baja fatiga (2) lo hace apto para bloques largos.
4. **E3 — Forecast Calibrado (Decisiones).** Introduce proper scoring (Brier) al MVP, una metrica de calibracion psicometricamente robusta y dificil de gamear. Habilita la dimension confianza-acierto que despues reutilizan Calibrated Comprehension (Lenguaje) y EM-E (Emociones): inversion de infra con retorno transversal. Facilidad 3 por la UI de dial de confianza, pero el motor `ScoringEquations` ya esta.
5. **Vigía Probabilístico (Enfoque).** El paradigma de atencion sostenida de referencia (d′, criterio c, decremento de vigilancia), single-tap, facilidad 5/5, todos los motores (`DefaultMetricsEngine`, `AdaptiveEngine`, `personalization/*`) ya integrados. Aporta la cobertura del dominio atencional que ningun ejercicio de Lenguaje/Logica cubre, indispensable para un perfil cognitivo creible desde el dia 1.

## Top 5 para diferenciar MindBoost de cualquier competidor

1. **Transferencia Espejo (Logica).** El diferenciador estructural absoluto: es un meta-ejercicio que mide near transfer dentro de la propia sesion (gemelo entrenado vs isomorfo con mismo `logicalStructureId`, transfer=1.0 solo si AMBOS correctos). Ningun competidor de brain-training mide transferencia honesta en tiempo real; la mayoria solo reporta mejora en la tarea entrenada. Robustez 5/5. Es la prueba viva de que el entreno generaliza.
2. **E1 — Bayes Arena: Calibrador de Posterior Continuo (Decisiones).** Dial continuo sobre P(H|E) integrando base-rate, sensibilidad y FP. Entrena actualizacion bayesiana real contra base-rate neglect con respuesta continua (no multiple choice), lo que da una superficie de error fina e imposible de adivinar. Profundidad psicometrica que ningun trainer comercial ofrece.
3. **E8 — Valor de la Informacion (Decisiones).** Mide la parada optima recolectar-vs-decidir contra el umbral VOI: entrena metacognicion de "cuando dejar de informarse y decidir", un constructo de decision de orden superior ausente del mercado. Distinto de E1 (actualizacion) — aqui se mide el control del muestreo de evidencia.
4. **Detección de lo Inverosímil (Creatividad).** Resuelve el problema irresoluble del scoring de creatividad sin IA: rareza x plausibilidad contra un banco pre-rankeado da un proxy honesto de originalidad, verificable y reproducible. Convierte la creatividad —tipicamente no medible o medida con IA opaca— en una metrica defendible.
5. **Freno Adaptativo / SSRT (Enfoque).** Stop-signal con escalera SSD online y SSRT por integracion de Verbruggen: estandar de oro de laboratorio para inhibicion reactiva, raramente implementado bien en apps de consumo. La escalera online + el calculo correcto de SSRT son tecnicamente exigentes y señalan rigor cientifico real.

## Top 5 con mayor riesgo

1. **Transferencia Espejo (facilidad 2).** *Riesgo:* requiere bancos de items isomorfos validados (mismo `logicalStructureId`, superficie distinta) y un emparejado gemelo-isomorfo sin contaminacion; un isomorfismo mal construido invalida toda la metrica de transfer. *Mitigacion:* generar pares por plantilla parametrica con `logicalStructureId` como invariante forzado por test de consistencia; validar empiricamente que el gemelo y el isomorfo correlacionan en dificultad antes de exponer; arrancar con un set pequeño y curado, expandir tras evidencia.
2. **E8 — Valor de la Informacion (facilidad 2).** *Riesgo:* la frontera VOI optima exige un solver de referencia correcto; un umbral mal calibrado entrena el comportamiento equivocado (sobre- o infra-muestreo de evidencia). *Mitigacion:* solver determinista verificado con casos cerrados y cobertura Kover alta sobre el motor; exponer solo escenarios donde el optimo es analiticamente computable; bloquear puntuacion autoritativa hasta validar el solver contra un oraculo.
3. **Cadencia Anticipatoria / Tiempo de Coincidencia (facilidad 2, Velocidad).** *Riesgo:* la medida de error temporal con signo depende de la latencia de presentacion del frame y del input; la varianza de display/touch en Android contamina la señal predictiva. *Mitigacion:* anclar el reloj a Choreographer (vsync) y registrar la SSD/instante real efectivo por trial; calibrar el offset de input por dispositivo; reportar solo variabilidad intra-sujeto (z contra baseline propio), nunca valores absolutos cross-device.
4. **Argument X-Ray (facilidad 2, Lenguaje).** *Riesgo:* etiquetar rol de oracion (conclusion/premisa/irrelevante) + juzgar suficiencia tiene ambiguedad de gold-standard; textos mal disenados producen claves disputables y baja fiabilidad inter-item. *Mitigacion:* restringir a argumentos de estructura canonica con rol univoco; doble-clave por revision experta antes de cablear; medir consistencia interna del banco y descartar items con baja discriminacion.
5. **Reencuadre del Problema / Analogías Remotas (Creatividad, robustez 2).** *Riesgo:* diversidad de marcos y puentes analogicos son intrinsecamente abiertos; sin IA, el scoring colapsa a heuristicas debiles o a banco cerrado que mata la rejugabilidad. *Mitigacion:* reducir al nucleo verificable (modo proporcional A:B::C:? con clave estructural para Analogias; conteo de dimensiones-de-marco distintas contra taxonomia cerrada para Reencuadre); marcar la metrica como exploratoria/no-autoritativa en el perfil hasta tener validacion; no agregar a score global hasta superar gate de fiabilidad.

---

# 7. Roadmap MVP de 30, 60 y 90 dias

## Hito 30 dias — Nucleo medible y honesto

**Ejercicios (8, todos P0 de alta facilidad/robustez):**
- Lenguaje: Evidence Lock Pro, Scope & Negation Drill.
- Logica: Necesario vs Suficiente, Cadena Deductiva Progresiva.
- Decisiones: E3 Forecast Calibrado, E4 Check-in Cognitivo Go/No-Go (gate de readiness).
- Enfoque: Vigía Probabilístico, Filtro de Distracción.

**Infra de scoring/adaptacion:**
- Consolidar `ScoringEngine` (Lenguaje), `ReasoningScorer/ICR` (Logica), `ScoringEquations`/`BayesEngine` (Decisiones) y `DefaultMetricsEngine`+`AdaptiveEngine` (Enfoque) bajo el contrato comun ya existente.
- Brier proper-scoring operativo (E3) como cimiento de la dimension confianza.
- E4 cableado como gate de validez: ninguna sesion produce score autoritativo si el CRI (accuracy/RT-CV/fatiga) cae bajo umbral.
- Persistencia local cifrada de resumenes por ejercicio (`SecureKeyValueStore`), sin trials crudos.

**Gates de calidad (bloqueantes en hub):**
- Test de consistencia catalogo: `registry.keys` ⟺ `implemented` (un generador render-compatible no expuesto rompe el build).
- Detekt ratchet verde + cobertura Kover sobre motores nuevos (>=90% del paquete de scoring).
- Gates de fatiga y fairness activos por sesion; baja fatiga declarada verificada.

## Hito 60 dias — Expansion, transferencia y recomendador

**Ejercicios (+8):**
- Logica: Silogismo Categorico, Caza-Contraejemplo, **Transferencia Espejo** (transfer probe canonico).
- Lenguaje: Calibrated Comprehension, Semantic Surgery.
- Decisiones: E1 Bayes Arena, E2 Triaje de Reversibilidad.
- Velocidad: Cifrado Veloz (Símbolo-Dígito).

**Transfer probes:**
- Cablear Transferencia Espejo como sonda de generalizacion intra-modulo (gemelo↔isomorfo via `logicalStructureId`); reportar transferConsistencyScore separado del score de tarea.
- Definir pares isomorfos validados por plantilla para Logica; iniciar bancos para Lenguaje.

**Recomendador:**
- `personalization/*` selecciona el siguiente ejercicio por deficit relativo del perfil (sub-skill mas debil contra baseline propio), no por novedad.
- Plan diario adaptativo: mezcla de dominios con balance de carga (evitar dos altos-fatiga consecutivos).

**Gates:**
- Reliability gate por banco de items (discriminacion, consistencia interna) antes de exponer ejercicios de clave disputable.
- Calibracion de dificultad inicial del staircase por dispositivo (Velocidad).

## Hito 90 dias — Cobertura completa, validacion empirica y deload

**Ejercicios (+8, cobertura de los 6 modulos):**
- Creatividad: Detección de lo Inverosímil, Supresión del Primer Impulso, Elaboración Incremental.
- Emociones: EM-B Reentrada tras Error, EM-C Separador (Granularity Quad), EM-G Interferencia Afectiva.
- Enfoque: Freno Adaptativo (SSRT), Conmutador de Reglas.

**Validacion empirica:**
- Holdout/baseline: split de items de evaluacion no entrenados para estimar transfer real cross-sesion.
- Test-retest de fiabilidad por metrica nuclear (d′, ICR, Brier, switch cost, SSRT) sobre datos propios agregados localmente.
- Agregacion de perfil de 10 dimensiones solo con metricas que superaron el reliability gate; las no validadas permanecen CALIBRATING (nunca 0).

**Plateau / deload:**
- Deteccion de meseta por estancamiento de la curva propia; introducir deload (reduccion de carga/dificultad) y rotacion de dominio.
- Senal de fatiga acumulada que fuerza descanso o ejercicios de baja carga (penalizar secuencias que disparen FatigueIndex).

**Gates:**
- Privacy/safety review de la agregacion de perfil (datos cognitivos sensibles, copy no clinico).
- Launch-readiness gate completo antes de exponer el brain-profile agregado.

---

# 8. Lista final de riesgos y mitigaciones

| Riesgo | Tipo | Severidad | Mitigacion |
|---|---|---|---|
| Claves disputables en ejercicios de juicio abierto (Argument X-Ray, Compression Ladder) bajan fiabilidad inter-item | Medicion | Alta | Restringir a estructura canonica con rol univoco; doble-clave experta; reliability gate (discriminacion + consistencia) antes de cablear |
| Scoring de creatividad sin IA colapsa a heuristicas debiles (Reencuadre, Analogías Remotas, Improvisación) | Medicion | Alta | Reducir al nucleo verificable (proporcional/banco pre-rankeado); marcar exploratorio/no-autoritativo; excluir del score global hasta validar |
| Isomorfos mal construidos invalidan la metrica de transfer (Transferencia Espejo) | Medicion | Alta | `logicalStructureId` invariante forzado por test; validar equivalencia de dificultad gemelo↔isomorfo antes de exponer; set curado inicial |
| Solver VOI/optimo mal calibrado entrena el comportamiento erroneo (E8, E7 Presupuesto) | Cognitivo | Alta | Solver determinista verificado contra oraculo; cobertura Kover alta; bloquear score autoritativo hasta validacion; escenarios analiticos cerrados |
| Varianza de display/touch en Android contamina medidas temporales finas (Cadencia Anticipatoria, RSVP, SSRT) | Tecnico | Alta | Reloj anclado a Choreographer/vsync; registrar instante efectivo por trial; calibrar offset por dispositivo; reportar solo z intra-sujeto |
| Sobre-claim cognitivo/clinico en copy y perfil agregado | Fairness | Critica | Lenguaje funcional ("perfil funcional", "practica", "baseline propio"); scanner de terminologia prohibida; privacy/safety review; nunca diagnostico |
| Induccion afectiva por encima de "leve" en modulo Emociones | Emocional | Critica | Distractor afectivo solo leve (lock CEO); sin contenido aversivo; gate de fatiga; opt-out; revision de claims emocionales |
| Fatiga acumulada degrada medida y adherencia (Reenganche Atencional 5, Doble Vía, MOT, Lexical Sprint) | Cognitivo | Alta | FatigueIndex por sesion; penalizar secuencias alto-fatiga consecutivas; deload por meseta; limitar duracion de bloques alto-coste |
| Fuga de datos cognitivos sensibles en persistencia/backup | Tecnico | Critica | `SecureKeyValueStore` AES-GCM+Keystore; solo resumenes (no trials crudos); exclusion de backups; clave device-bound no respaldada |
| Agregacion de perfil reporta 0 en metricas no calibradas (señal falsa de deficit) | Medicion | Alta | Estados CALIBRATING que renderizan "—" nunca 0; agregar solo metricas que superaron reliability gate |
| Gameo del trade-off velocidad-exactitud (Velocidad, Calibración de Apuesta Temporal) | Medicion | Media | IES gobernante (penaliza accuracy baja a alta velocidad); techo por comision; staircase multi-señal que castiga estrategias degeneradas |
| Decremento de vigilancia mal modelado da d′ inestable (Vigía Probabilístico) | Medicion | Media | Tasa de evento variable controlada; ventana minima de trials para confidence; MAD solo en tendencia central, lapsos sobre crudos |
| Item exposure / aprendizaje de respuestas por repeticion baja validez | Medicion | Media | Bancos amplios + generacion parametrica; rotacion; no reexponer item reciente; rejugabilidad por variacion de superficie, no de clave |
| Ejercicio implementado pero inalcanzable (presentacion incompleta) | Tecnico | Media | Test de consistencia `registry.keys`⟺`implemented`; distinguir `domainReady` de UI alcanzable; solo exponer paradigmas con renderer existente |
| Desfase regla Firestore↔query cliente o flag↔impl multi-modulo al cablear backend futuro | Tecnico | Media | Mantener tests de reglas espejo de queries reales; actualizar TODAS las impls de interfaces multi-modulo; verificar con `:androidApp:compileDebugKotlin` |

---

# 9. Checklist de calidad para aprobar futuros ejercicios

Un nuevo ejercicio NO se cablea al hub hasta cumplir TODOS los items aplicables:

1. **Constructo declarado.** Define explicitamente el constructo cognitivo objetivo y el sub-skill del perfil al que contribuye; no se solapa de forma redundante con un ejercicio ya existente sin justificacion de valor incremental.
2. **Mecanismo cientifico validado.** El paradigma tiene base en literatura cognitiva (o es variante honesta de un estandar); revisado por la skill de cognitive-science / review-cognitive-claims.
3. **Metrica primaria deterministica.** La señal nuclear (d′, c, ICR, Brier, switch cost, SSRT, transferConsistency, etc.) se computa sin IA, con clave univoca y reproducible.
4. **Clave no disputable.** La respuesta correcta es formalmente verificable; los ejercicios de juicio pasan doble-clave experta y reliability gate (discriminacion + consistencia interna) antes de exponerse.
5. **Resistencia al gameo.** El scoring castiga estrategias degeneradas (over-claiming, sacrificio de accuracy por velocidad, respuesta por azar); IES/penalizaciones documentadas.
6. **Motor conectado, no inventado.** Reusa un motor de scoring/adaptacion existente del modulo; no introduce cripto, persistencia ni motores nuevos sin justificacion explicita.
7. **Adaptacion calibrada.** El `AdaptiveEngine`/staircase ajusta dificultad al baseline propio del usuario; el rango de dificultad esta acotado y testeado en sus extremos.
8. **Gate de validez por sesion.** Honra el gate de readiness/CRI y los gates de fatiga y fairness; no produce score autoritativo bajo confidence insuficiente (n minimo de trials).
9. **Manejo honesto de baja confianza.** Con muestra insuficiente reporta CALIBRATING/"—", nunca 0 ni un valor fabricado; `confident=false` coherente con lo que produce el MetricsEngine.
10. **Sin claims clinicos.** Copy en lenguaje funcional (practica/baseline/perfil funcional); pasa el scanner de terminologia prohibida (sin clinical/diagnostic/IQ ni compuestos con guion); nada de neurociencia falsa.
11. **Seguridad emocional.** Si induce afecto, solo nivel leve, con opt-out y sin contenido aversivo; revisado por privacy/safety.
12. **Persistencia minima y cifrada.** Solo persiste resumenes via `SecureKeyValueStore` (AES-GCM+Keystore); nunca trials crudos; DTOs `@Serializable` versionados (`schemaVersion`); excluido de backups.
13. **Fatiga acotada.** Riesgo de fatiga evaluado; duracion de bloque limitada; no se encadena con otro ejercicio alto-fatiga; respeta el FatigueIndex.
14. **Estabilidad temporal (si aplica).** Las medidas dependientes de tiempo anclan el reloj a Choreographer/vsync, registran el instante efectivo por trial y reportan z intra-sujeto, no absolutos cross-device.
15. **Rejugabilidad por variacion de superficie.** Banco amplio o generacion parametrica; la rejugabilidad proviene de variar superficie, no la clave; control de item-exposure (no reexponer item reciente).
16. **UI alcanzable, no solo compilable.** Renderer existente y captura de respuesta completa; pasa el test de consistencia `registry.keys` ⟺ `implemented`; distingue `domainReady` de jugable; toda lista de cards crece con `verticalScroll`/`LazyColumn`.
17. **Cobertura de tests.** Tests de dominio del generador/clasificador/scorer (incluida regresion de los casos limite) verdes; cobertura Kover del motor >=90% via `koverVerifyDesktop`; Detekt ratchet sin regenerar baseline.
18. **Analitica y perfil.** Emite los eventos de metrica necesarios para el perfil; solo se agrega al score global de 10 dimensiones tras superar el reliability gate; las metricas exploratorias quedan fuera del agregado hasta validarse.
19. **Verificacion multiplataforma.** Compila en `:shared:compileAndroidMain` y `:androidApp:compileDebugKotlin`; `expect`/`actual` cubiertos en todos los targets declarados; sin dependencias Android-only en `:shared`.
20. **Veredicto QA final.** Aprobado por el qa-release-gate (verdict approve/revise/block) antes del merge a la rama de feature.