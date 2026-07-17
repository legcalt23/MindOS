# Handoff de Claude, baseline e impacto

## Índice

1. Resolución del handoff
2. Resolución del baseline
3. Working tree
4. Mapa de impacto
5. Clasificación de riesgo
6. Contrato opcional de handoff

## 1. Resolución del handoff

Determinar la última funcionalidad entregada usando la primera fuente inequívoca disponible:

1. Documento reciente de handoff que identifique feature, commit y criterios de aceptación.
2. Último commit no validado registrado en el informe QA anterior.
3. Commits de la rama actual posteriores al último `tested_commit` con `PASS`.
4. Diferencia contra el merge-base de la rama por defecto.
5. En primera ejecución y sin historia suficiente: último commit funcional más cambios actuales, siempre que el límite pueda justificarse.

No asumir que `HEAD~1` equivale a una funcionalidad. No usar fecha de modificación como única señal. Si hay varias features mezcladas y no pueden separarse lógicamente, ampliar el alcance a todas y elevar riesgo; si la matriz resulta inabarcable o incoherente, `FAIL — HANDOFF_NOT_ATOMIC`.

Registrar la fuente elegida y por qué las fuentes superiores no aplicaron.

## 2. Resolución del baseline

El baseline debe ser un commit real y alcanzable. Preferencia:

1. `base_commit` explícito del handoff.
2. `tested_commit` del último `PASS` en la misma línea de historia.
3. Padre común anterior al conjunto de commits de la feature.
4. Merge-base con rama por defecto detectada a través de Git, sin asumir `main` o `master`.

Verificar relación ancestro/descendiente. Si el historial fue reescrito, registrar el problema y reconstruir por merge-base; no modificar historia.

El diff de alcance incluye:

- Cambios committed entre baseline y HEAD.
- Cambios staged.
- Cambios unstaged.
- Archivos untracked relevantes.

No incluir por defecto artefactos generados, caches o evidencia `.qa/`, pero confirmar mediante configuración del proyecto y `.gitignore`, no por nombre solamente.

## 3. Working tree

Capturar con formato que preserve nombres especiales. No alterar cambios preexistentes. Etiquetar cada archivo:

- `HANDOFF`: forma parte de la entrega.
- `PREEXISTING`: estaba antes y no pertenece al handoff.
- `QA_ADDED`: test/evidencia/corrección creada durante `/qa`.
- `GENERATED`: salida reproducible de herramientas.
- `UNKNOWN`: origen no demostrado; obliga a prudencia.

Si un archivo `PREEXISTING` se solapa con una reparación, no editarlo hasta demostrar que el cambio puede preservarse. Si no, `FAIL — USER_CHANGE_CONFLICT`.

## 4. Mapa de impacto

Para cada archivo/símbolo modificado, recorrer al menos:

- Callers y callees.
- Interfaces, expect/actual y dependencias KMP.
- Modelos de dominio, DTO, serializers y migraciones.
- Repositorios, caché, Firestore y sincronización.
- ViewModels/state holders, coroutines/flows y lifecycle.
- Navegación y destinos.
- Composables y previews.
- Analytics y eventos.
- Tests existentes y fixtures.
- Android/iOS source sets y configuración de build.

Producir un grafo textual `origen → consumidores → riesgos → pruebas`. No limitar la prueba a archivos modificados cuando exista efecto transitorio.

## 5. Clasificación de riesgo

### CRITICAL

Scoring, calibración, rangos, sesiones, persistencia, sincronización, Auth, aislamiento de usuarios, suscripción/entitlements, IA Coach data access, esquema, migración, concurrencia o código `commonMain` usado por flujos críticos.

Requisitos mínimos: matriz completa, tests de borde/propiedad, Android + iOS aplicable, cold start, process death, offline/reconexión, repetición de estabilidad y observabilidad.

### HIGH

Onboarding, evaluación inicial, motor de ejercicio, resultado, dashboard, navegación central, estado de sesión, performance sensible o UI común multiplataforma.

Requisitos mínimos: tests automatizados focales, dos configuraciones de dispositivo aplicables, recorrido visual, offline smoke y regresión del flujo anterior/posterior.

### MEDIUM

Pantalla secundaria, visualización sin mutar datos, analytics no crítico o feedback textual.

Requisitos mínimos: build, tests focales, recorrido visual, accesibilidad y smoke del flujo contenedor.

### LOW

Documentación o cambio puramente interno sin efecto ejecutable demostrado.

Requisitos mínimos: validación documental/configuración y confirmación de que no afecta build. Un cambio de dependencia nunca es LOW.

Usar el riesgo máximo de cualquier impacto transitorio. No rebajar riesgo para reducir tiempo.

## 6. Contrato opcional de handoff

Si existe `.qa/handoffs/latest.md`, validar estos campos; no confiar en contenido incompleto:

```markdown
# Claude Handoff
- feature_id:
- title:
- base_commit:
- head_commit:
- architecture_decision_refs:
- prd_requirement_refs:
- changed_modules:
- expected_user_flows:
- acceptance_criteria:
- known_limitations:
- tests_claimed:
- migrations:
- data_contract_changes:
- security_sensitive_changes:
```

Las pruebas “claimed” son pistas, no evidencia. `/qa` debe ejecutarlas de nuevo.
