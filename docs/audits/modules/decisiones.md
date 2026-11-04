## Modulo: Decisiones

The state-class doc comment for Reversibility claims "Only the prompt is exposed... so classifying the card stays an honest decision" — but the prompt is `"Reversibility triage level N"`, which contains nothing to decide on. The lock duration IS leaked behaviorally: only irreversible cards have `lockedDurationMs > 0`, and the screen calls `holder.currentLockDurationMs()` before any choice; choosing "Irreversible" triggers a lock only when `lockDurationMs > 0`. This is a subtle but real answer-leak path I need to flag. I have everything I need. Let me write the audit.

I have read all required code. Producing the audit now.

---

## Ejercicio: Check-in (CRI / Go-No-Go) (`DecisionCheckInStateHolder` / EX-Check-in)

### Diagnóstico rápido
Es un Go/No-Go clásico de 20 trials que NO entrena toma de decisiones: mide estado basal (control inhibitorio + velocidad + variabilidad) para producir un índice de "preparación" (CRI). Como gate de estado vale; como "ejercicio de decisiones" es un impostor — no hay ninguna decisión bajo incertidumbre aquí.

### Mecanismo cognitivo
Entrena/mide **control inhibitorio prepotente** (suprimir el toque en NO_GO) y **velocidad de procesamiento + estabilidad atencional** (RT y su CV). Esto está implementado correctamente: `DecisionCheckInStateHolder.finish()` (líneas 79-105) calcula accuracy sobre aciertos GO/NO_GO, RT medio solo sobre `GO && isCorrect` (línea 82-84, decisión correcta: no contamina el RT con errores), y CV poblacional con `n-1` (líneas 107-116). El `isCorrect` (líneas 123-127) es honesto: GO→tapped, NO_GO→!tapped.

- **Qué produce adaptación:** nada — no hay adaptación. Es medición pura de un solo bloque.
- **Qué SOBRA:** el `subjectiveFatigue` self-report de 3 botones (`FatigueSelector`, DecisionCheckInScreen líneas 130-148) entra con peso 20% al CRI (`CRI_FATIGUE_WEIGHT = 20.0`). Un autoinforme de 3 niveles inyecta 20 puntos de ruido autorreportado en un índice presentado como objetivo.
- **Qué CONTAMINA la medición:** [Riesgo científico] el CRI mezcla en una sola cifra accuracy (50%), estabilidad (30%) y fatiga subjetiva (20%) — `ScoringEquations.calculateCRI` líneas 99-112. Tres constructos distintos colapsados a un escalar no es interpretable: un CRI=70 puede venir de alta accuracy+alta fatiga o lo contrario.
- **Qué es solo práctica de la tarea:** todo. 20 trials de Go/No-Go con `DEFAULT_GO_RATIO = 0.7` (70% GO) es exactamente el paradigma donde el sujeto desarrolla un sesgo prepotente de respuesta y mejora por práctica del propio test.

### Potencial de transferencia
**Muy baja.** Es mejora-en-la-tarea pura. El control inhibitorio Go/No-Go tiene una de las literaturas de transferencia más débiles (no generaliza a inhibición en contextos distintos). Pero su rol aquí NO es entrenar — es calibrar estado. Como **diagnóstico de readiness** su transferencia es irrelevante; como ejercicio sería casi nula. El problema es que está vendido dentro de un "módulo de decisiones".

### Problemas detectados
- `DecisionCheckInScreen` RunningContent (líneas 158-162): el `LaunchedEffect(state.trialIndex) { delay(window); onTimeout() }` se relanza por índice, pero **no hay delay aleatorio inter-estímulo (ISI)**. El estímulo aparece instantáneamente al cambiar de trial, sin foreperiod variable. Esto permite **anticipación rítmica**: el usuario aprende el tempo y responde por ritmo, no por estímulo. Mata la validez del RT como medida de procesamiento.
- No hay distinción entre **omisión** (no tocar GO) y **comisión** (tocar NO_GO). Ambos caen en `!isCorrect` y se promedian a una sola accuracy. Comisión (fallo inhibitorio) y omisión (lapso atencional) son procesos distintos; fusionarlos pierde la señal de impulsividad que un módulo de decisiones debería querer.
- `goRatio = 0.7` fijo: ratio GO/NO_GO no adaptativo. El balance prepotencia/inhibición no se ajusta al sujeto.
- Sin filtro de RT mínimo en check-in: un toque en 50 ms cuenta como acierto GO válido y entra al RT medio (`onTap` solo hace `coerceAtLeast(0L)`, línea 52). Anticipaciones contaminan el baseline.

### Riesgos de falsa mejora
- **Efecto práctica máximo:** el CRI subirá sesión a sesión por familiaridad con el paradigma, NO por mejor "preparación cognitiva". Como el CRI alimenta el eje "Preparación" del polígono final (`DecisionResultStateHolder` línea 84), el usuario verá su preparación "mejorar" cuando solo se ha acostumbrado al test.
- **Fatiga autorreportada infla/desinfla a voluntad:** tocar "Descansado" (0.9) sube el CRI 18 puntos sin ningún cambio real. Pseudoprogreso trivialmente gameable.

### Métricas recomendadas
- **Principal:** d' (sensibilidad, teoría de detección de señales) separando hits/false-alarms — NO accuracy agregada.
- **Secundarias:** tasa de comisión (impulsividad) y omisión (lapsos) por separado; RT solo de hits; CV de RT; pendiente intra-bloque (deriva atencional).
- **Calibración:** correlación entre fatiga autorreportada y RT/CV reales (¿el usuario sabe leer su estado? — esto SÍ es metacognición útil).
- **Engañan hoy:** `accuracy` agregada (oculta el tipo de error), `criValue` (mezcla 3 constructos), eje "Preparación" del polígono (efecto práctica).

### Progresión recomendada
El check-in **no debe progresar en dificultad** — es un instrumento de medida, debe ser estable para ser comparable longitudinalmente. Lo que debe hacer es: ISI aleatorio (1-3 s), filtro de anticipación (<150 ms = inválido), y usar el baseline para **calibrar la dificultad de Bayes/Reversibilidad** (hoy NO lo hace: la dificultad de esos ejercicios está hardcodeada e ignora el CRI).

### Rediseño propuesto
Convertir en **diagnóstico de readiness puro y bien medido**, no en "ejercicio":
1. ISI aleatorio uniforme 1000-2500 ms antes de cada estímulo.
2. Filtro: RT<150 ms → anticipación inválida (descartar y re-presentar). RT en check-in calculado solo sobre respuestas válidas.
3. Reportar **d'** + comisión/omisión separadas + CV, no un CRI agregado opaco. Si se quiere un escalar, que sea solo d' o solo RT-CV, etiquetado por lo que es.
4. **Cablear el CRI a la dificultad de la sesión:** CRI bajo → `DifficultyAdapter` baja `timePressure`/`memoryLoad` de Bayes y Reversibilidad (hoy el `DifficultyAdapter` existe pero NUNCA se llama en el flujo — ver Veredicto).
5. La fatiga autorreportada se guarda como dato de calibración metacognitiva (¿predice el RT real?), no como input directo del score.
**Por qué es superior:** medida válida (sin anticipación), interpretable (constructos separados), y por fin funcional (calibra la sesión en vez de ser un trámite decorativo).

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: 2 | 2) Transferencia a la vida real: 1 | 3) Claridad del mecanismo: 6 | 4) Calidad de la progresión: 1 (no progresa, ni calibra nada) | 5) Calidad de la medición: 4 (sin ISI/SDT está roto) | 6) Personalización: 1 | 7) Resistencia al efecto práctica: 2 | 8) Valor metacognitivo: 3 (la fatiga podría serlo, no se usa así) | 9) Valor emocional: 2 | 10) Riesgo de fatiga/frustración (10=sin riesgo): 8 | 11) Riesgo de pseudoprogreso (10=sin riesgo): 3 | 12) Adherencia: 5 | 13) Escalabilidad a perfiles: 4 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 3.4/10**
**Decisión final:** **Convertir en diagnóstico** (readiness puro, bien medido) y cablearlo a la dificultad de la sesión. No es un ejercicio de decisiones y no debe presentarse como tal.

---

## Ejercicio: Bayes Arena (`BayesArenaStateHolder` / EX-07/08)

### Diagnóstico rápido
Es el único ejercicio del módulo con un mecanismo cognitivo real y defendible: estimación de probabilidad posterior con base rate, sensibilidad y tasa de falsos positivos visibles. Entrena razonamiento bayesiano / sensibilidad a la tasa base. Pero la mecánica con TODA la información visible en pantalla simultáneamente lo degrada de "entrenar intuición probabilística" a "aplicar una fórmula con los tres números delante".

### Mecanismo cognitivo
Entrena **razonamiento probabilístico bajo incertidumbre** y **resistencia a la falacia de la tasa base** — exactamente el constructo declarado. La mecánica está alineada: `ConsoleCard` (BayesArenaScreen líneas 163-185) muestra prevalencia/sensibilidad/falsas alarmas; el usuario fija un slider de "probabilidad real" (posterior) y otro de "certeza". `BayesEngine.calculatePosterior` (líneas 46-62) implementa Bayes correctamente; `evaluateBayesianUpdate` (líneas 85-99) mide cuánto del movimiento logit del usuario (prior→posterior) coincide con el movimiento normativo — esto es bueno: mide **dirección y magnitud de la actualización**, no proximidad ingenua.

- **Qué produce adaptación:** la discrepancia entre tu posterior y el normativo, mostrada en `ScenarioFeedbackRow` (Tú X% vs Bayes Y%, líneas 251-281). El feedback de actualización es el motor de aprendizaje legítimo.
- **Qué SOBRA / contamina:** el slider de **certeza** se captura (`confidence`) pero **no se usa para nada puntuable** — el comentario en `BayesArenaUiState.kt` (líneas 6-9) admite que Brier/calibración/CAG son "undefined" sin outcome binario resuelto. Correcto que no se finjan, pero entonces **pedir certeza es teatro cognitivo**: recoges el dato y lo tiras. Es carga sin medición.
- **Qué es solo práctica de la tarea:** con base rate, sensibilidad y FPR los tres en pantalla, esto no entrena **intuición** probabilística — entrena **ejecución de la fórmula de Bayes**. Tras unas sesiones el usuario memoriza el procedimiento (P·S / (P·S + (1−P)·FPR)) y deja de razonar.

### Potencial de transferencia
**Baja a media.** El razonamiento bayesiano explícito tiene mejor evidencia de transferencia que la mayoría de tareas de brain-training **si se entrena en formato de frecuencias naturales y con la información NO toda presentada en bandeja**. Tal como está (porcentajes, todo visible, mismo template "da positivo, ¿probabilidad real?"), la transferencia es **baja**: el usuario aprende a resolver ESTE formato, no a detectar que ignoró la tasa base en una decisión de la vida real. Para subirla: formatos de frecuencia natural ("de 1000 personas..."), ocultar parte de la info forzando recuperación, variar el dominio narrativo (médico, fraude, contratación), y pedir la decisión derivada (¿actúas o no?), no solo el número.

### Problemas detectados
- **Template único repetido:** `ConsoleCard` siempre muestra la misma frase "Da positivo. ¿Qué probabilidad real hay de que sea cierto?" (línea 178). Solo cambian los tres porcentajes. Cinco escenarios idénticos en estructura → el usuario resuelve por patrón, no por comprensión. [Mejora crítica]
- **Certeza recogida y descartada** (`submit`, BayesArenaStateHolder líneas 80, y `BayesEstimateFeedback.confidence` que nunca alimenta un score). Mecánica muerta.
- **`objectivesAlignment` = proximidad lineal** (`1.0 - abs(posterior - expected)`, línea 66) entra al ICD con peso 30%, PERO el ICD también penaliza vía `burError` con peso 35% (`calculateICD` líneas 61-81). Hay **doble conteo del mismo error**: tanto la proximidad como el BUR miden lo mismo (acierto del posterior) con dos transformaciones distintas. El ICD de Bayes está sobredeterminado por un único acto.
- **`biasDetected = false` siempre** (línea 67) y **`alternativesScannedRatio = 1.0` siempre** (línea 68): dos de los cuatro términos del ICD son constantes en Bayes. El ICD de un round Bayes es efectivamente `30%·proximidad + 35%·(1−burError) + 20%·1 + 15%·1` → 35 puntos regalados fijos. Inflación estructural del score.
- [Riesgo científico] **time limit de 45 s sin presión real:** `DEFAULT_TIME_LIMIT_MS = 45_000`. El `calculateIED` penaliza por `delayRatio²` con `lambda` reversible 0.8 — pero un round Bayes pasa `isReversible=false` (orchestrator línea 103) → `IRREVERSIBLE_DELAY_LAMBDA = 0.15`, penalización mínima. La velocidad casi no cuenta, lo cual es correcto para razonamiento... pero entonces el IED en Bayes es casi idéntico al ICD (efectividad ≈ calidad). Otro eje del polígono redundante.

### Riesgos de falsa mejora
- **Memorización de procedimiento:** con todo visible y template fijo, el progreso medido (BUR→1.0, error→0) reflejará dominio del formato, no mejor calibración probabilística general. Pseudoprogreso alto.
- **Score inflado por constantes:** 35/100 puntos del ICD son regalo fijo (bias=0, alt=1). El "ICD" parece alto desde el primer día.
- **Efecto novedad bajo / aburrimiento alto:** cinco escenarios estructuralmente idénticos por sesión → fatiga de repetición, no de carga.

### Métricas recomendadas
- **Principal:** error absoluto en logit-odds (no en probabilidad lineal — el espacio probabilístico no es lineal) entre posterior del usuario y normativo. Hoy se reporta `absoluteError` en espacio lineal (engaña: un error de 0.05 cerca de 0.5 ≠ cerca de 0.99).
- **Secundarias:** **base-rate neglect index** (¿el error correlaciona con cuánto se aleja la base rate del 50%? — mide si ignora la tasa base, el constructo real); dirección del sesgo (¿sobre o infra-estima sistemáticamente?); BUR como está (es bueno).
- **Calibración metacognitiva:** AHORA SÍ usable — correlacionar `confidence` con `absoluteError`. Alta certeza + alto error = sobreconfianza. Esto es oro y está tirado.
- **Engañan hoy:** `meanAbsoluteError` lineal, ICD del round (35 puntos fijos), IED (≈ICD, redundante).

### Progresión recomendada
- Curva: empezar con frecuencias naturales explícitas → pasar a porcentajes → ocultar progresivamente uno de los tres parámetros (forzar recuperación de memoria/estimación) → introducir **evidencia contradictoria** (dos sensores, ya hay `calculatePosterior(baseRate, List<SensorEvidence>)` en BayesEngine líneas 67-83 ¡implementado y NO usado!) → cadenas de actualización secuencial (posterior de un test es prior del siguiente).
- Zona óptima: ajustar la "trampa" (cuánto se aleja la base rate de la intuición) al error histórico del usuario. El `DifficultyAdapter` debería mover `noiseLevel` (que controla la separación base-rate, BayesEngine líneas 15-36).
- Evitar techo: el formato actual tiene techo bajo (se domina rápido); la actualización secuencial multi-evidencia sube el techo enormemente.

### Rediseño propuesto
1. **Usar el motor multi-sensor que ya existe** (`calculatePosterior` con `List<SensorEvidence>`): presentar 2-3 fuentes de fiabilidad distinta, algunas contradictorias. El usuario integra evidencia, no aplica una fórmula de un paso.
2. **Frecuencias naturales** como formato base ("De 1000 personas, 10 tienen X. El test detecta 9 de esos 10. De los 990 sanos, marca 50."). Mejor evidencia de transferencia que porcentajes.
3. **Ocultar un parámetro** en niveles altos → memoria de trabajo + estimación, no lectura.
4. **Activar la certeza:** medir CAG (gap confianza-acierto) con `calculateCAG` (ya existe, líneas 152-153) usando |error|<umbral como "acierto". Surfacear "tu certeza vs tu precisión real" — calibración metacognitiva visible.
5. **Eliminar el doble conteo:** el ICD de Bayes = solo término BUR (o solo proximidad en logit), no ambos. Quitar los términos constantes bias/alt del cómputo en este ejercicio.
6. **Dominios narrativos rotatorios** (médico/fraude/clima/contratación) sobre el mismo motor → reduce memorización de patrón.
**Cómo testearlo:** test de generador que verifique que la posterior normativa multi-sensor coincide con cálculo manual; test de que el CAG correlaciona con sobreconfianza simulada; test de que el ICD ya no regala 35 puntos.
**Por qué es superior:** entrena integración de evidencia (transferible) en vez de ejecución de fórmula (memorizable); usa código que YA está escrito; activa metacognición que hoy se desperdicia.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: 5 | 2) Transferencia a la vida real: 4 | 3) Claridad del mecanismo: 8 | 4) Calidad de la progresión: 2 (no progresa, difficulty hardcodeada) | 5) Calidad de la medición: 5 (BUR bueno, error lineal y doble conteo malos) | 6) Personalización: 1 | 7) Resistencia al efecto práctica: 3 | 8) Valor metacognitivo: 4 (certeza recogida y tirada) | 9) Valor emocional: 3 | 10) Riesgo de fatiga/frustración (10=sin riesgo): 6 | 11) Riesgo de pseudoprogreso (10=sin riesgo): 4 | 12) Adherencia: 4 | 13) Escalabilidad a perfiles: 5 | 14) Robustez científica: 6 | 15) Prioridad de mejora: 8
**Nota global del ejercicio: 4.5/10**
**Decisión final:** **Rediseñar parcialmente.** El núcleo es bueno y único en el módulo. Activar el motor multi-sensor existente, frecuencias naturales, certeza→calibración, y eliminar el doble conteo del ICD. Es el ejercicio con mayor ROI de mejora.

---

## Ejercicio: Triaje de Reversibilidad (`ReversibilityTriageStateHolder` / EX-19)

### Diagnóstico rápido
Roto en su raíz cognitiva: pide clasificar 6 "decisiones" como reversibles o irreversibles, pero **la carta no contiene ninguna decisión** — el prompt es literalmente el string `"Reversibility triage level N"` (`ReversibilityEngine.kt` líneas 34-35) y la respuesta correcta es **una tirada de moneda** (`random.nextDouble() >= irreversibility`, línea 18) sin relación alguna con el texto mostrado. El usuario no puede razonar nada; solo puede adivinar. Es un generador de aleatoriedad disfrazado de ejercicio de juicio.

### Mecanismo cognitivo
**Pretende** entrenar la distinción Tipo 1 (irreversible, requiere deliberación) vs Tipo 2 (reversible, requiere velocidad) — un concepto real y valioso (la asimetría reversibilidad/coste de error de Bezos). **Lo que realmente entrena: nada.** No hay contenido sobre el que aplicar el criterio. La "mecánica de la firma" (lock de deliberación en irreversibles, `IrreversibleConfirm` líneas 191-217) está bien diseñada en abstracto, pero opera sobre cartas vacías.

- **Qué produce adaptación:** cero. No se puede aprender a discriminar reversible/irreversible cuando la etiqueta correcta es ruido sin features.
- **Qué CONTAMINA / rompe la medición:** [Riesgo científico — crítico] la accuracy (`correct = selectedReversible == scenario.isReversible`, orchestrator línea 122) mide **suerte**, no juicio. Con `irreversibility=0.5` (DEFAULT_DIFFICULTY) es 50/50: el accuracy esperado de un usuario perfecto y de un mono son idénticos. El eje del polígono y el ICD que derivan de esto son **puro azar promediado**.
- **Fuga de respuesta:** [Mejora crítica] el lock SOLO existe en cartas irreversibles (`lockedDurationMs = 0` si reversible, >0 si no, `ReversibilityEngine` líneas 25-30). La pantalla llama `holder.currentLockDurationMs()` ANTES de la elección (ReversibilityTriageScreen línea 79) y el botón "Irreversible" solo abre el lock `if (lockDurationMs > 0)` (línea 160). El comentario en `ReversibilityTriageUiState.kt` (líneas 31-34) afirma "never the lock duration... so classifying stays honest" — **es falso**: un usuario que toque "Irreversible" y vea si aparece "Espera…" puede inferir la respuesta correcta y rectificar mentalmente. La respuesta se filtra por el comportamiento del propio lock.

### Potencial de transferencia
**Nula.** No se transfiere lo que no se entrena. Aunque el concepto reversibilidad/irreversibilidad SÍ tiene altísimo valor funcional en la vida real (es quizá el heurístico de decisión más útil que existe), esta implementación no lo entrena en absoluto. Transferencia potencial del CONCEPTO bien implementado: **alta/muy alta**. Transferencia de ESTO: nula.

### Problemas detectados
- **`description` vacía de contenido** (`ReversibilityEngine.generateScenario` líneas 33-39): no hay banco de escenarios, no hay features, no hay nada que clasificar. Defecto fundacional. [Mejora crítica]
- **`isReversible` aleatorio** sin relación con el prompt (línea 18). La "respuesta correcta" no es derivable de nada visible. Imposible de aprender.
- **Fuga de respuesta vía lock** (descrita arriba).
- **`alternativesScannedRatio = 1.0` y `biasDetected = false`** fijos en el round (ReversibilityTriageStateHolder líneas 70-72) → mismo problema de inflación del ICD que Bayes, pero peor porque aquí la `objectivesAlignment` es binaria 0/1 azarosa (orchestrator líneas 122-125).
- **`delayPenalty`** se calcula y promedia (`meanDelayPenalty`) pero solo sobre cartas reversibles, midiendo "sobre-deliberación" — pero como no hay nada que deliberar, penaliza pensar sobre una carta vacía.
- **6 cartas idénticas** salvo el número de nivel en el texto. Banco efectivo: 0 escenarios reales.

### Riesgos de falsa mejora
- **Pseudoprogreso por azar:** la accuracy oscilará por suerte alrededor de 50%. Una racha buena se leerá como "mejora del juicio de reversibilidad". Es la métrica más engañosa de todo el módulo.
- **Gaming trivial vía lock:** estrategia óptima del usuario = tocar siempre "Irreversible" primero; si aparece el lock, la carta era irreversible (confirmar); si no, era reversible (entonces re-elegir reversible). Accuracy potencial: ~100% **sin ningún juicio**. El ejercicio es explotable hasta el 100% por un bug de diseño.
- **Feedback falsamente positivo:** "X% clasificación correcta" sobre respuestas aleatorias.

### Métricas recomendadas
- **Principal:** d' de discriminación reversible/irreversible **sobre un banco con features reales** (sin banco real, ninguna métrica es válida).
- **Secundarias:** sensibilidad al coste asimétrico (¿delibera más cuando el coste de error irreversible es alto?); RT en irreversibles vs reversibles (¿ajusta velocidad al tipo? — esto es el constructo real: rápido en Tipo 2, lento/deliberado en Tipo 1); tasa de respeto del lock genuino.
- **Engañan hoy TODAS:** `accuracy` (azar), `lockViolations` (sobre cartas vacías), `meanDelayPenalty` (penaliza pensar sobre nada). Ninguna métrica actual mide lo que dice medir.

### Progresión recomendada
- Necesita primero **existir** (banco de escenarios con contenido). Luego: nivel 1 = casos prototípicos claros (borrar un email vs enviar dinero); nivel 2 = casos ambiguos donde la reversibilidad depende del coste/tiempo de deshacer; nivel 3 = casos con reversibilidad parcial o costosa (recuperable pero caro) que fuerzan juicio fino; nivel 4 = presión temporal + coste de error variable que obliga a asignar deliberación proporcional al riesgo.
- Zona óptima: ajustar la ambigüedad de los casos al d' del usuario.

### Rediseño propuesto
1. **Banco real de escenarios** (`ReversibilityScenario.description` con contenido genuino): situaciones donde reversibilidad es una propiedad inferible de features (coste de deshacer, ventana temporal, dependencias). 60+ casos rotatorios para evitar memorización.
2. **`isReversible` derivado de features del escenario**, no aleatorio — exactamente el patrón que el propio repo ya aprendió con E6 task-switching (CLAUDE.md 2026-06-18: "el payload debe transportar las features... NUNCA exponer la respuesta directamente"). Aquí se viola esa regla: la respuesta no es derivable de nada.
3. **Cerrar la fuga del lock:** el lock NO puede depender de la respuesta correcta. Opción: aplicar una pausa de confirmación a CUALQUIER elección "irreversible" del usuario (sea correcta o no), independiente de `scenario.isReversible`. Así el lock refleja la decisión del usuario, no la verdad oculta.
4. **Medir asignación de deliberación**, no solo clasificación binaria: el constructo valioso es "¿inviertes más cognición cuando el error es irreversible?". Métrica = correlación entre coste-de-error del caso y tiempo/profundidad de análisis.
5. **Introducir reversibilidad graduada** (no binaria): "recuperable pero cuesta X". Fuerza juicio real en vez de etiqueta sí/no.
**Cómo testearlo:** test que verifique que `isReversible` es función determinista de las features del escenario (no de Random); test que el lock se dispara por la elección del usuario y no por la verdad; test de banco ≥ N escenarios distintos sin repetición en una sesión.
**Por qué es superior:** pasa de medir suerte a medir juicio; cierra una vía de gaming al 100%; entrena el heurístico de decisión más transferible que existe; alinea con la regla que el propio repo ya documentó para otros ejercicios.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: 1 | 2) Transferencia a la vida real: 1 | 3) Claridad del mecanismo: 2 (concepto claro, implementación inexistente) | 4) Calidad de la progresión: 1 | 5) Calidad de la medición: 1 (mide azar) | 6) Personalización: 1 | 7) Resistencia al efecto práctica: 1 (gameable al 100%) | 8) Valor metacognitivo: 1 | 9) Valor emocional: 2 | 10) Riesgo de fatiga/frustración (10=sin riesgo): 5 | 11) Riesgo de pseudoprogreso (10=sin riesgo): 1 | 12) Adherencia: 3 | 13) Escalabilidad a perfiles: 1 | 14) Robustez científica: 1 | 15) Prioridad de mejora: 10
**Nota global del ejercicio: 1.7/10**
**Decisión final:** **Rediseñar completamente.** El concepto vale mucho; la implementación no entrena nada y es explotable al 100%. Sin banco de contenido real y `isReversible` derivado de features, debe considerarse **no funcional** (la accuracy reportada es ruido). Máxima prioridad del módulo.

---

## Ejercicio: Resultado + Diario + Polígono Cognitivo (`DecisionResultStateHolder`)

### Diagnóstico rápido
No es un ejercicio sino la pantalla de cierre: ensambla la sesión, pinta un polígono de 5 ejes, muestra un mensaje de coach y captura un diario de texto libre. El diario tiene valor metacognitivo genuino; el polígono es honesto en estructura (no falsea) pero hereda toda la basura de los ejercicios que lo alimentan, presentando con autoridad visual unas cifras que vienen de azar (Reversibilidad) y procedimiento memorizable (Bayes).

### Mecanismo cognitivo
El **diario reflexivo** es lo único con mecanismo propio: **metacognición / reflexión post-decisión** — escribir por qué decidiste algo mejora la calibración futura (literatura de prediction journaling / decision journals es de las pocas intervenciones de "decisiones" con evidencia decente). El **polígono** es solo visualización. El **coach** (`CoachEngine.selectTemplate`, líneas 118-136) es una cascada de reglas que selecciona un mensaje por perfil (impulsivo/sobreconfiado/etc.).

- **Qué SOBRA:** el polígono tiene ejes redundantes: "Calidad" (ICD) y "Efectividad" (IED) son casi colineales porque el IED en estos ejercicios ≈ ICD (penalización temporal mínima, ver Bayes). Dos ejes que miden casi lo mismo crean falsa riqueza visual.
- **Qué CONTAMINA:** [Riesgo científico] "Integridad" = `(1 - gamingScore)·100` y "Frescura" = `(1 - fatigueScore)·100` (DecisionResultStateHolder líneas 78-79). El `gamingScore` (AGRS) en Bayes/Reversibilidad recibe `spatialEntropy=1.0` e `isDialDefault=false` fijos → `calculateAGRS` (líneas 199-210) queda determinado SOLO por `rtFactor` (RT<400 ms). Como nadie resuelve Bayes en <400 ms, AGRS≈0 siempre → "Integridad" siempre ≈100. Eje decorativo clavado al máximo.
- **Coach honesto pero sobre datos sucios:** el coach es razonable (`render`, líneas 140-153), pero dispara perfiles ("sobreconfiado", "impulsivo") a partir de señales que en parte son azar (la accuracy de Reversibilidad) o constantes (AGRS).

### Potencial de transferencia
Del **diario: media** (la práctica de articular razonamiento de decisión SÍ transfiere a decisiones reales, es el componente más "vida real" del módulo). Del polígono/coach: **nula** como entrenamiento (son output, no práctica). El riesgo es que el polígono dé sensación de progreso medible que los ejercicios subyacentes no sostienen.

### Problemas detectados
- **Diario almacenado en plaintext** pese a llamarse `encryptedContent`: `buildDiaryEntry` (líneas 69-74) hace `diaryText.encodeToByteArray()` sin cifrar; `safetyFlag=false` siempre. El nombre del campo miente sobre lo que hace. (La memoria del proyecto dice que Track A pivotó a `SecureKeyValueStore`, pero en ESTE estado el contenido es texto plano en el `DiaryEntry`.) [Riesgo de privacidad — dato sensible de decisiones personales]
- **Ejes redundantes** (Calidad≈Efectividad; Integridad clavada a ~100).
- **El polígono no muestra evolución longitudinal:** es una foto de UNA sesión. Sin comparación contra baseline propio, no comunica progreso — justo lo que un trainer cognitivo necesita.
- **`complexity` ("Reto enfrentado")** = CDE, derivado de `DEFAULT_DIFFICULTY` hardcodeado → es **constante sesión a sesión** porque la dificultad nunca cambia. Mostrar un "reto" que nunca varía es engañoso.

### Riesgos de falsa mejora
- **Autoridad visual sobre datos de azar:** un radar pulido de 5 ejes comunica precisión científica; debajo, dos ejes son azar/constante. Es el vehículo perfecto de pseudoprogreso.
- **Coach reforzando ruido:** decir "sobreconfiado" basado en una accuracy aleatoria de Reversibilidad puede dar feedback psicológicamente cargado y falso.
- **Sin longitudinal, la "mejora" es invisible o inventada** según el día.

### Métricas recomendadas
- **Principal de la pantalla:** delta contra baseline propio del usuario (mejora longitudinal) por constructo separado, NO un radar de una sesión.
- **Calibración metacognitiva:** comparar lo que el usuario escribe en el diario / su certeza declarada con su acierto real (cuando los ejercicios la midan bien).
- **Quitar del polígono:** "Integridad" (clavada) y fusionar "Calidad"/"Efectividad" mientras IED≈ICD.
- **Engañan hoy:** "Integridad" (~100 fijo), "Reto enfrentado" (constante), y el radar entero presentado sin contexto longitudinal.

### Progresión recomendada
La pantalla debe **evolucionar con el historial**: mostrar tendencia (sesión actual vs media móvil propia), destacar el constructo con mayor margen de mejora, y guiar la próxima sesión (cerrar el loop con `DifficultyAdapter`). Hoy es un dead-end estático.

### Rediseño propuesto
1. **Cifrar de verdad el diario** (usar el `SecureKeyValueStore` que el repo ya tiene para datos cognitivos sensibles, CLAUDE.md 2026-06-05) o renombrar el campo a lo que realmente es. No llamar "encrypted" a texto plano.
2. **Polígono → 3 ejes no redundantes**: Razonamiento probabilístico (Bayes), Juicio de reversibilidad (cuando exista), Calibración metacognitiva (certeza vs acierto). Eliminar Integridad/Frescura del radar principal (moverlos a un indicador de validez de sesión, no a un eje de habilidad).
3. **Vista longitudinal**: cada eje contra el baseline propio + flecha de tendencia.
4. **Diario estructurado opcional**: prompts ("¿qué evidencia pesó más?", "¿qué harías distinto?") en vez de campo libre vacío — sube el valor metacognitivo y permite análisis.
5. **Coach gated por validez:** no disparar perfiles psicológicos sobre ejes que provienen de azar o constantes.
**Cómo testearlo:** test de que el diario persiste cifrado; test de que el polígono no incluye ejes con varianza ~0; test de que el coach no se activa cuando la señal subyacente es inválida.
**Por qué es superior:** comunica progreso real (longitudinal), no infla con ejes muertos, protege datos sensibles, y eleva el único componente con transferencia (el diario).

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: 3 (solo el diario) | 2) Transferencia a la vida real: 4 (diario) | 3) Claridad del mecanismo: 5 | 4) Calidad de la progresión: 2 (estática, sin longitudinal) | 5) Calidad de la medición: 3 (ejes redundantes/clavados) | 6) Personalización: 2 | 7) Resistencia al efecto práctica: 4 | 8) Valor metacognitivo: 5 (diario) | 9) Valor emocional: 5 (cierre + reflexión) | 10) Riesgo de fatiga/frustración (10=sin riesgo): 8 | 11) Riesgo de pseudoprogreso (10=sin riesgo): 2 (radar engañoso) | 12) Adherencia: 6 | 13) Escalabilidad a perfiles: 3 | 14) Robustez científica: 3 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 3.9/10**
**Decisión final:** **Rediseñar parcialmente.** Conservar y reforzar el diario (cifrarlo + estructurarlo); reconstruir el polígono a ejes no redundantes y longitudinales; gatear el coach por validez. La pantalla es honesta en intención pero amplifica visualmente el ruido de los ejercicios.

---

## Veredicto del módulo "Decisiones (toma de decisiones bajo incertidumbre)"

### Capacidades bien cubiertas / huecos / capacidades AUSENTES
- **Bien cubiertas (parcial):** razonamiento probabilístico bayesiano (Bayes Arena — único ejercicio con mecanismo real); control inhibitorio/readiness basal (Check-in, como diagnóstico). Reflexión post-decisión (diario).
- **Huecos:** el juicio de reversibilidad (concepto presente, implementación nula); calibración metacognitiva (la certeza se recoge en Bayes y se TIRA; el motor `calculateCAG`/`calculateBrierScore`/`calculateCalibrationError` está implementado en `ScoringEquations` y **nunca se usa** en la UI).
- **AUSENTES por completo:** integración de evidencia multi-fuente bajo conflicto (el motor `calculatePosterior(baseRate, List<SensorEvidence>)` existe y está **muerto**); decisión secuencial / planificación bajo incertidumbre (MDP — el paquete se llama `core/mdp` pero no hay ningún proceso de decisión multi-paso, solo juicios de un disparo); trade-off coste-beneficio explícito; decisión bajo presión temporal real (los time limits son 15-45 s, sin presión); manejo de ambigüedad/ignorancia (vs riesgo cuantificado); aprendizaje por feedback de outcomes resueltos (no hay outcome binario en ningún ejercicio → por eso Brier está desactivado).

### Ejercicios redundantes o a fusionar dentro del módulo
- **Check-in NO pertenece al módulo como ejercicio** — es un diagnóstico de estado prestado. Mantenerlo como gate/calibrador, no como "1 de 3 ejercicios de decisiones".
- **No hay redundancia entre Bayes y Reversibilidad** (entrenan cosas distintas) — el problema es que Reversibilidad no entrena nada. No fusionar; arreglar.
- **Dentro del scoring sí hay redundancia grave:** ICD≈IED (penalización temporal casi nula), y dos ejes del polígono (Calidad/Efectividad) son colineales. El término `objectivesAlignment` y `burError` doble-cuentan el mismo error en Bayes.

### Riesgos sistémicos del módulo (medición común, banco de contenido, adaptación)
1. **El `DifficultyAdapter` está MUERTO.** Existe en `core/adaptive/DifficultyAdapter.kt`, implementa una política de subida/bajada por error e ICD, pero **nunca se invoca en el flujo de Decisiones** (grep confirmado: solo el módulo Logic usa su propio adapter). Toda la dificultad es `DEFAULT_DIFFICULTY` hardcodeada en cada state holder. **Cero personalización, cero zona óptima de desafío, cero adaptación.** Es el fallo sistémico nº1: el módulo no se adapta a nadie.
2. **Banco de contenido inexistente en Reversibilidad** y **template único en Bayes.** No hay variedad real de escenarios; el riesgo de memorización de patrón es alto en Bayes y la respuesta es azar en Reversibilidad.
3. **Inflación estructural del score común:** en TODOS los rounds, `biasDetected=false` y `alternativesScannedRatio=1.0` se pasan fijos → ~35 puntos del ICD (peso bias 20 + alt 15) son regalo constante en cada ejercicio. El ICD nunca puede bajar de ~35 aunque el usuario falle todo lo medible. Pseudoprogreso incorporado en el motor.
4. **Métricas de calibración implementadas y desconectadas:** Brier, error de calibración, CAG, coraje decisional — todo en `ScoringEquations`, todo sin cablear. El módulo desperdicia su mejor capacidad (medir sobre/infraconfianza).
5. **Validez de sesión clavada:** AGRS (anti-gaming) depende solo de RT<400 ms en estos ejercicios → "Integridad" siempre ~100. El control anti-gaming no protege nada real aquí.
6. **Privacidad:** el diario de decisiones (dato sensible) se persiste como `encryptedContent` que en realidad es **texto plano** (`encodeToByteArray()`).

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Reconstruir Reversibilidad con banco de contenido real + `isReversible` derivado de features + cerrar la fuga del lock.** Hoy un tercio del módulo mide azar y es gameable al 100%. Es la diferencia entre "no entrena nada" y "entrena el heurístico de decisión más transferible que existe". Máxima prioridad.
2. **Cablear el `DifficultyAdapter` al flujo y al CRI del check-in** (CRI bajo → menos presión/carga; ICD alto → subir variables/ambigüedad). Sin adaptación no hay zona óptima de desafío, no hay personalización, y el efecto práctica domina. Convierte un test estático en un entrenador.
3. **Activar la medición de calibración metacognitiva que ya está escrita** (CAG/Brier sobre la certeza recogida en Bayes y, tras resolverlos, sobre outcomes) y limpiar la inflación del ICD (quitar los términos constantes bias/alt). Esto convierte "¿acertaste el número?" en "¿sabes cuándo NO sabes?" — el constructo de decisiones bajo incertidumbre de mayor valor funcional, hoy completamente desperdiciado.

**Nota global del módulo (media ponderada por prioridad, ejercicios reales de decisiones): ≈3.3/10.** Un núcleo de cálculo serio y bien testeado (Bayes correcto, scoring elaborado) construido sobre tres fallos que lo vacían: un ejercicio que mide azar, una dificultad que nunca adapta, y la mejor capacidad de medición (calibración) desconectada. El potencial es real; el estado actual entrena poco y arriesga pseudoprogreso visible.

---

