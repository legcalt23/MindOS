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
| TEC-002 | Android 8/API26 e iOS15 como mínimos. | `SUPERSEDED` por ADR-FOCUS-PLATFORM-MIN (§20.2, 2026-07-20): Android mínimo = **API 29**. iOS 15 sigue vigente. |
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
| TEC-002-API26 | Android mínimo API 26 (canon) frente a minSdk 29 (repo real) | Android mínimo = API 29; el canon se alinea con el repo, no al revés | `SUPERSEDED` por ADR-FOCUS-PLATFORM-MIN (§20.2) |

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

## 20.2 Decisiones de Focus Engine V5 · Slice S1 (Contratos y ADR) — 2026-07-20

Fuente: `docs/engines/focus_v5/` (bundle V5 vendorizado, manifiesto verificado). Las seis decisiones fueron
presentadas al CEO en un solo lote con las opciones del `ADR_REGISTER_V5.md` y ratificadas en esa sesión. Ninguna
de ellas autoriza wiring a perfil vivo / Global Score / rango / UI / Firestore / cloud, ni desbloquea
`ADR-FOCUS-SCORING` (que permanece `BLOCKED_BY_DESIGN`: ninguna fórmula de capacidad antes de datos).

```yaml
id: ADR-FOCUS-PURPOSE
date: 2026-07-20
status: APPROVED
problem: "Dos autoridades vivas y contradictorias sobre si una sesion puede alimentar el perfil:
  SessionMeasurementPurpose (8 valores, TRAINING_FREE NO elegible) y SessionPurpose.mayContributeToProfile()
  (6 valores, TRAINING_FREE SI). Una de las dos miente en cada llamada."
decision: "Opcion 3. El enum de 8 valores es la UNICA autoridad; se extiende con TUTORIAL, FAMILIARIZATION y
  RESEARCH_ONLY. El gate devuelve un sealed de 3 vias Eligible | NotEligibleByPurpose | Indeterminate(reason);
  UNKNOWN queda reservado a no-sellado/ausente/cambiado y cae SIEMPRE a Indeterminate (deny). Se retira
  mayContributeToProfile() en el mismo cambio. Cero proyeccion entre enums."
rationale: "Dos enums con proyeccion no son una autoridad, son dos opiniones con un mapa. La opcion 1 (colapsar
  FAMILIARIZATION a PRACTICE) es lossy justo donde S3/S4 necesitan distinguir el washout de familiarizacion de la
  practica real. Verificado en el repo antes de decidir: mayContributeToProfile() tiene UN solo call-site de
  produccion (CognitiveSessionEligibility.kt:159), asi que el coste real de retirarla es bajo."
evidence:
  - "ProfileMeasurementEligibility.kt:10-51 (autoridad 8v, fail-closed)"
  - "CognitiveSessionEnums.kt:25 (autoridad rival, TRAINING_FREE permisivo)"
  - "SessionEvidenceGates.kt:52 (la capa de evidencia YA se re-deriva sola por 6R3-P1-06)"
confidence: "Alta en el contrato; el coste de migracion esta medido, no estimado."
consequences:
  - "Anadir miembros a un enum persistido por nombre es aditivo: ninguna fila historica cambia de significado."
  - "Un reader antiguo ante un valor nuevo devuelve Indeterminate: nunca crash, nunca elegible."
  - "El purpose se SELLA antes del primer trial; cambio mid-session => sesion Indeterminate para evidencia,
    con la practica conservada. TRAINING nunca se eleva retroactivamente a oficial."
migration: "Aditiva. Sin reescritura de datos."
rollback: "Dejar de emitir los miembros nuevos; los ya persistidos caen a Indeterminate (fail-closed)."
supersedes: []
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md", "MINDBOOST_AUDIT_LEDGER.md"]
tests_or_gates: ["T-S1", "disposition 3-vias exhaustivo incl. valor desconocido",
  "propiedad: NINGUN camino de codigo responde elegibilidad desde el enum de 6 valores"]
owner: "kmp-architect"
```

```yaml
id: ADR-FOCUS-DATA
date: 2026-07-20
status: APPROVED
problem: "C-06. Que se persiste por sesion de Focus v2: solo resumenes, journal replay-complete, o tiered — y
  con que retencion. CLAUDE.md §5 dice 'solo resumenes, nunca trials crudos' (POLICY viva); el research pack pide
  raw (EXTERNAL_UNVERIFIED)."
decision: "Opcion (iii) TIERED. Resumenes por defecto; journal por-trial UNICAMENTE en sesiones cuyo proposito
  sellado es de medicion, con volumen capado por dosis diaria. Proposito indeterminable => cae a (i) resumenes.
  Cifrado en namespace AccountBound, retencion acotada con retentionPolicyVersion y purga por edad, y DoS-cap
  obligatorio con rechazo TIPADO. Si el cap o el cifrado fallan: WITHHELD_TECHNICAL fail-closed, jamas escritura
  degradada silenciosa."
rationale: "La eleccion no es simetrica en el tiempo. El protocolo de reapertura fija que un cambio (i)->(iii)
  solo cubre sesiones POSTERIORES a la decision: elegir (i) hoy dejaria TODO lo recogido en alpha permanentemente
  sin poder re-puntuarse, y el proposito declarado del programa es elegir la formula de scoring a partir de datos
  humanos reales en S8/S9. (iii) mantiene esa puerta abierta al minimo coste de privacidad, porque el journal solo
  existe donde hay proposito de medicion, no en el entrenamiento diario."
evidence:
  - "CLAUDE.md §5 (politica de solo-resumenes, viva)"
  - "ReproducibleSessionIdentity + FormDescriptorV1 (la reproducibilidad de FORMA ya existe sin raw:
     re-genera estimulos, NO respuestas)"
  - "docs/engines/focus_v5/agent_outputs/04 §A-B (analisis de opciones y contrato de datos de 14 ejes)"
confidence: "Media-alta. La forma es solida; la ventana concreta de retencion se revisa con datos de alpha."
consequences:
  - "HONESTIDAD OBLIGATORIA: un journal LOCAL no es evidencia anti-manipulacion (dispositivo del usuario, sin
     firma ni sync). Su valor es auditoria de coherencia interna y re-scoring, y asi debe declararse siempre."
  - "El DoS-cap deja de ser opcional: al elegir (iii) se construye ANTES que cualquier journal (S1, no S8)."
  - "Se requiere consentimiento por finalidades separado (patron PRI-001/002), no un consentimiento generico."
  - "Los datos ya recolectados CONSERVAN el regimen bajo el que se recolectaron: etiqueta de regimen por fila,
     jamas re-etiquetado retroactivo."
migration: "Ninguna sobre datos existentes: namespace v2 nuevo. El journal solo cubre sesiones posteriores a esta
  fecha; prohibida cualquier reconstruccion retroactiva."
rollback: "(iii)->(i): el journal existente se purga por su retencion declarada y los outcomes replay-dependientes
  vuelven a BLOCKED desde la fecha del cambio. Todo cambio re-dispara la revision de privacidad y el DPIA."
supersedes: []
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-DATA"]
tests_or_gates: ["T-S1", "DoS-cap: escritura sobre cap => rechazo tipado, nunca excepcion ni escritura parcial",
  "escritura que sobrevive al borrado de cuenta = hueco GDPR (patron RevocationGapsTest)"]
owner: "CEO (propietario y responsable de datos) / implementa kmp-architect"
```

```yaml
id: ADR-FOCUS-SESSION-STATE
date: 2026-07-20
status: APPROVED
problem: "Focus v2 necesita estados de runtime (incluida PAUSA) que el SessionStatus global
  {STARTED, CHECKPOINT, FINALIZED, ABANDONED} no tiene."
decision: "Opcion (a). Estado de runtime Focus-LOCAL: un tipo propio con SIETE estados
  (Idle/Tutorial/Running/Paused/Interrupted/Completed/Invalidated) en el paquete focus2, mapeado al SessionStatus
  global SOLO en los puntos de write-ahead (Started->STARTED, checkpoint->CHECKPOINT, terminal->FINALIZED/ABANDONED).
  El contrato global no se toca. La maquina transita por LOOKUP en una tabla estado x evento -> estado' | RECHAZO
  tipado."
decision_amendment: "ENMENDADA EN LA IMPLEMENTACION (misma fecha, S1-P7). El texto ratificado decia OCHO estados
  e incluia `Resumed`; el codigo implementa SIETE y modela `Resumed` como EVENTO. Razon: un estado `Resumed` se
  comporta de forma identica a `Running` en toda transicion, y dos estados indistinguibles por comportamiento son
  una distincion FABRICADA — el hecho real que se quiere conservar ('esta sesion se reanudo') es un EVENTO del
  journal, no un modo del runtime. Se enmienda el texto en vez de restaurar `Resumed` porque restaurarlo anadiria
  un estado que ningun test podria discriminar. Se implementa como `enum` y no como `sealed` porque ningun estado
  transporta payload; si alguno lo necesita, pasa a sealed en ese mismo cambio."
rationale: "La pausa es semantica de RUNTIME, no de recovery: meterla en el contrato transversal ensucia 32
  archivos y 7+ features con un concepto que no les pertenece, y obliga a tocar todos sus when exhaustivos
  (learning 2026-06-16). El repo ya tiene el precedente exacto de capa separada en core.session.cognitive."
evidence:
  - "core/session/SessionRecord.kt:17 (SessionStatus global, sin PAUSED)"
  - "docs/engines/focus_v5/agent_outputs/01 §D (inventario: 32 archivos, 7+ features)"
confidence: "Alta."
consequences:
  - "El sealed cubre el SUBCONJUNTO de eventos de CICLO DE VIDA DE SESION del estandar de 16. Los eventos de
     trial (TrialPresented, ResponseStarted, ResponseSubmitted, TrialResolved, DifficultyChanged) y los de
     infraestructura (ExerciseOpened, ResultPersisted, ResultSynced) son EVENTOS del journal, no estados, y no
     generan tipo de estado. No es un mapeo 1:1 con los 16."
  - "Una transicion no declarada en la tabla se RECHAZA de forma tipada; no existe transicion implicita."
migration: "Aditiva y local."
rollback: "Borrar el paquete focus2 lo revierte por completo."
supersedes: []
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-SESSION-STATE"]
tests_or_gates: ["T-S1", "toda transicion legal e ilegal cubierta por tabla (transicion sin test = estado fabricado)"]
owner: "kmp-architect"
```

```yaml
id: ADR-FOCUS-MIGRATION
date: 2026-07-20
status: APPROVED
problem: "Cuantas versiones de reader mantiene vivas Focus v2, y con que criterio se retira el namespace legacy."
decision: "Ventana N-1: el reader de la version N entiende N y N-1, y escribe siempre N. Marco heredado del
  patron del repo y reafirmado aqui: schemaVersion en todo DTO persistido; reader fail-closed ante version futura;
  claves subject-scoped desde el dia 1; namespace nuevo => AccountBound + eraser + coverage-test EN EL MISMO
  CAMBIO; prohibido last-write-wins para historia cognitiva; toda migracion destructiva exige sign-off de privacy."
rationale: "N-1 cubre el caso real (un usuario que se salto una version) sin acumular ramas de deserializacion que
  nadie ejercita y que se pudren sin test. No hay dual-read/write porque hoy NINGUN consumidor vivo lo exige: v2 no
  reemplaza datos v1, viven en namespaces distintos y v1 se conserva RETIRED."
evidence: ["learning 2026-06-28 (namespace sin eraser = dato sensible que nunca se borra)",
  "S6R3-002/003 (jamas claves sin sujeto)"]
confidence: "Alta."
consequences:
  - "El historial v1 nunca se migra in-place: se preserva bajo su propio namespace RETIRED."
  - "Criterio de retirada del namespace legacy: solo cuando exista el reemplazo funcional completo MAS export de
     usuario. Antes de eso, retirarlo es perdida de datos del usuario."
migration: "N/A (define el marco de las demas)."
rollback: "Flag fail-closed OFF + el paquete/namespace v2 es borrable sin tocar v1."
supersedes: []
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-MIGRATION"]
tests_or_gates: ["T-S1", "AccountBoundErasureCoverageTest", "reader N-1 y reader ante version futura"]
owner: "kmp-architect"
```

```yaml
id: ADR-FOCUS-EVIDENCE
date: 2026-07-20
status: APPROVED
problem: "La estructura de evidencia (4 ejes ortogonales) esta aprobada por canon, pero el gate de cobertura de
  faceta seguia sin umbral."
decision: "Se sella como INTERINO: una faceta requiere >=2 metodos (ejercicios distintos) para declararse
  cubierta, y ninguna tarea individual puede aportar mas del 50% de la informacion de su faceta. Revision empirica
  OBLIGATORIA en S9. El modelo de error para confidence (SEM/split-half real) queda ABIERTO: hasta entonces
  confidence es baja o UNKNOWN, JAMAS fabricada."
rationale: "Sin umbral, una sola tarea puede impersonar una faceta entera y la cobertura se declara con criterio
  improvisado sesion a sesion. Un umbral interino explicito y revisable es honesto; la ausencia de umbral no lo es.
  Los cuatro ejes siguen siendo ORTOGONALES: confidence JAMAS se deriva de sufficiency (learning 2026-06-28)."
evidence: ["ProfileUpdateGate.kt:40-58 (sufficiency y confidence separados)",
  "ADR-MSR-001/-020 del pack como referencia EXTERNAL, sin autoridad propia"]
confidence: "Baja-media en el NUMERO (por eso es interino); alta en la estructura."
consequences:
  - "Toda ausencia se modela como Missingness(reason, scope) tipado, con test en AMBOS sentidos: ausencia que
     fabrica un minimo Y ausencia que fabrica un maximo (learning 2026-07-15)."
  - "Comparabilidad no establecida por defecto => prohibido promediar, impuesto por AUSENCIA del input."
migration: "Tipos nuevos aditivos en el namespace evidence."
rollback: "No-emision."
supersedes: ["CV-confidence como modelo final (ya SUPERSEDED en §18)"]
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-EVIDENCE"]
tests_or_gates: ["T-S1", "T-S8"]
owner: "psychometrician propone / CEO ratifica"
```

```yaml
id: ADR-FOCUS-PLATFORM-MIN
date: 2026-07-20
status: APPROVED
problem: "Contradiccion viva: el canon TEC-002 fija Android API 26 como minimo; el repo real tiene minSdk 29."
decision: "Opcion (b). Se registra TEC-002 como SUPERSEDED y el minimo canonico pasa a API 29, alineando el canon
  con la realidad del repo. No se baja minSdk."
rationale: "Bajar a 26 exigiria una matriz de compatibilidad AGP 9.1 / Compose 1.10.3 / Kotlin 2.3.10 en API 26,
  coste de QA en hardware antiguo y ensanchar las bandas de timing justo donde la medicion es mas fragil — a cambio
  de dispositivos que representan una fraccion decreciente del mercado. Resolver la contradiccion 'arreglando el
  doc' en silencio estaba explicitamente prohibido (R-12); esta entrada es la decision registrada que lo permite."
evidence: ["minSdk=29 en el build real del repo", "TEC-002 APPROVED en §15 (ahora SUPERSEDED)"]
confidence: "Alta."
consequences:
  - "El protocolo de timing de S5 deja de necesitar la banda de API 26."
  - "iOS 15 como minimo NO se toca en esta decision: sigue vigente y se revisa en su propia slice."
migration: "Documental. Cero codigo."
rollback: "Una nueva entrada de decision que reinstaure API 26 junto con su slice tecnica de compatibilidad."
supersedes: ["TEC-002"]
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-PLATFORM-MIN"]
tests_or_gates: ["T-S5"]
owner: "CEO (decision de producto exclusiva)"
```

```yaml
id: ADR-FOCUS-BOUNDARY-EXCEPTION
date: 2026-07-20
status: PROVISIONAL
problem: "SecureKeyValueStore — la primitiva de almacenamiento cifrado de TODOS los datos cognitivos de
  MindBoost — vive DENTRO del paquete de feature focus/ (focus/data/local/). 44 archivos de produccion fuera de
  focus/ ya importan de ahi (104 contando tests). Focus v2 no puede declarar una frontera 'focus2 no depende de
  focus' sin resolverlo, y S1 debia decidir entre extraerlo a core/ o documentar una excepcion (plan R-10, gate
  T-S1)."
decision: "EXCEPCION EXPLICITA Y ACOTADA, no extraccion. focus2 puede importar EXACTAMENTE
  `focus.data.local.SecureKeyValueStore` y sus extensiones de lectura (`readVersioned`, `readValueOrNull`,
  `readParsed`) y `InMemorySecureKeyValueStore` en tests. Ningun otro simbolo de `focus.*` es importable desde
  `focus2`. La excepcion es PROVISIONAL y se retira cuando la extraccion a core/ ocurra."
rationale: "Extraer ahora significa tocar 104 archivos en la slice de contratos, con un riesgo de regresion
  desproporcionado para cero ganancia funcional. Pero importar en silencio y seguir adelante habria dejado la
  frontera v2 indefendible y la decision que el plan pedia sin tomar. Una excepcion nombrada y acotada es
  honesta; una dependencia no declarada no lo es."
evidence:
  - "MB-FOCUS-0003 en MINDBOOST_AUDIT_LEDGER.md (conteo re-derivado: 44 produccion / 104 total)"
  - "focus2/data/consent/FocusDataConsent.kt importa focus.data.local.SecureKeyValueStore"
confidence: "Alta en el diagnostico; la excepcion es un puente, no una solucion."
consequences:
  - "El boundary-test de S3 (Focus2DependencyBoundaryTest) debe implementar la ALLOWLIST de esta entrada, no una
     prohibicion total de focus.* — si no, nacera rojo o se escribira debil para pasar."
  - "Mientras la excepcion viva, MB-FOCUS-0003 sigue OPEN: esta decision lo acota, no lo cierra."
migration: "Ninguna hoy. La extraccion a core/ es su propia slice, con owner kmp-architect."
rollback: "N/A (no cambia comportamiento)."
supersedes: []
documents: ["MINDBOOST_AUDIT_LEDGER.md", "docs/engines/focus_v5/FOCUS_ENGINE_MASTER_PLAN_V5.md §10 R-10"]
tests_or_gates: ["T-S1", "S3 Focus2DependencyBoundaryTest"]
owner: "kmp-architect"
```

### 20.3 · Huecos DECLARADOS de S1 (no resueltos, con owner y gate de cierre)

Salidos del gate de salida de S1 (efficacy-guardian, diff-auditor, privacy-security en contexto fresco). Se
registran aqui porque **taparlos habria sido peor que declararlos**: los cuatro son reales, ninguno es explotable
hoy, y cada uno tiene un punto exacto donde deja de ser teorico.

```yaml
id: GAP-FOCUS-RETENTION
severity: MEDIA
status: OPEN — BLOQUEA S8
problem: "ADR-FOCUS-DATA exige retencion acotada para el journal por-trial. NO EXISTE: cero coincidencias de
  retentionPolicyVersion/retention/purge/expire/ttl en todo el repo. MAX_JOURNAL_ENTRIES_PER_SUBJECT (500) es un
  tope de CANTIDAD, no de EDAD: por debajo de 500 entradas, una entrada vive indefinidamente."
why_not_fixed_now: "Construir una politica de retencion para un almacen que no existe es especulacion. Lo que S1
  puede garantizar honestamente es que NO se puede escribir nada todavia: FocusJournalAdmission no tiene ningun
  call-site de produccion."
closure_gate: "S8 no puede cerrar sin (a) campo de edad + purga probada y (b) retentionPolicyVersion persistido."
owner: "privacy-security + kmp-architect"
```

```yaml
id: GAP-FOCUS-PERSISTED-FLAG-REGIME
severity: BAJA (latente, no vivo)
status: DECLARADO
problem: "SessionEligibility.profileEligible se PERSISTE y su significado cambio en S1 (antes: denylist que
  admitia TRAINING_FREE; ahora: allowlist de 3 propositos + lineage). SCHEMA_VERSION sigue en 1, asi que una fila
  pre-S1 y una post-S1 son indistinguibles aunque respondan a reglas distintas."
why_not_fixed_now: "El namespace SessionRuns esta documentado VACIO en produccion: no hay ni una fila que
  reetiquetar. Bumpear el schema activaria la maquinaria de reader N-1 para cero datos. Y el unico lector
  (CognitiveSessionHistory.mayCountTowardProfile) es un campo de PRESENTACION: ninguna decision lo consume."
closure_gate: "La primera slice que escriba filas reales de CognitiveSessionRun debe llevar etiqueta de regimen
  por fila (ADR-FOCUS-DATA lo exige) o bumpear SCHEMA_VERSION. No se permite reetiquetado retroactivo."
owner: "kmp-architect"
```

```yaml
id: GAP-ERASER-MAP-UNTESTED
severity: MEDIA (PRE-EXISTENTE, no introducido por S1)
status: DECLARADO
problem: "Los dos tests de cobertura de borrado construyen un mapa SINTETICO
  (AccountBound.associateWith { InMemorySecureKeyValueStore() }), asi que son tautologicamente completos: nadie
  lee el mapo REAL de AppGraph. Un namespace olvidado alli no lo detecta CI."
correction_to_the_original_premise: "El eraser NO omite en silencio — usa requireNotNull y LANZA. El fallo se
  manifiesta como crash de arranque, no como dato que sobrevive. Es un hueco de COBERTURA, no de borrado."
why_not_fixed_now: "El mapa se construye dentro de un remember{} en un @Composable; hoistearlo para testearlo
  toca la estructura de AppGraph y arriesga su test de igualdad estructural (learning 2026-06-19). Es su propia
  tarea, no un efecto colateral de una slice de contratos. mindboost_focus2 SI esta correctamente borrado."
closure_gate: "Extraer el mapa a una funcion pura y afirmarla contra AccountBound."
owner: "privacy-security"
```

```yaml
id: GAP-SHARED-DEVICE-CONSENT
severity: BAJA
status: DECLARADO
problem: "El borrado de cuenta usa clearNamespace() sobre mindboost_focus2, que es un wipe de NAMESPACE: en un
  dispositivo compartido, borrar la cuenta A elimina tambien el consentimiento de B."
why_not_fixed_now: "Es el patron legacy de TODOS los namespaces del repo; focus2 no lo empeora. Corregirlo solo
  para focus2 exige borrado subject-scoped en AccountDeletionCoordinator — una slice transversal."
mitigation_shipped: "Las claves de focus2 ya son subject-scoped desde el dia 1, asi que el borrado selectivo es
  IMPLEMENTABLE cuando se decida; no hay que migrar claves para poder hacerlo."
closure_gate: "Borrado por-sujeto en el coordinador + test de que la cuenta B sobrevive."
owner: "privacy-security"
```

**No decidido en S1, y deliberadamente:** `ADR-FOCUS-SCORING` sigue `BLOCKED_BY_DESIGN` (ninguna formula de
capacidad antes de simulacion + datos humanos repetidos + analisis), `ADR-FOCUS-CONSTRUCT` se decide en S2 y
`ADR-FOCUS-TIMING` en S5. Un eje sin decision no avanza: jamas un default implicito.

---

## 20.4 Decisiones de Focus Engine V5 · Slice S2 (Modelo cientifico y portfolio) — 2026-07-20

> **Orden de registro deliberado.** `ADR-FOCUS-CONSTRUCT` se registra ANTES que la ratificacion del portfolio.
> El portfolio esta construido sobre "la opcion recomendada (a)" de ese ADR (`EXERCISE_PORTFOLIO_V5.md:11`),
> asi que ratificarlo primero habria decidido el ADR POR IMPLEMENTACION — algo que su propio registro prohibe
> expresamente: *"Ningun ADR de este registro puede decidirse 'por implementacion'"* (`ADR_REGISTER_V5.md:6-8`).

```yaml
id: ADR-FOCUS-CONSTRUCT
date: 2026-07-20
status: APPROVED
problem: "Que estructura de constructo gobierna Focus v2 y que subhabilidades pueden mostrar subscore."
decision: "Opcion (a) — Navegacion 5 / medicion 3 + propiedad. Las CINCO subhabilidades del PRD §6.1
  (sostenida, selectiva, inhibicion de distractores, control atencional, estabilidad bajo tiempo) permanecen
  como ETIQUETAS de navegacion y organizacion de ejercicios. Los subscores futuros (post-V3) solo podran
  existir para SOSTENIDA, SELECTIVA e INHIBICION DE DISTRACTORES. Control atencional y estabilidad bajo tiempo
  se muestran como composite hipotetico etiquetado o como dinamica, JAMAS como subscore."
rationale: "Es la unica opcion compatible a la vez con el dictamen de eficacia y con CAT-002 sin reabrir el
  PRD. El dictamen degrada dos de las cinco COMO FACTORES —no como etiquetas—: control atencional sale
  CONTRADICTED como subscore (evidencia en conflicto, E001 vs E002/E005, veredicto de existencia CONFLICTED) y
  estabilidad bajo tiempo sale CONTRADICTED como capacidad separada: es una propiedad de SUSTAIN, con
  fiabilidad de pendiente ~0 y confounds que incluyen throttling termico y bateria. Fijarlo AHORA cierra la
  puerta a inventar subscores que la evidencia no sostiene; diferirlo (opcion b) dejaria el portfolio
  construido sobre una premisa sin ratificar y trasladaria la decision a un punto donde ya habria codigo
  escrito asumiendola. La opcion (c) (reabrir el PRD a 3 facetas) estaba pre-RECHAZADA en el propio ADR."
evidence:
  - "docs/engines/focus_v5/ADR_REGISTER_V5.md:27-49 (enunciado del ADR y sus tres opciones)"
  - "docs/engines/focus_v5/agent_outputs/02_efficacy_guardian.md:17-23, :45-52 (degradacion de las dos facetas)"
  - "CAT-002 (09 §5:70): los motores visibles no implican factores independientes"
confidence: "Alta en el recorte de facetas; la estructura latente sigue siendo 100% HIPOTESIS hasta S9/beta."
consequences:
  - "En C0 NINGUN subscore se muestra, bajo cualquier opcion. El copy usa las 5 como categorias de practica."
  - "RESTRAIN (go/no-go, stop-signal) NO pertenece a las 5 (E051: freno motor != inhibicion de interferencia).
     Su ruta al perfil sigue exigiendo un ADR propio, no cubierto por esta decision."
  - "El registro de constructo v2 lleva `constructModelVersion` para poder re-etiquetar sin reescribir
     historia. La estructura latente no se congela: se versiona."
migration: "Ninguna (contrato de copy/registro)."
rollback: "Nueva entrada de decision. Re-etiquetado via constructModelVersion, sin reescribir historia."
supersedes: []
documents: ["docs/engines/focus_v5/ADR_REGISTER_V5.md §ADR-FOCUS-CONSTRUCT"]
tests_or_gates: ["S2 revision cientifica registrada", "S9 subscores"]
owner: "CEO (producto) + revision cientifica"
```

### 20.4.1 · Correcciones al bundle V5 detectadas en S2 (defectos, no preferencias)

```yaml
id: FIX-FOCUS-S2-GATE-NAME
date: 2026-07-20
status: APPROVED
problem: "El plan describe un gate `T-S2` para esta slice. NO EXISTE: grep sobre docs/ = 0 hits. El catalogo
  retiro ese codigo y lo remapeo (`GATE_CATALOG_V5.md:55`, G-S2 -> T-ADV)."
decision: "S2 NO tiene gate tecnico con nombre. Su exit gate es el enunciado en prosa del plan:
  'revision cientifica registrada + seleccion de tracers en 09 §22' (`FOCUS_ENGINE_MASTER_PLAN_V5.md:273`).
  PROHIBIDO reportar 'T-S2 PASS' en cualquier informe, ledger o commit: seria un gate inventado."
rationale: "Un gate con nombre inexistente reportado como PASS es indistinguible de un gate real aprobado.
  Es exactamente la clase de falso verde que el charter prohibe."
confidence: "Alta (verificado por grep exhaustivo sobre docs/)."
owner: "kmp-architect"
```

```yaml
id: FIX-FOCUS-S2-PORTFOLIO-APPROVED
date: 2026-07-20
status: APPROVED
problem: "El plan declara que S2 alcanza el estado `PORTFOLIO_APPROVED` (`FOCUS_ENGINE_MASTER_PLAN_V5.md:254`),
  pero ese estado NO EXISTE en el vocabulario de madurez del catalogo, que es
  SPEC -> INTERNAL_TECHNICAL_TRACER -> TRAINING_READY -> MEASUREMENT_CANDIDATE -> OFFICIAL_MEASURE
  (`GATE_CATALOG_V5.md:99-102`). Una sola ocurrencia en todo el repo. Por la regla de completitud del propio
  catalogo (`:6-8`) eso es un DEFECTO DE BUNDLE."
decision: "Se DEFINE `PORTFOLIO_APPROVED` en el catalogo como un estado DOCUMENTAL, explicitamente separado de
  la escalera tecnica: 'portfolio y tracers ratificados por el CEO con revision cientifica registrada; NINGUN
  ejercicio implementado ni jugable'. La escalera SPEC->...->OFFICIAL_MEASURE describe madurez de UN EJERCICIO;
  este estado describe la ratificacion de un CONJUNTO. Son ejes distintos y no se comparan."
rationale: "Las dos alternativas eran peores: usar SPEC encaja mal (describe un ejercicio, no un portfolio) y
  deja el hueco del catalogo abierto; no declarar estado deja el plan con una casilla vacia. Definirlo cierra
  el defecto y permite que el informe de S2 diga la verdad."
consequences:
  - "PORTFOLIO_APPROVED no implica NADA sobre implementacion: ningun ejercicio del portfolio es jugable por
     haberlo alcanzado. Confundirlo con TRAINING_READY seria un overclaim."
owner: "CEO"
```

```yaml
id: FIX-FOCUS-S2-DOSSIER-FORMAT
date: 2026-07-20
status: APPROVED
problem: "La tarea (4) de S2 pide dossiers en 'formato Quality Standard §20'. §20 es 'Gates de entrada y
  salida' (`04_EXERCISE_QUALITY_STANDARD.md:807`); el formato de dossier es §5 (`:136-249`). La referencia
  salio casi con seguridad del item de gate §20.2 'Dossier completo' (`:822`) — el gate que EXIGE un dossier,
  no la seccion que lo DEFINE. El mismo plan cita ademas un segundo formato rival de 19 encabezados
  (`FOCUS_ENGINE_MASTER_PLAN_V5.md:259`)."
decision: "HIBRIDO: §5 del Quality Standard como dossier autoritativo (12 bloques, ~92 campos), MAS los dos
  encabezados adversariales que §5 no tiene y que el formato de 19 si aporta: 'Revision critica' y 'Veredicto'."
rationale: "§5 es lo unico en el repo que es de verdad un formato de dossier y es lo que satisface el gate
  §20.2; pero carece por completo de seccion adversarial, que es justo lo que un portfolio falsificador-driven
  necesita. El formato de 19 tiene 66 ejemplos trabajados pero no cubre identidad, versionado, privacidad,
  tecnica ni validacion, asi que por si solo no satisface el gate."
consequences:
  - "~30 de los ~92 campos describen artefactos que crea S3 y NO son conocibles en S2. El estandar prohibe
     rellenar con texto generico (`04_EXERCISE_QUALITY_STANDARD.md:251`). Se sanciona UNA forma no vacia:
     `PENDING_S3 (owner: <nombre>, gate: T-S3)`. Cualquier otra cosa es un gate bloqueado o un campo fabricado."
  - "La tarea (4) es AGUAS ABAJO de la (5), no paralela: `measurement.primaryMetrics`, `scoringVersion`,
     `aggregationPolicy` y `uncertaintyMethod` son la SALIDA de la simulacion. Se ejecuta (5) y luego (4)."
owner: "CEO"
```

### 20.4.4 · Tarea (1) — Ratificacion del portfolio

```yaml
id: RAT-FOCUS-S2-PORTFOLIO
date: 2026-07-20
status: APPROVED
decision: "Portfolio de 18 ejercicios (FOC2-E01..FOC2-E18) RATIFICADO con sus estados C0, tras completar los
  dos dossiers que no eran ratificables y REDISENAR uno de ellos. Distribucion final: 13 ACCEPT,
  4 PROTOTYPE, 1 RESEARCH_ONLY."
process_note: "16 de las 18 filas se ratificaron tal cual. Las otras dos NO se podian ratificar honestamente
  —E14 tenia 2 de los 7 campos de la plantilla del propio documento, E16 no tenia falsificador en un
  documento cuyo metodo entero es falsificador-driven— y el CEO opto por COMPLETARLAS en vez de retenerlas.
  El trabajo se enruto a efficacy-guardian (claims y confounds) y psychometrician (matematica y potencia),
  no se redacto desde la sesion principal: rellenar campos de dosier sin dictamen fresco habria sido
  exactamente el texto generico que 04_EXERCISE_QUALITY_STANDARD.md:251 prohibe."

E16_Contramanda:
  estado: "RESEARCH_ONLY — RATIFICADO"
  que_faltaba: "falsificador y clausula de transferencia (5 de 7 campos presentes)"
  resolucion: "Falsificador anadido con cuatro condiciones numericas —(a) supuesto de carrera roto: RT medio
    de stop-fallido >= RT medio de go en >20% de sesiones; (b) staircase que no converge: p(responder|senal)
    fuera de 0.45-0.55; (c) el dispositivo domina la varianza: SSD entregada desviada de la nominal por mas
    que la diferencia entre usuarios que se pretende medir; (d) trigger-failure > 0.10— mas MITAD
    DISCRIMINANTE OBLIGATORIA (un perfil sintetico con freno bien especificado DEBE pasar (a)-(d); si no
    pasa nadie, el falsificador esta clampando). Transferencia: NINGUNA afirmable, ni siquiera T1."
  razon_de_que_ahora_si: "Un RESEARCH_ONLY sin falsificador no es un estado, es un aplazamiento indefinido:
    no existia condicion bajo la cual graduarse ni bajo la cual retirarse. Ahora tiene ambas."
  consecuencia_S3: "(c) exige clase de timing T2, INALCANZABLE hoy ⇒ E16 no puede salir de RESEARCH_ONLY
    antes del banco fisico de latencia. Y el reuse autorizado del generador v1 «E4 Freno» es SOLO del
    generador: SSRT como metrica gobernante esta retirado por la regla 9, lo que exige barrera de build al
    estilo evidenceBoundaryScanner, no un KDoc."

E14_redisenado:
  decision_CEO: "REDISENAR con otra mecanica (frente a retirar la fila o dejarla RESEARCH_ONLY)."
  por_que_el_anterior_no_servia: "No era falta de madurez. Era (i) IDENTIDAD DE DATOS con E03 —mismo bloque
    largo, misma sesion, mismo generador, y E03 ya reporta la curva por cuartil—, (ii) su unica aportacion
    sobre E03 era la PENDIENTE, un difference-score de la clase retirada por la regla 1 y con fiabilidad
    individual ~0 por construccion, y (iii) su confound tecnico (throttling termico/bateria) no es separable
    de la degradacion cognitiva en clase T0/T1. Estaba dominado por los dos lados: su exactitud es E03, su
    estabilidad de RT es E13."
  nuevo: "FOC2-E14 «Compas variable» — PROTOTYPE. Bloque single-tap con formas no-lexicas, organizado en
    tramos contrabalanceados que alternan ISI ISOCRONO (llegada anticipable) e ISI JITTER (no anticipable),
    con la MEDIA de ISI, la tasa de objetivos, la discriminabilidad y la duracion IGUALADAS entre
    condiciones. Lo unico que difiere es la PREDICTIBILIDAD TEMPORAL. Primario: exactitud/d'/criterio por
    condicion (ISO y JIT por separado). ISO-JIT existe pero es DESCRIPTIVO y esta PROHIBIDO como puntuacion."
  que_mide: "NO una capacidad. La faceta sigue CONTRADICTED como capacidad separada y este rediseno no la
    reabre. Mide una PROPIEDAD de la atencion sostenida: cuanto depende la exactitud del usuario de que las
    cosas lleguen a un ritmo anticipable."
  confounds_disjuntos: "Los confounds letales de E03 (deriva termica/bateria, aburrimiento, hora, fatiga
    acumulada) y de E13 (jitter del dispositivo, timebase T0/T1) son de DERIVA LENTA o de temporizacion
    fina. Ninguno puede producir el efecto de E14: las condiciones estan entrelazadas dentro del mismo
    intervalo, asi que cualquier degradacion monotona afecta a ambas por igual y se cancela; y el primario
    es exactitud, no RT, asi que el jitter no lo contamina. Ademas el gaming ritmico pasa de confound a
    MANIPULACION: en JIT es estructuralmente imposible y en ISO es medible."
  por_que_PROTOTYPE_y_no_ACCEPT: "**Tension aritmetica que NO cierra**, hallada por el psicometra: no caben
    a la vez (1) bloque corto, (2) d' por condicion con n>=20 por celda, y (3) manipulacion temporal
    detectable. A 4-6 min quedan ~15-22 objetivos por condicion, por DEBAJO del piso de
    MinSampleGate.DPRIME=20. La dosis que si lo alcanza son ~220 trials => 8-9 min reales => ES bloque largo
    y COMPITE con E03 por la cuota de la regla 7. ACCEPT afirmaria que el diseno es estimable en su
    envolvente de dosis, y la aritmetica demuestra que todavia no lo es."
  disidencia_registrada: "AMBOS especialistas prefieren, si la envolvente no cierra, RETIRAR la faceta† y
    dejar E13 (ACCEPT) bajo sostenida con el portfolio a 17. La envolvente NO cierra. El CEO opto por el
    rediseno; la preferencia de los especialistas queda registrada aqui, no en una nota al pie."
  riesgo_dominante_no_resuelto: "Que E14 sea E01 con dos etiquetas —es decir, que no aporte varianza
    incremental—. Ni el diseno ni la psicometria lo resuelven: SOLO lo decide la condicion (iii) del
    falsificador (r(ISO,JIT) desatenuada <= 0.90), y ese dato NO EXISTE hoy."
  falsificador: "Sobre >=120 usuarios con >=2 sesiones: (i) split-half par/impar Spearman-Brown de d'_ISO y
    d'_JIT, cada una >=0.60; (ii) test-retest de cada d' >=0.50; (iii) r(ISO,JIT) desatenuada <=0.90;
    (iv) objetivos efectivos >=20 por condicion en >=90% de las sesiones. Falla cualquiera ⇒ RETIRAR E14.
    Mitad discriminante obligatoria: simulador con delta~N(0.45,0.30) DEBE pasar (i)-(iv), y espejo negativo
    con delta~N(0.05,0.30) DEBE fallar (iii); ambos corridos ANTES de ratificar el falsificador."
  clasificacion_pendiente: "El propio disenador admite que «predictibilidad temporal» puede pertenecer mas a
    alerting que a sostenida. NO RESUELTO — decision de producto, no tecnica."

no_ratificado_por_omision: "Los identificadores de evidencia que cita el portfolio (E001, E013, E051, etc.)
  NO EXISTEN en docs/knowledge/cognitive_science/ (el corpus usa prefijos F/T/L/M/B/S/D/C/P). Son del
  research pack externo, marcado EXTERNAL_UNVERIFIED. Las filas completadas en esta slice re-anclan cada
  afirmacion al corpus REAL del repo (F03, P01, P02, P03, T07). Las 16 filas heredadas conservan sus
  citas del pack SIN verificar: ratificar su estado C0 no ratifica sus citas."
owner: "CEO (ratificacion) / efficacy-guardian + psychometrician (dictamenes)"
```

### 20.4.2 · Tarea (2) — Ratificacion del par de tracers

```yaml
id: RAT-FOCUS-S2-TRACERS
date: 2026-07-20
status: APPROVED
decision: "Par de tracers RATIFICADO tal y como lo proponen, de forma independiente y coincidente, el plan
  (:263), el portfolio (:158, marcado en tabla en :24 y :30) y el dictamen de eficacia (02:95-96):
    TRACER A = FOC2-E07 «Filtro vectorial» — CONSTRAIN/inhibicion, flanker, eleccion L/R, deadline generoso, 3-5 m.
    TRACER B = FOC2-E01 «Centinela»        — SUSTAIN/sostenida, vigilancia, single-tap, ISI largos, targets escasos, 3-5 m."
rationale: "Seis criterios (`EXERCISE_PORTFOLIO_V5.md:158-164`; `02:97-101`): (1) modalidad de respuesta
  distinta (eleccion L/R vs single-tap go-only); (2) estructura temporal distinta (trials discretos densos vs
  monitorizacion continua dispersa); (3) estructura de trial distinta (ventana de respuesta obligatoria vs NO
  responder es la norma, con falsas alarmas y decremento); (4) CONFOUNDS DISJUNTOS — A: velocidad +
  difference-score + proporcion de congruencia; B: criterio + ritmo + estado + decremento — este es el criterio
  que carga el peso: 'un arreglo de una clase no enmascara la otra', de modo que un motor sobreajustado a una
  mecanica muere en el otro tracer; (5) cubre CONSTRAIN + SUSTAIN, las dos facetas mas defendibles; (6) ambos
  ACCEPT accuracy-primary, asi que sobreviven al gate de timebase y nada depende de SSRT ni de dispositivos
  RESEARCH_ONLY."
rejected_pairings: "SUSTAIN+SUSTAIN (no estresa el motor), CONSTRAIN+CONSTRAIN (estructura compartida),
  stop-signal ('bloqueado por timing -> confundiria arquitectura-ok con medida-bloqueada-shippeada')."
non_claims_VERBATIM_from_dictamen: >
  «Que NO demuestra el par: factor Focus, M0-M5, transferencia, validez de RT, RESTRAIN; con alpha ~20, nada
  psicometrico.» (agent_outputs/02_efficacy_guardian.md:104)
dissent_recorded:
  - "El veredicto del dictamen es CONDICIONAL, no APTO limpio (02:133-137). Se ratifica el par CON su condicion."
  - "El par cubre 2 de 3 facetas y CERO RESTRAIN. Un motor sobreajustado a freno motor NO lo caza este par.
     Punto ciego DECLARADO Y ACEPTADO, no un descuido."
  - "Ratificar los tracers NO ratifica como medibles las dos facetas que el dictamen degrada: control
     atencional (CONTRADICTED como subscore) ni estabilidad bajo tiempo (CONTRADICTED como capacidad separada)."
  - "El scoring del ancestro v1 de E07 esta RETIRADO: `EXERCISE_PORTFOLIO_V5.md:89` — 'v1 usa
     CONGRUENCY_EFFECT gobernante -> RETIRADO' — mientras el ancestro (`03_1_Enfoque.md:119,:128`) lo hace
     metrica primaria y construye su formula sobre el. EL GENERADOR ES REUTILIZABLE; EL EJE DE MEDIDA SE
     RE-DERIVA DESDE CERO."
blockers_that_remain_open: "B-1 sin difference-score/SSRT como score; B-2 control atencional y estabilidad sin
  subscore, pendientes de ADR; B-3 la ruta de RESTRAIN al perfil exige ADR y MOT/RSVP/stop siguen
  RESEARCH_ONLY; B-4 RT fail-closed hasta timebase fisico; B-5 modelo de bloque + M0-M5 en beta; B-6 sondas
  retenidas disenadas ANTES de entrenar. Todos ellos gatean SCORE/PERFIL, explicitamente NO S3."
owner: "CEO (ratificacion) / efficacy-guardian (dictamen)"
```

### 20.4.3 · Tarea (3) — Ratificacion de las reglas transversales

> **Aviso de alcance.** El portfolio tiene **DIEZ** reglas transversales (`EXERCISE_PORTFOLIO_V5.md:168-184`).
> El plan nombra CUATRO. Se ratifican esas cuatro y se deja constancia de que las otras seis EXISTEN y NO
> quedan ratificadas por omision. Ninguna de las cuatro estaba ratificada antes en este log: las cuatro son
> NUEVAS (`CAT-006` es adyacente pero no cubre ninguna).

```yaml
id: RAT-FOCUS-S2-RULES
date: 2026-07-20
status: APPROVED
rule_1_difference_scores:
  verbatim: "Primario SIEMPRE exactitud/d'/criterio POR CONDICION; difference-scores solo descriptivos (R1/H2)."
  source: "EXERCISE_PORTFOLIO_V5.md:170, extendida portfolio-wide por la regla 9 (:178-180)"
  rationale_in_document: "E013 Hedge, 'paradoja de fiabilidad' (:7-9)."
  retired_six: "CONGRUENCY_EFFECT, SET_SIZE_SLOPE, SWITCH_COST, DUAL_TASK_COST, PRESSURE_DELTA, SSRT."
  what_it_changes_in_code: "Las seis se computan y emiten HOY (`DefaultMetricsEngine.kt:288-300, :303-313,
    :316-324, :341-351, :364-376, :379-410`). La ratificacion NO las borra: siguen siendo legales como salidas
    DESCRIPTIVAS. Crea UNA barrera: ningun ejercicio de Focus v2 puede nombrar ninguna de las seis como metrica
    gobernante/primaria, y el scorer v2 no puede leerlas. Enforcement: un desktopTest sobre el registro de
    ejercicios v2 (dato estructurado -> test, NO scanner de texto Gradle, learning 2026-06-28) MAS la garantia
    estructural de que el tipo de entrada del scorer candidato no contiene un MetricSet, asi que no PUEDE leerlas."
rule_2_rt_secundario:
  verbatim: "RT secundario en C0 en TODO el portfolio; oficial solo >=T2 (ADR-FOCUS-TIMING)."
  source: "EXERCISE_PORTFOLIO_V5.md:171"
  rationale_in_document: "NINGUNO — puntero a ADR-FOCUS-TIMING (sin decidir, vence en S5). Fragmentos de
    apoyo: E07 'contaminacion de Velocidad (-> RT y exactitud SEPARADOS)' (:83-84); E13 'no importar
    Velocidad' (:120)."
  what_it_changes_in_code: "RT debe ser ESTRUCTURALMENTE incapaz de entrar en el estadistico primario, no
    excluido por convencion. La entrada del scorer se parte: el calculo de exactitud/SDT no recibe campo de
    timing; el timing vive en un tipo aparte que la ruta primaria nunca destructura. Un solo mecanismo
    satisface la regla 2 y la asercion A5."
rule_4_sondas_retenidas:
  verbatim: "Formas paralelas + bancos training/measurement separados + sondas retenidas disenadas ANTES de
    entrenar (R5)."
  source: "EXERCISE_PORTFOLIO_V5.md:173; restated como blocker B-6 (02:137)"
  rationale_in_document: "NINGUNO — puntero a R5."
  what_it_changes_in_code: "CERO codigo nuevo en S2. El repo ya tiene la forma: `ProfileUpdatingBanks.all` es
    un registro opt-in VACIO por defecto y `BankIntegrityAuthority.integrityFor` cae fail-closed a
    SMALL_UNVALIDATED para bancos no registrados (learning 2026-07-05). Ratificarla significa que un tracer de
    S3 que shippee un banco compartido NO PUEDE registrarse, y por tanto no puede producir evidencia elegible
    para perfil: la ruta fail-closed existente ya impone la consecuencia."
rule_5_no_lexico:
  verbatim: "Estimulos no-lexicos en el nucleo (02 §2.3); ES-first solo en copy."
  source: "EXERCISE_PORTFOLIO_V5.md:174; reforzada per-ejercicio solo en E09 (:98, :32)"
  rationale_in_document: "NINGUNO — puntero a 02 §2.3."
  what_it_changes_in_code: "CERO codigo en S2. Solo E09 esta en riesgo material (un Stroop que se lee como
    palabras es lexico por construccion). El enforcement pertenece al CONTENIDO, no al scoring: los generadores
    v2 emiten un tipo marcado o el validador de banco rechaza items lexicos. ALCANCE: la regla es de Focus v2
    UNICAMENTE — los bancos de Lenguaje son lexicos POR DISENO y no quedan afectados. Sin esta acotacion, la
    regla se leeria como una prohibicion de repo entero."
not_ratified_by_omission: "Las otras SEIS reglas transversales del documento (:168-184) NO quedan ratificadas
  por esta entrada. Que no se nombren aqui no las aprueba."
owner: "CEO (ratificacion)"
```

---

## 20.5 Decisiones de Focus Engine V5 · Slice S5 REOPEN (calidad temporal) — 2026-07-21

```yaml
id: ADR-FOCUS-TEMPORAL-QUALITY
date: 2026-07-21
status: PROVISIONAL
problem: "S5 exige instrumentar la calidad temporal por ventana y decidir cómo degrada la evidencia sin fabricar ni destruir señal (contrato de reapertura S5, S5-03..S5-08)."
decision: "Exclusión proporcional por ventana (trial) × elegibilidad por métrica: carve-out de truncación (la accuracy solo se retira en buckets EXCLUDED_*); RT solo sombra sobre CLEAN con excludedFraction adjunta; buckets CLEAN/DEGRADED_USABLE/NO_OBSERVATION/EXCLUDED_*; clases T0-T3 con T2/T3 sin constructor en S5; celdas (condición × bucket) de conteos ENTEROS (jamás series); NotCaptured como tercer veredicto ante ausencia; composición AND de 5 inputs → SessionEvidenceState con OFFICIAL_ELIGIBLE inconstructible; down-weight fraccional RECHAZADO (rompe la distribución muestral de var(d′)); umbrales en config versionada EN SOMBRA; todos los campos nuevos en write-ahead efímero (S5 no crea registro permanente)."
rationale: "Funde los 4 dictámenes del gate de entrada (eficacia/psicómetra/privacy APTO-CONDICIONAL, arquitecto APTO) con el protocolo orden-2 (05_mobile_performance §C) y las decisiones cerradas del contrato de reapertura. El riesgo de primer orden de v1 es la ACCURACY mediada por timing (ventana armada en el dispatch, no en el onset visual), no el RT (que no es evidencia y ni se retiene)."
evidence:
  - "docs/engines/focus_v5/S5_REOPEN_STATUS.md §4/§6/§8"
  - "docs/engines/focus_v5/agent_outputs/05_mobile_performance.md §C/§E"
confidence: "alta en estructura; nula en valores de umbral (DESIGNED_NOT_RUN hasta validación física, Paso 6)"
consequences:
  - "S5-P3 instrumenta sin cambiar ningún veredicto oficial (sombra; fórmulas intactas)"
  - "iOS: port CADisplayLink en S5 (techo T1 con límite epistémico declarado); rama sin-marcador conservadora"
  - "gate de resurrección de mindboost_focus2 declarado (SubjectRevocationAuthority o cierre CONDITIONAL con owner)"
migration: "descriptor/snapshot SCHEMA_VERSION 1→2; N-1 ⇒ NotCaptured (jamás se inventa calidad buena ni mala); v1 ante blob v2 ⇒ rechazo fail-closed tipado"
rollback: "los campos nuevos dejan de emitirse (write-ahead efímero); focus2/domain/evidence y tipos core/timing son aditivos y borrables sin tocar v1"
supersedes: []
documents:
  - "docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md"
  - "docs/engines/focus_v5/ADR_REGISTER_V5.md (fila FROZEN_FOR_S5)"
tests_or_gates:
  - "10 simulaciones adversarias con oráculo exacto (Paso 4)"
  - "property-test de monotonía (3 cláusulas) + arnés anti-vacuidad"
  - "criterios de aceptación §17 del ADR"
  - "gates en serie desktopTest/detekt/compileAndroidMain con ejecución real demostrada"
owner: "sesión principal (mandato del contrato de reapertura S5); CEO: inventario de dispositivos (Paso 6) y ratificación en el cierre de S5"
```

## 20.6 Decisiones de Focus Engine V5 · Slice S5 REOPEN R2→R3 (falsación externa) — 2026-07-22

```yaml
id: ADR-FOCUS-TEMPORAL-QUALITY
revision: R3
date: 2026-07-22
status: REOPENED_AFTER_EXTERNAL_AUDIT  # R3 = CANDIDATA (no APPROVED); techo hasta auditoría externa
supersedes: ["09_DECISION_LOG.md §20.5 (ADR-FOCUS-TEMPORAL-QUALITY PROVISIONAL, R2)"]
trigger: "Auditoría externa (ChatGPT) sobre la R2. Rol confirmado = Claude implementador produciendo la R3 candidata (diseño/pruebas/reconciliación; SIN implementación dependiente)."
falsification:
  dominates: true
  label: VERIFIED_PRIMARY
  claim: "El gate de elegibilidad temporal R2 §6.1 (S_full-o-Absent con monotonicGuard mg-1) es NO-MONÓTONO bajo el scorer real."
  counterexample: "30H/10M/10FA/30CR (loglinear Hautus, Probit Acklam, Z_95=1.959963984540054; valores del scorer real a 6 dp, triple-verificados por 3 probits independientes): D1 (excluir 5 misses) → d′ 1.680016 → Absent; D2 (excluir 5 misses + 15 hits; señal 20 = MinSampleGate.DPRIME, exclusión 25% < 50% backstop) → d′ 1.292908 ≤ S_full 1.310847 → Present (margen 0.017939). Más exclusión técnica ABRE el gate que menos exclusión cerraba; CI-lower(S_full)>0 hace la elegibilidad dependiente del rendimiento (sesgo de selección longitudinal)."
  consequence: "El mecanismo se ELIMINA como gate de elegibilidad."
decisions:
  D-R3-01: "La elegibilidad TEMPORAL es OUTCOME-INDEPENDENT: función solo de hechos técnicos pre-outcome (capability, coverage/report-loss, continuidad de reloj, cadencia esperada/observada, integridad de ventana, versión, blueprint/modalidad). NUNCA de H/M/FA/CR, accuracy, d′, CI-lower>0 ni de si excluir ayuda/perjudica al score."
  D-R3-02: "S5 NO certifica accuracy ni RT temporal. S_full/S_adm/excluded-fraction existen solo en SOMBRA; estado de evidencia temporal = TRAINING_ONLY/NotCaptured; OFFICIAL_ELIGIBLE inconstructible. S6 monta e instrumenta pero NO desbloquea medición."
  D-R3-03: "Sin «commit real» por FrameMetrics: fuera ADR/plan/S6 «commit real»/«onset real»/«desbloquea rt−D por API»/«exactitud por NANOS_NATIVE». Nombres epistémicos (FRAME_ISSUED_PROXY). La futura elegibilidad RT se basa en validatedEndToEndErrorBound + asociación correcta + validación física, no en la unidad/nivel de API del timestamp."
  D-R3-04: "Retención coherente con drain-not-adopt: residuo write-ahead en cold start = intento abandonado ⇒ purga-antes-de-leer, idempotente, SIN MAX_ABANDONED_SESSION_AGE_HOURS ni wall-clock. PRECISIÓN (no confundir diseño con estado cableado): account-deletion está CABLEADO hoy (AccountBound + clearNamespace en el eraser de AppGraph) y por sí solo cubre el borrado inmediato del residuo focus2; subject-revocation cableada a los stores de focus2 (SecureFocusSessionStore/SecureFocusConsentStore vs SubjectRevocationAuthority) es IMPLEMENTATION = PENDING (GAP-1/GAP-2, MB-FOCUS-0048 OPEN). Hasta cablearla, el borrado inmediato de un sujeto revocado-pero-no-eliminado-de-cuenta depende ÚNICAMENTE de account-deletion. La política de retención (drain-not-adopt) no depende de ese wiring."
  D-P7-A: "CUSTODIA del bundle V5 = Opción D (procedencia versionada + cuarentena del legado). El manifest sellado se conserva intacto; los 2 birth-defects (GATE_CATALOG, EXERCISE_PORTFOLIO) se marcan VOID_PROVENANCE en CURRENT_AUTHORITY_INDEX.md; el contenido vivo sigue VIGENTE. Re-sellado fiel = cierre real de S5."
  D-P7-B: "DISCOVERABILITY del índice de autoridad = ADR_REGISTER + CLAUDE.md + scanner. CURRENT_AUTHORITY_INDEX.md es referenciado por un marcador máquina en la cabecera de ADR_REGISTER_V5.md y en CLAUDE.md §6; scripts/validate-authority-entrypoints.mjs verifica la discoverability (self-test + run real). FOCUS_ENGINE_MASTER_PLAN_V5 NO es entrypoint (permanece en denylist)."
gate_result: "Falsación de la R2 = VERIFIED_PRIMARY (domina). Los veredictos por-lente del gate 6/6 (entrada) + 7/7 (salida) NO se conservan en este árbol (tracker scratchpad efímero) ⇒ «7/7 APTO» = MISSING_PRIMARY_EVIDENCE; NO se afirma. La R3 se entrega CANDIDATA a auditoría externa (la autoridad que aprueba)."
scope: "doc-only; ninguna implementación dependiente avanza; techo = CANDIDATA hasta auditoría externa; NO se emite FROZEN_FOR_S5_R3."
findings: ["MB-FOCUS-0047 (falsación del gate, OPEN)", "MB-FOCUS-0048 (inflación de estado R2 + wiring PENDING, OPEN)"]
evidence:
  - "docs/engines/focus_v5/S5_REOPEN_STATUS.md §0"
  - "docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md (+.json)"
  - "docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md · PROVENANCE_INCIDENT_V5_BUNDLE.md"
  - "MINDBOOST_AUDIT_LEDGER.md (validador VERDE tras reseal)"
documents:
  - "docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md (R3)"
  - "docs/engines/focus_v5/ADR_REGISTER_V5.md (fila REOPENED_AFTER_EXTERNAL_AUDIT / R3 CANDIDATA)"
owner: "sesión principal (mandato del contrato de reapertura S5); CEO ratifica en el cierre real de S5 + inventario de dispositivos (Paso 6)"
```

---

## 21. Registro de cambios

### S5 REOPEN (calidad temporal) — 2026-07-21

- Añade §20.5 con **ADR-FOCUS-TEMPORAL-QUALITY** (PROVISIONAL; contrato de diseño FROZEN_FOR_S5, umbrales
  sombra DESIGNED_NOT_RUN). Autoridad: contrato de reapertura S5 (avance autónomo) + gate de entrada 4/4.
- Cambio de **documentación** (registro de decisión), NO de producto ni de código: no cablea nada a perfil
  vivo / Global Score / rango / UI / Firestore / cloud. El CEO debe espejar en su master canónico si aplica.

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
