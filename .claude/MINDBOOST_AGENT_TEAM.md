# Equipo de agentes MindBoost — Claude Code

Sistema de agentes especializados + orquestador para construir el mejor entrenador cognitivo del mundo en Kotlin Multiplatform. Foco único: **maximizar la capacidad cognitiva real del usuario promedio**, con honestidad científica como principio no negociable.

> **Estado en este repo (MindOS):** instalado y adaptado el 2026-06-30. Los 10 agentes están en
> `.claude/agents/`, el orquestador en `.claude/commands/mb.md` y la skill en
> `.claude/skills/cognitive-science-principles/`. Las rutas de docs, el layout de módulos y las
> raíces de paquete se ajustaron a la realidad de este repo (ver más abajo). Convive con la flota
> previa `mindboost-*` / `mindverse-*`; este es un equipo paralelo más fino, no la reemplaza.

---

## Nota de arquitectura (importante)

Querías un orquestador que fuese "uno de los agentes" y repartiese a cada equipo. Restricción dura de Claude Code: **un subagente no puede lanzar otros subagentes**. Si el orquestador fuese un subagente, sería un callejón sin salida.

Por eso el orquestador está implementado como un **comando** (`/mb`) que convierte tu sesión principal en el lead que reparte trabajo. Ese es el "un comando" que pediste, y respeta el límite del sistema: un comando → enruta a todo el equipo.

---

## El equipo (orquestador + 5 squads, 10 agentes)

**Entrada:** `/mb <objetivo>` — planifica en slices verticales y enruta.

**Squad A — Ciencia y Eficacia (el foso)**
- `efficacy-guardian` — guarda la validez científica y la honestidad de transferencia. Gate de eficacia. Solo lectura. (opus)
- `psychometrician` — fórmulas de scoring, métricas de RT, dificultad adaptativa, fiabilidad, anti-gaming. (opus)

**Squad B — Núcleo KMP**
- `kmp-architect` — estructura de módulos, expect/actual, Gradle, fronteras. (sonnet)
- `cognitive-engine-dev` — motor cognitivo puro en commonMain. (sonnet)

**Squad C — Experiencia**
- `compose-ui-dev` — pantallas Compose MP, design system, panel de control. (sonnet)
- `motion-experience` — animaciones y "feel" cinematográfico (Vision Pro / Villeneuve). (sonnet)

**Squad D — Datos y Plataforma**
- `data-sync-dev` — Firestore + offline-first (SQLDelight), perfil cognitivo dinámico. (sonnet)
- `privacy-security` — privacidad de datos sensibles, secretos, permisos. Solo lectura. (sonnet)

**Squad E — Calidad (los raíles)**
- `test-engineer` — tests, sobre todo verificación de la matemática del scoring. (sonnet)
- `diff-auditor` — revisión de diff en contexto fresco. Gate de salida. Solo lectura. (sonnet)

La potencia (opus) se concentra en el foso científico, que es tu prioridad nº1 y tu diferenciador de marca.

---

## Pipeline de un slice

1. `efficacy-guardian` + `psychometrician` definen/validan QUÉ se mide → **gate de entrada**.
2. `cognitive-engine-dev` y `data-sync-dev` en paralelo (si son independientes).
3. `compose-ui-dev` → `motion-experience`.
4. `test-engineer` blinda la lógica (casos input→output conocidos).
5. `efficacy-guardian` + `diff-auditor` revisan en contexto fresco → **gate de salida**. Si se tocaron datos del usuario, `privacy-security` también firma.

**Regla dura:** nada se da por terminado sin el visto bueno del `efficacy-guardian`.

---

## Instalación

Ya está instalado en este repo (los archivos viven en `.claude/`). Para replicarlo en otra máquina o
repo:

1. Copia la carpeta `.claude/` a la raíz del repo MindBoost (fusiónala si ya tienes una).
2. Versiona `.claude/agents/`, `.claude/commands/` y `.claude/skills/` en git para que el equipo (y tú en ambas máquinas) los compartáis.
3. Reinicia la sesión de Claude Code en la raíz del proyecto.
4. Verifica con `/agents` (deberías ver los 10) y que `/mb` aparece como comando.

### Modelo y coste
- Corre la sesión principal en **opus** (el orquestador necesita juicio): `claude --model opus`.
- Multi-agente gasta 4-7x tokens. El `/mb` usa paralelo solo cuando el tiempo importa; secuencial cuando importa el coste.
- Puedes fijar un modelo por defecto para subagentes con `CLAUDE_CODE_SUBAGENT_MODEL`, o editar el campo `model:` de cada agente (acepta `opus`/`sonnet`/`haiku`/`inherit`, o una versión concreta).

---

## Conecta tu documentación científica (clave)

Los agentes de ciencia ya cargan la doctrina transversal vía la skill `cognitive-science-principles` (proceso > resultado, anti-falso-progreso, honestidad de far-transfer citando Owen/Simons/Melby-Lervåg/Sala & Gobet, gating de impulsividad, validez de la dificultad adaptativa).

Para los **docs densos de cada módulo**, no los metas en la skill ni en `CLAUDE.md` (las skills se inyectan enteras y los inflarían). En este repo los docs **ya existen** y los agentes están instruidos para **leerlos bajo demanda** según el módulo en el que trabajen:

```
Foco / Enfoque   → docs/mindboost_enfoque_implementation_plan/
Velocidad Mental → docs/mindboost_velocidad_implementation_plan/
Lógica           → docs/mindboost_logica_implementation_plan/
Emociones        → docs/mindboost_emociones_implementation_plan/
Lenguaje         → docs/mindboost_lenguaje/
Memoria          → docs/mindboost_memoria_implementation_plan/
Decisiones       → docs/mindboost_exercise_design/03_6_Decisiones.md
Diseño maestro / scoring común → docs/mindboost_exercise_design/
```

Así cargan solo el módulo en el que trabajan, sin saturar contexto. El código de cada módulo vive en
`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/<módulo>/` (el compute core de
Decisiones, en `com.mindverse.mindboost.core`).

Este repo ya tiene un `CLAUDE.md` extenso con las reglas duras (lógica fuera de UI, nada de plataforma
en commonMain, ningún cambio científico sin pasar por `efficacy-guardian`, estado de score honesto
`null` ≠ 0, lenguaje no-clínico). El rename de paquete `com.example.neu` → `com.mindos.app` **ya está
hecho**: no hay nada pendiente ahí.

---

## Uso

```
/mb implementa el primer ejercicio del módulo Foco de punta a punta
/mb valida que el scoring de Decisiones penaliza la suerte y mide calibración
/mb audita la privacidad del perfil cognitivo antes de subirlo a Firestore
```

Para invocar un agente directo, sáltate el orquestador:
> Usa el subagente psychometrician para revisar la fórmula de Brier de Decisiones.
