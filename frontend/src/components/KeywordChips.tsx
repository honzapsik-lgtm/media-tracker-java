"use client";

import { useLayoutEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronDown } from "lucide-react";

/** Keyword chips limited to about three rows; a small chevron expands the rest when they overflow. */
export default function KeywordChips({ keywords, mediaTypeKey }: { keywords: string[]; mediaTypeKey: string }) {
  const listRef = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useState(false);
  const [overflows, setOverflows] = useState(false);

  useLayoutEffect(() => {
    const el = listRef.current;
    if (!el) return;
    const check = () => setOverflows(el.scrollHeight > el.clientHeight + 1);
    check();
    const observer = new ResizeObserver(check);
    observer.observe(el);
    return () => observer.disconnect();
  }, [keywords]);

  const plural = mediaTypeKey === "show" ? "shows" : `${mediaTypeKey}s`;

  return (
    <div>
      <div
        ref={listRef}
        className={`flex flex-wrap gap-1.5 overflow-hidden ${expanded ? "" : "max-h-[5.75rem]"}`}
      >
        {keywords.map((kw) => (
          <Link
            key={kw}
            href={`/discover?type=${mediaTypeKey}&keyword=${encodeURIComponent(kw)}`}
            title={`Browse ${plural} tagged "${kw}"`}
            className="bg-gray-900/80 hover:bg-gray-800 border border-gray-800 hover:border-gray-700 text-gray-400 hover:text-white px-2.5 py-1 rounded-md text-xs font-medium transition-colors"
          >
            {kw}
          </Link>
        ))}
      </div>
      {(overflows || expanded) && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          aria-expanded={expanded}
          aria-label={expanded ? "Show fewer keywords" : "Show all keywords"}
          className="mt-1.5 mx-auto flex items-center justify-center w-8 h-4 text-gray-600 hover:text-gray-300 transition-colors"
        >
          <ChevronDown className={`w-4 h-4 transition-transform ${expanded ? "rotate-180" : ""}`} />
        </button>
      )}
    </div>
  );
}
