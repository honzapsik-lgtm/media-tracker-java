// The rating style controls how a user *gives* ratings. Scores are always
// stored as 0-100: with 5 stars each star is worth 20 (0, 20, ... 100), with
// 10 stars each star is worth 10. Community scores, statistics and covers stay
// in percent; only the user's own ratings on their profile are shown as stars.

export const RATING_STYLES = ["PERCENT", "STARS_5", "STARS_10"] as const;
export type RatingStyle = (typeof RATING_STYLES)[number];

export const DEFAULT_RATING_STYLE: RatingStyle = "PERCENT";

export const RATING_STYLE_LABELS: Record<RatingStyle, { title: string; description: string }> = {
  PERCENT: { title: "Percentage", description: "Slider from 0 to 100%" },
  STARS_5: { title: "5 stars", description: "0–5 stars, each star is 20%" },
  STARS_10: { title: "10 stars", description: "0–10 stars, each star is 10%" },
};

export function isRatingStyle(value: unknown): value is RatingStyle {
  return typeof value === "string" && (RATING_STYLES as readonly string[]).includes(value);
}

/** Number of stars for a star style, or null for percent. */
export function starCount(style: RatingStyle): 5 | 10 | null {
  if (style === "STARS_5") return 5;
  if (style === "STARS_10") return 10;
  return null;
}

/** Whole stars closest to a 0-100 score (older ratings may sit between stars). */
export function scoreToStars(score: number, stars: number): number {
  return Math.round((Math.min(100, Math.max(0, score)) / 100) * stars);
}

/** The stored 0-100 score for a number of whole stars. */
export function starsToScore(filled: number, stars: number): number {
  return Math.round((filled / stars) * 100);
}

/** Snaps a 0-100 score onto the style's scale (unchanged for percent). */
export function snapScore(score: number, style: RatingStyle): number {
  const stars = starCount(style);
  return stars === null ? score : starsToScore(scoreToStars(score, stars), stars);
}
