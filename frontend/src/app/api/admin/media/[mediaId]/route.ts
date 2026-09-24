import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import * as api from "@/lib/api-client";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ mediaId: string }> }
) {
  try {
    await requireAdmin();
    const { mediaId } = await params;
    const data = await api.apiFetch<any>(`/admin/media/${encodeURIComponent(mediaId)}`);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/admin/media/[mediaId] GET] Error:", error);
    return NextResponse.json(
      { error: error.message || "Failed to fetch media diagnostics" },
      { status: error.status || 500 }
    );
  }
}
