import { getUnifiedPersonProfile } from "@/lib/person";
import { notFound, redirect } from "next/navigation";
import Link from "next/link";
import ExpandableText from "@/components/ExpandableText";
import MediaCardVertical from "@/components/MediaCardVertical";
import Carousel from "@/components/Carousel";
import { UnifiedCredit } from "@/types/person";
import PersonCredits from "@/components/PersonCredits";
import BackToSearchButton from "@/components/BackToSearchButton";
import { getMediaRankings } from "@/lib/media-api";

export default async function PersonProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = await params;
  const personSlug = resolvedParams.id;
  
  const profile = await getUnifiedPersonProfile(personSlug);
  if (!profile) return notFound();

  // Redirect to canonical slug if it differs
  let canonicalSlug = personSlug;
  if (profile.tmdbId) {
    canonicalSlug = `tmdb-${profile.tmdbId}`;
  } else if (profile.mangadexId) {
    canonicalSlug = `mangadex-${profile.mangadexId}`;
  } else if (profile.anilistId) {
    canonicalSlug = `anilist-${profile.anilistId}`;
  } else if (profile.igdbId) {
    canonicalSlug = `igdb-${profile.igdbId}`;
  } else if (profile.rawgId) {
    canonicalSlug = `rawg-${profile.rawgId}`;
  }

  if (personSlug !== canonicalSlug) {
    redirect(`/person/${canonicalSlug}`);
  }

  // "Known For" horizontal carousel: top 6 performing items
  const isActor = profile.knownForDepartment?.toLowerCase().includes("act") ?? true;
  const primaryCredits = isActor ? profile.credits.cast : profile.credits.crew;
  const secondaryCredits = isActor ? profile.credits.crew : profile.credits.cast;

  // Filter out pure "Self" / talk show / interview appearances from Known For if real character/creative roles exist
  const filteredPrimary = primaryCredits.filter(c => {
    const roleLower = (c.role || "").toLowerCase().trim();
    return !roleLower.startsWith("self") && !roleLower.startsWith("himself") && !roleLower.startsWith("herself");
  });

  const combinedCredits = [
    ...(filteredPrimary.length > 0 ? filteredPrimary : primaryCredits),
    ...secondaryCredits
  ];

  const uniqueCredits = Array.from(new Map(combinedCredits.map(c => [c.mediaId || c.title, c])).values());

  const mediaIds = uniqueCredits.map(c => c.mediaId).filter(Boolean) as string[];
  // Community scores, list ranks and the signed-in user's own ratings for every credit
  const batch = mediaIds.length
    ? await getMediaRankings(mediaIds).catch(() => ({ stats: {}, ranks: {}, mine: {} }))
    : { stats: {}, ranks: {}, mine: {} };
  const statsMap: Record<string, number> = batch.stats || {};
  const rankMap: Record<string, number> = batch.ranks || {};
  const myRatings: Record<string, number> = batch.mine || {};

  const creditsWithStats = uniqueCredits.map(credit => ({
    ...credit,
    communityScore: credit.mediaId ? statsMap[credit.mediaId] : null,
    listRank: credit.mediaId ? rankMap[credit.mediaId] : null,
    myScore: credit.mediaId ? myRatings[credit.mediaId] ?? null : null,
  }));

  // If local database has community scores, prioritize them;
  // otherwise maintain the provider's authoritative vote_count/popularity order!
  creditsWithStats.sort((a, b) => {
    if (a.communityScore && b.communityScore && a.communityScore !== b.communityScore) {
      return b.communityScore - a.communityScore;
    }
    if (a.communityScore && !b.communityScore) return -1;
    if (!a.communityScore && b.communityScore) return 1;
    return 0;
  });

  const topKnownFor = creditsWithStats.slice(0, 6);

  // Credits are grouped and sorted in the client component

  const formatBirthOrDeathDate = (dateStr?: string | null) => {
    if (!dateStr) return null;
    const parts = dateStr.trim().split("-");
    if (parts.length !== 3) return dateStr;
    const year = parts[0];
    const monthIndex = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);

    const months = [
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    ];
    if (monthIndex < 0 || monthIndex > 11 || isNaN(day)) return dateStr;

    const s = ["th", "st", "nd", "rd"];
    const v = day % 100;
    const ordinal = day + (s[(v - 20) % 10] || s[v] || s[0]);

    return `${year} ${months[monthIndex]} ${ordinal}`;
  };

  const calculateAge = (birth?: string | null, death?: string | null) => {
    if (!birth) return null;
    const b = new Date(birth);
    const end = death ? new Date(death) : new Date();
    let age = end.getFullYear() - b.getFullYear();
    const m = end.getMonth() - b.getMonth();
    if (m < 0 || (m === 0 && end.getDate() < b.getDate())) {
      age--;
    }
    return age;
  };

  const age = calculateAge(profile.birthDate, profile.deathDate);

  return (
    <main className="min-h-screen bg-gray-950 text-white relative pb-24 selection:bg-blue-500/30">
      <div className="max-w-7xl mx-auto px-6 sm:px-8 pt-24 relative z-10 space-y-20">
        
        <BackToSearchButton />

        {/* Hero Section */}
        <section className="flex flex-col md:flex-row gap-10 lg:gap-14 items-start">
          <div className="w-full md:w-1/3 lg:w-1/4 shrink-0">
            {profile.profileImage ? (
              <img src={profile.profileImage} alt={profile.name} className="w-full rounded-3xl shadow-2xl shadow-black/50 border border-gray-800 object-cover aspect-[2/3]" />
            ) : (
              <div className="w-full aspect-[2/3] bg-gray-900 rounded-3xl border border-gray-800 flex items-center justify-center text-gray-500 text-lg font-medium shadow-inner">No Image Available</div>
            )}
          </div>

          <div className="flex-1 flex flex-col justify-start space-y-5 min-w-0">
            <div>
              <h1 className="text-4xl sm:text-6xl font-black text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400 tracking-tight leading-tight">
                {profile.name}
              </h1>
              {profile.nativeName && (
                <h2 className="text-xl font-bold text-gray-500 mt-1 tracking-wide">{profile.nativeName}</h2>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-x-8 gap-y-3 text-sm font-bold text-gray-400 bg-gray-900/50 p-4 sm:p-5 rounded-2xl border border-gray-800/50 inline-flex w-fit">
              {profile.knownForDepartment && (
                <div>
                  <span className="text-[10px] text-gray-500 uppercase tracking-widest block mb-0.5">Department</span>
                  <span className="text-gray-200">{profile.knownForDepartment}</span>
                </div>
              )}
              {profile.birthDate && (
                <div>
                  <span className="text-[10px] text-gray-500 uppercase tracking-widest block mb-0.5">Born</span>
                  <span className="text-gray-200">{formatBirthOrDeathDate(profile.birthDate)} {age && !profile.deathDate ? `(Age ${age})` : ''}</span>
                </div>
              )}
              {profile.deathDate && (
                <>
                  <div className="w-px h-8 bg-gray-800 hidden sm:block" />
                  <div>
                    <span className="text-[10px] text-gray-500 uppercase tracking-widest block mb-0.5">Died</span>
                    <span className="text-gray-200">{formatBirthOrDeathDate(profile.deathDate)} {age ? `(Age ${age})` : ''}</span>
                  </div>
                </>
              )}
            </div>

            {profile.bio && (
              <div className="max-w-3xl">
                <h3 className="text-base font-bold mb-2 text-gray-300">Biography</h3>
                <div className="prose prose-invert prose-gray max-w-none text-gray-400 leading-relaxed">
                  <ExpandableText text={profile.bio} lineClamp={3} />
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Known For Section */}
        {topKnownFor.length > 0 && (
          <section>
            <div className="flex items-center gap-4 mb-8">
              <h2 className="text-3xl font-black text-white">Known For</h2>
              <div className="h-px flex-1 bg-gradient-to-r from-gray-800 to-transparent"></div>
            </div>
            
            <Carousel>
              {topKnownFor.map((credit, idx) => (
                <div key={`${credit.mediaId}-${idx}`} className="w-40 sm:w-48 lg:w-56 shrink-0 snap-start">
                  <MediaCardVertical item={{
                    id: credit.mediaId || `temp-${idx}`,
                    title: credit.title,
                    type: credit.mediaType.toLowerCase() as any,
                    image: credit.poster,
                    releaseDate: credit.releaseYear?.toString() || 'N/A',
                    communityScore: credit.communityScore || null,
                    listRank: credit.listRank || null,
                    myScore: credit.myScore,
                  }} />
                </div>
              ))}
            </Carousel>
          </section>
        )}

        {/* Unified Credits Timeline */}
        <PersonCredits initialCredits={profile.credits} personSlug={canonicalSlug} myRatings={myRatings} />

      </div>
    </main>
  );
}
