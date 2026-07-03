# MindBoost · Lenguaje v6 — Overview de arquitectura

Feature aislada en `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/`,
misma convención que `focus`/`memory`/`speed`/`logic`. **Sin módulos Gradle nuevos**: el mapa de
módulos del v6 §12.2 se traduce a paquetes.

## Principios

1. **Offline-first.** El entrenamiento base funciona sin backend. IA/evaluador remoto, calibración
   avanzada y sync son capas opcionales detrás de interfaces, nunca dependencias duras del núcleo.
2. **Toda la lógica en `commonMain`.** Pura, testeable en JVM (`desktopTest`), sin Compose/Firebase.
   La única superficie de plataforma reusada es el `SecureKeyValueStore` (AES-GCM + Keystore) de focus.
3. **Honestidad cableada.** La diferencia entre app de ejercicios y entrenador real está codificada:
   gates que impiden subir el perfil con evidencia débil, ClaimEngine que bloquea copy no demostrado,
   event sourcing que hace el perfil reconstruible y borrable.
4. **Sin Room/SQLDelight.** Datos cognitivos sensibles → `@Serializable` + `SecureKeyValueStore`;
   la integridad relacional vive en los repositorios, no en SQL.

## Paquetes (`domain/`)

| Paquete | Responsabilidad |
|---------|-----------------|
| `model` | Entidades y value objects (taxonomía, EvidenceState, TaskPassport, Attempt, EvidencePacket, perfiles, dosis, consentimiento, summary). |
| `content` | `ContentValidator` → reporte de issues accionables. |
| `claim` | `ClaimEngine` → claims gobernados por evidencia. |
| `session` | `LanguageSessionReducer` puro (8 fases, request→feedback→next, branch de reparación). |
| `scoring` | `ClosedScoringEngine` (MCQ / evidencia / orden) + gates §5.6. |
| `repair` | `RepairEngine` (catálogo 14 patrones, detección por frecuencia cruda). |
| `adaptation` | `AdaptationEngine` (utilidad §6.4, exposición, anti-repetición). |
| `gates` | `FatigueGate`, `ConfounderEngine`, `FairnessGate` — computan las señales que el scoring consume. |
| `projection` | `ProfileProjector` — perfil reconstruido del stream de `EvidencePacket` (peso 0 ≠ promoción). |
| `transfer` | Transfer Lab: niveles T0–T6, misiones funcionales, verificador de transferencia cercana. |
| `evaluator` | `EvaluatorClient` (interfaz) + `LocalStubEvaluatorClient` offline (nunca certifica E2+). |
| `evidence` | `EvidenceEngine` — política de promoción avanzada E1→E4. |
| `repository` | Contratos de repositorio (`suspend`, `deleteUserData` por usuario). |

`data/local/` implementa los repositorios sobre `SecureKeyValueStore`.
`presentation/` contiene UI models puros + builders (`GenomeUiBuilder`, `CoachUiBuilder`,
`ProgressUiBuilder`) — los contratos que harán la UI correcta; las pantallas Compose están diferidas.

## Flujo del ciclo (verificado en `LanguageCycleEndToEndTest`)

```
Respuesta → ClosedScoringEngine (gates) → EvidenceEngine (estado soportado)
          → EvidencePacket → ProfileProjector (perfil) → ClaimEngine (copy honesto)
          → presentation builders (UI sin falsa precisión)
```

## Cómo extender

- **Nuevo motor**: `fun interface` + `Default*` impl + modelos `@Serializable`, todo en `commonMain`,
  con tests en `commonTest` y constantes nombradas (detekt ratchet).
- **Tiempo**: inyectar epoch millis; nunca `Clock.System.now()` directo en dominio/presentación.
- **Verificar**: `:shared:desktopTest --tests "com.mindos.app.features.mindboost.language.*"` y `:shared:detekt`.
