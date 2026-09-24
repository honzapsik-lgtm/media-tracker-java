import { NextRequest, NextResponse } from "next/server";
import { apiFetch } from "@/lib/api-client";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const offset = Number(request.nextUrl.searchParams.get("offset") || 0);
  if (!Number.isInteger(offset) || offset < 0 || offset > 9500 || offset % 500 !== 0) return NextResponse.json({ error: "Invalid offset" }, { status: 400 });
  try {
    return NextResponse.json(await apiFetch(`/media/${encodeURIComponent(id)}/chapters?offset=${offset}`));
  } catch (error) {
    const missing = error instanceof Error && error.message.includes("API Error [404]");
    return NextResponse.json({ error: "Unable to load media data" }, { status: missing ? 404 : 502 });
  }
}
