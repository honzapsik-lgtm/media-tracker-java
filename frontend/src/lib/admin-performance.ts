import { ADMIN_DEFAULT_PAGE_SIZE, ADMIN_MAX_PAGE_SIZE } from "@/lib/admin-constants";

export type PerformanceFilters = {
  page?: number;
  pageSize?: number;
  operation?: string;
  userId?: string;
  mediaId?: string;
  sinceHours?: number;
};

export function serializeSlowOperation(log: any) {
  return {
    id: log?.id,
    createdAt: log?.createdAt,
    operation: log?.operation || null,
    durationMs: log?.durationMs,
    level: log?.level,
    message: log?.message,
    requestId: log?.requestId,
    userId: log?.userId,
    mediaId: log?.mediaId,
    mediaType: log?.mediaType,
    metadata: log?.metadata,
  };
}

export async function getPerformanceSummary() {
  return {
    lastHour: 0,
    last24Hours: 0,
    slowestLast24Hours: null as ReturnType<typeof serializeSlowOperation> | null,
    topOperationsLast24Hours: [] as Array<{ operation: string; count: number }>,
  };
}

export async function getRecentSlowOperations(filters: PerformanceFilters) {
  const page = Math.max(1, filters.page ?? 1);
  const pageSize = Math.min(Math.max(1, filters.pageSize ?? ADMIN_DEFAULT_PAGE_SIZE), ADMIN_MAX_PAGE_SIZE);

  return {
    items: [] as ReturnType<typeof serializeSlowOperation>[],
    pagination: {
      page,
      pageSize,
      total: 0,
      pageCount: 1,
    },
    summary: await getPerformanceSummary(),
  };
}
