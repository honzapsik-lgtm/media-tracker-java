import { NextResponse } from "next/server";
import * as api from "@/lib/api-client";

export async function POST() {
  try {
    const data = await api.apiFetch<any>("/cron/sweep-ranks", { method: "POST" });
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[cron/sweep-ranks] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to sweep ranks" }, { status: 500 });
  }
}
