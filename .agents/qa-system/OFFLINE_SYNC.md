# Persistencia, pérdida de red y resincronización

## Índice

1. Modelo observable
2. Entorno seguro
3. Escenarios obligatorios
4. Aserciones de integridad
5. Concurrencia y conflictos
6. Evidencia y puerta

## 1. Modelo observable

Descubrir y documentar la máquina de estados real antes de probar. Como mínimo, identificar equivalentes de:

```text
IN_PROGRESS
COMPLETED_LOCAL_PENDING_SYNC
SYNCING
SYNCED
RETRYABLE_ERROR
PERMANENT_ERROR
ABANDONED
```

No exigir estos nombres literales. Sí exigir que cada transición sea observable y no ambigua. Identificar:

- Punto en que una respuesta se considera aceptada.
- Punto de persistencia local duradera.
- Momento de cálculo de score.
- Clave de idempotencia (`sessionId` u otra aprobada).
- Worker/retry que sincroniza.
- Política de conflicto y orden.
- Transacción/batch remoto.
- Fuente de verdad al reabrir.

Si no existe estrategia identificable para evitar pérdida/duplicado, `FAIL — SYNC_CONTRACT_MISSING`.

## 2. Entorno seguro

- Preferir Firebase Emulator Suite y almacenamiento local aislado.
- Usar usuarios sintéticos únicos por ejecución.
- Confirmar visualmente/configuración que no apunta a producción antes de escribir.
- No cambiar reglas o desactivar Auth para facilitar pruebas.
- No usar PII real.
- Capturar estado local y remoto antes/después con herramientas aprobadas, redactando tokens.
- Controlar reloj cuando caducidad, backoff o timestamps intervengan.

La falta de emulador puede suplirse con dobles de integración y un proyecto explícito de testing, nunca con producción. Si no existe entorno seguro suficiente, `FAIL — SAFE_BACKEND_UNAVAILABLE`.

## 3. Escenarios obligatorios

### N1 — Camino online nominal

Completar sesión con red estable. Confirmar una escritura remota, estado local final `synced`, score/historial correctos y ausencia de reintentos pendientes.

### N2 — Pérdida durante ejercicio

1. Iniciar online.
2. Registrar varias respuestas.
3. Cortar toda conectividad verificando realmente el estado, no solo el icono.
4. Continuar hasta completar.
5. Confirmar que la UI no bloquea el entrenamiento.
6. Confirmar persistencia local antes de recuperar red.

### N3 — Finalización completamente offline

Lanzar o continuar según contrato sin red, completar y salir del resultado. Reabrir sin red. La sesión, score mostrado y estado pendiente deben sobrevivir.

### N4 — Process death antes del resultado

Matar el proceso en un punto controlado después de respuestas significativas y antes de completar. Reabrir y verificar la política aprobada: restaurar o marcar abandonada sin convertirla en válida. Nunca inventar una sesión completada.

### N5 — Process death después de persistencia local y antes de cloud

Completar offline, verificar escritura local, matar proceso, reabrir todavía offline y después reconectar. Debe sincronizar exactamente una vez.

### N6 — Reconexión

Reconectar y observar transición pendiente→syncing→synced. Confirmar:

- Mismo `sessionId`.
- Una sola sesión remota.
- Un solo delta de bloque.
- Un solo incremento de contador/calibración.
- Un solo evento analítico semántico si el contrato lo exige.
- Sin cambio de score por serialización/rounding.

### N7 — Red intermitente

Alternar conectividad al menos tres veces durante subida/reintento. No deben existir loops calientes, batería/CPU descontrolada, duplicados o error permanente prematuro.

### N8 — Timeout y error transitorio

Simular timeout, 5xx/unavailable y cancelación. Verificar backoff/cancelación según arquitectura, feedback no bloqueante y recuperación posterior.

### N9 — Error no reintentable

Simular payload inválido o rechazo permanente dentro del entorno de test. No reintentar infinitamente. Preservar datos locales y mostrar estado accionable sin borrar progreso.

### N10 — Auth expirada

Caducar/inutilizar credencial en entorno de prueba entre persistencia y sync. No asociar datos a otro usuario. Recuperar sesión/Auth según contrato y sincronizar o mantener pendiente de forma segura.

### N11 — Doble acción

Pulsar completar/guardar rápidamente varias veces y provocar recreación/background. Debe existir un único efecto lógico.

### N12 — Orden fuera de secuencia

Crear dos sesiones offline y reconectar con delays invertidos. El historial y score final deben seguir la política aprobada; no aplicar deltas dos veces ni perder la más antigua.

### N13 — Cambio de usuario

Con una sesión pendiente del usuario A, cerrar sesión e iniciar usuario B. B jamás ve ni sincroniza datos de A. No modificar reglas para probarlo. Cualquier fallo se deriva inmediatamente como `FAIL — SECURITY_REVIEW_REQUIRED`.

### N14 — Almacenamiento limitado/corrupto

Cuando las herramientas lo permitan de forma segura, simular error de escritura, cuota o registro inválido. La app debe fallar de forma visible/controlada, nunca informar guardado exitoso sin durabilidad.

## 4. Aserciones de integridad

Por escenario, comparar local y remoto campo a campo para la sesión:

- Identidades y owner.
- Duración/fechas.
- Ejercicio/bloque.
- Contadores de respuestas.
- Métricas de reacción.
- Dificultad.
- Score bruto, normalizado y delta.
- Estado de calibración.
- Versiones de ejercicio/scoring.
- Flags de anomalía.
- Estado de sync.

Además:

- No hay pérdida silenciosa.
- No hay duplicación.
- No hay regresión a un estado anterior después de `SYNCED`.
- Reintentar es idempotente.
- Cancelar una coroutine/tarea no deja transacción parcial.
- Timestamps usan la fuente aprobada y se ordenan de forma estable.
- La UI diferencia guardado local, sincronizando, sincronizado y error cuando sea relevante.

## 5. Concurrencia y conflictos

Si el sistema soporta múltiples dispositivos o writers, probar:

- Dos actualizaciones del mismo perfil con versiones distintas.
- Sesión subida desde un dispositivo mientras otro actualiza historial.
- Lectura stale seguida de escritura.
- Retries concurrentes de la misma sesión.

Aplicar la estrategia existente: transacción, compare-and-set, versión, merge determinista u otra aprobada. Si “last write wins” puede perder progreso y no está aprobado, no cambiarlo dentro de QA: `FAIL — ARCHITECTURE_DECISION_REQUIRED`.

## 6. Evidencia y puerta

Conservar por escenario:

- Timeline UTC con eventos de red, lifecycle, local DB y backend.
- Captura o grabación de la UI.
- Logs correlacionados por run/session ID redactado.
- Snapshots local/remoto antes y después.
- Número de operaciones remotas/reintentos.
- Aserciones automatizadas y resultado.

La puerta offline solo pasa si N2, N5 y N6 se ejecutan siempre para un flujo de entrenamiento afectado, y todos los demás escenarios aplicables al cambio pasan. Un simple test con “modo avión” sin process death ni comparación remota no es suficiente.
