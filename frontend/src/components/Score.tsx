"use client";

import { useState } from "react";
import { useRatingStyle } from "@/components/RatingStyleProvider";
import { formatAverage, formatRating, scoreToStars, starCount, starsToScore } from "@/lib/rating-style";

/**
 * A score in the viewer's rating style. `kind="rating"` (one person's rating) shows whole
 * stars, e.g. "4/5 ★"; `kind="average"` (community score, statistics) keeps one decimal,
 * e.g. "4.3/5 ★". In percent style both show "%". Works inside server components too.
 */
export function Score({
  value,
  kind = "rating",
  fallback = "-",
  percentPrefix = "",
  className,
}: {
  value: number | null | undefined;
  kind?: "rating" | "average";
  fallback?: string;
  /** Shown before the value in percent style only (star styles already end in ★). */
  percentPrefix?: string;
  className?: string;
}) {
  const { ratingStyle } = useRatingStyle();
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return <span className={className}>{starCount(ratingStyle) === null ? percentPrefix : ""}{fallback}</span>;
  }
  const n = Number(value);
  const text = kind === "average" ? formatAverage(n, ratingStyle) : formatRating(n, ratingStyle);
  const prefix = starCount(ratingStyle) === null ? percentPrefix : "";
  return <span className={className}>{prefix}{text}</span>;
}

/** Percent ranges shown in the viewer's style ("0-20" → "0-2★" with 10 stars). */
export function ScoreRangeLabel({ from, to }: { from: number; to: number }) {
  const { ratingStyle } = useRatingStyle();
  const stars = starCount(ratingStyle);
  if (stars === null) return <>{from}-{to}</>;
  const f = (v: number) => Number(((v / 100) * stars).toFixed(1));
  return <>{f(from)}-{f(to)}★</>;
}

/** A row of whole stars: the first `filled` lit, the rest dim. */
export function StarRow({ filled, total, sizeClass = "w-5 h-5" }: { filled: number; total: number; sizeClass?: string }) {
  return (
    <span className="inline-flex items-center gap-0.5" aria-hidden="true">
      {Array.from({ length: total }, (_, i) => (
        <StarIcon key={i} className={`${sizeClass} ${i < filled ? "text-yellow-400" : "text-gray-700"}`} />
      ))}
    </span>
  );
}

/**
 * Whole-star rating input. `value` and `onChange` use the 0-100 scale; picking
 * 3 of 5 stars reports 60. A "0" button allows a zero-star rating.
 */
export function StarPicker({
  value,
  onChange,
  stars,
  sizeClass = "w-8 h-8",
  label,
}: {
  value: number;
  onChange: (score: number) => void;
  stars: 5 | 10;
  sizeClass?: string;
  label: string;
}) {
  const [hover, setHover] = useState<number | null>(null);
  const selected = scoreToStars(value, stars);
  const shown = hover ?? selected;
  // Lit stars take the colour of the score they stand for, following the hover.
  const litColor = starTierColor(starsToScore(shown, stars));

  return (
    <div role="radiogroup" aria-label={label} className={`flex flex-wrap items-center ${stars === 10 ? "gap-0" : "gap-1"}`} onMouseLeave={() => setHover(null)}>
      <button
        type="button"
        role="radio"
        aria-checked={selected === 0}
        aria-label="0 stars"
        onClick={() => onChange(0)}
        onMouseEnter={() => setHover(0)}
        className={`mr-1 w-6 h-6 rounded-full text-[10px] font-black border transition-colors ${
          selected === 0 ? "border-blue-500 text-blue-400 bg-blue-500/10" : "border-gray-700 text-gray-500 hover:border-gray-500"
        }`}
      >
        0
      </button>
      {Array.from({ length: stars }, (_, i) => {
        const n = i + 1;
        return (
          <button
            key={n}
            type="button"
            role="radio"
            aria-checked={selected === n}
            aria-label={`${n} star${n === 1 ? "" : "s"}`}
            onClick={() => onChange(starsToScore(n, stars))}
            onMouseEnter={() => setHover(n)}
            onFocus={() => setHover(n)}
            onBlur={() => setHover(null)}
            className="p-0.5 rounded transition-transform hover:scale-110 focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue-500"
          >
            <StarIcon className={`${sizeClass} transition-colors ${n <= shown ? litColor : "text-gray-800"}`} />
          </button>
        );
      })}
    </div>
  );
}

/** Same tiers as the percentage colours: gold 95+, green 75+, blue 50+, grey below. */
export function starTierColor(score: number): string {
  if (score >= 95) return "text-yellow-400 drop-shadow-[0_0_6px_rgba(250,204,21,0.6)]";
  if (score >= 75) return "text-green-400";
  if (score >= 50) return "text-blue-400";
  if (score >= 25) return "text-gray-400";
  return "text-gray-600";
}

function StarIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M12 2.5l2.94 5.96 6.58.96-4.76 4.64 1.12 6.55L12 17.52l-5.88 3.09 1.12-6.55L2.48 9.42l6.58-.96L12 2.5z" />
    </svg>
  );
}
