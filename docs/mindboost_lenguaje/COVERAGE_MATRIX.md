# MindBoost · Lenguaje v6 — Matriz de cobertura

Estado de implementación frente a `MindBoost_Lenguaje_v6_NoCompromise_WorldClass.md`.
Leyenda: ✅ implementado offline en `commonMain` · 🟡 parcial / contrato listo · 🔵 diferido (necesita backend, datos reales o validación empírica) · ⬜ no iniciado.

Ubicación raíz: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/`.

| v6 §  | Tema | Estado | Dónde |
|------|------|:------:|-------|
| §0–§2 | Dictamen / paradigma / LTOS | 🟡 | El LTOS no es una clase única; sus decisiones viven repartidas en los engines puros. La traza `TrainingOperatingDecision` global está 🔵. |
| §3 | Arquitectura estratégica, Decision Risk Tiers | 🟡 | Separación de responsabilidades por paquete; tiers D0–D6 no formalizados como tipo. |
| §4.2 | ConstructGraph — 19 rasgos | ✅ | `domain/model/LanguageTaxonomy.kt` (`TraitCode`). Aristas del grafo (prerrequisitos) 🔵. |
| §4.3 | `CognitiveDose` (10 ejes) | ✅ | `domain/model/CognitiveDose.kt` |
| §4.4 | Reglas de dosis / fatiga | 🟡 | `domain/gates/FatigueGate.kt` computa fatiga; el `CognitiveDoseEngine` que mueve la dosis es 🔵. |
| §5.2 | Estados de evidencia E0–E5 | ✅ | `domain/model/EvidenceState.kt` |
| §5.4 | `AbilityPosterior` | 🟡 | Tipo ✅; θ (IRT) es placeholder (0.0) en `ProfileProjector` → 🔵 calibración real. |
| §5.5 | Gates multiplicativos de evidencia | ✅ | `EvidenceGate` + `ClosedScoringEngine` + `DefaultEvidenceEngine`. |
| §5.6 | No subir perfil con fuerza (confusores) | ✅ | `ClosedScoringEngine.deriveFlags` + `DefaultEvidenceEngine`. |
| §5.7 / §11.6 | ClaimEngine + copy prohibido | ✅ | `domain/claim/` + `presentation/ProgressUiBuilder.kt`. |
| §6.2 / §12.4 | Session Orchestrator / reducer | ✅ | `domain/session/LanguageSessionReducer.kt` (8 fases, puro y total). |
| §6.3 | Tipos de sesión | 🟡 | Fases modeladas; catálogo de tipos (Pulse/Core/…) como configuración 🔵. |
| §6.4 | Política de adaptación (utilidad) | ✅ | `domain/adaptation/DefaultAdaptationEngine.kt`. IRT/CAT completo 🔵. |
| §6.5 / §6.6 | Repair Protocol + taxonomía de error | ✅ | `domain/repair/` (catálogo 14 patrones, detección por frecuencia cruda). |
| §6.7 | Mantenimiento / olvido | 🟡 | `RetentionPolicy` + promoción E4 en `EvidenceEngine`; scheduler de intervalos 🔵. |
| §7.2 | Task Passport + validación | ✅ | `domain/model/TaskPassport.kt` + `domain/content/ContentValidator.kt`. |
| §8.2 | Content Compiler (validación bloqueante) | 🟡 | Validación de metadatos ✅; validación del cuerpo del ítem (clave/distractores) 🔵 (necesita `RawTask`). |
| §8.3 | Bancos separados | ✅ | `BankType` + uso en scoring/evidencia. |
| §9 | Evaluator OS | 🟡 | `domain/evaluator/` interfaz + `LocalStubEvaluatorClient` offline ✅; evaluador real (jueces, harness, bias suite) 🔵. |
| §9.6 | Human Review Queue | 🟡 | `HumanReviewStatus`/`ReviewTrigger` modelados; cola real 🔵. |
| §10 | Transfer Lab (T0–T6, misiones) | 🟡 | `domain/transfer/` niveles + contrato de misión + verificador cercano ✅; transferencia funcional real depende del evaluador 🔵. |
| §11 | UX (Skill Genome, Cockpit, Coach, Progress) | 🟡 | `presentation/` UI models + builders ✅; pantallas Compose + wiring en `MindBoostHubScreen` ⬜ (diferido por diseño). |
| §12.3 | Event sourcing (proyección de perfil) | ✅ | `domain/projection/ProfileProjector.kt`. |
| §12.5 | Repositorios | ✅ | `domain/repository/LanguageRepositories.kt` + `data/local/`. |
| §12.6 | Persistencia local | ✅ | `data/local/LocalLanguageRepositories.kt` sobre `SecureKeyValueStore` cifrado (sin SQL/Room). Sync 🔵. |
| §13 | Privacidad (consentimiento, retención, borrado) | ✅ | `domain/model/Consent.kt` + `deleteUserData` por usuario; evaluador remoto consent-gated. |
| §5.3 / §8.6 | Fairness / DIF | 🟡 | `domain/gates/FairnessGate.kt` aplica política; cálculo real de DIF 🔵 (datos de cohorte). |
| §14–§17 | Analítica, governance, roadmap, launch gates | 🔵 | Fuera del núcleo offline; backend/operación. |

## Cadena mínima v6 (§0.2) — estado

`Calibrar → Entrenar → Detectar error → Reparar → Verificar (material nuevo) → Transferir → Mantener → Claim honesto`
está cubierta **de extremo a extremo offline** y verificada por `LanguageCycleEndToEndTest`. Lo que falta para "world-class" es empírico: evaluador real, calibración IRT, datos longitudinales y la UI.
