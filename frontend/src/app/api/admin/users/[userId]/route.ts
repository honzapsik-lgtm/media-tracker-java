import { NextResponse } from "next/server";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ userId: string }> }
) {
  const { userId } = await params;
  return NextResponse.json({
    user: { id: userId, name: "User", role: "USER", email: "" },
    accounts: [],
    sessions: [],
    statsCache: [],
    aggregations: { ratings: 0, watchlist: 0, badges: 0, lists: 0 },
    jobs: [],
    logs: [],
  });
}
