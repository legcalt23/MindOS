# MindBoost — Post-Phase Review Protocol

**Versión:** 1.0.0  
**Fecha:** 2026-07-10  
**Owner:** CEO Copilot  
**Estado:** APPROVED  
**Uso:** auditoría independiente de cada mini-fase de Claude y generación del prompt de corrección

---

## 1. Mandato

Después de cada fase, ChatGPT debe actuar como un comité ejecutivo-técnico-científico independiente cuyo único interés es elevar el resultado real de MindBoost.

No se limita a responder si el trabajo “parece bien”. Debe:

- Reconstruir el objetivo y el cambio real.
- Verificar cada afirmación contra archivos/evidencia disponible.
- Detectar defectos, omisiones, contradicciones y mejoras pequeñas.
- Evaluar efectos de segundo orden.
- Distinguir código correcto de producto válido.
- Convertir todo hallazgo accionable en un único prompt perfecto para Claude.

Resultado esperado:

> Claude recibe una especificación de reapertura que le obliga a investigar, corregir, integrar y demostrar el cierre sin depender de contexto implícito.

---

## 2. Cuándo se ejecuta

Este protocolo se activa cuando el CEO aporta uno o más de:

- Resumen final de Claude.
- Archivos creados/modificados.
- Diff.
- Cierre de slice.
- Logs de tests/gates.
- Plan maestro o contrato de fase.

No se activa durante cada microdecisión de implementación salvo solicitud expresa. El valor principal es el contexto fresco post-fase.

---

## 3. Paquete ideal de entrada

### 3.1 Requerido

1. Objetivo original de la fase.
2. Resumen final íntegro de Claude.
3. Archivos modificados completos o diff.
4. Tests ejecutados y resultados.

### 3.2 Muy recomendable

- Plan maestro/slice contract.
- Callers y consumers de contratos modificados.
- Schemas/migraciones.
- Capturas o vídeo si cambia UX.
- Agentes usados y veredictos.
- Baseline anterior.
- Follow-ups declarados.
- Commit SHA y estado del worktree.

### 3.3 Si falta evidencia

ChatGPT no paraliza una auditoría útil. Debe:

- Revisar lo disponible.
- Etiquetar lo no verificable.
- Reducir la fuerza del veredicto.
- Incluir en el prompt a Claude los comandos/archivos que deben aportar la evidencia faltante.

Solo pregunta al CEO si la información ausente representa una elección empresarial irreducible o nueva autorización.

---

## 4. Modelo de evidencia

Cada afirmación de Claude se clasifica:

| Estado | Criterio |
|---|---|
| `VERIFIED` | Se observa directamente en archivos/diff o salida reproducible aportada. |
| `REPORTED` | Claude la declara, pero no hay comprobación directa suficiente. |
| `INFERRED` | ChatGPT la deduce de evidencia parcial. |
| `NOT_VERIFIED` | No se puede sostener. |
| `CONTRADICTED` | Los archivos/evidencia la contradicen. |

Para ciencia/claims usar además:

- `SUPPORTED`.
- `HYPOTHESIS`.
- `UNKNOWN`.
- `CONFLICTED`.

Un test reportado sin output/scope es `REPORTED`. Un archivo con lógica no cableada no verifica una feature de usuario.

---

## 5. Veredictos

### `APROBADA`

La fase cumple su estado y DoD; no quedan P0/P1 ni mejoras relevantes in-scope sin aplicar.

### `REOPEN`

Hay defectos o mejoras que deben implementarse antes de cerrar. Es el veredicto normal cuando existe cualquier P0/P1 o trabajo valioso dentro del scope.

### `BLOCKED`

No se puede avanzar de manera segura sin un prerequisito, evidencia o autorización externa.

### `CONDITIONAL`

El contrato inerte puede aceptarse bajo barreras mecánicas, pero no cablearse/publicarse hasta condiciones explícitas. No debe usarse para disfrazar una feature incompleta.

---

## 6. Severidad y prioridad

### P0 — bloqueante inmediato

- Exposición/pérdida de datos o credenciales.
- Score falso presentado como válido.
- Corrupción irreversible.
- Bypass de auth/autorización.
- Claim clínico/peligroso.
- Función crítica inutilizable.
- Publicación con auth temporal insegura.

### P1 — bloquea cierre o wiring

- Contradicción de fuente de verdad.
- Algoritmo incorrecto o no comparable.
- Falta de test crítico.
- Integración no demostrada.
- Accesibilidad que cambia el constructo sin marcar.
- Migración ausente.
- Camino reproducible a falso progreso.
- Plataforma objetivo no verificada.

### P2 — mejora importante

- Robustez, UX, mantenibilidad o ciencia significativamente mejorables.
- Riesgo contenido por barrera mecánica y fuera del wiring activo.

### P3 — refinamiento

- Claridad, micro-UX, consistencia, eficiencia o documentación.
- Se implementa si es segura, relevante e in-scope.

### 6.1 Orden real

La severidad expresa daño; el orden de implementación también considera dependencia. Una corrección P2 habilitadora puede preceder a un P1 si es técnicamente necesaria.

---

## 7. Procedimiento de auditoría

### Paso 1 — reconstruir intención

Extraer:

- Resultado para el usuario.
- Resultado técnico.
- Resultado científico.
- Alcance y no-alcance.
- Estado prometido: diseño, contrato, implementado, cableado, validado o release-ready.

Si Claude redefine el objetivo durante el cierre, señalar scope drift.

### Paso 2 — leer fuentes de verdad

Aplicar precedencia de `00_CONTEXT_INDEX.md`.

Leer al menos:

- PRD relevante.
- Charter científico si toca métricas/ejercicios.
- Exercise Standard si toca catálogo.
- Technical Current State.
- Decision Log/ADR.
- Plan/slice.

### Paso 3 — inventariar claims

Crear mentalmente o explícitamente una tabla:

| Claim de Claude | Evidencia | Estado | Riesgo si es falso |
|---|---|---|---|

Claims típicos:

- “Completo”.
- “No cableado”.
- “Offline”.
- “Cross-platform”.
- “Score honesto”.
- “Tests verdes”.
- “Sin regresiones”.
- “Seguro”.
- “Accesible”.

### Paso 4 — trazar el sistema

Para cada cambio, seguir:

```text
entrada
→ dominio
→ persistencia
→ sync/backend
→ proyección
→ UI/feedback
→ analítica
→ borrado/export
```

Buscar writers, readers, callers, consumers, defaults, mocks, flags y rutas antiguas.

### Paso 5 — buscar contradicciones

Contrastar:

- Resumen vs código.
- Código vs tests.
- Tests vs acceptance criteria.
- UI vs estado científico.
- Schema nuevo vs datos antiguos.
- Android vs iOS.
- Online vs offline.
- Free vs Elite.
- Documento vs implementación.

### Paso 6 — adversarial review

Preguntar:

- ¿Cómo podría producir falso progreso?
- ¿Cómo podría un dato válido convertirse en inválido o viceversa?
- ¿Qué pasa al repetir, duplicar, reordenar o perder eventos?
- ¿Qué pasa con cero datos, un dato, muchos datos o datos extremos?
- ¿Qué pasa al cambiar versión, dispositivo, hora o zona?
- ¿Qué pasa sin red, permiso, suscripción o consentimiento?
- ¿Qué pasa al cerrar app a mitad?
- ¿Qué pasa si la IA devuelve basura o excede coste?
- ¿Qué puede interpretar mal el usuario?

### Paso 7 — buscar mejoras

No terminar al encontrar bugs. Evaluar:

- Precisión.
- Simplicidad.
- Experiencia.
- Accesibilidad.
- Observabilidad.
- Coste.
- Reversibilidad.
- Documentación.
- Cobertura de tests.
- Transferencia y valor cotidiano.

### Paso 8 — decidir y convertir en tareas

Cada hallazgo debe incluir:

- Severidad.
- Archivo/componente.
- Evidencia.
- Por qué importa.
- Cambio esperado.
- Test/gate de aceptación.
- Dependencias.

### Paso 9 — seleccionar agentes

Usar `/mb` y nombrar agentes adicionales exactos solo cuando aporten un resultado concreto.

### Paso 10 — redactar prompt autocontenido

El prompt debe permitir a Claude actuar sin preguntarle al CEO detalles ya decididos.

### Paso 11 — autocrítica

Antes de entregar, ejecutar el checklist de sección 19.

---

## 8. Matriz de trazabilidad obligatoria

La auditoría debe mapear:

| Requisito/criterio | Evidencia aportada | Estado | Hallazgo/acción |
|---|---|---|---|

Estados:

- `PASS`.
- `PARTIAL`.
- `FAIL`.
- `NOT_TESTED`.
- `NOT_APPLICABLE` con razón.

No aceptar “cubierto” sin referencia a archivo, test o output.

---

## 9. Checklist científico y psicométrico

Si la fase toca un ejercicio, métrica, perfil o plan:

- ¿Constructo y no-constructo están definidos?
- ¿La mecánica exige el mecanismo objetivo?
- ¿Confounds dominan el resultado?
- ¿Training y measurement están separados?
- ¿Dificultad y exposición están controladas?
- ¿Velocidad y precisión se modelan honestamente?
- ¿Ausencia permanece ausencia?
- ¿Una sesión puede mover demasiado el perfil?
- ¿Práctica se confunde con capacidad?
- ¿El algoritmo usa todas las observaciones relevantes de forma robusta?
- ¿Se cuantifica error/cobertura/frescura?
- ¿Data sufficiency se confunde con confidence?
- ¿Versiones incompatibles se mezclan?
- ¿Contexto subjetivo altera score oficial?
- ¿XP/racha/suscripción contaminan score?
- ¿El claim supera la evidencia?
- ¿Existe evidencia de transferencia o solo plausibilidad?
- ¿Hay ceiling/floor o sesgo de dispositivo/grupo?

Exigir simulación adversaria para fórmulas nuevas.

---

## 10. Checklist de producto y UX

- ¿El usuario entiende qué hacer en segundos?
- ¿El flujo entrega valor completo?
- ¿La pantalla distingue sesión, perfil y progreso?
- ¿La próxima acción es clara?
- ¿El feedback es directo, exigente y constructivo?
- ¿Hay falsa precisión o lenguaje absoluto?
- ¿El plan funciona sin IA en Free?
- ¿Rechazar un ejercicio ofrece alternativa?
- ¿Las duraciones 2/5/10/20/30 se comportan bien?
- ¿Descanso planificado preserva racha?
- ¿No se bloquea exceso de práctica, pero se limita evidencia oficial?
- ¿El dashboard prioriza acción sobre decoración?
- ¿La estética reduce claridad o timing?
- ¿Estados vacíos/error/loading/offline están diseñados?
- ¿La función funciona para novatos?
- ¿Hay dark patterns, presión o culpa?
- ¿El usuario controla perfil, permisos y datos?

---

## 11. Checklist de arquitectura/KMP

- ¿La lógica pura está en `commonMain`?
- ¿Hay API de plataforma filtrándose al dominio?
- ¿Existen autoridades duplicadas?
- ¿La UI calcula dominio?
- ¿El modelo Firestore contamina dominio?
- ¿Las dependencias apuntan hacia dentro?
- ¿El cambio compila realmente en Android e iOS?
- ¿Hay comportamiento divergente por plataforma?
- ¿Los relojes y dispatchers son inyectables?
- ¿La state machine es determinista?
- ¿Hay paths antiguos todavía activos?
- ¿La feature flag tiene default seguro?
- ¿La refactorización preserva cambios del usuario?
- ¿Se añadió dependencia innecesaria o incompatible?

---

## 12. Checklist de datos, offline y migración

- ¿Cada sesión tiene ID idempotente?
- ¿Reintento duplica?
- ¿Out-of-order cambia resultado?
- ¿Process death pierde o duplica?
- ¿Offline queda provisional hasta integridad?
- ¿Conflictos usan last-write-wins de forma peligrosa?
- ¿Schema y versión están persistidos?
- ¿Datos antiguos migran o fallan cerrado?
- ¿Dual-read/write tiene fin y rollback?
- ¿Export incluye los nuevos datos?
- ¿Delete los elimina en local, cloud, analytics y backups según política?
- ¿La retención está definida?
- ¿Se conservan valores originales al recalcular score?
- ¿Los tests usan emulator/fixtures realistas?

---

## 13. Checklist de privacidad, seguridad e identidad

- ¿La finalidad y base/consentimiento son correctos?
- ¿Se minimizan datos?
- ¿Sueño/estrés/energía/fatiga siguen locales?
- ¿Pseudónimo se trata como dato personal?
- ¿Hay acceso cruzado posible?
- ¿Reglas default deny?
- ¿Logs o analytics contienen secretos/datos cognitivos?
- ¿Se separan loginUsername y displayName?
- ¿Auth de email sintético está bloqueada en release?
- ¿Recuperación evita preguntas personales?
- ¿Tokens, sesiones y revocación son seguros?
- ¿Permiso sensible tiene alternativa?
- ¿La IA recibe solo lo necesario y con entitlement?
- ¿Prompts/respuestas tienen retención y borrado?
- ¿Se validan outputs de IA?
- ¿Un atacante puede forzar coste o ranking?

Todo hallazgo de seguridad se describe sin exponer un secreto real en la respuesta.

---

## 14. Checklist Elite AI y negocio

- ¿Free funciona completamente sin IA?
- ¿Entitlement se verifica server-side?
- ¿El modelo solo reorganiza componentes validados?
- ¿El usuario confirma el cambio de plan?
- ¿La memoria es estructurada, visible, editable y borrable?
- ¿Existe fallback determinista?
- ¿Se controlan cuota, concurrencia, retries y circuit breaker?
- ¿El coste esperado mantiene margen positivo con 2 €/mes?
- ¿No existe una prueba gratuita de IA no aprobada?
- ¿No se han introducido features pay-to-score?
- ¿Proveedor/modelo/prompt están versionados?
- ¿Errores y safety refusals tienen UX segura?

---

## 15. Checklist de accesibilidad y equidad

- ¿Contraste, targets, escalado y reduced motion?
- ¿Se depende solo de color/audio/motricidad?
- ¿Screen reader es viable o se declara modalidad?
- ¿Un ajuste cambia el constructo?
- ¿Textos caben y son comprensibles?
- ¿Hay alternativas sin sensores?
- ¿Gama baja y pantalla pequeña funcionan?
- ¿Idioma/educación introducen sesgo no relevante?
- ¿El feedback evita etiquetas de capacidad esencialistas?
- ¿Se planificó análisis de DIF/device effects?

---

## 16. Checklist de tests y gates

- ¿Los tests fallarían antes del cambio?
- ¿Prueban outputs, no solo que no crashea?
- ¿Hay golden inputs → outputs?
- ¿Hay property-based tests donde aplica?
- ¿Casos n=0, n=1, boundary, NaN, infinito, duplicado y orden?
- ¿Tests de tiempo usan reloj controlado?
- ¿Tests de sync prueban retries/duplicados?
- ¿Tests de migración usan datos de versión anterior?
- ¿Tests UI cubren offline, permisos, background y accesibilidad?
- ¿Android e iOS se ejecutaron?
- ¿Se reportan tests skipped/quarantined?
- ¿Static analysis fue completo o incremental?
- ¿El scope del comando coincide con el claim “todo verde”?
- ¿Exit gates revisaron el diff posterior a la última corrección?

---

## 17. Catálogo de edge cases

Aplicar los relevantes:

### Datos

- Lista vacía.
- Un elemento.
- Máximo volumen.
- Duplicados exactos/parciales.
- Orden invertido.
- Timestamps iguales.
- Zona horaria/DST/medianoche.
- Reloj del dispositivo alterado.
- Datos antiguos o corruptos.

### Sesión

- Tutorial abandonado.
- Background a mitad.
- Llamada/route de audio cambia.
- Rotación/recomposición.
- Process death.
- Sin espacio.
- Sin red o red intermitente.
- Permiso denegado/revocado.

### Scoring

- Todo correcto/incorrecto.
- Muy rápido con azar.
- Muy lento y perfecto.
- Omitir para evitar penalización.
- Cambiar dificultad estratégicamente.
- Repetir ítems.
- Score cerca de umbral/rango.
- Cambio de versión/dispositivo.
- Inactividad prolongada.

### Cuenta/IA

- Username colisionado.
- Dispositivo perdido sin canal vinculado.
- Recovery code usado dos veces.
- Suscripción expira durante petición.
- Respuesta IA inválida/vacía/peligrosa.
- Timeout, retry y doble cobro de tokens.
- Usuario borra memoria mientras hay request.

---

## 18. Formato de respuesta post-fase

ChatGPT entrega, en este orden:

### 18.1 Veredicto ejecutivo

Una frase con `APROBADA`, `REOPEN`, `BLOCKED` o `CONDITIONAL` y razón principal.

### 18.2 Límites de evidencia

Qué se leyó y qué no se puede comprobar.

### 18.3 Fortalezas verificadas

Solo las que importan y con evidencia.

### 18.4 Trazabilidad

Tabla requirement → evidencia → estado.

### 18.5 Hallazgos

Orden P0, P1, P2, P3. Cada uno:

```text
[ID] [Severidad] Título
Evidencia:
Impacto:
Causa:
Corrección:
Aceptación:
```

### 18.6 Contradicciones y decisiones

Documentos/fórmulas/claims a actualizar.

### 18.7 Routing

Agentes exactos y por qué.

### 18.8 Prompt íntegro para Claude

Bloque autocontenido y listo para pegar.

### 18.9 Criterio de cierre

Qué evidencia permitirá aprobar la reapertura.

---

## 19. Checklist de autocrítica de ChatGPT

Antes de responder:

- ¿He leído todas las fuentes relevantes?
- ¿Separé reportado de verificado?
- ¿Seguí callers y consumers, no solo diff?
- ¿Busqué un camino a falso progreso?
- ¿Diferencié exactitud técnica de validez científica?
- ¿Evalué offline, versiones y migración?
- ¿Evalué Android e iOS?
- ¿Evalué privacidad, auth y borrado?
- ¿Evalué accesibilidad como validez?
- ¿Evalué coste Elite?
- ¿Busqué P3 seguros in-scope?
- ¿Evité scope creep sin dejar riesgos invisibles?
- ¿El prompt contiene hechos, tareas, tests y gates?
- ¿Nombré agentes adicionales exactos?
- ¿Claude puede ejecutarlo sin volver a preguntar lo ya decidido?
- ¿La solución es reversible y documentada?

Si alguna respuesta es no, revisar antes de entregar.

---

## 20. Investigación externa

ChatGPT debe buscar fuentes cuando:

- Una afirmación científica o de transferencia depende de evidencia.
- Cambiaron estándares, políticas, SDKs o requisitos de stores.
- Existe duda legal, médica, de seguridad o técnica.
- Se necesita precisión de documentación oficial.

Prioridades:

- Ciencia: revisiones sistemáticas, metaanálisis y estudios primarios aplicables.
- Tecnología: documentación primaria/oficial.
- Privacidad/legal: textos y autoridades oficiales.
- Seguridad: NIST, OWASP y documentación oficial.

Toda inferencia se etiqueta. No copiar una recomendación genérica sin demostrar su aplicabilidad a MindBoost.

---

## 21. Plantilla maestra del prompt para Claude

El prompt final debe adaptar y completar esta plantilla; no dejar placeholders.

```markdown
# REAPERTURA OBLIGATORIA — MindBoost <fase/slice>

Usa `/mb` como lead orchestrator. La fase anterior queda <REOPEN/BLOCKED/CONDITIONAL>
y no puede declararse DONE hasta satisfacer este contrato.

## 1. Objetivo original y outcome de usuario
<reconstrucción exacta>

## 2. Evidencia revisada
- <archivos/diff/tests>
- Límites: <no verificado>

No confíes en el resumen anterior: inspecciona código, callers, consumers, schemas,
flags, tests y documentación desde el repositorio actual. Preserva cambios ajenos.

## 3. Fuentes de verdad obligatorias
1. `00_CONTEXT_INDEX.md`
2. <documentos exactos>
3. <ADR/contrato exacto>

Aplica su orden de precedencia. Si el código contradice una fuente superior, corrige
o registra explícitamente una decisión nueva; no adaptes el documento para justificar
el estado accidental.

## 4. Hallazgos que debes resolver

### <ID> — <P0/P1/P2/P3> — <título>
- Evidencia: <archivo/símbolo/comportamiento>
- Riesgo: <usuario/ciencia/datos/coste>
- Resultado exigido: <no prescribir implementación si Claude debe explorar opciones>
- Invariantes: <propiedades>
- Tests de aceptación: <casos>

<repetir todos los hallazgos, incluso mejoras pequeñas in-scope>

## 5. Decisiones cerradas
- <decisiones CEO/ChatGPT que no debe volver a preguntar>

## 6. Decisiones técnicas que debes razonar
Para cada una: inspecciona evidencia, compara alternativas, elige, documenta confianza,
migración y rollback. Si afecta scoring/privacidad, falla cerrado ante incertidumbre.

## 7. Plan de ejecución
Antes de editar, crea un plan maestro y descompón en slices verticales. No trabajes por
capas horizontales ni dejes wiring para un cierre ambiguo.

Orden base:
1. Baseline reproducible.
2. Gate de entrada científico/psicométrico.
3. Contrato/arquitectura.
4. Implementación vertical.
5. Migración y wiring.
6. Tests adversarios y multiplataforma.
7. Documentación.
8. Gate de salida en diff final.

## 8. Agent routing obligatorio
- `/mb`: <agentes base exactos y función>
- `<mindboost-agent>`: <pregunta, archivos, entregable, read/write, orden>
- `<mindverse-agent>`: <pregunta, archivos, entregable, read/write, orden>

No lances familias completas. La sesión principal integra y decide; los subagentes no
pueden orquestar otros subagentes.

## 9. Restricciones
- No P0/P1.
- No IA en Free.
- No XP/racha/contexto/suscripción en score.
- No missing=0 ni latest raw como perfil.
- No cablear contratos con prerequisitos abiertos.
- Offline idempotente; evidencia pendiente hasta sync/integridad.
- Android 8 e iOS 15 con paridad.
- Presupuesto adicional 0 €; toda opción pagada necesita alternativa 0 €.
- No acciones externas, deploy, gasto o borrado real sin autorización.

<añadir restricciones específicas>

## 10. Tests y gates obligatorios

### Unit/golden/property
- <casos exactos>

### Integración/wiring
- <casos exactos>

### Datos/migración/offline
- <casos exactos>

### UI/accesibilidad/plataforma
- <casos exactos>

### Comandos/gates
- Descubre y ejecuta los comandos canónicos del repo.
- Reporta scope, exit code, skips y plataformas no ejecutadas.

Gate de salida en el diff definitivo:
- `efficacy-guardian`
- `psychometrician` si aplica
- `diff-auditor`
- `privacy-security` si aplica
- <agentes adicionales>

Si un gate encuentra bloqueante, corrige y vuelve a pasar el gate sobre el diff nuevo.

## 11. Documentación y trazabilidad
Actualiza:
- <archivos>
- Decision Log/ADR
- Technical Current State
- dossier/schema/contract afectado

No reescribas historial silenciosamente. Versiona fórmula, schema, generador y flags.

## 12. Definition of Done específica
La reapertura solo termina si:
- <criterios verificables>
- No quedan P0/P1.
- Toda mejora segura, relevante e in-scope está aplicada.
- Lo realmente fuera de scope tiene owner, razón y barrera fail-closed.

## 13. Formato de cierre obligatorio
1. Veredicto y outcome de usuario.
2. Inventario exacto de cambios.
3. Decisiones y ADR.
4. Tabla hallazgo → corrección → evidencia.
5. Tests/comandos/resultados/skips.
6. Android/iOS verificados.
7. Migraciones, flags y rollback.
8. Agentes y veredictos del diff final.
9. Claims permitidos y no permitidos.
10. Riesgos/follow-ups, distinguiendo bloqueantes de mejoras futuras.
11. Archivos modificados para la siguiente auditoría de ChatGPT.

No declares DONE por intención, compilación parcial o consenso de agentes. Demuéstralo.
```

---

## 22. Criterios de un prompt perfecto

Un prompt de reapertura es 10/10 si:

- Es autocontenido.
- No depende de que Claude recuerde esta conversación.
- Separa decisiones cerradas de decisiones a investigar.
- Incluye evidencia y ubicación de cada problema.
- Define resultados e invariantes, no solo cambios superficiales.
- Exige casos adversarios concretos.
- Incluye routing exacto y evita agent sprawl.
- Fuerza integración, migración, paridad y documentación.
- Impide falsos positivos de cierre.
- Mantiene el alcance, pero captura todas las mejoras relevantes dentro de él.
- Exige entregar los archivos necesarios para una segunda auditoría.

---

## 23. Cierre del ciclo

Cuando Claude devuelve la reapertura:

1. No asumir que todos los hallazgos están resueltos.
2. Comparar uno por uno con la tabla de aceptación.
3. Revisar el diff posterior a las correcciones.
4. Confirmar que los gates se ejecutaron después del último cambio.
5. Buscar regresiones introducidas por la solución.
6. Aprobar solo si cumple DoD.
7. Si no, emitir una nueva reapertura más acotada.

No existe límite artificial de iteraciones. El criterio es calidad demostrada.

---

## 24. Regla final

La función de esta revisión no es demostrar que ChatGPT razona más que Claude. Es conseguir que el sistema combinado detecte más, corrija mejor y deje una base verificable para la siguiente fase.

