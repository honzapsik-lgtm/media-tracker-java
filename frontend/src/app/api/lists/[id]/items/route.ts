import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const body = await request.json();
    const { mediaIds } = body;

    if (!Array.isArray(mediaIds)) {
      return NextResponse.json({ error: "mediaIds must be an array" }, { status: 400 });
    }

    const res = await api.updateListItems(id, mediaIds);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/lists/[id]/items POST] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to update list items" }, { status: 500 });
  }
}
