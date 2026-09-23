import { ADMIN_DEFAULT_PAGE_SIZE, ADMIN_MAX_PAGE_SIZE } from "@/lib/admin-constants";

export type CacheFilters = {
  page?: number;
  pageSize?: number;
  q?: string;
  type?: string;
  provider?: string;
  expired?: boolean;
  sort?: string;
};

export function inferCacheType(id: string) {
  if (id.startsWith("discover-")) return "discover";
  if (id.includes("-search-")) return "search";
  if (id.includes("trending")) return "trending";
  return "detail";
}

export function parseBooleanFilter(value: string | null | undefined) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

export async function getCacheSummary() {
  return {
    totalEntries: 0,
    expiredEntries: 0,
    freshEntries: 0,
    oldestExpiredAgeSeconds: null,
    byProvider: {} as Record<string, number>,
    byType: {} as Record<string, number>,
    lastCleanupLog: null,
  };
}

export async function getPaginatedCacheEntries(filters: CacheFilters) {
  const page = Math.max(1, filters.page ?? 1);
  const pageSize = Math.min(Math.max(1, filters.pageSize ?? ADMIN_DEFAULT_PAGE_SIZE), ADMIN_MAX_PAGE_SIZE);

  return {
    items: [] as ReturnType<typeof serializeCacheEntry>[],
    pagination: {
      page,
      pageSize,
      total: 0,
      pageCount: 1,
    },
  };
}

export async function cleanupExpiredCache(_options?: { requestId?: string; userId?: string }) {
  return 0;
}

export function serializeCacheEntry(entry: any) {
  const now = new Date();
  return {
    id: entry?.id || "",
    key: entry?.id || "",
    provider: entry?.provider || "",
    type: inferCacheType(entry?.id || ""),
    createdAt: entry?.created_at ? new Date(entry.created_at) : now,
    updatedAt: entry?.updated_at ? new Date(entry.updated_at) : now,
    expiresAt: entry?.expires_at ? new Date(entry.expires_at) : now,
    expired: false,
    isExpired: false,
    ageSeconds: 0,
    expiresInSeconds: 0,
    payloadSizeBytes: 0,
  };
}
