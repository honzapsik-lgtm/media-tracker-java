import { NextResponse } from "next/server";
import * as api from "@/lib/api-client";
import { requireAdmin } from "@/lib/admin-auth";
import { adminErrorResponse } from "@/lib/admin-api";

export async function POST() {
  try {
    await requireAdmin();
    const data = await api.apiFetch<any>("/cron/sweep-ranks", { method: "POST" });
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[cron/sweep-ranks] Error:", err);
    return adminErrorResponse(err);
  }
}
