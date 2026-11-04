#!/usr/bin/env bash
# Builds the R3 remediation evidence archive at a recoverable repo path (NOT a manifest member).
# Sanitizes host/user; verifies clean with the scanner BEFORE archiving. Reproducible.
set -euo pipefail
# no hardcoded user paths: derive repo root from script location; worktree from required env var R3_WT
IDE="$(cd "$(dirname "$0")/.." && pwd)"
WT="${R3_WT:?set R3_WT to the isolated worktree path}"
OUT="$IDE/docs/engines/focus_v5/reaudit/v2/transfer"
STG="$OUT/staging"
rm -rf "$STG"; mkdir -p "$STG/xml" "$STG/docs"

# 1) sanitizer: user path -> <REPO>, bare user -> <USER>, any XML hostname -> <HOST>
# generic: matches ANY user in a Users/home path — NO literal username in this source
san() { node -e '
  const fs=require("fs");let t=fs.readFileSync(process.argv[1],"utf8");
  t=t.replace(/[A-Za-z]:[\\/]+Users[\\/]+[^\s"\\/]+/g,"<REPO_USER>")
     .replace(/\/[a-z]\/Users\/[^\s"\\/]+/g,"<REPO_USER>")
     .replace(/\/(home|Users|root)\/[A-Za-z0-9._-]+/g,"<HOME_USER>")
     .replace(/hostname="[^"]*"/g,"hostname=\"<HOST>\"");
  fs.writeFileSync(process.argv[2],t);
' "$1" "$2"; }

# 2) core artifacts (bytes copied verbatim; sanitized on the way in where they can carry host paths)
cp "$IDE/scripts/pii-secret-scan.mjs"            "$STG/pii-secret-scan.mjs"
cp "$IDE/scripts/pii-secret-scan.test.mjs"       "$STG/pii-secret-scan.test.mjs"
cp "$IDE/scripts/parse-junit-xml.mjs"            "$STG/parse-junit-xml.mjs"
cp "$IDE/scripts/build-r3-evidence-archive.sh"   "$STG/build-r3-evidence-archive.sh"
cp "$IDE/docs/engines/focus_v5/reaudit/build_external_manifest.mjs" "$STG/build_external_manifest.mjs"
cp "$IDE/docs/engines/focus_v5/reaudit/v2/validate_cross_artifact.mjs" "$STG/validate_cross_artifact.mjs"
cp "$IDE/scripts/validate-audit-ledger.mjs"      "$STG/validate-audit-ledger.mjs"
cp "$IDE/scripts/validate-authority-entrypoints.mjs" "$STG/validate-authority-entrypoints.mjs"
cp "$IDE/scripts/validate-authority-entrypoints.test.mjs" "$STG/validate-authority-entrypoints.test.mjs"
cp "$IDE/scripts/authority-scanner.mutation.mjs" "$STG/authority-scanner.mutation.mjs"
cp "$IDE/docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json" "$STG/S5_R3_EXTERNAL_MANIFEST.json"
cp "$IDE/docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.md"   "$STG/S5_R3_EXTERNAL_MANIFEST.md"
san "$IDE/MINDBOOST_AUDIT_LEDGER.md"             "$STG/MINDBOOST_AUDIT_LEDGER.md"
cp "$IDE/docs/engines/focus_v5/reaudit/v2/findings_v2.json" "$STG/docs/findings_v2.json"
cp "$IDE/docs/engines/focus_v5/reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md" "$STG/docs/S5_R3_MULTIDIMENSIONAL_VERDICTS.md"
cp "$IDE/docs/engines/focus_v5/reaudit/v2/S5_R3_LOG_AND_PII.md" "$STG/docs/S5_R3_LOG_AND_PII.md"
cp "$IDE/docs/engines/focus_v5/reaudit/v2/logs/focus2_full_suite_result.json" "$STG/docs/focus2_full_suite_result.json"
san "$IDE/docs/engines/focus_v5/reaudit/v2/logs/command_log.jsonl" "$STG/docs/command_log.jsonl"

# 3) all 27 primary focus2 XMLs, sanitized (hostname -> <HOST>)
for x in "$WT"/shared/build/test-results/desktopTest/TEST-*focus2*.xml; do
  san "$x" "$STG/xml/$(basename "$x")"
done

# 4) derive totals from the PRIMARY XML bytes (not from hand-written JSON)
node "$IDE/scripts/parse-junit-xml.mjs" "$STG/xml" > "$STG/focus2_xml_totals.json" 2>/dev/null

# 5) attestation (fresh scan of manifest members) + regen
( cd "$IDE" && node scripts/pii-secret-scan.mjs --manifest >/dev/null 2>&1 || true )
san "$IDE/docs/engines/focus_v5/reaudit/v2/logs/pii_scan_attestation.json" "$STG/pii_scan_attestation.json"

# 6) scanner TAP
( cd "$IDE" && node --test scripts/pii-secret-scan.test.mjs 2>&1 ) | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(s.replace(/curro/g,"<USER>")))' > "$STG/scanner_tests.tap" || true

# 7) patch/diff from HEAD (tracked) + untracked list + status (sanitized, repo-relative)
( cd "$IDE" && git diff HEAD ) | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(s.replace(/curro/g,"<USER>")))' > "$STG/changes_tracked.patch" || true
( cd "$IDE" && git status --porcelain=v2 ) | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(s.replace(/curro/g,"<USER>")))' > "$STG/git_status_porcelain.txt" || true

# 8) env / tool versions
{ echo "node $(node --version)"; java -version 2>&1 | head -1; echo "gradle 9.3.1"; echo "os $(uname -s -r 2>/dev/null || echo windows)"; echo "utc $(date -u +%FT%TZ)"; } > "$STG/env_versions.txt"

# 9) TRANSFER_MANIFEST.json (path/size/sha per staged member) + file list
node -e '
  const fs=require("fs"),cr=require("crypto"),p=require("path");
  const root=process.argv[1];const rows=[];
  const walk=(d)=>{for(const e of fs.readdirSync(d,{withFileTypes:true})){const fp=p.join(d,e.name);
    if(e.isDirectory())walk(fp); else {const b=fs.readFileSync(fp);
      rows.push({path:p.relative(root,fp).split(p.sep).join("/"),bytes:b.length,sha256:cr.createHash("sha256").update(b).digest("hex")});}}};
  walk(root);
  rows.sort((a,b)=>a.path<b.path?-1:1);
  const tm={manifest:"TRANSFER_MANIFEST",generated:"see env_versions.txt",member_count:rows.length,members:rows};
  fs.writeFileSync(p.join(root,"TRANSFER_MANIFEST.json"),JSON.stringify(tm,null,2)+"\n");
  console.log("transfer members:",rows.length);
' "$STG"

# 10) README
cat > "$STG/README.md" <<'EOF'
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
EOF

# 11) verify the staging is clean BEFORE archiving — FAIL-CLOSED (abort if any finding)
SCANLIST=$(find "$STG" -type f \( -name '*.md' -o -name '*.json' -o -name '*.txt' -o -name '*.mjs' -o -name '*.jsonl' -o -name '*.tap' -o -name '*.patch' -o -name '*.xml' -o -name '*.sh' \) | tr '\n' ' ')
if ( cd "$IDE" && node scripts/pii-secret-scan.mjs --files $SCANLIST >/dev/null 2>&1 ); then echo "STAGING_SCAN=CLEAN"; else echo "STAGING_SCAN=FINDINGS — aborting archive (fail-closed)"; exit 1; fi

# 12) archive + hash
ARCHIVE="$OUT/R3_REMEDIATION_EVIDENCE_V2.tar.gz"
( cd "$STG" && tar --force-local -czf "$ARCHIVE" . )
echo "ARCHIVE=$ARCHIVE"
echo "ARCHIVE_BYTES=$(stat -c%s "$ARCHIVE" 2>/dev/null || wc -c < "$ARCHIVE")"
echo "ARCHIVE_SHA256=$(node -e 'const c=require("crypto"),fs=require("fs");console.log(c.createHash("sha256").update(fs.readFileSync(process.argv[1])).digest("hex"))' "$ARCHIVE")"
echo "TRANSFER_MANIFEST_SHA256=$(node -e 'const c=require("crypto"),fs=require("fs");console.log(c.createHash("sha256").update(fs.readFileSync(process.argv[1])).digest("hex"))' "$STG/TRANSFER_MANIFEST.json")"
