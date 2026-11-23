"use client";

import { createContext, useContext, useState } from "react";
import { DEFAULT_RATING_STYLE, type RatingStyle } from "@/lib/rating-style";

type RatingStyleContextValue = {
  ratingStyle: RatingStyle;
  setRatingStyle: (style: RatingStyle) => void;
};

const RatingStyleContext = createContext<RatingStyleContextValue>({
  ratingStyle: DEFAULT_RATING_STYLE,
  setRatingStyle: () => {},
});

export function RatingStyleProvider({
  initialStyle,
  children,
}: {
  initialStyle: RatingStyle;
  children: React.ReactNode;
}) {
  const [ratingStyle, setRatingStyle] = useState<RatingStyle>(initialStyle);
  return (
    <RatingStyleContext.Provider value={{ ratingStyle, setRatingStyle }}>
      {children}
    </RatingStyleContext.Provider>
  );
}

export function useRatingStyle() {
  return useContext(RatingStyleContext);
}
