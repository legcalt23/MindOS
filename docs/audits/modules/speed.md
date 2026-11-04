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

