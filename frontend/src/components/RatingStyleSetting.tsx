"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useRatingStyle } from "@/components/RatingStyleProvider";
import { StarRow } from "@/components/Score";
import { RATING_STYLES, RATING_STYLE_LABELS, type RatingStyle } from "@/lib/rating-style";

export default function RatingStyleSetting() {
  const router = useRouter();
  const { ratingStyle, setRatingStyle } = useRatingStyle();
  const [saving, setSaving] = useState<RatingStyle | null>(null);
  const [error, setError] = useState<string | null>(null);

  const choose = async (style: RatingStyle) => {
    if (style === ratingStyle || saving) return;
    const previous = ratingStyle;
    setError(null);
    setSaving(style);
    setRatingStyle(style); // optimistic: every score on screen updates immediately
    try {
      const res = await fetch("/api/profile/preferences", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ratingStyle: style }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || "Could not save the setting.");
      }
      router.refresh();
    } catch (err) {
      setRatingStyle(previous);
      setError(err instanceof Error ? err.message : "Could not save the setting.");
    } finally {
      setSaving(null);
    }
  };

  return (
    <section className="p-6 bg-gray-900 border border-gray-800 rounded-xl">
      <h2 className="text-lg font-black mb-5">Rating style</h2>

      <div role="radiogroup" aria-label="Rating style" className="grid gap-3 sm:grid-cols-3">
        {RATING_STYLES.map((style) => {
          const selected = style === ratingStyle;
          return (
            <button
              key={style}
              type="button"
              role="radio"
              aria-checked={selected}
              disabled={saving !== null}
              onClick={() => choose(style)}
              className={`text-left p-4 rounded-lg border transition-colors disabled:cursor-wait ${
                selected
                  ? "border-blue-500 bg-blue-950/40 ring-1 ring-blue-500"
                  : "border-gray-800 bg-gray-950 hover:border-gray-600"
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <span className="font-bold text-sm">{RATING_STYLE_LABELS[style].title}</span>
                <span
                  className={`w-4 h-4 rounded-full border-2 ${selected ? "border-blue-500 bg-blue-500" : "border-gray-600"}`}
                  aria-hidden="true"
                />
              </div>
              {style === "PERCENT" ? (
                <span className="text-green-400 font-black text-lg">60%</span>
              ) : (
                <StarRow
                  filled={style === "STARS_5" ? 3 : 6}
                  total={style === "STARS_5" ? 5 : 10}
                  sizeClass={style === "STARS_10" ? "w-3 h-3" : "w-4 h-4"}
                />
              )}
            </button>
          );
        })}
      </div>

      <p className="text-xs mt-3 h-4" aria-live="polite">
        {saving && <span className="text-gray-500">Saving…</span>}
        {error && <span className="text-red-400">{error}</span>}
      </p>
    </section>
  );
}
