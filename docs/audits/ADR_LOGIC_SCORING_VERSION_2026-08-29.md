# ADR — Marcar la versión del instrumento de scoring de Lógica en vez de reescribir el histórico

**Fecha:** 2026-08-29 · **Hallazgo:** `MB-EFFICACY-0017` · **Estado:** ACEPTADO
**Antecedente:** `MB-EFFICACY-0002` (bug de precedencia en el bloque de penalización de `DefaultReasoningScorer`, cerrado el mismo día)

---

## 1. El problema, medido (no el enunciado heredado)

`MB-EFFICACY-0002` arregló un bug de precedencia que inflaba el ICR. El arreglo no toca lo ya escrito
en disco, así que un usuario con historial previo conserva sesiones medidas con el instrumento viejo.

La fila que yo mismo escribí para `MB-EFFICACY-0017` afirmaba tres cosas. **Dos eran falsas y la
tercera se quedaba corta.** Todo lo de abajo está medido sobre el árbol, no razonado.

### 1.1 FALSO — «el bucle adaptativo mueve theta con valores inflados»

`DefaultLogicDifficultyAdapter.decide(current, recentAttempts)` recibe los intentos **por parámetro**, y
el único call-site de producción (`LogicStateHolder:211`) le pasa los `attempts` de la sesión **en
curso**, in-memory. No lee el repositorio. La decisión de dificultad nunca vio un valor persistido.

Lo que sí es acumulativo es `SkillState.thetaEstimate`
(`old.thetaEstimate + (averageIcr − TARGET_ICR) / THETA_SCALE`): cada sesión pasada empujó ese valor y
la parte purgada es **irrecuperable**, porque solo se guarda la integral, no la serie. Sería el daño
más duro de la fase… salvo por el hecho decisivo:

> **`thetaEstimate` no tiene ningún consumidor.** Barrido de todo `shared/src`: dos apariciones, la
> definición del campo y su propia escritura. Nadie lo lee para decidir nada — ni la dificultad, ni el
> perfil (que lee `uncertainty`, un decay puro independiente del ICR), ni la UI.

Un valor escrito y jamás leído no puede contaminar una afirmación. La contaminación irreversible existe
y **no tiene consecuencia viva**.

### 1.2 FALSO — «dos pantallas vivas lo leen»

`LogicStateHolder.loadHome` usa `sessions.size`, el conteo. No toca el ICR.

### 1.3 CIERTO, y PEOR de lo que la fila decía

`LogicProgressStateHolder` es el único consumidor real, y lo que hace no es «mostrar una serie mixta»:

```kotlin
val delta = newer.average() - older.average()
when {
    delta >  TREND_EPSILON -> IMPROVING     // TREND_EPSILON = 2.0 puntos ICR
    delta < -TREND_EPSILON -> DECLINING
    else -> STABLE
}
```

La mitad **vieja** de la serie está inflada; la mitad **nueva** es correcta. El arreglo hace que
`delta` salga negativo por sí solo. Con la penalización conceptual pasando de 1.5 a 15 puntos, superar
un epsilon de 2.0 es trivial.

> **Nuestro propio arreglo fabrica un veredicto `DECLINING`** —«Tendencia: a la baja ↘»— en cualquier
> usuario con historial mixto. Es una afirmación adversa sobre la persona causada por un cambio de
> nuestro instrumento.

Es la forma **atributiva** de `null != 0` (learning 2026-08-26): un límite de nuestro diseño presentado
como un hecho sobre el usuario. Ése es el defecto que esta fase tiene que impedir, y no estaba escrito
en ninguna parte.

### 1.4 IMPRECISO — «subir `CURRENT_SCHEMA_VERSION`»

`readVersioned` solo cuarentena cuando `version > currentVersion` (dato de una versión **más nueva**).
Una versión más vieja se devuelve tal cual. Subir la versión de schema **no migra nada**: es, como mucho,
la marca de que una migración escrita aparte ya corrió.

---

## 2. Las salidas, y por qué gana la tercera

### (a) Migrar: recomputar `totalIcr` y `averageIcr` desde los campos persistidos

**Es técnicamente posible y exacta.** `ReasoningScore` conserva los seis sumandos de `weightedBase` y
los dos flags de penalización como 0/1 independientes, así que el valor correcto es una función pura de
lo ya guardado. Y los pesos **nunca han cambiado**: `git log -p --follow` sobre `ReasoningScorer.kt`
devuelve exactamente una aparición de cada constante, en el commit inicial, siempre como línea añadida.
Recomputar con los pesos de hoy reproduce byte a byte lo que el scorer habría escrito con el paréntesis
puesto.

**Se rechaza igualmente, y no por imposibilidad sino por riesgo asimétrico.** La migración reescribe el
agregado cifrado de datos personales del usuario: si el recálculo tuviera un defecto, el original se
pierde para siempre y no hay forma de re-derivarlo. Lo que compra a cambio es recuperar una flecha de
tendencia unas semanas antes de que la ventana de 30 sesiones purgue sola. Coste potencial irreversible
sobre datos del usuario frente a beneficio pequeño y temporal: no sale.

### (b) Declarar que se purgue y no hacer nada

Deja el veredicto `DECLINING` falso vivo hasta que la ventana purgue. Inaceptable una vez medido que no
es «una serie mixta» sino una afirmación adversa fabricada.

### (c) ACEPTADA — marcar el instrumento y negarse a comparar a través de él

No se toca un solo byte del usuario. Las sesiones declaran con qué versión de la ecuación se midieron, y
el consumidor comparativo devuelve `UNKNOWN` cuando la serie cruza esa frontera.

Tiene precedente explícito en este repo, así que es coherencia y no invención: el learning de 2026-07-10
ya decidió, para otro módulo, **no seleccionar la escala autoritativa por recencia, preservar todas y
marcar cross-escala como `NON_COMPARABLE`**. Aquí es el mismo hecho: dos instrumentos, series no
comparables entre sí, cada una válida en su tramo.

Y el veredicto de renuncia **ya existe y ya tiene copy honesta**: `LogicProgressTrend.UNKNOWN` se pinta
como «Tendencia: en calibración». No hay que inventar ni un estado ni una cadena.

---

## 3. El diseño

1. `LogicScoringVersion` vive **junto a la ecuación** (`ReasoningScorer.kt`), que es lo que versiona:
   `LEGACY_PRECEDENCE_BUG = 1` y `CURRENT = 2`.
2. `LogicSessionResult.scoringVersion: Int = LEGACY_PRECEDENCE_BUG`.
   **El default es fail-closed y ésa es la pieza clave:** una sesión sin marca es, por definición,
   anterior a que la marca existiera. Si el default fuera `CURRENT`, todo el histórico contaminado se
   auto-declararía actual y el guard sería decorativo — el mismo fallo de la familia `null != 0`.
   El default es además **obligatorio** por compatibilidad: sin él, `decodeFromString` fallaría sobre el
   JSON ya escrito, `readParsed` cuarentenaría la clave y **se perderían los datos del usuario**. Un
   campo sin default aquí no es rigor, es borrado.
3. `logicIcrTrend` pasa a recibir `List<LogicIcrPoint>` (icr + versión) y devuelve `UNKNOWN` en cuanto la
   serie contiene más de una versión. El guard vive **dentro de la función pura**, no en el holder, para
   que sea imposible construir el veredicto saltándoselo.

### Criterios de aborto, declarados ANTES de implementar

- **No se escribe en el store del usuario.** Ni migración, ni reescritura, ni `CURRENT_SCHEMA_VERSION`
  tocada. El diff no puede contener una llamada de escritura nueva.
- **Una serie homogénea se comporta EXACTAMENTE igual que hoy**, en las cuatro ramas del clasificador.
- **Ningún dato existente se vuelve ilegible**: un JSON sin el campo nuevo debe seguir deserializando.

---

## 4. Techos declarados

- Los números por-sesión anteriores al arreglo **siguen siendo los que esa sesión produjo**. No se
  ocultan ni se corrigen. `latestAverageIcr` puede por tanto ser un valor del instrumento viejo mientras
  no haya una sesión nueva. Se acepta a propósito: es un dato histórico que no afirma ninguna dirección,
  a diferencia del veredicto de tendencia, que sí. Ampliar el alcance a ocultarlo empeoraría la pantalla
  sin evidencia de daño.
- `SkillState.recentIcr` se sobrescribe entera en la primera sesión que toque ese skill: se auto-limpia
  sin que nadie haga nada.
- `SkillState.thetaEstimate` queda contaminado de forma permanente e irrecuperable. **No se arregla
  porque no lo consume nadie**; si algún día alguien lo cablea, hereda el problema. Registrado como
  hallazgo aparte con dueño, no cerrado en silencio.
- El guard protege la **tendencia de la pantalla de progreso**, y solo esa. No hay nada mecánico que
  obligue a otro consumidor comparativo a consultar la versión: la disciplina no está impuesta.
- **Ya existe un segundo consumidor comparativo**, y no es hipotético:
  `LogicProfileSource.trend()` (`profile/data/adapter/LogicProfileSource.kt:103`) hace exactamente la
  misma comparación mitad-antigua contra mitad-nueva sobre `averageIcr`, con el mismo `TREND_EPSILON`.
  **No se toca en esta fase, y la razón se midió en vez de suponerse** (el error de `MB-CATALOG-0021`
  fue dar por inerte una ruta sin comprobar QUÉ la apaga):
  - Su único llamador es la línea 67, dentro de `contribution()`.
  - `contribution()` retorna `null` en su primera instrucción:
    `measurementEligible(UNKNOWN, lineageComplete = false)` con **ambos argumentos literales**, y
    `disposition(UNKNOWN, _)` devuelve `Indeterminate(PURPOSE_NOT_SEALED)`, que no es `Eligible` bajo
    ninguna entrada. La denegación es del sistema de tipos, no de una configuración.
  - Luego `trend()` es **inalcanzable**, no "apagado por un vecino".
  - El propio comentario D1 de ese fichero **ya nombra esta carencia** como una de las dos razones por
    las que deniega ("no versioned scoringVersion/scale-key ... NOR comparability-established"), y
    declara que el estimador de la carrier slice "must partition by scale-key". Tiene dueño escrito.
  - Esta fase **aporta esa scale-key** (`scoringVersion`) y **no abre el gate**: `purpose` sigue siendo
    `UNKNOWN` y `lineageComplete` sigue siendo `false`, ambos literales sin tocar. El cambio es
    estrictamente aditivo respecto de la elegibilidad de perfil.

## 5. Correcciones a este mismo documento

- §4 decía "cualquier consumidor comparativo **futuro**". Es **falso**: al medir el alcance apareció
  `LogicProfileSource.trend()`, que ya existe hoy. El bullet se reescribió arriba con la medición.
  Se registra como corrección en vez de reescribirse en silencio, porque la diferencia entre "puede
  aparecer uno" y "ya hay uno" es justo la que decide si el techo es una advertencia o un hallazgo.
