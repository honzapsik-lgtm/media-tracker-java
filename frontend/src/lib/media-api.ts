import { apiFetch, getMediaDetails, getSeasonEpisodes } from "@/lib/api-client";

// Adapt transport field names at the page boundary, without provider access.
export async function getMediaPageDetails(slug: string) {
  try {
    const media = await getMediaDetails(slug);
    return { ...media, studioData: media.studios || [], streamingLinks: media.externalLinks || [],
      mangadexId: media.id?.startsWith("mangadex-manga-") ? media.id.slice(15) : null };
  } catch (error) {
    if (error instanceof Error && error.message.includes("API Error [404]")) return null;
    throw error;
  }
}

export async function getMediaSeasonEpisodes(slug: string, season: number) {
  const episodes = await getSeasonEpisodes(slug, season);
  return episodes.map(ep => ({ ...ep, episode_number: ep.episodeNumber,
    air_date: ep.airDate, originalImage: ep.image }));
}

export function getSeasonThemes(slug: string, season: number) {
  return apiFetch<any>(`/media/${encodeURIComponent(slug)}/season/${season}/themes`);
}

export function getEpisodeCredits(slug: string, season: number, episode: number) {
  return apiFetch<{ cast: any[]; crew: any[] }>(`/media/${encodeURIComponent(slug)}/season/${season}/episode/${episode}/credits`);
}

export function getMediaRankings(ids: string[]) {
  return apiFetch<{ stats: Record<string, number>; ranks: Record<string, number> }>("/media/batch-stats", {
    method: "POST", body: JSON.stringify({ ids }),
  });
}

export function cleanStudioData(studios: any[], isAnime: boolean, isTv: boolean): any[] {
  const seen = new Set<string>();
  const unique = (studios || []).filter(studio => {
    const name = String(studio.name || studio).toLowerCase().replace(/[^a-z0-9]/g, "");
    if (!name || seen.has(name)) return false;
    seen.add(name);
    return true;
  });
  if (isTv) {
    const preferred = unique.filter(s => String(s.id || "").startsWith("tmdbnet-") !== isAnime);
    if (preferred.length) return preferred.slice(0, isAnime ? 3 : 2);
  }
  return unique.slice(0, 3);
}
