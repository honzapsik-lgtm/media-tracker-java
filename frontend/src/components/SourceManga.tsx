"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { coverSrc } from "@/lib/images";

interface RelatedMangaData {
  id: string | number;
  title: string;
  image?: string | null;
}

export default function SourceManga({
  mediaId,
  initialManga,
  isAnime = false,
}: {
  mediaId: string;
  initialManga?: RelatedMangaData | null;
  isAnime?: boolean;
}) {
  const [manga, setManga] = useState<RelatedMangaData | null>(initialManga || null);
  const [loading, setLoading] = useState<boolean>(!initialManga && isAnime);

  useEffect(() => {
    if (initialManga) {
      setManga(initialManga);
      setLoading(false);
      return;
    }

    if (!isAnime || !mediaId) {
      setLoading(false);
      return;
    }

    let isMounted = true;
    setLoading(true);

    fetch(`/api/media/${encodeURIComponent(mediaId)}/related-manga`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!isMounted) return;
        if (data && data.id && data.title) {
          setManga(data);
        } else {
          setManga(null);
        }
        setLoading(false);
      })
      .catch(() => {
        if (!isMounted) return;
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [initialManga, mediaId, isAnime]);

  if (loading) {
    return (
      <div>
        <h2 className="text-2xl font-bold mb-6 text-gray-400">Source Manga</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
          <div className="bg-gray-900 p-4 rounded-xl border border-gray-800 flex items-center gap-4 animate-pulse">
            <div className="w-14 h-20 bg-gray-800 rounded-lg shrink-0"></div>
            <div className="min-w-0 space-y-2 flex-1">
              <div className="h-3 w-12 bg-gray-800 rounded"></div>
              <div className="h-4 w-32 bg-gray-800 rounded"></div>
              <div className="h-3 w-20 bg-gray-800 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!manga) return null;

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6 text-gray-400">Source Manga</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        <Link 
          href={String(manga.id).startsWith("mangadex-") || String(manga.id).startsWith("anilist-") ? `/media/${manga.id}` : `/media/anilist-${manga.id}`} 
          className="bg-gray-900 p-4 rounded-xl border border-gray-800 hover:border-blue-500 hover:bg-gray-800/80 transition-colors flex items-center gap-4 group"
        >
          {manga.image ? (
            <img src={coverSrc(manga.image)} alt={manga.title} className="w-14 h-20 object-cover rounded-lg shadow-md shrink-0" />
          ) : (
            <div className="w-14 h-20 bg-gray-800 rounded-lg flex items-center justify-center text-xs text-gray-500 shrink-0">No Img</div>
          )}
          <div className="min-w-0">
            <span className="text-[10px] font-bold text-blue-400 uppercase tracking-widest">Manga</span>
            <p className="font-bold text-sm text-gray-200 group-hover:text-blue-400 transition-colors truncate">{manga.title}</p>
            <p className="text-xs text-gray-500 mt-1">Read on MangaDex →</p>
          </div>
        </Link>
      </div>
    </div>
  );
}
