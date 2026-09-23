import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const settings = await api.apiFetch<any>("/profile/privacy");
    return NextResponse.json({ settings });
  } catch (error: any) {
    console.error("[api/profile/privacy GET] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to fetch privacy settings" }, { status: 500 });
  }
}

export async function PATCH(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const updated = await api.apiFetch<any>("/profile/privacy", {
      method: "PATCH",
      body: JSON.stringify({
        profileVisibility: body.profile_visibility,
        ratingsVisibility: body.ratings_visibility,
        watchlistVisibility: body.watchlist_visibility,
        activityVisibility: body.activity_visibility,
      }),
    });

    return NextResponse.json({ success: true, settings: updated });
  } catch (error: any) {
    console.error("[api/profile/privacy PATCH] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to update privacy settings" }, { status: 500 });
  }
}
