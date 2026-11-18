"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { MediaItem } from "@/types";
import MediaCardHorizontal from "./MediaCardHorizontal";
import FriendActionButton from "./FriendActionButton";
import { Users, Film, Tv, Gamepad2, BookOpen, Layers, MapPin, Star, User } from "lucide-react";

interface SearchUser {
  id: string;
  name: string | null;
  username: string | null;
  image: string | null;
  country?: string | null;
  stateRegion?: string | null;
  _count?: {
    ratings: number;
  };
}

interface SearchResultsTabsProps {
  results: MediaItem[];
  userResults?: SearchUser[];
}

type Tab = "all" | "movies" | "shows" | "games" | "manga" | "people" | "users";

export default function SearchResultsTabs({
  results,
  userResults = [],
}: SearchResultsTabsProps) {
  const [activeTab, setActiveTab] = useState<Tab>("all");

  useEffect(() => {
    if (typeof window !== "undefined") {
      sessionStorage.setItem("lastSearchUrl", window.location.pathname + window.location.search);
    }
  }, []);

  const peopleResults = results.filter((r) => r.type === "person");

  // Media tabs strictly display only media
  const filteredResults = results.filter((item) => {
    if (activeTab === "all") return item.type !== "person";
    if (activeTab === "movies") return item.type === "movie";
    if (activeTab === "shows") return item.type === "show";
    if (activeTab === "games") return item.type === "game";
    if (activeTab === "manga") return item.type === "manga";
    if (activeTab === "people") return item.type === "person";
    return false;
  });

  const tabs: { id: Tab; label: string; icon: any; count: number }[] = [
    {
      id: "all",
      label: "All",
      icon: Layers,
      count: results.length,
    },
    {
      id: "movies",
      label: "Movies",
      icon: Film,
      count: results.filter((r) => r.type === "movie").length,
    },
    {
      id: "shows",
      label: "TV Shows",
      icon: Tv,
      count: results.filter((r) => r.type === "show").length,
    },
    {
      id: "games",
      label: "Games",
      icon: Gamepad2,
      count: results.filter((r) => r.type === "game").length,
    },
    {
      id: "manga",
      label: "Manga",
      icon: BookOpen,
      count: results.filter((r) => r.type === "manga").length,
    },
    {
      id: "people",
      label: "People",
      icon: User,
      count: peopleResults.length,
    },
    {
      id: "users",
      label: "Users",
      icon: Users,
      count: userResults.length,
    },
  ];

  return (
    <div>
      {/* The Tabs UI */}
      <div className="flex flex-wrap gap-2 mb-8 border-b border-gray-800 pb-4">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-full text-sm font-semibold transition-all duration-200 flex items-center gap-2 ${
                isActive
                  ? tab.id === "users"
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-900/50"
                    : "bg-blue-600 text-white shadow-lg shadow-blue-900/50"
                  : "bg-gray-900 text-gray-400 hover:bg-gray-800 hover:text-white border border-gray-800"
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
              <span
                className={`text-xs px-2 py-0.5 rounded-full ${
                  isActive
                    ? tab.id === "users"
                      ? "bg-purple-800"
                      : "bg-blue-800"
                    : "bg-gray-800"
                }`}
              >
                {tab.count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Users Tab Content */}
      {activeTab === "users" ? (
        userResults.length === 0 ? (
          <p className="text-gray-400 text-center py-12">
            No users found matching your search.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {userResults.map((user) => {
              const displayName = user.name || user.username || "User";
              const profileLink = user.username
                ? `/user/${user.username}`
                : `/user/${user.id}`;
              const location = [user.stateRegion, user.country]
                .filter(Boolean)
                .join(", ");

              return (
                <div
                  key={user.id}
                  className="bg-zinc-900/70 border border-zinc-800 rounded-2xl p-4 flex items-center justify-between gap-4 hover:border-zinc-700 transition-colors shadow-lg"
                >
                  <Link
                    href={profileLink}
                    className="flex items-center gap-3.5 min-w-0 group"
                  >
                    {user.image ? (
                      <img
                        src={user.image}
                        alt={displayName}
                        className="w-12 h-12 rounded-full object-cover border border-zinc-700 shrink-0 group-hover:opacity-85 transition-opacity"
                      />
                    ) : (
                      <div className="w-12 h-12 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center text-sm font-bold text-zinc-300 shrink-0">
                        {displayName[0]?.toUpperCase()}
                      </div>
                    )}

                    <div className="min-w-0">
                      <p className="text-sm font-bold text-white group-hover:text-purple-400 truncate transition-colors">
                        {displayName}
                      </p>
                      {user.username && (
                        <p className="text-xs text-zinc-400 font-mono truncate">
                          @{user.username}
                        </p>
                      )}

                      <div className="flex items-center gap-3 mt-1 text-[11px] text-zinc-500">
                        {location && (
                          <span className="flex items-center gap-1 truncate">
                            <MapPin className="w-3 h-3 shrink-0" />
                            {location}
                          </span>
                        )}
                        {typeof user._count?.ratings === "number" && (
                          <span className="flex items-center gap-1 shrink-0">
                            <Star className="w-3 h-3 text-yellow-500/70 fill-yellow-500/70" />
                            {user._count.ratings} rated
                          </span>
                        )}
                      </div>
                    </div>
                  </Link>

                  <div className="shrink-0">
                    <FriendActionButton
                      targetUserId={user.id}
                      targetUsername={user.username}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : activeTab === "people" ? (
        /* People Tab Content */
        peopleResults.length === 0 ? (
          <p className="text-gray-400 text-center py-12">
            No people or actors found matching your search.
          </p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {peopleResults.map((person) => (
              <Link
                key={person.id}
                href={`/person/${person.id}`}
                className="bg-gray-900/80 border border-gray-800 rounded-2xl p-4 flex items-center gap-4 hover:border-blue-500/50 hover:bg-gray-800 transition-all shadow-lg group"
              >
                <div className="w-16 h-16 rounded-full overflow-hidden bg-gray-800 border border-gray-700 group-hover:border-blue-500 shrink-0 transition-colors flex items-center justify-center">
                  {person.image ? (
                    <img
                      src={person.image}
                      alt={person.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="text-lg font-bold text-gray-500">
                      {person.title ? person.title.charAt(0) : "P"}
                    </div>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="font-bold text-base text-gray-200 group-hover:text-blue-400 truncate transition-colors">
                    {person.title}
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800/60 inline-block">
                      {person.releaseDate || "Person"}
                    </span>
                  </div>
                  {person.description && (
                    <p className="text-xs text-gray-400 truncate mt-1.5">
                      {person.description}
                    </p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )
      ) : (
        /* Media / All Results */
        <div>
          {/* Spotlight on People in All tab if matching people exist */}
          {activeTab === "all" && peopleResults.length > 0 && (
            <div className="mb-8 bg-gray-900/40 p-5 rounded-2xl border border-gray-800/60">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-xs font-black uppercase tracking-widest text-amber-400 flex items-center gap-2">
                  <User className="w-3.5 h-3.5" /> People &amp; Actors
                </h3>
                {peopleResults.length > 3 && (
                  <button
                    onClick={() => setActiveTab("people")}
                    className="text-xs font-semibold text-blue-400 hover:text-blue-300 transition-colors"
                  >
                    View all ({peopleResults.length}) →
                  </button>
                )}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {peopleResults.slice(0, 3).map((person) => (
                  <Link
                    key={`spotlight-${person.id}`}
                    href={`/person/${person.id}`}
                    className="bg-gray-900 border border-gray-800 rounded-xl p-3.5 flex items-center gap-3.5 hover:border-blue-500/50 hover:bg-gray-800/80 transition-all group"
                  >
                    <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-800 border border-gray-700 group-hover:border-blue-500 shrink-0 flex items-center justify-center">
                      {person.image ? (
                        <img
                          src={person.image}
                          alt={person.title}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="text-sm font-bold text-gray-500">
                          {person.title ? person.title.charAt(0) : "P"}
                        </div>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-bold text-sm text-gray-200 group-hover:text-blue-400 truncate transition-colors">
                        {person.title}
                      </h4>
                      <p className="text-[11px] text-gray-400 truncate mt-0.5">
                        {person.description || person.releaseDate || "Person"}
                      </p>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          )}

          {filteredResults.length === 0 ? (
            <p className="text-gray-400 text-center py-12">
              No results found in this category.
            </p>
          ) : (
            <div className="space-y-2">
              {filteredResults.map((item, index) => (
                <MediaCardHorizontal key={`${item.id}-${index}`} item={item} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}