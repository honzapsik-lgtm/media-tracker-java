import { readDiagnostics } from "@/lib/admin-diagnostics";

export type IntegrityCheckResult = {
  id: string; label: string; severity: "ok" | "warning" | "error";
  count: number; detail: string; href?: string;
};

export function getDatabaseSummary() {
  return readDiagnostics<{
    users: number; accounts: number; sessions: number; ratings: number;
    ratingsWithReviewText: number; deepReviews: number; watchlistEntries: number;
    mediaStats: number; userBadges: number; userStatsCache: number; apiCache: number;
    backgroundJobsByStatus: Record<string, number>; systemLogsByLevel: Record<string, number>;
  }>("database/summary");
}

export async function getUsersMissingStatsCacheCount(): Promise<number> {
  return (await runDatabaseIntegrityChecks()).find(check => check.id === "users_missing_stats_cache")!.count;
}

export async function getMediaStatsMismatchCount(): Promise<number> {
  return (await runDatabaseIntegrityChecks()).find(check => check.id === "media_stats_inconsistencies")!.count;
}

export function runDatabaseIntegrityChecks(): Promise<IntegrityCheckResult[]> {
  return readDiagnostics<IntegrityCheckResult[]>("database/integrity");
}

export function countRowsByName(rows: Array<{ name: string; count: bigint | number }>) {
  return Object.fromEntries(rows.map((row) => [row.name, Number(row.count)]));
}
