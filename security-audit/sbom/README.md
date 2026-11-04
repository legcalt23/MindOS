# Estado del SBOM

No se genero un SBOM formal CycloneDX/SPDX porque Syft/Trivy/Grype/OSV no estaban disponibles y la politica impidio instalar o resolver herramientas/dependencias sin necesidad. Fabricar un SBOM incompleto como si fuera autoritativo seria evidencia falsa.

Artefactos disponibles:

- `npm-lock-summary.json`: resumen determinista de ambos lockfiles.
- `gradle-inventory.md`: inventario y gaps de reproducibilidad.

Para release se requiere generar SBOM desde el mismo job y artefacto firmado, con herramienta/version/checksum fijados y digest enlazado a la atestacion.

