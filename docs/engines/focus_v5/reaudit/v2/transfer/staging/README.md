# R3_REMEDIATION_EVIDENCE_V2 — reproducible evidence bundle
This archive is OUTSIDE the release manifest. No preimages, no secrets, host/user redacted (<REPO>/<USER>/<HOST>).
Verify:
  node pii-secret-scan.mjs --selftest            # exit 0
  node --test pii-secret-scan.test.mjs           # 10/10
  node parse-junit-xml.mjs xml                    # derives focus2 totals from the 27 primary XML
  node build_external_manifest.mjs               # (in a repo checkout) reprints package_root
  node validate_cross_artifact.mjs               # (in a repo checkout) BIDIRECTIONAL OK
TRANSFER_MANIFEST.json lists every member with size+SHA-256. focus2_xml_totals.json is DERIVED from xml/.
Android: :shared:compileAndroidMain + :androidApp:compileDebugKotlin = BUILD SUCCESSFUL (see env). iOS BLOCKED (no macOS).
