import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const username = searchParams.get("username")?.trim();
    if (!username) {
      return NextResponse.json({ error: "Username parameter is required" }, { status: 400 });
    }

    const res = await api.checkUsernameAvailability(username);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/profile/username GET] Error:", error);
    return NextResponse.json({ error: error.message || "Internal Server Error" }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const cleanUsername = body.username?.trim();
    if (!cleanUsername) {
      return NextResponse.json({ error: "Nickname is required" }, { status: 400 });
    }

    const res = await api.updateUsername(cleanUsername);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/profile/username POST] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to update nickname" }, { status: 400 });
  }
}
