#!/usr/bin/env node
/**
 * Static Storage Rules guardrails.
 *
 * The Storage emulator used in this project does not populate request.app for App Check,
 * so App-Check-gated Storage rules cannot be exercised reliably in CI. This script checks
 * the production invariants that must never regress.
 */

'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const rulesPath = path.join(ROOT, 'storage.rules');
const rules = fs.readFileSync(rulesPath, 'utf8');

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
