# MindBoost — Capa de Conocimiento Científico (Cognitive Science Corpus)

> **Qué es esto.** Un corpus curado de fichas de conocimiento *originales* sobre capacidad
> cognitiva humana, cerebro, aprendizaje y entrenamiento cognitivo. Cada ficha sintetiza una obra
> (o un cluster temático) y separa de forma explícita **lo que la evidencia respalda** de **lo que
> NO respalda**, para que la frontera de claims de MindBoost se derive de la evidencia y no del
> marketing. Es una **herramienta de decisión interna**, no copy de usuario.
>
> **Qué NO es.** No contiene texto completo con copyright, ni resúmenes que sustituyan a las obras.
> Las fichas son síntesis estructurada + cita bibliográfica. No es contenido de producto ni genera
> pantallas nuevas.

## Por qué existe

MindBoost es un sistema **medible** de progreso cognitivo cuya ventaja competitiva es la
**honestidad de medición** (rigor charter: `null ≠ 0`; nunca fabricar señal; nunca prometer
transferencia lejana no probada). Ese rigor solo es defendible si está anclado en la literatura
real. Este corpus es esa ancla: cada ficha conecta una mecánica REAL del repo con la evidencia que
la sostiene o la limita, y se ingiere al grafo Graphify (`graphify-out/`) para que
`graphify query/path/explain` responda preguntas de eficacia cruzando **código ↔ ciencia**.

## Frontera de honestidad (no negociable)

1. **Separación obligatoria** en cada ficha entre claims respaldados y no-respaldados. Ninguna ficha
   promete mejora de inteligencia general ni transferencia lejana no probada.
2. **`null ≠ 0`.** La evidencia débil o ausente se declara como tal; no se convierte en una promesa
   suave.
3. **Vocabulario prohibido.** El corpus (en `docs/`) SÍ puede usar terminología científica real
   (constructos, nombres de tests clásicos, crítica de la industria). Pero **nada de ese vocabulario
   puede copiarse** a copy de producto, comentarios de código ni tests dentro de `shared/src/**`
   (lo imponen `ForbiddenLexicon.kt` + scanners). El corpus define la frontera de lo que MindBoost
   *puede afirmar*, expresado luego en lenguaje permitido ("perfil cognitivo funcional", "práctica",
   "progreso frente a tu propia línea base", "dificultad adaptativa", "orientativo").

## Esquema de una ficha

Un archivo `.md` por **obra** o por **cluster temático** (cuando varias obras forman un único
argumento, p. ej. los meta-análisis escépticos de entrenamiento de WM). Prefijos de agrupación:

| Prefijo | Familia |
|---|---|
| `F##_` | Fundamentos (constructos, neurociencia, estructura de la inteligencia) |
| `T##_` | Entrenamiento cognitivo y transferencia (incl. literatura escéptica) |
| `L##_` | Ciencia del aprendizaje (retrieval, spacing, dificultades deseables) |
| `M##_` | Metacognición y autorregulación |
| `B##_` | Cambio de conducta y hábito (adherencia) |
| `S##_` | Estado y rendimiento (sueño, ejercicio, estrés/arousal) |
| `D##_` | Decisión y sesgos |
| `P##_` | Medición / psicometría (sostiene la honestidad) |

### Frontmatter YAML (fijo)

```yaml
---
title: "Título de la obra o del cluster"
authors: "Apellido, Inicial.; Apellido, Inicial."   # autores principales
year: 2013                                            # año (o rango "2002–2016" para clusters)
tier: T1                                              # T1 | T2 | T3 (ver abajo)
domains: [working_memory, learning_strategy]         # subconjunto de los 11 dominios canónicos
constructs: [working_memory, cognitive_control]      # constructos psicológicos tratados
paradigms: [complex_span, n_back]                    # mecánicas de laboratorio (vacío si no aplica)
mindboost_anchors:                                    # rutas REALES del repo a las que aplica
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/ComplexSpanScorer.kt
evidence_level: strong                                # strong | moderate | weak | contested
---
```

**Dominios canónicos** (los 11 del producto; `domains` es un subconjunto de estos):
`attention`, `working_memory`, `processing_speed`, `logical_reasoning`, `verbal_reasoning`,
`spatial_reasoning`, `creativity`, `metacognition`, `decision_making`,
`emotional_regulation_for_performance`, `learning_strategy`.

**Mapeo dominio canónico → `CognitiveDimension` del repo**
(`profile/domain/model/CognitiveTaxonomy.kt`): `attention→ATTENTION`,
`processing_speed→PROCESSING_SPEED`, `working_memory→WORKING_MEMORY` (+`EPISODIC_MEMORY` para
recuerdo diferido), `logical_reasoning→REASONING`, `verbal_reasoning→VERBAL`,
`decision_making→DECISION_MAKING`, `emotional_regulation_for_performance→EMOTIONAL_REGULATION`,
`creativity→CREATIVITY`. `spatial_reasoning`, `metacognition` y `learning_strategy` **no** tienen
dimensión de perfil dedicada hoy (son transversales / huecos declarados).

### Tiers de evidencia

- **T1** — Literatura primaria de alta calidad: meta-análisis, RCTs, revisiones de consenso,
  statements de consenso. Es la fuente que fija la frontera de claims.
- **T2** — Libros académicos rigurosos y capítulos de handbook (teoría y síntesis de campo).
- **T3** — Divulgación de calidad, basada en evidencia pero **marcada como divulgación**, con sus
  críticas de replicación/exageración anotadas. Nunca se cita un T3 para *sostener* un claim de
  producto sin un T1/T2 que lo respalde.

### `evidence_level` (fuerza del cuerpo de evidencia del tema)

`strong` (replicado, meta-analítico, consenso) · `moderate` (evidencia real pero limitada o mixta) ·
`weak` (preliminar, subpotente, sin réplica sólida) · `contested` (debate activo o crisis de
replicación).

### Secciones fijas del cuerpo

Toda ficha tiene **exactamente** estas seis secciones, en este orden:

- **(a) Tesis central** — 2–4 frases: qué afirma la obra/cluster.
- **(b) Claims RESPALDADOS** — lista, cada uno con el **tipo de evidencia** entre corchetes, usando el
  descriptor metodológico más preciso — típicamente uno de `[meta-análisis]`, `[RCT]`, `[experimental]`,
  `[correlacional]`, `[longitudinal]`, `[factor-analítico]`, `[revisión]`, `[consenso]`, `[teoría]`.
- **(c) Claims NO respaldados o refutados** — qué **no** puedes prometer citando esta obra. Es la
  sección más importante para la frontera de honestidad.
- **(d) Estado de replicación / críticas** — robustez, tamaños de efecto, críticas metodológicas,
  crisis de replicación cuando aplica.
- **(e) Implicaciones de diseño para MindBoost** — accionables y **específicas**, referenciando
  mecánicas reales del repo por nombre (ejercicio, scorer, gate, dimensión de perfil): dificultad
  adaptativa, dosis, spacing, medición, claims permitidos.
- **(f) Referencias** — cita bibliográfica completa (APA-ish). Sin enlaces a texto pirateado.

## Cobertura por dominio canónico (mapa vivo)

Marca `T1 ✓` cuando existe al menos una ficha T1 que cubra el dominio. Actualizar al añadir fichas.

| Dominio canónico | T1 | T2 | T3 | Fichas |
|---|---|---|---|---|
| attention | ✓ | ✓ |  | F02 (Miyake/Friedman EF), F03 (Posner networks) |
| working_memory | ✓ | ✓ |  | F01 (Baddeley), F02, T02 (Melby-Lervåg/Hulme), T05 (n-back) |
| processing_speed | ✓ | ✓ |  | F04 (speed/RT), T04 (ACTIVE/UFOV), P03 (RT methodology) |
| logical_reasoning | ✓ | ✓ |  | F05 (CHC), T05, T07 (far transfer) |
| verbal_reasoning | ✓ | ✓ |  | F05 (CHC/Gc, T1), F08 (verbal/vocabulario) |
| spatial_reasoning | ✓ |  |  | F07 (Uttal 2013 meta-análisis) |
| creativity | ✓ | ✓ |  | C01 (divergent thinking / creatividad) |
| metacognition | ✓ | ✓ |  | M01 (Dunlosky metacog), M02 (Zimmerman SRL) |
| decision_making | ✓ | ✓ | ✓ | D01 (Kahneman), D02 (Gigerenzer), D03 (calibración) |
| emotional_regulation_for_performance | ✓ | ✓ |  | S03 (arousal/estrés + Gross) |
| learning_strategy | ✓ | ✓ | ✓ | L01–L07, M02, B01/B02 |

**Fichas transversales de honestidad (aplican a todos los dominios):** T01 (Simons 2016), T03 (Owen
2010), T06 (consenso Stanford), T07 (near vs far transfer), P01 (fiabilidad/validez), P02 (efectos
de práctica/test-retest).

## Cómo se usa (después de la ingesta)

```bash
graphify query "¿qué evidencia respalda o limita el entrenamiento de memoria de trabajo?"
graphify path "E4_FRENO" "inhibitory_control"
graphify explain "far_transfer"
```

El corpus se fusiona al grafo existente vía el flujo `--update` de la skill `/graphify` (extracción
semántica sobre los `.md`); `graph.json` permanece gitignored — lo que se versiona es este `docs/`.

## Reglas de mantenimiento

1. No añadir una ficha sin las 6 secciones y el frontmatter completo.
2. No inflar la sección (b): un claim sin `[tipo de evidencia]` no entra.
3. La sección (c) nunca puede quedar vacía en una ficha de entrenamiento/transferencia.
4. Un T3 nunca es la única fuente de un claim de producto.
5. Ningún término de `ForbiddenLexicon.kt` debe migrar de aquí a `shared/src/**`.
6. Al añadir/editar fichas, re-ejecutar el `--update` de graphify para mantener el grafo al día.
