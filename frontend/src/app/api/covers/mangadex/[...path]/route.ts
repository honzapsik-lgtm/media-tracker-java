import { NextResponse } from "next/server";

// Server-side proxy for MangaDex cover images. Browsers get a bot challenge from uploads.mangadex.org,
// while plain server requests receive the image, so covers are fetched here and passed through.
const SEGMENT = /^[A-Za-z0-9._-]+$/;
const ONE_WEEK = 7 * 24 * 3600;

export async function GET(_req: Request, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;

  // Expected shape: <mangaId>/<fileName>, e.g. 0123.../abcd....jpg or ....jpg.512.jpg
  if (!Array.isArray(path) || path.length !== 2 || !path.every((s) => SEGMENT.test(s) && s !== "." && s !== "..")) {
    return new NextResponse("Bad cover path", { status: 400 });
  }

  try {
    const upstream = await fetch(`https://uploads.mangadex.org/covers/${path[0]}/${path[1]}`, {
      headers: { "User-Agent": "MediaTracker/1.0 (cover proxy)" },
      redirect: "manual", // a redirect means a challenge page, not an image
      next: { revalidate: ONE_WEEK },
    });

    const contentType = upstream.headers.get("content-type") || "";
    if (!upstream.ok || !contentType.startsWith("image/")) {
      return new NextResponse("Cover unavailable", { status: 502 });
    }

    return new NextResponse(upstream.body, {
      status: 200,
      headers: {
        "Content-Type": contentType,
        "Cache-Control": `public, max-age=${ONE_WEEK}, immutable`,
      },
    });
  } catch {
    return new NextResponse("Cover unavailable", { status: 502 });
  }
}
