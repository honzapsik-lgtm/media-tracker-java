import { NextResponse } from "next/server";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ mediaId: string }> }
) {
  const { mediaId } = await params;
  return NextResponse.json({
    tracking: {
      id: mediaId,
      type: "MOVIE",
    },
    caches: [],
    stats: null,
    aggregations: {
      totalRatings: 0,
      writtenReviews: 0,
      deepReviews: 0,
      watchlistInclusions: 0,
    },
    logs: [],
  });
}
