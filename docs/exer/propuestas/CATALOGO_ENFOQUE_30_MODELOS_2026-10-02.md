# Molvex · Enfoque
## 30 modelos de ejercicio con filtro de calidad

**Versión:** 1.0 · **Fecha:** 2026-10-02  
**Estado del documento:** propuesta conceptual completa, **DESIGN_ONLY / CONDITIONAL**  
**Plataformas de destino:** iOS y Android · **Uso previsto:** práctica cognitiva, no diagnóstico

> Este dossier define treinta modelos de práctica con reglas reproducibles y límites explícitos. Superar el filtro de diseño significa que la mecánica es suficientemente clara para evaluarla; no demuestra que el ejercicio ya esté implementado, que entrene una capacidad general ni que produzca transferencia. Los treinta modelos no son treinta factores independientes ni treinta ejercicios listos para publicar.

## Brief y criterio rector

El objetivo es ampliar el bloque Enfoque con prácticas para operaciones observables de atención: sostener una meta, detectar señales, seleccionar información pertinente, orientar o reorientar el foco, resolver competencia, inhibir una respuesta y recuperar una tarea tras una interrupción. El público inicial se trata como una población adulta heterogénea, con principiantes incluidos. La personalización individual queda subordinada a señales estables y a la robustez poblacional.

«Entrenar al máximo la capacidad cerebral» se traduce aquí en maximizar oportunidades de práctica válida, comprensible y progresiva sobre operaciones concretas, y diseñar pruebas independientes para saber qué se aprende. No se usa como promesa de aumento de inteligencia general, mejora garantizada, diagnóstico o cambio cerebral.

### Directriz maestra de selección

> Diseña una tarea móvil breve cuya acción mental objetivo pueda nombrarse en una sola frase y observarse ensayo a ensayo. Antes de admitirla, define la regla, los estímulos, la respuesta correcta, cada tipo de error, un ejemplo completo, una progresión que cambie un solo eje, los atajos que permitirían acertar sin usar la operación objetivo, los confundidores, la métrica de tarea, la adaptación, una alternativa accesible y una prueba que podría refutar la hipótesis. Compara el modelo con los ejercicios de Enfoque existentes y con las propuestas V5; una nueva apariencia no es una mecánica nueva. Conserva la precisión y el tiempo como señales separadas; no inventes umbrales, score oficial, equivalencia de plataforma ni claims de transferencia. Si una dependencia no está comprobada, etiqueta el diseño como condicionado y especifica el gate pendiente. Descarta cualquier ficha que no pueda explicar su acierto, error, falsador o límite de uso.

## Por qué este catálogo tiene seis zonas

La atención no es una operación única. La taxonomía de Molvex conserva Enfoque como bloque visible y usa estas zonas solo para organizar las propuestas; no crea submotores ni cambia el canon. Las familias se solapan por diseño. Un modelo puede entrenar una operación primaria y contener demanda secundaria de memoria, percepción, control motor o cambio de regla.

~~~mermaid
flowchart TB
  F["ENFOQUE · atención orientada a metas"]
  F --> A["01–05 · Sostener y vigilar"]
  F --> B["06–13 · Seleccionar y filtrar"]
  F --> C["14–18 · Orientar y reorientar"]
  F --> D["19–23 · Inhibir y resolver competencia"]
  F --> E["24–27 · Cambiar y recuperar la meta"]
  F --> G["28–30 · Seguir o repartir recursos"]
  A --> M01["M01 · Centinela"]
  A --> M02["M02 · Presencia o ausencia"]
  A --> M03["M03 · Deriva de patrón"]
  A --> M04["M04 · Compás imprevisible"]
  A --> M05["M05 · Ráfagas neutras"]
  B --> M06["M06 · Punto único"]
  B --> M07["M07 · Constelación"]
  B --> M08["M08 · Límite de canal"]
  B --> M09["M09 · Filtro vectorial"]
  B --> M10["M10 · Doble mapa"]
  B --> M11["M11 · Filtro cruzado"]
  B --> M12["M12 · Eco ignorado"]
  B --> M13["M13 · Marca de nuevos"]
  C --> M14["M14 · Captura periférica"]
  C --> M15["M15 · Foco por indicación"]
  C --> M16["M16 · Vuelta a zona"]
  C --> M17["M17 · Alerta temporal"]
  C --> M18["M18 · Parpadeo selectivo"]
  D --> M19["M19 · Mantén la línea"]
  D --> M20["M20 · Contramanda"]
  D --> M21["M21 · Excepción visible"]
  D --> M22["M22 · Respuesta espejo"]
  D --> M23["M23 · Guardián de meta"]
  E --> M24["M24 · Cambio con tarjeta"]
  E --> M25["M25 · Cambio predecible"]
  E --> M26["M26 · Cambio reactivo"]
  E --> M27["M27 · Retoma el flujo"]
  G --> M28["M28 · Enjambre de objetivos"]
  G --> M29["M29 · Dos corrientes"]
  G --> M30["M30 · Prioridad móvil"]
~~~

| Zona | Modelos | Pregunta de diseño | Demanda secundaria que se vigila |
|---|---:|---|---|
| Sostener y vigilar | 01–05 | ¿Se mantiene una regla ante eventos escasos, variables o distractores? | Criterio de respuesta, expectativa temporal, sensibilidad perceptiva |
| Seleccionar y filtrar | 06–13 | ¿Se prioriza el rasgo, lugar o canal objetivo frente a competidores? | Agudeza visual, lectura, memoria de distractores, mapeo respuesta |
| Orientar y reorientar | 14–18 | ¿Se dirige el foco por una señal voluntaria o involuntaria y se corrige cuando falla? | Comprensión de la señal, latencia física de pantalla, memoria de secuencia |
| Inhibir y resolver competencia | 19–23 | ¿Se retiene, cancela o corrige una respuesta dominante sin perder la meta? | Velocidad motora, aprendizaje del mapeo, memoria de regla |
| Cambiar y recuperar la meta | 24–27 | ¿Se cambia de regla con señal clara y se vuelve a la tarea tras una pausa? | Lectura de claves, coste de cambio, carga incidental |
| Seguir o repartir recursos | 28–30 | ¿Se conserva el objetivo al seguir varios objetos o atender dos flujos? | Visión periférica, memoria de trabajo, priorización estratégica |

La cobertura no equivale a independencia psicométrica. En particular, 01/02/03/04 comparten vigilancia; 06/07 comparten búsqueda visual; 14/15/16 comparten orientación; 19/20 comparten control de respuesta; 24/25/26 comparten cambio de tarea. Son modelos de práctica distintos cuando cambia la operación ejercitada o la estrategia requerida; no deben contarse como familias de evaluación independientes sin evidencia.

## Gate de entrada y trazabilidad

Una ficha entra en la selección conceptual solo si cumple todos estos puntos:

1. Un constructo primario delimitado y ninguna atribución automática a otros bloques.
2. Procedimiento que otra persona puede representar sin inventar reglas.
3. Aciertos, errores, omisiones, anticipaciones y respuestas inválidas diferenciados.
4. Un eje de dificultad principal; los demás factores se fijan o se registran.
5. Una estrategia de éxito barato analizada y una defensa proporcionada.
6. Métricas que identifican denominador, condición y trade-offs.
7. Ayuda, descanso y cambio de condición sin castigar al usuario ni contaminar una sonda.
8. Ruta táctil comprensible en ambos sistemas, operación offline y sin IA generativa.
9. Barreras perceptivas, motoras, lingüísticas y de audio declaradas, con alternativa o condición de uso.
10. Falsador, estado científico y uso permitido explícitos.

**Resultado del filtro:** 30/30 pasan este umbral de especificación conceptual; ninguno pasa por ello un gate de implementación, equivalencia iOS/Android, medición oficial o eficacia. Las fichas utilizan hipótesis de mecanismo y transferencia. No se les asigna una puntuación de calidad ficticia.

## Contrato común para los treinta

### Bucle de sesión

~~~mermaid
flowchart LR
  P["Preparar"] --> T["Tutorial reabrible"]
  T --> C{"Comprensión demostrada"}
  C -->|No| T
  C -->|Sí| B["Bloque de práctica"]
  B --> F["Feedback de tarea"]
  F --> R{"¿Continuar?"}
  R -->|Pausa o descanso| B
  R -->|Siguiente bloque| B
  R -->|Cerrar| X["Resumen y condición de la sesión"]
~~~

- **Estados:** preparación, demostración, práctica guiada, comprobación conductual de comprensión, práctica, feedback, pausa/recuperación y cierre. Salir, recibir una llamada o volver del background conserva una terminación explícita; no reconstruye ensayos ambiguos.
- **Regla de ajuste:** al inicio se elige una forma de entrada fácil de comprender. Solo al cierre del bloque, con muestra útil todavía por determinar, se mueve un paso de un único eje gobernante. Señal técnica inválida, incomprensión, interrupción o datos insuficientes retienen la interpretación; no fabrican un nivel inferior del usuario. Los umbrales y número de ensayos son parámetros de piloto, no constantes aprobadas.
- **Descanso y coaching:** si se repiten omisiones o la persona indica cansancio, ofrecer pausa, finalización o un bloque más simple y explicar qué condición cambia. Una sesión aislada no etiqueta fatiga ni déficit. El coach puede recomendar una acción breve; no recalcula el score ni hace diagnósticos.
- **Duración:** las franjas por ficha son propuestas para probar usabilidad, no dosis óptimas. Los planes de producto de 2/5/10/20/30 minutos no obligan a comprimir un mecanismo: 2 minutos pueden servir para tutorial o familiarización, pero no se llama práctica de vigilancia sostenida si no hay continuidad suficiente. Las franjas largas se dividen en bloques cuando el constructo lo permita.

### Scoring y adaptación

La salida de cada bloque es un **vector de señales de tarea**, no un «porcentaje de enfoque»:

- Precisión y denominador, aciertos, rechazos correctos, omisiones, falsas alarmas, errores de selección, errores por cambio y anticipaciones.
- Tiempo de respuesta solo cuando la tarea lo requiere, junto con su condición de captura y calidad temporal. En el estado observado del proyecto no se asume comparabilidad de timing entre dispositivos. RT no concede score oficial en este dossier.
- Consistencia temporal, variación por subbloque, rendimiento por condición y mejora contra formas previamente no expuestas solo si el diseño de evaluación lo permite.
- Sin promedio opaco de rapidez × precisión. Sin convertir una métrica de práctica o una dificultad elegida en una capacidad global.

**Adaptación común:** primero verificar comprensión y exposición; después ajustar el eje nombrado en la ficha. Si el usuario acierta con estrategia de respuesta fija, no subir dificultad suponiendo aprendizaje. Mantener series de entrenamiento y probes separados. Los modelos con temporización sensible, audio o competencia entre tareas permanecen en entrenamiento hasta superar sus gates específicos.

### Contrato móvil, accesibilidad y datos

- **Dominio compartido propuesto:** generador determinista con versión/semilla, reglas, validador de ítems, clasificador de respuesta y política adaptativa pura en KMP. Compose presenta estímulos y captura entradas; cada adaptación se decide fuera de la capa visual. No se afirma que estos contratos estén implementados.
- **Superficie común:** orientación vertical como default, estímulos escalables, objetivo visual central y áreas táctiles amplias (mínimo de diseño 48 dp donde corresponda), contraste y formas además de color, sin destellos, vibración ni animaciones decorativas. No hacer la pantalla pequeña o el target minúsculo como forma de dificultad.
- **iOS/Android:** misma regla, contenido versionado, transiciones y taxonomía de error. Medir por separado diferencias de render, safe areas, eventos táctiles, interrupciones y versiones del sistema. La paridad no se presume desde common code.
- **Acceso:** sin permisos sensibles. Audio y movimiento son opcionales; sus variantes no se mezclan con la forma visual en un perfil o comparación sin validación. Lector de pantalla, texto grande, daltonismo, reduced motion y control motor necesitan protocolo por tarea. Una adaptación que cambia la demanda queda identificada como variante y puede ser training-only.
- **Datos:** sesión, ID/version de modelo, forma/semilla, modalidad, eje y nivel, respuesta clasificada, tipo de error, condición y motivo de cierre, con retención mínima necesaria. Trials detallados, si son imprescindibles para depuración autorizada, se quedan locales y protegidos el tiempo mínimo; analítica remota propuesta solo agrega hechos allowlisted y consentimiento. No recopilar audio, vídeo, texto personal ni notificaciones reales.
- **API:** ninguna API de red necesaria para jugar, generar estímulos, puntuar o adaptarse. El catálogo puede operar offline. Una API futura para sincronizar resúmenes requiere decisión y contratos de privacidad separados.
- **Pruebas futuras:** generador reproducible por seed; ítem siempre válido; acertar/errar/omitir/anticipar; prevalencia que no permita responder siempre igual; ajuste de un solo eje; seed/formas no memorizables; cambio de dificultad sin salto de score; tutorial y accesibilidad; background/llamada/offline; equivalencia lógica por plataforma; instrumentación sin datos de contenido innecesarios. Este documento no ejecuta tests.

## Los 30 modelos

En todas las fichas, el resultado es **práctica de tarea**. Las referencias a E1–E13 y FOC2-E01–E18 indican proximidad con el catálogo o portfolio documentado; no afirman activación, aceptación ni implementación vigente. Cuando comparten paradigma, no son medidas independientes. Los parámetros de dificultad y duración son candidatos que requieren piloto.

### Zona 1 · Sostener y vigilar

#### M01 · Centinela de señal escasa
**Objetivo:** sostener una regla simple y detectar un evento infrecuente.  
**Mecánica:** flujo de formas neutras; tocar solo cuando una forma cumpla el rasgo objetivo. **Acierto:** toque al objetivo. **Error:** falsa alarma, omisión, toque anticipado o respuesta fuera de ventana, cada uno por separado.  
**Ejemplo sintético:** círculo · círculo · triángulo objetivo · círculo; la persona toca únicamente en el tercer evento. La tasa de objetivos queda fija dentro de la forma para que la dificultad no cambie el criterio de respuesta.  
**Progresión:** un eje, similitud perceptiva entre objetivo y señuelo; duración continua fija por bloque.  
**Señales y atajo:** aciertos, falsas alarmas, omisiones, lapsos por tramo; responder nunca no obtiene buen resultado porque se conserva el denominador de objetivos. Evasión adicional: tocar a ritmo; comprobar respuestas sin evento objetivo.  
**Adaptación y móvil:** subir similitud en un bloque posterior; si aumentan errores, conservar o reducir ese eje tras revisar comprensión. Duración candidata 5–8 min; no usar RT para comparar plataformas. Cercano a E1 Vigía y FOC2-E01; misma familia de vigilancia, no independiente.
**Falsador y límite:** si una forma con prevalencia balanceada permite igualar el resultado tocando a ritmo o sin discriminar ensayos objetivo, no se atribuye el rendimiento a vigilancia; se revisa o descarta esa forma.

#### M02 · Presencia o ausencia
**Objetivo:** sostener detección y expresar un criterio de respuesta sin depender de un único botón de objetivo.  
**Mecánica:** cada ensayo presenta una ventana con o sin la señal; elegir uno de dos botones persistentes: «Está» o «No está». **Error:** elección incorrecta, omisión, anticipación.  
**Ejemplo sintético:** una parrilla breve contiene la señal entre distractores; la respuesta se toma después de la parrilla para evitar tocar por reflejo al ver algo llamativo.  
**Progresión:** similitud objetivo/señuelo; la proporción de ensayos con señal permanece constante en la forma.  
**Señales y atajo:** aciertos, omisiones y falsas alarmas forman el vector principal; sensibilidad y criterio son análisis candidatos, no score aprobado. Botón siempre «está» queda expuesto por ensayos sin señal.  
**Adaptación y móvil:** ajustar similitud entre bloques, mostrar contraste de aciertos/errores sin etiquetar impulsividad. Duración candidata 4–6 min. Relación: FOC2-E05; variante de detección, no nueva familia independiente.
**Falsador y límite:** si la aparente mejora es solo un cambio de criterio «está/no está» sin mejorar discriminación bajo formas equilibradas, no se afirma mejor detección.

#### M03 · Deriva de patrón
**Objetivo:** mantener una referencia visual explícita y notar una desviación gradual.  
**Mecánica:** un patrón de barras permanece como referencia visible; la secuencia cambia un atributo en pequeños pasos y la persona toca cuando cruza una regla enseñada. **Error:** marcar antes/después del cruce, no detectar o responder sin cruce.  
**Ejemplo sintético:** la separación entre dos barras cambia gradualmente; el tutorial muestra qué cuenta como «cruzó el límite». No se pide recordar una cifra escondida.  
**Progresión:** magnitud del cambio por paso; tasa de eventos y contraste se fijan.  
**Señales y atajo:** detección por condición, ubicación del punto de respuesta y falsas alarmas. Si puede resolverse contando pasos o memorizando una cadencia, cambiar formas y secuencias; si domina agudeza visual, retirar la inferencia sobre Enfoque.  
**Adaptación y móvil:** disminuir magnitud de cambio solo si el tamaño sigue siendo accesible. Duración candidata 4–6 min. Hipótesis: selección sostenida ante cambio, no vigilancia general. Nueva mecánica; memoria visual secundaria declarada.
**Falsador y límite:** si contar pasos o memorizar la cadencia permite responder sin inspeccionar el rasgo que deriva, la forma no prueba detección sostenida y se descarta.

#### M04 · Compás imprevisible
**Objetivo:** sostener el criterio de detección cuando los intervalos no permiten anticipar el siguiente evento.  
**Mecánica:** serie visual con intervalos irregulares; responder al objetivo, no al ritmo. **Error:** anticipación sin objetivo, falsa alarma, omisión o acierto.  
**Ejemplo sintético:** evento · pausa larga · evento · pausa breve · objetivo; el instante del objetivo se sortea por forma determinista y no se anuncia con cuenta atrás.  
**Progresión:** incertidumbre del intervalo; frecuencia del objetivo fija, saliencia fija.  
**Señales y atajo:** detección y anticipaciones por ventanas temporales. Una ráfaga rítmica de toques sin objetivo se marca como conducta no interpretable para esa sesión; no como fraude.  
**Adaptación y móvil:** el eje solo cambia entre bloques. El tiempo táctil se registra como señal local; no se usa para comparar iOS/Android hasta validar onset y entrada física. Duración candidata 5–7 min. Relación con E1/V5 E14; misma familia atencional.
**Falsador y límite:** si una cadencia de toques o los intervalos predicen los objetivos, la secuencia deja de ser imprevisible y no sustenta la hipótesis de vigilancia.

#### M05 · Vigilancia con ráfagas neutras
**Objetivo:** mantener la tarea principal durante breves interferencias no emocionales.  
**Mecánica:** detectar la misma señal del bloque base; en algunos segmentos aparecen patrones geométricos periféricos irrelevantes. No se simulan notificaciones ni contenido personal. **Error:** los errores de la tarea primaria siguen clasificados igual; el distractor no requiere respuesta.  
**Ejemplo sintético:** tras un tramo limpio aparece una banda de puntos en el borde; la regla objetivo no cambia.  
**Progresión:** saliencia del distractor, con distribución y tasa congeladas durante una forma.  
**Señales y atajo:** comparar bloques limpios/con distractor manteniendo dificultad y prevalencia iguales. La mejora solo en el bloque limpio no se presenta como resistencia a distractores.  
**Adaptación y móvil:** empezar sin distractor, introducirlo tras comprobar la regla y ofrecer salida sin penalización. Duración candidata 5–8 min; movimiento reducido o pantalla pequeña pueden requerir training-only. Cercano a los conceptos de interferencia del portfolio, no equivalente a E11 digital.
**Falsador y límite:** si el contraste, tamaño o saliencia no están igualados entre condiciones, cualquier coste puede ser perceptivo; no se interpreta como interferencia atencional.

### Zona 2 · Seleccionar y filtrar

#### M06 · Punto único
**Objetivo:** seleccionar un rasgo objetivo que destaca por una sola dimensión.  
**Mecánica:** campo de formas iguales salvo una que cambia orientación; tocar su ubicación. **Error:** tocar señuelo, omitir, tocar una coordenada vacía.  
**Ejemplo sintético:** localizar el único símbolo inclinado entre símbolos verticales. El tutorial enseña el rasgo; la ubicación varía.  
**Progresión:** densidad de elementos, manteniendo tamaño, contraste y disposición táctil legible.  
**Señales y atajo:** acierto por conjunto de elementos, omisión y selección errónea. Si el objetivo siempre ocupa una posición, invalidar la forma; no inferir atención de velocidad de dedo.  
**Adaptación y móvil:** aumentar densidad, nunca reducir target táctil bajo el mínimo de diseño. Duración candidata 3–5 min. Relación con E7 Rastreo; es búsqueda de rasgo, no evidencia independiente de búsqueda de conjunción.
**Falsador y límite:** si una ubicación fija o un objetivo perceptivamente más saliente explica las elecciones, la forma no evalúa selección por rasgo y debe regenerarse.

#### M07 · Constelación de conjunción
**Objetivo:** filtrar distractores que comparten solo parte de la plantilla objetivo.  
**Mecánica:** encontrar una combinación visible de dos rasgos, como forma y orientación, entre distractores que comparten uno u otro. Tocar el elemento que reúne ambos.  
**Ejemplo sintético:** la tarjeta objetivo es un rombo rayado; hay rombos lisos y círculos rayados, pero solo una tarjeta contiene ambos rasgos.  
**Progresión:** similitud de distractores; tamaño del campo se conserva dentro del bloque.  
**Señales y atajo:** aciertos, omisiones, señuelo elegido y errores por posición. Evitar que color, longitud del texto o ubicación revelen la solución.  
**Adaptación y móvil:** complejizar un rasgo cada vez; texto alternativo etiqueta forma y orientación. Duración candidata 3–5 min. Relación directa con FOC2-E04; misma familia de búsqueda si cambia solo la skin.
**Falsador y límite:** si cualquiera de los dos rasgos por separado basta para hallar siempre el objetivo, o la posición lo revela, no se exige conjunción y la forma se descarta.

#### M08 · Límite de canal
**Objetivo:** mantener una ventana espacial relevante e ignorar eventos fuera de ella.  
**Mecánica:** se muestra una zona amplia y fija; responder a objetivos dentro de ella y retenerse ante señuelos equivalentes fuera. **Acierto/error:** hit interior, falsa alarma exterior, omisión interior.  
**Ejemplo sintético:** objetivo aparece entre elementos dentro de un marco circular; un señuelo del mismo tipo aparece fuera.  
**Progresión:** cercanía de distractores al límite; la zona y el tamaño de estímulo permanecen fijos.  
**Señales y atajo:** tasas interiores/exteriores y omisiones. Si los elementos externos son siempre más tenues, la persona puede ignorar brillo en vez de filtrar espacio; los estímulos se emparejan.  
**Adaptación y móvil:** subir cercanía antes de introducir más objetos. Zona táctil y visual no se estrecha con dificultad. Duración candidata 3–5 min. Relación con FOC2-E06; no crea un constructo nuevo por cambiar la forma del marco.
**Falsador y límite:** si brillo, tamaño o visibilidad diferencian los eventos fuera del límite, el desempeño no permite aislar selección espacial.

#### M09 · Filtro vectorial
**Objetivo:** priorizar el objetivo central ante distractores laterales congruentes o incompatibles.  
**Mecánica:** una flecha central indica izquierda/derecha; flancos apuntan en igual u opuesta dirección. Elegir la dirección central.  
**Ejemplo sintético:** izquierda · izquierda · derecha · izquierda · izquierda; respuesta correcta: derecha.  
**Progresión:** compatibilidad de flancos y distancia respecto al objetivo, alteradas en pasos separados.  
**Señales y atajo:** precisión y tiempo por condición congruente/incongruente, sin restarlos en un score compuesto. La estrategia de responder siempre al flanco se detecta en ensayos incompatibles.  
**Adaptación y móvil:** respuesta con dos botones grandes, target central claramente distinguible de flancos. Duración candidata 3–5 min. E2 Filtro y FOC2-E07 son familia cercana; no contar skins como modelos adicionales.
**Falsador y límite:** si respuestas y errores no dependen de la compatibilidad del flanco tras controlar la regla y el mapeo, no se afirma interferencia de distractores.

#### M10 · Doble mapa
**Objetivo:** responder al rasgo relevante aunque la ubicación espacial sugiera otra respuesta.  
**Mecánica:** la dirección de una flecha determina el botón; su posición izquierda/derecha es irrelevante y varía de forma balanceada.  
**Ejemplo sintético:** flecha «izquierda» dibujada en el lado derecho; se selecciona izquierda.  
**Progresión:** proporción y separación de ensayos compatibles/incompatibles; no cambiar también la legibilidad del estímulo.  
**Señales y atajo:** precisión y latencia por compatibilidad, etiquetadas por dispositivo. Si el código de respuesta se memoriza por ubicación fija, se invalida la forma.  
**Adaptación y móvil:** feedback nombra «seguiste la dirección» o «seguiste la posición», no una etiqueta personal. Duración candidata 3–5 min. Cercano a Simon/FOC2-E08; solapa control ejecutivo y no se considera independiente del conflicto espacial.
**Falsador y límite:** si la localización predice sistemáticamente el botón correcto o el mapeo no se contrabalancea, no puede atribuirse la respuesta al rasgo direccional.

#### M11 · Filtro cruzado
**Objetivo:** seleccionar una señal visual relevante cuando aparece un distractor auditivo irrelevante.  
**Mecánica:** responder a un símbolo visual objetivo mientras suenan tonos neutros sin significado; el audio nunca contiene la respuesta correcta.  
**Ejemplo sintético:** elegir si aparece una figura abierta; ignorar un tono breve. **Error:** falsa alarma, omisión, identificación visual incorrecta.  
**Progresión:** interferencia auditiva perceptible, no volumen incómodo.  
**Señales y atajo:** comparar condiciones visuales equivalentes con/sin audio; audición, auriculares, latency y entorno son confundidores.  
**Adaptación y móvil:** audio opt-in y opción visual equivalente; no se mezclan los resultados. Sin permiso de micrófono, sin grabación. Si el audio no es accesible o el onset no es fiable, versión visual training-only. Duración candidata 3–5 min. Hipótesis intermodal, no efecto demostrado.
**Falsador y límite:** si el tono no se percibe de forma consistente o las condiciones difieren en temporización o dispositivo, el coste no identifica filtrado intermodal y la modalidad queda fuera de inferencia.

#### M12 · Eco ignorado
**Objetivo:** seleccionar un objetivo cuando un distractor irrelevante del ensayo anterior reaparece como objetivo.  
**Mecánica:** elegir la forma objetivo entre dos opciones; en condición de repetición, una opción fue el distractor ignorado inmediatamente antes.  
**Ejemplo sintético:** en el ensayo anterior se ignoró el triángulo; ahora el triángulo es la respuesta correcta.  
**Progresión:** similitud objetivo/distractor; el intervalo y la secuencia se mantienen fijos.  
**Señales y atajo:** aciertos y errores por repetición; no interpretar el coste como inhibición pura porque memoria episódica y familiaridad explican el patrón.  
**Adaptación y móvil:** feedback sobre la regla actual, nunca «recuerda lo que ignoraste». Botones de opción simétricos y mapeos contrabalanceados. Duración candidata 4–6 min. Nueva familia secuencial; permanece training-only si el componente de memoria domina.
**Falsador y límite:** si el efecto se explica por familiaridad o repetición perceptiva sin contraste con un distractor visto pero no ignorado, no se atribuye a filtrado del distractor previo.

#### M13 · Marca de nuevos
**Objetivo:** mantener un conjunto irrelevante fuera del foco mientras se seleccionan elementos que llegan después.  
**Mecánica:** primero aparecen distractores que pasan a tono tenue; luego entran elementos nuevos. Buscar un objetivo solo entre los nuevos.  
**Ejemplo sintético:** tres tarjetas existentes quedan atenuadas; al añadirse dos, localizar si una nueva contiene el símbolo objetivo.  
**Progresión:** similitud de elementos nuevos con los marcados; duración de vista previa fija.  
**Señales y atajo:** selección correcta entre nuevos, falsas selecciones de elementos previos, omisión. Si la memoria visual de las posiciones basta, variar posiciones con dificultad constante.  
**Adaptación y móvil:** permitir zoom y contraste; reduced motion sustituye el desplazamiento por aparición estática etiquetada. Duración candidata 3–5 min. Memoria visual secundaria declarada; no comparar directamente con búsqueda simple.
**Falsador y límite:** si basta recordar posiciones de la vista previa para acertar, sin seleccionar los elementos recién llegados, no se demuestra exclusión del conjunto previo.

### Zona 3 · Orientar y reorientar

#### M14 · Captura periférica
**Objetivo:** observar si una señal saliente y no informativa desplaza la selección del objetivo.  
**Mecánica:** un marcador periférico breve puede aparecer antes de un objetivo; la señal no predice dónde aparecerá. Elegir la ubicación objetivo.  
**Ejemplo sintético:** brillo neutro en el margen derecho, objetivo aleatorio en izquierda o derecha; la instrucción es seguir el símbolo central, no el brillo.  
**Progresión:** separación temporal entre señal y objetivo, como parámetro candidato; saliencia fija.  
**Señales y atajo:** exactitud por condición y tiempo solo descriptivo; en móvil la latencia física puede variar. Responder siempre al marcador fracasa con ensayos no coincidentes.  
**Adaptación y móvil:** sin flash ni parpadeo; usar un borde estático que aparece y se retira. Duración candidata 3–5 min. Hipótesis de captura exógena; no inferir rasgo estable de personalidad atencional.
**Falsador y límite:** si la señal periférica predice el lado del objetivo, no es informativa-neutral; si no resulta perceptible, tampoco prueba captura. La forma exige ambos controles.

#### M15 · Foco por indicación
**Objetivo:** orientar voluntariamente la atención con una clave central antes de buscar un objetivo periférico.  
**Mecánica:** una flecha central indica el lado más probable; la persona atiende a ese lado y elige el target si aparece. Incluir ensayos neutros para medir la ayuda de la clave.  
**Ejemplo sintético:** flecha central hacia arriba significa revisar el sector superior; las posiciones posibles se equilibran entre formas.  
**Progresión:** fiabilidad informativa de la clave, cambiada solo entre formas de entrenamiento y etiquetada.  
**Señales y atajo:** exactitud por clave válida/neutra/incorrecta; no ocultar instrucciones ni penalizar comprender el idioma.  
**Adaptación y móvil:** clave con flecha y texto opcional, no solo color. Duración candidata 3–5 min. Cercano a FOC2-E10; si coincide el generador, tratarlo como una misma familia.
**Falsador y límite:** si la clave válida no mejora la selección frente a una clave neutra con estímulos emparejados, no se respalda la hipótesis de orientación voluntaria.

#### M16 · Vuelta a zona
**Objetivo:** reorientarse después de que una clave espacial conduzca a una ubicación incorrecta.  
**Mecánica:** una clave predice un lado en la mayoría de ensayos; en ensayos balanceados señala el lado opuesto. Buscar y contestar sobre el target real, no el lado sugerido.  
**Ejemplo sintético:** flecha apunta a izquierda; aparece un objetivo válido solo a derecha; responder a su atributo, no tocar la flecha.  
**Progresión:** incertidumbre de la señal o similitud de targets, una a la vez; conservar visible la regla.  
**Señales y atajo:** respuestas correctas en ensayos inválidos y omisiones; errores de lectura de clave van aparte. No usa solo tiempo de reacción para afirmar flexibilidad.  
**Adaptación y móvil:** si fallan las reglas, volver a tutorial. Duración candidata 3–5 min. Comparte familia con M14/M15; la hipótesis específica es reorientación, no otra familia independiente.
**Falsador y límite:** si los ensayos con clave inválida no separan errores de lectura de clave de la respuesta al objetivo real, no se puede sostener que la tarea ejercite reorientación.

#### M17 · Alerta temporal
**Objetivo:** usar una pista de tiempo para preparar la selección sin responder a una cuenta atrás motora.  
**Mecánica:** una clave indica si el objetivo suele aparecer pronto o más tarde; responder al objetivo cuando aparece, y no durante la espera.  
**Ejemplo sintético:** un icono de reloj vacío predice ventana temprana; un icono lleno predice ventana tardía. La posición del objetivo no cambia.  
**Progresión:** confiabilidad de la clave temporal; no variar a la vez rapidez del estímulo.  
**Señales y atajo:** detecciones/omisiones por clave; RT es exploratorio hasta validar presentación física y entrada.  
**Adaptación y móvil:** guía estática sin animación, ventanas suficientemente largas para accesibilidad. Duración candidata 3–5 min. Si el onset no puede medirse, conserva entrenamiento por exactitud y no ofrece comparación temporal oficial.
**Falsador y límite:** si la clave no predice realmente la ventana, o el onset del estímulo no está verificado, no se infiere preparación temporal ni se interpreta RT.

#### M18 · Parpadeo selectivo
**Objetivo:** seleccionar dos objetivos relevantes presentados en una secuencia visual rápida pese a distractores intermedios.  
**Mecánica:** al terminar la secuencia, seleccionar cuáles dos objetivos aparecieron y en qué orden; nunca exige tocar cada frame.  
**Ejemplo sintético:** una letra objetivo, varias letras neutrales y una segunda letra objetivo; la separación entre ambas varía.  
**Progresión:** número de elementos entre objetivos; velocidad y contraste se fijan tras calibrar comprensión.  
**Señales y atajo:** detección del primer y segundo objetivo, orden y falsas identificaciones, estratificados por separación. Leer una pista de posición no debe revelar respuestas.  
**Adaptación y móvil:** no usar parpadeo luminoso; desplazamiento opcional reducido y presentación con contraste estable. Duración candidata 3–5 min. Relación con RSVP/E10 Flujo, pero doble objetivo y separación temporal cambian la demanda. Training-only hasta validar refresh y legibilidad.
**Falsador y límite:** si la detección del segundo objetivo cambia por tasa de refresco, legibilidad o exposición desigual entre plataformas, la tarea queda training-only y no compara rendimiento temporal.

### Zona 4 · Inhibir y resolver competencia

#### M19 · Mantén la línea
**Objetivo:** responder a estímulos objetivo frecuentes y retener la respuesta ante un estímulo de excepción.  
**Mecánica:** tocar ante las formas «sí» y no tocar ante la forma «no». **Error:** comisión ante no-go, omisión ante go, anticipación.  
**Ejemplo sintético:** pulsar ante todos los círculos; detenerse solo ante el círculo con una marca diagonal.  
**Progresión:** similitud perceptiva entre go/no-go; prevalencia y mapeo se fijan por forma.  
**Señales y atajo:** comisiones, omisiones y post-error conductual descriptivo. No premiar «no tocar nunca» ni reducir el denominador go.  
**Adaptación y móvil:** forma y marca, nunca solo color; pausa disponible. Duración candidata 3–5 min. Cercano a E3 Gatillo/FOC2-E15; no es medición oficial de control inhibitorio.
**Falsador y límite:** si la proporción de ensayos permite rendir bien sin responder nunca, el bloque no contiene un trade-off interpretable y se invalida.

#### M20 · Contramanda
**Objetivo:** practicar cancelación de una respuesta ya preparada cuando aparece una señal de stop.  
**Mecánica:** responder a cada go; si una señal claramente distinta aparece después, intentar cancelar la respuesta.  
**Ejemplo sintético:** flecha izquierda invita a elegir izquierda; una señal de stop sustituye el fondo antes de la respuesta; el ensayo termina con cancelación o respuesta registrada.  
**Progresión:** demora de la señal stop, candidata a calibración; los escalones se congelan durante sondas.  
**Señales y atajo:** aciertos go, respuestas no canceladas y omisiones; no estimar SSRT ni comparar el timing entre dispositivos desde este diseño.  
**Adaptación y móvil:** bloques breves, respuesta motora grande; si el jugador no ve la señal, revisar visibilidad, no bajar capacidad. Duración candidata 4–6 min. Cercano a E4 Freno/FOC2-E16. **Training-only / temporalidad pendiente**; no se admite para perfil oficial.
**Falsador y límite:** sin presentación y captura temporal verificadas, o si el usuario no procesa la señal stop, no hay evidencia interpretable de cancelación; no calcular SSRT.

#### M21 · Excepción visible
**Objetivo:** abandonar una respuesta dominante cuando una clave explícita cambia la regla del ensayo.  
**Mecánica:** regla base visible, por ejemplo «símbolo A = botón izquierdo»; una tarjeta de excepción visible dice cuándo usar el botón contrario.  
**Ejemplo sintético:** A normalmente va a izquierda; si aparece la etiqueta «invertir», A va a derecha.  
**Progresión:** similitud de la clave de excepción con claves neutrales; la frecuencia de excepciones no se mueve en un bloque.  
**Señales y atajo:** precisión por regla base/excepción, omisiones y errores de lectura separados. Una excepción rara que se adivina por posición invalida la forma.  
**Adaptación y móvil:** la tarjeta persiste en pantalla hasta que la regla se entienda; ayudas se registran. Duración candidata 3–5 min. Cercano a FOC2-E12; memoria de regla es demanda secundaria explícita.
**Falsador y límite:** si posición o frecuencia permiten anticipar la excepción sin leer la clave, la forma no exige abandonar la respuesta dominante y debe regenerarse.

#### M22 · Respuesta espejo
**Objetivo:** frenar la respuesta espacial directa y elegir deliberadamente el lado opuesto al target.  
**Mecánica:** detectar un símbolo a izquierda/derecha y responder con el botón opuesto. El mapeo se demuestra en tutorial y se mantiene visible.  
**Ejemplo sintético:** target aparece a la derecha; toque correcto a izquierda.  
**Progresión:** similitud del target entre posiciones; no aumentar velocidad junto con el cambio de mapeo.  
**Señales y atajo:** errores de respuesta congruente/incongruente, acierto y latencia descriptiva. Un mapeo fijo puede aprenderse por hábito; formas reservadas cambian localización sin cambiar regla.  
**Adaptación y móvil:** botones separados y grandes; el diseño no mide movimientos oculares. Duración candidata 3–5 min. Cercano a conflicto espacial; no se denomina antisacada ni se atribuye una medida ocular.
**Falsador y límite:** si un sesgo lateral, la ubicación del botón o un mapeo no invertido explica los aciertos, la tarea no prueba respuesta espacial opuesta.

#### M23 · Guardián de meta
**Objetivo:** mantener la regla principal activa cuando un rasgo irrelevante ofrece una respuesta competidora.  
**Mecánica:** la persona responde a una dimensión indicada de forma persistente; otro rasgo del mismo estímulo coincide a veces con una regla alternativa ilustrada en el tutorial.  
**Ejemplo sintético:** clasificar por orientación aunque el color sugiera la otra opción.  
**Progresión:** compatibilidad del rasgo irrelevante; la regla objetivo nunca queda oculta ni cambia sin señal.  
**Señales y atajo:** precisión por compatibilidad, errores de regla y omisiones separados. Si leer una etiqueta adicional domina cada ensayo, simplificar la instrucción; si memoria de trabajo domina, reclasificar el ejercicio.  
**Adaptación y móvil:** indicador visual persistente de la regla; sin presión temporal. Duración candidata 3–5 min. Cercano a Stroop/no-lexical y a FOC2-E11; cuenta como el mismo vecindario de interferencia.
**Falsador y límite:** si el rasgo irrelevante no cambia errores por condición, o la regla objetivo no se comprende sin ayuda, no se afirma resolución de competencia.

### Zona 5 · Cambiar y recuperar la meta

#### M24 · Cambio con tarjeta
**Objetivo:** cambiar de operación atendida cuando una clave indica qué dimensión juzgar.  
**Mecánica:** clasificar por forma o por relleno según una tarjeta central que precede a cada estímulo. Ambas reglas permanecen visibles.  
**Ejemplo sintético:** tarjeta «forma» + triángulo rayado: elegir triángulo; tarjeta «relleno» + círculo liso: elegir liso.  
**Progresión:** similitud de clave o complejidad de regla, una por vez; las secuencias de repetición/cambio se balancean.  
**Señales y atajo:** aciertos, omisiones, error de dimensión, condición switch/repeat y latencia descriptiva. Una clave difícil de leer no cuenta como dificultad ejecutiva.  
**Adaptación y móvil:** texto + símbolo redundantes, botones grandes. Duración candidata 3–5 min. Similar a E6 Cambio; switch cost requiere validación antes de score longitudinal.
**Falsador y límite:** si la diferencia cambio/repetición desaparece al igualar legibilidad, balance y exposición de claves, no se atribuye a alternancia de regla.

#### M25 · Cambio predecible
**Objetivo:** preparar un cambio de regla cuando la secuencia alterna de manera regular.  
**Mecánica:** dos reglas simples alternan con un patrón anunciado; responder a la tarjeta actual, no adivinar por el número de ensayo.  
**Ejemplo sintético:** forma, color, forma, color; un icono visible confirma la regla del ensayo.  
**Progresión:** proporción de repeticiones dentro del patrón; no ocultar el próximo patrón ni penalizar el uso correcto de anticipación como estrategia.  
**Señales y atajo:** aciertos de cambio/repetición. Si puede responderse por posición en secuencia sin leer la señal, variar forma y comprobar transferencia de regla.  
**Adaptación y móvil:** cadencia no acelerada y opción de repasar reglas. Duración candidata 3–5 min. Es una práctica de preparación de cambio, no una segunda prueba independiente de M24.
**Falsador y límite:** si la posición serial permite responder sin atender a la clave, el patrón anunciado invalida la interpretación de preparación de cambio.

#### M26 · Cambio reactivo con señal
**Objetivo:** abandonar una regla previa ante una clave nueva, clara e impredecible.  
**Mecánica:** la regla puede cambiar en cualquier ensayo; una clave visible antes del target especifica inequívocamente la regla vigente.  
**Ejemplo sintético:** «forma» se reemplaza por «orientación»; el usuario lee la señal y responde al target.  
**Progresión:** solapamiento visual de las claves, con legibilidad intacta; frecuencia de cambio fija.  
**Señales y atajo:** errores tras cambio y tras repetición; no atribuirlos a flexibilidad si el cue no se percibe. La señal explícita evita confundir sorpresa con ambigüedad.  
**Adaptación y móvil:** reducir interferencia si falla la comprensión, repetir tutorial sin penalización. Duración candidata 3–5 min. Relación con M24/E6; familia de cambio, no nuevo factor cognitivo.
**Falsador y límite:** si la clave no se percibe o los cambios son predecibles por la secuencia, no se concluye que la persona reoriente la regla de forma reactiva.

#### M27 · Retoma el flujo
**Objetivo:** reanudar una regla activa después de una pausa breve y neutral.  
**Mecánica:** durante una secuencia, la sesión presenta una pausa controlada con pantalla neutra; al volver se repite la instrucción vigente y se retoma la tarea.  
**Ejemplo sintético:** buscar círculos abiertos; pausa; aparece la misma regla y vuelve una secuencia nueva, no el ensayo perdido.  
**Progresión:** punto de interrupción en el bloque; duración de pausa no es un reto ni se usa para clasificar al usuario.  
**Señales y atajo:** comparar errores iniciales tras pausa con bloques sin pausa, preservando contenido. Un reanudar más lento por relectura de regla no equivale a lapsus.  
**Adaptación y móvil:** usar pausa/resume real de iOS/Android para la interrupción de sistema y una pausa simulada solo como condición separada. Duración candidata 4–6 min. V5 E17 es antecedente; compatibilidad del ciclo de vida queda por verificar.
**Falsador y límite:** si el efecto se explica por releer la regla, perder estado o por una interrupción técnica no separada, la medida no identifica recuperación atencional tras pausa.

### Zona 6 · Seguir o repartir recursos

#### M28 · Enjambre de objetivos
**Objetivo:** mantener seguimiento de varios objetos señalados mientras comparten espacio con distractores idénticos.  
**Mecánica:** marcar visualmente un conjunto, seguir su movimiento suave y al detenerse indicar cuáles conservan la marca objetivo.  
**Ejemplo sintético:** tres círculos reciben un borde temporal; todos se mueven y pierden el borde; al final se pregunta por uno.  
**Progresión:** número de objetos objetivo; velocidad y densidad permanecen fijas.  
**Señales y atajo:** identificación por objeto y errores de confusión; si la velocidad de animación o pantalla pequeña domina, degradar a forma estática training-only.  
**Adaptación y móvil:** movimiento reducido ofrece otra forma etiquetada, no asumida equivalente; alternativa accesible de posiciones estáticas. Duración candidata 3–5 min. Cercano a E9 Enjambre; memoria espacial y procesamiento visual son secundarios.
**Falsador y límite:** si se sigue cada objeto solo por su ubicación inicial, o la velocidad/refresco domina la respuesta, no se demuestra seguimiento múltiple; la variante estática no se presume equivalente.

#### M29 · Dos corrientes
**Objetivo:** practicar la selección de dos flujos concurrentes sin presentar «multitarea» como capacidad general.  
**Mecánica:** una corriente visual primaria exige clasificar símbolos; una segunda corriente visual sencilla exige detectar un objetivo ocasional. Hay bloques de cada tarea sola y de ambas juntas.  
**Ejemplo sintético:** clasificar triángulos centrales y, al mismo tiempo, tocar cuando un borde periférico se cierra.  
**Progresión:** tasa de eventos de la secundaria, después de medir cada tarea sola; mantener una corriente constante por bloque.  
**Señales y atajo:** cada tarea tiene aciertos, omisiones y errores propios; coste dual es análisis exploratorio, con priorización y abandono secundaria visibles.  
**Adaptación y móvil:** zonas separadas pero táctiles amplias; no animar texto secundario. Duración candidata 4–6 min. E8 Dúo es antecedente; resultado no se titula «capacidad de multitarea».
**Falsador y límite:** si el resultado depende de abandonar siempre la corriente secundaria o no se compara con cada tarea en solitario, solo se informa el trade-off observado.

#### M30 · Prioridad móvil
**Objetivo:** redistribuir el foco cuando una clave cambia qué flujo tiene prioridad.  
**Mecánica:** dos corrientes visibles se mantienen; una clave cambia la prioridad y se responde primero al flujo indicado mientras se conserva la regla del otro.  
**Ejemplo sintético:** la tarjeta «prioridad arriba» pide responder a la forma superior; una señal posterior cambia a «prioridad abajo».  
**Progresión:** proximidad temporal entre cambio de prioridad y estímulo; las reglas permanecen constantes.  
**Señales y atajo:** precisión de tarea prioritaria y secundaria por separado, errores justo después del cambio y omisiones. Sacrificar siempre una corriente no cuenta como éxito.  
**Adaptación y móvil:** ofrecer bloques monoflujo de práctica guiada; el reporte muestra trade-off, no capacidad. Duración candidata 4–6 min. Hipótesis de asignación atencional; debe demostrar fiabilidad y utilidad independiente antes de cualquier perfil.
**Falsador y límite:** si una prioridad fija funciona igual aunque cambie la clave, no se evidencia redistribución; no asignar perfil sin fiabilidad y utilidad independientes.

## Lectura visual del portfolio y no duplicación

| Relación documental | Modelos | Tratamiento de catálogo |
|---|---|---|
| Familia presente en catálogo E1–E13 | M01, M06–M10, M18–M20, M24, M26, M28–M29 | Propuesta de refinamiento, sustitución o entrenamiento análogo. No duplicar automáticamente el generador ni contar como nuevos factores. |
| Cercanos al portfolio V5 | M01–M02, M04, M06–M10, M15, M19–M21, M23, M27 | El portfolio es propuesta de diseño; su fila no equivale a adopción. Releer la autoridad del motor antes de decidir compatibilidad. |
| Mecanismo secuencial/temporal o de modalidad adicional | M03, M05, M11–M13, M14, M16–M17, M22, M25, M30 | Candidatos con confundidores declarados. La modalidad o la secuencia solo justifican otra familia si cambian la operación, no por cambiar estímulo. |
| Atraviesan memoria, percepción o velocidad | M03, M11–M13, M17–M18, M22, M27–M30 | Mantener un único objetivo primario; degradar a training-only o rediseñar si la demanda secundaria explica la mayor parte del resultado. |

El número treinta responde al encargo y ofrece espacio de selección, no un mandato de construir todos. Para un primer lote futuro, elegir un conjunto complementario pequeño después de reconciliar el portfolio y el motor: uno de vigilancia, uno de búsqueda, uno de interferencia, uno de orientación, uno de control inhibitorio y uno de cambio. No escoger por preferencia visual ni anunciarlo como plan aprobado.

## Métricas, feedback y evaluación independiente

Para cada modelo la especificación de implementación deberá enumerar, sin completar todavía fórmulas de perfil:

| Campo de contrato | Ejemplos admisibles | Límite de interpretación |
|---|---|---|
| Respuesta por ensayo | HIT, MISS, FALSE_ALARM, CORRECT_REJECT, WRONG_SELECTION, WRONG_RULE, OMISSION, ANTICIPATION | No combinar clases que tengan significado distinto |
| Contexto de ensayo | id de forma, condición, versión, eje, valor de parámetro, orden, modalidad, calidad temporal | El ID no da por sí solo comparabilidad |
| Resumen de bloque | denominadores, precisión por condición, tipo de error, patrón temporal, exposición, interrupción | Performance de tarea, no estimación general de atención |
| Tiempo | eventos que lo delimitan, reloj, calidad y dispositivo, solo cuando el diseño lo requiere | Excluir de inferencia si el tiempo no es autoritativo |
| Feedback | una pauta ligada al error observable y una siguiente acción | No mostrar rasgos, diagnóstico, ranking o cambio de capacidad |
| Evaluación | familia/formas no entrenadas, exposición bloqueada, outcome independiente | La práctica repetida no demuestra transferencia ni causalidad |

**Claims permitidos en el producto de entrenamiento:** «En este bloque detectaste X de Y señales», «aumentaron las omisiones en los últimos tramos», «en esta condición los distractores coincidían más con el objetivo». **Claims no permitidos por este dossier:** «tu inteligencia/enfoque subió», «tu cerebro rinde X% más», diagnóstico, protección frente a enfermedad o mejora garantizada en estudio/trabajo.

Un resultado mejor en la tarea entrenada sirve para describir esa tarea. Una forma no vista puede examinar generalización dentro de la familia; una transferencia cercana o funcional exige tareas y estudios independientes, comparadores creíbles, incertidumbre y seguimiento. Ningún dato de práctica incluido aquí concede medición oficial, crecimiento fiable o ranking.

## Contrato de implementación futura

Esto define necesidades, no APIs ya existentes:

- **Identidad del modelo:** ID estable propuesto, versión, zona, operación primaria, secundarias, familia vecina, estatus y claim permitido.
- **Definición del ejercicio:** reglas, generador/banco, semilla, restricciones y validación, dificultad con un eje rector, clasificación de respuesta y explicaciones del resultado.
- **Sesión y persistencia:** finalidad TRAINING_ONLY, modalidad/variante, condición de accesibilidad, versión de forma, estado de entrada/salida, resumen local minimizado y razón de interrupción.
- **UI Compose común:** instrucción, escena de ensayo, botones/zonas, ayuda, progreso, pausa, feedback y resumen; lectores de pantalla y reduced motion probados por modelo.
- **API/servicios:** no requerido para la experiencia primaria. Sin IA generativa en generación o scoring. Sin depender de Firebase, servidor, micrófono, cámara o notificación real.
- **Analítica propuesta:** inicio/cierre/pausa, versión, condición agregada, banda de exposición, motivo técnico y adaptación elegida. Respuestas y secuencias cognitivas detalladas no salen del dispositivo por defecto.
- **Pruebas de lógica:** golden de una forma fija, propiedad de validez, balance de condiciones, ausencia de atajo de respuesta constante, métricas correctas en denominador/extremos, adaptación monoeje, seed reservado, recuperación idempotente.
- **Pruebas móviles:** Android e iOS por separado, tamaño de pantalla, safe area, 60 Hz y tasas mayores si se soportan, input táctil, audio opcional, reduced motion, lector, background/llamada, process death, offline y sesiones parciales. En Windows no se declara compilación iOS.
- **Puerta para implementar:** decisión de producto pendiente donde corresponda; compatibilidad en working tree verificada; dossier de cada candidato completo; review independiente de ciencia/psicometría cuando exista; sin P0/P1 para el uso declarado.
- **Puerta para declarar entrenamiento listo:** código, generador, UI, offline, recuperación, accesibilidad, seguridad de datos, tests, paridad y revisión de contenido.
- **Puerta para medir:** forms independientes, exposición, comparabilidad, precisión del estimador y error adecuados al uso, validación empírica y decisión autorizada. Entrenamiento puede ser útil antes de ese punto, pero queda sin score de perfil.

## Plan de validación por etapas

| Etapa | Pregunta | Prueba propuesta | Qué no concluye |
|---|---|---|---|
| Análisis de diseño | ¿La regla exige la operación declarada? | Revisión manual de estados, ejemplos correctos/incorrectos y estrategias de respuesta fija | Aprendizaje humano |
| Lógica sintética | ¿Los ítems, clasificadores y adaptaciones obedecen al contrato? | Seeds adversarias, todos los errores, prevalencias extremas, omisión, cambio de eje y reinicio | Validez cognitiva |
| Verificación móvil | ¿La misma regla y condición llega a pantalla y se captura en ambos OS? | Matriz de dispositivos, timing si aplica, accesibilidad, background, llamadas y offline | Igualdad de latencia o eficacia |
| Comprensión | ¿Principiantes entienden y realizan la operación prevista? | Sesión de usabilidad separada, observación del proceso, carga y errores de comprensión | Eficacia causal |
| Calibración | ¿La métrica es estable y sensible al cambio para el uso elegido? | Formas, exposición, test-retest, error, techo/suelo y diferencias relevantes por dispositivo | Transferencia |
| Eficacia y transferencia | ¿La práctica causa una mejora distinta de la familiaridad? | Estudio preregistrado con control activo, tarea no entrenada, outcome funcional definido y seguimiento | Beneficio universal |

No se da por válido un tamaño muestral o umbral con una cifra de catálogo. Cada estudio necesita estimando, efecto mínimo relevante, varianza, pérdidas y análisis definidos antes de observar resultados. La sesión de usabilidad y el análisis con usuarios requieren autorización y protocolo propios.

## Revisión de calidad y descarte

**Pasadas realizadas para este dossier:** requisitos y canon; comparación de familias/demandas; revisión crítica de atajos, confusores y claims. La última es una pasada secuencial propia, no revisión independiente. No hubo agentes revisores ni participante humano en esta ejecución.

**Ideas descartadas como modelos autónomos:** versiones que solo cambian skin/color; juego de notificaciones reales o contenido social; dificultad basada en tocar targets pequeños o acelerar sin control temporal; doble tarea etiquetada como multitarea general; modalidad obligatoria de audio/movimiento; punto global de «enfoque»; adaptación que convierte un mal bloque en supuesto déficit; leaderboard, vida, recompensa aleatoria o penalización por descansar. Algunas pueden ser detalles de interfaz o condiciones de comparación, no nuevas familias.

**Hallazgo de alcance:** la revisión total del motor reporta readiness 2/10 para expansión de ejercicios y propone resolver precondiciones de constructo, telemetría, acceso y medición. La petición actual autoriza este trabajo conceptual, pero no resuelve esas dependencias ni decisiones CEO. La auditoría declara código intacto en el snapshot bdc252f; el checkout actual tiene un árbol de trabajo muy modificado, por lo que no se extrapola compatibilidad completa desde aquella auditoría. En particular, iOS, el timing físico y el acceso real de entrenamiento no se declaran comprobados por este documento.

## Fuentes y límites de aplicabilidad

1. **Petersen, S. E., & Posner, M. I. (2012).** *The Attention System of the Human Brain: 20 Years After*. Annual Review of Neuroscience, 35, 73–89. [DOI](https://doi.org/10.1146/annurev-neuro-062111-150525). Marco de operaciones y redes de atención; no valida este catálogo ni sus efectos.
2. **Langner, R., & Eickhoff, S. B. (2013).** *Sustaining attention to simple tasks: A meta-analytic review of the neural mechanisms of vigilant attention*. Psychological Bulletin, 139(4), 870–900. [DOI](https://doi.org/10.1037/a0030694) · [Texto en PubMed Central](https://pmc.ncbi.nlm.nih.gov/articles/PMC3627747/). Revisión/meta-análisis de mecanismos de vigilancia; no establece una dosis de Molvex.
3. **Simons, D. J., et al. (2016).** *Do “Brain-Training” Programs Work?* Psychological Science in the Public Interest, 17(3), 103–186. [DOI](https://doi.org/10.1177/1529100616661983) · [PubMed](https://pubmed.ncbi.nlm.nih.gov/27697851/). La revisión distingue mejoría en tareas entrenadas de transferencia cercana y lejana; evalúa los estudios citados por programas comerciales, no cada intervención posible.
4. **Jahn, N., et al. (2023).** *Neural correlates of the attention training technique as used in metacognitive therapy: A randomized sham-controlled fMRI study in healthy volunteers*. Frontiers in Psychology. [Artículo](https://doi.org/10.3389/fpsyg.2023.1084022). En esa muestra y protocolo se informó un resultado de tiempo en disengagement atencional, sin mejora relativa en varias otras medidas. Es evidencia de un protocolo concreto, no de estos treinta modelos.
5. **Savulich, G., et al. (2019).** *Improvements in Attention Following Cognitive Training With the Novel “Decoder” Game on an iPad*. Frontiers in Behavioral Neuroscience. [Artículo](https://doi.org/10.3389/fnbeh.2019.00002). Estudio de una intervención específica de atención sostenida visual en iPad; no permite transferir eficacia a Molvex.

**Base documental del proyecto:** [Estándar de calidad de ejercicios](../../context_pack/04_EXERCISE_QUALITY_STANDARD.md), [dossier EXER](../plantillas/02_DOSSIER_EJERCICIO.md), [constructos y cobertura](../sistema/06_CONSTRUCTOS_Y_COBERTURA.md), [diseño de práctica](../sistema/07_DISENO_DEL_ENTRENAMIENTO.md), [medición y transferencia](../sistema/09_MEDICION_Y_TRANSFERENCIA.md), [motor actual y vNext](../sistema/10_MOTOR_ACTUAL_Y_VNEXT.md), [accesibilidad móvil](../sistema/11_MOVIL_ACCESIBILIDAD_DATOS.md), [validación empírica](../sistema/13_VALIDACION_EMPIRICA.md), [índice de autoridad Focus V5](../../engines/focus_v5/CURRENT_AUTHORITY_INDEX.md), [catálogo de 18 modelos V5](../../engines/focus_v5/EXERCISE_PORTFOLIO_V5.md) y [auditoría total de Enfoque](../../audits/AUDITORIA_TOTAL_ENFOQUE_MOLVEX_2026-09-14.md).

## Disposición de esta entrega

- **document_completion:** COMPLETE para 30 modelos conceptuales y sus contratos comunes.
- **design_disposition:** CONDITIONAL; el motor, la taxonomía operativa, los gates de acceso y las decisiones CEO siguen requiriendo reconciliación antes de adoptar o implementar.
- **readiness:** DESIGN_ONLY. El siguiente paso sería revisión humana del catálogo contra la autoridad Focus V5 y, tras resolver bloqueadores aplicables, seleccionar un primer lote pequeño para dossiers de implementación.
- **Evidence status:** hipótesis de diseño apoyadas en paradigmas relacionados; **eficacia de Molvex, equivalencia móvil, entrenamiento listo, medición oficial y transferencia: NOT ESTABLISHED**.
- **Cambios de producto:** ninguno. No se modifica motor, scoring, flag, datos, API, app ni despliegue.

