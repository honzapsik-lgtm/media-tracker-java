import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) return NextResponse.json({ reviewText: null });

    const { searchParams } = new URL(request.url);
    const mediaId = searchParams.get("mediaId");
    if (!mediaId) {
      return NextResponse.json({ error: "mediaId is required" }, { status: 400 });
    }

    const data = await api.getRatings(mediaId);
    return NextResponse.json({ reviewText: data?.personal?.reviewText ?? null });
  } catch (error: any) {
    console.error("[api/reviews GET] Error:", error);
    return NextResponse.json({ reviewText: null });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in to review." }, { status: 401 });
    }

    const body = await request.json();
    if (!body.mediaId) {
      return NextResponse.json({ error: "mediaId is required" }, { status: 400 });
    }

    await api.saveRating({
      mediaId: body.mediaId,
      score: body.score ?? 50,
      reviewText: body.reviewText?.trim() || null,
      mediaTitle: body.mediaTitle,
      mediaImage: body.mediaImage,
    });

    return NextResponse.json({ ok: true });
  } catch (error: any) {
    console.error("[api/reviews POST] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to save review" }, { status: 500 });
  }
}
