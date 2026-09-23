import { NextResponse } from "next/server";
import { revalidatePath } from "next/cache";
import * as api from "@/lib/api-client";

export async function POST() {
  try {
    const data = await api.apiFetch<any>("/admin/nuke", { method: "POST" });
    revalidatePath("/", "layout");
    return NextResponse.json(data);
  } catch (error: any) {
    console.error("[api/admin/nuke] Error:", error);
    return NextResponse.json({ error: error.message || "Unauthorized or failed" }, { status: 500 });
  }
}
