# Ejemplo — Directo (B01): cambio local trivial

**Entrada:** `/ceo renombra el botón "Empezar" a "Entrenar ahora" en la home de Enfoque`

**Modo:** Directo (total riesgo ~2; 1 disciplina, local, reversible). Sin DAG, sin agentes, sin integrador.

---

## Interpretación
Cambio de copy en un Composable. Debe pasar el copy-safety scanner (sin léxico prohibido) y compilar.

## Contexto (mínimo)
- `HECHO` El hub/pantallas de Enfoque viven en `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/presentation/ui/` (source: REPO_CONTEXT.md).
- `INFERENCIA` "Entrenar ahora" no contiene lexemas prohibidos; lo confirma el scanner.

## Prompt (único)
```text
Actúa como Staff KMP Engineer de Molvex (código MindBoost, com.mindos.app).
Objetivo: cambiar el texto del botón "Empezar" a "Entrenar ahora" en la home de Enfoque.
Antes de editar: localiza el string real con Grep en shared/.../features/mindboost/focus/presentation/ui/ (puede estar en AppStrings o inline).
Trabajo: edita solo ese literal. No toques lógica, estado ni otros textos.
Restricciones: sin léxico clínico/IQ (debe pasar mindboostCopySafetyScanner); Detekt ratchet (no regeneres baseline).
Aceptación: el botón muestra "Entrenar ahora"; nada más cambia.
Verificación: :shared:compileKotlinDesktop + mindboostCopySafetyScanner. Si Android Studio posee el daemon Gradle, entrégame esos comandos en vez de ejecutarlos.
Entrega: archivo(s) del working-tree tocados. Sin commit/PR.
```

## Gates
`:shared:compileKotlinDesktop` · `mindboostCopySafetyScanner`.

## Riesgos
1) String duplicado en otra pantalla → Grep confirma alcance. 2) Literal en scanner de copy → texto neutro, sin riesgo. 3) Daemon del IDE → entregar comandos, no ejecutar CLI.

> Nota de proporcionalidad: aquí NO hay DAG, ni validator, ni subagentes. Meterlos sería sobreingeniería (el linter avisaría `overengineered_direct`).
