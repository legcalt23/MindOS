# MindBoost — Guardrails científicos y de claims del sistema de entrenamiento

> **Destino:** `docs/plans/mindboost_science_and_claims_guardrails.md` · v1.0 · 2026-07-02
> **Fuentes vinculantes (en orden):** memo D1–D14 + addenda A1–A18 (prevalecen), panel `science` y
> `mind-efficacy`, PRD §6.5/§27/§28, lexicon `core/copy/ForbiddenLexicon.kt`.
> **Ubicación:** este doc vive en `docs/`, FUERA del árbol que escanea `mindboostCopySafetyScanner`;
> por eso puede nombrar lexemas prohibidos como documentación. PROHIBIDO copiar sus ejemplos
> literales a fuentes bajo `features/mindboost/**`, `design/` o a copy de usuario.

## 1. Principios científicos no negociables

1. **Medir > afirmar.** Solo se comunica lo que una métrica observable con gate de muestra
   (`MinSampleGate`) y anti-gaming testeado sostiene. Sin medición elegible, el producto calla:
   "no podemos afirmarlo aún" es una respuesta válida y obligatoria.
2. **null ≠ 0.** La ausencia de señal se representa como ausencia (`normalizedScore = null`, UI "—"),
   jamás como cero ni número fabricado. Un 0–100 solo aparece bajo el gate de display A1
   (ESTABLISHED, o EMERGING con confidence ≥ 0.55 y ≥2 bloques/sesiones cross-session; por debajo,
   banda ordinal + "provisional"). CALIBRATING nunca renderiza un entero.
3. **Todo es intra-persona.** El único comparador es la línea base del propio usuario. Sin normas
   poblacionales propias → prohibidos percentiles y todo "mejor que el X%". Comparar usuarios
   exigiría normas + validación server-side (V2); hasta entonces ni se insinúa.
4. **Practice effects y regresión a la media son la explicación por defecto de una mejora.** La
   sesión 1 es familiarización (se descarta); una subida rápida inicial es práctica del test; una
   bajada tras un pico es regresión a la media. Solo los probes fijos (§6) separan mejora real de
   práctica. La memorización de bancos es practice effect: ningún banco alimenta el perfil sin
   validación, versionado y rotación/holdout (`BankValidator`).
5. **La transferencia NO se garantiza — y no se afirma.** Postura: el entrenamiento mejora las
   tareas entrenadas y, a lo sumo, produce *near transfer* a tareas muy similares; la evidencia de
   *far transfer* (vida diaria, trabajo, CI) es heterogénea y los meta-análisis más sólidos (Sala
   et al., ref. 6 del PRD) no la sustentan de forma robusta. Consecuencia: **cero claims de
   transferencia**; todo claim se limita a "dentro de MindBoost, contra tu propia línea base".
   Solo la escalera de §6 puede cambiarlo.
6. **Una señal solo existe si su medición es elegible.** RT-crítico exige timebase autoritativo
   (`CHOREOGRAPHER_FRAME + NANOS_NATIVE`); anti-gaming solo cuenta con suite red-team
   (adversario-dispara + honesto-no-dispara); un banco solo cuenta validado. Sin elegibilidad, la
   dimensión sigue calibrando y la UI explica por qué.
7. **La seguridad precede a la medición.** Pausar o salir jamás penaliza una métrica (Regla #8
   anti-gaming; patrón `OverloadDetector`).

## 2. Claims: permitidos por bloque y prohibidos transversales

Formato canónico único (A18): **"Entrenas y mides X dentro de MindBoost contra tu propia línea
base."** Cada ejercicio declara su variante "Entrena X medido por Y dentro de MindBoost".

| Bloque | X permitido (dentro de MindBoost, contra tu línea base) | Prohibición específica |
|---|---|---|
| Enfoque | Atención sostenida, control de interferencia e inhibición de respuesta en las tareas del bloque | Concentración fuera de sesión (la vigilancia es estado intra-sesión, no rasgo) |
| Memoria | Retención y manipulación de información en los ejercicios (span, recuerdo diferido, pares) | Memoria cotidiana; prevención de deterioro; subdimensiones sin señal propia |
| Velocidad | Rapidez de respuesta CON precisión, solo en dispositivos con medición elegible | Rapidez "en la vida"/al conducir; lecturas sin timebase + detector |
| Lógica | Razonamiento deductivo/inductivo sobre los ítems del motor | CI/inteligencia; razonamiento general como rasgo |
| Decisiones | Calibración de juicio y actualización con evidencia en los escenarios del bloque | "Mejores decisiones"/vida real; léxico de apuestas |
| Lenguaje | Precisión de comprensión y análisis de textos dentro de las tareas | Nivel lector; "pensamiento crítico" general; verificación de hechos del mundo real |
| Creatividad | Práctica de generación de ideas con retroalimentación de proceso (fluidez, flexibilidad, variedad) | "Eres creativo"; originalidad "medida"; nota o rango |
| Emociones | Estabilidad y consistencia de respuesta ante contenido afectivo leve, dentro de sesión | Todo lexema clínico y de regulación/inteligencia emocional (en el lexicon); lecturas de bienestar |

**Prohibidos transversales** (TODO copy, interno y externo):

- CI/IQ/inteligencia y cualquier promesa de aumentarla.
- Lecturas clínicas: TDAH, Alzheimer, demencia, ansiedad, depresión, diagnóstico, tratamiento,
  terapia, síntoma, trastorno, deterioro cognitivo, salud mental.
- "Científicamente probado"/"demostrado clínicamente" sin estudio propio del nivel exigido en §6.
- Transferencia garantizada (o insinuada) a vida real, trabajo, estudios, conducción, productividad.
- Percentiles y comparaciones poblacionales.
- Rasgos de personalidad o identidad ("eres creativo/impulsivo/inteligente").
- Neurohype ("reconfigura tu cerebro", "neuroplasticidad garantizada").
- Score/rango fabricado: nunca un entero sin gate A1; rango solo desde ESTABLISHED (V1.1+ por A3).

## 3. Reglas de copy operativas

- **Lexicon canónico:** `core/copy/ForbiddenLexicon.kt`, 5 familias — FAR_TRANSFER, CLINICAL,
  GAMBLING, MISINFORMATION, PSEUDOSCIENCE. Vive fuera del árbol escaneado; la task Gradle mantiene
  copia de paridad. Única fuente de verdad del vocabulario prohibido.
- **Scanner de build:** `mindboostCopySafetyScanner` cubre `commonMain`; las variantes
  `…ScannerCommonTest` y `…ScannerDesktopTest`, sus source-sets — al tocar un archivo escaneado se
  corre LA VARIANTE correspondiente, no solo la base. Escanea archivos COMPLETOS (strings,
  comentarios, KDoc) y PARA en el primer hit → re-correr hasta verde.
- **Anti-negación:** match whole-word con folding de acentos que cruza guiones y negaciones.
  Prohibido nombrar un lexema incluso para negarlo ("no es un test de inteligencia" FALLA).
  Reformular en positivo: "no es una nota", "proxy funcional", "orientativo", "práctica",
  "progreso contra tu línea base".
- **Polisemia como frases:** los ambiguos se codifican como frases, nunca palabra suelta
  ("deterioro cognitivo", no "cognitivo"; "rindes mejor en el trabajo", no "trabajo").
- **Copy externo pasa el mismo gate:** store, marketing, web, notificaciones, capturas y prensa se
  revisan contra el MISMO lexicon antes de publicar (A18) vía checklist manual firmada en el
  registro de §7. Ningún asset externo se publica sin esa pasada.
- **Precedente — FTC vs. Lumosity (2016):** sanción por publicidad engañosa (50 M USD, suspendida
  a 2 M) por afirmar sin evidencia mejoras en trabajo/estudios y retraso del declive asociado a
  edad y salud. Exactamente la familia de claims aquí prohibida: el riesgo no es teórico.
- **Encuadre EU AI Act / MDR (mínimo):** el MVP no incorpora inferencia de IA (planner
  determinista) → exposición mínima. Guardrails: (a) nunca posicionar el producto con finalidad
  médica/diagnóstica (activaría MDR); (b) la retención evita técnicas manipulativas (art. 5);
  (c) si el IA Coach entra (V1.x), revisar transparencia (art. 50) antes de lanzar. No sustituye
  revisión legal formal pre-lanzamiento.

## 4. Guardrails por bloque sensible

### Emociones
- NO clínico, NO terapia: inducción leve-only, sin reappraisal guiado, sin caras reales, sin
  mood-tracking. El safety-exit JAMÁS penaliza; `OverloadDetector` actúa antes que la adaptación.
- `normalizedScore = null` hasta holdout/baseline (Ola 4). PROHIBIDO derivar 0–100 de tier o
  `dataSufficiency` (madurez de datos ≠ competencia): la dimensión muestra "—" con explicación.
- La supresión no es el constructo: "no responder / no sentir" nunca se premia.

### Creatividad
- Solo métricas de proceso: fluidez, flexibilidad, variedad; "originalidad aproximada" solo si el
  método la respalda (listas de respuestas comunes por prompt).
- Sin nota 0–100, sin rango, sin `CreativityProfileSource` en MVP (A7): CREATIVITY = "—" honesto.
  Copy del hub: "retroalimentación funcional, sin nota ni rango". Nunca juicios de rasgo.
- La fluidez cruda es gameable con ideas triviales: validez del conteo antes de subir su peso.

### Velocidad
- **Elegibilidad por dispositivo:** solo timebase autoritativo alimenta el perfil (Android API≥34,
  Choreographer + nanos nativos); el fallback es jugable pero NO medible. La UI expone la
  elegibilidad en vez de fallar en silencio.
- **Jamás velocidad sin precisión:** gate precisión ≥ 0.80 intocable.
- PARTIAL → ADEQUATE solo con detector de fast-guessing calibrado (suelo fisiológico de RT por
  paradigma, banda de precisión-azar, CAF, guard de muestra) validado adversarialmente; mientras,
  `contribution() = null` y la UI explica por qué sigue calibrando.

### Decisiones
- **El banco excluye dominios sensibles:** nada de dinero real/finanzas, legal, salud/enfermedad ni
  autolesión (stems excluidos en `ReversibilityScenarioBank`, verificados por test). Escenarios
  cotidianos de bajo riesgo emocional. Cero léxico de apuestas.
- Se puntúa calibración/actualización (Brier), no "acertar". Reversibility solo alimenta el perfil
  tras validación inter-rater (≥2 raters, κ ≥ 0.75); hasta entonces `SMALL_UNVALIDATED` bloquea.

### Evaluación inicial
- **Es siembra, no medición.** Con ~50 s/bloque la fiabilidad es casi nula: su único output es
  dificultad inicial (3 bins) + bandas ordinales provisionales con confidence mínima (A17).
- NUNCA produce scores 0–100, EMERGING, rango, SSRT, d′ ni difference-scores; no cuenta como
  sesión de calibración.
- Copy obligado: "estimación inicial para ajustar tu plan" + "te faltan N sesiones para tu primera
  lectura" — nunca se presenta como perfil medido.

## 5. Requisitos de evidencia por ejercicio (plantilla obligatoria, PRD §28.3)

Todo ejercicio nuevo — y todo cambio de scoring o claim — presenta ANTES de entrar al catálogo:

1. **Hipótesis cognitiva:** qué proceso ejercita y por qué la mecánica lo demanda (verificado
   contra el flujo de captura real, no el asumido).
2. **Métrica observable:** definición operativa + nMin (`MinSampleGate`) + fiabilidad A2
   (RT-mediana n≥20 split-half ≥0.7; accuracy n≥20 IC Wilson; span ≥2 administraciones;
   difference-scores ≥20/celda y solo secundarios; SSRT ≥30 stops; Brier ≥40; d′ ≥20+20).
3. **Riesgo de medición:** practice effect, memorización de banco, sesgo de respuesta, techo/suelo,
   dependencia de timebase, contaminación por exposición previa.
4. **Señales de gaming + detector/gate que las neutraliza** (`core/antigaming` D1–D11). Reclamar
   ADEQUATE exige suite red-team: cada adversario dispara y un honesto realista no dispara ninguno.
5. **Criterio de éxito:** qué resultado valida que mide/entrena lo declarado.
6. **Nivel de confianza** (alto/medio/bajo) **+ fuente si aplica:** paradigma canónico citable o
   autoría propia paramétrica. PROHIBIDOS ítems publicados (Raven, RAT, TTCT, CRT): copyright +
   contaminación por familiaridad.
7. **Claim permitido del ejercicio** en el formato canónico de §2.

Bancos: tamaño mínimo declarado, no-repetición, `bankVersion` y registro en `BankValidationGateTest`
si alimentan perfil. Sin plantilla completa, el ejercicio no entra ni al plan de diseño.

## 6. Roadmap de validación propia (escalera de claims)

**Cómo se mide mejora honestamente:**
- Probes FIJOS no adaptativos por bloque (patrón `BaselineProtocols`): la mejora se mide
  probe-a-probe, NUNCA sobre scores de entrenamiento adaptativo.
- RCI (Reliable Change Index) a 7 / 30 / 90 días contra la línea base; la sesión 1 se descarta.
- Telemetría honesta: `mb_block_improvement_30d` bucketed on-device (±0.02/±0.05/±0.10, flag
  `validated`), consent-first y PII-free (A6).

**Escalera para subir un claim** (no se salta peldaños; bajar es inmediato si la evidencia se debilita):

| Nivel | Claim habilitado | Evidencia requerida |
|---|---|---|
| 0 (hoy) | "Entrenas y mides X dentro de MindBoost contra tu línea base" | Métrica + gates de §5 en verde |
| 1 | "Quienes completan N sesiones mejoran su métrica de probe X dentro de MindBoost" | RCI positivo en probes fijos con n suficiente, test-retest ≥0.7, mejora que persiste tras descontar familiarización |
| 2 | Mejora sostenida a 90 días dentro de la app | Estudio interno longitudinal pre-registrado (cohortes 7/30/90, deserción analizada) |
| 3 | Cualquier claim de transferencia (incluso near-transfer fuera de la app) | Estudio controlado propio (idealmente aleatorizado con grupo activo) + revisión externa; hasta entonces PROHIBIDO |

Ningún nivel habilita jamás los prohibidos transversales de §2 (clínico, CI, percentiles, rasgos).

## 7. Proceso de revisión y registro versionado

**Aprobación de un claim nuevo o cambio de uno existente:**
1. El autor presenta la plantilla §5 completa con el claim exacto propuesto.
2. **Gate de eficacia** (rol efficacy-guardian): verifica principios §1, umbral de display A1, que
   no se fabrica señal y que el nivel de escalera (§6) corresponde a la evidencia.
3. **Gate QA/claims** (skills `review-cognitive-claims` + `qa-release-gate`): copy contra el
   lexicon (scanner en código; checklist manual en externo), consistencia UI↔claim y veredicto
   approve/revise/block.
4. Las contradicciones con el PRD se registran y reconcilian, nunca se ignoran.

**Registro versionado:** `docs/claims/CLAIMS_REGISTRY.md` (se crea con el primer claim aprobado).
Cada entrada: claim literal, bloque/ejercicio, nivel de escalera, evidencia, aprobadores, fecha y
`scoringVersion`. Los cambios de fórmula de scoring se registran con razón y fecha (PRD §28.2·7):
un score histórico nunca cambia retroactivamente sin traza.

**Cadencia y emergencia:** lexicon + este documento se revisan en cada corte de release; un fallo
del scanner en CI bloquea el merge. Si un claim publicado resulta insostenible, se retira primero
y se discute después.
