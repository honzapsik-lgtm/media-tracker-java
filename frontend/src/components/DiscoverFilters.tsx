"use client";

import { useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";

const GENRE_MAP: Record<string, { value: string; label: string }[]> = {
  movie: [
    { value: "action", label: "Action" },
    { value: "adventure", label: "Adventure" },
    { value: "animation", label: "Animation" },
    { value: "comedy", label: "Comedy" },
    { value: "crime", label: "Crime" },
    { value: "documentary", label: "Documentary" },
    { value: "drama", label: "Drama" },
    { value: "family", label: "Family" },
    { value: "fantasy", label: "Fantasy" },
    { value: "history", label: "History" },
    { value: "horror", label: "Horror" },
    { value: "music", label: "Music" },
    { value: "mystery", label: "Mystery" },
    { value: "romance", label: "Romance" },
    { value: "scifi", label: "Sci-Fi" },
    { value: "tvmovie", label: "TV Movie" },
    { value: "thriller", label: "Thriller" },
    { value: "war", label: "War" },
    { value: "western", label: "Western" },
  ],
  show: [
    { value: "actionadventure", label: "Action & Adventure" },
    { value: "animation", label: "Animation" },
    { value: "comedy", label: "Comedy" },
    { value: "crime", label: "Crime" },
    { value: "documentary", label: "Documentary" },
    { value: "drama", label: "Drama" },
    { value: "family", label: "Family" },
    { value: "kids", label: "Kids" },
    { value: "mystery", label: "Mystery" },
    { value: "news", label: "News" },
    { value: "reality", label: "Reality" },
    { value: "scififantasy", label: "Sci-Fi & Fantasy" },
    { value: "soap", label: "Soap" },
    { value: "talk", label: "Talk" },
    { value: "warpolitics", label: "War & Politics" },
    { value: "western", label: "Western" },
  ],
  game: [
    { value: "action", label: "Action" },
    { value: "adventure", label: "Adventure" },
    { value: "rpg", label: "RPG" },
    { value: "shooter", label: "Shooter" },
    { value: "strategy", label: "Strategy" },
    { value: "simulation", label: "Simulation" },
    { value: "puzzle", label: "Puzzle" },
    { value: "racing", label: "Racing" },
    { value: "sports", label: "Sports" },
    { value: "fighting", label: "Fighting" },
    { value: "platformer", label: "Platformer" },
    { value: "indie", label: "Indie" },
    { value: "arcade", label: "Arcade" },
    { value: "casual", label: "Casual" },
    { value: "boardgames", label: "Board / Card" },
    { value: "massmultiplayer", label: "MMO" },
  ],
  manga: [
    { value: "action", label: "Action" },
    { value: "adventure", label: "Adventure" },
    { value: "comedy", label: "Comedy" },
    { value: "drama", label: "Drama" },
    { value: "fantasy", label: "Fantasy" },
    { value: "horror", label: "Horror" },
    { value: "mystery", label: "Mystery" },
    { value: "psychological", label: "Psychological" },
    { value: "romance", label: "Romance" },
    { value: "scifi", label: "Sci-Fi" },
    { value: "slice", label: "Slice of Life" },
    { value: "sports", label: "Sports" },
    { value: "supernatural", label: "Supernatural" },
    { value: "suspense", label: "Suspense / Thriller" },
    { value: "historical", label: "Historical" },
    { value: "martialarts", label: "Martial Arts" },
    { value: "mecha", label: "Mecha" },
    { value: "seinen", label: "Seinen" },
    { value: "shounen", label: "Shounen" },
    { value: "shoujo", label: "Shoujo" },
    { value: "josei", label: "Josei" },
  ],
};

const selectClass =
  "bg-gray-950 border border-gray-700 text-white text-sm font-semibold rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-3 outline-none cursor-pointer hover:border-gray-500 transition-colors";

const normalize = (s: string) => s.trim().toLowerCase().replace(/[^a-z0-9]/g, "");

export default function DiscoverFilters() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [keywordDraft, setKeywordDraft] = useState("");

  const type = searchParams.get("type") || "movie";
  const year = searchParams.get("year") || "";
  const sort = searchParams.get("sort") || "popular";
  const availableGenres = GENRE_MAP[type] || GENRE_MAP.movie;

  // Accept slugs or labels (e.g. "sci-fi", "Sci-Fi", "scifi") and map them onto this type's genre slugs.
  const selectedGenres = Array.from(
    new Set(
      searchParams
        .getAll("genre")
        .flatMap((g) => g.split(","))
        .map((g) => {
          const n = normalize(g);
          return availableGenres.find((opt) => normalize(opt.value) === n || normalize(opt.label) === n)?.value || n;
        })
        .filter(Boolean)
    )
  );
  const keywords = Array.from(new Set(searchParams.getAll("keyword").map((k) => k.trim()).filter(Boolean)));

  const push = (next: { type?: string; year?: string; sort?: string; genres?: string[]; keywords?: string[] }) => {
    const params = new URLSearchParams();
    const t = next.type ?? type;
    params.set("type", t);
    const y = next.year ?? year;
    if (y) params.set("year", y);
    const s = next.sort ?? sort;
    if (s && s !== "popular") params.set("sort", s);
    // Genre slugs differ per media type, so switching type drops them; keywords carry over.
    const g = next.type && next.type !== type ? [] : next.genres ?? selectedGenres;
    g.forEach((v) => params.append("genre", v));
    (next.keywords ?? keywords).forEach((v) => params.append("keyword", v));
    router.push(`${pathname}?${params.toString()}`);
  };

  const toggleGenre = (value: string) =>
    push({ genres: selectedGenres.includes(value) ? selectedGenres.filter((g) => g !== value) : [...selectedGenres, value] });

  const addKeyword = () => {
    const kw = keywordDraft.trim();
    setKeywordDraft("");
    if (!kw || keywords.some((k) => k.toLowerCase() === kw.toLowerCase())) return;
    push({ keywords: [...keywords, kw] });
  };

  const removeKeyword = (kw: string) => push({ keywords: keywords.filter((k) => k !== kw) });

  const handleClearFilters = () => router.push(`${pathname}?type=${type}`);

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear - 1890 + 1 }, (_, i) => currentYear - i);

  const activeFilterCount = selectedGenres.length + keywords.length + (year ? 1 : 0) + (sort !== "popular" ? 1 : 0);

  return (
    <div className="mb-8 bg-gray-900 p-5 rounded-2xl border border-gray-800 shadow-xl space-y-5">
      <div className="flex flex-col md:flex-row flex-wrap gap-4 md:items-center">
        <select value={type} onChange={(e) => push({ type: e.target.value })} className={`${selectClass} font-black`}>
          <option value="movie">Movies</option>
          <option value="show">TV Shows</option>
          <option value="game">Games</option>
          <option value="manga">Manga</option>
        </select>

        <div className="w-px h-8 bg-gray-800 hidden md:block"></div>

        <select value={year} onChange={(e) => push({ year: e.target.value })} className={`${selectClass} max-h-60 overflow-y-auto`}>
          <option value="">Any Year</option>
          {years.map((y) => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>

        <select value={sort} onChange={(e) => push({ sort: e.target.value })} className={`${selectClass} md:ml-auto`}>
          <option value="popular">Trending / Popular</option>
          <option value="all_time">All-Time Most Popular</option>
          <option value="top_rated">Highest Rated</option>
          <option value="newest">Newest First</option>
        </select>

        {activeFilterCount > 0 && (
          <button
            onClick={handleClearFilters}
            className="bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500 hover:text-white text-sm font-bold rounded-lg px-4 py-3 transition-colors"
          >
            Clear Filters
          </button>
        )}
      </div>

      <div className="space-y-2">
        <div className="text-xs font-black text-gray-500 uppercase tracking-widest">
          Genres{selectedGenres.length > 1 && <span className="normal-case tracking-normal font-semibold text-gray-600"> · must match all</span>}
        </div>
        <div className="flex flex-wrap gap-1.5">
          {availableGenres.map((g) => {
            const active = selectedGenres.includes(g.value);
            return (
              <button
                key={g.value}
                type="button"
                onClick={() => toggleGenre(g.value)}
                aria-pressed={active}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold border transition-colors ${
                  active
                    ? "bg-blue-600 border-blue-500 text-white"
                    : "bg-gray-950 border-gray-800 text-gray-400 hover:text-white hover:border-gray-600"
                }`}
              >
                {g.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="space-y-2">
        <div className="text-xs font-black text-gray-500 uppercase tracking-widest">
          Keywords{keywords.length > 1 && <span className="normal-case tracking-normal font-semibold text-gray-600"> · must match all</span>}
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          {keywords.map((kw) => (
            <span key={kw} className="flex items-center gap-1 bg-blue-600/15 border border-blue-500/40 text-blue-200 pl-3 pr-1 py-1 rounded-lg text-xs font-bold">
              {kw}
              <button
                type="button"
                onClick={() => removeKeyword(kw)}
                aria-label={`Remove keyword ${kw}`}
                className="w-5 h-5 rounded-md text-blue-300 hover:text-white hover:bg-blue-500/30 transition-colors"
              >
                ×
              </button>
            </span>
          ))}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              addKeyword();
            }}
            className="flex items-center gap-1.5"
          >
            <input
              value={keywordDraft}
              onChange={(e) => setKeywordDraft(e.target.value)}
              placeholder="Add keyword, e.g. time travel"
              className="bg-gray-950 border border-gray-800 focus:border-blue-500 text-white text-xs font-semibold rounded-lg px-3 py-1.5 outline-none w-56 placeholder:text-gray-600"
            />
            {keywordDraft.trim() && (
              <button type="submit" className="text-xs font-bold text-blue-400 hover:text-white px-2 py-1.5">
                Add
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
