import { ADMIN_DEFAULT_PAGE_SIZE, ADMIN_MAX_PAGE_SIZE } from "@/lib/admin-constants";
import { apiFetch } from "@/lib/api-client";

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
  try {
    const summary = await apiFetch<any>("/admin/cache/summary");
    return {
      totalEntries: Number(summary.totalEntries ?? 0),
      expiredEntries: Number(summary.expiredEntries ?? 0),
      freshEntries: Number(summary.freshEntries ?? 0),
      oldestExpiredAgeSeconds: summary.oldestExpiredAgeSeconds ?? null,
      byProvider: (summary.byProvider ?? {}) as Record<string, number>,
      byType: (summary.byType ?? {}) as Record<string, number>,
      lastCleanupLog: summary.lastCleanupLog ?? null,
    };
  } catch (err) {
    console.error("[getCacheSummary] Error fetching cache summary:", err);
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
  try {
    const res = await apiFetch<{ deleted: number }>("/admin/cache/cleanup", { method: "POST" });
    return res.deleted ?? 0;
  } catch (err) {
    console.error("[cleanupExpiredCache] Error:", err);
    return 0;
  }
}

export function serializeCacheEntry(entry: any) {
  const now = new Date();
  return {
    id: entry?.id || "",
    key: entry?.id || entry?.key || "",
    provider: entry?.provider || "unknown",
    createdAt: entry?.createdAt ? new Date(entry.createdAt) : now,
    updatedAt: entry?.updatedAt ? new Date(entry.updatedAt) : (entry?.createdAt ? new Date(entry.createdAt) : now),
    expiresAt: entry?.expiresAt ? new Date(entry.expiresAt) : now,
    isExpired: entry?.expiresAt ? new Date(entry.expiresAt) < now : false,
    expired: entry?.expiresAt ? new Date(entry.expiresAt) < now : false,
    payloadSizeBytes: Number(entry?.payloadSizeBytes ?? entry?.size ?? 0),
    type: inferCacheType(entry?.id || ""),
  };
}
