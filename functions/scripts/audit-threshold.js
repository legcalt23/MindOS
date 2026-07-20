#!/usr/bin/env node

const severityRank = {
  info: 0,
  low: 1,
  moderate: 2,
  high: 3,
  critical: 4,
};

const thresholdName = process.argv[2] || 'high';
const threshold = severityRank[thresholdName];

if (threshold == null) {
  console.error(`Unknown audit threshold: ${thresholdName}`);
  process.exit(2);
}

let input = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', chunk => {
  input += chunk;
});

process.stdin.on('end', () => {
  const report = JSON.parse(input || '{}');
  const vulnerabilities = Object.values(report.vulnerabilities || {});
  const blocking = vulnerabilities.filter(vulnerability => {
    const severity = vulnerability && vulnerability.severity;
    return severityRank[severity] >= threshold;
  });
  const counts = report.metadata?.vulnerabilities || {};

  console.log(
    `audit-threshold: info=${counts.info || 0} low=${counts.low || 0} ` +
      `moderate=${counts.moderate || 0} high=${counts.high || 0} ` +
      `critical=${counts.critical || 0} threshold=${thresholdName}`,
  );

  if (blocking.length > 0) {
    blocking.forEach(vulnerability => {
      console.error(`blocking vulnerability: ${vulnerability.name} (${vulnerability.severity})`);
    });
    process.exit(1);
  }
});
