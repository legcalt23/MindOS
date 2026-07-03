# MindBoost CEO Prompt Engineering Protocol

`/ceo` transforma lenguaje humano en prompts de grado-ejecución para Claude Code, aterrizados en el
repo real de MindOS (ver `REPO_CONTEXT.md`).

## Pipeline de transformación

1. Decodifica el objetivo real de producto/negocio (no el literal).
2. Identifica la capa: producto, UX, arquitectura, implementación, tests, analítica, growth,
   monetización o launch.
3. Elimina lenguaje vago.
4. Añade restricciones concretas (entorno + rigor charter).
5. Añade pasos de descubrimiento del repo (archivos concretos del feature).
6. Añade fronteras KMP (commonMain vs expect/actual; módulos reales).
7. Añade criterios de aceptación observables.
8. Añade verificación con **tareas Gradle reales** (o instrucción de entregarlas al usuario).
9. Añade non-goals.
10. Añade formato de entrega final (working-tree, sin commit/PR).

## Anti-patrones a eliminar

Sustituye prompts como:

- "Hazlo mejor." · "Mejora la app." · "Crea algo brutal." · "Optimiza el onboarding." · "Añade
  gamificación." · "Hazlo premium."

Por prompts como:

- "Rediseña el onboarding para que en <90s el usuario complete baseline, entienda su plan diario y
  llegue a la primera sesión sin fricción — en `shared/.../features/mindboost/**`, respetando el
  hub `MindBoostHubScreen`."
- "Implementa un motor de dificultad adaptativa compartido en `commonMain` que ajuste nivel por
  precisión, tiempo de reacción y fatiga estimada, con score honesto (`null≠0`) y tests en
  `desktopTest` con casos input→output conocidos."
- "Crea un dashboard de progreso que muestre tendencia por dominio cognitivo y muestre '—' cuando la
  dimensión esté `CALIBRATING`; sin claims clínicos (pasa `mindboostCopySafetyScanner`)."

## Componentes obligatorios de un buen prompt

Rol · contexto de producto · **contexto del repo real** (módulos/paquete/target de tests) ·
objetivo · instrucciones de descubrimiento (archivos concretos) · restricciones estratégicas ·
restricciones de honestidad (rigor charter) · restricciones técnicas (KMP + Detekt ratchet) ·
secuencia de implementación por fases · criterios de aceptación · **verificación con gates reales**
· non-goals · formato de entrega (working-tree, sin commit; entrada de aprendizaje si se corrigió un
error).

## Escalado CEO (cuándo preguntar)

Cuando la petición esté sub-especificada, **no bloquees** salvo que sea imprescindible: haz
supuestos explícitos y avanza. Pregunta solo si la respuesta cambia toda la dirección:

- Plataforma ambigua.
- Petición de claim médico/clínico.
- Dependencia de assets, APIs o decisiones de negocio inexistentes.
- Riesgo de privacidad/legal que no se puede asumir con seguridad.

## Enrutado (aprovecha lo que ya existe)

Cuando encaje, el prompt puede delegar a los especialistas del repo en el orden del orquestador
`/mb`: `efficacy-guardian` + `psychometrician` (gate de entrada: qué se mide y cómo) →
`cognitive-engine-dev` / `data-sync-dev` → `compose-ui-dev` (+ `motion-experience` solo si piden
pulido) → `test-engineer` → gate de salida `efficacy-guardian` + `diff-auditor` (+ `privacy-security`
si tocó datos de usuario). Ver `REPO_CONTEXT.md`.
