import { getCacheSummary } from "@/lib/admin-cache";
import { ADMIN_STUCK_JOB_MINUTES, PERF_SLOW_THRESHOLD_MS } from "@/lib/admin-constants";
import { getJobSummary } from "@/lib/admin-jobs";
import { getPerformanceSummary } from "@/lib/admin-performance";

export type AdminOverviewWarning = {
  severity: "warning" | "error";
  label: string;
  detail: string;
  href: string;
};

export async function getAdminOverview() {
  const [jobs, cache, performance] = await Promise.all([
    getJobSummary(),
    getCacheSummary(),
    getPerformanceSummary(),
  ]);

  const warnings: AdminOverviewWarning[] = [];

  if (jobs.failed > 0) {
    warnings.push({
      severity: "warning",
      label: "Failed jobs exist",
      detail: `There are ${jobs.failed} failed background jobs.`,
      href: "/admin/jobs?status=failed",
    });
  }

  if (jobs.stuckProcessing > 0) {
    warnings.push({
      severity: "error",
      label: "Stuck processing jobs",
      detail: `${jobs.stuckProcessing} jobs have been processing for more than ${ADMIN_STUCK_JOB_MINUTES} minutes.`,
      href: "/admin/jobs?status=processing",
    });
  }

  if (jobs.oldestPendingAgeSeconds != null && jobs.oldestPendingAgeSeconds > 15 * 60) {
    warnings.push({
      severity: "warning",
      label: "Old pending job",
      detail: `The oldest pending job has waited ${jobs.oldestPendingAgeSeconds} seconds.`,
      href: "/admin/jobs?status=pending",
    });
  }

  if ((performance.slowestLast24Hours?.durationMs ?? 0) >= PERF_SLOW_THRESHOLD_MS) {
    warnings.push({
      severity: "error",
      label: "Very slow operation",
      detail: `${performance.slowestLast24Hours?.operation ?? "unknown"} took ${performance.slowestLast24Hours?.durationMs}ms.`,
      href: "/admin/performance?sinceHours=24",
    });
  }

  return {
    jobs: {
      ...jobs,
      cancelled: 0,
    },
    logs: {
      errorsLastHour: 0,
      warningsLastHour: 0,
      errorsLast24Hours: 0,
      warningsLast24Hours: 0,
      latestErrorLogs: [] as any[],
    },
    cache,
    database: {
      users: 0,
      ratings: 0,
      ratingsWithReviews: 0,
      watchlistEntries: 0,
      mediaStatsRows: 0,
      userStatsCacheRows: 0,
      badges: 0,
      systemLogCount: 0,
      backgroundJobCount: 0,
    },
    performance,
    warnings,
  };
}
