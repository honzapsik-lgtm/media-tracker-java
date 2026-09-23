import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function PATCH(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const { friendId, hide_activity, hide_ratings } = body;

    if (!friendId) {
      return NextResponse.json({ error: "friendId is required" }, { status: 400 });
    }

    const updated = await api.updateFriendPreferences(friendId, hide_activity, hide_ratings);
    return NextResponse.json({ success: true, preference: updated });
  } catch (error: any) {
    console.error("[api/friends/preferences] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to update preferences" }, { status: 500 });
  }
}
