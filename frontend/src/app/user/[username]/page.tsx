import { notFound, redirect } from "next/navigation";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import * as api from "@/lib/api-client";
import OtherUserProfileView from "@/components/OtherUserProfileView";
import { FriendshipRelation } from "@/components/FriendActionButton";

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

export default async function UserProfilePage({
  params,
}: {
  params: Promise<{ username: string }>;
}) {
  const resolvedParams = await params;
  const rawHandle = decodeURIComponent(resolvedParams.username).trim().replace(/^@/, "");

  let profile: any = null;
  try {
    profile = await api.getProfile(rawHandle);
  } catch {
    notFound();
  }

  if (!profile) {
    notFound();
  }

  const session = await getServerSession(authOptions);
  const currentUserId = session?.user?.id;

  if (currentUserId === profile.id) {
    redirect("/profile");
  }

  let ratingsData: any = { results: [], count: 0 };
  try {
    ratingsData = await api.apiFetch<{ results: any[]; count: number }>(
      `/profile/ratings?userId=${encodeURIComponent(profile.id)}&limit=50`
    );
  } catch {
    // Ignore
  }

  const formattedRatings = (ratingsData.results || []).map((r: any) => ({
    mediaId: r.mediaId || r.media_id,
    score: r.score,
    reviewText: r.reviewText || r.review_text || null,
    title: r.mediaTitle || r.media_title || "Unknown Title",
    image: r.mediaImage || r.media_image || null,
    type: inferType(r.mediaId || r.media_id || ""),
    rankPosition: r.rankPosition || r.rank_position || null,
    criteriaScores: r.criteriaScores || r.criteria_scores || null,
    releaseDate: r.mediaReleaseDate || r.media_release_date || null,
  }));

  const badges = (profile.badges || []).map((b: string) => ({ badge_id: b, unlocked_at: new Date().toISOString() }));

  return (
    <main className="min-h-screen bg-gray-950 text-white pt-24 pb-12 px-4 sm:px-8">
      <OtherUserProfileView
        user={{
          id: profile.id,
          name: profile.name,
          username: profile.username,
          image: profile.image,
          country: profile.country,
          stateRegion: profile.stateRegion,
          created_at: profile.createdAt,
          showcaseBadges: profile.showcaseBadges,
        }}
        ratings={formattedRatings}
        activities={[]}
        badges={badges}
        statsCache={profile.stats}
        canViewProfile={true}
        canViewRatings={true}
        canViewActivity={true}
        isFriend={false}
        initialRelation="NONE"
      />
    </main>
  );
}
