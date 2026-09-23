import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const data = await api.getListById(id);
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/lists/[id] GET] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to fetch list" }, { status: 500 });
  }
}

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const res = await api.deleteList(id);
    return NextResponse.json(res);
  } catch (error: any) {
    console.error("[api/lists/[id] DELETE] Error:", error);
    return NextResponse.json({ error: error.message || "Failed to delete list" }, { status: 500 });
  }
}
