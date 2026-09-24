import { NextResponse } from "next/server";
import { revalidatePath } from "next/cache";
import { adminErrorResponse } from "@/lib/admin-api";
import { AdminAuthError, requireAdmin } from "@/lib/admin-auth";
import { appLog } from "@/lib/logger";
import { getOrCreateRequestId } from "@/lib/request-id";
import * as api from "@/lib/api-client";

export async function POST(request: Request) {
  const requestId = getOrCreateRequestId(request.headers);

  try {
    const admin = await requireAdmin();
    await appLog({
      level: "info",
      event: "admin.ranking.process_requested",
      requestId,
      userId: admin.id,
      persist: true,
    });
    
    // Call Spring Boot backend to calculate PageRank aggregation across all media types
    await api.apiFetch("/admin/ranking", { method: "POST" });

    // Globally invalidate the Next.js cache so the UI reflects the new ranks immediately
    revalidatePath("/", "layout");

    return NextResponse.redirect(new URL("/admin?message=ranking_completed", request.url), 303);
  } catch (error) {
    await appLog({
      level: error instanceof AdminAuthError ? "warn" : "error",
      event: "admin.ranking.process_failed",
      requestId,
      error,
      persist: true,
    });
    return adminErrorResponse(error, requestId);
  }
}
