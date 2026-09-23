import { NextResponse } from "next/server";
import { adminErrorResponse, confirmationRequiredResponse, hasConfirmation, readAdminPostBody } from "@/lib/admin-api";
import { AdminAuthError, requireAdmin } from "@/lib/admin-auth";
import { ADMIN_COMPLETED_JOB_CLEANUP_DAYS, ADMIN_JOB_CLEANUP_CONFIRM_TEXT } from "@/lib/admin-constants";
import { apiFetch } from "@/lib/api-client";
import { appLog } from "@/lib/logger";
import { getOrCreateRequestId } from "@/lib/request-id";

export async function POST(request: Request) {
  const requestId = getOrCreateRequestId(request.headers);

  try {
    const admin = await requireAdmin();
    const body = await readAdminPostBody(request) as { olderThanDays?: number | string };
    if (!hasConfirmation(body, ADMIN_JOB_CLEANUP_CONFIRM_TEXT)) {
      await appLog({
        level: "warn",
        event: "admin.job.cleanup_confirmation_missing",
        requestId,
        userId: admin.id,
        persist: true,
      });
      return confirmationRequiredResponse(requestId);
    }

    const requestedDays = Number(body.olderThanDays);
    const olderThanDays = Number.isFinite(requestedDays) && requestedDays > 0
      ? requestedDays
      : ADMIN_COMPLETED_JOB_CLEANUP_DAYS;

    const res = await apiFetch<{ deletedCount: number }>(`/admin/jobs/cleanup?olderThanDays=${olderThanDays}`, {
      method: "POST",
    });

    await appLog({
      level: "info",
      event: "admin.job.cleanup",
      requestId,
      userId: admin.id,
      metadata: { olderThanDays, deletedCount: res.deletedCount },
      persist: true,
    });
    return NextResponse.json({ ok: true, deletedCount: res.deletedCount });
  } catch (error) {
    await appLog({
      level: error instanceof AdminAuthError ? "warn" : "error",
      event: "admin.job.cleanup_failed",
      requestId,
      error,
      persist: true,
    });
    return adminErrorResponse(error, requestId);
  }
}
