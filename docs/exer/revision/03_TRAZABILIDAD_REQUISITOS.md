# Trazabilidad de requisitos

Cada fila vincula una necesidad a instrucciones concretas y una comprobación. Los escenarios son especificaciones de aceptación; los ejecutados realmente se enumeran en el informe de revisión.

| ID | Requisito/fuente | Dónde se resuelve | Comprobación |
|---|---|---|---|
| R01 | ZIP solo con instrucciones Markdown · encargo actual | Paquete completo y manifiesto | Archivo ZIP, extensiones, integridad |
| R02 | Diseñar conceptualmente antes de redactar el sistema | 02_DISENO_CONCEPTUAL.md | Arquitectura documentada antes del resto |
| R03 | Destino GPT-6 Astra y entrada deseada /exer | 01_PROMPT; 03_INSTALACION; comando/EXER | T26; no sustitución silenciosa |
| R04 | Diseñar ejercicios, no implementarlos | sistema/01,04,10; comando/EXER | T23 |
| R05 | Especialización amplia con coordinación real | sistema/03 | T19–20 |
| R06 | Máxima calidad cognitiva defendible | sistema/05–09; dossier | T03,11,29–30 |
| R07 | Plataforma seria y modalidad sencilla seria | sistema/07; contexto/00 | T13–15 |
| R08 | Media poblacional y personalización individual | sistema/01,08,13 | T30 y análisis de sobreajuste |
| R09 | Grafo Graphify+Obsidian como contexto vivo | sistema/02; plantilla07 | T01,06,21–22 |
| R10 | Actualizar hechos obsoletos preservando decisiones | sistema/01–02 | T06,08,22 |
| R11 | Preguntar cualquier cambio incluso pequeño | sistema/01; plantilla05 | T08–10,25 |
| R12 | Incorporar auditoría después de preparar comando | sistema/10; plantilla04 | T02,18 |
| R13 | Motor flexible sin reescritura silenciosa | sistema/10 | Actual/vNext, T07,18 |
| R14 | Revisión profunda y cierre acotado | sistema/04,12 | T24–25 |
| R15 | Once adjuntos íntegros y trazables | contexto/fuentes; manifiesto | Comparación SHA-256/bytes |
| R16 | Decisiones 1B/2B/3A/4A | contexto/00–01 | No reabrir nombre, IA o Game |
| R17 | Training/medición y estados ortogonales · 03/04 | sistema/09; dossier | T03–05,13 |
| R18 | Dificultad, adaptación y dosis sin números mágicos | sistema/08 | Parámetros con fundamento/gates |
| R19 | Procedimiento, contenido y feedback completos · 04 | sistema/07; dossier D–I | Representación de sesión sin inventar reglas |
| R20 | Móvil, timing, accesibilidad, offline · 03/04/05 | sistema/11; dossier L–M | T12–13 y plan de dispositivos |
| R21 | Privacidad/subject/minimización · PRD/05/09 | sistema/02,11 | Sin datos de usuarios en grafo; integridad |
| R22 | Catálogo ≥15/120 sin skins · PRD/04/09 | sistema/06; plantilla03 | T16 |
| R23 | No confundir ocho motores y once dimensiones · 09 | contexto/00; sistema/06,10 | T17 |
| R24 | Facts-only, preliminar y ADR abiertos · 05/09 | sistema/09–11; contexto/00 | Sin perfiles/puentes/proxies inventados |
| R25 | Alpha no prueba eficacia; validación futura · 08 | sistema/13; plantilla08 | T03,28–30 |
| R26 | Fuentes científicas precisas y aplicabilidad | sistema/05; contexto/02 | DOI/PMID/fechas/acceso, límites explícitos |
| R27 | Guardar continuidad sin ciclos interminables | sistema/14; plantilla07 | T27 |
| R28 | No declarar instalación/actualización/verificación no realizada | 00_LEEME; 03_INSTALACION; revisión | T01–02,06–07,19,26 |

## Límites que debe conservar la instancia receptora

El paquete no contiene el repo, el grafo, la auditoría ejecutada ni los datos de Molvex. La preparación de instrucciones es verificable aquí; la activación real del cliente, actualización del grafo y eficacia de los ejercicios futuros son comprobaciones posteriores distintas.
