# Verificación de la preparación EXER

## Alcance y método

La verificación estructural comprueba archivos, hashes, frontmatter y enlaces locales. La revisión de conducta es una **pasada secuencial del agente principal**, no una invocación nueva del cliente ni una certificación independiente. Las pruebas de la app que cita la auditoría no se volvieron a ejecutar.

Las tareas reales `/root/exer_science_sources` y `/root/exer_ops_sources` se lanzaron para lecturas independientes. La primera confirmó lectura íntegra de originales 00–05; ambas terminaron por límite de uso. No existe veredicto final independiente en esta sesión. Se completó localmente la lectura pendiente 06–10, módulos y plantillas. El manifiesto conserva esta diferencia.

## Comprobaciones ejecutadas

| Comprobación | Resultado y alcance |
|---|---|
| Comparación con ZIP mediante `zipfile` y SHA‑256 | 45/45 miembros idénticos byte a byte; incluye las once fuentes originales |
| `verificar_paquete.py` | PASS: 45 originales intactos, frontmatter válido y 89 enlaces locales comprobados fuera de fuentes históricas. Salida final en [RESULTADOS_CHECKS.json](RESULTADOS_CHECKS.json) |
| `quick_validate.py` de skill-creator | No ejecutable en el Python bundled por `ModuleNotFoundError: yaml`; no se instaló dependencia. El comprobador local verifica el frontmatter simple presente, sin atribuirse toda la cobertura del validador oficial |
| `node scripts/validate-audit-ledger.mjs` | PASS antes y después del checkpoint: 337 findings, hash consistente; filas previas preservadas, sin transiciones de estado |
| `npm run check:governance` | FAIL reproducido por `CEO_FIELD_DISAGREEMENT` y valores GRANTED/NOT_GRANTED; no se silenció el gate |
| `node scripts/memoryctl.mjs` | OK con warning de grafo STALE (`8a06b9ac` frente a `bdc252f4`); no prueba que Graphify ejecute consultas |
| `codex --version` / `--help` | CLI 0.137.0; no acredita el registro de `/exer` ni la versión desktop |
| `graphify --help` | Falla el trampoline; Python configurado ausente. No se consultó ni regeneró el índice |
| Control de cambios previos | PASS: 542 archivos previos con cambios permanecen idénticos al snapshot. Retirar el bloque nuevo de `AGENTS.md` y el enlace con sus separadores del índice recupera sus hashes previos; el ledger conserva sus filas. [Control](CONTROL_CAMBIOS_PREVIOS.json) y resultado de checks |
| `git diff --check` acotado | PASS para los archivos documentales de esta tarea; no prueba el contenido de archivos untracked, cubierto por el verificador del paquete |

Los enlaces de `contexto/fuentes/` son históricos y pueden referir a materiales no incluidos; se preservan y no se presentan como validados. No se verificaron URLs científicas ni anchors Markdown en este pase. La bibliografía del ZIP conserva su propio alcance de acceso, sin nueva ratificación científica.

## Revisión secuencial frente a escenarios del paquete

| Caso | Resultado observado en esta preparación | Límite |
|---|---|---|
| T02: conversación frente a auditoría | Conversación original clasificada AUDIT_PROMPT; informe recibido clasificado AUDIT_RESULTS | Clasificación documental |
| T04–T05: cinco sesiones y madurez | Se conserva 5/3/48 más los demás requisitos; se señala que tres administraciones no cumplen el contrato completo y que el mínimo no prueba eficacia | Análisis de mesa, no test del motor |
| T06–T07: contexto obsoleto y capacidad | Entradas antes ausentes se reconcilian con repositorio actual; tests antiguos siguen REPORTED, no nuevo TEST_VERIFIED | Comprobación focalizada, no auditoría completa |
| T08 y T10: cambio pequeño/autoridad | Políticas y alternativas quedan PROPOSED; no se modificó charter, flag ni campo CEO | No se simuló una aprobación de producto |
| T18: recepción de auditoría | Handoff separa estado actual y vNext, y no crea o reescribe un catálogo | No se inspeccionaron todos los dossiers históricos |
| T19 y T21: capacidad real de herramientas | Se registraron límites de agentes y fallo de Graphify; no se inventó revisión independiente ni instalación nativa | Las consultas de grafo siguen pendientes |
| T23: órdenes históricas | La preparación conserva el alcance de diseño; no ejecuta el prompt histórico `/mb` | Integridad de originales comprobada |
| T26: activación | Alias conversacional aprobado explícitamente y añadido a `AGENTS.md`; no se declara `/exer` operativo por tener Markdown | Invocación real NOT_RUN |
| T27: continuidad | Checkpoint, hashes y preguntas pendientes permiten recuperar el estado | Segunda invocación real NOT_RUN |

T01, T03, T09, T11–T17, T20, T22, T24–T25 y T28–T30 se conservan como pruebas de uso futuro; no se cuentan como ejecutadas de forma independiente. No se afirma «30/30 pruebas pasadas» ni se inventa un fallo basal.

## Disposición

El contenido portable y el análisis de los cinco bloqueos están preparados. El alias aprobado está configurado y debe probarse en el cliente; una prueba final independiente queda pendiente por disponibilidad de agentes. El motor, la evidencia humana y los gates de release mantienen sus estados anteriores.
