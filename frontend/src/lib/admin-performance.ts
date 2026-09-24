import { readDiagnostics, type DiagnosticLog, type Pagination } from "@/lib/admin-diagnostics";

export type PerformanceFilters = {
  page?: number; pageSize?: number; operation?: string;
  userId?: string; mediaId?: string; sinceHours?: number;
};

export function serializeSlowOperation(log: DiagnosticLog) {
  return { ...log, createdAt: new Date(log.createdAt) };
}

type PerformanceSummary = {
  lastHour: number; last24Hours: number;
  slowestLast24Hours: DiagnosticLog | null;
  topOperationsLast24Hours: Array<{ operation: string; count: number }>;
};

export function getPerformanceSummary() {
  return readDiagnostics<PerformanceSummary>("performance/summary");
}

export async function getRecentSlowOperations(filters: PerformanceFilters) {
  const result = await readDiagnostics<{
    items: DiagnosticLog[]; pagination: Pagination; summary: PerformanceSummary;
  }>("performance", filters);
  return { ...result, items: result.items.map(serializeSlowOperation) };
}
