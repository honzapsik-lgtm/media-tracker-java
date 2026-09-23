import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const mediaId = searchParams.get("mediaId");
    const session = await getServerSession(authOptions);

    if (mediaId) {
      if (!session?.user?.id) {
        return NextResponse.json({ status: null });
      }
      const data = await api.apiFetch<any>(`/watchlist?mediaId=${encodeURIComponent(mediaId)}`);
      return NextResponse.json(data?.status === "none" ? { status: null } : data);
    }

    const page = parseInt(searchParams.get("page") || "1", 10);
    const limit = parseInt(searchParams.get("limit") || "50", 10);
    const media_type = searchParams.get("media_type") || undefined;
    const status = searchParams.get("status") || undefined;
    const userId = searchParams.get("userId") || session?.user?.id;

    if (!userId) {
      return NextResponse.json({ results: [], count: 0 });
    }

    const data = await api.getWatchlist({ userId, media_type, status, page, limit });
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/watchlist GET] Error:", err);
    return NextResponse.json({ results: [], count: 0 });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in to track media." }, { status: 401 });
    }

    const body = await request.json();
    if (!body.mediaId) {
      return NextResponse.json({ error: "mediaId is required" }, { status: 400 });
    }

    const data = await api.upsertWatchlist(body);
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/watchlist POST] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to update watchlist" }, { status: 500 });
  }
}

export async function PATCH(request: Request) {
  return POST(request);
}

export async function DELETE(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in." }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const mediaId = searchParams.get("mediaId");
    if (!mediaId) {
      return NextResponse.json({ error: "mediaId is required" }, { status: 400 });
    }

    const res = await api.deleteWatchlistItem(mediaId);
    return NextResponse.json({ ok: res.success });
  } catch (err: any) {
    console.error("[api/watchlist DELETE] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to delete item" }, { status: 500 });
  }
}
