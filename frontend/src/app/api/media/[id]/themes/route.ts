import { NextRequest, NextResponse } from "next/server";
import { apiFetch } from "@/lib/api-client";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  try {
    return NextResponse.json(await apiFetch(`/media/${encodeURIComponent(id)}/themes`));
  } catch (error) {
    return NextResponse.json({ openings: [], endings: [], groups: [] }, { status: 200 });
  }
}
