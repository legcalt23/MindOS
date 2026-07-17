---
title: "ACTIVE trial: cognitive training in older adults (reasoning, speed, memory)"
authors: "Ball, K.; Willis, S. L.; Rebok, G. W.; Edwards, J. D.; et al."
year: "2002–2016"
tier: T1
domains: [processing_speed, logical_reasoning, working_memory]
constructs: [speed_of_processing_training, useful_field_of_view, reasoning_training, functional_transfer]
paradigms: [ufov, reasoning, verbal_memory]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt
evidence_level: moderate
---

## (a) Tesis central

ACTIVE (Advanced Cognitive Training for Independent and Vital Elderly) es el mayor RCT de
entrenamiento cognitivo en adultos mayores (n ≈ 2 832, seguimiento hasta 10 años). Comparó tres
entrenamientos (razonamiento, velocidad de procesamiento/UFOV, memoria). Hallazgo matizado: cada
entrenamiento mejoró **su** capacidad diana de forma **duradera**, y el entrenamiento de **velocidad de
procesamiento** (UFOV) mostró alguna **transferencia funcional** (menos accidentes de conducción, mejor
mantenimiento de actividades instrumentales de la vida diaria, IADL). Es la mejor evidencia pro-
transferencia — pero acotada, específica de dominio y de población.

## (b) Claims RESPALDADOS

- El entrenamiento específico mejora la capacidad entrenada, con durabilidad de años. [RCT
  longitudinal]
- El entrenamiento de velocidad de procesamiento (UFOV) transfiere a algunos resultados funcionales en
  mayores: conducción (Ball et al. 2010) e IADL (en parte **auto-reportadas**). [RCT — efecto real pero
  específico]
- La transferencia es más probable cuando la tarea entrenada comparte demandas con el resultado diana
  (UFOV ↔ atención visual útil para conducir). [mecanismo específico]

## (c) Claims NO respaldados o refutados

- **No** generaliza a inteligencia general ni a población joven/sana: los efectos son específicos de
  adultos mayores y de dominios concretos.
- El entrenamiento de memoria/razonamiento **no** mostró la misma transferencia funcional que la
  velocidad; no todo entrenamiento transfiere igual.
- Los hallazgos de "menor riesgo de demencia" son análisis secundarios y deben tratarse con cautela;
  no autorizan claims de salud.

## (d) Estado de replicación / críticas

ACTIVE es metodológicamente fuerte (RCT, seguimiento largo). Las críticas apuntan a que la
transferencia funcional fue modesta y específica, y que los análisis de demencia son exploratorios. Su
valor es matizar el escepticismo total: **la transferencia cercana/específica de dominio existe cuando
la tarea entrenada comparte demandas con el objetivo** — coherente con Uttal (espacial) y con la
taxonomía de transferencia ([[T07_far_vs_near_transfer]]).

## (e) Implicaciones de diseño para MindBoost

- **Da apoyo cauto y honesto a Velocidad:** el entrenamiento de velocidad de procesamiento es de los
  pocos con transferencia funcional demostrada (en mayores). MindBoost puede tratar el progreso en
  velocidad como valioso, siempre atado a *la propia línea base* y sin extrapolar a población joven o a
  claims de salud/demencia.
- Refuerza el principio "transferencia sólo cuando comparte demandas": diseñar ejercicios cuya
  estructura se parezca a la habilidad objetivo, en vez de esperar magia general.
- **Prohibido** importar los hallazgos de demencia a MindBoost (no es producto de salud; el charter lo
  veta). El aprendizaje útil es de diseño (especificidad), no un claim clínico.
- **Claim permitido:** "practicar velocidad de respuesta con precisión controlada, midiendo tu propio
  progreso". **Prohibido:** "reduce tu riesgo de deterioro / previene el declive".

## (f) Referencias

- Ball, K., et al. (2002). Effects of cognitive training interventions with older adults: A randomized
  controlled trial (ACTIVE). *JAMA, 288*(18), 2271–2281.
- Willis, S. L., et al. (2006). Long-term effects of cognitive training on everyday functional
  outcomes in older adults. *JAMA, 296*(23), 2805–2814.
- Rebok, G. W., et al. (2014). Ten-year effects of the ACTIVE cognitive training trial. *Journal of
  the American Geriatrics Society, 62*(1), 16–24.
- Ball, K., Edwards, J. D., Ross, L. A., & McGwin, G. (2010). Cognitive training decreases motor
  vehicle collision involvement of older drivers. *Journal of the American Geriatrics Society, 58*(11),
  2107–2113.
- Edwards, J. D., et al. (2017). Speed of processing training results in lower risk of dementia
  [secondary/exploratory analysis]. *Alzheimer's & Dementia: TRCI, 3*(4), 603–611.
