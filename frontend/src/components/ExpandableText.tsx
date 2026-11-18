"use client";

import { useState } from "react";

interface ExpandableTextProps {
  text: string;
  maxLength?: number;
  lineClamp?: number;
}

export default function ExpandableText({ text, maxLength = 250, lineClamp }: ExpandableTextProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  if (!text) return <p className="text-gray-400 italic">No description available.</p>;

  // Replace <br> with actual newlines, strip remaining HTML tags, and collapse 3+ consecutive newlines to 2
  const cleanText = text
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]*>?/gm, '')
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();

  if (lineClamp) {
    const isLong = cleanText.length > 180 || cleanText.includes('\n');
    return (
      <div className="max-w-3xl">
        <p
          className={`text-gray-300 text-sm sm:text-base leading-relaxed whitespace-pre-wrap ${
            !isExpanded ? "line-clamp-3" : ""
          }`}
          style={
            !isExpanded
              ? {
                  display: "-webkit-box",
                  WebkitLineClamp: lineClamp,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }
              : undefined
          }
        >
          {cleanText}
        </p>
        {isLong && (
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-blue-400 hover:text-blue-300 font-semibold mt-2.5 transition-colors text-xs sm:text-sm uppercase tracking-wider inline-flex items-center gap-1 cursor-pointer"
          >
            {isExpanded ? "Show Less ↑" : "Read More ↓"}
          </button>
        )}
      </div>
    );
  }

  // Fallback character-based truncation
  const isLong = cleanText.length > maxLength;
  const displayText = isExpanded || !isLong ? cleanText : `${cleanText.slice(0, maxLength).trim()}...`;

  return (
    <div className="max-w-3xl">
      <p className="text-gray-300 text-sm sm:text-base leading-relaxed whitespace-pre-wrap">
        {displayText}
      </p>
      {isLong && (
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="text-blue-400 hover:text-blue-300 font-semibold mt-2.5 transition-colors text-xs sm:text-sm uppercase tracking-wider inline-flex items-center gap-1 cursor-pointer"
        >
          {isExpanded ? "Show Less ↑" : "Read More ↓"}
        </button>
      )}
    </div>
  );
}
