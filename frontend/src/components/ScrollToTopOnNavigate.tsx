"use client";

import { useEffect, useRef } from "react";
import { usePathname } from "next/navigation";

/**
 * Next.js sometimes keeps the previous page's scroll position when a new page streams in (for
 * example from a long Discover list to a media page), so the new page opened at its bottom, on
 * the reviews. This scrolls to the top whenever the path changes through a normal navigation.
 * Back/forward navigations are left alone so the browser can restore their position.
 * Query-only changes (filters, profile tabs) keep the pathname, so they don't scroll either.
 */
export default function ScrollToTopOnNavigate() {
  const pathname = usePathname();
  const previous = useRef(pathname);
  const historyNavigation = useRef(false);

  useEffect(() => {
    const onPopState = () => {
      historyNavigation.current = true;
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    if (previous.current === pathname) return;
    previous.current = pathname;
    if (historyNavigation.current) {
      historyNavigation.current = false;
      return;
    }
    if (window.location.hash) return;
    window.scrollTo({ top: 0, left: 0, behavior: "instant" as ScrollBehavior });
  }, [pathname]);

  return null;
}
