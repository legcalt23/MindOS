# Frontera de autoridad del ADR-FOCUS-TEMPORAL-QUALITY — disposición de `MB-FOCUS-0079` y `MB-FOCUS-0080`

**Fecha:** 2026-08-28 · **Alcance:** `shared/src` · **Estado del motor:** R3 sigue CANDIDATE (este documento no
congela, monta, promueve ni declara paridad, measurement-ready ni launch-ready).

---

## 1. El defecto, en una frase

`docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md` es **VIGENTE en su portada y RETIRADO por dentro**: la línea
524 declara `# HISTORIA — R2 (SUPERSEDED por R3, conservada íntegra como evidencia; NO es autoritativa)`, y todo lo
que hay debajo es evidencia conservada. Nada distinguía una cita a ese bloque usada como **evidencia** de una usada
como **autoridad viva**, y la única frontera era una línea de prosa que ningún mecanismo leía.

---

## 2. Medición del alcance (reproducible, sustituye a la cifra anterior)

El registro previo de `MB-FOCUS-0079` decía «68 citas en 29 ficheros». **Esa cifra no es reproducible**: se midieron
ocho predicados candidatos distintos de «cita por sección llana» sobre el árbol vivo y ninguno la produce. La
medición defendible, sobre `shared/src` excluyendo `build/`:

| Magnitud | Valor |
|---|---|
| Ficheros `.kt` que nombran el ADR | **19** |
| Líneas con una cita por sección llana | **29** |
| …de las que apuntan al ADR | **27** |
| …de las que apuntan a otro documento (`charter`) | **2** |
| Secciones llanas distintas citadas | **14** |
| Secciones llanas en el ADR / secciones `R3-*` | 21 / 23 |

Las 2 del `charter` se descubrieron porque un KDoc parte `charter` y su signo de sección en **dos líneas**, así que
un clasificador que sólo mire la línea de la cita las atribuye al documento equivocado. La atribución se lee de la
línea citante **más la cola de la anterior**.

El alcance en `docs/` (613 citas) queda **fuera**: clasificar prosa no es lo que hace que una cláusula retirada se
comporte como un contrato vivo. Registrado como alcance residual, no como cobertura.

---

## 3. El hallazgo que la medición produjo y el enunciado del hallazgo no contenía

**El bloque VIGENTE no es autocontenido.** Medido sobre las 14 secciones retiradas que producción cita:

| Sección retirada citada | ¿La menciona el bloque vigente (líneas 1..523)? | Sitios en `shared/src` |
|---|---|---|
| §3 | sí (1) | 5 |
| §3.1 | **no** | 1 |
| §4 | sí (2) | 1 |
| §4.1 | **no** | 3 |
| §5 | **no** | 4 |
| §7 | sí (4) | 3 |
| §8 | sí (4) | 3 |
| §9 | sí (5) | 1 |
| §11 | **no** | 2 |
| §12 | sí (2) | 3 |
| §13 | **no** | 2 |
| §14 | sí (4) | 2 |
| §15 | **no** | 1 |
| §17 | **no** | 2 |

Siete de catorce el bloque vigente **no las nombra**, y las otras siete **las invoca él mismo**, citando hacia abajo,
hacia su propia historia. La taxonomía T0/T1, `AC#3`/`AC#4` y la aritmética del backstop se usan como obligaciones
vivas y sólo están definidas por debajo del marcador.

**Consecuencia de diseño, y la razón de que el guard NO sea una prohibición:** partir el ADR en dos documentos —el
otro candidato— habría sido la forma equivocada del guard. Con el bloque vigente apoyado en definiciones que sólo
existen abajo, una cita a la historia es a veces **necesaria**. Lo que no puede pasar es que se lea como autoridad.
Prohibirlas habría obligado a duplicar definiciones o a romper 27 sitios honestos.

Registrado como hallazgo propio (`MB-FOCUS-0084`) — **REFUTADO el 2026-08-29, ver §7**; NO cerrado aquí: reconciliar el bloque vigente para que sea
autocontenido es decisión de contrato, no plumbing de esta fase.

---

## 4. Lo implementado

### 4.1 Atribución (`MB-FOCUS-0079`)

Cada una de las 27 citas al bloque retirado lleva el token de evidencia `[R2-HIST]` junto a la sección. **No se
re-apuntó ninguna cita a una sección `R3-*`.** El token dice de qué documento y de qué bloque procede el número,
no que la cláusula siga vigente.

> **Corrección 2026-08-29 (fase `MB-FOCUS-0084`).** La justificación que esta sección daba —«para la mayoría no
> existe sucesora»— es FALSA y no se había medido. Medido: **21 de las 27** marcas citan una sección retirada que
> SÍ tiene una sucesora viva candidata (§3→`§R3-9`, §5→`§R3-7`, §11→`§R3-18`, §13→`§R3-14`, §8→`§R3-10`,
> §14→`§R3-12/13/17`, §17→`§R3-20`, §7→`§R3-6`, §15→`§R3-11`). La decisión de no re-apuntar SIGUE siendo correcta,
> pero por otra razón: la sucesión es un juicio por sección, no un hecho derivable, y congelarla en marcas de
> código la volvería norma sin revisión. Se verificó además que **no hay daño hoy**: se auditaron los dos casos
> con mayor riesgo de contradicción —§5 vs `§R3-7` (¿tiene drain#1 consumidor?) y §11 vs `§R3-18` (¿siguen los
> umbrales en sombra?)— y en ambos el KDoc ya enuncia la verdad viva. Techo declarado: `[R2-HIST]` afirma
> procedencia y calla sobre supersesión; nada impide que una marca futura repita una cláusula ya superada.

### 4.2 El guard es una TAREA GRADLE, y eso se decidió por medición

`:shared:adrAuthorityBoundaryScanner` (`shared/build.gradle.kts`), enganchada a `check`.

La primera implementación fue un test de `desktopTest`, y **la mutación la refutó**: al quitar un marcador real, el
resultado fue `> Task :shared:desktopTest UP-TO-DATE` con **0 XML frescos**. El marcador vive en un **comentario**, y
una edición de comentario que no mueve líneas deja el bytecode idéntico, así que la tarea de test hereda su verde
justo ante la deriva que el guard existe para cazar. Un `@InputDirectory` hashea el **contenido** del fichero, así que
la misma edición sí re-ejecuta la tarea. Un solo dueño: el test se **borró**.

Lo que la tarea comprueba:

1. El marcador de retiro existe **exactamente una vez** y declara `SUPERSEDED`.
2. Ninguna sección llana está por ENCIMA del marcador y ninguna `R3-*` por DEBAJO — con anti-vacuidad (≥20 y ≥20),
   porque un patrón de cabecera que dejara de casar haría vacuamente ciertas ambas mitades.
3. Toda cita por sección llana en `shared/src` está **atribuida**: token de evidencia, o pertenencia a otro
   documento de la allowlist. Anti-vacuidad de 27 citas en 19 ficheros.
4. **Mitad discriminante ejecutada por el propio gate:** re-escanea el corpus en memoria con el token borrado y
   exige que el número de no-atribuidas iguale EXACTAMENTE al de marcadas. Sin ella, «cero no-atribuidas» podría
   significar que el clasificador no mira.

**Techo declarado:** afirma que una cita está ATRIBUIDA, nunca que la atribución sea VERDADERA. Si una cláusula
retirada fue arrastrada o no es un juicio por sección, y vive en este documento, no en el gate.

El escáner **escribe el signo de sección como escape** (`§`), para que no pueda encontrarse a sí mismo.

### 4.3 `Verdict.Degraded` borrado (`MB-FOCUS-0080`)

`FocusTemporalEligibility.Verdict.Degraded` no tenía productor y estaba reservado para una fase que cerró sin
construirlo. La razón registrada para conservarlo —«el ADR §9 lo nombra como parte del vocabulario»— es **falsa por
la misma frontera**: §9 está en la línea 819, muy por debajo del marcador de la 524.

Retirada la razón, el bloqueo también se había disuelto: `D-R3-02` (CERRADA, R3-11) deja todo diagnóstico temporal en
SOMBRA y declara que ninguno produce evidencia de medición en S5, luego **no existe afirmación de medida que un
veredicto degradado pueda ablandar**, y no hay aritmética que congelar. La variante se BORRA, restaurando la simetría
con las entradas sin productor que Paso 5 borró de `WindowExclusionReason` en vez de reservarlas.

El pin de inalcanzabilidad del oráculo se retira **con** ella y lo sustituye un `when` exhaustivo sobre la jerarquía
sellada: si S6 reintroduce el tier, el compilador para hasta que alguien lo trate en el oráculo Y lo ejercite en la
allowlist de codominio. **Ausencia impuesta por el compilador, no vigilada en ejecución** — la regla del repo para
variantes sin productor.

`docs/engines/focus_v5/S5_PASO7_CLOSURE.md` se corrigió en sus DOS sitios (§4 tabla y el párrafo de `P7-F4`): la
disposición documental que allí se selló se apoyaba en una sección retirada.

---

## 5. Mutaciones ejecutadas

| # | Mutación | Resultado | Restauración |
|---|---|---|---|
| M1 | Quitar el token `[R2-HIST]` de `TemporalClassPolicy.kt:6` | **ROJO** — `Unattributed citation … [TemporalClassPolicy.kt:6]` | SHA-256 idéntico |
| M2 | Insertar `## 99.` (sección llana) por encima del marcador en el ADR | **ROJO** — `plainly numbered sections inside the LIVE block, at ADR lines [524]` | SHA-256 idéntico |
| M3 | `viaMarker = true` (clasificador que declara todo atribuido) | **ROJO** — `self-check failed: stripping the token flagged 0 sites, expected 29` | revertida, gate VERDE |
| M4 | Reintroducir `Verdict.Degraded` | **ROJO de compilación** en el `when` exhaustivo del oráculo | SHA-256 idéntico |

M3 confirma de paso el reparto medido: 29 citas totales, 27 al ADR y 2 al `charter`.

**Una mutación INCONCLUSA, registrada como tal:** el primer intento de M1, con el guard aún escrito como test, dio
`BUILD SUCCESSFUL` — pero la tarea salió `UP-TO-DATE` con 0 XML frescos, así que no probó nada sobre el guard. Es lo
que produjo §4.2.

---

## 6. Lo que esta fase NO hizo

- No se re-apuntó ninguna cita a una sección `R3-*` (sin sucesora medida, sería fabricar sucesión).
- No se reconcilió el bloque vigente para hacerlo autocontenido (`MB-FOCUS-0084`, abierto con dueño).
- No se clasificaron las 613 citas de `docs/` (alcance residual declarado).
- No se tocó `rt-infl-1`, `MB-FOCUS-0071` ni C1/C2/C3.
- Ningún wiring, montaje, promoción ni congelación. R3 sigue CANDIDATE.

---

## 7. Disposición de `MB-FOCUS-0084` (2026-08-29) — **REFUTADO por medición, no implementado**

`MB-FOCUS-0084` lo escribí yo en §3 de este documento y decía que el bloque vigente **invoca como obligaciones
vivas definiciones que sólo existen por debajo de su marcador de retiro**. Verificarlo antes de implementar su
gate de cierre refutó **tres** enunciados sucesivos — el original y los dos candidatos que lo sustituían:

| # | Enunciado | Veredicto | Evidencia que lo refuta |
|---|---|---|---|
| E1 | «el bloque vigente invoca la historia como autoridad» | **PARCIALMENTE FALSO** | los `§N` de las cabeceras `R3-*` son back-references al **contrato de reapertura** (plantilla en `07_PHASE_REVIEW_PROTOCOL.md`), no obligaciones R2 — pero el bloque vigente TAMBIÉN cita R2 con el mismo signo (`§6.1`, `§10.2`, `§10.3c`): `§7` en `§R3-19` es observer-effect mientras la R2 «7.» es «Sufficient statistics»; `§10` en `§R3-20` son 5 familias de tests mientras la R2 «10.» es «Detectores anti-cherry-picking» |
| E2 | «16 punteros de evidencia `§9.1..§9.16` colgantes» | **FALSO** | los 16 ítems existen y **son** `§R3-1..§R3-16`; el conteo casa exacto con «16 ítems de evidencia §9» de `§R3-22`, y `§10`↔`§R3-20` prueba la convención (`§R3-20` contiene exactamente 5 familias de tests) |
| E3 | «hay marcas que repiten una cláusula que su sucesora contradice» | **NO INSTANCIADO** | auditados los dos casos de mayor riesgo: §5 vs `§R3-7` (drain#1 sin consumidor) y §11 vs `§R3-18` (umbrales en sombra); en ambos el KDoc ya enuncia la verdad viva. `§R3-18` «umbrales ejecutables» es *resolver-o-quitar-consumidor* (el validador NO existe), no una promoción |

**Lo que la medición SÍ produjo, y es lo único que se remedia:** dentro del ADR el signo de sección está
**sobrecargado**, y la cita por sí sola no determina su destino. El bloque vigente cita con el mismo signo (a) el
**contrato de reapertura** «REAPERTURA OBLIGATORIA», cuya plantilla vive en
`docs/context_pack/07_PHASE_REVIEW_PROTOCOL.md` con numeración propia §1..§13 — su §8 «Agent routing obligatorio»,
su §10 «Tests y gates obligatorios» con exactamente 5 subsecciones y su §12 «Definition of Done específica» casan
uno a uno con los títulos de `§R3-21`, `§R3-20` y `§R3-22` —, y (b) las **secciones R2 retiradas de este mismo
ADR** (`§6.1`, `§10.2`, `§10.3c`). El caso que lo demuestra: el `§10` del título de `§R3-20` y el `§10.2` de unas
líneas más abajo apuntan a documentos distintos. El documento ya lo sabía: `§R3-17` desambiguaba un caso a mano y
dos citas llevan prefijo «R2» explícito. Remedio: una **nota de convención** al inicio del bloque vigente con las
dos ramas y sus pruebas. No cambia ninguna cláusula.

> **Corrección dentro de esta misma fase.** La primera versión de esa nota afirmaba que un `§N` desnudo del bloque
> vigente se lee «contra el contrato, NO contra las secciones R2». Es FALSO: `§6.1`/`§10.2`/`§10.3c` son R2. Lo
> detectó el gate adversarial (lente 1), que además localizó la plantilla del contrato EN EL REPO — yo la había
> inferido como «documento externo». La nota se reescribió a la forma de dos ramas antes de correr ningún gate.
> Es el mismo defecto que esta fase audita, cometido al redactar su propia corrección.

**Contribución del gate adversarial (18 agentes, read-only, `agentType: architect` sin Bash):** 5 lentes,
veredictos `PARCIALMENTE_FALSA` ×4 y **`CONFIRMADA`** ×1. La confirmada es la que más importa aquí: **lente 3
verificó 1-a-1 las 27 marcas `[R2-HIST]` contra el contenido real de la sección a la que apuntan** y no encontró
ninguna mal atribuida — corroboración independiente del trabajo de la fase anterior.

**Lo que deliberadamente NO se hizo, y por qué:** no se implementó el gate de cierre que `0084` prescribía
—reconciliar el bloque vigente para hacerlo autocontenido—, porque su premisa es falsa (regla 2026-08-19:
verificar el enunciado Y la prescripción de un hallazgo heredado antes de implementarlo; aquí el hallazgo era
mío y eso exige MÁS escrutinio, no menos, porque no tiene revisor externo). Tampoco se añadió guard mecánico:
la sobrecarga que queda es de prosa y no tiene consecuencia viva medida, y un guard sobre un invariante sin
consecuencia es el anti-patrón del símbolo sin consumidor aplicado a los gates.

**Cambio de código en esta fase: NINGUNO.** Sólo documentación y registro. R3 sigue CANDIDATE.
