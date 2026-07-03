# MindBoost — Execution Kickoff (handoff a la sesión de implementación)

**Propósito:** este documento es el punto de entrada único para la sesión que IMPLEMENTA el plan de entrenamiento cognitivo de MindBoost. La fase de diseño está cerrada; aquí empieza la construcción. Léelo entero antes de tocar código.

---

## 0. Misión

Implementar el sistema de entrenamiento cognitivo de MindBoost hasta el MVP, siguiendo el plan maestro sin improvisar. El estándar es: **cero P0 y cero P1 de integridad** (pérdida de datos, señal fabricada, UX engañosa, claims, privacidad) en la superficie publicada, con honestidad de medición como principio rector. No es una app de juegos cerebrales: es un entrenador cognitivo serio, medible y científicamente prudente.

## 1. Lectura obligatoria (en este orden, completa)

1. `docs/plans/mindboost_cognitive_training_master_plan.md` — el plan (22 secciones; §3 decisiones DC-1..DC-20, §8 rediseño de los 8 motores, §17 fases).
2. `docs/plans/mindboost_scoring_system_v1.md` — la matemática de scoring de dos capas (fórmulas por bloque, gates, versionado).
3. `docs/plans/mindboost_mvp_training_acceptance_criteria.md` — el gate B1–B10 y los criterios de "apto MVP".
4. `docs/plans/mindboost_science_and_claims_guardrails.md` — claims permitidos/prohibidos y guardrails por bloque.
5. `docs/plans/mindboost_15_exercises_per_block.md` — las 123 fichas de ejercicio (27 campos c/u); es tu especificación por ejercicio.
6. `docs/plans/mindboost_implementation_backlog.md` — P0/P1, 104 micro-mejoras, riesgos, tests, mapa a fases.
7. `docs/audits/mindboost_exercises_audit.md` — el estado real que motiva todo (referencia).
8. `CLAUDE.md` (sección "Aprendizajes acumulados del proyecto") — restricciones activas del repo; **aplícalas como reglas duras**.

## 2. Restricciones no negociables (resumen operativo del plan)

- **Scoring de dos capas (DC-1):** capa interna `ScoreResult` (`CALIBRATING/EMERGING/ESTABLISHED`, `normalizedScore 0–1|null` — nunca 0 fabricado) es la fuente de verdad; capa usuario 0–100 = `UserScoreMapper` puro y versionado, gateado por DC-2 (número visible solo si ESTABLISHED, o EMERGING con confidence≥0.55 y ≥2 sesiones cross-session). La UI muestra "—", jamás 0.
- **Local-first cifrado (DC-6):** todos los datos cognitivos por `SecureKeyValueStore` (patrón AES-256-GCM + Keystore ya establecido). NO introducir Room/SQLDelight/Firestore para datos de entrenamiento. Todo namespace nuevo entra en `AccountBound` + eraser + `AccountBoundErasureCoverageTest` **en el mismo cambio**.
- **Honestidad de claims (DC-20):** claim único "entrenas y mides X dentro de MindBoost contra tu propia línea base"; prohibido IQ/clínico/transferencia garantizada/percentiles/rasgos. Todo copy pasa el `mindboostCopySafetyScanner` (3 variantes: commonMain/commonTest/desktopTest); el lexicon prohibido no se nombra ni negado.
- **No romper los bloques verdes:** Enfoque y Emociones se homogeneizan SOLO en frontera (adapters aditivos → `ScoreResult`); **golden/characterization tests ANTES** de cualquier cambio; nunca scorer y wiring en el mismo PR.
- **Anti-gaming demostrado, no declarado:** reclamar `DetectorCoverage.ADEQUATE` exige suite red-team (adversario dispara / honesto realista no dispara).
- **Abandono jamás alimenta el perfil;** velocidad sin precisión jamás suma; sesiones inválidas se excluyen con estado explícito.

## 3. Reglas de trabajo en este repo (de CLAUDE.md / memoria — obligatorias)

- **Fases pequeñas.** Slices verticales (lógica commonMain + datos + UI de una pieza completa), no capas horizontales. Tras cada slice: tests + Detekt + verificación de compilación. No avanzar si un gate falla.
- **Gradle:** si el IDE (Android Studio) posee el daemon en este worktree, **no invocar Gradle CLI**; verificar estáticamente y **entregar los comandos de gate al usuario** para que los corra. Tareas reales del módulo: `:shared:compileAndroidMain` (Android de shared; NO `compileDebugKotlinAndroid`), `:shared:compileKotlinDesktop`, `:shared:desktopTest`, `:shared:detekt`, `:androidApp:compileDebugKotlin`. No ejecutar tareas Gradle en paralelo sobre el mismo worktree. Evitar `clean`/`--stop` salvo petición.
- **Ramas:** ningún trabajo sobre `main`/`master`; worktree por tarea (Vibe Kanban). No ofrecer commit/push/PR salvo que el usuario lo pida.
- **Detekt es ratchet:** archivos nuevos con findings rompen el build. Orden: arreglar `UnusedParameter`/`UnusedPrivateProperty` (suelen ser bugs), refactorizar `ReturnCount`≤2 / `MaxLineLength`=120 / `LongMethod`, y solo entonces `@Suppress` localizado con comentario para lo idiomático (Composables, `MagicNumber` en tokens/estadística). Nunca regenerar el baseline para silenciar. `const val` top-level en SCREAMING_SNAKE_CASE.
- **kotlinx-datetime 0.7.1:** no llamar `Clock.System.now()` en dominio/presentación; inyectar `EpochClock`/`AppClock`. Un `ProfileSource` no usa `SystemEpochClock` por defecto (rompe desktopTest).
- **Protocolo post-error (obligatorio):** cada vez que corrijas un error real (compilación, crash, navegación, imports, Gradle, Kotlin, Compose, Firebase, permisos, arquitectura, estado, integración, UI, suposición previa), **añade una entrada a la sección "Aprendizajes acumulados del proyecto" de `CLAUDE.md`** con el formato exacto (Error / Causa raíz / Solución / Regla futura / Archivos).
- **Reportes largos** (auditorías, análisis) van a archivos markdown en el repo; el chat solo lleva updates breves.
- **Verificación end-to-end del perfil:** todo `ProfileSource` nuevo se prueba a través de `MindBoostProfileAggregator` (no solo la contribución aislada — un test aislado enmascara el descarte por `minSufficiency`).

## 4. Orquestación con agentes `/mb`

Usa el orquestador `/mb` (`.claude/commands/mb.md`) para slices que crucen ciencia + scoring + motor + datos + UI. Enrutado por especialidad: `efficacy-guardian` + `psychometrician` como gate de ENTRADA (qué se mide y cómo) y de SALIDA (validez), `cognitive-engine-dev` (motor commonMain), `data-sync-dev` (persistencia), `compose-ui-dev` (UI), `test-engineer` (tests, especialmente la matemática del scoring), `privacy-security` (si toca datos de usuario), `diff-auditor` (revisión en contexto fresco). Regla: máximo 2–3 agentes concurrentes; si uno devuelve vacío, reintentar solo esa lente. Gate de salida no negociable: `efficacy-guardian` + `diff-auditor` antes de dar algo por terminado.

## 5. Orden de ejecución — empieza aquí

Sigue las fases del plan maestro §17. **No empieces contenido (Fase 3+) antes de cerrar Fases 0–2: son integridad, no features.**

### FASE 0 — Seguridad y P0 (haz esto primero, en este orden)
1. **Commit del árbol sin versionar** (P0 operacional): `git status` muestra ~40% de `mindboost/` untracked (`core/{scoring,antigaming,bank,timing,copy}`, `creativity/`…). Commitéalo antes de editar nada (rama de trabajo, no main).
2. **Decisiones — persistencia (B1):** en `DecisionsRootScreen.kt` (~línea 76) `onSealed = { _, _ -> onExit() }` descarta `DecisionSession` + `DiaryEntry`. Escribe primero el test rojo (sellar→reabrir→diario visible), instancia `SecureDiaryRepository`/`SecureDecisionSessionRepository` en el grafo y conecta `onSealed`→`save()` antes de `onExit()`. Verde.
3. **Decisiones — Reversibility (B2):** arranca YA (camino crítico humano) la validación inter-rater del banco de 30 escenarios (≥2 raters, κ≥0.75). Cuando valide: `BankIntegrity.PROCEDURAL`, retira `SMALL_UNVALIDATED`, `includeReversibility=true`, conecta su `ScoreResult` al perfil vía `ProfileUpdateGate`.
4. **Gate de display (B6):** property tests de `UserScoreMapper`/`BrainProfileUiModelBuilder` (CALIBRATING→nunca número; DC-2). Si aún no existe `UserScoreMapper`, este punto se solapa con Fase 1.
5. **Copy honesto (B5):** Creatividad (hub/`CreativityStrings` a "retroalimentación funcional, sin nota ni rango" + KDoc `CreativitySessionCoordinator.kt:44`) y Velocidad (explicar el gate). Scanner verde 3 variantes.

**Criterio de cierre de Fase 0:** B1, B2, B3, B5, B6 cerrados; `git status` limpio de untracked en `core/`; suites verdes.

### FASES 1–7
Continúa con Fase 1 (scoring dos capas + Lenguaje/Velocidad/Bayes al perfil), Fase 2 (SessionRecord + abandono + analytics consent-first), Fase 3 (bloques débiles a cuota MVP), Fase 4 (evaluación inicial), Fase 5 (hacia 15/bloque), Fase 6 (QA), Fase 7 (freeze + PRD v2). Cada fase con su objetivo/tareas/archivos/dependencias/riesgos/aceptación/agente revisor está detallada en el plan maestro §17 y desglosada en el backlog §6.

## 6. Definición de "hecho" por tarea

Una tarea está hecha cuando: (a) el slice cruza lógica+datos+UI y es alcanzable end-to-end desde la app; (b) tiene tests (unit del scorer/generador con input→output conocido + adversarial del anti-gaming + consistency catálogo↔registry si añade ejercicio + round-trip de persistencia); (c) `:shared:detekt`, `:shared:desktopTest` y `:androidApp:compileDebugKotlin` verdes (comandos entregados al usuario si el IDE tiene el daemon); (d) copy-scanner verde en las 3 variantes; (e) `efficacy-guardian` + `diff-auditor` firman en contexto fresco; (f) si tocó datos de usuario, `privacy-security` firma; (g) `CLAUDE.md` actualizado si se corrigió un error real.

## 7. Qué NO hacer

No reescribir los motores nativos de Enfoque/Emociones. No fabricar un score sin gate. No introducir Firestore/Room/SQLDelight para entrenamiento. No lanzar iOS sin store durable (Keychain, DC-18). No nombrar lexemas clínicos ni negados. No inflar el inventario (15/bloque es diseñado; el MVP es 41, con suelo de contingencia etiquetado honestamente). No ejecutar Gradle CLI si el IDE tiene el daemon. No trabajar sobre main.
