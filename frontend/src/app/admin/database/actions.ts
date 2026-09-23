"use server";

import * as api from "@/lib/api-client";
import { revalidatePath } from "next/cache";

export async function wipeDatabaseAction() {
  await api.apiFetch("/admin/nuke", { method: "POST" });
  revalidatePath("/");
  return { success: true };
}
