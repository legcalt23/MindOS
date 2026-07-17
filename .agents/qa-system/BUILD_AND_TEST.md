# Compilación y pruebas automatizadas

## Índice

1. Descubrimiento
2. Política de comandos
3. Matriz de compilación
4. Capas de prueba
5. Intensidad por riesgo
6. Integridad de tests
7. Evidencia

## 1. Descubrimiento

No ejecutar comandos de plantilla hasta confirmar la estructura real. Inspeccionar:

- `settings.gradle`, `settings.gradle.kts` y proyectos incluidos.
- `build.gradle(.kts)`, convention plugins y version catalogs.
- `gradlew`/`gradlew.bat` y `gradle.properties`.
- Targets/source sets KMP y configuración Compose.
- Proyectos/workspaces Xcode y scripts iOS.
- `firebase.json`, `.firebaserc`, emulators y Test Lab.
- Configuración de tests, cobertura, screenshot tests y Journeys.
- CI vigente: sus comandos son candidatos fuertes, no verdad automática.

Enumerar tareas con el wrapper del repositorio. Elegir la variante que corresponda al sistema operativo. No usar un Gradle global si existe wrapper.

## 2. Política de comandos

- Registrar comando exacto, cwd, variables no secretas, inicio/fin, exit code y rutas de reportes.
- Ejecutar primero la tarea más estrecha que reproduce; cerrar con las tareas amplias afectadas.
- No añadir `--continue` en gates finales.
- No ocultar output ni redirigir errores a `/dev/null`.
- No usar `--offline` salvo que el escenario lo exija y dependencias estén cacheadas.
- No actualizar dependencias, Gradle, Kotlin, AGP, Xcode o Firebase automáticamente.
- Si una descarga de dependencia es necesaria y permitida por el entorno, registrar versión y origen.
- Separar fallo de código, fallo de test, fallo de infraestructura y fallo por dependencia externa.

## 3. Matriz de compilación

### Siempre

- Compilación del source set/módulo afectado.
- `check` o equivalente descubierto para módulos afectados.
- Ensamblado de una app Android instalable de prueba.
- Compilación de tests.

### Cambio en `commonMain` o contrato compartido

- Compilar todos los targets soportados alcanzados.
- Ejecutar `commonTest` y tests por target disponibles.
- Compilar Android.
- Compilar/enlazar iOS en macOS o runner macOS adecuado.
- Si iOS aplicable no puede ejecutarse, resultado final `FAIL — IOS_NOT_VERIFIED`; no declarar compatibilidad por inferencia.

### Cambio Android-only

- Variante debug de prueba y, cuando configuración/obfuscación pueda alterar conducta, variante release no publicada.
- API 26 y una API Android estable reciente, más cualquier configuración explícita del proyecto.

### Cambio iOS-only

- iOS 15 y una versión estable reciente soportada, en simulador o dispositivo compatible.

### Dependencias/configuración/build logic

- Build limpio focal.
- Resolución de dependencias.
- Compilación de todos los consumidores.
- Verificación de reproducibilidad en una segunda ejecución sin cambios.

Descubrir los nombres exactos; ejemplos como `build`, `check`, `allTests`, `assembleDebug` o `linkDebugFrameworkIosSimulatorArm64` no autorizan asumir que existen.

## 4. Capas de prueba

### Unitarias

- Lógica pura, reducers/state, use cases, scoring, validadores, mappers y serialización.
- Fake clock y fake random para tiempo/aleatoriedad.
- Dispatchers de prueba; no sleeps.
- Casos nominales, límites, inválidos y errores.

### Contrato

- DTO ↔ dominio ↔ almacenamiento sin pérdida.
- Compatibilidad de versiones.
- Nullability, defaults y campos desconocidos.
- Igualdad semántica entre local y cloud.

### Integración

- Repositorio local + remoto simulado/emulador.
- Auth de prueba y aislamiento por usuario sin tocar producción.
- Escritura, lectura, reintento, conflicto, cancelación y process death.
- Compra/entitlement solo en sandbox o mock aprobado.

### UI/instrumentación

- Semantics/test tags estables cuando ya formen parte del diseño de test.
- Navegación, estados loading/error/empty/content.
- Recreación de Activity/scene, background/foreground y process death aplicable.
- Entradas rápidas/repetidas para detectar doble acción.

### End-to-end

- Ejecutar el flujo de usuario afectado sobre build instalado.
- Confirmar efecto persistido, no solo feedback visual.
- Encadenar precondición y consecuencia: por ejemplo, completar sesión → reabrir → historial/score idénticos.

## 5. Intensidad por riesgo

- `CRITICAL`: pruebas deterministas repetidas al menos 5 veces; recorridos E2E al menos 3 veces por configuración crítica; dos configuraciones de dispositivo/plataforma aplicables; una ejecución desde proceso y datos fríos.
- `HIGH`: suite focal repetida 3 veces; recorrido E2E 2 veces; dos configuraciones cuando el layout/plataforma pueda variar.
- `MEDIUM`: suite focal y recorrido una vez, más regresión contenedora.
- `LOW`: validación específica y build si existe cualquier efecto ejecutable.

Repetir no sustituye diversidad de casos. Una repetición que usa el mismo estado contaminado no cuenta.

## 6. Integridad de tests

Prohibido:

- Borrar/deshabilitar un test fallido.
- Cambiar el expected para igualar el bug sin fuente de verdad.
- Reducir tolerancias o cobertura sin justificación aprobada.
- Añadir retries automáticos que oculten flakiness.
- Introducir delays fijos para resolver carreras.
- Mockear la unidad que se pretende validar.
- Capturar una excepción y considerar éxito.

Un test nuevo debe fallar contra la conducta defectuosa y pasar con el parche. Cuando no sea viable ejecutar el estado anterior, explicar cómo se demostró la sensibilidad del test mediante mutación controlada o fixture equivalente, sin dejar la mutación en el código final.

## 7. Evidencia

Conservar:

- Output completo o reporte nativo.
- XML/HTML/coverage generado.
- Lista de casos y estados.
- Duración y repeticiones.
- Stack traces completos redactados.
- Hash del APK/AAB/framework/app probado.
- Relación prueba ↔ requisito en `traceability.md`.

Un build exitoso no demuestra funcionalidad. Un test exitoso no demuestra que se ejecutó el binario correcto: correlacionar hashes y timestamps.
