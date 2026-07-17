# MindBoost — Exercise Quality Standard

**Versión:** 1.0.0  
**Fecha:** 2026-07-10  
**Owner:** Product + Cognitive Science + Psychometrics + Engineering  
**Estado:** APPROVED  
**Objetivo de catálogo:** al menos 15 ejercicios por cada uno de los 8 bloques visibles; mínimo 120 ejercicios completos

---

## 1. Propósito

Este estándar define qué significa que un ejercicio de MindBoost sea digno de llegar al usuario.

Un ejercicio no está terminado porque pueda jugarse. Debe tener una razón cognitiva defendible, mecánicas correctas, dificultad controlable, medición honesta, feedback útil, accesibilidad, privacidad, rendimiento y pruebas suficientes para el estado que muestra.

Principio rector:

> Cada ejercicio debe ser simultáneamente una experiencia clara, un entrenamiento intencional y un sistema de datos que no prometa más de lo que puede medir.

---

## 2. Alcance del catálogo

### 2.1 Cobertura visible

El lanzamiento inicial debe incluir al menos:

| Bloque visible | Mínimo técnico completo |
|---|---:|
| Enfoque | 15 |
| Decisiones | 15 |
| Lógica | 15 |
| Creatividad | 15 |
| Lenguaje | 15 |
| Emociones | 15 |
| Velocidad mental | 15 |
| Memoria | 15 |
| **Total** | **120** |

Puede haber más si aportan cobertura, variedad o transferencia sin diluir calidad.

### 2.2 Qué significa “completo”

Para contar en el mínimo de lanzamiento, un ejercicio debe:

- Tener loop jugable de principio a fin.
- Funcionar en Android e iOS con paridad funcional.
- Funcionar offline en el plan gratuito.
- Generar o cargar contenido válido sin IA generativa.
- Incluir tutorial, práctica y cierre.
- Implementar dificultad, scoring interno y métricas definidas.
- Gestionar pausa, interrupción, reanudación y salida.
- Cumplir accesibilidad mínima y reduced motion.
- Tener tests y telemetría del contrato.
- Mostrar su nivel real de evidencia.

No necesita estar psicométricamente validado para ser entrenable, pero no puede contribuir a un score oficial hasta superar los gates correspondientes.

### 2.3 Cobertura sin permisos sensibles

Debe existir una ruta completa para entrenar los ocho bloques sin conceder cámara, micrófono, movimiento, ubicación, contactos ni otros permisos sensibles.

Los sensores se seleccionan por ejercicio y solo si aportan valor claro. Denegar un permiso activa una alternativa funcional; nunca bloquea un bloque completo.

---

## 3. Estados del ciclo de vida

Todo ejercicio tiene un `validationStatus` visible para producto y auditable:

```text
IDEA
DESIGNED
IMPLEMENTED
TECHNICALLY_VERIFIED
TRAINING_READY
PROVISIONAL_MEASUREMENT
CALIBRATION_DATA_COLLECTING
OFFICIAL_MEASUREMENT
TRANSFER_RESEARCHED
DEPRECATED
RETIRED
```

### 3.1 Reglas de uso

| Estado | Entrenamiento | Perfil provisional | Score oficial | Claims de transferencia |
|---|---:|---:|---:|---:|
| `TRAINING_READY` | Sí | No | No | No |
| `PROVISIONAL_MEASUREMENT` | Sí | Sí, etiquetado | No | No |
| `CALIBRATION_DATA_COLLECTING` | Sí | Sí | No | No |
| `OFFICIAL_MEASUREMENT` | Sí | Sí | Sí | Solo si existe evidencia separada |
| `TRANSFER_RESEARCHED` | Sí | Sí | Sí | En el alcance exacto respaldado |

Un único campo no sustituye a un expediente de evidencia; resume el gate más alto superado.

### 3.2 Elegibilidad ortogonal

El ciclo de vida no debe confundirse con qué puede alimentar cada ejecución. Mantener además un campo separado `evidenceEligibility`:

```text
TRAINING_ONLY
PROVISIONAL_PROFILE_ELIGIBLE
PROFILE_ELIGIBLE
RANK_ELIGIBLE
```

- `validationStatus` responde qué evidencia acumulada tiene la versión del ejercicio.
- `evidenceEligibility` responde qué rutas permite una ejecución bajo condiciones concretas.
- Una versión puede estar `OFFICIAL_MEASUREMENT`, pero una sesión offline pendiente, repetida o técnicamente comprometida sigue sin ser oficial.
- `RANK_ELIGIBLE` requiere un gate adicional; no se deriva automáticamente de `PROFILE_ELIGIBLE`.

---

## 4. Anatomía obligatoria de un ejercicio

Cada ejercicio debe tener:

1. **Promesa de usuario:** qué práctica y por qué importa, sin exagerar.
2. **Constructo objetivo:** subhabilidad primaria y secundarias.
3. **Mecánica central:** acción repetible que exige el mecanismo previsto.
4. **Generador o banco:** contenido reproducible, válido y versionado.
5. **Máquina de estados:** transiciones deterministas y recuperables.
6. **Control de dificultad:** parámetros, límites y adaptación.
7. **Contrato de métricas:** eventos, reglas y agregación.
8. **Feedback:** durante y después, según modo.
9. **Accesibilidad:** capacidades exigidas, ajustes y alternativas.
10. **Integridad:** exposición, anti-automatización proporcional y duplicados.
11. **Persistencia offline:** resultado provisional y sincronización idempotente.
12. **Tests:** lógica, scoring, UI, accesibilidad, rendimiento y casos adversarios.
13. **Expediente científico:** rationale, amenazas, evidencia y claims permitidos.

---

## 5. Exercise Dossier obligatorio

No se implementa un ejercicio sin un dossier versionado. Plantilla mínima:

```yaml
identity:
  exerciseId: stable-kebab-case-id
  exerciseVersion: semver
  visibleName: ""
  internalName: ""
  blockId: ""
  owners: []
  status: IDEA

user_value:
  oneSentencePromise: ""
  everydayRelevanceHypothesis: ""
  targetPersonas: []
  sessionDurations: [2, 5, 10, 20, 30]

construct:
  primaryConstructId: ""
  secondaryConstructIds: []
  operationalDefinition: ""
  excludedInterpretations: []
  mechanismRationale: ""
  confounds: []
  evidenceLabels: []

gameplay:
  coreLoop: ""
  inputModality: "touch"
  tutorialFlow: ""
  practiceFlow: ""
  terminationRules: []
  interruptionRules: []
  failureStates: []
  antiFrustrationRules: []

content:
  source: procedural|controlled_bank
  generatorVersion: ""
  itemBankVersion: ""
  seedPolicy: ""
  solutionValidator: ""
  exposurePolicy: ""
  localizationRisks: []

difficulty:
  parameters: []
  levelDefinitions: []
  adaptationPolicyVersion: ""
  startingRule: ""
  promotionRule: ""
  demotionRule: ""
  explorationRule: ""
  ceilingFloorProtection: ""

measurement:
  modeEligibility: [TRAINING_ONLY]
  primaryMetrics: []
  secondaryMetrics: []
  timingContract: ""
  omissionRules: ""
  outlierRules: ""
  speedAccuracyPolicy: ""
  aggregationPolicy: ""
  officialEvidenceCap: ""
  calibrationRequirements: ""
  scoringVersion: ""
  uncertaintyMethod: ""

feedback:
  realtime: []
  sessionSummary: []
  deterministicNextActions: []
  prohibitedClaims: []

accessibility:
  requiredCapabilities: []
  supportedAdjustments: []
  alternativeMode: ""
  comparabilityImpact: ""
  screenReaderPolicy: ""
  reducedMotionPolicy: ""

privacy_security:
  requiredPermissions: []
  optionalPermissions: []
  rawSensitiveData: []
  localOnlyData: []
  retentionClass: ""
  integrityThreats: []

technical:
  engineModule: ""
  uiRoute: ""
  schemaVersion: ""
  offlinePolicy: ""
  syncIdempotencyKey: ""
  performanceBudget: ""
  featureFlag: ""

validation:
  unitTests: []
  propertyTests: []
  goldenCases: []
  uiTests: []
  accessibilityTests: []
  deviceMatrix: []
  humanPilotPlan: ""
  knownLimitations: []
  nextGate: ""
```

Los valores vacíos bloquean el gate correspondiente; no se rellenan con texto genérico.

---

## 6. Diseño cognitivo

### 6.1 Aislamiento suficiente

Todo ejercicio implica varias capacidades. El objetivo no es pureza imposible, sino demostrar que el mecanismo objetivo domina el resultado lo suficiente para el uso declarado.

El dossier debe responder:

- ¿Qué habilidad hace variar el éxito?
- ¿Qué estrategia alternativa permite acertar sin usarla?
- ¿Lectura, velocidad motora, audición o memoria incidental dominan el resultado?
- ¿Qué cambia al subir dificultad?
- ¿El ejercicio sigue midiendo lo mismo en todos los niveles?
- ¿Qué resultado refutaría el rationale?

### 6.2 Entrenamiento deliberado

Una mecánica de calidad:

- Hace visible el objetivo.
- Mantiene el reto en una zona útil.
- Proporciona feedback accionable.
- Varía estímulos y contextos.
- Reduce repetición mecánica sin reflexión.
- Permite recuperar de errores.
- Evita sobrecarga irrelevante.

### 6.3 Transferencia por diseño

No se promete transferencia por incluir una estética “realista”. Para mejorar plausibilidad:

- Variar representaciones y estímulos.
- Evitar una única estrategia superficial.
- Incluir tareas nuevas para near transfer.
- Relacionar el plan con tareas cotidianas concretas.
- Espaciar y mezclar cuando la evidencia lo aconseje.
- Evaluar fuera del ejercicio entrenado.

---

## 7. Diseño de gameplay

### 7.1 Core loop

El loop debe poder explicarse en una frase y aprenderse con acción, no con una pared de texto.

Secuencia preferida:

```text
objetivo breve
→ demostración
→ ensayo guiado
→ comprobación de comprensión
→ ronda real
→ feedback
→ siguiente acción
```

### 7.2 Tutorial

El tutorial debe:

- Usar estímulos distintos de la medición oficial.
- Exigir al menos una respuesta correcta demostrada.
- Permitir repetir sin penalización.
- No alimentar score.
- Ser reabrible.
- Adaptarse a accesibilidad y modalidad.

### 7.3 Error

El error se trata como información, no como fracaso moral.

- Feedback inmediato si no invalida la tarea.
- Explicación breve del patrón, no de la “inteligencia” del usuario.
- Sin animaciones humillantes.
- Sin pérdida manipulativa de racha.
- Sin convertir una mala sesión en diagnóstico.

### 7.4 Duraciones

Los planes admiten 2, 5, 10, 20 y 30 minutos.

- Una sesión corta puede centrarse en un bloque.
- Una sesión larga puede intercalar bloques o ejercicios.
- Cada ejercicio declara duración mínima útil y punto seguro de cierre.
- Una ronda no se corta en un estado que pierda evidencia o confunda al usuario.

### 7.5 Pausa e interrupciones

Definir:

- Si el temporizador se pausa.
- Si la ronda queda inválida.
- Cómo se recupera tras background/process death.
- Cómo se trata una llamada o cambio de audio.
- Qué se guarda localmente.

Nunca reconstruir un resultado oficial con eventos parciales ambiguos.

---

## 8. Contenido: procedural y bancos controlados

### 8.1 Entrenamiento gratuito

Los ejercicios Free deben funcionar offline y sin IA. La generación procedural será:

- Determinista con semilla.
- Validada antes de mostrar el ítem.
- Reproducible para tests y soporte.
- Suficientemente variada.
- Limitada por constraints formales.
- Capaz de producir solución única o criterio inequívoco.

### 8.2 Medición

Para medición oficial se usan:

- Bancos controlados, o
- Familias procedurales previamente calibradas.

No usar un generador no calibrado como si cada salida tuviera dificultad equivalente.

### 8.3 Separación de contenido

- Tutorial: conjunto público de enseñanza.
- Training: gran variación y feedback.
- Probes: formas controladas con exposición limitada.
- Reassessment: formas paralelas reservadas.

### 8.4 Validador de contenido

Todo generador debe probar:

- Solución válida.
- Ausencia de ambigüedad.
- Parámetros dentro de rango.
- Dificultad prevista coherente.
- No duplicación excesiva.
- Localización segura.
- Ausencia de contenido ofensivo o sensible accidental.

---

## 9. Dificultad adaptativa

### 9.1 Objetivo

Adaptar para entrenar, no para hacer subir el score.

### 9.2 Requisitos

- Estado inicial informado por calibración o ruta conservadora.
- Parámetros explícitos; no un único “nivel” opaco.
- Promoción y descenso con histéresis.
- Protección contra oscilación.
- Exploración periódica.
- Límites de seguridad y comprensión.
- Registro de cada decisión.
- Tests de transición input → output.

### 9.3 Variables permitidas

Según ejercicio:

- Número de elementos.
- Tiempo de exposición.
- Similaridad de distractores.
- Profundidad de regla.
- Número de cambios.
- Carga de memoria.
- Velocidad de secuencia.
- Ruido o interferencia.
- Complejidad semántica.

No mezclar parámetros hasta no saber qué constructo altera cada uno.

### 9.4 Contexto y fatiga

Fatiga declarada puede reducir dosis o reto del entrenamiento. No modifica score oficial ni facilita probes sin etiquetar el cambio de condición.

---

## 10. Métricas y scoring

### 10.1 Métricas por bloque

Cada ejercicio medirá todas las métricas relevantes y técnicamente defendibles de su subhabilidad, no una lista ornamental.

Posibles familias:

- Precisión y error por tipo.
- RT robusto en respuestas válidas.
- Variabilidad temporal.
- Omisiones y anticipaciones.
- Capacidad/carga máxima estimada.
- Eficiencia condicionada por dificultad.
- Flexibilidad tras cambio de regla.
- Calibración entre confianza y acierto.
- Sincronización temporal.
- Trayectoria o precisión motora.
- Estrategia observable.

### 10.2 Prohibiciones

- Un `score = accuracy × speed` sin rationale y simulación.
- Bonificación universal por rapidez.
- Penalización por accesibilidad.
- Recompensa cognitiva por XP, racha o pago.
- Sumar métricas correlacionadas como si aportaran evidencia independiente.
- Convertir ausencia en cero.
- Actualizar global con una partida.

### 10.3 Contrato temporal

Cada tarea con RT define:

- Evento exacto que inicia el reloj.
- Evento exacto que lo detiene.
- Reloj monotónico.
- Tratamiento de frame/animation delay.
- Anticipación mínima.
- Timeout.
- Hardware/modalidad elegibles.

### 10.4 Resultado local y oficial

La pantalla de cierre puede mostrar rendimiento de sesión. Debe distinguirlo del Performance Score longitudinal.

Ejemplo correcto:

> “Hoy resolviste 18 de 22 con una mediana de 740 ms. Tu perfil oficial sigue calibrándose.”

---

## 11. Feedback determinista

### 11.1 Contenido mínimo

El feedback Free debe ser útil sin IA:

- Resultado observable.
- Patrón principal.
- Comparación apropiada: contigo mismo y condiciones comparables.
- Una recomendación concreta.
- Límite de inferencia.

### 11.2 Motor de reglas

Las reglas deben ser:

- Versionadas.
- Mutuamente compatibles.
- Priorizadas.
- Testeadas con casos frontera.
- Localizables.
- Explicables.

Ejemplo estructural:

```yaml
ruleId: accuracy_high_rt_slow_v1
when:
  accuracyBand: HIGH
  rtBand: SLOW_FOR_PERSONAL_BASELINE
  evidenceQuality: [MEDIUM, HIGH]
then:
  insightKey: "precision_is_strong"
  actionKey: "reduce_hesitation_without_sacrificing_accuracy"
  claimLimitKey: "single_session_not_capacity_change"
```

### 11.3 Feedback durante medición

No revelar estrategias, dificultad de ítems o soluciones de formas reservadas. El feedback detallado llega después y de manera agregada.

---

## 12. Accesibilidad

### 12.1 Baseline obligatorio

- Contraste y escalado de texto.
- Targets táctiles adecuados.
- No depender solo de color, sonido o movimiento.
- Reduced motion.
- Hápticos configurables.
- Subtítulos/representaciones alternativas cuando el audio no sea el constructo.
- Orientaciones claras.
- Compatibilidad con lector de pantalla cuando sea viable.
- Tiempo razonable fuera de tareas donde el tiempo sea parte del constructo.

### 12.2 Validez de ajustes

Cada ajuste se clasifica:

- `COMPARABILITY_PRESERVED`.
- `SEPARATE_MODALITY`.
- `TRAINING_ONLY`.
- `NOT_APPLICABLE_CONSTRUCT_DEPENDENCY`.

Una alternativa no debe fingir medir el mismo constructo si elimina la habilidad objetivo.

### 12.3 Pruebas humanas

La alpha debe incluir deliberadamente diversidad de:

- Tamaños de pantalla.
- Habilidad digital.
- Visión/color.
- Audición si aplica.
- Motricidad si aplica.
- Nivel educativo y comprensión lectora.

---

## 13. Sensores y permisos

### 13.1 Principios

- Minimización.
- Permiso just-in-time.
- Valor comprensible.
- Alternativa sin permiso.
- Procesamiento local preferido.
- No almacenar señal cruda si bastan features.
- Borrado y retención definidos.

### 13.2 Ejemplos

- Micrófono: solo para ejercicios donde la respuesta vocal aporte valor; preferir extracción local y descarte de audio.
- Movimiento: solo coordinación o estabilidad con calibración de sensor.
- Cámara: fuera del MVP salvo caso extraordinariamente justificado.
- Hápticos: salida, no permiso sensible, siempre configurables.

### 13.3 Gate de privacidad

Todo ejercicio que toque datos sensibles requiere revisión de `privacy-security` en `/mb` y de `mindboost-security-privacy` o `mindverse-security-privacy` si el riesgo lo justifica.

---

## 14. Offline, sincronización e integridad

### 14.1 Offline first

Todos los ejercicios Free:

- Inician sin red después de instalar recursos necesarios.
- Guardan sesión localmente de forma transaccional.
- Usan identificador idempotente.
- Conservan versión y semilla.
- Reintentan sincronización sin duplicar.

### 14.2 Oficialidad diferida

Una sesión offline elegible queda `PROVISIONAL` hasta:

- Sincronizar eventos mínimos.
- Resolver duplicados.
- Validar clock/orden.
- Comprobar versión e integridad.

### 14.3 Conflictos

El servidor no debe usar last-write-wins para sobrescribir historia cognitiva. Los eventos son append-only o equivalentes; la proyección se recalcula de forma determinista.

---

## 15. Seguridad e integridad proporcional

El objetivo no es tratar a cada usuario como tramposo, sino proteger el significado de resultados compartibles.

Controles posibles:

- Nonces e IDs de sesión.
- Seeds firmadas para formas oficiales.
- Detección de duplicados.
- Límites de exposición.
- Validación de secuencias imposibles.
- Attestation solo si el riesgo lo exige.
- Separación training/ranking.

La acción ante sospecha será gradual:

1. Excluir evidencia dudosa.
2. Solicitar recalibración.
3. Restringir ranking, no entrenamiento.
4. Revisar casos persistentes.

Nunca hacer acusaciones automáticas basadas en un detector opaco.

---

## 16. Rendimiento y experiencia

Cada ejercicio define presupuesto para:

- Tiempo de arranque.
- Frame pacing.
- Jank.
- Memoria.
- Batería.
- Audio.
- Tamaño de recursos.
- Persistencia.

Para tareas temporales:

- Las animaciones no alteran el reloj.
- No se aceptan frames perdidos sistemáticos.
- Se registra calidad técnica.
- Se degrada a training-only si no puede medirse con fiabilidad.

Dirección estética aprobada: híbrido laboratorio + cósmico + cinematográfico contenido. La estética nunca oscurece reglas, estímulos, contraste o feedback.

---

## 17. Arquitectura mínima

El motor cognitivo en `commonMain` debe ser puro cuando sea razonable:

```text
ExerciseConfig + Seed + PriorState + UserAction
    → StateMachine
    → NewState + DomainEvents + DeterministicOutcome
```

Separar:

- Lógica de ejercicio.
- Reloj/plataforma.
- Renderizado Compose.
- Persistencia.
- Scoring longitudinal.
- Telemetría.
- Contenido.

La UI no recalcula puntuaciones y el backend no reinterpreta eventos sin versión.

---

## 18. Taxonomía de eventos

Eventos mínimos según ejercicio:

```text
ExerciseOpened
TutorialStarted
TutorialCompleted
SessionStarted
TrialPresented
ResponseStarted
ResponseSubmitted
TrialResolved
DifficultyChanged
SessionPaused
SessionResumed
SessionInterrupted
SessionCompleted
SessionInvalidated
ResultPersisted
ResultSynced
```

Cada evento incluye únicamente lo necesario:

- IDs y versiones.
- Tiempo monotónico relativo.
- Estado/dificultad.
- Resultado estructurado.
- Contexto técnico aprobado.
- Consent/analytics route correspondiente.

No enviar contenido o respuestas sensibles a analytics general si no son necesarios.

---

## 19. Matriz mínima de pruebas

### 19.1 Motor

- Transición válida por acción.
- Acción inválida.
- Inicio/fin.
- Pausa/interrupción.
- Seed reproducible.
- Solución correcta.
- Dificultad en límites.
- Ningún estado imposible.

### 19.2 Scoring

- Todos correctos.
- Todos incorrectos.
- Omisiones.
- Anticipaciones.
- Timeout.
- Velocidad alta con baja precisión.
- Lentitud con precisión.
- Cambio de dificultad.
- Outliers.
- Datos faltantes.
- Duplicados.

### 19.3 Property-based

Ejemplos:

- Un ítem validado siempre tiene solución.
- Repetir la misma seed/config produce el mismo contenido.
- Métricas permanecen en su dominio.
- Una sesión duplicada no cambia la proyección.
- XP no altera Performance Score.
- Training-only no altera score oficial.

### 19.4 UI

- Tutorial nuevo y repetido.
- Orientación/tamaños.
- Fuente grande.
- Dark/light si se soporta.
- Reduced motion.
- Lector de pantalla.
- Back/background/process death.
- Sin red.
- Permiso denegado.
- Idioma y texto largo.

### 19.5 Dispositivos

Mínimo de lanzamiento:

- Android 8 y versiones objetivo superiores.
- iOS 15 y versiones objetivo superiores.
- Pantalla pequeña/media/grande.
- 60 Hz y alta tasa cuando se soporte.
- Gama baja y gama representativa.
- Audio interno/Bluetooth para tareas auditivas.

### 19.6 Tests humanos

- Comprensión sin ayuda.
- Detección de estrategia superficial.
- Frustración/aburrimiento.
- Mareo o sobrecarga.
- Accesibilidad.
- Duración real.
- Interpretación del feedback.

---

## 20. Gates de entrada y salida

### 20.1 Antes de implementar

`efficacy-guardian` y `psychometrician` deben aprobar:

- Constructo.
- Mecánica.
- Métricas.
- Confounds.
- Dificultad.
- Claim permitido.

### 20.2 Antes de declarar `TRAINING_READY`

- Dossier completo.
- Motor y UI completos.
- Contenido válido.
- Offline.
- Accesibilidad.
- Tests verdes.
- Paridad.
- No P0/P1.

### 20.3 Antes de medición provisional

- Carril separado.
- Versionado y exposición.
- Integridad.
- Blueprint.
- UI provisional.
- Plan de análisis.

### 20.4 Antes de medición oficial

- Evidencia humana suficiente.
- Fiabilidad/error adecuados al uso.
- Comparabilidad.
- Práctica/dificultad controladas.
- Auditoría científica y de diff.
- ADR aprobado.

---

## 21. Definition of Done por ejercicio

Un ejercicio cuenta como terminado para su estado solo cuando:

- Está conectado desde catálogo/plan hasta persistencia y feedback.
- No existen mocks, placeholders, rutas muertas ni TODO críticos.
- El resultado se puede reproducir desde versión/seed/eventos.
- La solución y scoring superan golden tests.
- La dificultad responde a la política definida.
- Offline y sync son idempotentes.
- Funciona en ambas plataformas objetivo.
- Accesibilidad y permisos tienen fallback.
- Telemetría respeta consentimientos.
- Claims y UI coinciden con evidencia.
- Documentación, catálogo y Decision Log están actualizados.
- Gate de salida de `/mb` aprobado.
- No quedan P0/P1 ni una mejora segura, relevante y dentro del alcance sin implementar.

---

## 22. Retirada y migración

Retirar o degradar un ejercicio cuando:

- Mide principalmente un confound.
- Tiene fiabilidad insuficiente para su uso.
- Presenta sesgo no corregible.
- Su contenido se agota.
- Tiene bug de scoring no puenteable.
- La experiencia daña accesibilidad o seguridad.

Proceso:

1. Desactivar nuevas evidencias oficiales.
2. Conservar historial versionado.
3. Marcar resultados afectados.
4. Ofrecer sustituto de constructo equivalente.
5. Recalcular solo con linaje y transparencia.
6. Registrar ADR y comunicación.

---

## 23. Catálogo maestro

El repositorio debe mantener una tabla generada o verificable:

| exerciseId | bloque | subhabilidad primaria | versión | estado | offline | permisos | measurement | evidencia | owner |
|---|---|---|---|---|---:|---|---|---|---|

Checks automáticos:

- Al menos 15 `TRAINING_READY` por bloque para lanzamiento.
- IDs y versiones únicos.
- Dossiers presentes.
- Todos los recursos localizables.
- Toda ruta de catálogo resoluble.
- Ningún ejercicio oficial con gate vencido.
- Ningún bloque dependiente de permiso sensible.

---

## 24. Checklist de auditoría rápida

Antes de aprobar un ejercicio, responder sí con evidencia:

- ¿Sé exactamente qué pretende entrenar?
- ¿Sé qué no permite inferir?
- ¿La solución siempre es correcta y no ambigua?
- ¿La dificultad cambia el reto previsto?
- ¿La adaptación puede explotarse?
- ¿La métrica separa precisión, velocidad y dificultad?
- ¿La UI distingue sesión y perfil?
- ¿Hay estímulos nuevos y control de exposición?
- ¿Funciona sin red y sin IA?
- ¿Funciona sin permisos sensibles o tiene alternativa?
- ¿Es accesible y honesto sobre modalidades?
- ¿Los eventos se pueden reconstruir?
- ¿La sincronización no duplica?
- ¿Los tests cubren casos adversarios?
- ¿Android e iOS tienen paridad?
- ¿El claim coincide con el estado científico?

Un “no” no siempre bloquea entrenamiento, pero obliga a degradar estado y registrar el trabajo restante.

---

## 25. Regla final

La cantidad mínima de 120 no justifica 120 mini-juegos superficiales.

MindBoost gana si cada ejercicio añade una combinación clara de:

- Cobertura cognitiva.
- Entrenamiento deliberado.
- Variación significativa.
- Medición útil.
- Transferencia plausible y comprobable.
- Experiencia excelente.

Si dos ejercicios solo cambian la estética, cuentan como una misma familia a efectos de cobertura científica, aunque sean dos experiencias de producto.
