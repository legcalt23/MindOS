# EXER · Integración de la auditoría de Enfoque

**Run:** `TASK-2026-09-15-MOLVEX-EXER-PREPARACION` · **Alcance:** preparar instrucciones reutilizables e incorporar la auditoría, sin implementación del producto.

## 1. Autoridad y snapshot

El usuario adoptó explícitamente el ZIP como instrucciones en esta conversación. Gobiernan su encargo de preparación y los módulos de EXER; los prompts históricos de `contexto/fuentes/` siguen siendo historia, conforme a `sistema/01`. Se conservan las decisiones **1B/2B/3A/4A**. Ninguna propuesta de esta integración está aprobada por el mero hecho de estar escrita.

| Entrada | Observación | Estado |
|---|---|---|
| Repositorio accesible | `C:/Users/curro/AndroidStudioProjects/Molvex`, rama `product/focus2-v5`, HEAD `bdc252f496384e52e3ac14e451a139df57b47792`, con cambios previos | REPO_VERIFIED |
| Auditoría recibida | [AUDITORIA_TOTAL_ENFOQUE_MOLVEX_2026-09-14.md](../../audits/AUDITORIA_TOTAL_ENFOQUE_MOLVEX_2026-09-14.md), informe fechado/cerrado el 15 de septiembre sobre gates del 14 | AUDIT_RESULTS / SOURCE_OBSERVED |
| Conversación del ZIP | `contexto/fuentes/10_CONVERSACION_AUDITORIA_ENFOQUE.md` | AUDIT_PROMPT con decisiones explícitas, no resultados |
| Integridad y lectura | [MANIFIESTO_LECTURA.json](MANIFIESTO_LECTURA.json), hashes del ZIP y sus 45 miembros; lectores reales por archivo | Ver informe de verificación |
| Código y documentos vivos contrastados | [SNAPSHOT_FUENTES.json](SNAPSHOT_FUENTES.json), hashes por archivo y snapshot Git de esta preparación | REPO_VERIFIED para las lecturas señaladas |
| Pruebas de la app | El informe aporta 4468 tests desktop; esta preparación no los reejecutó | REPORTED, no TEST_VERIFIED nuevo |
| Datos humanos y ejecución móvil | No se produjo una corrida humana en esta tarea; el runbook registra emulador | NO VERIFICADO aquí |

El HEAD coincide con el del informe, pero eso no demuestra identidad del árbol sucio. Para cada capacidad utilizada se revisó fuente directa o se mantuvo `REPORTED`.

## 2. Reconciliación factual del paquete

Los 45 originales se conservan byte a byte. Las frases «no se han aportado repo/auditoría» de `00_LEEME.md`, `contexto/00` y `revision/01` describen el momento de creación del ZIP. Esta integración registra que ahora sí están disponibles. Sus revisiones y búsquedas bibliográficas previas no se atribuyen a esta sesión.

| Objeto | Tratamiento actual | Procedencia |
|---|---|---|
| Canon de producto/ciencia | Consultar el canon vivo por objeto y sus decisiones posteriores | [índice canónico](../../context_pack/00_CONTEXT_INDEX.md) y [Decision Log](../../context_pack/09_DECISION_LOG.md) |
| Archivos `contexto/fuentes` | Snapshots históricos íntegros; útiles para procedencia, no segunda autoridad editable | Manifiesto de lectura |
| Memoria Obsidian | La memoria documenta abrir la raíz del repo como vault; Markdown accesible. No se verificó la app Obsidian abierta ni existe `.obsidian` en esta raíz | [Memory Router](../../memory/00_SYSTEM/MEMORY_ROUTER.md), [índice](../../memory/00_MEMORY_INDEX.md) |
| Graphify | `GRAPH_REPORT.md` declara `8a06b9ac`; HEAD es `bdc252f…`. `.graphify_root` aún apunta a `MindOS`. El Python referenciado no existe y `graphify --help` falla con `uv trampoline failed to canonicalize script path` | REPO_VERIFIED; ejecución de consultas/refresh bloqueada por instalación local |
| Actualización de memoria | Esta vista y el enlace de navegación desde el índice registran los hechos nuevos; no se reescriben decisiones | Delta documental autorizado |
| Índice estructural | No actualizado ni editado a mano. Los documentos EXER no aportan cambios al código; queda pendiente revisar su exclusión del corpus de Graphify antes de un refresh | Limitación explícita, no grafo nuevo |

## 3. Handoff para diseño, actual frente a propuesto

| Área | Informe recibido / contraste local | Uso en EXER y dependencia |
|---|---|---|
| Constructos | PARTES IV–VI: taxonomía actual y alternativa CORE/SECONDARY/CROSS | La alternativa sigue PROPOSED; no sustituir canon ni adoptar sus números automáticamente |
| Catálogo y cobertura | PARTES VII–IX y XXXII: E1–E13, familias y redundancias; tres sondas basadas en tareas entrenadas | REPORTED desde auditoría; contrastar dossier/generador al diseñar cada familia. No retirar E12/E13 ni crear PRESSURE |
| Acceso al entrenamiento | Home exige `baseline != null`; una base denegada persiste `null`; default lifecycle `false`; iOS/Desktop devuelven `NoOpAppLifecycleProbe` | Contraste local; `MB-FOCUS-0166`. Un diseño viable necesita separar acceso a práctica y admisión de medida, pendiente del CEO |
| Adaptación | Call-site envía `fatigue`, `perAxisMastery`, `blocksSinceChange` a `null`. La rama de rendimiento retiene por ausencia; `ContainImpulsivity` se evalúa antes | Contraste local; `MB-FOCUS-0168`. C0 no significa que todas las ramas sean inertes |
| Base y personalización | Base construida desde una administración; el charter exige requisitos adicionales para pasar a calibrado | Contraste local; `MB-FOCUS-0174`, `0167`. Base inicial provisional y calibración son objetos distintos |
| Métricas/scoring | PARTES X–XI, XV–XVI: problemas de denominador, allow-list, SAT y cambio aparente | REPORTED; ninguna fórmula candidata ni threshold se incorpora como decisión aprobada |
| Tiempo y plataforma | PARTE XII: timing de focus v1 y comparabilidad limitados. NoOp iOS/Desktop confirmado en fuente | Diseñar contratos temporales; exigir evidencia por plataforma para su uso, sin asumir RT físico desde tests desktop |
| Dosis y fatiga | PARTES XIV, XX–XXI: señales incompletas y propuestas de dosis | Todo rango nuevo es propuesta/pregunta empírica; no adoptar ~80 % como óptimo universal |
| Exposición y transferencia | PARTE XIX/XXXII: formas reservadas y carencia de evaluación independiente suficiente | Separar entrenamiento, evaluación de desarrollo y confirmatoria; no convertir nuevas semillas en prueba de transferencia |
| Datos, accesibilidad y variantes | PARTES XXII–XXIV: carencias de modo/linaje, permisos y condiciones | Dossier debe precisar contratos. Los cambios de retención, datos o modalidad requieren decisiones concretas antes de adopción |
| Validación | PARTE XXVII; runbook físico separado | Las simulaciones, la prueba del sensor y los estudios de eficacia son evidencias distintas |
| Gobernanza V5 | `check:governance` reproduce `CEO_FIELD_DISAGREEMENT`; `MOUNT=BLOCKED` sigue declarado | `MB-FOCUS-0155`. Preparar EXER no concede autorización de ejecución ni de montaje V5 |

### Precisiones necesarias al consumir la auditoría

1. **D‑1 no equivale al charter tal como está escrita:** propone mediana de tres administraciones y costes en dos días, mientras §11.1 exige al menos cinco sesiones oficiales válidas, tres días y 48 horas, más cobertura/integridad/comparabilidad y ausencia de dependencia crítica de una tarea. Tres administraciones pueden estudiarse como estadístico auxiliar; no sustituyen el gate aprobado. Tampoco debe reutilizarse `EvidenceQuantityMaturity` como capacidad disponible sin inspección: la historia S6R3-001 documenta su retirada del contrato facts-only.
2. **C0 es una contención parcial:** `decide()` prioriza fatiga e impulsividad antes de la rama que retiene por señales ausentes. `ContainImpulsivity` llama a `stepUp` del eje; no resumir el motor como apagado ni afirmar que esa llamada es un `stepDown`. La interpretación de demanda depende del eje y la familia.
3. **Madurez no es eficacia:** el umbral 5/3/48 no valida el instrumento. Los objetivos numéricos de §51.6 y la rúbrica 0–100 son propuestas de la auditoría, no umbrales empíricos ratificados.
4. **Modalidad y finalidad siguen separadas:** D‑10 propone mezclar OFFICIAL/TRAINING/RELAXED en un enum; EXER exige distinguir finalidad, modalidad y plan comercial. Su adopción requiere resolver esa semántica, sin imponerla aquí.

Estas precisiones son observaciones de fuentes y alternativas de diseño; el informe original, el charter y sus decisiones permanecen conservados.

## 4. Impacto y siguiente trabajo

Esta preparación no crea ningún dossier de ejercicio. No hay dossiers producidos por EXER que invalidar. La revisión focalizada no evaluó todo dossier histórico del repositorio.

Se puede continuar con un **brief de Enfoque y comparación conceptual de familias**, manteniendo hipótesis, compatibilidad condicionada y dependencias de los cinco bloqueos. El catálogo definitivo, su implementación, la admisión oficial y la activación de flags requieren sus decisiones/evidencias respectivas.

La próxima ejecución lee este handoff, el checkpoint y [el paquete de decisiones](DECISIONES_CEO.md), compara hashes de las dependencias relevantes y reabre solo las conclusiones afectadas.
