import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const mediaType = searchParams.get("media_type") || undefined;

    const data = await api.getUserLists(mediaType);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/lists GET] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to fetch lists" }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { title, media_type } = body;
    if (!title || !media_type) {
      return NextResponse.json({ error: "Title and media_type are required" }, { status: 400 });
    }

    const data = await api.createList(title, media_type);
    return NextResponse.json(data, { status: 201 });
  } catch (error: any) {
    console.error("[api/lists POST] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to create list" }, { status: 500 });
  }
}
