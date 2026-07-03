# Mindboost Lógica · Estado de implementación

Fecha: 2026-06-15

## Vertical slice implementada

- Entrada `MindBoost → Lógica` desde el hub existente.
- Taxonomía de 28 subcapacidades y cinco familias activas:
  deducción, condicionales, necesario/suficiente, consistencia y contraejemplos.
- Content pack declarativo de seis ítems, con un par isomorfo de condicionales.
- Intento trazable: respuesta, premisas, regla, justificación estructurada, confianza,
  pistas, tiempo, ICR, errores y transferencia.
- Scorer ICR de proceso con calibración, independencia de pistas y penalizaciones.
- Feedback determinista por patrón lógico sin revelar inmediatamente la solución.
- Adaptación inicial por ICR, justificación, errores repetidos e isomorfos.
- Flujo Compose completo: entrada, ejercicio, feedback y resumen.
- Persistencia local cifrada en Android mediante `SecureKeyValueStore`.

## Arquitectura

```text
features/mindboost/logic/
  domain/
    model/       modelos y taxonomía
    content/     contrato y content pack inicial
    scoring/     ReasoningScorer
    feedback/    FeedbackEngine
    adaptive/    DifficultyAdapter
    repository/  puerto de persistencia
  data/local/    agregado JSON versionado
  di/            LogicGraph
  presentation/  state holder y Compose UI
```

El dominio no depende de Compose ni de plataforma. La UI orquesta a través de `LogicGraph`.
Un proveedor generativo futuro puede sustituir `DefaultLogicContentProvider` sin cambiar scorer,
sesión o presentación.

## Privacidad

- Los intentos y estados cognitivos son datos sensibles y se guardan cifrados en Android.
- No se recoge ni persiste texto libre de justificación.
- La justificación es estructurada: premisas, regla y tipo de conclusión.
- La persistencia conserva como máximo 30 sesiones por usuario.
- `LogicTrainingRepository.deleteUserData(userId)` elimina el agregado del usuario.
- Desktop/iOS usan el fallback en memoria ya establecido por el proyecto.

## Añadir una familia de ejercicios

1. Añadir el tipo a `LogicExerciseType` si no existe.
2. Crear ítems declarativos con respuesta, premisas requeridas, reglas válidas, distractores
   explicados, estructura lógica e isomorfos.
3. Añadir el mapeo de feedback si introduce un nuevo `DistractorType`.
4. Añadir opciones de justificación estructurada en `claimChoices`.
5. Añadir tests de validez, error dominante, scoring, feedback y transferencia.

## Verificación

```powershell
.\gradlew.bat :shared:desktopTest
.\gradlew.bat :shared:compileAndroidMain
.\gradlew.bat :shared:detekt
```

## Pendiente para Fase 2

- Generación paramétrica con oráculo.
- Retests espaciados y retención.
- Ítems T2-T5 y Transfer Passport.
- Reasoning Replay y Logic Autopsy completos.
- Analítica minimizada y consentida.
- Calibración psicométrica con datos reales; IRT/BKT/CAT.
- Persistencia durable iOS mediante Keychain.

## Riesgos conocidos

- El content pack inicial es pequeño y authored; sirve para validar arquitectura, no psicometría.
- `thetaEstimate` es una heurística inicial y no debe presentarse como medida clínica.
- La transferencia solo se reporta para el isomorfo incluido; el resto permanece en calibración.

