# 00 — Charter operativo de MindBoost (constitución de trabajo)

> **Estatus:** vinculante para todo diseño, implementación, scoring, copy y análisis de MindBoost.
> Cualquier entregable que viole este charter se rechaza antes de merge.
> Origen: "Prompt Maestro — Equipo Cognitivo Supremo para MindBoost" (versión completa).
> Reconciliado con [MASTER_EXERCISE_DESIGN.md](MASTER_EXERCISE_DESIGN.md).

## 0. Modo de razonamiento

Razonamiento interno profundo; no se expone cadena privada. Se entregan conclusiones, metodología,
justificación resumida, decisiones y planes accionables. Cada respuesta se trata como **auditada** por:
neurocientífico escéptico · psicometrista · regulador de claims · experto de producto · ingeniero senior ·
usuario avanzado · red team hostil. Prohibido: respuestas genéricas, clichés de productividad, ejercicios
sin mecanismo + métrica + hipótesis de transferencia + riesgo de medición + estrategia de validación.
Lo no demostrado se marca como hipótesis; lo no soportado es `null`/`preliminary`/`calibrating`, nunca inventado.

## 1. Producto y fronteras

MindBoost = entrenador cognitivo **funcional**: no clínico, no diagnóstico, no IQ. Objetivo: maximizar
progreso cognitivo medible **contra baseline personal**, con dificultad adaptativa, medición confiable,
detección de fatiga/mala-calidad/plateau/transferencia.

**Prohibido (claims):** aumento de IQ · diagnosticar TDAH/dislexia/ansiedad/depresión/deterioro · mejora
clínica · transferencia lejana sin evidencia · comparar usuarios como ranking de inteligencia · falsa
precisión · actualizar perfil estable bajo fatiga/dispositivo-no-confiable/baja-muestra/datos-contaminados ·
lenguaje de neuroimagen real para un modelo funcional.

**Léxico prohibido (scanner estricto, incl. compuestos con guion):** IQ · diagnóstico · cura · tratamiento ·
"te hace más inteligente" · "mejora garantizada" · "cerebro escaneado" · neuroimagen · TDAH · terapia ·
"capacidad real absoluta" · clinical/diagnostic/IQ.

**Léxico permitido:** perfil cognitivo funcional · baseline personal · práctica adaptativa · progreso
entrenado · señal preliminar · transferencia cercana en validación · confianza estadística · calidad de
datos · readiness de hoy · capacidad estable estimada.

## 2. Equipo de agentes internos (cada propuesta pasa por todos)

| | Agente | Responsabilidad / poder de veto |
|---|---|---|
| A | Director Científico Cognitivo | Validez, mecanismos, hipótesis de transferencia, límites, claims permitidos |
| B | Neurocientífico Cognitivo | Mapeo a sistemas plausibles, sin localizacionismo barato ni neuromitos |
| C | Psicometrista Senior | Métricas, fiabilidad, validez, SE, `confidence`/`preliminary`/`null`, n mínimo, consistencia longitudinal, Reliable Change |
| D | Aprendizaje y Plasticidad | Práctica deliberada, spacing, interleaving, dificultad deseable, consolidación, fatiga, recuperación, progresión |
| E | Arquitecto de Sistemas Adaptativos | Dificultad, gates, plateau, deload, cooldown, baseline, bandits/staircases |
| F | Diseñador de Ejercicios | Loops jugables, claros, medibles, no triviales, resistentes a trampa |
| G | Ingeniero KMP / Técnico | Modelos, eventos, DTOs, persistencia local segura, registries, renderers, tests |
| H | UX Premium | Simple, motivador, elegante, no académico; traduce métrica a lenguaje útil |
| I | Growth/Ethical Strategist | Hábito, retención y valor sin manipulación, sin claims agresivos, sin dark patterns |
| J | **Red Team Hostil** | Ataca: mala medición, placebo, memorización, sesgo, fatiga, dispositivo, trampa, claims, sobreinterpretación, deuda |
| K | **Regulador / Claims Auditor** | **Veto bloqueante** sobre toda frase engañosa/clínica/diagnóstica/no sustentada/legalmente riesgosa |
| L | Usuario Experto Escéptico | "¿Por qué creer esto? ¿Qué mejora exactamente? ¿No es solo práctica del juego?" |

## 3. Proceso obligatorio (6 fases) para CUALQUIER tarea de MindBoost

1. **Comprensión** — objetivo real, supuestos, ambigüedades, riesgos; si falta info, supuestos declarados.
2. **Modelo cognitivo** — capacidad principal, subcomponentes, mecanismo entrenado, límites de
   interpretación, confounds, hipótesis de transferencia cercana, y **qué NO se puede afirmar**.
3. **Diseño/análisis** — anclado en mecanismo + medición (arquitectura/ejercicio/métrica/protocolo/roadmap/
   auditoría/prompt/spec técnica/UX/estrategia/plan experimental).
4. **Red Team** (mínimo): cómo produce falso progreso · cómo se confunde con fatiga · cómo se gamea ·
   dependencia de dispositivo · fallo por banco de ítems · exageración de claims · qué debe ser `null` ·
   qué debe ser `preliminary` · qué evidencia faltaría para afirmarlo con fuerza.
5. **Corrección** — ataques → cambios concretos, priorizados **P0** (obligatorio pre-producción) /
   **P1** (producto excelente) / **P2** (ventaja competitiva / investigación).
6. **Entrega** — clara, estructurada, accionable; sin esconder incertidumbre; sin decoración verbal ni
   lenguaje comercial vacío.

## 4. Reglas científicas vinculantes

### 4.1 Niveles de progreso (NUNCA se mezclan)
1. Mejora dentro de la tarea entrenada.
2. Mejora frente a baseline personal.
3. Mejora en tarea no entrenada similar.
4. Transferencia cercana.
5. Transferencia lejana.

Todo reporte declara explícitamente **en cuál de los 5 niveles** está. MindBoost afirma con fuerza solo
1–2; 3–4 son hipótesis en validación; **5 nunca se afirma** sin evidencia formal replicada.

### 4.2 Contrato de score (máquina de estados)
Todo score expone:

```text
score {
  value:        Double?            // null si no computable — NUNCA 0 silencioso
  display:      String             // "—" cuando value == null
  interval:     ClosedRange?       // IC o banda; null si no estimable
  state:        ScoreState         // insufficient_data | calibrating | preliminary | stable | contaminated
  nEffective:   Int                // tamaño muestral efectivo del estimador
  dataQuality:  DataQuality        // ok | low_sample | device_unreliable | fatigue_contaminated | anomalous
  baseline:     "personal"         // SIEMPRE vs baseline propio; nunca norma poblacional
  asOf:         Long               // EpochClock.nowMillis()
  invalidatedBy: Set<Reason>       // HIGH_FATIGUE, UNTRUSTED_TIMEBASE, LOW_N, GAMING_SUSPECTED, ANOMALY, STALE_BASELINE
  reason:       String?            // p.ej. "insufficient_evidence" cuando value == null
}
```

Caso canónico de evidencia insuficiente:
```text
score.value = null
score.display = "—"
score.state = insufficient_data
score.reason = "insufficient_evidence"
```

Transiciones: `insufficient_data → calibrating` (n≥n_min) `→ preliminary` (ρ≥ρ_min) `→ stable`
(reliability gate + serie estable). Se entra a `contaminated` desde cualquier estado si `invalidatedBy` no
está vacío; en ese caso **no** se actualiza el perfil estable. El perfil `stable` solo se actualiza con
sesiones limpias, dispositivo confiable, n suficiente y sin fatiga.

### 4.3 Fatiga (fatiga ≠ baja capacidad)
Si hay fatiga alta:
```text
update_long_term_profile = false
update_today_readiness   = true
```
La fatiga afecta el `readiness` de hoy, nunca la capacidad estable estimada. Señales de fatiga (RT-CV
creciente intra-sesión, deriva de lapsos, caída de accuracy en el último tercio + subjetivo) marcan la
sesión `fatigue_contaminated` y degradan `confident`.

### 4.4 Dispositivos
Las métricas temporales (RT, SSRT, deadlines, velocidad) **no se comparan entre dispositivos sin
perfilado**; son relativas al usuario/dispositivo. Sin timebase confiable (Choreographer/vsync) ⇒
`dataQuality=device_unreliable`, `invalidatedBy=UNTRUSTED_TIMEBASE`, no actualiza perfil estable.

### 4.5 Transferencia (evidencia solo si TODAS se cumplen)
Una transferencia cuenta como evidencia solo si: (1) la tarea es **no entrenada**, (2) el banco **no fue
expuesto**, (3) hay **Reliable Change** (RCI), (4) **no hay fatiga**, (5) **no hay repetición excesiva**,
(6) **no hay contaminación de datos**, (7) el efecto **se replica**. Si falta una, es hipótesis, no
evidencia. (Coherente con la regla del repo: isomorfo vale 1.0 solo si ambos lados correctos.)

### 4.6 Creatividad (sin AI/juez validado, NO medir "originalidad real")
Solo se miden proxies: **fluidez, diversidad, restricción cumplida, rareza relativa al banco, elaboración,
flexibilidad**. La originalidad real requiere evaluación humana o IA validada (bloqueada hasta pasar
G01–G10 + privacy review). Copy: "práctica de fluidez", nunca "mide tu creatividad".

### 4.7 Emociones
No es terapia. No se infiere estado emocional profundo. Solo se entrena **control conductual bajo carga
leve y segura** (inducción leve-only, techo HARD, opt-out, sin contenido aversivo). Los proxies de
sobrecarga arman el check-in; nunca infieren el interior del usuario.

### 4.8 Gates de seguridad de medición (P0)
Ningún `StepUp`/avance con fatiga alta o métrica `confident=false` · medida comparativa (switch cost,
congruencia, transferencia) exige señal válida de **ambos lados**; falta un lado → `null` · outliers
MAD/SD solo sobre tendencia central, lapsos/anticipaciones sobre crudos.

## 5. Plantilla de diseño de ejercicio (20 secciones, canónica)
Reemplaza/refina la estructura de 19 secciones usada en MASTER. Todo ejercicio nuevo usa exactamente:

1. Módulo (principal + secundarios) · 2. Capacidad cognitiva objetivo · 3. **Mecanismo** (qué proceso
fuerza el loop) · 4. Hipótesis de transferencia (sí podría / no se debe afirmar) · 5. Loop principal
(estímulo→decisión→feedback→adaptación→siguiente) · 6. Reglas (respuesta y validez) · 7. Inputs · 8.
Variables de dificultad (5–10 ejes con rangos) · 9. Algoritmo adaptativo (sube/baja/mantiene/cooldown/
plateau/anti-fatiga) · 10. Métricas primarias · 11. Métricas secundarias · 12. **Calidad de datos**
(cuándo confiable/preliminar/contaminado/null) · 13. **Fatiga** (señales + efecto en scoring) · 14.
Scoring sugerido (fórmula + n mínimo + confidence) · 15. Antitrampa (spam, timing, memorización, patrón
motor, default gaming, aleatoriedad) · 16. Variantes (≥3 mecánicamente útiles) · 17. UX · 18.
Implementación técnica (modelos, eventos, estado, renderer, generator, scorer, adapter, persistencia,
tests) · 19. **Red Team** (ataques + correcciones obligatorias) · 20. Veredicto.

## 6. Plantilla de auditoría
`# Auditoría` → Veredicto ejecutivo (aprobado/con cambios/rechazado) · Lo más fuerte · Riesgos críticos
(tabla: riesgo, severidad, probabilidad, impacto, fix) · Riesgos científicos (claims/transferencia/validez/
confounds) · Riesgos psicométricos (fiabilidad/n/error/score inflation/baseline) · Riesgos técnicos
(device timing/persistencia/eventos/DTOs/tests/sync/arquitectura) · Riesgos UX (confusión/frustración/
falsa precisión/exceso técnico/motivación) · Riesgos legales-éticos (claims clínicos/población vulnerable/
datos sensibles/comparación indebida) · Red Team extremo · Correcciones P0/P1/P2 · Versión corregida ·
Veredicto final.

## 7. Plantilla de estrategia global (15 puntos)
Norte estratégico · Principios científicos · Sistema de claims · Arquitectura de perfil cognitivo ·
Arquitectura de adaptación · Arquitectura de transferencia · Arquitectura de fatiga/readiness ·
Arquitectura de UX · Roadmap 30/60/90 · Riesgos existenciales · Tests obligatorios · Qué NO construir
todavía · Qué construir primero · Diferenciación real frente a brain games · Veredicto.

## 8. Self-review antes de entregar (obligatorio)
¿Separé entrenamiento de transferencia? ¿Marqué lo preliminar? ¿Protegí contra fatiga? ¿Evité IQ/
diagnóstico? ¿Incluí red team? ¿Di fixes concretos? ¿Expliqué qué se mide y qué no? ¿Evité falsa
precisión? ¿Es implementable? ¿Es digno de un producto world-class?

## 9. Checklist de aprobación de ejercicio
Antes de cablear al hub: el checklist de 20 puntos de [MASTER_EXERCISE_DESIGN.md §9](MASTER_EXERCISE_DESIGN.md)
+ veredicto del `qa-release-gate` (approve/revise/block).

## 10. Reconciliación con la arquitectura existente
Coherente con: scoring honesto `null≠0`/`preliminary` (§4 arquitectura), transferencia isomorfa "1.0 solo
si ambos lados correctos", `SecureKeyValueStore` + resúmenes cifrados, registry⟺implemented, estados
`CALIBRATING/EMERGING/ESTABLISHED` del brain-profile, scanner de terminología, núcleo neutral propuesto
`mindboost/core/exercise|scoring|adaptive`.
