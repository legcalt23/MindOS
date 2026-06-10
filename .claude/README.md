# MindOS · Claude Code toolkit (ECC curated port)

Este directorio es un **port curado** de [ECC — Everything Claude Code](https://ecc.tools)
(`v2.0.0-rc.1`, por Affaan Mustafa, MIT) adaptado al stack real de MindOS:
**Kotlin Multiplatform · Compose Multiplatform · Android · iOS/Swift · Firebase ·
Cloud Functions (TypeScript)**.

No se instaló el plugin completo (249 skills / 63 agentes / runtime de hooks Node). Se
copió **solo lo que aplica a MindOS** y se versiona en git, de modo que viaja con el repo
y los worktrees de Vibe Kanban sin depender de nada global.

> Las skills de pensamiento (`pre-mortem`, `primeros-principios`, `red-team`, `steelman`)
> y las de animación Compose preexistentes **no se tocaron**.

---

## Qué hay aquí

### `skills/` — 23 skills de stack (auto-activadas por descripción)
No necesitas invocarlas: Claude las activa cuando la tarea coincide con su descripción.

| Área | Skills |
|------|--------|
| Kotlin / KMP | `android-clean-architecture`, `compose-multiplatform-patterns`, `kotlin-coroutines-flows`, `kotlin-patterns`, `kotlin-testing` |
| iOS / Swift | `swift-concurrency-6-2`, `swiftui-patterns`, `swift-actor-persistence`, `swift-protocol-di-testing` |
| Backend (TS Functions) | `backend-patterns`, `api-design` |
| Seguridad | `security-review`, `security-scan` |
| Calidad / fiabilidad | `production-audit`, `verification-loop`, `error-handling` |
| UI / UX | `design-system`, `make-interfaces-feel-better`, `accessibility` |
| Workflow / meta | `codebase-onboarding`, `git-workflow`, `context-budget` |

### `agents/` — 13 subagentes especializados (vía la tool Agent/Task)
`kotlin-reviewer`, `kotlin-build-resolver`, `swift-reviewer`, `swift-build-resolver`,
`typescript-reviewer`, `security-reviewer`, `database-reviewer`, `architect`, `planner`,
`code-reviewer`, `silent-failure-hunter`, `performance-optimizer`, `refactor-cleaner`.

Todos llevan el **Prompt Defense Baseline** de ECC (no filtrar secretos, tratar input
externo como no confiable, etc.).

### `commands/` — 8 slash commands
| Comando | Qué hace |
|---------|----------|
| `/kotlin-build` | Arregla errores de build Kotlin/Gradle incrementalmente (1 a la vez, verificando) |
| `/gradle-build` | Igual, orientado a Android/KMP (compileKotlinMetadata, compileDebugKotlin…) |
| `/kotlin-review` | Review de calidad Kotlin (idioms, null-safety, coroutines, seguridad) |
| `/kotlin-test` | TDD Kotlin: tests primero (Kotest), luego implementación, cobertura Kover |
| `/security-scan` | Audita el propio `.claude/` con AgentShield (ver opt-in abajo) |
| `/plan` | Replantea requisitos, evalúa riesgos, plan paso a paso (espera confirmación) |
| `/quality-gate` | Pipeline de calidad (format + typecheck + lint + tests) sobre un scope |
| `/refactor-clean` | Detecta y elimina código muerto con verificación tras cada cambio |

> Se evitaron a propósito comandos que colisionan con built-ins de tu Claude Code
> (`/code-review`, `/security-review`, `/verify`, `/simplify`, `/review`).

### `rules/` — referencia por lenguaje
`kotlin/`, `swift/`, `typescript/`, `common/` — cada uno con `coding-style`, `patterns`,
`security`, `testing`. Son material de consulta para los reviewers (no se auto-enforzan;
el enforcement vive en los hooks ligeros + agentes).

### `hooks/mindos-guard.cjs` — guard ligero, **no bloqueante**
Cableado en `settings.json`. Nunca bloquea una tool call (siempre `exit 0`); solo avisa.

- **PreToolUse (Write/Edit/MultiEdit)**
  - *Config protection*: avisa al tocar archivos críticos (`firestore.rules`, `storage.rules`,
    `detekt-baseline.xml`, `libs.versions.toml`, `google-services.json`, manifests de
    backup/network…) recordando los invariantes de MindOS (límites firestore.rules ↔ `.limit()`
    cliente; baseline detekt solo tras auditoría; signing nunca hardcodeado).
  - *Secret scan*: detecta patrones de alta confianza (claves PEM, service-account GCP, tokens
    AWS/Slack/GitHub, Google API keys). Excluye `google-services.json`/`GoogleService-Info.plist`
    (su API key de cliente es pública y sí va al repo) para no dar falsos positivos.
- **PostToolUse (Bash)**: si un `gradlew`/`detekt` termina en `BUILD FAILED`, sugiere
  `/gradle-build` o `/kotlin-build`.

Es self-locating (`CLAUDE_PROJECT_DIR`), así que funciona en PowerShell, cmd y bash, y en
worktrees. Para desactivarlo: borra el bloque `hooks` de `settings.json`.

---

## Opt-in: MCP servers (no instalados — requieren tu aprobación)

No se añadió `.mcp.json` porque lanza código externo (`npx`) al arrancar. Firebase MCP ya lo
tienes a nivel global. Si quieres los dos servidores keyless más útiles para este stack,
crea `/.mcp.json` en la raíz del repo con:

```json
{
  "mcpServers": {
    "context7": {
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp@latest"]
    },
    "sequential-thinking": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-sequential-thinking@latest"]
    }
  }
}
```

- **context7** — docs API en vivo (Compose, Kotlin, Firebase, KMP) por versión. Alto valor.
- **sequential-thinking** — razonamiento estructurado (algo redundante con el thinking nativo).

Claude Code te pedirá aprobar cada server la primera vez (es lo esperado).

## Opt-in: AgentShield (scanner de `.claude/`)

`/security-scan` usa `npx ecc-agentshield scan .` — un scanner standalone (mismo autor de ECC)
que audita esta carpeta `.claude/` buscando inyección de prompts, permisos demasiado amplios y
MCP riesgosos. No necesita instalar el plugin ECC completo.

---

## Lo que se excluyó (a propósito)

- Stacks que MindOS no usa (Go, Rust, Django, healthcare, blockchain, homelab, Flutter…).
- Skills de animación/motion (tu memoria fija: *animaciones solo bajo petición explícita*).
- El runtime pesado de hooks de ECC (continuous-learning, governance, cost-tracking) — elegiste
  el set ligero.
- `plan-orchestrate` (dependía de `/orchestrate`, no portado).

## Atribución

Material derivado de **ECC — Everything Claude Code** (MIT) · https://github.com/affaan-m/ECC ·
https://ecc.tools — curado y adaptado para MindOS.
