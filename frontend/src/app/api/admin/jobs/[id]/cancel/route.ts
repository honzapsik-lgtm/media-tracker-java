import { NextResponse } from "next/server";
import { adminErrorResponse } from "@/lib/admin-api";
import { AdminAuthError, requireAdmin } from "@/lib/admin-auth";
import { apiFetch } from "@/lib/api-client";
import { appLog } from "@/lib/logger";
import { getOrCreateRequestId } from "@/lib/request-id";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const requestId = getOrCreateRequestId(request.headers);
  const { id } = await params;

  try {
    const admin = await requireAdmin();
    const result = await apiFetch<any>(`/admin/jobs/${encodeURIComponent(id)}/cancel`, { method: "POST" });
    await appLog({
      level: "info",
      event: "admin.job.cancel",
      requestId,
      userId: admin.id,
      jobId: id,
      metadata: result,
      persist: true,
    });
    return NextResponse.json({ ok: true, jobId: id });
  } catch (error) {
    await appLog({
      level: error instanceof AdminAuthError ? "warn" : "error",
      event: "admin.job.cancel_failed",
      requestId,
      jobId: id,
      error,
      persist: true,
    });
    return adminErrorResponse(error, requestId);
  }
}
