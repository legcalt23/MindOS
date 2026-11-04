# MindBoost — Decision Log & ADR Backlog

**Versión:** 2.0.2  
**Fecha:** 2026-07-12 (doc-pass 6R3-W3; base 2.0.0 = 2026-07-10)  
**Owner:** CEO de MindBoost  
**Estado:** APPROVED  
**Regla:** las decisiones aquí registradas sustituyen supuestos anteriores cuando se marca explícitamente

---

## 1. Propósito

Este archivo registra las decisiones cerradas que una IA no debe volver a preguntar, los requisitos que sustituyen al PRD anterior y las decisiones técnicas que aún necesitan ADR.

No almacena cada conversación. Almacena el resultado vigente.

---

## 2. Estados

| Estado | Significado |
|---|---|
| `APPROVED` | Decisión ejecutiva vigente. |
| `POLICY` | Regla operativa obligatoria. |
| `HYPOTHESIS` | Objetivo o mecanismo por validar. |
| `PROVISIONAL` | Decisión temporal con gate de sustitución. |
| `DEFERRED_ADR` | La dirección está acotada, falta diseño técnico. |
| `SUPERSEDED` | Ya no vigente; se conserva por trazabilidad. |
| `PROHIBITED` | No implementar salvo nueva decisión explícita. |

---

## 3. Visión y posicionamiento

| ID | Decisión | Estado |
|---|---|---|
| VIS-001 | MindBoost aspira a ser el entrenador cognitivo de referencia mundial, centrado en progreso cognitivo útil y medible. | `APPROVED` |
| VIS-002 | La ambición no autoriza claims, scores o rangos por encima de la evidencia. | `POLICY` |
| VIS-003 | Producto standalone para iOS y Android. | `APPROVED` |
| VIS-004 | Mercado inicial: España; idioma: español de España. | `APPROVED` |
| VIS-005 | Público inicial: adultos 18+, cualquier nivel económico y sin experiencia previa requerida. | `APPROVED` |
| VIS-006 | Personas prioritarias: estudiantes ambiciosos y emprendedores; también cualquier adulto motivado por entrenar su mente. | `APPROVED` |
| VIS-007 | Problema cultural: la vida moderna reduce oportunidades de esfuerzo cognitivo deliberado; MindBoost ofrece una rutina estructurada, no discurso alarmista. | `APPROVED` |
| VIS-008 | MVP actual descrito por CEO como parcial, poco refinado pero bastante sólido. | `APPROVED` como evaluación; validar repo. |
| VIS-009 | Dirección estética: híbrido laboratorio + cósmico + cinematográfico contenido. | `APPROVED` |

---

## 4. Jerarquía de outcomes

| ID | Decisión | Estado |
|---|---|---|
| OUT-001 | Prioridad 1: transferencia a estudiar, trabajar, decidir o concentrarse. | `APPROVED` |
| OUT-002 | Prioridad 2: transferencia a ejercicios distintos de la misma capacidad. | `APPROVED` |
| OUT-003 | Prioridad 3: conocimiento del perfil cognitivo propio. | `APPROVED` |
| OUT-004 | Prioridad 4: motivación, progreso percibido y adherencia. | `APPROVED` |
| OUT-005 | Prioridad 5: hábito estable. | `APPROVED` |
| OUT-006 | Prioridad 6: mejora en ejercicios entrenados con estímulos nuevos. | `APPROVED` |
| OUT-007 | Tras 30 días el usuario debería notar al menos una mejora leve cotidiana. Es objetivo de producto, no claim validado. | `HYPOTHESIS` |
| OUT-008 | North Star basada en transferencia cotidiana válida a 30 días, acompañada por guardrails. | `APPROVED` |
| OUT-009 | Seguimiento de durabilidad a 90 días. | `APPROVED` |

---

## 5. Motores y catálogo

| ID | Decisión | Estado |
|---|---|---|
| CAT-001 | Se mantienen ocho motores visibles: Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental y Memoria. | `APPROVED` |
| CAT-002 | Los motores visibles no implican ocho factores independientes; internamente se usan subhabilidades científicas. | `POLICY` |
| CAT-003 | Lanzamiento con al menos 15 ejercicios completos por motor: mínimo 120. | `APPROVED` |
| CAT-004 | Puede haber más de 120 si añaden valor real sin reducir calidad. | `APPROVED` |
| CAT-005 | Todos los ejercicios objetivo deben estar técnicamente completos y disponibles en el primer lanzamiento, con estados de validación honestos. | `APPROVED` |
| CAT-006 | Un ejercicio entrenable no se presenta como medición oficial hasta superar su gate. | `POLICY` |
| CAT-007 | Todos los motores tienen ruta completa sin permisos sensibles. | `APPROVED` |
| CAT-008 | Sensores/permisos se eligen por ejercicio, con minimización, just-in-time y alternativa. | `APPROVED` |
| CAT-009 | Accesibilidad es obligatoria y parte de la validez. | `POLICY` |
| CAT-010 | Free usa generación procedural determinista para training y bancos/generación calibrada para measurement. | `APPROVED` |
| CAT-011 | Ningún ejercicio Free depende de IA. | `PROHIBITED` para IA en Free |
| CAT-012 | Un cambio puramente estético no crea una nueva familia científica. | `POLICY` |

---

## 6. Entrenamiento, evaluación y dosis

| ID | Decisión | Estado |
|---|---|---|
| SES-001 | Separar carril de entrenamiento y carril de medición. | `APPROVED` |
| SES-002 | Evaluación mediante prueba explícita + probes embebidos controlados. | `APPROVED` |
| SES-003 | Evaluación inicial en dos fases: rápida provisional + calibración progresiva. | `APPROVED` |
| SES-004 | Plan disponible inmediatamente y refinado con evidencia. | `APPROVED` |
| SES-005 | Reevaluación estructurada mensual y probes continuos. | `APPROVED` |
| SES-006 | Duraciones de sesión: 2, 5, 10, 20 y 30 minutos. | `APPROVED` |
| SES-007 | Sesión corta puede centrarse en un motor; sesión larga puede intercalar. | `APPROVED` |
| SES-008 | El usuario puede entrenar más de la dosis oficial; el exceso pasa a training-only para evidencia. | `APPROVED` |
| SES-009 | La dosis oficial exacta se define por familia mediante ADR/datos, no una regla universal arbitraria. | `APPROVED` |
| SES-010 | Feedback de training Free: determinista, directo, exigente, constructivo y suficientemente detallado. | `APPROVED` |
| SES-011 | Feedback de measurement: agregado para proteger integridad y reducir aprendizaje de ítems. | `APPROVED` |

---

## 7. Personalización y planes profesionales

| ID | Decisión | Estado |
|---|---|---|
| PER-001 | Inputs: profesión/estudios, tareas, objetivo, evaluación, fortalezas/debilidades, tiempo, horario, historial, fatiga, sueño/estrés/energía, preferencias/rechazos y adherencia. | `APPROVED` |
| PER-002 | Macroplan se adapta semanalmente. | `APPROVED` |
| PER-003 | Microdificultad se adapta durante sesión. | `APPROVED` |
| PER-004 | Planes de lanzamiento: general, estudiante y emprendedor. | `APPROVED` |
| PER-005 | Arquitectura debe poder mapear cualquier profesión futura mediante tareas concretas. | `APPROVED` |
| PER-006 | Una profesión desconocida usa fallback general + tareas declaradas. | `APPROVED` |
| PER-007 | No estereotipar capacidad por título profesional. | `POLICY` |
| PER-008 | Rechazar un ejercicio ofrece sustituto cognitivo equivalente. | `APPROVED` |
| PER-009 | Free usa planes preconstruidos/deterministas que se sienten personalizados por reglas. | `APPROVED` |
| PER-010 | Elite puede reorganizar componentes validados con IA; usuario confirma cambios. | `APPROVED` |

---

## 8. Scoring, progreso, calibración y ranking

| ID | Decisión | Estado |
|---|---|---|
| SCO-001 | Performance Score y Growth Index son objetos separados. | `APPROVED` |
| SCO-002 | Performance Score puede bajar honestamente. | `APPROVED` |
| SCO-003 | XP, racha, volumen y suscripción nunca alteran score cognitivo. | `PROHIBITED` |
| SCO-004 | No usar missing=0. | `PROHIBITED` |
| SCO-005 | No usar la última sesión cruda como perfil. | `PROHIBITED` |
| SCO-006 | Toda estimación controla dificultad o rechaza comparabilidad. | `POLICY` |
| SCO-007 | Mostrar baseline, actual, mejor comparable y cambio fiable sin ocultar bajadas. | `APPROVED` |
| SCO-008 | Score visible puede usar 0–100, pero la escala/modelo subyacente queda versionada. | `APPROVED` |
| SCO-009 | Acompañar score con cobertura, calidad/confianza, frescura y versión. | `APPROVED` |
| SCO-010 | Inactividad marca `STALE`; no provoca descenso arbitrario. | `APPROVED` |
| SCO-011 | Tendencia debe incorporar error y suficiente evidencia. | `POLICY` |
| SCO-012 | Mínimo inicial de calibración: 5 sesiones oficiales válidas. | `APPROVED` |
| SCO-013 | Además: 3 días distintos y al menos 48 h entre primera/última; más evidencia si hace falta. | `APPROVED` |
| SCO-014 | Perfil/global provisional puede existir con cobertura explícita. | `APPROVED` |
| SCO-015 | Global oficial solo con los ocho motores calibrados y comparables. | `APPROVED` |
| SCO-016 | Ranking solo usa rendimiento calibrado e íntegro. | `APPROVED` |
| SCO-017 | No hay ranking público oficial durante beta. | `APPROVED` |
| SCO-018 | Rangos en alpha/beta son provisionales. | `APPROVED` |
| SCO-019 | `World Class` bloqueado hasta normas representativas e invariancia suficiente. | `APPROVED` |
| SCO-020 | Creatividad y Emociones pueden permanecer sin ranking si su medición no alcanza el estándar. | `APPROVED` |
| SCO-021 | Perfiles privados por defecto; compartir es explícito, granular y revocable. | `APPROVED` |
| SCO-022 | Dashboard con visualización progresiva: acción/resumen primero, subhabilidades después y metodología inspeccionable. | `APPROVED` |

---

## 9. Contexto subjetivo

| ID | Decisión | Estado |
|---|---|---|
| CTX-001 | Sueño, estrés, energía y fatiga se guardan localmente por defecto. | `APPROVED` |
| CTX-002 | Pueden influir en plan, dificultad, confianza o interpretación. | `APPROVED` |
| CTX-003 | No influyen en Performance Score oficial. | `APPROVED` |
| CTX-004 | Un score contextual ajustado solo puede existir como experimento privado hasta validarse. | `APPROVED` |
| CTX-005 | No enviar contexto a analytics, cloud o IA sin finalidad/consentimiento separados. | `POLICY` |

---

## 10. Motivación, rachas y comportamiento

| ID | Decisión | Estado |
|---|---|---|
| MOT-001 | Racha basada en días de entrenamiento elegidos, no calendario rígido. | `APPROVED` |
| MOT-002 | Descansos planificados no rompen racha. | `APPROVED` |
| MOT-003 | Margen máximo aprobado: hasta 2 días de fallo por semana según plan. | `APPROVED` |
| MOT-004 | XP sirve para motivación, nunca capacidad. | `APPROVED` |
| MOT-005 | Notificaciones opt-in, útiles y no manipulativas. | `APPROVED` |
| MOT-006 | Sin culpa, amenazas de pérdida o dark patterns. | `POLICY` |
| MOT-007 | Dashboard prioriza próxima acción y contexto de progreso sobre decoración. | `APPROVED` |

---

## 11. Modelo Free y Elite

| ID | Decisión | Estado |
|---|---|---|
| BUS-001 | Free debe ser un entrenador plenamente efectivo; Elite no compra capacidad estimada. | `APPROVED` |
| BUS-002 | Free no usa IA generativa. | `PROHIBITED` |
| BUS-003 | Elite incluye análisis/dirección con IA. | `APPROVED` |
| BUS-004 | Elite disponible durante beta abierta. | `APPROVED` |
| BUS-005 | Precio inicial: 2 €/mes, ajustable con evidencia y economía. | `APPROVED` |
| BUS-006 | No habrá trial gratuito de IA. | `APPROVED` |
| BUS-007 | API externa para Elite. | `APPROVED` |
| BUS-008 | Cuota dinámica y circuit breaker, no límite fijo ciego. | `APPROVED` |
| BUS-009 | Cada uso de IA debe mantener unit economics positivos. | `POLICY` |
| BUS-010 | Presupuesto adicional de desarrollo actual: 0 €. | `APPROVED` |
| BUS-011 | Toda opción pagada recomendada debe tener alternativa 0 € salvo infraestructura de lanzamiento indispensable aprobada aparte. | `POLICY` |
| BUS-012 | Sin anuncios. | `APPROVED` |

---

## 12. AI Coach y memoria

| ID | Decisión | Estado |
|---|---|---|
| AIC-001 | IA solo para suscriptores Elite. | `APPROVED` |
| AIC-002 | Solo reorganiza/explica componentes validados; no inventa score, ejercicio oficial ni tratamiento. | `POLICY` |
| AIC-003 | Cambios de plan requieren confirmación del usuario. | `APPROVED` |
| AIC-004 | Memoria estructurada, visible, editable y borrable. | `APPROVED` |
| AIC-005 | Enviar mínimo dato y validar output contra schema/política. | `POLICY` |
| AIC-006 | Fallback determinista ante fallo, cuota o rechazo. | `APPROVED` |
| AIC-007 | Modelo/proveedor/prompt versionados. | `POLICY` |
| AIC-008 | La IA no puede modificar directamente score/rango/evidencia oficial. | `PROHIBITED` |
| AIC-009 | Proveedor/modelo se abstraen detrás de un contrato y todo cambio crítico pasa benchmark/eval de calidad, safety, schema y coste. | `APPROVED` |

---

## 13. Cuenta e identidad

| ID | Decisión | Estado |
|---|---|---|
| IDN-001 | Cuenta de lanzamiento con username + password; email no obligatorio. | `APPROVED` |
| IDN-002 | Email o teléfono vinculables voluntariamente. | `APPROVED` |
| IDN-003 | Auth de lanzamiento en VPS propio. | `APPROVED` |
| IDN-004 | `loginUsername` privado, estable e inmutable; `displayName` separado y editable. | `APPROVED` |
| IDN-005 | Recuperación: recovery codes y enlace/PIN a canal previamente verificado; sin esos factores no existe bypass de soporte y la cuenta puede ser irrecuperable. | `APPROVED` |
| IDN-006 | Passkeys son evolución futura recomendada. | `DEFERRED_ADR` |
| IDN-007 | No usar preguntas personales/de conocimiento como recuperación. | `PROHIBITED` |
| IDN-008 | Durante desarrollo se permite Firebase Auth con email sintético solo bajo flag/entorno. | `PROVISIONAL` |
| IDN-009 | Email sintético/Firebase temporal bloquea open beta; requiere kill-switch y migración. | `POLICY` |
| IDN-010 | Detalles del VPS no se diseñan en esta fase; requieren ADR antes de implementar. | `DEFERRED_ADR` |

---

## 14. Privacidad y control del usuario

| ID | Decisión | Estado |
|---|---|---|
| PRI-001 | Distinguir tratamiento necesario de analytics, investigación e IA opcionales. | `APPROVED` |
| PRI-002 | Consentimientos por finalidad, revocables donde corresponda. | `APPROVED` |
| PRI-003 | Pseudonimizado sigue siendo dato personal; solo anonimización irreversible sale de alcance personal. | `POLICY` |
| PRI-004 | Usuario puede ver, exportar, corregir y borrar sus datos. | `APPROVED` |
| PRI-005 | Borrado cubre local/offline, cloud, colas, analytics, IA/memoria, backups y datos anonimizados con explicación. | `APPROVED` |
| PRI-006 | Objetivo inicial de borrado: local inmediato, sistemas activos ≤30 días y backups ≤90 días; validar excepciones legales/técnicas antes de publicación. | `APPROVED` |
| PRI-007 | Región UE y cifrado cuando sea viable; proveedores/transferencias documentados. | `APPROVED` |
| PRI-008 | Datos cognitivos privados por defecto y minimizados. | `APPROVED` |
| PRI-009 | Analytics siempre se usa para mejorar el sistema, pero la ruta opcional requiere elección separada. | `APPROVED` |
| PRI-010 | No acceso cruzado entre usuarios. | `POLICY` |

---

## 15. Tecnología y plataforma

| ID | Decisión | Estado |
|---|---|---|
| TEC-001 | Kotlin Multiplatform + Compose Multiplatform. | `APPROVED` |
| TEC-002 | Android 8/API26 e iOS15 como mínimos. | `APPROVED` |
| TEC-003 | Paridad funcional Android/iOS. | `APPROVED` |
| TEC-004 | Free funciona offline. | `APPROVED` |
| TEC-005 | Sync idempotente; resultados offline provisionales hasta validación. | `APPROVED` |
| TEC-006 | Controlar latencia/clase de dispositivo para métricas temporales. | `APPROVED` |
| TEC-007 | Lógica cognitiva pura en `commonMain` cuando sea posible. | `APPROVED` |
| TEC-008 | Firestore es persistencia cloud próxima, no cuello de botella ni única fuente de funcionamiento. | `APPROVED` |
| TEC-009 | Anti-cheat proporcional; protege score/ranking sin acusación automática. | `APPROVED` |
| TEC-010 | Fórmulas, schemas, generadores, bancos, modelos y normas versionados. | `POLICY` |
| TEC-011 | Cambios de scoring requieren dual/shadow scoring o migración defendible. | `POLICY` |

---

## 16. Lanzamiento y validación

| ID | Decisión | Estado |
|---|---|---|
| LCH-001 | Primero alpha privada con aproximadamente 20 adultos. | `APPROVED` |
| LCH-002 | Después beta abierta en App Store y Google Play. | `APPROVED` |
| LCH-003 | Reclutamiento por Discord (~240), universidades, institutos y centros de enseñanza; solo adultos. | `APPROVED` |
| LCH-004 | Alpha valida QA/usabilidad, no psicometría completa ni eficacia causal. | `POLICY` |
| LCH-005 | Tamaños de beta/estudios definidos por potencia/precisión. | `APPROVED` |
| LCH-006 | Experimentos confirmatorios preregistrados. | `APPROVED` |
| LCH-007 | Release estable determinado por gates, no por fecha. | `APPROVED` |
| LCH-008 | No claims fuertes durante beta sin evidencia específica. | `POLICY` |
| LCH-009 | World Class/normas requieren muestra representativa, no beta autoseleccionada. | `POLICY` |
| LCH-010 | Todas las métricas disponibles se usarán para mejorar el sistema solo bajo finalidad/consentimiento y análisis válidos. | `APPROVED` |

---

## 17. Gobierno de desarrollo con IA

| ID | Decisión | Estado |
|---|---|---|
| GOV-001 | CEO es único humano y usa IA como equipos especializados. | `APPROVED` |
| GOV-002 | Claude tiene acceso completo al repositorio, terminal, tests y git. | `APPROVED` |
| GOV-003 | Siempre crear plan maestro antes de implementar. | `POLICY` |
| GOV-004 | `/mb` es el orquestador de la sesión principal; no es agente y solo se invoca explícitamente. | `APPROVED` |
| GOV-005 | Subagentes no lanzan subagentes; el lead hace routing. | `POLICY` |
| GOV-006 | Trabajar en slices verticales/tracer bullets. | `POLICY` |
| GOV-007 | Gate de salida con efficacy + diff; psychometrician/privacy/architecture según cambio. | `POLICY` |
| GOV-008 | ChatGPT revisa después de cada fase usando resumen y archivos modificados. | `APPROVED` |
| GOV-009 | ChatGPT debe detectar incluso mejoras pequeñas y producir prompt autocontenido para Claude. | `APPROVED` |
| GOV-010 | Agentes `mindboost-*` y `mindverse-*` se nombran explícitamente; no se auto-invocan con `/mb`. | `APPROVED` |
| GOV-011 | Paralelizar solo tareas independientes; considerar coste 4–7×. | `POLICY` |
| GOV-012 | ChatGPT puede decidir autónomamente con confianza muy alta y alineación, registrando evidencia/migración. | `APPROVED` |
| GOV-013 | Deploy, gasto, borrado real, contacto externo y publicación requieren autorización separada. | `POLICY` |
| GOV-014 | Investigar en las mejores fuentes aplicables; técnica en fuentes primarias, ciencia de calidad y normas oficiales. | `POLICY` |
| GOV-015 | 10/10: sin P0/P1 y sin mejora segura, relevante e in-scope pendiente. | `POLICY` |
| GOV-016 | Follow-up fuera de alcance necesita owner/gate; si amenaza wiring debe existir barrera fail-closed. | `POLICY` |

---

## 18. Decisiones sustituidas

| ID anterior/tema | Antes | Ahora | Estado |
|---|---|---|---|
| PRD1-CAT | MVP de ~38 ejercicios | ≥120, al menos 15 por motor | `SUPERSEDED` por CAT-003 |
| PRD1-AUTH | Firebase Auth recomendado para MVP | Firebase sintético solo DEV; VPS propio antes de open beta | `SUPERSEDED` por IDN-003/008/009 |
| Context-score | Sueño/estrés/fatiga podían influir directamente en score | Solo plan/dificultad/confianza; no score oficial | `SUPERSEDED` por CTX-003 |
| Recovery-question | Pregunta personal como tercera vía | Prohibida; recovery codes + canal verificado | `SUPERSEDED` por IDN-005/007 |
| Calibration-count | “5 sesiones” sin dispersión temporal suficiente | 5 sesiones, 3 días y ≥48 h, adaptativo | `SUPERSEDED` por SCO-012/013 |
| Latest-score | Última puntuación como contribución viva | Estimador robusto; latest raw prohibido | `SUPERSEDED` por SCO-005 |
| CV-confidence | CV como confianza final | Requiere error/estabilidad apropiado a escala y evidencia | `SUPERSEDED` como modelo final |
| Public-ranking-beta | Ranking competitivo temprano | No ranking público oficial en beta | `SUPERSEDED` por SCO-017 |
| Global-with-gaps | Global aparente con bloques sin medir | Solo provisional con cobertura; oficial con ocho | `SUPERSEDED` por SCO-014/015 |
| Generic-consent | Consentimiento general para todas las métricas | Finalidades y controles separados | `SUPERSEDED` por PRI-001/002 |

---

## 19. ADR backlog obligatorio

Estas decisiones no se preguntan de nuevo al CEO salvo que cambien visión o requieran gasto/acción externa. Claude/ChatGPT deben resolverlas con evidencia y registrarlas.

| ADR | Pregunta | Owner/agentes | Gate/fecha lógica |
|---|---|---|---|
| ADR-SCI-001 | Modelo/subhabilidades y agregación por cada motor. | psychometrician + assessment-psychometrics | Antes de score oficial del motor. |
| ADR-SCI-002 | Fórmula velocidad–precisión por familia. | psychometrician + cognitive-science | Antes de measurement provisional. |
| ADR-SCI-003 | Estimador longitudinal robusto e incertidumbre. | psychometrician + diagnostics | Antes de cablear ingestor a perfil vivo. |
| ADR-SCI-004 | Trend y reliable change con práctica/error. | psychometrician + assessment-psychometrics | Antes de Growth Index oficial. |
| ADR-SCI-005 | Límite de evidencia oficial diaria/exposición. | efficacy + experimentation | Antes de beta. |
| ADR-SCI-006 | Modelo global y pesos/correlaciones. | chief-architect + psychometrics | Antes de global oficial. |
| ADR-SCI-007 | Normas/rangos y requisito World Class. | psychometrics + research | Antes de ranking oficial. |
| ADR-SCI-008 | Puentes por dispositivo/modalidad/accesibilidad. | psychometrician + mobile-performance | Antes de mezclar resultados. |
| ADR-SCI-009 | Protocolo VEPR-30 y outcome cotidiano. | transfer + experimentation | Antes de recogida confirmatoria. |
| ADR-DATA-001 | Event schema, local store y sync state machine. | data-sync + data-engineer | Antes de open beta. |
| ADR-DATA-002 | Retención/borrado exacto, backups, analytics e IA. | privacy + legal | Antes de alpha con datos reales. |
| ADR-ID-001 | Arquitectura VPS auth, hashing, sesiones, rate limits y operación. | backend + security + legal | Antes de implementar auth release. |
| ADR-ID-002 | Migración Firebase synthetic → VPS IDs. | identity/backend/data | Antes de open beta. |
| ADR-ID-003 | Passkeys y recuperación de cuenta futura. | security/backend | Post-beta o si se prioriza. |
| ADR-AI-001 | Proveedor/modelo y contrato Elite. | ai-platform + unit-economics | Antes de activar Elite. |
| ADR-AI-002 | Cuota dinámica y circuit breaker. | backend + finance | Antes de cobro real. |
| ADR-AI-003 | Schema de memoria, minimización y retención. | ai-coach + privacy | Antes de memoria Elite. |
| ADR-PER-001 | Taxonomía profesión → tareas → subhabilidades. | personalization + UX | Antes de ampliar profesiones. |
| ADR-EXP-001 | Potencia, análisis y preregistro beta. | experimentation | Antes de reclutar estudio. |
| ADR-REL-001 | SLOs y gates cuantitativos alpha/beta/stable. | qa/devops/performance | Antes de cada release. |

---

## 20. Decisiones de Slice 6 pendientes de integración

Fuente: cierre aportado `SYSTEM1_SLICE_6_INGESTOR_CONTRACT.md`.

| ID | Estado |
|---|---|
| S6-001 Ingestor puro/no cableado | `CLAIMED_BY_CLAUDE`; reproducir en repo. |
| S6-002 `null` como ausencia | Alineado; obligatorio al cablear. |
| S6-003 `preliminary=true` hasta validación física | Alineado. |
| S6-004 5 sesiones + 3 días | Parcial: añadir/verificar ≥48 h. |
| S6-005 latest normalized score | Bloqueante antes de wiring; ADR-SCI-003. |
| S6-006 confidence vía CV | Bloqueante como modelo final; ADR-SCI-003. |
| S6-007 trend first-last | Bloqueante antes de Growth oficial; ADR-SCI-004. |
| S6-008 bucket UTC | Sustituir por política temporal aprobada. |
| S6-009 difficulty invariance upstream | Debe demostrarse antes de mezclar sesiones. |

No activar E9/perfil vivo/ranking basándose solo en el cierre.

---

## 20.1 Decisiones de Slice 6-R3 (registro, 2026-07-11)

Fuente: `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R3_REOPEN.md`. El CEO reabrió 6-R2 (verdicto
independiente = REOPEN). Registro de las decisiones cerradas y de los hallazgos pendientes de disposición.
Ninguna de estas entradas autoriza wiring a perfil vivo / Global Score / Growth Index / rango / UI / Firestore /
SyncQueue / analytics / cloud.

| ID | Decisión / hallazgo | Estado |
|---|---|---|
| S6R3-001 | **Falsación NUMÉRICA de la barrera "no cableable" de 6-R2 + rediseño facts-only.** El output evidence-only de 6-R2 seguía siendo reconstruible en un perfil SIN tocar ningún tipo `profile.*` (Performance-Score proxy 0.620; confianza 1−CV 0.907; mejor comparable 0.710; trend +0.160; suficiencia 1.000; calibración CALIBRATED; frescura FRESH; rank 1.000). Corrección facts-only: se ELIMINAN `orderedNormalizedScores`, `cappedObservationCount`, `observationCountProgress`/`distinctDayProgress`, `EvidenceQuantityMaturity`, `FreshnessStatus` y los conteos `RankReadiness.{rankSessionCount,distinctDays,spanMs,acceleratingSessionCount}`. Se conservan solo hechos crudos (identidad versionada + `PolicyVersionBundle` §21, conteos enteros, timestamps + `ClockProvenance`, exclusiones, estados). Enum de comparabilidad REEMPLAZADO por sealed `ComparabilityNotEstablished`(default) / `Incompatible` / `Bridged(version)` (esta última inalcanzable/fail-closed); comparabilidad no establecida ⇒ prohibido promediar/dispersar (enforce por AUSENCIA del input). RT-criticidad: se sustituye el hardcode `{SPEED}` por metadata versionada `RtCriticalityRegistry`; metadata ausente ⇒ UNKNOWN ⇒ NOT_OFFICIAL/fail-closed. `TRAINING_FREE` completo ⇒ `TRAINING_ONLY` (fail-closed). | `APPROVED` (contrato); wiring `BLOCKED` |
| S6R3-002 | **Fix P0 6R3-P0C-01 `CognitiveSessionRunStore`.** El store no tenía concepto de sujeto ⇒ commingling cross-account + wipe colateral en borrado de cuenta. Corregido con `subject` obligatorio en construcción del store seguro + claves scopeadas por sujeto (`crun:<subject>:…`, índices `_recent`/`_block`) + guard `require(':' !in subject)` + exclusión del store del `clearNamespace()` masivo del eraser (borrado por `deleteAll()` scopeado). Conservación probada (sign-out ≠ borrado de cuenta; nunca clear-on-sign-in). Revisado por `privacy-security` = **APTO**. | `APPROVED` |
| S6R3-003 | **HALLAZGO NUEVO (CONFIRMADO) · P1 · `SessionRecordStore`/`SecureSessionRecordStore` (`mindboost_sessions`).** Mismo defecto estructural (sin sujeto en la clave física `session_record:$sessionId`), a lo largo de las 8 DIs de módulo (Focus/Memory/Speed/Logic/Creativity/Emotions/Decisiones + hub). Peor que una lectura pasiva: `SessionRecoveryScanner.recover()` **MUTA** filas STARTED/CHECKPOINT de cuentas ajenas → ABANDONED en cada track-mount (escritura cross-account viva); además `Sessions` sigue en el eraser masivo (wipe colateral). Severidad **P1**, no P0: el payload es solo metadata de ciclo de vida (status/timestamps/conteos), sin score ni contenido cognitivo. NO bloquea el entregable P0/evidence de este slice (regla explícita de privacy-security). También: `MindBoostStorageNamespaces.kt:22-24` llama a `Sessions` "Per-user" en su KDoc, lo cual es INEXACTO; corregir ese KDoc cuando se arregle el P1. **Disposición: PENDIENTE decisión CEO** — privacy-security recomienda su PROPIO slice, owner `data-sync-dev`, con el mismo patrón probado en S6R3-002. **ACTUALIZACIÓN 2026-07-12 (W3):** el CEO decidió PLEGARLO a este programa (reopen §14); el fix aterrizó siguiendo el patrón probado de S6R3-002 (reopen §16) y `privacy-security` lo revisó **APTO con reservas** (§17). | `APPROVED` (folded a este programa; fix landed §16, APTO §17) |
| S6R3-004 | **Posiciones ADR interinas.** `ADR-SCI-003` (estimador), `-004` (trend), `-005` (dosis oficial diaria/exposición), `-006` (modelo global/pesos), `-007` (normas/rangos) y `-008` (puentes dispositivo/modalidad) permanecen **ABIERTOS** (§19). El contrato evidence-only se mantiene **estrictamente por debajo** de ellos: emite hechos, no estimaciones/tendencias/bandas/frescura/rank; nada se cablea a perfil vivo, rango, Growth Index, global ni cloud hasta cerrarlos. | `POLICY` |
| S6R3-005 | **Dos preguntas de diseño ABIERTAS → RESUELTAS en W4 (2026-07-12, review fresh-context).** (1) `SessionEvidenceState`/`SessionEvidenceStatePolicy`: el estado canónico SÍ se surfacea en el audit vía `stateCounts`/`routeEligibilityCounts` (conteo por estado y por elegibilidad), y desde W4 `SessionEvidenceStatePolicy.dispositionOf` es la ÚNICA autoridad por-sesión que PRODUCE esos conteos en producción (`CognitiveSessionEvidenceExtractor.evidenceCountsOf`) — máquina de estados INTEGRADA, no desconectada (C2.1). No se emite una lista per-sesión de dispositions (minimización facts-only). (2) `ComparabilityVerdict.Incompatible` SÍ se construye en producción (`CognitiveSessionEvidenceExtractor.kt:75`, ≥2 grupos) desde el fix de recuperación del crash — el claim "nunca se construye" quedó OBSOLETO (C2.2). | `APPROVED` (resuelto W4 §27) |
| S6R3-006 | **Corrección de citación en `03 §29`.** Mis-atribuía PMID `35953659` a Passell. Corregido (verificado en PubMed): `35953659` = Nicosia (latencia BYOD, 2022), `36062528` = Nicosia (fiabilidad), `33954913` = Passell (elección de dispositivo). El código `MeasurementLineage.kt` ya era correcto en los PMIDs (matiz: `35953659` es 2022, no 2023). Retirado el claim 6-R2 de "citation audit clean". | `APPROVED` (corrección documental) |
| S6R3-007 | **Purga local de claves legacy sin sujeto (C1) — LANDED (W1A), APTO privacy-security (§21).** Migración local, versionada, idempotente y fail-closed que **BORRA** las claves/índices legacy sin sujeto inequívoco en AMBOS namespaces (`mindboost_sessions` + `mindboost_session_runs`). Formas legacy (regex anclada): `^session_record:[^:]+$`, `^crun:[^:]+$`, `^crun_idx_recent$`, `^crun_idx_block:[^:]+$` (uno/cero colons = legacy; una clave subject-scoped lleva SIEMPRE ≥1 colon extra ⇒ garantía ESTRUCTURAL, jamás decodifica payload). Mecanismo `LegacyNamespacePurge.ensurePurged(store)` = 1ª sentencia de todo reader (`find/all/delete/findEvidence/deleteAll/reconcile*`), corre ANTES de `SessionRecoveryScanner.recover()` y de `reconcileIndices()` por construcción; `upsert`/`save` NO gateados (clave exacta por-sujeto, nunca prefix-scan). Marker `legacy_purge_marker:v1` (valor = solo el int de versión, no colisiona con checks `rawBlobs.none{contains}`) escrito SOLO tras completar la purga; crash ⇒ marker ausente ⇒ reintento total; idempotente (`remove` es no-op); mutex + short-circuit para concurrencia. NUNCA corre en login/logout; nunca toca claves subject-scoped ni datos de otras cuentas; el marker sobrevive al `deleteAll()` del sujeto (es infraestructura de dispositivo). Evidencia que justifica la purga conservadora en ambos ns: runs-store `??` untracked (nunca commiteado), repo con cero git tags, `docs/release/RELEASE_EVIDENCE.json`=NO_GO (2026-06-19), versionCode=1 ⇒ sin evidencia de que llegara a usuarios reales (dev-installs no excluibles ⇒ posture conservadora en ambos). Revisado **APTO + sign-off de migración destructiva** por `privacy-security` (§21). Irreversible por diseño (no existe atribución que restaurar). Cross-ref `docs/mindboost_velocidad_implementation_plan/LEGACY_KEY_MIGRATION.md`. | `APPROVED` (migración cerrada; ship sujeto a gates §13 verdes; iOS NOT VERIFIED en Windows) |
| S6R3-008 | **Perfil vivo #1/#2 (C2.4) — ADR-BLOCKED, siguen `preliminary` (NO resueltos).** Las contribuciones vivas de Focus y Logic al brain-profile quedan fijadas `preliminary = true` **HARDCODEADO (no derivado)** en `FocusProfileSource.kt:58` y `LogicProfileSource.kt:49` porque: (#1) ninguno tiene gate de propósito de sesión que distinga TRAINING_FREE de evaluación, y (#2) no existe modelo de equating ni de compatibilidad de `scoringVersion`. Son **ADR-blocked**: esperan las decisiones científicas de los ADR ABIERTOS de estimador/equating — `ADR-SCI-003` (estimador longitudinal) y `ADR-SCI-008` (puentes dispositivo/modalidad ≈ equating), ambos ABIERTOS en §19. Barrera mecánica HOY = el pin hardcodeado + un test invariante (`Focus/LogicProfileSourceTest`); el pin es lo único que impide que se afirmen como oficiales. **NO están resueltos**: siguen preliminares hasta cerrar esos ADR. (Contexto: los otros cuatro P1 de C2.4 SÍ son fixes reales ya aterrizados — #3 dedup `.distinctBy` Focus:48/Logic:39, #4 exclusión de futuro `endedAtMs<=nowMs` en `ProfileSessionEligibility.kt:30`, #5 rename `ProvisionalStable/ProvisionalEmerging`, #6 = C2.5/S6R3-009; ver reopen §23.) | `DEFERRED_ADR` (barrera = pin `preliminary=true` + test invariante; decisión científica pendiente ADR-SCI-003/008) |
| S6R3-009 | **Barrera del global cross-dimensión (C2.5) — implementación mecánica de SCO-014/SCO-015 — LANDED (§23).** Autoridad ÚNICA = `MindBoostProfileAggregator.summarize` (KDoc: "the ONLY authority… no other class duplicates this gate"). Dos ramas independientes: **(i) PROVISIONAL (SCO-014):** `provisionalCompositeIndex` = `null` salvo el flag `mindboostGlobalScoreEnabled` (default-OFF en las TRES impls: `DefaultFeatureFlags` `FeatureFlags.kt:91`, `StaticFeatureFlags` `:110`, `RemoteConfigFeatureFlags` `MindOSApplication.kt:145`) ⇒ tipo DISTINTO `ProvisionalGlobalIndex(value, coveredDimensions, missingDimensions)` (`MindBoostProfile.kt:65`), expone dims cubiertas/ausentes, KDoc "NEVER feeds ranking", nunca confundible con el oficial. **(ii) OFICIAL (SCO-015):** `officialCompositeIndex` = `null` salvo la CONSTANTE de código `ADR_SCI_006_APPROVED` (`const val = false`, `MindBoostProfileAggregator.kt:56` — cambio de CÓDIGO revisado citando el ADR, JAMÁS un flag remoto) **Y** las 11 `CognitiveDimension` canónicas ESTABLISHED ⇒ **provablemente inalcanzable hoy**. Registrar un banco extra cambia cuántas dims son ESTABLISHED pero **NUNCA** desbloquea por sí solo ni el oficial (const) ni el provisional (flag) — probado (9 módulos ESTABLISHED + flag OFF ⇒ ambos `null`). Resuelve la duda CEO "sin segunda autoridad": `OFFICIAL_SCORING_ENABLED` no existía; se creó UNA capacidad (flag) + UNA const, con gate único. Gated por `ADR-SCI-006` (modelo global/pesos, ABIERTO §19). | `APPROVED` (barrera implementada; global OFICIAL inalcanzable hasta ADR-SCI-006 + flip de la const) |
| S6R3-010 | **Residuales conocidos aceptados/rastreados (NO bloqueantes).** **(A) 3 residuales LOW de `privacy-security` (§21):** (a) sin test que fuerce un `CancellationException` real a mitad de purga (código correcto por inspección); (b) sin test de DOS INSTANCIAS purgando en paralelo (seguro por idempotencia de `remove`/`put`, no por el mutex); (c) exposición transitoria inherente al storage compartido (el prefix-scan enumera claves de otros sujetos EN MEMORIA, nunca logueadas/devueltas — preexistente, no regresión de C1). **(B) 2 huecos residuales genuinos de C3** (elevados al `kmp-architect` en W4, **NO cerrados por ningún test** — producción congelada): (1) una extension-property/función top-level DENTRO del paquete que computa un ratio escalado — invisible al token-scanner Y al golden por reflexión (no es campo de un tipo pineado); (2) un tipo-wrapper nuevo con nombre distinto — invisible al golden hasta que un revisor lo añada. Hallazgos honestos alineados con el mandato de honestidad de C3; barrera candidata = una **allowlist POSITIVA de declaraciones top-level del paquete** (4ª barrera mecánica), **decisión de diseño futura**, no improvisada. | `PROVISIONAL` (residuales aceptados/rastreados; barrera candidata a decisión en W4) |

Reglas heredadas reafirmadas por este contrato: **SCO-004** (missing≠0), **SCO-005** (latest-raw prohibido),
**SCO-003** (XP/racha/suscripción nunca tocan score), **PRI-003/PRI-010** (pseudonimizado sigue siendo dato
personal; sin acceso cruzado), **TEC-011** (cambios de scoring exigen dual/shadow o migración). El contrato
evidence-only NO establece comparabilidad (§21 lineage incompleto) ⇒ prohibido promediar/dispersar entre
observaciones — impuesto por la AUSENCIA del input, no por un verdict que un consumidor podría ignorar.

---

## 21. Registro de cambios

### Reconciliación de canon en git (packRelease 2.0.2) — 2026-07-17

- **Adjudicación ChatGPT (D-H1).** `docs/context_pack` se establece como el **único canon vivo trazable en git**
  (antes ambas copias, hyphen y underscore, estaban untracked — `MB-ENG-0006`). Contenido base = la variante
  **underscore** (correcciones 2.0.2 ya presentes en `03`/`05`/`09`, 2026-07-12); no se altera el contenido científico.
- **`docs/context-pack` (hyphen)** queda `SUPERSEDED_SNAPSHOT_DO_NOT_USE`: permanece FUERA de HEAD, preservado en
  quarantine como snapshot histórico. Retenía la atribución PMID incorrecta (`35953659` → Passell), ya corregida en
  este canon (**S6R3-006**: `35953659` = Nicosia — latencia BYOD; `33954913` = Passell — variación de puntuaciones por
  dispositivo; verificado en PubMed 2026-07-17).
- **Mirror externo vivo** (`DOCS MB/00..09`) se sincroniza con este canon tras un backup recuperable; el snapshot
  **`PHASE0_FOCUS_FINAL`** permanece **INMUTABLE** (puede divergir del canon vivo por diseño).
- Cambio de **gobernanza documental** (no de producto ni de código): no cablea nada a perfil vivo / Global Score /
  Growth Index / rango / UI / Firestore / SyncQueue / cloud. Ledger: cierre de `MB-ENG-0006`. Focus v2/Slice 1 NO
  iniciados.

### 2.0.2 — 2026-07-12

- Añade a §20.1 las decisiones de cierre del contrato §18 (CIERRE OBLIGATORIO) ya ATERRIZADAS en código: **S6R3-007**
  (purga legacy sin sujeto, C1 — APTO privacy-security), **S6R3-008** (#1/#2 del perfil vivo Focus/Logic ADR-blocked,
  C2.4 — siguen `preliminary` hardcodeado), **S6R3-009** (barrera del global cross-dimensión, C2.5 — implementación
  mecánica de SCO-014/SCO-015 en `MindBoostProfileAggregator.summarize`) y **S6R3-010** (residuales conocidos: 3 LOW de
  privacy + 2 huecos C3 para `kmp-architect` W4, no bloqueantes).
- Actualiza **S6R3-003** de "disposición PENDIENTE CEO" a **folded**: el CEO decidió plegar el P1 de `SessionRecordStore`
  a este programa (reopen §14), el fix aterrizó (§16) y `privacy-security` lo revisó APTO con reservas (§17).
- Pase de **documentación W3** (`mindboost-docs-knowledge`), NO de producto ni de código: no cablea nada a perfil vivo /
  Global Score / Growth Index / rango / UI / Firestore / SyncQueue / cloud. Esta copia del repo **DIVERGE** del adjunto
  fuente del CEO; el CEO debe **espejar** estos cambios en su master canónico (y regenerar cualquier SHA-256 del context
  pack que dependa de este archivo).

### 2.0.1 — 2026-07-11

- Añade §20.1 con las decisiones de Slice 6-R3 (S6R3-001…006): falsación numérica de la barrera 6-R2 + rediseño
  facts-only, fix P0 `CognitiveSessionRunStore` (APTO), hallazgo P1 nuevo `SessionRecordStore` (disposición
  PENDIENTE CEO), posiciones ADR interinas y las dos preguntas de diseño abiertas.
- Registra la corrección de citación de `03 §29` (`35953659` = Nicosia, no Passell).
- Cambio de **documentación** (`mindboost-docs-knowledge`), NO de producto: esta copia del repo **DIVERGE** ahora
  del adjunto fuente 2.0.0 del CEO; el CEO debe **espejar** el cambio en su master canónico.

### 2.0.0 — 2026-07-10

- Consolida todas las decisiones CEO de la sesión de definición.
- Sustituye mínimo de 38 por 120 ejercicios.
- Separa PRD, ciencia, operación, técnica y validación.
- Fija Free sin IA y Elite de 2 €/mes con IA externa controlada.
- Fija auth VPS para release y Firebase sintético solo DEV.
- Corrige recuperación insegura por preguntas personales.
- Fija calibración 5 sesiones/3 días/48 h.
- Formaliza score, Growth Index, global, rangos y World Class.
- Formaliza alpha/beta, North Star de transferencia y gobierno multiagente.

---

## 22. Cómo añadir una decisión

Toda nueva decisión incluye:

```yaml
id: DOMAIN-NNN
date: YYYY-MM-DD
status: APPROVED|POLICY|HYPOTHESIS|PROVISIONAL|DEFERRED_ADR|SUPERSEDED|PROHIBITED
problem: ""
decision: ""
rationale: ""
evidence: []
confidence: ""
consequences: []
migration: ""
rollback: ""
supersedes: []
documents: []
tests_or_gates: []
owner: ""
```

Una conversación no cambia el sistema hasta actualizar las fuentes afectadas.

---

## 23. Regla final

Las IA deben usar este log para evitar ciclos de preguntas ya resueltas y para detectar implementaciones que arrastran supuestos superados.

Cuando una decisión aún sea técnica, deben resolverla con ADR, evidencia y gates; cuando sea empresarial o una acción externa, deben pedir autorización al CEO.
