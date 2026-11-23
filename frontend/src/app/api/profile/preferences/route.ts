import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";
import { isRatingStyle } from "@/lib/rating-style";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }
    const preferences = await api.apiFetch<{ ratingStyle: string }>("/profile/preferences");
    return NextResponse.json(preferences);
  } catch (error) {
    console.error("[api/profile/preferences GET] Error:", error);
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to fetch preferences" }, { status: 500 });
  }
}

export async function PATCH(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json().catch(() => ({}));
    if (!isRatingStyle(body.ratingStyle)) {
      return NextResponse.json({ error: "Invalid rating style" }, { status: 400 });
    }

    const updated = await api.apiFetch<{ ratingStyle: string }>("/profile/preferences", {
      method: "PATCH",
      body: JSON.stringify({ ratingStyle: body.ratingStyle }),
    });
    return NextResponse.json(updated);
  } catch (error) {
    console.error("[api/profile/preferences PATCH] Error:", error);
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to update preferences" }, { status: 500 });
  }
}
