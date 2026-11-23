"use client";

import { useState } from "react";
import { useRatingStyle } from "@/components/RatingStyleProvider";
import { scoreToStars, starCount, starsToScore } from "@/lib/rating-style";

/**
 * The signed-in user's own rating, in their chosen style: "73%" for percent,
 * or whole stars (e.g. ★★★★☆) for star styles. Only use for the user's own ratings.
 */
export function OwnScore({
  value,
  fallback = "-",
  compact = false,
  className,
}: {
  value: number | null | undefined;
  fallback?: string;
  /** Text form ("4/5 ★") for tight spaces instead of star icons. */
  compact?: boolean;
  className?: string;
}) {
  const { ratingStyle } = useRatingStyle();
  if (value === null || value === undefined || Number.isNaN(value)) {
    return <span className={className}>{fallback}</span>;
  }

  const stars = starCount(ratingStyle);
  if (stars === null) return <span className={className}>{value}%</span>;

  const filled = scoreToStars(value, stars);
  const label = `${filled} of ${stars} stars`;
  if (compact) {
    return <span className={className} title={label}>{filled}/{stars} ★</span>;
  }
  return (
    <span className={`inline-flex flex-col items-end gap-0.5 ${className ?? ""}`} title={label}>
      <StarRow filled={filled} total={stars} sizeClass={stars === 10 ? "w-2.5 h-2.5" : "w-3.5 h-3.5"} />
      <span className="text-[10px] text-gray-500 font-bold">{filled}/{stars}</span>
    </span>
  );
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
            <StarIcon className={`${sizeClass} transition-colors ${n <= shown ? "text-yellow-400" : "text-gray-700"}`} />
          </button>
        );
      })}
    </div>
  );
}

function StarIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M12 2.5l2.94 5.96 6.58.96-4.76 4.64 1.12 6.55L12 17.52l-5.88 3.09 1.12-6.55L2.48 9.42l6.58-.96L12 2.5z" />
    </svg>
  );
}
