# Logcat, Crashlytics y rendimiento

## Índice

1. Preparación
2. Logcat/runtime
3. Crashlytics
4. Rendimiento
5. Presupuestos provisionales
6. Evidencia y puerta

## 1. Preparación

- Usar el mismo build final de los recorridos.
- Limpiar buffers locales de logs antes del caso cuando sea seguro y registrar el momento.
- Añadir un run/session correlation ID solo si puede hacerse sin cambiar semántica ni exponer PII.
- Registrar dispositivo, estado térmico, batería/carga, build type y procesos en segundo plano relevantes.
- Hacer warm-up separado de mediciones; no mezclar cold/warm starts.

## 2. Logcat/runtime

Capturar desde antes del lanzamiento hasta después de persistencia/sync. Buscar y clasificar:

- Fatal exceptions, ANR y tombstones.
- Excepciones no capturadas/capturadas repetitivas.
- StrictMode, bloqueo de main thread e I/O en UI.
- Coroutine cancellation mal gestionada, `JobCancellationException` anómala y leaks de scopes.
- Errores de Compose snapshot/state, recomposición patológica y claves inestables observables.
- OutOfMemory, GC thrash, fugas y crecimiento sostenido.
- Firestore/Auth/Network/serialization/permission errors.
- WorkManager/retry loops.
- Recursos faltantes, layout/rendering y warnings de plataforma relevantes.
- Datos sensibles impresos.

No tratar todo warning como defecto. Correlacionar con el flujo y distinguir ruido conocido documentado. Un warning nuevo repetitivo, un error silencioso o cualquier secreto/PII en logs impide `PASS`.

## 3. Crashlytics

Si Crashlytics está configurado:

1. Confirmar app ID/proyecto de prueba y build/version filtrados.
2. Revisar fatals, non-fatals y ANR del build o ventana de prueba.
3. Correlacionar stack, versión y timestamp con la ejecución.
4. Comprobar que nuevas excepciones controladas no generan tormentas de eventos.
5. Confirmar símbolos/mapping cuando sean necesarios para interpretar.

No provocar un crash en producción. Un crash de prueba deliberado debe ejecutarse solo en entorno aislado y etiquetarse para no confundirlo con regresión.

Si el cambio es HIGH/CRITICAL y Crashlytics aplicable no puede consultarse, `FAIL — CRASHLYTICS_NOT_VERIFIED`. Una lectura limpia de Logcat no sustituye datos remotos.

## 4. Rendimiento

Medir sobre build comparable y entorno estable:

- Cold start y warm start.
- Tiempo desde CTA hasta inicio interactivo del ejercicio.
- Latencia de respuesta visual durante ejercicio.
- Jank/frame timing durante interacción intensa y gráficas/animaciones.
- Tiempo desde completar hasta resultado utilizable.
- Tiempo de persistencia local.
- Tiempo de resincronización tras recuperar red.
- CPU, memoria y crecimiento después de repetir sesiones.
- Red/lecturas/escrituras por sesión cuando sea observable.
- Batería/loops únicamente si el cambio afecta trabajo sostenido.

Preferir herramientas nativas: Macrobenchmark/Baseline Profile cuando existan, profiler, frame metrics, Perfetto, Instruments/XCTest metrics y Firebase Performance configurado. No instalar una nueva plataforma de telemetría dentro de QA.

Comparar con:

1. Presupuesto oficial del repositorio.
2. Baseline commit en mismo dispositivo/build type.
3. Presupuestos provisionales de la sección siguiente, solo si no existe fuente superior.

Ejecutar suficientes muestras: al menos 10 para startup/latencias cortas y 3 sesiones sostenidas para memoria/jank en riesgo HIGH/CRITICAL. Reportar mediana, p95 cuando el tamaño lo permita, dispersión y outliers; no seleccionar la mejor muestra.

## 5. Presupuestos provisionales

Estos límites son guardrails QA, no autorización para cambiar requisitos. Una especificación aprobada los sustituye.

- Cero crashes y cero ANR.
- Ninguna regresión reproducible superior al 10 % frente al baseline en una métrica crítica, salvo variación absoluta trivial documentada.
- Cold start de release de prueba: objetivo ≤ 2.5 s en dispositivo de referencia; si el hardware/emulador invalida el absoluto, usar comparación de baseline.
- CTA→ejercicio interactivo: objetivo ≤ 500 ms con datos locales listos.
- Input→feedback visual durante ejercicio: p95 objetivo ≤ 100 ms.
- Completar→resultado utilizable: objetivo ≤ 1 s cuando no requiere red; la UI no espera cloud.
- Persistencia local de sesión: objetivo ≤ 250 ms sin bloquear main thread.
- Tras red estable, sync pendiente visible inicia ≤ 5 s; el tiempo total depende de backend, pero no puede quedar estancado.
- Jank no aumenta más de 1 punto porcentual ni supera el presupuesto oficial.
- Memoria no presenta crecimiento sostenido después de cinco sesiones equivalentes y retorno al dashboard.

Si un umbral provisional falla pero no existe requisito oficial, registrar `FAIL — PERFORMANCE_BUDGET_REVIEW_REQUIRED`; no degradar el test ni realizar cambios arquitectónicos.

## 6. Evidencia y puerta

Conservar:

- Logs brutos redactados y consulta/filtros usados.
- Export/resumen de Crashlytics con build y ventana temporal.
- Trazas/profiles/reportes nativos.
- Dataset de muestras, unidades y cálculo.
- Comparación baseline↔final en mismo entorno.
- Hallazgos con timestamp y reproducción.

La puerta pasa únicamente con cero crash/ANR, cero errores relevantes sin resolver, sin PII/secrets y presupuestos cumplidos o fuente aprobada que justifique otra expectativa.
