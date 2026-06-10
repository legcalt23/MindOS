# 14 · Riesgos y Decisiones

> Riesgos técnicos, científicos y de producto, con mitigación. Decisiones
> abiertas que debes responder antes de la implementación completa.

## 1. Riesgos técnicos

| ID | Riesgo | Prob. | Impacto | Mitigación |
|----|--------|-------|---------|-----------|
| T1 | **Timing/RT en móvil** (latencia touch→evento→frame contamina RT). | Alta | Alto | Reloj monótono (`kotlin.time`); sellar onset en `withFrameNanos`; comparar solo cambios intra-usuario/mismo device; caracterizar y documentar latencia; tests de timing. |
| T2 | **kotlinx-datetime 0.7.1** rompe `Clock.System` en tests. | Alta | Medio | Todo "ahora" vía `EpochClock`/`MonotonicTimeSource` inyectado. Regla ya documentada en CLAUDE.md. |
| T3 | **Volumen de trials** → coste Firestore / lentitud. | Media | Alto | Local-first; solo resúmenes a cloud; retención corta de crudos. |
| T4 | **expect/actual incompletos** (iOS) rompen build no detectado en Windows. | Media | Medio | Checklist 3 targets por `expect`; stubs documentados; CI iOS (diferido). |
| T5 | **Render de ejercicios con movimiento** (E7/E9/E10) exige timing/animación fina; jank falsea métricas. | Media | Medio | Avanzados al final (F9); medир framerate; degradar dificultad si el device no sostiene FPS. |
| T6 | **Detekt ratchet** bloquea CI por findings nuevos. | Baja | Bajo | Escribir limpio; funciones <50 líneas; evitar `ComplexMethod`. |
| T7 | **Reglas Firestore `limit`** olvidadas → PERMISSION_DENIED silencioso. | Media | Alto | `.limit()` en toda query + regla + test espejo `.mjs`. |
| T8 | **Sobre-arquitectura** (framework genérico para 13 ejercicios antes de tener 1). | Media | Medio | YAGNI; vertical slice E1; abstraer tras 2–3 ejercicios reales. |

## 2. Riesgos científicos

| ID | Riesgo | Mitigación |
|----|--------|-----------|
| C1 | **Confundir aprendizaje de la tarea con mejora atencional** (mejora falsa). | Probes reservados no entrenados + RCI; el motor adaptativo nunca afirma mejora (docs 04/06). |
| C2 | **SSRT inválido por gaming** (ralentizar a propósito). | Método de integración + detección de gaming; marcar no fiable (doc 03). |
| C3 | **RT medio sesgado por lapsos.** | RT mediano como primaria; MAD para outliers (doc 03). |
| C4 | **RCI prematuro sin fiabilidad.** | RCI gated por `reliability` (≥2–3 administraciones). |
| C5 | **Prometer transferencia lejana** (CI, inteligencia, vida diaria). | Prohibido en código y UI; solo near-transfer con probes, con incertidumbre. |
| C6 | **Contaminación de probes** (entrenarlos sin querer). | Bancos disjuntos + test de intersección vacía; formas paralelas rotadas. |
| C7 | **Ignorar fatiga/estado** → conclusiones erróneas. | Cuestionario de estado; fatiga modula adaptación; reportar el mal día. |
| C8 | **Comparar RT entre dispositivos** sin calibrar. | Solo cambios intra-usuario/mismo device; `deviceFingerprint` en baseline. |

## 3. Riesgos de producto

| ID | Riesgo | Mitigación |
|----|--------|-----------|
| P1 | **Convertirlo en minijuegos desconectados.** | Jerarquía dominio→medición→…; cada ejercicio define capacidad/métricas/ejes; no hay "juego" sin medición. |
| P2 | **Gamificación que incentiva entrenar fatigado** (streaks tóxicos). | Gamificación DESPUÉS de medición; `OvertrainingGuard` puede forzar descanso; no premiar volumen ciego. |
| P3 | **Vanity metrics** (números bonitos sin significado). | Toda métrica con `n`/`confident`/banda; probes como evidencia fuerte. |
| P4 | **Privacidad de datos cognitivos** (inferir TDAH/salud). | Local-first cifrado; opt-in para sync; lenguaje no diagnóstico; excluir de backups. |
| P5 | **Frustración/abandono** por dificultad mal calibrada. | Calibración inicial a ~80% accuracy; paso de subida pequeño; deload. |
| P6 | **Expectativas infladas** del usuario. | Onboarding honesto sobre qué es y qué NO es transferencia. |

## 4. Decisiones tomadas en este plan (con justificación)

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Ubicación | `:shared/features/mindboost/focus` | Feature de app, no del build `mindos/` (command-router). |
| Paquete | `com.mindos.app.features.mindboost.focus` | Sigue convención; `mindboost` raíz permite futuros subsistemas (memoria, etc.). |
| Presentación | StateHolder (no ViewModel) | Patrón establecido del repo, multiplataforma. |
| DI | `FocusGraph` sub-graph + `rememberFocusGraph()` | Patrón `AppGraph`; no inflar el graph raíz. |
| Entrada | `HomeDestination.MindBoost` primero | Mínima superficie; promover a `AppRoute` solo si hace falta deep-link. |
| Persistencia | Local-first cifrado (patrón `PublisherLocalDraftStore`) + Firestore opt-in | Privacidad + coste + reutilización. |
| Timing | `kotlin.time.TimeSource.Monotonic` | Multiplataforma, sin deps nuevas. |
| Serialización | kotlinx-serialization (ya presente) | Cero deps nuevas. |
| Dependencias nuevas | **ninguna** para el núcleo | El dominio/métricas/adaptación son Kotlin puro (`kotlin.math`). |

## 5. Decisiones ABIERTAS (debes responder antes de F5/F9)

1. **¿Cloud sync desde el principio o local-only en MVP?**
   Recomendación: local-only en F5; cloud opt-in en F10. ¿De acuerdo?
2. **¿Retención de trials crudos?** ¿Descartar tras resumen, o guardar N sesiones
   localmente para re-análisis? (Impacta privacidad y tamaño.)
3. **¿Idioma de la UI?** El repo mezcla ES/EN; ¿strings de Enfoque en español?
4. **¿`minSdk 29` suficiente?** El timing fino y sensores no requieren más, pero
   confirma que no necesitas APIs de timing de Android 12+ (`FrameMetrics`).
5. **¿Gamificación: alcance?** El plan la pospone a post-medición. ¿Qué forma
   tendrá (XP honesto, rachas con guard anti-fatiga, ninguna)? Definir en F10.
6. **¿Integración con `mindos/` (mind-action / command-router)?** ¿MindBoost debe
   ser invocable por comando de voz/acción? Si sí, hace falta un adaptador; si no,
   queda ortogonal (recomendado para MVP).
7. **¿Banco de probes: cuántas formas paralelas por capacidad?** Mínimo 3 para RCI.
8. **¿Quién valida la ciencia?** ¿Hay revisión de un experto en cognición, o nos
   apoyamos solo en paradigmas publicados citados en la doc?
9. **¿Soporte iOS real en qué fase?** Hoy iOS es shell; ¿los ejercicios deben
   correr en iOS desde F5 o se difiere (stubs) hasta tener macOS/CI?
10. **¿Desktop como target de producto o solo dev/test?** Afecta cuánto pulir
    render no-táctil (teclado).

## 6. Señales de que el plan se está desviando (revisar)

- Aparece una pantalla antes de que su métrica/adaptación esté testeada.
- Se añade una dependencia sin entrada en §4/§5.
- Una métrica se muestra sin `confident`/banda.
- Un probe aparece en una sesión de training.
- El motor adaptativo afirma "mejoraste".
- Se promete transferencia lejana en cualquier string.
- Dificultad escala solo velocidad ignorando RT-CV/lapsos/fatiga.
