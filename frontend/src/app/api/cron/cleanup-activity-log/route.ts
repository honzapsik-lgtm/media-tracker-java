import { NextResponse } from "next/server";
import * as api from "@/lib/api-client";

export async function POST() {
  try {
    const data = await api.apiFetch<any>("/cron/cleanup-activity-log", { method: "POST" });
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[cron/cleanup-activity-log] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to cleanup activity log" }, { status: 500 });
  }
}
