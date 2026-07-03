# CLAUDE.md

> **Flujo con agentes / Vibe Kanban:** antes de operar, lee
> [`docs/AI_WORKFLOW.md`](docs/AI_WORKFLOW.md) (reglas de edición, ramas, commits, build/test,
> checklist pre-merge) y [`docs/VIBE_KANBAN_SETUP.md`](docs/VIBE_KANBAN_SETUP.md) (entorno local).
> Ningún agente trabaja sobre `main`/`master`: Vibe Kanban usa un worktree por tarea.

> **Toolkit Claude Code (port curado de ECC):** este repo incluye skills, subagentes,
> slash-commands, reglas por lenguaje y un guard ligero en `.claude/` — adaptados al stack de
> MindOS (KMP/Compose/iOS/Firebase/Functions). Índice y uso en
> [`.claude/README.md`](.claude/README.md).
> Atajos: `/kotlin-build` · `/gradle-build` · `/kotlin-review` · `/kotlin-test` · `/plan` ·
> `/quality-gate` · `/refactor-clean` · `/security-scan`. Subagentes para review/build de
> Kotlin/Swift/TypeScript, seguridad, arquitectura y *silent-failure-hunter*. Las skills de
> stack se auto-activan por contexto; no requieren invocación manual.

## Context Navigation: Graphify Memory Graph

Este repo tiene una **memoria estructural persistente** en `graphify-out/` (grafo de conocimiento
AST del código, generado localmente y sin coste de LLM). Úsala como PRIMER recurso para orientarte,
en lugar de releer el repositorio entero.

**Regla núcleo:** si existe `graphify-out/graph.json`, NO releas todo el repo al empezar una
conversación ni hagas búsquedas amplias con Grep/Glob/Read a ciegas. Orienta primero con el grafo.

**Antes de un Grep/Glob/Read masivo, consulta primero (en este orden):**
1. `graphify query "<pregunta>"` — devuelve un subgrafo acotado (módulos, símbolos, llamadas,
   dependencias, comunidades y rutas relevantes). Es mucho más barato que grepear o que leer
   `graph.json` crudo. Flags: `--dfs` (traza una ruta concreta), `--budget N` (limita tokens).
2. `graphify explain "<Nodo>"` — explicación en lenguaje llano de un nodo y sus vecinos.
3. `graphify path "<A>" "<B>"` — camino más corto entre dos conceptos/símbolos.
4. `graphify affected "<X>"` — traversal inverso: qué nodos se ven IMPACTADOS por X (ideal para
   evaluar el impacto de un cambio antes de hacerlo).
5. `graphify-out/GRAPH_REPORT.md` — resumen legible de arquitectura, hubs de comunidad y frescura
   del grafo. Léelo para una visión amplia cuando query/explain/path/affected no basten.

> No leas `graphify-out/graph.json` en crudo: son ~40 MB. Consúltalo SIEMPRE a través de los
> comandos `graphify query|explain|path|affected` (operan sobre él y devuelven un subgrafo pequeño).

**Empieza por Graphify** cuando el usuario pregunte por: arquitectura, impacto de un cambio,
dependencias, "quién llama a X", ownership lógico, módulos críticos, o flujo de ejecución.

**Lee archivos fuente concretos (Read/Grep dirigido) sólo cuando:**
- vayas a editarlos,
- necesites verificar la implementación exacta línea a línea,
- el grafo esté obsoleto (ver frescura abajo),
- o el grafo no contenga la información requerida (p. ej. contenido de docs/markdown: ver alcance).

**Alcance del grafo:** incluye **código** (AST: Kotlin/Swift/TS/JS/…) y **documentos** markdown/texto
de forma **estructural** (cada archivo y sus encabezados/secciones son nodos; p. ej. el PRD del
producto en [`docs/MindBoost_PRD_Completo.md`](docs/MindBoost_PRD_Completo.md), añadido con 177 nodos).
Lo que NO se hace por defecto (sin API key) es la extracción **semántica por LLM** de los docs
(conceptos + aristas inferidas cruzadas); esa capa es opt-in (Gemini key o host-agent). Para el
contenido exacto de un doc, sigue leyendo el `.md`; para su estructura/relaciones, usa el grafo.

**Frescura / cuándo está obsoleto:** `GRAPH_REPORT.md` indica el commit desde el que se construyó
(`Built from commit: <hash>`). Compáralo con `git rev-parse HEAD`; si difieren mucho o hiciste
cambios estructurales, refresca el grafo.

**Tras cambios estructurales de código, refresca el grafo al terminar** (AST-only, sin coste):

```bash
# Refrescar / reconstruir el grafo (code-only, sin LLM, incremental vía caché AST)
graphify update .
# o el script equivalente del repo:
.claude/scripts/graphify-refresh.sh

# Consultas (operan sobre graphify-out/graph.json, devuelven subgrafo acotado)
graphify query "What are the main modules and their dependencies?"
graphify explain "ProfileStateHolder"
graphify path "MindBoostHubScreen" "SpeedProfileSource"
graphify affected "SecureKeyValueStore"          # impacto inverso de un cambio

# Watch mode (auto-reconstruye al cambiar el código; sin LLM)
graphify watch .

# Visualización local (árbol colapsable; se regenera, está en .gitignore)
graphify tree                                    # -> graphify-out/GRAPH_TREE.html
```

> NOTA: NO uses `graphify extract . --out .` para refrescos rutinarios — esa ruta intenta
> extracción semántica de docs y EXIGE una API key de LLM. La ruta sin coste es `graphify update .`.
> `graph.json` está en `.gitignore` (artefacto pesado, regenerable): si no existe tras un `clone`,
> reconstrúyelo con `graphify update .`.

**Scripts npm equivalentes:** `npm run memory:build` (grafo + viz), `npm run memory:update`
(incremental), `npm run memory:watch` (auto-rebuild). Requieren `graphify` en el PATH
(uv lo instala en `~/.local/bin`); el script `.claude/scripts/graphify-refresh.sh` es el wrapper
robusto (arregla el PATH, neutraliza claves LLM y verifica los artefactos).

### Automatización (opcional) y hooks

- **Hooks de Claude Code (ya activos):** el instalador de Graphify registró hooks `PreToolUse` en
  [`.claude/settings.json`](.claude/settings.json) que, cuando existe `graphify-out/graph.json`,
  inyectan un recordatorio para consultar el grafo antes de grepear/leer fuente. Son *advisory*
  (nunca bloquean ni hacen fallar una tool). **Caveat Windows:** parsean el input con `python3`, que
  en esta máquina es el stub de Microsoft Store, así que el recordatorio automático hará *no-op*
  silencioso. Las reglas de esta sección son el mecanismo fiable; los hooks son un extra.
- **Hooks de git (NO instalados — opt-in):** para reconstruir el grafo automáticamente en cada
  commit/checkout (code-only, sin LLM), actívalos tú mismo:

```bash
graphify hook install     # instala post-commit + post-checkout (auto-rebuild code-only)
graphify hook status      # comprueba si están instalados
graphify hook uninstall   # los elimina
```

  No se instalan por defecto a propósito: en este repo trabajan agentes automáticos (Vibe Kanban,
  n8n) que hacen muchos commits/checkouts, y un rebuild por cada uno generaría churn. Prefiere el
  refresco manual (`.claude/scripts/graphify-refresh.sh` o `npm run memory:update`) tras cambios
  estructurales.

## Browser Automation

Use `agent-browser` (Vercel Labs) for browser automation and visual/web interaction tasks. It is
installed as a **project dependency** (not global), and the Claude Code skill lives in
[`.claude/skills/agent-browser`](.claude/skills/agent-browser/SKILL.md). Run it with
`npx agent-browser …` (o `npm exec -- agent-browser …`). Load the version-matched guide first with
`npx agent-browser skills get core`.

Basic workflow:
1. `npx agent-browser open <url>` — Open a page
2. `npx agent-browser snapshot -i` — Get interactive elements and refs (`@eN`)
3. `npx agent-browser click @e1` — Click an element by ref
4. `npx agent-browser fill @e2 "text"` — Fill an input by ref
5. Re-run `npx agent-browser snapshot -i` after page changes; `npx agent-browser close --all` when done

Browser setup (already done in this repo): `npx agent-browser install` downloads Chrome to
`~/.agent-browser/` (machine cache, outside the repo).

For local development, start your web target first, then open its URL — e.g.
`npx agent-browser open http://localhost:3000`. This repo is KMP/Compose/Android/Firebase and has no
web `dev` server by default; typical targets are the Firebase Hosting emulator or any external URL.

## Convenciones de trabajo del proyecto

Este es un proyecto Kotlin Multiplatform / Compose Multiplatform. Antes de crear componentes
nuevos, revisar convenciones existentes y reutilizar componentes del repo.

Trabajar siempre por fases pequeñas. Después de cada fase, correr tests, Detekt y verificación de
compilación. No avanzar si algún gate falla.

Si el IDE posee el Gradle daemon en este worktree, evitar invocar Gradle CLI directamente y usar
verificación estática/compilación alternativa.

Los reportes largos, auditorías y análisis multi-agente deben escribirse en archivos markdown
dentro del repo. El chat solo debe contener updates breves.

Para auditorías multi-agente, lanzar máximo 2-3 agentes concurrentes. Si un agente devuelve output
vacío, reintentar solo esa lente.

No sobreactivar el Token Budget Gate para tareas locales, aditivas o de UI simple.

## Regla núcleo: bucle de mejora automática del proyecto

Cada vez que corrijas un error, bug, fallo de compilación, fallo de arquitectura, fallo de integración, fallo de imports, fallo de dependencias, fallo de tipos, fallo de UI, fallo de estado, fallo de navegación, fallo de permisos, fallo de Firebase, fallo de Gradle, fallo de Kotlin, fallo de Compose o cualquier problema causado por una mala suposición previa, debes actualizar este archivo.

Tu obligación es convertir cada error corregido en una regla útil para evitar que vuelva a ocurrir.

---

## Protocolo obligatorio después de corregir un error

Después de resolver cualquier error real del proyecto, añade una entrada nueva en la sección:

`## Aprendizajes acumulados del proyecto`

La entrada debe tener este formato exacto:

```md
### YYYY-MM-DD — [Título corto del error]

**Error detectado:**
Describe el error concreto que ocurrió.

**Causa raíz:**
Explica por qué ocurrió realmente. No describas síntomas. Identifica la causa técnica.

**Solución aplicada:**
Describe qué cambio solucionó el problema.

**Regla futura:**
Define una regla clara que deberás seguir en futuras modificaciones para no repetir este error.

**Archivos afectados:**
- `ruta/del/archivo.ext`
```

---

## Criterios para actualizar este archivo

Debes actualizar este archivo cuando ocurra cualquiera de estos casos:

1. Has corregido un error de compilación.
2. Has corregido un crash.
3. Has corregido un bug visual.
4. Has corregido una navegación rota.
5. Has corregido una integración mal conectada.
6. Has corregido imports incorrectos.
7. Has corregido un problema de Gradle, Kotlin, Compose, Firebase o permisos.
8. Has detectado que una decisión anterior era incorrecta.
9. Has descubierto una convención específica de este proyecto.
10. Has aprendido una restricción técnica que debe recordarse.

No actualices este archivo por cambios triviales de texto, estilos menores o modificaciones que no enseñen una regla reutilizable.

---

## Reglas para escribir aprendizajes

Los aprendizajes deben ser:

- Concretos.
- Técnicos.
- Reutilizables.
- Breves.
- Basados en errores reales corregidos.
- Escritos para evitar repetir el mismo fallo en el futuro.

No escribas reflexiones largas. No escribas teoría genérica. No escribas documentación innecesaria. No inventes problemas. No añadas reglas si no se ha corregido un error real.

---

## Uso obligatorio antes de modificar código

Antes de hacer cualquier cambio en el código, revisa la sección `Aprendizajes acumulados del proyecto`.

Debes aplicar esas reglas como restricciones activas durante la edición.

Si una regla previa entra en conflicto con una instrucción nueva del usuario, sigue la instrucción nueva del usuario, pero conserva la regla anterior salvo que haya quedado obsoleta.

Si una regla queda obsoleta, no la borres: añade una nota debajo indicando que ha sido reemplazada y explica por qué.

---

## Referencia rápida — errores que obligan a actualizar

- compilación
- crash
- navegación
- imports
- dependencias
- Gradle
- Kotlin
- Compose
- Firebase
- permisos
- arquitectura
- estado
- integración
- UI rota
- suposición incorrecta del agente

No añadas teoría. No añadas reglas genéricas. No inventes aprendizajes. No borres aprendizajes antiguos salvo que el usuario lo pida.

Actualiza este archivo solo si el error corregido enseña una regla reutilizable.

---

## Aprendizajes acumulados del proyecto

### 2026-07-03 — Un adapter módulo→perfil debe gatear por `BankIntegrity`, no solo por estado de evidencia

**Error detectado:**
No era un bug vivo (la fuente no está cableada en producción): la FASE-0 de un slice de blindaje
(`LANGUAGE_EVIDENCE_INTEGRITY_HARDENING`) reveló que `LanguageProfileSource` gateaba la contribución
al brain-profile por `EvidenceState` (E1+) pero NO por integridad de banco. Si alguien cableara los
repos reales —que persistirían evidencia del banco DEMO `LanguageDemoBank`, `SMALL_UNVALIDATED`— la
dimensión VERBAL saldría de CALIBRATING con **señal fabricada**: el evidence-gate no distingue un banco
validado de uno demo/stub.

**Causa raíz:**
El adapter asumía "hay evidencia medible" ⇒ "la evidencia es fiable". Son ortogonales: la integridad
del BANCO (¿el contenido sostiene una inferencia?) y la provenance del EVALUADOR (¿el scorer es un
modelo real o un stub con `thetaMean` hardcodeado?) son gates independientes del estado de evidencia.
Un adapter módulo→perfil sin gate de banco puede filtrar señal de contenido no validado.

**Solución aplicada:**
`BankIntegrity.blocksProfileEligibility()` como fuente ÚNICA del set bloqueante `{CONTAMINATED,
HOLDOUT_EXHAUSTED, SMALL_UNVALIDATED}` (`ScoreStateMachine.bankContaminated` ahora delega ahí, misma
semántica). `LanguageProfileSource` gana `contentIntegrity` (default `SMALL_UNVALIDATED`) +
`provenanceValidated` (default `false`) y un `internal fun evaluate(): LanguageEvidenceDecision` puro
con prioridad `SMALL_UNVALIDATED_BANK → STUB_PROVENANCE → INSUFFICIENT_VALID_EVIDENCE → Eligible`;
`Blocked → null → CALIBRATING`. Defaults **fail-closed**. Sin persistencia nueva, sin cambio de copy.
Probado end-to-end (repo cifrado real con 5 sesiones + evidencia E3 → VERBAL CALIBRATING, fuera del
Global Score).

**Regla futura:**
Todo adapter módulo→perfil (`Cognitive*ProfileSource`) debe gatear por
`BankIntegrity.blocksProfileEligibility()` ADEMÁS del estado de evidencia: un banco demo/no validado o
provenance stub NUNCA debe producir una contribución no-nula. El **gate de lectura** (no una bandera
persistida `profileEligible`) es la autoridad de elegibilidad — evita una segunda fuente de verdad. Los
defaults del gate deben ser fail-closed. Un flag de provenance NO debe ser un literal en el call-site:
derivarlo de versiones auditables (evaluator/rubric/projector). Verificar end-to-end vía
`MindBoostProfileAggregator` que la dimensión queda CALIBRATING (`value==null`) y no entra al
`OverallSummary.compositeIndex`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreTypes.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/{LanguageProfileSource,LanguageEvidenceDecision}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/adapter/LanguageProfileSourceTest.kt`
- `docs/mindboost_profile_2a/LANGUAGE_EVIDENCE_INTEGRITY_HARDENING_2026-07-03.md`

### 2026-07-03 — Un callback de "confirmado" que descarta su payload (`onSealed = { _, _ -> onExit() }`) es pérdida silenciosa de datos

**Error detectado:**
Al sellar una sesión de Decisiones (MindBoost, B1), la UI confirmaba un guardado que nunca ocurría:
`DecisionsRootScreen` cableaba el stage Result con `onSealed = { _, _ -> onExit() }`, descartando la
`DecisionSession` y el `DiaryEntry` (diario reflexivo de texto libre) que `DecisionResultScreen` SÍ emitía
(`onSealed(holder.session, entry)`). Reabrir la app no mostraba ni el diario ni la sesión.

**Causa raíz:**
Un slice previo construyó el flujo (holder `seal()` → `onSealed(session, entry)`) pero dejó la persistencia como
TODO: el callback del hub ignoraba ambos argumentos y solo navegaba. Los repos cifrados
(`SecureDecisionSessionRepository`/`SecureDiaryRepository`, namespace `mindboost_decisiones`) existían y estaban
testeados, pero nunca se instanciaban ni se invocaban. Un callback `{ _, _ -> }` que la UI presenta como
"guardado" es una mentira de UX + pérdida garantizada de datos.

**Solución aplicada:**
Se extrajo `internal fun handleSealed(session, entry, persist, exit)` (pura, unit-testable, espejo de
`handleReversibilityFinish` del mismo archivo) que **persiste ANTES de salir**; `DecisionsRootScreen` gana un
param `onSessionSealed` con default `{ _, _ -> }`; el hub instancia los dos repos sobre el store `Decisiones`
ya existente y cablea `onSessionSealed = { s, e -> mindBoostScope.launch { sessionRepo.save(s); diaryRepo.save(e) } }`.
El save va en `mindBoostScope` (`rememberCoroutineScope` a nivel del hub, FUERA del `when(track)`), que sobrevive
a la navegación Decisiones→Hub (mismo patrón fire-and-forget que `onReversibilityTrials`). Test rojo→verde:
(a) orden persist-antes-de-exit; (b) round-trip real sellar→save→"reabrir" (repos frescos sobre el mismo
`InMemorySecureKeyValueStore`)→sesión+diario visibles byte a byte. `mindboost_decisiones` ya estaba en
AccountBound+eraser → sin hueco GDPR nuevo.

**Regla futura:**
Todo callback de "completado/confirmado" que reciba un payload (`onSealed`/`onFinished`/`onSaved`) debe
CONSUMIRLO (persistir/emitir) ANTES de navegar; un `{ _, _ -> onExit() }` que la UI presenta como guardado es
pérdida silenciosa. Extraer la orquestación persist-antes-de-navegar a una función pura (`internal fun`, espejo
de las ya existentes del módulo) y probarla con round-trip REAL (save→reabrir con repos frescos sobre el mismo
store), no solo "se llamó el callback". Lanzar el save en un scope que sobreviva a la navegación
(`rememberCoroutineScope` del contenedor que permanece montado, no el del hijo que se desmonta). Riesgo residual
conocido: matar el proceso entre el `launch` y su suspensión pierde la escritura (idéntico al patrón preexistente;
B1 no lo agrava; el write-ahead/checkpoint transversal es Fase 2). No confundir el nombre `DiaryEntry.encryptedContent`
con cifrado a nivel de dominio: guarda bytes en claro y el cifrado real ocurre en `SecureKeyValueStore` (Android
Keystore AES-256-GCM); el modelo NO cifra.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ui/DecisionsRootScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ui/HandleSealedFinishTest.kt`

### 2026-07-01 — Overlay de carga: `matchParentSize` en TODOS los hijos colapsa el Box, y `mergeDescendants`+`contentDescription` duplica el anuncio a11y

**Error detectado:**
Al crear `AppLoadingOverlay` (sistema de carga global), (1) el `Box` exterior envolvía el contenido en
`Box(Modifier.matchParentSize()){content()}` Y el scrim también usaba `matchParentSize()` → el Box exterior
no tenía ningún hijo que lo dimensionara y colapsaba a 0 (overlay invisible). (2) El scrim usaba
`semantics(mergeDescendants=true){ contentDescription = message }` mientras el loader hijo aportaba su propio
`contentDescription` y el `Text` del mensaje → TalkBack/VoiceOver anunciaba el mensaje 2-3 veces
("Cargando…, Cargando contenido, Cargando…").

**Causa raíz:**
(1) `Modifier.matchParentSize()` mide el hijo contra el tamaño del padre pero NO contribuye a determinar ese
tamaño. Si TODOS los hijos del Box lo usan, el padre no tiene de qué dimensionarse → colapsa. (2) La política
de merge de `ContentDescription` en Compose ACUMULA (no reemplaza): un nodo `mergeDescendants` con su propio
`contentDescription` lo concatena con los de sus descendientes. `Modifier.blur` es draw-only → no altera la medida.

**Solución aplicada:**
(1) El wrapper de contenido NO usa `matchParentSize` (dimensiona el Box desde el contenido); solo el scrim
superpuesto lo usa. (2) El scrim usa `clearAndSetSemantics { liveRegion=Polite; contentDescription = message
?: A11yLoading; progressBarRangeInfo = Indeterminate }` → UN anuncio determinista (mejor que el
`mergeDescendants` de `PhotoUploadLoader`).

**Regla futura:**
En un overlay `Box { contenido; if(visible) scrim }`, exactamente UN hijo debe dimensionar el Box (el wrapper
de contenido, SIN `matchParentSize`); los hijos superpuestos (scrim) usan `matchParentSize`. Nunca ponerlo en
todos los hijos → colapsa a 0. Para el a11y de un grupo de carga modal, usar `clearAndSetSemantics` (anuncio
único), NO `mergeDescendants`+`contentDescription` (duplica, porque la política de ContentDescription acumula).
El loader base puede llevar su propio `contentDescription`/`ProgressBarRangeInfo.Indeterminate` para uso
standalone; el grupo lo re-declara con clearAndSet. Para portar un loader CSS `ease-in-out`, usar
`CubicBezierEasing(0.42f,0f,0.58f,1f)` (simétrico), NO `FastOutSlowInEasing` (asimétrico). Constantes top-level
de un archivo NUEVO deben ir en `SCREAMING_SNAKE_CASE` (TopLevelPropertyNaming); las de archivos ya baselined
que son PascalCase (p.ej. `PhotoUploadLoader`) solo pasan por estar en el baseline. EXCEPCIÓN: un
`CompositionLocal` top-level (`val LocalXxx = staticCompositionLocalOf{...}`) DEBE conservar el nombre idiomático
`LocalXxx` con `@Suppress("TopLevelPropertyNaming")` (no es constante; el prefijo `Local` es contrato de Compose).
Para tintar un loader ya migrado por el color favorito SIN regresión, leer
`favoriteAccentColor(LocalFavoriteAccent.current, fallback = <colorDeMarcaDeLaPantalla>)`: conserva el color de
marca hasta que exista un provider raíz. Añadir un campo a `AppGraph` para inyectar una dependencia exige un
DEFAULT singleton estable (un `object` no-op como `EmptyProfileCardRepository`, NUNCA `Clase()` por-instancia) o
rompe `AppGraphTest.graph_isEquatable_dataClass` (ver 2026-06-19).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/design/components/{AppLoadingOverlay,AppBarsLoader,AppLoadingSamples}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/design/FavoriteColor.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/design/{FavoriteColorTest,components/AppBarsLoaderProgressTest}.kt`

### 2026-06-30 — Un detector declara su propia `COVERAGE`; el consumidor la consulta (no el literal `DetectorCoverage`)

**Error detectado:**
`:shared:desktopTest` falló (`AdequateCoverageInvariantTest`) al añadir un gate de cobertura en
`SpeedProfileSource`: el nuevo literal `DetectorCoverage.PARTIAL` en ese archivo no estaba en el registro
de sitios respaldados por un detector testeado, así que el escáner de la invariante lo marcó como
claim de cobertura no respaldado.

**Causa raíz:**
La invariante (`ADEQUATE/PARTIAL ⇒ control testeado invocado`) escanea TODO archivo de producción que
contenga el literal `DetectorCoverage.ADEQUATE`/`.PARTIAL`. `SpeedProfileSource` usaba el literal para
NEGAR la contribución (PARTIAL → no adecuado → CALIBRATING), pero el escáner no distingue intención: ve
el literal y exige respaldo. Además tener el nivel de cobertura hardcodeado en el consumidor permite que
derive a un claim más fuerte que el detector real.

**Solución aplicada:**
El detector DECLARA su propia cobertura como única fuente de verdad: `SpeedAntiGaming.COVERAGE =
DetectorCoverage.PARTIAL`. El consumidor (`SpeedProfileSource.contribution`) consulta
`SpeedAntiGaming.COVERAGE.adequateFor(RT_CRITICAL)` — NO contiene el literal, así que deja de ser un
"claiming site". `SpeedAntiGaming.kt` (que ahora tiene el literal) se registró en el invariante mapeado a
un detector que SÍ invoca (`RhythmTappingDetector`), y su red-team (`SpeedAntiGamingTest`) es su prueba
adversarial. El gate quedó como expression-body (`if (...) eligibleContribution else null`) → 0 returns
(ReturnCount OK), con la lógica bespoke preservada en un `internal eligibleContribution` testeable.

**Regla futura:**
Para gatear un consumidor por cobertura anti-gaming, que el DETECTOR declare `val COVERAGE =
DetectorCoverage.X` (única fuente de verdad) y el consumidor consulte `Detector.COVERAGE.adequateFor(...)`,
NUNCA hardcodee el literal en el consumidor. Todo archivo de producción que contenga un literal
`DetectorCoverage.ADEQUATE`/`.PARTIAL` DEBE registrarse en `AdequateCoverageInvariantTest` mapeado a un
control testeado que invoque (con su suite adversarial). Para gatear una métrica RT-crítica con cobertura
PARTIAL no hace falta el routing completo por `ScoreResultFactory`: basta `COVERAGE.adequateFor(RT_CRITICAL)`
(misma regla que el state machine) — el outcome (CALIBRATING) es idéntico con mucho menos riesgo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGaming.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt`
- `shared/src/desktopTest/.../core/scoring/AdequateCoverageInvariantTest.kt` · `.../profile/adapter/SpeedProfileSourceTest.kt`

### 2026-06-30 — Componer detectores anti-gaming genéricos da cobertura PARCIAL, no ADEQUATE; y los tests deben modelar el flujo de captura REAL

**Error detectado:**
`SpeedAntiGaming` (nuevo) componía los detectores INFRA-03 generales D1/D3/D4 y su KDoc afirmaba que un
futuro wiring "podría reclamar `DetectorCoverage.ADEQUATE` para Velocidad". Una verificación adversarial
(workflow, 3 lentes) confirmó falsos NEGATIVOS reales: fast-guessing uniforme ~260ms a chance (el floor de
D3 es `median<250`, exclusivo), alternar rápido/lento que mantiene la mediana ≥250, y la banda [100,250)ms.
Además los tests usaban dificultad VARIABLE por trial, pero los bloques MVP-0 son de dificultad FIJA, así que
el guard RT↔dificultad de D1 (correlación) es trivialmente flat en producción y nunca se ejercitaba de verdad.

**Causa raíz:**
(1) Los umbrales de los detectores generales (D3 250ms, D4 30%) no están calibrados para el gaming específico
de Velocidad, que tiene una banda de explotación de "adivinar rápido pero no ultra-rápido". Componer
detectores genéricos NO equivale a cobertura adecuada. (2) El test modelaba un flujo idealizado (dificultad
variable + RT que la sigue) que no ocurre en el runtime real (dificultad fija por bloque), dejando un guard
muerto sin cobertura de test — el mismo patrón "verifica el flujo de captura vivo" de EX4/EX1.

**Solución aplicada:**
KDoc corregido a **COBERTURA PARCIAL**: un futuro wiring debe usar `DetectorCoverage.PARTIAL` (insuficiente
para RT_CRITICAL → Velocidad sigue CALIBRATING), NUNCA `ADEQUATE`; ADEQUATE exige un detector de fast-guessing
calibrado por §15. Añadido `MIN_TRIALS_FOR_DETECTION=12` (un ratio/mediana sobre muestra diminuta no es
evidencia de gaming → verdict NONE). Tests corregidos a dificultad fija `{0.5}` (D1 se reduce correctamente a
la cadencia-IRI sola; el honesto se salva por la varianza de IRI del foreperiodo aleatorio) + test del guard.

**Regla futura:**
Antes de reclamar `DetectorCoverage.ADEQUATE` para un módulo, VERIFICAR adversarialmente que los detectores
realmente cazan sus adversarios — componer detectores genéricos suele dar solo PARCIAL (umbrales no calibrados
dejan bandas de explotación). Reclamar ADEQUATE sin eso viola el `AdequateCoverageInvariantTest`. Los tests de
un detector deben construir adversarios y un honesto que reflejen el flujo de captura REAL (en Velocidad MVP-0:
dificultad FIJA por bloque, foreperiodo aleatorio), no un flujo idealizado que deje guards muertos sin cobertura.
Todo detector de ratio/mediana necesita un guard de mínimo de muestra (como D1 tiene `MIN_IRI`).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGaming.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGamingTest.kt`

### 2026-06-30 — Un generador paramétrico con label aleatorio + descripción plantilla fabrica una señal de perfil

**Error detectado:**
`ReversibilityEngine.generateScenario` (E2 Reversibilidad, Decisiones) producía `description =
"Reversibility triage level N"` (plantilla genérica, sin contenido de decisión) e `isReversible =
random.nextDouble() >= irreversibility` (CARA O CRUZ). La tarjeta no daba ninguna pista de
reversibilidad y la etiqueta correcta era aleatoria → la tarea es INjuzgable, pero `ReversibilityScore`
alimentaba un `REVERSIBILITY_DISCRIMINATION` a DECISION_MAKING en producción (hub escribe
`mindboost_decisiones`, brain-profile lo lee) → señal cognitiva FABRICADA.

**Causa raíz:**
El adapter clasificaba el contenido como `BankIntegrity.PROCEDURAL` (válido procedural), tratando un
generador placeholder degenerado como si fuera un banco real. Además el state machine solo bloqueaba
`CONTAMINATED`/`HOLDOUT_EXHAUSTED`, NO `SMALL_UNVALIDATED` — así que la invariante de la carta
("banco pequeño/no validado bloquea el perfil") no se enforzaba en runtime (solo en el `BankValidator`
de build de INFRA-04).

**Solución aplicada:**
(1) `ScoreStateMachine.bankContaminated` ahora también bloquea `SMALL_UNVALIDATED` (enforce de la
invariante en runtime). (2) `ReversibilityProfileAdapter.toScoreResult` gana un parámetro
`bankIntegrity` (default `PROCEDURAL`, mantiene el adapter genérico/reusable para un banco real). (3)
`ReversibilityScore.scoreOf` (el fold de producción) pasa `BankIntegrity.SMALL_UNVALIDATED` → todo
resultado es `BANK_CONTAMINATED` (no actualiza el perfil) sin importar las respuestas → DECISION_MAKING
queda CALIBRATING hasta que exista un banco validado real. El ejercicio sigue jugable (minijuego). Los
gates de discriminación/anti-gaming siguen testeados en el adapter (banco `PROCEDURAL` por defecto).

**Regla futura:**
Antes de cablear un ejercicio al perfil, AUDITAR su CONTENIDO real, no solo el scorer: una etiqueta
correcta aleatoria o una descripción-plantilla sin señal hace la tarea injuzgable y cualquier métrica
derivada es fabricada. Un generador placeholder degenerado debe marcarse `SMALL_UNVALIDATED` (no
`PROCEDURAL`) para que el bank gate lo bloquee. Mantener `BankIntegrity` como parámetro del adapter (no
hardcoded) para separar "el adapter" de "el contenido actual". Verificar que el state machine bloquea
de verdad el valor de integridad elegido (hoy: CONTAMINATED/HOLDOUT_EXHAUSTED/SMALL_UNVALIDATED).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring/{ReversibilityProfileAdapter,ReversibilityScore}.kt`
- `shared/src/desktopTest/.../core/scoring/ScoringStateMachineTest.kt` · `.../decisiones/domain/scoring/ReversibilityScoreTest.kt` · `.../profile/adapter/{DecisionsProfileSourceTest,MindBoostProfileSourcesTest}.kt`

### 2026-06-30 — `kotlin.test.assertTrue/assertFalse` NO tienen overload de mensaje lazy `() -> String`

**Error detectado:**
`:shared:compileTestKotlinDesktop` falló con "Argument type mismatch: actual type is 'Function0<String>',
but 'String?' was expected" al escribir `assertTrue(cond) { "mensaje $var" }` en un test nuevo.

**Causa raíz:**
A diferencia de `kotlin.require`/`check`/`error` (stdlib, que sí toman `lazyMessage: () -> String`) y de
algunos `assert*` de JUnit, las funciones de `kotlin.test` solo exponen `assertTrue(actual: Boolean,
message: String? = null)` y `assertTrue(message: String? = null, block: () -> Boolean)` — NINGUNA acepta
`(Boolean, () -> String)`. El `{ ... }` se interpretó como el `block: () -> Boolean` del otro overload.

**Solución aplicada:**
Construir el mensaje en un `String` eager y pasarlo como 2º argumento: `val msg = "..."; assertTrue(cond, msg)`.

**Regla futura:**
En tests `kotlin.test` (KMP commonTest/desktopTest), el mensaje de `assertTrue`/`assertFalse`/`assertEquals`
es un `String?` eager, nunca un lambda. Si el mensaje es caro de construir, calcularlo antes en un `val`.
Reservar la sintaxis `{ "msg" }` para `require`/`check`/`requireNotNull`/`checkNotNull`, no para `assert*`.

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/scoring/AdequateCoverageInvariantTest.kt`

### 2026-06-30 — Una invariante de reglas no se auto-extiende a colecciones nuevas; el secret-scanner no cubría fuente cliente

**Error detectado:**
Una auditoría de secretos encontró (1) 4 reglas `allow list` de marketplace
(`marketplace_catalog`, `marketplace_assets`, `user_entitlements/.../items`,
`user_inventory/.../items`) SIN `request.query.limit`, pese a que el resto del archivo lo impone
(invariante 2026-05-26); y (2) que `functions/scripts/scan-secrets.js` (el gate de secretos de CI)
solo escaneaba `.ts/.js/.json/.rules/.md/.xml/.plist/.yml/.yaml` y NO `.kt/.kts/.swift` — toda la
fuente cliente (847 archivos) quedaba fuera del único gate automático contra secretos.

**Causa raíz:**
Las dos invariantes existían pero no se auto-aplican: una regla `allow list` añadida DESPUÉS del
endurecimiento 2026-05-26 no hereda el límite (ni el validador `firebase_validate_security_rules` ni
el emulador exigen que toda `list` tenga `request.query.limit`), y el allowlist de extensiones del
scanner nunca se amplió a la fuente cliente aunque cualquier literal en `.kt/.swift` es extraíble de
un APK/IPA por ingeniería inversa.

**Solución aplicada:**
(1) Las 4 reglas ahora exigen `request.query.limit != null && <= N` (catálogo/assets=50,
entitlements/inventory=100), manteniendo la autorización (`signedIn()`/`isOwner()`) ortogonal al
límite; +12 tests de regresión (M12–M23) en `firestore.security.rules.test.mjs` (134/134 verde en
emulador). (2) Se añadieron `.kt/.kts/.swift` a `isScannable()`; `scan:secrets` sigue verde con 847
archivos cliente en alcance (re-confirma cero secretos hardcodeados) y sus 21 tests unit pasan.
NOTA: marketplace no tiene cliente Kotlin aún (grep 0 hits), así que el límite es un contrato
forward sin desfase regla↔cliente (invariante 2026-05-29).

**Regla futura:**
Al añadir una colección con `allow list`, aplicar SIEMPRE el cap `request.query.limit` (autorización
y límite son controles ortogonales) y añadir su test de regresión `list-sin-limit→FAIL` +
`list-con-limit→OK` en el mismo cambio — ninguna herramienta lo exige automáticamente. Todo
secret-scanner debe cubrir la fuente CLIENTE (`.kt/.kts/.swift`), no solo backend: un literal
compilado en un APK/IPA es tan extraíble como uno de servidor.

**Archivos afectados:**
- `firestore.rules`
- `firestore.security.rules.test.mjs`
- `functions/scripts/scan-secrets.js`
- `.gitignore` (defensa anti-commit de dumps de conversaciones IA personales no trackeados)

### 2026-06-30 — Una métrica RT-crítica no debe alimentar el perfil sin gate de timebase autoritativo en el profile-source

**Error detectado:**
`SpeedProfileSource` (cableado en producción, `MindBoostProfileSources.kt:57`) alimentaba la dimensión
PROCESSING_SPEED filtrando solo runs `LOW`/`CONTAMINATED`. En desktop/iOS el `FallbackSpeedTimebaseHandle`
persiste `SYSTEM_CLOCK_MONOTONIC`/`MILLIS_DERIVED` → `deviceTimingRisk=MODERATE` → `SessionQuality.ACCEPTABLE`,
que SÍ entraba al perfil: un RT medido con base temporal no-autoritativa (fallback monotónico, no
Choreographer) actualizaba silenciosamente el perfil cognitivo (falso-positivo de validez VIVO).

**Causa raíz:**
El gate de honestidad del adapter (`honestSpeedScore`) solo exigía que la barra de velocidad fuese
computable, NO que el timebase fuese autoritativo. La clasificación de calidad (`SessionQualityEvaluator`)
ya niega `HIGH` a runs no-nativos, pero `ACCEPTABLE` no se excluía del perfil — y para una métrica
RT-crítica, "aceptable para jugar" ≠ "autoritativo para medir". El `TimingPolicyClassifier` que define
`Authoritative` (`CHOREOGRAPHER_FRAME && NANOS_NATIVE`) existía pero estaba muerto (no cableado al
profile-source).

**Solución aplicada:**
En `SpeedProfileSource.qualifyingSessions` se añadió `hasAuthoritativeTimebase(sessionId)` que excluye toda
sesión cuyo `SessionMetrics` no sea `timestampSource==CHOREOGRAPHER_FRAME && inputTimestampPrecision==NANOS_NATIVE`
(métricas ausentes → inverificable → excluida). Al vivir el gate en `qualifyingSessions`, todo el pipeline
(conteo→dataSufficiency/confidence, trend, latest) refleja solo medidas autoritativas. Velocidad solo
alimenta el perfil en Android API≥34 (Choreographer + nanos nativos); fallback desktop/iOS y Android API<34
quedan jugables pero excluidos. Los campos de timebase ya estaban en `SessionMetrics` (cargado por el adapter)
y `QualityDecisionLog` — no hicieron falta nuevos métodos de store.

**Regla futura:**
Toda señal RT-crítica que alimente el perfil debe gatearse por timebase AUTORITATIVO en el propio
profile-source (`CHOREOGRAPHER_FRAME && NANOS_NATIVE`), no solo por la clasificación de calidad de la sesión:
`SessionQuality.ACCEPTABLE` (jugable) NO implica timebase válido para medir. El gate va donde se decide qué
sesiones informan el perfil (`qualifyingSessions`), para que el CONTEO (sufficiency/confidence/trend) también
excluya las no-autoritativas, no solo el último valor. Verificar SIEMPRE los campos de autoridad (timestampSource/
inputTimestampPrecision) contra lo que persiste el `Fallback*Handle` de cada plataforma antes de fijar la regla.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/adapter/SpeedProfileSourceTest.kt`

### 2026-06-25 — El hub de MindBoost necesita `verticalScroll`: los tracks nuevos quedaban fuera de pantalla

**Error detectado:**
El track "Creatividad" (y "Lenguaje") no se podían seleccionar para entrenar: la tarjeta existía y el
módulo compilaba, pero no aparecía en pantalla.

**Causa raíz:**
`HubContent` de `MindBoostHubScreen` apila las tarjetas en un `Column` SIN `verticalScroll`. Al crecer
el hub a 8 tracks, las últimas tarjetas (Creatividad es la última) se renderizan por debajo del
viewport del teléfono y quedan inalcanzables. No es gating ni falta de wiring — es overflow sin scroll.

**Solución aplicada:**
Se añadió `.verticalScroll(rememberScrollState())` (+ `navigationBarsPadding()`) al `Column` del hub.

**Regla futura:**
Toda lista vertical de tarjetas/opciones que pueda crecer (hubs, menús de ejercicios) DEBE usar
`Modifier.verticalScroll(rememberScrollState())` (o `LazyColumn`). Al añadir un track al hub, verificar
que sea ALCANZABLE en un teléfono, no solo que compile: un Composable que compila no equivale a una UI
alcanzable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt`

### 2026-06-25 — `Modifier.background(if cond Color else Brush)` no resuelve (tipo común `Any`)

**Error detectado:**
`:shared:compileKotlinDesktop` falló en el botón "Añadir amigo" del nuevo perfil con
"Argument type mismatch: actual type is 'Any', but 'Color'/'Brush' was expected" en `.background(...)`.

**Causa raíz:**
Se pasó `background(if (cond) Color(...) else Brush.verticalGradient(...))`. `Color` y `Brush` no
comparten supertipo útil, así que el `if/else` se infiere como `Any`, y ninguno de los dos overloads
de `Modifier.background` (uno toma `Color`, otro `Brush`) es aplicable.

**Solución aplicada:**
Ramificar el `Modifier`, no el argumento:
`.then(if (cond) Modifier.background(color) else Modifier.background(brush))`.

**Regla futura:**
Cuando una propiedad visual de Compose dependa de una condición y las dos ramas sean de tipos distintos
no relacionados (`Color` vs `Brush`, dp vs lambda overloads, etc.), ramificar el `Modifier` con
`.then(if … Modifier.x(a) else Modifier.x(b))`, nunca meter el `if` dentro del argumento. Mismo patrón
ya usado para `border` condicional en este repo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/profile/card/presentation/ui/ProfileCardHeader.kt`

### 2026-06-22 — Añadir un miembro a `FeatureFlags` obliga a actualizar TODAS sus implementaciones

**Error detectado:**
`:androidApp:compileDebugKotlin` falló con "Class 'RemoteConfigFeatureFlags' is not abstract and
does not implement abstract member" para `arenaRatingEngineEnabled`, `arenaShadowRankedEnabled` y
`arenaMentalSpeedRankedEnabled`. `:shared:compileAndroidMain` compilaba: el desfase solo aparece al
compilar el módulo `androidApp`.

**Causa raíz:**
La interfaz `core/config/FeatureFlags.kt` (en `:shared`) ganó tres flags de Arena y se actualizaron
`DefaultFeatureFlags`/`StaticFeatureFlags`, pero NO la implementación Android
`RemoteConfigFeatureFlags`, que vive en `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
—fuera de `:shared`—. Compilar solo `:shared` no detecta la implementación incompleta del otro módulo.

**Solución aplicada:**
Se añadieron los tres `override val arena*` a `RemoteConfigFeatureFlags` con el patrón existente
`boolOrDefault(DefaultFeatureFlags.KEY_*, DefaultFeatureFlags.*Enabled)` (claves y defaults ya
existían en `DefaultFeatureFlags`).

**Regla futura:**
Al añadir un miembro a `FeatureFlags` (u otra interfaz con impls repartidas por módulos), actualizar
TODAS las implementaciones: `DefaultFeatureFlags`, `StaticFeatureFlags` (en `:shared`) y
`RemoteConfigFeatureFlags` (en `androidApp`). Verificar con `:androidApp:compileDebugKotlin`, no solo
`:shared:compileAndroidMain`: las impls del módulo de app no se compilan al validar únicamente `:shared`.

**Archivos afectados:**
- `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/core/config/FeatureFlags.kt`

### 2026-06-20 — La telemetría remota nunca debe aceptar Throwable externos

**Error detectado:**
Los fallos de borrado de cuenta y publicación social llegaban por identidad a `Crashlytics.recordException`, conservando mensaje y cadena de causas del SDK.

**Causa raíz:**
El contrato central `ObservabilitySink.recordNonFatal` aceptaba un `Throwable` arbitrario y no imponía minimización en la frontera con telemetría de terceros.

**Solución aplicada:**
El contrato se sustituyó por `NonFatalTelemetryEvent` con operación y tipo normalizados; Crashlytics crea una excepción local sin causa ni mensaje externo y los tests usan canarios para impedir regresiones.

**Regla futura:**
Ningún sink remoto puede aceptar excepciones externas crudas; convertirlas antes a campos allowlisted y probar que mensaje, causa e identidad original no cruzan la frontera.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/logging/Observability.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/core/logging/SafeDebugLogging.kt`
- `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/core/logging/ObservabilityHubTest.kt`

### 2026-06-20 — Las exportaciones debug no deben incorporar identificadores estables

**Error detectado:**
Los nombres y contenidos de exportaciones de Velocidad incluían UID, session ID, modelo y versión del dispositivo, y podían terminar copiados a evidencias locales de QA.

**Causa raíz:**
Se trató el gating debug y el almacenamiento privado como controles suficientes, sin minimizar los datos antes de escribirlos ni desacoplar el nombre de archivo de la identidad.

**Solución aplicada:**
Los exportadores usan nombres aleatorios, reemplazan identificadores estables y metadatos de dispositivo por valores de depuración, y no muestran mensajes crudos de errores de escritura.

**Regla futura:**
Toda exportación diagnóstica debe minimizar o pseudonimizar identidades antes de I/O y usar nombres no derivados del usuario, aunque esté limitada a builds debug o almacenamiento privado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExport.kt`
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExporter.android.kt`
- `shared/src/desktopMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExporter.desktop.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExportTest.kt`

### 2026-06-20 — Los fixtures de tests para lógica gated deben usar flags de confianza realistas

**Error detectado:**
`PersonalizedSessionScorerTest.isGated_whenTooFewTrials` falló: construía un `MetricValue` de ACCURACY
con `n=10` pero `confident=true`, esperando que el scorer devolviera `value=null`. El scorer puntúa
cualquier ACCURACY marcada `confident`, así que devolvió un valor.

**Causa raíz:**
El fixture fabricó un estado imposible: `DefaultMetricsEngine` marca `confident=false` para muestras
pequeñas, pero el test puso `confident=true` con `n=10`. La lógica gated del scorer depende del flag
`confident`, no de `n` directamente.

**Solución aplicada:**
El test modela la realidad: muestra pequeña → `confident=false` en el `MetricValue`, y el scorer
devuelve `value=null` con `confident=false`.

**Regla futura:**
Al testear lógica gated por `MetricValue.confident`, construir los fixtures con flags coherentes con lo
que produce el `MetricsEngine` (n pequeño ⇒ `confident=false`). Nunca fijar `confident=true` con un `n`
por debajo del umbral para "forzar" un camino.

**Archivos afectados:**
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/focus/domain/personalization/PersonalizedSessionScorerTest.kt`
- `CLAUDE.md`

### 2026-06-19 — Los overrides npm deben respetar rangos independientes

**Error detectado:**
`npm ls uuid --all` falló después de aplicar un override global a `uuid@11.1.1`: `universal-analytics@0.5.4` requiere `uuid@^14` y su instalación quedó marcada como inválida.

**Causa raíz:**
Se interpretó “override exacto” como reemplazo global sin comprobar que el árbol raíz contiene consumidores con contratos mayores e incompatibles.

**Solución aplicada:**
El override raíz se acotó a `uuid@<11.1.1` con reemplazo exacto `11.1.1`; así se endurecen versiones antiguas sin alterar el contrato `uuid@^14`.

**Regla futura:**
Antes de fijar un override npm global, ejecutar `npm ls <paquete> --all` y revisar todos los rangos; usar selector de versión acotado cuando existan ramas legítimas incompatibles.

**Archivos afectados:**
- `package.json`
- `docs/security/DEPENDENCY_HARDENING_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`


### 2026-06-19 — Las mutaciones de posts requieren autoridad backend e identidad canónica

**Error detectado:**
Las reglas permitían escrituras directas del cliente en `posts` y `user_posts`, y Android podía construir la identidad del autor desde campos fallback si faltaba `profile_visuals/{uid}`.

**Causa raíz:**
La autoridad de creación estaba dividida entre el cliente y la callable, y la compatibilidad de bootstrap se trató como sustituto de una identidad canónica.

**Solución aplicada:**
Las reglas deniegan toda mutación cliente de posts, Android usa `createPost`, la callable valida el payload y deriva la identidad exclusivamente del perfil canónico antes de escribir ambos mirrors.

**Regla futura:**
Toda mutación de posts debe pasar por la callable autoritativa; nunca aceptar ni reconstruir identidad de autor desde campos controlados por el cliente.

**Archivos afectados:**
- `firestore.rules`
- `functions/src/posts.ts`
- `functions/src/lib/validators.ts`
- `shared/src/androidMain/kotlin/com/mindos/app/features/social/data/feed/GlobalFeedRepositoryProvider.android.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los mapas de recibos DM deben limitar claves y propietario

**Error detectado:**
Un participante podía reemplazar `lastReadAtByUser` y escribir el recibo de lectura del otro participante o añadir claves ajenas al chat.

**Causa raíz:**
La regla validaba solo que el campo fuese un mapa, sin limitar sus claves ni comprobar qué entrada había cambiado.

**Solución aplicada:**
Las claves se restringieron a los participantes canónicos y `Map.diff().affectedKeys()` se limita a `request.auth.uid`; en creación solo se permite la clave del creador.

**Regla futura:**
Todo mapa Firestore indexado por UID debe validar la allowlist de claves y la propiedad de las claves modificadas; validar únicamente `is map` no protege su integridad.

**Archivos afectados:**
- `firestore.rules`
- `firestore.security.rules.test.mjs`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — El gate Android KMP se ejecuta desde la app y las propiedades se citan en PowerShell

**Error detectado:**
El gate intentó ejecutar la tarea inexistente `:shared:compileDebugKotlinAndroid`; el siguiente intento interpretó `-Pkotlin.incremental=false` como una tarea por el parsing de PowerShell.

**Causa raíz:**
Se asumió el nombre de tarea del plugin Android clásico aunque `shared` usa Android Multiplatform Library, y no se protegió el argumento Gradle con punto al invocarlo desde PowerShell.

**Solución aplicada:**
Se verificaron las tareas reales y se ejecutó `:androidApp:compileDebugKotlin`, que compila `:shared:compileAndroidMain`, pasando `"-Pkotlin.incremental=false"` entre comillas.

**Regla futura:**
Para validar Android de `shared`, usar `:androidApp:compileDebugKotlin` o verificar primero `:shared:tasks --all`; en PowerShell citar siempre propiedades Gradle `-P...` que contengan puntos.

**Archivos afectados:**
- `AGENTS.md`
- `CLAUDE.md`


### 2026-06-19 — Un prefijo versionado no puede confundirse con Base64 legacy

**Error detectado:**
La primera detección de envelope trataba cualquier ciphertext que empezara por `v` como versionado, aunque un Base64 legacy válido también puede empezar por ese carácter.

**Causa raíz:**
Se comprobó solo el primer carácter y no la presencia del delimitador que distingue el envelope de la codificación legacy.

**Solución aplicada:**
Solo se parsea una versión cuando existe `:`; los valores sin delimitador conservan la ruta legacy y las versiones futuras siguen devolviendo estado tipado.

**Regla futura:**
Todo formato versionado añadido sobre datos existentes debe usar una firma no ambigua y mantener un test/razonamiento explícito de compatibilidad con el alfabeto legacy.

**Archivos afectados:**
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `docs/security/SECURE_STORAGE_FAILURE_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — El modelo tipado de storage debe nacer compatible con Detekt

**Error detectado:**
`:shared:detekt` rechazó el nuevo secure storage por catches tipados como swallowed, exceso de returns, nombre de constante de instancia y líneas largas en tests.

**Causa raíz:**
Se implementó primero la semántica de seguridad sin adaptar su control de flujo y estilo al ratchet del módulo.

**Solución aplicada:**
Se documentó el catch que convierte parse errors en cuarentena, se redujeron returns con expresiones `when/if`, se normalizó el nombre de instancia y se ajustaron los tests.

**Regla futura:**
Al añadir resultados sellados y parsers de seguridad, diseñar desde el inicio un único retorno principal y localizar cualquier supresión de excepción convertida en estado tipado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.kt`
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/data/LocalLogicTrainingRepositoryTest.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/memory/data/LocalMemoryRepositoriesTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los creates inmutables de Storage deben exigir resource null

**Error detectado:**
El emulador aceptó sobrescribir un post attachment aunque el match declaraba `allow update: false`.

**Causa raíz:**
La rama `allow create` de profile/post no expresaba la precondición `resource == null`, dejando la inmutabilidad dependiente de la clasificación de operación del runtime.

**Solución aplicada:**
Todos los creates de media exigen explícitamente `resource == null` y un check estático fija las tres rutas.

**Regla futura:**
Para objetos Storage inmutables, combinar siempre `allow update: false` con `resource == null` en `allow create`; no confiar solo en la clasificación create/update del emulador.

**Archivos afectados:**
- `storage.rules`
- `storage.dm.rules.test.mjs`
- `functions/scripts/check-storage-rules.js`
- `docs/security/STORAGE_RULES_DYNAMIC_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los nombres thumbnail no deben caer al validador genérico

**Error detectado:**
El test dinámico aceptó un `*_thumb.jpg` mayor de 1 MiB porque, tras fallar `validThumb`, el mismo nombre coincidía con `validImage` y obtenía el límite de imagen principal.

**Causa raíz:**
Las ramas de validación de thumbnail e imagen genérica se combinaron con OR sin hacer sus patrones mutuamente excluyentes.

**Solución aplicada:**
La rama genérica excluye explícitamente el patrón canónico de thumbnail; se añadieron regresiones dinámica y estática.

**Regla futura:**
Cuando distintos tipos de archivo tengan límites diferentes, sus clasificadores deben ser mutuamente excluyentes; probar siempre que el tipo más restringido no pueda caer a una rama más permisiva.

**Archivos afectados:**
- `storage.rules`
- `storage.dm.rules.test.mjs`
- `functions/scripts/check-storage-rules.js`
- `docs/security/STORAGE_RULES_DYNAMIC_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Storage Rules con firestore.get requieren ambos emuladores

**Error detectado:**
El harness dinámico de Storage terminó con `Cannot determine host and port of firestore` al evaluar reglas que contienen `firestore.exists/get`.

**Causa raíz:**
Se inició únicamente el emulador de Storage, pero su runtime necesita el endpoint de Firestore para resolver dependencias cross-service de las reglas.

**Solución aplicada:**
El comando dinámico inicia `firestore,storage` dentro de la misma ejecución de emuladores.

**Regla futura:**
Todo test de Storage Rules que referencie documentos Firestore debe arrancar ambos emuladores, aunque el escenario concreto parezca cortocircuitar esa rama.

**Archivos afectados:**
- `package.json`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los guardrails estáticos deben tolerar formato equivalente

**Error detectado:**
El check estático de Storage Rules rechazó el harness válido porque buscaba `assert.equal(occurrences,` como texto contiguo y el argumento estaba en la línea siguiente.

**Causa raíz:**
La comprobación de estructura se implementó con un `includes` dependiente del formato en vez de aceptar espacios y saltos de línea.

**Solución aplicada:**
El guardrail usa una expresión regular acotada que valida la llamada y su primer argumento sin depender del layout.

**Regla futura:**
Los checks estáticos de código deben validar invariantes estructurales y tolerar whitespace equivalente; reservar `includes` para literales canónicos exactos.

**Archivos afectados:**
- `functions/scripts/check-storage-rules.js`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Editar archivos legacy activa su deuda completa en el ratchet Detekt

**Error detectado:**
`:shared:detekt` rechazó los archivos modificados por reglas legacy de longitud, nombres Compose y parámetros sin uso, además de los catches que convierten fallos de recuperación en estados tipados.

**Causa raíz:**
Al retirar una supresión existente y ampliar wiring legacy, los archivos volvieron a entrar en el ratchet con deuda previa; las excepciones del coordinador se transforman deliberadamente en resultados de recuperación.

**Solución aplicada:**
Se restauraron y localizaron las supresiones estrictamente necesarias en funciones/archivos afectados y se documentó el catch tipado del coordinador.

**Regla futura:**
Al modificar un archivo legacy, preservar sus supresiones localizadas y ejecutar Detekt inmediatamente; toda nueva supresión debe quedar limitada al archivo o función cuya API o flujo la haga inevitable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/home/presentation/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los smoke tests autenticados no pueden asumir Firebase inicializado

**Error detectado:**
El test de navegación autenticada falló al montar `MindOSApp` porque `HomeScreen` construye repositorios Firebase concretos y el runtime desktop no tenía `FirebaseApp` inicializada.

**Causa raíz:**
Se asumió que inyectar `AppGraph` aislaba todo el árbol autenticado, aunque `HomeScreen` todavía crea colaboradores Firebase fuera del grafo.

**Solución aplicada:**
La entrada visible de ajustes se extrajo a un composable puro y se probó su callback sin inicializar Firebase; el flujo de borrado se mantiene cubierto por el test dedicado de Settings.

**Regla futura:**
No montar el árbol autenticado completo en tests desktop hasta que todos sus colaboradores Firebase estén inyectados; probar componentes puros o inyectar explícitamente cada dependencia externa.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/home/presentation/HomeScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/home/presentation/HomeAccountActionsTest.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/MindOSAppNavigationSmokeTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los defaults mutables rompen la igualdad estructural de AppGraph

**Error detectado:**
`AppGraphTest.graph_isEquatable_dataClass` falló porque cada construcción de `AppGraph` creaba un `InMemoryPendingAccountDeletionStore` distinto como valor por defecto.

**Causa raíz:**
Se introdujo una dependencia mutable con una instancia nueva en un parámetro por defecto de un data class cuya igualdad estructural está fijada por tests.

**Solución aplicada:**
El default se sustituyó por un singleton no-op estable; producción sigue inyectando el journal cifrado y los tests de borrado inyectan un store en memoria explícito.

**Regla futura:**
Los parámetros por defecto de `AppGraph` deben tener identidad estable; toda dependencia mutable o stateful debe inyectarse explícitamente desde producción o desde el test.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Un release publicable nunca puede usar firma debug

**Error detectado:**
`assembleRelease` y `bundleRelease` generaban artefactos con el keystore debug cuando faltaban las propiedades de firma, dejando una ruta publicable protegida solo por un warning.

**Causa raíz:**
Se mezcló la validación R8 de CI con la generación del artefacto publicable y se consideró suficiente advertir del fallback en logs.

**Solución aplicada:**
La variante `release` ahora falla durante configuración cuando se solicita un artefacto publicable sin las cuatro propiedades de firma; CI omite ese bundle si no tiene secretos y el workflow de publicación exige evidencias externas aprobadas.

**Regla futura:**
Ningún build type publicable puede caer a firma debug. Separar o saltar validaciones de release sin secretos y exigir fallo cerrado en `assembleRelease`/`bundleRelease`.

**Archivos afectados:**
- `androidApp/build.gradle.kts`
- `.github/workflows/kotlin-ci.yml`
- `.github/workflows/release.yml`
- `docs/release/RELEASE_EVIDENCE.json`
- `scripts/check-release-evidence.mjs`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Firebase Functions 6 cambia el entrypoint TypeScript a v2

**Error detectado:**
Al subir `firebase-functions` de 4.9.0 a 6.6.0 para cerrar CVE transitivas, el typecheck dejó de resolver `CallableContext`, `runWith`, `firestore.document` y `storage.object`; los wrappers de tests también cambiaron de firma.

**Causa raíz:**
La línea 6 expone v2 desde el entrypoint principal, mientras este backend está implementado y probado sobre la API v1. La compatibilidad de Node y peer dependencies no garantiza compatibilidad del API importado.

**Solución aplicada:**
Se seleccionó `firebase-functions` 5.1.1, última línea compatible con los imports v1 actuales, se mantuvo `firebase-admin` 12.7.0 por su peer range y se actualizaron las transitivas vulnerables del lockfile dentro de rangos compatibles.

**Regla futura:**
Antes de subir `firebase-functions` a 6+, migrar explícitamente todos los imports a `firebase-functions/v1` o los triggers a v2 en una tarea dedicada; validar `npm run typecheck` además de peer dependencies y audit.

**Archivos afectados:**
- `functions/package.json`
- `functions/package-lock.json`
- `docs/security/DEPENDENCY_HARDENING_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los gates Gradle del mismo worktree deben ejecutarse en serie

**Error detectado:**
`:shared:desktopTest` produjo 539 fallos `NoClassDefFoundError` y después no pudo regenerar el snapshot de classpath al ejecutarse a la vez que `:shared:detekt` sobre el mismo directorio `shared/build`.

**Causa raíz:**
Dos invocaciones Gradle independientes escribieron simultáneamente los outputs de compilación KMP del mismo worktree. La carrera dejó clases y snapshots incrementales incompletos; el primer reintento conservó ese estado corrupto.

**Solución aplicada:**
Se detuvieron los daemons, se limpió `:shared` y se repitió `:shared:desktopTest` en serie, sin build/configuration cache y con compilación incremental desactivada; los 549 tests pasaron.

**Regla futura:**
No ejecutar en paralelo tareas Gradle que compartan worktree y directorios `build`; serializar `detekt`, compilación y tests. Si aparece una cascada de clases ausentes o snapshots ilegibles, detener daemons, limpiar el módulo y revalidar sin caches antes de cambiar código.

**Archivos afectados:**
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — SQLDelight custom Boolean requiere adapter explícito

**Error detectado:**
`:shared:desktopTest` falló al cambiar `safety_flag` a `INTEGER AS Boolean`: el código generado intentó importar `Boolean` como tipo custom y exigía un adapter.

**Causa raíz:**
Se asumió que SQLDelight mapearía automáticamente `INTEGER AS Boolean` al primitivo Kotlin en este setup. En realidad, esa sintaxis activa un tipo custom y cambia el constructor generado de la base.

**Solución aplicada:**
Se mantuvo `safety_flag` como `INTEGER NOT NULL CHECK (safety_flag IN (0, 1))` y se conservó el mapper explícito `Long`/`Boolean` en el repositorio.

**Regla futura:**
No usar `AS Boolean` en SQLDelight sin comprobar el adapter generado; para flags simples, preferir `INTEGER CHECK (0, 1)` con mapper explícito si no se quiere cambiar el wiring de la base.

**Archivos afectados:**
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DiaryEntry.sq`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/storage/SqlDelightRepositories.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — SQLDelight añade Queries al nombre del archivo .sq

**Error detectado:**
`:shared:desktopTest` falló porque los repositorios importaban `DecisionSessionQueries` y `DiaryEntryQueries`, pero SQLDelight generó `DecisionSessionQueriesQueries` y `DiaryEntryQueriesQueries`.

**Causa raíz:**
Los archivos de esquema se nombraron `DecisionSessionQueries.sq` y `DiaryEntryQueries.sq`, sin considerar que SQLDelight deriva la clase de queries añadiendo el sufijo `Queries` al nombre base del archivo.

**Solución aplicada:**
Se renombraron los esquemas a `DecisionSession.sq` y `DiaryEntry.sq`, manteniendo las queries internas y los repositorios con imports limpios.

**Regla futura:**
Al crear archivos SQLDelight, nombrar el `.sq` por entidad o agregado, no con sufijo `Queries`, para evitar clases generadas con `QueriesQueries`.

**Archivos afectados:**
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DecisionSession.sq`
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DiaryEntry.sq`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/storage/SqlDelightRepositories.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — Estados nuevos deben cerrar UI y ratchet Detekt

**Error detectado:**
`:shared:desktopTest` falló porque `LanguageStage.MENU` no estaba cubierto en `LanguageRootScreen`; después, `:shared:detekt` rechazó nuevas fórmulas por complejidad, números mágicos, nombres de constructor requeridos y exceso de funciones.

**Causa raíz:**
Se añadió un estado de flujo sin actualizar todos los `when` exhaustivos. Además, las ecuaciones públicas de Alpha Core copiaban una especificación matemática con muchas constantes y nombres obligatorios sin adaptar el archivo al ratchet Detekt del repo.

**Solución aplicada:**
Se añadió la rama exhaustiva `MENU` sin wiring nuevo de UI, se localizó la supresión de `TooManyFunctions` del holder y se normalizaron/suprimieron de forma localizada las reglas Detekt inevitables en las fórmulas y plantillas de Alpha Core.

**Regla futura:**
Al introducir un nuevo estado de UI, actualizar todos los `when` exhaustivos antes de compilar; al portar fórmulas de especificación, declarar constantes o supresiones locales antes de ejecutar Detekt.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/ui/LanguageRootScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/LanguageSessionStateHolder.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/domain/Models.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/scoring/ScoringEquations.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/adaptive/DifficultyAdapter.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/mdp/bayes/BayesEngine.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/mdp/reversib/ReversibilityEngine.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/coach/CoachEngine.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Los ports Compose deben adaptarse a extensiones de scope y al ratchet Detekt

**Error detectado:**
La primera compilación de Personal Identity no resolvió imports de `consume` y `align`; después, Detekt rechazó el entry point requerido `LabScreen()`.

**Causa raíz:**
El paquete externo asumía extensiones importables de otra versión de Compose, aunque en este repo `consume()` es miembro y `align` pertenece a `BoxScope`. Además, el entry point composable con nombre PascalCase no tenía la supresión localizada de `FunctionNaming`.

**Solución aplicada:**
Se retiraron los imports incompatibles y se añadió `@file:Suppress("FunctionNaming")` únicamente al archivo del entry point composable.

**Regla futura:**
Al portar Compose externo, validar imports contra la versión del repo y ejecutar compilación más Detekt; no importar extensiones que sean miembros o de scope, y suprimir `FunctionNaming` localmente en todo archivo que exponga composables PascalCase.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/PersonalIdentityFeature.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/signature/SignatureCaptureBox.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/signature/SignatureOverlay.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — El contexto de seguridad y locale debe llegar intacto al matcher

**Error detectado:**
El gateway descartaba el sujeto de abuso, `es-US` no encontraba alias `es` y `configuración` falló porque la normalización no elimina acentos.

**Causa raíz:**
Los DTOs intermedios no transportaban todo el contexto, el locale exigía coincidencia exacta y se asumió incorrectamente plegado de diacríticos.

**Solución aplicada:**
Se propagó session/source/abuse context, se añadió fallback BCP-47 y se declararon alias acentuados y no acentuados.

**Regla futura:**
Conservar contexto de seguridad en cada límite; usar fallback BCP-47 y no asumir que la normalización elimina acentos.

**Archivos afectados:**
- `mindos/mind-command-runtime/src/commonMain/kotlin/com/mindos/command/runtime/TextCommandGateway.kt`
- `mindos/mind-action-matching/src/commonMain/kotlin/com/mindos/action/matching/Indices.kt`

### 2026-06-15 — Todo cierre negativo de confirmación debe revocar el token canónico

**Error detectado:**
Rechazo por voz, cambio de acción y expiración limpiaban estado Android sin consumir la confirmación canónica.

**Causa raíz:**
Se trataron como transiciones terminales de UI y no como revocaciones de una capacidad de seguridad.

**Solución aplicada:**
Todas las ramas terminales no ejecutadas llaman a `cancelConfirmation`.

**Regla futura:**
No cerrar visualmente una confirmación sin revocar primero su token en la autoridad canónica.

**Archivos afectados:**
- `mindos/mind-android-voice/src/main/kotlin/com/mindos/android/voice/AndroidConfirmationFlow.kt`

### 2026-06-15 — BuildConfig personalizado requiere habilitación explícita en AGP 9

**Error detectado:**
El build raíz falló al añadir `MINDOS_SPEECH_BROKER_URL` mediante `buildConfigField`.

**Causa raíz:**
AGP 9.1 no genera `BuildConfig` si la feature no se activa expresamente.

**Solución aplicada:**
Se habilitó `android.buildFeatures.buildConfig = true`.

**Regla futura:**
Antes de añadir `buildConfigField` con AGP 9+, habilitar `buildFeatures.buildConfig` y compilar el módulo raíz.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

### 2026-06-15 — fetch de Node requiere un BodyInit explícito para buffers

**Error detectado:**
El typecheck falló al enviar un `Buffer` multipart al `fetch` global de Node 20.

**Causa raíz:**
Los tipos actuales no aceptan `Buffer<ArrayBufferLike>` directamente como `BodyInit`.

**Solución aplicada:**
El multipart se convierte con `Uint8Array.from(...)` antes de `fetch`.

**Regla futura:**
Convertir buffers binarios a un `BodyInit` explícito y ejecutar `npm run typecheck`.

**Archivos afectados:**
- `functions/src/speechBroker.ts`

### 2026-06-15 — La telemetría no debe imprimir correladores crudos

**Error detectado:**
`SpeechTelemetryEvent.toString()` incluía `requestId` y `sessionId` sin redacción.

**Causa raíz:**
La allowlist del evento no cubría su representación textual de depuración.

**Solución aplicada:**
`toString()` redacta ambos correladores y una prueba evita regresiones.

**Regla futura:**
Revisar también `toString()` y serializaciones auxiliares; nunca imprimir correladores crudos.

**Archivos afectados:**
- `mindos/mind-speech/src/commonMain/kotlin/com/mindos/speech/SpeechTelemetry.kt`
- `mindos/mind-speech/src/commonTest/kotlin/com/mindos/speech/SpeechTelemetryPrivacyTest.kt`

<!-- Aprendizajes añadidos automáticamente por el agente tras corregir errores reales. -->

### 2026-05-26 — Queries `list` sin `request.query.limit` permiten amplificación de coste

**Error detectado:**
Cuatro reglas `allow list` en `firestore.rules` no enforzaban `request.query.limit`:
`dm_chats`, `dm_chats/.../messages`, `user_posts/{uid}/posts`, `feeds/{viewerId}/posts`.
Un atacante (incluso autorizado, con app modificada o token comprometido) podía leer toda la colección de una sentada, amplificando coste de lectura en Firestore y abusando del presupuesto del proyecto.

**Causa raíz:**
Al diseñar reglas para colecciones donde el acceso ya está restringido (participantes, owner), se asumió que la restricción de acceso era suficiente. La autorización limita *quién* lee, no *cuánto* lee. Sin `request.query.limit`, la app cliente — o un atacante que se hace pasar por ella — puede solicitar la colección completa.

**Solución aplicada:**
Añadido `request.query.limit != null && request.query.limit <= N` a las cuatro reglas. Límites:
- `dm_chats`: 50
- `dm_chats/.../messages`: 100
- `user_posts/{uid}/posts`: 100
- `feeds/{viewerId}/posts`: 100

**Regla futura:**
TODA regla `allow list` debe incluir validación de `request.query.limit` independientemente de quién pueda leer. La autorización (quién) y el límite (cuánto) son controles ortogonales — ambos son necesarios.

Plantilla mínima para cualquier nueva `allow list`:
```
allow list: if request.query.limit != null &&
            request.query.limit <= MAX_LIMIT &&
            <condiciones de autorización>;
```

**Archivos afectados:**
- `firestore.rules` (líneas ~1610, ~1627, ~1639, ~1648)
- `firestore.security.rules.test.mjs` (tests T49-T52)

### 2026-05-26 — App Check no enforcado en uploads y lecturas de `dm_media`

**Error detectado:**
En `storage.rules`, las funciones `canCreateDMMedia` y `canAccessDMResource` solo validaban `signedIn()` pero NO llamaban a `isAppChecked()`. En cambio, `profile_visuals` y `post_attachments` privados sí lo enforzaban a través de `isOwner()` → `isAppChecked()`.

Un cliente modificado (APK repackaged, app sin App Check) podía subir y descargar archivos de DM saltándose la attestation del dispositivo. La consistencia de seguridad entre paths Storage estaba rota.

**Causa raíz:**
`dm_media` se diseñó con su propio sistema de validación de metadata (participantA, participantB, chatId, ownerUid) y se asumió que la validación de metadata era suficiente. Pero App Check es un control ortogonal al de autorización — uno valida el dispositivo, el otro al usuario. Ambos son necesarios.

**Solución aplicada:**
Añadido `isAppChecked()` a ambas funciones:
```
function canAccessDMResource(chatId) {
  return signedIn() && isAppChecked() && ...
}
function canCreateDMMedia(chatId) {
  return signedIn() && isAppChecked() && ...
}
```

**Regla futura:**
TODA operación de Firebase Storage que escriba o lea contenido privado debe enforzar App Check. La regla mínima:
```
function canDoX() {
  return signedIn() && isAppChecked() && <validación de autorización>;
}
```

Excepción documentada: lectura de contenido público (visibility=public) puede no requerir App Check si la app sirve estos archivos como CDN público. Pero contenido privado SIEMPRE debe enforzarlo.

Antes de añadir un nuevo `match` block en `storage.rules`, verificar que la función de autorización llamada incluya `isAppChecked()`.

**Archivos afectados:**
- `storage.rules` (funciones `canAccessDMResource` y `canCreateDMMedia`)
- `docs/security/SELF_AUDIT_BRUTAL.md` (hallazgos S01 y S02)

### 2026-05-27 — Build cache corrupto por referencia a proyecto externo (NEUv3)

**Error detectado:**
`./gradlew :androidApp:assembleDebug` fallaba con `mergeLibDexDebug FAILED` porque Gradle intentaba dexificar un `.class` en `C:\Users\curro\AndroidStudioProjects\NEUv3\shared\build\...`, que está fuera del directorio raíz de MindOS.

**Causa raíz:**
El cache de build intermedio de Gradle contenía una referencia antigua a un proyecto previo (`NEUv3`) del que MindOS fue probablemente renombrado o copiado. El directorio `shared/build/intermediates/runtime_library_classes_dir/` tenía artefactos con rutas absolutas al proyecto anterior.

**Solución aplicada:**
`./gradlew clean` seguido de rebuild. El clean elimina los artefactos intermedios y el rebuild regenera todo con las rutas correctas.

**Regla futura:**
Cuando un build falle con rutas absolutas que apuntan a un directorio fuera del proyecto, ejecutar `./gradlew clean` antes de investigar más. Es un síntoma de cache corrupto, no de un error en el código.

**Archivos afectados:**
- `shared/build/` (directorio de build, limpiado)

### 2026-05-27 — Design tokens dispersos por feature en vez de centralizados

**Error detectado:**
275 literales `Color(0xFF...)` dispersos en 30 archivos. Tokens locales duplicados en `SearchVisualTokens.kt` y `ViewerDesign.kt` con los mismos colores (`#13ECA4`, `#F4FFF9`, `#020504`) definidos independientemente.

**Causa raíz:**
Cada feature creó sus propios tokens de color de forma aislada. No existía un design system centralizado. Los valores eran correctos (mismos colores) pero no tenían una fuente de verdad única.

**Solución aplicada:**
Creados archivos centralizados en `shared/src/commonMain/kotlin/com/example/mindos/design/`:
- `AppColors.kt` — tokens de color (brand, neutral, semantic, state)
- `AppSpacing.kt` — tokens de espaciado (escala 4dp)
- `AppShapes.kt` — tokens de radio/forma
- `AppElevation.kt` — tokens de elevación
- `AppAnimation.kt` — tokens de duración de animación
- `AppStrings.kt` — constantes de strings de UI
- `components/StateComponents.kt` — componentes compartidos (Loading, Error, Empty, Offline)

**Regla futura:**
NUNCA crear tokens visuales locales a un feature. Todos los colores, espaciados, radios, elevaciones y strings deben definirse en los archivos centralizados del paquete `design/`. Si un feature necesita un valor que no existe, añadirlo al archivo central correspondiente.

Antes de añadir un `Color(0xFF...)` en cualquier archivo, verificar que el color no exista ya en `AppColors.kt`. Si existe, usar la referencia. Si no existe, añadirlo a `AppColors.kt` primero.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppSpacing.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppShapes.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppElevation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppAnimation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppStrings.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/components/StateComponents.kt` (NUEVO)

### 2026-05-27 — applicationId no debe ser com.example.* en producción

**Error detectado:**
El `applicationId` era `com.mindos.app`, que es un ID de ejemplo rechazado por Google Play Store y que colisiona con otros proyectos ejemplo.

**Causa raíz:**
El template de proyecto de Android Studio genera `com.example.*` como placeholder. No se cambió al configurar el proyecto para producción.

**Solución aplicada:**
Cambiado `applicationId` a `com.neu.app` en `androidApp/build.gradle.kts`. Actualizado `google-services.json` para que el `package_name` coincida. El `namespace` se mantiene como `com.mindos.app` (solo afecta a R class generation, no a identidad en Play Store).

**Regla futura:**
El `applicationId` y el `namespace` son conceptos independientes en AGP. El `applicationId` identifica la app en la tienda y en Firebase. El `namespace` genera la clase R y no necesita coincidir. Al cambiar `applicationId`, SIEMPRE actualizar `google-services.json` y registrar el nuevo ID en Firebase Console.

**Archivos afectados:**
- `androidApp/build.gradle.kts`
- `androidApp/google-services.json`

### 2026-05-27 — Signing config debe usar variables de entorno, nunca hardcoded

**Error detectado:**
No existía configuración de signing para release builds. Sin signing, no se puede publicar en Play Store ni distribuir APKs de release.

**Causa raíz:**
No se configuró signing al crear el proyecto. Es un paso que se omite frecuentemente hasta que se necesita publicar.

**Solución aplicada:**
Añadido `signingConfigs.create("release")` que lee credenciales de `gradle.properties` vía `findProperty()`. Las propiedades son:
- `MINDOS_KEYSTORE_FILE` — ruta al .jks
- `MINDOS_KEYSTORE_PASSWORD`
- `MINDOS_KEY_ALIAS` (default: "neu")
- `MINDOS_KEY_PASSWORD`

Si el keystore no existe, el release build cae al debug signing config. Esto permite builds en CI sin keystore (para validación) y builds firmados cuando las credenciales están disponibles.

**Regla futura:**
NUNCA incluir rutas de keystore ni contraseñas en archivos versionados. Siempre usar `findProperty()` o variables de entorno. El fallback a debug signing permite que CI compile release sin fallar.

> Nota 2026-06-19: la parte que permitía fallback a firma debug queda reemplazada por la regla fail-closed anterior; CI no debe producir un artefacto `release` si no dispone de firma real.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

### 2026-05-28 — Mensajes de error de auth revelan si un email existe (account enumeration)

**Error detectado:**
`AuthDiagnostics.toUserMessage()` devolvía mensajes distintos para `UserNotFound` ("No existe ningun usuario") y `WrongPassword` ("Contrasena incorrecta"). Un atacante podía distinguir si un email está registrado probando contraseñas incorrectas.

**Causa raíz:**
Se mapearon los códigos de error de Firebase directamente a mensajes de usuario sin considerar que la diferencia entre "no existe" y "contraseña incorrecta" filtra información.

**Solución aplicada:**
Unificados `UserNotFound`, `WrongPassword`, `InvalidLoginCredentials` y `MalformedCredential` bajo un único mensaje genérico: "Email o contrasena incorrectos." Eliminada la interpolación de `$email` en mensajes de credenciales inválidas.

**Regla futura:**
Los mensajes de error de autenticación NUNCA deben revelar si un email está registrado. Todos los fallos de credenciales (usuario no encontrado, contraseña incorrecta, credenciales inválidas) deben usar el mismo mensaje genérico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`

### 2026-05-28 — Android sin network_security_config (cleartext permitido)

**Error detectado:**
No existía `network_security_config.xml`. Sin esta configuración, Android permite tráfico cleartext (HTTP) en versiones anteriores a API 28, y no se declara explícitamente la política de seguridad de red.

**Causa raíz:**
Omisión al crear el proyecto. Android Studio no genera esta configuración por defecto.

**Solución aplicada:**
Creado `androidApp/src/main/res/xml/network_security_config.xml` con `cleartextTrafficPermitted="false"` y trust-anchors limitados a certificados del sistema. Referenciado en `AndroidManifest.xml` vía `android:networkSecurityConfig`.

**Regla futura:**
Todo proyecto Android que haga networking DEBE tener un `network_security_config.xml` que prohíba cleartext explícitamente. Si se necesita una excepción para un dominio de desarrollo, usar `<domain-config>` específico, nunca habilitar cleartext globalmente.

**Archivos afectados:**
- `androidApp/src/main/res/xml/network_security_config.xml` (NUEVO)
- `androidApp/src/main/AndroidManifest.xml`

### 2026-05-28 — updateMessage acepta campos arbitrarios sin whitelist

**Error detectado:**
`FirebaseDMRepository.updateMessage()` aceptaba un `Map<String, Any?>` y escribía todos los campos a Firestore sin validación. Un caller malintencionado o un bug podrían sobrescribir campos protegidos como `senderId` o `createdAt`.

**Causa raíz:**
Se diseñó `updateMessage` como una operación genérica flexible sin considerar que los campos actualizables deben estar restringidos por la capa de dominio.

**Solución aplicada:**
Añadida whitelist `AllowedMessageUpdateFields = setOf("status", "attachmentUrl", "thumbnailUrl")` en el companion object. `updateMessage` valida con `require` que todos los campos estén en la whitelist antes de escribir.

**Regla futura:**
Toda operación de update a Firestore debe validar los campos contra una whitelist explícita. Nunca pasar `Map<String, Any?>` sin restricciones a operaciones de escritura.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Faltaba implementación actual de WaveMailboxIcon para iOS

**Error detectado:**
`WaveMailboxIcon` tenía `expect` en commonMain y `actual` en androidMain y desktopMain, pero no en iosMain. Cualquier build iOS fallaría por falta de `actual`.

**Causa raíz:**
Al crear el composable expect/actual, se omitió la implementación iOS. Como no hay módulo iosApp que compile el target, el error no se detectó.

**Solución aplicada:**
Creado `WaveMailboxIcon.ios.kt` en iosMain con implementación usando `Icons.Default.MailOutline` como fallback (consistente con el fallback del actual de Android).

**Regla futura:**
Cada `expect` declaration DEBE tener `actual` en TODOS los targets declarados en `build.gradle.kts`, incluyendo los que aún no se compilan regularmente. Al crear un nuevo expect/actual, verificar que existan actuals en: androidMain, desktopMain, iosMain.

**Archivos afectados:**
- `shared/src/iosMain/kotlin/com/example/mindos/features/waves/presentation/WaveMailboxIcon.ios.kt` (NUEVO)

### 2026-05-28 — Unsafe smart casts en LaunchedEffect de App.kt (race condition)

**Error detectado:**
El bloque `LaunchedEffect(sessionRepository)` en `App.kt` usaba `(route as AppRoute.X)` repetidamente para acceder a propiedades de la ruta actual. Como `route` es una variable mutable, podía cambiar entre dos accesos consecutivos a `route as`, causando un `ClassCastException` en runtime.

**Causa raíz:**
Se usó la variable mutable `route` directamente dentro del `when` y después se hizo cast sobre ella de nuevo fuera del `when`. El compilador no puede garantizar que el valor no haya cambiado entre la evaluación del `when` y el uso posterior.

**Solución aplicada:**
Capturar `route` en una variable local `val currentRoute = route` al inicio del `collect`, y usar `currentRoute` en todas las ramas del `when`. Kotlin smart-cast funciona correctamente con variables locales inmutables.

**Regla futura:**
Antes de hacer smart cast sobre una propiedad mutable (var, MutableState, etc.), SIEMPRE capturarla en un `val` local. Nunca hacer cast directo sobre `route` o cualquier estado mutable compartido.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`

### 2026-05-28 — Backup rules no excluían cache de Firebase ni tokens

**Error detectado:**
`backup_rules.xml` y `data_extraction_rules.xml` solo excluían `sharedpref` y `dm_audio/`. Los directorios de Firebase (databases, crashlytics NDK, installation tokens) se incluían en backups, exponiendo potencialmente tokens de sesión y datos de cache.

**Causa raíz:**
Al configurar las reglas de backup se pensó solo en datos de la app, no en los directorios internos que Firebase crea automáticamente.

**Solución aplicada:**
Añadidos exclusiones para `database` (dominio completo), `com.google.firebase.crashlytics-ndk`, `generate_fid.lock`, y `PersistedInstallation` en ambos archivos XML.

**Regla futura:**
Las reglas de backup DEBEN excluir todos los directorios internos de Firebase y tokens de sesión. Al añadir un nuevo SDK de Firebase, verificar si crea directorios locales que necesiten exclusión.

**Archivos afectados:**
- `androidApp/src/main/res/xml/backup_rules.xml`
- `androidApp/src/main/res/xml/data_extraction_rules.xml`

### 2026-05-28 — Sin Crashlytics: crashes en producción no se capturaban

**Error detectado:**
No había integración de Firebase Crashlytics. Cualquier crash en producción se perdía sin registro centralizado.

**Causa raíz:**
Omisión al configurar Firebase. Se integró Auth, Firestore, Storage y App Check, pero no Crashlytics.

**Solución aplicada:**
Añadido `firebase-crashlytics` y `firebase-analytics` al BOM en `androidApp/build.gradle.kts`. Añadido plugin `com.google.firebase.crashlytics` v3.0.4. Analytics es necesario como dependencia de Crashlytics.

**Regla futura:**
TODO proyecto Firebase en producción DEBE tener Crashlytics habilitado. Al añadir Firebase a un proyecto, incluir Crashlytics desde el inicio, no como paso posterior.

**Archivos afectados:**
- `gradle/libs.versions.toml`
- `build.gradle.kts` (root)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Shared module no puede depender directamente de Crashlytics SDK

**Error detectado:**
Se intentó añadir `FirebaseCrashlytics.getInstance().recordException(error)` directamente en `SafeDebugLogging.android.kt` (que vive en `:shared`). Falló con "Unresolved reference 'crashlytics'" porque el SDK de Crashlytics solo es dependencia de `:androidApp`, no de `:shared`.

**Causa raíz:**
El módulo `:shared` es KMP y sus dependencias deben ser multiplataforma o estar disponibles en todos los targets. El Crashlytics SDK es Android-only y vive en `:androidApp`. Referenciar clases de `:androidApp` desde `:shared` invierte la dirección de dependencias.

**Solución aplicada:**
Creado `NonFatalReporter` (singleton con callback nullable) en `SafeDebugLogging.kt` (commonMain). `safeErrorLog()` invoca el callback si está registrado. `MindOSApplication.kt` registra Crashlytics como handler solo en release: `NonFatalReporter.handler = { crashlytics.recordException(it) }`.

**Regla futura:**
El módulo `:shared` NUNCA debe depender de SDKs Android-only. Para integrar funcionalidad de `:androidApp` en código compartido, usar el patrón callback/interface: definir la abstracción en commonMain, registrar la implementación concreta desde la capa de aplicación en el `onCreate()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt`
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt`

### 2026-05-28 — iOS GoogleService-Info.plist apuntaba a proyecto Firebase incorrecto

**Error detectado:**
`GoogleService-Info.plist` contenía `PROJECT_ID=mindos-4aef3` y `BUNDLE_ID=com.mindos.app`, pero el proyecto de producción es `mindos-8452f` y el bundle ID correcto es `com.neu.app`. Solo la app Android estaba registrada en el proyecto correcto.

**Causa raíz:**
Al crear el proyecto se generó un plist desde un proyecto Firebase de desarrollo/test (`mindos-4aef3`). Cuando se migró Android al proyecto de producción (`mindos-8452f`), el plist iOS no se actualizó.

**Solución aplicada:**
Registrada app iOS en Firebase project `mindos-8452f` con bundle `com.neu.app` vía Firebase API. Reemplazado `GoogleService-Info.plist` con el generado por el proyecto correcto.

**Regla futura:**
Todos los archivos de configuración de Firebase (google-services.json y GoogleService-Info.plist) deben apuntar al MISMO `PROJECT_ID`. Al cambiar de proyecto Firebase, actualizar AMBOS archivos. Verificar que `BUNDLE_ID` coincida con `applicationId`.

**Archivos afectados:**
- `shared/src/iosMain/resources/GoogleService-Info.plist`

### 2026-05-28 — Operaciones Firestore sin timeout (bloqueo indefinido)

**Error detectado:**
Ninguna llamada a Firestore (`.get()`, `.runTransaction()`, `.updateFields()`) tenía timeout. Una operación podía bloquearse indefinidamente si el dispositivo estaba offline sin cache disponible.

**Causa raíz:**
Se dependió del timeout implícito del SDK de Firebase, que es muy largo o inexistente para ciertas operaciones. No se implementó una capa de timeout a nivel de aplicación.

**Solución aplicada:**
Creado `FirestoreDefaults.kt` con tiempos estándar (ReadTimeout=15s, WriteTimeout=20s, BatchTimeout=30s) y funciones `withFirestoreReadTimeout`/`withFirestoreWriteTimeout`. Aplicados en `FirebaseUsernameSearchRepository` y `FirebaseDMRepository`.

**Regla futura:**
Toda operación Firestore suspendible (reads, writes, transactions) debe ejecutarse dentro de `withFirestoreReadTimeout` o `withFirestoreWriteTimeout`. Los Flows (snapshots) no necesitan timeout porque son streams continuos con su propio manejo de errores.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/search/data/FirebaseUsernameSearchRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Transacciones multi-documento necesitan BatchTimeout, no WriteTimeout

**Error detectado:**
Al envolver transacciones de Firestore que escriben a 3+ documentos (e.g. `observe` en `FirebaseSocialInteractionRepository` que toca observationRef + observedProfileRef + notificationRef), usar `WriteTimeout=20s` por defecto puede ser demasiado corto en redes lentas. Las transacciones son retried automáticamente por Firestore si hay contention.

**Causa raíz:**
`withFirestoreWriteTimeout` usa `FirestoreDefaults.WriteTimeout` (20s) como default. Las transacciones que tocan múltiples documentos pueden necesitar más tiempo debido a retries internos del SDK por contention optimista.

**Solución aplicada:**
Para transacciones multi-doc, pasar `FirestoreDefaults.BatchTimeout` (30s) explícitamente:
```kotlin
withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {
    firestore.runTransaction { /* multi-doc operations */ }
}
```

**Regla futura:**
Las transacciones single-doc usan `withFirestoreWriteTimeout {}` (default 20s). Las transacciones que tocan 3+ documentos o que hacen reads previos antes de writes deben usar `withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {}` (30s) para tolerar retries por contention.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/FirebaseSocialInteractionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`

### 2026-05-28 — Lógica de navegación inline en App.kt es difícil de testear

**Error detectado:**
`App.kt` tenía un bloque `when` de 24 líneas dentro de `LaunchedEffect` para rebindear el `AuthenticatedUser` actual a la ruta activa cuando el `authState` emitía. Estaba inline en un composable, no tenía tests, y duplicaba lógica de construcción de cada `data class` de `AppRoute`.

**Causa raíz:**
Cuando se diseñó la observación de `authState`, la lógica de rebinding se metió directamente en el composable como un detalle de implementación. No se extrajo a una función pura porque "es navegación, va aquí". Resultado: regresiones en el rebinding (e.g. omitir un campo de un `DMChatUserInfo`) solo se detectaban en runtime.

**Solución aplicada:**
Extraído a `AppRoute.rebindWithUser(user): AppRoute` en `core/navigation/AppRouteRebinder.kt` (commonMain). Tests exhaustivos en `AppRouteRebinderTest` (14 tests cubriendo todas las variantes de `AppRoute`). `App.kt` ahora llama a `currentRoute.rebindWithUser(user)`.

**Regla futura:**
Toda lógica de transformación de rutas (rebinding por auth, mapeo de parent route para back navigation, etc.) debe extraerse a una extensión pura de `AppRoute` en `core/navigation/`. Si la lógica está dentro de un composable y depende solo de `AppRoute` + datos primitivos, NO pertenece al composable — pertenece a una función testeable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteRebinder.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteRebinderTest.kt` (NUEVO)

### 2026-05-28 — Navegación sin back stack: botón back físico de Android no funcionaba

**Error detectado:**
`App.kt` usaba `var route by mutableStateOf()` como única fuente de verdad de la ruta actual. No había historial. El botón físico/gesture back de Android no estaba conectado a nada de la app — invocaba el comportamiento por defecto (cerrar la activity), atrapando al usuario en pantallas profundas (DMCajonera, MediaViewer) sin forma de salir excepto cerrar la app.

**Causa raíz:**
La navegación se diseñó como un simple state holder en lugar de una pila. Cada `onBack` reconstruía la ruta destino manualmente con `route = AppRoute.X(currentRoute.user)`, lo cual:
1. Duplicaba lógica en cada pantalla.
2. No respondía al back hardware.
3. Perdía el contexto previo (e.g. MediaViewer abierto desde DMChat volvía a Main).

**Solución aplicada:**
1. `AppRouteStack` (commonMain) — clase observable con `push/pop/replace/replaceAll/popTo/rebindStackWithUser`. Respaldada por `SnapshotStateList` para recomposición automática.
2. `PlatformBackHandler(enabled, onBack)` — expect/actual: Android usa `androidx.activity.compose.BackHandler`; desktop/iOS son no-op.
3. Refactorizado `App.kt` — cada `onBack = { stack.pop() }`, cada navegación profunda usa `stack.push()`, sign-out usa `stack.replaceAll(AppRoute.Auth)`.
4. Tests: 12 tests en `AppRouteStackTest` cubren todas las operaciones del stack.

**Regla futura:**
NUNCA usar `var route by mutableStateOf()` directamente para navegación. Toda app con más de 2 pantallas debe usar un back stack explícito. Reglas:
- `push()` cuando vas más profundo (Main → DMList).
- `replace()` cuando swap al mismo nivel (post-login routing).
- `replaceAll()` cuando reset total (sign-out, deep link entry).
- `pop()` para back.
- Wirear SIEMPRE `PlatformBackHandler(enabled = stack.canGoBack, onBack = { stack.pop() })` cuando hay un stack activo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteStack.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.kt` (NUEVO expect)
- `shared/src/androidMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.android.kt` (NUEVO actual)
- `shared/src/desktopMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.desktop.kt` (NUEVO actual)
- `shared/src/iosMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.ios.kt` (NUEVO actual)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteStackTest.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (refactorizado completo)

### 2026-05-28 — POST_NOTIFICATIONS no declarado (Android 13+ no podía notificar)

**Error detectado:**
`AndroidManifest.xml` no declaraba `android.permission.POST_NOTIFICATIONS`. Con `targetSdk=36`, cualquier intento de mostrar una notificación local o FCM en Android 13+ (API 33+) sería ignorado silenciosamente — el SDK requiere runtime permission desde Android 13.

**Causa raíz:**
Omisión al configurar el manifest. POST_NOTIFICATIONS no existía pre-Android 13 (los install-time grants cubrían todo), así que en proyectos creados antes era invisible.

**Solución aplicada:**
1. Añadido `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` al manifest.
2. Creado `NotificationPermissionState` (commonMain) — modelo puro con reglas de gating (`shouldShowPrompt`, `shouldDirectToSettings`).
3. Creado `rememberNotificationPermissionHandle()` (androidMain) — wrap de `ActivityResultContracts.RequestPermission` con detección de API 33+.
4. Tests: 9 tests cubriendo todas las combinaciones (pre-13, granted, denied, dismissed, permanent denial).

**Regla futura:**
Todo `targetSdk >= 33` con cualquier feature de notificaciones (local, FCM, foreground service notifications) DEBE:
1. Declarar `POST_NOTIFICATIONS` en AndroidManifest.
2. Solicitar permiso en momento **contextual** (cuando el usuario activa la feature), NUNCA al arrancar la app.
3. Manejar el caso "permanent denial" redirigiendo a system settings.

Antes de añadir cualquier nueva integración que muestre notificaciones, verificar que el feature invoque `rememberNotificationPermissionHandle().requestIfNeeded()` antes de la primera notificación.

**Archivos afectados:**
- `androidApp/src/main/AndroidManifest.xml`
- `shared/src/commonMain/kotlin/com/example/mindos/core/notifications/NotificationPermissionGate.kt` (NUEVO)
- `shared/src/androidMain/kotlin/com/example/mindos/core/notifications/NotificationPermission.android.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/notifications/NotificationPermissionGateTest.kt` (NUEVO)

### 2026-05-28 — Release sin keystore real fallaba silenciosamente al debug signing

**Error detectado:**
La lógica de signing tenía fallback a debug `signingConfigs.getByName("debug")` cuando no había `MINDOS_KEYSTORE_FILE`, pero NO emitía ningún log. Era posible ejecutar `./gradlew assembleRelease` y obtener un APK firmado con debug sin darse cuenta. Si alguien subía ese APK a Play Store, sería rechazado o (peor) aceptado pero no recibiría updates por mismatch de firma futura.

**Causa raíz:**
El fallback silencioso se diseñó para que CI compilase release sin keystore (para validar R8/ProGuard), pero el mismo path se ejecutaba en desarrollo local sin warning. La intención (CI build) y el efecto secundario (dev build sin firma real) eran indistinguibles.

**Solución aplicada:**
1. Añadido `logger.warn("⚠️ androidApp:release is being signed with the DEBUG keystore...")` cuando el fallback se activa. Visible en cada build, imposible de ignorar.
2. Creado `gradle.properties.example` documentando las 4 variables (`MINDOS_KEYSTORE_FILE`, `MINDOS_KEYSTORE_PASSWORD`, `MINDOS_KEY_ALIAS`, `MINDOS_KEY_PASSWORD`) + comando `keytool` para generar el keystore + ejemplo de GitHub Actions con `MINDOS_KEYSTORE_B64`.
3. `.gitignore` actualizado para excluir `*.jks`, `*.keystore`, `key.properties`, `keystore.properties`, `/androidApp/keystore/`.

**Regla futura:**
Todo fallback de configuración crítica para seguridad (signing, encryption keys, API tokens) DEBE emitir un warning visible en el build log. El warning debe explicar:
- Qué configuración falta.
- Cuál fue la decisión (qué se usa en su lugar).
- Por qué eso NO sirve para producción.
- Dónde está la documentación para resolverlo.

> Nota 2026-06-19: para variantes publicables esta regla queda reemplazada: no se permite fallback aunque emita warning; debe fallar cerrado.

**Archivos afectados:**
- `.gitignore`
- `gradle.properties.example` (NUEVO)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Scaffold iosApp sin Xcode project committed

**Error detectado:**
No existía estructura `iosApp/` para consumir el shared KMP framework en iOS. Sin esto, aunque la app estaba registrada en Firebase (`mindos-8452f` con bundle `com.neu.app`), no había forma de compilarla.

**Causa raíz:**
El proyecto se empezó priorizando Android y desktop. iOS requiere infraestructura adicional (Xcode project, Podfile, Swift entry point) que solo es construible/verificable en macOS.

**Solución aplicada:**
Scaffold completo de `iosApp/` con:
- `iosApp/iosApp/iOSApp.swift` — `@main` SwiftUI app que monta `ComposeView` (UIViewControllerRepresentable) hospedando `MainViewControllerKt.MainViewController()`.
- `iosApp/iosApp/Info.plist` — HTTPS-only (`NSAllowsArbitraryLoads=false`, espejo del `network_security_config` de Android), modo oscuro forzado, descripciones de Camera/Microphone/Photos.
- `iosApp/Podfile` — referencia el shared module via `pod 'shared', :path => '../shared'` + Firebase pods (Core, Auth, Firestore, Storage, Functions, AppCheck, Crashlytics).
- `iosApp/.gitignore` — excluye `Pods/`, `*.xcworkspace`, `xcuserdata/`, `build/`, `DerivedData/`.
- `iosApp/README.md` — instrucciones de setup macOS paso a paso.
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` — entry point Kotlin que devuelve `UIViewController` envolviendo `App()`.

`iosApp.xcodeproj` NO se commit — se genera per-machine al ejecutar `pod install` la primera vez en macOS.

**Regla futura:**
Cuando un módulo de un proyecto multiplataforma requiere un toolchain que no está disponible en la máquina del autor (Xcode en Windows, MSVC en Linux, etc):
1. Crear todo lo scaffoldable (fuentes, configs, manifests).
2. NUNCA crear archivos de proyecto binarios/UUID a mano (xcodeproj, *.sln).
3. Documentar exhaustivamente los pasos para el primer setup en la plataforma objetivo.
4. Marcar como "verification deferred to <plataforma>" en el aprendizaje correspondiente.

La verificación real de que iosApp compila queda DIFERIDA a la primera vez que se abra en macOS — esto es un riesgo aceptado y explícito.

**Archivos afectados:**
- `iosApp/iosApp/iOSApp.swift` (NUEVO)
- `iosApp/iosApp/Info.plist` (NUEVO)
- `iosApp/Podfile` (NUEVO)
- `iosApp/.gitignore` (NUEVO)
- `iosApp/README.md` (NUEVO)
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` (NUEVO)

### 2026-05-28 — Auth y Storage operations sin timeout (bloqueo indefinido)

**Error detectado:**
Solo Firestore tenía timeouts. `signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `signOut`, `sendPasswordResetEmail` y todas las operaciones de Storage (`putData`, `delete`) podían colgarse indefinidamente sin emitir error. Esto se traduce en spinners infinitos en la UI cuando hay problemas de red.

**Causa raíz:**
Cuando se diseñó `FirestoreDefaults.kt` (Wave 4) se cubrió solo el caso de Firestore. Auth y Storage tienen sus propios mecanismos internos de retry/timeout pero ninguno garantiza un tope a nivel de aplicación.

**Solución aplicada:**
Extendido `FirestoreDefaults.kt` con dos nuevas familias de timeouts:
- **AuthDefaults**: SignInTimeout=20s, SignOutTimeout=10s, PasswordResetTimeout=15s, TokenRefreshTimeout=10s + helper `withAuthTimeout`.
- **StorageDefaults**: SmallReadTimeout=30s, LargeReadTimeout=180s, SmallUploadTimeout=60s, LargeUploadTimeout=300s, DownloadUrlTimeout=15s, DeleteTimeout=15s + helpers `withStorageReadTimeout` y `withStorageUploadTimeout`.

Aplicados timeouts en `FirebaseSessionRepository` (sign in/up/out/reset) y en `FirebaseDMRepository` + `ProfileVisualRepositories` (uploads de media discriminados por tipo: video usa Large, image/audio usa Small).

**Regla futura:**
Toda operación suspendible de Firebase (Auth, Firestore, Storage, Functions) DEBE envolverse en `with*Timeout`. Las operaciones de Storage que manejen video o archivos >5MB deben usar el `Large*` timeout explícito. Los flows (snapshots) no necesitan timeout porque son streams continuos.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (NUEVO)

### 2026-05-28 — Crashlytics sin breadcrumbs ni custom keys (crashes sin contexto)

**Error detectado:**
Crashlytics solo capturaba el stack trace. Sin breadcrumbs ni custom keys, era imposible saber en qué pantalla ocurrió un crash, si el usuario estaba autenticado, qué feature flag estaba activo, etc. Triage de crashes en producción se vuelve adivinanza.

**Causa raíz:**
El bridge inicial (Wave 3) solo expuso `NonFatalReporter.handler` para `recordException`. No había forma desde el módulo `:shared` de añadir `log()` ni `setCustomKey()`. La capa de observabilidad estaba incompleta.

**Solución aplicada:**
1. Creado `ObservabilitySink` interface (commonMain) con 5 primitivas: `breadcrumb(message)`, `customKey(k, v)` (String/Boolean/Int), `recordNonFatal(error)`.
2. `ObservabilityHub` singleton — sink registrable por la capa de aplicación.
3. `CrashlyticsObservabilitySink` (en `MindOSApplication`) forwarda todas las llamadas a Crashlytics SDK.
4. `AppRoute.telemetryName()` — nombre estable PII-free para cada ruta.
5. En `App.kt`: `LaunchedEffect(stack.current)` emite breadcrumb (`nav_to:dm_chat`) + custom keys (`current_route`, `nav_depth`) en cada cambio de ruta. `LaunchedEffect(sessionRepository)` actualiza `authenticated`.
6. Tests: `ObservabilityHubTest` (3 tests con sink mock), `AppRouteTelemetryTest` (2 tests verificando PII safety + estabilidad).

**Regla futura:**
Cualquier evento que ayude a contextualizar un crash debe pasar por `ObservabilityHub`. Reglas:
- `breadcrumb()` para eventos transitorios (navegación, network state, lifecycle).
- `customKey()` para contexto persistente (current_route, user role, feature flags).
- NUNCA pasar PII directamente. Si necesitas user id, hashearlo en la capa nativa antes de pasar al sink.
- Cada nueva pantalla añadida a `AppRoute` DEBE tener su `telemetryName()` actualizado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/Observability.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt` (route safeErrorLog through hub)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteTelemetry.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (wire LaunchedEffect)
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt` (CrashlyticsObservabilitySink)
- `shared/src/commonTest/kotlin/com/example/mindos/core/logging/ObservabilityHubTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteTelemetryTest.kt` (NUEVO)

### 2026-05-28 — Detekt baseline ocultaba bug latente: self en discovery list

**Error detectado:**
`FirebaseDMRepository.queryProfileVisualUsers(currentUserId: String)` recibía el `currentUserId` pero nunca lo usaba para filtrar al propio usuario de la lista de discovery. Resultado: el usuario se veía a sí mismo como contacto disponible para DM. El parámetro estaba en el baseline de detekt como `UnusedParameter`, escondido como ruido.

**Causa raíz:**
El baseline acumulaba findings indiscriminadamente. `UnusedParameter` y `UnusedPrivateProperty` son a menudo bugs reales (parámetros declarados pero nunca cumplidos) o code smell (dead code). Mezclarlos con `MagicNumber` (1200+ entries cosméticas) impedía detectarlos.

**Solución aplicada:**
1. Auditado el baseline filtrando solo `UnusedParameter` (29) + `UnusedPrivateProperty` (9).
2. Cada finding evaluado caso por caso:
   - **Bugs latentes**: `queryProfileVisualUsers.currentUserId` (no filtraba self), `decodeProfileVisualDiscoveryDto.path` (no se incluía en logs), `resolveUserId.path` (mismo).
   - **Dead code real**: `initialRadius`, `centerOffset`, `lum: Float`, `val h/w = maxHeight/maxWidth`, `causeMessage`, `val ProfileEditButton`, `val ProfileVisualUsernamePattern` (duplicado), `val carol` test fixture.
   - **API surface intencional**: callbacks de `ProfileScreen` (17), `HomeScreen.onOpenSettings`, `GlobalFeedScreen.onBack`, `AuthDiagnostic.toUserMessage.email`, `ChatBubble.unitH`. Suprimidos con `@Suppress` + comentario explicando el contrato.

**Regla futura:**
Detekt baseline es deuda técnica controlada, NO basurero. Categorías de findings que merecen revisión periódica caso-por-caso:
- `UnusedParameter` — frecuentemente bugs latentes
- `UnusedPrivateProperty` — siempre dead code real
- `UnreachableCode` — siempre bug
- `EqualsWithHashCodeExist` — siempre bug
- `UnsafeCallOnNullableType` — siempre riesgo

Categorías que se pueden tolerar en baseline indefinidamente:
- `MagicNumber` (ruidoso, raramente bug)
- `MaxLineLength` (cosmético)
- `WildcardImport` (estilo)

Antes de hacer "ratchet" (regenerar baseline), ejecutar la auditoría filtrada de las categorías de alto valor.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt` (self-filter + logs enriquecidos)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ChromaInteractiveEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/DMChatScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileEntryScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/model/ProfileVisualModels.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/ranking/HomeFeedRankingEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/domain/SocialGraphRulesTest.kt`
- `shared/detekt-baseline.xml` (de 2355 → 2273 entries; UnusedParameter/UnusedPrivateProperty de 38 → 0)

### 2026-05-29 — Cinco dependencias inline en `MindOSApp` dispersaban el wiring

**Error detectado:**
`MindOSApp(...)` aceptaba 5 parámetros con defaults `remember { FQN(...) }` (DMRepository, WalletApiClient, WaveRequestRepository, GlobalFeedRepository, privacyEnabledState). Cada caller (tests, main activity, preview) tenía que conocer las 5 y pasarlas individualmente. Añadir una sexta dependencia rompía la signature en todos los call sites.

**Causa raíz:**
La inyección de dependencias se hacía a nivel de parámetro de composable. Sin agrupación, el compose function se volvía un god-constructor con responsabilidades mezcladas (orquestación + wiring).

**Solución aplicada:**
Creado `AppGraph` (data class @Stable en `core/di/`) con las 5 dependencias agrupadas. `rememberAppGraph()` es la factory composable que construye el wiring de producción. `MindOSApp(...)` ahora recibe un `appGraph: AppGraph = rememberAppGraph()` único parámetro de wiring. Tests construyen un `AppGraph` con fakes y lo pasan.

**Regla futura:**
Toda app con 3+ dependencias inyectables al nivel root debe agruparlas en un `*Graph` data class. Reglas:
- El graph es `@Stable data class` (Compose skipping + equatable).
- Las dependencias son INTERFACES (nunca clases concretas como `FirebaseXyz`).
- La factory composable `rememberAppGraph()` vive junto al graph.
- Tests construyen el graph directamente con fakes; nunca usan `rememberAppGraph()`.

Antes de añadir un nuevo parámetro inyectable a `MindOSApp`, añadirlo al `AppGraph` y a `rememberAppGraph()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/di/AppGraph.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (signature simplificada)
- `shared/src/commonTest/kotlin/com/example/mindos/core/di/AppGraphTest.kt` (NUEVO)
- `shared/src/desktopTest/kotlin/com/example/mindos/MindOSAppNavigationSmokeTest.kt` (usa AppGraph)

### 2026-05-29 — kotlinx-datetime 0.7.1 (pulled transitively) removió Clock.System en runtime

**Error detectado:**
Tests que ejercitan código con `kotlinx.datetime.Clock.System.now()` fallan con `NoClassDefFoundError: kotlinx/datetime/Clock$System` en desktopTest. La versión pinned en `libs.versions.toml` es 0.6.0, pero un compilation-time dep pulled 0.7.1 que removió `Clock.System`.

**Causa raíz:**
kotlinx-datetime 0.7.0 deprecó `kotlinx.datetime.Clock` y movió la API a `kotlin.time.Clock`. La 0.7.1 ya no expone la clase a runtime. Compose Multiplatform / Kotlin 2.3 traen 0.7.1 transitivamente, sobrescribiendo el pin de 0.6.0.

**Solución aplicada:**
Para casos individuales: marcar los tests afectados como NO-COBERTURA documentando la razón. Workaround long-term: inyectar `AppClock` (patrón ya usado por `ProfileStateHolder`) en lugar de llamar `Clock.System.now()` directamente.

**Regla futura:**
TODO uso de tiempo en código de dominio/presentación debe ir a través de `AppClock` inyectado. Llamadas directas a `Clock.System.now()` son aceptables solo en:
- Capa de datos (repos) — usado en seeding de timestamps a Firestore.
- `MindOSApplication.onCreate()` y entry points donde el clock real es lo correcto.

Antes de añadir `Clock.System.now()` a un StateHolder o ViewModel, refactorizar para inyectar `AppClock`.

**Archivos afectados:**
- `shared/build.gradle.kts` (añadida dep explícita de kotlinx-datetime en desktopTest)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (2 tests deshabilitados con documentación)

### 2026-05-29 — Tests de StateHolder con flows infinitos requieren `cancelChildren()` o `backgroundScope`

**Error detectado:**
`DMStateHolderTest` fallaba con `UncompletedCoroutinesError` porque el holder lanza `scope.launch { Flow.collect { } }` que suspende indefinidamente. `runTest` espera a que todos los hijos del scope completen y falla cuando hay jobs activos.

**Causa raíz:**
La convención en kotlinx-coroutines-test 1.10+ es que `runTest` aborta si quedan hijos activos al final del bloque. StateHolders que observan `StateFlow` (chats, messages) lanzan colectores infinitos.

**Solución aplicada:**
Patrón estándar para tests de StateHolders con flows infinitos:
1. Pasar `scope = this` (TestScope) al holder.
2. Al final de cada test que haya invocado `loadX`/`resolveX` que lance jobs persistentes: `coroutineContext.cancelChildren()`.
3. Alternativa: usar `backgroundScope` — pero NO siempre se comporta igual con `advanceUntilIdle` en flujos complejos (multi-launch nested).

**Regla futura:**
Plantilla para tests de StateHolder que lance Flow collectors:
```kotlin
@Test
fun myTest() = runTest {
    val holder = StateHolder(scope = this, ...)
    holder.startObservingStuff()
    advanceUntilIdle()
    // assertions ...
    coroutineContext.cancelChildren()  // ← cleanup
}
```

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/example/mindos/features/dm/presentation/DMStateHolderTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (NUEVO — usa GatedFakeFeedRepository para mid-flight asserts)

### 2026-05-29 — Cloud Functions (wallet) sin timeout y `callSafely` tragaba la cancelación

**Error detectado:**
Las 4 llamadas de `WalletApiClient` (`getBalance`, `getTransactions`, `initiatePurchase`, `verifyPurchase`) no se envolvían en ningún timeout de nivel de app — el único subsistema Firebase sin él, pese a que `FirestoreDefaults.kt` ya cubría Auth/Firestore/Storage. Además, `callSafely` capturaba `catch (e: Exception)` sin re-lanzar `CancellationException`, tragando la cancelación estructural (un job cancelado se mapeaba a `WalletError.NetworkError` en lugar de propagarse).

**Causa raíz:**
Cuando se diseñaron los timeouts (Wave 4/8) solo se cubrieron Auth/Firestore/Storage; Functions se omitió. El `callSafely` se escribió antes de aplicar el patrón "re-lanzar CancellationException" que el resto de repos ya seguían.

**Solución aplicada:**
1. Añadida familia `FunctionsDefaults` (`ReadCallTimeout=15s`, `WriteCallTimeout=20s`) + `withFunctionsReadTimeout`/`withFunctionsWriteTimeout` en `FirestoreDefaults.kt`.
2. Envueltas las 4 llamadas (read en getBalance/getTransactions; write en initiate/verifyPurchase).
3. `callSafely` ahora distingue: `TimeoutCancellationException` → `WalletError.NetworkError` (error recuperable, el servidor deduplica por idempotency key/token); `CancellationException` genuina → re-lanzada; resto → `mapException`.

**Regla futura:**
TODA llamada suspendible a Firebase Cloud Functions (HTTPS callables) debe envolverse en `withFunctionsReadTimeout` (no mutante) o `withFunctionsWriteTimeout` (mutante). Cualquier `catch (e: Exception)` que envuelva trabajo suspendible debe re-lanzar `CancellationException` ANTES del catch genérico; solo `TimeoutCancellationException` puede mapearse a un error de dominio.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/wallet/data/WalletApiClient.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (+5 tests)

### 2026-05-29 — `Clock.System.now()` directo en StateHolder impedía cobertura (datetime 0.7.1)

**Error detectado:**
`GlobalFeedComposerStateHolder.draftId()` llamaba a `kotlinx.datetime.Clock.System.now()` directamente. Por el conflicto kotlinx-datetime 0.7.1 (documentado arriba), dos tests (`onAudioRecorded_enforcesMaxClipsLimit`, `onDeleteAudio_removesClipById`) no se podían escribir porque tropezaban con `NoClassDefFoundError` en el JVM de test.

**Causa raíz:**
La fuente de "ahora" estaba acoplada al SDK frágil en una función de presentación, sin inyección. El workaround "inyectar reloj" ya estaba anotado como deuda pero no implementado.

**Solución aplicada:**
Creada abstracción `core/time/EpochClock` (fun interface `nowMillis(): Long`) + `SystemEpochClock` como ÚNICO punto que toca `Clock.System`. `GlobalFeedComposerStateHolder` recibe `clock: EpochClock = SystemEpochClock`; `draftId` pasó de función top-level a método miembro que usa `clock.nowMillis()`. Los 2 tests antes imposibles ahora corren con un `IncrementingEpochClock` fake.

**Regla futura:**
NINGÚN StateHolder/ViewModel/composable debe llamar `Clock.System.now()` directamente. Inyectar `EpochClock` (epoch millis) o el `AppClock` social (`Timestamp`), con default de producción. La única clase que puede tocar `Clock.System` es `SystemEpochClock`. Para tiempo en capa de datos (seeding de timestamps a Firestore) sigue siendo aceptable el uso directo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/time/EpochClock.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolder.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (+2 tests reactivados)

### 2026-05-29 — Registro de usuario no enviaba email de verificación

**Error detectado:**
`createUserWithEmailAndPassword` capturaba `isEmailVerified` pero NUNCA disparaba `sendEmailVerification()`. Se podían crear cuentas con emails ajenos/inexistentes sin ninguna verificación; no existía forma de reenviar.

**Causa raíz:**
El flujo de registro se diseñó solo para crear la cuenta + perfil; la verificación de email se omitió.

**Solución aplicada:**
`SessionRepository` ahora expone `sendEmailVerification()` y `reloadCurrentUser()`. `createUserWithEmailAndPassword` dispara el email de verificación automáticamente (envuelto en `runCatching` + timeout, para no abortar el registro si el envío falla). `PreviewSessionRepository` implementa no-ops seguros. El gating de acciones por verificación queda como decisión de producto (hoy NO bloquea — banner no implementado, ver `docs/architecture/PHASE2_DEFERRALS.md`).

**Regla futura:**
Todo flujo de registro con email/contraseña debe disparar `sendEmailVerification()` tras crear la cuenta, sin bloquear el registro si el envío falla. Al añadir una nueva implementación de `SessionRepository`, implementar `sendEmailVerification()` y `reloadCurrentUser()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/SessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/PreviewSessionRepository.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/session/SessionRepositoryContractTest.kt` (+3 tests)

### 2026-05-29 — Queries cliente de DM sin `.limit()` eran denegadas por las propias reglas

**Error detectado:**
`FirebaseDMRepository.getUserChats` y `getChatMessages` ejecutaban `.where/.orderBy.snapshots` SIN `.limit()`. Las reglas de `dm_chats` (`allow list` exige `request.query.limit != null && <= 50`) y de `messages` (`<= 100`) deniegan toda query con `request.query.limit == null`. Resultado: la lista de chats y los mensajes nunca cargarían en producción (PERMISSION_DENIED) — la feature de DM rota de extremo a extremo.

**Causa raíz:**
Cuando se endurecieron las reglas con límites de query (aprendizaje 2026-05-26) NO se actualizaron las queries cliente para fijar el `.limit()` correspondiente. Desfase regla↔cliente latente, invisible porque iOS es un shell de preview y el flujo nunca se ejercitó contra las reglas desplegadas.

**Solución aplicada:**
`getUserChats` → `.limit(ChatsPageLimit=50)` (50 chats más recientes, orderBy updatedAt DESC). `getChatMessages` → `orderBy(createdAt, DESCENDING).limit(MessagesPageLimit=100)` + `.reversed()` en el `map` (los 100 mensajes más recientes, mostrados cronológicamente como antes). Constantes con nota de que deben mantenerse <= los topes de `firestore.rules`.

**Regla futura:**
TODA query `list`/`snapshots` cliente sobre una colección cuya regla exige `request.query.limit` DEBE fijar `.limit(N)` con N <= el tope de la regla. Al cambiar un límite en `firestore.rules`, actualizar la query cliente en el mismo cambio (y viceversa). Validación real requiere el emulador de reglas (Fase 5 T5.1) — la verificación aquí fue solo de compilación.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-29 — Estado "en línea" fabricado (isOnline=true) mostraba presencia inexistente

**Error detectado:**
`App.kt` pasaba `isOnline = true` hardcodeado al abrir el info del chat, y `AppRoute.DMChatUserInfo`/`DMCajonera` + `DMChatUserInfoRoute` tenían `isOnline` con default `true`. La UI (`WavesChatUserInfoScreen`) pinta un punto verde "en línea" cuando `isOnline==true`. No existe ningún sistema de presencia → se mostraba a los usuarios un estado de presencia falso (UX engañosa + implica tracking que no ocurre).

**Causa raíz:**
Se usó `true` como placeholder de presencia sin backend que lo respaldara. Un placeholder se convirtió en un claim visible y falso.

**Solución aplicada:**
Cambiado el `true` fabricado en `App.kt` y todos los defaults de `isOnline` a `false`. La presencia real (RTDB/Firestore presence + ocultar el indicador cuando es desconocida) es una feature con backend, diferida.

**Regla futura:**
NUNCA fabricar valores que la UI presenta como hechos verificados (presencia, "visto", contadores en vivo). Si no hay fuente de datos, el default debe ser el conservador que NO afirme la capacidad. Los placeholders de UI no deben renderizarse como estado real.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRoute.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/WavesChatUserInfoScreen.kt`

### 2026-05-29 — Scripts de tests de reglas no eran ejecutables (sin --project; emulador exige JDK 21)

**Error detectado:**
Los scripts `test:*-rules` de `package.json` llamaban `firebase emulators:exec` SIN `--project`, y no existe `.firebaserc`. Sin proyecto activo, el emulador falla con "No currently active project". Además, el script combinado `test:rules` corría los 3 archivos `.mjs` bajo UN solo emulador, pero cada archivo fija un `projectId` distinto y `firebase.json` tiene `singleProjectMode: true` → conflicto. Y `firebase-tools` 15 exige **JDK 21+**, incompatible con el JDK 17 del build Gradle.

**Causa raíz:**
Los scripts se escribieron asumiendo un proyecto activo local. Nunca corrieron en CI, así que el desfase (project + singleProjectMode + versión de Java) quedó latente.

**Solución aplicada:**
Cada script ahora pasa su `--project` correspondiente (igual al `projectId` del `.mjs`) y corre en su PROPIO emulador; `test:rules` los encadena (3 emuladores secuenciales, no uno compartido). Nuevo job CI `backend-ci.yml` corre el emulador con **temurin 21**. La suite de Functions (704 tests jest) sí se verificó localmente.

**Regla futura:**
Los scripts `emulators:exec` DEBEN pasar `--project <projectId>` que coincida con el `initializeTestEnvironment({projectId})` del test. Con `singleProjectMode`, NUNCA correr tests de varios projectId bajo un solo emulador: uno por proyecto. El emulador de Firebase requiere JDK 21+ (el build Kotlin usa 17) — separar versiones de Java por job en CI.

**Archivos afectados:**
- `package.json` (scripts test:*-rules)
- `.github/workflows/backend-ci.yml` (NUEVO)

### 2026-05-30 — `.gitignore` `lib/` sin anclar excluía TODO el backend `functions/src/lib`

**Error detectado:**
`functions/.gitignore` tenía `lib/` (patrón sin anclar). gitignore aplica patrones sin barra inicial a CUALQUIER nivel, así que `lib/` no solo ignoraba `functions/lib/` (output compilado, intencional) sino también `functions/src/lib/` → los **19 archivos fuente del backend** (guards, rateLimiter, ledgerService, walletRepository, validators, fraudSignalService, reconciliationService, idempotencyService, etc.) **nunca se commitearon**. El repo no podía compilar/desplegar las Cloud Functions; un clone limpio fallaría. `accountDeletion.ts` (sí trackeado) importaba de `./lib/` inexistente en el repo.

**Causa raíz:**
Patrón gitignore sin anclar (`lib/` en vez de `/lib/`). El compilado vive en `functions/lib/` y el fuente en `functions/src/lib/`; ambos terminan en `lib/`, así que el patrón sin anclar capturó los dos.

**Solución aplicada:**
Cambiado `lib/` → `/lib/` en `functions/.gitignore` (ancla a `functions/lib/` únicamente). Añadidos al repo los 19 fuentes de `functions/src/lib/`. Verificado: `git check-ignore` ya no ignora `src/lib`, sigue ignorando `functions/lib/` compilado; `scan:secrets` limpio; jest 713 verde; build OK.

**Regla futura:**
En `.gitignore`, los patrones de output compilado que coincidan con un nombre de dir común (`lib`, `build`, `dist`, `out`) DEBEN anclarse con barra inicial (`/lib/`, `/build/`) para no capturar dirs homónimos de código fuente. Tras tocar un `.gitignore` de output, ejecutar `git check-ignore -v <ruta-fuente>` sobre una ruta de código para confirmar que NO queda ignorada, y `git ls-files <dir>` para confirmar que los fuentes están trackeados.

**Archivos afectados:**
- `functions/.gitignore`
- `functions/src/lib/*.ts` (19 archivos, ahora trackeados)
- `functions/src/accountDeletion.ts` + `accountDeletion.test.ts` (NUEVO — CF de borrado de cuenta B1)
- `functions/src/index.ts`, `functions/src/lib/rateLimiter.ts`

### 2026-05-30 — `gradlew` sin bit de ejecución rompía TODO el CI Linux (Permission denied)

**Error detectado:**
Todos los jobs Gradle del CI (assembleDebug, bundleRelease, detekt, desktopTest) fallaban en ~8 s con `./gradlew: Permission denied` (exit 126). El archivo `gradlew` estaba commiteado con modo `100644` (sin `+x`). En Windows (máquina del autor) se usa `gradlew.bat` y el bit es irrelevante, así que el fallo era invisible localmente.

**Causa raíz:**
`gradlew` se añadió al repo sin el bit de ejecución. En runners Linux, `./gradlew` necesita `+x`.

**Solución aplicada:**
`git update-index --chmod=+x gradlew` (modo → `100755`). En checkout Windows el bit es cosmético; git lo almacena y Linux lo recibe ejecutable.

**Regla futura:**
Todo script de entrada ejecutable en CI Linux (`gradlew`, `*.sh`) DEBE commitearse con `100755`. Verificar con `git ls-files -s <script>`; si es `100644`, `git update-index --chmod=+x`. Síntoma en CI: fallo en segundos con "Permission denied" / exit 126.

**Archivos afectados:**
- `gradlew` (modo 100644 → 100755)

### 2026-05-30 — Test de reglas positivo (T49) no reflejaba la query real → denegado correctamente

**Error detectado:**
`firestore.security.rules.test.mjs` T49 ("DM chats list with limit succeeds") hacía `getDocs(query(collection(alice,"dm_chats"), limit(50)))` SIN el filtro `where participants array-contains`. La regla `dm_chats allow list` exige `uid() in resource.data.participants`, así que el emulador denegaba la query (seguridad funcionando). El test esperaba `assertSucceeds` → fallo (1/81). Nunca se había detectado porque el CI de reglas no corría (gradlew/JDK rotos).

**Causa raíz:**
El test positivo de una `list` no replicaba las constraints que la regla exige; una `list` sin el `where` que acota a los docs permitidos es denegada por Firestore (no puede garantizar que todo doc devuelto cumpla la regla).

**Solución aplicada:**
Añadido `where("participants","array-contains","alice")` a T49 (espejo de `FirebaseDMRepository.getUserChats`). Importado `where`.

**Regla futura:**
Un test positivo (`assertSucceeds`) de una `list` con regla per-documento (`uid() in resource.data.X`) DEBE incluir el mismo `where`/filtro que la regla exige y que usa el cliente de producción. Si no, el emulador deniega legítimamente. Mantener los tests de reglas espejo de las queries cliente reales.

**Archivos afectados:**
- `firestore.security.rules.test.mjs` (T49 + import `where`)

### 2026-05-30 — `by rememberUpdatedState(...)` sin `import androidx.compose.runtime.getValue` no compila

**Error detectado:**
Varios actuals de plataforma nuevos (`ProfileAudioRecorder.desktop.kt`, `ProfileAudioPlayer.desktop.kt`, `ProfileForegroundEffect.desktop.kt`, etc.) fallaban con `Property delegate must have a 'getValue(Nothing?, KProperty0<*>)' method` y `Unresolved reference 'currentOnError'` al usar `val currentOnError by rememberUpdatedState(onError)`.

**Causa raíz:**
`rememberUpdatedState` devuelve un `State<T>`. El operador de delegación `by` sobre `State<T>` requiere la función de extensión `getValue` de `androidx.compose.runtime.getValue`. Importar solo `rememberUpdatedState`/`mutableStateOf` NO trae ese operador. En archivos `commonMain` grandes suele entrar transitivamente por otros imports, lo que enmascara la necesidad en archivos pequeños (actuals de plataforma).

**Solución aplicada:**
Añadir `import androidx.compose.runtime.getValue` en TODO archivo que delegue con `by` sobre Compose state (`mutableStateOf`/`rememberUpdatedState`/`collectAsState`). Para `by var` también hace falta `androidx.compose.runtime.setValue`.

**Regla futura:**
Al escribir un archivo nuevo (sobre todo actuals de plataforma pequeños) que use `by` sobre Compose state, importar SIEMPRE `androidx.compose.runtime.getValue` (y `setValue` si es `var`). No confiar en imports transitivos.

**Archivos afectados:**
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/presentation/**`

### 2026-05-30 — Git worktree sin `local.properties`: build Android falla con "SDK location not found"

**Error detectado:**
`:androidApp:assembleDebug` en el worktree (`.claude/worktrees/...`) fallaba con `SDK location not found. Define a valid SDK location with an ANDROID_HOME ... or sdk.dir`. El target Desktop compilaba bien (no necesita SDK Android).

**Causa raíz:**
`local.properties` está en `.gitignore` (per-máquina) y NO se copia al crear un git worktree. Sin `ANDROID_HOME`/`ANDROID_SDK_ROOT` en el entorno, AGP no localiza el SDK. Además, escribir `sdk.dir` con backslashes Windows sin escapar (`C:\Users\...`) hace que Java los lea como escapes → `IOException: ... sintaxis ... no son correctos`.

**Solución aplicada:**
Crear `local.properties` en la raíz del worktree con barras NORMALES (Java las acepta en Windows): `sdk.dir=C:/Users/curro/AppData/Local/Android/Sdk`. Queda gitignored (verificado con `git check-ignore`).

**Regla futura:**
Tras crear un git worktree, para builds Android crear `local.properties` con `sdk.dir` usando `/` (no `\`). Verificar que sigue ignorado. Síntoma: "SDK location not found" SOLO en Android; Desktop compila igual. Copiar el `sdk.dir` del checkout principal.

**Archivos afectados:**
- `local.properties` (worktree, no versionado)

### 2026-05-30 — Nueva pantalla de perfil MindOS (Perfil.html) como feature aislada

**Error detectado:**
No era un error: decisión de arquitectura al implementar el diseño `Perfil.html`. El `ProfileScreen` existente (`features/social/presentation/profile`) es un sistema complejo (chroma, módulos, segmented Portada/Perfil, publicaciones reales) divergente del nuevo diseño premium.

**Causa raíz:**
Reescribir en sitio habría arriesgado el editor chroma/módulos y sus tests. El diseño nuevo introduce conceptos que el modelo viejo no tiene (emoji prefix, audio con waveform real, donación, 3 enlaces custom, presencia online/offline).

**Solución aplicada:**
Feature nueva y autocontenida en `features/profile/` (domain/data/presentation/audio) con repos in-memory (`MindOSProfileRepository`, `ProfileObserverRepository`, `ProfilePresenceRepository`). `HomeScreen` renderiza `MindOSProfileScreen` en `HomeDestination.Profile`; el `ProfileEntryScreen` viejo permanece en el repo (sin usar). Imágenes reutilizan `rememberProfileMediaPicker`/`ProfilePlatformMediaPreview` del feature social. Audio real en Android (`MediaRecorder` + `getMaxAmplitude` para waveform real), stubs documentados en desktop/iOS.

**Regla futura:**
Para rediseños grandes que divergen de un sistema existente y tienen tests, preferir feature nueva aislada + un único punto de wiring (aquí `HomeDestination.Profile`) en vez de reescribir en sitio. Reutilizar utilidades multimedia compartidas (picker/preview) en vez de duplicar expect/actual. Inyectar abstracciones de plataforma (audio/permiso/presencia) por expect/actual con Android real + stubs documentados.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/profile/**` (NUEVO)
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/**` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt` (wiring)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (tokens ProfileCanvas/ProfileTextPrimary/ProfileOnline)

### 2026-05-31 — `List<T> + (a..b)` resuelve al overload `plus(Iterable)` y expande el rango

**Error detectado:**
En el `TokenTrie` del nuevo módulo `mind-action-matching` (proyecto `mindos/`), `spans + (index..index)` con `spans: List<IntRange>` no compilaba: `Argument type mismatch: actual type is 'List<Any>', but 'List<IntRange>' was expected`. La intención era añadir el `IntRange` como UN elemento.

**Causa raíz:**
`IntRange` implementa `Iterable<Int>`. El operador `+` sobre `List<IntRange>` tiene dos overloads: `plus(element: T)` y `plus(elements: Iterable<T>)`. Como `(index..index)` ES un `Iterable<Int>`, Kotlin resuelve a `plus(Iterable)`, que CONCATENA los `Int` del rango en vez de añadir el rango como elemento → lista mixta `List<Any>`.

**Solución aplicada:**
Envolver el elemento en `listOf(...)`: `spans + listOf(index..index)`. Fuerza el overload `plus(Collection)` que concatena el rango como un único elemento.

**Regla futura:**
NUNCA usar `lista + elementoIterable` cuando el tipo de elemento es a su vez `Iterable` (`IntRange`/`CharRange`, `String`, otra `List`/`Set`). Kotlin elige `plus(Iterable)` y expande el elemento. Para añadir un elemento que es Iterable: `lista + listOf(elemento)` o `lista.plusElement(elemento)`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonMain/kotlin/com/mindos/action/matching/TokenTrie.kt`

### 2026-05-31 — Tests asumían strip+match de zero-width; la capa de seguridad lo rechaza

**Error detectado:**
Un test de regresión (REG-003) en `mind-action-matching` asumía que un input con un carácter zero-width (`​`) inyectado se **normalizaba (strip) y matcheaba** igual (`"open set​tings"` → `Understood(open_settings)`). Falló: el matcher devolvía `Rejected`, no `Understood`.

**Causa raíz:**
La capa de seguridad (`mind-security`, integrada en `DefaultActionMatcher`) inspecciona el input ANTES del matching y **rechaza** caracteres invisibles/format (zero-width, control) como defensa anti-spoofing/homoglyph. El comportamiento correcto pasó de "strip silencioso" (asunción de la fase de normalización) a "rechazo explícito tipado" (decisión de seguridad), que es una garantía más fuerte. La asunción del test quedó obsoleta tras integrarse la capa de seguridad.

**Solución aplicada:**
Actualizado REG-003 para fijar el comportamiento real y seguro: input con zero-width → `Rejected` o `NoMatch` (nunca un `Understood` espurio). El assert acepta ambos resultados seguros.

**Regla futura:**
Al escribir tests sobre el matcher, recordar que `DefaultActionMatcher` aplica **inspección de seguridad antes de normalizar/matchear**: inputs con caracteres invisibles/control/homoglyph se **rechazan** (no se sanean silenciosamente). Nunca asumir strip+match para Unicode sospechoso; asumir rechazo tipado. Los tests de robustez deben aceptar `Rejected`/`NoMatch` como el resultado seguro correcto, no `Understood`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonTest/kotlin/com/mindos/action/matching/RegressionSuiteTest.kt`

### 2026-06-04 — Lapsos contados sobre RT ya filtrados por MAD (el filtro borra justo los lapsos)

**Error detectado:**
En `DefaultMetricsEngine.blockMetrics` (MindBoost Enfoque), los lapsos se contaban sobre `cleanRts` (RT de aciertos ya filtrados por MAD). Un test con RTs `[300,320,600,700,310,330]` y `lapseRtMs=500` esperaba 2 lapsos pero obtenía 0: el rechazo de outliers por MAD eliminaba los RT lentos (600, 700) ANTES de contarlos como lapsos.

**Causa raíz:**
Se reutilizó la lista limpia (post-MAD) para todas las métricas. Pero un lapso es una respuesta legítimamente lenta, no un artefacto de medición. El filtro MAD protege la tendencia central (RT mediano / RT-CV) descartando colas extremas — y esas colas extremas SON los lapsos. Mezclar ambos propósitos hace que el filtro borre la señal que otra métrica necesita.

**Solución aplicada:**
Contar lapsos sobre `rawHitRts` (filtrado solo de anticipaciones, NO de MAD). La tendencia central sigue usando `cleanRts`; el conteo de lapsos usa los crudos. Ambos coexisten.

**Regla futura:**
El rechazo de outliers (MAD/SD) se aplica SOLO a métricas de tendencia central (mediana, CV, IES). Las métricas que cuentan eventos extremos por definición (lapsos = RT lentos, anticipaciones = RT ultra-rápidos) se calculan sobre los datos crudos (tras filtrar solo la categoría opuesta). Nunca contar un evento extremo sobre una lista de la que se han eliminado los extremos.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/mindboost/focus/domain/metrics/DefaultMetricsEngineTest.kt`

### 2026-06-04 — `kotlin.math.cos/sin` sobre `Float` devuelve `Double` y rompe APIs Compose (Offset/Float)

**Error detectado:**
En `FocusAtmosphere.kt`, `Offset(x = w * (0.30f + 0.10f * cos(drift)), ...)` con `drift: Float` no compilaba: "Argument type mismatch: actual type is 'Double', but 'Float' was expected".

**Causa raíz:**
`kotlin.math.cos`/`sin` solo tienen overload `(Double): Double`. Al pasar un `Float`, Kotlin promueve a `Double` y devuelve `Double`. Las APIs gráficas de Compose (`Offset`, radios, `drawCircle`) esperan `Float`, así que la expresión resultante es `Double` y falla.

**Solución aplicada:**
Convertir explícitamente: `val d = drift.toDouble()` y `cos(d).toFloat()`. Centralizar la conversión una vez por bloque de canvas.

**Regla futura:**
En código gráfico de Compose, toda llamada a funciones de `kotlin.math` (`cos`, `sin`, `sqrt`, `pow`…) dentro de expresiones que alimentan `Offset`/`Size`/radios/`Float` debe envolverse con `.toFloat()`, y el argumento angular pasarse como `Double`. No mezclar literales `0.3f` con resultados de trig sin convertir.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/presentation/ui/components/FocusAtmosphere.kt`

### 2026-06-04 — Feature nueva grande vs ratchet de Detekt: estrategia de findings

**Error detectado:**
Al añadir la feature MindBoost Enfoque (≈45 archivos), `:shared:detekt` falló con ~62 findings nuevos fuera del baseline (MagicNumber, FunctionNaming, LongMethod, LongParameterList, CyclomaticComplexMethod, TooManyFunctions, ReturnCount, UnusedParameter), bloqueando el build por el ratchet.

**Causa raíz:**
El baseline de Detekt solo cubre el código existente. Todo archivo nuevo se evalúa con las reglas por defecto y cualquier finding rompe el build (ratchet one-way).

**Solución aplicada (jerarquía: arreglar > justificar > suprimir idiomático):**
1. **Arreglar de verdad** las categorías de alto valor y estructurales: `UnusedParameter` (era un `template` realmente sin usar — bug latente); `ReturnCount`/`LoopWithTooManyJumpStatements` refactorizados a forma sin múltiples `return`/`continue`; `MaxLineLength` envolviendo; `blockMetrics` (LongMethod) extrayendo `addErrorTallies`. Constructor del StateHolder con 11 params → recibe el `FocusGraph` (4 params).
2. **Nombrar constantes de dominio** reutilizables (`MAD_SCALE`, `PERCENT`, `CV_VERY_STABLE`…).
3. **Suprimir con justificación** lo idiomático: Composables → `@file:Suppress("FunctionNaming","LongMethod","MagicNumber")`; motores con tabla de decisión/estadística densa → `@Suppress("CyclomaticComplexMethod"/"TooManyFunctions"/"MagicNumber")` con comentario; `AppColors` (archivo-token de hex) → `@Suppress("MagicNumber")` a nivel de `object`.

**Regla futura:**
Para una feature nueva grande, NO regenerar el baseline a ciegas. Orden: (1) arreglar `UnusedParameter`/`UnusedPrivateProperty`/`UnreachableCode` SIEMPRE (suelen ser bugs); (2) refactorizar `ReturnCount`/`MaxLineLength`/`LongMethod` cuando es barato; (3) suprimir con `@Suppress` + comentario lo idiomático de Compose/estadística (preferible al baseline porque es local y revisable). Las supresiones de `MagicNumber` son aceptables en: Composables, `data class` de config cuyos defaults SON la constante, enums cuyos valores SON los datos, y el `object AppColors`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/**`
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt`

### 2026-06-05 — `kotlinx-serialization-core` no produce formato string; falta `-json`

**Error detectado:**
Para la persistencia local de MindBoost se necesitaba serializar DTOs a string, pero `commonMain` solo declaraba `kotlinx-serialization-core`. `core` aporta `@Serializable` y los descriptores/`KSerializer`, pero NO un formato: `Json`, `encodeToString`/`decodeFromString` viven en el artefacto separado `kotlinx-serialization-json`, que no estaba.

**Causa raíz:**
El repo usa `@Serializable` solo para la serialización de Firebase (gitlive), que consume los descriptores de `core` sin necesitar un formato. Por eso `core` bastaba hasta ahora y `-json` nunca se añadió.

**Solución aplicada:**
Añadido `kotlinx-serialization-json` (misma versión pineada 1.9.0, multiplataforma) al catálogo y a `commonMain`, con comentario justificando la adición. El plugin `kotlinSerialization` ya estaba aplicado.

**Regla futura:**
Si necesitas (de)serializar a/desde String (JSON), `kotlinx-serialization-core` NO basta: añade `kotlinx-serialization-json` a la MISMA versión. `core` solo sirve cuando otro formato (p. ej. el de Firebase gitlive) consume los descriptores. Antes de escribir `Json { }`/`encodeToString`, verifica que `-json` esté declarado.

**Archivos afectados:**
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`

### 2026-06-05 — Patrón de persistencia local cifrada para datos sensibles (KMP)

**Error detectado:**
No era un bug: decisión de arquitectura/seguridad al persistir datos cognitivos de MindBoost (perfil, baseline, resultados de probe), que son sensibles (pueden inferir rasgos de salud).

**Causa raíz:**
Persistir datos cognitivos requiere cifrado en reposo y aislamiento de la superficie cripto/plataforma para mantener la lógica testeable sin device.

**Solución aplicada:**
Superficie cripto/plataforma MÍNIMA: `expect fun rememberSecureKeyValueStore(namespace): SecureKeyValueStore` (get/put/remove/keys/clearNamespace). Android = AES-256-GCM con clave en Android Keystore (device-bound), **replicando** el patrón ya probado de `PublisherPlatformStore.android.kt` (no se inventó cripto); desktop/iOS = in-memory (no escriben datos sensibles a disco; iOS Keychain diferido). Toda la lógica (DTOs `@Serializable` con `schemaVersion`, mappers, repos `Local*`) vive en `commonMain` y se testea contra `InMemorySecureKeyValueStore`. Solo se persisten **resúmenes** de sesión (sin trials crudos).

**Regla futura:**
Para datos sensibles en KMP: (1) NUNCA inventar cripto — reutilizar el patrón AES/GCM+Keystore existente; (2) aislar plataforma en una superficie KV genérica mínima vía expect/actual, dejando DTOs/mappers/repos en commonMain (testeables con un fake in-memory); (3) versionar DTOs con `schemaVersion` + `Json { ignoreUnknownKeys = true; encodeDefaults = true }`; (4) verificar que las SharedPreferences están excluidas de backups (`backup_rules.xml`/`data_extraction_rules.xml` ya tienen `sharedpref path="."` — cubre cualquier prefs nuevo); (5) la clave del Keystore nunca se respalda (device-bound), doble protección; (6) persistir solo resúmenes, nunca datos crudos de alto volumen.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.kt`
- `shared/src/androidMain/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `shared/src/{desktop,ios}Main/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.*.kt`

### 2026-06-12 — La tarea de compilación Android del módulo KMP es `compileAndroidMain`, no `compileDebugKotlinAndroid`

**Error detectado:**
`./gradlew :shared:compileDebugKotlinAndroid` falló con "task 'compileDebugKotlinAndroid' not found in project ':shared'".

**Causa raíz:**
`:shared` usa el plugin nuevo `com.android.kotlin.multiplatform.library` (AGP 9) declarado vía `kotlin { android { ... } }`. Ese plugin NO genera las tareas por-variante `compile<Variant>KotlinAndroid` del plugin clásico; expone una única compilación `main` con la tarea `compileAndroidMain`.

**Solución aplicada:**
Usar `:shared:compileAndroidMain` para compilar el target Android del módulo compartido. (`:androidApp:assembleDebug` sigue construyendo el APK end-to-end.)

**Regla futura:**
Para compilar solo el target Android de `:shared`, usar `:shared:compileAndroidMain`. Tareas útiles del módulo: `:shared:compileKotlinDesktop` (desktop main), `:shared:desktopTest` (tests JVM), `:shared:compileAndroidMain` (Android main), `:shared:detekt` (ratchet). Si una tarea "no se encuentra", listar con `:shared:tasks --all` antes de asumir nombres del plugin clásico.

**Archivos afectados:**
- (ninguno — comando de build)

### 2026-06-12 — MindBoost Velocidad Mental (MVP-0): persistencia con SecureKeyValueStore, no Room

**Error detectado:**
No es un bug: decisión de arquitectura al implementar MVP-0 de Velocidad Mental. El plan markdown especifica entidades Room, pero el repo no tiene Room/KSP/SQLDelight y su patrón establecido para datos cognitivos sensibles es `@Serializable` + `SecureKeyValueStore` cifrado + repos en commonMain testeables con fakes.

**Causa raíz:**
Añadir Room sobre AGP 9.1.0 / Kotlin 2.3.10 / el plugin KMP-library nuevo es alto riesgo de build, y empujaría los tests de borrado transaccional / limpieza de huérfanos / integridad de persistencia a un emulador, contradiciendo el mandato "simulación primero como suite automatizada" (§12 del plan). El cifrado en reposo de Room tampoco es por defecto.

**Solución aplicada:**
Feature `features/mindboost/speed/` (espejo de `focus/`). Las 8 entidades del plan son data classes `@Serializable` puras (`domain/model/SpeedRecords.kt`); la integridad relacional (referencias por id, cascada, borrado transaccional por usuario, limpieza de `DeviceProfile` huérfanos, versionado) vive en `SpeedMeasurementStore` con dos impls: `InMemorySpeedMeasurementStore` (modelo semántico autoritativo, base de tests/simulador) y `SecureSpeedMeasurementStore` (durable, cifrado vía `SecureKeyValueStore`). Toda la lógica (timebase, métricas, edge-cases, trial engine, calidad, simulación) es commonMain y se testea en `desktopTest` sin dispositivo. La única superficie de plataforma es el `expect rememberSpeedTimebaseHandle`/`SpeedTapArea` (Android = Choreographer + MotionEvent; desktop/iOS = fallback monotónico documentado).

**Regla futura:**
Para una feature de datos cognitivos sensibles en este repo, NO introducir Room/SQLDelight: usar el patrón `@Serializable` + `SecureKeyValueStore` + repos commonMain + fake in-memory. Si un plan externo pide Room, preservar la INTENCIÓN relacional (FK por id, cascada, borrado atómico por usuario, huérfanos, versionado) en una abstracción de repositorio testeable en JVM, y documentar la desviación.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO)
- `shared/src/{android,desktop,ios}Main/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO actuals)
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO, 67 tests)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt` (track Velocidad)

### 2026-06-14 — El build **debug** usa `src/debug/google-services.json` placeholder → no autentica contra Firebase real

**Error detectado:**
El APK debug (`com.mindos.app.debug`) fallaba TODO login email/contraseña con `RecaptchaCallWrapper … [ API key not valid. Please pass a valid API key. ]`, idéntico en cada intento. Se perdió mucho tiempo editando restricciones de la API key, registrando el SHA-1 de debug y desactivando reCAPTCHA en el proyecto `mindos-8452f` — sin ningún efecto.

**Causa raíz:**
El repo commitea `androidApp/src/debug/google-services.json`, un **placeholder** (`project_id: mindos-debug-placeholder`, `project_number: 000000000000`, y una `api_key` claramente falsa con sufijo de equis). El plugin Gradle `google-services` usa el `google-services.json` **específico de variante** (`src/debug/`) con precedencia sobre el de la raíz. Así que el build debug enviaba una API key FALSA a Firebase → `API key not valid`. Todas las ediciones en la consola de `mindos-8452f` eran irrelevantes: el build debug **nunca contacta** ese proyecto. La raíz `androidApp/google-services.json` (key real de producción, paquete `com.mindos.app`) solo la usa el build **release**.

**Solución aplicada:**
Diagnóstico, no fix de código. El build debug, tal cual, **no puede** autenticar contra Firebase real (placeholder trackeado: intencional o setup incompleto). Para autenticar en debug hay que reemplazar `src/debug/google-services.json` por una config real (registrando `com.mindos.app.debug` + SHA-1 de debug en `mindos-8452f` y descargando su `google-services.json`). Diagnóstico clave: un GET de navegador `https://identitytoolkit.googleapis.com/v1/projects?key=<KEY_REAL>` devuelve JSON (key real OK) pero la app debug sigue fallando → la app usa OTRA key (la del override de variante).

**Regla futura:**
Ante un fallo de Firebase Auth/Firestore SOLO en una variante (p. ej. `API key not valid` en debug), ANTES de tocar Google Cloud, revisar si existe `androidApp/src/<variant>/google-services.json` que sobrescriba al de la raíz (el plugin prioriza el de variante). Verificar `project_id`/`api_key` reales (no `…-placeholder` ni una key con sufijo de equis). La validación física §15 que requiere login debe usar un build que SÍ autentique (release con config real, o debug con `src/debug/google-services.json` real); un build debug vanilla se queda en la pantalla de Auth y nunca llega a MindBoost.

**Archivos afectados:**
- `androidApp/src/debug/google-services.json` (placeholder — causa raíz, sin cambios)
- `androidApp/google-services.json` (config real, solo release)
- `docs/mindboost_velocidad_implementation_plan/PHYSICAL_VALIDATION_GATE.md` (caveat añadido)

**Nota de reemplazo (2026-06-14):**
El bloqueo descrito quedó resuelto: `com.mindos.app.debug` ya está registrada en Firebase, el
archivo de variante contiene configuración real y el emulador tiene App Check debug autorizado.
La regla de diagnóstico sobre precedencia por variante sigue vigente.

---

### 2026-06-15 — No deduplicar señales antes de calcular su frecuencia

**Error detectado:**
El progreso de Mindboost Lógica nunca podía marcar la señal de posible impulsividad aunque hubiera
dos o más respuestas impulsivas.

**Causa raíz:**
Los errores se deduplicaban con `distinct()` antes de contar impulsividad, por lo que el recuento
máximo posible era uno.

**Solución aplicada:**
La frecuencia se calcula sobre la lista completa de errores y la lista deduplicada se conserva solo
para reporting. Se añadió una regresión con dos intentos impulsivos.

**Regla futura:**
Cuando una decisión dependa de repetición o frecuencia, contar siempre sobre eventos crudos; aplicar
`distinct()` únicamente después y solo para presentación o almacenamiento resumido.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/adaptive/LogicDifficultyAdapter.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/domain/adaptive/LogicDifficultyAdapterTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Scripts para Windows PowerShell 5.1 deben evitar UTF-8 sin BOM no ASCII

**Error detectado:**
`powershell -File scripts/verify-mindos.ps1 -Full` fallaba al parsear con una cadena sin cerrar,
antes de ejecutar cualquier quality gate.

**Causa raíz:**
El `.ps1` estaba guardado como UTF-8 sin BOM. Windows PowerShell 5.1 lo leyó como ANSI y los bytes
de la `Ó` de `FALLÓ` incluyeron un carácter interpretado como comilla tipográfica, abriendo una
cadena que nunca se cerraba.

**Solución aplicada:**
Se convirtieron los mensajes del script a ASCII para que `powershell -File` los interprete de forma
estable en PowerShell 5.1.

**Regla futura:**
Los scripts `.ps1` ejecutados mediante `powershell.exe` deben usar texto ASCII o UTF-8 con BOM;
no introducir caracteres no ASCII en UTF-8 sin BOM y validar siempre el script con Windows
PowerShell 5.1.

**Archivos afectados:**
- `scripts/verify-mindos.ps1`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — La transferencia isomorfa debe evaluar ambos intentos

**Error detectado:**
El desglose de transferencia de un ítem isomorfo podía guardar `1.0` aunque el usuario fallara el
isomorfo, siempre que hubiera acertado antes el gemelo entrenado.

**Causa raíz:**
El cálculo consultaba únicamente el resultado previo con la misma estructura lógica y no incorporaba
la corrección de la respuesta actual.

**Solución aplicada:**
La consistencia de transferencia solo vale `1.0` cuando tanto el gemelo entrenado como el isomorfo
actual son correctos; cualquier fallo produce `0.0`. Se añadió una regresión de sesión completa.

**Regla futura:**
Toda métrica comparativa o de transferencia debe calcularse con señales de ambos lados de la
comparación; no derivarla únicamente del baseline o intento entrenado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/presentation/LogicStateHolder.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/presentation/LogicStateHolderTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Parentetizar `if` al combinarlo con fórmulas Kotlin

**Error detectado:**
El scorer ICR de Mindboost Lógica solo aplicaba el peso de respuesta final cuando la respuesta era
correcta y omitía los pesos de regla, premisas, justificación, confianza e independencia.

**Causa raíz:**
Se escribió `peso * if (condición) 1.0 else 0.0 + resto` sin paréntesis. En Kotlin, el `else`
absorbió la suma posterior como parte de la expresión condicional, alterando la fórmula.

**Solución aplicada:**
Se parentetizó explícitamente el término condicional:
`peso * (if (condición) 1.0 else 0.0) + resto`.

**Regla futura:**
En fórmulas Kotlin, todo `if` usado como operando debe ir entre paréntesis antes de continuar con
sumas, restas o multiplicaciones; añadir un test numérico del resultado compuesto.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/scoring/ReasoningScorer.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/domain/scoring/ReasoningScorerTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-17 — `Modifier.weight` exige receptor `ColumnScope`/`RowScope` en un composable extraído

**Error detectado:**
`:shared:compileKotlinDesktop` falló con "Unresolved reference 'weight'" en `LanguageRootScreen` al usar
`Modifier.weight(1f)` dentro de un `@Composable private fun MenuView(...)`.

**Causa raíz:**
`Modifier.weight` es una extensión de `ColumnScope`/`RowScope`. Aunque `MenuView` se llama dentro de un
`Column`, al extraerlo a una función composable normal su cuerpo NO tiene el receptor `ColumnScope`, así que
`weight` no resuelve.

**Solución aplicada:**
Declarar el composable como extensión del scope: `private fun ColumnScope.MenuView(...)`. La llamada sigue
estando dentro del `Column { }` del padre, que aporta el receptor.

**Regla futura:**
Todo composable extraído que use `weight` (u otros miembros de `ColumnScope`/`RowScope` como `align`) debe
declararse como extensión `ColumnScope.`/`RowScope.`; no basta con invocarlo dentro de un `Column`/`Row`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/ui/LanguageRootScreen.kt`
- `CLAUDE.md`

### 2026-06-18 — Ejercicios de Enfoque implementados pero inalcanzables (presentación cableada a E1)

**Error detectado:**
El catálogo de Enfoque declaraba 13 ejercicios y existían 13 generadores de dominio completos y
testeados (E1–E13), pero solo E1 era seleccionable: 12 ejercicios implementados quedaban invisibles
en el entrenador.

**Causa raíz:**
La tubería de presentación estaba acoplada a un único ejercicio: `FocusGraph` exponía solo
`e1Generator`/`e1Definition`, `FocusSessionStateHolder` estaba hardwired a E1, `FocusHomeScreen` no
tenía menú de selección y `ExerciseCatalog.implemented` listaba solo E1. No era un filtro silencioso
(el gate era correcto): era integración de presentación incompleta, sin red anti-regresión que atara
generador ↔ `implemented` ↔ renderer.

**Solución aplicada:**
`FocusExerciseGenerators.registry` (mapa id→generador) como única fuente de verdad de lo wired;
`FocusGraph` parametrizado (`generators` + `definitionFor`/`generatorFor`); `FocusSessionStateHolder`
y la ruta de sesión parametrizados por `ExerciseId`; menú en `FocusHomeScreen` driven por
`implemented`; se expuso E3 (go/no-go, render-compatible con el campo single-tap actual). Se añadieron
`domainReady` y `pendingUiIntegration` al catálogo y un `ExerciseCatalogConsistencyTest` que rompe el
build si un generador render-compatible se termina sin exponerlo, o si se expone un id sin
generador/renderer.

**Regla futura:**
Para baterías de ejercicios no exponer por catálogo: usar un registry id→generador como única fuente
de verdad, parametrizar session holder + navegación por id, y un test que ate `implemented` ⟺
`registry.keys`. Distinguir "dominio listo" de "UI alcanzable" (`domainReady` vs
`implemented`/`pendingUiIntegration`): un generador con tests NO equivale a un ejercicio jugable hasta
tener renderer + captura de respuesta. Solo exponer paradigmas compatibles con el renderer existente
(E1/E3 single-tap); elección/espacial/stream requieren su propio renderer. Al cambiar `implemented`,
revisar los defaults que dependen de él (p. ej. `DefaultDailyPlanGenerator`) y sus tests.

**Archivos afectados:**
- `shared/.../focus/domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` (NUEVO)
- `shared/.../focus/di/FocusGraph.kt`
- `shared/.../focus/presentation/FocusSessionStateHolder.kt`
- `shared/.../focus/presentation/{FocusHomeStateHolder,RememberFocusStateHolders}.kt`
- `shared/.../focus/presentation/state/FocusUiState.kt`
- `shared/.../focus/presentation/ui/{FocusHomeScreen,FocusRootScreen}.kt`
- `shared/.../focus/domain/catalog/ExerciseCatalogConsistencyTest.kt` (NUEVO)
- `shared/.../focus/presentation/FocusHomeStateHolderTest.kt` (NUEVO)
- `shared/.../focus/domain/session/DailyPlanGeneratorTest.kt`
- `CLAUDE.md`

### 2026-06-18 — Integración del paradigma de dos alternativas (E2/E5) en Enfoque

**Error detectado:**
Al integrar ejercicios de elección (flanker E2, Stroop/Simon E5) en el runtime de Enfoque, el
`FocusSessionAssembler` clasificaba SIEMPRE con `TrialClassifier.classify` (go/no-go) desde un
`RecordedTrial(stimulus, latencyMs)` que no llevaba la tecla pulsada. En tareas de dos alternativas,
acertar depende de QUÉ lado se eligió, no de si se respondió: toda respuesta en ventana se habría
contado como acierto y el efecto de congruencia nunca habría salido.

**Causa raíz:**
El runtime/ensamblador se diseñaron para el slice E1 (single-tap). `classifyChoice` existía en el
dominio pero nunca se cableó: faltaba transportar la tecla de respuesta hasta el ensamblador, que es
la autoridad de clasificación (los contadores en vivo del holder son secundarios).

**Solución aplicada:**
`ResponseKey.isDirectional` (LEFT/RIGHT) marca un trial de elección; `RecordedTrial` gana
`responseKey`; `assembleBlock` despacha a `classifyChoice` cuando `correctResponseKey.isDirectional`,
si no a `classify`. El holder captura LEFT/RIGHT (`onResponse(key)`, `onTap`=PRIMARY) y la pantalla se
divide en dos mitades táctiles; nuevos `FocusFieldState.Choice` + `FocusChoiceField`. E2/E5 expuestos
vía `registry` + `implemented`, con el `ExerciseCatalogConsistencyTest` actualizado.

**Regla futura:**
Para un nuevo paradigma de Enfoque: (1) decidir la modalidad de respuesta y propagar TODO lo que la
clasificación necesita hasta el `RecordedTrial` (no asumir que la latencia basta); (2) el ensamblador
—no los contadores del holder— es la fuente de verdad de outcome/métricas; (3) una métrica
comparativa (congruencia, switch cost…) solo es fiable con HIT-con-RT en AMBOS lados de la
comparación; (4) exponer en `registry` + `implemented` a la vez y mantener el delta en
`pendingUiIntegration` (el test de consistencia lo exige).

**Archivos afectados:**
- `shared/.../focus/domain/model/Stimulus.kt` · `domain/session/FocusSessionAssembler.kt`
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` · `presentation/state/FocusUiState.kt`
- `shared/.../focus/presentation/ui/FocusSessionScreen.kt` · `ui/components/FocusChoiceField.kt` (NUEVO)
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusSessionAssemblerChoiceTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Integración del paradigma stop-signal (E4 Freno) en Enfoque

**Error detectado:**
Al cablear E4 al runtime, la clasificación go/no-go existente marcaba como HIT un trial de stop
respondido — porque E4 pone `isTarget=true` en TODOS los trials (se responde por defecto). Un stop
respondido es una inhibición FALLIDA, no un acierto; además la SSD real (online) no llegaba a
`TrialResult.stopSignalDelayMs`, así que el SSRT no era computable desde el ensamblador.

**Causa raíz:**
El stop-signal no es go/no-go: todos los trials piden respuesta y el de stop pide ABORTAR. La SSD es
ONLINE (depende de las respuestas, escalera 1-up/1-down) y por diseño el generador la deja null; el
runtime debe resolverla por trial con `StopSignalTracker`. El ensamblador re-clasifica y necesita una
clasificación específica de stop y la SSD usada.

**Solución aplicada:**
`TrialClassifier.classifyStop` (respondió→FALSE_ALARM con RT; retuvo→CORRECT_REJECTION). El holder crea
el `StopSignalTracker` del generador, presenta en DOS fases (GO; si no responde dentro de la SSD,
cambia a `FocusFieldState.Stop` rojo el resto de la ventana), registra la SSD usada en `RecordedTrial`
y actualiza el tracker (`successfulStop = sin respuesta`). El ensamblador despacha
classifyStop/classifyChoice/classify y propaga `stopSignalDelayMs` a `TrialResult`. El SSRT (ya en
dominio) computa desde el bloque ensamblado.

**Regla futura:**
Un paradigma con dificultad ONLINE (depende de la respuesta) NO cabe en el patrón generate-y-reproduce
puro: el generador emite el ESQUEMA + un tracker, y el runtime resuelve el parámetro por trial y lo
registra en `RecordedTrial` para que el ensamblador lo propague. La clasificación vive en
`TrialClassifier` (una función por modalidad) y holder + ensamblador deben usar EXACTAMENTE la misma
vía de despacho. Al extraer ramas con varios `return`, respetar `ReturnCount` (≤2) partiendo en
funciones — nunca regenerar el baseline de Detekt para silenciarlo.

**Archivos afectados:**
- `shared/.../focus/domain/exercise/TrialClassifier.kt` (classifyStop)
- `shared/.../focus/domain/session/FocusSessionAssembler.kt` (dispatch stop + propaga SSD)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` (tracker + GO→STOP dos fases)
- `shared/.../focus/presentation/state/FocusUiState.kt` (FocusFieldState.Stop) · `ui/components/FocusStimulusField.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusStopSignalAssemblerTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Task-switching (E6) exige enriquecer el payload, no mostrar la respuesta

**Error detectado:**
`E6CambioGenerator` fijaba `correctResponse` con un `nextBoolean()` aleatorio y `TaskSwitch` solo
llevaba `rule/isSwitch/correctResponse`. Al integrarlo, el usuario solo vería una respuesta que
pulsar: no habría tarea de cambio real (sin estímulo al que aplicar la regla) y `SWITCH_COST` no
mediría nada cognitivo.

**Causa raíz:**
El task-switching exige aplicar la regla ACTIVA a un estímulo multidimensional; el payload no
transportaba esas dimensiones, así que un render fiel era imposible sin enriquecer el dominio.

**Solución aplicada:**
`StimulusPayload.TaskSwitch` gana `colorIsRed` + `shapeIsCircle` (estímulo color/forma); el generador
deriva `correctResponse` de (rule, dimensión) — RULE_A=color (rojo→izq), RULE_B=forma (círculo→izq).
Nuevo `FocusFieldState.RuleSwitch` + `FocusRuleSwitchField` (forma coloreada + cue de regla) reutiliza
las mitades táctiles LEFT/RIGHT (`ChoiceResponseLayer`). `SWITCH_COST` (ya en dominio) computa desde
el bloque ensamblado.

**Regla futura:**
Si un paradigma exige que el usuario DERIVE la respuesta de un estímulo (task-switch, conflicto real,
búsqueda), el payload de dominio debe transportar las features del estímulo y el generador derivar
`correctResponse` de ellas — NUNCA exponer la respuesta directamente (render deshonesto). Enriquecer
el payload con defaults para no romper construcciones existentes; los tests de generador determinista
siguen valiendo porque las nuevas extracciones salen del mismo `Random`.

**Archivos afectados:**
- `shared/.../focus/domain/model/Stimulus.kt` (TaskSwitch +color/forma)
- `shared/.../focus/domain/exercise/E6CambioGenerator.kt` (deriva correctResponse)
- `shared/.../focus/presentation/state/FocusUiState.kt` (RuleSwitch) · `ui/components/FocusRuleSwitchField.kt` (NUEVO)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` · `ui/FocusSessionScreen.kt` (ChoiceResponseLayer)
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusTaskSwitchTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Paradigmas temporales/dinámicos (MOT/RSVP/dual) caben en el runtime de una sola respuesta

**Error detectado:**
No era un bug: decisión de integración. E9 (MOT, objetos en movimiento), E10 (RSVP, ráfaga rápida) y
E8 (dual-task) parecían exigir un runtime con sub-temporización o múltiples respuestas por trial, lo
que habría obligado a reescribir el bucle `presentAndAwait` (una respuesta por trial).

**Causa raíz:**
El dominio ya modela los tres como UN solo juicio por trial: E9 sondea un objeto (`isTarget` =
"¿el sondeado era objetivo?"), E10 es detección (`isTarget=true` → tocar al ver el target), E8 es
go/no-go con un flag `dual`. La dificultad (movimiento/velocidad/carga) es de PRESENTACIÓN, no de
respuesta.

**Solución aplicada:**
El `FocusFieldState` lleva lo necesario para que el renderer "tonto" corra su PROPIA animación sobre
la ventana de respuesta (E10 `LaunchedEffect` que avanza la ráfaga; E9 `animateFloatAsState`
fases highlight→move→probe en `Canvas`; E8 contador concurrente), mientras el holder sigue esperando
UNA pulsación. La estabilidad/variación por trial se logra con un `layoutSeed = stimulus.index`
(`remember(field)` + `Random(seed)`), sin tocar el dominio. E9 deriva el objeto sondeado de
`targetIndices` + `probedIsTarget` sin enriquecer el payload.

**Regla futura:**
Si la dificultad de un paradigma es temporal (stream) o espacio-dinámica (movimiento) pero el dominio
lo modela con una sola respuesta, NO reescribas el runtime: mete el movimiento/temporización
ENTERAMENTE en el composable (keyed por un seed por trial para estabilidad + variación) y deja que el
holder capture una pulsación sobre la ventana. En `Canvas`, `cos/sin` de `kotlin.math` devuelven
`Double` → `.toFloat()` antes de alimentar `Offset`/radios. Estas animaciones son funcionales (la
tarea), no decorativas (respeta "no animaciones por defecto").

**Archivos afectados:**
- `shared/.../focus/presentation/state/FocusUiState.kt` (Tracking/Stream/Dual)
- `shared/.../focus/presentation/ui/components/{FocusTrackingField,FocusStreamField,FocusDualTaskField}.kt` (NUEVOS)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` (fieldFor → 3 mapeos) · `ui/FocusSessionScreen.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt` (13/13)
- `shared/.../focus/domain/session/FocusAdvancedParadigmsAssemblerTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-22 — Arena Module III: gate de integridad VERIFIED y Detekt en archivos nuevos

**Error detectado:**
Al implementar el Arena Bayesian Core (Fases 03–07.5) surgieron dos conflictos: (1) la fuente de
verdad sobre el gate de rating era ambigua —el plan detallado `03/rating_update_algorithm.md` exige
`Integrity == OPEN`, pero `17_metric_integrity_hardening.md` exige `IntegrityStateEnum == VERIFIED`—;
(2) `:shared:detekt` rechazó dos archivos nuevos por `ComplexCondition` (`UuidV4`: 4 condiciones `==`
unidas por `||`) y `MatchingDeclarationName` (`RatingConfig.kt` con única declaración top-level
`RatingEngineConfig`).

**Causa raíz:**
(1) El árbol `docs/module_iii_plan` (el plan auditado) tiene menor prioridad que los docs de revisión
(`17_*`) en el orden de fuentes de verdad; el gate correcto es VERIFIED. (2) Detekt (default config +
buildUponDefaultConfig) aplica `ComplexCondition` (umbral 4 operandos) y `MatchingDeclarationName`
(un único top-level obliga a que el archivo se llame igual) a TODO archivo nuevo fuera del baseline.

**Solución aplicada:**
(1) Se añadió `IntegrityStateEnum.VERIFIED` y el motor sólo aplica delta cuando el estado es VERIFIED;
los demás estados son `Rejected(INTEGRITY_NOT_VERIFIED)`. (2) La condición de posición de guion se
movió a un `setOf(4,6,8,10)` (`i in DASH_POSITIONS`); el archivo se renombró a `RatingEngineConfig.kt`.

**Regla futura:**
Cuando el plan detallado contradiga un doc de revisión posterior, gana el doc de mayor prioridad en el
orden de fuentes; documenta la reconciliación en el KDoc del símbolo afectado. En archivos nuevos: una
condición con ≥4 operandos booleanos se factoriza a pertenencia en colección; un archivo con una sola
declaración top-level DEBE llamarse igual que ella (o añade una 2ª declaración para que
`MatchingDeclarationName` no aplique). Para tests pesados/IO o escaneo de fuentes, ubícalos en
`desktopTest` (no lo escanea Detekt); `commonTest` sí se escanea.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/**` (NUEVO: rating/engine, hardware,
  matchmaking, evidence/ledger, evidence/motors, sanitization, observability, simulation, config)
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/config/Enums.kt` (+VERIFIED y enums Fase 03)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/arena/**` (NUEVO: 65 tests de aceptación)
- `CLAUDE.md`

### 2026-06-23 — Cobertura Kover en KMP: usar la tarea por-target `koverVerifyDesktop`, no la fusionada

**Error detectado:**
Al añadir Kover (`org.jetbrains.kotlinx.kover` 0.9.8) para medir/forzar cobertura del Arena core,
la tarea fusionada `koverVerify`/`koverVerifyAll` mezcla los targets KMP: el target Android no ejecuta
los tests JVM (los tests de arena viven en `desktopTest`), así que las clases `commonMain` figuran sin
cubrir por el lado Android y una regla a nivel "all code" puede fallar pese a estar el código cubierto
por `desktopTest`.

**Causa raíz:**
En KMP, Kover crea variantes de reporte por target (`...Desktop`, `...Android`) además de la fusionada.
La cobertura real proviene del target cuyos tests se ejecutan (desktop). La regla configurada en
`kover { reports { filters {} verify {} } }` se hereda por TODAS las variantes, pero sólo la variante
cuyo test corrió tiene datos de ejecución.

**Solución aplicada:**
Medir y verificar con las tareas Desktop: `:shared:koverLogDesktop`, `:shared:koverHtmlReportDesktop`,
`:shared:koverVerifyDesktop`. Se filtró el alcance a `…arena.rating.engine.*` y `…arena.sanitization.*`
y se fijó `minBound(90)` (cobertura real 96.18%). La regla se DEJÓ fuera de `check` (ejecución manual)
para no introducir fallos sorpresa en CI; el guard de terminología SÍ se amplió a `commonTest`/
`desktopTest` y se enganchó a `check`. Compatibilidad verificada: Kover 0.9.8 aplica limpio sobre
Gradle 9.3.1 / AGP 9.1.0 / Kotlin 2.3.10. Se probó que la regla realmente bloquea subiendo el bound a
99 (falló con 96.18%) y restaurándolo a 90.

**Regla futura:**
Para cobertura en este repo KMP, usar las tareas `kover*Desktop` (el target con tests JVM), nunca la
fusionada, y filtrar `reports.filters.includes.classes(...)` al paquete medido. Verificar la versión de
Kover contra Maven Central antes de fijarla (Gradle 9 es reciente). No enganchar `koverVerify*` a `check`
sin acordar el gate; sí es seguro ampliar guards estáticos aditivos (terminology/production) a test dirs.

**Archivos afectados:**
- `gradle/libs.versions.toml` (+kover 0.9.8)
- `shared/build.gradle.kts` (plugin kover + bloque `kover {}` filtrado + scanners de terminología sobre test dirs)
- `CLAUDE.md`

### 2026-06-23 — El scanner de terminología hace match por palabra completa a través de guiones

**Error detectado:**
`:shared:arenaTerminologyScanner` falló con "Forbidden terminology found: 'clinical' in file
UserSafeExplanationMapping.kt": un comentario KDoc decía "non-clinical product language". El segundo
intento ("non-diagnostic") habría fallado igual por 'diagnostic'.

**Causa raíz:**
El scanner usa `Regex("\\b$term\\b")`. El guion `-` es un carácter no-palabra, así que `\bclinical\b`
SÍ coincide dentro de "non-clinical" (y `\bdiagnostic\b` dentro de "non-diagnostic"). Los términos
prohibidos (ELO, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out…)
se detectan aunque vayan precedidos/seguidos de guion u otro signo.

**Solución aplicada:**
Reescribir el comentario con lenguaje neutro ("plain product language"), sin los lexemas prohibidos
ni siquiera como parte de un compuesto con guion. Verificado con `arenaTerminologyScanner` (commonMain)
y los scanners de `commonTest`/`desktopTest`.

**Regla futura:**
En cualquier fuente del módulo `arena` (incluidos comentarios y tests), no escribir un término
prohibido NI como parte de un compuesto con guion (`non-clinical`, `anti-...`). Antes de cerrar una
tarea que toque `arena`, ejecutar `:shared:arenaTerminologyScanner` (+ variantes de test). Para copy
de usuario, preferir lenguaje funcional ("calibración", "práctica", "perfil"), nunca clínico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/session/explanation/UserSafeExplanationMapping.kt`
- `CLAUDE.md`

### 2026-06-25 — El borrado de cuenta no purgaba la media/docs de profile-cards (erasure diferido al GC)

**Error detectado:**
`executeAccountDeletion(uid)` purgaba los prefijos de Storage `profile_visuals/${uid}/` y
`post_attachments/${uid}/` y borraba docs sueltos (`profiles`, `profile_visuals`, `privacy`,
`counters`), pero NO tocaba la feature profile-card: ni los docs `profile_cards_private/{uid}` /
`profile_cards_public/{uid}` ni sus prefijos de Storage. Al borrar la cuenta, las imágenes de la
tarjeta de perfil quedaban en Storage (preocupación GDPR Art. 17) y solo se reclamaban con retraso por
el GC programado `reconcileProfileCardMedia` (tras la ventana de gracia + siguiente run).

**Causa raíz:**
La feature profile-card (docs + prefijos `profile_cards_{private,public}/`) se añadió DESPUÉS del
flujo de borrado y se apoyó únicamente en el GC perezoso como recolector. El borrado inmediato nunca
se amplió al nuevo namespace de datos por-usuario; el GC es backstop durable, no erasure inmediato.

**Solución aplicada:**
En `executeAccountDeletion`: (2) se añadieron `profile_cards_private/{uid}` y
`profile_cards_public/{uid}` a los `delete()` de docs sueltos; (7) se añadieron
`deleteStoragePrefix('profile_cards_private/'+uid+'/')` y el público al paso de Storage bloqueante
(fallo ⇒ job retryable, sin borrar Auth), con sus `*PrefixRef` redactados vía `logSafeStoragePrefix`
en los extras de `markStep`. Tests actualizados (single-docs + "cleans up user Storage prefixes"). El
GC programado sigue siendo el backstop durable. Verificado: `npm run typecheck`, `jest accountDeletion`
(14/14), `scan:secrets` limpio.

**Regla futura:**
Al introducir un nuevo namespace de datos por-usuario (prefijo de Storage `X/{uid}/` o doc
`X/{uid}`), reflejarlo SIEMPRE en `executeAccountDeletion`: docs sueltos en el paso de single-docs y
prefijos en el paso de Storage bloqueante (con `logSafeStoragePrefix` en los extras). Un GC perezoso
es backstop, no sustituye el erasure inmediato exigible por GDPR Art. 17. Logs PII-free con los
helpers `logSafe*` existentes.

**Archivos afectados:**
- `functions/src/accountDeletion.ts`
- `functions/src/accountDeletion.test.ts`
- `CLAUDE.md`

### 2026-06-28 — Un copy-scanner de build escanea archivos COMPLETOS (comentarios + disclaimers negados)

**Error detectado:**
Al cablear el `mindboostCopySafetyScanner` global (INFRA-05, espejo de `arenaTerminologyScanner`), el
build falló sobre copy "defensivo" legítimo: `"no es un test de inteligencia"`, `"Not an IQ test"` en
comentarios KDoc, `"NO es un diagnóstico"` en disclaimers, e incluso `"en la vida real"` y `ansiedad`
(palabra-estímulo de memoria). El scanner para en el PRIMER término por corrida, así que la enumeración
manual dejó pasar varios (`TrainingCycle`, `MetricEdgeCasePolicy`, `MemoryExerciseCatalog`,
`SpeedDashboardScreen` continuación de comentario).

**Causa raíz:**
El task lee `file.readText()` ENTERO (no solo strings de UI): comentarios y disclaimers cuentan. El
match es whole-word con folding de acentos y cruza guiones/negaciones (`\b` trata `-`/espacios como
borde), así que un lexema prohibido dispara aunque vaya negado o en un compuesto. La forma femenina no
cae en la masculina (`clínica`≠`clinico`, `diagnóstica`≠`diagnostico`), lo que confunde el grep manual.

**Solución aplicada:**
Lexicon canónico en `core/copy/ForbiddenLexicon.kt` FUERA de los dirs escaneados (no se auto-trippea);
scanner puro literal-free; task con lista de paridad. Se reformuló TODO el copy en alcance (7 módulos +
`AppStrings`) a vocabulario permitido (doc 12 §2: "no es una nota", "proxy funcional", "orientativo"),
incluidos comentarios. En archivos de TEST escaneados, los literales prohibidos se ensamblan por
fragmentos (`"i" + "q"`, `"intelig" + "encia"`) como en `EmotionTerminologyComplianceTest`.

**Regla futura:**
Para un copy/terminology-scanner de build: (1) el lexicon vive FUERA del árbol escaneado; (2) reformula
comentarios y disclaimers, no solo strings de UI — el anti-negación prohíbe nombrar el lexema aunque sea
para negarlo; (3) el scanner para en el primer hit → enumera con un grep COMPLETO (acentos + femenino +
multi-palabra) o itera; (4) polisémicos como frase, nunca palabra suelta (`deterioro cognitivo` no
`cognitivo`; `rindes mejor en el trabajo` no `trabajo`); (5) palabras-estímulo de contenido que choquen
con el lexicon clínico se sustituyen por sinónimos no-clínicos (`ansiedad`→`inquietud`).

**Archivos afectados:**
- `shared/build.gradle.kts` · `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/*`
- `shared/.../mindboost/{creativity,focus,speed,memory}/**` (copy reformulada)
- `CLAUDE.md`

### 2026-06-28 — Añadir un namespace a `AccountBound` exige actualizar el eraser de borrado de cuenta

**Error detectado:**
`AccountBoundErasureCoverageTest.account_bound_is_the_canonical_namespace_set` fallaba: el namespace
`mindboost_profile` (brain-profile, Fase 2A) está en `MindBoostStorageNamespaces.AccountBound` pero NO
en la lista del `SecureNamespaceUserDataEraser` de `AppGraph` → los datos cognitivos del perfil NO se
borraban al eliminar la cuenta (hueco GDPR Art. 17). Pre-existente, no introducido por INFRA-05.

**Causa raíz:**
Al añadir `Profile` a `AccountBound` (Fase 2A) no se actualizó ni la lista del eraser en
`AppGraph.rememberAppGraph()` ni el pin del test. El test de cobertura existe justo para forzar ambas
actualizaciones, y estaba (correctamente) en rojo.

**Solución aplicada:**
Se añadió `profileStore` (`rememberSecureKeyValueStore(MindBoostStorageNamespaces.Profile)`) a las keys
del `remember` y al `listOf(...)` del `SecureNamespaceUserDataEraser`, y `Profile` al set canónico del
test. Borrado inmediato de `mindboost_profile` restaurado; `desktopTest` verde.

**Regla futura:**
Al añadir un namespace a `MindBoostStorageNamespaces.AccountBound`, actualizar SIEMPRE en el mismo
cambio: (1) la lista del `SecureNamespaceUserDataEraser` en `AppGraph` (keys del `remember` + `listOf`),
y (2) el pin de `AccountBoundErasureCoverageTest`. Un namespace en `AccountBound` sin entrada en el
eraser es un dato sensible que nunca se borra.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/storage/AccountBoundErasureCoverageTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Una state machine de "cascada de guards" choca con 3 reglas Detekt a la vez

**Error detectado:**
`:shared:detekt` rechazó `ScoreStateMachine.kt` (INFRA-01) con 6 `MaxLineLength` (>120): las lambdas
de gate inline con enums totalmente cualificados (`ScoreState.DEVICE_UNRELIABLE`,
`ClaimLevel.PRACTICE_SIGNAL`, `TimingCriticality.RT_CRITICAL`) se pasaban de 120 columnas.

**Causa raíz:**
Una máquina de estados que es una cascada de guards tropieza con TRES reglas Detekt simultáneas y en
tensión: (1) `ReturnCount` (≤2) prohíbe el patrón natural de 11 `if (...) return X` tempranos;
(2) `TooManyFunctions` (≤11 en object) prohíbe partir la cascada en 11 funciones de gate con nombre;
(3) `MaxLineLength` (120) revienta las lambdas inline cuando los enums van cualificados.

**Solución aplicada:**
Tabla ordenada `(state, claim, predicate)` como `private data class Gate`, resuelta con
`gates.firstOrNull { it.fires(inputs) }?.let { noUpdate(it.state, it.claim) } ?: validVerdict(...)`
(1 return, 0 funciones extra), e **import de las entradas de enum** usadas
(`import …ScoreState.DEVICE_UNRELIABLE`, etc.) para acortar las líneas por debajo de 120 sin tocar el
baseline.

**Regla futura:**
Para las state machines de cascada que vienen (INFRA-02/03): modelar los gates como una tabla de datos
ordenada resuelta con `firstOrNull`/`firstNotNullOfOrNull` (no `return` tempranos ni N funciones con
nombre), e importar las entradas de enum para no superar `MaxLineLength`. Nunca regenerar el baseline
para silenciar. Detekt en este repo: `ReturnCount`≤2, `TooManyFunctions`≤11/object, `MaxLineLength`=120.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `CLAUDE.md`

### 2026-06-28 — El "bug" del plan (computeRtNanos lanza) estaba mal ubicado; y patrón para señal de plataforma testeable sin device

**Error detectado:**
El plan INFRA-02 afirmaba que `TimebasePolicy.computeRtNanos` LANZA con rt<0. En el repo real
`computeRtNanos` devuelve el delta con signo y NO lanza; el guard de RT negativo está en
`TrialEngine.kt:71` `require(TimebasePolicy.isValidRt(rtNanos))`. Seguir el plan al pie habría llevado
a "arreglar" el archivo equivocado.

**Causa raíz:**
El plan se redactó desde una auditoría previa y ubicó el throw en el símbolo equivocado. Regla FASE-0:
verificar CADA símbolo del plan contra el repo antes de tocar código.

**Solución aplicada:**
`signedRt` se añadió como helper PURO aditivo en `core/timing/TimingPolicy.kt` (sin tocar `TrialEngine`,
cuyo guard sirve a E5/E12 = fase futura). El jank-gate se implementó con la medición real desacoplada en
un calculador PURO `FrameStatsCalculator` (testeable en `desktopTest` con streams sintéticos de frames),
y `FrameTimingProbe` como **interface + `@Composable expect fun rememberFrameTimingProbe()`** (no
`expect class`, idioma del repo, ver `SpeedTimebaseHandle`) con actual Android (Choreographer) y
fallback no-op desktop/iOS. El `SpeedSessionStateHolder` recibió `frameProbe: FrameTimingProbe =
NoOpFrameTimingProbe` (param con default → 0 impacto en los 1800 tests; Android inyecta el real).

**Regla futura:**
(1) Para una señal medida en plataforma (jank, sensores), extraer SIEMPRE un calculador PURO que el
`actual` delega, para poder testear el umbral sin dispositivo. (2) Para inyectar una dependencia de
plataforma en un holder vivo sin romper tests, añadir un param con default no-op (último), y proveer el
real solo desde el composable de plataforma. (3) El clasificador timebase que depende de enums de un
feature (TimestampSource/MetricThresholds de `speed`) vive en ESE feature, no en `core` (core no
depende de features); solo `FrameStats`/`FrameTimingProbe` neutrales viven en `core/timing`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/*` (NUEVO)
- `shared/src/{android,desktop,ios}Main/.../core/timing/FrameTimingProbe.*.kt` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimebaseClassifier.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/{SpeedSessionStateHolder,ui/SpeedExerciseScreen}.kt`
- `CLAUDE.md`

### 2026-06-28 — El `ProfileUpdateGate` del plan asumía un aggregator mutable; el real es PURO

**Error detectado:**
El plan (doc 04 §4 / INFRA-06) define `ProfileUpdateGate { aggregator.contribute(result.toModuleContribution()) }`
y lo ubica en `core/scoring`. En el repo real `MindBoostProfileAggregator` es una función PURA
`aggregate(userId, List<ModuleContribution>, nowMs): MindBoostProfile` — NO tiene `contribute(...)`. Seguir
el plan literal no compilaría, y además invertiría la dirección de dependencias (core → feature `profile`).

**Causa raíz:**
El plan se redactó sobre un diseño hipotético del aggregator. Regla FASE-0: verificar la API real antes de
cablear. Además `core` debe permanecer neutral (no depender de features); el bridge ScoreResult→perfil
acopla `core/scoring` con `profile/domain/model` (ModuleContribution/CognitiveModuleId).

**Solución aplicada:**
`MinSampleGate` (tabla golden de nMin) queda en `core/scoring` (puro, sin deps de feature). El
`ProfileUpdateGate` + `toModuleContribution` + `ScoreModuleId.toCognitiveModuleId` se ubican en el módulo
`profile` (profile → core, dirección correcta) y exponen `contributionFor(result): ModuleContribution?`
(null = ausencia de update, nunca un valor bajo) + `contributions(list)`, que el caller pasa al aggregator
PURO. Un `primarySignal` no normalizado (fuera de 0..1) mapea a `normalizedScore=null` (no lanza; el
aggregator lo trata como no-cualificante).

**Regla futura:**
(1) El veredicto sellado se transforma en `ModuleContribution?` y se PASA al aggregator puro; nunca asumir
un sink mutable. (2) Todo bridge que acople `core` con un feature vive en el FEATURE, no en `core`. (3) La
migración de `DefaultMetricsEngine` para consumir `MinSampleGate` (subir MIN_N=3..5 → ≥20) se DIFIRIÓ: es un
cambio de calibración que reclasifica muestras pequeñas y rompería los golden de métricas; hacerlo es una
tarea deliberada con actualización de goldens, no un efecto colateral de plumbing.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/MinSampleGate.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/ProfileUpdateGate.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Añadir una variante a un `sealed UiState` y su renderer son la MISMA fase, no dos

**Error detectado:**
El plan dividía el trabajo en "P2 holder flow" (añadir `MemorySessionUiState.Processing` + el gate) y
"P3 UI" (pintar la fase processing) como fases separadas. Implementar solo P2 deja el build roto: el
`when (uiState)` exhaustivo de `MemorySessionScreen` (sin `else`, estilo del repo) no compila al faltar
la rama `Processing`.

**Causa raíz:**
Un `sealed interface` de UiState fuerza un `when` exhaustivo en su renderer. Añadir una variante y NO
tocar el renderer es un error de compilación, no una fase posterior. La suposición del plan (estado y
render desacoplables) era incorrecta para sealed types con `when` exhaustivo.

**Solución aplicada:**
P2 añadió la variante `Processing`, el `processingGate`/`submitProcessing`, la fase en `runSession`
(`presentProcessing` con `processingTasksOf` exhaustivo: ComplexSpan→tasks; List/Pairs→emptyList, que
saltan a recall sin cambios) y `lastSpanTrial` en `finish`; e INCLUYÓ obligatoriamente la rama
`ProcessingContent` en el screen (afford. Sí/No por prompt) para compilar. El test se actualizó en el
mismo cambio (los flujos que tocan EX1 deben responder processing antes de recall).

**Regla futura:**
Al añadir una variante a un `sealed` UiState (u otro sealed con `when` exhaustivo), actualizar TODOS
sus `when` (renderers, mappers, holders) en el MISMO cambio — nunca planificar "añadir estado" y
"renderizar estado" como fases distintas. Un flujo turn-based con gate `CompletableDeferred` se extiende
añadiendo el campo gate, el callback `submit*`, el `await*`, limpiarlo en `abort()`, y presentar la fase
solo cuando aplica (rama exhaustiva del plan; las demás devuelven vacío y saltan la fase).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/state/MemoryUiState.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolder.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/ui/MemorySessionScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un fallback silencioso asimétrico convierte un test de discriminación en gameable

**Error detectado:**
DR-P2 (flujo de recuerdo diferido EX3): `buildTest` resolvía las palabras estudiadas HONESTAMENTE (si
ninguna resuelve → null → NothingDue), pero el muestreo de distractores iba en
`runCatching{…}.getOrDefault(emptyList())`. Si el sampler fallaba (ejercicio no soportado, o pool − exclusiones
< 6), se PRESENTABA un test SIN señuelos: `distractorCount==0` → `falseAlarmRate` fija 0.0 →
`isDiscriminating` siempre true → "seleccionar todo" daba un 100% gratis — justo el free-100% que el KDoc
afirmaba prevenir. Latente hoy (pool ≥100, solo ~5 excluidas) pero hueco de honestidad real. Lo cazaron
las DOS lentes del workflow adversarial (MEDIUM/LOW).

**Causa raíz:**
Asimetría: un lado del test de discriminación (estudiadas) fallaba honestamente; el otro (distractores)
degradaba en silencio a un test sin señuelos en vez de tratarse como "no presentable". Un test de
reconocimiento SIN señuelos no puede medir discriminación → no debe puntuarse.

**Solución aplicada (detente y corrige):**
Ambos lados fallan SIMÉTRICAMENTE: `buildTest` devuelve null (→ NothingDue) si no resuelve ninguna
estudiada O si no se pueden muestrear los `DISTRACTOR_COUNT` señuelos requeridos. Nunca se presenta un
test sin señuelos. Test de regresión: ítem DUE con ejercicio no soportado (sampler lanza) → NothingDue,
no un Finished con distractorCount==0.

**Regla futura:**
En un test de discriminación (señal corregida por false-alarms), AMBOS lados —los objetivos Y los
señuelos— deben fallar de la misma forma honesta: si cualquiera no se puede presentar en su totalidad, el
ítem se SALTA, nunca se puntúa degradado. Un `runCatching{…}.getOrDefault(emptyList())` sobre el muestreo
de señuelos es un fallback silencioso que rompe el gate anti-gaming (FAR estructuralmente 0 ⇒ siempre
discriminante). Exigir el nº COMPLETO de señuelos (no ≥1) antes de presentar; probar el camino de fallo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/DeferredRecallSessionStateHolder.kt`
- `shared/src/desktopTest/.../memory/presentation/DeferredRecallSessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un probe cuya respuesta no es seleccionable corrompe la señal en silencio (infra-crédito)

**Error detectado:**
EX4-P2b: el `pairsTrialOf` del holder derivaba `targetCount = scored.studiedCount = plan.probes.size`. A
dificultad alta (RETRIEVAL ≥ 0.5) `Ex4PairsGenerator` emite probes BACKWARD cuyo `expectedTarget` es la
PISTA (cue) del par, pero el recall flat-multi-select solo ofrece los TARGETS como candidatos (las pistas
no están). Así, los probes backward son inalcanzables (siempre OMISSION), pero contaban en `targetCount`:
un usuario honesto perfecto quedaba **capado a hitRate 0.5** en sesiones bidireccionales, y el índice
asociativo persistido mezclaba escalas 0..1 (unidireccional) y 0..0.5 (bidireccional). Silencioso (sin
crash, sin log). Lo cazaron las DOS lentes del workflow adversarial (HIGH); ningún test lo veía porque el
único test end-to-end corría a EASIEST (unidireccional).

**Causa raíz:**
La derivación asumió que TODO probe es contestable, sin verificar que el conjunto de candidatos del recall
fuese fiel al conjunto de probes. Un probe cuya respuesta esperada no está entre los candidatos no es
medible y no debe entrar en el denominador.

**Solución aplicada (detente y corrige):**
`pairsTrialOf` filtra a probes ANSWERABLE (`expectedTarget.id in candidateIds`); `targetCount` y `hits`
se cuentan solo sobre ellos (mismo denominador → hitRate comparable entre dificultades); devuelve `null`
si no hay ninguno (nada que registrar). Los probes backward (pista no candidata) quedan excluidos del
score en V1 (medir B→A real exige un recall por-probe con pistas como opciones — diferido). Test que fija
el arreglo: a RETRIEVAL=MAX, `probes.size == 2*forward` (confirma bidireccional) y `targetCount == forward`
con selección completa → `hits == targetCount` (hitRate 1.0, no 0.5).

**Regla futura:**
Al derivar una señal de un flujo de captura, VERIFICAR que el conjunto presentado al usuario (candidatos,
opciones) sea fiel al conjunto evaluado (probes/preguntas): un ítem cuya respuesta correcta no es
seleccionable NO entra en el denominador (infla a la baja en silencio). Para señales accuracy-based,
`targetCount` debe ser el nº de ítems REALMENTE contestables, no el total generado. Probar la derivación
en TODAS las ramas de dificultad (no solo la más fácil): un test a EASIEST enmascara bugs de las ramas
duras (bidireccional, interferencia, etc.).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolder.kt`
- `shared/src/desktopTest/.../memory/presentation/MemorySessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Diseñar la señal/anti-gaming ANTES de verificar el flujo de captura vivo = puntuación gameable

**Error detectado:**
EX4-P1 (Pares Asociados) modeló la señal como hit-rate crudo (aciertos/probes) y el anti-gaming como
entropía de POSICIÓN de opción por probe. Al aterrizar P2 contra el holder real, el flujo de recall de
EX4 es un MULTI-SELECT plano sobre candidatos (targets + 6 distractores) → `Set<String>`, y `scorePairs`
marca HIT sii el target está en `selected`. Es decir: **seleccionar todo → 100% de acierto** (señal
inflable), y el adversario de "posición" NI SIQUIERA EXISTE en ese flujo.

**Causa raíz:**
Se diseñó el scorer/anti-gaming sobre un flujo de captura ASUMIDO (cued recall por probe con posiciones)
sin verificar el flujo vivo (FASE-0). Un hit-rate de recognition multi-select es trivialmente gameable y
viola el charter ("no señales fabricadas; el anti-gaming solo cuenta si se prueba contra el adversario
REAL"). El anti-gaming de P1 probaba un adversario imposible.

**Solución aplicada (detente y corrige + documenta):**
Señal corregida a DISCRIMINACIÓN: `PairsTrial(targetCount, hits, distractorCount, falseAlarms)`; un trial
solo cuenta si DISCRIMINA (false-alarm rate ≤ 0.34); el índice es el hit-rate medio SOLO sobre trials
discriminantes; un bloque con <85% discriminantes → INVALIDATED (espejo del gate de validez de
procesamiento del span). "Seleccionar todo" → FAR=1 → INVALIDATED (probado como adversario sintético). Se
RETIRÓ `MemoryPairsAntiGaming` (modelo de posición erróneo): el gate de discriminación ES el anti-gaming
testeado (no hay secuencia de respuesta por probe en el multi-select). El adapter volvió a manejar
INVALIDATED (`onlyInvalidated` → QUALITY_LOW), igual que el del span.

**Regla futura:**
Antes de diseñar el scorer o el anti-gaming de un ejercicio, VERIFICAR el flujo de captura vivo real (qué
señal produce de verdad el holder), no el asumido por el plan. Para recognition de selección múltiple, la
señal honesta DEBE corregir la sobre-selección (descuento por false-alarms / gate de discriminación), no
el hit-rate crudo (gameable con "seleccionar todo"). El anti-gaming debe probarse contra el adversario que
EXISTE en ese flujo; si el gate de validez ya neutraliza al adversario (→INVALIDATED) y se prueba, basta —
no inventar un detector para una señal que el flujo no captura.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/AssociativePairsScorer.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/MemoryPairsProfileAdapter.kt`
- `shared/src/.../memory/domain/scoring/MemoryPairsAntiGaming.kt` (ELIMINADO) + su test (ELIMINADO)
- `shared/src/desktopTest/.../memory/domain/scoring/{AssociativePairsScorerTest,MemoryPairsProfileAdapterTest}.kt`
- `CLAUDE.md`

### 2026-06-28 — `dataSufficiency` ≠ `confidence`: una contribución vía gate puede descartarse en silencio

**Error detectado:**
Tras cablear MEMORIA al brain-profile (EX1 span → `ProfileUpdateGate`), la dimensión WORKING_MEMORY se
quedaba CALIBRATING PARA SIEMPRE pese a un `ScoreResult` VALID con ≥20 trials: la contribución se
descartaba silenciosamente y los docstrings ("EMERGING/preliminary tras ≥20 trials") eran falsos.

**Causa raíz:**
`ScoreResult.toModuleContribution()` mapeaba `dataSufficiency = sampling.confidence`. Para un bloque
único el adapter fija `confidence = 0.4` (estabilidad baja, NO falta de muestra). El agregador exige
`dataSufficiency >= minSufficiency(0.5)` para contar la contribución → 0.4 < 0.5 → descartada. Se
confundió "pocos bloques ⇒ menos estable" (confidence) con "muestra insuficiente" (dataSufficiency).
Ningún test lo detectaba: los unit tests asertaban la `ModuleContribution` aislada, no el plegado
end-to-end por `MindBoostProfileAggregator`.

**Solución aplicada:**
`dataSufficiency` ahora deriva de la suficiencia de MUESTRA (`nEffective/minNRequired`, =1.0 cuando el
veredicto es VALID porque ya pasó MinSampleGate); `confidence` sigue separado y mantiene la señal en
EMERGING (no ESTABLISHED). Test end-to-end añadido (≥20 trials → `service.refresh` → WORKING_MEMORY
EMERGING/0.5/preliminary). Blast radius en producción: solo MEMORIA (Focus/Speed/Logic/Lenguaje/Emociones
construyen `ModuleContribution` directamente, no por el gate).

**Regla futura:**
`dataSufficiency` (tamaño de muestra) y `confidence` (certeza/estabilidad) son ORTOGONALES: nunca mapear
uno desde el otro. Todo ProfileSource que produzca su contribución vía `ProfileUpdateGate` debe
verificarse END-TO-END a través del `MindBoostProfileAggregator` (un test sobre la contribución aislada
enmascara el descarte por `minSufficiency`). Un veredicto VALID ya pasó MinSampleGate ⇒
dataSufficiency=1.0. Recordatorio: un ProfileSource NO debe usar `SystemEpochClock` por defecto (rompe
desktopTest, kotlinx-datetime 0.7.1); pasar `timestampMs=0L` si el sello es metadato no consumido (ver
2026-05-29). Comparar Doubles calculados (media ponderada) con tolerancia, no igualdad exacta.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/ProfileUpdateGate.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/MemoryProfileSource.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/MindBoostProfileSources.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/presentation/RememberBrainProfileStateHolder.kt`
- `shared/src/desktopTest/.../profile/adapter/{MemoryProfileSourceTest,MindBoostProfileSourcesTest}.kt` · `.../aggregation/ProfileUpdateGateTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un gate que valida DATOS estructurados es un test en `check`, no un scanner de texto Gradle

**Error detectado:**
El plan INFRA-04 describe el gate de bancos como "espejo de `arenaTerminologyScanner`" (una task Gradle).
Pero validar un banco (tamaño efectivo, holdout servido, balance chi² de posición, inter-rater) requiere
introspeccionar OBJETOS Kotlin compilados, no escanear texto fuente — una task Gradle no puede hacerlo.

**Causa raíz:**
`arenaTerminologyScanner`/`mindboostCopySafetyScanner` funcionan porque leen `file.readText()` buscando
lexemas; la validación de bancos opera sobre estructura/estadística, no sobre substrings.

**Solución aplicada:**
El gate es un test `BankValidationGateTest` en `desktopTest` que corre `BankValidator` sobre el registro
`ProfileUpdatingBanks.all`. Verificado que `desktopTest` ∈ `:shared:check` (`check --dry-run` lista 13
refs a desktopTest), así que el test ES el gate de build. El registro está VACÍO hoy (ningún ejercicio es
profile-updating): los bancos fijos existentes (Logic 6, Language 36, Emotions, Creativity 14) quedan
"explícitamente no-profile" por ausencia del registro; migrar un banco al contrato es trabajo P1
por-ejercicio (futuro), hecho SOLO cuando ese ejercicio enciende profile-update. `core/bank` es neutral
(sin deps de feature).

**Regla futura:**
Para un gate que valide datos estructurados/estadísticos (bancos, anti-gaming sintéticos, INFRA-03):
implementarlo como test en `desktopTest` (no como task Gradle de texto), confirmar que `desktopTest` ∈
`check`, y usar un registro opt-in (vacío hasta que algo cumpla) en vez de migrar contenido vivo
prematuramente. Un gate sobre un registro vacío es honesto si la lógica de rechazo está probada aparte
(aquí `BankValidatorTest` con casos adversarios).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/*` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/bank/*` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Un framework anti-gaming "cuenta" solo con telemetría de defaults-limpios + suite red-team

**Error detectado:**
No es un bug: decisión de diseño para que el invariante "anti-gaming solo cuenta si testeado con
adversarios sintéticos" sea verificable de verdad (INFRA-03), sin falsos positivos sobre usuarios reales.

**Causa raíz:**
Un detector "diseñado" no cuenta; necesita (1) un perfil adversario que lo dispare Y (2) un usuario
honesto realista que NO dispare ninguno. Si la telemetría no tiene defaults limpios, el control de
falsos positivos es frágil.

**Solución aplicada:**
`TrialTelemetry` con TODOS los campos en valor neutro/limpio por defecto → `TrialTelemetry()` no dispara
ningún detector (control FP base). 11 detectores PUROS (`GamingDetector` → `DetectorSignal` →
`AntiGamingFramework.aggregate` que toma la acción más severa). Suite red-team `AntiGamingRedTeamTest`
(D12): cada adversario dispara su detector, un `honestUser` realista (12 trials, RT correlado con
dificultad, keys balanceadas) no dispara NINGUNO, y `safetyExit` emocional NUNCA penaliza (Regla #8: D11
hace `idle` si `isEmotionalOrFrustration`). Estadística pura (cv/correlación/entropía Shannon) en helpers
top-level testeables. Acciones: WITHHOLD_PROFILE/INVALIDATE; nunca emite copy ni escribe score bajo.

**Regla futura:**
Para un detector/gate estadístico: (1) telemetría con defaults limpios (el "usuario vacío" no dispara);
(2) cada detector necesita su par adversario-dispara + honesto-no-dispara en una suite red-team; (3) los
controles de seguridad (Regla #8) se prueban explícitamente (perfil que cumpliría el patrón pero está
exento). Wiring a producción se DIFIERE: D2/D4 consumen señales que SsrtCalculator/TrialClassifier ya
producen (el adapter las alimenta), y D6 inyectar entropía real al `entropyFactor` del AGRS (reemplazar
`spatialEntropy=1.0` en los holders de decisiones) es integración por-ejercicio P1, no parte del framework.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/*` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/antigaming/*` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Cablear un ejercicio P1 al perfil: núcleo aditivo (scorer+adapter) vs wiring vivo

**Error detectado:**
No es un bug: P1 (cablear un ejercicio para que actualice el perfil) es trabajo categóricamente distinto
al P0 (infra). EX1 Span (Memoria): el `DefaultMemoryMetricsEngine` ya producía `WORKING_MEMORY_SPAN =
trial.hits` — un span SIMPLE sin gate de validez de procesamiento, que V1_01 llama "span simple
disfrazado" e INSERVIBLE para perfil. El `ComplexSpanScorer` requerido NO existía; el pipeline vivo
(`ScoredTrial`) no captura `processingCorrect`; y EX1 tope hoy = `CALIBRATING` (sin detector anti-gaming
de Memoria → `DetectorCoverage.NONE`).

**Causa raíz:**
"Profile-update: sí (tras gates)" del plan está gateado por componentes ausentes. Cablear de verdad al
perfil es integración multi-capa (captura viva de processing + persistencia + adapter del brain-profile),
de alto riesgo y SIN valor de perfil hoy (EX1 igual sería CALIBRATING).

**Solución aplicada:**
Núcleo ADITIVO y seguro: `ComplexSpanScorer` (gate processingAccuracy≥0.85 → INVALIDATED; span
accuracy-based mean serial-correct sobre trials válidos; sessionSpan ponderado, null si no hay bloques
válidos) + `MemorySpanProfileAdapter` (memory→core: SpanBlockResult→ScoreResult vía ScoreResultFactory,
RT_IRRELEVANT, BankIntegrity.PROCEDURAL, MinSampleGate.ACCURACY_RT_BASE para nMin, `detectorCoverage`
param default NONE → tope CALIBRATING honesto). Tests unit+adversarial (minEffort/processing-azar →
INVALIDATED)+integración (vía ProfileUpdateGate→ModuleContribution(MEMORIA))+golden. NINGÚN edit a la
sesión/modelo vivos.

**Regla futura:**
Para un ejercicio P1: (1) implementar primero el scorer honesto + el adapter a ScoreResult como núcleo
ADITIVO y testeado (incluye los adversarios que el doc enumera); (2) el adapter expone `detectorCoverage`
con default NONE para que el tope CALIBRATING sea explícito hasta que exista el detector anti-gaming; (3)
el min-n de sesión usa MinSampleGate (autoridad central) y el por-bloque vive en el scorer; (4) RT nunca
entra en el signal/ModuleContribution (accuracy-based); (5) DIFERIR el wiring vivo (captura de processing
en el holder + persistencia + adapter del brain-profile) — no aporta valor de perfil mientras el estado
tope sea CALIBRATING, y es integración multi-capa de riesgo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/{ComplexSpanScorer,MemorySpanProfileAdapter}.kt` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/*` (NUEVO)
- `CLAUDE.md`

### 2026-07-01 — `@file:Suppress("FunctionNaming")` NO cubre `TopLevelPropertyNaming`; los `const val` top-level van en SCREAMING_SNAKE_CASE

**Error detectado:**
`:shared:detekt` rechazó `PhotoUploadLoader.kt` (design/components, nuevo) con 4 `TopLevelPropertyNaming`
por `private const val CornerRatio/CycleMillis/DotSwingStartDeg/DotSwingEndDeg` en PascalCase, pese al
`@file:Suppress("MagicNumber","FunctionNaming","LongMethod")` del archivo.

**Causa raíz:**
Se asumió que el patrón de supresión a nivel de archivo para Composables (`FunctionNaming`/`MagicNumber`/
`LongMethod`) también legitimaba el nombre de las constantes. Son reglas ORTOGONALES: `FunctionNaming`
gobierna funciones; `TopLevelPropertyNaming` exige `[A-Z][_A-Z0-9]*` (SCREAMING_SNAKE_CASE) para toda
propiedad top-level compile-time-constante (`const val`). Un `private val = 48.dp` (no `const`) NO cae en
esta regla (inicializador no constante) y no se marca.

**Solución aplicada:**
Renombrados los `const val` a `CORNER_RATIO`, `CYCLE_MILLIS`, `DOT_SWING_START_DEG`, `DOT_SWING_END_DEG`.
`:shared:compileKotlinDesktop` verde, `PhotoUploadLoader.kt` desaparece de los findings de detekt, sus 9
tests de `desktopTest` verdes, y `:androidApp:compileDebugKotlin` verde.

**Regla futura:**
Todo `const val` top-level (aunque sea `private` y viva en un archivo de Composables) va en
SCREAMING_SNAKE_CASE por `TopLevelPropertyNaming`. El `@file:Suppress` de reglas de Composable NO cubre el
naming de propiedades; arreglar/suprimir cada regla por separado. Un `val` top-level con inicializador no
constante (p. ej. `48.dp`) sí puede ir en PascalCase (no es `const`). NOTA de entorno: el árbol de trabajo
se muta en paralelo por otros agentes (Vibe Kanban/n8n) — la lista de ofensores de un gate de módulo
(`detekt`) cambió entre dos corridas seguidas (todos `??` untracked, p. ej. AppTextField→AppBarsLoader/
AppLoadingOverlay). Verifica TU archivo confirmando que NO aparece en los findings; no "arregles" WIP
untracked ajeno.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/design/components/PhotoUploadLoader.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/design/components/PhotoUploadLoaderTest.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/design/{AppColors,AppStrings}.kt`

### 2026-07-01 — El copy-scanner tiene variantes por source-set: verificar un `commonTest`/`desktopTest` exige su variante, no la tarea base

**Error detectado:**
Un comentario en `ReversibilityScenarioBankTest.kt` (commonTest, `decisiones` ∈ scope) decía "The product is
non-clinical … (Clinical/IQ/gambling terms …)". `:shared:mindboostCopySafetyScannerCommonTest` falló con
"Forbidden copy terminology found: 'IQ'". El fallo apareció DOS fases después de crear el archivo, la primera
vez que se corrió la variante CommonTest del scanner.

**Causa raíz:**
Al crear el archivo se verificó SOLO con `:shared:mindboostCopySafetyScanner` (tarea BASE, que escanea
`commonMain`), NO con `…ScannerCommonTest` (que escanea `commonTest`). El scanner lee archivos COMPLETOS
incluidos comentarios (regla 2026-06-28) y la anti-negación prohíbe nombrar el lexema aunque sea para NEGARLO
—"non-clinical"/"Clinical"/"IQ"/"gambling" en un comentario disparan—. Como la variante base no toca
`commonTest`, la violación quedó latente. El scanner PARA en el primer hit, así que un fallo puede ocultar más
términos aguas abajo.

**Solución aplicada:**
Comentario reescrito sin nombrar ningún lexema prohibido ("The `excluded` stems below reject the sensitive
real-world domains … the build copy-safety scanner additionally enforces the forbidden lexicon"). Re-corridas
las TRES variantes (base + `…CommonTest` + `…DesktopTest`) → verdes. Confirmado que los stems del `excluded`
(dinero/invers/financ/abogad/demanda/suicid/autolesi/enferm) NO están en el lexicon (pasan como datos del test).

**Regla futura:**
Al añadir/editar un archivo en un dir escaneado por el copy-scanner (focus/memory/speed/logic/decisiones/
language/creativity + design), correr la VARIANTE de su source-set: `…ScannerCommonTest` para `commonTest`,
`…ScannerDesktopTest` para `desktopTest`; la tarea base `mindboostCopySafetyScanner` solo cubre `commonMain`.
Como el scanner para en el primer hit y lee comentarios, tras un fix re-correr hasta VERDE. No nombrar un
lexema prohibido ni para negarlo, tampoco en comentarios de test.

**Archivos afectados:**
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/decisiones/domain/content/ReversibilityScenarioBankTest.kt`

---

# Mindverse / Mindboost — Claude Code Project Memory

Product context:
Mindboost is the cognitive intelligence trainer inside Mindverse. It trains functional cognitive abilities such as attention, working memory, processing speed, reasoning, creativity, metacognition, decision-making, and emotional regulation applied to performance.

Safety boundaries:
- Mindboost is not a medical, clinical, diagnostic, or therapeutic product.
- Do not diagnose ADHD, dementia, learning disorders, anxiety, depression, or any health condition.
- Do not promise guaranteed IQ increases or clinical cognitive improvement.
- Do not use fake neuroscience or vague brain-boosting claims.
- Prefer language such as "functional cognitive profile", "practice", "training", "progress against your own baseline", and "adaptive difficulty".

Engineering standards:
- Inspect existing project architecture before editing.
- Preserve conventions and style.
- Make small, reviewable changes.
- Add or update tests when behavior changes.
- Validate inputs and protect user data.
- Prefer measurable outcomes and analytics events.
- Explain dependencies before adding them.
- Separate UI, business logic, scoring, persistence, and analytics concerns.

Required completion summary:
- What changed
- Files changed
- Tests added/updated
- How to verify
- Risks or follow-up work

## Claude Code working style

When working in this repository:

- Use the specialized subagents in `.claude/agents/` when the task matches their description.
- Use multiple subagents for complex features: science, product, backend/frontend, QA, privacy.
- Keep the main context clean by delegating specialized reviews.
- Prefer concrete diffs and tests over abstract suggestions.
- Do not silently change product claims, scoring semantics, or data collection scope without explaining why.

## Preferred feature workflow

1. `mindboost-chief-architect` designs the module.
2. `mindboost-cognitive-science` validates the cognitive mechanism and claims.
3. `mindboost-product-ux` designs flow and retention loop.
4. `mindboost-assessment-diagnostics` defines baseline/scoring if applicable.
5. `mindboost-personalization-engine` defines adaptation.
6. `mindboost-backend` implements data/API/business logic.
7. `mindboost-frontend` implements UI.
8. `mindboost-data-analytics` defines events/metrics.
9. `mindboost-security-privacy` reviews data handling.
10. `mindboost-qa-safety` gives final approve/revise/block verdict.

---

# Mindverse Enterprise Claude Project Memory

Mindverse is an AI-native human intelligence, productivity, learning, wellness, creation, and personal operating-system platform. Mindboost is the cognitive intelligence trainer inside Mindverse. The product should be scalable, credible, premium, privacy-first, and engineered like a category-defining global platform. Mindverse is not a medical, diagnostic, or therapeutic product unless explicitly developed under proper regulated-health controls in the future.

## Core rule

Build Mindverse as a category-defining, globally scalable, premium AI-native platform. Every decision must improve trust, measurable value, scalability, maintainability, or strategic advantage.

## Product boundaries

- Mindverse may include Mindboost, learning, productivity, reflection, coaching, creativity, community, and personal operating-system modules.
- Mindboost is not a clinical, medical, diagnostic, or therapeutic product.
- Do not claim guaranteed IQ increases, treatment of disorders, or clinical outcomes.
- Use language such as functional cognitive profile, training progress, adaptive practice, and personal baseline.

## Engineering standards

- Read existing architecture before changing code.
- Prefer small, reviewable diffs.
- Add tests when behavior changes.
- Add analytics events for important product actions.
- Protect personal, behavioral, cognitive, and coaching data.
- Minimize data collection.
- Use typed interfaces, validation, and clear domain boundaries.
- Design for mobile, web, and future platform expansion.

## Agent workflow

For major tasks, use specialized agents in this order:

1. Strategy / architecture agent
2. Domain expert agent
3. Product / UX agent
4. Engineering agent
5. Security / privacy agent
6. QA / release agent
7. Metrics / analytics agent

## Completion format

Every completed implementation should include:

- Summary
- Files changed
- Tests added or updated
- How to verify
- Risks
- Follow-up recommendations

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
