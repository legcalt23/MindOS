# Agent, Model & Tool Routing (/ceo)

Gobierna etapa 9. `/ceo` **nombra** agentes en el paquete; no los ejecuta (un subagente no puede lanzar
subagentes). La ejecución la hace la sesión principal o `/mb`. La sesión principal conserva ownership de
integración y cierre; los agentes asesoran, implementan segmentos o auditan.

**Antes de nombrar un agente, verifica que existe** en `.claude/agents/` (el validator lo comprueba;
`/mb` conoce los 10 base). No inventes agentes, comandos ni herramientas.

## 1. Inline vs especialista

**Inline** (0 agentes) cuando: tarea pequeña, local, determinista, bajo riesgo, verificable por
compilación/tests, sin competencia diferencial, y el coste de coordinación supera el beneficio.

**Especialista** cuando aporta conocimiento diferencial, independencia, reducción de riesgo, aislamiento
de contexto, revisión adversarial o paralelización segura — sobre todo en: ciencia cognitiva,
psicometría, scoring, claims, privacidad, seguridad, integridad de datos, arquitectura central,
migraciones, personalización, accesibilidad crítica, UX central, monetización, rendimiento sistémico.

Presupuesto: bajo riesgo 0 · medio 1 · implementación con gate 1–2 · feature multidisciplinar 2–4 · >4
requiere justificación explícita. No dos agentes del mismo propósito salvo debate científico real,
revisión adversarial, comparación de alternativas o coste de error excepcional.

## 2. `/mb` y los 10 especialistas base (verificados en `.claude/agents/`)

`/mb <objetivo>` convierte la sesión principal en lead orchestrator y activa los 10 base. Gates de
validez (lectura): `efficacy-guardian`, `psychometrician`, `diff-auditor`, `privacy-security`.
Arquitectura: `kmp-architect`. Implementación (escritura): `cognitive-engine-dev`, `data-sync-dev`,
`compose-ui-dev`, `motion-experience` (tras UI funcional). Verificación: `test-engineer`.

Orden estándar de slice (de `docs/context-pack/06 §7`): gate de entrada `efficacy-guardian`+`psychometrician` →
`kmp-architect` (si hay fronteras) → núcleo `cognitive-engine-dev`(+`data-sync-dev` si contratos
congelados) → `compose-ui-dev` → `motion-experience` → `test-engineer` → **gate de salida no negociable**
`efficacy-guardian`+`diff-auditor` (+`psychometrician` si cambió medición, +`privacy-security` si tocó
datos/permisos/auth/IA/red, +`kmp-architect` si cambió arquitectura).

## 3. Equipos ampliados (nombrar explícitamente; no auto-invocados por `/mb`)

`mindboost-*` (19) para diseño/ciencia/UX/coach/personalización/ingeniería/datos/QA/docs. `mindverse-*`
(31) cuando la decisión trasciende MindBoost (estrategia, marca, growth, monetización, legal, plataforma,
ML, seguridad de empresa). Matriz de routing completa en `docs/context-pack/06 §10`; resumen:

| Cambio | Base `/mb` | Añadir explícitamente |
|---|---|---|
| Ejercicio nuevo | efficacy, psychometrician, engine, UI, motion, test, diff | `mindboost-training-gameplay-lab`; `mindboost-cognitive-science` si mecanismo nuevo |
| Score/calibración | efficacy, psychometrician, engine, test, diff | `mindboost-assessment-psychometrics` + `mindboost-assessment-diagnostics` |
| Transferencia | efficacy, psychometrician, test, diff | `mindboost-cognitive-transfer-agent` + `mindboost-experimentation` |
| Plan Free personalizado | efficacy, psychometrician, engine/data/UI/test/diff | `mindboost-personalization-engine` + `mindboost-product-ux` |
| AI Coach Elite | privacy, data/UI/test/diff | `mindboost-ai-coach` + `mindboost-coach-behavior-designer` + `mindverse-ai-platform-engineer` + `mindverse-finance-unit-economics` |
| Auth/VPS | privacy, architect, data, test, diff | `mindverse-security-privacy` + `mindverse-backend-platform` + `mindverse-legal-compliance` + `mindverse-devops-sre` |
| Dashboard/rangos | efficacy, psychometrician, UI/test/diff | `mindboost-product-ux` + `mindboost-growth-retention`; privacy si social |
| Pricing Elite | — | `mindverse-monetization-pricing` + `mindverse-finance-unit-economics` |

La tabla es baseline, no excusa para lanzar todos los agentes. Regla de ownership: un archivo/frontera
tiene **un** owner por slice (write_set disjunto); otros proponen por diff/mensaje separado.

## 4. Contrato de tarea por agente nombrado (de `docs/context-pack/06 §11`)

Rol exacto y autoridad · objetivo único · contexto obligatorio (docs/archivos/decisiones/commit) ·
alcance · fuera de alcance (qué no tocar/cablear) · preguntas a resolver · criterios de aceptación ·
evidencia requerida (diff/archivo/test/cita/cálculo) · formato de entrega (P0–P3, cambios, tests,
unknowns, veredicto APTO/NO_APTO) · read/write · orden/dependencias · gate que debe verificar. Prohibido
"revísalo todo" sin scope ni criterio.

## 5. Selección de modelo y effort (relativa; no hardcodear nombres volátiles)

Exploración/extracción mecánica: modelo eficiente, effort bajo. Arquitectura/ciencia/decisiones críticas
e integración multidisciplinar: máxima capacidad, effort alto. Reviewer crítico: contexto independiente
+ alta capacidad. Validación estructural: script determinista (el validator), no un modelo. Deja que la
plataforma resuelva el nombre concreto; expresa "eficiente" vs "alta capacidad".

## 6. Agente inexistente

No lo inventes. En orden: busca el equivalente real en `.claude/agents/` · ejecútalo inline · usa un
generalista (`general-purpose`) con misión precisa · propón crear uno **solo** con recurrencia demostrada
(`PROPUESTA`, no como existente). Declara la limitación (`NO DISPONIBLE`) si nada encaja.
