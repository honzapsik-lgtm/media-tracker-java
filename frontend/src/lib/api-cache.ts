import { PERF_WARN_THRESHOLD_MS } from "@/lib/admin-constants";
import { inferCacheType } from "@/lib/admin-cache";
import { timeOperation } from "@/lib/logger";

function safeCacheKey(id: string) {
  return id.length > 120 ? `${id.slice(0, 120)}...[TRUNCATED]` : id;
}

const inMemoryCache = new Map<string, { data: unknown; expiresAt: number }>();

export async function readApiCache<T>(id: string): Promise<T | null> {
  const item = inMemoryCache.get(id);
  if (!item || item.expiresAt <= Date.now()) return null;
  return item.data as T;
}

export async function writeApiCache(
  id: string,
  _provider: string,
  data: unknown,
  ttlSeconds: number
) {
  inMemoryCache.set(id, {
    data,
    expiresAt: Date.now() + ttlSeconds * 1000,
  });
}

export async function timeProviderFetch<T>({
  provider,
  cacheId,
  operation,
  fetcher,
}: {
  provider: string;
  cacheId: string;
  operation: string;
  fetcher: () => Promise<T>;
}) {
  return timeOperation({
    event: "provider.fetch",
    slowThresholdMs: PERF_WARN_THRESHOLD_MS,
    metadata: {
      source: operation,
      provider,
      cacheKey: safeCacheKey(cacheId),
      cacheType: inferCacheType(cacheId),
      cacheHit: false,
    },
  }, fetcher);
}
