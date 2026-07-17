# Claude Code setup — Auditoría MindOS

> **Modo:** auditoría + arreglo de lo roto + huecos reales (NO barrido greenfield).
> **Generado con:** skill `claude-code-setup:claude-automation-recommender` orientada con el
> inventario real del repo (no re-recomienda lo que ya existe).
> **Fecha:** 2026-07-06 · **Rama:** `feat/mindboost-enfoque-integration` · **Entorno:** Windows 11 / PowerShell-first.
> **Cambios ya aplicados por esta pasada:** solo (B1) los 2 hooks rotos y la higiene de permisos
> (§ "Cambios aplicados"). Todo lo de la sección (C) queda **opt-in** con snippet listo para pegar.

---

## Matriz de aceptación (toda recomendación de la sección C la cumple, o se descarta)

Una automatización solo entra en este repo si:

1. **Node `.cjs` self-locating** (`node -e "…require(process.env.CLAUDE_PROJECT_DIR||process.cwd()/.claude/hooks/…)"`) — nunca `python3` (stub de la Store) ni bash-only (`$(...)`, `case`).
2. **Nunca bloquea** una tool call (`exit 0`, solo `additionalContext`/`systemMessage`), como `mindos-guard.cjs`.
3. **Nunca ejecuta Gradle** ni comandos destructivos (`clean`, `--stop`, `--no-build-cache`).
4. **Sobrevive worktrees** y no depende de estado global de la máquina.
5. **Cero churn por commit/checkout** (compatible con los agentes Vibe Kanban / n8n).
6. **Coste de contexto justificado** para cualquier skill/MCP always-on (lente: skill `context-budget`).
7. **Respeta la rigor charter y el copy-safety lexicon** (nada que empuje claims clínicos/IQ).

---

## (A) Inventario existente — YA CUBIERTO, NO TOCAR

Verificado en disco (no según lo reportado). Los números del `.claude/README.md` describen solo el
subconjunto ECC; en disco hay bastante más porque conviven ECC + memoria + agentes/skills de dominio.

| Superficie | En disco | Estado | Nota |
|---|---|---|---|
| **Skills** | **41** (`.claude/skills/*/SKILL.md`) | ✅ auto-activadas por descripción | 23 stack ECC + pensamiento (`red-team`/`pre-mortem`/`primeros-principios`/`steelman`) + dominio MindBoost (`create-cognitive-exercise`, `review-cognitive-claims`, `design-adaptive-scoring`, `qa-release-gate`, `cognitive-science-principles`, …) + `ceo`, `graphify`, `agent-browser` |
| **Subagentes** | **74** (`.claude/agents/*.md`) | ✅ vía tool Agent/Task | 13 reviewers ECC + 10 KMP MindBoost (`kmp-architect`, `cognitive-engine-dev`, `compose-ui-dev`, `psychometrician`, `efficacy-guardian`, `diff-auditor`, …) + **31 `mindverse-*`** + **20 `mindboost-*`** de estrategia/producto |
| **Slash-commands** | **14** (`.claude/commands/*.md`) | ✅ | 8 ECC (`/kotlin-build`, `/gradle-build`, `/kotlin-review`, `/kotlin-test`, `/security-scan`, `/plan`, `/quality-gate`, `/refactor-clean`) + memoria (`/session-start`, `/recall`, `/session-end`, `/adr`, `/memory`) + `/mb` (orquestador MindBoost, user-only) |
| **Hooks** | 1 script `mindos-guard.cjs` | ✅ Node, no bloqueante, self-locating | Config-protection + secret-scan (Pre Write/Edit/MultiEdit) + recordatorio de fallo Gradle/detekt (Post Bash). **Es el patrón de referencia.** |
| **Memory Layer** | `.claude/memory/*` + 5 comandos | ✅ local-first, node:sqlite+FTS5 | MCP `mcp-server.mjs` opt-in (no auto-lanzado) |
| **Rules por lenguaje** | `.claude/rules/{kotlin,swift,typescript,common}/` | ✅ referencia para reviewers | no auto-enforzadas |
| **MCP — Firebase** | global, **conectado** | ✅ | ~40 tools (auth/firestore/crashlytics/functions/remoteconfig/storage/deploy) |
| **MCP — CodeGraph** | global, **conectado** (`codegraph_explore`) | ✅ + inyector `UserPromptSubmit` activo | Ya inyecta contexto estructural en CADA prompt (visto al arrancar esta sesión) |
| **MCP — Claude Preview** | `mcp__Claude_Preview__*` | ✅ | preview/verify de UI |
| **Grafos de conocimiento** | **2** — Graphify (`graphify-out/`, CLI) + CodeGraph (`.codegraph/`, MCP) | ✅ | `graphify-out/graph.json` presente (~40 MB, gitignored) |
| **Opt-in NO instalado (a propósito)** | `.mcp.json` (context7 / sequential-thinking), AgentShield (`/security-scan`) | ⏸️ decisión del usuario | documentado en README |

**Conclusión de (A):** el repo está **sobre-equipado**, no infra-equipado. El trabajo real es
*arreglar lo roto* y *no añadir bloat*, no "añadir automatizaciones".

---

## (B) ROTO / no-op en este entorno

### B1 — 🔴 Los 2 hooks de Graphify son un no-op silencioso en Windows *(ARREGLADO en esta pasada)*

**Qué:** en `.claude/settings.json`, `PreToolUse` matcher `Bash` (grep/find) y matcher `Read|Glob`
parseaban el JSON de stdin con `python3 -c "…"`.

**Por qué está roto (confirmado, no asumido):**

```text
$ command -v python3
/c/Users/curro/AppData/Local/Microsoft/WindowsApps/python3      # alias stub de Microsoft Store

$ printf '{"tool_input":{"command":"grep -r foo ."}}' | python3 -c "import json,sys; …"
no se encontró Python; ejecutar sin argumentos para instalar desde el Microsoft Store …
# exit code 49, stdout VACÍO
```

`python3` no ejecuta nada → `CMD`/`HIT` quedan vacíos → el `case`/`if` nunca casa → el recordatorio
Graphify **nunca se inyecta**, aunque `graphify-out/graph.json` existe. Además el cuerpo usa sintaxis
bash-only (`$(...)`, `case … esac`), que tampoco es portable si el hook corre bajo pwsh/cmd. Doble fallo.

**Fix aplicado:** se plegó la lógica de ambos recordatorios DENTRO de `mindos-guard.cjs` (parseo de
stdin con Node, misma puerta `fs.existsSync('graphify-out/graph.json')`, mensaje corto, `exit 0`).
En `settings.json` los 4 matchers apuntan a la MISMA invocación self-locating `node -e`. Cero `python3`.

**Por qué plegar en `mindos-guard.cjs` y no crear 2 scripts nuevos:** un patrón, un archivo, DRY; el
guard ya tiene `readInput()`/`emit()` no bloqueante. Se decidió **arreglar** (no retirar) porque, aunque
la regla Graphify ya vive en `CLAUDE.md` (mecanismo fiable, always-on), el nudge JIT en el momento exacto
del Read/grep es el "extra" que el propio `CLAUDE.md` reconoce; mantenerlo funcional cuesta ~60 tokens por
Read/grep cualificado (coste declarado abajo) y es fácil de estrechar si molesta (ver C6).

### B2 — ⚠️ Duplicación de permisos Gradle Bash vs PowerShell *(ARREGLADO en esta pasada)*

`allow` tenía `Bash(./gradlew :shared:desktopTest|compileKotlinDesktop|detekt:*)` **y**
`PowerShell(.\gradlew.bat :shared:desktopTest:*)` — dos formas para el mismo gate, y la forma PowerShell
solo cubría 1 de las 3 tareas. Reconciliado a la forma canónica del entorno (PowerShell/Windows). Ver
§ "Cambios aplicados". (Recordatorio de entorno: los gates Gradle se **entregan al usuario**; el IDE posee
el daemon. El permiso solo evita un prompt si alguna vez se corre uno puntual — no obliga a correrlo.)

### B3 — ℹ️ Observaciones (no roto, sin acción)

- El inyector `UserPromptSubmit` de **CodeGraph** ya funciona (Node) e inyecta contexto en cada prompt →
  **no añadir** un segundo inyector `UserPromptSubmit` (p. ej. de Graphify): sería doble coste. Ver (D).
- `graphify-out/graph.json` existe pero es gitignored: tras un `clone` limpio la puerta de los hooks
  no pasará hasta reconstruir con `graphify update .` (comportamiento correcto, fail-closed).

---

## (C) Huecos reales — recomendaciones TIERED (todas OPT-IN, no aplicadas)

> Cada una cumple la matriz de aceptación. Coste de contexto declarado con la lente `context-budget`.

### 🟢 Tier ALTO

#### C1 — MCP `context7` (docs API en vivo por versión) — *ya staged en README, se confirma*
- **Valor:** el stack (Compose MP, Kotlin 2.3, AGP 9, Firebase, KMP) es sensible a versión; `context7`
  reduce alucinaciones de API. Es el opt-in de MCP de mayor valor para este repo.
- **Coste de contexto:** casi nulo en reposo — los esquemas de tool se cargan on-demand (deferred). El
  coste real es el proceso `npx` al arrancar + el primer uso. **Always-on aceptable.**
- **Matriz:** ✅ (no es hook; no Gradle; per-repo vía `.mcp.json`; Claude pide aprobar al primer uso).
- **Cómo:** crear `/.mcp.json` con el bloque del README. `sequential-thinking` **NO** (redundante con el thinking nativo → ver D).

#### C2 — Nudge JIT de copy-safety al editar copy de MindBoost *(plegable en `mindos-guard.cjs`)*
- **Valor:** el gate `mindboostCopySafetyScanner` es de **build** y falla tarde; hay historial de fallos
  (aprendizajes 2026-06-28, 2026-07-01). Un aviso al **editar** `AppStrings.kt` / `**/presentation/**` /
  `*Screen.kt` bajo `features/mindboost/**` recuerda el lexicon prohibido (clínico/IQ/gambling) antes del build.
- **Coste de contexto:** ~70 tokens **solo** al editar un archivo de copy de MindBoost (raro). Trivial.
- **Matriz:** ✅ (Node, no bloqueante, no Gradle, respeta copy-safety por diseño).
- **Snippet (añadir al bloque Write/Edit/MultiEdit de `mindos-guard.cjs`):**
  ```js
  // Copy-safety JIT: avisar al tocar copy de usuario de MindBoost (el scanner es un gate de build tardío).
  const isMindboostCopy =
    /features\/mindboost\/.*(presentation|ui)\//.test(filePath) ||
    /AppStrings\.kt$/.test(filePath) || /Screen\.kt$/.test(filePath);
  if (isMindboostCopy) {
    notes.push(
      '🧪 Copy MindBoost: evita el lexicon prohibido (clínico/diagnóstico/IQ/gambling) incluso en ' +
      'comentarios y disclaimers negados. Lo caza el gate de build mindboostCopySafetyScanner (falla tarde). ' +
      'Prefiere "perfil funcional/práctica/orientativo". Lexicon: core/copy/ForbiddenLexicon.kt.'
    );
  }
  ```
- **Por qué opt-in:** es una conducta nueva; el usuario decide si el nudge aporta o ruido.

### 🟡 Tier MEDIO

#### C3 — Hook `SessionStart`: frescura de Graphify + Memory Layer *(1 vez por sesión, barato)*
- **Valor:** recuerda `graphify update .` si `GRAPH_REPORT.md "Built from commit"` diverge de `HEAD`, y
  sugerir `/session-start`. Una nudge por sesión, no por tool-call.
- **Coste de contexto:** ~50 tokens **una vez** por sesión. Corre `git rev-parse HEAD` (read-only).
- **Matriz:** ✅ (Node, no bloqueante, no Gradle; `git rev-parse` es read-only). ⚠️ también dispara en
  sesiones de agentes automáticos → mantener el mensaje mínimo. Opt-in.

#### C4 — Hook `Stop`: recordatorio de la regla núcleo de aprendizajes
- **Valor:** MindOS obliga a registrar cada error real en "Aprendizajes acumulados". Un nudge al cerrar
  ("si arreglaste un error real, añade el learning; entrega los gates Gradle al usuario") refuerza el protocolo.
- **Coste de contexto:** ~40 tokens una vez por Stop.
- **Matriz:** ✅. ⚠️ dispara en cada Stop incluidos agentes n8n → puede ser ruido para ellos. Opt-in.

### 🔵 Tier BAJO

#### C5 — Ampliar el set `PROTECTED` del guard con `.env*` y `*.jks/*.keystore`
- Marginal: el repo ya usa `gradle.properties` gitignored y disciplina de secretos alta. ~1 línea.

#### C6 — Añadir el tool `Grep` al matcher Graphify (además de `Read|Glob`)
- Aumenta cobertura (el tool `Grep` es el caso "búsqueda amplia" por excelencia) pero **más ruido/coste**.
  Se dejó fuera para mantener paridad con el diseño original y la verificación limpia. Opt-in.

#### C7 — (Observación `context-budget`, sin acción automática) El mayor consumidor always-on
- El **`CLAUDE.md` raíz** (sobre todo el log "Aprendizajes acumulados", ya con decenas de entradas) es de
  lejos el mayor coste de contexto por sesión (orden de decenas de miles de tokens). **No se toca** — el
  protocolo prohíbe borrar aprendizajes. *Si algún día pesa demasiado:* archivar aprendizajes con >N meses
  a `docs/learnings/ARCHIVE_YYYY.md` (no always-on), dejando en `CLAUDE.md` solo un índice. **Decisión 100%
  del usuario**, fuera del alcance de esta pasada.

---

## (D) ANTI-recomendaciones — qué NO añadir y POR QUÉ

| ❌ No añadir | Por qué |
|---|---|
| **Git hooks de rebuild del grafo** (`post-commit`/`post-checkout` de Graphify/CodeGraph) | Vibe Kanban / n8n hacen MUCHOS commits/checkouts en worktrees → un rebuild por cada uno = churn masivo. Decisión ya tomada en el repo. Refresco manual (`graphify update .`). |
| **Cualquier hook que ejecute Gradle** (format/lint/type-check/test on-save, ktlint, detekt on-save) | El IDE posee el daemon Gradle en este worktree; correr Gradle por CLI desde un hook choca con él y arriesga cache corrupto (aprendizaje 2026-06-19: gates del mismo worktree en serie). Los gates se **entregan al usuario**. |
| **Más subagentes** | Ya hay **74** en `.claude/agents/` + decenas globales. Añadir más = overhead de selección y bloat, no valor. El cuello de botella no es falta de agentes. |
| **`sequential-thinking` MCP always-on** | Redundante con el extended-thinking nativo; proceso `npx` extra sin valor incremental claro. |
| **Playwright / browser MCP pesado** | No hay target web (KMP/Compose/Android). Ya existen `agent-browser` (skill) y Claude Preview para lo que aplique. |
| **Database MCP (Postgres/Supabase/…)** | Los datos son Firestore; el **Firebase MCP ya está conectado**. Un DB MCP genérico no aplica. |
| **Segundo inyector `UserPromptSubmit`** (p. ej. Graphify) | **CodeGraph ya inyecta** contexto estructural en cada prompt. Un segundo inyector = doble coste de contexto por prompt. |
| **Auto-commit / headless CI hooks** | Los agentes automáticos ya commitean; auto-commit añade churn y conflictos. |
| **Skills `disable-model-invocation` de deploy/envío automáticas** | Los deploys son user-gated a propósito; no automatizar side-effects irreversibles. |

---

## Cambios aplicados por esta pasada (diff conceptual)

### `.claude/settings.json`

**Permisos — antes → después:**
```diff
-  "Bash(./gradlew :shared:desktopTest:*)",
-  "Bash(./gradlew :shared:compileKotlinDesktop:*)",
-  "Bash(./gradlew :shared:detekt:*)",
-  "PowerShell(.\\gradlew.bat :shared:desktopTest:*)",
-  "PowerShell(git status:*)",
+  "PowerShell(.\\gradlew.bat :shared:desktopTest:*)",
+  "PowerShell(.\\gradlew.bat :shared:compileKotlinDesktop:*)",
+  "PowerShell(.\\gradlew.bat :shared:detekt:*)",
+  "PowerShell(git status:*)",
+  "PowerShell(git diff:*)",
+  "PowerShell(git log:*)",
   "mcp__Claude_Preview__preview_screenshot",
   "mcp__plugin_firebase_firebase__firebase_deploy_status",  (… MCP read-only sin cambios)
```
- Gradle reconciliado a forma canónica PowerShell (3 tareas), sin duplicados Bash.
- +`git diff`/`git log` (read-only, alta frecuencia, PowerShell-canónico). Sin permisos de escritura nuevos.
- Perms Firebase MCP read-only ya presentes: sin cambios (solo se mantienen las "ya usadas").

**Hooks — antes → después:** los 4 matchers (`Write|Edit|MultiEdit`, `Read|Glob`, `Bash` Pre, `Bash` Post)
usan ahora la MISMA invocación self-locating `node -e "…require(mindos-guard.cjs)"`. **Cero `python3`.**

### `.claude/hooks/mindos-guard.cjs`
- +`path` + `projectDir` (`CLAUDE_PROJECT_DIR||cwd`) + `graphExists()` (`fs.existsSync graphify-out/graph.json`).
- +bloque `PreToolUse` `Bash` → recordatorio Graphify si el comando es grep/rg/find/fd/ack/ag y el grafo existe.
- +bloque `PreToolUse` `Read|Glob` → recordatorio Graphify si el path/patrón es de fuente/docs (skip `graphify-out/`).
- Bloques existentes (config-protection, secret-scan, fallo Gradle Post-Bash) intactos.

---

## Verificación (config de agente, NO build)

- `settings.json` es JSON válido (`node -e "JSON.parse(readFileSync(...))"`) y **no contiene `python3`**.
- Simulación del guard con stdin real:
  - `PreToolUse Read Foo.kt` → **emerge** el recordatorio Graphify. ✅
  - `PreToolUse Read graphify-out/GRAPH_REPORT.md` → **no emerge** (skip correcto). ✅
  - `PreToolUse Bash "grep -r foo ."` → **emerge** el recordatorio. ✅
  - `PreToolUse Write firestore.rules` → **sigue** avisando (config protegida). ✅
- **No** se corrió `:shared:detekt`/`:shared:desktopTest`/Kover: no verifican config de `.claude/`, y no se
  tocó Kotlin/Gradle.
- **Caveat honesto:** los hooks vivos pueden requerir reinicio de sesión de Claude Code para recargar
  `settings.json`; la prueba anterior valida el **script** de forma determinista (que es donde estaba el defecto).

---

## Rollback exacto

```powershell
# Revertir los 3 archivos tocados a su estado previo (working-tree, sin commits):
git checkout -- .claude/settings.json .claude/hooks/mindos-guard.cjs CLAUDE.md
# Y borrar este informe si no se quiere:
Remove-Item docs/tooling/CLAUDE_CODE_SETUP_AUDIT.md
```
(Para desactivar TODOS los hooks: borrar el bloque `"hooks"` de `.claude/settings.json` — deja el guard inerte.)

---

## Siguiente paso recomendado

1. **Aprobar C1 (`context7` MCP)** — el opt-in de mayor valor; crear `/.mcp.json` con el bloque del README
   (solo `context7`, **no** `sequential-thinking`).
2. **Opcional C2 (copy-safety JIT)** — pegar el snippet en `mindos-guard.cjs` si el equipo edita copy a menudo.
3. Tras estos cambios, correr **`/security-scan`** (AgentShield) sobre `.claude/` para auditar los hooks/permisos nuevos.

---

## Post-cambios — AgentShield (`/security-scan`, 2026-07-06)

**C1 aplicado:** `/.mcp.json` creado con **solo `context7`** (sin `sequential-thinking`).

Se corrió `npx ecc-agentshield scan` dos veces. La lente correcta es la acotada a `.claude/`:

| Scan | Target | Grade | Por qué |
|---|---|---|---|
| Root (ruido) | `.` | **F (34)** | Barre `CLAUDE.md` (falsos positivos), 34 skills y **worktrees** (`.codex-security-worktrees/…`, `.claude/worktrees/*`) → 540 findings duplicados |
| Acotado (señal) | `.claude` | **C (74)** | Superficie real de config de agente |

**Score acotado (`.claude/`):** Secrets 70 · Permissions 0 · **Hooks 100** · **MCP 100** · **Agents 100**.

> **Veredicto sobre lo que esta pasada cambió: LIMPIO.** Hooks **100/100** (rewrite Node de
> `mindos-guard.cjs` + los 4 matchers), MCP **100/100** (el nuevo `.mcp.json`/context7 — el `@latest`
> unpinned NO disparó hallazgo), Agents **100/100**. Cero regresiones. El grado bajo del scan root lo
> causan falsos positivos y ruido pre-existente, no los cambios de esta auditoría.

### Triaje de los 16 HIGH acotados (5 únicos, ×2–3 por copias en worktrees Vibe Kanban)

| Hallazgo | Clasificación | ¿Mi scope? | Acción |
|---|---|---|---|
| **`No deny list` en `settings.json`** | **Real, accionable** | **Sí** | **APLICADO ✅** — `permissions.deny` añadido (verificado: 16→15 HIGH) |
| `CLAUDE.md 0o666 world-writable` (auto-fixable) | **Artefacto Windows** — node reporta `0o666` para TODO archivo regular en NTFS; `chmod 600` no cambia la ACL real | No | Sin acción en Windows (sí importaría en host Unix/CI) |
| `python3 -c …` + `find /root/.gradle`,`/home/curro/.gradle` en **`settings.local.json`** | **Cruft local pre-existente** (grants personales gitignored de sesiones previas; AgentShield los marca "project-local optional") | No (archivo personal) | Recomendado: podar tus aprobaciones locales |
| `Sensitive env var: PasswordResetTimeout` (worktrees/CLAUDE.md) | **Falso positivo** — nombre de constante de timeout documentado en un aprendizaje (`PasswordResetTimeout=15s`), no un secreto | No | Descartar |
| 32 skills/commands `unobserved` + `base64 payloads`/`zero-width` (root, en `CLAUDE.md`) | **Ruido / FP** — "unobserved" es el framework de instrumentación de ECC (no vuln); los "base64" son RUTAS de archivo largas del log de aprendizajes; los zero-width son ejemplos documentados de un bug pasado | No | Descartar |

### Único ítem real en mi scope — APLICADO: deny list en `settings.json`

Aditivo y solo-restrictivo (nunca concede). Bloquea ops catastróficas sin estorbar a Vibe Kanban/n8n:

```jsonc
// dentro de "permissions", junto a "allow":
"deny": [
  "Bash(rm -rf:*)",
  "Bash(git push --force:*)",
  "Bash(git push -f:*)",
  "Read(./**/*.jks)",
  "Read(./**/*.keystore)",
  "Read(./**/.env)"
]
```

> **Aplicado y verificado** (2026-07-06): el finding "No deny list" de `settings.json` se limpió
> (16→15 HIGH; `settings.json` ya no figura en la lista). Los "No deny list" restantes son de
> `settings.local.json` (personal, gitignored) y sus copias en worktrees — fuera de scope. El `--fix`
> de AgentShield (chmod 600 a `CLAUDE.md`) NO se corrió: en Windows es notional y podría confundir el
> tracking de modo de git.
