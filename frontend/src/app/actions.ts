"use server";

import * as api from "@/lib/api-client";

export interface DiscoverMediaItem {
  id: string;
  title: string;
  image: string;
  type: string;
  globalScore: number;
  releaseDate?: string | null;
}

export async function getSeasonEpisodes(tvId: string, seasonNumber: number) {
  try {
    const slug = tvId.startsWith("tmdb-") ? tvId : `tmdb-show-${tvId}`;
    return await api.getSeasonEpisodes(slug, seasonNumber);
  } catch (error) {
    console.error("[actions.getSeasonEpisodes] Failed to fetch episodes from Spring Boot:", error);
    return [];
  }
}

export async function discoverMedia(
  type: string,
  genre: string,
  year: string,
  sort: string
): Promise<DiscoverMediaItem[]> {
  try {
    const items = await api.discoverMedia(type, genre, year, sort);
    return (items || []).map((item: any) => ({
      id: item.id || "",
      title: item.title || "Untitled",
      image: item.image || "",
      type: item.type || type,
      globalScore: typeof item.globalScore === "number" ? item.globalScore : 0,
      releaseDate: item.releaseDate || null,
    }));
  } catch (error) {
    console.error("[actions.discoverMedia] Failed to fetch discover media from Spring Boot:", error);
    return [];
  }
}
