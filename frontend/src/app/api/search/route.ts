import { NextRequest, NextResponse } from "next/server";
import * as api from "@/lib/api-client";

export const dynamic = 'force-dynamic';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const q = searchParams.get("q");

  if (!q || !q.trim()) {
    return NextResponse.json([]);
  }

  try {
    const results = await api.searchMedia(q.trim());
    return NextResponse.json(results);
  } catch (error: any) {
    console.error("[api/search GET] Error:", error);
    return NextResponse.json({ error: "Failed to fetch search results" }, { status: 500 });
  }
}
