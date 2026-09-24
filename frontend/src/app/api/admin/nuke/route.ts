import { NextResponse } from "next/server";
import { revalidatePath } from "next/cache";
import * as api from "@/lib/api-client";
import { requireAdmin } from "@/lib/admin-auth";
import { adminErrorResponse } from "@/lib/admin-api";

export async function POST() {
  try {
    await requireAdmin();
    const data = await api.apiFetch<any>("/admin/nuke", { method: "POST" });
    revalidatePath("/", "layout");
    return NextResponse.json(data);
  } catch (error) {
    return adminErrorResponse(error);
  }
}
