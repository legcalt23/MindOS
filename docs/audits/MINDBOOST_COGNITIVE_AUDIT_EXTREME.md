# AUDITORIA EXTREMA DE MINDBOOST — Seccion 3: auditoria ejercicio por ejercicio

> Unidad multidisciplinar (agentes A-O) sobre el CODIGO REAL. Marco 6.1-6.8 + 15 ejes de puntuacion por ejercicio.
> Estado: 8/8 modulos auditados. Secciones 1,2,4-13 pendientes (workflow interrumpido por limite de sesion).

---



## Modulo: Enfoque (atencion / control ejecutivo)

I now have a thorough understanding of the entire module: catalog, all 13 generators, classifier, metrics engine, SSRT, adaptive engine, session assembler, runtime state holder, every renderer, personalization, evaluation/transfer, and the home/recommendation. I have what I need to write the brutal audit. One last critical detail: the runtime presents stimuli with a fixed feedback flash and `delay` per trial, and E9/E10/E8 push their own animation into the render while the holder awaits a single tap with a generic window. This is the crux of the measurement-validity critique for the dynamic paradigms.

Let me compose the final audit.

# Auditoría cognitiva — Módulo "Enfoque (atención / control ejecutivo)" de MindBoost

> Metodología: leído el código real (catálogo, los 13 generadores, `TrialClassifier`, `DefaultMetricsEngine`, `SsrtCalculator`, `DefaultAdaptiveEngine`, `FocusSessionAssembler`, `FocusSessionStateHolder` y **todos** los renderers, más personalización y evaluación/transfer). El veredicto se basa en lo que el runtime PRESENTA y MIDE de verdad, no en los nombres de los paradigmas ni en los KDoc.

**Hallazgo transversal que condiciona casi todo lo que sigue (léelo antes de los ejercicios):** el runtime (`FocusSessionStateHolder.runSession`, líneas 163-190) ejecuta TODOS los ejercicios con el MISMO bucle de "una respuesta por estímulo": `delay(ISI)` → `presentAndAwait` → captura UNA pulsación → `delay(FEEDBACK_MS=320)`. Esto encaja con E1/E2/E3/E4/E5/E6/E11/E13 (paradigmas de respuesta discreta), pero **falsea E7, E8, E9 y E10**, donde la dificultad real es temporal/espacial y el dominio la ha colapsado a un solo juicio. La animación de esos cuatro vive ENTERA dentro del composable (`FocusTrackingField`, `FocusStreamField`, `FocusDualTaskField`) y el `layoutSeed = stimulus.index` (no el seed del bloque) — es decir, **lo que el usuario ve no procede del generador determinista que luego se puntúa**. Eso rompe la cadena estímulo→respuesta→métrica en esos ejercicios. Volveré a ello por ejercicio.

---

## Ejercicio: Vigía (E1)

### Diagnóstico rápido
Es un PVT/Mackworth honesto y bien construido: el único ejercicio del módulo cuyo paradigma, runtime y métricas están perfectamente alineados. Vale, y es la joya del módulo. Su problema no es validez sino que entrena lo más estrecho.

### Mecanismo cognitivo
Atención sostenida / vigilancia con baja carga perceptiva: señales raras (`targetRarity` 0.30→0.06, `E1DifficultyMapping`) con ISI largo e impredecible (1200-7000 ms). El proceso entrenado es mantener readiness motora durante "casi nada" y resistir el decremento de vigilancia. **Lo que produce adaptación cognitiva:** la rareza + la incertidumbre temporal, que obligan a sostener el set atencional. **Lo que sobra:** nada estructural. **Lo que contamina:** los lures (`lure` en `Vigilance`) solo cambian el *render* (`FocusFieldState.Lure`), pero `isTarget=false` y `correctResponseKey=null` igual que un blanco normal — el lure NO es un no-go discriminable distinto, así que el `DISTRACTOR_SALIENCE` declarado relevante (catálogo) casi no muerde. **Solo práctica de la tarea:** mínima.

### Potencial de transferencia
**Baja.** Mejora-en-la-tarea alta; transferencia cercana a otras tareas de vigilancia/TR plausible; lejana/funcional (atención en la vida real) **no demostrada** — y el propio código es honesto al respecto (`honestNote`). El RT-CV sí es un marcador con cierto valor ecológico (estabilidad atencional), eso eleva ligeramente el techo.

### Problemas detectados
- `E1VigiaGenerator` (líneas 70-71): el lure se computa pero el clasificador no lo trata como categoría aparte → no hay "false alarm a lure" distinguible de cualquier comisión. El `lureProbability` (hasta 0.35) es casi cosmético para la métrica.
- `targetRarity` se mapea desde `perceptualLoad`, no desde `TIME_ON_TASK` (el `governingAxis` declarado). El eje que el catálogo dice gobernar el ejercicio (`TIME_ON_TASK`) NO altera la rareza ni el ISI en `E1DifficultyMapping`. Desalineación catálogo↔generador.
- `SessionTemplate.MAINTENANCE_5` por defecto + `estimatedTrialCount` (24-160) puede dar bloques demasiado cortos para que `VIGILANCE_DECREMENT_SLOPE` (MIN_N_SLOPE=6 hits) sea estable.

### Riesgos de falsa mejora
Bajo. Banco infinito (PRNG), sin patrón memorizable. Riesgo real: **mejora por aprendizaje del tempo de la app** (el usuario aprende el rango de ISI y "prepara el dedo"), inflando RT sin mejorar vigilancia. No hay jitter anti-anticipación más allá del floor de 150 ms.

### Métricas recomendadas
- **Principal:** RT-CV (ya es `governingMetric`, correcto) + pendiente de decremento.
- Secundarias: lapsos (sobre `rawHitRts`, ya bien resuelto — ver CLAUDE.md 2026-06-04), falsas alarmas a lure (no implementado), anticipaciones.
- **Métrica ACTUAL que engaña:** `SUSTAINED_FOCUS_INDEX = 100·(1−rtCv)·(1−lapseRate)` (`addFocusIndices`, l.418): es un compuesto inventado, no validado, presentado como índice. Sirve para UI, no para inferir capacidad.

### Progresión recomendada
La curva existe (rareza, ISI, ventana, contraste) pero el eje gobernante real debería ser tiempo-en-tarea (duración del bloque), no `perceptualLoad`. Añadir crecimiento de duración por bloque y jitter de ISI no aprendible. Evitar techo: bajar contraste (`0.4`) ya lo hace; suelo: con rareza 0.30 es fácil, bien.

### Rediseño propuesto
Mantener el motor. Cambios concretos: (1) que `TIME_ON_TASK` controle la duración del bloque y la rareza, alineando con el catálogo; (2) convertir el lure en un no-go discriminable real (`isTarget=false` + categoría de comisión propia) para que el filtrado selectivo cuente; (3) ISI con distribución exponencial truncada por trial (no uniforme) para matar el aprendizaje del tempo; (4) reportar pendiente de decremento intra-bloque como métrica primaria de vigilancia, no el índice compuesto. Testeable con el simulador determinista ya existente.

### Puntuación 0-10 (15 ejes)
1) 6 · 2) 4 · 3) 9 · 4) 6 · 5) 8 · 6) 6 · 7) 7 · 8) 4 · 9) 3 · 10) 7 · 11) 7 · 12) 5 · 13) 7 · 14) 7 · 15) 4
**Nota global: 6.2/10**
**Decisión final:** Mantener con cambios menores (alinear eje gobernante + lure discriminable).

---

## Ejercicio: Filtro (E2 · flanker)

### Diagnóstico rápido
Flanker de Eriksen correcto en dominio y bien clasificado (`classifyChoice`). El efecto de congruencia se mide de verdad. Es de los ejercicios sólidos del módulo. Su debilidad: la dificultad escala metiendo más flankers y acortando ventana, no necesariamente más interferencia.

### Mecanismo cognitivo
Control inhibitorio de respuesta selectiva / resolución de conflicto a nivel de respuesta. Responde al centro, ignora flancos. **Produce adaptación:** la proporción incongruente (`incongruentProportion` 0.20→0.60). **Contamina:** acortar `responseWindowMs` con `PRESSURE` (1500→900) mezcla velocidad con filtrado — un usuario puede empeorar por presión temporal sin que su filtrado cambie, y el `CONGRUENCY_EFFECT` (resta de medianas) absorbe parte de eso. **Solo práctica:** el mapeo flecha→lado se automatiza rápido.

### Potencial de transferencia
**Media-baja.** El efecto de congruencia es un marcador clásico de control atencional. Transferencia cercana a otras tareas de interferencia: plausible. Lejana: la literatura de entrenamiento de flanker muestra mejora-en-la-tarea robusta y transferencia escasa. El código no promete más (bien).

### Problemas detectados
- `E2FiltroGenerator` (l.45): dirección por `nextBoolean()` sin balanceo garantizado izquierda/derecha → sesgos de respuesta posibles en bloques cortos, contaminan el RT por mano.
- `flankerCount` (2→6) desde `perceptualLoad`: más flancos ≠ más interferencia (la interferencia flanker satura a ±2 flancos en la literatura). Aumentar el número infla carga visual, no conflicto. Es "dificultad falsa" parcial.
- `congruencyEffect` exige `MIN_N_CONGRUENCY=3` por celda — muy bajo; con 3 trials por condición la resta de medianas es ruidosa.
- El render (`FocusChoiceField.FlankerRow`) dibuja `perSide = distractorCount/2` flankers, pero el conteo no afecta a la clasificación: el efecto puede medirse aunque el render no respete `flankerCount`.

### Riesgos de falsa mejora
Aprendizaje del mapeo S-R y de la proporción de congruencia (si el usuario detecta que la mayoría son congruentes, baja la guardia). Banco infinito, sin memorización de ítems. Riesgo de inflar precisión bajando incongruentes en bloques fáciles.

### Métricas recomendadas
- **Principal:** efecto de congruencia (RT incongruente − congruente). Ya correcto.
- Secundarias: precisión por congruencia, post-error slowing en incongruentes, "conflict adaptation" (efecto Gratton: congruencia condicionada al trial previo) — **ausente y es la métrica de oro aquí**.
- **Métrica ACTUAL que engaña:** la precisión global mezcla congruentes/incongruentes; sube trivialmente si la proporción incongruente baja.

### Progresión recomendada
Gobernar por proporción incongruente y por **adaptación de conflicto** (secuencias cI/iI), no por nº de flankers ni por ventana. Mantener ventana fija para no confundir con velocidad.

### Rediseño propuesto
(1) Balancear dirección 50/50 por bloque; (2) congelar `responseWindowMs` y mover la dificultad a proporción incongruente + manipulación de secuencia (Gratton); (3) añadir métrica de adaptación de conflicto; (4) subir MIN_N por celda a ≥10. Mismo renderer, cambios de generador+métrica. Superior porque mide control de conflicto, no carga visual.

### Puntuación 0-10
1) 6 · 2) 5 · 3) 8 · 4) 6 · 5) 7 · 6) 6 · 7) 6 · 8) 4 · 9) 3 · 10) 7 · 11) 6 · 12) 6 · 13) 7 · 14) 7 · 15) 5
**Nota global: 6.0/10**
**Decisión final:** Mantener con cambios menores.

---

## Ejercicio: Gatillo (E3 · go/no-go)

### Diagnóstico rápido
Go/no-go limpio, clasificación correcta (`classify`), prepotencia bien modelada por frecuencia de go. Válido. Solapa fuertemente con E4 (ambos son inhibición, módulo C) — candidato a fusión/diferenciación.

### Mecanismo cognitivo
Inhibición de respuesta prepotente. `goProportion` 0.70→0.90 (`E3DifficultyMapping`) construye la prepotencia: cuanto más frecuente el go, más cuesta frenar el no-go. **Produce adaptación:** la prepotencia. **Contamina:** `PRESSURE` acorta ventana (900→600) → velocidad disfrazada de inhibición. **Solo práctica:** baja.

### Potencial de transferencia
**Baja.** Las falsas alarmas en go/no-go son un marcador de control, pero go/no-go entrena más detección de infrecuencia que inhibición pura (por eso E4/stop-signal es mejor para SSRT). Transferencia funcional a "control de impulsos en la vida": no demostrada.

### Problemas detectados
- `governingMetric = FALSE_ALARMS` (catálogo) es un **conteo crudo**, no una tasa. `addErrorTallies` (l.168) cuenta FA absolutas; con más trials, más FA — el adaptativo (`isImpulsive`, `falseAlarms >= 2.0`) usa un umbral absoluto que no escala con la longitud del bloque. Sesgo por longitud.
- Solo se garantiza **1** no-go (`forcedNoGoIndex`). Con go 0.90, un bloque corto puede tener poquísimos no-go → la métrica de inhibición se basa en 2-3 eventos. Frágil.
- Sin SDT: no se computa d′ ni criterio β. FA y omisiones se reportan sueltas, sin separar sensibilidad de sesgo de respuesta — y el sesgo de respuesta es exactamente lo que un go/no-go con go alto induce.

### Riesgos de falsa mejora
**Alto en pseudoprogreso por estrategia:** el usuario puede subir precisión simplemente respondiendo más lento (estrategia de cautela), reduciendo FA sin mejorar inhibición. Sin SDT no se detecta. El feedback `CORRECT_REJECTION` premia retener — bien — pero no penaliza la lentitud estratégica.

### Métricas recomendadas
- **Principal:** tasa de comisión (FA/no-go), no conteo. Idealmente **d′** y **criterio**.
- Secundarias: RT de go (para detectar enlentecimiento estratégico), post-error slowing.
- **Métricas ACTUALES que engañan:** `FALSE_ALARMS` como conteo absoluto; precisión global (inflable por lentitud).

### Progresión recomendada
Gobernar por prepotencia (go-rate) y por ratio no-go garantizado por bloque (p. ej. ≥15% no-go siempre), no por ventana. Penalizar enlentecimiento estratégico vinculando dificultad al RT de go.

### Rediseño propuesto
(1) FA como **tasa**; (2) garantizar nº mínimo de no-go por bloque proporcional a longitud; (3) añadir d′/criterio (SDT) para separar inhibición de cautela; (4) métrica anti-estrategia (penalizar RT-go creciente). Diferenciar de E4 haciendo E3 el de **detección de infrecuencia** y E4 el de **freno reactivo** explícitamente.

### Puntuación 0-10
1) 5 · 2) 4 · 3) 8 · 4) 6 · 5) 6 · 6) 6 · 7) 6 · 8) 4 · 9) 3 · 10) 7 · 11) 5 · 12) 6 · 13) 6 · 14) 6 · 15) 6
**Nota global: 5.5/10**
**Decisión final:** Rediseñar parcialmente (SDT + tasas) o fusionar el rol con E4.

---

## Ejercicio: Freno (E4 · stop-signal / SSRT)

### Diagnóstico rápido
El ejercicio **científicamente más ambicioso y mejor ejecutado** del módulo: staircase 1-up/1-down online real (`StopSignalTracker`), SSRT por método de integración (`SsrtCalculator`, Verbruggen), y detección de "gaming" cuando p(respond|signal) se aleja de 0.5. Esto está hecho por alguien que leyó el consenso. Es el mejor candidato a entrenamiento avanzado / medida de inhibición.

### Mecanismo cognitivo
Inhibición reactiva: cancelar una respuesta ya iniciada. El SSD se ajusta online hacia p≈0.5, el régimen donde SSRT es válido. **Produce adaptación:** el freno reactivo. **Contamina:** nada grave en dominio; el riesgo es la implementación de timing en dispositivo (latencia táctil), no el diseño. **Sobra:** nada.

### Potencial de transferencia
**Baja-media** (la más alta plausible del módulo en inhibición). SSRT es el marcador más validado de control inhibitorio. La literatura de entrenamiento stop-signal muestra mejora de SSRT y transferencia inhibitoria limitada pero más real que flanker. Funcional a la vida (autocontrol): sigue sin demostrarse, pero es el mejor candidato.

### Problemas detectados
- `presentStopTrial` (l.305-319): la fase GO dura `goPhaseMs = ssd` y solo entonces aparece el STOP. Pero la **latencia táctil del dispositivo** (no descontada) se suma al SSD efectivo → el SSRT estará inflado por el lag de input. `MonotonicClock` mide bien el RT, pero el SSD lo fija el código, no el hardware. [Dato necesario] calibración de latencia de pantalla/touch.
- `meanSsd` se promedia sobre los SSD de los stop trials (`stopSignalReactionTime`, l.394). Con staircase, el SSD final converge; usar la media de toda la trayectoria (incluido el período de convergencia) sesga el SSRT. El consenso recomienda excluir el burn-in.
- Un solo stop garantizado (`forcedStopIndex`). El método de integración necesita ~≥30 stop trials para un SSRT estable; un bloque de mantenimiento (24-160 trials × stopProportion 0.25-0.33) puede quedarse corto en el extremo bajo.
- `classifyStop` no distingue stop-respondido-rápido de stop-respondido-lento más allá del RT; bien, pero no marca anticipaciones en stop.

### Riesgos de falsa mejora
**El propio código lo mitiga** (flag `trustworthy` cuando p∉[0.25,0.75]). Riesgo restante: el usuario aprende a "esperar siempre" (proactive slowing), lo que dispara `trustworthy=false` correctamente. Bien diseñado contra el gaming. Banco infinito.

### Métricas recomendadas
- **Principal:** SSRT (integración), ya correcto, con su flag de confianza.
- Secundarias: p(respond|signal) (control de calidad), RT de go, proactive slowing (RT-go en bloques con vs sin stop).
- **Métrica ACTUAL que podría engañar:** SSRT cuando el burn-in del staircase se incluye en `meanSsd`; mitigar excluyéndolo.

### Progresión recomendada
Gobernar por `INHIBITORY_DEMAND` (proporción stop) y dejar el SSD que lo gobierne el staircase (ya lo hace). Asegurar nº mínimo de stop trials por evaluación. No tocar la ventana por presión en modo medida.

### Rediseño propuesto
(1) Descontar latencia de input medida del SSD; (2) calcular SSRT solo sobre la fase post-convergencia del staircase; (3) garantizar ≥30 stop trials en modo EVALUATION; (4) reportar proactive slowing como métrica de "trampa". Es ya el mejor; estos cambios lo hacen publicable.

### Puntuación 0-10
1) 7 · 2) 5 · 3) 9 · 4) 8 · 5) 8 · 6) 6 · 7) 8 · 8) 5 · 9) 3 · 10) 6 · 11) 8 · 12) 5 · 13) 7 · 14) 8 · 15) 4
**Nota global: 6.8/10** (el más alto del módulo)
**Decisión final:** Mantener con cambios menores → Convertir en entrenamiento avanzado / medida de inhibición de referencia.

---

## Ejercicio: Conflicto (E5 · Stroop/Simon)

### Diagnóstico rápido
**No es un Stroop.** Es un Simon espacial empobrecido: una flecha que aparece en un lado, respondes a su dirección. No hay dimensión de color-palabra, no hay lectura automática que inhibir. El `CONGRUENCY_EFFECT` se mide bien, pero el conflicto es puramente espacial (posición vs dirección), redundante con el flanker E2.

### Mecanismo cognitivo
Control de interferencia espacial (Simon), no Stroop. `congruent` = la flecha está en su propio lado; `incongruent` = lado opuesto (`ConflictArrow`, `FocusChoiceField` l.83). El proceso es inhibir la tendencia a responder hacia la *posición*. **Produce adaptación:** proporción incongruente (0.25→0.65). **Contamina:** `PRESSURE` acorta ventana. **Sobra/redundante:** todo el ejercicio solapa con E2 en mecanismo (resolución de conflicto E-R).

### Potencial de transferencia
**Baja.** Efecto Simon es marcador válido de interferencia, pero su transferencia es tan limitada como flanker. Y al ser otro ejercicio de conflicto de respuesta, no añade cobertura de capacidad respecto a E2 — añade redundancia.

### Problemas detectados
- Etiquetado erróneo: catálogo dice `Paradigm.STROOP`, instrucciones hablan de "dimensión relevante/conflicto", pero el render es Simon espacial puro (`ConflictArrow`). El enum `Paradigm.SIMON` existe pero NO se usa. Falsa etiqueta científica.
- `StimulusPayload.Conflict` solo lleva `relevantResponse` + `congruent`. No transporta NINGUNA segunda dimensión real (color, palabra). El "conflicto" lo fabrica el *render* moviendo la flecha de lado. El dominio no modela conflicto; lo simula la UI. Mismo pecado que el grupo dinámico, en menor grado.
- Sin balanceo de dirección ni control de proporción por celda robusto (mismo MIN_N_CONGRUENCY=3 frágil).

### Riesgos de falsa mejora
Aprendizaje de "ignora la posición, sigue la flecha" se automatiza en pocas sesiones → mejora-en-la-tarea rápida y techo bajo. Banco infinito pero mecanismo trivial de dominar.

### Métricas recomendadas
- **Principal:** efecto Simon (RT incongruente − congruente). Correcto.
- Secundarias: distribución delta (el efecto Simon decrece con RT — *delta plots* — es LA métrica diagnóstica del Simon y está ausente), adaptación de conflicto.
- **Métrica ACTUAL que engaña:** precisión global; y llamar a esto "Stroop" en cualquier copy de usuario sería una afirmación falsa.

### Progresión recomendada
Si se mantiene como Simon: gobernar por proporción incongruente + delta plots. Si se quiere Stroop de verdad: rehacer con dimensión color-palabra real.

### Rediseño propuesto
Dos caminos: (A) **Renombrar a Simon** honestamente y añadir delta plots — barato. (B) **Implementar un Stroop real** (palabra de color vs tinta) con dos dimensiones en el payload y respuesta por tinta — caro pero añade una capacidad distinta (interferencia semántica, no espacial) y deja de solapar con E2. Recomiendo (B) o **fusión con E2** si se queda en conflicto espacial.

### Puntuación 0-10
1) 4 · 2) 4 · 3) 5 · 4) 5 · 5) 6 · 6) 5 · 7) 5 · 8) 3 · 9) 3 · 10) 7 · 11) 5 · 12) 5 · 13) 6 · 14) 4 · 15) 7
**Nota global: 4.8/10**
**Decisión final:** Rediseñar parcialmente (Stroop real) o Fusionar con E2.

---

## Ejercicio: Cambio (E6 · task-switching)

### Diagnóstico rápido
**El task-switch mejor resuelto del grupo "derivar respuesta de un estímulo".** El generador deriva `correctResponse` de (regla activa, dimensiones color/forma) — no muestra la respuesta (ver CLAUDE.md 2026-06-18). El render presenta forma coloreada + cue de regla. Es honesto y `SWITCH_COST` se mide de verdad. Buen trabajo.

### Mecanismo cognitivo
Flexibilidad cognitiva controlada: reconfigurar el set de tarea (color vs forma) trial a trial. **Produce adaptación:** `switchFrequency` (0.30→0.60) e intervalo de preparación (`preparationMs` 1000→500 vía `TEMPORAL_UNCERTAINTY`). **Contamina:** `PRESSURE` en ventana. **Sobra:** nada grave. La preparación variable es el corazón del switch cost — bien modelada.

### Potencial de transferencia
**Baja-media.** El switch cost es un marcador central de control ejecutivo y flexibilidad. La literatura de entrenamiento de switching muestra transferencia cercana algo mejor que la de inhibición. Es de los pocos del módulo que toca flexibilidad (capacidad poco cubierta), eso sube su valor estratégico.

### Problemas detectados
- Solo dos dimensiones binarias (color rojo/azul, forma círculo/cuadrado) → 4 estados de estímulo posibles. Banco efectivamente **finito y minúsculo**: el usuario ve repetidamente las mismas 4 combinaciones. Riesgo de aprendizaje de mapeos estímulo-respuesta concretos en vez de aplicar reglas.
- `isSwitch` se sortea con `switchFrequency` pero sin control de longitud de run (puede haber rachas largas de repeat o switch) → switch cost ruidoso. No hay balanceo switch/repeat garantizado por bloque.
- `preparationMs` se mapea a `interStimulusDelayMs` (l.58) = es el ISI, no un cue-stimulus interval explícito con la regla mostrada durante la preparación. La manipulación clave del switch cost (CSI) es aproximada.
- `SWITCH_COST` exige `MIN_N_CONGRUENCY=3` por celda (switch/repeat). Frágil.

### Riesgos de falsa mejora
**Alto banco finito:** 4 estados de estímulo + 2 reglas = 8 situaciones. El usuario memoriza "rojo+regla color→izquierda" como tabla, no como aplicación de regla. La mejora puede ser memorización de la tabla S-R, no flexibilidad. Esto es el riesgo más serio de E6.

### Métricas recomendadas
- **Principal:** switch cost (RT switch − repeat). Correcto.
- Secundarias: mixing cost (bloques mixtos vs puros — ausente), switch cost asimétrico, residual switch cost con preparación larga.
- **Métrica ACTUAL que engaña:** switch cost con bancos de 8 estados puede reflejar memorización, no reconfiguración.

### Progresión recomendada
Ampliar el espacio de estímulos (más colores/formas/dimensiones) para impedir memorización S-R; gobernar por nº de reglas activas y por CSI (preparación), garantizar balanceo switch/repeat por bloque.

### Rediseño propuesto
(1) ≥3 dimensiones y ≥3 valores por dimensión para romper el banco finito; (2) CSI explícito (mostrar la regla durante una preparación cronometrada, separada del estímulo); (3) bloques mixtos vs puros para medir mixing cost; (4) controlar runs switch/repeat. Superior porque mide reconfiguración, no recuerdo de una tabla de 8 entradas.

### Puntuación 0-10
1) 6 · 2) 5 · 3) 7 · 4) 6 · 5) 6 · 6) 6 · 7) 4 · 8) 4 · 9) 3 · 10) 6 · 11) 4 · 12) 6 · 13) 7 · 14) 6 · 15) 6
**Nota global: 5.5/10**
**Decisión final:** Rediseñar parcialmente (romper banco finito + CSI real).

---

## Ejercicio: Rastreo (E7 · visual search / set-size slope)

### Diagnóstico rápido
Aquí empieza la zona de **medición falseada**. El generador modela búsqueda visual con set sizes variables para fitear una pendiente set-size, pero el render (`FocusSearchField`) muestra Ls entre Ts y **el runtime captura un solo tap de toda la pantalla** sin medir la búsqueda real. La pendiente RT vs set-size se mide, pero el RT incluye todo el bucle de la app, no el tiempo de escaneo.

### Mecanismo cognitivo
**Pretendido:** atención selectiva / búsqueda serial (pendiente set-size = ms/ítem). **Real:** el usuario ve un grid estático de letras y toca "presente/ausente". El proceso es búsqueda visual, sí, PERO: la pendiente set-size solo es interpretable si el RT mide *exclusivamente* el escaneo. Como el render es estático y el tap es global, el RT sí refleja búsqueda — **este es el menos roto del grupo dinámico**. **Contamina:** target ("L") y distractores ("T") comparten tamaño/color (bien, no hay pop-out), pero la disposición en grid `chunked(columns)` con `Arrangement.spacedBy(16.dp)` no controla excentricidad ni densidad → la pendiente mezcla set-size con área visual.

### Potencial de transferencia
**Muy baja.** La búsqueda L-entre-T es ultra-específica. La pendiente set-size es un marcador de eficiencia de búsqueda, no de "atención" general. Transferencia funcional: nula a baja.

### Problemas detectados
- `setSizeSlope` (l.309) exige `distinctSizes ≥ 2` y `MIN_N_SLOPE=6`. Pero `E7DifficultyMapping` genera solo 3 set sizes (`listOf(4, max/2, max)`) ciclados por índice → pocos puntos por tamaño, pendiente ruidosa.
- El render coloca exactamente **1** target cuando `present` (`searchLayout`), pero el set size visual ("L" + (n−1) "T") no garantiza que el grid tenga n ítems legibles en pantalla en sets grandes (32 ítems a 26sp pueden no caber). [Riesgo científico] el set-size efectivo ≠ set-size nominal en móviles.
- `targetPresentRate = 0.5` fijo: bien para balancear, pero la pendiente clásica se calcula sobre target-present; mezclar present/absent sin separar las dos pendientes (que difieren ×2) contamina.
- Un solo tap = no se mide dónde miró ni la búsqueda; el RT es la suma de escaneo + decisión + motor. Aceptable, pero no es un eye-tracking.

### Riesgos de falsa mejora
Aprendizaje de la disposición en grid y de la estrategia de escaneo de la app (no transferible). Banco infinito de posiciones pero patrón de layout muy repetitivo (grid). Riesgo medio de pseudoprogreso por familiaridad con el formato.

### Métricas recomendadas
- **Principal:** pendiente set-size (ms/ítem), **separada para present y absent**.
- Secundarias: intercepto (procesos no-de-búsqueda), precisión por set-size.
- **Métrica ACTUAL que engaña:** una pendiente única present+absent mezclada; y `ACCURACY` sube si el usuario adivina "presente" (50% base).

### Progresión recomendada
Más set sizes (5-6 niveles) para una pendiente fiable; gobernar por similitud target-distractor (T/L es fácil; usar conjunción de rasgos para forzar búsqueda serial real); separar pendientes present/absent.

### Rediseño propuesto
(1) ≥5 set sizes; (2) búsqueda de **conjunción** (color×forma) en vez de L-entre-T para que la pendiente refleje atención, no agudeza; (3) separar pendiente present/absent; (4) controlar densidad/excentricidad fijando el área. Mismo render-base, generador más rico. Testeable con simulador.

### Puntuación 0-10
1) 4 · 2) 3 · 3) 6 · 4) 5 · 5) 5 · 6) 5 · 7) 5 · 8) 3 · 9) 2 · 10) 7 · 11) 5 · 12) 5 · 13) 5 · 14) 5 · 15) 6
**Nota global: 4.6/10**
**Decisión final:** Rediseñar parcialmente (conjunción + pendientes separadas).

---

## Ejercicio: Dúo (E8 · dual-task)

### Diagnóstico rápido
**Medición rota.** El "coste de doble tarea" se calcula como precisión(single) − precisión(dual), pero la "segunda tarea" es un contador decorativo (`FocusDualTaskField`: un texto "Contador: N" que tickea cada 450 ms) que **el usuario no responde ni se puntúa**. No hay segunda tarea real. El coste medido es ruido, no carga dual.

### Mecanismo cognitivo
**Pretendido:** integración / reparto atencional bajo doble demanda. **Real:** un go/no-go single con un contador parpadeando en pantalla que el usuario *puede ignorar sin penalización*. El KDoc del propio render lo confiesa: "the secondary load is not separately scored". Sin respuesta ni puntuación de la tarea 2, NO hay doble tarea — hay una tarea con distractor visual. **Lo que contamina la medición:** todo. `DUAL_TASK_COST` (precisión single − dual) puede salir ≈0 o negativo aleatoriamente porque la tarea "dual" no impone carga real.

### Potencial de transferencia
**Nula** (como está). Una doble tarea real (PRP, dual N-back) tendría transferencia cercana media; esto no es una doble tarea.

### Problemas detectados
- `FocusDualTaskField` (l.40-56): el contador es un `LaunchedEffect` que incrementa un número; no se captura respuesta sobre él, no entra en `RecordedTrial`, no se puntúa. Es un adorno.
- `dualTaskCost` (`DefaultMetricsEngine` l.322): compara precisión single vs dual del **go/no-go primario**, asumiendo que la condición dual impone coste. Como no lo impone, la métrica mide ruido. [Riesgo científico crítico] métrica con nombre serio sobre una manipulación inexistente.
- El runtime de "una respuesta por trial" es estructuralmente incapaz de doble tarea (PRP necesita dos respuestas con SOA). El dominio colapsó dual-task a un flag booleano sin segunda respuesta.

### Riesgos de falsa mejora
El usuario "mejora" en dual-task cost por azar (la métrica fluctúa alrededor de 0). Cualquier número que se le muestre es **espurio**. Esto es el peor riesgo de pseudoprogreso del módulo: una métrica que no mide nada.

### Métricas recomendadas
- Real dual-task: **PRP slope** (RT2 vs SOA) o coste de precisión con DOS respuestas puntuadas.
- **Métrica ACTUAL que engaña:** `DUAL_TASK_COST` entero, hoy. No mostrar hasta que exista tarea 2.

### Progresión recomendada
Imposible sin rediseñar el runtime a dos respuestas. Gobernar por SOA y por similitud de las dos tareas.

### Rediseño propuesto
Requiere romper el bucle de una-respuesta-por-trial: (1) tarea 2 con respuesta propia capturada y puntuada (p. ej. tono agudo/grave → segunda tecla); (2) SOA variable entre T1 y T2; (3) medir PRP (RT2 crece al acortar SOA) y coste de precisión con ambas respuestas; (4) prioridad instruida. Es trabajo de runtime, no cosmético. **Hasta entonces, ocultar la métrica.**

### Puntuación 0-10
1) 2 · 2) 2 · 3) 4 · 4) 3 · 5) 2 · 6) 3 · 7) 4 · 8) 2 · 9) 2 · 10) 7 · 11) 2 · 12) 4 · 13) 3 · 14) 2 · 15) 9
**Nota global: 3.1/10**
**Decisión final:** Rediseñar completamente (runtime de dos respuestas) o Eliminar hasta tenerlo.

---

## Ejercicio: Enjambre (E9 · MOT)

### Diagnóstico rápido
**Medición rota por desacople estímulo↔medida.** El MOT real exige seguir objetos en movimiento; aquí el movimiento lo genera el composable (`FocusTrackingField`) a partir de `layoutSeed = stimulus.index`, mientras el "acierto" lo determina el generador de dominio (`probedIsTarget`) **que no conoce las trayectorias renderizadas**. El render incluso *elige el dot sondeado para que coincida con `probedIsTarget`* (`pickProbe`). El usuario sigue puntos, pero la corrección está predeterminada por el dominio, no por si realmente rastreó.

### Mecanismo cognitivo
**Pretendido:** atención selectiva sostenida espacial / index de objetos múltiples. **Real:** el dominio fija `targetIndices` y `probedIsTarget` deterministamente; el render anima dots orbitando (`dot.turns`, `cos/sin`) y resalta los targets al inicio (fase HIGHLIGHT 0-0.28) y sondea uno al final (PROBE >0.82). El problema: **el dot sondeado se elige por `pickProbe` para casar con el booleano del dominio**, y las trayectorias son órbitas independientes con `layoutSeed`. Si las órbitas se cruzan o no, el dominio ya decidió la respuesta correcta. La carga de tracking existe perceptualmente pero **no determina la métrica**: un usuario que pierde todos los targets y otro que los sigue perfectamente reciben la misma "respuesta correcta" esperada. Se mide si el usuario *toca* cuando `probedIsTarget`, no si *rastreó*.

### Potencial de transferencia
**Muy baja a nula** (como está). MOT bien hecho tiene de las mejores transferencias del módulo (capacidad atencional espacial, usado en deporte). Pero esta implementación no mide tracking, así que su transferencia efectiva es nula.

### Problemas detectados
- `FocusTrackingField` (l.82-93): trayectorias = órbitas circulares con fase aleatoria por `layoutSeed`. Los dots no se "mezclan" de forma que el tracking importe; orbitan separados. El tracking visual es débil.
- `pickProbe` (l.96): el dot sondeado se selecciona POST-HOC para que `(it in targetIndices) == probedIsTarget`. La respuesta correcta está cocinada por el dominio antes de cualquier movimiento. [Riesgo científico crítico] la métrica no es función del rastreo del usuario.
- `ACCURACY` es la `governingMetric` (catálogo): mide aciertos a una pregunta cuyo correcto está predeterminado, no capacidad de tracking.
- `speed` (1.0→3.0) afecta a `dot.turns` (render) pero el dominio no usa la velocidad para nada medible.
- El movimiento real (cruces, oclusiones, identidad de objetos idénticos) — la esencia del MOT — no está modelado.

### Riesgos de falsa mejora
**Altísimo.** El usuario puede acertar el ~50% adivinando (probe binario) sin rastrear nada. Como el correcto es independiente del tracking, la "precisión" es casi azar + sesgo de respuesta. Cualquier mejora reportada es pseudoprogreso.

### Métricas recomendadas
- MOT real: **% de targets correctamente identificados** sobre tracking genuino, con velocidad/nº targets como dificultad, midiendo el *threshold de velocidad* a un % fijo.
- **Métrica ACTUAL que engaña:** `ACCURACY` entera — hoy no mide tracking.

### Progresión recomendada
Imposible medir bien sin que el dominio conozca el render (o el render alimente la respuesta). Gobernar por velocidad y nº de targets con trayectorias que crucen.

### Rediseño propuesto
(1) El render debe ser la fuente de verdad del tracking, o el dominio simular trayectorias con cruces reales; (2) al final, sondear **todos** los targets (o varios) y puntuar cuántos identificó, no un binario; (3) trayectorias con cruces/rebotes para que perder un objeto sea posible; (4) medir umbral de velocidad. Trabajo de arquitectura (acoplar render↔scoring). **Hasta entonces, la métrica es ficticia.**

### Puntuación 0-10
1) 2 · 2) 2 · 3) 3 · 4) 4 · 5) 2 · 6) 3 · 7) 3 · 8) 2 · 9) 2 · 10) 7 · 11) 1 · 12) 5 · 13) 3 · 14) 2 · 15) 9
**Nota global: 3.0/10**
**Decisión final:** Rediseñar completamente o Eliminar hasta que el scoring dependa del tracking.

---

## Ejercicio: Flujo (E10 · RSVP / attentional blink)

### Diagnóstico rápido
**Medición rota.** El RSVP/attentional blink mide si detectas T2 poco después de T1 (dos juicios separados). Aquí el render (`FocusStreamField`) lanza un stream a 80 ms/ítem con T1 y T2 en color, y el runtime captura **un único tap** para todo el stream. No se mide T1 ni T2 por separado → el attentional blink (el fenómeno entero) es **imposible de medir**. El generador calcula `t1Index`, `t2Index` y `lag`, pero nada de eso entra en el scoring.

### Mecanismo cognitivo
**Pretendido:** atención temporal / recuperación atencional entre estímulos (attentional blink). **Real:** ver letras pasar rápido y tocar "vi una de color". Un solo tap = un solo juicio. El blink (déficit de detección de T2 en lag 2-4) requiere reportar T1 Y T2 independientemente. **Todo lo que define el paradigma (lag T1-T2, detección selectiva de T2) está en el generador pero no en la medición.** `isTarget=true` siempre (l.50), `correctResponseKey=PRIMARY` → cualquier tap dentro de ventana es HIT.

### Potencial de transferencia
**Nula** (como está). El AB es un marcador teórico fino; sin medir T2|T1 no hay nada. Transferencia funcional inexistente.

### Problemas detectados
- `E10FlujoGenerator` (l.44-57): computa `lag`, `t1`, `t2` con cuidado (régimen de blink), pero `isTarget=true` y un único `correctResponseKey` → el assembler clasifica con `classify` (tap-or-withhold) sobre "hay target": trivialmente HIT si toca.
- `FocusStreamField` muestra T1="X" y T2="O" en color, pero el usuario solo da un tap global; no se sabe si detectó T1, T2, ambos o ninguno. [Riesgo científico crítico] el constructo medido (¿detectó algo de color?) no es el constructo nombrado (attentional blink / recuperación temporal).
- `ACCURACY` governing: mide "tocó cuando había stream con target", ≈100% trivial.
- 80 ms/ítem es razonable, pero sin captura por-target la velocidad no produce métrica interpretable.

### Riesgos de falsa mejora
**Altísimo.** Precisión cercana al 100% por diseño (siempre hay target, un tap basta). El número sube y no significa nada. Pseudoprogreso garantizado.

### Métricas recomendadas
- AB real: **p(T2 correcto | T1 correcto) en función del lag**, con dos respuestas (identificar T1 y T2 entre opciones).
- **Métrica ACTUAL que engaña:** `ACCURACY` (trivialmente alta).

### Progresión recomendada
Imposible sin dos juicios. Gobernar por lag (forzar lag 2-4 para el blink) y por velocidad del stream.

### Rediseño propuesto
(1) Tras el stream, pedir **identificación** de T1 y T2 (p. ej. ¿qué letra/color era cada target?), dos respuestas puntuadas; (2) medir p(T2|T1) por lag y mostrar la curva del blink; (3) gobernar por lag y velocidad. Trabajo de runtime (respuesta diferida post-stream con opciones). **Hasta entonces, métrica ficticia.**

### Puntuación 0-10
1) 2 · 2) 1 · 3) 3 · 4) 3 · 5) 1 · 6) 3 · 7) 3 · 8) 2 · 9) 2 · 10) 7 · 11) 1 · 12) 5 · 13) 3 · 14) 2 · 15) 9
**Nota global: 2.9/10**
**Decisión final:** Rediseñar completamente (dos juicios post-stream) o Eliminar.

---

## Ejercicio: Ancla (E11 · distracción digital)

### Diagnóstico rápido
Concepto válido y medición razonable: tarea principal LEFT/RIGHT con intruso tipo notificación que ignorar; `DISTRACTION_RESISTANCE_INDEX` = precisión(con intruso)/precisión(baseline). Funciona porque la tarea principal SÍ se responde y puntúa. El intruso, sin embargo, es estáticamente visual y fácil de ignorar — la distracción es débil.

### Mecanismo cognitivo
Resistencia a la distracción / mantenimiento de meta ante saliencia exógena. **Produce adaptación:** `intruderProportion` (0.20→0.55). **Contamina:** el intruso (`FocusAnchorField`: un banner "Notificación nueva — ignórala") es texto estático, no compite por la respuesta ni cambia con la tarea → captura de atención mínima. **Sobra:** la saliencia declarada (`DISTRACTOR_SALIENCE`) apenas se manifiesta visualmente.

### Potencial de transferencia
**Baja-media.** La resistencia a distracción digital es ecológicamente relevante (el constructo más "vida real" del módulo en concepto). Pero un banner que se ignora trivialmente no captura la distracción real (que es *engagement* con el contenido, no solo verlo). Transferencia funcional: prometedora en concepto, débil en ejecución.

### Problemas detectados
- El intruso no impone coste medible: es un banner estático arriba, la flecha sigue clara abajo. La caída de precisión con intruso será mínima → `DISTRACTION_RESISTANCE_INDEX` ≈ 1.0 casi siempre, poco discriminativo.
- `coerceIn(0.0, 1.0)` en `distractionResistance` (l.340): si el intruso *ayudara* por azar (precisión intruso > baseline), se trunca a 1.0 — enmascara ruido.
- Sin captura de si el usuario *atendió* al intruso (no hay coste de re-orientación medido). Solo precisión global.

### Riesgos de falsa mejora
Medio. El índice está cerca del techo (1.0) → poca varianza, "mejora" difícil de distinguir de ruido. Banco infinito. El riesgo es que el ejercicio sea demasiado fácil (techo) y reporte resistencia alta espuria.

### Métricas recomendadas
- **Principal:** índice de resistencia (precisión y RT bajo intrusión vs baseline). Añadir el **coste en RT** (no solo precisión).
- Secundarias: coste de re-enganche tras intruso saliente, tasa de "captura" si el intruso exige una micro-decisión.
- **Métrica ACTUAL que engaña:** el índice truncado a [0,1] cerca de 1.0.

### Progresión recomendada
Subir la saliencia y la demanda del intruso (intrusos que se mueven, parpadean, o exigen un no-go) para sacar el índice del techo; gobernar por saliencia real.

### Rediseño propuesto
(1) Intrusos dinámicos/multimodales que de verdad compitan (movimiento, sonido, o un no-go embebido); (2) medir coste en RT además de precisión; (3) coste de re-enganche en el trial post-intruso; (4) destruncar el índice o usar diferencia con signo. Mismo render-base, intruso más potente. Superior porque genera varianza medible.

### Puntuación 0-10
1) 4 · 2) 5 · 3) 6 · 4) 5 · 5) 5 · 6) 5 · 7) 6 · 8) 4 · 9) 4 · 10) 8 · 11) 5 · 12) 6 · 13) 6 · 14) 5 · 15) 6
**Nota global: 5.3/10**
**Decisión final:** Rediseñar parcialmente (intruso con carga real + coste RT).

---

## Ejercicio: Re-enganche (E12 · post-error)

### Diagnóstico rápido
Intención valiosa (medir recuperación post-error), pero el mecanismo está roto: el ejercicio dice "provocar errores", marca `afterProvokedError`, pero **no provoca nada** — `afterProvokedError` solo es un flag que ni cambia el estímulo ni se usa en el scoring. La recuperación post-error se mide sobre errores *naturales* del usuario, igual que en cualquier otro ejercicio.

### Mecanismo cognitivo
**Pretendido:** control metacognitivo / re-enganche tras fallar (post-error slowing/accuracy). **Real:** es un PVT con `signalRate=0.35` y un flag `afterProvokedError` que el render ignora y el scoring no usa (`POST_ERROR_ACCURACY`/`POST_ERROR_SLOWING` se computan en `addPostErrorMetrics` sobre errores reales de cualquier ejercicio, no sobre los "provocados"). **Todo `provokedErrorRate` y `afterProvokedError` es código muerto funcional.** No hay manipulación que fuerce errores.

### Potencial de transferencia
**Muy baja.** El post-error slowing es un marcador automático, difícil de entrenar y de transferir. Y aquí ni siquiera se manipula. Funcional a la vida (recuperarse de un fallo sin espiral): bonito en copy, sin soporte mecánico.

### Problemas detectados
- `E12ReengancheGenerator` (l.40-41): `previousWasProvoked = random.nextDouble() < provokedErrorRate` decide `afterError`, pero NO altera el estímulo para inducir un error (no acorta ventana, no mete trampa). El "error provocado" no existe; el flag marca trials aleatorios.
- `afterProvokedError` no entra en `RecordedTrial` ni en `TrialMarkers` (l.131-137: `Reengagement → TrialMarkers.NONE`) → el dominio lo descarta. Código muerto.
- `POST_ERROR_ACCURACY` (governing) se calcula sobre transiciones error→trial reales (`addPostErrorMetrics`), idéntico a como se calcularía en E1/E3. E12 no aporta nada que E1 no tenga.
- Recuperación post-error necesita densidad de errores; con un PVT fácil hay pocos errores naturales → `MIN_N_POST_ERROR=3` raramente se alcanza → métrica casi siempre no-confiable.

### Riesgos de falsa mejora
La métrica suele salir "datos insuficientes" (pocos errores). Cuando sale, es ruido sobre 3 eventos. Riesgo de mostrar un número de "recuperación" basado en 3 trials.

### Métricas recomendadas
- **Principal:** post-error accuracy y post-error slowing, pero sobre errores **realmente inducidos**.
- Secundarias: tiempo de re-enganche (RT del primer trial post-error vs línea base).
- **Métrica ACTUAL que engaña:** ambas, por baja N y por falta de inducción.

### Progresión recomendada
Inducir errores de verdad (ventanas imposibles intermitentes, trampas perceptuales, deadlines ocasionales) para generar densidad de error suficiente; gobernar por densidad de error inducido.

### Rediseño propuesto
(1) Inducir errores reales: trials trampa con ventana ultra-corta o conflicto fuerte cada N; (2) propagar `afterProvokedError` a `TrialMarkers` y puntuar específicamente la recuperación tras esos trials; (3) medir tiempo de re-enganche; (4) garantizar N de errores ≥10. Sin inducción, E12 = E1 con otro nombre → fusionar.

### Puntuación 0-10
1) 3 · 2) 3 · 3) 4 · 4) 4 · 5) 4 · 6) 5 · 7) 5 · 8) 5 · 9) 4 · 10) 7 · 11) 4 · 12) 5 · 13) 5 · 14) 3 · 15) 7
**Nota global: 4.2/10**
**Decisión final:** Rediseñar parcialmente (inducción real de errores) o Fusionar con E1.

---

## Ejercicio: Presión (E13 · deadline)

### Diagnóstico rápido
Concepto sólido y medición honesta: misma tarea de elección con/sin deadline, `PRESSURE_DELTA` = precisión(calma) − precisión(deadline). El deadline se aplica de verdad como ventana de respuesta (`effectiveWindowMs`, l.292). Es el mejor del grupo "integración" porque la manipulación (deadline) sí afecta la respuesta y se mide limpiamente.

### Mecanismo cognitivo
Precisión bajo presión temporal / trade-off velocidad-precisión controlado. **Produce adaptación:** `pressureProportion` y `deadlineMs` (800→450). El deadline real recorta la ventana en trials presión (`effectiveWindowMs` usa `deadlineMs` como window) → fuerza el trade-off. **Contamina:** la tarea base es la misma flecha LEFT/RIGHT trivial de E5/E11 → el techo de la tarea base es alto, así que el delta refleja sobre todo el deadline. **Bien resuelto.**

### Potencial de transferencia
**Baja-media.** Rendir bajo presión es ecológicamente relevante. Pero la "tarea" es una discriminación trivial; la presión sobre una tarea fácil no es la presión de la vida (que es sobre tareas complejas). Transferencia funcional: limitada.

### Problemas detectados
- Tarea base demasiado fácil (flecha izquierda/derecha): en calma la precisión es ~techo, así que `PRESSURE_DELTA` solo captura el colapso por deadline, no la interacción presión×dificultad de tarea.
- `deadlineMs` se usa como `responseWindowMs` efectivo, pero el assembler clasifica con `responseWindowMs` original del estímulo (l.69, 77) — el comentario (l.291-299) dice que deadline ≤ window mantiene consistencia, pero un tap entre `deadlineMs` y `responseWindowMs` en trial presión: la UI lo corta (`awaitResponse(effectiveWindowMs)`), así que no llega — OK, consistente, pero frágil y dependiente de que UI y assembler no diverjan.
- Sin medir el **shift de la curva velocidad-precisión** (lo correcto bajo presión sería ver si el usuario sacrifica precisión proporcionalmente o colapsa). Solo un delta de precisión.

### Riesgos de falsa mejora
Medio. El usuario puede "mejorar" el delta volviéndose más rápido en general (menos colapso) sin mejorar control bajo presión. Banco infinito. Tarea fácil = techo en calma = delta dominado por el deadline.

### Métricas recomendadas
- **Principal:** delta de precisión calma−presión. Correcto, pero añadir **delta de RT** y la **función velocidad-precisión**.
- Secundarias: tasa de anticipaciones bajo deadline (precipitación), estabilidad bajo presión (RT-CV presión vs calma).
- **Métrica ACTUAL que engaña:** `PRESSURE_DELTA` solo de precisión sobre tarea fácil.

### Progresión recomendada
Subir la dificultad de la tarea base (conflicto, no flecha trivial) para que la presión tenga sobre qué morder; gobernar por tightness del deadline y por dificultad de tarea conjuntamente.

### Rediseño propuesto
(1) Tarea base con conflicto real (reusar flanker/Stroop bajo deadline) para que presión×dificultad interactúen; (2) medir SAT (speed-accuracy tradeoff) explícito, no solo delta de precisión; (3) penalizar anticipaciones bajo presión; (4) reportar estabilidad bajo presión. Mejor porque mide control del trade-off, no solo colapso.

### Puntuación 0-10
1) 5 · 2) 5 · 3) 7 · 4) 6 · 5) 7 · 6) 6 · 7) 6 · 8) 5 · 9) 5 · 10) 6 · 11) 6 · 12) 6 · 13) 6 · 14) 6 · 15) 5
**Nota global: 5.7/10**
**Decisión final:** Mantener con cambios menores (tarea base con conflicto + SAT).

---

## Veredicto del módulo "Enfoque (atención / control ejecutivo)"

### Capacidades bien cubiertas / huecos / ausentes
- **Bien cubiertas (medición válida):** vigilancia sostenida (E1), inhibición reactiva (E4, el mejor), conflicto de respuesta (E2), flexibilidad (E6, con reservas de banco finito), precisión bajo presión (E13). Estos cinco tienen cadena estímulo→respuesta→métrica intacta.
- **Cubiertas pero mal medidas / falseadas:** búsqueda visual (E7, pendiente contaminada), distracción (E11, techo), inhibición prepotente (E3, sin SDT), Stroop (E5, es Simon mal etiquetado), post-error (E12, sin inducción).
- **AUSENTES de medición real pese a aparecer:** doble tarea (E8 = contador decorativo), MOT (E9 = respuesta predeterminada, no tracking), attentional blink (E10 = un solo tap, sin T2|T1). Estos tres son **paradigmas en el nombre, no en la medición**.
- **Capacidad cognitiva genuinamente AUSENTE del módulo:** **memoria de trabajo** (no hay N-back ni span; la "atención ejecutiva" sin WM cojea — WM es el predictor con más transferencia documentada) y **alerta fásica/orientación** (red de Posner: alerting/orienting/executive — solo se toca executive).

### Ejercicios redundantes / a fusionar
- **E3 (go/no-go) + E4 (stop-signal):** misma capacidad (RESPONSE_INHIBITION, módulo C). E4 es estrictamente superior. Fusionar el rol o diferenciar E3 como "detección de infrecuencia/SDT".
- **E2 (flanker) + E5 ("Stroop"=Simon):** mismo mecanismo (conflicto E-R espacial/perceptual). E5 no añade capacidad distinta hasta que sea un Stroop real (interferencia semántica). Fusionar o rehacer E5.
- **E1 (PVT) + E12 (re-enganche, paradigma PVT):** E12 sin inducción de error es un E1 con otro nombre. Fusionar o dar a E12 inducción real.

### Riesgos sistémicos
1. **Runtime de "una respuesta por trial" impuesto a paradigmas que necesitan ≥2 respuestas o medición temporal (E8/E9/E10).** Es el fallo arquitectónico raíz: tres ejercicios reportan métricas (`DUAL_TASK_COST`, `ACCURACY`, attentional blink) que **no son función de la conducta del usuario**. Cualquier "mejora" ahí es pseudoprogreso puro. Riesgo científico y reputacional máximo.
2. **Render desacoplado del scoring** en E8/E9/E10/E7: el `layoutSeed = stimulus.index` y las animaciones intra-composable significan que lo que el usuario VE no es lo que el dominio PUNTÚA. Mata la validez de constructo.
3. **N mínimos demasiado bajos** (`MIN_N_CONGRUENCY=3`, `MIN_N_POST_ERROR=3`, 1 evento garantizado de no-go/stop): métricas diferenciales sobre 3 trials por celda son ruido con barra de error gigante. El sistema es honesto con su `confident` flag (excelente), pero los umbrales son optimistas.
4. **Índices compuestos inventados presentados como medida** (`SUSTAINED_FOCUS_INDEX`, `FocusScore`): fórmulas ad-hoc no validadas. El código es honesto en el `honestNote`, pero un número 0-100 etiquetado "foco" se leerá como capacidad.
5. **Transfer/RCI correctamente gateado** (`ProbeReliabilityEstimator` vacío → nunca afirma "cambio fiable"): esto es **lo más honesto del módulo** y debe preservarse a toda costa. Pero los probes solo cubren E1/E2/E3 (`ProbeExerciseMapping`) → la transferencia de E4-E13 ni siquiera es medible. El módulo entrena 13 cosas y solo puede *evaluar transferencia* de 3.
6. **Etiquetas científicas falsas:** E5 dice STROOP y es SIMON; E8/E9/E10 nombran paradigmas que no implementan. En un producto que promete "no pseudo-neurociencia", esto es deuda de credibilidad.

### Las 3 cosas que más elevarían el poder cognitivo del módulo
1. **Arreglar o retirar E8/E9/E10 antes que cualquier otra cosa.** O se rediseña el runtime para dos respuestas / medición temporal real (acoplando render→scoring), o se ocultan sus métricas. Hoy son tres generadores de números espurios bajo nombres prestigiosos; restan más credibilidad de la que suman. Es la corrección de mayor impacto: convierte 3 mediciones falsas en honestas (o en ausentes honestas).
2. **Añadir memoria de trabajo** (N-back adaptativo) como ejercicio nuevo y como construct de transferencia. Es la capacidad con más evidencia de transferencia, y su ausencia es el mayor hueco. Encaja en el runtime de respuesta discreta sin romper la arquitectura.
3. **Subir el rigor de medición de los ejercicios que SÍ funcionan (E1-E4):** SDT (d′/criterio) en E3, descontar latencia de input y excluir burn-in del staircase en E4, delta plots / adaptación de conflicto en E2/E5, y elevar los N mínimos por celda a ≥10-15. Y **ampliar `ProbeExerciseMapping`** para poder medir transferencia de más capacidades, porque sin probes la única evidencia es mejora-en-la-tarea, que el propio sistema (con razón) declara insuficiente.

> Nota final de honestidad del propio sistema: el módulo es, en su capa de *reporte*, sorprendentemente honesto (gating por `confident`, RCI bloqueado, `honestNote` que niega transferencia lejana). El problema no es que mienta sobre lo que mide — es que **mide cosas que no miden lo que dicen su nombre** en E8/E9/E10 (y parcialmente E5/E7/E12). Arreglar la validez de constructo, no la honestidad del copy, es la prioridad.

---


## Modulo: Velocidad Mental

Confirmed: there is no timeout in the `Stimulus` phase. The deadline only converts a *late tap* into "incorrect"; if the user never taps, the trial never resolves (no omission channel). This is a critical design finding. I now have a complete, code-grounded picture. Producing the audit.

## Ejercicio: Destello (E1)

### Diagnóstico rápido
Es un test de tiempo de reacción simple (TR/SRT) puro: aparece un destello, tocas, se mide el RT. La medición es metodológicamente seria (`TimebasePolicy`, foreperiodo variable 800-2200 ms, anticipación <100 ms). Como **medición** vale; como **entrenamiento cognitivo** no entrena prácticamente nada — el SRT es de los rasgos más estables y menos entrenables que existen.

### Mecanismo cognitivo
Mide latencia sensoriomotora pura: detección de un onset visual + ejecución de un toque. No hay carga de memoria de trabajo, ni selección, ni inhibición, ni decisión. En `FixedTrialEngine.onResponse` (`TrialEngine.kt:68-101`) cualquier toque en fase `Stimulus` es `isCorrect = true` (`SpeedSessionStateHolder.kt:189`, rama `?: true`): no hay nada que clasificar salvo anticipación. El foreperiodo aleatorio (`SpeedSessionStateHolder.kt:357-361`) es lo único científicamente correcto: impide que el usuario cronometre el estímulo, forzando reacción genuina y no anticipación. **Lo que sobra cognitivamente:** todo lo que no sea capturar el primer toque tras el destello. **Lo que produce "adaptación":** nada — E1 corre con `staircase = null`, `blockCount = 1`, dificultad fija (`SpeedExerciseScreen.kt:86,98-99`). Es un instrumento de medición disfrazado de ejercicio.

### Potencial de transferencia
**Nula.** El SRT es altamente heredable y resistente al entrenamiento; mejorar el SRT en la app no transfiere a velocidad de procesamiento en tareas reales (leer, conducir, decidir). Cualquier mejora observada será mejora-en-la-tarea (aprendizaje del ritmo del foreperiodo + warm-up motor), no transferencia cercana ni lejana. Para que tuviera transferencia tendría que dejar de ser SRT puro y añadir discriminación/elección — pero entonces deja de ser E1 y se convierte en E6.

### Problemas detectados
- **No hay canal de omisión.** En fase `Stimulus` no existe timeout: el `LaunchedEffect` solo pone delays en `Foreperiod` y `Feedback` (`SpeedExerciseScreen.kt:109-121`). Si el usuario no toca, el trial **nunca se resuelve**. `MetricEdgeCasePolicy.omissionRate` (`MetricEdgeCasePolicy.kt:69-70`) existe pero `omissions` siempre se pasa como 0 (`SpeedSessionStateHolder.kt:208` no pasa omisiones; el `assembleRun` tampoco). El comentario del modelo dice "diseño de respuesta obligatoria" — pero "obligatoria" aquí significa "la app se cuelga esperando", no "se registra la omisión".
- **`isCorrect = true` siempre** (`SpeedSessionStateHolder.kt:189`): la `pc` de E1 es trivialmente 1.0 (descontando anticipaciones). Eso hace que `pc` no aporte información en E1 y que cualquier métrica derivada de `pc` (VCE, score control) esté inflada por construcción.
- **El SRT del dispositivo se deriva del primer bloque del usuario** (`SpeedSessionStateHolder.kt:312-317`): `simpleReactionMedianMs` mezcla latencia de hardware con latencia humana. Es un proxy contaminado: confundes la lentitud del teléfono con la lentitud de la persona.
- **3 toques mínimos para mediana de lapsos** (`TrialEngine.kt:114`) sobre 12 trials/bloque (`SpeedSessionStateHolder.kt:334`): muestra pequeña, mediana móvil inestable al inicio del bloque.

### Riesgos de falsa mejora
- **Warm-up motor confundido con mejora cognitiva:** las primeras corridas siempre serán más lentas; la "mejora" entre sesión 1 y 3 es práctica de la mecánica, no velocidad cognitiva.
- **Inflación de `pc`:** al ser siempre 1.0, el dashboard de E1 (si se mostrara) marcaría control alto sin que el usuario haya controlado nada.
- **El feedback muestra el RT en ms** (`SpeedExerciseScreen.kt:313`): el usuario perseguirá el número, fomentando anticipación (toques antes del destello) que el sistema sí penaliza — bien — pero el incentivo a "batir el número" empuja al borde del corte de 100 ms.

### Métricas recomendadas
- **Principal:** RT mediano de aciertos (ya está, `rtMedianNanos`) — correcto usar mediana, no media.
- **Secundarias clave que faltan o no se exponen:** **RT-CV intra-bloque** (consistencia; `cv` ya se calcula pero E1 no lo muestra) — es más informativo que el RT medio para velocidad de procesamiento; **tasa de anticipación** (ya está) como índice de impulsividad; **omisiones reales** (hoy inexistentes).
- **Métrica ACTUAL que engaña:** `pc` de E1 (siempre 1.0) y por tanto la barra "control" y la `VCE` cuando se calculan en E1 — no miden control, miden que la mecánica marca todo correcto.
- **Falta:** mejora longitudinal con banda de confianza (el RT varía ±30-50 ms entre días por sueño/cafeína/hora; sin intervalos, cualquier comparación sesión-a-sesión es ruido).

### Progresión recomendada
E1 no debe "progresar" como entrenamiento: es un **calentamiento/baseline**. Lo correcto es fijarlo a un número estable de trials (≥20 para que la mediana sea fiable) y usarlo para anclar el SRT que después se resta de los TR de elección (E6/E3/E7) para aislar el tiempo de decisión. No tiene zona óptima de desafío porque no hay desafío.

### Rediseño propuesto
Convertir E1 en **baseline/calentamiento explícito**, no en ejercicio puntuable:
- **Mecánica:** 20-24 destellos, foreperiodo variable (ya está), añadir **timeout de omisión** (p. ej. 1500 ms ⇒ omisión registrada, no cuelgue).
- **Salida:** SRT mediano + RT-CV con intervalo de confianza por bootstrap; **este SRT se inyecta como constante de corrección sensoriomotora** en E6/E3/E7 (decision time = RT_total − SRT). Eso es lo que lo hace superior: deja de medir lo mismo dos veces y empieza a *limpiar* la medición de los otros ejercicios.
- **Feedback:** ocultar el ms por trial (incentiva anticipación); mostrar solo el resumen al final con banda de variabilidad.
- **Cómo testearlo:** el `SyntheticUserRunner` ya permite perfiles; añadir un perfil "omisor" para verificar que el timeout produce omisiones y no cuelgues.
- **Por qué es superior:** convierte una medición redundante en un instrumento de calibración que aumenta la validez de los ejercicios reales.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **1** | 2) Transferencia a la vida real: **0** | 3) Claridad del mecanismo: **9** | 4) Calidad de la progresión: **1** (no hay) | 5) Calidad de la medición: **7** (sólida salvo omisiones) | 6) Personalización: **1** | 7) Resistencia al efecto práctica: **3** | 8) Valor metacognitivo: **2** | 9) Valor emocional: **2** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **9** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **3** | 12) Adherencia: **3** | 13) Escalabilidad a perfiles distintos: **3** | 14) Robustez científica: **6** | 15) Prioridad de mejora: **6**
**Nota global del ejercicio: 4.0/10**
**Decisión final:** Convertir en calentamiento / baseline (no es un entrenamiento; es un instrumento de calibración que debe alimentar a los demás).

---

## Ejercicio: Cifrado (E3)

### Diagnóstico rápido
Se vende como "digit-symbol / cifrado" (el paradigma clásico de velocidad de procesamiento tipo DSST/SDMT) pero **el código NO implementa digit-symbol**: implementa una verificación binaria "¿este símbolo mapea a este valor? sí/no" con una clave de **4 entradas fijas** visible en pantalla todo el rato (`SpeedPromptGenerators.kt:36-67`, `SpeedExerciseScreen.kt:269-278`). Es una tarea de comparación/matching de 2 alternativas, no el DSST. La etiqueta promete un mecanismo que el código no tiene.

### Mecanismo cognitivo
Lo que realmente entrena: **velocidad de búsqueda en una tabla de asociación símbolo→valor + comparación + decisión sí/no**. Hay un componente legítimo de velocidad de procesamiento (consultar la clave, comparar) y algo de memoria de trabajo si memorizas la clave en vez de releerla. PERO: la clave está **siempre visible** (`KeyRow`, `SpeedExerciseScreen.kt:235`), así que el componente de memoria de trabajo es opcional y, con solo **4 pares**, el usuario los memoriza en los primeros trials y la tarea colapsa a "reconocimiento de 4 asociaciones sobrelearned". El generador elige el "valor incorrecto" de entre los otros 3 valores de la misma clave (`wrongValue`, `SpeedPromptGenerators.kt:59-62`): el banco de estímulos posibles es minúsculo (4 símbolos × {match | 3 no-matches} = 16 combinaciones). **Lo que produce adaptación:** el `MultiSignalStaircaseEngine` aprieta el deadline según `pc` (`SpeedExerciseScreen.kt:99`). **Lo que sobra/contamina:** la naturaleza binaria + clave persistente + banco de 4 elimina casi todo el coste de búsqueda que justifica el paradigma DSST.

### Potencial de transferencia
**Muy baja.** El DSST real (clave grande, no memorizable en una sesión, presión de hojear) tiene cierta validez como índice de velocidad de procesamiento. Esta versión, con clave de 4 y banco finito, se aprende como patrón en minutos ⇒ la mejora es memorización del banco, no velocidad de procesamiento. Sin un banco grande/rotativo y sin retirar la clave de la vista, no hay transferencia ni cercana.

### Problemas detectados
- **Banco finito y diminuto:** `DEFAULT_KEY` = 4 pares (`SpeedPromptGenerators.kt:65`). 16 estados de estímulo totales. Memorización casi inmediata.
- **Clave siempre visible** (`SpeedExerciseScreen.kt:235, 269-278`): anula el componente de memoria de trabajo que diferenciaría esto de un simple matching.
- **El "valor incorrecto" se toma de los valores de la propia clave** (`wrongValue`, `:59-62`): los distractores no escalan en dificultad (un "3" frente a un "4" es tan fácil como frente a un "1").
- **Mecánica no especificada en el plan** (lo admite el propio KDoc, `SpeedPromptGenerators.kt:6-11`): es una "interpretación razonable", no un paradigma validado. Se etiquetó "Cifrado/digit-symbol" sin implementar el paradigma que ese nombre evoca.
- **El deadline tardío NO genera omisión:** igual que E1, si no tocas no pasa nada (no hay timeout en `Stimulus`). El deadline solo convierte un toque tardío en incorrecto (`SpeedSessionStateHolder.kt:191-193`). Un usuario que duda indefinidamente cuelga el trial.
- **`pc` mezcla velocidad y exactitud bajo deadline:** una respuesta correcta pero tardía cuenta como incorrecta (`:193`), así que `pc` baja por lentitud, no por error de juicio. El staircase reacciona a esa `pc` mezclada ⇒ no distingue "se equivoca" de "es lento".

### Riesgos de falsa mejora
- **Memorización del banco de 4** = la mayor fuente de pseudoprogreso de todo el módulo. La curva de "mejora" será casi enteramente aprendizaje del set finito.
- **Dificultad falsa por deadline:** apretar el deadline (`MultiSignalStaircaseEngine`) hace la tarea "más difícil" en velocidad, pero como el contenido es trivial y memorizado, lo que sube es la presión temporal, no la carga cognitiva real — velocidad disfrazada de habilidad.
- **`pc` inflada:** con 4 pares sobrelearned, `pc` tiende a 1.0 salvo por toques tardíos; el score "control" = `(1 − ImpIdx)·pc` (`SpeedScoreEngine.kt:91`) se infla.

### Métricas recomendadas
- **Principal:** throughput correcto por segundo (aciertos / tiempo), no `pc` sola — captura el trade-off velocidad-exactitud, que es la esencia del paradigma.
- **Secundarias:** decision time = RT − SRT(de E1); RT-CV; tasa de error real (no contaminada por deadline).
- **Métrica ACTUAL que engaña:** la `pc` bajo deadline (mezcla lentitud con error) y el RT mediano cuando el contenido ya está memorizado (mide velocidad motora, no procesamiento).
- **Falta:** separar "error de juicio" (tocó el lado equivocado) de "tardío" (tocó bien pero tarde). Hoy ambos colapsan a `isCorrect = false`.

### Progresión recomendada
- Sustituir el banco de 4 por una **clave de 6-9 pares rotativa entre sesiones**, con clave **parcialmente oculta** (mostrar 2-3 segundos al inicio del bloque, luego retirar) para introducir carga de memoria de trabajo genuina y graduable.
- Progresión en **dos dimensiones independientes:** tamaño de la clave (carga MT) y deadline (presión temporal), no solo deadline.
- Zona óptima: mantener `pc` real (juicio) en 80-90% ajustando tamaño de clave, y mover el deadline para empujar velocidad sin destruir exactitud.

### Rediseño propuesto
**Digit-symbol real adaptativo:**
- **Mecánica:** clave de N pares símbolo→dígito; estímulo = un símbolo; el usuario teclea/elige el dígito correcto (no binario sí/no). Para mantener la superficie de 2 vías actual sería un retroceso; lo correcto es un teclado de 3-4 opciones.
- **Banco:** símbolos generados procedimentalmente (rotación/variación), clave nueva cada sesión ⇒ imposible memorizar entre sesiones.
- **Carga MT graduable:** clave visible → semi-visible → oculta, según `pc` de juicio.
- **Métrica:** throughput (símbolos correctos/min) + decision time corregido por SRT.
- **Adaptación:** dos staircases (tamaño de clave por exactitud de juicio; deadline por velocidad), no uno que mezcla todo en `pc`.
- **Cómo testearlo:** perfil sintético que memoriza vs perfil que no; verificar que el throughput del que memoriza se aplana entre sesiones (señal de banco bien diseñado) solo si el banco rota.
- **Por qué es superior:** recupera el mecanismo que el nombre promete, elimina la memorización trivial y separa velocidad de exactitud.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **3** | 2) Transferencia a la vida real: **2** | 3) Claridad del mecanismo: **4** (nombre engaña) | 4) Calidad de la progresión: **4** | 5) Calidad de la medición: **4** | 6) Personalización: **5** | 7) Resistencia al efecto práctica: **1** (banco de 4) | 8) Valor metacognitivo: **2** | 9) Valor emocional: **3** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **7** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **2** | 12) Adherencia: **4** | 13) Escalabilidad a perfiles distintos: **4** | 14) Robustez científica: **3** | 15) Prioridad de mejora: **8**
**Nota global del ejercicio: 3.4/10**
**Decisión final:** Rediseñar completamente (implementar digit-symbol real con banco rotativo y carga de MT graduable; el actual es matching binario con banco de 4).

---

## Ejercicio: Multivía (E6)

### Diagnóstico rápido
Es un **tiempo de reacción de elección de 2 alternativas (2-CRT)** con la regla más simple posible: una flecha apunta al lado correcto, tocas ese lado (`SpeedPromptGenerators.kt:17-29`). Es el ejercicio más limpio del módulo en cuanto a mecanismo y el único con adaptación bien cerrada (4 bloques + staircase multi-señal). Pero la regla es casi nula: "toca donde apunta la flecha" no impone selección ni mapeo estímulo-respuesta no trivial — es 2-CRT compatible espacialmente (efecto Simon a favor del usuario), el caso más fácil de la familia.

### Mecanismo cognitivo
Entrena **velocidad de elección + mapeo estímulo-respuesta compatible**. Hay un componente real de velocidad de procesamiento (el 2-CRT mide algo más que el SRT: añade discriminación + selección de respuesta). La adaptación funciona: `MultiSignalStaircaseEngine` aprieta el deadline por `pc`, con puertas de impulsividad (`impIdx`), inestabilidad (`cv`) y fatiga (`fatIdx`) (`StaircaseEngine.kt:201-214`). El cierre del lazo pc→deadline→pc está bien pensado (`SpeedSessionStateHolder.kt:191-193`, comentario `:68-73`). **Lo que produce adaptación cognitiva:** la presión temporal creciente sobre la selección de respuesta. **Lo que sobra:** la compatibilidad espacial (flecha→lado) hace la tarea demasiado fácil; no hay conflicto que resolver. **Lo que NO contamina** (a su favor): el contenido es generado, no un banco finito, así que no hay memorización de patrón.

### Potencial de transferencia
**Baja.** El 2-CRT compatible es entrenable pero la mejora es en gran parte específica de la tarea (aprendizaje del mapeo flecha→lado). Hay literatura mixta sobre transferencia de velocidad de elección a velocidad funcional; con regla compatible, la transferencia se acerca a la de E1. Subiría a **media** si se introdujera incompatibilidad estímulo-respuesta (flecha→lado opuesto, mapeos arbitrarios), lo que ya hace E7 — con lo que E6 y E7 se solapan (ver veredicto).

### Problemas detectados
- **Regla trivial / compatibilidad espacial:** flecha apunta al lado correcto (`SpeedPromptGenerators.kt:24`). Sin conflicto ⇒ poco coste de selección. Es casi un SRT con dos botones.
- **Sin canal de omisión** (mismo defecto sistémico): no hay timeout en `Stimulus`; el deadline solo penaliza el toque tardío (`SpeedSessionStateHolder.kt:191-193`). Un no-toque cuelga el trial.
- **`pc` mezcla error de selección con lentitud** (toque correcto pero tardío = incorrecto, `:193`): el staircase no distingue "eligió mal" de "fue lento". Para velocidad de procesamiento eso puede ser aceptable (el deadline ES la medida), pero entonces el `faRate`/error de juicio se pierde.
- **Solo 12 trials/bloque** (`SpeedSessionStateHolder.kt:334`): para estimar `cv` y `impIdx` con MAD (que necesita ≥4 valores tras filtrar, `SpeedRtStatistics.kt:56`) es muestra muy justa; las puertas de `cv`/`impIdx` del staircase operan sobre estimadores ruidosos.
- **`MultiviaGenerator` ignora su nombre "Multivía":** son 2 vías, no multi. El nombre vuelve a prometer más de lo que hay.

### Riesgos de falsa mejora
- **Aprendizaje del mapeo flecha→lado** = mejora-en-la-tarea; rápido y casi total en las primeras corridas.
- **Dificultad falsa por deadline:** el staircase sube presión temporal; el usuario "mejora" porque tolera deadlines más cortos en una regla trivial, no porque procese más rápido en general.
- **Estimadores ruidosos con n=12:** `cv` e `impIdx` pueden disparar puertas STABILIZE/LOWER por azar muestral, haciendo la dificultad errática y dando una sensación de progreso/regreso que es ruido.

### Métricas recomendadas
- **Principal:** RT de elección corregido por SRT (RT_E6 − SRT_E1) — aísla el tiempo de *decisión*, que es lo que E6 añade sobre E1. Hoy no se hace.
- **Secundarias:** `cv` (consistencia), `impIdx` (ya está), exactitud de juicio **separada** de tardanza.
- **Métrica ACTUAL que engaña:** RT mediano sin restar SRT (confunde latencia motora con tiempo de decisión); `pc` que colapsa error+tardanza.
- **Falta:** medir el **coste de elección** = RT_E6 − RT_E1 como índice limpio de velocidad de procesamiento de decisión.

### Progresión recomendada
- Primera dimensión: deadline (ya está, bien).
- Segunda dimensión que falta: **complejidad del mapeo** — empezar compatible (flecha→lado), escalar a 3-4 alternativas y a mapeos incompatibles. Eso evita el techo trivial.
- Subir trials/bloque a ≥20 para estabilizar `cv`/`impIdx` antes de que gobiernen el staircase.

### Rediseño propuesto
**2/4-CRT con mapeo escalable:**
- **Mecánica:** N alternativas (2→4) con mapeo configurable (compatible → arbitrario). Mantiene el lazo adaptativo actual.
- **Progresión bidimensional:** nº de alternativas (Hick: RT crece con log2(N)) + deadline.
- **Métrica:** pendiente de Hick (RT vs log2 N) como índice de velocidad de procesamiento robusto y poco "practicable"; decision time corregido por SRT.
- **Omisión:** timeout en `Stimulus` que registre omisión real.
- **Cómo testearlo:** perfiles sintéticos a distintas N; verificar que RT crece ~log2(N) (validez de constructo del paradigma de Hick).
- **Por qué es superior:** rompe el techo trivial, introduce un índice (pendiente de Hick) que resiste el efecto práctica mucho mejor que un RT absoluto, y deja de solaparse con E7 al ocupar el eje "número de alternativas" mientras E7 ocupa "conflicto de regla".

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **3** | 2) Transferencia a la vida real: **2** | 3) Claridad del mecanismo: **8** | 4) Calidad de la progresión: **5** | 5) Calidad de la medición: **6** | 6) Personalización: **6** (staircase multi-señal) | 7) Resistencia al efecto práctica: **3** | 8) Valor metacognitivo: **2** | 9) Valor emocional: **3** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **7** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **4** | 12) Adherencia: **5** | 13) Escalabilidad a perfiles distintos: **6** | 14) Robustez científica: **6** | 15) Prioridad de mejora: **6**
**Nota global del ejercicio: 4.7/10**
**Decisión final:** Redisañar parcialmente (es el mejor del módulo; añadir eje de nº de alternativas/incompatibilidad y corrección por SRT lo eleva a entrenamiento real).

---

## Ejercicio: Compuerta (E7)

### Diagnóstico rápido
Es el único ejercicio con **conflicto cognitivo real**: una flecha + una puerta; si la puerta está abierta tocas la dirección de la flecha, si está cerrada tocas la **opuesta** (`SpeedPromptGenerators.kt:73-89`). Eso es un paradigma de **inversión de regla condicional** (familia Simon/Stroop espacial), que sí impone control inhibitorio y mapeo no compatible. Es el ejercicio más valioso del módulo conceptualmente — pero está implementado de forma demasiado simple (binario, señal textual "puerta abierta/cerrada") y arrastra los mismos defectos sistémicos de medición.

### Mecanismo cognitivo
Entrena **control inhibitorio + flexibilidad de regla bajo presión temporal**: en los trials de puerta cerrada hay que suprimir la respuesta prepotente (seguir la flecha) e invertirla. Esto añade un componente que NINGÚN otro ejercicio del módulo tiene y que es genuinamente relevante para "velocidad de procesamiento bajo demanda de control". El staircase adaptativo (igual que E6) aprieta deadline + puertas de impulsividad/CV/fatiga. **Lo que produce adaptación cognitiva:** la presión temporal sobre la inversión de regla — apretar el deadline en trials incongruentes es genuinamente exigente. **Lo que sobra/contamina:** la señal es **texto** ("puerta abierta"/"puerta cerrada", `SpeedPromptGenerators.kt:81`), no un estímulo perceptivo; leer texto introduce coste de lectura que contamina el RT y varía con la fluidez lectora del usuario. Además, al ser binario y la puerta ~50/50, el coste de cambio (switch cost) **no se mide separadamente** de la congruencia.

### Potencial de transferencia
**Baja-media** (la más alta del módulo). Los paradigmas de inhibición/conflicto tienen evidencia de transferencia algo mejor que el SRT/CRT puro, aunque sigue siendo discutida y mayormente cercana. Subiría a **media** si: (a) la señal fuera perceptiva (no texto), (b) se midiera el efecto de congruencia (RT incongruente − RT congruente) como índice de control, y (c) la proporción de trials incongruentes fuera manipulable. Transferencia funcional a la vida real (frenar una respuesta automática) es plausible pero no demostrada aquí.

### Problemas detectados
- **Señal de regla en texto** (`SpeedPromptGenerators.kt:81`): "puerta abierta/cerrada" obliga a leer; el RT mezcla lectura + decisión + inversión. Contaminación grave para un ejercicio de velocidad. Debería ser un icono perceptivo (puerta abierta/cerrada dibujada).
- **No se mide el efecto de congruencia ni el switch cost:** el generador marca correcto/incorrecto pero el `MetricCalculator` agrega `pc`/RT sobre TODOS los trials sin separar congruentes (puerta abierta) de incongruentes (puerta cerrada). El índice de control real (RT_incongruente − RT_congruente) se pierde. El `SpeedTrialPrompt` ni siquiera transporta un flag de congruencia (`SpeedPrompt.kt:15-20`).
- **Banco binario** (`gateOpen = nextBoolean`, arrow = `nextBoolean`): 4 estados (flecha L/R × puerta abierta/cerrada). Más diverso que E3 pero aún memorizable como 4 reglas.
- **Mismo defecto de omisión y de `pc` mezclada** que E6 (toque tardío = incorrecto, sin timeout/omisión real, `SpeedSessionStateHolder.kt:191-193`).
- **El error de inversión y el error por lentitud colapsan** en `isCorrect = false`: no puedes distinguir "no supo invertir la regla" (fallo de control) de "supo pero fue lento" (fallo de velocidad) — justo la distinción que da valor a este ejercicio.

### Riesgos de falsa mejora
- **Aprendizaje de las 4 reglas** ⇒ los trials incongruentes dejan de ser conflictivos con la práctica; el efecto de congruencia se reduce no por mejor control sino por automatización del mapeo invertido. Sin medir el efecto de congruencia, esto se leerá como "mejora" cuando es automatización.
- **Coste de lectura del texto:** usuarios lectores rápidos parecerán "más rápidos cognitivamente"; es un sesgo de medición, no habilidad.
- **`pc` inflada y dificultad falsa por deadline:** idéntico a E6.

### Métricas recomendadas
- **Principal (la que falta y es la razón de ser del ejercicio):** **efecto de congruencia** = RT_mediano(incongruente) − RT_mediano(congruente), y **error de inversión** = exactitud en trials incongruentes. Esa es la métrica de control inhibitorio.
- **Secundarias:** RT corregido por SRT; `impIdx` (ya está); proporción de errores rápidos en incongruentes (impulsividad específica del conflicto).
- **Métrica ACTUAL que engaña:** `pc` global y RT global agregados sobre congruentes+incongruentes (`MetricCalculator.computeCore` no segrega): borran exactamente la señal de control que el ejercicio existe para medir.
- **Falta todo lo de congruencia/switch**: el modelo de datos (`TrialRecord`, `SpeedTrialPrompt`) ni transporta la condición de congruencia.

### Progresión recomendada
- Dimensiones independientes: deadline + **proporción de trials incongruentes** (más incongruentes = más demanda de control) + complejidad de la señal de regla.
- Zona óptima: mantener exactitud de inversión en 80-90% subiendo proporción de incongruentes y deadline; evitar que el banco de 4 reglas se automatice introduciendo más reglas/señales.

### Rediseño propuesto
**Tarea de conflicto perceptivo con métrica de congruencia:**
- **Mecánica:** señal de regla **perceptiva** (puerta dibujada abierta/cerrada, o color), no texto. Flag de congruencia en `SpeedTrialPrompt`. Proporción de incongruentes manipulable (p. ej. 30/50/70%).
- **Adaptación:** staircase que distingue exactitud de inversión (control) de velocidad (deadline) — dos señales, no `pc` colapsada.
- **Métrica:** efecto de congruencia (ms) + tasa de error de inversión, ambos con corrección por SRT y banda de confianza.
- **Banco:** rotar reglas/señales para evitar automatización del set de 4.
- **Cómo testearlo:** perfil sintético con "control bajo" (más errores y RT más alto en incongruentes) vs "control alto"; verificar que el efecto de congruencia los separa y que el staircase reacciona a la exactitud de inversión, no solo a la velocidad.
- **Por qué es superior:** convierte el único ejercicio con conflicto real en el único con una **métrica de control inhibitorio limpia**, elimina la contaminación por lectura y bloquea la falsa mejora por automatización.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **4** | 2) Transferencia a la vida real: **3** | 3) Claridad del mecanismo: **7** | 4) Calidad de la progresión: **4** | 5) Calidad de la medición: **3** (no segrega congruencia, RT contaminado por texto) | 6) Personalización: **6** | 7) Resistencia al efecto práctica: **3** | 8) Valor metacognitivo: **3** | 9) Valor emocional: **4** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **6** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **3** | 12) Adherencia: **5** | 13) Escalabilidad a perfiles distintos: **6** | 14) Robustez científica: **4** | 15) Prioridad de mejora: **9**
**Nota global del ejercicio: 4.5/10**
**Decisión final:** Redisañar parcialmente (es el de mayor techo del módulo; señal perceptiva + métrica de congruencia/inversión + segregación congruente/incongruente lo convierten en el verdadero ejercicio de control bajo presión).

---

## Veredicto del módulo "Velocidad Mental (velocidad de procesamiento)"

### Qué es realmente este módulo
Es una **batería de medición psicométrica de tiempo de reacción** (SRT, 2-CRT, matching, conflicto) con una capa adaptativa decente, vestida de "entrenamiento". La ingeniería de medición es de calidad poco común para una app (timebase monotónica unificada `TimebasePolicy`, separación hardware/conducta en `SessionQualityEvaluator`, manejo explícito de nulos en `MetricEdgeCasePolicy`, versionado de umbrales, simulador sintético, score rotulado honestamente como `preliminary`). Eso merece reconocimiento. Pero como **entrenamiento cognitivo**, los cuatro ejercicios miden y entrenan casi la misma capacidad estrecha (latencia de respuesta) con techos de práctica bajos y transferencia entre nula y baja-media.

### Capacidades bien cubiertas / huecos / ausentes
- **Bien cubierto:** tiempo de reacción simple (E1) y de elección (E6); medición temporal rigurosa; control de calidad técnica de la señal.
- **Huecos:** velocidad de procesamiento *sostenida* (vigilancia, no hay tarea larga tipo PVT/TOVA); velocidad de *barrido visual / búsqueda* (E3 lo simula mal con banco de 4); flexibilidad/switching medido como tal.
- **Ausentes por completo:** velocidad de procesamiento con **carga perceptiva creciente** (búsqueda visual con N distractores), **switch cost** explícito, **velocidad de acceso semántico/léxico** (naming speed), y cualquier **corrección por SRT** que aísle el tiempo de decisión del motor. Sin esa corrección, todos los RT del módulo mezclan latencia sensoriomotora con procesamiento — el defecto de validez más grave a nivel de batería.

### Ejercicios redundantes o a fusionar
- **E1 y E6 se solapan:** E6 es E1 + una elección trivial. E1 debe degradarse a **baseline/calentamiento** que alimenta el SRT de corrección a los demás (deja de competir como "ejercicio").
- **E6 y E7 se solapan en el eje de "regla":** E6 (compatible) y E7 (incompatible) son los dos extremos del mismo continuo. Lo limpio es **fusionarlos en un único ejercicio de 2-CRT con eje de compatibilidad/conflicto adaptativo** (compatible → incompatible → proporción de incongruentes), midiendo el efecto de congruencia. Hoy son dos pantallas para un solo constructo.
- **E3 está mal posicionado:** es el único que *intenta* velocidad de barrido/asociación, pero el banco de 4 lo arruina. O se rediseña a digit-symbol real (banco rotativo, clave graduable) o se elimina.

### Riesgos sistémicos del módulo
1. **Medición común sin corrección por SRT:** todos los ejercicios reportan RT bruto; ninguno resta la latencia motora basal. Resultado: imposible separar "procesa rápido" de "tiene dedos rápidos / buen teléfono". Atraviesa los 4 ejercicios.
2. **`pc` colapsa error de juicio y tardanza** (`SpeedSessionStateHolder.kt:193`): el toque correcto-pero-tardío = incorrecto. Para un staircase de velocidad puede pasar, pero destruye la capacidad de distinguir fallos de exactitud de fallos de velocidad — y de eso depende todo diagnóstico real (E3/E7).
3. **Sin canal de omisión real:** no hay timeout en fase `Stimulus` en ningún ejercicio (`SpeedExerciseScreen.kt:109-126`); `omissionRate` se calcula pero siempre recibe 0. Un usuario que no toca cuelga el trial. La métrica de omisión (clave para vigilancia/lapsos) es código muerto.
4. **Bancos de contenido finitos y pequeños** (E3: 4 pares; E7: 4 reglas): memorización ⇒ pseudoprogreso. E1/E6 se salvan por generación, pero su techo de práctica es bajo igual.
5. **Muestra de 12 trials/bloque** para estimadores (`cv`, `impIdx`, MAD que exige ≥4): las puertas multi-señal del staircase operan sobre estadística ruidosa, generando adaptación errática que el usuario percibe como progreso/regreso aleatorio.
6. **Pseudoprogreso por presión temporal:** el staircase aprieta el deadline; "mejorar" = tolerar deadlines más cortos en tareas triviales. Velocidad disfrazada de habilidad. Bien rotulado como `preliminary`, pero el riesgo conductual persiste.
7. **Nombres que prometen más que el código:** "Cifrado/digit-symbol" (es matching binario), "Multivía" (son 2 vías). Riesgo de claim científico no respaldado por la mecánica.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Corrección por SRT en toda la batería (la palanca #1).** Usar E1 como baseline y reportar **decision time = RT − SRT** en E3/E6/E7. Convierte 4 medidas de "latencia total" en medidas de *procesamiento* real, aislando lo entrenable de lo que es hardware + motricidad. Es barato (el SRT ya se calcula, `SpeedSessionStateHolder.kt:312-317`) y multiplica la validez.
2. **Separar exactitud-de-juicio de tardanza, y medir el efecto de congruencia (E7) / throughput (E3).** Dejar de colapsar todo en `pc`. Añadir flags de congruencia/condición al `SpeedTrialPrompt`/`TrialRecord` y segregar las métricas. Eso da, por fin, índices de **control inhibitorio** (E7: RT_incongruente − RT_congruente) y de **velocidad de procesamiento con carga** (E3: throughput) que resisten el efecto práctica mucho mejor que un RT absoluto.
3. **Bancos generativos/rotativos + segunda dimensión de dificultad cognitiva (no solo deadline).** E3: clave grande rotativa y graduable (carga de MT). E6/E7 fusionados: eje de nº de alternativas (Hick) y de proporción de incongruentes. Esto rompe la memorización (pseudoprogreso) y sube la presión sobre la *carga cognitiva*, no solo sobre el cronómetro — que es la única forma de que "velocidad de procesamiento" signifique algo entrenable y no "practicar el ritmo de un foreperiodo".

---


## Modulo: Logica (razonamiento)

I now have everything needed. I've confirmed the critical architecture: the content provider returns a fixed 6-item list with hardcoded `difficultyLevel`s, the `DifficultyDecision` output is never fed back to select harder content (only stored as a `recommendation` string and per-skill theta), and the operator-precedence in the penalty calc (line 49-50) has a real bug. Let me produce the full audit.

## Ejercicio: Deducción desde premisas (`logic-e1-audit-01`, `DEDUCTION_FROM_PREMISES`)

### Diagnóstico rápido
Es un ítem único de modus ponens con superficie "software", respondido con un formulario de 5 campos (respuesta + premisas + regla + justificación + confianza). El contenido lógico es correcto y el andamiaje metacognitivo es serio, pero es UN SOLO ítem fijo: no es un ejercicio, es un reactivo. Vale como diagnóstico de calibración, no como entrenamiento.

### Mecanismo cognitivo
Entrena razonamiento deductivo necesario (separar lo que se sigue de lo que "suena razonable") + control inhibitorio sobre la sobreinferencia (distractor `OVERINFERENCE` "todo archivo del equipo azul fue auditado") + metacognición vía confianza explícita. El acoplamiento respuesta↔premisas↔regla↔justificación es el activo real: obliga a explicitar la estructura, no a adivinar. **Lo que sobra/contamina:** el campo de premisas se puntúa por Jaccard (`overlapScore`, `ReasoningScorer.kt:79-85`) — un usuario que marca TODAS las premisas obtiene overlap parcial gratis, y como hay solo 3 premisas el espacio de adivinación es trivial. **Solo práctica de la tarea:** con un único ítem, repetir = memorizar que "a" es correcta.

### Potencial de transferencia
**Muy baja.** Con un ítem fijo, lo que se entrena es ESTE ítem. El andamiaje (regla + justificación) apunta a transferencia cercana, pero sin un banco que varíe la estructura superficial manteniendo la lógica, no hay nada que transferir. Para subirlo: ≥20 variantes de superficie por estructura `modus_ponens_two_premises`, e introducir formas inválidas (afirmación del consecuente como ítem completo, no solo como distractor) para que el usuario tenga que DECIDIR si algo se sigue, no solo elegir entre opciones precocidas.

### Problemas detectados
- `DefaultLogicContentProvider.sessionItems()` (`LogicContentProvider.kt:29-36`) devuelve una lista FIJA de 6 ítems hardcodeados. No hay generador, no hay aleatorización, no hay variación. Banco finito = 6.
- `difficultyLevel = FOUNDATION` está hardcodeado (`:45`) y NUNCA se usa para seleccionar contenido. El adapter calcula una decisión de dificultad pero el `startSession()` (`LogicStateHolder.kt:53-68`) ignora la dificultad: siempre sirve los mismos 6 ítems. **La adaptación es decorativa.**
- `overlapScore` (Jaccard) premia marcar premisas de más con crédito parcial; `requiredPremiseIds=["p1","p2"]` con p3 presente como ruido es el único filtro, demasiado fácil.

### Riesgos de falsa mejora
- **Memorización de banco finito (crítico):** 6 ítems, mismas posiciones, mismas respuestas correctas. En 2-3 sesiones el usuario memoriza "a/b/c/a/c/c" sin razonar. El ICR subirá; el razonamiento no.
- **Puntuación inflada:** la justificación estructurada se elige de 2 opciones (`claimChoices`, `LogicRootScreen.kt:361`) → 50% de acierto por azar en ese componente (15% del peso).
- **Pseudoprogreso por confianza:** acertar + marcar confianza alta da `confidence.probability=0.9` directo al score; el usuario aprende a marcar "alta" siempre que acierta.

### Métricas recomendadas
- **Principal:** precisión inferencial en ítems NO vistos de la misma estructura lógica (transferencia cercana), no ICR agregado.
- **Secundarias:** tasa de sobreinferencia (selección del distractor `OVERINFERENCE`) a lo largo del tiempo; consistencia premisa-correcta vs respuesta-correcta (¿acierta la respuesta pero falla las premisas? = adivinó).
- **Calibración metacognitiva:** Brier score real sobre confianza, no el sustituto actual `confidence.probability` que solo recompensa confianza alta-en-acierto.
- **Engañan hoy:** `averageIcr` (mezcla 6 dimensiones heterogéneas en un número), `inferentialAccuracy` sobre 6 ítems fijos (n minúsculo + reuso = ruido).

### Progresión recomendada
Niveles reales: 2 premisas → 3-4 premisas con ruido → cadenas de 2 pasos (modus ponens encadenado) → premisas con cuantificadores ("algunos/todos/ninguno"). La curva debe venir de la estructura lógica, no de un enum `FOUNDATION..ELITE` que no toca el contenido.

### Rediseño propuesto
Generador paramétrico de modus ponens: plantilla `∀x (A(x)→B(x)); A(k); ∴ B(k)` con (1) slots de superficie intercambiables (software/biología/legal/cotidiano) de un pool de ≥30, (2) inyección controlada de ítems inválidos (afirmación del consecuente, premisa faltante) donde la respuesta correcta es "no se sigue", (3) dificultad = nº de premisas + presencia de cuantificador + nº de pasos. Métrica: precisión en holdout de estructura nunca entrenada. Feedback: ya es bueno; mantener. Superior porque elimina el banco finito y mide deducción, no recuerdo.

### Puntuación 0-10 (los 15 ejes)
1) 4 · 2) 3 · 3) 8 · 4) 2 · 5) 5 · 6) 2 · 7) 1 · 8) 7 · 9) 5 · 10) 8 · 11) 2 · 12) 5 · 13) 3 · 14) 6 · 15) 9
**Nota global del ejercicio: 4.2/10**
**Decisión final:** Redisenar parcialmente (convertir el ítem fijo en generador paramétrico; el andamiaje vale, el banco no).

---

## Ejercicio: Prueba condicional / refutación (`logic-e3-access-01`, `CONDITIONAL_TEST`)

### Diagnóstico rápido
Versión miniatura de la tarea de selección de Wason ("¿qué caso refuta P→Q?"). Cognitivamente es la pieza MÁS valiosa del módulo: ataca directamente el sesgo de confirmación. Pero de nuevo es UN ítem fijo con 4 opciones, y la respuesta correcta ("b") es memorizable en un intento.

### Mecanismo cognitivo
Entrena el corazón del razonamiento condicional: entender que P→Q solo se refuta con P∧¬Q, e inhibir la tentación de buscar confirmación (distractor `CONFIRMATION_BIAS` "tarjeta activada y luz verde"). Esto es control inhibitorio + razonamiento condicional, los dos procesos con mayor valor funcional del módulo. La mecánica está perfectamente alineada con el objetivo. **Contamina:** los 4 distractores cubren los 4 cuadrantes de Wason de forma tan limpia que, una vez visto el patrón, el ítem es trivial. **Sobra:** nada en la estructura; el problema es la cardinalidad (1 ítem).

### Potencial de transferencia
**Baja → media (potencial alta).** Wason es la tarea con literatura de transferencia más conocida: el rendimiento abstracto NO transfiere bien a contextos de permiso social, pero entrenar la regla formal explícita + múltiples superficies SÍ mejora la detección de P∧¬Q en dominios nuevos. El isomorfo de biblioteca (`logic-e3-library-iso-01`) es el inicio correcto. Para subirlo a alta: rotar contextos deónticos (permiso/obligación) vs indicativos (causa), porque el sesgo varía con el tipo de regla.

### Problemas detectados
- Ítem fijo único (`LogicContentProvider.kt:88-140`). Banco = 1 por estructura entrenada + 1 isomorfo.
- `validReasoningRules = [CONDITIONAL_FALSIFICATION, MODUS_TOLLENS]` (`:128`): aceptar MODUS_TOLLENS como "regla válida" para refutar por contraejemplo P∧¬Q es laxo — el usuario puede elegir la regla equivocada conceptualmente y aún puntuar 1.0 en `ruleApplicationScore` (`ReasoningScorer.kt:30`). Eso contamina la medición del componente "regla".
- La justificación se elige entre 2 opciones (`RULE_IS_REFUTED` vs `CONCLUSION_DOES_NOT_FOLLOW`, `LogicRootScreen.kt:366`): 50% azar.

### Riesgos de falsa mejora
- **Efecto repetición severo:** un ítem de Wason visto 3 veces ya no mide razonamiento, mide memoria de "la activada-y-apagada".
- **Dificultad falsa:** `DEVELOPING` hardcodeado; no escala.
- **Crédito de regla laxo:** dos reglas aceptadas como correctas diluyen la señal de comprensión.

### Métricas recomendadas
- **Principal:** tasa de selección de la carta ¬Q-relevante (P∧¬Q) en ítems nuevos; clásico índice de Wason.
- **Secundarias:** tasa de selección del caso confirmatorio (sesgo de confirmación) en el tiempo — debe bajar.
- **Engañan hoy:** `ruleApplicationScore` (acepta 2 reglas), `transferConsistency` binaria sobre 1 probe.

### Progresión recomendada
Reglas indicativas → deónticas → con negación en antecedente ("si NO está activada...") → biconcionales ("solo si"). La negación embebida es el escalón duro real.

### Rediseño propuesto
Banco generativo de Wason: plantilla P→Q con 4 cartas, ≥40 contextos, etiquetando cada uno como deóntico/indicativo. Forzar elegir TODAS las cartas que hay que voltear (no una opción precocida), que es la tarea de Wason real y elimina el azar de 1-de-4. Endurecer `validReasoningRules` a la regla única correcta por ítem. Métrica: índice-Wason en holdout deóntico vs indicativo. Superior porque mide el constructo canónico con la respuesta canónica, sin azar de opción múltiple.

### Puntuación 0-10
1) 5 · 2) 5 · 3) 9 · 4) 2 · 5) 5 · 6) 2 · 7) 1 · 8) 6 · 9) 4 · 10) 8 · 11) 2 · 12) 5 · 13) 4 · 14) 7 · 15) 10
**Nota global del ejercicio: 4.8/10**
**Decisión final:** Redisenar parcialmente. Es la mejor mecánica del módulo; merece ser un generador completo de Wason y el ancla del módulo.

---

## Ejercicio: Necesario vs suficiente (`logic-e4-publish-01`, `NECESSARY_SUFFICIENT`)

### Diagnóstico rápido
Clasificar una condición como necesaria-pero-no-suficiente. Concepto valioso y mal entendido por la mayoría. Pero el prompt YA REGALA la respuesta: dice literalmente "es necesario verificar el email. Verificarlo no garantiza la publicación" — el usuario solo tiene que copiar esa frase a la opción "a". No mide razonamiento, mide lectura.

### Mecanismo cognitivo
Pretende entrenar la distinción necesario/suficiente (razonamiento condicional, base de causalidad y diagnóstico). **Problema crítico de diseño:** el enunciado verbaliza la conclusión correcta antes de preguntar (`LogicContentProvider.kt:153` el prompt afirma ambas mitades de la respuesta). El proceso cognitivo entrenado es emparejamiento texto-opción, no clasificación lógica. La mecánica NO está alineada con el objetivo: la inferencia ya está hecha en el prompt.

### Potencial de transferencia
**Nula → muy baja** tal como está. El concepto necesario/suficiente tiene transferencia funcional alta (diagnóstico médico, depuración, derecho), pero ESTE ítem no la entrena porque no exige inferir nada. Para tener transferencia: dar premisas y exigir que el usuario DERIVE si la condición es necesaria, suficiente, ambas o ninguna — sin que el texto lo diga.

### Problemas detectados
- **Filtración de respuesta en el prompt** (`:153`): el enunciado contiene la respuesta verbatim. Fallo de diseño grave de medición.
- Ítem fijo único.
- 4 opciones cubren las 4 combinaciones (nec∧suf, etc.) de forma demasiado limpia → memorizable.
- `requiredPremiseIds=["p1","p2"]` donde p2 ("puede haber otros requisitos") es prácticamente un parafraseo de la respuesta.

### Riesgos de falsa mejora
- **Pseudoprogreso máximo:** acierto por copia textual, no por razonamiento. ICR alto, comprensión cero.
- Memorización trivial.

### Métricas recomendadas
- **Principal:** precisión en la matriz 2×2 (necesaria sí/no × suficiente sí/no) en ítems donde el texto NO revela la clasificación.
- **Engañan hoy:** absolutamente todo el score de este ítem, porque la respuesta está filtrada.

### Progresión recomendada
Dar tabla de casos (se publicó / no se publicó, con/sin email) y exigir inferir la relación — estilo "datos → clasificación". Escalar a 2 condiciones interactuando (A necesaria, B suficiente).

### Rediseño propuesto
Presentar SOLO premisas observacionales ("Hubo casos con email verificado que no se publicaron; nunca se publicó sin email") y preguntar la clasificación, sin nombrarla en el enunciado. Banco generativo con la matriz 2×2 balanceada. Métrica: precisión por celda de la matriz. Superior porque obliga a INFERIR la relación en vez de reconocerla escrita.

### Puntuación 0-10
1) 3 · 2) 2 · 3) 6 · 4) 2 · 5) 2 · 6) 2 · 7) 1 · 8) 4 · 9) 3 · 10) 8 · 11) 1 · 12) 4 · 13) 3 · 14) 4 · 15) 10
**Nota global del ejercicio: 3.1/10**
**Decisión final:** Redisenar completamente. La filtración de respuesta en el prompt lo invalida como medición.

---

## Ejercicio: Detección de contradicción / control de falso positivo (`logic-e5-reservation-01`, `CONTRADICTION_DETECTION`)

### Diagnóstico rápido
Detectar si un conjunto de premisas es contradictorio, con la respuesta correcta = "NO hay contradicción" (control de falso positivo). Buena idea: penaliza la sobredetección de contradicciones. Pero solo hay UN ítem y la respuesta correcta es siempre "consistente", lo que enseña la heurística equivocada: "responde siempre consistente".

### Mecanismo cognitivo
Entrena verificación de consistencia lógica + inhibición del falso positivo (marcar contradicción donde solo hay información distinta; distractores `FALSE_CONTRADICTION`). Esto es razonamiento + control inhibitorio + atención a la diferencia entre incompatibilidad y mera distinción. La idea del "control de falso positivo" es excelente y rara. **Contamina:** que el único ítem tenga respuesta "consistente" sesga el aprendizaje hacia esa respuesta; sin ítems con contradicción REAL mezclados, el usuario aprende a decir "no" siempre.

### Potencial de transferencia
**Baja (potencial media).** Detectar consistencia transfiere a evaluación de argumentos, testimonios, requisitos contradictorios. Pero requiere ítems balanceados (50% consistentes / 50% inconsistentes) y variar el tipo de inconsistencia (directa, transitiva, vía cuantificadores). Con 1 ítem fijo, transferencia nula.

### Problemas detectados
- Ítem único con respuesta "consistente" → riesgo de sesgo de respuesta (`LogicContentProvider.kt:193-236`).
- No hay ningún ítem con contradicción real en el pack (verificable: ningún `correctAnswerId` apunta a "sí hay contradicción"). El control de falso positivo sin su par positivo es metodológicamente cojo.
- `expectedJustificationClaim=SET_IS_CONSISTENT` con solo 2 opciones de claim → 50% azar.

### Riesgos de falsa mejora
- **Sesgo de respuesta:** "siempre consistente" gana el 100% en este banco. Pseudoprogreso puro.
- Memorización de 1 ítem.

### Métricas recomendadas
- **Principal:** d' (sensibilidad de detección de señal) separando aciertos de falsas alarmas — necesita ítems positivos y negativos.
- **Secundaria:** tasa de falsa alarma (marcar contradicción inexistente) en el tiempo.
- **Engañan hoy:** accuracy en un banco con base-rate 100% "consistente".

### Progresión recomendada
Consistencia directa → inconsistencia transitiva (A>B, B>C, C>A) → inconsistencia vía cuantificadores → conjuntos grandes (5-6 premisas) donde hay que rastrear la cadena.

### Rediseño propuesto
Generador con base-rate balanceada 50/50, tipos de inconsistencia rotados, tamaño de conjunto como dificultad. Reportar d' y criterio (β) para separar habilidad de sesgo de respuesta. Superior porque sin el par positivo/negativo el "control de falso positivo" no es medible.

### Puntuación 0-10
1) 4 · 2) 3 · 3) 7 · 4) 2 · 5) 3 · 6) 2 · 7) 1 · 8) 5 · 9) 4 · 10) 8 · 11) 2 · 12) 5 · 13) 3 · 14) 5 · 15) 9
**Nota global del ejercicio: 3.9/10**
**Decisión final:** Redisenar parcialmente (urgente: añadir ítems con contradicción real y balancear base-rate).

---

## Ejercicio: Construcción de contraejemplo (`logic-e23-chef-01`, `COUNTEREXAMPLE`)

### Diagnóstico rápido
Invalidar un argumento (afirmación del consecuente: "todos los chefs son precisos; Marta es precisa; ∴ Marta es chef") eligiendo el caso que mantiene premisas verdaderas y conclusión falsa. Conceptualmente de los más altos del módulo. Pero "construcción de contraejemplo" es un nombre inflado: el usuario ELIGE entre 3 contraejemplos prefabricados, no construye nada.

### Mecanismo cognitivo
Entrena la habilidad de refutar por contraejemplo: razonamiento contrafactual + comprensión de validez (premisas verdaderas + conclusión falsa = inválido). El modelo `CounterexampleCase(premisesRemainTrue, conclusionBecomesFalse)` y `CounterexampleValidator` (`ReasoningScorer.kt:105-108`) es una pieza de diseño sólida y honesta. **Lo que sobra:** la etiqueta "construcción" — es reconocimiento, no generación. **Contamina:** los distractores son tan distintos (uno destruye una premisa, otro confirma) que la discriminación es fácil una vez visto.

### Potencial de transferencia
**Baja → media (potencial alta).** Refutar por contraejemplo es una de las habilidades de razonamiento con mayor transferencia funcional a la vida real (evaluar argumentos, detectar generalizaciones falsas, debate). El límite es el formato: elegir un contraejemplo entrena reconocer, no generar. La transferencia real requiere PRODUCIR contraejemplos.

### Problemas detectados
- Ítem único (`LogicContentProvider.kt:238-290`).
- `CounterexampleValidator.invalidates` existe pero NO se usa en el scoring (`ReasoningScorer.kt` puntúa por `isCorrect` del option, no valida la lógica del case seleccionado). El validador es código muerto en el camino de puntuación; solo lo toca un test (`ReasoningScorerTest.kt:78-83`). [Riesgo científico] la estructura semántica del contraejemplo no se evalúa, solo el id elegido.
- Formato de selección ≠ construcción.

### Riesgos de falsa mejora
- **Mejora-en-la-tarea pura:** memorizar "la contable precisa".
- **Etiqueta engañosa:** el módulo cree medir "construcción de contraejemplos"; mide selección.

### Métricas recomendadas
- **Principal:** en formato de construcción — proporción de contraejemplos producidos que satisfacen (premisas∧¬conclusión).
- **Secundaria:** tasa de error tipo "destruir premisa" vs "confirmar conclusión" (los dos modos de fallo).
- **Engañan hoy:** accuracy de selección 1-de-3.

### Progresión recomendada
Seleccionar contraejemplo válido → seleccionar entre casos sutiles → CONSTRUIR (rellenar atributos de una entidad para que premisas se mantengan y conclusión caiga) → argumentos de 3 premisas con cuantificadores anidados.

### Rediseño propuesto
Modo construcción: dar el argumento y un constructor de entidad (atributos seleccionables: profesión, propiedad-precisa sí/no, etc.); el motor evalúa con `CounterexampleValidator` (que YA existe) si la entidad construida mantiene premisas y falsa conclusión. Esto cablea el validador huérfano al scoring. Superior porque mide generación, el constructo real, reutilizando código ya escrito.

### Puntuación 0-10
1) 5 · 2) 5 · 3) 8 · 4) 2 · 5) 4 · 6) 2 · 7) 2 · 8) 6 · 9) 4 · 10) 7 · 11) 3 · 12) 5 · 13) 4 · 14) 6 · 15) 8
**Nota global del ejercicio: 4.6/10**
**Decisión final:** Redisenar parcialmente (pasar a modo construcción y cablear `CounterexampleValidator` al scoring).

---

## Ejercicio: Isomorfo condicional / sonda de transferencia (`logic-e3-library-iso-01`, `CONDITIONAL_TEST` isomorfo)

### Diagnóstico rápido
Clon estructural de `logic-e3-access-01` con superficie de biblioteca, marcado `isIsomorphicProbe=true`. Es el ÚNICO mecanismo de transferencia del módulo y conceptualmente lo correcto: misma lógica, distinta piel. Pero un solo par entrenado→isomorfo no es una medida de transferencia, es n=1 anecdótico.

### Mecanismo cognitivo
No entrena una habilidad nueva: mide si la habilidad de `logic-e3` se aplica a superficie distinta (transferencia cercana). El concepto de sonda isomorfa es exactamente el diseño correcto para distinguir aprendizaje superficial de estructural. **Problema:** `transferConsistencyFor` (`LogicStateHolder.kt:223-228`) requiere ambos correctos para puntuar 1.0 (correcto, ya corregido según CLAUDE.md 2026-06-15), pero busca el twin con `attempts.lastOrNull { logicalStructureId == ... }` — depende del ORDEN de sesión y de que el twin entrenado haya aparecido antes en la MISMA sesión. Si el orden cambia o el twin está en otra sesión, la transferencia no se computa.

### Potencial de transferencia
Es el instrumento de transferencia, no un objeto de transferencia. Su valor depende de la cantidad de pares: con 1 par, la métrica `transferConsistency` es binaria y ruidosa (`LogicStateHolder.kt:215-217` promedia probes pero solo hay 1). **Muy baja como medición** por n=1.

### Problemas detectados
- Un solo par isomorfo en todo el módulo.
- `transferConsistencyFor` acoplado al orden de aparición intra-sesión (`:225`): el twin entrenado debe preceder al probe en la lista de 6, lo cual está garantizado por el orden fijo de `sessionItems()` pero es frágil — si se aleatoriza el contenido (necesario para evitar memorización) esto se rompe silenciosamente. [Riesgo científico]
- `transferConsistency` de sesión promedia sobre probes pero ignora el ICR del probe (`LogicStateHolder.kt:215-217` usa solo `isCorrect`), mientras el adapter sí exige ICR≥70 (`LogicDifficultyAdapter.kt:97`). Dos definiciones de "transferencia confirmada" incoherentes entre summary y adapter.

### Riesgos de falsa mejora
- **n=1:** acertar 1 isomorfo "confirma transferencia" y sube dificultad (`LogicDifficultyAdapter.kt:96-103`). Un acierto por azar (1/3) dispara progresión. Falsa señal de transferencia.
- Como el isomorfo es estructuralmente idéntico y aparece en la misma sesión, puede resolverse por analogía superficial reciente, no por abstracción.

### Métricas recomendadas
- **Principal:** tasa de transferencia sobre ≥5 pares isomorfos por estructura, con separación temporal (probe en sesión posterior, no inmediata).
- **Secundaria:** caída entrenado→isomorfo (transfer gap) como índice de aprendizaje superficial.
- **Engañan hoy:** `transferConsistency` binaria sobre n=1; la decisión de subir dificultad por 1 probe.

### Progresión recomendada
Aumentar distancia entre entrenado e isomorfo: misma estructura distinta superficie (cercano) → estructura análoga con relación distinta (medio) → transferencia lejana a dominio no entrenado. Separar temporalmente.

### Rediseño propuesto
Pool de ≥5 isomorfos por estructura, presentar el probe en sesión N+1 (no inmediata), unificar la definición de "transferencia confirmada" (mismo umbral ICR en summary y adapter), y exigir ≥3 probes antes de mover dificultad por transferencia. Superior porque convierte una anécdota n=1 en una medida.

### Puntuación 0-10
1) 4 · 2) 4 · 3) 7 · 4) 3 · 5) 3 · 6) 3 · 7) 2 · 8) 5 · 9) 3 · 10) 8 · 11) 2 · 12) 5 · 13) 4 · 14) 5 · 15) 9
**Nota global del ejercicio: 4.1/10**
**Decisión final:** Mantener con cambios mayores (es el diseño correcto; necesita N pares + separación temporal + unificación de umbral).

---

## Veredicto del módulo "Lógica (razonamiento)"

### Capacidades bien cubiertas / huecos / ausentes
**Bien cubiertas (a nivel de concepto, no de banco):** deducción necesaria, refutación condicional (Wason), necesario/suficiente, consistencia, contraejemplo. La taxonomía declara 28 subhabilidades (`LogicModels.kt:7-36`) pero solo 5 estructuras tienen contenido — **23 de 28 subskills son enum sin ningún ítem.** El andamiaje metacognitivo (confianza explícita, justificación estructurada, independencia de pistas) es serio y honesto.

**Huecos:** razonamiento con cuantificadores (`QUANTIFIERS` sin contenido), bases/tasas (`BASE_RATES`), revisión de creencias (`BELIEF_REVISION`), valor de la información, restricciones multipaso (`MULTISTEP_CONSTRAINTS`), analogía relacional, abducción. Todo declarado, nada implementado.

**Ausentes por completo:** memoria de trabajo bajo carga (todos los ítems son de 2-3 premisas, sin presión de retención), razonamiento bajo tiempo/presión real, y cualquier ítem que exija GENERAR en vez de SELECCIONAR (todo es opción múltiple de 3-4).

### Ejercicios redundantes / a fusionar
- `logic-e3-access-01` y `logic-e3-library-iso-01` son el MISMO ítem con piel distinta (por diseño, como probe). No fusionar — pero sí multiplicar a un pool real.
- Todos los ítems comparten el mismo formulario de 5 campos. No hay redundancia de tipo, hay escasez de instancias: 6 ítems, 5 estructuras.

### Riesgos sistémicos del módulo
1. **Banco finito de 6 ítems fijos sin generador (crítico).** `DefaultLogicContentProvider.sessionItems()` (`LogicContentProvider.kt:29`) hardcodea 6 ítems. Tras 2-3 sesiones todo es memoria, no razonamiento. Es el riesgo #1 de pseudoprogreso de todo el módulo.
2. **Adaptación de dificultad totalmente desconectada del contenido (crítico).** `LogicDifficultyAdapter` calcula `DifficultyDecision` y `thetaEstimate` por skill (`LogicDifficultyAdapter.kt:30-111`), pero `startSession()` (`LogicStateHolder.kt:53-68`) NUNCA lee la dificultad ni el theta para seleccionar ítems — siempre sirve los mismos 6. La "zona óptima de desafío" no existe; el `difficultyLevel` de cada ítem es decorativo. **Toda la maquinaria adaptativa es UI sin efecto.**
3. **Bug de precedencia de operadores en la penalización (real, citado).** En `ReasoningScorer.kt:48-51`:
   ```kotlin
   val penaltyPoints = MAX_SCORE * (
       IMPULSIVITY_WEIGHT * if (impulsive) 1.0 else 0.0 +
           CONCEPTUAL_ERROR_WEIGHT * if (conceptualError) 1.0 else 0.0
       )
   ```
   En Kotlin `if` como expresión sin paréntesis absorbe lo que sigue: `IMPULSIVITY_WEIGHT * (if (impulsive) 1.0 else (0.0 + CONCEPTUAL_ERROR_WEIGHT * (if (conceptualError) 1.0 else 0.0)))`. Cuando `impulsive=true`, el término del error conceptual DESAPARECE de la penalización; cuando `impulsive=false`, la penalización por error conceptual se multiplica por `IMPULSIVITY_WEIGHT` (0.10), no por `CONCEPTUAL_ERROR_WEIGHT` (0.15). **La penalización conceptual casi nunca se aplica con el peso correcto.** Es exactamente el patrón que CLAUDE.md (2026-06-15, "Parentetizar `if`...") dice que ya mordió en `ReasoningScorer.kt` — reapareció aquí en el cálculo de penaltyPoints. Hay que parentetizar: `IMPULSIVITY_WEIGHT * (if (impulsive) 1.0 else 0.0) + CONCEPTUAL_ERROR_WEIGHT * (if (conceptualError) 1.0 else 0.0)`. [Mejora crítica]
4. **Azar de opción múltiple inflando 2 componentes del ICR.** Justificación (2 opciones, `LogicRootScreen.kt:361`) y a veces regla (2 reglas aceptadas) → ~50% de acierto por azar en el 35% del peso del ICR (15% justificación + 20% regla). El "process-first ICR" se contamina con ruido de azar.
5. **`averageIcr` como número único agregando 6 dimensiones heterogéneas** (`LogicModels.kt:190-201`) oculta el perfil real: un usuario que acierta respuestas pero falla justificación, y otro que justifica bien pero adivina, pueden tener el mismo ICR. La métrica de cara al usuario engaña.
6. **`CounterexampleValidator` huérfano:** definido y testeado, nunca cableado al scoring (solo `isCorrect` del option decide). La semántica del contraejemplo no se valida en producción.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Sustituir los 6 ítems fijos por generadores paramétricos por estructura (banco efectivamente infinito).** Es la única forma de que el módulo mida razonamiento y no memoria. Sin esto, todo ICR longitudinal es pseudoprogreso. Prioridad absoluta.
2. **Cablear la dificultad/theta a la selección de contenido.** El adapter ya calcula la decisión; falta que `startSession()` la consuma para servir ítems en la zona óptima de desafío (theta±). Hoy la adaptación es teatro. Convierte ~150 líneas de maquinaria muerta en entrenamiento adaptativo real.
3. **Arreglar la medición:** (a) corregir el bug de precedencia en `penaltyPoints` (`ReasoningScorer.kt:48`); (b) eliminar el azar de opción múltiple en justificación y regla (selección abierta o ≥4 opciones); (c) reemplazar `averageIcr` único por un perfil multidimensional + Brier real de calibración; (d) cablear `CounterexampleValidator`; (e) corregir la filtración de respuesta en el prompt de necesario/suficiente (`LogicContentProvider.kt:153`). Sin medición honesta, no se puede saber si (1) y (2) funcionan.

**Nota global del módulo: 4.1/10.** Arquitectura de software limpia, andamiaje metacognitivo honesto y conceptos lógicos correctos, montados sobre un banco de 6 ítems memorizable y una adaptación desconectada del contenido. Hoy entrena reconocimiento de 6 reactivos fijos, no razonamiento general. El diseño tiene los huesos correctos; le falta el contenido generativo y el cableado adaptativo que lo harían un entrenador real en vez de un test de 6 preguntas con buena UI.

---


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


## Modulo: Memoria

I now have a complete, verified picture. The decisive gap: the **live session path** (`MemorySessionStateHolder` → `MemorySessionScreen`) bypasses nearly all the scientific machinery. The generators emit timing/delay/serial/processing-task structure that the UI never renders; recall is a single untimed recognition multi-select; the assembler, deferred recall, recovery, partition-based probes, RCI, RealImprovement, plateau and fitness reporting are built and tested but never invoked from a real session. I'll now write the audit.

## Ejercicio: Span Complejo (EX1_SPAN_COMPLEJO)

### Diagnóstico rápido
El dominio genera una tarea de span complejo legítima (memoranda intercalados con verificación aritmética, recuerdo forward/backward); el **runtime la destruye**. `MemorySessionStateHolder.studyOf` (líneas 204-220) presenta los memoranda como `Text` estáticos sin tiempo, **descarta por completo los `ProcessingTask`** (nunca se muestran ni se responden), y el recall es un multi-select de reconocimiento sobre `studied + 6 distractores` (`candidatesFor`, líneas 222-237) que **ignora el orden serial**. Lo que el usuario hace de verdad no es span complejo: es reconocimiento sí/no de 2-8 palabras vistas hace segundos. No vale en su forma actual.

### Mecanismo cognitivo
El objetivo declarado es memoria de trabajo bajo carga con interferencia dual-task (`primaryMetrics = WORKING_MEMORY_SPAN, DUAL_TASK_COST`, catálogo líneas 38-43). El mecanismo que *debería* entrenar (mantenimiento activo + procesamiento concurrente + recuperación ordenada) es correcto y plausible **en el papel del generador** (`Ex1SpanGenerator`, líneas 77-88).
- **Lo que produce adaptación cognitiva:** mantener N ítems mientras un distractor consume recursos ejecutivos, en el límite del span. Eso es exactamente lo que NO ocurre en runtime.
- **Lo que SOBRA:** nada en el generador; el problema es por defecto.
- **Lo que CONTAMINA la medición:** (1) la presentación simultánea y sin límite temporal de todos los memoranda (`StudyContent`, `MemorySessionScreen` líneas 74-83) permite repaso/subvocalización ilimitada y elimina la carga; (2) el `ProcessingTask` no se ejecuta → no hay dual-task → `DUAL_TASK_COST` es incomputable y la tarea colapsa a span simple; (3) el reconocimiento con 6 distractores es trivial frente al recall serial: acertar por familiaridad infla `WORKING_MEMORY_SPAN`, que se computa como `trial.hits.toDouble()` (`MemoryMetricsEngine` líneas 77-86) — es decir, **número de reconocimientos correctos, no span**.
- **Lo que es solo práctica de la tarea:** todo, dado el banco finito y el reconocimiento (ver riesgos).

### Potencial de transferencia
**Nula** en el estado actual. La WM entrenada con span complejo *real* tiene transferencia cercana documentada y discutida (Gf es polémico), pero aquí no se entrena WM: se entrena reconocimiento episódico inmediato de palabras españolas. Mejora-en-la-tarea: alta (memorizar 6 palabras y reconocerlas es fácil y se satura). Transferencia cercana: nula (no hay carga ni dual-task). Funcional a la vida real: nula. Para que suba a "baja/media": **renderizar la presentación serial temporizada, ejecutar los `ProcessingTask` con su propia respuesta y latencia, y exigir recall por producción/reconstrucción de orden, no multi-select.**

### Problemas detectados
- `MemorySessionStateHolder.studyOf` (204-209): presenta `expectedRecall` como lista; **el `steps` con los `ProcessingTask` se ignora**. El dual-task no existe en producción.
- `MemorySessionScreen.StudyContent` (74-83): sin temporización; `studyMsPerItem`/orden de presentación no se usan. `ComplexSpanTrial` no tiene tiempos, pero el holder tampoco impone ninguno.
- `score()` (241-244): para EX1 construye `recalled` mapeando `expectedRecall` en su orden canónico y marcando hit si el id está `in selected`. **Esto evalúa pertenencia al conjunto, no posición serial** → `ORDER_ERROR` (definido en `MemoryScorer.scoreComplexSpan` líneas 76-81) es **inalcanzable** desde el runtime: nunca se pasa una palabra en posición equivocada.
- `WORKING_MEMORY_SPAN = trial.hits.toDouble()` (`MemoryMetricsEngine` 81): no es un span (ni partial-credit unit normalizado, ni longitud de secuencia perfecta). Es un conteo crudo de aciertos dependiente del `studiedCount` del día → no comparable entre dificultades.
- `candidatesFor` (222-237): los distractores se muestrean del mismo sampler con `excludeRecentIds = answerWords`, así que no colisionan con la respuesta, pero **el número de distractores es fijo (6)** independientemente del span (2-8) → la tasa base de acierto por azar varía con la dificultad y no se corrige (sin d-prime).

### Riesgos de falsa mejora
- **Banco finito + reconocimiento:** ≈142 palabras totales, menos el 20% reservado (`TransferProbePartition`, ratio 0.20) → ~113 de entrenamiento (`SpanishMemoryContentData` líneas 65-121). Con reconocimiento de 2-8 ítems el techo se alcanza en sesiones y el "acierto %" sube por familiaridad con el pool, no por capacidad.
- **Dificultad falsa:** subir `LOAD` añade memoranda, pero como la presentación es estática y sin tiempo, no añade carga real → la curva de "dificultad" no mapea a esfuerzo cognitivo.
- **Feedback inflado:** `FinishedContent` muestra "Acierto: X% (hits/studied)" tras una tarea de reconocimiento trivial → el usuario percibe alto rendimiento sin entrenar WM.
- **`delayedAccuracy = null` siempre** (`finish`, línea 164): la nota honesta promete "recuerdo diferido" que el runtime nunca ejecuta.

### Métricas recomendadas
- **Principal:** span con partial-credit load scoring (proporción de ítems en posición correcta ponderada por longitud) o longitud de secuencia con 50% de aciertos (umbral adaptativo), NO conteo de hits.
- **Procesamiento:** exactitud y RT medio del `ProcessingTask` (gate de validez: descartar trials con accuracy de procesamiento < ~85%, el control estándar anti-trade-off en complex span).
- **Secundarias:** `DUAL_TASK_COST` real (span solo vs span+procesamiento), curva de posición serial (primacía/recencia), tasa de errores de orden vs de ítem, intrusiones.
- **Consistencia:** varianza intra-bloque del span.
- **Engañan hoy:** `WORKING_MEMORY_SPAN` (es un conteo de reconocimiento), `IMMEDIATE_RECALL_ACCURACY` (saturada por reconocimiento), cualquier % de acierto mostrado.

### Progresión recomendada
Mantener la zona de desafío en el span umbral (≈50-75% de secuencias perfectas) con escalera 1-up/1-down sobre **longitud**, no sobre conteo. Procesamiento como eje ortogonal de carga (1→2→3 operaciones por ítem, con ventana de tiempo). Evitar techo bloqueando el reconocimiento (usar producción) y evitar fondo con warmup a span−1.

### Rediseño propuesto
Mecánica: presentación **serial temporizada** (cada memorandum 1000-1500 ms), intercalando el `ProcessingTask` como pantalla con respuesta verdadero/falso y latencia registrada (ya existe en el plan, solo hay que renderizarlo). Recall por **reconstrucción de orden** (arrastrar/teclear la secuencia) o, en V1 mínimo, recall por producción libre (teclado) clasificado por `MemoryScorer.scoreComplexSpan` con alineación posicional real. Dificultad adaptativa: escalera sobre longitud + nº de operaciones, gobernada por el `DefaultMemoryAdaptiveEngine` que ya existe (hoy se invoca pero su zona se calcula sobre métricas falsas). Métricas: partial-credit span + processing accuracy/RT + dual-task cost. Feedback: span estimado + "operaciones correctas" (penalizar trade-off). Variantes: verbal/espacial. Test: simulación con respondedores sintéticos de span conocido — el span estimado debe recuperar el parámetro. Superior porque restaura la carga, el dual-task y la sensibilidad posicional que definen el paradigma; sin eso, no es span complejo.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **2** | 2) Transferencia a la vida real: **1** | 3) Claridad del mecanismo: **4** (claro en dominio, roto en runtime) | 4) Calidad de la progresión: **3** | 5) Calidad de la medición: **2** | 6) Personalización: **4** | 7) Resistencia al efecto práctica: **2** | 8) Valor metacognitivo: **1** (EX16 no cableado, `confidenceBias=0.0`) | 9) Valor emocional: **2** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **8** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **2** | 12) Adherencia: **4** | 13) Escalabilidad a perfiles distintos: **4** | 14) Robustez científica: **2** | 15) Prioridad de mejora: **9**
**Nota global del ejercicio: 3/10**
**Decisión final:** Rediseñar parcialmente — el generador es bueno; hay que conectar presentación serial temporizada, ejecución real del processing-task y recall por producción/orden. Sin eso, **eliminar** del set hasta que el runtime esté a la altura del dominio.

---

## Ejercicio: Lista con Estrategia (EX3_LISTA_ESTRATEGIA)

### Diagnóstico rápido
Free recall episódico con clustering ARC — el ejercicio con mejor base científica del trío. Pero el runtime lo convierte otra vez en **reconocimiento multi-select sin demora**, lo que mata su métrica estrella (`ARC_CLUSTERING` requiere recall por producción, no reconocimiento) y la demora declarada (`delayMs` hasta 5 min) **nunca se aplica** (`StudyContent` no espera nada; `finish` guarda `delayedAccuracy = null`). Vale el diseño; no vale la ejecución.

### Mecanismo cognitivo
Objetivo: memoria episódica con codificación elaborativa y estrategias de organización (clustering semántico), medida tras demora (`governingMetric = DELAYED_RECALL_ACCURACY`, catálogo 88). El mecanismo correcto: estudiar una lista, **organizarla activamente por categorías** y recuperarla libremente; el ARC mide si el usuario impuso estructura (señal de estrategia de codificación profunda, el corazón del entrenamiento).
- **Produce adaptación:** la presión por organizar/elaborar bajo capacidad limitada y recuperar tras demora.
- **SOBRA:** nada en el generador.
- **CONTAMINA:** (1) reconocimiento en vez de recall → **el clustering es inmedible** (en un multi-select el "orden de recuperación" lo fija el orden de los chips en pantalla, no el usuario; `putArcClustering` lee `trial.responses` filtrando HIT y mapeando `category`, líneas 109-112 — pero esas responses provienen de `selectedWords` que filtra `studyList` en su **orden de estudio**, `Candidates.selectedWords` líneas 282-283, no en orden de recuperación). El ARC computado es por tanto **artefactual**: refleja el orden en que el generador colocó la lista, no la organización del usuario; (2) sin demora, `IMMEDIATE_RECALL_ACCURACY` es lo único que se mide y `DELAYED_RECALL_ACCURACY` (la métrica gobernante) jamás se produce.
- **Solo práctica:** reconocer palabras del pool.

### Potencial de transferencia
**Muy baja** ahora; **media** si se arregla. El free recall con estrategias de codificación tiene el puente funcional más fuerte del módulo (`TransferProtocols.STUDY`, `v1Available=true`): estudio eficiente, retención a 24h. Pero ese puente exige recall por producción + demora real, ninguno presente. Mejora-en-la-tarea: alta. Cercana: baja. Funcional (estudiar/retener): potencialmente media **solo** con el rediseño.

### Problemas detectados
- **ARC artefactual:** `Candidates.selectedWords` (282-283) reconstruye los recalled filtrando `studyList` → preserva el orden de estudio, no el de recuperación. `ArcClustering.arcIndex` (líneas 26-35) cuenta repeticiones de categoría adyacentes **sobre ese orden falso**. La métrica de clustering no mide clustering. [Riesgo científico]
- **Demora ignorada:** `RecallListTrial.delayMs` (generado 0-300_000 ms, `Ex3DifficultyMapping` línea 39) nunca se consume; `MemoryMetricsEngine.putCommon` decide la clave por `trial.delayMs > 0` (41-44), pero el `scoreList` propaga `plan.delayMs` (línea 109) — así que **si el adaptativo sube DELAY, la métrica cambia de IMMEDIATE a DELAYED sin que haya habido ninguna demora real**. Es una etiqueta de demora sin demora. [Riesgo científico grave]
- **Sin distinción primacía/recencia, sin curva de posición serial** (no se registra orden de recuperación en absoluto).
- **Banco finito:** listas de 8-20 palabras desde ~113 de entrenamiento → solapamiento alto entre sesiones pese a `excludeRecentWordIds`; con `verticalScroll` el usuario ve toda la lista a la vez sin presión temporal.

### Riesgos de falsa mejora
- **Reconocimiento sobre banco pequeño:** `IMMEDIATE_RECALL_ACCURACY` se satura; el "% acierto" sube por familiaridad.
- **DELAYED etiquetado sin demora:** el sistema puede reportar "recuerdo diferido" alto que en realidad es recuerdo inmediato → **pseudoprogreso de consolidación**, justo lo que `RealImprovementCriterion.POOR_CONSOLIDATION` pretende detectar, pero ese criterio nunca se alimenta con datos reales.
- **ARC inflado/aleatorio** presentado como evidencia de "estrategia".

### Métricas recomendadas
- **Principal:** `DELAYED_RECALL_ACCURACY` con demora **real** (intra-sesión ≥ varios minutos con tarea distractora, y entre-sesión a 24h vía `DeferredRecallScheduler` que ya existe pero no se ejecuta en el test diferido).
- **Secundarias:** ARC **real** (sobre orden de recuperación capturado), curva de posición serial, índice de primacía/recencia, intrusiones (ya crudo, correcto por aprendizaje 2026-06-04), `CONSOLIDATION_INDEX` (existe `consolidationIndex` pero nadie lo llama con dos medidas reales).
- **Engañan hoy:** `ARC_CLUSTERING` (artefactual), `DELAYED_RECALL_ACCURACY` (etiqueta sin demora), cualquier % inmediato.

### Progresión recomendada
LOAD (8→20) + DEPTH (menos tiempo de estudio fuerza elaboración, ya en `Ex3DifficultyMapping` 37) + DELAY real escalado. Zona óptima ~60-75% de recall diferido. Evitar techo con recall por producción (mucho más sensible que reconocimiento); evitar fondo con listas cortas categorizadas y cue de categoría (`CUED` ya existe).

### Rediseño propuesto
Recall por **producción libre** (teclado): el usuario escribe lo que recuerda, en su orden → captura el orden de recuperación real para ARC y curva serial. Demora real: insertar tarea distractora (el propio `ProcessingTask` de EX1 sirve) entre estudio y test; y **activar el flujo de recuerdo diferido a 24h** (`DeferredRecallScheduler.due` + `MemoryHomeStateHolder.dueDeferredCount` ya lo cuentan, pero no hay pantalla que ejecute el test diferido). Métricas: delayed real + ARC real + consolidación. Feedback: "recordaste X/Y, y tu organización por categorías fue alta/baja", más el resultado a 24h al día siguiente. Variantes: listas categorizables vs no categorizables (manipula la oportunidad de clustering). Test: respondedores sintéticos con clustering conocido → ARC debe recuperarlo; respondedores con olvido conocido → consolidación debe recuperarlo. Superior porque restaura las dos señales que justifican el ejercicio (organización estratégica y retención diferida), hoy ambas falsas.

### Puntuación 0-10 (los 15 ejes)
1) **3** | 2) **2** | 3) **5** | 4) **4** | 5) **2** (ARC y delayed falsos) | 6) **4** | 7) **2** | 8) **2** | 9) **3** | 10) **8** | 11) **2** | 12) **5** | 13) **4** | 14) **3** | 15) **9**
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente — el generador, el ARC y el delayed/consolidation son ciencia correcta; hay que (a) capturar recall por producción y orden, (b) aplicar demora real, (c) cablear el test diferido a 24h. Es el ejercicio con mayor retorno del módulo si se arregla.

---

## Ejercicio: Pares Asociados (EX4_PARES_ASOCIADOS)

### Diagnóstico rápido
Memoria asociativa con cued recall, errores de conjunción y dirección bidireccional — el paradigma con mayor utilidad declarada en la vida real (vocabulario, nombres). El generador es sólido (`Ex4PairsGenerator`), pero el runtime vuelve a colapsarlo: la prueba con **cue** desaparece, se convierte en reconocimiento multi-select de los **targets**, y el `CONJUNCTION_ERROR` se vuelve casi inalcanzable porque el usuario nunca elige un target equivocado dado un cue (no hay cue). Diseño bueno, ejecución que neutraliza su discriminación.

### Mecanismo cognitivo
Objetivo: binding asociativo cue→target y su resistencia a confusión (`governingMetric = ASSOCIATIVE_INDEX`, `CONJUNCTION_ERROR_RATE`, catálogo 116-123). El mecanismo correcto: estudiar pares arbitrarios y, dado el cue, recuperar el target — el binding relacional, distinto del memoria de ítem.
- **Produce adaptación:** formar y recuperar vínculos arbitrarios bajo interferencia (pares débiles, bidireccionalidad).
- **SOBRA:** nada en el generador.
- **CONTAMINA:** (1) `score()` para pares (líneas 249-254) mapea `plan.probes` y marca hit si `p.expectedTarget.id.value in selected` — **el cue nunca se presenta como tal**; el usuario ve `studied + distractores` y selecciona los que reconoce. Esto mide reconocimiento de ítems estudiados, **no recuperación dirigida por cue**; (2) por eso `CONJUNCTION_ERROR` (un target estudiado pero de otro par, `MemoryScorer.scorePairs` 128) es prácticamente inalcanzable desde el runtime: requiere que el usuario, *dado un cue concreto*, responda el target de *otro* par — imposible sin cue; (3) la bidireccionalidad (`RecallDirection.BACKWARD`) se genera pero es invisible en reconocimiento.
- **Solo práctica:** reconocer palabras-target del pool.

### Potencial de transferencia
**Muy baja** ahora; **media-alta** si se arregla — es el candidato con mejor puente funcional (`TransferProtocols.VOCABULARY`, `v1Available=true`: pares L1-L2, retención a 24h/7d). El cued-recall asociativo *real* transfiere a aprendizaje de vocabulario y, con caras, a nombres. Pero requiere presentar el cue y exigir el target por producción. Mejora-en-la-tarea: alta. Cercana: baja. Funcional (idiomas): media-alta **solo** con rediseño.

### Problemas detectados
- **Sin cue en la prueba:** `MemorySessionStateHolder.candidatesFor` (222-237) construye candidatos desde `answerWords` (= targets distintos, `studyOf` 218) + distractores; `RecallContent` los muestra todos juntos. El `CuedRecallProbe.cue` nunca llega a la UI. El paradigma cue→target no se ejerce. [Riesgo científico grave]
- **`ASSOCIATIVE_INDEX = hits/studiedCount`** (`MemoryMetricsEngine` 91-94) donde `studiedCount = plan.probes.size` (`scorePairs` 137) — mide reconocimiento de targets, etiquetado como índice asociativo.
- **Cap de 10 pares** (`Ex4DifficultyMapping` 38) por tamaño de pool; documentado, pero limita el techo de carga.
- **Fan-effect / interferencia diferida** (pares que comparten palabras) explícitamente no soportado (comentario líneas 26-30) → la métrica `CONJUNCTION_ERROR` carece de la condición que la haría informativa aun con cue.
- **`studyMsPerPair` y `delayMs` ignorados** por el runtime (mismo defecto que EX1/EX3).

### Riesgos de falsa mejora
- **Reconocimiento de targets sobre banco finito:** 56 pares totales − 20% reservado ≈ 44 de entrenamiento (`SpanishMemoryContentData` línea 124, `TransferProbePartition` ratio 0.20). Se memoriza el pool en pocas sesiones.
- **`CONJUNCTION_ERROR ≈ 0` siempre** → el sistema reporta "resistencia a la confusión perfecta" que es un artefacto de no presentar cue.
- **Bidireccionalidad fantasma:** subir RETRIEVAL genera probes B→A que no cambian nada en reconocimiento → dificultad falsa.

### Métricas recomendadas
- **Principal:** `ASSOCIATIVE_INDEX` como **cued recall accuracy real** (dado cue, producir target).
- **Secundarias:** `CONJUNCTION_ERROR_RATE` (con pares que comparten elementos para que sea informativa), asimetría forward/backward, RT de recuperación, retención diferida del par a 24h.
- **Engañan hoy:** `ASSOCIATIVE_INDEX` (reconocimiento, no asociación), `CONJUNCTION_ERROR_RATE` (≈0 artefactual), `RETRIEVAL_SPEED` (RT no se captura en el multi-select).

### Progresión recomendada
LOAD (4→10 pares) + SIMILARITY (`weakOnly`, ya en mapping 46) + DELAY real + bidireccionalidad. Introducir fan-effect (pares solapados) como eje de interferencia cuando crezca el pool. Zona óptima ~60-75% cued recall. Evitar techo eliminando reconocimiento; evitar fondo con pares fuertes y unidireccionales.

### Rediseño propuesto
Prueba de **cued recall**: mostrar el cue, el usuario produce el target (teclado o, en V1, selección de **una** opción por cue entre target + conjunción-distractores de otros pares estudiados — esto hace `CONJUNCTION_ERROR` medible sin teclado). Presentar cada probe por separado con su cue, capturando RT. Aplicar demora real y cablear retención a 24h. Crecer el pool con pares solapados para fan-effect. Métricas: cued recall + conjunción real + asimetría direccional. Feedback: "dado X recordaste Y" y, al día siguiente, retención del par. Variante directa: vocabulario L1-L2 (puente `VOCABULARY` ya definido). Test: respondedores sintéticos con tasa de binding conocida → el índice debe recuperarla; inyectar confusiones → conjunción debe subir. Superior porque restaura la recuperación dirigida por cue y la confusión inter-par, que son la definición operativa de memoria asociativa.

### Puntuación 0-10 (los 15 ejes)
1) **3** | 2) **2** | 3) **5** | 4) **4** | 5) **2** | 6) **4** | 7) **2** | 8) **1** | 9) **3** | 10) **8** | 11) **2** | 12) **5** | 13) **5** (mejor puente funcional) | 14) **3** | 15) **9**
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente — presentar el cue y exigir el target (selección 1-de-N con conjunción-distractores como mínimo viable). Con eso es el ejercicio de mayor utilidad real del módulo. En su forma actual, no mide memoria asociativa.

---

## Veredicto del módulo "Memoria (memoria de trabajo y episódica)"

**Capacidades bien cubiertas (en el DOMINIO, no en runtime):**
- WM bajo carga (EX1), episódica/clustering (EX3), asociativa (EX4) — las tres subcapacidades nucleares.
- Infraestructura científica **excelente y bien testeada pero desconectada**: `ReliableChangeIndex` (Jacobson-Truax correcto), `RealImprovementCriterion` (gate untrained-probe + effect size + 2 semanas — honesto), `TransferProbePartition` (reserva disjunta verificada en `init`), `DeferredRecallScheduler` (ventana 20-48h), `PlateauDetector`/`InterventionPlanner`, `MemoryRecoveryPolicy` (anti-overtraining), `MemoryFitnessCalculator` (bandas de incertidumbre por n), métricas con `confident`/`n` por construcción, intrusiones sobre datos crudos (aprendizaje 2026-06-04 aplicado bien).

**Huecos / capacidades AUSENTES en el producto vivo:**
- **El runtime no ejecuta nada de la ciencia.** `MemorySessionStateHolder.runSession` (95-130) corre **un único trial**, presentación **sin temporización**, recall por **reconocimiento multi-select** (`studied + 6 distractores`), `delayedAccuracy = null` (línea 164). `MemorySessionAssembler` (multi-bloque, warmup/encoding/interference/delayed), `DeferredRecallScheduler`, `MemoryRecoveryPolicy`, `RealImprovementCriterion`, `ReliableChangeIndex`, `PlateauDetector`, `MemoryFitnessCalculator` y los **probes reservados** (`MemoryProbeBank`, `MemoryBaselineProtocols.INITIAL`) **no se invocan desde ninguna sesión real**. Son código muerto operacional: construido, testeado, no cableado.
- **No hay recall por producción.** Las tres tareas colapsan a reconocimiento, lo que (a) destruye ARC (orden de recuperación falso), (b) hace `CONJUNCTION_ERROR`/`ORDER_ERROR` inalcanzables, (c) satura la precisión.
- **No hay demora real ni recuerdo diferido a 24h ejecutable.** El home cuenta `dueDeferredCount` pero no existe pantalla que administre el test diferido.
- **No hay baseline ni probes en el flujo.** `MemoryBaselineProtocols.INITIAL` y los `MemoryProbe` reservados existen; el usuario nunca los corre → **no hay medida untrained → el sistema no puede distinguir mejora real de efecto práctica**, que es exactamente su principio rector declarado.
- **Metacognición (EX16) sin cablear:** `confidenceBias = 0.0` fijo (`MemorySessionStateHolder` 145; `MemoryAdaptiveInput` 119) → `ActivateMetacognition` nunca dispara; `CALIBRATION_INDEX`/`BRIER_SCORE`/`GAMMA` no se computan.

**Ejercicios redundantes o a fusionar:** ninguno redundante (cubren subcapacidades distintas). El `ProcessingTask` de EX1 puede **reutilizarse como tarea distractora** para inducir demora real en EX3/EX4 (DRY, sin fusionar ejercicios). EX14 Dual-Task (catálogo, no implementado) es conceptualmente EX1 con la concurrencia explícita → cuando se implemente, derívalo del runtime de EX1, no como código nuevo.

**Riesgos sistémicos:**
1. **Medición común contaminada por reconocimiento:** todas las métricas de precisión heredan la inflación del multi-select sobre banco finito. El `confident`/`n` honesto no salva que el **constructo medido es el equivocado** (reconocimiento ≠ recall/span/asociación).
2. **Banco de contenido demasiado pequeño:** ~142 palabras / 56 pares, autoconfeso muy por debajo del ideal (>2000). Con reconocimiento, el efecto práctica/familiaridad domina la señal en semanas. `excludeRecentWordIds` mitiga repetición intra-corto-plazo, pero el pool se agota como universo de reconocimiento.
3. **Etiqueta de demora sin demora:** la métrica salta IMMEDIATE↔DELAYED por `delayMs` del plan aunque no transcurra tiempo real (`MemoryMetricsEngine.putCommon` 41-44 + `scoreList`/`scorePairs` propagando `plan.delayMs`). Riesgo de reportar consolidación inexistente. [Riesgo científico]
4. **Adaptación gobernada por métricas falsas:** `DefaultMemoryAdaptiveEngine` es correcto, pero su `MemoryZone` se calcula sobre accuracy de reconocimiento saturada → empuja dificultad por las razones equivocadas.
5. **El anti-engaño es estructural pero inerte:** la reserva de probes y los criterios RCI/RealImprovement están bien diseñados y desconectados → el producto **afirma** honestidad (nota "la mejora real se confirma con pruebas no entrenadas") sin **ejecutar** el mecanismo que la confirmaría.

**Las 3 cosas que más elevarían el poder cognitivo de este módulo (en orden):**

1. **Sustituir reconocimiento por recall por producción y conectar la demora real.** Es el cambio de mayor impacto: restaura simultáneamente span posicional (EX1), clustering ARC verdadero (EX3) y cued recall con conjunción (EX4), y desbloquea `DELAYED_RECALL_ACCURACY` legítimo. Sin esto, ninguna métrica del módulo mide lo que dice medir. (Para EX1, además, **renderizar y puntuar el `ProcessingTask`** para que exista el dual-task.)

2. **Cablear el ciclo de evaluación honesto que ya está construido:** baseline con `MemoryBaselineProtocols.INITIAL`, **probes reservados** (`MemoryProbeBank`) en pre/post, **test diferido a 24h** vía `DeferredRecallScheduler`, y alimentar `RealImprovementCriterion`/`ReliableChangeIndex`/`PlateauDetector` con datos reales. Es trabajo de *integración*, no de diseño nuevo — el código existe y está testeado. Esto convierte la honestidad declarada en honestidad ejecutada y es lo único que permite afirmar "mejora real vs efecto práctica".

3. **Multiplicar el banco de contenido (objetivo >2000 palabras / cientos de pares, con corpus de frecuencia como LEXESP/EsPal) y activar el flujo multi-bloque del `MemorySessionAssembler`** (warmup → encoding+immediate → interferencia → delayed). Esto ataca el efecto práctica de raíz, da varianza suficiente para `ReliableChangeIndex`, y hace que la dificultad (LOAD/DELAY/INTERFERENCE) module esfuerzo cognitivo real en una sesión estructurada, no en un trial aislado.

**Nota transversal de prioridad:** el módulo está en una situación peculiar — **el dominio está entre el mejor del repositorio (riguroso, honesto, testeado) y el runtime entre los más débiles (un solo trial, reconocimiento, sin tiempo, sin demora, sin probes).** El riesgo no es ciencia mala; es ciencia construida y **no conectada**, presentada al usuario con una "nota honesta" que el sistema no cumple. La prioridad 15 es máxima (9/10) en los tres ejercicios precisamente porque el coste de arreglarlo es integración, no investigación.

---


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


## Modulo: Emociones

I now have everything needed. Let me confirm one final critical inference — whether the EM3 separation metric uses the confusion matrix or just an "ET accuracy" subset, and the catalog consistency. I've already seen the metrics engine uses `etTrials` (INTERNAL_STATE + THOUGHT only) for separation, ignoring IMPULSE/FACT. Let me verify the `EmotionTrend`/trend computation is genuinely "UNKNOWN" always (no real longitudinal trend), which I saw hardcoded in `aggregateOf`. That confirms a systemic issue. 

I have read the catalog, all 6 wired generators, content bank, classifier, assembler, metrics engine, RT stats, adaptive engine, difficulty model, session holder, renderers, safety, feedback, strings, and profile. I have enough to write the brutal audit.

## Ejercicio: Pausa antes de actuar (EM1_PAUSA)

### Diagnóstico rápido
Es un go/no-go afectivo estándar: el usuario toca cuando el marco está ABIERTO y retiene cuando está CERRADO; la palabra emocional (`fallo`, `calma`) es solo distractor y nunca decide la respuesta (`EM1PausaGenerator.kt:48`, `EmotionTrialClassifier.kt:20-31`). Es honesto y mide control inhibitorio de verdad (tasa de comisión sobre el denominador no-go). Pero NO entrena regulación emocional: entrena inhibición motora con decorado afectivo superficial.

### Mecanismo cognitivo
Control inhibitorio (supresión de respuesta prepotente) más, en segundo plano, interferencia afectiva tipo emotional-Stroop vía `AFFECTIVE_INTERFERENCE_COST` (`DefaultEmotionMetricsEngine.kt:39-47`). El proceso REAL entrenado es la inhibición de comisión; eso está bien alineado con la capacidad declarada `PAUSE_BEFORE_ACTION`.
- **Qué produce adaptación:** elevar la proporción de no-go vía `INHIBITORY_DEMAND` (`EM1PausaGenerator.kt:30`). Es el único motor real de dificultad.
- **Qué SOBRA:** la palabra afectiva no añade carga de regulación; añade ruido perceptivo. El marco visual (`ABIERTO`/`CERRADO`) ya es la tarea completa; el "afecto" es cosmético.
- **Qué CONTAMINA la medición:** la tasa de no-go base (0.28) es baja; con `TRIALS_PER_BLOCK = 10` y dos bloques (`EmotionSessionStateHolder.kt:410-411`), salen ~5-6 trials no-go por sesión, justo en el filo de `MIN_NOGO = 6`. La métrica gobernante (`COMMISSION_RATE`) llegará frecuentemente a `confident = false` (datos insuficientes). Mecanismo entrenado real, medición subdimensionada.
- **Qué es solo práctica de tarea:** reconocer el patrón borde-grueso = ABIERTO. Mejora rapidísima por aprendizaje perceptivo, no por mejor regulación.

### Potencial de transferencia
**Muy baja.** El propio catálogo lo etiqueta `TASK_ONLY` (`EmotionExerciseCatalog.kt:30`), lo cual es honesto. Go/no-go entrena inhibición específica de la tarea; la evidencia de transferencia de inhibición a control de impulsos en la vida real es débil/nula. Es mejora-en-la-tarea. Para subir a transferencia cercana habría que: (1) variar el mapeo de regla entre bloques (a veces ABIERTO=retener) para forzar flexibilidad, no aprendizaje S-R fijo; (2) introducir coste real por comisión (no solo conteo).

### Problemas detectados
- **Subpotencia de la métrica gobernante:** `NO_GO_RATIO = 0.28` × 20 trials ≈ 5-6 no-go < `MIN_NOGO = 6` (`EM1PausaGenerator.kt:79`, `DefaultEmotionMetricsEngine.kt:181`). La métrica primaria del ejercicio nace casi siempre no-confiable. Fallo de diseño de medición grave.
- **Interferencia afectiva casi nunca medible:** requiere `MIN_CELL = 5` celdas congruentes E incongruentes de HITs (`DefaultEmotionMetricsEngine.kt:41,184`). Con ~14 go-trials divididos, raramente se alcanzan 5+5 con RT válido. La métrica "estrella afectiva" es decorativa.
- **Aprendizaje perceptivo del marco:** el borde de 6dp vs 2dp (`EmotionGoNoGoField.kt:48`) se aprende en una sesión; la "mejora" es discriminación visual, no inhibición.
- **`congruent` en no-go mal definido:** marco cerrado + tono negativo se marca `congruent = negative` (`EM1PausaGenerator.kt:71-75`), pero el coste de interferencia solo se computa sobre HITs (go-trials), así que esa congruencia de no-go nunca entra en la métrica. Código muerto semántico.

### Riesgos de falsa mejora
- **Efecto práctica perceptiva:** alto. La comisión baja rápido por familiaridad con el marco, no por control.
- **Banco finito de 24 palabras** (`EmotionContent.kt:16-41`): memorización trivial; pero como el tono no es la respuesta, esto no infla el score directamente — solo elimina cualquier valor afectivo tras 2 sesiones.
- **Dificultad falsa:** `INHIB_SPAN = 0.12` mueve la ratio no-go de 0.28 a 0.40 como máximo. El techo de dificultad es bajísimo; un usuario competente toca el techo en 2-3 sesiones.

### Métricas recomendadas
- **Principal:** d-prime (sensibilidad de señal) en vez de comisión cruda — separa criterio de discriminabilidad. La comisión sola confunde "impulsivo" con "sesgo de respuesta liberal".
- **Secundarias:** SSRT-like si se añade stop-signal; coste de interferencia afectiva (ya existe, pero necesita ≥30 trials para ser medible).
- **Consistencia:** RT-CV de go-trials (ya disponible en otros paradigmas, ausente aquí).
- **Engañan hoy:** `COMMISSION_RATE` con n<6 mostrada como número, y `ANTICIPATIONS` con `IMPULSIVE_FLOOR_MS = 350ms` (`DefaultEmotionMetricsEngine.kt:187`) — un umbral arbitrario sin calibración por usuario.

### Progresión recomendada
Subir trials/bloque a ≥24 para que la comisión sea medible. Curva: no-go de 0.25→0.50, ventana de respuesta decreciente (1500→700ms), y a partir de dominio, introducir reversiones de regla. Zona óptima: comisión 0.10-0.25. Hoy el techo se alcanza demasiado pronto (fondo de tarea).

### Rediseño propuesto
Convertir en **stop-signal afectivo** real: estímulo go que a veces dispara una señal de stop con SOA adaptativo (staircase 1-up/1-down → SSRT). El afecto entra como modulador: señales de stop tras palabra negativa miden si la carga afectiva degrada la inhibición (interacción real, medible con suficientes trials). Métricas: SSRT, SSRT-bajo-carga-afectiva vs neutra. Progresión: SOA adaptativo por trial. Testeable: SSRT debe estabilizarse y el delta afectivo debe ser positivo y replicable. Superior porque mide un constructo inhibitorio validado (SSRT) y la interacción afecto×inhibición que el diseño actual finge medir pero no logra.

### Puntuación 0-10 (los 15 ejes)
1) Mejora cognitiva real: 3 | 2) Transferencia: 2 | 3) Claridad mecanismo: 8 | 4) Progresión: 3 | 5) Medición: 3 | 6) Personalización: 3 | 7) Resistencia a práctica: 2 | 8) Metacognición: 1 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin riesgo): 8 | 11) Riesgo pseudoprogreso (10=sin riesgo): 3 | 12) Adherencia: 4 | 13) Escalabilidad perfiles: 4 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 8
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Rediseñar parcialmente (a stop-signal afectivo) o fusionar con EM8 (son el mismo paradigma con distinta ratio).

---

## Ejercicio: Recuperación tras error (EM2_RECUPERACION)

### Diagnóstico rápido
Es una discriminación perceptiva 2-alternativa ("¿qué barra es más alta?", `EmotionTwoAltField.kt:45`) donde la dificultad baja la discriminabilidad para forzar errores y medir recuperación post-error. El constructo (recuperación tras error) es el más interesante del módulo, PERO el host es perceptivo-neutro: no hay nada emocional. El "error" es perceptual, no afectivo. Vale como tarea de control post-error; es engañoso llamarlo "emocional".

### Mecanismo cognitivo
Monitorización de errores y recuperación de rendimiento tras fallo (post-error slowing/accuracy, un constructo de control cognitivo real con base en ERN/Pe). El compuesto condicional está bien pensado: penaliza el enlentecimiento SOLO si la precisión no se recuperó, y penaliza impulsividad post-error (`DefaultEmotionMetricsEngine.kt:160-166`). Eso es ciencia decente.
- **Qué produce adaptación:** bajar discriminabilidad vía `POST_ERROR_LOAD` (`EM2RecuperacionGenerator.kt:28-29`) hasta que aparecen errores normales.
- **Qué SOBRA:** nada del host es emocional. La "recuperación emocional tras error" es una analogía, no el constructo medido.
- **Qué CONTAMINA:** la discriminabilidad determinista (`EmotionSessionStateHolder.kt:53-59`) significa que un usuario con buena visión y atención NUNCA falla los trials fáciles; los errores solo ocurren en los `harder` (disc=0.45). El "error" depende de agudeza perceptiva, no de regulación. Un usuario puede tener 0 errores y la métrica gobernante sale `unavailable` (`DefaultEmotionMetricsEngine.kt:90-93`).
- **Qué es práctica de tarea:** comparar dos alturas de barra. Cero valor de regulación emocional.

### Potencial de transferencia
**Muy baja a la vida real como "regulación emocional".** El catálogo dice `PLAUSIBLE` (`EmotionExerciseCatalog.kt:48`), lo cual es OPTIMISTA y discutible: la recuperación post-error en una tarea perceptiva no se ha demostrado que transfiera a recuperación emocional tras un fallo real (despido, discusión). Es transferencia cercana, como mucho, dentro de tareas de rendimiento. La etiqueta debería ser `NOT_DEMONSTRATED`. Para aumentar transferencia: el "feedback negativo" tras error tendría que tener carga afectiva real (no un perceptual neutro), y medir si el usuario mantiene estrategia tras un fallo *significativo*.

### Problemas detectados
- **EM2 ≈ EM11 idénticos:** `EM11ContinuidadGenerator.kt` es EM2 con más trials plantados (`HARD_EVERY=3` vs `5`, `MIN_HARD=4` vs `3`). Mismo payload, mismo renderer, misma métrica `POST_ERROR_RECOVERY`. Redundancia casi total (ver veredicto).
- **Errores no garantizados:** los trials `harder` tienen disc=0.45 pero la respuesta correcta sigue siendo determinista (barra mayor); un usuario atento acierta igualmente. El generador "planta trials difíciles" pero NO planta errores. La premisa "post-error recovery measurable" (`EM2RecuperacionGenerator.kt:14`) depende de que el usuario falle, lo cual no está garantizado por diseño.
- **`MIN_POST_ERROR = 3` pares con ~20 trials:** si el usuario comete <3 errores (probable en usuarios competentes), la métrica gobernante del ejercicio sale no-confiable. El ejercicio puede "no medir nada" en sus mejores usuarios — un fallo de medición que castiga al competente.
- **`discriminability` mapeada a altura de barra** (`leftMagnitude`/`rightMagnitude`, `EmotionSessionStateHolder.kt:54-59`): a disc=0.45, gap=0.18, las barras difieren ~18% de altura — perfectamente discriminable visualmente. La "near-threshold" no es near-threshold en absoluto.

### Riesgos de falsa mejora
- **Inflado del score gobernante por ausencia de errores:** si no hay 3+ errores, `unavailable` (correcto), pero el dashboard puede interpretar "sin datos" como neutro; el usuario percibe que "le va bien" sin medición.
- **Práctica perceptiva:** comparar barras se automatiza; los errores bajan, la recuperación deja de medirse → falsa sensación de dominio.
- **Banco infinito (generado), sin memorización** — el único punto fuerte anti-práctica.

### Métricas recomendadas
- **Principal:** post-error accuracy adjustment (PEA) condicionado, ya implementado decentemente; mantener pero exigir ≥6 pares.
- **Secundarias:** post-error slowing (existe), recuperación de RT-CV tras error.
- **Engaña hoy:** la métrica gobernante sale `unavailable` justo en usuarios buenos; eso no es "honesto", es "no mide". Y `POST_ERROR_RECOVERY` con solo 3 pares es estadísticamente ruidoso.

### Progresión recomendada
Forzar una tasa de error objetivo (~25-30%) con staircase psicofísico real (no plantado fijo), de modo que SIEMPRE haya suficientes errores para medir recuperación independientemente de la habilidad. Esto resuelve el "techo del competente".

### Rediseño propuesto
Sustituir el host perceptivo por una tarea con **error afectivamente cargado pero leve**: p.ej. una clasificación rápida donde el feedback de error es un aviso neutro pero la siguiente decisión es bajo presión temporal. Medir: ¿mantiene precisión y estrategia en el trial post-error? Staircase para fijar tasa de error en 30%. Métricas: PEA, post-error strategy persistence. Superior porque (a) garantiza datos de recuperación en todos los niveles, (b) acerca el constructo a "recuperarme tras un fallo que importa" en vez de "tras no ver bien dos barras".

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 2 | 3) Claridad mecanismo: 7 | 4) Progresión: 4 | 5) Medición: 4 | 6) Personalización: 4 | 7) Resistencia práctica: 3 | 8) Metacognición: 2 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 4 | 13) Escalabilidad: 5 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 4.0/10**
**Decisión final:** Rediseñar parcialmente (staircase + carga afectiva real) y fusionar EM11 dentro de EM2.

---

## Ejercicio: Separar emoción, pensamiento, impulso y hecho (EM3_SEPARADOR)

### Diagnóstico rápido
Clasificación de 4 categorías: el usuario lee una frase ("Siento tensión en los hombros") y la etiqueta como Estado interno / Pensamiento / Impulso / Hecho (`EmotionFourCategoryField.kt`, `EmotionContent.kt:50-95`). ESTE es el único ejercicio del módulo con un mecanismo cognitivo-emocional genuino (defusión / granularidad emocional). Es el más valioso del módulo y, a la vez, el peor medido por un banco finito de 40 ítems.

### Mecanismo cognitivo
Defusión cognitiva y granularidad emocional: distinguir estado interno de pensamiento de impulso de hecho es un componente real de la regulación (reappraisal se apoya en ello; la granularidad emocional predice mejor regulación en la literatura de Feldman Barrett). La mecánica está alineada con el objetivo.
- **Qué produce adaptación:** declaradamente, "ambigüedad de categoría" vía ventana de respuesta — pero el generador NO aplica dificultad (`EM3SeparadorGenerator.kt:19-20`, `@Suppress("UnusedParameter")`). El banco es fijo y pre-balanceado; no hay progresión real de dificultad de contenido.
- **Qué SOBRA:** nada estructural; el problema es escasez, no exceso.
- **Qué CONTAMINA:** el banco de entrenamiento son 28 ítems (40 menos 12 eval, `EmotionContent.kt:50-95`). Con 20 trials/sesión, en la sesión 2 el usuario ya ha visto casi todo el banco. Se mide memorización frase→categoría, no la habilidad de separar.
- **Qué es práctica de tarea:** memorizar que "Siento tensión" = Estado interno. Tras 2 sesiones es recall, no clasificación.

### Potencial de transferencia
**Baja-media (la más alta del módulo), pero solo con banco grande y generativo.** El catálogo lo marca `NOT_DEMONSTRATED` (`EmotionExerciseCatalog.kt:61`), honesto. La granularidad/defusión tiene la base teórica más sólida para transferir a regulación real. Pero con 28 ítems memorizables, la transferencia tiende a NULA por overfitting al banco. Para subir: banco de 200+ ítems generados composicionalmente, ítems ambiguos deliberados en el eje pensamiento↔hecho, e items autorreferenciales del propio estado del usuario.

### Problemas detectados
- **Banco finito y minúsculo:** 28 ítems de entrenamiento (`EmotionContent.kt`). El comentario del propio código admite "target = 80; v1 ships ≥30" (`EmotionContent.kt:8`). Memorización garantizada en 2-3 sesiones. Es el fallo más grave del ejercicio más valioso.
- **Métrica de separación ignora 2 de 4 categorías:** `EMOTION_THOUGHT_SEPARATION` solo usa INTERNAL_STATE + THOUGHT (`DefaultEmotionMetricsEngine.kt:109-115`). IMPULSE y FACT no entran en la métrica estrella. Un usuario que confunde sistemáticamente impulso↔hecho (el error más común y más importante para la regulación) saca separación = 1.0 si acierta estado↔pensamiento. La métrica nombra mal lo que mide.
- **Matriz de confusión construida pero no explotada:** `EmotionSessionAssembler.kt:37-49` calcula la confusión 4×4 completa, pero el `EmotionBlockResult` la guarda y nada la convierte en métrica de separación. El dato más rico (qué confunde con qué) se descarta para la puntuación.
- **`GRANULARITY_INDEX` trivial:** = categorías-distintas-acertadas / 4 (`DefaultEmotionMetricsEngine.kt:117-119`). Acertar 1 de cada categoría = 1.0. No mide granularidad; mide cobertura de categorías. Engañoso.
- **Sin dificultad real:** el `@Suppress("UnusedParameter")` (`EM3SeparadorGenerator.kt:19`) confirma que la dificultad declarada (ambigüedad) no existe en generación. La progresión es ficticia.

### Riesgos de falsa mejora
- **Memorización del banco:** riesgo MÁXIMO. 28 ítems, mapeo fijo frase→categoría. La precisión sube a ~100% por recall en sesión 3. Pseudoprogreso puro.
- **Score inflado por la métrica de 2 categorías:** separación = 1.0 fácil ignorando impulso/hecho.
- **Eval holdout no usado:** los 12 ítems `isEval=true` se reservan "para Ola-4 holdout" (`EmotionContent.kt:44`), pero el holdout no existe; no hay ninguna defensa activa contra el overfitting hoy.

### Métricas recomendadas
- **Principal:** precisión sobre ítems NUNCA vistos (holdout rotatorio) — la única defensa contra memorización. Implementarla YA, no en "Ola 4".
- **Secundarias:** confusión específica impulso↔hecho y pensamiento↔hecho (de la matriz que ya se calcula); d-prime por categoría.
- **Granularidad real:** entropía/diversidad de uso correcto de categorías ponderada por dificultad del ítem, no conteo de categorías tocadas.
- **Engañan hoy:** `EMOTION_THOUGHT_SEPARATION` (ignora mitad de categorías) y `GRANULARITY_INDEX` (cobertura disfrazada de granularidad).

### Progresión recomendada
Banco generativo composicional (plantilla + sujeto + objeto → cientos de frases). Dificultad por ambigüedad real: ítems frontera (p.ej. "Siento que voy a fallar" = pensamiento disfrazado de estado). Zona óptima 75-85% en ítems nuevos. Holdout rotatorio para que la precisión refleje habilidad, no memoria.

### Rediseño propuesto
**Defusión adaptativa con banco generativo y holdout obligatorio.** Generar ítems composicionalmente; etiquetar cada uno con su eje de confusión esperado; el adaptativo sube la proporción de ítems frontera según dominio. Métrica gobernante = precisión en holdout (ítems nunca presentados a ESTE usuario). Explotar la matriz de confusión para feedback específico ("confundes impulso con hecho"). Superior porque ataca el único riesgo letal (memorización), mide separación en las 4 categorías, y convierte el dato más rico (confusión) en señal accionable. Testear: precisión-holdout debe correlacionar con precisión-entrenamiento solo si hay aprendizaje real; si diverge, es memorización.

### Puntuación 0-10
1) Mejora cognitiva: 5 | 2) Transferencia: 4 | 3) Claridad mecanismo: 8 | 4) Progresión: 2 | 5) Medición: 3 | 6) Personalización: 2 | 7) Resistencia práctica: 1 | 8) Metacognición: 6 | 9) Valor emocional: 7 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 2 | 12) Adherencia: 5 | 13) Escalabilidad: 3 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 10
**Nota global del ejercicio: 4.5/10** (potencial 8/10 con rediseño)
**Decisión final:** Rediseñar parcialmente — es la joya del módulo, pero con banco finito es pseudoprogreso. Prioridad máxima de mejora.

---

## Ejercicio: Anti-impulso por comisión (EM8_ANTIIMPULSO)

### Diagnóstico rápido
Es EM1 con más no-go (0.40 vs 0.28) y sin balanceo de congruencia afectiva (`EM8AntiImpulsoGenerator.kt:49`). Mismo renderer, mismo clasificador, misma métrica de comisión. Es una variante de parámetro de EM1, no un ejercicio distinto. Su única ventaja sobre EM1: la métrica de comisión SÍ alcanza confianza (más no-go).

### Mecanismo cognitivo
Idéntico a EM1: control inhibitorio de comisión. El único cambio funcional es que `NO_GO_RATIO = 0.40` × 20 ≈ 8 no-go ≥ `MIN_NOGO = 6` → la métrica gobernante es confiable, a diferencia de EM1.
- **Qué produce adaptación:** ratio no-go vía `INHIBITORY_DEMAND` (0.40→0.55).
- **Qué SOBRA:** la palabra afectiva sigue presente (`EM8AntiImpulsoGenerator.kt:33`) pero su congruencia ya no se mide (no hay `AFFECTIVE_INTERFERENCE_COST` en sus métricas, `EmotionExerciseCatalog.kt:124`). Es decorado puro, ahora reconocido como tal.
- **Qué CONTAMINA:** mismo aprendizaje perceptivo del marco que EM1.
- **Qué es práctica de tarea:** igual que EM1.

### Potencial de transferencia
**Muy baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:126`) — de nuevo optimista para un go/no-go. Misma crítica que EM1: inhibición específica de tarea, sin transferencia demostrada a control de impulsos vital. La etiqueta debería ser `TASK_ONLY` como EM1, dado que es el MISMO paradigma.

### Problemas detectados
- **Redundancia con EM1:** mismo payload (`AffectiveGoNoGo`), mismo renderer (`EmotionGoNoGoField`), mismo clasificador, mismo eje gobernante (`INHIBITORY_DEMAND`). Difieren en 3 constantes. No justifica ser un "ejercicio" separado en un menú de 6.
- **Inconsistencia de etiqueta de transferencia:** EM1=`TASK_ONLY`, EM8=`PLAUSIBLE` para el mismo paradigma (`EmotionExerciseCatalog.kt:30` vs `:126`). Una de las dos es deshonesta.
- **Mismo techo de dificultad bajo** que EM1; `INHIB_SPAN=0.15` → tope 0.55 no-go.
- **Word `congruent` calculado pero nunca usado** (`EM8AntiImpulsoGenerator.kt:40`): `congruent = frameOpen != word.negative` se guarda pero EM8 no computa interferencia. Código semánticamente muerto.

### Riesgos de falsa mejora
- **Práctica perceptiva del marco:** alto, igual que EM1.
- **Sensación de "dos ejercicios"** cuando es uno: infla la percepción de cobertura del módulo sin añadir entrenamiento.
- Banco de 24 palabras irrelevante (no afecta score).

### Métricas recomendadas
- **Principal:** d-prime, no comisión cruda (separar criterio de sensibilidad).
- **Secundarias:** SSRT si se migra a stop-signal; RT-CV de go.
- **Engaña hoy:** comisión cruda confunde sesgo de respuesta con impulsividad; un usuario "lento y cuidadoso" puntúa igual que uno con inhibición real fuerte.

### Progresión recomendada
Si se mantiene separado de EM1, debe ser el "nivel avanzado" del paradigma go/no-go: stop-signal con SOA adaptativo. Si no, fusionar.

### Rediseño propuesto
Fusionar con EM1 en un único ejercicio "Inhibición" con dificultad continua (ratio no-go + eventual señal de stop). EM8 desaparece como entrada de menú y se convierte en el tramo alto de la curva de dificultad de EM1. Superior porque elimina redundancia, concentra los trials en una sola tarea bien medida, y libera un slot del menú para una capacidad ausente (reappraisal/flexibilidad).

### Puntuación 0-10
1) Mejora cognitiva: 3 | 2) Transferencia: 2 | 3) Claridad mecanismo: 8 | 4) Progresión: 3 | 5) Medición: 5 (comisión sí confiable) | 6) Personalización: 3 | 7) Resistencia práctica: 2 | 8) Metacognición: 1 | 9) Valor emocional: 1 | 10) Riesgo fatiga (10=sin): 8 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 3 | 13) Escalabilidad: 4 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 7
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Fusionar con EM1 (mismo paradigma; EM8 = tramo de dificultad alta).

---

## Ejercicio: Separación bajo presión leve (EM10_SEP_PRESION)

### Diagnóstico rápido
Es EM3 con la ventana de respuesta multiplicada por 0.65 (`EM10SepPresionGenerator.kt:41`). Mismo banco, mismo renderer, misma métrica. Variante de parámetro de EM3 (presión temporal en lugar de ventana normal). Hereda TODOS los problemas de EM3 (banco finito, métrica de 2 categorías) y añade el suyo: presión temporal aplicada a una tarea de clasificación que NO debería premiar velocidad.

### Mecanismo cognitivo
Igual que EM3 (defusión/granularidad) bajo presión temporal. La idea —separar bajo presión— es razonable conceptualmente (regular bajo tiempo es más vital que en calma). Pero:
- **Qué produce adaptación:** la ventana recortada (`PRESSURE_FACTOR=0.65`, mínimo 900ms). Fijo, no adaptativo dentro del generador.
- **Qué SOBRA:** nada nuevo respecto a EM3; misma escasez de banco.
- **Qué CONTAMINA:** la presión temporal sobre una tarea conceptual convierte la métrica de precisión en una mezcla velocidad×conocimiento. Un usuario que CONOCE las categorías pero es lento sale "peor en separación", cuando su separación es perfecta. La presión contamina el constructo que dice medir.
- **Qué es práctica de tarea:** memorización del banco + velocidad de pulsación. Doble pseudoprogreso.

### Potencial de transferencia
**Baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:150`). Mismo banco memorizable que EM3 → con práctica, la transferencia tiende a nula. La presión temporal no añade transferencia a regulación real; añade un confound de velocidad. Para mejorar: presión solo como modulador medido por separado (precisión-bajo-presión vs precisión-en-calma como índice de robustez), no mezclada en la precisión principal.

### Problemas detectados
- **Redundancia con EM3:** mismo payload `Classification`, mismo banco (`EmotionContent.SEPARATOR_ITEMS`), mismo renderer, mismo clasificador. Difiere en un factor de ventana.
- **Hereda el banco de 28 ítems** → memorización (ver EM3).
- **Hereda la métrica de separación de 2 categorías** (ignora impulso/hecho).
- **Presión confunde precisión:** `CATEGORY_ACCURACY` bajo ventana de 900ms penaliza al lento-pero-correcto. La métrica gobernante mezcla dos constructos.
- **Mínimo de ventana 900ms** puede ser demasiado corto para leer una frase de 5-7 palabras Y decidir entre 4 botones — riesgo de medir velocidad lectora, no separación.

### Riesgos de falsa mejora
- **Memorización del banco** (idéntico a EM3): máximo.
- **Mejora por velocidad de pulsación:** el usuario aprende posiciones de botón (mapeo fijo 2×2, `EmotionFourCategoryField.kt:47-54`) y responde por motor, no por análisis. Pseudoprogreso de velocidad.
- **Dificultad falsa:** "presión" = ventana más corta, que es velocidad disfrazada de habilidad de regulación.

### Métricas recomendadas
- **Principal:** precisión en holdout bajo presión.
- **Secundarias:** índice de robustez = precisión-bajo-presión / precisión-en-calma (mide si la separación aguanta la presión, que ES el constructo interesante).
- **Engaña hoy:** `CATEGORY_ACCURACY` con presión, porque confunde velocidad con conocimiento.

### Progresión recomendada
Mantener separación y presión como ejes ortogonales: medir precisión sin presión primero (conocimiento), luego presión como reto adicional con su propia métrica de robustez. Banco generativo + holdout.

### Rediseño propuesto
Integrar como un MODO de EM3 ("modo presión"), no un ejercicio separado: tras dominar EM3 sin presión, desbloquear presión adaptativa que reduce la ventana mientras la precisión-holdout se mantenga ≥80%. Métrica = índice de robustez bajo presión. Superior porque desacopla conocimiento de velocidad, reutiliza el banco generativo de EM3, y mide algo real (mantener la separación cuando hay prisa) en vez de premiar dedos rápidos.

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 3 | 3) Claridad mecanismo: 6 | 4) Progresión: 2 | 5) Medición: 2 | 6) Personalización: 2 | 7) Resistencia práctica: 1 | 8) Metacognición: 5 | 9) Valor emocional: 6 | 10) Riesgo fatiga (10=sin): 7 | 11) Riesgo pseudoprogreso (10=sin): 1 | 12) Adherencia: 4 | 13) Escalabilidad: 3 | 14) Robustez científica: 4 | 15) Prioridad de mejora: 8
**Nota global del ejercicio: 3.5/10**
**Decisión final:** Fusionar con EM3 (como "modo presión" adaptativo). No merece slot propio.

---

## Ejercicio: Continuidad tras feedback negativo neutro (EM11_FEEDBACK_NEG)

### Diagnóstico rápido
Es EM2 con más trials difíciles plantados (`HARD_EVERY=3`, `MIN_HARD=4`, `EM11ContinuidadGenerator.kt:55-59`). Mismo payload perceptivo de barras, mismo renderer, misma métrica `POST_ERROR_RECOVERY`. Variante de parámetro de EM2. El nombre promete "feedback negativo neutro" pero el feedback es el mismo perceptual de EM2 — no hay aviso de error diferenciado en el código que leí.

### Mecanismo cognitivo
Idéntico a EM2: recuperación post-error en tarea perceptiva 2-AFC. La diferencia es densidad de errores potenciales (más trials near-threshold). Esto SÍ mejora la medición respecto a EM2 (más pares post-error → métrica más probablemente confiable), que es su única ventaja real.
- **Qué produce adaptación:** discriminabilidad vía `POST_ERROR_LOAD` + más trials plantados.
- **Qué SOBRA:** la promesa de "feedback negativo neutro" no está implementada como algo distinto del feedback estándar de EM2. El `displayName` sobre-vende.
- **Qué CONTAMINA:** igual que EM2, errores dependen de agudeza perceptiva, no de regulación.
- **Qué es práctica de tarea:** comparar barras.

### Potencial de transferencia
**Muy baja.** Catálogo: `PLAUSIBLE` (`EmotionExerciseCatalog.kt:162`). Misma crítica que EM2: recuperación post-error perceptiva no transfiere demostradamente a recuperación emocional vital. Para mejorar habría que añadir el "feedback negativo" prometido: un aviso de error explícito (neutro pero salience) y medir continuidad de estrategia tras él.

### Problemas detectados
- **Redundancia casi total con EM2:** mismo payload `TwoAltPerceptual`, mismo renderer `EmotionTwoAltField`, mismo clasificador, misma métrica gobernante, mismo eje `POST_ERROR_LOAD`. Difieren en 3 constantes de plantado (`EM11ContinuidadGenerator.kt:55-59` vs `EM2RecuperacionGenerator.kt:58-63`).
- **Promesa incumplida:** "tras un aviso neutro de error" (`EmotionExerciseCatalog.kt:164`) — no existe un aviso de error distinto en el flujo. El feedback inmediato (`EmotionStrings.NEUTRAL_NEGATIVE="No esa vez"`) ni siquiera se ve cableado en el holder que leí.
- **Hereda "errores no garantizados"** de EM2 (la respuesta correcta sigue siendo determinista).
- **Hereda `MIN_POST_ERROR=3`** ruidoso.

### Riesgos de falsa mejora
- **Doble conteo de cobertura:** EM2 y EM11 cuentan como 2 de 6 ejercicios, siendo el mismo paradigma con distinta densidad de plantado. El módulo aparenta más variedad de la que tiene.
- **Práctica perceptiva** baja errores → la recuperación deja de medirse.

### Métricas recomendadas
- **Principal:** post-error accuracy adjustment con ≥6 pares.
- **Secundarias:** continuidad de estrategia (¿cambia el patrón de respuesta tras error?), abandono post-error.
- **Engaña hoy:** misma que EM2; además el nombre sugiere medir reacción a feedback negativo que no se mide.

### Progresión recomendada
Si se mantiene, debe implementar de verdad el feedback negativo salience y medir continuidad tras él. Si no, fusionar en EM2.

### Rediseño propuesto
Fusionar en EM2 como el tramo de "alta densidad de error". Si se quiere un ejercicio de "continuidad tras feedback negativo" real, hay que construirlo: tarea con feedback de error explícito (animación/sonido neutro pero notable) y métrica de cuánto se degrada el rendimiento en los 2-3 trials siguientes. Superior porque (a) elimina la redundancia, (b) si se conserva como ejercicio propio, al menos cumpliría su nombre.

### Puntuación 0-10
1) Mejora cognitiva: 4 | 2) Transferencia: 2 | 3) Claridad mecanismo: 6 | 4) Progresión: 4 | 5) Medición: 5 (más pares que EM2) | 6) Personalización: 4 | 7) Resistencia práctica: 3 | 8) Metacognición: 2 | 9) Valor emocional: 2 | 10) Riesgo fatiga (10=sin): 7 | 11) Riesgo pseudoprogreso (10=sin): 4 | 12) Adherencia: 4 | 13) Escalabilidad: 5 | 14) Robustez científica: 5 | 15) Prioridad de mejora: 6
**Nota global del ejercicio: 4.0/10**
**Decisión final:** Fusionar con EM2 (mismo paradigma; EM11 = tramo de alta densidad de error) O implementar de verdad el feedback negativo prometido.

---

## Ejercicios de catálogo (no cableados): EM4, EM5, EM6, EM7, EM9, EM12, EM13, EM14, EM15

No tienen generador ni renderer (`EmotionExerciseGenerators.kt:18-25` registra solo 6; el resto vive como `EmotionExerciseDefinition` sin código ejecutable). No son auditables como ejercicios porque NO existe la mecánica, el clasificador de aciertos ni la presentación — solo una ficha declarativa. Evaluación breve del DISEÑO declarado (lo único auditable):

- **EM4 Calibrador / EM14 Confianza (`CALIBRATION_ERROR`):** son los DOS ejercicios con mayor valor metacognitivo potencial (calibración confianza↔acierto es un constructo medible y entrenable de verdad). Que estén sin cablear es el mayor hueco del módulo. `EmotionMetricKey.CALIBRATION_ERROR` existe pero NINGÚN generador wired produce datos de calibración → la métrica nunca se computa. **Prioridad de implementación: máxima.**
- **EM5 Sesgo afectivo (`AFFECTIVE_DECISION_QUALITY`):** el único con potencial de transferencia a decisiones reales bajo carga afectiva. Requiere renderer de texto + banco de 50 escenarios (no existe). Alto valor, alto coste.
- **EM6 Tolerancia (`FRUSTRATION_TOLERANCE_INDEX`):** catálogo lo marca `PLACEBO_RISK` (`EmotionExerciseCatalog.kt:101`) — honestidad notable; el propio diseño admite riesgo de placebo. Bien etiquetado.
- **EM7 Reentrada / EM13 Variabilidad (`REGULATION_STABILITY`, `RT_CV`):** miden estabilidad/variabilidad de RT. Constructo débil como "regulación"; RT-CV es ruido de atención, no regulación emocional. Riesgo de medir fatiga disfrazada de "estabilidad".
- **EM9 Etiquetado (`GRANULARITY_INDEX`):** solapa con EM3; la granularidad ya se (mal)mide en EM3. Redundante con el ejercicio que sí está cableado.
- **EM12 Espera (`ANTICIPATIONS`):** go/no-go temporal; otra variante inhibitoria. Tercera redundancia del paradigma EM1/EM8.

**Riesgo de catálogo:** 9 de 15 ejercicios son fichas sin código. El menú v1 NO los muestra (`implemented` se deriva del registry, `EmotionExerciseCatalog.kt:231`), así que no engañan al usuario. Pero el diseño concentra los 6 cableados en SOLO 3 paradigmas (go/no-go, 2-AFC perceptivo, clasificación), dejando fuera justo los 2 más valiosos (calibración, decisión afectiva).

---

## Veredicto del módulo "Emociones (regulación cognitivo-emocional) [PRIORIDAD]"

### Capacidades bien cubiertas / huecos / capacidades AUSENTES
- **Cubiertas (con reservas):** inhibición de comisión (EM1/EM8, sobre-representada, 2 ejercicios para lo mismo) y monitorización post-error (EM2/EM11, también duplicada). Defusión/separación (EM3/EM10) está cubierta conceptualmente pero rota por banco finito.
- **Huecos:** la separación (EM3) tiene el banco demasiado pequeño (28 ítems) y métrica que ignora 2 de 4 categorías.
- **AUSENTES por completo (no cableadas):** calibración metacognitiva (EM4/EM14 — la capacidad de MAYOR valor entrenable y demostrable), calidad de decisión bajo sesgo afectivo (EM5), reappraisal/reinterpretación (no existe en ningún ejercicio), y flexibilidad cognitiva (cambio de regla — ningún ejercicio cambia la regla entre bloques). **El módulo entrena 3 paradigmas y llama a eso "regulación cognitivo-emocional".**

### Ejercicios redundantes o a fusionar
- **EM1 + EM8 + (EM12) → un ejercicio "Inhibición"** con dificultad continua (ratio no-go → stop-signal). Hoy son 2-3 entradas para el mismo go/no-go.
- **EM2 + EM11 → un ejercicio "Recuperación"** con densidad de error adaptativa. Hoy son 2 entradas idénticas salvo 3 constantes.
- **EM3 + EM10 + (EM9) → un ejercicio "Separación"** con presión como modo opcional. Hoy 2-3 entradas para la misma clasificación.
- **Resultado real:** los "6 ejercicios cableados" son **3 paradigmas**. El menú aparenta el doble de variedad de la que existe.

### Riesgos sistémicos del módulo
1. **Medición común sub-dimensionada:** `BLOCKS=2 × TRIALS_PER_BLOCK=10 = 20 trials/sesión` (`EmotionSessionStateHolder.kt:410-411`). Casi todas las métricas gobernantes rozan o caen bajo su `MIN_*` (comisión n<6 en EM1; post-error pares <3 en usuarios buenos). El módulo frecuentemente devuelve `unavailable`/`confident=false` — es honesto pero significa "no mide". 20 trials es insuficiente para estimación estable de cualquiera de estos constructos.
2. **Banco de contenido finito (EM3/EM10):** 28 ítems memorizables → el ejercicio más valioso produce pseudoprogreso por recall en 2-3 sesiones. El holdout que lo defendería está "diferido a Ola 4" y no existe.
3. **Trend/longitudinal fabricado:** `aggregateOf` fija `trend = ProfileTrend.UNKNOWN` y `transferConfidence = NOT_DEMONSTRATED` SIEMPRE (`EmotionSessionStateHolder.kt:368,370`). No hay cálculo de mejora longitudinal real — el snippet de perfil no puede mostrar progreso porque no se computa. Honesto, pero el módulo no tiene forma de demostrar que mejora nada.
4. **Adaptación que no progresa la habilidad real:** el adaptativo mueve un eje (`STEP_UP=0.07`) sobre dificultad de TAREA (ratio no-go, discriminabilidad, ventana), no sobre el constructo. Más no-go o barras más parecidas no entrenan mejor regulación; entrenan la tarea.
5. **El "afecto" es decorativo en 4 de 6 ejercicios cableados:** go/no-go (palabra distractora cuyo efecto casi nunca se mide), 2-AFC (barras perceptivas neutras). Solo EM3/EM10 tienen contenido genuinamente emocional-cognitivo. El módulo es, en su mayoría, tareas de control cognitivo con etiqueta emocional.
6. **Seguridad sobre-ingenierizada vs. medición sub-ingenierizada:** hay un sistema elaborado de proxies de overload, pause governor, induction intensity 1-5, check-in (`OverloadDetector`, `PauseGovernor`, `EmotionSafetyLimits`). Es buen trabajo de seguridad. Pero se ha invertido más rigor en NO dañar que en MEDIR/ENTRENAR algo real. El módulo es seguro y honesto, pero su eficacia cognitiva es débil.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Cablear calibración metacognitiva (EM4/EM14) y explotar `CALIBRATION_ERROR`.** Es la capacidad de mayor valor entrenable y demostrable que YA tiene clave de métrica pero ningún generador. Pedir confianza (1-5) tras cada respuesta en CUALQUIER paradigma existente y medir Brier/calibración convierte 20 trials inertes en señal metacognitiva real, transversal a todos los ejercicios, sin banco nuevo. Coste bajo, impacto máximo.
2. **Arreglar EM3 (la joya) con banco generativo + holdout obligatorio + métrica de separación sobre las 4 categorías.** Es el único ejercicio con base teórica sólida de transferencia (granularidad/defusión). Hoy lo mata el banco de 28 ítems y la métrica que ignora impulso/hecho. Banco composicional de 200+ ítems, precisión-en-holdout como métrica gobernante, y explotar la matriz de confusión (ya calculada, hoy descartada) para feedback específico.
3. **Subir la potencia de medición y reducir a 3 paradigmas distintos.** Fusionar EM1/EM8, EM2/EM11, EM3/EM10 en 3 ejercicios con dificultad continua, y subir trials/sesión (≥30, idealmente con staircase que fije tasa de error/no-go target) para que las métricas gobernantes alcancen confianza de forma fiable. Con los slots liberados, añadir un paradigma genuinamente ausente (reappraisal o flexibilidad/cambio-de-regla). Esto convierte "6 ejercicios que aparentan variedad y rara vez miden" en "menos ejercicios, bien medidos, con una capacidad nueva real".

---


## Modulo: Creatividad / Inteligencia

Confirmed. There is NO adaptive engine, NO baseline, NO longitudinal trend analysis, NO difficulty progression — `historyForUser` exists but nothing reads it to inform scoring, difficulty, or feedback. The "Difficulty bounds" comment in the catalog is a lie: there are no difficulty bounds, only `minIdeas`/`targetIdeas` static constants. EX02's "rotation" is never enforced (no per-idea category check). I have everything I need to write the brutal audit.

# Auditoría cognitiva — Módulo "Creatividad / Inteligencia (pensamiento divergente)" de MindBoost

> Auditoría basada en el **código real** (no en el plan ni en los docstrings). Todas las rutas son relativas a `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/`.
>
> **Corrección de premisa del encargo.** El brief asume un rubric con cuatro ejes (fluidez/flexibilidad/originalidad/elaboración). **Eso no existe en el código.** `CreativityRubricScorer.kt:21-37` solo computa DOS señales: `fluency` (conteo de ideas distintas) y `elaboration` (media de palabras por idea). **Flexibilidad y originalidad NO se miden en ninguna parte.** Tampoco existe `TrialClassifier`, ni motor de métricas tipo Enfoque/Velocidad, ni motor adaptativo. El módulo son **dos prompts estáticos + un contador de palabras**. Audito lo que hay, no lo que el plan prometía.
>
> **Inventario real de ejercicios reachable** (`CreativityExerciseCatalog.kt:33-36`): exactamente **2** — `EX01_SUPPRESS_FIRST_IDEA` y `EX02_CATEGORY_ROTATION`. No hay más. El `enum CreativityExerciseId` (`CreativityExercise.kt:13-16`) solo declara esos dos; el plan `05_exercise_review.md` referenciado no añadió ninguno más al código.

---

## Ejercicio: Suprime la primera idea (EX01_SUPPRESS_FIRST_IDEA)

### Diagnóstico rápido
Es el clásico "usos alternativos" (Guilford AUT) con una instrucción extra de "salta la primera idea obvia" que **el sistema jamás verifica**. Cognitivamente la consigna apunta a inhibición de respuesta prepotente, pero el scorer solo cuenta ideas distintas y palabras: un usuario que escribe SOLO ideas obvias puntúa idéntico a uno que las suprime. La instrucción es decorativa. Vale como tarea-semilla; como está medido, no entrena lo que dice entrenar.

### Mecanismo cognitivo
- **Declarado:** inhibición de la respuesta dominante + pensamiento divergente (generar usos no-obvios).
- **Realmente entrenado:** generación divergente por fluidez bruta (`CreativityRubricScorer.kt:25-26`: `ideas.distinct()` → tamaño). El componente inhibitorio ("skip the first, most obvious one") es **puro texto de instrucción** (`CreativityExerciseCatalog.kt:19`); no hay banco de "respuestas obvias", ni penalización por obviedad, ni detección de la primera idea. La parte que produciría adaptación cognitiva (suprimir lo prepotente y rebuscar lo remoto) **no toca la medición**.
- **Qué SOBRA:** `elaboration` = palabras/idea (`CreativityRubricScorer.kt:27-31`). Premia escribir frases largas, no ideas mejores. "Usar el ladrillo como pisapapeles muy pesado y útil para la oficina" puntúa más en elaboración que "arma", aunque la segunda sea más original. **Contamina**: incentiva verbosidad.
- **Qué es solo práctica de la tarea:** todo. Con 8 objetos (`CreativityPromptCatalog.kt:12-21`) y selección por `Random(seed)` sobre un seed = monotonic start (`CreativityExerciseEngine.kt:18-23`), el usuario reverá los mismos objetos en pocas sesiones.

### Potencial de transferencia
**Muy baja.** La AUT entrena divergencia *en la AUT*. La evidencia de transferencia lejana de los "usos alternativos" a inteligencia fluida o creatividad cotidiana es débil incluso en la literatura bien diseñada; aquí, sin medir originalidad ni flexibilidad, ni siquiera se entrena el sub-proceso interesante. Es mejora-en-la-tarea, casi nada de transferencia cercana. Para subirla: medir y reforzar **originalidad** (rareza respecto al propio histórico) y **flexibilidad** (saltos de categoría), que son los proxies con algún respaldo de divergencia genuina.

### Problemas detectados
1. **La consigna no se mide** — `CreativityExerciseCatalog.kt:19` pide suprimir la primera idea; `CreativityResponseValidator.kt:15-26` y `CreativityRubricScorer.kt` no inspeccionan ni el orden ni la obviedad. La habilidad declarada es invisible al sistema. **[Mejora crítica]**
2. **Originalidad ausente** — no hay rareza, ni frecuencia, ni distancia semántica. Sin IA (bloqueada por `CreativityRuntimePolicy.kt:20`) y sin tablas de frecuencia locales, el constructo central de la creatividad divergente **no existe**. **[Riesgo científico]**
3. **`elaboration` premia verbosidad** — `CreativityRubricScorer.kt:30` divide palabras totales entre ideas; recompensa rellenar, no elaborar conceptualmente.
4. **Dedup demasiado crudo** — `ideas.distinct()` (`CreativityRubricScorer.kt:25`) es igualdad exacta de strings tras `trim()` (`CreativityResponse.kt:26-31`). "arma" y "Arma" cuentan como distintas (no hay lowercase); "pisapapeles" y "pisa papeles" cuentan como distintas. La fluidez se **infla trivialmente** con variantes ortográficas. **[Mejora crítica]**
5. **Banco finito y minúsculo** — 8 objetos (`CreativityPromptCatalog.kt:12-21`). Memorización inevitable.
6. **`minIdeas=3` como única puerta** (`CreativityExerciseCatalog.kt:21`, validado en `CreativityResponseValidator.kt:23-24`): 3 ideas distintas cualquiera = "sesión válida". Fondo bajísimo.

### Riesgos de falsa mejora
- **Inflado por variantes:** sin normalización (lowercase/colapso de espacios) más allá de `trim`, el usuario sube "fluidez" repitiendo casi-duplicados. **[Riesgo científico]**
- **Banco finito:** 8 prompts → memorización de listas de usos por objeto; la fluidez sube por recuperación de memoria, no por divergencia.
- **Feedback siempre positivo:** las tres etiquetas (`CreativityPracticeLabel.kt:13-17`) son `KEEP_PRACTICING / STEADY_PRACTICE / STRONG_LOCAL_SESSION` — no existe una etiqueta negativa ni de estancamiento. Siempre se siente como progreso.
- **`elaboration` como dificultad falsa:** parece sofisticación medir "palabras por idea"; es solo longitud de texto.

### Métricas recomendadas
- **Principal:** Originalidad intra-personal — proporción de ideas que NO aparecen en las últimas N sesiones del propio usuario (banco de frecuencia local construido desde `historyForUser`, que ya existe en `CreativitySessionStore.kt:21` pero **nadie consume**).
- **Secundarias:** Flexibilidad = nº de categorías distintas tocadas (requiere taxonomía local por objeto); Fluidez = ideas distintas **tras normalización fuerte** (lowercase + colapso espacios + stemming básico).
- **Precisión/velocidad:** ratio ideas-únicas / ideas-totales (control de repetición); curva temporal de producción (ideas por intervalo de 30 s vía `elapsedMonotonicMs`, hoy capturado en `CreativityResponse.kt:18` pero **no usado para nada salvo el check `<0`**).
- **Consistencia / fatiga:** caída de tasa de producción en el último tercio del tiempo.
- **Calibración metacognitiva:** pedir al usuario que prediga cuántas ideas "buenas" produjo vs. su originalidad real.
- **Métricas ACTUALES que engañan:** `fluency` sin normalización (inflable) y `elaboration` (verbosidad). Ambas se presentan crudas al usuario en `CreativityRootScreen.kt:211-212` como `$fluency`/`$elaboration` (un `Double` sin formato, p. ej. "Elaboración: 3.6666666666666665"), lo que ni siquiera es presentable.

### Progresión recomendada
Hoy NO hay progresión: `minIdeas`/`targetIdeas` son constantes fijas (3/8) y el comentario "Difficulty bounds are per-exercise domain data" (`CreativityExerciseCatalog.kt:10`) es **falso** — no hay bounds adaptativos. Propuesta:
- **Nivel 1 (fondo):** objeto común, sin restricción, objetivo fluidez.
- **Nivel 2:** exigir ≥K categorías distintas (flexibilidad).
- **Nivel 3:** "modo originalidad" — solo cuentan ideas que no diste en sesiones previas (presiona contra lo prepotente, que es el objetivo declarado).
- **Adaptación:** subir/bajar el objetivo de categorías y el umbral de novedad según la mediana de las últimas 3 sesiones (zona óptima ~70-80% de logro). Evita techo (objetos triviales repetidos) y fondo (3 ideas obvias bastan).

### Rediseño propuesto
**"Cadena rota" (EX01 rediseñado):**
- **Mecánica:** mismo objeto, pero la UI muestra en vivo la "categoría" de cada idea aceptada (mapeada por una **taxonomía local** objeto→{categorías canónicas conocidas}); las primeras 2 ideas en la categoría más obvia se aceptan pero **marcadas en gris y no puntúan originalidad** ("eso es lo obvio"). Puntúan: ideas en categorías nuevas + ideas no vistas en el histórico.
- **Objetivo refinado:** producir usos remotos suprimiendo la categoría dominante — ahora **sí medido**.
- **Dificultad adaptativa:** umbral de categorías y de novedad ligado a `historyForUser`.
- **Métricas:** originalidad intra-personal (principal), flexibilidad (categorías), fluidez normalizada, curva temporal.
- **Feedback:** además de las 3 etiquetas, mostrar "tus ideas más raras de hoy" y "categorías que no exploraste" (accionable).
- **Variantes:** "solo verbos", "solo contextos imposibles".
- **Cómo testearlo:** unit tests deterministas sobre la taxonomía (objeto→categoría) y sobre el dedup normalizado; simulación de 20 sesiones para verificar que la curva de novedad cae (sin banco de frecuencia, no habría señal — eso prueba que la métrica mide algo).
- **Por qué es superior:** alinea medición con consigna (inhibición de lo prepotente), introduce el constructo ausente (originalidad/flexibilidad) sin IA, y usa persistencia que ya existe pero está muerta.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **3** | 2) Transferencia a la vida real: **2** | 3) Claridad del mecanismo: **4** (consigna clara, medición desalineada) | 4) Calidad de la progresión: **1** (no hay) | 5) Calidad de la medición: **2** | 6) Personalización: **1** | 7) Resistencia al efecto práctica: **2** (banco de 8, sin normalización) | 8) Valor metacognitivo: **2** | 9) Valor emocional: **3** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **8** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **2** | 12) Adherencia: **3** | 13) Escalabilidad a perfiles distintos: **2** | 14) Robustez científica: **2** | 15) Prioridad de mejora: **9** (alta)
**Nota global del ejercicio: 3.0/10**
**Decisión final:** Rediseñar parcialmente (alinear medición con la consigna inhibitoria + añadir originalidad/flexibilidad intra-personal + normalización de dedup). Mantenible como **calentamiento** si se quiere algo trivial, pero NO como entrenamiento serio en su estado actual.

---

## Ejercicio: Rotación de categorías (EX02_CATEGORY_ROTATION)

### Diagnóstico rápido
La instrucción pide "rotar a un ángulo fresco con cada idea" (`CreativityExerciseCatalog.kt:27`) — es decir, **flexibilidad de categorías**, el constructo central de Guilford. Pero el scorer es **idéntico** al de EX01 (`CreativityRubricScorer.score` no distingue ejercicio: `CreativitySessionCoordinator.kt:78`): cuenta ideas distintas y palabras. **La rotación —la única razón de ser del ejercicio— no se mide en absoluto.** Es EX01 con otro enunciado y otro banco de 6 temas.

### Mecanismo cognitivo
- **Declarado:** flexibilidad cognitiva / set-shifting (cambiar de categoría conceptual deliberadamente).
- **Realmente entrenado:** la misma fluidez bruta que EX01. No hay clasificación de categorías, no hay detección de "cambio de ángulo", no hay penalización por quedarse en una categoría. El usuario puede dar 10 ideas de la MISMA categoría y obtener `STRONG_LOCAL_SESSION` si `distinctIdeas >= targetIdeas` (`CreativityRubricScorer.kt:43-48`).
- **Qué SOBRA / CONTAMINA:** igual que EX01 — `elaboration` premia verbosidad; el dedup exacto infla fluidez.
- **Qué es solo práctica de la tarea:** todo, y peor que EX01 porque su mecanismo declarado (flexibilidad) es **literalmente inexistente en el código**.

### Potencial de transferencia
**Nula a muy baja.** Como la flexibilidad no se entrena (no se mide ni se refuerza), no hay siquiera mejora-en-la-tarea sobre el constructo objetivo: solo mejora en "escribir muchas cosas distintas". La flexibilidad de categorías sí tiene mejor pedigrí teórico que la fluidez bruta como proxy de divergencia, pero aquí está ausente. Para subir transferencia: **el ejercicio depende por completo de una taxonomía local de categorías** que clasifique cada idea y mida saltos.

### Problemas detectados
1. **El constructo declarado no existe en el código** — "category rotation" sin ninguna noción de categoría. `CreativityRubricScorer.kt` es agnóstico al ejercicio. **[Mejora crítica][Riesgo científico]**
2. **Scorer compartido sin diferenciación** — `CreativitySessionCoordinator.kt:78` llama al mismo `score()` para ambos ejercicios; no hay forma de que EX02 mida algo distinto de EX01. Diseño que hace imposible medir flexibilidad sin refactor.
3. **Banco de 6 temas** (`CreativityPromptCatalog.kt:23-30`) — memorización aún más rápida que EX01.
4. **Mismos defectos de normalización y feedback positivo** que EX01 (dedup exacto, etiquetas siempre amables, `Double` crudo en pantalla).
5. **`minIdeas=4`** (`CreativityExerciseCatalog.kt:28`): 4 strings distintos cualesquiera = válido, sin importar si son una sola categoría.

### Riesgos de falsa mejora
- **Pseudohabilidad:** el usuario cree que entrena flexibilidad; entrena fluidez. **Falsa atribución de mejora a un constructo no ejercitado.** Es el riesgo más grave del módulo: el ejercicio *miente sobre lo que mide*. **[Riesgo científico]**
- **Banco finito (6):** memorización.
- **Inflado por variantes** (sin normalización).
- **Feedback positivo garantizado.**

### Métricas recomendadas
- **Principal:** Flexibilidad = nº de categorías canónicas distintas tocadas / nº de ideas (índice de rotación real), con la taxonomía local por tema.
- **Secundarias:** "switch rate" = proporción de ideas consecutivas que cambian de categoría (mide la *rotación* literal de la consigna, no solo cobertura); originalidad intra-personal; fluidez normalizada.
- **Consistencia / fatiga:** caída del switch rate en el último tercio.
- **Metacognición:** auto-estimación de "cuántas categorías toqué" vs. real.
- **Métricas ACTUALES que engañan:** las DOS (`fluency`, `elaboration`) — ninguna mide flexibilidad, que es el punto entero del ejercicio.

### Progresión recomendada
- **N1:** rotación libre, objetivo = ≥3 categorías.
- **N2:** prohibir 2 ideas seguidas en la misma categoría (la UI rechaza/avisa), objetivo switch-rate ≥0.6.
- **N3:** "categorías obligatorias" rotativas (la app pide explícitamente la siguiente categoría; mide latencia de cambio = coste de switch, un proxy real de flexibilidad).
- **Adaptación:** umbral de categorías/switch-rate ligado al histórico; zona óptima 70-80%.

### Rediseño propuesto
**"Rueda de categorías" (EX02 rediseñado):**
- **Mecánica:** taxonomía local por tema (`"uses for water"` → {higiene, energía, cocina, ocio, industria, supervivencia, arte...}). Cada idea se auto-clasifica por *keywords locales*; la UI muestra la rueda de categorías iluminando las ya tocadas. **Penaliza** repetir categoría inmediata; **premia** cubrir categorías nuevas y el **coste de cambio** (tiempo entre ideas de categorías distintas).
- **Objetivo refinado:** maximizar cobertura y switch-rate de categorías = flexibilidad **medida de verdad**.
- **Dificultad adaptativa:** número de categorías objetivo y prohibición de repetición según histórico.
- **Métricas:** flexibilidad (principal), switch-rate, coste de switch (latencia), originalidad intra-personal.
- **Feedback:** "tocaste 4/7 categorías; te faltó {industria, arte}" — accionable, no solo una etiqueta.
- **Variantes:** modo "cadena prohibida" (no repetir categoría), modo "categoría forzada".
- **Cómo testearlo:** tests deterministas del clasificador keyword→categoría; simulación que verifique que un agente que se queda en una categoría puntúa bajo en flexibilidad y uno que rota puntúa alto (hoy ambos puntúan igual — esa es la prueba de que el rediseño arregla el bug de constructo).
- **Por qué es superior:** convierte un ejercicio que **no mide su propio constructo** en uno que sí, sin IA, reusando persistencia existente.

### Puntuación 0-10 (los 15 ejes)
1) Potencial real de mejora cognitiva: **2** | 2) Transferencia a la vida real: **2** | 3) Claridad del mecanismo: **2** (consigna clara pero medición totalmente desalineada) | 4) Calidad de la progresión: **1** | 5) Calidad de la medición: **1** (no mide flexibilidad en absoluto) | 6) Personalización: **1** | 7) Resistencia al efecto práctica: **2** | 8) Valor metacognitivo: **2** | 9) Valor emocional: **3** | 10) Riesgo de fatiga/frustración (10=sin riesgo): **8** | 11) Riesgo de pseudoprogreso (10=sin riesgo): **1** (el peor: vende flexibilidad, entrena fluidez) | 12) Adherencia: **3** | 13) Escalabilidad: **2** | 14) Robustez científica: **1** | 15) Prioridad de mejora: **10**
**Nota global del ejercicio: 2.2/10**
**Decisión final:** Rediseñar completamente (sin clasificación de categorías es un duplicado deshonesto de EX01). Alternativa táctica: **Fusionar con EX01** bajo un único motor de generación divergente con métricas reales (fluidez normalizada + flexibilidad + originalidad), y diferenciar EX01/EX02 solo por la *restricción activa* (suprimir-categoría-dominante vs. rotar-categorías).

---

## Veredicto del módulo "Creatividad / Inteligencia (pensamiento divergente)"

### Capacidades cubiertas / huecos / ausentes
- **Cubierto (parcial y mal medido):** generación divergente por **fluidez bruta**. Es lo único que el sistema realmente cuantifica (`CreativityRubricScorer.kt:25-26`).
- **Hueco grave:** **inhibición de la respuesta prepotente** (EX01 lo declara, no lo mide).
- **Ausente por completo:**
  - **Flexibilidad cognitiva** — EX02 la promete; el código no tiene noción de categoría. **El ejercicio miente sobre su constructo.**
  - **Originalidad** — ningún proxy (ni rareza, ni frecuencia, ni distancia semántica). Es el corazón de la creatividad divergente y no existe.
  - **Elaboración real** — lo que hay (`palabras/idea`) es longitud de texto, no profundidad conceptual.
  - **Convergencia / insight** (RAT, problemas de uso remoto), **analogía/abstracción** (constructos centrales de "inteligencia") — el módulo se llama "Creatividad / **Inteligencia**" pero no contiene NADA de razonamiento fluido, abstracción, ni resolución de problemas. El segundo nombre es marketing sin respaldo.

### Ejercicios redundantes / a fusionar
- **EX01 y EX02 son el mismo ejercicio** a nivel de medición: comparten `CreativityRubricScorer.score()` (`CreativitySessionCoordinator.kt:78`) y solo difieren en el banco de prompts y en el texto de la consigna. **Fusionar** en un único motor divergente y diferenciar por *restricción activa medida*, no por enunciado.

### Riesgos sistémicos del módulo
1. **Medición común degenerada** — un solo scorer de 2 señales (fluidez no-normalizada + verbosidad) sirve a ambos ejercicios y a ningún constructo declarado correctamente. El sistema **no puede distinguir** un usuario creativo de uno verboso/repetitivo. **[Riesgo científico]**
2. **Banco de contenido finito y diminuto** — 8 + 6 = 14 prompts totales (`CreativityPromptCatalog.kt`), seleccionados por `Random(seed)` con seed = tiempo de inicio (`CreativityExerciseEngine.kt:18`). Memorización y repetición inevitables en días.
3. **Cero adaptación / cero longitudinal** — `historyForUser` existe (`CreativitySessionStore.kt:21`) pero **ningún consumidor lo lee**. No hay baseline, ni tendencia, ni dificultad adaptativa, ni "progreso contra tu propia línea base" (que es la *promesa de marca* en `CLAUDE.md`). El comentario "Difficulty bounds are per-exercise domain data" (`CreativityExerciseCatalog.kt:10`) es **falso**: no hay bounds, solo dos constantes fijas. **[Mejora crítica]**
4. **Feedback estructuralmente positivo** — las 3 etiquetas (`CreativityPracticeLabel.kt`) nunca comunican estancamiento o regresión; combinado con métricas inflables, produce **pseudoprogreso garantizado**.
5. **Presentación rota** — los `Double` se pintan crudos (`CreativityRootScreen.kt:211-212`: `"$fluency"`, `"$elaboration"`), exponiendo cosas como `3.6666666666666665`. Defecto de UI que además delata la pobreza de la métrica.
6. **Dedup frágil** — `distinct()` sobre strings solo `trim`-eados (sin lowercase ni colapso de espacios): la fluidez se infla con variantes triviales. **[Mejora crítica]**
7. *(No es riesgo, es acierto:)* el blindaje anti-IA/red/telemetría (`CreativityRuntimePolicy.kt`, `CreativityEvidenceGate.kt`, `CreativityAiLayers.kt`) está **bien hecho**: fail-closed, gates que exigen evidencia ejecutada, stubs `Blocked*` que no reenvían input. La seguridad/privacidad del módulo es sólida; el problema es 100% **cognitivo-científico**, no de plataforma.

### Las 3 cosas que más elevarían el poder cognitivo de este módulo
1. **Introducir originalidad y flexibilidad medidas localmente, sin IA** — construir (a) un **banco de frecuencia intra-personal** desde `historyForUser` (ya persistido, hoy muerto) para puntuar rareza, y (b) una **taxonomía local objeto→categorías** + clasificador por keywords para medir cobertura y switch-rate. Esto convierte dos contadores de palabras en dos ejercicios que **miden su constructo declarado**. Es el cambio de mayor impacto y desbloquea la promesa de marca ("progreso contra tu propia línea base").
2. **Alinear medición con consigna y normalizar la fluidez** — hacer que EX01 *mida* la supresión de lo prepotente (penalizar/no-puntuar la categoría dominante) y que EX02 *mida* rotación; y normalizar el dedup (lowercase + colapso de espacios + stemming básico) para matar el inflado. Sin esto, cualquier métrica nueva hereda la base podrida.
3. **Adaptación y honestidad longitudinal** — leer el histórico para fijar objetivos en la zona óptima (70-80% de logro), añadir una etiqueta de **estancamiento/regresión** real, y reemplazar prompts repetidos cuando el banco se agota (o expandir el banco con generación combinatoria local: objeto × restricción). Y renombrar/recortar el "Inteligencia" del título, o añadir de verdad un eje de razonamiento/convergencia (p. ej. usos remotos tipo RAT) — hoy "Inteligencia" no tiene ningún sustrato en el código.

**Nota global del módulo: 2.5/10** — Plomería de plataforma y seguridad excelentes; sustancia cognitiva casi nula. Tal como está, es un **demostrador offline honesto en lo legal/privacidad pero deshonesto en lo cognitivo**: EX02 vende un constructo (flexibilidad) que el código no mide, y todo el módulo carece de originalidad, adaptación y línea base, que son precisamente lo que distingue entrenamiento de entretenimiento.

---
