import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { adminErrorResponse } from "@/lib/admin-api";
import { getDiagnosticLogs } from "@/lib/admin-diagnostics";
import { getOrCreateRequestId } from "@/lib/request-id";

export async function GET(request: Request) {
  const requestId = getOrCreateRequestId(request.headers);
  try {
    await requireAdmin();
    const filters = Object.fromEntries(new URL(request.url).searchParams);
    const data = await getDiagnosticLogs({ ...filters, pageSize: filters.pageSize ?? 20 });
    return NextResponse.json({
      items: data.items,
      total: data.pagination.total,
      page: data.pagination.page,
      pageSize: data.pagination.pageSize,
      totalPages: data.pagination.pageCount,
    });
  } catch (error) {
    return adminErrorResponse(error, requestId);
  }
}
