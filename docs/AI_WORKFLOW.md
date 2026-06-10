# MindOS — Flujo de trabajo con agentes de IA

Reglas operativas para cualquier agente (Claude Code, Codex, o un humano dirigiéndolos vía
**Vibe Kanban**) que edite este repositorio. **El código de MindOS es la fuente de verdad.**

> Instalación y arranque de Vibe Kanban: [`docs/VIBE_KANBAN_SETUP.md`](VIBE_KANBAN_SETUP.md).
> Aprendizajes acumulados (errores ya corregidos = reglas activas): [`CLAUDE.md`](../CLAUDE.md) y [`AGENTS.md`](../AGENTS.md).

---

## 1. Arquitectura (mapa rápido)

MindOS es un proyecto **Kotlin Multiplatform (KMP) + Compose Multiplatform** con backend
**Firebase Cloud Functions (TypeScript)**.

| Carpeta | Qué es | Toolchain |
|---|---|---|
| `shared/` | Núcleo KMP: `commonMain` (lógica, UI Compose, dominio) + `androidMain`/`desktopMain`/`iosMain` (actuals de plataforma) | Kotlin / Gradle |
| `androidApp/` | App Android (entry point, manifest, Firebase, signing) | AGP / Gradle |
| `desktopApp/` | App de escritorio (target de prueba rápido, sin SDK Android) | Compose Desktop |
| `iosApp/` | Scaffold iOS (Xcode/Podfile; build real diferido a macOS) | Xcode (no aquí) |
| `functions/` | Cloud Functions (wallet, webhooks, posts, account deletion…) | TypeScript / Jest |
| `mindos/` | Plataforma "Action" (command router, matching, security) — módulos KMP propios | Kotlin |
| `infra/`, `docs/` | Infra, seguridad, release runbooks, decisiones de arquitectura | — |
| `firestore.rules`, `storage.rules` | Reglas de seguridad Firebase | emulador (JDK 21) |

Convenciones clave (de los aprendizajes en `CLAUDE.md` — son **restricciones activas**):

- Tokens visuales **solo** en `shared/.../design/` (`AppColors`, `AppSpacing`, …). Nunca `Color(0xFF…)` local.
- Toda operación suspendible de Firebase va envuelta en `with*Timeout`.
- Navegación con `AppRouteStack` (back stack), nunca `var route` suelto.
- `:shared` **no** depende de SDKs Android-only; usa el patrón callback/interface.
- Cada `expect` tiene `actual` en android/desktop/ios.
- Toda regla `allow list` exige `request.query.limit`; el cliente debe fijar `.limit(N)` espejo.

---

## 2. Reglas de edición

1. **Lee antes de escribir.** Revisa la sección *Aprendizajes acumulados* de `CLAUDE.md` antes de tocar código relacionado.
2. **No cambies lógica de negocio** fuera del alcance de la tarea asignada.
3. **Una tarea = un cambio acotado.** Si descubres trabajo adicional, créalo como tarea aparte.
4. **No reescribas en sitio** sistemas grandes con tests; prefiere feature aislada + un punto de wiring (ver aprendizaje 2026-05-30, perfil MindOS).
5. **Mantén el bucle de aprendizaje:** si corriges un error real (compilación/crash/navegación/Firebase/Gradle…), añade una entrada a `CLAUDE.md` / `AGENTS.md` con el formato exacto definido ahí.

---

## 3. Comandos de build / test / verificación

Usa los scripts (envoltura segura) o los comandos directos:

| Objetivo | Script | Comando directo |
|---|---|---|
| Auditar entorno | `scripts/check-ai-env.ps1` | — |
| Tests Desktop (rápido, KMP) | `scripts/safe-build.ps1 desktop` | `./gradlew :shared:desktopTest` |
| Build Android debug | `scripts/safe-build.ps1 android` | `./gradlew :androidApp:assembleDebug` |
| Análisis estático | `scripts/safe-build.ps1 detekt` | `./gradlew detekt` |
| Backend (secrets+types+jest) | `scripts/safe-build.ps1 functions` | `cd functions; npm run verify` |
| Reglas Firebase (JDK 21) | — | `npm run test:rules` (raíz) |
| Verificación pre-merge | `scripts/verify-mindos.ps1 -Full` | — |

Notas:
- **Desktop** no necesita Android SDK → es el smoke test más rápido para un agente.
- **`functions/`** declara Node 20 en `engines`; la máquina usa Node 24. Sirve para dev/test local;
  para `deploy` real respeta la versión de runtime de Functions (no desplegar a ciegas).
- El **emulador de reglas** exige JDK 21+ (ya disponible). El build Kotlin usa toolchain Gradle.

---

## 4. Normas de commits

- Mensajes en imperativo, una línea de asunto clara (`Add…`, `Fix…`, `Refactor…`), cuerpo opcional explicando *por qué*.
- **Un commit = un cambio coherente.** No mezclar refactor + feature + formato.
- **No commitear automáticamente** sin que el responsable humano lo pida.
- **Nunca** usar `--no-verify` ni saltarse hooks.
- No commitear artefactos de build, logs, ni nada cubierto por `.gitignore`.

## 5. Normas de ramas

- `main` es protegida: **ningún agente trabaja ni commitea sobre `main`/`master`**.
- Vibe Kanban crea **un worktree + rama por tarea**. Para trabajo manual: rama `feat/…`, `fix/…`, `chore/…`.
- Una rama por tarea; PR pequeño y revisable. Rebase/merge solo tras pasar la verificación.

---

## 6. Secretos y archivos críticos

**Prohibido tocar secretos.** `.gitignore` ya excluye `.env*`, `*.jks`, `*.keystore`,
`key.properties`, `*-service-account*.json`, `firebase-adminsdk-*.json`, `*.pem/.p12/.pfx`, claves RSA.

- **No** copiar valores de secretos a documentación, issues, ni mensajes de commit.
- **No** convertir un archivo gitignored en trackeado.
- `google-services.json` y `GoogleService-Info.plist` **sí** están versionados: son **config cliente
  de Firebase, no secretos** (la seguridad la imponen las reglas + App Check). No confundir con service-accounts.

**Archivos críticos — no modificar sin justificación explícita en la tarea:**
`firestore.rules`, `storage.rules`, `androidApp/build.gradle.kts` (signing/applicationId),
`gradle/libs.versions.toml`, `.github/workflows/**`, `functions/src/lib/**` (wallet/ledger/guards),
`google-services.json` / `*.plist`.

---

## 7. Reparto de agentes recomendado

| Fase | Agente | Por qué |
|---|---|---|
| **Planificar / revisar / seguridad** | **Claude Code** | razona arquitectura KMP, audita diffs, conoce los aprendizajes de `CLAUDE.md` |
| **Implementar / refactorizar** | **Codex CLI** | rápido en cambios mecánicos y acotados |
| **Verificar** | cualquiera + scripts | `verify-mindos.ps1 -Full` es la prueba objetiva |

Patrón típico de una tarea en Vibe Kanban:
1. **Claude Code** redacta el plan y los criterios de aceptación (no escribe aún).
2. **Codex** implementa en su worktree contra ese plan.
3. **Claude Code** revisa el diff (correctness, seguridad, convenciones).
4. Se corre `verify-mindos.ps1 -Full` en el worktree.
5. Humano aprueba → merge.

> Solo añade un tercer agente si Vibe Kanban lo **soporta** y está **autenticado**. Nota: el
> **Antigravity CLI (`agy`)** ya está instalado (sucesor de Gemini CLI), pero VK **aún no lo lista**
> como agente — úsalo standalone por ahora. Ver `docs/VIBE_KANBAN_SETUP.md › 6.1`.

---

## 8. Checklist antes de merge

- [ ] La rama **no** es `main`/`master`.
- [ ] El cambio está dentro del alcance de la tarea (sin lógica de negocio colateral).
- [ ] `scripts/verify-mindos.ps1 -Full` pasa (detekt + desktopTest + functions verify).
- [ ] Si tocó reglas Firebase: `npm run test:rules` verde, y el cliente fija `.limit()` espejo.
- [ ] Si tocó `expect`: hay `actual` en android/desktop/ios.
- [ ] Sin secretos nuevos trackeados (`verify-mindos.ps1` lo comprueba).
- [ ] Si corrigió un error real: entrada nueva en `CLAUDE.md` / `AGENTS.md`.
- [ ] Commit atómico, mensaje claro, sin `--no-verify`.

---

## 9. Riesgos pendientes / próximos pasos

- **Auth de agentes** es interactiva y personal (suscripción). No automatizable sin exponer claves.
- **iOS**: build real diferido a macOS (scaffold listo). Verificación pendiente.
- **Worktrees + Android**: requieren copiar `local.properties` manualmente (gitignored).
- **Functions Node 20 vs local 24**: ok para test; cuidado en `deploy`.
- **Mejora futura (opcional):** DevContainer Docker con Node + JDK 21 + CLIs para sandbox de agentes.
