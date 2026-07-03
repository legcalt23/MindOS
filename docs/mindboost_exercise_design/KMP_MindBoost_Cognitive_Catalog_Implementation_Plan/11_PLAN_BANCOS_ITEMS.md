# 11 · Plan de Bancos de Ítems (por banco: módulo, tamaño, validación, holdout/rotación)

> Fuente: `AUDIT_V2_TECNICO.md` (backlog científico — tabla de bancos),
> `AUDIT_V2_GROUND_TRUTH.md` (inventario real de bancos). Leyenda: `hecho` · `inferido` ·
> `por_confirmar`. **Invariante #e:** banco pequeño/no-validado BLOQUEA el perfil (gate INFRA-04).

---

## 1. Inventario real HOY (`hecho`, ground-truth)

| Banco | Módulo | Tamaño real | Generación | Holdout | Validación |
|---|---|---|---|---|---|
| Logic (6 ítems fijos) | Logic | 6, orden fijo | autorada fija | no | ninguna; memorizable 1-2 sesiones |
| Language (6 bancos × 6) | Language | 36 + demo 7 | autorada fija | no | ninguna; correcto="b" en E4 6/6 |
| Bayes/Reversibility | Decisiones | procedural | `generateScenario(seed)` | n/a | ninguna; no memorizable como texto |
| PAUSA_WORDS | Emociones | 24 | autorada fija | no | ninguna |
| SEPARATOR_ITEMS | Emociones | 40 (10/cat) | autorada fija | `isEval` 12/40 **reservado, nunca servido** (Ola-4) | ninguna; holdout fantasma |
| Creativity prompts | Creatividad | 14 (8+6) | estática | no | ninguna |

**Campos psicométricos globales (`hecho`):** `discriminability` solo perceptual en emotions;
`interRater`, `culturalLoad`, `exposure` = 0 resultados; `holdout`/`isEval` solo emotions inactivo.

## 2. Tabla del backlog científico (bancos a construir/validar — gating de claim)

> Reproducida del §backlog científico de `AUDIT_V2_TECNICO.md` (`hecho`). "Tamaño min" e inter-rater
> son objetivos de release; sin ellos el ejercicio NO actualiza perfil.

| Banco | Módulo | Tamaño min | Validación | Holdout/rotación |
|---|---|---|---|---|
| Objetos prohibidos (inhibición) | Creatividad EX01 | ≥40 | inter-rater respuestas prepotentes | sí |
| Frecuencia léxica usos (rareza) | Creatividad EX06 | cobertura auditada | freq_rel empírica declarada | banco oculto |
| Separador 4-cat | Emociones EM3 | inter-rater ≥80%/ítem | descarte no-acuerdo | holdout SERVIDO |
| Valencia neutra (granularidad) | Emociones EM9 | inter-rater ≥80% | disjunto de SEPARATOR | sí |
| Evidence-controlled (forecast) | Decisiones E3 | prob-por-diseño o IR≥80% | outcome verificable | sí |
| Sesgo inequívoco/ítem | Decisiones E6 | IR≥80%, 1 sesgo/ítem | excludeRecent estricto | por familia ≥8 |
| Source triage (criterios EN ítem) | Lenguaje E6 | ≥40 | atributos en el ítem, no prestigio | decoy bidireccional |
| Evidence Lock / Scope-Neg / Arg | Lenguaje | ≥40-50 | balanceo posición + operatorType | exposure/holdout |
| Caras diversas (nombres-rostros) | Memoria EX8 | licenciado | balance etnia/género/edad | sí |
| Formas paralelas A/B/C UNTRAINED | Memoria | equated | equating antes de comparar | partición reservada |

## 3. Plan de construcción por banco

### 3.1 Logic (procedural — eliminar 6-fijos)
- **Acción:** construir generador procedural (KDoc ya admite que no existe — `hecho`) o, mínimo,
  banco ≥40 con balanceo de posición correcta + exposure tracking.
- **Holdout/rotación:** partición UNTRAINED isomorfa servida; `selectNext()` sin orden fijo.
- **Claim sin esto:** `BANK_CONTAMINATED`/`CALIBRATING` permanente (D5/D7 fuerzan `WITHHOLD`).

### 3.2 Language (6 bancos → ≥40-50 c/u, balanceados)
- **Acción:** por banco (Evidence Lock, Semantic Surgery, Compression Ladder, Argument X-Ray,
  Scope & Negation, Source Triage): subir a ≥40-50; **balancear posición correcta** (mata "siempre b"
  de E4 — `hecho`); Scope & Negation con cuota ≥20% ítems sin inferencia válida; Semantic Surgery
  balanceado en 5 changeType + longitud/posición de pivote (≥50).
- **Holdout/rotación:** exposure + holdout servido por banco; HIGH_PRIOR por banco-tema (temas neutros).
- **Source Triage (E6):** atributos EN el ítem (no prestigio), decoy bidireccional, sourceCount≥4;
  **bloqueado por copy hasta INFRA-05** (riesgo "fake news" — ver doc 12).

### 3.3 Decisiones (procedural — validar el procedimiento)
- **Acción:** mantener procedural (`PROCEDURAL`, no memorizable); `seed≠0` + rotación de marcadores
  cross-sesión; excluir trials priori=posterior del BUR. Para E3 Forecast / E6 Caza-Trampas: si se
  quiere perfil, banco evidence-controlled validado (IR≥80% o prob-por-diseño, formato numérico) o
  RETIRE.

### 3.4 Emociones (activar holdout, inter-rater)
- **Acción EM3:** inter-rater ≥80%/ítem con descarte de no-acuerdo; **servir el holdout `isEval`**
  (hoy fantasma — `hecho`); detector de entropía (D6); RT-floor proporcional a longitud; objetivo ≥80
  (doc actual: "v1 ships ≥30").
- **EM9:** banco de valencia neutra validado, disjunto de SEPARATOR.

### 3.5 Creatividad (banco prohibido + frecuencias)
- **EX01:** banco prohibido ≥40 con inter-rater de respuestas prepotentes + near-duplicate/overlap +
  rotación/holdout.
- **EX06:** banco de frecuencias con cobertura auditada (punto único de fallo — `hecho`/`inferido`);
  plausibilidad prioriza banco-de-válidas; banco oculto + rotación dura.

### 3.6 Memoria (formas paralelas equiparadas + sonda UNTRAINED)
- **Acción:** formas paralelas A/B/C **equiparadas (equating) antes de comparar**; partición UNTRAINED
  reservada y **servida** (hoy `TransferProbePartition` reservado, nunca servido — `hecho`); RCI gated
  por reliability ≥2-3 administraciones. EX8 caras: banco diverso licenciado o MERGE en EX4.

## 4. Estudios de calibración (gating del `minBankSize`)
- **Curva de memorización por tamaño de banco** (`hecho` backlog): nº de sesiones hasta plateau de
  familiaridad ⇒ calibra `minBankSize` por tipo. Hasta tenerlo, `minBankSize` es `por_confirmar`.
- **Sesgo cultural/léxico** por banco textual (Lenguaje, Emociones, Decisiones) ⇒ alimenta
  `culturalLoad`.
- **Equating de formas paralelas** (Memoria) antes de cualquier RCI.

## 5. Regla de cierre
Un banco habilita `updatesProfile=true` solo si: (1) tamaño ≥ `minBankSize` del tipo; (2) holdout
SERVIDO (no fantasma); (3) rotación sin orden fijo; (4) posición correcta balanceada (chi²);
(5) inter-rater ≥0.80 en bancos de juicio; (6) exposure tracking activo. Si falta cualquiera, INFRA-04
fuerza `BANK_CONTAMINATED`/degradación y el ejercicio queda en `CALIBRATING`/minijuego.
