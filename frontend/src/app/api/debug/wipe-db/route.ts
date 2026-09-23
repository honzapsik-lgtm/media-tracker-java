import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) {
      return NextResponse.json({ error: "You must be logged in." }, { status: 401 });
    }

    const body = await request.json().catch(() => ({}));
    const data = await api.apiFetch<any>("/debug/wipe-db", {
      method: "POST",
      body: JSON.stringify(body),
    });
    return NextResponse.json(data);
  } catch (err: any) {
    console.error("[api/debug/wipe-db POST] Error:", err);
    return NextResponse.json({ error: err.message || "Failed to wipe database" }, { status: 500 });
  }
}
