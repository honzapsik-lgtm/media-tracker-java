import { NextResponse } from "next/server";
import { revalidatePath } from "next/cache";
import { requireAdmin, AdminAuthError } from "@/lib/admin-auth";
import * as api from "@/lib/api-client";
import { appLog } from "@/lib/logger";
import { getOrCreateRequestId } from "@/lib/request-id";
import { adminErrorResponse } from "@/lib/admin-api";

export async function POST(request: Request) {
  const requestId = getOrCreateRequestId(request.headers);
  try {
    const admin = await requireAdmin();
    const data = await api.apiFetch<{ deleted: number }>("/admin/cache/flush-all", { method: "POST" });

    await appLog({
      level: "info",
      event: "admin.cache.flush_all",
      requestId,
      userId: admin.id,
      metadata: { deletedCount: data.deleted },
    });

    revalidatePath("/admin/cache");
    return NextResponse.json({ ok: true, deletedCount: data.deleted });
  } catch (error) {
    await appLog({
      level: error instanceof AdminAuthError ? "warn" : "error",
      event: error instanceof AdminAuthError ? "admin.cache.flush_all_denied" : "admin.cache.flush_all_failed",
      requestId,
      error,
    });
    return adminErrorResponse(error, requestId);
  }
}
