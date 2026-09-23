import { revalidatePath } from "next/cache";
import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const mediaId = searchParams.get("mediaId") || undefined;
    const prefix = searchParams.get("prefix") || undefined;

    const data = await api.getRatings(mediaId, prefix);
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/ratings GET] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to get ratings" }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in to rate media." }, { status: 401 });
    }

    const body = await request.json();
    if (!body.mediaId || typeof body.score !== "number") {
      return NextResponse.json({ error: "mediaId and score are required" }, { status: 400 });
    }

    const data = await api.saveRating(body);
    if (body.mediaId) {
      revalidatePath(`/media/${body.mediaId}`);
    }
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/ratings POST] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to save rating" }, { status: 500 });
  }
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

    const res = await api.deleteRating(mediaId);
    revalidatePath(`/media/${mediaId}`);
    return NextResponse.json({ ok: res.success });
  } catch (err: any) {
    console.error("[api/ratings DELETE] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to delete rating" }, { status: 500 });
  }
}
