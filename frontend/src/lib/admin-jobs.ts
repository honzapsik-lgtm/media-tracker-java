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
  const page = Math.max(1, filters.page ?? 1);
  const pageSize = Math.min(Math.max(1, filters.pageSize ?? ADMIN_DEFAULT_PAGE_SIZE), ADMIN_MAX_PAGE_SIZE);

  try {
    const q = new URLSearchParams();
    if (filters.status) q.set("status", filters.status);
    q.set("page", String(page));
    q.set("limit", String(pageSize));

    const data = await apiFetch<{ jobs: any[]; total: number }>(`/admin/jobs?${q.toString()}`);
    const items = (data.jobs || []).map(serializeJob);
    const total = data.total ?? items.length;

    return {
      items,
      pagination: {
        page,
        pageSize,
        total,
        pageCount: Math.max(1, Math.ceil(total / pageSize)),
      },
    };
  } catch (error) {
    console.error("[getPaginatedJobs] Error fetching jobs from Spring Boot:", error);
    return {
      items: [],
      pagination: {
        page,
        pageSize,
        total: 0,
        pageCount: 1,
      },
    };
  }
}

export async function getJobSummary() {
  try {
    const [pendingRes, processingRes, failedRes] = await Promise.allSettled([
      apiFetch<{ jobs: any[]; total: number }>("/admin/jobs?status=pending&limit=1"),
      apiFetch<{ jobs: any[]; total: number }>("/admin/jobs?status=processing&limit=1"),
      apiFetch<{ jobs: any[]; total: number }>("/admin/jobs?status=failed&limit=1"),
    ]);

    const pending = pendingRes.status === "fulfilled" ? pendingRes.value.total ?? 0 : 0;
    const processing = processingRes.status === "fulfilled" ? processingRes.value.total ?? 0 : 0;
    const failed = failedRes.status === "fulfilled" ? failedRes.value.total ?? 0 : 0;

    return {
      pending,
      processing,
      failed,
      completedLastHour: 0,
      oldestPendingAgeSeconds: null,
      stuckProcessing: 0,
    };
  } catch {
    return {
      pending: 0,
      processing: 0,
      failed: 0,
      completedLastHour: 0,
      oldestPendingAgeSeconds: null,
      stuckProcessing: 0,
    };
  }
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
