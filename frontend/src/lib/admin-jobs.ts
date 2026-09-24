import { ADMIN_DEFAULT_PAGE_SIZE, ADMIN_MAX_PAGE_SIZE } from "@/lib/admin-constants";
import { apiFetch } from "@/lib/api-client";

export const JOB_STATUS = {
  PENDING: "pending",
  PROCESSING: "processing",
  COMPLETED: "completed",
  FAILED: "failed",
  CANCELLED: "cancelled",
} as const;

export type JobFilters = {
  page?: number;
  pageSize?: number;
  status?: string;
  type?: string;
  dedupeKey?: string;
  userId?: string;
  q?: string;
};

export const parsePositiveInt = (value: string | null | undefined, fallback: number) => {
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
};

export async function getPaginatedJobs(filters: JobFilters) {
  const boundedInt = (value: number | undefined, fallback: number, max: number) =>
    value !== undefined && Number.isFinite(value)
      ? Math.min(max, Math.max(1, Math.trunc(value))) : fallback;
  const pageSize = boundedInt(filters.pageSize, ADMIN_DEFAULT_PAGE_SIZE, ADMIN_MAX_PAGE_SIZE);
  const page = boundedInt(filters.page, 1, Math.floor(2147483647 / pageSize) + 1);
  const q = new URLSearchParams();
  for (const key of ["status", "type", "dedupeKey", "userId", "q"] as const) {
    const value = filters[key]?.trim();
    if (value) q.set(key, value);
  }
  q.set("page", String(page));
  q.set("limit", String(pageSize));

  const data = await apiFetch<{ jobs: any[]; total: number }>(`/admin/jobs?${q.toString()}`, { cache: "no-store" });
  const items = data.jobs.map(serializeJob);
  return {
    items,
    pagination: {
      page,
      pageSize,
      total: data.total,
      pageCount: Math.max(1, Math.ceil(data.total / pageSize)),
    },
  };
}

export type JobSummary = {
  pending: number;
  processing: number;
  failed: number;
  completedLastHour: number;
  oldestPendingAgeSeconds: number | null;
  stuckProcessing: number;
};

export async function getJobSummary(): Promise<JobSummary> {
  return apiFetch<JobSummary>("/admin/jobs/summary", { cache: "no-store" });
}

export type WorkerBatchResult = {
  ok: boolean;
  workerId: string;
  processed: number;
  completed: number;
  retried: number;
  failed: number;
};

export async function processWorkerBatch(options?: { requestId?: string; batchSize?: number }) {
  const batchSize = options?.batchSize ?? 10;
  if (!Number.isInteger(batchSize) || batchSize < 1 || batchSize > 100) {
    throw new Error("batchSize must be between 1 and 100");
  }
  return apiFetch<WorkerBatchResult>(`/admin/jobs/process?batchSize=${batchSize}`, {
    method: "POST",
    headers: options?.requestId ? { "X-Request-Id": options.requestId } : undefined,
    cache: "no-store",
  });
}

export function serializeJob(job: any) {
  const now = new Date();
  const parseDate = (d: any) => (d ? new Date(d) : null);
  const createdDate = parseDate(job.createdAt || job.created_at) || now;
  const runDate = parseDate(job.runAt || job.run_at) || now;

  return {
    id: String(job.id),
    type: String(job.type || ""),
    status: String(job.status || "pending"),
    dedupeKey: job.dedupeKey || job.dedupe_key || null,
    dedupe_key: job.dedupeKey || job.dedupe_key || null,
    attempts: Number(job.attempts ?? 0),
    maxAttempts: Number(job.maxAttempts || job.max_attempts || 3),
    max_attempts: Number(job.maxAttempts || job.max_attempts || 3),
    runAt: runDate,
    run_at: runDate,
    lockedAt: parseDate(job.lockedAt || job.locked_at),
    locked_at: parseDate(job.lockedAt || job.locked_at),
    lockedBy: job.lockedBy || job.locked_by || null,
    lastError: job.lastError || job.last_error || null,
    last_error: job.lastError || job.last_error || null,
    completedAt: parseDate(job.processedAt || job.processed_at || job.completedAt),
    processed_at: parseDate(job.processedAt || job.processed_at || job.completedAt),
    createdAt: createdDate,
    created_at: createdDate,
    updatedAt: parseDate(job.updatedAt || job.updated_at) || now,
    payload: job.payload,
  };
}
