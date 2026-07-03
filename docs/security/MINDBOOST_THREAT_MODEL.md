# MindBoost — Threat Model

Fecha: 2026-06-20  
Alcance: MindBoost KMP y dependencias directas de integración, Android, iOS/Desktop actuals, Firebase compartido, CI/CD y agentes MindBoost.  
Fuente superior: `docs/security/THREAT_MODEL.md`.

## Arquitectura resumida

MindBoost ejecuta ejercicios y evaluaciones en `commonMain`, usa implementaciones `actual` por plataforma y persiste datos cognitivos localmente. Android es el target de lanzamiento con mayor cobertura: Keystore para datos sensibles, Firebase para identidad/integraciones compartidas y Crashlytics para observabilidad. iOS y Desktop contienen actuals de desarrollo que no deben considerarse publicables sin un gate específico. Los agentes locales bajo `.claude/agents/mindboost-*.md` pueden leer o modificar el workspace y algunos pueden usar Bash.

## Activos críticos

- Identidad, UID y estado de sesión.
- Resultados cognitivos, baselines, progreso, trials y métricas temporales.
- Integridad del scoring, adaptación y recomendaciones.
- Claves de cifrado y datos locales persistidos.
- Adjuntos y publicaciones que cruzan interfaces compartidas con Firebase.
- Diagnósticos, crash reports y evidencias locales de QA.
- Credenciales del desarrollador y autoridad de herramientas/agentes.

## Entradas no confiables

- Texto, respuestas, tiempos y acciones del usuario.
- Datos remotos de Firebase y errores devueltos por SDK/backend.
- JSON/CSV persistido o importado, parámetros de navegación y metadatos de archivos.
- Contenido del repositorio, comentarios, documentación y output de herramientas leído por agentes.
- Dependencias, plugins, workflows y artefactos de build.

## Trust boundaries

1. UI y eventos de plataforma → dominio/scoring KMP.
2. Dominio → almacenamiento local cifrado o fallbacks de plataforma.
3. Cliente autenticado → Firebase Auth, Functions, Firestore y Storage.
4. Excepciones SDK/backend → observabilidad → Crashlytics.
5. Datos de medición → exportación debug → filesystem/evidencias QA.
6. Contenido no confiable → agente → herramientas de escritura/Bash.
7. `commonMain` → implementaciones Android/iOS/Desktop.
8. Código/dependencias → CI → artefacto release y firma.

## Abuse cases prioritarios

- Forzar un fallo autenticado cuyo mensaje contenga PII y lograr que se conserve en telemetría.
- Copiar exportaciones debug o capturas con UID, email o métricas cognitivas fuera del almacenamiento privado.
- Inyectar instrucciones persistentes en archivos revisados por un agente con Bash/escritura.
- Publicar iOS/Desktop con storage, timebase o exportadores que siguen siendo de desarrollo.
- Manipular estado local, serialización o métricas para contaminar progreso y recomendaciones.
- Reutilizar identificadores controlados por cliente contra colecciones o rutas de otro usuario.
- Obtener un artefacto release con firma/configuración debug o dependencia alterada.

## STRIDE

| Categoría | Riesgo principal | Controles observados | Gap/residual |
|---|---|---|---|
| Spoofing | Identidad o autoría controlada por cliente | Auth y mutaciones sociales autoritativas en callable | Validar siempre la identidad canónica backend en nuevas interfaces |
| Tampering | Manipulación de progreso, trials o scoring local | Tipos cerrados, rangos acotados, storage cifrado Android | Integridad local no equivale a autoridad server-side |
| Repudiation | Operaciones sensibles sin evidencia útil | Eventos estructurados y logs minimizados | Probar ACL/retención real de telemetría |
| Information disclosure | PII/cognición en errores o exports | Telemetría tipada y exportación pseudonimizada tras hardening | Evidencias antiguas bajo `validation/` requieren retención y limpieza manual |
| Denial of service | Payloads, loops o sesiones no acotadas | Validaciones, timeouts y límites en rutas revisadas | Mantener límites en futuras APIs/IA |
| Elevation of privilege | Agentes o clientes amplían su autoridad | Reglas Firebase, callable autoritativa, guardrails de agentes | Reducir herramientas por rol cuando sea posible |

## Riesgos específicos

### IA y agentes

- Prompt injection directa/indirecta desde archivos y tool output.
- Abuso de Bash/escritura, lectura de secretos y expansión de alcance.
- Persistencia de instrucciones maliciosas en prompts o memoria.
- Mezcla de datos personales/cognitivos en contexto de modelos futuros.

Controles: boundary explícita en los 16 agentes MindBoost con herramientas consecuenciales y retirada de dos permisos shell wildcard. Antes de integrar IA de producto se requiere aislamiento por usuario, clasificación/minimización de datos, tool allowlists y tests de inyección.

### KMP

- Divergencia de controles entre actuals.
- Serialización compatible pero insegura o tolerante a corrupción.
- Fallbacks Desktop/iOS usados accidentalmente en producción.
- Coroutines que capturen o registren datos al fallar.

### Android

- Exposición por backups, logs, componentes exportados, cleartext o variantes debug.
- Uso incorrecto de Keystore o artefactos release firmados como debug.
- Crashlytics como frontera de tercero.

### iOS

- Falta de evidencia de Keychain/Data Protection/ATS y timebase autoritativo para release.
- Logs o filesystem sin política equivalente a Android.

### Backend/cloud

- Broken access control, IDOR, falta de validación server-side y ausencia de rate limit en nuevas callables.
- Reglas Firestore/Storage divergentes de la autoridad backend.
- No se identificó una API remota exclusiva de MindBoost en el snapshot; las interfaces compartidas siguen sujetas al threat model global.

### CI/CD y supply chain

- Workflows con permisos excesivos, acciones sin pinning, secretos persistentes y builds no reproducibles.
- El release debe fallar cerrado sin firma válida y conservar evidencia de gates.

## Controles existentes relevantes

- Cifrado versionado Android Keystore y fallos de lectura tipados.
- Mutaciones sociales autoritativas y reglas Firebase endurecidas.
- Firma release sin fallback debug.
- Timeouts de red y validación de payloads en interfaces revisadas.
- Logging de release minimizado y telemetría remota estructurada.
- Tests KMP, reglas Firebase y gates de release existentes.

## Controles faltantes o manuales

- Canary no productivo que confirme contenido, ACL y retención real de Crashlytics.
- Gate que declare explícitamente qué targets son publicables; iOS/Desktop permanecen fuera.
- Política ejecutable de retención/borrado para `validation/` y otras evidencias locales.
- Escaneo continuo de prompt/tool boundaries y permisos locales wildcard.
- Evidencia de secret scanning, SBOM/dependency review y aprobación de release para el commit exacto.

## Priorización inicial

- P0: cualquier bypass de authz, secreto o build release inseguro; ninguno quedó abierto en este scan.
- P1: telemetría cruda y exportaciones con identidad estable; corregidos.
- P1: publicar iOS/Desktop sin controles equivalentes; gate abierto.
- P2: validación real de Crashlytics y retención de evidencias.
- P2: reducir herramientas de agentes por rol y automatizar checks.

