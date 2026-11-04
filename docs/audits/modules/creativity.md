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
