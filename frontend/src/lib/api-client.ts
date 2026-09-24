import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint.startsWith("/") ? "" : "/"}${endpoint}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  // Attach gateway secret and auth if running on the server
  if (typeof window === "undefined") {
    const gatewaySecret = process.env.INTERNAL_GATEWAY_SECRET || "default-internal-secret-change-in-prod-123456";
    headers.set("X-Internal-Gateway-Key", gatewaySecret);

    if (!headers.has("Authorization") && !headers.has("X-User-Id")) {
      try {
        const session = await getServerSession(authOptions);
        if (session?.user?.id) {
          headers.set("X-User-Id", session.user.id);
          if (session.user.role) {
            headers.set("X-User-Role", session.user.role);
          }
          if (session.user.email) {
            headers.set("X-User-Email", session.user.email);
          }
        }
      } catch {
        // Ignore if session is not available
      }
    }
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorDetail = response.statusText;
    try {
      const errJson = await response.json();
      errorDetail = errJson.message || errJson.error || errorDetail;
    } catch {
      // Ignore
    }
    throw new Error(`API Error [${response.status}] ${url}: ${errorDetail}`);
  }

  // If response is 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

// Media API
export async function getMediaDetails(slug: string) {
  return apiFetch<any>(`/media/${encodeURIComponent(slug)}`);
}

export async function getSeasonEpisodes(slug: string, seasonNumber: number) {
  return apiFetch<any[]>(`/media/${encodeURIComponent(slug)}/season/${seasonNumber}`);
}

// Discover & Search API
export async function discoverMedia(
  type = "movie",
  genre = "",
  year = "",
  sort = "popular",
  page = 1
) {
  const params = new URLSearchParams({ type, genre, year, sort, page: String(page) });
  return apiFetch<any[]>(`/discover?${params.toString()}`);
}

export async function getRecommendations() {
  return apiFetch<any>("/discover/recommendations");
}

export async function searchMedia(query: string) {
  return apiFetch<any[]>(`/search?q=${encodeURIComponent(query)}`);
}

// Ratings API
export async function getRatings(mediaId?: string, prefix?: string) {
  const params = new URLSearchParams();
  if (mediaId) params.set("mediaId", mediaId);
  if (prefix) params.set("prefix", prefix);
  return apiFetch<any>(`/ratings?${params.toString()}`);
}

export async function saveRating(body: {
  mediaId: string;
  score: number;
  isDeepReview?: boolean;
  criteriaScores?: Record<string, number>;
  reviewText?: string;
  mediaTitle?: string;
  mediaImage?: string | null;
  mediaReleaseDate?: string | null;
}) {
  return apiFetch<any>("/ratings", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function deleteRating(mediaId: string) {
  return apiFetch<{ success: boolean }>(`/ratings?mediaId=${encodeURIComponent(mediaId)}`, {
    method: "DELETE",
  });
}

export async function getFriendRatings(mediaId: string) {
  return apiFetch<any[]>(`/ratings/friends?mediaId=${encodeURIComponent(mediaId)}`);
}

// Watchlist API
export async function getWatchlist(params: {
  userId?: string;
  media_type?: string;
  status?: string;
  page?: number;
  limit?: number;
}) {
  const q = new URLSearchParams();
  if (params.userId) q.set("userId", params.userId);
  if (params.media_type) q.set("media_type", params.media_type);
  if (params.status) q.set("status", params.status);
  if (params.page) q.set("page", String(params.page));
  if (params.limit) q.set("limit", String(params.limit));
  return apiFetch<{ results: any[]; count: number }>(`/watchlist?${q.toString()}`);
}

export async function upsertWatchlist(body: {
  mediaId: string;
  title?: string;
  image?: string | null;
  mediaType?: string;
  status?: string;
  episodesWatched?: number;
  chaptersRead?: number;
  volumesRead?: number;
  hoursPlayed?: number;
  platform?: string | null;
  watchCount?: number;
}) {
  return apiFetch<any>("/watchlist", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function deleteWatchlistItem(mediaId: string) {
  return apiFetch<{ success: boolean }>(`/watchlist?mediaId=${encodeURIComponent(mediaId)}`, {
    method: "DELETE",
  });
}

// Custom Lists API
export async function getUserLists(mediaType?: string) {
  const q = mediaType ? `?media_type=${encodeURIComponent(mediaType)}` : "";
  return apiFetch<{ lists: any[] }>(`/lists${q}`);
}

export async function createList(title: string, mediaType: string) {
  return apiFetch<{ list: any }>("/lists", {
    method: "POST",
    body: JSON.stringify({ title, media_type: mediaType }),
  });
}

export async function getListById(id: string) {
  return apiFetch<any>(`/lists/${encodeURIComponent(id)}`);
}

export async function updateListItems(id: string, mediaIds: any[]) {
  return apiFetch<{ success: boolean }>(`/lists/${encodeURIComponent(id)}/items`, {
    method: "POST",
    body: JSON.stringify({ mediaIds }),
  });
}

export async function deleteList(id: string) {
  return apiFetch<{ success: boolean }>(`/lists/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

// Friends & Activity API
export async function getFriends() {
  return apiFetch<{ friends: any[]; pendingReceived: any[]; pendingSent: any[] }>("/friends");
}

export async function sendFriendRequest(target: string) {
  return apiFetch<any>("/friends", {
    method: "POST",
    body: JSON.stringify({ target }),
  });
}

export async function respondToFriendRequest(id: string, action: "accept" | "decline" | "block") {
  return apiFetch<{ success: boolean }>(`/friends/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify({ action }),
  });
}

export async function removeFriend(id: string) {
  return apiFetch<{ success: boolean }>(`/friends/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export async function updateFriendPreferences(
  friendId: string,
  hideActivity?: boolean,
  hideRatings?: boolean
) {
  return apiFetch<any>("/friends/preferences", {
    method: "PATCH",
    body: JSON.stringify({ friendId, hideActivity, hideRatings }),
  });
}

export async function getActivityFeed(scope = "friends", page = 1, limit = 50) {
  return apiFetch<{ activities: any[] }>(`/activity?scope=${scope}&page=${page}&limit=${limit}`);
}

// Profile API
export async function getProfile(username: string) {
  return apiFetch<any>(`/profile/${encodeURIComponent(username)}`);
}

export async function checkUsernameAvailability(username: string) {
  return apiFetch<{ available: boolean }>(`/profile/username/available?username=${encodeURIComponent(username)}`);
}

export async function updateUsername(username: string) {
  return apiFetch<{ success: boolean; user: any }>("/profile/username", {
    method: "POST",
    body: JSON.stringify({ username }),
  });
}

export async function updateProfile(body: {
  realName?: string;
  stateRegion?: string;
  country?: string;
  showcaseBadges?: string[];
}) {
  return apiFetch<any>("/profile", {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export async function getRankings(type = "SHOW", sort = "list_rank", page = 1, limit = 20) {
  const q = new URLSearchParams({ type, sort, page: String(page), limit: String(limit) });
  return apiFetch<{ results: any[]; count: number }>(`/rankings?${q.toString()}`);
}
