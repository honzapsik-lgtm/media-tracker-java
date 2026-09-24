import { readDiagnostics, type DiagnosticLog, type Pagination } from "@/lib/admin-diagnostics";
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

export function parseBooleanFilter(value: string | null | undefined) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

export function getCacheSummary() {
  return readDiagnostics<{
    totalEntries: number; expiredEntries: number; freshEntries: number;
    oldestExpiredAgeSeconds: number | null; byProvider: Record<string, number>;
    byType: Record<string, number>; lastCleanupLog: DiagnosticLog | null;
  }>("cache/summary");
}

export async function getPaginatedCacheEntries(filters: CacheFilters) {
  const result = await readDiagnostics<{
    items: Parameters<typeof serializeCacheEntry>[0][]; pagination: Pagination;
  }>("cache", filters);
  return { ...result, items: result.items.map(serializeCacheEntry) };
}

export async function cleanupExpiredCache(options?: { requestId?: string }) {
  const res = await apiFetch<{ deleted: number }>("/admin/cache/cleanup", {
    method: "POST",
    headers: options?.requestId ? { "X-Request-Id": options.requestId } : undefined,
  });
  return res.deleted;
}

type CacheEntry = {
  id: string; key: string; provider: string; createdAt: string;
  updatedAt: string; expiresAt: string; expired: boolean;
  payloadSizeBytes: number; type: string;
};

export function serializeCacheEntry(entry: CacheEntry) {
  return {
    ...entry,
    createdAt: new Date(entry.createdAt),
    updatedAt: new Date(entry.updatedAt),
    expiresAt: new Date(entry.expiresAt),
    isExpired: entry.expired,
  };
}
