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

