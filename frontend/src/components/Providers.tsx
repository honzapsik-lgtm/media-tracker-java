"use client";

import { SessionProvider } from "next-auth/react";
import { RatingStyleProvider } from "@/components/RatingStyleProvider";
import type { RatingStyle } from "@/lib/rating-style";

export function Providers({ children, ratingStyle }: { children: React.ReactNode; ratingStyle: RatingStyle }) {
  return (
    <SessionProvider>
      <RatingStyleProvider initialStyle={ratingStyle}>{children}</RatingStyleProvider>
    </SessionProvider>
  );
}
