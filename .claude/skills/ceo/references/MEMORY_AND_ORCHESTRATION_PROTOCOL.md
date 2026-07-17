# Protocolo de memoria, orquestación y epistemología para `/ceo` (Molvex)

Este archivo es la doctrina densa del `/ceo` reconstruido. El `SKILL.md` la referencia; **no**
la repitas entera en el output — extrae solo lo que la tarea concreta necesita.

> **Autoridad (v2, evitar doble autoridad):** este archivo es la visión NARRATIVA/panorámica. Para su
> tema concreto mandan los protocolos dedicados: modos y riesgo→`REQUIREMENT_RISK_AND_QUESTIONS_PROTOCOL.md`;
> secciones de salida por modo→`QA_EVAL_AND_SATURATION_PROTOCOL.md §8`; routing de agentes/modelo→
> `AGENT_MODEL_AND_TOOL_ROUTING.md`; etiquetas epistémicas, vigencia y conflictos→
> `TRUST_FRESHNESS_AND_CONFLICT_PROTOCOL.md`; nodo/DAG/validator/resumibilidad→
> `EXECUTION_DAG_AND_VALIDATION_PROTOCOL.md`; contrato de cada prompt→`PROMPT_ENGINEERING_PROTOCOL.md`.
> Si algo aquí choca con ellos, gana el dedicado.

`/ceo` es **read-only** y **no implementa código**: produce un *paquete de ejecución*
(diagnóstico + prompts operativos + revisión + integrador + secuencia) que ejecuta luego el
operador (sesión principal) o el orquestador `/mb`. La restricción dura del entorno —
**un subagente no puede lanzar otros subagentes**— es la razón: `/ceo` diseña la orquestación;
no la corre por dentro.

---

## 0. Naming Molvex ↔ código (decisión aprobada — no es inferencia)

**Molvex** es el naming de **producto/marca** (antes "MindBoost"). Decisión vigente del repo
(`docs/mindboost_profile_v2/PROFILE_V2_MASTER_PLAN.md:10` y `:504`): **CERO renombres de
paquetes, clases, ids o namespaces**. Por tanto:

- Externo (copy, marketing, onboarding, brief de founder) → **"Molvex"**.
- Interno (código, gates, agentes, docs de arquitectura) → siguen siendo **MindBoost /
  `com.mindos.app` / `:shared` / features/mindboost/**. Un prompt que diga "eres el equipo de
  Molvex" y luego mande `grep molvex` no encontrará nada: mapea siempre Molvex→entidades reales.
- Un "rebrand a Molvex en código" es una **migración deliberada fuera de alcance** por defecto;
  trátalo como PROPUESTA salvo que el founder lo pida explícitamente.

---

## 1. Los DOS sistemas de conocimiento reales (úsalos; no grep a ciegas)

El repo tiene conocimiento persistente en dos sistemas canónicos. La recuperación selectiva del
meta-prompt se ejecuta **con estas herramientas concretas**, no de forma abstracta.

### A. Grafo estructural del CÓDIGO — graphify / codegraph (primero, para "cómo/dónde")
- `graphify query "<pregunta>"` → subgrafo acotado (módulos, símbolos, llamadas, rutas).
  Variantes: `graphify explain "<Nodo>"`, `graphify path "<A>" "<B>"`, `graphify affected "<X>"`
  (impacto inverso — qué se rompe si cambio X).
- Si el MCP **codegraph** está conectado: `codegraph_explore "<pregunta o símbolos>"` devuelve
  el **source verbatim con líneas** + call-paths + blast-radius en una llamada. Preferirlo a
  Grep+Read para orientarte.
- **Nunca** leas `graphify-out/graph.json` en crudo (~600 KB). Frescura: compara
  `Built from commit:` de `graphify-out/GRAPH_REPORT.md` con `git rev-parse HEAD`.

### B. Memoria de DECISIONES/producto — Memory Layer CLI (para "qué se decidió y por qué")
- `node .claude/memory/cli.mjs recall "<query>"` → splice RAG de entradas relevantes; cada hit
  cita un `source:` file — **ábrelo y verifica antes de confiar**.
- `node .claude/memory/cli.mjs session-context --task "<query>"` → visión, arquitectura, roadmap
  activo, resúmenes de sesión recientes + lo relevante a la tarea.
- `node .claude/memory/cli.mjs list --type <T>` / `get <id>` para navegar.
- Si el header sale `index-stale`: `node .claude/memory/cli.mjs reindex` y reintenta.
- **15 tipos de memoria** (elige el tipo correcto al recuperar/priorizar): `PRODUCT_VISION`,
  `ARCHITECTURE`, `ADR`, `FEATURE_SPEC`, `ROADMAP`, `BUG_REPORT`, `SESSION_SUMMARY`,
  `CODING_STANDARD`, `SECURITY_NOTE`, `PROMPT_PATTERN`, `RESEARCH_NOTE`, `USER_PERSONA`,
  `API_CONTRACT`, `DATA_MODEL`, `DEPLOYMENT_NOTE`.

### C. Índice de memoria del agente + reglas activas
- `MEMORY.md` (auto-memoria) — índice de una línea por hecho; abre el archivo concreto para el
  detalle. Aquí viven los estados vivos: PROFILE V2, SYSTEM-1, Velocidad, Lenguaje, Arena,
  Decisiones, Emociones, brain-profile, etc.
- `CLAUDE.md` → sección **"Aprendizajes acumulados del proyecto"**: son **restricciones ACTIVAS**,
  no historia. Un aprendizaje que nombra un archivo/tarea/flag puede estar obsoleto: verifica que
  siga existiendo antes de imponerlo en un prompt.
- Reference files de esta skill (`REPO_CONTEXT.md`, `MINDBOOST_PRODUCT_DOCTRINE.md`,
  `KMP_TECHNICAL_DOCTRINE.md`, `PROMPT_ENGINEERING_PROTOCOL.md`) — doctrina densa ya destilada.

**Orden por defecto:** clasifica la tarea → (código) codegraph/graphify → (decisiones) recall +
session-context → MEMORY.md/CLAUDE.md aprendizajes → reference files → Read dirigido del feature
concreto solo para verificar líneas exactas.

---

## 2. Protocolo de recuperación (Fase A–F del meta-prompt, aterrizado)

- **A. Clasifica** la petición en 1+ categorías (producto, UX, iOS, Android, backend, datos, IA,
  entrenamiento cognitivo, ciencia, growth, monetización, marca, legal, privacidad, seguridad,
  analítica, testing, docs, estrategia…). La clasificación decide qué familia de agentes y qué
  sistema de memoria consultar.
- **B. Entidades:** extrae features/pantallas/flujos/módulos/ejercicios/métricas/segmentos/
  decisiones/riesgos implicados. Son las *queries* que lanzarás a graphify/recall.
- **C. Expansión relacional:** no te quedes en el nodo que coincide; sigue `depende-de`,
  `compuesto-por`, `sustituye-a`, `contradice-a`, `aprobado-por`, `bloqueado-por`, `mide`,
  `afecta-a`. `graphify affected "<X>"` y `codegraph_explore` te dan el blast-radius real.
- **D. Prioriza** por: pertinencia directa → autoridad de la fuente (decisión aprobada > plan >
  hipótesis) → estado de aprobación → recencia → dependencia arquitectónica → riesgo de
  contradicción → impacto.
- **E. Presupuesto de contexto:** cada agente del paquete recibe un **paquete contextual mínimo
  pero suficiente**. No copies todo lo recuperado en todos los prompts. Menos ruido = mejor output.
- **F. Integridad:** detecta contradicciones entre fuentes; marca versiones viejas; prioriza la
  decisión vigente; nunca mezcles PROPUESTA con DECISIÓN; no inventes lo ausente; declara los
  vacíos que cambian materialmente el resultado.

---

## 3. Etiquetado epistemológico (obligatorio en la salida)

Toda afirmación de contexto que uses o pases a un agente lleva etiqueta. Nunca promuevas
`HIPÓTESIS`→`HECHO`.

| Etiqueta | Significado | Regla |
|---|---|---|
| `HECHO` | Confirmado en código/gate/doc vigente | cita `source:` (archivo\:línea o memoria id) |
| `DECISIÓN` | Aprobada (ADR, plan aprobado, CEO sign-off) | cita la fuente y su estado |
| `PREF-FOUNDER` | Preferencia declarada del founder | no la trates como restricción técnica dura |
| `HIPÓTESIS` | Plausible, sin verificar | marca cómo verificarla |
| `PROPUESTA` | Idea nueva no aprobada (incl. libs/módulos nuevos) | jamás como si ya existiera |
| `INFERENCIA` | Deducción del modelo | etiquétala como tal |
| `NO-ENCONTRADO` | Vacío real que puede cambiar el resultado | dilo; no rellenes |

---

## 4. Selección de modo (proporcionalidad — auto, con 1 frase de justificación)

El antipatrón nº1 es desplegar arquitectura multiagente para una tarea trivial. Escala el output
al modo; **más abajo (§6) qué secciones emite cada modo**.

| Modo | Cuándo | Forma del paquete |
|---|---|---|
| **Directo** | tarea pequeña, bien definida, 1 disciplina (copy de un botón, ajuste UI local, fix acotado) | 1 prompt + criterios + riesgos. Sin mapa multiagente. |
| **Profesional** | complejidad media, 1–3 disciplinas, 1 feature end-to-end | recuperación + descomposición en 2–5 prompts + 1 revisión + integrador ligero. |
| **Enterprise** | alto impacto, varias disciplinas, decisión o entregable serio | recuperación profunda + arquitectura multiagente + dependencias + revisión especializada + red-team + integrador + trazabilidad. |
| **Misión Crítica** | arquitectura central, privacidad, seguridad, ciencia, monetización, migración, datos sensibles, difícil de revertir | todo lo Enterprise + alternativas + pre-mortem + plan de rollback + criterios de interrupción + observabilidad + validación independiente. |

`mode=` fuerza el modo. Si no se da, infiérelo del alcance/riesgo/reversibilidad. Ante duda entre
dos, elige el **más bajo** que cubra el riesgo (economía de contexto), salvo señales de misión
crítica (privacidad/seguridad/ciencia/dinero/irreversibilidad), donde subes.

---

## 5. Mapa de agentes REALES (enruta task→familia→agente; no inventes roles)

`/ceo` no ejecuta agentes: los **nombra en el paquete** y define qué hace cada uno y qué le pasas.
La ejecución de build normalmente se delega a **`/mb`** (que sí puede lanzar los toolsmith), o el
operador lanza el agente concreto con la tool Agent.

### Familia estrategia/plataforma (`mindverse-*`) — read-oriented, para decisiones de negocio/UX/infra
`mindverse-product-strategy-ceo`, `mindverse-north-star-metrics`, `mindverse-monetization-pricing`,
`mindverse-growth-engine`, `mindverse-retention-habit-loops`, `mindverse-onboarding-activation`,
`mindverse-research-and-insights`, `mindverse-brand-identity-director`, `mindverse-copy-conversion`,
`mindverse-legal-compliance`, `mindverse-security-privacy`, `mindverse-finance-unit-economics`,
`mindverse-design-system-architect`, `mindverse-ml-personalization`, `mindverse-data-engineer`,
`mindverse-mobile-platform`, `mindverse-frontend-platform`, `mindverse-backend-platform`,
`mindverse-devops-sre`, `mindverse-performance-engineer`, `mindverse-qa-release-engineer`,
`mindverse-enterprise-b2b`, `mindverse-localization-globalization`, `mindverse-community-network-effects`,
`mindverse-content-curriculum`, `mindverse-customer-success-support`,
`mindverse-trust-safety-moderation`, `mindverse-ai-platform-engineer`,
`mindverse-api-integration-architect`, `mindverse-operating-system-orchestrator`,
`mindverse-chief-platform-architect`.

### Familia MindBoost/producto (`mindboost-*`) — diseño y build del entrenador cognitivo
`mindboost-chief-architect`, `mindboost-cognitive-science`, `mindboost-product-ux`,
`mindboost-assessment-diagnostics`, `mindboost-assessment-psychometrics`,
`mindboost-personalization-engine`, `mindboost-backend`, `mindboost-frontend`,
`mindboost-data-analytics`, `mindboost-security-privacy`, `mindboost-qa-safety`,
`mindboost-ai-coach`, `mindboost-coach-behavior-designer`, `mindboost-experimentation`,
`mindboost-devops-release`, `mindboost-docs-knowledge`, `mindboost-mobile-performance`,
`mindboost-training-gameplay-lab`, `mindboost-cognitive-transfer-agent`.

### Toolsmith / código (los que ejecuta `/mb` — editan/revisan código real)
`efficacy-guardian` (gate eficacia), `psychometrician` (scoring/RT/dificultad),
`kmp-architect`, `cognitive-engine-dev`, `compose-ui-dev`, `motion-experience` (solo pulido bajo
petición), `data-sync-dev`, `privacy-security`, `test-engineer`, `diff-auditor` (gate de salida).
Reviewers ECC: `kotlin-reviewer`, `kotlin-build-resolver`, `swift-reviewer`, `typescript-reviewer`,
`security-reviewer`, `silent-failure-hunter`, `architect`, `planner`.

**Regla de selección:** activa solo los agentes que aporten. `mindboost-*` diseña; los toolsmith
implementan; `mindverse-*` decide negocio/estrategia. Un slice de build cognitivo típico:
`mindboost-cognitive-science`/`efficacy-guardian` (qué se mide) → `psychometrician` (cómo se
puntúa) → `mindboost-product-ux` (experiencia) → `cognitive-engine-dev`+`data-sync-dev` (build) →
`compose-ui-dev` → `test-engineer` → gate `efficacy-guardian`+`diff-auditor`. Máx 2–3 concurrentes.

### Skills a las que un prompt puede apoyarse
`create-cognitive-exercise`, `design-adaptive-scoring`, `review-cognitive-claims`,
`cognitive-science-principles`, `create-mindboost-feature`, `enterprise-feature-brief`,
`growth-experiment-design`, `qa-release-gate`, `launch-readiness-gate`, `privacy-safety-review`,
`scalability-review`, `production-audit`, `compose-multiplatform-patterns`, `kotlin-testing`,
`red-team`, `pre-mortem`, `steelman`, `primeros-principios`, `verification-loop`.

---

## 6. Contrato de salida por modo (qué secciones emitir)

Secciones canónicas (del meta-prompt §14). Cada modo emite un subconjunto — **no** fabriques
secciones vacías:

1. Interpretación ejecutiva · 2. Contexto recuperado (etiquetado) · 3. Alcance y criterios de
éxito · 4. Arquitectura de ejecución · 5. Mapa de prompts (tabla) · 6. Prompts completos · 7.
Prompts de revisión · 8. Prompt integrador final · 9. Riesgos y controles · 10. Secuencia de
ejecución · 11. Checklist de finalización.

| Sección | Directo | Profesional | Enterprise | Misión Crítica |
|---|---|---|---|---|
| 1 Interpretación | ✔ (breve) | ✔ | ✔ | ✔ |
| 2 Contexto recuperado | mínimo | ✔ | ✔ (profundo) | ✔ (profundo) |
| 3 Alcance/éxito | ✔ | ✔ | ✔ | ✔ |
| 4 Arquitectura ejecución | — | ligera | ✔ | ✔ |
| 5 Mapa de prompts | — | ✔ | ✔ | ✔ |
| 6 Prompts completos | 1 prompt | 2–5 | los que hagan falta | idem + ramas |
| 7 Prompts de revisión | — | 1 | ✔ (especializada + transversal) | ✔ + red-team |
| 8 Integrador final | — | ligero | ✔ | ✔ |
| 9 Riesgos y controles | 3 bullets | tabla | tabla | tabla + pre-mortem + rollback |
| 10 Secuencia de ejecución | — | ✔ | ✔ | ✔ + criterios de interrupción |
| 11 Checklist finalización | ✔ (corto) | ✔ | ✔ | ✔ |

Tabla del **Mapa de prompts**: `| Orden | Prompt | Función | Entrada | Salida | Dependencia |
Ejecución |` donde Ejecución ∈ {Secuencial, Paralela, Condicional, Final}.

---

## 7. Estructura obligatoria de CADA prompt operativo generado (§8 meta-prompt)

Identificador · Función (especialidad exacta) · Misión (resultado concreto) · Contexto de Molvex
(solo lo que ese agente necesita, etiquetado) · Entradas (docs/decisiones/resultados previos/
archivos exactos `shared/.../features/<x>/**`) · Trabajo requerido (pasos) · Decisiones que debe
tomar · Restricciones (técnicas + rigor charter + entorno) · Entregables · Formato de salida
(consumible por la fase siguiente) · Criterios de aceptación (observables) · Comprobaciones
obligatorias (errores/contradicciones/casos límite) · Handoff (qué pasa al siguiente y en qué
formato). Los prompts obligan a **trabajo real**, no explicaciones educativas.

---

## 8. Control de calidad en capas (§12 — solo si el modo lo pide)

1. **Autoverificación** del productor contra sus criterios de aceptación.
2. **Revisión especializada** (mismo dominio) — el agente reviewer correcto de §5.
3. **Revisión transversal** — producto/UX/negocio/privacidad/ciencia/rendimiento/plataformas.
4. **Integración ejecutiva** — el prompt integrador: resuelve contradicciones, elimina duplicados,
   comprueba dependencias, prioriza, consolida, verifica criterios, dictamina si el objetivo
   inicial se cumplió.

Enterprise añade red-team (`red-team`/`security-reviewer`/`efficacy-guardian` adversarial).
Misión Crítica añade pre-mortem, alternativas y validación independiente en contexto fresco.

---

## 9. Perspectiva multiplataforma (activar si toca el producto móvil)

Evalúa explícito: qué es común (commonMain) vs qué diverge por plataforma; navegación; permisos;
accesibilidad; almacenamiento (Android Keystore AES-GCM vs iOS Keychain — iOS **diferido** en
Windows); notificaciones (POST_NOTIFICATIONS API 33+); suscripciones/compras; rendimiento; ciclo
de vida; publicación; fuente de verdad compartida; riesgo de divergencia funcional; estrategia de
paridad. Nunca asumas que una solución iOS se copia literal a Android o al revés.

## 10. Perspectiva científica y ética (activar si toca cognición/medición/claims)

Distingue evidencia/hipótesis/marketing. Sin claims clínicos/diagnósticos/IQ. Sin transferencia
lejana no respaldada — mide contra el propio baseline. Considera efectos de práctica, fatiga,
frustración, accesibilidad, poblaciones vulnerables. Nada de patrones oscuros. `null ≠ 0`,
`ScoreResult` sellado, anti-gaming con red-team. Ante una petición de claim indefendible
(p. ej. "Molvex mejora la inteligencia"), **reconduce** la tarea a mensajes defendibles y dilo.

## 11. Trazabilidad (§15)

Cada decisión relevante del paquete se vincula a: la petición original · un HECHO (con source) ·
una restricción · una dependencia · un criterio de éxito · un riesgo · una DECISIÓN aprobada · o
una INFERENCIA etiquetada. Sin burocracia, pero sin decisiones sin fundamento.
