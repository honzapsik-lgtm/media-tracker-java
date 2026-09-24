'use client';

import React, { useRef, useState, useEffect, useCallback } from 'react';

export default function Carousel({ children }: { children: React.ReactNode }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [showLeftArrow, setShowLeftArrow] = useState(false);
  const [showRightArrow, setShowRightArrow] = useState(false);

  const checkArrows = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const canScrollLeft = el.scrollLeft > 10;
    const canScrollRight = el.scrollLeft < el.scrollWidth - el.clientWidth - 10;
    setShowLeftArrow(canScrollLeft);
    setShowRightArrow(canScrollRight);
  }, []);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;

    checkArrows();
    const timer1 = setTimeout(checkArrows, 150);
    const timer2 = setTimeout(checkArrows, 500);

    const resizeObserver = new ResizeObserver(() => {
      checkArrows();
    });
    resizeObserver.observe(el);

    el.addEventListener('scroll', checkArrows, { passive: true });

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      resizeObserver.disconnect();
      el.removeEventListener('scroll', checkArrows);
    };
  }, [children, checkArrows]);

  const scroll = (direction: 'left' | 'right') => {
    const el = scrollRef.current;
    if (!el) return;
    const scrollAmount = direction === 'left' ? -el.clientWidth * 0.75 : el.clientWidth * 0.75;
    el.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  };

  return (
    <div className="relative group/carousel w-full">
      {/* Left Arrow Button */}
      {showLeftArrow && (
        <button
          type="button"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            scroll('left');
          }}
          className="absolute -left-3 sm:-left-5 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-gray-900/90 hover:bg-blue-600 border border-gray-700/80 hover:border-blue-400 text-white flex items-center justify-center transition-all shadow-2xl hover:scale-110 active:scale-95 cursor-pointer backdrop-blur-md"
          aria-label="Scroll Left"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
        </button>
      )}

      {/* Scrollable Container with vertical padding headroom to prevent hover scale clipping */}
      <div
        ref={scrollRef}
        className="flex overflow-x-auto pt-4 pb-4 -my-4 gap-6 snap-x hide-scrollbar scroll-smooth w-full -mx-6 px-6 sm:mx-0 sm:px-0"
      >
        {children}
      </div>

      {/* Right Arrow Button */}
      {showRightArrow && (
        <button
          type="button"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            scroll('right');
          }}
          className="absolute -right-3 sm:-right-5 top-1/2 -translate-y-1/2 z-30 w-11 h-11 rounded-full bg-gray-900/90 hover:bg-blue-600 border border-gray-700/80 hover:border-blue-400 text-white flex items-center justify-center transition-all shadow-2xl hover:scale-110 active:scale-95 cursor-pointer backdrop-blur-md"
          aria-label="Scroll Right"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </button>
      )}
    </div>
  );
}
