import { NextRequest, NextResponse } from "next/server";
import { apiFetch } from "@/lib/api-client";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  
  try {
    return NextResponse.json(await apiFetch(`/media/${encodeURIComponent(id)}`));
  } catch (error) {
    const missing = error instanceof Error && error.message.includes("API Error [404]");
    return NextResponse.json({ error: "Unable to load media data" }, { status: missing ? 404 : 502 });
  }
}
