import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ results: [], count: 0 }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1", 10);
    const limit = parseInt(searchParams.get("limit") || "50", 10);
    const userId = searchParams.get("userId") || session.user.id;

    const data = await api.apiFetch<any>(`/reviews?userId=${encodeURIComponent(userId)}&page=${page}&limit=${limit}`);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/profile/reviews GET] Error:", error);
    return NextResponse.json({ results: [], count: 0 });
  }
}
