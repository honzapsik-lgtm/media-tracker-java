import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(req: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ activities: [] });
    }

    const { searchParams } = new URL(req.url);
    const scope = searchParams.get("scope") || "friends";
    const page = parseInt(searchParams.get("page") || "1", 10);
    const limit = parseInt(searchParams.get("limit") || "50", 10);

    const data = await api.getActivityFeed(scope, page, limit);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/activity GET] Error:", error);
    return NextResponse.json({ activities: [] });
  }
}
