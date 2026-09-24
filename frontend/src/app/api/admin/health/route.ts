import { NextResponse } from "next/server";
import * as api from "@/lib/api-client";
import { requireAdmin } from "@/lib/admin-auth";
import { adminErrorResponse } from "@/lib/admin-api";

export async function GET() {
  try {
    await requireAdmin();
    await api.apiFetch<any>("/admin/jobs?page=1&limit=1");
    return NextResponse.json({
      ok: true,
      admin: true,
      databaseReachable: true,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return adminErrorResponse(error);
  }
}
