import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import * as api from "@/lib/api-client";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ userId: string }> }
) {
  try {
    await requireAdmin();
    const { userId } = await params;
    const body = await request.json();
    const data = await api.apiFetch<any>(`/admin/users/${encodeURIComponent(userId)}/actions`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/admin/users/[userId]/actions POST] Error:", error);
    return NextResponse.json(
      { error: error.message || "Failed to execute user action" },
      { status: error.status || 500 }
    );
  }
}
