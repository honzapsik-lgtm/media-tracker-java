"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { discoverMedia, type DiscoverMediaItem } from "@/app/actions";
import MediaCardVertical from "./MediaCardVertical";

interface Props {
  initialItems: DiscoverMediaItem[];
  type: string;
  genre: string;
  year: string;
  sort: string;
  keywords: string[];
}

// The providers cap how deep you can page (TMDb stops at 500); this keeps a runaway scroll bounded.
const MAX_PAGES = 100;

/** Discover grid that keeps loading the next page as you scroll toward the bottom. */
export default function DiscoverResults({ initialItems, type, genre, year, sort, keywords }: Props) {
  const [items, setItems] = useState<DiscoverMediaItem[]>(initialItems);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(initialItems.length === 0);
  const [failed, setFailed] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const loadingRef = useRef(false);

  const loadMore = useCallback(async () => {
    if (loadingRef.current || done) return;
    loadingRef.current = true;
    setLoading(true);
    setFailed(false);
    try {
      const nextPage = page + 1;
      const next = await discoverMedia(type, genre, year, sort, keywords, nextPage);
      setItems((prev) => {
        const seen = new Set(prev.map((i) => i.id));
        const fresh = next.filter((i) => i.id && !seen.has(i.id));
        // An empty page, or one with nothing new, means the provider has run out.
        if (fresh.length === 0 || nextPage >= MAX_PAGES) setDone(true);
        return [...prev, ...fresh];
      });
      setPage(nextPage);
    } catch {
      setFailed(true);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [page, done, type, genre, year, sort, keywords]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el || done || failed) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) loadMore();
      },
      { rootMargin: "800px 0px" } // start fetching well before the user reaches the end
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [loadMore, done, failed]);

  if (items.length === 0) {
    return (
      <div className="text-center py-20 bg-gray-900/50 rounded-2xl border border-gray-800 border-dashed">
        <p className="text-gray-400 text-lg">No results found for these filters.</p>
      </div>
    );
  }

  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        {items.map((item) => (
          <MediaCardVertical
            key={item.id}
            item={{
              ...item,
              communityScore: item.communityScore ?? null,
              listRank: item.listRank ?? null,
              releaseDate: item.releaseDate ?? null,
            } as any}
          />
        ))}
      </div>

      <div ref={sentinelRef} className="flex justify-center py-10">
        {loading && (
          <div className="flex items-center gap-3 text-sm font-bold text-gray-400">
            <span className="w-4 h-4 border-2 border-gray-600 border-t-blue-500 rounded-full animate-spin" />
            Loading more…
          </div>
        )}
        {failed && !loading && (
          <button
            onClick={loadMore}
            className="text-sm font-bold text-blue-400 hover:text-white bg-gray-900 border border-gray-800 hover:border-gray-600 rounded-lg px-4 py-2 transition-colors"
          >
            Couldn&apos;t load more. Try again
          </button>
        )}
        {done && items.length > 0 && (
          <p className="text-xs font-bold text-gray-600 uppercase tracking-widest">You&apos;ve reached the end</p>
        )}
      </div>
    </>
  );
}
