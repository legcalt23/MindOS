# Execution Package Template (/ceo)

Esqueleto copy-paste de un paquete Enterprise. Recorta secciones por modo (`QA_EVAL_AND_SATURATION_PROTOCOL.md §8`).
No dejes placeholders resolubles; marca vacíos reales con `[FALTA: … — cómo obtenerlo]` (el validator los
detecta). El JSON validable se deriva de esto (`schemas/execution-package.schema.json`).

## 1. Interpretación ejecutiva
Qué se quiere · resultado final · problema · partes de Molvex implicadas · modo elegido + por qué (1 frase) · intent.

## 2. Snapshot (Enterprise+)
`commit · branch · dirty · modelos/agentes disponibles · capacidades soportadas/no soportadas`.

## 3. Contexto y provenance (etiquetado)
Lista de `context_item` (ver Trust protocol §2), con `epistemic_label` + `source`. Incluye `NO_ENCONTRADO` y `CONFLICTO`.

## 4. Conflictos y supuestos
Cada conflicto: fuentes, dominio, resolución provisional, `invalidation_condition`. Supuestos etiquetados.

## 5. Alcance y criterios de éxito
Incluido · excluido · antiobjetivos · entregables · Definition of Done.

## 6. Requisitos + matriz de trazabilidad
Tabla `requisito → nodo → estado → gate`. Un requisito material sin nodo/estado terminal = defecto.

## 7. DAG
Nodos y aristas (`dependencies`). Marca `parallel_group` y camino crítico.

## 8. Routing
Por nodo: agente real | inline · modelo (relativo) · effort · por qué.

## 9. Mapa de prompts
`| Orden | Prompt | Función | Entrada | Salida | Dependencia | Ejecución(Secuencial/Paralela/Condicional/Final) |`

## 10. Prompts completos — CONTRATO POR PROMPT
```text
[PROMPT ID — TÍTULO]
<mission>Objetivo único, resultado y motivo.</mission>
<epistemic_context>HECHOS/DECISIONES/PREF_FOUNDER/HIPOTESIS/PROPUESTAS/INFERENCIAS/CONFLICTOS/NO_ENCONTRADO relevantes, con source.</epistemic_context>
<sources>Paths, símbolos, memoria y referencias verificadas (existentes).</sources>
<inputs>Artefactos disponibles y su versión.</inputs>
<scope>Incluido · excluido · antiobjetivos.</scope>
<decision_rights>Autónomas · escalables · defaults.</decision_rights>
<constraints>Invariantes y prohibiciones (charter + entorno).</constraints>
<work>Pasos en orden. Inspecciona el repo real ANTES de actuar.</work>
<deliverables>Outputs exactos (sin "etc.").</deliverables>
<acceptance>Criterios observables y medibles.</acceptance>
<verification>Comandos/tests/inspecciones reales del repo, o "entrégalos al usuario si el IDE posee el daemon Gradle".</verification>
<failure_policy>Clasificación, reintentos, fallback, stop conditions.</failure_policy>
<handoff>Output exacto, consumidor y formato.</handoff>
```
Reglas: sin lenguaje vago; sin "asegura calidad" indefinido; sin doctrina global innecesaria; no repetir
discovery ya hecho; no mezclar implementación con review independiente; no pedir cadena de pensamiento
privada; no inventar resultados; no presentar recomendación como gate ejecutado; incluir versión del paquete.

## 11. Nodo (task) — YAML
Ver `EXECUTION_DAG_AND_VALIDATION_PROTOCOL.md §1` (campos completos).

## 12. Review prompts (Profesional+)
Primer pase sin autodefensa del constructor. `review` YAML (`QA_EVAL_AND_SATURATION_PROTOCOL.md §1`).

## 13. Integrador
Checklist de 13 puntos (`QA_EVAL_AND_SATURATION_PROTOCOL.md §2`). Consolida; no resume.

## 14. Gates + informe del validator
Gates reales del repo por nodo. Informe: `node scripts/validate-ceo-package.mjs <pkg>.json` con estado
real (`EJECUTADO Y VERDE`/`FALLIDO`). FAIL ⇒ no entregar.

## 15. Riesgos (tabla Profesional+)
`riesgo · probabilidad · impacto · detección · prevención · mitigación · owner`. Crítica añade pre-mortem.

## 16. Rollback (si aplica / Crítica)
Por nodo con write_set y riesgo alto.

## 17. Secuencia de ejecución
Qué primero, qué en paralelo, qué alimenta cada fase, qué condición ramifica, quién ejecuta (operador/`/mb`/agente). Crítica: stop conditions.

## 18. Checklist de finalización
Lista verificable de "realmente completado".

## 19. Estado resumible (Enterprise+)
`execution_state` YAML (`EXECUTION_DAG_AND_VALIDATION_PROTOCOL.md §5`).
