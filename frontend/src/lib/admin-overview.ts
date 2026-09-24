import { getDatabaseSummary } from "@/lib/admin-database";
import { getLogSummary } from "@/lib/admin-diagnostics";
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
  const [jobs, cache, performance, database, logs] = await Promise.all([
    getJobSummary(),
    getCacheSummary(),
    getPerformanceSummary(),
    getDatabaseSummary(),
    getLogSummary(),
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
      cancelled: database.backgroundJobsByStatus.cancelled ?? 0,
    },
    logs,
    cache,
    database: {
      users: database.users,
      ratings: database.ratings,
      ratingsWithReviews: database.ratingsWithReviewText,
      watchlistEntries: database.watchlistEntries,
      mediaStatsRows: database.mediaStats,
      userStatsCacheRows: database.userStatsCache,
      badges: database.userBadges,
      systemLogCount: Object.values(database.systemLogsByLevel).reduce((sum, count) => sum + count, 0),
      backgroundJobCount: Object.values(database.backgroundJobsByStatus).reduce((sum, count) => sum + count, 0),
    },
    performance,
    warnings,
  };
}
