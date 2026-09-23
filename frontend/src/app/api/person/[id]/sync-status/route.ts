import { NextResponse } from "next/server";

export async function GET() {
  return NextResponse.json({
    status: "idle",
    isSyncing: false,
    credits: [],
  });
}
