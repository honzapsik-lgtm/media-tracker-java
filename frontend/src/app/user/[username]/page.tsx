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

  // Visibility is decided by the backend from the user's privacy settings and the viewer's friendship.
  const canViewProfile = profile.canViewProfile !== false;
  const canViewRatings = canViewProfile && profile.canViewRatings !== false;
  const canViewActivity = canViewProfile && profile.canViewActivity !== false;
  const relation: FriendshipRelation = (profile.relation as FriendshipRelation) || "NONE";

  const [ratingsData, activityData] = await Promise.all([
    canViewRatings
      ? api.apiFetch<{ results: any[]; count: number }>(`/profile/ratings?userId=${encodeURIComponent(profile.id)}&limit=50`)
          .catch(() => ({ results: [], count: 0 }))
      : Promise.resolve({ results: [], count: 0 }),
    canViewActivity
      ? api.apiFetch<{ canView: boolean; activities: any[] }>(`/profile/${encodeURIComponent(profile.username || rawHandle)}/activity?limit=60`)
          .catch(() => ({ canView: true, activities: [] }))
      : Promise.resolve({ canView: false, activities: [] }),
  ]);

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
        activities={activityData.activities || []}
        insights={canViewRatings ? profile.insights ?? null : null}
        badges={badges}
        statsCache={Array.isArray(profile.stats) ? profile.stats : Object.entries(profile.stats || {}).map(([mediaType, statsJson]) => ({ media_type: mediaType, stats_json: statsJson }))}
        canViewProfile={canViewProfile}
        canViewRatings={canViewRatings}
        canViewActivity={canViewActivity && activityData.canView !== false}
        isFriend={relation === "FRIENDS"}
        initialRelation={relation}
      />
    </main>
  );
}
