# Gate de claim-safety del marketing de MindBoost

> **Checkpoint obligatorio antes de publicar** cualquier copy: ficha de Play, screenshot/caption, post de
> comunidad, guion de vídeo, push de retención, pitch de PR, email a partners.

## Fuente de verdad

`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` — léxico
canónico consumido por el `mindboostCopySafetyScanner` de build. Vive FUERA de los dirs escaneados, así que
puede contener los literales sin auto-dispararse. **Este documento NO reproduce esos lexemas** (misma
disciplina): los referimos por **familia**, nunca verbatim.

## Familias prohibidas (por nombre, sin lexemas)

| Familia | Qué prohíbe (descripción, sin nombrar términos) |
|---|---|
| `FAR_TRANSFER` | Promesas de capacidad general y de transferencia lejana (a la vida diaria, al estudio, etc.). |
| `CLINICAL` | Cualquier claim de salud o de condición médica (evaluación, intervención, recuperación…). |
| `GAMBLING` | Todo el léxico de azar (relevante sobre todo para el módulo Decisiones). |
| `MISINFORMATION` | Léxico regulatorio del módulo Lenguaje sobre información y verificación (ver la familia en el archivo fuente). |
| `PSEUDOSCIENCE` | Promesas mágicas sobre el órgano cognitivo y «garantías» pseudocientíficas. |

**Invariante (no negociable):** cero de esos lexemas en el copy, **ni siquiera para negarlos**. El matcher es
palabra-completa, insensible a mayúsculas y acentos, y cruza guiones/negaciones.

## Léxico permitido

"perfil cognitivo funcional" · "práctica" · "entrenamiento" · "progreso frente a tu propia línea base" ·
"dificultad adaptativa" · "orientativo" · "entrena/practica/trabaja tu concentración" · "rutina de foco medible"
· "constancia" · "hábito" · "racha" · "medición honesta" · "CALIBRANDO / —".

## Regla del "hype rechazado"

Cuando documentes qué NO decir, represéntalo por **categoría entre corchetes** («[promesa de capacidad
general]», «[claim de salud]», «[transferencia lejana]», «[léxico de azar]», «[promesa pseudocientífica]»).
Nunca lo escribas verbatim.

## Matiz "intención vs promesa"

Se puede **capturar la intención de búsqueda** de términos de alto volumen (p. ej. concentración para estudiar)
tejiéndolos en la copy, pero el texto visible describe la **PRÁCTICA** ("entrena/practica tu concentración") y
el **progreso-vs-tu-línea-base**, nunca el **resultado** de capacidad. "entrena tu concentración" ✅ ·
"mejora tu concentración" como promesa ⚠️ (evítalo; usa "entrena/practica/trabaja").

## Checklist pre-publicación

- [ ] ¿El texto describe la práctica/el progreso-vs-baseline, y NO promete un resultado de capacidad?
- [ ] ¿Cero lexemas de las 5 familias (incluidos comentarios/disclaimers), ni para negarlos?
- [ ] Disclaimers redactados sin lexemas de las familias prohibidas: usa "es una herramienta de práctica y
      progreso personal; no es una herramienta de evaluación; su perfil es orientativo".
- [ ] Prueba social: solo números **reales**, nunca fabricados (`null ≠ 0` también en marketing).
- [ ] Presencia/contadores en vivo: solo si hay fuente de datos real detrás.

## Cómo enchufa con el resto

- **Skill `review-cognitive-claims`:** correr sobre cualquier copy nuevo antes de publicar — es el checkpoint humano/agente de esta regla.
- **`mindboostCopySafetyScanner` (build):** aplica al copy que llega a `AppStrings.kt` o a los dirs de feature
  (`focus/memory/speed/logic/decisiones/language/creativity`). Correr la variante del source-set tocado
  (`…CommonTest`/`…DesktopTest`) si el copy se cablea al runtime.
- **Este directorio (`docs/marketing/`) NO lo escanea el scanner de build** — por eso el checklist es manual y
  obligatorio aquí.
