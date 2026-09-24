import type { UnifiedProfile } from '@/types/person';
import { apiFetch } from '@/lib/api-client';

export function parsePersonSlug(slug: string): [string, string] | null {
  if (!slug) return null;
  const lower = slug.toLowerCase().trim();
  const uuid = lower.match(/^(?:mangadex-)?([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$/);
  if (uuid) return ['mangadex', uuid[1]];
  const provider = lower.match(/^(tmdb|anilist|igdb|mal|rawg)(?:-[a-z]+)?-(\d+)$/);
  if (provider) return [provider[1], provider[2]];
  return /^\d+$/.test(lower) ? ['tmdb', lower] : null;
}

export async function getUnifiedPersonProfile(slug: string): Promise<UnifiedProfile | null> {
  const parsed = parsePersonSlug(slug);
  if (!parsed) return null;
  try {
    return await apiFetch<UnifiedProfile>(`/person/${encodeURIComponent(parsed.join('-'))}`);
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('API Error [404]')) return null;
    throw error;
  }
}
