import assert from 'node:assert/strict';
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = fileURLToPath(new URL('../', import.meta.url));
function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? sourceFiles(path) : /\.tsx?$/.test(entry.name) ? [path] : [];
  });
}
const files = sourceFiles(join(root, 'src'));

test('provider integrations and credentials stay in the Java backend', () => {
  const providerApi = /api\.(?:themoviedb\.org|igdb\.com|rawg\.io|mangadex\.org|jikan\.moe|animethemes\.moe|malsync\.moe)|graphql\.anilist\.co|id\.twitch\.tv/;
  const providerSecret = /process\.env\.(?:TMDB_API_KEY|RAWG_API_KEY|TWITCH_CLIENT_ID|TWITCH_CLIENT_SECRET)/;
  for (const file of files) {
    const source = readFileSync(file, 'utf8');
    assert.ok(!providerApi.test(source), `Direct provider API reference in ${file}`);
    assert.ok(!providerSecret.test(source), `Provider credential in ${file}`);
  }
});

test('frontend has no database dependencies or legacy worker implementation', () => {
  const pkg = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'));
  for (const name of Object.keys({ ...pkg.dependencies, ...pkg.devDependencies })) {
    assert.ok(!/prisma|supabase|^pg$/.test(name), `Database dependency: ${name}`);
  }
  for (const path of ['src/lib/prisma.ts', 'src/lib/api-cache.ts', 'src/config/ranking.ts', 'src/app/api/worker/route.ts']) {
    assert.ok(!existsSync(join(root, path)), `Legacy backend implementation: ${path}`);
  }
});
