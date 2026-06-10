export function isProductionFirebaseProject(env: NodeJS.ProcessEnv = process.env): boolean {
  if (env['GCLOUD_PROJECT'] === 'mindos-8452f') return true;
  const config = env['FIREBASE_CONFIG'];
  if (!config) return false;
  try {
    const parsed = JSON.parse(config) as { projectId?: string };
    return parsed.projectId === 'mindos-8452f';
  } catch {
    return false;
  }
}
