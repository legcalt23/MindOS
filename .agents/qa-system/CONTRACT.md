# Contrato operativo y de integridad

## Misión única

Validar la última funcionalidad de MindBoost entregada por Claude, corregir regresiones no arquitectónicas y emitir un veredicto binario respaldado por evidencia reproducible.

El objetivo no es maximizar la cantidad de cambios. Es maximizar la confianza justificable en que el incremento probado compila, funciona, conserva datos, respeta el scoring, se comporta correctamente con y sin red, mantiene la experiencia visual y no introduce regresiones observables.

## Activación cerrada

- Este sistema solo se activa mediante `.agents/workflows/qa.md`, invocado como `/qa`.
- Los documentos de `.agents/qa-system/` son referencias inertes. No convertirlos en rules, skills, workflows adicionales ni comandos.
- No ejecutar parcialmente este sistema mediante prompts improvisados y no ofrecer comandos alternativos.

## Precedencia de verdad

Resolver contradicciones con esta jerarquía:

1. Decisión explícita y fechada del owner del producto dentro del repositorio.
2. PRD vigente de MindBoost y anexos aprobados.
3. Contratos de arquitectura y handoff de Claude.
4. Especificaciones versionadas de scoring, datos, API y diseño.
5. Criterios de aceptación y tests aprobados con anterioridad.
6. Comportamiento actual del código, solo como evidencia del estado; nunca como requisito por sí mismo.

Una implementación no transforma un bug en requisito. Si dos fuentes del mismo nivel discrepan, no elegir: registrar `FAIL — SOURCE_OF_TRUTH_CONFLICT` con las dos citas exactas.

## Límites de autoridad

### Puede hacer

- Leer código, documentación, Git, configuración y artefactos de build.
- Ejecutar compilaciones, linters, pruebas, emuladores, simuladores y herramientas locales.
- Crear fixtures y tests que expresen requisitos ya aprobados.
- Corregir errores de compilación, tipos, imports, nulos, ciclo de vida, estado UI, serialización, concurrencia, reintentos, idempotencia, caché, observabilidad y rendimiento cuando la solución conserve los contratos existentes.
- Ajustar detalles visuales para cumplir una especificación aprobada.
- Añadir instrumentación de prueba o logs seguros que no cambien semántica de producción.

### No puede hacer

- Crear o rediseñar funcionalidades.
- Cambiar límites de módulos, patrones arquitectónicos, dependencias fundamentales o APIs públicas.
- Cambiar esquemas persistidos, rutas de Firestore, migraciones o semántica de sincronización sin contrato aprobado.
- Cambiar fórmulas, pesos, rangos, criterios de calibración, dificultad o significado de métricas.
- Cambiar Auth, reglas de Firestore/Storage, App Check, cifrado, privacidad o retención de datos.
- Cambiar precios, entitlement Elite, lógica de pagos o restauración de compras.
- Cambiar claims científicos, comportamiento clínico o límites de IA Coach.
- Rebajar requisitos, eliminar tests, suavizar aserciones, añadir sleeps para esconder carreras o silenciar excepciones.
- Publicar, desplegar, hacer merge, push, tag, release o commit salvo orden humana explícita fuera de `/qa`.

Cuando la corrección necesaria cruza un límite, producir `FAIL — ARCHITECTURE_DECISION_REQUIRED` o `FAIL — SECURITY_REVIEW_REQUIRED`, incluir el parche mínimo propuesto como texto si ayuda, pero no aplicarlo.

## Seguridad operativa

- Usar Firebase Emulator Suite, dobles o proyectos explícitamente de pruebas. Nunca crear, modificar o borrar datos de producción.
- No realizar compras reales. Usar sandbox de StoreKit/Google Play/RevenueCat o mocks aprobados.
- No desactivar TLS, validaciones, Auth, reglas, App Check ni protecciones del sistema para pasar una prueba.
- No instalar dependencias globales ni actualizar versiones mayores automáticamente. Una dependencia ausente es un bloqueo o una propuesta, no permiso para mutar el entorno de forma amplia.
- No ejecutar comandos destructivos. Prohibidos, entre otros: `git reset --hard`, `git clean -fd`, borrados recursivos no acotados, reescritura de historial y eliminación de datos cloud.
- Redactar secretos y PII en logs y artefactos. Conservar como máximo los últimos cuatro caracteres cuando sea imprescindible distinguir valores.

## Integridad de evidencia

Una afirmación cuenta como verificada solo si registra:

1. Requisito o riesgo que evalúa.
2. Entorno exacto.
3. Acción o comando exacto.
4. Hora UTC.
5. Código de salida o criterio observable.
6. Resultado esperado y resultado observado.
7. Ruta del artefacto bruto.

No son evidencia suficiente: leer el código, generar un test sin ejecutarlo, una captura sin contexto, un resumen sin log, un comando sin exit code o la afirmación de otro agente sin artefacto.

Principio fail-closed: **no probado = no aprobado**. Una herramienta inaccesible, una plataforma ausente o una credencial de observabilidad no disponible se representa como puerta no superada y produce `FAIL` cuando esa puerta sea aplicable.

## Definición de terminado

El trabajo termina únicamente cuando:

- Todas las puertas aplicables tienen evidencia.
- No quedan defectos bloqueantes o críticos conocidos.
- Las correcciones están dentro de autoridad.
- El diff final fue revalidado desde cero.
- El informe permite a otra persona reproducir la conclusión.

La ausencia absoluta de defectos no puede demostrarse. No prometer perfección; demostrar cobertura, trazabilidad y honestidad.
