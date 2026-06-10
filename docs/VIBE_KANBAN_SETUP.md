# Vibe Kanban — Setup local para MindOS

Guía para correr **Vibe Kanban en local** (sin alojamiento externo) y orquestar agentes de
codificación (Claude Code, Codex CLI) sobre este repositorio.

> Fuente de verdad del flujo de trabajo: [`docs/AI_WORKFLOW.md`](AI_WORKFLOW.md).
> Esta guía cubre **instalación y arranque**; la otra cubre **cómo trabajar**.

---

## 1. Qué es y qué NO es este setup

- **Local-first.** Vibe Kanban corre en tu máquina vía `npx`. No se sube código a ningún
  servidor de terceros: los agentes corren con tus CLIs locales y sus sesiones (suscripción).
- **Sin API keys obligatorias.** Claude Code y Codex se autentican por **login de suscripción**
  (`/login` y `codex login`). No hace falta meter claves en el repo.
- **Aislamiento por tarea.** Vibe Kanban crea un **git worktree** por tarea, así que los agentes
  **no comparten** la misma copia de trabajo y **no tocan `main`** directamente.

---

## 2. Requisitos (verificados en esta máquina · 2026-06-04)

| Herramienta | Versión detectada | Necesaria para |
|---|---|---|
| Node.js | v24.14.1 | Vibe Kanban (npx) + agentes |
| npm / npx | 11.12.1 | lanzar Vibe Kanban |
| Git | 2.54.0 | worktrees por tarea |
| Docker | 29.5.2 | (opcional) sandbox / emuladores |
| Java (Temurin) | 21.0.10 | build Gradle + emulador de reglas |
| Gradle | wrapper (`gradlew.bat`) | builds KMP |
| Android SDK | `…\AppData\Local\Android\Sdk` | builds Android (no Desktop) |
| **Claude Code CLI** | **2.1.162** (instalada global) | agente arquitecto/reviewer |
| **Codex CLI** | **0.137.0** (instalada global) | agente implementador |

Verifícalo en cualquier momento:

```powershell
powershell -File scripts/check-ai-env.ps1
```

---

## 3. Instalación de los agentes (ya hecha)

Las CLIs se instalaron globalmente (única instalación global necesaria; Vibe Kanban en sí va por `npx`):

```powershell
npm install -g @anthropic-ai/claude-code @openai/codex
```

El directorio de binarios globales de npm (`C:\Users\curro\AppData\Roaming\npm`) ya está en tu
**PATH de usuario**, así que `claude` y `codex` se resuelven en cualquier terminal nueva.

### 3.1 Autenticación (interactiva — la haces tú una vez)

```powershell
claude          # dentro: /login   -> usa tu suscripción de Claude (sin API key)
codex login     # abre el flujo de login de ChatGPT/Codex (sin API key)
```

> Si prefieres API keys en lugar de suscripción, NO las pongas en el repo. Van en variables de
> entorno de tu usuario (`setx ANTHROPIC_API_KEY ...`). El default recomendado aquí es suscripción.

---

## 4. Arrancar Vibe Kanban

```powershell
powershell -File scripts/start-vibe-kanban.ps1
# equivale a:  npx vibe-kanban
```

- La primera vez descarga el binario y abre la **UI web local** (imprime la URL, típicamente
  `http://127.0.0.1:<puerto>`).
- Para parar: `Ctrl+C` en la ventana.

---

## 5. Añadir el repo MindOS a Vibe Kanban

1. En la UI, **Projects → Add / Create project**.
2. Selecciona la carpeta raíz del repo:
   `C:\Users\curro\AndroidStudioProjects\MindOS`
3. Configura la **base branch** del proyecto como `main` (de ahí salen los worktrees por tarea).
4. En **Settings → Agents**, confirma que detecta `claude` y `codex`. Si no, abre una terminal
   nueva (para refrescar PATH) y reinicia Vibe Kanban.

---

## 6. Configuración de agentes recomendada

| Rol | Agente | Uso |
|---|---|---|
| Arquitecto / Reviewer | **Claude Code** | planificar, revisar diffs, seguridad, decisiones KMP |
| Implementador / Refactor | **Codex CLI** | implementar tareas acotadas, refactors mecánicos |

Detalle del reparto y el porqué: [`docs/AI_WORKFLOW.md`](AI_WORKFLOW.md).

### 6.1 Antigravity CLI (`agy`) — instalado, pero aún no en Vibe Kanban

Google transicionó **Gemini CLI → Antigravity CLI** (Google I/O, 19-may-2026). El CLI oficial es
`agy` (binario Go), instalado en esta máquina vía winget (vía oficial y verificable, no npm):

```powershell
winget install --id Google.AntigravityCLI --exact   # ya hecho · v1.0.5 · comando: agy
agy --version                                        # 1.0.5
```

Login por **suscripción Google** (AI Pro/Ultra/Code Assist), sin API key — la 1ª ejecución abre el
navegador y guarda la credencial en el keyring del sistema:

```powershell
agy        # primer arranque -> login en navegador.   Cerrar sesión: /logout dentro de agy
```

> **Estado en Vibe Kanban (jun 2026):** la lista de agentes soportados de Vibe Kanban es
> *Claude Code, Codex, Gemini CLI, Cursor, Copilot, Amp, OpenCode, Droid, Qwen, CCR* — **todavía
> NO incluye Antigravity/`agy`**, y no hay opción de "ejecutable arbitrario". Por tanto, hoy `agy`
> se usa **standalone** (terminal), no seleccionable dentro de VK.
> - El agente de Google que VK sí soporta es **Gemini CLI**, pero Google **deja de servir Gemini
>   CLI para AI Pro/Ultra el 18-jun-2026**. Si dependes de Google dentro de VK, vigila la actualización
>   de Vibe Kanban que añada Antigravity, o usa una licencia Code Assist Standard/Enterprise.
> - Recomendación: en VK usa **Claude Code (arquitecto) + Codex (implementador)**, ya logueados, y
>   reserva `agy` para uso directo hasta que VK lo soporte nativamente.

> **Regla general:** Vibe Kanban solo orquesta agentes con **ejecutable de terminal soportado**.
> No asumas soporte de una herramienta solo porque exista su CLI: verifícalo en la lista de VK.

---

## 7. Aislamiento y seguridad de ramas

- Vibe Kanban crea **un git worktree por tarea** bajo un directorio propio → cada agente trabaja en
  su copia, nunca sobre tu checkout principal ni sobre `main`.
- **Tras crear un worktree para un build Android**, copia `local.properties` (no se hereda; está
  gitignored). Usa barras normales: `sdk.dir=C:/Users/curro/AppData/Local/Android/Sdk`
  (ver aprendizaje 2026-05-30 en `CLAUDE.md`).
- Antes de mergear el trabajo de un agente:
  ```powershell
  powershell -File scripts/verify-mindos.ps1 -Full
  ```

---

## 8. Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| Vibe Kanban no ve `claude`/`codex` | PATH stale en la terminal | abre terminal **nueva**, relanza |
| Agente pide API key | sin login de suscripción | `claude` → `/login`; `codex login` |
| Build Android falla "SDK location not found" | worktree sin `local.properties` | crear `local.properties` con `sdk.dir` (barras `/`) |
| Emulador de reglas falla | necesita JDK 21+ | ya tienes Temurin 21; verifica `java -version` |
| `npx vibe-kanban` lento la 1ª vez | descarga binario | normal; siguientes arranques son rápidos |

---

## 9. (Opcional) Sandbox con Docker

No es necesario para el flujo local simple. Si más adelante quieres aislar los agentes del sistema
host, la vía recomendada es un **DevContainer** que monte el repo y traiga Node + JDK 21 + las CLIs.
Queda como mejora futura (ver `docs/AI_WORKFLOW.md › Próximos pasos`), no como requisito.
