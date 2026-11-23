"use client";

import Link from "next/link";
import { Score } from "@/components/Score";
import { coverSrc } from "@/lib/images";

export interface Insights {
  totalRatings: number;
  average: number;
  reviews: number;
  deepReviews: number;
  perfectScores: number;
  byType: { type: string; count: number; average: number }[];
  monthly: { month: string; count: number }[];
  decades: { decade: string; count: number }[];
  currentStreak: number;
  longestStreak: number;
  episodesWatched: number;
  chaptersRead: number;
  hoursPlayed: number;
  movieWatches: number;
  completed: number;
  topRated: { mediaId: string; title: string | null; image: string | null; score: number }[];
}

const TYPE_LABELS: Record<string, string> = {
  movie: "Movies",
  show: "TV shows",
  season: "Seasons",
  game: "Games",
  manga: "Manga",
};

const fmt = (n: number) => n.toLocaleString("en-US");

function Card({ title, children, className = "" }: { title: string; children: React.ReactNode; className?: string }) {
  return (
    <div className={`bg-gray-900 p-6 rounded-2xl border border-gray-800 shadow-xl ${className}`}>
      <h3 className="text-lg font-black mb-5">{title}</h3>
      {children}
    </div>
  );
}

function Tile({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
      <div className="text-2xl font-black text-white tabular-nums">{value}</div>
      <div className="text-[11px] font-bold uppercase tracking-wider text-gray-500 mt-1">{label}</div>
    </div>
  );
}

/** One-series vertical bars; the value appears on hover/focus, labels sit under each bar. */
function ColumnChart({ data, ariaLabel }: { data: { label: string; value: number; title: string }[]; ariaLabel: string }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div role="img" aria-label={ariaLabel} className="flex items-end h-40 gap-[2px]">
      {data.map((d) => (
        <div key={d.label} className="group flex-1 h-full flex flex-col items-center justify-end gap-1.5 min-w-0" title={d.title}>
          <span className="text-[10px] font-bold text-gray-300 tabular-nums opacity-0 group-hover:opacity-100 transition-opacity">
            {d.value}
          </span>
          <div
            className="w-full max-w-8 rounded-t bg-blue-600 group-hover:bg-blue-400 transition-colors"
            style={{ height: `${d.value === 0 ? 0 : Math.max(3, (d.value / max) * 100)}%` }}
          />
          <span className="text-[10px] text-gray-500 font-bold truncate max-w-full">{d.label}</span>
        </div>
      ))}
    </div>
  );
}

export default function ProfileInsights({ insights }: { insights: Insights | null | undefined }) {
  if (!insights || insights.totalRatings === 0) {
    return (
      <div className="text-center py-12 text-gray-500 italic text-sm border border-gray-800 border-dashed rounded-xl">
        Rate a few titles to see your statistics.
      </div>
    );
  }

  const typeRows = insights.byType.filter((t) => t.count > 0);
  const maxType = Math.max(1, ...typeRows.map((t) => t.count));
  const monthly = insights.monthly.map((m) => {
    const date = new Date(`${m.month}-01T00:00:00Z`);
    const short = date.toLocaleDateString("en-US", { month: "short", timeZone: "UTC" });
    const long = date.toLocaleDateString("en-US", { month: "long", year: "numeric", timeZone: "UTC" });
    return { label: short, value: m.count, title: `${long}: ${m.count} ratings` };
  });
  const decades = insights.decades.map((d) => ({ label: `'${d.decade.slice(2)}`, value: d.count, title: `${d.decade}: ${d.count} titles` }));

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <Tile label="Ratings" value={fmt(insights.totalRatings)} />
        <Tile label="Average" value={<Score value={insights.average} kind="average" />} />
        <Tile label="Reviews" value={fmt(insights.reviews)} />
        <Tile label="Advanced ratings" value={fmt(insights.deepReviews)} />
        <Tile label="Perfect scores" value={fmt(insights.perfectScores)} />
        <Tile label="Day streak (best)" value={<>{insights.currentStreak}<span className="text-gray-500 text-base"> ({insights.longestStreak})</span></>} />
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <Card title="By media type">
          <div className="space-y-3">
            {typeRows.map((t) => (
              <div key={t.type} className="group" title={`${TYPE_LABELS[t.type] ?? t.type}: ${t.count} ratings`}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="font-bold text-gray-300">{TYPE_LABELS[t.type] ?? t.type}</span>
                  <span className="text-gray-400 tabular-nums">
                    {fmt(t.count)} <span className="text-gray-600">·</span> avg <Score value={t.average} kind="average" className="text-gray-200 font-bold" />
                  </span>
                </div>
                <div className="h-2 rounded bg-gray-800 overflow-hidden">
                  <div className="h-full rounded bg-blue-600 group-hover:bg-blue-400 transition-colors" style={{ width: `${(t.count / maxType) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Ratings per month">
          <ColumnChart data={monthly} ariaLabel="Ratings per month over the last year" />
        </Card>

        <Card title="Release decades">
          {decades.length ? (
            <ColumnChart data={decades} ariaLabel="Rated titles by release decade" />
          ) : (
            <p className="text-sm text-gray-500">No release dates yet.</p>
          )}
        </Card>

        <Card title="Time spent">
          <div className="grid grid-cols-2 gap-3">
            <Tile label="Episodes watched" value={fmt(insights.episodesWatched)} />
            <Tile label="Movie watches" value={fmt(insights.movieWatches)} />
            <Tile label="Hours played" value={fmt(insights.hoursPlayed)} />
            <Tile label="Chapters read" value={fmt(insights.chaptersRead)} />
          </div>
          <p className="text-sm text-gray-400 mt-4">
            <span className="font-black text-white">{fmt(insights.completed)}</span> titles completed on lists
          </p>
        </Card>
      </div>

      {insights.topRated.length > 0 && (
        <Card title="Highest rated">
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-4">
            {insights.topRated.map((t) => (
              <Link key={t.mediaId} href={`/media/${t.mediaId}`} className="group block min-w-0">
                <div className="aspect-[2/3] rounded-lg overflow-hidden bg-gray-800 border border-gray-800 group-hover:border-blue-500 transition-colors">
                  {t.image && <img src={coverSrc(t.image)} alt={t.title ?? ""} className="w-full h-full object-cover" />}
                </div>
                <p className="text-xs font-bold text-gray-300 truncate mt-2 group-hover:text-white">{t.title}</p>
                <Score value={t.score} className="text-xs font-black text-blue-400" />
              </Link>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
