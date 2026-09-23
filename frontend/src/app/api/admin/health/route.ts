import { NextResponse } from "next/server";
import * as api from "@/lib/api-client";

export async function GET() {
  try {
    await api.apiFetch<any>("/admin/jobs?page=1&limit=1");
    return NextResponse.json({
      ok: true,
      admin: true,
      databaseReachable: true,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return NextResponse.json({ ok: false, databaseReachable: false, error: String(error) }, { status: 500 });
  }
}
