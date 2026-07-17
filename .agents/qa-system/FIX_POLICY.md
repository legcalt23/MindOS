# Política de reparación no arquitectónica

## Índice

1. Clasificación previa
2. Reparaciones permitidas
3. Reparaciones prohibidas
4. Calidad del parche
5. Validación de la reparación
6. Escalado

## 1. Clasificación previa

Antes de editar, registrar:

- Defecto y severidad.
- Requisito violado.
- Reproducción mínima.
- Causa raíz demostrada.
- Archivos/símbolos implicados.
- Contratos que no deben cambiar.
- Test que detecta el defecto.
- Por qué la reparación está dentro de autoridad.

No parchear un síntoma si la causa sigue siendo una conjetura.

## 2. Reparaciones permitidas

Siempre que preserven arquitectura y contratos:

- Imports, tipos, nullability y errores de compilación locales.
- Manejo correcto de lifecycle, estado Compose, claves, navegación y recomposición.
- Cancelación/concurrencia, race conditions y dispatchers dentro del diseño existente.
- Serialización/mapping que implemente el esquema aprobado.
- Reintentos/idempotencia ya definidos pero implementados incorrectamente.
- Persistencia local o sincronización defectuosa sin cambiar su contrato.
- Errores de cálculo que contradigan la fórmula versionada.
- Estados loading/error/empty y feedback de red existentes.
- Accesibilidad, clipping, contraste, truncado y layout conforme al diseño.
- Rendimiento local mediante eliminación de trabajo redundante, bloqueo de main thread o recomposición innecesaria sin rediseñar capas.
- Tests/fixtures faltantes para requisitos existentes.
- Logs de diagnóstico seguros y acotados.

## 3. Reparaciones prohibidas

- Cambiar pesos/fórmulas/umbrales/mapping de rango.
- Cambiar APIs públicas, contratos DTO, esquema o rutas cloud.
- Reorganizar módulos/capas o sustituir frameworks.
- Actualizar dependencias mayores “porque podría arreglarlo”.
- Modificar Auth, Firestore/Storage Rules, App Check, secretos o permisos.
- Cambiar entitlement, precios o flujo de compra.
- Eliminar datos incompatibles en vez de migrarlos.
- Capturar/ignorar errores para silenciar logs.
- Desactivar analytics/Crashlytics para obtener una ejecución limpia.
- Reducir precisión de mediciones o redondear para ocultar discrepancias.
- Aumentar timeouts/sleeps sin demostrar la causa.
- Reescribir código no relacionado o hacer refactor cosmético.
- Cambiar textos científicos/claims/límites clínicos sin decisión aprobada.

## 4. Calidad del parche

Cada parche debe ser:

- Mínimo: toca solo lo necesario.
- Causal: corrige la causa reproducida.
- Determinista: no depende de timing accidental.
- Compatible: preserva datos/versiones/plataformas.
- Observable: el fallo queda detectado por una prueba.
- Reversible: diff legible, sin mezclar cambios ajenos.
- Seguro: sin secretos, debug bypasses o configuración de producción alterada.

No añadir abstracciones para un solo uso salvo patrón ya establecido. Seguir estilo, arquitectura y convenciones existentes, no preferencias del agente.

## 5. Validación de la reparación

Secuencia obligatoria:

1. Guardar evidencia del fallo.
2. Confirmar que el test/aserción falla por la causa correcta.
3. Aplicar un parche pequeño.
4. Compilar el módulo.
5. Ejecutar test focal.
6. Repetir la reproducción original.
7. Ejecutar tests de consumidores/transitivos.
8. Repetir plataforma/device aplicable.
9. Revisar logs y performance del mismo caso.
10. Inspeccionar diff por cambios accidentales.

Si un test solo pasa cuando se ejecuta aislado, existe contaminación/flakiness y la reparación no está validada.

## 6. Escalado

Clasificar y no aplicar:

- `ARCHITECTURE_DECISION_REQUIRED`: límites, APIs, esquema, estrategia de sync, dependencia fundamental.
- `SCORING_DECISION_REQUIRED`: fórmula, guard, normalización, rango, rounding semántico.
- `SECURITY_REVIEW_REQUIRED`: Auth, aislamiento, rules, secretos, permisos, PII.
- `PRODUCT_DECISION_REQUIRED`: criterio de aceptación contradictorio o comportamiento no especificado.
- `PLATFORM_ENVIRONMENT_REQUIRED`: falta Mac/device/SDK/credencial necesaria.

El informe debe contener: decisión exacta, alternativas mínimas, evidencia y área/owner recomendado. No implementar una alternativa por defecto.
