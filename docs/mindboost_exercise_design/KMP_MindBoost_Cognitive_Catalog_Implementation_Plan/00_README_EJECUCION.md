# 00 · README de ejecucion — Plan de implementacion del catalogo cognitivo MindBoost

> Fuentes de este plan (leidas, no inventadas): `docs/mindboost_exercise_design/AUDIT_V2_PRODUCTO.md`,
> `AUDIT_V2_TECNICO.md`, `AUDIT_V2_GROUND_TRUTH.md`. Todo dato sin respaldo en esas fuentes va marcado
> `POR_CONFIRMAR_EN_REPO`. Rutas no verificadas contra el arbol real van como `ruta probable`.
> Distincion de evidencia usada en todo el arbol: `hecho` (con evidencia archivo:linea en GROUND_TRUTH) /
> `inferido` (deduccion del auditor) / `por_confirmar` (no verificado en repo).

## 1. Que es esto y como usar el ZIP

Este arbol (`KMP_MindBoost_Cognitive_Catalog_Implementation_Plan/`) es el **plan de release** del catalogo de
~81 ejercicios de MindBoost. No es codigo: es la lista priorizada de lo que hay que construir/arreglar/retirar
para poder afirmar honestamente que un ejercicio "actualiza el perfil cognitivo".

Orden de lectura:

1. **`00_README_EJECUCION.md`** (este archivo) — como aplicar el plan, P0/P1/P2/P3, criterio GO/NO-GO, indice.
2. **`01_VEREDICTO_EJECUTIVO.md`** — estado actual, por que NO esta listo como perfil, los 3 bloqueadores
   estructurales con evidencia `archivo:linea`, release recomendado vs prohibido, decision final.
3. **`02_DECISION_PRODUCTO_CATALOGO.md`** — tabla completa ejercicio por ejercicio (los ~81) con veredicto,
   prioridad, si puede actualizar perfil, riesgo y accion exacta.
4. **`03_ROADMAP_FASES_P0_P1_P2_P3.md`** — fases con dependencias y el bloqueo duro: ningun P1 que actualice
   perfil arranca antes de los gates P0 minimos.

El detalle tecnico por issue (titulo, archivos probables, criterios de aceptacion, tests, DoD) vive en
`AUDIT_V2_TECNICO.md` (secciones 9–13). Este arbol lo referencia; no lo duplica entero.

## 2. Orden de aplicacion del plan (no negociable)

```
P0 (infra transversal)  →  P1 (v1 defendible)  →  P2 (experimental v2)  →  P3 (diferido/retirado)
        │
        └── BLOQUEO: ningun ejercicio P1 que actualice perfil empieza antes de los gates P0 minimos
            (jank-gate vivo + timebase autoritativa + anti-gaming testeado + bancos validados + copy-scanner).
```

Regla de aplicacion: se ejecutan **primero las 6 piezas de infra P0** (INFRA-1..6 en `AUDIT_V2_TECNICO.md` §9),
porque cada una desbloquea decenas de ejercicios. Una infra, no 71 parches. Solo cuando un ejercicio P1 tiene
TODAS sus dependencias P0 satisfechas se cablean sus gates.

## 3. Que significa P0 / P1 / P2 / P3

| Prioridad | Significado | Puede tocar perfil |
|---|---|---|
| **P0** | Infra transversal o bloqueador propio sin el cual NADA mide cognicion (jank-gate, timebase con signo, anti-gaming framework, validacion de bancos, copy-scanner, min-n central) y rebuilds de runtime cuyo output hoy es ruido (E8 Duo, Check-in, EM3, Source Triage, Transfer Probe, EM6). | No, hasta resolverse |
| **P1** | Los ejercicios mas defendibles del v1: accuracy-based o clasificacion, no RT-criticos para el perfil. Se cablean DESPUES de P0. | Si, solo tras sus gates |
| **P2** | Prometedores con rediseño o merges (n-back con d', updating, grid, pares, episodica; merges de Emociones; rediseños de Creatividad). | Solo-calibrando / no, hasta rediseño |
| **P3** | Experimentales caros o candidatos a RETIRE (Velocidad E3/E5/E7/E8, Decisiones E5/E6/E7, Creatividad EX08, clones). | No |

## 4. Criterio GO / NO-GO

Un ejercicio pasa a **GO (puede actualizar perfil)** solo si cumple TODOS estos invariantes. Estos invariantes
se repiten en cada documento del arbol porque son la frontera de honestidad del producto:

- **(a) `null != 0`.** Ningun estado no-`Valid` se serializa como un score bajo. `InsufficientData`,
  `Calibrating`, `Contaminated`, `UntrustedTimebase` mapean a "calibrando"/"datos insuficientes", jamas a un
  percentil. (`hecho`: la regla ya se respeta hoy via `DimensionState.kt:35 require((CALIBRATING)==(value==null))`,
  `MetricValue.unavailable()`, barras `Double?`; excepcion conocida: `ScoringEquations` usa `0.0` como sentinela).
- **(b) Enfoque y Velocidad NO actualizan perfil** hasta timebase autoritativa + jank-gate real
  (`frameDropFraction` medido, no hardcoded `0.0`). (`hecho`: hoy `SpeedSessionStateHolder.kt:259-260`
  pasa `frameDropFraction = 0.0` y `criticalFrameJitter = false`; Focus mide RT con `MonotonicClock`
  a nivel de corrutina, no de vsync).
- **(c) Cero claims de far-transfer / clinico / IQ / fake-news / regulacion-emocional**, NI siquiera para
  negarlos en copy escaneable. (`hecho`: solo `arena` y `emotions` tienen terminology-scanner en `check`;
  focus/memory/speed/logic/decisiones/language/creativity NO).
- **(d) Anti-gaming solo cuenta si implementado + testeado con perfiles sinteticos adversarios.** Diseñado no
  cuenta. (`hecho`: ~4 detectores reales y testeados —anticipacion, AGRS RT<400, SSRT waiting-strategy,
  fast-commission—; cadencia de tap real, sesgo posicional y keyword-stuffing NO existen; `spatialEntropy=1.0`
  hardcoded neutraliza el termino espacial del AGRS).
- **(e) Banco pequeño / no-validado bloquea perfil.** (`hecho`: Logic 6 fijos, Language 36 fijos,
  Emotions PAUSA(24)+SEPARATOR(40), Creativity 14; cero inter-rater, cero holdout activo —el `isEval` de
  emotions se filtra y nunca se sirve—, cero exposure tracking, cero validacion empirica).

**GO** = el ejercicio cumple (a)–(e) para su categoria de metrica. En la tabla de `02_*` se etiqueta como
**GO** (equivalente a APPROVE). **NO-GO** = falla cualquier invariante aplicable.

## 5. Indice de documentos del arbol

| Archivo | Contenido | Audiencia |
|---|---|---|
| `00_README_EJECUCION.md` | Este. Uso del ZIP, orden, P0/P1/P2/P3, GO/NO-GO. | Todos |
| `01_VEREDICTO_EJECUTIVO.md` | Estado, 3 bloqueadores con evidencia, release recomendado/prohibido, decision final. | Direccion / release manager |
| `02_DECISION_PRODUCTO_CATALOGO.md` | Tabla completa de los ~81 ejercicios: veredicto, prioridad, perfil, riesgo, accion. | Producto / ingenieria |
| `03_ROADMAP_FASES_P0_P1_P2_P3.md` | Fases, dependencias, bloqueo P0→P1. | Ingenieria / planning |

Referencias externas (no parte de este arbol, pero fuente del detalle tecnico):
`AUDIT_V2_TECNICO.md` §9 (roadmap KMP / issues), §10 (test plan adversarial), §11 (copy permitido/prohibido),
§12 (tabla de desbloqueo por ejercicio), §13 (dissent log); `AUDIT_V2_PRODUCTO.md` (veredicto, decision,
redundancias, claims, riesgos por dispositivo, catalogo v1/v2); `AUDIT_V2_GROUND_TRUTH.md` (evidencia
`archivo:linea` del estado real del repo).

## 6. Como verificar avance (sin ejecutar nada aqui)

Cada issue de `AUDIT_V2_TECNICO.md` trae sus `tests obligatorios` y, cuando aplica, su `comando/check`. Los
checks de build relevantes ya existentes en el repo (`hecho`): `arenaTerminologyScanner` y
`emotionsTerminologyScanner` enganchados a `check` en `shared/build.gradle.kts` son el **patron exacto** para el
nuevo `CopySafetyScanner` global (INFRA-5). No ejecutes scripts desde este plan: es documentacion.
