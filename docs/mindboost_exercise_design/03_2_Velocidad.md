# Velocidad

# MindBoost Velocidad — Catálogo de 8 ejercicios de calidad suprema

> Fronteras de seguridad vinculantes: perfil cognitivo funcional, práctica contra baseline personal, dificultad adaptativa. Nada de diagnóstico/IQ. Toda barra sin soporte estadístico = `null` (em-dash), nunca 0. `CognitiveProfileScore.preliminary=true` mientras el gate físico §3/§15 siga abierto (timebase autoritativo solo Android API34+). Todos reusan `XorShiftSpeedRng` determinista, `SpeedMeasurementStore` (corrida = unidad atómica), `MetricCalculator`/`FatigueIndex`/`StaircaseEngine`/`SpeedScoreEngine`.

---

## 1. Discriminación Cromática Binaria (Tinte Crítico)

### Módulo
Principal: Velocidad (procesamiento perceptivo-decisional). Secundarios: Enfoque (control inhibitorio de respuesta), Profile (dimensión PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad de decisión perceptiva pura en 2-AFC: minimizar el tiempo de transducción estímulo→categoría→respuesta motora cuando la evidencia perceptiva es nítida, y medir el coste de degradar esa evidencia (separación perceptiva pequeña). Aísla velocidad de codificación de bajo nivel, casi libre de carga semántica.

### Hipótesis de transferencia
SÍ podría transferir (near): otras tareas 2-AFC perceptivas y de tiempo de reacción de elección simple; reducción de RT-mediano en tareas con decisión binaria nítida. NO se debe afirmar: mejora de "velocidad mental general", lectura, conducción, ni transferencia far a tareas semánticas/atencionales sostenidas. No es agudeza visual clínica.

### Loop principal
1. Estímulo: un parche central con un tinte (p.ej. azul-verdoso) cuya separación respecto al eje de decisión es `deltaPerceptual`; etiquetas LEFT="más frío" / RIGHT="más cálido" fijas toda la sesión.
2. Decisión: tap en mitad izquierda/derecha de pantalla (paradigma choice) dentro de `deadlineNanos`.
3. Feedback: borde verde (HIT) / rojo (error de lado) / ámbar (sin respuesta) <150 ms; sin texto.
4. Adaptación: `MultiSignalStaircaseEngine.evaluate(BlockMetrics)` ajusta CCE→`deadline` y `deltaPerceptual`.
5. Siguiente trial: ISI jitter 400–900 ms para romper ritmo predecible.

### Reglas
Responder al lado correcto según el tinte. No hay no-go. Anticipación (rt<antThreshold) se descarta del cómputo de tendencia central pero se cuenta como evento. Sesión = 4–6 bloques de 24 trials.

### Inputs
Tap (mitad izquierda/derecha = `SpeedChoice.LEFT/RIGHT`). Sin teclado/voz/texto.

### Variables de dificultad
1. `deltaPerceptual` (separación de tinte respecto al borde de decisión): 40→4 unidades CIELAB ΔE.
2. `deadlineNanos`: 1200→450 ms (gobernado por CCE).
3. `parchSizeDp`: 160→64 dp (estímulo más pequeño = codificación más difícil).
4. `isiJitterMs`: 200→700 ms de rango (mayor jitter = menos preparación).
5. `distractorRingPresent`: 0/1 (anillo cromático periférico no-objetivo).
6. `luminanceNoise`: 0.0→0.25 (ruido de luminancia que obliga a usar solo el canal de tono).
7. `axisRotationDeg`: rotación del eje de decisión 0→30° (reduce aprendizaje de mapeo fijo).

### Algoritmo adaptativo
`MultiSignalStaircaseEngine` (1-up/3-down sobre pc, objetivo ≈0.79): COOLDOWN si `fatIdx>cap`; LOWER si `pc<banda` o `impIdx>cap` o `anticipationRate` alto; STABILIZE si `pc` alto pero `cv>cap` (sube ISI jitter, no baja deadline); RAISE recorta `deadline` y reduce `deltaPerceptual`; HOLD con histéresis. VCE NO autoriza avance. Baseline personal (`BaselineSnapshotDto`) ancla `deltaPerceptual` y deadline iniciales.

### Métricas primarias
pc (accuracy de lado), `rtMedian` sobre HIT-RT MAD-limpio, `vce` (velocidad-control), CCE alcanzado.

### Métricas secundarias
`cv` (RT-CV), `anticipationRate`, `impIdx` (`impulsivityIndexNoNoGo`), pendiente RT vs `deltaPerceptual` (función psicométrica de coste perceptivo), `fatIdx` longitudinal.

### Señales de fatiga o mala calidad
`FatigueIndex.compute` (≥3 bloques): subida de RT-mediano + subida de lapsos + caída de VCE. `anticipationRate` alto sostenido = adivinación/rítmica. `cv` creciente intra-sesión = desenganche. `DeviceTimingRisk` alto o timebase no-Android → `SessionQuality.LOW/CONTAMINATED`.

### Scoring sugerido
`usefulSpeed = clamp(VCE / VCE_ref_baseline)`; `control = (1 - impIdx) * pc`; `stability = (1 - cv) * (1 - lapseRate)`. `total = mean(usefulSpeed, control, stability)`; techo: si `pc<0.80` o `cv>0.30` → `total *= 0.5` (`ceilingPenaltyApplied=true`). `preliminary=true`. Barra `null` si `n<MIN_N` o no `confident`.

### Antitrampa / calidad de datos
Eje de decisión rotado (`axisRotationDeg`) y jitter de ISI rompen estrategias de ritmo. `impIdx` y `anticipationRate` detectan tap-spamming. Determinismo seeded permite re-simular la corrida. Solo timebase Android autoritativo cuenta para score no-preliminar.

### Variantes
1. Eje luminancia (claro/oscuro) en vez de tono → daltonismo-safe.
2. Eje saturación (apagado/vívido).
3. Triple-difícil con `luminanceNoise` máximo forzando uso exclusivo del canal de tono.

### UX
Pantalla: parche central grande sobre fondo neutro `AppColors`; dos zonas táctiles a pantalla completa (`MinTouchTarget` cumplido por construcción). Feedback de borde por token semántico <150 ms; el rojo de error añade forma (no solo hue) para daltonismo. Microanimación funcional: pulso de aparición del parche (onset claro), sin decoración. Duración ideal 3–4 min.

### Implementación KMP
Reusa renderer **choice** (mitades táctiles `SpeedChoice`). Generador nuevo `TinteGenerator : SpeedPromptGenerator` (deriva `correctChoice` del tinte vs eje; nunca lo expone), seeded con `XorShiftSpeedRng`. `FixedTrialEngine`→futuro adaptativo; `MetricCalculator.blockMetrics/sessionMetrics`; `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Persistencia: `MeasurementRun` en `SecureSpeedMeasurementStore`; DTO `@Serializable`+`schemaVersion`; registry de prompts. Modelo: `data class TintePrompt(val correctChoice: SpeedChoice, val deltaPerceptual: Float, val axisRotationDeg: Float)`. Eventos: stimulus onset uptime, response uptime, isCorrect, deadlineNanos, difficultyEstimate. Estado mínimo: `SpeedTrialPrompt` + deadline + RNG. Sin renderer nuevo.

### Riesgos
Dependencia de calibración de pantalla/daltonismo; ΔE percibido varía por dispositivo. Mitigación: variante luminancia, no depender solo de hue, `parchSize` y ruido como ejes alternativos.

### Revisión crítica
El Revisor exige: (1) eliminar el riesgo de que `deltaPerceptual` sea no comparable cross-device → anclar SIEMPRE al baseline personal del propio dispositivo (`DeviceProfile`), nunca cross-device, y marcar `preliminary` mientras el gate físico siga abierto; (2) demostrar que no es solo aprendizaje del mapeo fijo LEFT/RIGHT → exigir `axisRotationDeg` activo en ≥30% de trials; (3) probar que `anticipationRate` filtra realmente al adivinador (test con perfil sintético `SyntheticUserRunner` de tap aleatorio rápido debe puntuar bajo). Sin esas tres correcciones, mide familiaridad y reflejo, no velocidad perceptiva.

### Veredicto
Aprobado con cambios: añadir rotación de eje obligatoria, anclaje estricto al baseline por dispositivo y variante luminancia daltonismo-safe.

---

## 2. Sustitución Símbolo-Dígito Rápida (Cifrado Veloz)

### Módulo
Principal: Velocidad (rediseño premium del clásico DSST). Secundarios: Enfoque (mantenimiento de regla en memoria de trabajo), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad de procesamiento con una regla de mapeo símbolo→tecla mantenida activa: combina lectura del estímulo, consulta de la clave y respuesta motora. Mide throughput perceptivo-asociativo, no memorización (la clave es siempre visible y rota).

### Hipótesis de transferencia
SÍ (near): otras tareas de sustitución/codificación y throughput perceptivo-motor; mejora de RT-mediano en mapeos arbitrarios. NO afirmar: que el DSST "predice inteligencia" o salud cerebral; no es marcador clínico aunque el DSST se use en investigación.

### Loop principal
1. Estímulo: símbolo central + leyenda de clave (símbolo↔LEFT/RIGHT) visible arriba; la clave se REGENERA cada bloque para impedir memorizar.
2. Decisión: tap del lado que la clave asigna al símbolo, antes del deadline.
3. Feedback: borde verde/rojo <120 ms; sin texto.
4. Adaptación: `DefaultStaircaseEngine`/`MultiSignalStaircaseEngine` ajusta deadline y `keySize`.
5. Siguiente: ISI corto 250–500 ms (loop rápido tipo DSST).

### Reglas
La respuesta correcta se deriva consultando la leyenda visible; nunca se muestra como affordance (render honesto). Regla cambia por bloque. 6 bloques de 20.

### Inputs
Tap (mitades LEFT/RIGHT). Variante teclado opcional (2 teclas) en desktop.

### Variables de dificultad
1. `keySize` (nº de pares símbolo↔lado distintos en la leyenda): 2→8.
2. `deadlineNanos`: 1500→500 ms.
3. `symbolSimilarity` (parecido visual entre símbolos): baja→alta.
4. `legendStablePerBlock` (1) vs `legendShufflePerTrial` (0): refuerza derivación, no memoria.
5. `legendVisibleMs` antes de ocultar parcialmente: ∞→breve (introduce fricción deseable de recuperación).
6. `isiMs`: 500→250 ms.
7. `distractorSymbols`: 0→3 símbolos no-mapeados (false-target).

### Algoritmo adaptativo
1-up/3-down sobre pc. LOWER si `pc<banda`, `impIdx>cap` o `anticipation` alta. STABILIZE (sube ISI / baja `keySize`) si `cv>cap` con pc alto. RAISE recorta deadline y sube `keySize`. COOLDOWN por `fatIdx`. SWITCH/asimetría: subir `keySize` cuesta más que bajar. Baseline ancla `keySize` inicial.

### Métricas primarias
Throughput = trials correctos por minuto; `rtMedian` HIT MAD-limpio; pc.

### Métricas secundarias
`cv`, pendiente RT vs `keySize` (coste de carga de mapeo), `lapseRate`, `omissionRate`, `impIdx`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Caída de throughput intra-sesión + subida RT-mediano. `omissionRate` creciente = desenganche. `impIdx` alto con leyenda grande = adivinación. Timebase no autoritativo → preliminar.

### Scoring sugerido
`usefulSpeed=VCE/VCE_ref`; `control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; throughput escalado entra como modificador de `usefulSpeed`. Techo `pc<0.80`||`cv>0.30`→×0.5. `preliminary=true`.

### Antitrampa / calidad de datos
Regenerar leyenda por bloque mata la memorización; `legendShufflePerTrial` para casos de sospecha. `distractorSymbols` detecta respuesta-por-posición. Determinismo seeded reproducible.

### Variantes
1. Símbolo→dígito (mapeo a 1 de 2 dígitos) clásico.
2. Color→forma.
3. "Inversa": dado el lado, ¿qué símbolo? (recuperación inversa, fricción deseable).

### UX
Leyenda compacta arriba con tokens `AppSpacing`; símbolo grande centrado; dos zonas táctiles. Feedback de borde <120 ms. Microanimación: highlight del par de la leyenda al acertar (funcional, refuerza mapeo). Duración 3–5 min.

### Implementación KMP
Reusa renderer **choice** + un slot de leyenda (componente nuevo pequeño, no nuevo paradigma). Generador `CifradoGenerator` YA existe (E3) → extender con `keySize`/leyenda rotada. `XorShiftSpeedRng`; `MetricCalculator`; `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Persistencia `MeasurementRun`. Modelo: `data class CifradoPrompt(val symbolId:Int, val legend:Map<Int,SpeedChoice>, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, keySize, deadline. Estado: prompt+legend+deadline. Renderer existente + slot leyenda.

### Riesgos
Con `keySize` grande y leyenda visible, puede degenerar en búsqueda visual lenta (mide search, no speed). Mitigación: tope de `keySize`, medir pendiente RT vs keySize para separar componentes.

### Revisión crítica
El Revisor ataca: con leyenda siempre visible, a `keySize`≤4 esto se convierte en búsqueda+lectura, no en "velocidad" — el RT estará dominado por sacadas oculares. Exige: (1) reportar la pendiente RT/keySize como métrica primaria explícita para que el score no confunda search con speed; (2) capar `keySize` a 6 en runtime adaptativo y reservar 8 a variante de carga; (3) introducir `legendVisibleMs` para forzar codificación, no relectura infinita. Sin esto, duplica E7 (búsqueda) bajo otro nombre.

### Veredicto
Aprobado con cambios: elevar pendiente RT/keySize a métrica primaria, capar keySize en runtime y añadir ventana de leyenda para diferenciarlo de búsqueda.

---

## 3. Conmutación Perceptiva de Tarea (Multivía Veloz)

### Módulo
Principal: Velocidad (flexibilidad de set perceptivo bajo presión temporal). Secundarios: Enfoque (switch cost), Profile (PROCESSING_SPEED + control).

### Objetivo cognitivo
Velocidad de reconfiguración de regla: el mismo estímulo bidimensional (color+forma) se juzga por una dimensión u otra según un cue, midiendo el coste de cambiar de regla (switch cost) a alta velocidad. Distingue throughput estable de throughput-bajo-cambio.

### Hipótesis de transferencia
SÍ (near): otras tareas de switching perceptivo; reducción de switch-cost en mapeos análogos. NO afirmar: flexibilidad cognitiva "general", multitarea de la vida real, ni función ejecutiva clínica.

### Loop principal
1. Estímulo: figura con color (rojo/azul) y forma (círculo/cuadrado) + cue de regla persistente (icono "COLOR" o "FORMA").
2. Decisión: aplicar la regla activa → tap LEFT/RIGHT.
3. Feedback: borde <130 ms.
4. Adaptación: deadline + proporción de switch ajustados.
5. Siguiente: ISI 350–700 ms; secuencia de cues seeded (mezcla switch/repeat).

### Reglas
RULE_COLOR: rojo→izq, azul→der. RULE_SHAPE: círculo→izq, cuadrado→der. La regla se deriva del cue; nunca se muestra la respuesta. 6 bloques de 24.

### Inputs
Tap LEFT/RIGHT.

### Variables de dificultad
1. `deadlineNanos`: 1400→500 ms.
2. `switchRate` (proporción de trials switch): 0.2→0.6.
3. `cueStimulusIntervalMs` (CSI, tiempo cue→estímulo): 600→0 ms (menos preparación = más coste).
4. `dimensionCongruence`: proporción de estímulos incongruentes (la otra dimensión empuja al lado opuesto): 0→0.5.
5. `cueSalience`: cue grande/persistente → pequeño/breve.
6. `isiJitterMs`: 150→550 ms.
7. `ruleCount`: 2→3 dimensiones (añadir tamaño).

### Algoritmo adaptativo
1-up/3-down sobre pc global. COOLDOWN por `fatIdx`. LOWER si pc bajo / `impIdx` alto. STABILIZE si `cv>cap`. RAISE recorta deadline y/o sube `switchRate`/baja CSI. **SWITCH_COST solo válido con HIT-RT en ambos lados (switch y repeat)**; si falta un lado → `null`, y el motor no usa un switch-cost no fiable para decidir.

### Métricas primarias
`SWITCH_COST` = `rtMedian_switch − rtMedian_repeat` (solo HITs); pc; `rtMedian` global.

### Métricas secundarias
`cv`, efecto de congruencia (incongruente−congruente), `impIdx`, `lapseRate`, pendiente switch-cost vs CSI, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Switch-cost que explota intra-sesión + RT global creciente = fatiga ejecutiva. n insuficiente en celda switch/repeat → switch-cost `null`. `impIdx` alto = respuesta sin leer cue.

### Scoring sugerido
`control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; `usefulSpeed=VCE/VCE_ref` penalizado por switch-cost normalizado: `usefulSpeed*=(1 - clamp(switchCost/switchCost_ref)*w)`. Techo estándar. `preliminary=true`. Switch-cost `null` no penaliza (no fabrica).

### Antitrampa / calidad de datos
Cue derivado (no respuesta visible); incongruencia detecta respuesta por dimensión fija; `impIdx`/`anticipationRate` filtran tap rítmico. Secuencia seeded → balance switch/repeat reproducible y auditable.

### Variantes
1. Cue auditivo-visual (tono asociado a regla) para reducir lectura.
2. Tres reglas (color/forma/tamaño) = mayor reconfiguración.
3. "Voluntario": el usuario elige regla algunos trials (mide coste de selección endógena).

### UX
Cue persistente arriba con icono+texto (doble canal), estímulo central, mitades táctiles. Feedback borde <130 ms. Microanimación: el cue parpadea suave SOLO en trial switch (señal funcional). Duración 4–5 min.

### Implementación KMP
**Requiere lógica de cue/regla** análoga al `FocusFieldState.RuleSwitch` de Enfoque (E6) portada al runtime de Velocidad → renderer choice + slot de cue (componente nuevo ligero). Generador `MultiviaGenerator` YA existe (E6) → enriquecer payload con `colorIsRed/shapeIsCircle/activeRule` y derivar `correctChoice`. `XorShiftSpeedRng`; `MetricCalculator` (necesita exponer switch-cost o computarlo en capa de speed); `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Modelo: `data class MultiviaPrompt(val rule:Rule, val colorIsRed:Boolean, val shapeIsCircle:Boolean, val isSwitch:Boolean, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, isSwitch, ruleId, deadline. Estado: prompt+activeRule+deadline. Nota: `MetricCalculator` actual no computa SWITCH_COST → añadir cálculo en speed reusando el patrón de Enfoque (HIT-RT ambos lados).

### Riesgos
Switch-cost necesita muchos trials por celda para ser `confident`; sesiones cortas pueden dejarlo siempre `null`. Riesgo de medir lectura de cue, no reconfiguración, si CSI es largo. Mitigación: capar CSI, exigir n mínimo por celda.

### Revisión crítica
El Revisor exige: (1) garantizar n suficiente por celda switch/repeat antes de reportar SWITCH_COST — si la sesión es de 4 bloques×24 con 0.4 switch, hay ~38 switch HITs: justo en el límite; documentar `MIN_N` por celda y reportar `null` honesto cuando no llega; (2) no penalizar el score por un switch-cost `null` (ya cumplido); (3) probar que el efecto de congruencia no contamina el switch-cost → reportarlos separados. Además: `MetricCalculator` no tiene switch-cost hoy → es deuda real, no "reuso directo". Que el spec lo declare explícitamente.

### Veredicto
Aprobado con cambios: declarar el cómputo de SWITCH_COST como trabajo nuevo en speed, fijar MIN_N por celda y separar congruencia de switch-cost.

---

## 4. Compuerta Lógica Express (Go/No-Go Veloz Asimétrico)

### Módulo
Principal: Velocidad (velocidad de respuesta con freno selectivo). Secundarios: Enfoque (inhibición de prepotencia), Profile (control inhibitorio + speed).

### Objetivo cognitivo
Velocidad de go-response con coste de control: responder rápido a estímulos "go" frecuentes mientras se inhibe en "no-go" raros. Mide el equilibrio velocidad/impulsividad (no solo RT), capturando la curva velocidad-control.

### Hipótesis de transferencia
SÍ (near): otras tareas go/no-go y de RT con inhibición prepotente; mejora de control sin perder velocidad. NO afirmar: control de impulsos en la vida real, TDAH (PROHIBIDO diagnóstico), ni autorregulación clínica.

### Loop principal
1. Estímulo: símbolo "compuerta" verde (GO, ~80%) o rojo/forma distinta (NO-GO, ~20%).
2. Decisión: tap rápido si GO; retener si NO-GO.
3. Feedback: borde verde (GO acierto) / rojo (comisión NO-GO) / neutro (omisión GO) <130 ms.
4. Adaptación: deadline + `noGoRate` + similitud go/no-go.
5. Siguiente: ISI corto 300–600 ms para construir prepotencia.

### Reglas
GO→tap; NO-GO→withhold. Comisión = error de control; omisión = lapso/inatención (separadas). 6 bloques de 30 (más trials por la rareza del no-go). Renderer single-tap.

### Inputs
Tap (PRIMARY) sobre toda la pantalla; retención = no-input dentro de ventana.

### Variables de dificultad
1. `deadlineNanos` (ventana GO): 1200→450 ms.
2. `noGoRate`: 0.30→0.10 (más raro = más prepotente = más difícil inhibir).
3. `goNoGoSimilarity`: baja→alta (discriminación perceptiva del freno).
4. `isiMs`: 600→300 ms.
5. `goStreakBias`: rachas GO largas antes de NO-GO (aumenta prepotencia).
6. `stimulusDurationMs`: 600→200 ms (menos tiempo de discriminación).
7. `lureRate`: NO-GO perceptivamente parecido a GO (false-go).

### Algoritmo adaptativo
Tabla MultiSignal: COOLDOWN(`fatIdx`)>LOWER(`impIdx>cap` ← comisiones, pc bajo, `omissionRate` alta)>STABILIZE(`cv>cap`)>RAISE>HOLD. `impIdx` (`impulsivityIndexNoNoGo` NO aplica aquí; usar tasa de comisión real go/no-go) gobierna: si comisión alta, el motor baja `noGoRate`/sube similitud-no, NUNCA acelera. Asimetría: bajar (seguridad) > subir.

### Métricas primarias
`rtMedian` de GO-HIT MAD-limpio; pc; `faRate` (comisión NO-GO) — aquí SÍ existe no-go, a diferencia de E1/E6.

### Métricas secundarias
`omissionRate` (GO perdidos), `cv`, `anticipationRate`, post-error slowing (RT tras comisión), `lapseRate`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Subida conjunta de comisión + omisión = colapso de control. Anticipación alta = respuesta sin discriminar. RT GO creciente + comisión creciente = fatiga. Timebase no-Android → preliminar.

### Scoring sugerido
`control=(1-faRate)*pc` (comisión penaliza directamente); `usefulSpeed=VCE/VCE_ref` solo de GO-HIT; `stability=(1-cv)*(1-lapseRate)`. Techo: `pc<0.80`||`cv>0.30`||`faRate>0.30`→×0.5. Macro `FAST_IMPULSIVE` si VCE alta pero faRate alta. `preliminary=true`.

### Antitrampa / calidad de datos
Premiar velocidad sin penalizar comisión está PROHIBIDO → el score castiga `faRate`. `anticipationRate` filtra tap-spam (que dispararía comisión). `lureRate` evita estrategia "tap a todo verde-ish". Seeded reproducible.

### Variantes
1. Stop-signal real (E4 de Enfoque): señal de freno tardía → SSRT vía `SsrtCalculator` con `StopSignalTracker` (escalera online).
2. Go/no-go semántico-libre (forma/color).
3. "Doble compuerta": dos reglas de go alternas.

### UX
Símbolo central claro; verde=go, rojo+forma distinta=no-go (doble canal, daltonismo-safe). Feedback borde <130 ms; comisión muestra micro-shake funcional breve. Sin penalización por culpa (mensaje neutro). Duración 4–6 min.

### Implementación KMP
Reusa renderer **single-tap** (go/no-go = tap-or-withhold). Generador `CompuertaGenerator` YA existe (E7) → añadir `noGoRate`/`lureRate`/similitud. `TrialEngine`→adaptativo; `MetricCalculator` (faRate ya soportado donde hay no-go); `MultiSignalStaircaseEngine` con gate de comisión; `FatigueIndex`; `SpeedScoreEngine`. Variante stop-signal usa `SsrtCalculator.SsrtInput`+`StopSignalTracker` (de focus) con `trustworthy=false` si p(respond|signal)∉0.25..0.75. Modelo: `data class CompuertaPrompt(val isGo:Boolean, val isLure:Boolean, val correctChoice:SpeedChoice?)`. Eventos: onset/response/withhold, isCommission, isOmission, deadline. Estado: prompt+deadline+ventana. Sin renderer nuevo (variante stop reusa Stop de focus).

### Riesgos
Con `noGoRate` muy bajo, pocos trials no-go → `faRate` poco `confident`. Mitigación: más trials/bloque, reportar `faRate` `null` honesto si n no-go insuficiente; no inflar dificultad por debajo de n mínimo.

### Revisión crítica
El Revisor es agresivo: "go/no-go premia velocidad y solo descuenta comisión por score — pero el adaptativo podría seguir empujando RT a costa de inhibición si el gate de comisión está mal calibrado". Exige: (1) que el gate de comisión (`LOWER` por `impIdx/faRate`) tenga PRIORIDAD sobre RAISE en la tabla (ya está, documentarlo y testearlo con perfil sintético impulsivo); (2) `MIN_N` de no-go antes de reportar `faRate`; (3) que el techo incluya `faRate>0.30`, no solo pc/cv. Si no, es un generador de impulsividad recompensada — violación directa de "PROHIBIDO premiar impulsividad sin control".

### Veredicto
Aprobado con cambios: techo por faRate, gate de comisión prioritario testeado con sintético impulsivo, y faRate `null` honesto bajo n mínimo.

---

## 5. Cadencia Anticipatoria (Tiempo de Coincidencia)

### Módulo
Principal: Velocidad (timing motor/anticipación temporal). Secundarios: Enfoque (control de impulsividad temporal), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Precisión temporal predictiva: un objeto se desplaza hacia una línea-meta y el usuario debe tocar EN el instante de coincidencia. Mide error temporal con signo (adelanto/retraso) y su variabilidad, no RT reactivo. Castiga la impulsividad temporal por construcción (tocar antes = error grande).

### Hipótesis de transferencia
SÍ (near): tareas de coincidence-timing y anticipación de eventos predecibles. NO afirmar: reflejos deportivos generales, conducción, ni "tiempo de reacción" clásico (esto es predicción, no reacción).

### Loop principal
1. Estímulo: objeto que viaja a velocidad `v` hacia una meta; ETA conocido por trayectoria.
2. Decisión: tap en el instante de coincidencia (ventana de tolerancia `tol`).
3. Feedback: barra de error con signo (+adelanto/−retraso) y magnitud en ms <150 ms.
4. Adaptación: `v`, `tol`, visibilidad de la trayectoria.
5. Siguiente: jitter de velocidad/dirección.

### Reglas
Objetivo = |error temporal| mínimo. Tocar fuera de `tol` = fallo. Tocar muy antes (`occlusionViolation`) = impulsividad. 5 bloques de 20. **Renderer nuevo o reuso del paradigma dinámico** (animación dentro del composable keyed por seed; holder espera UNA pulsación, ya soportado en Enfoque MOT/RSVP).

### Inputs
Tap (timing). Sin elección de lado.

### Variables de dificultad
1. `objectSpeedPxPerS`: lento→rápido (ETA 1500→500 ms).
2. `toleranceMs` (ventana de acierto): ±200→±40 ms.
3. `occlusionFraction` (porción final de trayectoria oculta): 0→0.6 (obliga a extrapolar).
4. `velocityJitter`: 0→0.3 (rompe predicción simple).
5. `trajectoryCurvature`: recta→curva (predicción no lineal).
6. `isiJitterMs`: 200→600 ms.
7. `distractorObjects`: 0→2 (objetos señuelo que no son la meta).

### Algoritmo adaptativo
Staircase sobre |error temporal| (objetivo: mantener `tol`-hit ≈0.79). COOLDOWN(`fatIdx`). LOWER (sube `tol`, baja `v`) si error medio alto o sesgo de adelanto creciente (impulsividad temporal). STABILIZE si variabilidad de error (`cv` temporal) alta con media baja. RAISE recorta `tol`, sube `occlusionFraction`. Baseline ancla `v` inicial.

### Métricas primarias
`timingError_median` (con signo), `timingError_absMedian`, `tol-hit rate`.

### Métricas secundarias
`timingVariability` (SD del error = consistencia), sesgo direccional (adelanto vs retraso), `anticipationRate` (toques pre-occlusion), pendiente error vs `occlusionFraction`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Aumento de variabilidad temporal + deriva del sesgo. Sesgo de adelanto creciente = impulsividad/cansancio. Touch latency del dispositivo contamina (necesita timebase autoritativo Android API34+; en fallback → CONTAMINATED).

### Scoring sugerido
`stability = 1 - clamp(timingVariability / var_ref)`; `control = 1 - clamp(|biasAdelanto|/bias_ref)`; `usefulSpeed = tol-hit rate ponderado por estrechez de tol alcanzada`. `total=mean(...)`; techo si `tol-hit<0.80` o `variability>var_cap`→×0.5. `preliminary=true`. Barras `null` si n insuficiente.

### Antitrampa / calidad de datos
Oclusión + jitter impiden cronometrar visualmente el borde; el adelanto sistemático se penaliza (no se puede "tapear pronto" para ganar). El distractor castiga tap-a-cualquier-movimiento. Seeded reproducible. CRÍTICO: la latencia de input del dispositivo es un sesgo constante → se calibra contra el baseline del MISMO dispositivo (`DeviceProfile`), nunca cross-device.

### Variantes
1. Audio-coincidencia (tono que sube → tocar al pico) accesible.
2. Doble objeto: tocar cuando ambos coinciden.
3. Ritmo: serie de coincidencias periódicas (sincronización sostenida).

### UX
Pista/línea de meta clara, objeto contrastado; barra de error con signo y color semántico (verde centrado, ámbar/rojo a los lados) + magnitud numérica. Microanimación: la animación del objeto ES la tarea (funcional). Duración 3–4 min.

### Implementación KMP
**Requiere renderer dinámico** (animación en composable keyed por `layoutSeed=stimulus.index`, patrón MOT/RSVP de Enfoque; holder espera UNA pulsación — runtime de una respuesta por trial, SIN sub-eventos). En `Canvas`, `cos/sin` de `kotlin.math` → `.toFloat()` antes de `Offset`/radios. Timebase: `SpeedTimebaseHandle` (Choreographer `frameTimeNanos` para onset+posición, MotionEvent nanos para tap) es CRÍTICO aquí. `TimebasePolicy.computeRtNanos` se generaliza a `timingError = tapUptime − coincidenceUptime` (puede ser negativo = adelanto; NO lanzar, este signo es la señal). Generador nuevo `CadenciaGenerator` seeded. `MetricCalculator` necesita un cómputo de error temporal con signo (deuda nueva en speed). `FatigueIndex`; `SpeedScoreEngine` (mapear a usefulSpeed/control/stability). Persistencia `MeasurementRun`. Modelo: `data class CadenciaTrial(val coincidenceUptimeNanos:Long, val tolNanos:Long, val occlusionFraction:Float)`. Eventos: coincidenceUptime, tapUptime, signedErrorNanos, withinTol. Estado: trayectoria+coincidenceTime+tol.

### Riesgos
Latencia táctil del dispositivo = sesgo constante que invalida cross-device; sin timebase autoritativo no hay medida fiable. `computeRtNanos` actual LANZA si rt<0 → aquí el adelanto es negativo legítimo: requiere una política de timing con signo NUEVA (no reusar la de RT que prohíbe negativos).

### Revisión crítica
El Revisor lo casi-descarta: "`TimebasePolicy.computeRtNanos` lanza con rt negativo; tu señal central es el error negativo (adelanto). Reusar el motor de RT como está ROMPE el ejercicio." Exige: (1) una `TimingPolicy` con signo separada, documentada como trabajo nuevo, no "reuso"; (2) demostrar que el sesgo de latencia táctil se cancela por anclaje al baseline del dispositivo, y marcar CONTAMINATED en fallback desktop/iOS; (3) probar que la oclusión realmente fuerza extrapolación (sin oclusión, mide reacción visual, no predicción). Sin (1) no compila la semántica; sin (2) el score es ruido de hardware.

### Veredicto
Aprobado con cambios: implementar `TimingPolicy` con signo, anclaje estricto al baseline del dispositivo, oclusión obligatoria y marca CONTAMINATED fuera de Android autoritativo.

---

## 6. Resolución de Conflicto Veloz (Flanker Express)

### Módulo
Principal: Velocidad (velocidad de selección bajo interferencia). Secundarios: Enfoque (control atencional selectivo), Profile (speed + control).

### Objetivo cognitivo
Velocidad de respuesta cuando flancos distractores empujan a la respuesta opuesta: mide el efecto de congruencia (coste de filtrar interferencia) a alta velocidad. Aísla la eficiencia de selección perceptiva, no la mera velocidad reactiva.

### Hipótesis de transferencia
SÍ (near): otras tareas de interferencia tipo flanker/Stroop perceptivo; reducción del efecto de congruencia. NO afirmar: atención "general", filtrado en entornos reales, ni control ejecutivo clínico.

### Loop principal
1. Estímulo: objetivo central flanqueado por distractores congruentes (→→→→→) o incongruentes (→→←→→).
2. Decisión: responder según la flecha CENTRAL (LEFT/RIGHT) antes del deadline.
3. Feedback: borde <120 ms.
4. Adaptación: deadline, proporción incongruente, separación de flancos.
5. Siguiente: ISI 300–600 ms.

### Reglas
Responder solo al centro, ignorar flancos. Incongruente induce error de comisión hacia el lado del flanco. 6 bloques de 24. Renderer choice.

### Inputs
Tap LEFT/RIGHT.

### Variables de dificultad
1. `deadlineNanos`: 1300→500 ms.
2. `incongruentRate`: 0.3→0.6.
3. `flankerTargetDistanceDp`: lejos→cerca (más cerca = más interferencia): 48→8 dp.
4. `flankerCount`: 2→6.
5. `targetSizeRatio` (centro vs flancos): grande→similar (menos saliente).
6. `isiJitterMs`: 150→500 ms.
7. `responseStimulusInterval`: introduce conflicto de secuencia (Gratton) controlado.

### Algoritmo adaptativo
1-up/3-down sobre pc. COOLDOWN(`fatIdx`). LOWER si pc bajo / `impIdx` alto (responder al flanco). STABILIZE si `cv>cap`. RAISE recorta deadline / sube `incongruentRate` / acerca flancos. **CONGRUENCY_EFFECT solo con HIT-RT en ambos lados** (congruente e incongruente); si falta uno → `null`.

### Métricas primarias
`CONGRUENCY_EFFECT` = `rtMedian_incong − rtMedian_cong` (solo HITs); pc; `rtMedian` global.

### Métricas secundarias
`cv`, comisión en incongruentes (`faRate` direccional), `anticipationRate`, post-conflicto adjustment (Gratton), `lapseRate`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Efecto de congruencia que crece intra-sesión + comisión incongruente creciente = fatiga de control. n insuficiente en celda → efecto `null`. Anticipación alta = adivinación.

### Scoring sugerido
`control=(1-impIdx)*pc`; `stability=(1-cv)*(1-lapseRate)`; `usefulSpeed=VCE/VCE_ref` penalizado por congruency-effect normalizado: `*=(1-clamp(congrEffect/ref)*w)`. Techo estándar. `preliminary=true`. Efecto `null`→no penaliza.

### Antitrampa / calidad de datos
Respuesta derivada del centro (no affordance); incongruencia detecta respuesta-al-flanco; `impIdx`/`anticipationRate` filtran tap rítmico. Seeded balance congruente/incongruente reproducible.

### Variantes
1. Stroop perceptivo (palabra COLOR vs tinta) — requiere adaptación idioma → usar formas/flechas para evitar dependencia cultural.
2. Simon (posición irrelevante vs respuesta).
3. Flanker con carga (más flancos + menor tamaño objetivo).

### UX
Fila de flechas, centro destacado sutilmente (sin revelar respuesta), mitades táctiles. Feedback borde <120 ms. Microanimación: ninguna decorativa; solo feedback. Duración 4–5 min.

### Implementación KMP
Reusa renderer **choice** (mitades LEFT/RIGHT). Generador nuevo `FlankerExpressGenerator` seeded (deriva `correctChoice` del centro). `MetricCalculator` necesita cómputo de CONGRUENCY_EFFECT en speed (deuda; patrón de Enfoque E2 reusable). `MultiSignalStaircaseEngine`; `FatigueIndex`; `SpeedScoreEngine`. Modelo: `data class FlankerPrompt(val centerDir:SpeedChoice, val congruent:Boolean, val flankerCount:Int, val correctChoice:SpeedChoice)`. Eventos: onset/response uptime, isCorrect, congruent, deadline. Estado: prompt+deadline. Sin renderer nuevo; deuda de métrica de congruencia.

### Riesgos
Solapa conceptualmente con E3 (switch) y E4 (go/no-go) en "control". Diferenciador real: aquí la interferencia es ESPACIAL-perceptiva simultánea, no temporal ni de regla. Riesgo: congruency-effect `null` en sesiones cortas.

### Revisión crítica
El Revisor: "¿qué aporta sobre la conmutación (#3) y la compuerta (#4)? Los tres miden 'control a velocidad'." Exige diferenciación medible: (1) #6 mide interferencia perceptiva simultánea (congruency-effect), #3 reconfiguración secuencial (switch-cost), #4 inhibición de prepotencia (comisión no-go) — declarar que las tres métricas son ORTOGONALES y que el brain-profile las trata como sub-señales distintas, no redundantes; (2) usar flechas/formas, NO palabras-color, para evitar dependencia idiomática (PROHIBIDA); (3) MIN_N por celda. Sin (1) es redundante; sin (2) viola la regla cultural.

### Veredicto
Aprobado con cambios: justificar ortogonalidad de la métrica vs #3/#4, estímulos no-lingüísticos obligatorios y MIN_N por celda.

---

## 7. Ráfaga de Detección Serial (Detección RSVP Veloz)

### Módulo
Principal: Velocidad (velocidad de detección perceptiva en flujo rápido). Secundarios: Enfoque (vigilancia breve), Profile (PROCESSING_SPEED).

### Objetivo cognitivo
Velocidad y umbral de detección de un objetivo dentro de una ráfaga serial rápida (RSVP) de no-objetivos: mide la mínima exposición a la que el usuario aún detecta y responde a tiempo. Throughput perceptivo bajo presión de presentación, no de respuesta.

### Hipótesis de transferencia
SÍ (near): tareas de detección rápida en flujo serial; reducción del umbral de exposición. NO afirmar: lectura veloz, percepción "general", ni atención sostenida prolongada (la ráfaga es breve).

### Loop principal
1. Estímulo: secuencia rápida de ítems (formas/letras-neutras) con 0–1 objetivo por ráfaga, a `itemDurationMs`.
2. Decisión: tap en cuanto aparece el objetivo (detección), antes de que termine la ventana.
3. Feedback: HIT/MISS/FALSE_ALARM (tap en no-objetivo) <150 ms.
4. Adaptación: `itemDurationMs`, densidad de distractores, similitud.
5. Siguiente: nueva ráfaga seeded.

### Reglas
Tap solo al objetivo. Tap a no-objetivo = false alarm. No detectar = omisión. 6 ráfagas-bloque de ~15 ítems. **Renderer Stream (RSVP)** ya existe en Enfoque (E10) → portar; holder espera UNA pulsación, animación en composable keyed por seed.

### Inputs
Tap (PRIMARY) durante la ráfaga.

### Variables de dificultad
1. `itemDurationMs` (SOA): 400→80 ms (más corto = más difícil detectar).
2. `distractorSimilarity`: baja→alta (objetivo se camufla).
3. `targetProbability`: 0.8→0.4 por ráfaga (a veces no hay objetivo → mide false alarms).
4. `burstLength`: 10→24 ítems.
5. `targetPositionJitter`: posición serial del objetivo aleatoria (evita anticipar).
6. `interItemMaskMs`: 0→60 ms de máscara (corta el procesamiento icónico).
7. `dualTargetRate`: 0→0.2 ráfagas con 2 objetivos (attentional blink controlado).

### Algoritmo adaptativo
Staircase sobre detección (hit rate objetivo ≈0.79) ajustando `itemDurationMs`. COOLDOWN(`fatIdx`). LOWER (sube `itemDurationMs`) si hit bajo o false-alarm alto. STABILIZE si variabilidad de RT-detección alta. RAISE recorta `itemDurationMs` / sube similitud. Baseline ancla `itemDurationMs` inicial.

### Métricas primarias
`hitRate` de detección, `rtMedian` de detección (onset-objetivo→tap, HIT MAD-limpio), umbral `itemDurationMs` al 79%.

### Métricas secundarias
`faRate` (taps a no-objetivo), `omissionRate`, sensibilidad d' (señal/ruido), `cv`, attentional-blink cost (en `dualTargetRate`), `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Caída de hitRate + subida de false-alarm intra-sesión. RT-detección creciente. False-alarm alto = tap-spam durante ráfaga. Timebase no-Android → onset de ítem no fiable → CONTAMINATED.

### Scoring sugerido
`usefulSpeed = 1 - clamp(umbralItemDuration / dur_ref)` (umbral más bajo = más rápido perceptivamente); `control=(1-faRate)*hitRate`; `stability=(1-cv)*(1-omissionRate)`. Techo si `hitRate<0.80`||`faRate>0.30`→×0.5. `preliminary=true`. d' como señal secundaria descriptiva.

### Antitrampa / calidad de datos
`targetPositionJitter` + `targetProbability<1` castigan tap-anticipado (false alarm en ráfagas sin objetivo). d' separa detección real de sesgo de respuesta. Seeded reproducible. Onset de cada ítem por `frameTimeNanos` (Choreographer) para RT de detección fiable.

### Variantes
1. Detección por color (objetivo = ítem de color único).
2. Conjunción (objetivo = forma+color específicos → detección más lenta).
3. Doble objetivo (attentional blink): dos objetivos cercanos en la ráfaga.

### UX
Ráfaga central a pantalla completa, máscara opcional entre ítems; tap a cualquier parte. Feedback borde + flash del ítem detectado <150 ms. Microanimación: la ráfaga ES la tarea (funcional). `layoutSeed=stimulus.index` para variación. Duración 3–4 min (ráfagas son intensas → bloques cortos anti-fatiga).

### Implementación KMP
**Reusa renderer Stream/RSVP** de Enfoque (E10) portado a Velocidad (animación en composable, una pulsación). Generador nuevo `RafagaGenerator` seeded (posición de objetivo, distractores). Timebase CRÍTICO: onset de ítem vía Choreographer `frameTimeNanos`; RT-detección = `tapUptime − targetOnsetUptime` (`TimebasePolicy.computeRtNanos`, ≥0). `MetricCalculator` necesita hit/fa/d' de detección serial (deuda nueva). `FatigueIndex`; `SpeedScoreEngine`. Persistencia `MeasurementRun`. Modelo: `data class RafagaTrial(val targetSerialPos:Int?, val itemDurationMs:Int, val items:List<ItemId>)`. Eventos: targetOnsetUptime, tapUptime, isHit/isFa/isOmission, itemDuration. Estado: ráfaga+targetPos+ventana. Renderer Stream reusado; deuda de métricas de detección.

### Riesgos
Onset de ítem depende del frame rate real; jank de UI contamina el SOA efectivo. En dispositivos con throttling, `itemDurationMs` nominal ≠ real. Mitigación: registrar `frameTimeNanos` reales por ítem y marcar CONTAMINATED si la varianza de frame es alta.

### Revisión crítica
El Revisor: "tu `itemDurationMs` nominal es una mentira si el dispositivo no renderiza a esa cadencia — un SOA de 80 ms con frames de 16.6 ms tiene granularidad de ±1 frame, y el jank lo arruina." Exige: (1) registrar el onset REAL de cada ítem por `frameTimeNanos` y computar SOA efectivo, no nominal; (2) marcar la ráfaga CONTAMINATED si la varianza de frame supera umbral (jank); (3) no permitir `itemDurationMs`<2 frames del dispositivo. Además: tap único durante ráfaga no distingue a cuál ítem responde → debe atribuirse al ítem visible en `tapUptime`, no al objetivo asumido. Sin (1)/(3), mide el GPU, no la percepción.

### Veredicto
Aprobado con cambios: SOA efectivo medido por frame real, gate de jank (CONTAMINATED), piso de `itemDurationMs` en frames del dispositivo y atribución de tap al ítem visible.

---

## 8. Calibración de Apuesta Temporal (Velocidad-Confianza Metacognitiva)

### Módulo
Principal: Velocidad (velocidad + calibración metacognitiva del propio RT). Secundarios: Enfoque (control de impulsividad), Profile (speed + metacognición).

### Objetivo cognitivo
Velocidad de respuesta acoplada a metacognición temporal: el usuario fija ANTES de cada bloque un "compromiso de deadline" (cuán rápido cree que puede ir manteniendo accuracy), y se mide el alineamiento entre velocidad prometida y rendimiento real. Entrena la calibración velocidad-exactitud, no solo la velocidad.

### Hipótesis de transferencia
SÍ (near): mejor calibración del trade-off velocidad-exactitud en tareas RT; reducción de over/under-confidence temporal. NO afirmar: metacognición "general", toma de decisiones de la vida real, ni rasgo clínico.

### Loop principal
1. Pre-bloque: el usuario elige un nivel de deadline-compromiso (lento/medio/rápido) → `committedDeadline`.
2. Estímulo: tarea 2-AFC simple bajo ESE deadline.
3. Decisión: tap LEFT/RIGHT dentro del `committedDeadline`.
4. Feedback: por trial (HIT/error); por bloque, alineamiento prometido-vs-real (¿cumpliste la velocidad SIN perder accuracy?).
5. Adaptación: las opciones de deadline ofrecidas se recentran sobre el umbral real del usuario.

### Reglas
Cumplir el deadline comprometido manteniendo pc≥banda. Over-commit (prometer rápido y fallar accuracy) penaliza calibración; under-commit (ir lento de sobra) también. 5 bloques de 20.

### Inputs
Selección (nivel de deadline pre-bloque) + tap LEFT/RIGHT en trials.

### Variables de dificultad
1. `committedDeadlineNanos` (elegido): rejilla 1200→450 ms.
2. `deadlineGridSpacing`: ancho→estrecho (opciones más finas = calibración más exigente).
3. `stimulusDiscriminability`: alta→baja (2-AFC más difícil).
4. `accuracyBand` requerida: 0.75→0.85.
5. `isiJitterMs`: 200→500 ms.
6. `commitGranularity`: 3→7 niveles de deadline ofrecidos.
7. `feedbackDelaySec`: inmediato→diferido (fricción metacognitiva).

### Algoritmo adaptativo
Doble: (a) staircase clásico sobre pc dentro del deadline comprometido (objetivo banda); (b) recentrado de la rejilla de opciones sobre el umbral RT real estimado del usuario (las opciones se vuelven más finas a medida que el baseline se estabiliza). COOLDOWN(`fatIdx`). LOWER si pc<banda bajo el compromiso. El motor NUNCA fuerza un deadline; solo recentra las opciones.

### Métricas primarias
`calibrationError` = |deadline prometido − RT-mediano real cumpliendo accuracy|; pc bajo compromiso; `rtMedian`.

### Métricas secundarias
`overCommitRate` (prometer rápido, fallar accuracy), `underCommitRate` (ir lento de sobra), `cv`, tendencia de calibración entre bloques, `impIdx`, `fatIdx`.

### Señales de fatiga o mala calidad
`FatigueIndex` ≥3 bloques. Over-commit creciente + pc cayendo = fatiga/impulsividad. Under-commit sistemático = desenganche/evitación. Selección de deadline siempre máxima sin cumplir = gaming. Timebase no-Android → preliminar.

### Scoring sugerido
`stability=(1-cv)*(1-lapseRate)`; `control=(1-impIdx)*pc`; `usefulSpeed=VCE/VCE_ref`; **modificador metacognitivo**: `metacog=1-clamp(calibrationError/ref)`; `total=mean(usefulSpeed,control,stability)*metacog_weight + ...`. Techo estándar. `metacog` se reporta como señal descriptiva separada (no como velocidad). `preliminary=true`. `null` si insuficiente.

### Antitrampa / calidad de datos
Over-commit no se premia (penaliza calibración Y dispara techo por pc<banda). Under-commit detectado (deadline holgado con RT muy por debajo). El recentrado evita que el usuario "gane" eligiendo siempre lento. Seeded reproducible. `impIdx`/`anticipationRate` filtran tap-spam.

### Variantes
1. Confianza post-respuesta (juzgar acierto tras cada trial) en vez de pre-deadline → calibración de exactitud.
2. Apuesta de puntos por velocidad (sin dinero; gamificación honesta sin culpa).
3. Calibración adaptativa ciega (sin feedback inmediato, solo diferido).

### UX
Pantalla pre-bloque: selector de nivel de velocidad (3–7 chips, `MinTouchTarget`, `verticalScroll` si crecen). Trials: 2-AFC con mitades táctiles. Feedback de bloque: barra "prometido vs real" con color semántico (verde=calibrado, ámbar=over/under). Sin culpa: mensaje neutro de calibración. Duración 4–5 min.

### Implementación KMP
Reusa renderer **choice** + pantalla de selección pre-bloque (componente nuevo de selección, no nuevo paradigma de trial). `committedDeadline` alimenta `FixedTrialEngine`/adaptativo como `deadlineNanos` del bloque. Generador simple 2-AFC seeded (`XorShiftSpeedRng`). `MetricCalculator` + nueva señal `calibrationError` (deuda en speed). `MultiSignalStaircaseEngine` para pc; recentrado de rejilla en capa de presentación. `FatigueIndex`; `SpeedScoreEngine` con modificador metacognitivo. Persistencia: `MeasurementRun` registra `committedDeadlineNanos` por `BlockRecord` (campo nuevo). Modelo: `data class CalibracionBlock(val committedDeadlineNanos:Long, val accuracyBand:Float)`. Eventos: committedDeadline, RT real, pc bloque, calibrationError. Estado: deadline comprometido + trials. Sin renderer de trial nuevo; añade pantalla pre-bloque + métrica de calibración.

### Riesgos
La metacognición temporal es ruidosa y necesita varios bloques para `confident`; sesión única puede dejar `calibrationError` siempre `null`. Riesgo de gaming eligiendo siempre el deadline más holgado. Mitigación: penalizar under-commit, recentrar opciones, reportar `null` honesto.

### Revisión crítica
El Revisor es duro: "esto es un meta-ejercicio: si el usuario elige siempre el deadline más lento, cumple accuracy trivialmente y tu `calibrationError` es bajo — premiarías la cobardía." Exige: (1) penalizar explícitamente el under-commit (deadline holgado con RT muy por debajo del comprometido) además del over-commit; (2) recentrado de rejilla que ESTRECHE las opciones sobre el umbral real, eliminando la opción trivialmente lenta una vez estabilizado el baseline; (3) reportar `metacog` SEPARADO de velocidad para no contaminar el score de speed con una señal metacognitiva ruidosa; (4) requerir varios bloques antes de cualquier claim → `preliminary` + `null`. Sin (1)/(2) es explotable; sin (3) confunde dos constructos.

### Veredicto
Aprobado con cambios: penalizar under-commit, recentrar/estrechar la rejilla de opciones, separar la señal metacognitiva del score de velocidad y exigir multi-bloque antes de reportar.
