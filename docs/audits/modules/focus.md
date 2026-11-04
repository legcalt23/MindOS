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

