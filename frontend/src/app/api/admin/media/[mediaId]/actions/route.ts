import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { adminErrorResponse, readAdminPostBody } from "@/lib/admin-api";
import { apiFetch } from "@/lib/api-client";
import { getOrCreateRequestId } from "@/lib/request-id";

export async function POST(request: Request, { params }: { params: Promise<{ mediaId: string }> }) {
  const requestId = getOrCreateRequestId(request.headers);
  try {
    await requireAdmin();
    const { mediaId } = await params;
    const body = await readAdminPostBody(request);
    if (!body || (body.action !== "clear-cache" && body.action !== "refresh-stats")) {
      return NextResponse.json({ error: "Invalid media action", requestId }, { status: 400 });
    }
    const data = await apiFetch<{ success: boolean; deletedCount?: number }>(
      `/admin/media/${encodeURIComponent(mediaId)}/actions`, {
        method: "POST",
        headers: { "X-Request-Id": requestId },
        body: JSON.stringify({ action: body.action }),
      }
    );
    return NextResponse.json(data);
  } catch (error) {
    return adminErrorResponse(error, requestId);
  }
}
