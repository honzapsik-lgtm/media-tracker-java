import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import * as api from "@/lib/api-client";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ userId: string }> }
) {
  try {
    await requireAdmin();
    const { userId } = await params;
    const data = await api.apiFetch<any>(`/admin/users/${encodeURIComponent(userId)}`);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/admin/users/[userId] GET] Error:", error);
    return NextResponse.json(
      { error: error.message || "Failed to fetch user diagnostics" },
      { status: error.status || 500 }
    );
  }
}
