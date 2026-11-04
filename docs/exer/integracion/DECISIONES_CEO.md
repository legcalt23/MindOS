# Molvex · Cinco bloqueadores del CEO

**Estado de todas las propuestas:** `PROPOSED` · **Implementación:** `NOT_AUTHORIZED` por este encargo EXER. Una respuesta sobre una fila no aprueba las demás. Los IDs canónicos siguen en el [ledger](../../../MINDBOOST_AUDIT_LEDGER.md).

## Resumen de decisiones

| ID de propuesta | Decisión concreta | Recomendación conceptual | Qué sigue pendiente |
|---|---|---|---|
| EXER-CEO-01 · `0166`/`0116` | Default/RC de lifecycle y acceso a entrenamiento | Mantener `false` en release hasta evidencia física y diseñar práctica accesible sin base admitida, conservando gates de medida | Decisión de producto, futura implementación y test de usuario nuevo; verificación RC real |
| EXER-CEO-02 · `0174` | Política de base y paso a calibrado | Base inicial provisional; mantener §11.1 completo para madurez | Estimador/reevaluación/versionado y su posterior mecanización |
| EXER-CEO-03 · `0168` | Mantener o levantar C0 adaptativo | Mantener C0; diseñar productores reales y criterios de salida antes de activación | Fuentes de señal, pruebas de ausencia, oscilación y sesgos por familia |
| EXER-CEO-04 · `0155` | Valor y alcance de `EXECUTION_AUTHORIZATION` | No escoger por el CEO. Registrar GRANTED con alcance preciso o NOT_GRANTED | Declaración explícita y posterior reconciliación documental; el gate sigue FAIL |
| EXER-CEO-05 · `0101` | Ejecución humana en dispositivo físico | Completar protocolo Android existente antes de considerar activación release | Dispositivo/humano, build/flag verificados y evidencia de ambas corridas; iOS separado |

## EXER-CEO-01 · Acceso y sonda de ciclo de vida

**Actual:** `DefaultFeatureFlags.focusLifecycleProbeEnabled=false` ([fuente](../../../shared/src/commonMain/kotlin/com/mindos/app/core/config/FeatureFlags.kt)); Home exige base y la base rechazada persiste `null`. iOS/Desktop usan NoOp incluso con flag activo. El runbook **reporta** RC v1 acotado a debug; la consola publicada no se inspeccionó ahora.

| Opción | Cambio y beneficio | Coste/riesgo | Reversibilidad |
|---|---|---|---|
| A · Recomendada | Mantener release OFF; diseñar acceso a práctica desde el inicio, con medida retenida cuando no cumpla sus gates | Requiere cambios futuros de navegación y separación de finalidad/consumidores; no cierra hoy `0166` | Alta en diseño; historial y consumidores exigen revisión antes de código |
| B | Tras `0101`, planificar RC release acotado y rollback, conservando el default conservador | Solo resuelve sensor Android; no arregla NoOp en otros targets ni el resto del motor | RC reversible tras activación efectiva; no borra evidencia ya producida |
| C | Cambiar el default compilado a `true` | Mayor alcance del cambio; sigue sin fuente iOS/Desktop y requiere validación/rollout | Otro build o RC para revertir |
| D | Mantener todo como hoy | El usuario nuevo sigue bloqueado bajo los defaults | Sin cambio |

**Pregunta:** ¿apruebas A como dirección de diseño y dejas cualquier activación release condicionada a `0101`, o eliges B/C/D? No es autorización para publicar RC ahora.

**Cierre futuro:** decisión en canon, test de ruta de usuario nuevo bajo la configuración elegida, aislamiento de prácticas no medibles y evidencia física requerida por `0166`/`0101`.

## EXER-CEO-02 · Base provisional y calibración

**Actual:** [charter §11.1](../../context_pack/03_SCIENTIFIC_MEASUREMENT_CHARTER.md) exige **5 sesiones oficiales válidas / 3 días / 48 h**, cobertura, integridad/comparabilidad y ninguna dependencia crítica de una tarea/forma para pasar a calibrado. No prohíbe por sí mismo un punto inicial provisional.

**Problema del informe:** D‑1A propone tres administraciones y dos días y las equipara al charter. Esa equivalencia no se sostiene por el texto. El conteo de cinco tampoco demuestra fiabilidad o eficacia ni debe implementarse mediante circularidad: hay que definir qué evidencia puede acumularse para la madurez sin exigir previamente que el perfil ya esté calibrado.

| Opción | Cambio y beneficio | Coste/riesgo | Reversibilidad |
|---|---|---|---|
| A · Recomendada | Primer registro provisional; promoción solo por §11.1 completo y criterios adicionales aplicables. Diseñar reevaluación y versionado por separado | Más evidencia y estado explícito; el estadístico de base aún requiere contrato y validación | Alta como propuesta; preservación/versionado de historia necesarios al implementar |
| B | Mantener una administración con re-baseline manual, sin llamarla calibrada ni habilitar usos oficiales | Menor fricción; mayor variabilidad y no cumple por sí sola el gate de madurez | Alta con series separadas |
| C | Cambiar formalmente el charter para otra política | Requiere decisión explícita anterior al código; no transforma menor evidencia en validación científica | Reversible en política, no vuelve comparables datos incompatibles |

**Pregunta:** ¿apruebas A como contrato de diseño, conservando el mínimo completo y dejando el estimador y la política de re-baseline para propuesta específica?

**Cierre futuro:** fronteras de conteo/tiempo/cobertura, datos insuficientes, cambios de versión/dispositivo y ausencia tipada; una sola administración no otorga estado calibrado.

## EXER-CEO-03 · Contención C0 adaptativa

**Actual:** el [call-site](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/presentation/FocusSessionStateHolder.kt) envía tres señales a `null`. [AdaptiveModels](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/adaptive/AdaptiveModels.kt) declara esa ausencia intencional. [AdaptiveEngine](../../../shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/adaptive/AdaptiveEngine.kt) mantiene dificultad en la rama de rendimiento; la ruta anterior de `ContainImpulsivity` exige revisión por familia.

| Opción | Cambio y beneficio | Coste/riesgo | Reversibilidad |
|---|---|---|---|
| A · Recomendada | Mantener C0 y especificar productores de fatiga, dominio por eje e histéresis con procedencia | No desbloquea adaptación ahora; permite definir señales sin fabricar usuarios sanos | Alta |
| B | Aprobar un diseño de levantamiento gradual, solo para familias y señales verificadas | Contratos, límites por decisión, pruebas de ida/vuelta, observación y revisión antes de ejecución | Controlable con versionado y rollback explícitos |
| C | Sustituir ausencias por constantes para activar ramas | Fabricaría observaciones. No es una solución compatible con el contrato de evidencia | No recomendada; no debe presentarse como medición |

**Pregunta:** ¿mantienes A o autorizas diseñar B con alcance por familia? Una autorización de diseño no activa ninguna rama.

**Cierre futuro:** valores producidos y consumidos de verdad; evidencia insuficiente sigue retenida; pruebas de sensibilidad, histéresis, límites y ausencia de escalada unidireccional no deseada.

## EXER-CEO-04 · Desacuerdo de autoridad V5

**Hecho reproducido:** `npm run check:governance` falla por `CEO_FIELD_DISAGREEMENT`:

- [ADR_REGISTER_V5.md](../../engines/focus_v5/ADR_REGISTER_V5.md): `EXECUTION_AUTHORIZATION = NOT_GRANTED`.
- [CURRENT_AUTHORITY_INDEX.md](../../engines/focus_v5/CURRENT_AUTHORITY_INDEX.md): `GRANTED (scope plan V5, T-DR por slice)`; el mismo índice mantiene `MOUNT=BLOCKED` y R3 candidata.

**Opciones:** A) declarar `GRANTED` únicamente para un plan/slices y gates expresos, manteniendo montaje/release bloqueados; B) declarar `NOT_GRANTED` para ejecución V5 hasta nueva autorización. Ambas requieren procedencia y reconciliación posterior de las autoridades vivas. Elegir un valor para que el test pase ocultaría la decisión pendiente.

**Pregunta:** ¿cuál es el valor vigente y su alcance exacto? Si eliges A, indica el plan/slices autorizados. Este encargo EXER de diseño no resuelve esa pregunta por implicación.

**Cierre futuro:** declaración CEO registrable, documentos consistentes con ella y `check:governance` verde sin debilitar el validador. Las barreras de montaje, revisión humana y release continúan según su propia evidencia.

## EXER-CEO-05 · Corrida física humana

Usar el [runbook existente](../../audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md), cuyo estado indica emulador con guiones y evidencia parcial. No se ha observado aquí una corrida humana nueva.

**Resultado exigido en Android:** build y hash identificados; activación efectiva del flag verificada antes de la base; sesión E1 sin pérdida de primer plano que llegue a `Admitted`; segunda sesión con salida a segundo plano, retorno y finalización que llegue a `NotMeasurable(INTERRUPTED)`. El gate del ledger también nombra llamada entrante: conservar esa cobertura al preparar la corrida, no sustituirla silenciosamente por abandono o por un guion.

Registrar dispositivo/API, app/build, HEAD/huella del árbol, APK SHA‑256, fechas UTC, parámetro activo y nota final literal de cada corrida. No incluir credenciales ni datos personales en el repo. Los registros actuales de emulador no se reclasifican como físicos.

**Opciones:** A) realizar la prueba con el fundador u otro humano designado y dispositivo autorizado; B) diferirla, manteniendo `0101` abierto. El coste es tiempo humano y disponibilidad de dispositivo. El riesgo principal es tomar una prueba del sensor como validación de timing o eficacia; su alcance no permite esa inferencia. La corrida no resuelve `0155` ni cubre iOS.

**Pregunta:** ¿se programa la ejecución Android con dispositivo y humano designados o queda diferida? No se ha creado recordatorio ni iniciado estudio; falta una instrucción operativa para esa fase.

## Activación EXER · decisión independiente

Los archivos están preparados en `docs/exer`. La documentación oficial respalda skills y menciones explícitas; no se ha observado un registro nativo arbitrario `/exer` en este cliente. Véase [ENTREGA.md](ENTREGA.md).

**Aprobación recibida el 2026-09-15:** «Sí, añade el alias conversacional /exer». Se conservó `/exer` como alias conversacional de proyecto mediante el bloque aditivo de `AGENTS.md` que carga `docs/exer/SKILL.md`. Falta probarlo en una nueva interacción. Esta aprobación no resuelve ninguna de las cinco decisiones de producto anteriores.
