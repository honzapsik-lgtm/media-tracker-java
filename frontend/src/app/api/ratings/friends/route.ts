import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ friendsRatings: [] });
    }

    const { searchParams } = new URL(req.url);
    const mediaId = searchParams.get("mediaId");
    if (!mediaId) {
      return NextResponse.json({ error: "mediaId is required" }, { status: 400 });
    }

    const ratings = await api.getFriendRatings(mediaId);
    return NextResponse.json({ friendsRatings: ratings });
  } catch (error: any) {
    console.error("[api/ratings/friends] Error:", error);
    return NextResponse.json({ friendsRatings: [] });
  }
}
