import { apiFetch } from "@/lib/api-client";

export type Pagination = { page: number; pageSize: number; total: number; pageCount: number };
export type DiagnosticLog = {
  id: string; createdAt: string; level: string; event: string;
  message: string | null; operation: string | null; durationMs: number | null;
  requestId: string | null; userId: string | null; mediaId: string | null;
  mediaType: string | null; jobId: string | null;
  errorName: string | null; errorMessage: string | null; metadata: unknown;
};

export function readDiagnostics<T>(path: string, filters: object = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
  });
  return apiFetch<T>(`/admin/diagnostics/${path}?${query}`);
}

export async function getLogSummary() {
  const result = await readDiagnostics<{
    errorsLastHour: number; warningsLastHour: number;
    errorsLast24Hours: number; warningsLast24Hours: number;
    latestErrorLogs: DiagnosticLog[];
  }>("logs/summary");
  return { ...result, latestErrorLogs: result.latestErrorLogs.map(log => ({
    ...log, createdAt: new Date(log.createdAt),
  })) };
}

export function getDiagnosticLogs(filters: object) {
  return readDiagnostics<{ items: DiagnosticLog[]; pagination: Pagination }>("logs", filters);
}
