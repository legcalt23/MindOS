# S5_R3_EXTERNAL_MANIFEST — Focus V5 · S5 · R3 (reaudit) package closure

```
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW (not a human/institutional external audit)
HASH: sha256 (full 64 hex) over worktree bytes as-on-disk
PACKAGE_ROOT_SHA256: 0761c4d30e6d842b8c1f288894836b45ad3eea7297cebf42fad3ae576d00234a
MEMBERS_PRESENT: 52   MISSING: 0
```

> The manifest resolves the handoff self-reference paradox: the handoff cannot contain its own hash, 
> but this sibling manifest hashes it. To verify: checkout the same HEAD, run this script, compare 
> `package_root_sha256` and each member sha256. EOL differences (core.autocrlf) shift CRLF/MIXED hashes.

| Member | Status | Bytes | EOL | last | sha256 (full) |
|---|---|---|---|---|---|
| `docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md` | ok | 93873 | LF | 0a | `bd8f44d85f54b6e8edf555e59836790d11b9e76c8b431055cb5ad516ca5645b0` |
| `docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md` | ok | 9454 | LF | 0a | `4d0a185254129ca3abef63c65a0063b0f3b2b38d425427acf83c72b7bf23426b` |
| `docs/engines/focus_v5/PROVENANCE_INCIDENT_V5_BUNDLE.md` | ok | 5534 | LF | 0a | `e623b811cee611e42ed9b5ff4a6ce6f8548dffe4e953666e9b44c58d51bfe2b4` |
| `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md` | ok | 10937 | LF | 0a | `b7b60bf35b39deed7c437ed218adcc7a5b0ed27b07f039c824adb13a394ad74c` |
| `docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.json` | ok | 6052 | LF | 0a | `86e91e12dcba916f9945de9d2b3562b20aeaa35bd9d2a0d28365c9bb751b75c2` |
| `docs/engines/focus_v5/S5_R3_P7_HANDOFF.md` | ok | 10084 | LF | 0a | `17a8f765e30f0decba99dabb5384b70eb617e5a96b79bcada4ae2520d1867acc` |
| `docs/engines/focus_v5/S5_REOPEN_STATUS.md` | ok | 52938 | LF | 0a | `441a568935603966a0a30fdaf0022b9185e444f84e65c3517b7549914ff61933` |
| `docs/engines/focus_v5/ADR_REGISTER_V5.md` | ok | 18160 | LF | 0a | `76196e475442c46ff06dae75e3952682eeaf97bd9ab384ea43b13bccbf65e214` |
| `docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_MASTER_REPORT.md` | ok | 6744 | LF | 0a | `8864e70274bac5e1a4737aeb7fab4f60c1f3467457fb82b1248710106e74bdb3` |
| `docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_FINDINGS.json` | ok | 16814 | LF | 0a | `89708a7d2fb8bbcc67202ae0321375dd6a57a9e1b0837595c205da2d581422c7` |
| `docs/engines/focus_v5/reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md` | ok | 6901 | LF | 0a | `20d5cb7d708f24a938e42119fb84db28b498582d8c58804854467fdd25883797` |
| `docs/engines/focus_v5/reaudit/S5_R3_ELIGIBILITY_CONTRACT_SPEC.md` | ok | 5397 | LF | 0a | `2497bd1c6f2ab064d82765985528604fdba02f55fc1b17d953a661974c00fb3d` |
| `docs/engines/focus_v5/reaudit/S5_R3_DATAFLOW_THREATMODEL.md` | ok | 3889 | LF | 0a | `83769927ffc3953ea012ab596db5f527f287b6be3891ecb6156e5abb9a27c62f` |
| `docs/engines/focus_v5/reaudit/S5_R3_BLOCKERS_MATRIX.md` | ok | 6024 | LF | 0a | `4f335979bde23772526550c0e2e435c046a2703a5d058bd577f08a68387fbd51` |
| `docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs` | ok | 20858 | LF | 0a | `6d7220ff5e4af474faf6337066227d2bf9a072f59e479b5c518dbd2c20e9a0e5` |
| `docs/engines/focus_v5/reaudit/r2_falsification_result.json` | ok | 7004 | LF | 0a | `5891a321590e155e534ad48f1452bb9ef60d53f1038497dec6819eeaeb3857cd` |
| `docs/engines/focus_v5/reaudit/r2_falsification_summary.txt` | ok | 877 | LF | 0a | `6925165450bbd713ead02992a6034805e206fe275ff85358fa6cafefa8eb8117` |
| `scripts/validate-authority-entrypoints.mjs` | ok | 10774 | LF | 0a | `25bb54ed4f12e71c82f88123c3dbbb0a5a9d8bb440c331a6c10e99f723754ec9` |
| `docs/engines/focus_v5/authority-registry.json` | ok | 1920 | LF | 0a | `b1243831365971417b203d7a1dd590a4f49a8089041873798fb0207e01447a0a` |
| `scripts/validate-authority-entrypoints.test.mjs` | ok | 10595 | LF | 0a | `5a6ab450f796b26bf36af7b4df0082e5c002588bbbd06a50c0b7fa9a086415f0` |
| `docs/engines/focus_v5/reaudit/v2/findings_v2.json` | ok | 15677 | LF | 0a | `370282655a833c8702439a6fc73969f6ccc6f72c56d6596e455652330228e291` |
| `docs/engines/focus_v5/reaudit/v2/validate_cross_artifact.mjs` | ok | 9466 | LF | 0a | `3da6498c31ba66f902a739f423ada95c01c9bf9e83a2d2fd77f0f3d06f61a3a7` |
| `docs/engines/focus_v5/reaudit/v2/phase0_inventory.mjs` | ok | 5983 | LF | 0a | `d1c0ebb5cfc2e2df31499ed23dc11602a2bc071b2985338a3a9bca2b08b40a87` |
| `docs/engines/focus_v5/reaudit/v2/phase0_inventory.json` | ok | 13804 | LF | 7d | `0183a3a9b35a5364a82a021393eba915fda4a92094e91acb4f857d36e0e0f153` |
| `docs/engines/focus_v5/reaudit/v2/logs/command_log.jsonl` | ok | 1495 | LF | 0a | `cb25fa0fa0585babbcb7867c97bfc293f2ac6d2a9181516e351059a758941955` |
| `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus2/domain/scoring/DescriptiveScorerGoldenReauditTest.kt` | ok | 2803 | LF | 0a | `9dde6e35cee4c437633132c5babfd4e46a30134d6f7535ff7b9d9b9332b2d452` |
| `scripts/validate-audit-ledger.mjs` | ok | 4114 | LF | 0a | `b3274436b7729f50af025736bccc7a6a3b5bc6375f978c211653b8caf7e0e3a0` |
| `docs/engines/focus_v5/reaudit/build_external_manifest.mjs` | ok | 9530 | LF | 0a | `1289368ba64d44976450dc754849e8b9691513d8e6a8d8b8354ef166e10c09c4` |
| `docs/engines/focus_v5/reaudit/S5_R3_CEO_MEMO.md` | ok | 3851 | LF | 0a | `cfa81e7588ff634dc9e93244882088e3f9299bc07d6eef6fbfc5f3ecc82d2663` |
| `docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_REDLINE.md` | ok | 2914 | LF | 0a | `5895d30c8d8daac9c622ef88f4d7cb943d1f81a0e602af8cbe78dc12bd771515` |
| `docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_COMMAND_LOG.md` | ok | 5109 | LF | 0a | `aa3f1ec8f80473fc7cac9e100e672e8dbb59b2989ecdb3ba277f9714bc7040a8` |
| `docs/engines/focus_v5/reaudit/S5_R3_LENS_OUTPUTS.json` | ok | 136311 | LF | 7d | `e103b897e480b873ecbe2a78f0f35187b9865b9cbf57094f76129ee748711c82` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_REAUDIT_V2_MASTER_REPORT.md` | ok | 8049 | LF | 0a | `b0d8f768cb87d439c5daece3101dd7daf0469af990390486b16839ad5ab2e42b` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_KOTLIN_EXECUTION_REPORT.md` | ok | 6976 | LF | 0a | `8be9142d0ddb97975c461bad9bcf06f2757ea445bcc2d28c24ab72056b3609b2` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_FINDINGS_LINEAGE.md` | ok | 9306 | LF | 0a | `353e47097f46f788527eecc6123f1321f169634e9b8f8c73cb22b3bd63a3eab3` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_LENS_COVERAGE.md` | ok | 6849 | LF | 0a | `dce4e3e988858599671b0bae7eb395f74270be9d5634dac16eccfe76bd82e874` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_ROOT_OF_TRUST_AUDIT.md` | ok | 6298 | LF | 0a | `c756d91e1662d5ec3b444baa242f17782c42b3ba627b2d42a45efee100c3ddf2` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_R2_DOMAIN_SPEC.md` | ok | 5072 | LF | 0a | `b8a23a30f7d2fac6b6a27301b7535c9be86d6b26e4dd2d226837cf2847973688` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_CEO_DECISION_PACKET.md` | ok | 3311 | LF | 0a | `546d9fd6cdd9c7960e11e07ca6a0e6aa9ac809a38c7d8d860a9df22e5cf2d70f` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_PSYCHOMETRIC_PROTOCOL.md` | ok | 2704 | LF | 0a | `9041313227cfe7dc98df301d23810db10a2052d88c500f99c346681b403abf23` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_PRIVACY_DATA_MAP.md` | ok | 3147 | LF | 0a | `64236543078d09fed7f329b426dd20081a8add5b90d4cafe526b901dd8e5e418` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_ACCESSIBILITY_DECISION_RECORD.md` | ok | 2454 | LF | 0a | `701ef16e68540897a11f63628af9ba5d13bc8afeecef7d25d2fc370b8d2129f8` |
| `docs/engines/focus_v5/reaudit/v2/S5_R3_LOG_AND_PII.md` | ok | 4531 | LF | 0a | `a54c61dc573cb8b1d64cace4f2f31066427fe53285acf4aa5e84e1b981e2dfdc` |
| `docs/engines/focus_v5/reaudit/v2/README_VERIFY.md` | ok | 4115 | LF | 0a | `3d938cbad064cebc99bc85c9a8121e5e695dafd6ca5d129dcee6deb31fd31462` |
| `scripts/authority-scanner.mutation.mjs` | ok | 4013 | LF | 0a | `f2cf175dc806a6f0d1db4cf6696a26e168d4eac85a48f432cfb78e4c4b1355c1` |
| `scripts/pii-secret-scan.mjs` | ok | 15048 | LF | 0a | `d2c7f6abc540cd3b1d654fe509f527284b77730456d356aac249960a88f75cee` |
| `scripts/pii-secret-scan.test.mjs` | ok | 3786 | LF | 0a | `f7fcf6d7b3b253902ed3f3134b72b7584716071cafc71c7c2e16d333cd2056b3` |
| `docs/engines/focus_v5/reaudit/v2/logs/focus2_full_suite_result.json` | ok | 7615 | LF | 0a | `d7ca15b02298d973ff10d6cd39d36285fe45016fd443f49c8048c74af2b7991e` |
| `docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorerGoldenReauditTest.xml` | ok | 732 | CRLF | 0a | `1c1cb70ec3eca1d6a33f4a7b30bd92cfede60f8ad36bd87200f4f9838f4032b0` |
| `docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibilityTest.xml` | ok | 2031 | CRLF | 0a | `5f25df6ba7e4a27a1b7b4ec70090f5d3cd1951bcd251efce01fe832f9f882178` |
| `docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.evidence.FocusEvidenceStatePolicyTest.xml` | ok | 1629 | CRLF | 0a | `4725b187381573278641a496a13d923533ef75aae521cc9a0ed2ebaa8faf3de1` |
| `MINDBOOST_AUDIT_LEDGER.md` | ok | 391200 | LF | 0a | `2b8a56f304dd6c5a02290ca5b3bee9803f18e772185750bcdb9f2e0079179833` |

