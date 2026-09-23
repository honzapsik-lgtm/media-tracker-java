import { NextResponse } from "next/server";

export async function GET() {
  return NextResponse.json({
    items: [],
    total: 0,
    page: 1,
    pageSize: 20,
    totalPages: 0,
  });
}
