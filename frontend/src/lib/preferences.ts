import { cache } from "react";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import { apiFetch } from "@/lib/api-client";
import { DEFAULT_RATING_STYLE, isRatingStyle, type RatingStyle } from "@/lib/rating-style";

/** The signed-in user's rating style, or the default for guests / on backend errors. Cached per request. */
export const getRatingStyle = cache(async (): Promise<RatingStyle> => {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.id) return DEFAULT_RATING_STYLE;
    const prefs = await apiFetch<{ ratingStyle?: string }>("/profile/preferences", { cache: "no-store" });
    return isRatingStyle(prefs.ratingStyle) ? prefs.ratingStyle : DEFAULT_RATING_STYLE;
  } catch (error) {
    console.error("[preferences] Failed to load rating style:", error);
    return DEFAULT_RATING_STYLE;
  }
});
