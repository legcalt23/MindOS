# Checkpoint · EXER

## Ejecución vigente · 2026-10-04

**Run:** `TASK-2026-10-04-MOLVEX-DECISIONS-30-MODELOS`.

- Encargo: repetir la estrategia documental para Decisiones, con treinta modelos gráficos para una app iOS + Android.
- [Catálogo](../propuestas/CATALOGO_DECISIONES_30_MODELOS_2026-10-04.md): 30 contratos en cinco zonas, mapa visual, wireframe móvil, cinco fuentes científicas, ejemplos de jugada, señales con denominador, un eje de reto, riesgos, acceso, falsadores y gates.
- `document_completion`: COMPLETE para el dossier conceptual. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY; no cambia identidades, taxonomía, scoring, perfil, runtime, almacenamiento ni publicación.
- Revisión secuencial del autor, no independiente. Sin cambios de código, motor, datos, flags, despliegues, prototipos, builds/tests de app ni piloto humano.
- Estado observado en el working tree sucio: [la ruta raíz](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ui/DecisionsRootScreen.kt) conecta check-in → Bayes Arena → triaje → resultado; [check-in](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/DecisionCheckInStateHolder.kt) genera 20 estímulos por defecto; [Bayes Arena](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/BayesArenaStateHolder.kt) usa cinco escenarios por defecto y ahora recibe `dialMoved`; [triaje](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ReversibilityTriageStateHolder.kt) usa seis tarjetas por defecto. La raíz pasa `resultTimestampMs` como semilla de Bayes y reversibilidad. Son rutas/tamaños leídos, no evidencia de exposición o calidad.
- La señal del check-in es contexto de sesión, no modelo autónomo de práctica. En el resumen Bayes se observó fallback a cero cuando la lista está vacía y falta un gate de suficiencia/exclusión; el resumen de triaje observado no exige muestra mínima separada por clase. Los cambios locales de juego limpio en los dos state holders están sin confirmar y no se validaron con tests.
- El documento E1–E8 es linaje histórico; tres state holders de tarea/contexto son las únicas rutas identificadas en la raíz actual. La ruta iOS, equivalencia Android/iOS, fiabilidad de la señal, exposición, adaptación, duración y resultados humanos quedan `NOT_VERIFIED`.
- Árbol de trabajo preexistente sucio. Graphify no se refrescó: el informe indica `8a06b9ac`, mientras HEAD observado es `bdc252f496384e52e3ac14e451a139df57b47792`.
- Evidencia de práctica de pronósticos y de tareas de competencia en decisiones se usa con límites de población, tarea y formato; no demuestra efectos de estos modelos ni del producto.

## Ejecución anterior · 2026-10-04

**Run:** `TASK-2026-10-04-MOLVEX-CREATIVITY-30-MODELOS`.

- Encargo: repetir la estrategia documental para Creatividad, con treinta modelos gráficos y suficientemente desarrollados para una app iOS + Android.
- [Catálogo](../propuestas/CATALOGO_CREATIVIDAD_30_MODELOS_2026-10-04.md): 30 fichas, seis zonas, mapa de cobertura, wireframe móvil, linaje histórico, compatibilidad observada, ejemplos, medidas descriptivas, un eje por ficha, acceso móvil, privacidad, falsadores y plan de validación.
- `document_completion`: COMPLETE para el dossier conceptual. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY; no cambia taxonomía, IDs, scoring, runtime, almacenamiento ni publicación.
- Revisión secuencial del autor, no independiente. Sin cambios de código, motor, datos, flags, despliegues, prototipos, builds/tests de app ni piloto humano.
- Estado observado en el working tree: dos IDs alcanzables (`EX01_SUPPRESS_FIRST_IDEA`, `EX02_CATEGORY_ROTATION`), ocho prompts estáticos para EX01 y seis temas para EX02; respuesta de texto libre; señales deterministas de cantidad de cadenas distintas y palabras medias por idea. La instrucción de omitir la primera idea y el giro de categoría no quedan verificados por la rúbrica observada.
- El runtime declara desactivadas generación mediante IA, sincronización de red y telemetría remota. Los registros de sesión locales conservan el texto de ideas. Durabilidad/paridad iOS no verificadas.
- Árbol de trabajo preexistente sucio. No se usó como fuente el informe de auditoría local no verificado. Graphify no se refrescó: `GRAPH_REPORT.md` indica `8a06b9ac`, mientras el HEAD observado es `bdc252f496384e52e3ac14e451a139df57b47792`.
- `NOT_VERIFIED`: contenido y banco, comprensión, señal de novedad/calidad semántica, adaptación, consistencia, paridad real iOS/Android, dosis, retención y resultados en tareas/contextos no practicados.

## Ejecución anterior · 2026-10-03

**Run:** `TASK-2026-10-03-MOLVEX-LOGIC-30-MODELOS`.

- Encargo: repetir la estrategia documental para el bloque Lógica, con 30 modelos visuales y suficientemente desarrollados para una app iOS + Android.
- [Catálogo](../propuestas/CATALOGO_LOGICA_30_MODELOS_2026-10-03.md): 30 fichas M01–M30, mapa de cobertura y flujo, trazabilidad de 28 `LogicSkill`, linaje de 10 propuestas históricas, compatibilidad observada, errores/métricas, dificultad de un eje, accesibilidad, falsadores y plan de validación.
- `document_completion`: COMPLETE para el dossier conceptual. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY; no cambia taxonomía, tipos, scoring ni implementación.
- Revisión secuencial del autor, no independiente. Sin cambios de código, motor, scoring, flags, datos, despliegues, prototipos, builds/tests de app ni piloto humano. Graphify no refrescado.
- Estado observado: 28 valores de subhabilidad, cinco tipos de ejercicio y seis ítems fijos en `DefaultLogicContentProvider`. Un tipo o skill existente no equivale a 30 familias implementadas; la sesión observada consume el pack sin generador paramétrico. Árbol de trabajo preexistente sucio; no se atribuye la observación a un baseline limpio.
- `NOT_VERIFIED`: fidelidad de UI/captura, generación y exposición, dificultad efectiva, propiedades psicométricas, scoring en dispositivo, iOS/equivalencia multiplataforma, dosis, eficacia, retención y transferencia. Decisiones de cartera/desarrollo permanecen pendientes.

## Ejecución anterior · 2026-10-03

**Run:** `TASK-2026-10-03-MOLVEX-MEMORY-30-MODELOS`.

- Encargo: repetir la estrategia documental del catálogo de Enfoque para diseñar al menos 30 modelos gráficos de práctica del bloque Memoria en iOS + Android.
- [Catálogo](../propuestas/CATALOGO_MEMORIA_30_MODELOS_2026-10-03.md): 30 modelos M01–M30, mapa visual de cobertura, matriz de linaje con EX1–EX16, contrato móvil común, scoring/error, adaptabilidad, accesibilidad, privacidad, falsadores y plan de validación.
- `document_completion`: COMPLETE para el dossier conceptual. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY; no cambia IDs/taxonomía y no aprueba implementación ni publicación.
- Revisión secuencial del autor, no independiente. Sin cambios de código, motor, scoring, flags, datos, despliegues, prototipos, builds/tests de app, piloto humano ni refresh de Graphify.
- Estado de código contrastado: el catálogo declara 16 identidades; EX1/EX3/EX4 están en la lista implementada y en el registro de generadores observado. La sesión principal observada usa selección de reconocimiento; el recall diferido tiene un flujo separado. Esto no acredita fidelidad completa, integración al perfil, validez psicométrica ni transferencia.
- Permanecen `NOT_VERIFIED`: plataforma iOS/equivalencia, completitud y exposición del banco, eficacia/dosis de las 30 fichas, señal del perfil, transferencia a outcomes no entrenados y decisiones de cartera/CEO.

## Ejecución anterior · 2026-10-02

**Run:** `TASK-2026-10-02-MOLVEX-FOCUS-30-MODELOS`.

- Encargo: diseñar un catálogo gráfico de al menos 30 modelos de ejercicios para el bloque Enfoque, con contratos conceptuales completos para una app móvil iOS + Android.
- [Catálogo](../propuestas/CATALOGO_ENFOQUE_30_MODELOS_2026-10-02.md): 30 modelos M01–M30, mapa visual de cobertura, procedimiento común, reglas de scoring/adaptación, accesibilidad móvil, riesgos de solapamiento, falsadores y contratos futuros de validación.
- `document_completion`: COMPLETE para el dossier solicitado. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY; los modelos son propuestas, no ejercicios implementados ni aprobados para publicación.
- Revisión final secuencial del autor, no independiente. Sin implementación, ejecución de app o tests, cambios de motor, flags, datos, despliegues ni estudio humano.
- Las cinco decisiones CEO pendientes y el estado sucio del árbol impiden afirmar compatibilidad con la autoridad ejecutable actual. iOS, equivalencia física de timing y eficacia de los 30 modelos permanecen `NOT_VERIFIED`.
- Bibliografía usada con alcance acotado: la atención se trata como constructo multifacético; la evidencia de transferencia de brain training es más limitada que la mejora en tareas entrenadas. No se atribuye eficacia al catálogo.

## Ejecución previa · 2026-09-16

**Run:** `TASK-2026-09-16-EXER-MOTOR-ENFOQUE`.

- Encargo: evaluar el motor antes de crear ejercicios y proponer mejoras de nivel de referencia.
- [Propuesta completa](../propuestas/MOTOR_ENFOQUE_VNEXT_2026-09-16.md): revisión focalizada, siete paquetes de contratos, alternativas, gates y decisiones de adopción. No se creó catálogo ni se modificó producto.
- `document_completion`: COMPLETE para propuesta conceptual. `design_disposition`: CONDITIONAL. `readiness`: DESIGN_ONLY.
- `client_activation`: CONVERSATIONAL_INVOCATION_OBSERVED. En esta interacción se recibió `/exer`, se cargó SKILL.md y se aplicaron sus módulos. No es registro nativo ni test completo de comportamiento.
- La instrucción posterior «continua por donde ibas de la mejor manera posible» mantiene el alcance; no aprueba cambios de producto, flags, V5 ni estudios.
- Dependencias previas coinciden con el cierre anterior: 23/26 hashes iniciales iguales y 3/26 iguales a sus hashes finales documentados. [Snapshot](MOTOR_ENFOQUE_SNAPSHOT_2026-09-16.json).
- Revisión técnica y científica por dos agentes reales; revisión integrada y resultados en [comprobaciones](MOTOR_ENFOQUE_CHECKS_2026-09-16.json). No equivalen a revisión humana ni validación empírica.
- Ledger conserva hallazgos y estados. `check:governance` continúa fallando por `0155`; no se resolvió por inferencia. Graphify no actualizado.
- Siguiente acción: resolver adopción de las propuestas específicas de §10 y cerrar contratos acotados antes de cualquier encargo posterior de implementación. No iniciar ejercicios por el mero cierre documental.

## Checkpoint histórico conservado · preparación 2026-09-15

# Checkpoint · EXER

**Run:** `TASK-2026-09-15-MOLVEX-EXER-PREPARACION`.

## Estado

- `document_completion`: COMPLETE para preparación portable e integración focalizada de la auditoría.
- `design_disposition`: CONDITIONAL por prueba de activación pendiente y decisiones de producto sin responder.
- `readiness`: DESIGN_ONLY.
- `client_activation`: CONFIGURED_NOT_VERIFIED; alias conversacional añadido a `AGENTS.md` tras aprobación explícita del usuario el 2026-09-15 («Sí, añade el alias conversacional /exer»). Falta prueba en una nueva interacción.
- `graph_integration`: PARTIAL; memoria Markdown accesible y enlazada, consultas/refresh Graphify no ejecutables.
- `independent_final_use_test`: NOT_PERFORMED; las dos tareas delegadas terminaron por límite de uso.

## Continuidad

1. Conservar 1B/2B/3A/4A: Molvex, personalización individual con robustez poblacional, variante sencilla del mismo entrenamiento serio, núcleo sin IA generativa.
2. No interpretar «CONTINUA» como decisión sobre flags, calibración, C0, `0155` ni estudio humano.
3. Probar el alias conversacional aprobado desde [ENTREGA](ENTREGA.md) y [bloque aplicado](PROPUESTA_ALIAS.md) en una nueva interacción. La elección del mecanismo ya está resuelta.
4. Para cada respuesta CEO, registrar texto/fecha/alcance y actualizar solo la propuesta afectada. El diseño no autoriza implementación ni release.
5. Recuperar [DECISIONES_CEO](DECISIONES_CEO.md): `0166`/`0116`, `0174`, `0168`, `0155`, `0101` siguen pendientes. Verificar el ledger vivo antes de reutilizar el estado.
6. Comparar [snapshot inicial](SNAPSHOT_FUENTES.json) con archivos actuales, aplicando las actualizaciones documentales propias registradas en [resultados](RESULTADOS_CHECKS.json). Si cambia una dependencia, releerla y revisar su consecuencia; no reiniciar los once originales sin motivo.
7. Puede seguir el brief y comparación conceptual acotada de Enfoque. No se inició catálogo ni se creó dossier de ejercicio.

## Lectura y revisión

El agente `/root/exer_science_sources` informó lectura completa de las seis fuentes originales 00–05 antes de su interrupción. El principal leyó 06–10 completos, los 14 módulos y las ocho plantillas, y reabrió las fuentes decisivas de gobernanza/calibración en el repo. El segundo agente no dejó confirmación suficiente de cobertura: no se le atribuye lectura.

El informe de revisión que venía en el ZIP pertenece al autor del paquete. Esta sesión conserva sus afirmaciones como historia; los checks y análisis nuevos constan en [VERIFICACION](VERIFICACION.md).

## Memoria y reversión

El resultado está bajo `docs/exer`; un enlace en el índice de memoria lo hace navegable y `AGENTS.md` contiene el alias aprobado. Los registros de hallazgos previos conservan sus IDs/estados. La tarea queda trazada mediante evento/checkpoint del ledger con sello recomputado.

Para deshacer la preparación documental, retirar solo los archivos creados por esta tarea, su enlace y el bloque `/exer` añadido, preservando la historia append-only del ledger. No restaurar el árbol completo ni revertir cambios previos ajenos. No se mutó ningún servicio remoto.
