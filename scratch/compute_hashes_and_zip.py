import os
import hashlib
import zipfile

# Directory configuration
dest_dir = r"c:\Users\curro\AndroidStudioProjects\MindOS\.antigravity-only\plans\current-plan-review-evidence-based_20260621_050307"

# Get all files 00 to 14
files_to_hash = sorted([f for f in os.listdir(dest_dir) if f.endswith(".md") and f[:2].isdigit() and int(f[:2]) <= 14])

# Compute SHA256 hashes
hashes = {}
for filename in files_to_hash:
    path = os.path.join(dest_dir, filename)
    hasher = hashlib.sha256()
    with open(path, "rb") as f:
        buf = f.read()
        hasher.update(buf)
    hashes[filename] = hasher.hexdigest()

# Write 15_package_integrity.md content
integrity_content = """# 15 Package Integrity & Verification Record

Este documento registra los hashes criptográficos (SHA-256) de todos los archivos que componen el paquete de diseño blindado de MindBoost, garantizando la inmutabilidad de la especificación técnica antes de proceder con el desarrollo local y las auditorías de seguridad.

## Declaración Oficial de Aprobación

> [!IMPORTANT]
> **APROBACIÓN DE DISEÑO (10/10 DESIGN ARTIFACT):**
> Este paquete ha sido auditado y aprobado como un artefacto de diseño teórico de nivel 10/10, conteniendo las mitigaciones y la arquitectura técnica requeridas para el control de riesgos.
>
> **NO APROBACIÓN OPERATIVA (NOT YET 10/10 RUNTIME):**
> Este paquete **NO** está aprobado como un sistema operativo de producción en runtime. Ninguna de las capas de Inteligencia Artificial (IA) o características competitivas basadas en red está validada o lista para producción. Todo el runtime de IA permanece bloqueado hasta presentar evidencias reales superando los gates correspondientes.

## Reglas de Integridad del Runtime
* **Runtime de IA Desactivado:** Las capas de IA, llamadas a LLMs, embeddings externos, APIs de terceros, accesos MCP y herramientas de navegador están estrictamente prohibidas y bloqueadas en la implementación actual.
* **Cero Falsos Positivos sobre Validación:** Ningún claim del sistema afirmará que la IA está validada o que la privacidad/seguridad del sistema es definitiva sin evidencias empíricas ejecutadas.
* **Sin Gates PASSED Pre-Ejecución:** Todos los gates de `14_evidence_manifest.md` deben mantenerse en estado `DESIGNED / PENDING` hasta que se ejecute la prueba técnica oficial.

## Registro de Hashes SHA-256 (Archivos del Paquete)

| Archivo | Hash SHA-256 |
| ------- | ------------ |
"""

for filename in files_to_hash:
    integrity_content += f"| `{filename}` | `{hashes[filename]}` |\n"

integrity_content += """
---

## Verificación de Archivos de Distribución

* **ZIP de Distribución:** `current-plan-review-evidence-based_20260621_050307.zip`
* **SHA-256 de Distribución:** *(Calculado tras la compresión)*
"""

# Write the integrity file
integrity_path = os.path.join(dest_dir, "15_package_integrity.md")
with open(integrity_path, "w", encoding="utf-8") as f:
    f.write(integrity_content)

# Now zip everything in the folder (including the newly created 15_package_integrity.md)
zip_path = dest_dir + ".zip"
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(dest_dir):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, dest_dir)
            zipf.write(file_path, arcname)

# Compute hash of the final zip archive
zip_hasher = hashlib.sha256()
with open(zip_path, "rb") as f:
    zip_buf = f.read()
    zip_hasher.update(zip_buf)
zip_hash = zip_hasher.hexdigest()

# Output the zip hash and verify
print("COMPLETED SUCCESS.")
print(f"ZIP SHA256: {zip_hash}")
print(f"Directory contents frozen and hashed.")
