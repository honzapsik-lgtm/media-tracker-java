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

    const data = await api.getFriends();
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/friends GET] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to fetch friends" }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const target = body.target || body.targetUsername || body.targetUserId;
    if (!target) {
      return NextResponse.json({ error: "Target is required" }, { status: 400 });
    }

    const res = await api.sendFriendRequest(target);
    return NextResponse.json({ success: true, friendship: res });
  } catch (error: any) {
    console.error("[api/friends POST] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to send friend request" }, { status: 400 });
  }
}

export async function PATCH(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const { friendshipId, action } = body;
    if (!friendshipId || !action) {
      return NextResponse.json({ error: "Friendship ID and action are required" }, { status: 400 });
    }

    const actionLower = action.toLowerCase() as "accept" | "decline" | "block";
    const res = await api.respondToFriendRequest(friendshipId, actionLower);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/friends PATCH] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to update friendship" }, { status: 400 });
  }
}

export async function DELETE(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(req.url);
    const friendshipId = searchParams.get("friendshipId") || searchParams.get("id");
    if (!friendshipId) {
      return NextResponse.json({ error: "Friendship ID is required" }, { status: 400 });
    }

    const res = await api.removeFriend(friendshipId);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/friends DELETE] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to remove friend" }, { status: 500 });
  }
}
