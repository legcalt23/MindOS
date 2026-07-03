# 12 · Copy Safety & Claims (prohibidos, permitidos, TransferLabel, reglas UI, lexicon)

> Fuente: `AUDIT_V2_TECNICO.md` (§11 copy permitido/prohibido, backlog UX),
> `AUDIT_V2_PRODUCTO.md` (mapa de claims), `AUDIT_V2_GROUND_TRUTH.md` (scanners reales).
> Leyenda: `hecho` · `inferido` · `por_confirmar`. **Invariante #c:** cero claims de
> far-transfer/clínico/IQ/fake-news/regulación-emocional, **ni para negarlos**.
> **Invariante #a (`null != 0`):** estados no-VALID → "calibrando"/"—", nunca 0/percentil bajo.

---

## 1. Claims PROHIBIDOS (lexicon del scanner — INFRA-05)

Detectar como substring de palabra Y en compuesto/negación/guion (`hecho`: `\b` cruza guiones).

**Far-transfer / vida real (Regla #4):** `inteligencia`, `IQ`, `CI`, `Gf`,
`coeficiente intelectual`, `te hace mas listo`, `mejores decisiones`, `decides mejor`,
`en la vida real`, `multitarea`, `productividad`, `conduccion`, `conducir`, `al volante`,
`regulacion emocional`, `inteligencia emocional`, `controlas tus emociones`, `autocontrol`,
`personalidad`, `te distraes menos con el movil`, `usas menos el telefono`.

**Salud / clínico:** `TDAH`, `ADHD`, `demencia`, `Alzheimer`, `ansiedad`, `depresion`,
`diagnostico`, `clinico`, `terapeutico`, `tratamiento`, `cura`, `sintoma`, `trastorno`,
`deterioro cognitivo`, `salud mental`.

**Apuesta (Decisiones — renombrar):** `apuesta`, `apostar`, `fichas`, `bote`, `casino`, `odds`,
`cuota`, `bankroll`, `stake`, `bet`.

**Misinformación (Lenguaje E6 — regulatorio):** `fake news`, `noticias falsas`, `desinformacion`,
`bulos`, `fuentes fiables`, `medios fiables`, `detectar mentiras`,
`verificar hechos del mundo real`.

**Pseudociencia / promesa:** `reconfigura tu cerebro`, `neuroplasticidad garantizada`,
`potencia tu cerebro`, `desbloquea tu mente`, `% mas inteligente`, `aumenta tu CI N puntos`,
`entrena tu cerebro como un musculo` (como claim de transferencia).

**Constructos absolutos sin `Valid` (Regla #6):** mostrar "velocidad de procesamiento", "memoria de
trabajo", "control inhibitorio", "flexibilidad cognitiva" como NÚMERO/percentil cuando el
`ScoreResult` no es `VALID` ⇒ prohibido (INFRA-01 lo enforca en UI; el scanner cubre el copy estático
que lo afirme). Nota de afinado (`inferido`): bloquear la **frase** ("rindes mejor en el trabajo"), no
la palabra suelta polisémica ("memoria de **trabajo**" es permitido).

> Candidato #1 a revisar (`hecho`): `language ClaimEngine ROBUST_GENERALIZATION → "Hay evidencia
> fuerte de transferencia amplia"` — único claim afirmativo fuerte, sin gate de build. Recortar a
> task-only o gatearlo.

## 2. Claims PERMITIDOS (frases EXACTAS de UI)

**Estados de medición (INFRA-01):**
- "Señal de practica" · "Estimacion preliminar" · "Datos insuficientes para estimar" ·
  "Calibrando tu linea base" · "No actualizado por calidad de medicion" ·
  "Medicion contaminada — no contabilizada" · "Proxy interno, no diagnostico" ·
  "Aun calibrando — vuelve tras mas sesiones".

**Construct honesto (proxy, no absoluto):**
- "Practica de atencion sostenida en esta tarea" · "Tiempo de respuesta en esta tarea" ·
  "Aciertos frente a tu propia linea base" · "Dificultad adaptativa" ·
  "Perfil cognitivo funcional" · "Progreso contra tu linea base personal" ·
  "Reconocimiento de fidelidad del resumen" (Compression Ladder) ·
  "Aplicacion de los criterios dados en este ejercicio" (Source Triage) ·
  "Inferencia soportada por la evidencia mostrada" (Evidence Lock).

**Readiness / seguridad (Emociones, Check-in):**
- "Disponibilidad cognitiva de hoy" (semáforo, NO score) · "Esto es un chequeo, no una nota" ·
  "Puedes parar cuando quieras" (abandono = señal, no culpa).

## 3. TransferLabel por grupo de ejercicio

> `hecho`: `TransferLabel { TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }` ya existe en
> emotions, 1 por ejercicio, renderizado junto al feedback de 4 partes. Aquí se eleva a todos los
> grupos (`inferido` del mapa de claims de PRODUCTO; etiquetas NEAR_TASK_PRACTICE/NO_TRANSFER/
> EXPERIMENTAL_PROXY/SAME_TASK_IMPROVEMENT mapeadas al enum existente).

| Grupo | TransferLabel | Claim permitido | Claim prohibido |
|---|---|---|---|
| Enfoque RT (E1,E2,E4,E5,E6,E7,E9,E10,E12,E13) | TASK_ONLY (NEAR_TASK) | "señal de práctica en esta tarea; preliminar" | atención/multitarea/concentración en la vida |
| Enfoque E8 Dúo | NOT_DEMONSTRATED (NO_TRANSFER) | ninguno (mide ruido) | cualquier claim hasta rebuild |
| Enfoque E11/E13 | TASK_ONLY | "práctica bajo intrusión/deadline" | distracción digital real, estrés/trabajo/estudios |
| Velocidad E1,E2,E4,E6 | TASK_ONLY | "throughput en esta tarea, calibrando" | "procesas más rápido en general/IQ" |
| Velocidad E5,E7,E8 | PLACEBO_RISK (EXPERIMENTAL) | ninguno hasta infra | cualquier claim de velocidad |
| Lenguaje E1–E5 | TASK_ONLY | "práctica de inferencia soportada en esta tarea" | comprensión general, razonamiento crítico |
| Lenguaje E6 Source Triage | NOT_DEMONSTRATED (NO_TRANSFER) | "aplicar criterios estructurales DADOS en el ítem" | fake news, desinformación, evaluar medios |
| Memoria accuracy (EX1–EX8) | TASK_ONLY | "práctica; mejora solo vía sonda UNTRAINED" | memoria/inteligencia/Gf |
| Memoria EX_NBACK | TASK_ONLY | "updating en esta tarea" (si se implementa d') | IQ, inteligencia, Gf (bloquear en código) |
| Memoria TRANSFER_PROBE | PLACEBO_RISK (EXPERIMENTAL) | "índice de cambio fiable, solo con RCI≥2-3 admin" | mejora garantizada de capacidad |
| Decisiones E1,E2,E4 | TASK_ONLY | "práctica de actualización/triaje en esta tarea" | mejores decisiones vitales/financieras |
| Decisiones E3,E5,E6,E7,E8 | PLACEBO_RISK (EXPERIMENTAL) | ninguno hasta banco validado | "apuesta", debiasing real, decisiones de vida |
| Emociones EM1,EM2,EM8,EM11,EM15 | TASK_ONLY | "control inhibitorio/recuperación en esta tarea" | regulación emocional, autocontrol, IE |
| Emociones EM3,EM6,EM9,EM13 | NOT_DEMONSTRATED (NO_TRANSFER) | ninguno hasta banco/no-inducción | tolerancia a la frustración, regulación, IE, salud mental |
| Creatividad EX01,EX06 | TASK_ONLY | "fluidez/rareza relativa a banco en esta tarea" | "eres más creativo/innovador" |
| Creatividad EX02,EX04,EX08 | NOT_DEMONSTRATED/PLACEBO_RISK | ninguno hasta clasificador/modo proporcional/timebase | creatividad bajo presión, pensamiento lateral |

**Regla dura (`hecho`):** ningún ítem con TransferLabel ≠ SAME_TASK_IMPROVEMENT puede mostrar un
constructo absoluto en UI. Todo lo demás → "señal de práctica" / "calibrando".

## 4. Reglas UI (backlog UX/copy)

| Pantalla | Label/mensaje | Estado |
|---|---|---|
| Resultado de bloque | "Señal de practica registrada" + barra CALIBRATING (nunca 0) | INFRA-01 |
| Brain-profile radar | dimensiones CALIBRATING ⇒ "—", jamás 0 | `hecho` (respetar) |
| Insuficiencia | "Necesitamos N sesiones mas para estimar esto" | INFRA-06 |
| Invalidación | "Esta sesion no cuenta para tu perfil: medicion contaminada" | INFRA-02/03 |
| Timebase no fiable | "En este dispositivo solo medimos practica, no tu perfil" | INFRA-02 |
| Check-in (Decisiones) | semáforo verde/ámbar/rojo + "chequeo, no nota" — sin número | EX-E4-CHECKIN-DEC |
| Emociones (abandono) | "Puedes parar cuando quieras" — sin penalización visible | Regla #8 |
| Source Triage | "Aplica los criterios de ESTE ejercicio" — sin "fake news" | EX-E6-SOURCE-TRIAGE |
| Creatividad calidad | calidad semántica como CALIBRATING, no nota | EX03/EX05/EX07 |
| Memoria mejora | "Mejora confirmada" solo si `isGenuine && reliable` (RCI) | EX-TRANSFER-PROBE-MEM |
| Forecast (renombrado) | "Confianza"/"estimacion" — nunca "apuesta"/"fichas" | EX-E3-FORECAST-DEC |

## 5. Lexicon inicial del scanner (`ForbiddenLexicon.kt`)
Las 5 familias del §1 son la lista canónica para INFRA-05 (`ForbiddenLexicon.kt` + contraparte pura),
con folding de acentos español y whole-word que cruza guiones, sobre `commonMain`+tests de los 7
módulos sin gate + `AppStrings.kt`. Enganchado a `check`.

## 6. Ejemplos rojo / verde

| Copy | Veredicto | Razón |
|---|---|---|
| "Mejora tu **inteligencia** y tu **multitarea** en el **trabajo**" | ROJO | far-transfer × 3 |
| "Esto **no** es un test de **inteligencia**" | ROJO | lexema presente incluso negado (Invariante #c) → reformular a "práctica funcional, no es una nota" |
| "Detecta **fake news** y **fuentes fiables**" | ROJO | misinformación (regulatorio) |
| "Anti-**clinical** product language" | ROJO | `\b` cruza guion (`hecho` 2026-06-23) |
| "Calcula la mejor **apuesta** con tus **fichas**" | ROJO | léxico de apuesta |
| "Señal de práctica en esta tarea · estimación preliminar" | VERDE | construct honesto |
| "Aplica los criterios DADOS en este ejercicio" (Source Triage) | VERDE | task-only, sin "fake news" |
| "Progreso contra tu línea base personal · proxy interno, no diagnóstico" | VERDE | permitido |
| "Disponibilidad cognitiva de hoy — chequeo, no nota" (Check-in) | VERDE | semáforo, no score |
