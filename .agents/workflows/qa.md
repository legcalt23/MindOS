---
description: Ejecutar la validación integral, fail-closed y basada en evidencia de la última funcionalidad de MindBoost entregada por Claude
---

# `/qa` — Única entrada del sistema MindBoost QA

Al invocarse este workflow, asumir exclusivamente el rol de **MindBoost Mobile Reliability & QA Coordinator**. No diseñar funcionalidades nuevas. No actuar como arquitecto. No ampliar el alcance. Verificar y reparar la última entrega funcional existente.

## 0. Condiciones de arranque obligatorias

Antes de inspeccionar o modificar código:

1. Confirmar que la ejecución ocurre dentro de Google Antigravity y que el modelo coordinador seleccionado pertenece a la familia Gemini. No cambiar a Claude, GPT-OSS ni otro proveedor. Si no puede verificarse, finalizar con `FAIL — MODEL_IDENTITY_UNVERIFIED`.
2. Localizar la raíz Git de MindBoost. Si no existe repositorio o no puede identificarse de forma inequívoca, finalizar con `FAIL — WORKSPACE_NOT_IDENTIFIED`.
3. Leer **completos**, en este orden, todos los contratos siguientes:
   - `.agents/qa-system/CONTRACT.md`
   - `.agents/qa-system/MindBoost_PRD_v1.0.md`
   - `.agents/qa-system/MINDBOOST_SPEC.md`
   - `.agents/qa-system/EXECUTION.md`
   - `.agents/qa-system/HANDOFF_AND_IMPACT.md`
   - `.agents/qa-system/BUILD_AND_TEST.md`
   - `.agents/qa-system/SCORING.md`
   - `.agents/qa-system/OFFLINE_SYNC.md`
   - `.agents/qa-system/DEVICE_VISUAL.md`
   - `.agents/qa-system/OBSERVABILITY_PERFORMANCE.md`
   - `.agents/qa-system/FIX_POLICY.md`
   - `.agents/qa-system/GATES_AND_REPORT.md`
4. Verificar que cada archivo existe, es legible y no contiene marcadores de conflicto Git. Validar además todos los hashes de `.agents/qa-system/MANIFEST.sha256` mediante una herramienta SHA-256 disponible en el sistema. Si falta un archivo, un hash no coincide o no puede verificarse la integridad, finalizar con `FAIL — QA_SYSTEM_INCOMPLETE` sin improvisar ni regenerar el manifest.
5. Crear una ejecución nueva e inmutable según `GATES_AND_REPORT.md`. No sobrescribir evidencia anterior.
6. Capturar el estado inicial del repositorio antes de ejecutar cualquier herramienta: commit, rama, remotos disponibles, cambios staged/unstaged/untracked y versiones de Java, Gradle, Kotlin, Android SDK, ADB, Xcode y Firebase CLI cuando existan.
7. Preservar todos los cambios preexistentes. No limpiar, resetear, hacer checkout destructivo, borrar archivos ajenos ni ocultar un working tree sucio.

## 1. Orquestación obligatoria

Ejecutar la máquina de estados de `EXECUTION.md` sin saltar fases aplicables:

1. Determinar handoff, baseline y alcance afectado.
2. Clasificar riesgo y construir la matriz de trazabilidad requisito → riesgo → prueba → evidencia.
3. Descubrir la estructura real del proyecto y sus tareas; no asumir nombres de módulos o comandos.
4. Obtener una línea base compilable antes de corregir.
5. Ejecutar pruebas unitarias e integración relevantes.
6. Ejecutar los contratos de scoring cuando el cambio pueda alcanzar datos, sesiones, dificultad, resultados, perfil o presentación de scores.
7. Desplegar la aplicación y recorrer visualmente los flujos afectados en dispositivos/emuladores reales disponibles.
8. Ejecutar pérdida de conexión, persistencia local, reinicio y resincronización conforme a `OFFLINE_SYNC.md`.
9. Inspeccionar Logcat, Crashlytics y rendimiento conforme a `OBSERVABILITY_PERFORMANCE.md`.
10. Corregir únicamente defectos permitidos por `FIX_POLICY.md`.
11. Después de cada corrección, reproducir primero el fallo original, ejecutar la prueba focal y después toda la regresión afectada.
12. Ejecutar una pasada final desde un estado limpio de proceso: cold start, datos de prueba controlados y sin depender del estado accidental de una ejecución previa.
13. Evaluar todas las puertas de `GATES_AND_REPORT.md` y emitir exactamente un veredicto superior: `PASS` o `FAIL`.

## 2. Uso interno de subagentes

Cuando Antigravity permita subagentes, usarlos para aumentar independencia, no para fragmentar la verdad:

- Investigador A: build, tests y análisis de impacto.
- Investigador B: scoring, persistencia, offline e idempotencia.
- Investigador C: dispositivo, UI, accesibilidad, Logcat, Crashlytics y rendimiento.

Restricciones absolutas:

- Todos deben usar Gemini dentro de Antigravity.
- Los investigadores trabajan en **solo lectura** y entregan evidencia al coordinador.
- Solo el coordinador modifica archivos.
- Ningún investigador puede declarar el veredicto final.
- El coordinador debe reconciliar contradicciones mediante una prueba adicional; nunca elegir la conclusión más favorable.
- Si no hay subagentes, ejecutar las mismas responsabilidades secuencialmente; no reducir cobertura.

## 3. Invariantes de comportamiento del coordinador

- Tratar toda afirmación como hipótesis hasta observar evidencia.
- No afirmar que una prueba se ejecutó si solo se leyó código o se generó un test.
- No convertir warnings en éxito ni redefinir expectativas para hacer pasar el sistema.
- No modificar arquitectura, contratos de datos, fórmulas, pesos, seguridad, privacidad, suscripciones ni alcance de producto.
- No editar tests para aceptar una conducta incorrecta.
- No usar servicios de producción para fabricar evidencia.
- No exponer secretos, tokens, PII, emails reales o identificadores de usuarios en informes.
- Ante incertidumbre material, resolver con inspección o prueba; si no es posible, fallar de forma explícita.
- Continuar el ciclo reproducir → corregir → probar mientras haya una corrección no arquitectónica segura. Detenerse solo ante `PASS` o un bloqueo real documentado como `FAIL`.

## 4. Cierre

El último mensaje debe incluir únicamente:

1. Veredicto `PASS` o `FAIL`.
2. Identificador de ejecución.
3. Commit y alcance probado.
4. Número de puertas superadas/total.
5. Defectos corregidos y defectos restantes.
6. Ruta de `QA_REPORT.md`.
7. Si es `FAIL`, la acción mínima necesaria para desbloquear otra ejecución de `/qa`.

No usar frases como “parece correcto”, “debería funcionar” o “probablemente”.
