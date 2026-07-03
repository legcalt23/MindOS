# MindBoost — MVP Training Acceptance Criteria

**Versión:** 1.0 · **Fecha:** 2026-07-02 · **Alcance:** sistema de entrenamiento cognitivo (sin Elite/IA Coach/PvP).
**Gate global:** el entrenamiento cognitivo es "apto MVP" ⇔ **cero P0 y cero P1 de integridad** (pérdida de datos, señal fabricada, UX engañosa, claims, privacidad) en la superficie publicada, con la cuota MVP por bloque (o el suelo de contingencia DC-19 con etiquetado honesto). Los P1 de cobertura no bloquean si el etiquetado es veraz.

## 1. Ejercicios MVP y post-MVP por bloque

La lista canónica (41 MVP + resto post-MVP, 15+ diseñados por bloque) vive en [mindboost_15_exercises_per_block.md](mindboost_15_exercises_per_block.md) §11–§12 y se resume en el plan maestro §10. Cuotas: Enfoque 6 · Decisiones 5 · Lógica 5 · Creatividad 4 · Lenguaje 6 · Emociones 5 · Velocidad 5 · Memoria 5. Suelo de contingencia 25-jul (CEO): Enfoque 5–7 · Emociones 4–6 · Lenguaje 5–6 · Velocidad 4 · Memoria 4 · Lógica 3 · Decisiones 3 · Creatividad 2–3, siempre con etiquetado honesto (BETA / "Motor de Razonamiento" / "PRÁCTICA").

**Criterio por ejercicio publicado (checklist de 12 puntos, todos obligatorios):**
1. Accesible desde el menú de su bloque (consistency test catálogo↔registry↔renderer en verde).
2. Puede iniciarse, completarse y abortarse sin crash (smoke test).
3. Persiste su resumen de sesión (round-trip test) y su `SessionRecord` transversal.
4. El abandono deja registro `ABANDONED` sin actualizar perfil.
5. Actualiza el perfil solo vía `ScoreResult` con gates (o declara honestamente que no actualiza).
6. Muestra feedback post-sesión con el `ResultScreenScaffold` (informe, sin XP, sin CTA Elite).
7. Dificultad escalable definida (o "fija por diseño" documentado en su ficha).
8. Anti-gaming concreto: suite adversarial del detector/gate que su ficha declara.
9. Versionado: `exerciseVersion` en registry + `scoringVersion`/`bankVersion` en provenance.
10. Copy pasa las 3 variantes del scanner; claim = el de su ficha, nada más.
11. Métricas capturadas = las de su ficha (ni más — minimización — ni menos).
12. No parece infantil ni arbitrario (revisión product/UX firmada).

## 2. P0/P1 que bloquean MVP

| # | Bloqueante | Criterio de cierre verificable |
|---|---|---|
| B1 | Diario/sesión de Decisiones se pierden al sellar | Sellar → reabrir app → diario y sesión visibles; test de regresión rojo→verde en el PR del fix |
| B2 | Reversibility Triage oculto/bloqueado | Banco validado inter-rater (κ≥0.75 o acuerdo ≥85%), `BankIntegrity` validado, `includeReversibility=true`, jugable end-to-end, ScoreResult llega al perfil |
| B3 | Árbol `mindboost/` sin commitear (~40%) | `git status` limpio de untracked en `core/` y `creativity/` |
| B4 | Lenguaje no persiste ninguna sesión | `finish()` invoca repos; resumen cifrado persistido; VERBAL puede salir de CALIBRATING (test end-to-end vía aggregator) |
| B5 | Copy de Creatividad promete "sin evaluación" con scoring visible | Copy nuevo en hub + strings; KDoc corregido; scanner verde |
| B6 | Capa usuario podría mostrar números sin señal | Property tests del `UserScoreMapper`: CALIBRATING→nunca número; gate DC-2 (ESTABLISHED, o EMERGING conf≥0.55 + ≥2 sesiones) |
| B7 | Sesión en curso se pierde sin rastro (transversal) | Matar app a mitad → registro ABANDONED presente, perfil intacto (test por bloque) |
| B8 | Namespaces nuevos fuera del borrado GDPR | `mindboost_sessions`/`mindboost_assessment` en AccountBound + eraser + `AccountBoundErasureCoverageTest` verdes |
| B9 | Analytics sin consentimiento (GA4 activo por defecto) | Manifest disable-by-default + opt-in en onboarding + test de no-emisión sin consentimiento |
| B10 | (Solo si se lanza iOS) store iOS in-memory | Contract test de store durable Keychain + borrado verificado en dispositivo; si no, MVP declarado Android-first |

## 3. P2/P3 que pueden quedar a V1.1

28 rangos; `CreativityProfileSource`; sync cloud opt-in; export/backup si no llega a V1.0; resto de renderers compartidos; rename del namespace legacy de Decisiones; borrado de memoria IA (no aplica a entrenamiento); informes semanales; dashboard interno avanzado; subdimensión visual-espacial de Memoria (hasta señal propia con nMin); detector fast-guessing si no converge (Velocidad queda BETA honesta).

## 4. Qué scores se muestran / cuáles quedan provisionales

| Estado interno | UI |
|---|---|
| CALIBRATING | "Perfil provisional" + "—" (nunca 0) + progreso "X de 5 sesiones · Y de nMin" |
| EMERGING con conf<0.55 o <2 sesiones | Banda ordinal cualitativa + badge "provisional" (sin entero) |
| EMERGING con conf≥0.55 y ≥2 sesiones | Score 0–100 con badge "provisional" |
| ESTABLISHED | Score 0–100 oficial + progresión vs. línea base |
| Anti-gaming WITHHOLD/INVALIDATE | Sesión excluida con estado explícito; score no cambia |
| Emociones (by design) | Métricas de proceso; dimensión "—" hasta holdout |
| Creatividad (MVP) | Métricas de proceso (fluidez/flexibilidad/variedad); sin nota; dimensión "—" |
| Velocidad sin detector ADEQUATE o sin timebase | Jugable + BETA + explicación de elegibilidad de medición |

## 5. Rangos y calibración

- **Ningún bloque muestra rango en MVP** (rangos = V1.1, solo desde ESTABLISHED). En MVP: estado Provisional/Calibrado + score gateado + progresión propia.
- **Calibrado** ⇔ ≥5 sesiones válidas en ≥3 días distintos + ≥2×nMin trials + estabilidad métrica + anti-gaming limpio (DC-3). La UI comunica ambas unidades.
- Bloques esperados en calibración prolongada en MVP (honesto): Emociones (by design), Creatividad (sin source), Velocidad (hasta detector/timebase), y cualquier bloque con uso insuficiente.

## 6. Métricas que se guardan

`SessionRecord` normalizado (plan maestro §11): identificadores+versiones, duraciones, estado de completitud, dificultad inicio/fin/max, contadores (total/correctas/incorrectas/omisiones), accuracy, RT agregado (mediana, CV, p10/p90), `ScoreResultDto`, flags de anomalía enumerados, timebaseQuality, plataforma coarse. **Se omiten deliberadamente:** XP, best/worst RT individuales, modelo de dispositivo, texto libre del usuario fuera de su namespace, trials crudos.

## 7. Analytics que se disparan

Los 12 eventos `mb_*` del plan maestro §12, con opt-in, tipos cerrados y sin scores crudos; más `mb_block_improvement_30d` (cohorte bucketed). Tests: emisión única + canario no-PII por evento; no-emisión sin consentimiento; no-emisión en debug.

## 8. Feedback que se muestra

Post-sesión = informe de rendimiento (`ResultScreenScaffold`): métricas de la ficha del ejercicio, cambio de estado/score si lo hay (con provisionalidad), errores clave, siguiente recomendación (`recommendation_generated`), CTA "entrenar otra vez"/"volver". Histórico por bloque: tendencia basada en sesiones válidas; probes fijos para mejora real (RCI) cuando existan. Nada de celebraciones vacías; tono exigente y constructivo.

## 9. Claims permitidos

Solo los del [guardrails doc](mindboost_science_and_claims_guardrails.md): formato "entrenas y mides X dentro de MindBoost contra tu propia línea base" por bloque + los 7 claims del PRD §27.1. Prohibidos (lista PRD §27.2 + guardrails): IQ, clínicos, transferencia garantizada, "científicamente probado", percentiles poblacionales, rasgos. El copy de store pasa el mismo lexicon del scanner.

## 10. Tests que deben pasar

La batería completa del plan maestro §19 (técnicos + producto + científicos), ejecutada en serie: unit scorers/generadores/dificultad/anti-gaming/persistencia/migración/perfil/calibración/abandono/offline/no-duplicación; consistency tests en los 8 bloques; property tests de visibilidad; scanners (3 variantes) + `:shared:detekt` + `:shared:desktopTest` + `:androidApp:compileDebugKotlin` verdes.

## 11. Pantallas que deben funcionar

1. Dashboard Cognitivo (hub evolucionado con polígono + plan de hoy + tarjetas con estado).
2. Pantalla de bloque estándar (los 8, via `BlockHomeScaffold`).
3. Pantalla de ejercicio (ficha previa con habilidad/dificultad/duración).
4. Sesión de ejercicio (cada MVP, con abandono seguro).
5. Resultado post-sesión estándar (`ResultScreenScaffold`).
6. Evaluación inicial (8 pasos + resultado provisional).
7. Historial/progreso por bloque.
Smoke en Android real y desktop; iOS solo si B10 cerrado.

## 12. Documentación que debe actualizarse

- **PRD v2:** §8.2 (plataformas, Firestore, XP), §10.6/§10.7 (notas Emociones/Velocidad), §11.1 (cuotas + diseñado-vs-implementado), §12.4 (bandas, no scores), §13.4/§13.5 (señales del plan; sin penalización), §14 (dos capas, calibración compuesta, rangos V1.1, global), §15.5 (sin XP/Elite), §19.4 (contrato minimizado), §20 (local-first), §21 (12 eventos + métrica 30d redefinida), §22.3 (offline local-first).
- **CLAUDE.md/HANDOFF:** aprendizajes de las fases; estado por bloque.
- **Docs hermanos de este plan** actualizados si cambia una decisión (versionados).
- **AGENTS.md científico** (PRD §28): fichas de evidencia por paradigma nuevo.

## 13. Checklist final de freeze (Fase 7)

- [ ] B1–B9 cerrados (B10 si iOS) · [ ] cuota o suelo por bloque con etiquetado honesto · [ ] batería §19 verde en serie · [ ] 12 eventos activos con opt-in · [ ] evaluación inicial operativa · [ ] PRD v2 publicado · [ ] backlog V1.1 priorizado · [ ] tag de release del plan · [ ] go/no-go firmado (ceo + qa + efficacy).
