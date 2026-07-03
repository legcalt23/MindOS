#!/usr/bin/env node
/**
 * Static Storage Rules guardrails.
 *
 * Production invariants are checked here. Dynamic tests run the same rules after replacing
 * only the canonical request.app guard because the Storage emulator does not expose App Check.
 */

'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const rulesPath = path.join(ROOT, 'storage.rules');
const rules = fs.readFileSync(rulesPath, 'utf8');
const dynamicHarness = fs.readFileSync(path.join(ROOT, 'storage.dm.rules.test.mjs'), 'utf8');

const checks = [
  {
    name: 'validVisibility only allows private',
    pass: /function\s+validVisibility\s*\(\s*value\s*\)\s*{[\s\S]*?return\s+value\s*==\s*"private"\s*;[\s\S]*?}/.test(rules)
      && !rules.includes('value == "public"')
      && !rules.includes('visibility == "public"'),
  },
  {
    name: 'no public resource helpers remain',
    pass: !/\b(resourcePublic|requestPublic)\s*\(/.test(rules),
  },
  {
    name: 'owner-scoped media reads require App Check and private resources',
    pass: /function\s+canReadOwnerScopedMedia\s*\([^)]*\)\s*{[\s\S]*?resourcePrivate\(\)[\s\S]*?signedIn\(\)[\s\S]*?isAppChecked\(\)[\s\S]*?}/.test(rules),
  },
  {
    name: 'DM reads require App Check',
    pass: /function\s+canAccessDMResource\s*\([^)]*\)\s*{[\s\S]*?signedIn\(\)\s*&&\s*isAppChecked\(\)[\s\S]*?}/.test(rules),
  },
  {
    name: 'DM uploads require App Check',
    pass: /function\s+canCreateDMMedia\s*\([^)]*\)\s*{[\s\S]*?signedIn\(\)\s*&&\s*isAppChecked\(\)[\s\S]*?}/.test(rules),
  },
  {
    name: 'post attachment reads are bound to existing post owner',
    pass: rules.includes('firestore.exists(postPath(postId))')
      && rules.includes('firestore.get(postPath(postId)).data.userId == userId'),
  },
  {
    name: 'profile thumbnails cannot fall through to the generic image limit',
    pass: rules.includes("!fileName.matches('^[0-9]+_thumb\\\\.jpg$')")
      && /!fileName\.matches\([^)]*thumb[^)]*\)\s*&&\s*validImage\(fileName\)/.test(rules),
  },
  {
    name: 'all mutable media creates require a missing existing resource',
    // profile_visuals + post_attachments + dm_media... + profile_cards_private/public media = 5
    pass: (rules.match(/allow\s+create:\s+if\s+resource\s*==\s*null/g) ?? []).length === 5,
  },
  {
    name: 'dynamic harness substitutes only the canonical App Check guard',
    pass: dynamicHarness.includes('const productionGuard = "return request.app != null;"')
      && /assert\.equal\(\s*occurrences\s*,/.test(dynamicHarness)
      && !dynamicHarness.includes('storage.test.rules'),
  },
  {
    name: 'dynamic harness pins the required assertion count',
    pass: dynamicHarness.includes('const EXPECTED_DYNAMIC_ASSERTIONS = 44;')
      && dynamicHarness.includes('assert.equal(assertionCount, EXPECTED_DYNAMIC_ASSERTIONS)'),
  },
];

const failed = checks.filter((check) => !check.pass);

if (failed.length > 0) {
  console.error('Storage rules static checks failed:');
  for (const check of failed) {
    console.error(`  - ${check.name}`);
  }
  process.exit(1);
}

console.log(`✓ storage.rules static checks passed (${checks.length} checks).`);
