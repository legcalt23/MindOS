# INFRA-05 · CopySafetyScanner de build (multi-módulo, anti-negación)

> Issue P0. Fuente: `AUDIT_V2_TECNICO.md` (§9 INFRA-5, §11 copy), `AUDIT_V2_GROUND_TRUTH.md`
> (scanners de terminología), `AUDIT_V2_PRODUCTO.md` (mapa de claims).
> Leyenda: `hecho` · `inferido` · `por_confirmar`. Rutas no verificadas → `ruta probable`.

---

## Título
INFRA-05 · `CopySafetyScanner` enganchado a `check`, espejo de `arenaTerminologyScanner`

## Prioridad
P0 (mata el riesgo regulatorio/reputacional; el control técnico que garantiza que ningún screenshot
muestre far-transfer/clínico/IQ/apuesta/fake-news).

## Objetivo
Bloquear el build (`hecho` patrón de `check`) si aparece léxico prohibido — far-transfer, clínico,
IQ/inteligencia, apuesta, misinformación — **incluso en negaciones y compuestos con guion**
(Invariante #c: cero claims de far-transfer ni para negarlos). Hoy (`hecho`) existen DOS gates
acotados por carpeta: `arenaTerminologyScanner` (`features/arena/**`) y `emotionsTerminologyScanner`
(`features/mindboost/emotions/**`). **Cobertura cero** para focus, memory, speed, logic, **decisiones**,
**language**, creativity y el global `AppStrings.kt`.

## Archivos probables (`ruta probable`, salvo anclas `hecho`)
- `shared/build.gradle.kts` (registrar task `mindboostCopySafetyScanner` + variantes test; enganchar a
  `check`; espejo de `arenaTerminologyScanner` `:198-272` — `hecho`).
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` (NUEVO).
- Contraparte pura en código (`hecho` patrón): `arena/config/TerminologyScanner.kt`,
  `emotions/domain/safety/EmotionTerminologyScanner.kt`.

## Dependencias
- Ninguna de código; depende de acordar el lexicon (ver doc 12 `COPY_SAFETY_CLAIMS`).
- INFRA-01 enforca en UI los constructos absolutos sin `Valid`; el scanner cubre el **copy estático**
  que los afirme.

## Cambios técnicos
1. **Algoritmo de match** (`hecho` aprendizaje 2026-06-23): `Regex("\\b$term\\b")` case-insensitive;
   `-` actúa como borde → detecta el término dentro de compuestos con guion (`non-clinical`,
   `anti-fake-news`). Folding de acentos español (`á→a … ñ→n`) como `emotionsTerminologyScanner`.
2. **Anti-negación**: el match no exime un término por ir precedido de `no`/`non`/`sin`/`anti` — la
   sola presencia del lexema dispara (un "no es un test de inteligencia" usa "inteligencia" como
   disclaimer legítimo → ver §regresión de falsos positivos).
3. **Alcance**: `commonMain` + `commonTest` + `desktopTest` (los tests también filtran copy — `hecho`
   patrón Arena/Emotions). `requireDir=true` en commonMain (un typo no desactiva el gate — `hecho`).
4. **Cobertura nueva**: focus, memory, speed, logic, decisiones, language, creativity, `AppStrings.kt`.

## Lexicon inicial (`inferido` de §11 TECNICO; lista canónica en doc 12)
- **Far-transfer / vida real:** `inteligencia`, `IQ`, `CI`, `Gf`, `coeficiente intelectual`,
  `mejores decisiones`, `en la vida real`, `multitarea`, `productividad`, `trabajo`, `estudios`,
  `conduccion`, `conducir`, `al volante`, `regulacion emocional`, `inteligencia emocional`,
  `autocontrol`, `personalidad`, `te distraes menos con el movil`.
- **Salud / clínico:** `TDAH`, `ADHD`, `demencia`, `Alzheimer`, `ansiedad`, `depresion`,
  `diagnostico`, `clinico`, `terapeutico`, `tratamiento`, `cura`, `sintoma`, `trastorno`,
  `deterioro cognitivo`, `salud mental`.
- **Apuesta (Decisiones):** `apuesta`, `apostar`, `fichas`, `bote`, `casino`, `odds`, `cuota`,
  `bankroll`, `stake`, `bet`.
- **Misinformación (Lenguaje E6):** `fake news`, `noticias falsas`, `desinformacion`, `bulos`,
  `fuentes fiables`, `medios fiables`, `detectar mentiras`, `verificar hechos del mundo real`.
- **Pseudociencia / promesa:** `reconfigura tu cerebro`, `neuroplasticidad garantizada`,
  `potencia tu cerebro`, `% mas inteligente`, `aumenta tu CI`, `entrena tu cerebro como un musculo`.

> Nota (`hecho`): hoy el copy real es defensivo ("no es un test de inteligencia", disclaimers). El
> riesgo es de **regresión futura**, no de copy actual: por eso es gate de build, no limpieza puntual.
> El claim afirmativo más sensible HOY es `language ClaimEngine ROBUST_GENERALIZATION → "evidencia
> fuerte de transferencia amplia"` (`hecho`), sin gate de build: candidato #1 a revisar.

## Regresión de falsos positivos (obligatoria)
- Distinguir `balance` semántico (legítimo) de `bankroll` (apuesta) — el match whole-word ya lo logra.
- Decidir disclaimers legítimos: "no es un test de **inteligencia**" usa el lexema → o se reformula a
  vocabulario permitido ("no es una nota / práctica funcional") o se mantiene una **allowlist de
  frases-disclaimer exactas** auditada (`inferido`; preferible reformular para no abrir una puerta).

## Criterios de aceptación
- Detecta término prohibido como substring de palabra y en compuesto/negación/guion.
- Cubre `commonMain` + `commonTest` + `desktopTest` de los 7 módulos sin gate + `AppStrings.kt`.
- Enganchado a `check`; rojo bloquea merge.
- `requireDir=true` en commonMain.

## Tests obligatorios
- Cada lexema prohibido + su forma negada/compuesta ⇒ scanner rojo.
- Copy permitido (doc 12 §permitido) ⇒ verde.
- Regresión del falso positivo razonable (`balance` semántico vs `bankroll`).
- `ClaimEngine ROBUST_GENERALIZATION` queda cubierto o explícitamente recortado.

## Riesgos
- Reformular disclaimers existentes que usan "inteligencia" puede tocar muchos strings (`hecho`:
  focus/creativity tienen disclaimers con el lexema) → coordinar con copy permitido (doc 12).
- Sobre-bloqueo de términos polisémicos (`trabajo` de "memoria de trabajo") → usar whole-word +
  contexto de frase; "memoria de trabajo" es permitido, "rindes mejor en el trabajo" no (`inferido`,
  requiere afinado del lexicon: bloquear frases, no la palabra suelta `trabajo`).

## Definition of Done
- Task registrada, enganchada a `check`, espejo del patrón Arena.
- Lexicon canónico (doc 12) implementado en `ForbiddenLexicon.kt` + contraparte pura.
- Tests rojo/verde + regresión de FP verdes.
- `:shared:check` verde con el copy actual (tras reformular disclaimers si aplica).

## Comando/check de validación (`por_confirmar` nombre exacto)
- `:shared:mindboostCopySafetyScanner` (+ variantes `*CommonTest`/`*DesktopTest`) · `:shared:check`.
