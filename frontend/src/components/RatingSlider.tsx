"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { CRITERIA_CONFIG } from "@/lib/constants";
import RankListSelectModal from "@/components/RankListSelectModal";
import { StarPicker } from "@/components/Score";
import { useRatingStyle } from "@/components/RatingStyleProvider";
import { scoreToStars, snapScore, starCount } from "@/lib/rating-style";

type RatingMediaType = "game" | "movie" | "show" | "season" | "episode" | "manga";

export const getScoreColor = (score: number | null | undefined) => {
  if (score === null || score === undefined) return "text-gray-500";
  if (score >= 95) return "text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.6)]"; // Gold
  if (score >= 75) return "text-green-400";
  if (score >= 50) return "text-blue-400";
  if (score >= 25) return "text-gray-400";
  return "text-gray-700"; // Black/Dark Grey
};

interface RatingSliderProps {
  mediaId: string; mediaType: RatingMediaType;
  mediaTitle: string; mediaImage: string | null;
  mediaReleaseDate?: string | null;
  initialRating?: number; initialCriteria?: Record<string, number>;
  onSaved?: (score: number) => void;
}

export default function RatingSlider({ mediaId, mediaType, mediaTitle, mediaImage, mediaReleaseDate, initialRating = 50, initialCriteria, onSaved }: RatingSliderProps) {
  const router = useRouter();
  const { ratingStyle } = useRatingStyle();
  const stars = starCount(ratingStyle);
  const [rating, setRating] = useState<number>(initialRating);
  const [hasRated, setHasRated] = useState<boolean>(false);
  // Sub-scores stay hidden until the user opens "Advanced rating"; only then are they saved.
  const [showAdvanced, setShowAdvanced] = useState<boolean>(false);
  // True when the saved rating already has sub-scores, so a master-only update keeps them.
  const [hasSavedCriteria, setHasSavedCriteria] = useState<boolean>(
    !!initialCriteria && Object.keys(initialCriteria).length > 0
  );
  
  const [criteria, setCriteria] = useState<Record<string, number>>(() => {
    const config = CRITERIA_CONFIG[mediaType] || [];
    const initial: Record<string, number> = {};
    config.forEach((item) => { initial[item.key] = 50; });
    if (initialCriteria && Object.keys(initialCriteria).length > 0) {
      return { ...initial, ...initialCriteria };
    }
    return initial;
  });

  useEffect(() => {
    if (initialCriteria && Object.keys(initialCriteria).length > 0) {
      setCriteria(prev => ({ ...prev, ...initialCriteria }));
      setHasRated(true);
      setHasSavedCriteria(true);
    }
  }, [initialCriteria]);

  useEffect(() => {
    if (typeof initialRating === "number" && initialRating !== 50) {
      setRating(initialRating);
      setHasRated(true);
    }
  }, [initialRating]);

  const [globalCriteriaAverages, setGlobalCriteriaAverages] = useState<Record<string, number>>({});
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [showListModal, setShowListModal] = useState<boolean>(false);

  const fetchData = useCallback(async () => {
    try {
      const res = await fetch(`/api/ratings?mediaId=${encodeURIComponent(mediaId)}`);
      if (!res.ok) return;
      const data = await res.json() as {
        personal?: {
          score: number;
          is_deep_review?: boolean | null;
          isDeepReview?: boolean | null;
          criteria_scores?: Record<string, number> | null;
          criteriaScores?: Record<string, number> | null;
        } | null;
        globalCriteriaAverages?: Record<string, number>;
      };

      if (data.personal) {
        setRating(data.personal.score);
        setHasRated(true);
        const scores = data.personal.criteriaScores || data.personal.criteria_scores;
        const isDeep = data.personal.isDeepReview ?? data.personal.is_deep_review;
        if (scores && typeof scores === "object" && Object.keys(scores).length > 0) {
          setCriteria(prev => ({ ...prev, ...scores }));
          setHasSavedCriteria(isDeep !== false);
        } else {
          setHasSavedCriteria(false);
        }
      }

      setGlobalCriteriaAverages(data.globalCriteriaAverages ?? {});
    } catch (err) { console.error(err); }
  }, [mediaId]);

  useEffect(() => {
    // The async fetch hydrates client-only rating state after the first render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchData();
  }, [fetchData]);

  const updateCriterion = (key: string, value: number) => { setCriteria((prev) => ({ ...prev, [key]: value })); };

  const globalKeys = Object.keys(globalCriteriaAverages);
  const overallGlobalAverage = globalKeys.length > 0
    ? Math.round(globalKeys.reduce((sum, key) => sum + globalCriteriaAverages[key], 0) / globalKeys.length)
    : null;

  const handleSave = async () => {
    setIsSaving(true); setMessage(null);
    try {
      // Star styles only allow whole stars, so snap (older or default values may sit between stars).
      const score = snapScore(rating, ratingStyle);
      // Expanded: save the sub-scores. Collapsed: send none, so the backend keeps any saved earlier.
      const payloadCriteria = showAdvanced
        ? Object.fromEntries(Object.entries(criteria).map(([k, v]) => [k, snapScore(v, ratingStyle)]))
        : undefined;
      const isDeepReview = showAdvanced || hasSavedCriteria;

      const res = await fetch("/api/ratings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mediaId,
          mediaType,
          score,
          isDeepReview,
          criteriaScores: payloadCriteria,
          mediaTitle,
          mediaImage,
          mediaReleaseDate,
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || "Something went wrong.");
      }
      setMessage({ type: "success", text: "Rating saved successfully!" });
      setHasRated(true);
      fetchData(); 
      onSaved?.(score);
      setRating(score);
      router.refresh(); // Tells Next.js to instantly reload the server data on the main page
      if (!hasRated) {
        setShowListModal(true); // Open the Rank List prompt only for new ratings
      }
    } catch (err) {
      setMessage({ type: "error", text: err instanceof Error ? err.message : "Something went wrong." });
    } 
    finally { setIsSaving(false); }
  };

  return (
    <div className="bg-gray-900 border border-gray-800 p-6 rounded-2xl max-w-md w-full space-y-6 shadow-xl">
      <div className="flex justify-between items-center gap-3 border-b border-gray-800 pb-3">
        <h3 className="font-black text-xl tracking-tight text-white whitespace-nowrap">Your Rating</h3>
        {(CRITERIA_CONFIG[mediaType] || []).length > 0 && (
          <button
            type="button"
            onClick={() => setShowAdvanced((v) => !v)}
            aria-expanded={showAdvanced}
            title={!showAdvanced && hasSavedCriteria ? "Advanced sub-scores saved" : undefined}
            className={`shrink-0 flex items-center gap-1.5 text-xs font-bold border rounded-lg px-2.5 py-1.5 transition-colors ${showAdvanced ? "text-white border-blue-500/50 bg-blue-500/10" : "text-gray-400 hover:text-white bg-gray-950/50 border-gray-800 hover:border-gray-700"}`}
          >
            {!showAdvanced && hasSavedCriteria && <span className="w-1.5 h-1.5 rounded-full bg-blue-500" aria-hidden="true" />}
            <span>Advanced</span>
            <span className={`transition-transform ${showAdvanced ? "rotate-180" : ""}`}>▾</span>
          </button>
        )}
      </div>

      <div className="space-y-2">
        <div className="flex justify-between items-end">
          <span className="text-sm font-bold text-gray-400">Master Score</span>
          {stars === null ? (
            <span className={`text-3xl font-black ${getScoreColor(rating)}`}>{rating}%</span>
          ) : (
            <span className="text-sm font-bold text-gray-400">
              <span className="text-2xl font-black text-white">{scoreToStars(rating, stars)}</span>/{stars}
            </span>
          )}
        </div>
        {stars === null ? (
          <input type="range" min="0" max="100" value={rating} onChange={(e) => setRating(Number(e.target.value))} className="w-full h-2 bg-gray-800 rounded-lg appearance-none cursor-pointer accent-blue-500" />
        ) : (
          <StarPicker value={rating} onChange={setRating} stars={stars} sizeClass={stars === 10 ? "w-[18px] h-[18px]" : "w-9 h-9"} label="Master score" />
        )}
      </div>

      {showAdvanced && (
      <div className="space-y-4 bg-gray-950/50 p-4 rounded-xl border border-gray-800">
        {(CRITERIA_CONFIG[mediaType] || []).map((item) => (
          <div key={item.key} className="space-y-1.5">
            <div className="flex justify-between text-xs font-bold">
              <span className="text-gray-400">{item.label}</span>
              {stars === null ? (
                <span className={getScoreColor(criteria[item.key] ?? 50)}>{criteria[item.key] ?? 50}%</span>
              ) : (
                <span className="text-gray-400">{scoreToStars(criteria[item.key] ?? 50, stars)}/{stars}</span>
              )}
            </div>
            {stars === null ? (
              <input type="range" min="0" max="100" value={criteria[item.key] ?? 50} onChange={(e) => updateCriterion(item.key, Number(e.target.value))} className="w-full h-1.5 bg-gray-800 rounded-lg appearance-none cursor-pointer accent-blue-500" />
            ) : (
              <StarPicker value={criteria[item.key] ?? 50} onChange={(v) => updateCriterion(item.key, v)} stars={stars} sizeClass={stars === 10 ? "w-3.5 h-3.5" : "w-5 h-5"} label={item.label} />
            )}
          </div>
        ))}
      </div>
      )}

      {message && <div className={`p-3 rounded-lg text-xs font-bold text-center border ${message.type === "success" ? "bg-green-500/10 border-green-500/20 text-green-400" : "bg-red-500/10 border-red-500/20 text-red-400"}`}>{message.text}</div>}
      <button onClick={handleSave} disabled={isSaving} className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-gray-800 disabled:text-gray-600 text-white font-black py-3 px-4 rounded-xl transition-all tracking-wide text-sm shadow-lg shadow-blue-600/10">
        {isSaving ? "Saving..." : hasRated ? "Update Rating Data" : "Save Rating Data"}
      </button>

      {showListModal && (
        <RankListSelectModal
          item={{
            id: mediaId,
            type: mediaType,
            title: mediaTitle,
            image: mediaImage,
            releaseDate: mediaReleaseDate ?? null,
            communityScore: 0, // Dummies for TS type requirement, unused by modal
            listRank: null,
          }}
          onClose={() => setShowListModal(false)}
        />
      )}
    </div>
  );
}
