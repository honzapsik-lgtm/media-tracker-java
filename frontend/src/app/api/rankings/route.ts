import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const type = searchParams.get("type") || "SHOW";
    const sort = searchParams.get("sort") || "list_rank";
    const page = parseInt(searchParams.get("page") || "1", 10);
    const limit = parseInt(searchParams.get("limit") || "20", 10);

    const data = await api.getRankings(type, sort, page, limit);
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/rankings GET] Error:", err);
    return NextResponse.json({ results: [], count: 0 });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in to save ranks." }, { status: 401 });
    }

    const body = await request.json();
    const res = await api.apiFetch<{ ok: boolean }>("/rankings", {
      method: "POST",
      body: JSON.stringify(body),
    });
    return NextResponse.json(res);
  } catch (err: any) {
    console.error("[api/rankings POST] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to save ranks" }, { status: 500 });
  }
}
