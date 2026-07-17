# MindBoost — Contexto de marketing canónico

> **Fuente única de verdad.** Toda tarea de marketing (ficha, contenido, comunidad, retención, PR, pricing futuro)
> lee este archivo primero. Si algo aquí entra en conflicto con un activo, gana este archivo.

## 1. Producto

MindBoost es un **entrenador cognitivo funcional** dentro de Mindverse (Kotlin Multiplatform; Android primero,
iOS diferido sin fecha). No es un juego casual: es un sistema **medible** de progreso cognitivo.

**Loop nuclear:** baseline → plan diario → sesión de entrenamiento adaptativo → feedback inmediato → **progreso
frente a tu propia línea base** → insight longitudinal → hábito.

**Mecánica diferencial (munición de marketing):** cuando no hay datos suficientes, MindBoost muestra
**CALIBRANDO / «—», nunca un cero ni una puntuación fabricada** (`null ≠ 0`). Mide tu progreso contra ti mismo
en el tiempo, no contra una población. Datos de práctica cifrados en local; recolección mínima (privacy-first).

## 2. Cuña (wedge)

La categoría "brain-training" está desacreditada por el hype (caso FTC-Lumosity 2016; consenso escéptico
Simons 2016 / Melby-Lervåg / Owen 2010). **MindBoost se posiciona EN CONTRA:** honesto, medible frente a tu
propio baseline, privacy-first, anti-humo. Toda técnica de marketing debe REFORZAR esta cuña, no diluirla.

## 3. Beachhead / ICP

**Opositores y estudiantes en España.** Cultura de oposiciones masiva y con comunidades densas: administrativo,
sanidad, justicia, docentes, cuerpos de seguridad, Correos. Motivación: constancia sostenida bajo preparación
larga; hábito de estudio diario. Encaja de forma nativa con "progreso medible frente a tu propia línea base".

### Personas seed (rellenar con VOC real — ver teardown §A3)
- **Opositor de larga preparación:** JTBD = "sostener una rutina de foco constante durante meses". Gatillo =
  meseta / bajón de constancia. Deseo = ver que avanza aunque el examen esté lejos. Vocabulario a robar de
  r/opositores, foros de academias, Telegram.
- **Estudiante universitario en época de exámenes:** JTBD = "concentrarme en bloques de estudio sin dispersarme".

## 4. Frontera de claims (LÍMITE DURO)

**Fuente de verdad:** `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt`
(familias: `FAR_TRANSFER`, `CLINICAL`, `GAMBLING`, `MISINFORMATION`, `PSEUDOSCIENCE`). Ver
[`CLAIMS_SAFETY.md`](CLAIMS_SAFETY.md) para el checklist operativo.

- **PROHIBIDO** (por FAMILIA, sin nombrar lexemas): promesas de capacidad general, claims de la familia
  `CLINICAL` (salud/condiciones médicas), transferencia lejana no respaldada, léxico de azar, promesas
  pseudocientíficas sobre el órgano cognitivo. **Ni siquiera para negarlos.**
- **PERMITIDO:** "perfil cognitivo funcional", "práctica", "entrenamiento", "progreso frente a tu propia línea
  base", "dificultad adaptativa", "orientativo", "entrena/practica tu concentración", "rutina de foco medible".
- **Matiz clave (captura la intención, no prometas el resultado):** las búsquedas de alto volumen se capturan
  tejiéndolas en la copy, pero el copy describe la **práctica** ("entrena/practica tu concentración") y el
  **progreso-vs-tu-línea-base**, nunca el resultado de capacidad.

## 5. Mercado / tienda / idioma

España · Google Play (Android primero; iOS diferido) · **español de España**.

## 6. Presupuesto / equipo / roadmap

€0 orgánico-bootstrap los próximos 3-6 meses (ASO, contenido, comunidad, founder-led, PR, listados). Fundador
en solitario / equipo mínimo. iOS y billing diferidos sin fecha.

## 7. Monetización (hipótesis — billing NO cableado)

Freemium + suscripción. Todo lo de pricing/paywall/trials es **"preparar ahora, activar cuando exista billing"**.
La métrica de valor honesta candidata: el **histórico de progreso frente a tu línea base** (lo que se acumula
con la constancia). Ver PREPARAR-DESPUÉS en [`PLAYBOOK.md`](PLAYBOOK.md).

## 8. North Star y jerarquía de mensaje

- **North Star candidato:** usuarios activos semanales que ven una **lectura de progreso frente a su línea base**.
- **Métrica de activación (aha):** el usuario **calibra su línea base y ve su primera lectura de progreso**.
- **Jerarquía de mensaje (de mayor a menor):**
  1. *Medición honesta* — "cuando no hay datos, ves CALIBRANDO, nunca un número inventado".
  2. *Progreso frente a tu propia línea base* — "te comparas contigo mismo, no con nadie".
  3. *Privacidad primero* — "cifrado en tu dispositivo, recogemos lo mínimo".
  4. *Para opositar/estudiar* — "una rutina de foco medible que acompaña tu preparación".
