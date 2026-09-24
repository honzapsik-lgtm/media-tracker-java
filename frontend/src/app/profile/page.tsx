import ProfileTabs from "@/components/ProfileTabs";
import ProfileHeader from "@/components/ProfileHeader";
import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";

export const dynamic = "force-dynamic";
export const revalidate = 0;

function inferType(mediaId: string): string {
  const parts = mediaId.split("-");
  if (parts[0] === "tmdb" && parts[1] === "movie") return "MOVIE";
  if (parts[0] === "tmdb" && parts[1] === "tv") {
    if (mediaId.includes("-e") || parts.length === 5) return "EPISODE";
    if (mediaId.includes("-s") || parts.length === 4) return "SEASON";
    return "SHOW";
  }
  if (parts[1] === "game" || parts[0] === "rawg" || parts[0] === "igdb") return "GAME";
  if (parts[1] === "manga" || parts[0] === "manga" || parts[0] === "mangadex" || parts[0] === "jikan" || parts[0] === "anilist") return "MANGA";
  return "OTHER";
}

export default async function ProfilePage() {
  const session = await getServerSession(authOptions);
  if (!session?.user?.id) redirect("/");

  let profile: any = null;
  let ratingsData: any = { results: [], count: 0 };

  try {
    const usernameOrId = session.user.username || session.user.id;
    [profile, ratingsData] = await Promise.all([
      api.getProfile(usernameOrId),
      api.apiFetch<{ results: any[]; count: number }>(`/profile/ratings?userId=${encodeURIComponent(session.user.id)}&limit=50`).catch(() => ({ results: [], count: 0 })),
    ]);
  } catch (e) {
    console.error("[ProfilePage] Failed to fetch profile from Spring Boot:", e);
  }

  if (!profile) {
    profile = {
      id: session.user.id,
      name: session.user.name || "User",
      username: session.user.username || "user",
      image: session.user.image,
      role: session.user.role || "user",
      createdAt: new Date().toISOString(),
      badges: [],
      stats: {},
      showcaseBadges: [],
    };
  }

  const ratings = ratingsData.results || [];
  const formattedData = ratings.map((r: any) => ({
    mediaId: r.mediaId || r.media_id,
    score: r.score,
    reviewText: r.reviewText || r.review_text || null,
    title: r.mediaTitle || r.media_title || "Unknown Title",
    image: r.mediaImage || r.media_image || null,
    type: inferType(r.mediaId || r.media_id || ""),
    rankPosition: r.rankPosition || r.rank_position || null,
    criteriaScores: r.criteriaScores || r.criteria_scores || null,
    releaseDate: r.mediaReleaseDate || r.media_release_date || null,
    inUserList: false,
  }));

  const badges = (profile.badges || []).map((b: string) => ({ badge_id: b, unlocked_at: new Date().toISOString() }));
  const totalCount = ratingsData.count || formattedData.length;
  const statsCache = Array.isArray(profile.stats)
    ? profile.stats
    : Object.entries(profile.stats || {}).map(([mediaType, statsJson]) => ({
        media_type: mediaType,
        stats_json: statsJson,
      }));

  return (
    <main className="min-h-screen bg-gray-950 text-white pt-24 pb-12 px-8">
      <div className="max-w-7xl mx-auto">
        <ProfileHeader user={profile} ratings={formattedData} userBadges={badges} />

        <ProfileTabs
          initialData={formattedData}
          initialCount={totalCount}
          userBadges={badges}
          statsCache={statsCache}
        />
      </div>
    </main>
  );
}
