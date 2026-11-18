"use client";

import { useState } from "react";
import Link from "next/link";
import { UnifiedCredit } from "@/types/person";

interface PersonCreditsProps {
  initialCredits: { cast: UnifiedCredit[]; crew: UnifiedCredit[] };
  personSlug: string;
}

type Category = "movies_shows" | "shorts" | "documentary" | "talk" | "all";
type RoleTab = "all" | "cast" | "crew";

interface ConsolidatedCredit {
  mediaId: string | null;
  mediaType: string;
  title: string;
  poster: string | null;
  releaseYear: number | null;
  roles: string[];
  isVoiceRole: boolean;
  characterImage?: string | null;
  category: string;
}

export default function PersonCredits({ initialCredits: credits }: PersonCreditsProps) {
  const getCategory = (c: UnifiedCredit): string => {
    if (c.category) return c.category;
    const role = (c.role || "").toLowerCase().trim();
    const title = (c.title || "").toLowerCase().trim();
    const g = c.genreIds || [];

    if (
      title.includes("(short)") ||
      title.includes("short film") ||
      title.endsWith(" short")
    ) {
      return "short";
    }

    if (
      g.includes(99) ||
      title.includes("behind the scenes") ||
      title.includes("the making of") ||
      title.includes("a football life") ||
      title.includes("retrospective") ||
      title.includes("documentary")
    ) {
      return "documentary";
    }

    if (
      g.includes(10767) ||
      g.includes(10763) ||
      role.startsWith("self") ||
      role.startsWith("himself") ||
      role.startsWith("herself") ||
      role === "guest" ||
      title.includes("town hall") ||
      title.includes("interview") ||
      title.includes("the tonight show") ||
      title.includes("jimmy kimmel") ||
      title.includes("the late show") ||
      title.includes("the ellen") ||
      title.includes("live with ")
    ) {
      return "talk";
    }

    return "narrative";
  };

  // Consolidate multiple credits for the same media item into a single entry with combined roles
  const consolidateCredits = (items: UnifiedCredit[]): ConsolidatedCredit[] => {
    const map = new Map<string, ConsolidatedCredit>();

    for (const item of items) {
      const key = item.mediaId || `${item.title}-${item.mediaType}-${item.releaseYear}`;
      const isRoleResolving = item.role?.startsWith("[RESOLVING_ROLE]");
      const cleanRole = isRoleResolving
        ? item.role.includes(":")
          ? item.role.split(":")[1]
          : "Developer"
        : item.role?.trim() || "";

      if (!map.has(key)) {
        map.set(key, {
          mediaId: item.mediaId,
          mediaType: item.mediaType,
          title: item.title,
          poster: item.poster,
          releaseYear: item.releaseYear,
          roles: cleanRole ? [cleanRole] : [],
          isVoiceRole: item.isVoiceRole,
          characterImage: item.characterImage,
          category: getCategory(item),
        });
      } else {
        const existing = map.get(key)!;
        if (cleanRole && !existing.roles.includes(cleanRole)) {
          existing.roles.push(cleanRole);
        }
        if (item.isVoiceRole) {
          existing.isVoiceRole = true;
        }
        if (!existing.characterImage && item.characterImage) {
          existing.characterImage = item.characterImage;
        }
        if (!existing.poster && item.poster) {
          existing.poster = item.poster;
        }
      }
    }

    return Array.from(map.values());
  };

  const consolidatedCast = consolidateCredits(credits.cast);
  const consolidatedCrew = consolidateCredits(credits.crew);

  const allConsolidated = [...consolidatedCast, ...consolidatedCrew];
  const narrativeCount = allConsolidated.filter((c) => c.category === "narrative").length;
  const shortCount = allConsolidated.filter((c) => c.category === "short").length;
  const documentaryCount = allConsolidated.filter((c) => c.category === "documentary").length;
  const talkCount = allConsolidated.filter((c) => c.category === "talk").length;
  const totalCount = allConsolidated.length;

  const defaultCategory: Category = narrativeCount > 0 ? "movies_shows" : "all";
  const [activeCategory, setActiveCategory] = useState<Category>(defaultCategory);
  const [activeRole, setActiveRole] = useState<RoleTab>("all");

  const filterByCategory = (items: ConsolidatedCredit[]) => {
    if (activeCategory === "all") return items;
    if (activeCategory === "movies_shows") {
      return items.filter((c) => c.category === "narrative");
    }
    if (activeCategory === "shorts") {
      return items.filter((c) => c.category === "short");
    }
    if (activeCategory === "documentary") {
      return items.filter((c) => c.category === "documentary");
    }
    if (activeCategory === "talk") {
      return items.filter((c) => c.category === "talk");
    }
    return items;
  };

  const filteredCast = filterByCategory(consolidatedCast);
  const filteredCrew = filterByCategory(consolidatedCrew);

  // Grouping credits by decade/year
  const groupCredits = (items: ConsolidatedCredit[]) => {
    const sorted = [...items].sort((a, b) => (b.releaseYear || 0) - (a.releaseYear || 0));
    return sorted.reduce((acc, c) => {
      const year = c.releaseYear ? String(c.releaseYear) : "Upcoming";
      if (!acc[year]) acc[year] = [];
      acc[year].push(c);
      return acc;
    }, {} as Record<string, ConsolidatedCredit[]>);
  };

  const sortYearsDesc = (a: string, b: string) => {
    if (a === "Upcoming") return -1;
    if (b === "Upcoming") return 1;
    return Number(b) - Number(a);
  };

  const castGroups = groupCredits(filteredCast);
  const crewGroups = groupCredits(filteredCrew);

  const hasCredits = credits.cast.length > 0 || credits.crew.length > 0;
  const hasBothRoles = credits.cast.length > 0 && credits.crew.length > 0;

  if (!hasCredits) {
    return (
      <section>
        <div className="flex items-center gap-4 mb-10">
          <h2 className="text-3xl font-black text-white">Credits</h2>
          <div className="h-px flex-1 bg-gradient-to-r from-gray-800 to-transparent"></div>
        </div>
        <div className="text-gray-500 text-lg font-medium p-8 bg-gray-900/20 rounded-2xl border border-gray-800/50 text-center">
          No credits found.
        </div>
      </section>
    );
  }

  const categoryTabs = [
    { id: "movies_shows" as Category, label: "Movies & Shows", count: narrativeCount },
    { id: "shorts" as Category, label: "Short Films", count: shortCount },
    { id: "documentary" as Category, label: "Documentaries", count: documentaryCount },
    { id: "talk" as Category, label: "Talk & Appearances", count: talkCount },
    { id: "all" as Category, label: "All Credits", count: totalCount },
  ].filter((t) => t.id === "all" || t.id === "movies_shows" || t.count > 0);

  const renderCreditCard = (c: ConsolidatedCredit, i: number, fallbackRole: string) => {
    const displayRoles = c.roles.length > 0 ? c.roles : [fallbackRole];

    const cardContent = (
      <>
        {c.isVoiceRole && c.characterImage ? (
          <img
            src={c.characterImage}
            alt={displayRoles.join(", ")}
            className="w-10 h-10 rounded-full object-cover border border-gray-700 shrink-0"
          />
        ) : (
          <div className="w-10 h-10 rounded-full bg-gray-800 border border-gray-700 shrink-0 flex items-center justify-center text-gray-500 text-xs font-bold">
            {c.mediaType.slice(0, 2)}
          </div>
        )}

        <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex flex-col sm:flex-row sm:items-center gap-1.5 sm:gap-3 min-w-0">
            <h4 className="font-bold text-base text-gray-200 truncate group-hover:text-white shrink-0 max-w-full sm:max-w-md">
              {c.title}
            </h4>
            <div className="flex flex-wrap items-center gap-1.5 min-w-0">
              <span className="text-gray-600 text-xs hidden sm:inline">•</span>
              {c.isVoiceRole && (
                <span className="text-[9px] font-black uppercase bg-purple-500/10 text-purple-400 border border-purple-500/20 px-1.5 py-0.5 rounded shrink-0">
                  Voice
                </span>
              )}
              {displayRoles.map((role, rIdx) => (
                <span
                  key={rIdx}
                  className="text-xs text-gray-300 bg-gray-800/90 group-hover:bg-gray-700/80 px-2 py-0.5 rounded-md border border-gray-700/60 font-medium whitespace-nowrap transition-colors"
                >
                  {role}
                </span>
              ))}
            </div>
          </div>

          <span className="text-[10px] font-black uppercase tracking-wider px-2 py-1 rounded bg-gray-950 text-gray-500 border border-gray-800 shrink-0 self-start sm:self-auto">
            {c.mediaType}
          </span>
        </div>
      </>
    );

    const className =
      "group bg-gray-900/40 hover:bg-gray-800/60 py-2.5 px-4 rounded-xl border border-gray-800/50 hover:border-blue-500/30 transition-colors flex items-center gap-4 w-full cursor-pointer";

    if (c.mediaId) {
      return (
        <Link key={i} href={`/media/${c.mediaId}`} className={className}>
          {cardContent}
        </Link>
      );
    }

    return (
      <div key={i} className={className}>
        {cardContent}
      </div>
    );
  };

  return (
    <section>
      <div className="flex items-center gap-4 mb-8">
        <h2 className="text-3xl font-black text-white">Credits</h2>
        <div className="h-px flex-1 bg-gradient-to-r from-gray-800 to-transparent"></div>
      </div>

      {/* Top Filter Buttons */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-10">
        <div className="flex flex-wrap items-center gap-2">
          {categoryTabs.map((cat) => {
            const isActive = activeCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`px-4 py-2 rounded-full text-sm font-semibold transition-all duration-200 flex items-center gap-2 cursor-pointer ${
                  isActive
                    ? "bg-blue-600 text-white shadow-lg shadow-blue-900/50"
                    : "bg-gray-900 text-gray-400 hover:bg-gray-800 hover:text-white border border-gray-800"
                }`}
              >
                <span>{cat.label}</span>
                <span
                  className={`text-xs px-2 py-0.5 rounded-full ${
                    isActive ? "bg-blue-800 text-white" : "bg-gray-800 text-gray-400"
                  }`}
                >
                  {cat.count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Cast vs Crew Sub-Toggle if both exist */}
        {hasBothRoles && (
          <div className="flex items-center bg-gray-900/80 p-1 rounded-xl border border-gray-800 text-xs font-bold">
            <button
              onClick={() => setActiveRole("all")}
              className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer ${
                activeRole === "all" ? "bg-gray-800 text-white" : "text-gray-400 hover:text-white"
              }`}
            >
              All ({filteredCast.length + filteredCrew.length})
            </button>
            <button
              onClick={() => setActiveRole("cast")}
              className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer ${
                activeRole === "cast" ? "bg-gray-800 text-white" : "text-gray-400 hover:text-white"
              }`}
            >
              Cast ({filteredCast.length})
            </button>
            <button
              onClick={() => setActiveRole("crew")}
              className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer ${
                activeRole === "crew" ? "bg-gray-800 text-white" : "text-gray-400 hover:text-white"
              }`}
            >
              Crew ({filteredCrew.length})
            </button>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-16 w-full">
        {/* CAST */}
        {credits.cast.length > 0 && activeRole !== "crew" && (
          <div className="w-full">
            <h3 className="text-2xl font-bold mb-6 flex items-center gap-3">
              <span className="bg-blue-500/10 text-blue-400 px-4 py-1.5 rounded-full text-sm tracking-widest uppercase border border-blue-500/20">
                Cast
              </span>
              <span className="text-gray-500 text-base">{filteredCast.length} Credits</span>
            </h3>

            {filteredCast.length === 0 ? (
              <div className="p-8 bg-gray-900/20 rounded-2xl border border-gray-800/40 text-center text-gray-500 text-sm">
                No cast roles in this category.
              </div>
            ) : (
              <div className="space-y-6">
                {Object.entries(castGroups)
                  .sort(([a], [b]) => sortYearsDesc(a, b))
                  .map(([year, yearCredits]) => (
                    <div key={`cast-${year}`} className="space-y-3">
                      <div className="flex items-center gap-4 pt-4 pb-2">
                        <span className="text-gray-400 font-extrabold text-sm tracking-wider uppercase shrink-0">
                          {year}
                        </span>
                        <div className="h-[2px] flex-1 bg-gray-900"></div>
                      </div>

                      <div className="space-y-3">
                        {yearCredits.map((c, i) => renderCreditCard(c, i, "Actor"))}
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}

        {/* CREW */}
        {credits.crew.length > 0 && activeRole !== "cast" && (
          <div className="w-full">
            <h3 className="text-2xl font-bold mb-6 flex items-center gap-3">
              <span className="bg-emerald-500/10 text-emerald-400 px-4 py-1.5 rounded-full text-sm tracking-widest uppercase border border-emerald-500/20">
                Crew
              </span>
              <span className="text-gray-500 text-base">{filteredCrew.length} Credits</span>
            </h3>

            {filteredCrew.length === 0 ? (
              <div className="p-8 bg-gray-900/20 rounded-2xl border border-gray-800/40 text-center text-gray-500 text-sm">
                No crew credits in this category.
              </div>
            ) : (
              <div className="space-y-6">
                {Object.entries(crewGroups)
                  .sort(([a], [b]) => sortYearsDesc(a, b))
                  .map(([year, yearCredits]) => (
                    <div key={`crew-${year}`} className="space-y-3">
                      <div className="flex items-center gap-4 pt-4 pb-2">
                        <span className="text-gray-400 font-extrabold text-sm tracking-wider uppercase shrink-0">
                          {year}
                        </span>
                        <div className="h-[2px] flex-1 bg-gray-900"></div>
                      </div>

                      <div className="space-y-3">
                        {yearCredits.map((c, i) => renderCreditCard(c, i, "Crew"))}
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
