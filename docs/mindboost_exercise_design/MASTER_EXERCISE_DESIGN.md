> Documento maestro — organización de diseño cognitivo de MindBoost (workflow multi-agente, 18 agentes).

> Anclado en los motores reales del repo. No constituye claim médico ni clínico.

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

# 3. Ejercicios por módulo



# 3.1 Enfoque (8 ejercicios)

＃ Catálogo de 8 ejercicios de calidad suprema — MindBoost Enfoque

> Contrato transversal: todos cumplen las fronteras de seguridad (perfil cognitivo funcional, no diagnóstico, no IQ), dificultad paramétrica sobre `DifficultyVector`, autoridad de clasificación en el `*SessionAssembler`, persistencia `SecureKeyValueStore` + `@Serializable`+`schemaVersion`, scoring null-si-no-confident y `preliminary=true`. Marco de reutilización/extensión de renderer indicado por ejercicio.

---

## Vigía Probabilístico (Sustained Attention con tasa de evento variable)

### Módulo
Principal: Enfoque (atención sostenida / vigilancia). Secundarios: Velocidad (RT base como covariable), Decisiones (sesgo de criterio bajo incertidumbre).

### Objetivo cognitivo
Entrenar atención sostenida y detección de señal rara bajo carga temporal prolongada, midiendo decremento de vigilancia y estabilidad del criterio de respuesta (Teoría de Detección de Señal: d′ y criterio c). Ataca el sub-componente "mantener sensibilidad perceptual cuando la señal es infrecuente y el tiempo en tarea crece".

### Hipótesis de transferencia
SÍ podría transferir: near transfer a otras tareas de vigilancia/monitoreo con señal rara y a la resistencia al decremento temporal. NO se debe afirmar: mejora de "concentración general", reducción de síntomas de TDAH, ni transferencia far a productividad/estudio. Se reporta como hipótesis no garantizada.

### Loop principal
1. Estímulo: flujo de estímulos `Vigilance` (no-target frecuentes + target raro, p. ej. una orientación/longitud crítica) con onset monotónico (`MonotonicClock.nowNanos()`).
2. Decisión: tap solo cuando aparece el target (go/withhold). Una respuesta por trial.
3. Feedback: inmediato por trial (HIT verde / FALSE_ALARM ámbar / MISS gris), breve (<250 ms), sin romper el ritmo.
4. Adaptación: tras cada bloque, `DefaultAdaptiveEngine.decide` ajusta tasa de target, ISI y similitud target/lure.
5. Siguiente trial: ISI determinista del `BlockSpec` (seed) con jitter controlado.

### Reglas
Responder solo al target; retener ante no-targets. Respuesta válida dentro de `responseWindowMs`. RT < `antFloor` = ANTICIPATION (descartado de tendencia central, contado aparte). Bloques largos (3–6) para exponer decremento de vigilancia.

### Inputs
Tap único (renderer single-tap existente, `FocusStimulusField`). Sin swipe/drag/voz.

### Variables de dificultad
1. `targetRate` 0.08–0.30 (proporción de targets; más bajo = más difícil).
2. `interStimulusDelayMs` 700–2200 ms (ISI; más largo = más decremento).
3. `targetLureSimilarity` 0.0–0.85 (similitud perceptual target↔lure; más alta = menor d′).
4. `responseWindowMs` 500–1100 ms.
5. `blockLengthTrials` 40–120 (tiempo en tarea).
6. `stimulusContrast` 0.4–1.0 (saliencia perceptual).
7. `jitterMs` 0–400 ms (irregularidad temporal del onset; rompe predicción rítmica).

### Algoritmo adaptativo
`AdaptiveInput` con d′ y accuracy como gobernantes. Banda objetivo accuracy ≈ 0.78–0.85 sobre targets. Si `hitRate` alto y `falseAlarmRate` bajo → StepUp (baja `targetRate`, sube `targetLureSimilarity`). Si `falseAlarmRate` alta (impulsividad) → `ContainImpulsivity` (sube `responseWindowMs`, baja similitud). Fatiga (slope intra-bloque de RT/accuracy vía `FatigueIndex`/`FatigueSignal`) → `Cooldown`/`StepDown`. Plateau detectado sobre serie de summaries → SwitchAxis (de `targetRate` a `jitterMs`). Baseline: `targetRate` inicial anclada a `BaselineSnapshotDto`.

### Métricas primarias
`SUSTAINED_FOCUS_INDEX`, `VIGILANCE_DECREMENT_SLOPE` (pendiente accuracy/RT por sub-bloque temporal), d′ y criterio c (derivados de HIT/FALSE_ALARM por bloque).

### Métricas secundarias
`RT_MEDIAN` (HIT), `RT_CV` (`BLOCK_STABILITY`), omission vs commission separadas, `IES`, `FATIGUE_INDEX`, `POST_ERROR_SLOWING`.

### Señales de fatiga o mala calidad
Slope de RT positivo y de accuracy negativo sostenido intra-sesión; subida de omisiones en últimos sub-bloques; `RT_CV` creciente; `SubjectiveState` adverso; n efectivo de targets < umbral → `confident=false`.

### Scoring sugerido
`score = w_d*norm(dPrime) + w_stab*(1 - RT_CV) - w_dec*max(0, decrementSlope)` con `w_d=0.5, w_stab=0.3, w_dec=0.2`; `dPrime = z(hitRate) - z(falseAlarmRate)`. `value=null` si n_targets < MIN_N(20) o `confident=false`; `preliminary=true`.

### Antitrampa / calidad de datos
Detección de tap rítmico (respuestas equiespaciadas independientes del onset) → marca bloque CONTAMINATED. ANTICIPATION excesiva → invalida. Mash-tapping (FA muy alta + RT ultracorto constante) → `ContainImpulsivity` + descuento de confianza. Seed determinista permite auditar la secuencia.

### Variantes
1. Visual-orientación (target = barra con orientación crítica).
2. Auditiva-tono (target = tono raro; requiere canal audio, secundaria).
3. Numérico-paridad (target = dígito que rompe regla; reduce dependencia perceptual fina, más cultural-neutro vía símbolos).

### UX
Pantalla limpia, estímulo central grande (>48 dp), zona de tap = pantalla completa. Feedback por color semántico (`AppColors`) + microcambio de forma (no solo hue, accesibilidad daltónica). Duración ideal 4–7 min. Microanimación funcional: pulso de confirmación de tap (no decorativa).

### Implementación KMP
Reusa renderer single-tap (`FocusStimulusField`). Generador nuevo `VigiaProbGenerator: TrialGenerator` en `FocusExerciseGenerators.registry`; `StimulusPayload.Vigilance` (existe). Clasificación `TrialClassifier.classify` (tap-or-withhold). Métricas `DefaultMetricsEngine.blockMetrics` con `primaryMetrics={SUSTAINED_FOCUS_INDEX,VIGILANCE_DECREMENT_SLOPE}`. Adaptación `DefaultAdaptiveEngine`. Persistencia: `SessionSummaryDto` + `LocalSessionHistoryRepository`, namespace Focus. Modelo: `data class VigiaSummary(schemaVersion:Int, dPrime:Double?, decrementSlope:Double?, n:Int, confident:Boolean)`. Eventos a registrar: onset/tap/outcome por trial (en memoria), solo summary persistido. Estado mínimo: `FocusFieldState.Signal/Lure/Feedback`. No requiere renderer nuevo.

### Riesgos
Decremento de vigilancia exige bloques largos → riesgo de fatiga/abandono. d′ inestable con pocos targets. Posible aprendizaje de la cadencia si jitter insuficiente.

### Revisión crítica
El Revisor exige: (1) probar que d′ se computa con `n_targets` suficiente o se reporta `null` —no fabricar c con 3 targets—; (2) jitter obligatorio ≥150 ms para que no mida ritmo motor; (3) separar omission/commission o el SUSTAINED_FOCUS_INDEX confundirá inatención con impulsividad; (4) gate de fatiga ANTES de cualquier StepUp; (5) corrección de continuidad en z() cuando hitRate=1 o FA=0 para evitar d′ infinito. Sin estas correcciones, el ejercicio mide cadencia y se rompe en extremos.

### Veredicto
Aprobado con cambios (añadir jitter mínimo, corrección de continuidad en d′, gate de fatiga pre-StepUp y reporte null con n insuficiente).

---

## Filtro de Distracción (Flanker dinámico con carga perceptual)

### Módulo
Principal: Enfoque (control atencional selectivo / inhibición de interferencia). Secundarios: Velocidad, Decisiones (control de conflicto).

### Objetivo cognitivo
Entrenar atención selectiva y supresión de distractores congruentes/incongruentes (paradigma Flanker), midiendo el efecto de congruencia (interferencia) y su modulación por carga perceptual. Sub-componente: resolver conflicto estímulo-respuesta sin sacrificar velocidad base.

### Hipótesis de transferencia
SÍ: near transfer a tareas de filtrado de distractores y control de interferencia. NO: afirmar mejora de "multitarea", atención general o rendimiento académico. Far transfer = hipótesis no garantizada.

### Loop principal
1. Estímulo: target central + flancos congruentes/incongruentes (`StimulusPayload.Flanker`).
2. Decisión: LEFT/RIGHT según el target central (dos alternativas).
3. Feedback: inmediato (correcto/incorrecto + latencia).
4. Adaptación: ajusta proporción de incongruentes, número/excentricidad de flancos, deadline.
5. Siguiente trial: orden entrelazado (interleaving) seed-determinista.

### Reglas
Responder al target central, ignorar flancos. Acierto depende del LADO (`classifyChoice`), no de responder en ventana. Incongruentes mezclados (no bloqueados) para forzar selección por trial.

### Inputs
Pantalla dividida en mitades táctiles LEFT/RIGHT (renderer `FocusChoiceField`, `ChoiceParadigm.FLANKER` — existe).

### Variables de dificultad
1. `incongruentRatio` 0.25–0.65.
2. `flankerCount` 2–8 (más flancos = más carga).
3. `flankerEccentricityDeg` 0.5–3.0 (cercanía = más interferencia cuando es pequeña).
4. `responseWindowMs` 600–1200 ms (deadline adaptativo).
5. `targetContrast` 0.4–1.0.
6. `perceptualLoadDistractors` 0–6 (distractores neutros que consumen capacidad).
7. `interStimulusDelayMs` 500–1500 ms.

### Algoritmo adaptativo
Gobernante: `CONGRUENCY_EFFECT` (RT_incong − RT_cong) y accuracy. Banda accuracy ≈ 0.80. RT bajo + congruency controlado → StepUp (sube `incongruentRatio`, `flankerCount`). Commission alta en incongruentes → `ContainImpulsivity`. Fatiga → `Cooldown`. Anti-oscilación: histéresis sobre deadline. Baseline ancla `incongruentRatio` y deadline inicial.

### Métricas primarias
`CONGRUENCY_EFFECT` (solo HIT-con-RT en ambas condiciones), accuracy en incongruentes, `IES` por condición.

### Métricas secundarias
`RT_MEDIAN`, `RT_CV`, `POST_ERROR_SLOWING`/`POST_ERROR_ACCURACY`, `LISAS`, commission rate, `RATE_CORRECT_SCORE`.

### Señales de fatiga o mala calidad
Congruency effect que colapsa por caída global de accuracy (no por mejora de control); RT_CV alto; respuestas a un solo lado (sesgo motor) → mala calidad; n incongruente insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accIncong + w_ctrl*(1 - norm(congruencyEffect)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_ctrl=0.3, w_stab=0.2`. `congruencyEffect=null` si falta RT válido en un lado → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detección de sesgo de lado (ratio LEFT/RIGHT alejado de la mezcla real). Respuestas pre-onset = ANTICIPATION. Patrón de "responder siempre congruente" detectable por accuracy condicionada. Bloque CONTAMINATED si congruency<0 con accuracy degradada.

### Variantes
1. Flechas (clásico, símbolos no-lingüísticos).
2. Formas/colores (target = forma, flancos = formas distractoras).
3. Letras-símbolo abstracto (evita dependencia de alfabeto concreto; usar glifos neutros).

### UX
Estímulo central grande, flancos a ambos lados; mitades táctiles claras. Cue de target persistente. Feedback semántico + forma. Duración 3–5 min. Microanimación: flash de mitad pulsada.

### Implementación KMP
Reusa `FocusChoiceField` (`ChoiceParadigm.FLANKER`). `FlankerLoadGenerator` ya alineado a E2; aquí es rediseño premium con `perceptualLoad`/`eccentricity`. `RecordedTrial(responseKey,...)`; `TrialClassifier.classifyChoice` (autoridad). `DefaultMetricsEngine` con `CONGRUENCY_EFFECT`. `DefaultAdaptiveEngine`. Persistencia summary en namespace Focus. Modelo: `data class FlankerSummary(schemaVersion, congruencyEffectMs:Double?, accIncong:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Congruency effect ruidoso con pocos trials. Sesgo motor de lado. Carga perceptual puede medir agudeza visual más que control si contraste mal calibrado.

### Revisión crítica
El Revisor exige: (1) congruency `null` si falta un lado (no rellenar con media); (2) controlar contraste para no confundir percepción con control; (3) balancear LEFT/RIGHT correctos para que el sesgo motor no infle accuracy; (4) reportar IES por condición para detectar trade-off velocidad-error; (5) evitar bloquear incongruentes (mata el interleaving). Sin balanceo de lado y null honesto, mide impulsividad lateral, no inhibición.

### Veredicto
Aprobado con cambios (balanceo de lado obligatorio, congruency null honesto, control de contraste, IES por condición).

---

## Freno Adaptativo (Stop-Signal con escalera online)

### Módulo
Principal: Enfoque (inhibición de respuesta / control inhibitorio proactivo y reactivo). Secundarios: Velocidad (go-RT), Decisiones (frenado de impulso).

### Objetivo cognitivo
Entrenar la capacidad de cancelar una respuesta ya iniciada (inhibición reactiva), cuantificada por SSRT (Stop-Signal Reaction Time, método de integración de Verbruggen). Sub-componente: latencia del proceso de freno.

### Hipótesis de transferencia
SÍ: near transfer a control inhibitorio motor y a tareas go/stop. NO: afirmar control de impulsos en decisiones de vida, gambling o autorregulación general. Far = hipótesis.

### Loop principal
1. Estímulo: trial GO (responder por defecto); en subset, señal de stop tras SSD variable.
2. Decisión: responder rápido en GO; ABORTAR si aparece stop dentro de la SSD.
3. Feedback: inmediato (GO correcto / freno exitoso / inhibición fallida).
4. Adaptación: SSD online por escalera 1-up/1-down (`StopSignalTracker`) → p(respond|signal)≈0.5.
5. Siguiente trial.

### Reglas
GO = responder en ventana. STOP = retener. Responder en stop = FALSE_ALARM (inhibición fallida, RT retenido como go-RT). p(respond|signal) debe quedar en 0.25–0.75 o el SSRT no es fiable (`trustworthy=false`).

### Inputs
Renderer `FocusFieldState.Stop` (existe): fase GO; si no responde dentro de SSD, mitad de ventana cambia a estado STOP rojo (forma + color, no solo hue). Tap único.

### Variables de dificultad
1. `stopTrialRatio` 0.20–0.33 (proporción de stops).
2. `ssdStartMs` 150–350 ms (SSD inicial; la escalera la ajusta online).
3. `ssdStepMs` 25–75 ms (paso de la escalera).
4. `goResponseWindowMs` 700–1200 ms.
5. `goStimulusSalience` 0.4–1.0.
6. `interStimulusDelayMs` 600–1600 ms.
7. `blockLengthTrials` 48–96.

### Algoritmo adaptativo
SSD intra-bloque por `StopSignalTracker` (escalera). Inter-bloque `DefaultAdaptiveEngine`: si SSRT estable y go-RT bajo → StepUp (sube `stopTrialRatio`/baja salience). Si p(respond|signal) fuera de banda → recalibrar escalera (no premiar gaming). Fatiga → `Cooldown`. Commission en GO ≠ stop. Baseline ancla `ssdStart`.

### Métricas primarias
`SSRT` (`SsrtCalculator`, integración Verbruggen), p(respond|signal), `trustworthy`.

### Métricas secundarias
go-RT mediana, go-RT_CV, go-accuracy, stop-failure rate, `POST_ERROR_SLOWING`, `FATIGUE_INDEX`.

### Señales de fatiga o mala calidad
Estrategia de ralentizar GO para "esperar" el stop (go-RT inflado intencionalmente) → invalida SSRT; `trustworthy=false` si p sale de banda; SSD que diverge (escalera rota); go-omissions altas.

### Scoring sugerido
`score = w_ssrt*(1 - norm(SSRT)) + w_go*(1 - norm(goRT)) + w_stab*(1 - goRT_CV)` con `w_ssrt=0.55, w_go=0.2, w_stab=0.25`, SOLO si `trustworthy`. Si `!trustworthy` → `value=null`, caption "calibrando freno". `preliminary=true`.

### Antitrampa / calidad de datos
Gate clave: detectar "waiting strategy" comparando go-RT en bloques con vs sin stop (inflación sistemática). p(respond|signal) fuera 0.25–0.75 = no fiable. Anticipaciones en GO descartadas. Escalera auditada por seed/log.

### Variantes
1. Stop visual (señal = cambio de color/forma del estímulo go).
2. Stop posicional (señal = aparición de marcador en zona fija).
3. Selective-stop (frenar solo ante una clase de señal, no otra — incrementa carga de selección).

### UX
GO claro; transición a STOP inequívoca y abrupta (rojo + forma + posición). Feedback de freno exitoso reforzado positivamente (no culpa). Duración 4–6 min. Microanimación: corte visual al activarse stop.

### Implementación KMP
Reusa `FocusFieldState.Stop` + `E4FrenoGenerator.tracker()` patrón (`StopSignalTracker`). `RecordedTrial(stopSignalDelayMs=...)`; `TrialClassifier.classifyStop`. `SsrtCalculator.SsrtInput(goReactionTimes, stopTrials, stopFailures, meanStopSignalDelayMs)` → `SsrtResult`. `DefaultAdaptiveEngine` inter-bloque. Persistencia: `data class StopSummary(schemaVersion, ssrtMs:Double?, pRespond:Double?, trustworthy:Boolean, n:Int)`. Estado mínimo: GO→STOP dos fases en holder. No requiere renderer nuevo.

### Riesgos
SSRT muy sensible a estrategia de espera y a escalera mal calibrada. Requiere n de stops suficiente. Daltonismo si stop depende solo de color.

### Revisión crítica
El Revisor exige: (1) detección obligatoria de waiting strategy con go-RT comparado; (2) `trustworthy=false`→null sin excepción; (3) stop señalado por ≥2 canales (color+forma+posición); (4) n_stops mínimo para SSRT o null; (5) registrar la SSD usada por trial (no recalcular post-hoc). Sin el gate de estrategia, el SSRT premiará a quien ralentiza GO — métrica destruida.

### Veredicto
Aprobado con cambios (gate anti-waiting-strategy, null estricto si no trustworthy, señal stop multicanal).

---

## Conmutador de Reglas (Task-switching con coste de cambio)

### Módulo
Principal: Enfoque (flexibilidad cognitiva / set-shifting). Secundarios: Decisiones (selección de regla), Velocidad.

### Objetivo cognitivo
Entrenar la reconfiguración del set mental al alternar entre reglas (color vs forma), midiendo `SWITCH_COST` (RT_switch − RT_repeat). Sub-componente: coste de reconfiguración y resistencia a interferencia de tarea previa.

### Hipótesis de transferencia
SÍ: near transfer a tareas de alternancia de reglas y flexibilidad de set. NO: afirmar "mejor multitasking" real (la multitarea es mito), creatividad o adaptabilidad general. Far = hipótesis.

### Loop principal
1. Estímulo: figura multidimensional (color+forma) + cue de regla activa (`StimulusPayload.TaskSwitch` con `colorIsRed`/`shapeIsCircle`).
2. Decisión: LEFT/RIGHT derivado de aplicar la regla activa al estímulo (el usuario DERIVA, no se muestra la respuesta).
3. Feedback: inmediato.
4. Adaptación: ajusta switch ratio, CSI (cue-stimulus interval), proporción de incongruentes entre reglas.
5. Siguiente trial.

### Reglas
RULE_A=color (rojo→izq), RULE_B=forma (círculo→izq). Trials switch vs repeat entrelazados. `SWITCH_COST` solo con HIT-con-RT en ambos lados (switch y repeat).

### Inputs
`FocusFieldState.RuleSwitch` + `FocusRuleSwitchField` (existe): forma coloreada + cue de regla, mitades táctiles LEFT/RIGHT.

### Variables de dificultad
1. `switchRatio` 0.25–0.60.
2. `cueStimulusIntervalMs` 100–900 ms (CSI corto = más coste).
3. `responseWindowMs` 700–1300 ms.
4. `crossRuleIncongruentRatio` 0.2–0.6 (estímulos donde ambas reglas dan respuestas distintas).
5. `ruleCount` 2–3 (añadir tercera regla, p. ej. tamaño).
6. `interStimulusDelayMs` 500–1400 ms.
7. `cueSalience` 0.4–1.0.

### Algoritmo adaptativo
Gobernante: `SWITCH_COST` + accuracy. Banda ≈0.80. Coste bajo y estable → StepUp (sube `switchRatio`, baja CSI). Errores de perseveración (aplicar regla anterior) altos → StepDown o `Cooldown`. Fatiga → `Cooldown`. SwitchAxis si `switchRatio` satura → mover a `crossRuleIncongruentRatio`. Baseline ancla CSI.

### Métricas primarias
`SWITCH_COST` (HIT-RT switch vs repeat), accuracy en switch, error de perseveración.

### Métricas secundarias
`RT_MEDIAN`, `RT_CV`, `IES` por condición, `POST_ERROR_ACCURACY`, mixing cost (bloques mono-regla vs mixtos).

### Señales de fatiga o mala calidad
Coste de cambio que sube por colapso global de accuracy; perseveración creciente; ignorar el cue (responder a una sola regla); n switch insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accSwitch + w_flex*(1 - norm(switchCost)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_flex=0.3, w_stab=0.2`. `switchCost=null` si falta RT en una condición → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar "ignorar cue" (accuracy condicionada a regla = azar en una regla). Respuestas pre-cue inválidas. CSI registrado por trial. Balanceo de lado por regla.

### Variantes
1. Color/forma (clásico).
2. Paridad/magnitud numérica (dígito: par/impar vs mayor/menor que 5 — neutro cultural con símbolos).
3. Tres reglas (color/forma/tamaño) con cue rotatorio (carga de set extra).

### UX
Cue de regla persistente y visualmente distinto del estímulo (icono dedicado). Estímulo claro, mitades táctiles. Feedback semántico. Duración 4–6 min. Microanimación: cambio de cue marcado.

### Implementación KMP
Reusa `FocusRuleSwitchField`/`E6CambioGenerator` patrón. Payload deriva `correctResponse` de (rule,dimensión) — nunca expone respuesta. `TrialClassifier.classifyChoice`. `DefaultMetricsEngine` `SWITCH_COST`. `DefaultAdaptiveEngine`. Persistencia: `data class SwitchSummary(schemaVersion, switchCostMs:Double?, accSwitch:Double?, perseverationRate:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Switch cost ruidoso; riesgo de medir lectura del cue más que reconfiguración si cue mal diseñado. Perseveración confundible con error perceptual.

### Revisión crítica
El Revisor exige: (1) cue inequívoco y distinto del estímulo o se mide otra cosa; (2) switch cost `null` si falta un lado; (3) separar error de perseveración de error perceptual (registrar regla previa); (4) balanceo de lado por regla; (5) prohibir afirmar "multitasking". Sin separar perseveración, el coste mezcla flexibilidad con ruido.

### Veredicto
Aprobado con cambios (cue dedicado, null honesto de switch cost, registro de regla previa para aislar perseveración).

---

## Seguimiento Múltiple (MOT — Multiple Object Tracking)

### Módulo
Principal: Enfoque (atención dividida espacial sostenida). Secundarios: Memoria (mantenimiento espacial), Velocidad (procesamiento dinámico).

### Objetivo cognitivo
Entrenar el seguimiento atencional de varios objetivos en movimiento entre distractores idénticos, midiendo la capacidad de tracking (número de objetos mantenibles) y su umbral. Sub-componente: indexación atencional dinámica (FINST).

### Hipótesis de transferencia
SÍ: near transfer a tareas de tracking dinámico y atención espacial dividida (interés en deportes/conducción documentado pero NO garantizable). NO: afirmar mejora de conducción real, deporte o atención general. Far = hipótesis explícita.

### Loop principal
1. Estímulo: N objetos; un subset se marca como targets, luego todos se vuelven idénticos y se mueven (`StimulusPayload.MultiObjectTracking`).
2. Decisión: al detenerse, el sistema sondea UN objeto; el usuario indica si era target (tap o LEFT/RIGHT sí/no).
3. Feedback: inmediato.
4. Adaptación: ajusta nº targets, velocidad, densidad, duración.
5. Siguiente trial.

### Reglas
Una respuesta por trial (juicio de sonda: "¿el sondeado era target?"). La animación corre dentro del composable keyed por `layoutSeed=stimulus.index`; el holder espera UNA pulsación.

### Inputs
`FocusFieldState.Tracking` + `FocusTrackingField` (existe, `Canvas` con `animateFloatAsState`). Tap sí/no o mitades táctiles.

### Variables de dificultad
1. `targetCount` 1–5.
2. `totalObjects` 4–12.
3. `objectSpeedDpPerS` 40–220.
4. `trackingDurationMs` 2500–9000.
5. `objectRadiusDp` 10–22 (más pequeño = más difícil de indexar).
6. `proximityThresholdDp` 12–40 (cuán cerca pasan; cerca = confusión).
7. `distractorCount` derivado = totalObjects − targetCount.

### Algoritmo adaptativo
Staircase 1-up/2-down sobre `targetCount`/`objectSpeed` → umbral ≈79% correcto. Accuracy alta → StepUp (más targets/velocidad). Caída → StepDown. Fatiga (la tarea es exigente) → `Cooldown` con prioridad. SwitchAxis entre `speed` y `targetCount`. Baseline ancla `targetCount`. Convergencia de capacidad de tracking como umbral.

### Métricas primarias
`tracking capacity` (umbral de targets al ~79%), accuracy de sonda, `DISTRACTION_RESISTANCE_INDEX` (degradación por proximidad/densidad).

### Métricas secundarias
RT de juicio de sonda, `RT_CV`, accuracy por nivel de velocidad, slope de fatiga, sensibilidad a proximidad.

### Señales de fatiga o mala calidad
Accuracy que cae a azar (50%) sostenida = sobrecarga/desenganche; respuestas constantes "sí" o "no" (sesgo); RT de sonda ultracorto (adivinar); `SubjectiveState` alto de cansancio.

### Scoring sugerido
`score = w_cap*norm(trackingCapacity) + w_acc*probeAccuracy + w_res*DISTRACTION_RESISTANCE_INDEX` con `w_cap=0.5, w_acc=0.3, w_res=0.2`. `null` si accuracy ≈ azar o n insuficiente. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar sesgo de respuesta (proporción sí/no vs base real de sonda). RT de sonda < piso = adivinar. Trayectorias seed-deterministas → auditables. Bloque CONTAMINATED si accuracy = azar.

### Variantes
1. MOT clásico (puntos idénticos).
2. MOT con identidad (MOMIT: además recordar qué target era cuál — añade carga de memoria, secundaria).
3. MOT con oclusión (objetos pasan tras barras — fricción deseable de mantenimiento).

### UX
Marcado inicial claro de targets (flash), movimiento fluido, sonda destacada al final. Zona de respuesta sí/no grande. Duración 4–6 min (exigente, bloques cortos). Microanimación: la propia tarea (funcional). `cos/sin` → `.toFloat()` en `Canvas`.

### Implementación KMP
Reusa `FocusTrackingField` (E9). Aproximación: el holder espera UNA pulsación (no captura sub-trial). `TrialClassifier.classify` (sonda = target sí/no). Métricas vía `DefaultMetricsEngine` con índice de capacidad derivado del staircase. Adaptación `DefaultAdaptiveEngine`/staircase. Persistencia: `data class MotSummary(schemaVersion, trackingCapacity:Double?, probeAccuracy:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo (reusa Tracking). NOTA: tracking continuo real (multi-respuesta) requeriría reescribir el runtime `presentAndAwait` — fuera de alcance; se queda en juicio de sonda.

### Riesgos
Carga alta → fatiga/abandono rápido. Una sonda por trial da poca info → requiere muchos trials para umbral fiable. Rendimiento del `Canvas` en dispositivos lentos puede contaminar timing.

### Revisión crítica
El Revisor exige: (1) sonda balanceada target/no-target o el sesgo de respuesta infla accuracy; (2) suficientes trials por nivel para umbral fiable o `null`; (3) verificar frame-rate del `Canvas` (drops = timing falso) y marcar CONTAMINATED; (4) bloques cortos por fatiga; (5) no afirmar transferencia a conducción/deporte. Con una sonda por trial el umbral converge lento — exigir min n explícito.

### Veredicto
Aprobado con cambios (sonda balanceada, min-n para umbral, guardia de frame-rate, copy honesto sin claims de conducción).

---

## Cazador Visual (Búsqueda visual con pendiente de set-size)

### Módulo
Principal: Enfoque (atención selectiva / búsqueda visual eficiente). Secundarios: Velocidad, Decisiones (criterio de presencia/ausencia).

### Objetivo cognitivo
Entrenar búsqueda visual y medir la pendiente de RT por tamaño de conjunto (`SET_SIZE_SLOPE`), distinguiendo búsqueda eficiente (paralela, pendiente plana) de ineficiente (serial). Sub-componente: eficiencia del despliegue atencional.

### Hipótesis de transferencia
SÍ: near transfer a tareas de búsqueda y filtrado visual. NO: afirmar "mejor atención al detalle" general o productividad. Far = hipótesis.

### Loop principal
1. Estímulo: array de N items; target presente o ausente entre distractores (`StimulusPayload.Search`).
2. Decisión: target presente (tap/LEFT) vs ausente (withhold/RIGHT).
3. Feedback: inmediato.
4. Adaptación: ajusta set size, similitud target/distractor, heterogeneidad.
5. Siguiente trial.

### Reglas
Responder "presente"/"ausente". `SET_SIZE_SLOPE` calculada sobre HIT-con-RT por set size (regresión RT vs set size). Mezcla de presente/ausente balanceada.

### Inputs
`FocusFieldState.Search` + `FocusSearchField` (existe). Tap presente / o mitades táctiles presente/ausente.

### Variables de dificultad
1. `setSizeLevels` p. ej. {4,8,16,24} (rango 4–32).
2. `targetDistractorSimilarity` 0.0–0.85 (alta = serial).
3. `distractorHeterogeneity` 0.0–1.0 (heterogéneo = más difícil).
4. `targetPresentRatio` 0.4–0.6.
5. `responseWindowMs` 900–2500 ms (búsqueda lleva más tiempo).
6. `featureConjunction` bool (feature search vs conjunction search).
7. `itemSizeDp` 16–32.

### Algoritmo adaptativo
Gobernante: accuracy + `SET_SIZE_SLOPE`. Banda accuracy ≈0.85. Pendiente plana y accuracy alta → StepUp (sube similitud → conjunción). Errores de "ausente" falsos (terminar búsqueda pronto) → ajustar deadline/`ContainImpulsivity`. Fatiga → `Cooldown`. SwitchAxis entre similitud y heterogeneidad. Baseline ancla set sizes y similitud.

### Métricas primarias
`SET_SIZE_SLOPE` (ms/item, presente y ausente por separado), accuracy, intercepto de RT.

### Métricas secundarias
`RT_MEDIAN` por set size, `RT_CV`, miss rate (no encontrar presente), false-absent rate, `IES`.

### Señales de fatiga o mala calidad
Pendiente que se aplana por adivinar (accuracy cae); RT de "ausente" sistemáticamente corto (no busca); RT_CV alto; respuestas a un solo veredicto; n por set size insuficiente → `null`.

### Scoring sugerido
`score = w_acc*accuracy + w_eff*(1 - norm(setSizeSlope)) + w_stab*(1 - RT_CV)` con `w_acc=0.5, w_eff=0.3, w_stab=0.2`. `setSizeSlope=null` si <2 set sizes con RT válido o R² de regresión bajo → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Detectar "ausente sin buscar" (RT_ausente << RT esperado por set size). Balanceo presente/ausente. Regresión con R² mínimo o pendiente no fiable. Arrays seed-deterministas auditables.

### Variantes
1. Feature search (target difiere en 1 característica — pendiente plana esperada).
2. Conjunction search (target = conjunción de 2 features — serial).
3. Búsqueda de ausencia de característica (target = el que NO tiene X — fricción deseable).

### UX
Array claro, items >16 dp con suficiente separación. Veredicto presente/ausente con dos zonas táctiles grandes. Feedback semántico. Duración 4–6 min. Microanimación: resaltado breve del target tras responder (aprendizaje).

### Implementación KMP
Reusa `FocusSearchField` (E7). `SearchSlopeGenerator: TrialGenerator`. `TrialClassifier.classify` (presente/ausente) o `classifyChoice`. `DefaultMetricsEngine` con `SET_SIZE_SLOPE` (regresión sobre HIT-RT por set size). `DefaultAdaptiveEngine`. Persistencia: `data class SearchSummary(schemaVersion, setSizeSlopePresentMs:Double?, setSizeSlopeAbsentMs:Double?, accuracy:Double?, n:Int, confident:Boolean)`. No requiere renderer nuevo.

### Riesgos
Pendiente exige varios set sizes con suficientes trials → sesiones largas. Confusión con agudeza visual si items muy pequeños. "Ausente" estratégico difícil de detectar.

### Revisión crítica
El Revisor exige: (1) ≥3 set sizes con n suficiente o pendiente `null`; (2) reportar pendiente presente/ausente por separado (la ausente es ~2× la presente en serial); (3) R² mínimo para la regresión o no afirmar pendiente; (4) detección de "ausente sin buscar"; (5) tamaño de item por encima de umbral perceptual. Sin set sizes suficientes y R², la pendiente es ruido.

### Veredicto
Aprobado con cambios (≥3 set sizes con min-n, pendientes presente/ausente separadas con R², gate anti-"ausente sin buscar").

---

## Reenganche Atencional (Sostenida con recuperación post-lapso)

### Módulo
Principal: Enfoque (control atencional dinámico / recuperación tras divagación). Secundarios: Metacognición (detección de propio lapso), Velocidad.

### Objetivo cognitivo
Entrenar la detección y recuperación rápida tras un lapso atencional (mind-wandering), midiendo la latencia de reenganche y la frecuencia de lapsos. Sub-componente: meta-atención (notar que la atención se fue) y recuperación. Inspirado en gradCPT/SART pero rediseñado para medir reenganche, no solo lapsos.

### Hipótesis de transferencia
SÍ: near transfer a tareas de atención sostenida con detección de lapso. NO: afirmar reducción de mind-wandering en vida real, mindfulness clínico o concentración general. Far = hipótesis.

### Loop principal
1. Estímulo: flujo continuo gradual (transición suave entre estímulos, `StimulusPayload.Reengagement`) con go frecuente + no-go raro.
2. Decisión: responder al go habitual, retener en no-go raro; tras un error de commission (lapso), un "probe de reenganche" mide la recuperación.
3. Feedback: inmediato + probe metacognitivo ocasional ("¿estabas enganchado?").
4. Adaptación: ajusta tasa de transición, ratio no-go, frecuencia de probe.
5. Siguiente trial.

### Reglas
Go = responder; no-go raro = retener. Commission en no-go = lapso. `reengagementLatency` = nº de trials/ms hasta recuperar RT_CV/accuracy basal tras lapso. Probe metacognitivo intermitente (`SubjectiveState`).

### Inputs
`FocusFieldState.Reengagement`/Anchor (existe `FocusAnchorField`). Tap go + probe ocasional (tap binario sí/no en mitades).

### Variables de dificultad
1. `goRate` 0.80–0.92 (más go = más automatismo = más lapsos).
2. `noGoRatio` 0.08–0.20.
3. `transitionMs` 400–1200 (transición gradual; lenta = más divagación).
4. `responseWindowMs` 600–1100 ms.
5. `probeFrequency` cada 15–60 trials.
6. `blockLengthTrials` 60–140 (largo para inducir mind-wandering).
7. `stimulusMonotony` 0.0–1.0 (mayor monotonía = más lapsos).

### Algoritmo adaptativo
Gobernante: lapse rate + `reengagementLatency`. Banda: mantener commission ≈0.10–0.20 (suficiente para medir reenganche sin frustrar). Recuperación rápida y accuracy estable → StepUp (sube `goRate`/monotonía). Lapsos excesivos → StepDown. Fatiga (clave aquí) → `Cooldown`/`StopSession`. Baseline ancla `goRate`. `MicroPauseGate` anti-precipitación para impulsivos.

### Métricas primarias
`reengagementLatency` (post-error recovery), lapse rate (commission en no-go), `POST_ERROR_SLOWING`/`POST_ERROR_ACCURACY`.

### Métricas secundarias
`RT_CV` (variabilidad = proxy de divagación), `SUSTAINED_FOCUS_INDEX`, alineamiento confianza-error (probe vs accuracy real), `VIGILANCE_DECREMENT_SLOPE`.

### Señales de fatiga o mala calidad
RT_CV creciente + lapsos en racha (desenganche total, no lapso puntual); probe metacognitivo desalineado de forma extrema; go-omissions altas; `SubjectiveState` adverso → marcar y posiblemente abortar.

### Scoring sugerido
`score = w_lapse*(1 - lapseRate) + w_recov*(1 - norm(reengagementLatency)) + w_meta*confidenceErrorAlignment` con `w_lapse=0.4, w_recov=0.35, w_meta=0.25`. `null` si n_noGo o n probes insuficiente. `preliminary=true`.

### Antitrampa / calidad de datos
Diferenciar lapso (commission aislada con RT_CV elevado alrededor) de error perceptual. Detectar "responder a todo" (no inhibe nunca) vs desenganche total (omisiones en racha). Probe honesto: comparar auto-reporte vs accuracy. Bloque CONTAMINATED si desenganche sostenido.

### Variantes
1. gradCPT-style (transición gradual de imágenes; no-go = categoría rara).
2. SART-numérico (no-go = dígito raro; neutro cultural).
3. Con probe de pensamiento (thought-probe explícito intermitente — refuerza señal metacognitiva).

### UX
Flujo suave y monótono por diseño (induce divagación medible), no aburrido-frustrante. Probe breve y no intrusivo. Feedback de recuperación positivo. Duración 5–8 min (necesita longitud). Microanimación: transición gradual (funcional, la tarea).

### Implementación KMP
Reusa `FocusAnchorField`/`Reengagement`. `ReengageGenerator: TrialGenerator`. `TrialClassifier.classify` (go/no-go) + cálculo de `reengagementLatency` en `FocusSessionAssembler` (ventana post-error). `DefaultMetricsEngine` con `POST_ERROR_*`. `DefaultAdaptiveEngine` + `MicroPauseGate`. Persistencia: `data class ReengageSummary(schemaVersion, lapseRate:Double?, reengagementLatencyMs:Double?, confidenceAlignment:Double?, n:Int, confident:Boolean)`. Probe metacognitivo via `SubjectiveState`. No requiere renderer nuevo (reusa Anchor/Reengagement).

### Riesgos
Inducir mind-wandering deliberadamente raya la línea de "dificultad artificial": debe ser monotonía controlada y medida, no aburrimiento gratuito. Reengagement latency con pocos lapsos = inestable. Probe puede sesgar la atención (acto de medir altera).

### Revisión crítica
El Revisor exige: (1) demostrar que la monotonía es una `DifficultyAxis` medida (`stimulusMonotony`), no aburrimiento sin métrica — si no, es dificultad artificial prohibida; (2) `reengagementLatency` `null` si <N lapsos; (3) separar desenganche total de lapso puntual o la métrica mezcla dos cosas; (4) probe metacognitivo no debe contaminar el flujo (frecuencia baja); (5) gate de fatiga fuerte por la longitud. El mayor riesgo: medir tolerancia al aburrimiento en vez de reenganche. Exige validar que reengagementLatency correlaciona con recuperación de RT_CV, no con tedio.

### Veredicto
Aprobado con cambios (monotonía como eje medido, null estricto de reengagement con pocos lapsos, separar desenganche total, probe de baja frecuencia, gate de fatiga reforzado).

---

## Doble Vía Conflictiva (Dual-task con coste de interferencia central)

### Módulo
Principal: Enfoque (atención dividida / cuello de botella central). Secundarios: Velocidad (RT bajo carga), Memoria (mantenimiento concurrente).

### Objetivo cognitivo
Entrenar la gestión de dos tareas concurrentes y medir `DUAL_TASK_COST` (degradación al hacer dos tareas vs una sola), exponiendo el cuello de botella de procesamiento central. Sub-componente: coste de coordinación atencional. (Honestidad: NO entrena "multitarea" como habilidad; mide y reduce el coste, que es lo único defendible.)

### Hipótesis de transferencia
SÍ: near transfer a tareas dual-task con estructura similar; reducción del coste medible. NO: afirmar capacidad de multitarea general (mito); productividad. Far = hipótesis explícita. Copy debe desmentir el mito de multitarea.

### Loop principal
1. Estímulo: tarea primaria (go/no-go) + tarea secundaria concurrente (conteo/monitoreo, `StimulusPayload.DualTask`), con bloques single y dual entrelazados.
2. Decisión: responder a la primaria; la secundaria corre en paralelo (animación en composable keyed por `layoutSeed`).
3. Feedback: inmediato sobre la primaria; resultado de la secundaria al final del trial.
4. Adaptación: ajusta carga de la secundaria, SOA, ratio dual/single.
5. Siguiente trial.

### Reglas
Bloques single-task (baseline) vs dual-task. `DUAL_TASK_COST` = (RT/accuracy_dual − RT/accuracy_single) con HIT-RT en ambas condiciones. Una respuesta primaria por trial (limitación del runtime); secundaria evaluada por resultado final, no por sub-respuestas.

### Inputs
`FocusFieldState.Dual` + `FocusDualTaskField` (existe; contador concurrente). Tap primario único; secundaria reportada al cierre del bloque.

### Variables de dificultad
1. `secondaryLoad` 0–5 (carga de la tarea concurrente).
2. `dualRatio` 0.3–0.7 (proporción de trials dual).
3. `stimulusOnsetAsyncMs` 0–600 (SOA entre tareas; corto = más interferencia).
4. `primaryResponseWindowMs` 600–1200 ms.
5. `secondaryUpdateRateMs` 600–1800 (velocidad de la secundaria).
6. `interStimulusDelayMs` 500–1400 ms.
7. `secondaryModality` (visual/auditiva — distinta modalidad reduce conflicto).

### Algoritmo adaptativo
Gobernante: `DUAL_TASK_COST` + accuracy primaria. Banda accuracy primaria ≈0.80. Coste bajo y estable → StepUp (sube `secondaryLoad`/baja SOA). Si la secundaria se abandona (accuracy secundaria ≈ azar) → no premiar (gate: ambas tareas deben tener accuracy mínima). Fatiga → `Cooldown`. Baseline ancla `secondaryLoad`.

### Métricas primarias
`DUAL_TASK_COST` (RT y accuracy, ambas condiciones con datos válidos), accuracy primaria dual, accuracy secundaria.

### Métricas secundarias
`RT_MEDIAN` single vs dual, `RT_CV`, `IES`, trade-off entre tareas (prioriza una a costa de otra), `FATIGUE_INDEX`.

### Señales de fatiga o mala calidad
Abandono de la secundaria (accuracy secundaria = azar) → dual cost falso; sacrificio extremo de una tarea; RT_CV alto en dual; n single o dual insuficiente → `null`.

### Scoring sugerido
`score = w_cost*(1 - norm(dualTaskCost)) + w_bal*taskBalance + w_acc*min(accPrimary,accSecondary)` con `w_cost=0.4, w_bal=0.25, w_acc=0.35`; `taskBalance` penaliza sacrificar una tarea. `dualTaskCost=null` si falta condición válida o accuracy secundaria=azar → score `null`. `preliminary=true`.

### Antitrampa / calidad de datos
Gate central: ambas tareas con accuracy ≥ umbral, si no el "coste bajo" es por abandonar la secundaria. Detectar priorización extrema (`taskBalance` bajo). Single y dual balanceados. Bloque CONTAMINATED si secundaria ignorada.

### Variantes
1. Visual+visual (go/no-go + conteo de eventos visuales).
2. Visual+auditiva (cross-modal, menor conflicto — comparación de modalidad).
3. Go/no-go + n-back ligero (secundaria de memoria de trabajo — más cuello de botella).

### UX
Dos zonas claras (primaria central, secundaria periférica/contador). Feedback primario inmediato; secundario al cierre. Duración 4–6 min. Microanimación: actualización del contador secundario (funcional). Evitar sobrecarga visual que mida agudeza.

### Implementación KMP
Reusa `FocusDualTaskField` (E8). `DualConflictGenerator`. `TrialClassifier.classify` para primaria; secundaria evaluada por el `FocusSessionAssembler` al ensamblar el bloque (resultado agregado). `DefaultMetricsEngine` con `DUAL_TASK_COST`. `DefaultAdaptiveEngine` con gate de accuracy secundaria. Persistencia: `data class DualSummary(schemaVersion, dualTaskCostMs:Double?, accPrimaryDual:Double?, accSecondary:Double?, taskBalance:Double?, n:Int, confident:Boolean)`. LIMITACIÓN: el runtime `presentAndAwait` es de una respuesta por trial; la secundaria real multi-respuesta requeriría reescribir el runtime — aquí la secundaria se evalúa por resultado final agregado, no por captura sub-trial. No requiere renderer nuevo.

### Riesgos
El runtime de una respuesta limita la fidelidad dual-task real (la secundaria no captura sub-eventos). Riesgo de medir abandono en vez de coordinación. Tentación de afirmar "multitarea". Carga visual puede confundir con percepción.

### Revisión crítica
El Revisor es el más duro aquí: (1) sin gate de accuracy secundaria, el "coste bajo" premia abandonar la secundaria — métrica destruida; obligatorio; (2) la secundaria evaluada solo al final es una aproximación débil: documentar que NO es dual-task psicofísico estricto y NO sobre-afirmar; (3) `taskBalance` obligatorio para detectar priorización; (4) copy DEBE desmentir el mito de multitarea o viola fronteras de claims; (5) dual cost `null` si falta condición. Si no se puede garantizar engagement de la secundaria, el Revisor recomienda degradar a "Aprobado con cambios" estricto, no aprobado limpio.

### Veredicto
Aprobado con cambios (gate obligatorio de accuracy secundaria, `taskBalance`, copy anti-mito-multitarea, documentar la aproximación de secundaria-agregada y null honesto de dual cost).

---



# 3.2 Velocidad (8 ejercicios)

# MindBoost Velocidad — Catálogo de 8 ejercicios de calidad suprema

> Fronteras de seguridad vinculantes: perfil cognitivo funcional, práctica contra baseline personal, dificultad adaptativa. Nada de diagnóstico/IQ. Toda barra sin soporte estadístico = `null` (em-dash), nunca 0. `CognitiveProfileScore.preliminary=true` mientras el gate físico §3/§15 siga abierto (timebase autoritativo solo Android API34+). Todos reusan `XorShiftSpeedRng` determinista, `SpeedMeasurementStore` (corrida = unidad atómica), `MetricCalculator`/`FatigueIndex`/`StaircaseEngine`/`SpeedScoreEngine`.

---

## 1. Discriminación Cromática Binaria (Tinte Crítico)

### Módulo
Principal: Velocidad (procesamiento perceptivo-decisional). Secundarios: Enfoque (control inhibitorio de respuesta), Profile (dimensión PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad de decisión perceptiva pura en 2-AFC: minimizar el tiempo de transducción estímulo→categoría→respuesta motora cuando la evidencia perceptiva es nítida, y medir el coste de degradar esa evidencia (separación perceptiva pequeña). Aísla velocidad de codificación de bajo nivel, casi libre de carga semántica.

### Hipótesis de transferencia
SÍ podría transferir (near): otras tareas 2-AFC perceptivas y de tiempo de reacción de elección simple; reducción de RT-mediano en tareas con decisión binaria nítida. NO se debe afirmar: mejora de "velocidad mental general", lectura, conducción, ni transferencia far a tareas semánticas/atencionales sostenidas. No es agudeza visual clínica.

### Loop principal
1. Estímulo: un parche central con un tinte (p.ej. azul-verdoso) cuya separación respecto al eje de decisión es `deltaPerceptual`; etiquetas LEFT="más frío" / RIGHT="más cálido" fijas toda la sesión.
2. Decisión: tap en mitad izquierda/derecha de pantalla (paradigma choice) dentro de `deadlineNanos`.
3. Feedback: borde verde (HIT) / rojo (error de lado) / ámbar (sin respuesta) <150 ms; sin texto.
4. Adaptación: `MultiSignalStaircaseEngine.evaluate(BlockMetrics)` ajusta CCE→`deadline` y `deltaPerceptual`.
5. Siguiente trial: ISI jitter 400–900 ms para romper ritmo predecible.

### Reglas
Responder al lado correcto según el tinte. No hay no-go. Anticipación (rt<antThreshold) se descarta del cómputo de tendencia central pero se cuenta como evento. Sesión = 4–6 bloques de 24 trials.

### Inputs
Tap (mitad izquierda/derecha = `SpeedChoice.LEFT/RIGHT`). Sin teclado/voz/texto.

### Variables de dificultad
1. `deltaPerceptual` (separación de tinte respecto al borde de decisión): 40→4 unidades CIELAB ΔE.
2. `deadlineNanos`: 1200→450 ms (gobernado por CCE).
3. `parchSizeDp`: 160→64 dp (estímulo más pequeño = codificación más difícil).
4. `isiJitterMs`: 200→700 ms de rango (mayor jitter = menos preparación).
5. `distractorRingPresent`: 0/1 (anillo cromático periférico no-objetivo).
6. `luminanceNoise`: 0.0→0.25 (ruido de luminancia que obliga a usar solo el canal de tono).
7. `axisRotationDeg`: rotación del eje de decisión 0→30° (reduce aprendizaje de mapeo fijo).

### Algoritmo adaptativo
`MultiSignalStaircaseEngine` (1-up/3-down sobre pc, objetivo ≈0.79): COOLDOWN si `fatIdx>cap`; LOWER si `pc<banda` o `impIdx>cap` o `anticipationRate` alto; STABILIZE si `pc` alto pero `cv>cap` (sube ISI jitter, no baja deadline); RAISE recorta `deadline` y reduce `deltaPerceptual`; HOLD con histéresis. VCE NO autoriza avance. Baseline personal (`BaselineSnapshotDto`) ancla `deltaPerceptual` y deadline iniciales.

### Métricas primarias
pc (accuracy de lado), `rtMedian` sobre HIT-RT MAD-limpio, `vce` (velocidad-control), CCE alcanzado.

### Métricas secundarias
`cv` (RT-CV), `anticipationRate`, `impIdx` (`impulsivityIndexNoNoGo`), pendiente RT vs `deltaPerceptual` (función psicométrica de coste perceptivo), `fatIdx` longitudinal.

### Señales de fatiga o mala calidad
`FatigueIndex.compute` (≥3 bloques): subida de RT-mediano + subida de lapsos + caída de VCE. `anticipationRate` alto sostenido = adivinación/rítmica. `cv` creciente intra-sesión = desenganche. `DeviceTimingRisk` alto o timebase no-Android → `SessionQuality.LOW/CONTAMINATED`.

### Scoring sugerido
`usefulSpeed = clamp(VCE / VCE_ref_baseline)`; `control = (1 - impIdx) * pc`; `stability = (1 - cv) * (1 - lapseRate)`. `total = mean(usefulSpeed, control, stability)`; techo: si `pc<0.80` o `cv>0.30` → `total *= 0.5` (`ceilingPenaltyApplied=true`). `preliminary=true`. Barra `null` si `n<MIN_N` o no `confident`.

### Antitrampa / calidad de datos
Eje de decisión rotado (`axisRotationDeg`) y jitter de ISI rompen estrategias de ritmo. `impIdx` y `anticipationRate` detectan tap-spamming. Determinismo seeded permite re-simular la corrida. Solo timebase Android autoritativo cuenta para score no-preliminar.

### Variantes
1. Eje luminancia (claro/oscuro) en vez de tono → daltonismo-safe.
2. Eje saturación (apagado/vívido).
3. Triple-difícil con `luminanceNoise` máximo forzando uso exclusivo del canal de tono.

### UX
Pantalla: parche central grande sobre fondo neutro `AppColors`; dos zonas táctiles a pantalla completa (`MinTouchTarget` cumplido por construcción). Feedback de borde por token semántico <150 ms; el rojo de error añade forma (no solo hue) para daltonismo. Microanimación funcional: pulso de aparición del parche (onset claro), sin decoración. Duración ideal 3–4 min.

### Implementación KMP
Reusa renderer **choice** (mitades táctiles `SpeedChoice`). Generador nuevo `TinteGenerator : SpeedPromptGenerator` (deriva `correctChoice` del tinte vs eje; nunca lo expone), seeded con `XorShiftSpeedRng`. `FixedTrialEngine`→futuro adaptativo; `MetricCalculator.blockMetrics/sessionMetrics`; `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Persistencia: `MeasurementRun` en `SecureSpeedMeasurementStore`; DTO `@Serializable`+`schemaVersion`; registry de prompts. Modelo: `data class TintePrompt(val correctChoice: SpeedChoice, val deltaPerceptual: Float, val axisRotationDeg: Float)`. Eventos: stimulus onset uptime, response uptime, isCorrect, deadlineNanos, difficultyEstimate. Estado mínimo: `SpeedTrialPrompt` + deadline + RNG. Sin renderer nuevo.

### Riesgos
Dependencia de calibración de pantalla/daltonismo; ΔE percibido varía por dispositivo. Mitigación: variante luminancia, no depender solo de hue, `parchSize` y ruido como ejes alternativos.

### Revisión crítica
El Revisor exige: (1) eliminar el riesgo de que `deltaPerceptual` sea no comparable cross-device → anclar SIEMPRE al baseline personal del propio dispositivo (`DeviceProfile`), nunca cross-device, y marcar `preliminary` mientras el gate físico siga abierto; (2) demostrar que no es solo aprendizaje del mapeo fijo LEFT/RIGHT → exigir `axisRotationDeg` activo en ≥30% de trials; (3) probar que `anticipationRate` filtra realmente al adivinador (test con perfil sintético `SyntheticUserRunner` de tap aleatorio rápido debe puntuar bajo). Sin esas tres correcciones, mide familiaridad y reflejo, no velocidad perceptiva.

### Veredicto
Aprobado con cambios: añadir rotación de eje obligatoria, anclaje estricto al baseline por dispositivo y variante luminancia daltonismo-safe.

---

## 2. Sustitución Símbolo-Dígito Rápida (Cifrado Veloz)

### Módulo
Principal: Velocidad (rediseño premium del clásico DSST). Secundarios: Enfoque (mantenimiento de regla en memoria de trabajo), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad de procesamiento con una regla de mapeo símbolo→tecla mantenida activa: combina lectura del estímulo, consulta de la clave y respuesta motora. Mide throughput perceptivo-asociativo, no memorización (la clave es siempre visible y rota).

### Hipótesis de transferencia
SÍ (near): otras tareas de sustitución/codificación y throughput perceptivo-motor; mejora de RT-mediano en mapeos arbitrarios. NO afirmar: que el DSST "predice inteligencia" o salud cerebral; no es marcador clínico aunque el DSST se use en investigación.

### Loop principal
1. Estímulo: símbolo central + leyenda de clave (símbolo↔LEFT/RIGHT) visible arriba; la clave se REGENERA cada bloque para impedir memorizar.
2. Decisión: tap del lado que la clave asigna al símbolo, antes del deadline.
3. Feedback: borde verde/rojo <120 ms; sin texto.
4. Adaptación: `DefaultStaircaseEngine`/`MultiSignalStaircaseEngine` ajusta deadline y `keySize`.
5. Siguiente: ISI corto 250–500 ms (loop rápido tipo DSST).

### Reglas
La respuesta correcta se deriva consultando la leyenda visible; nunca se muestra como affordance (render honesto). Regla cambia por bloque. 6 bloques de 20.

### Inputs
Tap (mitades LEFT/RIGHT). Variante teclado opcional (2 teclas) en desktop.

### Variables de dificultad
1. `keySize` (nº de pares símbolo↔lado distintos en la leyenda): 2→8.
2. `deadlineNanos`: 1500→500 ms.
3. `symbolSimilarity` (parecido visual entre símbolos): baja→alta.
4. `legendStablePerBlock` (1) vs `legendShufflePerTrial` (0): refuerza derivación, no memoria.
5. `legendVisibleMs` antes de ocultar parcialmente: ∞→breve (introduce fricción deseable de recuperación).
6. `isiMs`: 500→250 ms.
7. `distractorSymbols`: 0→3 símbolos no-mapeados (false-target).

### Algoritmo adaptativo
1-up/3-down sobre pc. LOWER si `pc<banda`, `impIdx>cap` o `anticipation` alta. STABILIZE (sube ISI / baja `keySize`) si `cv>cap` con pc alto. RAISE recorta deadline y sube `keySize`. COOLDOWN por `fatIdx`. SWITCH/asimetría: subir `keySize` cuesta más que bajar. Baseline ancla `keySize` inicial.

### Métricas primarias
Throughput = trials correctos por minuto; `rtMedian` HIT MAD-limpio; pc.

### Métricas secundarias
`cv`, pendiente RT vs `keySize` (coste de carga de mapeo), `lapseRate`, `omissionRate`, `impIdx`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Caída de throughput intra-sesión + subida RT-mediano. `omissionRate` creciente = desenganche. `impIdx` alto con leyenda grande = adivinación. Timebase no autoritativo → preliminar.

### Scoring sugerido
`usefulSpeed=VCE/VCE_ref`; `control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; throughput escalado entra como modificador de `usefulSpeed`. Techo `pc<0.80`||`cv>0.30`→×0.5. `preliminary=true`.

### Antitrampa / calidad de datos
Regenerar leyenda por bloque mata la memorización; `legendShufflePerTrial` para casos de sospecha. `distractorSymbols` detecta respuesta-por-posición. Determinismo seeded reproducible.

### Variantes
1. Símbolo→dígito (mapeo a 1 de 2 dígitos) clásico.
2. Color→forma.
3. "Inversa": dado el lado, ¿qué símbolo? (recuperación inversa, fricción deseable).

### UX
Leyenda compacta arriba con tokens `AppSpacing`; símbolo grande centrado; dos zonas táctiles. Feedback de borde <120 ms. Microanimación: highlight del par de la leyenda al acertar (funcional, refuerza mapeo). Duración 3–5 min.

### Implementación KMP
Reusa renderer **choice** + un slot de leyenda (componente nuevo pequeño, no nuevo paradigma). Generador `CifradoGenerator` YA existe (E3) → extender con `keySize`/leyenda rotada. `XorShiftSpeedRng`; `MetricCalculator`; `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Persistencia `MeasurementRun`. Modelo: `data class CifradoPrompt(val symbolId:Int, val legend:Map<Int,SpeedChoice>, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, keySize, deadline. Estado: prompt+legend+deadline. Renderer existente + slot leyenda.

### Riesgos
Con `keySize` grande y leyenda visible, puede degenerar en búsqueda visual lenta (mide search, no speed). Mitigación: tope de `keySize`, medir pendiente RT vs keySize para separar componentes.

### Revisión crítica
El Revisor ataca: con leyenda siempre visible, a `keySize`≤4 esto se convierte en búsqueda+lectura, no en "velocidad" — el RT estará dominado por sacadas oculares. Exige: (1) reportar la pendiente RT/keySize como métrica primaria explícita para que el score no confunda search con speed; (2) capar `keySize` a 6 en runtime adaptativo y reservar 8 a variante de carga; (3) introducir `legendVisibleMs` para forzar codificación, no relectura infinita. Sin esto, duplica E7 (búsqueda) bajo otro nombre.

### Veredicto
Aprobado con cambios: elevar pendiente RT/keySize a métrica primaria, capar keySize en runtime y añadir ventana de leyenda para diferenciarlo de búsqueda.

---

## 3. Conmutación Perceptiva de Tarea (Multivía Veloz)

### Módulo
Principal: Velocidad (flexibilidad de set perceptivo bajo presión temporal). Secundarios: Enfoque (switch cost), Profile (PROCESSING_SPEED + control).

### Objetivo cognitivo
Velocidad de reconfiguración de regla: el mismo estímulo bidimensional (color+forma) se juzga por una dimensión u otra según un cue, midiendo el coste de cambiar de regla (switch cost) a alta velocidad. Distingue throughput estable de throughput-bajo-cambio.

### Hipótesis de transferencia
SÍ (near): otras tareas de switching perceptivo; reducción de switch-cost en mapeos análogos. NO afirmar: flexibilidad cognitiva "general", multitarea de la vida real, ni función ejecutiva clínica.

### Loop principal
1. Estímulo: figura con color (rojo/azul) y forma (círculo/cuadrado) + cue de regla persistente (icono "COLOR" o "FORMA").
2. Decisión: aplicar la regla activa → tap LEFT/RIGHT.
3. Feedback: borde <130 ms.
4. Adaptación: deadline + proporción de switch ajustados.
5. Siguiente: ISI 350–700 ms; secuencia de cues seeded (mezcla switch/repeat).

### Reglas
RULE_COLOR: rojo→izq, azul→der. RULE_SHAPE: círculo→izq, cuadrado→der. La regla se deriva del cue; nunca se muestra la respuesta. 6 bloques de 24.

### Inputs
Tap LEFT/RIGHT.

### Variables de dificultad
1. `deadlineNanos`: 1400→500 ms.
2. `switchRate` (proporción de trials switch): 0.2→0.6.
3. `cueStimulusIntervalMs` (CSI, tiempo cue→estímulo): 600→0 ms (menos preparación = más coste).
4. `dimensionCongruence`: proporción de estímulos incongruentes (la otra dimensión empuja al lado opuesto): 0→0.5.
5. `cueSalience`: cue grande/persistente → pequeño/breve.
6. `isiJitterMs`: 150→550 ms.
7. `ruleCount`: 2→3 dimensiones (añadir tamaño).

### Algoritmo adaptativo
1-up/3-down sobre pc global. COOLDOWN por `fatIdx`. LOWER si pc bajo / `impIdx` alto. STABILIZE si `cv>cap`. RAISE recorta deadline y/o sube `switchRate`/baja CSI. **SWITCH_COST solo válido con HIT-RT en ambos lados (switch y repeat)**; si falta un lado → `null`, y el motor no usa un switch-cost no fiable para decidir.

### Métricas primarias
`SWITCH_COST` = `rtMedian_switch − rtMedian_repeat` (solo HITs); pc; `rtMedian` global.

### Métricas secundarias
`cv`, efecto de congruencia (incongruente−congruente), `impIdx`, `lapseRate`, pendiente switch-cost vs CSI, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Switch-cost que explota intra-sesión + RT global creciente = fatiga ejecutiva. n insuficiente en celda switch/repeat → switch-cost `null`. `impIdx` alto = respuesta sin leer cue.

### Scoring sugerido
`control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; `usefulSpeed=VCE/VCE_ref` penalizado por switch-cost normalizado: `usefulSpeed*=(1 - clamp(switchCost/switchCost_ref)*w)`. Techo estándar. `preliminary=true`. Switch-cost `null` no penaliza (no fabrica).

### Antitrampa / calidad de datos
Cue derivado (no respuesta visible); incongruencia detecta respuesta por dimensión fija; `impIdx`/`anticipationRate` filtran tap rítmico. Secuencia seeded → balance switch/repeat reproducible y auditable.

### Variantes
1. Cue auditivo-visual (tono asociado a regla) para reducir lectura.
2. Tres reglas (color/forma/tamaño) = mayor reconfiguración.
3. "Voluntario": el usuario elige regla algunos trials (mide coste de selección endógena).

### UX
Cue persistente arriba con icono+texto (doble canal), estímulo central, mitades táctiles. Feedback borde <130 ms. Microanimación: el cue parpadea suave SOLO en trial switch (señal funcional). Duración 4–5 min.

### Implementación KMP
**Requiere lógica de cue/regla** análoga al `FocusFieldState.RuleSwitch` de Enfoque (E6) portada al runtime de Velocidad → renderer choice + slot de cue (componente nuevo ligero). Generador `MultiviaGenerator` YA existe (E6) → enriquecer payload con `colorIsRed/shapeIsCircle/activeRule` y derivar `correctChoice`. `XorShiftSpeedRng`; `MetricCalculator` (necesita exponer switch-cost o computarlo en capa de speed); `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Modelo: `data class MultiviaPrompt(val rule:Rule, val colorIsRed:Boolean, val shapeIsCircle:Boolean, val isSwitch:Boolean, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, isSwitch, ruleId, deadline. Estado: prompt+activeRule+deadline. Nota: `MetricCalculator` actual no computa SWITCH_COST → añadir cálculo en speed reusando el patrón de Enfoque (HIT-RT ambos lados).

### Riesgos
Switch-cost necesita muchos trials por celda para ser `confident`; sesiones cortas pueden dejarlo siempre `null`. Riesgo de medir lectura de cue, no reconfiguración, si CSI es largo. Mitigación: capar CSI, exigir n mínimo por celda.

### Revisión crítica
El Revisor exige: (1) garantizar n suficiente por celda switch/repeat antes de reportar SWITCH_COST — si la sesión es de 4 bloques×24 con 0.4 switch, hay ~38 switch HITs: justo en el límite; documentar `MIN_N` por celda y reportar `null` honesto cuando no llega; (2) no penalizar el score por un switch-cost `null` (ya cumplido); (3) probar que el efecto de congruencia no contamina el switch-cost → reportarlos separados. Además: `MetricCalculator` no tiene switch-cost hoy → es deuda real, no "reuso directo". Que el spec lo declare explícitamente.

### Veredicto
Aprobado con cambios: declarar el cómputo de SWITCH_COST como trabajo nuevo en speed, fijar MIN_N por celda y separar congruencia de switch-cost.

---

## 4. Compuerta Lógica Express (Go/No-Go Veloz Asimétrico)

### Módulo
Principal: Velocidad (velocidad de respuesta con freno selectivo). Secundarios: Enfoque (inhibición de prepotencia), Profile (control inhibitorio + speed).

### Objetivo cognitivo
Velocidad de go-response con coste de control: responder rápido a estímulos "go" frecuentes mientras se inhibe en "no-go" raros. Mide el equilibrio velocidad/impulsividad (no solo RT), capturando la curva velocidad-control.

### Hipótesis de transferencia
SÍ (near): otras tareas go/no-go y de RT con inhibición prepotente; mejora de control sin perder velocidad. NO afirmar: control de impulsos en la vida real, TDAH (PROHIBIDO diagnóstico), ni autorregulación clínica.

### Loop principal
1. Estímulo: símbolo "compuerta" verde (GO, ~80%) o rojo/forma distinta (NO-GO, ~20%).
2. Decisión: tap rápido si GO; retener si NO-GO.
3. Feedback: borde verde (GO acierto) / rojo (comisión NO-GO) / neutro (omisión GO) <130 ms.
4. Adaptación: deadline + `noGoRate` + similitud go/no-go.
5. Siguiente: ISI corto 300–600 ms para construir prepotencia.

### Reglas
GO→tap; NO-GO→withhold. Comisión = error de control; omisión = lapso/inatención (separadas). 6 bloques de 30 (más trials por la rareza del no-go). Renderer single-tap.

### Inputs
Tap (PRIMARY) sobre toda la pantalla; retención = no-input dentro de ventana.

### Variables de dificultad
1. `deadlineNanos` (ventana GO): 1200→450 ms.
2. `noGoRate`: 0.30→0.10 (más raro = más prepotente = más difícil inhibir).
3. `goNoGoSimilarity`: baja→alta (discriminación perceptiva del freno).
4. `isiMs`: 600→300 ms.
5. `goStreakBias`: rachas GO largas antes de NO-GO (aumenta prepotencia).
6. `stimulusDurationMs`: 600→200 ms (menos tiempo de discriminación).
7. `lureRate`: NO-GO perceptivamente parecido a GO (false-go).

### Algoritmo adaptativo
Tabla MultiSignal: COOLDOWN(`fatIdx`)>LOWER(`impIdx>cap` ← comisiones, pc bajo, `omissionRate` alta)>STABILIZE(`cv>cap`)>RAISE>HOLD. `impIdx` (`impulsivityIndexNoNoGo` NO aplica aquí; usar tasa de comisión real go/no-go) gobierna: si comisión alta, el motor baja `noGoRate`/sube similitud-no, NUNCA acelera. Asimetría: bajar (seguridad) > subir.

### Métricas primarias
`rtMedian` de GO-HIT MAD-limpio; pc; `faRate` (comisión NO-GO) — aquí SÍ existe no-go, a diferencia de E1/E6.

### Métricas secundarias
`omissionRate` (GO perdidos), `cv`, `anticipationRate`, post-error slowing (RT tras comisión), `lapseRate`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Subida conjunta de comisión + omisión = colapso de control. Anticipación alta = respuesta sin discriminar. RT GO creciente + comisión creciente = fatiga. Timebase no-Android → preliminar.

### Scoring sugerido
`control=(1-faRate)*pc` (comisión penaliza directamente); `usefulSpeed=VCE/VCE_ref` solo de GO-HIT; `stability=(1-cv)*(1-lapseRate)`. Techo: `pc<0.80`||`cv>0.30`||`faRate>0.30`→×0.5. Macro `FAST_IMPULSIVE` si VCE alta pero faRate alta. `preliminary=true`.

### Antitrampa / calidad de datos
Premiar velocidad sin penalizar comisión está PROHIBIDO → el score castiga `faRate`. `anticipationRate` filtra tap-spam (que dispararía comisión). `lureRate` evita estrategia "tap a todo verde-ish". Seeded reproducible.

### Variantes
1. Stop-signal real (E4 de Enfoque): señal de freno tardía → SSRT vía `SsrtCalculator` con `StopSignalTracker` (escalera online).
2. Go/no-go semántico-libre (forma/color).
3. "Doble compuerta": dos reglas de go alternas.

### UX
Símbolo central claro; verde=go, rojo+forma distinta=no-go (doble canal, daltonismo-safe). Feedback borde <130 ms; comisión muestra micro-shake funcional breve. Sin penalización por culpa (mensaje neutro). Duración 4–6 min.

### Implementación KMP
Reusa renderer **single-tap** (go/no-go = tap-or-withhold). Generador `CompuertaGenerator` YA existe (E7) → añadir `noGoRate`/`lureRate`/similitud. `TrialEngine`→adaptativo; `MetricCalculator` (faRate ya soportado donde hay no-go); `MultiSignalStaircaseEngine` con gate de comisión; `FatigueIndex`; `SpeedScoreEngine`. Variante stop-signal usa `SsrtCalculator.SsrtInput`+`StopSignalTracker` (de focus) con `trustworthy=false` si p(respond|signal)∉0.25..0.75. Modelo: `data class CompuertaPrompt(val isGo:Boolean, val isLure:Boolean, val correctChoice:SpeedChoice?)`. Eventos: onset/response/withhold, isCommission, isOmission, deadline. Estado: prompt+deadline+ventana. Sin renderer nuevo (variante stop reusa Stop de focus).

### Riesgos
Con `noGoRate` muy bajo, pocos trials no-go → `faRate` poco `confident`. Mitigación: más trials/bloque, reportar `faRate` `null` honesto si n no-go insuficiente; no inflar dificultad por debajo de n mínimo.

### Revisión crítica
El Revisor es agresivo: "go/no-go premia velocidad y solo descuenta comisión por score — pero el adaptativo podría seguir empujando RT a costa de inhibición si el gate de comisión está mal calibrado". Exige: (1) que el gate de comisión (`LOWER` por `impIdx/faRate`) tenga PRIORIDAD sobre RAISE en la tabla (ya está, documentarlo y testearlo con perfil sintético impulsivo); (2) `MIN_N` de no-go antes de reportar `faRate`; (3) que el techo incluya `faRate>0.30`, no solo pc/cv. Si no, es un generador de impulsividad recompensada — violación directa de "PROHIBIDO premiar impulsividad sin control".

### Veredicto
Aprobado con cambios: techo por faRate, gate de comisión prioritario testeado con sintético impulsivo, y faRate `null` honesto bajo n mínimo.

---

## 5. Cadencia Anticipatoria (Tiempo de Coincidencia)

### Módulo
Principal: Velocidad (timing motor/anticipación temporal). Secundarios: Enfoque (control de impulsividad temporal), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Precisión temporal predictiva: un objeto se desplaza hacia una línea-meta y el usuario debe tocar EN el instante de coincidencia. Mide error temporal con signo (adelanto/retraso) y su variabilidad, no RT reactivo. Castiga la impulsividad temporal por construcción (tocar antes = error grande).

### Hipótesis de transferencia
SÍ (near): tareas de coincidence-timing y anticipación de eventos predecibles. NO afirmar: reflejos deportivos generales, conducción, ni "tiempo de reacción" clásico (esto es predicción, no reacción).

### Loop principal
1. Estímulo: objeto que viaja a velocidad `v` hacia una meta; ETA conocido por trayectoria.
2. Decisión: tap en el instante de coincidencia (ventana de tolerancia `tol`).
3. Feedback: barra de error con signo (+adelanto/−retraso) y magnitud en ms <150 ms.
4. Adaptación: `v`, `tol`, visibilidad de la trayectoria.
5. Siguiente: jitter de velocidad/dirección.

### Reglas
Objetivo = |error temporal| mínimo. Tocar fuera de `tol` = fallo. Tocar muy antes (`occlusionViolation`) = impulsividad. 5 bloques de 20. **Renderer nuevo o reuso del paradigma dinámico** (animación dentro del composable keyed por seed; holder espera UNA pulsación, ya soportado en Enfoque MOT/RSVP).

### Inputs
Tap (timing). Sin elección de lado.

### Variables de dificultad
1. `objectSpeedPxPerS`: lento→rápido (ETA 1500→500 ms).
2. `toleranceMs` (ventana de acierto): ±200→±40 ms.
3. `occlusionFraction` (porción final de trayectoria oculta): 0→0.6 (obliga a extrapolar).
4. `velocityJitter`: 0→0.3 (rompe predicción simple).
5. `trajectoryCurvature`: recta→curva (predicción no lineal).
6. `isiJitterMs`: 200→600 ms.
7. `distractorObjects`: 0→2 (objetos señuelo que no son la meta).

### Algoritmo adaptativo
Staircase sobre |error temporal| (objetivo: mantener `tol`-hit ≈0.79). COOLDOWN(`fatIdx`). LOWER (sube `tol`, baja `v`) si error medio alto o sesgo de adelanto creciente (impulsividad temporal). STABILIZE si variabilidad de error (`cv` temporal) alta con media baja. RAISE recorta `tol`, sube `occlusionFraction`. Baseline ancla `v` inicial.

### Métricas primarias
`timingError_median` (con signo), `timingError_absMedian`, `tol-hit rate`.

### Métricas secundarias
`timingVariability` (SD del error = consistencia), sesgo direccional (adelanto vs retraso), `anticipationRate` (toques pre-occlusion), pendiente error vs `occlusionFraction`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Aumento de variabilidad temporal + deriva del sesgo. Sesgo de adelanto creciente = impulsividad/cansancio. Touch latency del dispositivo contamina (necesita timebase autoritativo Android API34+; en fallback → CONTAMINATED).

### Scoring sugerido
`stability = 1 - clamp(timingVariability / var_ref)`; `control = 1 - clamp(|biasAdelanto|/bias_ref)`; `usefulSpeed = tol-hit rate ponderado por estrechez de tol alcanzada`. `total=mean(...)`; techo si `tol-hit<0.80` o `variability>var_cap`→×0.5. `preliminary=true`. Barras `null` si n insuficiente.

### Antitrampa / calidad de datos
Oclusión + jitter impiden cronometrar visualmente el borde; el adelanto sistemático se penaliza (no se puede "tapear pronto" para ganar). El distractor castiga tap-a-cualquier-movimiento. Seeded reproducible. CRÍTICO: la latencia de input del dispositivo es un sesgo constante → se calibra contra el baseline del MISMO dispositivo (`DeviceProfile`), nunca cross-device.

### Variantes
1. Audio-coincidencia (tono que sube → tocar al pico) accesible.
2. Doble objeto: tocar cuando ambos coinciden.
3. Ritmo: serie de coincidencias periódicas (sincronización sostenida).

### UX
Pista/línea de meta clara, objeto contrastado; barra de error con signo y color semántico (verde centrado, ámbar/rojo a los lados) + magnitud numérica. Microanimación: la animación del objeto ES la tarea (funcional). Duración 3–4 min.

### Implementación KMP
**Requiere renderer dinámico** (animación en composable keyed por `layoutSeed=stimulus.index`, patrón MOT/RSVP de Enfoque; holder espera UNA pulsación — runtime de una respuesta por trial, SIN sub-eventos). En `Canvas`, `cos/sin` de `kotlin.math` → `.toFloat()` antes de `Offset`/radios. Timebase: `SpeedTimebaseHandle` (Choreographer `frameTimeNanos` para onset+posición, MotionEvent nanos para tap) es CRÍTICO aquí. `TimebasePolicy.computeRtNanos` se generaliza a `timingError = tapUptime − coincidenceUptime` (puede ser negativo = adelanto; NO lanzar, este signo es la señal). Generador nuevo `CadenciaGenerator` seeded. `MetricCalculator` necesita un cómputo de error temporal con signo (deuda nueva en speed). `FatigueIndex`; `SpeedScoreEngine` (mapear a usefulSpeed/control/stability). Persistencia `MeasurementRun`. Modelo: `data class CadenciaTrial(val coincidenceUptimeNanos:Long, val tolNanos:Long, val occlusionFraction:Float)`. Eventos: coincidenceUptime, tapUptime, signedErrorNanos, withinTol. Estado: trayectoria+coincidenceTime+tol.

### Riesgos
Latencia táctil del dispositivo = sesgo constante que invalida cross-device; sin timebase autoritativo no hay medida fiable. `computeRtNanos` actual LANZA si rt<0 → aquí el adelanto es negativo legítimo: requiere una política de timing con signo NUEVA (no reusar la de RT que prohíbe negativos).

### Revisión crítica
El Revisor lo casi-descarta: "`TimebasePolicy.computeRtNanos` lanza con rt negativo; tu señal central es el error negativo (adelanto). Reusar el motor de RT como está ROMPE el ejercicio." Exige: (1) una `TimingPolicy` con signo separada, documentada como trabajo nuevo, no "reuso"; (2) demostrar que el sesgo de latencia táctil se cancela por anclaje al baseline del dispositivo, y marcar CONTAMINATED en fallback desktop/iOS; (3) probar que la oclusión realmente fuerza extrapolación (sin oclusión, mide reacción visual, no predicción). Sin (1) no compila la semántica; sin (2) el score es ruido de hardware.

### Veredicto
Aprobado con cambios: implementar `TimingPolicy` con signo, anclaje estricto al baseline del dispositivo, oclusión obligatoria y marca CONTAMINATED fuera de Android autoritativo.

---

## 6. Resolución de Conflicto Veloz (Flanker Express)

### Módulo
Principal: Velocidad (velocidad de selección bajo interferencia). Secundarios: Enfoque (control atencional selectivo), Profile (speed + control).

### Objetivo cognitivo
Velocidad de respuesta cuando flancos distractores empujan a la respuesta opuesta: mide el efecto de congruencia (coste de filtrar interferencia) a alta velocidad. Aísla la eficiencia de selección perceptiva, no la mera velocidad reactiva.

### Hipótesis de transferencia
SÍ (near): otras tareas de interferencia tipo flanker/Stroop perceptivo; reducción del efecto de congruencia. NO afirmar: atención "general", filtrado en entornos reales, ni control ejecutivo clínico.

### Loop principal
1. Estímulo: objetivo central flanqueado por distractores congruentes (→→→→→) o incongruentes (→→←→→).
2. Decisión: responder según la flecha CENTRAL (LEFT/RIGHT) antes del deadline.
3. Feedback: borde <120 ms.
4. Adaptación: deadline, proporción incongruente, separación de flancos.
5. Siguiente: ISI 300–600 ms.

### Reglas
Responder solo al centro, ignorar flancos. Incongruente induce error de comisión hacia el lado del flanco. 6 bloques de 24. Renderer choice.

### Inputs
Tap LEFT/RIGHT.

### Variables de dificultad
1. `deadlineNanos`: 1300→500 ms.
2. `incongruentRate`: 0.3→0.6.
3. `flankerTargetDistanceDp`: lejos→cerca (más cerca = más interferencia): 48→8 dp.
4. `flankerCount`: 2→6.
5. `targetSizeRatio` (centro vs flancos): grande→similar (menos saliente).
6. `isiJitterMs`: 150→500 ms.
7. `responseStimulusInterval`: introduce conflicto de secuencia (Gratton) controlado.

### Algoritmo adaptativo
1-up/3-down sobre pc. COOLDOWN(`fatIdx`). LOWER si pc bajo / `impIdx` alto (responder al flanco). STABILIZE si `cv>cap`. RAISE recorta deadline / sube `incongruentRate` / acerca flancos. **CONGRUENCY_EFFECT solo con HIT-RT en ambos lados** (congruente e incongruente); si falta uno → `null`.

### Métricas primarias
`CONGRUENCY_EFFECT` = `rtMedian_incong − rtMedian_cong` (solo HITs); pc; `rtMedian` global.

### Métricas secundarias
`cv`, comisión en incongruentes (`faRate` direccional), `anticipationRate`, post-conflicto adjustment (Gratton), `lapseRate`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Efecto de congruencia que crece intra-sesión + comisión incongruente creciente = fatiga de control. n insuficiente en celda → efecto `null`. Anticipación alta = adivinación.

### Scoring sugerido
`control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; `usefulSpeed=VCE/VCE_ref` penalizado por congruency-effect normalizado: `*=(1-clamp(congrEffect/ref)*w)`. Techo estándar. `preliminary=true`. Efecto `null`→no penaliza.

### Antitrampa / calidad de datos
Respuesta derivada del centro (no affordance); incongruencia detecta respuesta-al-flanco; `impIdx`/`anticipationRate` filtran tap rítmico. Seeded balance congruente/incongruente reproducible.

### Variantes
1. Stroop perceptivo (palabra COLOR vs tinta) — requiere adaptación idioma → usar formas/flechas para evitar dependencia cultural.
2. Simon (posición irrelevante vs respuesta).
3. Flanker con carga (más flancos + menor tamaño objetivo).

### UX
Fila de flechas, centro destacado sutilmente (sin revelar respuesta), mitades táctiles. Feedback borde <120 ms. Microanimación: ninguna decorativa; solo feedback. Duración 4–5 min.

### Implementación KMP
Reusa renderer **choice** (mitades LEFT/RIGHT). Generador nuevo `FlankerExpressGenerator` seeded (deriva `correctChoice` del centro). `MetricCalculator` necesita cómputo de CONGRUENCY_EFFECT en speed (deuda; patrón de Enfoque E2 reusable). `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Modelo: `data class FlankerPrompt(val centerDir:SpeedChoice, val congruent:Boolean, val flankerCount:Int, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, congruent, deadline. Estado: prompt+deadline. Sin renderer nuevo; deuda de métrica de congruencia.

### Riesgos
Solapa conceptualmente con E3 (switch) y E4 (go/no-go) en "control". Diferenciador real: aquí la interferencia es ESPACIAL-perceptiva simultánea, no temporal ni de regla. Riesgo: congruency-effect `null` en sesiones cortas.

### Revisión crítica
El Revisor: "¿qué aporta sobre la conmutación (#3) y la compuerta (#4)? Los tres miden 'control a velocidad'." Exige diferenciación medible: (1) #6 mide interferencia perceptiva simultánea (congruency-effect), #3 reconfiguración secuencial (switch-cost), #4 inhibición de prepotencia (comisión no-go) — declarar que las tres métricas son ORTOGONALES y que el brain-profile las trata como sub-señales distintas, no redundantes; (2) usar flechas/formas, NO palabras-color, para evitar dependencia idiomática (PROHIBIDA); (3) MIN_N por celda. Sin (1) es redundante; sin (2) viola la regla cultural.

### Veredicto
Aprobado con cambios: justificar ortogonalidad de la métrica vs #3/#4, estímulos no-lingüísticos obligatorios y MIN_N por celda.

---

## 7. Ráfaga de Detección Serial (Detección RSVP Veloz)

### Módulo
Principal: Velocidad (velocidad de detección perceptiva en flujo rápido). Secundarios: Enfoque (vigilancia breve), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad y umbral de detección de un objetivo dentro de una ráfaga serial rápida (RSVP) de no-objetivos: mide la mínima exposición a la que el usuario aún detecta y responde a tiempo. Throughput perceptivo bajo presión de presentación, no de respuesta.

### Hipótesis de transferencia
SÍ (near): tareas de detección rápida en flujo serial; reducción del umbral de exposición. NO afirmar: lectura veloz, percepción "general", ni atención sostenida prolongada (la ráfaga es breve).

### Loop principal
1. Estímulo: secuencia rápida de ítems (formas/letras-neutras) con 0–1 objetivo por ráfaga, a `itemDurationMs`.
2. Decisión: tap en cuanto aparece el objetivo (detección), antes de que termine la ventana.
3. Feedback: HIT/MISS/FALSE_ALARM (tap en no-objetivo) <150 ms.
4. Adaptación: `itemDurationMs`, densidad de distractores, similitud.
5. Siguiente: nueva ráfaga seeded.

### Reglas
Tap solo al objetivo. Tap a no-objetivo = false alarm. No detectar = omisión. 6 ráfagas-bloque de ~15 ítems. **Renderer Stream (RSVP)** ya existe en Enfoque (E10) → portar; holder espera UNA pulsación, animación en composable keyed por seed.

### Inputs
Tap (PRIMARY) durante la ráfaga.

### Variables de dificultad
1. `itemDurationMs` (SOA): 400→80 ms (más corto = más difícil detectar).
2. `distractorSimilarity`: baja→alta (objetivo se camufla).
3. `targetProbability`: 0.8→0.4 por ráfaga (a veces no hay objetivo → mide false alarms).
4. `burstLength`: 10→24 ítems.
5. `targetPositionJitter`: posición serial del objetivo aleatoria (evita anticipar).
6. `interItemMaskMs`: 0→60 ms de máscara (corta el procesamiento icónico).
7. `dualTargetRate`: 0→0.2 ráfagas con 2 objetivos (attentional blink controlado).

### Algoritmo adaptativo
Staircase sobre detección (hit rate objetivo ≈0.79) ajustando `itemDurationMs`. COOLDOWN(`fatIdx`). LOWER (sube `itemDurationMs`) si hit bajo o false-alarm alto. STABILIZE si variabilidad de RT-detección alta. RAISE recorta `itemDurationMs` / sube similitud. Baseline ancla `itemDurationMs` inicial.

### Métricas primarias
`hitRate` de detección, `rtMedian` de detección (onset-objetivo→tap, HIT MAD-limpio), umbral `itemDurationMs` al 79%.

### Métricas secundarias
`faRate` (taps a no-objetivo), `omissionRate`, sensibilidad d' (señal/ruido), `cv`, attentional-blink cost (en `dualTargetRate`), `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Caída de hitRate + subida de false-alarm intra-sesión. RT-detección creciente. False-alarm alto = tap-spam durante ráfaga. Timebase no-Android → onset de ítem no fiable → CONTAMINATED.

### Scoring sugerido
`usefulSpeed = 1 - clamp(umbralItemDuration / dur_ref)` (umbral más bajo = más rápido perceptivamente); `control=(1-faRate)*hitRate`; `stability=(1-cv)*(1-omissionRate)`. Techo si `hitRate<0.80`||`faRate>0.30`→×0.5. `preliminary=true`. d' como señal secundaria descriptiva.

### Antitrampa / calidad de datos
`targetPositionJitter` + `targetProbability<1` castigan tap-anticipado (false alarm en ráfagas sin objetivo). d' separa detección real de sesgo de respuesta. Seeded reproducible. Onset de cada ítem por `frameTimeNanos` (Choreographer) para RT de detección fiable.

### Variantes
1. Detección por color (objetivo = ítem de color único).
2. Conjunción (objetivo = forma+color específicos → detección más lenta).
3. Doble objetivo (attentional blink): dos objetivos cercanos en la ráfaga.

### UX
Ráfaga central a pantalla completa, máscara opcional entre ítems; tap a cualquier parte. Feedback borde + flash del ítem detectado <150 ms. Microanimación: la ráfaga ES la tarea (funcional). `layoutSeed=stimulus.index` para variación. Duración 3–4 min (ráfagas son intensas → bloques cortos anti-fatiga).

### Implementación KMP
**Reusa renderer Stream/RSVP** de Enfoque (E10) portado a Velocidad (animación en composable, una pulsación). Generador nuevo `RafagaGenerator` seeded (posición de objetivo, distractores). Timebase CRÍTICO: onset de ítem vía Choreographer `frameTimeNanos`; RT-detección = `tapUptime − targetOnsetUptime` (`TimebasePolicy.computeRtNanos`, ≥0). `MetricCalculator` necesita hit/fa/d' de detección serial (deuda nueva). `FatigueIndex`; `SpeedScoreEngine`. Persistencia `MeasurementRun`. Modelo: `data class RafagaTrial(val targetSerialPos:Int?, val itemDurationMs:Int, val items:List<ItemId>)`. Eventos: targetOnsetUptime, tapUptime, isHit/isFa/isOmission, itemDuration. Estado: ráfaga+targetPos+ventana. Renderer Stream reusado; deuda de métricas de detección.

### Riesgos
Onset de ítem depende del frame rate real; jank de UI contamina el SOA efectivo. En dispositivos con throttling, `itemDurationMs` nominal ≠ real. Mitigación: registrar `frameTimeNanos` reales por ítem y marcar CONTAMINATED si la varianza de frame es alta.

### Revisión crítica
El Revisor: "tu `itemDurationMs` nominal es una mentira si el dispositivo no renderiza a esa cadencia — un SOA de 80 ms con frames de 16.6 ms tiene granularidad de ±1 frame, y el jank lo arruina." Exige: (1) registrar el onset REAL de cada ítem por `frameTimeNanos` y computar SOA efectivo, no nominal; (2) marcar la ráfaga CONTAMINATED si la varianza de frame supera umbral (jank); (3) no permitir `itemDurationMs`<2 frames del dispositivo. Además: tap único durante ráfaga no distingue a cuál ítem responde → debe atribuirse al ítem visible en `tapUptime`, no al objetivo asumido. Sin (1)/(3), mide el GPU, no la percepción.

### Veredicto
Aprobado con cambios: SOA efectivo medido por frame real, gate de jank (CONTAMINATED), piso de `itemDurationMs` en frames del dispositivo y atribución de tap al ítem visible.

---

## 8. Calibración de Apuesta Temporal (Velocidad-Confianza Metacognitiva)

### Módulo
Principal: Velocidad (velocidad + calibración metacognitiva del propio RT). Secundarios: Enfoque (control de impulsividad), Profile (speed + metacognición).

### Objetivo cognitivo
Velocidad de respuesta acoplada a metacognición temporal: el usuario fija ANTES de cada bloque un "compromiso de deadline" (cuán rápido cree que puede ir manteniendo accuracy), y se mide el alineamiento entre velocidad prometida y rendimiento real. Entrena la calibración velocidad-exactitud, no solo la velocidad.

### Hipótesis de transferencia
SÍ (near): mejor calibración del trade-off velocidad-exactitud en tareas RT; reducción de over/under-confidence temporal. NO afirmar: metacognición "general", toma de decisiones de la vida real, ni rasgo clínico.

### Loop principal
1. Pre-bloque: el usuario elige un nivel de deadline-compromiso (lento/medio/rápido) → `committedDeadline`.
2. Estímulo: tarea 2-AFC simple bajo ESE deadline.
3. Decisión: tap LEFT/RIGHT dentro del `committedDeadline`.
4. Feedback: por trial (HIT/error); por bloque, alineamiento prometido-vs-real (¿cumpliste la velocidad SIN perder accuracy?).
5. Adaptación: las opciones de deadline ofrecidas se recentran sobre el umbral real del usuario.

### Reglas
Cumplir el deadline comprometido manteniendo pc≥banda. Over-commit (prometer rápido y fallar accuracy) penaliza calibración; under-commit (ir lento de sobra) también. 5 bloques de 20.

### Inputs
Selección (nivel de deadline pre-bloque) + tap LEFT/RIGHT en trials.

### Variables de dificultad
1. `committedDeadlineNanos` (elegido): rejilla 1200→450 ms.
2. `deadlineGridSpacing`: ancho→estrecho (opciones más finas = calibración más exigente).
3. `stimulusDiscriminability`: alta→baja (2-AFC más difícil).
4. `accuracyBand` requerida: 0.75→0.85.
5. `isiJitterMs`: 200→500 ms.
6. `commitGranularity`: 3→7 niveles de deadline ofrecidos.
7. `feedbackDelaySec`: inmediato→diferido (fricción metacognitiva).

### Algoritmo adaptativo
Doble: (a) staircase clásico sobre pc dentro del deadline comprometido (objetivo banda); (b) recentrado de la rejilla de opciones sobre el umbral RT real estimado del usuario (las opciones se vuelven más finas a medida que el baseline se estabiliza). COOLDOWN(`fatIdx`). LOWER si pc<banda bajo el compromiso. El motor NUNCA fuerza un deadline; solo recentra las opciones.

### Métricas primarias
`calibrationError` = |deadline prometido − RT-mediano real cumpliendo accuracy|; pc bajo compromiso; `rtMedian`.

### Métricas secundarias
`overCommitRate` (prometer rápido, fallar accuracy), `underCommitRate` (ir lento de sobra), `cv`, tendencia de calibración entre bloques, `impIdx`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Over-commit creciente + pc cayendo = fatiga/impulsividad. Under-commit sistemático = desenganche/evitación. Selección de deadline siempre máxima sin cumplir = gaming. Timebase no-Android → preliminar.

### Scoring sugerido
`stability=(1-cv)*(1-lapseRate)`; `control=(1-impIdx)*pc`; `usefulSpeed=VCE/VCE_ref`; **modificador metacognitivo**: `metacog=1-clamp(calibrationError/ref)`; `total=mean(usefulSpeed,control,stability)*metacog_weight + ...`. Techo estándar. `metacog` se reporta como señal descriptiva separada (no como velocidad). `preliminary=true`. `null` si insuficiente.

### Antitrampa / calidad de datos
Over-commit no se premia (penaliza calibración Y dispara techo por pc<banda). Under-commit detectado (deadline holgado con RT muy por debajo). El recentrado evita que el usuario "gane" eligiendo siempre lento. Seeded reproducible. `impIdx`/`anticipationRate` filtran tap-spam.

### Variantes
1. Confianza post-respuesta (juzgar acierto tras cada trial) en vez de pre-deadline → calibración de exactitud.
2. Apuesta de puntos por velocidad (sin dinero; gamificación honesta sin culpa).
3. Calibración adaptativa ciega (sin feedback inmediato, solo diferido).

### UX
Pantalla pre-bloque: selector de nivel de velocidad (3–7 chips, `MinTouchTarget`, `verticalScroll` si crecen). Trials: 2-AFC con mitades táctiles. Feedback de bloque: barra "prometido vs real" con color semántico (verde=calibrado, ámbar=over/under). Sin culpa: mensaje neutro de calibración. Duración 4–5 min.

### Implementación KMP
Reusa renderer **choice** + pantalla de selección pre-bloque (componente nuevo de selección, no nuevo paradigma de trial). `committedDeadline` alimenta `FixedTrialEngine`/adaptativo como `deadlineNanos` del bloque. Generador simple 2-AFC seeded (`XorShiftSpeedRng`). `MetricCalculator` + nueva señal `calibrationError` (deuda en speed). `MultiSignalStaircaseEngine` para pc; recentrado de rejilla en capa de presentación. `FatigueIndex`; `SpeedScoreEngine` con modificador metacognitivo. Persistencia: `MeasurementRun` registra `committedDeadlineNanos` por `BlockRecord` (campo nuevo). Modelo: `data class CalibracionBlock(val committedDeadlineNanos:Long, val accuracyBand:Float)`. Eventos: committedDeadline, RT real, pc bloque, calibrationError. Estado: deadline comprometido + trials. Sin renderer de trial nuevo; añade pantalla pre-bloque + métrica de calibración.

### Riesgos
La metacognición temporal es ruidosa y necesita varios bloques para `confident`; sesión única puede dejar `calibrationError` siempre `null`. Riesgo de gaming eligiendo siempre el deadline más holgado. Mitigación: penalizar under-commit, recentrar opciones, reportar `null` honesto.

### Revisión crítica
El Revisor es duro: "esto es un meta-ejercicio: si el usuario elige siempre el deadline más lento, cumple accuracy trivialmente y tu `calibrationError` es bajo — premiarías la cobardía." Exige: (1) penalizar explícitamente el under-commit (deadline holgado con RT muy por debajo del comprometido) además del over-commit; (2) recentrado de rejilla que ESTRECHE las opciones sobre el umbral real, eliminando la opción trivialmente lenta una vez estabilizado el baseline; (3) reportar `metacog` SEPARADO de velocidad para no contaminar el score de speed con una señal metacognitiva ruidosa; (4) requerir varios bloques antes de cualquier claim → `preliminary` + `null`. Sin (1)/(2) es explotable; sin (3) confunde dos constructos.

### Veredicto
Aprobado con cambios: penalizar under-commit, recentrar/estrechar la rejilla de opciones, separar la señal metacognitiva del score de velocidad y exigir multi-bloque antes de reportar.


---



# 3.3 Logica (10 ejercicios)

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

---



# 3.4 Lenguaje (8 ejercicios)

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

---



# 3.5 Memoria (9 ejercicios)

> Catálogo de diseño de 8 ejercicios de calidad suprema para el módulo **MindBoost · Memoria** (KMP).
> Anclado al código real: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/**`.
> Motores reusados por nombre: `ReliableChangeIndex` (Jacobson-Truax, `CRITICAL_Z = 1.96`),
> `RealImprovementCriterion` (delta en probe UNTRAINED), `TransferProtocol`/`TransferProtocols`,
> `PlateauDetector` + `InterventionPlanner`, `DefaultMemoryAdaptiveEngine` (vector de 8 ejes
> `MemoryDifficultyVector`, mueve UN eje por decisión), `MemoryMetricsEngine`/`MemoryMetricKey`,
> `MemorySessionSummary` (solo resúmenes), `SecureKeyValueStore` + `@Serializable` + `schemaVersion`,
> `EpochClock`, `RecentItemRegistry`/`TransferProbePartition`, wiring hub→`MemoryGraph.generators`
> (registry id→`MemoryTrialGenerator`)→`MemorySessionStateHolder`→`MemorySessionScreen`.
> Fronteras: producto NO clínico, NO diagnóstico, NO IQ garantizado. Lenguaje: "perfil cognitivo
> funcional", "práctica", "baseline personal", "dificultad adaptativa".

Los 3 ejercicios actuales jugables (EX1 Span Complejo, EX3 Lista con Estrategia, EX4 Pares Asociados)
permanecen; estos 8 son rediseños premium / nuevos que cubren memoria de trabajo verbal y visoespacial,
complex span con procesamiento concurrente, n-back adaptativo, binding asociativo, episódica con recall
diferido, orden serial, running memory (actualización) y una sonda de transferencia UNTRAINED. Ninguno
es una variación del mismo loop.

---

## Span Adaptativo Dual (rediseño premium de EX1)

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`MemorySubcapacity.WORKING_MEMORY`). Secundarios: M5_RESISTANCE
(`INTERFERENCE_RESISTANCE`), M6 (`METAMEMORY_CALIBRATION` vía overlay EX16).

### Objetivo cognitivo
Capacidad de mantenimiento activo de memoranda verbales bajo carga de una tarea de procesamiento
concurrente (paradigma complex-span clásico: operation span / reading span), forzando el componente
ejecutivo de la WM más allá del simple buffer fonológico.

### Hipótesis de transferencia  (qué SÍ podría transferir y qué NO se debe afirmar)
SÍ plausible: near-transfer a otras tareas de complex-span y a `WORKING_MEMORY_SPAN` medido en una
sonda UNTRAINED. POSIBLE far-transfer débil a comprensión lectora bajo carga (literatura mixta). NO se
afirma: aumento de IQ, mejora de "concentración general", ni beneficio clínico. La única prueba de
mejora real es el delta en la tarea UNTRAINED vía `RealImprovementCriterion.isGenuine`.

### Loop principal  (1 estímulo, 2 decisión, 3 feedback, 4 adaptación, 5 siguiente trial)
1. **Estímulo:** se presenta `Memorandum` (palabra) → `ProcessingTask` (verificación aritmética
   "a + b = shown", correcta ~50%) → siguiente memorandum, interleaved (`ComplexSpanTrial.steps`).
2. **Decisión:** el usuario juzga cada `ProcessingTask` (tap VERDADERO/FALSO con deadline) y, al final,
   recupera los memoranda en orden (`RecallType.SERIAL_FORWARD`/`SERIAL_BACKWARD`).
3. **Feedback:** inmediato por processing (✓/✗ con RT); diferido por recall (posiciones correctas,
   intrusiones, omisiones) al cerrar el trial.
4. **Adaptación:** `DefaultMemoryAdaptiveEngine.decide` mueve UN eje (gobernante `LOAD`).
5. **Siguiente trial:** nuevo plan con `excludeRecentWordIds` del `RecentItemRegistry`.

### Reglas
- Cada `ProcessingTask` debe responderse dentro de su deadline o cuenta como fallo de procesamiento
  (controla que el usuario no ignore la tarea concurrente para proteger los memoranda).
- El recall solo puntúa si la precisión de procesamiento ≥ 85% en el trial (umbral de validez): por
  debajo, el trial se marca de baja calidad y NO alimenta el staircase de span.
- Span gobernado por accuracy serial, no por RT.

### Inputs  (tap/swipe/drag/teclado/selección múltiple/voz opcional/texto libre)
Tap binario (VERDADERO/FALSO) en processing; recall por selección múltiple ordenada (tap secuencial
sobre un banco mezclado) o teclado opcional. Sin texto libre en V1 (scoring determinista).

### Variables de dificultad  (entre 5 y 10, con rangos concretos)
- `spanLength` (eje `LOAD`): **2–8** memoranda (`DifficultyDecode.decodeCount(load,2,8)`).
- `processingPerItem` (eje `CONCURRENCY`): **1–2** tareas por memorandum.
- `sameCategory` (eje `SIMILARITY`): false→true (≥0.5) ⇒ misma `WordCategory`, más confusable.
- `backwardRecall` (eje `RETRIEVAL`): false→true (≥0.5) ⇒ recall inverso.
- `processingDeadlineMs`: **3500→1800 ms** (presión de la tarea concurrente).
- `studyMsPerItem`: **1200→700 ms**.
- `interItemMaskMs` (eje `INTERFERENCE`): **0→400 ms** de máscara entre ítems.

### Algoritmo adaptativo  (accuracy, RT, errores, racha, fatiga, plateau, baseline personal)
Por bloque (no por trial), `MemoryPerformance.of(recentBlock,...)` → zona → `DefaultMemoryAdaptiveEngine`:
StepUp/StepDown sobre `LOAD` con histéresis (`cooldownBlocks`), pasos asimétricos (`stepBig` baja sin
histéresis como safety). `dimensionToRaise` desvía a `INTERFERENCE` si `INTRUSION_RATE` alto. Baseline
personal = `MemoryBaselineProtocol`. `PlateauDetector` + `InterventionPlanner` rotan estímulo/estrategia.

### Métricas primarias
`WORKING_MEMORY_SPAN`, `DUAL_TASK_COST`, `INTRUSION_RATE`, `RETRIEVAL_SPEED` (todas vía `MemoryMetricKey`).

### Métricas secundarias
`SERIAL_ORDER_ACCURACY`, `OMISSION_RATE`, consistencia (CV de span entre bloques), `LEARNING_CURVE_SLOPE`,
`CALIBRATION_INDEX` (overlay EX16).

### Señales de fatiga o mala calidad
RT de processing creciente con accuracy estable (drift de fatiga), span efectivo cayendo >1 punto vs
baseline de la sesión, processing accuracy <85% (trial inválido), racha de omisiones ≥3.

### Scoring sugerido  (fórmula o pseudofórmula con variables nombradas)
```
spanScore = sum_over_trials( serialPositionsCorrect / spanLength ) ponderado por validez
IES_proc  = meanRT_proc / (1 - errRate_proc)        // inverse efficiency del procesamiento
dualCost  = span_single - span_dual                  // DUAL_TASK_COST
intrusionRate = intrusions / (intrusions + correct)
qualityFlag = processingAccuracy >= 0.85
```
`confident=false` si `n` de trials válidos < umbral (`MemoryMetricValue.unavailable`).

### Antitrampa / calidad de datos
Processing con deadline impide ignorar la tarea concurrente. Trials con processing accuracy <85% no
cuentan para span. RT por debajo de `ANTICIPATION_FLOOR` (~150 ms) marca respuesta anticipada. Orden de
banco de recall aleatorizado para impedir patrón espacial.

### Variantes  (≥3 concretas)
1. **Operation span:** processing = verificación aritmética (default).
2. **Reading span:** processing = juicio semántico "¿frase plausible?" (requiere banco de frases V2).
3. **Symmetry span (visoespacial):** memoranda = posiciones en grid; processing = juicio de simetría
   (puente a Grid Espacial; comparte `COWAN_K`).

### UX  (pantalla, interacción, feedback, microanimaciones funcionales, duración ideal)
Pantalla limpia centrada; memorandum en grande, processing como tarjeta con dos botones. Microanimación
funcional: pulso del deadline (anillo que se vacía), no decorativo. Recall = banco inferior + zona de
orden. Duración ideal por trial 12–25 s; bloque de 6–8 trials; sesión 6–9 min.

### Implementación KMP  (modelo de datos, eventos, estado mínimo, motor a conectar)
```kotlin
// Reusa ComplexSpanTrial / SpanStep / Memorandum / ProcessingTask (MemoryTrialModels.kt).
// Generador ya existe: Ex1SpanGenerator : MemoryTrialGenerator (registrar en MemoryGraph.generators).
@Serializable
data class SpanTrialResult(           // efímero en sesión, NO persistido crudo
    val spanLength: Int, val serialCorrect: Int,
    val processingAccuracy: Double, val processingRtMs: List<Long>,
    val intrusions: Int, val omissions: Int, val valid: Boolean,
)
```
Eventos a registrar: `memory_span_trial_completed` (span, dualCost, valid), `memory_processing_lapse`.
Estado mínimo: `MemoryUiState` (fase Study/Process/Recall), índice de step, deadline activo. Persistir
solo `MemorySessionSummary` vía `LocalMemorySessionHistoryRepository` (`SecureKeyValueStore`,
`MindBoostStorageNamespaces.Memory`). Motores: `DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`,
`PlateauDetector`; tiempo vía `EpochClock`.

### Riesgos
Practice effect MEDIO (`PracticeEffectRisk.MEDIUM`): el processing aritmético se aprende; rotar a
reading/symmetry span (`STIMULUS_ROTATION`). Riesgo de que el usuario sacrifique processing por span
(mitigado por umbral de validez).

### Revisión crítica  (el Revisor Crítico intenta destruirlo)
"Es EX1 con maquillaje: el span verbal es el ejercicio más sobreusado y con mayor practice effect del
sector; un usuario memoriza la estructura del processing y el span sube sin WM real. Además 'aritmética'
es dependiente de habilidad numérica → confound cultural/educativo." **Correcciones exigidas:** (1) el
staircase de span DEBE alimentarse solo de trials con processing ≥85% (ya añadido); (2) reportar span
SOLO contra la sonda UNTRAINED para la claim de mejora; (3) ofrecer reading-span como default rotatorio
para neutralizar el confound numérico; (4) calibrar `processingDeadlineMs` al RT base del usuario, no fijo.

### Veredicto
**Aprobado con cambios** — premium solo si el span se valida contra processing y la mejora se afirma
únicamente sobre la sonda UNTRAINED.

---

## N-Back Adaptativo de Doble Modalidad

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`WORKING_MEMORY`). Secundarios: M4_SPATIAL_VISUAL (`SPATIAL`/`VISUAL`),
M5_RESISTANCE (`INTERFERENCE_RESISTANCE`, vía lures).

### Objetivo cognitivo
Actualización y matching continuo en WM (paradigma n-back): mantener una ventana deslizante de n ítems
y decidir si el actual coincide con el de hace n pasos. Entrena el componente de *updating* del modelo
de Miyake, distinto del mantenimiento puro del span.

### Hipótesis de transferencia
SÍ plausible: near-transfer a tareas de updating y a `WORKING_MEMORY_SPAN`. La literatura de far-transfer
del n-back a Gf es **controvertida y débil**; NO se afirma mejora de inteligencia ni de "foco". Solo se
reporta progreso contra baseline personal y delta UNTRAINED. NO afirmar beneficio académico directo.

### Loop principal
1. **Estímulo:** stream de ítems (posición en grid 3×3 + sílaba auditiva/visual), uno cada ISI.
2. **Decisión:** por ítem, "¿coincide con hace n?" en modalidad posición y/o sonido (single o dual n-back).
3. **Feedback:** inmediato por ítem (✓/✗ sutil) o diferido por bloque (configurable: el diferido reduce
   dependencia del feedback y mide aprendizaje real).
4. **Adaptación:** n sube/baja según accuracy del bloque (staircase sobre eje `LOAD` reinterpretado como n).
5. **Siguiente:** nuevo stream con tasa de targets controlada y lures n±1.

### Reglas
- Tasa de targets fija ~25–30% por modalidad (evita que "siempre no" sea ganador).
- Lures n−1/n+1 (`INTERFERENCE`) presentes para impedir matching por familiaridad vaga.
- Respuesta solo en ventana; sin respuesta = omisión (no "no-match" gratis).

### Inputs
Dos taps dedicados (botón Posición / botón Sonido) o swipe lateral para dual. Sin teclado. Voz no aplica.

### Variables de dificultad
- `nLevel` (eje `LOAD`): **1–5**.
- `dualModality` (eje `CONCURRENCY`): single→dual (≥0.5).
- `isiMs`: **3000→1500 ms** (presión temporal).
- `lureRate` (eje `INTERFERENCE`): **0.0→0.35** de ítems lure n±1.
- `targetRate`: **0.20→0.30** (mantener detectabilidad; no es palanca de "más=difícil").
- `stimulusSetSize` (eje `SIMILARITY`): **6→9** sonidos/posiciones (más confusables).
- `stimulusOnMs`: **800→400 ms**.

### Algoritmo adaptativo
Staircase 1-up/1-down asimétrico sobre `nLevel`: subir n con accuracy de bloque ≥80% en AMBAS
modalidades; bajar con <55%. Histéresis vía `cooldownBlocks`. `DefaultMemoryAdaptiveEngine` desvía a
`INTERFERENCE` (subir `lureRate`) cuando `FALSE_ALARM_RATE` alto en lugar de subir n (evita inflar carga
sin discriminación). Baseline = n máximo sostenido en `MemoryBaselineProtocol`.

### Métricas primarias
`WORKING_MEMORY_SPAN` (n efectivo), `RECOGNITION_DPRIME` (hits vs false alarms — honesto, no hit rate
crudo), `FALSE_ALARM_RATE`, `RETRIEVAL_SPEED`.

### Métricas secundarias
`RESPONSE_CRITERION` (sesgo), `DUAL_TASK_COST` (single vs dual), `BLOCK_STABILITY`, `LEARNING_CURVE_SLOPE`,
consistencia (CV de d').

### Señales de fatiga o mala calidad
d' descendiendo con criterio derivando a "todo no-match" (desistimiento), false alarms en lures subiendo
con RT alto (sobrecarga), accuracy <55% dos bloques seguidos (n demasiado alto), omisiones en racha.

### Scoring sugerido
```
dPrime = z(hitRate) - z(falseAlarmRate)              // RECOGNITION_DPRIME, sensibilidad real
criterion = -0.5 * (z(hitRate) + z(falseAlarmRate))  // RESPONSE_CRITERION, sesgo
nEffective = max nLevel con dPrime >= 1.0 sostenido
dualCost = dPrime_single - dPrime_dual
```
`confident=false` si targets/lures por bloque insuficientes para z estable.

### Antitrampa / calidad de datos
Reportar d' (no accuracy bruto) neutraliza la estrategia "responder siempre no". Lures impiden matching
laxo. Criterio extremo (siempre sí / siempre no) ⇒ bloque marcado de baja calidad. ISI con jitter para
impedir respuesta rítmica.

### Variantes  (≥3 concretas)
1. **Single n-back posición** (espacial puro).
2. **Single n-back auditivo** (verbal/fonológico puro).
3. **Dual n-back** (posición+sonido simultáneos, el clásico).
4. (V2) **n-back adaptativo continuo** sin bloques fijos.

### UX
Grid 3×3 central, dos botones grandes inferiores (Posición/Sonido). Microanimación funcional: el ítem
"hace n" se insinúa con un trail de memoria muy sutil solo en onboarding (luego off, para no andamiar).
ISI con barra de ritmo. Duración: bloque 60–90 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable enum class NBackModality { POSITION, SOUND }
data class NBackTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val stream: List<NBackItem>, val nLevel: Int,
    val modalities: Set<NBackModality>, val targetMask: List<Boolean>,
) : MemoryTrialPlan
data class NBackItem(val position: Int, val soundId: Int, val isLure: Boolean)
class NBackGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
Nuevo `MemoryExerciseId.EX_NBACK` + entrada en `MemoryExerciseCatalog` (governingMetric =
`WORKING_MEMORY_SPAN`). Eventos: `nback_item_response` (modality, hit/fa/miss, rt), `nback_block_dprime`.
Estado mínimo: ventana circular de n + índice. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine` (d'), `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO-ALTO en stream fijo; mitigar con `stimulusSetSize` rotatorio y jitter. Riesgo de
sobre-claim de far-transfer: prohibido en copy. Fatiga alta en dual n-back largo → cap de duración.

### Revisión crítica
"El n-back es el ejercicio con peor reputación científica para far-transfer; venderlo como 'entrena tu
inteligencia' sería pseudociencia. Y un d' alto puede venir de criterio, no de sensibilidad." **Correcciones
exigidas:** (1) copy DEBE limitarse a 'updating de memoria de trabajo' y progreso vs baseline, jamás Gf/IQ;
(2) reportar d' Y criterio, descartando bloques con criterio extremo; (3) la claim de mejora pasa por
`RealImprovementCriterion` sobre sonda UNTRAINED, no por n alcanzado.

### Veredicto
**Aprobado con cambios** — científicamente defendible solo con d' honesto y copy sin claims de inteligencia.

---

## Updating de Ranura (Running Memory / EX2 jugable)

### Módulo  (principal + secundarios)
Principal: M1_EXECUTIVE_WM (`WORKING_MEMORY`, paradigma `RUNNING_MEMORY`). Secundarios: M5_RESISTANCE
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria de actualización (*memory updating* puro): mantener el valor MÁS RECIENTE de k ranuras mientras
llega un flujo de actualizaciones, descartando activamente el valor antiguo (inhibición de lo obsoleto).
Distinto del n-back: aquí hay binding ranura↔valor explícito y la respuesta es el estado final.

### Hipótesis de transferencia
SÍ plausible: a `pending_tasks`/`real_instructions` (`TransferProtocols.PENDING_TASKS`, `INSTRUCTIONS`)
y a recordar "el último valor" en contexto real (precio actual, hueco de agenda). NO afirmar mejora
general de memoria ni clínica.

### Loop principal
1. **Estímulo:** se muestran k ranuras etiquetadas (A,B,C…) con un valor inicial; luego un stream de
   updates "ranura X ← nuevo valor", a veces "ranura X ← +/− operación" (running computation).
2. **Decisión:** al final, reportar el valor vigente de cada ranura (o de una ranura sondeada).
3. **Feedback:** diferido por ranura (correcto / valor antiguo intruido / valor de otra ranura).
4. **Adaptación:** sube k o longitud del stream (eje `LOAD`), o similitud de valores (`SIMILARITY`).
5. **Siguiente:** nuevo set con `excludeRecent` para evitar repetición.

### Reglas
- El número de updates por ranura varía (evita estrategia de "contar updates").
- Distingue dos errores: **intrusión de valor antiguo** (fallo de updating) vs **binding error**
  (valor correcto, ranura equivocada) — métricas separadas.
- Sonda puede ser de UNA ranura aleatoria (reduce carga de output, aísla la señal de updating).

### Inputs
Selección múltiple por ranura (banco de valores) o teclado numérico en variante de cómputo. Drag opcional
para asignar valor↔ranura.

### Variables de dificultad
- `slotCount` k (eje `LOAD`): **2–6** ranuras.
- `updateStreamLen` (eje `LOAD` secundario): **k → 4k** updates.
- `valueSimilarity` (eje `SIMILARITY`): valores de un set pequeño/parecido (0.0→1.0).
- `runningComputation` (eje `DEPTH`): reemplazo simple → operación (+/−) sobre el valor previo (≥0.5).
- `updateRateMs`: **2500→1200 ms** por update.
- `interferenceFillers` (eje `INTERFERENCE`): **0→3** updates a ranuras inexistentes (lures a ignorar).
- `probeAll` vs `probeOne` (eje `RETRIEVAL`): sondear todas (≥0.5) o una.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (k). `specificWeakAxis`: si `INTRUSION_RATE` (valor antiguo)
alto → subir `INTERFERENCE` (más fillers); si binding errors altos → subir `SIMILARITY`. Histéresis y
pasos asimétricos estándar. Baseline = k sostenido con accuracy ≥75%.

### Métricas primarias
`WORKING_MEMORY_SPAN` (k efectivo), `INTRUSION_RATE` (valor obsoleto), `BINDING_ERROR_RATE` (ranura
equivocada).

### Métricas secundarias
`RETRIEVAL_SPEED`, `OMISSION_RATE`, `RESPONSE_CRITERION` (en variante recognition), consistencia (CV de k),
`LEARNING_CURVE_SLOPE`.

### Señales de fatiga o mala calidad
Intrusiones de valor antiguo creciendo (updating fallando), binding errors creciendo con k estable,
respuestas a fillers (no ignora lures), RT de output muy alto.

### Scoring sugerido
```
kEffective = max k con (correctSlots/k) >= 0.75
intrusionRate = oldValueIntrusions / probes      // fallo de updating
bindingErrorRate = wrongSlotCorrectValue / probes
updatingScore = 1 - (intrusionRate + 0.5*bindingErrorRate)
```
`confident=false` si probes < umbral.

### Antitrampa / calidad de datos
Fillers a ranuras inexistentes detectan respuesta automática. Updates por ranura variables impiden conteo.
Sonda de una ranura aleatoria impide preparar solo algunas. Banco de respuesta mezclado.

### Variantes  (≥3 concretas)
1. **Keep-track (categorías):** ranuras = categorías, recordar el último ejemplar de cada una.
2. **Running computation:** ranura mantiene un número y los updates son operaciones (+/−).
3. **Spatial updating:** ranuras = posiciones, valor = objeto que se mueve (puente visoespacial).

### UX
Fila de "ranuras" como tarjetas etiquetadas; el update destaca brevemente la ranura afectada
(microanimación funcional: flash de reemplazo que comunica "lo viejo se va"). Output = tocar cada ranura
y elegir valor. Duración: trial 15–30 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class SlotUpdate(val slot: Int, val value: String, val isOp: Boolean)
data class UpdatingTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val slotCount: Int, val initial: List<String>,
    val updates: List<SlotUpdate>, val probedSlots: List<Int>,
    val expectedFinal: Map<Int, String>,   // scoring key embebida (patrón MemoryTrialPlan)
) : MemoryTrialPlan
class Ex2UpdatingGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX2_ACTUALIZACION_SET` ya existe en el catálogo (paradigma `RUNNING_MEMORY`); falta el
generador → añadir a `implementedGenerators()`. Eventos: `updating_probe` (intrusion/binding/correct),
`updating_filler_response`. Motores: `DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`,
`PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO. Riesgo de confusión con n-back en el usuario (UX debe dejar claro: "estado final",
no "matching"). Carga de output alta con probeAll → preferir probeOne para señal limpia.

### Revisión crítica
"¿Es esto distinto del n-back o es n-back con etiquetas? Si no separas intrusión-de-valor de
binding-error, mides una sopa." **Correcciones exigidas:** (1) separar formalmente `INTRUSION_RATE` y
`BINDING_ERROR_RATE` (hecho); (2) la sonda por defecto es de UNA ranura para aislar updating del coste de
output; (3) los fillers son obligatorios para impedir respuesta automática; (4) documentar la diferencia
paradigmática con n-back en la ficha del catálogo.

### Veredicto
**Aprobado** — paradigma distinto y bien aislado si se mantienen las dos métricas de error separadas.

---

## Grid Espacial con Binding (memoria de trabajo visoespacial)

### Módulo  (principal + secundarios)
Principal: M4_SPATIAL_VISUAL (`SPATIAL`). Secundarios: M1 (`WORKING_MEMORY`), M5
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria de trabajo visoespacial con binding objeto↔posición: recordar QUÉ objeto estaba en CADA celda
(no solo cuántos/dónde). Captura el binding feature-location, complementario al canal verbal del span.

### Hipótesis de transferencia
SÍ plausible: near-transfer a tareas espaciales (Corsi, change detection) y a `COWAN_K`. POSIBLE puente a
método de loci (`TransferProtocols.PRESENTATIONS`). NO afirmar mejora de "orientación" o navegación real.

### Loop principal
1. **Estímulo:** grid N×N; se iluminan c celdas, cada una con un objeto (color/forma/icono), durante un
   tiempo de estudio; luego se ocultan.
2. **Decisión:** reconstruir colocando cada objeto en su celda (recall) o juzgar si una sonda es correcta
   (recognition / change detection).
3. **Feedback:** diferido: celdas correctas, swaps (binding errors), posiciones acertadas con objeto malo.
4. **Adaptación:** sube c (eje `LOAD`), similitud de objetos (`SIMILARITY`) o demora (`DELAY`).
5. **Siguiente:** nuevo layout con `excludeRecent`.

### Reglas
- Distinguir **error de localización** (posición mal) de **error de binding** (posición bien, objeto mal)
  de **swap** (dos objetos intercambiados) — tres señales separadas.
- Recall por reconstrucción (no solo "¿cambió algo?") para forzar binding completo.
- En variante change-detection, usar d' (no hit rate).

### Inputs
Drag-and-drop de objetos a celdas (recall); tap en celda + selección de objeto; tap binario en
change-detection. Sin teclado.

### Variables de dificultad
- `itemCount` c (eje `LOAD`): **2–7** objetos-posición.
- `gridSize` N (acompaña a `LOAD`): **3×3 → 5×5**.
- `objectSimilarity` (eje `SIMILARITY`): colores muy distintos → tonos vecinos / formas parecidas (0→1).
- `delayMs` (eje `DELAY`): **0 → 8000 ms** con máscara.
- `studyMsPerItem`: **900→450 ms**.
- `interferenceGrid` (eje `INTERFERENCE`): **0→1** grid distractor intermedio.
- `recallMode` (eje `RETRIEVAL`): recognition (≥0 baja) → reconstrucción libre (alta).

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (c). `specificWeakAxis`: binding errors altos → subir
`SIMILARITY`; olvido tras demora → subir `DELAY`. d' guía la zona en change-detection. Baseline = `COWAN_K`
sostenido. `PlateauDetector` rota a `MODALITY_CHANGE` si mesetiza.

### Métricas primarias
`COWAN_K` (capacidad visoespacial), `BINDING_ERROR_RATE`, `IMMEDIATE_RECALL_ACCURACY`,
`DELAYED_RECALL_ACCURACY`.

### Métricas secundarias
`RECOGNITION_DPRIME` y `FALSE_ALARM_RATE` (change-detection), `RETRIEVAL_SPEED`, swap rate, consistencia
(CV de K).

### Señales de fatiga o mala calidad
K efectivo cayendo, binding errors subiendo con localización estable (binding fallando selectivamente),
random placement (accuracy ~ azar = desistimiento), RT de drag muy alto.

### Scoring sugerido
```
cowanK = c * (hitRate - falseAlarmRate)              // change-detection
locationAcc = correctCells / c
bindingAcc  = correctObjectGivenCorrectCell / correctCells
swapRate    = swappedPairs / c
visuospatialScore = locationAcc * bindingAcc
```
`confident=false` si trials < umbral o c demasiado bajo para K estable.

### Antitrampa / calidad de datos
Reconstrucción completa impide adivinar "algo cambió". Objetos similares impiden codificación verbal
trivial ("el rojo arriba"). Grid distractor controla rehearsal. Random-placement detectado por accuracy ≈
chance. Posiciones balanceadas para evitar sesgo de centro.

### Variantes  (≥3 concretas)
1. **Corsi-like (orden):** secuencia de celdas a reproducir en orden (puente a orden serial espacial).
2. **Change detection:** una celda cambia de objeto tras la demora → tap si detectas el cambio (d').
3. **What-where binding:** reconstrucción completa objeto↔celda (default, máximo binding).

### UX
Grid táctil grande; objetos en bandeja inferior; drag con snap a celda. Microanimación funcional: las
celdas con objeto pulsan una vez al estudiar (codificación), la máscara cubre con grano neutro. Duración:
trial 12–25 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class GridCell(val row: Int, val col: Int, val objectId: Int)
data class GridBindingTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val gridSize: Int, val items: List<GridCell>,
    val delayMs: Long, val recallMode: RecallType,
    val expected: List<GridCell>,                 // scoring key embebida
) : MemoryTrialPlan
class Ex5GridGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX5_GRID_ESPACIAL` ya en catálogo (governingMetric `COWAN_K`); falta generador. Eventos:
`grid_placement` (location/binding/swap), `grid_change_detection`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine` (Cowan's K, d'), `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO/BAJO (`PracticeEffectRisk.LOW` para change-detection). Riesgo de verbalización
(mitigado por similitud de objetos). Drag en pantallas pequeñas → asegurar hit-area amplia.

### Revisión crítica
"Un grid bonito que en realidad se resuelve verbalizando ('círculo rojo arriba-izquierda') no mide
memoria visoespacial, mide memoria verbal disfrazada." **Correcciones exigidas:** (1) objetos sin nombre
verbal fácil y alta similitud cromática/forma para bloquear recoding verbal; (2) separar location vs
binding vs swap (hecho); (3) en change-detection reportar `COWAN_K` con false alarms, no hit rate; (4)
grid distractor para impedir rehearsal sostenido.

### Veredicto
**Aprobado con cambios** — visoespacial genuino solo si el recoding verbal está activamente bloqueado.

---

## Pares Asociados con Interferencia A-B/A-C (rediseño premium de EX4)

### Módulo  (principal + secundarios)
Principal: M3_ASSOCIATIVE (`ASSOCIATIVE`). Secundarios: M5_RESISTANCE (`INTERFERENCE_RESISTANCE`), M2
(`DELAYED_RETRIEVAL`).

### Objetivo cognitivo
Memoria asociativa (binding relacional entre elementos no relacionados) y su resistencia a interferencia
proactiva/retroactiva mediante el paradigma A-B / A-C: aprender pares, luego re-emparejar el mismo cue
con un target nuevo, midiendo intrusiones del aprendizaje previo.

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.VOCABULARY` (pares L1-L2, `v1Available=true`) y `names_faces_context`.
La interferencia A-B/A-C es la más cercana a la realidad de "actualizar" una asociación (cambió el número
de alguien). NO afirmar mejora de "memoria fotográfica".

### Loop principal
1. **Estímulo:** estudiar lista A-B (cue→target). Tras demora, estudiar A-C (mismos cues, nuevos targets).
2. **Decisión:** recall cued: dado A, producir C (o B según la lista pedida), forward/backward.
3. **Feedback:** diferido: correcto, intrusión (dio B cuando pedían C), error de conjunción.
4. **Adaptación:** sube nº de pares (`LOAD`), similitud (`SIMILARITY`), demora (`DELAY`) o grado de
   solapamiento A-B/A-C (`INTERFERENCE`).
5. **Siguiente:** nueva lista con `excludeRecentPairIds`.

### Reglas
- El cue de A-C es el MISMO que en A-B (eso crea la interferencia proactiva controlada).
- Recall bidireccional opcional (A→C y C→A) para medir simetría del binding.
- Error de conjunción (mezcla cue de un par con target de otro) se mide aparte de la intrusión A-B.

### Inputs
Selección múltiple (banco de targets + distractores), teclado opcional para producción libre con matching
tolerante. Sin texto libre no validado.

### Variables de dificultad
- `pairCount` (eje `LOAD`): **3–10** pares.
- `overlap A-B/A-C` (eje `INTERFERENCE`): **0% → 100%** de cues compartidos.
- `semanticSimilarity` (eje `SIMILARITY`): targets de distinta vs misma categoría (0→1).
- `delayMs` (eje `DELAY`): **0 → 24 h** (vía `DeferredRecall`/`DeferredRecallScheduler`).
- `studyMsPerPair`: **2000→900 ms**.
- `recallDirection` (eje `RETRIEVAL`): forward → bidireccional (≥0.5).
- `encodingPrompt` (eje `DEPTH`): rote → elaborativo (sugerir imagen mental/keyword).

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD`. `specificWeakAxis`: intrusiones A-B altas → subir
`INTERFERENCE` (más overlap); olvido diferido → subir `DELAY`. Baseline = `ASSOCIATIVE_INDEX` sostenido.
`InterventionPlanner` → `ENCODING_STRATEGY_CHANGE` si meseta (rote→elaborativo).

### Métricas primarias
`ASSOCIATIVE_INDEX`, `CONJUNCTION_ERROR_RATE`, `INTRUSION_RATE` (A-B sobre A-C), `INTERFERENCE_INDEX`.

### Métricas secundarias
`DELAYED_RECALL_ACCURACY`, `RETRIEVAL_SPEED`, asimetría forward/backward, `LEARNING_CURVE_SLOPE`,
consistencia.

### Señales de fatiga o mala calidad
Intrusiones A-B disparándose (interferencia ganando), errores de conjunción altos (binding ruidoso),
accuracy ≈ azar en el banco, RT de selección creciente.

### Scoring sugerido
```
associativeIndex = correctTargets / pairCount
proactiveInterference = abIntrusions / (abIntrusions + correctC)
conjunctionRate = conjunctionErrors / probes
interferenceIndex = recall_AC_withOverlap / recall_AC_noOverlap   // <1 = interferencia
```
`confident=false` si pares < umbral.

### Antitrampa / calidad de datos
Banco con distractores que incluyen los targets B (detecta intrusión real, no azar). Cues idénticos A-B/A-C
verificados. Producción libre con normalización (lemmatize) y matching tolerante para no penalizar typos.
Demora real anclada a `EpochClock` (no se puede saltar).

### Variantes  (≥3 concretas)
1. **A-B/A-C clásico** (interferencia proactiva, default).
2. **Vocabulario L1-L2** (keyword method, `TransferProtocols.VOCABULARY`).
3. **Bidireccional simétrico** (mide si A→C y C→A son igual de fuertes).
4. (V2) **Caras-nombres** como caso asociativo de alto valor (`EX8_NOMBRES_ROSTROS`).

### UX
Estudio: tarjeta cue↑/target↓ con prompt de estrategia opcional. Recall: cue grande + banco inferior.
Microanimación funcional: al re-aprender A-C, el cue "parpadea" señalando "mismo cue, nuevo target".
Duración: estudio 30–60 s, recall 30–60 s, sesión 6–8 min (+ probe diferida 24 h).

### Implementación KMP
```kotlin
// Reusa PairedAssociatesTrial / CuedRecallProbe / RecallDirection (MemoryTrialModels.kt) + Ex4PairsGenerator.
// Añadir fase A-C: lista secundaria con cues compartidos.
@Serializable data class InterferenceProbeResult(
    val correct: Boolean, val abIntrusion: Boolean, val conjunction: Boolean, val rtMs: Long,
)
```
`MemoryExerciseId.EX4_PARES_ASOCIADOS` ya jugable; extender `Ex4PairsGenerator` con `overlap`. Demora 24 h
vía `DeferredRecallScheduler` + `LocalDeferredRecallRepository`. Eventos: `paired_probe`
(correct/abIntrusion/conjunction), `paired_deferred_due`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine`, `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect ALTO (`PracticeEffectRisk.HIGH`): pares se memorizan; obligatorio rotar contenido vía
`RecentItemRegistry` y medir solo sobre material nuevo. Demora 24 h depende de re-engagement (riesgo de
abandono de la probe diferida).

### Revisión crítica
"Pares asociados con HIGH practice-effect: si repites el mismo banco, el 'progreso' es memorización de
ítems, no mejora de capacidad asociativa." **Correcciones exigidas:** (1) contenido SIEMPRE nuevo por
sesión (excludeRecentPairIds estricto); (2) la mejora se afirma solo sobre pares UNTRAINED de la sonda;
(3) interferencia A-B/A-C obligatoria para que el ejercicio mida binding robusto y no calco; (4) probe
diferida real anclada a reloj, no simulada.

### Veredicto
**Aprobado con cambios** — premium solo con rotación estricta de contenido y A-B/A-C como núcleo.

---

## Episódica con Recall Diferido y Reconstrucción (rediseño premium de EX3)

### Módulo  (principal + secundarios)
Principal: M2_DEEP_ENCODING (`EPISODIC`). Secundarios: M6 (`METAMEMORY_CALIBRATION`), M5
(`INTERFERENCE_RESISTANCE`).

### Objetivo cognitivo
Memoria episódica de lista con codificación profunda (elaborativa) y consolidación: estudiar material,
recuperar inmediato Y diferido (mismo día + 24 h), midiendo curva de olvido, organización (clustering) y
beneficio de la estrategia de codificación.

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.STUDY` (`study_efficient`, `v1Available=true`) y `ACTIVE_READING`.
Codificación elaborativa y testing-effect transfieren a estudio real. NO afirmar mejora clínica de
memoria ni prevención de declive.

### Loop principal
1. **Estímulo:** lista de palabras (categorizable) con prompt de estrategia (agrupar/historia/imagen).
2. **Decisión:** recall inmediato (libre o cued por categoría); recall diferido mismo-día y +24 h.
3. **Feedback:** diferido: aciertos, intrusiones, clustering (ARC), comparación inmediato vs diferido.
4. **Adaptación:** sube `LOAD`, `DELAY`, `SIMILARITY`, o exige codificación más profunda (`DEPTH`).
5. **Siguiente:** nueva lista; la probe +24 h se agenda con `DeferredRecallScheduler`.

### Reglas
- La estrategia de codificación se SUGIERE y se mide su uso vía clustering (ARC), no se impone.
- Recall diferido +24 h es parte del scoring de consolidación (no opcional para la métrica de mejora real).
- Intrusiones extralista penalizan (control de falsas memorias).

### Inputs
Selección múltiple o producción libre con matching tolerante (lemmatize). Voz opcional (V2, transcripción
local) para recall hablado. Texto libre normalizado.

### Variables de dificultad
- `listLength` (eje `LOAD`): **6–24** palabras.
- `categoryStructure` (eje `DEPTH`/`SIMILARITY`): aleatoria → categorizable (favorece clustering).
- `immediateDelayMs`: **0 → 60 s** (con tarea distractora para impedir rehearsal).
- `longDelay` (eje `DELAY`): mismo día (~20 min) y **24 h** (vía scheduler).
- `studyMsPerItem`: **2500→1000 ms**.
- `interferenceList` (eje `INTERFERENCE`): **0→1** lista B intermedia (interferencia retroactiva).
- `recallType` (eje `RETRIEVAL`): cued por categoría → libre.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD`. `delayedLags()`: si `DELAYED_RECALL_ACCURACY` <<
`IMMEDIATE_RECALL_ACCURACY` → subir `DELAY` no, sino reforzar consolidación (más espaciado); si
clustering bajo → `ENCODING_STRATEGY_CHANGE`. Baseline = `DELAYED_RECALL_ACCURACY`. `PlateauDetector`
distingue FalseImprovement (sube inmediato pero no diferido).

### Métricas primarias
`DELAYED_RECALL_ACCURACY` (gobernante), `IMMEDIATE_RECALL_ACCURACY`, `ARC_CLUSTERING`,
`CONSOLIDATION_INDEX`, `INTRUSION_RATE`.

### Métricas secundarias
`FORGETTING_RATE` (curva de olvido), `SPACED_REPETITION_BENEFIT`, `LEARNING_CURVE_SLOPE`,
`CALIBRATION_INDEX` (JOL: predice cuántas recordará), consistencia.

### Señales de fatiga o mala calidad
Inmediato sube pero diferido plano (FalseImprovement por `PlateauDetector`), intrusiones extralista
subiendo (falsas memorias), clustering bajo persistente (no codifica profundo), recall ≈ primacía/recencia
sin medio (rehearsal superficial).

### Scoring sugerido
```
immediateAcc = recalledImmediate / listLength
delayedAcc   = recalledDelayed24h / listLength
forgettingRate = (immediateAcc - delayedAcc) / immediateAcc
arcClustering = (observedCategoryRuns - expected) / (maxRuns - expected)   // ARC de Roenker
consolidationIndex = delayedAcc / immediateAcc
intrusionRate = extraListIntrusions / responses
```
`confident=false` si la probe +24 h no se completó (mejora real requiere ambos puntos).

### Antitrampa / calidad de datos
Tarea distractora antes del recall inmediato bloquea rehearsal (mide LTM, no buffer). Probe +24 h anclada
a `EpochClock` real. Matching tolerante con lemmatize; intrusiones extralista detectan invención. Lista B
controla interferencia retroactiva.

### Variantes  (≥3 concretas)
1. **Free recall categorizable** (mide ARC clustering, default).
2. **Cued recall por categoría** (aísla disponibilidad vs accesibilidad).
3. **Narrativa episódica** (`EX15_NARRATIVA_AUDITIVA`): recordar quién/qué/orden de una historia.
4. **Testing-effect:** recall repetido vs re-estudio, mide beneficio de recuperar.

### UX
Estudio: palabras una a una con prompt de estrategia y, opcional, "agrúpalas tú". Recall: campo + banco /
producción libre. Microanimación funcional: al estudiar, las de la misma categoría comparten un tinte
sutil solo si el usuario activa "ayuda de agrupado". Duración: estudio 30–90 s, recall 60 s; probe +24 h
2–3 min al día siguiente.

### Implementación KMP
```kotlin
// Reusa RecallListTrial (studyList, recallType, studyMsPerItem, delayMs, cueCategory, expected) + Ex3ListGenerator.
@Serializable data class EpisodicRecallResult(
    val immediateRecalled: List<String>, val delayedRecalled: List<String>,
    val extraListIntrusions: Int, val arcClustering: Double, val jolPrediction: Int?,
)
```
`MemoryExerciseId.EX3_LISTA_ESTRATEGIA` ya jugable. Probe +24 h: `DeferredRecall` + `DeferredRecallScheduler`
+ `LocalDeferredRecallRepository`. ARC vía `ArcClustering`. Eventos: `episodic_immediate_recall`,
`episodic_delayed_due`, `episodic_intrusion`. Motores: `DefaultMemoryAdaptiveEngine`,
`DefaultMemoryMetricsEngine`, `PlateauDetector` (FalseImprovement), `ReliableChangeIndex` (cambio fiable en
diferido). Persistir `MemorySessionSummary` (immediateAccuracy, delayedAccuracy).

### Riesgos
Practice effect ALTO (`PracticeEffectRisk.HIGH`): listas se memorizan; rotación obligatoria. La probe +24 h
puede no completarse → la claim de "mejora real" queda gated (correcto, pero reduce datos). Producción libre
con typos.

### Revisión crítica
"Es 'memoriza una lista', el ejercicio más viejo y más sujeto a practice effect del mundo. Sin recall
diferido real es solo un buffer test, y si reutilizas listas mides familiaridad." **Correcciones exigidas:**
(1) recall diferido +24 h ANCLADO a reloj es obligatorio para la métrica de mejora; (2) tarea distractora
antes del inmediato para no medir recencia; (3) contenido nuevo por sesión + sonda UNTRAINED;
(4) `PlateauDetector.FalseImprovement` debe vigilar el gap inmediato↔diferido.

### Veredicto
**Aprobado con cambios** — deja de ser "memoriza números" solo con diferido real + distractor + rotación.

---

## Orden Serial Temporal (secuencias forward/backward)

### Módulo  (principal + secundarios)
Principal: M4_SPATIAL_VISUAL (`SEQUENTIAL`, paradigma `SERIAL_ORDER`). Secundarios: M1
(`WORKING_MEMORY`), M2 (`EPISODIC`).

### Objetivo cognitivo
Memoria del ORDEN (no del contenido): reproducir una secuencia de elementos en la posición temporal
correcta. Aísla el componente de orden serial, disociable del item memory (puedes saber QUÉ ítems sin
saber su ORDEN).

### Hipótesis de transferencia
SÍ plausible: `TransferProtocols.PRESENTATIONS`, `INSTRUCTIONS`, `PENDING_TASKS` (secuencias de pasos,
método de loci). NO afirmar mejora general de "memoria" ni de planificación.

### Loop principal
1. **Estímulo:** secuencia de s elementos (iconos/posiciones/sílabas) presentados uno a uno.
2. **Decisión:** reproducir el orden (forward o backward) tocando los elementos en secuencia.
3. **Feedback:** diferido: posiciones correctas, errores de transposición (ítem correcto, posición ±1),
   distancia de edición.
4. **Adaptación:** sube longitud (`LOAD`), similitud (`SIMILARITY`), demora (`DELAY`) o backward (`RETRIEVAL`).
5. **Siguiente:** nueva secuencia.

### Reglas
- Puntuar por POSICIÓN (no por "ítems presentes"): un ítem en posición equivocada NO es acierto pleno.
- Medir errores de transposición (intercambios adyacentes) aparte de omisiones e intrusiones.
- Backward recall como palanca de `RETRIEVAL` (mayor demanda ejecutiva).

### Inputs
Tap secuencial sobre un banco de elementos; drag para reordenar una fila. Sin teclado.

### Variables de dificultad
- `sequenceLength` s (eje `LOAD`): **3–9** elementos.
- `direction` (eje `RETRIEVAL`): forward → backward (≥0.5).
- `itemSimilarity` (eje `SIMILARITY`): elementos distintos → confusables (0→1).
- `presentationRateMs`: **1000→500 ms** por elemento.
- `delayMs` (eje `DELAY`): **0 → 6000 ms** con máscara.
- `concurrentTap` (eje `CONCURRENCY`): **0→1** tarea de tap rítmico durante la presentación (suprime rehearsal).
- `grouping`: presentación agrupada (chunking visible) → continua.

### Algoritmo adaptativo
`DefaultMemoryAdaptiveEngine` sobre `LOAD` (s). `specificWeakAxis`: transposiciones altas → subir
`SIMILARITY`; olvido con demora → subir `DELAY`. Backward solo se introduce con forward estable. Baseline =
`SERIAL_ORDER_ACCURACY` sostenido.

### Métricas primarias
`SERIAL_ORDER_ACCURACY`, `IMMEDIATE_RECALL_ACCURACY` (item memory, para disociar), `INTRUSION_RATE`.

### Métricas secundarias
Transposition rate (curva de gradiente posicional), `RETRIEVAL_SPEED`, span de orden vs span de ítem
(disociación), consistencia, `LEARNING_CURVE_SLOPE`.

### Señales de fatiga o mala calidad
Order accuracy cayendo con item accuracy estable (orden fallando selectivamente), transposiciones
adyacentes disparadas, respuesta aleatoria (gradiente plano = azar), RT alto.

### Scoring sugerido
```
serialOrderAcc = positionsCorrect / s
itemAcc        = itemsPresentRegardlessOfOrder / s
transpositionRate = adjacentSwaps / s
orderSpecificScore = serialOrderAcc - (itemAcc * chanceOrderGivenItems)  // orden por encima de item memory
```
`confident=false` si trials < umbral.

### Antitrampa / calidad de datos
Scoring por posición impide "los toqué todos" como acierto. Tap rítmico concurrente suprime rehearsal
articulatorio. Banco mezclado por trial. Gradiente posicional (errores deben concentrarse en medio):
gradiente plano = respuesta azarosa, marca baja calidad.

### Variantes  (≥3 concretas)
1. **Forward span de orden** (Corsi/Digit-span de orden, default).
2. **Backward span** (mayor demanda ejecutiva).
3. **Reconstrucción de orden:** se dan los ítems desordenados, ordenarlos (aísla orden de item recall).
4. **Orden con supresión articulatoria** (tap concurrente).

### UX
Presentación secuencial con resalte por elemento (microanimación funcional: "destello" por paso =
codificación temporal). Reproducción: banco + fila de orden con números de posición. Duración: trial
10–20 s, sesión 6–8 min.

### Implementación KMP
```kotlin
@Serializable data class SerialItem(val itemId: Int, val position: Int)
data class SerialOrderTrialPlan(
    override val exerciseId: MemoryExerciseId,
    val sequence: List<SerialItem>, val direction: RecallDirection,
    val delayMs: Long, val expectedOrder: List<Int>,    // scoring key embebida
) : MemoryTrialPlan
class Ex7SerialGenerator : MemoryTrialGenerator { /* registrar en MemoryGraph.generators */ }
```
`MemoryExerciseId.EX7_SECUENCIAS` ya en catálogo (governingMetric `SERIAL_ORDER_ACCURACY`); falta generador.
Eventos: `serial_reproduction` (positionsCorrect, transpositions), `serial_item_vs_order`. Motores:
`DefaultMemoryAdaptiveEngine`, `DefaultMemoryMetricsEngine`, `PlateauDetector`. Persistir `MemorySessionSummary`.

### Riesgos
Practice effect MEDIO. Riesgo de que el usuario use chunking (mitigado por `grouping` continuo y tap
concurrente). Confusión con span de ítem si no se disocia (mitigado por reconstrucción de orden).

### Revisión crítica
"Sin disociar order memory de item memory, esto es indistinguible de un span más. Y si scoreas 'ítems
presentes' premias recall de ítem, no de orden." **Correcciones exigidas:** (1) scoring estrictamente por
posición (hecho); (2) variante de reconstrucción de orden que da los ítems para aislar orden puro; (3)
medir transposiciones y gradiente posicional para validar que la señal es de orden; (4) supresión
articulatoria opcional para bloquear rehearsal verbal del orden.

### Veredicto
**Aprobado** — aporta una dimensión cognitiva distinta (orden) si se disocia explícitamente del item memory.

---

## Sonda de Transferencia UNTRAINED (Probe de capacidad latente)

### Módulo  (principal + secundarios)
Transversal a todos los módulos. Mide capacidad, no entrena. Usa material y a veces paradigmas RESERVADOS
(`TransferProbePartition`), nunca vistos en entrenamiento.

### Objetivo cognitivo
Estimar la capacidad cognitiva REAL (no la familiaridad con un ejercicio entrenado) administrando tareas
con material/paradigma UNTRAINED. Es el patrón de oro de honestidad del sistema: la única evidencia
admisible de mejora genuina (`RealImprovementCriterion.untrainedDelta`).

### Hipótesis de transferencia
Por construcción mide transferencia: near (mismo paradigma, material nuevo) y far (paradigma relacionado
no entrenado, p. ej. complex span no practicado). SÍ se afirma: "tu capacidad funcional cambió de forma
fiable" SOLO si `ReliableChangeIndex.compute` da `reliable=true` (|RCI|>1.96) Y
`RealImprovementCriterion.isGenuine`. NO se afirma nada con una sola administración.

### Loop principal
1. **Estímulo:** mini-batería corta de ítems UNTRAINED (material reservado por `TransferProbePartition`),
   paradigmas estandarizados, dificultad FIJA (no adaptativa — es medición, no entrenamiento).
2. **Decisión:** el usuario responde igual que en el ejercicio base, pero sin feedback de rendimiento
   (no se entrena al medir).
3. **Feedback:** NINGUNO durante la probe (evita aprendizaje); solo al final, un resumen honesto de tendencia.
4. **Adaptación:** NO hay adaptación intra-probe (dificultad fija = comparabilidad pre/post).
5. **Siguiente:** se administra periódicamente (cada N sesiones / semanal), no cada sesión.

### Reglas
- Dificultad FIJA y estandarizada entre administraciones (comparabilidad).
- Material SIEMPRE reservado (`TransferProbePartition`): nunca aparece en entrenamiento.
- Sin feedback de acierto durante la probe (no entrena, mide).
- Requiere ≥2–3 administraciones antes de computar RCI (hasta entonces, solo delta crudo con caveat).

### Inputs
Idénticos al ejercicio base medido (span, recall, recognition) para validez de constructo. Sin inputs nuevos.

### Variables de dificultad
La probe es de dificultad FIJA por diseño; lo que varía es la COBERTURA de constructos, no la dificultad:
- `probeBatteryCoverage`: **1–4** subcapacidades medidas por administración.
- `itemsPerConstruct`: **8–20** ítems (más = `confident=true` antes; menos = más rápido).
- `fixedDifficultyLevel`: anclado al ~percentil 50 poblacional (estándar, NO adaptativo).
- `parallelFormId`: **A/B/C** formas paralelas equiparadas (rota para evitar memorización entre probes).
- `delayProbeMs`: incluye un punto diferido (+24 h) para `CONSOLIDATION_INDEX` UNTRAINED.

### Algoritmo adaptativo
NINGUNO intra-probe (la fijeza es el punto). El "adaptativo" del sistema solo decide CUÁNDO administrar:
tras un bloque de progreso aparente en entrenado, o si `PlateauDetector` reporta posible FalseImprovement,
se dispara una probe para verificar si la mejora es real o familiaridad. Formas paralelas (A/B/C) rotan por
administración.

### Métricas primarias
`NEAR_TRANSFER_INDEX`, el delta UNTRAINED que alimenta `RealImprovementCriterion.untrainedDelta`, y el
`ReliableChangeIndex` (RCI) sobre la métrica gobernante del constructo medido.

### Métricas secundarias
`effectSize` (Cohen's d para `RealImprovementCriterion.effectSize`), `sustainedWeeks`, `CONSOLIDATION_INDEX`
UNTRAINED, comparación trained-delta vs untrained-delta (clasifica `ImprovementPattern`).

### Señales de fatiga o mala calidad
Probe administrada con fatiga alta (invalida la medición — el sistema NO debe lanzar probe si
`MemoryFatigue` es HIGH), reliability aún no estimada (`RciResult.NotEnoughData`), n insuficiente
(`confident=false`), varianza pre anómala.

### Scoring sugerido
```
untrainedDelta = probePostScore - probePreScore                      // RealImprovementCriterion
effectSize     = untrainedDelta / pooledSD                           // Cohen's d
RCI = (post - pre) / (sqrt(2) * sdPre * sqrt(1 - reliability))       // ReliableChangeIndex.compute
pattern = RealImprovementCriterion.classify(ImprovementEvidence(
    trainedDelta, untrainedDelta, immediateDelta, delayedDelta, effectSize, sustainedWeeks))
genuine = isGenuine(...) && (RCI.reliable == true)
```
Claim de mejora SOLO si `genuine && RCI reliable`. Si no: "progreso en el ejercicio entrenado; aún sin
evidencia de cambio de capacidad" (honesto).

### Antitrampa / calidad de datos
Material reservado (`TransferProbePartition`) garantiza que no es lo entrenado. Formas paralelas A/B/C
impiden memorizar la propia probe. Sin feedback evita que la probe entrene. Gate de RCI (`NotEnoughData`
hasta tener reliability) impide claims prematuros. No se administra con fatiga alta.

### Variantes  (≥3 concretas)
1. **Near-transfer probe:** mismo paradigma que el entrenado, material reservado (p. ej. span con
   palabras nunca usadas).
2. **Far-transfer probe:** paradigma relacionado NO entrenado (entrenó span verbal → se mide updating o
   span visoespacial).
3. **Consolidation probe:** componente +24 h UNTRAINED para `CONSOLIDATION_INDEX` real.
4. **Calibration probe:** añade JOL (predicción de rendimiento) → `CALIBRATION_INDEX` UNTRAINED.

### UX
Marco visual distinto y honesto: "Esto es una MEDICIÓN, no entrenamiento. Sin pistas, sin feedback —
para ver tu capacidad real." Sin gamificación de puntos durante la probe. Resumen final sobrio con caveats
(`RciResult.NotEnoughData` → "necesitamos más mediciones para confirmar tendencias"). Duración: 3–6 min,
periódica.

### Implementación KMP
```kotlin
// TransferProbePartition ya reserva material UNTRAINED; ReliableChangeIndex + RealImprovementCriterion existen.
@Serializable data class TransferProbeSummary(
    val constructKey: MemoryMetricKey, val score: Double, val n: Int,
    val parallelForm: String, val administeredAtEpoch: Long, val fatigueWasLow: Boolean,
)
data class TransferProbePlan(
    override val exerciseId: MemoryExerciseId,         // EX_TRANSFER_PROBE
    val items: List<MemoryTrialPlan>, val fixedDifficulty: MemoryDifficultyVector,
    val parallelForm: String,
) : MemoryTrialPlan
```
Nuevo `MemoryExerciseId.EX_TRANSFER_PROBE` (NO en `implemented` como drill normal; orquestado por el
scheduler de sesión). Persistir solo `TransferProbeSummary` en `MindBoostStorageNamespaces.Memory` vía
`SecureKeyValueStore` (`@Serializable`, `schemaVersion`). Eventos: `transfer_probe_administered`,
`reliable_change_computed` (rci, reliable). Motores: `ReliableChangeIndex`, `RealImprovementCriterion`,
`PlateauDetector` (dispara la probe ante FalseImprovement). Tiempo vía `EpochClock`.

### Riesgos
Si las formas paralelas A/B/C no están equiparadas en dificultad, el delta se contamina (riesgo
psicométrico real → requiere equating). Coste de re-engagement de la componente +24 h. Usuarios pueden
percibir la probe como "examen" y abandonar (mitigar con framing positivo y brevedad).

### Revisión crítica
"Esta es la pieza más fácil de hacer mal: si las formas no están equiparadas, mides ruido y luego declaras
'mejora fiable' con un RCI basura. Y administrar la probe con el usuario cansado invalida todo." **Correcciones
exigidas:** (1) formas paralelas equiparadas (equating estadístico) ANTES de comparar; (2) NO administrar
con fatiga HIGH (gate explícito); (3) RCI gated por reliability estimada con ≥2–3 administraciones
(`RciResult.NotEnoughData` por defecto); (4) la claim de mejora requiere RCI reliable Y
`RealImprovementCriterion.isGenuine` simultáneamente; (5) cero feedback durante la probe.

### Veredicto
**Aprobado con cambios** — es el corazón de la honestidad del módulo; válido solo con formas equiparadas,
gate de fatiga y RCI gated por reliability.

---

## Resumen de cobertura cognitiva

| # | Ejercicio | Subcapacidad principal | Paradigma | Eje gobernante | Métrica gobernante |
|---|-----------|------------------------|-----------|----------------|--------------------|
| 1 | Span Adaptativo Dual | WORKING_MEMORY | COMPLEX_SPAN | LOAD | WORKING_MEMORY_SPAN |
| 2 | N-Back Doble Modalidad | WORKING_MEMORY | (n-back, nuevo) | LOAD (n) | WORKING_MEMORY_SPAN / RECOGNITION_DPRIME |
| 3 | Updating de Ranura | WORKING_MEMORY | RUNNING_MEMORY | LOAD (k) | WORKING_MEMORY_SPAN |
| 4 | Grid Espacial con Binding | SPATIAL | SPATIAL_GRID | LOAD (c) | COWAN_K |
| 5 | Pares A-B/A-C | ASSOCIATIVE | PAIRED_ASSOCIATES | LOAD | ASSOCIATIVE_INDEX |
| 6 | Episódica Diferida | EPISODIC | FREE_RECALL | LOAD | DELAYED_RECALL_ACCURACY |
| 7 | Orden Serial Temporal | SEQUENTIAL | SERIAL_ORDER | LOAD (s) | SERIAL_ORDER_ACCURACY |
| 8 | Sonda de Transferencia UNTRAINED | transversal | (medición fija) | — (fijo) | NEAR_TRANSFER_INDEX + RCI |

Los 8 cubren WM verbal (1,2,3), WM visoespacial (4), binding asociativo (5), episódica con diferido (6),
orden serial (7), updating/running memory (3) y la sonda UNTRAINED de transferencia (8) — sin solapar el
mismo loop. Toda claim de mejora real pasa por `RealImprovementCriterion` + `ReliableChangeIndex` sobre la
sonda UNTRAINED; `PlateauDetector`/`InterventionPlanner` gobiernan el largo plazo; el adaptativo mueve UN
eje del `MemoryDifficultyVector` por decisión.


---



# 3.6 Decisiones (8 ejercicios)

> **Fronteras de seguridad (vinculantes):** MindBoost entrena un perfil cognitivo funcional; no diagnostica, no mide CI ni promete mejora clinica. Toda salida sin soporte estadistico se reporta `null` (em-dash), nunca 0, y arranca `preliminary = true`. Motores reales conectados: `BayesEngine` (`com.mindverse.mindboost.core.mdp.bayes`), `ReversibilityEngine` (`...mdp.reversib`), `ScoringEquations` (`...scoring`), `CoachEngine` (`...coach`). Persistencia: patron `SecureKeyValueStore` + DTO `@Serializable` + `schemaVersion` + namespace `Decisiones`; renderers via registry id->renderer.

---

## E1 — Bayes Arena: Calibrador de Posterior Continuo

### Modulo
Principal: Decisiones (razonamiento probabilistico / actualizacion bayesiana). Secundarios: Metacognicion (calibracion confianza-acierto), Velocidad de procesamiento controlada (RT bajo presion sin premiar impulsividad).

### Objetivo cognitivo
Entrenar la actualizacion bayesiana correcta de una creencia ante evidencia con sensibilidad y tasa de falsos positivos conocidas: el usuario debe mover su estimacion del posterior P(H|E) hacia el valor matematico, no anclarse en la base-rate ni sobre-reaccionar a la evidencia (base-rate neglect y over-updating son los dos errores diana).

### Hipotesis de transferencia
SI podria transferir (near): mejor estimacion de posteriores en tareas isomorfas de diagnostico/test-positivo con base-rates distintas; reduccion del base-rate neglect en variantes no entrenadas. NO se debe afirmar: mejora del "razonamiento" general, del CI, ni que el usuario tomara mejores decisiones medicas/financieras reales (far transfer no garantizado; solo se reporta como hipotesis).

### Loop principal
1. **Estimulo:** escenario textual minimo + 3 sliders/indicadores de base-rate, sensibilidad (TP) y falso-positivo (FP) renderizados como barras (no numeros crudos para evitar calculo memorizado), mas un panel de evidencia.
2. **Decision:** el usuario arrastra un dial continuo [0,1] hasta su estimacion del posterior P(H|E).
3. **Feedback:** se revela el posterior matematico; el dial muestra el gap (sobre/infra-estimacion) coloreado por signo; coach inmediato si BUR fuera de banda.
4. **Adaptacion:** `DifficultyAdapter.calculateNextDifficulty` ajusta numero de evidencias encadenadas, separacion TP-FP y ruido.
5. **Siguiente trial:** nuevo `BayesScenario` seeded.

### Reglas
Una respuesta = una posicion del dial confirmada (tap "Confirmar"). El dial no muestra el valor objetivo como affordance. RT se cuenta desde render completo hasta confirmacion. Trials con confirmacion <500 ms o sin mover el dial desde el default se marcan no validos (anti-default-gaming).

### Inputs
Drag continuo del dial (primario) + tap Confirmar. Opcional: doble-tap para "no se / abstencion calibrada" (registra abstencion, no penaliza accuracy pero alimenta metacognicion).

### Variables de dificultad
1. `evidenceChainLen` 1..5 (numero de `SensorEvidence` encadenadas, likelihood ratios compuestos).
2. `baseRate` 0.02..0.50 (extremos hacen el neglect mas costoso).
3. `tpFpSeparation` 0.05..0.60 (|sensitivity - falsePositiveRate|; menor = mas dificil).
4. `noiseLevel` 0.0..0.30 (jitter del BayesEngine sobre los parametros mostrados).
5. `timeLimitMs` 4000..14000 (ventana de respuesta; deadline adaptativo).
6. `dialGranularity` 100..20 pasos (menos pasos = mas tosco al final, fuerza precision gruesa->fina).
7. `representationMode` {frecuencias_naturales, probabilidades} (frecuencias = mas facil; alterna como friccion deseable).
8. `distractorPriming` 0..2 (pistas irrelevantes que invitan al anclaje).

### Algoritmo adaptativo
Banda objetivo: |BUR| en [0.85, 1.15] (BUR=1.0 perfecto) en ~75-80% de trials. Staircase 1-up/2-down sobre `tpFpSeparation` y `evidenceChainLen` con histeresis (subir cuesta mas). Gates: si `FRS` (de `ScoringEquations.calculateFRS`) > 0.20 o `AGRS` > 0.75 -> deload (timeLimit +30%, chainLen-1). Plateau: sin mejora de BUR mediano en 7 sesiones cross-day -> `InterventionPlanner`-style switch de `representationMode`. Baseline personal ancla `baseRate`/`tpFpSeparation` iniciales.

### Metricas primarias
- **BUR (Bayesian Update Ratio):** `BayesEngine.evaluateBayesianUpdate(scenario, userPosterior)` en espacio logit, coerced [0,3]. Mediana por bloque sobre trials validos.
- **Calibracion (CAG):** `ScoringEquations.calculateCAG(avgConfidence, actualAccuracy)` usando |gap| binarizado a acierto-en-banda.

### Metricas secundarias
- Brier sobre estimaciones tratadas como predicciones (`calculateBrierScore`).
- Direccion del sesgo: media firmada de (userPosterior - mathPosterior) -> over vs under-updating.
- RT mediano de HITs (estimaciones en banda) sobre `cleanRts` post-MAD.
- IES = RT_hits / accuracy_en_banda.

### Senales de fatiga o mala calidad
RT drift intra-sesion (slope RT vs indice de trial); aumento de confirmaciones <500 ms; aumento de respuestas pegadas al default; subida de `FRS`. Cualquiera marca el bloque `confident=false`.

### Scoring sugerido
`scoreBayes = w_bur*f(BUR) + w_cal*(1-CAGabs) + w_speed*g(IES)` con `f(BUR)=exp(-((ln BUR)^2)/k)`, `w_bur=0.55, w_cal=0.30, w_speed=0.15`. Barra `null` si n<MIN_N o `confident=false`. `preliminary=true` siempre.

### Antitrampa / calidad de datos
`AGRS` (`calculateAGRS(rt, spatialEntropy, isDialDefault)`) marca dial-default y RT<400 ms; entropia espacial baja del recorrido del dial (movimientos identicos repetidos) -> gaming. Trials con confirmacion sin desplazamiento se descartan del scoring pero se loguean. Seed determinista permite auditoria.

### Variantes
1. **Cadena de evidencia secuencial:** evidencias llegan una a una; el usuario reactualiza el dial tras cada una (mide actualizacion incremental, no solo final).
2. **Frecuencias naturales vs probabilidades:** mismo escenario en ambos formatos para medir el efecto de representacion intra-sujeto.
3. **Contra-evidencia tardia:** una evidencia final contradice las previas; mide si el usuario sobre-corrige o ancla.

### UX
Pantalla: panel de evidencia arriba, dial semicircular grande abajo (zona tactil >=48 dp), barras TP/FP/base-rate como glyphs no numericos. Feedback: aguja objetivo aparece con microanimacion funcional de 180 ms mostrando el gap; color semantico por signo (no solo hue). Duracion ideal: 6-9 min (24-32 trials).

### Implementacion KMP
Renderer NUEVO: `dial` continuo (no existe; reusa patron de captura de `RecordedTrial` con `responseKey`=posicion normalizada). Modelo:
```kotlin
@Serializable data class BayesTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val scenarioSeed: Long, val userPosterior: Double,
  val mathPosterior: Double, val bur: Double,
  val latencyMs: Long, val movedFromDefault: Boolean)
```
Eventos: `bayes_trial_confirmed`, `bayes_deload`, `bayes_plateau`. Estado minimo: `BayesArenaStateHolder(generators=registry[E1])` clock-free (EpochClock). Motores: `BayesEngine.generateScenario`/`evaluateBayesianUpdate` (autoridad), `ScoringEquations.calculateCAG/calculateBrierScore/calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine.render(CoachSignals(bur=..., cag=...))`. Persistencia: `SecureDecisionSessionRepository` (solo summaries). Registry: `DecisionesExerciseGenerators.registry[E1_BAYES_DIAL]`.

### Riesgos
El dial continuo puede medir destreza motora fina en vez de razonamiento (confound). El formato textual introduce dependencia de lectura. Over-updating y base-rate neglect pueden confundirse si el escenario no separa bien ambos errores.

### Revision critica
El Revisor exige: (1) **separar destreza motora de razonamiento** -> `dialGranularity` gruesa (20 pasos) en la condicion de medida primaria para que la precision motora no domine; banda de acierto >= 1 paso. (2) **El "math posterior" mostrado debe usar el MISMO BayesEngine que clasifica**, o el feedback miente. (3) BUR via logit colapsa a 1.0 cuando logitMath~0 (priori = posterior): esos trials NO miden actualizacion -> deben excluirse del scoring, no contarse como BUR perfecto. (4) Texto = riesgo cultural/idioma; exige plantillas neutras y validacion de legibilidad. Sin estas 4 correcciones, descartado.

### Veredicto
Aprobado con cambios: excelente senal bayesiana si se neutraliza el confound motor y se excluyen los trials priori=posterior del BUR.

---

## E2 — Triaje de Reversibilidad: Puerta de Una Via vs Dos Vias

### Modulo
Principal: Decisiones (clasificacion reversible/irreversible + control inhibitorio de la prisa). Secundarios: Control inhibitorio (esperar lo correcto en irreversibles), Regulacion del impulso.

### Objetivo cognitivo
Entrenar la discriminacion rapida entre decisiones reversibles (Type-2, optimizar velocidad) e irreversibles (Type-1, optimizar deliberacion): responder rapido cuando es barato deshacer y forzar revision cuando el coste de error es permanente. Diana: el sesgo de tratar todo como urgente (impulsividad) y el opuesto (paralisis en reversibles).

### Hipotesis de transferencia
SI (near): mejor calibracion velocidad/cautela en variantes isomorfas con costes de bloqueo distintos. NO afirmar: que el usuario sera mas prudente en la vida real, ni reduccion de impulsividad clinica.

### Loop principal
1. **Estimulo:** tarjeta de decision con marcadores de coste-de-deshacer (delayCost, lockedDuration) renderizados como icono/forma + intensidad.
2. **Decision:** swipe IZQ = "actua ya" (tratar como reversible) / swipe DER = "revisa antes" (tratar como irreversible). En irreversibles correctos: aparece una ventana de revision obligatoria (lock 2000..8000 ms) que el usuario debe respetar.
3. **Feedback:** correcto si `selectedReversible == scenario.isReversible`; en irreversible respetado, recompensa por esperar; en reversible, recompensa por rapidez.
4. **Adaptacion:** ajusta `irreversibility`, `delayCostRate`, ambiguedad de marcadores.
5. **Siguiente trial.**

### Reglas
En reversibles: scoring premia RT bajo + acierto. En irreversibles: responder antes del lock = error de impulsividad (penaliza fuerte aunque el lado sea correcto). El estimulo nunca dice "irreversible"; el usuario lo deriva de los marcadores de coste.

### Inputs
Swipe horizontal (primario) + en irreversibles un tap de confirmacion solo habilitado tras el lock (drag-to-confirm). Sin teclado.

### Variables de dificultad
1. `irreversibilityProb` 0.30..0.70 (mezcla de tipos; cerca de 0.5 = max incertidumbre).
2. `markerAmbiguity` 0.0..0.40 (ruido en los marcadores de coste).
3. `delayCostRate` 0.1..1.0 (penalizacion por segundo de demora en reversibles).
4. `lockDurationMs` 2000..8000 (ventana de espera en irreversibles).
5. `timePressureMs` 1200..4000 (deadline visible que tienta a impulsar).
6. `distractorUrgencyCues` 0..3 (senuelos de "urgencia" falsa que invitan a swipe rapido).
7. `interleaveRatio` (proporcion reversible/irreversible por bloque; entrelazado fuerza seleccion de regla).

### Algoritmo adaptativo
Banda: accuracy de clasificacion 0.75-0.85. 1-up/2-down sobre `markerAmbiguity`. Gate de impulsividad: tasa de respuestas-antes-del-lock alta -> `ContainImpulsivity`-style: sube `lockDurationMs` y baja `timePressure`. `FRS`>0.20 -> deload. Plateau cross-day -> switch a variante de coste-asimetrico.

### Metricas primarias
- **Accuracy de triaje** (clasificacion correcta del tipo), separando por tipo.
- **Tasa de violacion de lock** (impulsividad en irreversibles) = respuestas antes de `validateLockDuration`.

### Metricas secundarias
- RT mediano en reversibles (HITs) sobre `cleanRts`.
- `IED` (`ScoringEquations.calculateIED(icd, rt, timeLimit, isReversible)`) que penaliza demora con lambda 0.8 reversible / 0.15 irreversible.
- Penalty de demora acumulada (`ReversibilityEngine.calculateDelayPenalty`).
- Omision (no responder) vs comision (lado erroneo) separadas.

### Senales de fatiga o mala calidad
Subida de violaciones de lock + caida de accuracy con RT estable (impulsividad por fatiga); drift de RT en reversibles; `FRS` alto. Marca bloque `confident=false`.

### Scoring sugerido
`scoreTriage = w_acc*accuracyTriage + w_imp*(1 - lockViolationRate) + w_eff*meanIED_reversibles`, `w_acc=0.45, w_imp=0.35, w_eff=0.20`. Lock-violation pesa fuerte para que la rapidez nunca compense violar la cautela. `null` si n insuficiente.

### Antitrampa / calidad de datos
`AGRS` con `isDialDefault`-analogo = "siempre el mismo swipe" (entropia de respuesta baja). RT<400 ms en irreversibles = pre-respuesta. Seed reproduce la mezcla exacta de tipos. Respuestas alternando mecanicamente IZQ/DER sin correlacion con marcadores -> flag de patron.

### Variantes
1. **Coste asimetrico:** irreversibles con coste de error variable; mide si el usuario calibra cautela al coste, no solo al tipo.
2. **Reversibilidad parcial:** decisiones "reversibles con coste"; tres salidas (actua/revisa/mitiga) -> requiere renderer de 3 zonas.
3. **Presion temporal creciente:** deadline se acorta intra-bloque; mide robustez del control inhibitorio bajo prisa.

### UX
Pantalla: tarjeta central, dos mitades tactiles laterales (IZQ/DER) cubriendo toda la pantalla; marcadores de coste como glyph+forma (no solo color). Lock = anillo de progreso que se llena (microanimacion funcional, no decorativa). Feedback: verde/forma-check vs rojo/forma-stop. Duracion: 5-7 min.

### Implementacion KMP
Renderer: reusa **choice** (LEFT/RIGHT) + estado nuevo `LockState` (analogo a `FocusFieldState.Stop`). Modelo:
```kotlin
@Serializable data class ReversibilityTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val scenarioSeed: Long, val selectedReversible: Boolean,
  val isReversible: Boolean, val respondedBeforeLock: Boolean,
  val latencyMs: Long, val delayPenalty: Double)
```
Eventos: `triage_swipe`, `lock_violation`, `lock_respected`, `triage_deload`. Motores: `ReversibilityEngine.generateScenario/validateLockDuration/calculateDelayPenalty/classifyDecision` (autoridad), `ScoringEquations.calculateIED/calculateFRS/calculateAGRS`, `CoachEngine` (template impulsivo `irreversibleReview<2000ms`). Persistencia summaries. Registry `[E2_REVERSIBILITY_TRIAGE]`. Reusa `DecisionSessionOrchestrator.reversibilityRound`.

### Riesgos
El "lock obligatorio" puede sentirse como friccion artificial si no se justifica como medida. Riesgo de que el usuario aprenda el patron de marcadores (familiaridad UI) en vez de razonar coste. La banda de tipos cerca de 0.5 puede frustrar.

### Revision critica
Revisor: (1) **El lock no debe penalizar la espera honesta**: si el usuario espera y luego acierta, la espera es la conducta correcta -> el scoring debe premiar lock-respetado, nunca tratar el tiempo de espera como "lentitud" via IES. (2) Marcadores deben **rotarse/variar visualmente** entre sesiones para que mida derivacion de coste, no memorizacion de iconos. (3) `calculateDelayPenalty` solo aplica a reversibles -> en irreversibles NO usar IES de velocidad como penalizacion (contradiccion ya cubierta por lambda 0.15, pero el UI no debe mostrar "demasiado lento" en irreversibles). Corregir 1-3 antes de aprobar.

### Veredicto
Aprobado con cambios: senal de impulsividad/cautela muy limpia si el lock premia la espera correcta y los marcadores rotan.

---

## E3 — Forecast Calibrado: Apuesta tu Confianza

### Modulo
Principal: Metacognicion / Decisiones (calibracion confianza-acierto). Secundarios: Razonamiento probabilistico, Coraje decisional (no colapsar siempre al 50%).

### Objetivo cognitivo
Entrenar la calibracion: que la confianza declarada coincida con la frecuencia real de acierto (90% de confianza => acierto 90% de las veces). Diana: sobreconfianza (confianza > acierto) e infraconfianza (lo opuesto), y la cobardia metacognitiva de declarar siempre confianza media.

### Hipotesis de transferencia
SI (near): mejor calibracion en juicios binarios isomorfos; reduccion de la brecha confianza-acierto en variantes no entrenadas. NO afirmar: que el usuario sera mejor pronosticador en su dominio profesional, ni claims clinicos.

### Loop principal
1. **Estimulo:** pregunta binaria con evidencia parcial (A vs B), resoluble pero incierta.
2. **Decision:** elige A o B + fija confianza [0.5, 1.0] con un slider de "apuesta".
3. **Feedback:** se revela el resultado; scoring proper (Brier) recompensa la honestidad de la confianza, no solo el acierto.
4. **Adaptacion:** ajusta dificultad de las preguntas para mantener accuracy en banda y forzar uso del rango completo de confianza.
5. **Siguiente trial.**

### Reglas
Confianza es obligatoria y continua [0.5,1.0]. El score usa una regla de puntuacion proper (Brier): declarar alta confianza y fallar penaliza mas que declarar confianza media. Declarar siempre 0.5 minimiza varianza pero tope el score (no se puede ganar mucho sin arriesgar calibracion). Diana metacognitiva: `calculateDecisionalCourage` (sd de confianzas) debe ser > 0.

### Inputs
Tap A/B (eleccion) + drag del slider de confianza. Opcional voz: declarar confianza hablada (diferido, no V1).

### Variables de dificultad
1. `evidenceStrength` 0.55..0.95 (probabilidad real de que la opcion "mejor" sea correcta).
2. `optionSeparation` 0.05..0.40 (diferencia de evidencia entre A y B).
3. `nQuestionsPerBlock` 12..30.
4. `confidenceGranularity` (pasos del slider 5..20).
5. `timeLimitMs` 5000..12000.
6. `distractorEvidence` 0..2 (pistas que apoyan la opcion erronea).
7. `domainRotation` (rota familias de contenido para evitar conocimiento previo).

### Algoritmo adaptativo
Banda: accuracy 0.65-0.80 (suficiente incertidumbre para que la confianza importe). Si la sd de confianzas (`calculateDecisionalCourage`) ~0 durante varios bloques -> el coach empuja a usar el rango; no se sube dificultad para no premiar el colapso al 50%. `FRS`>0.20 -> deload. Plateau de Brier cross-day -> rota dominio.

### Metricas primarias
- **Brier score** (`ScoringEquations.calculateBrierScore`) — menor = mejor, proper scoring.
- **Calibration error** (`calculateCalibrationError`, 5 bins width 0.1) — desviacion confianza vs acierto por bin.

### Metricas secundarias
- **CAG** (`calculateCAG(avgConfidence, actualAccuracy)`) — direccion del sesgo (over/under).
- **Decisional courage** (`calculateDecisionalCourage`) — sd de confianzas (anti-colapso).
- Accuracy bruta (control, no es la diana).
- RT mediano de decision.

### Senales de fatiga o mala calidad
Colapso de confianza a un valor unico (sd->0); accuracy cae a azar (0.5); RT drift; respuestas <500 ms. `FRS` alto. Marca `confident=false`.

### Scoring sugerido
`scoreCalib = w_brier*(1 - Brier) + w_calerr*(1 - calErr) + w_courage*clamp(courage)`, `w_brier=0.50, w_calerr=0.35, w_courage=0.15`. Penaliza tanto sobre como infraconfianza via calErr. `null` si n<MIN bins suficientes.

### Antitrampa / calidad de datos
Detectar confianza-constante (sd~0) como degenerado: no puntua alto. Detectar siempre-misma-opcion. `AGRS` por RT<400 ms. Las respuestas deben correlacionar confianza con dificultad real del item (si confianza no covaria con evidencia -> flag). Seed reproduce items.

### Variantes
1. **Mercado de prediccion personal:** confianza como "fichas apostadas" con payoff proper-scored; mide gestion de riesgo metacognitivo.
2. **Confianza pre y post evidencia:** declarar confianza antes y despues de ver una pista; mide actualizacion metacognitiva.
3. **Calibracion bajo presion:** deadline corto; mide si la calibracion se degrada con prisa (la sobreconfianza suele subir).

### UX
Pantalla: pregunta + dos botones A/B grandes + slider de confianza con etiqueta "% seguro" (glyph, no solo numero). Feedback: barra de Brier del trial + tendencia de calibracion diferida (curva confianza-vs-acierto). Microanimacion: la ficha de confianza "cae" en el bin (funcional). Duracion: 6-8 min.

### Implementacion KMP
Renderer: **choice** (A/B) + slider de confianza (componente nuevo reutilizable con E1). Modelo:
```kotlin
@Serializable data class ForecastTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val chosen: Int, val confidence: Double,
  val correct: Boolean, val latencyMs: Long)
```
`BrierPrediction` ya existe en `Models.kt`. Eventos: `forecast_committed`, `calibration_bin_update`, `courage_low_nudge`. Motores: `ScoringEquations.calculateBrierScore/calculateCalibrationError/calculateCAG/calculateDecisionalCourage/calculateFRS`, `CoachEngine` (templates sobreconfiado CAG>+0.15, subconfiante CAG<-0.15). Persistencia summaries + serie cross-day para calibration curve. Registry `[E3_FORECAST_CALIB]`.

### Riesgos
Las preguntas binarias dependen de un banco de items resolubles, no triviales y libres de conocimiento previo (riesgo cultural/idioma fuerte). El Brier puede premiar conservadurismo si la dificultad no se calibra. Confundir acierto con calibracion.

### Revision critica
Revisor: (1) **El banco de items es el punto debil**: si un item se puede acertar por conocimiento, mide cultura, no calibracion -> exige items de evidencia-controlada (probabilidad de acierto fijada por diseno, no por saber la respuesta). (2) **Brier solo es proper si el outcome es objetivamente verificable** -> nada de preguntas de opinion. (3) Colapso al 50% no debe puntuar alto NI penalizar tanto que empuje a sobreconfianza temeraria; el peso de `courage` debe ser suave (0.15) y nunca dominar. (4) calErr con 5 bins necesita n>=~25 para ser fiable -> reportar `null` por debajo. Sin items evidence-controlled, descartado.

### Veredicto
Aprobado con cambios: la mejor senal metacognitiva del modulo si y solo si los items son de evidencia controlada (no trivia cultural).

---

## E4 — Check-in Cognitivo Go/No-Go: Estado de Disponibilidad

### Modulo
Principal: Decisiones (readiness / CRI antes de decidir). Secundarios: Control inhibitorio (Go/No-Go base), Atencion sostenida breve.

### Objetivo cognitivo
Medir y entrenar la **disponibilidad cognitiva** (readiness) antes de una sesion de decision: combina accuracy Go/No-Go, variabilidad de RT y fatiga subjetiva en un indice CRI. No es un drill de fondo: es la puerta de calibracion que protege la validez de los demas ejercicios (no decidir cansado).

### Hipotesis de transferencia
SI (near): autoconciencia de estado pre-tarea; mejor deteccion de cuando posponer una decision. NO afirmar: medicion de "estado mental" clinico, deteccion de fatiga patologica, ni diagnostico de TDAH/atencion.

### Loop principal
1. **Estimulo:** secuencia rapida de estimulos Go (responder) / No-Go (inhibir) de alta densidad.
2. **Decision:** tap en Go, inhibir en No-Go.
3. **Feedback:** minimal por trial (para no romper el flujo); al final, CRI + recomendacion (proceder / deload / posponer).
4. **Adaptacion:** corto y de dificultad casi fija (es medida, no entrenamiento adaptativo agresivo); ajusta solo densidad para mantener carga.
5. **Siguiente:** alimenta el `CheckInInput` del flujo de Decisiones.

### Reglas
Es un bloque de **calibracion**, no de entrenamiento (mode != TRAINING, nunca adapta para subir score). Duracion fija corta (~60-90 trials). El No-Go ratio fijo para comparabilidad cross-sesion. Tras el bloque, se pregunta fatiga subjetiva (0..1) como covariable.

### Inputs
Tap unico (single-tap) en Go; abstencion en No-Go. Slider de fatiga subjetiva al final (1 vez).

### Variables de dificultad
1. `noGoRatio` 0.15..0.30 (fijo por sesion para comparabilidad; valor del baseline).
2. `stimulusOnsetMs` 600..1200 (ritmo).
3. `responseWindowMs` 400..900.
4. `blockTrials` 60..90.
5. `stimulusContrast` (legibilidad; control de carga perceptiva, no cognitiva).
6. `interTrialJitter` 0..200 ms (evita anticipacion ritmica).

### Algoritmo adaptativo
Minimo por diseno: NO sube dificultad para inflar CRI. Solo deload si fatiga subjetiva extrema (>0.8) acorta el bloque. El CRI resultante alimenta el gate del resto de ejercicios (CRI bajo -> recomienda posponer drills pesados). Baseline personal define `noGoRatio` y RT esperado de referencia.

### Metricas primarias
- **CRI** (`ScoringEquations.calculateCRI(goNoGoAccuracy, reactionTimeCV, subjectiveFatigue)`, pesos 50/30/20).
- **RT-CV** (consistencia) sobre `cleanRts` de Go-HITs.

### Metricas secundarias
- Comision en No-Go (impulsividad) vs omision en Go (inatencion), separadas.
- RT mediano Go-HITs.
- Fatigue drift (slope RT vs posicion de trial).
- Concordancia fatiga subjetiva vs RT-CV objetiva (metacognicion de estado).

### Senales de fatiga o mala calidad
RT-CV alto, drift adverso, subida de comision, fatiga subjetiva alta. El propio ejercicio ES el detector de fatiga del modulo; su salida degrada `confident` de la sesion completa de decisiones.

### Scoring sugerido
`CRI` directo (0..1) como readiness. No produce "score de habilidad" (no es entrenamiento). Recomendacion: CRI>=0.7 proceder; 0.5-0.7 deload; <0.5 posponer. Siempre `preliminary` hasta tener baseline.

### Antitrampa / calidad de datos
`AGRS` (RT<400 ms = anticipacion). Responder-a-todo (sin inhibir) detectado por comision~ratio. Patron ritmico detectado por jitter-correlacion. Como es medida, el anti-trampa protege la validez del gate, no un leaderboard.

### Variantes
1. **Sustained (SART):** Go frecuente, No-Go raro (atencion sostenida; comision = lapsus atencional).
2. **Auditivo:** estimulo sonoro Go/No-Go (reduce dependencia visual; accesibilidad).
3. **Doble-CRI pre/post sesion:** mide drift de readiness atribuible a la propia sesion de decisiones.

### UX
Pantalla: estimulo central grande, zona tactil = pantalla completa (single-tap), feedback por trial minimo (flash breve). Final: tarjeta CRI con recomendacion honesta y no culpabilizante ("hoy conviene una sesion ligera"). Duracion: 2-3 min (es breve a proposito).

### Implementacion KMP
Renderer: reusa **single-tap** (`FocusStimulusField` analogo) + `classify` Go/No-Go ya existente en patron focus. Modelo:
```kotlin
@Serializable data class CheckInRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val goNoGoAccuracy: Double, val reactionTimeCV: Double,
  val subjectiveFatigue: Double, val criValue: Double, val endedAtMs: Long)
```
`CognitiveReadinessState` y `CheckInInput` ya existen. Eventos: `checkin_started`, `checkin_cri_computed`, `checkin_recommend_postpone`. Motores: `ScoringEquations.calculateCRI` (autoridad), `DecisionSessionOrchestrator.checkIn` (ya lo computa), `CoachEngine` (template fatigado FRS>0.20). Persistencia: summary CRI por sesion (serie cross-day para readiness trend). Registry `[E4_CHECKIN_GONOGO]`. Ya wired como stage `CheckIn` del flow.

### Riesgos
Riesgo de que el usuario lo perciba como "examen" y se estrese (contamina la medida). Si se gamifica el CRI, se rompe su funcion de gate honesto. RT-CV es sensible a interrupciones externas (no atribuibles a cognicion).

### Revision critica
Revisor: (1) **No gamificar el CRI**: si se muestra como puntuacion a maximizar, el usuario lo manipula y se pierde el gate -> presentarlo como semaforo de estado, no como score. (2) RT-CV contaminado por interrupciones del entorno -> necesita deteccion de outliers (pausa larga) y descarte, no penalizacion. (3) Bloque demasiado corto (60 trials) puede dar `confident=false` para RT-CV -> definir MIN_N y reportar `null` honestamente, nunca un CRI fabricado. (4) fatiga subjetiva auto-reportada es ruidosa; debe ser covariable, jamas el unico driver. Corregir antes de aprobar.

### Veredicto
Aprobado con cambios: gate de readiness imprescindible para la validez del modulo, siempre que se presente como semaforo (no score) y descarte interrupciones.

---

## E5 — Escaner de Alternativas: Anti-Fijacion en la Primera Opcion

### Modulo
Principal: Decisiones (amplitud de busqueda / alternativesScannedRatio del ICD). Secundarios: Flexibilidad cognitiva, Control del sesgo de anclaje/satisfaccion.

### Objetivo cognitivo
Entrenar la **busqueda exhaustiva de alternativas** antes de comprometer una decision: combatir el "satisficing" prematuro (elegir la primera opcion aceptable) y el anclaje en la opcion saliente. Diana: el ratio de alternativas realmente evaluadas vs disponibles, y la deteccion de la opcion dominante cuando no es la mas obvia.

### Hipotesis de transferencia
SI (near): mayor amplitud de exploracion en problemas de eleccion multiple isomorfos; menos fijacion en la primera opcion. NO afirmar: mejor toma de decisiones complejas reales, creatividad general, ni IQ.

### Loop principal
1. **Estimulo:** set de N opciones (3..8) cada una con varios atributos (dominio multidimensional); una opcion es dominante/optima pero no necesariamente la primera ni la mas saliente.
2. **Decision:** el usuario explora (debe abrir/revelar atributos) y selecciona la mejor; el sistema registra cuantas alternativas inspecciono antes de decidir.
3. **Feedback:** correcto si eligio la dominante; muestra cuantas opciones ignoro y si la ignorada era mejor.
4. **Adaptacion:** ajusta N, numero de atributos y donde se ubica la dominante.
5. **Siguiente.**

### Reglas
La opcion correcta es la dominante por atributos (objetiva, no de gusto). El acierto exige eleccion correcta; el ICD pondera `alternativesScannedRatio` (peso 0.15). Elegir rapido la primera sin explorar = baja exploracion aunque acierte por suerte. La dominante rota de posicion (anti-aprendizaje de "siempre la 2a").

### Inputs
Tap para revelar atributos de cada opcion (seleccion multiple de exploracion) + tap final de seleccion. Opcional: long-press para "descartar" una opcion (registra poda activa).

### Variables de dificultad
1. `nOptions` 3..8.
2. `nAttributes` 2..5 (dimensiones por opcion).
3. `dominanceMargin` 0.05..0.40 (cuanto gana la optima; menor = mas hay que explorar).
4. `salienceMismatch` 0..1 (la opcion mas vistosa NO es la mejor).
5. `optimalPosition` (posicion de la dominante; rota).
6. `timeLimitMs` 6000..20000.
7. `distractorDominance` 0..1 (una opcion casi-dominante que tienta a satisficing).

### Algoritmo adaptativo
Banda: accuracy 0.70-0.85. 1-up/2-down sobre `dominanceMargin` y `nOptions`. Si `alternativesScannedRatio` cronicamente bajo con accuracy alta (suerte) -> sube `salienceMismatch` para forzar exploracion. `FRS`>0.20 -> deload. Plateau -> sube `nAttributes`.

### Metricas primarias
- **Accuracy de eleccion dominante.**
- **alternativesScannedRatio** (opciones inspeccionadas / disponibles) — feed directo al `ICD`.

### Metricas secundarias
- **ICD** (`ScoringEquations.calculateICD(objectivesAlignment, bur, biasDetected, alternativesScannedRatio)`, peso scan 0.15).
- RT hasta primera seleccion vs RT total (impulsividad de cierre).
- Tasa de "primera-opcion-aceptable" (satisficing index).
- Deteccion de la dominante cuando salienceMismatch alto (resistencia al anclaje).

### Senales de fatiga o mala calidad
Caida del scan ratio con el tiempo en tarea (exploracion se acorta por fatiga); subida de satisficing; RT drift. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreScan = w_acc*accuracy + w_scan*scanRatio + w_anchor*(1 - anchorErrorRate)`, `w_acc=0.50, w_scan=0.30, w_anchor=0.20`. Penaliza acertar-por-suerte con scan bajo (limita w_acc si scanRatio muy bajo). `null` si n insuficiente.

### Antitrampa / calidad de datos
Detectar "revelar todo mecanicamente sin leer" via RT por revelacion (<150 ms = no leyo) -> scan inflado falso, se descuenta. Seleccion <400 ms tras revelar = no evaluo. Seed reproduce el set. `AGRS` por patron de exploracion identico.

### Variantes
1. **Coste de exploracion:** revelar atributos cuesta "tiempo/fichas"; mide trade-off exploracion vs explotacion (no explorar de mas sin razon).
2. **Atributos en conflicto:** ninguna opcion domina en todo; exige integracion multi-atributo (la "mejor" es por suma ponderada).
3. **Opcion oculta superior:** la dominante esta en la ultima posicion no revelada por defecto; mide resistencia a parar pronto.

### UX
Pantalla: grid/lista de opciones con atributos colapsados; tap revela; barra de "exploradas X/N". Seleccion final con confirmacion. Feedback: resalta la dominante y las ignoradas mejores. Sin animacion decorativa. Duracion: 6-9 min.

### Implementacion KMP
Renderer: NUEVO (lista revelable + seleccion; reusa patron de captura multi-tap). Modelo:
```kotlin
@Serializable data class ScanTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val nOptions: Int, val scannedCount: Int,
  val chosenIndex: Int, val optimalIndex: Int,
  val revealLatenciesMs: List<Long>, val totalLatencyMs: Long)
```
Eventos: `option_revealed`, `option_pruned`, `scan_committed`, `scan_deload`. Motores: `ScoringEquations.calculateICD` (scanRatio es input directo), `calculateFRS`, `calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (template paralisis si scan excesivo + RT enorme; impulsivo si scan~0). Persistencia summaries. Registry `[E5_ALTERNATIVE_SCAN]`. Alimenta `DecisionRound.objectivesAlignment` via dominante correcta.

### Riesgos
Definir "dominante objetiva" es dificil con atributos en conflicto (puede volverse subjetivo). El scan ratio puede inflarse abriendo todo sin leer (medido pero ruidoso). Riesgo de medir paciencia/perfeccionismo en vez de calidad de busqueda.

### Revision critica
Revisor: (1) **"Dominante" debe ser matematicamente inequivoca** (dominancia de Pareto estricta o suma ponderada con pesos mostrados), o el feedback es opinable -> en variante de conflicto, mostrar los pesos para que el optimo sea verificable. (2) Scan ratio alto NO es bueno per se: explorar de mas con coste = ineficiencia -> la variante con coste de exploracion debe penalizar over-scanning, no solo under-scanning. (3) Revelar-sin-leer infla la metrica diana -> el gate de RT-por-revelacion es OBLIGATORIO, no opcional. (4) Riesgo idioma en atributos textuales -> usar atributos numericos/iconicos comparables sin lenguaje. Sin 1-4, descartado.

### Veredicto
Aprobado con cambios: ataca un sesgo real (satisficing/anclaje) con senal clara solo si la dominancia es inequivoca y el over-scanning se penaliza.

---

## E6 — Deteccion de Sesgo: Caza-Trampas de Razonamiento

### Modulo
Principal: Decisiones (biasDetected del ICD / metacognicion de sesgos). Secundarios: Razonamiento critico, Control inhibitorio de la respuesta intuitiva.

### Objetivo cognitivo
Entrenar la **deteccion del sesgo activo** en un micro-escenario de decision: identificar rapidamente cual heuristica/sesgo esta empujando hacia la respuesta intuitiva-pero-erronea (anclaje, base-rate neglect, framing, coste hundido, confirmacion) y vetar la respuesta automatica. Diana: la respuesta System-1 rapida que cae en la trampa.

### Hipotesis de transferencia
SI (near): mejor reconocimiento del mismo sesgo en variantes isomorfas; mayor latencia ante items-trampa (deliberacion). NO afirmar: que el usuario sera "menos sesgado" globalmente, debiasing duradero, ni transferencia a decisiones reales (la literatura de debiasing es debil; solo near transfer).

### Loop principal
1. **Estimulo:** micro-escenario con una respuesta intuitiva-trampa y una correcta; el sesgo esta "plantado".
2. **Decision:** elegir la respuesta correcta Y (segunda fase) etiquetar que sesgo operaba (seleccion de un set de 5 sesgos).
3. **Feedback:** correcto/incorrecto en respuesta + en etiquetado del sesgo; explica la trampa.
4. **Adaptacion:** ajusta sutileza del sesgo y atractivo de la trampa.
5. **Siguiente.**

### Reglas
Doble juicio: (a) respuesta correcta, (b) identificacion del sesgo. `biasDetected` (boolean, peso 0.20 en ICD) = etiqueto correctamente el sesgo. La respuesta-trampa es la intuitiva; resistirla exige inhibir. Items rotan sesgo para evitar "siempre es anclaje".

### Inputs
Tap (eleccion de respuesta) + seleccion de etiqueta de sesgo (choice de 5). Sin texto libre en V1.

### Variables de dificultad
1. `biasType` {anchoring, base_rate, framing, sunk_cost, confirmation} (5 familias).
2. `trapSalience` 0.3..0.9 (cuan atractiva es la respuesta-trampa).
3. `biasSubtlety` 0.0..0.6 (cuan oculto esta el sesgo).
4. `distractorBiases` 1..4 (etiquetas senuelo plausibles).
5. `timeLimitMs` 5000..12000 (presion que favorece System-1).
6. `framingIntensity` (en items de framing, fuerza del encuadre).
7. `interleaveBias` (mezcla de tipos por bloque; entrelazado fuerza identificacion).

### Algoritmo adaptativo
Banda: accuracy de respuesta 0.70-0.85. 1-up/2-down sobre `biasSubtlety`. Si accuracy de respuesta alta pero `biasDetected` bajo (acierta sin entender) -> sube peso de la fase de etiquetado / baja `trapSalience`. `FRS`>0.20 -> deload. Plateau -> rota a familias de sesgo menos entrenadas (weakness-first).

### Metricas primarias
- **Accuracy de respuesta** (resistir la trampa).
- **biasDetectionAccuracy** (etiquetado correcto del sesgo) — feed a `ICD.biasDetected`.

### Metricas secundarias
- **ICD** (`calculateICD(..., biasDetected, ...)`).
- RT en items-trampa vs items-control (latencia extra = deliberacion sana).
- Tasa de captura por la trampa por familia de sesgo (perfil de vulnerabilidad).
- Concordancia respuesta-correcta ^ sesgo-correcto (entendimiento real vs suerte).

### Senales de fatiga o mala calidad
Subida de capturas por trampa con RT bajo (vuelta a System-1 por fatiga); etiquetado al azar; RT drift. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreBias = w_resp*accuracyResp + w_detect*biasDetectAcc + w_consistency*jointCorrectRate`, `w_resp=0.40, w_detect=0.40, w_consistency=0.20`. `jointCorrect` = respuesta Y sesgo correctos (entendimiento). `null` si n por familia insuficiente.

### Antitrampa / calidad de datos
Acertar respuesta + etiquetar sesgo al azar detectado por baja `jointCorrectRate` con accuracy alta. RT<400 ms = no leyo. Seed reproduce items. Detectar memorizacion de items repetidos (excluir items vistos recientemente, como `excludeRecentWordIds` en memory).

### Variantes
1. **Solo deteccion:** dado el escenario y la respuesta, identificar el sesgo (aisla la metacognicion).
2. **Veto temporal:** el sistema "sugiere" la respuesta-trampa y el usuario debe aceptar/vetar bajo deadline (mide inhibicion del default).
3. **Sesgo compuesto:** dos sesgos operan a la vez; identificar el dominante (dificultad alta).

### UX
Pantalla: escenario breve + 2-3 botones de respuesta + panel de etiquetas de sesgo. Feedback en dos pasos (respuesta, luego sesgo) con explicacion concisa no clinica. Sin animacion decorativa. Duracion: 7-10 min.

### Implementacion KMP
Renderer: reusa **choice** (respuesta) + **choice** secundario (etiqueta). Modelo:
```kotlin
@Serializable data class BiasTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val biasType: String,
  val respondedCorrect: Boolean, val biasLabeledCorrect: Boolean,
  val latencyMs: Long, val capturedByTrap: Boolean)
```
Eventos: `bias_response`, `bias_labeled`, `trap_captured`, `bias_deload`. Motores: `ScoringEquations.calculateICD` (biasDetected input), `calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (impulsivo/sobreconfiado segun captura). Persistencia summaries + perfil de vulnerabilidad por familia. Registry `[E6_BIAS_HUNTER]`. Banco de items con `excludeRecent` para anti-memorizacion.

### Riesgos
Debiasing es notoriamente dificil de transferir (riesgo de overclaim). Items dependen fuertemente de lenguaje/cultura (framing, escenarios). Memorizacion de items con repeticion. Etiquetar el sesgo "correcto" puede ser discutible en items ambiguos.

### Revision critica
Revisor: (1) **Riesgo de overclaim de debiasing**: la evidencia cientifica de transferencia de debiasing es debil -> el copy debe limitarse a "reconocimiento del patron en esta tarea", jamas "te volveras menos sesgado". (2) Items textuales = bomba cultural/idioma -> requiere validacion de items y, donde sea posible, formato numerico (base-rate, anclaje). (3) El "sesgo correcto" debe ser INEQUIVOCO por diseno (un solo sesgo plantado en V1; variante compuesta diferida) o el etiquetado es injusto. (4) Banco grande + `excludeRecent` obligatorio o mide memoria, no deteccion. (5) Sin un banco validado, esto es trivia de psicologia, no entrenamiento. Corregir 1-5.

### Veredicto
Aprobado con cambios: valioso para metacognicion de sesgos solo con copy honesto (no debiasing claim), items inequivocos y anti-memorizacion estricto.

---

## E7 — Presupuesto de Decision: Asignacion bajo Restriccion

### Modulo
Principal: Decisiones (decision multi-atributo bajo restriccion / CDE complejidad). Secundarios: Memoria de trabajo (mantener restricciones), Planificacion.

### Objetivo cognitivo
Entrenar la **decision optimizadora bajo restriccion**: asignar un recurso limitado entre opciones con retornos y costes distintos para maximizar un objetivo, manteniendo varias restricciones en memoria de trabajo simultaneamente. Diana: la integracion multi-atributo correcta y el respeto de constraints sin desbordar la WM.

### Hipotesis de transferencia
SI (near): mejor optimizacion en problemas de asignacion isomorfos; mayor respeto de restricciones bajo carga. NO afirmar: mejor gestion financiera/temporal real, planificacion de vida, ni inteligencia general.

### Loop principal
1. **Estimulo:** un presupuesto B y un set de items con coste/retorno (y restricciones: max por categoria, minimos obligatorios).
2. **Decision:** el usuario asigna (drag de unidades / steppers) hasta agotar o decidir parar; debe respetar restricciones.
3. **Feedback:** retorno obtenido vs optimo (solver), violaciones de restriccion senaladas.
4. **Adaptacion:** ajusta numero de items, restricciones y holgura del presupuesto.
5. **Siguiente.**

### Reglas
El optimo lo computa un solver determinista (knapsack/LP pequeno) -> feedback verificable. Violacion de restriccion = error duro (no cuenta el retorno). Acercarse al optimo (ratio retorno/optimo) es la senal continua. La complejidad (variables, restricciones, presion temporal, carga de memoria) se cuantifica con `CDE`.

### Inputs
Drag/steppers para asignar unidades (drag primario) + tap confirmar. Reset disponible. Sin texto.

### Variables de dificultad
1. `nItems` 3..10.
2. `nConstraints` 1..4 (restricciones simultaneas a mantener en WM).
3. `budgetSlack` 0.0..0.5 (holgura; menor = mas ajustado, mas dificil).
4. `returnVariance` (dispersion de retornos; mayor = mas importa optimizar).
5. `timePressureMs` 8000..30000 -> feed a CDE.timePressure.
6. `memoryLoad` 1..7 (restricciones ocultas que hay que recordar) -> feed a CDE.memoryLoad.
7. `reversibilityOfMoves` (si reasignar tiene coste; conecta con ReversibilityEngine).
8. `distractorHighCostItems` 0..2 (items vistosos de mala relacion retorno/coste).

### Algoritmo adaptativo
Banda: ratio retorno/optimo 0.75-0.90. 1-up/2-down sobre `nConstraints` y `budgetSlack`. Gate WM: si violaciones suben con carga -> baja `memoryLoad`. `CDE` (`calculateCDE`) modula la dificultad percibida; `FRS`>0.20 o `AGRS`>0.75 -> deload (timePressure*0.6, memoryLoad-1, igual que `DifficultyAdapter`). Plateau -> sube `returnVariance`.

### Metricas primarias
- **Optimality ratio** (retorno obtenido / retorno optimo del solver).
- **Constraint violation rate** (restricciones rotas / disponibles).

### Metricas secundarias
- **CDE** (`ScoringEquations.calculateCDE(nVar, hProb, hMax, timePressure, memoryLoad, reversibility)`) como complejidad afrontada.
- RT total y numero de reasignaciones (eficiencia de busqueda).
- Decremento de optimalidad bajo `memoryLoad` alto (capacidad WM efectiva).
- Sunk-cost index: si reasignar tiene coste, ¿se aferra a malas asignaciones previas?

### Senales de fatiga o mala calidad
Subida de violaciones con carga estable; caida de optimality ratio con RT alto (esfuerzo sin resultado); reasignaciones erraticas. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreBudget = w_opt*optimalityRatio*(1 - violationRate) + w_eff*efficiency`, con `efficiency = 1/(1+reassignments/nItems)`, `w_opt=0.80, w_eff=0.20`. Violacion anula el retorno de ese trial. `null` si n insuficiente.

### Antitrampa / calidad de datos
Asignar-todo-al-primero o patron fijo detectado por baja entropia de asignacion. Confirmar <500 ms = no penso. Seed reproduce items + optimo. Detectar "fuerza bruta" (probar todas las combinaciones via reset masivo) limitando resets. `AGRS` por patron.

### Variantes
1. **Restricciones ocultas (WM):** algunas restricciones solo se muestran al inicio y desaparecen; deben mantenerse en memoria (carga WM real).
2. **Retornos estocasticos:** cada item da retorno esperado +/- ruido; mide decision bajo incertidumbre (conecta BayesEngine).
3. **Reasignacion costosa (sunk cost):** cambiar una asignacion previa cuesta; mide resistencia al coste hundido.

### UX
Pantalla: barra de presupuesto + items con steppers/drag + panel de restricciones (visible u oculto segun variante). Feedback: barra retorno-vs-optimo + restricciones violadas resaltadas (forma+color). Sin animacion decorativa; el drag es funcional. Duracion: 7-10 min.

### Implementacion KMP
Renderer: NUEVO (**drag/stepper** de asignacion; no existe; mayor coste de UI). Modelo:
```kotlin
@Serializable data class BudgetTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val allocation: List<Int>,
  val obtainedReturn: Double, val optimalReturn: Double,
  val violations: Int, val reassignments: Int, val totalLatencyMs: Long)
```
Solver determinista en commonMain (knapsack DP / LP pequeno) = autoridad del optimo. Eventos: `budget_allocate`, `budget_violation`, `budget_committed`, `budget_deload`. Motores: `ScoringEquations.calculateCDE` (complejidad), `calculateFRS/calculateAGRS`, `DifficultyAdapter` (deload), `ReversibilityEngine` (variante reasignacion costosa), `BayesEngine` (variante estocastica), `CoachEngine`. Persistencia summaries. Registry `[E7_DECISION_BUDGET]`.

### Riesgos
Renderer drag complejo = mayor riesgo de implementacion y de medir destreza UI. El solver debe ser rapido y correcto (bug -> feedback falso). nItems alto satura WM perceptiva, no solo cognitiva. Riesgo de medir paciencia/perfeccionismo.

### Revision critica
Revisor: (1) **El solver es critico**: si el "optimo" es incorrecto, todo el feedback y scoring mienten -> el solver debe estar testeado exhaustivamente (knapsack exacto para n pequeno) y la misma fuente de verdad para feedback y score. (2) Drag mide destreza motora -> usar steppers discretos en la condicion de medida primaria (drag solo en variante avanzada). (3) `CDE` describe complejidad pero NO es la metrica de habilidad -> no confundir "afronto alta complejidad" con "decidio bien"; la diana es optimality ratio. (4) Limitar resets o el usuario hace fuerza bruta (mide perseverancia, no decision). (5) WM oculta debe verificarse que no degenere en simple memoria de listas (debe integrarse en la decision). Corregir 1-5.

### Veredicto
Aprobado con cambios: ejercicio rico de decision-bajo-restriccion, pero el mas caro de implementar; solo si el solver es correcto y se usan steppers en la medida primaria.

---

## E8 — Resolucion bajo Ambiguedad: Decidir con Informacion Incompleta

### Modulo
Principal: Decisiones (decision bajo incertidumbre / valor de informacion). Secundarios: Razonamiento bayesiano (cuando recolectar mas evidencia), Tolerancia a la ambiguedad calibrada.

### Objetivo cognitivo
Entrenar el **trade-off recolectar-mas-info vs decidir-ya**: en cada trial el usuario puede pagar (tiempo/coste) por mas evidencia que reduce incertidumbre, o decidir con lo que tiene. Diana: el sobre-recolector (paralisis, nunca decide) y el sub-recolector (decide ciego). El optimo es un umbral de valor-de-informacion.

### Hipotesis de transferencia
SI (near): mejor parada de recoleccion en problemas isomorfos; aproximacion al umbral optimo de informacion. NO afirmar: mejor toma de decisiones reales bajo incertidumbre, gestion de riesgo profesional, ni rasgos de personalidad.

### Loop principal
1. **Estimulo:** una hipotesis a decidir (A/B) con prior debil; opciones para "comprar" evidencias que actualizan el posterior (cada evidencia tiene coste y poder informativo).
2. **Decision:** en cada paso, comprar otra evidencia o comprometer la decision; al comprometer, declarar A/B (+confianza).
3. **Feedback:** resultado + comparacion con la politica optima (umbral de informacion): ¿recolecto de mas/de menos?
4. **Adaptacion:** ajusta coste de evidencia, poder informativo y prior.
5. **Siguiente.**

### Reglas
Cada evidencia comprada actualiza el posterior via `BayesEngine.calculatePosterior(baseRate, evidence)`. Decidir antes del umbral optimo = sub-recoleccion; seguir comprando tras superar el umbral = sobre-recoleccion (coste desperdiciado). El optimo se deriva del valor esperado de la informacion (VOI) determinista. El estimulo no muestra el umbral.

### Inputs
Tap "comprar evidencia" (repetible) + tap "decidir" -> choice A/B + slider confianza. Sin texto.

### Variables de dificultad
1. `evidenceCost` 0.05..0.40 (coste por evidencia; mayor = parar antes optimo).
2. `evidencePower` 0.55..0.90 (likelihood ratio por evidencia).
3. `priorStrength` 0.5..0.85 (prior inicial; cercano a 0.5 = mas info necesaria).
4. `maxEvidences` 2..8.
5. `payoffAsymmetry` (coste de error A vs B distinto; cambia el umbral optimo).
6. `timeLimitMs` 8000..25000.
7. `noiseLevel` 0.0..0.25 (jitter del BayesEngine).
8. `costVisibility` {explicito, implicito} (friccion: a veces el coste acumulado no se muestra).

### Algoritmo adaptativo
Banda: distancia al numero optimo de evidencias dentro de +-1 en 75-85% de trials. 1-up/2-down sobre `evidencePower` y `priorStrength`. Gate paralisis: si el usuario compra siempre el maximo (sobre-recoleccion cronica) -> el coach interviene y se sube `evidenceCost`. `FRS`>0.20 -> deload. Plateau -> sube `payoffAsymmetry`.

### Metricas primarias
- **Information stopping error** = |evidencesComprasdas - evidenciasOptimas| (menor = mejor).
- **Decision accuracy** (A/B correcto dado el ground truth).

### Metricas secundarias
- **BUR** del posterior final declarado (`evaluateBayesianUpdate`) — ¿integro bien la evidencia comprada?
- Net payoff = valor_acierto - coste_evidencias (eficiencia economica).
- Over vs under-collection rate (perfil: paralisis vs impulsividad).
- **CAG** sobre la confianza final (calibracion).

### Senales de fatiga o mala calidad
Subida de sobre-recoleccion (paralisis por fatiga) o de sub-recoleccion (impulsividad por fatiga); RT drift; decisiones <500 ms. `FRS` alto. `confident=false`.

### Scoring sugerido
`scoreVOI = w_stop*(1 - normStoppingError) + w_acc*accuracy + w_pay*normNetPayoff`, `w_stop=0.45, w_acc=0.35, w_pay=0.20`. Penaliza tanto comprar de mas (coste) como de menos (error). `null` si n insuficiente.

### Antitrampa / calidad de datos
Detectar "siempre compra max" o "nunca compra" (politica degenerada, no adaptada al item) via correlacion compras-vs-incertidumbre: si no covaria con `priorStrength`/`evidencePower` -> flag. Decidir <400 ms = no integro. Seed reproduce evidencias + optimo. `AGRS` por patron fijo.

### Variantes
1. **Coste creciente:** cada evidencia adicional cuesta mas; mide parada optima con coste marginal creciente.
2. **Evidencia ruidosa:** algunas evidencias son poco informativas; mide seleccion de que evidencia comprar (si hay eleccion).
3. **Payoff asimetrico:** coste de falso-negativo >> falso-positivo; el umbral optimo se desplaza; mide adaptacion al payoff.

### UX
Pantalla: medidor de posterior (barra A vs B) que se actualiza al comprar evidencia + contador de coste + botones comprar/decidir. Feedback: politica optima vs la del usuario (linea de umbral). Microanimacion funcional: la barra de posterior se mueve al integrar evidencia (180 ms). Duracion: 7-10 min.

### Implementacion KMP
Renderer: reusa **choice** (A/B) + slider confianza (compartido E3) + botones de accion (comprar/decidir). Modelo:
```kotlin
@Serializable data class VoiTrialRecord(
  val schemaVersion: Int = DecisionesSchema.CURRENT,
  val seed: Long, val evidencesPurchased: Int, val optimalEvidences: Int,
  val finalChoice: Int, val confidence: Double, val correct: Boolean,
  val netPayoff: Double, val finalBur: Double, val totalLatencyMs: Long)
```
VOI/optimo determinista en commonMain (autoridad). `SensorEvidence` ya existe. Eventos: `evidence_purchased`, `voi_decided`, `over_collection_nudge`, `voi_deload`. Motores: `BayesEngine.calculatePosterior(baseRate, evidence)`/`evaluateBayesianUpdate` (autoridad), `ScoringEquations.calculateCAG/calculateBUR/calculateFRS/calculateAGRS`, `DifficultyAdapter`, `CoachEngine` (paralisis si over-collection + inactivity; impulsivo si sub-collection). Persistencia summaries. Registry `[E8_DECISION_UNDER_AMBIGUITY]`.

### Riesgos
El "optimo de informacion" (VOI) es no trivial de computar correctamente (depende de payoff y coste); bug -> feedback falso. Puede medir aversion al riesgo (rasgo) mas que habilidad. Sobre-recoleccion y cautela legitima son dificiles de separar.

### Revision critica
Revisor: (1) **El VOI optimo debe ser matematicamente correcto y testeado** (incluyendo payoffAsymmetry) o el "stopping error" es ruido -> derivar el umbral de un calculo VOI explicito, no de una heuristica. (2) **No confundir aversion al riesgo con error**: si el payoff es simetrico, sobre-recolectar un poco es racionalmente defendible -> la banda de "optimo +-1" debe ser tolerante y el copy no debe culpar la cautela. (3) BUR del posterior final colapsa a 1.0 si prior=posterior (pocas evidencias) -> excluir esos del BUR. (4) Politica degenerada (siempre max/nunca) debe detectarse o el ejercicio no mide adaptacion. (5) Riesgo de solapamiento conceptual con E1 (Bayes) -> diferenciar: E1 mide la actualizacion, E8 mide CUANDO parar de recolectar (control de la busqueda). Corregir 1-5.

### Veredicto
Aprobado con cambios: cierra un hueco real (valor de la informacion / parada optima) y es distinto de E1, siempre que el VOI optimo sea correcto y la cautela no se penalice como error.

---



# 3.7 Emociones (9 ejercicios)

# MindBoost Emociones — Catalogo de 8 ejercicios de calidad suprema

> Fronteras de seguridad vinculantes: entrenamos un **perfil cognitivo funcional**, no diagnostico, no CI, no terapia. Toda salida sin soporte estadistico se reporta `null` (em-dash), nunca `0`; todo score arranca `preliminary = true`. La induccion es **leve-only** con techo duro `InductionIntensity.OPERATIONAL_CEILING = 3`: el `DefaultEmotionAdaptiveEngine` jamas auto-sube intensidad; solo sube **dificultad** sobre UN `EmotionAxis` por decision. Los `OverloadProxy` ARMAN un check-in, **nunca infieren estado emocional**. El `EmotionSessionAssembler` es la unica autoridad de clasificacion; los contadores en vivo del holder son display-only. Toda copy pasa por `EmotionTerminologyScanner` (whole-word, accent- y hyphen-insensitive).

Los 8 ejercicios son mecanicamente distintos (no variaciones del mismo loop). Mapean sobre los simbolos reales del repo: `EmotionTrialGenerator`, `EmotionStimulusPayload` (`AffectiveGoNoGo`/`TwoAltPerceptual`/`Classification`), `EmotionResponseKey`, `EmotionMetricKey`, `EmotionAxis`, `DefaultEmotionMetricsEngine`, `DefaultEmotionAdaptiveEngine`, `EmotionAdaptiveInput/Decision`, `AdaptiveZone`, `EmotionCapacity`, `TransferLabel`, renderers `EmotionGoNoGoField`/`EmotionTwoAltField`/`EmotionFourCategoryField`.

---

## EM-A · Freno con Senuelo Afectivo (Stop-on-Affect)
### Modulo
Principal: Emociones (capacidad `PAUSE_BEFORE_ACTION` / control inhibitorio bajo carga afectiva leve). Secundarios: Enfoque (inhibicion go/no-go), Velocidad (timebase de alta resolucion para la SSD), Profile (contribucion coarse one-way).

### Objetivo cognitivo
Entrenar la **inhibicion de una respuesta ya iniciada** (parada reactiva) cuando aparece una senal de stop, con un distractor afectivo congruente/incongruente que aumenta la carga sin inducir estado. Aisla control inhibitorio del mero tiempo de reaccion via SSRT estimado por escalera SSD.

### Hipotesis de transferencia
SI plausible: near transfer a tareas go/no-go y stop-signal del propio paradigma (mejora de SSRT contra baseline personal). NO afirmar: reduccion de impulsividad "en la vida real", control de impulsos clinicos, ni far transfer a decisiones financieras/sociales. `TransferLabel.PLAUSIBLE` para near; el copy diferido nunca afirma control emocional general.

### Loop principal
1. Estimulo: marco GO (abierto) aparece; el usuario debe responder rapido (PRIMARY). 2. Decision: en una fraccion de trials, tras una **SSD** (stop-signal delay) el marco cambia a senal de STOP -> debe RETENER. Un `affectiveWord` mild se muestra como distractor (congruente/incongruente con el frame). 3. Feedback: inmediato por trial (acierto-go / inhibicion correcta / falsa alarma / omision) con color+forma. 4. Adaptacion: la SSD sube/baja por escalera 1-up/1-down (online) para fijar ~50% de inhibicion exitosa; la dificultad afectiva sube otro eje. 5. Siguiente trial.

### Reglas
GO = responder PRIMARY dentro de la ventana. STOP = NO responder tras la senal. La SSD es **online** (depende de la respuesta previa): inhibicion exitosa -> SSD +step (mas dificil); fallida -> SSD -step. El `affectiveWord` jamas determina la respuesta (honestidad: la regla es solo frame GO->STOP). ~30% de trials son stop.

### Inputs
Tap unico (PRIMARY) sobre zona grande para GO; **ausencia de tap** = inhibicion. Sin swipe ni teclado. Exit siempre visible.

### Variables de dificultad (rangos)
1. `EmotionAxis.INHIBITORY_DEMAND` (proporcion de stop-trials, 0.20–0.40). 2. SSD base online (120–450 ms, escalera). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana de respuesta GO, 1100–650 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (ratio incongruente, 0.0–0.6). 5. ISI jitter (300–900 ms, anti-ritmo). 6. `EmotionAxis.TIME_ON_TASK` (trials/bloque, 24–40). 7. Intensidad afectiva del banco (1–3, techo duro 3).

### Algoritmo adaptativo
Escalera SSD 1-up/1-down online por-trial dentro del bloque (converge a ~50% inhibicion). Entre bloques, `DefaultEmotionAdaptiveEngine.decide(EmotionAdaptiveInput)` mueve UN eje: prioridad `safetyStop -> reduceIntensity -> checkIn -> cooldown -> stepDown -> hold -> stepUp`. Step-up gated: `!escalationLocked && consecutiveErrors==0 && selfReport.intensity<=2 && zone mastery (COMMISSION)`. Plateau: ausencia de mejora de SSRT en N bloques -> switch de eje (de `TEMPORAL_PRESSURE` a `AFFECTIVE_INTERFERENCE`). Baseline personal ancla la SSD inicial.

### Metricas primarias
`COMMISSION_RATE` (falsa-alarma en stop = falló inhibicion), SSRT estimado (derivado de SSD media + RT-go median; expuesto como `IMPULSIVITY` index gated `confident=n>=minN`), `PAUSE_BEFORE_ACTION_RATE`.

### Metricas secundarias
`RT_MEDIAN` (solo HIT-go, post-MAD), `RT_CV`, `OMISSIONS` (lapsos go), `AFFECTIVE_INTERFERENCE_COST` (1.0 - cost congruente vs incongruente, gated MIN_CELL=5), `ANTICIPATIONS` (RT<350 crudos).

### Senales de fatiga o mala calidad
`OverloadProxy.SUSTAINED_SLOWING` (deriva RT-go vs baseline rolling intra-sesion), `ERROR_STREAK`, `EXTREME_RT_VARIABILITY`, RT-go median creciente con SSD estancada (escalera no converge -> bloque contaminado). `n<minN` en stop-trials -> SSRT `unavailable`.

### Scoring sugerido
`inhibitionScore = w1*(1 - COMMISSION_RATE) + w2*(1 - normSSRT) + w3*goAccuracy`, con `normSSRT = clamp((SSRT - SSRT_floor)/(SSRT_ceil - SSRT_floor),0,1)`. Barra `null` si `!confident`. `preliminary=true`. Pesos `w1=0.5,w2=0.35,w3=0.15`.

### Antitrampa / calidad de datos
RT<`IMPULSIVE_FLOOR_MS`(350) = anticipacion (no cuenta como HIT). Estrategia de "no responder nunca para no fallar stop" se detecta: `goAccuracy` cae -> `OMISSIONS` alto invalida el bloque. SSD online impide farming de la ventana. Latencias no-monotonas rechazadas.

### Variantes
1. **Freno visual** (frame abierto/cerrado, base). 2. **Freno por cambio de color** (GO verde -> STOP rojo+forma, daltonico-safe). 3. **Freno selectivo** (responder solo a frames de un color; STOP en el otro — anade discriminacion). Las tres reusan el mismo SSD-tracker.

### UX
Pantalla: zona tap grande central (`MinTouchTarget` >= 48dp via token), cue de regla persistente arriba, `affectiveWord` como subtitulo neutro. Feedback inmediato: borde verde/rojo + glifo (check/stop) ~200 ms. Stop = overlay rojo con forma de mano (no solo hue). Microanimacion funcional: el frame "se cierra" al disparar STOP (no decorativa). Duracion ideal 3–4 min/bloque, 2 bloques.

### Implementacion KMP
Reusa **renderer go/no-go** (`EmotionGoNoGoField`) con extension de dos fases (GO -> STOP, patron del E4 Freno de focus). Genera `EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, affectiveWord, congruent)` ampliado con un `stopSignalDelayMs` resuelto online por el holder y registrado en `RecordedEmotionTrial`. `EmotionTrialGenerator.generate(difficulty, spec, seed)` deterministico (esquema + tracker SSD; la SSD efectiva la fija el runtime). Clasificacion: el `EmotionTrialClassifier`/`EmotionSessionAssembler` necesita una rama `classifyStop` (HIT-go / CORRECT_REJECTION / FALSE_ALARM / OMISSION). Metricas via `DefaultEmotionMetricsEngine.goNoGoMetrics` extendida con SSRT. Adaptacion: `DefaultEmotionAdaptiveEngine` (zona COMMISSION). Persistencia: DTO `@Serializable`+`schemaVersion` -> `SecureKeyValueStore` (namespace Emotions, solo summary). Registro de eventos: `trial.go`, `trial.stop_success`, `trial.stop_fail`, `ssd.update`, `proxy.armed`, `checkin.shown`.

```kotlin
data class StopOnAffectTrial(val ssdMs: Long, val wasStop: Boolean, val frameOpen: Boolean,
    val congruent: Boolean, val responded: Boolean, val latencyMs: Long?)
```

### Riesgos
SSRT es ESTIMADO (no medido directo): si la escalera no converge, no reportar. La estrategia de omision masiva contamina; gate de `goAccuracy` minimo obligatorio. Riesgo de medir solo velocidad si `INHIBITORY_DEMAND` muy bajo.

### Revision critica
El Revisor exige: (a) SSRT solo se expone si la escalera convergio (rango SSD estable >= K trials) y `goAccuracy >= 0.6`, si no `IMPULSIVITY.unavailable`; (b) prohibir el "no-respondas-nunca" con un suelo de go-accuracy que invalide el bloque, no que premie; (c) el `affectiveWord` debe estar escaneado y dentro del techo leve, y un test de paridad debe probar que NO correlaciona con la respuesta correcta (anti render deshonesto); (d) requiere rama `classifyStop` en el assembler (deuda real, no opcional). Sin (a)–(d), es un go/no-go disfrazado.

### Veredicto
Aprobado con cambios — solido y medible, pero exige la rama `classifyStop` en el assembler y el doble gate (convergencia SSD + suelo go-accuracy) antes de exponer SSRT.

---

## EM-B · Reentrada tras Error (Post-Error Re-engagement)
### Modulo
Principal: Emociones (`POST_ERROR_RECOVERY`). Secundarios: Enfoque (control atencional post-error), Profile.

### Objetivo cognitivo
Entrenar la **recuperacion funcional tras un error**: tras un fallo, mantener accuracy sin sobre-frenar (post-error slowing patologico) ni impulsividad compensatoria. Mide el composite condicional ya codificado en `postErrorRecovery` (EM2): slowing solo penaliza si la accuracy NO se recupero.

### Hipotesis de transferencia
SI plausible: near transfer a estabilidad de rendimiento tras error en tareas de eleccion. NO afirmar: resiliencia emocional, tolerancia a la frustracion "vital", ni mejora de autoestima. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: discriminacion perceptual de 2 alternativas (LEFT/RIGHT); algunos trials son `harder` (near-threshold, error-likely plantado). 2. Decision: elegir lado. 3. Feedback: inmediato; tras un error, el siguiente trial se marca como "ventana de reentrada". 4. Adaptacion: ajusta `discriminability` y densidad de trials `harder`. 5. Siguiente trial.

### Reglas
Correcto = lado correcto dentro de ventana. Los trials `harder` garantizan secuencias post-error medibles **sin induccion injusta** (la dificultad es perceptual, no afectiva). Se requieren `MIN_POST_ERROR=3` pares para reportar. Se mide el bloque post-error (1–3 trials tras cada fallo).

### Inputs
Tap LEFT/RIGHT en mitades tactiles de pantalla completa. Sin teclado.

### Variables de dificultad (rangos)
1. `EmotionAxis.POST_ERROR_LOAD` (densidad de `harder`, 0.15–0.45). 2. `discriminability` base (0.85–0.45). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1400–800 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor leve, 0.0–0.5). 5. ISI jitter (300–800 ms). 6. `EmotionAxis.TIME_ON_TASK` (28–44 trials).

### Algoritmo adaptativo
Zona `GENERAL` (~0.80–0.85). `decide`: si `consecutiveErrors>0` -> nunca step-up (gate). Step-down si accuracy < banda o `POST_ERROR_ACCURACY_DROP` proxy. Plateau (sin mejora de `POST_ERROR_RECOVERY`) -> switch a `POST_ERROR_LOAD`. Baseline ancla `discriminability` inicial.

### Metricas primarias
`POST_ERROR_RECOVERY` (composite: slowing penalizado SOLO si `!accRecovered`, `accRecovered = postErrAcc >= baseAcc - ACC_TOL`), `POST_ERROR_ACCURACY`.

### Metricas secundarias
`POST_ERROR_SLOWING` (RT post-error vs base; descriptivo), `IMPULSIVITY` post-error (RT<`POST_ERROR_IMPULSE_MS`=250), `ACCURACY`, `RT_MEDIAN`, `RT_CV`.

### Senales de fatiga o mala calidad
`POST_ERROR_ACCURACY_DROP`, `POST_ERROR_IMPULSIVITY` (proxies que ARMAN check-in). `n<MIN_POST_ERROR` -> `POST_ERROR_RECOVERY.unavailable`. Deriva global de accuracy = fatiga.

### Scoring sugerido
`recoveryScore = clamp(1 - penalty, 0, 1)` donde `penalty = (accRecovered ? impulsePenalty : slowingPenalty + accGapPenalty)`; `accGap = max(0, baseAcc - postErrAcc)`. `null` si no confident. `preliminary=true`.

### Antitrampa / calidad de datos
Trial `harder` plantado deterministicamente (seed) impide al usuario predecirlo. RT<250 post-error = impulsividad (penaliza, no premia). Se excluyen del computo los pares donde el "error" fue una anticipacion (no es error genuino de discriminacion).

### Variantes
1. **Reentrada perceptual** (orientacion/gabor-like, base). 2. **Reentrada por conteo** (mas/menos elementos, 2-alt). 3. **Reentrada con cue de regla** (la regla LEFT/RIGHT cambia cada N trials -> error de regla + recuperacion). Reusan renderer 2-alt.

### UX
Mitades LEFT/RIGHT full-screen, estimulo central. Feedback de error: pulso neutro (NO culpa) + frase de encuadre "sigue, contra tu baseline". Sin penalizacion visual agresiva. Microanimacion: transicion suave al siguiente (anti-rumiacion). Duracion 3–4 min/bloque.

### Implementacion KMP
Reusa **EM2RecuperacionGenerator** (ya wired) + **renderer 2-alt** (`EmotionTwoAltField`). `EmotionStimulusPayload.TwoAltPerceptual(correct, discriminability, harder)` ya existe. Clasificacion: `EmotionTrialClassifier.classify` (directional). Metricas: `DefaultEmotionMetricsEngine.choiceMetrics` + `postErrorRecovery`. Adaptacion: `DefaultEmotionAdaptiveEngine`. Persistencia summary via `SecureKeyValueStore`. Eventos: `trial.choice`, `trial.error`, `post_error.window`, `post_error.recovered`, `proxy.armed`. **No requiere renderer nuevo** (es un redisenio premium de EM2 con metrica composite ya codificada).

### Riesgos
Si los trials `harder` son demasiado frecuentes, frustracion -> arma check-in y deload. Distinguir error de regla de error perceptual exige etiquetar la causa en el trial.

### Revision critica
El Revisor exige: (a) excluir anticipaciones del conjunto de "errores" que abren ventana post-error (un error genuino requiere respuesta-en-ventana incorrecta); (b) `ACC_TOL` y `POST_ERROR_IMPULSE_MS` nombrados, no magicos; (c) probar que `harder` plantado garantiza `>=MIN_POST_ERROR` pares en el peor seed, si no el bloque no puede reportar y debe avisarlo; (d) la frase de encuadre debe pasar el scanner (sin culpa, sin claim). Cumplido en gran parte por el codigo EM2 existente; (a) y (c) son el riesgo real.

### Veredicto
Aprobado con cambios — la metrica composite ya es honesta; falta blindar la definicion de "error genuino" (excluir anticipaciones) y garantizar suficiencia post-error por seed.

---

## EM-C · Separador Estado/Pensamiento/Impulso/Hecho (Granularity Quad)
### Modulo
Principal: Emociones (`EMOTION_THOUGHT_SEPARATION`, `GRANULARITY`). Secundarios: Lenguaje (clasificacion semantica neutra), Profile.

### Objetivo cognitivo
Entrenar la **separacion metacognitiva** de cuatro registros de una frase neutra: estado interno, pensamiento, impulso, hecho. Mide granularidad (categorias distintas correctas / 4) y exactitud ajustada por azar.

### Hipotesis de transferencia
SI plausible (con cautela): near transfer a etiquetado de registros en frases del mismo banco. NO afirmar: insight terapeutico, regulacion emocional clinica, ni que "separar pensamientos cure ansiedad". `TransferLabel.NOT_DEMONSTRATED` (holdout authored pero transferencia empirica pendiente).

### Loop principal
1. Estimulo: frase neutra cotidiana (`Classification.phrase`). 2. Decision: clasificar en 1 de 4 categorias fijas (`CATEGORY_A..D` mapeadas a quadrant fijo toda la sesion). 3. Feedback: correcto/incorrecto + la categoria correcta. 4. Adaptacion: sube `CATEGORY_AMBIGUITY` (frases mas ambiguas). 5. Siguiente trial.

### Reglas
4 botones fijos en quadrantes estables (mapeo quadrant->categoria constante toda la sesion). El holdout (`isEval`) usa items disjuntos del entrenamiento. Correcto = categoria == `truth`. Exactitud ajustada por azar: `((etAcc - 0.25)/(1-0.25))`.

### Inputs
Seleccion de 4 botones (no LEFT/RIGHT). Tap unico por trial.

### Variables de dificultad (rangos)
1. `EmotionAxis.CATEGORY_AMBIGUITY` (proporcion de frases ambiguas, 0.1–0.6). 2. Longitud de frase (4–14 palabras). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 6000–2500 ms). 4. Densidad de pares cuasi-sinonimos (0.0–0.4). 5. `EmotionAxis.TIME_ON_TASK` (16–28 trials). 6. Ratio eval/holdout (0.2–0.3 fijo).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` sube `CATEGORY_AMBIGUITY` solo con mastery; step-down si `CATEGORY_ACCURACY` < banda. Plateau -> mas pares cuasi-sinonimos. Baseline ancla ambiguedad inicial.

### Metricas primarias
`EMOTION_THOUGHT_SEPARATION` (accuracy ajustada por azar), `CATEGORY_ACCURACY`, `GRANULARITY_INDEX` (categorias distintas correctas / 4).

### Metricas secundarias
`RT_MEDIAN` (solo CORRECT, post-MAD), `EmotionConfusionMatrix` (4x4, solo para esta capacity), confusiones por par (descriptivo, no clinico).

### Senales de fatiga o mala calidad
Colapso a una sola categoria (boton-spam) -> `GRANULARITY_INDEX` bajo + matriz degenerada. RT muy bajo uniforme = respuesta sin lectura. `n<minN` por categoria -> celda `unavailable`.

### Scoring sugerido
`separationScore = 0.6*adjAcc + 0.4*GRANULARITY_INDEX`, `adjAcc = (etAcc-0.25)/0.75 clamp 0..1`. Reporta tambien matriz de confusion como senal descriptiva. `null` si no confident.

### Antitrampa / calidad de datos
Mapeo quadrant->categoria FIJO impide aprender posicion en vez de concepto (pero rota entre sesiones). Boton-spam detectado por entropia de respuestas baja. Holdout disjunto evita inflar por memorizacion de items vistos. RT-floor por lectura minima (no aceptar < tiempo de lectura plausible por longitud).

### Variantes
1. **Separador 4-cat** (base). 2. **Separador binario progresivo** (estado-vs-hecho, luego pensamiento-vs-impulso — reduce carga inicial). 3. **Separador con frase-fuente** (clasificar Y senalar la palabra-clave que decide — anade evidencia, requiere drag/seleccion de token). V1/V2 reusan renderer 4-cat; V3 necesita renderer de seleccion de token.

### UX
4 botones en grid 2x2 estable (`MinTouchTarget`), frase central legible <1s. Feedback: resalta boton correcto. Sin animacion decorativa. Duracion 3–5 min (menos trials, mas lectura). Texto escalable.

### Implementacion KMP
Reusa **EM3SeparadorGenerator** (wired) + **renderer 4-cat** (`EmotionFourCategoryField`). `EmotionStimulusPayload.Classification(phrase, truth, isEval)` ya existe; `RecordedEmotionTrial.chosenCategory` transporta la eleccion. Clasificacion + `EmotionConfusionMatrix` en `EmotionSessionAssembler` (solo `EMOTION_THOUGHT_SEPARATION`). Metricas: `DefaultEmotionMetricsEngine.classificationMetrics`. Banco: `EmotionContent.SEPARATOR_ITEMS` (40, 7 train + 3 eval/categoria, disjuntos). **No requiere renderer nuevo** salvo V3 (token-select). Eventos: `trial.classify`, `category.confusion`, `holdout.item`.

### Riesgos
Dependencia de idioma/cultura en las frases -> requiere banco localizado y re-escaneado por idioma. Ambiguedad intrinseca de "pensamiento vs impulso" puede generar items sin verdad clara -> revision humana del banco.

### Revision critica
El Revisor exige: (a) cada item del banco debe tener verdad inter-rater alta (>= 80% acuerdo) o se descarta — sin esto la "verdad" es arbitraria; (b) el RT-floor por lectura debe escalar con longitud de frase, no ser constante; (c) `GRANULARITY_INDEX`=categorias/4 es fragil con pocos trials — debe gated por `n` por categoria; (d) NOT_DEMONSTRATED es honesto: prohibido cualquier copy que sugiera beneficio emocional. (a) es bloqueante: sin acuerdo inter-rater, mide ruido.

### Veredicto
Aprobado con cambios — mecanica y metrica solidas, pero la calidad del banco (acuerdo inter-rater + localizacion) es condicion previa a exponerlo.

---

## EM-D · Espera Calibrada (Tolerance Hold)
### Modulo
Principal: Emociones (`FRUSTRATION_TOLERANCE`, capacidad `FRUSTRATION_TOLERANCE`). Secundarios: Enfoque (espera sostenida), Profile.

### Objetivo cognitivo
Entrenar la **persistencia segura ante demora**: mantener una accion sostenida (o esperar una ventana variable) sin abandonar antes de tiempo ni actuar impulsivamente, midiendo tolerancia a la demora **sin** induccion de frustracion (la demora es la unica fuente de carga, dentro del techo leve).

### Hipotesis de transferencia
SI plausible: near transfer a tareas de espera/demora del paradigma. NO afirmar: tolerancia a la frustracion "en la vida", autocontrol global, ni paciencia como rasgo. `TransferLabel.PLACEBO_RISK` (alto riesgo de claim inflado -> copy ultra-conservador).

### Loop principal
1. Estimulo: aparece un "objetivo madurando" (barra que se llena en tiempo variable). 2. Decision: tap SOLO cuando este maduro (ventana al final); tap temprano = impulsivo, no-tap = abandono. 3. Feedback: maduro/temprano/abandono. 4. Adaptacion: alarga el tiempo de maduracion. 5. Siguiente trial.

### Reglas
La ventana de "maduro" es variable y NO senalizada por countdown explicito (se infiere del llenado). Tap antes de la ventana = `FALSE_ALARM` (impulsivo). No-tap hasta el fin = `ABANDONMENT`/`OMISSION`. Tap en ventana = HIT. El abandono SIEMPRE permitido (exit visible) y nunca penalizado con culpa.

### Inputs
Tap unico temporizado (PRIMARY). Opcional hold-and-release como variante. Exit/abandono siempre disponible.

### Variables de dificultad (rangos)
1. `EmotionAxis.TIME_ON_TASK` (tiempo de maduracion, 1500–9000 ms). 2. Varianza del tiempo de maduracion (CV 0.1–0.5, incertidumbre). 3. Anchura de la ventana "maduro" (900–300 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor leve durante la espera, 0.0–0.4). 5. Numero de trials (12–22; pocos por la duracion). 6. Densidad de "objetivos que nunca maduran" (catch trials, 0.0–0.15).

### Algoritmo adaptativo
Zona `TOLERANCE` (~0.70–0.80, safety estricta). `decide` sube tiempo de maduracion solo con mastery; `REPEATED_EXIT_ATTEMPTS`/`REPEATED_PAUSE` -> check-in -> reduce. NUNCA sube con `selfReport.intensity>2`. Plateau -> sube varianza (incertidumbre), no el tiempo medio.

### Metricas primarias
`SAFE_PERSISTENCE_INDEX` (HITs maduros / oportunidades, penalizando impulsivos pero NO abandonos seguros), `FRUSTRATION_TOLERANCE_INDEX` (tiempo medio esperado en HITs vs baseline).

### Metricas secundarias
`IMPULSIVITY` (tap temprano), `ABANDONMENT` (count, descriptivo, NO penaliza score de tolerancia — es senal de seguridad), `RT_CV` de la ventana de respuesta.

### Senales de fatiga o mala calidad
`REPEATED_EXIT_ATTEMPTS`, `SUSTAINED_SLOWING`, abandono creciente -> ARMAN check-in (no infieren frustracion). Catch-trials respondidos = tap-spam (invalida). `n<minN` -> indices `unavailable`.

### Scoring sugerido
`toleranceScore = matureHits / (matureHits + earlyTaps)` (abandonos excluidos del denominador — un abandono seguro NO es fracaso). `persistenceTime = median(waitTime | HIT)` normalizado vs baseline. `null` si denominador < minN. `preliminary=true`.

### Antitrampa / calidad de datos
Catch-trials (nunca maduran) detectan tap-spam: responder a uno = penalizado/invalida bloque. Tiempo de maduracion con varianza impide aprender un timer fijo. Hold-variant requiere presion continua (anti auto-clicker).

### Variantes
1. **Barra madurando** (base). 2. **Hold sostenido** (mantener presion hasta la senal, soltar en ventana). 3. **Espera con eleccion** (esperar para mayor recompensa-puntos vs tap ya para menos — coste/beneficio, sin gambling lexicon). Las tres usan timebase de alta resolucion.

### UX
Barra/objeto central que se llena; SIN countdown numerico (evita reducir a lectura de timer). Estados maduro/temprano/abandono por color+forma. Exit grande siempre visible. Microanimacion: llenado suave (funcional, es el estimulo). Duracion 3–5 min. Mensajes sin culpa: abandonar es valido.

### Implementacion KMP
**Requiere renderer nuevo** (timed single-tap con barra de maduracion + catch). Modela sobre `EmotionStimulusPayload.AffectiveGoNoGo`-like ampliado o un nuevo payload `ToleranceHold(maturationMs, windowMs, isCatch)`. Generator deterministico (tiempos seeded). Clasificacion: nueva rama `classifyTolerance` (HIT/FALSE_ALARM/ABANDONMENT) en `EmotionSessionAssembler`. Metricas nuevas en `DefaultEmotionMetricsEngine` (`SAFE_PERSISTENCE_INDEX`, `FRUSTRATION_TOLERANCE_INDEX`, hoy catalogadas no producidas). Adaptacion: `DefaultEmotionAdaptiveEngine` zona TOLERANCE. Eventos: `trial.mature_hit`, `trial.early_tap`, `trial.abandon`, `catch.spam`, `proxy.armed`. Persistencia summary.

### Riesgos
Riesgo alto de cruzar de "espera funcional" a induccion de frustracion -> por eso techo leve y zona TOLERANCE con safety estricta. Aburrimiento con tiempos largos -> limitar maduracion <= 9s y pocos trials. `PLACEBO_RISK`: facil de sobre-vender.

### Revision critica
El Revisor ataca: (a) esto roza la induccion emocional — debe probarse via scanner + diseno que la UNICA carga es demora temporal, sin contenido afectivo escalado; (b) el abandono NUNCA debe penalizar el score ni mostrar culpa (codificar `ABANDONMENT` como senal de seguridad, no de fracaso); (c) catch-trials obligatorios o el tap-spam infla todo; (d) sin baseline de tiempo de espera, `FRUSTRATION_TOLERANCE_INDEX` no es interpretable -> NOT computable hasta baseline. Exige las 4 correcciones; (a) y (b) son fronteras de seguridad no negociables.

### Veredicto
Aprobado con cambios — concepto valioso pero el de mayor riesgo de seguridad: solo procede con zona TOLERANCE estricta, abandono no-penalizado, catch-trials y baseline previo; copy ultra-conservador por `PLACEBO_RISK`.

---

## EM-E · Calibrador Confianza/Acierto (Metacognitive Calibration)
### Modulo
Principal: Emociones (`CALIBRATION_ACCURACY`, capacidad `CALIBRATION_ACCURACY`). Secundarios: Profile, todos los modulos de eleccion (capa de confianza reusable).

### Objetivo cognitivo
Entrenar el **alineamiento confianza-acierto** (metacognicion): tras decidir, reportar confianza; bien calibrado = alta confianza en aciertos, baja en errores. Mide `CALIBRATION_ERROR` (hoy catalogada no producida).

### Hipotesis de transferencia
SI plausible: near transfer a calibracion en el mismo paradigma de decision. NO afirmar: mejor toma de decisiones vital, autoconocimiento, ni reduccion de sesgos clinicos. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: decision perceptual 2-alt (LEFT/RIGHT, reusa EM-B). 2. Decision: elegir lado + DESPUES reportar confianza (escala 2–3 niveles: bajo/medio/alto). 3. Feedback: correcto/incorrecto + (diferido por bloque) curva de calibracion. 4. Adaptacion: ajusta dificultad para mantener accuracy en banda (~0.75) que maximiza varianza de confianza. 5. Siguiente trial.

### Reglas
Confianza se reporta SOLO tras la respuesta (no antes, para no sesgar). 3 niveles de confianza fijos. La dificultad se mantiene en banda intermedia a proposito (accuracy ~0.7–0.8) para que haya aciertos Y errores que calibrar. Feedback de calibracion es DIFERIDO (por bloque), no por trial (evita gaming).

### Inputs
Tap LEFT/RIGHT (decision) + seleccion de 3 botones de confianza (bajo/medio/alto). Dos taps por trial.

### Variables de dificultad (rangos)
1. `discriminability` (mantenida en banda, 0.7–0.55). 2. `EmotionAxis.TEMPORAL_PRESSURE` (ventana decision, 1500–900 ms). 3. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor, 0.0–0.4). 4. Granularidad de confianza (2 o 3 niveles). 5. `EmotionAxis.TIME_ON_TASK` (24–40 trials; calibracion necesita n alto). 6. Ratio de trials ambiguos (para poblar confianza media, 0.2–0.4).

### Algoritmo adaptativo
Zona `GENERAL` pero objetivo INVERSO: mantener accuracy en banda media (no maximizar). `decide` ajusta `discriminability` para que accuracy ~0.75; NO sube indefinidamente. Plateau de `CALIBRATION_ERROR` -> mas trials ambiguos. Baseline ancla discriminability.

### Metricas primarias
`CALIBRATION_ERROR` (|confianza media reportada - accuracy real| por nivel, agregado tipo ECE — Expected Calibration Error), `AFFECTIVE_DECISION_QUALITY` (si se anade coste/beneficio).

### Metricas secundarias
`ACCURACY`, `RT_MEDIAN`, resolution (separacion de accuracy entre niveles de confianza), over/under-confidence bias (descriptivo, no clinico).

### Senales de fatiga o mala calidad
Confianza colapsada a un solo nivel (siempre "alto") = no calibracion -> `CALIBRATION_ERROR` no interpretable, arma check-in. RT de confianza ~0 = reporte sin reflexion. `n<minN` por nivel -> `unavailable`.

### Scoring sugerido
`calibrationScore = 1 - ECE`, `ECE = sum_k (n_k/N) * |conf_k - acc_k|` sobre niveles k. `resolution = var(acc_k)`. Reporta ambos; `calibrationScore null` si algun nivel con `n_k<minN`. `preliminary=true`.

### Antitrampa / calidad de datos
Confianza POST-respuesta evita ajustar la decision a la confianza. Feedback de calibracion diferido (no por trial) evita aprender a reportar la confianza "correcta" por ensayo-error. Colapso de confianza detectado por entropia. Dos taps obligan a no spamear un solo boton.

### Variantes
1. **Calibracion perceptual** (base, 2-alt + confianza). 2. **Calibracion con apuesta de puntos** (asignar puntos segun confianza — scoring proper-score tipo Brier, sin lexicon de gambling). 3. **Calibracion 4-cat** (sobre EM-C: clasificar + confianza). V1/V2 reusan 2-alt + capa confianza; V3 sobre 4-cat.

### UX
Tras el tap de decision, aparece fila de 3 botones de confianza (bajo/medio/alto, iconos distintos). Feedback diferido: curva de calibracion honesta al final del bloque (diagonal = perfecto). Sin claim. Duracion 4–5 min. Texto escalable.

### Implementacion KMP
Reusa **EM2RecuperacionGenerator** + **renderer 2-alt** (`EmotionTwoAltField`) **mas una capa de confianza nueva** (3 botones). El `RecordedEmotionTrial` necesita un campo `reportedConfidence: Int?`. Metrica `CALIBRATION_ERROR` nueva en `DefaultEmotionMetricsEngine`. `SubjectiveState`/`SelfReport` ya existe para confianza subjetiva — reusar su patron. Clasificacion via `choiceMetrics` + agregacion ECE en el assembler. **Requiere renderer aumentado** (decision + confidence overlay), no totalmente nuevo. Eventos: `trial.choice`, `trial.confidence`, `calibration.curve`. Persistencia summary.

### Riesgos
Necesita n alto para ECE estable -> bloques mas largos (riesgo fatiga). Dos taps por trial alarga el loop (rompe "loop corto" si no se optimiza la UI de confianza). Sesgo de orden si confianza siempre en mismo lado.

### Revision critica
El Revisor exige: (a) ECE con binning honesto y gated por `n_k` minimo por nivel — un nivel con 2 trials no entra; (b) la capa de confianza no debe romper el loop corto: pre-render de botones, no pantalla nueva; (c) feedback diferido obligatorio (por trial = gaming); (d) prohibir interpretar over-confidence como rasgo — es descriptivo de la tarea. (a) es bloqueante: ECE con celdas vacias es ruido.

### Veredicto
Aprobado con cambios — metrica metacognitiva valiosa y reusable, pero exige gating estricto de ECE por nivel y una UI de confianza que no rompa el loop corto.

---

## EM-F · Etiquetado Rapido de Registro (Speeded Affective Granularity)
### Modulo
Principal: Emociones (`GRANULARITY`). Secundarios: Lenguaje, Velocidad (presion temporal). 

### Objetivo cognitivo
Entrenar la **granularidad bajo presion**: discriminar rapido entre registros afectivos neutros (p.ej. agradable/desagradable/neutro/mixto) de palabras/frases cotidianas, distinto de EM-C (4 registros logicos): aqui el eje es valencia/activacion en banco neutro, con presion temporal y midiendo velocidad de etiquetado sin perder exactitud.

### Hipotesis de transferencia
SI plausible: near transfer a velocidad de categorizacion afectiva del banco. NO afirmar: inteligencia emocional, empatia, ni reconocimiento de emociones ajenas. `TransferLabel.NOT_DEMONSTRATED`.

### Loop principal
1. Estimulo: palabra/frase neutra del banco. 2. Decision: clasificar valencia en 1 de 4 (CATEGORY_A..D = p.ej. agradable/desagradable/neutro/mixto). 3. Feedback: inmediato. 4. Adaptacion: acorta ventana (presion) y sube ambiguedad. 5. Siguiente trial.

### Reglas
4 categorias fijas (mapeo quadrant estable). La valencia "verdad" del banco esta pre-etiquetada con acuerdo inter-rater. Ventana corta fuerza etiquetado rapido. Correcto = categoria == verdad. Holdout disjunto.

### Inputs
Seleccion de 4 botones, tap unico rapido.

### Variables de dificultad (rangos)
1. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 3500–1200 ms). 2. `EmotionAxis.CATEGORY_AMBIGUITY` (items mixtos, 0.1–0.5). 3. Longitud (1–6 palabras). 4. Densidad de cuasi-vecinos de valencia (0.0–0.4). 5. `EmotionAxis.TIME_ON_TASK` (24–40 trials). 6. ISI jitter (300–700 ms).

### Algoritmo adaptativo
Zona `GENERAL` con IES como gobernante (velocidad-exactitud). `decide` baja ventana solo con mastery; step-down si IES empeora o accuracy < banda. Plateau -> ambiguedad. Baseline ancla ventana inicial.

### Metricas primarias
`GRANULARITY_INDEX` (categorias distintas correctas / 4), `CATEGORY_ACCURACY`, IES (`RT_MEDIAN_hits / accuracy`).

### Metricas secundarias
`RT_MEDIAN` (CORRECT, post-MAD), `RT_CV`, `ANTICIPATIONS`, matriz de confusion de valencia (descriptiva).

### Senales de fatiga o mala calidad
IES creciente sostenido = fatiga/velocidad-sin-control. Colapso a una categoria. RT-floor por lectura. `OverloadProxy.EXTREME_RT_VARIABILITY`. `n<minN` -> celdas `unavailable`.

### Scoring sugerido
`granularitySpeedScore = 0.5*adjAcc + 0.5*(1 - normIES)`, `normIES = clamp((IES - IES_floor)/(IES_ceil - IES_floor),0,1)`. `null` si no confident. `preliminary=true`.

### Antitrampa / calidad de datos
Anticipaciones (RT<350) excluidas de HIT. RT-floor por lectura segun longitud. Banco con verdad inter-rater (descarta items ambiguos sin acuerdo). Holdout disjunto. Mapeo quadrant rota entre sesiones (no posicion).

### Variantes
1. **Valencia 4-cat veloz** (base). 2. **Activacion 4-cat** (calma/tension/neutro/mixto en vez de valencia). 3. **Doble eje** (valencia + activacion en 2 taps secuenciales — anade carga). V1/V2 reusan 4-cat; V3 dos fases.

### UX
Grid 2x2 estable, estimulo central, ventana visible como aro decreciente (funcional). Feedback inmediato resaltando correcto. Duracion 3–4 min. Texto escalable.

### Implementacion KMP
Reusa **renderer 4-cat** (`EmotionFourCategoryField`) + patron de **EM3SeparadorGenerator**. `EmotionStimulusPayload.Classification(phrase, truth, isEval)` reusable (truth = valencia). Banco NUEVO en `EmotionContent` (valencia neutra, escaneado, inter-rater) — disjunto de `SEPARATOR_ITEMS`. Metricas: `classificationMetrics` + IES. Adaptacion: `DefaultEmotionAdaptiveEngine` (IES-driven). **No requiere renderer nuevo** (V1/V2). Eventos: `trial.classify`, `valence.confusion`, `holdout.item`. Persistencia summary.

### Riesgos
Solapa con EM-C salvo por eje (valencia vs registro logico) y presion temporal -> debe quedar claro que mide cosa distinta. Dependencia cultural de valencia -> banco localizado. Riesgo de "reconocer emociones" claim (prohibido).

### Revision critica
El Revisor ataca: (a) ?es realmente distinto de EM-C o es una variante trivial? Defensa: eje afectivo (valencia/activacion) + presion temporal + IES gobernante, no registro logico sin presion — aceptado SI el banco es disjunto y el eje es valencia, no registro; (b) "agradable/desagradable" roza claim afectivo -> banco debe ser neutro-cotidiano y escaneado, sin contenido intimo; (c) inter-rater obligatorio o la verdad de valencia es opinion; (d) RT-floor por lectura no constante. Exige (a)-(c); sin banco disjunto + inter-rater colapsaria a EM-C.

### Veredicto
Aprobado con cambios — distinto de EM-C solo si el eje es valencia/activacion con presion temporal e IES y el banco es disjunto con inter-rater; de lo contrario, fusionar con EM-C.

---

## EM-G · Estabilidad bajo Interferencia Afectiva (Affective Flanker/Stroop)
### Modulo
Principal: Emociones (`REGULATION_STABILITY`, `AFFECTIVE_INTERFERENCE_COST`). Secundarios: Enfoque (control de interferencia), Profile.

### Objetivo cognitivo
Entrenar el **control de interferencia afectiva**: responder a una dimension objetivo (p.ej. la direccion de una flecha / el color del marco) ignorando un distractor afectivo congruente/incongruente. Mide el coste de interferencia y la ESTABILIDAD (RT-CV) bajo carga afectiva — distinto del go/no-go (no es inhibir, es seleccionar bajo conflicto).

### Hipotesis de transferencia
SI plausible: near transfer al control de interferencia del paradigma. NO afirmar: regulacion emocional, estabilidad de animo, ni resistencia al estres. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: objetivo direccional (LEFT/RIGHT) con un distractor afectivo congruente/incongruente (la tonalidad del marco/palabra apunta a un lado o al contrario). 2. Decision: responder a la dimension objetivo, ignorar el distractor. 3. Feedback: inmediato. 4. Adaptacion: sube ratio incongruente y presion. 5. Siguiente trial.

### Reglas
La respuesta depende SOLO de la dimension objetivo (honestidad: el distractor NUNCA es la respuesta). Trials congruentes (distractor coincide) vs incongruentes (conflicto). El coste = RT/accuracy incongruente - congruente, solo con HIT-con-RT en ambos lados.

### Inputs
Tap LEFT/RIGHT en mitades tactiles.

### Variables de dificultad (rangos)
1. `EmotionAxis.AFFECTIVE_INTERFERENCE` (ratio incongruente, 0.2–0.6). 2. Saliencia del distractor (0.3–1.0). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1300–700 ms). 4. `EmotionAxis.INHIBITORY_DEMAND` (proporcion de no-go intercalados, 0.0–0.2 — opcional). 5. ISI jitter (300–800 ms). 6. `EmotionAxis.TIME_ON_TASK` (28–44 trials).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` sube `AFFECTIVE_INTERFERENCE` con mastery; step-down si accuracy < banda o `EXTREME_RT_VARIABILITY`. Plateau de `AFFECTIVE_INTERFERENCE_COST` -> sube saliencia. Gate de fatiga antes de step-up. Baseline ancla ratio inicial.

### Metricas primarias
`AFFECTIVE_INTERFERENCE_COST` (1.0 - cost, gated MIN_CELL=5 por celda congruente/incongruente), `REGULATION_STABILITY` (1 - normalized RT-CV en incongruentes).

### Metricas secundarias
`ACCURACY`, `RT_MEDIAN` (HIT, post-MAD), `RT_CV`, `COMMISSION_RATE` (si hay no-go), `OMISSIONS`.

### Senales de fatiga o mala calidad
RT-CV creciente en incongruentes = inestabilidad/fatiga. Celda congruente o incongruente con `n<MIN_CELL` -> coste `unavailable`. `SUSTAINED_SLOWING`. Deriva de accuracy.

### Scoring sugerido
`stabilityScore = w1*(1 - normCost) + w2*REGULATION_STABILITY + w3*accuracy`; `normCost = clamp(cost/COST_MAX,0,1)`; `cost = RT_incong - RT_cong` (solo si ambas celdas confident). `null` si alguna celda < MIN_CELL. `w1=0.4,w2=0.4,w3=0.2`. `preliminary=true`.

### Antitrampa / calidad de datos
Coste solo computa con HIT-con-RT en AMBOS lados (regla §2.1.4). Distractor verificado por test de no-correlacion con la respuesta (anti render deshonesto). Anticipaciones excluidas. Daltonico-safe (forma+posicion, no solo color).

### Variantes
1. **Flanker afectivo** (flechas con marco de tono, base). 2. **Stroop-afectivo neutro** (palabra cotidiana cuyo tono apunta a un lado, responder a otra dimension). 3. **Simon-afectivo** (posicion del estimulo conflictua con la respuesta). Las tres reusan 2-alt + medicion de congruencia (patron E5 de focus).

### UX
Mitades LEFT/RIGHT, estimulo central con distractor claramente secundario. Cue de regla persistente. Feedback inmediato. Daltonico-safe. Microanimacion: ninguna decorativa. Duracion 3–4 min.

### Implementacion KMP
Reusa **renderer 2-alt** (`EmotionTwoAltField`) + patron de congruencia del E5 (Stroop/Simon) de focus. `EmotionStimulusPayload.TwoAltPerceptual` extendido o nuevo `AffectiveConflict(target, distractorSide, congruent, salience)`. Clasificacion: `EmotionTrialClassifier` (directional) + agregacion de coste congruente/incongruente en `EmotionSessionAssembler`. Metricas: `DefaultEmotionMetricsEngine.choiceMetrics` + `AFFECTIVE_INTERFERENCE_COST` (ya parcialmente codificada) + `REGULATION_STABILITY` (catalogada no producida). Adaptacion: `DefaultEmotionAdaptiveEngine`. **Renderer reusable** (anadir capa de distractor afectivo). Eventos: `trial.choice`, `congruency.incongruent`, `cost.cell`. Persistencia summary.

### Riesgos
"Estabilidad regulatoria" roza claim emocional -> copy debe decir "estabilidad de respuesta bajo interferencia", no "regulacion emocional". Solapa con EM-B si no se enfatiza el conflicto (no el error).

### Revision critica
El Revisor exige: (a) coste `unavailable` si cualquier celda < MIN_CELL=5 — no rellenar con 0; (b) test de paridad que el distractor afectivo NO predice la respuesta correcta; (c) `REGULATION_STABILITY` debe llamarse "estabilidad de respuesta" en UI, jamas "regulacion emocional" (frontera de seguridad + scanner); (d) distinguir de EM-B: aqui se mide conflicto congruente/incongruente, no recuperacion post-error. (a) y (c) bloqueantes.

### Veredicto
Aprobado con cambios — paradigma de conflicto clasico y medible; exige gating MIN_CELL, prueba de distractor no-predictivo y copy que evite el claim "regulacion emocional".

---

## EM-H · Mantener-el-Plan bajo Distractor (Goal-Maintenance Sequence)
### Modulo
Principal: Emociones (`REGULATION_STABILITY` aplicada a persistencia de meta). Secundarios: Enfoque (memoria de regla activa), Profile.

### Objetivo condicional
Entrenar el **mantenimiento de una regla/meta activa a lo largo de una secuencia** pese a distractores afectivos leves e intrusiones, midiendo la estabilidad de aplicar la regla correcta trial-a-trial (no cambio de regla como E6: aqui la regla es FIJA y el reto es no derivar de ella bajo carga).

### Hipotesis de transferencia
SI plausible: near transfer a mantenimiento de regla en el paradigma. NO afirmar: perseverancia vital, disciplina, ni autorregulacion global. `TransferLabel.NOT_DEMONSTRATED`.

### Loop principal
1. Estimulo: secuencia de estimulos; una regla FIJA declarada al inicio del bloque (p.ej. "responde LEFT si el simbolo es circulo"). Distractores afectivos leves aparecen entre trials. 2. Decision: aplicar la regla a cada estimulo. 3. Feedback: inmediato; errores de "olvido de regla" (deriva) se distinguen de errores perceptuales. 4. Adaptacion: alarga la secuencia y sube distractores. 5. Siguiente trial.

### Reglas
La regla es FIJA todo el bloque (no switch). El reto es no olvidarla/derivar tras distractores. Correcto = aplicar la regla al estimulo. Se mide deriva de accuracy en funcion de la posicion-en-secuencia y de la proximidad a un distractor.

### Inputs
Tap LEFT/RIGHT (o 4-cat segun regla) en mitades tactiles. Sin teclado.

### Variables de dificultad (rangos)
1. Longitud de secuencia entre recordatorios de regla (`EmotionAxis.TIME_ON_TASK`, 8–24 trials sin recordatorio). 2. `EmotionAxis.AFFECTIVE_INTERFERENCE` (densidad de distractores, 0.1–0.5). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1400–800 ms). 4. Complejidad de la regla (1–2 condiciones). 5. Saliencia de distractor (0.3–1.0). 6. ISI jitter (300–800 ms).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` alarga la secuencia (mas distancia al recordatorio) solo con mastery; step-down si la deriva de accuracy crece. Plateau -> sube distractores. Gate de fatiga. Baseline ancla longitud inicial.

### Metricas primarias
`REGULATION_STABILITY` (1 - pendiente de deriva de accuracy en secuencia), `ACCURACY` global, `AFFECTIVE_INTERFERENCE_COST` (accuracy cerca vs lejos de distractor).

### Metricas secundarias
`RT_MEDIAN`, `RT_CV`, deriva intra-secuencia (slope), `POST_ERROR_RECOVERY` (tras error de regla), `OMISSIONS`.

### Senales de fatiga o mala calidad
Deriva de accuracy creciente = fatiga o perdida de regla -> `OverloadProxy.SUSTAINED_SLOWING`/`ERROR_STREAK` arman check-in. Accuracy random (~chance) = regla no retenida -> bloque invalido. `n<minN` -> `unavailable`.

### Scoring sugerido
`maintenanceScore = accuracy * (1 - normDriftSlope)`, `driftSlope = regress(accuracy ~ positionInSequence)` (negativa = deriva). `normDriftSlope = clamp(-driftSlope/SLOPE_MAX,0,1)`. `null` si secuencia < minN o accuracy ~ chance. `preliminary=true`.

### Antitrampa / calidad de datos
La regla se declara una vez; recordatorios espaciados impiden re-leerla cada trial. Accuracy ~ chance invalida (no retuvo la regla -> no mide mantenimiento). Distractor no-predictivo (test de paridad). Anticipaciones excluidas.

### Variantes
1. **Regla simple 2-alt** (1 condicion, base). 2. **Regla compuesta** (2 condiciones AND/OR). 3. **Regla 4-cat** (mantener mapeo de 4 categorias bajo distractor). V1/V2 reusan 2-alt; V3 reusa 4-cat.

### UX
Cue de regla persistente pero discreto (esquina), recordatorio explicito solo en intervalos. Estimulo central, distractores claramente secundarios. Feedback inmediato. Duracion 4–5 min (secuencias largas). Texto escalable, daltonico-safe.

### Implementacion KMP
Reusa **renderer 2-alt** (`EmotionTwoAltField`) y/o **4-cat** segun regla. Nuevo payload `GoalMaintenance(rule, target, distractorPresent, positionInSequence)` o extension de `TwoAltPerceptual`/`Classification` con `positionInSequence`. Generator deterministico (secuencia + posiciones de distractor seeded). Clasificacion: `EmotionTrialClassifier` (directional/category) + agregacion de deriva por posicion en `EmotionSessionAssembler`. Metricas: `DefaultEmotionMetricsEngine` con `REGULATION_STABILITY` nueva (slope de deriva). Adaptacion: `DefaultEmotionAdaptiveEngine`. **Renderer reusable** (la regla es overlay de cue, patron E6 sin el switch). Eventos: `trial.apply_rule`, `rule.reminder`, `drift.measured`, `distractor.near`. Persistencia summary.

### Riesgos
Solapa con EM-G (interferencia) y E6 (task-switch). Diferenciador: regla FIJA + medicion de DERIVA por posicion, no congruencia por trial ni switch. Si la secuencia es corta, no hay deriva que medir.

### Revision critica
El Revisor ataca: (a) ?como se distingue un error de regla (deriva) de un error perceptual? -> el trial debe etiquetar si la respuesta dada corresponde a OTRA regla plausible vs ruido; sin esto la "deriva" es inferida, no medida; (b) accuracy ~ chance debe invalidar (no retuvo la regla) — un usuario que nunca entendio la regla no esta "derivando"; (c) secuencias minimas para slope estable (>= K posiciones); (d) `REGULATION_STABILITY` en UI = "estabilidad de aplicacion de regla", no emocional. (a) es el reto duro: medir deriva de meta requiere distinguir tipo de error.

### Veredicto
Aprobado con cambios — concepto fuerte de mantenimiento de meta, pero solo medible si el trial distingue error-de-regla de error-perceptual y se invalida el desempeno ~ chance; sin eso, la "deriva" no es interpretable.

---

## Resumen de cobertura y deuda tecnica
- Renderers **reusados** (sin nuevo): EM-A (go/no-go + fase stop), EM-B (2-alt), EM-C (4-cat), EM-F (4-cat), EM-G (2-alt+distractor), EM-H (2-alt/4-cat+cue).
- Renderers **nuevos/aumentados**: EM-D (timed-hold con barra+catch, NUEVO), EM-E (2-alt + capa de confianza, AUMENTADO).
- Ramas de assembler nuevas requeridas: `classifyStop` (EM-A), `classifyTolerance` (EM-D), agregacion de confianza/ECE (EM-E), deriva-por-posicion (EM-H).
- Metricas hoy catalogadas-no-producidas que estos ejercicios activarian honestamente (gated `confident`): `SAFE_PERSISTENCE_INDEX`/`FRUSTRATION_TOLERANCE_INDEX` (EM-D), `CALIBRATION_ERROR` (EM-E), `REGULATION_STABILITY` (EM-G/EM-H), `IMPULSIVITY` via SSRT (EM-A).
- Fronteras de seguridad: todos mantienen induccion leve-only (techo 3), proxies que arman check-in sin inferir estado, copy escaneado, `null` nunca `0`, `preliminary=true`, `TransferLabel` honesto (NOT_DEMONSTRATED/PLACEBO_RISK donde corresponde).

---



# 3.8 Creatividad (9 ejercicios)

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
