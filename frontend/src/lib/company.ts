import type { UnifiedCompanyProfile } from '@/types/company';
import { apiFetch } from '@/lib/api-client';

export function parseCompanySlug(slug: string): [string, number] | null {
  const provider = slug.match(/^(tmdb|tmdbnet|anilist|igdb|mal)(?:-[a-z]+)?-(\d+)$/i);
  return provider ? [provider[1].toLowerCase(), parseInt(provider[2], 10)] : null;
}

export async function getUnifiedCompanyProfile(slug: string): Promise<UnifiedCompanyProfile | null> {
  const parsed = parseCompanySlug(slug);
  if (!parsed) return null;
  try {
    return await apiFetch<UnifiedCompanyProfile>(`/company/${encodeURIComponent(parsed.join('-'))}`);
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('API Error [404]')) return null;
    throw error;
  }
}
