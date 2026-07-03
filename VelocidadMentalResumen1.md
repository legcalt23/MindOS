# MindOS · Módulo "Velocidad Mental"
### Diseño del sistema de entrenamiento cognitivo (no de software)

> Documento de diseño de entrenamiento. No contiene código, arquitectura KMP ni UI salvo lo imprescindible para describir el entrenamiento. Está escrito para ser convertible directamente en lógica de producto e implementable por un agente de código.

---

## 1. Resumen ejecutivo del sistema "Velocidad Mental"

**Qué lo hace único.** El sistema no trata la velocidad como un único tiempo de reacción (TR), sino como un **vector descomponible**. Cada respuesta se separa en tres componentes inspirados en los modelos de acumulación de evidencia (drift-diffusion / LBA):

- **Tiempo no-decisional (Ter):** codificación perceptiva + ejecución motora. *No es velocidad mental* y debe controlarse, no premiarse.
- **Tasa de evidencia (deriva, v):** velocidad real de procesamiento/decisión. Es el corazón del módulo.
- **Umbral / cautela (a):** cuánta evidencia exige el usuario antes de responder. Bajarlo da TR menores **a costa de errores**: es la firma de la impulsividad, no de la mejora.

Sobre cada respuesta se modela además la **distribución completa de TR** (ex-Gaussiana: μ, σ, **τ** = cola lenta/lapsos) y la **variabilidad intraindividual** (IIV). Esto permite distinguir "rápido y estable" de "rápido a ratos con lapsos".

**Filosofía central: velocidad útil, no impulsividad.** La mejora real se define como responder **más rápido manteniendo o mejorando** precisión, estabilidad (menor IIV) y control inhibitorio. Velocidad que degrada precisión, dispara falsas alarmas o aumenta la variabilidad **no cuenta como progreso** y el sistema lo bloquea explícitamente.

**Cómo optimiza la frontera velocidad-precisión.** Para cada usuario y condición se estima su **función velocidad-precisión (SAT)** individual. La mejora real es un **desplazamiento de la frontera hacia arriba-izquierda** (más rápido *y* igual o más preciso), no un movimiento *a lo largo* de la frontera (canjear precisión por velocidad). El motor adaptativo solo aumenta presión temporal cuando hay margen demostrado para empujar la frontera, no para empujar al usuario a errar.

---

## 2. Modelo cognitivo base

### 2.1 Subcapacidades y su correspondencia con el modelo de decisión

| Subcapacidad | Qué es | Componente del modelo | Por qué importa |
|---|---|---|---|
| **Velocidad perceptiva** | Detectar/discriminar/identificar un estímulo | Calidad inicial de evidencia + parte de Ter (codificación) | Limita el "techo" de todo lo demás: si la entrada es lenta, la decisión también |
| **Velocidad de decisión** | Acumular evidencia y elegir | Tasa de deriva (v) | Núcleo de la "velocidad mental"; lo más transferible |
| **Velocidad motora** | Ejecutar la respuesta | Parte de Ter | **No es cognición**; se mide para descontarla, nunca para premiarla |
| **Velocidad ejecutiva** | Cambiar de regla, inhibir, actualizar | Coste de cambio, SSRT, tasa de actualización | Determina la rapidez bajo control, no solo en piloto automático |
| **Velocidad de actualización** | Sustituir información en memoria operativa | Tasa de refresco de WM | Decisión rápida con información cambiante |
| **Velocidad bajo interferencia** | Mantener ritmo con conflicto/distractores | Caída de v y subida de Ter bajo conflicto | Las situaciones reales nunca son "limpias" |

### 2.2 Diferenciación operativa de los "tipos de velocidad"

- **Perceptiva vs. de decisión:** se separan variando la *dificultad de discriminación* (perceptiva) frente al *número de alternativas / ambigüedad de evidencia* (decisión) manteniendo lo demás constante.
- **De decisión vs. motora:** todo ejercicio incluye **bloques de TR simple** y mapeos motores variables para estimar y descontar el componente motor; las ganancias se reportan sobre el componente cognitivo, no sobre el TR bruto.
- **Ejecutiva vs. el resto:** se aísla con manipulaciones de *cambio de regla*, *go/no-go*, *stop-signal* y *congruencia*, midiendo el **coste** (diferencia condición difícil − fácil), no el TR absoluto.

### 2.3 Qué NO se debe considerar mejora real

1. Reducir TR **bajando el umbral** (a) a costa de precisión → es impulsividad.
2. Mejorar solo en **estímulos, posiciones o secuencias memorizadas** (efecto práctica específico).
3. Ganancia que **no aparece en tareas no entrenadas** ni en pruebas de transferencia cercana.
4. Reducir el **TR motor** sin cambiar el componente cognitivo (entrenar el dedo, no la mente).
5. Cambios **dentro del error de medida** (fiabilidad test-retest): ruido, no señal.
6. Subir velocidad media mientras crecen la **variabilidad (IIV)** o la **cola lenta (τ)**: rendimiento más frágil disfrazado de más rápido.

---

## 3. Arquitectura del entrenamiento (no del software)

El entrenamiento se organiza en **12 módulos cognitivos**. Para cada uno: objetivo, ejercicios ancla, estímulos, progresión, métricas, errores típicos y escalado élite.

### M1 · Velocidad perceptiva
- **Objetivo:** reducir el tiempo de codificación e identificación de estímulos.
- **Ejercicios:** Destello (detección), Gemelos (discriminación fina).
- **Estímulos:** formas, orientaciones, colores, contrastes; exposiciones con enmascaramiento.
- **Progresión:** menor duración de exposición → menor contraste/diferencia → enmascaramiento más agresivo.
- **Métricas:** umbral de tiempo de inspección, PC a exposición fija, IIV perceptiva.
- **Errores típicos:** confundir agudeza visual con velocidad; respuestas anticipadas.
- **Élite:** discriminación cerca del umbral psicofísico con enmascaramiento y bajo contraste.

### M2 · Velocidad de reacción (simple)
- **Objetivo:** mínima latencia ante señal clara, separando componente motor.
- **Ejercicios:** Destello (variante TR simple).
- **Estímulos:** señal única, ISI variable e impredecible.
- **Progresión:** ventanas de respuesta más estrechas; control de anticipaciones.
- **Métricas:** TR simple, anticipaciones (<150 ms), lapsos (cola lenta).
- **Errores típicos:** premiar el "machaque"; ritmos predecibles que invitan a anticipar.
- **Élite:** ISI muy variable + penalización fuerte de anticipación; foco en bajar la cola lenta, no la media.

### M3 · Velocidad de elección
- **Objetivo:** elegir entre alternativas según regla con baja latencia.
- **Ejercicios:** Multivía.
- **Estímulos:** 2→4→6→8 alternativas con mapeo estímulo-respuesta.
- **Progresión:** más alternativas → mapeos arbitrarios → mapeos que rotan.
- **Métricas:** TR de elección, pendiente de Hick (TR vs. log nº alternativas), PC.
- **Errores típicos:** memorización del mapeo fijo.
- **Élite:** mapeos rotatorios + presión temporal + alternativas perceptivamente cercanas.

### M4 · Velocidad de comparación
- **Objetivo:** comparar símbolos/números/formas con rapidez.
- **Ejercicios:** Cifrado (estilo símbolo-dígito), Gemelos.
- **Estímulos:** pares y claves símbolo↔número↔forma.
- **Progresión:** claves más largas → claves que cambian → comparación de múltiples atributos.
- **Métricas:** ítems correctos/min, IIV, coste de cambio de clave.
- **Errores típicos:** aprender la clave de memoria (deja de medir velocidad).
- **Élite:** clave nueva cada bloque + interferencia de claves previas.

### M5 · Velocidad de búsqueda visual
- **Objetivo:** localizar objetivos entre distractores de forma eficiente.
- **Ejercicios:** Rastreo.
- **Estímulos:** búsqueda por característica (eficiente) y por conjunción (serial).
- **Progresión:** mayor set size → conjunción → objetivos que comparten rasgos con distractores.
- **Métricas:** pendiente TR/ítem, PC, omisiones, falsas alarmas.
- **Errores típicos:** layouts fijos que se memorizan.
- **Élite:** set sizes altos, conjunciones múltiples, posiciones aleatorizadas, deadline.

### M6 · Velocidad de clasificación
- **Objetivo:** categorizar bajo presión temporal.
- **Ejercicios:** Clasificador.
- **Estímulos:** ítems con varias dimensiones (color/forma/tamaño/paridad).
- **Progresión:** reglas más abstractas → reglas combinadas → reglas que cambian.
- **Métricas:** TR de clasificación, errores por dimensión, coste de regla.
- **Errores típicos:** confundir clasificación con simple discriminación.
- **Élite:** clasificación multidimensional con regla cambiante e interferencia.

### M7 · Velocidad con inhibición
- **Objetivo:** responder rápido cuando toca y frenar cuando no.
- **Ejercicios:** Compuerta (go/no-go), Freno (stop-signal).
- **Estímulos:** alta proporción de "go" para construir prepotencia; "no-go"/"stop" intercalados.
- **Progresión:** mayor proporción go → señal de stop más tardía → SSD adaptativo.
- **Métricas:** FA en no-go, SSRT, TR de go, índice de impulsividad.
- **Errores típicos:** diseño donde la mejor estrategia es responder siempre.
- **Élite:** SSD adaptativo cercano al 50% de inhibición + presión temporal en los go.

### M8 · Velocidad con cambio de regla
- **Objetivo:** alternar criterios sin bloqueos ni perseveración.
- **Ejercicios:** Bisagra (task switching).
- **Estímulos:** mismo estímulo, dos reglas (p.ej. color vs. forma) con señal de tarea.
- **Progresión:** switch predecible → aleatorio → señal de tarea más corta.
- **Métricas:** coste de cambio (switch − repeat), errores perseverativos.
- **Errores típicos:** secuencias predecibles que eliminan el coste real.
- **Élite:** switching aleatorio + tercera regla + interferencia.

### M9 · Velocidad bajo interferencia
- **Objetivo:** mantener ritmo con conflicto/ruido (no como único pilar).
- **Ejercicios:** Conflicto (estilo Stroop/flanker).
- **Estímulos:** congruentes vs. incongruentes; flancos distractores.
- **Progresión:** mayor proporción incongruente → distractores más fuertes → deadline.
- **Métricas:** coste de interferencia (incong − cong) en TR y PC.
- **Errores típicos:** depender de un solo paradigma; saturar con conflicto.
- **Élite:** conflicto + cambio de regla simultáneos, manteniendo precisión alta.

### M10 · Velocidad sostenida
- **Objetivo:** mantener rendimiento rápido sin degradación a lo largo del bloque.
- **Ejercicios:** versiones largas de M2/M5 con baja densidad de objetivos.
- **Estímulos:** flujo prolongado, objetivos infrecuentes.
- **Progresión:** bloques más largos → menor frecuencia de objetivo.
- **Métricas:** pendiente de decremento de vigilancia, subida de τ y de lapsos en el tiempo.
- **Errores típicos:** confundir fatiga con falta de capacidad.
- **Élite:** bloques largos con objetivos raros y exigencia de precisión sostenida.

### M11 · Velocidad de recuperación post-error
- **Objetivo:** volver rápido a rendimiento estable tras equivocarse.
- **Ejercicios:** Reenganche (cualquier paradigma con análisis post-error).
- **Estímulos:** dificultad calibrada para generar errores controlados.
- **Progresión:** menor ventana de recuperación tolerada → mayor exigencia tras error.
- **Métricas:** post-error slowing (PES), precisión y IIV en los 2-3 ensayos post-error.
- **Errores típicos:** ignorar la dinámica post-error (es donde se cae el rendimiento real).
- **Élite:** errores forzados con exigencia de re-estabilización inmediata.

### M12 · Transferencia funcional
- **Objetivo:** tender puentes hacia tareas reales (no prometer transferencia lejana).
- **Ejercicios:** microprotocolos aplicados (ver §11).
- **Estímulos:** material semirrealista (texto, tableros, señales).
- **Progresión:** de estímulo abstracto → estímulo con estructura realista.
- **Métricas:** desempeño en pruebas de transferencia cercana y reportes funcionales.
- **Errores típicos:** asumir transferencia sin medirla.
- **Élite:** sondas de transferencia variadas y no entrenadas como criterio de progreso.

---

## 4. Catálogo de ejercicios

Estructura común por ejercicio: **nombre · capacidad · descripción · reglas · variables de dificultad · métricas · riesgo de efecto práctica · mitigación · principiante / avanzado / élite.**

> Mitigación transversal (aplica a todos): aleatorización de estímulos/posiciones/mapeos, generación procedimental de ítems, rotación de claves y reglas por bloque, sondas no entrenadas, y *gating* de presión temporal por precisión + impulsividad + estabilidad.

### E1 · Destello
- **Capacidad:** velocidad perceptiva / TR simple.
- **Descripción:** aparece una señal tras un ISI variable; responder al detectarla.
- **Reglas:** una sola respuesta; responder antes del límite; anticipaciones penalizan.
- **Dificultad:** duración de exposición, contraste, enmascaramiento, variabilidad del ISI, ventana de respuesta.
- **Métricas:** TR simple, anticipaciones, lapsos, IIV.
- **Riesgo práctica:** alto si el ritmo es predecible (se anticipa).
- **Mitigación:** ISI no predecible, ensayos "catch" sin señal, penalización de anticipación.
- **Principiante:** exposición larga, alto contraste, ventana amplia. **Avanzado:** exposición corta, contraste medio, ISI muy variable. **Élite:** umbral perceptivo con enmascaramiento, foco en reducir la cola lenta (τ) más que la media.

### E2 · Gemelos
- **Capacidad:** discriminación de estímulos similares.
- **Descripción:** dos estímulos casi idénticos; decidir "iguales / distintos" rápido.
- **Reglas:** respuesta binaria con deadline; precisión gateada.
- **Dificultad:** magnitud de la diferencia, número de dimensiones, exposición, distractores.
- **Métricas:** umbral de diferencia, TR, PC, coste por dimensión.
- **Riesgo práctica:** memorización de pares concretos.
- **Mitigación:** pares generados proceduralmente; nunca repetir el par exacto.
- **Principiante:** diferencias grandes en una dimensión. **Avanzado:** diferencias pequeñas, dos dimensiones. **Élite:** diferencia cercana al umbral + enmascaramiento + deadline.

### E3 · Cifrado
- **Capacidad:** velocidad de comparación (símbolo-número-forma).
- **Descripción:** clave que asocia símbolos↔dígitos↔formas; emparejar ítems contra la clave a máxima velocidad útil.
- **Reglas:** responder según clave visible; precisión gateada; sin penalizar consulta de clave.
- **Dificultad:** longitud de clave, arbitrariedad del mapeo, cambio de clave, ocultar clave parcialmente.
- **Métricas:** ítems correctos/min, IIV, coste de cambio de clave, dependencia de la clave (mide automatización).
- **Riesgo práctica:** **muy alto** (memorizar la clave deja de medir velocidad).
- **Mitigación:** clave nueva cada bloque; interferencia con claves previas; medir caída al rotar clave.
- **Principiante:** clave corta y estable. **Avanzado:** clave que cambia entre bloques. **Élite:** clave nueva por bloque + interferencia proactiva de la clave anterior.

### E4 · Rastreo
- **Capacidad:** velocidad de búsqueda visual.
- **Descripción:** localizar objetivo(s) entre distractores; indicar presencia/posición.
- **Reglas:** búsqueda por característica o conjunción; deadline; omisiones y FA penalizan distinto.
- **Dificultad:** set size, tipo de búsqueda (rasgo vs. conjunción), similitud objetivo-distractor, deadline.
- **Métricas:** pendiente TR/ítem, PC, omisiones, FA.
- **Riesgo práctica:** layouts memorizables.
- **Mitigación:** posiciones aleatorias, identidad de objetivo cambiante, sin repetir disposición.
- **Principiante:** búsqueda por rasgo, set pequeño. **Avanzado:** conjunción, set medio. **Élite:** conjunción múltiple, set grande, distractores muy similares, deadline estricto.

### E5 · Clasificador
- **Capacidad:** velocidad de clasificación por reglas.
- **Descripción:** asignar ítems a categorías según una regla activa.
- **Reglas:** una regla por mini-bloque; precisión gateada; respuesta con deadline.
- **Dificultad:** nº de categorías, abstracción de la regla, dimensiones relevantes, regla combinada.
- **Métricas:** TR de clasificación, errores por dimensión, coste de regla.
- **Riesgo práctica:** mapear ítem→tecla de memoria.
- **Mitigación:** ítems nuevos, reglas que rotan, dimensiones irrelevantes variables.
- **Principiante:** una dimensión, dos categorías. **Avanzado:** dos dimensiones, regla combinada. **Élite:** clasificación multidimensional con regla cambiante e interferencia.

### E6 · Multivía
- **Capacidad:** velocidad de elección (choice RT).
- **Descripción:** elegir entre N alternativas según mapeo estímulo-respuesta.
- **Reglas:** respuesta correcta única; deadline; precisión gateada.
- **Dificultad:** nº de alternativas, arbitrariedad del mapeo, rotación del mapeo, similitud perceptiva.
- **Métricas:** TR de elección, pendiente de Hick, PC, IIV.
- **Riesgo práctica:** mapeo fijo memorizado.
- **Mitigación:** mapeos rotatorios y arbitrarios entre bloques.
- **Principiante:** 2-3 alternativas, mapeo natural. **Avanzado:** 4-6, mapeo arbitrario. **Élite:** 6-8, mapeo rotatorio + presión temporal + alternativas cercanas.

### E7 · Compuerta
- **Capacidad:** velocidad con inhibición (go/no-go).
- **Descripción:** responder rápido a "go", inhibir en "no-go".
- **Reglas:** alta proporción de go (prepotencia); FA en no-go penaliza fuerte.
- **Dificultad:** proporción go:no-go, velocidad del flujo, similitud go/no-go.
- **Métricas:** FA en no-go, TR de go, omisiones, índice de impulsividad.
- **Riesgo práctica:** aprender la regla y relajar control.
- **Mitigación:** no-go perceptivamente cercano al go; proporción variable; deadline en go.
- **Principiante:** 70:30, go/no-go muy distintos. **Avanzado:** 80:20, similares. **Élite:** ≥85:15, alta velocidad, go/no-go muy próximos.

### E8 · Freno
- **Capacidad:** inhibición reactiva (stop-signal).
- **Descripción:** iniciar respuesta a "go"; ante señal de stop, cancelarla.
- **Reglas:** SSD (retardo de la señal de stop) **adaptativo** apuntando a ~50% de inhibición.
- **Dificultad:** SSD adaptativo, velocidad de go, frecuencia de stop.
- **Métricas:** **SSRT** (tiempo de inhibición), TR de go, tasa de inhibición.
- **Riesgo práctica:** ralentizar el go a propósito para "ganar tiempo" de frenar.
- **Mitigación:** detectar y penalizar el go-slowing estratégico; deadline en go independiente.
- **Principiante:** stop frecuente, SSD generoso. **Avanzado:** SSD adaptativo estándar. **Élite:** SSD ajustado al 50% real + presión en el go + estímulos próximos.

### E9 · Conflicto
- **Capacidad:** velocidad bajo interferencia (Stroop/flanker). *No es pilar único.*
- **Descripción:** responder a una dimensión ignorando otra incongruente.
- **Reglas:** atender dimensión relevante; congruentes e incongruentes mezclados.
- **Dificultad:** proporción incongruente, fuerza del distractor, deadline, congruencia de proporción variable.
- **Métricas:** coste de interferencia (TR y PC: incong − cong), IIV bajo conflicto.
- **Riesgo práctica:** estrategia de proporción (predecir incongruencia).
- **Mitigación:** proporción variable e impredecible; mezclar con cambio de regla.
- **Principiante:** baja proporción incongruente. **Avanzado:** ~50%, distractor fuerte. **Élite:** conflicto + cambio de regla simultáneos manteniendo PC alta.

### E10 · Bisagra
- **Capacidad:** velocidad de cambio de regla (task switching).
- **Descripción:** mismo estímulo, dos+ reglas; una señal indica cuál aplicar.
- **Reglas:** aplicar la regla señalada; switch aleatorio; errores perseverativos penalizan.
- **Dificultad:** frecuencia de switch, predecibilidad, intervalo señal-estímulo, nº de reglas.
- **Métricas:** coste de cambio (switch − repeat), errores perseverativos, IIV.
- **Riesgo práctica:** secuencias predecibles que eliminan el coste.
- **Mitigación:** switching aleatorio, intervalo corto, mezclar reglas nuevas.
- **Principiante:** switch predecible, dos reglas. **Avanzado:** aleatorio, intervalo corto. **Élite:** tres reglas, switching aleatorio + interferencia.

### E11 · Niebla
- **Capacidad:** decisión bajo información parcial (acumulación de evidencia).
- **Descripción:** la evidencia se revela gradualmente (ruido que se aclara); decidir lo antes posible con la precisión exigida.
- **Reglas:** se puede responder en cualquier momento; precisión gateada; responder demasiado pronto con poca evidencia penaliza.
- **Dificultad:** velocidad de revelado, nivel de ruido, ambigüedad de la evidencia, coste del error.
- **Métricas:** punto de decisión (cuánta evidencia esperó), TR, PC, proxy de deriva.
- **Riesgo práctica:** memorizar el desenlace de patrones repetidos.
- **Mitigación:** evidencia generada proceduralmente; nunca repetir la secuencia exacta.
- **Principiante:** revelado rápido, poco ruido. **Avanzado:** revelado lento, ruido medio. **Élite:** evidencia muy ambigua, coste de error alto, exige decisión temprana óptima (ni impulsiva ni tardía).

### E12 · Cascada Visual
- **Capacidad:** velocidad perceptiva en secuencia (estilo RSVP).
- **Descripción:** flujo rápido de estímulos; detectar/identificar objetivos dentro del flujo.
- **Reglas:** marcar objetivos; el flujo no se detiene; FA y omisiones penalizan.
- **Dificultad:** velocidad del flujo (SOA), densidad de objetivos, similitud objetivo-distractor, doble objetivo cercano (lag).
- **Métricas:** PC por posición, parpadeo atencional (caída en lag corto), omisiones, FA.
- **Riesgo práctica:** ritmos y secuencias memorizables.
- **Mitigación:** orden y contenido aleatorizados; posiciones de objetivo variables.
- **Principiante:** flujo lento, objetivos distintos. **Avanzado:** flujo rápido, objetivos similares. **Élite:** SOA mínimo, doble objetivo en lag corto, alta precisión.

### E13 · Eco
- **Capacidad:** velocidad perceptiva auditiva.
- **Descripción:** reconocer tonos/patrones/secuencias auditivas y responder rápido.
- **Reglas:** discriminar/clasificar señal auditiva; deadline; precisión gateada.
- **Dificultad:** diferencia de frecuencia/duración, ruido de fondo, longitud de secuencia, velocidad.
- **Métricas:** TR auditivo, umbral de discriminación, PC, IIV.
- **Riesgo práctica:** memorizar secuencias concretas.
- **Mitigación:** secuencias generadas proceduralmente; aleatorizar tonos.
- **Principiante:** tonos muy distintos, sin ruido. **Avanzado:** tonos próximos, ruido leve. **Élite:** discriminación cercana al umbral + ruido + secuencias largas.

### E14 · Órbita
- **Capacidad:** rastreo de objetivos múltiples simplificado (MOT).
- **Descripción:** varios objetos se marcan como objetivos, todos se mueven entre distractores idénticos; al detenerse, identificar los objetivos.
- **Reglas:** seguir mentalmente los objetivos; responder rápido al final; sin tocar durante el movimiento.
- **Dificultad:** nº de objetivos, nº de distractores, velocidad y aleatoriedad del movimiento, duración.
- **Métricas:** PC de identificación, TR final, capacidad de rastreo (máx. objetivos al criterio).
- **Riesgo práctica:** trayectorias predecibles.
- **Mitigación:** movimiento aleatorio, rebotes impredecibles, sin patrones repetidos.
- **Principiante:** 2 objetivos, lento. **Avanzado:** 3-4, velocidad media. **Élite:** 4-5 objetivos, alta velocidad, muchos distractores, larga duración.

### E15 · Dúo
- **Capacidad:** velocidad bajo doble tarea ligera.
- **Descripción:** una tarea primaria de velocidad + una secundaria intermitente y sencilla.
- **Reglas:** mantener la primaria; atender la secundaria cuando aparezca; medir coste de doble tarea.
- **Dificultad:** frecuencia de la secundaria, solapamiento temporal, dificultad de cada tarea.
- **Métricas:** coste de doble tarea (single − dual) en TR y PC de cada tarea, priorización.
- **Riesgo práctica:** automatizar una de las dos y "soltar" la otra.
- **Mitigación:** alternar cuál es prioritaria; secundaria variable; vigilar que ninguna se abandone.
- **Principiante:** secundaria muy esporádica y fácil. **Avanzado:** secundaria frecuente. **Élite:** ambas exigentes, prioridad cambiante, alto solapamiento (sin convertirlo en sobrecarga crónica).

### E16 · Reenganche
- **Capacidad:** recuperación post-error y estabilización.
- **Descripción:** paradigma con dificultad calibrada para inducir errores controlados; se entrena y mide la vuelta inmediata a rendimiento estable.
- **Reglas:** tras un error, exigencia de re-estabilización en los siguientes ensayos; sin "congelarse" ni acelerar impulsivamente.
- **Dificultad:** tasa de error inducida, ventana de recuperación, presión temporal tras error.
- **Métricas:** post-error slowing (PES) adaptativo, PC e IIV en ensayos post-error, tiempo hasta línea base.
- **Riesgo práctica:** anticipar dónde llegará el error.
- **Mitigación:** errores surgen de dificultad adaptativa, no de trampas fijas.
- **Principiante:** ventana de recuperación amplia. **Avanzado:** recuperación en 2-3 ensayos. **Élite:** re-estabilización inmediata bajo presión, sin sobrecorrección ni impulsividad.

---

## 5. Sistema adaptativo de dificultad

### 5.1 Principio rector
La presión temporal **nunca** sube por responder rápido. Sube solo cuando se cumplen **simultáneamente**: precisión suficiente, impulsividad bajo techo y estabilidad aceptable. Así es imposible "progresar" siendo rápido-impreciso.

### 5.2 Señales de entrada (por mini-bloque de 12–16 ensayos)
precisión (PC) · TR medio · TR mediano · variabilidad intraindividual (CV / IIV) · percentil 90 de lentitud (cola lenta) · falsas alarmas · omisiones · impulsividad · lapsos · fatiga · estabilidad por bloque · recuperación post-error (PES) · coste de interferencia · coste de cambio de regla.

### 5.3 Reglas de decisión (staircase gateado por precisión)
Sea `D` la ventana/deadline de respuesta (menor `D` = más presión). Tras cada mini-bloque:

- **SUBIR presión (D↓):** si `PC ≥ A_high` (≈0.85) **y** `ImpIdx ≤ I_cap` **y** `CV ≤ V_cap` **y** sin subida de τ. Paso adaptativo proporcional al margen.
- **BAJAR presión (D↑):** si `PC < A_low` (≈0.70) **o** `ImpIdx > I_cap` **o** omisiones↑ **o** anticipaciones↑.
- **MANTENER:** si `A_low ≤ PC < A_high`: recoger más datos antes de mover.
- **ESTABILIZAR (no subir aunque PC sea alta):** si `CV` alto o `τ`/lapsos en aumento → insertar bloque de estabilización a la misma `D` hasta consolidar.
- **CAMBIAR TIPO DE CARGA (no solo nivel):** si la `VCE` se estanca con métricas saturadas → rotar dominio (velocidad pura → +inhibición → +cambio de regla → +interferencia) manteniendo dificultad subjetiva equivalente.
- **REGRESAR / COOLDOWN:** si `FatIdx` supera umbral o cae el rendimiento intra-sesión → reducir carga o terminar.

### 5.4 Fórmulas conceptuales

**(a) Velocidad Cognitiva Efectiva (VCE)** — por condición `c`:
```
VCE_c = (PC_c ^ β) / ( RTmed_c · (1 + λ · CV_c) )
```
- `PC_c`: proporción de aciertos (0–1).
- `RTmed_c`: mediana de TR de ensayos correctos (s).
- `CV_c`: coeficiente de variación de TR (= SD/media) → penaliza inestabilidad (IIV).
- `β` (≈2): peso de precisión (castiga fuerte la imprecisión).
- `λ` (≈0.7): peso de penalización por variabilidad.
- *Sube* al responder más rápido **y** más preciso **y** más estable. No sube por velocidad impulsiva.

**(b) Carga Cognitiva Efectiva (CCE)** — normalizada 0–1:
```
CCE = Σ wᵢ · dᵢ      (Σ wᵢ = 1)
```
Drivers `dᵢ` (cada uno normalizado 0–1): tasa de presentación · set size/distractores · interferencia/incongruencia · frecuencia de cambio de regla · estrechez del deadline · ambigüedad de evidencia · doble tarea · novedad del estímulo. Se usa para situar al usuario en su **zona óptima de desafío** (la `CCE` que produce ≈78–85% de aciertos).

**(c) Índice de Eficiencia Velocidad-Precisión (IEVP):**
```
IEVP = z(VCE) − γ · z(ImpIdx)
```
Premia la velocidad eficiente y **resta explícitamente** la velocidad impulsiva (`γ`≈1). Se complementa con el **diagnóstico de desplazamiento de frontera** entre dos evaluaciones:
- *Mejora eficiente:* `ΔRT<0` y `ΔPC ≥ 0` → la frontera sube (objetivo).
- *Trade sobre la frontera:* `ΔRT<0` y `ΔPC<0` proporcional → no es mejora, es canje.
- *Impulsividad:* `ΔRT<0` y `ΔPC` cae más de lo esperado por el trade → bloquear progreso.

**(d) Índice de Impulsividad (ImpIdx):**
```
ImpIdx = wa·FA_nogo + wb·anticip(<150ms) + wc·fastErrorShare + wd·(1 − SSRT_norm)
```
`fastErrorShare` = fracción de errores cuyo TR es menor que la mediana de TR de aciertos (firma de respuesta impulsiva: errar más rápido de lo que se acierta).

**(e) Índice de Fatiga (FatIdx):**
```
FatIdx = z(pendiente_VCE↓ por bloque) + z(Δτ) + z(Δlapsos) + z(decremento_vigilancia)
```

---

## 6. Sistema de puntuación y métricas

### 6.1 Clasificación de métricas

| Tipo | Métricas | Para qué |
|---|---|---|
| **Rendimiento** (estado actual) | PC, TR simple, TR de elección, TR mediano, FA, omisiones, anticipaciones, coste de interferencia, coste de cambio | Dónde estás hoy |
| **Aprendizaje** (cambio real) | Δ de cada índice vs. baseline, desplazamiento de frontera, transferencia a no entrenadas, pendiente longitudinal | Si de verdad mejoras |
| **Fiabilidad** (¿es señal o ruido?) | IIV/CV, error test-retest, consistencia split-half, lapsos, estabilidad por bloque | Si el cambio es creíble |

Índices derivados: velocidad perceptiva · velocidad de decisión · control a alta velocidad · eficiencia cognitiva (`VCE` con bajo coste subjetivo de esfuerzo y baja fatiga) · transferencia cercana.

### 6.2 Por qué el TR medio no basta
La media oculta la **forma de la distribución**. Dos usuarios con igual TR medio pueden diferir radicalmente en **τ** (cola lenta/lapsos), en **IIV** (estabilidad) y en **posición en la frontera** (uno rápido-preciso, otro rápido-impreciso compensando). El sistema reporta mediana + IIV + τ + percentil 90 + tasa de lapsos, no solo la media.

### 6.3 Puntuación de cara al usuario
La puntuación visible **no** es "más rápido = más puntos". Es una función de la `VCE` y del `IEVP`, con **techo si la precisión, la estabilidad o el control caen** bajo umbral. Responder rápido e impreciso *reduce* la puntuación. Se muestran tres barras: **Velocidad útil**, **Precisión/Control** y **Estabilidad**, para que el usuario vea el equilibrio, no solo el cronómetro.

---

## 7. Diseño de sesiones

Fases canónicas: **calentamiento → calibración breve → núcleo de velocidad → pico de dificultad → bloque de control/inhibición → bloque de transferencia → cooldown → reflexión metacognitiva.**

| Duración | Cuándo usarla | Estructura |
|---|---|---|
| **5 min** | Días de poco tiempo, mantenimiento, "micro-dosis" | Calentamiento 30 s · calibración 30 s · núcleo 3 min · cooldown + reflexión 1 min. Sin pico ni transferencia. |
| **10 min** | Sesión estándar entre semana | Calentamiento 1 min · calibración 1 min · núcleo 4 min · control/inhibición 2 min · cooldown 1 min · reflexión 1 min. |
| **20 min** | Sesión completa | Calentamiento · calibración · núcleo · **pico** · control · **transferencia** · cooldown · reflexión, con 1–2 micro-descansos. |
| **30 min** | Días dedicados, fines de semana, fase de carga alta | Como la de 20 + segundo bloque temático + transferencia ampliada + más descansos. Solo si `FatIdx` lo permite. |

**Descansos:** micro-pausas de 20–40 s entre bloques; pausa mayor si `FatIdx`↑. **Reglas de recuperación:** tras pico de dificultad, bloque más fácil obligatorio. **Criterios de interrupción/reducción:** si en dos bloques seguidos cae `VCE`, sube `ImpIdx` o se disparan omisiones → reducir carga o cerrar sesión en cooldown (nunca terminar en pico).

---

## 8. Progresión por ciclos (8 semanas)

| Sem | Foco | Volumen | Intensidad | Ejercicios | Dificultad dominante | Evaluación | Objetivo adaptativo | Señales de riesgo | Ajuste si fatiga/meseta |
|---|---|---|---|---|---|---|---|---|---|
| **1** | Baseline + velocidad perceptiva/reacción | Bajo | Baja-media | E1,E2,E3,E6 | Perceptiva, TR | **Baseline completo** | Estabilizar técnica, fijar línea base | Anticipaciones, IIV alta | Reducir presión, alargar exposición |
| **2** | Velocidad de elección y comparación | Medio | Media | E3,E4,E6,E5 | Elección, comparación | Retest parcial | Bajar TR de elección sin perder PC | Errores rápidos | Bajar `A_high`, más calibración |
| **3** | **+ Inhibición** | Medio | Media-alta | +E7,E8 | Control inhibitorio | Sonda go/no-go no entrenada | Reducir FA manteniendo TR de go | FA↑, impulsividad | Más no-go, menos presión en go |
| **4** | Consolidación (deload ligero) | **Bajo** | Media | Mezcla suave | Estabilidad/IIV | Retest + 1ª transferencia | Reducir IIV y τ | Fatiga acumulada | Semana de menor volumen |
| **5** | **+ Cambio de regla** | Medio-alto | Alta | +E10,E5(reglas) | Coste de cambio | Sonda switching no entrenada | Bajar coste de cambio | Perseveración | Switch predecible temporalmente |
| **6** | **+ Interferencia** | Alto | Alta | +E9, combinados | Coste de interferencia | Retest + transferencia | Bajar coste interferencia con PC alta | Caída de PC bajo conflicto | Reducir proporción incongruente |
| **7** | Integración (multimodal + doble tarea + sostenida) | Alto | **Pico** | E11–E16 combinados | Carga combinada bajo control | Sonda de transferencia funcional | Mantener `VCE` bajo carga combinada | Sobrecarga, saturación | Quitar doble tarea, acortar bloques |
| **8** | **Consolidación + evaluación final** | **Bajo (deload)** | Media | Repaso + tests | Estabilidad y retención | **Evaluación completa + transferencia** | Confirmar mejora real vs. práctica | — | Cerrar ciclo, planificar siguiente |

**Cuándo introducir cada exigencia:** interferencia y cambio de regla **no** antes de tener velocidad de elección estable (sem. 3–6). Presión temporal fuerte solo sobre condiciones ya dominadas en precisión. Doble tarea al final (sem. 7), nunca como base. **Deload** programado (sem. 4 y 8) y a demanda si `FatIdx` sostenido.

---

## 9. Evaluación inicial y recalibración

### 9.1 Protocolo de baseline (≈12–15 min, una vez)
Mide, con suficientes ensayos por condición para fiabilidad: **TR simple** (separar componente motor) · **TR de elección** (pendiente de Hick) · **velocidad perceptiva** (umbral de inspección) · **búsqueda visual** (pendiente TR/ítem) · **comparación rápida** (ítems/min) · **inhibición bajo velocidad** (FA, SSRT) · **cambio de regla** (coste de switch) · **velocidad sostenida** (decremento) · **fatiga** (caída intra-test) · **IIV** (CV, τ). Se guardan **distribuciones**, no solo medias, y el **error de medida** por condición.

### 9.2 Retests y sondas
- **Retest semanal:** subconjunto corto de las condiciones entrenadas, mismo formato, para curva longitudinal.
- **Pruebas NO entrenadas:** paradigmas con la misma capacidad subyacente pero formato distinto (controlan efecto práctica específico).
- **Transferencia cercana:** tareas estructuralmente relacionadas no practicadas (p.ej. búsqueda con estímulos nuevos, decisión con material semirrealista).

### 9.3 Interpretación: mejora real vs. efecto práctica
Una ganancia se considera **real** solo si: (1) supera el **error test-retest** de esa condición (no es ruido); (2) aparece también en **tareas no entrenadas** y/o **transferencia cercana**; (3) es **eficiente** (frontera sube: `ΔRT<0` con `ΔPC≥0`), no un canje; (4) no se explica por **mejora motora** ni por estímulos memorizados; (5) se mantiene en **deload** (retención). Ganancia solo en lo entrenado y sin transferencia = efecto práctica.

### 9.4 Detección de perfiles (a partir de PC, TR, IIV, FA, τ, FatIdx)
| Perfil | Firma cuantitativa |
|---|---|
| Rápido-impreciso | TR bajo + PC baja + `fastErrorShare` alta + FA↑ |
| Lento-preciso | TR alto + PC alta + IIV baja |
| Variable | IIV/CV alto + τ alta + PC media |
| Fatigable | `FatIdx` alto, decremento intra-sesión marcado |
| Impulsivo | `ImpIdx` alto, anticipaciones, FA en no-go |
| Estable | PC alta + IIV baja + τ baja |
| Élite | TR bajo **y** PC alta **y** IIV baja **bajo** interferencia/cambio/incertidumbre |

---

## 10. Sistema anti-estancamiento

### 10.1 Señales de meseta (cuantitativas)
1. Precisión estable **sin** reducción de TR durante ≥2 semanas.
2. Reducción de TR **con caída de PC** (mejora falsa = canje).
3. **IIV/CV creciente** (rendimiento más frágil).
4. **Lapsos** (τ, percentil 90) repetidos.
5. **Coste de interferencia / cambio** elevado y sin bajar.
6. **Sin transferencia** a pruebas no entrenadas pese a mejorar en lo entrenado.
7. **Fatiga acumulada** (`FatIdx` sostenido entre sesiones).

### 10.2 Qué cambiar y reglas concretas
- **Estímulos / posiciones / reglas / modalidad:** rotar si la mejora se concentra en lo familiar (señal 6) → introducir set nuevo y medir si la ganancia persiste.
- **Dominio cognitivo:** si `VCE` satura en velocidad pura → mover a velocidad + inhibición/cambio/interferencia (misma dificultad subjetiva).
- **Presión temporal:** si señal 2 → **subir el gate de precisión** (`A_high`) y relajar deadline hasta recuperar precisión, luego volver a presionar.
- **Ratio velocidad/precisión:** si señal 3 → priorizar precisión y estabilidad varios bloques antes de volver a velocidad.
- **Descansos / volumen / deload:** si señal 7 → semana de descarga; si señales 3+4 juntas → bloque de estabilización obligatorio.
- **Bloque de transferencia:** si señal 6 → aumentar peso de transferencia y usarla como criterio de avance.
- **Objetivo de sesión:** redefinir de "más rápido" a "más estable" o "menor coste de interferencia" según la señal dominante.

---

## 11. Transferencia funcional

> No se promete transferencia lejana automática. Se diseñan **puentes** entre el mecanismo entrenado y la situación real, y se **mide** si el puente funciona.

### 11.1 Microprotocolos (mecanismo → aplicación)
| Situación real | Mecanismo entrenado | Puente práctico | Cómo medir transferencia |
|---|---|---|---|
| Estudio | comparación + clasificación rápida | sesiones cortas de discriminación sobre material de estudio real | velocidad/precisión en ejercicios con su propio material |
| Lectura rápida con comprensión | velocidad perceptiva + sostenida | RSVP con texto + preguntas de comprensión | palabras/min **con** comprensión ≥ umbral |
| Trabajo profundo | velocidad sostenida + recuperación post-error | bloques largos baja densidad + reenganche | decremento de vigilancia, retorno tras interrupción |
| Decisión bajo presión | decisión bajo información parcial | "Niebla" con coste de error realista | calidad de decisión vs. tiempo, no impulsividad |
| Competición / videojuegos | elección + inhibición + MOT | Multivía + Compuerta + Órbita | TR de elección y FA bajo presión |
| Deporte | reacción + go/no-go + búsqueda | señales tipo "salida"/"finta" | TR a señal válida, FA a finta |
| Trading / análisis rápido | decisión bajo info parcial + actualización | "Niebla" + actualización con coste asimétrico | precisión de decisión bajo incertidumbre |
| Reuniones exigentes | cambio de regla + actualización | Bisagra con contenido verbal | coste de cambio en contexto |
| Resistencia a distracciones digitales | interferencia + inhibición | Conflicto + Compuerta con notificaciones simuladas | FA a distractor, mantenimiento de PC |
| Comunicación rápida | elección + clasificación | Multivía con respuestas verbales | TR sin pérdida de precisión |
| Priorización de tareas | clasificación + cambio de regla | Clasificador multidimensional | TR de categorización correcta |

### 11.2 Medición de transferencia funcional
Sondas estandarizadas (con material semirrealista, **no** practicadas como entrenamiento) + reporte estructurado del usuario antes/después. Se considera transferencia funcional cuando hay mejora en la sonda **y** señal convergente en el reporte, no solo una de las dos.

---

## 12. Personalización por perfil

Para cada perfil: prioridad · ejercicios recomendados · ejercicios a limitar · métricas clave · progresión ideal · riesgos · criterios de avance.

| Perfil | Prioridad | Recomendados | Limitar | Métricas clave | Progresión | Riesgos | Avance si… |
|---|---|---|---|---|---|---|---|
| **Principiante** | Técnica y línea base | E1,E2,E3,E6 | Doble tarea, interferencia fuerte | PC, IIV | Lenta, mucha calibración | Frustración temprana | PC estable + IIV baja |
| **Baja velocidad general** | Subir techo perceptivo | E1,E2,E5 | Cambio de regla complejo | TR perceptivo, umbral | Gradual, exposición amplia→corta | Desánimo | TR baja sin perder PC |
| **Impulsivo** | Control inhibitorio | E7,E8,E16 | Go/no-go con prisa excesiva | ImpIdx, FA, SSRT | Gate de precisión alto | Reforzar prisa | ImpIdx↓ con TR estable |
| **Lento-preciso** | Empujar velocidad con red | E6,E11, presión gradual | Bloques largos lentos | TR, frontera | Subir presión sobre PC ya alta | Romper su precisión | ΔRT<0 con ΔPC≥0 |
| **Rápido-impreciso** | Recuperar precisión | E2,E9,E16 | Premios por velocidad pura | PC, fastErrorShare | Bajar presión, subir gate | Premiar impulsividad | PC↑ sin perder TR útil |
| **Alta variabilidad** | Estabilidad | E10,E16, sostenida | Picos de dificultad frecuentes | IIV, τ, lapsos | Mucho bloque de estabilización | Sesiones inconsistentes | IIV y τ↓ sostenidas |
| **Se fatiga rápido** | Gestión de carga | Sesiones 5–10 min | Sesiones 30 min, picos largos | FatIdx, decremento | Volumen bajo, deload frecuente | Sobreentrenamiento | Menor decremento intra-sesión |
| **Ansioso bajo presión** | Control con calma | E11,E16, presión suave | Deadlines agresivos al inicio | IIV bajo presión, PES | Exposición gradual a presión | Bloqueo/abandono | Mantiene PC al subir presión |
| **Avanzado** | Carga combinada | E9,E10,E15 | Repetir lo ya dominado | Costes (interf./cambio) | Multimodal, élite parcial | Meseta por falta de variación | Bajan costes con PC alta |
| **Élite** | Velocidad **bajo control** | E8,E11,E14,E15 combinados | Velocidad pura sin contexto | Frontera bajo interferencia, SSRT | Incertidumbre + ruido + cambio | Sobreentrenamiento, aburrimiento | Mejora con interferencia/incertidumbre |
| **Gamer competitivo** | Elección + inhibición + MOT | E6,E7,E14 | Sostenida monótona | TR elección, FA, rastreo | Alta intensidad, mucha rotación | Transferir solo a un juego | TR/FA mejoran en sondas variadas |
| **Estudiante exigente** | Comparación + sostenida + transferencia estudio | E3,E5,E12 + §11 | Picos de presión innecesarios | Velocidad sostenida, transferencia | Integrar material real | Olvidar comprensión | Mejora en sonda de lectura con comprensión |
| **Profesional bajo presión** | Decisión + cambio + interferencia | E10,E11,E9 | Reflejos puros sin contexto | Calidad de decisión, coste cambio | Escenarios semirrealistas | Confundir prisa con eficacia | Mejor decisión bajo incertidumbre |

---

## 13. Gestión de fatiga, impulsividad y sobrecarga

### 13.1 Qué detectar
fatiga cognitiva · fatiga visual · impulsividad · frustración · sobreentrenamiento · caída de precisión · exceso de presión temporal · saturación por estímulos.

### 13.2 Índices y umbrales
- **`FatIdx`** (§5.4e): pendiente de `VCE`↓ por bloque + Δτ + Δlapsos + decremento de vigilancia.
- **`ImpIdx`** (§5.4d): FA en no-go + anticipaciones + `fastErrorShare` + SSRT lento.
- **Frustración (proxy):** racha de errores + abandono de respuestas + caída brusca de PC tras errores + (si disponible) señales de interacción.
- **Saturación:** subida simultánea de omisiones y τ con flujo alto.

### 13.3 Reglas de actuación
- **Pausa:** micro-pausa si `FatIdx` > umbral en un bloque.
- **Regresión:** bajar `CCE` un escalón si `FatIdx` o `ImpIdx` sostenidos dos bloques.
- **Cambio de ejercicio:** rotar dominio/modalidad ante saturación por estímulo.
- **Finalización temprana:** cerrar en cooldown si caen `VCE` y PC dos bloques seguidos (nunca terminar en pico).
- **Recuperación activa:** bloque fácil de baja presión + reflexión metacognitiva tras pico o tras racha de errores.
- **Anti-sobreentrenamiento:** límites de volumen diario/semanal; deload programado; respetar señales entre sesiones (no solo intra-sesión).

---

## 14. Errores de diseño a evitar

- **Confundir velocidad con impulsividad:** premiar TR bajo sin gate de precisión → entrena a errar rápido.
- **Confundir velocidad cognitiva con reflejo motor:** no separar el componente motor → "entrenas el dedo".
- **Usar solo la media de TR:** se pierde IIV, τ y la forma de la distribución → autoengaño.
- **"Menos tiempo = más difícil" como único eje:** ignora precisión, estabilidad y control.
- **Estímulos/posiciones/reglas fijas:** la mejora se vuelve memorización, no procesamiento.
- **Prometer transferencia lejana o subida de CI:** pseudociencia; daña credibilidad y ética.
- **No medir contra el error test-retest:** confundir ruido con progreso.
- **Sin pruebas no entrenadas ni de transferencia:** imposible distinguir efecto práctica.
- **Gamificación que premia velocidad bruta:** refuerza el comportamiento equivocado.
- **Picos de dificultad sin recuperación:** fatiga, frustración, abandono.
- **Ignorar la dinámica post-error:** se cae el rendimiento real donde más importa.
- **Stroop/interferencia como único pilar:** sobreajuste a un paradigma, sin cubrir las subcapacidades.
- **Adaptar solo por velocidad:** permite progresar siendo rápido-impreciso.
- **Sin deload ni límites de volumen:** sobreentrenamiento.

Cada uno es peligroso porque rompe la cadena "medir bien → adaptar bien → mejorar de verdad → transferir": basta un eslabón roto para que el sistema produzca *engagement* sin mejora cognitiva real.

---

## 15. Versión "best in the world" — 10 características

1. **Descomposición de cada respuesta** en velocidad perceptiva / de decisión / motora / control (modelo de acumulación de evidencia), no un TR único. *(medición avanzada)*
2. **Frontera velocidad-precisión individual** estimada por condición; el progreso es *desplazarla*, no recorrerla. *(detección de mejora falsa)*
3. **Adaptación multi-señal gateada** por precisión + impulsividad + estabilidad simultáneas; imposible avanzar siendo rápido-impreciso. *(adaptación fina + protección contra impulsividad)*
4. **Métricas de distribución** (mediana, IIV/CV, τ, percentil 90, lapsos), no medias. *(medición avanzada)*
5. **Separación estricta** de entrenamiento / evaluación / calibración, con sondas **no entrenadas** y de **transferencia cercana** como criterio de avance. *(detección de mejora falsa + transferencia)*
6. **Entrenamiento multimodal** (visual, auditivo, doble tarea, MOT) para entrenar mecanismos, no patrones. *(entrenamiento multimodal)*
7. **Generación procedimental** de estímulos/reglas/posiciones para anular la memorización. *(detección de mejora falsa)*
8. **Personalización por perfil** (impulsivo, variable, fatigable, élite…) con prioridades, límites y criterios propios. *(personalización por perfil)*
9. **Análisis longitudinal** con retención bajo deload y separación señal/ruido vs. error test-retest. *(análisis longitudinal)*
10. **Velocidad bajo control como techo élite:** el nivel máximo es rápido **con** interferencia, incertidumbre, cambio de regla y alta precisión, nunca solo "más rápido". *(progresión élite + entrenamiento de velocidad bajo control)*

---

## 16. Tabla maestra (especificación accionable)

| Módulo | Capacidad | Ejercicios | Métricas | Progresión | Señal de fatiga | Señal de impulsividad | Avance si… | Regresión si… | Objetivo de transferencia |
|---|---|---|---|---|---|---|---|---|---|
| M1 Perceptiva | Codificación/discriminación | E1,E2 | umbral, PC, IIV | exposición↓, contraste↓ | τ↑, lapsos↑ | anticipaciones | PC alta a exposición corta | PC<A_low | lectura, escaneo |
| M2 Reacción | TR simple | E1 | TR, anticip., lapsos | ventana↓ | decremento | anticip.>umbral | TR↓ sin anticip. | anticip.↑ | señales/salidas |
| M3 Elección | Choice RT | E6 | TR, Hick, PC | alternativas↑, mapeo rota | decremento | fastErrors | TR↓ con PC≥A_high | PC<A_low | comunicación, gaming |
| M4 Comparación | Comparar símbolos | E3,E2 | ítems/min, IIV | clave↑, cambia | IIV↑ | errores rápidos | ítems/min↑ con PC alta | PC↓ | estudio |
| M5 Búsqueda | Búsqueda visual | E4 | pendiente/ítem, omis., FA | set↑, conjunción | omisiones↑ | FA↑ | pendiente↓ con PC | FA↑ | trabajo, deporte |
| M6 Clasificación | Categorizar | E5 | TR, coste regla | reglas↑, combinada | decremento | errores rápidos | coste↓ con PC | PC<A_low | priorización |
| M7 Inhibición | Go/no-go | E7,E8 | FA, SSRT, TR go | proporción go↑, SSD adapt. | TR go↑ | FA, ImpIdx↑ | FA↓ con TR go estable | FA↑ | distracciones |
| M8 Cambio regla | Switching | E10 | coste cambio, perseverac. | switch aleatorio | decremento | switch errors | coste↓ con PC | perseverac.↑ | reuniones |
| M9 Interferencia | Conflicto | E9 | coste interf. (TR,PC) | %incong↑, distractor↑ | PC bajo conflicto↓ | fastErrors | coste↓ con PC alta | PC<A_low | foco con ruido |
| M10 Sostenida | Vigilancia | E12, largos | decremento, τ | bloque↑, objetivos raros | decremento↑, τ↑ | omis./FA | decremento↓ | FatIdx↑ | trabajo profundo |
| M11 Recuperación | Post-error | E16 | PES, PC post-error | ventana↓ | IIV post-error↑ | sobrecorrección | re-estabiliza rápido | PES errático | resiliencia bajo presión |
| M12 Transferencia | Aplicación | §11 | sondas no entrenadas | estímulo realista↑ | — | — | mejora en sondas | sin transferencia | situaciones reales |

---

## 17. Preguntas críticas finales (20)

1. **Población objetivo:** ¿optimizamos primero para usuario general o para perfiles de alto rendimiento, dado que cambia los valores por defecto de gate, presión y volumen?
2. **Ambición científica:** ¿nos comprometemos públicamente a no reclamar transferencia lejana ni subida de CI, como diferenciador de marca?
3. **Métrica norte:** ¿cuál es la métrica primaria de éxito del módulo: `VCE`, `IEVP`, o transferencia cercana medida?
4. **Definición de "mejora real":** ¿qué tamaño de cambio sobre el error test-retest aceptamos como señal y no ruido?
5. **Duración por defecto:** ¿la sesión estándar es de 10 min, y cuándo empujamos a 20–30 sin dañar adherencia?
6. **Frecuencia semanal:** ¿qué frecuencia mínima recomendamos para esperar mejora real sin sobreentrenar?
7. **Tolerancia a dificultad:** ¿qué rango de PC objetivo fijamos (p.ej. 78–85%) y cómo lo ajustamos por perfil ansioso/fatigable?
8. **Gate de impulsividad:** ¿dónde ponemos el techo de `ImpIdx` por encima del cual se bloquea la subida de presión?
9. **Componente motor:** ¿qué método usamos para estimar y descontar el TR motor en cada plataforma/dispositivo?
10. **Modalidad auditiva:** ¿cuánto peso damos a lo auditivo (E13) dado que depende de hardware y entorno del usuario?
11. **Pruebas de transferencia:** ¿qué set de sondas no entrenadas estandarizamos y con qué periodicidad?
12. **Generación procedimental:** ¿qué reglas garantizan ítems nuevos sin que la dificultad derive de forma incontrolada?
13. **Gamificación:** ¿cuánta gamificación sin que premie velocidad bruta y contradiga la filosofía velocidad-útil?
14. **Puntuación visible:** ¿mostramos las tres barras (velocidad útil / control / estabilidad) o un único número, y cómo evitamos que el usuario "juegue" a una sola?
15. **Deload:** ¿el deload es automático por `FatIdx` o también programado por calendario, y cómo lo comunicamos sin desmotivar?
16. **Perfiles iniciales:** ¿cuántos ensayos de baseline necesitamos para clasificar perfil con fiabilidad aceptable?
17. **Longitudinal:** ¿cuánto historial guardamos y cómo presentamos la curva sin inducir interpretación errónea del ruido?
18. **Integración con Foco:** ¿cómo se coordinan Velocidad Mental y el módulo Foco para no duplicar carga ni solapar inhibición/sostenida?
19. **Criterio élite:** ¿qué umbrales concretos definen "élite" (TR + PC + IIV bajo interferencia) y cómo evitamos que se vuelva inalcanzable o aburrido?
20. **Validación interna:** ¿haremos un estudio interno (pre/post con grupo control de práctica) antes de afirmar eficacia, y con qué N y diseño?
